package com.minecraftmode.client;

import com.minecraftmode.client.map.MinimapHud;
import com.minecraftmode.client.mixin.BossHealthOverlayAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import org.jspecify.annotations.Nullable;

/**
 * Keeps the boss bars (raid bosses, lords, titans, the dungeon header) clear of the top-left HUD panels
 * and the top-right minimap and effect icons. They stay centered while that is free; otherwise they slide right
 * past the panels, and drop below whatever is in the way when the gap beside it is too narrow.
 */
public final class BossBarLayout {
	private static final int GAP = 4;
	/** BossHealthOverlay: first name top, first bar top, bar height, row step, bar half width. */
	private static final int NAME_TOP = 3;
	private static final int FIRST_BAR = 12;
	private static final int BAR_HEIGHT = 5;
	private static final int ROW = 19;
	private static final int BAR_HALF = 91;
	/** Left-edge panels drawn this frame as {right, top, bottom}; they are drawn before the boss bars. */
	private static final List<int[]> LEFT = new ArrayList<>();
	/** Last frame's bars as {left, top, right, bottom} (null without bars) and effect rows. */
	private static int @Nullable [] drawn;
	private static List<int[]> drawnRight = List.of();

	public static void init() {
		HudElementRegistry.replaceElement(VanillaHudElements.BOSS_BAR, bars -> (graphics, delta) -> extract(bars, graphics, delta));
	}

	/** Forgets last frame's panels; called by the first left panel (the class HUD) every frame. */
	public static void newFrame() {
		LEFT.clear();
	}

	/** A panel on the left edge covering x below {@code right} from {@code top} to {@code bottom}. */
	public static void reserveLeft(final int right, final int top, final int bottom) {
		LEFT.add(new int[] {right, top, bottom});
	}

	private static void extract(final HudElement bars, final GuiGraphicsExtractor g, final DeltaTracker delta) {
		Minecraft minecraft = Minecraft.getInstance();
		Collection<LerpingBossEvent> events = ((BossHealthOverlayAccessor)minecraft.gui.hud.getBossOverlay()).minecraftMode$events().values();
		if (events.isEmpty()) {
			drawn = null;
			bars.extractRenderState(g, delta);
			return;
		}
		// measure the bars the overlay will draw: widest name and how many fit above a third of the screen
		int half = BAR_HALF;
		int rows = 0;
		for (LerpingBossEvent event : events) {
			half = Math.max(half, (minecraft.font.width(event.getName()) + 1) / 2);
			rows++;
			if (FIRST_BAR + rows * ROW >= g.guiHeight() / 3) {
				break;
			}
		}
		int height = FIRST_BAR + (rows - 1) * ROW + BAR_HEIGHT - NAME_TOP;
		List<int[]> right = effectRows(minecraft, g.guiWidth());
		int[] offset = place(g.guiWidth(), g.guiHeight(), half, height, right);
		int center = g.guiWidth() / 2 + offset[0];
		drawn = new int[] {center - half, NAME_TOP + offset[1], center + half, NAME_TOP + offset[1] + height};
		drawnRight = right;
		g.pose().pushMatrix();
		g.pose().translate(offset[0], offset[1]);
		bars.extractRenderState(g, delta);
		g.pose().popMatrix();
	}

	/**
	 * Offset {dx, dy} for bars {@code 2 * half} wide and {@code height} tall: the smallest drop (none, or
	 * just below one of the obstacles) at which they fit between the left panels and the effect icons,
	 * as close to the center as they fit there.
	 */
	private static int[] place(final int width, final int screenHeight, final int half, final int height, final List<int[]> right) {
		int center = width / 2;
		List<Integer> drops = new ArrayList<>();
		drops.add(0);
		for (int[] r : LEFT) {
			drops.add(r[2] + GAP - NAME_TOP);
		}
		for (int[] r : right) {
			drops.add(r[2] + GAP - NAME_TOP);
		}
		drops.sort(null);
		for (int dy : drops) {
			// never push the first bar below a third of the screen
			if (NAME_TOP + dy > screenHeight / 3) {
				break;
			}
			int top = NAME_TOP + dy;
			int lo = GAP;
			int hi = width - GAP;
			for (int[] r : LEFT) {
				if (r[1] < top + height && r[2] > top) {
					lo = Math.max(lo, r[0] + GAP);
				}
			}
			for (int[] r : right) {
				if (r[1] < top + height && r[2] > top) {
					hi = Math.min(hi, r[0] - GAP);
				}
			}
			if (hi - lo >= 2 * half) {
				return new int[] {Mth.clamp(center, lo + half, hi - half) - center, dy};
			}
		}
		// no room anywhere: stay on top and clear the left panels as far as the screen allows
		int lo = GAP;
		for (int[] r : LEFT) {
			if (r[1] < NAME_TOP + height) {
				lo = Math.max(lo, r[0] + GAP);
			}
		}
		return new int[] {Math.max(0, Math.min(lo + half, width - GAP - half) - center), 0};
	}

	/** For the game tests: what last frame's boss bars covered, or "" when they were clear. */
	public static String overlap() {
		if (drawn == null) {
			return "no boss bars on screen";
		}
		for (int[] r : LEFT) {
			if (r[0] > drawn[0] && r[1] < drawn[3] && r[2] > drawn[1]) {
				return "bars " + drawn[0] + ".." + drawn[2] + " x " + drawn[1] + ".." + drawn[3] + " cover a left panel up to x " + r[0] + ", y " + r[1] + ".." + r[2];
			}
		}
		for (int[] r : drawnRight) {
			if (r[0] < drawn[2] && r[1] < drawn[3] && r[2] > drawn[1]) {
				return "bars " + drawn[0] + ".." + drawn[2] + " x " + drawn[1] + ".." + drawn[3] + " cover effect icons from x " + r[0] + ", y " + r[1] + ".." + r[2];
			}
		}
		return "";
	}

	/** Bottom of last frame's boss bars (the target health bar goes below them), or -1 without bars. */
	public static int bottom() {
		return drawn == null ? -1 : drawn[3];
	}

	/** The minimap and the effect icon rows on the top right as {left, top, bottom}, effects laid out like {@code Hud.extractEffects}. */
	private static List<int[]> effectRows(final Minecraft minecraft, final int width) {
		List<int[]> rows = new ArrayList<>(3);
		int[] minimap = MinimapHud.box(minecraft, width);
		if (minimap != null) {
			rows.add(minimap);
		}
		LocalPlayer player = minecraft.player;
		Screen screen = minecraft.gui.screen();
		if (player == null || screen != null && screen.showsActiveEffects()) {
			return rows;
		}
		int beneficial = 0;
		int harmful = 0;
		for (MobEffectInstance instance : player.getActiveEffects()) {
			if (instance.showIcon()) {
				if (instance.getEffect().value().isBeneficial()) {
					beneficial++;
				} else {
					harmful++;
				}
			}
		}
		int top = minecraft.isDemo() ? 16 : 1;
		if (beneficial > 0) {
			rows.add(new int[] {width - 25 * beneficial, top, top + 24});
		}
		if (harmful > 0) {
			rows.add(new int[] {width - 25 * harmful, top + 26, top + 50});
		}
		return rows;
	}

	private BossBarLayout() {
	}
}
