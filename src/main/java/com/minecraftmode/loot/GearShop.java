package com.minecraftmode.loot;

import com.minecraftmode.economy.ShopOffers.Trade;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.registry.ModItems;
import net.minecraft.world.item.Item;

/**
 * Guild prices of class gear. A bracket's coin price grows by 55% per 10 levels (2 silver at Lv 10,
 * about 11 gold at Lv 100), plus essence (condensed from Lv 40); armor costs 80% of a weapon.
 * Auctions start at 1.5x the bracket's weapon price.
 */
public final class GearShop {
	/** Coin price of a bracket's weapon, in copper. */
	public static int bracketPrice(final int bracket) {
		return Math.round(18.0F * (float)Math.pow(1.55, (ItemLevels.bracket(bracket) - 10) / 10.0));
	}

	/** Copper price of {@code gear} at the guild (before market pressure). */
	public static int price(final ClassGear gear) {
		int base = bracketPrice(gear.bracket());
		return gear.isWeapon() ? base : Math.max(1, Math.round(base * 0.8F));
	}

	/** Auction start price for a drop of {@code gear}: 1.5x its bracket's shop price. */
	public static int auctionStart(final ClassGear gear) {
		return Math.round(bracketPrice(gear.bracket()) * 1.5F);
	}

	/** The guild trade: the price in the largest coin that keeps it close, plus essence. */
	public static Trade trade(final ClassGear gear) {
		int copper = price(gear);
		Item coin;
		int count;
		if (copper >= Coins.GOLD) {
			coin = ModItems.GOLD_COIN;
			count = Math.max(1, Math.round(copper / (float)Coins.GOLD));
		} else if (copper >= Coins.SILVER) {
			coin = ModItems.SILVER_COIN;
			count = Math.max(1, Math.round(copper / (float)Coins.SILVER));
		} else {
			coin = ModItems.COPPER_COIN;
			count = copper;
		}
		Item essence = gear.bracket() >= 40 ? ModItems.CONDENSED_ESSENCE : ModItems.ESSENCE;
		int essenceCount = gear.bracket() >= 40 ? gear.bracket() / 20 : 4 * gear.bracket() / 10;
		return new Trade(coin, count, GearIndex.item(gear), 1, essence, essenceCount);
	}

	private GearShop() {
	}
}
