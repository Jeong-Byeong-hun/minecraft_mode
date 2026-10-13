package com.minecraftmode.accessory;

import com.minecraftmode.job.gear.ArmorSetDef;
import com.minecraftmode.raid.BossDef;
import java.util.List;

/**
 * The accessories one raid boss drops (0.5.24): five pieces of the boss's level with 2-, 3- and 5-piece bonuses. Only that boss
 * drops them, in its raid, so they are the raid-only reward. The id is the boss id (item ids are {@code <boss>_<slot>}).
 *
 * @param adjEn   item name prefix ("Thornbloom" Necklace)
 * @param metal   band and setting colour of the icons
 * @param gem     stone colour
 */
public record AccessorySet(BossDef boss, String adjEn, String adjKo, int metal, int gem, List<ArmorSetDef.SetBonus> bonuses) {
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
