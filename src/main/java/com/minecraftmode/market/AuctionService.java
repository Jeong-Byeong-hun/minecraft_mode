package com.minecraftmode.market;

import com.minecraftmode.economy.Wallet;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.network.AuctionActionPayload;
import com.minecraftmode.network.AuctionStatePayload;
import com.minecraftmode.progress.Progress;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * The rules of the market (거래소): listing costs a 1% fee up front, a sale pays the seller the price minus 5% into their
 * mailbox, a listing runs for three in-game days and then goes back to the mailbox. Players collect their mailbox at a broker.
 * Times use the overworld game time (it never runs backwards, unlike the day clock).
 *
 * <p>Searching, filtering, sorting and paging happen here, so a viewer only ever receives one page ({@link #PAGE_SIZE}
 * listings) however big the market grows. Stacks that hold other items (shulker boxes, bundles) and stacks too large to send
 * cheaply ({@link #MAX_ITEM_BYTES}) cannot be listed.
 */
public final class AuctionService {
	public static final int MAX_LISTINGS = 10;
	public static final long DURATION = 72000L;
	public static final int LIST_FEE_PERCENT = 1;
	public static final int SALE_FEE_PERCENT = 5;
	public static final int MAX_PRICE = 1000 * Coins.GOLD;
	private static final int EXPIRE_INTERVAL = 200;
	public static final int PAGE_SIZE = 8;
	/** Mailbox items shown at once (collecting takes them all). */
	public static final int MAIL_SHOWN = 27;
	/** A listed stack may take at most this many bytes on the wire (written books and the like stay off the market). */
	public static final int MAX_ITEM_BYTES = 8192;

	public enum Category {
		ALL, GEAR, CONSUMABLE, MATERIAL;

		public boolean test(final ItemStack stack) {
			boolean gear = ClassGear.of(stack) != null || stack.has(DataComponents.EQUIPPABLE) || stack.has(DataComponents.WEAPON) || stack.has(DataComponents.TOOL);
			boolean consumable = !gear && (stack.has(DataComponents.CONSUMABLE) || stack.has(DataComponents.FOOD));
			return switch (this) {
				case ALL -> true;
				case GEAR -> gear;
				case CONSUMABLE -> consumable;
				case MATERIAL -> !gear && !consumable;
			};
		}

		public static Category byOrdinal(final int i) {
			return values()[Math.floorMod(i, values().length)];
		}
	}

	public enum Sort {
		CHEAP, DEAR, ENDING;

		public static Sort byOrdinal(final int i) {
			return values()[Math.floorMod(i, values().length)];
		}
	}

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

	/** Whether {@code stack} may be put up for sale: not coins, not a container with items inside. */
	public static boolean sellable(final ItemStack stack) {
		return !stack.isEmpty() && Wallet.value(stack.getItem()) == 0 && !hasContents(stack);
	}

	/** A shulker box (or any container item) or a bundle that holds items. */
	public static boolean hasContents(final ItemStack stack) {
		ItemContainerContents container = stack.get(DataComponents.CONTAINER);
		BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
		return container != null && container.nonEmptyItemCopyStream().findAny().isPresent() || bundle != null && !bundle.isEmpty();
	}

	/** Bytes {@code stack} takes on the wire. */
	public static int encodedSize(final ItemStack stack, final RegistryAccess registries) {
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
		try {
			ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
			return buf.readableBytes();
		} finally {
			buf.release();
		}
	}

	public static boolean tooLarge(final ItemStack stack, final RegistryAccess registries) {
		return encodedSize(stack, registries) > MAX_ITEM_BYTES;
	}

	/** Puts the stack in inventory {@code slot} up for {@code price} copper. */
	public static boolean list(final ServerPlayer player, final int slot, final int price) {
		Inventory inventory = player.getInventory();
		if (slot < 0 || slot >= Inventory.INVENTORY_SIZE) {
			return false;
		}
		ItemStack stack = inventory.getItem(slot);
		if (hasContents(stack)) {
			return fail(player, "message.minecraft_mode.market.has_contents");
		}
		if (!sellable(stack)) {
			return fail(player, "message.minecraft_mode.market.not_sellable");
		}
		if (tooLarge(stack, player.level().registryAccess())) {
			return fail(player, "message.minecraft_mode.market.too_large");
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
		if (player.getInventory().getFreeSlot() < 0) {
			return fail(player, "message.minecraft_mode.market.full");
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
		if (player.getInventory().getFreeSlot() < 0) {
			return fail(player, "message.minecraft_mode.market.full");
		}
		house.remove(listing);
		player.getInventory().placeItemBackInInventory(listing.item().copy(), Prediction.SERVER_ONLY);
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.market.cancelled", listing.item().getHoverName()).withStyle(ChatFormatting.YELLOW));
		return true;
	}

	/**
	 * Empties the mailbox: proceeds into the wallet, returned items into the inventory (as many as have a free slot; the rest
	 * stays in the mailbox instead of landing on the floor); sales count toward the records.
	 */
	public static boolean claim(final ServerPlayer player) {
		AuctionHouse house = AuctionHouse.get(player.level().getServer());
		AuctionHouse.Mail mail = house.takeMail(player.getUUID());
		if (mail.isEmpty()) {
			return fail(player, "message.minecraft_mode.market.no_mail");
		}
		Wallet.add(player, mail.coins());
		int given = 0;
		List<ItemStack> left = new ArrayList<>();
		for (ItemStack stack : mail.items()) {
			if (!left.isEmpty() || player.getInventory().getFreeSlot() < 0) {
				left.add(stack);
				continue;
			}
			player.getInventory().placeItemBackInInventory(stack.copy(), Prediction.SERVER_ONLY);
			given++;
		}
		for (ItemStack stack : left) {
			house.sendItem(player.getUUID(), stack);
		}
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.market.claimed", Coins.component(mail.coins()), given)
			.withStyle(ChatFormatting.GREEN));
		if (!left.isEmpty()) {
			player.sendSystemMessage(Component.translatable("message.minecraft_mode.market.mail_left", left.size()).withStyle(ChatFormatting.YELLOW));
		}
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.0F);
		if (mail.sales() > 0) {
			Progress.marketSold(player, mail.sales());
		}
		return true;
	}

	private static int each(final AuctionHouse.Listing l) {
		return l.price() / Math.max(1, l.item().getCount());
	}

	/** Whether listing {@code l} answers {@code query} for {@code viewer}. */
	public static boolean matches(final AuctionHouse.Listing l, final AuctionActionPayload.Query query, final Set<Integer> items, final UUID viewer) {
		if (query.mine()) {
			return l.seller().equals(viewer);
		}
		if (!Category.byOrdinal(query.category()).test(l.item())) {
			return false;
		}
		String search = query.search().trim().toLowerCase(Locale.ROOT);
		if (search.isEmpty()) {
			return true;
		}
		ItemStack stack = l.item();
		if (query.itemFilter() && items.contains(BuiltInRegistries.ITEM.getId(stack.getItem()))) {
			return true;
		}
		// what the server can read itself: the item id, a name given at an anvil, the server-language name
		Component custom = stack.get(DataComponents.CUSTOM_NAME);
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().contains(search)
			|| custom != null && custom.getString().toLowerCase(Locale.ROOT).contains(search)
			|| stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(search);
	}

	/** One page of the market as {@code player} asked for it at broker {@code entityId}. */
	public static AuctionStatePayload state(final ServerPlayer player, final int entityId, final AuctionActionPayload.Query query, final int suggestSlot,
		final int suggestEach) {
		MinecraftServer server = player.level().getServer();
		AuctionHouse house = AuctionHouse.get(server);
		long now = now(server);
		Set<Integer> items = new HashSet<>(query.items());
		List<AuctionHouse.Listing> found = new ArrayList<>();
		for (AuctionHouse.Listing l : house.listings()) {
			if (l.expires() > now && matches(l, query, items, player.getUUID())) {
				found.add(l);
			}
		}
		Comparator<AuctionHouse.Listing> order = switch (query.mine() ? Sort.ENDING : Sort.byOrdinal(query.sort())) {
			case CHEAP -> Comparator.comparingInt(AuctionService::each);
			case DEAR -> Comparator.comparingInt(AuctionService::each).reversed();
			case ENDING -> Comparator.comparingLong(AuctionHouse.Listing::expires);
		};
		found.sort(order.thenComparingInt(AuctionHouse.Listing::id));
		int pages = Math.max(1, (found.size() + PAGE_SIZE - 1) / PAGE_SIZE);
		int page = Math.max(0, Math.min(query.page(), pages - 1));
		List<AuctionStatePayload.Entry> entries = new ArrayList<>();
		for (AuctionHouse.Listing l : found.subList(page * PAGE_SIZE, Math.min(found.size(), (page + 1) * PAGE_SIZE))) {
			entries.add(new AuctionStatePayload.Entry(l.id(), l.item(), l.price(), l.sellerName(), l.seller().equals(player.getUUID()), l.expires() - now));
		}
		AuctionHouse.Mail mail = house.mail(player.getUUID());
		AuctionStatePayload.Mail shown = new AuctionStatePayload.Mail(mail.coins(), List.copyOf(mail.items().subList(0, Math.min(MAIL_SHOWN, mail.items().size()))),
			mail.items().size());
		return new AuctionStatePayload(entityId, entries, found.size(), page, pages, shown, house.countBy(player.getUUID()), suggestSlot, suggestEach);
	}

	/** The cheapest price per piece of listings like the stack in inventory {@code slot} (-1 when there are none). */
	public static int cheapestEach(final ServerPlayer player, final int slot) {
		if (slot < 0 || slot >= Inventory.INVENTORY_SIZE) {
			return -1;
		}
		ItemStack stack = player.getInventory().getItem(slot);
		if (stack.isEmpty()) {
			return -1;
		}
		long now = now(player.level().getServer());
		int best = -1;
		for (AuctionHouse.Listing l : AuctionHouse.get(player.level().getServer()).listings()) {
			if (l.expires() > now && ItemStack.isSameItemSameComponents(l.item(), stack)) {
				best = best < 0 ? each(l) : Math.min(best, each(l));
			}
		}
		return best;
	}

	/** Opens (or refreshes) the market at its first page for {@code player}. */
	public static void send(final ServerPlayer player, final int entityId) {
		send(player, entityId, AuctionActionPayload.Query.DEFAULT, -1, -1);
	}

	public static void send(final ServerPlayer player, final int entityId, final AuctionActionPayload.Query query, final int suggestSlot, final int suggestEach) {
		if (ServerPlayNetworking.canSend(player, AuctionStatePayload.TYPE)) {
			ServerPlayNetworking.send(player, state(player, entityId, query, suggestSlot, suggestEach));
		}
	}

	private static boolean fail(final ServerPlayer player, final String key) {
		player.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
		return false;
	}

	private AuctionService() {
	}
}
