package com.minecraftmode.client.residence;

import com.minecraftmode.network.LandActionPayload;
import com.minecraftmode.network.OpenLandPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The land screen (sneak-use a deed, or {@code /land}): the player's plots with a two-click "give up" each, and the players they
 * trust on all of their land, added by name (online players) and removed with ✕. The server answers every action with a fresh
 * {@link OpenLandPayload}, which replaces this screen.
 */
public class LandScreen extends Screen {
	private static final int W = 300;
	private static final int H = 226;
	private static final int ROW = 13;
	private static final int FRIEND_ROWS = 4;
	private static final int FRIEND_COLUMN = 92;
	private static final int PLOTS_Y = 34;
	private static final String K = "screen.minecraft_mode.land.";

	private final OpenLandPayload land;
	private int left;
	private int top;
	private int confirm = -1;

	public LandScreen(final OpenLandPayload land) {
		super(Component.translatable(K + "title"));
		this.land = land;
	}

	@Override
	protected void init() {
		this.left = (this.width - W) / 2;
		this.top = Math.max(2, (this.height - H) / 2);
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(this.left + W - 54, this.top + 5, 46, 14).build());
		for (int i = 0; i < this.land.plots().size(); i++) {
			int index = i;
			OpenLandPayload.Plot plot = this.land.plots().get(i);
			this.addRenderableWidget(Button.builder(Component.translatable(K + "abandon"), b -> {
				if (this.confirm == index) {
					send(LandActionPayload.ABANDON, "", plot.chunkX(), plot.chunkZ(), plot.minY());
				} else {
					this.confirm = index;
					b.setMessage(Component.translatable(K + "confirm").withStyle(ChatFormatting.RED));
				}
			}).tooltip(Tooltip.create(Component.translatable(K + "abandon_hint"))).bounds(this.left + W - 58, this.top + PLOTS_Y + i * ROW - 2, 50, 12).build());
		}
		int friendsTop = this.friendsTop();
		EditBox name = new EditBox(this.font, this.left + 12, friendsTop + 11, 150, 14, Component.translatable(K + "name_hint"));
		name.setMaxLength(16);
		name.setHint(Component.translatable(K + "name_hint"));
		this.addRenderableWidget(name);
		Button add = Button.builder(Component.translatable(K + "add"), b -> {
			if (!name.getValue().isBlank()) {
				send(LandActionPayload.TRUST, name.getValue().trim(), 0, 0, 0);
			}
		}).bounds(this.left + 168, friendsTop + 11, 50, 14).build();
		add.active = this.land.friends().size() < this.land.maxFriends();
		this.addRenderableWidget(add);
		for (int i = 0; i < this.land.friends().size(); i++) {
			OpenLandPayload.Friend friend = this.land.friends().get(i);
			this.addRenderableWidget(Button.builder(Component.literal("✕"), b -> send(LandActionPayload.UNTRUST, friend.id().toString(), 0, 0, 0))
				.bounds(this.friendX(i) + 74, this.friendY(i) - 2, 12, 12).build());
		}
	}

	private int friendsTop() {
		return this.top + PLOTS_Y + this.land.maxPlots() * ROW + 2;
	}

	private int friendX(final int i) {
		return this.left + 12 + (i / FRIEND_ROWS) * FRIEND_COLUMN;
	}

	private int friendY(final int i) {
		return this.friendsTop() + 32 + (i % FRIEND_ROWS) * ROW;
	}

	private static void send(final int action, final String text, final int x, final int z, final int y) {
		ClientPlayNetworking.send(new LandActionPayload(action, text, x, z, y));
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor g, final int mouseX, final int mouseY, final float a) {
		int x = this.left;
		int y = this.top;
		g.fill(x, y, x + W, y + H, 0xE8101814);
		g.outline(x, y, W, H, 0xFF5AA050);
		g.text(this.font, this.title.copy().withStyle(ChatFormatting.GOLD), x + 10, y + 8, 0xFFFFFFFF, true);
		g.text(this.font, Component.translatable(K + "plots", this.land.plots().size(), this.land.maxPlots()).withStyle(ChatFormatting.YELLOW), x + 12, y + 22, 0xFFFFFFFF, false);
		if (this.land.plots().isEmpty()) {
			g.text(this.font, Component.translatable(K + "no_plots").withStyle(ChatFormatting.GRAY), x + 16, y + PLOTS_Y, 0xFFFFFFFF, false);
		}
		for (int i = 0; i < this.land.plots().size(); i++) {
			OpenLandPayload.Plot plot = this.land.plots().get(i);
			int bx = plot.chunkX() << 4;
			int bz = plot.chunkZ() << 4;
			Component line = Component.translatable(K + "plot", bx, bx + 15, bz, bz + 15, plot.minY());
			g.text(this.font, line, x + 16, y + PLOTS_Y + i * ROW, 0xFFDDDDDD, false);
		}
		int fy = this.friendsTop();
		g.text(this.font, Component.translatable(K + "friends", this.land.friends().size(), this.land.maxFriends()).withStyle(ChatFormatting.YELLOW), x + 12, fy, 0xFFFFFFFF, false);
		if (this.land.friends().isEmpty()) {
			g.text(this.font, Component.translatable(K + "no_friends").withStyle(ChatFormatting.GRAY), x + 16, fy + 32, 0xFFFFFFFF, false);
		}
		for (int i = 0; i < this.land.friends().size(); i++) {
			g.text(this.font, this.font.plainSubstrByWidth(this.land.friends().get(i).name(), 70), this.friendX(i) + 4, this.friendY(i), 0xFFDDDDDD, false);
		}
		super.extractRenderState(g, mouseX, mouseY, a);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
