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
	ALCHEMIST("alchemist");

	private final String id;

	ShopType(final String id) {
		this.id = id;
	}

	public String id() {
		return this.id;
	}

	public String titleKey() {
		return "container.minecraft_mode.shop." + this.id;
	}
}
