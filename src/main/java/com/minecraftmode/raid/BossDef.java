package com.minecraftmode.raid;

import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.loot.GearShop;
import java.util.List;
import net.minecraft.world.BossEvent;

/**
 * A raid boss: names, the gear levels it drops ({@code lo}..{@code hi}, also the level needed to
 * enter), effective health for one player (scaled by party size), stats, body size, arena and the
 * health fractions where new phases start (e.g. 0.7, 0.4 = phase 2 below 70%, phase 3 below 40%).
 */
public record BossDef(
	String id, String en, String ko, String epithetEn, String epithetKo, String descEn, String descKo, int lo, int hi, double health, double damage, double armor,
	double speed, float width, float height, ArenaTheme arena, BossEvent.BossBarColor color, List<Float> phases
) {
	public BossDef {
		phases = List.copyOf(phases);
	}

	public String nameKey() {
		return "entity.minecraft_mode." + this.id;
	}

	public String epithetKey() {
		return this.nameKey() + ".epithet";
	}

	public String descKey() {
		return this.nameKey() + ".desc";
	}

	/** Level shown on the boss and needed to enter. */
	public int minLevel() {
		return this.lo;
	}

	/** Entry fee per player, in copper: the guild price of a weapon of the boss's bracket. */
	public int fee() {
		return GearShop.bracketPrice(ItemLevels.bracket(this.lo));
	}

	/** Number of phases (1 + thresholds). */
	public int phaseCount() {
		return this.phases.size() + 1;
	}
}
