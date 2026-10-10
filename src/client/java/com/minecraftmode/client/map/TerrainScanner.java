package com.minecraftmode.client.map;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * Colours chunks the client has loaded the way vanilla maps do: the top block's map colour, brighter when the column is higher
 * than its northern neighbour and darker when lower, water darkening with depth. Under a ceiling (the Nether) the scan starts a
 * little above the player instead of at the bedrock roof.
 */
final class TerrainScanner {
	/** Chunks scanned per client tick. */
	private static final int PER_TICK = 2;
	/** A chunk is scanned again after this many ticks (blocks change). */
	private static final int RESCAN = 200;
	/** Chunk radius around the player that is kept current. */
	private static final int RADIUS = 8;

	private int spiral;

	/** Scans up to {@link #PER_TICK} chunks near {@code center}, nearest first, that are loaded and due. */
	void tick(final ClientLevel level, final MapData data, final BlockPos center, final long now, final MapTexture... textures) {
		int pcx = center.getX() >> 4;
		int pcz = center.getZ() >> 4;
		int done = 0;
		int cells = (2 * RADIUS + 1) * (2 * RADIUS + 1);
		for (int step = 0; step < cells && done < PER_TICK; step++) {
			int i = (this.spiral + step) % cells;
			int dx = i % (2 * RADIUS + 1) - RADIUS;
			int dz = i / (2 * RADIUS + 1) - RADIUS;
			int cx = pcx + dx;
			int cz = pcz + dz;
			if (!level.hasChunk(cx, cz) || now - data.scannedAt(cx, cz) < RESCAN) {
				continue;
			}
			int[] colors = scan(level, cx, cz, center.getY());
			data.put(cx, cz, colors, now);
			for (MapTexture texture : textures) {
				texture.paint(cx, cz, colors);
			}
			done++;
			this.spiral = (i + 1) % cells;
		}
	}

	static int[] scan(final ClientLevel level, final int cx, final int cz, final int eyeY) {
		int[] out = new int[256];
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		boolean ceiling = level.dimensionType().hasCeiling();
		for (int z = 0; z < 16; z++) {
			for (int x = 0; x < 16; x++) {
				int bx = cx << 4 | x;
				int bz = cz << 4 | z;
				int top = top(level, pos, bx, bz, ceiling, eyeY);
				if (top < level.getMinY()) {
					continue;
				}
				BlockState state = level.getBlockState(pos.set(bx, top, bz));
				MapColor color = state.getMapColor(level, pos);
				int steps = 0;
				while (color == MapColor.NONE && top > level.getMinY() && steps++ < 12) {
					top--;
					state = level.getBlockState(pos.set(bx, top, bz));
					color = state.getMapColor(level, pos);
				}
				if (color == MapColor.NONE) {
					continue;
				}
				MapColor.Brightness brightness;
				if (state.is(Blocks.WATER) || !state.getFluidState().isEmpty() && color == MapColor.WATER) {
					int depth = 0;
					while (depth < 10 && top - depth - 1 > level.getMinY() && level.getBlockState(pos.set(bx, top - depth - 1, bz)).is(Blocks.WATER)) {
						depth++;
					}
					brightness = depth < 2 ? MapColor.Brightness.HIGH : depth < 5 ? MapColor.Brightness.NORMAL : MapColor.Brightness.LOW;
				} else {
					int north = top(level, pos, bx, bz - 1, ceiling, eyeY);
					brightness = north < top ? MapColor.Brightness.HIGH : north > top ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
				}
				out[z << 4 | x] = ARGB.opaque(color.calculateARGBColor(brightness));
			}
		}
		return out;
	}

	/** Y of the highest block that blocks motion (or, under a ceiling, the first solid block below the player's head). */
	private static int top(final ClientLevel level, final BlockPos.MutableBlockPos pos, final int x, final int z, final boolean ceiling, final int eyeY) {
		if (!level.hasChunk(x >> 4, z >> 4)) {
			return level.getMinY() - 1;
		}
		if (!ceiling) {
			return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
		}
		int y = Math.min(level.getMaxY(), eyeY + 8);
		while (y > level.getMinY() && !level.getBlockState(pos.set(x, y, z)).isAir()) {
			y--; // start inside the open space around the player, not in the roof
		}
		while (y > level.getMinY() && level.getBlockState(pos.set(x, y, z)).isAir()) {
			y--;
		}
		return y;
	}
}
