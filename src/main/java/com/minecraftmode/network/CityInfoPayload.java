package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client on join: whether this world's overworld has the capital ({@code CityZone.isCityLevel} needs the server's chunk
 * generator, so the client cannot tell by itself). The map uses it to draw Stormhold and its NPCs.
 */
public record CityInfoPayload(boolean city) implements CustomPacketPayload {
	public static final Type<CityInfoPayload> TYPE = new Type<>(MinecraftMode.id("city_info"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CityInfoPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, CityInfoPayload::city,
		CityInfoPayload::new
	);

	@Override
	public Type<CityInfoPayload> type() {
		return TYPE;
	}
}
