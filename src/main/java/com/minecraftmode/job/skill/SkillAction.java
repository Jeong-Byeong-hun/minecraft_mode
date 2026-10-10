package com.minecraftmode.job.skill;

import net.minecraft.network.chat.Component;

/**
 * One step of a skill. Modifiers (on-hit effects, lifesteal, ...) run before the other steps so
 * they apply no matter where they are listed. {@link #describe()} builds the tooltip line from
 * translatable keys, so descriptions always match the numbers the skill really uses.
 */
public interface SkillAction {
	void run(SkillContext ctx);

	Component describe();

	default boolean isModifier() {
		return false;
	}

	/** Takes the caster somewhere (dash, leap, blink, step, grapple). */
	default boolean moves() {
		return false;
	}

	/** Can hurt enemies. A skill with a step that {@link #moves} and one that damages closes in on enemies (see {@link Skill#engages}). */
	default boolean damages() {
		return false;
	}
}
