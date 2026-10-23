package com.minecraftmode.test;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.bounty.Bounties;
import com.minecraftmode.city.CityServices;
import com.minecraftmode.city.DailyBread;
import com.minecraftmode.city.StarterKit;
import com.minecraftmode.client.BossBarLayout;
import com.minecraftmode.client.companion.CompanionScreen;
import com.minecraftmode.client.craft.CraftScreen;
import com.minecraftmode.client.dungeon.DungeonScreen;
import com.minecraftmode.client.endgame.CodexScreen;
import com.minecraftmode.client.endgame.EnhanceScreen;
import com.minecraftmode.client.endgame.TalentScreen;
import com.minecraftmode.client.guide.GuideScreen;
import com.minecraftmode.client.map.FieldBosses;
import com.minecraftmode.client.map.MapClient;
import com.minecraftmode.client.map.MapScreen;
import com.minecraftmode.client.map.MapSettings;
import com.minecraftmode.client.map.WaypointScreen;
import com.minecraftmode.client.map.Waypoints;
import com.minecraftmode.companion.Companions;
import com.minecraftmode.companion.MountEntity;
import com.minecraftmode.companion.PetEntity;
import com.minecraftmode.consumable.Consumables;
import com.minecraftmode.entity.CreatureAnim;
import com.minecraftmode.craft.CraftMenu;
import com.minecraftmode.craft.CraftPrices;
import com.minecraftmode.craft.CraftRecipes;
import com.minecraftmode.craft.Crops;
import com.minecraftmode.craft.FarmCropBlock;
import com.minecraftmode.craft.Fishing;
import com.minecraftmode.craft.Herbs;
import com.minecraftmode.craft.Profession;
import com.minecraftmode.dungeon.DungeonData;
import com.minecraftmode.dungeon.DungeonDef;
import com.minecraftmode.dungeon.DungeonDimension;
import com.minecraftmode.dungeon.DungeonInstance;
import com.minecraftmode.dungeon.DungeonLayout;
import com.minecraftmode.dungeon.Dungeons;
import com.minecraftmode.dungeon.Keystone;
import com.minecraftmode.dungeon.KeystoneItem;
import com.minecraftmode.economy.ShopOffers;
import com.minecraftmode.economy.ShopType;
import com.minecraftmode.economy.Wallet;
import com.minecraftmode.enchantment.EnchantLevels;
import com.minecraftmode.enchantment.LifeEnchantments;
import com.minecraftmode.enhance.EnhanceMenu;
import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.entity.CityNpc;
import com.minecraftmode.entity.MobPower;
import com.minecraftmode.entity.named.NamedMob;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.event.WorldEvents;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.Paragon;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.gear.ClassArmor;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.GearStats;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.job.skill.Actions;
import com.minecraftmode.job.skill.CombatHooks;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.WeaponDef;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.loot.GearIndex;
import com.minecraftmode.network.OpenDungeonPayload;
import com.minecraftmode.progress.Progress;
import com.minecraftmode.progress.ResetCycle;
import com.minecraftmode.registry.ModAttachments;
import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.registry.ModEntities;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.story.Story;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Everything added after the endgame: paragon levels and awakening past +15, professions (recipes, herbs, stations), pets (bonus,
 * levels) and mounts (riding, flying), a keystone dungeon run (halls, doors, falling, the champion, the upgraded keystone) and a
 * depleted one through the warden, a titan world boss with shared rewards, the main story through the herald, the new
 * achievements, and screenshots of every new screen and of the pets and mounts.
 */
public class ContentClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(final ClientGameTestContext context) {
		if (TestSelection.skip(this)) {
			return;
		}
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
			server.runCommand("effect give @p resistance 999999 4 true");
			context.waitTicks(5);
			server.runOnServer(s -> {
				ServerPlayer player = connection.getServerPlayer();
				player.getInventory().clearContent();
				JobProgression.set(player, JobProgression.get(player).withJob(JobClass.WARRIOR, 4).withProgress(JobProgression.MAX_LEVEL, 0));
				Wallet.add(player, 2000 * Coins.GOLD);
			});

			checkParagon(server, connection);
			checkAwakening(context, server, connection);
			checkProfessions(context, server, connection);
			checkFarming(server, connection);
			checkFishing(server, connection);
			checkCompanions(context, server, connection);
			checkDungeon(context, server, connection);
			checkWarden(context, server, connection);
			checkTitan(context, server, connection);
			checkStory(context, server, connection);
			checkPlazaNpcs(context, server, connection);
			checkCrierAndStylist(context, server, connection);
			screens(context, server, connection);
			checkMapAndLight(context, server, connection);
			WorldClose.prepare(context, server);
		}
	}

	// ------------------------------------------------------------------ paragon and awakening

	private static void checkParagon(final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			require(Paragon.get(player).level() == 0, "paragon starts at 0");
			JobProgression.addExp(player, Paragon.expToNext(0) + Paragon.expToNext(1) + 10);
			Paragon.ParagonData data = Paragon.get(player);
			require(JobProgression.get(player).level() == JobProgression.MAX_LEVEL && data.level() == 2, "experience past level 100 makes paragon levels, got " + data.level());
			require(data.available() == 2, "every paragon level gives a point");
			GearStats.invalidate(player);
			float before = GearStats.of(player).get(EngraveStat.BASIC_DAMAGE);
			require(Paragon.spend(player, Paragon.Stat.MIGHT.id), "a point goes into Might");
			GearStats.invalidate(player);
			float after = GearStats.of(player).get(EngraveStat.BASIC_DAMAGE);
			require(Paragon.get(player).rank(Paragon.Stat.MIGHT) == 1 && Math.abs(after - before - 1.0F) < 0.001F, "Might adds 1% basic damage, got " + (after - before));
			require(Paragon.reset(player) && Paragon.get(player).available() == 2, "a reset refunds every point");
			// past the top rank: Precision stops, Might (endless) goes on at half a rank per point
			player.setAttached(ModAttachments.PARAGON, new Paragon.ParagonData(102, 0,
				Map.of(Paragon.Stat.PRECISION.id, Paragon.MAX_RANK, Paragon.Stat.MIGHT.id, Paragon.MAX_RANK)));
			require(!Paragon.spend(player, Paragon.Stat.PRECISION.id), "Precision stops at rank " + Paragon.MAX_RANK);
			GearStats.invalidate(player);
			float atMax = GearStats.of(player).get(EngraveStat.SKILL_DAMAGE);
			require(Paragon.spend(player, Paragon.Stat.MIGHT.id) && Paragon.get(player).rank(Paragon.Stat.MIGHT) == Paragon.MAX_RANK + 1,
				"Might keeps taking points past rank " + Paragon.MAX_RANK);
			GearStats.invalidate(player);
			float past = GearStats.of(player).get(EngraveStat.SKILL_DAMAGE) - atMax;
			require(Math.abs(past - 0.5F) < 0.001F, "a Might rank past the top adds 0.5% skill damage, got " + past);
			player.setAttached(ModAttachments.PARAGON, new Paragon.ParagonData(data.level(), data.exp(), Map.of()));
			GearStats.invalidate(player);
			Paragon.spend(player, Paragon.Stat.MIGHT.id);
			return "paragon " + data.level() + ", might +" + (after - before) + ", past the top +" + past;
		});
		MinecraftMode.LOGGER.info("[content] {}", report);
	}

	private static void checkAwakening(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ClassGear gear = ClassGear.of(activeWeapon(JobProgression.get(player)));
			ItemStack weapon = GearIndex.stack(gear);
			weapon.set(ModDataComponents.ENHANCEMENT, new Enhancement(Enhancement.MAX, 0));
			player.getInventory().add(new ItemStack(ModItems.AWAKENING_CRYSTAL, 16));
			EnhanceMenu menu = new EnhanceMenu(0, player.getInventory(), null);
			menu.slots.get(0).set(weapon.copy());
			require(menu.canAwaken(), "a +15 weapon can be awakened");
			int crystals = JobProgression.count(player.getInventory(), ModItems.AWAKENING_CRYSTAL);
			require(menu.clickMenuButton(player, EnhanceMenu.BUTTON_AWAKEN) && menu.result() == EnhanceMenu.RESULT_AWAKENED, "awakening goes through");
			ItemStack awakened = menu.input().copy();
			require(Enhancement.of(awakened).awaken() == 1 && Enhancement.level(awakened) == Enhancement.MAX, "the weapon is +15 ✦1");
			require(crystals - JobProgression.count(player.getInventory(), ModItems.AWAKENING_CRYSTAL) == Enhancement.crystals(1), "awakening uses crystals");
			require(awakened.getHoverName().getString().contains("✦1"), "the name shows the awakening, got " + awakened.getHoverName().getString());
			require(Progress.get(player).maxAwaken() == 1 && Progress.get(player).has("awaken_1"), "awakening counts toward achievements");
			player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
			GearStats.invalidate(player);
			float plain = GearStats.of(player).get(EngraveStat.BASIC_DAMAGE);
			player.setItemInHand(InteractionHand.MAIN_HAND, awakened);
			GearStats.invalidate(player);
			float stronger = GearStats.of(player).get(EngraveStat.BASIC_DAMAGE);
			require(Math.abs(stronger - plain - 2.0F) < 0.01F, "✦1 adds 2% basic damage, got " + (stronger - plain));
			// four +15 ✦5 armor pieces leave room under the 50% damage reduction cap (they filled it alone): +1..+10 0.5% a step,
			// +11..+15 0.3% and 0.5% final damage, ✦ 0.2% each: 4 x (5 + 1.5 + 1) = 30% and 4 x 2.5 = 10% final damage
			ClassGear chest = ClassGear.of(ClassArmor.pieces(JobClass.WARRIOR).getFirst());
			float reduction = 4.0F * (float)Enhancement.lines(chest, Enhancement.MAX, 5).stream()
				.filter(l -> l.stat() == EngraveStat.DAMAGE_REDUCTION).mapToDouble(StatLine::value).sum();
			require(Math.abs(reduction - 30.0F) < 0.01F, "four +15 ✦5 armor pieces should give 30% damage reduction, give " + reduction);
			float armorFinal = 4.0F * (float)Enhancement.lines(chest, Enhancement.MAX, 5).stream()
				.filter(l -> l.stat() == EngraveStat.FINAL_DAMAGE).mapToDouble(StatLine::value).sum();
			require(Math.abs(armorFinal - 10.0F) < 0.01F && Enhancement.lines(chest, 10).stream().noneMatch(l -> l.stat() == EngraveStat.FINAL_DAMAGE),
				"four +15 armor pieces should give 10% final damage and +10 none, give " + armorFinal);
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			String name = awakened.getHoverName().getString();
			// kept in the backpack (the hand is used for charms and whistles later) for the awakening screenshot
			player.getInventory().setItem(30, awakened);
			return name;
		});
		MinecraftMode.LOGGER.info("[content] awakened: {}", report);
	}

	// ------------------------------------------------------------------ farming and fishing

	private static void checkFarming(final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = player.level();
			require(Crops.all().size() == 12 && Crops.seeds().size() == 9, "12 crops, 9 of them with seeds");
			Crops.CropDef rice = Crops.def("rice");
			FarmCropBlock crop = (FarmCropBlock)Crops.block(rice);
			// a row of three ripe rice plants on farmland, away from everything else
			BlockPos center = new BlockPos(-14, -60, 9);
			for (int dx = -1; dx <= 1; dx++) {
				level.setBlockAndUpdate(center.offset(dx, -1, 0), Blocks.FARMLAND.defaultBlockState());
				level.setBlockAndUpdate(center.offset(dx, 0, 0), crop.getStateForAge(crop.getMaxAge()));
			}
			BlockPos young = center.offset(0, 0, 2);
			level.setBlockAndUpdate(young.below(), Blocks.FARMLAND.defaultBlockState());
			level.setBlockAndUpdate(young, crop.getStateForAge(0));
			require(!BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, young) && crop.getAge(level.getBlockState(young)) == 0,
				"bone meal does nothing on the mod's crops");
			require(!crop.defaultBlockState().is(BlockTags.BEE_GROWABLES), "bees do not grow the mod's crops either");

			// a hoe with Reaping I and Replant harvests the row and plants it again
			ItemStack hoe = new ItemStack(Items.IRON_HOE);
			hoe.enchant(EnchantLevels.holder(level, LifeEnchantments.REAPING.key()).orElseThrow(), 1);
			hoe.enchant(EnchantLevels.holder(level, LifeEnchantments.REPLANT.key()).orElseThrow(), 1);
			ItemStack held = player.getMainHandItem().copy();
			player.setItemInHand(InteractionHand.MAIN_HAND, hoe);
			int exp = Profession.data(player).exp(Profession.FARMING);
			require(player.gameMode.destroyBlock(center), "the ripe rice can be harvested");
			player.setItemInHand(InteractionHand.MAIN_HAND, held);
			int gained = Profession.data(player).exp(Profession.FARMING) - exp;
			require(gained >= 3 * Crops.BASIC_EXP, "three ripe crops teach farming (Reaping), got " + gained);
			for (int dx = -1; dx <= 1; dx++) {
				BlockState state = level.getBlockState(center.offset(dx, 0, 0));
				require(state.is(crop) && crop.getAge(state) == 0, "Replant planted the rice again at " + dx + ", got " + state);
			}
			int riceDropped = 0;
			for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(3.0))) {
				if (drop.getItem().is(Crops.produce(rice))) {
					riceDropped += drop.getItem().getCount();
				}
				drop.discard();
			}
			require(riceDropped >= 3, "every harvested plant drops rice, got " + riceDropped);

			// advanced seeds only for farmers of the advanced level
			RandomSource random = RandomSource.create(7);
			Map<String, Integer> withoutFarming = new HashMap<>(Profession.data(player).exp());
			withoutFarming.remove(Profession.FARMING.id);
			player.setAttached(ModAttachments.PROFESSIONS, new Profession.Data(withoutFarming));
			int advancedLow = 0;
			for (int i = 0; i < 3000; i++) {
				Item seed = Crops.grassSeed(player, 3, random);
				if (seed != null && Crops.advanced().stream().anyMatch(d -> Crops.planting(d) == seed)) {
					advancedLow++;
				}
			}
			Profession.FARMING.addExp(player, Profession.totalFor(Crops.ADVANCED_LEVEL));
			int seeds = 0;
			int advanced = 0;
			for (int i = 0; i < 3000; i++) {
				Item seed = Crops.grassSeed(player, 3, random);
				if (seed != null) {
					seeds++;
					if (Crops.advanced().stream().anyMatch(d -> Crops.planting(d) == seed)) {
						advanced++;
					}
				}
			}
			require(advancedLow == 0, "no advanced seeds below farming level " + Crops.ADVANCED_LEVEL + ", got " + advancedLow);
			require(seeds > 200 && advanced > 0, "grass gives seeds (" + seeds + ") and, from level " + Crops.ADVANCED_LEVEL + ", advanced ones (" + advanced + ")");

			// the grocer buys every crop and sells the basic seeds
			List<ShopOffers.Trade> grocer = ShopOffers.trades(ShopType.GROCER);
			for (Crops.CropDef def : Crops.all()) {
				require(grocer.stream().anyMatch(t -> t.cost().asItem() == Crops.produce(def) && Coins.value(t.result().asItem()) > 0), "the grocer buys " + def.id());
				require(def.advanced() || grocer.stream().anyMatch(t -> t.result().asItem() == Crops.planting(def)), "the grocer sells what " + def.id() + " grows from");
			}
			return "farming +" + gained + ", seeds " + seeds + " (" + advanced + " advanced), rice " + riceDropped;
		});
		MinecraftMode.LOGGER.info("[content] {}", report);
	}

	private static void checkFishing(final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = player.level();
			BlockPos plains = player.blockPosition();
			RandomSource random = RandomSource.create(11);
			Set<String> low = new HashSet<>();
			Set<String> anywhere = new HashSet<>();
			Set<String> master = new HashSet<>();
			for (int i = 0; i < 3000; i++) {
				low.add(Fishing.fishFor(level, plains, 1, false, 0, 0.0F, random).id());
				anywhere.add(Fishing.fishFor(level, plains, 1, true, 0, 0.0F, random).id());
				master.add(Fishing.fishFor(level, plains, Profession.MAX_LEVEL, false, 3, 3.0F, random).id());
			}
			for (Fishing.FishDef def : Fishing.all()) {
				require(def.minLevel() <= 1 || !low.contains(def.id()) && !anywhere.contains(def.id()), def.id() + " needs fishing level " + def.minLevel());
				require(def.habitat() == Fishing.Habitat.ANY || !low.contains(def.id()) && !master.contains(def.id()), def.id() + " does not live in the plains");
				require(def.habitat() != Fishing.Habitat.ANY || master.contains(def.id()), def.id() + " bites anywhere for a master angler");
				require(def.habitat() == Fishing.Habitat.ANY || def.minLevel() > 1 || anywhere.contains(def.id()), "Any Waters brings " + def.id() + " to the plains");
			}

			// Net Cast II: sometimes two; every catch teaches fishing
			ItemStack rod = new ItemStack(Items.FISHING_ROD);
			rod.enchant(EnchantLevels.holder(level, LifeEnchantments.NET_CAST.key()).orElseThrow(), 2);
			int exp = Profession.data(player).exp(Profession.FISHING);
			int doubles = 0;
			for (int i = 0; i < 100; i++) {
				ItemStack fish = Fishing.land(player, player.position(), rod, 0.0F, random);
				require(Fishing.all().stream().anyMatch(d -> fish.is(Fishing.item(d))), "a landed fish is one of the mod's, got " + fish);
				if (fish.getCount() >= 2) {
					doubles++;
				}
			}
			int gained = Profession.data(player).exp(Profession.FISHING) - exp;
			require(doubles > 0 && gained >= 100 * Fishing.Tier.COMMON.exp, "Net Cast doubles some catches (" + doubles + ") and fishing is taught (" + gained + ")");

			List<ShopOffers.Trade> grocer = ShopOffers.trades(ShopType.GROCER);
			for (Fishing.FishDef def : Fishing.all()) {
				require(grocer.stream().anyMatch(t -> t.cost().asItem() == Fishing.item(def) && Coins.value(t.result().asItem()) > 0), "the grocer buys " + def.id());
			}

			// a dish of the new crops: kimchi at the kitchen station
			List<CraftRecipes.Recipe> cooking = CraftRecipes.of(Profession.COOKING);
			int index = 0;
			while (!cooking.get(index).id().equals("kimchi")) {
				index++;
			}
			CraftRecipes.Recipe kimchi = cooking.get(index);
			for (CraftRecipes.Ingredient in : kimchi.ingredients()) {
				player.getInventory().add(new ItemStack(in.item(), in.count()));
			}
			CraftMenu menu = new CraftMenu(CraftMenu.type(Profession.COOKING), 0, player.getInventory(), Profession.COOKING, null);
			Item made = Consumables.item("kimchi");
			int before = JobProgression.count(player.getInventory(), made);
			require(menu.clickMenuButton(player, index * 2) && JobProgression.count(player.getInventory(), made) > before, "kimchi is cooked from the new crops");
			require(CraftPrices.value(made) > 0.0F, "the grocer buys kimchi");
			return "fish " + low.size() + "/" + anywhere.size() + "/" + master.size() + " kinds, doubles " + doubles + ", fishing +" + gained;
		});
		MinecraftMode.LOGGER.info("[content] {}", report);
	}

	// ------------------------------------------------------------------ professions

	private static void checkProfessions(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = player.level();
			List<String> lines = new ArrayList<>();
			for (Profession profession : Profession.CRAFTING) {
				CraftRecipes.Recipe recipe = CraftRecipes.of(profession).getFirst();
				for (CraftRecipes.Ingredient in : recipe.ingredients()) {
					player.getInventory().add(new ItemStack(in.item(), in.count() * 2));
				}
				CraftMenu menu = new CraftMenu(CraftMenu.type(profession), 0, player.getInventory(), profession, null);
				require(CraftMenu.makeable(player, recipe) >= 1, profession.id + ": the first recipe should be makeable");
				Item made = recipe.output().apply(player).getItem();
				int before = JobProgression.count(player.getInventory(), made);
				require(menu.clickMenuButton(player, 0), profession.id + ": crafting goes through");
				require(JobProgression.count(player.getInventory(), made) > before, profession.id + ": the product lands in the inventory");
				require(Profession.data(player).exp(profession) > 0, profession.id + ": crafting teaches the profession");
				lines.add(profession.id + " " + recipe.id());
			}
			require(Herbs.herbFor(level, new BlockPos(0, -60, 0), Blocks.SHORT_GRASS.defaultBlockState()) == ModItems.SUNLEAF, "grass in the plains gives sunleaf");
			require(Herbs.herbFor(level, new BlockPos(0, -60, 0), Blocks.RED_MUSHROOM.defaultBlockState()) == ModItems.GLOWCAP, "mushrooms give glowcap");
			require(Herbs.herbFor(level, new BlockPos(0, -60, 0), Blocks.STONE.defaultBlockState()) == null, "stone gives no herb");
			Profession.COOKING.addExp(player, Profession.totalFor(10));
			require(Profession.COOKING.level(player) >= 10 && Progress.get(player).has("profession_10"), "profession levels count toward achievements");
			return String.join(", ", lines);
		});
		MinecraftMode.LOGGER.info("[content] crafted: {}", report);
	}

	// ------------------------------------------------------------------ pets and mounts

	private static void checkCompanions(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = player.level();
			// a pet charm adds the pet and summons it; using it again is refused
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Companions.petItem("ember_fox")));
			player.getMainHandItem().use(level, player, InteractionHand.MAIN_HAND);
			require(Companions.data(player).hasPet("ember_fox") && player.getMainHandItem().isEmpty(), "the charm teaches the pet and is used up");
			PetEntity pet = Companions.summonedPet(player);
			require(pet != null && pet.distanceTo(player) < 4, "the pet is summoned next to its owner");
			GearStats.invalidate(player);
			require(Math.abs(GearStats.of(player).get(EngraveStat.ITEM_FIND) - Companions.pet("ember_fox").lines(1).getFirst().value()) < 0.01F,
				"the summoned pet gives its bonus");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Companions.petItem("ember_fox")));
			player.getMainHandItem().use(level, player, InteractionHand.MAIN_HAND);
			require(!player.getMainHandItem().isEmpty(), "a known pet's charm is not used up");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			// the owner's kills raise the pet
			Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			zombie.snapTo(player.getX() + 3, player.getY(), player.getZ(), 0, 0);
			level.addFreshEntity(zombie);
			zombie.hurtServer(level, player.damageSources().playerAttack(player), 10000.0F);
			require(Companions.data(player).pets().get("ember_fox") == 1, "a kill gives the summoned pet experience");
			require(Companions.summonedPet(player).anim() == CreatureAnim.LEAP, "the fox should leap for its owner's kill");
			// a mount whistle: ride it, the flyer keeps the rider, the key gets off and sends it away
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Companions.mountItem("storm_griffin")));
			player.getMainHandItem().use(level, player, InteractionHand.MAIN_HAND);
			require(player.getVehicle() instanceof MountEntity mount && mount.def() != null && mount.def().flying(), "the whistle puts the player on the griffin");
			MountEntity mount = (MountEntity)player.getVehicle();
			require(mount.getControllingPassenger() == player, "the owner steers the mount");
			// landings: a flyer's glide is never a fall; a ground mount passes on only what a horse would feel (6 blocks safe, half damage)
			// (the test player wears Resistance V, which would hide any fall damage: take it off for these checks)
			player.removeEffect(MobEffects.RESISTANCE);
			float health = player.getHealth();
			mount.causeFallDamage(30.0, 1.0F, player.damageSources().fall());
			require(player.getHealth() == health, "landing the griffin after a 30-block glide should not hurt the rider");
			Companions.toggleMount(player, "");
			require(player.getVehicle() == null && mount.isRemoved(), "the mount key gets off and the mount leaves");
			MountEntity stag = Companions.mountType(Companions.mount("crystal_stag")).create(level, EntitySpawnReason.COMMAND);
			stag.snapTo(player.getX(), player.getY(), player.getZ(), 0, 0);
			level.addFreshEntity(stag);
			stag.setup(player);
			require(player.startRiding(stag, true, false), "the player should ride the stag");
			stag.causeFallDamage(5.0, 1.0F, player.damageSources().fall());
			require(player.getHealth() == health, "a 5-block jump on the stag should not hurt the rider");
			stag.causeFallDamage(10.0, 1.0F, player.damageSources().fall());
			require(player.getHealth() < health, "a 10-block drop on the stag should still hurt the rider");
			player.stopRiding();
			stag.discard();
			player.setHealth(player.getMaxHealth());
			player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, MobEffectInstance.INFINITE_DURATION, 4, false, false));
			Companions.summonPet(player, "");
			require(Companions.summonedPet(player) == null && Companions.lines(player).isEmpty(), "dismissing the pet removes its bonus");
			// the whole collection for the screen and the lineup
			Map<String, Integer> pets = new HashMap<>();
			Companions.pets().forEach(d -> pets.put(d.id(), d.id().equals("ember_fox") ? Companions.petExpFor(4) : 0));
			player.setAttached(ModAttachments.COMPANIONS, new Companions.Data(pets, Companions.mounts().stream().map(Companions.MountDef::id).toList(), "", "storm_griffin", Map.of()));
			Progress.changed(player);
			require(Progress.get(player).has("pets_all") && Progress.get(player).has("mounts_all"), "full collections count toward achievements");
			Companions.summonPet(player, "ember_fox");
			return "pet level " + Companions.petLevel(Companions.data(player).pets().get("ember_fox"));
		});
		MinecraftMode.LOGGER.info("[content] companions: {}", report);

		// companions and the caster's own summons never stop a shot (until 0.5.20 the pet caught its owner's arrows, which bounced off)
		server.runCommand("tp @p 20.5 -60 -6.5 -90 0");
		context.waitTicks(5);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = player.level();
			PetEntity pet = Companions.summonedPet(player);
			require(pet != null && !pet.canBeHitByProjectile() && !CombatHooks.canHarm(player, pet), "a pet is no target for shots");
			MountEntity stag = Companions.mountType(Companions.mount("crystal_stag")).create(level, EntitySpawnReason.COMMAND);
			require(!stag.canBeHitByProjectile() && !CombatHooks.canHarm(player, stag), "a mount is no target for shots");
			Zombie summon = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			Actions.markSummon(summon, player, 1.0);
			require(!CombatHooks.canHarm(player, summon), "the caster's own skill summon is no target for shots");
			// a real arrow through the pet into a husk behind it
			pet.setNoAi(true);
			pet.snapTo(player.getX() + 3.0, player.getY(), player.getZ(), 0, 0);
			Husk husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
			husk.setNoAi(true);
			husk.snapTo(player.getX() + 8.0, player.getY(), player.getZ(), 0, 0);
			husk.addTag("pet_shot_target");
			level.addFreshEntity(husk);
			Arrow arrow = new Arrow(level, player, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
			arrow.setPos(player.getX() + 1.0, player.getY() + 0.4, player.getZ());
			arrow.setDeltaMovement(2.0, 0.05, 0.0);
			level.addFreshEntity(arrow);
		});
		context.waitTicks(20);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			Husk husk = player.level().getEntitiesOfClass(Husk.class, player.getBoundingBox().inflate(16), h -> h.entityTags().contains("pet_shot_target")).getFirst();
			require(husk.getHealth() < husk.getMaxHealth(), "the arrow should fly through the pet and hit the husk behind it");
			husk.discard();
			PetEntity pet = Companions.summonedPet(player);
			if (pet != null) {
				pet.setNoAi(false);
			}
			// a sprinting rider speeds any mount up; flyers take off with jump held, so they show no jump bar
			MountEntity griffin = Companions.mountType(Companions.mount("storm_griffin")).create(player.level(), EntitySpawnReason.COMMAND);
			MountEntity stallion = Companions.mountType(Companions.mount("swift_stallion")).create(player.level(), EntitySpawnReason.COMMAND);
			require(griffin.canSprint() && stallion.canSprint() && !griffin.canJump() && stallion.canJump(), "mounts let riders sprint; only ground mounts charge a jump");
		});

		// a walking pet sits by an owner who stands still and gets up when they move; a flying one lands on their shoulder
		server.runCommand("tp @p 20.5 -60 0.5 0 0");
		context.waitTicks(PetEntity.SETTLE_TICKS + 40);
		server.runOnServer(s -> {
			PetEntity pet = Companions.summonedPet(connection.getServerPlayer());
			require(pet != null && pet.isResting(), "the fox should sit down by its still owner");
		});
		// crouching by a resting pet makes it hop
		server.runOnServer(s -> connection.getServerPlayer().setShiftKeyDown(true));
		context.waitTicks(2);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.setShiftKeyDown(false);
			PetEntity pet = Companions.summonedPet(player);
			require(pet != null && pet.anim() == CreatureAnim.LEAP, "the fox should hop when its owner crouches by it");
		});
		server.runCommand("tp @p 23.5 -60 0.5 0 0");
		context.waitTicks(3);
		server.runOnServer(s -> {
			PetEntity pet = Companions.summonedPet(connection.getServerPlayer());
			require(pet != null && !pet.isResting(), "the fox should get up when its owner moves");
			Companions.summonPet(connection.getServerPlayer(), "frost_owl");
		});
		context.waitTicks(PetEntity.SETTLE_TICKS + 60);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			PetEntity owl = Companions.summonedPet(player);
			require(owl != null && owl.isResting(), "the owl should land on its still owner's shoulder");
			double above = owl.getY() - player.getY();
			double aside = Math.hypot(owl.getX() - player.getX(), owl.getZ() - player.getZ());
			require(aside < 0.6 && above > 1.2 && above < 1.6, "the perched owl should sit on the shoulder: " + aside + " aside, " + above + " up");
			Companions.summonPet(player, "ember_fox");
		});

		// every pet and mount in a lineup
		server.runCommand("tp @p 20.5 -60 0.5 0 10");
		context.waitTicks(10);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = player.level();
			Companions.summonPet(player, "");
			int i = 0;
			for (Companions.PetDef def : Companions.pets()) {
				PetEntity pet = Companions.petType(def).create(level, EntitySpawnReason.COMMAND);
				pet.setOwner(player.getUUID());
				pet.setNoAi(true);
				pet.snapTo(20.5 - 6.0 + i * 1.7, -60 + (def.flying() ? 1.0 : 0.0), 5.5, 180.0F, 0.0F);
				pet.setYBodyRot(180.0F);
				pet.setYHeadRot(180.0F);
				level.addFreshEntity(pet);
				i++;
			}
			mountLineup(player);
		});
		context.getInput().lookAt(new BlockPos(20, -59, 8));
		context.waitTicks(25);
		shot(context, "content_companions");
		// and resting: walkers sit or lie down, flyers fold their wings
		server.runOnServer(s -> {
			for (PetEntity pet : connection.getServerPlayer().level().getEntitiesOfClass(PetEntity.class, connection.getServerPlayer().getBoundingBox().inflate(30))) {
				pet.setResting(true);
			}
		});
		context.waitTicks(5);
		shot(context, "content_companions_resting");
		// close-ups: resting pets (three or so per shot), then the mounts two by two
		for (int k = 0; k < 3; k++) {
			double x = 15.3 + k * 4.6;
			server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.1f -60 2.3 0 10", x));
			context.waitTicks(10);
			shot(context, "content_pets_close" + k);
		}
		for (int k = 0; k < 3; k++) {
			double x = 13.3 + k * 7.2;
			server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.1f -60 5.5 0 12", x));
			// a mount nobody rides leaves after a few seconds: line them up again for every shot
			server.runOnServer(s -> {
				ServerPlayer player = connection.getServerPlayer();
				player.level().getEntitiesOfClass(MountEntity.class, player.getBoundingBox().inflate(30)).forEach(Entity::discard);
				mountLineup(player);
			});
			context.waitTicks(10);
			shot(context, "content_mounts_close" + k);
		}
		server.runOnServer(s -> {
			for (Entity e : connection.getServerPlayer().level().getEntitiesOfClass(Entity.class, connection.getServerPlayer().getBoundingBox().inflate(30),
				e -> e instanceof PetEntity || e instanceof MountEntity)) {
				e.discard();
			}
			Companions.summonPet(connection.getServerPlayer(), "ember_fox");
		});
	}

	/** Every mount in a row at z 11, facing north. */
	private static void mountLineup(final ServerPlayer player) {
		int i = 0;
		for (Companions.MountDef def : Companions.mounts()) {
			MountEntity mount = Companions.mountType(def).create(player.level(), EntitySpawnReason.COMMAND);
			mount.setup(player);
			mount.setNoAi(true);
			mount.snapTo(20.5 - 9.0 + i * 3.6, -60, 11.0, 180.0F, 0.0F);
			mount.setYBodyRot(180.0F);
			mount.setYHeadRot(180.0F);
			player.level().addFreshEntity(mount);
			i++;
		}
	}

	// ------------------------------------------------------------------ dungeons

	private static void checkDungeon(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		// past vanilla's 1024 health a champion keeps 1000 and takes damage divided by the rest
		server.runOnServer(s -> {
			ServerLevel level = connection.getServerPlayer().level();
			NamedMob champion = NamedMobs.type(NamedMobs.byId("astral_knight")).create(level, EntitySpawnReason.COMMAND);
			champion.makeChampion(10.0F, 1.0F, "entity.minecraft_mode.dungeon_boss", connection.getServerPlayer().blockPosition(), 18.0);
			float wanted = (float)(NamedMobs.byId("astral_knight").health() * 1.3 * 10.0);
			require(champion.getMaxHealth() == MobPower.HEALTH_CAP && Math.abs(MobPower.toughness(champion) * MobPower.HEALTH_CAP - wanted) < 1.0F,
				"a champion wanting " + wanted + " health should keep " + MobPower.HEALTH_CAP + " with toughness, has " + champion.getMaxHealth() + " / " + MobPower.toughness(champion));
			// its 18 armor reads the whole hit: 60 damage loses the floor share (18 x 0.2 / 25 = 14.4%), not the 46% it would take off
			// 60 / toughness (cactus: armor applies, no class bonuses)
			float before = champion.getHealth();
			champion.hurtServer(level, level.damageSources().cactus(), 60.0F);
			float lost = (before - champion.getHealth()) * MobPower.toughness(champion);
			float expected = 60.0F * (1.0F - 18.0F * 0.2F / 25.0F);
			require(Math.abs(lost - expected) < 0.5F, "armor should take 14.4% of a 60 hit on a tough champion: lost " + lost + ", expected " + expected);
			// double strike's second hit repeats the first as it landed: divided by the toughness once (it was twice), no bonuses again
			ServerPlayer striker = connection.getServerPlayer();
			float beforeRepeat = champion.getHealth();
			CombatHooks.deal(striker, champion, 60.0F, striker.damageSources().playerAttack(striker), CombatHooks.DamageKind.REPEAT);
			float repeated = (beforeRepeat - champion.getHealth()) * MobPower.toughness(champion);
			require(Math.abs(repeated - expected) < 0.5F, "a repeated 60 hit should land like the first one (" + expected + "), landed " + repeated);
			// a world boss may heal again; a dungeon boss heals once per fight
			champion.setHealth(champion.getMaxHealth() * 0.4F);
			require(champion.castHeal(level) && champion.canStillHeal(), "a champion under half health should heal and may heal again later");
			NamedMob dungeonBoss = NamedMobs.type(NamedMobs.byId("astral_knight")).create(level, EntitySpawnReason.COMMAND);
			dungeonBoss.makeChampion(10.0F, 1.0F, "entity.minecraft_mode.dungeon_boss", connection.getServerPlayer().blockPosition(), 18.0);
			dungeonBoss.healOnlyOnce();
			dungeonBoss.setHealth(dungeonBoss.getMaxHealth() * 0.4F);
			require(dungeonBoss.castHeal(level) && !dungeonBoss.canStillHeal(), "a dungeon boss should heal once and then never again");
			dungeonBoss.discard();
			champion.discard();
			require(MobPower.toughness(champion) == 1.0F, "a removed champion forgets its toughness");
			require(Dungeons.rewardLevel(20) == 20 && Dungeons.rewardLevel(30) == 25, "rewards grow at half speed past +20");
		});
		DungeonDef def = Dungeons.def("fungal_depths");
		// an item an earlier party left in the run's slot (slot 0 at z 512: the test's first run) is gone when the run opens
		Vec3 slot = new Vec3(5.5, DungeonDimension.FLOOR_Y + 1, 512.5);
		server.runOnServer(s -> SweepChecks.hold(DungeonDimension.level(s), slot, true));
		context.waitTicks(5);
		ItemEntity litter = server.computeOnServer(s -> SweepChecks.litter(DungeonDimension.level(s), slot));
		context.waitTicks(1);
		server.runOnServer(s -> Dungeons.start(s, List.of(connection.getServerPlayer()), def, connection.getServerPlayer().getUUID(), 4,
			connection.getServerPlayer().getUUID()));
		DungeonInstance instance = server.computeOnServer(s -> Dungeons.instances().getFirst());
		server.runOnServer(s -> {
			SweepChecks.check(DungeonDimension.level(s), litter, slot);
			SweepChecks.hold(DungeonDimension.level(s), slot, false);
		});
		require(server.computeOnServer(s -> DungeonDimension.is(connection.getServerPlayer().level())), "the party arrives in the dungeon dimension");
		require(instance.affixes().size() == 1 && instance.level() == 4, "a +4 keystone brings one modifier, got " + instance.affixes());
		waitFor(context, server, s -> instance.state() == DungeonInstance.State.RUN, 200, "the countdown ends");
		require(server.computeOnServer(s -> DungeonDimension.level(s).getBlockState(instance.origin().offset(DungeonLayout.doorX(0), 0, 0)).isAir()), "the first door opens");
		BlockPos origin = instance.origin();
		for (int room = 1; room <= DungeonLayout.HALLS; room++) {
			DungeonLayout.Room r = DungeonLayout.ROOMS.get(room);
			server.runCommand("execute in minecraft_mode:dungeon run tp @p " + (origin.getX() + r.x0() + 3.5) + " " + origin.getY() + " " + (origin.getZ() + 0.5) + " -90 0");
			final int hall = room;
			waitFor(context, server, s -> instance.room() == hall && !instance.roomMobs().isEmpty(), 60, "hall " + room + " wakes its monsters");
			int count = instance.roomMobs().size();
			require(server.computeOnServer(s -> instance.roomMobs().stream().allMatch(id -> MobPower.factor(DungeonDimension.level(s).getEntity(id)) > 1.0F)),
				"dungeon monsters hit harder");
			require(!server.computeOnServer(s -> DungeonDimension.level(s).getBlockState(origin.offset(DungeonLayout.doorX(hall), 0, 0)).isAir()),
				"the next door stays shut while monsters live");
			if (room == 1) {
				// a hall monster pushed out through the wall comes back into the hall (it would block the door forever)
				UUID escaped = server.computeOnServer(s -> {
					Mob mob = (Mob)DungeonDimension.level(s).getEntity(instance.roomMobs().iterator().next());
					mob.teleportTo(origin.getX() + r.centerX() + 0.5, origin.getY(), origin.getZ() + r.halfZ() + 3.5);
					return mob.getUUID();
				});
				context.waitTicks(30);
				require(server.computeOnServer(s -> DungeonLayout.interior(origin, DungeonDimension.level(s).getEntity(escaped).position())),
					"a hall monster outside the walls should be brought back in");
				context.getInput().lookAt(new BlockPos(origin.getX() + r.centerX() + 3, origin.getY() + 1, origin.getZ()));
				context.waitTicks(20);
				shot(context, "content_dungeon_hall");
				requireBossBarsClear(context, "dungeon hall");
			}
			if (room == 2) {
				// falling wakes the player at the entrance and costs time
				int elapsed = instance.elapsed();
				server.runCommand("kill @p");
				context.waitTicks(5);
				String where = server.computeOnServer(s -> {
					ServerPlayer player = connection.getServerPlayer();
					require(player.isAlive() && DungeonDimension.is(player.level()), "falling does not kill in a dungeon");
					require(player.position().x - origin.getX() < 10, "the fallen player wakes at the entrance, is at " + player.position());
					return player.blockPosition().toShortString();
				});
				require(instance.elapsed() - elapsed >= Dungeons.DEATH_PENALTY_SECONDS * 20, "falling costs " + Dungeons.DEATH_PENALTY_SECONDS + " seconds");
				MinecraftMode.LOGGER.info("[content] fell, woke at {}", where);
			}
			server.runOnServer(s -> {
				ServerLevel level = DungeonDimension.level(s);
				for (UUID id : instance.roomMobs()) {
					if (level.getEntity(id) instanceof LivingEntity e) {
						e.kill(level);
					}
				}
			});
			waitFor(context, server, s -> instance.room() == hall + 1, 60, "clearing hall " + room + " (" + count + " monsters) opens the next door");
			require(server.computeOnServer(s -> DungeonDimension.level(s).getBlockState(origin.offset(DungeonLayout.doorX(hall), 0, 0)).isAir()), "door " + room + " is open");
		}
		DungeonLayout.Room boss = DungeonLayout.ROOMS.get(DungeonLayout.BOSS_ROOM);
		server.runCommand("execute in minecraft_mode:dungeon run tp @p " + (origin.getX() + boss.x0() + 3.5) + " " + origin.getY() + " " + (origin.getZ() + 0.5) + " -90 0");
		waitFor(context, server, s -> instance.bossId() != null, 60, "the boss room wakes the boss");
		require(server.computeOnServer(s -> DungeonDimension.level(s).getEntity(instance.bossId()) instanceof NamedMob mob && mob.isChampion()
			&& mob.def().id().equals(def.boss())), "the boss is a champion of " + def.boss());
		context.getInput().lookAt(new BlockPos(origin.getX() + boss.x1() - 6, origin.getY() + 2, origin.getZ()));
		context.waitTicks(30);
		shot(context, "content_dungeon_boss");
		requireBossBarsClear(context, "dungeon boss");
		int stones = server.computeOnServer(s -> JobProgression.count(connection.getServerPlayer().getInventory(), ModItems.ENHANCEMENT_STONE));
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = DungeonDimension.level(s);
			LivingEntity mob = (LivingEntity)level.getEntity(instance.bossId());
			mob.hurtServer(level, player.damageSources().playerAttack(player), 1.0E7F);
		});
		waitFor(context, server, s -> instance.state() == DungeonInstance.State.VICTORY, 40, "killing the boss wins the run");
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			DungeonData data = Dungeons.data(player);
			require(data.clears(def.id()) == 1 && data.best(def.id()) == 4, "the clear and the timed +4 are recorded, got " + data);
			require(Progress.get(player).has("dungeon_first"), "the first clear unlocks its achievement");
			require(JobProgression.count(player.getInventory(), ModItems.ENHANCEMENT_STONE) > stones, "the run rewards enhancement stones");
			Keystone next = null;
			for (ItemStack stack : player.getInventory()) {
				if (stack.has(ModDataComponents.KEYSTONE)) {
					next = stack.get(ModDataComponents.KEYSTONE);
				}
			}
			require(next != null && next.level() == 6 && !next.dungeon().equals(def.id()), "a fast run raises the keystone by 2 and moves it, got " + next);
			return "cleared in " + instance.elapsed() / 20 + " s, keystone now " + next;
		});
		MinecraftMode.LOGGER.info("[content] dungeon: {}", report);
		server.runOnServer(s -> Dungeons.leave(connection.getServerPlayer()));
		waitFor(context, server, s -> Dungeons.instances().isEmpty() && !DungeonDimension.is(connection.getServerPlayer().level()), 60, "leaving ends the run");
	}

	/** The warden takes the leader's keystone; giving the run up gives it back one level lower. */
	private static void checkWarden(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("tp @p 0.5 -60 0.5");
		context.waitTicks(5);
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = player.level();
			for (ItemStack stack : player.getInventory()) {
				if (stack.is(ModItems.DUNGEON_KEYSTONE)) {
					stack.setCount(0);
				}
			}
			CityNpc warden = ModEntities.CITY_NPC.create(level, EntitySpawnReason.COMMAND);
			warden.setRole(CityNpc.Role.DUNGEON_WARDEN);
			warden.snapTo(2.5, -60, 0.5, 90, 0);
			level.addFreshEntity(warden);
			DungeonDef def = Dungeons.def("bandit_hideout");
			require(!Dungeons.tryEnter(player, warden.getId(), def, true), "no keystone, no keystone run");
			player.getInventory().add(KeystoneItem.of(new Keystone(def.id(), 5)));
			require(Dungeons.keystone(player, def.id()).level() == 5, "the keystone is found in the inventory");
			require(Dungeons.tryEnter(player, warden.getId(), def, true), "the warden starts the keystone run");
			require(Dungeons.keystone(player, def.id()) == null, "the keystone is taken for the run");
			warden.discard();
			return Dungeons.instances().getFirst().level() + "";
		});
		require(report.equals("5"), "the run uses the keystone's level, got " + report);
		context.waitTicks(5);
		server.runOnServer(s -> Dungeons.leave(connection.getServerPlayer()));
		waitFor(context, server, s -> Dungeons.instances().isEmpty(), 80, "giving up closes the run");
		Keystone back = server.computeOnServer(s -> Dungeons.keystone(connection.getServerPlayer(), "bandit_hideout"));
		require(back != null && back.level() == 4, "an unfinished run gives the keystone back one lower, got " + back);
		MinecraftMode.LOGGER.info("[content] warden: keystone +5 given up -> {}", back);
	}

	// ------------------------------------------------------------------ world events

	private static void checkTitan(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("tp @p 400.5 -60 400.5");
		context.waitTicks(20);
		NamedMob titan = server.computeOnServer(s -> WorldEvents.spawnTitan(s.overworld(), connection.getServerPlayer(), NamedMobs.byId("goblin_warchief")));
		require(titan != null && titan.isChampion() && titan.getScale() > 2.0F, "a titan rises near the player");
		require(server.computeOnServer(s -> WorldEvents.spawnTitan(s.overworld(), connection.getServerPlayer(), null)) == null, "only one titan at a time");
		context.getInput().lookAt(titan.blockPosition().above(3));
		context.waitTicks(30);
		// the map marks it: as a boss the client sees, and through the server's report (which reaches the whole overworld)
		context.runOnClient(minecraft -> {
			MapSettings.minimap = true;
			Entity seen = minecraft.level.getEntity(titan.getId());
			require(seen != null && FieldBosses.all(minecraft.level).stream().anyMatch(m -> m.big() && m.x() == seen.getX() && m.z() == seen.getZ()),
				"the titan is a boss mark on the map");
			require(FieldBosses.reported().size() == 1, "the server reports the titan for the map, got " + FieldBosses.reported().size());
		});
		shot(context, "content_titan");
		requireBossBarsClear(context, "titan");
		int shards = server.computeOnServer(s -> JobProgression.count(connection.getServerPlayer().getInventory(), ModItems.TITAN_SHARD));
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = s.overworld();
			((LivingEntity)level.getEntity(titan.getUUID())).hurtServer(level, player.damageSources().playerAttack(player), 1.0E7F);
		});
		context.waitTicks(5);
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			require(WorldEvents.titan() == null, "the titan is gone");
			int now = JobProgression.count(player.getInventory(), ModItems.TITAN_SHARD);
			require(now >= shards + 2, "the top contributor gets at least two titan shards, got " + (now - shards));
			require(Story.data(player).worldBosses() == 1 && Progress.get(player).has("world_boss"), "the titan counts for the story and achievements");
			return (now - shards) + " shards";
		});
		MinecraftMode.LOGGER.info("[content] titan: {}", report);
	}

	// ------------------------------------------------------------------ story

	private static void checkStory(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = player.level();
			CityNpc herald = ModEntities.CITY_NPC.create(level, EntitySpawnReason.COMMAND);
			herald.setRole(CityNpc.Role.HERALD);
			herald.snapTo(player.getX() + 2, player.getY(), player.getZ(), 90, 0);
			level.addFreshEntity(herald);
			List<Story.Chapter> chapters = Story.chapters();
			int expected = 0;
			while (expected < chapters.size() && chapters.get(expected).done(player)) {
				expected++;
			}
			int wallet = Wallet.balance(player);
			int merit = Bounties.get(player).merit();
			int coins = 0;
			int chapterMerit = 0;
			for (int i = 0; i < expected; i++) {
				coins += Story.coins(i);
				chapterMerit += Story.merit(i);
			}
			for (int i = 0; i < chapters.size() + 1; i++) {
				Story.talk(player, herald);
			}
			require(Story.data(player).chapter() == expected, "the herald hands out every finished chapter in order: expected " + expected + ", got "
				+ Story.data(player).chapter());
			require(Wallet.balance(player) - wallet == coins, "each chapter pays its coins");
			require(Bounties.get(player).merit() - merit >= chapterMerit, "each chapter pays its merit");
			require(expected >= 2 && expected < chapters.size(), "a level 100 warrior without lair clears stops before the lair chapter, got " + expected);
			herald.discard();
			return "chapter " + expected + " of " + chapters.size() + " (" + chapters.get(expected).id() + ")";
		});
		MinecraftMode.LOGGER.info("[content] story: {}", report);
	}

	// ------------------------------------------------------------------ town crier and stylist

	/** The crier's notice board opens; the stylist's looks keep the stats, match slots and come back for free. */
	private static void checkCrierAndStylist(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			CityNpc odo = ModEntities.CITY_NPC.create(player.level(), EntitySpawnReason.COMMAND);
			odo.setRole(CityNpc.Role.CRIER);
			odo.snapTo(player.getX() + 1.5, player.getY(), player.getZ(), 90, 0);
			player.level().addFreshEntity(odo);
			player.interactOn(odo, InteractionHand.MAIN_HAND, odo.position().add(0, 1, 0));
			odo.discard();
		});
		context.waitForScreen(com.minecraftmode.client.guide.NoticeScreen.class);
		context.waitTicks(5);
		shot(context, "content_notice_board");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
		int[] celesteId = new int[1];
		String looks = server.computeOnServer(s -> {
			ItemStack sword = new ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
			sword.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Old Faithful"));
			ItemStack diamond = new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
			ItemStack helmet = new ItemStack(net.minecraft.world.item.Items.IRON_HELMET);
			ItemStack golden = new ItemStack(net.minecraft.world.item.Items.GOLDEN_HELMET);
			if (!com.minecraftmode.style.Looks.compatible(sword, diamond) || !com.minecraftmode.style.Looks.compatible(helmet, golden)) {
				return "a sword takes a sword's look and a helmet a helmet's";
			}
			if (com.minecraftmode.style.Looks.compatible(helmet, diamond) || com.minecraftmode.style.Looks.compatible(helmet, new ItemStack(net.minecraft.world.item.Items.IRON_BOOTS))) {
				return "a helmet cannot take a sword's or boots' look";
			}
			ItemStack styled = com.minecraftmode.style.Looks.apply(sword, diamond);
			if (!java.util.Objects.equals(styled.get(DataComponents.ITEM_MODEL), diamond.get(DataComponents.ITEM_MODEL)) || !styled.is(net.minecraft.world.item.Items.IRON_SWORD)
				|| !styled.getHoverName().getString().equals("Old Faithful")) {
				return "the sword should look like a diamond sword and stay an iron sword named Old Faithful";
			}
			ItemStack worn = com.minecraftmode.style.Looks.apply(helmet, golden);
			if (!worn.get(DataComponents.EQUIPPABLE).assetId().equals(golden.get(DataComponents.EQUIPPABLE).assetId())) {
				return "the helmet should be worn as a golden helmet";
			}
			ItemStack back = com.minecraftmode.style.Looks.restore(worn);
			if (!back.getComponentsPatch().isEmpty()) {
				return "restoring should leave a plain iron helmet, got " + back.getComponentsPatch();
			}
			ServerPlayer player = connection.getServerPlayer();
			CityNpc celeste = ModEntities.CITY_NPC.create(player.level(), EntitySpawnReason.COMMAND);
			celeste.setRole(CityNpc.Role.STYLIST);
			celeste.snapTo(player.getX() + 1.5, player.getY(), player.getZ(), 90, 0);
			player.level().addFreshEntity(celeste);
			celesteId[0] = celeste.getId();
			// she stays until the end: her table closes when she is gone
			player.interactOn(celeste, InteractionHand.MAIN_HAND, celeste.position().add(0, 1, 0));
			if (!(player.containerMenu instanceof com.minecraftmode.style.StylistMenu menu)) {
				return "Celeste should open her table";
			}
			menu.getSlot(0).set(sword.copy());
			menu.getSlot(1).set(diamond.copy());
			return "";
		});
		require(looks.isEmpty(), "looks: " + looks);
		context.waitForScreen(com.minecraftmode.client.style.StylistScreen.class);
		context.waitTicks(5);
		shot(context, "content_stylist");
		String applied = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			if (!(player.containerMenu instanceof com.minecraftmode.style.StylistMenu menu)) {
				return "Celeste's table should still be open";
			}
			int wallet = Wallet.balance(player);
			int price = com.minecraftmode.style.Looks.price(menu.target());
			if (!menu.clickMenuButton(player, com.minecraftmode.style.StylistMenu.BUTTON_APPLY)) {
				return "restyling should work";
			}
			if (!menu.donor().isEmpty() || !com.minecraftmode.style.Looks.styled(menu.target()) || wallet - Wallet.balance(player) != (player.isCreative() ? 0 : price)) {
				return "the donor is used up and the price paid";
			}
			menu.clickMenuButton(player, com.minecraftmode.style.StylistMenu.BUTTON_RESTORE);
			if (com.minecraftmode.style.Looks.styled(menu.target())) {
				return "the original look comes back";
			}
			player.closeContainer();
			if (player.level().getEntity(celesteId[0]) != null) {
				player.level().getEntity(celesteId[0]).discard();
			}
			return "";
		});
		require(applied.isEmpty(), "stylist: " + applied);
		// the server's close packet arrives a moment later and would close whatever screen opens next
		context.waitFor(minecraft -> minecraft.gui.screen() == null);
		context.waitTicks(2);
	}

	// ------------------------------------------------------------------ guide and quartermaster

	/** Right-clicking the quartermaster gives the plain iron kit once; right-clicking the guide opens her topics. */
	private static void checkPlazaNpcs(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		int[] bakerId = new int[1];
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = player.level();
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			CityNpc bram = ModEntities.CITY_NPC.create(level, EntitySpawnReason.COMMAND);
			bram.setRole(CityNpc.Role.QUARTERMASTER);
			bram.snapTo(player.getX() + 1.5, player.getY(), player.getZ(), 90, 0);
			level.addFreshEntity(bram);
			require(!StarterKit.taken(player), "a new player has not taken the kit");
			Map<Item, Integer> before = new HashMap<>();
			for (Item item : StarterKit.ITEMS) {
				before.put(item, JobProgression.count(player.getInventory(), item));
			}
			player.interactOn(bram, InteractionHand.MAIN_HAND, bram.position().add(0, 1, 0));
			for (Item item : StarterKit.ITEMS) {
				require(JobProgression.count(player.getInventory(), item) == before.get(item) + 1, "the kit should contain one " + item);
			}
			for (ItemStack stack : player.getInventory()) {
				if (StarterKit.ITEMS.contains(stack.getItem())) {
					require(!stack.isEnchanted(), "kit items come without enchantments: " + stack);
					require(stack.has(DataComponents.UNBREAKABLE), "kit items never wear out: " + stack);
					require(stack.getHoverName().getString().startsWith("Quartermaster's"), "kit items carry the quartermaster name: " + stack.getHoverName().getString());
				}
			}
			require(StarterKit.taken(player), "the kit is remembered");
			player.interactOn(bram, InteractionHand.MAIN_HAND, bram.position().add(0, 1, 0));
			require(JobProgression.count(player.getInventory(), net.minecraft.world.item.Items.IRON_SWORD) == before.get(net.minecraft.world.item.Items.IRON_SWORD) + 1,
				"the kit is given only once");
			// the shield joined the kit later: someone who took the kit before collects only the shield on the next visit
			require(StarterKit.shieldTaken(player), "a new kit includes the shield");
			player.setAttached(ModAttachments.STARTER_SHIELD, false);
			int shields = JobProgression.count(player.getInventory(), net.minecraft.world.item.Items.SHIELD);
			int swords = JobProgression.count(player.getInventory(), net.minecraft.world.item.Items.IRON_SWORD);
			player.interactOn(bram, InteractionHand.MAIN_HAND, bram.position().add(0, 1, 0));
			require(JobProgression.count(player.getInventory(), net.minecraft.world.item.Items.SHIELD) == shields + 1
				&& JobProgression.count(player.getInventory(), net.minecraft.world.item.Items.IRON_SWORD) == swords, "an earlier kit owner gets only the shield");
			player.interactOn(bram, InteractionHand.MAIN_HAND, bram.position().add(0, 1, 0));
			require(JobProgression.count(player.getInventory(), net.minecraft.world.item.Items.SHIELD) == shields + 1, "the shield is given once too");
			bram.discard();
			// Baker Hanna: a stack of bread once per Minecraft day
			CityNpc hanna = ModEntities.CITY_NPC.create(level, EntitySpawnReason.COMMAND);
			hanna.setRole(CityNpc.Role.BAKER);
			hanna.snapTo(player.getX() + 1.5, player.getY(), player.getZ(), 90, 0);
			level.addFreshEntity(hanna);
			bakerId[0] = hanna.getId();
			int bread = bread(player);
			player.interactOn(hanna, InteractionHand.MAIN_HAND, hanna.position().add(0, 1, 0));
			require(bread(player) == bread + DailyBread.COUNT && DailyBread.takenToday(player), "Hanna hands out " + DailyBread.COUNT + " bread, got " + (bread(player) - bread));
			player.interactOn(hanna, InteractionHand.MAIN_HAND, hanna.position().add(0, 1, 0));
			require(bread(player) == bread + DailyBread.COUNT, "the bread is once a day");
			CityNpc nella = ModEntities.CITY_NPC.create(level, EntitySpawnReason.COMMAND);
			nella.setRole(CityNpc.Role.GUIDE);
			nella.snapTo(player.getX() + 1.5, player.getY(), player.getZ(), 90, 0);
			level.addFreshEntity(nella);
			player.interactOn(nella, InteractionHand.MAIN_HAND, nella.position().add(0, 1, 0));
			nella.discard();
			return StarterKit.ITEMS.size() + " items";
		});
		context.waitForScreen(GuideScreen.class);
		context.waitTicks(5);
		shot(context, "content_guide");
		requireFits(context, "guide", 380);
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
		GuideScreen.showTopic(GuideScreen.Topic.LEVELING);
		context.runOnClient(minecraft -> minecraft.gui.setScreen(new GuideScreen()));
		context.waitForScreen(GuideScreen.class);
		context.waitTicks(5);
		shot(context, "content_guide_leveling");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
		GuideScreen.showTopic(GuideScreen.Topic.PLACES);
		context.runOnClient(minecraft -> minecraft.gui.setScreen(new GuideScreen()));
		context.waitForScreen(GuideScreen.class);
		context.waitTicks(5);
		shot(context, "content_guide_places");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
		// the longest answer scrolls instead of running over the buttons
		GuideScreen.showTopic(GuideScreen.Topic.TOWN);
		context.runOnClient(minecraft -> minecraft.gui.setScreen(new GuideScreen()));
		context.waitForScreen(GuideScreen.class);
		context.waitTicks(5);
		shot(context, "content_guide_town");
		GuideScreen.showTopic(GuideScreen.Topic.START);
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
		// a lone % in an answer without arguments breaks the whole format and shows the raw text, %% included (until 0.5.17 the raids topic did)
		String rawTexts = context.computeOnClient(minecraft -> {
			List<String> keys = new ArrayList<>();
			for (GuideScreen.Topic topic : GuideScreen.Topic.values()) {
				if (topic != GuideScreen.Topic.LEVELING && topic != GuideScreen.Topic.KEYS && topic != GuideScreen.Topic.PLACES) {
					keys.add(topic.key() + ".text");
				}
			}
			for (int i = 1; i <= CityServices.GUIDE_PAGES; i++) {
				keys.add("book.minecraft_mode.guide.page" + i);
			}
			List<String> broken = new ArrayList<>();
			for (String key : keys) {
				String raw = net.minecraft.locale.Language.getInstance().getOrDefault(key);
				if (raw.indexOf('%') >= 0 && Component.translatable(key).getString().equals(raw)) {
					broken.add(key);
				}
			}
			return String.join(", ", broken);
		});
		require(rawTexts.isEmpty(), "guide texts that fail to format: " + rawTexts);
		// Nella replaces old handbooks with the current edition
		String handbook = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.getInventory().add(CityServices.guideBook());
			CityServices.reissueGuideBook(player);
			int copies = 0;
			int pages = 0;
			for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
				ItemStack stack = player.getInventory().getItem(i);
				if (CityServices.isGuideBook(stack)) {
					copies++;
					pages = stack.get(DataComponents.WRITTEN_BOOK_CONTENT).pages().size();
				}
			}
			return copies == 1 && pages == CityServices.GUIDE_PAGES ? "" : copies + " handbooks, " + pages + " pages";
		});
		require(handbook.isEmpty(), "reissued handbook: " + handbook);
		// every townsperson has a skin (Garrick shipped without one and rendered as the missing texture)
		String skins = context.computeOnClient(minecraft -> {
			List<String> missing = new ArrayList<>();
			for (CityNpc.Role role : CityNpc.Role.values()) {
				if (minecraft.getResourceManager().getResource(MinecraftMode.id("textures/entity/npc/" + role.id() + ".png")).isEmpty()) {
					missing.add(role.id());
				}
			}
			return String.join(", ", missing);
		});
		require(skins.isEmpty(), "city NPCs without a skin: " + skins);
		// the next day there is bread again
		server.runCommand("time add " + ResetCycle.DAY_TICKS);
		int fresh = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			Entity hanna = player.level().getEntity(bakerId[0]);
			require(hanna != null && !DailyBread.takenToday(player), "a new day resets the bread");
			int bread = bread(player);
			player.interactOn(hanna, InteractionHand.MAIN_HAND, hanna.position().add(0, 1, 0));
			hanna.discard();
			return bread(player) - bread;
		});
		require(fresh == DailyBread.COUNT, "Hanna should hand out bread again the next day, got " + fresh);
		MinecraftMode.LOGGER.info("[content] plaza npcs: starter kit {} with the shield, daily bread, guide screen opened", report);
	}

	/** Bread in the inventory plus loaves dropped next to the player (a full inventory drops the rest). */
	private static int bread(final ServerPlayer player) {
		int dropped = 0;
		for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class,
			player.getBoundingBox().inflate(4.0), e -> e.getItem().is(net.minecraft.world.item.Items.BREAD))) {
			dropped += item.getItem().getCount();
		}
		// the bread goes into the supply bag when the player carries one
		return com.minecraftmode.bag.Bags.count(player, net.minecraft.world.item.Items.BREAD) + dropped;
	}

	// ------------------------------------------------------------------ screens

	private static void screens(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		context.runOnClient(minecraft -> minecraft.gui.toastManager().clear());
		// the collection
		context.runOnClient(minecraft -> minecraft.gui.setScreen(new CompanionScreen()));
		context.waitForScreen(CompanionScreen.class);
		context.waitTicks(5);
		shot(context, "content_collection");
		requireFits(context, "collection", 340);
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
		// the dungeon warden
		server.runOnServer(s -> ServerPlayNetworking.send(connection.getServerPlayer(), new OpenDungeonPayload(-1, ResetCycle.cycle(s.overworld()))));
		context.waitForScreen(DungeonScreen.class);
		context.waitTicks(5);
		shot(context, "content_dungeons");
		requireFits(context, "dungeons", 340);
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
		// a station
		server.runOnServer(s -> connection.getServerPlayer().openMenu(new SimpleMenuProvider((id, inventory, p) -> new CraftMenu(CraftMenu.type(Profession.ALCHEMY), id,
			inventory, Profession.ALCHEMY, null), Component.translatable("container.minecraft_mode.station.alchemy"))));
		context.waitForScreen(CraftScreen.class);
		context.waitTicks(5);
		shot(context, "content_alchemy");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
		// the awakening bench with the awakened weapon
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new EnhanceMenu(id, inventory, null), Component.translatable("container.minecraft_mode.enhance")));
		});
		context.waitForScreen(EnhanceScreen.class);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			if (player.containerMenu instanceof EnhanceMenu menu) {
				for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
					ItemStack stack = player.getInventory().getItem(i);
					if (Enhancement.of(stack).awaken() > 0) {
						menu.slots.get(0).set(stack.copy());
						player.getInventory().setItem(i, ItemStack.EMPTY);
						break;
					}
				}
				menu.broadcastChanges();
			}
		});
		context.waitTicks(10);
		shot(context, "content_awaken");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
		// paragon and the story journal
		TalentScreen.showParagon(true);
		context.runOnClient(minecraft -> minecraft.gui.setScreen(new TalentScreen()));
		context.waitForScreen(TalentScreen.class);
		context.waitTicks(5);
		shot(context, "content_paragon");
		requireFits(context, "paragon", 320);
		TalentScreen.showParagon(false);
		CodexScreen.showTab("story");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(new CodexScreen()));
		context.waitForScreen(CodexScreen.class);
		context.waitTicks(5);
		shot(context, "content_story");
		requireFits(context, "story", 320);
		// the chapter list scrolls instead of running past the footer
		CodexScreen.scrollToEnd();
		context.waitTicks(2);
		shot(context, "content_story_end");
		CodexScreen.showTab("codex");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
	}

	// ------------------------------------------------------------------ map, minimap and held light

	private static void checkMapAndLight(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		// a torch in the off hand lights the block at the player's head; putting it away removes the light again
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(net.minecraft.world.item.Items.TORCH));
		});
		context.waitTicks(10);
		String light = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			BlockPos head = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
			int level = player.level().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, head);
			require(level >= 13, "a held torch lights the player's surroundings, block light at the head is " + level);
			require(player.level().getBlockState(head).is(net.minecraft.world.level.block.Blocks.LIGHT), "the light is an invisible light block");
			player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
			return "held torch light " + level;
		});
		context.waitTicks(10);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			BlockPos head = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
			require(!player.level().getBlockState(head).is(net.minecraft.world.level.block.Blocks.LIGHT), "the light block goes away with the torch");
		});
		// the minimap has scanned the chunks around the player; the big map opens, shows a waypoint and fits any screen
		context.waitTicks(40);
		context.runOnClient(minecraft -> {
			require(MapClient.data().size() > 0, "the map scanner has coloured chunks around the player");
			MapSettings.minimap = true;
			MapClient.waypoints().add(MapClient.dimensionKey(), new Waypoints.Waypoint("Test camp", 12, -60, -8, Waypoints.COLORS[1]));
		});
		context.waitTicks(15);
		shot(context, "content_minimap");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(new MapScreen()));
		context.waitForScreen(MapScreen.class);
		context.waitTicks(25);
		shot(context, "content_map");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(new WaypointScreen((MapScreen)minecraft.gui.screen(), null, 30, -60, 30)));
		context.waitForScreen(WaypointScreen.class);
		context.waitTicks(5);
		shot(context, "content_map_waypoint");
		context.runOnClient(minecraft -> {
			List<Waypoints.Waypoint> list = MapClient.currentWaypoints();
			list.clear();
			MapClient.waypoints().touch();
			minecraft.gui.setScreen(null);
		});
		MinecraftMode.LOGGER.info("[content] {}, map chunks {}", light, context.computeOnClient(minecraft -> MapClient.data().size()));
	}

	// ------------------------------------------------------------------ helpers

	private static WeaponDef activeWeapon(final JobData data) {
		WeaponDef best = null;
		for (WeaponDef def : JobWeapons.of(JobClass.WARRIOR)) {
			if (JobWeapons.isActive(data, def) && (best == null || def.level() > best.level())) {
				best = def;
			}
		}
		require(best != null, "no usable warrior weapon at level " + data.level());
		return best;
	}

	/** A screenshot without toasts, chat lines or titles over the view. */
	private static void shot(final ClientGameTestContext context, final String name) {
		context.runOnClient(minecraft -> {
			minecraft.gui.toastManager().clear();
			minecraft.gui.hud.getChat().clearMessages(false);
			minecraft.gui.hud.clearTitles();
		});
		context.waitTicks(2);
		context.takeScreenshot(name);
	}

	/** The boss bars on screen stay clear of the class panel and the effect icons (854x480 window). */
	private static void requireBossBarsClear(final ClientGameTestContext context, final String what) {
		String report = context.computeOnClient(minecraft -> BossBarLayout.overlap());
		require(report.isEmpty(), what + ": " + report);
	}

	private static void requireFits(final ClientGameTestContext context, final String what, final int width) {
		String report = context.computeOnClient(minecraft -> {
			net.minecraft.client.gui.screens.Screen screen = minecraft.gui.screen();
			if (screen == null) {
				return what + " is not open";
			}
			int lo = (screen.width - width) / 2;
			int hi = lo + width;
			for (var child : screen.children()) {
				if (child instanceof net.minecraft.client.gui.components.AbstractWidget w && (w.getX() < lo || w.getRight() > hi)) {
					return what + ": " + w.getMessage().getString() + " at " + w.getX() + ".." + w.getRight() + " is outside " + lo + ".." + hi;
				}
			}
			return "";
		});
		require(report.isEmpty(), report);
	}

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

	private static void require(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
