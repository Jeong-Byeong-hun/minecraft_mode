package com.minecraftmode.client.map;

import com.minecraftmode.entity.EliteMob;
import com.minecraftmode.entity.TrainingDummy;
import com.minecraftmode.entity.boss.RaidBoss;
import com.minecraftmode.job.quest.Quests;
import com.minecraftmode.network.FieldBossPayload;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Monsters worth a mark on the map: named monsters and mini-bosses ({@link EliteMob}, never training dummies), raid bosses and the
 * vanilla bosses the client sees, plus the field bosses the server reports from farther away (the titan, {@link FieldBossPayload}).
 */
public final class FieldBosses {
	/** A reported boss not repeated for this long is forgotten (the server repeats every second). */
	private static final long REMOTE_MS = 3000;

	/** @param big a boss (raid boss, titan or champion, vanilla boss) rather than a named monster or mini-boss */
	public record Mark(double x, double z, Component name, boolean big) {
	}

	private static List<FieldBossPayload.Boss> remote = List.of();
	private static @Nullable ResourceKey<Level> remoteDimension;
	private static long remoteAt;

	static void receive(final FieldBossPayload payload, final @Nullable ClientLevel level) {
		remote = payload.bosses();
		remoteDimension = level == null ? null : level.dimension();
		remoteAt = System.currentTimeMillis();
	}

	static void clear() {
		remote = List.of();
		remoteDimension = null;
	}

	/** What the server reported last (the titan), also while the client sees it. */
	public static List<FieldBossPayload.Boss> reported() {
		return System.currentTimeMillis() - remoteAt < REMOTE_MS ? remote : List.of();
	}

	public static List<Mark> all(final Level level) {
		List<Mark> out = new ArrayList<>();
		if (!(level instanceof ClientLevel client)) {
			return out;
		}
		Set<Integer> seen = new HashSet<>();
		for (Entity entity : client.entitiesForRendering()) {
			if (entity instanceof LivingEntity living && living.isAlive() && isBoss(living)) {
				out.add(new Mark(living.getX(), living.getZ(), living.getDisplayName(), big(living)));
				seen.add(living.getId());
			}
		}
		if (level.dimension() == remoteDimension && System.currentTimeMillis() - remoteAt < REMOTE_MS) {
			for (FieldBossPayload.Boss boss : remote) {
				if (!seen.contains(boss.id())) {
					out.add(new Mark(boss.x() + 0.5, boss.z() + 0.5, boss.name(), true));
				}
			}
		}
		return out;
	}

	static boolean isBoss(final LivingEntity entity) {
		return entity instanceof EliteMob && !(entity instanceof TrainingDummy) || Quests.BOSSES.contains(entity.getType());
	}

	/** Raid bosses, champions (titans, dungeon bosses: grown past 1.5x) and vanilla bosses. */
	private static boolean big(final LivingEntity entity) {
		return entity instanceof RaidBoss || !(entity instanceof EliteMob) || entity.getScale() >= 1.5F;
	}

	/** A diamond with a black rim: orange for named monsters and mini-bosses, larger and crimson for bosses. */
	static void icon(final GuiGraphicsExtractor g, final int px, final int py, final boolean big) {
		int radius = big ? 5 : 4;
		int color = big ? 0xFFE02050 : 0xFFFF8020;
		for (int dy = -radius; dy <= radius; dy++) {
			int r = radius - Math.abs(dy);
			g.fill(px - r, py + dy, px + r + 1, py + dy + 1, 0xFF000000);
			if (r >= 1 && Math.abs(dy) < radius) {
				g.fill(px - r + 1, py + dy, px + r, py + dy + 1, color);
			}
		}
		if (big) {
			g.fill(px, py - 1, px + 1, py + 2, 0xFFFFFFFF);
		}
	}

	private FieldBosses() {
	}
}
