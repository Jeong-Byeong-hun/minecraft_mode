package com.minecraftmode.city;

import com.minecraftmode.city.Houses.Style;
import com.minecraftmode.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * South row: the Archer park and range with the Urahara Shop (west), the market and Adventurers' Guild (center), the harbor with a
 * pirate ship (east).
 */
final class CitySouth {
	static void build(final Build b) {
		archerPark(b);
		market(b);
		forge(b);
		harbor(b);
	}

	// ------------------------------------------------------------------ archer park

	private static void archerPark(final Build b) {
		if (!b.touches(-97, 45, -35, 97)) {
			return;
		}
		// trees on a jittered grid, away from the lodge and the range
		for (int gx = -92; gx <= -40; gx += 8) {
			for (int gz = 50; gz <= 94; gz += 8) {
				int h = Build.hash(gx, 9, gz);
				int x = gx + h % 3 - 1;
				int z = gz + (h / 3) % 3 - 1;
				boolean lodge = x >= -63 && x <= -41 && z >= 45 && z <= 63;
				boolean range = x >= -96 && x <= -62 && z >= 74 && z <= 96;
				boolean shop = x >= -94 && x <= -72 && z >= 45 && z <= 64;
				if (!lodge && !range && !shop) {
					boolean birch = h % 4 == 0;
					b.tree(x, z, 5 + h % 3, birch ? Blocks.BIRCH_LOG : Blocks.OAK_LOG, birch ? Blocks.BIRCH_LEAVES : Blocks.OAK_LEAVES);
				}
			}
		}
		// ranger lodge: log cabin
		int x0 = -60;
		int x1 = -44;
		int z0 = 48;
		int z1 = 60;
		if (b.touches(x0 - 2, z0 - 2, x1 + 2, z1 + 2)) {
			b.fill(x0, -1, z0, x1, -1, z1, Blocks.SPRUCE_PLANKS);
			b.walls(x0, 0, z0, x1, 4, z1, Build.log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
			b.air(x0 + 1, 0, z0 + 1, x1 - 1, 4, z1 - 1);
			for (int x = x0 + 3; x <= x1 - 3; x += 4) {
				b.fill(x, 1, z0, x + 1, 2, z0, Blocks.GLASS);
				b.fill(x, 1, z1, x + 1, 2, z1, Blocks.GLASS);
			}
			b.gableRoof(x0, z0, x1, z1, 5, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_PLANKS, true);
			b.door(-52, 0, z0, Blocks.SPRUCE_DOOR, Direction.NORTH);
			b.set(x0 + 2, 0, z1 - 1, Blocks.FLETCHING_TABLE);
			b.set(x0 + 4, 0, z1 - 1, Blocks.TARGET);
			b.set(x1 - 2, 0, z1 - 1, Blocks.BARREL);
			b.set(x1 - 4, 0, z1 - 1, Blocks.HAY_BLOCK);
			b.lantern(-52, 4, 54, true, false);
			b.flag(-62, 50, 8, Build.wool(DyeColor.GREEN), Build.wool(DyeColor.LIME), Direction.WEST);
		}
		// archery range: three lanes, targets on hay, a hay backstop
		if (b.touches(-96, 76, -62, 96)) {
			for (int z = 80; z <= 92; z += 4) {
				b.fill(-91, -1, z, -68, -1, z, Blocks.DIRT_PATH);
				b.set(-92, 0, z, Blocks.HAY_BLOCK);
				b.set(-92, 1, z, Blocks.TARGET);
				b.set(-92, 2, z, Blocks.TARGET);
				b.set(-67, 0, z, Blocks.SPRUCE_FENCE);
				b.lantern(-67, 1, z, false, false);
			}
			b.fill(-94, 0, 78, -94, 3, 94, Blocks.HAY_BLOCK);
			b.fill(-68, -1, 78, -68, -1, 94, Blocks.SPRUCE_PLANKS);
		}
		uraharaShop(b);
	}

	/** The Soul Reaper trainer's post: a little candy shop with a striped green-and-white awning (Bleach). */
	private static void uraharaShop(final Build b) {
		int x0 = -90;
		int x1 = -76;
		int z0 = 50;
		int z1 = 60;
		if (!b.touches(x0 - 2, z0 - 3, x1 + 2, z1 + 2)) {
			return;
		}
		b.fill(x0, -1, z0, x1, -1, z1, Blocks.SPRUCE_PLANKS);
		b.walls(x0, 0, z0, x1, 4, z1, Blocks.DARK_OAK_PLANKS);
		b.air(x0 + 1, 0, z0 + 1, x1 - 1, 4, z1 - 1);
		for (int x = x0; x <= x1; x += 7) {
			b.fill(x, 0, z0, x, 4, z0, Build.log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
			b.fill(x, 0, z1, x, 4, z1, Build.log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
		}
		// shop front: paper screens either side of the door, a striped awning and the sign board
		for (int x = x0 + 1; x <= x1 - 1; x++) {
			if (Math.abs(x + 83) > 1) {
				b.fill(x, 1, z0, x, 2, z0, Blocks.STAINED_GLASS_PANE.pick(DyeColor.WHITE));
			}
			b.set(x, 3, z0 - 1, Build.wool((x & 1) == 0 ? DyeColor.GREEN : DyeColor.WHITE));
		}
		b.fill(x0 + 3, 4, z0 - 1, x1 - 3, 4, z0 - 1, Blocks.DARK_OAK_PLANKS);
		b.gableRoof(x0, z0, x1, z1, 5, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DARK_OAK_PLANKS, true);
		b.door(-83, 0, z0, Blocks.SPRUCE_DOOR, Direction.NORTH);
		// candy shelves, a tea table and the trapdoor to the training ground below
		for (int x = x0 + 1; x <= x1 - 1; x += 2) {
			b.set(x, 0, z1 - 1, Blocks.BARREL);
			b.set(x, 1, z1 - 1, (x / 2 & 1) == 0 ? Blocks.CAKE : Blocks.DECORATED_POT);
		}
		b.set(x0 + 2, 0, z0 + 2, Blocks.SPRUCE_TRAPDOOR);
		b.set(x1 - 3, 0, z0 + 4, Blocks.SPRUCE_SLAB);
		b.set(x1 - 2, 0, z0 + 4, Blocks.POTTED_BAMBOO);
		b.lantern(-83, 4, 55, true, false);
		b.lantern(x0 + 2, 2, z0 - 1, true, false);
		b.lantern(x1 - 2, 2, z0 - 1, true, false);
	}

	// ------------------------------------------------------------------ market and guild

	private static void market(final Build b) {
		if (!b.touches(-27, 45, 27, 97)) {
			return;
		}
		for (int x = -26; x <= -6; x++) {
			for (int z = 46; z <= 80; z++) {
				b.set(x, -1, z, Build.paving(x, z));
			}
		}
		stall(b, -24, 48, Build.wool(DyeColor.RED), ModBlocks.SHOP_BLOCK);
		stall(b, -24, 58, Build.wool(DyeColor.ORANGE), ModBlocks.BLACKSMITH_SHOP);
		stall(b, -24, 68, Build.wool(DyeColor.GREEN), ModBlocks.GROCER_SHOP);
		stall(b, -14, 48, Build.wool(DyeColor.PURPLE), ModBlocks.JEWELER_SHOP);
		stall(b, -14, 58, Build.wool(DyeColor.CYAN), ModBlocks.ALCHEMIST_SHOP);
		stall(b, -14, 68, Build.wool(DyeColor.LIGHT_BLUE), Blocks.MELON);
		// Adventurers' Guild: guild shop and engraving tables
		Houses.house(b, 8, 48, 26, 70, Style.CAPITAL, Direction.WEST, 2);
		b.fill(9, -1, 49, 25, -1, 69, Blocks.POLISHED_ANDESITE);
		b.fill(9, -1, 58, 25, -1, 60, Build.wool(DyeColor.BLUE));
		b.set(24, 0, 56, ModBlocks.GUILD_SHOP);
		b.set(24, 0, 62, ModBlocks.GUILD_SHOP);
		for (int z = 51; z <= 67; z += 4) {
			b.set(14, 0, z, ModBlocks.ENGRAVING_TABLE);
			b.set(19, 0, z, ModBlocks.ENGRAVING_TABLE);
		}
		b.set(24, 0, 50, Blocks.CARTOGRAPHY_TABLE);
		b.set(24, 0, 68, Blocks.LECTERN);
		b.flag(8, 46, 9, Build.wool(DyeColor.BLUE), Build.wool(DyeColor.YELLOW), Direction.EAST);
		// south square by the gate
		for (int x = -12; x <= 12; x++) {
			for (int z = 82; z <= 96; z++) {
				if (Math.abs(x) > 4) {
					b.set(x, -1, z, Build.paving(x, z));
				}
			}
		}
		b.cylinder(-9.0, 89.0, 2.0, 0, 0, Blocks.POLISHED_DIORITE, true);
		b.cylinder(-9.0, 89.0, 1.0, 0, 0, Blocks.WATER, false);
		b.cylinder(9.0, 89.0, 2.0, 0, 0, Blocks.POLISHED_DIORITE, true);
		b.cylinder(9.0, 89.0, 1.0, 0, 0, Blocks.WATER, false);
		b.tree(18, 84, 6, Blocks.CHERRY_LOG, Blocks.CHERRY_LEAVES);
		b.tree(-18, 84, 6, Blocks.CHERRY_LOG, Blocks.CHERRY_LEAVES);
	}

	/**
	 * The blacksmith's forge west of the south square: an open-fronted smithy (cobblestone walls, dark oak
	 * roof, brick chimney with a smoking campfire) with blast furnace, furnace, lava cauldron, anvil,
	 * grindstone and smithing table. Master Smith Volund stands at its open side ({@link CityZone#npcHome}).
	 */
	private static void forge(final Build b) {
		int x0 = -26;
		int x1 = -15;
		int z0 = 82;
		int z1 = 92;
		if (!b.touches(x0 - 1, z0 - 1, x1 + 1, z1 + 1)) {
			return;
		}
		b.fill(x0, -1, z0, x1, -1, z1, Blocks.STONE_BRICKS);
		b.air(x0, 0, z0, x1, 6, z1);
		b.fill(x0, 0, z0, x0, 4, z1, Blocks.COBBLESTONE);
		b.fill(x0, 0, z0, x1 - 3, 4, z0, Blocks.COBBLESTONE);
		b.fill(x0, 0, z1, x1 - 3, 4, z1, Blocks.COBBLESTONE);
		b.fill(x0, 2, z0 + 3, x0, 2, z0 + 4, Blocks.IRON_BARS);
		b.fill(x0, 2, z1 - 4, x0, 2, z1 - 3, Blocks.IRON_BARS);
		for (int[] post : new int[][] {{x1, z0}, {x1, z1}, {x1 - 3, z0}, {x1 - 3, z1}}) {
			b.fill(post[0], 0, post[1], post[0], 4, post[1], Build.log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
		}
		b.fill(x0, 5, z0, x1, 5, z1, Blocks.DARK_OAK_PLANKS);
		b.fill(x0, 6, z0, x1, 6, z0, Blocks.DARK_OAK_SLAB);
		b.fill(x0, 6, z1, x1, 6, z1, Blocks.DARK_OAK_SLAB);
		// chimney with a smoking campfire
		b.fill(x0 + 1, 0, z0 + 1, x0 + 2, 8, z0 + 2, Blocks.BRICKS);
		b.set(x0 + 1, 9, z0 + 1, Blocks.CAMPFIRE);
		b.set(x0 + 2, 0, z0 + 2, Blocks.BLAST_FURNACE);
		b.set(x0 + 1, 0, z0 + 3, Blocks.FURNACE);
		b.set(x0 + 1, 0, z0 + 5, Blocks.LAVA_CAULDRON);
		b.set(x0 + 1, 0, z0 + 6, Blocks.WATER_CAULDRON);
		b.set(x0 + 1, 0, z1 - 2, Blocks.SMITHING_TABLE);
		b.set(x0 + 1, 0, z1 - 1, Blocks.BARREL);
		b.set(x0 + 6, 0, z0 + 4, Blocks.ANVIL);
		b.set(x0 + 6, 0, z0 + 7, Blocks.GRINDSTONE);
		b.fill(x0 + 4, 4, z0 + 5, x0 + 4, 4, z0 + 5, Blocks.IRON_CHAIN);
		b.lantern(x0 + 4, 3, z0 + 5, true, false);
		b.lantern(x1 - 1, 4, z0 + 2, true, false);
		b.lantern(x1 - 1, 4, z1 - 2, true, false);
		b.set(x1 + 1, 0, z0 + 1, Blocks.BARREL);
		b.set(x1 + 1, 0, z1 - 1, Blocks.BARREL);
		b.flag(x1 + 1, z0 + 5, 5, Build.wool(DyeColor.ORANGE), Build.wool(DyeColor.BLACK), Direction.EAST);
	}

	/** Market stall: four posts, striped awning, counter and the goods (a shop block) at the back. */
	private static void stall(final Build b, final int x0, final int z0, final Block awning, final Block goods) {
		if (!b.touches(x0 - 1, z0 - 1, x0 + 7, z0 + 7)) {
			return;
		}
		for (int[] c : new int[][] {{x0, z0}, {x0 + 6, z0}, {x0, z0 + 6}, {x0 + 6, z0 + 6}}) {
			b.fill(c[0], 0, c[1], c[0], 3, c[1], Blocks.SPRUCE_FENCE);
		}
		for (int x = x0; x <= x0 + 6; x++) {
			for (int z = z0; z <= z0 + 6; z++) {
				b.set(x, 4, z, (x - x0) % 2 == 0 ? awning : Build.wool(DyeColor.WHITE));
			}
		}
		BlockState counter = Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
		b.fill(x0 + 6, 0, z0 + 1, x0 + 6, 0, z0 + 5, Blocks.SPRUCE_PLANKS);
		b.fill(x0 + 6, 1, z0 + 1, x0 + 6, 1, z0 + 5, counter);
		b.set(x0 + 3, 0, z0 + 3, goods);
		b.set(x0 + 1, 0, z0 + 1, Blocks.BARREL);
		b.lantern(x0 + 3, 3, z0 + 3, true, false);
	}

	// ------------------------------------------------------------------ harbor (pirate)

	private static void harbor(final Build b) {
		if (!b.touches(35, 45, 97, 97)) {
			return;
		}
		// quay
		for (int x = 36; x <= 96; x++) {
			for (int z = 46; z <= 57; z++) {
				b.set(x, -1, z, Build.paving(x, z));
			}
		}
		// basin with stone walls and water
		int bx0 = 44;
		int bx1 = 94;
		int bz0 = 58;
		int bz1 = 94;
		b.fill(bx0 - 1, -9, bz0 - 1, bx1 + 1, -1, bz1 + 1, Blocks.STONE_BRICKS);
		b.fill(bx0, -8, bz0, bx1, -8, bz1, Blocks.SAND);
		b.fill(bx0, -7, bz0, bx1, -2, bz1, Blocks.WATER);
		b.air(bx0, -1, bz0, bx1, 12, bz1);
		// piers on log posts
		pier(b, 48, 51, 58, 82);
		pier(b, 64, 67, 58, 76);
		ship(b, 80, 62, 90);
		lighthouse(b, 89, 89);
		// the Drunken Kraken tavern
		Houses.house(b, 74, 46, 92, 56, Style.HARBOR, Direction.WEST, 2);
		for (int x = 77; x <= 89; x += 4) {
			b.set(x, 0, 48, Blocks.BARREL);
		}
		// cargo on the quay
		for (int x = 40; x <= 70; x += 6) {
			b.set(x, 0, 47, Blocks.BARREL);
			b.set(x + 1, 0, 47, Blocks.SPRUCE_PLANKS);
			b.set(x, 1, 47, Blocks.BARREL);
		}
		b.flag(56, 50, 9, Build.wool(DyeColor.YELLOW), Build.wool(DyeColor.BLACK), Direction.EAST);
		b.flag(72, 50, 9, Build.wool(DyeColor.BLACK), Build.wool(DyeColor.WHITE), Direction.WEST);
	}

	private static void pier(final Build b, final int x0, final int x1, final int z0, final int z1) {
		b.fill(x0, -1, z0, x1, -1, z1, Blocks.SPRUCE_PLANKS);
		for (int z = z0; z <= z1; z += 6) {
			b.fill(x0, -8, z, x0, -2, z, Build.log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
			b.fill(x1, -8, z, x1, -2, z, Build.log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
			b.set(x1, 0, z, Blocks.SPRUCE_FENCE);
			b.lantern(x1, 1, z, false, false);
		}
	}

	/** Three-masted pirate ship lying along z with its deck at street level. */
	private static void ship(final Build b, final int cx, final int bow, final int stern) {
		if (!b.touches(cx - 8, bow - 8, cx + 8, stern + 2)) {
			return;
		}
		BlockState hull = Blocks.DARK_OAK_PLANKS.defaultBlockState();
		BlockState deck = Blocks.SPRUCE_PLANKS.defaultBlockState();
		for (int z = bow; z <= stern; z++) {
			int along = z - bow;
			double half = along < 6 ? 1.0 + along * 0.55 : stern - z < 2 ? 3.5 : 4.0;
			for (int dy = -6; dy <= 0; dy++) {
				double w = half - Math.max(0, -dy - 2) * 0.6;
				if (w < 0.5) {
					continue;
				}
				for (int x = (int)Math.floor(cx - w); x <= (int)Math.ceil(cx + w); x++) {
					boolean edge = Math.abs(x - cx) >= w - 0.5 || dy == -6;
					if (dy == 0) {
						b.set(x, dy, z, edge ? hull : deck);
					} else if (edge) {
						b.set(x, dy, z, hull);
					} else {
						b.set(x, dy, z, Blocks.AIR);
					}
				}
			}
			// railing
			int edgeX = (int)Math.ceil(half);
			b.set(cx - edgeX, 1, z, Blocks.DARK_OAK_FENCE);
			b.set(cx + edgeX, 1, z, Blocks.DARK_OAK_FENCE);
			if (along % 6 == 3 && along > 4 && along < 22) {
				b.set(cx - edgeX + 1, 1, z, Blocks.POLISHED_BLACKSTONE);
				b.set(cx + edgeX - 1, 1, z, Blocks.POLISHED_BLACKSTONE);
			}
		}
		b.lantern(cx, -1, bow + 12, true, false);
		// bowsprit
		for (int i = 1; i <= 5; i++) {
			b.set(cx, 1 + i / 2, bow - i, Build.log(Blocks.SPRUCE_LOG, Direction.Axis.Z));
		}
		// masts, yards and sails
		mast(b, cx, bow + 9, 18);
		mast(b, cx, bow + 18, 21);
		// captain's cabin at the stern
		b.walls(cx - 3, 1, stern - 4, cx + 3, 4, stern, Blocks.DARK_OAK_PLANKS);
		b.air(cx - 2, 1, stern - 3, cx + 2, 4, stern - 1);
		b.fill(cx - 3, 5, stern - 4, cx + 3, 5, stern, Blocks.DARK_OAK_SLAB);
		b.fill(cx - 2, 2, stern, cx + 2, 3, stern, Blocks.GLASS);
		b.door(cx, 1, stern - 4, Blocks.DARK_OAK_DOOR, Direction.NORTH);
		b.lantern(cx, 4, stern - 2, true, false);
		// gangplank from the second pier
		BlockState plank = Blocks.SPRUCE_SLAB.defaultBlockState();
		b.fill(68, 0, bow + 10, cx - 5, 0, bow + 11, plank);
	}

	private static void mast(final Build b, final int x, final int z, final int height) {
		b.fill(x, 1, z, x, height, z, Build.log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
		for (int yard : new int[] {7, height - 5}) {
			b.fill(x - 5, yard, z, x + 5, yard, z, Build.log(Blocks.SPRUCE_LOG, Direction.Axis.X));
		}
		int top = height - 6;
		for (int sx = x - 4; sx <= x + 4; sx++) {
			int bulge = Math.abs(sx - x) <= 2 ? 1 : 0;
			b.fill(sx, 8, z + bulge, sx, top, z + bulge, Build.wool(DyeColor.WHITE));
		}
		b.fill(x - 1, height - 1, z - 1, x + 1, height - 1, z + 1, Blocks.SPRUCE_PLANKS);
		b.set(x, height, z, Blocks.SPRUCE_FENCE);
		// jolly roger
		for (int i = 1; i <= 3; i++) {
			b.set(x + i, height, z, Build.wool(DyeColor.BLACK));
			b.set(x + i, height + 1, z, i == 2 ? Build.wool(DyeColor.WHITE) : Build.wool(DyeColor.BLACK));
		}
		b.set(x, height + 1, z, Blocks.SPRUCE_FENCE);
	}

	private static void lighthouse(final Build b, final int cx, final int cz) {
		if (!b.touches(cx - 5, cz - 5, cx + 5, cz + 5)) {
			return;
		}
		b.cylinder(cx, cz, 4, -8, -1, Blocks.STONE_BRICKS, false);
		for (int y = 0; y <= 24; y++) {
			b.cylinder(cx, cz, 3, y, y, (y / 4) % 2 == 0 ? Build.concrete(DyeColor.WHITE) : Build.concrete(DyeColor.RED), true);
		}
		b.cylinder(cx, cz, 2, 0, 24, Blocks.AIR, false);
		b.cylinder(cx, cz, 4, 25, 25, Blocks.STONE_BRICKS, false);
		b.cylinder(cx, cz, 3, 26, 28, Blocks.GLASS, true);
		b.cylinder(cx, cz, 2, 26, 28, Blocks.AIR, false);
		b.set(cx, 26, cz, Blocks.SEA_LANTERN);
		b.set(cx, 27, cz, Blocks.GLOWSTONE);
		b.cone(cx + 0.0, cz + 0.0, 3.5, 29, 0.8, Build.concrete(DyeColor.RED).defaultBlockState());
		b.air(cx - 3, 0, cz, cx - 3, 1, cz);
	}

	private CitySouth() {
	}
}
