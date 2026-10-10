package com.minecraftmode.city;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The homestead plains: open, flat land east of the capital's walls where players build their homes (beds, farms, storage). The
 * generator flattens it to the city floor like the city itself ({@code CityGenerator}), with no trees or structures; a gravel road
 * leads there from the east gate to a travel circle in the middle that goes back to the plaza (and the plaza's circle comes here,
 * see {@link CityFixtures#useWaystone}). It is part of the outskirts: no hostile mobs spawn on its surface. Building is allowed.
 * Worlds generated before it existed keep their terrain there (the travel circles still work).
 */
public final class Homestead {
	/** West edge: right where the city's flattened apron ends. */
	public static final int X0 = CityZone.CORE + 1;
	public static final int X1 = 272;
	/** North-south half width. */
	public static final int HALF_Z = 80;
	/** The middle, with the travel circle. */
	public static final int CENTER_X = 192;
	public static final int CENTER_Z = 0;

	public static boolean inside(final int x, final int z) {
		return x >= X0 && x <= X1 && Math.abs(z) <= HALF_Z;
	}

	/** Chebyshev distance from x, z to the plains (0 inside). */
	public static int distance(final int x, final int z) {
		int dx = Math.max(Math.max(X0 - x, x - X1), 0);
		int dz = Math.max(Math.abs(z) - HALF_Z, 0);
		return Math.max(dx, dz);
	}

	/** Chebyshev distance from the plains to the nearest column of the box x0..x1, z0..z1 (0 when they overlap). */
	public static int distance(final int x0, final int z0, final int x1, final int z1) {
		int dx = Math.max(Math.max(X0 - x1, x0 - X1), 0);
		int dz = Math.max(Math.max(-HALF_Z - z1, z0 - HALF_Z), 0);
		return Math.max(dx, dz);
	}

	public static BlockPos waystone(final int base) {
		return new BlockPos(CENTER_X, base, CENTER_Z);
	}

	/** Where the plaza's travel circle drops players: just south of the homestead's circle, on top of whatever is there. */
	public static Vec3 arrival(final ServerLevel level) {
		return CityFixtures.surface(level, CENTER_X, CENTER_Z + 3);
	}

	/** The road from the east gate and the circle in the middle, clipped to {@code b}'s chunk. */
	static void build(final Build b) {
		if (b.touches(X0, -2, CENTER_X, 2)) {
			b.fill(X0, -1, -1, CENTER_X - 4, -1, 1, Blocks.GRAVEL);
		}
		if (!b.touches(CENTER_X - 4, CENTER_Z - 4, CENTER_X + 4, CENTER_Z + 4)) {
			return;
		}
		b.fill(CENTER_X - 3, -1, CENTER_Z - 3, CENTER_X + 3, -1, CENTER_Z + 3, Blocks.POLISHED_ANDESITE);
		b.walls(CENTER_X - 3, -1, CENTER_Z - 3, CENTER_X + 3, -1, CENTER_Z + 3, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
		for (int[] c : new int[][] {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
			b.lampPost(CENTER_X + c[0], CENTER_Z + c[1], false);
		}
		CityFixtures.waystone(b, CENTER_X, CENTER_Z);
	}

	/**
	 * Puts back the homestead circle when it is missing, but only where the plains were generated flat (the platform is there): in an
	 * older world the circle would float or sit underground, and arrivals use the surface instead.
	 */
	static void ensure(final ServerLevel level) {
		int base = CityZone.baseY(level);
		BlockPos stone = waystone(base);
		if (!level.isLoaded(stone) || level.getBlockState(stone).is(Blocks.LODESTONE)
			|| !level.getBlockState(stone.offset(1, -1, 0)).is(Blocks.POLISHED_ANDESITE)) {
			return;
		}
		build(new Build(level, base, (CENTER_X >> 4) << 4, (CENTER_Z >> 4) << 4));
	}

	private Homestead() {
	}
}
