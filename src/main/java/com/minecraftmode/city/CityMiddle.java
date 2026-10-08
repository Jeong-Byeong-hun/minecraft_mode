package com.minecraftmode.city;

import com.minecraftmode.city.Houses.Style;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;

/** Middle row: Old Town and the rogues' Shadow Hall (west), the Cathedral and the residential ward (east). */
final class CityMiddle {
	static void build(final Build b) {
		oldTown(b);
		cathedral(b);
		residential(b);
	}

	// ------------------------------------------------------------------ old town (rogue)

	private static void oldTown(final Build b) {
		if (!b.touches(-97, -39, -33, 39)) {
			return;
		}
		// cobbled alleys everywhere except the avenue
		for (int x = -96; x <= -34; x++) {
			for (int z = -38; z <= 38; z++) {
				if (Math.abs(z) <= 4 || !b.inside(x, z)) {
					continue;
				}
				int h = Build.hash(x, 5, z) % 10;
				b.set(x, -1, z, (h < 5 ? Blocks.COBBLESTONE : h < 8 ? Blocks.MOSSY_COBBLESTONE : Blocks.GRAVEL).defaultBlockState());
			}
		}
		// north block: the Pig and Whistle tavern and houses
		Houses.house(b, -60, -34, -44, -22, Style.OLD_TOWN, Direction.EAST, 2);
		b.fill(-58, 0, -33, -58, 0, -23, Blocks.DARK_OAK_PLANKS);
		for (int z = -32; z <= -24; z += 2) {
			b.set(-56, 0, z, Blocks.BARREL);
		}
		Houses.house(b, -94, -36, -86, -28, Style.OLD_TOWN, Direction.SOUTH, 2);
		Houses.house(b, -82, -36, -72, -28, Style.OLD_TOWN, Direction.SOUTH, 2);
		Houses.house(b, -94, -22, -86, -12, Style.OLD_TOWN, Direction.NORTH, 2);
		Houses.house(b, -82, -22, -72, -12, Style.OLD_TOWN, Direction.NORTH, 1);
		Houses.house(b, -68, -18, -60, -10, Style.OLD_TOWN, Direction.SOUTH, 2);
		Houses.house(b, -56, -18, -46, -10, Style.OLD_TOWN, Direction.SOUTH, 1);
		// south block: Shadow Hall (rogue trainer) and houses
		Houses.house(b, -78, 14, -58, 34, Style.SHADOW, Direction.NORTH, 2);
		b.fill(-77, -1, 15, -59, -1, 33, Blocks.POLISHED_BLACKSTONE);
		b.fill(-69, 0, 15, -67, 0, 33, Build.carpet(DyeColor.PURPLE));
		b.fill(-71, 0, 21, -65, 0, 21, Blocks.DARK_OAK_PLANKS);
		b.fill(-71, 0, 27, -65, 0, 27, Blocks.DARK_OAK_PLANKS);
		for (int z = 17; z <= 31; z += 4) {
			b.fill(-77, 1, z, -77, 6, z, Build.wool(DyeColor.PURPLE));
			b.fill(-59, 1, z, -59, 6, z, Build.wool(DyeColor.PURPLE));
		}
		b.lantern(-68, 7, 24, true, true);
		b.lantern(-72, 7, 18, true, true);
		b.lantern(-64, 7, 30, true, true);
		b.flag(-66, 10, 8, Build.wool(DyeColor.BLACK), Build.wool(DyeColor.PURPLE), Direction.WEST);
		Houses.house(b, -94, 8, -84, 18, Style.OLD_TOWN, Direction.NORTH, 2);
		Houses.house(b, -94, 24, -84, 34, Style.OLD_TOWN, Direction.SOUTH, 1);
		Houses.house(b, -54, 10, -44, 20, Style.OLD_TOWN, Direction.NORTH, 2);
		Houses.house(b, -54, 26, -44, 36, Style.OLD_TOWN, Direction.SOUTH, 2);
		b.lampPost(-63, 8, true);
		b.lampPost(-73, 8, true);
	}

	// ------------------------------------------------------------------ cathedral

	private static void cathedral(final Build b) {
		if (!b.touches(38, -38, 94, -8)) {
			return;
		}
		int x0 = 48;
		int x1 = 88;
		int z0 = -30;
		int z1 = -16;
		b.fill(38, -1, -36, 94, -1, -10, Blocks.POLISHED_DIORITE);
		// nave
		b.walls(x0, 0, z0, x1, 14, z1, Blocks.SMOOTH_QUARTZ);
		b.air(x0 + 1, 0, z0 + 1, x1 - 1, 14, z1 - 1);
		b.fill(x0 + 1, -1, z0 + 1, x1 - 1, -1, z1 - 1, Blocks.POLISHED_ANDESITE);
		for (int x = x0 + 2; x <= x1 - 2; x += 5) {
			b.fill(x, 0, z0 - 1, x, 10, z0 - 1, Blocks.STONE_BRICKS);
			b.fill(x, 0, z1 + 1, x, 10, z1 + 1, Blocks.STONE_BRICKS);
			b.set(x, 11, z0 - 1, Build.stairs(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH));
			b.set(x, 11, z1 + 1, Build.stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH));
			if (x + 2 < x1) {
				b.fill(x + 2, 3, z0, x + 3, 10, z0, Build.glass(DyeColor.YELLOW));
				b.fill(x + 2, 3, z1, x + 3, 10, z1, Build.glass(DyeColor.YELLOW));
				b.fill(x + 2, 7, z0, x + 3, 7, z0, Build.glass(DyeColor.WHITE));
				b.fill(x + 2, 7, z1, x + 3, 7, z1, Build.glass(DyeColor.WHITE));
			}
		}
		b.gableRoof(x0, z0, x1, z1, 15, Blocks.DARK_PRISMARINE_STAIRS, Blocks.SMOOTH_QUARTZ, true);
		// bell tower at the west end, entrance facing the road
		int tx0 = 40;
		int tx1 = 48;
		int tz0 = -28;
		int tz1 = -18;
		b.walls(tx0, 0, tz0, tx1, 30, tz1, Blocks.SMOOTH_QUARTZ);
		b.air(tx0 + 1, 0, tz0 + 1, tx1 - 1, 29, tz1 - 1);
		b.walls(tx0, 30, tz0, tx1, 30, tz1, Blocks.POLISHED_DIORITE);
		for (int y = 6; y <= 26; y += 10) {
			b.fill(tx0, y, -24, tx0, y + 3, -22, Build.glass(DyeColor.YELLOW));
			b.fill(44, y, tz0, 44, y + 3, tz0, Build.glass(DyeColor.YELLOW));
			b.fill(44, y, tz1, 44, y + 3, tz1, Build.glass(DyeColor.YELLOW));
		}
		b.hipRoof(tx0, tz0, tx1, tz1, 31, Blocks.DARK_PRISMARINE_STAIRS, Blocks.DARK_PRISMARINE);
		b.air(tx0, 0, -24, tx0, 4, -22);
		b.fill(tx0, 5, -25, tx0, 5, -21, Blocks.CHISELED_QUARTZ_BLOCK);
		b.air(tx1, 0, -25, x0, 6, -21);
		b.set(44, 29, -23, Blocks.BELL);
		// aisle, pews, altar
		b.fill(tx0 + 1, -1, -23, x1 - 4, -1, -23, Build.wool(DyeColor.RED));
		for (int x = x0 + 3; x <= x1 - 8; x += 3) {
			for (int z = z0 + 2; z <= z1 - 2; z++) {
				if (Math.abs(z + 23) > 1) {
					b.set(x, 0, z, Build.stairs(Blocks.OAK_STAIRS, Direction.EAST));
				}
			}
		}
		b.fill(x1 - 4, 0, z0 + 3, x1 - 1, 0, z1 - 3, Blocks.POLISHED_DIORITE);
		b.fill(x1 - 2, 1, -25, x1 - 2, 1, -21, Blocks.GOLD_BLOCK);
		b.set(x1 - 2, 2, -23, Blocks.SEA_LANTERN);
		for (int z = -25; z <= -21; z += 2) {
			b.set(x1 - 2, 2, z, Build.candle(DyeColor.WHITE).defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
		}
		for (int x = x0 + 4; x <= x1 - 4; x += 8) {
			b.lantern(x, 14, -23, true, false);
		}
	}

	// ------------------------------------------------------------------ residential ward

	private static void residential(final Build b) {
		if (!b.touches(34, 4, 97, 39)) {
			return;
		}
		int[][] houses = {
			{38, 10, 48, 18, 0}, {54, 10, 64, 18, 0}, {70, 10, 80, 18, 0}, {86, 10, 95, 18, 0},
			{38, 26, 48, 34, 1}, {54, 26, 64, 34, 1}, {70, 26, 80, 34, 1}, {86, 26, 95, 34, 1},
		};
		for (int[] h : houses) {
			Houses.house(b, h[0], h[1], h[2], h[3], Style.CAPITAL, h[4] == 0 ? Direction.NORTH : Direction.SOUTH, 2);
		}
		// garden lane between the rows
		for (int x = 36; x <= 96; x++) {
			for (int z = 20; z <= 24; z++) {
				b.set(x, -1, z, z == 22 ? Blocks.DIRT_PATH.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState());
			}
			if (x % 8 == 4) {
				Block hedge = Blocks.AZALEA_LEAVES;
				b.set(x, 0, 20, hedge.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
				b.set(x, 0, 24, hedge.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
			}
		}
	}

	private CityMiddle() {
	}
}
