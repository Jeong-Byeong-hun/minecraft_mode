package com.minecraftmode.job.gear;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.engrave.EngraveStat;

/** One piece of a class armor set. */
public record ArmorPieceDef(ArmorSetDef set, GearSlot slot) {
	public String id() {
		return this.set.id() + "_" + this.slot.suffix();
	}

	public JobClass job() {
		return this.set.job();
	}

	public int level() {
		return this.set.level();
	}

	public int tier() {
		return this.set.tier();
	}

	public String nameKey() {
		return "item.minecraft_mode." + this.id();
	}

	/** Armor points: 9 + 0.13 per level for a full set, split 15/40/30/15 and scaled by the class style. */
	public int armor() {
		float set = (9.0F + 0.13F * this.level()) * this.set.style().armorFactor();
		float share = switch (this.slot) {
			case HEAD, FEET -> 0.15F;
			case CHEST -> 0.40F;
			default -> 0.30F;
		};
		return Math.max(1, Math.round(set * share));
	}

	public float toughness() {
		return Math.round(this.level() / 40.0F * this.set.style().toughnessFactor() * 10.0F) / 10.0F;
	}

	/** The fixed option every piece of this slot has, growing with the level. */
	public StatLine baseOption() {
		int level = this.level();
		return switch (this.slot) {
			case HEAD -> StatLine.of(EngraveStat.MAX_MANA, 4 + level / 5);
			case CHEST -> StatLine.of(EngraveStat.MAX_HEALTH, 1 + level / 20);
			case LEGS -> StatLine.of(EngraveStat.DAMAGE_REDUCTION, 1 + level / 25);
			default -> StatLine.of(EngraveStat.MOVE_SPEED, 2 + level / 25);
		};
	}

	/** How many random extra options a piece of this level rolls. */
	public int optionCount() {
		return this.level() < 30 ? 1 : this.level() < 60 ? 2 : 3;
	}
}
