package com.minecraftmode.worldgen.lair;

import com.minecraftmode.entity.named.NamedMob;
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
 * lair, every cycle's treasure and caches, and the lord's kill (paid through {@code JobEvents} with {@link #lordExp}). Every amount is a
 * share of a level ({@link JobProgression#levelExp}) measured at the level of what was actually fought, or the player's own level when that
 * is lower: the lair's top level, or the level its lord was beaten at when it rose for a stronger player (0.5.36, with the lair map
 * loop). A strong player who lets the lord rise to their level is paid at their level; one who kills a weak lord, opens caches without a
 * lord or walks into low lairs is paid at the lair's level. The shares were doubled at the same time.
 */
public final class LairExp {
	/** First walk into a lair's grounds, once per lair. */
	public static final float DISCOVER = 0.6F;
	/** First goal chest a player ever opens in a lair, on top of {@link #CHEST}. */
	public static final float FIRST_CLEAR = 2.0F;
	/** A goal chest opened for the first time this cycle. */
	public static final float CHEST = 0.6F;
	/** A dead-end cache opened for the first time this cycle. */
	public static final float CACHE = 0.1F;
	/** A lair lord's kill (shared by contribution like every kill). */
	public static final float LORD = 1.0F;

	public static List<String> found(final Player player) {
		return player.getAttachedOrElse(ModAttachments.LAIRS_FOUND, List.of());
	}

	/** {@code share} of a level for {@code player}, measured at {@code contentLevel} or the player's own level, whichever is lower. */
	public static int exp(final Player player, final int contentLevel, final float share) {
		return JobProgression.levelExp(player, contentLevel, share);
	}

	/** What a lair lord's kill pays {@code player} before the contribution split: at the lord's level (raised or not), capped by the player's. */
	public static int lordExp(final Player player, final NamedMob lord) {
		return JobProgression.levelExp(player, lord.effectiveLevel(), LORD);
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
		int exp = exp(player, lair.named().hi(), DISCOVER);
		JobEvents.gainExp(player, exp);
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.lair.discovered", Component.translatable(lair.nameKey()), exp)
			.withStyle(ChatFormatting.AQUA));
	}

	/**
	 * A chest of {@code lair} opened fresh this cycle; {@code firstClear} when it is the player's first goal chest there ever. {@code contentLevel}:
	 * the chest's {@link LairChestBlockEntity#contentLevel} (the lair's top level, or the level the player beat its lord at).
	 */
	public static void chestOpened(final ServerPlayer player, final LairDef lair, final boolean cache, final boolean firstClear, final int contentLevel) {
		float share = cache ? CACHE : CHEST + (firstClear ? FIRST_CLEAR : 0.0F);
		int exp = exp(player, contentLevel, share);
		JobEvents.gainExp(player, exp);
		player.sendSystemMessage(Component.translatable(firstClear ? "message.minecraft_mode.lair.first_clear" : "message.minecraft_mode.lair.chest_exp",
			Component.translatable(lair.nameKey()), exp).withStyle(firstClear ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
	}

	private LairExp() {
	}
}
