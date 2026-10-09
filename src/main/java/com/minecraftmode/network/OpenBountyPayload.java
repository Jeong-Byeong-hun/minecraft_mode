package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: open the bounty board of the clerk {@code entityId}; ticks until the daily and the cycle bounties renew. */
public record OpenBountyPayload(int entityId, long dayLeft, long cycleLeft) implements CustomPacketPayload {
	public static final Type<OpenBountyPayload> TYPE = new Type<>(MinecraftMode.id("open_bounty"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenBountyPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, OpenBountyPayload::entityId,
		ByteBufCodecs.VAR_LONG, OpenBountyPayload::dayLeft,
		ByteBufCodecs.VAR_LONG, OpenBountyPayload::cycleLeft,
		OpenBountyPayload::new
	);

	@Override
	public Type<OpenBountyPayload> type() {
		return TYPE;
	}
}
