package com.minecraftmode.job.gear;

import com.minecraftmode.job.JobClass;

/**
 * How well each class's armor protects. A full set of level {@code L} gives
 * <ul>
 * <li>armor {@code (12 + 0.16 L) x armor} and toughness {@code L / 50 x 8 x armor} (split over the four pieces),</li>
 * <li>protection {@code min(55, 0.6 L)% x armor} against all damage (like the Protection enchantment),</li>
 * <li>magic defense {@code min(40, 0.4 L)% x magic} on top against magic attacks ({@code MagicDamage}),</li>
 * <li>a dodge chance {@code dodgeBase + dodgePerLevel x L}% and, for mages, a mana shield (part of every hit paid with MP).</li>
 * </ul>
 * Each piece carries a quarter of the set's protection, magic defense, dodge and shield, so a partial set protects partly.
 * At level 50 every class is tougher than plain diamond, and at 80 the mage matches diamond with Protection IV while the melee
 * classes are about twice as tough (the tables are in {@code docs/GEAR.md}).
 *
 * @param armor         armor, toughness and protection factor
 * @param magic         magic defense factor
 * @param dodgeBase     dodge chance of a full set at level 0 (%)
 * @param dodgePerLevel dodge chance per item level (%)
 * @param manaShield    share of every hit paid with MP by a full set (%)
 */
public record ClassDefense(float armor, float magic, float dodgeBase, float dodgePerLevel, float manaShield) {
	public static final float ARMOR_BASE = 12.0F;
	public static final float ARMOR_PER_LEVEL = 0.16F;
	public static final float TOUGHNESS_PER_50 = 8.0F;
	public static final float PROTECTION_PER_LEVEL = 0.6F;
	public static final float PROTECTION_MAX = 55.0F;
	public static final float MAGIC_PER_LEVEL = 0.4F;
	public static final float MAGIC_MAX = 40.0F;

	public static ClassDefense of(final JobClass job) {
		return switch (job) {
			case WARRIOR -> new ClassDefense(1.15F, 0.6F, 0.0F, 0.0F, 0.0F);
			case PIRATE -> new ClassDefense(1.05F, 0.8F, 0.0F, 0.0F, 0.0F);
			case SHINIGAMI -> new ClassDefense(1.0F, 1.2F, 2.0F, 0.08F, 0.0F);
			case HUNTER -> new ClassDefense(1.0F, 1.0F, 3.0F, 0.1F, 0.0F);
			case ARCHER -> new ClassDefense(0.95F, 1.0F, 3.0F, 0.1F, 0.0F);
			case ROGUE -> new ClassDefense(0.95F, 0.8F, 5.0F, 0.15F, 0.0F);
			case MAGE -> new ClassDefense(0.9F, 1.5F, 0.0F, 0.0F, 20.0F);
			default -> new ClassDefense(1.0F, 1.0F, 0.0F, 0.0F, 0.0F);
		};
	}

	/** Armor points of a full set of this level. */
	public float setArmor(final int level) {
		return (ARMOR_BASE + ARMOR_PER_LEVEL * level) * this.armor;
	}

	/** Toughness of a full set of this level. */
	public float setToughness(final int level) {
		return level / 50.0F * TOUGHNESS_PER_50 * this.armor;
	}

	/** Damage reduction (%) of a full set against everything. */
	public float setProtection(final int level) {
		return Math.min(PROTECTION_MAX, PROTECTION_PER_LEVEL * level) * this.armor;
	}

	/** Extra damage reduction (%) of a full set against magic attacks. */
	public float setMagicDefense(final int level) {
		return Math.min(MAGIC_MAX, MAGIC_PER_LEVEL * level) * this.magic;
	}

	/** Dodge chance (%) of a full set. */
	public float setDodge(final int level) {
		return this.dodgePerLevel <= 0.0F && this.dodgeBase <= 0.0F ? 0.0F : this.dodgeBase + this.dodgePerLevel * level;
	}
}
