package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: the waymaster {@code entityId} offers these places ({@code Destination} ordinals). */
public record OpenTravelPayload(int entityId, List<Integer> destinations) implements CustomPacketPayload {
	public static final Type<OpenTravelPayload> TYPE = new Type<>(MinecraftMode.id("open_travel"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenTravelPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, OpenTravelPayload::entityId,
		ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), OpenTravelPayload::destinations,
		OpenTravelPayload::new
	);

	@Override
	public Type<OpenTravelPayload> type() {
		return TYPE;
	}
}
