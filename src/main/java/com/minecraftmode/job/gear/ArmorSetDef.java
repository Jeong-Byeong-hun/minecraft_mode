package com.minecraftmode.job.gear;

import com.minecraftmode.job.JobClass;
import java.util.List;

/**
 * A class armor set: four pieces of one level with 2-, 3- and 4-piece bonuses. Colors feed the
 * armor artist (item icons and the worn texture).
 *
 * @param primary   main cloth/metal color
 * @param secondary trim and second material
 * @param accent    gems, emblems and glow
 */
public record ArmorSetDef(String id, JobClass job, int level, String en, String ko, int primary, int secondary, int accent, List<SetBonus> bonuses) {
	public ArmorSetDef {
		bonuses = List.copyOf(bonuses);
	}

	/** Lines for {@code pieces} worn pieces. */
	public record SetBonus(int pieces, List<StatLine> lines) {
		public SetBonus {
			lines = List.copyOf(lines);
		}
	}

	public int tier() {
		return ItemLevels.tier(this.level);
	}

	public ArmorStyle style() {
		return ArmorStyle.of(this.job);
	}

	public String nameKey() {
		return "armor_set.minecraft_mode." + this.id;
	}
}
