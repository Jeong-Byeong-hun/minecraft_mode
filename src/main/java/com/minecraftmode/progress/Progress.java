package com.minecraftmode.progress;

import com.minecraftmode.bounty.Bounties;
import com.minecraftmode.bounty.BountyKind;
import com.minecraftmode.craft.Profession;
import com.minecraftmode.economy.Wallet;
import com.minecraftmode.entity.named.NamedMob;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.Parties;
import com.minecraftmode.raid.RaidDifficulty;
import com.minecraftmode.registry.ModAttachments;
import com.minecraftmode.story.Story;
import com.minecraftmode.talent.Talents;
import com.minecraftmode.worldgen.lair.LairDef;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * The player's record book: everything that counts toward the codex, achievements, titles, collection
 * bonuses and bounties goes through here (kills, lair clears, raid clears, finished bounties, sales,
 * enhancement). New achievements are announced and pay their merit.
 */
public final class Progress {
	/** Party members this close to a kill share it (codex, achievements and bounties). */
	public static final double SHARE_RANGE = 48.0;

	public static void init() {
		ServerLivingEntityEvents.AFTER_DEATH.register(Progress::afterDeath);
		ServerPlayerEvents.JOIN.register(Titles::apply);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 100 == 0) {
				for (ServerPlayer p : server.getPlayerList().getPlayers()) {
					Bounties.ensure(p);
					check(p);
				}
			}
		});
	}

	public static PlayerRecords get(final Player player) {
		return player.getAttachedOrElse(ModAttachments.RECORDS, PlayerRecords.DEFAULT);
	}

	public static void set(final ServerPlayer player, final PlayerRecords records) {
		player.setAttached(ModAttachments.RECORDS, records);
	}

	// ------------------------------------------------------------------ events

	private static void afterDeath(final LivingEntity entity, final DamageSource source) {
		if (!(source.getEntity() instanceof ServerPlayer killer)) {
			return;
		}
		for (ServerPlayer player : sharers(killer, entity, Parties.onlineMembers(killer))) {
			if (entity instanceof NamedMob named) {
				set(player, get(player).withNamedKill(named.def().id()));
				Bounties.progress(player, BountyKind.KILL_NAMED, named.def().id(), 1);
			}
			if (entity instanceof Enemy) {
				Bounties.progress(player, BountyKind.KILL_ANY, "", 1);
				Bounties.progress(player, BountyKind.KILL_TYPE, BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(), 1);
			}
			check(player);
		}
	}

	/**
	 * Who a kill counts for: the killer, and every other member of their party in the same world within {@link #SHARE_RANGE} of
	 * the kill, so a party hunting together fills its codex and bounties together.
	 */
	public static List<ServerPlayer> sharers(final ServerPlayer killer, final LivingEntity dead, final List<ServerPlayer> party) {
		List<ServerPlayer> out = new ArrayList<>();
		out.add(killer);
		for (ServerPlayer member : party) {
			if (member != killer && !member.isSpectator() && member.level() == dead.level() && member.distanceToSqr(dead) <= SHARE_RANGE * SHARE_RANGE) {
				out.add(member);
			}
		}
		return out;
	}

	public static void lairCleared(final ServerPlayer player, final LairDef lair) {
		set(player, get(player).withLairClear(lair.id()));
		Bounties.progress(player, BountyKind.CLEAR_LAIR, lair.id(), 1);
		check(player);
	}

	public static void raidCleared(final ServerPlayer player, final BossDef boss, final RaidDifficulty difficulty, final long cycle) {
		set(player, get(player).withRaidClear(PlayerRecords.raidKey(boss.id(), difficulty.id()), cycle));
		Bounties.progress(player, BountyKind.RAID, boss.id(), 1);
		check(player);
	}

	public static void oreMined(final ServerPlayer player) {
		Bounties.progress(player, BountyKind.MINE_ORE, "", 1);
	}

	public static void bountyDone(final ServerPlayer player) {
		set(player, get(player).withBountyDone());
		check(player);
	}

	public static void marketSold(final ServerPlayer player, final int sales) {
		PlayerRecords records = get(player);
		for (int i = 0; i < sales; i++) {
			records = records.withMarketSale();
		}
		set(player, records);
		check(player);
	}

	/** A pet or mount joined the collection (story chapters watch this). */
	public static void companionLearned(final ServerPlayer player) {
		check(player);
	}

	/** A profession reached {@code level} (story chapters watch this). */
	public static void professionLevel(final ServerPlayer player, final Profession profession, final int level) {
		check(player);
	}

	/** A dungeon run was cleared (the dungeon records are already updated). */
	public static void dungeonCleared(final ServerPlayer player) {
		check(player);
	}

	/** {@code player} helped defeat a world boss. */
	public static void worldBossDefeated(final ServerPlayer player) {
		player.setAttached(ModAttachments.STORY, Story.data(player).withWorldBoss());
		check(player);
	}

	public static void awakened(final ServerPlayer player, final int awaken) {
		set(player, get(player).withAwaken(awaken));
		check(player);
	}

	/** {@code player} helped repel an invasion of the capital. */
	public static void invasionRepelled(final ServerPlayer player) {
		set(player, get(player).withInvasion());
		check(player);
	}

	/** A paragon level or anything else achievements watch changed. */
	public static void changed(final ServerPlayer player) {
		check(player);
	}

	public static void enhanced(final ServerPlayer player, final int level) {
		set(player, get(player).withEnhance(level));
		check(player);
	}

	// ------------------------------------------------------------------ achievements and titles

	public static Achievements.State state(final Player player) {
		return new Achievements.State(get(player), JobProgression.get(player), Wallet.balance(player), Talents.spent(player), player);
	}

	/** Unlocks every achievement that is now reached; each pays its merit and is announced. */
	public static void check(final ServerPlayer player) {
		Achievements.State state = state(player);
		PlayerRecords records = state.records();
		boolean changed = false;
		for (Achievements.Achievement a : Achievements.all()) {
			if (records.has(a.id()) || !a.done(state)) {
				continue;
			}
			records = records.withAchievement(a.id());
			changed = true;
			Bounties.addMerit(player, a.merit());
			player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 50, 15));
			player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("message.minecraft_mode.achievement.title").withStyle(ChatFormatting.GOLD)));
			player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(a.nameKey()).withStyle(ChatFormatting.YELLOW)));
			player.sendSystemMessage(Component.translatable("message.minecraft_mode.achievement.unlocked", Component.translatable(a.nameKey()), a.merit())
				.withStyle(ChatFormatting.GOLD));
			if (a.hasTitle()) {
				player.sendSystemMessage(Component.translatable("message.minecraft_mode.achievement.title_unlocked", Component.translatable(a.titleKey()))
					.withStyle(ChatFormatting.LIGHT_PURPLE));
			}
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.7F, 1.2F);
		}
		if (changed) {
			set(player, records);
		}
		Story.check(player);
	}

	/** Wears the title of achievement {@code id} ("" takes it off). Returns false when it is not unlocked. */
	public static boolean setTitle(final ServerPlayer player, final String id) {
		PlayerRecords records = get(player);
		if (!id.isEmpty()) {
			Achievements.Achievement a = Achievements.get(id);
			if (a == null || !a.hasTitle() || !records.has(id)) {
				return false;
			}
		}
		set(player, records.withTitle(id));
		Titles.apply(player);
		return true;
	}

	private Progress() {
	}
}
