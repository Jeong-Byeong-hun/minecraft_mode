package com.minecraftmode.raid;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** One party's fight against one boss in its own arena slot of the raid dimension. */
public final class RaidInstance {
	public enum State {
		/** Arena built, players arrived, countdown running. */
		COUNTDOWN,
		FIGHT,
		/** Boss down; loot is being shared, players can leave. */
		VICTORY,
		CLOSED
	}

	/** A participant: where to send them back, and whether they are still fighting. */
	public static final class Member {
		final UUID id;
		final String name;
		final ResourceKey<Level> returnLevel;
		final Vec3 returnPos;
		final float returnYaw;
		/** In the arena right now (not fallen, not left, not logged off). */
		boolean active = true;
		/** Fell in this fight (cannot come back to it). */
		boolean fallen;

		Member(final UUID id, final String name, final ResourceKey<Level> returnLevel, final Vec3 returnPos, final float returnYaw) {
			this.id = id;
			this.name = name;
			this.returnLevel = returnLevel;
			this.returnPos = returnPos;
			this.returnYaw = returnYaw;
		}

		public UUID id() {
			return this.id;
		}

		public String name() {
			return this.name;
		}

		public boolean active() {
			return this.active;
		}

		public boolean fallen() {
			return this.fallen;
		}
	}

	final int id;
	final int slot;
	final BossDef boss;
	final BlockPos center;
	final UUID leader;
	final Map<UUID, Member> members = new LinkedHashMap<>();
	State state = State.COUNTDOWN;
	/** Ticks spent in the current state. */
	int timer;
	@Nullable UUID bossId;
	int partySize;
	final RaidDifficulty difficulty;
	/** Members already rewarded for this boss and difficulty this cycle: they fight for practice (no fee, no rewards). */
	final Set<UUID> practice = new HashSet<>();
	List<RaidAffix> affixes = List.of();

	RaidInstance(final int id, final int slot, final BossDef boss, final BlockPos center, final UUID leader, final RaidDifficulty difficulty) {
		this.id = id;
		this.slot = slot;
		this.boss = boss;
		this.center = center;
		this.leader = leader;
		this.difficulty = difficulty;
	}

	public RaidDifficulty difficulty() {
		return this.difficulty;
	}

	public boolean practice(final UUID player) {
		return this.practice.contains(player);
	}

	public List<RaidAffix> affixes() {
		return this.affixes;
	}

	/** Ticks spent in the current state (the fight time once the boss is up). */
	public int timer() {
		return this.timer;
	}

	public int id() {
		return this.id;
	}

	public BossDef boss() {
		return this.boss;
	}

	public BlockPos center() {
		return this.center;
	}

	public State state() {
		return this.state;
	}

	public UUID leader() {
		return this.leader;
	}

	public Collection<Member> members() {
		return Collections.unmodifiableCollection(this.members.values());
	}

	public @Nullable Member member(final UUID id) {
		return this.members.get(id);
	}

	boolean anyActive() {
		for (Member m : this.members.values()) {
			if (m.active) {
				return true;
			}
		}
		return false;
	}
}
