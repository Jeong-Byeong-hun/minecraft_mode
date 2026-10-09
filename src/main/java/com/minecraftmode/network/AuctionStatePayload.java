package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * Server -> client: one page of the market as the viewer asked for it at broker {@code entityId} (opens the screen if needed):
 * the listings on the page, how many matched and the page count, the viewer's mailbox, how many listings they have up, and
 * the going price per piece of the stack in {@code suggestSlot} (-1 when none was asked for or nothing like it is listed).
 */
public record AuctionStatePayload(int entityId, List<Entry> entries, int total, int page, int pages, Mail mail, int myListings, int suggestSlot,
	int suggestEach) implements CustomPacketPayload {
	public record Entry(int id, ItemStack item, int price, String seller, boolean mine, long ticksLeft) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Entry> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Entry::id,
			ItemStack.OPTIONAL_STREAM_CODEC, Entry::item,
			ByteBufCodecs.VAR_INT, Entry::price,
			ByteBufCodecs.stringUtf8(64), Entry::seller,
			ByteBufCodecs.BOOL, Entry::mine,
			ByteBufCodecs.VAR_LONG, Entry::ticksLeft,
			Entry::new
		);
	}

	/** The mailbox: proceeds, the first items waiting (all of them are collected) and how many items there are. */
	public record Mail(int coins, List<ItemStack> items, int count) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Mail> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Mail::coins,
			ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), Mail::items,
			ByteBufCodecs.VAR_INT, Mail::count,
			Mail::new
		);

		public boolean isEmpty() {
			return this.coins == 0 && this.count == 0;
		}
	}

	public static final Type<AuctionStatePayload> TYPE = new Type<>(MinecraftMode.id("auction_state"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AuctionStatePayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, AuctionStatePayload::entityId,
		Entry.CODEC.apply(ByteBufCodecs.list()), AuctionStatePayload::entries,
		ByteBufCodecs.VAR_INT, AuctionStatePayload::total,
		ByteBufCodecs.VAR_INT, AuctionStatePayload::page,
		ByteBufCodecs.VAR_INT, AuctionStatePayload::pages,
		Mail.CODEC, AuctionStatePayload::mail,
		ByteBufCodecs.VAR_INT, AuctionStatePayload::myListings,
		ByteBufCodecs.VAR_INT, AuctionStatePayload::suggestSlot,
		ByteBufCodecs.VAR_INT, AuctionStatePayload::suggestEach,
		AuctionStatePayload::new
	);

	@Override
	public Type<AuctionStatePayload> type() {
		return TYPE;
	}
}
