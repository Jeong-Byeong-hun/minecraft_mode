package com.minecraftmode.accessory;

import static com.minecraftmode.job.engrave.EngraveStat.*;

import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.gear.StatLine;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * The five accessory slots (0.5.24), worn beside the armor in the accessory window. Each has a fixed line that grows with the
 * piece's level and its own pool of random options (stat -> value at Lv 100, rolled like armor options). Ids are saved keys (item
 * ids end in them) and the ordinal is the slot index of the worn list: append only.
 */
public enum AccessorySlot {
	NECKLACE("necklace", "Necklace", "목걸이", pool(SKILL_DAMAGE, 10.0F, MAX_HEALTH, 6.0F, MAX_MANA, 25.0F, MANA_REGEN, 2.5F, COOLDOWN, 6.0F, HEAL_ON_SKILL, 3.0F,
		BOSS_DAMAGE, 8.0F, LIFESTEAL, 3.0F, EXP_BONUS, 12.0F, ECHO, 3.0F)),
	EARRING("earring", "Earring", "귀걸이", pool(CRIT_CHANCE, 6.0F, CRIT_DAMAGE, 20.0F, FIRST_STRIKE, 12.0F, EXECUTE, 15.0F, MANA_COST, 8.0F, MAGIC_DEFENSE, 4.0F,
		DODGE, 4.0F, COOLDOWN_FLAT, 1.5F, KILL_MANA, 4.0F, ITEM_FIND, 10.0F)),
	RING("ring", "Ring", "반지", pool(BASIC_DAMAGE, 10.0F, SHOT_DAMAGE, 10.0F, SKILL_DAMAGE, 8.0F, DAMAGE, 4.0F, BOSS_DAMAGE, 8.0F, DOUBLE_STRIKE, 4.0F,
		CHAIN_LIGHTNING, 4.0F, GOLD_FIND, 5.0F, BACKSTAB, 12.0F, RANGE_BONUS, 3.0F)),
	BRACELET("bracelet", "Bracelet", "팔찌", pool(ATTACK_SPEED, 8.0F, DRAW_SPEED, 12.0F, CRIT_DAMAGE, 20.0F, SKILL_AREA, 10.0F, MANA_ON_HIT, 1.0F, LIFESTEAL, 3.0F,
		KILL_HEAL, 3.0F, SPEED_ON_KILL, 3.0F, THORNS, 20.0F, DOUBLE_STRIKE, 3.0F)),
	TALISMAN("talisman", "Talisman", "부적", pool(PROTECTION, 4.0F, MAGIC_DEFENSE, 4.0F, DAMAGE_REDUCTION, 6.0F, LAST_STAND, 10.0F, HEALTH_REGEN, 2.5F, MAX_HEALTH, 6.0F,
		KNOCKBACK_RES, 25.0F, DODGE, 4.0F, KILL_HEAL, 3.0F, MOVE_SPEED, 6.0F));

	private final String id;
	private final String en;
	private final String ko;
	private final Map<EngraveStat, Float> pool;

	AccessorySlot(final String id, final String en, final String ko, final Map<EngraveStat, Float> pool) {
		this.id = id;
		this.en = en;
		this.ko = ko;
		this.pool = pool;
	}

	private static Map<EngraveStat, Float> pool(final Object... pairs) {
		Map<EngraveStat, Float> map = new EnumMap<>(EngraveStat.class);
		for (int i = 0; i < pairs.length; i += 2) {
			map.put((EngraveStat)pairs[i], (Float)pairs[i + 1]);
		}
		return Collections.unmodifiableMap(map);
	}

	public String id() {
		return this.id;
	}

	public String en() {
		return this.en;
	}

	public String ko() {
		return this.ko;
	}

	public String nameKey() {
		return "accessory_slot.minecraft_mode." + this.name().toLowerCase(Locale.ROOT);
	}

	/** Random options this slot can roll: stat -> value at Lv 100 (in {@link EngraveStat} order). */
	public Map<EngraveStat, Float> pool() {
		return this.pool;
	}

	/** The fixed line every accessory of this slot has. */
	public StatLine baseLine(final int level) {
		return switch (this) {
			case NECKLACE -> StatLine.of(MAX_HEALTH, 1 + level / 20);
			case EARRING -> StatLine.of(CRIT_CHANCE, 1 + level / 25);
			case RING -> StatLine.of(DAMAGE, 1 + level / 25);
			case BRACELET -> StatLine.of(CRIT_DAMAGE, 2 + level / 10);
			case TALISMAN -> StatLine.of(MAGIC_DEFENSE, 1 + level / 25);
		};
	}
}
