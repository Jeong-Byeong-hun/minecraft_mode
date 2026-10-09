package com.minecraftmode.raid;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Raid modifiers on Heroic and Nightmare. Two of them rule each three-day cycle (the same for the
 * whole server); Normal raids never have them. Ids are shown in screens and saved on bosses.
 */
public enum RaidAffix {
	/** Below 30% health the boss hits 30% harder. */
	ENRAGE("enrage"),
	/** Mechanics come 20% more often (never closer than the 45 second gap). */
	TURBULENT("turbulent"),
	/** The boss takes 15% less damage. */
	FORTIFIED("fortified"),
	/** Every 20 seconds everyone bleeds. */
	BLEEDING("bleeding"),
	/** Every 30 seconds darkness pulses over the arena. */
	GLOOM("gloom"),
	/** At 75%, 50% and 25% health the boss erupts around itself (telegraphed). */
	VOLATILE("volatile");

	public static final int PER_CYCLE = 2;

	private final String id;

	RaidAffix(final String id) {
		this.id = id;
	}

	public String id() {
		return this.id;
	}

	public String nameKey() {
		return "raid.minecraft_mode.affix." + this.id;
	}

	public String descKey() {
		return this.nameKey() + ".desc";
	}

	/** The modifiers of a cycle. */
	public static List<RaidAffix> forCycle(final long cycle) {
		List<RaidAffix> pool = new ArrayList<>(List.of(values()));
		Random random = new Random(cycle * 0x2545F4914F6CDD1DL + 17L);
		List<RaidAffix> out = new ArrayList<>();
		while (out.size() < PER_CYCLE) {
			out.add(pool.remove(random.nextInt(pool.size())));
		}
		return out;
	}

	public static RaidAffix byId(final String id) {
		for (RaidAffix a : values()) {
			if (a.id.equals(id)) {
				return a;
			}
		}
		return ENRAGE;
	}
}
