package com.minecraftmode.test;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.companion.CompanionScreen;
import com.minecraftmode.client.craft.CraftScreen;
import com.minecraftmode.client.dungeon.DungeonScreen;
import com.minecraftmode.client.endgame.CodexScreen;
import com.minecraftmode.client.endgame.EnhanceScreen;
import com.minecraftmode.client.endgame.TalentScreen;
import com.minecraftmode.companion.Companions;
import com.minecraftmode.companion.MountEntity;
import com.minecraftmode.companion.PetEntity;
import com.minecraftmode.craft.CraftMenu;
import com.minecraftmode.craft.CraftRecipes;
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
import com.minecraftmode.economy.Wallet;
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
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.GearStats;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
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
			checkCompanions(context, server, connection);
			checkDungeon(context, server, connection);
			checkWarden(context, server, connection);
			checkTitan(context, server, connection);
			checkStory(context, server, connection);
			screens(context, server, connection);
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
			require(Paragon.get(player).rank(Paragon.Stat.MIGHT) == 1 && Math.abs(after - before - 0.3F) < 0.001F, "Might adds 0.3% basic damage, got " + (after - before));
			require(Paragon.reset(player) && Paragon.get(player).available() == 2, "a reset refunds every point");
			Paragon.spend(player, Paragon.Stat.MIGHT.id);
			return "paragon " + data.level() + ", might +" + (after - before);
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
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			String name = awakened.getHoverName().getString();
			// kept in the backpack (the hand is used for charms and whistles later) for the awakening screenshot
			player.getInventory().setItem(30, awakened);
			return name;
		});
		MinecraftMode.LOGGER.info("[content] awakened: {}", report);
	}

	// ------------------------------------------------------------------ professions

	private static void checkProfessions(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = player.level();
			List<String> lines = new ArrayList<>();
			for (Profession profession : Profession.values()) {
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
			// a mount whistle: ride it, the flyer keeps the rider, the key gets off and sends it away
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Companions.mountItem("storm_griffin")));
			player.getMainHandItem().use(level, player, InteractionHand.MAIN_HAND);
			require(player.getVehicle() instanceof MountEntity mount && mount.def() != null && mount.def().flying(), "the whistle puts the player on the griffin");
			MountEntity mount = (MountEntity)player.getVehicle();
			require(mount.getControllingPassenger() == player, "the owner steers the mount");
			Companions.toggleMount(player, "");
			require(player.getVehicle() == null && mount.isRemoved(), "the mount key gets off and the mount leaves");
			Companions.summonPet(player, "");
			require(Companions.summonedPet(player) == null && Companions.lines(player).isEmpty(), "dismissing the pet removes its bonus");
			// the whole collection for the screen and the lineup
			Map<String, Integer> pets = new HashMap<>();
			Companions.pets().forEach(d -> pets.put(d.id(), d.id().equals("ember_fox") ? Companions.petExpFor(4) : 0));
			player.setAttached(ModAttachments.COMPANIONS, new Companions.Data(pets, Companions.mounts().stream().map(Companions.MountDef::id).toList(), "", "storm_griffin"));
			Progress.changed(player);
			require(Progress.get(player).has("pets_all") && Progress.get(player).has("mounts_all"), "full collections count toward achievements");
			Companions.summonPet(player, "ember_fox");
			return "pet level " + Companions.petLevel(Companions.data(player).pets().get("ember_fox"));
		});
		MinecraftMode.LOGGER.info("[content] companions: {}", report);

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
				level.addFreshEntity(pet);
				i++;
			}
			i = 0;
			for (Companions.MountDef def : Companions.mounts()) {
				MountEntity mount = Companions.mountType(def).create(level, EntitySpawnReason.COMMAND);
				mount.setup(player);
				mount.setNoAi(true);
				mount.snapTo(20.5 - 9.0 + i * 3.6, -60, 11.0, 180.0F, 0.0F);
				level.addFreshEntity(mount);
				i++;
			}
		});
		context.getInput().lookAt(new BlockPos(20, -59, 8));
		context.waitTicks(25);
		shot(context, "content_companions");
		server.runOnServer(s -> {
			for (Entity e : connection.getServerPlayer().level().getEntitiesOfClass(Entity.class, connection.getServerPlayer().getBoundingBox().inflate(30),
				e -> e instanceof PetEntity || e instanceof MountEntity)) {
				e.discard();
			}
			Companions.summonPet(connection.getServerPlayer(), "ember_fox");
		});
	}

	// ------------------------------------------------------------------ dungeons

	private static void checkDungeon(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		DungeonDef def = Dungeons.def("fungal_depths");
		server.runOnServer(s -> Dungeons.start(s, List.of(connection.getServerPlayer()), def, connection.getServerPlayer().getUUID(), 4,
			connection.getServerPlayer().getUUID()));
		DungeonInstance instance = server.computeOnServer(s -> Dungeons.instances().getFirst());
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
				context.getInput().lookAt(new BlockPos(origin.getX() + r.centerX() + 3, origin.getY() + 1, origin.getZ()));
				context.waitTicks(20);
				shot(context, "content_dungeon_hall");
			}
			if (room == 2) {
				// falling wakes the player at the entrance and costs time
				int elapsed = instance.elapsed();
				// the first lethal blow is turned aside by the warrior's Avalon; the second (after the hurt cooldown) is a real fall
				server.runCommand("kill @p");
				context.waitTicks(25);
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
		shot(context, "content_titan");
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
			int coins = 0;
			for (int i = 0; i < expected; i++) {
				coins += Story.coins(i);
			}
			for (int i = 0; i < chapters.size() + 1; i++) {
				Story.talk(player, herald);
			}
			require(Story.data(player).chapter() == expected, "the herald hands out every finished chapter in order: expected " + expected + ", got "
				+ Story.data(player).chapter());
			require(Wallet.balance(player) - wallet == coins, "each chapter pays its coins");
			require(expected >= 2 && expected < chapters.size(), "a level 100 warrior without lair clears stops before the lair chapter, got " + expected);
			herald.discard();
			return "chapter " + expected + " of " + chapters.size() + " (" + chapters.get(expected).id() + ")";
		});
		MinecraftMode.LOGGER.info("[content] story: {}", report);
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
		CodexScreen.showTab("codex");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
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
