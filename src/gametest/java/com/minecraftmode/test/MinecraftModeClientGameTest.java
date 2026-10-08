package com.minecraftmode.test;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.economy.ShopMerchant;
import com.minecraftmode.economy.ShopOffers;
import com.minecraftmode.economy.ShopType;
import com.minecraftmode.enchantment.ModEnchantments;
import com.minecraftmode.entity.MythrilGolem;
import com.minecraftmode.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import com.minecraftmode.registry.ModBlocks;
import com.minecraftmode.registry.ModEntities;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.worldgen.ModOreGeneration;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;

/**
 * End-to-end checks in a real client + integrated server. Screenshots land in
 * build/run/clientGameTest/screenshots.
 */
public class MinecraftModeClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(final ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerConnection connection = singleplayer.getConnection();
			TestServerContext server = singleplayer.getServer();
			connection.waitForChunksRender();
			server.runCommand("difficulty normal");
			server.runCommand("time set noon");

			server.runOnServer(MinecraftModeClientGameTest::checkDataLoaded);
			server.runOnServer(MinecraftModeClientGameTest::checkOreInjectedIntoBiomes);
			checkOreFeaturePlaces(context, server);
			checkBleeding(context, server, connection);
			checkMineRaiderLoot(context, server, connection);
			checkNewEnchantments(context, server, connection);
			inventoryScreenshot(context, server);
			mineRaiderScreenshot(context, server, connection);
			checkMythrilGolem(context, server, connection);
			armorScreenshot(context, server);
			blocksScreenshot(context, server);
			checkShopTrade(context, server, connection);
		}
	}

	private static void checkDataLoaded(final MinecraftServer server) {
		RegistryAccess access = server.registryAccess();

		for (ResourceKey<Enchantment> key : List.of(ModEnchantments.LIFESTEAL, ModEnchantments.BLEEDING_EDGE, ModEnchantments.COIN_FINDER)) {
			require(access.lookupOrThrow(Registries.ENCHANTMENT).get(key).isPresent(), "enchantment missing: " + key);
		}
		require(
			access.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ModEnchantments.BLEEDING_EDGE).is(EnchantmentTags.IN_ENCHANTING_TABLE),
			"bleeding_edge is not offered by the enchanting table"
		);
		require(access.lookupOrThrow(Registries.FEATURE).get(ModOreGeneration.ORE_MYTHRIL).isPresent(), "ore_mythril feature missing");
		require(access.lookupOrThrow(Registries.PLACED_FEATURE).get(ModOreGeneration.ORE_ALUMINUM_PLACED).isPresent(), "ore_aluminum placed feature missing");

		for (String recipe : List.of("mythril_pickaxe", "mythril_chestplate", "shop_block", "plastic_sheet_from_slime_ball", "silver_coin_from_copper_coins",
			"mythril_ingot_from_smelting_raw_mythril", "aluminum_ingot_from_blasting_aluminum_ore")) {
			require(server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, MinecraftMode.id(recipe))).isPresent(), "recipe missing: " + recipe);
		}

		require(
			server.reloadableRegistries().getLootTable(ModEntities.MINE_RAIDER.getDefaultLootTable().orElseThrow()) != LootTable.EMPTY,
			"mine_raider loot table missing"
		);
		require(
			server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, MinecraftMode.id("blocks/mythril_ore"))) != LootTable.EMPTY,
			"mythril_ore loot table missing"
		);

		require(ModBlocks.MYTHRIL_ORE.defaultBlockState().is(BlockTags.NEEDS_IRON_TOOL), "mythril_ore should need an iron tool");
		require(ModBlocks.ALUMINUM_ORE.defaultBlockState().is(ConventionalBlockTags.ORES), "aluminum_ore should be in c:ores");
		require(new ItemStack(ModItems.MYTHRIL_SWORD).is(ItemTags.SWORDS), "mythril_sword should be in #swords");
		MinecraftMode.LOGGER.info("[test] data, registries and tags OK");
	}

	private static void checkOreInjectedIntoBiomes(final MinecraftServer server) {
		var plains = server.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS).value();
		boolean injected = plains.getGenerationSettings().features().stream()
			.flatMap(HolderSet::stream)
			.anyMatch(feature -> feature.is(ModOreGeneration.ORE_MYTHRIL_PLACED));
		require(injected, "ore_mythril is not in the plains biome feature list");
		MinecraftMode.LOGGER.info("[test] ore features injected into overworld biomes");
	}

	private static void checkOreFeaturePlaces(final ClientGameTestContext context, final TestServerContext server) {
		// The test world is superflat (no stone), so build a stone cube in the air and place the ore inside it.
		server.runCommand("fill -4 100 -4 4 108 4 minecraft:stone");
		server.runCommand("place feature minecraft_mode:ore_mythril 0 104 0");
		server.runCommand("place feature minecraft_mode:ore_aluminum 0 104 0");
		context.waitTicks(2);
		int[] counts = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			int mythril = 0, aluminum = 0;
			for (BlockPos pos : BlockPos.betweenClosed(-4, 100, -4, 4, 108, 4)) {
				if (level.getBlockState(pos).is(ModBlocks.MYTHRIL_ORE)) {
					mythril++;
				} else if (level.getBlockState(pos).is(ModBlocks.ALUMINUM_ORE)) {
					aluminum++;
				}
			}
			return new int[] {mythril, aluminum};
		});
		require(counts[0] > 0 && counts[1] > 0, "ore features placed nothing: mythril=" + counts[0] + " aluminum=" + counts[1]);
		server.runCommand("fill -4 100 -4 4 108 4 minecraft:air");
		MinecraftMode.LOGGER.info("[test] ore features placed mythril={} aluminum={}", counts[0], counts[1]);
	}

	private static void checkBleeding(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("execute at @p run summon minecraft:pig ~6 ~ ~6 {NoAI:1b,Tags:[\"bleed_test\"]}");
		server.runCommand("effect give @e[tag=bleed_test] minecraft_mode:bleeding 10 0");
		context.waitTicks(45);
		float health = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			List<LivingEntity> pigs = player.level().getEntitiesOfClass(
				LivingEntity.class, player.getBoundingBox().inflate(16), e -> e.entityTags().contains("bleed_test")
			);
			require(!pigs.isEmpty(), "test pig not found");
			return pigs.getFirst().getHealth() / pigs.getFirst().getMaxHealth();
		});
		require(health < 1.0F, "bleeding did not damage the pig");
		server.runCommand("kill @e[tag=bleed_test]");
		MinecraftMode.LOGGER.info("[test] bleeding damaged the pig (health ratio {})", health);
	}

	private static void checkMineRaiderLoot(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("execute at @p run summon minecraft_mode:mine_raider ~5 ~ ~ {NoAI:1b,Tags:[\"loot_test\"]}");
		server.runCommand("damage @e[tag=loot_test,limit=1] 1000 minecraft:player_attack by @p");
		context.waitTicks(5);
		int coins = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			return player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(16), e -> e.getItem().is(ModItems.COPPER_COIN))
				.stream().mapToInt(e -> e.getItem().getCount()).sum();
		});
		require(coins >= 1, "mine raider dropped no copper coins");
		server.runCommand("kill @e[type=minecraft:item]");
		MinecraftMode.LOGGER.info("[test] mine raider dropped {} copper coins", coins);
	}

	private static void checkNewEnchantments(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("gamemode survival @p");
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("item replace entity @p weapon.mainhand with minecraft:iron_pickaxe[enchantments={\"minecraft_mode:auto_smelt\":1}]");
		server.runCommand("item replace entity @p armor.feet with minecraft:iron_boots[enchantments={\"minecraft_mode:swift_step\":3}]");
		context.waitTicks(2);

		double speed = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = player.level();
			BlockPos base = player.blockPosition().offset(3, 0, 0);
			level.setBlockAndUpdate(base, Blocks.IRON_ORE.defaultBlockState());
			level.setBlockAndUpdate(base.above(), ModBlocks.MYTHRIL_ORE.defaultBlockState());
			level.setBlockAndUpdate(base.above(2), Blocks.SAND.defaultBlockState());
			player.gameMode.destroyBlock(base.above(2));
			player.gameMode.destroyBlock(base.above());
			player.gameMode.destroyBlock(base);
			return player.getAttributeValue(Attributes.MOVEMENT_SPEED);
		});
		context.waitTicks(3);
		List<String> drops = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			return player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(8)).stream()
				.map(e -> BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).getPath())
				.sorted()
				.toList();
		});
		require(drops.contains("iron_ingot") && drops.contains("mythril_ingot") && drops.contains("glass"), "auto smelt drops were " + drops);
		require(!drops.contains("raw_iron") && !drops.contains("raw_mythril") && !drops.contains("sand"), "auto smelt left raw drops: " + drops);
		require(Math.abs(speed - 0.13) < 0.001, "swift step III should give movement speed 0.13, got " + speed);
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("clear @p");
		MinecraftMode.LOGGER.info("[test] auto smelt drops {}; swift step III speed {}", drops, speed);
	}

	private static void inventoryScreenshot(final ClientGameTestContext context, final TestServerContext server) {
		server.runCommand("gamemode survival @p");
		server.runCommand("clear @p");
		for (Item item : BuiltInRegistries.ITEM) {
			var id = BuiltInRegistries.ITEM.getKey(item);
			if (id.getNamespace().equals(MinecraftMode.MOD_ID)) {
				server.runCommand("give @p " + id);
			}
		}
		context.waitTicks(5);
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitForScreen(InventoryScreen.class);
		context.waitTicks(5);
		context.takeScreenshot("inventory_items");
		context.setScreen(() -> null);
	}

	private static void mineRaiderScreenshot(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("tp @p 0.5 -60 0.5 180 10");
		// No NBT here: /summon only runs finalizeSpawn (which hands out the pickaxe) without NBT.
		server.runCommand("summon minecraft_mode:mine_raider 0.5 -60 -3");
		server.runCommand("data merge entity @e[type=minecraft_mode:mine_raider,limit=1] {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f],Tags:[\"pose\"]}");
		boolean armed = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(16), e -> e.entityTags().contains("pose"))
				.stream().anyMatch(e -> e.getMainHandItem().is(ItemTags.PICKAXES));
		});
		require(armed, "mine raider spawned without a pickaxe");
		connection.waitForClientboundEntityUpdates(ModEntities.MINE_RAIDER);
		context.waitTicks(20);
		context.takeScreenshot("mine_raider");
		server.runCommand("kill @e[tag=pose]");
	}

	private static void checkMythrilGolem(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("gamemode survival @p");
		server.runCommand("effect give @p minecraft:instant_health 1 10");
		server.runCommand("tp @p 0.5 -60 0.5 180 0");
		server.runCommand("summon minecraft_mode:mythril_golem 0.5 -60 -5");
		server.runCommand("data merge entity @e[type=minecraft_mode:mythril_golem,limit=1] {NoAI:1b,Rotation:[0f,0f],Tags:[\"golem\"]}");
		connection.waitForClientboundEntityUpdates(ModEntities.MYTHRIL_GOLEM);
		// Let the death particles from the previous step fade out
		context.waitTicks(60);
		context.takeScreenshot("mythril_golem");

		String stats = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			MythrilGolem golem = player.level().getEntitiesOfClass(MythrilGolem.class, player.getBoundingBox().inflate(16)).getFirst();
			require(golem.getMaxHealth() == 150.0F && golem.getArmorValue() == 10, "golem stats " + golem.getMaxHealth() + " hp / " + golem.getArmorValue() + " armor");
			require(golem.bossEvent().getPlayers().contains(player), "boss bar is not shown to the nearby player");
			golem.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 100, 0));
			require(!golem.hasEffect(ModEffects.BLEEDING), "golem should be immune to bleeding");

			// Ground slam: stand next to it
			player.teleportTo(golem.getX(), golem.getY(), golem.getZ() + 2.5);
			float before = player.getHealth();
			int hit = golem.groundSlam(player.level());
			require(hit == 1 && player.getHealth() < before && player.hasEffect(ModEffects.BLEEDING), "ground slam should hurt and cut the player");
			return "slam hit " + hit + " player(s) for " + (before - player.getHealth());
		});

		server.runCommand("effect give @p minecraft:instant_health 1 10");
		server.runCommand("damage @e[tag=golem,limit=1] 1000 minecraft:player_attack by @p");
		context.waitTicks(5);
		List<String> drops = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			return player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(16)).stream()
				.map(e -> BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).getPath())
				.distinct()
				.sorted()
				.toList();
		});
		require(drops.contains("gold_coin") && drops.contains("raw_mythril_block") && drops.contains("mythril_ingot"), "golem drops were " + drops);
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("effect clear @p");
		server.runCommand("effect give @p minecraft:instant_health 1 10");
		MinecraftMode.LOGGER.info("[test] mythril golem: 150 hp / 10 armor, boss bar shown, bleeding immune, {}, drops {}", stats, drops);
	}

	private static void armorScreenshot(final ClientGameTestContext context, final TestServerContext server) {
		server.runCommand("item replace entity @p armor.head with minecraft_mode:mythril_helmet");
		server.runCommand("item replace entity @p armor.chest with minecraft_mode:mythril_chestplate");
		server.runCommand("item replace entity @p armor.legs with minecraft_mode:mythril_leggings");
		server.runCommand("item replace entity @p armor.feet with minecraft_mode:mythril_boots");
		server.runCommand("item replace entity @p weapon.mainhand with minecraft_mode:mythril_pickaxe");
		context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(20);
		context.takeScreenshot("mythril_armor");
		context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
	}

	private static void blocksScreenshot(final ClientGameTestContext context, final TestServerContext server) {
		String[][] rows = {
			{"shop_block", "blacksmith_shop", "plastic_block", "grocer_shop", "jeweler_shop"},
			{"mythril_ore", "deepslate_mythril_ore", "mythril_block", "raw_mythril_block", "plastic_block"},
			{"aluminum_ore", "deepslate_aluminum_ore", "aluminum_block", "raw_aluminum_block", "plastic_block"},
		};
		server.runCommand("item replace entity @p weapon.mainhand with minecraft:air");
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("tp @p 0.5 -60 0.5 180 15");
		for (int row = 0; row < rows.length; row++) {
			for (int i = 0; i < rows[row].length; i++) {
				server.runCommand("setblock " + (i - 2) + " " + (-58 - row) + " -4 minecraft_mode:" + rows[row][i]);
			}
		}
		// Let the death particles from the previous step fade out
		context.waitTicks(60);
		context.takeScreenshot("blocks");
		server.runCommand("fill -2 -60 -4 2 -58 -4 minecraft:air");
	}

	private static void checkShopTrade(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("clear @p");
		server.runCommand("give @p minecraft:cobblestone 64");
		server.runCommand("setblock 0 -60 -3 minecraft_mode:shop_block");
		context.waitTicks(2);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			new ShopMerchant(player, player.level(), new BlockPos(0, -60, -3), ShopType.GENERAL)
				.openTradingScreen(player, Component.translatable(ShopType.GENERAL.titleKey()), 1);
		});
		context.waitForScreen(MerchantScreen.class);
		context.waitTicks(10);
		context.takeScreenshot("shop_screen");

		// Shift-click the result slot once (one trade): vanilla would cast the trader to Entity here (see MerchantMenuMixin).
		int[] result = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			MerchantMenu menu = (MerchantMenu) player.containerMenu;
			menu.setSelectionHint(0);
			menu.tryMoveItems(0);
			menu.quickMoveStack(player, 2);
			int newPrice = menu.getOffers().getFirst().getCostA().getCount();
			player.closeContainer();
			return new int[] {player.getInventory().countItem(ModItems.COPPER_COIN), player.getInventory().countItem(Items.COBBLESTONE), newPrice};
		});
		require(result[0] == 1 && result[1] == 32, "expected 1 copper coin and 32 cobblestone left, got " + result[0] + " / " + result[1]);
		// Market pressure 1: 32 + floor(32 * 0.1) = 35 cobblestone per coin
		require(result[2] == 35, "selling should raise the cobblestone price from 32 to 35, got " + result[2]);

		// A fresh shop screen still sees the raised price (shared, persistent market).
		int reopenedPrice = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			return new ShopMerchant(player, player.level(), new BlockPos(0, -60, -3), ShopType.GENERAL).getOffers().getFirst().getCostA().getCount();
		});
		require(reopenedPrice == 35, "reopened shop should keep the market price 35, got " + reopenedPrice);
		MinecraftMode.LOGGER.info("[test] shop shift-click trade OK; cobblestone price 32 -> {} after one sale", result[2]);

		for (ShopType type : ShopType.values()) {
			int offers = server.computeOnServer(s -> {
				ServerPlayer player = connection.getServerPlayer();
				return new ShopMerchant(player, player.level(), new BlockPos(0, -60, -3), type).getOffers().size();
			});
			require(offers == ShopOffers.trades(type).size() && offers > 0, type + " shop has " + offers + " offers");
		}
		server.runCommand("setblock 0 -60 -3 minecraft_mode:blacksmith_shop");
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			new ShopMerchant(player, player.level(), new BlockPos(0, -60, -3), ShopType.BLACKSMITH)
				.openTradingScreen(player, Component.translatable(ShopType.BLACKSMITH.titleKey()), 1);
		});
		context.waitForScreen(MerchantScreen.class);
		context.waitTicks(10);
		context.takeScreenshot("blacksmith_screen");
		context.setScreen(() -> null);
	}

	private static void require(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
