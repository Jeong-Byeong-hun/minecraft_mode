package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: buy back line {@code index} of the player's recent sales (see {@code economy/Buyback}) while a shop is open. */
public record BuybackPayload(int index) implements CustomPacketPayload {
	public static final Type<BuybackPayload> TYPE = new Type<>(MinecraftMode.id("buyback"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BuybackPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, BuybackPayload::index,
		BuybackPayload::new);

	@Override
	public Type<BuybackPayload> type() {
		return TYPE;
	}
}
