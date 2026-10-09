package com.minecraftmode.loot;

import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.gear.ArmorOptions;
import com.minecraftmode.job.gear.ArmorPieceDef;
import com.minecraftmode.job.gear.ClassArmor;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.registry.ModDataComponents;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Gear drops of named monsters and bosses. A drop is drop-only gear (any class) from the source's
 * level range; 60% of the time it is the killer's class. Armor options are rolled right away.
 */
public final class GearDrops {
	/** Share of drops that are the killer's own class (when there are any). */
	private static final float OWN_CLASS = 0.6F;

	/** Rolls the drop chance for a named kill (level-based, raised by equipment drop chance). */
	public static ItemStack namedDrop(final ServerPlayer killer, final int lo, final int hi, final RandomSource random) {
		float chance = ItemLevels.dropChance((lo + hi) / 2) * (1.0F + JobWeapons.activeTotals(killer).fraction(EngraveStat.ITEM_FIND));
		if (random.nextFloat() >= chance) {
			return ItemStack.EMPTY;
		}
		return pick(JobProgression.get(killer), lo, hi, random);
	}

	/** A guaranteed piece from [lo, hi], biased toward {@code data}'s class (null = any class). */
	public static ItemStack pick(final @Nullable JobData data, final int lo, final int hi, final RandomSource random) {
		List<ClassGear> pool = GearIndex.dropPool(lo, hi);
		if (pool.isEmpty()) {
			return ItemStack.EMPTY;
		}
		if (data != null && data.hasClass() && random.nextFloat() < OWN_CLASS) {
			List<ClassGear> own = pool.stream().filter(g -> g.job() == data.job()).toList();
			if (!own.isEmpty()) {
				pool = own;
			}
		}
		return create(pool.get(random.nextInt(pool.size())), random);
	}

	/** A fresh stack of {@code gear}; armor gets its options rolled now. */
	public static ItemStack create(final ClassGear gear, final RandomSource random) {
		ItemStack stack = GearIndex.stack(gear);
		ArmorPieceDef piece = ClassArmor.def(stack);
		if (piece != null) {
			stack.set(ModDataComponents.GEAR_ROLLS, ArmorOptions.roll(piece, random));
		}
		return stack;
	}

	/** Ether for a kill in [lo, hi]: 1 + level / 30 pieces, graded by a random bracket of the range. */
	public static ItemStack ether(final int lo, final int hi, final RandomSource random) {
		int level = lo + random.nextInt(Math.max(1, hi - lo + 1));
		return EvolutionEtherItem.of(level, 1 + level / 30);
	}

	private GearDrops() {
	}
}
