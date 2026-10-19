package com.minecraftmode.worldgen.lair;

import com.minecraftmode.consumable.Consumables;
import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.loot.GearDrops;
import com.minecraftmode.loot.GearShop;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * What a lair holds. The goal chest always has coins and at least one item of the lair's level range - a consumable (55%) or a piece
 * of class gear (45%) - with a 60% and then 25% chance of more, plus Evolution Ether, essence and enhancement stones. Dead-end caches
 * hold small change, a snack and sometimes a stone. Every player rolls their own contents each cycle.
 * <p>
 * The range follows what was fought (0.5.36): chests are graded at the lower of the opener's level and the level they beat this cycle's
 * lord at ({@code LairChestBlockEntity#gradeLevel}); a lord rises to the strongest player near it, so a strong player who fights it at
 * their level is paid in their own bracket ({@link #range}), never below the lair's own range.
 */
public final class LairLoot {
	/** Chance that a reward item is gear rather than a consumable. */
	public static final float GEAR_SHARE = 0.45F;

	/** A level range rewards are graded by. */
	public record Range(int lo, int hi) {
	}

	/**
	 * The range a lair of {@code def} pays a player of class level {@code level} in: the monster's own range, or the player's 10-level bracket
	 * (up to the top bracket 90-100) when the player is above it.
	 */
	public static Range range(final NamedDef def, final int level) {
		if (level <= def.hi()) {
			return new Range(def.lo(), def.hi());
		}
		int lo = Math.max(def.lo(), Math.min(ItemLevels.bracket(level), ItemLevels.MAX_BRACKET - 10));
		return new Range(lo, Math.min(ItemLevels.MAX_BRACKET, lo + 10));
	}

	private static Range range(final NamedDef def, final @Nullable JobData data) {
		return range(def, data == null ? 0 : data.level());
	}

	/** {@code data}: the opener's class, so gear leans toward it like other drops do ({@link GearDrops#pick}) and the range follows their level. */
	public static List<ItemStack> goal(final NamedDef def, final @Nullable JobData data, final RandomSource random) {
		return goal(def, data, data == null ? 0 : data.level(), random);
	}

	/** A goal chest graded at {@code grade} ({@link #range}): the chest passes the lower of the opener's level and the level they beat the lord at. */
	public static List<ItemStack> goal(final NamedDef def, final @Nullable JobData data, final int grade, final RandomSource random) {
		Range r = range(def, grade);
		List<ItemStack> out = new ArrayList<>();
		int copper = Math.max(6, Math.round(GearShop.bracketPrice(r.hi()) * (0.5F + random.nextFloat() * 0.5F)));
		out.addAll(Coins.asItems(copper));
		out.add(item(r, data, random));
		if (random.nextFloat() < 0.60F) {
			out.add(item(r, data, random));
			if (random.nextFloat() < 0.25F) {
				out.add(item(r, data, random));
			}
		}
		out.add(GearDrops.ether(r.lo(), r.hi(), random));
		out.add(r.lo() >= 40
			? new ItemStack(ModItems.CONDENSED_ESSENCE, 2 + random.nextInt(3))
			: new ItemStack(ModItems.ESSENCE, 6 + random.nextInt(9)));
		out.add(new ItemStack(ModItems.ENHANCEMENT_STONE, 1 + random.nextInt(2)));
		out.removeIf(ItemStack::isEmpty);
		return out;
	}

	/** One reward: gear of the monster's range or a consumable of its level (never empty). */
	public static ItemStack item(final NamedDef def, final @Nullable JobData data, final RandomSource random) {
		return item(range(def, data), data, random);
	}

	private static ItemStack item(final Range r, final @Nullable JobData data, final RandomSource random) {
		if (random.nextFloat() < GEAR_SHARE) {
			ItemStack gear = GearDrops.pick(data, r.lo(), r.hi(), random);
			if (!gear.isEmpty()) {
				return gear;
			}
		}
		ItemStack supply = Consumables.forLevel(r.hi(), random);
		supply.setCount(Math.min(supply.getMaxStackSize(), 1 + random.nextInt(2)));
		return supply;
	}

	/** A dead-end cache: a little coin, sometimes a consumable, sometimes essence, graded at {@code grade}: the lair's own range unless the opener beat this cycle's lord higher. */
	public static List<ItemStack> cache(final NamedDef def, final @Nullable JobData data, final int grade, final RandomSource random) {
		Range r = range(def, grade);
		List<ItemStack> out = new ArrayList<>(Coins.asItems(Math.max(2, Math.round(GearShop.bracketPrice(r.lo()) * (0.08F + random.nextFloat() * 0.12F)))));
		if (random.nextFloat() < 0.5F) {
			out.add(Consumables.forLevel(r.lo(), random));
		}
		if (random.nextFloat() < 0.5F) {
			out.add(new ItemStack(ModItems.ESSENCE, 2 + random.nextInt(5)));
		}
		if (random.nextFloat() < 0.25F) {
			out.add(new ItemStack(ModItems.ENHANCEMENT_STONE));
		}
		out.removeIf(ItemStack::isEmpty);
		return out;
	}

	private LairLoot() {
	}
}
