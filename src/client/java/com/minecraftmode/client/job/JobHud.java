package com.minecraftmode.client.job;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.JobStats;
import com.minecraftmode.job.engrave.EngraveTotals;
import com.minecraftmode.job.skill.Skill;
import com.minecraftmode.job.skill.SkillCaster;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.WeaponDef;
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
		drawPanel(graphics, minecraft.font, player, data);
		drawSkills(graphics, minecraft, player, data);
	}

	private static void drawPanel(final GuiGraphicsExtractor g, final Font font, final LocalPlayer player, final JobData data) {
		int x = 4;
		int y = 4;
		Component title = Component.translatable(data.job().tierKey(data.tier())).withColor(data.job().color());
		String level = "Lv " + data.level();
		int w = Math.max(124, font.width(title) + font.width(level) + 18);
		int color = 0xFF000000 | data.job().color();
		g.fill(x, y, x + w, y + 30, 0x90000000);
		g.fill(x, y, x + 2, y + 30, color);
		g.text(font, title, x + 6, y + 3, 0xFFFFFFFF, true);
		g.text(font, level, x + w - 4 - font.width(level), y + 3, 0xFFFFE08A, true);
		// experience
		int need = JobProgression.expToNext(data.level());
		float expFraction = data.level() >= JobProgression.MAX_LEVEL ? 1.0F : Math.min(1.0F, (float)data.exp() / need);
		bar(g, x + 6, y + 14, w - 10, 3, expFraction, 0xFFE8C547);
		// MP
		int max = Math.max(1, JobStats.maxMana(player));
		String mp = data.mana() + "/" + max;
		bar(g, x + 6, y + 21, w - 16 - font.width(mp), 5, Math.min(1.0F, (float)data.mana() / max), 0xFF4A8CFF);
		g.text(font, mp, x + w - 4 - font.width(mp), y + 19, 0xFF9CC3FF, true);
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
		EngraveTotals mods = JobWeapons.engravings(stack).totals();
		long now = minecraft.level.getGameTime();
		int x0 = g.guiWidth() / 2 + 98;
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
				long left = data.readyAt(skill.id()) - now;
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
