package com.minecraftmode.city;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** A parametric house: stone base, timber or plaster upper floor, framed corners, windows and a gable roof. */
final class Houses {
	enum Style {
		/** White plaster and blue roofs of the capital. */
		CAPITAL(Blocks.STONE_BRICKS, Blocks.SMOOTH_QUARTZ, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.DARK_PRISMARINE_STAIRS, Build.glass(DyeColor.LIGHT_GRAY), Blocks.OAK_PLANKS, Blocks.DARK_OAK_DOOR),
		/** Old Town timber houses. */
		OLD_TOWN(Blocks.COBBLESTONE, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_STAIRS, Blocks.GLASS, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_DOOR),
		/** Harbor sheds and taverns. */
		HARBOR(Blocks.STONE_BRICKS, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_LOG, Blocks.SPRUCE_STAIRS, Blocks.GLASS, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_DOOR),
		/** The rogues' hall. */
		SHADOW(Blocks.DEEPSLATE_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.DARK_OAK_LOG, Blocks.DEEPSLATE_TILE_STAIRS, Build.glass(DyeColor.PURPLE), Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_DOOR);

		final Block base;
		final Block upper;
		final Block frame;
		final Block roof;
		final Block glass;
		final Block floor;
		final Block door;

		Style(final Block base, final Block upper, final Block frame, final Block roof, final Block glass, final Block floor, final Block door) {
			this.base = base;
			this.upper = upper;
			this.frame = frame;
			this.roof = roof;
			this.glass = glass;
			this.floor = floor;
			this.door = door;
		}
	}

	/** House on x0..x1, z0..z1; {@code floors} of 4 blocks; door in the middle of {@code doorSide}. */
	static void house(final Build b, final int x0, final int z0, final int x1, final int z1, final Style s, final Direction doorSide, final int floors) {
		if (!b.touches(x0 - 2, z0 - 2, x1 + 2, z1 + 2)) {
			return;
		}
		int top = floors * 4 - 1;
		b.fill(x0, -1, z0, x1, -1, z1, s.floor);
		b.walls(x0, 0, z0, x1, Math.min(3, top), z1, s.base);
		if (top > 3) {
			b.walls(x0, 4, z0, x1, top, z1, s.upper);
		}
		b.air(x0 + 1, 0, z0 + 1, x1 - 1, top, z1 - 1);
		for (int[] c : new int[][] {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
			b.fill(c[0], 0, c[1], c[0], top, c[1], Build.log(s.frame, Direction.Axis.Y));
		}
		if (top > 3) {
			b.fill(x0, 4, z0, x1, 4, z0, Build.log(s.frame, Direction.Axis.X));
			b.fill(x0, 4, z1, x1, 4, z1, Build.log(s.frame, Direction.Axis.X));
			b.fill(x0, 4, z0, x0, 4, z1, Build.log(s.frame, Direction.Axis.Z));
			b.fill(x1, 4, z0, x1, 4, z1, Build.log(s.frame, Direction.Axis.Z));
		}
		// windows
		for (int floor = 0; floor < floors; floor++) {
			int y = floor * 4 + 1;
			for (int x = x0 + 2; x <= x1 - 2; x += 3) {
				b.fill(x, y, z0, x, y + 1, z0, s.glass);
				b.fill(x, y, z1, x, y + 1, z1, s.glass);
			}
			for (int z = z0 + 2; z <= z1 - 2; z += 3) {
				b.fill(x0, y, z, x0, y + 1, z, s.glass);
				b.fill(x1, y, z, x1, y + 1, z, s.glass);
			}
		}
		// door
		int dx = (x0 + x1) / 2;
		int dz = (z0 + z1) / 2;
		switch (doorSide) {
			case NORTH -> door(b, dx, z0, s, doorSide);
			case SOUTH -> door(b, dx, z1, s, doorSide);
			case WEST -> door(b, x0, dz, s, doorSide);
			default -> door(b, x1, dz, s, doorSide);
		}
		// roof along the longer side
		boolean alongX = x1 - x0 >= z1 - z0;
		b.gableRoof(x0, z0, x1, z1, top + 1, s.roof, s.upper, alongX);
		// a little furniture
		b.lantern(dx, top, dz, true, s == Style.SHADOW);
		b.set(x0 + 1, 0, z0 + 1, Blocks.CRAFTING_TABLE);
		b.set(x1 - 1, 0, z0 + 1, Blocks.BARREL);
		b.set(x0 + 1, 0, z1 - 1, Blocks.FLOWER_POT);
	}

	private static void door(final Build b, final int x, final int z, final Style s, final Direction side) {
		b.door(x, 0, z, s.door, side);
		int ox = x + side.getStepX();
		int oz = z + side.getStepZ();
		b.set(ox, -1, oz, Blocks.STONE_BRICKS);
		b.lantern(x + (side.getAxis() == Direction.Axis.Z ? 1 : 0), 2, z + (side.getAxis() == Direction.Axis.X ? 1 : 0), false, s == Style.SHADOW);
	}

	private Houses() {
	}
}
