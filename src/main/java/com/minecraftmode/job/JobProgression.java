package com.minecraftmode.job;

import com.minecraftmode.registry.ModAttachments;
import com.minecraftmode.registry.ModParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Levels, experience and class advancement. Experience comes from killing hostile mobs and mining
 * ores (see {@link JobEvents}); advancing needs a level and a completed trial (see {@code QuestService}).
 */
public final class JobProgression {
	public static final int MAX_LEVEL = 100;
	public static final int BASE_MANA = 30;
	/** Level needed for tier 1..4 (index = tier). */
	private static final int[] TIER_LEVEL = {1, 10, 25, 45, 70};

	public static JobData get(final Player player) {
		return player.getAttachedOrElse(ModAttachments.JOB, JobData.DEFAULT);
	}

	public static void set(final Player player, final JobData data) {
		player.setAttached(ModAttachments.JOB, data);
	}

	/** Experience needed to go from {@code level} to {@code level + 1}. */
	public static int expToNext(final int level) {
		return 25 + 10 * level + level * level / 4;
	}

	public static int levelForTier(final int tier) {
		return TIER_LEVEL[Math.max(0, Math.min(4, tier))];
	}

	public static void addExp(final ServerPlayer player, final int amount) {
		if (amount <= 0) {
			return;
		}
		JobData data = get(player);
		int level = data.level();
		int exp = data.exp() + amount;
		int gained = 0;
		while (level < MAX_LEVEL && exp >= expToNext(level)) {
			exp -= expToNext(level);
			level++;
			gained++;
		}
		if (level >= MAX_LEVEL) {
			// past the cap experience fills paragon levels
			Paragon.addExp(player, exp);
			exp = 0;
		}
		set(player, data.withProgress(level, exp));
		if (gained > 0) {
			onLevelUp(player, data.level(), level);
		}
	}

	private static void onLevelUp(final ServerPlayer player, final int from, final int to) {
		ServerLevel level = player.level();
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.3F);
		JobData data = get(player);
		int color = data.job().color();
		level.sendParticles(ColorParticleOption.create(ModParticles.SPARK, 0xFF000000 | color), player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.5, 1.0, 0.5, 0.15);
		level.sendParticles(ColorParticleOption.create(ModParticles.RING, 0xFF000000 | color), player.getX(), player.getY() + 0.1, player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.job.level_up", to).withStyle(ChatFormatting.GOLD));
		JobStats.refresh(player);
		for (int tier = data.tier() + 1; tier <= 4; tier++) {
			int need = levelForTier(tier);
			if (need > from && need <= to) {
				player.sendSystemMessage(Component.translatable("message.minecraft_mode.job.can_advance").withStyle(ChatFormatting.AQUA));
				break;
			}
		}
	}

	/** Death costs 10% of the progress inside the current level; levels are never lost. */
	public static void applyDeathPenalty(final ServerPlayer player) {
		JobData data = get(player);
		if (data.exp() > 0) {
			set(player, data.withProgress(data.level(), data.exp() * 9 / 10));
		}
	}

	public enum AdvanceResult {
		OK,
		MAX_TIER,
		NEED_CLASS,
		LEVEL
	}

	/**
	 * Level and tier check only; the materials are handled by the advancement trial
	 * ({@code QuestService}). {@code choice} is only used for the first advancement.
	 */
	public static AdvanceResult canAdvance(final Player player, final JobClass choice) {
		JobData data = get(player);
		int next = data.tier() + 1;
		if (next > 4) {
			return AdvanceResult.MAX_TIER;
		}
		if (data.tier() == 0 && choice == JobClass.NONE) {
			return AdvanceResult.NEED_CLASS;
		}
		if (data.level() < levelForTier(next)) {
			return AdvanceResult.LEVEL;
		}
		return AdvanceResult.OK;
	}

	public static AdvanceResult advance(final ServerPlayer player, final JobClass choice) {
		AdvanceResult result = canAdvance(player, choice);
		if (result != AdvanceResult.OK) {
			return result;
		}
		JobData data = get(player);
		int next = data.tier() + 1;
		JobClass job = data.tier() == 0 ? choice : data.job();
		set(player, data.withJob(job, next));
		JobStats.refresh(player);

		ServerLevel level = player.level();
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);
		level.sendParticles(ColorParticleOption.create(ModParticles.RUNE, 0xFF000000 | job.color()), player.getX(), player.getY() + 0.1, player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ColorParticleOption.create(ModParticles.SPARK, 0xFF000000 | job.color()), player.getX(), player.getY() + 1.0, player.getZ(), 80, 0.6, 1.2, 0.6, 0.2);
		Component title = Component.translatable(job.tierKey(next)).withColor(job.color());
		level.getServer().getPlayerList().broadcastSystemMessage(
			Component.translatable("message.minecraft_mode.job.advanced", player.getDisplayName(), title), false
		);
		return AdvanceResult.OK;
	}

	/** Back to no class; level and experience are kept. */
	public static void resetClass(final ServerPlayer player) {
		JobData data = get(player);
		set(player, data.withJob(JobClass.NONE, 0).withCooldowns(java.util.Map.of()));
		JobStats.refresh(player);
	}

	public static int count(final Inventory inventory, final Item item) {
		int total = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	public static void removeItems(final Inventory inventory, final Item item, int amount) {
		for (int i = 0; i < inventory.getContainerSize() && amount > 0; i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(item)) {
				int take = Math.min(amount, stack.getCount());
				stack.shrink(take);
				amount -= take;
			}
		}
	}

	private JobProgression() {
	}
}
