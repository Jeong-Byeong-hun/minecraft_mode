package com.minecraftmode.client.job;

import com.minecraftmode.economy.Essence;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.GearRolls;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.job.weapon.JobWeaponItem;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.loot.GearDrops;
import com.minecraftmode.loot.GearIndex;
import com.minecraftmode.loot.GearUpgrades;
import com.minecraftmode.loot.UpgradeMenu;
import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.registry.ModItems;
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
 * The blacksmith's bench, in two tabs. Evolution: the next pieces the gear in the slot can become, with their Evolution Ether
 * cost (green when the player carries enough); click a row to evolve. Options (armor only): the current extra options and,
 * after a paid roll (⟳), the new ones under them; ✔ takes the new options, ✖ keeps the old ones.
 */
public class UpgradeScreen extends AbstractContainerScreen<UpgradeMenu> {
	private static final int ROW_X = 40;
	private static final int ROW_W = 152;
	private static final int ROWS_Y = 32;
	private static final int ROW_H = 22;
	private static final int TAB_Y = 16;
	private static final int TAB_W = 60;
	private static final int TAB_H = 13;
	private static final int BUTTON_X = 6;
	private static final int BUTTON_W = 30;
	private static final int BUTTON_H = 14;
	private static final int REROLL_Y = 70;
	private static final int APPLY_Y = 88;
	private static final int KEEP_Y = 106;

	/** False = evolution tab, true = options tab. */
	private boolean options;

	public UpgradeScreen(final UpgradeMenu menu, final Inventory inventory, final Component title) {
		super(menu, inventory, title, UpgradeMenu.WIDTH, UpgradeMenu.HEIGHT);
		this.inventoryLabelX = UpgradeMenu.INVENTORY_X;
		this.inventoryLabelY = UpgradeMenu.INVENTORY_Y - 11;
	}

	/** Opens on the options tab (the game test uses it for its screenshot). */
	public void showOptions() {
		this.options = true;
	}

	private int have(final ClassGear target) {
		return this.minecraft.player == null ? 0 : UpgradeMenu.ether(this.minecraft.player.getInventory(), GearUpgrades.grade(target));
	}

	private boolean canPay(final ClassGear target) {
		return this.minecraft.player != null && (this.minecraft.player.isCreative()
			|| this.have(target) >= GearUpgrades.etherCost(target) && Coins.total(this.minecraft.player) >= GearUpgrades.coinCost(target, this.menu.input()));
	}

	private int rerollCost() {
		ClassGear gear = ClassGear.of(this.menu.input());
		return gear == null ? 0 : GearUpgrades.rerollCost(gear);
	}

	private boolean canReroll() {
		return this.menu.canReroll() && this.minecraft.player != null && this.menu.canPayReroll(this.minecraft.player);
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
		if (this.minecraft.player != null) {
			String coins = "◎" + Coins.format(Coins.total(this.minecraft.player));
			g.text(this.font, coins, x + this.imageWidth - 6 - this.font.width(coins), y + 6, 0xFF8A5A2A, false);
		}
		this.tab(g, x + ROW_X, y + TAB_Y, Component.translatable("screen.minecraft_mode.upgrade.tab_evolve"), !this.options, mouseX, mouseY);
		this.tab(g, x + ROW_X + TAB_W + 2, y + TAB_Y, Component.translatable("screen.minecraft_mode.upgrade.tab_options"), this.options, mouseX, mouseY);
		if (this.options) {
			this.extractOptions(g, x, y, mouseX, mouseY);
		} else {
			this.extractEvolution(g, x, y, mouseX, mouseY);
		}
	}

	private void tab(final GuiGraphicsExtractor g, final int tx, final int ty, final Component label, final boolean selected, final int mouseX, final int mouseY) {
		boolean hover = this.inside(mouseX, mouseY, tx, ty, TAB_W, TAB_H);
		g.fill(tx, ty, tx + TAB_W, ty + TAB_H, selected ? 0xFF5A3A1A : hover ? 0xFF9A9A9A : 0xFF8B8B8B);
		g.centeredText(this.font, label, tx + TAB_W / 2, ty + 3, selected ? 0xFFFFD27F : 0xFFFFFFFF);
	}

	private void button(final GuiGraphicsExtractor g, final int bx, final int by, final String label, final boolean enabled, final int color, final int hoverColor,
		final int mouseX, final int mouseY) {
		boolean hover = this.inside(mouseX, mouseY, bx, by, BUTTON_W, BUTTON_H);
		g.fill(bx, by, bx + BUTTON_W, by + BUTTON_H, !enabled ? 0xFF6B6B6B : hover ? hoverColor : color);
		g.centeredText(this.font, label, bx + BUTTON_W / 2, by + 3, 0xFFFFFFFF);
	}

	private void extractEvolution(final GuiGraphicsExtractor g, final int x, final int y, final int mouseX, final int mouseY) {
		List<ClassGear> targets = this.menu.targets();
		if (this.menu.input().isEmpty()) {
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.upgrade.insert", GearUpgrades.etherCost(ItemLevels.MIN_BRACKET), GearUpgrades.etherCost(ItemLevels.MAX_BRACKET)), x + ROW_X, y + ROWS_Y + 2, ROW_W, 0xFF404040);
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
			String coins = "◎" + Coins.format(GearUpgrades.coinCost(target, this.menu.input()));
			g.text(this.font, coins, x + ROW_X + ROW_W - 3 - this.font.width(coins), ry + 2, ok ? 0xFFFFD27F : 0xFFFF8080, false);
			String cost = this.have(target) + "/" + GearUpgrades.etherCost(target);
			g.text(this.font, Component.translatable("screen.minecraft_mode.upgrade.cost", target.level(), GearUpgrades.grade(target)), x + ROW_X + 21, ry + 11, 0xFFE0C090, false);
			g.text(this.font, cost, x + ROW_X + ROW_W - 3 - this.font.width(cost), ry + 11, ok ? 0xFF7FFFD4 : 0xFFFF8080, false);
		}
	}

	private void extractOptions(final GuiGraphicsExtractor g, final int x, final int y, final int mouseX, final int mouseY) {
		if (!this.menu.canReroll()) {
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.upgrade.armor_only"), x + ROW_X, y + ROWS_Y + 2, ROW_W, 0xFF404040);
			return;
		}
		GearRolls pending = this.menu.input().get(ModDataComponents.GEAR_ROLLS_PENDING);
		this.button(g, x + BUTTON_X, y + REROLL_Y, "⟳", this.canReroll(), 0xFF2C5A51, 0xFF3E7A6E, mouseX, mouseY);
		this.button(g, x + BUTTON_X, y + APPLY_Y, "✔", pending != null, 0xFF2E6B2E, 0xFF3E8A3E, mouseX, mouseY);
		this.button(g, x + BUTTON_X, y + KEEP_Y, "✖", pending != null, 0xFF7A2E2E, 0xFF9A3E3E, mouseX, mouseY);

		int ty = y + ROWS_Y + 2;
		g.text(this.font, Component.translatable("screen.minecraft_mode.upgrade.current"), x + ROW_X, ty, 0xFF404040, false);
		ty = this.lines(g, this.menu.input().get(ModDataComponents.GEAR_ROLLS), x + ROW_X, ty + 11, 0xFF1E5F7A);
		ty = Math.max(ty, y + ROWS_Y + 48);
		g.text(this.font, Component.translatable("screen.minecraft_mode.upgrade.pending"), x + ROW_X, ty, 0xFF404040, false);
		if (pending == null) {
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.upgrade.no_pending"), x + ROW_X, ty + 11, ROW_W, 0xFF6B6B6B);
		} else {
			this.lines(g, pending, x + ROW_X, ty + 11, 0xFF2E6B2E);
		}
	}

	/** Draws the option lines (or a dash) and returns the y below them. */
	private int lines(final GuiGraphicsExtractor g, final GearRolls rolls, final int lx, final int top, final int color) {
		int ly = top;
		if (rolls == null || rolls.lines().isEmpty()) {
			g.text(this.font, "-", lx + 4, ly, 0xFF6B6B6B, false);
			return ly + 10;
		}
		for (StatLine line : rolls.lines()) {
			g.text(this.font, Component.literal("• ").append(Component.translatable(line.stat().key(), JobTooltips.num(line.value()))), lx + 4, ly, color, false);
			ly += 10;
		}
		return ly;
	}

	@Override
	protected void extractTooltip(final GuiGraphicsExtractor g, final int mouseX, final int mouseY) {
		super.extractTooltip(g, mouseX, mouseY);
		if (this.options) {
			if (!this.menu.canReroll()) {
				return;
			}
			if (this.inside(mouseX, mouseY, this.leftPos + BUTTON_X, this.topPos + REROLL_Y, BUTTON_W, BUTTON_H)) {
				int essence = this.minecraft.player == null ? 0 : Essence.held(this.minecraft.player.getInventory(), ModItems.CONDENSED_ESSENCE);
				g.setComponentTooltipForNextFrame(this.font, List.of(Component.translatable("screen.minecraft_mode.upgrade.reroll"),
					Component.translatable("screen.minecraft_mode.upgrade.reroll_essence", GearUpgrades.REROLL_CONDENSED, essence)
						.withStyle(essence >= GearUpgrades.REROLL_CONDENSED ? ChatFormatting.AQUA : ChatFormatting.RED),
					Component.translatable("screen.minecraft_mode.engraving.coins", Coins.format(this.rerollCost())).withStyle(ChatFormatting.GOLD),
					Component.translatable("screen.minecraft_mode.upgrade.reroll_hint").withStyle(ChatFormatting.DARK_GRAY)), mouseX, mouseY);
			} else if (this.inside(mouseX, mouseY, this.leftPos + BUTTON_X, this.topPos + APPLY_Y, BUTTON_W, BUTTON_H)) {
				g.setComponentTooltipForNextFrame(this.font, List.of(Component.translatable("screen.minecraft_mode.upgrade.apply")), mouseX, mouseY);
			} else if (this.inside(mouseX, mouseY, this.leftPos + BUTTON_X, this.topPos + KEEP_Y, BUTTON_W, BUTTON_H)) {
				g.setComponentTooltipForNextFrame(this.font, List.of(Component.translatable("screen.minecraft_mode.upgrade.keep")), mouseX, mouseY);
			}
			return;
		}
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

	private void press(final int buttonId) {
		if (this.minecraft.player != null && this.menu.clickMenuButton(this.minecraft.player, buttonId)) {
			this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
		}
	}

	@Override
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		for (int t = 0; t < 2; t++) {
			if (this.inside(event.x(), event.y(), this.leftPos + ROW_X + t * (TAB_W + 2), this.topPos + TAB_Y, TAB_W, TAB_H)) {
				if (this.options != (t == 1)) {
					this.options = t == 1;
					this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				}
				return true;
			}
		}
		if (this.options) {
			if (this.menu.canReroll()) {
				int[] ys = {REROLL_Y, APPLY_Y, KEEP_Y};
				int[] ids = {UpgradeMenu.BUTTON_REROLL, UpgradeMenu.BUTTON_APPLY, UpgradeMenu.BUTTON_KEEP};
				for (int i = 0; i < ys.length; i++) {
					if (this.inside(event.x(), event.y(), this.leftPos + BUTTON_X, this.topPos + ys[i], BUTTON_W, BUTTON_H)) {
						this.press(ids[i]);
						return true;
					}
				}
			}
			return super.mouseClicked(event, doubleClick);
		}
		List<ClassGear> targets = this.menu.targets();
		for (int i = 0; i < targets.size(); i++) {
			int ry = this.topPos + ROWS_Y + i * ROW_H;
			if (this.inside(event.x(), event.y(), this.leftPos + ROW_X, ry, ROW_W, ROW_H - 2)) {
				this.press(i);
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	private boolean inside(final double mx, final double my, final int x, final int y, final int w, final int h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}
}
