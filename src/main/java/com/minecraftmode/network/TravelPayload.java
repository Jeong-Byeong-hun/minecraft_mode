package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: ask the waymaster {@code entityId} for a trip to {@code destination} (a {@code Destination} ordinal). */
public record TravelPayload(int entityId, int destination) implements CustomPacketPayload {
	public static final Type<TravelPayload> TYPE = new Type<>(MinecraftMode.id("travel"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TravelPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, TravelPayload::entityId,
		ByteBufCodecs.VAR_INT, TravelPayload::destination,
		TravelPayload::new
	);

	@Override
	public Type<TravelPayload> type() {
		return TYPE;
	}
}
