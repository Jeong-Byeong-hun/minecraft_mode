package com.minecraftmode.loot;

import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.job.engrave.Engraving;
import com.minecraftmode.job.engrave.Engravings;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.registry.ModDataComponents;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * The blacksmith's evolution: a piece of class gear becomes one of the next pieces of the same class
 * and kind (any weapon for weapons, the same slot for armor) for {@link #ETHER_COST} Evolution Ether
 * of the target's bracket. Engravings that fit the new piece are kept; armor rolls new options.
 */
public final class GearUpgrades {
	public static final int ETHER_COST = 50;

	/** Coins for evolving into {@code target}: half its bracket's guild price. */
	public static int coinCost(final ClassGear target) {
		return Math.max(1, GearShop.bracketPrice(target.bracket()) / 2);
	}

	/** Coins to roll an armor piece's extra options again: half its bracket's guild price. */
	public static int rerollCost(final ClassGear gear) {
		return Math.max(1, GearShop.bracketPrice(gear.bracket()) / 2);
	}
	/** Targets may be up to this many levels above the first piece above the current one. */
	private static final int WINDOW = 12;
	private static final int MAX_TARGETS = 4;

	public static List<ClassGear> targets(final ClassGear from) {
		List<ClassGear> above = GearIndex.list().stream()
			.filter(g -> g.job() == from.job() && g.isWeapon() == from.isWeapon() && (from.isWeapon() || g.slot() == from.slot()) && g.level() > from.level())
			.sorted(Comparator.comparingInt(ClassGear::level))
			.toList();
		if (above.isEmpty()) {
			return List.of();
		}
		int limit = above.getFirst().level() + WINDOW;
		List<ClassGear> targets = new ArrayList<>();
		for (ClassGear g : above) {
			if (g.level() <= limit && targets.size() < MAX_TARGETS) {
				targets.add(g);
			}
		}
		return targets;
	}

	public static int grade(final ClassGear target) {
		return target.bracket();
	}

	/** The evolved stack (engravings that fit and the enhancement carried over). */
	public static ItemStack evolve(final ItemStack from, final ClassGear target, final RandomSource random) {
		ItemStack result = GearDrops.create(target, random);
		Engravings engravings = from.getOrDefault(ModDataComponents.ENGRAVINGS, Engravings.EMPTY);
		List<String> kept = new ArrayList<>();
		for (Engraving e : engravings.resolved()) {
			boolean fits = target.isWeapon() ? e.fits(target.job(), target.archetype()) : e.fits(target.job(), target.slot());
			if (fits && kept.size() < target.maxLines()) {
				kept.add(e.id());
			}
		}
		if (!kept.isEmpty()) {
			result.set(ModDataComponents.ENGRAVINGS, new Engravings(kept, random.nextInt()));
		}
		Enhancement enhancement = from.get(ModDataComponents.ENHANCEMENT);
		if (enhancement != null) {
			result.set(ModDataComponents.ENHANCEMENT, enhancement);
		}
		return result;
	}

	private GearUpgrades() {
	}
}
