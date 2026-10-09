package com.minecraftmode.progress;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * Repeatable content resets on the Minecraft calendar, not real time: a day is 24000 ticks of the
 * overworld clock (sleeping skips ahead too) and a cycle is {@link #DAYS} days. Lair rewards and lair
 * lords, raid lockouts, the cycle bounty and raid modifiers renew every cycle; daily bounties every day.
 */
public final class ResetCycle {
	public static final int DAYS = 3;
	public static final long DAY_TICKS = 24000L;
	public static final long CYCLE_TICKS = DAYS * DAY_TICKS;

	public static long ticks(final Level level) {
		return Math.max(0L, level.getOverworldClockTime());
	}

	public static long day(final Level level) {
		return ticks(level) / DAY_TICKS;
	}

	public static long cycle(final Level level) {
		return ticks(level) / CYCLE_TICKS;
	}

	public static long ticksToNextDay(final Level level) {
		return DAY_TICKS - ticks(level) % DAY_TICKS;
	}

	public static long ticksToNextCycle(final Level level) {
		return CYCLE_TICKS - ticks(level) % CYCLE_TICKS;
	}

	/** "2d 5h" in Minecraft days and hours (1000 ticks per hour). */
	public static Component remaining(final long ticks) {
		long hours = Math.max(1L, (ticks + 999L) / 1000L);
		return Component.translatable("time.minecraft_mode.remaining", hours / 24L, hours % 24L);
	}

	private ResetCycle() {
	}
}
