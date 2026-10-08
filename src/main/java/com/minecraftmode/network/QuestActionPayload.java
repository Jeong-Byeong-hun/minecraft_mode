package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: accept or complete the trial of trainer {@code entityId}, or abandon the active trial. */
public record QuestActionPayload(Action action, int entityId) implements CustomPacketPayload {
	public static final Type<QuestActionPayload> TYPE = new Type<>(MinecraftMode.id("quest_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, QuestActionPayload> CODEC = StreamCodec.composite(
		Action.STREAM_CODEC, QuestActionPayload::action, ByteBufCodecs.VAR_INT, QuestActionPayload::entityId, QuestActionPayload::new
	);

	public enum Action {
		ACCEPT,
		COMPLETE,
		ABANDON;

		public static final StreamCodec<ByteBuf, Action> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Action::ordinal);
	}

	@Override
	public Type<QuestActionPayload> type() {
		return TYPE;
	}
}
