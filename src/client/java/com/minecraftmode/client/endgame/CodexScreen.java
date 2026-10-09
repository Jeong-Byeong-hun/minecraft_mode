package com.minecraftmode.client.endgame;

import com.minecraftmode.client.job.JobTooltips;
import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.network.ProgressActionPayload;
import com.minecraftmode.progress.Achievements;
import com.minecraftmode.progress.CollectionBonuses;
import com.minecraftmode.progress.PlayerRecords;
import com.minecraftmode.progress.Progress;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.worldgen.lair.LairDef;
import com.minecraftmode.worldgen.lair.NamedLairs;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The adventurer's codex (J): the named monsters and lairs met so far (with the collection bonuses they give), every achievement
 * with its progress and merit, and the titles to wear.
 */
public class CodexScreen extends Screen {
	private static final int W = 320;
	private static final int H = 236;
	private static final int CELL = 24;
	private static final int PER_ROW = 12;
	private static final int ROW = 21;
	private static final int ROWS = 8;
	private static final int TITLE_ROW = 18;
	private static final int TITLE_ROWS = 10;
	private static final int TITLE_COL = 154;

	enum Tab {
		CODEX, ACHIEVEMENTS, TITLES
	}

	private static Tab tab = Tab.CODEX;
	private static int page;

	private PlayerRecords shown = PlayerRecords.DEFAULT;
	private int left;
	private int top;

	public CodexScreen() {
		super(Component.translatable("screen.minecraft_mode.codex.title"));
	}

	/** Opens the codex on tab {@code name} (codex, achievements, titles) next time (also used by tests). */
	public static void showTab(final String name) {
		tab = Tab.valueOf(name.toUpperCase(Locale.ROOT));
		page = 0;
	}

	@Override
	protected void init() {
		this.left = (this.width - W) / 2;
		this.top = (this.height - H) / 2;
		LocalPlayer player = this.minecraft.player;
		if (player == null) {
			return;
		}
		PlayerRecords records = Progress.get(player);
		this.shown = records;
		Tab[] tabs = Tab.values();
		for (int i = 0; i < tabs.length; i++) {
			Tab t = tabs[i];
			Button b = Button.builder(Component.translatable("screen.minecraft_mode.codex.tab." + t.name().toLowerCase(Locale.ROOT)), button -> {
				tab = t;
				page = 0;
				this.rebuildWidgets();
			}).bounds(this.left + W - 8 - (tabs.length - i) * 72, this.top + 5, 70, 16).build();
			b.active = t != tab;
			this.addRenderableWidget(b);
		}
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(this.left + W - 68, this.top + H - 22, 60, 18).build());
		if (tab == Tab.ACHIEVEMENTS) {
			int pages = Math.max(1, (Achievements.all().size() + ROWS - 1) / ROWS);
			page = Math.min(page, pages - 1);
			Button prev = Button.builder(Component.literal("◀"), b -> {
				page--;
				this.rebuildWidgets();
			}).bounds(this.left + 8, this.top + H - 22, 20, 18).build();
			prev.active = page > 0;
			this.addRenderableWidget(prev);
			Button next = Button.builder(Component.literal("▶"), b -> {
				page++;
				this.rebuildWidgets();
			}).bounds(this.left + 74, this.top + H - 22, 20, 18).build();
			next.active = page < pages - 1;
			this.addRenderableWidget(next);
		} else if (tab == Tab.TITLES) {
			List<Achievements.Achievement> titled = titled();
			for (int i = 0; i < titled.size(); i++) {
				Achievements.Achievement ach = titled.get(i);
				boolean wearing = records.title().equals(ach.id());
				Button wear = Button.builder(Component.translatable(wearing ? "screen.minecraft_mode.codex.worn" : "screen.minecraft_mode.codex.wear"),
					b -> EndgameClient.progress(ProgressActionPayload.TITLE, ach.id()))
					.bounds(this.left + 8 + i / TITLE_ROWS * TITLE_COL + 108, this.top + 28 + i % TITLE_ROWS * TITLE_ROW, 42, 16).build();
				wear.active = records.has(ach.id()) && !wearing;
				this.addRenderableWidget(wear);
			}
			Button off = Button.builder(Component.translatable("screen.minecraft_mode.codex.take_off"), b -> EndgameClient.progress(ProgressActionPayload.TITLE, ""))
				.bounds(this.left + 8, this.top + H - 22, 90, 18).build();
			off.active = !records.title().isEmpty();
			this.addRenderableWidget(off);
		}
	}

	private static List<Achievements.Achievement> titled() {
		return Achievements.all().stream().filter(Achievements.Achievement::hasTitle).toList();
	}

	@Override
	public void tick() {
		LocalPlayer player = this.minecraft.player;
		if (player != null && !Progress.get(player).equals(this.shown)) {
			this.rebuildWidgets();
		}
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
		g.outline(x, y, W, H, 0xFFE8C24A);
		g.text(this.font, this.title.copy().withStyle(ChatFormatting.GOLD), x + 10, y + 9, 0xFFFFFFFF, true);
		List<Component> tip = switch (tab) {
			case CODEX -> this.codex(g, player, mouseX, mouseY);
			case ACHIEVEMENTS -> this.achievements(g, player, mouseX, mouseY);
			case TITLES -> this.titles(g, player, mouseX, mouseY);
		};
		super.extractRenderState(g, mouseX, mouseY, a);
		if (tip != null) {
			g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
		}
	}

	private List<Component> codex(final GuiGraphicsExtractor g, final LocalPlayer player, final int mouseX, final int mouseY) {
		int x = this.left;
		int y = this.top;
		PlayerRecords records = Progress.get(player);
		List<Component> tip = null;
		List<NamedDef> named = new ArrayList<>(NamedMobs.all());
		int ty = y + 28;
		g.text(this.font, Component.translatable("screen.minecraft_mode.codex.named", CollectionBonuses.masteredKinds(records), named.size(), CollectionBonuses.KILLS), x + 10,
			ty, 0xFFFFD27F, false);
		ty += 11;
		for (int i = 0; i < named.size(); i++) {
			NamedDef def = named.get(i);
			int cx = x + 10 + i % PER_ROW * CELL;
			int cy = ty + i / PER_ROW * CELL;
			int kills = records.kills(def.id());
			boolean mastered = kills >= CollectionBonuses.KILLS;
			g.fill(cx, cy, cx + CELL - 2, cy + CELL - 2, kills > 0 ? 0x50FFFFFF : 0x40000000);
			if (mastered) {
				g.outline(cx, cy, CELL - 2, CELL - 2, 0xFFE8C24A);
			}
			g.fakeItem(new ItemStack(NamedMobs.egg(def)), cx + 3, cy + 3);
			if (kills == 0) {
				g.fill(cx + 1, cy + 1, cx + CELL - 3, cy + CELL - 3, 0xC0101018);
				g.centeredText(this.font, "?", cx + CELL / 2 - 1, cy + 7, 0xFF6A6A6A);
			}
			if (mouseX >= cx && mouseX < cx + CELL - 2 && mouseY >= cy && mouseY < cy + CELL - 2) {
				tip = kills == 0 ? List.of(Component.translatable("screen.minecraft_mode.codex.unknown").withStyle(ChatFormatting.GRAY),
					Component.translatable("screen.minecraft_mode.codex.level_range", def.lo(), def.hi()).withStyle(ChatFormatting.DARK_GRAY))
					: List.of(Component.translatable(def.nameKey()).withStyle(mastered ? ChatFormatting.GOLD : ChatFormatting.WHITE),
						Component.translatable("screen.minecraft_mode.codex.level_range", def.lo(), def.hi()).withStyle(ChatFormatting.GRAY),
						Component.translatable("screen.minecraft_mode.codex.kills", kills, CollectionBonuses.KILLS).withStyle(mastered ? ChatFormatting.GREEN : ChatFormatting.YELLOW),
						Component.translatable(def.descKey()).withStyle(ChatFormatting.DARK_GRAY));
			}
		}
		ty += (named.size() + PER_ROW - 1) / PER_ROW * CELL + 4;

		List<LairDef> lairs = new ArrayList<>(NamedLairs.all());
		g.text(this.font, Component.translatable("screen.minecraft_mode.codex.lairs", CollectionBonuses.lairKinds(records), lairs.size()), x + 10, ty, 0xFFFFD27F, false);
		ty += 11;
		for (int i = 0; i < lairs.size(); i++) {
			LairDef def = lairs.get(i);
			int cx = x + 10 + i % PER_ROW * CELL;
			int cy = ty + i / PER_ROW * CELL;
			int clears = records.lairClears(def.id());
			g.fill(cx, cy, cx + CELL - 2, cy + CELL - 2, clears > 0 ? 0x50FFFFFF : 0x40000000);
			if (clears > 0) {
				g.outline(cx, cy, CELL - 2, CELL - 2, 0xFF5AB0E8);
				g.fakeItem(new ItemStack(ModItems.LAIR_CACHE), cx + 3, cy + 3);
			} else {
				g.centeredText(this.font, "?", cx + CELL / 2 - 1, cy + 7, 0xFF6A6A6A);
			}
			if (mouseX >= cx && mouseX < cx + CELL - 2 && mouseY >= cy && mouseY < cy + CELL - 2) {
				tip = clears == 0 ? List.of(Component.translatable("screen.minecraft_mode.codex.unknown_lair").withStyle(ChatFormatting.GRAY))
					: List.of(Component.translatable(def.nameKey()).withStyle(ChatFormatting.AQUA),
						Component.translatable("screen.minecraft_mode.codex.clears", clears).withStyle(ChatFormatting.GREEN));
			}
		}
		ty += (lairs.size() + PER_ROW - 1) / PER_ROW * CELL + 4;

		g.text(this.font, Component.translatable("screen.minecraft_mode.codex.bonuses"), x + 10, ty, 0xFFC08AFF, false);
		ty += 11;
		List<StatLine> bonuses = CollectionBonuses.lines(records);
		if (bonuses.isEmpty()) {
			g.text(this.font, Component.translatable("screen.minecraft_mode.codex.no_bonus"), x + 14, ty, 0xFF6A6A6A, false);
		} else {
			for (StatLine bonus : bonuses) {
				g.text(this.font, Component.literal("• ").append(Component.translatable(bonus.stat().key(), JobTooltips.num(bonus.value()))), x + 14, ty, 0xFF7CFC7C, false);
				ty += 10;
			}
		}
		return tip;
	}

	private List<Component> achievements(final GuiGraphicsExtractor g, final LocalPlayer player, final int mouseX, final int mouseY) {
		int x = this.left;
		int y = this.top;
		Achievements.State state = Progress.state(player);
		List<Achievements.Achievement> all = Achievements.all();
		long done = all.stream().filter(ach -> state.records().has(ach.id())).count();
		g.text(this.font, Component.translatable("screen.minecraft_mode.codex.achieved", done, all.size()), x + 10, y + 26, 0xFFFFD27F, false);
		List<Component> tip = null;
		for (int i = 0; i < ROWS; i++) {
			int index = page * ROWS + i;
			if (index >= all.size()) {
				break;
			}
			Achievements.Achievement ach = all.get(index);
			boolean has = state.records().has(ach.id());
			int ry = y + 38 + i * ROW;
			g.fill(x + 8, ry, x + W - 8, ry + ROW - 2, has ? 0x40E8C24A : 0x40000000);
			g.text(this.font, (has ? "✔ " : "") + Component.translatable(ach.nameKey()).getString(), x + 12, ry + 2, has ? 0xFFFFD27F : 0xFFFFFFFF, false);
			String reward = "★" + ach.merit();
			g.text(this.font, reward, x + W - 12 - this.font.width(reward), ry + 2, 0xFFFFD27F, false);
			int progress = Math.min(ach.goal(), ach.progress(state));
			String count = progress + "/" + ach.goal();
			int barX = x + 12;
			int barW = 150;
			g.fill(barX, ry + 13, barX + barW, ry + 16, 0xFF2A2A30);
			g.fill(barX, ry + 13, barX + barW * progress / Math.max(1, ach.goal()), ry + 16, has ? 0xFFE8C24A : 0xFF5AB0E8);
			g.text(this.font, count, barX + barW + 6, ry + 10, 0xFFBBBBBB, false);
			if (ach.hasTitle()) {
				String title = "「" + Component.translatable(ach.titleKey()).getString() + "」";
				g.text(this.font, this.font.plainSubstrByWidth(title, 100), x + W - 12 - Math.min(100, this.font.width(title)), ry + 10, 0xFFC08AFF, false);
			}
			if (mouseX >= x + 8 && mouseX < x + W - 8 && mouseY >= ry && mouseY < ry + ROW - 2) {
				List<Component> lines = new ArrayList<>();
				lines.add(Component.translatable(ach.nameKey()).withStyle(has ? ChatFormatting.GOLD : ChatFormatting.WHITE));
				lines.add(Component.translatable(ach.descKey()).withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable("screen.minecraft_mode.codex.merit", ach.merit()).withStyle(ChatFormatting.YELLOW));
				if (ach.hasTitle()) {
					lines.add(Component.translatable("screen.minecraft_mode.codex.title_reward", Component.translatable(ach.titleKey())).withStyle(ChatFormatting.LIGHT_PURPLE));
				}
				tip = lines;
			}
		}
		int pages = Math.max(1, (all.size() + ROWS - 1) / ROWS);
		g.centeredText(this.font, (page + 1) + "/" + pages, x + 51, y + H - 17, 0xFFBBBBBB);
		return tip;
	}

	private List<Component> titles(final GuiGraphicsExtractor g, final LocalPlayer player, final int mouseX, final int mouseY) {
		int x = this.left;
		int y = this.top;
		PlayerRecords records = Progress.get(player);
		Component worn = records.title().isEmpty() ? Component.translatable("screen.minecraft_mode.codex.no_title")
			: Component.translatable("screen.minecraft_mode.codex.wearing", Component.translatable(Achievements.get(records.title()) == null ? ""
				: Achievements.get(records.title()).titleKey()));
		g.text(this.font, this.font.plainSubstrByWidth(worn.getString(), 140), x + 104, y + H - 17, 0xFFC08AFF, false);
		List<Achievements.Achievement> titled = titled();
		List<Component> tip = null;
		for (int i = 0; i < titled.size(); i++) {
			Achievements.Achievement ach = titled.get(i);
			boolean has = records.has(ach.id());
			int rx = x + 8 + i / TITLE_ROWS * TITLE_COL;
			int ry = y + 28 + i % TITLE_ROWS * TITLE_ROW;
			g.fill(rx, ry, rx + 106, ry + TITLE_ROW - 2, records.title().equals(ach.id()) ? 0x60C08AFF : 0x40000000);
			String name = "「" + Component.translatable(ach.titleKey()).getString() + "」";
			g.text(this.font, this.font.plainSubstrByWidth(name, 102), rx + 3, ry + 4, has ? 0xFFE0C0FF : 0xFF606060, false);
			if (mouseX >= rx && mouseX < rx + 106 && mouseY >= ry && mouseY < ry + TITLE_ROW - 2) {
				tip = List.of(Component.literal(name).withStyle(ChatFormatting.LIGHT_PURPLE),
					Component.translatable("screen.minecraft_mode.codex.title_from", Component.translatable(ach.nameKey())).withStyle(has ? ChatFormatting.GREEN : ChatFormatting.GRAY),
					Component.translatable(ach.descKey()).withStyle(ChatFormatting.DARK_GRAY));
			}
		}
		return tip;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
