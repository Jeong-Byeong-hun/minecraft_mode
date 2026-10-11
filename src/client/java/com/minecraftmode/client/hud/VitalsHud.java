package com.minecraftmode.client.hud;

import com.minecraftmode.client.ClientSettings;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.JobStats;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffects;

/**
 * HP and MP over the left half of the hotbar, where vanilla draws its hearts and armor row: class health went past 80 and the hearts
 * took four rows. The HP bar shows the numbers, absorption as a gold strip and "+N", the health just lost as a pale trail that drains
 * after a moment, and its colour turns for poison, wither and freezing; the frame flashes red below 30%. The MP bar sits on top, with the
 * armor points beside it. Drawn in vanilla's own places (only when the player can be hurt), so food, air and mount health stay put.
 * {@code /hpbar false} brings the hearts back ({@link ClientSettings#hpBar}).
 */
public final class VitalsHud {
	private static final Identifier ARMOR_SPRITE = Identifier.withDefaultNamespace("hud/armor_full");
	/** As wide as ten hearts. */
	public static final int WIDTH = 81;
	public static final int ROW = 9;
	/** The lost-health trail waits this long, then drains at {@link #TRAIL_SPEED} of max health per second. */
	private static final long TRAIL_HOLD = 450L;
	private static final float TRAIL_SPEED = 0.8F;

	private static float trail = -1.0F;
	private static float lastHealth = -1.0F;
	private static long trailStart;
	private static long lastMillis;

	public static void init() {
		HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR, hearts -> (g, delta) -> {
			if (ClientSettings.hpBar) {
				extract(g, delta);
			} else {
				hearts.extractRenderState(g, delta);
			}
		});
		HudElementRegistry.replaceElement(VanillaHudElements.ARMOR_BAR, armor -> (g, delta) -> {
			if (!ClientSettings.hpBar) {
				armor.extractRenderState(g, delta);
			}
		});
	}

	private static void extract(final GuiGraphicsExtractor g, final DeltaTracker delta) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null) {
			return;
		}
		int x = g.guiWidth() / 2 - 91;
		int y = g.guiHeight() - 39;
		health(g, minecraft.font, player, x, y);
		manaAndArmor(g, minecraft.font, player, x, y - ROW - 1);
	}

	private static void health(final GuiGraphicsExtractor g, final Font font, final LocalPlayer player, final int x, final int y) {
		float max = Math.max(1.0F, player.getMaxHealth());
		float health = Mth.clamp(player.getHealth(), 0.0F, max);
		float absorption = player.getAbsorptionAmount();
		long now = Util.getMillis();
		updateTrail(health, max, now);

		int inner = WIDTH - 2;
		int filled = Math.round(inner * health / max);
		int trailed = Math.round(inner * Math.max(health, trail) / max);
		boolean low = health < max * 0.3F;
		int frame = low && now / 250L % 2L == 0L ? 0xFFE03030 : 0xFF000000;
		g.fill(x, y, x + WIDTH, y + ROW, 0xC0101010);
		g.outline(x, y, WIDTH, ROW, frame);
		if (trailed > filled) {
			g.fill(x + 1 + filled, y + 1, x + 1 + trailed, y + ROW - 1, 0xFFF2E6C8);
		}
		g.fill(x + 1, y + 1, x + 1 + filled, y + ROW - 1, healthColor(player));
		g.fill(x + 1, y + 1, x + 1 + filled, y + 2, 0x40FFFFFF);
		if (absorption > 0.0F) {
			int gold = Math.round(inner * Math.min(1.0F, absorption / max));
			g.fill(x + 1, y + ROW - 3, x + 1 + gold, y + ROW - 1, 0xFFFFD34A);
		}
		Component text = Component.literal(Mth.ceil(health) + "/" + Math.round(max));
		if (absorption > 0.0F) {
			text = text.copy().append(Component.literal(" +" + Mth.ceil(absorption)).withColor(0xFFD34A));
		}
		g.centeredText(font, text, x + WIDTH / 2, y + 1, 0xFFFFFFFF);
	}

	/** The pale part of the bar: what the last hits took, held for a moment, then drained. */
	private static void updateTrail(final float health, final float max, final long now) {
		float seconds = lastMillis == 0L ? 0.0F : Math.min(0.25F, (now - lastMillis) / 1000.0F);
		lastMillis = now;
		if (trail < 0.0F || health >= trail) {
			trail = health;
		} else {
			if (health < lastHealth) {
				trailStart = now + TRAIL_HOLD;
			}
			if (now > trailStart) {
				trail = Math.max(health, trail - max * TRAIL_SPEED * seconds);
			}
		}
		lastHealth = health;
	}

	private static int healthColor(final LocalPlayer player) {
		if (player.hasEffect(MobEffects.WITHER)) {
			return 0xFF4A4A4A;
		}
		if (player.hasEffect(MobEffects.POISON)) {
			return 0xFF8DB43A;
		}
		if (player.isFullyFrozen()) {
			return 0xFF8FD3FF;
		}
		return 0xFFD8393A;
	}

	private static void manaAndArmor(final GuiGraphicsExtractor g, final Font font, final LocalPlayer player, final int x, final int y) {
		int armor = player.getArmorValue();
		String points = Integer.toString(armor);
		int armorWidth = armor > 0 ? 9 + 1 + font.width(points) + 2 : 0;
		int maxMana = JobStats.maxMana(player);
		if (maxMana > 0) {
			int w = WIDTH - armorWidth;
			int mana = Math.min(JobProgression.get(player).mana(), maxMana);
			g.fill(x, y, x + w, y + ROW, 0xC0101010);
			g.outline(x, y, w, ROW, 0xFF000000);
			int filled = Math.round((w - 2) * (float)mana / maxMana);
			g.fill(x + 1, y + 1, x + 1 + filled, y + ROW - 1, 0xFF3D7BFF);
			g.fill(x + 1, y + 1, x + 1 + filled, y + 2, 0x40FFFFFF);
			g.centeredText(font, mana + "/" + maxMana, x + w / 2, y + 1, 0xFFDCE8FF);
		}
		if (armor > 0) {
			int ax = x + WIDTH - armorWidth + 2;
			g.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_SPRITE, ax, y, 9, 9);
			g.text(font, points, ax + 10, y + 1, 0xFFE0E0E0, true);
		}
	}

	private VitalsHud() {
	}
}
