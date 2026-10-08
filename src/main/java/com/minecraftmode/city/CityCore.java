package com.minecraftmode.city;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Roads, the central plaza with its fountain, the city walls with towers and gates, and the final lamp/flower pass. */
final class CityCore {
	/** Road rectangles {x0, z0, x1, z1}; edges are drawn first so crossings stay clean. */
	static final int[][] ROADS = {
		{-100, -4, 100, 4},
		{-4, -50, 4, 100},
		{-96, -44, 96, -40},
		{-96, 40, 96, 44},
		{-32, -96, -28, 96},
		{28, -96, 32, 96},
	};

	static void build(final Build b) {
		roads(b);
		plaza(b);
		walls(b);
		gardens(b);
	}

	// ------------------------------------------------------------------ roads

	private static void roads(final Build b) {
		for (int[] r : ROADS) {
			if (b.touches(r[0], r[1], r[2], r[3])) {
				b.fill(r[0], -1, r[1], r[2], -1, r[3], Blocks.POLISHED_ANDESITE);
			}
		}
		for (int[] r : ROADS) {
			if (!b.touches(r[0], r[1], r[2], r[3])) {
				continue;
			}
			boolean alongX = r[2] - r[0] > r[3] - r[1];
			for (int x = alongX ? r[0] : r[0] + 1; x <= (alongX ? r[2] : r[2] - 1); x++) {
				for (int z = alongX ? r[1] + 1 : r[1]; z <= (alongX ? r[3] - 1 : r[3]); z++) {
					b.set(x, -1, z, Build.paving(x, z));
				}
			}
		}
		// gravel paths out of the gates
		b.fill(101, -1, -3, 112, -1, 3, Blocks.GRAVEL);
		b.fill(-112, -1, -3, -101, -1, 3, Blocks.GRAVEL);
		b.fill(-3, -1, 101, 3, -1, 112, Blocks.GRAVEL);
	}

	// ------------------------------------------------------------------ plaza

	private static void plaza(final Build b) {
		if (!b.touches(-21, -21, 21, 21)) {
			return;
		}
		for (int x = -21; x <= 21; x++) {
			for (int z = -21; z <= 21; z++) {
				double d = Math.hypot(x, z);
				if (d > 20.5) {
					continue;
				}
				BlockState floor;
				if (d > 19.5) {
					floor = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
				} else if (d > 7.0) {
					int ring = (int)d % 4;
					double angle = Math.toDegrees(Math.atan2(z, x));
					boolean spoke = Math.abs(((angle + 360) % 45) - 22.5) > 19.5;
					floor = (spoke ? Blocks.POLISHED_ANDESITE : ring < 2 ? Blocks.POLISHED_DIORITE : Blocks.SMOOTH_STONE).defaultBlockState();
				} else {
					floor = Blocks.POLISHED_ANDESITE.defaultBlockState();
				}
				b.set(x, -1, z, floor);
				b.air(x, 0, z, x, 6, z);
			}
		}
		// fountain: basin, water, pillar with four falling streams and a golden crown
		b.cylinder(0.0, 0.0, 5.0, -1, -1, Blocks.PRISMARINE_BRICKS, false);
		b.cylinder(0.0, 0.0, 5.0, 0, 0, Blocks.POLISHED_DIORITE, true);
		b.cylinder(0.0, 0.0, 4.0, 0, 0, Blocks.WATER, false);
		b.fill(0, -2, 0, 0, -2, 0, Blocks.SEA_LANTERN);
		b.fill(0, 0, 0, 0, 3, 0, Blocks.QUARTZ_PILLAR);
		b.set(0, 4, 0, Blocks.CHISELED_QUARTZ_BLOCK);
		b.set(0, 5, 0, Blocks.GOLD_BLOCK);
		b.set(0, 6, 0, Blocks.END_ROD);
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			b.set(dir.getStepX(), 3, dir.getStepZ(), Blocks.WATER);
			b.set(dir.getStepX() * 3, -1, dir.getStepZ() * 3, Blocks.SEA_LANTERN);
		}
		// spawn mark
		b.set(0, -1, 13, Blocks.CHISELED_STONE_BRICKS);
		// planters with trees and benches on the diagonals
		int[][] corners = {{-12, -12}, {12, -12}, {-12, 12}, {12, 12}};
		for (int[] c : corners) {
			b.walls(c[0] - 2, 0, c[1] - 2, c[0] + 2, 0, c[1] + 2, Blocks.STONE_BRICKS);
			b.fill(c[0] - 1, 0, c[1] - 1, c[0] + 1, 0, c[1] + 1, Blocks.GRASS_BLOCK);
			b.fill(c[0] - 1, -1, c[1] - 1, c[0] + 1, -1, c[1] + 1, Blocks.DIRT);
			b.tree(c[0], c[1], 6, Blocks.OAK_LOG, Blocks.OAK_LEAVES);
			b.set(c[0], 0, c[1], Blocks.OAK_LOG);
		}
		for (int i = 0; i < 8; i++) {
			double a = Math.PI / 4 * i + Math.PI / 8;
			int x = (int)Math.round(Math.cos(a) * 17.5);
			int z = (int)Math.round(Math.sin(a) * 17.5);
			b.lampPost(x, z, false);
		}
	}

	// ------------------------------------------------------------------ walls

	private static void walls(final Build b) {
		int w = CityZone.WALL;
		wallSegment(b, -w, -w, w, -w + 2);
		wallSegment(b, -w, w - 2, w, w);
		wallSegment(b, -w, -w, -w + 2, w);
		wallSegment(b, w - 2, -w, w, w);
		// gates (east, west, south)
		b.air(w - 2, 0, -4, w, 7, 4);
		b.air(-w, 0, -4, -w + 2, 7, 4);
		b.air(-4, 0, w - 2, 4, 7, w);
		gateFrame(b, w - 2, w, -4, 4, true);
		gateFrame(b, -w, -w + 2, -4, 4, true);
		gateFrame(b, -4, 4, w - 2, w, false);
		// towers
		int[][] big = {{-w + 1, -w + 1}, {w - 1, -w + 1}, {-w + 1, w - 1}, {w - 1, w - 1}};
		for (int[] t : big) {
			tower(b, t[0], t[1], 6, 18);
		}
		int[][] gate = {{w - 1, -8}, {w - 1, 8}, {-w + 1, -8}, {-w + 1, 8}, {-8, w - 1}, {8, w - 1}};
		for (int[] t : gate) {
			tower(b, t[0], t[1], 4, 16);
		}
		int[][] mid = {{w - 1, -50}, {w - 1, 50}, {-w + 1, -50}, {-w + 1, 50}, {-50, -w + 1}, {50, -w + 1}, {0, -w + 1}, {-50, w - 1}, {50, w - 1}};
		for (int[] t : mid) {
			tower(b, t[0], t[1], 4, 14);
		}
	}

	private static void wallSegment(final Build b, final int x0, final int z0, final int x1, final int z1) {
		if (!b.touches(x0, z0, x1, z1)) {
			return;
		}
		b.fill(x0, -3, z0, x1, -1, z1, Blocks.STONE_BRICKS);
		b.fill(x0, 0, z0, x1, 10, z1, Blocks.STONE_BRICKS);
		b.fill(x0, 0, z0, x1, 0, z1, Blocks.POLISHED_DIORITE);
		b.fill(x0, 9, z0, x1, 9, z1, Blocks.POLISHED_DIORITE);
		b.fill(x0, 10, z0, x1, 10, z1, Blocks.SMOOTH_STONE);
		boolean alongX = x1 - x0 > z1 - z0;
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean outer = alongX ? (z == z0 && z0 < 0 || z == z1 && z1 > 0) : (x == x0 && x0 < 0 || x == x1 && x1 > 0);
				boolean inner = alongX ? (z == z1 && z0 < 0 || z == z0 && z1 > 0) : (x == x1 && x0 < 0 || x == x0 && x1 > 0);
				int along = alongX ? x : z;
				if (outer && Math.floorMod(along, 2) == 0) {
					b.set(x, 11, z, Blocks.STONE_BRICKS);
				}
				if (inner && Math.floorMod(along, 6) == 0) {
					b.set(x, 11, z, Blocks.POLISHED_DIORITE);
				}
				if ((outer || inner) && Math.floorMod(along, 12) == 0) {
					b.fill(x, 1, z, x, 8, z, Blocks.CHISELED_STONE_BRICKS);
				}
			}
		}
	}

	private static void gateFrame(final Build b, final int x0, final int x1, final int z0, final int z1, final boolean eastWest) {
		if (eastWest) {
			b.fill(x0, 8, z0 - 1, x1, 8, z1 + 1, Blocks.CHISELED_STONE_BRICKS);
			b.fill(x0, 0, z0 - 1, x1, 7, z0 - 1, Blocks.POLISHED_DIORITE);
			b.fill(x0, 0, z1 + 1, x1, 7, z1 + 1, Blocks.POLISHED_DIORITE);
		} else {
			b.fill(x0 - 1, 8, z0, x1 + 1, 8, z1, Blocks.CHISELED_STONE_BRICKS);
			b.fill(x0 - 1, 0, z0, x0 - 1, 7, z1, Blocks.POLISHED_DIORITE);
			b.fill(x1 + 1, 0, z0, x1 + 1, 7, z1, Blocks.POLISHED_DIORITE);
		}
	}

	/** Round tower with a blue cone roof. */
	static void tower(final Build b, final int cx, final int cz, final int r, final int height) {
		if (!b.touches(cx - r - 2, cz - r - 2, cx + r + 2, cz + r + 2)) {
			return;
		}
		b.cylinder(cx, cz, r, -3, height, Blocks.STONE_BRICKS, false);
		b.cylinder(cx, cz, r - 1, 1, height - 1, Blocks.AIR, false);
		b.cylinder(cx, cz, r - 1, 10, 10, Blocks.SPRUCE_PLANKS, false);
		b.cylinder(cx, cz, r, 0, 0, Blocks.POLISHED_DIORITE, true);
		b.cylinder(cx, cz, r, height - 1, height - 1, Blocks.POLISHED_DIORITE, true);
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			int x = cx + dir.getStepX() * r;
			int z = cz + dir.getStepZ() * r;
			b.set(x, 5, z, Blocks.AIR);
			b.set(x, 6, z, Blocks.AIR);
			b.set(x, 13, z, Blocks.AIR);
		}
		b.cylinder(cx, cz, r + 1, height + 1, height + 1, Blocks.DARK_PRISMARINE, true);
		b.cone(cx + 0.0, cz + 0.0, r + 0.5, height + 2, 0.75, Blocks.DARK_PRISMARINE.defaultBlockState());
		b.lantern(cx, height - 1, cz, true, false);
		b.lantern(cx, 9, cz, true, false);
	}

	// ------------------------------------------------------------------ gardens

	private static void gardens(final Build b) {
		// small parks around the plaza between the avenues
		int[][] parks = {{-26, -36, -8, -10}, {8, -36, 26, -10}, {-26, 10, -8, 36}, {8, 10, 26, 36}};
		for (int[] p : parks) {
			if (!b.touches(p[0], p[1], p[2], p[3])) {
				continue;
			}
			b.walls(p[0], -1, p[1], p[2], -1, p[3], Blocks.POLISHED_ANDESITE.defaultBlockState());
			int cx = (p[0] + p[2]) / 2;
			int cz = (p[1] + p[3]) / 2;
			b.tree(cx - 4, cz - 5, 7, Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES);
			b.tree(cx + 4, cz + 5, 6, Blocks.OAK_LOG, Blocks.OAK_LEAVES);
			b.fill(cx - 1, -1, cz - 1, cx + 1, -1, cz + 1, Blocks.WATER);
			b.set(cx, -2, cz, Blocks.SEA_LANTERN);
			for (Direction dir : Direction.Plane.HORIZONTAL) {
				b.set(cx + dir.getStepX() * 3, 0, cz + dir.getStepZ() * 3, Build.stairs(Blocks.SPRUCE_STAIRS, dir));
			}
		}
	}

	// ------------------------------------------------------------------ final pass

	/** Street lamps along every road and flowers on open grass; runs after all buildings. */
	static void lamps(final Build b) {
		for (int[] r : ROADS) {
			if (!b.touches(r[0] - 1, r[1] - 1, r[2] + 1, r[3] + 1)) {
				continue;
			}
			boolean alongX = r[2] - r[0] > r[3] - r[1];
			if (alongX) {
				for (int x = r[0] + 6; x <= r[2] - 6; x += 12) {
					freeLamp(b, x, r[1] - 1);
					freeLamp(b, x, r[3] + 1);
				}
			} else {
				for (int z = r[1] + 6; z <= r[3] - 6; z += 12) {
					freeLamp(b, r[0] - 1, z);
					freeLamp(b, r[2] + 1, z);
				}
			}
		}
		Block[] flowers = {Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY, Blocks.ALLIUM, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS};
		for (int x = -CityZone.WALL + 3; x <= CityZone.WALL - 3; x++) {
			for (int z = -CityZone.WALL + 3; z <= CityZone.WALL - 3; z++) {
				if (!b.inside(x, z)) {
					continue;
				}
				int h = Build.hash(x, 7, z);
				if (h % 9 == 0 && b.get(x, -1, z).is(Blocks.GRASS_BLOCK) && b.get(x, 0, z).isAir()) {
					b.set(x, 0, z, flowers[(h / 9) % flowers.length]);
				}
			}
		}
	}

	private static void freeLamp(final Build b, final int x, final int z) {
		if (b.inside(x, z) && b.get(x, -1, z).is(Blocks.GRASS_BLOCK) && b.get(x, 0, z).isAir() && b.get(x, 2, z).isAir()) {
			b.lampPost(x, z, false);
		}
	}

	private CityCore() {
	}
}
