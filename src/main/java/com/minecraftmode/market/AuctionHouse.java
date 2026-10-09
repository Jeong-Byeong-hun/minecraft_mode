package com.minecraftmode.market;

import com.minecraftmode.MinecraftMode;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * The world's market: listings (item, buyout price in copper, seller, expiry in game ticks) and each
 * player's mailbox (proceeds, expired or cancelled items, and how many of their items sold).
 */
public class AuctionHouse extends SavedData {
	public record Listing(int id, UUID seller, String sellerName, ItemStack item, int price, long expires) {
		public static final Codec<Listing> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("id").forGetter(Listing::id),
			UUIDUtil.CODEC.fieldOf("seller").forGetter(Listing::seller),
			Codec.STRING.fieldOf("seller_name").forGetter(Listing::sellerName),
			ItemStack.CODEC.fieldOf("item").forGetter(Listing::item),
			Codec.INT.fieldOf("price").forGetter(Listing::price),
			Codec.LONG.fieldOf("expires").forGetter(Listing::expires)
		).apply(i, Listing::new));
	}

	public record Mail(int coins, List<ItemStack> items, int sales) {
		public static final Mail EMPTY = new Mail(0, List.of(), 0);
		public static final Codec<Mail> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.optionalFieldOf("coins", 0).forGetter(Mail::coins),
			ItemStack.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(Mail::items),
			Codec.INT.optionalFieldOf("sales", 0).forGetter(Mail::sales)
		).apply(i, Mail::new));

		public boolean isEmpty() {
			return this.coins == 0 && this.items.isEmpty() && this.sales == 0;
		}
	}

	private static final Codec<AuctionHouse> CODEC = RecordCodecBuilder.create(i -> i.group(
		Listing.CODEC.listOf().optionalFieldOf("listings", List.of()).forGetter(d -> d.listings),
		Codec.unboundedMap(Codec.STRING, Mail.CODEC).optionalFieldOf("mail", Map.of()).forGetter(d -> d.mail),
		Codec.INT.optionalFieldOf("next_id", 1).forGetter(d -> d.nextId)
	).apply(i, AuctionHouse::new));

	public static final SavedDataType<AuctionHouse> TYPE = new SavedDataType<>(MinecraftMode.id("auction_house"), AuctionHouse::new, CODEC, null);

	private final List<Listing> listings;
	private final Map<String, Mail> mail;
	private int nextId;

	public AuctionHouse() {
		this(List.of(), Map.of(), 1);
	}

	private AuctionHouse(final List<Listing> listings, final Map<String, Mail> mail, final int nextId) {
		this.listings = new ArrayList<>(listings);
		this.mail = new HashMap<>(mail);
		this.nextId = nextId;
	}

	public static AuctionHouse get(final MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public List<Listing> listings() {
		return List.copyOf(this.listings);
	}

	public @Nullable Listing listing(final int id) {
		for (Listing l : this.listings) {
			if (l.id() == id) {
				return l;
			}
		}
		return null;
	}

	public int countBy(final UUID seller) {
		return (int)this.listings.stream().filter(l -> l.seller().equals(seller)).count();
	}

	public Listing add(final UUID seller, final String sellerName, final ItemStack item, final int price, final long expires) {
		Listing listing = new Listing(this.nextId++, seller, sellerName, item.copy(), price, expires);
		this.listings.add(listing);
		this.setDirty();
		return listing;
	}

	public boolean remove(final Listing listing) {
		boolean removed = this.listings.remove(listing);
		if (removed) {
			this.setDirty();
		}
		return removed;
	}

	public Mail mail(final UUID player) {
		return this.mail.getOrDefault(player.toString(), Mail.EMPTY);
	}

	public void sendCoins(final UUID player, final int coins, final boolean sale) {
		Mail m = this.mail(player);
		this.mail.put(player.toString(), new Mail(m.coins() + coins, m.items(), m.sales() + (sale ? 1 : 0)));
		this.setDirty();
	}

	public void sendItem(final UUID player, final ItemStack item) {
		Mail m = this.mail(player);
		List<ItemStack> items = new ArrayList<>(m.items());
		items.add(item.copy());
		this.mail.put(player.toString(), new Mail(m.coins(), items, m.sales()));
		this.setDirty();
	}

	/** Takes everything out of {@code player}'s mailbox. */
	public Mail takeMail(final UUID player) {
		Mail m = this.mail.remove(player.toString());
		if (m != null) {
			this.setDirty();
		}
		return m == null ? Mail.EMPTY : m;
	}

	/** Listings past their time go back to the seller's mailbox. */
	public int expire(final long now) {
		List<Listing> expired = this.listings.stream().filter(l -> l.expires() <= now).toList();
		for (Listing l : expired) {
			this.listings.remove(l);
			this.sendItem(l.seller(), l.item());
		}
		if (!expired.isEmpty()) {
			this.setDirty();
		}
		return expired.size();
	}
}
