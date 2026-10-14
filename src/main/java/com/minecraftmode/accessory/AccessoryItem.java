package com.minecraftmode.accessory;

import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** An accessory (see {@link Accessories}); worn from the accessory window, never as armor. */
public class AccessoryItem extends Item {
	private final AccessoryDef def;

	public AccessoryItem(final AccessoryDef def, final Properties properties) {
		super(properties);
		this.def = def;
	}

	public AccessoryDef def() {
		return this.def;
	}

	@Override
	public Component getName(final ItemStack itemStack) {
		return AccessoryEnhance.decorate(Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.LIGHT_PURPLE), itemStack);
	}

	/** +{@value AccessoryEnhance#MAX} pieces shine. */
	@Override
	public boolean isFoil(final ItemStack itemStack) {
		return super.isFoil(itemStack) || Enhancement.level(itemStack) >= AccessoryEnhance.MAX;
	}

	/** Pieces made without options (creative, commands) roll a Normal piece's one option once they are carried. */
	@Override
	public void inventoryTick(final ItemStack itemStack, final ServerLevel level, final Entity owner, final @Nullable EquipmentSlot slot) {
		super.inventoryTick(itemStack, level, owner, slot);
		if (!itemStack.has(ModDataComponents.GEAR_ROLLS)) {
			itemStack.set(ModDataComponents.GEAR_ROLLS, Accessories.defaultRolls(this.def, level.getRandom()));
		}
	}
}
