package com.minecraftmode.mixin;

import com.minecraftmode.economy.ShopMerchant;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla casts the trader to Entity when shift-clicking a trade result, which would crash
 * for a block-backed merchant. Play the sound at the shop block instead.
 */
@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin {
	@Shadow
	@Final
	private Merchant trader;

	@Inject(method = "playTradeSound", at = @At("HEAD"), cancellable = true)
	private void minecraftMode$playShopTradeSound(final CallbackInfo ci) {
		if (this.trader instanceof ShopMerchant shop) {
			shop.playTradeSound();
			ci.cancel();
		}
	}
}
