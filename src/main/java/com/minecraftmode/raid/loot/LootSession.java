package com.minecraftmode.raid.loot;

import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.loot.GearShop;
import com.minecraftmode.network.LootStatePayload;
import com.minecraftmode.raid.BossDef;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The shared loot of one boss kill: lots are resolved one after another, each by auction (default)
 * or dice. See {@link LootSessions} for the rules; this class only holds the state.
 */
public final class LootSession {
	public enum Mode {
		AUCTION,
		DICE
	}

	public enum State {
		WAITING,
		RUNNING,
		DONE
	}

	public static final class Lot {
		final ItemStack stack;
		Mode mode = Mode.AUCTION;
		State state = State.WAITING;
		final int start;
		final int step;
		int bid;
		@Nullable UUID bidder;
		long endsAt;
		/** Dice: rolls of this round; eligible players (all, or the tied ones on a re-roll). */
		final Map<UUID, Integer> rolls = new LinkedHashMap<>();
		final Set<UUID> passed = new HashSet<>();
		final Set<UUID> eligible = new HashSet<>();
		int round;
		@Nullable UUID winner;
		int price;

		Lot(final ItemStack stack) {
			this.stack = stack;
			ClassGear gear = ClassGear.of(stack);
			this.start = gear == null ? Coins.SILVER : GearShop.auctionStart(gear);
			this.step = Coins.increment(this.start);
		}

		public ItemStack stack() {
			return this.stack;
		}

		public Mode mode() {
			return this.mode;
		}

		public State state() {
			return this.state;
		}

		public int start() {
			return this.start;
		}

		public int step() {
			return this.step;
		}

		public int bid() {
			return this.bid;
		}

		public @Nullable UUID bidder() {
			return this.bidder;
		}

		public @Nullable UUID winner() {
			return this.winner;
		}

		public int price() {
			return this.price;
		}

		/** The amount a bid of {@code steps} steps would be. */
		public int nextBid(final int steps) {
			return this.bid == 0 ? this.start + this.step * Math.max(0, steps - 1) : this.bid + this.step * Math.max(1, steps);
		}
	}

	final int id;
	final BossDef boss;
	final Map<UUID, String> participants;
	final UUID leader;
	final List<Lot> lots = new ArrayList<>();
	int current;
	long ticks;
	/** Tick the next lot starts (pause between lots). */
	long nextStart;
	long finishedAt = -1;

	LootSession(final int id, final BossDef boss, final Map<UUID, String> participants, final UUID leader, final List<ItemStack> items) {
		this.id = id;
		this.boss = boss;
		this.participants = new LinkedHashMap<>(participants);
		this.leader = leader;
		for (ItemStack stack : items) {
			this.lots.add(new Lot(stack));
		}
	}

	public int id() {
		return this.id;
	}

	public List<Lot> lots() {
		return List.copyOf(this.lots);
	}

	public Map<UUID, String> participants() {
		return java.util.Collections.unmodifiableMap(this.participants);
	}

	public UUID leader() {
		return this.leader;
	}

	public boolean finished() {
		return this.finishedAt >= 0;
	}

	public @Nullable Lot currentLot() {
		return this.current < this.lots.size() ? this.lots.get(this.current) : null;
	}

	String name(final @Nullable UUID id) {
		return id == null ? "" : this.participants.getOrDefault(id, "?");
	}

	LootStatePayload payload(final UUID viewer, final boolean open) {
		List<LootStatePayload.Lot> out = new ArrayList<>();
		for (Lot lot : this.lots) {
			Integer roll = lot.rolls.get(viewer);
			int myRoll = lot.passed.contains(viewer) ? -1 : roll == null ? 0 : roll;
			List<String> rolls = new ArrayList<>();
			for (Map.Entry<UUID, Integer> e : lot.rolls.entrySet()) {
				rolls.add(this.name(e.getKey()) + ": " + e.getValue());
			}
			int left = lot.state == State.RUNNING ? (int)Math.max(0, lot.endsAt - this.ticks) : 0;
			out.add(new LootStatePayload.Lot(lot.stack, lot.mode.ordinal(), lot.state.ordinal(), lot.start, lot.step, lot.bid, this.name(lot.bidder), left,
				this.name(lot.winner), lot.price, myRoll, lot.mode == Mode.DICE ? lot.eligible.contains(viewer) : true, rolls));
		}
		return new LootStatePayload(this.id, this.boss.nameKey(), this.leader.equals(viewer), open, this.current, out);
	}
}
