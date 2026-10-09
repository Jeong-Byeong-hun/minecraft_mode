package com.minecraftmode.raid;

import com.minecraftmode.city.CityZone;
import com.minecraftmode.companion.Companions;
import com.minecraftmode.consumable.Consumables;
import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.entity.CityNpc;
import com.minecraftmode.entity.boss.RaidBoss;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.loot.EvolutionEtherItem;
import com.minecraftmode.loot.GearDrops;
import com.minecraftmode.progress.PlayerRecords;
import com.minecraftmode.progress.Progress;
import com.minecraftmode.progress.ResetCycle;
import com.minecraftmode.raid.loot.LootSessions;
import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Raid instances. The raid marshal (or {@code /raid start}) sends a party (each paying the entry fee) into a fresh arena slot of
 * the raid dimension: 10 second countdown, then the boss. Dying in a raid never kills: the player is
 * healed and sent back where they entered (items kept) and cannot rejoin that fight; when nobody is
 * left fighting the raid fails. When the boss falls every participant gets Evolution Ether, condensed
 * essence, consumables and job experience, the gear drops go to a {@link LootSessions loot session} (auction or
 * dice), and everyone is sent home after a minute (or earlier with {@code /raid leave}).
 */
public final class Raids {
	public static final int COUNTDOWN_TICKS = 200;
	public static final int VICTORY_TICKS = 20 * 60;
	/** The leader must be this close to the marshal. */
	public static final double MARSHAL_RANGE = 8.0;
	/** Party members must be this close to the leader to come along. */
	public static final double GATHER_RANGE = 24.0;
	private static final int SLOT_SPACING = 1024;
	private static final int MAX_SLOTS = 32;

	private static final Map<Integer, RaidInstance> INSTANCES = new LinkedHashMap<>();
	private static final Map<UUID, RaidInstance> BY_PLAYER = new HashMap<>();
	/** Players to send home at the end of this tick (fallen or stranded), with the instance they belonged to (or null). */
	private static final Map<UUID, @Nullable RaidInstance> PENDING_EJECT = new LinkedHashMap<>();
	private static int nextId = 1;

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Raids::tick);
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> !(entity instanceof ServerPlayer player) || !fall(player));
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(entity instanceof ServerPlayer player && PENDING_EJECT.containsKey(player.getUUID())));
		ServerPlayerEvents.JOIN.register(Raids::onJoin);
		// minions saved with an arena chunk (server stopped mid-fight) must not haunt the next fight there
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (RaidDimension.is(level) && entity instanceof Mob && !(entity instanceof RaidBoss) && !insideRunningArena(entity.position())) {
				entity.discard();
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> server.execute(() -> onLeave(handler.player.getUUID())));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			INSTANCES.clear();
			BY_PLAYER.clear();
			PENDING_EJECT.clear();
		});
	}

	// ------------------------------------------------------------------ queries

	/** The raid {@code player} is fighting in (or has won and not left yet). */
	public static @Nullable RaidInstance instanceOf(final Player player) {
		RaidInstance instance = BY_PLAYER.get(player.getUUID());
		if (instance == null || instance.state == RaidInstance.State.CLOSED) {
			return null;
		}
		RaidInstance.Member member = instance.member(player.getUUID());
		return member != null && member.active ? instance : null;
	}

	/** True while {@code boss} belongs to a running instance. */
	public static boolean ownsBoss(final RaidBoss boss) {
		for (RaidInstance instance : INSTANCES.values()) {
			if (boss.getUUID().equals(instance.bossId) && instance.state != RaidInstance.State.CLOSED) {
				return true;
			}
		}
		return false;
	}

	private static boolean insideRunningArena(final Vec3 pos) {
		for (RaidInstance instance : INSTANCES.values()) {
			if (instance.state != RaidInstance.State.CLOSED && Arenas.inside(instance.center, pos, 16.0)) {
				return true;
			}
		}
		return false;
	}

	public static List<RaidInstance> instances() {
		return List.copyOf(INSTANCES.values());
	}

	// ------------------------------------------------------------------ entering

	/**
	 * The raid marshal's "enter" button: the leader (or a solo player) takes the party members within
	 * {@link #GATHER_RANGE} into a new instance. Everyone must meet the boss's level. Answers the
	 * leader with what went wrong.
	 */
	public static boolean tryEnter(final ServerPlayer leader, final int marshalId, final BossDef def) {
		return tryEnter(leader, marshalId, def, RaidDifficulty.NORMAL);
	}

	/**
	 * Enters {@code def} on {@code difficulty}. Every member must have cleared the difficulty below it; members already
	 * rewarded for it this cycle come along for practice (no fee, no rewards), the others pay the difficulty's fee.
	 */
	public static boolean tryEnter(final ServerPlayer leader, final int marshalId, final BossDef def, final RaidDifficulty difficulty) {
		if (!(leader.level().getEntity(marshalId) instanceof CityNpc npc) || npc.role() != CityNpc.Role.RAID_MARSHAL || npc.distanceTo(leader) > MARSHAL_RANGE) {
			return false;
		}
		if (!Parties.canLead(leader)) {
			return fail(leader, "not_leader");
		}
		List<ServerPlayer> going = new ArrayList<>();
		Set<UUID> practice = new HashSet<>();
		List<Component> problems = new ArrayList<>();
		long cycle = ResetCycle.cycle(leader.level());
		String key = PlayerRecords.raidKey(def.id(), difficulty.id());
		int fee = difficulty.fee(def);
		for (ServerPlayer p : Parties.onlineMembers(leader)) {
			JobData data = JobProgression.get(p);
			PlayerRecords records = Progress.get(p);
			RaidDifficulty previous = difficulty.previous();
			boolean locked = records.raidLocked(key, cycle);
			if (instanceOf(p) != null) {
				problems.add(problem(p, "in_raid"));
			} else if (p.level() != leader.level() || p.distanceTo(leader) > GATHER_RANGE) {
				problems.add(problem(p, "too_far"));
			} else if (data.level() < def.minLevel()) {
				problems.add(problem(p, "level", def.minLevel()));
			} else if (!p.isAlive()) {
				problems.add(problem(p, "dead"));
			} else if (previous != null && records.raidClears(PlayerRecords.raidKey(def.id(), previous.id())) == 0) {
				problems.add(problem(p, "difficulty", Component.translatable(previous.nameKey())));
			} else if (!locked && Coins.total(p) < fee) {
				problems.add(problem(p, "fee", Coins.component(fee)));
			} else {
				going.add(p);
				if (locked) {
					practice.add(p.getUUID());
				}
			}
		}
		if (!problems.isEmpty()) {
			leader.sendSystemMessage(msg("not_ready").withStyle(ChatFormatting.RED));
			for (Component c : problems) {
				leader.sendSystemMessage(c);
			}
			return false;
		}
		for (ServerPlayer p : going) {
			if (practice.contains(p.getUUID())) {
				p.sendSystemMessage(msg("practice", ResetCycle.remaining(ResetCycle.ticksToNextCycle(p.level()))).withStyle(ChatFormatting.YELLOW));
			} else {
				Coins.take(p, fee);
				p.sendSystemMessage(msg("fee_paid", Coins.component(fee)).withStyle(ChatFormatting.GRAY));
			}
		}
		return start(leader.level().getServer(), going, def, leader.getUUID(), difficulty, practice) != null;
	}

	private static Component problem(final ServerPlayer p, final String key, final Object... args) {
		return Component.literal(" - ").append(p.getDisplayName()).append(": ").append(msg("problem." + key, args)).withStyle(ChatFormatting.GRAY);
	}

	/** Builds an arena and sends {@code players} in (no level checks; used by the marshal and by {@code /raid start}). */
	public static @Nullable RaidInstance start(final MinecraftServer server, final List<ServerPlayer> players, final BossDef def, final UUID leader) {
		return start(server, players, def, leader, RaidDifficulty.NORMAL, Set.of());
	}

	public static @Nullable RaidInstance start(final MinecraftServer server, final List<ServerPlayer> players, final BossDef def, final UUID leader,
		final RaidDifficulty difficulty, final Set<UUID> practice) {
		ServerLevel level = RaidDimension.level(server);
		if (level == null || players.isEmpty()) {
			return null;
		}
		int slot = freeSlot();
		if (slot < 0) {
			for (ServerPlayer p : players) {
				fail(p, "busy");
			}
			return null;
		}
		BlockPos center = new BlockPos((slot + 1) * SLOT_SPACING, RaidDimension.FLOOR_Y, 0);
		Arenas.build(level, center, def.arena(), RandomSource.create(server.overworld().getGameTime() ^ slot));
		RaidInstance instance = new RaidInstance(nextId++, slot, def, center, leader, difficulty);
		instance.partySize = players.size();
		instance.practice.addAll(practice);
		instance.affixes = difficulty.modified() ? RaidAffix.forCycle(ResetCycle.cycle(server.overworld())) : List.of();
		INSTANCES.put(instance.id, instance);
		for (int i = 0; i < players.size(); i++) {
			ServerPlayer p = players.get(i);
			RaidInstance old = BY_PLAYER.get(p.getUUID());
			if (old != null && old != instance) {
				old.members.remove(p.getUUID());
			}
			instance.members.put(p.getUUID(), new RaidInstance.Member(p.getUUID(), p.getPlainTextName(), p.level().dimension(), p.position(), p.getYRot()));
			BY_PLAYER.put(p.getUUID(), instance);
			Vec3 to = Arenas.playerSpawn(center, i);
			p.teleportTo(level, to.x, to.y, to.z, Set.of(), 180.0F, 0.0F, true);
			p.setHealth(p.getMaxHealth());
			p.clearFire();
			title(p, Component.translatable(def.nameKey()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
				Component.translatable(def.epithetKey()).append(" · ").append(Component.translatable(difficulty.nameKey())).withStyle(ChatFormatting.YELLOW));
			p.sendSystemMessage(msg("entered", Component.translatable(def.nameKey()), COUNTDOWN_TICKS / 20).withStyle(ChatFormatting.GOLD));
			for (RaidAffix affix : instance.affixes) {
				p.sendSystemMessage(Component.literal(" ◆ ").append(Component.translatable(affix.nameKey())).append(": ")
					.append(Component.translatable(affix.descKey())).withStyle(ChatFormatting.LIGHT_PURPLE));
			}
		}
		return instance;
	}

	private static int freeSlot() {
		boolean[] used = new boolean[MAX_SLOTS];
		for (RaidInstance instance : INSTANCES.values()) {
			used[instance.slot] = true;
		}
		for (int i = 0; i < MAX_SLOTS; i++) {
			if (!used[i]) {
				return i;
			}
		}
		return -1;
	}

	// ------------------------------------------------------------------ leaving

	/** {@code /raid leave}: go home now (during a fight this counts as giving up that fight). */
	public static boolean leave(final ServerPlayer player) {
		RaidInstance instance = instanceOf(player);
		if (instance == null) {
			if (RaidDimension.is(player.level())) {
				PENDING_EJECT.put(player.getUUID(), null);
				return true;
			}
			return fail(player, "not_in_raid");
		}
		RaidInstance.Member member = instance.member(player.getUUID());
		if (member != null) {
			member.active = false;
		}
		PENDING_EJECT.put(player.getUUID(), instance);
		if (instance.state == RaidInstance.State.FIGHT) {
			tell(player.level().getServer(), instance, msg("left", player.getDisplayName()).withStyle(ChatFormatting.YELLOW));
		}
		return true;
	}

	/** Death in the raid dimension: heal, mark fallen and send home at the end of the tick. Returns true when handled. */
	private static boolean fall(final ServerPlayer player) {
		if (!RaidDimension.is(player.level())) {
			return false;
		}
		player.setHealth(1.0F);
		RaidInstance instance = BY_PLAYER.get(player.getUUID());
		RaidInstance.Member member = instance == null ? null : instance.member(player.getUUID());
		if (member != null && member.active) {
			member.active = false;
			member.fallen = true;
			tell(player.level().getServer(), instance, msg("fallen", player.getDisplayName()).withStyle(ChatFormatting.RED));
		}
		PENDING_EJECT.put(player.getUUID(), instance);
		return true;
	}

	private static void onJoin(final ServerPlayer player) {
		if (!RaidDimension.is(player.level())) {
			return;
		}
		RaidInstance instance = BY_PLAYER.get(player.getUUID());
		RaidInstance.Member member = instance == null ? null : instance.member(player.getUUID());
		if (instance != null && member != null && !member.fallen && instance.state != RaidInstance.State.CLOSED
			&& Arenas.inside(instance.center, player.position(), 4.0)) {
			member.active = true;
			return;
		}
		// the fight is over (or the server restarted): back to where they came from, or the city
		PENDING_EJECT.put(player.getUUID(), instance);
	}

	private static void onLeave(final UUID id) {
		RaidInstance instance = BY_PLAYER.get(id);
		RaidInstance.Member member = instance == null ? null : instance.member(id);
		if (member != null) {
			member.active = false;
		}
	}

	private static void eject(final MinecraftServer server, final ServerPlayer player, final @Nullable RaidInstance instance) {
		RaidInstance.Member member = instance == null ? null : instance.member(player.getUUID());
		ServerLevel to = member == null ? null : server.getLevel(member.returnLevel);
		Vec3 pos;
		float yaw;
		if (to != null && !RaidDimension.is(to)) {
			pos = member.returnPos;
			yaw = member.returnYaw;
		} else {
			to = server.overworld();
			BlockPos spawn = CityZone.isCityLevel(to) ? CityZone.spawn(CityZone.baseY(to)) : to.getRespawnData().pos();
			pos = Vec3.atBottomCenterOf(spawn);
			yaw = 180.0F;
		}
		player.teleportTo(to, pos.x, pos.y, pos.z, Set.of(), yaw, 0.0F, true);
		player.setHealth(player.getMaxHealth());
		player.clearFire();
		player.resetFallDistance();
		player.setDeltaMovement(Vec3.ZERO);
		if (instance != null && instance.state != RaidInstance.State.CLOSED && member != null && member.fallen) {
			player.sendSystemMessage(msg("fallen_self").withStyle(ChatFormatting.RED));
		}
	}

	// ------------------------------------------------------------------ ticking

	private static void tick(final MinecraftServer server) {
		if (!PENDING_EJECT.isEmpty()) {
			Map<UUID, RaidInstance> due = new LinkedHashMap<>(PENDING_EJECT);
			PENDING_EJECT.clear();
			for (Map.Entry<UUID, RaidInstance> e : due.entrySet()) {
				ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
				if (p != null && RaidDimension.is(p.level())) {
					eject(server, p, e.getValue());
				}
			}
		}
		if (INSTANCES.isEmpty()) {
			return;
		}
		ServerLevel level = RaidDimension.level(server);
		Iterator<RaidInstance> it = INSTANCES.values().iterator();
		List<RaidInstance> closing = new ArrayList<>();
		while (it.hasNext()) {
			RaidInstance instance = it.next();
			if (level == null) {
				closing.add(instance);
				continue;
			}
			instance.timer++;
			switch (instance.state) {
				case COUNTDOWN -> countdown(server, level, instance);
				case FIGHT -> fight(server, level, instance);
				case VICTORY -> victory(server, instance);
				case CLOSED -> closing.add(instance);
			}
		}
		for (RaidInstance instance : closing) {
			close(server, instance);
		}
	}

	private static void countdown(final MinecraftServer server, final ServerLevel level, final RaidInstance instance) {
		int left = COUNTDOWN_TICKS - instance.timer;
		if (left > 0 && left <= 60 && left % 20 == 0) {
			for (ServerPlayer p : activePlayers(server, instance)) {
				title(p, Component.literal(String.valueOf(left / 20)).withStyle(ChatFormatting.RED, ChatFormatting.BOLD), Component.empty());
				ping(p, SoundEvents.NOTE_BLOCK_BASEDRUM, 0.6F);
			}
		}
		if (!instance.anyActive()) {
			instance.state = RaidInstance.State.CLOSED;
			return;
		}
		if (left > 0) {
			return;
		}
		RaidBoss boss = RaidBosses.type(instance.boss).create(level, EntitySpawnReason.EVENT);
		if (boss == null) {
			instance.state = RaidInstance.State.CLOSED;
			return;
		}
		Vec3 at = Arenas.bossSpawn(instance.center, instance.boss.arena());
		boss.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		boss.configure(instance.partySize, instance.center, instance.difficulty, instance.affixes);
		level.addFreshEntity(boss);
		instance.bossId = boss.getUUID();
		instance.state = RaidInstance.State.FIGHT;
		instance.timer = 0;
		level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4.0F, 0.6F);
		for (ServerPlayer p : activePlayers(server, instance)) {
			title(p, Component.translatable(instance.boss.nameKey()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
				Component.translatable("raid.minecraft_mode.fight").withStyle(ChatFormatting.GOLD));
		}
	}

	private static void fight(final MinecraftServer server, final ServerLevel level, final RaidInstance instance) {
		if (instance.timer % 20 != 0) {
			return;
		}
		for (RaidInstance.Member m : instance.members.values()) {
			if (!m.active) {
				continue;
			}
			ServerPlayer p = server.getPlayerList().getPlayer(m.id);
			if (p == null || p.level() != level || !Arenas.inside(instance.center, p.position(), 8.0)) {
				m.active = false;
			}
		}
		Entity boss = instance.bossId == null ? null : level.getEntity(instance.bossId);
		if (!instance.anyActive() || boss == null) {
			tell(server, instance, msg("failed", Component.translatable(instance.boss.nameKey())).withStyle(ChatFormatting.RED));
			instance.state = RaidInstance.State.CLOSED;
		}
	}

	private static void victory(final MinecraftServer server, final RaidInstance instance) {
		int left = VICTORY_TICKS - instance.timer;
		if (left % 20 == 0) {
			for (ServerPlayer p : activePlayers(server, instance)) {
				p.sendOverlayMessage(msg("returning", left / 20).withStyle(ChatFormatting.GOLD));
			}
		}
		if (left <= 0 || !instance.anyActive()) {
			instance.state = RaidInstance.State.CLOSED;
		}
	}

	/** Removes what is left of the fight and sends the remaining players home. */
	private static void close(final MinecraftServer server, final RaidInstance instance) {
		instance.state = RaidInstance.State.CLOSED;
		INSTANCES.remove(instance.id);
		for (RaidInstance.Member m : instance.members.values()) {
			ServerPlayer p = server.getPlayerList().getPlayer(m.id);
			if (p != null && RaidDimension.is(p.level()) && Arenas.inside(instance.center, p.position(), 30.0)) {
				eject(server, p, instance);
			}
			if (BY_PLAYER.get(m.id) == instance) {
				BY_PLAYER.remove(m.id);
			}
		}
		ServerLevel level = RaidDimension.level(server);
		if (level != null) {
			AABB box = new AABB(instance.center).inflate(Arenas.RADIUS + 16, 48, Arenas.RADIUS + 16);
			for (Entity e : level.getEntitiesOfClass(Entity.class, box, e -> !(e instanceof Player))) {
				e.discard();
			}
		}
	}

	// ------------------------------------------------------------------ victory

	/** Called by the boss when it dies. */
	public static void onBossDefeated(final RaidBoss boss) {
		RaidInstance instance = null;
		for (RaidInstance i : INSTANCES.values()) {
			if (boss.getUUID().equals(i.bossId)) {
				instance = i;
			}
		}
		if (instance == null || instance.state != RaidInstance.State.FIGHT || !(boss.level() instanceof ServerLevel level)) {
			return;
		}
		MinecraftServer server = level.getServer();
		int fightTicks = instance.timer;
		instance.state = RaidInstance.State.VICTORY;
		instance.timer = 0;
		BossDef def = instance.boss;
		RaidDifficulty difficulty = instance.difficulty;
		RandomSource random = level.getRandom();
		long cycle = ResetCycle.cycle(level);
		Map<UUID, String> participants = new LinkedHashMap<>();
		List<String> names = new ArrayList<>();
		List<JobData> classes = new ArrayList<>();
		for (RaidInstance.Member m : instance.members.values()) {
			ServerPlayer p = server.getPlayerList().getPlayer(m.id);
			if (m.id.equals(instance.leader)) {
				names.addFirst(m.name);
			} else {
				names.add(m.name);
			}
			if (p == null) {
				continue;
			}
			if (instance.practice.contains(m.id)) {
				p.sendSystemMessage(msg("practice_done").withStyle(ChatFormatting.YELLOW));
				continue;
			}
			participants.put(m.id, m.name);
			JobData data = JobProgression.get(p);
			classes.add(data);
			reward(p, def, difficulty, random);
			Progress.raidCleared(p, def, difficulty, cycle);
			Companions.rollDrop(p, Companions.raidDrop(difficulty), Rarity.EPIC);
		}
		int lots = participants.isEmpty() ? 0 : 3 + participants.size() / 2 + difficulty.extraLots;
		List<ItemStack> items = new ArrayList<>();
		for (int i = 0; i < lots; i++) {
			JobData bias = classes.isEmpty() ? null : classes.get(random.nextInt(classes.size()));
			ItemStack stack = GearDrops.pick(bias, def.lo(), def.hi(), random);
			if (!stack.isEmpty()) {
				if (difficulty.enhanceMax > 0) {
					stack.set(ModDataComponents.ENHANCEMENT, new Enhancement(difficulty.enhanceMin + random.nextInt(difficulty.enhanceMax - difficulty.enhanceMin + 1), 0));
				}
				items.add(stack);
			}
		}
		tell(server, instance, msg("victory", Component.translatable(def.nameKey()), VICTORY_TICKS / 20).withStyle(ChatFormatting.GOLD));
		int place = RaidRecordsData.get(server).submit(def, difficulty, names, fightTicks, ResetCycle.day(level));
		tell(server, instance, msg("time", clock(fightTicks), Component.translatable(difficulty.nameKey())).withStyle(ChatFormatting.AQUA));
		if (place > 0) {
			tell(server, instance, msg("record", place).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
		}
		if (!participants.isEmpty() && !items.isEmpty()) {
			UUID leader = participants.containsKey(instance.leader) ? instance.leader : participants.keySet().iterator().next();
			LootSessions.start(server, def, participants, leader, items);
		}
	}

	/** "m:ss" of a fight time in ticks. */
	public static String clock(final int ticks) {
		int seconds = ticks / 20;
		return seconds / 60 + ":" + String.format(java.util.Locale.ROOT, "%02d", seconds % 60);
	}

	/**
	 * Everyone's share: Evolution Ether (5-8, more on harder difficulties), condensed essence (2-4), consumables, enhancement
	 * stones, sometimes a protection scroll, and job experience.
	 */
	private static void reward(final ServerPlayer player, final BossDef def, final RaidDifficulty difficulty, final RandomSource random) {
		int grade = def.lo() + random.nextInt(def.hi() - def.lo() + 1);
		give(player, EvolutionEtherItem.of(grade, Math.round((5 + random.nextInt(4)) * difficulty.ether)));
		give(player, new ItemStack(ModItems.ENHANCEMENT_STONE, difficulty.stones));
		if (random.nextFloat() < difficulty.scrollChance) {
			give(player, new ItemStack(ModItems.PROTECTION_SCROLL));
		}
		give(player, new ItemStack(ModItems.CONDENSED_ESSENCE, 2 + random.nextInt(3)));
		for (ItemStack supply : Consumables.raidRewards(RaidBosses.index(def), def == RaidBosses.AETHRYX, random)) {
			give(player, supply);
		}
		JobData data = JobProgression.get(player);
		JobProgression.addExp(player, Math.max(50, JobProgression.expToNext(data.level()) / 3));
		ping(player, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE), 1.0F);
	}

	private static void give(final ServerPlayer player, final ItemStack stack) {
		player.getInventory().placeItemBackInInventory(stack, net.minecraft.util.Prediction.SERVER_ONLY);
	}

	// ------------------------------------------------------------------ messages

	private static List<ServerPlayer> activePlayers(final MinecraftServer server, final RaidInstance instance) {
		List<ServerPlayer> out = new ArrayList<>();
		for (RaidInstance.Member m : instance.members.values()) {
			ServerPlayer p = server.getPlayerList().getPlayer(m.id);
			if (p != null && m.active) {
				out.add(p);
			}
		}
		return out;
	}

	private static void tell(final MinecraftServer server, final RaidInstance instance, final Component message) {
		for (RaidInstance.Member m : instance.members.values()) {
			ServerPlayer p = server.getPlayerList().getPlayer(m.id);
			if (p != null) {
				p.sendSystemMessage(message);
			}
		}
	}

	/** A sound only {@code p} hears. */
	public static void ping(final ServerPlayer p, final Holder<SoundEvent> sound, final float pitch) {
		p.connection.send(new ClientboundSoundPacket(sound, SoundSource.MASTER, p.getX(), p.getY(), p.getZ(), 1.0F, pitch, p.getRandom().nextLong()));
	}

	static void title(final ServerPlayer p, final Component title, final Component subtitle) {
		p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 15));
		p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
		p.connection.send(new ClientboundSetTitleTextPacket(title));
	}

	static MutableComponent msg(final String key, final Object... args) {
		return Component.translatable("message.minecraft_mode.raid." + key, args);
	}

	private static boolean fail(final ServerPlayer player, final String key, final Object... args) {
		player.sendSystemMessage(msg(key, args).withStyle(ChatFormatting.RED));
		return false;
	}

	private Raids() {
	}
}
