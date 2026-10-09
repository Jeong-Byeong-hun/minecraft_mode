package com.minecraftmode.client.raid;

import com.minecraftmode.client.job.JobTooltips;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.network.OpenRaidPayload;
import com.minecraftmode.network.PartySyncPayload;
import com.minecraftmode.network.RaidEnterPayload;
import com.minecraftmode.progress.PlayerRecords;
import com.minecraftmode.progress.Progress;
import com.minecraftmode.progress.ResetCycle;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.Parties;
import com.minecraftmode.raid.RaidAffix;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.raid.RaidDifficulty;
import com.minecraftmode.raid.RaidRecordsData;
import com.minecraftmode.raid.Raids;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

/**
 * The raid marshal's board: the six bosses on the left and the difficulty along the top; the chosen boss's level range, arena,
 * fee, whether this cycle's reward is still open, the cycle's modifiers and story on the right, then the party with who meets
 * the level. The records button swaps the story for the fastest clears and the modifier details. The leader (or a solo player)
 * starts the raid; the server checks everything again.
 */
public class RaidScreen extends Screen {
	/** 320 wide: the narrowest GUI Minecraft lays out (4:3 screens at auto GUI scale). */
	private static final int W = 320;
	private static final int H = 236;
	private static int selected;
	private static int difficulty;
	private static boolean records;

	private final int marshalId;
	private List<PartySyncPayload.Member> shownParty = List.of();
	private int left;
	private int top;
	private int age;

	public RaidScreen(final int marshalId) {
		super(Component.translatable("screen.minecraft_mode.raid.title"));
		this.marshalId = marshalId;
	}

	/** Picks the boss, the difficulty and the records view the next board opens with (the screen remembers them; also used by tests). */
	public static void preset(final int boss, final RaidDifficulty level, final boolean showRecords) {
		selected = boss;
		difficulty = level.ordinal();
		records = showRecords;
	}

	private BossDef boss() {
		List<BossDef> all = RaidBosses.all();
		return all.get(Math.floorMod(selected, all.size()));
	}

	private static RaidDifficulty difficulty() {
		return RaidDifficulty.values()[Math.floorMod(difficulty, RaidDifficulty.values().length)];
	}

	public static int color(final RaidDifficulty d) {
		return switch (d) {
			case NORMAL -> 0xFF7CE07C;
			case HEROIC -> 0xFFC08AFF;
			case NIGHTMARE -> 0xFFFF5A5A;
		};
	}

	@Override
	protected void init() {
		this.left = (this.width - W) / 2;
		this.top = (this.height - H) / 2;
		this.shownParty = RaidClient.party();
		List<BossDef> all = RaidBosses.all();
		for (int i = 0; i < all.size(); i++) {
			BossDef def = all.get(i);
			final int index = i;
			Button b = Button.builder(Component.translatable(def.nameKey()), button -> {
				selected = index;
				this.rebuildWidgets();
			}).bounds(this.left + 8, this.top + 26 + i * 22, 100, 20).build();
			b.active = i != Math.floorMod(selected, all.size());
			this.addRenderableWidget(b);
		}
		RaidDifficulty[] levels = RaidDifficulty.values();
		for (int i = 0; i < levels.length; i++) {
			RaidDifficulty d = levels[i];
			final int index = i;
			Button b = Button.builder(Component.translatable(d.nameKey()).withColor(color(d)), button -> {
				difficulty = index;
				this.rebuildWidgets();
			}).bounds(this.left + W - 8 - (levels.length - i) * 62, this.top + 5, 60, 16).build();
			b.active = d != difficulty();
			this.addRenderableWidget(b);
		}
		this.addRenderableWidget(Button.builder(Component.translatable(records ? "screen.minecraft_mode.raid.show_info" : "screen.minecraft_mode.raid.show_records"), b -> {
			records = !records;
			this.rebuildWidgets();
		}).bounds(this.left + 8, this.top + 160, 100, 20).build());
		Button enter = Button.builder(Component.translatable("screen.minecraft_mode.raid.enter").withStyle(ChatFormatting.BOLD), b -> this.enter())
			.bounds(this.left + 8, this.top + H - 52, 100, 20).build();
		enter.active = this.canEnter();
		this.addRenderableWidget(enter);
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(this.left + 8, this.top + H - 28, 100, 20).build());
	}

	private boolean isLeader() {
		LocalPlayer player = this.minecraft == null ? null : this.minecraft.player;
		List<PartySyncPayload.Member> party = RaidClient.party();
		return player != null && (party.isEmpty() || party.getFirst().id().equals(player.getUUID()));
	}

	private static long cycle() {
		OpenRaidPayload info = RaidClient.raidInfo();
		return info == null ? -1L : info.cycle();
	}

	/** Whether {@code player} already took this boss and difficulty's reward this cycle. */
	private boolean locked(final LocalPlayer player) {
		return Progress.get(player).raidLocked(PlayerRecords.raidKey(this.boss().id(), difficulty().id()), cycle());
	}

	/** The difficulty this player must clear first, or null when it is open. */
	private @Nullable RaidDifficulty missing(final LocalPlayer player) {
		RaidDifficulty previous = difficulty().previous();
		return previous != null && Progress.get(player).raidClears(PlayerRecords.raidKey(this.boss().id(), previous.id())) == 0 ? previous : null;
	}

	private boolean canEnter() {
		LocalPlayer player = this.minecraft == null ? null : this.minecraft.player;
		if (player == null || !this.isLeader()) {
			return false;
		}
		BossDef def = this.boss();
		if (this.missing(player) != null || !this.locked(player) && Coins.total(player) < difficulty().fee(def)) {
			return false;
		}
		List<PartySyncPayload.Member> party = RaidClient.party();
		if (party.isEmpty()) {
			return JobProgression.get(player).level() >= def.minLevel();
		}
		for (PartySyncPayload.Member m : party) {
			if (m.online() && m.level() < def.minLevel()) {
				return false;
			}
		}
		return true;
	}

	private void enter() {
		if (ClientPlayNetworking.canSend(RaidEnterPayload.TYPE)) {
			ClientPlayNetworking.send(new RaidEnterPayload(this.marshalId, this.boss().id(), difficulty().id()));
		}
		this.onClose();
	}

	@Override
	public void tick() {
		this.age++;
		if (RaidClient.party() != this.shownParty) {
			this.rebuildWidgets();
		}
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor g, final int mouseX, final int mouseY, final float a) {
		LocalPlayer player = this.minecraft == null ? null : this.minecraft.player;
		if (player == null) {
			return;
		}
		int x = this.left;
		int y = this.top;
		BossDef def = this.boss();
		RaidDifficulty diff = difficulty();
		int color = switch (def.color()) {
			case GREEN -> 0xFF7CE07C;
			case BLUE -> 0xFF6FB8FF;
			case RED -> 0xFFFF6A4A;
			case PURPLE -> 0xFFC08AFF;
			case PINK -> 0xFFFF8AD0;
			case YELLOW -> 0xFFFFE070;
			case WHITE -> 0xFFE8E8E8;
		};
		g.fill(x, y, x + W, y + H, 0xE8101018);
		g.outline(x, y, W, H, 0xFFC0263A);
		g.text(this.font, this.title.copy().withStyle(ChatFormatting.GOLD), x + 10, y + 10, 0xFFFFFFFF, true);
		g.fill(x + 116, y + 26, x + 117, y + H - 8, 0x40FFFFFF);

		int rx = x + 124;
		int rw = W - 132;
		int ty = y + 26;
		g.text(this.font, Component.translatable(def.nameKey()).withStyle(ChatFormatting.BOLD), rx, ty, color, true);
		g.text(this.font, Component.translatable(def.epithetKey()), rx, ty + 11, 0xFFAAAAAA, false);
		ty += 24;
		if (records) {
			this.records(g, rx, ty, rw, def);
			super.extractRenderState(g, mouseX, mouseY, a);
			return;
		}
		g.text(this.font, Component.translatable("screen.minecraft_mode.raid.levels", def.lo(), def.hi()), rx, ty, 0xFFFFD27F, false);
		g.text(this.font, Component.translatable("screen.minecraft_mode.raid.phases", def.phaseCount()), rx + rw / 2, ty, 0xFFFF8A8A, false);
		ty += 11;
		g.text(this.font, Component.translatable("screen.minecraft_mode.raid.arena", Component.translatable("screen.minecraft_mode.raid.arena." + def.arena().name().toLowerCase())),
			rx, ty, 0xFFBBBBBB, false);
		ty += 11;

		boolean locked = this.locked(player);
		int fee = diff.fee(def);
		Component feeText = locked ? Component.translatable("screen.minecraft_mode.raid.practice_fee")
			: Component.translatable("screen.minecraft_mode.raid.fee", Coins.format(fee));
		g.text(this.font, feeText, rx, ty, locked || Coins.total(player) >= fee ? 0xFFFFD27F : 0xFFFF6B6B, false);
		ty += 10;
		g.text(this.font, Component.translatable("screen.minecraft_mode.raid.reset", ResetCycle.remaining(this.cycleLeft())), rx, ty, 0xFF8A8A8A, false);
		ty += 11;
		RaidDifficulty missing = this.missing(player);
		Component status = missing != null ? Component.translatable("screen.minecraft_mode.raid.needs", Component.translatable(missing.nameKey()))
			: locked ? Component.translatable("screen.minecraft_mode.raid.reward_taken") : Component.translatable("screen.minecraft_mode.raid.reward_open");
		ty = g.textWithWordWrap(this.font, status, rx, ty, rw, missing != null ? 0xFFFF6B6B : locked ? 0xFFFFD27F : 0xFF7CFC7C) + 2;
		if (diff.modified()) {
			ty = g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.raid.difficulty_info",
				times(diff.health), times(diff.damage), affixNames()), rx, ty, rw, color(diff)) + 2;
		}
		ty += 2;
		ty = g.textWithWordWrap(this.font, Component.translatable(def.descKey()).withStyle(ChatFormatting.ITALIC), rx, ty, rw, 0xFFD0D0D0) + 4;
		g.fill(rx, ty, rx + rw, ty + 1, 0x40FFFFFF);
		ty += 4;

		List<PartySyncPayload.Member> party = RaidClient.party();
		g.text(this.font, Component.translatable("screen.minecraft_mode.raid.party", Math.max(1, party.size()), Parties.MAX_SIZE), rx, ty, 0xFFFFFFFF, false);
		ty += 11;
		if (party.isEmpty()) {
			int level = JobProgression.get(player).level();
			this.member(g, rx, ty, rw, player.getPlainTextName(), JobProgression.get(player).job(), level, true, level >= def.minLevel());
			ty += 10;
		} else {
			for (int i = 0; i < party.size(); i++) {
				PartySyncPayload.Member m = party.get(i);
				JobClass job = JobClass.values()[Math.floorMod(m.job(), JobClass.values().length)];
				this.member(g, rx, ty, rw, (i == 0 ? "★ " : "") + m.name(), job, m.level(), m.online(), m.level() >= def.minLevel());
				ty += 10;
			}
		}
		// the invite hint only while there is room (a full party fills the column)
		if (!this.isLeader()) {
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.raid.not_leader"), rx, ty + 4, rw, 0xFFFF9F6B);
		} else if (party.size() < 3) {
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.raid.hint", (int)Raids.GATHER_RANGE), rx, ty + 4, rw, 0xFF8A8A8A);
		}
		super.extractRenderState(g, mouseX, mouseY, a);
	}

	private static String times(final float v) {
		return "x" + JobTooltips.num(v);
	}

	private long cycleLeft() {
		OpenRaidPayload info = RaidClient.raidInfo();
		return info == null ? 0L : Math.max(0L, info.cycleLeft() - this.age);
	}

	private static Component affixNames() {
		MutableComponent out = Component.empty();
		List<RaidAffix> affixes = affixes();
		for (int i = 0; i < affixes.size(); i++) {
			if (i > 0) {
				out.append(", ");
			}
			out.append(Component.translatable(affixes.get(i).nameKey()));
		}
		return out;
	}

	private static List<RaidAffix> affixes() {
		OpenRaidPayload info = RaidClient.raidInfo();
		return info == null ? List.of() : info.affixes().stream().map(RaidAffix::byId).toList();
	}

	/** Fastest clears of the chosen boss and difficulty, then the cycle's modifiers in full. */
	private void records(final GuiGraphicsExtractor g, final int rx, int ty, final int rw, final BossDef def) {
		RaidDifficulty diff = difficulty();
		g.text(this.font, Component.translatable("screen.minecraft_mode.raid.records_title", Component.translatable(diff.nameKey())), rx, ty, color(diff), false);
		ty += 12;
		OpenRaidPayload info = RaidClient.raidInfo();
		List<RaidRecordsData.Entry> list = info == null ? List.of() : info.records().getOrDefault(PlayerRecords.raidKey(def.id(), diff.id()), List.of());
		if (list.isEmpty()) {
			g.text(this.font, Component.translatable("screen.minecraft_mode.raid.no_records"), rx, ty, 0xFF8A8A8A, false);
			ty += 11;
		}
		for (int i = 0; i < list.size(); i++) {
			RaidRecordsData.Entry e = list.get(i);
			int rank = i == 0 ? 0xFFFFD27F : i == 1 ? 0xFFD8D8E0 : i == 2 ? 0xFFD89A5A : 0xFFB0B0B0;
			String head = (i + 1) + ". " + Raids.clock(e.ticks());
			g.text(this.font, head, rx, ty, rank, false);
			Component day = Component.translatable("screen.minecraft_mode.raid.record_day", e.day() + 1);
			g.text(this.font, day, rx + rw - this.font.width(day), ty, 0xFF707070, false);
			ty += 10;
			String names = String.join(", ", e.names());
			ty = g.textWithWordWrap(this.font, Component.literal(names), rx + 8, ty, rw - 8, 0xFFCCCCCC) + 3;
		}
		ty += 4;
		g.fill(rx, ty, rx + rw, ty + 1, 0x40FFFFFF);
		ty += 5;
		g.text(this.font, Component.translatable("screen.minecraft_mode.raid.affix_title"), rx, ty, 0xFFC08AFF, false);
		ty += 11;
		for (RaidAffix affix : affixes()) {
			g.text(this.font, Component.translatable(affix.nameKey()).withStyle(ChatFormatting.BOLD), rx, ty, 0xFFE0C0FF, false);
			ty += 10;
			ty = g.textWithWordWrap(this.font, Component.translatable(affix.descKey()), rx + 8, ty, rw - 8, 0xFFB0B0B0) + 3;
		}
	}

	private void member(final GuiGraphicsExtractor g, final int x, final int y, final int w, final String name, final JobClass job, final int level, final boolean online,
		final boolean ready) {
		g.fill(x, y + 1, x + 2, y + 8, 0xFF000000 | job.color());
		g.text(this.font, name, x + 5, y, online ? 0xFFFFFFFF : 0xFF707070, false);
		String status = !online ? "-" : (ready ? "✔ " : "✘ ") + "Lv" + level;
		g.text(this.font, status, x + w - this.font.width(status), y, !online ? 0xFF707070 : ready ? 0xFF7CFC7C : 0xFFFF6B6B, false);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
