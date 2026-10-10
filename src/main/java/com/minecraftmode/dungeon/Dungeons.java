package com.minecraftmode.dungeon;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.city.CityZone;
import com.minecraftmode.companion.Companions;
import com.minecraftmode.job.quest.TrialHunts;
import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.entity.CityNpc;
import com.minecraftmode.entity.MobPower;
import com.minecraftmode.entity.combat.Attacks;
import com.minecraftmode.entity.combat.Telegraph;
import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMob;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.loot.EvolutionEtherItem;
import com.minecraftmode.loot.GearDrops;
import com.minecraftmode.loot.GearShop;
import com.minecraftmode.market.AuctionHouse;
import com.minecraftmode.progress.Progress;
import com.minecraftmode.progress.ResetCycle;
import com.minecraftmode.raid.Parties;
import com.minecraftmode.raid.RaidDamage;
import com.minecraftmode.raid.Raids;
import com.minecraftmode.registry.ModAttachments;
import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Dungeons for 2-4 players (solo works, with less health on the monsters). The dungeon warden sends a party into a fresh run of
 * the dungeon dimension: three halls whose doors open as each is cleared, then a champion of a named monster. Without a keystone
 * the run is untimed and the clear gives a +2 keystone; with one, monsters grow by {@link #healthScale}/{@link #damageScale} per
 * level, modifiers join from +4, +7 and +10, and beating the timer upgrades the keystone (+2 under 60% of the time) and moves it to
 * another dungeon; a late or failed run lowers it. Falling in a run never kills: the player wakes at the entrance and the run
 * loses {@link #DEATH_PENALTY_SECONDS} seconds.
 */
public final class Dungeons {
	public static final int COUNTDOWN_TICKS = 100;
	public static final int VICTORY_TICKS = 20 * 30;
	public static final double WARDEN_RANGE = 8.0;
	public static final double GATHER_RANGE = 24.0;
	public static final int MAX_PARTY = 4;
	public static final int DEATH_PENALTY_SECONDS = 15;
	/** A new keystone goes to a dungeon opened at most this many levels below the holder's level. */
	public static final int KEYSTONE_REACH = 20;
	/** The boss: health and damage on top of its top-level stats. */
	public static final float CHAMPION_HEALTH = 2.5F;
	public static final float CHAMPION_DAMAGE = 1.2F;
	/** One monster of every hall is an elite. */
	public static final float ELITE_HEALTH = 2.5F;
	public static final float ELITE_DAMAGE = 1.3F;
	private static final int SLOT_SPACING = 512;
	private static final int MAX_SLOTS = 32;
	/** Marks a mob the Raging affix already enraged. */
	private static final String RAGING_TAG = "minecraft_mode_raging";
	private static final Identifier SCALE_ID = MinecraftMode.id("dungeon_scale");

	private static final Map<String, DungeonDef> DEFS = new LinkedHashMap<>();
	private static final Map<Integer, DungeonInstance> INSTANCES = new LinkedHashMap<>();
	private static final Map<UUID, DungeonInstance> BY_PLAYER = new HashMap<>();
	/** Players to put back at the entrance (fallen) or send home (left, stranded) at the end of this tick. */
	private static final Map<UUID, Boolean> PENDING = new LinkedHashMap<>();
	private static int nextId = 1;

	static {
		def("bandit_hideout", "Bandit Hideout", "산적 은신처", "The bandit captain's den under the hills.", "언덕 아래 숨은 산적 두목의 소굴입니다.", 10, "bandit_captain", 20,
			new DungeonDef.Theme(List.of(Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.GRAVEL, Blocks.COARSE_DIRT, Blocks.SPRUCE_PLANKS), Blocks.STONE_BRICKS,
				Blocks.SPRUCE_PLANKS, Blocks.STRIPPED_SPRUCE_LOG, Blocks.GLOWSTONE, Blocks.IRON_BARS),
			List.of(EntityTypes.PILLAGER, EntityTypes.VINDICATOR), 600);
		def("fungal_depths", "Fungal Depths", "균사 심연", "Caverns overgrown by the myconids' spores.", "버섯족의 포자가 뒤덮은 동굴입니다.", 30, "myconid_shaman", 40,
			new DungeonDef.Theme(List.of(Blocks.MYCELIUM, Blocks.PODZOL, Blocks.MOSS_BLOCK, Blocks.ROOTED_DIRT), Blocks.MUD_BRICKS, Blocks.MUSHROOM_STEM,
				Blocks.MUSHROOM_STEM, Blocks.SHROOMLIGHT, Blocks.IRON_BARS),
			List.of(EntityTypes.ZOMBIE, EntityTypes.SPIDER, EntityTypes.CAVE_SPIDER, EntityTypes.WITCH), 660);
		def("sunken_grotto", "Sunken Grotto", "가라앉은 동굴", "A drowned crew guards its captain's hoard.", "익사한 선원들이 선장의 보물을 지킵니다.", 40, "drowned_corsair", 50,
			new DungeonDef.Theme(List.of(Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.SAND, Blocks.GRAVEL), Blocks.DARK_PRISMARINE, Blocks.PRISMARINE_BRICKS,
				Blocks.STRIPPED_DARK_OAK_LOG, Blocks.SEA_LANTERN, Blocks.IRON_BARS),
			List.of(EntityTypes.DROWNED, EntityTypes.SKELETON, EntityTypes.STRAY), 660);
		def("ember_forge", "Ember Forge", "잿불 용광로", "A forge of the deep nether that never cools.", "결코 식지 않는 네더 깊은 곳의 용광로입니다.", 60, "magma_behemoth", 70,
			new DungeonDef.Theme(List.of(Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE, Blocks.BASALT, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS),
				Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.GILDED_BLACKSTONE, Blocks.BASALT, Blocks.SHROOMLIGHT, Blocks.IRON_BARS),
			List.of(EntityTypes.BLAZE, EntityTypes.MAGMA_CUBE, EntityTypes.WITHER_SKELETON), 720);
		def("void_spire", "Void Spire", "공허의 첨탑", "A tower between the worlds where the chorus wraith sings.", "코러스 망령이 노래하는 세계 사이의 탑입니다.", 80,
			"chorus_wraith", 90,
			new DungeonDef.Theme(List.of(Blocks.END_STONE_BRICKS, Blocks.PURPUR_BLOCK, Blocks.END_STONE, Blocks.OBSIDIAN), Blocks.OBSIDIAN, Blocks.PURPUR_PILLAR,
				Blocks.PURPUR_PILLAR, Blocks.PEARLESCENT_FROGLIGHT, Blocks.IRON_BARS),
			List.of(EntityTypes.PHANTOM, EntityTypes.WITHER_SKELETON, EntityTypes.SKELETON), 720);
	}

	private static void def(final String id, final String en, final String ko, final String descEn, final String descKo, final int minLevel, final String boss,
		final int power, final DungeonDef.Theme theme, final List<EntityType<? extends Mob>> trash, final int timeLimit) {
		DEFS.put(id, new DungeonDef(id, en, ko, descEn, descKo, minLevel, boss, power, theme, trash, timeLimit));
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Dungeons::tick);
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> !(entity instanceof ServerPlayer player) || !fall(player));
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(entity instanceof ServerPlayer player && PENDING.containsKey(player.getUUID())));
		ServerLivingEntityEvents.AFTER_DEATH.register(Dungeons::afterDeath);
		ServerPlayerEvents.JOIN.register(Dungeons::onJoin);
		// monsters saved with a dungeon chunk (server stopped mid-run) must not haunt the next run there
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (DungeonDimension.is(level) && entity instanceof Mob && !insideRunning(entity.position()) && !TrialHunts.insideAnyArena(entity.position())) {
				entity.discard();
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> server.execute(() -> {
			DungeonInstance instance = BY_PLAYER.get(handler.player.getUUID());
			DungeonInstance.Member member = instance == null ? null : instance.member(handler.player.getUUID());
			if (member != null) {
				member.active = false;
			}
		}));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			INSTANCES.clear();
			BY_PLAYER.clear();
			MobPower.clearAll();
			PENDING.clear();
		});
	}

	// ------------------------------------------------------------------ definitions and scaling

	public static Collection<DungeonDef> all() {
		return DEFS.values();
	}

	public static @Nullable DungeonDef def(final String id) {
		return DEFS.get(id);
	}

	/** Monster health multiplier of a keystone level (0 = no keystone). */
	public static float healthScale(final int level) {
		return 1.0F + 0.12F * level;
	}

	public static float damageScale(final int level) {
		return 1.0F + 0.08F * level;
	}

	/** Monster health multiplier for a party of {@code players}: 0.7 solo, 1.0 for two, up to 1.6 for four. */
	public static float partyScale(final int players) {
		return 0.4F + 0.3F * Mth.clamp(players, 1, MAX_PARTY);
	}

	/** Trash health and damage from the dungeon's power level. */
	public static float powerHealth(final int power) {
		return 1.0F + power / 10.0F;
	}

	public static float powerDamage(final int power) {
		return 1.0F + power / 50.0F;
	}

	/** Damage multiplier of a dungeon monster (1 for anything else); read by {@code CombatHooks}. */
	public static float damageFactor(final @Nullable Entity attacker) {
		return MobPower.factor(attacker);
	}

	public static DungeonData data(final Player player) {
		return player.getAttachedOrElse(ModAttachments.DUNGEON, DungeonData.DEFAULT);
	}

	// ------------------------------------------------------------------ queries

	public static @Nullable DungeonInstance instanceOf(final Player player) {
		DungeonInstance instance = BY_PLAYER.get(player.getUUID());
		if (instance == null || instance.state == DungeonInstance.State.CLOSED) {
			return null;
		}
		DungeonInstance.Member member = instance.member(player.getUUID());
		return member != null && member.active ? instance : null;
	}

	private static boolean insideRunning(final Vec3 pos) {
		for (DungeonInstance instance : INSTANCES.values()) {
			if (instance.state != DungeonInstance.State.CLOSED && DungeonLayout.inside(instance.origin, pos, 8.0)) {
				return true;
			}
		}
		return false;
	}

	public static List<DungeonInstance> instances() {
		return List.copyOf(INSTANCES.values());
	}

	/** The highest keystone for {@code dungeon} in the player's inventory (or null). */
	public static @Nullable Keystone keystone(final Player player, final String dungeon) {
		Keystone best = null;
		for (ItemStack stack : player.getInventory()) {
			Keystone k = stack.get(ModDataComponents.KEYSTONE);
			if (k != null && k.dungeon().equals(dungeon) && (best == null || k.level() > best.level())) {
				best = k;
			}
		}
		return best;
	}

	private static boolean hasAnyKeystone(final Player player) {
		for (ItemStack stack : player.getInventory()) {
			if (stack.is(ModItems.DUNGEON_KEYSTONE)) {
				return true;
			}
		}
		return false;
	}

	private static boolean takeKeystone(final ServerPlayer player, final Keystone keystone) {
		for (ItemStack stack : player.getInventory()) {
			if (keystone.equals(stack.get(ModDataComponents.KEYSTONE))) {
				stack.shrink(1);
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ entering

	/**
	 * The warden's "enter" button: the leader (or a solo player) takes the party members within {@link #GATHER_RANGE} into a new run,
	 * with the leader's highest keystone for the dungeon when {@code useKeystone}. Answers the leader with what went wrong.
	 */
	public static boolean tryEnter(final ServerPlayer leader, final int wardenId, final DungeonDef def, final boolean useKeystone) {
		if (!(leader.level().getEntity(wardenId) instanceof CityNpc npc) || npc.role() != CityNpc.Role.DUNGEON_WARDEN || npc.distanceTo(leader) > WARDEN_RANGE) {
			return false;
		}
		if (!Parties.canLead(leader)) {
			return fail(leader, "not_leader");
		}
		List<ServerPlayer> party = Parties.onlineMembers(leader);
		if (party.size() > MAX_PARTY) {
			return fail(leader, "too_many", MAX_PARTY);
		}
		Keystone keystone = useKeystone ? keystone(leader, def.id()) : null;
		if (useKeystone && keystone == null) {
			return fail(leader, "no_keystone", Component.translatable(def.nameKey()));
		}
		List<Component> problems = new ArrayList<>();
		List<ServerPlayer> going = new ArrayList<>();
		for (ServerPlayer p : party) {
			if (instanceOf(p) != null || Raids.instanceOf(p) != null) {
				problems.add(problem(p, "busy"));
			} else if (p.level() != leader.level() || p.distanceTo(leader) > GATHER_RANGE) {
				problems.add(problem(p, "too_far"));
			} else if (JobProgression.get(p).level() < def.minLevel()) {
				problems.add(problem(p, "level", def.minLevel()));
			} else if (!p.isAlive()) {
				problems.add(problem(p, "dead"));
			} else {
				going.add(p);
			}
		}
		if (!problems.isEmpty()) {
			leader.sendSystemMessage(msg("not_ready").withStyle(ChatFormatting.RED));
			problems.forEach(leader::sendSystemMessage);
			return false;
		}
		if (keystone != null && !takeKeystone(leader, keystone)) {
			return false;
		}
		if (start(leader.level().getServer(), going, def, leader.getUUID(), keystone == null ? 0 : keystone.level(), keystone == null ? null : leader.getUUID()) == null) {
			if (keystone != null) {
				give(leader, KeystoneItem.of(keystone)); // the run never started: the keystone comes back
			}
			return false;
		}
		return true;
	}

	/** Builds a run and sends {@code players} in (no checks; used by the warden and by {@code /dungeon start}). */
	public static @Nullable DungeonInstance start(final MinecraftServer server, final List<ServerPlayer> players, final DungeonDef def, final UUID leader, final int level,
		final @Nullable UUID keystoneOwner) {
		ServerLevel dungeon = DungeonDimension.level(server);
		if (dungeon == null || players.isEmpty()) {
			return null;
		}
		int slot = freeSlot();
		if (slot < 0) {
			players.forEach(p -> fail(p, "full"));
			return null;
		}
		BlockPos origin = new BlockPos(0, DungeonDimension.FLOOR_Y, (slot + 1) * SLOT_SPACING);
		DungeonLayout.build(dungeon, origin, def.theme(), RandomSource.create(server.overworld().getGameTime() ^ slot * 31L));
		List<DungeonAffix> affixes = DungeonAffix.forRun(level, ResetCycle.cycle(server.overworld()));
		DungeonInstance instance = new DungeonInstance(nextId++, slot, def, origin, leader, level, affixes, keystoneOwner);
		instance.partySize = players.size();
		instance.bar = new ServerBossEvent(Mth.createInsecureUUID(dungeon.getRandom()), barName(instance), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
		INSTANCES.put(instance.id, instance);
		for (int i = 0; i < players.size(); i++) {
			ServerPlayer p = players.get(i);
			BY_PLAYER.put(p.getUUID(), instance);
			instance.members.put(p.getUUID(), new DungeonInstance.Member(p.getUUID(), p.getPlainTextName(), p.level().dimension(), p.position(), p.getYRot()));
			if (p.getVehicle() != null) {
				p.stopRiding();
			}
			Vec3 to = DungeonLayout.entrance(origin, i);
			p.teleportTo(dungeon, to.x, to.y, to.z, Set.of(), -90.0F, 0.0F, true);
			p.setHealth(p.getMaxHealth());
			p.clearFire();
			instance.bar.addPlayer(p);
			title(p, Component.translatable(def.nameKey()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
				level > 0 ? Component.translatable("dungeon.minecraft_mode.keystone_level", level).withStyle(ChatFormatting.LIGHT_PURPLE)
					: Component.translatable("dungeon.minecraft_mode.normal").withStyle(ChatFormatting.YELLOW));
			p.sendSystemMessage(msg("entered", Component.translatable(def.nameKey()), COUNTDOWN_TICKS / 20).withStyle(ChatFormatting.GOLD));
			if (level > 0) {
				p.sendSystemMessage(msg("timer", Raids.clock(instance.limit())).withStyle(ChatFormatting.AQUA));
			}
			for (DungeonAffix affix : affixes) {
				p.sendSystemMessage(Component.literal(" ◆ ").append(Component.translatable(affix.nameKey())).append(": ")
					.append(Component.translatable(affix.descKey())).withStyle(ChatFormatting.LIGHT_PURPLE));
			}
		}
		return instance;
	}

	private static int freeSlot() {
		boolean[] used = new boolean[MAX_SLOTS];
		for (DungeonInstance instance : INSTANCES.values()) {
			used[instance.slot] = true;
		}
		for (int i = 0; i < MAX_SLOTS; i++) {
			if (!used[i]) {
				return i;
			}
		}
		return -1;
	}

	// ------------------------------------------------------------------ leaving and falling

	/** {@code /dungeon leave}: go home now (during a run this gives the run up for this player). */
	public static boolean leave(final ServerPlayer player) {
		DungeonInstance instance = instanceOf(player);
		if (instance == null) {
			if (DungeonDimension.is(player.level())) {
				PENDING.put(player.getUUID(), false);
				return true;
			}
			return fail(player, "not_in_dungeon");
		}
		DungeonInstance.Member member = instance.member(player.getUUID());
		if (member != null) {
			member.active = false;
		}
		PENDING.put(player.getUUID(), false);
		if (instance.state == DungeonInstance.State.RUN) {
			tell(player.level().getServer(), instance, msg("left", player.getDisplayName()).withStyle(ChatFormatting.YELLOW));
		}
		return true;
	}

	/** Death in the dungeon dimension: wake at the entrance (end of tick), the run loses time. Returns true when handled. */
	private static boolean fall(final ServerPlayer player) {
		if (!DungeonDimension.is(player.level())) {
			return false;
		}
		player.setHealth(1.0F);
		DungeonInstance instance = instanceOf(player);
		if (instance == null || instance.state == DungeonInstance.State.CLOSED) {
			PENDING.put(player.getUUID(), false);
			return true;
		}
		DungeonInstance.Member member = instance.member(player.getUUID());
		if (member != null) {
			member.deaths++;
		}
		if (instance.state == DungeonInstance.State.RUN) {
			instance.elapsed += DEATH_PENALTY_SECONDS * 20;
		}
		tell(player.level().getServer(), instance, msg("fallen", player.getDisplayName(), DEATH_PENALTY_SECONDS).withStyle(ChatFormatting.RED));
		PENDING.put(player.getUUID(), true);
		return true;
	}

	private static void onJoin(final ServerPlayer player) {
		if (!DungeonDimension.is(player.level())) {
			return;
		}
		DungeonInstance instance = BY_PLAYER.get(player.getUUID());
		DungeonInstance.Member member = instance == null ? null : instance.member(player.getUUID());
		if (instance != null && member != null && instance.state != DungeonInstance.State.CLOSED && DungeonLayout.inside(instance.origin, player.position(), 2.0)) {
			member.active = true;
			if (instance.bar != null) {
				instance.bar.addPlayer(player);
			}
			return;
		}
		PENDING.put(player.getUUID(), false);
	}

	private static void sendHome(final MinecraftServer server, final ServerPlayer player) {
		DungeonInstance instance = BY_PLAYER.get(player.getUUID());
		DungeonInstance.Member member = instance == null ? null : instance.member(player.getUUID());
		if (instance != null && instance.bar != null) {
			instance.bar.removePlayer(player);
		}
		ServerLevel to = member == null ? null : server.getLevel(member.returnLevel);
		Vec3 pos;
		float yaw;
		if (to != null && !DungeonDimension.is(to)) {
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
	}

	private static void wakeAtEntrance(final ServerPlayer player) {
		DungeonInstance instance = instanceOf(player);
		if (instance == null) {
			return;
		}
		int index = new ArrayList<>(instance.members.keySet()).indexOf(player.getUUID());
		Vec3 to = DungeonLayout.entrance(instance.origin, Math.max(0, index));
		player.teleportTo(player.level(), to.x, to.y, to.z, Set.of(), -90.0F, 0.0F, true);
		player.setHealth(player.getMaxHealth());
		player.clearFire();
		player.resetFallDistance();
		player.setDeltaMovement(Vec3.ZERO);
		player.removeAllEffects();
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
						wakeAtEntrance(p);
					} else {
						sendHome(server, p);
					}
				}
			}
		}
		if (INSTANCES.isEmpty()) {
			return;
		}
		ServerLevel level = DungeonDimension.level(server);
		List<DungeonInstance> closing = new ArrayList<>();
		for (Iterator<DungeonInstance> it = INSTANCES.values().iterator(); it.hasNext();) {
			DungeonInstance instance = it.next();
			if (level == null) {
				closing.add(instance);
				continue;
			}
			instance.timer++;
			switch (instance.state) {
				case COUNTDOWN -> countdown(server, level, instance);
				case RUN -> run(server, level, instance);
				case VICTORY -> victoryTick(server, instance);
				case CLOSED -> closing.add(instance);
			}
		}
		for (DungeonInstance instance : closing) {
			close(server, instance);
		}
	}

	private static void countdown(final MinecraftServer server, final ServerLevel level, final DungeonInstance instance) {
		int left = COUNTDOWN_TICKS - instance.timer;
		if (left > 0 && left <= 60 && left % 20 == 0) {
			for (ServerPlayer p : activePlayers(server, instance)) {
				title(p, Component.literal(String.valueOf(left / 20)).withStyle(ChatFormatting.RED, ChatFormatting.BOLD), Component.empty());
			}
		}
		if (!instance.anyActive()) {
			instance.state = DungeonInstance.State.CLOSED;
			return;
		}
		if (left > 0) {
			return;
		}
		DungeonLayout.setDoor(level, instance.origin, 0, false, instance.def.theme());
		instance.state = DungeonInstance.State.RUN;
		instance.timer = 0;
		level.playSound(null, instance.origin.getX() + 14, instance.origin.getY(), instance.origin.getZ(), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.5F, 0.7F);
		for (ServerPlayer p : activePlayers(server, instance)) {
			title(p, Component.translatable("dungeon.minecraft_mode.go").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), Component.empty());
		}
	}

	private static void run(final MinecraftServer server, final ServerLevel level, final DungeonInstance instance) {
		instance.elapsed++;
		if (instance.timer % 20 == 0) {
			for (DungeonInstance.Member m : instance.members.values()) {
				ServerPlayer p = server.getPlayerList().getPlayer(m.id);
				if (m.active && (p == null || p.level() != level || !DungeonLayout.inside(instance.origin, p.position(), 4.0))) {
					m.active = false;
				}
			}
			if (!instance.anyActive()) {
				tell(server, instance, msg("failed", Component.translatable(instance.def.nameKey())).withStyle(ChatFormatting.RED));
				instance.state = DungeonInstance.State.CLOSED;
				return;
			}
			updateBar(instance);
		}
		List<ServerPlayer> players = activePlayers(server, instance);
		if (!instance.roomAwake) {
			for (ServerPlayer p : players) {
				if (DungeonLayout.inRoom(instance.origin, instance.room, p.position())) {
					wake(level, instance, players);
					break;
				}
			}
			return;
		}
		if (instance.timer % 10 == 0 && instance.room < DungeonLayout.BOSS_ROOM) {
			instance.roomMobs.removeIf(id -> !(level.getEntity(id) instanceof LivingEntity e) || !e.isAlive());
			if (instance.roomMobs.isEmpty()) {
				DungeonLayout.setDoor(level, instance.origin, instance.room, false, instance.def.theme());
				tell(server, instance, msg("hall_cleared", instance.room, DungeonLayout.HALLS).withStyle(ChatFormatting.GREEN));
				level.playSound(null, instance.origin.getX() + DungeonLayout.doorX(instance.room), instance.origin.getY(), instance.origin.getZ(), SoundEvents.IRON_DOOR_OPEN,
					SoundSource.BLOCKS, 1.5F, 0.7F);
				instance.room++;
				instance.roomAwake = false;
				updateBar(instance);
			}
		}
		if (instance.has(DungeonAffix.RAGING) && instance.timer % 20 == 5) {
			for (UUID id : instance.roomMobs) {
				if (level.getEntity(id) instanceof Mob mob && mob.getHealth() < mob.getMaxHealth() * 0.3F && !mob.entityTags().contains(RAGING_TAG)) {
					mob.addTag(RAGING_TAG); // once per mob, not again every time Strength runs out
					mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 60, 1));
					mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 60, 0));
					MobPower.scale(id, 1.5F);
					level.sendParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
				}
			}
		}
		if (instance.has(DungeonAffix.VOLCANIC) && instance.timer % (DungeonAffix.VOLCANIC_INTERVAL * 20) == 0 && !players.isEmpty()) {
			ServerPlayer target = players.get(level.getRandom().nextInt(players.size()));
			Vec3 at = target.position();
			Telegraph.circle(level, at, DungeonAffix.VOLCANIC_RADIUS, 30, Telegraph.RED, () -> {
				if (instance.state != DungeonInstance.State.RUN) {
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

	/** A player stepped into the next room: its monsters (or the boss) appear. */
	private static void wake(final ServerLevel level, final DungeonInstance instance, final List<ServerPlayer> players) {
		instance.roomAwake = true;
		RandomSource random = level.getRandom();
		DungeonDef def = instance.def;
		float health = healthScale(instance.level) * partyScale(instance.partySize);
		float damage = damageScale(instance.level);
		if (instance.room == DungeonLayout.BOSS_ROOM) {
			NamedDef named = NamedMobs.byId(def.boss());
			NamedMob boss = named == null ? null : NamedMobs.type(named).create(level, EntitySpawnReason.EVENT);
			if (boss == null) {
				instance.state = DungeonInstance.State.CLOSED;
				return;
			}
			Vec3 at = DungeonLayout.bossSpawn(instance.origin);
			boss.snapTo(at.x, at.y, at.z, 90.0F, 0.0F);
			boolean tyrant = instance.has(DungeonAffix.TYRANNICAL);
			boss.makeChampion(CHAMPION_HEALTH * health * (tyrant ? DungeonAffix.TYRANNICAL_HEALTH : 1.0F), 1.3F, "entity.minecraft_mode.dungeon_boss",
				instance.origin.offset(DungeonLayout.ROOMS.get(DungeonLayout.BOSS_ROOM).centerX(), 0, 0), 14.0);
			instance.bossId = boss.getUUID();
			MobPower.set(boss.getUUID(), CHAMPION_DAMAGE * damage * (tyrant ? DungeonAffix.TYRANNICAL_DAMAGE : 1.0F));
			level.addFreshEntity(boss);
			level.playSound(null, at.x, at.y, at.z, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3.0F, 0.6F);
			for (ServerPlayer p : players) {
				title(p, boss.getDisplayName().copy().withStyle(ChatFormatting.RED, ChatFormatting.BOLD), Component.translatable("dungeon.minecraft_mode.boss").withStyle(ChatFormatting.GOLD));
			}
			return;
		}
		int count = 4 + instance.room + (instance.partySize - 1) * 2;
		boolean fortified = instance.has(DungeonAffix.FORTIFIED);
		for (int i = 0; i < count; i++) {
			EntityType<? extends Mob> type = def.trash().get(random.nextInt(def.trash().size()));
			Mob mob = type.create(level, EntitySpawnReason.EVENT);
			if (mob == null) {
				continue;
			}
			Vec3 at = DungeonLayout.spawnPoint(instance.origin, instance.room, random);
			mob.snapTo(at.x, at.y, at.z, 90.0F, 0.0F);
			mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), EntitySpawnReason.EVENT, null);
			boolean elite = i == 0;
			float h = powerHealth(def.power()) * health * (fortified ? DungeonAffix.FORTIFIED_HEALTH : 1.0F) * (elite ? ELITE_HEALTH : 1.0F);
			float d = powerDamage(def.power()) * damage * (fortified ? DungeonAffix.FORTIFIED_DAMAGE : 1.0F) * (elite ? ELITE_DAMAGE : 1.0F);
			scaleHealth(mob, h);
			mob.setPersistenceRequired();
			if (elite) {
				mob.setCustomName(Component.translatable("entity.minecraft_mode.dungeon_elite", type.getDescription()).withStyle(ChatFormatting.GOLD));
				mob.setCustomNameVisible(true);
				mob.setGlowingTag(true);
			}
			instance.roomMobs.add(mob.getUUID());
			MobPower.set(mob.getUUID(), d);
			level.addFreshEntity(mob);
			ServerPlayer target = nearest(players, at);
			if (target != null) {
				mob.setTarget(target);
			}
		}
		level.playSound(null, instance.origin.getX() + DungeonLayout.ROOMS.get(instance.room).centerX(), instance.origin.getY(), instance.origin.getZ(),
			SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 2.0F, 1.2F);
	}

	private static void scaleHealth(final LivingEntity mob, final float multiplier) {
		AttributeInstance attr = mob.getAttribute(Attributes.MAX_HEALTH);
		if (attr != null) {
			attr.addOrReplacePermanentModifier(new AttributeModifier(SCALE_ID, multiplier - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			mob.setHealth(mob.getMaxHealth());
		}
	}

	private static @Nullable ServerPlayer nearest(final List<ServerPlayer> players, final Vec3 at) {
		ServerPlayer best = null;
		for (ServerPlayer p : players) {
			if (best == null || p.distanceToSqr(at) < best.distanceToSqr(at)) {
				best = p;
			}
		}
		return best;
	}

	private static void afterDeath(final LivingEntity entity, final net.minecraft.world.damagesource.DamageSource source) {
		if (!DungeonDimension.is(entity.level()) || !MobPower.has(entity.getUUID()) || !(entity.level() instanceof ServerLevel level)) {
			return;
		}
		MobPower.clear(entity.getUUID());
		for (DungeonInstance instance : INSTANCES.values()) {
			if (entity.getUUID().equals(instance.bossId) && instance.state == DungeonInstance.State.RUN) {
				victory(level.getServer(), level, instance);
				return;
			}
			if (instance.roomMobs.contains(entity.getUUID()) && instance.has(DungeonAffix.BOLSTERING)) {
				for (UUID id : instance.roomMobs) {
					if (!id.equals(entity.getUUID()) && level.getEntity(id) instanceof Mob mob && mob.isAlive()
						&& mob.distanceToSqr(entity) <= DungeonAffix.BOLSTER_RANGE * DungeonAffix.BOLSTER_RANGE) {
						AttributeInstance attr = mob.getAttribute(Attributes.MAX_HEALTH);
						AttributeModifier current = attr == null ? null : attr.getModifier(SCALE_ID);
						if (current != null) {
							float ratio = mob.getHealth() / mob.getMaxHealth();
							attr.addOrReplacePermanentModifier(new AttributeModifier(SCALE_ID, (current.amount() + 1.0) * (1.0 + DungeonAffix.BOLSTER) - 1.0,
								AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
							mob.setHealth(mob.getMaxHealth() * ratio);
						}
						MobPower.scale(id, (1.0F + DungeonAffix.BOLSTER));
						level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 5, 0.3, 0.3, 0.3, 0.0);
					}
				}
			}
		}
	}

	private static void victoryTick(final MinecraftServer server, final DungeonInstance instance) {
		int left = VICTORY_TICKS - instance.timer;
		if (left % 20 == 0) {
			for (ServerPlayer p : activePlayers(server, instance)) {
				p.sendOverlayMessage(msg("returning", left / 20).withStyle(ChatFormatting.GOLD));
			}
		}
		if (left <= 0 || !instance.anyActive()) {
			instance.state = DungeonInstance.State.CLOSED;
		}
	}

	private static void updateBar(final DungeonInstance instance) {
		if (instance.bar == null) {
			return;
		}
		instance.bar.setName(barName(instance));
		if (instance.level > 0) {
			boolean late = instance.elapsed > instance.limit();
			instance.bar.setColor(late ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.GREEN);
			instance.bar.setProgress(Mth.clamp(1.0F - (float)instance.elapsed / instance.limit(), 0.0F, 1.0F));
		} else {
			instance.bar.setColor(BossEvent.BossBarColor.BLUE);
			instance.bar.setProgress(Mth.clamp((instance.room - 1) / (float)DungeonLayout.BOSS_ROOM, 0.0F, 1.0F));
		}
	}

	private static Component barName(final DungeonInstance instance) {
		MutableComponent name = Component.translatable(instance.def.nameKey()).withStyle(ChatFormatting.GOLD);
		if (instance.level > 0) {
			name.append(Component.literal(" +" + instance.level).withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		String room = instance.room >= DungeonLayout.BOSS_ROOM ? "★" : instance.room + "/" + DungeonLayout.HALLS;
		name.append(Component.literal("  " + room).withStyle(ChatFormatting.WHITE));
		String clock = Raids.clock(instance.elapsed) + (instance.level > 0 ? " / " + Raids.clock(instance.limit()) : "");
		name.append(Component.literal("  " + clock).withStyle(instance.level > 0 && instance.elapsed > instance.limit() ? ChatFormatting.RED : ChatFormatting.AQUA));
		return name;
	}

	// ------------------------------------------------------------------ victory and closing

	private static void victory(final MinecraftServer server, final ServerLevel level, final DungeonInstance instance) {
		instance.state = DungeonInstance.State.VICTORY;
		instance.finished = true;
		instance.timer = 0;
		updateBar(instance);
		DungeonDef def = instance.def;
		boolean timed = instance.level > 0 && instance.elapsed <= instance.limit();
		RandomSource random = level.getRandom();
		tell(server, instance, msg("victory", Component.translatable(def.nameKey()), Raids.clock(instance.elapsed)).withStyle(ChatFormatting.GOLD));
		if (instance.level > 0) {
			tell(server, instance, msg(timed ? "timed" : "late", Raids.clock(instance.limit())).withStyle(timed ? ChatFormatting.GREEN : ChatFormatting.RED));
		}
		for (ServerPlayer p : activePlayers(server, instance)) {
			reward(p, instance, random);
			p.setAttached(ModAttachments.DUNGEON, data(p).withClear(def.id(), timed ? instance.level : 0));
			Progress.dungeonCleared(p);
			if (instance.level == 0 && !hasAnyKeystone(p)) {
				giveKeystone(p, new Keystone(randomDungeon(p, random, null).id(), Keystone.MIN_LEVEL), "keystone_new");
			}
			Raids.ping(p, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE), 1.0F);
		}
		if (instance.keystoneOwner != null) {
			ServerPlayer owner = server.getPlayerList().getPlayer(instance.keystoneOwner);
			int next = timed ? instance.level + (instance.elapsed <= instance.limit() * 0.6 ? 2 : 1) : Math.max(Keystone.MIN_LEVEL, instance.level - 1);
			if (owner != null) {
				DungeonDef to = timed ? randomDungeon(owner, random, def) : def;
				giveKeystone(owner, new Keystone(to.id(), next), timed ? "keystone_up" : "keystone_down");
			} else {
				mailKeystone(server, instance.keystoneOwner, new Keystone(def.id(), next));
			}
		}
	}

	/**
	 * Everyone's share: coins, Evolution Ether, enhancement stones, condensed essence and job experience (all growing with the
	 * keystone), a gear piece by chance (enhanced from +7), awakening crystals from +5, and a rare (epic from +10) pet or mount by chance.
	 */
	private static void reward(final ServerPlayer player, final DungeonInstance instance, final RandomSource random) {
		DungeonDef def = instance.def;
		int l = instance.level;
		NamedDef boss = NamedMobs.byId(def.boss());
		int lo = boss == null ? 10 : boss.lo();
		int hi = boss == null ? 20 : boss.hi();
		Coins.give(player, Math.max(1, Math.round(GearShop.bracketPrice(Math.max(10, def.power())) * (0.6F + 0.15F * l))));
		give(player, EvolutionEtherItem.of(hi, 2 + l / 2));
		give(player, new ItemStack(ModItems.ENHANCEMENT_STONE, 1 + l / 3));
		give(player, new ItemStack(ModItems.CONDENSED_ESSENCE, 1 + l / 4));
		if (random.nextFloat() < 0.4F + 0.04F * l) {
			JobData job = JobProgression.get(player);
			ItemStack gear = GearDrops.pick(job, lo, hi, random);
			if (!gear.isEmpty()) {
				if (l >= 7) {
					gear.set(ModDataComponents.ENHANCEMENT, new Enhancement(Math.min(10, 1 + l / 3 + random.nextInt(2)), 0));
				}
				give(player, gear);
			}
		}
		if (l >= 5 && random.nextFloat() < 0.15F + 0.03F * l) {
			give(player, new ItemStack(ModItems.AWAKENING_CRYSTAL, 1 + (l >= 12 ? 1 : 0)));
		}
		JobProgression.addExp(player, Math.max(30, JobProgression.expToNext(JobProgression.get(player).level()) / 5));
		if (l >= 10) {
			Companions.rollDrop(player, 0.02F + 0.005F * (l - 10), Rarity.EPIC);
		} else {
			Companions.rollDrop(player, 0.02F, Rarity.RARE);
		}
	}

	/** The keystone's owner is offline: it waits in their market mailbox (saved with the world) instead of being lost. */
	private static void mailKeystone(final MinecraftServer server, final UUID owner, final Keystone keystone) {
		AuctionHouse.get(server).sendItem(owner, KeystoneItem.of(keystone));
	}

	private static void giveKeystone(final ServerPlayer player, final Keystone keystone, final String message) {
		ItemStack stack = KeystoneItem.of(keystone);
		player.sendSystemMessage(msg(message, stack.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE));
		give(player, stack);
	}

	/**
	 * A dungeon {@code player} may enter that still fits their level (opened at most {@link #KEYSTONE_REACH} levels below it, so
	 * keystone rewards keep pace), other than {@code not} when there is another; the highest one they may enter when none fits.
	 */
	private static DungeonDef randomDungeon(final Player player, final RandomSource random, final @Nullable DungeonDef not) {
		int level = JobProgression.get(player).level();
		List<DungeonDef> pool = new ArrayList<>();
		DungeonDef highest = null;
		for (DungeonDef def : DEFS.values()) {
			if (def.minLevel() <= level) {
				if (highest == null || def.minLevel() > highest.minLevel()) {
					highest = def;
				}
				if (def.minLevel() >= level - KEYSTONE_REACH) {
					pool.add(def);
				}
			}
		}
		if (pool.size() > 1) {
			pool.remove(not);
		}
		if (pool.isEmpty()) {
			return highest != null ? highest : DEFS.values().iterator().next();
		}
		return pool.get(random.nextInt(pool.size()));
	}

	/** Removes what is left of the run, sends the remaining players home and lowers the keystone of a run that never finished. */
	private static void close(final MinecraftServer server, final DungeonInstance instance) {
		boolean unfinished = !instance.finished;
		instance.state = DungeonInstance.State.CLOSED;
		INSTANCES.remove(instance.id);
		if (unfinished && instance.keystoneOwner != null) {
			ServerPlayer owner = server.getPlayerList().getPlayer(instance.keystoneOwner);
			Keystone lowered = new Keystone(instance.def.id(), Math.max(Keystone.MIN_LEVEL, instance.level - 1));
			if (owner != null) {
				giveKeystone(owner, lowered, "keystone_down");
			} else {
				mailKeystone(server, instance.keystoneOwner, lowered);
			}
		}
		if (instance.bar != null) {
			instance.bar.removeAllPlayers();
		}
		for (DungeonInstance.Member m : instance.members.values()) {
			ServerPlayer p = server.getPlayerList().getPlayer(m.id);
			if (p != null && DungeonDimension.is(p.level()) && DungeonLayout.inside(instance.origin, p.position(), 16.0)) {
				sendHome(server, p);
			}
			if (BY_PLAYER.get(m.id) == instance) {
				BY_PLAYER.remove(m.id);
			}
		}
		ServerLevel level = DungeonDimension.level(server);
		if (level != null) {
			AABB box = DungeonLayout.bounds(instance.origin).inflate(16);
			for (Entity e : level.getEntitiesOfClass(Entity.class, box, e -> !(e instanceof Player))) {
				MobPower.clear(e.getUUID());
				e.discard();
			}
		}
		for (UUID id : instance.roomMobs) {
			MobPower.clear(id);
		}
		if (instance.bossId != null) {
			MobPower.clear(instance.bossId);
		}
	}

	// ------------------------------------------------------------------ messages

	private static List<ServerPlayer> activePlayers(final MinecraftServer server, final DungeonInstance instance) {
		List<ServerPlayer> out = new ArrayList<>();
		for (DungeonInstance.Member m : instance.members.values()) {
			ServerPlayer p = server.getPlayerList().getPlayer(m.id);
			if (p != null && m.active) {
				out.add(p);
			}
		}
		return out;
	}

	private static void tell(final MinecraftServer server, final DungeonInstance instance, final Component message) {
		for (DungeonInstance.Member m : instance.members.values()) {
			ServerPlayer p = server.getPlayerList().getPlayer(m.id);
			if (p != null) {
				p.sendSystemMessage(message);
			}
		}
	}

	private static void give(final ServerPlayer player, final ItemStack stack) {
		player.getInventory().placeItemBackInInventory(stack, net.minecraft.util.Prediction.SERVER_ONLY);
	}

	private static void title(final ServerPlayer p, final Component title, final Component subtitle) {
		p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 15));
		p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
		p.connection.send(new ClientboundSetTitleTextPacket(title));
	}

	static MutableComponent msg(final String key, final Object... args) {
		return Component.translatable("message.minecraft_mode.dungeon." + key, args);
	}

	private static Component problem(final ServerPlayer p, final String key, final Object... args) {
		return Component.literal(" - ").append(p.getDisplayName()).append(": ").append(msg("problem." + key, args)).withStyle(ChatFormatting.GRAY);
	}

	private static boolean fail(final ServerPlayer player, final String key, final Object... args) {
		player.sendSystemMessage(msg(key, args).withStyle(ChatFormatting.RED));
		return false;
	}

	/** Every dungeon's id (also for commands). */
	public static List<String> ids() {
		return List.copyOf(DEFS.keySet());
	}

	private Dungeons() {
	}
}
