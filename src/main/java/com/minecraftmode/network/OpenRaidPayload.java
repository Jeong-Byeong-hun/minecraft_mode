package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: open the raid screen of the raid marshal {@code entityId}. */
public record OpenRaidPayload(int entityId) implements CustomPacketPayload {
	public static final Type<OpenRaidPayload> TYPE = new Type<>(MinecraftMode.id("open_raid"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenRaidPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, OpenRaidPayload::entityId, OpenRaidPayload::new
	);

	@Override
	public Type<OpenRaidPayload> type() {
		return TYPE;
	}
}
