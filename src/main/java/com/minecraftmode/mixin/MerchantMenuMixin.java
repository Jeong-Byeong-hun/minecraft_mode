package com.minecraftmode.mixin;

import com.minecraftmode.bag.Bags;
import com.minecraftmode.economy.Essence;
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
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shops and the wallet. Vanilla casts the trader to Entity when shift-clicking a trade result, which
 * would crash for a block-backed merchant, so the sound plays at the shop block instead. And since
 * coins live in the {@link Wallet}, picking a trade that costs coins pays them out of the wallet into
 * the payment slot (enough for several trades; what is left goes back to the wallet when the shop closes).
 * Essence costs take either kind of essence, with change ({@link Essence}). Items sold to a shop come out of the bags too.
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
		this.minecraftMode$paymentBackToBags(serverPlayer);
	}

	/**
	 * Picking another trade: vanilla moves what is left in the payment slots into the inventory first. Plain stackable items the bags
	 * take go back into them instead (as a pickup would), so browsing the sell trades does not move bag items into the inventory;
	 * gear goes back to the inventory as before.
	 */
	@Inject(method = "tryMoveItems", at = @At("HEAD"))
	private void minecraftMode$paymentBackToBagsOnSwitch(final int selectedOffer, final CallbackInfo ci) {
		if (this.trader instanceof ShopMerchant && this.trader.getTradingPlayer() instanceof ServerPlayer player) {
			this.minecraftMode$paymentBackToBags(player);
		}
	}

	@Unique
	private void minecraftMode$paymentBackToBags(final ServerPlayer player) {
		for (int slot = 0; slot < 2; slot++) {
			ItemStack stack = this.tradeContainer.getItem(slot);
			boolean plain = stack.getMaxStackSize() > 1 && ItemStack.isSameItemSameComponents(stack, new ItemStack(stack.getItem()));
			if (plain && Wallet.value(stack.getItem()) <= 0 && Bags.absorb(player, stack) > 0) {
				this.tradeContainer.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
			}
		}
	}

	@Inject(method = "moveFromInventoryToPaymentSlot", at = @At("TAIL"))
	private void minecraftMode$payFromWallet(final int paymentSlot, final ItemCost cost, final CallbackInfo ci) {
		if (!(this.trader instanceof ShopMerchant) || !(this.trader.getTradingPlayer() instanceof ServerPlayer player)) {
			return;
		}
		Item coin = cost.item().value();
		if (Essence.is(coin)) {
			this.minecraftMode$essenceChange(player, paymentSlot, coin, this.minecraftMode$need(cost));
			return;
		}
		int each = Wallet.value(coin);
		if (each <= 0) {
			this.minecraftMode$sellFromBags(player, paymentSlot, cost);
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

	/** What the trade asks now: market pressure can raise the base count (6 essence -> 7 and up, 32 sticks -> 40). */
	@Unique
	private int minecraftMode$need(final ItemCost cost) {
		int need = cost.count();
		for (MerchantOffer offer : this.trader.getOffers()) {
			if (offer.getItemCostA() == cost) {
				need = offer.getCostA().getCount();
			} else if (offer.getItemCostB().orElse(null) == cost) {
				need = offer.getCostB().getCount();
			}
		}
		return Math.max(1, need);
	}

	/**
	 * Selling to a shop: vanilla only filled the payment slot from the inventory, so the bags top it up with the same plain item
	 * (stackable, no enchantments, damage or name: gear never leaves a bag this way), as many as make whole trades up to a full
	 * stack. Shift-clicking the result then sells them like inventory items.
	 */
	@Unique
	private void minecraftMode$sellFromBags(final ServerPlayer player, final int paymentSlot, final ItemCost cost) {
		ItemStack plain = new ItemStack(cost.item());
		if (plain.getMaxStackSize() <= 1 || !cost.test(plain)) {
			return;
		}
		ItemStack current = this.tradeContainer.getItem(paymentSlot);
		if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, plain)) {
			return;
		}
		int need = this.minecraftMode$need(cost);
		int have = current.getCount();
		int total = Math.min(plain.getMaxStackSize(), have + Bags.countInBags(player, plain));
		int take = total - total % need - have;
		if (take <= 0) {
			return;
		}
		int taken = Bags.takeFromBags(player, plain, take);
		if (taken > 0) {
			this.tradeContainer.setItem(paymentSlot, plain.copyWithCount(have + taken));
		}
	}

	/**
	 * Vanilla only moved essence of the exact kind; when that is short of one trade, the other kind makes up the rest
	 * ({@link Essence#take}: condensed essence is broken with the change going back to the inventory, or loose essence
	 * is condensed).
	 */
	@Unique
	private void minecraftMode$essenceChange(final ServerPlayer player, final int paymentSlot, final Item essence, final int need) {
		ItemStack current = this.tradeContainer.getItem(paymentSlot);
		if (!current.isEmpty() && !current.is(essence)) {
			return;
		}
		int have = current.getCount();
		int missing = need - have;
		if (missing <= 0 || Essence.held(player.getInventory(), essence) < missing) {
			return;
		}
		Essence.take(player.getInventory(), essence, missing);
		this.tradeContainer.setItem(paymentSlot, new ItemStack(essence, need));
	}
}
