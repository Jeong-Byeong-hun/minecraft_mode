package com.minecraftmode.client.map;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.job.JobKeys;
import com.minecraftmode.network.CityInfoPayload;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

/**
 * The world map: explored terrain per world and dimension ({@link MapData}), a minimap in the corner ({@link MinimapHud}), the
 * big map on M ({@link MapScreen}) and waypoints ({@link Waypoints}), all client side. The comma key shows or hides the minimap.
 */
public final class MapClient {
	public static final KeyMapping OPEN_MAP = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.minecraft_mode.map", InputConstants.Type.KEYBOARD, InputConstants.KEY_M, JobKeys.CATEGORY));
	public static final KeyMapping TOGGLE_MINIMAP = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.minecraft_mode.minimap", InputConstants.Type.KEYBOARD, InputConstants.KEY_COMMA, JobKeys.CATEGORY));

	/** Texture around the player for the minimap and around the viewed spot for the big map. */
	static final MapTexture NEAR = new MapTexture("near", 512);
	static final MapTexture VIEW = new MapTexture("view", 2048);
	private static final TerrainScanner SCANNER = new TerrainScanner();
	private static final int SAVE_INTERVAL = 20 * 120;

	private static @Nullable String worldKey;
	private static @Nullable ResourceKey<Level> dimension;
	private static MapData data = new MapData();
	private static Waypoints waypoints = new Waypoints();
	private static long lastSave;
	private static boolean wasDead;

	public static void init() {
		MapSettings.load();
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, MinecraftMode.id("minimap"), MinimapHud::extract);
		ClientTickEvents.END_CLIENT_TICK.register(MapClient::tick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
			CityPlaces.cityWorld = false;
			leave();
		}));
		ClientPlayNetworking.registerGlobalReceiver(CityInfoPayload.TYPE, (payload, context) -> context.client().execute(() -> CityPlaces.cityWorld = payload.city()));
	}

	public static MapData data() {
		return data;
	}

	public static Waypoints waypoints() {
		return waypoints;
	}

	/** The current dimension's id, the key waypoints are filed under. */
	public static String dimensionKey() {
		return dimension == null ? "minecraft:overworld" : dimension.identifier().toString();
	}

	public static List<Waypoints.Waypoint> currentWaypoints() {
		return waypoints.of(dimensionKey());
	}

	private static void tick(final Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		ClientLevel level = minecraft.level;
		while (OPEN_MAP.consumeClick()) {
			if (player != null && minecraft.gui.screen() == null) {
				minecraft.gui.setScreen(new MapScreen());
			}
		}
		while (TOGGLE_MINIMAP.consumeClick()) {
			MapSettings.minimap = !MapSettings.minimap;
			MapSettings.save();
			if (player != null) {
				player.sendOverlayMessage(Component.translatable(MapSettings.minimap ? "message.minecraft_mode.map.minimap_on" : "message.minecraft_mode.map.minimap_off")
					.withStyle(ChatFormatting.GRAY));
			}
		}
		if (player == null || level == null) {
			return;
		}
		String key = worldKey(minecraft);
		long now = level.getGameTime();
		if (!key.equals(worldKey) || level.dimension() != dimension) {
			if (!key.equals(worldKey)) {
				adoptLegacy(minecraft, key);
			}
			enter(key, level.dimension());
			// the save timer follows this world's clock, which may be behind the last one's
			lastSave = now;
		}
		SCANNER.tick(level, data, player.blockPosition(), now, NEAR, VIEW);
		NEAR.center(data, player.getBlockX(), player.getBlockZ());
		NEAR.upload(now);
		if (minecraft.gui.screen() instanceof MapScreen screen) {
			VIEW.center(data, (int)screen.centerX(), (int)screen.centerZ());
			VIEW.upload(now);
		}
		boolean dead = player.isDeadOrDying() || player.getHealth() <= 0.0F;
		if (dead && !wasDead) {
			waypoints.add(dimensionKey(), new Waypoints.Waypoint(Waypoints.DEATH, player.getBlockX(), player.getBlockY(), player.getBlockZ(), 0xFFE04040));
		}
		wasDead = dead;
		if (now - lastSave > SAVE_INTERVAL) {
			save();
			lastSave = now;
		}
	}

	private static void enter(final String key, final ResourceKey<Level> dim) {
		save();
		worldKey = key;
		dimension = dim;
		data = MapData.load(dataFile(key, dim));
		waypoints = Waypoints.load(waypointFile(key));
		NEAR.clear();
		VIEW.clear();
	}

	private static void leave() {
		save();
		worldKey = null;
		dimension = null;
		data = new MapData();
		waypoints = new Waypoints();
		NEAR.clear();
		VIEW.clear();
	}

	static void save() {
		if (worldKey != null && dimension != null) {
			data.save(dataFile(worldKey, dimension));
			waypoints.save(waypointFile(worldKey));
		}
	}

	private static Path dataFile(final String key, final ResourceKey<Level> dim) {
		return root().resolve("map").resolve(key).resolve(dim.identifier().toString().replace(':', '_') + ".bin");
	}

	private static Path waypointFile(final String key) {
		return root().resolve("waypoints").resolve(key + ".json");
	}

	static Path root() {
		return FabricLoader.getInstance().getGameDir().resolve("minecraft_mode");
	}

	/**
	 * A file-safe name for the world: its save folder in singleplayer (plus a hash, since non-ASCII names flatten to underscores),
	 * the address on a server.
	 */
	private static String worldKey(final Minecraft minecraft) {
		if (minecraft.isLocalServer() && minecraft.getSingleplayerServer() != null) {
			Path folder = minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName();
			String name = folder == null ? "world" : folder.toString();
			return safe("sp_" + name) + "_" + Integer.toHexString(name.hashCode());
		}
		return safe(minecraft.getCurrentServer() != null ? "mp_" + minecraft.getCurrentServer().ip : "unknown");
	}

	private static String safe(final String raw) {
		return raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
	}

	/**
	 * Earlier versions keyed a singleplayer world by its display name, so worlds with the same name shared one map. A world
	 * without a map of its own starts from a copy of what was saved under that old key.
	 */
	private static void adoptLegacy(final Minecraft minecraft, final String key) {
		if (!minecraft.isLocalServer() || minecraft.getSingleplayerServer() == null) {
			return;
		}
		String legacy = safe("sp_" + minecraft.getSingleplayerServer().getWorldData().getLevelName());
		Path maps = root().resolve("map");
		if (Files.exists(maps.resolve(key)) || Files.exists(waypointFile(key)) || !Files.isDirectory(maps.resolve(legacy))) {
			return;
		}
		try {
			Files.createDirectories(maps.resolve(key));
			try (Stream<Path> files = Files.list(maps.resolve(legacy))) {
				for (Path file : files.toList()) {
					Files.copy(file, maps.resolve(key).resolve(file.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
				}
			}
			if (Files.exists(waypointFile(legacy))) {
				Files.copy(waypointFile(legacy), waypointFile(key), StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			MinecraftMode.LOGGER.warn("Could not copy the old map {} to {}", legacy, key, e);
		}
	}

	private MapClient() {
	}
}
