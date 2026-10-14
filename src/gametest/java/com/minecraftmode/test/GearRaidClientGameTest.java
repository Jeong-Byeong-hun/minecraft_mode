package com.minecraftmode.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minecraftmode.MinecraftMode;
import com.minecraftmode.accessory.Accessories;
import com.minecraftmode.accessory.AccessoryDef;
import com.minecraftmode.accessory.AccessoryEnhance;
import com.minecraftmode.accessory.AccessoryMenu;
import com.minecraftmode.accessory.AccessorySet;
import com.minecraftmode.accessory.AccessorySlot;
import com.minecraftmode.bag.Bags;
import com.minecraftmode.client.accessory.AccessoryScreen;
import com.minecraftmode.client.endgame.EnhanceScreen;
import com.minecraftmode.client.job.UpgradeScreen;
import com.minecraftmode.client.raid.LootScreen;
import com.minecraftmode.client.raid.RaidClient;
import com.minecraftmode.client.raid.RaidScreen;
import com.minecraftmode.economy.Wallet;
import com.minecraftmode.enhance.EnhanceMenu;
import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.entity.boss.RaidBoss;
import com.minecraftmode.entity.combat.MobProjectile;
import com.minecraftmode.entity.combat.Telegraph;
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
import com.minecraftmode.job.gear.GearRolls;
import com.minecraftmode.job.gear.GearRules;
import com.minecraftmode.job.gear.GearSlot;
import com.minecraftmode.job.gear.GearStats;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.job.skill.CombatHooks;
import com.minecraftmode.job.skill.Skill;
import com.minecraftmode.job.skill.SkillCaster;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.loot.EvolutionEtherItem;
import com.minecraftmode.loot.GearDrops;
import com.minecraftmode.loot.GearIndex;
import com.minecraftmode.loot.GearUpgrades;
import com.minecraftmode.loot.UpgradeMenu;
import com.minecraftmode.network.LootActionPayload;
import com.minecraftmode.raid.Arenas;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.Parties;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.raid.RaidDifficulty;
import com.minecraftmode.raid.RaidDimension;
import com.minecraftmode.raid.RaidInstance;
import com.minecraftmode.raid.Raids;
import com.minecraftmode.raid.loot.LootSession;
import com.minecraftmode.raid.loot.LootSessions;
import com.minecraftmode.registry.ModDataComponents;
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
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
			checkAccessories(context, server, connection);
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
		require(RaidBosses.all().size() == 9, "expected 9 raid bosses");
		int lastLo = 0;
		java.util.Set<com.minecraftmode.raid.ArenaTheme> arenas = new java.util.HashSet<>();
		for (BossDef def : RaidBosses.all()) {
			require(def.phaseCount() >= 3, def.id() + " needs at least 3 phases");
			require(def.lo() > lastLo, def.id() + " should come after the bosses below its level");
			require(arenas.add(def.arena()), def.id() + " needs its own arena");
			lastLo = def.lo();
		}
		require(RaidBosses.all().getFirst() == RaidBosses.VERDESSA && RaidBosses.VERDESSA.lo() == 10, "the first raid opens at level 10");
		// supplies follow the level, so the old bosses kept theirs when new ones joined the list
		require(RaidBosses.supplyTier(RaidBosses.ARACHNE) == 0 && RaidBosses.supplyTier(RaidBosses.MALACHAR) == 4 && RaidBosses.supplyTier(RaidBosses.AETHRYX) == 5,
			"raid supply tiers by level");
		require(RaidBosses.AETHRYX.phaseCount() == 4, "Aethryx has 4 phases");
		// every raid boss drops its own accessory set: five slots, 2/3/5-piece bonuses, ten options per slot
		require(Accessories.sets().size() == RaidBosses.all().size() && Accessories.all().size() == RaidBosses.all().size() * Accessories.SLOTS,
			"expected one accessory set of " + Accessories.SLOTS + " per raid boss, got " + Accessories.all().size());
		for (BossDef def : RaidBosses.all()) {
			AccessorySet set = Accessories.setOf(def);
			require(set != null && set.level() == def.lo(), def.id() + " needs an accessory set of its level");
			List<Integer> pieces = set.bonuses().stream().map(ArmorSetDef.SetBonus::pieces).toList();
			require(pieces.equals(List.of(2, 3, 5)), set.id() + " accessories should have 2/3/5 piece bonuses, got " + pieces);
		}
		for (AccessorySlot slot : AccessorySlot.values()) {
			require(slot.pool().size() >= 10, slot + " accessories roll from only " + slot.pool().size() + " options");
		}
		checkAccessoryDrops();
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
			// raid accessories (0.5.24): every piece has an icon, a model and a name, every slot and set a name
			for (AccessoryDef def : Accessories.all()) {
				keys.add(def.nameKey());
				if (resources.getResource(MinecraftMode.id("items/" + def.id() + ".json")).isEmpty()
					|| resources.getResource(MinecraftMode.id("textures/item/" + def.id() + ".png")).isEmpty()) {
					missing.add("model/texture " + def.id());
				}
			}
			for (AccessorySlot slot : AccessorySlot.values()) {
				keys.add(slot.nameKey());
			}
			for (AccessorySet set : Accessories.sets()) {
				keys.add(set.nameKey());
			}
			keys.addAll(List.of("container.minecraft_mode.accessories", "tooltip.minecraft_mode.accessory.source", "screen.minecraft_mode.character.source.accessory",
				"tooltip.minecraft_mode.accessory.tier", "tooltip.minecraft_mode.accessory.enhanced", ModItems.ACCESSORY_ORB.getDescriptionId(),
				ModItems.ACCESSORY_ORB.getDescriptionId() + ".tooltip", "screen.minecraft_mode.upgrade.accessory", "screen.minecraft_mode.codex.source.raid_top"));
			for (String id : List.of("accessory_orb", "accessory_inherit_1", "accessory_inherit_2")) {
				if (resources.getResource(MinecraftMode.id("items/" + id + ".json")).isEmpty() || resources.getResource(MinecraftMode.id("textures/item/" + id + ".png")).isEmpty()) {
					missing.add("model/texture " + id);
				}
			}
			for (Item ticket : List.of(ModItems.ACCESSORY_INHERIT_1, ModItems.ACCESSORY_INHERIT_2)) {
				keys.add(ticket.getDescriptionId());
				keys.add(ticket.getDescriptionId() + ".tooltip");
			}
			keys.addAll(List.of("tooltip.minecraft_mode.accessory.inherit", "screen.minecraft_mode.upgrade.inherit_cost", "screen.minecraft_mode.upgrade.inherit_keeps",
				"screen.minecraft_mode.codex.source.accessory_inherit_1", "screen.minecraft_mode.codex.source.accessory_inherit_2"));
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

			// cooldown floor: percent and flat reductions together take at most 60% off (flat seconds used to take 6-9 s skills to 1 s)
			Skill skill = JobWeapons.all().stream().flatMap(d -> d.skills().stream()).filter(k -> k.cooldownTicks() >= 60).findFirst().orElseThrow();
			int floor = Math.round(skill.cooldownTicks() * (1.0F - SkillCaster.MAX_COOLDOWN_CUT));
			EngraveTotals heavy = EngraveTotals.builder().add(EngraveStat.COOLDOWN_FLAT, 6.0F).add(EngraveStat.COOLDOWN, 60.0F).build();
			int cd = SkillCaster.cooldown(JobProgression.get(player), skill, heavy);
			if (cd != floor) {
				return "cooldown with huge reductions should stop at 40% of " + skill.cooldownTicks() + " (" + floor + " ticks), got " + cd;
			}
			EngraveTotals two = EngraveTotals.builder().add(EngraveStat.COOLDOWN_FLAT, 2.0F).build();
			int cd2 = SkillCaster.cooldown(JobProgression.get(player), skill, two);
			if (cd2 != Math.max(floor, skill.cooldownTicks() - 40)) {
				return "-2 s should take 40 ticks off " + skill.cooldownTicks() + " (not past 40%), got " + cd2;
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
		// an even monster fan gets a middle shot (nothing flew along the aim line), odd fans and single shots stay as they are
		require(MobProjectile.fanShots(4, 36.0F) == 5 && MobProjectile.fanShots(2, 10.0F) == 3 && MobProjectile.fanShots(5, 40.0F) == 5
			&& MobProjectile.fanShots(1, 0.0F) == 1 && MobProjectile.fanShots(2, 0.0F) == 2, "even monster fans should get a middle shot");
		require(Telegraph.MIN_WARNING >= 20, "boss warnings should last at least a second");
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
			// breaths, curses and elemental blasts are spells (magic damage); the casters and the three new bosses have some
			List<String> spells = server.computeOnServer(s -> connection.getServerPlayer().level()
				.getEntitiesOfClass(RaidBoss.class, connection.getServerPlayer().getBoundingBox().inflate(40), b -> b.isAlive() && b.def() == def).getFirst().spellIds());
			boolean caster = def == RaidBosses.IGNIS || def == RaidBosses.MALACHAR || def == RaidBosses.AETHRYX || def == RaidBosses.VERDESSA || def == RaidBosses.NEFREKHET
				|| def == RaidBosses.RAIJIN;
			require(caster == !spells.isEmpty() && spells.size() < ids.size(), def.id() + " spells " + spells + " of " + ids);
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
		// an item an earlier party left in the arena slot (slot 0 at x 1024: the test's first raid) is gone when the arena opens
		Vec3 slot = new Vec3(1026.5, RaidDimension.FLOOR_Y + 1, 2.5);
		server.runOnServer(s -> SweepChecks.hold(RaidDimension.level(s), slot, true));
		context.waitTicks(5);
		ItemEntity litter = server.computeOnServer(s -> SweepChecks.litter(RaidDimension.level(s), slot));
		context.waitTicks(1);
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
		server.runOnServer(s -> {
			SweepChecks.check(RaidDimension.level(s), litter, slot);
			SweepChecks.hold(RaidDimension.level(s), slot, false);
		});
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
			// a class arrow replaces the amount the boss already divided (CombatHooks.modifyIncoming) and used to skip the divisor, so a
			// solo archer felled any boss in seconds; a six-member party's toughness makes the difference plain
			boss.configure(6, boss.home(), boss.difficulty(), boss.affixes());
			Arrow arrow = new Arrow(player.level(), player, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
			CombatHooks.trackArrow(arrow, 100.0F);
			boss.setInvulnerableTime(0);
			boss.hurtServer(player.level(), player.damageSources().arrow(arrow, player), 1.0F);
			float shot = RaidBoss.HEALTH_BAR - boss.getHealth();
			float share = 100.0F / boss.divisor();
			// a crit adds half again
			if (shot <= 0.0F || shot > share * 2.0F) {
				return "a class arrow of 100 should take about " + share + " off a six-member boss's bar (divisor " + boss.divisor() + "), took " + shot;
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
			// the raid-only accessories: on a solo Normal clear at most one (a 10% roll), of Arachne's own set, with one option
			List<AccessoryDef> accessories = new ArrayList<>();
			for (ItemStack stack : player.getInventory()) {
				AccessoryDef a = Accessories.def(stack);
				if (a != null) {
					accessories.add(a);
					if (stack.getOrDefault(ModDataComponents.GEAR_ROLLS, GearRolls.EMPTY).lines().size() != Accessories.optionCount(RaidDifficulty.NORMAL)) {
						return "a Normal accessory should roll " + Accessories.optionCount(RaidDifficulty.NORMAL) + " option, got " + stack.get(ModDataComponents.GEAR_ROLLS);
					}
				}
			}
			if (accessories.size() > 1 || accessories.stream().anyMatch(a -> a.set().boss() != RaidBosses.ARACHNE)) {
				return "solo Normal Arachne gives at most one of her own accessories, got " + accessories.stream().map(AccessoryDef::id).toList();
			}
			if (Bags.count(player, ModItems.ACCESSORY_ORB) != 0) {
				return "a Normal clear gives no accessory orbs";
			}
			if (Bags.count(player, ModItems.ACCESSORY_INHERIT_1) + Bags.count(player, ModItems.ACCESSORY_INHERIT_2) != 0) {
				return "a tier 1 boss gives no inheritance tickets";
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

	/**
	 * Accessory tiers and drop chances (0.5.24): Normal drops the boss's own set at its tier's chance, Heroic and Nightmare only
	 * tier 3 pieces, more often for harder bosses and difficulties (Heroic Verdessa beats Normal Aethryx), and orbs from Heroic on.
	 * Numbers only: item stacks need a world ({@link #sampleHeroicDrops} runs in it).
	 */
	private static void checkAccessoryDrops() {
		List<AccessorySet> sets = Accessories.sets();
		for (int i = 0; i < sets.size(); i++) {
			require(sets.get(i).tier() == 1 + i / 3, sets.get(i).id() + " should be tier " + (1 + i / 3) + ", is " + sets.get(i).tier());
			BossDef boss = sets.get(i).boss();
			float normal = Accessories.dropChance(boss, RaidDifficulty.NORMAL);
			require(Math.abs(normal - new float[] {0.15F, 0.08F, 0.015F}[sets.get(i).tier() - 1]) < 1.0E-4F, boss.id() + " Normal drop chance " + normal);
			require(Accessories.dropSets(boss, RaidDifficulty.NORMAL).equals(List.of(sets.get(i))), boss.id() + " drops its own set on Normal");
			for (RaidDifficulty difficulty : List.of(RaidDifficulty.HEROIC, RaidDifficulty.NIGHTMARE)) {
				List<AccessorySet> from = Accessories.dropSets(boss, difficulty);
				require(!from.isEmpty() && from.stream().allMatch(set -> set.tier() == Accessories.TOP_TIER), boss.id() + " drops only tier 3 on " + difficulty + ": " + from);
				require(sets.get(i).tier() != Accessories.TOP_TIER || from.equals(List.of(sets.get(i))), boss.id() + " keeps its own tier 3 set on " + difficulty);
			}
		}
		// the tier 3 ladder: Normal Raijin..Aethryx < Heroic Verdessa < ... < Heroic Aethryx < Nightmare Verdessa < ... < Nightmare Aethryx
		float last = Accessories.dropChance(RaidBosses.AETHRYX, RaidDifficulty.NORMAL);
		for (RaidDifficulty difficulty : List.of(RaidDifficulty.HEROIC, RaidDifficulty.NIGHTMARE)) {
			for (AccessorySet set : sets) {
				float chance = Accessories.dropChance(set.boss(), difficulty);
				require(chance > last, difficulty + " " + set.boss().id() + " (" + chance + ") should drop more often than the step before (" + last + ")");
				last = chance;
			}
		}
		require(Accessories.orbs(RaidDifficulty.NORMAL) == 0 && Accessories.orbs(RaidDifficulty.HEROIC) == 1 && Accessories.orbs(RaidDifficulty.NIGHTMARE) == 2,
			"accessory orbs: none on Normal, 1 on Heroic, 2 on Nightmare");
		// inheritance tickets: tier 2 bosses give the tier 1 ticket, tier 3 bosses the tier 2 one, more often on harder difficulties
		require(Accessories.ticketDrop(RaidBosses.VERDESSA) == null && Accessories.ticketDrop(RaidBosses.NEFREKHET) == ModItems.ACCESSORY_INHERIT_1
			&& Accessories.ticketDrop(RaidBosses.AETHRYX) == ModItems.ACCESSORY_INHERIT_2, "inheritance tickets by boss tier");
		require(Accessories.ticketChance(RaidDifficulty.NORMAL) > 0 && Accessories.ticketChance(RaidDifficulty.HEROIC) > Accessories.ticketChance(RaidDifficulty.NORMAL)
			&& Accessories.ticketChance(RaidDifficulty.NIGHTMARE) > Accessories.ticketChance(RaidDifficulty.HEROIC)
			&& Accessories.ticketChance(RaidDifficulty.NIGHTMARE) <= 0.10F, "ticket chances rise with difficulty and stay at 10% or less");
		for (AccessorySet set : sets) {
			for (AccessorySlot slot : AccessorySlot.values()) {
				List<AccessoryDef> next = Accessories.inheritTargets(Accessories.def(set, slot));
				require(set.tier() == Accessories.TOP_TIER ? next.isEmpty() : next.size() == 3 && next.stream().allMatch(t -> t.set().tier() == set.tier() + 1 && t.slot() == slot),
					set.id() + " " + slot + " inherits into " + next.stream().map(AccessoryDef::id).toList());
			}
		}
		// enhancement: +10 tops out, raises the piece's own lines by half and adds damage; never below 30% per attempt
		require(AccessoryEnhance.chance(1, 0) == 100 && AccessoryEnhance.baseRate(AccessoryEnhance.MAX) >= 30 && AccessoryEnhance.chance(AccessoryEnhance.MAX, 100) == 100,
			"accessory enhancement chances");
		require(AccessoryEnhance.scaled(StatLine.of(EngraveStat.DAMAGE, 4.0F), AccessoryEnhance.MAX).value() == 6.0F, "+10 raises a line by half");
		require(AccessoryEnhance.bonus(AccessoryEnhance.MAX).equals(List.of(StatLine.of(EngraveStat.DAMAGE, AccessoryEnhance.MAX_DAMAGE)))
			&& AccessoryEnhance.bonus(AccessoryEnhance.MAX - 1).isEmpty(), "+10 adds damage dealt");
		Enhancement failed = AccessoryEnhance.failed(new Enhancement(8, 0));
		require(failed.level() == 8 && failed.pity() == Enhancement.PITY_STEP, "a failed accessory attempt keeps the level and builds spirit");
	}

	/** A thousand Heroic Verdessa winners: about 3% of them roll a tier 3 piece with two options. */
	private static String sampleHeroicDrops() {
		List<ItemStack> drops = Accessories.raidDrops(RaidBosses.VERDESSA, RaidDifficulty.HEROIC, 1000, RandomSource.create(11L));
		if (drops.size() < 10 || drops.size() > 60) {
			return "1000 Heroic Verdessa rolls should give about 25 accessories, got " + drops.size();
		}
		for (ItemStack drop : drops) {
			AccessoryDef def = Accessories.def(drop);
			if (def == null || def.set().tier() != Accessories.TOP_TIER) {
				return "Heroic Verdessa dropped " + drop;
			}
			if (drop.getOrDefault(ModDataComponents.GEAR_ROLLS, GearRolls.EMPTY).lines().size() != Accessories.optionCount(RaidDifficulty.HEROIC)) {
				return "a Heroic accessory rolls " + Accessories.optionCount(RaidDifficulty.HEROIC) + " options, got " + drop.get(ModDataComponents.GEAR_ROLLS);
			}
		}
		if (drops.stream().map(stack -> Accessories.def(stack).set()).distinct().count() <= 1) {
			return "Heroic Verdessa should drop from more than one tier 3 set";
		}
		// inheritance tickets are loot lots too: a thousand Nightmare Nefrekhet winners, about 10% of them roll a tier 1 ticket
		List<ItemStack> tickets = Accessories.ticketLots(RaidBosses.NEFREKHET, RaidDifficulty.NIGHTMARE, 1000, RandomSource.create(12L));
		if (tickets.size() < 50 || tickets.size() > 160 || tickets.stream().anyMatch(ticket -> !ticket.is(ModItems.ACCESSORY_INHERIT_1))) {
			return "1000 Nightmare Nefrekhet rolls should give about 100 tier 1 tickets, got " + tickets.size();
		}
		if (!Accessories.ticketLots(RaidBosses.VERDESSA, RaidDifficulty.NIGHTMARE, 100, RandomSource.create(13L)).isEmpty()) {
			return "tier 1 bosses add no tickets";
		}
		return Accessories.ticketStart(tickets.getFirst()) > Coins.SILVER ? "" : "a ticket auction should open above the default silver";
	}

	/** Worn accessories add their lines and set bonuses under their own source; the window only takes a fitting piece. */
	private static void checkAccessories(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		String worn = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.MAGE, 4).withProgress(95, 0));
			AccessorySet set = Accessories.setOf(RaidBosses.AETHRYX);
			RandomSource random = RandomSource.create(7L);
			List<ItemStack> stacks = new ArrayList<>();
			for (AccessorySlot slot : AccessorySlot.values()) {
				stacks.add(Accessories.create(Accessories.def(set, slot), Accessories.optionCount(RaidDifficulty.NIGHTMARE), random));
			}
			Accessories.setWorn(player, stacks);
			GearStats.invalidate(player);
			var breakdown = GearStats.breakdown(player);
			float damage = breakdown.getOrDefault(EngraveStat.DAMAGE, java.util.Map.of()).getOrDefault(GearStats.Source.ACCESSORY, 0.0F);
			// the ring's fixed line (1 + 90 / 25) and the 5-piece bonus (8)
			if (damage < 4 + 8) {
				return "five Voidscale pieces should add at least 12% damage dealt, got " + damage;
			}
			if (Accessories.setCounts(player).getOrDefault(set.id(), 0) != Accessories.SLOTS) {
				return "all five Voidscale pieces should count, got " + Accessories.setCounts(player);
			}
			// +10 on every piece: half again on their own lines and 2% damage each
			List<ItemStack> enhanced = new ArrayList<>();
			for (ItemStack stack : stacks) {
				ItemStack copy = stack.copy();
				copy.set(ModDataComponents.ENHANCEMENT, new Enhancement(AccessoryEnhance.MAX, 0, 0));
				enhanced.add(copy);
			}
			Accessories.setWorn(player, enhanced);
			GearStats.invalidate(player);
			float boosted = GearStats.breakdown(player).getOrDefault(EngraveStat.DAMAGE, java.util.Map.of()).getOrDefault(GearStats.Source.ACCESSORY, 0.0F);
			if (boosted < damage + Accessories.SLOTS * AccessoryEnhance.MAX_DAMAGE) {
				return "five +10 pieces should add at least " + Accessories.SLOTS * AccessoryEnhance.MAX_DAMAGE + "% damage over " + damage + ", got " + boosted;
			}
			if (!enhanced.get(0).getHoverName().getString().startsWith("+10 ")) {
				return "a +10 accessory's name should show it, got " + enhanced.get(0).getHoverName().getString();
			}
			Accessories.setWorn(player, stacks);
			String sample = sampleHeroicDrops();
			if (!sample.isEmpty()) {
				return sample;
			}
			String bench = accessoryBenches(player, stacks.get(AccessorySlot.EARRING.ordinal()).copy());
			if (!bench.isEmpty()) {
				return bench;
			}
			ItemStack ring = stacks.get(AccessorySlot.RING.ordinal());
			if (AccessoryMenu.fits(player, AccessorySlot.NECKLACE, ring) || !AccessoryMenu.fits(player, AccessorySlot.RING, ring)) {
				return "a ring fits the ring slot only";
			}
			JobProgression.set(player, JobProgression.get(player).withProgress(50, 0));
			if (AccessoryMenu.fits(player, AccessorySlot.RING, ring) || !Accessories.lines(player).isEmpty()) {
				return "a Lv 90 piece neither fits nor counts at Lv 50";
			}
			JobProgression.set(player, JobProgression.get(player).withProgress(95, 0));
			AccessoryMenu.open(player);
			return "";
		});
		require(worn.isEmpty(), worn);
		context.waitTicks(10);
		String client = context.computeOnClient(minecraft -> {
			if (!(minecraft.gui.screen() instanceof AccessoryScreen)) {
				return "the accessory window should be open, got " + minecraft.gui.screen();
			}
			long synced = Accessories.worn(minecraft.player).stream().filter(stack -> !stack.isEmpty()).count();
			return synced == Accessories.SLOTS ? "" : "the client should see five worn accessories, sees " + synced;
		});
		require(client.isEmpty(), client);
		context.takeScreenshot("accessories");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
		context.waitTicks(5);
		// Brokk's bench and the blacksmith's options tab with an accessory in the slot
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ItemStack piece = Accessories.worn(player).get(AccessorySlot.NECKLACE.ordinal()).copy();
			piece.set(ModDataComponents.ENHANCEMENT, new Enhancement(6, 10, 0));
			player.getInventory().add(new ItemStack(ModItems.ACCESSORY_ORB, 3));
			player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new EnhanceMenu(id, inventory, null), Component.translatable("container.minecraft_mode.enhance")));
			if (player.containerMenu instanceof EnhanceMenu menu) {
				menu.slots.get(0).set(piece);
				menu.broadcastChanges();
			}
		});
		context.waitForScreen(EnhanceScreen.class);
		context.waitTicks(10);
		context.takeScreenshot("accessory_enhance");
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.closeContainer();
			ItemStack piece = Accessories.worn(player).get(AccessorySlot.TALISMAN.ordinal()).copy();
			player.getInventory().add(new ItemStack(ModItems.CONDENSED_ESSENCE, GearUpgrades.REROLL_CONDENSED));
			player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new UpgradeMenu(id, inventory, null), Component.translatable("container.minecraft_mode.upgrade")));
			if (player.containerMenu instanceof UpgradeMenu menu) {
				menu.slots.get(0).set(piece);
				menu.clickMenuButton(player, UpgradeMenu.BUTTON_REROLL);
				menu.broadcastChanges();
			}
		});
		context.waitForScreen(UpgradeScreen.class);
		context.runOnClient(minecraft -> {
			if (minecraft.gui.screen() instanceof UpgradeScreen screen) {
				screen.showOptions();
			}
		});
		context.waitTicks(10);
		context.takeScreenshot("accessory_options");
		// a tier 1 piece on the evolution tab lists the three tier 2 pieces it can be inherited into
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.closeContainer();
			ItemStack low = Accessories.create(Accessories.def(Accessories.setOf(RaidBosses.VERDESSA), AccessorySlot.NECKLACE), 1, RandomSource.create(9L));
			low.set(ModDataComponents.ENHANCEMENT, new Enhancement(4, 0, 0));
			player.getInventory().add(new ItemStack(ModItems.ACCESSORY_INHERIT_1));
			player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new UpgradeMenu(id, inventory, null), Component.translatable("container.minecraft_mode.upgrade")));
			if (player.containerMenu instanceof UpgradeMenu menu) {
				menu.slots.get(0).set(low);
				menu.broadcastChanges();
			}
		});
		context.waitForScreen(UpgradeScreen.class);
		context.waitTicks(10);
		context.takeScreenshot("accessory_inherit");
		server.runOnServer(s -> connection.getServerPlayer().closeContainer());
		context.waitTicks(5);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			Accessories.setWorn(player, List.of());
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.WARRIOR, 2).withProgress(30, 0));
		});
		MinecraftMode.LOGGER.info("[gearraid] accessories: 5-piece set counted, +10 scaling, slot and level checks, reroll and enhancement benches, window synced");
	}

	/** The blacksmith rolls an accessory's options again (same line count, no evolution) and Brokk enhances it with orbs and coins. */
	private static String accessoryBenches(final ServerPlayer player, final ItemStack piece) {
		AccessoryDef def = Accessories.def(piece);
		Wallet.add(player, 1_000_000);
		UpgradeMenu upgrade = new UpgradeMenu(0, player.getInventory(), null);
		upgrade.slots.get(0).set(piece);
		if (!upgrade.canReroll() || !upgrade.targets().isEmpty() || upgrade.clickMenuButton(player, 0)) {
			return "the blacksmith should take an accessory for options only";
		}
		GearRolls before = piece.getOrDefault(ModDataComponents.GEAR_ROLLS, GearRolls.EMPTY);
		player.getInventory().add(new ItemStack(ModItems.CONDENSED_ESSENCE, GearUpgrades.REROLL_CONDENSED));
		int wallet = Wallet.balance(player);
		if (!upgrade.clickMenuButton(player, UpgradeMenu.BUTTON_REROLL)) {
			return "rolling an accessory's options should work";
		}
		GearRolls pending = upgrade.input().get(ModDataComponents.GEAR_ROLLS_PENDING);
		if (pending == null || pending.lines().size() != before.lines().size() || !before.equals(upgrade.input().get(ModDataComponents.GEAR_ROLLS))) {
			return "an accessory roll waits beside the old options with as many lines, got " + pending;
		}
		if (wallet - Wallet.balance(player) != UpgradeMenu.rerollCoins(piece) || UpgradeMenu.rerollCoins(piece) <= 0) {
			return "an accessory roll costs " + UpgradeMenu.rerollCoins(piece) + " coins, paid " + (wallet - Wallet.balance(player));
		}
		if (!upgrade.clickMenuButton(player, UpgradeMenu.BUTTON_APPLY) || !pending.equals(upgrade.input().get(ModDataComponents.GEAR_ROLLS))) {
			return "applying should take the accessory's new options";
		}
		EnhanceMenu enhance = new EnhanceMenu(0, player.getInventory(), null);
		enhance.slots.get(0).set(upgrade.input().copy());
		if (enhance.clickMenuButton(player, EnhanceMenu.BUTTON_ENHANCE)) {
			return "enhancing an accessory needs orbs";
		}
		player.getInventory().add(new ItemStack(ModItems.ACCESSORY_ORB, 2));
		wallet = Wallet.balance(player);
		for (int target = 1; target <= 2; target++) {
			if (!enhance.clickMenuButton(player, EnhanceMenu.BUTTON_ENHANCE) || enhance.result() != EnhanceMenu.RESULT_SUCCESS) {
				return "+" + target + " on an accessory always succeeds";
			}
		}
		if (Enhancement.level(enhance.input()) != 2 || JobProgression.count(player.getInventory(), ModItems.ACCESSORY_ORB) != 0
			|| wallet - Wallet.balance(player) != AccessoryEnhance.coins(def, 1) + AccessoryEnhance.coins(def, 2)) {
			return "+2 should cost two orbs and " + (AccessoryEnhance.coins(def, 1) + AccessoryEnhance.coins(def, 2)) + " coins";
		}
		if (enhance.clickMenuButton(player, EnhanceMenu.BUTTON_PROTECTED) || enhance.clickMenuButton(player, EnhanceMenu.BUTTON_AWAKEN)) {
			return "accessories have no protected enhancement or awakening";
		}
		return accessoryInheritance(player);
	}

	/** A +5 tier 1 ring with artisan's spirit and two options becomes the tier 2 ring the player picks, keeping all of it, for a tier 1 ticket. */
	private static String accessoryInheritance(final ServerPlayer player) {
		AccessoryDef low = Accessories.def(Accessories.setOf(RaidBosses.VERDESSA), AccessorySlot.RING);
		ItemStack ring = Accessories.create(low, 2, RandomSource.create(3L));
		ring.set(ModDataComponents.ENHANCEMENT, new Enhancement(5, 10, 0));
		UpgradeMenu bench = new UpgradeMenu(0, player.getInventory(), null);
		bench.slots.get(0).set(ring.copy());
		AccessoryDef target = Accessories.inheritTargets(low).get(1);
		if (bench.clickMenuButton(player, 1)) {
			return "inheriting a tier 1 ring needs a tier 1 ticket";
		}
		player.getInventory().add(new ItemStack(ModItems.ACCESSORY_INHERIT_2));
		if (bench.clickMenuButton(player, 1)) {
			return "a tier 2 ticket does not inherit a tier 1 piece";
		}
		player.getInventory().add(new ItemStack(ModItems.ACCESSORY_INHERIT_1));
		int wallet = Wallet.balance(player);
		if (!bench.clickMenuButton(player, 1)) {
			return "a tier 1 ticket should inherit the ring";
		}
		ItemStack result = bench.input();
		if (Accessories.def(result) != target) {
			return "the ring should become " + target.id() + ", is " + result;
		}
		Enhancement kept = Enhancement.of(result);
		if (kept.level() != 5 || kept.pity() != 10) {
			return "inheritance keeps +5 and the artisan's spirit, got " + kept;
		}
		List<StatLine> before = ring.getOrDefault(ModDataComponents.GEAR_ROLLS, GearRolls.EMPTY).lines();
		List<StatLine> after = result.getOrDefault(ModDataComponents.GEAR_ROLLS, GearRolls.EMPTY).lines();
		if (after.size() != before.size()) {
			return "inheritance keeps the options, got " + after + " from " + before;
		}
		for (int i = 0; i < before.size(); i++) {
			if (after.get(i).stat() != before.get(i).stat() || after.get(i).value() < before.get(i).value()) {
				return "an inherited option keeps its stat and grows with the level, got " + after + " from " + before;
			}
		}
		if (Bags.count(player, ModItems.ACCESSORY_INHERIT_1) != 0 || Bags.count(player, ModItems.ACCESSORY_INHERIT_2) != 1
			|| wallet - Wallet.balance(player) != Accessories.inheritCoins(target)) {
			return "inheriting costs one tier 1 ticket and " + Accessories.inheritCoins(target) + " coins";
		}
		Bags.take(player, ModItems.ACCESSORY_INHERIT_2, 1);
		return "";
	}

	private static void checkDeathInRaid(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("effect clear @p");
		server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			// a Phoenix Feather among the last raid's supplies would revive the player before the raid catches the fall
			player.getInventory().clearContent();
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
		// second lot: the leader switches to dice and is the only one to roll (the offline ghost is not waited for)
		waitFor(context, server, s -> lot(session[0], 1).state() == LootSession.State.RUNNING, 200, "the second lot should open");
		server.runOnServer(s -> LootSessions.handle(connection.getServerPlayer(), new LootActionPayload(session[0], LootActionPayload.Action.SWITCH_MODE, 0)));
		context.waitTicks(4);
		context.takeScreenshot("raid_loot_dice");
		server.runOnServer(s -> LootSessions.handle(connection.getServerPlayer(), new LootActionPayload(session[0], LootActionPayload.Action.ROLL, 0)));
		waitFor(context, server, s -> lot(session[0], 1).state() == LootSession.State.DONE, 60, "a dice round should end once everyone online rolled");
		String dice = server.computeOnServer(s -> {
			LootSession.Lot lot = lot(session[0], 1);
			return lot.mode() == LootSession.Mode.DICE && connection.getServerPlayer().getUUID().equals(lot.winner()) ? "" : "the only roller should win the dice";
		});
		require(dice.isEmpty(), dice);
		checkLootForfeits(context, server, connection, ghost);
		server.runCommand("tick rate 20");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
	}

	/**
	 * Giving one lot up, a long lot list (scrolling), a new session taking its players out of an older one, and leaving a session
	 * (bid refunded, the last player left takes the rest).
	 */
	private static void checkLootForfeits(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection, final UUID ghost) {
		int many = 12;
		int second = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			Map<UUID, String> who = new LinkedHashMap<>();
			who.put(player.getUUID(), player.getPlainTextName());
			who.put(ghost, "Ghost");
			RandomSource random = RandomSource.create(11);
			List<ItemStack> items = new ArrayList<>();
			for (int i = 0; i < many; i++) {
				items.add(GearDrops.pick(null, 20, 30, random));
			}
			return LootSessions.start(s, RaidBosses.ARACHNE, who, player.getUUID(), items).id();
		});
		waitFor(context, server, s -> lot(second, 0).state() == LootSession.State.RUNNING, 200, "the long session's first lot should open");
		context.waitForScreen(LootScreen.class);
		context.waitTicks(4);
		context.takeScreenshot("raid_loot_many");
		requireFits(context, "long loot screen");
		// giving an auction up with no bids: everyone online gave it up, so it goes straight to dice; passing that too gives it to the leader
		String gaveUp = server.computeOnServer(s -> {
			LootSessions.handle(connection.getServerPlayer(), new LootActionPayload(second, LootActionPayload.Action.PASS, 0));
			return "";
		});
		require(gaveUp.isEmpty(), gaveUp);
		waitFor(context, server, s -> lot(second, 0).mode() == LootSession.Mode.DICE, 20, "an auction everyone online gave up should go to dice at once");
		server.runOnServer(s -> LootSessions.handle(connection.getServerPlayer(), new LootActionPayload(second, LootActionPayload.Action.PASS, 0)));
		waitFor(context, server, s -> lot(second, 0).state() == LootSession.State.DONE, 20, "a dice round everyone online passed should end at once");
		require(server.computeOnServer(s -> connection.getServerPlayer().getUUID().equals(lot(second, 0).winner())), "a lot everyone passed goes to the leader");
		// the list follows the current lot: scroll the screen to the end and back by the wheel
		String scrolled = context.computeOnClient(minecraft -> {
			if (!(minecraft.gui.screen() instanceof LootScreen screen)) {
				return "the loot screen should stay open";
			}
			for (int i = 0; i < many; i++) {
				screen.mouseScrolled(screen.width / 2.0, screen.height / 2.0, 0, -1);
			}
			return "";
		});
		require(scrolled.isEmpty(), scrolled);
		context.waitTicks(2);
		context.takeScreenshot("raid_loot_scrolled");
		// a new session takes the player out of the old one; the ghost, the only one left there, gets the rest for free
		int third = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			Map<UUID, String> who = new LinkedHashMap<>();
			who.put(player.getUUID(), player.getPlainTextName());
			who.put(ghost, "Ghost");
			RandomSource random = RandomSource.create(13);
			return LootSessions.start(s, RaidBosses.ARACHNE, who, player.getUUID(), List.of(GearDrops.pick(null, 20, 30, random), GearDrops.pick(null, 20, 30, random))).id();
		});
		String moved = server.computeOnServer(s -> {
			LootSession old = LootSessions.get(second);
			if (old == null || !old.finished()) {
				return "the older session should end when its only other player is the one left";
			}
			for (LootSession.Lot lot : old.lots()) {
				if (lot.state() != LootSession.State.DONE) {
					return "every lot of the older session should be given out";
				}
			}
			if (!ghost.equals(old.lots().get(many - 1).winner()) || old.lots().get(many - 1).price() != 0) {
				return "the one left in the older session should get its lots free";
			}
			LootSession now = LootSessions.sessionOf(connection.getServerPlayer().getUUID());
			return now != null && now.id() == third ? "" : "the player should only be in the new session";
		});
		require(moved.isEmpty(), moved);
		// leaving with the highest bid: the bid comes back, the ghost leads and takes the rest
		waitFor(context, server, s -> lot(third, 0).state() == LootSession.State.RUNNING, 200, "the new session's first lot should open");
		context.waitForScreen(LootScreen.class);
		String left = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			int before = Coins.total(player);
			LootSessions.handle(player, new LootActionPayload(third, LootActionPayload.Action.BID, 1));
			if (Coins.total(player) >= before) {
				return "the bid should be held";
			}
			LootSessions.handle(player, new LootActionPayload(third, LootActionPayload.Action.FORFEIT, 0));
			if (Coins.total(player) != before) {
				return "leaving should refund the bid: " + before + " -> " + Coins.total(player);
			}
			LootSession session = LootSessions.get(third);
			if (session == null || !session.finished() || !ghost.equals(session.leader())) {
				return "the ghost should lead and the session end with one player left";
			}
			for (LootSession.Lot lot : session.lots()) {
				if (!ghost.equals(lot.winner())) {
					return "the one left should take every lot";
				}
			}
			LootSession still = LootSessions.sessionOf(player.getUUID());
			return still == null || still.id() != third ? "" : "a player who left is no longer in that session";
		});
		require(left.isEmpty(), left);
		context.waitTicks(4);
		require(context.computeOnClient(minecraft -> !(minecraft.gui.screen() instanceof LootScreen) && RaidClient.loot() == null),
			"leaving should close the loot screen and forget the session");
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
