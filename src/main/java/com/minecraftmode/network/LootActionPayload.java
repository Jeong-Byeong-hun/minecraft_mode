package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server: an action in loot session {@code session} on the current lot. {@code value} is
 * the number of bid steps for {@link Action#BID}.
 */
public record LootActionPayload(int session, Action action, int value) implements CustomPacketPayload {
	public static final Type<LootActionPayload> TYPE = new Type<>(MinecraftMode.id("loot_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, LootActionPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, LootActionPayload::session,
		Action.STREAM_CODEC, LootActionPayload::action,
		ByteBufCodecs.VAR_INT, LootActionPayload::value,
		LootActionPayload::new
	);

	public enum Action {
		BID,
		ROLL,
		PASS,
		/** Leader: switch the current lot between auction and dice (before any bid). */
		SWITCH_MODE,
		/** Ask for the current state (opening the screen). */
		REFRESH;

		public static final StreamCodec<ByteBuf, Action> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[Math.floorMod(i, values().length)], Action::ordinal);
	}

	@Override
	public Type<LootActionPayload> type() {
		return TYPE;
	}
}
