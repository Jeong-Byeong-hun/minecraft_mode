package com.minecraftmode.client.menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Shared frame of the adventurer's menu and the admin screen: a panel with a row of tabs, and helpers that let a tab lay itself out
 * in one pass over {@link #init}. Labels are kept with the widgets and drawn after the panel, text boxes keep what was typed in
 * {@link #values} across {@link #rebuildWidgets}, and {@link #confirm} buttons need a second click.
 */
abstract class MenuScreenBase extends Screen {
	static final int ROW = 16;
	private static final int TAB_W = 70;

	protected final int w;
	protected final int h;
	protected int left;
	protected int top;
	protected int tab;
	private final List<Label> labels = new ArrayList<>();
	private final Map<String, String> values = new HashMap<>();
	private @Nullable String armed;

	private record Label(int x, int y, Component text, int color) {
	}

	protected MenuScreenBase(final Component title, final int w, final int h) {
		super(title);
		this.w = w;
		this.h = h;
	}

	/** Tab names, left to right. */
	protected abstract List<Component> tabs();

	/** Lays out tab {@link #tab}: its widgets and labels. */
	protected abstract void initTab(int x, int y);

	@Override
	protected final void init() {
		this.labels.clear();
		this.left = (this.width - this.w) / 2;
		this.top = Math.max(2, (this.height - this.h) / 2);
		List<Component> tabs = this.tabs();
		for (int i = 0; i < tabs.size(); i++) {
			int index = i;
			Button b = this.button(this.left + 6 + i * (TAB_W + 2), this.top + 20, TAB_W, tabs.get(i), () -> {
				this.tab = index;
				this.armed = null;
				this.rebuildWidgets();
			});
			b.active = i != this.tab;
		}
		this.button(this.left + this.w - 52, this.top + 4, 46, Component.translatable("gui.done"), this::onClose);
		this.initTab(this.left + 10, this.top + 44);
	}

	/** Switches to tab {@code index} (game tests take a screenshot of each). */
	public void showTab(final int index) {
		this.tab = index;
		this.armed = null;
		this.rebuildWidgets();
	}

	// ------------------------------------------------------------------ layout helpers

	protected Button button(final int x, final int y, final int width, final Component text, final Runnable action) {
		return this.addRenderableWidget(Button.builder(text, b -> action.run()).bounds(x, y, width, ROW - 2).build());
	}

	protected Button button(final int x, final int y, final int width, final Component text, final @Nullable Component tooltip, final Runnable action) {
		Button b = this.button(x, y, width, text, action);
		if (tooltip != null) {
			b.setTooltip(Tooltip.create(tooltip));
		}
		return b;
	}

	/** Disarms a "Sure?" button, e.g. when the player it acts on changes (its key does not name the player). */
	protected void disarm() {
		this.armed = null;
	}

	/** A button that asks "Sure?" on the first click and runs {@code action} on the second; {@code key} tells the buttons apart. */
	protected Button confirm(final int x, final int y, final int width, final String key, final Component text, final Runnable action) {
		boolean isArmed = key.equals(this.armed);
		Component shown = isArmed ? Component.translatable("screen.minecraft_mode.menu.sure").withStyle(ChatFormatting.RED) : text;
		return this.button(x, y, width, shown, () -> {
			if (key.equals(this.armed)) {
				this.armed = null;
				action.run();
			} else {
				this.armed = key;
				this.rebuildWidgets();
			}
		});
	}

	/** A text box whose text survives rebuilds under {@code key}; {@code filter} limits what can be typed. */
	protected EditBox box(final int x, final int y, final int width, final String key, final Component hint, final int max, final Predicate<String> filter) {
		EditBox box = new EditBox(this.font, x, y, width, ROW - 2, hint);
		box.setMaxLength(max);
		box.setHint(hint.copy().withStyle(ChatFormatting.DARK_GRAY));
		box.setValue(this.values.getOrDefault(key, ""));
		// 26.3 boxes have no input filter: a change the filter refuses is put back to the last good text
		box.setResponder(text -> {
			if (filter.test(text)) {
				this.values.put(key, text);
			} else {
				box.setValue(this.values.getOrDefault(key, ""));
			}
		});
		return this.addRenderableWidget(box);
	}

	protected EditBox numberBox(final int x, final int y, final int width, final String key, final Component hint) {
		return this.box(x, y, width, key, hint, 9, s -> s.matches("-?\\d*"));
	}

	protected String value(final String key) {
		return this.values.getOrDefault(key, "").strip();
	}

	protected void setValue(final String key, final String text) {
		this.values.put(key, text);
	}

	/** The number typed under {@code key}, or {@code fallback} when the box is empty or holds no number. */
	protected int number(final String key, final int fallback) {
		try {
			return Integer.parseInt(this.value(key));
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	protected void label(final int x, final int y, final Component text) {
		this.label(x, y, text, 0xFFE0E0E0);
	}

	protected void label(final int x, final int y, final Component text, final int color) {
		this.labels.add(new Label(x, y, text, color));
	}

	/** A section heading. */
	protected void heading(final int x, final int y, final Component text) {
		this.label(x, y, text.copy().withStyle(ChatFormatting.YELLOW));
	}

	/** Pages through {@code count} rows of {@code size}: ◀ page/pages ▶ at {@code x, y}; {@code go} gets the new page. */
	protected void pager(final int x, final int y, final int page, final int pages, final java.util.function.IntConsumer go) {
		this.button(x, y, 16, Component.literal("◀"), () -> go.accept(page - 1)).active = page > 0;
		this.label(x + 22, y + 3, Component.literal((page + 1) + "/" + Math.max(1, pages)));
		this.button(x + 50, y, 16, Component.literal("▶"), () -> go.accept(page + 1)).active = page + 1 < pages;
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor g, final int mouseX, final int mouseY, final float a) {
		g.fill(this.left, this.top, this.left + this.w, this.top + this.h, 0xE8101418);
		g.outline(this.left, this.top, this.w, this.h, this.frameColor());
		g.text(this.font, this.title.copy().withStyle(ChatFormatting.GOLD), this.left + 10, this.top + 7, 0xFFFFFFFF, true);
		g.fill(this.left + 6, this.top + 38, this.left + this.w - 6, this.top + 39, 0x60FFFFFF);
		this.extractExtra(g, mouseX, mouseY);
		for (Label label : this.labels) {
			g.text(this.font, label.text(), label.x(), label.y(), label.color(), false);
		}
		super.extractRenderState(g, mouseX, mouseY, a);
	}

	/** Anything a tab draws besides labels (item icons, highlights), under the widgets. */
	protected void extractExtra(final GuiGraphicsExtractor g, final int mouseX, final int mouseY) {
	}

	protected int frameColor() {
		return 0xFF5A80B0;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
