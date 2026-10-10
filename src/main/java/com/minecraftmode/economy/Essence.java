package com.minecraftmode.economy;

import com.minecraftmode.job.JobProgression;
import com.minecraftmode.registry.ModItems;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Essence as one currency: condensed essence is worth {@link #CONDENSED} essence, and every cost in
 * essence or condensed essence (shops, trials, crafting, the engraving table and the enhancement bench)
 * can be paid with either. A cost in essence breaks condensed essence and gives the change back as
 * essence; a cost in condensed essence is made up from loose essence.
 */
public final class Essence {
	public static final int CONDENSED = 9;

	/** Essence units one {@code item} is worth (1 or {@link #CONDENSED}; 0 for anything else). */
	public static int units(final Item item) {
		if (item == ModItems.ESSENCE) {
			return 1;
		}
		if (item == ModItems.CONDENSED_ESSENCE) {
			return CONDENSED;
		}
		return 0;
	}

	public static boolean is(final Item item) {
		return units(item) > 0;
	}

	/** Essence units in the inventory. */
	public static int total(final Inventory inventory) {
		return JobProgression.count(inventory, ModItems.ESSENCE) + CONDENSED * JobProgression.count(inventory, ModItems.CONDENSED_ESSENCE);
	}

	/** How many {@code item} the inventory can pay: essence counts both kinds, anything else just itself. */
	public static int held(final Inventory inventory, final Item item) {
		int each = units(item);
		return each > 0 ? total(inventory) / each : JobProgression.count(inventory, item);
	}

	/**
	 * Takes {@code count} {@code item} (the caller checked {@link #held}). Essence costs use loose
	 * essence first and break condensed essence for the rest, giving the change back; condensed costs use
	 * condensed essence first and loose essence (9 each) for the rest. Other items are simply removed.
	 */
	public static void take(final Inventory inventory, final Item item, final int count) {
		if (item == ModItems.ESSENCE) {
			pay(inventory, count);
		} else if (item == ModItems.CONDENSED_ESSENCE) {
			int condensed = Math.min(count, JobProgression.count(inventory, ModItems.CONDENSED_ESSENCE));
			JobProgression.removeItems(inventory, ModItems.CONDENSED_ESSENCE, condensed);
			JobProgression.removeItems(inventory, ModItems.ESSENCE, (count - condensed) * CONDENSED);
		} else {
			JobProgression.removeItems(inventory, item, count);
		}
	}

	/** Pays {@code units} essence: loose essence first, then condensed essence with the change given back. */
	public static void pay(final Inventory inventory, int units) {
		if (units <= 0) {
			return;
		}
		int loose = Math.min(units, JobProgression.count(inventory, ModItems.ESSENCE));
		JobProgression.removeItems(inventory, ModItems.ESSENCE, loose);
		units -= loose;
		if (units > 0) {
			int condensed = (units + CONDENSED - 1) / CONDENSED;
			JobProgression.removeItems(inventory, ModItems.CONDENSED_ESSENCE, condensed);
			change(inventory, condensed * CONDENSED - units);
		}
	}

	/** Gives {@code units} essence back as loose essence. */
	public static void change(final Inventory inventory, final int units) {
		give(inventory, ModItems.ESSENCE, units);
	}

	/** Gives {@code count} {@code item} in full stacks (what does not fit drops at the player's feet). */
	public static void give(final Inventory inventory, final Item item, int count) {
		while (count > 0) {
			int stack = Math.min(count, item.getDefaultMaxStackSize());
			inventory.placeItemBackInInventory(new ItemStack(item, stack), Prediction.SERVER_ONLY);
			count -= stack;
		}
	}

	private Essence() {
	}
}
