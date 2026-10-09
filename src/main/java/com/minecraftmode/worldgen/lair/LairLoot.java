package com.minecraftmode.worldgen.lair;

import com.minecraftmode.consumable.Consumables;
import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.loot.GearDrops;
import com.minecraftmode.loot.GearShop;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * What a lair holds. The goal chest always has coins and at least one item from the lair monster's
 * own level range - a consumable (55%) or a piece of class gear (45%) - with a 60% and then 25% chance
 * of more, plus the monster's Evolution Ether, essence and enhancement stones. Dead-end caches hold
 * small change, a snack and sometimes a stone. Every player rolls their own contents each cycle.
 */
public final class LairLoot {
	/** Chance that a reward item is gear rather than a consumable. */
	public static final float GEAR_SHARE = 0.45F;

	public static List<ItemStack> goal(final NamedDef def, final RandomSource random) {
		List<ItemStack> out = new ArrayList<>();
		int copper = Math.max(6, Math.round(GearShop.bracketPrice(def.hi()) * (0.5F + random.nextFloat() * 0.5F)));
		out.addAll(Coins.asItems(copper));
		out.add(item(def, random));
		if (random.nextFloat() < 0.60F) {
			out.add(item(def, random));
			if (random.nextFloat() < 0.25F) {
				out.add(item(def, random));
			}
		}
		out.add(GearDrops.ether(def.lo(), def.hi(), random));
		out.add(def.lo() >= 40
			? new ItemStack(ModItems.CONDENSED_ESSENCE, 1 + random.nextInt(2))
			: new ItemStack(ModItems.ESSENCE, 3 + random.nextInt(5)));
		out.add(new ItemStack(ModItems.ENHANCEMENT_STONE, 1 + random.nextInt(2)));
		out.removeIf(ItemStack::isEmpty);
		return out;
	}

	/** One reward: gear of the monster's range or a consumable of its level (never empty). */
	public static ItemStack item(final NamedDef def, final RandomSource random) {
		if (random.nextFloat() < GEAR_SHARE) {
			ItemStack gear = GearDrops.pick(null, def.lo(), def.hi(), random);
			if (!gear.isEmpty()) {
				return gear;
			}
		}
		ItemStack supply = Consumables.forLevel(def.hi(), random);
		supply.setCount(Math.min(supply.getMaxStackSize(), 1 + random.nextInt(2)));
		return supply;
	}

	/** A dead-end cache: a little coin, sometimes a consumable, sometimes essence. */
	public static List<ItemStack> cache(final NamedDef def, final RandomSource random) {
		List<ItemStack> out = new ArrayList<>(Coins.asItems(Math.max(2, Math.round(GearShop.bracketPrice(def.lo()) * (0.08F + random.nextFloat() * 0.12F)))));
		if (random.nextFloat() < 0.5F) {
			out.add(Consumables.forLevel(def.lo(), random));
		}
		if (random.nextFloat() < 0.5F) {
			out.add(new ItemStack(ModItems.ESSENCE, 1 + random.nextInt(3)));
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
