package com.minecraftmode.city;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * North row: the Keep (center), the Mage Quarter tower and the Enchanter's Hall (west) and the Warrior
 * arena and barracks (east).
 */
final class CityNorth {
	static void build(final Build b) {
		keep(b);
		mageQuarter(b);
		enchanterHall(b);
		warriorQuarter(b);
	}

	static Direction toward(final int dx, final int dz) {
		return Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
	}

	// ------------------------------------------------------------------ keep

	private static void keep(final Build b) {
		if (!b.touches(-27, -99, 27, -47)) {
			return;
		}
		// courtyard with low walls and a gatehouse
		for (int x = -24; x <= 24; x++) {
			for (int z = -62; z <= -48; z++) {
				b.set(x, -1, z, Build.paving(x, z));
			}
		}
		b.fill(-24, 0, -62, -24, 3, -48, Blocks.STONE_BRICKS);
		b.fill(24, 0, -62, 24, 3, -48, Blocks.STONE_BRICKS);
		b.fill(-24, 0, -48, 24, 3, -48, Blocks.STONE_BRICKS);
		b.fill(-24, 3, -62, 24, 3, -48, Blocks.AIR);
		b.fill(-24, 3, -62, -24, 3, -48, Blocks.POLISHED_DIORITE);
		b.fill(24, 3, -62, 24, 3, -48, Blocks.POLISHED_DIORITE);
		b.fill(-24, 3, -48, 24, 3, -48, Blocks.POLISHED_DIORITE);
		b.air(-4, 0, -48, 4, 3, -48);
		CityCore.tower(b, -7, -48, 3, 10);
		CityCore.tower(b, 7, -48, 3, 10);
		b.fill(-1, -1, -62, 1, -1, -49, Build.wool(DyeColor.RED));
		raidGate(b);

		// great hall
		int x0 = -18;
		int x1 = 18;
		int z0 = -88;
		int z1 = -63;
		b.fill(x0, -1, z0, x1, -1, z1, Blocks.POLISHED_ANDESITE);
		b.walls(x0, 0, z0, x1, 13, z1, Blocks.STONE_BRICKS);
		b.walls(x0, 0, z0, x1, 0, z1, Blocks.POLISHED_DIORITE);
		b.walls(x0, 13, z0, x1, 13, z1, Blocks.POLISHED_DIORITE);
		b.air(x0 + 1, 0, z0 + 1, x1 - 1, 13, z1 - 1);
		for (int z = z0 + 4; z <= z1 - 4; z += 6) {
			b.fill(x0, 4, z, x0, 8, z + 1, Build.glass(DyeColor.BLUE));
			b.fill(x1, 4, z, x1, 8, z + 1, Build.glass(DyeColor.BLUE));
			b.fill(x0 + 1, 3, z - 2, x0 + 1, 10, z - 2, Build.wool(DyeColor.BLUE));
			b.fill(x1 - 1, 3, z - 2, x1 - 1, 10, z - 2, Build.wool(DyeColor.BLUE));
		}
		// facade: doorway and rose window
		b.air(-2, 0, z1, 2, 5, z1);
		b.fill(-3, 6, z1, 3, 6, z1, Blocks.CHISELED_STONE_BRICKS);
		for (int x = -3; x <= 3; x++) {
			for (int y = 7; y <= 12; y++) {
				double d = Math.hypot(x, y - 9.5);
				if (d <= 2.6) {
					b.set(x, y, z1, d < 1.2 ? Build.glass(DyeColor.YELLOW) : Build.glass(DyeColor.LIGHT_BLUE));
				}
			}
		}
		b.hipRoof(x0, z0, x1, z1, 14, Blocks.DARK_PRISMARINE_STAIRS, Blocks.DARK_PRISMARINE);
		// nave: pillars, carpet, chandeliers, throne
		for (int z = z1 - 3; z >= z0 + 6; z -= 6) {
			b.fill(-9, 0, z, -9, 12, z, Blocks.QUARTZ_PILLAR);
			b.fill(9, 0, z, 9, 12, z, Blocks.QUARTZ_PILLAR);
			b.lantern(-4, 12, z, true, false);
			b.lantern(4, 12, z, true, false);
		}
		b.fill(-1, -1, z0 + 5, 1, -1, z1, Build.wool(DyeColor.RED));
		b.fill(-5, 0, z0 + 1, 5, 0, z0 + 4, Blocks.POLISHED_DIORITE);
		b.set(0, 1, z0 + 2, Build.stairs(Blocks.QUARTZ_STAIRS, Direction.NORTH));
		b.set(-1, 1, z0 + 2, Blocks.GOLD_BLOCK);
		b.set(1, 1, z0 + 2, Blocks.GOLD_BLOCK);
		b.fill(-1, 2, z0 + 1, 1, 4, z0 + 1, Blocks.LAPIS_BLOCK);
		b.set(0, 5, z0 + 1, Blocks.GOLD_BLOCK);
		b.lantern(-3, 1, z0 + 2, false, false);
		b.lantern(3, 1, z0 + 2, false, false);
		// towers: four corners and the great tower behind
		CityCore.tower(b, x0, z1, 4, 22);
		CityCore.tower(b, x1, z1, 4, 22);
		CityCore.tower(b, x0, z0, 4, 22);
		CityCore.tower(b, x1, z0, 4, 22);
		CityCore.tower(b, 0, -92, 6, 34);
		// banners in front
		b.flag(-12, -60, 9, Build.wool(DyeColor.BLUE), Build.wool(DyeColor.YELLOW), Direction.EAST);
		b.flag(12, -60, 9, Build.wool(DyeColor.BLUE), Build.wool(DyeColor.YELLOW), Direction.WEST);
	}

	// ------------------------------------------------------------------ mage quarter

	/**
	 * Raid gate in the keep courtyard: a crying-obsidian arch with a violet veil, braziers and banners.
	 * The raid marshal stands beside it and sends parties to the raid arenas.
	 */
	private static void raidGate(final Build b) {
		int x0 = 13;
		int x1 = 19;
		int z = -58;
		b.fill(x0, 0, z, x0, 6, z, Blocks.CRYING_OBSIDIAN);
		b.fill(x1, 0, z, x1, 6, z, Blocks.CRYING_OBSIDIAN);
		b.fill(x0, 7, z, x1, 7, z, Blocks.OBSIDIAN);
		b.fill(x0 + 1, 6, z, x1 - 1, 6, z, Blocks.CRYING_OBSIDIAN);
		b.fill(x0 + 1, 0, z, x1 - 1, 5, z, Build.glass(DyeColor.PURPLE));
		b.set((x0 + x1) / 2, 8, z, Blocks.END_ROD);
		b.fill(x0 - 1, -1, z - 1, x1 + 1, -1, z + 3, Blocks.POLISHED_BLACKSTONE_BRICKS);
		for (int x : new int[] {x0 - 1, x1 + 1}) {
			b.set(x, 0, z + 2, Blocks.POLISHED_BLACKSTONE_BRICKS);
			b.set(x, 1, z + 2, Blocks.SOUL_CAMPFIRE);
		}
		b.flag(x0 - 2, z + 1, 6, Build.wool(DyeColor.RED), Build.wool(DyeColor.BLACK), Direction.SOUTH);
		b.flag(x1 + 2, z + 1, 6, Build.wool(DyeColor.RED), Build.wool(DyeColor.BLACK), Direction.SOUTH);
	}

	private static void mageQuarter(final Build b) {
		if (!b.touches(-97, -97, -35, -47)) {
			return;
		}
		int cx = -66;
		int cz = -72;
		int r = 9;
		int top = 44;
		// tower body
		b.cylinder(cx, cz, r, -1, -1, Blocks.PURPUR_BLOCK, false);
		b.cylinder(cx, cz, r, 0, top, Blocks.PURPUR_BLOCK, true);
		for (int y = 0; y <= top; y += 10) {
			b.cylinder(cx, cz, r, y, y, Blocks.AMETHYST_BLOCK, true);
		}
		for (int floor = 10; floor < top; floor += 10) {
			b.cylinder(cx, cz, r - 1, floor, floor, Blocks.PURPUR_BLOCK, false);
			b.set(cx + 7, floor, cz, Blocks.AIR);
		}
		BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST);
		b.fill(cx + 8, 0, cz, cx + 8, top - 1, cz, Blocks.PURPUR_BLOCK);
		for (int y = 0; y < top; y++) {
			b.set(cx + 7, y, cz, ladder);
		}
		// windows on every floor in eight directions
		for (int floor = 0; floor < top; floor += 10) {
			for (int i = 0; i < 8; i++) {
				double a = Math.PI / 4 * i + Math.PI / 8;
				int x = cx + (int)Math.round(Math.cos(a) * r);
				int z = cz + (int)Math.round(Math.sin(a) * r);
				b.fill(x, floor + 3, z, x, floor + 6, z, Build.glass(DyeColor.PURPLE));
			}
			b.lantern(cx, floor + 9, cz, true, true);
		}
		// door (south) and roof
		b.air(cx - 1, 0, cz + r, cx + 1, 3, cz + r);
		b.fill(cx - 2, 4, cz + r, cx + 2, 4, cz + r, Blocks.CHISELED_QUARTZ_BLOCK);
		b.cylinder(cx, cz, r + 1, top + 1, top + 1, Build.terracotta(DyeColor.PURPLE), false);
		b.cone(cx + 0.0, cz + 0.0, r + 0.5, top + 2, 0.55, Build.terracotta(DyeColor.PURPLE).defaultBlockState());
		b.fill(cx, top + 21, cz, cx, top + 24, cz, Blocks.END_ROD);
		// ground floor: enchanting table among bookshelves
		b.set(cx, 0, cz, Blocks.ENCHANTING_TABLE);
		for (int i = 0; i < 16; i++) {
			double a = Math.PI / 8 * i;
			if (i % 4 == 3) {
				continue;
			}
			int x = cx + (int)Math.round(Math.cos(a) * 5);
			int z = cz + (int)Math.round(Math.sin(a) * 5);
			b.fill(x, 0, z, x, 1, z, Blocks.BOOKSHELF);
		}
		// library
		b.fill(-52, -1, -94, -40, -1, -82, Blocks.DARK_OAK_PLANKS);
		b.walls(-52, 0, -94, -40, 4, -82, Blocks.BOOKSHELF);
		b.air(-51, 0, -93, -41, 4, -83);
		for (int[] c : new int[][] {{-52, -94}, {-40, -94}, {-52, -82}, {-40, -82}}) {
			b.fill(c[0], 0, c[1], c[0], 4, c[1], Blocks.PURPUR_PILLAR);
		}
		b.gableRoof(-52, -94, -40, -82, 5, Blocks.PURPUR_STAIRS, Blocks.PURPUR_BLOCK, true);
		b.door(-46, 0, -82, Blocks.DARK_OAK_DOOR, Direction.SOUTH);
		b.set(-46, 0, -88, Blocks.LECTERN);
		b.lantern(-46, 4, -88, true, true);
		// arcane garden
		for (int x = -94; x <= -38; x += 3) {
			for (int z = -94; z <= -50; z += 3) {
				int h = Build.hash(x, 3, z);
				if (h % 5 == 0 && Math.hypot(x - cx, z - cz) > r + 2 && !(x >= -53 && x <= -39 && z >= -95 && z <= -81)
					&& !(x >= CityZone.HALL_X0 - 1 && x <= CityZone.HALL_X1 + 1 && z >= CityZone.HALL_Z0 - 1 && z <= CityZone.HALL_Z1 + 1)
					&& !(x >= -47 && x <= -41 && z >= CityZone.HALL_Z1 && z <= -38)) {
					b.set(x, -1, z, Blocks.CALCITE);
					b.set(x, 0, z, Blocks.AMETHYST_CLUSTER);
				}
			}
		}
		// crying obsidian arch
		b.fill(-86, 0, -54, -86, 5, -54, Blocks.CRYING_OBSIDIAN);
		b.fill(-80, 0, -54, -80, 5, -54, Blocks.CRYING_OBSIDIAN);
		b.fill(-86, 6, -54, -80, 6, -54, Blocks.CRYING_OBSIDIAN);
		b.fill(-85, 0, -54, -81, 5, -54, Build.glass(DyeColor.PURPLE));
		b.flag(-74, -56, 8, Build.wool(DyeColor.PURPLE), Build.wool(DyeColor.MAGENTA), Direction.WEST);
		b.flag(-58, -56, 8, Build.wool(DyeColor.PURPLE), Build.wool(DyeColor.MAGENTA), Direction.EAST);
	}

	/**
	 * The Enchanter's Hall east of the mage tower: a stone hall with a purpur roof and four enchanting
	 * stations, each table ringed by bookshelves two blocks out (open toward the aisle) for full level 30
	 * enchanting, plus anvils and a grindstone for books. A paved path leads south to the road.
	 */
	private static void enchanterHall(final Build b) {
		int x0 = CityZone.HALL_X0;
		int x1 = CityZone.HALL_X1;
		int z0 = CityZone.HALL_Z0;
		int z1 = CityZone.HALL_Z1;
		if (!b.touches(x0 - 2, z0 - 2, x1 + 2, -38)) {
			return;
		}
		int cx = (x0 + x1) / 2;
		// floor, walls and roof
		b.fill(x0, -1, z0, x1, -1, z1, Blocks.POLISHED_DEEPSLATE);
		b.fill(cx - 1, -1, z0 + 1, cx + 1, -1, z1 - 1, Blocks.PURPUR_BLOCK);
		b.fill(x0 + 1, -1, z0 + 8, x1 - 1, -1, z0 + 10, Blocks.PURPUR_BLOCK);
		b.walls(x0, 0, z0, x1, 6, z1, Blocks.STONE_BRICKS);
		b.air(x0 + 1, 0, z0 + 1, x1 - 1, 5, z1 - 1);
		b.fill(x0 + 1, 6, z0 + 1, x1 - 1, 6, z1 - 1, Blocks.DARK_OAK_PLANKS);
		b.walls(x0, 6, z0, x1, 6, z1, Blocks.CHISELED_STONE_BRICKS);
		for (int[] c : new int[][] {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}, {x0, (z0 + z1) / 2}, {x1, (z0 + z1) / 2}}) {
			b.fill(c[0], 0, c[1], c[0], 6, c[1], Blocks.PURPUR_PILLAR);
		}
		for (int z = z0 + 3; z <= z1 - 3; z += 4) {
			b.fill(x0, 2, z, x0, 3, z, Build.glass(DyeColor.PURPLE));
			b.fill(x1, 2, z, x1, 3, z, Build.glass(DyeColor.PURPLE));
		}
		for (int x = x0 + 3; x <= x1 - 3; x += 4) {
			b.fill(x, 2, z0, x, 3, z0, Build.glass(DyeColor.PURPLE));
		}
		b.gableRoof(x0, z0, x1, z1, 7, Blocks.PURPUR_STAIRS, Blocks.PURPUR_BLOCK, false);
		// entrance (south) and path to the road
		b.air(cx - 1, 0, z1, cx + 1, 3, z1);
		b.fill(cx - 2, 4, z1, cx + 2, 4, z1, Blocks.CHISELED_QUARTZ_BLOCK);
		for (int z = z1 + 1; z <= -38; z++) {
			for (int x = cx - 1; x <= cx + 1; x++) {
				b.set(x, -1, z, Build.paving(x, z));
				b.air(x, 0, z, x, 2, z);
			}
		}
		b.flag(cx - 3, z1 + 1, 6, Build.wool(DyeColor.PURPLE), Build.wool(DyeColor.MAGENTA), Direction.SOUTH);
		b.flag(cx + 3, z1 + 1, 6, Build.wool(DyeColor.PURPLE), Build.wool(DyeColor.MAGENTA), Direction.SOUTH);
		// four enchanting stations: bookshelves on the ring two blocks out, two high, open toward the south
		for (var table : CityZone.enchantingTables(0)) {
			int tx = table.getX();
			int tz = table.getZ();
			b.set(tx, 0, tz, Blocks.ENCHANTING_TABLE);
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					boolean ring = Math.abs(dx) == 2 || Math.abs(dz) == 2;
					boolean opening = dz == 2 && Math.abs(dx) <= 1;
					if (ring && !opening) {
						b.fill(tx + dx, 0, tz + dz, tx + dx, 1, tz + dz, Blocks.BOOKSHELF);
					}
				}
			}
			b.set(tx - 2, 2, tz - 2, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
			b.set(tx + 2, 2, tz - 2, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true));
			b.lantern(tx, 5, tz, true, false);
		}
		// anvils and a grindstone along the side walls, a lectern at the back
		for (var anvil : CityZone.anvils(0)) {
			if (anvil.getX() >= x0 && anvil.getX() <= x1 && anvil.getZ() >= z0 && anvil.getZ() <= z1) {
				b.set(anvil.getX(), 0, anvil.getZ(), Blocks.ANVIL);
			}
		}
		b.set(x0 + 1, 0, z0 + 13, Blocks.GRINDSTONE);
		b.set(x1 - 1, 0, z0 + 13, Blocks.GRINDSTONE);
		b.set(cx, 0, z0 + 1, Blocks.LECTERN);
		b.lantern(cx, 5, z0 + 9, true, false);
		b.lantern(cx, 5, z1 - 2, true, false);
	}

	// ------------------------------------------------------------------ warrior quarter

	private static void warriorQuarter(final Build b) {
		if (!b.touches(35, -97, 97, -47)) {
			return;
		}
		arena(b, 72, -78);
		barracks(b);
		// training yard with straw dummies
		for (int x = 40; x <= 50; x++) {
			for (int z = -92; z <= -64; z++) {
				b.set(x, -1, z, Build.hash(x, 1, z) % 3 == 0 ? Blocks.COARSE_DIRT : Blocks.DIRT_PATH);
			}
		}
		for (int z = -90; z <= -66; z += 4) {
			b.set(43, 0, z, Blocks.OAK_FENCE);
			b.set(43, 1, z, Blocks.HAY_BLOCK);
			b.set(43, 2, z, Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
		}
		b.set(48, 0, -66, Blocks.ANVIL);
		b.set(48, 0, -68, Blocks.GRINDSTONE);
		b.set(48, 0, -70, Blocks.SMITHING_TABLE);
		b.set(48, 0, -72, Blocks.BARREL);
		b.flag(40, -62, 9, Build.wool(DyeColor.RED), Build.wool(DyeColor.WHITE), Direction.EAST);
		b.flag(62, -62, 9, Build.wool(DyeColor.RED), Build.wool(DyeColor.WHITE), Direction.WEST);
	}

	private static void arena(final Build b, final int cx, final int cz) {
		if (!b.touches(cx - 20, cz - 20, cx + 20, cz + 20)) {
			return;
		}
		for (int x = cx - 19; x <= cx + 19; x++) {
			for (int z = cz - 19; z <= cz + 19; z++) {
				double d = Math.hypot(x - cx, z - cz);
				if (d <= 12.5) {
					b.set(x, -1, z, Build.hash(x, 2, z) % 6 == 0 ? Blocks.COARSE_DIRT : Blocks.SAND);
					b.air(x, 0, z, x, 8, z);
				} else if (d <= 17.5) {
					int tier = (int)(d - 12.5);
					b.fill(x, -1, z, x, tier, z, Blocks.STONE_BRICKS);
					b.set(x, tier, z, Build.stairs(Blocks.STONE_BRICK_STAIRS, toward(cx - x, cz - z)));
					b.air(x, tier + 1, z, x, 9, z);
				} else if (d <= 19.5) {
					b.fill(x, -1, z, x, 9, z, Blocks.STONE_BRICKS);
					if (d > 18.5) {
						double angle = Math.toDegrees(Math.atan2(z - cz, x - cx)) + 360;
						if (angle % 24 < 6) {
							b.air(x, 6, z, x, 8, z);
						}
						if (angle % 12 < 3) {
							b.set(x, 10, z, Blocks.STONE_BRICKS);
						}
						if (angle % 45 < 2) {
							b.fill(x, 3, z, x, 9, z, Build.wool(DyeColor.RED));
						}
					}
				}
			}
		}
		// entrances (west and south) through the stands
		b.air(cx - 20, 0, cz - 2, cx - 12, 3, cz + 2);
		b.fill(cx - 20, -1, cz - 2, cx - 12, -1, cz + 2, Blocks.STONE_BRICKS);
		b.air(cx - 2, 0, cz + 12, cx + 2, 3, cz + 20);
		b.fill(cx - 2, -1, cz + 12, cx + 2, -1, cz + 20, Blocks.STONE_BRICKS);
		b.fill(cx - 2, 4, cz + 18, cx + 2, 4, cz + 19, Blocks.CHISELED_STONE_BRICKS);
		b.fill(cx - 19, 4, cz - 2, cx - 18, 4, cz + 2, Blocks.CHISELED_STONE_BRICKS);
		for (int i = 0; i < 4; i++) {
			double a = Math.PI / 2 * i + Math.PI / 4;
			b.lampPost(cx + (int)Math.round(Math.cos(a) * 10), cz + (int)Math.round(Math.sin(a) * 10), false);
		}
	}

	private static void barracks(final Build b) {
		int x0 = 38;
		int x1 = 60;
		int z0 = -58;
		int z1 = -50;
		if (!b.touches(x0 - 2, z0 - 2, x1 + 2, z1 + 2)) {
			return;
		}
		b.fill(x0, -1, z0, x1, -1, z1, Blocks.SPRUCE_PLANKS);
		b.walls(x0, 0, z0, x1, 3, z1, Blocks.STONE_BRICKS);
		b.walls(x0, 4, z0, x1, 7, z1, Blocks.SPRUCE_PLANKS);
		b.air(x0 + 1, 0, z0 + 1, x1 - 1, 7, z1 - 1);
		for (int x = x0; x <= x1; x += 4) {
			b.fill(x, 4, z0, x, 7, z0, Build.log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
			b.fill(x, 4, z1, x, 7, z1, Build.log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
			if (x + 2 <= x1) {
				b.set(x + 2, 2, z1, Blocks.GLASS);
				b.set(x + 2, 5, z1, Blocks.GLASS);
				b.set(x + 2, 5, z0, Blocks.GLASS);
			}
		}
		b.gableRoof(x0, z0, x1, z1, 8, Blocks.DARK_OAK_STAIRS, Blocks.SPRUCE_PLANKS, true);
		b.door(49, 0, z1, Blocks.SPRUCE_DOOR, Direction.SOUTH);
		b.fill(48, 2, z1 + 1, 50, 2, z1 + 1, Build.wool(DyeColor.RED));
		for (int x = x0 + 2; x <= x1 - 2; x += 3) {
			b.set(x, 0, z0 + 1, Blocks.BARREL);
			b.lantern(x, 7, z0 + 4, true, false);
		}
		b.set(x0 + 2, 0, z1 - 1, Blocks.ANVIL);
		b.set(x1 - 2, 0, z1 - 1, Blocks.GRINDSTONE);
	}

	private CityNorth() {
	}
}
