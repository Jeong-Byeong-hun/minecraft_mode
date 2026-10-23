package com.minecraftmode.client.endgame;

import com.minecraftmode.accessory.Accessories;
import com.minecraftmode.accessory.AccessoryDef;
import com.minecraftmode.accessory.AccessoryEnhance;
import com.minecraftmode.bag.Bags;
import com.minecraftmode.client.job.JobTooltips;
import com.minecraftmode.enhance.EnhanceMenu;
import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.enhance.Imbue;
import com.minecraftmode.enhance.ImbueScrollItem;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.loot.Coins;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Artisan Brokk's bench: the gear slot on the left; on the right the next level, the success chance (with artisan's spirit), what
 * one attempt costs (green when carried), the risk from +{@value Enhancement#RISKY_FROM}, and the enhance and protected enhance
 * buttons. Hovering a button shows what the next level adds. A raid accessory shows its own panel instead: orbs and coins, the chance
 * and one enhance button (failures never drop its level).
 */
public class EnhanceScreen extends AbstractContainerScreen<EnhanceMenu> {
	private static final int PX = 40;
	private static final int PW = 152;
	private static final int BUTTON_Y = 80;
	private static final int BUTTON_W = 74;
	private static final int BUTTON_H = 16;
	private static final int IMBUE_X = 4;
	private static final int IMBUE_Y = 62;
	private static final int IMBUE_W = 34;

	public EnhanceScreen(final EnhanceMenu menu, final Inventory inventory, final Component title) {
		super(menu, inventory, title, EnhanceMenu.WIDTH, EnhanceMenu.HEIGHT);
		this.inventoryLabelX = EnhanceMenu.INVENTORY_X;
		this.inventoryLabelY = EnhanceMenu.INVENTORY_Y - 11;
	}

	private int target() {
		return Enhancement.level(this.menu.input()) + 1;
	}

	private EnhanceMenu.Cost cost() {
		ClassGear gear = ClassGear.of(this.menu.input());
		return gear == null ? null : EnhanceMenu.Cost.of(gear, this.target());
	}

	private boolean ready() {
		return ClassGear.of(this.menu.input()) != null && Enhancement.level(this.menu.input()) < Enhancement.MAX;
	}

	/** The raid accessory in the slot that can still go up a level, or null. */
	private AccessoryDef accessory() {
		AccessoryDef def = Accessories.def(this.menu.input());
		return def != null && Enhancement.level(this.menu.input()) < AccessoryEnhance.MAX ? def : null;
	}

	/** The imbue scroll the bench would put on the piece in the slot, or null. */
	private @Nullable ImbueScrollItem imbueScroll() {
		return this.minecraft.player == null || this.menu.input().isEmpty() ? null : EnhanceMenu.scrollFor(this.minecraft.player, this.menu.input());
	}

	private boolean canPay(final boolean protect) {
		return this.ready() && this.minecraft.player != null && EnhanceMenu.canPay(this.minecraft.player, ClassGear.of(this.menu.input()), this.cost(), protect);
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
		g.outline(x + EnhanceMenu.SLOT_X - 2, y + EnhanceMenu.SLOT_Y - 2, 20, 20, 0xFF2A9AB0);
		int result = this.menu.result();
		if (result != EnhanceMenu.RESULT_NONE) {
			String key = switch (result) {
				case EnhanceMenu.RESULT_SUCCESS -> "screen.minecraft_mode.enhance.result.success";
				case EnhanceMenu.RESULT_SAVED -> "screen.minecraft_mode.enhance.result.saved";
				case EnhanceMenu.RESULT_AWAKENED -> "screen.minecraft_mode.enhance.result.awakened";
				case EnhanceMenu.RESULT_IMBUED -> "screen.minecraft_mode.enhance.result.imbued";
				case EnhanceMenu.RESULT_DROP -> "screen.minecraft_mode.enhance.result.drop";
				default -> "screen.minecraft_mode.enhance.result.fail";
			};
			int color = result == EnhanceMenu.RESULT_AWAKENED || result == EnhanceMenu.RESULT_IMBUED ? 0xFFB02AB0
				: result == EnhanceMenu.RESULT_SUCCESS ? 0xFF1E8A2E : result == EnhanceMenu.RESULT_SAVED ? 0xFF2A6AB0 : 0xFFB02A2A;
			g.centeredText(this.font, Component.translatable(key), x + EnhanceMenu.SLOT_X + 8, y + EnhanceMenu.SLOT_Y + 24, color);
		}
		// an imbue scroll that fits the piece: the small button under the slot (0.5.49)
		if (this.imbueScroll() != null) {
			int ix = x + IMBUE_X;
			int iy = y + IMBUE_Y;
			boolean hover = inside(mouseX, mouseY, ix, iy, IMBUE_W, BUTTON_H);
			g.fill(ix, iy, ix + IMBUE_W, iy + BUTTON_H, hover ? 0xFFC070E0 : 0xFF8A4AB0);
			g.centeredText(this.font, Component.translatable("screen.minecraft_mode.enhance.imbue"), ix + IMBUE_W / 2, iy + 4, 0xFFFFFFFF);
		}
		if (this.minecraft.player != null) {
			String wallet = "◎" + Coins.format(Coins.total(this.minecraft.player));
			g.text(this.font, wallet, x + this.imageWidth - 6 - this.font.width(wallet), y + 6, 0xFF8A5A2A, false);
		}

		int px = x + PX;
		int ty = y + 17;
		if (this.menu.input().isEmpty()) {
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.enhance.insert"), px, ty + 2, PW, 0xFF404040);
			return;
		}
		Enhancement now = Enhancement.of(this.menu.input());
		AccessoryDef accessory = Accessories.def(this.menu.input());
		if (accessory != null) {
			this.accessoryPanel(g, x, y, px, ty, accessory, now, mouseX, mouseY);
			return;
		}
		if (now.level() >= Enhancement.MAX) {
			this.awakenPanel(g, x, y, px, ty, now, mouseX, mouseY);
			return;
		}
		int target = this.target();
		g.text(this.font, Component.literal("+" + now.level()), px, ty, darker(Enhancement.color(now.level())), false);
		int ax = px + this.font.width("+" + now.level()) + 4;
		g.text(this.font, "➜", ax, ty, 0xFF404040, false);
		g.text(this.font, Component.literal("+" + target).withStyle(ChatFormatting.BOLD), ax + 12, ty, darker(Enhancement.color(target)), false);
		int chance = Enhancement.chance(target, now.pity());
		String rate = Component.translatable("screen.minecraft_mode.enhance.chance", chance).getString();
		g.text(this.font, rate, px + PW - this.font.width(rate), ty, chance >= 100 ? 0xFF1E8A2E : chance >= 50 ? 0xFF6A6A1E : 0xFFB02A2A, false);
		ty += 10;
		if (now.pity() > 0) {
			g.text(this.font, Component.translatable("screen.minecraft_mode.enhance.pity", Enhancement.baseRate(target), now.pity()), px, ty, 0xFF2A6AB0, false);
		}
		ty += 11;

		EnhanceMenu.Cost cost = this.cost();
		Inventory inventory = this.minecraft.player.getInventory();
		boolean creative = this.minecraft.player.isCreative();
		this.costLine(g, px, ty, Component.translatable("screen.minecraft_mode.enhance.coins"), Coins.format(cost.coins()),
			creative || Coins.total(this.minecraft.player) >= cost.coins());
		ty += 9;
		int essence = EnhanceMenu.essenceHeld(inventory, cost);
		this.costLine(g, px, ty, Component.translatable(cost.condensed() ? ModItems.CONDENSED_ESSENCE.getDescriptionId() : ModItems.ESSENCE.getDescriptionId()),
			essence + "/" + cost.essence(), creative || essence >= cost.essence());
		ty += 9;
		if (cost.stones() > 0) {
			int stones = Bags.count(this.minecraft.player, ModItems.ENHANCEMENT_STONE);
			this.costLine(g, px, ty, Component.translatable(ModItems.ENHANCEMENT_STONE.getDescriptionId()), stones + "/" + cost.stones(), creative || stones >= cost.stones());
		}
		ty += 10;
		g.text(this.font, Component.translatable(Enhancement.risky(target) ? "screen.minecraft_mode.enhance.risky" : "screen.minecraft_mode.enhance.safe"), px, ty,
			Enhancement.risky(target) ? 0xFFB02A2A : 0xFF505050, false);

		this.button(g, x + PX, y + BUTTON_Y, Component.translatable("screen.minecraft_mode.enhance.button"), this.canPay(false), mouseX, mouseY);
		if (Enhancement.risky(target)) {
			int scrolls = Bags.count(this.minecraft.player, ModItems.PROTECTION_SCROLL);
			ClassGear held = ClassGear.of(this.menu.input());
			// without scrolls the button counts how many protections the ether covers
			Component count = scrolls > 0 || held == null ? Component.literal(String.valueOf(scrolls))
				: Component.translatable("screen.minecraft_mode.enhance.protected_ether", EnhanceMenu.protectionEther(this.minecraft.player.getInventory(), held) / EnhanceMenu.PROTECTION_ETHER);
			this.button(g, x + PX + PW - BUTTON_W, y + BUTTON_Y, Component.translatable("screen.minecraft_mode.enhance.protected", count), this.canPay(true), mouseX, mouseY);
		}
	}

	/** A raid accessory: the next level, its chance, coins and orbs, and the enhance button (or the fully enhanced note). */
	private void accessoryPanel(final GuiGraphicsExtractor g, final int x, final int y, final int px, int ty, final AccessoryDef def, final Enhancement now,
		final int mouseX, final int mouseY) {
		if (now.level() >= AccessoryEnhance.MAX) {
			g.text(this.font, Component.literal("+" + AccessoryEnhance.MAX).withStyle(ChatFormatting.BOLD), px, ty, darker(AccessoryEnhance.color(AccessoryEnhance.MAX)), false);
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.enhance.max"), px, ty + 12, PW, 0xFF404040);
			return;
		}
		int target = now.level() + 1;
		g.text(this.font, Component.literal("+" + now.level()), px, ty, darker(AccessoryEnhance.color(now.level())), false);
		int ax = px + this.font.width("+" + now.level()) + 4;
		g.text(this.font, "➜", ax, ty, 0xFF404040, false);
		g.text(this.font, Component.literal("+" + target).withStyle(ChatFormatting.BOLD), ax + 12, ty, darker(AccessoryEnhance.color(target)), false);
		int chance = AccessoryEnhance.chance(target, now.pity());
		String rate = Component.translatable("screen.minecraft_mode.enhance.chance", chance).getString();
		g.text(this.font, rate, px + PW - this.font.width(rate), ty, chance >= 100 ? 0xFF1E8A2E : chance >= 50 ? 0xFF6A6A1E : 0xFFB02A2A, false);
		ty += 10;
		if (now.pity() > 0) {
			g.text(this.font, Component.translatable("screen.minecraft_mode.enhance.pity", AccessoryEnhance.baseRate(target), now.pity()), px, ty, 0xFF2A6AB0, false);
		}
		ty += 11;
		boolean creative = this.minecraft.player.isCreative();
		int coins = AccessoryEnhance.coins(def, target);
		this.costLine(g, px, ty, Component.translatable("screen.minecraft_mode.enhance.coins"), Coins.format(coins), creative || Coins.total(this.minecraft.player) >= coins);
		ty += 9;
		int orbs = Bags.count(this.minecraft.player, ModItems.ACCESSORY_ORB);
		this.costLine(g, px, ty, Component.translatable(ModItems.ACCESSORY_ORB.getDescriptionId()), orbs + "/" + AccessoryEnhance.orbs(target),
			creative || orbs >= AccessoryEnhance.orbs(target));
		ty += 19;
		g.text(this.font, Component.translatable("screen.minecraft_mode.enhance.safe"), px, ty, 0xFF505050, false);
		this.button(g, x + PX, y + BUTTON_Y, Component.translatable("screen.minecraft_mode.enhance.button"), EnhanceMenu.canPayAccessory(this.minecraft.player, def, target),
			mouseX, mouseY);
	}

	/** +15 gear: the awakening step, its cost and the button (or the fully awakened note). */
	private void awakenPanel(final GuiGraphicsExtractor g, final int x, final int y, final int px, int ty, final Enhancement now, final int mouseX, final int mouseY) {
		g.text(this.font, Component.literal("+" + Enhancement.MAX + (now.awaken() > 0 ? " ✦" + now.awaken() : "")).withStyle(ChatFormatting.BOLD), px, ty, 0xFFB02AB0, false);
		if (now.awaken() >= Enhancement.MAX_AWAKEN) {
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.enhance.max"), px, ty + 12, PW, 0xFF404040);
			return;
		}
		ClassGear gear = ClassGear.of(this.menu.input());
		int target = now.awaken() + 1;
		g.text(this.font, Component.translatable("screen.minecraft_mode.enhance.awaken_to", target, Enhancement.MAX_AWAKEN), px, ty + 11, 0xFF6A2A8A, false);
		ty += 25;
		boolean creative = this.minecraft.player.isCreative();
		int coins = Enhancement.awakenCoins(gear, target);
		this.costLine(g, px, ty, Component.translatable("screen.minecraft_mode.enhance.coins"), Coins.format(coins), creative || Coins.total(this.minecraft.player) >= coins);
		ty += 9;
		int crystals = Bags.count(this.minecraft.player, ModItems.AWAKENING_CRYSTAL);
		this.costLine(g, px, ty, Component.translatable(ModItems.AWAKENING_CRYSTAL.getDescriptionId()), crystals + "/" + Enhancement.crystals(target),
			creative || crystals >= Enhancement.crystals(target));
		ty += 11;
		g.text(this.font, Component.translatable("screen.minecraft_mode.enhance.awaken_safe"), px, ty, 0xFF505050, false);
		this.button(g, x + PX, y + BUTTON_Y, Component.translatable("screen.minecraft_mode.enhance.awaken"), EnhanceMenu.canPayAwaken(this.minecraft.player, gear, target),
			mouseX, mouseY);
	}

	private static int darker(final int rgb) {
		int r = (rgb >> 16 & 0xFF) * 3 / 5;
		int gr = (rgb >> 8 & 0xFF) * 3 / 5;
		int b = (rgb & 0xFF) * 3 / 5;
		return 0xFF000000 | r << 16 | gr << 8 | b;
	}

	private void costLine(final GuiGraphicsExtractor g, final int x, final int y, final Component name, final String value, final boolean ok) {
		g.text(this.font, name, x, y, 0xFF404040, false);
		g.text(this.font, value, x + PW - this.font.width(value), y, ok ? 0xFF1E8A2E : 0xFFB02A2A, false);
	}

	private void button(final GuiGraphicsExtractor g, final int x, final int y, final Component label, final boolean active, final int mouseX, final int mouseY) {
		boolean hover = inside(mouseX, mouseY, x, y, BUTTON_W, BUTTON_H);
		g.fill(x, y, x + BUTTON_W, y + BUTTON_H, !active ? 0xFF6B6B6B : hover ? 0xFF3AA8C0 : 0xFF2A7A90);
		g.centeredText(this.font, label, x + BUTTON_W / 2, y + 4, 0xFFFFFFFF);
	}

	@Override
	protected void extractTooltip(final GuiGraphicsExtractor g, final int mouseX, final int mouseY) {
		super.extractTooltip(g, mouseX, mouseY);
		ImbueScrollItem scroll = this.imbueScroll();
		if (scroll != null && inside(mouseX, mouseY, this.leftPos + IMBUE_X, this.topPos + IMBUE_Y, IMBUE_W, BUTTON_H)) {
			Imbue imbue = new Imbue(scroll.slot(), scroll.grade());
			List<Component> tip = new ArrayList<>();
			tip.add(Component.translatable("screen.minecraft_mode.enhance.imbue_title", new ItemStack(scroll).getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE));
			tip.add(Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY)
				.append(Component.translatable(imbue.slot().stat().key(), JobTooltips.num(imbue.line().value())).withStyle(ChatFormatting.GREEN)));
			if (Imbue.of(this.menu.input()) != null) {
				tip.add(Component.translatable("screen.minecraft_mode.enhance.imbue_replace").withStyle(ChatFormatting.YELLOW));
			}
			tip.add(Component.translatable("screen.minecraft_mode.enhance.imbue_note").withStyle(ChatFormatting.DARK_GRAY));
			g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
			return;
		}
		AccessoryDef accessory = this.accessory();
		if (accessory != null && inside(mouseX, mouseY, this.leftPos + PX, this.topPos + BUTTON_Y, BUTTON_W, BUTTON_H)) {
			int target = Enhancement.level(this.menu.input()) + 1;
			List<Component> tip = new ArrayList<>();
			tip.add(Component.translatable("screen.minecraft_mode.enhance.next", target).withColor(AccessoryEnhance.color(target)));
			tip.add(Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY).append(Component.translatable("tooltip.minecraft_mode.accessory.enhanced",
				JobTooltips.num(Math.round(AccessoryEnhance.PER_LEVEL * target * 100.0F))).withStyle(ChatFormatting.GREEN)));
			for (StatLine line : AccessoryEnhance.bonus(target)) {
				tip.add(Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY)
					.append(Component.translatable(line.stat().key(), JobTooltips.num(line.value())).withStyle(ChatFormatting.GREEN)));
			}
			g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
			return;
		}
		ClassGear gear = ClassGear.of(this.menu.input());
		if (gear != null && this.menu.canAwaken() && inside(mouseX, mouseY, this.leftPos + PX, this.topPos + BUTTON_Y, BUTTON_W, BUTTON_H)) {
			int target = Enhancement.of(this.menu.input()).awaken() + 1;
			List<Component> tip = new ArrayList<>();
			tip.add(Component.translatable("screen.minecraft_mode.enhance.awaken_next", target).withColor(0xFF55FF));
			for (StatLine line : Enhancement.lines(gear, 0, target)) {
				tip.add(Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY)
					.append(Component.translatable(line.stat().key(gear.job()), JobTooltips.num(line.value())).withStyle(ChatFormatting.LIGHT_PURPLE)));
			}
			g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
			return;
		}
		if (!this.ready() || gear == null) {
			return;
		}
		int bx = this.leftPos + PX;
		int by = this.topPos + BUTTON_Y;
		if (inside(mouseX, mouseY, bx, by, PW, BUTTON_H)) {
			int target = this.target();
			List<Component> tip = new ArrayList<>();
			tip.add(Component.translatable("screen.minecraft_mode.enhance.next", target).withColor(Enhancement.color(target)));
			for (StatLine line : Enhancement.lines(gear, target)) {
				EngraveStat stat = line.stat();
				tip.add(Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY)
					.append(Component.translatable(stat.key(gear.job()), JobTooltips.num(line.value())).withStyle(ChatFormatting.GREEN)));
			}
			if (Enhancement.risky(target)) {
				tip.add(Component.translatable("screen.minecraft_mode.enhance.protect_hint").withStyle(ChatFormatting.GRAY));
			}
			g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		int bx = this.leftPos + PX;
		int by = this.topPos + BUTTON_Y;
		if (this.imbueScroll() != null && inside(event.x(), event.y(), this.leftPos + IMBUE_X, this.topPos + IMBUE_Y, IMBUE_W, BUTTON_H)
			&& this.menu.clickMenuButton(this.minecraft.player, EnhanceMenu.BUTTON_IMBUE)) {
			this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, EnhanceMenu.BUTTON_IMBUE);
			return true;
		}
		if (this.menu.canAwaken() && this.minecraft.player != null && inside(event.x(), event.y(), bx, by, BUTTON_W, BUTTON_H)
			&& this.menu.clickMenuButton(this.minecraft.player, EnhanceMenu.BUTTON_AWAKEN)) {
			this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, EnhanceMenu.BUTTON_AWAKEN);
			return true;
		}
		if (this.accessory() != null && this.minecraft.player != null && inside(event.x(), event.y(), bx, by, BUTTON_W, BUTTON_H)
			&& this.menu.clickMenuButton(this.minecraft.player, EnhanceMenu.BUTTON_ENHANCE)) {
			this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, EnhanceMenu.BUTTON_ENHANCE);
			return true;
		}
		if (this.ready() && this.minecraft.player != null) {
			int button = -1;
			if (inside(event.x(), event.y(), bx, by, BUTTON_W, BUTTON_H)) {
				button = EnhanceMenu.BUTTON_ENHANCE;
			} else if (Enhancement.risky(this.target()) && inside(event.x(), event.y(), bx + PW - BUTTON_W, by, BUTTON_W, BUTTON_H)) {
				button = EnhanceMenu.BUTTON_PROTECTED;
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
