package com.minecraftmode.job.gear;

import com.minecraftmode.job.JobProgression;

/**
 * Item levels: every class weapon and armor piece needs a level, falls into a 10-level bracket
 * (10, 20, ..., 100) and needs the class tier that level belongs to.
 */
public final class ItemLevels {
	public static final int MIN_BRACKET = 10;
	public static final int MAX_BRACKET = 100;

	/** 10..100. */
	public static int bracket(final int level) {
		return Math.max(MIN_BRACKET, Math.min(MAX_BRACKET, level / 10 * 10));
	}

	/** Class tier an item of this level belongs to (10-24: 1, 25-44: 2, 45-69: 3, 70+: 4). */
	public static int tier(final int level) {
		for (int tier = 4; tier >= 1; tier--) {
			if (level >= JobProgression.levelForTier(tier)) {
				return tier;
			}
		}
		return 1;
	}

	/** Chance that a named monster of this level drops a piece of gear: 38% at Lv 10 down to 6.5% at Lv 100. */
	public static float dropChance(final int level) {
		return Math.max(0.06F, 0.38F - 0.0035F * (level - 10));
	}

	private ItemLevels() {
	}
}
