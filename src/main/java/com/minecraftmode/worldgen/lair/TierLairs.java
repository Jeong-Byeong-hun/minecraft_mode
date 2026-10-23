package com.minecraftmode.worldgen.lair;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.bag.Bags;
import com.minecraftmode.companion.Companions;
import com.minecraftmode.dungeon.DungeonAffix;
import com.minecraftmode.dungeon.DungeonDimension;
import com.minecraftmode.dungeon.Dungeons;
import com.minecraftmode.entity.MobPower;
import com.minecraftmode.entity.SpaceCheck;
import com.minecraftmode.entity.combat.Attacks;
import com.minecraftmode.entity.combat.Telegraph;
import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMob;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.job.JobEvents;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.quest.TrialHunts;
import com.minecraftmode.jump.JumpForest;
import com.minecraftmode.market.AuctionHouse;
import com.minecraftmode.network.TierOfferPayload;
import com.minecraftmode.progress.Progress;
import com.minecraftmode.raid.InstanceSweep;
import com.minecraftmode.raid.Parties;
import com.minecraftmode.raid.RaidDamage;
import com.minecraftmode.raid.Raids;
import com.minecraftmode.registry.ModBlocks;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Tier lairs (0.5.49): a tier lair map ({@link LairTier}) opens a lair of its own for the reader's party in the dungeon dimension, like a map
 * in Path of Exile. One of the 22 lairs is built in a free slot (the same {@link LairPiece} as in the world, a new seed every time), its
 * corridors hold packs of the region's monsters and a few named ones, and its lord waits at the goal, all at Lv 100 times the tier
 * ({@link LairTier#health}/{@link LairTier#damage}, party size like dungeons) with dungeon affixes from tier 6. Killing the lord pays every
 * member there the lair treasure times {@link LairTier#rewardScale}, the next maps ({@link LairTier#nextMaps}), evolution shards and cores
 * and imbue scrolls; then the lair closes after {@link #VICTORY_TICKS}.
 * <p>
 * Only class Lv 100 characters go in. Falling wakes the player at the gate up to {@link #LIVES} times, then sends them home. Runs live in
 * server memory: a server stop or a party that all disconnected gets the map back in the leader's mailbox; leaving or falling out does not.
 * Chunks are asked for with a loading ticket and the lair is built once they are all there (never blocking loads), one lair per tick.
 */
public final class TierLairs {
	/** The slots: x {@link #ORIGIN_X}, z = slot x {@link #SPACING} (16 chunks: no two slots share a chunk ticket). */
	public static final int ORIGIN_X = 8192;
	public static final int SPACING = 256;
	private static final int MAX_SLOTS = 32;
	/** Maze floor of every tier lair; underground ones put their tower {@link #SURFACE_RISE} higher. */
	public static final int FLOOR = DungeonDimension.FLOOR_Y;
	public static final int SURFACE_RISE = 24;
	/** Blocks of ground around the apron, fenced with barriers so nobody walks into the void. */
	public static final int PLATE = 6;
	/** How far (blocks) party members may be from the reader to come along. */
	public static final double GATHER_RANGE = 24.0;
	/** Falls a member may take; the next one sends them home. */
	public static final int LIVES = 3;
	public static final int VICTORY_TICKS = 20 * 60;
	/** Ticks a new lair waits for its chunks before the run is given up (the map comes back). */
	public static final int WAIT_TICKS = 600;
	/** Monster packs: this many plus one per maze cell of a side. */
	public static final int PACKS = 10;
	/** Named monsters (not the lord) roaming the corridors. */
	public static final int ELITES = 3;
	/** Monsters of a pack. */
	public static final int PACK_MIN = 2;
	public static final int PACK_MAX = 3;
	/** Packs keep this many cells (maze steps) away from the gate. */
	public static final int SAFE_STEPS = 3;

	private static final TicketType TICKET = Registry.register(BuiltInRegistries.TICKET_TYPE, MinecraftMode.id("tier_lair"),
		new TicketType(TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING));
	private static final Identifier SCALE_ID = MinecraftMode.id("tier_lair_scale");
	private static final String RAGING_TAG = "minecraft_mode_tier_raging";

	public enum State {
		/** Waiting for the slot's chunks; the players are still at home. */
		BUILDING,
		RUN,
		/** The lord fell; the lair stays open for {@link #VICTORY_TICKS}. */
		VICTORY,
		CLOSED
	}

	/** A participant: where they came from, whether they are still in, how often they fell, whether they left by disconnecting. */
	public static final class Member {
		final UUID id;
		final ResourceKey<Level> returnLevel;
		final Vec3 returnPos;
		final float returnYaw;
		boolean active = true;
		boolean offline;
		int falls;

		Member(final ServerPlayer player) {
			this.id = player.getUUID();
			this.returnLevel = player.level().dimension();
			this.returnPos = player.position();
			this.returnYaw = player.getYRot();
		}

		public int falls() {
			return this.falls;
		}

		public boolean active() {
			return this.active;
		}
	}

	/** One party's run through one tier lair. */
	public static final class Run {
		final int id;
		final int slot;
		final LairDef def;
		final int tier;
		final LairPiece piece;
		final UUID leader;
		final String sweepTag = InstanceSweep.newTag();
		final List<DungeonAffix> affixes;
		final Map<UUID, Member> members = new LinkedHashMap<>();
		final Set<UUID> mobs = new HashSet<>();
		final Map<UUID, Integer> bolsters = new HashMap<>();
		final ChunkPos chunk;
		final int radius;
		State state = State.BUILDING;
		int timer;
		boolean asked;
		@Nullable UUID lordId;

		Run(final int id, final int slot, final LairDef def, final int tier, final LairPiece piece, final UUID leader, final List<DungeonAffix> affixes) {
			this.id = id;
			this.slot = slot;
			this.def = def;
			this.tier = tier;
			this.piece = piece;
			this.leader = leader;
			this.affixes = List.copyOf(affixes);
			this.chunk = ChunkPos.containing(piece.center());
			this.radius = reach(def) / 16 + 1;
		}

		public int id() {
			return this.id;
		}

		public LairDef def() {
			return this.def;
		}

		public int tier() {
			return this.tier;
		}

		public State state() {
			return this.state;
		}

		public LairPiece piece() {
			return this.piece;
		}

		public List<DungeonAffix> affixes() {
			return this.affixes;
		}

		public @Nullable UUID lordId() {
			return this.lordId;
		}

		public Set<UUID> mobs() {
			return Set.copyOf(this.mobs);
		}

		public @Nullable Member member(final UUID id) {
			return this.members.get(id);
		}

		public boolean has(final DungeonAffix affix) {
			return this.affixes.contains(affix);
		}

		boolean anyActive() {
			return this.members.values().stream().anyMatch(m -> m.active);
		}

		/** Everything that belongs to the run: what the sweep tags and closing removes. */
		AABB area() {
			BlockPos c = this.piece.center();
			int r = reach(this.def) + 8;
			return new AABB(c.getX() - r, FLOOR - 40, c.getZ() - r, c.getX() + r + 1, FLOOR + 200, c.getZ() + r + 1);
		}

		boolean inside(final Vec3 pos, final double margin) {
			return this.area().inflate(margin).contains(pos);
		}
	}

	private static final Map<Integer, Run> RUNS = new LinkedHashMap<>();
	private static final Map<UUID, Run> BY_PLAYER = new HashMap<>();
	/** Map tier last offered to a reader, waiting for their yes. */
	private static final Map<UUID, Integer> OFFERS = new HashMap<>();
	/** Players to move at the end of the tick: true = wake at the gate, false = home. */
	private static final Map<UUID, Boolean> PENDING = new LinkedHashMap<>();
	private static int nextId = 1;

	private TierLairs() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(TierLairs::tick);
		ServerLivingEntityEvents.AFTER_DEATH.register(TierLairs::afterDeath);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> server.execute(() -> {
			UUID id = handler.player.getUUID();
			OFFERS.remove(id);
			Run run = BY_PLAYER.get(id);
			Member member = run == null ? null : run.member(id);
			if (member != null && member.active) {
				member.active = false;
				member.offline = true;
			}
		}));
		// a run cut short by the server stopping is nobody's fault: the map waits in the leader's mailbox
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (Run run : RUNS.values()) {
				if (run.state == State.BUILDING || run.state == State.RUN) {
					AuctionHouse.get(server).sendItem(run.leader, LairTier.map(run.tier, 1));
				}
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			RUNS.clear();
			BY_PLAYER.clear();
			OFFERS.clear();
			PENDING.clear();
		});
	}

	// ------------------------------------------------------------------ queries (Dungeons asks these for the shared dimension)

	/** Half the side of what a lair of {@code def} builds: maze, apron and the fenced plate. */
	static int reach(final LairDef def) {
		return def.size() / 2 + LairPiece.APRON + PLATE + 2;
	}

	/** The running tier lair {@code player} is a member of (still in), or null. */
	public static @Nullable Run runOf(final Player player) {
		Run run = BY_PLAYER.get(player.getUUID());
		if (run == null || run.state == State.CLOSED || run.state == State.BUILDING) {
			return null;
		}
		Member member = run.member(player.getUUID());
		return member != null && member.active ? run : null;
	}

	/** Whether {@code player} is in or waiting for a tier lair. */
	public static boolean busy(final Player player) {
		Run run = BY_PLAYER.get(player.getUUID());
		return run != null && run.state != State.CLOSED && run.member(player.getUUID()) != null && run.member(player.getUUID()).active;
	}

	public static List<Run> runs() {
		return List.copyOf(RUNS.values());
	}

	/** Inside a running tier lair: its monsters are not cleaned up as strays of the dungeon dimension. */
	public static boolean insideAny(final Vec3 pos) {
		return tagAt(pos) != null;
	}

	/** The sweep tag of the tier lair whose area holds {@code pos} ({@link InstanceSweep}), or null. */
	public static @Nullable String tagAt(final Vec3 pos) {
		for (Run run : RUNS.values()) {
			if (run.state != State.CLOSED && run.inside(pos, 0.0)) {
				return run.sweepTag;
			}
		}
		return null;
	}

	// ------------------------------------------------------------------ reading a map: offer and confirm

	/** Why {@code player} cannot go into a tier lair now (a message key), or null. */
	private static @Nullable String problem(final ServerPlayer player) {
		if (JobProgression.get(player).level() < JobProgression.MAX_LEVEL) {
			return "level";
		}
		if (busy(player) || Dungeons.instanceOf(player) != null || Raids.instanceOf(player) != null || TrialHunts.hunting(player) || JumpForest.busy(player)) {
			return "busy";
		}
		if (!LairTravel.allowed(player)) {
			return "here";
		}
		if (!player.isAlive()) {
			return "dead";
		}
		return null;
	}

	/** The members who would come along with {@code leader}: online party members near them who may go (the leader first). */
	private static List<ServerPlayer> party(final ServerPlayer leader, final List<Component> problems) {
		List<ServerPlayer> going = new ArrayList<>();
		going.add(leader);
		for (ServerPlayer p : Parties.onlineMembers(leader)) {
			if (p == leader) {
				continue;
			}
			String problem = problem(p);
			if (problem == null && (p.level() != leader.level() || p.distanceTo(leader) > GATHER_RANGE)) {
				problem = "too_far";
			}
			if (problem == null && going.size() >= Dungeons.MAX_PARTY) {
				problem = "too_many";
			}
			if (problem == null) {
				going.add(p);
			} else {
				problems.add(Component.literal(" - ").append(p.getDisplayName()).append(": ").append(msg("problem." + problem)).withStyle(ChatFormatting.RED));
			}
		}
		return going;
	}

	/** {@code player} read a map of {@code tier}: check, then ask on the client whether to open the lair. */
	public static boolean offer(final ServerPlayer player, final int tier) {
		String problem = problem(player);
		if (problem != null) {
			player.sendOverlayMessage(msg("problem." + problem).withStyle(ChatFormatting.RED));
			return false;
		}
		if (!Parties.canLead(player)) {
			player.sendOverlayMessage(msg("problem.leader").withStyle(ChatFormatting.RED));
			return false;
		}
		List<Component> problems = new ArrayList<>();
		List<ServerPlayer> going = party(player, problems);
		problems.forEach(player::sendSystemMessage);
		OFFERS.put(player.getUUID(), tier);
		if (ServerPlayNetworking.canSend(player, TierOfferPayload.TYPE)) {
			ServerPlayNetworking.send(player, new TierOfferPayload(tier, going.stream().map(ServerPlayer::getPlainTextName).toList()));
		}
		return true;
	}

	/** The tier last offered to {@code player} (tests). */
	public static @Nullable Integer offered(final ServerPlayer player) {
		return OFFERS.get(player.getUUID());
	}

	/** {@code player} said yes: check everything again, use up one map of the tier and start building. */
	public static @Nullable Run confirm(final ServerPlayer player) {
		Integer tier = OFFERS.remove(player.getUUID());
		if (tier == null) {
			return null;
		}
		String problem = problem(player);
		if (problem != null || !Parties.canLead(player)) {
			player.sendOverlayMessage(msg("problem." + (problem == null ? "leader" : problem)).withStyle(ChatFormatting.RED));
			return null;
		}
		int slot = mapSlot(player, tier);
		if (slot < 0 && !player.isCreative()) {
			player.sendOverlayMessage(msg("no_map", tier).withStyle(ChatFormatting.RED));
			return null;
		}
		List<Component> problems = new ArrayList<>();
		List<ServerPlayer> going = party(player, problems);
		Run run = start(player.level().getServer(), going, tier, player.getUUID(), player.getRandom());
		if (run == null) {
			return null;
		}
		if (slot >= 0 && !player.isCreative()) {
			player.getInventory().getItem(slot).shrink(1);
			player.getInventory().setChanged();
		}
		return run;
	}

	/** The inventory slot of a lair map of {@code tier}: the hands first; -1 when there is none. */
	private static int mapSlot(final ServerPlayer player, final int tier) {
		Inventory inventory = player.getInventory();
		if (LairTier.of(player.getItemInHand(InteractionHand.MAIN_HAND)) == tier) {
			return inventory.getSelectedSlot();
		}
		if (LairTier.of(player.getItemInHand(InteractionHand.OFF_HAND)) == tier) {
			return Inventory.SLOT_OFFHAND;
		}
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (LairTier.of(inventory.getItem(i)) == tier) {
				return i;
			}
		}
		return -1;
	}

	// ------------------------------------------------------------------ starting a run

	/**
	 * Picks a lair and a free slot and starts waiting for its chunks (no checks: the map, {@code /lair tier} and tests use it). {@code def}
	 * null picks one of the 22 at random. Returns null when no slot is free.
	 */
	public static @Nullable Run start(final MinecraftServer server, final List<ServerPlayer> players, final int tier, final UUID leader, final RandomSource random) {
		List<LairDef> defs = new ArrayList<>(NamedLairs.all());
		return start(server, players, tier, leader, defs.get(random.nextInt(defs.size())), random);
	}

	public static @Nullable Run start(final MinecraftServer server, final List<ServerPlayer> players, final int tier, final UUID leader, final LairDef def,
		final RandomSource random) {
		if (DungeonDimension.level(server) == null || players.isEmpty()) {
			return null;
		}
		int slot = freeSlot();
		if (slot < 0) {
			players.forEach(p -> p.sendSystemMessage(msg("full").withStyle(ChatFormatting.RED)));
			return null;
		}
		int cx = ORIGIN_X + SPACING / 2;
		int cz = slot * SPACING + SPACING / 2;
		LairPiece piece = new LairPiece(def, cx, FLOOR, cz, FLOOR + SURFACE_RISE, random.nextLong());
		Run run = new Run(nextId++, slot, def, LairTier.clamp(tier), piece, leader, LairTier.affixes(tier, random));
		RUNS.put(run.id, run);
		for (ServerPlayer p : players) {
			Run old = BY_PLAYER.put(p.getUUID(), run);
			if (old != null && old != run && old.member(p.getUUID()) != null) {
				old.member(p.getUUID()).active = false;
			}
			run.members.put(p.getUUID(), new Member(p));
			p.sendSystemMessage(msg("opening", run.tier).withStyle(ChatFormatting.AQUA));
		}
		return run;
	}

	private static int freeSlot() {
		boolean[] used = new boolean[MAX_SLOTS];
		for (Run run : RUNS.values()) {
			used[run.slot] = true;
		}
		for (int i = 0; i < MAX_SLOTS; i++) {
			if (!used[i]) {
				return i;
			}
		}
		return -1;
	}

	// ------------------------------------------------------------------ ticking

	private static void tick(final MinecraftServer server) {
		if (!PENDING.isEmpty()) {
			Map<UUID, Boolean> due = new LinkedHashMap<>(PENDING);
			PENDING.clear();
			for (Map.Entry<UUID, Boolean> e : due.entrySet()) {
				ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
				if (p != null && DungeonDimension.is(p.level())) {
					if (e.getValue()) {
						wake(p);
					} else {
						sendHome(server, p);
					}
				}
			}
		}
		if (RUNS.isEmpty()) {
			return;
		}
		ServerLevel level = DungeonDimension.level(server);
		boolean built = false;
		List<Run> closing = new ArrayList<>();
		for (Run run : RUNS.values()) {
			if (level == null) {
				closing.add(run);
				continue;
			}
			run.timer++;
			switch (run.state) {
				case BUILDING -> {
					if (!built && building(server, level, run)) {
						built = true;
					}
				}
				case RUN -> running(server, level, run);
				case VICTORY -> {
					if (run.timer >= VICTORY_TICKS || !run.anyActive()) {
						run.state = State.CLOSED;
					} else if (run.timer == VICTORY_TICKS - 20 * 10) {
						tell(server, run, msg("closing", 10).withStyle(ChatFormatting.YELLOW));
					}
				}
				case CLOSED -> closing.add(run);
			}
		}
		for (Run run : closing) {
			close(server, run);
		}
	}

	/** Waits for the slot's chunks; once they are in, builds the lair and takes the party in. Returns whether it built this tick. */
	private static boolean building(final MinecraftServer server, final ServerLevel level, final Run run) {
		ServerChunkCache cache = level.getChunkSource();
		if (!run.asked) {
			cache.addTicketWithRadius(TICKET, run.chunk, run.radius);
			run.asked = true;
		}
		for (int dx = -run.radius; dx <= run.radius; dx++) {
			for (int dz = -run.radius; dz <= run.radius; dz++) {
				if (cache.getChunkNow(run.chunk.x() + dx, run.chunk.z() + dz) == null) {
					if (run.timer > WAIT_TICKS) {
						MinecraftMode.LOGGER.warn("Tier lair {} gave up waiting for its chunks", run.id);
						giveBackMap(server, run);
						run.state = State.CLOSED;
					}
					return false;
				}
			}
		}
		build(level, run);
		InstanceSweep.clear(level, run.area());
		run.state = State.RUN;
		run.timer = 0;
		enter(server, level, run);
		populate(level, run);
		return true;
	}

	/** Puts the leader's map back into their mailbox (a run that never got going through nobody's fault). */
	private static void giveBackMap(final MinecraftServer server, final Run run) {
		AuctionHouse.get(server).sendItem(run.leader, LairTier.map(run.tier, 1));
		ServerPlayer leader = server.getPlayerList().getPlayer(run.leader);
		if (leader != null) {
			leader.sendSystemMessage(msg("map_back", run.tier).withStyle(ChatFormatting.YELLOW));
		}
	}

	private static void running(final MinecraftServer server, final ServerLevel level, final Run run) {
		if (run.timer % 20 == 0) {
			for (Member m : run.members.values()) {
				ServerPlayer p = server.getPlayerList().getPlayer(m.id);
				if (m.active && (p == null || p.level() != level || !run.inside(p.position(), 4.0))) {
					m.active = false;
					m.offline |= p == null;
				}
			}
			if (!run.anyActive()) {
				boolean allOffline = run.members.values().stream().allMatch(m -> m.offline);
				tell(server, run, msg("failed", run.tier).withStyle(ChatFormatting.RED));
				if (allOffline) {
					giveBackMap(server, run);
				}
				run.state = State.CLOSED;
				return;
			}
			run.mobs.removeIf(id -> !(level.getEntity(id) instanceof LivingEntity e) || !e.isAlive());
		}
		List<ServerPlayer> players = activePlayers(server, run);
		if (run.has(DungeonAffix.RAGING) && run.timer % 20 == 5) {
			for (UUID id : run.mobs) {
				if (level.getEntity(id) instanceof Mob mob && mob.getHealth() < mob.getMaxHealth() * 0.3F && !mob.entityTags().contains(RAGING_TAG)) {
					mob.addTag(RAGING_TAG);
					mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 60, 1));
					mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 60, 0));
					MobPower.scale(id, 1.5F);
					level.sendParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
				}
			}
		}
		if (run.has(DungeonAffix.VOLCANIC) && run.timer % (DungeonAffix.VOLCANIC_INTERVAL * 20) == 0 && !players.isEmpty()) {
			ServerPlayer target = players.get(level.getRandom().nextInt(players.size()));
			Vec3 at = target.position();
			Telegraph.circle(level, at, DungeonAffix.VOLCANIC_RADIUS, 30, Telegraph.RED, () -> {
				if (run.state != State.RUN) {
					return;
				}
				for (LivingEntity e : Attacks.inCircle(level, at, DungeonAffix.VOLCANIC_RADIUS, 3.0)) {
					if (e instanceof Player player && !player.isCreative() && !player.isSpectator()) {
						RaidDamage.portion(level, player, null, DungeonAffix.VOLCANIC_PORTION);
					}
				}
				level.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.2, at.z, 20, 1.2, 0.2, 1.2, 0.0);
				level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.5, at.z, 30, 1.0, 0.8, 1.0, 0.05);
			});
		}
	}

	// ------------------------------------------------------------------ building and filling the lair

	/**
	 * Builds the lair in its slot: a plate of ground under surface, Nether and End lairs (their foundation would otherwise reach 40 blocks
	 * into the void) fenced with barriers, the lair itself chunk by chunk like world generation, then the treasure chests taken out: the
	 * lord's kill pays here.
	 */
	static void build(final ServerLevel level, final Run run) {
		LairDef def = run.def;
		LairPiece piece = run.piece;
		BlockPos c = piece.center();
		int cx = c.getX();
		int cz = c.getZ();
		int half = def.size() / 2;
		if (def.setting() != LairDef.Setting.UNDERGROUND) {
			BlockState ground = def.palette().foundation().defaultBlockState();
			BlockState barrier = Blocks.BARRIER.defaultBlockState();
			int r = half + LairPiece.APRON + PLATE;
			BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
			for (int x = cx - r; x <= cx + r; x++) {
				for (int z = cz - r; z <= cz + r; z++) {
					level.setBlock(at.set(x, FLOOR - 1, z), ground, 2);
					level.setBlock(at.set(x, FLOOR, z), ground, 2);
					if (Math.abs(x - cx) == r || Math.abs(z - cz) == r) {
						for (int y = FLOOR + 1; y <= FLOOR + 4; y++) {
							level.setBlock(at.set(x, y, z), barrier, 2);
						}
					}
				}
			}
		}
		BoundingBox box = piece.getBoundingBox();
		int reach = reach(def);
		for (int chunkX = (cx - reach) >> 4; chunkX <= (cx + reach) >> 4; chunkX++) {
			for (int chunkZ = (cz - reach) >> 4; chunkZ <= (cz + reach) >> 4; chunkZ++) {
				BoundingBox chunk = new BoundingBox(chunkX << 4, box.minY(), chunkZ << 4, (chunkX << 4) + 15, box.maxY(), (chunkZ << 4) + 15);
				piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(run.id), chunk,
					new ChunkPos(chunkX, chunkZ), new BlockPos(chunkX << 4, box.minY(), chunkZ << 4));
			}
		}
		for (BlockPos chest : chests(piece)) {
			if (level.getBlockState(chest).is(ModBlocks.LAIR_CHEST) || level.getBlockState(chest).is(ModBlocks.LAIR_CACHE)) {
				level.removeBlockEntity(chest);
				level.setBlock(chest, Blocks.AIR.defaultBlockState(), 2);
			}
		}
	}

	/** The goal chest and the dead-end caches a lair piece places. */
	private static List<BlockPos> chests(final LairPiece piece) {
		List<BlockPos> out = new ArrayList<>();
		out.add(piece.goalPos());
		for (int[] cache : piece.maze().caches()) {
			out.add(cellFloor(piece, cache[0], cache[1]));
		}
		return out;
	}

	/** Feet position in the middle of maze cell {@code i}, {@code j} (where chests stand). */
	static BlockPos cellFloor(final LairPiece piece, final int i, final int j) {
		LairDef def = piece.def();
		int size = piece.layout().size(def);
		BlockPos c = piece.center();
		int x0 = c.getX() - size / 2;
		int z0 = c.getZ() - size / 2;
		return new BlockPos(x0 + 2 + piece.layout().cell() * i, FLOOR + 1, z0 + 2 + piece.layout().cell() * j);
	}

	/** Where the party stands at the start and wakes after a fall: just outside the gate. */
	public static Vec3 gate(final Run run, final int index) {
		BlockPos e = run.piece.entrance();
		return new Vec3(e.getX() + 0.5 + (index % 2 == 0 ? 0 : index == 1 ? 1 : -1), e.getY(), e.getZ() + 0.5 + (index >= 2 ? 1 : 0));
	}

	private static void enter(final MinecraftServer server, final ServerLevel level, final Run run) {
		int index = 0;
		for (Member m : run.members.values()) {
			ServerPlayer p = server.getPlayerList().getPlayer(m.id);
			if (p == null || !p.isAlive() || BY_PLAYER.get(m.id) != run) {
				m.active = false;
				m.offline |= p == null;
				continue;
			}
			if (p.getVehicle() != null) {
				p.stopRiding();
			}
			Vec3 to = gate(run, index++);
			p.teleportTo(level, to.x, to.y, to.z, Set.of(), 180.0F, 0.0F, true);
			p.setHealth(p.getMaxHealth());
			p.clearFire();
			p.resetFallDistance();
			p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.translatable(run.def.nameKey()).withStyle(ChatFormatting.GOLD)));
			p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(msg("tier", run.tier).withStyle(ChatFormatting.LIGHT_PURPLE)));
			p.sendSystemMessage(msg("entered", Component.translatable(run.def.nameKey()), run.tier, LIVES).withStyle(ChatFormatting.GOLD));
			for (DungeonAffix affix : run.affixes) {
				p.sendSystemMessage(Component.literal(" ◆ ").append(Component.translatable(affix.nameKey())).append(": ")
					.append(Component.translatable(affix.descKey())).withStyle(ChatFormatting.LIGHT_PURPLE));
			}
		}
		Vec3 gate = gate(run, 0);
		level.playSound(null, gate.x, gate.y, gate.z, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.5F, 0.8F);
	}

	/** Members going in at the start (the party size every monster is scaled for). */
	private static int partySize(final Run run) {
		return (int)Math.max(1, run.members.values().stream().filter(m -> m.active).count());
	}

	/** Packs in the corridors, a few named monsters and the lord at the goal. */
	private static void populate(final ServerLevel level, final Run run) {
		RandomSource random = level.getRandom();
		LairDef def = run.def;
		Maze maze = run.piece.maze();
		int party = partySize(run);
		float health = LairTier.health(run.tier);
		float damage = LairTier.damage(run.tier);
		boolean fortified = run.has(DungeonAffix.FORTIFIED);
		boolean tyrant = run.has(DungeonAffix.TYRANNICAL);
		List<int[]> cells = new ArrayList<>();
		for (int i = 0; i < maze.cells(); i++) {
			for (int j = 0; j < maze.cells(); j++) {
				if (maze.distance(i, j) >= SAFE_STEPS && !(i == maze.goalX() && j == maze.goalZ())) {
					cells.add(new int[] {i, j});
				}
			}
		}
		// packs of the region's monsters, about Lv 100 like a dungeon of power 100
		float packHealth = Dungeons.powerHealth(JobProgression.MAX_LEVEL) * health * Dungeons.partyScale(party) * (fortified ? DungeonAffix.FORTIFIED_HEALTH : 1.0F);
		float packDamage = Dungeons.powerDamage(JobProgression.MAX_LEVEL) * damage * (fortified ? DungeonAffix.FORTIFIED_DAMAGE : 1.0F);
		int packs = Math.min(cells.size(), PACKS + maze.cells());
		for (int p = 0; p < packs; p++) {
			int[] cell = cells.remove(random.nextInt(cells.size()));
			int count = PACK_MIN + random.nextInt(PACK_MAX - PACK_MIN + 1);
			for (int i = 0; i < count; i++) {
				Mob mob = spawn(level, run, def.mobs().pick(random), cell, random);
				if (mob != null) {
					scaleHealth(mob, packHealth);
					MobPower.set(mob.getUUID(), packDamage);
				}
			}
		}
		NamedDef named = def.named();
		Component tierTag = Component.literal("[T" + run.tier + "] ").withStyle(ChatFormatting.LIGHT_PURPLE);
		for (int e = 0; e < ELITES && !cells.isEmpty(); e++) {
			int[] cell = cells.remove(random.nextInt(cells.size()));
			if (spawn(level, run, NamedMobs.type(named), cell, random) instanceof NamedMob elite) {
				elite.empower(JobProgression.MAX_LEVEL, health * Dungeons.partyScale(party) * (fortified ? DungeonAffix.FORTIFIED_HEALTH : 1.0F),
					damage * (fortified ? DungeonAffix.FORTIFIED_DAMAGE : 1.0F), tierTag.copy().append(Component.translatable(named.nameKey()).withStyle(ChatFormatting.GOLD)));
			}
		}
		// the lord at the goal: a champion (boss bar, the lair's wrath, leashed to the goal, heals once)
		BlockPos home = run.piece.goalPos();
		if (NamedMobs.type(named).create(level, EntitySpawnReason.EVENT) instanceof NamedMob lord) {
			lord.snapTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
			lord.makeChampion(NamedMob.LORD_HEALTH * Dungeons.bossPartyScale(party) * (tyrant ? DungeonAffix.TYRANNICAL_HEALTH : 1.0F), 1.0F,
				"entity.minecraft_mode.tier_lord", home, NamedMob.LORD_LEASH);
			lord.healOnlyOnce();
			lord.empower(JobProgression.MAX_LEVEL, health * LairTier.LORD_HEALTH, NamedMob.LORD_DAMAGE * damage * (tyrant ? DungeonAffix.TYRANNICAL_DAMAGE : 1.0F),
				Component.translatable("entity.minecraft_mode.tier_lord", tierTag.copy().append(Component.translatable(named.nameKey()).withStyle(ChatFormatting.GOLD)))
					.withStyle(ChatFormatting.RED));
			Vec3 free = SpaceCheck.nearestFree(level, lord, lord.position(), SpaceCheck.SEARCH);
			if (free != null) {
				lord.snapTo(free.x, free.y, free.z, lord.getYRot(), 0.0F);
			}
			level.addFreshEntity(lord);
			SpaceCheck.growTo(level, lord, NamedMob.LORD_SCALE);
			run.lordId = lord.getUUID();
		}
	}

	private static @Nullable Mob spawn(final ServerLevel level, final Run run, final EntityType<?> type, final int[] cell, final RandomSource random) {
		if (!(type.create(level, EntitySpawnReason.EVENT) instanceof Mob mob)) {
			return null;
		}
		BlockPos at = cellFloor(run.piece, cell[0], cell[1]);
		mob.snapTo(at.getX() + 0.5 + random.nextDouble() - 0.5, at.getY(), at.getZ() + 0.5 + random.nextDouble() - 0.5, random.nextFloat() * 360.0F, 0.0F);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.EVENT, null);
		Vec3 free = SpaceCheck.nearestFree(level, mob, mob.position(), SpaceCheck.SEARCH);
		if (free == null) {
			return null;
		}
		mob.snapTo(free.x, free.y, free.z, mob.getYRot(), 0.0F);
		if (mob instanceof Hoglin hoglin) {
			hoglin.setImmuneToZombification(true);
		}
		if (mob instanceof AbstractPiglin piglin) {
			piglin.setImmuneToZombification(true);
		}
		mob.setPersistenceRequired();
		level.addFreshEntity(mob);
		run.mobs.add(mob.getUUID());
		return mob;
	}

	/** Multiplies a pack monster's health; past the vanilla cap the rest becomes toughness (like dungeon halls), and zombies call no help. */
	private static void scaleHealth(final LivingEntity mob, final float multiplier) {
		AttributeInstance reinforcements = mob.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
		if (reinforcements != null) {
			reinforcements.removeModifiers();
			reinforcements.setBaseValue(0.0);
		}
		AttributeInstance attr = mob.getAttribute(Attributes.MAX_HEALTH);
		if (attr == null) {
			return;
		}
		double base = attr.getValue();
		double wanted = base * multiplier;
		double kept = Math.min(wanted, MobPower.HEALTH_CAP);
		attr.addOrReplacePermanentModifier(new AttributeModifier(SCALE_ID, kept / base - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		MobPower.setToughness(mob.getUUID(), (float)(wanted / kept));
		mob.setHealth(mob.getMaxHealth());
	}

	// ------------------------------------------------------------------ deaths: monsters, the lord, players

	private static void afterDeath(final LivingEntity entity, final net.minecraft.world.damagesource.DamageSource source) {
		if (!DungeonDimension.is(entity.level()) || !(entity.level() instanceof ServerLevel level) || RUNS.isEmpty()) {
			return;
		}
		for (Run run : RUNS.values()) {
			if (entity.getUUID().equals(run.lordId) && run.state == State.RUN) {
				MobPower.clear(entity.getUUID());
				victory(level.getServer(), run);
				return;
			}
			if (run.mobs.remove(entity.getUUID())) {
				MobPower.clear(entity.getUUID());
				if (run.has(DungeonAffix.BOLSTERING)) {
					for (UUID id : run.mobs) {
						if (level.getEntity(id) instanceof Mob mob && mob.isAlive()
							&& mob.distanceToSqr(entity) <= DungeonAffix.BOLSTER_RANGE * DungeonAffix.BOLSTER_RANGE
							&& run.bolsters.merge(id, 1, Integer::sum) <= DungeonAffix.BOLSTER_MAX) {
							MobPower.setToughness(id, MobPower.toughness(mob) * (1.0F + DungeonAffix.BOLSTER));
							MobPower.scale(id, 1.0F + DungeonAffix.BOLSTER);
							level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 5, 0.3, 0.3, 0.3, 0.0);
						}
					}
				}
				return;
			}
		}
	}

	/**
	 * A player fell in the dungeon dimension (from {@code Dungeons.fall}): a member of a running tier lair wakes at its gate, or goes home on
	 * the fall past {@link #LIVES}. Returns whether this handled it.
	 */
	public static boolean fall(final ServerPlayer player) {
		Run run = runOf(player);
		if (run == null) {
			return false;
		}
		player.setHealth(1.0F);
		Member member = run.member(player.getUUID());
		member.falls++;
		MinecraftServer server = player.level().getServer();
		if (member.falls > LIVES) {
			member.active = false;
			tell(server, run, msg("out", player.getDisplayName()).withStyle(ChatFormatting.RED));
			PENDING.put(player.getUUID(), false);
		} else {
			tell(server, run, msg("fallen", player.getDisplayName(), LIVES - member.falls).withStyle(ChatFormatting.RED));
			PENDING.put(player.getUUID(), true);
		}
		return true;
	}

	/** {@code player} leaves (pause menu, {@code /dungeon leave}): home now, the run goes on for the others. */
	public static boolean leave(final ServerPlayer player) {
		Run run = BY_PLAYER.get(player.getUUID());
		Member member = run == null ? null : run.member(player.getUUID());
		if (member == null || !member.active) {
			return false;
		}
		member.active = false;
		PENDING.put(player.getUUID(), false);
		if (run.state == State.RUN) {
			tell(player.level().getServer(), run, msg("left", player.getDisplayName()).withStyle(ChatFormatting.YELLOW));
		}
		return true;
	}

	/**
	 * A player joined in the dungeon dimension (from {@code Dungeons}): back into their run when it still runs and they stand in it, else
	 * home to where they came from. Returns false when they were never in a tier lair (Dungeons sends them home itself).
	 */
	public static boolean rejoin(final ServerPlayer player) {
		Run run = BY_PLAYER.get(player.getUUID());
		Member member = run == null ? null : run.member(player.getUUID());
		if (member == null) {
			return false;
		}
		if ((run.state == State.RUN || run.state == State.VICTORY) && run.inside(player.position(), 2.0) && member.falls <= LIVES) {
			member.active = true;
			member.offline = false;
			return true;
		}
		member.active = false;
		PENDING.put(player.getUUID(), false);
		return true;
	}

	private static void wake(final ServerPlayer player) {
		Run run = runOf(player);
		if (run == null) {
			sendHome(player.level().getServer(), player);
			return;
		}
		Vec3 to = gate(run, 0);
		player.teleportTo(player.level(), to.x, to.y, to.z, Set.of(), 180.0F, 0.0F, true);
		player.setHealth(player.getMaxHealth());
		player.clearFire();
		player.resetFallDistance();
		player.setDeltaMovement(Vec3.ZERO);
		player.removeAllEffects();
	}

	private static void sendHome(final MinecraftServer server, final ServerPlayer player) {
		Run run = BY_PLAYER.get(player.getUUID());
		Member member = run == null ? null : run.member(player.getUUID());
		ServerLevel to = member == null ? null : server.getLevel(member.returnLevel);
		Vec3 pos;
		float yaw;
		if (to != null && !DungeonDimension.is(to)) {
			pos = member.returnPos;
			yaw = member.returnYaw;
		} else {
			to = server.overworld();
			BlockPos spawn = to.getRespawnData().pos();
			pos = Vec3.atBottomCenterOf(spawn);
			yaw = 180.0F;
		}
		player.teleportTo(to, pos.x, pos.y, pos.z, Set.of(), yaw, 0.0F, true);
		player.setHealth(player.getMaxHealth());
		player.clearFire();
		player.resetFallDistance();
		player.setDeltaMovement(Vec3.ZERO);
		if (run != null && BY_PLAYER.get(player.getUUID()) == run && (run.state == State.CLOSED || member != null && !member.active)) {
			BY_PLAYER.remove(player.getUUID());
		}
	}

	// ------------------------------------------------------------------ the clear

	private static void victory(final MinecraftServer server, final Run run) {
		run.state = State.VICTORY;
		run.timer = 0;
		tell(server, run, msg("victory", Component.translatable(run.def.nameKey()), run.tier, VICTORY_TICKS / 20).withStyle(ChatFormatting.GOLD));
		for (ServerPlayer p : activePlayers(server, run)) {
			reward(p, run, p.getRandom());
			Raids.ping(p, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE), 1.0F);
		}
	}

	/**
	 * One member's share: the lair treasure at Lv 100 {@link LairTier#rewardScale} times (whole treasures, the rest as a chance of one more),
	 * the treasure's experience scaled the same way, the next maps, evolution shards (and a core by chance from tier 11), an imbue scroll
	 * by chance (a greater one too from tier 11) and the lair's pet chance.
	 */
	static void reward(final ServerPlayer player, final Run run, final RandomSource random) {
		int tier = run.tier;
		float scale = LairTier.rewardScale(tier);
		int treasures = (int)scale + (random.nextFloat() < scale - (int)scale ? 1 : 0);
		for (int i = 0; i < treasures; i++) {
			for (ItemStack stack : LairLoot.goal(run.def.named(), JobProgression.get(player), JobProgression.MAX_LEVEL, random)) {
				Bags.giveOrMail(player, stack);
			}
		}
		int exp = LairExp.exp(player, JobProgression.MAX_LEVEL, LairTier.expShare(tier));
		JobEvents.gainExp(player, exp);
		Map<Integer, Integer> maps = new TreeMap<>();
		for (int next : LairTier.nextMaps(tier, random)) {
			maps.merge(next, 1, Integer::sum);
		}
		MutableComponent mapText = Component.empty();
		for (Map.Entry<Integer, Integer> e : maps.entrySet()) {
			Bags.giveOrMail(player, LairTier.map(e.getKey(), e.getValue()));
			if (!mapText.getSiblings().isEmpty()) {
				mapText.append(", ");
			}
			mapText.append(msg("map_count", e.getKey(), e.getValue()));
		}
		Bags.giveOrMail(player, new ItemStack(ModItems.EVOLUTION_SHARD, LairTier.shards(tier)));
		if (random.nextFloat() < LairTier.coreChance(tier)) {
			Bags.giveOrMail(player, new ItemStack(ModItems.EVOLUTION_CORE));
			player.sendSystemMessage(msg("core").withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		if (random.nextFloat() < LairTier.scrollChance(tier)) {
			ItemStack scroll = new ItemStack(LairTier.scroll(false, random));
			player.sendSystemMessage(msg("scroll", scroll.getHoverName()).withStyle(ChatFormatting.AQUA));
			Bags.giveOrMail(player, scroll);
		}
		if (random.nextFloat() < LairTier.greaterChance(tier)) {
			ItemStack scroll = new ItemStack(LairTier.scroll(true, random));
			player.sendSystemMessage(msg("scroll", scroll.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE));
			Bags.giveOrMail(player, scroll);
		}
		Companions.rollDrop(player, Companions.LAIR_DROP, net.minecraft.world.item.Rarity.RARE);
		Progress.lairCleared(player, run.def);
		player.sendSystemMessage(msg("reward", treasures, exp, mapText, LairTier.shards(tier)).withStyle(ChatFormatting.YELLOW));
	}

	// ------------------------------------------------------------------ closing

	/** Sends everyone still there home, hands what lies on the floor to the leader and removes the rest. */
	private static void close(final MinecraftServer server, final Run run) {
		run.state = State.CLOSED;
		RUNS.remove(run.id);
		ServerLevel level = DungeonDimension.level(server);
		for (Member m : run.members.values()) {
			ServerPlayer p = server.getPlayerList().getPlayer(m.id);
			if (p != null && level != null && p.level() == level && run.inside(p.position(), 16.0)) {
				sendHome(server, p);
			}
			if (BY_PLAYER.get(m.id) == run) {
				BY_PLAYER.remove(m.id);
			}
		}
		if (level != null) {
			ServerPlayer leader = server.getPlayerList().getPlayer(run.leader);
			for (Entity e : level.getEntitiesOfClass(Entity.class, run.area(), e -> !(e instanceof Player))) {
				if (e instanceof ItemEntity item && !item.getItem().isEmpty()) {
					if (leader != null) {
						Bags.giveOrMail(leader, item.getItem().copy());
					} else {
						AuctionHouse.get(server).sendItem(run.leader, item.getItem().copy());
					}
				}
				MobPower.clear(e.getUUID());
				e.discard();
			}
			if (run.asked) {
				level.getChunkSource().removeTicketWithRadius(TICKET, run.chunk, run.radius);
			}
		}
		for (UUID id : run.mobs) {
			MobPower.clear(id);
		}
		if (run.lordId != null) {
			MobPower.clear(run.lordId);
		}
	}

	/** Operator stop: the run closes on the next tick (its map is not given back). */
	public static boolean stop(final int id) {
		Run run = RUNS.get(id);
		if (run == null) {
			return false;
		}
		run.state = State.CLOSED;
		return true;
	}

	// ------------------------------------------------------------------ messages

	private static List<ServerPlayer> activePlayers(final MinecraftServer server, final Run run) {
		List<ServerPlayer> out = new ArrayList<>();
		for (Member m : run.members.values()) {
			ServerPlayer p = m.active ? server.getPlayerList().getPlayer(m.id) : null;
			if (p != null) {
				out.add(p);
			}
		}
		return out;
	}

	private static void tell(final MinecraftServer server, final Run run, final Component message) {
		for (Member m : run.members.values()) {
			ServerPlayer p = server.getPlayerList().getPlayer(m.id);
			if (p != null) {
				p.sendSystemMessage(message);
			}
		}
	}

	static MutableComponent msg(final String key, final Object... args) {
		return Component.translatable("message.minecraft_mode.tier_lair." + key, args);
	}

	/** The run with {@code id}, or null (tests, the admin screen). */
	public static @Nullable Run byId(final int id) {
		return RUNS.get(id);
	}
}
