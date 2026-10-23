package com.minecraftmode.client.lair;

import com.minecraftmode.economy.Essence;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.worldgen.lair.LairTier;
import com.minecraftmode.worldgen.lair.MapCombinerMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Cartographer Ysolde's table: the map slot on the left; on the right the tier now and next, the chance, the material one attempt takes
 * (green when carried) and the raise button, plus the sure merge of three maps of one tier.
 */
public class MapCombinerScreen extends AbstractContainerScreen<MapCombinerMenu> {
	private static final int PX = 40;
	private static final int PW = 152;
	private static final int BUTTON_Y = 60;
	private static final int BUTTON_W = 74;
	private static final int BUTTON_H = 16;

	public MapCombinerScreen(final MapCombinerMenu menu, final Inventory inventory, final Component title) {
		super(menu, inventory, title, MapCombinerMenu.WIDTH, MapCombinerMenu.HEIGHT);
		this.inventoryLabelX = MapCombinerMenu.INVENTORY_X;
		this.inventoryLabelY = MapCombinerMenu.INVENTORY_Y - 11;
	}

	private boolean raisable() {
		int tier = this.menu.tier();
		return tier > 0 && tier < LairTier.MAX;
	}

	private boolean canPay() {
		return this.raisable() && this.minecraft.player != null && MapCombinerMenu.canPay(this.minecraft.player, this.menu.tier());
	}

	private boolean canMerge() {
		return this.raisable() && this.menu.input().getCount() >= LairTier.MERGE;
	}

	@Override
	public void extractBackground(final GuiGraphicsExtractor g, final int mouseX, final int mouseY, final float a) {
		super.extractBackground(g, mouseX, mouseY, a);
		int x = this.leftPos;
		int y = this.topPos;
		g.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFFC6C6C6);
		g.fill(x, y, x + this.imageWidth - 1, y + 1, 0xFFFFFFFF);
		g.fill(x, y, x + 1, y + this.imageHeight - 1, 0xFFFFFFFF);
		g.fill(x + 1, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF555555);
		g.fill(x + this.imageWidth - 1, y + 1, x + this.imageWidth, y + this.imageHeight, 0xFF555555);
		for (Slot slot : this.menu.slots) {
			int sx = x + slot.x - 1;
			int sy = y + slot.y - 1;
			g.fill(sx, sy, sx + 18, sy + 18, 0xFF8B8B8B);
			g.fill(sx, sy, sx + 17, sy + 1, 0xFF373737);
			g.fill(sx, sy, sx + 1, sy + 17, 0xFF373737);
			g.fill(sx + 1, sy + 17, sx + 18, sy + 18, 0xFFFFFFFF);
			g.fill(sx + 17, sy + 1, sx + 18, sy + 18, 0xFFFFFFFF);
		}
		g.outline(x + MapCombinerMenu.SLOT_X - 2, y + MapCombinerMenu.SLOT_Y - 2, 20, 20, 0xFF8A5AD0);
		int result = this.menu.result();
		if (result != MapCombinerMenu.RESULT_NONE) {
			String key = switch (result) {
				case MapCombinerMenu.RESULT_SUCCESS -> "screen.minecraft_mode.map_combiner.result.success";
				case MapCombinerMenu.RESULT_MERGED -> "screen.minecraft_mode.map_combiner.result.merged";
				default -> "screen.minecraft_mode.map_combiner.result.fail";
			};
			g.centeredText(this.font, Component.translatable(key), x + MapCombinerMenu.SLOT_X + 8, y + MapCombinerMenu.SLOT_Y + 24,
				result == MapCombinerMenu.RESULT_FAIL ? 0xFFB02A2A : 0xFF1E8A2E);
		}
		int px = x + PX;
		int ty = y + 17;
		int tier = this.menu.tier();
		if (tier <= 0) {
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.map_combiner.insert"), px, ty + 2, PW, 0xFF404040);
			return;
		}
		if (tier >= LairTier.MAX) {
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.map_combiner.top", LairTier.MAX), px, ty + 2, PW, 0xFF404040);
			return;
		}
		g.text(this.font, Component.literal("T" + tier), px, ty, 0xFF5A3A90, false);
		int ax = px + this.font.width("T" + tier) + 4;
		g.text(this.font, "➜", ax, ty, 0xFF404040, false);
		g.text(this.font, Component.literal("T" + (tier + 1)).withStyle(ChatFormatting.BOLD), ax + 12, ty, 0xFF8A2AB0, false);
		int chance = LairTier.combineChance(tier);
		String rate = Component.translatable("screen.minecraft_mode.map_combiner.chance", chance).getString();
		g.text(this.font, rate, px + PW - this.font.width(rate), ty, chance >= 50 ? 0xFF6A6A1E : 0xFFB02A2A, false);
		ty += 12;
		Inventory inventory = this.minecraft.player.getInventory();
		boolean creative = this.minecraft.player.isCreative();
		boolean ether = tier >= LairTier.ETHER_FROM;
		int held = ether ? MapCombinerMenu.ether(inventory) : Essence.held(inventory, ModItems.CONDENSED_ESSENCE);
		Component material = Component.translatable(ether ? "screen.minecraft_mode.map_combiner.ether" : "screen.minecraft_mode.map_combiner.essence");
		g.text(this.font, material, px, ty, 0xFF404040, false);
		String count = held + " / 1";
		g.text(this.font, count, px + PW - this.font.width(count), ty, creative || held >= 1 ? 0xFF1E8A2E : 0xFFB02A2A, false);
		ty += 11;
		g.text(this.font, Component.translatable("screen.minecraft_mode.map_combiner.keep"), px, ty, 0xFF606060, false);
		this.button(g, px, y + BUTTON_Y, Component.translatable("screen.minecraft_mode.map_combiner.raise"), this.canPay(), mouseX, mouseY);
		this.button(g, px + PW - BUTTON_W, y + BUTTON_Y, Component.translatable("screen.minecraft_mode.map_combiner.merge", LairTier.MERGE), this.canMerge(), mouseX, mouseY);
	}

	private void button(final GuiGraphicsExtractor g, final int x, final int y, final Component label, final boolean active, final int mouseX, final int mouseY) {
		boolean hover = inside(mouseX, mouseY, x, y, BUTTON_W, BUTTON_H);
		g.fill(x, y, x + BUTTON_W, y + BUTTON_H, !active ? 0xFF6B6B6B : hover ? 0xFFA070E0 : 0xFF7A4AB0);
		g.centeredText(this.font, label, x + BUTTON_W / 2, y + 4, 0xFFFFFFFF);
	}

	@Override
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		if (this.minecraft.player != null) {
			int bx = this.leftPos + PX;
			int by = this.topPos + BUTTON_Y;
			int button = -1;
			if (this.canPay() && inside(event.x(), event.y(), bx, by, BUTTON_W, BUTTON_H)) {
				button = MapCombinerMenu.BUTTON_UPGRADE;
			} else if (this.canMerge() && inside(event.x(), event.y(), bx + PW - BUTTON_W, by, BUTTON_W, BUTTON_H)) {
				button = MapCombinerMenu.BUTTON_MERGE;
			}
			if (button >= 0 && this.menu.clickMenuButton(this.minecraft.player, button)) {
				this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, button);
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	private static boolean inside(final double mx, final double my, final int x, final int y, final int w, final int h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}
}
