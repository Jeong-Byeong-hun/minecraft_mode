package com.minecraftmode.city;

import com.minecraftmode.job.JobProgression;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Training Grounds: a sealed hall under the capital for adventurers who have not chosen a class yet, reached by a stairway in
 * the park south-east of the plaza. While a classless player is inside, monsters (zombies, skeletons, spiders, drowned) keep
 * stepping out of the nine alcoves, up to {@link #BASE_CAP} plus
 * {@link #PER_TRAINEE} per trainee, and give classless killers {@link #EXP_MULTIPLIER} times the class experience. The monsters carry
 * {@link #TAG}: the city guards leave them alone inside the hall (and drive off any that follow a player up the stairs), and they
 * vanish when nobody is training. The hall is built by the city generator like every building: a pure function of coordinates.
 */
public final class TrainingGrounds {
	public static final String TAG = "minecraft_mode.training";
	/** Class experience multiplier for classless players killing training monsters. */
	public static final int EXP_MULTIPLIER = 2;
	public static final int BASE_CAP = 4;
	public static final int PER_TRAINEE = 2;
	public static final int MAX_CAP = 10;
	/** Monsters never step out of an alcove closer than this to a player. */
	private static final double SPAWN_CLEARANCE = 6.0;

	// geometry: absolute x/z, dy relative to the city floor (dy 0 = first air layer)
	/** The stairway: 3 wide, its top step at {@link #STAIR_Z}, one step down per block south. */
	private static final int STAIR_X0 = 22;
	private static final int STAIR_X1 = 24;
	private static final int STAIR_Z = 13;
	/** Steps down to the hall floor; steps up to {@link #OPEN_STEPS} - 1 are open to the sky under the pavilion. */
	private static final int STEPS = 17;
	private static final int OPEN_STEPS = 4;
	/** Hall interior (air): x, z and the walking layer {@link #FLOOR} up to {@link #TOP}. */
	private static final int HALL_X0 = 10;
	private static final int HALL_X1 = 36;
	private static final int HALL_Z0 = 31;
	private static final int HALL_Z1 = 57;
	public static final int FLOOR = -17;
	private static final int TOP = -10;
	private static final int[] SIDE_ALCOVES = {37, 44, 51};
	private static final int[] BACK_ALCOVES = {16, 23, 30};
	private static final int ALCOVE_DEPTH = 3;
	/** Where monsters step in: the middle of each alcove, {x, z}. */
	public static final List<int[]> SPAWNS = spawns();

	private static final List<EntityType<? extends Mob>> ROSTER = List.of(EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.SPIDER, EntityTypes.DROWNED);
	private static final int[] WEIGHTS = {3, 3, 3, 1};

	/** Players inside on the last tick (for the greeting when someone walks in). */
	private static final Set<UUID> PRESENT = new HashSet<>();

	private static List<int[]> spawns() {
		List<int[]> out = new ArrayList<>();
		for (int z : SIDE_ALCOVES) {
			out.add(new int[] {HALL_X0 - 1 - ALCOVE_DEPTH / 2 - 1, z});
			out.add(new int[] {HALL_X1 + 1 + ALCOVE_DEPTH / 2 + 1, z});
		}
		for (int x : BACK_ALCOVES) {
			out.add(new int[] {x, HALL_Z1 + 1 + ALCOVE_DEPTH / 2 + 1});
		}
		return List.copyOf(out);
	}

	/** The top of the stairway, in the park south-east of the fountain (for directions). */
	public static BlockPos entrance(final int base) {
		return new BlockPos((STAIR_X0 + STAIR_X1) / 2, base, STAIR_Z - 1);
	}

	/** The middle of the hall floor (walking layer). */
	public static BlockPos center(final int base) {
		return new BlockPos((HALL_X0 + HALL_X1) / 2, base + FLOOR, (HALL_Z0 + HALL_Z1) / 2);
	}

	/** The hall and its alcoves (not the stairway). */
	public static AABB area(final int base) {
		return new AABB(HALL_X0 - 1 - ALCOVE_DEPTH, base + FLOOR, HALL_Z0, HALL_X1 + 2 + ALCOVE_DEPTH, base + TOP + 1, HALL_Z1 + 2 + ALCOVE_DEPTH);
	}

	// ------------------------------------------------------------------ building

	static void build(final Build b) {
		if (!b.touches(HALL_X0 - 2 - ALCOVE_DEPTH, STAIR_Z - 2, HALL_X1 + 2 + ALCOVE_DEPTH, HALL_Z1 + 2 + ALCOVE_DEPTH)) {
			return;
		}
		hall(b);
		alcoves(b);
		stairway(b);
		pavilion(b);
	}

	private static void hall(final Build b) {
		int floor = FLOOR - 1;
		int ceiling = TOP + 1;
		b.fill(HALL_X0 - 1, floor, HALL_Z0 - 1, HALL_X1 + 1, ceiling, HALL_Z1 + 1, Blocks.STONE_BRICKS);
		b.air(HALL_X0, FLOOR, HALL_Z0, HALL_X1, TOP, HALL_Z1);
		for (int x = HALL_X0; x <= HALL_X1; x++) {
			for (int z = HALL_Z0; z <= HALL_Z1; z++) {
				b.set(x, floor, z, Build.paving(x, z));
				// a lit grid in the ceiling
				if ((x - HALL_X0 - 3) % 5 == 0 && (z - HALL_Z0 - 3) % 5 == 0) {
					b.set(x, ceiling, z, Blocks.SEA_LANTERN);
				}
			}
		}
		// the sparring ring in the middle and a band along the walls
		BlockPos mid = center(0);
		b.cylinder(mid.getX(), mid.getZ(), 5.0, floor, floor, Blocks.POLISHED_ANDESITE, true);
		b.cylinder(mid.getX(), mid.getZ(), 1.0, floor, floor, Blocks.CHISELED_STONE_BRICKS, false);
		b.walls(HALL_X0 - 1, FLOOR + 3, HALL_Z0 - 1, HALL_X1 + 1, FLOOR + 3, HALL_Z1 + 1, Blocks.POLISHED_DEEPSLATE);
		for (int z = HALL_Z0 + 2; z <= HALL_Z1; z += 4) {
			b.set(HALL_X0 - 1, FLOOR + 3, z, Blocks.SEA_LANTERN);
			b.set(HALL_X1 + 1, FLOOR + 3, z, Blocks.SEA_LANTERN);
		}
		for (int x = HALL_X0 + 2; x <= HALL_X1; x += 4) {
			b.set(x, FLOOR + 3, HALL_Z0 - 1, Blocks.SEA_LANTERN);
			b.set(x, FLOOR + 3, HALL_Z1 + 1, Blocks.SEA_LANTERN);
		}
		// four pillars for cover, with a lit ring
		for (int px : new int[] {HALL_X0 + 5, HALL_X1 - 6}) {
			for (int pz : new int[] {HALL_Z0 + 7, HALL_Z1 - 8}) {
				b.fill(px, FLOOR, pz, px + 1, TOP, pz + 1, Blocks.STONE_BRICKS);
				b.fill(px, FLOOR, pz, px + 1, FLOOR, pz + 1, Blocks.CHISELED_STONE_BRICKS);
				b.fill(px, FLOOR + 3, pz, px + 1, FLOOR + 3, pz + 1, Blocks.SEA_LANTERN);
				b.fill(px, TOP, pz, px + 1, TOP, pz + 1, Blocks.CHISELED_STONE_BRICKS);
			}
		}
		// archery targets by the door, hay in the corners
		for (int x : new int[] {STAIR_X0 - 4, STAIR_X1 + 4}) {
			b.set(x, FLOOR + 1, HALL_Z0 - 1, Blocks.TARGET);
		}
		int[][] corners = {{HALL_X0, HALL_Z0}, {HALL_X1, HALL_Z0}, {HALL_X0, HALL_Z1}, {HALL_X1, HALL_Z1}};
		for (int[] c : corners) {
			b.set(c[0], FLOOR, c[1], Blocks.HAY_BLOCK);
		}
		// the doorway from the stairs
		b.air(STAIR_X0, FLOOR, HALL_Z0 - 1, STAIR_X1, FLOOR + 2, HALL_Z0 - 1);
		b.fill(STAIR_X0 - 1, FLOOR, HALL_Z0 - 1, STAIR_X0 - 1, FLOOR + 3, HALL_Z0 - 1, Blocks.CHISELED_STONE_BRICKS);
		b.fill(STAIR_X1 + 1, FLOOR, HALL_Z0 - 1, STAIR_X1 + 1, FLOOR + 3, HALL_Z0 - 1, Blocks.CHISELED_STONE_BRICKS);
		b.fill(STAIR_X0, FLOOR + 3, HALL_Z0 - 1, STAIR_X1, FLOOR + 3, HALL_Z0 - 1, Blocks.CHISELED_STONE_BRICKS);
	}

	/** Nine alcoves the monsters step out of, each marked by a pair of soul torches. */
	private static void alcoves(final Build b) {
		for (int z : SIDE_ALCOVES) {
			alcove(b, HALL_X0 - 1 - ALCOVE_DEPTH, z - 1, HALL_X0 - 1, z + 1, Direction.EAST);
			alcove(b, HALL_X1 + 1, z - 1, HALL_X1 + 1 + ALCOVE_DEPTH, z + 1, Direction.WEST);
		}
		for (int x : BACK_ALCOVES) {
			alcove(b, x - 1, HALL_Z1 + 1, x + 1, HALL_Z1 + 1 + ALCOVE_DEPTH, Direction.NORTH);
		}
	}

	/**
	 * Carves an alcove (x0..x1, z0..z1 including the hall wall it opens through, 3 high) into a stone shell that stays behind that
	 * wall; {@code into} points from the alcove into the hall.
	 */
	private static void alcove(final Build b, final int x0, final int z0, final int x1, final int z1, final Direction into) {
		b.fill(into == Direction.WEST ? x0 : x0 - 1, FLOOR - 1, into == Direction.NORTH ? z0 : z0 - 1, into == Direction.EAST ? x1 : x1 + 1, FLOOR + 3, z1 + 1,
			Blocks.STONE_BRICKS);
		b.fill(x0, FLOOR - 1, z0, x1, FLOOR - 1, z1, Blocks.POLISHED_BLACKSTONE_BRICKS);
		b.air(x0, FLOOR, z0, x1, FLOOR + 2, z1);
		// the opening's frame and a soul torch on each side, on the hall wall
		boolean alongX = into.getAxis() == Direction.Axis.Z;
		int wallX = into == Direction.EAST ? x1 : into == Direction.WEST ? x0 : 0;
		int wallZ = into == Direction.NORTH ? z0 : 0;
		var torch = Blocks.SOUL_WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, into);
		if (alongX) {
			b.fill(x0, FLOOR + 3, wallZ, x1, FLOOR + 3, wallZ, Blocks.CHISELED_STONE_BRICKS);
			b.set(x0 - 1, FLOOR + 2, wallZ - 1, torch);
			b.set(x1 + 1, FLOOR + 2, wallZ - 1, torch);
		} else {
			b.fill(wallX, FLOOR + 3, z0, wallX, FLOOR + 3, z1, Blocks.CHISELED_STONE_BRICKS);
			b.set(wallX + into.getStepX(), FLOOR + 2, z0 - 1, torch);
			b.set(wallX + into.getStepX(), FLOOR + 2, z1 + 1, torch);
		}
	}

	/** Straight stairs down to the hall door, lit by sea lanterns in the walls. */
	private static void stairway(final Build b) {
		for (int k = 0; k < STEPS; k++) {
			int z = STAIR_Z + k;
			int step = -1 - k;
			boolean open = k < OPEN_STEPS;
			int top = open ? -1 : -k + 3;
			b.fill(STAIR_X0 - 1, step - 1, z, STAIR_X1 + 1, top, z, Blocks.STONE_BRICKS);
			b.air(STAIR_X0, step + 1, z, STAIR_X1, open ? 2 : -k + 2, z);
			for (int x = STAIR_X0; x <= STAIR_X1; x++) {
				b.set(x, step, z, Build.stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH));
			}
			if (!open && k % 4 == 1) {
				b.set(STAIR_X0 - 1, step + 2, z, Blocks.SEA_LANTERN);
				b.set(STAIR_X1 + 1, step + 2, z, Blocks.SEA_LANTERN);
			}
		}
	}

	/** A stone pavilion over the open top of the stairs, with a railing round the opening. */
	private static void pavilion(final Build b) {
		int x0 = STAIR_X0 - 1;
		int x1 = STAIR_X1 + 1;
		int z0 = STAIR_Z - 1;
		int z1 = STAIR_Z + OPEN_STEPS;
		b.fill(x0, -1, z0, x1, -1, z0, Blocks.POLISHED_ANDESITE);
		// generation places blocks without shape updates, so the railing gets its connections here
		BlockState wall = Blocks.STONE_BRICK_WALL.defaultBlockState().setValue(WallBlock.UP, false);
		BlockState alongZ = wall.setValue(WallBlock.NORTH, WallSide.LOW).setValue(WallBlock.SOUTH, WallSide.LOW);
		b.fill(x0, 0, STAIR_Z, x0, 0, z1, alongZ);
		b.fill(x1, 0, STAIR_Z, x1, 0, z1, alongZ);
		b.fill(x0, 0, z1, x1, 0, z1, wall.setValue(WallBlock.EAST, WallSide.LOW).setValue(WallBlock.WEST, WallSide.LOW));
		for (int x : new int[] {x0, x1}) {
			for (int z : new int[] {z0, z1}) {
				b.fill(x, 0, z, x, 3, z, Blocks.STONE_BRICKS);
			}
		}
		b.hipRoof(x0, z0, x1, z1, 4, Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICKS);
		b.lantern((STAIR_X0 + STAIR_X1) / 2, 3, STAIR_Z + 1, true, false);
	}

	/**
	 * Worlds whose capital was generated before the Training Grounds existed get the hall once, as soon as every chunk it touches is
	 * loaded (checked first: reading a block of an unloaded chunk would load it). A generated hall has a chiseled stone brick in the
	 * middle of its floor; natural terrain never does.
	 */
	public static void ensureBuilt(final ServerLevel level) {
		if (!CityZone.isCityLevel(level)) {
			return;
		}
		int cx0 = (HALL_X0 - 2 - ALCOVE_DEPTH) >> 4;
		int cx1 = (HALL_X1 + 2 + ALCOVE_DEPTH) >> 4;
		int cz0 = (STAIR_Z - 2) >> 4;
		int cz1 = (HALL_Z1 + 2 + ALCOVE_DEPTH) >> 4;
		for (int cx = cx0; cx <= cx1; cx++) {
			for (int cz = cz0; cz <= cz1; cz++) {
				if (!level.hasChunk(cx, cz)) {
					return;
				}
			}
		}
		int base = CityZone.baseY(level);
		if (level.getBlockState(center(base).below()).is(Blocks.CHISELED_STONE_BRICKS)) {
			return;
		}
		for (int cx = cx0; cx <= cx1; cx++) {
			for (int cz = cz0; cz <= cz1; cz++) {
				build(new Build(level, base, cx << 4, cz << 4));
			}
		}
		com.minecraftmode.MinecraftMode.LOGGER.info("Built the Training Grounds under the existing capital");
	}

	// ------------------------------------------------------------------ runtime

	/** A player the hall works for: alive, not spectating, no class yet. */
	public static boolean trainee(final Player player) {
		return player.isAlive() && !player.isSpectator() && !JobProgression.get(player).hasClass();
	}

	/** A training monster inside the hall (the guards leave these alone). */
	public static boolean holds(final Mob mob) {
		return mob.entityTags().contains(TAG) && mob.level() instanceof ServerLevel level && CityZone.isCityLevel(level)
			&& area(CityZone.baseY(level)).contains(mob.position());
	}

	/** Class experience multiplier for {@code killer} defeating {@code victim}. */
	public static int expMultiplier(final LivingEntity victim, final Player killer) {
		return victim.entityTags().contains(TAG) && !JobProgression.get(killer).hasClass() ? EXP_MULTIPLIER : 1;
	}

	/** Once a second: greet newcomers, keep monsters coming while someone trains, clear them when nobody does. */
	public static void tick(final ServerLevel level) {
		if (!CityZone.isCityLevel(level)) {
			return;
		}
		int base = CityZone.baseY(level);
		AABB area = area(base);
		List<ServerPlayer> inside = new ArrayList<>();
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator() && area.contains(player.position())) {
				inside.add(player);
			}
		}
		Set<UUID> now = new HashSet<>();
		for (ServerPlayer player : inside) {
			now.add(player.getUUID());
			if (!PRESENT.contains(player.getUUID())) {
				player.sendSystemMessage(Component.translatable(trainee(player) ? "message.minecraft_mode.training.enter" : "message.minecraft_mode.training.classed",
					EXP_MULTIPLIER).withStyle(ChatFormatting.GOLD));
			}
		}
		PRESENT.clear();
		PRESENT.addAll(now);

		List<ServerPlayer> trainees = inside.stream().filter(TrainingGrounds::trainee).toList();
		List<Mob> mobs = level.getEntitiesOfClass(Mob.class, area.inflate(24.0), m -> m.entityTags().contains(TAG));
		if (trainees.isEmpty()) {
			for (Mob mob : mobs) {
				level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + mob.getBbHeight() / 2.0, mob.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
				mob.discard();
			}
			return;
		}
		if (mobs.size() < cap(trainees.size())) {
			spawn(level, base, inside);
		}
	}

	/** Monsters kept in the hall for this many trainees. */
	public static int cap(final int trainees) {
		return Math.min(MAX_CAP, BASE_CAP + PER_TRAINEE * trainees);
	}

	private static void spawn(final ServerLevel level, final int base, final List<ServerPlayer> inside) {
		RandomSource random = level.getRandom();
		List<BlockPos> free = new ArrayList<>();
		for (int[] s : SPAWNS) {
			BlockPos pos = new BlockPos(s[0], base + FLOOR, s[1]);
			Vec3 at = Vec3.atBottomCenterOf(pos);
			if (level.isLoaded(pos) && level.shouldTickBlocksAt(pos) && inside.stream().noneMatch(p -> p.position().distanceToSqr(at) < SPAWN_CLEARANCE * SPAWN_CLEARANCE)) {
				free.add(pos);
			}
		}
		if (free.isEmpty()) {
			return;
		}
		BlockPos pos = free.get(random.nextInt(free.size()));
		Mob mob = pick(random).create(level, EntitySpawnReason.SPAWNER);
		if (mob == null) {
			return;
		}
		mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.SPAWNER, null);
		// plain monsters only: no babies, no jockeys
		if (mob instanceof Zombie zombie) {
			zombie.setBaby(false);
		}
		Entity vehicle = mob.getVehicle();
		if (vehicle != null) {
			mob.stopRiding();
			vehicle.discard();
		}
		mob.ejectPassengers();
		mob.addTag(TAG);
		level.addFreshEntity(mob);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, mob.getX(), mob.getY() + 1.0, mob.getZ(), 16, 0.3, 0.6, 0.3, 0.02);
		level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.0F, 0.8F);
	}

	/** A weighted pick from the roster. */
	private static EntityType<? extends Mob> pick(final RandomSource random) {
		int total = 0;
		for (int w : WEIGHTS) {
			total += w;
		}
		int roll = random.nextInt(total);
		for (int i = 0; i < ROSTER.size(); i++) {
			roll -= WEIGHTS[i];
			if (roll < 0) {
				return ROSTER.get(i);
			}
		}
		return ROSTER.getFirst();
	}

	private TrainingGrounds() {
	}
}
