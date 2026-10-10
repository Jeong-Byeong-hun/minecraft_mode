package com.minecraftmode.client.guide;

import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.network.OpenNoticePayload;
import com.minecraftmode.progress.PlayerRecords;
import com.minecraftmode.progress.ResetCycle;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.RaidAffix;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.raid.RaidDifficulty;
import com.minecraftmode.raid.RaidRecordsData;
import com.minecraftmode.raid.Raids;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The town crier's notice board: when the day and the three-day cycle renew, what rises at the next dusk (a titan or an invasion)
 * and what is going on now, this cycle's raid modifiers (hover for details) and the fastest clear of every raid boss on the hardest
 * difficulty anyone has beaten. Countdowns keep running while the board is open.
 */
public class NoticeScreen extends Screen {
	private static final int W = 340;
	private static final int H = 232;
	private static final int LINE = 11;

	private final OpenNoticePayload notice;
	private int left;
	private int top;
	private int age;

	public NoticeScreen(final OpenNoticePayload notice) {
		super(Component.translatable("screen.minecraft_mode.notice.title"));
		this.notice = notice;
	}

	@Override
	protected void init() {
		this.left = (this.width - W) / 2;
		this.top = (this.height - H) / 2;
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(this.left + W - 68, this.top + H - 22, 60, 18).build());
	}

	@Override
	public void tick() {
		this.age++;
	}

	private long left(final long ticks) {
		return Math.max(0L, ticks - this.age);
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor g, final int mouseX, final int mouseY, final float a) {
		int x = this.left;
		int y = this.top;
		g.fill(x, y, x + W, y + H, 0xE8101018);
		g.outline(x, y, W, H, 0xFFB02A2A);
		g.text(this.font, this.title.copy().withStyle(ChatFormatting.GOLD), x + 10, y + 9, 0xFFFFFFFF, true);
		int tx = x + 12;
		int ty = y + 26;
		// resets
		g.text(this.font, Component.translatable("screen.minecraft_mode.notice.day", ResetCycle.remaining(this.left(this.notice.dayLeft()))), tx, ty, 0xFFDDDDDD, false);
		ty += LINE;
		g.text(this.font, Component.translatable("screen.minecraft_mode.notice.cycle", ResetCycle.remaining(this.left(this.notice.cycleLeft()))), tx, ty, 0xFFDDDDDD, false);
		ty += LINE + 4;
		// tonight and now
		Component next = Component.translatable(this.notice.invasionNext() ? "screen.minecraft_mode.notice.next_invasion" : "screen.minecraft_mode.notice.next_titan",
			ResetCycle.remaining(this.left(this.notice.eventLeft())));
		g.text(this.font, next, tx, ty, this.notice.invasionNext() ? 0xFFFF7070 : 0xFFE0A0FF, false);
		ty += LINE;
		NamedDef titan = this.notice.titan().isEmpty() ? null : NamedMobs.byId(this.notice.titan());
		Component now;
		if (this.notice.invasionWave() > 0) {
			now = Component.translatable("screen.minecraft_mode.notice.invasion_now", this.notice.invasionWave()).withStyle(ChatFormatting.RED);
		} else if (titan != null) {
			now = Component.translatable("screen.minecraft_mode.notice.titan_now", Component.translatable(titan.nameKey()), this.notice.titanX(), this.notice.titanZ())
				.withStyle(ChatFormatting.LIGHT_PURPLE);
		} else {
			now = Component.translatable("screen.minecraft_mode.notice.quiet").withStyle(ChatFormatting.GRAY);
		}
		g.text(this.font, now, tx, ty, 0xFFFFFFFF, false);
		ty += LINE + 4;
		// raid modifiers
		g.text(this.font, Component.translatable("screen.minecraft_mode.notice.affixes").withStyle(ChatFormatting.GOLD), tx, ty, 0xFFFFFFFF, false);
		int ax = tx + this.font.width(Component.translatable("screen.minecraft_mode.notice.affixes")) + 6;
		List<Component> tip = null;
		for (String id : this.notice.affixes()) {
			RaidAffix affix = RaidAffix.byId(id);
			if (affix == null) {
				continue;
			}
			Component name = Component.translatable(affix.nameKey());
			int w = this.font.width(name);
			g.text(this.font, name, ax, ty, 0xFFC08AFF, false);
			if (mouseX >= ax && mouseX < ax + w && mouseY >= ty - 1 && mouseY < ty + 9) {
				tip = List.of(name.copy().withStyle(ChatFormatting.LIGHT_PURPLE), Component.translatable(affix.descKey()).withStyle(ChatFormatting.GRAY));
			}
			ax += w + 10;
		}
		ty += LINE + 4;
		// fastest raids
		g.text(this.font, Component.translatable("screen.minecraft_mode.notice.records").withStyle(ChatFormatting.GOLD), tx, ty, 0xFFFFFFFF, false);
		ty += LINE;
		boolean any = false;
		for (BossDef boss : RaidBosses.all()) {
			RaidDifficulty hardest = null;
			RaidRecordsData.Entry best = null;
			for (RaidDifficulty difficulty : RaidDifficulty.values()) {
				List<RaidRecordsData.Entry> list = this.notice.records().getOrDefault(PlayerRecords.raidKey(boss.id(), difficulty.id()), List.of());
				if (!list.isEmpty()) {
					hardest = difficulty;
					best = list.getFirst();
				}
			}
			if (best == null) {
				continue;
			}
			any = true;
			String names = String.join(", ", best.names());
			Component line = Component.empty().append(Component.translatable(boss.nameKey()).withStyle(ChatFormatting.YELLOW))
				.append(Component.literal(" · ")).append(Component.translatable(hardest.nameKey()))
				.append(Component.literal(" · " + Raids.clock(best.ticks()) + " · " + names));
			g.text(this.font, this.font.plainSubstrByWidth(line.getString(), W - 30), tx + 4, ty, 0xFFCCCCCC, false);
			ty += LINE;
		}
		if (!any) {
			g.text(this.font, Component.translatable("screen.minecraft_mode.notice.no_records").withStyle(ChatFormatting.GRAY), tx + 4, ty, 0xFFFFFFFF, false);
		}
		super.extractRenderState(g, mouseX, mouseY, a);
		if (tip != null) {
			g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
