package com.minecraftmode.test;

import com.google.gson.JsonObject;
import com.minecraftmode.MinecraftMode;
import com.minecraftmode.city.CityZone;
import com.minecraftmode.companion.Companions;
import com.minecraftmode.companion.MountEntity;
import com.minecraftmode.companion.PetEntity;
import com.minecraftmode.entity.ClassTrainer;
import com.minecraftmode.entity.boss.RaidBoss;
import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.raid.RaidDimension;
import com.minecraftmode.test.SiteShotCamera.Spot;
import com.minecraftmode.test.SiteShotCamera.View;
import com.minecraftmode.test.SiteShotFiles.Shot;
import com.minecraftmode.worldgen.lair.LairDef;
import com.minecraftmode.worldgen.lair.NamedLairs;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Screenshots for the server website (stormhold-site), not a check: run it on its own with
 * {@code ./gradlew runClientGameTest -Ptests=siteshots}. Korean, 1600×900, no HUD, in the same seeded normal world as
 * {@link NormalWorldClientGameTest}: the capital's districts and the seven trainers, every named monster standing in a biome of
 * its lair, every lair the world placed (underground ones with their roof cleared), every raid boss in its arena, and every pet and
 * mount on a plain. Writes {@link SiteShotFiles the pictures and their list} to {@code site-shots/} in the run folder; the site's
 * {@code npm run import-shots} takes them from there. The language and window size go back afterwards so later runs of the other
 * tests are unchanged. Camera placement is in {@link SiteShotCamera}.
 *
 * <p>The run is split into {@link #SECTIONS}. {@code -Pshots=trainers,raids} (system property {@code minecraft_mode.shots}) takes
 * only those, and {@code ./gradlew runSiteShotsParallel} spreads the sections over several game clients.
 */
public class SiteShotsClientGameTest implements FabricClientGameTest {
	/** The parts of a run, in order. Kept in step with {@code siteShotSections} in build.gradle. */
	static final List<String> SECTIONS = List.of("city", "trainers", "named", "lairs", "raids", "companions");
	private static final String SEED = "minecraft_mode";
	private static final int WIDTH = 1600;
	private static final int HEIGHT = 900;
	/** How far (blocks) to look for a named monster's biome and (chunks) for a lair, from the search origin. */
	private static final int BIOME_RADIUS = 6400;
	private static final int LAIR_RADIUS_CHUNKS = 160;
	private static final BlockPos SEARCH_ORIGIN = new BlockPos(600, 80, 300);
	private static final String MONSTERS_PAGE = "/wiki/monsters/";
	private static final String CONTENT_PAGE = "/wiki/content/";
	/** Time for new terrain around a far teleport to generate and draw. */
	private static final int TERRAIN_TICKS = 160;
	/** Clear ground a camera spot must have in front of the subject. */
	private static final double OPEN_DISTANCE = 3.0;
	private static final Map<String, String> CITY_CAPTIONS = Map.ofEntries(
		Map.entry("city_plaza", "수도 스톰홀드 광장"), Map.entry("city_keep", "수도의 성"), Map.entry("city_mage_quarter", "마법사 구역"),
		Map.entry("city_enchanter_hall", "마법 부여소"), Map.entry("city_enchanter_grindstone", "마법 부여대와 숫돌"),
		Map.entry("city_warrior_quarter", "전사 구역"), Map.entry("city_old_town", "구시가지"), Map.entry("city_market_guild", "시장과 길드"),
		Map.entry("city_harbor", "항구"), Map.entry("city_archer_park", "궁수 공원"), Map.entry("city_urahara_shop", "우라하라 상점"),
		Map.entry("city_hunter_association", "헌터 협회"), Map.entry("city_courtyard_portals", "성 안뜰의 차원문"),
		Map.entry("city_plaza_waystone", "광장의 웨이스톤"), Map.entry("homestead_plains", "거주구역 평원"),
		Map.entry("city_training_entrance", "수련장 입구"));

	/** Where this run writes and what it has taken so far. */
	private record Run(Path out, List<Shot> shots) {
	}

	/** Something to stand in front of the camera: its entity type, size, and where its picture goes on the site. */
	private record Subject(String section, EntityType<?> type, float width, float height, String name, String page, String after, String caption) {
	}

	@Override
	public void runTest(final ClientGameTestContext context) {
		if (TestSelection.skip(this)) {
			return;
		}
		Run run = new Run(FabricLoader.getInstance().getGameDir().resolve("site-shots"), new ArrayList<>());
		List<String> picked = pickedSections();
		List<String> done = new ArrayList<>();
		// this run's sections start empty; others a previous run left in this folder stay
		JsonObject previous = SiteShotFiles.previous(run.out());
		picked.forEach(section -> SiteShotFiles.resetDir(run.out().resolve(section)));
		String language = context.computeOnClient(minecraft -> minecraft.options.languageCode);
		int[] window = context.computeOnClient(minecraft -> new int[] {minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight()});
		setLanguage(context, "ko_kr");
		context.getInput().resizeWindow(WIDTH, HEIGHT);
		try {
			shootInWorld(context, run, picked, done);
		} finally {
			// put the client back first: a later test in this run folder must not start in Korean at 1600×900
			try {
				context.getInput().resizeWindow(window[0], window[1]);
				setLanguage(context, language);
			} finally {
				SiteShotFiles.write(run.out(), previous, picked, done, run.shots());
			}
		}
		MinecraftMode.LOGGER.info("[site] {} screenshots ({}) in {}", run.shots().size(), String.join(", ", done), run.out());
	}

	/** The sections this run takes: {@code minecraft_mode.shots} (comma separated), or all of them. */
	private static List<String> pickedSections() {
		List<String> asked = Arrays.stream(System.getProperty("minecraft_mode.shots", "").split(","))
			.map(String::trim).filter(x -> !x.isEmpty()).map(x -> x.toLowerCase(Locale.ROOT)).toList();
		List<String> unknown = asked.stream().filter(x -> !SECTIONS.contains(x)).toList();
		if (!unknown.isEmpty()) {
			throw new IllegalArgumentException("unknown screenshot section(s) " + unknown + "; known: " + SECTIONS);
		}
		return asked.isEmpty() ? SECTIONS : SECTIONS.stream().filter(asked::contains).toList();
	}

	/** Creates the seeded world and takes the picked sections in it; {@code done} gets each section as it finishes. */
	private static void shootInWorld(final ClientGameTestContext context, final Run run, final List<String> picked, final List<String> done) {
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
			for (String rule : new String[] {"send_command_feedback false", "log_admin_commands false", "spawn_mobs false"}) {
				server.runCommand("gamerule " + rule);
			}
			server.runCommand("weather clear 1000000");
			server.runCommand("gamemode spectator @p");
			setView(context, server, true, 12);
			for (String section : picked) {
				switch (section) {
					case "city" -> city(context, server, run);
					case "trainers" -> trainers(context, server, run);
					case "named" -> named(context, server, run);
					case "lairs" -> lairs(context, server, run);
					case "raids" -> raids(context, server, connection, run);
					case "companions" -> companions(context, server, run);
					default -> throw new IllegalStateException(section);
				}
				// only finished sections go in the list, so a run that fails halfway does not replace a section with half of it
				done.add(section);
			}
			setView(context, server, false, 5);
			WorldClose.prepare(context, server);
		}
	}

	/** The HUD hidden or shown (F1), and the client and server view distance. */
	private static void setView(final ClientGameTestContext context, final TestServerContext server, final boolean hideHud, final int distance) {
		context.runOnClient(minecraft -> {
			if (minecraft.gui.hud.isHidden() != hideHud) {
				minecraft.gui.hud.toggle();
			}
			minecraft.options.renderDistance().set(distance);
		});
		server.runOnServer(s -> s.getPlayerList().setViewDistance(distance));
	}

	// ------------------------------------------------------------ the capital and its trainers

	private static void city(final ClientGameTestContext context, final TestServerContext server, final Run run) {
		int base = server.computeOnServer(s -> CityZone.baseY(s.overworld()));
		// let the whole city stream in once, so each view below only waits for its own corner
		server.runCommand("tp @p 0.5 " + (base + 70) + " 95.5");
		context.waitTicks(300);
		for (CityChecks.Scene scene : CityChecks.scenes(base)) {
			look(context, server, scene.camera(), scene.target());
			shoot(context, run, "city", scene.name(), null, null, CITY_CAPTIONS.getOrDefault(scene.name(), ""));
		}
	}

	private static void trainers(final ClientGameTestContext context, final TestServerContext server, final Run run) {
		int base = server.computeOnServer(s -> CityZone.baseY(s.overworld()));
		for (JobClass job : JobClass.PLAYABLE) {
			BlockPos home = CityZone.trainerHome(job, base);
			BlockPos camera = CityChecks.besideHome(server, home, false);
			// the trainer turns to face the camera and holds still for the shot (it would otherwise keep its own facing)
			setTrainerFacing(server, job, home, camera, true);
			look(context, server, camera, home.above());
			// the site names class pages after the English heading ("Soul Reaper" -> soul-reaper), not the id
			String page = "/wiki/classes/" + job.en().toLowerCase(Locale.ROOT).replace(' ', '-') + "/";
			shoot(context, run, "trainers", "trainer_" + job.id(), page, null, job.ko() + " 교관");
			setTrainerFacing(server, job, home, camera, false);
		}
	}

	/** {@code hold}: turns {@code job}'s trainer toward {@code camera} and stops its AI; otherwise gives the AI back. */
	private static void setTrainerFacing(final TestServerContext server, final JobClass job, final BlockPos home, final BlockPos camera,
		final boolean hold) {
		server.runOnServer(s -> {
			for (ClassTrainer trainer : s.overworld().getEntitiesOfClass(ClassTrainer.class, new AABB(home).inflate(8), t -> t.job() == job)) {
				trainer.setNoAi(hold);
				if (hold) {
					float yaw = SiteShotCamera.facingYaw(trainer.position(), Vec3.atCenterOf(camera));
					trainer.setYRot(yaw);
					trainer.setYHeadRot(yaw);
					trainer.setYBodyRot(yaw);
					trainer.setXRot(0.0F);
				}
			}
		});
	}

	// ------------------------------------------------------------ named monsters, pets and mounts

	/** Each named monster in a biome where its lair stands (so where it lives), facing the camera. */
	private static void named(final ClientGameTestContext context, final TestServerContext server, final Run run) {
		for (NamedDef def : NamedMobs.all()) {
			LairDef lair = NamedLairs.all().stream().filter(l -> l.named() == def).findFirst().orElse(null);
			if (lair == null || lair.biomes().isEmpty()) {
				MinecraftMode.LOGGER.warn("[site] {}: no lair biome to stand in, skipped", def.id());
				continue;
			}
			ResourceKey<Level> dimension = SiteShotCamera.dimension(lair.setting());
			BlockPos found = server.computeOnServer(s -> {
				var hit = s.getLevel(dimension).findClosestBiome3d(b -> lair.biomes().stream().anyMatch(b::is), SEARCH_ORIGIN.atY(64), BIOME_RADIUS, 32, 64);
				return hit == null ? null : hit.getFirst();
			});
			if (found == null) {
				MinecraftMode.LOGGER.warn("[site] {}: none of its biomes within {} blocks, skipped", def.id(), BIOME_RADIUS);
				continue;
			}
			Subject subject = new Subject("named", NamedMobs.type(def), def.width(), def.height(), "named_" + def.id(), MONSTERS_PAGE, def.ko(), def.ko());
			portrait(context, server, run, dimension, found, lair.setting(), subject);
		}
		server.runCommand("effect clear @p");
	}

	/** Every pet and mount standing in a grassy plain, under its row of the pet and mount tables. */
	private static void companions(final ClientGameTestContext context, final TestServerContext server, final Run run) {
		BlockPos plain = server.computeOnServer(s -> {
			var hit = s.overworld().findClosestBiome3d(b -> b.is(Biomes.PLAINS) || b.is(Biomes.MEADOW), SEARCH_ORIGIN.atY(64), BIOME_RADIUS, 32, 64);
			return hit == null ? null : hit.getFirst();
		});
		if (plain == null) {
			MinecraftMode.LOGGER.warn("[site] no plains within {} blocks for the pets and mounts, skipped", BIOME_RADIUS);
			return;
		}
		List<Subject> subjects = new ArrayList<>();
		for (Companions.PetDef def : Companions.pets()) {
			subjects.add(new Subject("companions", Companions.petType(def), def.width(), def.height(), "pet_" + def.id(), CONTENT_PAGE, def.ko(), def.ko()));
		}
		for (Companions.MountDef def : Companions.mounts()) {
			subjects.add(new Subject("companions", Companions.mountType(def), def.width(), def.height(), "mount_" + def.id(), CONTENT_PAGE, def.ko(), def.ko()));
		}
		for (Subject subject : subjects) {
			portrait(context, server, run, Level.OVERWORLD, plain, LairDef.Setting.SURFACE, subject);
		}
	}

	/**
	 * Stands {@code subject} (no AI) on open ground near {@code sample}, facing a camera that sees it whole, and takes the picture.
	 * Skipped with a warning when there is no such spot around or it does not spawn.
	 */
	private static void portrait(final ClientGameTestContext context, final TestServerContext server, final Run run, final ResourceKey<Level> dimension,
		final BlockPos sample, final LairDef.Setting setting, final Subject subject) {
		load(context, server, dimension, sample, 2);
		// the spot needs at least OPEN_DISTANCE of clear ground (or it ends up in a pit); small pets are then shot from closer
		double distance = Math.max(1.6, subject.width() * 1.6 + subject.height());
		boolean surface = setting == LairDef.Setting.SURFACE;
		Spot found = server.computeOnServer(s -> SiteShotCamera.spot(s.getLevel(dimension), sample, surface, subject.height(), Math.max(OPEN_DISTANCE, distance)));
		Spot spot = found == null || distance >= OPEN_DISTANCE ? found
			: new Spot(found.feet(), found.middle(), found.middle().add(found.eye().subtract(found.middle()).scale(distance / OPEN_DISTANCE)));
		String in = "execute in " + dimension.identifier() + " run ";
		if (spot == null) {
			MinecraftMode.LOGGER.warn("[site] {}: no open spot to see it from near {}, skipped", subject.name(), sample);
		} else {
			// caves are dark; the Nether and the End have their own light (night vision would wash them out)
			server.runCommand(setting == LairDef.Setting.UNDERGROUND ? "effect give @p night_vision infinite 0 true" : "effect clear @p");
			moveCamera(context, server, dimension, new View(spot.eye(), spot.middle()), TERRAIN_TICKS);
			// the subject comes last: a pet leaves without its owner nearby, a mount without a rider after a few seconds
			int entityId = server.computeOnServer(s -> spawnFacing(s.getLevel(dimension), s.getPlayerList().getPlayers().getFirst(), subject, spot));
			if (entityId < 0) {
				MinecraftMode.LOGGER.warn("[site] {}: did not spawn at {}, skipped", subject.name(), spot.feet());
			} else {
				context.waitTicks(20);
				shoot(context, run, subject.section(), subject.name(), subject.page(), subject.after(), subject.caption());
				server.runOnServer(s -> {
					Entity entity = s.getLevel(dimension).getEntity(entityId);
					if (entity != null) {
						entity.discard();
					}
				});
			}
		}
		server.runCommand(in + "forceload remove all");
	}

	/** Spawns {@code subject} at the spot's feet, still and facing the camera, owned by {@code player} if it needs an owner. Its id, or -1. */
	private static int spawnFacing(final ServerLevel level, final ServerPlayer player, final Subject subject, final Spot spot) {
		Entity entity = subject.type().spawn(level, spot.feet(), EntitySpawnReason.SPAWN_ITEM_USE);
		if (entity == null) {
			return -1;
		}
		if (entity instanceof PetEntity pet) {
			pet.setOwner(player.getUUID());
		} else if (entity instanceof MountEntity mount) {
			mount.setup(player);
		}
		if (entity instanceof Mob mob) {
			mob.setNoAi(true);
			mob.setPersistenceRequired();
		}
		float yaw = SiteShotCamera.facingYaw(spot.middle(), spot.eye());
		entity.setYRot(yaw);
		entity.setYHeadRot(yaw);
		entity.setYBodyRot(yaw);
		return entity.getId();
	}

	// ------------------------------------------------------------ lairs and raid bosses

	/** Every lair the world placed by itself, seen whole from above its south side; an underground one is uncovered first. */
	private static void lairs(final ClientGameTestContext context, final TestServerContext server, final Run run) {
		for (LairDef def : NamedLairs.all()) {
			ResourceKey<Level> dimension = SiteShotCamera.dimension(def.setting());
			BlockPos found = server.computeOnServer(s -> {
				ServerLevel level = s.getLevel(dimension);
				var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(def.key());
				var nearest = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(structure), SEARCH_ORIGIN,
					LAIR_RADIUS_CHUNKS, false);
				return nearest == null ? null : nearest.getFirst();
			});
			if (found == null) {
				MinecraftMode.LOGGER.warn("[site] lair {}: none within {} chunks in {}, skipped", def.id(), LAIR_RADIUS_CHUNKS, dimension.identifier());
				continue;
			}
			BlockPos center = found.offset(8, 0, 8);
			load(context, server, dimension, center, 3);
			boolean underground = def.setting() == LairDef.Setting.UNDERGROUND;
			if (underground) {
				SiteShotCamera.uncover(server, def, center);
			}
			server.runCommand("effect clear @p");
			moveCamera(context, server, dimension, server.computeOnServer(s -> SiteShotCamera.wholeLair(s.getLevel(dimension), def, center)), 200);
			context.waitTicks(50);
			String caption = def.ko() + " — " + def.named().ko() + "의 소굴" + (underground ? " (지붕을 걷어 낸 모습)" : "");
			shoot(context, run, "lairs", "lair_" + def.id(), MONSTERS_PAGE, def.ko(), caption);
			server.runCommand("execute in " + dimension.identifier() + " run forceload remove all");
		}
		server.runCommand("effect clear @p");
	}

	/**
	 * Each raid boss in its arena, from in front of it (the arena and the boss, nothing of the fight). Goes under the boss's heading
	 * ("베르데사 — 가시덤불의 어머니") on the monsters page.
	 */
	private static void raids(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection, final Run run) {
		// the arenas want a creative player that can fly; the boss bar is part of the HUD, which stays hidden
		server.runCommand("gamemode creative @p");
		server.runCommand("item replace entity @p weapon.mainhand with air");
		int slot = 40;
		for (BossDef def : RaidBosses.all()) {
			GearRaidClientGameTest.stageBoss(server, connection, def, slot);
			context.waitTicks(20);
			final int arena = slot;
			// from where the party arrives the boss is small; step in front of it, as far back as its size needs
			View view = server.computeOnServer(s -> {
				RaidBoss boss = RaidDimension.level(s).getEntitiesOfClass(RaidBoss.class, new AABB(arena * 1024 - 60, 0, -60, arena * 1024 + 60, 200, 60)).getFirst();
				Vec3 middle = boss.position().add(0.0, boss.getBbHeight() * 0.5, 0.0);
				double distance = Math.max(5.0, boss.getBbWidth() * 1.4 + boss.getBbHeight() * 0.9);
				Vec3 ahead = boss.getForward().multiply(1.0, 0.0, 1.0).normalize().scale(distance);
				return new View(middle.add(ahead).add(0.0, boss.getBbHeight() * 0.2 + 0.5, 0.0), middle);
			});
			moveCamera(context, server, RaidDimension.LEVEL, view, TERRAIN_TICKS / 2);
			String heading = def.ko() + " — " + def.epithetKo();
			shoot(context, run, "raids", "raid_" + def.id(), MONSTERS_PAGE, heading, heading);
			GearRaidClientGameTest.clearBoss(server, slot);
			slot++;
		}
		server.runCommand("execute in minecraft:overworld run tp @p 0.5 200 0.5");
		server.runCommand("gamemode spectator @p");
	}

	// ------------------------------------------------------------ camera and pictures

	/**
	 * Puts the camera eye at {@code view} in {@code dimension}, looking at its target, at noon, and waits {@code settleTicks} for the
	 * terrain to generate and draw; then steps it a little, because a static camera does not redraw sections that arrived meanwhile
	 * (and waitForChunksRender() times out while a whole fresh view distance generates).
	 */
	private static void moveCamera(final ClientGameTestContext context, final TestServerContext server, final ResourceKey<Level> dimension,
		final View view, final int settleTicks) {
		double[] look = SiteShotCamera.yawPitch(view.eye(), view.target());
		String facing = " " + look[0] + " " + look[1];
		String tp = "execute in " + dimension.identifier() + " run tp @p ";
		// tp moves the feet; the spectator's eye is above them
		double feetY = view.eye().y - SiteShotCamera.EYE_HEIGHT;
		server.runCommand("time set noon");
		server.runCommand(tp + view.eye().x + " " + feetY + " " + view.eye().z + facing);
		context.waitTicks(settleTicks);
		server.runCommand(tp + (view.eye().x + 0.05) + " " + feetY + " " + view.eye().z + facing);
		context.waitTicks(10);
	}

	/** Stands at {@code camera} in the overworld looking at {@code target}, as the capital views do. */
	private static void look(final ClientGameTestContext context, final TestServerContext server, final BlockPos camera, final BlockPos target) {
		server.runCommand("time set noon");
		server.runCommand("tp @p " + (camera.getX() + 0.5) + " " + camera.getY() + " " + (camera.getZ() + 0.5));
		context.waitTicks(10);
		context.getInput().lookAt(target);
		context.waitTicks(60);
	}

	/** Force-loads the chunks around {@code at} and waits until the server has them. */
	private static void load(final ClientGameTestContext context, final TestServerContext server, final ResourceKey<Level> dimension, final BlockPos at,
		final int radiusChunks) {
		int reach = radiusChunks * 16;
		server.runCommand("execute in " + dimension.identifier() + " run forceload add " + (at.getX() - reach) + " " + (at.getZ() - reach) + " "
			+ (at.getX() + reach) + " " + (at.getZ() + reach));
		for (int i = 0; i < 60; i++) {
			boolean loaded = server.computeOnServer(s -> s.getLevel(dimension).hasChunk(at.getX() >> 4, at.getZ() >> 4));
			if (loaded) {
				return;
			}
			context.waitTicks(10);
		}
	}

	/** Takes the screenshot into {@code <section>/}, keeps it as a JPEG and notes where on the site it goes. */
	private static void shoot(final ClientGameTestContext context, final Run run, final String section, final String name, final String page,
		final String after, final String caption) {
		Path dir = run.out().resolve(section);
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			throw new UncheckedIOException("could not create " + dir, e);
		}
		Path png = context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withDestinationDir(dir));
		run.shots().add(new Shot(SiteShotFiles.saveJpeg(png, run.out(), section, name), page, after, caption));
	}

	private static void setLanguage(final ClientGameTestContext context, final String code) {
		AtomicReference<CompletableFuture<Void>> reload = new AtomicReference<>();
		context.runOnClient(minecraft -> {
			if (code.equals(minecraft.options.languageCode)) {
				reload.set(CompletableFuture.completedFuture(null));
				return;
			}
			minecraft.options.languageCode = code;
			minecraft.getLanguageManager().setSelected(code);
			reload.set(minecraft.reloadResourcePacks());
		});
		context.waitFor(minecraft -> reload.get().isDone(), 20 * 120);
	}
}
