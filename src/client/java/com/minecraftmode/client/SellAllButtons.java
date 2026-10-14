package com.minecraftmode.client;

import com.minecraftmode.client.mixin.MerchantScreenAccessor;
import com.minecraftmode.economy.SellAll;
import com.minecraftmode.economy.ShopType;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.network.SellAllPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Two buttons under the buyback panel beside a shop's trade window ({@link SellAll}): "sell all of this" sells every whole trade of the
 * line picked on the left, "sell everything" (clicked twice within a few seconds) every line the shop buys, essence excepted. Hovering
 * lists what would go and roughly for how much (the market pressure behind each price is estimated, so the server's total can differ a
 * little).
 */
public final class SellAllButtons {
	private static final String K = "screen.minecraft_mode.sell_all.";
	private static final int WIDTH = 84;
	private static final int HEIGHT = 20;
	private static final int GAP = 2;
	private static final long CONFIRM_MILLIS = 3000L;
	/** Lines listed in a tooltip before "and N more". */
	private static final int LISTED = 10;
	private static final int TOOLTIP_WIDTH = 220;

	/** How often the preview counts the goods again (it reads every bag once per sell line). */
	private static final long RECOUNT_MILLIS = 250L;

	/** One shop screen's buttons and what they would sell, counted at most every {@link #RECOUNT_MILLIS}. */
	private static final class State {
		long armedUntil;
		long countedAt;
		int countedPick = -1;
		boolean sale;
		List<SellAll.Line> one = List.of();
		List<SellAll.Line> all = List.of();
	}

	public static void init() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!BuybackPanel.isShop(screen) || !(screen instanceof MerchantScreen merchant)) {
				return;
			}
			State state = new State();
			Button one = Button.builder(Component.translatable(K + "one"),
				b -> ClientPlayNetworking.send(new SellAllPayload(((MerchantScreenAccessor) merchant).minecraftMode$shopItem())))
				.bounds(0, 0, WIDTH, HEIGHT).build();
			Button all = Button.builder(Component.translatable(K + "all"), b -> {
				long now = Util.getMillis();
				if (now < state.armedUntil) {
					state.armedUntil = 0L;
					ClientPlayNetworking.send(new SellAllPayload(SellAll.ALL));
				} else {
					state.armedUntil = now + CONFIRM_MILLIS;
				}
			}).bounds(0, 0, WIDTH, HEIGHT).build();
			Screens.getWidgets(screen).add(one);
			Screens.getWidgets(screen).add(all);
			update(merchant, state, one, all);
			ScreenEvents.beforeExtract(screen).register((s, g, mouseX, mouseY, delta) -> update(merchant, state, one, all));
			// inside the screen's own pass (afterExtract runs after this frame's tooltip is drawn, so one set there never shows)
			ScreenEvents.afterForeground(screen).register((s, g, mouseX, mouseY, delta) -> {
				Font font = Minecraft.getInstance().font;
				if (one.visible && one.isHovered()) {
					Component empty = Component.translatable(state.sale ? K + "nothing" : K + "pick");
					g.setTooltipForNextFrame(font, tooltip(font, Component.translatable(K + "one.title"), state.one, empty, false), mouseX, mouseY);
				} else if (all.visible && all.isHovered()) {
					g.setTooltipForNextFrame(font, tooltip(font, Component.translatable(K + "all.title"), state.all, Component.translatable(K + "nothing"), true),
						mouseX, mouseY);
				}
			});
		});
	}

	/** Places the buttons on the buyback panel's side (hidden when there is no room) and works out what each would sell now. */
	private static void update(final MerchantScreen screen, final State state, final Button one, final Button all) {
		int panel = BuybackPanel.left(screen);
		boolean right = panel >= screen.width / 2;
		int room = right ? screen.width - panel - GAP : screen.width / 2 - BuybackPanel.HALF_WIDTH - 4 - GAP;
		int width = Math.min(WIDTH, room);
		boolean shown = panel >= 0 && width >= 40;
		one.visible = shown;
		all.visible = shown;
		if (!shown) {
			return;
		}
		int x = right ? panel : screen.width / 2 - BuybackPanel.HALF_WIDTH - 4 - width;
		int y = BuybackPanel.bottom(screen) + 4;
		one.setRectangle(width, HEIGHT, x, y);
		all.setRectangle(width, HEIGHT, x, y + HEIGHT + GAP);

		int picked = ((MerchantScreenAccessor) screen).minecraftMode$shopItem();
		long now = Util.getMillis();
		if (picked != state.countedPick || now - state.countedAt >= RECOUNT_MILLIS) {
			state.countedPick = picked;
			state.countedAt = now;
			count(screen, state, picked);
		}
		one.active = !state.one.isEmpty();
		all.active = !state.all.isEmpty();
		if (now >= state.armedUntil) {
			state.armedUntil = 0L;
		}
		all.setMessage(state.armedUntil > 0L && all.active
			? Component.translatable(K + "confirm").withStyle(ChatFormatting.RED) : Component.translatable(K + "all"));
	}

	/** What each button would sell now: the picked line, and every line "sell everything" takes. */
	private static void count(final MerchantScreen screen, final State state, final int picked) {
		Minecraft minecraft = Minecraft.getInstance();
		MerchantMenu menu = screen.getMenu();
		MerchantOffers offers = menu.getOffers();
		boolean fixed = fixedPrices(screen);
		state.sale = picked >= 0 && picked < offers.size() && SellAll.isSale(offers.get(picked));
		state.one = List.of();
		List<SellAll.Line> bulk = new ArrayList<>();
		if (minecraft.player != null) {
			for (int i = 0; i < offers.size(); i++) {
				MerchantOffer offer = offers.get(i);
				boolean chosen = i == picked && state.sale;
				if (!chosen && !SellAll.inBulk(offer)) {
					continue;
				}
				SellAll.Line line = SellAll.estimate(offer, SellAll.stock(minecraft.player, menu, offer), fixed);
				if (line.trades() > 0) {
					if (chosen) {
						state.one = List.of(line);
					}
					if (SellAll.inBulk(offer)) {
						bulk.add(line);
					}
				}
			}
		}
		state.all = bulk;
	}

	/** Whether the open shop keeps its prices ({@link ShopType#fixedPrices}), read from the screen's title. */
	private static boolean fixedPrices(final MerchantScreen screen) {
		if (screen.getTitle().getContents() instanceof TranslatableContents title) {
			for (ShopType type : ShopType.values()) {
				if (type.titleKey().equals(title.getKey())) {
					return type.fixedPrices();
				}
			}
		}
		return false;
	}

	private static List<FormattedCharSequence> tooltip(final Font font, final Component title, final List<SellAll.Line> lines, final Component empty,
		final boolean bulk) {
		List<Component> out = new ArrayList<>();
		out.add(title.copy().withStyle(ChatFormatting.GOLD));
		if (lines.isEmpty()) {
			out.add(empty.copy().withStyle(ChatFormatting.GRAY));
		} else {
			int count = 0;
			int copper = 0;
			for (int i = 0; i < lines.size(); i++) {
				SellAll.Line line = lines.get(i);
				count += line.count();
				copper += line.copper();
				if (i < LISTED) {
					out.add(Component.translatable(K + "line", new ItemStack(line.item()).getHoverName(), line.count(), Coins.format(line.copper()))
						.withStyle(ChatFormatting.WHITE));
				}
			}
			if (lines.size() > LISTED) {
				out.add(Component.translatable(K + "more", lines.size() - LISTED).withStyle(ChatFormatting.GRAY));
			}
			if (lines.size() > 1) {
				out.add(Component.translatable(K + "total", count, Coins.format(copper)).withStyle(ChatFormatting.YELLOW));
			}
		}
		out.add(Component.translatable(K + "rules").withStyle(ChatFormatting.DARK_GRAY));
		if (bulk) {
			out.add(Component.translatable(K + "no_essence").withStyle(ChatFormatting.DARK_GRAY));
		}
		List<FormattedCharSequence> split = new ArrayList<>();
		for (Component line : out) {
			split.addAll(font.split(line, TOOLTIP_WIDTH));
		}
		return split;
	}

	private SellAllButtons() {
	}
}
