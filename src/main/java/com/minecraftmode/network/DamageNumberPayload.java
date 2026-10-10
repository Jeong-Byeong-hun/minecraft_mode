package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: the player (or their skill summon) just dealt {@code amount} to entity {@code entityId}, a critical hit when
 * {@code crit}. The client floats the number over the target ({@code DamageNumbers}).
 */
public record DamageNumberPayload(int entityId, float amount, boolean crit) implements CustomPacketPayload {
	public static final Type<DamageNumberPayload> TYPE = new Type<>(MinecraftMode.id("damage_number"));
	public static final StreamCodec<RegistryFriendlyByteBuf, DamageNumberPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, DamageNumberPayload::entityId,
		ByteBufCodecs.FLOAT, DamageNumberPayload::amount,
		ByteBufCodecs.BOOL, DamageNumberPayload::crit,
		DamageNumberPayload::new
	);

	@Override
	public Type<DamageNumberPayload> type() {
		return TYPE;
	}
}
