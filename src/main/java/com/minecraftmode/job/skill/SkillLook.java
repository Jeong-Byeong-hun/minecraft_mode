package com.minecraftmode.job.skill;

/**
 * Drawn skill effects that particles cannot show (a crescent sword wave, a spinning sphere). The client draws them as geometry
 * ({@code client/vfx}): {@link #GETSUGA} is the shape of a {@link SkillProjectile}, the others are {@link SkillVfx} entities that
 * live {@link #ticks} ticks. Synced by ordinal: append only.
 */
public enum SkillLook {
	NONE(0, 0.0F, false),
	/** Getsuga Tensho: a glowing crescent flying in place of the sword wave's trail (black with a red rim off a dark blade). */
	GETSUGA(0, 1.0F, false),
	/** Rasengan in the caster's hand while they dash in; lasts until the burst. */
	RASENGAN(6, 1.0F, true),
	/** Rasengan's spiral burst, as wide as the area it hits ({@link #size} is the radius before the Expansion engraving). */
	RASENGAN_BURST(14, 3.0F, false);

	private final int ticks;
	private final float size;
	private final boolean anchored;

	SkillLook(final int ticks, final float size, final boolean anchored) {
		this.ticks = ticks;
		this.size = size;
		this.anchored = anchored;
	}

	public int ticks() {
		return this.ticks;
	}

	public float size() {
		return this.size;
	}

	/** Follows its caster (held in the hand) instead of staying where it started. */
	public boolean anchored() {
		return this.anchored;
	}

	public static SkillLook byId(final int id) {
		SkillLook[] all = values();
		return id >= 0 && id < all.length ? all[id] : NONE;
	}
}
