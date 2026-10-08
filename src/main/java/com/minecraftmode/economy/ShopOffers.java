package com.minecraftmode.economy;

import com.minecraftmode.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;

/**
 * Fixed price list for the shop block. Prices are in copper (C), silver (S = 9C) and gold (G = 9S) coins.
 */
public final class ShopOffers {
	private static final int UNLIMITED = Integer.MAX_VALUE;

	public static MerchantOffers create() {
		MerchantOffers offers = new MerchantOffers();

		// Sell: player gives goods, receives coins
		offers.add(sell(Items.COBBLESTONE, 32, ModItems.COPPER_COIN, 1));
		offers.add(sell(Items.ROTTEN_FLESH, 16, ModItems.COPPER_COIN, 1));
		offers.add(sell(Items.COAL, 16, ModItems.COPPER_COIN, 2));
		offers.add(sell(ModItems.ALUMINUM_INGOT, 4, ModItems.COPPER_COIN, 3));
		offers.add(sell(Items.IRON_INGOT, 4, ModItems.COPPER_COIN, 3));
		offers.add(sell(ModItems.PLASTIC_SHEET, 8, ModItems.COPPER_COIN, 2));
		offers.add(sell(Items.GOLD_INGOT, 1, ModItems.COPPER_COIN, 2));
		offers.add(sell(Items.EMERALD, 1, ModItems.SILVER_COIN, 1));
		offers.add(sell(ModItems.MYTHRIL_INGOT, 1, ModItems.SILVER_COIN, 1));
		offers.add(sell(Items.DIAMOND, 1, ModItems.SILVER_COIN, 2));

		// Buy: player pays coins, receives goods
		offers.add(buy(ModItems.COPPER_COIN, 2, Items.TORCH, 16));
		offers.add(buy(ModItems.COPPER_COIN, 3, Items.BREAD, 6));
		offers.add(buy(ModItems.COPPER_COIN, 5, Items.COOKED_BEEF, 8));
		offers.add(buy(ModItems.SILVER_COIN, 1, Items.ENDER_PEARL, 2));
		offers.add(buy(ModItems.SILVER_COIN, 2, Items.NAME_TAG, 1));
		offers.add(buy(ModItems.SILVER_COIN, 3, ModItems.MYTHRIL_INGOT, 1));
		offers.add(buy(ModItems.SILVER_COIN, 4, Items.DIAMOND, 1));
		offers.add(buy(ModItems.GOLD_COIN, 1, Items.TOTEM_OF_UNDYING, 1));

		return offers;
	}

	private static MerchantOffer sell(final ItemLike goods, final int goodsCount, final ItemLike coin, final int coinCount) {
		return new MerchantOffer(new ItemCost(goods, goodsCount), new ItemStack(coin, coinCount), UNLIMITED, 0, 0.0F);
	}

	private static MerchantOffer buy(final ItemLike coin, final int price, final ItemLike goods, final int goodsCount) {
		return new MerchantOffer(new ItemCost(coin, price), new ItemStack(goods, goodsCount), UNLIMITED, 0, 0.0F);
	}

	private ShopOffers() {
	}
}
