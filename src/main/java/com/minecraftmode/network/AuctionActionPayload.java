package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server: a market action at broker {@code entityId}: list inventory {@code slot} for {@code price}, buy or cancel
 * listing {@code id}, claim the mailbox, or just refresh.
 */
public record AuctionActionPayload(int entityId, int action, int id, int slot, int price) implements CustomPacketPayload {
	public static final int REFRESH = 0;
	public static final int LIST = 1;
	public static final int BUY = 2;
	public static final int CANCEL = 3;
	public static final int CLAIM = 4;
	public static final Type<AuctionActionPayload> TYPE = new Type<>(MinecraftMode.id("auction_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AuctionActionPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, AuctionActionPayload::entityId,
		ByteBufCodecs.VAR_INT, AuctionActionPayload::action,
		ByteBufCodecs.VAR_INT, AuctionActionPayload::id,
		ByteBufCodecs.VAR_INT, AuctionActionPayload::slot,
		ByteBufCodecs.VAR_INT, AuctionActionPayload::price,
		AuctionActionPayload::new
	);

	@Override
	public Type<AuctionActionPayload> type() {
		return TYPE;
	}
}
