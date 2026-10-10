package com.minecraftmode.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.job.JobKeys;
import com.minecraftmode.client.job.JobScreen;
import com.minecraftmode.client.job.TrainerScreen;
import com.minecraftmode.economy.ShopOffers;
import com.minecraftmode.economy.ShopType;
import com.minecraftmode.economy.Wallet;
import com.minecraftmode.entity.ClassTrainer;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.JobStats;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.engrave.EngraveTotals;
import com.minecraftmode.job.engrave.Engraving;
import com.minecraftmode.job.engrave.EngravingMenu;
import com.minecraftmode.job.engrave.Engravings;
import com.minecraftmode.job.gear.ClassAbilities;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.GearStats;
import com.minecraftmode.job.quest.QuestDef;
import com.minecraftmode.job.quest.QuestService;
import com.minecraftmode.job.quest.Quests;
import com.minecraftmode.job.skill.Actions;
import com.minecraftmode.job.skill.Movement;
import com.minecraftmode.job.skill.Skill;
import com.minecraftmode.job.skill.SkillCaster;
import com.minecraftmode.job.weapon.Archetype;
import com.minecraftmode.job.weapon.BasicAttacks;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.WeaponDef;
import com.minecraftmode.loot.GearIndex;
import com.minecraftmode.loot.GearShop;
import com.minecraftmode.network.OpenTrainerPayload;
import com.minecraftmode.network.QuestActionPayload;
import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.registry.ModEntities;
import com.minecraftmode.registry.ModItems;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Class system: content counts and assets, leveling and advancement, skill keys, requirement
 * gating, stacking engravings, the engraving table, guild offers, boss drops, and a smoke test that
 * casts every one of the skills.
 */
public class JobClientGameTest implements FabricClientGameTest {
	private static final String TARGET = "job_target";

	@Override
	public void runTest(final ClientGameTestContext context) {
		checkContent(context);
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerConnection connection = singleplayer.getConnection();
			TestServerContext server = singleplayer.getServer();
			connection.waitForChunksRender();
			server.runCommand("difficulty normal");
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("time set noon");
			server.runCommand("gamemode survival @p");
			server.runCommand("tp @p 0.5 -60 0.5 180 0");
			context.waitTicks(5);

			checkProgression(context, server, connection);
			checkSkillKey(context, server, connection);
			checkGating(server, connection);
			checkNewClasses(server, connection);
			checkSafeMovement(context, server, connection);
			checkEngravingStacks(context, server, connection);
			checkEngravingTable(context, server, connection);
			checkBasicShot(context, server, connection);
			checkGuildAndDrops(context, server, connection);
			jobScreen(context, server);
			castEverySkill(context, server, connection);
			WorldClose.prepare(context, server);
		}
	}

	// ------------------------------------------------------------------ content and assets

	private static void checkContent(final ClientGameTestContext context) {
		require(JobWeapons.all().size() == 161, "expected 161 class weapons, got " + JobWeapons.all().size());
		int skills = 0;
		for (JobClass job : JobClass.PLAYABLE) {
			int[] perTier = new int[5];
			for (WeaponDef def : JobWeapons.of(job)) {
				perTier[def.tier()]++;
				int need = def.tier() == 4 ? 4 : 3;
				require(def.skills().size() >= need, def.id() + " has " + def.skills().size() + " skills");
				skills += def.skills().size();
			}
			require(perTier[1] == 3 && perTier[2] == 6 && perTier[3] == 6 && perTier[4] == 8,
				job.id() + " weapons per tier 3/6/6/8, got " + perTier[1] + "/" + perTier[2] + "/" + perTier[3] + "/" + perTier[4]);
			long engravings = java.util.Arrays.stream(Engraving.values()).filter(e -> e.job() == job).count();
			require(engravings >= 10, job.id() + " has only " + engravings + " engravings");
		}
		require(skills == JobWeapons.skillCount(), "skill ids are not unique");
		require(Quests.all().size() == 28 && Quests.tokens().size() == 28, "expected 28 trials and 28 tokens");
		for (JobClass job : JobClass.PLAYABLE) {
			for (int tier = 1; tier <= 4; tier++) {
				QuestDef quest = Quests.forTier(job, tier);
				require(quest != null && quest.job() == job && quest.tier() == tier, "missing trial for " + job.id() + " tier " + tier);
				if (tier == 1) {
					require(quest.instant(), quest.id() + " (the first choice of class) should need no trial");
				} else {
					require(!quest.kills().isEmpty() && quest.tokenCount() > 0, quest.id() + " should be a real trial");
					require(quest.materials().stream().anyMatch(m -> m.item() == ModItems.ESSENCE || m.item() == ModItems.CONDENSED_ESSENCE), quest.id() + " should cost essence");
				}
			}
		}
		final int skillTotal = skills;

		context.runOnClient(minecraft -> {
			var resources = minecraft.getResourceManager();
			JsonObject en = lang(minecraft, "en_us");
			JsonObject ko = lang(minecraft, "ko_kr");
			List<String> missing = new ArrayList<>();
			Set<String> keys = new HashSet<>();
			for (WeaponDef def : JobWeapons.all()) {
				keys.add(def.nameKey());
				def.skills().forEach(s -> keys.add(s.nameKey()));
				require(resources.getResource(MinecraftMode.id("textures/item/" + def.id() + ".png")).isPresent(), "missing texture for " + def.id());
				require(resources.getResource(MinecraftMode.id("items/" + def.id() + ".json")).isPresent(), "missing item model for " + def.id());
				if (def.archetype().mode() == Archetype.BasicMode.DRAW) {
					require(resources.getResource(MinecraftMode.id("textures/item/" + def.id() + "_pulling_2.png")).isPresent(), "missing pulling frame for " + def.id());
				}
			}
			Actions.texts().keySet().forEach(k -> keys.add("skill_action.minecraft_mode." + k));
			for (Engraving e : Engraving.values()) {
				keys.add(e.nameKey());
			}
			for (EngraveStat stat : EngraveStat.values()) {
				keys.add(stat.key());
			}
			for (JobClass job : JobClass.PLAYABLE) {
				for (int tier = 1; tier <= 4; tier++) {
					keys.add(job.tierKey(tier));
					keys.add(job.passiveKey(tier));
					keys.add(job.passiveDescKey(tier));
				}
				keys.add(ClassTrainer.nameKey(job));
				keys.add(ClassTrainer.greetingKey(job));
				require(resources.getResource(MinecraftMode.id("textures/entity/trainer/" + job.id() + ".png")).isPresent(), "missing trainer skin for " + job.id());
			}
			for (QuestDef quest : Quests.all()) {
				keys.add(quest.nameKey());
				keys.add(quest.storyKey());
				for (int i = 0; i < quest.kills().size(); i++) {
					keys.add(quest.goalKey(i));
				}
			}
			for (net.minecraft.world.item.Item token : Quests.tokens()) {
				keys.add(token.getDescriptionId());
				String id = BuiltInRegistries.ITEM.getKey(token).getPath();
				require(resources.getResource(MinecraftMode.id("textures/item/" + id + ".png")).isPresent(), "missing texture for " + id);
				require(resources.getResource(MinecraftMode.id("items/" + id + ".json")).isPresent(), "missing item model for " + id);
			}
			for (int page = 1; page <= com.minecraftmode.city.CityServices.GUIDE_PAGES; page++) {
				keys.add("book.minecraft_mode.guide.page" + page);
			}
			for (String key : keys) {
				if (!en.has(key)) {
					missing.add("en_us:" + key);
				}
				if (!ko.has(key)) {
					missing.add("ko_kr:" + key);
				}
			}
			require(missing.isEmpty(), "missing translations: " + missing.subList(0, Math.min(10, missing.size())));
			for (String particle : new String[] {"spark", "slash", "orb", "ring", "rune", "shard", "smoke", "petal", "coin", "bolt", "feather", "bubble"}) {
				require(resources.getResource(MinecraftMode.id("textures/particle/" + particle + ".png")).isPresent(), "missing particle " + particle);
			}
		});
		MinecraftMode.LOGGER.info("[job] 161 weapons (3/6/6/8 per class), {} skills, textures/models/lang (en+ko) for all", skillTotal);
	}

	private static JsonObject lang(final Minecraft minecraft, final String code) {
		Resource resource = minecraft.getResourceManager().getResource(MinecraftMode.id("lang/" + code + ".json")).orElseThrow();
		try (Reader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		} catch (java.io.IOException e) {
			throw new AssertionError("cannot read " + code, e);
		}
	}

	// ------------------------------------------------------------------ progression

	/**
	 * Class choice and advancement through the warrior trainer: the dialog opens over the network and
	 * choosing the class at level 10 (like the button does) makes a tier 1 warrior at once. The tier 2
	 * trial then counts kills, drops tokens into the inventory, and completing takes the tokens and
	 * materials and promotes the player. Ends at warrior tier 2,
	 * level 25, with an empty inventory and no active trial.
	 */
	private static void checkProgression(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		int trainerId = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			JobData start = JobProgression.get(player);
			require(start.job() == JobClass.NONE && start.level() == 1, "new players start without a class at level 1");
			require(QuestService.get(player).visitedCity(), "joining should mark the welcome as done");
			require(JobProgression.count(player.getInventory(), Items.WRITTEN_BOOK) == 1, "new players should get the guide book");
			player.getInventory().clearContent();
			ClassTrainer trainer = ModEntities.CLASS_TRAINER.create(player.level(), EntitySpawnReason.COMMAND);
			require(trainer != null, "could not create a trainer");
			trainer.setJob(JobClass.WARRIOR);
			trainer.snapTo(2.5, -60, -2.5, 135.0F, 0.0F);
			player.level().addFreshEntity(trainer);
			require(QuestService.status(player, JobClass.WARRIOR) == QuestService.Status.LOW_LEVEL, "level 1 players cannot take the first trial yet");
			int toTen = 0;
			for (int level = 1; level < 10; level++) {
				toTen += JobProgression.expToNext(level);
			}
			JobProgression.addExp(player, toTen);
			require(JobProgression.get(player).level() == 10, "level after " + toTen + " exp: " + JobProgression.get(player).level());
			require(QuestService.status(player, JobClass.WARRIOR) == QuestService.Status.AVAILABLE, "the warrior class should open at level 10");
			return trainer.getId();
		});

		// what a right-click does, then the Accept button's packet
		server.runOnServer(s -> ServerPlayNetworking.send(connection.getServerPlayer(), new OpenTrainerPayload(trainerId, JobClass.WARRIOR)));
		context.waitForScreen(TrainerScreen.class);
		context.waitTicks(3);
		context.takeScreenshot("trainer_dialog");
		context.runOnClient(minecraft -> ClientPlayNetworking.send(new QuestActionPayload(QuestActionPayload.Action.ACCEPT, trainerId)));
		context.waitTicks(5);
		context.takeScreenshot("trainer_dialog_accepted");
		context.setScreen(() -> null);
		context.waitTicks(2);

		int[] tokens = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			JobData data = JobProgression.get(player);
			require(data.job() == JobClass.WARRIOR && data.tier() == 1, "choosing at level 10 should make a tier 1 warrior at once, got " + data.job().id() + " " + data.tier());
			require(QuestService.active(player) == null, "the first class needs no trial");
			JobStats.refresh(player);
			double maxHealth = player.getAttributeValue(Attributes.MAX_HEALTH);
			require(maxHealth == 25.0, "warrior tier 1 at level 10 should have 20 + 4 (Iron Body) + 1 (level) health, got " + maxHealth);
			require(QuestService.status(player, JobClass.ROGUE) == QuestService.Status.OTHER_CLASS, "other trainers should turn a warrior away");

			// tier 2: level gate, abandon, kills and tokens, then the materials
			require(QuestService.status(player, JobClass.WARRIOR) == QuestService.Status.LOW_LEVEL, "tier 2 should need level 25");
			JobProgression.set(player, JobProgression.get(player).withProgress(25, 0));
			require(QuestService.accept(player, JobClass.WARRIOR), "accepting warrior_2 failed");
			QuestService.abandon(player);
			require(QuestService.active(player) == null, "abandoning should clear the trial");
			require(QuestService.accept(player, JobClass.WARRIOR), "accepting warrior_2 again failed");
			QuestDef quest = QuestService.active(player);
			require(quest != null && quest.id().equals("warrior_2"), "accepting should start warrior_2, got " + (quest == null ? null : quest.id()));
			require(QuestService.status(player, JobClass.ROGUE) == QuestService.Status.BUSY, "other trainers should see a busy player");
			require(!QuestService.complete(player, JobClass.WARRIOR), "an unfinished trial cannot be completed");
			// 6 pillagers killed by the player count; one killed by something else does not
			for (int i = 0; i < 7; i++) {
				var pillager = EntityTypes.PILLAGER.create(player.level(), EntitySpawnReason.COMMAND);
				require(pillager != null, "could not create a pillager");
				pillager.snapTo(0.5 + i % 4, -60, -6.5 - i / 4, 0.0F, 0.0F);
				pillager.setNoAi(true);
				player.level().addFreshEntity(pillager);
				pillager.hurtServer(player.level(), i == 0 ? player.damageSources().generic() : player.damageSources().playerAttack(player), 1000.0F);
			}
			int kills = QuestService.get(player).progress(0);
			int laurels = JobProgression.count(player.getInventory(), Quests.CHAMPIONS_LAUREL);
			boolean dropped = !player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(16), e -> e.getItem().is(Quests.CHAMPIONS_LAUREL)).isEmpty();
			require(kills == 6, "6 pillager kills should count, got " + kills);
			require(laurels > 0 && !dropped, "laurels should go straight into the inventory (got " + laurels + ", dropped " + dropped + ")");
			require(QuestService.status(player, JobClass.WARRIOR) == QuestService.Status.IN_PROGRESS, "the trial should still need its essence");

			// top up to exactly the 4 laurels and 8 essence it needs, plus one spare essence
			if (laurels < 4) {
				player.getInventory().add(new ItemStack(Quests.CHAMPIONS_LAUREL, 4 - laurels));
			}
			player.getInventory().add(new ItemStack(ModItems.ESSENCE, 9));
			require(QuestService.status(player, JobClass.WARRIOR) == QuestService.Status.READY, "warrior_2 should be ready");
			require(QuestService.complete(player, JobClass.WARRIOR), "completing warrior_2 failed");
			require(JobProgression.get(player).tier() == 2, "tier should be 2");
			require(JobProgression.count(player.getInventory(), ModItems.ESSENCE) == 1, "warrior_2 should take 8 of 9 essence");
			require(JobProgression.count(player.getInventory(), Quests.CHAMPIONS_LAUREL) == Math.max(0, laurels - 4), "warrior_2 should take 4 laurels");
			require(QuestService.active(player) == null, "no trial should be active after completing");
			// the trial kills gave EXP; later checks expect a fresh level 25
			JobProgression.set(player, JobProgression.get(player).withProgress(25, 0));
			player.getInventory().clearContent();
			for (ClassTrainer trainer : player.level().getEntitiesOfClass(ClassTrainer.class, player.getBoundingBox().inflate(16))) {
				trainer.discard();
			}
			return new int[] {laurels};
		});
		MinecraftMode.LOGGER.info("[job] trainer dialog + network class choice (tier 1 at once), warrior_2: 6 kills, {} laurels dropped into the inventory, completed", tokens[0]);
	}

	// ------------------------------------------------------------------ skills

	private static void checkSkillKey(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JobWeapons.item(JobWeapons.def("iron_greatsword"))));
			fill(player);
			spawnTarget(player.level(), 0.5, -60, -1.8);
			spawnTarget(player.level(), 1.3, -60, -2.4);
		});
		context.waitTicks(5);
		context.getInput().pressKey(JobKeys.SKILLS[0]);
		context.waitTicks(6);
		Object[] result = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			JobData data = JobProgression.get(player);
			float lowest = 400.0F;
			for (LivingEntity target : targets(player)) {
				lowest = Math.min(lowest, target.getHealth());
			}
			return new Object[] {lowest, data.mana(), JobStats.maxMana(player), data.readyAt("iron_greatsword.crushing_cleave") > player.level().getGameTime()};
		});
		require((float)result[0] < 400.0F, "Crushing Cleave (key R) did not damage the targets");
		require((int)result[1] < (int)result[2], "casting should spend MP");
		require((boolean)result[3], "Crushing Cleave should be on cooldown");
		SkillCaster.Result again = server.computeOnServer(s -> SkillCaster.tryCast(connection.getServerPlayer(), 0));
		require(again == SkillCaster.Result.COOLDOWN, "second cast right away should be on cooldown, got " + again);
		context.waitTicks(10);
		context.takeScreenshot("job_hud_cooldown");
		server.runCommand("kill @e[tag=" + TARGET + "]");
		MinecraftMode.LOGGER.info("[job] skill key R cast Crushing Cleave: hit targets, spent MP, started the cooldown");
	}

	private static void checkGating(final TestServerContext server, final TestServerConnection connection) {
		SkillCaster.Result[] results = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			fill(player);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JobWeapons.item(JobWeapons.def("excalibur"))));
			SkillCaster.Result lowTier = SkillCaster.tryCast(player, 0);
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.WARRIOR, 4));
			SkillCaster.Result lowLevel = SkillCaster.tryCast(player, 0);
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.ROGUE, 2));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JobWeapons.item(JobWeapons.def("iron_greatsword"))));
			SkillCaster.Result wrongClass = SkillCaster.tryCast(player, 1);
			// basic attacks still work with a locked weapon
			LivingEntity target = spawnTarget(player.level(), 0.5, -60, -1.5);
			player.resetAttackStrengthTicker();
			player.attack(target);
			boolean hit = target.getHealth() < target.getMaxHealth();
			target.discard();
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.WARRIOR, 2));
			JobStats.refresh(player);
			return new SkillCaster.Result[] {lowTier, lowLevel, wrongClass, hit ? SkillCaster.Result.OK : SkillCaster.Result.NO_SKILL};
		});
		require(results[0] == SkillCaster.Result.LOW_TIER, "excalibur at tier 2 should be LOW_TIER, got " + results[0]);
		require(results[1] == SkillCaster.Result.LOW_LEVEL, "excalibur at level 25 should be LOW_LEVEL, got " + results[1]);
		require(results[2] == SkillCaster.Result.WRONG_CLASS, "a rogue with a warrior weapon should be WRONG_CLASS, got " + results[2]);
		require(results[3] == SkillCaster.Result.OK, "basic attack with someone else's class weapon did no damage");
		MinecraftMode.LOGGER.info("[job] requirements: tier, level and class block skills; basic attacks still work");
	}

	private static void checkEngravingStacks(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		float[] result = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ItemStack sword = new ItemStack(JobWeapons.item(JobWeapons.def("iron_greatsword")));
			sword.set(ModDataComponents.ENGRAVINGS, new Engravings(List.of("wide_swing", "wide_swing", "wide_swing"), 1));
			player.setItemInHand(InteractionHand.MAIN_HAND, sword);
			float splash = JobWeapons.activeTotals(player).get(EngraveStat.SPLASH);
			LivingEntity main = spawnTarget(player.level(), 0.5, -60, -1.5);
			LivingEntity side = spawnTarget(player.level(), 0.5, -60, -5.5);
			player.resetAttackStrengthTicker();
			player.attack(main);
			float sideHealth = side.getHealth();
			main.discard();
			side.discard();
			return new float[] {splash, sideHealth};
		});
		require(result[0] == 4.5F, "three Wide Swing lines should add up to 4.5 blocks, got " + result[0]);
		require(result[1] < 400.0F, "Wide Swing x3 should splash a target 4 blocks behind the one hit");
		MinecraftMode.LOGGER.info("[job] engravings stack: Wide Swing x3 = 4.5 block splash, hit a target 4 blocks away");
	}

	private static void checkEngravingTable(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		int[] counts = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.getInventory().clearContent();
			player.getInventory().add(new ItemStack(ModItems.ESSENCE, 10));
			player.getInventory().add(new ItemStack(ModItems.CONDENSED_ESSENCE, 1));
			Wallet.add(player, 500);
			int wallet = Wallet.balance(player);
			EngravingMenu menu = new EngravingMenu(0, player.getInventory(), ContainerLevelAccess.NULL);
			ItemStack weapon = new ItemStack(JobWeapons.item(JobWeapons.def("gladiator_longsword")));
			menu.getSlot(0).set(weapon);
			require(menu.offer(0) != null && menu.offer(0).job() == JobClass.WARRIOR, "the table should offer warrior engravings");
			int cost = menu.engraveCost();
			int coins = menu.engraveCoins();
			require(coins > 0, "engraving should also cost coins");
			require(menu.clickMenuButton(player, 0), "engraving offer 0 failed");
			require(Wallet.balance(player) == wallet - coins, "engraving should take " + coins + " copper from the wallet");
			int lines = JobWeapons.engravings(menu.weapon()).lines().size();
			int left = EngravingMenu.essence(player.getInventory());
			require(menu.clickMenuButton(player, EngravingMenu.BUTTON_REMOVE), "removing a line failed");
			int afterRemove = JobWeapons.engravings(menu.weapon()).lines().size();
			int leftAfterRemove = EngravingMenu.essence(player.getInventory());
			menu.getSlot(0).set(ItemStack.EMPTY);
			return new int[] {cost, lines, left, afterRemove, leftAfterRemove};
		});
		require(counts[0] == 8, "tier 2 first line should cost 8 essence, got " + counts[0]);
		require(counts[1] == 1 && counts[2] == 19 - 8, "engraving: lines " + counts[1] + ", essence left " + counts[2]);
		require(counts[3] == 0 && counts[4] == 11 - 4, "removing: lines " + counts[3] + ", essence left " + counts[4]);

		// the real screen, with a weapon that has two lines
		server.runCommand("setblock 0 -60 -3 minecraft_mode:engraving_table");
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.getInventory().add(new ItemStack(ModItems.CONDENSED_ESSENCE, 5));
			player.openMenu(new SimpleMenuProvider(
				(id, inventory, p) -> new EngravingMenu(id, inventory, ContainerLevelAccess.create(p.level(), new BlockPos(0, -60, -3))),
				Component.translatable("container.minecraft_mode.engraving_table")
			));
			ItemStack weapon = new ItemStack(JobWeapons.item(JobWeapons.def("flamberge")));
			weapon.set(ModDataComponents.ENGRAVINGS, new Engravings(List.of("wide_swing", "crush"), 7));
			((EngravingMenu)player.containerMenu).getSlot(0).set(weapon);
			player.containerMenu.broadcastChanges();
		});
		context.waitForScreen(com.minecraftmode.client.job.EngravingScreen.class);
		context.waitTicks(5);
		context.takeScreenshot("engraving_table");
		context.setScreen(() -> null);
		context.waitTicks(3);
		server.runCommand("setblock 0 -60 -3 minecraft:air");
		server.runOnServer(s -> connection.getServerPlayer().getInventory().clearContent());
		MinecraftMode.LOGGER.info("[job] engraving table: line costs 8 essence (condensed gives change), removing costs 4");
	}

	private static void checkBasicShot(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.snapTo(0.5, -60, 0.5, 180.0F, 0.0F);
			player.connection.teleport(0.5, -60, 0.5, 180.0F, 0.0F);
			spawnTarget(player.level(), 0.5, -60, -5.5);
			ItemStack staff = new ItemStack(JobWeapons.item(JobWeapons.def("apprentice_staff")));
			player.setItemInHand(InteractionHand.MAIN_HAND, staff);
		});
		context.waitTicks(3);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.setXRot(10.0F);
			BasicAttacks.shoot(player, player.getMainHandItem(), JobWeapons.def(player.getMainHandItem()));
		});
		context.waitTicks(15);
		float health = server.computeOnServer(s -> targets(connection.getServerPlayer()).stream().map(LivingEntity::getHealth).min(Float::compare).orElse(400.0F));
		require(health < 400.0F, "the staff's magic bolt did not hit the target");
		server.runCommand("kill @e[tag=" + TARGET + "]");
		MinecraftMode.LOGGER.info("[job] staff basic shot hit a target 6 blocks away");
	}

	/** Guild lines that sell to the visitor (buyback lines, which take class gear, left out). */
	private static List<ShopOffers.Trade> sales(final List<ShopOffers.Trade> trades) {
		return trades.stream().filter(t -> ClassGear.of(new ItemStack(t.cost())) == null).toList();
	}

	private static void checkGuildAndDrops(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		int[] offers = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			int warriorTier2 = sales(ShopOffers.trades(ShopType.GUILD, player)).size();
			JobData saved = JobProgression.get(player);
			JobProgression.set(player, saved.withJob(JobClass.NONE, 0));
			int none = sales(ShopOffers.trades(ShopType.GUILD, player)).size();
			JobProgression.set(player, saved);
			return new int[] {warriorTier2, none};
		});
		// the guild buys any class gear back, own class included (one line per kind, whatever its components)
		String buyback = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			WeaponDef mage = JobWeapons.all().stream().filter(d -> d.job() == JobClass.MAGE).findFirst().orElseThrow();
			WeaponDef warrior = JobWeapons.all().stream().filter(d -> d.job() == JobClass.WARRIOR).findFirst().orElseThrow();
			ItemStack named = new ItemStack(JobWeapons.item(mage));
			named.set(DataComponents.CUSTOM_NAME, Component.literal("Loot"));
			List<ItemStack> before = player.getInventory().getNonEquipmentItems().stream().map(ItemStack::copy).toList();
			player.getInventory().add(named.copy());
			player.getInventory().add(new ItemStack(JobWeapons.item(mage)));
			player.getInventory().add(new ItemStack(JobWeapons.item(warrior)));
			List<ShopOffers.Trade> trades = ShopOffers.trades(ShopType.GUILD, player);
			List<ShopOffers.Trade> buys = trades.stream().filter(t -> ClassGear.of(new ItemStack(t.cost())) != null).toList();
			for (int slot = 0; slot < before.size(); slot++) {
				player.getInventory().setItem(slot, before.get(slot));
			}
			List<ShopOffers.Trade> mageBuys = buys.stream().filter(t -> t.cost().asItem() == JobWeapons.item(mage)).toList();
			long warriorBuys = buys.stream().filter(t -> t.cost().asItem() == JobWeapons.item(warrior)).count();
			if (mageBuys.size() != 1 || warriorBuys != 1) {
				return "a warrior carrying two mage weapons and a warrior weapon should see one buyback line each, got " + mageBuys.size() + " / " + warriorBuys;
			}
			ShopOffers.Trade sale = mageBuys.getFirst();
			int paid = sale.resultCount() * com.minecraftmode.economy.Wallet.value(sale.result().asItem());
			int expected = GearShop.buybackPrice(ClassGear.of(mage));
			if (Math.abs(paid - expected) > expected / 4 + 1 || paid >= GearShop.price(ClassGear.of(mage))) {
				return "the buyback pays " + paid + " copper, expected about " + expected;
			}
			return sale.toOffer().satisfiedBy(named, ItemStack.EMPTY) ? "" : "a named mage weapon should satisfy the buyback";
		});
		require(buyback.isEmpty(), buyback);
		// one weapon + one armor piece per bracket up to the next bracket (Lv 25 -> 10, 20, 30); no class: the Lv 10 items of every class
		int warriorItems = 0;
		for (int bracket = 10; bracket <= 30; bracket += 10) {
			warriorItems += GearIndex.shopItems(JobClass.WARRIOR, bracket).size();
		}
		int starterItems = 0;
		for (JobClass job : JobClass.PLAYABLE) {
			starterItems += GearIndex.shopItems(job, 10).size();
		}
		require(warriorItems == 6 && starterItems == 2 * JobClass.PLAYABLE.size(), "expected 2 shop items per class and bracket, got " + warriorItems + " / " + starterItems);
		require(offers[0] == warriorItems + 2, "a Lv 25 warrior should see " + warriorItems + " gear offers + 2 extras, got " + offers[0]);
		require(offers[1] == starterItems + 2, "players without a class should see " + starterItems + " starter offers + 2 extras, got " + offers[1]);

		// a boss kill: condensed essence, golem core, double experience
		server.runCommand("summon minecraft_mode:mythril_golem 0.5 -60 -6.5 {NoAI:1b}");
		context.waitTicks(3);
		int[] drops = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			Mob golem = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(12), e -> e.getType() == com.minecraftmode.registry.ModEntities.MYTHRIL_GOLEM).getFirst();
			int before = JobProgression.get(player).exp() + totalExp(JobProgression.get(player));
			golem.hurtServer(player.level(), player.damageSources().playerAttack(player), 10000.0F);
			int after = JobProgression.get(player).exp() + totalExp(JobProgression.get(player));
			return new int[] {after - before};
		});
		context.waitTicks(5);
		int[] items = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			int condensed = 0;
			int core = 0;
			for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(16))) {
				if (item.getItem().is(ModItems.CONDENSED_ESSENCE)) {
					condensed += item.getItem().getCount();
				}
				if (item.getItem().is(ModItems.GOLEM_CORE)) {
					core += item.getItem().getCount();
				}
			}
			return new int[] {condensed, core};
		});
		require(drops[0] == 300, "a 150 health boss should give 300 class exp, got " + drops[0]);
		require(items[0] >= 2 && items[1] == 1, "mythril golem drops: condensed essence " + items[0] + ", golem core " + items[1]);
		server.runCommand("kill @e[type=minecraft:item]");
		MinecraftMode.LOGGER.info("[job] guild offers follow class/tier; golem kill: 300 exp, {} condensed essence, golem core", items[0]);
	}

	private static int totalExp(final JobData data) {
		int total = 0;
		for (int level = 1; level < data.level(); level++) {
			total += JobProgression.expToNext(level);
		}
		return total;
	}

	private static void jobScreen(final ClientGameTestContext context, final TestServerContext server) {
		context.getInput().pressKey(JobKeys.OPEN_SCREEN);
		context.waitForScreen(JobScreen.class);
		context.waitTicks(3);
		context.takeScreenshot("job_screen");
		context.setScreen(() -> null);
		context.waitTicks(2);
	}

	// ------------------------------------------------------------------ every skill

	private static void castEverySkill(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("gamemode creative @p");
		server.runCommand("time set midnight");
		int casts = 0;
		Set<String> screenshots = Set.of("excalibur", "gate_of_babylon", "megumin_staff", "battleship_cannon", "kamish_wrath");
		for (JobClass job : JobClass.PLAYABLE) {
			for (WeaponDef def : JobWeapons.of(job)) {
				server.runOnServer(s -> {
					ServerPlayer player = connection.getServerPlayer();
					JobProgression.set(player, JobProgression.get(player).withJob(job, 4).withProgress(100, 0));
					player.connection.teleport(0.5, -60, 0.5, 180.0F, 5.0F);
					player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JobWeapons.item(def)));
					JobStats.refresh(player);
					ServerLevel level = player.level();
					if (targets(player).size() < 3) {
						spawnTarget(level, 0.5, -60, -4.5);
						spawnTarget(level, 2.5, -60, -6.5);
						spawnTarget(level, -1.5, -60, -8.5);
					}
				});
				context.waitTicks(2);
				for (int slot = 0; slot < def.skills().size(); slot++) {
					final int index = slot;
					Skill skill = def.skills().get(slot);
					SkillCaster.Result result = server.computeOnServer(s -> {
						ServerPlayer player = connection.getServerPlayer();
						fill(player);
						return SkillCaster.tryCast(player, index);
					});
					require(result == SkillCaster.Result.OK, skill.id() + " could not be cast: " + result);
					casts++;
					if (index == 0 && screenshots.contains(def.id())) {
						context.waitTicks(3);
						context.takeScreenshot("skill_" + def.id());
					}
					context.waitTicks(4);
				}
				context.waitTicks(20);
				server.runOnServer(s -> {
					ServerPlayer player = connection.getServerPlayer();
					for (Entity e : player.level().getEntitiesOfClass(Entity.class, player.getBoundingBox().inflate(48), e -> e != player)) {
						if (!(e instanceof LivingEntity living) || e.entityTags().contains(Actions.SUMMON_TAG) || living.getHealth() < 60.0F) {
							e.discard();
						}
					}
					player.removeAllEffects();
					player.setHealth(player.getMaxHealth());
				});
			}
		}
		server.runCommand("kill @e[tag=" + TARGET + "]");
		server.runCommand("time set noon");
		require(casts == JobWeapons.skillCount(), "cast " + casts + " of " + JobWeapons.skillCount() + " skills");
		MinecraftMode.LOGGER.info("[job] cast all {} skills of all 161 weapons without errors", casts);
	}

	// ------------------------------------------------------------------ helpers

	/** Soul Reaper and Hunter: their tier passives are stat lines that add up, and Flash Step and En work. */
	private static void checkNewClasses(final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			JobData saved = JobProgression.get(player);
			ItemStack held = player.getMainHandItem().copy();
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

			JobProgression.set(player, saved.withJob(JobClass.SHINIGAMI, 4).withProgress(80, 0));
			fill(player);
			JobStats.refresh(player);
			GearStats.invalidate(player);
			EngraveTotals reaper = GearStats.of(player);
			require(reaper.get(EngraveStat.BOSS_DAMAGE) >= 15 && reaper.get(EngraveStat.SKILL_DAMAGE) >= 12 && reaper.get(EngraveStat.DODGE) >= 5,
				"Soul Reaper tier passives should add boss damage, skill damage and dodge");
			require(reaper.get(EngraveStat.COOLDOWN) >= 15 + 12, "Mugetsu (15%) plus the Lv 80 bonus (12%) should cut cooldowns, got " + reaper.get(EngraveStat.COOLDOWN));
			player.teleportTo(20.5, -60, 20.5);
			player.snapTo(20.5, -60, 20.5, 0.0F, 0.0F);
			Vec3 before = player.position();
			ClassAbilities.use(player);
			double moved = player.position().distanceTo(before);
			require(moved >= 5.0, "Flash Step should move the player forward, moved " + moved);
			require(player.hasEffect(MobEffects.SPEED), "Flash Step should give Swiftness");

			JobProgression.set(player, saved.withJob(JobClass.HUNTER, 3).withProgress(50, 0));
			fill(player);
			JobStats.refresh(player);
			GearStats.invalidate(player);
			EngraveTotals hunter = GearStats.of(player);
			require(hunter.get(EngraveStat.DOUBLE_STRIKE) >= 15 && hunter.get(EngraveStat.CRIT_CHANCE) >= 10 && hunter.get(EngraveStat.DAMAGE_REDUCTION) >= 5,
				"Hunter tier passives should add double strike, crit chance and damage reduction");
			require(hunter.get(EngraveStat.SKILL_DAMAGE) < 20, "Limitation and Vow belongs to tier 4 only");
			require(hunter.get(EngraveStat.CRIT_DAMAGE) >= 20, "the Lv 50 hunter bonus should add 20% crit damage, got " + hunter.get(EngraveStat.CRIT_DAMAGE));
			LivingEntity hidden = spawnTarget(player.level(), player.getX() + 12, player.getY(), player.getZ());
			ClassAbilities.use(player);
			boolean glowing = hidden.hasEffect(MobEffects.GLOWING);
			hidden.discard();
			require(glowing, "En should reveal enemies within 32 blocks");

			player.removeAllEffects();
			JobProgression.set(player, saved);
			JobStats.refresh(player);
			GearStats.invalidate(player);
			player.setItemInHand(InteractionHand.MAIN_HAND, held);
			player.teleportTo(0.5, -60, 0.5);
			return String.format(java.util.Locale.ROOT, "flash step moved %.1f blocks, reaper cooldown -%.0f%%, hunter double strike %.0f%%",
				moved, reaper.get(EngraveStat.COOLDOWN), hunter.get(EngraveStat.DOUBLE_STRIKE));
		});
		MinecraftMode.LOGGER.info("[job] soul reaper and hunter: {}", report);
	}

	/**
	 * Movement never kills its caster: a blink looking straight up climbs at most {@link Movement#MAX_RISE} and stops at the edge
	 * of a platform 10 blocks up, and a leap that rises about 13 blocks lands without fall damage (and only after leaving the ground).
	 */
	private static void checkSafeMovement(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("fill 40 -51 40 44 -51 46 minecraft:stone");
		context.waitTicks(2);
		JobData[] saved = new JobData[1];
		String blink = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			saved[0] = JobProgression.get(player);
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.SHINIGAMI, 4).withProgress(80, 0));
			fill(player);
			JobStats.refresh(player);
			player.setHealth(player.getMaxHealth());
			player.teleportTo(42.5, -50, 40.5);
			player.snapTo(42.5, -50, 40.5, 0.0F, -90.0F);
			Vec3 end = Movement.end(player, player.getLookAngle(), 14.0, Movement.rise(player, 14.0));
			require(end.y - player.getY() <= Movement.MAX_RISE + 1.0E-6, "a blink looking up should climb at most " + Movement.MAX_RISE + ", climbed " + (end.y - player.getY()));
			require(end.z > 44.0 && end.z < 47.5, "a blink should stop at the platform edge (z 40..47), ended at z " + end.z);
			ClassAbilities.use(player);
			return String.format(java.util.Locale.ROOT, "blink up ends %.1f above at z %.1f, flash step to %.1f %.1f", end.y - (-50), end.z, player.getY(), player.getZ());
		});
		context.waitTicks(40);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			require(player.getHealth() >= player.getMaxHealth(), "Flash Step off a platform edge should not hurt, health " + player.getHealth());
			require(player.getY() >= -50.01, "Flash Step should leave the player on the platform, y " + player.getY());
		});
		String walls = checkWalls(context, server, connection);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.teleportTo(30.5, -60, 30.5);
			player.snapTo(30.5, -60, 30.5, 0.0F, 0.0F);
		});
		context.waitTicks(5);
		AtomicBoolean landed = new AtomicBoolean();
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.setDeltaMovement(0.0, 1.46, 0.0);
			player.syncVelocity = true;
			Movement.guardFall(player, () -> landed.set(true));
		});
		context.waitTicks(12);
		double high = context.computeOnClient(minecraft -> minecraft.player.getY());
		require(high > -55.0, "the test leap should rise, client y " + high);
		require(!landed.get(), "a leap should not land before it comes down");
		context.waitTicks(60);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			require(landed.get(), "the leap should have landed");
			require(player.getHealth() >= player.getMaxHealth(), "a leap back to its start height should not hurt, health " + player.getHealth());
			JobProgression.set(player, saved[0]);
			JobStats.refresh(player);
			player.removeAllEffects();
			player.teleportTo(0.5, -60, 0.5);
		});
		server.runCommand("fill 40 -51 40 44 -51 46 minecraft:air");
		MinecraftMode.LOGGER.info("[job] safe movement: {}, {}, leap peak y {}", blink, walls, high);
	}

	/**
	 * Blinks never go through what blocks them: in a 2-high corridor a blink looking straight up stays on the floor and stops in
	 * front of iron bars, a step it cannot climb under the low ceiling stops it, and Flash Step never leaves the player inside blocks.
	 */
	private static String checkWalls(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		// 3 wide, 2 high, along +z from z 59; iron bars across it at z 66 (their bar is z 66.44..66.56)
		server.runCommand("fill 59 -60 59 63 -58 73 minecraft:stone");
		server.runCommand("fill 60 -60 59 62 -59 73 minecraft:air");
		server.runCommand("fill 60 -60 66 62 -59 66 minecraft:iron_bars");
		context.waitTicks(2);
		String bars = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			fill(player);
			player.setHealth(player.getMaxHealth());
			player.teleportTo(61.5, -60, 60.5);
			player.snapTo(61.5, -60, 60.5, 0.0F, -90.0F);
			Vec3 end = Movement.end(player, player.getLookAngle(), 14.0, Movement.rise(player, 14.0));
			// the corridor leaves 0.2 blocks of headroom (ceiling at y -58, the player is 1.8 tall)
			require(end.y >= -60.0 && end.y + player.getBbHeight() <= -58.0 + 1.0E-6, "a blink looking up should stay under the ceiling, ended at y " + end.y);
			require(end.z > 63.0 && end.z < 66.44, "a blink should stop in front of the iron bars, ended at z " + end.z);
			// shadowstep's "behind the target" spot: never across the bars, fine along the open corridor
			require(!Movement.clearLine(player, new Vec3(61.5, -60, 65.6), new Vec3(61.5, -60, 67.2)), "a step behind a target should not cross iron bars");
			require(Movement.clearLine(player, new Vec3(61.5, -60, 61.0), new Vec3(61.5, -60, 64.0)), "a step along an open corridor should be clear");
			ClassAbilities.use(player);
			return String.format(java.util.Locale.ROOT, "under a ceiling stopped at z %.2f before bars", player.getZ());
		});
		context.waitTicks(20);
		server.runOnServer(s -> requireFree(connection.getServerPlayer(), 66.44));

		// a one-block step the ceiling leaves no room to climb
		server.runCommand("fill 60 -60 66 62 -59 66 minecraft:air");
		server.runCommand("fill 60 -60 63 62 -60 63 minecraft:stone");
		context.waitTicks(2);
		String step = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			fill(player);
			player.teleportTo(61.5, -60, 60.5);
			player.snapTo(61.5, -60, 60.5, 0.0F, 0.0F);
			Vec3 end = Movement.end(player, player.getLookAngle(), 14.0, Movement.rise(player, 14.0));
			require(Math.abs(end.y + 60.0) < 1.0E-6 && end.z < 62.71, "a blink should stop before a step it cannot climb under a low ceiling, ended at y "
				+ end.y + " z " + end.z);
			ClassAbilities.use(player);
			return String.format(java.util.Locale.ROOT, "before a step at z %.2f", player.getZ());
		});
		context.waitTicks(20);
		server.runOnServer(s -> requireFree(connection.getServerPlayer(), 62.71));
		server.runCommand("fill 59 -60 59 63 -58 73 minecraft:air");
		return bars + ", " + step;
	}

	/** The player is still in the corridor, short of {@code maxZ}, not inside any block and unhurt. */
	private static void requireFree(final ServerPlayer player, final double maxZ) {
		require(player.level().noCollision(player), "Flash Step left the player inside blocks at " + player.position());
		require(player.getY() < -59.0 && player.getZ() < maxZ, "Flash Step went through a wall or ceiling to " + player.position());
		require(player.getHealth() >= player.getMaxHealth(), "Flash Step in a corridor should not hurt, health " + player.getHealth());
	}

	private static void fill(final ServerPlayer player) {
		JobData data = JobProgression.get(player);
		JobProgression.set(player, data.withMana(JobStats.maxMana(player)).withCooldowns(Map.of()));
	}

	private static LivingEntity spawnTarget(final ServerLevel level, final double x, final double y, final double z) {
		var husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		require(husk != null, "could not create a husk");
		husk.snapTo(x, y, z, 0.0F, 0.0F);
		husk.setNoAi(true);
		husk.setPersistenceRequired();
		husk.addTag(TARGET);
		husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400.0);
		husk.setHealth(400.0F);
		level.addFreshEntity(husk);
		return husk;
	}

	private static List<LivingEntity> targets(final Player player) {
		return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(32), e -> e.entityTags().contains(TARGET));
	}

	private static void require(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
