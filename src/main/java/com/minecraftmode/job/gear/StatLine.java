package com.minecraftmode.job.gear;

import com.minecraftmode.job.engrave.EngraveStat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** One stat and its value: an armor option, a set bonus line or a level reward. */
public record StatLine(EngraveStat stat, float value) {
	public static final Codec<EngraveStat> STAT_CODEC = Codec.STRING.comapFlatMap(
		id -> Arrays.stream(EngraveStat.values()).filter(s -> s.id().equals(id)).findFirst()
			.map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Unknown stat " + id)),
		EngraveStat::id
	);

	public static final Codec<StatLine> CODEC = RecordCodecBuilder.create(i -> i.group(
		STAT_CODEC.fieldOf("stat").forGetter(StatLine::stat),
		Codec.FLOAT.fieldOf("value").forGetter(StatLine::value)
	).apply(i, StatLine::new));

	public static final StreamCodec<ByteBuf, StatLine> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.idMapper(i -> EngraveStat.values()[i], EngraveStat::ordinal), StatLine::stat,
		ByteBufCodecs.FLOAT, StatLine::value,
		StatLine::new
	);

	public static StatLine of(final EngraveStat stat, final float value) {
		return new StatLine(stat, value);
	}
}
