package com.minecraftmode.loot;

import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.job.engrave.Engraving;
import com.minecraftmode.job.engrave.Engravings;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.registry.ModDataComponents;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * The blacksmith's evolution: a piece of class gear becomes one of the next pieces of the same class
 * and kind (any weapon for weapons, the same slot for armor) for {@link #etherCost} Evolution Ether
 * of the target's bracket. Engravings that fit the new piece are kept; armor rolls new options.
 */
public final class GearUpgrades {
	/** Ether per target bracket (Lv 10, 20, ..., 100): a few early on, climbing in steps. */
	private static final int[] ETHER_COSTS = {3, 5, 8, 12, 16, 20, 25, 30, 40, 50};

	/** Evolution Ether of the target's bracket needed to evolve into {@code target}. */
	public static int etherCost(final ClassGear target) {
		return etherCost(target.bracket());
	}

	public static int etherCost(final int bracket) {
		int index = (ItemLevels.bracket(bracket) - ItemLevels.MIN_BRACKET) / 10;
		return ETHER_COSTS[Math.min(index, ETHER_COSTS.length - 1)];
	}

	/**
	 * Coins for evolving {@code from} into {@code target}: half the target bracket's guild price, plus re-tempering of the
	 * enhancement that is carried over - half of what those +N attempts would have cost at the target bracket instead of
	 * the old one, so a +15 made cheaply on level-10 gear cannot skip the coin sink of higher brackets.
	 */
	public static int coinCost(final ClassGear target, final ItemStack from) {
		int base = GearShop.bracketPrice(target.bracket()) / 2;
		ClassGear source = ClassGear.of(from);
		int level = Enhancement.of(from).level();
		if (source != null && level > 0) {
			float attempts = 0.1F * level + 0.025F * level * (level - 1); // sum of Enhancement.coins' price factors for +0..+N
			base += Math.round(Math.max(0, GearShop.bracketPrice(target.bracket()) - GearShop.bracketPrice(source.bracket())) * attempts * 0.5F);
		}
		return Math.max(1, base);
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
