package com.minecraftmode.job.skill;

import com.minecraftmode.registry.ModParticles;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Visual style of a skill: one of the mod's tinted particle sprites and an RGB color. All helpers
 * run on the server and send the particles to nearby players.
 */
public record Fx(Kind kind, int color) {
	public enum Kind {
		SPARK,
		SLASH,
		ORB,
		RING,
		RUNE,
		SHARD,
		SMOKE,
		PETAL,
		COIN,
		BOLT,
		FEATHER,
		BUBBLE;

		public ParticleType<ColorParticleOption> type() {
			return switch (this) {
				case SPARK -> ModParticles.SPARK;
				case SLASH -> ModParticles.SLASH;
				case ORB -> ModParticles.ORB;
				case RING -> ModParticles.RING;
				case RUNE -> ModParticles.RUNE;
				case SHARD -> ModParticles.SHARD;
				case SMOKE -> ModParticles.SMOKE;
				case PETAL -> ModParticles.PETAL;
				case COIN -> ModParticles.COIN;
				case BOLT -> ModParticles.BOLT;
				case FEATHER -> ModParticles.FEATHER;
				case BUBBLE -> ModParticles.BUBBLE;
			};
		}
	}

	public Fx withKind(final Kind kind) {
		return new Fx(kind, this.color);
	}

	public Fx withColor(final int color) {
		return new Fx(this.kind, color);
	}

	public ColorParticleOption option() {
		return option(this.kind);
	}

	/** Same color, different sprite (e.g. a spark burst for a slash skill). */
	public ColorParticleOption option(final Kind sprite) {
		return ColorParticleOption.create(sprite.type(), 0xFF000000 | this.color);
	}

	public void burst(final ServerLevel level, final Vec3 pos, final int count, final double spread, final double speed) {
		level.sendParticles(this.option(), pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
	}

	public void burst(final ServerLevel level, final Kind sprite, final Vec3 pos, final int count, final double spread, final double speed) {
		level.sendParticles(this.option(sprite), pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
	}

	/** A single particle that does not drift (count 0 makes the offsets a velocity). */
	public void still(final ServerLevel level, final Kind sprite, final Vec3 pos) {
		level.sendParticles(this.option(sprite), pos.x, pos.y, pos.z, 0, 0.0, 0.0, 0.0, 0.0);
	}

	public void moving(final ServerLevel level, final Kind sprite, final Vec3 pos, final Vec3 velocity) {
		level.sendParticles(this.option(sprite), pos.x, pos.y, pos.z, 0, velocity.x, velocity.y, velocity.z, 1.0);
	}

	public void line(final ServerLevel level, final Vec3 from, final Vec3 to, final double step) {
		Vec3 d = to.subtract(from);
		double len = d.length();
		int n = Math.max(1, (int)(len / step));
		for (int i = 0; i <= n; i++) {
			Vec3 p = from.add(d.scale((double)i / n));
			level.sendParticles(this.option(), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
		}
	}

	/** Flat circle of particles at {@code center.y}. */
	public void circle(final ServerLevel level, final Kind sprite, final Vec3 center, final double radius) {
		int n = Mth.clamp((int)(radius * 8), 8, 64);
		for (int i = 0; i < n; i++) {
			double a = Math.PI * 2 * i / n;
			this.still(level, sprite, center.add(Math.cos(a) * radius, 0.0, Math.sin(a) * radius));
		}
	}

	/** Horizontal arc in front of {@code eye} facing {@code yawDeg}, used by slashes. */
	public void arc(final ServerLevel level, final Vec3 eye, final float yawDeg, final double radius, final double arcDeg) {
		int n = Mth.clamp((int)(arcDeg / 12), 5, 24);
		double base = Math.toRadians(yawDeg + 90.0);
		for (int i = 0; i <= n; i++) {
			double a = base + Math.toRadians(-arcDeg / 2 + arcDeg * i / n);
			Vec3 p = eye.add(Math.cos(a) * radius, -0.3 + 0.3 * Math.sin(i * Math.PI / n), Math.sin(a) * radius);
			this.still(level, Kind.SLASH, p);
			if (i % 2 == 0) {
				level.sendParticles(this.option(), p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.02);
			}
		}
	}

	/** Vertical column, e.g. for lightning or a pillar of light. */
	public void column(final ServerLevel level, final Vec3 base, final double height) {
		for (double y = 0; y <= height; y += 0.5) {
			level.sendParticles(this.option(), base.x, base.y + y, base.z, 1, 0.08, 0.08, 0.08, 0.0);
		}
	}
}
