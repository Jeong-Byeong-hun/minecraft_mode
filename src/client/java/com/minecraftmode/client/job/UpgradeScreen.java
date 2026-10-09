package com.minecraftmode.client.job;

import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.weapon.JobWeaponItem;
import com.minecraftmode.loot.GearDrops;
import com.minecraftmode.loot.GearIndex;
import com.minecraftmode.loot.GearUpgrades;
import com.minecraftmode.loot.UpgradeMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The blacksmith's evolution bench: the gear slot on the left, the next pieces it can become on the
 * right with their Evolution Ether cost (green when the player carries enough). Click a row to evolve.
 */
public class UpgradeScreen extends AbstractContainerScreen<UpgradeMenu> {
	private static final int ROW_X = 40;
	private static final int ROW_W = 152;
	private static final int ROWS_Y = 18;
	private static final int ROW_H = 22;

	public UpgradeScreen(final UpgradeMenu menu, final Inventory inventory, final Component title) {
		super(menu, inventory, title, UpgradeMenu.WIDTH, UpgradeMenu.HEIGHT);
		this.inventoryLabelX = UpgradeMenu.INVENTORY_X;
		this.inventoryLabelY = UpgradeMenu.INVENTORY_Y - 11;
	}

	private int have(final ClassGear target) {
		return this.minecraft.player == null ? 0 : UpgradeMenu.ether(this.minecraft.player.getInventory(), GearUpgrades.grade(target));
	}

	private boolean canPay(final ClassGear target) {
		return this.minecraft.player != null && (this.minecraft.player.isCreative() || this.have(target) >= GearUpgrades.ETHER_COST);
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
		g.outline(x + UpgradeMenu.SLOT_X - 2, y + UpgradeMenu.SLOT_Y - 2, 20, 20, 0xFFE07B26);
		g.centeredText(this.font, "➜", x + UpgradeMenu.SLOT_X + 8, y + UpgradeMenu.SLOT_Y + 22, 0xFF8A5A2A);

		List<ClassGear> targets = this.menu.targets();
		if (this.menu.input().isEmpty()) {
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.upgrade.insert", GearUpgrades.ETHER_COST), x + ROW_X, y + ROWS_Y + 2, ROW_W, 0xFF404040);
			return;
		}
		if (targets.isEmpty()) {
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.upgrade.max"), x + ROW_X, y + ROWS_Y + 2, ROW_W, 0xFF404040);
			return;
		}
		for (int i = 0; i < targets.size(); i++) {
			ClassGear target = targets.get(i);
			int ry = y + ROWS_Y + i * ROW_H;
			boolean ok = this.canPay(target);
			boolean hover = this.inside(mouseX, mouseY, x + ROW_X, ry, ROW_W, ROW_H - 2);
			g.fill(x + ROW_X, ry, x + ROW_X + ROW_W, ry + ROW_H - 2, !ok ? 0xFF6B6B6B : hover ? 0xFF8A5A2A : 0xFF5A3A1A);
			g.fakeItem(GearIndex.stack(target), x + ROW_X + 2, ry + 2);
			Component name = Component.translatable(GearIndex.item(target).getDescriptionId()).withColor(JobWeaponItem.tierColor(target.tier()));
			g.text(this.font, name, x + ROW_X + 21, ry + 2, 0xFFFFFFFF, false);
			String cost = this.have(target) + "/" + GearUpgrades.ETHER_COST;
			g.text(this.font, Component.translatable("screen.minecraft_mode.upgrade.cost", target.level(), GearUpgrades.grade(target)), x + ROW_X + 21, ry + 11, 0xFFE0C090, false);
			g.text(this.font, cost, x + ROW_X + ROW_W - 3 - this.font.width(cost), ry + 11, ok ? 0xFF7FFFD4 : 0xFFFF8080, false);
		}
	}

	@Override
	protected void extractTooltip(final GuiGraphicsExtractor g, final int mouseX, final int mouseY) {
		super.extractTooltip(g, mouseX, mouseY);
		List<ClassGear> targets = this.menu.targets();
		for (int i = 0; i < targets.size(); i++) {
			int ry = this.topPos + ROWS_Y + i * ROW_H;
			if (this.inside(mouseX, mouseY, this.leftPos + ROW_X, ry, ROW_W, ROW_H - 2)) {
				ItemStack preview = GearDrops.create(targets.get(i), RandomSource.create(0L));
				List<Component> tip = new ArrayList<>(getTooltipFromItem(this.minecraft, preview));
				tip.add(Component.translatable("screen.minecraft_mode.upgrade.keeps").withStyle(ChatFormatting.DARK_GRAY));
				g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
			}
		}
	}

	@Override
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		List<ClassGear> targets = this.menu.targets();
		for (int i = 0; i < targets.size(); i++) {
			int ry = this.topPos + ROWS_Y + i * ROW_H;
			if (this.inside(event.x(), event.y(), this.leftPos + ROW_X, ry, ROW_W, ROW_H - 2) && this.minecraft.player != null
				&& this.menu.clickMenuButton(this.minecraft.player, i)) {
				this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, i);
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	private boolean inside(final double mx, final double my, final int x, final int y, final int w, final int h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}
}
