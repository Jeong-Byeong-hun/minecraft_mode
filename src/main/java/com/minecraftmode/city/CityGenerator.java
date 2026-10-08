package com.minecraftmode.city;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Builds the capital during chunk decoration ({@code ChunkGeneratorMixin}). Chunks inside the core
 * square skip surface features and structures (ores stay), get flattened to the city floor and
 * then receive their slice of every building. Chunks in the ring around it only blend the terrain
 * before vanilla decoration runs, so trees still grow on the new slopes.
 */
public final class CityGenerator {
	/** Distance (Chebyshev) from the city center to the nearest column of the chunk. */
	private static int chunkDistance(final ChunkAccess chunk) {
		ChunkPos pos = chunk.getPos();
		int nearX = pos.getMinBlockX() <= 0 && pos.getMaxBlockX() >= 0 ? 0 : Math.min(Math.abs(pos.getMinBlockX()), Math.abs(pos.getMaxBlockX()));
		int nearZ = pos.getMinBlockZ() <= 0 && pos.getMaxBlockZ() >= 0 ? 0 : Math.min(Math.abs(pos.getMinBlockZ()), Math.abs(pos.getMaxBlockZ()));
		return Math.max(nearX, nearZ);
	}

	/** Core chunks: no surface features or structures, the city is built there instead. */
	public static boolean isCore(final ChunkGenerator generator, final WorldGenLevel level, final ChunkAccess chunk) {
		return CityZone.isCityGenerator(generator, level.getLevel()) && chunkDistance(chunk) <= CityZone.CORE;
	}

	private static boolean isBlend(final ChunkGenerator generator, final WorldGenLevel level, final ChunkAccess chunk) {
		int d = chunkDistance(chunk);
		return CityZone.isCityGenerator(generator, level.getLevel()) && d > CityZone.CORE && d <= CityZone.CORE + CityZone.BLEND;
	}

	/** Keeps only the ore steps for core chunks. */
	public static List<HolderSet<PlacedFeature>> filterFeatures(final List<HolderSet<PlacedFeature>> features) {
		List<HolderSet<PlacedFeature>> filtered = new ArrayList<>(features.size());
		for (int step = 0; step < features.size(); step++) {
			boolean keep = step == GenerationStep.Decoration.UNDERGROUND_ORES.ordinal() || step == GenerationStep.Decoration.UNDERGROUND_DECORATION.ordinal();
			filtered.add(keep ? features.get(step) : HolderSet.empty());
		}
		return filtered;
	}

	public static void beforeDecoration(final ChunkGenerator generator, final WorldGenLevel level, final ChunkAccess chunk) {
		if (isBlend(generator, level, chunk)) {
			shapeTerrain(generator, level, chunk);
		}
	}

	public static void afterDecoration(final ChunkGenerator generator, final WorldGenLevel level, final ChunkAccess chunk) {
		if (!isCore(generator, level, chunk)) {
			return;
		}
		shapeTerrain(generator, level, chunk);
		ChunkPos pos = chunk.getPos();
		Build build = new Build(level, base(generator, level), pos.getMinBlockX(), pos.getMinBlockZ());
		CityCore.build(build);
		CityNorth.build(build);
		CityMiddle.build(build);
		CitySouth.build(build);
		CityCore.lamps(build);
	}

	private static int base(final ChunkGenerator generator, final WorldGenLevel level) {
		return CityZone.baseY(generator, level, level.getLevel().getChunkSource().randomState(), level.getSeed());
	}

	/**
	 * Flattens the core to the city floor and blends the ring back to natural height. Columns are
	 * scanned directly: in 26.x the worldgen heightmaps are no longer kept up to date after the
	 * terrain step, so they cannot be trusted here.
	 */
	private static void shapeTerrain(final ChunkGenerator generator, final WorldGenLevel level, final ChunkAccess chunk) {
		int base = base(generator, level);
		int sea = generator.getSeaLevel();
		ChunkPos chunkPos = chunk.getPos();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BlockState stone = Blocks.STONE.defaultBlockState();
		BlockState dirt = Blocks.DIRT.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		int highestSection = chunk.getHighestFilledSectionIndex();
		int top = highestSection < 0 ? chunk.getMinY() : SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(highestSection)) + 15;
		for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
			for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
				int d = Math.max(Math.abs(x), Math.abs(z));
				if (d > CityZone.CORE + CityZone.BLEND) {
					continue;
				}
				int surface = top;
				while (surface > chunk.getMinY() && chunk.getBlockState(pos.set(x, surface, z)).isAir()) {
					surface--;
				}
				int solid = surface;
				while (solid > chunk.getMinY() && !isGround(chunk.getBlockState(pos.set(x, solid, z)))) {
					solid--;
				}
				int target;
				boolean core = d <= CityZone.CORE;
				if (core) {
					target = base - 1;
				} else {
					float t = (d - CityZone.CORE) / (float)CityZone.BLEND;
					t = t * t * (3 - 2 * t);
					target = Math.round(Mth.lerp(t, base - 1, solid));
				}
				// clear above the target (never below sea level, so oceans do not flow into holes)
				for (int y = target + 1; y <= surface; y++) {
					pos.set(x, y, z);
					if ((y >= sea || core) && !chunk.getBlockState(pos).isAir()) {
						level.setBlock(pos, air, 2);
					}
				}
				// fill up to the target; the core also seals the 12 layers under its floor
				int from = core ? Math.min(solid + 1, target - 12) : solid + 1;
				for (int y = from; y <= target; y++) {
					pos.set(x, y, z);
					BlockState state = y == target ? (target < sea - 1 ? Blocks.SAND.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState())
						: y >= target - 3 ? dirt : stone;
					level.setBlock(pos, state, 2);
				}
				if (!core && solid >= target && surface > target && target >= sea - 1) {
					level.setBlock(pos.set(x, target, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
				}
			}
		}
	}

	/** Terrain a column stands on: motion-blocking, not foliage (water and plants are not ground). */
	private static boolean isGround(final BlockState state) {
		return state.is(BlockTags.BLOCKS_MOTION_IN_HEIGHTMAP) && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS);
	}

	private CityGenerator() {
	}
}
