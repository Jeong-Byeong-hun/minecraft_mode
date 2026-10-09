package com.minecraftmode.test;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.consumable.ConsumableItem;
import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMob;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.job.gear.GearArmorItem;
import com.minecraftmode.job.weapon.JobWeaponItem;
import com.minecraftmode.loot.EvolutionEtherItem;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.worldgen.lair.LairDef;
import com.minecraftmode.worldgen.lair.LairLoot;
import com.minecraftmode.worldgen.lair.LairPiece;
import com.minecraftmode.worldgen.lair.Maze;
import com.minecraftmode.worldgen.lair.NamedLairs;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.AABB;

/**
 * Named monster lairs: the 22 definitions and their spawn lists, the goal loot rules, mazes that
 * always reach their goal, and on a flat world one lair of every landmark shape (plus a Nether lair)
 * built with /place: walls, gate, a tall landmark, the goal chest with coins and a reward, and the
 * guardian. Screenshots of every shape.
 */
public class LairClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(final ClientGameTestContext context) {
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
				List<ItemStack> loot = LairLoot.goal(named, RandomSource.create(seed * 7919L + def.id().hashCode()));
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
						if (state.is(Blocks.CHEST)) {
							require(chest == null, id + ": more than one goal chest");
							chest = new BlockPos(bx, by, bz);
						} else if (state.is(Blocks.BARREL)) {
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
			// loot
			int coins = 0;
			int rewards = 0;
			if (level.getBlockEntity(chest) instanceof Container container) {
				for (int slot = 0; slot < container.getContainerSize(); slot++) {
					ItemStack stack = container.getItem(slot);
					coins += isCoin(stack) ? 1 : 0;
					rewards += isReward(stack) ? 1 : 0;
				}
			}
			require(coins > 0, id + ": the goal chest has no coins");
			require(rewards > 0, id + ": the goal chest has no reward item");
			// guardian
			List<NamedMob> guards = level.getEntitiesOfClass(NamedMob.class, new AABB(chest).inflate(4),
				m -> NamedMobs.def(m.getType()) == def.named() && m.isPersistenceRequired());
			require(!guards.isEmpty(), id + ": no guardian by the goal");
			guards.forEach(m -> m.setNoAi(true));
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
		server.runCommand(in + "forceload remove all");
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
