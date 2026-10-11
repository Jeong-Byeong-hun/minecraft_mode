package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client, every second while a titan is up: where the field bosses of the player's dimension are, so the map can show them
 * beyond the range the client tracks entities in. The client forgets a boss that is not repeated for a few seconds.
 */
public record FieldBossPayload(List<Boss> bosses) implements CustomPacketPayload {
	public static final Type<FieldBossPayload> TYPE = new Type<>(MinecraftMode.id("field_bosses"));

	/** @param id the entity id, so the client can skip a boss it already sees */
	public record Boss(int id, int x, int z, Component name) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Boss> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Boss::id,
			ByteBufCodecs.VAR_INT, Boss::x,
			ByteBufCodecs.VAR_INT, Boss::z,
			ComponentSerialization.STREAM_CODEC, Boss::name,
			Boss::new
		);
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, FieldBossPayload> CODEC = StreamCodec.composite(
		Boss.CODEC.apply(ByteBufCodecs.list()), FieldBossPayload::bosses,
		FieldBossPayload::new
	);

	@Override
	public Type<FieldBossPayload> type() {
		return TYPE;
	}
}
