package com.minecraftmode.client;

import com.minecraftmode.economy.Wallet;
import com.minecraftmode.loot.Coins;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;

/** Shows the wallet balance just above the shop window and the inventory (coins have no slots). */
public final class WalletDisplay {
	public static void init() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			int halfWidth;
			if (screen instanceof MerchantScreen) {
				halfWidth = 138;
			} else if (screen instanceof InventoryScreen) {
				halfWidth = 88;
			} else {
				return;
			}
			ScreenEvents.afterExtract(screen).register((s, g, mouseX, mouseY, delta) -> {
				Minecraft minecraft = Minecraft.getInstance();
				if (minecraft.player == null) {
					return;
				}
				Component text = Component.translatable("screen.minecraft_mode.wallet", Coins.format(Wallet.balance(minecraft.player)));
				g.text(minecraft.font, text, s.width / 2 - halfWidth, s.height / 2 - 83 - 11, 0xFFFFD27F, true);
			});
		});
	}

	private WalletDisplay() {
	}
}
