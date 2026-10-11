package com.minecraftmode.progress;

import com.minecraftmode.MinecraftMode;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Raids and guild bounties reset on real time, like an online game's daily rewards: a day starts at {@link #RESET_HOUR}:00 on the
 * server's clock and in its time zone (singleplayer: this computer; a server in another zone runs with {@code -Duser.timezone=...}),
 * and a raid cycle is {@link #DAYS} such days. Raid lockouts, raid modifiers and the three-day bounty renew every cycle, daily
 * bounties every day. Everything else (lairs, daily bread, dungeon modifiers and free keystones, world events) stays on the
 * Minecraft calendar ({@link ResetCycle}), so lairs keep paying as often as players play.
 *
 * <p>The day never goes back: the latest day the server has seen is saved with the world, so a clock turned back (by hand or a time
 * sync) cannot hand out a reset twice; nothing renews until the clock passes that day again.
 */
public final class RealReset {
	public static final int RESET_HOUR = 6;
	public static final int DAYS = 3;
	private static final long MILLIS_PER_TICK = 50L;

	/** Game tests move the clock forward to reach the next day or cycle ({@link #skipDays}). Never saved; 0 in play. */
	private static long shift;

	private static long now() {
		return System.currentTimeMillis() + shift;
	}

	private static ZoneId zone() {
		return ZoneId.systemDefault();
	}

	/** The day {@code millis} falls in: days since the epoch, each starting at {@link #RESET_HOUR} local time. */
	private static long dayAt(final long millis) {
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone()).minusHours(RESET_HOUR).toLocalDate().toEpochDay();
	}

	/** When {@code day} starts, in epoch milliseconds. */
	private static long start(final long day) {
		return LocalDate.ofEpochDay(day).atTime(RESET_HOUR, 0).atZone(zone()).toInstant().toEpochMilli();
	}

	public static long day(final MinecraftServer server) {
		long day = dayAt(now());
		Seen seen = server.getDataStorage().computeIfAbsent(Seen.TYPE);
		if (day > seen.day) {
			seen.day = day;
			seen.setDirty();
		}
		return seen.day;
	}

	public static long cycle(final MinecraftServer server) {
		return Math.floorDiv(day(server), DAYS);
	}

	/** Real time to the next daily reset, in ticks of 50 ms (screens count it down on their own ticks). */
	public static long ticksToNextDay(final MinecraftServer server) {
		return ticksUntil(start(day(server) + 1));
	}

	/** Real time to the next raid cycle, in ticks of 50 ms. */
	public static long ticksToNextCycle(final MinecraftServer server) {
		return ticksUntil(start((cycle(server) + 1) * DAYS));
	}

	private static long ticksUntil(final long millis) {
		return Math.max(1L, (millis - now() + MILLIS_PER_TICK - 1) / MILLIS_PER_TICK);
	}

	/** "2d 5h", "5h 12m" or "12m" of real time ({@code ticks} of 50 ms). */
	public static Component remaining(final long ticks) {
		long minutes = Math.max(1L, (ticks + 1199L) / 1200L);
		long hours = minutes / 60L;
		if (hours >= 24L) {
			return Component.translatable("time.minecraft_mode.real.days", hours / 24L, hours % 24L);
		}
		return hours > 0L ? Component.translatable("time.minecraft_mode.real.hours", hours, minutes % 60L)
			: Component.translatable("time.minecraft_mode.real.minutes", minutes);
	}

	/** Game tests: moves this server's clock to a minute past the start of the day {@code days} after today. */
	public static void skipDays(final MinecraftServer server, final int days) {
		shift += start(day(server) + days) + 60_000L - now();
	}

	private RealReset() {
	}

	/** The latest day this world has seen. */
	private static final class Seen extends SavedData {
		private static final Codec<Seen> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.optionalFieldOf("day", Long.MIN_VALUE).forGetter(d -> d.day)
		).apply(i, Seen::new));
		static final SavedDataType<Seen> TYPE = new SavedDataType<>(MinecraftMode.id("real_reset"), Seen::new, CODEC, null);

		private long day;

		Seen() {
			this(Long.MIN_VALUE);
		}

		private Seen(final long day) {
			this.day = day;
		}
	}
}
