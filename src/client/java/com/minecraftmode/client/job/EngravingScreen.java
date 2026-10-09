package com.minecraftmode.client.job;

import com.minecraftmode.job.engrave.Engraving;
import com.minecraftmode.job.engrave.EngravingMenu;
import com.minecraftmode.job.engrave.Engravings;
import com.minecraftmode.job.gear.ClassGear;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Engraving table screen: current lines (click ✕ to remove; 3 on weapons, 4 on armor), three offers
 * to engrave, reroll, and the essence the player carries. Drawn with plain fills, no texture.
 */
public class EngravingScreen extends AbstractContainerScreen<EngravingMenu> {
	private static final int ROW_X = 40;
	private static final int ROW_W = 152;
	private static final int LINES_Y = 18;
	private static final int LINE_STEP = 11;
	private static final int OFFERS_Y = 75;
	private static final int OFFER_STEP = 15;
	private static final int REROLL_X = 7;
	private static final int REROLL_Y = 46;

	public EngravingScreen(final EngravingMenu menu, final Inventory inventory, final Component title) {
		super(menu, inventory, title, EngravingMenu.WIDTH, EngravingMenu.HEIGHT);
		this.inventoryLabelX = EngravingMenu.INVENTORY_X;
		this.inventoryLabelY = EngravingMenu.INVENTORY_Y - 11;
	}

	private int essence() {
		return this.minecraft.player == null ? 0 : EngravingMenu.essence(this.minecraft.player.getInventory());
	}

	private boolean canPay(final int cost) {
		return this.minecraft.player != null && (this.minecraft.player.isCreative() || this.essence() >= cost);
	}

	@Override
	public void extractBackground(final GuiGraphicsExtractor g, final int mouseX, final int mouseY, final float a) {
		super.extractBackground(g, mouseX, mouseY, a);
		int x = this.leftPos;
		int y = this.topPos;
		// vanilla-like panel
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
		// weapon slot glow
		g.outline(x + EngravingMenu.WEAPON_X - 2, y + EngravingMenu.WEAPON_Y - 2, 20, 20, 0xFF7B3FBF);

		ItemStack weapon = this.menu.weapon();
		ClassGear def = ClassGear.of(weapon);
		Engravings engravings = EngravingMenu.engravings(weapon);

		// current lines
		int rows = def == null ? Engravings.WEAPON_LINES : def.maxLines();
		for (int i = 0; i < rows; i++) {
			int ry = y + LINES_Y + i * LINE_STEP;
			g.fill(x + ROW_X, ry, x + ROW_X + ROW_W, ry + 10, 0xFF2B2140);
			if (def != null && i < engravings.lines().size()) {
				Engraving e = Engraving.byId(engravings.lines().get(i));
				Component name = e == null ? Component.literal("?") : Component.translatable(e.nameKey());
				g.text(this.font, Component.literal((i + 1) + ". ").append(name), x + ROW_X + 3, ry + 1, 0xFFE3C9FF, false);
				boolean hover = this.inside(mouseX, mouseY, x + ROW_X + ROW_W - 12, ry, 12, 10);
				g.text(this.font, "✕", x + ROW_X + ROW_W - 9, ry + 1, hover ? 0xFFFF6B6B : 0xFFB0B0B0, false);
			} else {
				g.text(this.font, Component.literal((i + 1) + ". ").append(Component.translatable("screen.minecraft_mode.engraving.empty_line")), x + ROW_X + 3, ry + 1,
					0xFF6E6585, false);
			}
		}

		// offers
		g.text(this.font, Component.translatable("screen.minecraft_mode.engraving.offers"), x + ROW_X, y + OFFERS_Y - 11, 0xFF404040, false);
		for (int i = 0; i < 3 && def != null; i++) {
			int ry = y + OFFERS_Y + i * OFFER_STEP;
			Engraving offer = this.menu.offer(i);
			boolean enabled = offer != null && this.canPay(this.menu.engraveCost());
			boolean hover = offer != null && this.inside(mouseX, mouseY, x + ROW_X, ry, ROW_W, 14);
			g.fill(x + ROW_X, ry, x + ROW_X + ROW_W, ry + 14, !enabled ? 0xFF6B6B6B : hover ? 0xFF5E3A9E : 0xFF432A73);
			if (offer != null) {
				g.text(this.font, Component.translatable(offer.nameKey()), x + ROW_X + 3, ry + 3, enabled ? 0xFFFFFFFF : 0xFFBDBDBD, false);
				String cost = "◆" + this.menu.engraveCost();
				g.text(this.font, cost, x + ROW_X + ROW_W - 3 - this.font.width(cost), ry + 3, this.canPay(this.menu.engraveCost()) ? 0xFF7FFFD4 : 0xFFFF8080, false);
			}
		}

		// reroll + essence
		boolean rerollOk = def != null && this.canPay(this.menu.rerollCost());
		boolean rerollHover = this.inside(mouseX, mouseY, x + REROLL_X, y + REROLL_Y, 30, 14);
		g.fill(x + REROLL_X, y + REROLL_Y, x + REROLL_X + 30, y + REROLL_Y + 14, !rerollOk ? 0xFF6B6B6B : rerollHover ? 0xFF3E7A6E : 0xFF2C5A51);
		g.centeredText(this.font, "⟳", x + REROLL_X + 15, y + REROLL_Y + 3, 0xFFFFFFFF);
		String have = "◆" + this.essence();
		g.text(this.font, have, x + 8, y + REROLL_Y + 20, 0xFF1F7A68, false);
		if (def == null) {
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.engraving.insert"), x + ROW_X, y + OFFERS_Y + 2, ROW_W, 0xFF404040);
		}
	}

	@Override
	protected void extractTooltip(final GuiGraphicsExtractor g, final int mouseX, final int mouseY) {
		super.extractTooltip(g, mouseX, mouseY);
		int x = this.leftPos;
		int y = this.topPos;
		ItemStack weapon = this.menu.weapon();
		ClassGear def = ClassGear.of(weapon);
		if (def == null) {
			return;
		}
		Engravings engravings = EngravingMenu.engravings(weapon);
		for (int i = 0; i < engravings.lines().size(); i++) {
			int ry = y + LINES_Y + i * LINE_STEP;
			if (this.inside(mouseX, mouseY, x + ROW_X, ry, ROW_W, 10)) {
				Engraving e = Engraving.byId(engravings.lines().get(i));
				List<Component> tip = new ArrayList<>();
				if (e != null) {
					tip.add(JobTooltips.engravingLine(e, 1));
				}
				tip.add(Component.translatable("screen.minecraft_mode.engraving.remove", this.menu.removeCost()).withStyle(ChatFormatting.RED));
				g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
			}
		}
		for (int i = 0; i < 3; i++) {
			Engraving offer = this.menu.offer(i);
			int ry = y + OFFERS_Y + i * OFFER_STEP;
			if (offer != null && this.inside(mouseX, mouseY, x + ROW_X, ry, ROW_W, 14)) {
				int already = (int)engravings.resolved().stream().filter(e -> e == offer).count();
				List<Component> tip = new ArrayList<>();
				tip.add(JobTooltips.engravingLine(offer, 1));
				if (already > 0) {
					tip.add(Component.translatable("screen.minecraft_mode.engraving.after").withStyle(ChatFormatting.GOLD).append(JobTooltips.engravingLine(offer, already + 1)));
				}
				tip.add(Component.translatable("screen.minecraft_mode.engraving.cost", this.menu.engraveCost()).withStyle(ChatFormatting.AQUA));
				g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
			}
		}
		if (this.inside(mouseX, mouseY, x + REROLL_X, y + REROLL_Y, 30, 14)) {
			g.setComponentTooltipForNextFrame(this.font, List.of(
				Component.translatable("screen.minecraft_mode.engraving.reroll"),
				Component.translatable("screen.minecraft_mode.engraving.cost", this.menu.rerollCost()).withStyle(ChatFormatting.AQUA)
			), mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		int x = this.leftPos;
		int y = this.topPos;
		double mx = event.x();
		double my = event.y();
		Engravings engravings = EngravingMenu.engravings(this.menu.weapon());
		for (int i = 0; i < 3; i++) {
			if (this.inside(mx, my, x + ROW_X, y + OFFERS_Y + i * OFFER_STEP, ROW_W, 14) && this.press(i)) {
				return true;
			}
		}
		for (int i = 0; i < engravings.lines().size(); i++) {
			if (this.inside(mx, my, x + ROW_X + ROW_W - 12, y + LINES_Y + i * LINE_STEP, 12, 10) && this.press(EngravingMenu.BUTTON_REMOVE + i)) {
				return true;
			}
		}
		if (this.inside(mx, my, x + REROLL_X, y + REROLL_Y, 30, 14) && this.press(EngravingMenu.BUTTON_REROLL)) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private boolean press(final int button) {
		if (this.minecraft.player != null && this.menu.clickMenuButton(this.minecraft.player, button)) {
			this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, button);
			return true;
		}
		return false;
	}

	private boolean inside(final double mx, final double my, final int x, final int y, final int w, final int h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}
}
