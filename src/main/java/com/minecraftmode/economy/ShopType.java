package com.minecraftmode.economy;

/**
 * Each shop block sells a different price list (see {@link ShopOffers#trades(ShopType)}).
 * The id is used for the screen title key and for market pressure keys, so do not rename it.
 */
public enum ShopType {
	GENERAL("general"),
	BLACKSMITH("blacksmith"),
	GROCER("grocer"),
	JEWELER("jeweler"),
	/** Class guild: offers depend on the visitor's class and tier. */
	GUILD("guild"),
	/** Potions and the better consumables (see {@code Consumables}). */
	ALCHEMIST("alchemist"),
	/** Crops and seeds, on the residence square: the grocer's lines, under the grocer's market pressure. */
	FARMER("farmer", "grocer"),
	/** Fish, on the residence square: the grocer's lines, under the grocer's market pressure. */
	FISHMONGER("fishmonger", "grocer"),
	/** Building blocks, at the homestead plains and the residence square: fixed prices that buying never raises ({@link #fixedPrices}). */
	BUILDER("builder"),
	/** Sukuna's relic exchange beside the raid gate: his accessories and the tier 4 ticket for his fingers, at fixed prices. */
	CURSED_RELICS("cursed_relics");

	private final String id;
	private final String market;

	ShopType(final String id) {
		this(id, id);
	}

	ShopType(final String id, final String market) {
		this.id = id;
		this.market = market;
	}

	public String id() {
		return this.id;
	}

	/**
	 * The market whose pressure this shop's trades share. The residence stalls repeat the grocer's crop and fish lines, so they count
	 * as the grocer's: with a market of their own a full haul sold at full price twice before prices fell.
	 */
	public String marketId() {
		return this.market;
	}

	/**
	 * No market pressure: builders buy blocks by the thousand, and a price that climbed with every stack would punish building; the
	 * relic exchange is a fixed bad-luck protection.
	 */
	public boolean fixedPrices() {
		return this == BUILDER || this == CURSED_RELICS;
	}

	public String titleKey() {
		return "container.minecraft_mode.shop." + this.id;
	}
}
