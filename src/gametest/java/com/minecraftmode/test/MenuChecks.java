package com.minecraftmode.test;

import com.minecraftmode.client.ClientSettings;
import com.minecraftmode.client.menu.AdventurerScreen;
import com.minecraftmode.client.raid.RaidClient;
import com.minecraftmode.economy.Wallet;
import com.minecraftmode.network.MenuActionPayload;
import com.minecraftmode.raid.Parties;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The adventurer's menu: a party is made and left through the menu's payload, paying moves coins (never more than the payer has,
 * never to themselves), the settings toggles flip and are saved, and every tab is screenshotted.
 */
final class MenuChecks {
	private static final GameProfile STRANGER = new GameProfile(UUID.nameUUIDFromBytes("MenuStranger".getBytes()), "Stranger");
	private static final int WAIT = 100;
	private static final int ADVENTURER_TABS = 3;

	static void run(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		checkParty(context, server, connection);
		checkPay(server, connection);
		checkSettings(context);
		checkScreens(context);
	}

	private static void checkParty(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		context.runOnClient(mc -> ClientPlayNetworking.send(new MenuActionPayload(MenuActionPayload.PARTY_CREATE)));
		context.waitFor(mc -> !RaidClient.party().isEmpty(), WAIT);
		server.runOnServer(s -> require(Parties.of(connection.getServerPlayer().getUUID()) != null, "the menu made a party"));
		context.runOnClient(mc -> ClientPlayNetworking.send(new MenuActionPayload(MenuActionPayload.PARTY_LEAVE)));
		context.waitFor(mc -> RaidClient.party().isEmpty(), WAIT);
		server.runOnServer(s -> require(Parties.of(connection.getServerPlayer().getUUID()) == null, "and left it"));
	}

	private static void checkPay(final TestServerContext server, final TestServerConnection connection) {
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			FakePlayer friend = FakePlayer.get(player.level(), STRANGER);
			Wallet.add(player, 500);
			int before = Wallet.balance(player);
			require(Wallet.pay(player, friend, 120), "paying works");
			require(Wallet.balance(player) == before - 120 && Wallet.balance(friend) >= 120, "the coins moved");
			require(!Wallet.pay(player, friend, before * 10), "nobody pays more than they have");
			require(!Wallet.pay(player, player, 10), "nor themselves");
		});
	}

	private static void checkSettings(final ClientGameTestContext context) {
		context.runOnClient(mc -> {
			boolean was = ClientSettings.hpBar;
			ClientSettings.setHpBar(!was);
			require(ClientSettings.hpBar == !was, "the HP bar setting flips");
			ClientSettings.setHpBar(was);
		});
	}

	private static void checkScreens(final ClientGameTestContext context) {
		context.setScreen(AdventurerScreen::new);
		for (int tab = 0; tab < ADVENTURER_TABS; tab++) {
			int index = tab;
			context.runOnClient(mc -> ((AdventurerScreen)mc.gui.screen()).showTab(index));
			context.waitTicks(2);
			context.takeScreenshot("menu_adventurer_" + tab);
		}
		context.setScreen(() -> null);
	}


	private static void require(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private MenuChecks() {
	}
}
