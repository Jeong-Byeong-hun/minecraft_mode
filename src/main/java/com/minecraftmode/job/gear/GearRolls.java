package com.minecraftmode.job.gear;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Item component on class armor: the random extra options rolled when the piece first lands in a
 * player's inventory (see {@link ArmorOptions#roll}).
 */
public record GearRolls(List<StatLine> lines) {
	public static final GearRolls EMPTY = new GearRolls(List.of());
	public static final Codec<GearRolls> CODEC = StatLine.CODEC.listOf().xmap(GearRolls::new, GearRolls::lines);
	public static final StreamCodec<ByteBuf, GearRolls> STREAM_CODEC = StatLine.STREAM_CODEC.apply(ByteBufCodecs.list(8)).map(GearRolls::new, GearRolls::lines);

	public GearRolls {
		lines = List.copyOf(lines);
	}
}
