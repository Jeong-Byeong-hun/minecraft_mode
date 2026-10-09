package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: open Guide Nella's question screen ({@code entityId} is the guide). */
public record OpenGuidePayload(int entityId) implements CustomPacketPayload {
	public static final Type<OpenGuidePayload> TYPE = new Type<>(MinecraftMode.id("open_guide"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenGuidePayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, OpenGuidePayload::entityId,
		OpenGuidePayload::new
	);

	@Override
	public Type<OpenGuidePayload> type() {
		return TYPE;
	}
}
