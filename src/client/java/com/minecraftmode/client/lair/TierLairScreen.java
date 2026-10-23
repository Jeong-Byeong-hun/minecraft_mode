package com.minecraftmode.client.lair;

import com.minecraftmode.client.job.JobTooltips;
import com.minecraftmode.network.LairTravelPayload;
import com.minecraftmode.network.TierOfferPayload;
import com.minecraftmode.worldgen.lair.LairTier;
import com.minecraftmode.worldgen.lair.TierLairs;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Asks whether to open the tier lair a tier map leads to: its tier, how strong it is, the affixes, who comes along; Open uses the map up. */
public class TierLairScreen extends Screen {
	private static final int W = 300;
	private static final int H = 150;

	private final TierOfferPayload offer;
	private int left;
	private int top;

	public TierLairScreen(final TierOfferPayload offer) {
		super(Component.translatable("screen.minecraft_mode.tier_lair.title", offer.tier()));
		this.offer = offer;
	}

	public TierOfferPayload offer() {
		return this.offer;
	}

	@Override
	protected void init() {
		this.left = (this.width - W) / 2;
		this.top = (this.height - H) / 2;
		this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.tier_lair.go"), b -> {
			ClientPlayNetworking.send(new LairTravelPayload(LairTravelPayload.TIER));
			this.onClose();
		}).bounds(this.left + W / 2 - 104, this.top + H - 28, 100, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> this.onClose())
			.bounds(this.left + W / 2 + 4, this.top + H - 28, 100, 20).build());
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor g, final int mouseX, final int mouseY, final float a) {
		int tier = this.offer.tier();
		g.fill(this.left, this.top, this.left + W, this.top + H, 0xE8140C1C);
		g.outline(this.left, this.top, W, H, 0xFFC070FF);
		g.text(this.font, this.title.copy().withStyle(ChatFormatting.LIGHT_PURPLE), this.left + 10, this.top + 9, 0xFFFFFFFF, true);
		int y = this.top + 26;
		g.text(this.font, Component.translatable("screen.minecraft_mode.tier_lair.power", JobTooltips.fine(LairTier.health(tier)), JobTooltips.fine(LairTier.damage(tier)))
			.withStyle(ChatFormatting.WHITE), this.left + 12, y, 0xFFFFFFFF, false);
		y += 12;
		int affixes = LairTier.affixCount(tier);
		g.text(this.font, affixes > 0 ? Component.translatable("screen.minecraft_mode.tier_lair.affixes", affixes).withStyle(ChatFormatting.LIGHT_PURPLE)
			: Component.translatable("screen.minecraft_mode.tier_lair.no_affixes").withStyle(ChatFormatting.GRAY), this.left + 12, y, 0xFFFFFFFF, false);
		y += 12;
		g.text(this.font, Component.translatable("screen.minecraft_mode.tier_lair.party", String.join(", ", this.offer.going())).withStyle(ChatFormatting.AQUA),
			this.left + 12, y, 0xFFFFFFFF, false);
		y += 12;
		g.text(this.font, Component.translatable("screen.minecraft_mode.tier_lair.lives", TierLairs.LIVES).withStyle(ChatFormatting.GRAY), this.left + 12, y,
			0xFFFFFFFF, false);
		y += 12;
		g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.tier_lair.note").withStyle(ChatFormatting.GRAY), this.left + 12, y, W - 24, 0xFFFFFFFF);
		super.extractRenderState(g, mouseX, mouseY, a);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
