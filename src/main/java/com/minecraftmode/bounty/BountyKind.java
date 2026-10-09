package com.minecraftmode.bounty;

/** What a bounty asks for. Ids are saved keys. */
public enum BountyKind {
	/** Defeat any hostile monsters. */
	KILL_ANY("kill_any"),
	/** Defeat monsters of one type (target = entity id). */
	KILL_TYPE("kill_type"),
	/** Defeat named monsters (any; target unused). */
	KILL_NAMED("kill_named"),
	/** Clear named lairs (open a lair treasure after its lord fell). */
	CLEAR_LAIR("clear_lair"),
	/** Mine iron or better ores. */
	MINE_ORE("mine_ore"),
	/** Clear raids (any boss, any difficulty, rewarded clears only). */
	RAID("raid"),
	/** Hand in items at the board (target = item id). */
	DELIVER("deliver");

	private final String id;

	BountyKind(final String id) {
		this.id = id;
	}

	public String id() {
		return this.id;
	}

	public String key() {
		return "bounty.minecraft_mode." + this.id;
	}

	/** Bounties with a target only count that target; the rest count everything of the kind. */
	public boolean targeted() {
		return this == KILL_TYPE || this == DELIVER;
	}

	public static BountyKind byId(final String id) {
		for (BountyKind kind : values()) {
			if (kind.id.equals(id)) {
				return kind;
			}
		}
		return KILL_ANY;
	}
}
