package com.minecraftmode.client.lair;

import com.minecraftmode.job.JobProgression;
import com.minecraftmode.network.LairOfferPayload;
import com.minecraftmode.network.LairTravelPayload;
import com.minecraftmode.worldgen.lair.LairDef;
import com.minecraftmode.worldgen.lair.NamedLairs;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Asks whether to travel to the lair a lair map found: its name, monster and level range, where it is; Travel uses the map up. */
public class LairTravelScreen extends Screen {
	private static final int W = 300;
	private static final int H = 126;

	private final LairOfferPayload offer;
	private int left;
	private int top;

	public LairTravelScreen(final LairOfferPayload offer) {
		super(Component.translatable("screen.minecraft_mode.lair_travel.title"));
		this.offer = offer;
	}

	public LairOfferPayload offer() {
		return this.offer;
	}

	@Override
	protected void init() {
		this.left = (this.width - W) / 2;
		this.top = (this.height - H) / 2;
		this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.lair_travel.go"), b -> {
			ClientPlayNetworking.send(new LairTravelPayload(LairTravelPayload.TRAVEL));
			this.onClose();
		}).bounds(this.left + W / 2 - 104, this.top + H - 28, 100, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> this.onClose())
			.bounds(this.left + W / 2 + 4, this.top + H - 28, 100, 20).build());
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor g, final int mouseX, final int mouseY, final float a) {
		g.fill(this.left, this.top, this.left + W, this.top + H, 0xE8181208);
		g.outline(this.left, this.top, W, H, 0xFFFF9020);
		g.text(this.font, this.title.copy().withStyle(ChatFormatting.GOLD), this.left + 10, this.top + 9, 0xFFFFFFFF, true);
		LairDef def = NamedLairs.def(this.offer.lair());
		int y = this.top + 26;
		if (def != null) {
			g.text(this.font, Component.translatable(def.nameKey()).withStyle(ChatFormatting.YELLOW), this.left + 12, y, 0xFFFFFFFF, true);
			y += 13;
			g.text(this.font, Component.translatable("screen.minecraft_mode.lair_travel.monster", Component.translatable(def.named().nameKey()),
				def.named().lo(), def.named().hi()).withStyle(ChatFormatting.WHITE), this.left + 12, y, 0xFFFFFFFF, false);
			y += 12;
			Minecraft minecraft = Minecraft.getInstance();
			int level = minecraft.player == null ? 0 : JobProgression.get(minecraft.player).level();
			boolean fits = level >= def.named().lo() && level <= def.named().hi();
			g.text(this.font, Component.translatable(fits ? "screen.minecraft_mode.lair_travel.fits" : "screen.minecraft_mode.lair_travel.nearest")
				.withStyle(fits ? ChatFormatting.GREEN : ChatFormatting.GOLD), this.left + 12, y, 0xFFFFFFFF, false);
			y += 12;
		}
		g.text(this.font, Component.translatable("screen.minecraft_mode.lair_travel.where", this.offer.pos().getX(), this.offer.pos().getY(),
			this.offer.pos().getZ(), this.offer.distance()).withStyle(ChatFormatting.GRAY), this.left + 12, y, 0xFFFFFFFF, false);
		y += 12;
		g.text(this.font, Component.translatable("screen.minecraft_mode.lair_travel.note").withStyle(ChatFormatting.GRAY), this.left + 12, y,
			0xFFFFFFFF, false);
		super.extractRenderState(g, mouseX, mouseY, a);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
