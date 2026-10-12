package com.minecraftmode.character;

import com.minecraftmode.MinecraftMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The lobby dimension {@code minecraft_mode:lobby}: an empty void on the overworld dimension type (stem written by
 * {@code RaidDimensionProvider}) with one small room at 0, 0, where players wait on the select screen after joining
 * ({@link Characters#choosing}). Nothing else is there, so nothing can be used, hit or bought while choosing. Where the player
 * was before is kept in the roster ({@link CharacterRoster.Origin}), so a player who logs out while choosing comes back to the
 * lobby and still leaves it to the right place. A vehicle they rode stays behind and takes them back on when they return
 * ({@link #tick} looks for it while its chunk loads).
 */
public final class Lobby {
	public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, MinecraftMode.id("lobby"));
	/** Y of the room's floor. */
	public static final int FLOOR_Y = 64;
	private static final int HALF = 4;
	private static final BlockState FLOOR = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
	private static final BlockState BORDER = Blocks.DEEPSLATE_TILES.defaultBlockState();
	/** How long a returning player waits for their vehicle to load before walking on. */
	private static final int REMOUNT_TICKS = 100;
	private static final double REMOUNT_RANGE = 8.0;
	/** Players back from the lobby whose vehicle has not loaded yet: player -> vehicle and the ticks left to find it. */
	private static final Map<UUID, Remount> REMOUNTS = new HashMap<>();

	private record Remount(UUID vehicle, int ticksLeft) {
	}

	public static boolean is(final Level level) {
		return level.dimension() == LEVEL;
	}

	/** Sends the player to the room, remembering where they were (unless they already are in the lobby). */
	static void enter(final ServerPlayer player) {
		ServerLevel lobby = player.level().getServer().getLevel(LEVEL);
		if (lobby == null || is(player.level())) {
			return;
		}
		CharacterRoster roster = CharacterRoster.get(player.level().getServer());
		roster.put(player.getUUID(), roster.account(player.getUUID()).withOrigin(CharacterRoster.Origin.of(player)));
		build(lobby);
		player.stopRiding();
		player.teleportTo(lobby, 0.5, FLOOR_Y + 1, 0.5, Set.of(), 180.0F, 0.0F, true);
	}

	/** Back to where the player was before the lobby (the world spawn when that is unknown or gone). */
	static void leave(final ServerPlayer player) {
		CharacterRoster roster = CharacterRoster.get(player.level().getServer());
		CharacterRoster.Account account = roster.account(player.getUUID());
		CharacterRoster.Origin origin = account.origin();
		if (origin != null) {
			roster.put(player.getUUID(), account.withOrigin(null));
		}
		if (!is(player.level())) {
			return;
		}
		ServerLevel level = origin == null ? null : origin.level(player.level().getServer());
		if (level == null || is(level)) {
			Characters.toWorldSpawn(player);
			return;
		}
		player.teleportTo(level, origin.x(), origin.y(), origin.z(), Set.of(), origin.yRot(), origin.xRot(), true);
		origin.vehicle().ifPresent(vehicle -> REMOUNTS.put(player.getUUID(), new Remount(vehicle, REMOUNT_TICKS)));
	}

	/** Puts players back on the vehicle they left behind, once it has loaded near them. */
	static void tick(final MinecraftServer server) {
		if (REMOUNTS.isEmpty()) {
			return;
		}
		for (Map.Entry<UUID, Remount> entry : Map.copyOf(REMOUNTS).entrySet()) {
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			Remount remount = entry.getValue();
			Entity vehicle = player == null ? null : player.level().getEntity(remount.vehicle());
			if (vehicle != null && vehicle.distanceTo(player) <= REMOUNT_RANGE) {
				player.startRiding(vehicle, true, true);
				REMOUNTS.remove(entry.getKey());
			} else if (player == null || remount.ticksLeft() <= 1) {
				REMOUNTS.remove(entry.getKey());
			} else {
				REMOUNTS.put(entry.getKey(), new Remount(remount.vehicle(), remount.ticksLeft() - 1));
			}
		}
	}

	/** Where a player in the lobby really is: their origin, or the world spawn. */
	static CharacterRoster.Origin origin(final ServerPlayer player) {
		CharacterRoster.Origin origin = CharacterRoster.get(player.level().getServer()).account(player.getUUID()).origin();
		if (origin != null) {
			return origin;
		}
		ServerLevel overworld = player.level().getServer().overworld();
		BlockPos spawn = overworld.getRespawnData().pos();
		return new CharacterRoster.Origin(overworld.dimension().identifier().toString(), spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0.0F, 0.0F,
			Optional.empty());
	}

	/** The room: a deepslate floor with a tiled border and a lantern on each corner post. Built once. */
	private static void build(final ServerLevel lobby) {
		if (lobby.getBlockState(new BlockPos(0, FLOOR_Y, 0)).is(FLOOR.getBlock())) {
			return;
		}
		for (int x = -HALF; x <= HALF; x++) {
			for (int z = -HALF; z <= HALF; z++) {
				boolean edge = Math.abs(x) == HALF || Math.abs(z) == HALF;
				lobby.setBlockAndUpdate(new BlockPos(x, FLOOR_Y, z), edge ? BORDER : FLOOR);
			}
		}
		for (int x : new int[] {-HALF, HALF}) {
			for (int z : new int[] {-HALF, HALF}) {
				lobby.setBlockAndUpdate(new BlockPos(x, FLOOR_Y + 1, z), Blocks.DEEPSLATE_TILE_WALL.defaultBlockState());
				lobby.setBlockAndUpdate(new BlockPos(x, FLOOR_Y + 2, z), Blocks.LANTERN.defaultBlockState());
			}
		}
	}

	private Lobby() {
	}
}
