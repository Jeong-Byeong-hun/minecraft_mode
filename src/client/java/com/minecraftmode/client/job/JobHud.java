package com.minecraftmode.client.job;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.economy.Essence;
import com.minecraftmode.economy.Wallet;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.JobStats;
import com.minecraftmode.job.Paragon;
import com.minecraftmode.job.engrave.EngraveTotals;
import com.minecraftmode.job.quest.QuestData;
import com.minecraftmode.job.quest.QuestDef;
import com.minecraftmode.job.quest.QuestService;
import com.minecraftmode.job.skill.Skill;
import com.minecraftmode.job.skill.SkillCaster;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.WeaponDef;
import com.minecraftmode.loot.Coins;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Top-left: class title, level, experience and MP bars. Right of the hotbar: the held class weapon's
 * skill slots with their key, cooldown sweep and seconds left (grey when the weapon is not usable,
 * blue when MP is short).
 */
public final class JobHud {
	/** Bottom of the top-left panels drawn this frame (other HUD parts stay below it). */
	private static int leftBottom = 44;
	/** Right edge of the class panel drawn last (the target health bar stays right of it). */
	private static int panelRight = 128;

	public static void init() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, MinecraftMode.id("job_hud"), JobHud::extract);
	}

	private static void extract(final GuiGraphicsExtractor graphics, final DeltaTracker delta) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || player.isSpectator()) {
			return;
		}
		JobData data = JobProgression.get(player);
		leftBottom = 44;
		drawPanel(graphics, minecraft.font, player, data);
		drawQuest(graphics, minecraft.font, player);
		drawSkills(graphics, minecraft, player, data);
	}

	private static void drawPanel(final GuiGraphicsExtractor g, final Font font, final LocalPlayer player, final JobData data) {
		int x = 4;
		int y = 4;
		Component title = Component.translatable(data.job().tierKey(data.tier())).withColor(data.job().color());
		Paragon.ParagonData paragon = Paragon.get(player);
		String level = "Lv " + data.level() + (data.level() >= JobProgression.MAX_LEVEL && paragon.level() > 0 ? " ✦" + paragon.level() : "");
		int w = Math.max(124, font.width(title) + font.width(level) + 18);
		int color = 0xFF000000 | data.job().color();
		panelRight = x + w;
		g.fill(x, y, x + w, y + 40, 0x90000000);
		g.fill(x, y, x + 2, y + 40, color);
		g.text(font, title, x + 6, y + 3, 0xFFFFFFFF, true);
		g.text(font, level, x + w - 4 - font.width(level), y + 3, 0xFFFFE08A, true);
		// experience
		int need = JobProgression.expToNext(data.level());
		boolean capped = data.level() >= JobProgression.MAX_LEVEL;
		float expFraction = capped ? Math.min(1.0F, (float)paragon.exp() / Paragon.expToNext(paragon.level())) : Math.min(1.0F, (float)data.exp() / need);
		// past the cap the bar fills paragon levels (violet)
		bar(g, x + 6, y + 14, w - 10, 3, expFraction, capped ? 0xFFC060FF : 0xFFE8C547);
		// MP
		int max = Math.max(1, JobStats.maxMana(player));
		String mp = data.mana() + "/" + max;
		bar(g, x + 6, y + 21, w - 16 - font.width(mp), 5, Math.min(1.0F, (float)data.mana() / max), 0xFF4A8CFF);
		g.text(font, mp, x + w - 4 - font.width(mp), y + 19, 0xFF9CC3FF, true);
		// wallet
		g.text(font, "◎ " + Coins.format(Wallet.balance(player)), x + 6, y + 30, 0xFFFFD27F, true);
	}

	/** Active trial under the panel: goals and items with counts, green when done. */
	private static void drawQuest(final GuiGraphicsExtractor g, final Font font, final LocalPlayer player) {
		QuestDef quest = QuestService.active(player);
		if (quest == null) {
			return;
		}
		QuestData data = QuestService.get(player);
		List<Component> labels = new ArrayList<>();
		List<int[]> counts = new ArrayList<>();
		for (int i = 0; i < quest.kills().size(); i++) {
			labels.add(Component.translatable(quest.goalKey(i)));
			counts.add(new int[] {data.progress(i), quest.kills().get(i).count()});
		}
		labels.add(Component.translatable(quest.token().getDescriptionId()));
		counts.add(new int[] {JobProgression.count(player.getInventory(), quest.token()), quest.tokenCount()});
		for (QuestDef.Material material : quest.materials()) {
			labels.add(Component.translatable(material.item().getDescriptionId()));
			counts.add(new int[] {Essence.held(player.getInventory(), material.item()), material.count()});
		}
		int x = 4;
		int y = 48;
		int w = 150;
		int h = 13 + labels.size() * 10;
		leftBottom = y + h;
		g.fill(x, y, x + w, y + h, 0x70000000);
		g.text(font, Component.translatable(quest.nameKey()).withColor(quest.job().color()), x + 4, y + 3, 0xFFFFFFFF, true);
		for (int i = 0; i < labels.size(); i++) {
			int[] c = counts.get(i);
			boolean done = c[0] >= c[1];
			int ly = y + 13 + i * 10;
			String count = Math.min(c[0], c[1]) + "/" + c[1];
			// long goal names (e.g. "Monsters (ranged kills)") must stop short of the counter
			String label = font.plainSubstrByWidth(labels.get(i).getString(), w - 12 - 6 - font.width(count));
			g.text(font, label, x + 8, ly, done ? 0xFF7CFC7C : 0xFFD0D0D0, false);
			g.text(font, count, x + w - 4 - font.width(count), ly, done ? 0xFF7CFC7C : 0xFFD0D0D0, false);
		}
	}

	public static int leftBottom() {
		return leftBottom;
	}

	public static int panelRight() {
		return panelRight;
	}

	private static void bar(final GuiGraphicsExtractor g, final int x, final int y, final int w, final int h, final float fraction, final int color) {
		g.fill(x, y, x + w, y + h, 0xFF1A1A1A);
		g.fill(x, y, x + Math.round(w * fraction), y + h, color);
		g.fill(x, y, x + Math.round(w * fraction), y + 1, 0x40FFFFFF);
	}

	private static void drawSkills(final GuiGraphicsExtractor g, final Minecraft minecraft, final LocalPlayer player, final JobData data) {
		ItemStack stack = player.getMainHandItem();
		WeaponDef def = JobWeapons.def(stack);
		if (def == null || minecraft.level == null) {
			return;
		}
		Font font = minecraft.font;
		boolean active = JobWeapons.isActive(data, def);
		EngraveTotals mods = JobWeapons.activeTotals(player);
		long now = minecraft.level.getGameTime();
		// right of the hotbar, but never past the screen edge (the GUI can be as narrow as 320 px)
		int x0 = Math.min(g.guiWidth() / 2 + 98, g.guiWidth() - def.skills().size() * 22 - 2);
		int y = g.guiHeight() - 22;
		for (int i = 0; i < def.skills().size(); i++) {
			Skill skill = def.skills().get(i);
			int x = x0 + i * 22;
			int kindColor = 0xFF000000 | skill.kind().color();
			g.fill(x, y, x + 20, y + 20, 0xC0101010);
			g.outline(x, y, 20, 20, active ? kindColor : 0xFF5A1E1E);
			String name = Component.translatable(skill.nameKey()).getString();
			String glyph = name.isEmpty() ? "?" : name.substring(0, name.offsetByCodePoints(0, 1));
			g.centeredText(font, glyph, x + 11, y + 3, active ? 0xFFFFFFFF : 0xFF777777);
			if (active) {
				long left = SkillCaster.readyAt(data, skill, i) - now;
				if (left > 0) {
					int total = Math.max(1, SkillCaster.cooldown(data, skill, mods));
					int h = Math.round(20 * Math.min(1.0F, (float)left / total));
					g.fill(x + 1, y + 20 - h, x + 19, y + 19, 0xA0000000);
					String secs = left >= 200 ? Long.toString((left + 19) / 20) : String.format(java.util.Locale.ROOT, "%.1f", left / 20.0);
					g.centeredText(font, secs, x + 11, y + 3, 0xFFFFD84A);
				} else if (data.mana() < SkillCaster.manaCost(skill, mods) && !player.isCreative()) {
					g.fill(x + 1, y + 1, x + 19, y + 19, 0x603D7BFF);
				}
			}
			String key = JobKeys.SKILLS[i].getTranslatedKeyMessage().getString();
			if (key.length() > 2) {
				key = key.substring(0, 2);
			}
			g.text(font, key, x + 2, y + 11, 0xFFC0C0C0, true);
		}
	}

	private JobHud() {
	}
}
