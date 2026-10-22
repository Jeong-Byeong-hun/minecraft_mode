package com.minecraftmode.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.vfx.LookEdges;
import com.minecraftmode.client.vfx.LookRanges;
import com.minecraftmode.client.job.JobKeys;
import com.minecraftmode.client.mixin.AbstractContainerScreenAccessor;
import com.minecraftmode.client.job.JobScreen;
import com.minecraftmode.client.job.TrainerScreen;
import com.minecraftmode.economy.ShopOffers;
import com.minecraftmode.economy.ShopType;
import com.minecraftmode.economy.Wallet;
import com.minecraftmode.city.DummyMeter;
import com.minecraftmode.entity.ClassTrainer;
import com.minecraftmode.entity.TrainingDummy;
import com.minecraftmode.entity.named.NamedMob;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobEvents;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.JobStats;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.engrave.EngraveTotals;
import com.minecraftmode.job.engrave.Engraving;
import com.minecraftmode.job.engrave.EngravingMenu;
import com.minecraftmode.job.engrave.Engravings;
import com.minecraftmode.job.gear.ArmorOptions;
import com.minecraftmode.job.gear.ClassAbilities;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.GearSlot;
import com.minecraftmode.job.gear.GearStats;
import com.minecraftmode.job.quest.QuestDef;
import com.minecraftmode.job.quest.QuestService;
import com.minecraftmode.job.quest.Quests;
import com.minecraftmode.job.skill.Actions;
import com.minecraftmode.job.skill.HitArea;
import com.minecraftmode.job.skill.CombatHooks;
import com.minecraftmode.job.skill.Movement;
import com.minecraftmode.job.skill.Skill;
import com.minecraftmode.job.skill.SkillAction;
import com.minecraftmode.job.skill.SkillCaster;
import com.minecraftmode.job.skill.SkillContext;
import com.minecraftmode.job.skill.SkillLook;
import com.minecraftmode.job.skill.SkillProjectile;
import com.minecraftmode.job.skill.SkillVfx;
import com.minecraftmode.job.weapon.Archetype;
import com.minecraftmode.job.weapon.BasicAttacks;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.ProjectileStyle;
import com.minecraftmode.job.weapon.WeaponDef;
import com.minecraftmode.loot.GearIndex;
import com.minecraftmode.loot.GearShop;
import com.minecraftmode.mixin.LivingEntityAccessor;
import com.minecraftmode.network.OpenTrainerPayload;
import com.minecraftmode.network.QuestActionPayload;
import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.registry.ModEffects;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
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
		if (TestSelection.skip(this)) {
			return;
		}
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
			checkFullSwings(server, connection);
			checkNewClasses(server, connection);
			checkSafeMovement(context, server, connection);
			checkEngravingStacks(context, server, connection);
			checkEngravingTable(context, server, connection);
			checkBasicShot(context, server, connection);
			checkGuildAndDrops(context, server, connection);
			jobScreen(context, server, connection);
			weaponTooltip(context, server, connection);
			searchTooltips(context);
			checkPointBlank(context, server, connection);
			checkSkillLooks(context, server, connection);
			checkLookRanges();
			checkReaperLooks(context, server, connection);
			checkArcherLooks(context, server, connection);
			checkMageLooks(context, server, connection);
			checkPirateLooks(context, server, connection);
			checkWarriorLooks(context, server, connection);
			checkRogueLooks(context, server, connection);
			checkHunterLooks(context, server, connection);
			checkVolleys(context, server, connection);
			checkCritsAndDummy(server, connection);
			checkBoosts(server, connection);
			checkProjectileImmunities(context, server, connection);
			checkLockedAim(context, server, connection);
			castEverySkill(context, server, connection);
			WorldClose.prepare(context, server);
		}
	}

	// ------------------------------------------------------------------ content and assets

	private static void checkContent(final ClientGameTestContext context) {
		require(JobWeapons.all().size() == 182, "expected 182 class weapons, got " + JobWeapons.all().size());
		int skills = 0;
		for (JobClass job : JobClass.PLAYABLE) {
			int[] perTier = new int[5];
			for (WeaponDef def : JobWeapons.of(job)) {
				perTier[def.tier()]++;
				int need = def.tier() == 4 ? 4 : 3;
				require(def.skills().size() >= need, def.id() + " has " + def.skills().size() + " skills");
				skills += def.skills().size();
			}
			require(perTier[1] == 3 && perTier[2] == 7 && perTier[3] == 7 && perTier[4] == 9,
				job.id() + " weapons per tier 3/7/7/9, got " + perTier[1] + "/" + perTier[2] + "/" + perTier[3] + "/" + perTier[4]);
			long engravings = java.util.Arrays.stream(Engraving.values()).filter(e -> e.job() == job).count();
			require(engravings >= 10, job.id() + " has only " + engravings + " engravings");
			for (GearSlot slot : GearSlot.ARMOR) {
				// 0.5.24: every armor slot offers at least ten engravings and ten random options
				int pool = Engraving.pool(job, slot).size();
				require(pool >= Engraving.MIN_ARMOR_POOL, job.id() + " " + slot + " has only " + pool + " armor engravings");
				require(ArmorOptions.pool(slot).size() >= ArmorOptions.MIN_POOL, slot + " has only " + ArmorOptions.pool(slot).size() + " armor options");
			}
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
		MinecraftMode.LOGGER.info("[job] 182 weapons (3/7/7/9 per class), {} skills, textures/models/lang (en+ko) for all", skillTotal);
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
			require(maxHealth == 20.0 + 4 + 1 + JobClass.WARRIOR.vitalityHealth(),
				"warrior tier 1 at level 10 should have 20 + 4 (Iron Body) + 1 (level) + 4 (melee vitality) health, got " + maxHealth);
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

	/**
	 * A fully charged swing of a class weapon lands through the target's hurt cooldown (claws swing faster than its half second, and a
	 * double strike or skill hit no longer swallows the next swing), while an uncharged one still bounces off it.
	 */
	private static void checkFullSwings(final TestServerContext server, final TestServerConnection connection) {
		float[] dealt = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ItemStack held = player.getMainHandItem().copy();
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JobWeapons.item(JobWeapons.def("phantom_thief_claws"))));
			LivingEntity target = spawnTarget(player.level(), 0.5, -60, -1.5);
			LivingEntityAccessor swing = (LivingEntityAccessor)player;
			float before = target.getHealth();
			swing.minecraftMode$setAttackStrengthTicker(100);
			player.attack(target);
			float first = target.getHealth();
			// same tick, deep inside the hurt cooldown the first hit started
			swing.minecraftMode$setAttackStrengthTicker(100);
			player.attack(target);
			float second = target.getHealth();
			player.resetAttackStrengthTicker();
			player.attack(target);
			float third = target.getHealth();
			target.discard();
			player.setItemInHand(InteractionHand.MAIN_HAND, held);
			return new float[] {before - first, first - second, second - third};
		});
		require(dealt[0] > 0.0F, "a fully charged claw swing should hurt the target");
		require(dealt[1] > 0.0F, "a second fully charged swing inside the hurt cooldown should land, dealt " + dealt[1]);
		require(dealt[2] == 0.0F, "an uncharged swing inside the hurt cooldown should still bounce, dealt " + dealt[2]);
		MinecraftMode.LOGGER.info("[job] full swings land through the hurt cooldown ({} then {}), uncharged spam bounces", dealt[0], dealt[1]);
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
			List<Engraving> offered = List.of(menu.offer(0), menu.offer(1), menu.offer(2));
			require(menu.clickMenuButton(player, EngravingMenu.BUTTON_REMOVE), "removing a line failed");
			require(offered.equals(List.of(menu.offer(0), menu.offer(1), menu.offer(2))), "removing a line should keep the offers, had " + offered);
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

	/**
	 * Point-blank hits: a magic bolt fired from inside a monster pressed against the caster, a lightning aimed at a monster right
	 * in front (used to land on the ground behind it), and a Getsuga Tensho over a spider (the wave is as wide as its slash).
	 */
	private static void checkPointBlank(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		LivingEntity[] pressed = new LivingEntity[1];
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.snapTo(0.5, -60, 0.5, 180.0F, 0.0F);
			player.connection.teleport(0.5, -60, 0.5, 180.0F, 0.0F);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JobWeapons.item(JobWeapons.def("apprentice_staff"))));
			pressed[0] = spawnTarget(player.level(), 0.5, -60, 0.35);
			require(pressed[0].getBoundingBox().contains(player.getEyePosition()), "the pressed husk should hold the caster's eye");
			BasicAttacks.shoot(player, player.getMainHandItem(), JobWeapons.def(player.getMainHandItem()));
		});
		context.waitTicks(5);
		float bolt = server.computeOnServer(s -> pressed[0].getHealth());
		require(bolt < 400.0F, "a magic bolt fired from inside a pressed monster went through it");
		server.runCommand("kill @e[tag=" + TARGET + "]");

		LivingEntity[] front = new LivingEntity[1];
		boolean aimed = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.MAGE, 4).withProgress(100, 0));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JobWeapons.item(JobWeapons.def("ice_lightning_wand"))));
			JobStats.refresh(player);
			player.connection.teleport(0.5, -60, 0.5, 180.0F, 0.0F);
			front[0] = spawnTarget(player.level(), 0.5, -60, -1.0);
			return SkillContext.aimed(player, 14.0, e -> e == front[0]) == front[0];
		});
		require(aimed, "the husk right in front is not the aimed target");
		context.waitTicks(2);
		SkillCaster.Result cast = server.computeOnServer(s -> {
			fill(connection.getServerPlayer());
			return SkillCaster.tryCast(connection.getServerPlayer(), 1);
		});
		require(cast == SkillCaster.Result.OK, "Thunder Bolt could not be cast: " + cast);
		context.waitTicks(3);
		float struck = server.computeOnServer(s -> front[0].getHealth());
		require(struck < 400.0F, "Thunder Bolt aimed at a husk right in front landed elsewhere");
		server.runCommand("kill @e[tag=" + TARGET + "]");

		LivingEntity[] spider = new LivingEntity[1];
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.SHINIGAMI, 4).withProgress(100, 0));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JobWeapons.item(JobWeapons.def("true_zangetsu"))));
			JobStats.refresh(player);
			player.connection.teleport(0.5, -60, 0.5, 180.0F, 0.0F);
			var mob = EntityTypes.SPIDER.create(player.level(), EntitySpawnReason.COMMAND);
			require(mob != null, "could not create a spider");
			mob.snapTo(0.5, -60, -3.5, 0.0F, 0.0F);
			mob.setNoAi(true);
			mob.addTag(TARGET);
			player.level().addFreshEntity(mob);
			spider[0] = mob;
		});
		context.waitTicks(2);
		cast = server.computeOnServer(s -> {
			fill(connection.getServerPlayer());
			return SkillCaster.tryCast(connection.getServerPlayer(), 0);
		});
		require(cast == SkillCaster.Result.OK, "Getsuga Tensho could not be cast: " + cast);
		context.waitTicks(8);
		boolean cut = server.computeOnServer(s -> !spider[0].isAlive() || spider[0].getHealth() < spider[0].getMaxHealth());
		require(cut, "Getsuga Tensho passed over a spider in its slash");
		server.runCommand("kill @e[tag=" + TARGET + "]");

		// Twin Getsuga's waves fly side by side: a monster straight ahead 15 blocks out takes both (they used to fan out 20°, missing it from 7 on)
		LivingEntity[] far = new LivingEntity[1];
		server.runOnServer(s -> far[0] = spawnTarget(connection.getServerPlayer().level(), 0.5, -60, -14.5));
		context.waitTicks(2);
		float farBefore = server.computeOnServer(s -> far[0].getHealth());
		cast = server.computeOnServer(s -> {
			fill(connection.getServerPlayer());
			return SkillCaster.tryCast(connection.getServerPlayer(), 1);
		});
		require(cast == SkillCaster.Result.OK, "Twin Getsuga could not be cast: " + cast);
		context.waitTicks(15);
		float farTaken = farBefore - server.computeOnServer(s -> far[0].getHealth());
		// one wave = skill power x the shot multiplier of twin_getsuga (1.24 since the 0.4.4 balance pass)
		float wave = server.computeOnServer(s -> (float)(JobWeapons.def("true_zangetsu").skillPower() * 1.24));
		require(farTaken >= wave * 1.5F, "both Twin Getsuga waves should hit a monster 15 blocks ahead, it took " + farTaken + " of " + wave + " per wave");
		server.runCommand("kill @e[tag=" + TARGET + "]");
		MinecraftMode.LOGGER.info("[job] point blank: bolt from inside a husk, lightning on a husk in front and Getsuga over a spider all hit");
	}

	/**
	 * Drawn skill looks reach the client: Getsuga Tensho flies as a crescent (black off Tensa Zangetsu, blue off True Zangetsu) and
	 * Rasengan shows the sphere in the hand, then its burst. Screenshots from behind the caster.
	 */
	private static void checkSkillLooks(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("time set midnight");
		server.runCommand("title @a clear");
		// the death puffs of the last check's targets lie along the waves' path
		context.waitTicks(40);
		for (String weapon : List.of("tensa_zangetsu", "true_zangetsu")) {
			server.runOnServer(s -> {
				ServerPlayer player = connection.getServerPlayer();
				JobProgression.set(player, JobProgression.get(player).withJob(JobClass.SHINIGAMI, 4).withProgress(100, 0));
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JobWeapons.item(JobWeapons.def(weapon))));
				JobStats.refresh(player);
				player.removeAllEffects();
				player.connection.teleport(0.5, -60, 0.5, 180.0F, 0.0F);
			});
			context.waitTicks(3);
			SkillCaster.Result cast = server.computeOnServer(s -> {
				fill(connection.getServerPlayer());
				return SkillCaster.tryCast(connection.getServerPlayer(), 0);
			});
			require(cast == SkillCaster.Result.OK, "Getsuga Tensho of " + weapon + " could not be cast: " + cast);
			context.waitTicks(2);
			// True Zangetsu's is the black-and-white one (0.5.36)
			SkillLook crescent = weapon.equals("true_zangetsu") ? SkillLook.GETSUGA_DUAL : SkillLook.GETSUGA;
			int crescents = context.computeOnClient(minecraft -> lookCount(minecraft, crescent));
			require(crescents > 0, "the client sees no Getsuga crescent after casting it with " + weapon);
			// hold the wave still and look at it from the side, then from behind
			server.runCommand("tick freeze");
			Vec3 wave = server.computeOnServer(s -> connection.getServerPlayer().level().getEntitiesOfClass(SkillProjectile.class,
				connection.getServerPlayer().getBoundingBox().inflate(32)).getFirst().position());
			lookShot(context, server, connection, wave.add(6.0, 0.4, 0.0), 90.0F, 0.0F, "look_getsuga_" + weapon + "_side");
			lookShot(context, server, connection, wave.add(0.0, 0.2, 4.5), 180.0F, 5.0F, "look_getsuga_" + weapon);
			if (weapon.equals("true_zangetsu")) {
				// the Fabulous preset draws blended types through order-independent transparency; the glow type crashed there until 0.5.36
				context.runOnClient(minecraft -> minecraft.options.improvedTransparency().set(true));
				context.waitTicks(5);
				lookShot(context, server, connection, wave.add(0.0, 0.2, 4.5), 180.0F, 5.0F, "look_getsuga_" + weapon + "_fabulous");
				context.runOnClient(minecraft -> minecraft.options.improvedTransparency().set(false));
				context.waitTicks(5);
			}
			server.runCommand("tick unfreeze");
			context.waitTicks(20);
		}

		int target = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.ROGUE, 4).withProgress(100, 0));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JobWeapons.item(JobWeapons.def("rasenshuriken"))));
			JobStats.refresh(player);
			player.connection.teleport(0.5, -60, 0.5, 180.0F, 0.0F);
			return spawnTarget(player.level(), 0.5, -60, -6.5).getId();
		});
		// the held sphere moves with its caster: seen through the target's eyes as the caster dashes in, later in first person
		context.waitTicks(3);
		context.runOnClient(minecraft -> {
			hideHud(minecraft, true);
			Entity eyes = minecraft.level.getEntity(target);
			require(eyes != null, "the client does not know the Rasengan target");
			minecraft.setCameraEntity(eyes);
		});
		context.waitTicks(3);
		SkillCaster.Result cast = server.computeOnServer(s -> {
			fill(connection.getServerPlayer());
			return SkillCaster.tryCast(connection.getServerPlayer(), 0);
		});
		require(cast == SkillCaster.Result.OK, "Rasengan could not be cast: " + cast);
		context.waitTicks(2);
		require(context.computeOnClient(minecraft -> lookCount(minecraft, SkillLook.RASENGAN)) > 0, "the client sees no Rasengan in the hand");
		context.takeScreenshot("look_rasengan");
		context.runOnClient(minecraft -> {
			hideHud(minecraft, false);
			minecraft.setCameraEntity(minecraft.player);
		});
		context.waitTicks(5);
		require(context.computeOnClient(minecraft -> lookCount(minecraft, SkillLook.RASENGAN_BURST)) > 0, "the client sees no Rasengan burst");
		server.runCommand("tick freeze");
		Vec3 burst = server.computeOnServer(s -> connection.getServerPlayer().level().getEntitiesOfClass(SkillVfx.class,
			connection.getServerPlayer().getBoundingBox().inflate(32)).getFirst().position());
		lookShot(context, server, connection, burst.add(0.0, 1.5, 8.0), 180.0F, 10.0F, "look_rasengan_burst");
		server.runCommand("tick unfreeze");
		context.waitTicks(20);
		boolean gone = server.computeOnServer(s -> connection.getServerPlayer().level().getEntitiesOfClass(SkillVfx.class,
			connection.getServerPlayer().getBoundingBox().inflate(32)).isEmpty());
		require(gone, "Rasengan's drawn effects should be gone a second after the cast");

		server.runOnServer(s -> connection.getServerPlayer().connection.teleport(0.5, -60, 0.5, 180.0F, 0.0F));
		context.waitTicks(3);
		cast = server.computeOnServer(s -> {
			fill(connection.getServerPlayer());
			return SkillCaster.tryCast(connection.getServerPlayer(), 0);
		});
		require(cast == SkillCaster.Result.OK, "Rasengan could not be cast again: " + cast);
		context.waitTicks(2);
		context.runOnClient(minecraft -> hideHud(minecraft, true));
		context.waitTicks(1);
		context.takeScreenshot("look_rasengan_first_person");
		context.runOnClient(minecraft -> hideHud(minecraft, false));
		context.waitTicks(20);
		server.runCommand("kill @e[tag=" + TARGET + "]");
		server.runCommand("time set noon");
		MinecraftMode.LOGGER.info("[job] Getsuga crescents and Rasengan's sphere and burst are drawn on the client");
	}

	/**
	 * One drawn look of a tier 4 skill: the weapon and skill slot, the looks the client must see after {@code ticks} ticks, the one
	 * photographed ({@code focus}, the first by default; {@link SkillLook#NONE}: the caster), where the camera stands from what it looks at (caster facing north:
	 * {@code side} east, {@code back} south), how far above the look's spot the camera aims ({@code lift}, for looks high in the sky)
	 * and the screenshot's name ({@code look_<skill>} by default); the husk stands {@code husk} blocks in front of the caster.
	 */
	private record LookCase(
		String weapon, int slot, List<SkillLook> looks, SkillLook focus, int ticks, double side, double up, double back, double lift, String shot, double husk
	) {
		LookCase(final String weapon, final int slot, final SkillLook look, final int ticks, final double side, final double up, final double back) {
			this(weapon, slot, List.of(look), ticks, side, up, back);
		}

		LookCase(final String weapon, final int slot, final List<SkillLook> looks, final int ticks, final double side, final double up, final double back) {
			this(weapon, slot, looks, looks.getFirst(), ticks, side, up, back, 0.0, "", 6.0);
		}

		/** Aims {@code lift} blocks above the look's spot. */
		LookCase lift(final double blocks) {
			return new LookCase(this.weapon, this.slot, this.looks, this.focus, this.ticks, this.side, this.up, this.back, blocks, this.shot, this.husk);
		}

		/** Photographs {@code look} (one of {@link #looks}). */
		LookCase focus(final SkillLook look) {
			return new LookCase(this.weapon, this.slot, this.looks, look, this.ticks, this.side, this.up, this.back, this.lift, this.shot, this.husk);
		}

		/** Names the screenshot {@code look_<name>} (a skill photographed twice). */
		LookCase shot(final String name) {
			return new LookCase(this.weapon, this.slot, this.looks, this.focus, this.ticks, this.side, this.up, this.back, this.lift, name, this.husk);
		}

		/** Puts the husk {@code blocks} in front of the caster (a skill that only reaches close by). */
		LookCase husk(final double blocks) {
			return new LookCase(this.weapon, this.slot, this.looks, this.focus, this.ticks, this.side, this.up, this.back, this.lift, this.shot, blocks);
		}
	}

	private static final String CAMERA = "job_camera";

	/**
	 * Every skill of the nine tier 4 Soul Reaper weapons draws its own look (0.5.36). Screenshots {@code look_<skill>}.
	 */
	private static void checkReaperLooks(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		List<LookCase> cases = List.of(
			new LookCase("tensa_zangetsu", 0, SkillLook.GETSUGA, 3, 6, 1.5, 3),
			new LookCase("tensa_zangetsu", 1, SkillLook.FLASH_TRAIL, 6, 10, 3, 0),
			new LookCase("tensa_zangetsu", 2, SkillLook.GETSUGA, 3, 2, 3, 6),
			new LookCase("tensa_zangetsu", 3, SkillLook.REIATSU, 8, 2, 1, -5),
			new LookCase("senbonzakura_kageyoshi", 0, SkillLook.PETAL_STORM, 8, 3, 5, 9),
			new LookCase("senbonzakura_kageyoshi", 1, SkillLook.PETAL_SPHERE, 6, 4, 4, 10),
			new LookCase("senbonzakura_kageyoshi", 2, SkillLook.SENKEI, 20, 4, 6, 13),
			new LookCase("senbonzakura_kageyoshi", 3, SkillLook.HAKUTEIKEN, 3, 9, 3, 4),
			new LookCase("daiguren_hyorinmaru", 0, SkillLook.ICE_FLOWER, 6, 4, 1.5, 3),
			new LookCase("daiguren_hyorinmaru", 1, SkillLook.ICE_PRISON, 14, 3, 5, 9),
			new LookCase("daiguren_hyorinmaru", 2, SkillLook.ICE_WINGS, 70, 3, 1, 6),
			new LookCase("daiguren_hyorinmaru", 3, SkillLook.ICE_BLOOM, 40, 5, 6, 15),
			new LookCase("ryujin_jakka", 0, SkillLook.FLAME_BURST, 5, 3, 5, 10),
			new LookCase("ryujin_jakka", 1, SkillLook.FLAME_WALL, 15, 4, 5, 12),
			new LookCase("ryujin_jakka", 2, SkillLook.TAIMATSU, 3, 4, 1, 2),
			new LookCase("ryujin_jakka", 3, SkillLook.ZANKA, 5, 3, 7, 12),
			new LookCase("kamishini_no_yari", 0, SkillLook.SPEAR, 2, 10, 3, 0),
			new LookCase("kamishini_no_yari", 1, SkillLook.SPEAR, 4, 10, 3, 0),
			new LookCase("kamishini_no_yari", 2, SkillLook.DUST, 8, 3, 6, 10),
			new LookCase("kamishini_no_yari", 3, SkillLook.SPEAR_KILL, 5, 8, 3, 2),
			new LookCase("kyoka_suigetsu", 0, SkillLook.MIRROR, 5, 3, 1, -5),
			new LookCase("kyoka_suigetsu", 1, SkillLook.KUROHITSUGI, 20, 5, 5, 9),
			new LookCase("kyoka_suigetsu", 2, SkillLook.MIRROR, 5, 5, 1.5, 3),
			new LookCase("kyoka_suigetsu", 3, SkillLook.HOGYOKU, 6, 3, 4, 10),
			new LookCase("karamatsu_shinju", 0, SkillLook.SLASH, 2, 3, 5, 7),
			new LookCase("karamatsu_shinju", 1, SkillLook.CROSS_SLASH, 4, 2, 2, -8),
			new LookCase("karamatsu_shinju", 2, SkillLook.ABYSS, 20, 3, 6, 9),
			new LookCase("karamatsu_shinju", 3, SkillLook.BINDING, 10, 3, 1.5, 2),
			new LookCase("true_zangetsu", 0, SkillLook.GETSUGA_DUAL, 2, 5, 1.5, 3),
			new LookCase("true_zangetsu", 1, List.of(SkillLook.SLASH, SkillLook.GETSUGA_DUAL), 2, 3, 5, 8),
			new LookCase("true_zangetsu", 2, SkillLook.FLASH_TRAIL, 3, 10, 3, 0),
			new LookCase("true_zangetsu", 3, List.of(SkillLook.MUGETSU, SkillLook.MUGETSU_VOID), 8, 16, 6, 6),
			new LookCase("soo_zabimaru", 0, SkillLook.BONE_FANGS, 7, 3, 5, 9),
			new LookCase("soo_zabimaru", 1, SkillLook.BONE_SNAKE, 4, 7, 2.5, 2),
			new LookCase("soo_zabimaru", 2, SkillLook.BONE_LASH, 3, 3, 6, 8),
			new LookCase("soo_zabimaru", 3, SkillLook.BONE_CAGE, 10, 2, 2, -5)
		);
		checkClassLooks(context, server, connection, JobClass.SHINIGAMI, cases, SkillLook.GETSUGA_DUAL, SkillLook.BONE_CAGE, "");
	}

	/**
	 * Every skill of the nine tier 4 Archer weapons draws its own look (0.5.38): the Fate archers' arrows, gates, chains, suns and
	 * moons. Screenshots {@code look_<skill>}, and Enuma Elish once more with improved transparency ({@code _fabulous}).
	 */
	private static void checkArcherLooks(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		List<LookCase> cases = List.of(
			new LookCase("chiron_greatbow", 0, List.of(SkillLook.STAR_ARROW, SkillLook.ANTARES_MARK), 2, 6, 2, 6),
			new LookCase("chiron_greatbow", 0, SkillLook.ANTARES_MARK, 8, 2, 1, 7).lift(2).shot("chiron_greatbow.antares_snipe_mark"),
			new LookCase("chiron_greatbow", 1, SkillLook.PANKRATION, 2, 3, 2.5, 5),
			new LookCase("chiron_greatbow", 2, SkillLook.ARROW_WARD, 8, 2, 1.5, -5),
			new LookCase("chiron_greatbow", 3, SkillLook.GALLOP, 6, 10, 3, 0),
			new LookCase("kanshou_bakuya_alter", 0, SkillLook.ULW_FIELD, 55, 5, 5, 16).lift(4),
			new LookCase("kanshou_bakuya_alter", 1, List.of(SkillLook.GUN_KATA, SkillLook.TRACER), 2, 3, 2.5, 5),
			new LookCase("kanshou_bakuya_alter", 2, SkillLook.COUNTER_GUARDIAN, 10, 3, 1.5, 4),
			new LookCase("kanshou_bakuya_alter", 3, List.of(SkillLook.FLASH_TRAIL, SkillLook.TRACER), 6, 10, 3, 0),
			new LookCase("apollo_bow", 0, SkillLook.SUN_BEAM, 4, 15, 4, 2),
			new LookCase("apollo_bow", 1, SkillLook.HEEL_STRIKE, 7, 4, 2, 4),
			new LookCase("apollo_bow", 2, SkillLook.PAEAN, 10, 3, 2.5, 7),
			new LookCase("apollo_bow", 3, SkillLook.SUN_STEP, 3, 10, 3, 0),
			new LookCase("artemis_bow", 0, SkillLook.MOON_ARROW, 3, 4, 1.5, 3),
			new LookCase("artemis_bow", 1, SkillLook.CRESCENT_ARROW, 2, 2, 2.5, 5),
			new LookCase("artemis_bow", 2, SkillLook.MOON_SIGIL, 10, 3, 4, 11).lift(1.5),
			new LookCase("artemis_bow", 3, SkillLook.MOON_VEIL, 10, 2, 1.5, -5),
			new LookCase("gandiva", 0, SkillLook.PASHUPATA_BOLT, 6, 4, 0, 17).lift(4.5),
			new LookCase("gandiva", 1, SkillLook.FLAME_ARROW, 2, 4, 1.5, 3),
			new LookCase("gandiva", 2, SkillLook.ENDOWED, 10, 2, 2.5, -6),
			new LookCase("gandiva", 3, SkillLook.MANA_FLAME, 6, 10, 3, 0),
			new LookCase("maanna", 0, SkillLook.VENUS_STAR, 5, 5, 0, 17).lift(4.5),
			new LookCase("maanna", 0, SkillLook.VENUS_STAR, 12, 5, 4, 15).lift(1.5).shot("maanna.an_gal_ta_kigal_she_impact"),
			new LookCase("maanna", 1, SkillLook.GEM, 2, 2, 1, 3),
			new LookCase("maanna", 2, SkillLook.BEAUTY, 10, 3, 4, 9),
			new LookCase("maanna", 3, SkillLook.VENUS_WINGS, 10, 3, 1, 6),
			new LookCase("gate_of_babylon", 0, List.of(SkillLook.BABYLON_SKY, SkillLook.BABYLON_GATE, SkillLook.TREASURE), 40, 7, 1, 20).lift(5),
			new LookCase("gate_of_babylon", 1, List.of(SkillLook.BABYLON_GATE, SkillLook.TREASURE), 7, 6, 1.5, -7).focus(SkillLook.NONE),
			new LookCase("gate_of_babylon", 2, SkillLook.ENKIDU, 12, 7, 3, 1),
			new LookCase("gate_of_babylon", 3, SkillLook.TREASURY_SHIELDS, 8, 2.5, 1.5, -5),
			new LookCase("ea_and_merodach", 0, SkillLook.EA_CHARGE, 2, 3, 1, 3).shot("ea_and_merodach.enuma_elish_charge"),
			new LookCase("ea_and_merodach", 0, SkillLook.ENUMA_ELISH, 17, 14, 6, 6),
			new LookCase("ea_and_merodach", 1, List.of(SkillLook.SIN_WAVE, SkillLook.SLASH), 2, 2, 2, 5),
			new LookCase("ea_and_merodach", 2, SkillLook.SHA_NAQBA, 10, 3, 3, 9).lift(2.5),
			new LookCase("ea_and_merodach", 3, SkillLook.GOLDEN_RULE, 10, 2, 1.5, -5),
			new LookCase("pashupata", 0, SkillLook.PASHUPATA_DISC, 5, 5, 0, 15).lift(3.5),
			new LookCase("pashupata", 1, SkillLook.DISCUS, 2, 3, 2.5, 3),
			new LookCase("pashupata", 2, SkillLook.FLAME_BURST, 5, 3, 5, 10),
			new LookCase("pashupata", 3, SkillLook.ENDOWED, 10, 2, 2.5, -6)
		);
		checkClassLooks(context, server, connection, JobClass.ARCHER, cases, SkillLook.STAR_ARROW, SkillLook.PASHUPATA_DISC, "ea_and_merodach.enuma_elish");
	}

	/**
	 * Every skill of the nine tier 4 Mage weapons draws its own look (0.5.39): Medea's witchcraft, Tamamo's mirror and talismans,
	 * Megumin's EXPLOSION, Zhuge Liang's maze, Mavis's Fairy Law, Merlin's garden, Frieren's Zoltraak and Solomon's rings. Screenshots
	 * {@code look_<skill>}, and EXPLOSION! once more with improved transparency ({@code _fabulous}).
	 */
	private static void checkMageLooks(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		List<LookCase> cases = List.of(
			new LookCase("colchis_scepter", 0, List.of(SkillLook.HECATIC_SKY, SkillLook.WITCH_CIRCLE, SkillLook.WITCH_BOLT), 14, 7, 1, 20).lift(5),
			new LookCase("colchis_scepter", 1, SkillLook.RULE_BREAKER, 6, 4, 1.5, 4),
			new LookCase("colchis_scepter", 2, SkillLook.DIVINE_WORDS, 10, 2.5, 1.5, -5),
			new LookCase("colchis_scepter", 3, SkillLook.GOLDEN_FLEECE, 10, 2, 1.2, 4),
			new LookCase("yata_no_kagami", 0, SkillLook.AMATERASU, 10, 3, 2.5, 7).lift(1.5),
			new LookCase("yata_no_kagami", 1, SkillLook.ENTEN, 16, 3, 4, 9).lift(1),
			new LookCase("yata_no_kagami", 2, List.of(SkillLook.HYOTEN, SkillLook.ICE_OFUDA), 12, 5, 2, 15).lift(4),
			new LookCase("yata_no_kagami", 3, SkillLook.FOX_FIRE, 10, 2.5, 1.5, 5),
			new LookCase("megumin_staff", 0, SkillLook.EXPLOSION, 6, 6, 1, 22).lift(5).shot("megumin_staff.explosion_chant"),
			new LookCase("megumin_staff", 0, SkillLook.EXPLOSION, 20, 8, 3, 26).lift(6),
			new LookCase("megumin_staff", 1, SkillLook.DRAIN_TOUCH, 8, 3.5, 1.5, 3).husk(3.2),
			new LookCase("megumin_staff", 2, SkillLook.CRIMSON_POSE, 6, 2.5, 0.5, -6),
			new LookCase("megumin_staff", 3, SkillLook.MANATITE, 8, 2.5, 1, -4),
			new LookCase("sleeping_dragon_treatise", 0, SkillLook.STONE_MAZE, 14, 6, 6, 16).lift(1),
			new LookCase("sleeping_dragon_treatise", 1, List.of(SkillLook.EAST_WIND, SkillLook.WIND_FIRE), 3, 6, 3, 4),
			new LookCase("sleeping_dragon_treatise", 2, SkillLook.DISCERNING_EYE, 10, 3, 1.5, 4),
			new LookCase("sleeping_dragon_treatise", 3, SkillLook.WAR_BANNER, 10, 3, 2, 6).lift(1),
			new LookCase("lumen_histoire", 0, SkillLook.FAIRY_LAW, 10, 3, 2, 8).lift(2.5).shot("lumen_histoire.fairy_law_gather"),
			new LookCase("lumen_histoire", 0, SkillLook.FAIRY_LAW, 24, 10, 6, 24).lift(2),
			new LookCase("lumen_histoire", 1, SkillLook.FAIRY_GLITTER, 2, 3, 1, 3),
			new LookCase("lumen_histoire", 1, SkillLook.GLITTER_BURST, 6, 4, 2, 6).shot("lumen_histoire.fairy_glitter_burst"),
			new LookCase("lumen_histoire", 2, SkillLook.FAIRY_SPHERE, 8, 2.5, 1.5, -5),
			new LookCase("lumen_histoire", 3, SkillLook.FAIRY_WINGS, 8, 2, 1.2, 4),
			new LookCase("merlin_staff", 0, SkillLook.AVALON, 40, 5, 6, 14),
			new LookCase("merlin_staff", 1, SkillLook.HERO_CREATION, 8, 2.5, 1.5, -5),
			new LookCase("merlin_staff", 2, SkillLook.PETAL_ORB, 2, 3, 1.5, 3),
			new LookCase("merlin_staff", 3, SkillLook.ILLUSION, 4, 10, 3, 0),
			new LookCase("frieren_staff", 0, List.of(SkillLook.ZOLTRAAK_CIRCLE, SkillLook.ZOLTRAAK), 12, 6, 1.5, -7).focus(SkillLook.NONE),
			new LookCase("frieren_staff", 1, SkillLook.JUDRADJIM, 3, 5, 0, 17).lift(4.5),
			new LookCase("frieren_staff", 2, SkillLook.HEX_BARRIER, 10, 3, 1.5, -5),
			new LookCase("frieren_staff", 3, SkillLook.FLOWER_FIELD, 40, 4, 5, 11),
			new LookCase("lemegeton", 0, SkillLook.ARS_ALMADEL, 26, 7, 4, 22).lift(5),
			new LookCase("lemegeton", 1, SkillLook.DEMON_PILLARS, 10, 4, 3, 9),
			new LookCase("lemegeton", 2, SkillLook.CLAIRVOYANCE, 10, 3, 1.5, 5).lift(1),
			new LookCase("lemegeton", 3, SkillLook.TEN_RINGS, 10, 2.5, 1.5, -5),
			new LookCase("rule_breaker", 0, SkillLook.SPELL_BREAK, 4, 10, 3, 2),
			new LookCase("rule_breaker", 1, List.of(SkillLook.WITCH_CIRCLE, SkillLook.RUNE_SYLLABLE), 4, 5, 1.5, -6).focus(SkillLook.NONE),
			new LookCase("rule_breaker", 2, SkillLook.WITCH_SLASH, 2, 3, 2.5, 5),
			new LookCase("rule_breaker", 3, SkillLook.TERRITORY, 10, 4, 6, 14)
		);
		checkClassLooks(context, server, connection, JobClass.MAGE, cases, SkillLook.HECATIC_SKY, SkillLook.TEN_RINGS, "megumin_staff.explosion");
	}

	/**
	 * Every skill of the nine tier 4 Pirate weapons draws its own look (0.5.40): the Corsair's battleship and octopus, Drake's ghost fleet,
	 * Anne and Mary, the kraken, Big Mom's homies, Whitebeard's quake, Gear 5's giant fist and the Pirate King's haki. Screenshots
	 * {@code look_<skill>}, and Bajrang Gun once more with improved transparency ({@code _fabulous}).
	 */
	private static void checkPirateLooks(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		List<LookCase> cases = List.of(
			new LookCase("battleship_cannon", 0, SkillLook.CANNON_SHOT, 2, 3, 1, 3).husk(12).shot("battleship_cannon.battleship_shot"),
			new LookCase("battleship_cannon", 0, List.of(SkillLook.BATTLESHIP, SkillLook.CANNON_BLAST), 16, 7, 3, 7).lift(1.5),
			new LookCase("battleship_cannon", 1, SkillLook.OCTOPUS, 14, 4, 3, 6),
			new LookCase("battleship_cannon", 2, SkillLook.AIR_STRIKE, 16, 7, 4, 11).lift(3.5),
			new LookCase("battleship_cannon", 3, SkillLook.BULLSEYE, 10, 3, 1.5, 4),
			new LookCase("el_draque", 0, List.of(SkillLook.WILD_HUNT, SkillLook.GHOST_CANNON, SkillLook.CANNON_SHOT), 12, 6, 3, -10).lift(2),
			new LookCase("el_draque", 0, SkillLook.GOLDEN_HIND, 34, 12, 4, 6).lift(4).shot("el_draque.golden_hind"),
			new LookCase("el_draque", 1, SkillLook.COIN_SHOT, 2, 3, 1, 3),
			new LookCase("el_draque", 2, SkillLook.STORM_VOYAGE, 12, 3.5, 2, 5).lift(0.3),
			new LookCase("el_draque", 3, SkillLook.STAR_WAKE, 8, 10, 3, 0),
			new LookCase("bonny_read_musket", 0, SkillLook.FREE_BIRD, 8, 4, 1.5, 3),
			new LookCase("bonny_read_musket", 0, SkillLook.BIRD_SHOT, 12, 3, 1, 3).husk(22).shot("bonny_read_musket.caribbean_free_bird_shot"),
			new LookCase("bonny_read_musket", 1, SkillLook.CUTLASS_ARC, 3, 3, 2.5, 5),
			new LookCase("bonny_read_musket", 2, SkillLook.MUSKET_SHOT, 2, 3, 1, 3).husk(12),
			new LookCase("bonny_read_musket", 2, SkillLook.CANNON_BLAST, 6, 4, 2, 6).shot("bonny_read_musket.annes_powder_shot_blast"),
			new LookCase("bonny_read_musket", 3, SkillLook.TWIN_BIRDS, 10, 3, 1.5, -5),
			new LookCase("dutchman_harpoon", 0, SkillLook.KRAKEN, 30, 5, 5, 14).lift(2),
			new LookCase("dutchman_harpoon", 0, SkillLook.KRAKEN_SLAM, 46, 10, 4, 6).lift(3).shot("dutchman_harpoon.release_the_kraken_slam"),
			new LookCase("dutchman_harpoon", 1, SkillLook.TENTACLE_LASH, 4, 3, 3, 5),
			new LookCase("dutchman_harpoon", 2, SkillLook.DAVY_JONES, 12, 2, 2.5, -7).lift(2),
			new LookCase("dutchman_harpoon", 3, SkillLook.DEEP_RISE, 5, 10, 3, 0),
			new LookCase("napoleon_rapier", 0, SkillLook.IKOKU_SWING, 3, 8, 3, 9).lift(1),
			new LookCase("napoleon_rapier", 0, SkillLook.IKOKU_WAVE, 7, 7, 2, 4).shot("napoleon_rapier.ikoku_sovereignty_wave"),
			new LookCase("napoleon_rapier", 1, SkillLook.SOUL_POCUS, 10, 5.5, 1.2, -1.8).lift(0.6).husk(3.5),
			new LookCase("napoleon_rapier", 2, SkillLook.ZEUS, 4, 5, 1, 14).lift(4),
			new LookCase("napoleon_rapier", 3, SkillLook.CROQUEMBOUCHE, 18, 3, 1.5, -5).lift(0.5),
			new LookCase("moby_dick_anchor", 0, SkillLook.QUAKE, 4, 4, 2, 6).shot("moby_dick_anchor.island_shaker_crack"),
			new LookCase("moby_dick_anchor", 0, SkillLook.QUAKE, 12, 6, 4, 12).lift(1),
			new LookCase("moby_dick_anchor", 1, SkillLook.TSUNAMI, 3, 5, 2, 5),
			new LookCase("moby_dick_anchor", 2, SkillLook.ANCHOR_DROP, 10, 9, 3, 8).lift(2).husk(16),
			new LookCase("moby_dick_anchor", 2, SkillLook.ANCHOR_DROP, 44, 6, 3, 6).lift(1).husk(16).shot("moby_dick_anchor.anchor_drop_land"),
			new LookCase("moby_dick_anchor", 3, SkillLook.MY_SONS, 12, 3, 2, -8).lift(2),
			new LookCase("nika_knuckles", 0, SkillLook.BAJRANG_GUN, 6, 6, 1, -14).lift(5),
			new LookCase("nika_knuckles", 0, SkillLook.BAJRANG_GUN, 11, 9, 3, -11).lift(2.5).shot("nika_knuckles.bajrang_gun_impact"),
			new LookCase("nika_knuckles", 1, SkillLook.RED_HAWK, 4, 7, 1.5, 2),
			new LookCase("nika_knuckles", 2, SkillLook.JET_GATLING, 6, 4, 1.2, -3),
			new LookCase("nika_knuckles", 3, SkillLook.LIBERATION_DRUMS, 12, 3, 1.5, -5),
			new LookCase("pirate_kings_sabre", 0, SkillLook.KAMUSARI, 3, 7, 4.5, 10).lift(0.3),
			new LookCase("pirate_kings_sabre", 0, SkillLook.KAMUSARI_WAVE, 4, 6, 2.5, 7).shot("pirate_kings_sabre.divine_departure_wave"),
			new LookCase("pirate_kings_sabre", 1, SkillLook.HAKI_CLASH, 4, 6, 3, 9).lift(1.5),
			new LookCase("pirate_kings_sabre", 2, SkillLook.WILL_OF_D, 14, 1.8, 0.8, -3).lift(1.2),
			new LookCase("pirate_kings_sabre", 3, SkillLook.PIRATE_ERA, 24, -3, 1.5, -6).lift(3),
			new LookCase("thunderclap_blunderbuss", 0, List.of(SkillLook.BROADSIDE, SkillLook.THUNDER_FLASH, SkillLook.THUNDER_SHOT), 15, 5, 5, 16).lift(3),
			new LookCase("thunderclap_blunderbuss", 1, SkillLook.BUCKSHOT, 3, 4, 2, 5),
			new LookCase("thunderclap_blunderbuss", 2, SkillLook.HAKI_SHOT, 2, 2.2, 0.5, -7.5).husk(16),
			new LookCase("thunderclap_blunderbuss", 3, SkillLook.RECOIL, 4, 9, 3, 1)
		);
		checkClassLooks(context, server, connection, JobClass.PIRATE, cases, SkillLook.BATTLESHIP, SkillLook.RECOIL, "nika_knuckles.bajrang_gun");
	}

	/**
	 * Every skill of the nine tier 4 Warrior weapons draws its own look (0.5.41): Artoria and Saber Alter's Excaliburs, the Lion King's
	 * Rhongomyniad, Lancelot, Mordred, Gawain, St. George, Kusanagi and the Round Table's shield. Screenshots {@code look_<skill>}, the
	 * sword of promised victory once more with improved transparency ({@code _fabulous}).
	 */
	private static void checkWarriorLooks(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		List<LookCase> cases = List.of(
			new LookCase("excalibur", 0, SkillLook.EXCALIBUR, 4, 4, 1.5, 4).focus(SkillLook.NONE).lift(1.5).shot("excalibur.promised_victory_raise"),
			new LookCase("excalibur", 0, SkillLook.EXCALIBUR, 14, 9, 3, 2),
			new LookCase("excalibur", 1, SkillLook.AIR_RELEASE, 4, 5, 4, 7),
			new LookCase("excalibur", 1, SkillLook.STRIKE_AIR, 3, 3, 1, 3).husk(14).shot("excalibur.strike_air_shot"),
			new LookCase("excalibur", 2, SkillLook.MANA_BURST, 4, 9, 3, 1),
			new LookCase("excalibur", 3, SkillLook.AVALON_SHEATH, 4, 2.5, 1, -4).shot("excalibur.avalon_guard_sheath"),
			new LookCase("excalibur", 3, SkillLook.AVALON_SHEATH, 28, 3.5, 2, -5).lift(0.3),
			new LookCase("excalibur_morgan", 0, SkillLook.VORTIGERN, 4, 4, 1.5, 4).focus(SkillLook.NONE).lift(1.5).shot("excalibur_morgan.vortigern_raise"),
			new LookCase("excalibur_morgan", 0, SkillLook.VORTIGERN, 14, 9, 3, 2),
			new LookCase("excalibur_morgan", 1, SkillLook.DARK_BURST, 5, 5, 4, 8),
			new LookCase("excalibur_morgan", 2, SkillLook.TYRANT_TRAIL, 4, 9, 3, 1),
			new LookCase("excalibur_morgan", 3, SkillLook.BLACK_ARMOR, 14, 2.5, 1.5, -4),
			new LookCase("rhongomyniad", 0, SkillLook.RHONGOMYNIAD, 4, 10, 6, 14).lift(7).shot("rhongomyniad.pillar_of_light_descent"),
			new LookCase("rhongomyniad", 0, SkillLook.RHONGOMYNIAD, 12, 9, 4, 13).lift(3),
			new LookCase("rhongomyniad", 1, SkillLook.STORM_LANCE, 3, 7, 1.5, 2),
			new LookCase("rhongomyniad", 2, SkillLook.LION_CHARGE, 4, 9, 3, 1),
			new LookCase("rhongomyniad", 3, SkillLook.LANCE_WARD, 12, 4, 2, -6).lift(1.5),
			new LookCase("arondight", 0, List.of(SkillLook.ARONDIGHT_ARC, SkillLook.OVERLOAD_AURA), 6, 4, 2.5, 5),
			new LookCase("arondight", 0, SkillLook.OVERLOAD_AURA, 14, 3, 1.5, -5).shot("arondight.overload_aura"),
			new LookCase("arondight", 1, SkillLook.LAKE_KNIGHT, 16, 4, 1.5, -3).lift(0.3),
			new LookCase("arondight", 2, SkillLook.LAKE_WAVE, 3, 3, 1, 3).husk(14),
			new LookCase("arondight", 3, SkillLook.UNBLEMISHED, 12, 3, 1.5, -4),
			new LookCase("clarent", 0, List.of(SkillLook.BLOOD_ARTHUR, SkillLook.RED_BOLT), 3, 4, 1.5, 4).focus(SkillLook.NONE).lift(1.5)
				.shot("clarent.blood_arthur_raise"),
			new LookCase("clarent", 0, SkillLook.BLOOD_ARTHUR, 10, 9, 3, 2),
			new LookCase("clarent", 0, SkillLook.RED_BOLT, 3, 6, 3, 10).lift(3).shot("clarent.blood_arthur_bolt"),
			new LookCase("clarent", 1, SkillLook.RED_CHAIN, 3, 6, 2, 1).focus(SkillLook.NONE).husk(5),
			new LookCase("clarent", 2, SkillLook.REBEL_TRAIL, 4, 9, 3, 1),
			new LookCase("clarent", 3, SkillLook.PEDIGREE_HELM, 10, 2, 0.6, -3).lift(0.6),
			new LookCase("galatine", 0, SkillLook.GALATINE_SUN, 5, 8, 3, 13).lift(4).shot("galatine.excalibur_galatine_sun"),
			new LookCase("galatine", 0, SkillLook.GALATINE_SUN, 12, 9, 5, 12).lift(1),
			new LookCase("galatine", 1, SkillLook.SUNLIGHT, 14, 5, 2, -9).lift(2),
			new LookCase("galatine", 2, SkillLook.SOLAR_FLARE, 5, 6, 4, 9).lift(0.5),
			new LookCase("galatine", 3, SkillLook.NOON_ARC, 3, 3, 2.5, 5),
			new LookCase("ascalon", 0, List.of(SkillLook.DRACONIS_SEAL, SkillLook.HOLY_LANCE), 4, 8, 2, 2).focus(SkillLook.HOLY_LANCE),
			new LookCase("ascalon", 0, SkillLook.DRACONIS_SEAL, 20, 4, 1.5, 5).lift(0.5).shot("ascalon.abyssus_draconis_seal"),
			new LookCase("ascalon", 1, SkillLook.DRAGONBANE, 3, 6, 1.5, 2),
			new LookCase("ascalon", 2, SkillLook.MARTYR_SHIELD, 10, 2.5, 0.8, -4.5).lift(0.2),
			new LookCase("ascalon", 3, SkillLook.SAINT_WARD, 14, 5, 4, -8),
			new LookCase("kusanagi", 0, SkillLook.MURAKUMO, 10, 9, 4, 13).lift(2),
			new LookCase("kusanagi", 1, SkillLook.GRASS_CUTTER, 4, 5, 4, 6),
			new LookCase("kusanagi", 2, SkillLook.WIND_STEP, 4, 9, 3, 1),
			new LookCase("kusanagi", 3, SkillLook.SERPENT, 3, 3, 1, 3).husk(14),
			new LookCase("round_table_shield", 0, SkillLook.CAMELOT, 14, 8, 5, 12).lift(1),
			new LookCase("round_table_shield", 1, SkillLook.SNOW_BULWARK, 14, 3, 1.5, -5),
			new LookCase("round_table_shield", 2, SkillLook.ROUND_TABLE, 3, 3, 2.5, 5),
			new LookCase("round_table_shield", 3, SkillLook.SHIELD_CHARGE, 4, 9, 3, 1)
		);
		checkClassLooks(context, server, connection, JobClass.WARRIOR, cases, SkillLook.EXCALIBUR, SkillLook.SHIELD_CHARGE, "excalibur.promised_victory");
	}

	/**
	 * Every skill of the nine tier 4 Rogue weapons draws its own look (0.5.42): Kasaka, the Knight Killer, Beru, the Shadow Monarch's chain
	 * sickle and Kamish's Wrath (Solo Leveling), Minato's kunai, Itachi's Totsuka Blade, Kakashi's Kamui and Madara's gunbai (Naruto).
	 * Screenshots {@code look_<skill>}, Itachi's Susanoo once more with improved transparency ({@code _fabulous}).
	 */
	private static void checkRogueLooks(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		List<LookCase> cases = List.of(
			new LookCase("kasaka_venom_fang", 0, SkillLook.SERPENT_LAIR, 12, 9, 6, 12).lift(1.5),
			new LookCase("kasaka_venom_fang", 1, SkillLook.VENOM_BITE, 4, 3, 1, 4),
			new LookCase("kasaka_venom_fang", 2, SkillLook.VENOM_MIST, 12, 6, 5, 8),
			new LookCase("kasaka_venom_fang", 3, SkillLook.SPRINT_TRAIL, 8, 4, 1.5, 3),
			new LookCase("knight_killer", 0, SkillLook.KNIGHTS_BANE, 4, 3, 1, 5),
			new LookCase("knight_killer", 1, SkillLook.MUTILATION, 2, 3, 2.5, 4).husk(3),
			new LookCase("knight_killer", 2, SkillLook.KILLING_INTENT, 12, 8, 5, -9),
			new LookCase("knight_killer", 3, SkillLook.RIPOSTE, 10, 1.5, 0.5, -3.5),
			new LookCase("beru_talons", 0, SkillLook.ANT_DASH, 5, 9, 3, 1),
			new LookCase("beru_talons", 0, SkillLook.ANT_FRENZY, 10, 6, 5, 7).shot("beru_talons.ant_king_rampage_frenzy"),
			new LookCase("beru_talons", 1, SkillLook.DEVOUR, 4, 3, 1.2, 4).husk(4),
			new LookCase("beru_talons", 2, SkillLook.STOLEN_HEALING, 16, 9, 8, 12),
			new LookCase("beru_talons", 3, SkillLook.WING_BURST, 8, 5, 1, 4).focus(SkillLook.NONE),
			new LookCase("beru_talons", 3, SkillLook.WING_BURST, 32, 6, 5, 8).shot("beru_talons.wing_burst_land"),
			new LookCase("hiraishin_kunai", 0, List.of(SkillLook.YELLOW_FLASH, SkillLook.RASENGAN), 7, 4, 1.5, 5),
			new LookCase("hiraishin_kunai", 0, SkillLook.RASENGAN_BURST, 23, 5, 3, 6).shot("hiraishin_kunai.yellow_flash_rasengan"),
			new LookCase("hiraishin_kunai", 1, SkillLook.FORMULA_KUNAI, 2, 2, 0.6, 2).husk(14),
			new LookCase("hiraishin_kunai", 2, SkillLook.HIRAISHIN, 4, 9, 3, 1),
			new LookCase("hiraishin_kunai", 3, SkillLook.SPACE_TIME_BARRIER, 12, 3, 1.5, -4.5),
			new LookCase("totsuka_blade", 0, List.of(SkillLook.SUSANOO_ITACHI, SkillLook.TOTSUKA), 10, 7, 3, -10).lift(2),
			new LookCase("totsuka_blade", 0, SkillLook.TOTSUKA, 6, 7, 2, 2).shot("totsuka_blade.susanoo_totsuka"),
			new LookCase("totsuka_blade", 1, SkillLook.BLACK_FLAMES, 14, 5, 4, 7),
			new LookCase("totsuka_blade", 2, SkillLook.TSUKUYOMI, 10, 5, 1.5, 3),
			new LookCase("totsuka_blade", 3, SkillLook.CROW_CLONE, 14, 4, 1.5, 5),
			new LookCase("monarch_chain_sickle", 0, SkillLook.MONARCH_DOMAIN, 30, 10, 8, 12),
			new LookCase("monarch_chain_sickle", 1, SkillLook.RULERS_AUTHORITY, 6, 9, 6, 10).lift(6).shot("monarch_chain_sickle.rulers_authority_lift"),
			new LookCase("monarch_chain_sickle", 1, SkillLook.RULERS_AUTHORITY, 12, 8, 5, 9),
			new LookCase("monarch_chain_sickle", 2, SkillLook.REAPER_CHAIN, 2, 3, 9, 7),
			new LookCase("monarch_chain_sickle", 3, SkillLook.ABYSSAL_GUARD, 16, 4, 2, -6),
			new LookCase("kamui_shuriken", 0, SkillLook.KAMUI_SHURIKEN, 2, 3, 1.5, 3).husk(14),
			new LookCase("kamui_shuriken", 0, SkillLook.KAMUI_VORTEX, 6, 6, 2, 8).shot("kamui_shuriken.susanoo_kamui_shuriken_vortex"),
			new LookCase("kamui_shuriken", 1, SkillLook.PURPLE_CHAIN, 3, 6, 2, 1).focus(SkillLook.NONE).husk(5),
			new LookCase("kamui_shuriken", 2, SkillLook.KAMUI_WARP, 12, 4, 1, 5),
			new LookCase("kamui_shuriken", 3, SkillLook.KAMUI_RAIKIRI, 4, 8, 4, 2),
			new LookCase("kamish_wrath", 0, SkillLook.ARISE, 30, 9, 5, 11).lift(1),
			new LookCase("kamish_wrath", 1, SkillLook.DRAGON_FEAR, 12, 7, 3, -10).lift(2),
			new LookCase("kamish_wrath", 2, SkillLook.DRAGON_FANG, 2, 2, 0.8, 2).husk(14),
			new LookCase("kamish_wrath", 3, SkillLook.SHADOW_EXCHANGE, 6, 9, 4, 1),
			new LookCase("madara_gunbai", 0, SkillLook.UCHIHA_RETURN, 12, 3, 1, -4.5).shot("madara_gunbai.uchiha_return_raise"),
			new LookCase("madara_gunbai", 0, SkillLook.UCHIHA_RETURN, 22, 8, 6, 10),
			new LookCase("madara_gunbai", 1, SkillLook.DESTROYER_FLAME, 4, 7, 4, 4).shot("madara_gunbai.great_fire_annihilation_wave"),
			new LookCase("madara_gunbai", 1, SkillLook.DESTROYER_FLAME, 12, 7, 5, 9),
			new LookCase("madara_gunbai", 2, SkillLook.GUNBAI_SWEEP, 3, 4, 3.5, 5),
			new LookCase("madara_gunbai", 3, SkillLook.GUNBAI_LEAP, 8, 5, 1.5, 4).focus(SkillLook.NONE),
			new LookCase("madara_gunbai", 3, SkillLook.GUNBAI_LEAP, 30, 6, 5, 8).shot("madara_gunbai.gunbai_leap_land")
		);
		checkClassLooks(context, server, connection, JobClass.ROGUE, cases, SkillLook.SERPENT_LAIR, SkillLook.GUNBAI_LEAP, "totsuka_blade.susanoo");
	}

	/**
	 * Every skill of the nine tier 4 Hunter weapons draws its own look (0.5.43): Killua's Godspeed, Kurapika's chains, Adult Gon, Chrollo's
	 * Skill Hunter, Zeno's dragons, Silva's orbs, Meruem, Netero's Guanyin and Illumi's needles. Screenshots {@code look_<skill>}; the
	 * Fabulous one is the Zero Hand.
	 */
	private static void checkHunterLooks(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		List<LookCase> cases = List.of(
			new LookCase("godspeed", 0, SkillLook.GODSPEED_STEP, 4, 9, 3, 1),
			new LookCase("godspeed", 1, SkillLook.GODSPEED_AURA, 12, 3, 1.5, -4.5),
			new LookCase("godspeed", 2, SkillLook.THUNDERBOLT, 4, 6, 4, 8),
			new LookCase("godspeed", 3, List.of(SkillLook.NARUKAMI, SkillLook.THUNDERBOLT), 3, 6, 2, 1).husk(5),
			new LookCase("emperor_time", 0, SkillLook.JUDGMENT_CHAIN, 7, 7, 2.5, 2),
			new LookCase("emperor_time", 1, SkillLook.STEAL_CHAIN, 8, 6, 3, 4).husk(4),
			new LookCase("emperor_time", 2, SkillLook.CHAIN_JAIL, 20, 5, 5, 9),
			new LookCase("emperor_time", 3, SkillLook.SCARLET_EYES, 14, 3, 1.5, -5).lift(0.6),
			new LookCase("adult_gon", 0, SkillLook.GON_RAGE, 14, 3, 1.5, -6).lift(1),
			new LookCase("adult_gon", 1, SkillLook.JAJANKEN_ROCK, 8, 5, 1, 4).focus(SkillLook.NONE),
			new LookCase("adult_gon", 1, SkillLook.JAJANKEN_ROCK, 32, 6, 5, 8).shot("adult_gon.first_comes_rock_land"),
			new LookCase("adult_gon", 2, SkillLook.LONG_SCISSORS, 4, 8, 3, 3),
			new LookCase("adult_gon", 3, SkillLook.GON_ROCK, 12, 6, 2, 8).lift(2),
			new LookCase("adult_gon", 3, SkillLook.ROCK_BLAST, 25, 6, 7, 11).shot("adult_gon.thats_all_i_need_blast"),
			new LookCase("skill_hunter", 0, SkillLook.SUN_MOON, 12, 3, 1.5, 4),
			new LookCase("skill_hunter", 0, SkillLook.SEAL_CHAIN, 33, 6, 2, 1).focus(SkillLook.NONE).husk(5).shot("skill_hunter.sun_and_moon_blast"),
			new LookCase("skill_hunter", 1, SkillLook.INDOOR_FISH, 4, 3.5, 0.8, 0.5).husk(14),
			new LookCase("skill_hunter", 2, SkillLook.FUN_CLOTH, 12, 3, 1.5, -4),
			new LookCase("skill_hunter", 3, List.of(SkillLook.BANDIT_BOOK, SkillLook.PAGE_GATE), 8, 4, 1.5, 3),
			new LookCase("skill_hunter", 3, SkillLook.STOLEN_SKILL, 6, 2, 1, 2).husk(14).shot("skill_hunter.bandits_secret_shot"),
			new LookCase("zeno_dragon", 0, SkillLook.DRAGON_HEAD, 3, 3, 1.5, 3).husk(14),
			new LookCase("zeno_dragon", 0, SkillLook.DRAGON_BURST, 8, 5, 2, 6).shot("zeno_dragon.dragon_head_burst"),
			new LookCase("zeno_dragon", 1, SkillLook.DRAGON_LANCE, 5, 8, 3, 3),
			new LookCase("zeno_dragon", 2, SkillLook.ZENO_AURA, 14, 3, 1.5, -5),
			new LookCase("zeno_dragon", 3, List.of(SkillLook.DRAGON_DIVE, SkillLook.DIVE_DRAGON), 24, 8, 9, 16).lift(3),
			new LookCase("zeno_dragon", 3, SkillLook.DIVE_DRAGON, 20, 3, 1, 3).shot("zeno_dragon.dragon_dive_dragons"),
			new LookCase("silva_orbs", 0, SkillLook.SILVA_ORB, 3, 2, 1, 2).husk(14),
			new LookCase("silva_orbs", 0, SkillLook.ORB_BURST, 6, 5, 2, 6).shot("silva_orbs.aura_orb_burst"),
			new LookCase("silva_orbs", 1, SkillLook.SILVA_ORB, 3, 3, 2, 3).husk(14),
			new LookCase("silva_orbs", 2, SkillLook.SILVA_PRESENCE, 14, 7, 5, 9).lift(1.5),
			new LookCase("silva_orbs", 3, List.of(SkillLook.ASSASSIN_HAND, SkillLook.SILVA_ORB), 2, 4, 1.5, 3),
			new LookCase("silva_orbs", 3, SkillLook.ORB_BURST, 7, 5, 2, 6).shot("silva_orbs.assassins_judgment_burst"),
			new LookCase("meruem_tail", 0, SkillLook.TAIL_PIERCE, 3, 6, 2, 2),
			new LookCase("meruem_tail", 1, SkillLook.PHOTON, 5, 4, 5, 9),
			new LookCase("meruem_tail", 2, SkillLook.KINGS_AURA, 16, 6, 4, -12).lift(2),
			new LookCase("meruem_tail", 3, SkillLook.KING_ROSE, 14, 6, 8, 14).lift(3),
			new LookCase("hundred_type_guanyin", 0, SkillLook.NETERO_PRAYER, 14, 4, 2, -6).lift(1),
			new LookCase("hundred_type_guanyin", 1, SkillLook.GUANYIN_SWEEP, 4, 2, 10, 6),
			new LookCase("hundred_type_guanyin", 2, SkillLook.PALM_SLAM, 5, 5, 5, 10),
			new LookCase("hundred_type_guanyin", 3, List.of(SkillLook.ZERO_HAND, SkillLook.ZERO_BEAM), 14, 8, 6, -14).lift(3),
			new LookCase("hundred_type_guanyin", 3, SkillLook.ZERO_BEAM, 6, 8, 3, 3).shot("hundred_type_guanyin.zero_hand_beam"),
			new LookCase("illumi_needles", 0, List.of(SkillLook.NEEDLE_RAIN, SkillLook.NEN_NEEDLE), 20, 5, 5, 9),
			new LookCase("illumi_needles", 1, SkillLook.NEN_NEEDLE, 2, 2, 0.6, 2).husk(14),
			new LookCase("illumi_needles", 2, SkillLook.NEEDLE_DISGUISE, 14, 2, 0.5, -3).lift(0.4),
			new LookCase("illumi_needles", 3, SkillLook.NEEDLE_PUPPET, 8, 3, 1.5, 4).husk(4)
		);
		checkClassLooks(context, server, connection, JobClass.HUNTER, cases, SkillLook.GODSPEED_STEP, SkillLook.NEEDLE_PUPPET, "hundred_type_guanyin.zero_hand");
	}

	/**
	 * Casts each case's skill as a tier 4 {@code job} with a big husk in front, checks the client sees its looks, then freezes ticking
	 * and photographs the look from an invisible armor stand (the caster stays where the skill left them), by night and by day; the
	 * shot named {@code fabulous} is taken again with improved transparency (the Fabulous preset draws blended types through
	 * order-independent transparency, which crashed on a type without OIT pipelines until 0.5.36). Fails when a look from {@code first}
	 * to {@code last} is never shown.
	 */
	private static void checkClassLooks(
		final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection, final JobClass job, final List<LookCase> cases,
		final SkillLook first, final SkillLook last, final String fabulous
	) {
		server.runCommand("time set midnight");
		server.runCommand("kill @e[tag=" + TARGET + "]");
		Set<SkillLook> seen = new HashSet<>();
		for (LookCase look : cases) {
			Skill skill = JobWeapons.def(look.weapon()).skills().get(look.slot());
			server.runOnServer(s -> {
				ServerPlayer player = connection.getServerPlayer();
				ServerLevel level = player.level();
				for (Entity e : level.getEntitiesOfClass(Entity.class, player.getBoundingBox().inflate(48),
					e -> e instanceof SkillVfx || e instanceof SkillProjectile || e.entityTags().contains(TARGET) || e.entityTags().contains(Actions.SUMMON_TAG))) {
					e.discard();
				}
				JobProgression.set(player, JobProgression.get(player).withJob(job, 4).withProgress(100, 0));
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JobWeapons.item(JobWeapons.def(look.weapon()))));
				JobStats.refresh(player);
				player.removeAllEffects();
				player.setHealth(player.getMaxHealth());
				player.connection.teleport(0.5, -60, 0.5, 180.0F, 0.0F);
				LivingEntity husk = spawnTarget(level, 0.5, -60, 0.5 - look.husk());
				husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(4000.0);
				husk.setHealth(4000.0F);
			});
			// the last skill's particles would cloud this one's picture
			context.runOnClient(minecraft -> minecraft.particleEngine.clearParticles());
			context.waitTicks(4);
			LookAreaOverlay.record(skill);
			SkillCaster.Result cast = server.computeOnServer(s -> {
				fill(connection.getServerPlayer());
				return SkillCaster.tryCast(connection.getServerPlayer(), look.slot());
			});
			require(cast == SkillCaster.Result.OK, skill.id() + " could not be cast for its look: " + cast);
			context.waitTicks(look.ticks());
			for (SkillLook want : look.looks()) {
				require(context.computeOnClient(minecraft -> lookCount(minecraft, want)) > 0, "the client sees no " + want + " after casting " + skill.id());
				seen.add(want);
			}
			server.runCommand("tick freeze");
			Vec3 focus = context.computeOnClient(minecraft -> look.focus() == SkillLook.NONE ? minecraft.player.position().add(0, 1.2, 0) : lookFocus(minecraft, look.focus()));
			require(focus != null, "no " + look.focus() + " on the client after casting " + skill.id());
			Vec3 aim = focus.add(0, look.lift(), 0);
			String name = "look_" + (look.shot().isEmpty() ? skill.id() : look.shot());
			cameraShot(context, server, connection, aim, aim.add(look.side(), look.up(), look.back()), name);
			if (name.equals("look_" + fabulous)) {
				context.runOnClient(minecraft -> minecraft.options.improvedTransparency().set(true));
				context.waitTicks(5);
				cameraShot(context, server, connection, aim, aim.add(look.side(), look.up(), look.back()), name + "_fabulous");
				context.runOnClient(minecraft -> minecraft.options.improvedTransparency().set(false));
				context.waitTicks(5);
			}
			areaShot(context, server, connection, focus, "look_area_" + name.substring("look_".length()));
			LookAreaOverlay.stop();
			server.runCommand("tick unfreeze");
		}
		context.waitTicks(10);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			for (Entity e : player.level().getEntitiesOfClass(Entity.class, player.getBoundingBox().inflate(48),
				e -> e instanceof SkillVfx || e instanceof SkillProjectile || e.entityTags().contains(Actions.SUMMON_TAG))) {
				e.discard();
			}
			player.removeAllEffects();
			player.setHealth(player.getMaxHealth());
			player.connection.teleport(0.5, -60, 0.5, 180.0F, 0.0F);
		});
		server.runCommand("kill @e[tag=" + TARGET + "]");
		server.runCommand("time set noon");
		for (SkillLook want : SkillLook.values()) {
			if (want.ordinal() >= first.ordinal() && want.ordinal() <= last.ordinal()) {
				require(seen.contains(want), want + " is never shown by a tier 4 " + job + " skill");
			}
		}
		MinecraftMode.LOGGER.info("[job] every tier 4 {} skill draws its look on the client ({} looks)", job, seen.size());
	}

	/**
	 * Every tier 4 look is drawn the size of what its skill really hits (0.5.43, {@link LookRanges}): each look has an audited edge
	 * ({@link LookEdges}), every ring, cone and beam edge sits within 10% of the area of its step (the radius, range, arc and length the
	 * actions use, {@link HitArea}), and no area that hurts or helps is left undrawn. Catches a look resized by eye or an action retuned
	 * without its look; {@code docs/SKILL-LOOKS.md} is the same table.
	 */
	private static void checkLookRanges() {
		List<LookRanges.Row> rows = LookRanges.tier4();
		require(rows.size() == 7 * 9 * 4, "expected 252 tier 4 skills, found " + rows.size());
		List<String> wrong = new ArrayList<>();
		for (LookRanges.Row row : rows) {
			for (LookRanges.Drawn d : row.drawn()) {
				if (d.verdict() == LookRanges.Verdict.MISMATCH || d.verdict() == LookRanges.Verdict.UNAUDITED) {
					wrong.add(row.skill().id() + ": " + d.look() + " " + d.drawn() + " vs " + (d.matched() == null ? "nothing" : d.matched().describe()) + " ("
						+ d.verdict() + ")");
				}
			}
			for (HitArea area : row.undrawn()) {
				wrong.add(row.skill().id() + ": " + area.describe() + " is not drawn");
			}
		}
		require(wrong.isEmpty(), "tier 4 looks off their real areas:\n" + String.join("\n", wrong));
		long rings = rows.stream().flatMap(r -> r.drawn().stream()).filter(d -> d.verdict() == LookRanges.Verdict.MATCH).count();
		MinecraftMode.LOGGER.info("[job] every tier 4 look sits on its real area ({} edges compared over {} skills)", rings, rows.size());
	}

	/**
	 * The skill's real hit areas so far ({@link HitArea#trace}, drawn by {@link LookAreaOverlay} in dashed lime and magenta on top of
	 * everything) over its look, from high above (by night): the drawn edge should lie on the dashes. Nothing for a skill without an area.
	 */
	private static void areaShot(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection, final Vec3 look,
		final String name) {
		AABB box = LookAreaOverlay.bounds();
		if (box == null) {
			return;
		}
		Vec3 center = new Vec3((box.minX + box.maxX) / 2, box.minY, (box.minZ + box.maxZ) / 2);
		double half = Math.max(box.getXsize(), box.getZsize()) / 2 + 1.5;
		double height = half / 0.6 + 2.0;
		context.runOnClient(minecraft -> LookAreaOverlay.show(minecraft, true));
		cameraShot(context, server, connection, center, center.add(0, height, height * 0.25), name, false);
		context.runOnClient(minecraft -> LookAreaOverlay.show(minecraft, false));
	}

	/** What the camera looks at for {@code look}:the middle of a beam or cone, a trail's middle, a projectile, else above its spot. */
	private static @Nullable Vec3 lookFocus(final Minecraft minecraft, final SkillLook look) {
		for (Entity e : minecraft.level.entitiesForRendering()) {
			if (e instanceof SkillProjectile shot && shot.look() == look) {
				return shot.position();
			}
			if (e instanceof SkillVfx vfx && vfx.look() == look) {
				// the client's own player is ahead of the server's copy (a leap), so held looks are framed where the client draws them
				Entity holder = vfx.anchor();
				Vec3 at = vfx.position();
				Vec3 reach = new Vec3(vfx.reach().x(), vfx.reach().y(), vfx.reach().z());
				if (look.hold() == SkillLook.Hold.TRAIL) {
					return holder == null ? at.add(0, 1.0, 0) : at.add(holder.position()).scale(0.5).add(0, 1.0, 0);
				}
				if (holder != null) {
					return holder.position().add(0, 1.2, 0);
				}
				if (look == SkillLook.SPEAR_KILL) {
					return at.add(reach.scale(0.9));
				}
				if (reach.length() > 1.5) {
					return at.add(reach.scale(0.5));
				}
				return at.add(0, 1.2, 0);
			}
		}
		return null;
	}

	/**
	 * A screenshot without the HUD from {@code eye} looking at {@code focus}, through an invisible armor stand so the caster stays in
	 * the picture (ticking frozen, so the effect stays put).
	 */
	private static void cameraShot(
		final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection, final Vec3 focus, final Vec3 eye,
		final String name
	) {
		cameraShot(context, server, connection, focus, eye, name, true);
	}

	/** {@link #cameraShot}, by night only unless {@code dayToo}. */
	private static void cameraShot(
		final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection, final Vec3 focus, final Vec3 eye,
		final String name, final boolean dayToo
	) {
		int id = server.computeOnServer(s -> {
			ServerLevel level = connection.getServerPlayer().level();
			ArmorStand stand = EntityTypes.ARMOR_STAND.create(level, EntitySpawnReason.COMMAND);
			require(stand != null, "could not create the camera stand");
			Vec3 d = focus.subtract(eye);
			float yaw = (float)(Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG);
			float pitch = (float)(-Mth.atan2(d.y, d.horizontalDistance()) * Mth.RAD_TO_DEG);
			stand.setInvisible(true);
			stand.setNoGravity(true);
			stand.addTag(CAMERA);
			stand.snapTo(eye.x, eye.y - stand.getEyeHeight(), eye.z, yaw, pitch);
			stand.setYHeadRot(yaw);
			stand.setYBodyRot(yaw);
			level.addFreshEntity(stand);
			return stand.getId();
		});
		for (int i = 0; i < 40 && !context.computeOnClient(minecraft -> minecraft.level.getEntity(id) != null); i++) {
			context.waitTicks(1);
		}
		context.runOnClient(minecraft -> {
			Entity stand = minecraft.level.getEntity(id);
			require(stand != null, "the client never saw the camera stand for " + name);
			hideHud(minecraft, true);
			minecraft.options.setCameraType(CameraType.FIRST_PERSON);
			minecraft.setCameraEntity(stand);
		});
		context.waitTicks(3);
		context.takeScreenshot(name);
		if (dayToo) {
			// again by day: dark bodies (Tensa Zangetsu, Mugetsu, Kurohitsugi) only show against light
			server.runCommand("time set noon");
			context.waitTicks(2);
			context.takeScreenshot(name + "_day");
			server.runCommand("time set midnight");
		}
		context.runOnClient(minecraft -> {
			minecraft.setCameraEntity(minecraft.player);
			hideHud(minecraft, false);
		});
		server.runCommand("kill @e[tag=" + CAMERA + "]");
	}

	/** A screenshot without the HUD in first person from {@code eye} (ticking frozen, so the effect stays put). */
	private static void lookShot(
		final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection, final Vec3 eye, final float yaw,
		final float pitch, final String name
	) {
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.connection.teleport(eye.x, eye.y - player.getEyeHeight(), eye.z, yaw, pitch);
		});
		context.runOnClient(minecraft -> {
			hideHud(minecraft, true);
			minecraft.options.setCameraType(CameraType.FIRST_PERSON);
		});
		context.waitTicks(3);
		context.takeScreenshot(name);
		context.runOnClient(minecraft -> hideHud(minecraft, false));
	}

	private static void hideHud(final Minecraft minecraft, final boolean hidden) {
		if (minecraft.gui.hud.isHidden() != hidden) {
			minecraft.gui.hud.toggle();
		}
	}

	private static int lookCount(final Minecraft minecraft, final SkillLook look) {
		int n = 0;
		for (Entity e : minecraft.level.entitiesForRendering()) {
			if (e instanceof SkillProjectile p && p.look() == look || e instanceof SkillVfx v && v.look() == look) {
				n++;
			}
		}
		return n;
	}

	/**
	 * Rain and arrows: Unlimited Blade Works under a roof five blocks up drops its blades on the one husk in its circle (they used
	 * to scatter over the circle and stop at the roof), class arrows land through the damage cooldown (a volley's second arrow
	 * used to bounce off), and a bow shot from inside a pressed husk hits it.
	 */
	/**
	 * Skills crit now (a tier 4 rogue's 15%), a skill summon bites with its caster's power instead of vanilla's, an area hit on more
	 * than five shares out, and the training dummy measures it all without dying.
	 */
	private static void checkCritsAndDummy(final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = player.level();
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.ROGUE, 4).withProgress(100, 0));
			JobStats.refresh(player);
			TrainingDummy dummy = ModEntities.TRAINING_DUMMY.create(level, EntitySpawnReason.COMMAND);
			dummy.snapTo(player.getX() + 3, player.getY(), player.getZ(), 0, 0);
			level.addFreshEntity(dummy);
			require(CombatHooks.isEnemy(player, dummy), "skills should treat the training dummy as an enemy");
			for (int i = 0; i < 200; i++) {
				CombatHooks.deal(player, dummy, 10.0F, player.damageSources().playerAttack(player), CombatHooks.DamageKind.SKILL);
			}
			DummyMeter.Session session = DummyMeter.session(player);
			require(session != null && session.hits() == 200, "the dummy should count 200 skill hits, got " + (session == null ? 0 : session.hits()));
			require(session.crits() > 0 && session.total() > 2000.0F, "skill hits should crit: " + session.crits() + " crits, " + session.total() + " damage");
			require(dummy.isAlive() && dummy.getHealth() == dummy.getMaxHealth(), "the dummy never takes damage");
			// a wolf summoned at power 10 bites for 10 x its share, not vanilla's 4
			var wolf = EntityTypes.WOLF.create(level, EntitySpawnReason.COMMAND);
			wolf.snapTo(player.getX() + 2, player.getY(), player.getZ(), 0, 0);
			level.addFreshEntity(wolf);
			Actions.markSummon(wolf, player, 10.0);
			float before = session.total();
			dummy.hurtServer(level, level.damageSources().mobAttack(wolf), 4.0F);
			float bite = session.total() - before;
			float share = (float)(10.0 * Actions.Summon.WOLF.share());
			require(Math.abs(bite - share) < 0.01F || bite > share * 1.49F && bite < share * 3.0F,
				"a summoned wolf should bite for " + share + " (or a crit of it), bit " + bite);
			wolf.discard();
			dummy.discard();
			require(SkillContext.areaShare(5) == 1.0 && Math.abs(SkillContext.areaShare(20) - 0.5) < 1.0E-9, "an area hit on 20 should give each half");
			return session.crits() + " crits in 200 skill hits, wolf bite " + bite;
		});
		MinecraftMode.LOGGER.info("[job] crits and dummy: {}", report);
	}

	/**
	 * Support skills boost damage instead of defense (0.5.20): Iron Wall gives +20% damage dealt, which every hit reads, and
	 * Whirlwind gives both of its boosts (its two stances used to overwrite each other, so only the second ever counted).
	 */
	private static void checkBoosts(final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = player.level();
			JobData saved = JobProgression.get(player);
			ItemStack hand = player.getMainHandItem().copy();
			JobProgression.set(player, saved.withJob(JobClass.WARRIOR, 1).withProgress(10, 0));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JobWeapons.item(JobWeapons.def("iron_greatsword"))));
			JobStats.refresh(player);
			JobProgression.set(player, JobProgression.get(player).withMana(JobStats.maxMana(player)));
			GearStats.invalidate(player);
			require(GearStats.of(player).get(EngraveStat.CRIT_CHANCE) == 0.0F, "a level 10 warrior should have no crit chance for this check");
			TrainingDummy dummy = ModEntities.TRAINING_DUMMY.create(level, EntitySpawnReason.COMMAND);
			dummy.snapTo(player.getX() + 3, player.getY(), player.getZ(), 0, 0);
			level.addFreshEntity(dummy);
			float plain = skillHit(player, dummy);
			require(SkillCaster.tryCast(player, 1) == SkillCaster.Result.OK, "Iron Wall should cast");
			GearStats.invalidate(player);
			require(player.hasEffect(ModEffects.VALOR) && GearStats.of(player).get(EngraveStat.DAMAGE) == 20.0F, "Iron Wall should give +20% damage dealt");
			float boosted = skillHit(player, dummy);
			require(Math.abs(boosted - plain - 2.0F) < 0.01F, "a 10-damage skill hit should deal 2 more under +20% damage: " + plain + " -> " + boosted);
			player.removeEffect(ModEffects.VALOR);

			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.HUNTER, 4).withProgress(100, 0));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JobWeapons.item(JobWeapons.def("godspeed"))));
			JobStats.refresh(player);
			JobProgression.set(player, JobProgression.get(player).withMana(JobStats.maxMana(player)));
			require(SkillCaster.tryCast(player, 1) == SkillCaster.Result.OK, "Whirlwind should cast");
			require(player.hasEffect(ModEffects.INSIGHT) && player.hasEffect(ModEffects.FEROCITY), "Whirlwind should give both its crit chance and crit damage boosts");
			player.removeEffect(ModEffects.INSIGHT);
			player.removeEffect(ModEffects.FEROCITY);
			dummy.discard();
			player.setItemInHand(InteractionHand.MAIN_HAND, hand);
			JobProgression.set(player, saved);
			JobStats.refresh(player);
			return "skill hit " + plain + " -> " + boosted;
		});
		MinecraftMode.LOGGER.info("[job] boosts: {}", report);
	}

	/** What one 10-damage skill hit on {@code dummy} counts for after the caster's bonuses. */
	private static float skillHit(final ServerPlayer player, final TrainingDummy dummy) {
		DummyMeter.Session session = DummyMeter.session(player);
		float before = session == null ? 0.0F : session.total();
		CombatHooks.deal(player, dummy, 10.0F, player.damageSources().playerAttack(player), CombatHooks.DamageKind.SKILL);
		return DummyMeter.session(player).total() - before;
	}

	private static void checkVolleys(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("fill 14 -55 14 26 -55 26 minecraft:stone");
		LivingEntity[] roofed = new LivingEntity[1];
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.ARCHER, 4).withProgress(100, 0));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JobWeapons.item(JobWeapons.def("emiya_black_bow"))));
			JobStats.refresh(player);
			player.snapTo(20.5, -60, 24.5, 180.0F, 0.0F);
			player.connection.teleport(20.5, -60, 24.5, 180.0F, 0.0F);
			roofed[0] = spawnTarget(player.level(), 20.5, -60, 18.5);
		});
		context.waitTicks(2);
		SkillCaster.Result cast = server.computeOnServer(s -> {
			fill(connection.getServerPlayer());
			return SkillCaster.tryCast(connection.getServerPlayer(), 2);
		});
		require(cast == SkillCaster.Result.OK, "Unlimited Blade Works could not be cast: " + cast);
		context.waitTicks(50);
		float[] blades = server.computeOnServer(s -> new float[] {400.0F - roofed[0].getHealth(), JobWeapons.def("emiya_black_bow").skillPower() * 0.35F});
		require(blades[0] >= 4 * blades[1], "Unlimited Blade Works under a roof took " + blades[0] + " health, less than 4 blades of " + blades[1]);
		server.runCommand("kill @e[tag=" + TARGET + "]");
		server.runCommand("fill 14 -55 14 26 -55 26 minecraft:air");

		float[] drops = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = player.level();
			WeaponDef def = JobWeapons.def("hunter_shortbow");
			ItemStack bow = new ItemStack(JobWeapons.item(def));
			player.setItemInHand(InteractionHand.MAIN_HAND, bow);
			JobStats.refresh(player);
			LivingEntity husk = spawnTarget(level, 0.5, -60, -3.5);
			SkillContext ctx = new SkillContext(player, def, def.skills().get(0), bow, EngraveTotals.EMPTY, 0.0);
			float[] lost = new float[4];
			for (int i = 0; i < lost.length; i++) {
				Arrow arrow = new Arrow(level, player, new ItemStack(Items.ARROW), bow);
				if (i < 2) {
					CombatHooks.trackSkillArrow(arrow, ctx, 1.0);
				} else {
					CombatHooks.trackArrow(arrow, 6.0F);
				}
				float before = husk.getHealth();
				husk.hurtServer(level, player.damageSources().arrow(arrow, player), 1.0F);
				lost[i] = before - husk.getHealth();
			}
			husk.discard();
			return lost;
		});
		for (int i = 0; i < drops.length; i++) {
			require(drops[i] > 0.0F, (i < 2 ? "skill" : "bow") + " arrow " + (i + 1) + " in the same tick did no damage (damage cooldown)");
		}

		LivingEntity[] pressed = new LivingEntity[1];
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.snapTo(0.5, -60, 0.5, 180.0F, 0.0F);
			player.connection.teleport(0.5, -60, 0.5, 180.0F, 0.0F);
			pressed[0] = spawnTarget(player.level(), 0.5, -60, 0.35);
			BasicAttacks.loose(player, player.getMainHandItem(), JobWeapons.def(player.getMainHandItem()), 1.0F);
		});
		context.waitTicks(5);
		float arrowed = server.computeOnServer(s -> pressed[0].getHealth());
		require(arrowed < 400.0F, "a bow shot from inside a pressed husk went through it");
		server.runCommand("kill @e[tag=" + TARGET + "]");

		// the arrows of one basic shot hit each enemy once: of a fan of four (three Multishot lines) the husk in front takes the middle one
		// (three used to land), and an arrow passing through it goes on to the husk beside and behind it
		LivingEntity[] fanned = new LivingEntity[2];
		float[] arrowHit = new float[3];
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			// tier 3: no treasure blades joining the shot
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.ARCHER, 3).withProgress(100, 0));
			WeaponDef def = JobWeapons.def("hunter_shortbow");
			ItemStack bow = new ItemStack(JobWeapons.item(def));
			bow.set(ModDataComponents.ENGRAVINGS, new Engravings(List.of("multishot", "multishot", "multishot"), 1));
			player.setItemInHand(InteractionHand.MAIN_HAND, bow);
			JobStats.refresh(player);
			player.snapTo(0.5, -60, 0.5, 180.0F, 0.0F);
			player.connection.teleport(0.5, -60, 0.5, 180.0F, 0.0F);
			fanned[0] = spawnTarget(player.level(), 0.5, -60, -2.5);
			fanned[1] = spawnTarget(player.level(), 1.35, -60, -5.5);
			EngraveTotals mods = JobWeapons.activeTotals(player);
			arrowHit[0] = mods.get(EngraveStat.EXTRA_SHOT);
			arrowHit[1] = def.shotDamage() * (1.0F + BasicAttacks.shotBonus(JobProgression.get(player), mods)) * (1.0F + BasicAttacks.EAGLE_EYE);
			arrowHit[2] = BasicAttacks.shotBonus(JobProgression.get(player), mods) - mods.fraction(EngraveStat.SHOT_DAMAGE);
		});
		require(arrowHit[0] == 3.0F, "three Multishot lines should add 3 arrows, got " + arrowHit[0]);
		// what the extra arrows are worth on a lone target
		require(Math.abs(arrowHit[2] - 3 * BasicAttacks.EXTRA_SHOT_DAMAGE / 100.0F) < 1.0E-4F,
			"three extra arrows should add " + 3 * BasicAttacks.EXTRA_SHOT_DAMAGE + "% basic shot damage, got " + arrowHit[2] * 100.0F + "%");
		context.waitTicks(2);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.setYRot(180.0F);
			player.setXRot(0.0F);
			BasicAttacks.loose(player, player.getMainHandItem(), JobWeapons.def(player.getMainHandItem()), 1.0F);
		});
		context.waitTicks(8);
		float[] fanLost = server.computeOnServer(s -> new float[] {400.0F - fanned[0].getHealth(), 400.0F - fanned[1].getHealth()});
		// a crit adds half again; a second arrow would double it
		require(fanLost[0] > 0.0F && fanLost[0] < arrowHit[1] * 1.75F, "the husk in front should take one arrow of " + arrowHit[1] + " from a fan of four, took " + fanLost[0]);
		require(fanLost[1] > 0.0F, "an arrow of the fan should pass the husk in front and hit the one beside it");
		server.runCommand("kill @e[tag=" + TARGET + "]");

		// a homing fan splits the enemies between its shots: a lone husk takes Flower Storm's middle orb, not all seven of them
		LivingEntity[] lone = new LivingEntity[1];
		float[] orb = new float[1];
		int storm = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.MAGE, 4).withProgress(100, 0));
			WeaponDef merlin = JobWeapons.def("merlin_staff");
			ItemStack staff = new ItemStack(JobWeapons.item(merlin));
			player.setItemInHand(InteractionHand.MAIN_HAND, staff);
			JobStats.refresh(player);
			player.snapTo(0.5, -60, 0.5, 180.0F, 0.0F);
			player.connection.teleport(0.5, -60, 0.5, 180.0F, 0.0F);
			lone[0] = spawnTarget(player.level(), 0.5, -60, -6.5);
			int slot = 0;
			while (!merlin.skills().get(slot).id().equals("merlin_staff.flower_storm")) {
				slot++;
			}
			orb[0] = new SkillContext(player, merlin, merlin.skills().get(slot), staff, JobWeapons.activeTotals(player), 0.15).damageFor(1.85);
			return slot;
		});
		context.waitTicks(2);
		SkillCaster.Result flowers = server.computeOnServer(s -> {
			fill(connection.getServerPlayer());
			return SkillCaster.tryCast(connection.getServerPlayer(), storm);
		});
		require(flowers == SkillCaster.Result.OK, "Flower Storm could not be cast: " + flowers);
		context.waitTicks(40);
		float stormed = server.computeOnServer(s -> 400.0F - lone[0].getHealth());
		require(stormed > 0.0F && stormed < 3.0F * orb[0], "Flower Storm should hit a lone husk with about one orb of " + orb[0] + ", took " + stormed);
		server.runCommand("kill @e[tag=" + TARGET + "]");

		// every shot of a fan hits each enemy once: point blank, all three of Lake Slash's waves used to land on the husk in front
		LivingEntity[] close = new LivingEntity[1];
		float[] wave = new float[1];
		int lake = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.WARRIOR, 4).withProgress(100, 0));
			WeaponDef arondight = JobWeapons.def("arondight");
			ItemStack sword = new ItemStack(JobWeapons.item(arondight));
			player.setItemInHand(InteractionHand.MAIN_HAND, sword);
			JobStats.refresh(player);
			player.snapTo(0.5, -60, 0.5, 180.0F, 0.0F);
			player.connection.teleport(0.5, -60, 0.5, 180.0F, 0.0F);
			close[0] = spawnTarget(player.level(), 0.5, -60, -1.5);
			int slot = 0;
			while (!arondight.skills().get(slot).id().equals("arondight.lake_slash")) {
				slot++;
			}
			wave[0] = new SkillContext(player, arondight, arondight.skills().get(slot), sword, JobWeapons.activeTotals(player), 0.0).damageFor(1.65);
			return slot;
		});
		context.waitTicks(2);
		SkillCaster.Result waves = server.computeOnServer(s -> {
			fill(connection.getServerPlayer());
			return SkillCaster.tryCast(connection.getServerPlayer(), lake);
		});
		require(waves == SkillCaster.Result.OK, "Lake Slash could not be cast: " + waves);
		context.waitTicks(20);
		float slashed = server.computeOnServer(s -> 400.0F - close[0].getHealth());
		require(slashed > 0.0F && slashed < 1.75F * wave[0], "Lake Slash should hit a husk two blocks ahead with one wave of " + wave[0] + ", took " + slashed);
		server.runCommand("kill @e[tag=" + TARGET + "]");
		MinecraftMode.LOGGER.info("[job] volleys: {} health from blades under a roof, arrows through the damage cooldown {}, bow shot from inside a husk hit,"
			+ " a fan of four took {} off the husk in front (one arrow {}) and {} off the one beside it, Flower Storm took {} off a lone husk (one orb {}),"
			+ " Lake Slash took {} off a husk two blocks ahead (one wave {})",
			blades[0], java.util.Arrays.toString(drops), fanLost[0], arrowHit[1], fanLost[1], stormed, orb[0], slashed, wave[0]);
	}

	/**
	 * Aim that holds: a cannonball barrage reaches a husk 20 blocks away (the balls used to drop short), and a strike after the
	 * caster turned away still lands on the husk aimed at when the skill started (it used to follow the crosshair).
	 */
	private static void checkLockedAim(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		SkillContext[] ctx = new SkillContext[1];
		LivingEntity[] far = new LivingEntity[1];
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.PIRATE, 4).withProgress(100, 0));
			WeaponDef def = JobWeapons.def("el_draque");
			ItemStack pistol = new ItemStack(JobWeapons.item(def));
			player.setItemInHand(InteractionHand.MAIN_HAND, pistol);
			JobStats.refresh(player);
			player.snapTo(0.5, -60, 0.5, 180.0F, 0.0F);
			player.connection.teleport(0.5, -60, 0.5, 180.0F, 0.0F);
			far[0] = spawnTarget(player.level(), 0.5, -60, -19.5);
			ctx[0] = new SkillContext(player, def, def.skills().get(0), pistol, EngraveTotals.EMPTY, 0.0);
			Actions.barrage(ProjectileStyle.CANNONBALL, 6, 1.0).run(ctx[0]);
		});
		context.waitTicks(30);
		float[] balls = server.computeOnServer(s -> new float[] {400.0F - far[0].getHealth(), (float)ctx[0].damageFor(1.0)});
		require(balls[0] >= 3 * balls[1] * 0.5F, "a cannonball barrage at a husk 20 blocks away took " + balls[0] + ", under 3 balls of " + balls[1]);
		server.runCommand("kill @e[tag=" + TARGET + "]");

		LivingEntity[] aimed = new LivingEntity[1];
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.snapTo(0.5, -60, 0.5, 180.0F, 10.0F);
			player.connection.teleport(0.5, -60, 0.5, 180.0F, 10.0F);
			aimed[0] = spawnTarget(player.level(), 0.5, -60, -7.5);
			WeaponDef def = JobWeapons.def("el_draque");
			ctx[0] = new SkillContext(player, def, def.skills().get(0), player.getMainHandItem(), EngraveTotals.EMPTY, 0.0);
			// turn around before the bombardment comes down
			player.setYRot(0.0F);
			Actions.strike(22, 2, 1, 1.0).run(ctx[0]);
		});
		context.waitTicks(15);
		float struck = server.computeOnServer(s -> aimed[0].getHealth());
		require(struck < 400.0F, "a strike after turning away did not land on the husk aimed at when the skill started");
		server.runCommand("kill @e[tag=" + TARGET + "]");
		MinecraftMode.LOGGER.info("[job] locked aim: barrage cannonballs took {} at 20 blocks, a strike after turning away hit the aimed husk", balls[0]);
	}

	/**
	 * Mobs vanilla makes dodge or shrug off projectiles take class shots: an enderman (teleports away), a breeze (reflects) and
	 * the magic bolt; the armored Wither (below half health) and a closed shulker and a class arrow.
	 */
	private static void checkProjectileImmunities(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		require(shotLands(context, server, connection, EntityTypes.ENDERMAN, "apprentice_staff", 0.0F, 0.0F), "a magic bolt did not hit an enderman");
		require(shotLands(context, server, connection, EntityTypes.BREEZE, "apprentice_staff", 0.0F, 0.0F), "a magic bolt did not hit a breeze");
		require(shotLands(context, server, connection, EntityTypes.ENDERMAN, "hunter_shortbow", 0.0F, 0.0F), "a class arrow did not hit an enderman");
		require(shotLands(context, server, connection, EntityTypes.WITHER, "hunter_shortbow", 0.0F, 140.0F), "a class arrow did not hit the armored Wither");
		require(shotLands(context, server, connection, EntityTypes.SHULKER, "hunter_shortbow", 14.0F, 0.0F), "a class arrow did not hit a closed shulker");
		MinecraftMode.LOGGER.info("[job] class shots hit an enderman, a breeze, the armored Wither and a closed shulker");
	}

	/** Fires the weapon's basic shot at a {@code type} 4 blocks ahead (health set when above 0); true when it lost health in place. */
	private static boolean shotLands(
		final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection, final EntityType<? extends Mob> type,
		final String weapon, final float pitch, final float health
	) {
		Mob[] target = new Mob[1];
		float[] start = new float[1];
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.connection.teleport(0.5, -60, 0.5, 180.0F, pitch);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JobWeapons.item(JobWeapons.def(weapon))));
			Mob mob = type.create(player.level(), EntitySpawnReason.COMMAND);
			require(mob != null, "could not create " + type);
			mob.snapTo(0.5, -60, -3.5, 0.0F, 0.0F);
			mob.setNoAi(true);
			mob.setPersistenceRequired();
			mob.addTag(TARGET);
			player.level().addFreshEntity(mob);
			if (health > 0.0F) {
				mob.setHealth(health);
			}
			target[0] = mob;
			start[0] = mob.getHealth();
		});
		context.waitTicks(2);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.setYRot(180.0F);
			player.setXRot(pitch);
			ItemStack stack = player.getMainHandItem();
			WeaponDef def = JobWeapons.def(stack);
			if (def.archetype().shot() == ProjectileStyle.ARROW) {
				BasicAttacks.loose(player, stack, def, 1.0F);
			} else {
				BasicAttacks.shoot(player, stack, def);
			}
		});
		context.waitTicks(6);
		boolean landed = server.computeOnServer(s -> target[0].getHealth() < start[0] && target[0].distanceToSqr(0.5, -60, -3.5) < 1.0);
		server.runCommand("kill @e[tag=" + TARGET + "]");
		context.waitTicks(2);
		return landed;
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
		require(offers[0] == warriorItems + 3, "a Lv 25 warrior should see " + warriorItems + " gear offers + 3 extras (essence, condensed essence, reset scroll), got " + offers[0]);
		require(offers[1] == starterItems + 3, "players without a class should see " + starterItems + " starter offers + 3 extras, got " + offers[1]);

		// a boss kill: condensed essence, golem core, a share of a level measured at its trial's level (every trial boss has one)
		for (var type : Quests.BOSSES) {
			require(JobEvents.trialBoss(type) != null, "trial boss " + type + " has no experience share");
		}
		server.runCommand("summon minecraft_mode:mythril_golem 0.5 -60 -6.5 {NoAI:1b}");
		context.waitTicks(3);
		int[] drops = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			Mob golem = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(12), e -> e.getType() == com.minecraftmode.registry.ModEntities.MYTHRIL_GOLEM).getFirst();
			JobEvents.TrialBossExp trial = JobEvents.trialBoss(golem.getType());
			int expected = JobProgression.levelExp(player, trial.level(), trial.share());
			int before = JobProgression.get(player).exp() + totalExp(JobProgression.get(player));
			golem.hurtServer(player.level(), player.damageSources().playerAttack(player), 10000.0F);
			int after = JobProgression.get(player).exp() + totalExp(JobProgression.get(player));
			return new int[] {after - before, expected};
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
		require(drops[0] == drops[1] && drops[0] < 300, "a mythril golem should give " + drops[1] + " class exp (a share of a level, not twice its health), got " + drops[0]);
		require(items[0] >= 2 && items[1] == 1, "mythril golem drops: condensed essence " + items[0] + ", golem core " + items[1]);
		server.runCommand("kill @e[type=minecraft:item]");

		// a Warden drops a fixed 2 condensed essence (shriekers summon the next one at once), not one per 150 health
		server.runCommand("summon minecraft:warden 0.5 -60 -6.5 {NoAI:1b}");
		context.waitTicks(3);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			Mob warden = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(12), e -> e.getType() == EntityTypes.WARDEN).getFirst();
			warden.hurtServer(player.level(), player.damageSources().playerAttack(player), 10000.0F);
		});
		context.waitTicks(5);
		int wardenCondensed = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			int condensed = 0;
			for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(16))) {
				if (item.getItem().is(ModItems.CONDENSED_ESSENCE)) {
					condensed += item.getItem().getCount();
				}
			}
			return condensed;
		});
		require(wardenCondensed == JobEvents.WARDEN_CONDENSED, "a warden should drop " + JobEvents.WARDEN_CONDENSED + " condensed essence, got " + wardenCondensed);
		server.runCommand("kill @e[type=minecraft:item]");

		// experience comes from a monster's own health: a zombie made ten times tougher (a keystone, an event) still pays 20 and is no boss;
		// a named monster pays a share of a level; ores pay a share of the miner's level past the low levels
		String growth = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = player.level();
			Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			zombie.snapTo(player.getX() + 3, player.getY(), player.getZ(), 0, 0);
			zombie.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier(MinecraftMode.id("test_scale"), 9.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			zombie.setHealth(zombie.getMaxHealth());
			level.addFreshEntity(zombie);
			int before = JobProgression.get(player).exp() + totalExp(JobProgression.get(player));
			zombie.hurtServer(level, player.damageSources().playerAttack(player), 10000.0F);
			int zombieExp = JobProgression.get(player).exp() + totalExp(JobProgression.get(player)) - before;
			require(zombieExp == 20, "a zombie with 10x health should pay its own 20 class exp, got " + zombieExp);
			NamedMob named = NamedMobs.type(NamedMobs.byId("bandit_captain")).create(level, EntitySpawnReason.COMMAND);
			named.snapTo(player.getX() - 3, player.getY(), player.getZ(), 0, 0);
			named.applyLevel(15);
			level.addFreshEntity(named);
			int expected = JobProgression.levelExp(player, 15, JobEvents.NAMED_SHARE);
			before = JobProgression.get(player).exp() + totalExp(JobProgression.get(player));
			named.hurtServer(level, player.damageSources().playerAttack(player), 10000.0F);
			int namedExp = JobProgression.get(player).exp() + totalExp(JobProgression.get(player)) - before;
			require(namedExp == expected, "a Lv 15 named monster should pay " + expected + " (a quarter of a Lv 15 level), got " + namedExp);
			int lowIron = JobEvents.oreExp(net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState(), 10);
			int highIron = JobEvents.oreExp(net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState(), 90);
			require(lowIron == 5 && highIron > lowIron * 2, "iron ore should pay 5 at Lv 10 and grow with the level, " + lowIron + " / " + highIron);
			return "zombie x10 health " + zombieExp + ", Lv 15 named " + namedExp + ", iron Lv 10/90 " + lowIron + "/" + highIron;
		});
		server.runCommand("kill @e[type=minecraft:item]");
		MinecraftMode.LOGGER.info("[job] growth: {}", growth);
		MinecraftMode.LOGGER.info("[job] guild offers follow class/tier; golem kill: {} exp, {} condensed essence, golem core", drops[0], items[0]);
	}

	private static int totalExp(final JobData data) {
		int total = 0;
		for (int level = 1; level < data.level(); level++) {
			total += JobProgression.expToNext(level);
		}
		return total;
	}

	private static void jobScreen(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		context.getInput().pressKey(JobKeys.OPEN_SCREEN);
		context.waitForScreen(JobScreen.class);
		context.waitTicks(3);
		context.takeScreenshot("job_screen");
		context.setScreen(() -> null);
		context.waitTicks(2);
		// the longest level bonus (a Lv 100 pirate: four rewards) still fits above the buttons
		JobData saved = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			JobData data = JobProgression.get(player);
			JobProgression.set(player, data.withJob(JobClass.PIRATE, 4).withProgress(100, 0));
			return data;
		});
		context.waitTicks(3);
		context.getInput().pressKey(JobKeys.OPEN_SCREEN);
		context.waitForScreen(JobScreen.class);
		context.waitTicks(3);
		context.takeScreenshot("job_screen_pirate");
		context.setScreen(() -> null);
		server.runOnServer(s -> {
			JobProgression.set(connection.getServerPlayer(), saved);
			JobStats.refresh(connection.getServerPlayer());
		});
		context.waitTicks(2);
	}

	/** A class weapon's tooltip names its skill power and gives every damaging skill's damage per cast (the effect lines are percentages). */
	private static void weaponTooltip(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		ItemStack saved = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ItemStack old = player.getInventory().getItem(0).copy();
			player.getInventory().setItem(0, new ItemStack(JobWeapons.item(JobWeapons.def("gladiator_longsword"))));
			return old;
		});
		context.waitTicks(5);
		String text = context.computeOnClient(minecraft -> minecraft.player.getInventory().getItem(0)
			.getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player, TooltipFlag.NORMAL).stream().map(Component::getString)
			.collect(Collectors.joining("\n")));
		require(text.contains("Skill power") && text.contains("dmg/cast"), "the weapon tooltip should show the skill power and each skill's damage per cast:\n" + text);
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitForScreen(InventoryScreen.class);
		context.waitTicks(5);
		// the middle of hotbar slot 0 (8, 142 in the inventory panel)
		double[] pos = context.computeOnClient(minecraft -> {
			AbstractContainerScreenAccessor screen = (AbstractContainerScreenAccessor)minecraft.gui.screen();
			double scale = minecraft.getWindow().getGuiScale();
			return new double[] {(screen.minecraftMode$leftPos() + 16) * scale, (screen.minecraftMode$topPos() + 150) * scale};
		});
		context.getInput().setCursorPos(pos[0], pos[1]);
		context.waitTicks(5);
		context.takeScreenshot("job_weapon_tooltip");
		context.setScreen(() -> null);
		server.runOnServer(s -> connection.getServerPlayer().getInventory().setItem(0, saved));
		context.waitTicks(2);
		MinecraftMode.LOGGER.info("[job] weapon tooltip shows skill power and damage per cast");
	}

	/**
	 * The recipe book and creative search build every item's tooltip on a background thread with no player: mod tooltips must not touch
	 * the font or the client stat caches there. Until 0.5.19 searching the crafting table's recipe book (in Korean) crashed the game:
	 * {@code TooltipLayout} measured lines off the render thread, which baked unseen glyphs into the GPU font atlas.
	 */
	private static void searchTooltips(final ClientGameTestContext context) {
		Item.TooltipContext tooltip = context.computeOnClient(minecraft -> Item.TooltipContext.of(minecraft.level));
		List<ItemStack> stacks = new ArrayList<>();
		for (Item item : BuiltInRegistries.ITEM) {
			stacks.add(new ItemStack(item));
		}
		// glyphs no screen has drawn yet, in a line long enough to wrap: measuring them would bake them
		ItemStack unseen = new ItemStack(Items.COMPASS);
		unseen.set(DataComponents.CUSTOM_NAME, Component.literal("龘靐齉 뷁햏 " + "꽑".repeat(80)));
		stacks.add(unseen);
		String weapon = CompletableFuture.supplyAsync(() -> {
			stacks.forEach(stack -> stack.getTooltipLines(tooltip, null, TooltipFlag.NORMAL));
			return new ItemStack(JobWeapons.item(JobWeapons.def("gladiator_longsword"))).getTooltipLines(tooltip, null, TooltipFlag.NORMAL).stream()
				.map(Component::getString).collect(Collectors.joining("\n"));
		}, Util.backgroundExecutor()).join();
		require(weapon.contains("Skill power"), "a class weapon's tooltip built for a search should still list its skills:\n" + weapon);
		// the real path: rebuild the session's search trees and search them as the recipe book and creative tab do
		SessionSearchTrees trees = context.computeOnClient(minecraft -> {
			minecraft.getConnection().searchTrees().rebuildAfterLanguageChange();
			return minecraft.getConnection().searchTrees();
		});
		trees.recipes().search("compass");
		trees.creativeNameSearch().search("compass");
		MinecraftMode.LOGGER.info("[job] {} tooltips build off the render thread for searches", stacks.size());
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
					int before = server.computeOnServer(s -> summonsOf(connection.getServerPlayer()));
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
					// summons must still be there a few ticks later (until 0.5.15 the cleanup of saved summons removed every fresh one)
					int summoned = server.computeOnServer(s -> summonsOf(connection.getServerPlayer())) - before;
					require(summoned == summonCount(skill), skill.id() + " should summon " + summonCount(skill) + ", " + summoned + " are there");
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
		MinecraftMode.LOGGER.info("[job] cast all {} skills of all 182 weapons without errors", casts);
	}

	/** Living skill summons fighting for {@code player}. */
	private static int summonsOf(final ServerPlayer player) {
		return player.level().getEntities((Entity)null, player.getBoundingBox().inflate(32), e -> player.getUUID().equals(Actions.summoner(e))).size();
	}

	/** How many helpers {@code skill} summons, read from its summon steps' tooltip lines. */
	private static int summonCount(final Skill skill) {
		int count = 0;
		for (SkillAction action : skill.actions()) {
			if (action.describe().getContents() instanceof TranslatableContents text && text.getKey().equals("skill_action.minecraft_mode.summon")) {
				count += (Integer)text.getArgs()[0];
			}
		}
		return count;
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
			require(reaper.get(EngraveStat.BOSS_DAMAGE) >= 15 && reaper.get(EngraveStat.SKILL_DAMAGE) >= 8 && reaper.get(EngraveStat.DODGE) >= 5,
				"Soul Reaper tier passives should add boss damage, skill damage and dodge");
			require(reaper.get(EngraveStat.COOLDOWN) >= 8 + 8, "Mugetsu (8%) plus the Lv 80 bonus (8%) should cut cooldowns, got " + reaper.get(EngraveStat.COOLDOWN));
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
			require(hunter.get(EngraveStat.SKILL_DAMAGE) < 10, "Limitation and Vow belongs to tier 4 only");
			require(hunter.get(EngraveStat.CRIT_DAMAGE) >= 15, "the Lv 50 hunter bonus should add 15% crit damage, got " + hunter.get(EngraveStat.CRIT_DAMAGE));
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
