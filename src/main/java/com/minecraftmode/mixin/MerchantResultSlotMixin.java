package com.minecraftmode.mixin;

import com.minecraftmode.economy.Buyback;
import com.minecraftmode.economy.ShopMerchant;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remembers what a player sells to a shop, before the trade uses up the payment slots ({@link Buyback}). */
@Mixin(MerchantResultSlot.class)
public abstract class MerchantResultSlotMixin {
	@Shadow
	@Final
	private MerchantContainer slots;

	@Shadow
	@Final
	private Merchant merchant;

	@Inject(method = "onTake", at = @At("HEAD"))
	private void minecraftMode$rememberSale(final Player player, final ItemStack carried, final CallbackInfo ci) {
		if (!(this.merchant instanceof ShopMerchant) || !(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		MerchantOffer offer = this.slots.getActiveOffer();
		if (offer != null) {
			Buyback.beforeTrade(serverPlayer, offer, this.slots.getItem(0), this.slots.getItem(1));
		}
	}
}
