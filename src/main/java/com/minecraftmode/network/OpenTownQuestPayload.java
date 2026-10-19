package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: open the town quests of the resident {@code entityId} ({@code role} = its {@code CityNpc.Role} id). {@code day} is
 * today's real day ({@code RealReset.day}, so the screen knows which daily quests are done today) and {@code dayLeft} the real-time
 * ticks (50 ms) until they open again. The quests and their progress come from the synced {@code TownQuestData}.
 */
public record OpenTownQuestPayload(int entityId, String role, long day, long dayLeft) implements CustomPacketPayload {
	public static final Type<OpenTownQuestPayload> TYPE = new Type<>(MinecraftMode.id("open_town_quest"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenTownQuestPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, OpenTownQuestPayload::entityId,
		ByteBufCodecs.STRING_UTF8, OpenTownQuestPayload::role,
		ByteBufCodecs.VAR_LONG, OpenTownQuestPayload::day,
		ByteBufCodecs.VAR_LONG, OpenTownQuestPayload::dayLeft,
		OpenTownQuestPayload::new
	);

	@Override
	public Type<OpenTownQuestPayload> type() {
		return TYPE;
	}
}
