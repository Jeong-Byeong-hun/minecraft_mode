package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: put a waypoint named {@code name} at {@code pos} in {@code dimension} on the player's map (the lair map marks
 * the lair it found). The client skips it when a waypoint is already there.
 */
public record MapMarkPayload(String dimension, BlockPos pos, Component name, int color) implements CustomPacketPayload {
	public static final Type<MapMarkPayload> TYPE = new Type<>(MinecraftMode.id("map_mark"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MapMarkPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, MapMarkPayload::dimension,
		BlockPos.STREAM_CODEC, MapMarkPayload::pos,
		ComponentSerialization.STREAM_CODEC, MapMarkPayload::name,
		ByteBufCodecs.INT, MapMarkPayload::color,
		MapMarkPayload::new
	);

	@Override
	public Type<MapMarkPayload> type() {
		return TYPE;
	}
}
