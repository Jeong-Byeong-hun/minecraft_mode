package com.minecraftmode.admin;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.command.EventCommands;
import com.minecraftmode.command.JobCommand;
import com.minecraftmode.command.WalletCommand;
import com.minecraftmode.dungeon.DungeonDef;
import com.minecraftmode.dungeon.Dungeons;
import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.event.WorldEvents;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.market.AuctionService;
import com.minecraftmode.network.AdminActionPayload;
import com.minecraftmode.progress.RealReset;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.Parties;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.raid.Raids;
import com.minecraftmode.residence.LandClaims;
import com.minecraftmode.residence.LandOffice;
import com.minecraftmode.residence.ResidenceDimension;
import com.minecraftmode.travel.Destination;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

/**
 * The admin screen's server side. Every {@link AdminActionPayload} passes {@link #allowed} first (the client hiding the screen is
 * only cosmetic), is logged as {@code [Admin]}, calls the same code as the operator commands and is answered with a fresh
 * {@link AdminState}. Text and numbers from the client are range-checked here.
 */
public final class Admin {
	/** City guards leave monsters an operator summoned alone ({@code CityServices.driveOffHostiles}). */
	public static final String KEEP_TAG = "minecraft_mode_admin";
	public static final int MAX_SUMMON = 20;
	public static final int MAX_TIER = 4;
	/** The highest keystone level an operator can start a run at (as {@code /dungeon start}). */
	public static final int MAX_KEYSTONE = 30;
	private static final double LOOK_RANGE = 48.0;
	/** Vanilla {@code /time set} and {@code /weather} commands for the fixed choices of the server tab (never client text). */
	private static final String[] TIMES = {"time set day", "time set noon", "time set night", "time set midnight"};
	private static final String[] WEATHER = {"weather clear", "weather rain", "weather thunder"};

	public static boolean allowed(final ServerPlayer player) {
		return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}

	public static void handle(final ServerPlayer op, final AdminActionPayload action) {
		if (!allowed(op)) {
			MinecraftMode.LOGGER.warn("[Admin] {} is not an operator; refused action {}", op.getPlainTextName(), action.action());
			return;
		}
		int result = -1;
		if (action.action() != AdminActionPayload.REFRESH) {
			boolean ok = act(op, action);
			result = ok ? 1 : 0;
			MinecraftMode.LOGGER.info("[Admin] {} action={} target='{}' text='{}' a={} b={} c={} -> {}", op.getPlainTextName(), action.action(), action.target(),
				action.text(), action.a(), action.b(), action.c(), ok ? "ok" : "failed");
		}
		AdminState.send(op, action, result);
	}

	private static boolean act(final ServerPlayer op, final AdminActionPayload action) {
		MinecraftServer server = op.level().getServer();
		if (action.action() >= AdminActionPayload.TIME || isWorldAction(action.action())) {
			// not about a player: a picked player who has left must not block these
			return actOnWorld(op, server, action);
		}
		ServerPlayer target = action.target().isEmpty() ? op : server.getPlayerList().getPlayerByName(action.target());
		if (target == null) {
			return false;
		}
		return switch (action.action()) {
			case AdminActionPayload.JOB_SET -> {
				JobClass job = JobClass.byId(action.text());
				// an unknown id reads as NONE; only an explicit "none" may take a class away
				yield (job != JobClass.NONE || job.id().equals(action.text())) && run(() -> JobCommand.setJob(target, job, Math.clamp(action.a(), 1, MAX_TIER)));
			}
			case AdminActionPayload.LEVEL_SET -> run(() -> JobCommand.setLevel(target, action.a()));
			case AdminActionPayload.EXP_ADD -> action.a() > 0 && run(() -> JobProgression.addExp(target, action.a()));
			case AdminActionPayload.MANA_FILL -> run(() -> JobCommand.fillMana(target));
			case AdminActionPayload.COOLDOWNS -> run(() -> JobCommand.resetCooldowns(target));
			case AdminActionPayload.QUEST_CLEAR -> run(() -> JobCommand.quest(target, false));
			case AdminActionPayload.QUEST_GOALS -> run(() -> JobCommand.quest(target, true));
			case AdminActionPayload.MONEY_GIVE -> action.a() > 0 && run(() -> WalletCommand.grant(target, action.a(), false));
			case AdminActionPayload.MONEY_SET -> action.a() >= 0 && run(() -> WalletCommand.grant(target, action.a(), true));
			case AdminActionPayload.TP_TO -> target != op && teleport(op, target.level(), target.getX(), target.getY(), target.getZ());
			case AdminActionPayload.BRING -> target != op && teleport(target, op.level(), op.getX(), op.getY(), op.getZ());
			case AdminActionPayload.GAMEMODE -> action.a() >= 0 && action.a() <= GameType.SPECTATOR.getId()
				&& run(() -> target.setGameMode(GameType.byId(action.a())));
			// as vanilla /kick: admins (level 3) only, and never the host of a singleplayer/LAN world (that stops the server)
			case AdminActionPayload.KICK -> target != op && op.permissions().hasPermission(Permissions.COMMANDS_ADMIN)
				&& !server.isSingleplayerOwner(target.nameAndId()) && run(() -> target.connection.disconnect(action.text().isBlank()
				? Component.translatable("message.minecraft_mode.admin.kicked") : Component.literal(action.text().strip())));
			// a dead player stays dead: health above 0 would make the server refuse their respawn button
			case AdminActionPayload.HEAL -> !target.isDeadOrDying() && run(() -> {
				target.setHealth(target.getMaxHealth());
				target.getFoodData().setFoodLevel(20);
				target.getFoodData().setSaturation(20.0F);
				target.clearFire();
			});
			case AdminActionPayload.STORY_SET -> run(() -> EventCommands.setChapter(target, action.a()));
			case AdminActionPayload.DUNGEON_START -> startDungeon(server, target, action.text(), action.a());
			case AdminActionPayload.RAID_START -> startRaid(server, target, action.text());
			case AdminActionPayload.TITAN -> {
				// titans live in the overworld: that is where the event (and "stop events") looks for them
				NamedDef def = action.text().isEmpty() ? null : NamedMobs.byId(action.text());
				yield target.level() == server.overworld() && (def != null || action.text().isEmpty())
					&& WorldEvents.spawnTitan(target.level(), target, def) != null;
			}
			case AdminActionPayload.SUMMON -> summon(target, action.text(), action.a(), action.b() == 1);
			case AdminActionPayload.TP_COORDS -> teleportTo(target, action.text(), action.a(), action.b(), action.c());
			case AdminActionPayload.TP_PLACE -> {
				Destination destination = Destination.byOrdinal(action.a());
				Destination.Spot spot = destination == null ? null : destination.spot(server);
				yield spot != null && teleport(target, spot.level(), spot.pos().x, spot.pos().y, spot.pos().z);
			}

			default -> false;
		};
	}

	private static boolean isWorldAction(final int action) {
		return action == AdminActionPayload.DUNGEON_STOP || action == AdminActionPayload.RAID_STOP || action == AdminActionPayload.INVASION
			|| action == AdminActionPayload.EVENT_STOP || action == AdminActionPayload.GATHER;
	}

	/** Actions on the server, the world or saved data rather than on a player. */
	private static boolean actOnWorld(final ServerPlayer op, final MinecraftServer server, final AdminActionPayload action) {
		return switch (action.action()) {
			case AdminActionPayload.DUNGEON_STOP -> Dungeons.stop(action.a());
			case AdminActionPayload.RAID_STOP -> Raids.stop(action.a());
			case AdminActionPayload.INVASION -> WorldEvents.startInvasion(server.overworld());
			case AdminActionPayload.EVENT_STOP -> run(() -> {
				WorldEvents.stopTitan(server.overworld());
				WorldEvents.stopInvasion(server.overworld());
			});
			case AdminActionPayload.GATHER -> run(() -> {
				for (ServerPlayer p : server.getPlayerList().getPlayers()) {
					if (p != op) {
						teleport(p, op.level(), op.getX(), op.getY(), op.getZ());
					}
				}
			});
			case AdminActionPayload.TIME -> action.a() >= 0 && action.a() < TIMES.length && vanilla(server, TIMES[action.a()]);
			case AdminActionPayload.WEATHER -> action.a() >= 0 && action.a() < WEATHER.length && vanilla(server, WEATHER[action.a()]);
			case AdminActionPayload.SKIP_DAYS -> action.a() >= 1 && action.a() <= RealReset.DAYS && run(() -> RealReset.skipDays(server, action.a()));
			case AdminActionPayload.BROADCAST -> broadcast(server, action.text());
			case AdminActionPayload.AUCTION_REMOVE -> AuctionService.adminCancel(server, action.a());
			case AdminActionPayload.LAND_RELEASE -> releaseLand(op, server, action);
			default -> false;
		};
	}

	private static boolean run(final Runnable step) {
		step.run();
		return true;
	}

	private static boolean vanilla(final MinecraftServer server, final String command) {
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
		return true;
	}

	private static boolean startDungeon(final MinecraftServer server, final ServerPlayer leader, final String id, final int level) {
		DungeonDef def = Dungeons.def(id);
		if (def == null || level < 0 || level > MAX_KEYSTONE) {
			return false;
		}
		return Dungeons.start(server, Parties.onlineMembers(leader), def, leader.getUUID(), level, level > 0 ? leader.getUUID() : null) != null;
	}

	private static boolean startRaid(final MinecraftServer server, final ServerPlayer leader, final String id) {
		BossDef def = RaidBosses.byId(id);
		return def != null && Raids.start(server, Parties.onlineMembers(leader), def, leader.getUUID()) != null;
	}

	/** {@code count} of entity type {@code id} at the target's feet or where they look; tagged so the city guards leave them. */
	private static boolean summon(final ServerPlayer target, final String id, final int count, final boolean where) {
		Identifier key = Identifier.tryParse(id);
		EntityType<?> type = key == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(key).orElse(null);
		if (type == null || !type.canSummon() || count < 1 || count > MAX_SUMMON) {
			return false;
		}
		ServerLevel level = target.level();
		BlockPos pos = where ? lookPoint(target) : target.blockPosition();
		int spawned = 0;
		for (int i = 0; i < count; i++) {
			Entity entity = type.spawn(level, pos, EntitySpawnReason.COMMAND);
			if (entity != null) {
				entity.addTag(KEEP_TAG);
				spawned++;
			}
		}
		return spawned > 0;
	}

	private static BlockPos lookPoint(final ServerPlayer player) {
		HitResult hit = player.pick(LOOK_RANGE, 1.0F, false);
		return hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
			? block.getBlockPos().relative(block.getDirection())
			: player.blockPosition();
	}

	/** To {@code x, y, z} in dimension {@code dimension} ("" = where the player is); {@link AdminActionPayload#SURFACE} = on top. */
	private static boolean teleportTo(final ServerPlayer player, final String dimension, final int x, final int y, final int z) {
		ServerLevel level = dimension.isEmpty() ? player.level() : level(player.level().getServer(), dimension);
		if (level == null || !level.getWorldBorder().isWithinBounds(x, z)) {
			return false;
		}
		int feet = y == AdminActionPayload.SURFACE ? surface(level, x, z) : y;
		if (feet < level.getMinY() || feet > level.getMaxY()) {
			return false;
		}
		return teleport(player, level, x + 0.5, feet, z + 0.5);
	}

	private static @Nullable ServerLevel level(final MinecraftServer server, final String dimension) {
		Identifier id = Identifier.tryParse(dimension);
		return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
	}

	/** The first spot with two free blocks at or above the column's top block (loads the chunk). */
	static int surface(final ServerLevel level, final int x, final int z) {
		level.getChunk(x >> 4, z >> 4);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
		while (pos.getY() < level.getMaxY() && !(free(level, pos) && free(level, pos.above()))) {
			pos.move(0, 1, 0);
		}
		return pos.getY();
	}

	private static boolean free(final Level level, final BlockPos pos) {
		return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}

	static boolean teleport(final ServerPlayer player, final ServerLevel level, final double x, final double y, final double z) {
		if (player.getVehicle() != null) {
			player.stopRiding();
		}
		boolean moved = player.teleportTo(level, x, y, z, java.util.Set.of(), player.getYRot(), player.getXRot(), true);
		player.resetFallDistance();
		return moved;
	}

	private static boolean broadcast(final MinecraftServer server, final String text) {
		String message = text.strip();
		if (message.isEmpty()) {
			return false;
		}
		Component line = Component.translatable("message.minecraft_mode.admin.notice", message).withStyle(ChatFormatting.GOLD);
		server.getPlayerList().broadcastSystemMessage(line, false);
		server.getPlayerList().broadcastAll(new ClientboundSetTitleTextPacket(Component.literal(message).withStyle(ChatFormatting.GOLD)));
		return true;
	}

	private static boolean releaseLand(final ServerPlayer op, final MinecraftServer server, final AdminActionPayload action) {
		UUID owner;
		try {
			owner = UUID.fromString(action.text());
		} catch (IllegalArgumentException e) {
			return false;
		}
		LandClaims.Claim claim = new LandClaims.Claim(net.minecraft.world.level.ChunkPos.pack(action.a(), action.b()), action.c(), owner);
		ServerLevel residence = ResidenceDimension.level(server);
		if (residence == null || !residence.areEntitiesLoaded(claim.chunk())) {
			// frames, stands and carts in an unloaded chunk would be missed and left on open land
			op.sendSystemMessage(Component.translatable("message.minecraft_mode.admin.land_unloaded").withStyle(ChatFormatting.RED));
			return false;
		}
		return LandOffice.release(server, claim);
	}

	private Admin() {
	}
}
