package com.minecraftmode.test;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.lair.LairClearScreen;
import com.minecraftmode.consumable.ConsumableItem;
import com.minecraftmode.entity.MobPower;
import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.SpaceCheck;
import com.minecraftmode.entity.combat.Attacks;
import com.minecraftmode.entity.named.NamedMob;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.gear.GearArmorItem;
import com.minecraftmode.job.weapon.JobWeaponItem;
import com.minecraftmode.loot.EvolutionEtherItem;
import com.minecraftmode.network.LairTravelPayload;
import com.minecraftmode.progress.ResetCycle;
import com.minecraftmode.registry.ModBlocks;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.worldgen.lair.LairChestBlock;
import com.minecraftmode.worldgen.lair.LairChestBlockEntity;
import com.minecraftmode.worldgen.lair.LairDef;
import com.minecraftmode.worldgen.lair.LairExp;
import com.minecraftmode.worldgen.lair.LairLoot;
import com.minecraftmode.worldgen.lair.LairPiece;
import com.minecraftmode.worldgen.lair.LairTravel;
import com.minecraftmode.worldgen.lair.Maze;
import com.minecraftmode.worldgen.lair.NamedLairs;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Named monster lairs: the 22 definitions and their spawn lists, the goal loot rules, mazes that
 * always reach their goal, and on a flat world one lair of every landmark shape (plus a Nether lair)
 * built with /place: walls, gate, a tall landmark, the personal goal chest with coins and a reward, and the
 * lair lord that wakes for the cycle and seals the chest while it lives. Screenshots of every shape.
 */
public class LairClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(final ClientGameTestContext context) {
		if (TestSelection.skip(this)) {
			return;
		}
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerConnection connection = singleplayer.getConnection();
			TestServerContext server = singleplayer.getServer();
			connection.waitForChunksRender();
			// item stacks need the loaded registries, so the loot rules are checked with a world open
			server.runOnServer(s -> checkDefinitions());
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("time set noon");
			server.runCommand("gamemode creative @p");
			checkRegistry(server);

			String[][] lairs = {
				{"dune_scorpion", "600", "600"}, {"bandit_captain", "-600", "600"}, {"goblin_warchief", "600", "-600"},
				{"golden_enderman", "-600", "-600"}, {"drowned_corsair", "1000", "0"}, {"ancient_treant", "-1000", "0"}, {"wendigo", "0", "1000"}
			};
			for (String[] lair : lairs) {
				placeAndCheck(context, server, Level.OVERWORLD, lair[0], Integer.parseInt(lair[1]), Integer.parseInt(lair[2]), -60, true);
			}
			placeAndCheck(context, server, Level.NETHER, "wither_knight", 600, 600, 40, false);
			context.runOnClient(minecraft -> minecraft.options.renderDistance().set(5));
			server.runOnServer(s -> s.getPlayerList().setViewDistance(5));
			WorldClose.prepare(context, server);
		}
	}

	/** Definitions, sizes, loot rules and maze properties. */
	private static void checkDefinitions() {
		require(NamedLairs.all().size() == NamedMobs.all().size(), "every named monster needs a lair, got " + NamedLairs.all().size());
		Map<LairDef.Shape, Integer> shapes = new EnumMap<>(LairDef.Shape.class);
		for (LairDef def : NamedLairs.all()) {
			NamedDef named = def.named();
			require(!def.biomes().isEmpty(), def.id() + " has no biomes");
			require(def.size() >= 53 && def.size() <= 77, def.id() + " size " + def.size());
			shapes.merge(def.shape(), 1, Integer::sum);
			// loot: coins always and at least one reward, for many rolls
			for (int seed = 0; seed < 40; seed++) {
				List<ItemStack> loot = LairLoot.goal(named, null, RandomSource.create(seed * 7919L + def.id().hashCode()));
				require(loot.stream().anyMatch(LairClientGameTest::isCoin), def.id() + " goal without coins");
				require(loot.stream().anyMatch(LairClientGameTest::isReward), def.id() + " goal without a reward");
				require(loot.stream().anyMatch(s -> s.getItem() instanceof EvolutionEtherItem), def.id() + " goal without ether");
			}
		}
		require(shapes.keySet().size() == LairDef.Shape.values().length, "every landmark shape should be used: " + shapes);
		// the goal moves around: over many seeds all three kinds appear and every goal is reachable
		Map<Maze.GoalKind, Integer> kinds = new EnumMap<>(Maze.GoalKind.class);
		for (long seed = 0; seed < 200; seed++) {
			Maze maze = new Maze(15, seed);
			require(maze.distance(maze.goalX(), maze.goalZ()) >= 0, "unreachable goal for seed " + seed);
			kinds.merge(maze.goalKind(), 1, Integer::sum);
		}
		require(kinds.size() == 3, "goal kinds should vary: " + kinds);
		MinecraftMode.LOGGER.info("[lair] {} lairs, shapes {}, goals over 200 mazes {}", NamedLairs.all().size(), shapes, kinds);
	}

	/** The structures and their shared set are registered, and each lair favours its named monster. */
	private static void checkRegistry(final TestServerContext server) {
		String report = server.computeOnServer(s -> {
			var structures = s.registryAccess().lookupOrThrow(Registries.STRUCTURE);
			for (LairDef def : NamedLairs.all()) {
				Structure structure = structures.getValue(def.key());
				require(structure != null, "missing structure " + def.key());
				StructureSpawnOverride monsters = structure.spawnOverrides().get(MobCategory.MONSTER);
				require(monsters != null, def.id() + " has no monster spawn list");
				int named = 0;
				int total = 0;
				for (var entry : monsters.spawns().unwrap()) {
					total += entry.weight();
					MobSpawnSettings.SpawnerData data = entry.value();
					if (data.type() == NamedMobs.type(def.named())) {
						named += entry.weight();
					}
				}
				float share = named / (float)total;
				require(share >= 0.05F && share <= 0.21F, def.id() + " named share " + share);
				require(share > def.named().rarity(), def.id() + " should spawn its named monster more often than its habitat does");
			}
			StructureSet set = s.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getValue(NamedLairs.SET);
			require(set != null && set.structures().size() == NamedLairs.all().size(), "the lair set should hold every lair");
			return "22 structures, one set, named share " + NamedLairs.all().stream().map(d -> d.id() + "=" + d.mobs().namedWeight(d.named()))
				.limit(3).toList();
		});
		MinecraftMode.LOGGER.info("[lair] registry: {}", report);
	}

	private static void placeAndCheck(final ClientGameTestContext context, final TestServerContext server, final ResourceKey<Level> dimension, final String id,
		final int px, final int pz, final int y, final boolean screenshot) {
		// a lair is centered on the middle of the chunk it starts in
		int x = (px >> 4) * 16 + 8;
		int z = (pz >> 4) * 16 + 8;
		LairDef def = NamedLairs.def(id);
		require(def != null, "unknown lair " + id);
		String in = "execute in " + dimension.identifier() + " run ";
		int reach = def.size() / 2 + LairPiece.APRON + LairPiece.GROUNDS + 8;
		server.runCommand(in + "forceload add " + (x - reach) + " " + (z - reach) + " " + (x + reach) + " " + (z + reach));
		for (int i = 0; i < 60; i++) {
			boolean loaded = server.computeOnServer(s -> {
				ServerLevel level = s.getLevel(dimension);
				for (int cx = (x - reach) >> 4; cx <= (x + reach) >> 4; cx++) {
					for (int cz = (z - reach) >> 4; cz <= (z + reach) >> 4; cz++) {
						if (!level.hasChunk(cx, cz)) {
							return false;
						}
					}
				}
				return true;
			});
			if (loaded) {
				break;
			}
			context.waitTicks(10);
		}
		server.runCommand(in + "place structure minecraft_mode:" + def.structureId() + " " + x + " " + y + " " + z);
		context.waitTicks(5);
		int[] result = server.computeOnServer(s -> {
			ServerLevel level = s.getLevel(dimension);
			int half = def.size() / 2;
			BlockPos chest = null;
			int top = Integer.MIN_VALUE;
			int barrels = 0;
			for (int bx = x - half; bx <= x + half; bx++) {
				for (int bz = z - half; bz <= z + half; bz++) {
					for (int by = y - 24; by <= y + 140; by++) {
						var state = level.getBlockState(new BlockPos(bx, by, bz));
						if (state.isAir()) {
							continue;
						}
						top = Math.max(top, by);
						if (state.is(ModBlocks.LAIR_CHEST)) {
							require(chest == null, id + ": more than one goal chest");
							chest = new BlockPos(bx, by, bz);
						} else if (state.is(ModBlocks.LAIR_CACHE)) {
							barrels++;
						}
					}
				}
			}
			require(chest != null, id + ": no goal chest");
			int floor = chest.getY() - 1;
			require(level.getBlockState(new BlockPos(x, floor + 2, z + half)).isAir(), id + ": the gate in the south wall should be open");
			int walls = 0;
			int open = 0;
			for (int bx = x - half; bx <= x + half; bx++) {
				for (int bz = z - half; bz <= z + half; bz++) {
					if (level.getBlockState(new BlockPos(bx, floor + 2, bz)).isAir()) {
						open++;
					} else {
						walls++;
					}
				}
			}
			float wallShare = walls / (float)(walls + open);
			require(wallShare > 0.25F && wallShare < 0.75F, id + ": maze wall share " + wallShare);
			require(level.getBlockState(new BlockPos(x - half, floor + 2, z - half)).isSolid(), id + ": the corner should be walled");
			int height = top - floor;
			if (def.shape() == LairDef.Shape.TREE) {
				StringBuilder trunk = new StringBuilder();
				int logs = 0;
				for (int dy = 6; dy <= 36; dy += 6) {
					var state = level.getBlockState(new BlockPos(x + 5, floor + dy, z));
					trunk.append(dy).append('=').append(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath()).append(' ');
					logs += state.is(def.palette().wall()) ? 1 : 0;
				}
				MinecraftMode.LOGGER.info("[lair] {} trunk column: {}", id, trunk);
				require(logs >= 4, id + ": the tree should have a trunk, got " + trunk);
			}
			require(def.shape() == LairDef.Shape.NONE || height >= 24, id + ": the landmark should rise high above the maze, got " + height);
			// personal loot (the test player's own roll for this cycle)
			require(level.getBlockEntity(chest) instanceof LairChestBlockEntity, id + ": the goal chest has no block entity");
			LairChestBlockEntity goal = (LairChestBlockEntity)level.getBlockEntity(chest);
			require(def.id().equals(goal.lair()) && !goal.isCache(), id + ": the goal chest is not set up for its lair");
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			long cycle = ResetCycle.cycle(level);
			int coins = 0;
			int rewards = 0;
			Container container = goal.container(player, cycle);
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);
				coins += isCoin(stack) ? 1 : 0;
				rewards += isReward(stack) ? 1 : 0;
			}
			require(coins > 0, id + ": the goal chest has no coins");
			require(rewards > 0, id + ": the goal chest has no reward item");
			// no guardian waits in the structure; the lord wakes for the cycle and seals the chest
			require(level.getEntitiesOfClass(NamedMob.class, new AABB(chest).inflate(6), NamedMob::isLord).isEmpty(), id + ": a lord before anyone came");
			NamedMob lord = goal.wakeLord(level, cycle);
			require(lord != null && lord.isLord() && goal.sealed(), id + ": the lord should wake and seal the chest");
			require(NamedMobs.def(lord.getType()) == def.named(), id + ": the lord should be the lair's named monster");
			require(!LairChestBlock.open(player, goal), id + ": a sealed chest must not open");
			lord.setNoAi(true);
			// the lair map loop's gate spot: kept by the chest, just outside the gate (south of the maze), with safe ground to stand on
			BlockPos exit = goal.exit(level);
			require(exit != null && exit.equals(LairPiece.entrance(def, x, floor, z)), id + ": the chest should know its gate spot "
				+ LairPiece.entrance(def, x, floor, z) + ", got " + exit);
			require(exit.getZ() > z + half && exit.getZ() <= z + half + LairPiece.APRON, id + ": the gate spot should be just outside the maze, got " + exit);
			BlockPos spot = LairTravel.safeSpot(level, exit);
			require(spot != null && spot.distManhattan(exit) <= 4 && LairTravel.safe(level, spot), id + ": no safe spot by the gate " + exit + ", got " + spot);
			require(level.getBlockState(new BlockPos(x, floor + 2, z + half)).isAir(), id + ": the gate is open in front of the spot");
			return new int[] {chest.getX(), chest.getY(), chest.getZ(), height, coins, rewards, barrels};
		});
		MinecraftMode.LOGGER.info("[lair] {} at {}, {}: goal {} {} {}, landmark {} blocks high, {} coin and {} reward stacks, {} caches", id, x, z,
			result[0], result[1], result[2], result[3], result[4], result[5], result[6]);
		if (screenshot) {
			context.runOnClient(minecraft -> minecraft.options.renderDistance().set(10));
			server.runOnServer(s -> s.getPlayerList().setViewDistance(10));
			server.runCommand("gamemode spectator @p");
			int floor = result[1] - 1;
			int distance = def.size() + 18;
			server.runCommand(in + "tp @p " + (x + distance * 0.3) + " " + (floor + result[3] * 0.55 + 6) + " " + (z + distance));
			context.waitTicks(20);
			context.getInput().lookAt(new BlockPos(x, floor + result[3] / 3, z));
			context.waitTicks(240);
			// a static camera does not see sections that arrived after it stopped; a small step refreshes the view
			double cameraX = x + distance * 0.3 + 1.0;
			server.runCommand(in + "tp @p " + cameraX + " " + (floor + result[3] * 0.55 + 6) + " " + (z + distance));
			context.waitTicks(10);
			context.getInput().lookAt(new BlockPos(x, floor + result[3] / 3, z));
			context.waitTicks(40);
			context.takeScreenshot("lair_" + id);
			server.runCommand("gamemode creative @p");
		}
		if (id.equals("wendigo")) {
			checkLoop(context, server, dimension, def, new BlockPos(result[0], result[1], result[2]));
		}
		if (id.equals("ancient_treant")) {
			checkRoom(context, server, dimension, new BlockPos(result[0], result[1], result[2]));
		}
		server.runCommand(in + "forceload remove all");
	}

	/**
	 * Big monsters in the four-high corridors (the tallest named monster, the treant, 3.6 high): its lord grows only as far as the room
	 * allows, walks without choking, comes back home into free space after being dragged off, and minions summoned by it land where they fit.
	 * Until 0.5.46 the 1.2x lord stood in the roof and suffocated, and minions were dropped into walls.
	 */
	private static void checkRoom(final ClientGameTestContext context, final TestServerContext server, final ResourceKey<Level> dimension, final BlockPos chest) {
		int lordId = server.computeOnServer(s -> {
			ServerLevel level = s.getLevel(dimension);
			NamedMob lord = level.getEntitiesOfClass(NamedMob.class, new AABB(chest).inflate(8), NamedMob::isLord).stream().findFirst().orElse(null);
			require(lord != null, "the treant lord should still be up");
			lord.setNoAi(false);
			lord.setHealth(lord.getMaxHealth());
			return lord.getId();
		});
		context.waitTicks(100);
		String walked = server.computeOnServer(s -> {
			ServerLevel level = s.getLevel(dimension);
			NamedMob lord = (NamedMob)level.getEntity(lordId);
			require(lord != null && lord.isAlive(), "the treant lord should still be alive");
			require(lord.getHealth() == lord.getMaxHealth(), "the treant lord lost health on its own: " + lord.getHealth() + " / " + lord.getMaxHealth());
			require(SpaceCheck.fits(level, lord, lord.position()), "the treant lord should stand free of the walls at scale " + lord.getScale());
			float scale = lord.getScale();
			// dragged off down a corridor, it comes home to a free spot
			lord.teleportTo(lord.getX(), lord.getY(), lord.getZ() - NamedMob.LORD_LEASH - 6);
			return String.format("scale %.2f", scale);
		});
		context.waitTicks(40);
		String home = server.computeOnServer(s -> {
			ServerLevel level = s.getLevel(dimension);
			NamedMob lord = (NamedMob)level.getEntity(lordId);
			require(lord.distanceToSqr(Vec3.atBottomCenterOf(chest)) < NamedMob.LORD_LEASH * NamedMob.LORD_LEASH, "the lord should be back home, at " + lord.position());
			require(SpaceCheck.fits(level, lord, lord.position()), "the lord came home into a wall at " + lord.position());
			List<? extends Mob> minions = Attacks.summon(level, (EntityType<? extends Mob>)lord.getType(), lord.position(), 6, null, 200);
			for (Mob minion : minions) {
				require(SpaceCheck.fits(level, minion, minion.position()), "a minion landed in a wall at " + minion.position());
				minion.discard();
			}
			return minions.size() + " minions";
		});
		context.waitTicks(20);
		MinecraftMode.LOGGER.info("[lair] treant lord in the corridors: {}, home, {} placed free", walked, home);
	}

	/**
	 * The lair map loop on a built lair, for a player far above its range: rewards and the lord follow the player's level, the first open of the
	 * cycle remembers the clear, hands out the next map and offers the way back, the clear screen shows once the chest closes (English and
	 * Korean screenshots), and "Return to the exit" takes the player to the safe spot by the gate.
	 */
	private static void checkLoop(final ClientGameTestContext context, final TestServerContext server, final ResourceKey<Level> dimension, final LairDef def,
		final BlockPos chest) {
		int high = 85;
		String report = server.computeOnServer(s -> {
			ServerLevel level = s.getLevel(dimension);
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.WARRIOR, 4).withProgress(high, 0));
			NamedDef named = def.named();
			// rewards: the player's bracket above the lair, never below the lair's own range
			LairLoot.Range range = LairLoot.range(named, high);
			require(range.lo() == 80 && range.hi() == 90, "a Lv " + high + " player should be paid in 80-90, got " + range);
			require(LairLoot.range(named, named.lo()).equals(new LairLoot.Range(named.lo(), named.hi())), "a player in range gets the lair's range");
			require(LairLoot.range(named, 1).equals(new LairLoot.Range(named.lo(), named.hi())), "a low player never gets less than the lair's range");
			require(LairLoot.range(named, 100).equals(new LairLoot.Range(90, 100)), "the cap pays the top bracket");
			JobData data = JobProgression.get(player);
			for (int seed = 0; seed < 20; seed++) {
				for (ItemStack stack : LairLoot.goal(named, data, RandomSource.create(seed))) {
					if (stack.getItem() instanceof EvolutionEtherItem) {
						require(EvolutionEtherItem.grade(stack) >= 80, "ether for a Lv " + high + " player should be graded 80+, got " + EvolutionEtherItem.grade(stack));
					}
				}
			}
			// the lord rises to the strongest player near the chest
			player.teleportTo(level, chest.getX() + 0.5, chest.getY(), chest.getZ() + 3.5, java.util.Set.of(), 180.0F, 0.0F, true);
			LairChestBlockEntity goal = (LairChestBlockEntity)level.getBlockEntity(chest);
			long cycle = ResetCycle.cycle(level);
			NamedMob lord = goal.wakeLord(level, cycle);
			require(lord != null && lord.powerLevel() == high && goal.lordPower() == high, "the lord should rise to Lv " + high + ", got "
				+ (lord == null ? "none" : lord.powerLevel()));
			double[] typical = NamedMobs.typicalPower(high);
			float wantedTough = (float)(typical[0] / (named.health() * NamedMobs.levelScale(named, named.hi())));
			require(MobPower.toughness(lord) >= wantedTough * 0.99F && MobPower.factor(lord) > 1.2F, "the raised lord should take damage divided by "
				+ wantedTough + " and hit harder, got " + MobPower.toughness(lord) + " / " + MobPower.factor(lord));
			float tough = MobPower.toughness(lord);
			lord.raiseTo(high);
			require(MobPower.toughness(lord) == tough, "raising twice to the same level changes nothing");
			require(LairExp.lordExp(player, lord) == JobProgression.levelExp(player, high, LairExp.LORD)
				&& LairExp.lordExp(player, lord) > JobProgression.levelExp(player, named.hi(), LairExp.LORD), "a raised lord pays a share of the player's own level");
			lord.setNoAi(true);
			// the strong player leaves: out of combat the lord falls back to its own level; back again, it rises again
			player.teleportTo(level, chest.getX() + 0.5, chest.getY(), chest.getZ() + 60.5, java.util.Set.of(), 180.0F, 0.0F, true);
			goal.refit(level, lord);
			require(lord.powerLevel() == 0 && goal.lordPower() == 0 && MobPower.factor(lord) == 1.0F && Math.abs(MobPower.toughness(lord)
				- tough / wantedTough) < 0.01F, "with nobody strong near, the lord should fall back to its own level, got " + lord.powerLevel() + ", toughness "
				+ MobPower.toughness(lord) + ", damage x" + MobPower.factor(lord));
			require(LairExp.lordExp(player, lord) == JobProgression.levelExp(player, named.hi(), LairExp.LORD), "an unraised lord pays at the lair's level");
			player.teleportTo(level, chest.getX() + 0.5, chest.getY(), chest.getZ() + 3.5, java.util.Set.of(), 180.0F, 0.0F, true);
			goal.refit(level, lord);
			require(lord.powerLevel() == high && MobPower.toughness(lord) == tough, "the lord rises again for the strong player, got " + lord.powerLevel());
			// in combat it keeps its power, even when the strong player steps away
			lord.hurtServer(level, player.damageSources().playerAttack(player), 1.0F);
			player.teleportTo(level, chest.getX() + 0.5, chest.getY(), chest.getZ() + 60.5, java.util.Set.of(), 180.0F, 0.0F, true);
			goal.refit(level, lord);
			require(lord.powerLevel() == high, "a lord in combat is not refitted");
			player.teleportTo(level, chest.getX() + 0.5, chest.getY(), chest.getZ() + 3.5, java.util.Set.of(), 180.0F, 0.0F, true);
			lord.hurtServer(level, player.damageSources().playerAttack(player), 1.0E9F);
			require(!lord.isAlive() && goal.victor(player, cycle), "the player should beat the raised lord");
			require(goal.beatenLevel(player, cycle) == high && goal.gradeLevel(player, cycle) == high, "the lord was beaten at Lv " + high + ", got "
				+ goal.beatenLevel(player, cycle));
			LairChestBlockEntity cache = cacheOf(level, chest, def);
			require(cache != null && cache.gradeLevel(player, cycle) == high, "a cache pays at the level its opener beat the lord at, got "
				+ (cache == null ? "no cache" : cache.gradeLevel(player, cycle)));
			// the first open of the cycle: graded by the player's level, remembered, the next map and the way back
			goal.forgetPersonal();
			player.getInventory().clearContent();
			int expBefore = JobProgression.get(player).exp();
			require(LairChestBlock.open(player, goal), "the treasure opens for the victor");
			Container contents = goal.container(player, cycle);
			boolean highEther = false;
			for (int i = 0; i < contents.getContainerSize(); i++) {
				ItemStack stack = contents.getItem(i);
				highEther |= stack.getItem() instanceof EvolutionEtherItem && EvolutionEtherItem.grade(stack) >= 80;
			}
			require(highEther, "the treasure should hold ether of the player's bracket");
			require(player.getInventory().countItem(ModItems.LAIR_MAP) == 1, "clearing a lair should hand out the next lair map");
			LairTravel.Target exit = LairTravel.exitOffer(player);
			require(exit != null && exit.entrance().equals(goal.exit(level)), "clearing should offer the way back to the gate");
			require(LairTravel.cleared(player).contains(LairTravel.key(dimension, chest)) && LairTravel.clearedThisCycle(player, exit),
				"the clear should be remembered for this cycle");
			require(JobProgression.get(player).exp() > expBefore || JobProgression.get(player).level() > high, "the treasure pays experience");
			// one step inside the maze for the way back
			player.teleportTo(level, chest.getX() + 0.5, chest.getY(), chest.getZ() + 0.5, java.util.Set.of(), 180.0F, 0.0F, true);
			return "lord toughness " + tough + ", damage x" + MobPower.factor(lord) + ", exit " + exit.entrance();
		});
		MinecraftMode.LOGGER.info("[lair] loop: {}", report);
		context.waitTicks(5);
		// the clear screen waits for the chest screen to close
		server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().closeContainer());
		context.waitForScreen(LairClearScreen.class);
		context.waitTicks(10);
		context.takeScreenshot("lair_clear_en");
		setLanguage(context, "ko_kr");
		context.waitTicks(10);
		context.takeScreenshot("lair_clear_ko");
		setLanguage(context, "en_us");
		// "Return to the exit"
		context.runOnClient(minecraft -> {
			ClientPlayNetworking.send(new LairTravelPayload(LairTravelPayload.EXIT));
			minecraft.gui.setScreen(null);
		});
		for (int i = 0; i < 40; i++) {
			context.waitTicks(5);
			boolean busy = server.computeOnServer(s -> LairTravel.travelling(s.getPlayerList().getPlayers().getFirst()));
			if (!busy) {
				break;
			}
		}
		String arrived = server.computeOnServer(s -> {
			ServerLevel level = s.getLevel(dimension);
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			LairChestBlockEntity goal = (LairChestBlockEntity)level.getBlockEntity(chest);
			BlockPos exit = goal.exit(level);
			BlockPos at = player.blockPosition();
			require(at.distManhattan(exit) <= 6 && LairTravel.safe(level, at), "the way back should end on a safe spot by the gate " + exit + ", got " + at);
			require(LairTravel.exitOffer(player) == null, "the way back is used once");
			require(!LairTravel.exit(player), "no second way back without a new clear");
			player.getInventory().clearContent();
			return at.toShortString();
		});
		MinecraftMode.LOGGER.info("[lair] back at the gate: {}", arrived);

		// next cycle: the strong player kills a lord that woke at its own level, so everything pays at the lair's level
		server.runCommand("time add " + ResetCycle.CYCLE_TICKS);
		context.waitTicks(5);
		String weak = server.computeOnServer(s -> {
			ServerLevel level = s.getLevel(dimension);
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			NamedDef named = def.named();
			LairChestBlockEntity goal = (LairChestBlockEntity)level.getBlockEntity(chest);
			LairChestBlockEntity cache = cacheOf(level, chest, def);
			long cycle = ResetCycle.cycle(level);
			require(goal.beatenLevel(player, cycle) == 0 && cache.gradeLevel(player, cycle) == named.hi(),
				"a new cycle forgets the beaten level; a cache without a beaten lord pays at the lair's level, got " + cache.gradeLevel(player, cycle));
			// discovery always pays at the lair's level
			player.setAttached(com.minecraftmode.registry.ModAttachments.LAIRS_FOUND, List.of());
			int before = classExp(player);
			LairExp.entered(player, def);
			int found = classExp(player) - before;
			require(paid(player, found, named.hi(), high, LairExp.DISCOVER), "discovery pays a share of the lair's level, got " + found);
			// the lord wakes with nobody strong near and stays at its own level
			player.teleportTo(level, chest.getX() + 0.5, chest.getY(), chest.getZ() + 60.5, java.util.Set.of(), 180.0F, 0.0F, true);
			NamedMob lord = goal.wakeLord(level, cycle);
			require(lord != null && lord.powerLevel() == 0, "a lord woken with nobody strong near stays at its own level");
			lord.setNoAi(true);
			player.teleportTo(level, chest.getX() + 0.5, chest.getY(), chest.getZ() + 3.5, java.util.Set.of(), 180.0F, 0.0F, true);
			require(LairExp.lordExp(player, lord) == JobProgression.levelExp(player, named.hi(), LairExp.LORD), "a weak lord pays at its own level");
			lord.hurtServer(level, player.damageSources().playerAttack(player), 1.0E9F);
			require(!lord.isAlive() && goal.beatenLevel(player, cycle) == named.hi() && goal.gradeLevel(player, cycle) == named.hi(),
				"the weak lord was beaten at the lair's level, got " + goal.beatenLevel(player, cycle));
			goal.forgetPersonal();
			before = classExp(player);
			require(LairChestBlock.open(player, goal), "the treasure opens again next cycle");
			int chestExp = classExp(player) - before;
			require(paid(player, chestExp, named.hi(), high, LairExp.CHEST), "the treasure pays a share of the lair's level, "
				+ JobProgression.levelExp(player, named.hi(), LairExp.CHEST) + ", got " + chestExp);
			Container contents = goal.container(player, cycle);
			for (int i = 0; i < contents.getContainerSize(); i++) {
				ItemStack stack = contents.getItem(i);
				require(!(stack.getItem() instanceof EvolutionEtherItem) || EvolutionEtherItem.grade(stack) <= named.hi(),
					"the treasure of a weak lord holds ether of the lair's range, got " + EvolutionEtherItem.grade(stack));
			}
			player.closeContainer();
			JobProgression.set(player, JobData.DEFAULT);
			player.getInventory().clearContent();
			return "discovery " + found + ", treasure " + chestExp + " exp at Lv " + high + " in a Lv " + named.lo() + "-" + named.hi() + " lair";
		});
		MinecraftMode.LOGGER.info("[lair] weak lord: {}", weak);
		// the clear screen of that clear pops up once the chest closed; put it away
		context.waitForScreen(LairClearScreen.class);
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
	}

	/**
	 * Whether {@code got} is {@code share} of a level measured at {@code content} (with the player's experience bonus), clearly below what the
	 * same share of {@code own} would pay.
	 */
	private static boolean paid(final ServerPlayer player, final int got, final int content, final int own, final float share) {
		float bonus = com.minecraftmode.job.weapon.JobWeapons.activeTotals(player).fraction(com.minecraftmode.job.engrave.EngraveStat.EXP_BONUS);
		int wanted = Math.round(JobProgression.levelExp(player, content, share) * (1.0F + bonus));
		return Math.abs(got - wanted) <= 1 && got < JobProgression.levelExp(player, own, share);
	}

	/** The total class experience of {@code player} (levels and progress) for exact before/after comparisons. */
	private static int classExp(final ServerPlayer player) {
		JobData data = JobProgression.get(player);
		int total = data.exp();
		for (int l = 1; l < data.level(); l++) {
			total += JobProgression.expToNext(l);
		}
		return total;
	}

	/** One of the lair's dead-end caches (they lie on the maze floor around the goal chest). */
	private static LairChestBlockEntity cacheOf(final ServerLevel level, final BlockPos chest, final LairDef def) {
		int size = def.size();
		for (int dx = -size; dx <= size; dx++) {
			for (int dz = -size; dz <= size; dz++) {
				BlockPos at = new BlockPos(chest.getX() + dx, chest.getY(), chest.getZ() + dz);
				if (level.getBlockState(at).is(ModBlocks.LAIR_CACHE) && level.getBlockEntity(at) instanceof LairChestBlockEntity cache) {
					return cache;
				}
			}
		}
		return null;
	}

	private static void setLanguage(final ClientGameTestContext context, final String code) {
		java.util.concurrent.atomic.AtomicReference<java.util.concurrent.CompletableFuture<Void>> reload = new java.util.concurrent.atomic.AtomicReference<>();
		context.runOnClient(minecraft -> {
			if (code.equals(minecraft.options.languageCode)) {
				reload.set(java.util.concurrent.CompletableFuture.completedFuture(null));
				return;
			}
			minecraft.options.languageCode = code;
			minecraft.getLanguageManager().setSelected(code);
			reload.set(minecraft.reloadResourcePacks());
		});
		context.waitFor(minecraft -> reload.get().isDone(), 20 * 120);
		// the loading overlay fades out after the reload; screenshots taken under it show only the logo
		context.waitFor(minecraft -> minecraft.gui.overlay() == null, 20 * 60);
	}

	private static boolean isCoin(final ItemStack stack) {
		return stack.is(ModItems.COPPER_COIN) || stack.is(ModItems.SILVER_COIN) || stack.is(ModItems.GOLD_COIN);
	}

	private static boolean isReward(final ItemStack stack) {
		return stack.getItem() instanceof ConsumableItem || stack.getItem() instanceof JobWeaponItem || stack.getItem() instanceof GearArmorItem;
	}

	private static void require(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
