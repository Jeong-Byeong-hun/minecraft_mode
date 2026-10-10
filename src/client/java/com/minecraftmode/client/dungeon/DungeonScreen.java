package com.minecraftmode.client.dungeon;

import com.minecraftmode.dungeon.DungeonAffix;
import com.minecraftmode.dungeon.DungeonData;
import com.minecraftmode.dungeon.DungeonDef;
import com.minecraftmode.dungeon.Dungeons;
import com.minecraftmode.dungeon.Keystone;
import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.network.DungeonEnterPayload;
import com.minecraftmode.network.OpenDungeonPayload;
import com.minecraftmode.raid.Raids;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

/**
 * The dungeon warden's screen: every dungeon with its level, boss, time limit and your records; enter without a keystone, or with
 * your best keystone for it. Shows this cycle's keystone modifiers. The list shows {@link #VISIBLE} dungeons at a time and scrolls
 * with the mouse wheel; it opens on the highest dungeon the player may enter.
 */
public class DungeonScreen extends Screen {
	private static final int W = 340;
	private static final int H = 232;
	private static final int ROW = 32;
	private static final int ROWS_Y = 24;
	private static final int VISIBLE = 5;

	private final OpenDungeonPayload payload;
	private int left;
	private int top;
	/** Index of the first dungeon shown, -1 until the screen first opens. */
	private int first = -1;

	public DungeonScreen(final OpenDungeonPayload payload) {
		super(Component.translatable("screen.minecraft_mode.dungeon.title"));
		this.payload = payload;
	}

	public int warden() {
		return this.payload.entityId();
	}

	@Override
	protected void init() {
		this.left = (this.width - W) / 2;
		this.top = (this.height - H) / 2;
		LocalPlayer player = this.minecraft.player;
		if (player == null) {
			return;
		}
		int level = JobProgression.get(player).level();
		List<DungeonDef> defs = new ArrayList<>(Dungeons.all());
		if (this.first < 0) {
			int highest = 0;
			for (int i = 0; i < defs.size(); i++) {
				if (level >= defs.get(i).minLevel()) {
					highest = i;
				}
			}
			this.first = highest - VISIBLE + 2;
		}
		this.first = Math.clamp(this.first, 0, Math.max(0, defs.size() - VISIBLE));
		for (int i = this.first; i < Math.min(defs.size(), this.first + VISIBLE); i++) {
			DungeonDef def = defs.get(i);
			int ry = this.top + ROWS_Y + (i - this.first) * ROW;
			Button normal = Button.builder(Component.translatable("screen.minecraft_mode.dungeon.enter"), b -> this.enter(def, false))
				.bounds(this.left + W - 112, ry + 7, 50, 18).build();
			normal.active = level >= def.minLevel();
			this.addRenderableWidget(normal);
			Keystone keystone = Dungeons.keystone(player, def.id());
			Button key = Button.builder(keystone == null ? Component.literal("—") : Component.literal("+" + keystone.level()).withStyle(ChatFormatting.LIGHT_PURPLE),
				b -> this.enter(def, true)).bounds(this.left + W - 58, ry + 7, 50, 18).build();
			key.active = keystone != null && level >= def.minLevel();
			this.addRenderableWidget(key);
		}
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(this.left + W - 68, this.top + H - 22, 60, 18).build());
	}

	private void enter(final DungeonDef def, final boolean keystone) {
		if (ClientPlayNetworking.canSend(DungeonEnterPayload.TYPE)) {
			ClientPlayNetworking.send(new DungeonEnterPayload(this.payload.entityId(), def.id(), keystone));
		}
		this.onClose();
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
		g.outline(x, y, W, H, 0xFF5AA87A);
		g.text(this.font, this.title.copy().withStyle(ChatFormatting.GREEN), x + 10, y + 9, 0xFFFFFFFF, true);
		Component party = Component.translatable("screen.minecraft_mode.dungeon.party", Dungeons.MAX_PARTY);
		g.text(this.font, party, x + W - 10 - this.font.width(party), y + 9, 0xFF8A8A8A, false);
		DungeonData data = Dungeons.data(player);
		int level = JobProgression.get(player).level();
		List<Component> tip = null;
		List<DungeonDef> defs = new ArrayList<>(Dungeons.all());
		for (int i = this.first; i < Math.min(defs.size(), this.first + VISIBLE); i++) {
			DungeonDef def = defs.get(i);
			int ry = y + ROWS_Y + (i - this.first) * ROW;
			boolean open = level >= def.minLevel();
			g.fill(x + 8, ry, x + W - 8, ry + ROW - 2, open ? 0x30FFFFFF : 0x40000000);
			NamedDef boss = NamedMobs.byId(def.boss());
			if (boss != null) {
				g.fakeItem(new ItemStack(NamedMobs.egg(boss)), x + 12, ry + 7);
			}
			g.text(this.font, Component.translatable(def.nameKey()), x + 34, ry + 4, open ? 0xFFFFD27F : 0xFF6A6A6A, false);
			MutableComponent info = Component.translatable("screen.minecraft_mode.dungeon.info", def.minLevel(), Raids.clock(def.timeLimit() * 20));
			g.text(this.font, info, x + 34, ry + 16, open ? 0xFFBBBBBB : 0xFF6A6A6A, false);
			String record = data.clears(def.id()) == 0 ? "" : "×" + data.clears(def.id()) + (data.best(def.id()) > 0 ? "  ★+" + data.best(def.id()) : "");
			g.text(this.font, record, x + W - 118 - this.font.width(record), ry + 16, 0xFF7CFC7C, false);
			if (mouseX >= x + 8 && mouseX < x + W - 116 && mouseY >= ry && mouseY < ry + ROW - 2) {
				List<Component> lines = new ArrayList<>();
				lines.add(Component.translatable(def.nameKey()).withStyle(ChatFormatting.GOLD));
				lines.add(Component.translatable(def.descKey()).withStyle(ChatFormatting.GRAY));
				if (boss != null) {
					lines.add(Component.translatable("screen.minecraft_mode.dungeon.boss", Component.translatable(boss.nameKey())).withStyle(ChatFormatting.RED));
				}
				lines.add(Component.translatable("screen.minecraft_mode.dungeon.clears", data.clears(def.id()), data.best(def.id())).withStyle(ChatFormatting.GREEN));
				lines.add(Component.translatable("screen.minecraft_mode.dungeon.keystone_hint").withStyle(ChatFormatting.DARK_GRAY));
				tip = lines;
			}
		}
		if (defs.size() > VISIBLE) {
			// scroll bar beside the rows
			int track = VISIBLE * ROW - 2;
			int thumb = Math.max(10, track * VISIBLE / defs.size());
			int thumbTop = y + ROWS_Y + (track - thumb) * this.first / (defs.size() - VISIBLE);
			g.fill(x + W - 6, y + ROWS_Y, x + W - 4, y + ROWS_Y + track, 0x40FFFFFF);
			g.fill(x + W - 6, thumbTop, x + W - 4, thumbTop + thumb, 0xC0FFFFFF);
		}
		// this cycle's modifiers
		int fy = y + ROWS_Y + Math.min(VISIBLE, defs.size()) * ROW + 2;
		MutableComponent affixes = Component.translatable("screen.minecraft_mode.dungeon.affixes");
		for (DungeonAffix affix : DungeonAffix.forRun(99, this.payload.cycle())) {
			affixes.append(Component.literal("  +" + affix.level() + " ")).append(Component.translatable(affix.nameKey()));
		}
		g.text(this.font, affixes, x + 10, fy, 0xFFC08AFF, false);
		if (mouseX >= x + 8 && mouseX < x + W - 8 && mouseY >= fy - 2 && mouseY < fy + 10) {
			List<Component> lines = new ArrayList<>();
			for (DungeonAffix affix : DungeonAffix.forRun(99, this.payload.cycle())) {
				lines.add(Component.literal("+" + affix.level() + " ").append(Component.translatable(affix.nameKey())).withStyle(ChatFormatting.LIGHT_PURPLE));
				lines.add(Component.translatable(affix.descKey()).withStyle(ChatFormatting.GRAY));
			}
			tip = lines;
		}
		g.text(this.font, this.font.plainSubstrByWidth(Component.translatable("screen.minecraft_mode.dungeon.footer").getString(), W - 84), x + 10, y + H - 17,
			0xFF8A8A8A, false);
		super.extractRenderState(g, mouseX, mouseY, a);
		if (tip != null) {
			g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
		int max = Math.max(0, Dungeons.all().size() - VISIBLE);
		if (max > 0 && scrollY != 0) {
			int next = Math.clamp(this.first - (int)Math.signum(scrollY), 0, max);
			if (next != this.first) {
				this.first = next;
				this.rebuildWidgets();
			}
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
