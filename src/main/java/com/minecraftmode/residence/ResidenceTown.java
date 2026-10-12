package com.minecraftmode.residence;

import com.minecraftmode.city.Build;
import com.minecraftmode.city.CityCore;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * The residence's public ground, built at decoration in Stormhold's style: the square (chunks within
 * {@link ResidenceDimension#SPAWN_CHUNKS} of 0, 0) is the capital's plaza ({@link CityCore#plaza}) with a round tower in each
 * corner, and four avenues paved like the capital's roads run from it along chunk column/row 0 to the edge of the world. Every
 * block is a pure function of its coordinates. Neither can be claimed ({@link ResidenceDimension#isPublic}).
 */
public final class ResidenceTown {
	/** The middle of the square and of both avenues. */
	public static final int MID = 8;
	/** The road across an avenue chunk: blocks 4..12 (9 wide like the capital's main roads), andesite edges. */
	private static final int ROAD_LO = 4;
	private static final int ROAD_HI = 12;
	private static final int LAMP_EVERY = 12;
	private static final Block[] FLOWERS = {Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY, Blocks.ALLIUM,
		Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS};

	public static void build(final WorldGenLevel level, final ChunkAccess chunk) {
		if (!ResidenceDimension.is(level.getLevel())) {
			return;
		}
		ChunkPos pos = chunk.getPos();
		int x0 = pos.getMinBlockX();
		int z0 = pos.getMinBlockZ();
		Build b = new Build(level, ResidenceDimension.SURFACE_Y + 1, x0, z0);
		if (ResidenceDimension.isSpawn(new BlockPos(x0, 0, z0))) {
			square(b);
		} else if (pos.x() == 0) {
			avenue(b, z0, true);
		} else if (pos.z() == 0) {
			avenue(b, x0, false);
		} else {
			return;
		}
		flowers(b, x0, z0);
	}

	/** The capital's plaza in the middle, the avenues running in to meet it, and a tower in each corner. */
	private static void square(final Build b) {
		int lo = -16 * ResidenceDimension.SPAWN_CHUNKS;
		int hi = 16 * ResidenceDimension.SPAWN_CHUNKS + 15;
		road(b, lo, hi, true);
		road(b, lo, hi, false);
		CityCore.plaza(b.shifted(MID, MID));
		for (int x : new int[] {lo + 6, hi - 6}) {
			for (int z : new int[] {lo + 6, hi - 6}) {
				CityCore.tower(b, x, z, 4, 14);
			}
		}
		ResidenceDimension.shops().forEach((block, pos) -> {
			b.set(pos.getX(), 0, pos.getZ(), block);
			b.lantern(pos.getX(), 1, pos.getZ(), false, false);
		});
	}

	/** One chunk of an avenue starting at {@code along}, with lamps on the verges as in the capital. */
	private static void avenue(final Build b, final int along, final boolean northSouth) {
		road(b, along, along + 15, northSouth);
		for (int a = along; a < along + 16; a++) {
			if (Math.floorMod(a, LAMP_EVERY) == LAMP_EVERY / 2) {
				for (int c : new int[] {ROAD_LO - 1, ROAD_HI + 1}) {
					b.lampPost(northSouth ? c : a, northSouth ? a : c, false);
				}
			}
		}
	}

	/** Capital road paving from {@code from} to {@code to} along the avenue's axis. */
	private static void road(final Build b, final int from, final int to, final boolean northSouth) {
		for (int a = from; a <= to; a++) {
			for (int c = ROAD_LO; c <= ROAD_HI; c++) {
				int x = northSouth ? c : a;
				int z = northSouth ? a : c;
				b.set(x, -1, z, c == ROAD_LO || c == ROAD_HI ? Blocks.POLISHED_ANDESITE.defaultBlockState() : Build.paving(x, z));
			}
		}
	}

	/** Flowers and tufts on the open grass, as inside the capital's walls. */
	private static void flowers(final Build b, final int x0, final int z0) {
		for (int x = x0; x < x0 + 16; x++) {
			for (int z = z0; z < z0 + 16; z++) {
				int h = Build.hash(x, 7, z);
				if (h % 9 == 0 && b.get(x, -1, z).is(Blocks.GRASS_BLOCK) && b.get(x, 0, z).isAir()) {
					b.set(x, 0, z, FLOWERS[(h / 9) % FLOWERS.length]);
				}
			}
		}
	}

	private ResidenceTown() {
	}
}
