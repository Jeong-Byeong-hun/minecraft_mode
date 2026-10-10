package com.minecraftmode.client.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;

/**
 * Long item tooltips (26.3 neither wraps tooltip lines nor keeps a tall tooltip on screen): lines wider than half the screen
 * (at least {@link #MIN_WIDTH}) wrap with a small indent, and a tooltip taller than the screen starts at its top and scrolls with
 * the mouse wheel ({@code GuiGraphicsExtractorMixin} places it through {@link #place}). Registered after every other tooltip
 * callback, so it sees the finished lines.
 */
public final class TooltipLayout {
	private static final int MIN_WIDTH = 200;
	private static final int MARGIN = 4;
	private static final int SCROLL_STEP = 20;
	/** A tooltip not drawn for this long is gone: the next one starts at its top again. */
	private static final long FORGET_MS = 250L;
	private static final String INDENT = "  ";

	private static int offset;
	private static int lastWidth;
	private static int lastHeight;
	private static long lastTall;
	private static long lastShown;

	public static void init() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			Minecraft minecraft = Minecraft.getInstance();
			int max = Math.max(MIN_WIDTH, minecraft.getWindow().getGuiScaledWidth() / 2);
			Font font = minecraft.font;
			for (int i = 0; i < lines.size(); i++) {
				if (font.width(lines.get(i)) > max) {
					List<Component> wrapped = wrap(font, lines.get(i), max);
					lines.remove(i);
					lines.addAll(i, wrapped);
					i += wrapped.size() - 1;
				}
			}
		});
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> ScreenMouseEvents.allowMouseScroll(screen)
			.register((s, mouseX, mouseY, horizontal, vertical) -> !scroll(vertical)));
	}

	/** The pieces of {@code line} that fit {@code max} pixels, styles kept; every piece after the first is indented. */
	private static List<Component> wrap(final Font font, final Component line, final int max) {
		List<Component> out = new ArrayList<>();
		int indent = font.width(INDENT);
		List<FormattedText> parts = font.getSplitter().splitLines(line, max - indent, Style.EMPTY);
		for (int i = 0; i < parts.size(); i++) {
			MutableComponent piece = Component.literal(i == 0 ? "" : INDENT);
			parts.get(i).visit((style, text) -> {
				piece.append(Component.literal(text).withStyle(style));
				return Optional.empty();
			}, Style.EMPTY);
			out.add(piece);
		}
		return out;
	}

	/** How far a tall tooltip is scrolled (pixels), and whether one is on screen right now. */
	public static int offset() {
		return offset;
	}

	public static boolean tall() {
		return System.currentTimeMillis() - lastTall <= FORGET_MS;
	}

	/** Wheel turned over a screen: moves a tall tooltip shown right now. Returns true when it did (the screen then gets no scroll). */
	private static boolean scroll(final double vertical) {
		if (System.currentTimeMillis() - lastTall > FORGET_MS || vertical == 0.0) {
			return false;
		}
		offset += vertical < 0.0 ? SCROLL_STEP : -SCROLL_STEP;
		return true;
	}

	/**
	 * Called for every tooltip drawn ({@code y} as the positioner placed it): a tooltip taller than the screen starts at the top and
	 * is shifted up by the scroll offset; others are left alone. The offset resets when another tooltip appears.
	 */
	public static int place(final int screenHeight, final int y, final int width, final int height) {
		long now = System.currentTimeMillis();
		if (width != lastWidth || height != lastHeight || now - lastShown > FORGET_MS) {
			offset = 0;
		}
		lastWidth = width;
		lastHeight = height;
		lastShown = now;
		int overflow = height + 2 * MARGIN - screenHeight;
		if (overflow <= 0) {
			return y;
		}
		lastTall = now;
		offset = Mth.clamp(offset, 0, overflow);
		return MARGIN - offset;
	}

	/** A thin scroll bar in the right padding of a tooltip taller than the screen, so the wheel is worth a try. */
	public static void scrollBar(final GuiGraphicsExtractor g, final int x, final int width, final int height) {
		int screenHeight = g.guiHeight();
		int overflow = height + 2 * MARGIN - screenHeight;
		if (overflow <= 0) {
			return;
		}
		int track = screenHeight - 2 * MARGIN;
		int thumb = Math.max(8, track * track / height);
		int top = MARGIN + (track - thumb) * offset / overflow;
		int bx = x + width + 2;
		g.fill(bx, MARGIN, bx + 1, MARGIN + track, 0x40FFFFFF);
		g.fill(bx, top, bx + 1, top + thumb, 0xD0FFFFFF);
	}

	private TooltipLayout() {
	}
}
