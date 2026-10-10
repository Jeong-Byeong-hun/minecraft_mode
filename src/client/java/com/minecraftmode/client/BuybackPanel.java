package com.minecraftmode.client;

import com.minecraftmode.economy.Buyback;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.network.BuybackPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.sounds.SoundEvents;

/**
 * Beside a shop's trade window: the player's last sales ({@link Buyback}, newest first) as a 2 × 5 grid. Hovering shows the item
 * and the buyback price ({@link Buyback#MARKUP} × what the shop paid); a click buys it back. Only shop blocks (their titles), not
 * villagers.
 */
public final class BuybackPanel {
	private static final String SHOP_TITLE = "container.minecraft_mode.shop.";
	/** Half the merchant window's width. */
	private static final int HALF_WIDTH = 138;
	private static final int COLUMNS = 2;
	private static final int CELL = 18;
	private static final int HEADER = 11;
	private static final int WIDTH = COLUMNS * CELL + 4;

	public static void init() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!isShop(screen)) {
				return;
			}
			ScreenEvents.afterExtract(screen).register((s, g, mouseX, mouseY, delta) -> {
				Minecraft minecraft = Minecraft.getInstance();
				if (minecraft.player == null) {
					return;
				}
				List<Buyback.Entry> entries = Buyback.list(minecraft.player);
				int x = left(s);
				int y = top(s);
				if (x < 0) {
					return;
				}
				int rows = (Buyback.LIMIT + COLUMNS - 1) / COLUMNS;
				g.fill(x, y, x + WIDTH, y + HEADER + rows * CELL + 2, 0xC0101010);
				g.centeredText(minecraft.font, Component.translatable("screen.minecraft_mode.buyback.title"), x + WIDTH / 2, y + 2, 0xFFFFD27F);
				int wallet = Coins.total(minecraft.player);
				for (int i = 0; i < Buyback.LIMIT; i++) {
					int cx = cellX(x, i);
					int cy = cellY(y, i);
					Buyback.Entry entry = i < entries.size() ? entries.get(i) : null;
					boolean hover = mouseX >= cx && mouseY >= cy && mouseX < cx + CELL && mouseY < cy + CELL;
					int color = entry == null ? 0xFF2A2A2A : entry.price() > wallet && !minecraft.player.isCreative() ? 0xFF5A1E1E : hover ? 0xFF5A5A5A : 0xFF373737;
					g.fill(cx, cy, cx + CELL - 1, cy + CELL - 1, color);
					if (entry == null) {
						continue;
					}
					g.item(entry.stack(), cx, cy);
					g.itemDecorations(minecraft.font, entry.stack(), cx, cy);
					if (hover) {
						List<Component> tip = new ArrayList<>(Screen.getTooltipFromItem(minecraft, entry.stack()));
						tip.add(Component.translatable("screen.minecraft_mode.buyback.price", Coins.format(entry.price()), Coins.format(entry.paid()))
							.withStyle(entry.price() > wallet ? ChatFormatting.RED : ChatFormatting.GOLD));
						tip.add(Component.translatable("screen.minecraft_mode.buyback.click").withStyle(ChatFormatting.DARK_GRAY));
						g.setComponentTooltipForNextFrame(minecraft.font, tip, mouseX, mouseY);
					}
				}
			});
			ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> {
				Minecraft minecraft = Minecraft.getInstance();
				int x = left(s);
				if (x < 0 || minecraft.player == null) {
					return true;
				}
				int y = top(s);
				List<Buyback.Entry> entries = Buyback.list(minecraft.player);
				for (int i = 0; i < entries.size(); i++) {
					int cx = cellX(x, i);
					int cy = cellY(y, i);
					if (event.x() >= cx && event.y() >= cy && event.x() < cx + CELL && event.y() < cy + CELL) {
						minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
						ClientPlayNetworking.send(new BuybackPayload(i));
						return false;
					}
				}
				return true;
			});
		});
	}

	private static boolean isShop(final Screen screen) {
		return screen instanceof MerchantScreen && screen.getTitle().getContents() instanceof TranslatableContents title && title.getKey().startsWith(SHOP_TITLE);
	}

	/** Right of the trade window, or left of it when the screen is too narrow; -1 when neither fits. */
	private static int left(final Screen screen) {
		int right = screen.width / 2 + HALF_WIDTH + 4;
		if (right + WIDTH <= screen.width) {
			return right;
		}
		int left = screen.width / 2 - HALF_WIDTH - 4 - WIDTH;
		return left >= 0 ? left : -1;
	}

	private static int top(final Screen screen) {
		return screen.height / 2 - 83;
	}

	private static int cellX(final int x, final int i) {
		return x + 2 + i % COLUMNS * CELL;
	}

	private static int cellY(final int y, final int i) {
		return y + HEADER + i / COLUMNS * CELL;
	}

	private BuybackPanel() {
	}
}
