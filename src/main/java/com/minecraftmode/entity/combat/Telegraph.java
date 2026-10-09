package com.minecraftmode.entity.combat;

import com.minecraftmode.job.skill.SkillScheduler;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Ground warnings for monster and boss attacks: a colored outline drawn on the ground with a
 * filling ring that shows how much time is left, then the attack itself after {@code delay} ticks.
 * Every area attack of named monsters and bosses is telegraphed so players can dodge.
 */
public final class Telegraph {
	public static final int RED = 0xFF3030;
	public static final int ORANGE = 0xFF8A20;
	public static final int PURPLE = 0xB040FF;

	/** A circle that fills from the center outward; {@code then} runs when it is full. */
	public static void circle(final ServerLevel level, final Vec3 center, final double radius, final int delay, final int color, final Runnable then) {
		for (int t = 0; t < delay; t += 3) {
			final float fill = t / (float)delay;
			SkillScheduler.schedule(t, () -> {
				ring(level, center, radius, color, 1.0F);
				ring(level, center, Math.max(0.3, radius * fill), color, 0.7F);
			});
		}
		SkillScheduler.schedule(delay, then);
	}

	/** A ring with a safe hole in the middle (donut): outer and inner edges, filling inward. */
	public static void donut(final ServerLevel level, final Vec3 center, final double inner, final double outer, final int delay, final int color, final Runnable then) {
		for (int t = 0; t < delay; t += 3) {
			final float fill = t / (float)delay;
			SkillScheduler.schedule(t, () -> {
				ring(level, center, outer, color, 1.0F);
				ring(level, center, inner, 0x40FF60, 1.0F);
				ring(level, center, outer - (outer - inner) * fill, color, 0.7F);
			});
		}
		SkillScheduler.schedule(delay, then);
	}

	/** A strip from {@code from} to {@code to}, {@code width} wide, filling along its length. */
	public static void line(final ServerLevel level, final Vec3 from, final Vec3 to, final double width, final int delay, final int color, final Runnable then) {
		Vec3 dir = to.subtract(from).multiply(1, 0, 1);
		double length = dir.length();
		if (length < 0.01) {
			SkillScheduler.schedule(delay, then);
			return;
		}
		Vec3 unit = dir.scale(1.0 / length);
		Vec3 side = new Vec3(-unit.z, 0, unit.x).scale(width / 2);
		for (int t = 0; t < delay; t += 3) {
			final float fill = t / (float)delay;
			SkillScheduler.schedule(t, () -> {
				for (double d = 0; d <= length; d += 0.6) {
					Vec3 p = from.add(unit.scale(d));
					dot(level, p.add(side), color, 1.0F);
					dot(level, p.subtract(side), color, 1.0F);
					if (d <= length * fill) {
						dot(level, p, color, 0.7F);
					}
				}
			});
		}
		SkillScheduler.schedule(delay, then);
	}

	/** A cone from {@code origin} toward {@code yawDeg} (Minecraft yaw), {@code range} long and {@code halfAngle} degrees wide. */
	public static void cone(final ServerLevel level, final Vec3 origin, final float yawDeg, final double range, final double halfAngle, final int delay, final int color,
		final Runnable then) {
		for (int t = 0; t < delay; t += 3) {
			final float fill = t / (float)delay;
			SkillScheduler.schedule(t, () -> {
				for (double a = -halfAngle; a <= halfAngle; a += 6) {
					Vec3 dir = Vec3.directionFromRotation(0.0F, (float)(yawDeg + a));
					dot(level, origin.add(dir.scale(range)), color, 1.0F);
					dot(level, origin.add(dir.scale(range * fill)), color, 0.7F);
				}
				for (double d = 0; d <= range; d += 0.8) {
					dot(level, origin.add(Vec3.directionFromRotation(0.0F, (float)(yawDeg - halfAngle)).scale(d)), color, 1.0F);
					dot(level, origin.add(Vec3.directionFromRotation(0.0F, (float)(yawDeg + halfAngle)).scale(d)), color, 1.0F);
				}
			});
		}
		SkillScheduler.schedule(delay, then);
	}

	/** A dotted ring on the ground (visible from far away). */
	public static void ring(final ServerLevel level, final Vec3 center, final double radius, final int color, final float size) {
		int n = Mth.clamp((int)(radius * 7), 10, 160);
		for (int i = 0; i < n; i++) {
			double a = Math.PI * 2 * i / n;
			dot(level, center.add(Math.cos(a) * radius, 0.0, Math.sin(a) * radius), color, size);
		}
	}

	public static void dot(final ServerLevel level, final Vec3 p, final int color, final float size) {
		level.sendParticles(new DustParticleOptions(color, size * 1.6F), true, true, p.x, p.y + 0.15, p.z, 1, 0.0, 0.0, 0.0, 0.0);
	}

	private Telegraph() {
	}
}
