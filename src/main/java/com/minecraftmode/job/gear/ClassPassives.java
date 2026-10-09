package com.minecraftmode.job.gear;

import static com.minecraftmode.job.engrave.EngraveStat.*;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tier passives that are plain stats (Soul Reaper and Hunter). Every reached tier adds its lines,
 * and they count like gear in {@link GearStats}. The older classes keep their passives as combat
 * rules in {@code CombatHooks} and {@code JobStats}.
 */
public final class ClassPassives {
	private static final Map<JobClass, List<List<StatLine>>> TIERS = Map.of(
		JobClass.SHINIGAMI, List.of(
			List.of(StatLine.of(MOVE_SPEED, 8), StatLine.of(DODGE, 5)),
			List.of(StatLine.of(SKILL_DAMAGE, 12)),
			List.of(StatLine.of(BASIC_DAMAGE, 15), StatLine.of(ATTACK_SPEED, 10)),
			List.of(StatLine.of(COOLDOWN, 15), StatLine.of(BOSS_DAMAGE, 15))),
		JobClass.HUNTER, List.of(
			List.of(StatLine.of(DAMAGE_REDUCTION, 5), StatLine.of(MAX_HEALTH, 2)),
			List.of(StatLine.of(CRIT_CHANCE, 10)),
			List.of(StatLine.of(DOUBLE_STRIKE, 15)),
			List.of(StatLine.of(SKILL_DAMAGE, 20), StatLine.of(MANA_REGEN, 2))));

	public static List<StatLine> of(final JobData data) {
		List<List<StatLine>> tiers = TIERS.get(data.job());
		if (tiers == null || !data.hasClass()) {
			return List.of();
		}
		List<StatLine> out = new ArrayList<>();
		for (int tier = 1; tier <= Math.min(data.tier(), tiers.size()); tier++) {
			out.addAll(tiers.get(tier - 1));
		}
		return out;
	}

	/** Lines of one tier's passive (for tests and docs). */
	public static List<StatLine> tier(final JobClass job, final int tier) {
		List<List<StatLine>> tiers = TIERS.get(job);
		return tiers == null ? List.of() : tiers.get(tier - 1);
	}

	private ClassPassives() {
	}
}
