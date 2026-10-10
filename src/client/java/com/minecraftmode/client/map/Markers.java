package com.minecraftmode.client.map;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Draws the things on top of the terrain: waypoints, other players, the capital and its NPCs, and the player's own arrow. */
final class Markers {
	/** From this zoom on the big map names every NPC; below it only the hovered and the selected one are named. */
	static final float NPC_LABEL_ZOOM = 8.0F;
	/** How close (in screen pixels) the mouse must be to a marker to hover it. */
	private static final int HOVER_PX = 5;

	/**
	 * @param labels draw names next to markers (the big map); the minimap shows dots only
	 * @param selected the NPC picked in the big map's list (ringed and always named), or null
	 */
	static void draw(final GuiGraphicsExtractor g, final Font font, final LocalPlayer player, final int x, final int y, final int w, final int h,
		final double centerX, final double centerZ, final float zoom, final boolean labels, final CityPlaces.Place selected) {
		int cx = x + w / 2;
		int cy = y + h / 2;
		// the capital
		if (CityPlaces.shown(player.level())) {
			int sx = cx + (int)Math.round((0.5 - centerX) * zoom);
			int sy = cy + (int)Math.round((0.5 - centerZ) * zoom);
			if (inside(sx, sy, x, y, w, h)) {
				g.fill(sx - 2, sy - 2, sx + 3, sy + 3, 0xFFFFD040);
				g.outline(sx - 3, sy - 3, 7, 7, 0xFF000000);
				if (labels && zoom < NPC_LABEL_ZOOM) {
					g.text(font, Component.translatable("screen.minecraft_mode.map.capital"), sx + 5, sy - 4, 0xFFFFD040, true);
				}
			}
			// its NPCs, trainers and the Training Grounds stairs
			List<CityPlaces.Place> named = new ArrayList<>();
			for (CityPlaces.Place place : CityPlaces.all()) {
				int px = cx + (int)Math.round((place.x() + 0.5 - centerX) * zoom);
				int py = cy + (int)Math.round((place.z() + 0.5 - centerZ) * zoom);
				if (!inside(px, py, x, y, w, h)) {
					continue;
				}
				icon(g, place, px, py);
				if (place.equals(selected)) {
					int pulse = 5 + (int)(System.currentTimeMillis() / 150 % 3);
					g.outline(px - pulse, py - pulse, pulse * 2 + 1, pulse * 2 + 1, 0xFFFFFF60);
					named.addFirst(place);
				} else if (labels && zoom >= NPC_LABEL_ZOOM) {
					named.add(place);
				}
			}
			if (labels) {
				// names under the marker, or above it when that spot is taken; a name with no free spot is left to the hover tooltip
				List<int[]> taken = new ArrayList<>();
				for (CityPlaces.Place place : named) {
					int px = cx + (int)Math.round((place.x() + 0.5 - centerX) * zoom);
					int py = cy + (int)Math.round((place.z() + 0.5 - centerZ) * zoom);
					int half = font.width(place.name()) / 2 + 2;
					for (int ty : new int[] {py + 5, py - 14}) {
						int[] box = {px - half, ty, px + half, ty + 10};
						if (taken.stream().noneMatch(other -> box[0] < other[2] && other[0] < box[2] && box[1] < other[3] && other[1] < box[3])) {
							taken.add(box);
							g.centeredText(font, place.name(), px, ty + 1, place.equals(selected) ? 0xFFFFFF60 : 0xFFFFFFFF);
							break;
						}
					}
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

	/** NPCs are round dots, trainers diamonds and landmarks squares, each in its own color with a black rim. */
	private static void icon(final GuiGraphicsExtractor g, final CityPlaces.Place place, final int px, final int py) {
		int color = place.color();
		switch (place.kind()) {
			case NPC -> {
				g.fill(px - 2, py - 3, px + 3, py + 4, 0xFF000000);
				g.fill(px - 3, py - 2, px + 4, py + 3, 0xFF000000);
				g.fill(px - 1, py - 2, px + 2, py + 3, color);
				g.fill(px - 2, py - 1, px + 3, py + 2, color);
			}
			case TRAINER -> {
				for (int dy = -4; dy <= 4; dy++) {
					int r = 4 - Math.abs(dy);
					g.fill(px - r, py + dy, px + r + 1, py + dy + 1, 0xFF000000);
					if (r >= 1 && Math.abs(dy) <= 3) {
						g.fill(px - r + 1, py + dy, px + r, py + dy + 1, color);
					}
				}
			}
			case LANDMARK -> {
				g.fill(px - 3, py - 3, px + 4, py + 4, 0xFF000000);
				g.fill(px - 2, py - 2, px + 3, py + 3, color);
				g.fill(px - 2, py, px + 3, py + 1, 0xFF000000);
			}
		}
	}

	/** The capital marker under the mouse on a map view, or null (only in the city's dimension). */
	static CityPlaces.Place placeAt(final LocalPlayer player, final int mouseX, final int mouseY, final int x, final int y, final int w, final int h,
		final double centerX, final double centerZ, final float zoom) {
		if (!CityPlaces.shown(player.level())) {
			return null;
		}
		int cx = x + w / 2;
		int cy = y + h / 2;
		CityPlaces.Place best = null;
		int bestDist = HOVER_PX * HOVER_PX + 1;
		for (CityPlaces.Place place : CityPlaces.all()) {
			int px = cx + (int)Math.round((place.x() + 0.5 - centerX) * zoom);
			int py = cy + (int)Math.round((place.z() + 0.5 - centerZ) * zoom);
			int dist = Mth.square(px - mouseX) + Mth.square(py - mouseY);
			if (dist < bestDist) {
				bestDist = dist;
				best = place;
			}
		}
		return best;
	}

	private static boolean inside(final int sx, final int sy, final int x, final int y, final int w, final int h) {
		return sx >= x && sy >= y && sx < x + w && sy < y + h;
	}

	private Markers() {
	}
}
