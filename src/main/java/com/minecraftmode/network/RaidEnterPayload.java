package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: the party leader asks the raid marshal {@code marshalId} to start the raid on {@code boss}. */
public record RaidEnterPayload(int marshalId, String boss) implements CustomPacketPayload {
	public static final Type<RaidEnterPayload> TYPE = new Type<>(MinecraftMode.id("raid_enter"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RaidEnterPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, RaidEnterPayload::marshalId,
		ByteBufCodecs.stringUtf8(32), RaidEnterPayload::boss,
		RaidEnterPayload::new
	);

	@Override
	public Type<RaidEnterPayload> type() {
		return TYPE;
	}
}
