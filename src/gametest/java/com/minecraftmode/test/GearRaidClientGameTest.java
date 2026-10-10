package com.minecraftmode.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.raid.LootScreen;
import com.minecraftmode.client.raid.RaidScreen;
import com.minecraftmode.entity.boss.RaidBoss;
import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMob;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.engrave.EngraveTotals;
import com.minecraftmode.job.engrave.Engraving;
import com.minecraftmode.job.engrave.EngravingMenu;
import com.minecraftmode.job.engrave.Engravings;
import com.minecraftmode.job.gear.ArmorPieceDef;
import com.minecraftmode.job.gear.ArmorSetDef;
import com.minecraftmode.job.gear.ClassArmor;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.GearRules;
import com.minecraftmode.job.gear.GearSlot;
import com.minecraftmode.job.gear.GearStats;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.job.skill.Skill;
import com.minecraftmode.job.skill.SkillCaster;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.loot.EvolutionEtherItem;
import com.minecraftmode.loot.GearDrops;
import com.minecraftmode.loot.GearIndex;
import com.minecraftmode.loot.GearUpgrades;
import com.minecraftmode.network.LootActionPayload;
import com.minecraftmode.raid.Arenas;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.Parties;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.raid.RaidDimension;
import com.minecraftmode.raid.RaidInstance;
import com.minecraftmode.raid.Raids;
import com.minecraftmode.raid.loot.LootSession;
import com.minecraftmode.raid.loot.LootSessions;
import com.minecraftmode.registry.ModItems;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Class armor, set bonuses, the cooldown floor, armor engraving, gear evolution, named monsters,
 * every boss pattern, the raid flow (enter, countdown, kill, rewards, leave), dying in a raid, and
 * loot sharing by auction and dice. Takes screenshots of the raid board, the loot screen, every boss
 * in its arena and the named monsters.
 */
public class GearRaidClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(final ClientGameTestContext context) {
		checkContent(context);
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerConnection connection = singleplayer.getConnection();
			TestServerContext server = singleplayer.getServer();
			connection.waitForChunksRender();
			server.runCommand("difficulty normal");
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("time set noon");
			server.runCommand("gamemode survival @p");
			server.runCommand("tp @p 0.5 -60 0.5 180 0");
			context.waitTicks(5);

			checkArmor(server, connection);
			checkUpgrade(server, connection);
			checkNamed(context, server, connection);
			checkBossPatterns(context, server, connection);
			checkRaidFlow(context, server, connection);
			checkDeathInRaid(context, server, connection);
			checkLoot(context, server, connection);
			raidBoard(context, server, connection);
			arenaShots(context, server, connection);
			WorldClose.prepare(context, server);
		}
	}

	// ------------------------------------------------------------------ content

	private static void checkContent(final ClientGameTestContext context) {
		require(ClassArmor.sets().size() == 70, "expected 70 armor sets, got " + ClassArmor.sets().size());
		require(ClassArmor.pieces().size() == 280, "expected 280 armor pieces, got " + ClassArmor.pieces().size());
		for (ArmorSetDef set : ClassArmor.sets()) {
			List<Integer> pieces = set.bonuses().stream().map(ArmorSetDef.SetBonus::pieces).toList();
			require(pieces.equals(List.of(2, 3, 4)), set.id() + " should have 2/3/4 piece bonuses, got " + pieces);
		}
		require(NamedMobs.all().size() >= 20, "expected at least 20 named monsters, got " + NamedMobs.all().size());
		require(RaidBosses.all().size() == 6, "expected 6 raid bosses");
		for (BossDef def : RaidBosses.all()) {
			require(def.phaseCount() >= 3, def.id() + " needs at least 3 phases");
		}
		require(RaidBosses.AETHRYX.phaseCount() == 4, "Aethryx has 4 phases");
		// every bracket offers one weapon (some classes have none at Lv 100) and one armor piece per class
		for (JobClass job : JobClass.PLAYABLE) {
			for (int bracket = 10; bracket <= 100; bracket += 10) {
				List<ClassGear> shop = GearIndex.shopItems(job, bracket);
				long weapons = shop.stream().filter(ClassGear::isWeapon).count();
				require((weapons == 1 || bracket == 100 && weapons == 0) && shop.size() - weapons == 1,
					job.id() + " Lv" + bracket + " shop should be 1 weapon + 1 armor, got " + shop.stream().map(ClassGear::id).toList());
			}
		}

		context.runOnClient(minecraft -> {
			var resources = minecraft.getResourceManager();
			JsonObject en = lang(minecraft, "en_us");
			JsonObject ko = lang(minecraft, "ko_kr");
			List<String> keys = new ArrayList<>();
			List<String> missing = new ArrayList<>();
			for (ArmorPieceDef piece : ClassArmor.pieces()) {
				keys.add(piece.nameKey());
				if (resources.getResource(MinecraftMode.id("items/" + piece.id() + ".json")).isEmpty()
					|| resources.getResource(MinecraftMode.id("textures/item/" + piece.id() + ".png")).isEmpty()) {
					missing.add("model/texture " + piece.id());
				}
			}
			for (NamedDef def : NamedMobs.all()) {
				keys.add(def.nameKey());
				keys.add(def.descKey());
				if (resources.getResource(MinecraftMode.id("textures/entity/creature/" + def.id() + ".png")).isEmpty()) {
					missing.add("texture " + def.id());
				}
			}
			for (BossDef def : RaidBosses.all()) {
				keys.add(def.nameKey());
				keys.add(def.epithetKey());
				keys.add(def.descKey());
				for (int phase = 2; phase <= def.phaseCount(); phase++) {
					keys.add(def.nameKey() + ".phase" + phase);
				}
				if (resources.getResource(MinecraftMode.id("textures/entity/creature/" + def.id() + ".png")).isEmpty()
					|| resources.getResource(MinecraftMode.id("textures/entity/creature/" + def.id() + "_glow.png")).isEmpty()) {
					missing.add("texture " + def.id());
				}
			}
			keys.addAll(List.of("raid.minecraft_mode.phase", "raid.minecraft_mode.victory", "screen.minecraft_mode.raid.title", "screen.minecraft_mode.loot.title",
				"message.minecraft_mode.party.created", "message.minecraft_mode.raid.entered", "message.minecraft_mode.loot.won_auction", "key.minecraft_mode.loot_screen"));
			for (String key : keys) {
				if (!en.has(key) || !ko.has(key) || en.get(key).getAsString().isEmpty() || ko.get(key).getAsString().isEmpty()) {
					missing.add("lang " + key);
				}
			}
			require(missing.isEmpty(), "missing assets: " + missing.subList(0, Math.min(10, missing.size())));
		});
	}

	// ------------------------------------------------------------------ armor

	private static void checkArmor(final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			JobData saved = JobProgression.get(player);
			JobProgression.set(player, saved.withJob(JobClass.WARRIOR, 2).withProgress(30, 0));
			ArmorSetDef set = ClassArmor.sets(JobClass.WARRIOR).stream().filter(x -> x.level() == 20).findFirst().orElseThrow();
			RandomSource random = player.getRandom();
			for (ArmorPieceDef piece : ClassArmor.piecesOf(set)) {
				player.setItemSlot(piece.slot().equipmentSlot(), GearDrops.create(ClassGear.of(piece), random));
			}
			GearStats.invalidate(player);
			if (GearStats.setCounts(player).getOrDefault(set.id(), 0) != 4) {
				return "4 worn pieces of " + set.id() + " should count, got " + GearStats.setCounts(player);
			}
			EngraveTotals totals = GearStats.of(player);
			for (StatLine line : GearStats.activeSetLines(set, 4)) {
				if (totals.get(line.stat()) + 1.0E-3 < line.value()) {
					return "set bonus " + line.stat() + " " + line.value() + " missing (total " + totals.get(line.stat()) + ")";
				}
			}
			if (GearStats.activeSetLines(set, 2).size() >= GearStats.activeSetLines(set, 4).size()) {
				return "more pieces should unlock more set lines";
			}
			// other classes cannot wear it; enforce moves it back to the inventory
			if (GearRules.canUse(saved.withJob(JobClass.MAGE, 2).withProgress(30, 0), JobClass.WARRIOR, 20)) {
				return "a mage must not wear warrior armor";
			}
			if (GearRules.canUse(saved.withJob(JobClass.WARRIOR, 1).withProgress(15, 0), JobClass.WARRIOR, 20)) {
				return "a Lv 15 warrior must not wear Lv 20 armor";
			}
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.MAGE, 2));
			GearRules.enforce(player);
			for (GearSlot slot : GearSlot.ARMOR) {
				if (!player.getItemBySlot(slot.equipmentSlot()).isEmpty()) {
					return "enforce should take off armor of another class";
				}
			}
			player.getInventory().clearContent();
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.WARRIOR, 2));

			// cooldown floor: flat reduction never takes a skill below 1 second
			Skill skill = JobWeapons.all().stream().flatMap(d -> d.skills().stream()).filter(k -> k.cooldownTicks() >= 60).findFirst().orElseThrow();
			EngraveTotals heavy = EngraveTotals.builder().add(EngraveStat.COOLDOWN_FLAT, 6.0F).add(EngraveStat.COOLDOWN, 60.0F).build();
			int cd = SkillCaster.cooldown(JobProgression.get(player), skill, heavy);
			if (cd != 20) {
				return "cooldown with huge reductions should floor at 20 ticks, got " + cd;
			}
			EngraveTotals two = EngraveTotals.builder().add(EngraveStat.COOLDOWN_FLAT, 2.0F).build();
			int cd2 = SkillCaster.cooldown(JobProgression.get(player), skill, two);
			if (cd2 != Math.max(20, skill.cooldownTicks() - 40)) {
				return "-2 s should take 40 ticks off " + skill.cooldownTicks() + ", got " + cd2;
			}

			// armor engraving: 4 lines, armor engravings that fit the slot
			ArmorPieceDef helmet = ClassArmor.piecesOf(set).stream().filter(p -> p.slot() == GearSlot.HEAD).findFirst().orElseThrow();
			ClassGear gear = ClassGear.of(helmet);
			if (gear.maxLines() != 4 || ClassGear.of(JobWeapons.of(JobClass.WARRIOR).getFirst()).maxLines() != 3) {
				return "armor should take 4 engraving lines and weapons 3";
			}
			List<Engraving> offers = EngravingMenu.offers(gear, Engravings.EMPTY);
			if (offers.isEmpty() || !offers.stream().allMatch(e -> e.isArmor() && e.fits(JobClass.WARRIOR, GearSlot.HEAD))) {
				return "helmet engraving offers should be warrior head engravings: " + offers;
			}
			return "";
		});
		require(report.isEmpty(), report);
	}

	private static void checkUpgrade(final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			if (GearUpgrades.etherCost(10) != 3) {
				return "evolving into Lv 10-19 gear should take 3 ether, takes " + GearUpgrades.etherCost(10);
			}
			for (int bracket = 20; bracket <= 100; bracket += 10) {
				if (GearUpgrades.etherCost(bracket) < GearUpgrades.etherCost(bracket - 10)) {
					return "ether cost should climb with the bracket, Lv " + bracket + " is cheaper";
				}
			}
			// fuse 3 -> 1 of the next grade, split 1 -> 3 of the grade below
			ServerPlayer player = connection.getServerPlayer();
			player.getInventory().clearContent();
			player.setItemInHand(InteractionHand.MAIN_HAND, EvolutionEtherItem.of(10, 4));
			player.setShiftKeyDown(false);
			player.getMainHandItem().use(player.level(), player, InteractionHand.MAIN_HAND);
			int fused = ether(player, 20);
			int left = ether(player, 10);
			player.getInventory().clearContent();
			player.setItemInHand(InteractionHand.MAIN_HAND, EvolutionEtherItem.of(30, 1));
			player.setShiftKeyDown(true);
			player.getMainHandItem().use(player.level(), player, InteractionHand.MAIN_HAND);
			player.setShiftKeyDown(false);
			int split = ether(player, 20);
			int whole = ether(player, 30);
			player.getInventory().clearContent();
			if (fused != 1 || left != 1 || split != EvolutionEtherItem.FUSE || whole != 0) {
				return "ether fuse/split: 4x Lv10 -> " + fused + " Lv20 + " + left + " Lv10, 1x Lv30 -> " + split + " Lv20 + " + whole + " Lv30";
			}
			ClassGear from = ClassGear.of(JobWeapons.of(JobClass.ROGUE).getFirst());
			List<ClassGear> targets = GearUpgrades.targets(from);
			if (targets.isEmpty()) {
				return "a Lv " + from.level() + " weapon should evolve into something";
			}
			for (ClassGear t : targets) {
				if (t.job() != from.job() || t.level() <= from.level() || !t.isWeapon()) {
					return "bad evolution target " + t.id();
				}
			}
			ItemStack evolved = GearUpgrades.evolve(GearIndex.stack(from), targets.getFirst(), RandomSource.create(1));
			ClassGear after = ClassGear.of(evolved);
			if (after == null || !after.id().equals(targets.getFirst().id())) {
				return "evolve should give " + targets.getFirst().id();
			}
			return "";
		});
		require(report.isEmpty(), report);
	}

	private static int ether(final ServerPlayer player, final int grade) {
		int total = 0;
		for (ItemStack stack : player.getInventory()) {
			if (stack.is(ModItems.EVOLUTION_ETHER) && EvolutionEtherItem.grade(stack) == grade) {
				total += stack.getCount();
			}
		}
		return total;
	}

	// ------------------------------------------------------------------ named monsters

	private static void checkNamed(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = player.level();
			StringBuilder out = new StringBuilder();
			int i = 0;
			for (NamedDef def : NamedMobs.all()) {
				BlockPos pos = new BlockPos(-15 + (i % 11) * 3, -60, -18 - (i / 11) * 7);
				NamedMob mob = NamedMobs.type(def).spawn(level, pos, EntitySpawnReason.SPAWN_ITEM_USE);
				if (mob == null) {
					out.append(def.id()).append(" did not spawn; ");
					continue;
				}
				mob.setNoAi(true);
				mob.setYRot(0.0F);
				mob.setYHeadRot(0.0F);
				mob.setYBodyRot(0.0F);
				if (mob.namedLevel() < def.lo() || mob.namedLevel() > def.hi() || mob.getCustomName() == null) {
					out.append(def.id()).append(" level ").append(mob.namedLevel()).append("; ");
				}
				i++;
			}
			return out.toString();
		});
		require(report.isEmpty(), report);
		server.runCommand("gamemode creative @p");
		server.runCommand("tp @p 0.5 -56 -4 180 12");
		context.waitTicks(20);
		connection.waitForChunksRender();
		context.takeScreenshot("named_lineup");
		server.runCommand("gamemode survival @p");

		// a kill always drops ether
		int[] ether = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = player.level();
			NamedMob mob = level.getEntitiesOfClass(NamedMob.class, player.getBoundingBox().inflate(80)).getFirst();
			mob.hurtServer(level, player.damageSources().playerAttack(player), 1.0E6F);
			return new int[] {mob.getBlockX(), mob.getBlockZ()};
		});
		context.waitTicks(5);
		boolean dropped = server.computeOnServer(s -> {
			ServerLevel level = connection.getServerPlayer().level();
			return !level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(ether[0] - 4, -64, ether[1] - 4, ether[0] + 4, -50, ether[1] + 4),
				e -> e.getItem().is(ModItems.EVOLUTION_ETHER)).isEmpty();
		});
		require(dropped, "a named kill should drop Evolution Ether");
		server.runCommand("kill @e[type=!player]");
		context.waitTicks(2);
	}

	// ------------------------------------------------------------------ bosses

	/** Spawns every boss (no arena) and runs each of its patterns once; any exception fails the test. */
	private static void checkBossPatterns(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("tp @p 0.5 -60 0.5 180 0");
		server.runCommand("effect give @p resistance 999999 4 true");
		server.runCommand("effect give @p regeneration 999999 4 true");
		for (BossDef def : RaidBosses.all()) {
			List<String> ids = server.computeOnServer(s -> {
				ServerPlayer player = connection.getServerPlayer();
				RaidBoss boss = RaidBosses.type(def).create(player.level(), EntitySpawnReason.COMMAND);
				boss.snapTo(0.5, -60, -9.5, 0.0F, 0.0F);
				boss.configure(1, null);
				player.level().addFreshEntity(boss);
				return boss.patternIds();
			});
			require(ids.size() >= 6, def.id() + " should have at least 6 patterns, got " + ids);
			for (String id : ids) {
				boolean ran = server.computeOnServer(s -> {
					ServerPlayer player = connection.getServerPlayer();
					RaidBoss boss = player.level().getEntitiesOfClass(RaidBoss.class, player.getBoundingBox().inflate(40), b -> b.isAlive() && b.def() == def).getFirst();
					player.setHealth(player.getMaxHealth());
					player.teleportTo(0.5, -60, 0.5);
					return boss.runPattern(id, player);
				});
				require(ran, def.id() + " could not run " + id);
				context.waitTicks(6);
			}
			context.waitTicks(70);
			server.runCommand("kill @e[type=!player]");
			context.waitTicks(25);
		}
		server.runCommand("effect clear @p");
		boolean alive = server.computeOnServer(s -> connection.getServerPlayer().isAlive());
		require(alive, "the player should survive the pattern test");
	}

	// ------------------------------------------------------------------ raids

	private static void checkRaidFlow(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("effect give @p resistance 999999 4 true");
		String started = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.getInventory().clearContent();
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.WARRIOR, 2).withProgress(30, 0));
			Parties.create(player);
			RaidInstance instance = Raids.start(s, List.of(player), RaidBosses.ARACHNE, player.getUUID());
			return instance == null ? "raid did not start" : "";
		});
		require(started.isEmpty(), started);
		context.waitTicks(5);
		String arrived = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			RaidInstance instance = Raids.instanceOf(player);
			if (instance == null || !RaidDimension.is(player.level())) {
				return "player should be in the raid dimension, is in " + player.level().dimension();
			}
			BlockPos floor = BlockPos.containing(player.position()).below();
			return player.level().getBlockState(floor).isAir() ? "no arena floor under the player" : "";
		});
		require(arrived.isEmpty(), arrived);
		connection.waitForChunksRender();
		waitFor(context, server, s -> {
			RaidInstance instance = Raids.instanceOf(connection.getServerPlayer());
			return instance != null && instance.state() == RaidInstance.State.FIGHT;
		}, 300, "the boss should appear after the countdown");
		context.waitTicks(30);
		context.takeScreenshot("raid_arachne_fight");
		String fight = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			RaidBoss boss = player.level().getEntitiesOfClass(RaidBoss.class, player.getBoundingBox().inflate(60)).stream().findFirst().orElse(null);
			if (boss == null) {
				return "no boss in the arena";
			}
			if (Math.abs(boss.divisor() - RaidBosses.ARACHNE.health() / RaidBoss.HEALTH_BAR) > 1.0E-3 || boss.getMaxHealth() != RaidBoss.HEALTH_BAR) {
				return "solo Arachne should have " + RaidBosses.ARACHNE.health() + " effective health, divisor " + boss.divisor();
			}
			// a neutral source, so class bonuses do not change the number
			boss.hurtServer(player.level(), player.damageSources().generic(), 100.0F);
			float lost = RaidBoss.HEALTH_BAR - boss.getHealth();
			float expected = 100.0F / boss.divisor();
			// armor takes a few percent more
			if (lost > expected + 0.5F || lost < expected * 0.85F) {
				return "100 damage should take about " + expected + " off the bar, took " + lost;
			}
			boss.setInvulnerableTime(0);
			boss.hurtServer(player.level(), player.damageSources().playerAttack(player), 1.0E7F);
			return boss.isAlive() ? "boss should be dead" : "";
		});
		require(fight.isEmpty(), fight);
		context.waitTicks(5);
		String rewards = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			RaidInstance instance = Raids.instanceOf(player);
			if (instance == null || instance.state() != RaidInstance.State.VICTORY) {
				return "raid should be won";
			}
			int ether = JobProgression.count(player.getInventory(), ModItems.EVOLUTION_ETHER);
			int condensed = JobProgression.count(player.getInventory(), ModItems.CONDENSED_ESSENCE);
			int gear = 0;
			for (ItemStack stack : player.getInventory()) {
				ClassGear g = ClassGear.of(stack);
				if (g != null && g.level() >= RaidBosses.ARACHNE.lo() && g.level() <= RaidBosses.ARACHNE.hi()) {
					gear++;
				}
			}
			if (ether < 5 || condensed < 2 || gear < 3) {
				return "solo victory should give 5+ ether, 2+ condensed essence and 3 gear, got " + ether + "/" + condensed + "/" + gear;
			}
			Raids.leave(player);
			return "";
		});
		require(rewards.isEmpty(), rewards);
		context.waitTicks(5);
		String home = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			return RaidDimension.is(player.level()) ? "player should be home after /raid leave" : Raids.instances().isEmpty() ? "" : "the instance should close";
		});
		require(home.isEmpty(), home);
		connection.waitForChunksRender();
	}

	private static void checkDeathInRaid(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("effect clear @p");
		server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.getInventory().add(new ItemStack(ModItems.ESSENCE, 7));
			return Raids.start(s, List.of(player), RaidBosses.GORVATH, player.getUUID()) != null;
		});
		context.waitTicks(5);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.hurtServer(player.level(), player.damageSources().fellOutOfWorld(), 1000.0F);
		});
		context.waitTicks(5);
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			if (!player.isAlive() || player.isDeadOrDying()) {
				return "dying in a raid must not kill";
			}
			if (RaidDimension.is(player.level())) {
				return "a fallen player should be sent home";
			}
			if (JobProgression.count(player.getInventory(), ModItems.ESSENCE) != 7) {
				return "a fallen player keeps their items";
			}
			return "";
		});
		require(report.isEmpty(), report);
		waitFor(context, server, s -> Raids.instances().isEmpty(), 60, "a raid with nobody left should close");
		connection.waitForChunksRender();
	}

	// ------------------------------------------------------------------ loot

	private static void checkLoot(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		UUID ghost = UUID.nameUUIDFromBytes("ghost".getBytes(StandardCharsets.UTF_8));
		int[] session = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.getInventory().clearContent();
			player.getInventory().add(new ItemStack(ModItems.GOLD_COIN, 20));
			Map<UUID, String> who = new LinkedHashMap<>();
			who.put(player.getUUID(), player.getPlainTextName());
			who.put(ghost, "Ghost");
			RandomSource random = RandomSource.create(7);
			List<ItemStack> items = List.of(GearDrops.pick(null, 20, 30, random), GearDrops.pick(null, 20, 30, random));
			LootSession started = LootSessions.start(s, RaidBosses.ARACHNE, who, player.getUUID(), items);
			return new int[] {started.id(), Coins.total(player)};
		});
		context.waitForScreen(LootScreen.class);
		server.runCommand("tick rate 100");
		waitFor(context, server, s -> lot(session[0], 0).state() == LootSession.State.RUNNING, 200, "the first lot should open");
		context.waitTicks(2);
		context.takeScreenshot("raid_loot_auction");
		requireFits(context, "loot screen");
		int start = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			LootSessions.handle(player, new LootActionPayload(session[0], LootActionPayload.Action.BID, 1));
			return lot(session[0], 0).bid();
		});
		int afterBid = server.computeOnServer(s -> Coins.total(connection.getServerPlayer()));
		require(start > 0 && afterBid == session[1] - start, "a bid should hold " + start + " coins in escrow, wallet " + session[1] + " -> " + afterBid);
		waitFor(context, server, s -> lot(session[0], 0).state() == LootSession.State.DONE, 1200, "the auction should end");
		String auction = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			LootSession.Lot lot = lot(session[0], 0);
			if (!player.getUUID().equals(lot.winner()) || lot.price() != start) {
				return "the only bidder should win at " + start + ", winner " + lot.winner() + " price " + lot.price();
			}
			return Coins.total(player) == session[1] - start ? "" : "the winner pays the bid";
		});
		require(auction.isEmpty(), auction);
		// second lot: the leader switches to dice and is the only one to roll
		waitFor(context, server, s -> lot(session[0], 1).state() == LootSession.State.RUNNING, 200, "the second lot should open");
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			LootSessions.handle(player, new LootActionPayload(session[0], LootActionPayload.Action.SWITCH_MODE, 0));
			LootSessions.handle(player, new LootActionPayload(session[0], LootActionPayload.Action.ROLL, 0));
		});
		context.waitTicks(4);
		context.takeScreenshot("raid_loot_dice");
		waitFor(context, server, s -> lot(session[0], 1).state() == LootSession.State.DONE, 1200, "the dice round should end");
		String dice = server.computeOnServer(s -> {
			LootSession.Lot lot = lot(session[0], 1);
			return lot.mode() == LootSession.Mode.DICE && connection.getServerPlayer().getUUID().equals(lot.winner()) ? "" : "the only roller should win the dice";
		});
		require(dice.isEmpty(), dice);
		server.runCommand("tick rate 20");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
	}

	private static LootSession.Lot lot(final int session, final int index) {
		LootSession found = LootSessions.get(session);
		if (found == null) {
			throw new AssertionError("loot session " + session + " is gone");
		}
		return found.lots().get(index);
	}

	// ------------------------------------------------------------------ screens and screenshots

	private static void raidBoard(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		context.runOnClient(minecraft -> minecraft.gui.setScreen(new RaidScreen(-1)));
		context.waitForScreen(RaidScreen.class);
		context.waitTicks(10);
		context.takeScreenshot("raid_board");
		requireFits(context, "raid board");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(new com.minecraftmode.client.job.JobScreen()));
		context.waitForScreen(com.minecraftmode.client.job.JobScreen.class);
		requireFits(context, "class screen");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
	}

	/**
	 * Minecraft never lays the GUI out narrower than 320 (4:3 screens at auto GUI scale); every button
	 * of the open screen must sit inside the middle 320 so nothing is cut off on any monitor.
	 */
	private static void requireFits(final ClientGameTestContext context, final String what) {
		String report = context.computeOnClient(minecraft -> {
			net.minecraft.client.gui.screens.Screen screen = minecraft.gui.screen();
			if (screen == null) {
				return what + " is not open";
			}
			int lo = (screen.width - 320) / 2;
			int hi = lo + 320;
			for (var child : screen.children()) {
				if (child instanceof net.minecraft.client.gui.components.AbstractWidget w && (w.getX() < lo || w.getRight() > hi)) {
					return what + ": " + w.getMessage().getString() + " at " + w.getX() + ".." + w.getRight() + " is outside " + lo + ".." + hi;
				}
			}
			return "";
		});
		require(report.isEmpty(), report);
	}

	/** Every boss in its arena, seen from where the party arrives. */
	private static void arenaShots(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("gamemode creative @p");
		server.runCommand("item replace entity @p weapon.mainhand with air");
		int i = 0;
		for (BossDef def : RaidBosses.all()) {
			final int slot = 40 + i++;
			server.runOnServer(s -> {
				ServerPlayer player = connection.getServerPlayer();
				ServerLevel level = RaidDimension.level(s);
				BlockPos center = new BlockPos(slot * 1024, RaidDimension.FLOOR_Y, 0);
				Arenas.build(level, center, def.arena(), RandomSource.create(slot));
				RaidBoss boss = RaidBosses.type(def).create(level, EntitySpawnReason.COMMAND);
				Vec3 at = Arenas.bossSpawn(center, def.arena());
				// turned a little so the side shows too
				boss.snapTo(at.x, at.y + (def == RaidBosses.IGNIS || def == RaidBosses.AETHRYX ? 4 : 0), at.z, 35.0F, 0.0F);
				boss.setYHeadRot(35.0F);
				boss.setYBodyRot(35.0F);
				boss.configure(1, center);
				boss.setNoAi(true);
				level.addFreshEntity(boss);
				Vec3 eye = Arenas.playerSpawn(center, 1);
				player.teleportTo(level, eye.x, eye.y + 3, eye.z, Set.of(), 180.0F, 12.0F, true);
				player.getAbilities().flying = true;
				player.onUpdateAbilities();
			});
			context.waitTicks(20);
			connection.waitForChunksRender();
			context.waitTicks(10);
			context.takeScreenshot("raid_" + def.id());
			server.runOnServer(s -> {
				ServerLevel level = RaidDimension.level(s);
				level.getEntitiesOfClass(RaidBoss.class, new net.minecraft.world.phys.AABB(slot * 1024 - 60, 0, -60, slot * 1024 + 60, 200, 60)).forEach(b -> b.discard());
			});
		}
		server.runCommand("execute in minecraft:overworld run tp @p 0.5 -60 0.5");
		context.waitTicks(10);
	}

	// ------------------------------------------------------------------ helpers

	private static void waitFor(final ClientGameTestContext context, final TestServerContext server, final Function<MinecraftServer, Boolean> condition, final int maxTicks,
		final String message) {
		for (int t = 0; t < maxTicks; t += 5) {
			if (server.computeOnServer(condition::apply)) {
				return;
			}
			context.waitTicks(5);
		}
		throw new AssertionError(message);
	}

	private static JsonObject lang(final Minecraft minecraft, final String code) {
		Resource resource = minecraft.getResourceManager().getResource(MinecraftMode.id("lang/" + code + ".json")).orElseThrow();
		try (Reader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		} catch (java.io.IOException e) {
			throw new AssertionError("cannot read " + code, e);
		}
	}

	private static void require(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
