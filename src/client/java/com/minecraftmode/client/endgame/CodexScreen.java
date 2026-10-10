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
import com.minecraftmode.story.Story;
import com.minecraftmode.worldgen.lair.LairDef;
import com.minecraftmode.worldgen.lair.NamedLairs;
import com.minecraftmode.worldgen.lair.LairExp;
import com.minecraftmode.dungeon.DungeonData;
import com.minecraftmode.dungeon.DungeonDef;
import com.minecraftmode.dungeon.Dungeons;
import com.minecraftmode.event.WorldEvents;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.progress.Codex;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.raid.RaidDifficulty;
import java.util.Map;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;
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
	private static final int TITLES_PER_PAGE = TITLE_ROWS * 2;
	private static final int STORY_ROW = 16;
	private static final int TAB_W = 51;
	/** Top and bottom of the scrolling area of the monster and item tabs. */
	private static final int SCROLL_TOP = 26;
	private static final int ITEMS_TOP = 62;

	enum Tab {
		CODEX, MONSTERS, ITEMS, ACHIEVEMENTS, TITLES, STORY
	}

	private static Tab tab = Tab.CODEX;
	private static int page;
	/** Pixels scrolled in the monster and item tabs. */
	private static int scroll;
	private static Codex.@Nullable Category itemCategory;
	private static @Nullable JobClass itemJob;
	private static int itemBracket;
	private static String itemSearch = "";
	private int contentHeight;

	private PlayerRecords shown = PlayerRecords.DEFAULT;
	private int left;
	private int top;

	public CodexScreen() {
		super(Component.translatable("screen.minecraft_mode.codex.title"));
	}

	/** Opens the codex on tab {@code name} (codex, achievements, titles) next time (also used by tests). */
	public static void showTab(final String name) {
		tab = Tab.valueOf(name.toUpperCase(Locale.ROOT));
		scroll = 0;
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
				scroll = 0;
				page = 0;
				this.rebuildWidgets();
			}).bounds(this.left + 8 + i * TAB_W, this.top + 4, TAB_W - 2, 16).build();
			b.active = t != tab;
			this.addRenderableWidget(b);
		}
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(this.left + W - 68, this.top + H - 22, 60, 18).build());
		if (tab == Tab.ITEMS) {
			this.itemFilters();
		}
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
			int pages = Math.max(1, (titled.size() + TITLES_PER_PAGE - 1) / TITLES_PER_PAGE);
			page = Math.min(page, pages - 1);
			for (int i = 0; i < TITLES_PER_PAGE; i++) {
				int index = page * TITLES_PER_PAGE + i;
				if (index >= titled.size()) {
					break;
				}
				Achievements.Achievement ach = titled.get(index);
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
			Button prev = Button.builder(Component.literal("◀"), b -> {
				page--;
				this.rebuildWidgets();
			}).bounds(this.left + W - 116, this.top + H - 22, 20, 18).build();
			prev.active = page > 0;
			this.addRenderableWidget(prev);
			Button next = Button.builder(Component.literal("▶"), b -> {
				page++;
				this.rebuildWidgets();
			}).bounds(this.left + W - 94, this.top + H - 22, 20, 18).build();
			next.active = page < pages - 1;
			this.addRenderableWidget(next);
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
		List<Component> tip = switch (tab) {
			case CODEX -> this.codex(g, player, mouseX, mouseY);
			case MONSTERS -> this.monsters(g, player, mouseX, mouseY);
			case ITEMS -> this.items(g, player, mouseX, mouseY);
			case ACHIEVEMENTS -> this.achievements(g, player, mouseX, mouseY);
			case TITLES -> this.titles(g, player, mouseX, mouseY);
			case STORY -> this.story(g, player, mouseX, mouseY);
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
		List<String> found = LairExp.found(player);
		g.text(this.font, Component.translatable("screen.minecraft_mode.codex.lairs", CollectionBonuses.lairKinds(records), lairs.size()), x + 10, ty, 0xFFFFD27F, false);
		ty += 11;
		for (int i = 0; i < lairs.size(); i++) {
			LairDef def = lairs.get(i);
			int cx = x + 10 + i % PER_ROW * CELL;
			int cy = ty + i / PER_ROW * CELL;
			int clears = records.lairClears(def.id());
			boolean seen = clears > 0 || found.contains(def.id());
			if (seen) {
				this.cell(g, cx, cy, new ItemStack(ModItems.LAIR_CACHE), clears > 0, clears > 0 ? 0xFF5AB0E8 : 0);
			} else {
				g.fill(cx, cy, cx + CELL - 2, cy + CELL - 2, 0x40000000);
				g.centeredText(this.font, "?", cx + CELL / 2 - 1, cy + 7, 0xFF6A6A6A);
			}
			if (mouseX >= cx && mouseX < cx + CELL - 2 && mouseY >= cy && mouseY < cy + CELL - 2) {
				NamedDef lord = def.named();
				tip = !seen ? List.of(Component.translatable("screen.minecraft_mode.codex.unknown_lair").withStyle(ChatFormatting.GRAY))
					: List.of(Component.translatable(def.nameKey()).withStyle(clears > 0 ? ChatFormatting.AQUA : ChatFormatting.WHITE),
						Component.translatable("screen.minecraft_mode.codex.lair_lord", Component.translatable(lord.nameKey()), lord.lo(), lord.hi()).withStyle(ChatFormatting.GRAY),
						clears > 0 ? Component.translatable("screen.minecraft_mode.codex.clears", clears).withStyle(ChatFormatting.GREEN)
							: Component.translatable("screen.minecraft_mode.codex.lair_found").withStyle(ChatFormatting.YELLOW));
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
		g.text(this.font, this.font.plainSubstrByWidth(worn.getString(), 96), x + 104, y + H - 17, 0xFFC08AFF, false);
		List<Achievements.Achievement> titled = titled();
		List<Component> tip = null;
		for (int i = 0; i < TITLES_PER_PAGE; i++) {
			int index = page * TITLES_PER_PAGE + i;
			if (index >= titled.size()) {
				break;
			}
			Achievements.Achievement ach = titled.get(index);
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

	/** The story journal: every chapter (finished, current, or still hidden) and the current goal. */
	private List<Component> story(final GuiGraphicsExtractor g, final LocalPlayer player, final int mouseX, final int mouseY) {
		int x = this.left;
		int y = this.top;
		List<Story.Chapter> chapters = Story.chapters();
		int current = Story.data(player).chapter();
		List<Component> tip = null;
		for (int i = 0; i < chapters.size(); i++) {
			Story.Chapter chapter = chapters.get(i);
			int ry = y + 26 + i * STORY_ROW;
			boolean done = i < current;
			boolean now = i == current;
			g.fill(x + 8, ry, x + W - 8, ry + STORY_ROW - 2, now ? 0x40E8C24A : done ? 0x2040FF40 : 0x40000000);
			String label = (done ? "✔ " : now ? "▶ " : "  ") + Component.translatable("screen.minecraft_mode.codex.chapter", i + 1).getString() + "  "
				+ (done || now ? Component.translatable(chapter.key()).getString() : "???");
			g.text(this.font, label, x + 12, ry + 3, done ? 0xFF7CFC7C : now ? 0xFFFFD27F : 0xFF606060, false);
			if (now) {
				String goal = chapter.progress(player) + "/" + chapter.goal();
				g.text(this.font, goal, x + W - 12 - this.font.width(goal), ry + 3, chapter.done(player) ? 0xFF7CFC7C : 0xFFBBBBBB, false);
			}
			if ((done || now) && mouseX >= x + 8 && mouseX < x + W - 8 && mouseY >= ry && mouseY < ry + STORY_ROW - 2) {
				tip = List.of(Component.translatable(chapter.key()).withStyle(ChatFormatting.GOLD),
					Component.translatable(chapter.key() + ".text").withStyle(ChatFormatting.GRAY),
					Component.translatable("screen.minecraft_mode.codex.chapter_goal", Component.translatable(chapter.key() + ".goal")).withStyle(ChatFormatting.AQUA),
					Component.translatable("screen.minecraft_mode.codex.chapter_reward", Component.translatable(chapter.key() + ".reward")).withStyle(ChatFormatting.YELLOW));
			}
		}
		Component footer = current >= chapters.size() ? Component.translatable("screen.minecraft_mode.codex.story_done")
			: Component.translatable("screen.minecraft_mode.codex.story_hint");
		g.text(this.font, this.font.plainSubstrByWidth(footer.getString(), W - 90), x + 10, y + H - 17, 0xFF8A8A8A, false);
		return tip;
	}

	// ------------------------------------------------------------------ monsters

	private static final int[] MILESTONE_COLORS = {0xFFCD7F32, 0xFFC8C8D0, 0xFFE8C24A};

	/** Raid bosses have no spawn eggs; each gets an item that fits it. */
	private static ItemStack raidIcon(final BossDef boss) {
		return new ItemStack(switch (boss.id()) {
			case "arachne" -> Items.COBWEB;
			case "gorvath" -> Items.IRON_BLOCK;
			case "kraken" -> Items.HEART_OF_THE_SEA;
			case "ignis" -> Items.BLAZE_POWDER;
			case "malachar" -> Items.WITHER_SKELETON_SKULL;
			case "aethryx" -> Items.DRAGON_HEAD;
			default -> Items.NETHER_STAR;
		});
	}

	private boolean hovered(final int mouseX, final int mouseY, final int cx, final int cy) {
		return mouseX >= cx && mouseX < cx + CELL - 2 && mouseY >= cy && mouseY < cy + CELL - 2
			&& mouseY >= this.top + SCROLL_TOP && mouseY < this.top + H - 26;
	}

	/** One codex cell: background, optional frame, the icon and a dark veil when not met yet. */
	private void cell(final GuiGraphicsExtractor g, final int cx, final int cy, final ItemStack icon, final boolean met, final int frame) {
		g.fill(cx, cy, cx + CELL - 2, cy + CELL - 2, met ? 0x50FFFFFF : 0x40000000);
		if (frame != 0) {
			g.outline(cx, cy, CELL - 2, CELL - 2, frame);
		}
		g.fakeItem(icon, cx + 3, cy + 3);
		if (!met) {
			g.fill(cx + 1, cy + 1, cx + CELL - 3, cy + CELL - 3, 0xB0101018);
		}
	}

	/** Common monsters by place, then raid bosses, dungeon bosses and titans; scrolls. */
	private List<Component> monsters(final GuiGraphicsExtractor g, final LocalPlayer player, final int mouseX, final int mouseY) {
		int x = this.left;
		int y = this.top;
		List<Component> tip = null;
		g.enableScissor(x + 1, y + SCROLL_TOP, x + W - 1, y + H - 26);
		int ty = y + SCROLL_TOP + 2 - scroll;

		List<EntityType<?>> monsters = Codex.monsters();
		Map<String, Integer> kills = Codex.kills(player);
		long met = monsters.stream().filter(t -> Codex.kills(player, t) > 0).count();
		g.text(this.font, Component.translatable("screen.minecraft_mode.codex.monsters", met, monsters.size()), x + 10, ty, 0xFFFFD27F, false);
		ty += 11;
		for (int i = 0; i < monsters.size(); i++) {
			EntityType<?> type = monsters.get(i);
			int cx = x + 10 + i % PER_ROW * CELL;
			int cy = ty + i / PER_ROW * CELL;
			int count = kills.getOrDefault(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(), 0);
			int milestone = Codex.milestone(count);
			Item egg = Codex.egg(type);
			this.cell(g, cx, cy, new ItemStack(egg == null ? Items.SKELETON_SKULL : egg), count > 0, milestone == 0 ? 0 : MILESTONE_COLORS[milestone - 1]);
			if (this.hovered(mouseX, mouseY, cx, cy)) {
				List<Component> lines = new ArrayList<>();
				lines.add(type.getDescription().copy().withStyle(count > 0 ? ChatFormatting.WHITE : ChatFormatting.GRAY));
				lines.add(Component.translatable(Codex.place(type).key()).withStyle(ChatFormatting.DARK_AQUA));
				lines.add(Component.translatable("screen.minecraft_mode.codex.monster_kills", count).withStyle(count > 0 ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY));
				if (milestone < Codex.MILESTONES.length) {
					lines.add(Component.translatable("screen.minecraft_mode.codex.next_milestone", Codex.MILESTONES[milestone]).withStyle(ChatFormatting.DARK_GRAY));
				} else {
					lines.add(Component.translatable("screen.minecraft_mode.codex.all_milestones").withStyle(ChatFormatting.GOLD));
				}
				tip = lines;
			}
		}
		ty += (monsters.size() + PER_ROW - 1) / PER_ROW * CELL + 4;

		PlayerRecords records = Progress.get(player);
		List<BossDef> bosses = RaidBosses.all();
		long beaten = bosses.stream().filter(b -> raidClears(records, b) > 0).count();
		g.text(this.font, Component.translatable("screen.minecraft_mode.codex.raid_bosses", beaten, bosses.size()), x + 10, ty, 0xFFFFD27F, false);
		ty += 11;
		for (int i = 0; i < bosses.size(); i++) {
			BossDef boss = bosses.get(i);
			int cx = x + 10 + i % PER_ROW * CELL;
			int cy = ty + i / PER_ROW * CELL;
			int clears = raidClears(records, boss);
			this.cell(g, cx, cy, raidIcon(boss), clears > 0, clears > 0 ? 0xFFC08AFF : 0);
			if (this.hovered(mouseX, mouseY, cx, cy)) {
				List<Component> lines = new ArrayList<>();
				lines.add(Component.translatable(boss.nameKey()).withStyle(clears > 0 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY));
				lines.add(Component.translatable(boss.epithetKey()).withStyle(ChatFormatting.DARK_PURPLE));
				lines.add(Component.translatable("screen.minecraft_mode.codex.level_range", boss.lo(), boss.hi()).withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable("screen.minecraft_mode.codex.raid_where").withStyle(ChatFormatting.DARK_AQUA));
				for (RaidDifficulty difficulty : RaidDifficulty.values()) {
					int n = records.raidClears(PlayerRecords.raidKey(boss.id(), difficulty.id()));
					lines.add(Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY).append(Component.translatable(difficulty.nameKey())
						.append(" " + n)).withStyle(n > 0 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
				}
				tip = lines;
			}
		}
		ty += (bosses.size() + PER_ROW - 1) / PER_ROW * CELL + 4;

		DungeonData dungeon = Dungeons.data(player);
		List<DungeonDef> dungeons = new ArrayList<>(Dungeons.all());
		long cleared = dungeons.stream().filter(d -> dungeon.clears(d.id()) > 0).count();
		g.text(this.font, Component.translatable("screen.minecraft_mode.codex.dungeon_bosses", cleared, dungeons.size()), x + 10, ty, 0xFFFFD27F, false);
		ty += 11;
		for (int i = 0; i < dungeons.size(); i++) {
			DungeonDef def = dungeons.get(i);
			NamedDef boss = NamedMobs.byId(def.boss());
			int cx = x + 10 + i % PER_ROW * CELL;
			int cy = ty + i / PER_ROW * CELL;
			int clears = dungeon.clears(def.id());
			this.cell(g, cx, cy, new ItemStack(NamedMobs.egg(boss)), clears > 0, clears > 0 ? 0xFF5AB0E8 : 0);
			if (this.hovered(mouseX, mouseY, cx, cy)) {
				List<Component> lines = new ArrayList<>();
				lines.add(Component.translatable(def.nameKey()).withStyle(clears > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY));
				lines.add(Component.translatable("screen.minecraft_mode.codex.dungeon_boss", Component.translatable(boss.nameKey())).withStyle(ChatFormatting.WHITE));
				lines.add(Component.translatable("screen.minecraft_mode.codex.min_level", def.minLevel()).withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable("screen.minecraft_mode.codex.dungeon_where").withStyle(ChatFormatting.DARK_AQUA));
				lines.add(Component.translatable("screen.minecraft_mode.codex.clears", clears).withStyle(clears > 0 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
				if (dungeon.best(def.id()) > 0) {
					lines.add(Component.translatable("screen.minecraft_mode.codex.best_keystone", dungeon.best(def.id())).withStyle(ChatFormatting.GOLD));
				}
				tip = lines;
			}
		}
		ty += (dungeons.size() + PER_ROW - 1) / PER_ROW * CELL + 4;

		int titans = Story.data(player).worldBosses();
		g.text(this.font, Component.translatable("screen.minecraft_mode.codex.titans", titans), x + 10, ty, 0xFFFFD27F, false);
		ty += 11;
		ty = g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.codex.titans_hint", WorldEvents.TITAN_MIN_LEVEL), x + 14, ty, W - 28, 0xFF8A8A8A);
		g.disableScissor();
		this.contentHeight = ty + scroll - (y + SCROLL_TOP) + 4;
		this.scrollBar(g, H - 26 - SCROLL_TOP, SCROLL_TOP);
		return tip;
	}

	private static int raidClears(final PlayerRecords records, final BossDef boss) {
		int total = 0;
		for (RaidDifficulty difficulty : RaidDifficulty.values()) {
			total += records.raidClears(PlayerRecords.raidKey(boss.id(), difficulty.id()));
		}
		return total;
	}

	private void scrollBar(final GuiGraphicsExtractor g, final int view, final int from) {
		scroll = Math.max(0, Math.min(scroll, Math.max(0, this.contentHeight - view)));
		if (this.contentHeight <= view) {
			return;
		}
		int barX = this.left + W - 6;
		int barTop = this.top + from;
		int knob = Math.max(12, view * view / this.contentHeight);
		int knobY = barTop + (view - knob) * scroll / Math.max(1, this.contentHeight - view);
		g.fill(barX, barTop, barX + 3, barTop + view, 0x40FFFFFF);
		g.fill(barX, knobY, barX + 3, knobY + knob, 0xFFE8C24A);
	}

	@Override
	public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
		if (tab == Tab.MONSTERS || tab == Tab.ITEMS) {
			scroll = Math.max(0, scroll - (int) Math.signum(scrollY) * CELL);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	/** Scrolls the current tab to its end (the game test's second screenshot). */
	public static void scrollToEnd() {
		scroll = Integer.MAX_VALUE / 2;
	}

	// ------------------------------------------------------------------ items

	/** Category buttons, class and level cycles and the search box of the item tab. */
	private void itemFilters() {
		int bx = this.left + 8;
		int by = this.top + 24;
		Codex.Category[] categories = Codex.Category.values();
		for (int i = -1; i < categories.length; i++) {
			Codex.Category c = i < 0 ? null : categories[i];
			Button b = Button.builder(Component.translatable(c == null ? "screen.minecraft_mode.codex.items.all" : c.key()), button -> {
				itemCategory = c;
				scroll = 0;
				this.rebuildWidgets();
			}).bounds(bx, by, 58, 16).build();
			b.active = itemCategory != c;
			this.addRenderableWidget(b);
			bx += 60;
		}
		bx = this.left + 132;
		by = this.top + 42;
		Component job = itemJob == null ? Component.translatable("screen.minecraft_mode.codex.items.any_class") : Component.translatable(itemJob.nameKey());
		this.addRenderableWidget(Button.builder(job, button -> {
			List<JobClass> jobs = JobClass.PLAYABLE;
			int next = itemJob == null ? 0 : jobs.indexOf(itemJob) + 1;
			itemJob = next >= jobs.size() ? null : jobs.get(next);
			scroll = 0;
			this.rebuildWidgets();
		}).bounds(bx, by, 58, 16).build());
		bx += 60;
		Component level = itemBracket == 0 ? Component.translatable("screen.minecraft_mode.codex.items.any_level") : Component.literal("Lv " + itemBracket);
		this.addRenderableWidget(Button.builder(level, button -> {
			itemBracket = itemBracket >= ItemLevels.MAX_BRACKET ? 0 : itemBracket == 0 ? ItemLevels.MIN_BRACKET : itemBracket + 10;
			scroll = 0;
			this.rebuildWidgets();
		}).bounds(bx, by, 46, 16).build());
		EditBox search = new EditBox(this.font, this.left + 8, this.top + 43, 120, 14, Component.translatable("screen.minecraft_mode.codex.items.search"));
		search.setHint(Component.translatable("screen.minecraft_mode.codex.items.search").withStyle(ChatFormatting.DARK_GRAY));
		search.setValue(itemSearch);
		search.setResponder(text -> {
			itemSearch = text;
			scroll = 0;
		});
		this.addRenderableWidget(search);
	}

	private static List<Item> shownItems() {
		String query = itemSearch.trim().toLowerCase(Locale.ROOT);
		List<Item> out = new ArrayList<>();
		for (Map.Entry<Codex.Category, List<Item>> entry : Codex.items().entrySet()) {
			if (itemCategory != null && entry.getKey() != itemCategory) {
				continue;
			}
			for (Item item : entry.getValue()) {
				ClassGear gear = ClassGear.of(new ItemStack(item));
				if (itemJob != null && (gear == null || gear.job() != itemJob) || itemBracket != 0 && (gear == null || gear.bracket() != itemBracket)) {
					continue;
				}
				if (!query.isEmpty() && !Component.translatable(item.getDescriptionId()).getString().toLowerCase(Locale.ROOT).contains(query)) {
					continue;
				}
				out.add(item);
			}
		}
		return out;
	}

	private List<Component> items(final GuiGraphicsExtractor g, final LocalPlayer player, final int mouseX, final int mouseY) {
		int x = this.left;
		int y = this.top;
		List<Item> items = shownItems();
		java.util.Set<String> found = new java.util.HashSet<>(Codex.found(player));
		long have = items.stream().filter(i -> found.contains(BuiltInRegistries.ITEM.getKey(i).getPath())).count();
		Component count = Component.translatable("screen.minecraft_mode.codex.items.count", have, items.size());
		g.text(this.font, count, x + W - 8 - this.font.width(count), y + 46, 0xFFFFD27F, false);
		List<Component> tip = null;
		int view = H - 26 - ITEMS_TOP;
		g.enableScissor(x + 1, y + ITEMS_TOP, x + W - 1, y + H - 26);
		for (int i = 0; i < items.size(); i++) {
			int cx = x + 10 + i % PER_ROW * CELL;
			int cy = y + ITEMS_TOP + i / PER_ROW * CELL - scroll;
			if (cy + CELL < y + ITEMS_TOP || cy > y + H - 26) {
				continue;
			}
			Item item = items.get(i);
			boolean has = found.contains(BuiltInRegistries.ITEM.getKey(item).getPath());
			ItemStack stack = new ItemStack(item);
			this.cell(g, cx, cy, stack, has, 0);
			if (mouseX >= cx && mouseX < cx + CELL - 2 && mouseY >= cy && mouseY < cy + CELL - 2 && mouseY >= y + ITEMS_TOP && mouseY < y + H - 26) {
				List<Component> lines = new ArrayList<>();
				lines.add(stack.getHoverName().copy().withStyle(has ? ChatFormatting.WHITE : ChatFormatting.GRAY));
				ClassGear gear = ClassGear.of(stack);
				if (gear != null) {
					lines.add(Component.translatable("screen.minecraft_mode.codex.items.gear", Component.translatable(gear.job().nameKey()), gear.level())
						.withStyle(ChatFormatting.DARK_AQUA));
				}
				lines.add(Component.translatable(has ? "screen.minecraft_mode.codex.items.found" : "screen.minecraft_mode.codex.items.missing")
					.withStyle(has ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
				lines.add(Component.translatable("screen.minecraft_mode.codex.items.sources").withStyle(ChatFormatting.GOLD));
				for (Component source : CodexSources.of(item, player)) {
					lines.add(Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY).append(source.copy().withStyle(ChatFormatting.GRAY)));
				}
				tip = lines;
			}
		}
		g.disableScissor();
		if (items.isEmpty()) {
			g.text(this.font, Component.translatable("screen.minecraft_mode.codex.items.none"), x + 10, y + ITEMS_TOP + 4, 0xFF8A8A8A, false);
		}
		this.contentHeight = (items.size() + PER_ROW - 1) / PER_ROW * CELL;
		this.scrollBar(g, view, ITEMS_TOP);
		return tip;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
