package com.minecraftmode.job.quest;

import com.minecraftmode.dungeon.DungeonDimension;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.quest.QuestDef.KillGoal;
import com.minecraftmode.job.quest.QuestDef.TokenSource;
import com.minecraftmode.raid.RaidDimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Huntmaster Garrick's trial hunting grounds: a player with a trial steps into a private arena in the dungeon dimension where only the
 * monsters their trial still needs appear (the kill goals that are not done and the token sources while tokens are missing), one at a
 * time for bosses. Spawning stops when nothing is needed any more, so the arena cannot be farmed. The arena is walled with
 * reinforced deepslate (nothing breaks it, the Wither included) and has a pool for guardians. The lodestone by the entrance, a
 * finished trial, death (handled by {@code Dungeons}) or logging out ends the hunt; the player goes back where they came from. The
 * Ender Dragon is never brought here: its trial goal is fought in the End (the keep courtyard has a portal).
 */
public final class TrialHunts {
	public static final String TAG = "minecraft_mode.trial_hunt";
	/** Arenas sit far from the dungeon runs (which are built along x = 0). */
	private static final int ORIGIN_X = -4096;
	private static final int SPACING = 128;
	private static final int MAX_SLOTS = 32;
	/** Inside size (blocks) and height. */
	public static final int SIZE = 33;
	public static final int HEIGHT = 20;
	/** Monsters alive at once (bosses count for {@link #BOSS_WEIGHT}). */
	public static final int CAP = 5;
	private static final int BOSS_WEIGHT = 3;
	private static final int SPAWN_INTERVAL = 40;
	/** Ticks a hunt lasts after the trial has nothing left to hunt. */
	private static final int DONE_TICKS = 200;
	/** Monsters a ranged-kill goal sends in (the full hostile list would bring nether and ocean monsters). */
	private static final Set<EntityType<?>> COMMON = Set.of(EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.SPIDER, EntityTypes.HUSK, EntityTypes.STRAY);
	/** Bosses that only come when nothing else is up. The dragon belongs to the End. */
	private static final Set<EntityType<?>> NEVER = Set.of(EntityTypes.ENDER_DRAGON);
	private static final Set<EntityType<?>> FLYING = Set.of(EntityTypes.GHAST, EntityTypes.PHANTOM, EntityTypes.BLAZE, EntityTypes.WITHER);
	private static final Set<EntityType<?>> SWIMMING = Set.of(EntityTypes.GUARDIAN, EntityTypes.ELDER_GUARDIAN);

	private static final Map<UUID, Hunt> HUNTS = new HashMap<>();

	/** One running hunt. */
	private static final class Hunt {
		final UUID player;
		final int slot;
		final BlockPos origin;
		final String quest;
		final ResourceKey<Level> returnLevel;
		final Vec3 returnPos;
		final float returnYaw;
		final Set<UUID> mobs = new HashSet<>();
		int age;
		int doneFor;

		Hunt(final ServerPlayer player, final int slot, final BlockPos origin, final String quest) {
			this.player = player.getUUID();
			this.slot = slot;
			this.origin = origin;
			this.quest = quest;
			this.returnLevel = player.level().dimension();
			this.returnPos = player.position();
			this.returnYaw = player.getYRot();
		}
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(TrialHunts::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> server.execute(() -> {
			Hunt hunt = HUNTS.remove(handler.player.getUUID());
			if (hunt != null) {
				clear(server, hunt);
			}
		}));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> HUNTS.clear());
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (player instanceof ServerPlayer sp && DungeonDimension.is(level) && level.getBlockState(hit.getBlockPos()).is(Blocks.LODESTONE)) {
				Hunt hunt = HUNTS.get(sp.getUUID());
				if (hunt != null && inside(hunt.origin, Vec3.atCenterOf(hit.getBlockPos()), 1.0)) {
					leave(sp, "message.minecraft_mode.hunt.left");
					return InteractionResult.SUCCESS;
				}
			}
			return InteractionResult.PASS;
		});
	}

	// ------------------------------------------------------------------ what a trial still needs

	/** Monster groups the player's trial still needs, one entry per unfinished goal or missing token source. Empty when none. */
	public static List<Set<EntityType<?>>> needs(final ServerPlayer player) {
		QuestDef quest = QuestService.active(player);
		List<Set<EntityType<?>>> needs = new ArrayList<>();
		if (quest == null) {
			return needs;
		}
		QuestData data = QuestService.get(player);
		for (int i = 0; i < quest.kills().size(); i++) {
			KillGoal goal = quest.kills().get(i);
			if (data.progress(i) < goal.count()) {
				add(needs, goal.ranged() ? COMMON : goal.types());
			}
		}
		if (JobProgression.count(player.getInventory(), quest.token()) < quest.tokenCount()) {
			for (TokenSource source : quest.sources()) {
				add(needs, source.types().size() > 6 ? COMMON : source.types());
			}
		}
		return needs;
	}

	private static void add(final List<Set<EntityType<?>>> needs, final Set<EntityType<?>> types) {
		Set<EntityType<?>> allowed = new HashSet<>(types);
		allowed.removeAll(NEVER);
		if (!allowed.isEmpty() && !needs.contains(allowed)) {
			needs.add(allowed);
		}
	}

	public static boolean hunting(final ServerPlayer player) {
		return HUNTS.containsKey(player.getUUID());
	}

	public static @Nullable BlockPos arena(final ServerPlayer player) {
		Hunt hunt = HUNTS.get(player.getUUID());
		return hunt == null ? null : hunt.origin;
	}

	/** Inside an arena (with {@code margin} blocks to spare): its monsters are not cleaned up as strays of the dungeon dimension. */
	public static boolean insideAnyArena(final Vec3 pos) {
		for (Hunt hunt : HUNTS.values()) {
			if (inside(hunt.origin, pos, 4.0)) {
				return true;
			}
		}
		return false;
	}

	static boolean inside(final BlockPos origin, final Vec3 pos, final double margin) {
		return pos.x >= origin.getX() - margin && pos.x <= origin.getX() + SIZE + 2 + margin && pos.z >= origin.getZ() - margin
			&& pos.z <= origin.getZ() + SIZE + 2 + margin && pos.y >= origin.getY() - 6 - margin && pos.y <= origin.getY() + HEIGHT + 2 + margin;
	}

	// ------------------------------------------------------------------ entering and leaving

	/** Huntmaster Garrick: takes a player with something to hunt into a fresh arena. Returns whether they went in. */
	public static boolean enter(final ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		ServerLevel level = DungeonDimension.level(server);
		QuestDef quest = QuestService.active(player);
		if (quest == null) {
			return tell(player, "message.minecraft_mode.hunt.no_trial");
		}
		if (hunting(player) || DungeonDimension.is(player.level()) || RaidDimension.is(player.level()) || level == null) {
			return tell(player, "message.minecraft_mode.hunt.busy");
		}
		List<Set<EntityType<?>>> needs = needs(player);
		if (needs.isEmpty()) {
			boolean dragon = quest.kills().stream().anyMatch(g -> g.types().contains(EntityTypes.ENDER_DRAGON));
			return tell(player, dragon ? "message.minecraft_mode.hunt.dragon" : "message.minecraft_mode.hunt.nothing");
		}
		int slot = freeSlot();
		if (slot < 0) {
			return tell(player, "message.minecraft_mode.hunt.full");
		}
		BlockPos origin = new BlockPos(ORIGIN_X, DungeonDimension.FLOOR_Y, slot * SPACING);
		build(level, origin);
		Hunt hunt = new Hunt(player, slot, origin, quest.id());
		HUNTS.put(player.getUUID(), hunt);
		if (player.getVehicle() != null) {
			player.stopRiding();
		}
		Vec3 entrance = entrance(origin);
		player.teleportTo(level, entrance.x, entrance.y, entrance.z, Set.of(), 0.0F, 0.0F, true);
		player.resetFallDistance();
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.hunt.entered", Component.translatable(quest.nameKey()).withColor(quest.job().color()))
			.withStyle(ChatFormatting.GOLD));
		level.playSound(null, entrance.x, entrance.y, entrance.z, SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 0.6F, 1.4F);
		return true;
	}

	/** Where players arrive: by the north wall, facing the arena (south). */
	public static Vec3 entrance(final BlockPos origin) {
		return new Vec3(origin.getX() + 1 + SIZE / 2 + 0.5, origin.getY(), origin.getZ() + 3.5);
	}

	/** The way out: the lodestone at the entrance. */
	public static BlockPos exit(final BlockPos origin) {
		return new BlockPos(origin.getX() + 1 + SIZE / 2, origin.getY(), origin.getZ() + 1);
	}

	public static void leave(final ServerPlayer player, final String messageKey) {
		Hunt hunt = HUNTS.remove(player.getUUID());
		if (hunt == null) {
			return;
		}
		MinecraftServer server = player.level().getServer();
		clear(server, hunt);
		ServerLevel to = server.getLevel(hunt.returnLevel);
		if (to == null || DungeonDimension.is(to)) {
			to = server.overworld();
		}
		if (DungeonDimension.is(player.level())) {
			player.teleportTo(to, hunt.returnPos.x, hunt.returnPos.y, hunt.returnPos.z, Set.of(), hunt.returnYaw, 0.0F, true);
			player.resetFallDistance();
			player.clearFire();
		}
		player.sendSystemMessage(Component.translatable(messageKey).withStyle(ChatFormatting.YELLOW));
	}

	private static int freeSlot() {
		boolean[] used = new boolean[MAX_SLOTS];
		for (Hunt hunt : HUNTS.values()) {
			used[hunt.slot] = true;
		}
		for (int i = 0; i < MAX_SLOTS; i++) {
			if (!used[i]) {
				return i;
			}
		}
		return -1;
	}

	private static boolean tell(final ServerPlayer player, final String key) {
		player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.YELLOW));
		return false;
	}

	// ------------------------------------------------------------------ the arena

	/**
	 * A reinforced deepslate box ({@link #SIZE} inside, {@link #HEIGHT} high) lit by light blocks, with a 7x7 pool four deep in the
	 * middle and the exit lodestone at the entrance. Rebuilt for every hunt.
	 */
	static void build(final ServerLevel level, final BlockPos origin) {
		BlockState wall = Blocks.REINFORCED_DEEPSLATE.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockState water = Blocks.WATER.defaultBlockState();
		BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int x0 = origin.getX();
		int z0 = origin.getZ();
		int y0 = origin.getY();
		int max = SIZE + 1;
		int mid = 1 + SIZE / 2;
		for (int dx = 0; dx <= max; dx++) {
			for (int dz = 0; dz <= max; dz++) {
				boolean edge = dx == 0 || dz == 0 || dx == max || dz == max;
				boolean pool = Math.abs(dx - mid) <= 3 && Math.abs(dz - mid) <= 3;
				for (int dy = -6; dy <= HEIGHT + 1; dy++) {
					BlockState state;
					if (edge || dy == -6 || dy == HEIGHT + 1 || dy < -1 && !pool || dy == -1 && !pool) {
						state = wall;
					} else if (pool && dy < 0 && dy >= -5) {
						state = water;
					} else if (dy == HEIGHT - 2 && dx % 6 == 3 && dz % 6 == 3) {
						state = light;
					} else if (dy == 2 && dx % 6 == 3 && dz % 6 == 3 && !pool) {
						state = light;
					} else {
						state = air;
					}
					level.setBlock(pos.set(x0 + dx, y0 + dy, z0 + dz), state, 2);
				}
			}
		}
		// light along the walls at eye height (light blocks: the walls stay reinforced deepslate, so a Wither cannot open them)
		for (int d = 2; d < max; d += 4) {
			for (BlockPos p : List.of(new BlockPos(x0 + 1, y0 + 2, z0 + d), new BlockPos(x0 + max - 1, y0 + 2, z0 + d), new BlockPos(x0 + d, y0 + 2, z0 + 1),
				new BlockPos(x0 + d, y0 + 2, z0 + max - 1))) {
				level.setBlock(p, light, 2);
			}
		}
		level.setBlock(exit(origin), Blocks.LODESTONE.defaultBlockState(), 2);
		level.setBlock(exit(origin).below(), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState(), 2);
		// whatever was left from an earlier hunt here (drops, orbs, monsters) goes
		AABB box = new AABB(x0 - 2, y0 - 8, z0 - 2, x0 + max + 3, y0 + HEIGHT + 4, z0 + max + 3);
		for (Entity e : level.getEntitiesOfClass(Entity.class, box, e -> !(e instanceof ServerPlayer))) {
			e.discard();
		}
	}

	// ------------------------------------------------------------------ ticking

	private static void tick(final MinecraftServer server) {
		if (HUNTS.isEmpty()) {
			return;
		}
		ServerLevel level = DungeonDimension.level(server);
		for (Hunt hunt : List.copyOf(HUNTS.values())) {
			ServerPlayer player = server.getPlayerList().getPlayer(hunt.player);
			QuestDef quest = player == null ? null : QuestService.active(player);
			// fell (sent home by Dungeons), walked out some other way, or the trial changed: the hunt is over
			if (player == null || level == null || player.level() != level || !inside(hunt.origin, player.position(), 2.0) || quest == null
				|| !quest.id().equals(hunt.quest)) {
				HUNTS.remove(hunt.player);
				clear(server, hunt);
				continue;
			}
			hunt.age++;
			hunt.mobs.removeIf(id -> !(level.getEntity(id) instanceof Mob mob) || !mob.isAlive());
			List<Set<EntityType<?>>> needs = needs(player);
			if (needs.isEmpty()) {
				if (hunt.doneFor == 0) {
					player.sendSystemMessage(Component.translatable("message.minecraft_mode.hunt.done", DONE_TICKS / 20).withStyle(ChatFormatting.GREEN));
				}
				if (++hunt.doneFor >= DONE_TICKS) {
					leave(player, "message.minecraft_mode.hunt.finished");
				}
				continue;
			}
			hunt.doneFor = 0;
			if (hunt.age % SPAWN_INTERVAL == 0) {
				spawn(level, hunt, player, needs);
			}
		}
	}

	private static void spawn(final ServerLevel level, final Hunt hunt, final ServerPlayer player, final List<Set<EntityType<?>>> needs) {
		int load = 0;
		boolean bossUp = false;
		for (UUID id : hunt.mobs) {
			Entity e = level.getEntity(id);
			boolean boss = e != null && Quests.BOSSES.contains(e.getType());
			bossUp |= boss;
			load += boss ? BOSS_WEIGHT : 1;
		}
		RandomSource random = level.getRandom();
		for (int tries = 0; load < CAP && tries < CAP * 2; tries++) {
			List<EntityType<?>> pool = new ArrayList<>(needs.get(random.nextInt(needs.size())));
			EntityType<?> type = pool.get(random.nextInt(pool.size()));
			boolean boss = Quests.BOSSES.contains(type);
			if (boss && bossUp) {
				continue;
			}
			Mob mob = create(level, type, hunt.origin, player, random);
			if (mob == null) {
				continue;
			}
			hunt.mobs.add(mob.getUUID());
			bossUp |= boss;
			load += boss ? BOSS_WEIGHT : 1;
		}
	}

	private static @Nullable Mob create(final ServerLevel level, final EntityType<?> type, final BlockPos origin, final ServerPlayer player, final RandomSource random) {
		if (!(type.create(level, EntitySpawnReason.EVENT) instanceof Mob mob)) {
			return null;
		}
		int mid = 1 + SIZE / 2;
		Vec3 at = null;
		if (SWIMMING.contains(type)) {
			at = new Vec3(origin.getX() + mid + 0.5, origin.getY() - 3, origin.getZ() + mid + 0.5);
		} else {
			for (int i = 0; i < 12 && at == null; i++) {
				double x = origin.getX() + 2 + random.nextInt(SIZE - 2) + 0.5;
				double z = origin.getZ() + 2 + random.nextInt(SIZE - 2) + 0.5;
				boolean pool = Math.abs(x - (origin.getX() + mid + 0.5)) <= 4.5 && Math.abs(z - (origin.getZ() + mid + 0.5)) <= 4.5;
				Vec3 candidate = new Vec3(x, origin.getY() + (FLYING.contains(type) ? 6 : 0), z);
				if (!pool && candidate.distanceTo(player.position()) >= 10.0) {
					at = candidate;
				}
			}
		}
		if (at == null) {
			return null;
		}
		mob.snapTo(at.x, at.y, at.z, random.nextFloat() * 360.0F, 0.0F);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), EntitySpawnReason.EVENT, null);
		if (mob instanceof Hoglin hoglin) {
			hoglin.setImmuneToZombification(true);
		}
		if (mob instanceof AbstractPiglin piglin) {
			piglin.setImmuneToZombification(true);
		}
		mob.addTag(TAG);
		mob.setPersistenceRequired();
		mob.setTarget(player);
		level.addFreshEntity(mob);
		return mob;
	}

	private static void clear(final MinecraftServer server, final Hunt hunt) {
		ServerLevel level = DungeonDimension.level(server);
		if (level == null) {
			return;
		}
		for (UUID id : hunt.mobs) {
			Entity e = level.getEntity(id);
			if (e != null) {
				e.discard();
			}
		}
		hunt.mobs.clear();
	}

	private TrialHunts() {
	}
}
