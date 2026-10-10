package com.minecraftmode.raid;

import org.jspecify.annotations.Nullable;

/**
 * Raid difficulty: boss toughness and damage, entry fee, and what the win pays on top of Normal.
 * Heroic opens after a Normal clear of that boss, Nightmare after a Heroic clear. Ids are saved keys.
 *
 * @param extraLots      gear lots on top of Normal
 * @param enhanceMin     gear from the lots comes enhanced by this much at least
 * @param scrollChance   chance per player of a protection scroll
 * @param exp            class experience of a clear, as a share of a level (see {@code JobProgression.levelExp})
 */
public enum RaidDifficulty {
	NORMAL("normal", 1.0F, 1.0F, 1.0F, 0, 1.0F, 1, 0.0F, 0, 0, 0.6F),
	HEROIC("heroic", 1.8F, 1.3F, 1.5F, 1, 1.5F, 2, 0.15F, 1, 2, 0.9F),
	NIGHTMARE("nightmare", 3.0F, 1.6F, 2.0F, 2, 2.0F, 3, 0.40F, 2, 4, 1.2F);

	private final String id;
	public final float health;
	public final float damage;
	public final float fee;
	public final int extraLots;
	public final float ether;
	public final int stones;
	public final float scrollChance;
	public final int enhanceMin;
	public final int enhanceMax;
	public final float exp;

	RaidDifficulty(final String id, final float health, final float damage, final float fee, final int extraLots, final float ether, final int stones,
		final float scrollChance, final int enhanceMin, final int enhanceMax, final float exp) {
		this.id = id;
		this.health = health;
		this.damage = damage;
		this.fee = fee;
		this.extraLots = extraLots;
		this.ether = ether;
		this.stones = stones;
		this.scrollChance = scrollChance;
		this.enhanceMin = enhanceMin;
		this.enhanceMax = enhanceMax;
		this.exp = exp;
	}

	public String id() {
		return this.id;
	}

	public String nameKey() {
		return "raid.minecraft_mode.difficulty." + this.id;
	}

	/** The difficulty that must be cleared first (null for Normal). */
	public @Nullable RaidDifficulty previous() {
		return this.ordinal() == 0 ? null : values()[this.ordinal() - 1];
	}

	/** Raid modifiers apply from Heroic on. */
	public boolean modified() {
		return this != NORMAL;
	}

	public int fee(final BossDef boss) {
		return Math.max(1, Math.round(boss.fee() * this.fee));
	}

	public static RaidDifficulty byId(final String id) {
		for (RaidDifficulty d : values()) {
			if (d.id.equals(id)) {
				return d;
			}
		}
		return NORMAL;
	}
}
