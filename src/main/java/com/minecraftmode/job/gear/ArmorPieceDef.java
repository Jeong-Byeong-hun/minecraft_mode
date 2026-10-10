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

	public ClassDefense defense() {
		return ClassDefense.of(this.job());
	}

	/** Armor points: the class's full-set armor ({@link ClassDefense#setArmor}) split 15/40/30/15. */
	public int armor() {
		float share = switch (this.slot) {
			case HEAD, FEET -> 0.15F;
			case CHEST -> 0.40F;
			default -> 0.30F;
		};
		return Math.max(1, Math.round(this.defense().setArmor(this.level()) * share));
	}

	/** A quarter of the full set's toughness. */
	public float toughness() {
		return tenths(this.defense().setToughness(this.level()) / 4.0F);
	}

	/**
	 * What the piece adds to its wearer's defense besides armor: a quarter of the set's protection and magic defense, and of its dodge
	 * chance and mana shield when the class has them (summed by {@code GearStats}).
	 */
	public java.util.List<StatLine> defenseLines() {
		ClassDefense defense = this.defense();
		int level = this.level();
		java.util.List<StatLine> lines = new java.util.ArrayList<>();
		lines.add(StatLine.of(EngraveStat.PROTECTION, tenths(defense.setProtection(level) / 4.0F)));
		lines.add(StatLine.of(EngraveStat.MAGIC_DEFENSE, tenths(defense.setMagicDefense(level) / 4.0F)));
		if (defense.setDodge(level) > 0.0F) {
			lines.add(StatLine.of(EngraveStat.DODGE, tenths(defense.setDodge(level) / 4.0F)));
		}
		if (defense.manaShield() > 0.0F) {
			lines.add(StatLine.of(EngraveStat.MANA_SHIELD, tenths(defense.manaShield() / 4.0F)));
		}
		return lines;
	}

	private static float tenths(final float value) {
		return Math.round(value * 10.0F) / 10.0F;
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
