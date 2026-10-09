package com.minecraftmode.test;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.city.CityServices;
import com.minecraftmode.city.CityZone;
import com.minecraftmode.client.job.TrainerScreen;
import com.minecraftmode.entity.CityNpc;
import com.minecraftmode.entity.ClassTrainer;
import com.minecraftmode.event.WorldEvents;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.network.OpenTrainerPayload;
import com.minecraftmode.registry.ModBlocks;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The capital in a normal world: spawn on the plaza, landmark blocks, every trainer at their
 * posts, the safe-zone rules (no building for non-ops, no PvP, no explosion damage, no hostile
 * spawns), and screenshots of the districts and trainers.
 */
final class CityChecks {
	private static final int SAFE_SOAK_TICKS = 400;

	static void run(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		int base = checkLayout(server, connection);
		checkEnchanterHall(server, base);
		checkTrainers(context, server, base);
		map(server, base);
		checkProtection(context, server, connection, base);
		checkNoHostiles(context, server, base);
		screenshots(context, server, connection, base);
		checkInvasion(context, server, base);
	}

	/**
	 * An invasion: waves come in through the gates tagged as invaders, the city guards leave them alone, and stopping the event
	 * removes them.
	 */
	private static void checkInvasion(final ClientGameTestContext context, final TestServerContext server, final int base) {
		server.runCommand("time set midnight");
		require(server.computeOnServer(s -> WorldEvents.startInvasion(s.overworld())), "the invasion should start in the capital");
		context.waitTicks(10);
		int first = server.computeOnServer(s -> WorldEvents.invaders().size());
		require(first > 0 && WorldEvents.invasionWave() == 1, "wave 1 should bring invaders, got " + first);
		server.runCommand("tp @p " + (CityZone.WALL - 14) + ".5 " + (base + 6) + " 0.5");
		context.waitTicks(10);
		context.getInput().lookAt(new BlockPos(CityZone.WALL - 4, base + 1, 0));
		context.waitTicks(80);
		String report = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			CityServices.driveOffHostiles(level);
			int alive = 0;
			int inside = 0;
			for (java.util.UUID id : WorldEvents.invaders()) {
				if (level.getEntity(id) instanceof net.minecraft.world.entity.Mob mob && mob.isAlive()) {
					alive++;
					require(mob.entityTags().contains(WorldEvents.INVADER_TAG), "invaders carry the invader tag");
					inside += CityZone.inside(mob.blockPosition()) ? 1 : 0;
				}
			}
			require(alive > 0 && inside > 0, "the guards must not remove invaders (alive " + alive + ", inside the walls " + inside + ")");
			return alive + " invaders, " + inside + " inside the walls";
		});
		context.takeScreenshot("city_invasion");
		server.runOnServer(s -> WorldEvents.stopInvasion(s.overworld()));
		context.waitTicks(5);
		require(server.computeOnServer(s -> !WorldEvents.invasionRunning() && s.overworld().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
			new AABB(-CityZone.WALL, base - 10, -CityZone.WALL, CityZone.WALL, base + 40, CityZone.WALL), m -> m.entityTags().contains(WorldEvents.INVADER_TAG)).isEmpty()),
			"stopping the invasion removes the invaders");
		server.runCommand("time set noon");
		MinecraftMode.LOGGER.info("[city] invasion: {}", report);
	}

	/** Leaves the city for natural land (surface above sea level) so the terrain checks see untouched chunks. */
	static void leave(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		String where = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			int sea = level.getSeaLevel();
			int[][] candidates = {{600, 600}, {-600, 600}, {600, -600}, {-600, -600}, {900, 0}, {0, 900}, {-900, 0}};
			for (int[] c : candidates) {
				int ground = level.getChunkSource().getGenerator().getBaseHeight(c[0], c[1], Heightmap.Types.OCEAN_FLOOR_WG, level, level.getChunkSource().randomState());
				if (ground > sea + 2 || c == candidates[candidates.length - 1]) {
					level.getChunk(c[0] >> 4, c[1] >> 4);
					int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c[0], c[1]);
					connection.getServerPlayer().teleportTo(c[0] + 0.5, y + 1, c[1] + 0.5);
					return c[0] + ", " + y + ", " + c[1];
				}
			}
			return "?";
		});
		server.runCommand("forceload remove all");
		// fresh terrain takes longer than waitForChunksRender allows; the checks that follow load chunks on the server anyway
		context.waitTicks(200);
		MinecraftMode.LOGGER.info("[city] left the city for natural terrain at {}", where);
	}

	// ------------------------------------------------------------ layout

	private static int checkLayout(final TestServerContext server, final TestServerConnection connection) {
		return server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = player.level();
			require(CityZone.isCityLevel(level), "a normal overworld should host the city");
			int base = CityZone.baseY(level);
			BlockPos spawn = CityZone.spawn(base);
			require(level.getRespawnData().pos().equals(spawn), "world spawn should be the plaza " + spawn + ", got " + level.getRespawnData().pos());
			require(player.blockPosition().distManhattan(spawn) <= 2, "first join should put the player on the plaza, got " + player.blockPosition());
			require(player.getInventory().contains(stack -> stack.is(Items.WRITTEN_BOOK)), "first join should give the guide book");
			require(level.getBlockState(spawn.below()).is(Blocks.CHISELED_STONE_BRICKS), "spawn mark missing under " + spawn);
			require(level.getBlockState(new BlockPos(0, base + 5, 0)).is(Blocks.GOLD_BLOCK), "fountain crown missing");
			require(level.getBlockState(new BlockPos(30, base + 5, -99)).is(Blocks.STONE_BRICKS), "north wall missing");
			require(level.getBlockState(new BlockPos(-99, base + 5, 30)).is(Blocks.STONE_BRICKS), "west wall missing");
			require(level.getBlockState(new BlockPos(99, base + 3, 0)).isAir(), "east gate should be open");
			require(level.getBlockState(new BlockPos(0, base + 3, 99)).isAir(), "south gate should be open");
			require(level.getBlockState(new BlockPos(-90, base + 2, 50)).is(Blocks.SPRUCE_LOG), "the Urahara Shop is missing");
			require(level.getBlockState(new BlockPos(85, base + 10, -58)).is(Blocks.CONCRETE.pick(DyeColor.ORANGE)), "the Hunter Association is missing");
			// the walk from the spawn to the fountain (between the planters) is clear: nothing natural poking through
			int blocked = 0;
			for (int x = -8; x <= 8; x++) {
				for (int z = 6; z <= 18; z++) {
					if (Math.hypot(x, z) < 17.0 && !level.getBlockState(new BlockPos(x, base + 2, z)).isAir()) {
						blocked++;
					}
				}
			}
			require(blocked == 0, blocked + " blocks stick out of the plaza");
			MinecraftMode.LOGGER.info("[city] base Y {}, spawn {}, fountain, walls and gates in place", base, spawn);
			return base;
		});
	}

	/** Every hall table has the full 15 bookshelves, the anvils are there, and a worn anvil is put back as new. */
	private static void checkEnchanterHall(final TestServerContext server, final int base) {
		String report = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			StringBuilder out = new StringBuilder();
			for (BlockPos table : CityZone.enchantingTables(base)) {
				if (!level.getBlockState(table).is(Blocks.ENCHANTING_TABLE)) {
					return "no enchanting table at " + table;
				}
				long shelves = net.minecraft.world.level.block.EnchantingTableBlock.BOOKSHELF_OFFSETS.stream()
					.filter(offset -> net.minecraft.world.level.block.EnchantingTableBlock.isValidBookShelf(level, table, offset)).count();
				if (shelves < 15) {
					return "the table at " + table + " has only " + shelves + " working bookshelves (15 give level 30)";
				}
				out.append(shelves).append(" ");
			}
			for (BlockPos anvil : CityZone.anvils(base)) {
				if (!level.getBlockState(anvil).is(Blocks.ANVIL)) {
					return "no anvil at " + anvil;
				}
			}
			BlockPos worn = CityZone.anvils(base).get(1);
			level.setBlockAndUpdate(worn, Blocks.DAMAGED_ANVIL.defaultBlockState());
			CityServices.keepAnvils(level);
			if (!level.getBlockState(worn).is(Blocks.ANVIL)) {
				return "a damaged city anvil should be replaced";
			}
			return "shelves per table: " + out.toString().trim();
		});
		require(report.startsWith("shelves"), report);
		MinecraftMode.LOGGER.info("[city] enchanter's hall: 4 tables at level 30 ({}), anvils kept in repair", report);
	}

	// ------------------------------------------------------------ trainers

	private static void checkTrainers(final ClientGameTestContext context, final TestServerContext server, final int base) {
		server.runCommand("forceload add -112 -112 112 112");
		for (int i = 0; i < 120; i++) {
			boolean ready = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				for (JobClass job : JobClass.PLAYABLE) {
					BlockPos home = CityZone.trainerHome(job, base);
					if (!level.isLoaded(home) || !level.shouldTickBlocksAt(home)) {
						return false;
					}
				}
				return true;
			});
			if (ready) {
				break;
			}
			context.waitTicks(10);
		}
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			CityServices.keepTrainers(level);
			CityServices.keepNpcs(level);
			// a second pass must not duplicate anyone
			CityServices.keepTrainers(level);
			CityServices.keepNpcs(level);
		});
		context.waitTicks(5);
		String report = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			List<String> lines = new ArrayList<>();
			for (JobClass job : JobClass.PLAYABLE) {
				BlockPos home = CityZone.trainerHome(job, base);
				List<ClassTrainer> found = level.getEntitiesOfClass(ClassTrainer.class, new AABB(home).inflate(32), t -> t.job() == job);
				require(found.size() == 1, job.id() + " should have exactly one trainer, found " + found.size());
				ClassTrainer trainer = found.getFirst();
				BlockPos at = trainer.blockPosition();
				require(at.distManhattan(home) <= 2, job.id() + " trainer is at " + at + ", home is " + home);
				require(!level.getBlockState(at.below()).isAir(), job.id() + " trainer stands on air at " + at);
				require(!level.getBlockState(at).isSuffocating(level, at) && !level.getBlockState(at.above()).isSuffocating(level, at.above()),
					job.id() + " trainer is stuck in a block at " + at);
				require(trainer.isInvulnerable(), job.id() + " trainer should be invulnerable");
				lines.add(job.id() + "@" + at.toShortString());
			}
			// the service NPCs (smith, marshal, guild clerk, broker, enhancer) stand at their posts too
			for (CityNpc.Role role : CityNpc.Role.values()) {
				BlockPos home = CityZone.npcHome(role, base);
				List<CityNpc> npcs = level.getEntitiesOfClass(CityNpc.class, new AABB(home).inflate(32), n -> n.role() == role);
				require(npcs.size() == 1, role.id() + " should have exactly one NPC, found " + npcs.size());
				BlockPos at = npcs.getFirst().blockPosition();
				require(at.distManhattan(home) <= 2, role.id() + " is at " + at + ", home is " + home);
				require(!level.getBlockState(at.below()).isAir(), role.id() + " stands on air at " + at);
				require(!level.getBlockState(at).isSuffocating(level, at) && !level.getBlockState(at.above()).isSuffocating(level, at.above()),
					role.id() + " is stuck in a block at " + at);
				lines.add(role.id() + "@" + at.toShortString());
			}
			// the profession stations in the market and the forge
			require(level.getBlockState(new BlockPos(-23, base, 69)).is(ModBlocks.KITCHEN_STATION), "the grocer's stall should have the kitchen station");
			require(level.getBlockState(new BlockPos(-13, base, 59)).is(ModBlocks.ALCHEMY_STATION), "the alchemist's stall should have the alchemy station");
			require(level.getBlockState(new BlockPos(-25, base, 91)).is(ModBlocks.SMITHING_STATION), "the forge should have the smithing station");
			// a trainer that wandered off is brought back
			ClassTrainer mage = level.getEntitiesOfClass(ClassTrainer.class, new AABB(CityZone.trainerHome(JobClass.MAGE, base)).inflate(32), t -> t.job() == JobClass.MAGE).getFirst();
			mage.teleportTo(mage.getX() + 6, mage.getY(), mage.getZ());
			CityServices.keepTrainers(level);
			require(mage.blockPosition().distManhattan(CityZone.trainerHome(JobClass.MAGE, base)) <= 2, "keepTrainers should bring the mage trainer home");
			return String.join(", ", lines);
		});
		MinecraftMode.LOGGER.info("[city] trainers at their posts: {}", report);
	}

	// ------------------------------------------------------------ top-down map

	/**
	 * Writes screenshots/city_map.png (top-down, 3 px per block, map colours shaded by height) and
	 * fails if natural terrain is left standing on the city floor: every column inside the walls
	 * must be built or flattened, never an untouched hill.
	 */
	private static void map(final TestServerContext server, final int base) {
		int r = CityZone.CORE + 8;
		int scale = 3;
		String report = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			java.awt.image.BufferedImage img = new java.awt.image.BufferedImage((2 * r + 1) * scale, (2 * r + 1) * scale, java.awt.image.BufferedImage.TYPE_INT_RGB);
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			List<String> hills = new ArrayList<>();
			int hillColumns = 0;
			for (int x = -r; x <= r; x++) {
				for (int z = -r; z <= r; z++) {
					int top = base + 60;
					BlockState state = Blocks.AIR.defaultBlockState();
					for (; top > base - 20; top--) {
						state = level.getBlockState(pos.set(x, top, z));
						if (!state.isAir()) {
							break;
						}
					}
					int rgb = state.getMapColor(level, pos.set(x, top, z)).col;
					double shade = Math.max(0.55, Math.min(1.25, 1.0 + (top - base + 1) * 0.03));
					rgb = (int)Math.min(255, ((rgb >> 16) & 0xFF) * shade) << 16 | (int)Math.min(255, ((rgb >> 8) & 0xFF) * shade) << 8 | (int)Math.min(255, (rgb & 0xFF) * shade);
					// natural terrain at or above the floor, inside the walls
					boolean natural = false;
					if (CityZone.inside(x, z)) {
						for (int y = base; y <= base + 12 && !natural; y++) {
							BlockState at = level.getBlockState(pos.set(x, y, z));
							natural = at.is(Blocks.DIRT) || at.is(Blocks.STONE) || at.is(Blocks.GRAVEL) || at.is(Blocks.SAND) || at.is(Blocks.GRANITE)
								|| at.is(Blocks.DIORITE) || at.is(Blocks.ANDESITE) || at.is(Blocks.TUFF) || at.is(Blocks.CLAY) || at.is(Blocks.SNOW_BLOCK)
								|| at.is(Blocks.GRASS_BLOCK) && !planter(x, z);
						}
					}
					if (natural) {
						hillColumns++;
						rgb = 0xFF2020;
						if (hills.size() < 12) {
							hills.add(x + "," + z);
						}
					}
					for (int dx = 0; dx < scale; dx++) {
						for (int dz = 0; dz < scale; dz++) {
							img.setRGB((x + r) * scale + dx, (z + r) * scale + dz, rgb);
						}
					}
				}
			}
			try {
				java.nio.file.Path out = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("screenshots/city_map.png");
				java.nio.file.Files.createDirectories(out.getParent());
				javax.imageio.ImageIO.write(img, "png", out.toFile());
			} catch (java.io.IOException e) {
				throw new AssertionError("could not write city_map.png", e);
			}
			require(hillColumns == 0, hillColumns + " columns inside the walls still have natural terrain on the city floor, e.g. " + hills);
			return "city_map.png written, no natural terrain left on the floor";
		});
		MinecraftMode.LOGGER.info("[city] {}", report);
	}

	/** The four tree planters on the plaza diagonals are the only grass on the city floor. */
	private static boolean planter(final int x, final int z) {
		return Math.abs(x) >= 11 && Math.abs(x) <= 13 && Math.abs(z) >= 11 && Math.abs(z) <= 13;
	}

	// ------------------------------------------------------------ safe zone

	private static void checkProtection(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection, final int base) {
		server.runCommand("gamemode survival @p");
		context.waitTicks(2);
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = player.level();
			FakePlayer stranger = FakePlayer.get(level);
			stranger.setGameMode(GameType.SURVIVAL);
			BlockPos floor = new BlockPos(3, base - 1, 13);
			BlockPos above = floor.above();
			stranger.snapTo(3.5, base, 15.5, 180.0F, 30.0F);

			require(CityServices.blocksBuilding(stranger, floor), "a non-op survival player should not build in the city");
			require(!CityServices.blocksBuilding(stranger, new BlockPos(600, 80, 600)), "building outside the city should be allowed");
			require(!CityServices.blocksBuilding(player, floor), "operators may build in the city");

			// breaking goes through Player.blockActionRestricted
			BlockState before = level.getBlockState(floor);
			stranger.gameMode.destroyBlock(floor);
			require(level.getBlockState(floor) == before, "a stranger broke the plaza floor");
			// placing goes through BlockItem.place
			ItemStack stone = new ItemStack(Items.STONE);
			stranger.setItemInHand(InteractionHand.MAIN_HAND, stone);
			stone.useOn(new UseOnContext(stranger, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(floor), Direction.UP, floor, false)));
			require(level.getBlockState(above).isAir(), "a stranger placed a block on the plaza");
			require(!stranger.mayUseItemAt(floor, Direction.UP, stone), "mayUseItemAt should refuse inside the city");

			// no PvP
			float health = player.getHealth();
			require(!player.isInvulnerable() && !player.isCreative(), "the PvP check needs a vulnerable survival player");
			boolean hurt = player.hurtServer(level, stranger.damageSources().playerAttack(stranger), 4.0F);
			require(!hurt && player.getHealth() == health, "players should not hurt each other in the city");

			// explosions do not touch the city
			BlockPos blast = new BlockPos(30, base + 4, -96);
			Map<BlockPos, BlockState> around = new HashMap<>();
			for (BlockPos pos : BlockPos.betweenClosed(blast.offset(-4, -4, -4), blast.offset(4, 4, 4))) {
				around.put(pos.immutable(), level.getBlockState(pos));
			}
			level.explode(null, blast.getX() + 0.5, blast.getY() + 0.5, blast.getZ() + 0.5, 4.0F, Level.ExplosionInteraction.TNT);
			int changed = 0;
			for (Map.Entry<BlockPos, BlockState> entry : around.entrySet()) {
				if (level.getBlockState(entry.getKey()) != entry.getValue()) {
					changed++;
				}
			}
			require(changed == 0, "an explosion changed " + changed + " city blocks");
			return "break/place refused for strangers, ops allowed, no PvP damage, TNT-sized blast changed 0 of " + around.size() + " blocks";
		});
		server.runCommand("gamemode creative @p");
		MinecraftMode.LOGGER.info("[city] safe zone: {}", report);
	}

	/**
	 * Hostile mobs that spawn while recording (ENTITY_LOAD of a monster with tickCount 0). The city
	 * guards remove intruders every second, so counting mobs afterwards would hide a broken spawn
	 * rule; this records the spawns themselves.
	 */
	private static final List<String> FRESH_INSIDE = new CopyOnWriteArrayList<>();
	private static final AtomicInteger FRESH_OUTSIDE = new AtomicInteger();
	private static volatile boolean recording;

	static {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (!recording || entity.tickCount != 0 || entity.getType().getCategory() != MobCategory.MONSTER || level.dimension() != Level.OVERWORLD) {
				return;
			}
			if (CityZone.inside(entity.blockPosition())) {
				FRESH_INSIDE.add(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath() + "@" + entity.blockPosition().toShortString());
			} else {
				FRESH_OUTSIDE.incrementAndGet();
			}
		});
	}

	private static void checkNoHostiles(final ClientGameTestContext context, final TestServerContext server, final int base) {
		server.runCommand("time set midnight");
		FRESH_INSIDE.clear();
		FRESH_OUTSIDE.set(0);
		recording = true;
		context.waitTicks(SAFE_SOAK_TICKS);
		recording = false;
		require(FRESH_INSIDE.isEmpty(), "hostile mobs spawned inside the city: " + FRESH_INSIDE);

		// the guards: an intruder on the plaza is gone within a second, a NoAI decoration stays
		int[] ids = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			Mob intruder = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			Mob statue = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			require(intruder != null && statue != null, "could not create zombies");
			intruder.snapTo(4.5, base, 9.5, 0.0F, 0.0F);
			statue.snapTo(-4.5, base, 9.5, 0.0F, 0.0F);
			statue.setNoAi(true);
			level.addFreshEntity(intruder);
			level.addFreshEntity(statue);
			return new int[] {intruder.getId(), statue.getId()};
		});
		context.waitTicks(25);
		String report = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			require(level.getEntity(ids[0]) == null, "the city guards should drive off a zombie on the plaza");
			require(level.getEntity(ids[1]) != null, "NoAI decorations should stay");
			level.getEntity(ids[1]).discard();
			int inside = level.getEntitiesOfClass(Mob.class, new AABB(-CityZone.WALL, level.getMinY(), -CityZone.WALL, CityZone.WALL + 1, level.getMaxY(), CityZone.WALL + 1),
				m -> m.getType().getCategory() == MobCategory.MONSTER && CityZone.inside(m.blockPosition())).size();
			require(inside == 0, inside + " hostile mobs are still inside the walls");
			return "0 spawned inside (" + FRESH_OUTSIDE.get() + " outside), intruder driven off, none left inside";
		});
		server.runCommand("time set noon");
		MinecraftMode.LOGGER.info("[city] hostile mobs during a {} tick night: {}", SAFE_SOAK_TICKS, report);
	}

	// ------------------------------------------------------------ screenshots

	private static void screenshots(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection, final int base) {
		server.runCommand("gamerule send_command_feedback false");
		server.runCommand("gamerule log_admin_commands false");
		server.runCommand("gamemode spectator @p");
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		// the whole city (±112) from the aerial camera: the test harness runs at render distance 5,
		// so raise both the client and the server side and wait until everything around is drawn
		context.runOnClient(minecraft -> minecraft.options.renderDistance().set(12));
		server.runOnServer(s -> s.getPlayerList().setViewDistance(12));
		server.runCommand("tp @p 0.5 " + (base + 70) + " 95.5");
		context.waitTicks(20);
		context.getInput().lookAt(new BlockPos(0, base, 15));
		context.waitTicks(400);
		int[] seen = context.computeOnClient(minecraft -> new int[] {minecraft.options.getEffectiveRenderDistance(), minecraft.level.getChunkSource().getLoadedChunksCount()});
		context.takeScreenshot("city_aerial");
		MinecraftMode.LOGGER.info("[city] aerial at render distance {} with {} client chunks", seen[0], seen[1]);
		view(context, server, "city_plaza", new BlockPos(0, base + 4, 30), new BlockPos(0, base + 4, -20));
		view(context, server, "city_keep", new BlockPos(0, base + 8, -22), new BlockPos(0, base + 12, -75));
		view(context, server, "city_mage_quarter", new BlockPos(-35, base + 14, -30), new BlockPos(-66, base + 18, -62));
		view(context, server, "city_enchanter_hall", new BlockPos(-44, base + 2, -63), new BlockPos(-48, base, -72));
		view(context, server, "city_warrior_quarter", new BlockPos(40, base + 16, -40), new BlockPos(72, base + 2, -78));
		view(context, server, "city_old_town", new BlockPos(-36, base + 9, 22), new BlockPos(-68, base + 4, 24));
		view(context, server, "city_market_guild", new BlockPos(17, base + 7, 28), new BlockPos(17, base + 3, 60));
		view(context, server, "city_harbor", new BlockPos(38, base + 16, 38), new BlockPos(80, base + 2, 76));
		view(context, server, "city_archer_park", new BlockPos(-38, base + 12, 58), new BlockPos(-64, base + 2, 86));
		view(context, server, "city_urahara_shop", new BlockPos(-80, base + 6, 36), new BlockPos(-83, base + 2, 55));
		view(context, server, "city_hunter_association", new BlockPos(82, base + 7, -34), new BlockPos(85, base + 4, -53));

		for (JobClass job : JobClass.PLAYABLE) {
			BlockPos home = CityZone.trainerHome(job, base);
			BlockPos camera = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				for (int distance = 3; distance >= 2; distance--) {
					for (Direction direction : Direction.Plane.HORIZONTAL) {
						BlockPos candidate = home.relative(direction, distance).above();
						boolean clear = true;
						for (int step = 1; step <= distance; step++) {
							clear &= level.getBlockState(home.relative(direction, step).above()).isAir();
						}
						if (clear) {
							return candidate;
						}
					}
				}
				return home.offset(2, 1, 2);
			});
			view(context, server, "trainer_" + job.id(), camera, home.above());
		}

		for (CityNpc.Role role : new CityNpc.Role[] {CityNpc.Role.BOUNTY_CLERK, CityNpc.Role.BROKER, CityNpc.Role.ENHANCER, CityNpc.Role.DUNGEON_WARDEN,
			CityNpc.Role.HERALD}) {
			BlockPos home = CityZone.npcHome(role, base);
			BlockPos camera = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				for (int distance = 3; distance >= 2; distance--) {
					for (Direction direction : Direction.Plane.HORIZONTAL) {
						boolean clear = true;
						for (int step = 1; step <= distance; step++) {
							clear &= level.getBlockState(home.relative(direction, step).above()).isAir() && level.getBlockState(home.relative(direction, step)).isAir();
						}
						if (clear) {
							return home.relative(direction, distance).above();
						}
					}
				}
				return home.offset(2, 1, 2);
			});
			view(context, server, "npc_" + role.id(), camera, home.above());
		}

		// the dialog over the city, as a right-click opens it
		int mageId = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(ClassTrainer.class,
			new AABB(CityZone.trainerHome(JobClass.MAGE, base)).inflate(8), t -> t.job() == JobClass.MAGE).getFirst().getId());
		server.runOnServer(s -> ServerPlayNetworking.send(s.getPlayerList().getPlayers().getFirst(), new OpenTrainerPayload(mageId, JobClass.MAGE)));
		context.waitForScreen(TrainerScreen.class);
		context.waitTicks(5);
		context.takeScreenshot("city_trainer_dialog");
		context.setScreen(() -> null);
		context.runOnClient(minecraft -> minecraft.options.renderDistance().set(5));
		server.runOnServer(s -> s.getPlayerList().setViewDistance(5));
		server.runCommand("gamemode creative @p");
	}

	private static void view(final ClientGameTestContext context, final TestServerContext server, final String name, final BlockPos camera, final BlockPos target) {
		server.runCommand("tp @p " + (camera.getX() + 0.5) + " " + camera.getY() + " " + (camera.getZ() + 0.5));
		context.waitTicks(10);
		context.getInput().lookAt(target);
		context.waitTicks(60);
		context.takeScreenshot(name);
	}

	private static void require(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private CityChecks() {
	}
}
