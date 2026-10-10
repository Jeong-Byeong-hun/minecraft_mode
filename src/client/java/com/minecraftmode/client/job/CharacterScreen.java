package com.minecraftmode.client.job;

import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.JobStats;
import com.minecraftmode.job.Paragon;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.engrave.EngraveTotals;
import com.minecraftmode.job.gear.GearStats;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Character screen (I, or the button on the class screen): who the player is (class, level, paragon), their vanilla attributes and
 * MP on the left; on the right every stat their gear, level, passives, buffs, talents, codex, paragon and companions add up to
 * ({@link GearStats}), scrolling, with the sources of each line on hover and the cap where one applies.
 */
public class CharacterScreen extends Screen {
	private static final int W = 320;
	private static final int H = 236;
	private static final int COL = 150;
	private static final int ROW = 11;
	private static final int LIST_X = 162;
	private static final int LIST_Y = 28;
	private static final int LIST_H = H - LIST_Y - 32;

	private int left;
	private int top;
	private int scroll;

	public CharacterScreen() {
		super(Component.translatable("screen.minecraft_mode.character.title"));
	}

	@Override
	protected void init() {
		this.left = (this.width - W) / 2;
		this.top = (this.height - H) / 2;
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(this.left + W - 72, this.top + H - 26, 64, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.job"), b -> this.minecraft.gui.setScreen(new JobScreen()))
			.bounds(this.left + 8, this.top + H - 26, 64, 20).build());
	}

	/** The stats with a value, in the enum's order (offence, defence, utility). */
	private static List<EngraveStat> shown(final EngraveTotals totals, final Map<EngraveStat, Map<GearStats.Source, Float>> breakdown) {
		List<EngraveStat> stats = new ArrayList<>();
		for (EngraveStat stat : EngraveStat.values()) {
			if (breakdown.containsKey(stat) && totals.get(stat) != 0.0F) {
				stats.add(stat);
			}
		}
		return stats;
	}

	private int visibleRows() {
		return LIST_H / ROW;
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
		g.fill(x + LIST_X - 6, y + 24, x + LIST_X - 5, y + H - 30, 0x60FFFFFF);

		JobData data = JobProgression.get(player);
		int ty = y + 28;
		Component job = data.hasClass() ? Component.translatable(data.job().tierKey(data.tier())).withColor(data.job().color())
			: Component.translatable("screen.minecraft_mode.character.no_class").withStyle(ChatFormatting.GRAY);
		g.text(this.font, Component.empty().append(player.getName()).append(" · ").append(job), x + 10, ty, 0xFFFFFFFF, false);
		ty += ROW;
		Paragon.ParagonData paragon = Paragon.get(player);
		Component level = data.level() >= JobProgression.MAX_LEVEL && paragon.level() > 0
			? Component.translatable("screen.minecraft_mode.character.level_paragon", data.level(), paragon.level())
			: Component.translatable("screen.minecraft_mode.character.level", data.level(), data.exp(), JobProgression.expToNext(data.level()));
		g.text(this.font, level, x + 10, ty, 0xFFE0C090, false);
		ty += ROW + 4;

		g.text(this.font, Component.translatable("screen.minecraft_mode.character.attributes").withStyle(ChatFormatting.AQUA), x + 10, ty, 0xFFFFFFFF, false);
		ty += ROW;
		ty = this.attribute(g, x, ty, "health", Attributes.MAX_HEALTH, player, 1.0);
		ty = this.attribute(g, x, ty, "armor", Attributes.ARMOR, player, 1.0);
		ty = this.attribute(g, x, ty, "toughness", Attributes.ARMOR_TOUGHNESS, player, 1.0);
		ty = this.attribute(g, x, ty, "attack", Attributes.ATTACK_DAMAGE, player, 1.0);
		ty = this.attribute(g, x, ty, "attack_speed", Attributes.ATTACK_SPEED, player, 1.0);
		// blocks per second while walking (vanilla runs about 4.3 at the base 0.1)
		ty = this.attribute(g, x, ty, "speed", Attributes.MOVEMENT_SPEED, player, 43.17);
		ty = this.row(g, x, ty, Component.translatable("screen.minecraft_mode.character.mana"), String.valueOf(JobStats.maxMana(player)));
		ty = this.row(g, x, ty, Component.translatable("screen.minecraft_mode.character.mana_regen"), "+" + JobStats.manaRegen(player) + "/s");
		ty += 4;

		EngraveTotals totals = GearStats.of(player);
		g.text(this.font, Component.translatable("screen.minecraft_mode.character.combat").withStyle(ChatFormatting.AQUA), x + 10, ty, 0xFFFFFFFF, false);
		ty += ROW;
		ty = this.row(g, x, ty, Component.translatable("screen.minecraft_mode.character.crit_chance"), JobTooltips.num(totals.get(EngraveStat.CRIT_CHANCE)) + "%");
		this.row(g, x, ty, Component.translatable("screen.minecraft_mode.character.crit_damage"), JobTooltips.num(150.0F + totals.get(EngraveStat.CRIT_DAMAGE)) + "%");

		Map<EngraveStat, Map<GearStats.Source, Float>> breakdown = GearStats.breakdown(player);
		List<EngraveStat> stats = shown(totals, breakdown);
		int rows = this.visibleRows();
		this.scroll = Mth.clamp(this.scroll, 0, Math.max(0, stats.size() - rows));
		g.text(this.font, Component.translatable("screen.minecraft_mode.character.stats", stats.size()).withStyle(ChatFormatting.AQUA), x + LIST_X, y + 14, 0xFFFFFFFF, false);
		List<Component> tip = null;
		if (stats.isEmpty()) {
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.character.no_stats"), x + LIST_X, y + LIST_Y, COL, 0xFF8A8A8A);
		}
		for (int i = 0; i < rows && this.scroll + i < stats.size(); i++) {
			EngraveStat stat = stats.get(this.scroll + i);
			int ry = y + LIST_Y + i * ROW;
			float value = totals.get(stat);
			float raw = 0.0F;
			for (float v : breakdown.get(stat).values()) {
				raw += v;
			}
			boolean capped = stat.cap() > 0.0F && raw > stat.cap();
			Component line = Component.translatable(stat.key(), JobTooltips.num(value));
			// long lines are cut at the scroll bar; the tooltip has them whole
			String text = line.getString();
			int room = W - LIST_X - 12;
			String shown = this.font.width(text) <= room ? text : this.font.plainSubstrByWidth(text, room - this.font.width("…")) + "…";
			g.text(this.font, shown, x + LIST_X, ry, capped ? 0xFFFFB347 : 0xFFDDDDDD, false);
			if (mouseX >= x + LIST_X && mouseX < x + LIST_X + COL && mouseY >= ry && mouseY < ry + ROW) {
				tip = new ArrayList<>();
				tip.add(line.copy().withStyle(ChatFormatting.WHITE));
				for (Map.Entry<GearStats.Source, Float> source : breakdown.get(stat).entrySet()) {
					tip.add(Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY).append(Component.translatable(source.getKey().key())
						.append(" " + (source.getValue() >= 0 ? "+" : "") + JobTooltips.num(source.getValue())).withStyle(ChatFormatting.GRAY)));
				}
				if (stat.cap() > 0.0F) {
					tip.add(Component.translatable("screen.minecraft_mode.character.cap", JobTooltips.num(stat.cap()), JobTooltips.num(raw))
						.withStyle(capped ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
				}
			}
		}
		if (stats.size() > rows) {
			// scroll bar
			int barX = x + W - 7;
			int barTop = y + LIST_Y;
			int knob = Math.max(10, LIST_H * rows / stats.size());
			int knobY = barTop + (LIST_H - knob) * this.scroll / Math.max(1, stats.size() - rows);
			g.fill(barX, barTop, barX + 3, barTop + LIST_H, 0x40FFFFFF);
			g.fill(barX, knobY, barX + 3, knobY + knob, 0xFFE8C24A);
		}
		super.extractRenderState(g, mouseX, mouseY, a);
		if (tip != null) {
			g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
		}
	}

	private int attribute(final GuiGraphicsExtractor g, final int x, final int y, final String name, final Holder<Attribute> attribute, final LocalPlayer player,
		final double scale) {
		double value = player.getAttributes().hasAttribute(attribute) ? player.getAttributeValue(attribute) * scale : 0.0;
		return this.row(g, x, y, Component.translatable("screen.minecraft_mode.character." + name), JobTooltips.num(value));
	}

	private int row(final GuiGraphicsExtractor g, final int x, final int y, final Component label, final String value) {
		g.text(this.font, label, x + 14, y, 0xFFBBBBBB, false);
		g.text(this.font, value, x + 10 + COL - 6 - this.font.width(value), y, 0xFFFFFFFF, false);
		return y + ROW;
	}

	@Override
	public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
		if (mouseX >= this.left + LIST_X - 6) {
			this.scroll -= (int) Math.signum(scrollY) * 3;
			if (this.scroll < 0) {
				this.scroll = 0;
			}
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	/** Scrolls the stat list to the end (the game test's second screenshot). */
	public void scrollToEnd() {
		this.scroll = Integer.MAX_VALUE / 2;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
