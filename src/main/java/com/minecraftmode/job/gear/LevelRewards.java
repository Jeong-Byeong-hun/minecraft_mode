package com.minecraftmode.job.gear;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.engrave.EngraveStat;
import java.util.List;

/**
 * Stats every level adds for a player with a class, each class in its own direction: warriors hit
 * harder, rogues move and attack faster, mages get MP, archers crit more, pirates find more loot,
 * soul reapers cast more often and hunters crit harder.
 */
public final class LevelRewards {
	public static List<StatLine> of(final JobData data) {
		if (!data.hasClass()) {
			return List.of();
		}
		int level = data.level();
		return switch (data.job()) {
			case WARRIOR -> List.of(StatLine.of(EngraveStat.BASIC_DAMAGE, level * 0.5F));
			case ROGUE -> List.of(StatLine.of(EngraveStat.MOVE_SPEED, round(level * 0.15F)), StatLine.of(EngraveStat.ATTACK_SPEED, level * 0.1F));
			case MAGE -> List.of(StatLine.of(EngraveStat.MAX_MANA, level * 2.0F), StatLine.of(EngraveStat.MANA_REGEN, level / 20));
			case ARCHER -> List.of(StatLine.of(EngraveStat.CRIT_CHANCE, round(level * 0.15F)));
			case PIRATE -> List.of(StatLine.of(EngraveStat.ITEM_FIND, round(level * 0.3F)), StatLine.of(EngraveStat.GOLD_FIND, round(level * 0.05F)));
			case SHINIGAMI -> List.of(StatLine.of(EngraveStat.COOLDOWN, round(level * 0.15F)));
			case HUNTER -> List.of(StatLine.of(EngraveStat.CRIT_DAMAGE, round(level * 0.4F)));
			default -> List.of();
		};
	}

	/** Level at which the class's innate ability unlocks. */
	public static final int INNATE_LEVEL = 20;

	public static boolean hasInnate(final JobData data) {
		return data.hasClass() && data.job() != JobClass.NONE && data.level() >= INNATE_LEVEL;
	}

	private static float round(final float value) {
		return Math.round(value * 10.0F) / 10.0F;
	}

	private LevelRewards() {
	}
}
