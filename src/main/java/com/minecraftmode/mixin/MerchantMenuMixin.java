package com.minecraftmode.mixin;

import com.minecraftmode.economy.ShopMerchant;
import com.minecraftmode.economy.Wallet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shops and the wallet. Vanilla casts the trader to Entity when shift-clicking a trade result, which
 * would crash for a block-backed merchant, so the sound plays at the shop block instead. And since
 * coins live in the {@link Wallet}, picking a trade that costs coins pays them out of the wallet into
 * the payment slot (enough for several trades; what is left goes back to the wallet when the shop closes).
 */
@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin {
	/** Coins taken out per selection: this many trades' worth. */
	private static final int WALLET_TRADES = 8;

	@Shadow
	@Final
	private Merchant trader;

	@Shadow
	@Final
	private MerchantContainer tradeContainer;

	@Inject(method = "playTradeSound", at = @At("HEAD"), cancellable = true)
	private void minecraftMode$playShopTradeSound(final CallbackInfo ci) {
		if (this.trader instanceof ShopMerchant shop) {
			shop.playTradeSound();
			ci.cancel();
		}
	}

	/**
	 * Closing the shop (also by dying or logging off, when vanilla would drop the payment slots on the floor): coins the wallet
	 * put into the payment slots go straight back into the wallet.
	 */
	@Inject(method = "removed", at = @At("HEAD"))
	private void minecraftMode$coinsBackToWallet(final Player player, final CallbackInfo ci) {
		if (!(this.trader instanceof ShopMerchant) || !(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		for (int slot = 0; slot < 2; slot++) {
			ItemStack stack = this.tradeContainer.getItem(slot);
			int each = stack.isEmpty() ? 0 : Wallet.value(stack.getItem());
			if (each > 0) {
				Wallet.add(serverPlayer, each * stack.getCount());
				this.tradeContainer.setItem(slot, ItemStack.EMPTY);
			}
		}
	}

	@Inject(method = "moveFromInventoryToPaymentSlot", at = @At("TAIL"))
	private void minecraftMode$payFromWallet(final int paymentSlot, final ItemCost cost, final CallbackInfo ci) {
		if (!(this.trader instanceof ShopMerchant) || !(this.trader.getTradingPlayer() instanceof ServerPlayer player)) {
			return;
		}
		Item coin = cost.item().value();
		int each = Wallet.value(coin);
		if (each <= 0) {
			return;
		}
		ItemStack current = this.tradeContainer.getItem(paymentSlot);
		if (!current.isEmpty() && !current.is(coin)) {
			return;
		}
		int have = current.getCount();
		int want = Math.min(coin.getDefaultMaxStackSize(), cost.count() * WALLET_TRADES);
		int add = Math.min(want - have, Wallet.balance(player) / each);
		if (add > 0 && Wallet.take(player, add * each)) {
			this.tradeContainer.setItem(paymentSlot, new ItemStack(coin, have + add));
		}
	}
}
