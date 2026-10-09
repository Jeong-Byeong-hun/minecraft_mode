package com.minecraftmode.entity;

/**
 * One-shot animations of named monsters and bosses. The server starts one ({@link CreatureMob#playAnim});
 * the client poses the model over {@link #ticks()} ticks.
 */
public enum CreatureAnim {
	NONE(1),
	/** Quick swing or bite. */
	ATTACK(10),
	/** Wind up overhead, then slam down. */
	SLAM(24),
	/** Arms raised, channelling. */
	CAST(30),
	/** Head back, jaw open. */
	ROAR(30),
	/** Lean forward for a rush. */
	CHARGE(24),
	/** Head forward, jaw open, sustained. */
	BREATH(40),
	/** Whole body spin. */
	SPIN(20),
	/** Wings folded, plunging. */
	DIVE(24),
	/** Arms wide, calling allies. */
	SUMMON(30),
	/** Crouch and spring. */
	LEAP(18);

	private final int ticks;

	CreatureAnim(final int ticks) {
		this.ticks = ticks;
	}

	public int ticks() {
		return this.ticks;
	}

	public static CreatureAnim byId(final int id) {
		return id >= 0 && id < values().length ? values()[id] : NONE;
	}
}
