package com.minecraftmode.test;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.job.CharacterScreen;
import com.minecraftmode.client.job.UpgradeScreen;
import com.minecraftmode.economy.Buyback;
import com.minecraftmode.economy.ShopMerchant;
import com.minecraftmode.economy.ShopType;
import com.minecraftmode.economy.Wallet;
import com.minecraftmode.enhance.EnhanceMenu;
import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.GearRolls;
import com.minecraftmode.job.gear.GearStats;
import com.minecraftmode.loot.EvolutionEtherItem;
import com.minecraftmode.loot.GearDrops;
import com.minecraftmode.loot.GearIndex;
import com.minecraftmode.loot.GearUpgrades;
import com.minecraftmode.loot.UpgradeMenu;
import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.registry.ModItems;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * 0.4.1 gear care, run inside {@code EconomyClientGameTest}: buying back what was sold to a shop (exact stack, 1.5×, panel
 * screenshot), rolling armor options into a pending set that is kept or applied (paid with condensed essence), Evolution Ether as
 * enhancement protection, the enhancement glow steps and slot frames, and the character screen's per-source totals.
 */
final class GearCareChecks {
	static void run(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.getInventory().clearContent();
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.WARRIOR, 3).withProgress(60, 0));
			Wallet.add(player, 100000);
		});
		checkBuyback(context, server, connection);
		checkReroll(context, server, connection);
		checkProtection(server, connection);
		checkGlow(context, server, connection);
		checkCharacter(context, server, connection);
	}

	private static ClassGear warrior(final boolean weapon, final int minLevel) {
		return GearIndex.list().stream().filter(g -> g.job() == JobClass.WARRIOR && g.isWeapon() == weapon && g.level() >= minLevel).findFirst().orElseThrow();
	}

	private static void checkBuyback(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("setblock 0 -60 -3 minecraft_mode:guild_shop");
		context.waitTicks(2);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ItemStack sword = GearIndex.stack(warrior(true, 10));
			sword.set(ModDataComponents.ENHANCEMENT, new Enhancement(12, 0, 0));
			player.getInventory().add(sword);
			player.getInventory().add(new ItemStack(ModItems.ESSENCE, 12));
			new ShopMerchant(player, player.level(), new BlockPos(0, -60, -3), ShopType.GUILD)
				.openTradingScreen(player, Component.translatable(ShopType.GUILD.titleKey()), 1);
		});
		context.waitForScreen(MerchantScreen.class);
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			MerchantMenu menu = (MerchantMenu) player.containerMenu;
			int gearIndex = -1;
			int essenceIndex = -1;
			for (int i = 0; i < menu.getOffers().size(); i++) {
				MerchantOffer offer = menu.getOffers().get(i);
				if (Wallet.value(offer.getResult().getItem()) > 0 && ClassGear.of(offer.getCostA()) != null) {
					gearIndex = i;
				}
				if (Wallet.value(offer.getResult().getItem()) > 0 && offer.getCostA().is(ModItems.ESSENCE)) {
					essenceIndex = i;
				}
			}
			if (gearIndex < 0 || essenceIndex < 0) {
				return "the guild should buy the carried weapon and essence";
			}
			// sell essence twice (one line, merged), then the +12 weapon
			menu.setSelectionHint(essenceIndex);
			menu.tryMoveItems(essenceIndex);
			menu.quickMoveStack(player, 2);
			menu.quickMoveStack(player, 2);
			menu.setSelectionHint(gearIndex);
			menu.tryMoveItems(gearIndex);
			menu.quickMoveStack(player, 2);
			Wallet.deposit(player);
			var sold = Buyback.list(player);
			if (sold.size() != 2) {
				return "two buyback lines expected (essence merged), got " + sold.size();
			}
			Buyback.Entry weapon = sold.getFirst();
			if (Enhancement.level(weapon.stack()) != 12 || weapon.paid() <= 0) {
				return "the newest line should be the +12 weapon with its price, got " + weapon.stack() + " for " + weapon.paid();
			}
			if (!sold.get(1).stack().is(ModItems.ESSENCE) || sold.get(1).stack().getCount() != 12) {
				return "the essence sales should merge into one line of 12, got " + sold.get(1).stack();
			}
			if (weapon.price() != (int) Math.ceil(weapon.paid() * Buyback.MARKUP)) {
				return "buying back costs 1.5x";
			}
			return "";
		});
		require(report.isEmpty(), report);
		context.waitTicks(10);
		context.takeScreenshot("economy_shop_buyback");
		report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			Buyback.Entry weapon = Buyback.list(player).getFirst();
			int before = Wallet.balance(player);
			Buyback.buy(player, 0);
			if (before - Wallet.balance(player) != weapon.price()) {
				return "buying back should take " + weapon.price() + ", took " + (before - Wallet.balance(player));
			}
			boolean back = false;
			for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
				back |= ClassGear.of(stack) != null && Enhancement.level(stack) == 12;
			}
			if (!back || Buyback.list(player).size() != 1) {
				return "the +12 weapon should come back and leave the list";
			}
			player.closeContainer();
			// outside a shop nothing can be bought back
			Buyback.buy(player, 0);
			if (Buyback.list(player).size() != 1) {
				return "buybacks need an open shop";
			}
			// the list keeps the last ten
			for (int i = 0; i < Buyback.LIMIT + 3; i++) {
				recordSale(player, new ItemStack(i % 2 == 0 ? ModItems.ESSENCE : ModItems.CONDENSED_ESSENCE), 1);
			}
			return Buyback.list(player).size() == Buyback.LIMIT ? "" : "the list should keep " + Buyback.LIMIT + ", has " + Buyback.list(player).size();
		});
		require(report.isEmpty(), report);
		server.runCommand("setblock 0 -60 -3 minecraft:air");
		context.waitTicks(2);
	}

	/** Records a sale the way a shop trade does (alternating items so lines do not merge). */
	static void recordSale(final ServerPlayer player, final ItemStack stack, final int paid) {
		MerchantOffer offer = new MerchantOffer(new ItemCost(stack.getItem(), stack.getCount()), new ItemStack(ModItems.COPPER_COIN, paid), 1, 0, 0.0F);
		Buyback.beforeTrade(player, offer, stack.copy(), ItemStack.EMPTY);
	}

	private static void checkReroll(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new UpgradeMenu(id, inventory, null), Component.translatable("container.minecraft_mode.upgrade")));
		});
		context.waitForScreen(UpgradeScreen.class);
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			UpgradeMenu menu = (UpgradeMenu) player.containerMenu;
			Inventory inventory = player.getInventory();
			ClassGear armor = warrior(false, 60);
			ItemStack piece = GearDrops.create(armor, RandomSource.create(5));
			GearRolls original = piece.get(ModDataComponents.GEAR_ROLLS);
			if (original == null || original.lines().size() != 3) {
				return "a Lv 60+ piece should drop with 3 options, got " + original;
			}
			menu.slots.get(0).set(piece);
			if (menu.clickMenuButton(player, UpgradeMenu.BUTTON_REROLL)) {
				return "rolling should need condensed essence";
			}
			inventory.add(new ItemStack(ModItems.CONDENSED_ESSENCE, GearUpgrades.REROLL_CONDENSED));
			int wallet = Wallet.balance(player);
			if (!menu.clickMenuButton(player, UpgradeMenu.BUTTON_REROLL)) {
				return "rolling with 3 condensed essence should work";
			}
			ItemStack rolled = menu.input();
			GearRolls pending = rolled.get(ModDataComponents.GEAR_ROLLS_PENDING);
			if (pending == null || !original.equals(rolled.get(ModDataComponents.GEAR_ROLLS))) {
				return "the roll should wait beside the old options";
			}
			if (JobProgression.count(inventory, ModItems.CONDENSED_ESSENCE) != 0 || wallet - Wallet.balance(player) != GearUpgrades.rerollCost(armor)) {
				return "a roll costs " + GearUpgrades.REROLL_CONDENSED + " condensed essence and " + GearUpgrades.rerollCost(armor) + " coins";
			}
			if (!menu.clickMenuButton(player, UpgradeMenu.BUTTON_KEEP) || menu.input().has(ModDataComponents.GEAR_ROLLS_PENDING)
				|| !original.equals(menu.input().get(ModDataComponents.GEAR_ROLLS))) {
				return "keeping should drop the new roll and keep the old options";
			}
			// loose essence pays too (27 = 3 condensed)
			inventory.add(new ItemStack(ModItems.ESSENCE, 27));
			if (!menu.clickMenuButton(player, UpgradeMenu.BUTTON_REROLL)) {
				return "27 loose essence should pay a roll";
			}
			GearRolls second = menu.input().get(ModDataComponents.GEAR_ROLLS_PENDING);
			if (!menu.clickMenuButton(player, UpgradeMenu.BUTTON_APPLY) || !second.equals(menu.input().get(ModDataComponents.GEAR_ROLLS))
				|| menu.input().has(ModDataComponents.GEAR_ROLLS_PENDING)) {
				return "applying should take the new options";
			}
			// leave a pending roll for the screenshot
			inventory.add(new ItemStack(ModItems.CONDENSED_ESSENCE, GearUpgrades.REROLL_CONDENSED * 2));
			menu.clickMenuButton(player, UpgradeMenu.BUTTON_REROLL);
			menu.broadcastChanges();
			return "";
		});
		require(report.isEmpty(), report);
		context.runOnClient(minecraft -> {
			if (minecraft.gui.screen() instanceof UpgradeScreen screen) {
				screen.showOptions();
			}
		});
		context.waitTicks(10);
		context.takeScreenshot("economy_upgrade_options");
		server.runOnServer(s -> connection.getServerPlayer().closeContainer());
		context.waitTicks(2);
	}

	private static void checkProtection(final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			Inventory inventory = player.getInventory();
			inventory.clearContent();
			ClassGear gear = warrior(true, 30);
			if (EnhanceMenu.canProtect(player, gear)) {
				return "nothing to protect with yet";
			}
			inventory.add(EvolutionEtherItem.of(gear.bracket() - 10, EnhanceMenu.PROTECTION_ETHER));
			if (EnhanceMenu.canProtect(player, gear)) {
				return "ether below the piece's bracket should not protect it";
			}
			inventory.add(EvolutionEtherItem.of(gear.bracket(), EnhanceMenu.PROTECTION_ETHER - 1));
			if (EnhanceMenu.canProtect(player, gear)) {
				return EnhanceMenu.PROTECTION_ETHER - 1 + " ether should not be enough";
			}
			inventory.add(EvolutionEtherItem.of(gear.bracket() + 10, 1));
			if (!EnhanceMenu.canProtect(player, gear) || EnhanceMenu.protectionEther(inventory, gear) != EnhanceMenu.PROTECTION_ETHER) {
				return "ether of the bracket or higher should protect";
			}
			inventory.clearContent();
			inventory.add(new ItemStack(ModItems.PROTECTION_SCROLL));
			return EnhanceMenu.canProtect(player, gear) ? "" : "a scroll still protects";
		});
		require(report.isEmpty(), report);
	}

	private static void checkGlow(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		ClassGear gear = warrior(true, 10);
		int[][] steps = {{9, 0, 0}, {10, 0, 1}, {13, 0, 2}, {15, 0, 3}, {15, 2, 4}};
		for (int[] step : steps) {
			ItemStack stack = GearIndex.stack(gear);
			stack.set(ModDataComponents.ENHANCEMENT, new Enhancement(step[0], 0, step[1]));
			require(Enhancement.glow(stack) == step[2], "+" + step[0] + " awakened " + step[1] + " should glow " + step[2] + ", got " + Enhancement.glow(stack));
			require(stack.hasFoil() == (step[2] > 0), "+" + step[0] + " glint");
		}
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			Inventory inventory = player.getInventory();
			inventory.clearContent();
			for (int i = 0; i < steps.length; i++) {
				ItemStack stack = GearIndex.stack(gear);
				stack.set(ModDataComponents.ENHANCEMENT, new Enhancement(steps[i][0], 0, steps[i][1]));
				inventory.setItem(i, stack);
			}
			// a full set at +15 for the ring at the feet
			for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
				ClassGear piece = GearIndex.list().stream().filter(g -> g.job() == JobClass.WARRIOR && !g.isWeapon() && g.slot().equipmentSlot() == slot)
					.findFirst().orElseThrow();
				ItemStack stack = GearIndex.stack(piece);
				stack.set(ModDataComponents.ENHANCEMENT, new Enhancement(15, 0, 0));
				player.setItemSlot(slot, stack);
			}
			inventory.setSelectedSlot(3);
		});
		// the client owns the selected slot; look from behind to see the weapon particles and the ring at the feet
		context.runOnClient(minecraft -> {
			minecraft.player.getInventory().setSelectedSlot(3);
			minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		});
		context.waitTicks(30);
		context.takeScreenshot("economy_enhance_glow_world");
		context.runOnClient(minecraft -> minecraft.options.setCameraType(CameraType.FIRST_PERSON));
		context.runOnClient(minecraft -> minecraft.gui.setScreen(new InventoryScreen(minecraft.player)));
		context.waitTicks(10);
		context.takeScreenshot("economy_enhance_frames");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
		context.waitTicks(2);
	}

	private static void checkCharacter(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			GearStats.invalidate(player);
			Map<EngraveStat, Map<GearStats.Source, Float>> breakdown = GearStats.breakdown(player);
			var totals = GearStats.of(player);
			if (breakdown.isEmpty()) {
				return "a Lv 60 warrior in +15 armor should have stats";
			}
			for (Map.Entry<EngraveStat, Map<GearStats.Source, Float>> entry : breakdown.entrySet()) {
				float sum = 0.0F;
				for (float v : entry.getValue().values()) {
					sum += v;
				}
				float expected = entry.getKey().cap() > 0.0F ? Math.min(sum, entry.getKey().cap()) : sum;
				if (Math.abs(expected - totals.get(entry.getKey())) > 0.01F) {
					return entry.getKey() + ": sources add up to " + sum + " but the total is " + totals.get(entry.getKey());
				}
			}
			return "";
		});
		require(report.isEmpty(), report);
		context.runOnClient(minecraft -> minecraft.gui.setScreen(new CharacterScreen()));
		context.waitForScreen(CharacterScreen.class);
		context.waitTicks(5);
		context.takeScreenshot("economy_character");
		context.runOnClient(minecraft -> {
			if (minecraft.gui.screen() instanceof CharacterScreen screen) {
				screen.scrollToEnd();
			}
		});
		context.waitTicks(2);
		context.takeScreenshot("economy_character_scrolled");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
		context.waitTicks(2);
		MinecraftMode.LOGGER.info("[economy] gear care checks passed");
	}

	private static void require(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private GearCareChecks() {
	}
}
