package com.minecraftmode.city;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * Block placement for the city, clipped to the chunk being generated. Coordinates are absolute
 * x/z and {@code dy} relative to the city floor: dy = 0 is the first air layer, dy = -1 the
 * ground. Every builder can draw whole buildings; only the part inside the current chunk lands.
 */
public final class Build {
	private final WorldGenLevel level;
	public final int base;
	private final int minX;
	private final int maxX;
	private final int minZ;
	private final int maxZ;
	private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

	Build(final WorldGenLevel level, final int base, final int minX, final int minZ) {
		this.level = level;
		this.base = base;
		this.minX = minX;
		this.maxX = minX + 15;
		this.minZ = minZ;
		this.maxZ = minZ + 15;
	}

	public boolean touches(final int x0, final int z0, final int x1, final int z1) {
		return Math.max(x0, x1) >= this.minX && Math.min(x0, x1) <= this.maxX && Math.max(z0, z1) >= this.minZ && Math.min(z0, z1) <= this.maxZ;
	}

	public boolean inside(final int x, final int z) {
		return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
	}

	public void set(final int x, final int dy, final int z, final BlockState state) {
		if (this.inside(x, z)) {
			this.level.setBlock(this.pos.set(x, this.base + dy, z), state, Block.UPDATE_CLIENTS);
		}
	}

	public void set(final int x, final int dy, final int z, final Block block) {
		this.set(x, dy, z, block.defaultBlockState());
	}

	public BlockState get(final int x, final int dy, final int z) {
		return this.level.getBlockState(this.pos.set(x, this.base + dy, z));
	}

	public void fill(final int x0, final int dy0, final int z0, final int x1, final int dy1, final int z1, final BlockState state) {
		int ax = Math.max(Math.min(x0, x1), this.minX);
		int bx = Math.min(Math.max(x0, x1), this.maxX);
		int az = Math.max(Math.min(z0, z1), this.minZ);
		int bz = Math.min(Math.max(z0, z1), this.maxZ);
		for (int x = ax; x <= bx; x++) {
			for (int z = az; z <= bz; z++) {
				for (int y = Math.min(dy0, dy1); y <= Math.max(dy0, dy1); y++) {
					this.set(x, y, z, state);
				}
			}
		}
	}

	public void fill(final int x0, final int dy0, final int z0, final int x1, final int dy1, final int z1, final Block block) {
		this.fill(x0, dy0, z0, x1, dy1, z1, block.defaultBlockState());
	}

	public void air(final int x0, final int dy0, final int z0, final int x1, final int dy1, final int z1) {
		this.fill(x0, dy0, z0, x1, dy1, z1, Blocks.AIR);
	}

	/** Four walls of a box (no floor or ceiling). */
	public void walls(final int x0, final int dy0, final int z0, final int x1, final int dy1, final int z1, final BlockState state) {
		this.fill(x0, dy0, z0, x1, dy1, z0, state);
		this.fill(x0, dy0, z1, x1, dy1, z1, state);
		this.fill(x0, dy0, z0, x0, dy1, z1, state);
		this.fill(x1, dy0, z0, x1, dy1, z1, state);
	}

	public void walls(final int x0, final int dy0, final int z0, final int x1, final int dy1, final int z1, final Block block) {
		this.walls(x0, dy0, z0, x1, dy1, z1, block.defaultBlockState());
	}

	/** Solid or hollow vertical cylinder. */
	public void cylinder(final double cx, final double cz, final double r, final int dy0, final int dy1, final BlockState state, final boolean hollow) {
		int x0 = (int)Math.floor(cx - r - 1);
		int x1 = (int)Math.ceil(cx + r + 1);
		int z0 = (int)Math.floor(cz - r - 1);
		int z1 = (int)Math.ceil(cz + r + 1);
		if (!this.touches(x0, z0, x1, z1)) {
			return;
		}
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				double d = Math.hypot(x - cx, z - cz);
				if (d <= r + 0.5 && (!hollow || d > r - 0.5)) {
					for (int y = dy0; y <= dy1; y++) {
						this.set(x, y, z, state);
					}
				}
			}
		}
	}

	public void cylinder(final double cx, final double cz, final double r, final int dy0, final int dy1, final Block block, final boolean hollow) {
		this.cylinder(cx, cz, r, dy0, dy1, block.defaultBlockState(), hollow);
	}

	/** Pointed roof: rings shrinking by {@code step} per layer. */
	public void cone(final double cx, final double cz, final double r, final int dy, final double step, final BlockState state) {
		double radius = r;
		int y = dy;
		while (radius > 0.2) {
			this.cylinder(cx, cz, radius, y, y, state, radius > 1.5);
			radius -= step;
			y++;
		}
		this.set((int)Math.floor(cx), y, (int)Math.floor(cz), state);
	}

	/** Hip roof over a rectangle: stairs around each layer, shrinking inward until it closes. */
	public void hipRoof(final int x0, final int z0, final int x1, final int z1, final int dy, final Block stairs, final Block fill) {
		int ax = Math.min(x0, x1) - 1;
		int bx = Math.max(x0, x1) + 1;
		int az = Math.min(z0, z1) - 1;
		int bz = Math.max(z0, z1) + 1;
		int y = dy;
		while (ax <= bx && az <= bz) {
			if (ax == bx || az == bz) {
				this.fill(ax, y, az, bx, y, bz, fill);
				break;
			}
			for (int x = ax; x <= bx; x++) {
				this.set(x, y, az, stairs(stairs, Direction.SOUTH));
				this.set(x, y, bz, stairs(stairs, Direction.NORTH));
			}
			for (int z = az + 1; z < bz; z++) {
				this.set(ax, y, z, stairs(stairs, Direction.EAST));
				this.set(bx, y, z, stairs(stairs, Direction.WEST));
			}
			this.fill(ax + 1, y, az + 1, bx - 1, y, bz - 1, fill);
			ax++;
			bx--;
			az++;
			bz--;
			y++;
		}
	}

	/** Gable roof along x (ridge parallel to x) or along z. */
	public void gableRoof(final int x0, final int z0, final int x1, final int z1, final int dy, final Block stairs, final Block gableWall, final boolean alongX) {
		int ax = Math.min(x0, x1);
		int bx = Math.max(x0, x1);
		int az = Math.min(z0, z1);
		int bz = Math.max(z0, z1);
		if (alongX) {
			int lo = az - 1;
			int hi = bz + 1;
			int y = dy;
			while (lo <= hi) {
				for (int x = ax - 1; x <= bx + 1; x++) {
					if (lo == hi) {
						this.set(x, y, lo, slabFor(stairs));
					} else {
						this.set(x, y, lo, stairs(stairs, Direction.SOUTH));
						this.set(x, y, hi, stairs(stairs, Direction.NORTH));
					}
				}
				if (hi - lo > 1) {
					this.fill(ax, y, lo + 1, ax, y, hi - 1, gableWall);
					this.fill(bx, y, lo + 1, bx, y, hi - 1, gableWall);
				}
				lo++;
				hi--;
				y++;
			}
		} else {
			int lo = ax - 1;
			int hi = bx + 1;
			int y = dy;
			while (lo <= hi) {
				for (int z = az - 1; z <= bz + 1; z++) {
					if (lo == hi) {
						this.set(lo, y, z, slabFor(stairs));
					} else {
						this.set(lo, y, z, stairs(stairs, Direction.EAST));
						this.set(hi, y, z, stairs(stairs, Direction.WEST));
					}
				}
				if (hi - lo > 1) {
					this.fill(lo + 1, y, az, hi - 1, y, az, gableWall);
					this.fill(lo + 1, y, bz, hi - 1, y, bz, gableWall);
				}
				lo++;
				hi--;
				y++;
			}
		}
	}

	private static BlockState slabFor(final Block stairs) {
		if (stairs == Blocks.DARK_PRISMARINE_STAIRS) {
			return Blocks.DARK_PRISMARINE_SLAB.defaultBlockState();
		}
		if (stairs == Blocks.DARK_OAK_STAIRS) {
			return Blocks.DARK_OAK_SLAB.defaultBlockState();
		}
		if (stairs == Blocks.SPRUCE_STAIRS) {
			return Blocks.SPRUCE_SLAB.defaultBlockState();
		}
		if (stairs == Blocks.PURPUR_STAIRS) {
			return Blocks.PURPUR_SLAB.defaultBlockState();
		}
		if (stairs == Blocks.DEEPSLATE_TILE_STAIRS) {
			return Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState();
		}
		if (stairs == Blocks.STONE_BRICK_STAIRS) {
			return Blocks.STONE_BRICK_SLAB.defaultBlockState();
		}
		return Blocks.SMOOTH_STONE_SLAB.defaultBlockState();
	}

	/** Stairs whose high side faces {@code facing}'s opposite (vanilla convention: facing = the direction you walk up). */
	public static BlockState stairs(final Block stairs, final Direction facing) {
		return stairs.defaultBlockState().setValue(StairBlock.FACING, facing);
	}

	public static BlockState upsideDown(final Block stairs, final Direction facing) {
		return stairs(stairs, facing).setValue(StairBlock.HALF, Half.TOP);
	}

	public static BlockState log(final Block log, final Direction.Axis axis) {
		return log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
	}

	public void door(final int x, final int dy, final int z, final Block door, final Direction facing) {
		BlockState state = door.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
		this.set(x, dy, z, state.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		this.set(x, dy + 1, z, state.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
	}

	public void lantern(final int x, final int dy, final int z, final boolean hanging, final boolean soul) {
		this.set(x, dy, z, (soul ? Blocks.SOUL_LANTERN : Blocks.LANTERN).defaultBlockState().setValue(LanternBlock.HANGING, hanging));
	}

	/** Street lamp: stone base, fence post, lantern on top. */
	public void lampPost(final int x, final int z, final boolean soul) {
		if (!this.inside(x, z)) {
			return;
		}
		this.set(x, 0, z, Blocks.POLISHED_ANDESITE);
		this.fill(x, 1, z, x, 3, z, Blocks.DARK_OAK_FENCE);
		this.lantern(x, 4, z, false, soul);
	}

	/** Banner pole with a 2x3 cloth flag. */
	public void flag(final int x, final int z, final int height, final Block cloth, final Block stripe, final Direction toward) {
		this.fill(x, 0, z, x, height, z, Blocks.SPRUCE_FENCE);
		for (int i = 1; i <= 3; i++) {
			int fx = x + toward.getStepX() * i;
			int fz = z + toward.getStepZ() * i;
			this.set(fx, height, fz, i == 3 ? stripe : cloth);
			this.set(fx, height - 1, fz, cloth);
			this.set(fx, height - 2, fz, i == 2 ? stripe : cloth);
		}
	}

	/** Simple round tree. */
	public void tree(final int x, final int z, final int height, final Block log, final Block leaves) {
		BlockState leaf = leaves.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		int top = height;
		for (int dy = top - 3; dy <= top + 1; dy++) {
			double r = dy <= top - 1 ? 2.6 : 1.6;
			for (int lx = -3; lx <= 3; lx++) {
				for (int lz = -3; lz <= 3; lz++) {
					if (Math.hypot(lx, lz) <= r && hash(x + lx, dy, z + lz) % 7 != 0) {
						this.set(x + lx, dy, z + lz, leaf);
					}
				}
			}
		}
		this.fill(x, 0, z, x, top - 1, z, log);
	}

	public static Block wool(final DyeColor color) {
		return Blocks.WOOL.pick(color);
	}

	public static Block glass(final DyeColor color) {
		return Blocks.STAINED_GLASS.pick(color);
	}

	public static Block concrete(final DyeColor color) {
		return Blocks.CONCRETE.pick(color);
	}

	public static Block terracotta(final DyeColor color) {
		return Blocks.DYED_TERRACOTTA.pick(color);
	}

	public static Block carpet(final DyeColor color) {
		return Blocks.CARPET.pick(color);
	}

	public static Block candle(final DyeColor color) {
		return Blocks.DYED_CANDLE.pick(color);
	}

	/** Deterministic noise for material variety. */
	public static int hash(final int x, final int y, final int z) {
		int h = x * 73856093 ^ y * 19349663 ^ z * 83492791;
		h ^= h >>> 13;
		h *= 0x5bd1e995;
		h ^= h >>> 15;
		return h & 0x7FFFFFFF;
	}

	/** Paving with a few cracked and mossy bricks. */
	public static BlockState paving(final int x, final int z) {
		int h = hash(x, 0, z) % 20;
		return (h == 0 ? Blocks.CRACKED_STONE_BRICKS : h == 1 ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS).defaultBlockState();
	}
}
