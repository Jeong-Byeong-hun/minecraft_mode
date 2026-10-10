package com.minecraftmode.style;

import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.loot.GearShop;
import com.minecraftmode.registry.ModDataComponents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Changing how gear looks without touching what it does (Stylist Celeste): the gear takes the item model of another piece and, for
 * armor, the way that piece is worn ({@code equippable}). Stats, enhancement, engravings, options and the name stay. Armor takes the
 * look of armor for the same slot; held gear (weapons, tools) takes the look of held gear. The piece whose look is taken is used up;
 * the original look comes back for free. {@link ModDataComponents#LOOK} names the item whose look a styled piece wears.
 */
public final class Looks {
	private Looks() {
	}

	/** Gear whose look can change or be taken: class gear, anything with durability, or anything worn. */
	public static boolean styleable(final ItemStack stack) {
		return !stack.isEmpty() && stack.getCount() == 1 && (ClassGear.of(stack) != null || stack.isDamageableItem() || stack.has(DataComponents.EQUIPPABLE));
	}

	/** Whether {@code target} can take the look of {@code donor}: both worn in the same slot, or both held, and the look would change. */
	public static boolean compatible(final ItemStack target, final ItemStack donor) {
		if (!styleable(target) || !styleable(donor) || look(target).equals(look(donor))) {
			return false;
		}
		Equippable worn = target.get(DataComponents.EQUIPPABLE);
		Equippable other = donor.get(DataComponents.EQUIPPABLE);
		if (worn == null || other == null) {
			return worn == null && other == null;
		}
		return worn.slot() == other.slot();
	}

	/** The item whose look {@code stack} wears: its own unless styled. */
	public static Identifier look(final ItemStack stack) {
		Identifier styled = stack.get(ModDataComponents.LOOK);
		return styled != null ? styled : BuiltInRegistries.ITEM.getKey(stack.getItem());
	}

	public static boolean styled(final ItemStack stack) {
		return stack.has(ModDataComponents.LOOK);
	}

	/** Coins for a new look: half the guild weapon price of the gear's bracket (bracket 10 for other gear), at least a silver coin. */
	public static int price(final ItemStack target) {
		ClassGear gear = ClassGear.of(target);
		return Math.max(Coins.SILVER, GearShop.bracketPrice(gear == null ? 10 : gear.bracket()) / 2);
	}

	/** A copy of {@code target} wearing the look of {@code donor} (the caller checked {@link #compatible}). */
	public static ItemStack apply(final ItemStack target, final ItemStack donor) {
		ItemStack out = target.copy();
		out.set(DataComponents.ITEM_MODEL, donor.get(DataComponents.ITEM_MODEL));
		Equippable worn = donor.get(DataComponents.EQUIPPABLE);
		if (worn != null) {
			out.set(DataComponents.EQUIPPABLE, worn);
		}
		Identifier look = look(donor);
		if (look.equals(BuiltInRegistries.ITEM.getKey(out.getItem()))) {
			out.remove(ModDataComponents.LOOK);
		} else {
			out.set(ModDataComponents.LOOK, look);
		}
		return out;
	}

	/** A copy of {@code target} with its own look again. */
	public static ItemStack restore(final ItemStack target) {
		ItemStack out = target.copy();
		DataComponentMap own = out.getPrototype();
		out.set(DataComponents.ITEM_MODEL, own.get(DataComponents.ITEM_MODEL));
		out.set(DataComponents.EQUIPPABLE, own.get(DataComponents.EQUIPPABLE));
		out.remove(ModDataComponents.LOOK);
		return out;
	}
}
