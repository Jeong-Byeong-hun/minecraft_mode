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

	/** Sitting, lying or perched: the model takes its plan's rest pose. */
	default boolean isResting() {
		return false;
	}

	/** How far into its rest pose, 0..1 (eases in and out on the client). */
	default float rest(final float partialTicks) {
		return this.isResting() ? 1.0F : 0.0F;
	}

	/** How folded the wings are, 0 (spread, flying) .. 1 (folded). */
	default float wingsFolded(final float partialTicks) {
		return this.isFlyingCreature() && !this.isResting() ? 0.0F : 1.0F;
	}

	/** Rearing on the hind legs, 0..1 (mounts before a charged jump). */
	default float stand(final float partialTicks) {
		return 0.0F;
	}

	/** Bank into a turn while flying, in degrees (positive rolls left). */
	default float roll(final float partialTicks) {
		return 0.0F;
	}

	/** Nose up (positive) or down while flying, in degrees. */
	default float tilt(final float partialTicks) {
		return 0.0F;
	}
}
