package com.minecraftmode.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minecraftmode.MinecraftMode;
import com.minecraftmode.command.WalletCommand;
import com.minecraftmode.consumable.BuffEffects;
import com.minecraftmode.consumable.ConsumableDef;
import com.minecraftmode.consumable.Consumables;
import com.minecraftmode.economy.ShopMerchant;
import com.minecraftmode.economy.ShopType;
import com.minecraftmode.economy.Wallet;
import com.minecraftmode.entity.boss.RaidBoss;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.gear.GearStats;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.raid.RaidDimension;
import com.minecraftmode.raid.RaidInstance;
import com.minecraftmode.raid.Raids;
import com.minecraftmode.registry.ModEffects;
import com.minecraftmode.registry.ModItems;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

/**
 * The wallet (coins leave the inventory, shops pay from it), keep-inventory, the 40 consumables
 * (assets, effects, buffs as stats, the Phoenix Feather, the return scroll), raid fees, and the raid
 * mechanics: every mechanic runs without errors, standing in a safe zone survives, standing outside
 * dies (and is carried home), and a solo player survives the spread mechanic.
 */
public class EconomyClientGameTest implements FabricClientGameTest {
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

			checkWallet(context, server, connection);
			checkShop(context, server, connection);
			checkConsumables(context, server, connection);
			checkReturnScroll(context, server, connection);
			checkMechanicSmoke(context, server, connection);
			checkMechanicsInRaid(context, server, connection);
		}
	}

	private static void checkContent(final ClientGameTestContext context) {
		require(Consumables.all().size() == 40, "expected 40 consumables, got " + Consumables.all().size());
		int[] perTier = new int[6];
		for (ConsumableDef def : Consumables.all()) {
			perTier[def.tier()]++;
			for (ConsumableDef.Buff buff : def.effects()) {
				require(buff.ticks() == Consumables.LONG, def.id() + ": lasting effects run 10 minutes");
			}
			require(def.tier() < 5 || def.shop() == ConsumableDef.Shop.NONE, def.id() + ": tier 5 is raid-only");
		}
		require(perTier[1] > 0 && perTier[2] > 0 && perTier[3] > 0 && perTier[4] > 0 && perTier[5] > 0, "every tier needs consumables");
		require(Consumables.soldAt(ConsumableDef.Shop.ALCHEMIST).size() >= 10, "the alchemist should sell at least 10 consumables");
		require(WalletCommand.parse("2g 5s 3") == 2 * 81 + 45 + 3 && WalletCommand.parse("40s") == 360 && WalletCommand.parse("x") == -1, "wallet amounts");
		int lastFee = 0;
		for (BossDef def : RaidBosses.all()) {
			require(def.fee() > lastFee, def.id() + " fee should be above the previous boss");
			lastFee = def.fee();
		}
		require(RaidBosses.ARACHNE.fee() >= 20 && RaidBosses.ARACHNE.fee() <= 40, "Arachne costs about 30 copper, got " + RaidBosses.ARACHNE.fee());
		context.runOnClient(minecraft -> {
			var resources = minecraft.getResourceManager();
			JsonObject ko = lang(minecraft, "ko_kr");
			List<String> missing = new ArrayList<>();
			for (ConsumableDef def : Consumables.all()) {
				if (resources.getResource(MinecraftMode.id("textures/item/" + def.id() + ".png")).isEmpty()
					|| resources.getResource(MinecraftMode.id("items/" + def.id() + ".json")).isEmpty()) {
					missing.add("asset " + def.id());
				}
				if (!ko.has(def.nameKey()) || !ko.has(def.flavorKey())) {
					missing.add("lang " + def.id());
				}
			}
			for (var effect : BuffEffects.all()) {
				String id = BuiltInRegistries.MOB_EFFECT.getKey(effect.value()).getPath();
				if (resources.getResource(MinecraftMode.id("textures/mob_effect/" + id + ".png")).isEmpty() || !ko.has(effect.value().getDescriptionId())) {
					missing.add("effect " + id);
				}
			}
			for (String key : List.of("raid.minecraft_mode.mechanic.arachne_venom_deluge", "raid.minecraft_mode.mechanic.aethryx_judgment.hint",
				"death.attack.minecraft_mode.raid_mechanic", "screen.minecraft_mode.wallet", "block.minecraft_mode.alchemist_shop", "item.minecraft_mode.return_scroll")) {
				if (!ko.has(key)) {
					missing.add("lang " + key);
				}
			}
			require(missing.isEmpty(), "missing: " + missing);
		});
	}

	// ------------------------------------------------------------------ wallet and shops

	private static void checkWallet(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.getInventory().clearContent();
			player.getInventory().add(new ItemStack(ModItems.GOLD_COIN, 2));
			player.getInventory().add(new ItemStack(ModItems.SILVER_COIN, 3));
			player.getInventory().add(new ItemStack(ModItems.COPPER_COIN, 4));
		});
		context.waitTicks(25);
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			if (player.getInventory().countItem(ModItems.GOLD_COIN) + player.getInventory().countItem(ModItems.SILVER_COIN)
				+ player.getInventory().countItem(ModItems.COPPER_COIN) > 0) {
				return "coins should leave the inventory";
			}
			if (Wallet.balance(player) != 2 * 81 + 27 + 4) {
				return "wallet should hold 193 copper, got " + Wallet.balance(player);
			}
			if (!player.level().getGameRules().get(GameRules.KEEP_INVENTORY)) {
				return "keepInventory should be on";
			}
			return "";
		});
		require(report.isEmpty(), report);
		int synced = context.computeOnClient(minecraft -> minecraft.player == null ? -1 : Wallet.balance(minecraft.player));
		require(synced == 193, "the client should see the wallet, got " + synced);
	}

	private static void checkShop(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("setblock 0 -60 -3 minecraft_mode:alchemist_shop");
		context.waitTicks(2);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			Wallet.add(player, 500);
			new ShopMerchant(player, player.level(), new BlockPos(0, -60, -3), ShopType.ALCHEMIST)
				.openTradingScreen(player, Component.translatable(ShopType.ALCHEMIST.titleKey()), 1);
		});
		context.waitForScreen(MerchantScreen.class);
		context.waitTicks(10);
		context.takeScreenshot("alchemist_shop");
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			MerchantMenu menu = (MerchantMenu)player.containerMenu;
			int index = -1;
			for (int i = 0; i < menu.getOffers().size(); i++) {
				if (menu.getOffers().get(i).getResult().is(Consumables.item("greater_healing_draught"))) {
					index = i;
				}
			}
			if (index < 0) {
				return "the alchemist should sell greater healing draughts";
			}
			int before = Wallet.balance(player);
			menu.setSelectionHint(index);
			menu.tryMoveItems(index);
			menu.quickMoveStack(player, 2);
			player.closeContainer();
			Wallet.deposit(player);
			int paid = before - Wallet.balance(player);
			if (player.getInventory().countItem(Consumables.item("greater_healing_draught")) < 1) {
				return "the draught should be bought with wallet coins";
			}
			int price = menu.getOffers().get(index).getCostA().getCount() * Wallet.value(menu.getOffers().get(index).getCostA().getItem());
			return paid > 0 && paid % 9 == 0 ? "" : "wallet should pay whole silver, paid " + paid + " (price " + price + ")";
		});
		require(report.isEmpty(), report);
		server.runCommand("setblock 0 -60 -3 minecraft:air");
		context.waitTicks(2);
	}

	// ------------------------------------------------------------------ consumables

	private static void checkConsumables(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.setHealth(2.0F);
			Consumables.apply(player, Consumables.def("senzu_bean"));
			if (player.getHealth() < player.getMaxHealth() * 0.8F - 0.01F) {
				return "a Senzu Bean should restore 80% health, health " + player.getHealth();
			}
			Consumables.apply(player, Consumables.def("thunderbolt"));
			MobEffectInstance fury = player.getEffect(ModEffects.FURY);
			if (fury == null || fury.getAmplifier() != 1 || fury.getDuration() < Consumables.LONG - 5) {
				return "Thunderbolt should give Fury II for 10 minutes";
			}
			GearStats.invalidate(player);
			if (GearStats.of(player).get(EngraveStat.BASIC_DAMAGE) < 10.0F) {
				return "Fury II should add 10% basic damage, got " + GearStats.of(player).get(EngraveStat.BASIC_DAMAGE);
			}
			player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
			Consumables.apply(player, Consumables.def("antidote"));
			if (player.hasEffect(MobEffects.POISON)) {
				return "the antidote should cure poison";
			}
			JobData before = JobProgression.get(player);
			Consumables.apply(player, Consumables.def("rare_candy"));
			JobData after = JobProgression.get(player);
			if (after.level() == before.level() && after.exp() <= before.exp()) {
				return "a Rare Candy should give class experience";
			}
			// the Phoenix Feather turns a death into a revival
			player.getInventory().add(new ItemStack(Consumables.item("phoenix_feather")));
			player.hurtServer(player.level(), player.damageSources().magic(), 1000.0F);
			if (!player.isAlive() || player.getHealth() < player.getMaxHealth() * 0.45F) {
				return "the feather should revive at half health";
			}
			if (player.getInventory().countItem(Consumables.item("phoenix_feather")) != 0) {
				return "the feather should be used up";
			}
			return "";
		});
		require(report.isEmpty(), report);
		server.runCommand("effect clear @p");
	}

	private static void checkReturnScroll(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		BlockPos spawn = server.computeOnServer(s -> s.overworld().getRespawnData().pos());
		server.runCommand("tp @p 40.5 -60 40.5");
		context.waitTicks(5);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RETURN_SCROLL));
			player.getMainHandItem().use(player.level(), player, InteractionHand.MAIN_HAND);
		});
		context.waitTicks(115);
		double distance = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			return Math.hypot(player.getX() - spawn.getX() - 0.5, player.getZ() - spawn.getZ() - 0.5);
		});
		require(distance < 3.0, "the return scroll should bring the player to the spawn, " + distance + " blocks away");
		boolean used = server.computeOnServer(s -> connection.getServerPlayer().getMainHandItem().isEmpty());
		require(used, "the scroll should be used up");
	}

	// ------------------------------------------------------------------ raid mechanics

	/** Every mechanic of every boss runs once (creative: nobody is a target), so none of them throws. */
	private static void checkMechanicSmoke(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("gamemode creative @p");
		server.runCommand("tp @p 0.5 -60 0.5");
		server.runCommand("tick rate 100");
		int total = 0;
		for (BossDef def : RaidBosses.all()) {
			List<String> ids = server.computeOnServer(s -> {
				ServerPlayer player = connection.getServerPlayer();
				RaidBoss boss = RaidBosses.type(def).create(player.level(), EntitySpawnReason.COMMAND);
				boss.snapTo(0.5, -60, -10.5, 0.0F, 0.0F);
				boss.configure(1, null);
				player.level().addFreshEntity(boss);
				return boss.mechanicIds();
			});
			require(!ids.isEmpty() && ids.size() <= 3, def.id() + " should have 1-3 mechanics, got " + ids);
			total += ids.size();
			for (String id : ids) {
				boolean ran = server.computeOnServer(s -> {
					ServerPlayer player = connection.getServerPlayer();
					RaidBoss boss = player.level().getEntitiesOfClass(RaidBoss.class, player.getBoundingBox().inflate(40), b -> b.isAlive() && b.def() == def).getFirst();
					return boss.runMechanic(id, player);
				});
				require(ran, def.id() + " could not run " + id);
				waitFor(context, server, s -> {
					ServerPlayer player = connection.getServerPlayer();
					return player.level().getEntitiesOfClass(RaidBoss.class, player.getBoundingBox().inflate(40), b -> b.isAlive() && b.def() == def).stream()
						.noneMatch(RaidBoss::inMechanic);
				}, 600, def.id() + " " + id + " should finish");
			}
			server.runCommand("kill @e[type=!player]");
			context.waitTicks(10);
		}
		require(total == 13, "expected 13 mechanics, got " + total);
		server.runCommand("tick rate 20");
		server.runCommand("gamemode survival @p");
	}

	/** In a real raid: a safe spot survives, the wrong spot dies (and goes home), and a solo player shrugs off the spread mechanic. */
	private static void checkMechanicsInRaid(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("effect give @p resistance 999999 4 true");
		server.runCommand("tick rate 60");
		enterRaid(context, server, connection, RaidBosses.ARACHNE);
		// inside a cocoon: alive and still in the raid
		Vec3 safe = server.computeOnServer(s -> {
			RaidBoss boss = boss(connection);
			boss.runMechanic("arachne_venom_deluge", connection.getServerPlayer());
			return boss.mechanicSpots().getFirst();
		});
		server.runOnServer(s -> connection.getServerPlayer().teleportTo(safe.x, safe.y, safe.z));
		context.waitTicks(10);
		waitFor(context, server, s -> !boss(connection).inMechanic(), 400, "the venom deluge should end");
		String inside = server.computeOnServer(s -> RaidDimension.is(connection.getServerPlayer().level()) ? "" : "standing in a cocoon should survive the deluge");
		require(inside.isEmpty(), inside);
		context.takeScreenshot("raid_mechanic_safe");
		// outside: the deluge kills, and dying in a raid means going home with everything
		Vec3 danger = server.computeOnServer(s -> {
			RaidBoss boss = boss(connection);
			boss.runMechanic("arachne_venom_deluge", connection.getServerPlayer());
			Vec3 c = Vec3.atBottomCenterOf(boss.home());
			for (int r = 3; r < 18; r++) {
				Vec3 p = c.add(r, 0, 0);
				if (boss.mechanicSpots().stream().allMatch(s2 -> s2.subtract(p).horizontalDistance() > 5)) {
					return p;
				}
			}
			return c;
		});
		server.runOnServer(s -> connection.getServerPlayer().teleportTo(danger.x, danger.y, danger.z));
		waitFor(context, server, s -> !RaidDimension.is(connection.getServerPlayer().level()), 400, "outside the cocoons the deluge should carry the player home");
		boolean alive = server.computeOnServer(s -> connection.getServerPlayer().isAlive());
		require(alive, "a raid death never kills");
		waitFor(context, server, s -> Raids.instances().isEmpty(), 200, "the failed raid should close");
		connection.waitForChunksRender();

		// solo against the Kraken's ink marks: only your own mark hits you (20%)
		enterRaid(context, server, connection, RaidBosses.KRAKEN);
		float before = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.setHealth(player.getMaxHealth());
			boss(connection).runMechanic("kraken_ink_marks", player);
			return player.getHealth();
		});
		waitFor(context, server, s -> !boss(connection).inMechanic(), 400, "the ink marks should end");
		String solo = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			if (!RaidDimension.is(player.level())) {
				return "a solo player should survive the ink marks";
			}
			return player.getHealth() < before ? "" : "the own mark should still hurt";
		});
		require(solo.isEmpty(), solo);
		server.runOnServer(s -> Raids.leave(connection.getServerPlayer()));
		waitFor(context, server, s -> Raids.instances().isEmpty(), 200, "leaving should close the raid");
		server.runCommand("tick rate 20");
		server.runCommand("effect clear @p");
		connection.waitForChunksRender();
	}

	private static void enterRaid(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection, final BossDef def) {
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.WARRIOR, 4).withProgress(100, 0));
			Raids.start(s, List.of(player), def, player.getUUID());
		});
		waitFor(context, server, s -> {
			RaidInstance instance = Raids.instanceOf(connection.getServerPlayer());
			return instance != null && instance.state() == RaidInstance.State.FIGHT;
		}, 400, def.id() + " should appear");
		context.waitTicks(10);
		// Avalon (warrior tier 4) would otherwise save the player from the lethal mechanic
		server.runOnServer(s -> JobProgression.set(connection.getServerPlayer(), JobProgression.get(connection.getServerPlayer()).withJob(JobClass.ROGUE, 1)));
	}

	private static RaidBoss boss(final TestServerConnection connection) {
		ServerPlayer player = connection.getServerPlayer();
		ServerLevel level = player.level();
		return level.getEntitiesOfClass(RaidBoss.class, player.getBoundingBox().inflate(80), RaidBoss::isAlive).getFirst();
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
