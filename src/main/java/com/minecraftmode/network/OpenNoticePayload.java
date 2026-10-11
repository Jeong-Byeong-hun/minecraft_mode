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
 * Server -> client: open the town crier's notice board ({@code entityId} is the crier): when things renew ({@link Resets}), ticks to
 * the next dusk event (an invasion or a titan), the titan walking now (named monster id, "" when none, and where it rose), the
 * invasion wave (0 when none), this cycle's raid modifiers and the fastest clear of every boss and difficulty.
 */
public record OpenNoticePayload(int entityId, Resets resets, long eventLeft, boolean invasionNext, String titan, int titanX, int titanZ,
	int invasionWave, List<String> affixes, Map<String, List<RaidRecordsData.Entry>> records) implements CustomPacketPayload {
	public static final Type<OpenNoticePayload> TYPE = new Type<>(MinecraftMode.id("open_notice"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenNoticePayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, OpenNoticePayload::entityId,
		Resets.CODEC, OpenNoticePayload::resets,
		ByteBufCodecs.VAR_LONG, OpenNoticePayload::eventLeft,
		ByteBufCodecs.BOOL, OpenNoticePayload::invasionNext,
		ByteBufCodecs.STRING_UTF8, OpenNoticePayload::titan,
		ByteBufCodecs.VAR_INT, OpenNoticePayload::titanX,
		ByteBufCodecs.VAR_INT, OpenNoticePayload::titanZ,
		ByteBufCodecs.VAR_INT, OpenNoticePayload::invasionWave,
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), OpenNoticePayload::affixes,
		ByteBufCodecs.fromCodecWithRegistries(RaidRecordsData.TABLE_CODEC), OpenNoticePayload::records,
		OpenNoticePayload::new
	);

	/**
	 * Ticks (50 ms) to each reset: the daily bounties and the raid cycle run on real time ({@code RealReset}), the lair treasure and
	 * the daily bread on the Minecraft calendar ({@code ResetCycle}).
	 */
	public record Resets(long day, long cycle, long lairs, long bread) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Resets> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_LONG, Resets::day,
			ByteBufCodecs.VAR_LONG, Resets::cycle,
			ByteBufCodecs.VAR_LONG, Resets::lairs,
			ByteBufCodecs.VAR_LONG, Resets::bread,
			Resets::new
		);
	}

	@Override
	public Type<OpenNoticePayload> type() {
		return TYPE;
	}
}
