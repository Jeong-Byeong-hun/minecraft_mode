package com.minecraftmode.raid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A party of up to {@link Parties#MAX_SIZE} players; the first member is the leader. */
public final class Party {
	private final List<UUID> members = new ArrayList<>();
	private final Map<UUID, String> names = new HashMap<>();

	Party(final UUID leader, final String name) {
		this.add(leader, name);
	}

	public UUID leader() {
		return this.members.getFirst();
	}

	public boolean isLeader(final UUID id) {
		return this.leader().equals(id);
	}

	public List<UUID> members() {
		return Collections.unmodifiableList(this.members);
	}

	public int size() {
		return this.members.size();
	}

	public boolean contains(final UUID id) {
		return this.members.contains(id);
	}

	public String name(final UUID id) {
		return this.names.getOrDefault(id, "?");
	}

	void add(final UUID id, final String name) {
		if (!this.members.contains(id)) {
			this.members.add(id);
		}
		this.names.put(id, name);
	}

	void remove(final UUID id) {
		this.members.remove(id);
		this.names.remove(id);
	}

	/** Makes {@code id} the leader (moves it to the front). */
	void promote(final UUID id) {
		if (this.members.remove(id)) {
			this.members.addFirst(id);
		}
	}
}
