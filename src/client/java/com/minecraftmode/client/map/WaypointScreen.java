package com.minecraftmode.client.map;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Names (and colours) a new waypoint at a block, or edits/deletes an existing one; returns to the map. */
public class WaypointScreen extends Screen {
	private static final int W = 200;
	private static final int H = 96;

	private final MapScreen parent;
	private final Waypoints.@Nullable Waypoint existing;
	private final int x;
	private final int y;
	private final int z;
	private int color;
	private EditBox name;
	private int left;
	private int top;

	public WaypointScreen(final MapScreen parent, final Waypoints.@Nullable Waypoint existing, final int x, final int y, final int z) {
		super(Component.translatable(existing == null ? "screen.minecraft_mode.map.new_waypoint" : "screen.minecraft_mode.map.edit_waypoint"));
		this.parent = parent;
		this.existing = existing;
		this.x = x;
		this.y = y;
		this.z = z;
		this.color = existing == null ? Waypoints.COLORS[MapClient.currentWaypoints().size() % Waypoints.COLORS.length] : existing.color;
	}

	@Override
	protected void init() {
		this.left = (this.width - W) / 2;
		this.top = (this.height - H) / 2;
		this.name = new EditBox(this.font, this.left + 8, this.top + 26, W - 16, 18, Component.translatable("screen.minecraft_mode.map.name"));
		this.name.setMaxLength(24);
		this.name.setValue(this.existing == null ? Component.translatable("screen.minecraft_mode.map.default_name", MapClient.currentWaypoints().size() + 1).getString()
			: this.existing.isDeath() ? Component.translatable("screen.minecraft_mode.map.death").getString() : this.existing.name);
		this.addRenderableWidget(this.name);
		this.setInitialFocus(this.name);
		this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.map.color"), b -> {
			int i = 0;
			while (i < Waypoints.COLORS.length && Waypoints.COLORS[i] != this.color) {
				i++;
			}
			this.color = Waypoints.COLORS[(i + 1) % Waypoints.COLORS.length];
		}).bounds(this.left + 8, this.top + 50, 60, 18).build());
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.save()).bounds(this.left + W - 68, this.top + H - 24, 60, 18).build());
		if (this.existing != null) {
			this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.map.delete"), b -> {
				MapClient.waypoints().remove(MapClient.dimensionKey(), this.existing);
				this.onClose();
			}).bounds(this.left + 8, this.top + H - 24, 60, 18).build());
		}
		this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> this.onClose()).bounds(this.left + W - 132, this.top + H - 24, 60, 18).build());
	}

	private void save() {
		String text = this.name.getValue().trim();
		if (text.isEmpty()) {
			text = Component.translatable("screen.minecraft_mode.map.default_name", MapClient.currentWaypoints().size() + 1).getString();
		}
		if (this.existing != null) {
			if (!this.existing.isDeath()) {
				this.existing.name = text;
			}
			this.existing.color = this.color;
			MapClient.waypoints().touch();
		} else {
			MapClient.waypoints().add(MapClient.dimensionKey(), new Waypoints.Waypoint(text, this.x, this.y, this.z, this.color));
		}
		MapClient.save();
		this.onClose();
	}

	@Override
	public boolean keyPressed(final KeyEvent event) {
		if (event.isConfirmation()) {
			this.save();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor g, final int mouseX, final int mouseY, final float a) {
		g.fill(this.left, this.top, this.left + W, this.top + H, 0xF0101018);
		g.outline(this.left, this.top, W, H, 0xFF4AA8E8);
		g.text(this.font, this.title, this.left + 8, this.top + 8, 0xFFFFFFFF, true);
		String where = this.x + ", " + this.y + ", " + this.z;
		g.text(this.font, where, this.left + W - 8 - this.font.width(where), this.top + 8, 0xFFA0A0A0, false);
		g.fill(this.left + 74, this.top + 52, this.left + 88, this.top + 66, 0xFF000000);
		g.fill(this.left + 75, this.top + 53, this.left + 87, this.top + 65, this.color);
		super.extractRenderState(g, mouseX, mouseY, a);
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
