package com.minecraftmode.entity;

/**
 * What the generic creature model reads from an entity: the current one-shot animation, how far into it, whether it flies and
 * its phase. Monsters get it from {@link CreatureMob}; pets and mounts implement it directly.
 */
public interface AnimatedCreature {
	CreatureAnim anim();

	float animTime(float partialTicks);

	boolean isFlyingCreature();

	int phase();
}
