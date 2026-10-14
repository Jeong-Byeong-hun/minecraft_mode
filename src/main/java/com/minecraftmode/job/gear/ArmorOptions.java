package com.minecraftmode.job.gear;

import static com.minecraftmode.job.engrave.EngraveStat.*;

import com.minecraftmode.job.engrave.EngraveStat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.util.RandomSource;

/**
 * Random extra options of class armor. Each slot has its own pool; a piece rolls
 * {@link ArmorPieceDef#optionCount()} different stats, each at 60-100% of the slot's maximum for
 * the piece's level (the maximum grows from 35% at Lv 0 to 100% at Lv 100).
 */
public final class ArmorOptions {
	/** Smallest pool a slot may have (0.5.24: at least ten, so rerolls stay varied). */
	public static final int MIN_POOL = 10;

	/** Pool per slot: stat -> value at Lv 100. */
	private static final Map<GearSlot, Map<EngraveStat, Float>> POOLS = Map.of(
		GearSlot.HEAD, optionPool(COOLDOWN_FLAT, 2.0F, SKILL_DAMAGE, 12.0F, CRIT_CHANCE, 8.0F, MAX_MANA, 30.0F, MANA_COST, 10.0F, MANA_REGEN, 3.0F, EXP_BONUS, 15.0F,
			MAGIC_DEFENSE, 4.0F, BOSS_DAMAGE, 8.0F, ECHO, 3.0F, KILL_MANA, 4.0F, HEAL_ON_SKILL, 3.0F),
		GearSlot.CHEST, optionPool(MAX_HEALTH, 8.0F, DAMAGE_REDUCTION, 8.0F, LIFESTEAL, 4.0F, THORNS, 25.0F, HEALTH_REGEN, 3.0F, BASIC_DAMAGE, 12.0F, SHOT_DAMAGE, 12.0F,
			PROTECTION, 4.0F, LAST_STAND, 12.0F, DAMAGE, 4.0F, MANA_ON_HIT, 1.0F),
		GearSlot.LEGS, optionPool(DODGE, 5.0F, ATTACK_SPEED, 10.0F, CRIT_DAMAGE, 25.0F, SKILL_AREA, 12.0F, EXECUTE, 20.0F, KNOCKBACK_RES, 30.0F, ARMOR, 3.0F,
			FIRST_STRIKE, 15.0F, BACKSTAB, 15.0F, DOUBLE_STRIKE, 4.0F, MAX_HEALTH, 5.0F),
		GearSlot.FEET, optionPool(MOVE_SPEED, 8.0F, DODGE, 5.0F, ITEM_FIND, 12.0F, GOLD_FIND, 6.0F, KILL_HEAL, 3.0F, SPEED_ON_KILL, 3.0F, DRAW_SPEED, 15.0F,
			KNOCKBACK_RES, 25.0F, KILL_MANA, 4.0F, STEALTH_ON_KILL, 2.0F, RANGE_BONUS, 3.0F, EXP_BONUS, 10.0F)
	);

	/**
	 * A random option pool from {@code stat, value, stat, value, ...} (stat -> value at Lv 100; Map.of stops at ten pairs), in
	 * {@link EngraveStat} order and read-only. A stat listed twice or fewer than {@link #MIN_POOL} stats fail at startup. Armor slots
	 * and raid accessory slots ({@code AccessorySlot}) both build theirs here.
	 */
	public static Map<EngraveStat, Float> optionPool(final Object... pairs) {
		Map<EngraveStat, Float> map = new java.util.EnumMap<>(EngraveStat.class);
		for (int i = 0; i < pairs.length; i += 2) {
			if (map.put((EngraveStat)pairs[i], (Float)pairs[i + 1]) != null) {
				throw new IllegalArgumentException("option listed twice: " + pairs[i]);
			}
		}
		if (map.size() < MIN_POOL) {
			throw new IllegalArgumentException("option pool of " + map.size() + " stats, need " + MIN_POOL);
		}
		return java.util.Collections.unmodifiableMap(map);
	}

	/** The slot's pool in {@link EngraveStat} order (stable across runs). */
	public static Map<EngraveStat, Float> pool(final GearSlot slot) {
		return POOLS.get(slot);
	}

	/** Highest value a piece of {@code level} can roll for {@code stat}. */
	public static float max(final float atHundred, final int level) {
		return atHundred * (0.35F + 0.65F * Math.min(100, level) / 100.0F);
	}

	public static GearRolls roll(final ArmorPieceDef def, final RandomSource random) {
		List<EngraveStat> stats = new ArrayList<>(pool(def.slot()).keySet());
		stats.sort(java.util.Comparator.comparingInt(Enum::ordinal));
		List<StatLine> lines = new ArrayList<>();
		for (int i = 0; i < def.optionCount() && !stats.isEmpty(); i++) {
			EngraveStat stat = stats.remove(random.nextInt(stats.size()));
			float top = max(pool(def.slot()).get(stat), def.level());
			lines.add(StatLine.of(stat, round(top * (0.6F + 0.4F * random.nextFloat()), top)));
		}
		return new GearRolls(lines);
	}

	/** Small stats keep one decimal, the rest are whole numbers (at least 1). */
	public static float round(final float value, final float top) {
		if (top <= 3.5F) {
			return Math.max(0.1F, Math.round(value * 10.0F) / 10.0F);
		}
		return Math.max(1.0F, Math.round(value));
	}

	private ArmorOptions() {
	}
}
