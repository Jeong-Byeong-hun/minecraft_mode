package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server: a market action at broker {@code entityId}: list inventory {@code slot} for {@code price}, buy or cancel
 * listing {@code id}, claim the mailbox, ask for the going price of the stack in {@code slot}, or just refresh. Every action
 * carries the viewer's {@link Query}, and the server answers with that page of the market.
 */
public record AuctionActionPayload(int entityId, int action, int id, int slot, int price, Query query) implements CustomPacketPayload {
	public static final int REFRESH = 0;
	public static final int LIST = 1;
	public static final int BUY = 2;
	public static final int CANCEL = 3;
	public static final int CLAIM = 4;
	public static final int PRICE = 5;

	/**
	 * What the viewer is looking at: everyone's listings or their own, the search text plus the items whose names match it in
	 * the viewer's language ({@code itemFilter} false when there were too many to send), category and sort ordinals, page.
	 */
	public record Query(boolean mine, String search, List<Integer> items, boolean itemFilter, int category, int sort, int page) {
		public static final int MAX_ITEMS = 4096;
		public static final Query DEFAULT = new Query(false, "", List.of(), false, 0, 0, 0);
		public static final StreamCodec<RegistryFriendlyByteBuf, Query> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, Query::mine,
			ByteBufCodecs.stringUtf8(40), Query::search,
			ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(MAX_ITEMS)), Query::items,
			ByteBufCodecs.BOOL, Query::itemFilter,
			ByteBufCodecs.VAR_INT, Query::category,
			ByteBufCodecs.VAR_INT, Query::sort,
			ByteBufCodecs.VAR_INT, Query::page,
			Query::new
		);
	}

	public static final Type<AuctionActionPayload> TYPE = new Type<>(MinecraftMode.id("auction_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AuctionActionPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, AuctionActionPayload::entityId,
		ByteBufCodecs.VAR_INT, AuctionActionPayload::action,
		ByteBufCodecs.VAR_INT, AuctionActionPayload::id,
		ByteBufCodecs.VAR_INT, AuctionActionPayload::slot,
		ByteBufCodecs.VAR_INT, AuctionActionPayload::price,
		Query.CODEC, AuctionActionPayload::query,
		AuctionActionPayload::new
	);

	@Override
	public Type<AuctionActionPayload> type() {
		return TYPE;
	}
}
