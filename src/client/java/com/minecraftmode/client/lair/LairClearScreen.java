package com.minecraftmode.client.lair;

import com.minecraftmode.network.LairClearPayload;
import com.minecraftmode.network.LairTravelPayload;
import com.minecraftmode.worldgen.lair.LairDef;
import com.minecraftmode.worldgen.lair.NamedLairs;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Shown after a lair is cleared for the cycle: the next map is in the bags, and a button takes the player back to the gate. */
public class LairClearScreen extends Screen {
	private static final int W = 250;
	private static final int H = 96;

	private final LairClearPayload clear;
	private int left;
	private int top;

	public LairClearScreen(final LairClearPayload clear) {
		super(Component.translatable("screen.minecraft_mode.lair_clear.title"));
		this.clear = clear;
	}

	public LairClearPayload clear() {
		return this.clear;
	}

	@Override
	protected void init() {
		this.left = (this.width - W) / 2;
		this.top = (this.height - H) / 2;
		this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.lair_clear.exit"), b -> {
			ClientPlayNetworking.send(new LairTravelPayload(LairTravelPayload.EXIT));
			this.onClose();
		}).bounds(this.left + 10, this.top + H - 28, 140, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.lair_clear.close"), b -> this.onClose())
			.bounds(this.left + W - 94, this.top + H - 28, 84, 20).build());
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor g, final int mouseX, final int mouseY, final float a) {
		g.fill(this.left, this.top, this.left + W, this.top + H, 0xE8181208);
		g.outline(this.left, this.top, W, H, 0xFFFFD040);
		g.text(this.font, this.title.copy().withStyle(ChatFormatting.GOLD), this.left + 10, this.top + 9, 0xFFFFFFFF, true);
		LairDef def = NamedLairs.def(this.clear.lair());
		Component name = def == null ? Component.literal(this.clear.lair()) : Component.translatable(def.nameKey());
		g.text(this.font, Component.translatable("screen.minecraft_mode.lair_clear.body", name.copy().withStyle(ChatFormatting.YELLOW))
			.withStyle(ChatFormatting.WHITE), this.left + 12, this.top + 27, 0xFFFFFFFF, false);
		if (this.clear.nextMap()) {
			g.text(this.font, Component.translatable("screen.minecraft_mode.lair_clear.next").withStyle(ChatFormatting.AQUA), this.left + 12, this.top + 41,
				0xFFFFFFFF, false);
		}
		super.extractRenderState(g, mouseX, mouseY, a);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
