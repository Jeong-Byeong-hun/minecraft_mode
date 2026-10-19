package com.minecraftmode.client.raid;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.network.RaidReportPayload;
import com.minecraftmode.raid.Raids;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * The fight report of the last raid boss defeated, Lost Ark style (opens at the victory, before the loot screen): one card per member,
 * best contribution first, with their figure (while the client can see them), name in their class colour, damage, its share and per
 * second, and a damage bar. With two or more members the first card is the MVP: lifted, framed in gold, with a banner. The loot
 * button moves on to the loot screen while a sharing runs.
 */
public class RaidReportScreen extends Screen {
	private static final int CARD_MAX_W = 84;
	/** 320 wide at most: the narrowest GUI Minecraft lays out. */
	private static final int MAX_W = 320;
	private static final int GAP = 6;
	private static final int FIGURE_H = 70;
	private static final int CARD_H = FIGURE_H + 46;
	/** The MVP card stands this much higher than the others. */
	private static final int LIFT = 6;
	private static final int HEADER = 40;
	private static final int FOOTER = 44;
	private static final int GOLD = 0xFFE8C547;

	private final RaidReportPayload report;
	private int cardW;
	private int panelW;
	private int panelH;
	private int left;
	private int top;

	public RaidReportScreen(final RaidReportPayload report) {
		super(Component.translatable("screen.minecraft_mode.raid_report.title"));
		this.report = report;
	}

	@Override
	protected void init() {
		int n = Math.max(1, this.report.entries().size());
		this.cardW = Math.min(CARD_MAX_W, (MAX_W - 24 - GAP * (n - 1)) / n);
		this.panelW = Math.max(240, n * this.cardW + GAP * (n - 1) + 24);
		this.panelH = HEADER + CARD_H + FOOTER;
		this.left = (this.width - this.panelW) / 2;
		this.top = (this.height - this.panelH) / 2;
		int by = this.top + this.panelH - 26;
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(this.left + 10, by, 60, 20).build());
		if (RaidClient.loot() != null) {
			this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.raid_report.loot").withStyle(ChatFormatting.GOLD), b -> {
				if (this.minecraft != null) {
					this.minecraft.gui.setScreen(new LootScreen());
				}
			}).bounds(this.left + this.panelW - 110, by, 100, 20).build());
		}
	}

	private boolean hasMvp() {
		return this.report.entries().size() > 1 && this.report.entries().getFirst().share() > 0.0F;
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor g, final int mouseX, final int mouseY, final float a) {
		int x = this.left;
		int y = this.top;
		g.fill(x, y, x + this.panelW, y + this.panelH, 0xE8101018);
		g.outline(x, y, this.panelW, this.panelH, GOLD);
		float seconds = Math.max(1.0F, this.report.fightTicks() / 20.0F);
		float total = 0.0F;
		float most = 0.0F;
		for (RaidReportPayload.Entry e : this.report.entries()) {
			total += e.damage();
			most = Math.max(most, e.damage());
		}
		g.text(this.font, this.title.copy().withStyle(ChatFormatting.GOLD), x + 10, y + 8, 0xFFFFFFFF, true);
		g.text(this.font, Component.translatable(this.report.bossKey()), x + 14 + this.font.width(this.title), y + 8, 0xFFAAAAAA, false);
		g.text(this.font, Component.translatable("screen.minecraft_mode.raid_report.time", Component.translatable(this.report.difficultyKey()),
			Raids.clock(this.report.fightTicks())), x + 10, y + 19, 0xFF8A8A8A, false);
		Component sum = Component.translatable("screen.minecraft_mode.raid_report.total", compact(total), compact(total / seconds));
		g.text(this.font, sum, x + this.panelW - 10 - this.font.width(sum), y + 19, 0xFFFFD27F, false);

		int n = this.report.entries().size();
		int cx = x + (this.panelW - (n * this.cardW + GAP * (n - 1))) / 2;
		for (int i = 0; i < n; i++) {
			this.card(g, this.report.entries().get(i), i, cx + i * (this.cardW + GAP), y + HEADER, total, most, seconds, mouseX, mouseY);
		}
		if (this.hasMvp()) {
			Component help = Component.translatable("screen.minecraft_mode.raid_report.help");
			g.text(this.font, this.font.plainSubstrByWidth(help.getString(), this.panelW - 20), x + 10, y + HEADER + CARD_H + 4, 0xFF8A8A8A, false);
		}
		super.extractRenderState(g, mouseX, mouseY, a);
	}

	private void card(final GuiGraphicsExtractor g, final RaidReportPayload.Entry e, final int rank, final int x, final int top, final float total,
		final float most, final float seconds, final int mouseX, final int mouseY) {
		boolean mvp = rank == 0 && this.hasMvp();
		int y = mvp ? top - LIFT : top;
		int color = 0xFF000000 | JobClass.values()[Math.floorMod(e.job(), JobClass.values().length)].color();
		g.fill(x, y, x + this.cardW, y + CARD_H, mvp ? 0x50E8C547 : 0x30FFFFFF);
		g.outline(x, y, this.cardW, CARD_H, mvp ? GOLD : 0x60FFFFFF);
		if (mvp) {
			// a second gold frame and the banner over the figure
			g.outline(x - 1, y - 1, this.cardW + 2, CARD_H + 2, GOLD);
			g.fill(x + 4, y + 3, x + this.cardW - 4, y + 13, 0xFFB8901F);
			this.centered(g, "★ MVP ★", x, y + 4, 0xFFFFF4C0);
		} else {
			g.text(this.font, "#" + (rank + 1), x + 4, y + 4, 0xFFBBBBBB, true);
		}
		Player figure = this.minecraft == null || this.minecraft.level == null ? null : this.minecraft.level.getPlayerByUUID(e.id());
		if (figure != null) {
			InventoryScreen.extractEntityInInventoryFollowsMouse(g, x + 2, y + 14, x + this.cardW - 2, y + FIGURE_H, 28, 0.0625F, mouseX, mouseY, figure);
		} else {
			this.centered(g, "?", x, y + FIGURE_H / 2, 0xFF5A5A5A);
		}
		int ty = y + FIGURE_H + 2;
		this.centered(g, this.font.plainSubstrByWidth(e.name(), this.cardW - 4), x, ty, color);
		this.centered(g, compact(e.damage()), x, ty + 11, mvp ? GOLD : 0xFFFFFFFF);
		int pct = total > 0.0F ? Math.round(e.damage() / total * 100.0F) : 0;
		this.centered(g, this.fit(Component.translatable("screen.minecraft_mode.raid_report.percent", pct)), x, ty + 21, 0xFFBBBBBB);
		this.centered(g, this.fit(Component.translatable("screen.minecraft_mode.raid_report.dps", compact(e.damage() / seconds))), x, ty + 31, 0xFF8A8A8A);
		int barY = y + CARD_H - 4;
		g.fill(x + 3, barY, x + this.cardW - 3, barY + 2, 0x60000000);
		int fill = most > 0.0F ? Math.round((this.cardW - 6) * e.damage() / most) : 0;
		g.fill(x + 3, barY, x + 3 + fill, barY + 2, color);
	}

	private String fit(final Component text) {
		return this.font.plainSubstrByWidth(text.getString(), this.cardW - 4);
	}

	private void centered(final GuiGraphicsExtractor g, final String text, final int x, final int y, final int color) {
		g.text(this.font, text, x + (this.cardW - this.font.width(text)) / 2, y, color, true);
	}

	/** 950, 12.3k, 4.56M. */
	static String compact(final float value) {
		if (value >= 1.0E6F) {
			return String.format(Locale.ROOT, "%.2fM", value / 1.0E6F);
		}
		if (value >= 1.0E4F) {
			return String.format(Locale.ROOT, "%.1fk", value / 1.0E3F);
		}
		return Integer.toString(Math.round(value));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
