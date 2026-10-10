package com.minecraftmode.entity;

import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMob;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.registry.ModEntities;
import com.minecraftmode.worldgen.lair.NamedLairs;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.phys.AABB;

/**
 * Natural spawning picks a mob type from the spot's spawn list before it checks the spot, and a type that cannot spawn there throws
 * the whole group away. Cave-only monsters (mine raiders, mythril golems, cave named) and named monsters outside their height band or
 * near another named monster took about half of the surface monster picks, so every other monster came at about 60% of the vanilla rate.
 * {@code NaturalSpawnerMixin} drops those types from the list at the picked spot, so the rest keep vanilla density and the rare ones keep
 * their share wherever they can spawn. Their own spawn rules still run in full afterwards; this filter only has to be cheap and never
 * drop a type that could spawn.
 */
public final class SpawnCandidates {
	/** How long (ticks) a "monster of this kind nearby" answer is reused for a chunk. */
	private static final int NEARBY_TICKS = 20;
	private static final Map<ServerLevel, Nearby> NEARBY = new WeakHashMap<>();

	/** The entries of {@code list} that could spawn at {@code pos}; the same list when nothing is dropped. */
	public static WeightedList<MobSpawnSettings.SpawnerData> filter(final WeightedList<MobSpawnSettings.SpawnerData> list, final ServerLevel level,
		final BlockPos pos) {
		List<Weighted<MobSpawnSettings.SpawnerData>> entries = list.unwrap();
		List<Weighted<MobSpawnSettings.SpawnerData>> kept = null;
		Boolean inLair = null;
		for (int i = 0; i < entries.size(); i++) {
			Weighted<MobSpawnSettings.SpawnerData> entry = entries.get(i);
			EntityType<?> type = entry.value().type();
			NamedDef named = NamedMobs.def(type);
			if (named != null && inLair == null) {
				inLair = NamedLairs.at(level, pos) != null;
			}
			boolean fits = named != null ? fitsNamed(named, level, pos, inLair) : fits(type, level, pos);
			if (!fits && kept == null) {
				kept = new ArrayList<>(entries.subList(0, i));
			} else if (fits && kept != null) {
				kept.add(entry);
			}
		}
		return kept == null ? list : WeightedList.of(kept);
	}

	private static boolean fits(final EntityType<?> type, final ServerLevel level, final BlockPos pos) {
		if (type == EntityTypes.CREEPER) {
			return false;
		}
		if (type == ModEntities.MINE_RAIDER) {
			return MineRaider.fits(pos);
		}
		if (type == ModEntities.MYTHRIL_GOLEM) {
			return MythrilGolem.fitsHeight(pos) && !nearby(level, pos, MythrilGolem.class, MythrilGolem.SPAWN_EXCLUSION_RADIUS);
		}
		return true;
	}

	/** Inside a lair the lord's own rules (any height, closer spacing) apply, so the filter leaves lair spawns alone. */
	private static boolean fitsNamed(final NamedDef def, final ServerLevel level, final BlockPos pos, final boolean inLair) {
		return inLair || def.habitat().allows(pos.getY()) && !nearby(level, pos, NamedMob.class, NamedMob.SPAWN_SPACING);
	}

	/**
	 * Whether an entity of {@code kind} is within {@code radius} of the middle of the spot's 16 x 32 x 16 cell, cached per cell for
	 * {@link #NEARBY_TICKS}. The cell rounding (a few blocks against a 64-96 block radius) is the filter's only approximation.
	 */
	private static boolean nearby(final ServerLevel level, final BlockPos pos, final Class<? extends Entity> kind, final double radius) {
		Nearby cache = NEARBY.computeIfAbsent(level, l -> new Nearby());
		long bucket = level.getGameTime() / NEARBY_TICKS;
		if (cache.bucket != bucket) {
			cache.bucket = bucket;
			cache.answers.clear();
		}
		ChunkPos chunk = ChunkPos.containing(pos);
		int cellY = pos.getY() >> 5;
		return cache.answers.computeIfAbsent(kind, k -> new Long2BooleanOpenHashMap()).computeIfAbsent(BlockPos.asLong(chunk.x(), cellY, chunk.z()),
			key -> !level.getEntitiesOfClass(kind, new AABB(new BlockPos(chunk.getMiddleBlockX(), (cellY << 5) + 16, chunk.getMiddleBlockZ())).inflate(radius))
				.isEmpty());
	}

	private static final class Nearby {
		private long bucket = Long.MIN_VALUE;
		private final Map<Class<? extends Entity>, Long2BooleanOpenHashMap> answers = new HashMap<>();
	}

	private SpawnCandidates() {
	}
}
