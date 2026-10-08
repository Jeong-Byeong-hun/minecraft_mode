package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: a skill key was pressed ({@code slot} 0..3 of the main-hand weapon). */
public record CastSkillPayload(int slot) implements CustomPacketPayload {
	public static final Type<CastSkillPayload> TYPE = new Type<>(MinecraftMode.id("cast_skill"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CastSkillPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, CastSkillPayload::slot, CastSkillPayload::new
	);

	@Override
	public Type<CastSkillPayload> type() {
		return TYPE;
	}
}
