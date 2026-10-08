package com.minecraftmode.job.engrave;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Summed engraving values of one weapon, with caps applied. */
public final class EngraveTotals {
	public static final EngraveTotals EMPTY = new EngraveTotals(new EnumMap<>(EngraveStat.class));

	private final Map<EngraveStat, Float> values;

	private EngraveTotals(final Map<EngraveStat, Float> values) {
		this.values = values;
	}

	public static EngraveTotals of(final List<Engraving> lines) {
		if (lines.isEmpty()) {
			return EMPTY;
		}
		Map<EngraveStat, Float> map = new EnumMap<>(EngraveStat.class);
		for (Engraving e : lines) {
			map.merge(e.stat(), e.value(), Float::sum);
		}
		return new EngraveTotals(map);
	}

	/** Raw sum, capped. Percent stats are returned as percent (8 = 8%). */
	public float get(final EngraveStat stat) {
		float v = this.values.getOrDefault(stat, 0.0F);
		return stat.cap() > 0 ? Math.min(v, stat.cap()) : v;
	}

	/** {@link #get} / 100 for percent stats. */
	public float fraction(final EngraveStat stat) {
		return this.get(stat) / 100.0F;
	}

	public boolean isEmpty() {
		return this.values.isEmpty();
	}
}
