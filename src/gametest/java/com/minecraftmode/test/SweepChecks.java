package com.minecraftmode.test;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * Raid arenas and dungeon runs reuse slots: a new instance starts without what an earlier party left there (InstanceSweep), whether
 * it is still loaded when the instance opens or loads back from disk afterwards.
 */
final class SweepChecks {
	/** Forces the chunk at {@code at} so litter put there stays loaded until the instance opens; wait a few ticks before {@link #litter}. */
	static void hold(final ServerLevel level, final Vec3 at, final boolean held) {
		ChunkPos chunk = ChunkPos.containing(BlockPos.containing(at));
		level.setChunkForced(chunk.x(), chunk.z(), held);
	}

	/** A floating item where the next instance will be built (no gravity: the slot may still be empty air). */
	static ItemEntity litter(final ServerLevel level, final Vec3 at) {
		ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, new ItemStack(Items.ROTTEN_FLESH), 0.0, 0.0, 0.0);
		item.setNoGravity(true);
		item.setUnlimitedLifetime();
		require(level.addFreshEntity(item), "the litter should be added");
		return item;
	}

	/**
	 * After the instance opened around {@code inside}: the litter is gone, a monster or item loading there from disk is refused, and
	 * one spawned by the instance itself comes back when its chunk loads again.
	 */
	static String check(final ServerLevel level, final ItemEntity litter, final Vec3 inside) {
		require(litter.isRemoved(), "the item left in the slot should be cleared when the instance opens");
		Zombie leftover = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.LOAD);
		leftover.setPos(inside);
		ItemEntity junk = new ItemEntity(level, inside.x, inside.y, inside.z, new ItemStack(Items.ROTTEN_FLESH), 0.0, 0.0, 0.0);
		require(!load(leftover, level, true) && !load(junk, level, true), "a monster or item an earlier party left should not load back into the instance");
		ItemEntity own = new ItemEntity(level, inside.x, inside.y, inside.z, new ItemStack(Items.ARROW), 0.0, 0.0, 0.0);
		require(load(own, level, false), "the instance's own drops are added");
		require(load(own, level, true), "the instance's own drops load back after their chunk unloads");
		return "litter cleared, leftovers refused, own entities kept";
	}

	private static boolean load(final Entity entity, final ServerLevel level, final boolean fromDisk) {
		return ServerEntityEvents.ALLOW_LOAD.invoker().onAllowLoad(entity, level, entity.spawnReason(), fromDisk);
	}

	private static void require(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private SweepChecks() {
	}
}
