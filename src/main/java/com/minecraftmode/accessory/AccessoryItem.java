package com.minecraftmode.accessory;

import com.minecraftmode.registry.ModDataComponents;
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

	/** Pieces made without options (creative, commands) roll a Normal piece's one option once they are carried. */
	@Override
	public void inventoryTick(final ItemStack itemStack, final ServerLevel level, final Entity owner, final @Nullable EquipmentSlot slot) {
		super.inventoryTick(itemStack, level, owner, slot);
		if (!itemStack.has(ModDataComponents.GEAR_ROLLS)) {
			itemStack.set(ModDataComponents.GEAR_ROLLS, Accessories.defaultRolls(this.def, level.getRandom()));
		}
	}
}
