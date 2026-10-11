package com.minecraftmode.client.travel;

import com.minecraftmode.network.OpenTravelPayload;
import com.minecraftmode.network.TravelPayload;
import com.minecraftmode.travel.Destination;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The waymaster's list of places: one button per {@link Destination} offered here; a click asks the server for the trip. */
public class TravelScreen extends Screen {
	private static final int W = 200;
	private static final int ROW = 24;
	private static final int TOP = 26;

	private final OpenTravelPayload travel;
	private int left;
	private int top;

	public TravelScreen(final OpenTravelPayload travel) {
		super(Component.translatable("screen.minecraft_mode.travel.title"));
		this.travel = travel;
	}

	private int height() {
		return TOP + Math.max(1, this.travel.destinations().size()) * ROW + 30;
	}

	@Override
	protected void init() {
		this.left = (this.width - W) / 2;
		this.top = (this.height - this.height()) / 2;
		for (int i = 0; i < this.travel.destinations().size(); i++) {
			Destination destination = Destination.byOrdinal(this.travel.destinations().get(i));
			if (destination == null) {
				continue;
			}
			this.addRenderableWidget(Button.builder(Component.translatable(destination.nameKey()), b -> {
				ClientPlayNetworking.send(new TravelPayload(this.travel.entityId(), destination.ordinal()));
				this.onClose();
			}).bounds(this.left + 10, this.top + TOP + i * ROW, W - 20, 20).build());
		}
		this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> this.onClose())
			.bounds(this.left + W - 70, this.top + this.height() - 26, 60, 18).build());
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor g, final int mouseX, final int mouseY, final float a) {
		int h = this.height();
		g.fill(this.left, this.top, this.left + W, this.top + h, 0xE8101820);
		g.outline(this.left, this.top, W, h, 0xFF3AA8C8);
		g.text(this.font, this.title.copy().withStyle(ChatFormatting.AQUA), this.left + 10, this.top + 9, 0xFFFFFFFF, true);
		if (this.travel.destinations().isEmpty()) {
			g.text(this.font, Component.translatable("screen.minecraft_mode.travel.none").withStyle(ChatFormatting.GRAY), this.left + 12, this.top + TOP + 6, 0xFFFFFFFF, false);
		}
		super.extractRenderState(g, mouseX, mouseY, a);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
