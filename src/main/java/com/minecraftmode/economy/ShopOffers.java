package com.minecraftmode.economy;

import com.minecraftmode.registry.ModItems;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;

/**
 * Base price lists. Prices are in copper (C), silver (S = 9C) and gold (G = 9S) coins; the market
 * raises them with pressure (see {@link MarketData}). Built on demand so item fields are initialized.
 */
public final class ShopOffers {
	private static final int UNLIMITED = Integer.MAX_VALUE;

	/** One line of a price list: pay {@code costCount} x {@code cost}, receive {@code resultCount} x {@code result}. */
	public record Trade(ItemLike cost, int costCount, ItemLike result, int resultCount) {
		/** Stable id used for market pressure. */
		public String key() {
			return BuiltInRegistries.ITEM.getKey(this.cost.asItem()) + "->" + BuiltInRegistries.ITEM.getKey(this.result.asItem());
		}

		public MerchantOffer toOffer() {
			return new MerchantOffer(new ItemCost(this.cost, this.costCount), new ItemStack(this.result, this.resultCount), UNLIMITED, 0, 0.0F);
		}
	}

	public static List<Trade> trades(final ShopType type) {
		return switch (type) {
			case GENERAL -> List.of(
				sell(Items.COBBLESTONE, 32, ModItems.COPPER_COIN, 1),
				sell(Items.ROTTEN_FLESH, 16, ModItems.COPPER_COIN, 1),
				sell(Items.COAL, 16, ModItems.COPPER_COIN, 2),
				sell(ModItems.ALUMINUM_INGOT, 4, ModItems.COPPER_COIN, 3),
				sell(Items.IRON_INGOT, 4, ModItems.COPPER_COIN, 3),
				sell(ModItems.PLASTIC_SHEET, 8, ModItems.COPPER_COIN, 2),
				sell(Items.GOLD_INGOT, 1, ModItems.COPPER_COIN, 2),
				sell(Items.EMERALD, 1, ModItems.SILVER_COIN, 1),
				sell(ModItems.MYTHRIL_INGOT, 1, ModItems.SILVER_COIN, 1),
				sell(Items.DIAMOND, 1, ModItems.SILVER_COIN, 2),
				buy(ModItems.COPPER_COIN, 2, Items.TORCH, 16),
				buy(ModItems.COPPER_COIN, 3, Items.BREAD, 6),
				buy(ModItems.COPPER_COIN, 5, Items.COOKED_BEEF, 8),
				buy(ModItems.SILVER_COIN, 1, Items.ENDER_PEARL, 2),
				buy(ModItems.SILVER_COIN, 2, Items.NAME_TAG, 1),
				buy(ModItems.SILVER_COIN, 3, ModItems.MYTHRIL_INGOT, 1),
				buy(ModItems.SILVER_COIN, 4, Items.DIAMOND, 1),
				buy(ModItems.GOLD_COIN, 1, Items.TOTEM_OF_UNDYING, 1)
			);
			case BLACKSMITH -> List.of(
				sell(Items.IRON_INGOT, 4, ModItems.COPPER_COIN, 3),
				sell(ModItems.ALUMINUM_INGOT, 4, ModItems.COPPER_COIN, 3),
				sell(Items.COAL, 16, ModItems.COPPER_COIN, 2),
				sell(ModItems.RAW_MYTHRIL, 2, ModItems.SILVER_COIN, 1),
				sell(ModItems.MYTHRIL_INGOT, 1, ModItems.SILVER_COIN, 1),
				buy(ModItems.COPPER_COIN, 3, Items.ARROW, 16),
				buy(ModItems.COPPER_COIN, 6, Items.SHIELD, 1),
				buy(ModItems.COPPER_COIN, 8, Items.IRON_PICKAXE, 1),
				buy(ModItems.COPPER_COIN, 8, Items.IRON_SWORD, 1),
				buy(ModItems.SILVER_COIN, 2, Items.IRON_CHESTPLATE, 1),
				buy(ModItems.SILVER_COIN, 4, ModItems.MYTHRIL_PICKAXE, 1),
				buy(ModItems.SILVER_COIN, 4, ModItems.MYTHRIL_SWORD, 1),
				buy(ModItems.SILVER_COIN, 7, ModItems.MYTHRIL_CHESTPLATE, 1)
			);
			case GROCER -> List.of(
				sell(Items.WHEAT, 20, ModItems.COPPER_COIN, 1),
				sell(Items.CARROT, 24, ModItems.COPPER_COIN, 1),
				sell(Items.POTATO, 24, ModItems.COPPER_COIN, 1),
				sell(Items.BEETROOT, 20, ModItems.COPPER_COIN, 1),
				sell(Items.MELON_SLICE, 32, ModItems.COPPER_COIN, 1),
				sell(Items.PUMPKIN, 6, ModItems.COPPER_COIN, 1),
				sell(Items.SUGAR_CANE, 24, ModItems.COPPER_COIN, 1),
				buy(ModItems.COPPER_COIN, 3, Items.BREAD, 6),
				buy(ModItems.COPPER_COIN, 3, Items.BAKED_POTATO, 8),
				buy(ModItems.COPPER_COIN, 5, Items.COOKED_BEEF, 8),
				buy(ModItems.COPPER_COIN, 4, Items.CAKE, 1),
				buy(ModItems.SILVER_COIN, 1, Items.GOLDEN_APPLE, 1),
				buy(ModItems.SILVER_COIN, 2, Items.GOLDEN_CARROT, 4)
			);
			case JEWELER -> List.of(
				sell(Items.GOLD_INGOT, 1, ModItems.COPPER_COIN, 2),
				sell(Items.LAPIS_LAZULI, 16, ModItems.COPPER_COIN, 2),
				sell(Items.QUARTZ, 16, ModItems.COPPER_COIN, 2),
				sell(Items.REDSTONE, 32, ModItems.COPPER_COIN, 2),
				sell(Items.AMETHYST_SHARD, 8, ModItems.COPPER_COIN, 1),
				sell(Items.EMERALD, 1, ModItems.SILVER_COIN, 1),
				sell(Items.DIAMOND, 1, ModItems.SILVER_COIN, 2),
				buy(ModItems.SILVER_COIN, 2, Items.EMERALD, 1),
				buy(ModItems.SILVER_COIN, 2, Items.GOLD_INGOT, 8),
				buy(ModItems.SILVER_COIN, 3, ModItems.MYTHRIL_INGOT, 1),
				buy(ModItems.SILVER_COIN, 4, Items.DIAMOND, 1),
				buy(ModItems.GOLD_COIN, 1, Items.TOTEM_OF_UNDYING, 1),
				buy(ModItems.GOLD_COIN, 2, Items.NETHERITE_SCRAP, 1)
			);
		};
	}

	private static Trade sell(final ItemLike goods, final int goodsCount, final ItemLike coin, final int coinCount) {
		return new Trade(goods, goodsCount, coin, coinCount);
	}

	private static Trade buy(final ItemLike coin, final int price, final ItemLike goods, final int goodsCount) {
		return new Trade(coin, price, goods, goodsCount);
	}

	private ShopOffers() {
	}
}
