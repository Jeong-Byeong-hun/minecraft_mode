package com.minecraftmode.story;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A player's main story (attachment): chapters finished, whether the current chapter's goal was announced, and world bosses
 * defeated (story and achievements read it).
 */
public record StoryData(int chapter, boolean notified, int worldBosses) {
	public static final StoryData DEFAULT = new StoryData(0, false, 0);
	public static final Codec<StoryData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.optionalFieldOf("chapter", 0).forGetter(StoryData::chapter),
		Codec.BOOL.optionalFieldOf("notified", false).forGetter(StoryData::notified),
		Codec.INT.optionalFieldOf("world_bosses", 0).forGetter(StoryData::worldBosses)
	).apply(i, StoryData::new));
	public static final StreamCodec<ByteBuf, StoryData> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, StoryData::chapter,
		ByteBufCodecs.BOOL, StoryData::notified,
		ByteBufCodecs.VAR_INT, StoryData::worldBosses,
		StoryData::new
	);

	public StoryData next() {
		return new StoryData(this.chapter + 1, false, this.worldBosses);
	}

	public StoryData announced() {
		return new StoryData(this.chapter, true, this.worldBosses);
	}

	public StoryData withWorldBoss() {
		return new StoryData(this.chapter, this.notified, this.worldBosses + 1);
	}
}
