package com.minecraftmode.client.map;

import com.minecraftmode.city.CityZone;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Draws the things on top of the terrain: waypoints, other players, the capital, and the player's own arrow. */
final class Markers {
	/**
	 * @param labels draw names next to markers (the big map); the minimap shows dots only
	 */
	static void draw(final GuiGraphicsExtractor g, final Font font, final LocalPlayer player, final int x, final int y, final int w, final int h,
		final double centerX, final double centerZ, final float zoom, final boolean labels) {
		int cx = x + w / 2;
		int cy = y + h / 2;
		// the capital
		if (CityZone.isCityLevel(player.level())) {
			int sx = cx + (int)Math.round((0.5 - centerX) * zoom);
			int sy = cy + (int)Math.round((0.5 - centerZ) * zoom);
			if (inside(sx, sy, x, y, w, h)) {
				g.fill(sx - 2, sy - 2, sx + 3, sy + 3, 0xFFFFD040);
				g.outline(sx - 3, sy - 3, 7, 7, 0xFF000000);
				if (labels) {
					g.text(font, Component.translatable("screen.minecraft_mode.map.capital"), sx + 5, sy - 4, 0xFFFFD040, true);
				}
			}
		}
		// waypoints
		for (Waypoints.Waypoint wp : MapClient.currentWaypoints()) {
			int sx = cx + (int)Math.round((wp.x + 0.5 - centerX) * zoom);
			int sy = cy + (int)Math.round((wp.z + 0.5 - centerZ) * zoom);
			if (!inside(sx, sy, x, y, w, h)) {
				if (labels) {
					continue;
				}
				// off the minimap: pinned to the edge so the direction is still visible
				sx = Mth.clamp(sx, x + 2, x + w - 3);
				sy = Mth.clamp(sy, y + 2, y + h - 3);
			}
			if (wp.isDeath()) {
				g.fill(sx - 1, sy - 3, sx + 2, sy + 4, 0xFFFFFFFF);
				g.fill(sx - 3, sy - 1, sx + 4, sy + 2, 0xFFFFFFFF);
				g.fill(sx - 1, sy - 2, sx + 2, sy + 3, wp.color);
				g.fill(sx - 2, sy - 1, sx + 3, sy + 2, wp.color);
			} else {
				g.fill(sx - 2, sy - 2, sx + 3, sy + 3, 0xFF000000);
				g.fill(sx - 1, sy - 1, sx + 2, sy + 2, wp.color);
			}
			if (labels) {
				String name = wp.isDeath() ? Component.translatable("screen.minecraft_mode.map.death").getString() : wp.name;
				g.text(font, name, sx + 5, sy - 4, 0xFFFFFFFF, true);
			}
		}
		// other players
		for (Player other : player.level().players()) {
			if (other == player || other.isInvisible()) {
				continue;
			}
			int sx = cx + (int)Math.round((other.getX() - centerX) * zoom);
			int sy = cy + (int)Math.round((other.getZ() - centerZ) * zoom);
			if (inside(sx, sy, x, y, w, h)) {
				g.fill(sx - 2, sy - 2, sx + 3, sy + 3, 0xFF000000);
				g.fill(sx - 1, sy - 1, sx + 2, sy + 2, 0xFFFFFFFF);
				if (labels) {
					g.text(font, other.getPlainTextName(), sx + 5, sy - 4, 0xFFFFFFFF, true);
				}
			}
		}
		// the player: a dot with a short line in the facing direction
		int px = cx + (int)Math.round((player.getX() - centerX) * zoom);
		int py = cy + (int)Math.round((player.getZ() - centerZ) * zoom);
		if (inside(px, py, x, y, w, h)) {
			double yaw = Math.toRadians(player.getYRot());
			double dx = -Math.sin(yaw);
			double dz = Math.cos(yaw);
			for (int i = 1; i <= 5; i++) {
				int lx = px + (int)Math.round(dx * i);
				int ly = py + (int)Math.round(dz * i);
				g.fill(lx, ly, lx + 1, ly + 1, 0xFFFFFFFF);
			}
			g.fill(px - 2, py - 2, px + 3, py + 3, 0xFF000000);
			g.fill(px - 1, py - 1, px + 2, py + 2, 0xFF40FF40);
		}
	}

	private static boolean inside(final int sx, final int sy, final int x, final int y, final int w, final int h) {
		return sx >= x && sy >= y && sx < x + w && sy < y + h;
	}

	private Markers() {
	}
}
