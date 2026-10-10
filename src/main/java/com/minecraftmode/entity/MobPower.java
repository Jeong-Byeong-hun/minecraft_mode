package com.minecraftmode.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * Damage multipliers of event monsters (dungeon halls and bosses, invaders, world bosses), kept in server memory by entity id.
 * {@code CombatHooks} multiplies what such a monster (or its projectile) deals to players. Event monsters are never saved, so the
 * table never needs to survive a restart.
 *
 * <p>Toughness is the other way round: what such a monster takes is divided by it. Vanilla caps max health at 1024, so a monster
 * meant to have more keeps {@link #HEALTH_CAP} health and takes damage divided by (wanted health / cap), like raid bosses do.
 */
public final class MobPower {
	/** Health a monster keeps when it should have more than vanilla allows; the rest becomes toughness. */
	public static final float HEALTH_CAP = 1000.0F;

	private static final Map<UUID, Float> DAMAGE = new HashMap<>();
	private static final Map<UUID, Float> TOUGHNESS = new HashMap<>();

	public static void set(final UUID id, final float multiplier) {
		DAMAGE.put(id, multiplier);
	}

	public static boolean has(final UUID id) {
		return DAMAGE.containsKey(id);
	}

	/** Multiplies an existing entry (enrage, bolster). */
	public static void scale(final UUID id, final float multiplier) {
		DAMAGE.computeIfPresent(id, (k, v) -> v * multiplier);
	}

	public static void setToughness(final UUID id, final float divisor) {
		if (divisor > 1.0F) {
			TOUGHNESS.put(id, divisor);
		} else {
			TOUGHNESS.remove(id);
		}
	}

	/** What {@code victim} takes is divided by this (1 for anything without toughness). */
	public static float toughness(final @Nullable Entity victim) {
		return victim == null ? 1.0F : TOUGHNESS.getOrDefault(victim.getUUID(), 1.0F);
	}

	public static void clear(final UUID id) {
		DAMAGE.remove(id);
		TOUGHNESS.remove(id);
	}

	public static void clearAll() {
		DAMAGE.clear();
		TOUGHNESS.clear();
	}

	/** The multiplier of {@code attacker} (1 for anything that is not an event monster). */
	public static float factor(final @Nullable Entity attacker) {
		return attacker == null ? 1.0F : DAMAGE.getOrDefault(attacker.getUUID(), 1.0F);
	}

	private MobPower() {
	}
}
