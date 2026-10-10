package com.minecraftmode.city;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Damage meters of the training dummies, one per attacker (every dummy counts toward it): total damage, damage per second from the
 * first hit, critical share and the biggest hit, shown over the hotbar while hitting and sent to chat once the attacker stops for
 * {@link #IDLE_TICKS}. Kept in memory only.
 */
public final class DummyMeter {
	/** A pause this long ends a session and prints its result. */
	public static final int IDLE_TICKS = 100;
	/** How often the running numbers refresh over the hotbar. */
	private static final int SHOW_EVERY = 10;

	/** One attacker's run of hits. */
	public static final class Session {
		final long start;
		long last;
		long shown;
		float total;
		int hits;
		int crits;
		float max;

		Session(final long now) {
			this.start = now;
			this.last = now;
			this.shown = now - SHOW_EVERY;
		}

		/** Seconds from the first hit to the last, at least one tick. */
		public float seconds() {
			return Math.max(1L, this.last - this.start + 1L) / 20.0F;
		}

		public float dps() {
			return this.total / this.seconds();
		}

		public float total() {
			return this.total;
		}

		public int hits() {
			return this.hits;
		}

		public int crits() {
			return this.crits;
		}

		public float max() {
			return this.max;
		}
	}

	private static final Map<UUID, Session> SESSIONS = new HashMap<>();

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(DummyMeter::tick);
	}

	public static void record(final ServerPlayer player, final float damage, final boolean crit, final long now) {
		Session s = SESSIONS.computeIfAbsent(player.getUUID(), id -> new Session(now));
		s.last = now;
		s.total += damage;
		s.hits++;
		s.crits += crit ? 1 : 0;
		s.max = Math.max(s.max, damage);
		if (now - s.shown >= SHOW_EVERY) {
			s.shown = now;
			player.sendOverlayMessage(line("message.minecraft_mode.dummy.meter", s).withStyle(ChatFormatting.YELLOW));
		}
	}

	/** The running session of {@code player}, or null (for tests). */
	public static Session session(final ServerPlayer player) {
		return SESSIONS.get(player.getUUID());
	}

	private static void tick(final MinecraftServer server) {
		if (server.getTickCount() % 20 != 0 || SESSIONS.isEmpty()) {
			return;
		}
		long now = server.overworld().getGameTime();
		for (Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator(); it.hasNext();) {
			Map.Entry<UUID, Session> entry = it.next();
			Session s = entry.getValue();
			if (now - s.last < IDLE_TICKS) {
				continue;
			}
			it.remove();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player != null) {
				player.sendSystemMessage(line("message.minecraft_mode.dummy.result", s).withStyle(ChatFormatting.GOLD));
			}
		}
	}

	private static net.minecraft.network.chat.MutableComponent line(final String key, final Session s) {
		return Component.translatable(key, String.format("%.1f", s.seconds()), Math.round(s.total), String.format("%.1f", s.dps()), s.hits,
			s.hits == 0 ? 0 : Math.round(s.crits * 100.0F / s.hits), Math.round(s.max));
	}

	private DummyMeter() {
	}
}
