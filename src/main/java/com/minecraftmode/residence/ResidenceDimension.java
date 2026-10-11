package com.minecraftmode.residence;

import com.minecraftmode.MinecraftMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The residence dimension {@code minecraft_mode:residence}: flat grassland with the overworld's dimension type (sky, day and night,
 * beds), no structures or features and no hostile spawns ({@code CityServices.blocksSpawn}). Players get there through the
 * waymaster on the plaza ({@code travel/Travel}) and claim land with deeds ({@link LandClaims}). The level stem is written by
 * {@code RaidDimensionProvider}; the arrival square at 0, 0, where another waymaster takes players back, is paved the first time
 * someone comes ({@link #ensureSpawn}).
 */
public final class ResidenceDimension {
	public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, MinecraftMode.id("residence"));
	/** Y of the grass, the top block of the flat layers. */
	public static final int SURFACE_Y = 64;
	/** The flat layers from the bottom of the world: bedrock, stone, {@link #DIRT_DEPTH} dirt, grass at {@link #SURFACE_Y}. */
	public static final int BOTTOM_Y = -64;
	public static final int DIRT_DEPTH = 7;
	/** Chunks within this many chunks of 0, 0 are the arrival square: nobody can claim them. */
	public static final int SPAWN_CHUNKS = 1;
	private static final int SQUARE = 4;

	public static boolean is(final Level level) {
		return level.dimension() == LEVEL;
	}

	public static @Nullable ServerLevel level(final MinecraftServer server) {
		return server.getLevel(LEVEL);
	}

	public static boolean isSpawn(final BlockPos pos) {
		ChunkPos chunk = ChunkPos.containing(pos);
		return Math.abs(chunk.x()) <= SPAWN_CHUNKS && Math.abs(chunk.z()) <= SPAWN_CHUNKS;
	}

	/** Where players arrive: the middle of the square, facing south, away from the waymaster. */
	public static Vec3 arrival() {
		return new Vec3(0.5, SURFACE_Y + 1, 0.5);
	}

	/** Where the residence's waymaster stands, at the north edge of the square. */
	public static BlockPos waymaster() {
		return new BlockPos(0, SURFACE_Y + 1, -3);
	}

	/** The block the flat generator puts at height {@code y} (used to give abandoned land back its first look). */
	public static BlockState natural(final int y) {
		if (y > SURFACE_Y || y < BOTTOM_Y) {
			return Blocks.AIR.defaultBlockState();
		}
		if (y == SURFACE_Y) {
			return Blocks.GRASS_BLOCK.defaultBlockState();
		}
		if (y > SURFACE_Y - 1 - DIRT_DEPTH) {
			return Blocks.DIRT.defaultBlockState();
		}
		return (y == BOTTOM_Y ? Blocks.BEDROCK : Blocks.STONE).defaultBlockState();
	}

	/** Paves the arrival square unless it is already there. */
	public static void ensureSpawn(final ServerLevel level) {
		if (level.getBlockState(new BlockPos(0, SURFACE_Y, 0)).is(Blocks.SMOOTH_STONE)) {
			return;
		}
		int y = SURFACE_Y;
		for (int x = -SQUARE; x <= SQUARE; x++) {
			for (int z = -SQUARE; z <= SQUARE; z++) {
				boolean edge = Math.abs(x) == SQUARE || Math.abs(z) == SQUARE;
				level.setBlockAndUpdate(new BlockPos(x, y, z), (edge ? Blocks.CHISELED_STONE_BRICKS : Blocks.SMOOTH_STONE).defaultBlockState());
				for (int dy = 1; dy <= 5; dy++) {
					level.setBlockAndUpdate(new BlockPos(x, y + dy, z), Blocks.AIR.defaultBlockState());
				}
			}
		}
		for (int x : new int[] {-SQUARE, SQUARE}) {
			for (int z : new int[] {-SQUARE, SQUARE}) {
				level.setBlockAndUpdate(new BlockPos(x, y + 1, z), Blocks.LANTERN.defaultBlockState());
			}
		}
	}

	private ResidenceDimension() {
	}
}
