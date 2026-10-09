package com.minecraftmode.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A dungeon keystone's data (item component): which dungeon and how hard. Beating the run in time upgrades the keystone (and
 * moves it to another dungeon); failing or running late lowers it.
 */
public record Keystone(String dungeon, int level) {
	public static final int MIN_LEVEL = 2;
	public static final Codec<Keystone> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.fieldOf("dungeon").forGetter(Keystone::dungeon),
		Codec.INT.fieldOf("level").forGetter(Keystone::level)
	).apply(i, Keystone::new));
	public static final StreamCodec<ByteBuf, Keystone> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, Keystone::dungeon,
		ByteBufCodecs.VAR_INT, Keystone::level,
		Keystone::new
	);
}
