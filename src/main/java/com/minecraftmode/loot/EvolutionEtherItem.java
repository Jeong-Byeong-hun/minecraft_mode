package com.minecraftmode.loot;

import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.job.weapon.JobWeaponItem;
import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Evolution Ether: dropped by every named monster and boss kill, graded by the level bracket it came
 * from (the grade is a component, so different grades never stack). 50 of a bracket's grade let the
 * city blacksmith turn a piece of gear into the next one of that bracket.
 */
public class EvolutionEtherItem extends Item {
	public EvolutionEtherItem(final Properties properties) {
		super(properties);
	}

	public static int grade(final ItemStack stack) {
		return stack.getOrDefault(ModDataComponents.ETHER_GRADE, ItemLevels.MIN_BRACKET);
	}

	public static ItemStack of(final int grade, final int count) {
		ItemStack stack = new ItemStack(ModItems.EVOLUTION_ETHER, count);
		stack.set(ModDataComponents.ETHER_GRADE, ItemLevels.bracket(grade));
		return stack;
	}

	@Override
	public Component getName(final ItemStack itemStack) {
		int grade = grade(itemStack);
		return Component.translatable("item.minecraft_mode.evolution_ether.graded", grade).withColor(JobWeaponItem.tierColor(ItemLevels.tier(grade)));
	}

	@Override
	public boolean isFoil(final ItemStack itemStack) {
		return true;
	}
}
