package com.minecraftmode.client.raid;

import com.minecraftmode.loot.Coins;
import com.minecraftmode.network.LootActionPayload;
import com.minecraftmode.network.LootStatePayload;
import com.minecraftmode.network.RaidReportPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Raid loot: every lot down the left (done ones show the winner; {@link #ROWS} at a time, the wheel
 * or the scroll bar shows the rest, and the list follows the current lot), the current lot on the
 * right with its timer and either the auction (highest bid, bid buttons for 1/5/10 steps with their
 * prices, giving the lot up) or the dice round (rolls so far, roll/pass). The leader can switch the
 * current lot's mode before anyone bids or rolls; anyone can leave the whole sharing (two clicks).
 * Rebuilt whenever the server sends a new state.
 */
public class LootScreen extends Screen {
	/** 320 wide: the narrowest GUI Minecraft lays out (4:3 screens at auto GUI scale). */
	private static final int W = 320;
	private static final int H = 220;
	private static final int ROW = 22;
	/** Lot rows shown at once: they end above the fight report button. */
	static final int ROWS = 6;
	private static final int LIST_X = 8;
	private static final int LIST_Y = 24;
	private static final int LIST_W = 106;
	private static final int BAR_X = LIST_X + LIST_W + 2;

	private @Nullable LootStatePayload shown;
	private int left;
	private int top;
	/** First lot row shown. */
	private int scroll;
	/** The current lot the list last scrolled to, so it follows only when the lot changes. */
	private int followed = -1;
	private boolean leaveArmed;
	private boolean dragging;

	public LootScreen() {
		super(Component.translatable("screen.minecraft_mode.loot.title"));
	}

	private static LootStatePayload.@Nullable Lot current(final LootStatePayload state) {
		return state.current() < state.lots().size() ? state.lots().get(state.current()) : null;
	}

	@Override
	protected void init() {
		this.left = (this.width - W) / 2;
		this.top = (this.height - H) / 2;
		LootStatePayload state = RaidClient.loot();
		this.shown = state;
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(this.left + 10, this.top + H - 28, 60, 20).build());
		RaidReportPayload report = RaidClient.report();
		if (report != null) {
			this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.raid_report.open"), b -> {
				if (this.minecraft != null) {
					this.minecraft.gui.setScreen(new RaidReportScreen(report));
				}
			}).bounds(this.left + LIST_X, this.top + H - 52, LIST_W, 20).build());
		}
		if (state == null) {
			return;
		}
		if (state.current() != this.followed) {
			// keep the lot being decided in view
			this.followed = state.current();
			int at = Math.min(state.current(), state.lots().size() - 1);
			if (at < this.scroll || at >= this.scroll + ROWS) {
				this.scroll = at - ROWS / 2;
			}
		}
		this.scroll = Math.max(0, Math.min(this.maxScroll(), this.scroll));
		LootStatePayload.Lot lot = current(state);
		if (lot != null) {
			Button leave = Button.builder(this.leaveArmed ? Component.translatable("screen.minecraft_mode.loot.leave_confirm").withStyle(ChatFormatting.RED)
				: Component.translatable("screen.minecraft_mode.loot.leave"), b -> {
					if (this.leaveArmed) {
						this.leaveArmed = false;
						RaidClient.send(LootActionPayload.Action.FORFEIT, 0);
					} else {
						this.leaveArmed = true;
						this.rebuildWidgets();
					}
				}).bounds(this.left + 74, this.top + H - 28, 90, 20).build();
			leave.setTooltip(Tooltip.create(Component.translatable("screen.minecraft_mode.loot.leave_tip")));
			this.addRenderableWidget(leave);
		}
		if (lot == null || lot.state() != 1) {
			return;
		}
		int bx = this.left + 128;
		int by = this.top + H - 52;
		if (lot.mode() == 0) {
			boolean gaveUp = lot.myRoll() < 0;
			int[] steps = {1, 5, 10};
			for (int i = 0; i < steps.length; i++) {
				int s = steps[i];
				int amount = lot.bid() == 0 ? lot.start() + lot.step() * (s - 1) : lot.bid() + lot.step() * s;
				Button b = Button.builder(Component.literal("+" + Coins.format(amount)), button -> RaidClient.send(LootActionPayload.Action.BID, s))
					.bounds(bx + i * 61, by, 59, 20).build();
				b.active = !gaveUp && this.coins() >= amount && !this.isHighest(state, lot);
				this.addRenderableWidget(b);
			}
			Button giveUp = Button.builder(Component.translatable("screen.minecraft_mode.loot.give_up"), b -> RaidClient.send(LootActionPayload.Action.PASS, 0))
				.bounds(bx, by - 22, 100, 20).build();
			giveUp.active = !gaveUp && !this.isHighest(state, lot);
			this.addRenderableWidget(giveUp);
		} else {
			boolean undecided = lot.myRoll() == 0 && lot.eligible();
			Button roll = Button.builder(Component.translatable("screen.minecraft_mode.loot.roll"), b -> RaidClient.send(LootActionPayload.Action.ROLL, 0))
				.bounds(bx, by, 90, 20).build();
			roll.active = undecided;
			this.addRenderableWidget(roll);
			Button pass = Button.builder(Component.translatable("screen.minecraft_mode.loot.pass"), b -> RaidClient.send(LootActionPayload.Action.PASS, 0))
				.bounds(bx + 94, by, 70, 20).build();
			pass.active = undecided;
			this.addRenderableWidget(pass);
		}
		if (state.leader()) {
			boolean untouched = lot.bid() == 0 && lot.rolls().isEmpty();
			Button sw = Button.builder(Component.translatable(lot.mode() == 0 ? "screen.minecraft_mode.loot.switch_dice" : "screen.minecraft_mode.loot.switch_auction"),
				b -> RaidClient.send(LootActionPayload.Action.SWITCH_MODE, 0)).bounds(this.left + W - 130, this.top + H - 28, 120, 20).build();
			sw.active = untouched;
			this.addRenderableWidget(sw);
		}
	}

	private int coins() {
		LocalPlayer player = this.minecraft == null ? null : this.minecraft.player;
		return player == null ? 0 : Coins.total(player);
	}

	private boolean isHighest(final LootStatePayload state, final LootStatePayload.Lot lot) {
		LocalPlayer player = this.minecraft == null ? null : this.minecraft.player;
		return player != null && lot.bid() > 0 && lot.bidder().equals(player.getPlainTextName());
	}

	@Override
	public void tick() {
		if (RaidClient.loot() != this.shown) {
			this.rebuildWidgets();
		}
	}

	private int lotCount() {
		return this.shown == null ? 0 : this.shown.lots().size();
	}

	private int maxScroll() {
		return Math.max(0, this.lotCount() - ROWS);
	}

	@Override
	public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
		if (this.maxScroll() > 0 && scrollY != 0) {
			this.scroll = Math.max(0, Math.min(this.maxScroll(), this.scroll - (int)Math.signum(scrollY)));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		if (this.leaveArmed) {
			// the second click on the leave button leaves (and disarms); any other click only disarms it
			boolean handled = super.mouseClicked(event, doubleClick);
			if (this.leaveArmed) {
				this.leaveArmed = false;
				this.rebuildWidgets();
			}
			return handled;
		}
		if (this.maxScroll() > 0 && event.x() >= this.left + BAR_X - 1 && event.x() < this.left + BAR_X + 5 && event.y() >= this.top + LIST_Y
			&& event.y() < this.top + LIST_Y + ROWS * ROW) {
			this.dragging = true;
			this.scrollTo(event.y());
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(final MouseButtonEvent event, final double dx, final double dy) {
		if (this.dragging) {
			this.scrollTo(event.y());
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(final MouseButtonEvent event) {
		this.dragging = false;
		return super.mouseReleased(event);
	}

	private void scrollTo(final double mouseY) {
		double f = (mouseY - this.top - LIST_Y) / (ROWS * ROW);
		this.scroll = Math.max(0, Math.min(this.maxScroll(), (int)Math.round(f * this.maxScroll())));
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor g, final int mouseX, final int mouseY, final float a) {
		int x = this.left;
		int y = this.top;
		g.fill(x, y, x + W, y + H, 0xE8101018);
		g.outline(x, y, W, H, 0xFFE8C547);
		LootStatePayload state = this.shown;
		if (state == null) {
			g.text(this.font, Component.translatable("message.minecraft_mode.loot.none"), x + 10, y + 10, 0xFFAAAAAA, false);
			super.extractRenderState(g, mouseX, mouseY, a);
			return;
		}
		g.text(this.font, this.title.copy().withStyle(ChatFormatting.GOLD), x + 10, y + 8, 0xFFFFFFFF, true);
		g.text(this.font, Component.translatable(state.bossKey()), x + 10 + this.font.width(this.title) + 6, y + 8, 0xFFAAAAAA, false);
		String wallet = Component.translatable("screen.minecraft_mode.loot.coins").getString() + " " + Coins.format(this.coins());
		g.text(this.font, wallet, x + W - 10 - this.font.width(wallet), y + 8, 0xFFFFD27F, false);
		if (state.left() > 0) {
			String left = Component.translatable("screen.minecraft_mode.loot.left_count", state.left()).getString();
			g.text(this.font, left, x + W - 10 - this.font.width(left), y + 18, 0xFF8A8A8A, false);
		}

		// lots
		List<LootStatePayload.Lot> lots = state.lots();
		LootStatePayload.Lot hovered = null;
		int scroll = Math.max(0, Math.min(this.maxScroll(), this.scroll));
		for (int row = 0; row < ROWS && scroll + row < lots.size(); row++) {
			int i = scroll + row;
			LootStatePayload.Lot lot = lots.get(i);
			int ry = y + LIST_Y + row * ROW;
			boolean now = i == state.current() && lot.state() == 1;
			g.fill(x + LIST_X, ry, x + LIST_X + LIST_W, ry + ROW - 2, now ? 0x60E8C547 : 0x40000000);
			g.fakeItem(lot.stack(), x + LIST_X + 2, ry + 2);
			String line = lot.state() == 2 ? "✔ " + lot.winner() : now ? "▶ " + Component.translatable(lot.mode() == 0 ? "screen.minecraft_mode.loot.auction"
				: "screen.minecraft_mode.loot.dice").getString() : (i + 1) + ". " + Component.translatable("screen.minecraft_mode.loot.waiting").getString();
			g.text(this.font, this.font.plainSubstrByWidth(line, LIST_W - 24), x + LIST_X + 22, ry + 6, lot.state() == 2 ? 0xFF7CFC7C : now ? 0xFFFFE08A : 0xFF8A8A8A,
				false);
			if (mouseX >= x + LIST_X && mouseX < x + LIST_X + LIST_W && mouseY >= ry && mouseY < ry + ROW - 2) {
				hovered = lot;
			}
		}
		if (this.maxScroll() > 0) {
			int trackTop = y + LIST_Y;
			int trackH = ROWS * ROW - 2;
			int thumbH = Math.max(12, trackH * ROWS / lots.size());
			int thumbY = trackTop + (trackH - thumbH) * scroll / this.maxScroll();
			g.fill(x + BAR_X, trackTop, x + BAR_X + 4, trackTop + trackH, 0x60000000);
			g.fill(x + BAR_X, thumbY, x + BAR_X + 4, thumbY + thumbH, 0xFFE8C547);
		}

		// current lot
		int rx = x + 128;
		int rw = W - 138;
		LootStatePayload.Lot lot = current(state);
		if (lot == null) {
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.loot.finished"), rx, y + 30, rw, 0xFFFFD27F);
		} else {
			g.fakeItem(lot.stack(), rx, y + 26);
			g.text(this.font, this.font.plainSubstrByWidth(lot.stack().getHoverName().getString(), rw - 22), rx + 20, y + 26, 0xFFFFFFFF, true);
			Component mode = Component.translatable(lot.mode() == 0 ? "screen.minecraft_mode.loot.auction" : "screen.minecraft_mode.loot.dice");
			String time = lot.state() == 1 ? "  " + (lot.ticksLeft() + 19) / 20 + "s" : "";
			g.text(this.font, mode.getString() + time, rx + 20, y + 36, lot.mode() == 0 ? 0xFF9CC3FF : 0xFFFFB0E0, false);
			int ty = y + 52;
			if (lot.state() == 0) {
				g.text(this.font, Component.translatable("screen.minecraft_mode.loot.waiting"), rx, ty, 0xFF8A8A8A, false);
			} else if (lot.mode() == 0) {
				g.text(this.font, Component.translatable("screen.minecraft_mode.loot.start", Coins.format(lot.start()), Coins.format(lot.step())), rx, ty, 0xFFBBBBBB, false);
				ty += 12;
				Component highest = lot.bid() == 0 ? Component.translatable("screen.minecraft_mode.loot.no_bid")
					: Component.translatable("screen.minecraft_mode.loot.highest", Coins.format(lot.bid()), lot.bidder());
				g.text(this.font, highest, rx, ty, 0xFFFFD27F, false);
				ty += 14;
				g.textWithWordWrap(this.font, Component.translatable(lot.myRoll() < 0 ? "screen.minecraft_mode.loot.gave_up" : "screen.minecraft_mode.loot.auction_help"),
					rx, ty, rw, lot.myRoll() < 0 ? 0xFFFFB0B0 : 0xFF8A8A8A);
			} else {
				Component me = lot.myRoll() > 0 ? Component.translatable("screen.minecraft_mode.loot.my_roll", lot.myRoll())
					: lot.myRoll() < 0 ? Component.translatable("screen.minecraft_mode.loot.passed")
					: !lot.eligible() ? Component.translatable("screen.minecraft_mode.loot.not_eligible")
					: Component.translatable("screen.minecraft_mode.loot.roll_help");
				g.text(this.font, me, rx, ty, 0xFFFFD27F, false);
				ty += 13;
				for (String r : lot.rolls()) {
					g.text(this.font, "• " + r, rx, ty, 0xFFD0D0D0, false);
					ty += 10;
				}
			}
		}
		super.extractRenderState(g, mouseX, mouseY, a);
		if (hovered != null && this.minecraft != null) {
			List<Component> tip = new ArrayList<>(getTooltipFromItem(this.minecraft, hovered.stack()));
			if (hovered.state() == 2) {
				tip.add(Component.empty());
				tip.add(Component.translatable(hovered.price() > 0 ? "screen.minecraft_mode.loot.won_for" : "screen.minecraft_mode.loot.won_by", hovered.winner(),
					Coins.format(hovered.price())).withStyle(ChatFormatting.GREEN));
			}
			g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
		} else if (lot != null && mouseX >= rx && mouseX < rx + 16 && mouseY >= y + 26 && mouseY < y + 42 && this.minecraft != null) {
			g.setComponentTooltipForNextFrame(this.font, getTooltipFromItem(this.minecraft, lot.stack()), mouseX, mouseY);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
