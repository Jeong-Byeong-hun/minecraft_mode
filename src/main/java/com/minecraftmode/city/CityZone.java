package com.minecraftmode.city;

import com.minecraftmode.entity.CityNpc;
import com.minecraftmode.job.JobClass;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Where the capital (Stormhold) is: a walled square around x = 0, z = 0 in the overworld of
 * normal (noise) worlds. The floor height is derived from the natural terrain once per world seed,
 * so every chunk agrees on it without reading its neighbours.
 */
public final class CityZone {
	/** Walls stand at |x| = 100 or |z| = 100; everything inside is the protected city. */
	public static final int WALL = 100;
	/** Area rebuilt by the generator (walls plus an apron). */
	public static final int CORE = 112;
	/** Terrain blends from the city floor back to natural height over this many blocks. */
	public static final int BLEND = 24;
	/** The outskirts reach this far from the centre (96 blocks past the walls): no hostile spawns on their surface. */
	public static final int OUTSKIRTS = 196;
	/** "Surface" in the outskirts: down to this many blocks below the top block (hillsides, ditches), not the caves. */
	public static final int OUTSKIRTS_SURFACE_DEPTH = 6;

	private static final Map<Long, Integer> BASE = new ConcurrentHashMap<>();

	public static boolean isCityGenerator(final ChunkGenerator generator, final Level level) {
		return generator instanceof NoiseBasedChunkGenerator && level.dimension() == Level.OVERWORLD;
	}

	public static boolean isCityLevel(final Level level) {
		return level instanceof ServerLevel server && isCityGenerator(server.getChunkSource().getGenerator(), level);
	}

	/** How far around the homestead plains still counts as outskirts. */
	public static final int HOMESTEAD_MARGIN = 16;

	/** Outside the walls but within {@link #OUTSKIRTS} of the centre, or on (or near) the homestead plains ({@link Homestead}). */
	public static boolean outskirts(final int x, final int z) {
		return !inside(x, z) && (Math.abs(x) <= OUTSKIRTS && Math.abs(z) <= OUTSKIRTS || Homestead.distance(x, z) <= HOMESTEAD_MARGIN);
	}

	public static boolean inside(final int x, final int z) {
		return Math.abs(x) <= WALL && Math.abs(z) <= WALL;
	}

	public static boolean inside(final BlockPos pos) {
		return inside(pos.getX(), pos.getZ());
	}

	/** True when {@code pos} is inside the city of a city level. */
	public static boolean protectedAt(final Level level, final BlockPos pos) {
		return inside(pos) && isCityLevel(level);
	}

	/** First air layer of the city floor. */
	public static int baseY(final ChunkGenerator generator, final LevelHeightAccessor heights, final RandomState random, final long seed) {
		return BASE.computeIfAbsent(seed, s -> {
			int[] samples = new int[25];
			int i = 0;
			for (int x = -80; x <= 80; x += 40) {
				for (int z = -80; z <= 80; z += 40) {
					samples[i++] = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heights, random);
				}
			}
			Arrays.sort(samples);
			int sea = generator.getSeaLevel();
			return Mth.clamp(samples[samples.length / 2], sea + 1, sea + 24);
		});
	}

	public static int baseY(final ServerLevel level) {
		return baseY(level.getChunkSource().getGenerator(), level, level.getChunkSource().randomState(), level.getSeed());
	}

	/** Player spawn on the central plaza, facing the keep. */
	public static BlockPos spawn(final int base) {
		return new BlockPos(0, base, 13);
	}

	/** Where each class trainer stands. */
	public static BlockPos trainerHome(final JobClass job, final int base) {
		return switch (job) {
			case WARRIOR -> new BlockPos(51, base, -47);
			case ROGUE -> new BlockPos(-68, base, 24);
			case MAGE -> new BlockPos(-66, base, -59);
			case ARCHER -> new BlockPos(-64, base, 86);
			case PIRATE -> new BlockPos(52, base, 54);
			case SHINIGAMI -> new BlockPos(-83, base, 47);
			case HUNTER -> new BlockPos(85, base, -46);
			default -> spawn(base);
		};
	}

	/**
	 * Where the city service NPCs stand: the blacksmith and the enhancer at the forge, the raid marshal by the raid gate, the
	 * guild clerk between the guild shops, the broker in the market stall with the lectern, the dungeon warden by the dungeon gate
	 * and the royal herald, the guide, the quartermaster and the baker on the plaza by the spawn.
	 */
	public static BlockPos npcHome(final CityNpc.Role role, final int base) {
		return switch (role) {
			case BLACKSMITH -> new BlockPos(-17, base, 87);
			case RAID_MARSHAL -> new BlockPos(10, base, -55);
			case BOUNTY_CLERK -> new BlockPos(22, base, 59);
			case BROKER -> new BlockPos(-9, base, 71);
			case ENHANCER -> new BlockPos(-18, base, 84);
			case DUNGEON_WARDEN -> new BlockPos(-10, base, -55);
			case HERALD -> new BlockPos(5, base, 11);
			case GUIDE -> new BlockPos(-5, base, 11);
			case QUARTERMASTER -> new BlockPos(-5, base, 15);
			case BAKER -> new BlockPos(5, base, 15);
			case HUNT_MASTER -> new BlockPos(9, base, 13);
		};
	}

	/** The Enchanter's Hall east of the mage tower (outer walls). */
	public static final int HALL_X0 = -53;
	public static final int HALL_X1 = -35;
	public static final int HALL_Z0 = -79;
	public static final int HALL_Z1 = -61;

	/** The hall's four enchanting tables; each has a full ring of bookshelves (level 30 enchanting). */
	public static List<BlockPos> enchantingTables(final int base) {
		return List.of(new BlockPos(-48, base, -74), new BlockPos(-40, base, -74), new BlockPos(-48, base, -66), new BlockPos(-40, base, -66));
	}

	/** Anvils the city keeps in repair (the forge and the Enchanter's Hall); players cannot replace broken ones there. */
	public static List<BlockPos> anvils(final int base) {
		return List.of(new BlockPos(-20, base, 86), new BlockPos(-52, base, -70), new BlockPos(-36, base, -70));
	}

	private CityZone() {
	}
}
