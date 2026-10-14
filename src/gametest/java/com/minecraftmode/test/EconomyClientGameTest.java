package com.minecraftmode.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minecraftmode.MinecraftMode;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import com.minecraftmode.accessory.Accessories;
import com.minecraftmode.bag.Bags;
import com.minecraftmode.client.LootBuyerScreen;
import com.minecraftmode.command.WalletCommand;
import com.minecraftmode.consumable.BuffEffects;
import com.minecraftmode.consumable.ConsumableDef;
import com.minecraftmode.consumable.Consumables;
import com.minecraftmode.economy.Buyback;
import com.minecraftmode.economy.Essence;
import com.minecraftmode.economy.LootBuyer;
import com.minecraftmode.economy.MarketData;
import com.minecraftmode.economy.SellAll;
import com.minecraftmode.economy.ShopMerchant;
import com.minecraftmode.economy.ShopOffers;
import com.minecraftmode.economy.ShopType;
import com.minecraftmode.economy.Wallet;
import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.entity.CityNpc;
import com.minecraftmode.entity.boss.RaidBoss;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.GearStats;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.loot.GearDrops;
import com.minecraftmode.loot.GearShop;
import com.minecraftmode.network.LootSellPayload;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.raid.RaidDimension;
import com.minecraftmode.raid.RaidInstance;
import com.minecraftmode.raid.Raids;
import com.minecraftmode.registry.ModAttachments;
import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.registry.ModEffects;
import com.minecraftmode.registry.ModEntities;
import com.minecraftmode.registry.ModItems;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
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

			checkWallet(context, server, connection);
			checkShop(context, server, connection);
			checkSellFromBags(context, server, connection);
			checkSellAll(context, server, connection);
			checkLootBuyer(context, server, connection);
			checkBuilder(server, connection);
			checkEssence(context, server, connection);
			GearCareChecks.run(context, server, connection);
			checkConsumables(context, server, connection);
			checkReturnScroll(context, server, connection);
			checkMechanicSmoke(context, server, connection);
			checkMechanicsInRaid(context, server, connection);
			WorldClose.prepare(context, server);
		}
	}

	private static void checkContent(final ClientGameTestContext context) {
		require(Consumables.all().size() == 58, "expected 58 consumables, got " + Consumables.all().size());
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
			// extra shots read as arrows, bullets or throwing weapons for the classes that shoot them
			for (JobClass job : List.of(JobClass.ARCHER, JobClass.PIRATE, JobClass.ROGUE)) {
				String key = EngraveStat.EXTRA_SHOT.key(job);
				if (key.equals(EngraveStat.EXTRA_SHOT.key()) || !ko.has(key)) {
					missing.add("extra shot wording for " + job.id());
				}
			}
			require(ko.get("engrave_stat.minecraft_mode.extra_shot.archer").getAsString().startsWith("화살"), "archers read extra shots as arrows");
			require(EngraveStat.EXTRA_SHOT.key(JobClass.WARRIOR).equals(EngraveStat.EXTRA_SHOT.key()) && EngraveStat.EXTRA_SHOT.key(null).equals(EngraveStat.EXTRA_SHOT.key()),
				"other classes keep the plain wording");
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

	/**
	 * Selling to a shop also takes the same plain item out of the bags, as many as make whole trades up to a stack; a named stack in
	 * the bag stays where it is.
	 */
	private static void checkSellFromBags(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("setblock 0 -60 -3 minecraft_mode:shop_block");
		context.waitTicks(2);
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ItemStack bag = new ItemStack(ModItems.ORE_BAG);
			Bags.insert(bag, new ItemStack(Items.NETHERRACK, 64));
			Bags.insert(bag, new ItemStack(Items.NETHERRACK, 40));
			ItemStack named = new ItemStack(Items.NETHERRACK, 5);
			named.set(DataComponents.CUSTOM_NAME, Component.literal("keepsake"));
			Bags.insert(bag, named.copy());
			player.getInventory().add(bag);
			player.getInventory().add(new ItemStack(Items.NETHERRACK, 10));
			new ShopMerchant(player, player.level(), new BlockPos(0, -60, -3), ShopType.GENERAL)
				.openTradingScreen(player, Component.translatable(ShopType.GENERAL.titleKey()), 1);
			MerchantMenu menu = (MerchantMenu)player.containerMenu;
			int index = -1;
			for (int i = 0; i < menu.getOffers().size(); i++) {
				if (menu.getOffers().get(i).getCostA().is(Items.NETHERRACK)) {
					index = i;
				}
			}
			if (index < 0) {
				return "the general store should buy netherrack";
			}
			int need = menu.getOffers().get(index).getCostA().getCount();
			if (need > 64) {
				return "netherrack should sell by the stack or less, asks " + need;
			}
			int coins = Coins.total(player);
			menu.setSelectionHint(index);
			menu.tryMoveItems(index);
			int loaded = menu.getSlot(0).getItem().getCount();
			int whole = 64 - 64 % need;
			if (loaded != whole) {
				return "the payment slot should hold " + whole + " (10 from the inventory, the rest from the bag), got " + loaded;
			}
			// browsing to another trade puts the netherrack back into the bag, not the inventory
			int other = index == 0 ? 1 : 0;
			menu.setSelectionHint(other);
			menu.tryMoveItems(other);
			if (player.getInventory().countItem(Items.NETHERRACK) != 0 || Bags.countInBags(player, new ItemStack(Items.NETHERRACK)) != 114) {
				return "switching trades should put the netherrack into the bag: inventory " + player.getInventory().countItem(Items.NETHERRACK) + ", bag "
					+ Bags.countInBags(player, new ItemStack(Items.NETHERRACK));
			}
			menu.setSelectionHint(index);
			menu.tryMoveItems(index);
			if (menu.getSlot(0).getItem().getCount() != whole) {
				return "picking the trade again should load " + whole + " from the bag, got " + menu.getSlot(0).getItem().getCount();
			}
			menu.quickMoveStack(player, 2);
			player.closeContainer();
			int earned = Coins.total(player) - coins;
			int inBag = Bags.countInBags(player, new ItemStack(Items.NETHERRACK));
			int left = player.getInventory().countItem(Items.NETHERRACK);
			if (earned != whole / need || inBag != 114 - whole) {
				return "wrong sale: " + earned + " trades, bag keeps " + inBag + " (expected " + whole / need + " and " + (114 - whole) + ")";
			}
			if (left != 0 || Bags.countInBags(player, named) != 5) {
				return "the inventory netherrack should be sold (" + left + " left) and the named stack stay in the bag";
			}
			player.getInventory().clearContent();
			// the buyback checks later count their own sales only
			player.removeAttached(ModAttachments.SOLD);
			return "sold " + whole + " netherrack (" + earned + " copper), " + inBag + " left in the bag, inventory " + left;
		});
		require(report.startsWith("sold"), report);
		MinecraftMode.LOGGER.info("[economy] selling from bags: {}", report);
		server.runCommand("setblock 0 -60 -3 minecraft:air");
		context.waitTicks(2);
	}

	/**
	 * The shop's sell-all buttons, clicked with the real mouse: "sell all of this" sells every whole trade of the picked line (the
	 * general store's first, cobblestone) from the inventory and the bags at prices that fall trade by trade, as the preview estimated;
	 * "sell everything" takes two clicks and sells the other lines the shop buys. The hotbar and a named stack are never sold.
	 */
	private static void checkSellAll(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("setblock 0 -60 -3 minecraft_mode:shop_block");
		context.waitTicks(2);
		int[] expected = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.getInventory().clearContent();
			player.removeAttached(ModAttachments.SOLD);
			player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 64));
			for (int slot = 9; slot < 13; slot++) {
				player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
			}
			ItemStack named = new ItemStack(Items.COBBLESTONE, 10);
			named.set(DataComponents.CUSTOM_NAME, Component.literal("keepsake"));
			player.getInventory().setItem(13, named);
			player.getInventory().setItem(14, new ItemStack(Items.GRAVEL, 40));
			ItemStack bag = new ItemStack(ModItems.ORE_BAG);
			Bags.insert(bag, new ItemStack(Items.COBBLESTONE, 64));
			Bags.insert(bag, new ItemStack(Items.COBBLESTONE, 30));
			Bags.insert(bag, new ItemStack(Items.DIRT, 64));
			Bags.insert(bag, new ItemStack(Items.DIRT, 64));
			player.getInventory().setItem(15, bag);
			new ShopMerchant(player, player.level(), new BlockPos(0, -60, -3), ShopType.GENERAL)
				.openTradingScreen(player, Component.translatable(ShopType.GENERAL.titleKey()), 1);
			MerchantMenu menu = (MerchantMenu)player.containerMenu;
			// 256 in the inventory and 94 in the bag; the hotbar's 64 and the named 10 stay out of it
			SellAll.Line line = SellAll.estimate(menu.getOffers().getFirst(), SellAll.stock(player, menu, menu.getOffers().getFirst()), false);
			return new int[] {SellAll.stock(player, menu, menu.getOffers().getFirst()), line.count(), line.copper(), Wallet.balance(player)};
		});
		require(expected[0] == 4 * 64 + 94, "sell all counts the inventory and the bag but not the hotbar or a named stack, got " + expected[0]);
		require(expected[1] > 0 && expected[2] > 0, "the preview expects a sale, got " + expected[1] + " for " + expected[2]);
		context.waitForScreen(MerchantScreen.class);
		context.waitTicks(10);
		clickSellButton(context, "screen.minecraft_mode.sell_all.one", true);
		context.waitTicks(10);
		String one = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			MerchantMenu menu = (MerchantMenu)player.containerMenu;
			int earned = Wallet.balance(player) - expected[3];
			int left = SellAll.stock(player, menu, menu.getOffers().getFirst());
			int need = menu.getOffers().getFirst().getCostA().getCount();
			require(earned == expected[2], "selling all cobblestone pays what the preview said: " + earned + " vs " + expected[2]);
			require(left == expected[0] - expected[1] && left < need, "all whole trades are sold: " + left + " left, the next trade asks " + need);
			require(player.getInventory().getItem(0).getCount() == 64, "the hotbar keeps its cobblestone");
			require(player.getInventory().getItem(13).has(DataComponents.CUSTOM_NAME), "the named stack stays");
			require(need > 32, "the price moved with the sales (" + need + " cobblestone per coin now)");
			require(!Buyback.list(player).isEmpty() && Buyback.list(player).getFirst().stack().is(Items.COBBLESTONE), "the sales can be bought back");
			return "sold " + expected[1] + " cobblestone for " + Coins.format(earned) + ", " + left + " left, now " + need + " per coin";
		});
		MinecraftMode.LOGGER.info("[economy] sell all of this: {}", one);
		// "sell everything" arms on the first click and sells on the second
		hoverSellButton(context, "screen.minecraft_mode.sell_all.all");
		context.waitTicks(5);
		context.takeScreenshot("economy_sell_all");
		clickSellButton(context, "screen.minecraft_mode.sell_all.all", true);
		context.waitTicks(5);
		int gravel = server.computeOnServer(s -> connection.getServerPlayer().getInventory().countItem(Items.GRAVEL));
		require(gravel == 40, "one click on 'sell everything' only arms it, gravel " + gravel);
		clickSellButton(context, "screen.minecraft_mode.sell_all.all", true);
		context.waitTicks(10);
		String all = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			int dirt = Bags.countInBags(player, new ItemStack(Items.DIRT));
			int leftGravel = player.getInventory().countItem(Items.GRAVEL);
			require(leftGravel < 32 && dirt < 64, "sell everything sells the gravel and the bag's dirt, left " + leftGravel + " gravel, " + dirt + " dirt");
			require(player.getInventory().getItem(0).getCount() == 64, "the hotbar keeps its cobblestone");
			player.closeContainer();
			player.getInventory().clearContent();
			player.removeAttached(ModAttachments.SOLD);
			return leftGravel + " gravel and " + dirt + " dirt left, wallet " + Coins.format(Wallet.balance(player));
		});
		MinecraftMode.LOGGER.info("[economy] sell everything: {}", all);
		server.runCommand("setblock 0 -60 -3 minecraft:air");
		context.waitTicks(2);
	}

	/**
	 * The loot buyer lists class gear and accessories from the inventory and the bags (never bound pieces), "select all" leaves the
	 * hotbar and enhanced pieces out, and a sale pays the listed prices, takes exactly the picked pieces and can be bought back.
	 */
	private static void checkLootBuyer(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		int[] expected = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.getInventory().clearContent();
			player.removeAttached(ModAttachments.SOLD);
			RandomSource random = RandomSource.create(5);
			ItemStack loose = GearDrops.pick(null, 20, 30, random);
			ItemStack held = GearDrops.pick(null, 20, 30, random);
			ItemStack enhanced = GearDrops.pick(null, 20, 30, random);
			enhanced.set(ModDataComponents.ENHANCEMENT, new Enhancement(3, 0, 0));
			ItemStack bound = GearDrops.pick(null, 20, 30, random);
			bound.set(ModDataComponents.BOUND, Unit.INSTANCE);
			ItemStack accessory = Accessories.create(Accessories.all().getFirst(), 1, random);
			ItemStack inBag = GearDrops.pick(null, 40, 50, random);
			player.getInventory().setItem(0, held);
			player.getInventory().setItem(9, loose);
			player.getInventory().setItem(10, enhanced);
			player.getInventory().setItem(11, bound);
			player.getInventory().setItem(12, accessory);
			player.getInventory().setItem(13, new ItemStack(Items.IRON_SWORD));
			ItemStack bag = new ItemStack(ModItems.GEAR_BAG);
			Bags.insert(bag, inBag.copy());
			player.getInventory().setItem(15, bag);
			CityNpc npc = ModEntities.CITY_NPC.create(s.overworld(), EntitySpawnReason.COMMAND);
			npc.setRole(CityNpc.Role.LOOT_BUYER);
			npc.snapTo(2.5, -60, -1.5, 180.0F, 0.0F);
			s.overworld().addFreshEntity(npc);
			LootBuyer.open(player, npc.getId());
			require(LootBuyer.price(bound) == 0 && LootBuyer.price(new ItemStack(Items.IRON_SWORD)) == 0, "bound pieces and vanilla gear are not bought");
			require(LootBuyer.price(loose) == GearShop.buybackPrice(ClassGear.of(loose)), "class gear sells at the guild's buyback price");
			return new int[] {LootBuyer.price(loose) + LootBuyer.price(accessory) + LootBuyer.price(inBag), Wallet.balance(player), npc.getId()};
		});
		context.waitForScreen(LootBuyerScreen.class);
		context.waitTicks(5);
		String picked = context.computeOnClient(minecraft -> {
			LootBuyerScreen screen = (LootBuyerScreen)minecraft.gui.screen();
			if (screen.entries().size() != 5) {
				return "the buyer lists the hotbar, inventory, enhanced, accessory and bag pieces, got " + screen.entries().size();
			}
			screen.select(false);
			return screen.picked().size() == 3 ? "" : "select all leaves the hotbar and enhanced pieces out, picked " + screen.picked();
		});
		require(picked.isEmpty(), picked);
		context.waitTicks(2);
		context.takeScreenshot("economy_loot_buyer");
		context.runOnClient(minecraft -> ClientPlayNetworking.send(new LootSellPayload(expected[2], ((LootBuyerScreen)minecraft.gui.screen()).picked())));
		context.waitTicks(10);
		String sold = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			int earned = Wallet.balance(player) - expected[1];
			require(earned == expected[0], "the sale pays the listed prices: " + earned + " vs " + expected[0]);
			require(player.getInventory().getItem(9).isEmpty() && player.getInventory().getItem(12).isEmpty(), "the picked pieces are gone");
			require(!player.getInventory().getItem(0).isEmpty() && !player.getInventory().getItem(10).isEmpty() && !player.getInventory().getItem(11).isEmpty(),
				"the hotbar, enhanced and bound pieces stay");
			require(Bags.contents(player.getInventory().getItem(15)).stream().allMatch(ItemStack::isEmpty), "the bag's piece is sold too");
			require(Buyback.list(player).size() == 3, "every sold piece can be bought back, got " + Buyback.list(player).size());
			return "sold 3 pieces for " + Coins.format(earned);
		});
		MinecraftMode.LOGGER.info("[economy] loot buyer: {}", sold);
		require(context.computeOnClient(minecraft -> minecraft.gui.screen() instanceof LootBuyerScreen screen && screen.entries().size() == 2 && screen.picked().isEmpty()),
			"after a sale the buyer shows what is left");
		context.waitTicks(2);
		context.takeScreenshot("economy_loot_buyer_after");
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.getInventory().clearContent();
			player.removeAttached(ModAttachments.SOLD);
			s.overworld().getEntitiesOfClass(CityNpc.class, player.getBoundingBox().inflate(8)).forEach(CityNpc::discard);
		});
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
	}

	/** Clicks a sell-all button of the open shop screen with the real mouse; it must be shown and (when {@code active}) enabled. */
	private static void clickSellButton(final ClientGameTestContext context, final String key, final boolean active) {
		double[] pos = sellButton(context, key);
		require(pos[2] > 0 == active, key + " should be " + (active ? "enabled" : "disabled"));
		context.getInput().setCursorPos(pos[0], pos[1]);
		context.waitTicks(2);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
	}

	private static void hoverSellButton(final ClientGameTestContext context, final String key) {
		double[] pos = sellButton(context, key);
		context.getInput().setCursorPos(pos[0], pos[1]);
	}

	/** Window position of a sell-all button's middle, and 1 when it is enabled. */
	private static double[] sellButton(final ClientGameTestContext context, final String key) {
		double[] pos = context.computeOnClient(minecraft -> {
			double scale = minecraft.getWindow().getGuiScale();
			for (GuiEventListener child : minecraft.gui.screen().children()) {
				if (child instanceof Button button && button.visible && button.getMessage().getContents() instanceof TranslatableContents text
					&& (text.getKey().equals(key) || key.endsWith(".all") && text.getKey().equals("screen.minecraft_mode.sell_all.confirm"))) {
					return new double[] {(button.getX() + button.getWidth() / 2.0) * scale, (button.getY() + button.getHeight() / 2.0) * scale, button.active ? 1 : 0};
				}
			}
			return null;
		});
		require(pos != null, "the shop screen should show the " + key + " button");
		return pos;
	}

	/**
	 * The builder's merchant keeps its prices however much is bought (no market pressure), and sells blocks by the stack.
	 */
	private static void checkBuilder(final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.getInventory().clearContent();
			ShopMerchant builder = new ShopMerchant(player, player.level(), new BlockPos(0, -60, -3), ShopType.BUILDER);
			MerchantOffer stone = builder.getOffers().getFirst();
			require(stone.getResult().is(Items.STONE) && stone.getResult().getCount() == 64, "the builder sells stone by the stack, got " + stone.getResult());
			int cost = stone.getCostA().getCount();
			for (int i = 0; i < MarketData.MAX_PRESSURE + 5; i++) {
				builder.notifyTrade(stone);
			}
			String key = ShopType.BUILDER.marketId() + ":" + ShopOffers.trades(ShopType.BUILDER).getFirst().key();
			int pressure = MarketData.get(s).pressure(key, player.level().getGameTime());
			ShopMerchant again = new ShopMerchant(player, player.level(), new BlockPos(0, -60, -3), ShopType.BUILDER);
			require(stone.getCostA().getCount() == cost && again.getOffers().getFirst().getCostA().getCount() == cost && pressure == 0,
				"the builder's prices never move: " + cost + " -> " + stone.getCostA().getCount() + " / " + again.getOffers().getFirst().getCostA().getCount()
					+ ", pressure " + pressure);
			for (MerchantOffer offer : builder.getOffers()) {
				require(!SellAll.isSale(offer), "the builder only sells, but buys " + offer.getCostA());
			}
			return builder.getOffers().size() + " lines, stone " + cost + "C a stack after " + (MarketData.MAX_PRESSURE + 5) + " trades";
		});
		MinecraftMode.LOGGER.info("[economy] builder: {}", report);
	}

	/** Essence pays as one currency: condensed essence is broken for an essence trade (change back as essence), and both convert on use. */
	private static void checkEssence(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("setblock 0 -60 -3 minecraft_mode:guild_shop");
		context.waitTicks(2);
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			Inventory inventory = player.getInventory();
			inventory.clearContent();
			Wallet.add(player, 50000);
			inventory.add(new ItemStack(ModItems.CONDENSED_ESSENCE, 1));
			new ShopMerchant(player, player.level(), new BlockPos(0, -60, -3), ShopType.GUILD)
				.openTradingScreen(player, Component.translatable(ShopType.GUILD.titleKey()), 1);
			MerchantMenu menu = (MerchantMenu)player.containerMenu;
			int index = -1;
			for (int i = 0; i < menu.getOffers().size(); i++) {
				ItemStack costB = menu.getOffers().get(i).getCostB();
				if (costB.is(ModItems.ESSENCE) && costB.getCount() < Essence.CONDENSED) {
					index = i;
					break;
				}
			}
			if (index < 0) {
				player.closeContainer();
				return "the guild should sell something for fewer than 9 essence";
			}
			MerchantOffer offer = menu.getOffers().get(index);
			int need = offer.getCostB().getCount();
			menu.setSelectionHint(index);
			menu.tryMoveItems(index);
			ItemStack paid = menu.getSlot(1).getItem();
			if (!paid.is(ModItems.ESSENCE) || paid.getCount() != need) {
				player.closeContainer();
				return "a condensed essence should be broken into the " + need + " essence the trade needs, slot has " + paid;
			}
			if (JobProgression.count(inventory, ModItems.CONDENSED_ESSENCE) != 0 || JobProgression.count(inventory, ModItems.ESSENCE) != Essence.CONDENSED - need) {
				player.closeContainer();
				return "the change should come back as " + (Essence.CONDENSED - need) + " essence";
			}
			menu.quickMoveStack(player, 2);
			player.closeContainer();
			if (inventory.countItem(offer.getResult().getItem()) < 1) {
				return "the gear should be bought with the broken essence";
			}
			// condensed costs are made up from loose essence, and both kinds convert by hand
			inventory.clearContent();
			inventory.add(new ItemStack(ModItems.ESSENCE, 20));
			Essence.take(inventory, ModItems.CONDENSED_ESSENCE, 2);
			if (JobProgression.count(inventory, ModItems.ESSENCE) != 2) {
				return "2 condensed essence should take 18 loose essence, left " + JobProgression.count(inventory, ModItems.ESSENCE);
			}
			inventory.clearContent();
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CONDENSED_ESSENCE, 2));
			player.getMainHandItem().use(player.level(), player, InteractionHand.MAIN_HAND);
			if (JobProgression.count(inventory, ModItems.CONDENSED_ESSENCE) != 1 || JobProgression.count(inventory, ModItems.ESSENCE) != Essence.CONDENSED) {
				return "using condensed essence should break one into 9 essence";
			}
			inventory.clearContent();
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ESSENCE, 20));
			player.getMainHandItem().use(player.level(), player, InteractionHand.MAIN_HAND);
			int loose = JobProgression.count(inventory, ModItems.ESSENCE);
			int condensed = JobProgression.count(inventory, ModItems.CONDENSED_ESSENCE);
			inventory.clearContent();
			return loose == 11 && condensed == 1 ? "" : "using essence should condense 9 into one, got " + loose + " essence and " + condensed + " condensed";
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
		require(total == 17, "expected 17 mechanics, got " + total);
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
