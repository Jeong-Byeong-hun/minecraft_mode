package com.minecraftmode.job.content;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.skill.Fx;
import com.minecraftmode.job.skill.Skill;
import com.minecraftmode.job.skill.SkillAction;
import com.minecraftmode.job.skill.SkillKind;
import com.minecraftmode.job.weapon.Archetype;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.WeaponArt;
import com.minecraftmode.job.weapon.WeaponDef;
import java.util.Arrays;
import java.util.List;

/**
 * Base for the per-class content files. A weapon is one call:
 *
 * <pre>
 * weapon("iron_greatsword", 1, 10, Archetype.GREATSWORD, "Iron Greatsword", "철 대검",
 *     art(0xB8C0C8, 0x6B4A2B, 0xA33A2A), fx(Fx.Kind.SLASH, 0xE0E0E0),
 *     skill("crushing_cleave", "Crushing Cleave", "분쇄 베기", SkillKind.ATTACK, 6, 10, slash(4, 150, 1.6)),
 *     ...);
 * </pre>
 *
 * Rules (checked at startup): tier 1..4 with the level inside the tier's range
 * (T1 10-24, T2 25-44, T3 45-69, T4 70-100), at least 3 skills (4 for tier 4), unique ids.
 */
public abstract class ClassContent {
	protected final JobClass job;

	protected ClassContent(final JobClass job) {
		this.job = job;
	}

	public abstract void define();

	protected void weapon(
		final String id, final int tier, final int level, final Archetype archetype, final String en, final String ko, final WeaponArt art, final Fx fx,
		final Skill... skills
	) {
		if (tier < 1 || tier > 4) {
			throw new IllegalArgumentException(id + ": tier must be 1..4");
		}
		int min = JobProgression.levelForTier(tier);
		int max = tier == 4 ? JobProgression.MAX_LEVEL : JobProgression.levelForTier(tier + 1) - 1;
		if (level < min || level > max) {
			throw new IllegalArgumentException(id + ": level " + level + " outside tier " + tier + " range " + min + "-" + max);
		}
		if (skills.length < (tier == 4 ? 4 : 3)) {
			throw new IllegalArgumentException(id + ": needs at least " + (tier == 4 ? 4 : 3) + " skills");
		}
		if (skills.length > 4) {
			throw new IllegalArgumentException(id + ": at most 4 skills (4 skill keys)");
		}
		List<Skill> list = Arrays.stream(skills)
			.map(s -> new Skill(id + "." + s.id(), s.en(), s.ko(), s.kind(), s.cooldownTicks(), s.manaCost(), s.actions(), s.fx()))
			.toList();
		JobWeapons.add(new WeaponDef(id, this.job, tier, level, archetype, en, ko, art, fx, list));
	}

	/** @param cooldown seconds */
	protected static Skill skill(final String slug, final String en, final String ko, final SkillKind kind, final double cooldown, final int mana, final SkillAction... actions) {
		if (actions.length == 0) {
			throw new IllegalArgumentException(slug + ": no actions");
		}
		return new Skill(slug, en, ko, kind, (int)Math.round(cooldown * 20), mana, List.of(actions), null);
	}

	protected static WeaponArt art(final int metal, final int grip, final int accent) {
		return new WeaponArt(metal, grip, accent);
	}

	protected static Fx fx(final Fx.Kind kind, final int color) {
		return new Fx(kind, color);
	}
}
