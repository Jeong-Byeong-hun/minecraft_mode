package com.minecraftmode.dungeon;

import java.util.ArrayList;
import java.util.List;

/**
 * Keystone modifiers: from +4 Fortified or Tyrannical, from +7 Bolstering or Raging, from +10 Volcanic. Which of a pair applies
 * changes with the 3-day cycle.
 */
public enum DungeonAffix {
	/** Hall monsters have 25% more health and deal 15% more damage. */
	FORTIFIED("fortified", 4),
	/** The boss has 40% more health and deals 15% more damage. */
	TYRANNICAL("tyrannical", 4),
	/** A hall monster's death strengthens the others nearby. */
	BOLSTERING("bolstering", 7),
	/** Hall monsters enrage below 30% health. */
	RAGING("raging", 7),
	/** Fire erupts under the party from time to time (telegraphed). */
	VOLCANIC("volcanic", 10);

	public static final float FORTIFIED_HEALTH = 1.25F;
	public static final float FORTIFIED_DAMAGE = 1.15F;
	public static final float TYRANNICAL_HEALTH = 1.4F;
	public static final float TYRANNICAL_DAMAGE = 1.15F;
	/** Bolstering: each death within this range adds this much health and damage (multiplier) to the survivors. */
	public static final double BOLSTER_RANGE = 12.0;
	public static final float BOLSTER = 0.15F;
	/** A monster bolsters at most this many times (it compounded without end: a 13-monster hall reached ×5 damage). */
	public static final int BOLSTER_MAX = 5;
	/** Volcanic: seconds between eruptions, radius and the share of health one takes. */
	public static final int VOLCANIC_INTERVAL = 9;
	public static final double VOLCANIC_RADIUS = 2.5;
	public static final float VOLCANIC_PORTION = 0.25F;

	private final String id;
	private final int level;

	DungeonAffix(final String id, final int level) {
		this.id = id;
		this.level = level;
	}

	public String id() {
		return this.id;
	}

	/** Keystone level from which it can appear. */
	public int level() {
		return this.level;
	}

	public String nameKey() {
		return "affix.minecraft_mode.dungeon." + this.id;
	}

	public String descKey() {
		return this.nameKey() + ".desc";
	}

	public static List<DungeonAffix> forRun(final int level, final long cycle) {
		List<DungeonAffix> out = new ArrayList<>();
		if (level >= FORTIFIED.level) {
			out.add(cycle % 2 == 0 ? FORTIFIED : TYRANNICAL);
		}
		if (level >= BOLSTERING.level) {
			out.add(cycle / 2 % 2 == 0 ? BOLSTERING : RAGING);
		}
		if (level >= VOLCANIC.level) {
			out.add(VOLCANIC);
		}
		return out;
	}

	public static DungeonAffix byId(final String id) {
		for (DungeonAffix affix : values()) {
			if (affix.id.equals(id)) {
				return affix;
			}
		}
		return FORTIFIED;
	}
}
