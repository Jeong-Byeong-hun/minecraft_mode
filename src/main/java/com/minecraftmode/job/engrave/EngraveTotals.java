package com.minecraftmode.job.engrave;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Summed stat values with caps applied: the engravings of one item, or everything a player has
 * (see {@code GearStats}: weapon, armor options, set bonuses and level rewards).
 */
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
		Builder builder = builder();
		for (Engraving e : lines) {
			builder.add(e.stat(), e.value());
		}
		return builder.build();
	}

	public static Builder builder() {
		return new Builder();
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

	public static final class Builder {
		private final Map<EngraveStat, Float> map = new EnumMap<>(EngraveStat.class);

		public Builder add(final EngraveStat stat, final float value) {
			if (value != 0.0F) {
				this.map.merge(stat, value, Float::sum);
			}
			return this;
		}

		public Builder addAll(final List<Engraving> lines) {
			for (Engraving e : lines) {
				this.add(e.stat(), e.value());
			}
			return this;
		}

		public EngraveTotals build() {
			return this.map.isEmpty() ? EMPTY : new EngraveTotals(new EnumMap<>(this.map));
		}
	}
}
