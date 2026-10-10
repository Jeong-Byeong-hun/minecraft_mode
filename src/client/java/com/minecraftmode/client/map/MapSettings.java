package com.minecraftmode.client.map;

import com.minecraftmode.MinecraftMode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Minimap preferences (shown, size, zoom), kept in {@code minecraft_mode/map.properties}. */
public final class MapSettings {
	public static final int[] SIZES = {64, 96, 128};
	public static final float[] ZOOMS = {0.5F, 1.0F, 2.0F};

	public static boolean minimap = true;
	public static int size = 1;
	public static int zoom = 1;

	public static int sizePx() {
		return SIZES[Math.floorMod(size, SIZES.length)];
	}

	public static float zoomFactor() {
		return ZOOMS[Math.floorMod(zoom, ZOOMS.length)];
	}

	private static Path file() {
		return MapClient.root().resolve("map.properties");
	}

	static void load() {
		Path file = file();
		if (!Files.isRegularFile(file)) {
			return;
		}
		Properties p = new Properties();
		try (var in = Files.newInputStream(file)) {
			p.load(in);
			minimap = Boolean.parseBoolean(p.getProperty("minimap", "true"));
			size = Integer.parseInt(p.getProperty("size", "1"));
			zoom = Integer.parseInt(p.getProperty("zoom", "1"));
		} catch (IOException | RuntimeException e) {
			MinecraftMode.LOGGER.warn("Could not read {}", file, e);
		}
	}

	static void save() {
		Properties p = new Properties();
		p.setProperty("minimap", Boolean.toString(minimap));
		p.setProperty("size", Integer.toString(size));
		p.setProperty("zoom", Integer.toString(zoom));
		try {
			Files.createDirectories(file().getParent());
			try (var out = Files.newOutputStream(file())) {
				p.store(out, "minecraft_mode map settings");
			}
		} catch (IOException e) {
			MinecraftMode.LOGGER.warn("Could not write {}", file(), e);
		}
	}

	private MapSettings() {
	}
}
