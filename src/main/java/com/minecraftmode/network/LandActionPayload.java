package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server, from the land screen: {@link #TRUST} the online player named {@code text}, {@link #UNTRUST} the player whose
 * UUID is {@code text}, or {@link #ABANDON} the claim at chunk {@code chunkX, chunkZ} starting at {@code minY}.
 */
public record LandActionPayload(int action, String text, int chunkX, int chunkZ, int minY) implements CustomPacketPayload {
	public static final int TRUST = 0;
	public static final int UNTRUST = 1;
	public static final int ABANDON = 2;
	public static final Type<LandActionPayload> TYPE = new Type<>(MinecraftMode.id("land_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, LandActionPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, LandActionPayload::action,
		ByteBufCodecs.stringUtf8(64), LandActionPayload::text,
		ByteBufCodecs.VAR_INT, LandActionPayload::chunkX,
		ByteBufCodecs.VAR_INT, LandActionPayload::chunkZ,
		ByteBufCodecs.VAR_INT, LandActionPayload::minY,
		LandActionPayload::new
	);

	@Override
	public Type<LandActionPayload> type() {
		return TYPE;
	}
}
