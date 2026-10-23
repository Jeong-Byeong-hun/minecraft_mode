package com.minecraftmode.townquest;

import com.minecraftmode.bag.Bags;
import com.minecraftmode.bounty.Bounties;
import com.minecraftmode.craft.Profession;
import com.minecraftmode.entity.CityNpc;
import com.minecraftmode.entity.named.NamedMob;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.jump.JumpForest;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.loot.GearShop;
import com.minecraftmode.network.OpenTownQuestPayload;
import com.minecraftmode.network.TownQuestActionPayload;
import com.minecraftmode.progress.RealReset;
import com.minecraftmode.registry.ModAttachments;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.townquest.TownQuests.Goal;
import com.minecraftmode.townquest.TownQuests.Quest;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * The rules of town quests ({@link TownQuests}): accepting at the giver, progress (kills through {@code Progress.afterDeath}, so party
 * members nearby share them like bounties; lairs, dungeons, ores, fish, crops, crafting and the jump course through their own hooks),
 * turning in (deliveries are taken then, inventory first and then the bags) and abandoning. Only accepted quests count. Daily quests
 * open again on the next real day ({@link RealReset#day}).
 */
public final class TownQuestService {
	/** Where a quest stands for a player. */
	public enum Status {
		/** The player's class level is below the quest's. */
		LOCKED,
		AVAILABLE,
		ACTIVE,
		/** Accepted and finished (a delivery: the items are carried): it can be turned in. */
		READY,
		/** Turned in today (daily) or ever (one-time). */
		DONE
	}

	/** What a quest pays {@code player} now. */
	public record Reward(int exp, int coins, int stones, int merit, @Nullable Item bonus, int bonusCount) {
	}

	public static void init() {
		JumpForest.FINISHED.add((player, course) -> progress(player, Goal.JUMP, course, 1));
	}

	public static TownQuestData data(final Player player) {
		return player.getAttachedOrElse(ModAttachments.TOWN_QUESTS, TownQuestData.DEFAULT);
	}

	public static void set(final ServerPlayer player, final TownQuestData data) {
		player.setAttached(ModAttachments.TOWN_QUESTS, data);
	}

	private static int level(final Player player) {
		return Math.max(1, JobProgression.get(player).level());
	}

	// ------------------------------------------------------------------ state

	/** Where {@code quest} stands for {@code player} on real day {@code day}. */
	public static Status status(final Player player, final Quest quest, final long day) {
		TownQuestData data = data(player);
		if (data.isActive(quest.id())) {
			return ready(player, quest) ? Status.READY : Status.ACTIVE;
		}
		Long done = data.done().get(quest.id());
		if (done != null && (!quest.daily() || done == day)) {
			return Status.DONE;
		}
		return level(player) < quest.minLevel() ? Status.LOCKED : Status.AVAILABLE;
	}

	/** How far along an accepted quest is: the progress, or for a delivery the items carried (inventory and bags), capped at the need. */
	public static int progress(final Player player, final Quest quest) {
		if (quest.goal() == Goal.DELIVER) {
			Item item = item(quest.target());
			return item == Items.AIR ? 0 : Math.min(quest.need(), Bags.count(player, item));
		}
		return Math.min(quest.need(), data(player).progress(quest.id()));
	}

	public static boolean ready(final Player player, final Quest quest) {
		return data(player).isActive(quest.id()) && progress(player, quest) >= quest.need();
	}

	/**
	 * Enhancement stones only from this class level on: they are not bound and characters share the ender chest, so fresh characters
	 * doing the dailies would otherwise farm them for the main one.
	 */
	public static final int STONE_LEVEL = 10;

	public static Reward reward(final Player player, final Quest quest) {
		int level = level(player);
		return new Reward(JobProgression.levelExp(player, level, quest.tier().exp), coins(quest, level), level >= STONE_LEVEL ? quest.tier().stones : 0,
			quest.tier().merit, quest.bonus(), quest.bonusCount());
	}

	/** Coins {@code quest} pays at class level {@code level}. */
	public static int coins(final Quest quest, final int level) {
		return Math.max(3, Math.round(GearShop.bracketPrice(Math.max(10, level)) * quest.tier().coins));
	}

	// ------------------------------------------------------------------ actions

	public static boolean accept(final ServerPlayer player, final Quest quest) {
		if (status(player, quest, today(player)) != Status.AVAILABLE) {
			return false;
		}
		set(player, data(player).withProgress(quest.id(), 0));
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.townquest.accepted", Component.translatable(quest.titleKey()), describe(quest))
			.withStyle(ChatFormatting.YELLOW));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 1.0F);
		if (ready(player, quest)) {
			player.sendSystemMessage(done(quest));
		}
		return true;
	}

	/** Hands in a finished quest: takes the delivery, pays the reward and marks it done today. */
	public static boolean turnIn(final ServerPlayer player, final Quest quest) {
		if (status(player, quest, today(player)) != Status.READY) {
			return false;
		}
		if (quest.goal() == Goal.DELIVER) {
			Bags.take(player, item(quest.target()), quest.need());
		}
		Reward reward = reward(player, quest);
		set(player, data(player).finished(quest.id(), today(player)));
		JobProgression.addExp(player, reward.exp());
		Coins.give(player, reward.coins());
		if (reward.stones() > 0) {
			Bags.give(player, new ItemStack(ModItems.ENHANCEMENT_STONE, reward.stones()));
		}
		if (reward.bonus() != null && reward.bonusCount() > 0) {
			Bags.give(player, new ItemStack(reward.bonus(), reward.bonusCount()));
		}
		Bounties.addMerit(player, reward.merit());
		MutableComponent paid = Component.translatable("message.minecraft_mode.townquest.reward", reward.exp(), Coins.component(reward.coins()), reward.stones(),
			reward.merit());
		if (reward.bonus() != null) {
			paid.append(", ").append(new ItemStack(reward.bonus(), reward.bonusCount()).getHoverName()).append(" x" + reward.bonusCount());
		}
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.townquest.turned_in", Component.translatable(quest.titleKey()), paid)
			.withStyle(ChatFormatting.GREEN));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.5F);
		return true;
	}

	public static boolean abandon(final ServerPlayer player, final Quest quest) {
		if (!data(player).isActive(quest.id())) {
			return false;
		}
		set(player, data(player).without(quest.id()));
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.townquest.abandoned", Component.translatable(quest.titleKey())).withStyle(ChatFormatting.GRAY));
		return true;
	}

	/** Counts {@code amount} toward every accepted quest of {@code goal} whose target is {@code target} (or that has none). */
	public static void progress(final ServerPlayer player, final Goal goal, final String target, final int amount) {
		if (amount <= 0 || goal == Goal.DELIVER) {
			return;
		}
		TownQuestData data = data(player);
		if (data.active().isEmpty()) {
			return;
		}
		TownQuestData next = data;
		for (String id : data.active().keySet()) {
			Quest quest = TownQuests.get(id);
			if (quest == null || quest.goal() != goal || !quest.target().isEmpty() && !quest.target().equals(target)) {
				continue;
			}
			int before = data.progress(id);
			if (before >= quest.need()) {
				continue;
			}
			int after = Math.min(quest.need(), before + amount);
			next = next.withProgress(id, after);
			if (after >= quest.need()) {
				player.sendSystemMessage(done(quest));
				player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.7F, 1.4F);
			} else {
				player.sendOverlayMessage(Component.translatable("message.minecraft_mode.townquest.progress", Component.translatable(quest.titleKey()), after, quest.need())
					.withStyle(ChatFormatting.YELLOW));
			}
		}
		if (next != data) {
			set(player, next);
		}
	}

	/** A kill credited to {@code player} ({@code Progress.afterDeath}: contributors and their party members nearby). */
	public static void killed(final ServerPlayer player, final LivingEntity dead) {
		if (data(player).active().isEmpty()) {
			return;
		}
		progress(player, Goal.KILL, BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType()).toString(), 1);
		if (dead instanceof NamedMob named) {
			progress(player, Goal.KILL_NAMED, named.def().id(), 1);
			if (named.isLord()) {
				progress(player, Goal.KILL_LORD, named.def().id(), 1);
			}
		}
	}

	private static Component done(final Quest quest) {
		return Component.translatable("message.minecraft_mode.townquest.complete", Component.translatable(quest.titleKey()),
			Component.translatable(quest.giver().nameKey()).withColor(quest.giver().color())).withStyle(ChatFormatting.GOLD);
	}

	// ------------------------------------------------------------------ screen and network

	private static long today(final ServerPlayer player) {
		return RealReset.day(player.level().getServer());
	}

	public static OpenTownQuestPayload screen(final ServerPlayer player, final int entityId, final CityNpc.Role role) {
		MinecraftServer server = player.level().getServer();
		return new OpenTownQuestPayload(entityId, role.id(), RealReset.day(server), RealReset.ticksToNextDay(server));
	}

	/** A resident's right-click: their quest screen. */
	public static void open(final ServerPlayer player, final int entityId, final CityNpc.Role role) {
		if (ServerPlayNetworking.canSend(player, OpenTownQuestPayload.TYPE)) {
			ServerPlayNetworking.send(player, screen(player, entityId, role));
		}
	}

	/** One action from the quest screen; the caller checked that the quest's giver stands within reach. */
	public static void act(final ServerPlayer player, final Quest quest, final int action) {
		switch (action) {
			case TownQuestActionPayload.ACCEPT -> accept(player, quest);
			case TownQuestActionPayload.TURN_IN -> turnIn(player, quest);
			case TownQuestActionPayload.ABANDON -> abandon(player, quest);
			case TownQuestActionPayload.ENTER -> {
				if (quest.goal() == Goal.JUMP && status(player, quest, today(player)) != Status.LOCKED) {
					// going in from a quest not taken yet takes it, or the climb would count for nothing
					if (status(player, quest, today(player)) == Status.AVAILABLE) {
						accept(player, quest);
					}
					JumpForest.enter(player, quest.target());
				}
			}
			default -> {
			}
		}
	}

	// ------------------------------------------------------------------ text

	public static Item item(final String id) {
		Identifier key = Identifier.tryParse(id);
		return key == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(key);
	}

	/** The objective: "Defeat 15 Skeleton", "Bring 32 Wheat", "Reach the end of the Forest of Tenacity" ... */
	public static Component describe(final Quest quest) {
		Component target = switch (quest.goal()) {
			case KILL -> {
				Identifier id = Identifier.tryParse(quest.target());
				yield id == null ? Component.literal(quest.target())
					: BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(t -> (Component)t.getDescription()).orElse(Component.literal(quest.target()));
			}
			case DELIVER -> Component.translatable(item(quest.target()).getDescriptionId());
			case CRAFT -> {
				Profession profession = Profession.byId(quest.target());
				yield profession == null ? Component.empty() : Component.translatable(profession.nameKey());
			}
			default -> Component.empty();
		};
		return Component.translatable(quest.goal().key(), quest.need(), target);
	}

	private TownQuestService() {
	}
}
