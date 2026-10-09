package com.minecraftmode.job.engrave;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Item component on class gear: engraving ids (duplicates allowed and stacking; up to
 * {@link #WEAPON_LINES} on weapons and {@link #ARMOR_LINES} on armor) plus the seed that picks the
 * table's current offers.
 */
public record Engravings(List<String> lines, int seed) {
	public static final int WEAPON_LINES = 3;
	public static final int ARMOR_LINES = 4;
	public static final int MAX_LINES = Math.max(WEAPON_LINES, ARMOR_LINES);
	public static final Engravings EMPTY = new Engravings(List.of(), 0);

	public static final Codec<Engravings> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.listOf().optionalFieldOf("lines", List.of()).forGetter(Engravings::lines),
		Codec.INT.optionalFieldOf("seed", 0).forGetter(Engravings::seed)
	).apply(i, Engravings::new));

	public static final StreamCodec<ByteBuf, Engravings> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(MAX_LINES)), Engravings::lines,
		ByteBufCodecs.VAR_INT, Engravings::seed,
		Engravings::new
	);

	public Engravings {
		lines = List.copyOf(lines);
	}

	public List<Engraving> resolved() {
		return this.lines.stream().map(Engraving::byId).filter(Objects::nonNull).toList();
	}

	public Engravings with(final Engraving engraving, final int newSeed) {
		List<String> list = new ArrayList<>(this.lines);
		list.add(engraving.id());
		return new Engravings(list, newSeed);
	}

	public Engravings without(final int index, final int newSeed) {
		List<String> list = new ArrayList<>(this.lines);
		if (index >= 0 && index < list.size()) {
			list.remove(index);
		}
		return new Engravings(list, newSeed);
	}

	public Engravings withSeed(final int newSeed) {
		return new Engravings(this.lines, newSeed);
	}

	public EngraveTotals totals() {
		return EngraveTotals.of(this.resolved());
	}
}
