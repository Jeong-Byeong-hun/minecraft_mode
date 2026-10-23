package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: a lair map of {@code tier} was read; {@code going} are the players who would come along (the reader first). The client
 * asks whether to open the tier lair ({@code TierLairScreen}) and answers with {@link LairTravelPayload#TIER}.
 */
public record TierOfferPayload(int tier, List<String> going) implements CustomPacketPayload {
	public static final Type<TierOfferPayload> TYPE = new Type<>(MinecraftMode.id("tier_offer"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TierOfferPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, TierOfferPayload::tier,
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), TierOfferPayload::going,
		TierOfferPayload::new
	);

	public TierOfferPayload {
		going = List.copyOf(going);
	}

	@Override
	public Type<TierOfferPayload> type() {
		return TYPE;
	}
}
