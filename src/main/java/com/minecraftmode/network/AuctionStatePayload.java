package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * Server -> client: the market as seen at broker {@code entityId} (opens the screen if needed): every listing (the client
 * filters, sorts and pages), the mailbox of the viewer and how many listings they have up.
 */
public record AuctionStatePayload(int entityId, List<Entry> listings, int mailCoins, List<ItemStack> mailItems, int myListings)
	implements CustomPacketPayload {
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

	public static final Type<AuctionStatePayload> TYPE = new Type<>(MinecraftMode.id("auction_state"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AuctionStatePayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, AuctionStatePayload::entityId,
		Entry.CODEC.apply(ByteBufCodecs.list()), AuctionStatePayload::listings,
		ByteBufCodecs.VAR_INT, AuctionStatePayload::mailCoins,
		ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), AuctionStatePayload::mailItems,
		ByteBufCodecs.VAR_INT, AuctionStatePayload::myListings,
		AuctionStatePayload::new
	);

	@Override
	public Type<AuctionStatePayload> type() {
		return TYPE;
	}
}
