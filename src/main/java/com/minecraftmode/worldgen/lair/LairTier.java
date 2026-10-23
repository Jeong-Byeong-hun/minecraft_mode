package com.minecraftmode.worldgen.lair;

import com.minecraftmode.dungeon.DungeonAffix;
import com.minecraftmode.enhance.ImbueSlot;
import com.minecraftmode.loot.EvolutionEtherItem;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Lair map tiers 1..{@link #MAX} (0.5.49): the numbers behind tier lairs ({@link TierLairs}), the map combiner and their rewards. A lair map
 * a class Lv 100 character gets from a clear carries a tier ({@link ModDataComponents#LAIR_TIER}); every tier lair stands at Lv 100 with
 * health and damage multiplied by the tier, and from tier 6 dungeon affixes join. Pure functions: tests read the same numbers.
 */
public final class LairTier {
	public static final int MAX = 20;
	/** Odds of 1..5 maps from a tier lair clear (40/28/17/10/5, about 2.1 maps). */
	private static final int[] MAP_COUNT_WEIGHTS = {40, 28, 17, 10, 5};
	/** Combiner: chance (percent) to raise a map from tier t to t+1, index t. */
	private static final int[] COMBINE_CHANCE = {0, 80, 74, 68, 61, 55, 49, 43, 36, 30, 27, 25, 22, 20, 17, 15, 12, 10, 7, 5};
	/** Maps of one tier the combiner turns into one map of the next tier for sure. */
	public static final int MERGE = 3;
	/** From this tier the combiner takes Lv 100 Evolution Ether instead of condensed essence. */
	public static final int ETHER_FROM = 10;
	/** Pet evolution material: cores drop from this tier. */
	public static final int CORE_FROM = 11;
	/** Greater imbue scrolls drop from this tier. */
	public static final int GREATER_FROM = 11;
	/**
	 * Extra health of a tier lair's lord on top of the Lv 100 lord: a typical Lv 100 lord has about 1,600 effective health solo at tier 1,
	 * eight seconds for a full-gear player (about 220 DPS, docs/DAMAGE.md); x4 makes tier 1 about 6,300, some 50 s at the 130 DPS of
	 * fair gear, the plan's 45-60 s.
	 */
	public static final float LORD_HEALTH = 4.0F;

	private LairTier() {
	}

	public static int clamp(final int tier) {
		return Math.max(1, Math.min(MAX, tier));
	}

	/** The tier of a lair map, 0 for an untiered one. */
	public static int of(final ItemStack stack) {
		return stack.is(ModItems.LAIR_MAP) ? stack.getOrDefault(ModDataComponents.LAIR_TIER, 0) : 0;
	}

	/** {@code count} lair maps of {@code tier}. */
	public static ItemStack map(final int tier, final int count) {
		ItemStack stack = new ItemStack(ModItems.LAIR_MAP, count);
		stack.set(ModDataComponents.LAIR_TIER, clamp(tier));
		return stack;
	}

	// ------------------------------------------------------------------ strength

	/** Health multiplier on top of Lv 100: 1.6 at tier 1, 14% more each tier (about 19.4 at tier 20). */
	public static float health(final int tier) {
		return (float)(1.6 * Math.pow(1.14, clamp(tier) - 1));
	}

	/** Damage multiplier: 1.25 at tier 1, 5% more each tier (about 3.16 at tier 20); slower than health, players' damage reduction caps at 50%. */
	public static float damage(final int tier) {
		return (float)(1.25 * Math.pow(1.05, clamp(tier) - 1));
	}

	/** Dungeon affixes of a tier: none up to 5, one from 6, two from 11, three from 16. */
	public static int affixCount(final int tier) {
		return tier >= 16 ? 3 : tier >= 11 ? 2 : tier >= 6 ? 1 : 0;
	}

	/** The affixes of one run: Fortified or Tyrannical, then Bolstering or Raging, then Volcanic, each pair picked at random. */
	public static List<DungeonAffix> affixes(final int tier, final RandomSource random) {
		List<DungeonAffix> out = new ArrayList<>();
		int count = affixCount(tier);
		if (count >= 1) {
			out.add(random.nextBoolean() ? DungeonAffix.FORTIFIED : DungeonAffix.TYRANNICAL);
		}
		if (count >= 2) {
			out.add(random.nextBoolean() ? DungeonAffix.BOLSTERING : DungeonAffix.RAGING);
		}
		if (count >= 3) {
			out.add(DungeonAffix.VOLCANIC);
		}
		return out;
	}

	// ------------------------------------------------------------------ the next maps

	/** Chance that one next map is a tier higher: 50% at tier 1, 2.5 points less each tier; none at the top. */
	public static float upChance(final int tier) {
		return tier >= MAX ? 0.0F : 0.5F - 0.025F * (clamp(tier) - 1);
	}

	/** Chance that one next map is a tier lower (from tier 2). */
	public static float downChance(final int tier) {
		return tier >= 2 ? 0.10F : 0.0F;
	}

	/** How many maps a clear gives: 1..5 by {@link #MAP_COUNT_WEIGHTS}. */
	public static int mapCount(final RandomSource random) {
		int total = 0;
		for (int w : MAP_COUNT_WEIGHTS) {
			total += w;
		}
		int roll = random.nextInt(total);
		for (int i = 0; i < MAP_COUNT_WEIGHTS.length; i++) {
			roll -= MAP_COUNT_WEIGHTS[i];
			if (roll < 0) {
				return i + 1;
			}
		}
		return 1;
	}

	/** The tiers of the maps a clear of {@code tier} gives, each rolled on its own. */
	public static List<Integer> nextMaps(final int tier, final RandomSource random) {
		List<Integer> out = new ArrayList<>();
		int count = mapCount(random);
		for (int i = 0; i < count; i++) {
			float roll = random.nextFloat();
			out.add(roll < upChance(tier) ? tier + 1 : roll < upChance(tier) + downChance(tier) ? tier - 1 : tier);
		}
		return out;
	}

	/** The expected number of maps per clear (2.12). */
	public static float expectedMaps() {
		float total = 0.0F;
		float sum = 0.0F;
		for (int i = 0; i < MAP_COUNT_WEIGHTS.length; i++) {
			total += MAP_COUNT_WEIGHTS[i];
			sum += MAP_COUNT_WEIGHTS[i] * (i + 1);
		}
		return sum / total;
	}

	// ------------------------------------------------------------------ the combiner

	/** Percent chance the combiner raises a map of {@code tier} one tier (0 at the top). */
	public static int combineChance(final int tier) {
		return tier >= 1 && tier < MAX ? COMBINE_CHANCE[tier] : 0;
	}

	/** What one combiner attempt from {@code tier} uses up: condensed essence below {@link #ETHER_FROM}, Lv 100 Evolution Ether from it. */
	public static ItemStack combineCost(final int tier) {
		return tier >= ETHER_FROM ? EvolutionEtherItem.of(ItemLevels.MAX_BRACKET, 1) : new ItemStack(ModItems.CONDENSED_ESSENCE);
	}

	// ------------------------------------------------------------------ rewards

	/** How much more of the usual lair treasure a clear pays: +10% a tier (x2.9 at tier 20). */
	public static float rewardScale(final int tier) {
		return 1.0F + 0.1F * (clamp(tier) - 1);
	}

	/** A lair treasure share of a level paid as class (or paragon) experience. */
	public static float expShare(final int tier) {
		return LairExp.CHEST * rewardScale(tier);
	}

	/** Evolution shards per clear: 1 + tier / 4. */
	public static int shards(final int tier) {
		return 1 + clamp(tier) / 4;
	}

	/** Chance of an evolution core from tier {@link #CORE_FROM}: 10%, 2 points more each tier. */
	public static float coreChance(final int tier) {
		return tier >= CORE_FROM ? 0.10F + 0.02F * (tier - 10) : 0.0F;
	}

	/** Chance of an imbue scroll: 10% + 1.5% a tier. */
	public static float scrollChance(final int tier) {
		return 0.10F + 0.015F * clamp(tier);
	}

	/** Chance of a greater imbue scroll from tier {@link #GREATER_FROM}: 2% + 1% a tier past 10. */
	public static float greaterChance(final int tier) {
		return tier >= GREATER_FROM ? 0.02F + 0.01F * (tier - 10) : 0.0F;
	}

	/** A random imbue scroll item (any slot). */
	public static Item scroll(final boolean greater, final RandomSource random) {
		ImbueSlot slot = ImbueSlot.values()[random.nextInt(ImbueSlot.values().length)];
		return (greater ? ModItems.GREATER_IMBUE_SCROLLS : ModItems.IMBUE_SCROLLS).get(slot);
	}
}
