package com.minecraftmode.test;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.entity.SpawnCandidates;
import com.minecraftmode.registry.ModBlocks;
import com.minecraftmode.registry.ModEntities;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.worldgen.lair.LairDef;
import com.minecraftmode.worldgen.lair.LairPiece;
import com.minecraftmode.worldgen.lair.NamedLairs;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Checks what the superflat test cannot: the capital at 0, 0 ({@link CityChecks}), then, away from
 * the city, real terrain ore generation, Mine Raider natural spawning and mythril armor in
 * survival. Uses a normal (default preset) world with a fixed seed.
 */
public class NormalWorldClientGameTest implements FabricClientGameTest {
	private static final String SEED = "minecraft_mode";
	private static final int SCAN_RADIUS_CHUNKS = 4;
	private static final int SOAK_TICKS = 1200;
	/** How far a mine raider may climb above its spawn limit (Y 40) during the soak. */
	private static final int RAIDER_CLIMB = 8;

	@Override
	public void runTest(final ClientGameTestContext context) {
		if (TestSelection.skip(this)) {
			return;
		}
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
			.setUseConsistentSettings(false)
			.adjustSettings(settings -> {
				settings.setSeed(SEED);
				settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			})
			.create()) {
			TestServerConnection connection = singleplayer.getConnection();
			TestServerContext server = singleplayer.getServer();
			connection.waitForChunksRender();

			// the capital at 0, 0 first, then natural terrain away from it
			CityChecks.run(context, server, connection);
			CityChecks.leave(context, server, connection);

			BlockPos exposedMythril = checkOreGeneration(server, connection);
			checkSpawnRules(context, server, connection);
			checkArmor(context, server, connection);
			List<BlockPos> raiders = soakNaturalSpawns(context, server, connection);
			checkNaturalLair(context, server, connection);
			checkUndergroundLair(context, server);
			screenshots(context, server, exposedMythril, raiders);
			WorldClose.prepare(context, server);
		}
	}

	// ------------------------------------------------------------ ore generation

	private static BlockPos checkOreGeneration(final TestServerContext server, final TestServerConnection connection) {
		Block[] tracked = {
			ModBlocks.MYTHRIL_ORE, ModBlocks.DEEPSLATE_MYTHRIL_ORE, ModBlocks.ALUMINUM_ORE, ModBlocks.DEEPSLATE_ALUMINUM_ORE,
			Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
		};
		// block -> {count, minY, maxY}
		Map<Block, int[]> stats = new LinkedHashMap<>();
		for (Block block : tracked) {
			stats.put(block, new int[] {0, Integer.MAX_VALUE, Integer.MIN_VALUE});
		}
		BlockPos[] exposed = new BlockPos[1];

		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = player.level();
			int centerX = player.blockPosition().getX() >> 4, centerZ = player.blockPosition().getZ() >> 4;
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int cx = centerX - SCAN_RADIUS_CHUNKS; cx <= centerX + SCAN_RADIUS_CHUNKS; cx++) {
				for (int cz = centerZ - SCAN_RADIUS_CHUNKS; cz <= centerZ + SCAN_RADIUS_CHUNKS; cz++) {
					LevelChunk chunk = level.getChunk(cx, cz);
					for (int y = level.getMinY(); y < 128; y++) {
						for (int x = 0; x < 16; x++) {
							for (int z = 0; z < 16; z++) {
								pos.set((cx << 4) + x, y, (cz << 4) + z);
								BlockState state = chunk.getBlockState(pos);
								int[] s2 = stats.get(state.getBlock());
								if (s2 == null) {
									continue;
								}
								s2[0]++;
								s2[1] = Math.min(s2[1], y);
								s2[2] = Math.max(s2[2], y);
								if (exposed[0] == null && (state.is(ModBlocks.MYTHRIL_ORE) || state.is(ModBlocks.DEEPSLATE_MYTHRIL_ORE))) {
									for (Direction direction : Direction.values()) {
										if (level.getBlockState(pos.relative(direction)).isAir()) {
											exposed[0] = pos.immutable();
										}
									}
								}
							}
						}
					}
				}
			}
		});

		int chunks = (SCAN_RADIUS_CHUNKS * 2 + 1) * (SCAN_RADIUS_CHUNKS * 2 + 1);
		for (Map.Entry<Block, int[]> entry : stats.entrySet()) {
			int[] s = entry.getValue();
			MinecraftMode.LOGGER.info(
				"[normal] {} : {} blocks in {} chunks ({} per chunk), Y {}..{}",
				BuiltInRegistries.BLOCK.getKey(entry.getKey()).getPath(), s[0], chunks, String.format("%.1f", s[0] / (double) chunks),
				s[0] == 0 ? "-" : s[1], s[0] == 0 ? "-" : s[2]
			);
		}
		int[] mythril = merge(stats.get(ModBlocks.MYTHRIL_ORE), stats.get(ModBlocks.DEEPSLATE_MYTHRIL_ORE));
		int[] aluminum = merge(stats.get(ModBlocks.ALUMINUM_ORE), stats.get(ModBlocks.DEEPSLATE_ALUMINUM_ORE));
		require(mythril[0] > 0, "no mythril ore generated in " + chunks + " chunks");
		require(aluminum[0] > 0, "no aluminum ore generated in " + chunks + " chunks");
		// Veins grow a few blocks around their origin, so allow a small margin around the placement range.
		require(mythril[1] >= -64 && mythril[2] <= 20, "mythril outside Y -64..16: " + mythril[1] + ".." + mythril[2]);
		require(aluminum[1] >= -20 && aluminum[2] <= 116, "aluminum outside Y -16..112: " + aluminum[1] + ".." + aluminum[2]);
		return exposed[0];
	}

	private static int[] merge(final int[] a, final int[] b) {
		return new int[] {a[0] + b[0], Math.min(a[1], b[1]), Math.max(a[2], b[2])};
	}

	// ------------------------------------------------------------ spawn rules

	private static void checkSpawnRules(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		// A sealed stone box high above ground: dark like a cave but above the Y=40 limit.
		BlockPos box = server.computeOnServer(s -> connection.getServerPlayer().blockPosition().atY(100));
		server.runCommand("fill " + (box.getX() - 2) + " 99 " + (box.getZ() - 2) + " " + (box.getX() + 2) + " 103 " + (box.getZ() + 2) + " minecraft:stone");
		server.runCommand("fill " + (box.getX() - 1) + " 100 " + (box.getZ() - 1) + " " + (box.getX() + 1) + " 102 " + (box.getZ() + 1) + " minecraft:air");
		context.waitTicks(40);

		String result = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = player.level();

			List<BlockPos> caves = findDarkCaveFloors(level, player.blockPosition(), 60);
			require(!caves.isEmpty(), "no dark cave floor found below Y=40 near spawn");

			// 26.3 reads natural spawns from the NATURAL_MOB_SPAWNS environment attribute, not the biome directly.
			boolean inSpawnList = level.environmentAttributes().getValue(EnvironmentAttributes.NATURAL_MOB_SPAWNS, caves.getFirst())
				.getMobsToSpawn(MobCategory.MONSTER).unwrap().stream()
				.anyMatch(entry -> entry.value().type() == ModEntities.MINE_RAIDER);
			require(inSpawnList, "mine_raider is not in the natural monster spawn list at " + caves.getFirst());
			// no creepers: removed from every biome's list, on the surface as in the caves
			for (BlockPos at : List.of(caves.getFirst(), level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, caves.getFirst()))) {
				boolean creeper = level.environmentAttributes().getValue(EnvironmentAttributes.NATURAL_MOB_SPAWNS, at)
					.getMobsToSpawn(MobCategory.MONSTER).unwrap().stream()
					.anyMatch(entry -> entry.value().type() == EntityTypes.CREEPER);
				require(!creeper, "creepers are still in the natural monster spawn list at " + at);
			}
			int raiderOk = 0, zombieOk = 0;
			for (BlockPos cave : caves) {
				if (SpawnPlacements.checkSpawnRules(ModEntities.MINE_RAIDER, level, EntitySpawnReason.NATURAL, cave, level.getRandom())) {
					raiderOk++;
				}
				if (SpawnPlacements.checkSpawnRules(EntityTypes.ZOMBIE, level, EntitySpawnReason.NATURAL, cave, level.getRandom())) {
					zombieOk++;
				}
			}
			require(raiderOk > 0, "mine_raider may not spawn on any of " + caves.size() + " dark cave floors");

			// Mythril golem: only below Y=0
			int golemDeepOk = 0, golemShallowOk = 0;
			for (BlockPos cave : caves) {
				boolean ok = SpawnPlacements.checkSpawnRules(ModEntities.MYTHRIL_GOLEM, level, EntitySpawnReason.NATURAL, cave, level.getRandom());
				if (cave.getY() < 0 && ok) {
					golemDeepOk++;
				} else if (cave.getY() >= 0 && ok) {
					golemShallowOk++;
				}
			}
			require(golemShallowOk == 0, "mythril_golem may spawn at Y >= 0 (" + golemShallowOk + " cave floors)");
			boolean deepCaveExists = caves.stream().anyMatch(cave -> cave.getY() < 0);
			require(!deepCaveExists || golemDeepOk > 0, "mythril_golem may not spawn on any deep cave floor");

			BlockPos high = box;
			int sky = level.getBrightness(LightLayer.SKY, high), block = level.getBrightness(LightLayer.BLOCK, high);
			boolean zombieHigh = SpawnPlacements.checkSpawnRules(EntityTypes.ZOMBIE, level, EntitySpawnReason.NATURAL, high, level.getRandom());
			boolean raiderHigh = SpawnPlacements.checkSpawnRules(ModEntities.MINE_RAIDER, level, EntitySpawnReason.NATURAL, high, level.getRandom());
			require(sky == 0 && block == 0, "test box at Y=100 is not dark (sky " + sky + ", block " + block + ")");
			require(zombieHigh, "control failed: a zombie should be able to spawn in the dark box at Y=100");
			require(!raiderHigh, "mine_raider may spawn at Y=100, but natural spawns must stay below Y=40");
			require(
				!SpawnPlacements.checkSpawnRules(ModEntities.MYTHRIL_GOLEM, level, EntitySpawnReason.NATURAL, high, level.getRandom()),
				"mythril_golem may spawn at Y=100"
			);

			// the spawn group's type is picked only from mobs that fit the spot, so cave-only monsters no longer waste surface picks
			var highList = SpawnCandidates.filter(level.environmentAttributes().getValue(EnvironmentAttributes.NATURAL_MOB_SPAWNS, high)
				.getMobsToSpawn(MobCategory.MONSTER), level, high).unwrap();
			require(highList.stream().anyMatch(entry -> entry.value().type() == EntityTypes.ZOMBIE), "zombies were filtered out at Y=100");
			require(highList.stream().noneMatch(entry -> entry.value().type() == ModEntities.MINE_RAIDER || entry.value().type() == ModEntities.MYTHRIL_GOLEM),
				"cave-only monsters are still spawn candidates at Y=100");
			require(SpawnCandidates.filter(level.environmentAttributes().getValue(EnvironmentAttributes.NATURAL_MOB_SPAWNS, caves.getFirst())
					.getMobsToSpawn(MobCategory.MONSTER), level, caves.getFirst()).unwrap().stream().anyMatch(entry -> entry.value().type() == ModEntities.MINE_RAIDER),
				"mine_raider was filtered out on a cave floor at " + caves.getFirst());
			return caves.size() + " cave floors: raider ok " + raiderOk + ", zombie ok " + zombieOk + ", golem ok " + golemDeepOk
				+ " (all below Y=0); dark box at Y=100: zombie ok, raider and golem rejected";
		});
		server.runCommand("fill " + (box.getX() - 2) + " 99 " + (box.getZ() - 2) + " " + (box.getX() + 2) + " 103 " + (box.getZ() + 2) + " minecraft:air");
		MinecraftMode.LOGGER.info("[normal] spawn rules: {}", result);
	}

	private static List<BlockPos> findDarkCaveFloors(final ServerLevel level, final BlockPos center, final int limit) {
		List<BlockPos> found = new ArrayList<>();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int radius = SCAN_RADIUS_CHUNKS * 16;
		for (int x = center.getX() - radius; x <= center.getX() + radius && found.size() < limit; x += 3) {
			for (int z = center.getZ() - radius; z <= center.getZ() + radius && found.size() < limit; z += 3) {
				for (int y = level.getMinY() + 6; y < 40; y++) {
					pos.set(x, y, z);
					if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
						&& !level.getBlockState(pos.below()).isAir() && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
						&& level.getBrightness(LightLayer.SKY, pos) == 0 && level.getBrightness(LightLayer.BLOCK, pos) == 0) {
						found.add(pos.immutable());
						break;
					}
				}
			}
		}
		return found;
	}

	// ------------------------------------------------------------ armor

	private static void checkArmor(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("gamemode survival @p");
		server.runCommand("item replace entity @p armor.head with minecraft_mode:mythril_helmet");
		server.runCommand("item replace entity @p armor.chest with minecraft_mode:mythril_chestplate");
		server.runCommand("item replace entity @p armor.legs with minecraft_mode:mythril_leggings");
		server.runCommand("item replace entity @p armor.feet with minecraft_mode:mythril_boots");
		server.runCommand("effect give @p minecraft:instant_health 1 10");
		context.waitTicks(5);

		float[] before = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			return new float[] {player.getArmorValue(), (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), player.getHealth()};
		});
		require(before[0] == 19.0F, "mythril armor set should give 19 armor, got " + before[0]);
		require(before[1] == 4.0F, "mythril armor set should give 4 toughness, got " + before[1]);

		server.runCommand("damage @p 10 minecraft:mob_attack");
		context.waitTicks(2);
		float[] after = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			return new float[] {player.getHealth(), player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue()};
		});
		float taken = before[2] - after[0];
		// 10 damage vs 19 armor / 4 toughness: 10 * (1 - max(19 / 5, 19 - 10 / 3) / 25) = 3.73
		require(taken > 3.5F && taken < 4.0F, "10 damage should be reduced to ~3.73, player took " + taken);
		require(after[1] > 0, "chestplate did not lose durability");
		server.runCommand("effect give @p minecraft:instant_health 1 10");
		server.runCommand("gamemode creative @p");
		MinecraftMode.LOGGER.info("[normal] armor 19 / toughness 4: 10 damage -> {} taken, chestplate durability -{}", taken, (int) after[1]);
	}

	// ------------------------------------------------------------ natural spawning

	private static List<BlockPos> soakNaturalSpawns(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("time set midnight");
		context.waitTicks(SOAK_TICKS);
		List<BlockPos> raiders = new ArrayList<>();
		String summary = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			Map<String, Integer> counts = new TreeMap<>();
			for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(160, 400, 160))) {
				if (mob.getType().getCategory() != MobCategory.MONSTER) {
					continue;
				}
				counts.merge(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath(), 1, Integer::sum);
				if (mob.getType() == ModEntities.MINE_RAIDER) {
					raiders.add(mob.blockPosition());
				}
			}
			return counts.toString();
		});
		MinecraftMode.LOGGER.info("[normal] monsters after {} ticks: {}", SOAK_TICKS, summary);
		MinecraftMode.LOGGER.info("[normal] mine raiders spawned naturally at: {}", raiders);
		// they spawn below Y=40 and then wander through the caves for the whole soak, so allow a short climb; a raider on the
		// surface would still fail
		for (BlockPos raider : raiders) {
			require(raider.getY() < 40 + RAIDER_CLIMB, "a naturally spawned mine raider is at Y=" + raider.getY() + " (spawn limit 40, climb " + RAIDER_CLIMB + ")");
		}
		boolean armed = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			return player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(160, 400, 160), m -> m.getType() == ModEntities.MINE_RAIDER)
				.stream().allMatch(m -> m.getMainHandItem().is(ModItems.MYTHRIL_PICKAXE) || m.getMainHandItem().is(net.minecraft.world.item.Items.IRON_PICKAXE));
		});
		require(armed, "a naturally spawned mine raider has no pickaxe");
		if (raiders.isEmpty()) {
			MinecraftMode.LOGGER.warn("[normal] no mine raider spawned during the soak; spawn rules were still verified above");
		}
		return raiders;
	}

	// ------------------------------------------------------------ named lairs

	/** The world places lairs by itself: the locator finds one, and standing in it counts as its grounds. */
	private static void checkNaturalLair(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		BlockPos found = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
			List<Holder<Structure>> lairs = NamedLairs.all().stream().<Holder<Structure>>map(d -> registry.getOrThrow(d.key())).toList();
			var nearest = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(lairs), new BlockPos(600, 80, 300), 64, false);
			return nearest == null ? null : nearest.getFirst();
		});
		require(found != null, "no named lair within 64 chunks of 600, 300");
		BlockPos center = found.offset(8, 0, 8);
		server.runCommand("gamemode spectator @p");
		server.runCommand("tp @p " + center.getX() + " 220 " + (center.getZ() + 70));
		context.waitTicks(20);
		connection.waitForChunksRender();
		String lair = null;
		for (int i = 0; i < 30 && lair == null; i++) {
			lair = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				if (!level.hasChunk(center.getX() >> 4, center.getZ() >> 4)) {
					return null;
				}
				int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, center.getX(), center.getZ());
				LairDef def = NamedLairs.at(level, new BlockPos(center.getX(), y, center.getZ()));
				return def == null ? null : def.id() + "@" + y;
			});
			if (lair == null) {
				context.waitTicks(20);
			}
		}
		require(lair != null, "the lair grounds at " + center + " were not recognised");
		int ground = Integer.parseInt(lair.substring(lair.indexOf('@') + 1));
		server.runCommand("time set noon");
		context.runOnClient(minecraft -> minecraft.options.renderDistance().set(10));
		server.runOnServer(s -> s.getPlayerList().setViewDistance(10));
		server.runCommand("tp @p " + (center.getX() + 55) + " " + (ground + 40) + " " + (center.getZ() + 75));
		context.waitTicks(200);
		server.runCommand("tp @p " + (center.getX() + 56) + " " + (ground + 40) + " " + (center.getZ() + 75));
		context.waitTicks(10);
		context.getInput().lookAt(new BlockPos(center.getX(), ground + 8, center.getZ()));
		context.waitTicks(60);
		context.takeScreenshot("lair_natural");
		context.runOnClient(minecraft -> minecraft.options.renderDistance().set(5));
		server.runOnServer(s -> s.getPlayerList().setViewDistance(5));
		server.runCommand("gamemode creative @p");
		MinecraftMode.LOGGER.info("[normal] natural lair {} near {}", lair, center);
	}

	/** An underground lair: the maze is sealed far below, a shaft with a spiral stair leads up into a beacon tower. */
	private static void checkUndergroundLair(final ClientGameTestContext context, final TestServerContext server) {
		LairDef def = NamedLairs.def("cave_troll");
		// lairs are centered on the middle of their start chunk
		int x = (-700 >> 4) * 16 + 8;
		int z = (600 >> 4) * 16 + 8;
		int reach = def.size() / 2 + LairPiece.APRON + LairPiece.GROUNDS + 8;
		server.runCommand("forceload add " + (x - reach) + " " + (z - reach) + " " + (x + reach) + " " + (z + reach));
		for (int i = 0; i < 120; i++) {
			boolean loaded = server.computeOnServer(s -> {
				for (int cx = (x - reach) >> 4; cx <= (x + reach) >> 4; cx++) {
					for (int cz = (z - reach) >> 4; cz <= (z + reach) >> 4; cz++) {
						if (!s.overworld().hasChunk(cx, cz)) {
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
		server.runCommand("place structure minecraft_mode:" + def.structureId() + " " + x + " 0 " + z);
		context.waitTicks(5);
		String report = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			int half = def.size() / 2;
			int floor = def.depth();
			int chests = 0;
			for (int bx = x - half; bx <= x + half; bx++) {
				for (int bz = z - half; bz <= z + half; bz++) {
					if (level.getBlockState(new BlockPos(bx, floor + 1, bz)).is(ModBlocks.LAIR_CHEST)) {
						chests++;
					}
				}
			}
			require(chests == 1, "the underground lair should have one goal chest at Y " + (floor + 1) + ", found " + chests);
			int sz = LairPiece.shaftZ(def, z);
			int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, sz) - 1;
			require(level.getBlockState(new BlockPos(x, top, sz)).is(def.palette().light()), "the tower should end in a beacon light, got "
				+ level.getBlockState(new BlockPos(x, top, sz)));
			int stairs = 0;
			for (int by = floor + 1; by < top - 24; by++) {
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (level.getBlockState(new BlockPos(x + dx, by, sz + dz)).getBlock() == def.palette().stairs()) {
							stairs++;
						}
					}
				}
			}
			require(stairs >= (top - 24 - floor) * 3 / 4, "the shaft should have a stair on almost every level, got " + stairs);
			return "floor " + floor + ", tower top " + top + ", " + stairs + " stairs";
		});
		server.runCommand("forceload remove all");
		MinecraftMode.LOGGER.info("[normal] underground lair: {}", report);
	}

	// ------------------------------------------------------------ screenshots

	private static void screenshots(final ClientGameTestContext context, final TestServerContext server, final BlockPos exposedMythril, final List<BlockPos> raiders) {
		// Keep command echoes out of the screenshots
		server.runCommand("gamerule send_command_feedback false");
		server.runCommand("gamemode spectator @p");
		server.runCommand("effect give @p minecraft:night_vision 600 0 true");
		if (exposedMythril != null) {
			view(context, server, exposedMythril);
			context.takeScreenshot("normal_mythril_ore");
		}
		if (!raiders.isEmpty()) {
			view(context, server, raiders.getFirst().above());
			context.takeScreenshot("normal_mine_raider");
		}
	}

	/** Places the spectator camera a few blocks from the target and looks at it. */
	private static void view(final ClientGameTestContext context, final TestServerContext server, final BlockPos target) {
		BlockPos camera = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				BlockPos candidate = target.relative(direction, 3);
				if (level.getBlockState(candidate).isAir() && level.getBlockState(candidate.relative(direction.getOpposite())).isAir()) {
					return candidate;
				}
			}
			return target.offset(3, 1, 3);
		});
		server.runCommand("tp @p " + (camera.getX() + 0.5) + " " + (camera.getY() - 1.0) + " " + (camera.getZ() + 0.5));
		context.waitTicks(10);
		context.getInput().lookAt(target);
		context.waitTicks(40);
	}

	private static void require(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
