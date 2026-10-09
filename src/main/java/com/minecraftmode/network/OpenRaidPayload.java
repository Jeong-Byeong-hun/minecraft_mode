package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.raid.RaidRecordsData;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: open the raid screen of the raid marshal {@code entityId}, with the current cycle (for lockouts), the ticks
 * until it renews, the cycle's raid modifiers and the fastest clears of every boss and difficulty.
 */
public record OpenRaidPayload(int entityId, long cycle, long cycleLeft, List<String> affixes, Map<String, List<RaidRecordsData.Entry>> records)
	implements CustomPacketPayload {
	public static final Type<OpenRaidPayload> TYPE = new Type<>(MinecraftMode.id("open_raid"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenRaidPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, OpenRaidPayload::entityId,
		ByteBufCodecs.VAR_LONG, OpenRaidPayload::cycle,
		ByteBufCodecs.VAR_LONG, OpenRaidPayload::cycleLeft,
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), OpenRaidPayload::affixes,
		ByteBufCodecs.fromCodecWithRegistries(RaidRecordsData.TABLE_CODEC), OpenRaidPayload::records,
		OpenRaidPayload::new
	);

	@Override
	public Type<OpenRaidPayload> type() {
		return TYPE;
	}
}
