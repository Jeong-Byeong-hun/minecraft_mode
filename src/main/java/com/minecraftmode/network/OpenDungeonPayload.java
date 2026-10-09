package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: open the dungeon screen of the dungeon warden {@code entityId}; {@code cycle} picks the keystone modifiers. */
public record OpenDungeonPayload(int entityId, long cycle) implements CustomPacketPayload {
	public static final Type<OpenDungeonPayload> TYPE = new Type<>(MinecraftMode.id("open_dungeon"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenDungeonPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, OpenDungeonPayload::entityId,
		ByteBufCodecs.VAR_LONG, OpenDungeonPayload::cycle,
		OpenDungeonPayload::new
	);

	@Override
	public Type<OpenDungeonPayload> type() {
		return TYPE;
	}
}
