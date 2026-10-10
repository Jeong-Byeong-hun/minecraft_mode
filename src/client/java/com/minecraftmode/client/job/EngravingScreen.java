package com.minecraftmode.client.job;

import com.minecraftmode.job.engrave.Engraving;
import com.minecraftmode.job.engrave.EngravingMenu;
import com.minecraftmode.job.engrave.Engravings;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.loot.Coins;
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
import org.jspecify.annotations.Nullable;

/**
 * Engraving table screen: current lines (3 on weapons, 4 on armor), each with a remove button that shows its price and takes a
 * second click, three offers to engrave with their price, a labelled reroll button, and a status row with what the player carries
 * (or why a click did nothing). Drawn with plain fills, no texture.
 */
public class EngravingScreen extends AbstractContainerScreen<EngravingMenu> {
	private static final int ROW_X = 40;
	private static final int ROW_W = 152;
	private static final int LINES_Y = 18;
	private static final int LINE_STEP = 11;
	private static final int HEADER_Y = 64;
	private static final int OFFERS_Y = 77;
	private static final int OFFER_STEP = 15;
	private static final int STATUS_Y = 125;
	/** How long a remove button stays armed for its second click, and how long a status message shows. */
	private static final long ARM_MS = 3000L;
	private static final long STATUS_MS = 3000L;

	private int armed = -1;
	private long armedAt;
	private @Nullable Component status;
	private long statusAt;

	public EngravingScreen(final EngravingMenu menu, final Inventory inventory, final Component title) {
		super(menu, inventory, title, EngravingMenu.WIDTH, EngravingMenu.HEIGHT);
		this.inventoryLabelX = EngravingMenu.INVENTORY_X;
		this.inventoryLabelY = EngravingMenu.INVENTORY_Y - 11;
	}

	private int essence() {
		return this.minecraft.player == null ? 0 : EngravingMenu.essence(this.minecraft.player.getInventory());
	}

	private int wallet() {
		return this.minecraft.player == null ? 0 : Coins.total(this.minecraft.player);
	}

	private boolean canPay(final int cost, final int coins) {
		return this.minecraft.player != null && (this.minecraft.player.isCreative() || this.essence() >= cost && this.wallet() >= coins);
	}

	/** What is missing for a price the player cannot pay. */
	private Component shortage(final int cost, final int coins) {
		int essence = Math.max(0, cost - this.essence());
		int copper = Math.max(0, coins - this.wallet());
		if (essence > 0 && copper > 0) {
			return Component.translatable("screen.minecraft_mode.engraving.short_both", essence, Coins.format(copper));
		}
		return essence > 0 ? Component.translatable("screen.minecraft_mode.engraving.short_essence", essence)
			: Component.translatable("screen.minecraft_mode.engraving.short_coins", Coins.format(copper));
	}

	private void say(final Component message) {
		this.status = message;
		this.statusAt = System.currentTimeMillis();
	}

	private boolean isArmed(final int line) {
		return this.armed == line && System.currentTimeMillis() - this.armedAt < ARM_MS;
	}

	private static String price(final int cost, final int coins) {
		return "◆" + cost + " ◎" + Coins.format(coins);
	}

	/** The remove button of line {@code i}: its label and left edge (it is right-aligned in the row). */
	private String removeLabel(final int line) {
		String head = this.isArmed(line) ? Component.translatable("screen.minecraft_mode.engraving.confirm").getString() : "✕";
		return head + " " + price(this.menu.removeCost(), this.menu.removeCoins());
	}

	private int removeX(final int line) {
		return this.leftPos + ROW_X + ROW_W - this.font.width(this.removeLabel(line)) - 6;
	}

	private String rerollLabel() {
		return "⟳ " + Component.translatable("screen.minecraft_mode.engraving.reroll_short").getString() + " " + price(this.menu.rerollCost(), this.menu.rerollCoins());
	}

	private int rerollX() {
		return this.leftPos + ROW_X + ROW_W - this.font.width(this.rerollLabel()) - 6;
	}

	private static boolean full(final @Nullable ClassGear def, final Engravings engravings) {
		return def != null && engravings.lines().size() >= def.maxLines();
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

		// current lines, each with its remove button (price always shown, a second click removes)
		int rows = def == null ? Engravings.WEAPON_LINES : def.maxLines();
		boolean removeOk = this.canPay(this.menu.removeCost(), this.menu.removeCoins());
		for (int i = 0; i < rows; i++) {
			int ry = y + LINES_Y + i * LINE_STEP;
			g.fill(x + ROW_X, ry, x + ROW_X + ROW_W, ry + 10, 0xFF2B2140);
			if (def != null && i < engravings.lines().size()) {
				int bx = this.removeX(i);
				Engraving e = Engraving.byId(engravings.lines().get(i));
				String name = (i + 1) + ". " + (e == null ? "?" : Component.translatable(e.nameKey()).getString());
				g.text(this.font, this.font.plainSubstrByWidth(name, bx - (x + ROW_X) - 6), x + ROW_X + 3, ry + 1, 0xFFE3C9FF, false);
				boolean hover = this.inside(mouseX, mouseY, bx, ry, x + ROW_X + ROW_W - bx, 10);
				int bg = !removeOk ? 0xFF4A4A4A : this.isArmed(i) ? 0xFFB03030 : hover ? 0xFF7A3030 : 0xFF4A2A2A;
				g.fill(bx, ry, x + ROW_X + ROW_W, ry + 10, bg);
				g.text(this.font, this.removeLabel(i), bx + 3, ry + 1, removeOk ? 0xFFFFC0C0 : 0xFF9A9A9A, false);
			} else {
				g.text(this.font, Component.literal((i + 1) + ". ").append(Component.translatable("screen.minecraft_mode.engraving.empty_line")), x + ROW_X + 3, ry + 1,
					0xFF6E6585, false);
			}
		}

		// header: offers label and the reroll button with its price
		g.text(this.font, Component.translatable("screen.minecraft_mode.engraving.offers"), x + ROW_X, y + HEADER_Y + 2, 0xFF404040, false);
		if (def != null && !full(def, engravings)) {
			boolean rerollOk = this.canPay(this.menu.rerollCost(), this.menu.rerollCoins());
			int bx = this.rerollX();
			boolean hover = this.inside(mouseX, mouseY, bx, y + HEADER_Y, x + ROW_X + ROW_W - bx, 11);
			g.fill(bx, y + HEADER_Y, x + ROW_X + ROW_W, y + HEADER_Y + 11, !rerollOk ? 0xFF6B6B6B : hover ? 0xFF3E7A6E : 0xFF2C5A51);
			g.text(this.font, this.rerollLabel(), bx + 3, y + HEADER_Y + 2, rerollOk ? 0xFFFFFFFF : 0xFFBDBDBD, false);
		}

		// offers
		if (def == null) {
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.engraving.insert"), x + ROW_X, y + OFFERS_Y + 2, ROW_W, 0xFF404040);
		} else if (full(def, engravings)) {
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.engraving.full"), x + ROW_X, y + OFFERS_Y + 2, ROW_W, 0xFF5A3A80);
		} else {
			boolean engraveOk = this.canPay(this.menu.engraveCost(), this.menu.engraveCoins());
			for (int i = 0; i < 3; i++) {
				int ry = y + OFFERS_Y + i * OFFER_STEP;
				Engraving offer = this.menu.offer(i);
				boolean enabled = offer != null && engraveOk;
				boolean hover = offer != null && this.inside(mouseX, mouseY, x + ROW_X, ry, ROW_W, 14);
				g.fill(x + ROW_X, ry, x + ROW_X + ROW_W, ry + 14, !enabled ? 0xFF6B6B6B : hover ? 0xFF5E3A9E : 0xFF432A73);
				if (offer != null) {
					String cost = price(this.menu.engraveCost(), this.menu.engraveCoins());
					int costX = x + ROW_X + ROW_W - 3 - this.font.width(cost);
					String name = Component.translatable(offer.nameKey()).getString();
					g.text(this.font, this.font.plainSubstrByWidth(name, costX - (x + ROW_X) - 6), x + ROW_X + 3, ry + 3, enabled ? 0xFFFFFFFF : 0xFFBDBDBD, false);
					g.text(this.font, cost, costX, ry + 3, enabled ? 0xFF7FFFD4 : 0xFFFF8080, false);
				}
			}
		}

		// status row: why the last click did nothing, or what the player carries
		if (this.status != null && System.currentTimeMillis() - this.statusAt < STATUS_MS) {
			g.text(this.font, this.status, x + 8, y + STATUS_Y, 0xFFB02A2A, false);
		} else {
			g.text(this.font, Component.translatable("screen.minecraft_mode.engraving.have", this.essence(), Coins.format(this.wallet())), x + 8, y + STATUS_Y, 0xFF1F7A68, false);
		}
	}

	@Override
	protected void extractTooltip(final GuiGraphicsExtractor g, final int mouseX, final int mouseY) {
		super.extractTooltip(g, mouseX, mouseY);
		int x = this.leftPos;
		int y = this.topPos;
		if (this.inside(mouseX, mouseY, x + 8, y + STATUS_Y - 1, ROW_X + ROW_W - 8, 10)) {
			g.setComponentTooltipForNextFrame(this.font, List.of(Component.translatable("screen.minecraft_mode.engraving.legend")), mouseX, mouseY);
			return;
		}
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
				tip.add(Component.translatable("screen.minecraft_mode.engraving.coins", Coins.format(this.menu.removeCoins())).withStyle(ChatFormatting.GOLD));
				g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
			}
		}
		if (full(def, engravings)) {
			return;
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
				tip.add(Component.translatable("screen.minecraft_mode.engraving.coins", Coins.format(this.menu.engraveCoins())).withStyle(ChatFormatting.GOLD));
				g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
			}
		}
		int bx = this.rerollX();
		if (this.inside(mouseX, mouseY, bx, y + HEADER_Y, x + ROW_X + ROW_W - bx, 11)) {
			g.setComponentTooltipForNextFrame(this.font, List.of(
				Component.translatable("screen.minecraft_mode.engraving.reroll"),
				Component.translatable("screen.minecraft_mode.engraving.cost", this.menu.rerollCost()).withStyle(ChatFormatting.AQUA),
				Component.translatable("screen.minecraft_mode.engraving.coins", Coins.format(this.menu.rerollCoins())).withStyle(ChatFormatting.GOLD)
			), mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		int x = this.leftPos;
		int y = this.topPos;
		double mx = event.x();
		double my = event.y();
		ItemStack weapon = this.menu.weapon();
		ClassGear def = ClassGear.of(weapon);
		Engravings engravings = EngravingMenu.engravings(weapon);
		if (def != null) {
			for (int i = 0; i < engravings.lines().size(); i++) {
				int bx = this.removeX(i);
				if (this.inside(mx, my, bx, y + LINES_Y + i * LINE_STEP, x + ROW_X + ROW_W - bx, 10)) {
					this.clickRemove(i);
					return true;
				}
			}
			int rx = this.rerollX();
			if (!full(def, engravings) && this.inside(mx, my, rx, y + HEADER_Y, x + ROW_X + ROW_W - rx, 11)) {
				this.armed = -1;
				this.pay(EngravingMenu.BUTTON_REROLL, this.menu.rerollCost(), this.menu.rerollCoins());
				return true;
			}
			for (int i = 0; i < 3 && !full(def, engravings); i++) {
				if (this.menu.offer(i) != null && this.inside(mx, my, x + ROW_X, y + OFFERS_Y + i * OFFER_STEP, ROW_W, 14)) {
					this.armed = -1;
					this.pay(i, this.menu.engraveCost(), this.menu.engraveCoins());
					return true;
				}
			}
		}
		this.armed = -1;
		return super.mouseClicked(event, doubleClick);
	}

	/** First click arms the line's remove button, a second one within {@link #ARM_MS} removes it (nothing is refunded). */
	private void clickRemove(final int line) {
		if (!this.canPay(this.menu.removeCost(), this.menu.removeCoins())) {
			this.say(this.shortage(this.menu.removeCost(), this.menu.removeCoins()));
			return;
		}
		if (this.isArmed(line)) {
			this.armed = -1;
			this.press(EngravingMenu.BUTTON_REMOVE + line);
			return;
		}
		this.armed = line;
		this.armedAt = System.currentTimeMillis();
		this.say(Component.translatable("screen.minecraft_mode.engraving.armed"));
		this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.6F));
	}

	/** Presses {@code button} when the player can pay, otherwise says what is missing. */
	private void pay(final int button, final int cost, final int coins) {
		if (!this.canPay(cost, coins)) {
			this.say(this.shortage(cost, coins));
			return;
		}
		this.status = null;
		this.press(button);
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
