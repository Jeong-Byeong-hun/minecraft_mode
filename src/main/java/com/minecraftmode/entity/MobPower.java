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
 */
public final class MobPower {
	private static final Map<UUID, Float> DAMAGE = new HashMap<>();

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

	public static void clear(final UUID id) {
		DAMAGE.remove(id);
	}

	public static void clearAll() {
		DAMAGE.clear();
	}

	/** The multiplier of {@code attacker} (1 for anything that is not an event monster). */
	public static float factor(final @Nullable Entity attacker) {
		return attacker == null ? 1.0F : DAMAGE.getOrDefault(attacker.getUUID(), 1.0F);
	}

	private MobPower() {
	}
}
