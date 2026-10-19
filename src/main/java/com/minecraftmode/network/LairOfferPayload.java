package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: the lair map found {@code lair} (a lair id) with its entrance at {@code pos}, {@code distance} blocks away; the client asks
 * whether to travel there ({@code LairTravelScreen}) and answers with {@link LairTravelPayload#TRAVEL}.
 */
public record LairOfferPayload(String lair, BlockPos pos, int distance) implements CustomPacketPayload {
	public static final Type<LairOfferPayload> TYPE = new Type<>(MinecraftMode.id("lair_offer"));
	public static final StreamCodec<RegistryFriendlyByteBuf, LairOfferPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, LairOfferPayload::lair,
		BlockPos.STREAM_CODEC, LairOfferPayload::pos,
		ByteBufCodecs.VAR_INT, LairOfferPayload::distance,
		LairOfferPayload::new
	);

	@Override
	public Type<LairOfferPayload> type() {
		return TYPE;
	}
}
