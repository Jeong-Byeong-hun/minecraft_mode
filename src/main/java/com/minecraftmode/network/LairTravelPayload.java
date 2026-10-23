package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server: {@link #TRAVEL} accepts the lair the map last offered (the server checks the map and the lair again), {@link #EXIT} asks
 * for the way back to the gate of the lair just cleared. The server keeps what it offered, so the payload carries nothing else.
 */
public record LairTravelPayload(int action) implements CustomPacketPayload {
	public static final int TRAVEL = 0;
	public static final int EXIT = 1;
	/** Opens the tier lair a tier map last offered ({@code TierLairs#confirm}). */
	public static final int TIER = 2;
	public static final Type<LairTravelPayload> TYPE = new Type<>(MinecraftMode.id("lair_travel"));
	public static final StreamCodec<RegistryFriendlyByteBuf, LairTravelPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, LairTravelPayload::action,
		LairTravelPayload::new
	);

	@Override
	public Type<LairTravelPayload> type() {
		return TYPE;
	}
}
