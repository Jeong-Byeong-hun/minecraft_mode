package com.minecraftmode.domain;

import com.minecraftmode.network.DomainPayload;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Open domains (server memory): a circle around a center for a while, with a {@link DomainStyle} every player in that level is
 * told about ({@link DomainPayload}), so whoever stands inside sees it at once. What a domain does (Sukuna's sure hit) is up to
 * whoever opened it; {@link #at} says who stands where.
 */
public final class Domains {
	public record Domain(int id, ResourceKey<Level> dimension, Vec3 center, float radius, DomainStyle style, long until) {
		/** Inside the circle (and not far above or below it). */
		public boolean contains(final Entity e) {
			return e.level().dimension() == this.dimension && e.position().subtract(this.center).horizontalDistance() <= this.radius
				&& Math.abs(e.getY() - this.center.y) < this.radius;
		}
	}

	private static final Map<Integer, Domain> ACTIVE = new LinkedHashMap<>();
	private static int nextId = 1;

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Domains::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE.clear());
		// someone joining while a domain is open sees it too
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> {
			ServerPlayer player = handler.getPlayer();
			for (Domain d : ACTIVE.values()) {
				if (d.dimension() == player.level().dimension()) {
					send(player, d, (int)(d.until() - server.getTickCount()));
				}
			}
		}));
	}

	/** Opens a domain of {@code style} at {@code center} for {@code ticks}. */
	public static Domain open(final ServerLevel level, final Vec3 center, final float radius, final DomainStyle style, final int ticks) {
		Domain d = new Domain(nextId++, level.dimension(), center, radius, style, level.getServer().getTickCount() + ticks);
		ACTIVE.put(d.id(), d);
		for (ServerPlayer p : level.players()) {
			send(p, d, ticks);
		}
		return d;
	}

	/** Closes {@code d} before its time. */
	public static void close(final ServerLevel level, final Domain d) {
		if (ACTIVE.remove(d.id()) != null) {
			for (ServerPlayer p : level.players()) {
				send(p, d, 0);
			}
		}
	}

	/** The domain {@code e} stands in (the newest one), or null. */
	public static @Nullable Domain at(final Entity e) {
		Domain found = null;
		for (Domain d : ACTIVE.values()) {
			if (d.contains(e)) {
				found = d;
			}
		}
		return found;
	}

	public static List<Domain> active() {
		return List.copyOf(ACTIVE.values());
	}

	private static void tick(final MinecraftServer server) {
		List<Domain> ended = new ArrayList<>();
		for (Domain d : ACTIVE.values()) {
			if (server.getTickCount() >= d.until()) {
				ended.add(d);
			}
		}
		for (Domain d : ended) {
			ServerLevel level = server.getLevel(d.dimension());
			if (level != null) {
				close(level, d);
			} else {
				ACTIVE.remove(d.id());
			}
		}
	}

	private static void send(final ServerPlayer p, final Domain d, final int ticks) {
		if (ServerPlayNetworking.canSend(p, DomainPayload.TYPE)) {
			ServerPlayNetworking.send(p, DomainPayload.of(d, Math.max(0, ticks)));
		}
	}

	private Domains() {
	}
}
