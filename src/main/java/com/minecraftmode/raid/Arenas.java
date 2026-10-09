package com.minecraftmode.raid;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Builds a raid arena in the raid dimension: a floating disc of {@link #RADIUS} blocks with a rim
 * wall, eight 3x3 pillars with lights, hidden light blocks over the floor and the theme's own
 * features. Arenas are rebuilt (cleared first) every time an instance starts, so a fight never sees
 * what the last one left behind.
 */
public final class Arenas {
	public static final int RADIUS = 21;
	/** Kraken's pool radius (SUNKEN_SHIP). */
	public static final int POOL_RADIUS = 4;
	private static final int PILLAR_HEIGHT = 8;
	private static final int UPDATE = Block.UPDATE_CLIENTS;

	/** Where players arrive: the south edge, facing the boss. */
	public static Vec3 playerSpawn(final BlockPos center, final int index) {
		double x = center.getX() + 0.5 + (index % 3 - 1) * 2.0;
		double z = center.getZ() + 0.5 + RADIUS - 5 + index / 3 * 2.0;
		return new Vec3(x, center.getY(), z);
	}

	public static Vec3 bossSpawn(final BlockPos center, final ArenaTheme theme) {
		return theme == ArenaTheme.SUNKEN_SHIP ? Vec3.atBottomCenterOf(center).add(0, -1.5, 0) : Vec3.atBottomCenterOf(center).add(0, 0, -5);
	}

	public static boolean inside(final BlockPos center, final Vec3 pos, final double margin) {
		double dx = pos.x - center.getX() - 0.5;
		double dz = pos.z - center.getZ() - 0.5;
		return dx * dx + dz * dz <= (RADIUS + margin) * (RADIUS + margin) && pos.y > center.getY() - 8 && pos.y < center.getY() + 40;
	}

	public static void build(final ServerLevel level, final BlockPos center, final ArenaTheme theme, final RandomSource random) {
		clear(level, center);
		int cx = center.getX();
		int cy = center.getY();
		int cz = center.getZ();
		for (int x = -RADIUS - 1; x <= RADIUS + 1; x++) {
			for (int z = -RADIUS - 1; z <= RADIUS + 1; z++) {
				double d = Math.sqrt(x * x + z * z);
				if (d > RADIUS + 0.5) {
					continue;
				}
				set(level, cx + x, cy - 1, cz + z, pick(theme.floor(), random));
				int depth = 2 + (int)((1.0 - Math.pow(d / (RADIUS + 1.0), 2)) * 11) + random.nextInt(2);
				for (int dy = 2; dy <= depth; dy++) {
					set(level, cx + x, cy - dy, cz + z, pick(theme.underside(), random));
				}
				if (d > RADIUS - 0.5) {
					for (int h = 0; h < theme.rimHeight(); h++) {
						set(level, cx + x, cy + h, cz + z, theme.rim().defaultBlockState());
					}
					set(level, cx + x, cy + theme.rimHeight(), cz + z, theme.rimTop().defaultBlockState());
				}
			}
		}
		for (int k = 0; k < 8; k++) {
			double a = Math.PI / 4 * k + Math.PI / 8;
			int px = cx + (int)Math.round(Math.cos(a) * (RADIUS - 4));
			int pz = cz + (int)Math.round(Math.sin(a) * (RADIUS - 4));
			pillar(level, px, cy, pz, theme);
		}
		for (int x = -RADIUS + 3; x <= RADIUS - 3; x += 7) {
			for (int z = -RADIUS + 3; z <= RADIUS - 3; z += 7) {
				BlockPos p = new BlockPos(cx + x, cy + 5, cz + z);
				if (level.getBlockState(p).isAir()) {
					level.setBlock(p, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), UPDATE);
				}
			}
		}
		switch (theme) {
			case WEB_CAVE -> webCave(level, center, random);
			case MOUNTAIN -> mountain(level, center, random);
			case SUNKEN_SHIP -> sunkenShip(level, center, random);
			case VOLCANO -> volcano(level, center, random);
			case NECROPOLIS -> necropolis(level, center, random);
			case VOID_ISLES -> voidIsles(level, center, random);
		}
	}

	/** Removes everything in the arena's box (also leftovers of other themes). */
	public static void clear(final ServerLevel level, final BlockPos center) {
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int x = -RADIUS - 12; x <= RADIUS + 12; x++) {
			for (int z = -RADIUS - 12; z <= RADIUS + 12; z++) {
				for (int y = -16; y <= 26; y++) {
					p.set(center.getX() + x, center.getY() + y, center.getZ() + z);
					if (!level.getBlockState(p).isAir()) {
						level.setBlock(p, air, UPDATE);
					}
				}
			}
		}
	}

	private static void pillar(final ServerLevel level, final int px, final int cy, final int pz, final ArenaTheme theme) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				boolean corner = dx != 0 && dz != 0;
				int top = corner ? PILLAR_HEIGHT - 1 : PILLAR_HEIGHT;
				for (int y = 0; y < top; y++) {
					Block block = y == 0 || y == top - 1 ? theme.rim() : theme.pillar();
					set(level, px + dx, cy + y, pz + dz, block.defaultBlockState());
				}
			}
		}
		set(level, px, cy + PILLAR_HEIGHT, pz, theme.light().defaultBlockState());
	}

	// ------------------------------------------------------------------ themes

	private static void webCave(final ServerLevel level, final BlockPos c, final RandomSource random) {
		forRing(c, RADIUS - 2.5, RADIUS - 0.5, (x, z) -> {
			if (random.nextFloat() < 0.18F) {
				set(level, x, c.getY(), z, Blocks.COBWEB.defaultBlockState());
			}
		});
		forRing(c, RADIUS - 0.5, RADIUS + 0.5, (x, z) -> {
			if (random.nextFloat() < 0.5F) {
				set(level, x, c.getY() + ArenaTheme.WEB_CAVE.rimHeight() + 1, z, Blocks.COBWEB.defaultBlockState());
			}
		});
		// stone teeth outside the rim
		for (int i = 0; i < 18; i++) {
			double a = random.nextDouble() * Math.PI * 2;
			int r = RADIUS + 2 + random.nextInt(4);
			int x = c.getX() + (int)(Math.cos(a) * r);
			int z = c.getZ() + (int)(Math.sin(a) * r);
			int h = 4 + random.nextInt(9);
			for (int y = -3; y < h; y++) {
				set(level, x, c.getY() + y, z, (y > h - 3 ? Blocks.TUFF : Blocks.DEEPSLATE).defaultBlockState());
			}
		}
	}

	private static void mountain(final ServerLevel level, final BlockPos c, final RandomSource random) {
		forDisc(c, RADIUS - 1.0, (x, z) -> {
			if (random.nextFloat() < 0.25F && level.getBlockState(new BlockPos(x, c.getY(), z)).isAir()) {
				set(level, x, c.getY(), z, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1 + random.nextInt(2)));
			}
		});
		for (int i = 0; i < 12; i++) {
			double a = random.nextDouble() * Math.PI * 2;
			int r = RADIUS + 3 + random.nextInt(5);
			int x = c.getX() + (int)(Math.cos(a) * r);
			int z = c.getZ() + (int)(Math.sin(a) * r);
			int h = 6 + random.nextInt(10);
			for (int y = -4; y < h; y++) {
				set(level, x, c.getY() + y, z, (y > h - 4 ? Blocks.PACKED_ICE : Blocks.STONE).defaultBlockState());
				if (y < h - 5) {
					set(level, x + 1, c.getY() + y, z, Blocks.STONE.defaultBlockState());
					set(level, x, c.getY() + y, z + 1, Blocks.ANDESITE.defaultBlockState());
				}
			}
		}
	}

	private static void sunkenShip(final ServerLevel level, final BlockPos c, final RandomSource random) {
		forDisc(c, POOL_RADIUS + 1.5, (x, z) -> set(level, x, c.getY() - 1, z, Blocks.PRISMARINE_BRICKS.defaultBlockState()));
		forDisc(c, POOL_RADIUS + 0.5, (x, z) -> {
			for (int dy = 1; dy <= 4; dy++) {
				set(level, x, c.getY() - dy, z, Blocks.WATER.defaultBlockState());
			}
			set(level, x, c.getY() - 5, z, (random.nextFloat() < 0.2F ? Blocks.TUBE_CORAL_BLOCK : Blocks.SAND).defaultBlockState());
		});
		// two masts with furled sails
		for (int side = -1; side <= 1; side += 2) {
			int mx = c.getX() + side * 13;
			for (int y = 0; y < 16; y++) {
				set(level, mx, c.getY() + y, c.getZ(), Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState());
			}
			for (int dz = -4; dz <= 4; dz++) {
				set(level, mx, c.getY() + 11, c.getZ() + dz, Blocks.SPRUCE_FENCE.defaultBlockState());
				set(level, mx, c.getY() + 12, c.getZ() + dz, Blocks.WOOL.white().defaultBlockState());
			}
			set(level, mx, c.getY() + 16, c.getZ(), Blocks.LANTERN.defaultBlockState());
		}
	}

	private static void volcano(final ServerLevel level, final BlockPos c, final RandomSource random) {
		forRing(c, RADIUS - 1.5, RADIUS - 0.5, (x, z) -> {
			if (random.nextFloat() < 0.35F) {
				set(level, x, c.getY() - 1, z, Blocks.MAGMA_BLOCK.defaultBlockState());
			}
		});
		// crater wall: basalt spires around the rim, getting taller outward
		for (int i = 0; i < 40; i++) {
			double a = Math.PI * 2 * i / 40 + random.nextDouble() * 0.1;
			int r = RADIUS + 2 + random.nextInt(4);
			int x = c.getX() + (int)(Math.cos(a) * r);
			int z = c.getZ() + (int)(Math.sin(a) * r);
			int h = 5 + random.nextInt(12);
			for (int y = -4; y < h; y++) {
				set(level, x, c.getY() + y, z, (y == h - 1 && random.nextBoolean() ? Blocks.SHROOMLIGHT : Blocks.BASALT).defaultBlockState());
			}
		}
	}

	private static void necropolis(final ServerLevel level, final BlockPos c, final RandomSource random) {
		BlockState candles = Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true);
		forRing(c, 8.5, 9.5, (x, z) -> set(level, x, c.getY() - 1, z, Blocks.SOUL_SOIL.defaultBlockState()));
		for (int k = 0; k < 16; k++) {
			double a = Math.PI * 2 * k / 16;
			set(level, c.getX() + (int)Math.round(Math.cos(a) * 9), c.getY(), c.getZ() + (int)Math.round(Math.sin(a) * 9), candles);
		}
		forRing(c, RADIUS - 1.5, RADIUS - 0.5, (x, z) -> {
			if (random.nextFloat() < 0.12F) {
				set(level, x, c.getY(), z, candles);
			}
		});
		// tombstones outside the rim
		for (int i = 0; i < 24; i++) {
			double a = random.nextDouble() * Math.PI * 2;
			int r = RADIUS + 2 + random.nextInt(5);
			int x = c.getX() + (int)(Math.cos(a) * r);
			int z = c.getZ() + (int)(Math.sin(a) * r);
			set(level, x, c.getY() - 1, z, Blocks.SOUL_SOIL.defaultBlockState());
			set(level, x, c.getY(), z, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
			set(level, x, c.getY() + 1, z, Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
		}
	}

	private static void voidIsles(final ServerLevel level, final BlockPos c, final RandomSource random) {
		forRing(c, 4.5, 5.5, (x, z) -> set(level, x, c.getY() - 1, z, Blocks.CRYING_OBSIDIAN.defaultBlockState()));
		// floating shards around the island
		for (int i = 0; i < 14; i++) {
			double a = random.nextDouble() * Math.PI * 2;
			int r = RADIUS + 4 + random.nextInt(7);
			int x = c.getX() + (int)(Math.cos(a) * r);
			int z = c.getZ() + (int)(Math.sin(a) * r);
			int y = c.getY() - 6 + random.nextInt(20);
			int size = 1 + random.nextInt(2);
			for (int dx = -size; dx <= size; dx++) {
				for (int dz = -size; dz <= size; dz++) {
					for (int dy = -size; dy <= 0; dy++) {
						if (Math.abs(dx) + Math.abs(dz) - dy <= size + 1) {
							set(level, x + dx, y + dy, z + dz, (random.nextFloat() < 0.3F ? Blocks.OBSIDIAN : Blocks.END_STONE).defaultBlockState());
						}
					}
				}
			}
			set(level, x, y + 1, z, Blocks.END_ROD.defaultBlockState());
		}
	}

	// ------------------------------------------------------------------ helpers

	private interface Column {
		void at(int x, int z);
	}

	private static void forDisc(final BlockPos c, final double radius, final Column column) {
		forRing(c, -1.0, radius, column);
	}

	private static void forRing(final BlockPos c, final double inner, final double outer, final Column column) {
		int r = Mth.ceil(outer);
		for (int x = -r; x <= r; x++) {
			for (int z = -r; z <= r; z++) {
				double d = Math.sqrt(x * x + z * z);
				if (d > inner && d <= outer) {
					column.at(c.getX() + x, c.getZ() + z);
				}
			}
		}
	}

	private static BlockState pick(final List<Block> blocks, final RandomSource random) {
		return blocks.get(random.nextInt(blocks.size())).defaultBlockState();
	}

	private static void set(final ServerLevel level, final int x, final int y, final int z, final BlockState state) {
		level.setBlock(new BlockPos(x, y, z), state, UPDATE);
	}

	private Arenas() {
	}
}
