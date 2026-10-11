package com.minecraftmode.raid;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Raid arenas and dungeon runs reuse fixed slots, and the next party must not find what the last one left there (drops, thrown
 * junk, arrows, monsters). Closing an instance removes what is loaded, but a party that wiped or logged off is gone a tick before
 * that: its chunks unload and save everything with them, and it all loads back a few ticks after the next build in that slot
 * (entities load after the blocks), when the new instance already counts it as its own. So a new instance clears its area once
 * ({@link #clear}), everything spawned in it while it runs carries its tag ({@link #admit}), and whatever loads there from disk
 * without that tag is refused. Tagging instead of refusing every load keeps the instance's own monsters when a far room unloads and
 * loads again.
 */
public final class InstanceSweep {
	private static final String TAG = "minecraft_mode_instance:";

	public static String newTag() {
		return TAG + UUID.randomUUID();
	}

	/** Removes everything but players in {@code box} (what is loaded; the rest is refused when it loads). */
	public static int clear(final ServerLevel level, final AABB box) {
		int removed = 0;
		for (Entity e : level.getEntitiesOfClass(Entity.class, box, e -> !(e instanceof Player))) {
			e.discard();
			removed++;
		}
		return removed;
	}

	/**
	 * Whether {@code entity}, added to the world at a running instance whose tag is {@code tag} (null outside one), may stay: a new
	 * entity is tagged and stays, one read from disk stays only with the tag. Players always stay.
	 */
	public static boolean admit(final Entity entity, final @Nullable String tag, final boolean fromDisk) {
		if (tag == null || entity instanceof Player) {
			return true;
		}
		if (!fromDisk) {
			entity.addTag(tag);
			return true;
		}
		return entity.entityTags().contains(tag);
	}

	private InstanceSweep() {
	}
}
