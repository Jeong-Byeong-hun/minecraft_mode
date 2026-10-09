package com.minecraftmode.market;

import com.minecraftmode.economy.Wallet;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.network.AuctionStatePayload;
import com.minecraftmode.progress.Progress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The rules of the market (거래소): listing costs a 1% fee up front, a sale pays the seller the price minus 5% into their
 * mailbox, a listing runs for three in-game days and then goes back to the mailbox. Players collect their mailbox at a broker.
 * Times use the overworld game time (it never runs backwards, unlike the day clock).
 */
public final class AuctionService {
	public static final int MAX_LISTINGS = 10;
	public static final long DURATION = 72000L;
	public static final int LIST_FEE_PERCENT = 1;
	public static final int SALE_FEE_PERCENT = 5;
	public static final int MAX_PRICE = 1000 * Coins.GOLD;
	private static final int EXPIRE_INTERVAL = 200;

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % EXPIRE_INTERVAL == 0) {
				AuctionHouse.get(server).expire(now(server));
			}
		});
	}

	public static long now(final MinecraftServer server) {
		return server.overworld().getGameTime();
	}

	public static int listingFee(final int price) {
		return Math.max(1, price * LIST_FEE_PERCENT / 100);
	}

	public static int proceeds(final int price) {
		return price - price * SALE_FEE_PERCENT / 100;
	}

	/** Whether {@code stack} may be put up for sale (anything but coins). */
	public static boolean sellable(final ItemStack stack) {
		return !stack.isEmpty() && Wallet.value(stack.getItem()) == 0;
	}

	/** Puts the stack in inventory {@code slot} up for {@code price} copper. */
	public static boolean list(final ServerPlayer player, final int slot, final int price) {
		Inventory inventory = player.getInventory();
		if (slot < 0 || slot >= Inventory.INVENTORY_SIZE) {
			return false;
		}
		ItemStack stack = inventory.getItem(slot);
		if (!sellable(stack)) {
			return fail(player, "message.minecraft_mode.market.not_sellable");
		}
		if (price < 1 || price > MAX_PRICE) {
			return fail(player, "message.minecraft_mode.market.bad_price");
		}
		AuctionHouse house = AuctionHouse.get(player.level().getServer());
		if (house.countBy(player.getUUID()) >= MAX_LISTINGS) {
			player.sendOverlayMessage(Component.translatable("message.minecraft_mode.market.too_many", MAX_LISTINGS).withStyle(ChatFormatting.RED));
			return false;
		}
		int fee = listingFee(price);
		if (!Wallet.take(player, fee)) {
			player.sendOverlayMessage(Component.translatable("message.minecraft_mode.market.no_coins", Coins.component(fee)).withStyle(ChatFormatting.RED));
			return false;
		}
		ItemStack goods = stack.copy();
		inventory.setItem(slot, ItemStack.EMPTY);
		house.add(player.getUUID(), player.getPlainTextName(), goods, price, now(player.level().getServer()) + DURATION);
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.market.listed", goods.getHoverName(), goods.getCount(), Coins.component(price),
			Coins.component(fee)).withStyle(ChatFormatting.GREEN));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 1.0F);
		return true;
	}

	/** Buys listing {@code id} outright. */
	public static boolean buy(final ServerPlayer player, final int id) {
		MinecraftServer server = player.level().getServer();
		AuctionHouse house = AuctionHouse.get(server);
		AuctionHouse.Listing listing = house.listing(id);
		if (listing == null || listing.expires() <= now(server)) {
			return fail(player, "message.minecraft_mode.market.gone");
		}
		if (listing.seller().equals(player.getUUID())) {
			return fail(player, "message.minecraft_mode.market.own");
		}
		if (!Wallet.take(player, listing.price())) {
			player.sendOverlayMessage(Component.translatable("message.minecraft_mode.market.no_coins", Coins.component(listing.price())).withStyle(ChatFormatting.RED));
			return false;
		}
		house.remove(listing);
		house.sendCoins(listing.seller(), proceeds(listing.price()), true);
		player.getInventory().placeItemBackInInventory(listing.item().copy(), Prediction.SERVER_ONLY);
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.market.bought", listing.item().getHoverName(), listing.item().getCount(),
			Coins.component(listing.price())).withStyle(ChatFormatting.GREEN));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_TRADE, SoundSource.PLAYERS, 0.8F, 1.0F);
		ServerPlayer seller = server.getPlayerList().getPlayer(listing.seller());
		if (seller != null) {
			seller.sendSystemMessage(Component.translatable("message.minecraft_mode.market.sold", listing.item().getHoverName(), listing.item().getCount(),
				Coins.component(proceeds(listing.price()))).withStyle(ChatFormatting.GOLD));
		}
		return true;
	}

	/** Takes listing {@code id} off the market and gives the item back to its seller. */
	public static boolean cancel(final ServerPlayer player, final int id) {
		AuctionHouse house = AuctionHouse.get(player.level().getServer());
		AuctionHouse.Listing listing = house.listing(id);
		if (listing == null || !listing.seller().equals(player.getUUID())) {
			return fail(player, "message.minecraft_mode.market.gone");
		}
		house.remove(listing);
		player.getInventory().placeItemBackInInventory(listing.item().copy(), Prediction.SERVER_ONLY);
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.market.cancelled", listing.item().getHoverName()).withStyle(ChatFormatting.YELLOW));
		return true;
	}

	/** Empties the mailbox: proceeds into the wallet, returned items into the inventory; sales count toward the records. */
	public static boolean claim(final ServerPlayer player) {
		AuctionHouse.Mail mail = AuctionHouse.get(player.level().getServer()).takeMail(player.getUUID());
		if (mail.isEmpty()) {
			return fail(player, "message.minecraft_mode.market.no_mail");
		}
		Wallet.add(player, mail.coins());
		for (ItemStack stack : mail.items()) {
			player.getInventory().placeItemBackInInventory(stack.copy(), Prediction.SERVER_ONLY);
		}
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.market.claimed", Coins.component(mail.coins()), mail.items().size())
			.withStyle(ChatFormatting.GREEN));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.0F);
		if (mail.sales() > 0) {
			Progress.marketSold(player, mail.sales());
		}
		return true;
	}

	/** The market as {@code player} sees it at broker {@code entityId}, cheapest first. */
	public static AuctionStatePayload state(final ServerPlayer player, final int entityId) {
		MinecraftServer server = player.level().getServer();
		AuctionHouse house = AuctionHouse.get(server);
		long now = now(server);
		List<AuctionStatePayload.Entry> entries = new ArrayList<>();
		for (AuctionHouse.Listing l : house.listings()) {
			if (l.expires() > now) {
				entries.add(new AuctionStatePayload.Entry(l.id(), l.item(), l.price(), l.sellerName(), l.seller().equals(player.getUUID()), l.expires() - now));
			}
		}
		entries.sort(Comparator.comparingInt(AuctionStatePayload.Entry::price));
		AuctionHouse.Mail mail = house.mail(player.getUUID());
		return new AuctionStatePayload(entityId, entries, mail.coins(), mail.items(), house.countBy(player.getUUID()));
	}

	public static void send(final ServerPlayer player, final int entityId) {
		if (ServerPlayNetworking.canSend(player, AuctionStatePayload.TYPE)) {
			ServerPlayNetworking.send(player, state(player, entityId));
		}
	}

	private static boolean fail(final ServerPlayer player, final String key) {
		player.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
		return false;
	}

	private AuctionService() {
	}
}
