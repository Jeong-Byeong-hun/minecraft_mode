package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: at the clerk {@code entityId}, hand in bounty {@code index} ({@link #CLAIM}) or buy merit offer {@code index} ({@link #BUY}). */
public record BountyActionPayload(int entityId, int action, int index) implements CustomPacketPayload {
	public static final int CLAIM = 0;
	public static final int BUY = 1;
	public static final Type<BountyActionPayload> TYPE = new Type<>(MinecraftMode.id("bounty_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BountyActionPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, BountyActionPayload::entityId,
		ByteBufCodecs.VAR_INT, BountyActionPayload::action,
		ByteBufCodecs.VAR_INT, BountyActionPayload::index,
		BountyActionPayload::new
	);

	@Override
	public Type<BountyActionPayload> type() {
		return TYPE;
	}
}
