package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server: at the resident {@code entityId} (within 8 blocks, the quest's giver), {@link #ACCEPT}, {@link #TURN_IN} or
 * {@link #ABANDON} town quest {@code quest}, or {@link #ENTER} the jump course of a jump quest.
 */
public record TownQuestActionPayload(int entityId, int action, String quest) implements CustomPacketPayload {
	public static final int ACCEPT = 0;
	public static final int TURN_IN = 1;
	public static final int ABANDON = 2;
	public static final int ENTER = 3;
	public static final Type<TownQuestActionPayload> TYPE = new Type<>(MinecraftMode.id("town_quest_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TownQuestActionPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, TownQuestActionPayload::entityId,
		ByteBufCodecs.VAR_INT, TownQuestActionPayload::action,
		ByteBufCodecs.stringUtf8(64), TownQuestActionPayload::quest,
		TownQuestActionPayload::new
	);

	@Override
	public Type<TownQuestActionPayload> type() {
		return TYPE;
	}
}
