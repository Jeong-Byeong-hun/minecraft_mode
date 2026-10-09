package com.minecraftmode.progress;

import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.worldgen.lair.LairDef;
import com.minecraftmode.worldgen.lair.NamedLairs;
import java.util.ArrayList;
import java.util.List;

/**
 * Permanent bonuses from the codex: +0.5% damage to bosses and named monsters per kind of named
 * monster defeated {@link #KILLS} times, +0.5% equipment drop chance per kind of lair cleared, and +1%
 * class experience per {@link #ACHIEVEMENT_STEP} achievements.
 */
public final class CollectionBonuses {
	public static final int KILLS = 10;
	public static final int ACHIEVEMENT_STEP = 5;

	public static int masteredKinds(final PlayerRecords records) {
		int n = 0;
		for (NamedDef def : NamedMobs.all()) {
			if (records.kills(def.id()) >= KILLS) {
				n++;
			}
		}
		return n;
	}

	public static int lairKinds(final PlayerRecords records) {
		int n = 0;
		for (LairDef def : NamedLairs.all()) {
			if (records.lairClears(def.id()) > 0) {
				n++;
			}
		}
		return n;
	}

	public static List<StatLine> lines(final PlayerRecords records) {
		List<StatLine> out = new ArrayList<>();
		int mastered = masteredKinds(records);
		if (mastered > 0) {
			out.add(StatLine.of(EngraveStat.BOSS_DAMAGE, 0.5F * mastered));
		}
		int lairs = lairKinds(records);
		if (lairs > 0) {
			out.add(StatLine.of(EngraveStat.ITEM_FIND, 0.5F * lairs));
		}
		int steps = records.achievements().size() / ACHIEVEMENT_STEP;
		if (steps > 0) {
			out.add(StatLine.of(EngraveStat.EXP_BONUS, steps));
		}
		return out;
	}

	private CollectionBonuses() {
	}
}
