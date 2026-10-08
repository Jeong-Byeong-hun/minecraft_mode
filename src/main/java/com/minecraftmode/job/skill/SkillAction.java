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
}
