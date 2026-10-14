package com.minecraftmode.economy;

import com.minecraftmode.bag.Bags;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.mixin.MerchantMenuAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * "Sell all" beside a shop's trade window: sells every whole trade's worth of one sell line's goods, or of every line the shop buys
 * ({@link #ALL}), in one click instead of one click per trade. The goods come from the payment slots, the main inventory and the
 * carried bags; the hotbar is left alone (what is held there is meant for use), and only plain items go (stackable, no name,
 * enchantment, damage or binding), as with a shift-click from the bags. Each trade still counts as one: market pressure grows trade
 * by trade (later trades take more goods), the coins go to the wallet and the sales to the {@link Buyback} list.
 */
public final class SellAll {
	/** {@link #sell}'s line index for every line the shop buys. */
	public static final int ALL = -1;
	/** Safety stop per line (a full inventory and three full bags of one-item trades is about 7,000). */
	private static final int MAX_TRADES = 10_000;
	private static final int PAYMENT_SLOTS = 2;

	/** One sell line as sold (or as {@link #estimate} expects it to sell): the goods given, the trades made and the copper paid. */
	public record Line(Item item, int count, int trades, int copper) {
	}

	/** A line where the shop pays coins for one kind of stackable goods (no second cost). */
	public static boolean isSale(final MerchantOffer offer) {
		ItemStack goods = offer.getCostA();
		return offer.getItemCostB().isEmpty() && !goods.isEmpty() && goods.getMaxStackSize() > 1
			&& Wallet.value(goods.getItem()) <= 0 && Wallet.value(offer.getResult().getItem()) > 0;
	}

	/** The lines "sell everything" takes: every sale except essence, which players keep for trials, engraving and recipes. */
	public static boolean inBulk(final MerchantOffer offer) {
		return isSale(offer) && !Essence.is(offer.getCostA().getItem());
	}

	/** The plain goods {@code offer} takes (empty when its cost asks for more than the bare item). */
	private static ItemStack plain(final MerchantOffer offer) {
		ItemStack plain = new ItemStack(offer.getItemCostA().item());
		return offer.getItemCostA().test(plain) ? plain : ItemStack.EMPTY;
	}

	/** How many plain goods for {@code offer} the player can sell: payment slots, main inventory (not the hotbar) and bags. */
	public static int stock(final Player player, final AbstractContainerMenu menu, final MerchantOffer offer) {
		ItemStack plain = plain(offer);
		if (plain.isEmpty()) {
			return 0;
		}
		int total = Bags.countInBags(player, plain);
		for (int slot = 0; slot < PAYMENT_SLOTS; slot++) {
			ItemStack stack = menu.getSlot(slot).getItem();
			if (ItemStack.isSameItemSameComponents(stack, plain)) {
				total += stack.getCount();
			}
		}
		Inventory inventory = player.getInventory();
		for (int i = Inventory.SELECTION_SIZE; i < Inventory.INVENTORY_SIZE; i++) {
			ItemStack stack = inventory.getItem(i);
			if (ItemStack.isSameItemSameComponents(stack, plain)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	private static int coinsPerTrade(final MerchantOffer offer) {
		return Wallet.value(offer.getResult().getItem()) * offer.getResult().getCount();
	}

	/**
	 * What selling {@code have} goods to {@code offer} brings, for the client's preview: it only sees the current cost, so the market
	 * pressure behind it is guessed from that and grown trade by trade as {@link MarketData} would.
	 */
	public static Line estimate(final MerchantOffer offer, final int have, final boolean fixedPrices) {
		int base = offer.getItemCostA().count();
		int max = offer.getCostA().getMaxStackSize();
		int now = offer.getCostA().getCount();
		int pressure = 0;
		while (!fixedPrices && pressure < MarketData.MAX_PRESSURE && cost(base, pressure, max) < now) {
			pressure++;
		}
		int goods = 0;
		int trades = 0;
		while (trades < MAX_TRADES) {
			int need = fixedPrices ? now : cost(base, pressure, max);
			if (have - goods < need) {
				break;
			}
			goods += need;
			trades++;
			pressure = Math.min(MarketData.MAX_PRESSURE, pressure + 1);
		}
		return new Line(offer.getCostA().getItem(), goods, trades, trades * coinsPerTrade(offer));
	}

	/** A sale's cost under {@code pressure}, clamped to a stack as {@link MerchantOffer#getCostA} does. */
	private static int cost(final int base, final int pressure, final int max) {
		return Math.clamp(base + MarketData.priceIncrease(base, pressure), 1, max);
	}

	/** Sells line {@code index} (or {@link #ALL}) of the shop screen {@code player} has open. */
	public static void sell(final ServerPlayer player, final int index) {
		if (!(player.containerMenu instanceof MerchantMenu menu) || !(((MerchantMenuAccessor) menu).minecraftMode$trader() instanceof ShopMerchant shop)
			|| !shop.stillValid(player)) {
			return;
		}
		MerchantOffers offers = shop.getOffers();
		if (index != ALL && (index < 0 || index >= offers.size() || !isSale(offers.get(index)))) {
			return;
		}
		List<Line> lines = new ArrayList<>();
		List<Buyback.Entry> sales = new ArrayList<>();
		for (int i = 0; i < offers.size(); i++) {
			MerchantOffer offer = offers.get(i);
			if (index == ALL ? inBulk(offer) : i == index) {
				Line line = sellLine(player, menu, shop, offer, sales);
				if (line.trades() > 0) {
					lines.add(line);
				}
			}
		}
		if (lines.isEmpty()) {
			player.sendSystemMessage(Component.translatable("message.minecraft_mode.sell_all.none").withStyle(ChatFormatting.GRAY));
			return;
		}
		int count = 0;
		int copper = 0;
		for (Line line : lines) {
			count += line.count();
			copper += line.copper();
		}
		Wallet.add(player, copper);
		Buyback.recordAll(player, sales);
		shop.sendOffers(player);
		shop.playTradeSound();
		Component done = lines.size() == 1
			? Component.translatable("message.minecraft_mode.sell_all.one", new ItemStack(lines.getFirst().item()).getHoverName(), count, Coins.format(copper))
			: Component.translatable("message.minecraft_mode.sell_all.many", lines.size(), count, Coins.format(copper));
		player.sendSystemMessage(done.copy().withStyle(ChatFormatting.GOLD));
	}

	/** Every whole trade of {@code offer} the player's goods make, one at a time so the price moves as it would with clicks. */
	private static Line sellLine(final ServerPlayer player, final MerchantMenu menu, final ShopMerchant shop, final MerchantOffer offer,
		final List<Buyback.Entry> sales) {
		ItemStack plain = plain(offer);
		int have = stock(player, menu, offer);
		int each = coinsPerTrade(offer);
		int goods = 0;
		int trades = 0;
		while (trades < MAX_TRADES) {
			int need = offer.getCostA().getCount();
			if (have - goods < need) {
				break;
			}
			goods += need;
			trades++;
			sales.add(new Buyback.Entry(plain.copyWithCount(need), each));
			shop.traded(offer);
		}
		if (goods > 0) {
			take(player, menu, plain, goods);
		}
		return new Line(offer.getCostA().getItem(), goods, trades, trades * each);
	}

	/** Takes {@code amount} plain goods from the payment slots, then the main inventory, then the bags (as {@link #stock} counted them). */
	private static void take(final Player player, final AbstractContainerMenu menu, final ItemStack plain, final int amount) {
		int left = amount;
		for (int slot = 0; slot < PAYMENT_SLOTS && left > 0; slot++) {
			Slot payment = menu.getSlot(slot);
			ItemStack stack = payment.getItem();
			if (ItemStack.isSameItemSameComponents(stack, plain)) {
				int taken = Math.min(left, stack.getCount());
				stack.shrink(taken);
				left -= taken;
				// through the container, so the result slot is worked out again
				payment.set(stack.isEmpty() ? ItemStack.EMPTY : stack);
			}
		}
		Inventory inventory = player.getInventory();
		for (int i = Inventory.SELECTION_SIZE; i < Inventory.INVENTORY_SIZE && left > 0; i++) {
			ItemStack stack = inventory.getItem(i);
			if (ItemStack.isSameItemSameComponents(stack, plain)) {
				int taken = Math.min(left, stack.getCount());
				stack.shrink(taken);
				left -= taken;
				if (stack.isEmpty()) {
					inventory.setItem(i, ItemStack.EMPTY);
				}
			}
		}
		inventory.setChanged();
		if (left > 0) {
			Bags.takeFromBags(player, plain, left);
		}
	}

	private SellAll() {
	}
}
