package com.minecraftmode.client.map;

import com.minecraftmode.city.CityZone;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * The minimap in the top-right corner: explored terrain around the player (north up), other players, waypoints, the capital,
 * the player's own arrow and the coordinates underneath.
 */
final class MinimapHud {
	private static final int MARGIN = 6;

	static void extract(final GuiGraphicsExtractor g, final DeltaTracker delta) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (!MapSettings.minimap || player == null || minecraft.level == null || minecraft.gui.screen() instanceof MapScreen) {
			return;
		}
		int size = MapSettings.sizePx();
		float zoom = MapSettings.zoomFactor();
		int x = g.guiWidth() - size - MARGIN;
		int y = MARGIN;
		Font font = minecraft.font;
		g.fill(x - 2, y - 2, x + size + 2, y + size + 2, 0xC0101010);
		g.fill(x, y, x + size, y + size, 0xFF1A1A1A);
		g.enableScissor(x, y, x + size, y + size);
		MapClient.NEAR.draw(g, x, y, size, size, player.getX(), player.getZ(), zoom);
		Markers.draw(g, font, player, x, y, size, size, player.getX(), player.getZ(), zoom, false);
		g.disableScissor();
		g.outline(x - 2, y - 2, size + 4, size + 4, 0xFF6A6A6A);
		g.centeredText(font, "N", x + size / 2, y - 1, 0xFFFFFFFF);
		String coords = player.getBlockX() + ", " + player.getBlockY() + ", " + player.getBlockZ();
		g.centeredText(font, coords, x + size / 2, y + size + 4, 0xFFE0E0E0);
		String place = place(player);
		if (place != null) {
			g.centeredText(font, place, x + size / 2, y + size + 14, 0xFFFFD27F);
		}
	}

	/** A short name for where the player is (the capital, a dimension), null in the open overworld. */
	private static String place(final Player player) {
		if (CityZone.isCityLevel(player.level()) && CityZone.inside(player.blockPosition())) {
			return Component.translatable("screen.minecraft_mode.map.capital").getString();
		}
		return null;
	}

	private MinimapHud() {
	}
}
