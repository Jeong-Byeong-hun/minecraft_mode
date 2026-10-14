package com.minecraftmode.client.mixin;

import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The trade line picked in a merchant screen (the shop's "sell all of this" button sells that line). */
@Mixin(MerchantScreen.class)
public interface MerchantScreenAccessor {
	@Accessor("shopItem")
	int minecraftMode$shopItem();
}
