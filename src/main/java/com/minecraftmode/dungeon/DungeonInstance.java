package com.minecraftmode.dungeon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** One party's run through one dungeon in its own slot of the dungeon dimension. */
public final class DungeonInstance {
	public enum State {
		/** Built, players arrived, short countdown before the first door opens. */
		COUNTDOWN,
		/** Halls and boss; the timer runs. */
		RUN,
		/** Boss down; players can leave. */
		VICTORY,
		CLOSED
	}

	/** A participant: where to send them back, and whether they are still in the run. */
	public static final class Member {
		final UUID id;
		final String name;
		final ResourceKey<Level> returnLevel;
		final Vec3 returnPos;
		final float returnYaw;
		boolean active = true;
		int deaths;

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

		public int deaths() {
			return this.deaths;
		}
	}

	final int id;
	final int slot;
	final DungeonDef def;
	final BlockPos origin;
	final UUID leader;
	/** Keystone level; 0 for a run without a keystone. */
	final int level;
	final List<DungeonAffix> affixes;
	/** Whose keystone the run uses (null without one). */
	final @Nullable UUID keystoneOwner;
	final Map<UUID, Member> members = new LinkedHashMap<>();
	State state = State.COUNTDOWN;
	int timer;
	/** Ticks the run has taken (plus death penalties). */
	int elapsed;
	int partySize;
	/** The room whose monsters are up (or next to wake): 1..3 halls, 4 boss. */
	int room = 1;
	boolean roomAwake;
	/** The boss fell (the keystone is settled by the victory, not by closing). */
	boolean finished;
	final Set<UUID> roomMobs = new HashSet<>();
	/** How often each hall monster bolstered (Bolstering stops at {@link DungeonAffix#BOLSTER_MAX}). */
	final Map<UUID, Integer> bolsters = new HashMap<>();
	/** {@link #elapsed} when a hall monster was last hurt (or the hall woke): a long quiet means the rest are stuck somewhere. */
	int lastHit;
	@Nullable UUID bossId;
	@Nullable ServerBossEvent bar;

	DungeonInstance(final int id, final int slot, final DungeonDef def, final BlockPos origin, final UUID leader, final int level, final List<DungeonAffix> affixes,
		final @Nullable UUID keystoneOwner) {
		this.id = id;
		this.slot = slot;
		this.def = def;
		this.origin = origin;
		this.leader = leader;
		this.level = level;
		this.affixes = List.copyOf(affixes);
		this.keystoneOwner = keystoneOwner;
	}

	public DungeonDef def() {
		return this.def;
	}

	public int level() {
		return this.level;
	}

	public State state() {
		return this.state;
	}

	public BlockPos origin() {
		return this.origin;
	}

	public int room() {
		return this.room;
	}

	public int elapsed() {
		return this.elapsed;
	}

	public List<DungeonAffix> affixes() {
		return this.affixes;
	}

	public @Nullable UUID bossId() {
		return this.bossId;
	}

	public Set<UUID> roomMobs() {
		return Set.copyOf(this.roomMobs);
	}

	public @Nullable Member member(final UUID id) {
		return this.members.get(id);
	}

	public java.util.Collection<Member> members() {
		return java.util.Collections.unmodifiableCollection(this.members.values());
	}

	boolean anyActive() {
		return this.members.values().stream().anyMatch(m -> m.active);
	}

	public boolean has(final DungeonAffix affix) {
		return this.affixes.contains(affix);
	}

	/** Time limit in ticks. */
	public int limit() {
		return this.def.timeLimit() * 20;
	}
}
