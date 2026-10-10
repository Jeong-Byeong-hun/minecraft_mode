package com.minecraftmode.client.map;

import com.minecraftmode.MinecraftMode;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

/**
 * Everything the player has seen of one dimension, as one top-down colour per block (ARGB, 0 = never seen), kept per chunk and
 * saved to {@code minecraft_mode/map/<world>/<dimension>.bin} in the game directory so the big map keeps growing between sessions.
 */
public final class MapData {
	private final Map<Long, int[]> chunks = new HashMap<>();
	private final Map<Long, Long> scanned = new HashMap<>();
	private boolean dirty;

	public @Nullable int[] chunk(final int cx, final int cz) {
		return this.chunks.get(ChunkPos.pack(cx, cz));
	}

	/** The colour of block {@code x, z}, 0 when unexplored. */
	public int color(final int x, final int z) {
		int[] colors = this.chunk(x >> 4, z >> 4);
		return colors == null ? 0 : colors[(z & 15) << 4 | x & 15];
	}

	public void put(final int cx, final int cz, final int[] colors, final long now) {
		long key = ChunkPos.pack(cx, cz);
		this.chunks.put(key, colors);
		this.scanned.put(key, now);
		this.dirty = true;
	}

	/** Game time of the last scan of the chunk, -1 when never. */
	public long scannedAt(final int cx, final int cz) {
		return this.scanned.getOrDefault(ChunkPos.pack(cx, cz), -1L);
	}

	public int size() {
		return this.chunks.size();
	}

	public boolean dirty() {
		return this.dirty;
	}

	// ------------------------------------------------------------------ files

	public static MapData load(final Path file) {
		MapData data = new MapData();
		if (!Files.isRegularFile(file)) {
			return data;
		}
		try (DataInputStream in = new DataInputStream(new GZIPInputStream(Files.newInputStream(file)))) {
			int count = in.readInt();
			for (int i = 0; i < count; i++) {
				int cx = in.readInt();
				int cz = in.readInt();
				int[] colors = new int[256];
				for (int j = 0; j < 256; j++) {
					colors[j] = in.readInt();
				}
				data.chunks.put(ChunkPos.pack(cx, cz), colors);
			}
		} catch (IOException e) {
			MinecraftMode.LOGGER.warn("Could not read map data {}", file, e);
		}
		return data;
	}

	public void save(final Path file) {
		if (!this.dirty) {
			return;
		}
		try {
			Files.createDirectories(file.getParent());
			try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(file)))) {
				out.writeInt(this.chunks.size());
				for (Map.Entry<Long, int[]> e : this.chunks.entrySet()) {
					out.writeInt(ChunkPos.getX(e.getKey()));
					out.writeInt(ChunkPos.getZ(e.getKey()));
					for (int c : e.getValue()) {
						out.writeInt(c);
					}
				}
			}
			this.dirty = false;
		} catch (IOException e) {
			MinecraftMode.LOGGER.warn("Could not write map data {}", file, e);
		}
	}
}
