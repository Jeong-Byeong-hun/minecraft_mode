package com.minecraftmode.client.endgame;

import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.market.AuctionService;
import com.minecraftmode.network.AuctionActionPayload;
import com.minecraftmode.network.AuctionStatePayload;
import com.minecraftmode.progress.ResetCycle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Broker Morgan's market (거래소): browse (search, category, sort, pages, buyout), sell (pick an inventory stack, set the price in
 * gold, silver and copper, see the fees), the player's own listings (cancel) and the mailbox (proceeds and returned items). The
 * server sends the whole market; it is filtered and paged here and refreshed every few seconds while open.
 */
public class AuctionScreen extends Screen {
	private static final int W = 320;
	private static final int H = 236;
	private static final int ROW = 20;
	private static final int ROWS = 8;
	private static final int LIST_Y = 46;
	private static final int GRID_X = 8;
	private static final int GRID_Y = 30;
	private static final int REFRESH = 100;

	enum Tab {
		BROWSE, SELL, MINE, MAIL
	}

	enum Category {
		ALL, GEAR, CONSUMABLE, MATERIAL;

		boolean test(final ItemStack stack) {
			boolean gear = ClassGear.of(stack) != null || stack.has(DataComponents.EQUIPPABLE) || stack.has(DataComponents.WEAPON) || stack.has(DataComponents.TOOL);
			boolean consumable = !gear && (stack.has(DataComponents.CONSUMABLE) || stack.has(DataComponents.FOOD));
			return switch (this) {
				case ALL -> true;
				case GEAR -> gear;
				case CONSUMABLE -> consumable;
				case MATERIAL -> !gear && !consumable;
			};
		}
	}

	enum Sort {
		CHEAP, DEAR, ENDING
	}

	private static Tab tab = Tab.BROWSE;
	private static Category category = Category.ALL;
	private static Sort sort = Sort.CHEAP;
	private static String search = "";
	private static int page;
	private static int sellSlot = -1;
	private static String gold = "";
	private static String silver = "";
	private static String copper = "";

	private final int broker;
	private @Nullable AuctionStatePayload shown;
	private int left;
	private int top;
	private int age;
	private int sinceState;
	private @Nullable EditBox searchBox;
	private @Nullable Button listButton;
	private boolean dirty;

	public AuctionScreen(final int broker) {
		super(Component.translatable("screen.minecraft_mode.market.title"));
		this.broker = broker;
	}

	/** Opens the market on tab {@code name} (browse, sell, mine, mail) next time (also used by tests). */
	public static void showTab(final String name) {
		tab = Tab.valueOf(name.toUpperCase(Locale.ROOT));
		page = 0;
	}

	private void send(final int action, final int id, final int slot, final int price) {
		if (ClientPlayNetworking.canSend(AuctionActionPayload.TYPE)) {
			ClientPlayNetworking.send(new AuctionActionPayload(this.broker, action, id, slot, price));
		}
	}

	private int wallet() {
		LocalPlayer player = this.minecraft.player;
		return player == null ? 0 : Coins.total(player);
	}

	private List<AuctionStatePayload.Entry> visible(final AuctionStatePayload state) {
		String query = search.trim().toLowerCase(Locale.ROOT);
		List<AuctionStatePayload.Entry> out = new ArrayList<>();
		for (AuctionStatePayload.Entry e : state.listings()) {
			if (tab == Tab.MINE ? e.mine() : category.test(e.item()) && (query.isEmpty() || e.item().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query))) {
				out.add(e);
			}
		}
		Comparator<AuctionStatePayload.Entry> order = switch (tab == Tab.MINE ? Sort.ENDING : sort) {
			case CHEAP -> Comparator.comparingInt(e -> e.price() / Math.max(1, e.item().getCount()));
			case DEAR -> Comparator.<AuctionStatePayload.Entry>comparingInt(e -> e.price() / Math.max(1, e.item().getCount())).reversed();
			case ENDING -> Comparator.comparingLong(AuctionStatePayload.Entry::ticksLeft);
		};
		out.sort(order);
		return out;
	}

	private int pages(final int count) {
		return Math.max(1, (count + ROWS - 1) / ROWS);
	}

	private static int parse(final String s) {
		try {
			return s.isEmpty() ? 0 : Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static int price() {
		long total = (long)parse(gold) * Coins.GOLD + (long)parse(silver) * Coins.SILVER + parse(copper);
		return (int)Math.min(Integer.MAX_VALUE, total);
	}

	private ItemStack sellStack() {
		LocalPlayer player = this.minecraft.player;
		return player == null || sellSlot < 0 || sellSlot >= Inventory.INVENTORY_SIZE ? ItemStack.EMPTY : player.getInventory().getItem(sellSlot);
	}

	@Override
	protected void init() {
		this.left = (this.width - W) / 2;
		this.top = (this.height - H) / 2;
		AuctionStatePayload state = EndgameClient.auction();
		this.shown = state;
		Tab[] tabs = Tab.values();
		for (int i = 0; i < tabs.length; i++) {
			Tab t = tabs[i];
			Component label = Component.translatable("screen.minecraft_mode.market.tab." + t.name().toLowerCase(Locale.ROOT));
			if (t == Tab.MAIL && state != null && (state.mailCoins() > 0 || !state.mailItems().isEmpty())) {
				label = label.copy().append(Component.literal(" ●").withStyle(ChatFormatting.GOLD));
			}
			Button b = Button.builder(label, button -> {
				tab = t;
				page = 0;
				this.rebuildWidgets();
			}).bounds(this.left + 70 + i * 61, this.top + 5, 59, 16).build();
			b.active = t != tab;
			this.addRenderableWidget(b);
		}
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(this.left + W - 58, this.top + H - 22, 50, 18).build());
		this.searchBox = null;
		this.listButton = null;
		if (state == null) {
			return;
		}
		switch (tab) {
			case BROWSE, MINE -> this.initList(state);
			case SELL -> this.initSell(state);
			case MAIL -> {
				Button claim = Button.builder(Component.translatable("screen.minecraft_mode.market.claim"), b -> this.send(AuctionActionPayload.CLAIM, 0, 0, 0))
					.bounds(this.left + 8, this.top + 150, 100, 20).build();
				claim.active = state.mailCoins() > 0 || !state.mailItems().isEmpty();
				this.addRenderableWidget(claim);
			}
		}
	}

	private void initList(final AuctionStatePayload state) {
		if (tab == Tab.BROWSE) {
			EditBox box = new EditBox(this.font, this.left + 8, this.top + 27, 126, 14, Component.translatable("screen.minecraft_mode.market.search"));
			box.setMaxLength(40);
			box.setValue(search);
			box.setHint(Component.translatable("screen.minecraft_mode.market.search"));
			box.setResponder(value -> {
				if (!value.equals(search)) {
					search = value;
					page = 0;
					this.dirty = true;
				}
			});
			this.searchBox = box;
			this.addRenderableWidget(box);
			this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.market.category." + category.name().toLowerCase(Locale.ROOT)), b -> {
				category = Category.values()[(category.ordinal() + 1) % Category.values().length];
				page = 0;
				this.rebuildWidgets();
			}).bounds(this.left + 138, this.top + 26, 80, 16).build());
			this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.market.sort." + sort.name().toLowerCase(Locale.ROOT)), b -> {
				sort = Sort.values()[(sort.ordinal() + 1) % Sort.values().length];
				this.rebuildWidgets();
			}).bounds(this.left + 222, this.top + 26, 90, 16).build());
		}
		List<AuctionStatePayload.Entry> list = this.visible(state);
		int pages = this.pages(list.size());
		page = Math.min(page, pages - 1);
		int wallet = this.wallet();
		for (int i = 0; i < ROWS; i++) {
			int index = page * ROWS + i;
			if (index >= list.size()) {
				break;
			}
			AuctionStatePayload.Entry e = list.get(index);
			int ry = this.top + LIST_Y + i * ROW;
			if (e.mine()) {
				this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.market.cancel"), b -> this.send(AuctionActionPayload.CANCEL, e.id(), 0, 0))
					.bounds(this.left + W - 54, ry + 1, 46, 16).build());
			} else {
				Button buy = Button.builder(Component.translatable("screen.minecraft_mode.market.buy"), b -> this.send(AuctionActionPayload.BUY, e.id(), 0, 0))
					.bounds(this.left + W - 54, ry + 1, 46, 16).build();
				buy.active = wallet >= e.price();
				this.addRenderableWidget(buy);
			}
		}
		Button prev = Button.builder(Component.literal("◀"), b -> {
			page--;
			this.rebuildWidgets();
		}).bounds(this.left + 8, this.top + H - 22, 20, 18).build();
		prev.active = page > 0;
		this.addRenderableWidget(prev);
		Button next = Button.builder(Component.literal("▶"), b -> {
			page++;
			this.rebuildWidgets();
		}).bounds(this.left + 74, this.top + H - 22, 20, 18).build();
		next.active = page < pages - 1;
		this.addRenderableWidget(next);
	}

	private EditBox coinBox(final int x, final String value, final java.util.function.Consumer<String> set) {
		EditBox box = new EditBox(this.font, x, this.top + 66, 30, 14, Component.empty());
		box.setMaxLength(5);
		box.setValue(value);
		box.setResponder(v -> {
			String digits = v.replaceAll("[^0-9]", "");
			if (!digits.equals(v)) {
				box.setValue(digits);
				return;
			}
			set.accept(digits);
		});
		this.addRenderableWidget(box);
		return box;
	}

	private void initSell(final AuctionStatePayload state) {
		int rx = this.left + 182;
		this.coinBox(rx, gold, v -> gold = v);
		this.coinBox(rx + 44, silver, v -> silver = v);
		this.coinBox(rx + 88, copper, v -> copper = v);
		Button list = Button.builder(Component.translatable("screen.minecraft_mode.market.list"), b -> {
			this.send(AuctionActionPayload.LIST, 0, sellSlot, price());
			sellSlot = -1;
		}).bounds(rx, this.top + 150, 130, 20).build();
		list.active = this.canList(state);
		this.listButton = list;
		this.addRenderableWidget(list);
	}

	private boolean canList(final AuctionStatePayload state) {
		int price = price();
		return AuctionService.sellable(this.sellStack()) && price >= 1 && price <= AuctionService.MAX_PRICE && state.myListings() < AuctionService.MAX_LISTINGS
			&& this.wallet() >= AuctionService.listingFee(price);
	}

	@Override
	public void tick() {
		this.age++;
		this.sinceState++;
		if (EndgameClient.auction() != this.shown) {
			this.sinceState = 0;
			this.rebuild();
		} else if (this.dirty || tab != Tab.SELL && this.age % 20 == 0) {
			this.rebuild();
		}
		if (this.age % REFRESH == 0) {
			this.send(AuctionActionPayload.REFRESH, 0, 0, 0);
		}
		if (this.listButton != null && this.shown != null) {
			this.listButton.active = this.canList(this.shown);
		}
	}

	/** Rebuilds the widgets and gives the focus back to the text box that had it (typing goes on). */
	private void rebuild() {
		this.dirty = false;
		int focused = -1;
		List<EditBox> before = this.boxes();
		for (int i = 0; i < before.size(); i++) {
			if (before.get(i).isFocused()) {
				focused = i;
			}
		}
		this.rebuildWidgets();
		List<EditBox> after = this.boxes();
		if (focused >= 0 && focused < after.size()) {
			this.setFocused(after.get(focused));
		}
	}

	private List<EditBox> boxes() {
		return this.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList();
	}

	@Override
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		if (tab == Tab.SELL && event.button() == 0) {
			int slot = this.gridSlot(event.x(), event.y());
			if (slot >= 0) {
				sellSlot = slot;
				ItemStack stack = this.sellStack();
				if (!stack.isEmpty() && price() == 0) {
					this.suggest(stack);
				}
				this.rebuildWidgets();
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	/** Puts the cheapest listed price for the same item (per piece, times the count) into the price boxes. */
	private void suggest(final ItemStack stack) {
		AuctionStatePayload state = this.shown;
		if (state == null) {
			return;
		}
		int best = -1;
		for (AuctionStatePayload.Entry e : state.listings()) {
			if (ItemStack.isSameItemSameComponents(e.item(), stack)) {
				int each = e.price() / Math.max(1, e.item().getCount());
				best = best < 0 ? each : Math.min(best, each);
			}
		}
		if (best > 0) {
			int total = best * stack.getCount();
			gold = total / Coins.GOLD > 0 ? Integer.toString(total / Coins.GOLD) : "";
			silver = total % Coins.GOLD / Coins.SILVER > 0 ? Integer.toString(total % Coins.GOLD / Coins.SILVER) : "";
			copper = total % Coins.SILVER > 0 ? Integer.toString(total % Coins.SILVER) : "";
		}
	}

	/** Inventory slot under the mouse in the sell grid (main rows, then the hotbar), or -1. */
	private int gridSlot(final double mx, final double my) {
		int gx = this.left + GRID_X;
		int gy = this.top + GRID_Y;
		if (mx < gx || mx >= gx + 9 * 18) {
			return -1;
		}
		int col = (int)((mx - gx) / 18);
		if (my >= gy && my < gy + 3 * 18) {
			return 9 + (int)((my - gy) / 18) * 9 + col;
		}
		if (my >= gy + 58 && my < gy + 76) {
			return col;
		}
		return -1;
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
		g.outline(x, y, W, H, 0xFFB04AA0);
		g.text(this.font, this.title.copy().withStyle(ChatFormatting.GOLD), x + 10, y + 9, 0xFFFFFFFF, true);
		String wallet = "◎ " + Coins.format(this.wallet());
		g.text(this.font, wallet, x + W - 64 - this.font.width(wallet), y + H - 17, 0xFFFFD27F, false);
		AuctionStatePayload state = this.shown;
		List<Component> tip = null;
		if (state == null) {
			g.text(this.font, Component.translatable("screen.minecraft_mode.market.loading"), x + 10, y + 30, 0xFF8A8A8A, false);
		} else {
			tip = switch (tab) {
				case BROWSE, MINE -> this.drawList(g, state, mouseX, mouseY);
				case SELL -> this.drawSell(g, state, mouseX, mouseY);
				case MAIL -> this.drawMail(g, state, mouseX, mouseY);
			};
		}
		super.extractRenderState(g, mouseX, mouseY, a);
		if (tip != null) {
			g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
		}
	}

	private @Nullable List<Component> drawList(final GuiGraphicsExtractor g, final AuctionStatePayload state, final int mouseX, final int mouseY) {
		int x = this.left;
		int y = this.top;
		List<AuctionStatePayload.Entry> list = this.visible(state);
		List<Component> tip = null;
		if (tab == Tab.MINE) {
			g.text(this.font, Component.translatable("screen.minecraft_mode.market.mine", state.myListings(), AuctionService.MAX_LISTINGS), x + 10, y + 30, 0xFFD0D0D0, false);
		}
		if (list.isEmpty()) {
			g.text(this.font, Component.translatable(tab == Tab.MINE ? "screen.minecraft_mode.market.no_mine" : "screen.minecraft_mode.market.empty"), x + 10, y + LIST_Y + 6,
				0xFF8A8A8A, false);
		}
		for (int i = 0; i < ROWS; i++) {
			int index = page * ROWS + i;
			if (index >= list.size()) {
				break;
			}
			AuctionStatePayload.Entry e = list.get(index);
			int ry = y + LIST_Y + i * ROW;
			g.fill(x + 8, ry, x + W - 8, ry + ROW - 2, e.mine() ? 0x403A6ED8 : 0x40000000);
			g.fakeItem(e.item(), x + 10, ry + 1);
			g.itemDecorations(this.font, e.item(), x + 10, ry + 1);
			g.text(this.font, this.font.plainSubstrByWidth(e.item().getHoverName().getString(), 112), x + 30, ry + 1, 0xFFFFFFFF, false);
			String sub = e.mine() ? Component.translatable("screen.minecraft_mode.market.left", ResetCycle.remaining(Math.max(0, e.ticksLeft() - this.sinceState))).getString()
				: e.seller();
			g.text(this.font, this.font.plainSubstrByWidth(sub, 112), x + 30, ry + 10, 0xFF8A8A8A, false);
			String price = "◎" + Coins.format(e.price());
			g.text(this.font, price, x + W - 60 - this.font.width(price), ry + 1, 0xFFFFD27F, false);
			if (e.item().getCount() > 1) {
				String each = Component.translatable("screen.minecraft_mode.market.each", Coins.format(e.price() / e.item().getCount())).getString();
				g.text(this.font, each, x + W - 60 - this.font.width(each), ry + 10, 0xFF9A8A5A, false);
			}
			if (mouseX >= x + 8 && mouseX < x + W - 56 && mouseY >= ry && mouseY < ry + ROW - 2) {
				List<Component> lines = new ArrayList<>(getTooltipFromItem(this.minecraft, e.item()));
				lines.add(Component.translatable("screen.minecraft_mode.market.seller", e.seller()).withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable("screen.minecraft_mode.market.left", ResetCycle.remaining(Math.max(0, e.ticksLeft() - this.sinceState)))
					.withStyle(ChatFormatting.DARK_GRAY));
				tip = lines;
			}
		}
		String pages = (page + 1) + "/" + this.pages(list.size());
		g.centeredText(this.font, pages, x + 51, y + H - 17, 0xFFBBBBBB);
		return tip;
	}

	private @Nullable List<Component> drawSell(final GuiGraphicsExtractor g, final AuctionStatePayload state, final int mouseX, final int mouseY) {
		int x = this.left;
		int y = this.top;
		LocalPlayer player = this.minecraft.player;
		List<Component> tip = null;
		Inventory inventory = player.getInventory();
		for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
			int col = i % 9;
			int sx = x + GRID_X + col * 18;
			int sy = i < 9 ? y + GRID_Y + 58 : y + GRID_Y + (i / 9 - 1) * 18;
			ItemStack stack = inventory.getItem(i);
			boolean ok = AuctionService.sellable(stack);
			g.fill(sx, sy, sx + 17, sy + 17, i == sellSlot ? 0xC0B04AA0 : 0x60000000);
			g.fakeItem(stack, sx, sy);
			g.itemDecorations(this.font, stack, sx, sy);
			if (!stack.isEmpty() && !ok) {
				g.fill(sx, sy, sx + 17, sy + 17, 0x90101018);
			}
			if (mouseX >= sx && mouseX < sx + 17 && mouseY >= sy && mouseY < sy + 17 && !stack.isEmpty()) {
				tip = getTooltipFromItem(this.minecraft, stack);
			}
		}
		g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.market.sell_help"), x + GRID_X, y + GRID_Y + 82, 162, 0xFF8A8A8A);

		int rx = x + 182;
		ItemStack stack = this.sellStack();
		if (stack.isEmpty()) {
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.market.pick"), rx, y + 30, 130, 0xFFBBBBBB);
		} else {
			g.fakeItem(stack, rx, y + 28);
			g.itemDecorations(this.font, stack, rx, y + 28);
			g.text(this.font, this.font.plainSubstrByWidth(stack.getHoverName().getString(), 110), rx + 20, y + 32, 0xFFFFFFFF, false);
		}
		g.text(this.font, Component.translatable("screen.minecraft_mode.market.price"), rx, y + 54, 0xFFD0D0D0, false);
		g.text(this.font, "G", rx + 32, y + 69, 0xFFFFD27F, false);
		g.text(this.font, "S", rx + 76, y + 69, 0xFFD8D8E0, false);
		g.text(this.font, "C", rx + 120, y + 69, 0xFFD89A5A, false);
		int price = price();
		int ty = y + 88;
		g.text(this.font, Component.translatable("screen.minecraft_mode.market.total", Coins.format(price)), rx, ty, 0xFFFFD27F, false);
		ty += 11;
		g.text(this.font, Component.translatable("screen.minecraft_mode.market.fee", AuctionService.LIST_FEE_PERCENT, Coins.format(price > 0 ? AuctionService.listingFee(price) : 0)),
			rx, ty, 0xFFBBBBBB, false);
		ty += 11;
		g.text(this.font, Component.translatable("screen.minecraft_mode.market.net", AuctionService.SALE_FEE_PERCENT, Coins.format(AuctionService.proceeds(price))), rx, ty,
			0xFF7CFC7C, false);
		ty += 11;
		g.text(this.font, Component.translatable("screen.minecraft_mode.market.duration", ResetCycle.remaining(AuctionService.DURATION)), rx, ty, 0xFF8A8A8A, false);
		ty += 11;
		g.text(this.font, Component.translatable("screen.minecraft_mode.market.mine", state.myListings(), AuctionService.MAX_LISTINGS), rx, ty,
			state.myListings() < AuctionService.MAX_LISTINGS ? 0xFF8A8A8A : 0xFFFF6B6B, false);
		return tip;
	}

	private @Nullable List<Component> drawMail(final GuiGraphicsExtractor g, final AuctionStatePayload state, final int mouseX, final int mouseY) {
		int x = this.left;
		int y = this.top;
		List<Component> tip = null;
		g.text(this.font, Component.translatable("screen.minecraft_mode.market.mail_coins", Coins.format(state.mailCoins())), x + 10, y + 30, 0xFFFFD27F, false);
		g.text(this.font, Component.translatable("screen.minecraft_mode.market.mail_items", state.mailItems().size()), x + 10, y + 44, 0xFFD0D0D0, false);
		for (int i = 0; i < state.mailItems().size() && i < 45; i++) {
			ItemStack stack = state.mailItems().get(i);
			int sx = x + 10 + i % 15 * 18;
			int sy = y + 56 + i / 15 * 18;
			g.fill(sx, sy, sx + 17, sy + 17, 0x60000000);
			g.fakeItem(stack, sx, sy);
			g.itemDecorations(this.font, stack, sx, sy);
			if (mouseX >= sx && mouseX < sx + 17 && mouseY >= sy && mouseY < sy + 17) {
				tip = getTooltipFromItem(this.minecraft, stack);
			}
		}
		g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.market.mail_help"), x + 10, y + 176, W - 20, 0xFF8A8A8A);
		return tip;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
