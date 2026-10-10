package com.minecraftmode.mixin;

import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The merchant behind an open trade screen (shop buybacks check it is a shop). */
@Mixin(MerchantMenu.class)
public interface MerchantMenuAccessor {
	@Accessor("trader")
	Merchant minecraftMode$trader();
}
