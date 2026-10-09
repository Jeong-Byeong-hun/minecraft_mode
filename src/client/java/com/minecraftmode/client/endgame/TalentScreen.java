package com.minecraftmode.client.endgame;

import com.minecraftmode.client.job.JobTooltips;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.Paragon;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.network.ProgressActionPayload;
import com.minecraftmode.talent.TalentTree;
import com.minecraftmode.talent.Talents;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;

/**
 * Talents (N): the class's three branches side by side, five tiers down. Click a talent to put a point in it; a tier opens once
 * the branch holds {@value TalentTree#PER_TIER} points per tier above it. Points come from levels (two for every three levels
 * after 10); a reset costs coins. The paragon tab (초월) spends the points of paragon levels earned past level 100.
 */
public class TalentScreen extends Screen {
	private static final int W = 320;
	private static final int H = 236;
	private static final int COL_W = 100;
	private static final int NODE_H = 30;
	private static final int TIER_STEP = 34;
	private static final int NODES_Y = 40;

	private static boolean paragonTab;

	/** Opens on the paragon tab next time (also used by tests). */
	public static void showParagon(final boolean paragon) {
		paragonTab = paragon;
	}
	private Map<String, Integer> shown = Map.of();
	private Paragon.ParagonData shownParagon = Paragon.ParagonData.DEFAULT;
	private JobData shownJob = JobData.DEFAULT;
	private int left;
	private int top;

	public TalentScreen() {
		super(Component.translatable("screen.minecraft_mode.talent.title"));
	}

	@Override
	protected void init() {
		this.left = (this.width - W) / 2;
		this.top = (this.height - H) / 2;
		LocalPlayer player = this.minecraft.player;
		if (player == null) {
			return;
		}
		this.shown = Talents.ranks(player);
		this.shownJob = JobProgression.get(player);
		this.shownParagon = Paragon.get(player);
		Button talents = Button.builder(Component.translatable("screen.minecraft_mode.talent.title"), b -> {
			paragonTab = false;
			this.rebuildWidgets();
		}).bounds(this.left + W - 8 - 2 * 72, this.top + 5, 70, 16).build();
		talents.active = paragonTab;
		this.addRenderableWidget(talents);
		Button paragon = Button.builder(Component.translatable("screen.minecraft_mode.paragon.tab"), b -> {
			paragonTab = true;
			this.rebuildWidgets();
		}).bounds(this.left + W - 8 - 72, this.top + 5, 70, 16).build();
		paragon.active = !paragonTab;
		this.addRenderableWidget(paragon);
		if (paragonTab) {
			this.initParagon(player);
			return;
		}
		Button reset = Button.builder(Component.translatable("screen.minecraft_mode.talent.reset", Coins.format(Talents.resetCost(player))),
			b -> EndgameClient.progress(ProgressActionPayload.TALENT_RESET, "")).bounds(this.left + 8, this.top + H - 24, 150, 18).build();
		reset.active = Talents.spent(player) > 0 && (player.isCreative() || Coins.total(player) >= Talents.resetCost(player));
		this.addRenderableWidget(reset);
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(this.left + W - 68, this.top + H - 24, 60, 18).build());
	}

	@Override
	public void tick() {
		LocalPlayer player = this.minecraft.player;
		if (player != null && (!Talents.ranks(player).equals(this.shown) || !JobProgression.get(player).equals(this.shownJob) || !Paragon.get(player).equals(this.shownParagon))) {
			this.rebuildWidgets();
		}
	}

	private int nodeX(final int branch) {
		return this.left + 8 + branch * (COL_W + 2);
	}

	private int nodeY(final int tier) {
		return this.top + NODES_Y + tier * TIER_STEP;
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor g, final int mouseX, final int mouseY, final float a) {
		LocalPlayer player = this.minecraft.player;
		if (player == null) {
			return;
		}
		int x = this.left;
		int y = this.top;
		g.fill(x, y, x + W, y + H, 0xE8101018);
		g.outline(x, y, W, H, 0xFF5AE8F4);
		JobData job = JobProgression.get(player);
		if (paragonTab) {
			this.extractParagon(g, player, mouseX, mouseY);
			super.extractRenderState(g, mouseX, mouseY, a);
			return;
		}
		g.text(this.font, this.title.copy().withStyle(ChatFormatting.GOLD), x + 10, y + 9, 0xFFFFFFFF, true);
		if (!job.hasClass()) {
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.talent.no_class"), x + 10, y + 30, W - 20, 0xFFBBBBBB);
			super.extractRenderState(g, mouseX, mouseY, a);
			return;
		}
		Component points = Component.translatable("screen.minecraft_mode.talent.points", Talents.available(player), Talents.points(job.level()));
		g.text(this.font, points, x + 166, y + H - 19, Talents.available(player) > 0 ? 0xFF7CFC7C : 0xFFBBBBBB, false);
		g.text(this.font, Component.translatable(job.job().nameKey()), x + 10 + this.font.width(this.title) + 8, y + 9, 0xFF000000 | job.job().color(), false);

		List<TalentTree.Branch> branches = TalentTree.of(job.job());
		List<Component> tip = null;
		for (int b = 0; b < branches.size(); b++) {
			TalentTree.Branch branch = branches.get(b);
			int bx = this.nodeX(b);
			int inBranch = Talents.inBranch(player, branch);
			g.text(this.font, Component.translatable(branch.nameKey()).withStyle(ChatFormatting.BOLD), bx + 2, y + 27, 0xFF000000 | branch.color(), false);
			String count = Integer.toString(inBranch);
			g.text(this.font, count, bx + COL_W - 2 - this.font.width(count), y + 27, 0xFFBBBBBB, false);
			for (TalentTree.Node node : branch.nodes()) {
				int ny = this.nodeY(node.tier());
				int rank = Talents.rank(player, node.id());
				boolean open = inBranch >= node.tier() * TalentTree.PER_TIER;
				boolean can = Talents.canSpend(player, node);
				boolean hover = mouseX >= bx && mouseX < bx + COL_W && mouseY >= ny && mouseY < ny + NODE_H;
				int bg = !open ? 0x60000000 : rank >= node.maxRank() ? 0x80000000 | branch.color() & 0x00FFFFFF : rank > 0 ? 0x50000000 | branch.color() & 0x00FFFFFF
					: 0x40303040;
				g.fill(bx, ny, bx + COL_W, ny + NODE_H, bg);
				g.outline(bx, ny, COL_W, NODE_H, hover && can ? 0xFFFFFFFF : node.capstone() ? 0xFFE8C24A : open ? 0xFF000000 | branch.color() : 0xFF3A3A44);
				int text = open ? 0xFFFFFFFF : 0xFF707070;
				g.text(this.font, this.font.plainSubstrByWidth(Component.translatable(node.nameKey()).getString(), COL_W - 6), bx + 3, ny + 4, text, false);
				String ranks = rank + "/" + node.maxRank();
				g.text(this.font, ranks, bx + 3, ny + 17, rank >= node.maxRank() ? 0xFFFFD27F : open ? 0xFFBBBBBB : 0xFF606060, false);
				if (!open) {
					String need = Component.translatable("screen.minecraft_mode.talent.locked", node.tier() * TalentTree.PER_TIER).getString();
					g.text(this.font, need, bx + COL_W - 3 - this.font.width(need), ny + 17, 0xFF8A6A6A, false);
				} else if (can) {
					g.text(this.font, "+", bx + COL_W - 9, ny + 17, 0xFF7CFC7C, false);
				}
				if (hover) {
					tip = this.tooltip(node, rank, open);
				}
			}
		}
		super.extractRenderState(g, mouseX, mouseY, a);
		if (tip != null) {
			g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
		}
	}

	// ------------------------------------------------------------------ paragon (초월)

	private static final int ROW = 19;
	private static final int ROWS_Y = 52;

	private void initParagon(final LocalPlayer player) {
		Paragon.ParagonData data = Paragon.get(player);
		Paragon.Stat[] stats = Paragon.Stat.values();
		for (int i = 0; i < stats.length; i++) {
			Paragon.Stat stat = stats[i];
			Button plus = Button.builder(Component.literal("+"), b -> EndgameClient.progress(ProgressActionPayload.PARAGON, stat.id))
				.bounds(this.left + W - 30, this.top + ROWS_Y + i * ROW, 20, 16).build();
			plus.active = data.available() > 0 && data.rank(stat) < Paragon.MAX_RANK;
			this.addRenderableWidget(plus);
		}
		Button reset = Button.builder(Component.translatable("screen.minecraft_mode.talent.reset", Coins.format(Paragon.resetCost())),
			b -> EndgameClient.progress(ProgressActionPayload.PARAGON_RESET, "")).bounds(this.left + 8, this.top + H - 24, 150, 18).build();
		reset.active = data.spent() > 0 && (player.isCreative() || Coins.total(player) >= Paragon.resetCost());
		this.addRenderableWidget(reset);
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(this.left + W - 68, this.top + H - 24, 60, 18).build());
	}

	private void extractParagon(final GuiGraphicsExtractor g, final LocalPlayer player, final int mouseX, final int mouseY) {
		int x = this.left;
		int y = this.top;
		Paragon.ParagonData data = Paragon.get(player);
		g.text(this.font, Component.translatable("screen.minecraft_mode.paragon.title").withStyle(ChatFormatting.LIGHT_PURPLE), x + 10, y + 9, 0xFFFFFFFF, true);
		if (JobProgression.get(player).level() < JobProgression.MAX_LEVEL && data.level() == 0) {
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.paragon.locked", JobProgression.MAX_LEVEL), x + 10, y + 30, W - 20, 0xFFBBBBBB);
			return;
		}
		g.text(this.font, Component.translatable("screen.minecraft_mode.paragon.level", data.level()), x + 10, y + 26, 0xFFE0C0FF, false);
		Component points = Component.translatable("screen.minecraft_mode.talent.points", data.available(), data.level());
		g.text(this.font, points, x + W - 10 - this.font.width(points), y + 26, data.available() > 0 ? 0xFF7CFC7C : 0xFFBBBBBB, false);
		int need = Paragon.expToNext(data.level());
		g.fill(x + 10, y + 38, x + W - 10, y + 41, 0xFF2A2A30);
		g.fill(x + 10, y + 38, x + 10 + (W - 20) * Math.min(need, data.exp()) / Math.max(1, need), y + 41, 0xFFC060FF);
		Paragon.Stat[] stats = Paragon.Stat.values();
		List<Component> tip = null;
		for (int i = 0; i < stats.length; i++) {
			Paragon.Stat stat = stats[i];
			int ry = y + ROWS_Y + i * ROW;
			int rank = data.rank(stat);
			g.fill(x + 8, ry, x + W - 34, ry + 16, rank > 0 ? 0x50C060FF : 0x40000000);
			g.text(this.font, Component.translatable(stat.nameKey()), x + 12, ry + 4, 0xFFFFFFFF, false);
			g.text(this.font, rank + "/" + Paragon.MAX_RANK, x + 80, ry + 4, rank >= Paragon.MAX_RANK ? 0xFFFFD27F : 0xFFBBBBBB, false);
			String now = stat.lines(Math.max(1, rank)).stream().map(l -> Component.translatable(l.stat().key(), JobTooltips.num(rank == 0 ? 0 : l.value())).getString())
				.reduce((p, q) -> p + ", " + q).orElse("");
			g.text(this.font, this.font.plainSubstrByWidth(now, W - 160), x + 124, ry + 4, rank > 0 ? 0xFF7CFC7C : 0xFF707070, false);
			if (mouseX >= x + 8 && mouseX < x + W - 34 && mouseY >= ry && mouseY < ry + 16) {
				tip = List.of(Component.translatable(stat.nameKey()).withStyle(ChatFormatting.LIGHT_PURPLE),
					Component.translatable("screen.minecraft_mode.talent.next").withStyle(ChatFormatting.AQUA), lines(stat.lines(1), ChatFormatting.AQUA));
			}
		}
		if (tip != null) {
			g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
		}
	}

	private List<Component> tooltip(final TalentTree.Node node, final int rank, final boolean open) {
		List<Component> tip = new ArrayList<>();
		tip.add(Component.translatable(node.nameKey()).withStyle(node.capstone() ? ChatFormatting.GOLD : ChatFormatting.WHITE));
		tip.add(Component.translatable("screen.minecraft_mode.talent.rank", rank, node.maxRank()).withStyle(ChatFormatting.GRAY));
		if (rank > 0) {
			tip.add(Component.translatable("screen.minecraft_mode.talent.now").withStyle(ChatFormatting.GREEN));
			tip.add(lines(node.lines(rank), ChatFormatting.GREEN));
		}
		if (rank < node.maxRank()) {
			tip.add(Component.translatable("screen.minecraft_mode.talent.next").withStyle(ChatFormatting.AQUA));
			tip.add(lines(node.lines(rank + 1), ChatFormatting.AQUA));
		}
		if (!open) {
			tip.add(Component.translatable("screen.minecraft_mode.talent.locked_hint", node.tier() * TalentTree.PER_TIER).withStyle(ChatFormatting.RED));
		}
		return tip;
	}

	private static Component lines(final List<StatLine> lines, final ChatFormatting color) {
		MutableComponent out = Component.literal(" ");
		for (int i = 0; i < lines.size(); i++) {
			if (i > 0) {
				out.append(", ");
			}
			StatLine line = lines.get(i);
			out.append(Component.translatable(line.stat().key(), JobTooltips.num(line.value())));
		}
		return out.withStyle(color);
	}

	@Override
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		LocalPlayer player = this.minecraft.player;
		if (player != null && event.button() == 0 && !paragonTab) {
			JobData job = JobProgression.get(player);
			List<TalentTree.Branch> branches = job.hasClass() ? TalentTree.of(job.job()) : List.of();
			for (int b = 0; b < branches.size(); b++) {
				int bx = this.nodeX(b);
				for (TalentTree.Node node : branches.get(b).nodes()) {
					int ny = this.nodeY(node.tier());
					if (event.x() >= bx && event.x() < bx + COL_W && event.y() >= ny && event.y() < ny + NODE_H && Talents.canSpend(player, node)) {
						this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
						EndgameClient.progress(ProgressActionPayload.TALENT, node.id());
						return true;
					}
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
