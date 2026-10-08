package com.minecraftmode.test;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.enchantment.ArmorAuras;
import com.minecraftmode.enchantment.ArmorEnchantments;
import com.minecraftmode.enchantment.EnchantInfo;
import com.minecraftmode.enchantment.ModEnchantments;
import com.minecraftmode.enchantment.RangedEnchantments;
import com.minecraftmode.enchantment.ToolEnchantments;
import com.minecraftmode.enchantment.WeaponEnchantments;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Checks the per-equipment enchantment sets: counts, auras vs buffs, and the code-driven enchantments. */
public class EnchantmentClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(final ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerConnection connection = singleplayer.getConnection();
			TestServerContext server = singleplayer.getServer();
			connection.waitForChunksRender();
			server.runCommand("difficulty normal");
			server.runCommand("gamemode survival @p");
			server.runCommand("tp @p 0.5 -60 0.5 180 0");

			checkRegistry(server);
			checkAuras(context, server, connection);
			checkVeinMinerTimberExcavationMagnet(context, server, connection);
			checkQuickDraw(server, connection);
			checkVenom(context, server, connection);
			nightSightScreenshots(context, server);
		}
	}

	private static void checkRegistry(final TestServerContext server) {
		Map<String, List<EnchantInfo>> sets = Map.of(
			"sword", WeaponEnchantments.ALL.subList(0, 10),
			"axe", WeaponEnchantments.ALL.subList(10, 20),
			"bow", RangedEnchantments.ALL.subList(0, 10),
			"crossbow", RangedEnchantments.ALL.subList(10, 20),
			"tool", ToolEnchantments.ALL,
			"armor buff", ArmorEnchantments.BUFFS,
			"armor aura", ArmorEnchantments.AURAS
		);
		server.runOnServer(s -> {
			var registry = s.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
			for (var set : sets.entrySet()) {
				require(set.getValue().size() >= 10, set.getKey() + " has only " + set.getValue().size() + " enchantments");
				for (EnchantInfo info : set.getValue()) {
					require(registry.get(info.key()).isPresent(), "enchantment not loaded: " + info.key());
				}
			}
			for (EnchantInfo info : ModEnchantments.ALL) {
				require(registry.get(info.key()).isPresent(), "enchantment not loaded: " + info.key());
			}
		});
		MinecraftMode.LOGGER.info("[ench] {} mod enchantments loaded; 10+ each for sword/axe/bow/crossbow/tools/armor buffs/armor auras", ModEnchantments.ALL.size());
	}

	private static void checkAuras(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		// Strength buff II on the chestplate, Aura of Strength III on the chestplate and V on the helmet.
		server.runCommand("item replace entity @p armor.chest with minecraft:iron_chestplate[enchantments={\"minecraft_mode:strength\":2,\"minecraft_mode:strength_aura\":3}]");
		server.runCommand("item replace entity @p armor.head with minecraft:iron_helmet[enchantments={\"minecraft_mode:strength_aura\":5}]");
		context.waitTicks(45);
		double[] withAura = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			return new double[] {ArmorAuras.currentBonus(player, ArmorAuras.Aura.STRENGTH), player.getAttributeValue(Attributes.ATTACK_DAMAGE)};
		});
		// Aura levels never add up: max(3, 5) = 5, not 8. Buff (+2) and aura (+5) do combine: 1 base + 2 + 5 = 8.
		require(withAura[0] == 5.0, "aura should apply the strongest level (5), got " + withAura[0]);
		require(withAura[1] == 8.0, "attack damage should be 1 + buff 2 + aura 5 = 8, got " + withAura[1]);

		server.runCommand("item replace entity @p armor.chest with minecraft:air");
		server.runCommand("item replace entity @p armor.head with minecraft:air");
		context.waitTicks(25);
		double without = server.computeOnServer(s -> connection.getServerPlayer().getAttributeValue(Attributes.ATTACK_DAMAGE));
		require(without == 1.0, "aura and buff should be gone after taking the armor off, attack damage " + without);
		MinecraftMode.LOGGER.info("[ench] auras: strongest level wins (III + V -> V), buff + aura stack (attack 8.0), removed when unequipped");
	}

	private static void checkVeinMinerTimberExcavationMagnet(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("tp @p 0.5 -60 0.5 180 0");
		context.waitTicks(2);
		int[] remaining = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = player.level();

			// Vein Miner II: a row of 6 iron ores, break the first
			for (int i = 0; i < 6; i++) {
				level.setBlockAndUpdate(new BlockPos(10 + i, -60, 0), Blocks.IRON_ORE.defaultBlockState());
			}
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, enchanted(s, new ItemStack(Items.IRON_PICKAXE), ToolEnchantments.VEIN_MINER, 2));
			player.gameMode.destroyBlock(new BlockPos(10, -60, 0));
			int ores = 0;
			for (int i = 0; i < 6; i++) {
				ores += level.getBlockState(new BlockPos(10 + i, -60, 0)).is(Blocks.IRON_ORE) ? 1 : 0;
			}

			// Timber: a 5-log trunk, chop the bottom
			for (int y = -60; y <= -56; y++) {
				level.setBlockAndUpdate(new BlockPos(14, y, 4), Blocks.OAK_LOG.defaultBlockState());
			}
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, enchanted(s, new ItemStack(Items.IRON_AXE), WeaponEnchantments.TIMBER, 1));
			player.gameMode.destroyBlock(new BlockPos(14, -60, 4));
			int logs = 0;
			for (int y = -60; y <= -56; y++) {
				logs += level.getBlockState(new BlockPos(14, y, 4)).is(Blocks.OAK_LOG) ? 1 : 0;
			}

			// Excavation I: a 3x3 stone wall north of the player (who faces north), break the center
			for (int x = -1; x <= 1; x++) {
				for (int y = -59; y <= -57; y++) {
					level.setBlockAndUpdate(new BlockPos(x, y, -6), Blocks.STONE.defaultBlockState());
				}
			}
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, enchanted(s, new ItemStack(Items.IRON_PICKAXE), ToolEnchantments.EXCAVATION, 1));
			player.gameMode.destroyBlock(new BlockPos(0, -58, -6));
			int stones = 0;
			for (int x = -1; x <= 1; x++) {
				for (int y = -59; y <= -57; y++) {
					stones += level.getBlockState(new BlockPos(x, y, -6)).is(Blocks.STONE) ? 1 : 0;
				}
			}

			// Magnet: the drop goes into the inventory (clear the excavation drops first)
			level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(16)).forEach(ItemEntity::discard);
			player.getInventory().clearContent();
			level.setBlockAndUpdate(new BlockPos(3, -60, -3), Blocks.COBBLESTONE.defaultBlockState());
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, enchanted(s, new ItemStack(Items.IRON_PICKAXE), ToolEnchantments.MAGNET, 1));
			player.gameMode.destroyBlock(new BlockPos(3, -60, -3));
			int inInventory = player.getInventory().countItem(Items.COBBLESTONE);
			return new int[] {ores, logs, stones, inInventory};
		});
		context.waitTicks(2);
		int looseCobble = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			return player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(8), e -> e.getItem().is(Items.COBBLESTONE)).size();
		});
		require(remaining[0] == 0, "vein miner left " + remaining[0] + " of 6 iron ores");
		require(remaining[1] == 0, "timber left " + remaining[1] + " of 5 logs");
		require(remaining[2] == 0, "excavation left " + remaining[2] + " of 9 stones");
		require(remaining[3] == 1 && looseCobble == 0, "magnet: " + remaining[3] + " cobblestone in inventory, " + looseCobble + " on the ground");
		server.runCommand("kill @e[type=minecraft:item]");
		MinecraftMode.LOGGER.info("[ench] vein miner (6/6 ores), timber (5/5 logs), excavation (9/9 stone), magnet (drop in inventory) OK");
	}

	private static void checkQuickDraw(final TestServerContext server, final TestServerConnection connection) {
		double[] speeds = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.getInventory().add(new ItemStack(Items.ARROW, 8));
			double plain = shootAfter(player, new ItemStack(Items.BOW), 8);
			double quick = shootAfter(player, enchanted(s, new ItemStack(Items.BOW), RangedEnchantments.QUICK_DRAW, 4), 8);
			return new double[] {plain, quick};
		});
		// After 8 ticks a plain bow has power 0.32 (speed ~0.96); Quick Draw IV counts 16 ticks: power 0.75 (speed ~2.24).
		require(speeds[1] > speeds[0] * 1.8, "quick draw IV arrow speed " + speeds[1] + " vs plain " + speeds[0]);
		MinecraftMode.LOGGER.info("[ench] quick draw: arrow speed after 8 ticks {} (plain) -> {} (Quick Draw IV)", speeds[0], speeds[1]);
	}

	private static double shootAfter(final ServerPlayer player, final ItemStack bow, final int ticksDrawn) {
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, bow);
		bow.getItem().releaseUsing(bow, player.level(), player, bow.getUseDuration(player) - ticksDrawn);
		List<Projectile> projectiles = player.level().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(4));
		require(!projectiles.isEmpty(), "the bow did not shoot");
		double speed = projectiles.getFirst().getDeltaMovement().length();
		projectiles.forEach(p -> p.discard());
		return speed;
	}

	private static void checkVenom(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("summon minecraft:pig 0.5 -60 -1.5 {NoAI:1b,Tags:[\"venom_test\"]}");
		context.waitTicks(2);
		boolean poisoned = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			LivingEntity pig = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6), e -> e.entityTags().contains("venom_test")).getFirst();
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, enchanted(s, new ItemStack(Items.IRON_SWORD), WeaponEnchantments.VENOM, 1));
			player.resetAttackStrengthTicker();
			player.attack(pig);
			return pig.hasEffect(MobEffects.POISON);
		});
		require(poisoned, "venom sword did not poison the pig");
		server.runCommand("kill @e[tag=venom_test]");
		MinecraftMode.LOGGER.info("[ench] data-driven on-hit effect works (Venom poisoned the target)");
	}

	private static void nightSightScreenshots(final ClientGameTestContext context, final TestServerContext server) {
		server.runCommand("gamerule send_command_feedback false");
		server.runCommand("gamemode creative @p");
		server.runCommand("time set midnight");
		server.runCommand("item replace entity @p armor.head with minecraft:air");
		server.runCommand("tp @p 0.5 -60 0.5 180 20");
		context.waitTicks(40);
		context.takeScreenshot("night_without_night_sight");
		server.runCommand("item replace entity @p armor.head with minecraft:iron_helmet[enchantments={\"minecraft_mode:night_sight\":5}]");
		context.waitTicks(40);
		context.takeScreenshot("night_with_night_sight_5");
		server.runCommand("time set noon");
	}

	private static ItemStack enchanted(final net.minecraft.server.MinecraftServer server, final ItemStack stack, final EnchantInfo info, final int level) {
		stack.enchant(server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(info.key()), level);
		return stack;
	}

	private static void require(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
