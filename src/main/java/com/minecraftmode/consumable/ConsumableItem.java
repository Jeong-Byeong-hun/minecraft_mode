package com.minecraftmode.consumable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A consumable from {@link Consumables}: eating or drinking it applies its definition. */
public class ConsumableItem extends Item {
	private final ConsumableDef def;

	public ConsumableItem(final Properties properties, final ConsumableDef def) {
		super(properties);
		this.def = def;
	}

	public ConsumableDef def() {
		return this.def;
	}

	@Override
	public ItemStack finishUsingItem(final ItemStack itemStack, final Level level, final LivingEntity entity) {
		if (!level.isClientSide() && entity instanceof ServerPlayer player) {
			Consumables.apply(player, this.def);
		}
		return super.finishUsingItem(itemStack, level, entity);
	}
}
