package com.minecraftmode.job.quest;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.quest.QuestDef.KillGoal;
import com.minecraftmode.job.quest.QuestDef.Material;
import com.minecraftmode.registry.ModAttachments;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Accepting, tracking and completing advancement trials. Class trainers call {@link #accept} and
 * {@link #complete}; kills are counted in {@link #onDeath}. Every check works on the client too
 * (the data is synced), so screens can show the same state the server will enforce.
 */
public final class QuestService {
	public enum Status {
		/** The trainer offers its next trial and the player may take it. */
		AVAILABLE,
		LOW_LEVEL,
		/** The player follows another class. */
		OTHER_CLASS,
		/** The player is doing another trainer's trial. */
		BUSY,
		IN_PROGRESS,
		READY,
		MAX_TIER
	}

	public static void init() {
		ServerLivingEntityEvents.AFTER_DEATH.register(QuestService::onDeath);
	}

	public static QuestData get(final Player player) {
		return player.getAttachedOrElse(ModAttachments.QUEST, QuestData.DEFAULT);
	}

	public static void set(final Player player, final QuestData data) {
		player.setAttached(ModAttachments.QUEST, data);
	}

	public static @Nullable QuestDef active(final Player player) {
		QuestData data = get(player);
		return data.hasQuest() ? Quests.get(data.active()) : null;
	}

	/** The trial this trainer would give the player next, or null (other class / final tier). */
	public static @Nullable QuestDef offered(final Player player, final JobClass trainer) {
		JobData job = JobProgression.get(player);
		if (job.tier() == 0) {
			return Quests.forTier(trainer, 1);
		}
		if (job.job() == trainer && job.tier() < 4) {
			return Quests.forTier(trainer, job.tier() + 1);
		}
		return null;
	}

	public static Status status(final Player player, final JobClass trainer) {
		JobData job = JobProgression.get(player);
		QuestDef active = active(player);
		if (active != null) {
			if (active.job() != trainer) {
				return Status.BUSY;
			}
			return isReady(player, active) ? Status.READY : Status.IN_PROGRESS;
		}
		if (job.tier() > 0 && job.job() != trainer) {
			return Status.OTHER_CLASS;
		}
		if (job.tier() >= 4) {
			return Status.MAX_TIER;
		}
		QuestDef next = offered(player, trainer);
		if (next == null) {
			return Status.OTHER_CLASS;
		}
		return job.level() < JobProgression.levelForTier(next.tier()) ? Status.LOW_LEVEL : Status.AVAILABLE;
	}

	public static boolean goalsDone(final Player player, final QuestDef quest) {
		QuestData data = get(player);
		for (int i = 0; i < quest.kills().size(); i++) {
			if (data.progress(i) < quest.kills().get(i).count()) {
				return false;
			}
		}
		return true;
	}

	public static boolean isReady(final Player player, final QuestDef quest) {
		if (!goalsDone(player, quest)) {
			return false;
		}
		if (player.isCreative()) {
			return true;
		}
		if (JobProgression.count(player.getInventory(), quest.token()) < quest.tokenCount()) {
			return false;
		}
		for (Material material : quest.materials()) {
			if (JobProgression.count(player.getInventory(), material.item()) < material.count()) {
				return false;
			}
		}
		return true;
	}

	public static boolean accept(final ServerPlayer player, final JobClass trainer) {
		if (status(player, trainer) != Status.AVAILABLE) {
			return false;
		}
		QuestDef quest = offered(player, trainer);
		set(player, get(player).start(quest));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.quest.accepted", Component.translatable(quest.nameKey()).withColor(quest.job().color()))
			.withStyle(ChatFormatting.YELLOW));
		return true;
	}

	public static boolean complete(final ServerPlayer player, final JobClass trainer) {
		QuestDef quest = active(player);
		if (quest == null || quest.job() != trainer || !isReady(player, quest)) {
			return false;
		}
		if (!player.isCreative()) {
			JobProgression.removeItems(player.getInventory(), quest.token(), quest.tokenCount());
			for (Material material : quest.materials()) {
				JobProgression.removeItems(player.getInventory(), material.item(), material.count());
			}
		}
		set(player, get(player).cleared());
		JobProgression.advance(player, quest.job());
		return true;
	}

	public static void abandon(final ServerPlayer player) {
		QuestDef quest = active(player);
		if (quest != null) {
			set(player, get(player).cleared());
			player.sendSystemMessage(Component.translatable("message.minecraft_mode.quest.abandoned", Component.translatable(quest.nameKey()))
				.withStyle(ChatFormatting.GRAY));
		}
	}

	// ------------------------------------------------------------------ kills

	private static void onDeath(final LivingEntity entity, final DamageSource source) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return;
		}
		ServerPlayer killer = source.getEntity() instanceof ServerPlayer p ? p : null;
		boolean byProjectile = source.getDirectEntity() instanceof Projectile;
		List<ServerPlayer> credited;
		if (Quests.BOSSES.contains(entity.getType())) {
			credited = level.getPlayers(p -> p.isAlive() && !p.isSpectator() && p.distanceToSqr(entity) <= 64.0 * 64.0);
		} else if (killer != null) {
			credited = List.of(killer);
		} else {
			return;
		}
		for (ServerPlayer player : credited) {
			credit(player, entity, player == killer && byProjectile);
		}
	}

	static void credit(final ServerPlayer player, final LivingEntity entity, final boolean byProjectile) {
		QuestDef quest = active(player);
		if (quest == null) {
			return;
		}
		QuestData data = get(player);
		QuestData updated = data;
		for (int i = 0; i < quest.kills().size(); i++) {
			KillGoal goal = quest.kills().get(i);
			int have = updated.progress(i);
			if (have < goal.count() && goal.matches(entity.getType(), byProjectile)) {
				updated = updated.withProgress(i, have + 1);
				Component name = Component.translatable(quest.goalKey(i));
				player.sendOverlayMessage(Component.translatable("message.minecraft_mode.quest.progress", name, have + 1, goal.count())
					.withStyle(have + 1 >= goal.count() ? ChatFormatting.GREEN : ChatFormatting.WHITE));
			}
		}
		if (updated != data) {
			set(player, updated);
		}
		for (QuestDef.TokenSource tokenSource : quest.sources()) {
			if (tokenSource.types().contains(entity.getType()) && player.getRandom().nextFloat() < tokenSource.chance()) {
				player.getInventory().placeItemBackInInventory(new ItemStack(quest.token(), tokenSource.amount()), Prediction.SERVER_ONLY);
				int have = JobProgression.count(player.getInventory(), quest.token());
				player.sendSystemMessage(Component.translatable("message.minecraft_mode.quest.token",
					Component.translatable(quest.token().getDescriptionId()).withStyle(ChatFormatting.LIGHT_PURPLE), Math.min(have, quest.tokenCount()), quest.tokenCount()));
				player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.4F);
				break;
			}
		}
	}

	private QuestService() {
	}
}
