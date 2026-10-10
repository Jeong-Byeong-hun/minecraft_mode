package com.minecraftmode.client.craft;

import com.minecraftmode.craft.CraftMenu;
import com.minecraftmode.craft.CraftRecipes;
import com.minecraftmode.craft.Profession;
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
 * A profession station: the profession's level and experience on top, then its recipes six to a page (output, level needed,
 * ingredients with what the player carries, make one or five), and the inventory below. Locked recipes are greyed out.
 */
public class CraftScreen extends AbstractContainerScreen<CraftMenu> {
	private static final int ROWS = 6;
	private static final int ROW_H = 19;
	private static final int ROWS_Y = 18;
	private static final int ING_X = 128;
	private static final int ONE_X = 206;
	private static final int FIVE_X = 228;
	private static final int BUTTON_W = 20;
	private int page;

	public CraftScreen(final CraftMenu menu, final Inventory inventory, final Component title) {
		super(menu, inventory, title, CraftMenu.WIDTH, CraftMenu.HEIGHT);
		this.inventoryLabelX = CraftMenu.INVENTORY_X;
		this.inventoryLabelY = CraftMenu.INVENTORY_Y - 11;
	}

	private int pages() {
		return Math.max(1, (this.menu.recipes().size() + ROWS - 1) / ROWS);
	}

	private boolean inside(final double mx, final double my, final int x, final int y, final int w, final int h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}

	@Override
	public void extractBackground(final GuiGraphicsExtractor g, final int mouseX, final int mouseY, final float a) {
		super.extractBackground(g, mouseX, mouseY, a);
		int x = this.leftPos;
		int y = this.topPos;
		Profession profession = this.menu.profession();
		g.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFFC6C6C6);
		g.fill(x, y, x + this.imageWidth - 1, y + 1, 0xFFFFFFFF);
		g.fill(x, y, x + 1, y + this.imageHeight - 1, 0xFFFFFFFF);
		g.fill(x + 1, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF555555);
		g.fill(x + this.imageWidth - 1, y + 1, x + this.imageWidth, y + this.imageHeight, 0xFF555555);
		for (Slot slot : this.menu.slots) {
			int sx = x + slot.x - 1;
			int sy = y + slot.y - 1;
			g.fill(sx, sy, sx + 18, sy + 18, 0xFF8B8B8B);
		}
		if (this.minecraft.player == null) {
			return;
		}
		// level and experience
		int total = Profession.data(this.minecraft.player).exp(profession);
		int level = Profession.levelFor(total);
		String lv = Component.translatable("screen.minecraft_mode.profession.level", level, Profession.MAX_LEVEL).getString();
		g.text(this.font, lv, x + 150 - this.font.width(lv), y + 6, 0xFF000000 | profession.color, false);
		int need = Profession.expToNext(level);
		int have = Profession.progress(total);
		g.fill(x + 154, y + 8, x + 214, y + 11, 0xFF555555);
		g.fill(x + 154, y + 8, x + 154 + (level >= Profession.MAX_LEVEL ? 60 : 60 * have / Math.max(1, need)), y + 11, 0xFF000000 | profession.color);
		String pages = (this.page + 1) + "/" + this.pages();
		g.text(this.font, "◀", x + 220, y + 5, this.page > 0 ? 0xFF404040 : 0xFFA0A0A0, false);
		g.text(this.font, pages, x + 229, y + 6, 0xFF404040, false);
		g.text(this.font, "▶", x + 246, y + 5, this.page < this.pages() - 1 ? 0xFF404040 : 0xFFA0A0A0, false);

		List<CraftRecipes.Recipe> recipes = this.menu.recipes();
		for (int i = 0; i < ROWS; i++) {
			int index = this.page * ROWS + i;
			if (index >= recipes.size()) {
				break;
			}
			CraftRecipes.Recipe recipe = recipes.get(index);
			int ry = y + ROWS_Y + i * ROW_H;
			boolean unlocked = level >= recipe.level();
			int makeable = CraftMenu.makeable(this.minecraft.player, recipe);
			g.fill(x + 6, ry, x + this.imageWidth - 6, ry + ROW_H - 1, unlocked ? 0xFFB0B0B0 : 0xFF9A9A9A);
			ItemStack out = recipe.preview(this.minecraft.player);
			g.fakeItem(out, x + 8, ry + 1);
			g.itemDecorations(this.font, out, x + 8, ry + 1);
			g.text(this.font, this.font.plainSubstrByWidth(out.getHoverName().getString(), 92), x + 28, ry + 1, unlocked ? 0xFF202020 : 0xFF606060, false);
			g.text(this.font, Component.translatable("screen.minecraft_mode.profession.needs", recipe.level()), x + 28, ry + 10, unlocked ? 0xFF505050 : 0xFFB02A2A, false);
			int ix = x + ING_X;
			for (CraftRecipes.Ingredient in : recipe.ingredients()) {
				ItemStack stack = new ItemStack(in.item());
				g.fakeItem(stack, ix, ry + 1);
				int count = CraftMenu.held(this.minecraft.player, recipe, in);
				String text = in.count() > 1 ? Integer.toString(in.count()) : "";
				g.itemDecorations(this.font, stack, ix, ry + 1, text.isEmpty() ? null : text);
				if (count < in.count()) {
					g.fill(ix, ry + 16, ix + 16, ry + 17, 0xFFB02A2A);
				}
				ix += 18;
			}
			boolean one = unlocked && makeable >= 1;
			this.button(g, x + ONE_X, ry + 1, "×1", one, mouseX, mouseY);
			this.button(g, x + FIVE_X, ry + 1, "×5", one && makeable >= 1, mouseX, mouseY);
		}
	}

	private void button(final GuiGraphicsExtractor g, final int bx, final int by, final String label, final boolean active, final int mouseX, final int mouseY) {
		boolean hover = this.inside(mouseX, mouseY, bx, by, BUTTON_W, 16);
		g.fill(bx, by, bx + BUTTON_W, by + 16, !active ? 0xFF7A7A7A : hover ? 0xFF4A8A4A : 0xFF2E6A2E);
		g.centeredText(this.font, label, bx + BUTTON_W / 2, by + 4, 0xFFFFFFFF);
	}

	@Override
	protected void extractTooltip(final GuiGraphicsExtractor g, final int mouseX, final int mouseY) {
		super.extractTooltip(g, mouseX, mouseY);
		if (this.minecraft.player == null) {
			return;
		}
		List<CraftRecipes.Recipe> recipes = this.menu.recipes();
		for (int i = 0; i < ROWS; i++) {
			int index = this.page * ROWS + i;
			if (index >= recipes.size()) {
				break;
			}
			CraftRecipes.Recipe recipe = recipes.get(index);
			int ry = this.topPos + ROWS_Y + i * ROW_H;
			if (this.inside(mouseX, mouseY, this.leftPos + 6, ry, 116, ROW_H - 1)) {
				List<Component> tip = new ArrayList<>(getTooltipFromItem(this.minecraft, recipe.preview(this.minecraft.player)));
				tip.add(Component.translatable("screen.minecraft_mode.profession.exp", recipe.expFor(this.menu.profession().level(this.minecraft.player)))
					.withStyle(ChatFormatting.DARK_GREEN));
				g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
				return;
			}
			int ix = this.leftPos + ING_X;
			for (CraftRecipes.Ingredient in : recipe.ingredients()) {
				if (this.inside(mouseX, mouseY, ix, ry + 1, 16, 16)) {
					int count = CraftMenu.held(this.minecraft.player, recipe, in);
					g.setComponentTooltipForNextFrame(this.font, List.of(new ItemStack(in.item()).getHoverName(),
						Component.translatable("screen.minecraft_mode.profession.have", count, in.count()).withStyle(count >= in.count() ? ChatFormatting.GREEN : ChatFormatting.RED)),
						mouseX, mouseY);
					return;
				}
				ix += 18;
			}
		}
	}

	@Override
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		int x = this.leftPos;
		int y = this.topPos;
		if (this.inside(event.x(), event.y(), x + 218, y + 3, 10, 12) && this.page > 0) {
			this.page--;
			return true;
		}
		if (this.inside(event.x(), event.y(), x + 244, y + 3, 10, 12) && this.page < this.pages() - 1) {
			this.page++;
			return true;
		}
		List<CraftRecipes.Recipe> recipes = this.menu.recipes();
		for (int i = 0; i < ROWS; i++) {
			int index = this.page * ROWS + i;
			if (index >= recipes.size()) {
				break;
			}
			int ry = y + ROWS_Y + i * ROW_H;
			int button = this.inside(event.x(), event.y(), x + ONE_X, ry + 1, BUTTON_W, 16) ? index * 2
				: this.inside(event.x(), event.y(), x + FIVE_X, ry + 1, BUTTON_W, 16) ? index * 2 + 1 : -1;
			if (button >= 0 && this.minecraft.player != null && this.menu.clickMenuButton(this.minecraft.player, button)) {
				this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, button);
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(final double mx, final double my, final double scrollX, final double scrollY) {
		if (scrollY < 0 && this.page < this.pages() - 1) {
			this.page++;
			return true;
		}
		if (scrollY > 0 && this.page > 0) {
			this.page--;
			return true;
		}
		return super.mouseScrolled(mx, my, scrollX, scrollY);
	}
}
