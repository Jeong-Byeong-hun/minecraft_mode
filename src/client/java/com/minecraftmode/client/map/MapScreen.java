package com.minecraftmode.client.map;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The big map (M): everything explored in this dimension, dragged with the mouse and zoomed with the wheel. Right-click puts a
 * waypoint where you clicked; the panel on the right lists the waypoints (click to look there, edit to rename or delete). The
 * buttons also control the minimap (on/off, size, zoom).
 */
public class MapScreen extends Screen {
	private static final int PANEL_W = 110;
	private static final int BAR_H = 24;
	private static final float MIN_ZOOM = 0.5F;
	private static final float MAX_ZOOM = 8.0F;
	private static final int ROW = 12;

	private static double centerX;
	private static double centerZ;
	private static float zoom = 2.0F;
	private static boolean centered;

	private int listScroll;
	private double dragX;
	private double dragZ;
	private boolean dragging;
	private boolean moved;

	public MapScreen() {
		super(Component.translatable("screen.minecraft_mode.map.title"));
	}

	public double centerX() {
		return centerX;
	}

	public double centerZ() {
		return centerZ;
	}

	private int mapW() {
		return this.width - PANEL_W;
	}

	private int mapH() {
		return this.height - BAR_H;
	}

	@Override
	protected void init() {
		LocalPlayer player = this.minecraft.player;
		if (!centered && player != null) {
			centerX = player.getX();
			centerZ = player.getZ();
			centered = true;
		}
		int x = 4;
		int y = this.height - BAR_H + 3;
		this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.map.me"), b -> this.center()).bounds(x, y, 58, 18).build());
		x += 62;
		this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.map.add"), b -> this.addHere()).bounds(x, y, 78, 18).build());
		x += 82;
		this.addRenderableWidget(Button.builder(minimapLabel(), b -> {
			MapSettings.minimap = !MapSettings.minimap;
			MapSettings.save();
			b.setMessage(minimapLabel());
		}).bounds(x, y, 78, 18).build());
		x += 82;
		this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.map.size", MapSettings.sizePx()), b -> {
			MapSettings.size = (MapSettings.size + 1) % MapSettings.SIZES.length;
			MapSettings.save();
			b.setMessage(Component.translatable("screen.minecraft_mode.map.size", MapSettings.sizePx()));
		}).bounds(x, y, 70, 18).build());
		x += 74;
		this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.map.zoom", MapSettings.zoomFactor()), b -> {
			MapSettings.zoom = (MapSettings.zoom + 1) % MapSettings.ZOOMS.length;
			MapSettings.save();
			b.setMessage(Component.translatable("screen.minecraft_mode.map.zoom", MapSettings.zoomFactor()));
		}).bounds(x, y, 70, 18).build());
		this.addRenderableWidget(Button.builder(Component.literal("-"), b -> this.zoomBy(0.5F)).bounds(this.mapW() - 44, 4, 18, 18).build());
		this.addRenderableWidget(Button.builder(Component.literal("+"), b -> this.zoomBy(2.0F)).bounds(this.mapW() - 22, 4, 18, 18).build());
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(this.width - 64, y, 60, 18).build());
	}

	private static Component minimapLabel() {
		return Component.translatable(MapSettings.minimap ? "screen.minecraft_mode.map.minimap_on" : "screen.minecraft_mode.map.minimap_off");
	}

	private void center() {
		LocalPlayer player = this.minecraft.player;
		if (player != null) {
			centerX = player.getX();
			centerZ = player.getZ();
		}
	}

	private void addHere() {
		LocalPlayer player = this.minecraft.player;
		if (player != null) {
			this.minecraft.gui.setScreen(new WaypointScreen(this, null, player.getBlockX(), player.getBlockY(), player.getBlockZ()));
		}
	}

	private void zoomBy(final float factor) {
		zoom = Mth.clamp(zoom * factor, MIN_ZOOM, MAX_ZOOM);
	}

	/** Visible waypoints of the list panel. */
	private List<Waypoints.Waypoint> rows() {
		List<Waypoints.Waypoint> all = MapClient.currentWaypoints();
		int perPage = this.listRows();
		this.listScroll = Mth.clamp(this.listScroll, 0, Math.max(0, all.size() - perPage));
		return all.subList(this.listScroll, Math.min(all.size(), this.listScroll + perPage));
	}

	private int listRows() {
		return Math.max(1, (this.mapH() - 30) / ROW);
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor g, final int mouseX, final int mouseY, final float a) {
		LocalPlayer player = this.minecraft.player;
		int w = this.mapW();
		int h = this.mapH();
		g.fill(0, 0, this.width, this.height, 0xFF101014);
		g.fill(0, 0, w, h, 0xFF1A1A1A);
		if (player != null) {
			g.enableScissor(0, 0, w, h);
			MapClient.VIEW.draw(g, 0, 0, w, h, centerX, centerZ, zoom);
			Markers.draw(g, this.font, player, 0, 0, w, h, centerX, centerZ, zoom, zoom >= 1.0F);
			g.disableScissor();
		}
		// header: title, hovered block, scale
		g.fill(0, 0, w, 22, 0x90000000);
		g.text(this.font, this.title, 6, 7, 0xFFFFFFFF, true);
		if (mouseX < w && mouseY < h) {
			int bx = Mth.floor(centerX + (mouseX - w / 2.0) / zoom);
			int bz = Mth.floor(centerZ + (mouseY - h / 2.0) / zoom);
			g.text(this.font, bx + ", " + bz, 6 + this.font.width(this.title) + 12, 7, 0xFFC0C0C0, true);
		}
		g.text(this.font, Component.translatable("screen.minecraft_mode.map.scale", zoom), w - 48 - this.font.width(Component.translatable("screen.minecraft_mode.map.scale", zoom)), 9,
			0xFFA0A0A0, true);
		g.centeredText(this.font, Component.translatable("screen.minecraft_mode.map.hint").withStyle(ChatFormatting.GRAY), w / 2, h - 11, 0xFFA0A0A0);
		// waypoint panel
		int px = w;
		g.fill(px, 0, this.width, h, 0xFF16161C);
		g.fill(px, 0, px + 1, h, 0xFF4AA8E8);
		g.text(this.font, Component.translatable("screen.minecraft_mode.map.waypoints"), px + 6, 7, 0xFFFFD27F, true);
		List<Waypoints.Waypoint> rows = this.rows();
		int y = 22;
		for (Waypoints.Waypoint wp : rows) {
			boolean hover = mouseX >= px && mouseY >= y && mouseY < y + ROW;
			if (hover) {
				g.fill(px + 2, y - 1, this.width - 2, y + ROW - 1, 0x40FFFFFF);
			}
			g.fill(px + 5, y + 1, px + 10, y + 6, wp.color);
			String name = wp.isDeath() ? Component.translatable("screen.minecraft_mode.map.death").getString() : wp.name;
			int dist = player == null ? 0 : (int)Math.sqrt(Mth.square(wp.x + 0.5 - player.getX()) + Mth.square(wp.z + 0.5 - player.getZ()));
			String right = dist + "m";
			g.text(this.font, this.font.plainSubstrByWidth(name, PANEL_W - 22 - this.font.width(right)), px + 14, y, 0xFFE0E0E0, false);
			g.text(this.font, right, this.width - 6 - this.font.width(right), y, 0xFF909090, false);
			y += ROW;
		}
		if (MapClient.currentWaypoints().isEmpty()) {
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.map.none"), px + 6, 24, PANEL_W - 12, 0xFF808080);
		} else if (MapClient.currentWaypoints().size() > rows.size()) {
			g.centeredText(this.font, "▲▼", px + PANEL_W / 2, h - 11, 0xFF808080);
		}
		super.extractRenderState(g, mouseX, mouseY, a);
	}

	@Override
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		int w = this.mapW();
		int h = this.mapH();
		if (event.x() >= w && event.y() < h) {
			// the list: click looks there, right-click edits
			int index = (int)((event.y() - 22) / ROW);
			List<Waypoints.Waypoint> rows = this.rows();
			if (event.y() >= 22 && index >= 0 && index < rows.size()) {
				Waypoints.Waypoint wp = rows.get(index);
				if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
					this.minecraft.gui.setScreen(new WaypointScreen(this, wp, wp.x, wp.y, wp.z));
				} else {
					centerX = wp.x + 0.5;
					centerZ = wp.z + 0.5;
				}
			}
			return true;
		}
		if (event.x() < w && event.y() < h) {
			if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
				this.dragging = true;
				this.moved = false;
				this.dragX = event.x();
				this.dragZ = event.y();
				return true;
			}
			if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
				int bx = Mth.floor(centerX + (event.x() - w / 2.0) / zoom);
				int bz = Mth.floor(centerZ + (event.y() - h / 2.0) / zoom);
				int by = this.minecraft.player == null ? 64 : this.minecraft.player.getBlockY();
				this.minecraft.gui.setScreen(new WaypointScreen(this, null, bx, by, bz));
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(final MouseButtonEvent event, final double dx, final double dy) {
		if (this.dragging) {
			centerX -= (event.x() - this.dragX) / zoom;
			centerZ -= (event.y() - this.dragZ) / zoom;
			this.dragX = event.x();
			this.dragZ = event.y();
			this.moved = true;
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(final MouseButtonEvent event) {
		this.dragging = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
		if (x >= this.mapW() && y < this.mapH()) {
			this.listScroll -= (int)Math.signum(scrollY);
			return true;
		}
		if (x < this.mapW() && y < this.mapH()) {
			this.zoomBy(scrollY > 0 ? 2.0F : 0.5F);
			return true;
		}
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
