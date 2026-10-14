package com.minecraftmode.client.accessory;

import com.minecraftmode.accessory.Accessories;
import com.minecraftmode.accessory.AccessoryMenu;
import com.minecraftmode.accessory.AccessorySet;
import com.minecraftmode.accessory.AccessorySlot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The accessory window: five slots (necklace, earring, ring, bracelet, talisman) with the worn sets and their piece counts below.
 * An empty slot names itself on hover; the pieces' own tooltips show their lines and set bonuses.
 */
public class AccessoryScreen extends AbstractContainerScreen<AccessoryMenu> {
	private static final int SETS_Y = 58;

	public AccessoryScreen(final AccessoryMenu menu, final Inventory inventory, final Component title) {
		super(menu, inventory, title, AccessoryMenu.WIDTH, AccessoryMenu.HEIGHT);
		this.inventoryLabelX = 8;
		this.inventoryLabelY = AccessoryMenu.INVENTORY_Y - 11;
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
		// the accessory slots get a gold rim and their kind's initial while empty
		for (AccessorySlot kind : AccessorySlot.values()) {
			Slot slot = this.menu.slots.get(kind.ordinal());
			int sx = x + slot.x;
			int sy = y + slot.y;
			g.outline(sx - 2, sy - 2, 20, 20, 0xFFB8862B);
			if (!slot.hasItem()) {
				String initial = Component.translatable(kind.nameKey()).getString().substring(0, 1);
				g.text(this.font, initial, sx + 8 - this.font.width(initial) / 2, sy + 4, 0xFF6B6B6B, false);
			}
		}
	}

	@Override
	protected void extractLabels(final GuiGraphicsExtractor g, final int mouseX, final int mouseY) {
		super.extractLabels(g, mouseX, mouseY);
		g.text(this.font, Component.translatable("screen.minecraft_mode.accessories.hint"), 8, 18, 0xFF6B4A1B, false);
		if (this.minecraft.player == null) {
			return;
		}
		Map<String, Integer> counts = Accessories.setCounts(this.minecraft.player);
		int line = 0;
		if (counts.isEmpty()) {
			g.text(this.font, Component.translatable("screen.minecraft_mode.accessories.none"), 8, SETS_Y, 0xFF555555, false);
			return;
		}
		for (Map.Entry<String, Integer> entry : counts.entrySet()) {
			AccessorySet set = Accessories.set(entry.getKey());
			if (set == null || line >= 2) {
				continue;
			}
			Component text = Component.translatable("screen.minecraft_mode.accessories.set", Component.translatable(set.nameKey()), entry.getValue(),
				Accessories.SLOTS);
			g.text(this.font, text, 8, SETS_Y + line * 10, 0xFF000000 | set.gem() & 0xFFFFFF, false);
			line++;
		}
	}

	@Override
	protected void extractTooltip(final GuiGraphicsExtractor g, final int mouseX, final int mouseY) {
		super.extractTooltip(g, mouseX, mouseY);
		for (AccessorySlot kind : AccessorySlot.values()) {
			Slot slot = this.menu.slots.get(kind.ordinal());
			int sx = this.leftPos + slot.x;
			int sy = this.topPos + slot.y;
			if (!slot.hasItem() && mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
				List<Component> tip = new ArrayList<>();
				tip.add(Component.translatable(kind.nameKey()).withStyle(ChatFormatting.GOLD));
				tip.add(Component.translatable("screen.minecraft_mode.accessories.raid_only").withStyle(ChatFormatting.GRAY));
				g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
			}
		}
	}
}
