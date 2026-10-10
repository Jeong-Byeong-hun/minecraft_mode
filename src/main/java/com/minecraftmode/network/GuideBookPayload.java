package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: ask Guide Nella ({@code entityId}) for a fresh copy of the adventurer's handbook. */
public record GuideBookPayload(int entityId) implements CustomPacketPayload {
	public static final Type<GuideBookPayload> TYPE = new Type<>(MinecraftMode.id("guide_book"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GuideBookPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, GuideBookPayload::entityId,
		GuideBookPayload::new
	);

	@Override
	public Type<GuideBookPayload> type() {
		return TYPE;
	}
}
