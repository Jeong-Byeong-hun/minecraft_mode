package com.minecraftmode.worldgen.lair;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.bag.Bags;
import com.minecraftmode.character.Characters;
import com.minecraftmode.character.Lobby;
import com.minecraftmode.dungeon.DungeonDimension;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.network.LairClearPayload;
import com.minecraftmode.network.LairOfferPayload;
import com.minecraftmode.progress.ResetCycle;
import com.minecraftmode.raid.RaidDimension;
import com.minecraftmode.registry.ModAttachments;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.residence.ResidenceDimension;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.jspecify.annotations.Nullable;

/**
 * The lair map loop, a little like map runs: reading a lair map finds a lair for the reader's level ({@link #find}: the nearest one whose
 * named monster's range comes within {@link #LEVEL_REACH} of the reader's class level, never one they cleared this cycle; none at all when
 * nothing of that level is in reach) and asks on the client
 * whether to go ({@link LairOfferPayload}); on yes the map is used up and the player stands at the lair's gate. Clearing a lair (the first
 * goal chest of the cycle) gives the next map and offers the way back to the gate ({@link LairClearPayload}).
 * <p>
 * Trips wait for the gate's chunks with a loading ticket ({@link #TICKET}) instead of loading them on the spot, so a chunk whose generation
 * fails never freezes the server; the spot is then checked for solid ground and two free blocks ({@link #safeSpot}). Offers, exits and
 * trips live in server memory only; what was cleared is saved per character ({@link ModAttachments#LAIR_CLEARED}).
 */
public final class LairTravel {
	/** Placement rings (one ring = {@link NamedLairs#SPACING} chunks further out) the map searches at most. */
	public static final int RINGS = 8;
	/** After the first ring with a lair of the reader's level, this many more rings are searched for a nearer one. */
	public static final int NEARER_RINGS = 1;
	/**
	 * How far a lair's named range may be from the reader's class level: a map leads only to lairs whose range comes within this many
	 * levels (0.5.46; it used to fall back to the nearest lair of any level, sending a Lv 15 player to a Lv 90 lair). Readers below the
	 * lowest lair count as its level.
	 */
	public static final int LEVEL_REACH = 5;
	/** Ticks a trip waits for the gate's chunks before giving up (the map is kept). */
	public static final int WAIT_TICKS = 400;
	/** How far (blocks) from the spot the gate's ground may be searched for a safe place to stand. */
	public static final int SPOT_SEARCH = 4;

	private static final TicketType TICKET = Registry.register(BuiltInRegistries.TICKET_TYPE, MinecraftMode.id("lair_travel"),
		new TicketType(TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING));

	/** The lairs a player cleared this cycle ({@link #key}s); another cycle's list counts as empty. */
	public record Cleared(long cycle, List<String> lairs) {
		public static final Codec<Cleared> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("cycle").forGetter(Cleared::cycle),
			Codec.STRING.listOf().fieldOf("lairs").forGetter(Cleared::lairs)
		).apply(i, Cleared::new));

		public Cleared {
			lairs = List.copyOf(lairs);
		}
	}

	/**
	 * A lair someone may travel to.
	 *
	 * @param goal     the goal chest (also the lair's key)
	 * @param entrance the nominal spot outside the gate ({@link LairPiece#entrance()})
	 * @param center   the lair's middle (the waypoint)
	 */
	public record Target(ResourceKey<Level> dimension, LairDef lair, BlockPos goal, BlockPos entrance, BlockPos center) {
		/** Whether a reader of class {@code level} may be sent here ({@link #LEVEL_REACH}). */
		public boolean fits(final int level) {
			int reader = Math.max(level, NamedLairs.lowestLevel());
			return reader >= this.lair.named().lo() - LEVEL_REACH && reader <= this.lair.named().hi() + LEVEL_REACH;
		}

		public String key() {
			return LairTravel.key(this.dimension, this.goal);
		}
	}

	private static final class Trip {
		final Target target;
		/** A map trip (uses up a lair map) rather than the way back to the gate. */
		final boolean map;
		final ChunkPos chunk;
		boolean asked;
		int waited;

		Trip(final Target target, final boolean map) {
			this.target = target;
			this.map = map;
			this.chunk = ChunkPos.containing(target.entrance());
		}
	}

	private static final Map<UUID, Target> OFFERS = new HashMap<>();
	private static final Map<UUID, Target> EXITS = new HashMap<>();
	private static final Map<UUID, Trip> TRIPS = new HashMap<>();

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(LairTravel::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.player.getUUID();
			OFFERS.remove(id);
			EXITS.remove(id);
			Trip trip = TRIPS.remove(id);
			ServerLevel level = trip == null ? null : server.getLevel(trip.target.dimension());
			if (level != null) {
				release(level.getChunkSource(), trip);
			}
		});
		// tickets die with the server; the maps must not carry trips into the next world of a singleplayer session
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			OFFERS.clear();
			EXITS.clear();
			TRIPS.clear();
		});
	}

	// ------------------------------------------------------------------ cleared this cycle

	public static String key(final ResourceKey<Level> dimension, final BlockPos goal) {
		return dimension.identifier() + "|" + goal.asLong();
	}

	/** The keys of the lairs {@code player} cleared in the current cycle. */
	public static Set<String> cleared(final ServerPlayer player) {
		Cleared c = player.getAttached(ModAttachments.LAIR_CLEARED);
		return c == null || c.cycle() != ResetCycle.cycle(player.level()) ? Set.of() : Set.copyOf(c.lairs());
	}

	public static boolean clearedThisCycle(final ServerPlayer player, final Target target) {
		return cleared(player).contains(target.key());
	}

	private static void markCleared(final ServerPlayer player, final String key) {
		long cycle = ResetCycle.cycle(player.level());
		Cleared c = player.getAttached(ModAttachments.LAIR_CLEARED);
		List<String> lairs = new ArrayList<>(c == null || c.cycle() != cycle ? List.of() : c.lairs());
		if (!lairs.contains(key)) {
			lairs.add(key);
		}
		player.setAttached(ModAttachments.LAIR_CLEARED, new Cleared(cycle, lairs));
	}

	// ------------------------------------------------------------------ finding a lair

	/** Whether lair maps work where {@code player} stands (not in instanced or private worlds, not while choosing a character). */
	public static boolean allowed(final ServerPlayer player) {
		Level level = player.level();
		return !Characters.choosing(player) && !RaidDimension.is(level) && !DungeonDimension.is(level) && !Lobby.is(level) && !ResidenceDimension.is(level);
	}

	/** Lairs that can stand in {@code level}: surface and underground ones in the overworld, Nether ones in the Nether, End ones in the End. */
	private static boolean belongs(final LairDef def, final ServerLevel level) {
		if (level.dimension() == Level.NETHER) {
			return def.setting() == LairDef.Setting.NETHER;
		}
		if (level.dimension() == Level.END) {
			return def.setting() == LairDef.Setting.END;
		}
		if (level.dimension() == Level.OVERWORLD) {
			return def.setting() == LairDef.Setting.SURFACE || def.setting() == LairDef.Setting.UNDERGROUND;
		}
		return true;
	}

	/**
	 * The lair a map read at {@code from} leads to for a player of class level {@code playerLevel}: the nearest one whose named monster's range
	 * comes within {@link #LEVEL_REACH} levels of it ({@link Target#fits}); lairs in {@code skip} (keys) are passed over. Rings of the lair
	 * placement grid are searched outwards (at most {@link #RINGS}), and {@link #NEARER_RINGS} more after the first fitting lair turns up.
	 * Chunks that may hold a lair are loaded to their structure starts, like vanilla's locate does. Null when no lair of that level is in reach.
	 */
	public static @Nullable Target find(final ServerLevel level, final BlockPos from, final int playerLevel, final Set<String> skip) {
		if (!level.structureManager().shouldGenerateStructures()) {
			return null;
		}
		var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		List<Holder<Structure>> lairs = NamedLairs.all().stream().filter(d -> belongs(d, level))
			.<Holder<Structure>>map(d -> registry.getOrThrow(d.key())).toList();
		if (lairs.isEmpty()) {
			return null;
		}
		ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
		RandomSpreadStructurePlacement placement = null;
		for (StructurePlacement p : state.getPlacementsForStructure(lairs.getFirst())) {
			if (p instanceof RandomSpreadStructurePlacement spread) {
				placement = spread;
			}
		}
		if (placement == null) {
			return null;
		}
		int ox = from.getX() >> 4;
		int oz = from.getZ() >> 4;
		int spacing = placement.spacing();
		List<Target> found = new ArrayList<>();
		int fitRing = -1;
		for (int ring = 0; ring <= RINGS; ring++) {
			for (int x = -ring; x <= ring; x++) {
				for (int z = -ring; z <= ring; z++) {
					if (Math.abs(x) != ring && Math.abs(z) != ring) {
						continue;
					}
					ChunkPos chunk = placement.getPotentialStructureChunk(state.getLevelSeed(), ox + spacing * x, oz + spacing * z);
					Target target = lairAt(level, chunk, placement, lairs);
					if (target != null && target.fits(playerLevel) && !skip.contains(target.key()) && level.getWorldBorder().isWithinBounds(target.entrance())) {
						found.add(target);
					}
				}
			}
			if (!found.isEmpty() && fitRing < 0) {
				fitRing = ring;
			}
			if (fitRing >= 0 && ring >= fitRing + NEARER_RINGS) {
				break;
			}
		}
		return found.stream().min(Comparator.comparingDouble(t -> horizontal(from, t.entrance()))).orElse(null);
	}

	/** The lair starting in {@code chunk}, if any (one lair set, so at most one). */
	private static @Nullable Target lairAt(final ServerLevel level, final ChunkPos chunk, final StructurePlacement placement,
		final List<Holder<Structure>> lairs) {
		for (Holder<Structure> lair : lairs) {
			if (level.structureManager().checkStructurePresence(chunk, lair.value(), placement, false) == StructureCheckResult.START_NOT_PRESENT) {
				continue;
			}
			ChunkAccess access = level.getChunk(chunk.x(), chunk.z(), ChunkStatus.STRUCTURE_STARTS);
			for (StructureStart start : access.getAllStarts().values()) {
				if (start.isValid() && start.getStructure() instanceof LairStructure && !start.getPieces().isEmpty()
					&& start.getPieces().getFirst() instanceof LairPiece piece && piece.def() != null) {
					return target(level, piece);
				}
			}
			return null;
		}
		return null;
	}

	public static Target target(final ServerLevel level, final LairPiece piece) {
		return new Target(level.dimension(), piece.def(), piece.goalPos(), piece.entrance(), piece.center());
	}

	private static double horizontal(final BlockPos a, final BlockPos b) {
		double dx = a.getX() - b.getX();
		double dz = a.getZ() - b.getZ();
		return Math.sqrt(dx * dx + dz * dz);
	}

	// ------------------------------------------------------------------ the map: offer, confirm, travel

	/** {@code player} read a lair map: find their lair, mark it on their map and ask on the client whether to go there. */
	public static @Nullable Target offer(final ServerPlayer player) {
		if (!allowed(player)) {
			player.sendOverlayMessage(Component.translatable("message.minecraft_mode.lair.map_here").withStyle(ChatFormatting.RED));
			return null;
		}
		ServerLevel level = player.level();
		Target target = find(level, player.blockPosition(), JobProgression.get(player).level(), cleared(player));
		if (target == null) {
			player.sendOverlayMessage(Component.translatable("message.minecraft_mode.lair.no_map").withStyle(ChatFormatting.RED));
			return null;
		}
		OFFERS.put(player.getUUID(), target);
		LairMapItem.mark(player, level, target.lair(), target.center());
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.0F);
		if (ServerPlayNetworking.canSend(player, LairOfferPayload.TYPE)) {
			ServerPlayNetworking.send(player, new LairOfferPayload(target.lair().id(), target.entrance(),
				(int)Math.round(horizontal(player.blockPosition(), target.entrance()))));
		}
		return target;
	}

	/** The lair last offered to {@code player} (tests). */
	public static @Nullable Target offered(final ServerPlayer player) {
		return OFFERS.get(player.getUUID());
	}

	/** {@code player} said yes to the last offer: check again and start the trip (the map is used up on arrival). */
	public static boolean confirm(final ServerPlayer player) {
		Target target = OFFERS.remove(player.getUUID());
		if (target == null) {
			return false;
		}
		if (!allowed(player) || player.level().dimension() != target.dimension()) {
			player.sendOverlayMessage(Component.translatable("message.minecraft_mode.lair.map_here").withStyle(ChatFormatting.RED));
			return false;
		}
		if (clearedThisCycle(player, target)) {
			player.sendOverlayMessage(Component.translatable("message.minecraft_mode.lair.travel_cleared").withStyle(ChatFormatting.RED));
			return false;
		}
		if (mapSlot(player) < 0 && !player.isCreative()) {
			player.sendOverlayMessage(Component.translatable("message.minecraft_mode.lair.travel_no_map").withStyle(ChatFormatting.RED));
			return false;
		}
		start(player, target, true);
		return true;
	}

	/** The inventory slot of a lair map: the hands first, then the rest of the inventory; -1 when there is none. */
	private static int mapSlot(final ServerPlayer player) {
		Inventory inventory = player.getInventory();
		if (player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.LAIR_MAP)) {
			return inventory.getSelectedSlot();
		}
		if (player.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.LAIR_MAP)) {
			return Inventory.SLOT_OFFHAND;
		}
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).is(ModItems.LAIR_MAP)) {
				return i;
			}
		}
		return -1;
	}

	private static void start(final ServerPlayer player, final Target target, final boolean map) {
		Trip old = TRIPS.put(player.getUUID(), new Trip(target, map));
		if (old != null && old.asked) {
			ServerLevel level = player.level().getServer().getLevel(old.target.dimension());
			if (level != null) {
				level.getChunkSource().removeTicketWithRadius(TICKET, old.chunk, 1);
			}
		}
		player.sendOverlayMessage(Component.translatable("message.minecraft_mode.lair.travel_wait", Component.translatable(target.lair().nameKey()))
			.withStyle(ChatFormatting.AQUA));
	}

	private static void tick(final MinecraftServer server) {
		Iterator<Map.Entry<UUID, Trip>> it = TRIPS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Trip> entry = it.next();
			Trip trip = entry.getValue();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			ServerLevel level = server.getLevel(trip.target.dimension());
			if (level == null) {
				it.remove();
				continue;
			}
			ServerChunkCache cache = level.getChunkSource();
			if (player == null) {
				release(cache, trip);
				it.remove();
				continue;
			}
			if (!trip.asked) {
				cache.addTicketWithRadius(TICKET, trip.chunk, 1);
				trip.asked = true;
			}
			if (loaded(cache, trip.chunk)) {
				release(cache, trip);
				it.remove();
				arrive(player, level, trip);
			} else if (++trip.waited > WAIT_TICKS) {
				release(cache, trip);
				it.remove();
				fail(player, trip.target, trip.map);
			}
		}
	}

	private static void release(final ServerChunkCache cache, final Trip trip) {
		if (trip.asked) {
			cache.removeTicketWithRadius(TICKET, trip.chunk, 1);
		}
	}

	/** The chunk and its neighbours are fully loaded, so the gate is built and the ground around it is final. */
	private static boolean loaded(final ServerChunkCache cache, final ChunkPos pos) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (cache.getChunkNow(pos.x() + dx, pos.z() + dz) == null) {
					return false;
				}
			}
		}
		return true;
	}

	/** A trip that could not end: a map trip keeps its map, the way back just did not open. */
	private static void fail(final ServerPlayer player, final Target target, final boolean map) {
		player.sendSystemMessage(Component.translatable(map ? "message.minecraft_mode.lair.travel_failed" : "message.minecraft_mode.lair.exit_failed", Component.translatable(target.lair().nameKey()))
			.withStyle(ChatFormatting.RED));
	}

	/** The gate's chunks are in: check everything again, use up the map (map trips) and take the player to a safe spot by the gate. */
	private static void arrive(final ServerPlayer player, final ServerLevel level, final Trip trip) {
		Target target = trip.target;
		if (player.level() != level || !allowed(player) || !player.isAlive()) {
			fail(player, target, trip.map);
			return;
		}
		if (trip.map && clearedThisCycle(player, target)) {
			player.sendOverlayMessage(Component.translatable("message.minecraft_mode.lair.travel_cleared").withStyle(ChatFormatting.RED));
			return;
		}
		BlockPos spot = safeSpot(level, target.entrance());
		if (spot == null) {
			MinecraftMode.LOGGER.warn("No safe spot by the gate of {} at {}", target.lair().id(), target.entrance());
			fail(player, target, trip.map);
			return;
		}
		if (trip.map && !player.isCreative()) {
			int slot = mapSlot(player);
			if (slot < 0) {
				player.sendOverlayMessage(Component.translatable("message.minecraft_mode.lair.travel_no_map").withStyle(ChatFormatting.RED));
				return;
			}
			player.getInventory().getItem(slot).shrink(1);
			player.getInventory().setChanged();
		}
		teleport(player, level, spot);
		player.sendOverlayMessage(Component.translatable("message.minecraft_mode.lair.travel_arrived", Component.translatable(target.lair().nameKey()))
			.withStyle(ChatFormatting.GOLD));
	}

	private static void teleport(final ServerPlayer player, final ServerLevel level, final BlockPos spot) {
		if (player.getVehicle() != null) {
			player.stopRiding();
		}
		player.level().sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY(0.5), player.getZ(), 30, 0.4, 0.8, 0.4, 0.4);
		// facing north: the gate is north of the spot
		player.teleportTo(level, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
		player.resetFallDistance();
		level.playSound(null, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.0F);
	}

	/**
	 * A safe feet position at or near {@code nominal}: sturdy ground that does not burn, and two free blocks without liquid. Searched in growing
	 * squares up to {@link #SPOT_SEARCH} blocks out and three blocks up or down; null when there is none (the chunks must be loaded).
	 */
	public static @Nullable BlockPos safeSpot(final ServerLevel level, final BlockPos nominal) {
		int[] dys = {0, 1, -1, 2, -2, 3, -3};
		for (int r = 0; r <= SPOT_SEARCH; r++) {
			for (int dy : dys) {
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
							continue;
						}
						BlockPos feet = nominal.offset(dx, dy, dz);
						if (safe(level, feet)) {
							return feet;
						}
					}
				}
			}
		}
		return null;
	}

	public static boolean safe(final ServerLevel level, final BlockPos feet) {
		if (!level.isLoaded(feet) || !level.isInWorldBounds(feet) || !level.getWorldBorder().isWithinBounds(feet)) {
			return false;
		}
		BlockPos below = feet.below();
		BlockState ground = level.getBlockState(below);
		if (!ground.isFaceSturdy(level, below, Direction.UP) || !ground.getFluidState().isEmpty() || ground.is(Blocks.MAGMA_BLOCK)
			|| ground.is(BlockTags.CAMPFIRES) || ground.is(Blocks.POWDER_SNOW)) {
			return false;
		}
		return free(level, feet) && free(level, feet.above());
	}

	private static boolean free(final ServerLevel level, final BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty() && !state.is(BlockTags.FIRE)
			&& !state.is(Blocks.POWDER_SNOW) && !state.is(Blocks.COBWEB) && !state.is(Blocks.SWEET_BERRY_BUSH);
	}

	// ------------------------------------------------------------------ clearing: next map and the exit

	/**
	 * {@code player} opened {@code chest}'s treasure for the first time this cycle: remember the lair as cleared, hand out the next map and
	 * offer the way back to the gate when the chest knows it.
	 */
	public static void cleared(final ServerPlayer player, final LairChestBlockEntity chest, final LairDef def) {
		ServerLevel level = player.level();
		markCleared(player, key(level.dimension(), chest.getBlockPos()));
		// at the level cap the next map is a tier 1 map, the way into tier lairs (0.5.49)
		boolean capped = JobProgression.get(player).level() >= JobProgression.MAX_LEVEL;
		Bags.give(player, capped ? LairTier.map(1, 1) : new ItemStack(ModItems.LAIR_MAP));
		player.sendSystemMessage(Component.translatable(capped ? "message.minecraft_mode.lair.next_tier_map" : "message.minecraft_mode.lair.next_map")
			.withStyle(ChatFormatting.AQUA));
		BlockPos exit = chest.exit(level);
		if (exit == null) {
			return;
		}
		EXITS.put(player.getUUID(), new Target(level.dimension(), def, chest.getBlockPos(), exit, exit));
		if (ServerPlayNetworking.canSend(player, LairClearPayload.TYPE)) {
			ServerPlayNetworking.send(player, new LairClearPayload(def.id(), true));
		}
	}

	/** The way back out last offered to {@code player} (tests). */
	public static @Nullable Target exitOffer(final ServerPlayer player) {
		return EXITS.get(player.getUUID());
	}

	/** How far from the goal chest a player may still ask for the way back to the gate. */
	public static double exitRange(final LairDef def) {
		return def.size() + LairPiece.GROUNDS;
	}

	/** {@code player} asked for the way back to the gate of the lair they just cleared. */
	public static boolean exit(final ServerPlayer player) {
		Target target = EXITS.get(player.getUUID());
		if (target == null) {
			return false;
		}
		double range = exitRange(target.lair());
		if (player.level().dimension() != target.dimension() || !allowed(player)
			|| player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(target.goal())) > range * range) {
			player.sendOverlayMessage(Component.translatable("message.minecraft_mode.lair.exit_far", Component.translatable(target.lair().nameKey()))
				.withStyle(ChatFormatting.RED));
			return false;
		}
		if (!clearedThisCycle(player, target)) {
			return false;
		}
		EXITS.remove(player.getUUID());
		start(player, target, false);
		return true;
	}

	/** Whether a trip is waiting for its chunks (tests). */
	public static boolean travelling(final ServerPlayer player) {
		return TRIPS.containsKey(player.getUUID());
	}

	private LairTravel() {
	}
}
