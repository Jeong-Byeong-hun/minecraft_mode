package com.minecraftmode.bounty;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A player's guild bounties: three daily ones (rolled for {@code day}), one bigger bounty for the
 * three-day {@code cycle}, and the merit earned (spent in the merit shop). Kept on death, synced.
 */
public record BountyData(long day, List<Bounty> daily, long cycle, Bounty special, int merit) {
	public static final BountyData DEFAULT = new BountyData(-1L, List.of(), -1L, Bounty.EMPTY, 0);

	/**
	 * One bounty: what to do ({@code kind} + {@code target}, empty target = any), how many, and how far along. A finished but
	 * unclaimed bounty survives the day (or cycle) change as {@code carried}; claiming it puts the new day's bounty in its slot.
	 */
	public record Bounty(String kind, String target, int need, int progress, boolean claimed, boolean carried) {
		public static final Bounty EMPTY = new Bounty("", "", 0, 0, false);
		public static final Codec<Bounty> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("kind").forGetter(Bounty::kind),
			Codec.STRING.optionalFieldOf("target", "").forGetter(Bounty::target),
			Codec.INT.fieldOf("need").forGetter(Bounty::need),
			Codec.INT.optionalFieldOf("progress", 0).forGetter(Bounty::progress),
			Codec.BOOL.optionalFieldOf("claimed", false).forGetter(Bounty::claimed),
			Codec.BOOL.optionalFieldOf("carried", false).forGetter(Bounty::carried)
		).apply(i, Bounty::new));

		public Bounty(final String kind, final String target, final int need, final int progress, final boolean claimed) {
			this(kind, target, need, progress, claimed, false);
		}

		public boolean isEmpty() {
			return this.kind.isEmpty();
		}

		public BountyKind type() {
			return BountyKind.byId(this.kind);
		}

		public boolean complete() {
			return this.progress >= this.need;
		}

		public Bounty withProgress(final int value) {
			return new Bounty(this.kind, this.target, this.need, Math.min(this.need, value), this.claimed, this.carried);
		}

		public Bounty asClaimed() {
			return new Bounty(this.kind, this.target, this.need, this.progress, true, this.carried);
		}

		public Bounty asCarried() {
			return new Bounty(this.kind, this.target, this.need, this.progress, this.claimed, true);
		}

		/** Finished on its own (not a delivery) and not handed in yet: worth keeping past the reset. */
		public boolean keeps() {
			return !this.isEmpty() && !this.claimed && this.type() != BountyKind.DELIVER && this.complete();
		}
	}

	public static final Codec<BountyData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.LONG.optionalFieldOf("day", -1L).forGetter(BountyData::day),
		Bounty.CODEC.listOf().optionalFieldOf("daily", List.of()).forGetter(BountyData::daily),
		Codec.LONG.optionalFieldOf("cycle", -1L).forGetter(BountyData::cycle),
		Bounty.CODEC.optionalFieldOf("special", Bounty.EMPTY).forGetter(BountyData::special),
		Codec.INT.optionalFieldOf("merit", 0).forGetter(BountyData::merit)
	).apply(i, BountyData::new));

	public static final StreamCodec<ByteBuf, BountyData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

	public BountyData {
		daily = List.copyOf(daily);
	}

	/** Bounty {@code index}: 0..2 daily, 3 the cycle bounty. */
	public Bounty get(final int index) {
		if (index == 3) {
			return this.special;
		}
		return index >= 0 && index < this.daily.size() ? this.daily.get(index) : Bounty.EMPTY;
	}

	public BountyData with(final int index, final Bounty bounty) {
		if (index == 3) {
			return new BountyData(this.day, this.daily, this.cycle, bounty, this.merit);
		}
		List<Bounty> list = new ArrayList<>(this.daily);
		list.set(index, bounty);
		return new BountyData(this.day, list, this.cycle, this.special, this.merit);
	}

	public BountyData withMerit(final int merit) {
		return new BountyData(this.day, this.daily, this.cycle, this.special, Math.max(0, merit));
	}
}
