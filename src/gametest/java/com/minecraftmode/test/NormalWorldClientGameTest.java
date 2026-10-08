package com.minecraftmode.test;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.registry.ModBlocks;
import com.minecraftmode.registry.ModEntities;
import com.minecraftmode.registry.ModItems;
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
import net.minecraft.core.registries.BuiltInRegistries;
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

/**
 * Checks what the superflat test cannot: the capital at 0, 0 ({@link CityChecks}), then, away from
 * the city, real terrain ore generation, Mine Raider natural spawning and mythril armor in
 * survival. Uses a normal (default preset) world with a fixed seed.
 */
public class NormalWorldClientGameTest implements FabricClientGameTest {
	private static final String SEED = "minecraft_mode";
	private static final int SCAN_RADIUS_CHUNKS = 4;
	private static final int SOAK_TICKS = 1200;

	@Override
	public void runTest(final ClientGameTestContext context) {
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
			screenshots(context, server, exposedMythril, raiders);
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
		for (BlockPos raider : raiders) {
			require(raider.getY() < 40, "a naturally spawned mine raider is at Y=" + raider.getY() + " (limit is 40)");
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
