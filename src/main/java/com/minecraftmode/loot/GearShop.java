package com.minecraftmode.loot;

import com.minecraftmode.economy.ShopOffers.Trade;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.registry.ModItems;
import net.minecraft.world.item.Item;

/**
 * Guild prices of class gear. A bracket's coin price grows by 55% per 10 levels (2 silver at Lv 10,
 * about 11 gold at Lv 100), plus essence (condensed from Lv 40); armor costs 80% of a weapon.
 * Auctions start at 1.5x the bracket's weapon price. The guild buys any class gear back for
 * {@link #BUYBACK_SHARE} of its price.
 */
public final class GearShop {
	/** Share of the guild price paid for class gear sold back (enhancement, engravings and options are not paid for). */
	public static final float BUYBACK_SHARE = 0.3F;

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

	/** Copper the guild pays for one {@code gear} sold back. */
	public static int buybackPrice(final ClassGear gear) {
		return Math.max(1, Math.round(price(gear) * BUYBACK_SHARE));
	}

	/** The guild trade: the price in the largest coin that keeps it close, plus essence. */
	public static Trade trade(final ClassGear gear) {
		int copper = price(gear);
		Item coin = coin(copper);
		Item essence = gear.bracket() >= 40 ? ModItems.CONDENSED_ESSENCE : ModItems.ESSENCE;
		int essenceCount = gear.bracket() >= 40 ? gear.bracket() / 20 : 4 * gear.bracket() / 10;
		return new Trade(coin, coins(copper, coin), GearIndex.item(gear), 1, essence, essenceCount);
	}

	/** The guild buying one {@code gear} back, paid in the largest coin that keeps the price close. */
	public static Trade buyback(final ClassGear gear) {
		int copper = buybackPrice(gear);
		Item coin = coin(copper);
		return new Trade(GearIndex.item(gear), 1, coin, coins(copper, coin));
	}

	private static Item coin(final int copper) {
		return copper >= Coins.GOLD ? ModItems.GOLD_COIN : copper >= Coins.SILVER ? ModItems.SILVER_COIN : ModItems.COPPER_COIN;
	}

	private static int coins(final int copper, final Item coin) {
		int each = coin == ModItems.GOLD_COIN ? Coins.GOLD : coin == ModItems.SILVER_COIN ? Coins.SILVER : 1;
		return Math.max(1, Math.round(copper / (float)each));
	}

	private GearShop() {
	}
}
