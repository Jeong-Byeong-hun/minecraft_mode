package com.minecraftmode.job.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;

/**
 * Short-lived combat buffs a skill leaves on its caster (stances, empowered hits, stealth, marks).
 * Server-side only and not saved: everything here lasts seconds.
 */
public final class CombatState {
	private static final Map<UUID, CombatState> STATES = new HashMap<>();

	public enum Stance {
		/** value = % damage reduction */
		GUARD,
		/** value = % of damage taken reflected to the attacker */
		COUNTER,
		/** value = % dodge chance */
		EVADE
	}

	public Stance stance;
	public long stanceUntil;
	public float stanceValue;

	public int empowerHits;
	public float empowerBonus;
	public long empowerUntil;

	public float stealthBonus;
	public long stealthUntil;

	/** Avalon (warrior tier 4) is ready again at this game time. */
	public long avalonReadyAt;

	/** target -> mark */
	public final Map<UUID, Mark> marks = new HashMap<>();

	public record Mark(long until, float bonus) {
	}

	public static CombatState of(final Player player) {
		return STATES.computeIfAbsent(player.getUUID(), id -> new CombatState());
	}

	public static void forget(final Player player) {
		STATES.remove(player.getUUID());
	}

	public static void clear() {
		STATES.clear();
	}

	public float stanceValue(final Stance type, final long now) {
		return this.stance == type && now < this.stanceUntil ? this.stanceValue : 0.0F;
	}

	public float markBonus(final UUID target, final long now) {
		Mark mark = this.marks.get(target);
		if (mark == null) {
			return 0.0F;
		}
		if (now >= mark.until) {
			this.marks.remove(target);
			return 0.0F;
		}
		return mark.bonus;
	}
}
