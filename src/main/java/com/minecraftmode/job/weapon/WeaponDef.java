package com.minecraftmode.job.weapon;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.skill.Fx;
import com.minecraftmode.job.skill.Skill;
import java.util.List;

/**
 * A class weapon. Anyone can hold it and use its basic attack; skills and engravings only work for
 * a player of {@link #job()} who has reached {@link #tier()} and {@link #level()}.
 *
 * @param fx default effect style of its skills
 */
public record WeaponDef(String id, JobClass job, int tier, int level, Archetype archetype, String en, String ko, WeaponArt art, Fx fx, List<Skill> skills) {
	public WeaponDef {
		skills = List.copyOf(skills);
	}

	/** Base number every damage value of this weapon scales from; grows with tier and level. */
	public float power() {
		return 4.0F + 2.2F * this.tier + 0.06F * this.level;
	}

	public float attackDamage() {
		return this.power() * this.archetype.damageMultiplier();
	}

	public String nameKey() {
		return "item.minecraft_mode." + this.id;
	}
}
