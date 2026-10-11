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
 * {@code RaidDimensionProvider}; the square at 0, 0, where another waymaster takes players back, and the avenues are built during
 * generation ({@link ResidenceTown}).
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

	/** The avenues run from the square along chunk column x = 0 and chunk row z = 0 ({@link ResidenceTown}). */
	public static boolean isAvenue(final BlockPos pos) {
		ChunkPos chunk = ChunkPos.containing(pos);
		return !isSpawn(pos) && (chunk.x() == 0 || chunk.z() == 0);
	}

	/** The square and the avenues: nobody can claim them. */
	public static boolean isPublic(final BlockPos pos) {
		return isSpawn(pos) || isAvenue(pos);
	}

	/** Where players arrive: the plaza's spawn mark south of the fountain (as in the capital), facing down the south avenue. */
	public static Vec3 arrival() {
		return new Vec3(ResidenceTown.MID + 0.5, SURFACE_Y + 1, ResidenceTown.MID + 13.5);
	}

	/** Where the residence's waymaster stands, between the fountain and the arrival spot. */
	public static BlockPos waymaster() {
		return new BlockPos(ResidenceTown.MID, SURFACE_Y + 1, ResidenceTown.MID + 8);
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

	private ResidenceDimension() {
	}
}
