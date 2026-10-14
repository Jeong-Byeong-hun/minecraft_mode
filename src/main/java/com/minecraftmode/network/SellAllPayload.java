package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: sell all goods for line {@code offer} of the open shop, or for every line it buys ({@code economy/SellAll.ALL}). */
public record SellAllPayload(int offer) implements CustomPacketPayload {
	public static final Type<SellAllPayload> TYPE = new Type<>(MinecraftMode.id("sell_all"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SellAllPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, SellAllPayload::offer,
		SellAllPayload::new);

	@Override
	public Type<SellAllPayload> type() {
		return TYPE;
	}
}
