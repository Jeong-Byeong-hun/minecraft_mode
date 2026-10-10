package com.minecraftmode.client.style;

import com.minecraftmode.loot.Coins;
import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.style.Looks;
import com.minecraftmode.style.StylistMenu;
import java.util.List;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Stylist Celeste's table: the gear slot, the slot for the piece whose look it takes, the result preview and the price, a button
 * to change the look and one to bring the original look back. Restyled gear shows "Look: ..." in its tooltip.
 */
public class StylistScreen extends AbstractContainerScreen<StylistMenu> {
	private static final int PREVIEW_X = 134;
	private static final int BUTTON_Y = 50;
	private static final int APPLY_X = 8;
	private static final int APPLY_W = 90;
	private static final int RESTORE_X = 102;
	private static final int RESTORE_W = 66;
	private static final int BUTTON_H = 14;

	public StylistScreen(final StylistMenu menu, final Inventory inventory, final Component title) {
		super(menu, inventory, title, StylistMenu.WIDTH, StylistMenu.HEIGHT);
		this.inventoryLabelX = StylistMenu.INVENTORY_X;
		this.inventoryLabelY = StylistMenu.INVENTORY_Y - 11;
	}

	/** "Look: ..." under the name of restyled gear. */
	public static void registerTooltip() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			Identifier look = stack.get(ModDataComponents.LOOK);
			if (look != null && !lines.isEmpty()) {
				lines.add(1, Component.translatable("tooltip.minecraft_mode.look", Component.translatable(BuiltInRegistries.ITEM.getValue(look).getDescriptionId()))
					.withStyle(ChatFormatting.LIGHT_PURPLE));
			}
		});
	}

	private boolean canPay() {
		return this.minecraft.player != null && (this.minecraft.player.isCreative() || Coins.total(this.minecraft.player) >= Looks.price(this.menu.target()));
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
		g.outline(x + StylistMenu.TARGET_X - 2, y + StylistMenu.SLOT_Y - 2, 20, 20, 0xFF7A4AA8);
		g.centeredText(this.font, "+", x + (StylistMenu.TARGET_X + StylistMenu.DONOR_X) / 2 + 8, y + StylistMenu.SLOT_Y + 4, 0xFF5A3480);
		g.centeredText(this.font, "➜", x + (StylistMenu.DONOR_X + PREVIEW_X) / 2 + 8, y + StylistMenu.SLOT_Y + 4, 0xFF5A3480);
		// result preview
		int px = x + PREVIEW_X;
		int py = y + StylistMenu.SLOT_Y;
		g.fill(px - 1, py - 1, px + 17, py + 17, 0xFFB8A8D0);
		boolean ok = this.menu.canApply();
		if (ok) {
			g.fakeItem(Looks.apply(this.menu.target(), this.menu.donor()), px, py);
		}
		// buttons
		boolean applyHover = this.inside(mouseX, mouseY, x + APPLY_X, y + BUTTON_Y, APPLY_W, BUTTON_H);
		g.fill(x + APPLY_X, y + BUTTON_Y, x + APPLY_X + APPLY_W, y + BUTTON_Y + BUTTON_H, !ok || !this.canPay() ? 0xFF6B6B6B : applyHover ? 0xFF9A6AC8 : 0xFF7A4AA8);
		String price = this.menu.target().isEmpty() ? "" : " ◎" + Coins.format(Looks.price(this.menu.target()));
		g.centeredText(this.font, Component.translatable("screen.minecraft_mode.stylist.apply").getString() + price, x + APPLY_X + APPLY_W / 2, y + BUTTON_Y + 3, 0xFFFFFFFF);
		boolean styled = Looks.styled(this.menu.target());
		boolean restoreHover = this.inside(mouseX, mouseY, x + RESTORE_X, y + BUTTON_Y, RESTORE_W, BUTTON_H);
		g.fill(x + RESTORE_X, y + BUTTON_Y, x + RESTORE_X + RESTORE_W, y + BUTTON_Y + BUTTON_H, !styled ? 0xFF6B6B6B : restoreHover ? 0xFF6A8AA8 : 0xFF4A6A88);
		g.centeredText(this.font, Component.translatable("screen.minecraft_mode.stylist.restore"), x + RESTORE_X + RESTORE_W / 2, y + BUTTON_Y + 3, 0xFFFFFFFF);
		// what to do, or why it cannot be done
		Component hint;
		if (this.menu.target().isEmpty() || this.menu.donor().isEmpty()) {
			hint = Component.translatable("screen.minecraft_mode.stylist.hint");
		} else if (!ok) {
			hint = Component.translatable("screen.minecraft_mode.stylist.mismatch");
		} else {
			hint = Component.translatable("screen.minecraft_mode.stylist.used_up");
		}
		g.textWithWordWrap(this.font, hint, x + 8, y + BUTTON_Y + BUTTON_H + 4, this.imageWidth - 16, 0xFF404040);
	}

	@Override
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		if (this.minecraft.player != null) {
			int button = this.inside(event.x(), event.y(), this.leftPos + APPLY_X, this.topPos + BUTTON_Y, APPLY_W, BUTTON_H) ? StylistMenu.BUTTON_APPLY
				: this.inside(event.x(), event.y(), this.leftPos + RESTORE_X, this.topPos + BUTTON_Y, RESTORE_W, BUTTON_H) ? StylistMenu.BUTTON_RESTORE : -1;
			if (button >= 0 && this.menu.clickMenuButton(this.minecraft.player, button)) {
				this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, button);
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	protected void extractTooltip(final GuiGraphicsExtractor g, final int mouseX, final int mouseY) {
		super.extractTooltip(g, mouseX, mouseY);
		if (this.menu.canApply() && this.inside(mouseX, mouseY, this.leftPos + PREVIEW_X, this.topPos + StylistMenu.SLOT_Y, 16, 16)) {
			ItemStack preview = Looks.apply(this.menu.target(), this.menu.donor());
			g.setComponentTooltipForNextFrame(this.font, List.copyOf(getTooltipFromItem(this.minecraft, preview)), mouseX, mouseY);
		}
	}

	private boolean inside(final double mx, final double my, final int x, final int y, final int w, final int h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}
}
