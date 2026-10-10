package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: the trash button of the inventory was clicked (bin the cursor stack, or take the last binned one back). */
public record TrashPayload() implements CustomPacketPayload {
	public static final TrashPayload INSTANCE = new TrashPayload();
	public static final Type<TrashPayload> TYPE = new Type<>(MinecraftMode.id("trash"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TrashPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<TrashPayload> type() {
		return TYPE;
	}
}
