package com.minecraftmode.client.map;

import com.minecraftmode.MinecraftMode;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

/**
 * Everything the player has seen of one dimension, as one top-down colour per block (ARGB, 0 = never seen), kept per chunk and
 * saved to {@code minecraft_mode/map/<world>/<dimension>.bin} in the game directory so the big map keeps growing between sessions.
 * Files are read and written through buffers, and saves run on a background thread: until 0.5.51 every save (every two minutes
 * and on each dimension change) and load wrote or read the whole map four bytes at a time on the render thread, a stall that
 * grew with the explored area.
 */
public final class MapData {
	private static final int BUFFER = 1 << 16;
	/** One writer thread, so saves of the same file land in order; it ends when idle. */
	private static final ExecutorService WRITER = writer();
	/** The save still running per file: a load of that file waits for it, or it would read half a file. */
	private static final Map<Path, CompletableFuture<Void>> WRITING = new ConcurrentHashMap<>();

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

	/** Stores a chunk's colours; {@code colors} must not change afterwards (a background save may still be reading it). */
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
		CompletableFuture<Void> writing = WRITING.get(file);
		if (writing != null) {
			writing.join();
		}
		MapData data = new MapData();
		if (!Files.isRegularFile(file)) {
			return data;
		}
		try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(file), BUFFER), BUFFER))) {
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

	/** Saves in the background when anything changed: the chunks are copied now, the file is written to a temporary one and swapped in. */
	public void save(final Path file) {
		if (!this.dirty) {
			return;
		}
		this.dirty = false;
		Map<Long, int[]> copy = new HashMap<>(this.chunks);
		CompletableFuture<Void> previous = WRITING.getOrDefault(file, CompletableFuture.completedFuture(null));
		CompletableFuture<Void> next = previous.handleAsync((ignored, error) -> {
			write(file, copy);
			return null;
		}, WRITER);
		WRITING.put(file, next);
		next.whenComplete((ignored, error) -> WRITING.remove(file, next));
	}

	/** Waits for every background save (the game is closing). */
	public static void awaitSaves() {
		for (CompletableFuture<Void> writing : WRITING.values()) {
			writing.join();
		}
	}

	private static void write(final Path file, final Map<Long, int[]> chunks) {
		Path temp = file.resolveSibling(file.getFileName() + ".tmp");
		try {
			Files.createDirectories(file.getParent());
			try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(temp), BUFFER), BUFFER))) {
				out.writeInt(chunks.size());
				for (Map.Entry<Long, int[]> e : chunks.entrySet()) {
					out.writeInt(ChunkPos.getX(e.getKey()));
					out.writeInt(ChunkPos.getZ(e.getKey()));
					for (int c : e.getValue()) {
						out.writeInt(c);
					}
				}
			}
			try {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			MinecraftMode.LOGGER.warn("Could not write map data {}", file, e);
		}
	}

	private static ExecutorService writer() {
		ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), task -> {
			Thread thread = new Thread(task, "minecraft_mode map writer");
			thread.setDaemon(true);
			return thread;
		});
		executor.allowCoreThreadTimeOut(true);
		return executor;
	}
}
