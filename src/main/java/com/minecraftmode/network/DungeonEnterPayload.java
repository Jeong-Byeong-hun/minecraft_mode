package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: the warden's "enter" button for dungeon {@code dungeon}, with the leader's keystone when {@code keystone}. */
public record DungeonEnterPayload(int wardenId, String dungeon, boolean keystone) implements CustomPacketPayload {
	public static final Type<DungeonEnterPayload> TYPE = new Type<>(MinecraftMode.id("dungeon_enter"));
	public static final StreamCodec<RegistryFriendlyByteBuf, DungeonEnterPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, DungeonEnterPayload::wardenId,
		ByteBufCodecs.stringUtf8(64), DungeonEnterPayload::dungeon,
		ByteBufCodecs.BOOL, DungeonEnterPayload::keystone,
		DungeonEnterPayload::new
	);

	@Override
	public Type<DungeonEnterPayload> type() {
		return TYPE;
	}
}
