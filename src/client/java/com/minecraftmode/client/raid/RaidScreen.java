package com.minecraftmode.client.raid;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.network.PartySyncPayload;
import com.minecraftmode.network.RaidEnterPayload;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.Parties;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.raid.Raids;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * The raid marshal's board: the six bosses on the left; the chosen boss's level range, arena,
 * phases and story on the right, then the party with who meets the level. The leader (or a solo
 * player) starts the raid; the server checks everything again.
 */
public class RaidScreen extends Screen {
	/** 320 wide: the narrowest GUI Minecraft lays out (4:3 screens at auto GUI scale). */
	private static final int W = 320;
	private static final int H = 236;
	private static int selected;

	private final int marshalId;
	private List<PartySyncPayload.Member> shownParty = List.of();
	private int left;
	private int top;

	public RaidScreen(final int marshalId) {
		super(Component.translatable("screen.minecraft_mode.raid.title"));
		this.marshalId = marshalId;
	}

	private BossDef boss() {
		List<BossDef> all = RaidBosses.all();
		return all.get(Math.floorMod(selected, all.size()));
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
			}).bounds(this.left + 8, this.top + 26 + i * 24, 100, 20).build();
			b.active = i != Math.floorMod(selected, all.size());
			this.addRenderableWidget(b);
		}
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

	private boolean canEnter() {
		LocalPlayer player = this.minecraft == null ? null : this.minecraft.player;
		if (player == null || !this.isLeader()) {
			return false;
		}
		BossDef def = this.boss();
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
			ClientPlayNetworking.send(new RaidEnterPayload(this.marshalId, this.boss().id()));
		}
		this.onClose();
	}

	@Override
	public void tick() {
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
		ty += 26;
		g.text(this.font, Component.translatable("screen.minecraft_mode.raid.levels", def.lo(), def.hi()), rx, ty, 0xFFFFD27F, false);
		g.text(this.font, Component.translatable("screen.minecraft_mode.raid.phases", def.phaseCount()), rx + rw / 2, ty, 0xFFFF8A8A, false);
		ty += 11;
		g.text(this.font, Component.translatable("screen.minecraft_mode.raid.arena", Component.translatable("screen.minecraft_mode.raid.arena." + def.arena().name().toLowerCase())),
			rx, ty, 0xFFBBBBBB, false);
		ty += 14;
		ty = g.textWithWordWrap(this.font, Component.translatable(def.descKey()).withStyle(ChatFormatting.ITALIC), rx, ty, rw, 0xFFD0D0D0) + 6;
		g.fill(rx, ty, rx + rw, ty + 1, 0x40FFFFFF);
		ty += 5;

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
		} else if (party.size() < 5) {
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.raid.hint", (int)Raids.GATHER_RANGE), rx, ty + 4, rw, 0xFF8A8A8A);
		}
		super.extractRenderState(g, mouseX, mouseY, a);
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
