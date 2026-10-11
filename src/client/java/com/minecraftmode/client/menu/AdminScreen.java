package com.minecraftmode.client.menu;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.admin.Admin;
import com.minecraftmode.client.map.CityPlaces;
import com.minecraftmode.command.WalletCommand;
import com.minecraftmode.dungeon.DungeonDef;
import com.minecraftmode.dungeon.Dungeons;
import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.network.AdminActionPayload;
import com.minecraftmode.network.AdminStatePayload;
import com.minecraftmode.progress.RealReset;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.travel.Destination;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.Nullable;

/**
 * The admin screen (operators, from the adventurer's menu): players (class, money, trials, story, teleport, game mode, kick),
 * teleport and summoning, raids/dungeons/world events, server time, weather, resets and notices, and the market and land claims.
 * Actions apply to the player picked on the players tab (the operator when none is). Everything goes to the server as an
 * {@link AdminActionPayload}, which checks the operator again and answers with a fresh {@link AdminStatePayload}.
 */
public class AdminScreen extends MenuScreenBase {
	private static final String K = "screen.minecraft_mode.admin.";
	private static final int W = 410;
	private static final int H = 236;
	private static final int PLAYERS = 0;
	private static final int TRAVEL = 1;
	private static final int CONTENT = 2;
	private static final int SERVER = 3;
	private static final int ECONOMY = 4;
	private static final int PLAYER_ROWS = 10;
	private static final int LISTING_ROW = 17;
	private static final int PLACES_PER_PAGE = 8;
	private static final int INSTANCE_ROWS = 2;
	private static final String[] DIMENSIONS = {"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end", MinecraftMode.id("residence").toString(),
		MinecraftMode.id("dungeon").toString(), MinecraftMode.id("raid").toString()};
	private static final String[] TIMES = {"day", "noon", "night", "midnight"};
	private static final String[] WEATHER = {"clear", "rain", "thunder"};
	private static final String RESIDENCE = MinecraftMode.id("residence").toString();

	private @Nullable AdminStatePayload state;
	/** The picked player's name ("" = the operator). */
	private String selected = "";
	private int playersPage;
	private int jobIndex;
	private int tier = 1;
	private int dimension;
	private int placesPage;
	private int namedIndex = -1;
	private boolean atLook;
	private int dungeonIndex;
	private int bossIndex;
	private int titanIndex = -1;
	private boolean landView;

	public AdminScreen() {
		super(Component.translatable(K + "title"), W, H);
	}

	/** The server's answer has arrived (game tests wait for it). */
	public boolean loaded() {
		return this.state != null;
	}

	void update(final AdminStatePayload state) {
		this.state = state;
		this.rebuildWidgets();
	}

	@Override
	protected int frameColor() {
		return 0xFFB04A4A;
	}

	@Override
	protected List<Component> tabs() {
		return List.of(Component.translatable(K + "tab.players"), Component.translatable(K + "tab.travel"), Component.translatable(K + "tab.content"),
			Component.translatable(K + "tab.server"), Component.translatable(K + "tab.economy"));
	}

	@Override
	protected void initTab(final int x, final int y) {
		this.button(this.left + this.w - 120, this.top + 4, 64, Component.translatable(K + "back"), () -> this.minecraft.gui.setScreen(new AdventurerScreen()));
		if (this.state == null) {
			this.label(x, y + 4, Component.translatable(K + "loading").withStyle(ChatFormatting.GRAY));
			return;
		}
		// beside the title: the bottom edge can fall off small windows
		this.label(this.left + 60, this.top + 7, this.status());
		switch (this.tab) {
			case PLAYERS -> this.players(x, y);
			case TRAVEL -> this.travel(x, y);
			case CONTENT -> this.content(x, y);
			case SERVER -> this.server(x, y);
			default -> this.economy(x, y);
		}
	}

	/** The picked player and how the last action went. */
	private Component status() {
		Component who = Component.translatable(K + "target", this.selected.isEmpty() ? Component.translatable(K + "me") : Component.literal(this.selected));
		return switch (this.state.result()) {
			case AdminStatePayload.OK -> who.copy().append("   ").append(Component.translatable(K + "ok").withStyle(ChatFormatting.GREEN));
			case AdminStatePayload.FAILED -> who.copy().append("   ").append(Component.translatable(K + "failed").withStyle(ChatFormatting.RED));
			default -> who;
		};
	}

	private void send(final int action, final String text, final int a, final int b, final int c) {
		if (ClientPlayNetworking.canSend(AdminActionPayload.TYPE)) {
			ClientPlayNetworking.send(new AdminActionPayload(action, this.selected, text, a, b, c));
		}
	}

	private void send(final int action) {
		this.send(action, "", 0, 0, 0);
	}

	private void refresh(final int marketPage, final int landPage) {
		if (ClientPlayNetworking.canSend(AdminActionPayload.TYPE)) {
			ClientPlayNetworking.send(new AdminActionPayload(AdminActionPayload.REFRESH, "", this.value("search"), marketPage, landPage, 0));
		}
	}

	// ------------------------------------------------------------------ players

	private void players(final int x, final int y) {
		List<AdminStatePayload.PlayerInfo> players = this.state.players();
		this.heading(x, y, Component.translatable(K + "online", players.size()));
		int pages = Math.max(1, (players.size() + PLAYER_ROWS - 1) / PLAYER_ROWS);
		this.playersPage = Math.min(this.playersPage, pages - 1);
		for (int i = 0; i < PLAYER_ROWS; i++) {
			int index = this.playersPage * PLAYER_ROWS + i;
			if (index >= players.size()) {
				break;
			}
			String name = players.get(index).name();
			boolean me = name.equals(this.minecraft.player.getGameProfile().name());
			this.button(x, y + 12 + i * ROW, 110, Component.literal(me ? name + " ★" : name), () -> {
				this.selected = me ? "" : name;
				this.rebuildWidgets();
			}).active = !(me ? this.selected.isEmpty() : name.equals(this.selected));
		}
		if (pages > 1) {
			this.pager(x + 20, y + 14 + PLAYER_ROWS * ROW, this.playersPage, pages, page -> {
				this.playersPage = page;
				this.rebuildWidgets();
			});
		}
		AdminStatePayload.PlayerInfo info = this.picked();
		int rx = x + 122;
		if (info == null) {
			this.label(rx, y + 4, Component.translatable(K + "gone").withStyle(ChatFormatting.RED));
			return;
		}
		JobClass job = JobClass.values()[Math.floorMod(info.job(), JobClass.values().length)];
		this.label(rx, y, Component.literal(info.name()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
		this.label(rx, y + 11, Component.translatable(job.tierKey(info.tier())).withColor(job.color())
			.append(Component.literal("  Lv." + info.level() + "  ").withStyle(ChatFormatting.GRAY)).append(Coins.component(info.balance())));
		this.label(rx, y + 22, Component.literal(info.dimension() + " · ").append(GameType.byId(info.gameMode()).getLongDisplayName())
			.append(Component.translatable(K + "chapter_now", info.chapter())).withStyle(ChatFormatting.GRAY));
		int ry = y + 36;
		List<JobClass> classes = List.of(JobClass.values());
		this.jobIndex = Math.floorMod(this.jobIndex, classes.size());
		JobClass pick = classes.get(this.jobIndex);
		this.button(rx, ry, 110, Component.translatable(pick.tierKey(pick == JobClass.NONE ? 0 : this.tier)).withColor(pick.color()), () -> {
			this.jobIndex++;
			this.rebuildWidgets();
		});
		this.button(rx + 112, ry, 50, Component.translatable(K + "tier", this.tier), () -> {
			this.tier = this.tier % Admin.MAX_TIER + 1;
			this.rebuildWidgets();
		});
		this.confirm(rx + 164, ry, 110, "job", Component.translatable(K + "job_set"), () -> this.send(AdminActionPayload.JOB_SET, pick.id(), this.tier, 0, 0));
		ry += ROW;
		this.numberBox(rx, ry, 40, "level", Component.translatable(K + "level"));
		this.button(rx + 42, ry, 70, Component.translatable(K + "level_set"), () -> this.send(AdminActionPayload.LEVEL_SET, "", this.number("level", info.level()), 0, 0));
		this.numberBox(rx + 120, ry, 60, "exp", Component.translatable(K + "exp"));
		this.button(rx + 182, ry, 92, Component.translatable(K + "exp_add"), () -> this.send(AdminActionPayload.EXP_ADD, "", this.number("exp", 0), 0, 0));
		ry += ROW;
		this.button(rx, ry, 90, Component.translatable(K + "mana"), () -> this.send(AdminActionPayload.MANA_FILL));
		this.button(rx + 92, ry, 90, Component.translatable(K + "cooldowns"), () -> this.send(AdminActionPayload.COOLDOWNS));
		this.button(rx + 184, ry, 90, Component.translatable(K + "heal"), () -> this.send(AdminActionPayload.HEAL));
		ry += ROW;
		this.confirm(rx, ry, 136, "quest_clear", Component.translatable(K + "quest_clear"), () -> this.send(AdminActionPayload.QUEST_CLEAR));
		this.button(rx + 138, ry, 136, Component.translatable(K + "quest_goals"), () -> this.send(AdminActionPayload.QUEST_GOALS));
		ry += ROW;
		this.box(rx, ry, 80, "money", Component.translatable(K + "money"), 24, s -> s.matches("[0-9gGsScC ]*"));
		this.button(rx + 82, ry, 94, Component.translatable(K + "money_give"), () -> this.money(AdminActionPayload.MONEY_GIVE));
		this.confirm(rx + 178, ry, 96, "money_set", Component.translatable(K + "money_set"), () -> this.money(AdminActionPayload.MONEY_SET));
		ry += ROW;
		boolean other = !this.selected.isEmpty();
		this.button(rx, ry, 66, Component.translatable(K + "goto"), () -> this.send(AdminActionPayload.TP_TO)).active = other;
		this.button(rx + 68, ry, 66, Component.translatable(K + "bring"), () -> this.send(AdminActionPayload.BRING)).active = other;
		GameType[] modes = GameType.values();
		for (int i = 0; i < modes.length; i++) {
			GameType mode = modes[i];
			this.button(rx + 138 + i * 34, ry, 33, Component.translatable(K + "mode." + mode.getName()), mode.getLongDisplayName(),
				() -> this.send(AdminActionPayload.GAMEMODE, "", mode.getId(), 0, 0)).active = info.gameMode() != mode.getId();
		}
		ry += ROW;
		this.numberBox(rx, ry, 30, "chapter", Component.translatable(K + "chapter"));
		this.button(rx + 32, ry, 70, Component.translatable(K + "chapter_set"), () -> this.send(AdminActionPayload.STORY_SET, "", this.number("chapter", info.chapter()), 0, 0));
		this.box(rx + 110, ry, 110, "reason", Component.translatable(K + "reason"), 100, s -> true);
		this.confirm(rx + 222, ry, 52, "kick", Component.translatable(K + "kick").withStyle(ChatFormatting.RED),
			() -> this.send(AdminActionPayload.KICK, this.value("reason"), 0, 0, 0)).active = other;
	}

	private void money(final int action) {
		int copper = WalletCommand.parse(this.value("money"));
		if (copper >= 0) {
			this.send(action, "", copper, 0, 0);
		}
	}

	private AdminStatePayload.@Nullable PlayerInfo picked() {
		String name = this.selected.isEmpty() ? this.minecraft.player.getGameProfile().name() : this.selected;
		return this.state.players().stream().filter(p -> p.name().equals(name)).findFirst().orElse(null);
	}

	// ------------------------------------------------------------------ teleport and summoning

	private void travel(final int x, final int y) {
		this.heading(x, y, Component.translatable(K + "coords"));
		this.numberBox(x, y + 12, 60, "x", Component.literal("X"));
		this.numberBox(x + 62, y + 12, 60, "y", Component.translatable(K + "surface"));
		this.numberBox(x + 124, y + 12, 60, "z", Component.literal("Z"));
		this.dimension = Math.floorMod(this.dimension, DIMENSIONS.length);
		this.button(x + 188, y + 12, 120, Component.literal(DIMENSIONS[this.dimension].replace("minecraft:", "")), () -> {
			this.dimension++;
			this.rebuildWidgets();
		});
		this.button(x + 310, y + 12, 80, Component.translatable(K + "go"), () -> {
			if (!this.value("x").isEmpty() && !this.value("z").isEmpty()) {
				this.send(AdminActionPayload.TP_COORDS, DIMENSIONS[this.dimension], this.number("x", 0), this.number("y", AdminActionPayload.SURFACE),
					this.number("z", 0));
			}
		});
		int py = y + 34;
		this.heading(x, py, Component.translatable(K + "places"));
		for (Destination destination : Destination.values()) {
			this.button(x + 120 + destination.ordinal() * 92, py - 3, 90, Component.translatable(destination.nameKey()),
				() -> this.send(AdminActionPayload.TP_PLACE, "", destination.ordinal(), 0, 0));
		}
		this.confirm(x + 306, py - 3, 84, "gather", Component.translatable(K + "gather"), () -> this.send(AdminActionPayload.GATHER));
		List<CityPlaces.Place> places = CityPlaces.all();
		int pages = (places.size() + PLACES_PER_PAGE - 1) / PLACES_PER_PAGE;
		this.placesPage = Math.min(this.placesPage, pages - 1);
		for (int i = 0; i < PLACES_PER_PAGE; i++) {
			int index = this.placesPage * PLACES_PER_PAGE + i;
			if (index >= places.size()) {
				break;
			}
			CityPlaces.Place place = places.get(index);
			this.button(x + (i % 4) * 98, py + 14 + (i / 4) * ROW, 96, place.name(),
				() -> this.send(AdminActionPayload.TP_COORDS, "minecraft:overworld", place.x(), AdminActionPayload.SURFACE, place.z()));
		}
		this.pager(x + 324, py + 14 + 2 * ROW, this.placesPage, pages, page -> {
			this.placesPage = page;
			this.rebuildWidgets();
		});
		int sy = py + 14 + 3 * ROW + 6;
		this.heading(x, sy, Component.translatable(K + "summon"));
		List<NamedDef> named = List.copyOf(NamedMobs.all());
		Component namedLabel = this.namedIndex < 0 ? Component.translatable(K + "named_pick") : Component.translatable("entity.minecraft_mode." + named.get(this.namedIndex).id());
		this.button(x + 120, sy - 3, 150, namedLabel, () -> {
			this.namedIndex = (this.namedIndex + 1) % named.size();
			this.setValue("entity", MinecraftMode.id(named.get(this.namedIndex).id()).toString());
			this.rebuildWidgets();
		});
		this.box(x, sy + 12, 180, "entity", Component.translatable(K + "entity"), 96, s -> s.matches("[a-z0-9_.:/-]*"));
		this.numberBox(x + 182, sy + 12, 30, "count", Component.translatable(K + "count"));
		this.button(x + 214, sy + 12, 90, Component.translatable(this.atLook ? K + "at_look" : K + "at_feet"), () -> {
			this.atLook = !this.atLook;
			this.rebuildWidgets();
		});
		this.button(x + 306, sy + 12, 84, Component.translatable(K + "summon_go"),
			() -> this.send(AdminActionPayload.SUMMON, this.value("entity"), Math.clamp(this.number("count", 1), 1, Admin.MAX_SUMMON), this.atLook ? 1 : 0, 0));
		this.label(x, sy + 30, Component.translatable(K + "summon_hint", Admin.MAX_SUMMON).withStyle(ChatFormatting.DARK_GRAY));
	}

	// ------------------------------------------------------------------ raids, dungeons, world events

	private void content(final int x, final int y) {
		List<DungeonDef> dungeons = new ArrayList<>(Dungeons.all());
		this.dungeonIndex = Math.floorMod(this.dungeonIndex, dungeons.size());
		DungeonDef dungeon = dungeons.get(this.dungeonIndex);
		this.heading(x, y, Component.translatable(K + "dungeon"));
		this.button(x + 70, y - 3, 150, Component.translatable(dungeon.nameKey()), () -> {
			this.dungeonIndex++;
			this.rebuildWidgets();
		});
		this.numberBox(x + 222, y - 3, 40, "keystone", Component.translatable(K + "keystone"));
		this.button(x + 264, y - 3, 126, Component.translatable(K + "start_party"),
			() -> this.send(AdminActionPayload.DUNGEON_START, dungeon.id(), Math.clamp(this.number("keystone", 0), 0, Admin.MAX_KEYSTONE), 0, 0));
		List<BossDef> bosses = RaidBosses.all();
		this.bossIndex = Math.floorMod(this.bossIndex, bosses.size());
		BossDef boss = bosses.get(this.bossIndex);
		this.heading(x, y + ROW + 2, Component.translatable(K + "raid"));
		this.button(x + 70, y + ROW - 1, 192, Component.translatable(boss.nameKey()), () -> {
			this.bossIndex++;
			this.rebuildWidgets();
		});
		this.button(x + 264, y + ROW - 1, 126, Component.translatable(K + "start_party"), () -> this.send(AdminActionPayload.RAID_START, boss.id(), 0, 0, 0));
		int ly = y + 2 * ROW + 8;
		ly = this.instances(x, ly, K + "running_raids", this.state.raids(), AdminActionPayload.RAID_STOP);
		ly = this.instances(x, ly + 4, K + "running_dungeons", this.state.dungeons(), AdminActionPayload.DUNGEON_STOP);
		int ey = ly + 8;
		this.heading(x, ey, Component.translatable(K + "events"));
		List<NamedDef> named = List.copyOf(NamedMobs.all());
		Component titan = this.titanIndex < 0 ? Component.translatable(K + "titan_random") : Component.translatable("entity.minecraft_mode." + named.get(this.titanIndex).id());
		this.button(x, ey + 12, 150, titan, () -> {
			this.titanIndex = this.titanIndex + 1 >= named.size() ? -1 : this.titanIndex + 1;
			this.rebuildWidgets();
		});
		this.button(x + 152, ey + 12, 78, Component.translatable(K + "titan"),
			() -> this.send(AdminActionPayload.TITAN, this.titanIndex < 0 ? "" : named.get(this.titanIndex).id(), 0, 0, 0));
		this.button(x + 232, ey + 12, 78, Component.translatable(K + "invasion"), () -> this.send(AdminActionPayload.INVASION));
		this.confirm(x + 312, ey + 12, 78, "event_stop", Component.translatable(K + "event_stop"), () -> this.send(AdminActionPayload.EVENT_STOP));
	}

	/** Running raids or dungeon runs with a stop button each; returns the y below the list. */
	private int instances(final int x, final int y, final String title, final List<AdminStatePayload.Instance> list, final int stop) {
		this.heading(x, y, Component.translatable(title, list.size()));
		int ry = y + 12;
		if (list.isEmpty()) {
			this.label(x + 6, ry + 3, Component.translatable(K + "none").withStyle(ChatFormatting.GRAY));
			return ry + ROW;
		}
		for (int i = 0; i < Math.min(INSTANCE_ROWS, list.size()); i++) {
			AdminStatePayload.Instance instance = list.get(i);
			String line = "#" + instance.id() + " " + instance.what() + " · " + instance.state() + " · " + instance.members();
			this.label(x + 6, ry + 3, Component.literal(this.font.plainSubstrByWidth(line, 320)));
			this.confirm(x + 336, ry, 54, title + instance.id(), Component.translatable(K + "stop"), () -> this.send(stop, "", instance.id(), 0, 0));
			ry += ROW;
		}
		if (list.size() > INSTANCE_ROWS) {
			this.label(x + 6, ry + 1, Component.translatable(K + "more", list.size() - INSTANCE_ROWS).withStyle(ChatFormatting.GRAY));
			ry += 11;
		}
		return ry;
	}

	// ------------------------------------------------------------------ server

	private void server(final int x, final int y) {
		this.heading(x, y, Component.translatable(K + "time"));
		for (int i = 0; i < TIMES.length; i++) {
			int index = i;
			this.button(x + 80 + i * 78, y - 3, 76, Component.translatable(K + "time." + TIMES[i]), () -> this.send(AdminActionPayload.TIME, "", index, 0, 0));
		}
		this.heading(x, y + ROW + 2, Component.translatable(K + "weather"));
		for (int i = 0; i < WEATHER.length; i++) {
			int index = i;
			this.button(x + 80 + i * 78, y + ROW - 1, 76, Component.translatable(K + "weather." + WEATHER[i]), () -> this.send(AdminActionPayload.WEATHER, "", index, 0, 0));
		}
		int ry = y + 2 * ROW + 10;
		this.heading(x, ry, Component.translatable(K + "resets"));
		this.label(x + 6, ry + 13, Component.translatable(K + "next_day", RealReset.remaining(this.state.ticksToDay())));
		this.label(x + 6, ry + 25, Component.translatable(K + "next_cycle", RealReset.remaining(this.state.ticksToCycle())));
		this.confirm(x + 200, ry + 10, 94, "skip_day", Component.translatable(K + "skip_day"), () -> this.send(AdminActionPayload.SKIP_DAYS, "", 1, 0, 0));
		this.confirm(x + 296, ry + 10, 94, "skip_cycle", Component.translatable(K + "skip_cycle", RealReset.DAYS),
			() -> this.send(AdminActionPayload.SKIP_DAYS, "", RealReset.DAYS, 0, 0));
		this.label(x + 6, ry + 40, Component.translatable(K + "skip_hint").withStyle(ChatFormatting.DARK_GRAY));
		int by = ry + 58;
		this.heading(x, by, Component.translatable(K + "broadcast"));
		this.box(x, by + 12, 300, "notice", Component.translatable(K + "notice_hint"), AdminActionPayload.MAX_TEXT, s -> true);
		this.confirm(x + 304, by + 12, 86, "notice", Component.translatable(K + "notice_send"), () -> {
			if (!this.value("notice").isEmpty()) {
				this.send(AdminActionPayload.BROADCAST, this.value("notice"), 0, 0, 0);
			}
		});
	}

	// ------------------------------------------------------------------ market and land

	private void economy(final int x, final int y) {
		this.button(x, y - 3, 90, Component.translatable(K + "market"), () -> {
			this.landView = false;
			this.rebuildWidgets();
		}).active = this.landView;
		this.button(x + 92, y - 3, 90, Component.translatable(K + "land"), () -> {
			this.landView = true;
			this.rebuildWidgets();
		}).active = !this.landView;
		this.label(x + 188, y + 1, Component.translatable(this.landView ? K + "release_hint" : K + "remove_hint").withStyle(ChatFormatting.DARK_GRAY));
		if (this.landView) {
			this.land(x, y + ROW + 2);
		} else {
			this.market(x, y + ROW + 2);
		}
	}

	private void market(final int x, final int y) {
		this.box(x, y, 220, "search", Component.translatable(K + "search"), 40, s -> true);
		this.button(x + 222, y, 60, Component.translatable(K + "search_go"), () -> this.refresh(0, this.state.landPage()));
		List<AdminStatePayload.Listing> listings = this.state.listings();
		if (listings.isEmpty()) {
			this.label(x + 6, y + 22, Component.translatable(K + "none").withStyle(ChatFormatting.GRAY));
		}
		for (int i = 0; i < listings.size(); i++) {
			AdminStatePayload.Listing l = listings.get(i);
			int ry = y + 20 + i * LISTING_ROW;
			Component line = l.item().getHoverName().copy().append(" ×" + l.item().getCount()).append("  ").append(Coins.component(l.price()))
				.append(Component.literal("  " + l.seller() + "  ").withStyle(ChatFormatting.GRAY)).append(RealReset.remaining(l.ticksLeft()).copy().withStyle(ChatFormatting.DARK_GRAY));
			this.label(x + 20, ry + 4, line);
			this.confirm(x + 330, ry + 1, 60, "listing" + l.id(), Component.translatable(K + "remove"), () -> this.send(AdminActionPayload.AUCTION_REMOVE, "", l.id(), 0, 0));
		}
		this.pager(x + 324, y, this.state.marketPage(), this.state.marketPages(), page -> this.refresh(page, this.state.landPage()));
	}

	private void land(final int x, final int y) {
		List<AdminStatePayload.Plot> plots = this.state.plots();
		if (plots.isEmpty()) {
			this.label(x + 6, y + 4, Component.translatable(K + "none").withStyle(ChatFormatting.GRAY));
		}
		for (int i = 0; i < plots.size(); i++) {
			AdminStatePayload.Plot p = plots.get(i);
			int ry = y + i * ROW;
			int bx = p.chunkX() << 4;
			int bz = p.chunkZ() << 4;
			this.label(x, ry + 3, Component.literal(p.ownerName()).withStyle(ChatFormatting.WHITE)
				.append(Component.translatable(K + "plot", bx, bz, p.minY(), p.trusted()).withStyle(ChatFormatting.GRAY)));
			this.button(x + 270, ry, 58, Component.translatable(K + "go"),
				() -> this.send(AdminActionPayload.TP_COORDS, RESIDENCE, bx + 8, p.minY() + 1, bz + 8));
			this.confirm(x + 330, ry, 60, "plot" + p.chunkX() + "," + p.chunkZ() + "," + p.minY(), Component.translatable(K + "release").withStyle(ChatFormatting.RED),
				() -> this.send(AdminActionPayload.LAND_RELEASE, p.owner().toString(), p.chunkX(), p.chunkZ(), p.minY()));
		}
		this.pager(x + 324, y - ROW - 5, this.state.landPage(), this.state.landPages(), page -> this.refresh(this.state.marketPage(), page));
	}

	@Override
	protected void extractExtra(final GuiGraphicsExtractor g, final int mouseX, final int mouseY) {
		if (this.state == null || this.tab != ECONOMY || this.landView) {
			return;
		}
		int x = this.left + 10;
		int y = this.top + 44 + ROW + 2;
		for (int i = 0; i < this.state.listings().size(); i++) {
			g.item(this.state.listings().get(i).item(), x, y + 20 + i * LISTING_ROW);
		}
	}
}
