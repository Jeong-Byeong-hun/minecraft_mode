package com.minecraftmode.worldgen.lair;

import com.minecraftmode.job.JobEvents;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.registry.ModAttachments;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Class experience from the lairs, the main road to the level cap: the first walk into each lair's grounds, the first clear of each
 * lair, and every cycle's treasure (the lord's kill pays through {@code JobEvents}). Every amount is a share of a level measured at
 * the lair's top level ({@link JobProgression#levelExp}), so a high level gains little from an easy lair.
 */
public final class LairExp {
	/** First walk into a lair's grounds, once per lair. */
	public static final float DISCOVER = 0.3F;
	/** First goal chest a player ever opens in a lair, on top of {@link #CHEST}. */
	public static final float FIRST_CLEAR = 1.0F;
	/** A goal chest opened for the first time this cycle. */
	public static final float CHEST = 0.3F;
	/** A dead-end cache opened for the first time this cycle. */
	public static final float CACHE = 0.05F;

	public static List<String> found(final Player player) {
		return player.getAttachedOrElse(ModAttachments.LAIRS_FOUND, List.of());
	}

	/** {@code player} walked into {@code lair}'s grounds: the first time pays {@link #DISCOVER}. */
	public static void entered(final ServerPlayer player, final LairDef lair) {
		List<String> found = found(player);
		if (found.contains(lair.id())) {
			return;
		}
		List<String> updated = new ArrayList<>(found);
		updated.add(lair.id());
		player.setAttached(ModAttachments.LAIRS_FOUND, List.copyOf(updated));
		int exp = JobProgression.levelExp(player, lair.named().hi(), DISCOVER);
		JobEvents.gainExp(player, exp);
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.lair.discovered", Component.translatable(lair.nameKey()), exp)
			.withStyle(ChatFormatting.AQUA));
	}

	/** A chest of {@code lair} opened fresh this cycle; {@code firstClear} when it is the player's first goal chest there ever. */
	public static void chestOpened(final ServerPlayer player, final LairDef lair, final boolean cache, final boolean firstClear) {
		float share = cache ? CACHE : CHEST + (firstClear ? FIRST_CLEAR : 0.0F);
		int exp = JobProgression.levelExp(player, lair.named().hi(), share);
		JobEvents.gainExp(player, exp);
		player.sendSystemMessage(Component.translatable(firstClear ? "message.minecraft_mode.lair.first_clear" : "message.minecraft_mode.lair.chest_exp",
			Component.translatable(lair.nameKey()), exp).withStyle(firstClear ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
	}

	private LairExp() {
	}
}
