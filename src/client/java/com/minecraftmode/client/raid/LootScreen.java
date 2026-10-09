package com.minecraftmode.client.raid;

import com.minecraftmode.loot.Coins;
import com.minecraftmode.network.LootActionPayload;
import com.minecraftmode.network.LootStatePayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Raid loot: every lot down the left (done ones show the winner), the current lot on the right with
 * its timer and either the auction (highest bid, bid buttons for 1/5/10 steps with their prices) or
 * the dice round (rolls so far, roll/pass). The leader can switch the current lot's mode before
 * anyone bids or rolls. Rebuilt whenever the server sends a new state.
 */
public class LootScreen extends Screen {
	/** 320 wide: the narrowest GUI Minecraft lays out (4:3 screens at auto GUI scale). */
	private static final int W = 320;
	private static final int H = 220;
	private static final int ROW = 22;

	private @Nullable LootStatePayload shown;
	private int left;
	private int top;

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
		if (state == null) {
			return;
		}
		LootStatePayload.Lot lot = current(state);
		if (lot == null || lot.state() != 1) {
			return;
		}
		int bx = this.left + 128;
		int by = this.top + H - 52;
		if (lot.mode() == 0) {
			int[] steps = {1, 5, 10};
			for (int i = 0; i < steps.length; i++) {
				int s = steps[i];
				int amount = lot.bid() == 0 ? lot.start() + lot.step() * (s - 1) : lot.bid() + lot.step() * s;
				Button b = Button.builder(Component.literal("+" + Coins.format(amount)), button -> RaidClient.send(LootActionPayload.Action.BID, s))
					.bounds(bx + i * 61, by, 59, 20).build();
				b.active = this.coins() >= amount && !this.isHighest(state, lot);
				this.addRenderableWidget(b);
			}
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

		// lots
		List<LootStatePayload.Lot> lots = state.lots();
		LootStatePayload.Lot hovered = null;
		for (int i = 0; i < lots.size() && i < 8; i++) {
			LootStatePayload.Lot lot = lots.get(i);
			int ry = y + 24 + i * ROW;
			boolean now = i == state.current() && lot.state() == 1;
			g.fill(x + 8, ry, x + 118, ry + ROW - 2, now ? 0x60E8C547 : 0x40000000);
			g.fakeItem(lot.stack(), x + 10, ry + 2);
			String line = lot.state() == 2 ? "✔ " + lot.winner() : now ? "▶ " + Component.translatable(lot.mode() == 0 ? "screen.minecraft_mode.loot.auction"
				: "screen.minecraft_mode.loot.dice").getString() : Component.translatable("screen.minecraft_mode.loot.waiting").getString();
			g.text(this.font, this.font.plainSubstrByWidth(line, 86), x + 30, ry + 6, lot.state() == 2 ? 0xFF7CFC7C : now ? 0xFFFFE08A : 0xFF8A8A8A, false);
			if (mouseX >= x + 8 && mouseX < x + 118 && mouseY >= ry && mouseY < ry + ROW - 2) {
				hovered = lot;
			}
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
				g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.loot.auction_help"), rx, ty, rw, 0xFF8A8A8A);
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
