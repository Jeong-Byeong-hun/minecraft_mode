package com.minecraftmode.accessory;

import com.minecraftmode.job.gear.ArmorSetDef;
import com.minecraftmode.raid.BossDef;
import java.util.List;

/**
 * One raid boss's accessories (0.5.24): five pieces of the boss's level with 2-, 3- and 5-piece bonuses. The id is the boss id (item
 * ids are {@code <boss>_<slot>}). Sets come in three tiers of three bosses each: on Normal a boss drops its own set at its tier's
 * chance, from Heroic on every boss drops tier 3 pieces only ({@link Accessories#dropChance}).
 *
 * @param tier    1 (Verdessa, Arachne, Gorvath), 2 (Nefrekhet, Kraken, Ignis) or 3 (Raijin, Malachar, Aethryx)
 * @param adjEn   item name prefix ("Thornbloom" Necklace)
 * @param metal   band and setting colour of the icons
 * @param gem     stone colour
 */
public record AccessorySet(BossDef boss, int tier, String adjEn, String adjKo, int metal, int gem, List<ArmorSetDef.SetBonus> bonuses) {
	public AccessorySet {
		bonuses = List.copyOf(bonuses);
	}

	public String id() {
		return this.boss.id();
	}

	/** Level needed to wear the set's pieces: the boss's level (the level needed to enter its raid). */
	public int level() {
		return this.boss.lo();
	}

	public String nameKey() {
		return "accessory_set.minecraft_mode." + this.id();
	}
}
