package com.minecraftmode.job.gear.content;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.gear.ArmorSetDef;
import com.minecraftmode.job.gear.ArmorSetDef.SetBonus;
import com.minecraftmode.job.gear.ClassArmor;
import com.minecraftmode.job.gear.StatLine;
import java.util.Arrays;
import java.util.List;

/**
 * Base for the per-class armor files. A set is one call:
 *
 * <pre>
 * set("iron_bastion", 30, "Iron Bastion", "강철 보루", 0x5A6270, 0x2E4E9E, 0xD9B44A,
 *     two(line(ARMOR, 2)),
 *     three(line(KNOCKBACK_RES, 30), line(THORNS, 10)),
 *     four(line(LAST_STAND, 20)));
 * </pre>
 *
 * Rules (checked at startup): level 10..100 in steps of 10, exactly one 2-, 3- and 4-piece bonus,
 * unique ids. Set ids are saved item ids — never rename them.
 */
public abstract class ArmorContent {
	protected final JobClass job;

	protected ArmorContent(final JobClass job) {
		this.job = job;
	}

	public abstract void define();

	protected void set(
		final String id, final int level, final String en, final String ko, final int primary, final int secondary, final int accent, final SetBonus... bonuses
	) {
		if (level < 10 || level > 100 || level % 10 != 0) {
			throw new IllegalArgumentException(id + ": armor set level must be 10, 20, ..., 100");
		}
		List<Integer> pieces = Arrays.stream(bonuses).map(SetBonus::pieces).sorted().toList();
		if (!pieces.equals(List.of(2, 3, 4))) {
			throw new IllegalArgumentException(id + ": needs one 2-, 3- and 4-piece bonus, got " + pieces);
		}
		ClassArmor.add(new ArmorSetDef(id, this.job, level, en, ko, primary, secondary, accent, List.of(bonuses)));
	}

	protected static SetBonus two(final StatLine... lines) {
		return new SetBonus(2, List.of(lines));
	}

	protected static SetBonus three(final StatLine... lines) {
		return new SetBonus(3, List.of(lines));
	}

	protected static SetBonus four(final StatLine... lines) {
		return new SetBonus(4, List.of(lines));
	}

	protected static StatLine line(final EngraveStat stat, final float value) {
		return StatLine.of(stat, value);
	}
}
