package com.minecraftmode.client;

import com.minecraftmode.loot.Coins;
import com.minecraftmode.network.LootSellPayload;
import com.minecraftmode.network.OpenLootBuyerPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * The loot buyer's counter: every class gear piece and raid accessory the player could sell (inventory, then bags) in a grid that
 * scrolls; a click picks or drops a piece, "select all" picks everything but the hotbar, enhanced and engraved pieces, "other
 * classes" picks gear of other classes only. Selling takes two clicks and shows the total; picked enhanced or engraved pieces are
 * called out. The server answers a sale with a fresh list ({@link #update}).
 */
public class LootBuyerScreen extends Screen {
	private static final int W = 320;
	private static final int H = 220;
	static final int COLS = 16;
	static final int ROWS = 6;
	private static final int CELL = 18;
	private static final int GRID_X = 10;
	private static final int GRID_Y = 24;
	private static final int BAR_X = GRID_X + COLS * CELL + 4;

	private OpenLootBuyerPayload list;
	private final TreeSet<Integer> picked = new TreeSet<>();
	private int left;
	private int top;
	/** First grid row shown. */
	private int scroll;
	private boolean sellArmed;
	private boolean dragging;

	public LootBuyerScreen(final OpenLootBuyerPayload list) {
		super(Component.translatable("screen.minecraft_mode.loot_buyer.title"));
		this.list = list;
	}

	public int entityId() {
		return this.list.entityId();
	}

	/** A fresh list after a sale: the picks are gone (indices changed), the scroll stays where it can. */
	public void update(final OpenLootBuyerPayload list) {
		this.list = list;
		this.picked.clear();
		this.sellArmed = false;
		this.rebuildWidgets();
	}

	/** For tests and the buttons: pick entries by index. */
	public void pick(final int index) {
		if (index >= 0 && index < this.list.entries().size() && !this.picked.remove(index)) {
			this.picked.add(index);
		}
		this.sellArmed = false;
		this.rebuildWidgets();
	}

	public List<Integer> picked() {
		return List.copyOf(this.picked);
	}

	public List<OpenLootBuyerPayload.Entry> entries() {
		return this.list.entries();
	}

	private int rows() {
		return (this.list.entries().size() + COLS - 1) / COLS;
	}

	private int maxScroll() {
		return Math.max(0, this.rows() - ROWS);
	}

	private int total() {
		int total = 0;
		for (int i : this.picked) {
			total += this.list.entries().get(i).price();
		}
		return total;
	}

	private int valuablePicked() {
		int n = 0;
		for (int i : this.picked) {
			OpenLootBuyerPayload.Entry entry = this.list.entries().get(i);
			if (entry.kept()) {
				n++;
			}
		}
		return n;
	}

	@Override
	protected void init() {
		this.left = (this.width - W) / 2;
		this.top = (this.height - H) / 2;
		this.scroll = Math.max(0, Math.min(this.maxScroll(), this.scroll));
		int by = this.top + H - 50;
		this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.loot_buyer.all"), b -> this.select(false))
			.bounds(this.left + 10, by, 96, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.loot_buyer.others"), b -> this.select(true))
			.bounds(this.left + 112, by, 96, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.loot_buyer.clear"), b -> {
			this.picked.clear();
			this.sellArmed = false;
			this.rebuildWidgets();
		}).bounds(this.left + 214, by, 96, 20).build());
		Button sell = Button.builder(this.sellArmed
			? Component.translatable("screen.minecraft_mode.loot_buyer.confirm", Coins.format(this.total())).withStyle(ChatFormatting.RED)
			: Component.translatable("screen.minecraft_mode.loot_buyer.sell", this.picked.size()), b -> {
				if (this.sellArmed) {
					ClientPlayNetworking.send(new LootSellPayload(this.list.entityId(), new ArrayList<>(this.picked)));
					this.sellArmed = false;
				} else {
					this.sellArmed = true;
				}
				this.rebuildWidgets();
			}).bounds(this.left + 10, this.top + H - 26, 198, 20).build();
		sell.active = !this.picked.isEmpty();
		this.addRenderableWidget(sell);
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(this.left + 214, this.top + H - 26, 96, 20).build());
	}

	/** Picks every piece "select all" may take, or only other classes' gear; a second press of the same filter drops them. */
	public void select(final boolean othersOnly) {
		List<Integer> matching = new ArrayList<>();
		for (int i = 0; i < this.list.entries().size(); i++) {
			OpenLootBuyerPayload.Entry entry = this.list.entries().get(i);
			if (!entry.kept() && (!othersOnly || !entry.mine())) {
				matching.add(i);
			}
		}
		if (!matching.isEmpty() && this.picked.containsAll(matching) && this.picked.size() == matching.size()) {
			this.picked.clear();
		} else {
			this.picked.clear();
			this.picked.addAll(matching);
		}
		this.sellArmed = false;
		this.rebuildWidgets();
	}

	private int cellAt(final double mx, final double my) {
		int gx = (int)Math.floor((mx - this.left - GRID_X) / CELL);
		int gy = (int)Math.floor((my - this.top - GRID_Y) / CELL);
		if (mx < this.left + GRID_X || my < this.top + GRID_Y || gx >= COLS || gy >= ROWS) {
			return -1;
		}
		int index = (this.scroll + gy) * COLS + gx;
		return index < this.list.entries().size() ? index : -1;
	}

	@Override
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		int cell = this.cellAt(event.x(), event.y());
		if (cell >= 0) {
			this.pick(cell);
			return true;
		}
		if (this.maxScroll() > 0 && event.x() >= this.left + BAR_X - 1 && event.x() < this.left + BAR_X + 5 && event.y() >= this.top + GRID_Y
			&& event.y() < this.top + GRID_Y + ROWS * CELL) {
			this.dragging = true;
			this.scrollTo(event.y());
			return true;
		}
		if (this.sellArmed) {
			// the second click on the sell button sells (and disarms); any other click only disarms it
			boolean handled = super.mouseClicked(event, doubleClick);
			if (this.sellArmed) {
				this.sellArmed = false;
				this.rebuildWidgets();
			}
			return handled;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(final MouseButtonEvent event, final double dx, final double dy) {
		if (this.dragging) {
			this.scrollTo(event.y());
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(final MouseButtonEvent event) {
		this.dragging = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
		if (this.maxScroll() > 0 && scrollY != 0) {
			this.scroll = Math.max(0, Math.min(this.maxScroll(), this.scroll - (int)Math.signum(scrollY)));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private void scrollTo(final double mouseY) {
		double f = (mouseY - this.top - GRID_Y) / (ROWS * CELL);
		this.scroll = Math.max(0, Math.min(this.maxScroll(), (int)Math.round(f * this.maxScroll())));
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor g, final int mouseX, final int mouseY, final float a) {
		int x = this.left;
		int y = this.top;
		g.fill(x, y, x + W, y + H, 0xE8101814);
		g.outline(x, y, W, H, 0xFF3FA060);
		g.text(this.font, this.title.copy().withStyle(ChatFormatting.GREEN), x + 10, y + 9, 0xFFFFFFFF, true);
		String help = Component.translatable("screen.minecraft_mode.loot_buyer.help").getString();
		g.text(this.font, this.font.plainSubstrByWidth(help, W - 20 - this.font.width(this.title) - 8), x + 18 + this.font.width(this.title), y + 9, 0xFF8A8A8A, false);

		List<OpenLootBuyerPayload.Entry> entries = this.list.entries();
		OpenLootBuyerPayload.Entry hovered = null;
		int hoveredIndex = -1;
		for (int row = 0; row < ROWS; row++) {
			for (int col = 0; col < COLS; col++) {
				int cx = x + GRID_X + col * CELL;
				int cy = y + GRID_Y + row * CELL;
				int index = (this.scroll + row) * COLS + col;
				boolean has = index < entries.size();
				boolean on = has && this.picked.contains(index);
				boolean other = has && !entries.get(index).mine();
				g.fill(cx, cy, cx + CELL - 1, cy + CELL - 1, on ? 0xC0E8C547 : other ? 0x903A5A8A : 0x50000000);
				if (!has) {
					continue;
				}
				OpenLootBuyerPayload.Entry entry = entries.get(index);
				g.fakeItem(entry.stack(), cx + 1, cy + 1);
				g.itemDecorations(this.font, entry.stack(), cx + 1, cy + 1);
				if (entry.kept()) {
					g.fill(cx + CELL - 4, cy, cx + CELL - 1, cy + 3, 0xFFE05050);
				}
				if (mouseX >= cx && mouseX < cx + CELL - 1 && mouseY >= cy && mouseY < cy + CELL - 1) {
					hovered = entry;
					hoveredIndex = index;
				}
			}
		}
		if (this.maxScroll() > 0) {
			int trackTop = y + GRID_Y;
			int trackH = ROWS * CELL - 1;
			int thumbH = Math.max(12, trackH * ROWS / this.rows());
			int thumbY = trackTop + (trackH - thumbH) * this.scroll / this.maxScroll();
			g.fill(x + BAR_X, trackTop, x + BAR_X + 4, trackTop + trackH, 0x60000000);
			g.fill(x + BAR_X, thumbY, x + BAR_X + 4, thumbY + thumbH, 0xFF3FA060);
		}
		int ty = y + GRID_Y + ROWS * CELL + 6;
		if (entries.isEmpty()) {
			g.text(this.font, Component.translatable("screen.minecraft_mode.loot_buyer.empty"), x + 10, ty, 0xFFAAAAAA, false);
		} else {
			g.text(this.font, Component.translatable("screen.minecraft_mode.loot_buyer.total", this.picked.size(), entries.size(), Coins.format(this.total())),
				x + 10, ty, 0xFFFFD27F, false);
			int risky = this.valuablePicked();
			Component note = risky > 0 ? Component.translatable("screen.minecraft_mode.loot_buyer.warning", risky).withStyle(ChatFormatting.RED)
				: Component.translatable("screen.minecraft_mode.loot_buyer.legend").withStyle(ChatFormatting.DARK_GRAY);
			g.text(this.font, this.font.substrByWidth(note, W - 20).getString(), x + 10, ty + 11, risky > 0 ? 0xFFFF7070 : 0xFF7A7A7A, false);
		}
		super.extractRenderState(g, mouseX, mouseY, a);
		if (hovered != null && this.minecraft != null) {
			List<Component> tip = new ArrayList<>(getTooltipFromItem(this.minecraft, hovered.stack()));
			tip.add(Component.empty());
			tip.add(Component.translatable("screen.minecraft_mode.loot_buyer.price", Coins.format(hovered.price())).withStyle(ChatFormatting.GOLD));
			if (hovered.kept()) {
				tip.add(Component.translatable("screen.minecraft_mode.loot_buyer.kept").withStyle(ChatFormatting.RED));
			}
			tip.add(Component.translatable(this.picked.contains(hoveredIndex) ? "screen.minecraft_mode.loot_buyer.picked" : "screen.minecraft_mode.loot_buyer.click")
				.withStyle(ChatFormatting.GRAY));
			g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
