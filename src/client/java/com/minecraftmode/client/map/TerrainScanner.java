package com.minecraftmode.client.map;

import java.util.Arrays;
import java.util.Comparator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * Colours chunks the client has loaded the way vanilla maps do: the top block's map colour, brighter when the column is higher
 * than its northern neighbour and darker when lower, water darkening with depth. Under a ceiling (the Nether) the scan starts a
 * little above the player instead of at the bedrock roof.
 */
final class TerrainScanner {
	/** Chunks never seen before scanned per client tick, nearest first: a fast mount crosses about one chunk row a second. */
	private static final int NEW_PER_TICK = 6;
	/** Chunks already on the map scanned again per client tick (blocks change). */
	private static final int RESCAN_PER_TICK = 2;
	/** A chunk is scanned again after this many ticks. */
	private static final int RESCAN = 200;
	/** Chunk radius around the player that is kept current. */
	private static final int RADIUS = 8;
	/** Chunk offsets {dx, dz} within {@link #RADIUS}, nearest first. */
	private static final int[][] CELLS = cells();

	private int cursor;

	/**
	 * Scans chunks near {@code center} that are loaded: first up to {@link #NEW_PER_TICK} that have no colours yet, nearest first,
	 * then up to {@link #RESCAN_PER_TICK} that are due again. Until 0.5.51 one shared budget of 2 a tick went mostly to rescans
	 * in a row-major sweep, so a fast mount left black chunks next to the player.
	 */
	void tick(final ClientLevel level, final MapData data, final BlockPos center, final long now, final MapTexture... textures) {
		int pcx = center.getX() >> 4;
		int pcz = center.getZ() >> 4;
		int fresh = 0;
		for (int[] cell : CELLS) {
			if (fresh >= NEW_PER_TICK) {
				break;
			}
			int cx = pcx + cell[0];
			int cz = pcz + cell[1];
			if (data.chunk(cx, cz) == null && loaded(level, cx, cz)) {
				this.scan(level, data, cx, cz, center.getY(), now, textures);
				fresh++;
			}
		}
		int again = 0;
		for (int step = 0; step < CELLS.length && again < RESCAN_PER_TICK; step++) {
			int i = (this.cursor + step) % CELLS.length;
			int cx = pcx + CELLS[i][0];
			int cz = pcz + CELLS[i][1];
			if (!loaded(level, cx, cz) || now - data.scannedAt(cx, cz) < RESCAN) {
				continue;
			}
			this.scan(level, data, cx, cz, center.getY(), now, textures);
			again++;
			this.cursor = (i + 1) % CELLS.length;
		}
	}

	private void scan(final ClientLevel level, final MapData data, final int cx, final int cz, final int eyeY, final long now, final MapTexture[] textures) {
		byte[] colors = scan(level, cx, cz, eyeY);
		data.put(cx, cz, colors, now);
		for (MapTexture texture : textures) {
			texture.paint(cx, cz, colors);
		}
	}

	private static int[][] cells() {
		int side = 2 * RADIUS + 1;
		int[][] out = new int[side * side][];
		int n = 0;
		for (int dz = -RADIUS; dz <= RADIUS; dz++) {
			for (int dx = -RADIUS; dx <= RADIUS; dx++) {
				out[n++] = new int[] {dx, dz};
			}
		}
		Arrays.sort(out, Comparator.comparingInt(c -> c[0] * c[0] + c[1] * c[1]));
		return out;
	}

	static byte[] scan(final ClientLevel level, final int cx, final int cz, final int eyeY) {
		byte[] out = new byte[256];
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
					if (north < level.getMinY()) {
						north = top; // the chunk to the north has not arrived yet: no slope, or its edge row shows as a bright line
					}
					brightness = north < top ? MapColor.Brightness.HIGH : north > top ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
				}
				out[z << 4 | x] = color.getPackedId(brightness);
			}
		}
		return out;
	}

	/** Y of the highest block that blocks motion (or, under a ceiling, the first solid block below the player's head). */
	private static int top(final ClientLevel level, final BlockPos.MutableBlockPos pos, final int x, final int z, final boolean ceiling, final int eyeY) {
		if (!loaded(level, x >> 4, z >> 4)) {
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

	/**
	 * The client really holds the chunk. {@code ClientLevel.hasChunk} is true for every chunk (the client hands out an empty
	 * placeholder for missing ones), so until 0.5.51 chunks not yet received, or already dropped, were scanned as black and a
	 * rescan could black out land explored before.
	 */
	private static boolean loaded(final ClientLevel level, final int cx, final int cz) {
		return level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false) != null;
	}
}
