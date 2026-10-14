package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: open the accessory window (the button by the inventory or on the character screen). */
public record OpenAccessoriesPayload() implements CustomPacketPayload {
	public static final OpenAccessoriesPayload INSTANCE = new OpenAccessoriesPayload();
	public static final Type<OpenAccessoriesPayload> TYPE = new Type<>(MinecraftMode.id("open_accessories"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenAccessoriesPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<OpenAccessoriesPayload> type() {
		return TYPE;
	}
}
