package com.minecraftmode.client.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minecraftmode.MinecraftMode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** The player's own markers, per dimension, saved as JSON next to the map data. */
public final class Waypoints {
	/** Name used for the automatic marker at the place of the last death. */
	public static final String DEATH = "death";
	public static final int[] COLORS = {0xFFE04040, 0xFF40C040, 0xFF4080FF, 0xFFFFD040, 0xFFFF80C0, 0xFF40E0E0, 0xFFFFFFFF, 0xFFFF9020};
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static final class Waypoint {
		public String name;
		public int x;
		public int y;
		public int z;
		public int color;

		public Waypoint(final String name, final int x, final int y, final int z, final int color) {
			this.name = name;
			this.x = x;
			this.y = y;
			this.z = z;
			this.color = color;
		}

		public boolean isDeath() {
			return DEATH.equals(this.name);
		}
	}

	private final Map<String, List<Waypoint>> byDimension = new TreeMap<>();
	private boolean dirty;

	public List<Waypoint> of(final String dimension) {
		return this.byDimension.computeIfAbsent(dimension, k -> new ArrayList<>());
	}

	public void add(final String dimension, final Waypoint waypoint) {
		List<Waypoint> list = this.of(dimension);
		if (waypoint.isDeath()) {
			list.removeIf(Waypoint::isDeath);
			list.addFirst(waypoint);
		} else {
			list.add(waypoint);
		}
		this.dirty = true;
	}

	public void remove(final String dimension, final Waypoint waypoint) {
		this.of(dimension).remove(waypoint);
		this.dirty = true;
	}

	public void touch() {
		this.dirty = true;
	}

	public static Waypoints load(final Path file) {
		Waypoints out = new Waypoints();
		if (!Files.isRegularFile(file)) {
			return out;
		}
		try {
			JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
			for (Map.Entry<String, com.google.gson.JsonElement> e : root.entrySet()) {
				List<Waypoint> list = out.of(e.getKey());
				for (com.google.gson.JsonElement w : e.getValue().getAsJsonArray()) {
					JsonObject o = w.getAsJsonObject();
					list.add(new Waypoint(o.get("name").getAsString(), o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt(), o.get("color").getAsInt()));
				}
			}
		} catch (IOException | RuntimeException e) {
			MinecraftMode.LOGGER.warn("Could not read waypoints {}", file, e);
		}
		return out;
	}

	public void save(final Path file) {
		if (!this.dirty) {
			return;
		}
		JsonObject root = new JsonObject();
		for (Map.Entry<String, List<Waypoint>> e : this.byDimension.entrySet()) {
			JsonArray array = new JsonArray();
			for (Waypoint w : e.getValue()) {
				JsonObject o = new JsonObject();
				o.addProperty("name", w.name);
				o.addProperty("x", w.x);
				o.addProperty("y", w.y);
				o.addProperty("z", w.z);
				o.addProperty("color", w.color);
				array.add(o);
			}
			root.add(e.getKey(), array);
		}
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(root));
			this.dirty = false;
		} catch (IOException e) {
			MinecraftMode.LOGGER.warn("Could not write waypoints {}", file, e);
		}
	}
}
