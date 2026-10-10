package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: the player just damaged entity {@code entityId} (melee, shots or skills). The client shows that monster's
 * health bar for a while; health and max health come from the entity the client already tracks.
 */
public record TargetHealthPayload(int entityId) implements CustomPacketPayload {
	public static final Type<TargetHealthPayload> TYPE = new Type<>(MinecraftMode.id("target_health"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TargetHealthPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, TargetHealthPayload::entityId,
		TargetHealthPayload::new
	);

	@Override
	public Type<TargetHealthPayload> type() {
		return TYPE;
	}
}
