package com.minecraftmode.client.vfx;

import static com.minecraftmode.client.vfx.SkillVfxRenderer.glow;
import static com.minecraftmode.client.vfx.SkillVfxRenderer.shade;
import static com.minecraftmode.client.vfx.VfxOut.argb;
import static com.minecraftmode.client.vfx.VfxOut.hash;

import com.minecraftmode.client.vfx.SkillProjectileRenderer.State;
import com.minecraftmode.job.skill.SkillLook;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * The archer's projectile looks, drawn round the flying arrow, bullet, gem or blade: Antares Snipe's starlit arrow, gunshot tracers,
 * Artemis's moon arrows and crescents, Agni's flaming arrow, Ishtar's gems, the treasures of the Gate, Merodach's crimson wave and the
 * divine discus. Trails never reach further back than the projectile has flown, so they never start inside the archer.
 */
final class ArrowLooks {
	private static final int ANTARES = 0xFF3A1E;
	private static final int GOLD = 0xFFD24A;
	private static final int MOON = 0xC8D8FF;
	private static final int HEART = 0xFF7AB8;
	private static final int[] GEMS = {0xFF2A4A, 0x3A6AFF, 0x2AE07A, 0xFFC02A, 0xC04AFF};

	/** How far round the projectile its look reaches (its culling box grows by this). */
	static double reach(final SkillLook look) {
		return switch (look) {
			case STAR_ARROW -> 8.0;
			case TRACER, MOON_ARROW, FLAME_ARROW, TREASURE -> 4.5;
			default -> 2.5;
		};
	}

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case STAR_ARROW -> starArrow(s, ps, c);
			case TRACER -> tracer(s, ps, c);
			case MOON_ARROW -> moonArrow(s, ps, c);
			case CRESCENT_ARROW -> crescent(s, ps, c, 0.36F, 0.15F, MOON, 0xFFFFFF, false);
			case FLAME_ARROW -> flameArrow(s, ps, c);
			case GEM -> gem(s, ps, c);
			case TREASURE -> treasure(s, ps, c);
			case SIN_WAVE -> crescent(s, ps, c, 1.05F, 0.42F, 0xD01020, GOLD, true);
			case DISCUS -> discus(s, ps, c);
			default -> {
			}
		}
	}

	/** How far behind the projectile a trail of {@code length} may reach: no further than it has flown. */
	private static float trail(final State s, final float length) {
		return Math.min(length, Math.max(0.0F, s.ageInTicks * Math.max(0.6F, s.speed) - 0.4F));
	}

	// ------------------------------------------------------------ Chiron

	/** Antares Snipe: the red star at the head, a white-hot shaft and a long tail of starlight with stars glinting in it. */
	private static void starArrow(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f back = new Vector3f(s.forward).negate();
		float tail = trail(s, 7.0F);
		float time = s.ageInTicks;
		glow(ps, c, o -> {
			Vector3f head = new Vector3f(s.forward).mul(0.4F);
			Vector3f end = new Vector3f(back).mul(tail);
			o.glowLine(head, end, 0.4F, argb(0.45F, ANTARES), argb(0.0F, ANTARES));
			o.glowLine(head, end, 0.18F, argb(0.9F, 0xFFB08A), argb(0.0F, 0xFF6A40));
			o.bar(head, new Vector3f(back).mul(Math.min(tail, 2.0F)), 0.03F, 0.01F, argb(1.0F, 0xFFFFFF), argb(0.0F, 0xFFFFFF));
			// Antares burning at the head, a cross of light through it
			o.sprite(head, s.camRight, s.camUp, 0.6F, argb(0.6F, ANTARES));
			o.sprite(head, s.camRight, s.camUp, 0.35F, argb(1.0F, 0xFFE8D8));
			o.glint(head, s.camRight, s.camUp, 1.3F, time * 0.15F, argb(0.9F, 0xFFD0B0));
			// stars glinting in the tail, drifting out a little as they fall behind
			Vector3f side = VfxOut.across(back);
			Vector3f up = new Vector3f(side).cross(back).normalize();
			for (int i = 0; i < 9; i++) {
				float d = 0.8F + 6.0F * hash(s.seed * 7 + i);
				if (d > tail) {
					continue;
				}
				float spread = 0.15F + 0.35F * d / 7.0F;
				float a = Mth.TWO_PI * hash(s.seed * 13 + i);
				Vector3f at = new Vector3f(back).mul(d).add(new Vector3f(side).mul(Mth.cos(a) * spread)).add(new Vector3f(up).mul(Mth.sin(a) * spread));
				float twinkle = 0.6F + 0.4F * Mth.sin(time * 1.3F + i * 2.1F);
				o.glint(at, s.camRight, s.camUp, 0.3F * twinkle, i, argb(0.9F * (1.0F - d / 8.0F), i % 3 == 0 ? 0xFF8A70 : 0xFFF2E0));
			}
		});
	}

	// ------------------------------------------------------------ EMIYA Alter

	/** A tracer: a hot tip, a streak of light behind the bullet, and a muzzle flash where it left the gun. */
	private static void tracer(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f back = new Vector3f(s.forward).negate();
		float tail = trail(s, 3.2F);
		glow(ps, c, o -> {
			Vector3f end = new Vector3f(back).mul(tail);
			o.glowLine(new Vector3f(), end, 0.25F, argb(0.6F, s.color), argb(0.0F, s.color));
			o.glowLine(new Vector3f(), end, 0.05F, argb(1.0F, 0xFFE2A0), argb(0.0F, 0xFFB050));
			o.sprite(new Vector3f(), s.camRight, s.camUp, 0.16F, argb(1.0F, 0xFFF6E0));
			if (s.ageInTicks < 2.5F) {
				// the muzzle flash, left behind where the shot started
				float k = 1.0F - s.ageInTicks / 2.5F;
				Vector3f muzzle = new Vector3f(back).mul(Math.min(s.ageInTicks * s.speed, 8.0F));
				o.star(muzzle, s.camRight, s.camUp, 0.75F * k + 0.2F, 6, 0.35F, s.seed, argb(0.95F * k, 0xFFF0C0), argb(0.0F, 0xFF7A20));
				o.sprite(muzzle, s.camRight, s.camUp, 0.5F, argb(0.7F * k, 0xFFB050));
			}
		});
	}

	// ------------------------------------------------------------ Artemis

	/** Tri-Star Amore Mio: a pale arrow of moonlight trailing hearts and stars. */
	private static void moonArrow(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f back = new Vector3f(s.forward).negate();
		float tail = trail(s, 4.0F);
		float time = s.ageInTicks;
		glow(ps, c, o -> {
			Vector3f end = new Vector3f(back).mul(tail);
			o.glowLine(new Vector3f(s.forward).mul(0.3F), end, 0.32F, argb(0.5F, MOON), argb(0.0F, MOON));
			o.glowLine(new Vector3f(s.forward).mul(0.3F), end, 0.08F, argb(1.0F, 0xFFFFFF), argb(0.0F, 0xFFD8EC));
			o.sprite(new Vector3f(s.forward).mul(0.3F), s.camRight, s.camUp, 0.45F, argb(0.8F, 0xE8F0FF));
			Vector3f side = VfxOut.across(back);
			for (int i = 0; i < 6; i++) {
				float d = 0.5F + i * 0.6F;
				if (d > tail) {
					break;
				}
				float sway = Mth.sin(time * 0.8F + i * 1.7F) * 0.25F;
				Vector3f at = new Vector3f(back).mul(d).add(new Vector3f(side).mul(sway)).add(0, 0.12F * Mth.cos(time * 0.6F + i), 0);
				float k = 1.0F - d / 4.2F;
				if (i % 2 == 0) {
					heart(o, at, s.camRight, s.camUp, 0.3F * (0.6F + 0.4F * k), argb(0.9F * k, HEART));
				} else {
					o.glint(at, s.camRight, s.camUp, 0.4F, time * 0.2F + i, argb(0.95F * k, 0xFFF6C8));
				}
			}
		});
	}

	/** A heart facing the camera: two lobes and a point. */
	static void heart(final VfxOut o, final Vector3f c, final Vector3f right, final Vector3f up, final float r, final int color) {
		Vector3f l = new Vector3f(right).mul(-0.5F * r).add(new Vector3f(up).mul(0.3F * r)).add(c);
		Vector3f rr = new Vector3f(right).mul(0.5F * r).add(new Vector3f(up).mul(0.3F * r)).add(c);
		o.sprite(l, right, up, 0.62F * r, color);
		o.sprite(rr, right, up, 0.62F * r, color);
		Vector3f a = new Vector3f(right).mul(-1.02F * r).add(new Vector3f(up).mul(0.18F * r)).add(c);
		Vector3f b = new Vector3f(right).mul(1.02F * r).add(new Vector3f(up).mul(0.18F * r)).add(c);
		Vector3f tip = new Vector3f(up).mul(-1.0F * r).add(c);
		o.tri(a, b, tip, color, color, color);
	}

	/**
	 * A crescent across the flight (the shape of Getsuga Tensho, smaller or larger): Artemis's silver moon riding each arrow, or
	 * Merodach's crimson wave with its gold edge ({@code shaded}: a dark red body under the glow so it reads by day).
	 */
	private static void crescent(final State s, final PoseStack ps, final SubmitNodeCollector c, final float radius, final float thickness, final int body,
		final int rim, final boolean shaded) {
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		Vector3f up = new Vector3f(side).cross(f).normalize();
		// a moon lies on its back (horns up); the wave stands leaned like a swing
		Vector3f axis = shaded ? new Vector3f(side).mul(Mth.cos(s.tilt)).add(new Vector3f(up).mul(Mth.sin(s.tilt))) : new Vector3f(up);
		if (shaded && s.mirror) {
			axis.negate();
		}
		float grow = Math.min(1.0F, 0.5F + s.ageInTicks * 0.25F);
		float time = s.ageInTicks;
		c.submitCustomGeometry(ps, VfxDraw.GLOW, (pose, buffer) -> {
			for (int echo = 2; echo >= 1; echo--) {
				Vector3f shift = new Vector3f(f).mul(-echo * 0.5F * radius);
				SkillProjectileRenderer.crescent(pose, buffer, f, axis, shift, radius * grow * (1 - 0.08F * echo), thickness * grow, body, body,
					echo == 1 ? 0.35F : 0.15F, time, 0.0F);
			}
			SkillProjectileRenderer.crescent(pose, buffer, f, axis, new Vector3f(), radius * grow, thickness * grow, body, rim, 1.0F, time, shaded ? 0.55F : 1.0F);
		});
		if (shaded) {
			c.submitCustomGeometry(ps, VfxDraw.SHADE, (pose, buffer) -> SkillProjectileRenderer.body(pose, buffer, f, axis, radius * grow * 0.95F,
				thickness * grow * 0.8F, time, 0x3A0408));
			// golden glyph sparks scattering off the edge
			glow(ps, c, o -> {
				for (int i = 0; i < 6; i++) {
					float a = (hash(s.seed + i) - 0.5F) * 2.2F;
					Vector3f at = new Vector3f(axis).mul(-radius * Mth.cos(a) * 0.9F + radius * 0.5F).add(new Vector3f(f).cross(axis).mul(radius * Mth.sin(a)))
						.add(new Vector3f(f).mul(-0.6F - hash(s.seed * 3 + i)));
					o.glint(at, s.camRight, s.camUp, 0.25F, time * 0.3F + i, argb(0.8F, GOLD));
				}
			});
		} else {
			glow(ps, c, o -> o.sprite(new Vector3f(), s.camRight, s.camUp, 0.25F, argb(0.9F, 0xFFFFFF)));
		}
	}

	// ------------------------------------------------------------ Arjuna

	/** Agni Gandiva: the arrow wreathed in flame, white at the head, tongues streaming back. */
	private static void flameArrow(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f back = new Vector3f(s.forward).negate();
		float tail = trail(s, 3.0F);
		float time = s.ageInTicks;
		glow(ps, c, o -> {
			Vector3f head = new Vector3f(s.forward).mul(0.3F);
			o.sprite(head, s.camRight, s.camUp, 0.9F, argb(0.5F, 0xFF6A1A));
			o.sprite(head, s.camRight, s.camUp, 0.35F, argb(1.0F, 0xFFF4D0));
			o.glowLine(head, new Vector3f(back).mul(tail), 0.35F, argb(0.6F, 0xFF9A3A), argb(0.0F, 0xFF3A00));
			Vector3f side = VfxOut.across(back);
			Vector3f up = new Vector3f(side).cross(back).normalize();
			if (tail > 0.3F) {
				for (int i = 0; i < 6; i++) {
					float a = Mth.TWO_PI * i / 6 + time * 0.5F;
					Vector3f off = new Vector3f(side).mul(Mth.cos(a) * 0.12F).add(new Vector3f(up).mul(Mth.sin(a) * 0.12F));
					o.flame(off, back, Math.min(tail, 1.4F + 1.2F * hash(s.seed + i)), 0.32F, time * 0.9F + i * 1.9F, argb(0.85F, 0xFFD27A), argb(0.0F, 0xFF2A00));
				}
			}
			// Gandiva's blue at the heart of the fire
			o.sprite(head, s.camRight, s.camUp, 0.55F, argb(0.35F, s.color));
		});
	}

	// ------------------------------------------------------------ Ishtar

	/** Gem Shot: a cut gem (crown and pavilion facets) turning as it flies, a glint off it and a short coloured trail. */
	private static void gem(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		int color = GEMS[Math.floorMod(s.seed, GEMS.length)];
		float spin = s.ageInTicks * 0.35F;
		float r = 0.3F;
		int sides = 8;
		Vector3f[] girdle = new Vector3f[sides];
		Vector3f[] table = new Vector3f[sides];
		for (int i = 0; i < sides; i++) {
			float a = spin + Mth.TWO_PI * i / sides;
			girdle[i] = new Vector3f(Mth.cos(a) * r, 0.0F, Mth.sin(a) * r);
			table[i] = new Vector3f(Mth.cos(a + Mth.PI / sides) * r * 0.55F, r * 0.45F, Mth.sin(a + Mth.PI / sides) * r * 0.55F);
		}
		Vector3f culet = new Vector3f(0, -r * 1.1F, 0);
		Vector3f top = new Vector3f(0, r * 0.45F, 0);
		ps.pushPose();
		ps.translate(0.0F, 0.15F, 0.0F);
		shade(ps, c, o -> {
			for (int i = 0; i < sides; i++) {
				int j = (i + 1) % sides;
				// facets lit from above: crown bright, pavilion darker, every other one a shade off
				int light = VfxDraw.whiten(color, i % 2 == 0 ? 0.35F : 0.15F);
				int dark = VfxOut.lerp(color | 0xFF000000, 0xFF000000, i % 2 == 0 ? 0.25F : 0.45F) & 0xFFFFFF;
				o.tri(girdle[i], girdle[j], table[i], argb(0.85F, light), argb(0.85F, light), argb(0.85F, VfxDraw.whiten(color, 0.5F)));
				o.tri(table[i], girdle[j], table[j], argb(0.85F, VfxDraw.whiten(color, 0.5F)), argb(0.85F, light), argb(0.85F, light));
				o.tri(top, table[i], table[j], argb(0.8F, VfxDraw.whiten(color, 0.6F)), argb(0.8F, VfxDraw.whiten(color, 0.6F)), argb(0.8F, VfxDraw.whiten(color, 0.6F)));
				o.tri(girdle[i], culet, girdle[j], argb(0.9F, dark), argb(0.9F, dark), argb(0.9F, dark));
			}
		});
		Vector3f back = new Vector3f(s.forward).negate();
		float tail = trail(s, 2.2F);
		glow(ps, c, o -> {
			o.sprite(new Vector3f(), s.camRight, s.camUp, 0.75F, argb(0.45F, color));
			for (int i = 0; i < sides; i++) {
				o.bar(girdle[i], girdle[(i + 1) % sides], 0.012F, 0.012F, argb(0.9F, VfxDraw.whiten(color, 0.6F)), argb(0.9F, VfxDraw.whiten(color, 0.6F)));
			}
			o.glint(new Vector3f(s.camRight).mul(0.12F).add(0, 0.2F, 0), s.camRight, s.camUp, 0.45F + 0.15F * Mth.sin(s.ageInTicks * 1.7F), 0.4F,
				argb(0.95F, 0xFFFFFF));
			o.glowLine(new Vector3f(), new Vector3f(back).mul(tail), 0.2F, argb(0.6F, color), argb(0.0F, color));
			for (int i = 0; i < 3; i++) {
				float d = 0.6F + i * 0.55F;
				if (d < tail) {
					o.glint(new Vector3f(back).mul(d).add(0, 0.15F * Mth.sin(s.ageInTicks + i * 2), 0), s.camRight, s.camUp, 0.18F, i, argb(0.8F, GOLD));
				}
			}
		});
		ps.popPose();
	}

	// ------------------------------------------------------------ Gilgamesh

	/** A treasure of the Gate in flight: a golden streak behind it, a glow round it and gold dust falling off. */
	private static void treasure(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f back = new Vector3f(s.forward).negate();
		float tail = trail(s, 3.5F);
		glow(ps, c, o -> {
			Vector3f end = new Vector3f(back).mul(tail);
			o.glowLine(new Vector3f(), end, 0.4F, argb(0.45F, GOLD), argb(0.0F, GOLD));
			o.glowLine(new Vector3f(), end, 0.08F, argb(0.95F, 0xFFF6D0), argb(0.0F, 0xFFE07A));
			o.sprite(new Vector3f(), s.camRight, s.camUp, 0.6F, argb(0.35F, GOLD));
			for (int i = 0; i < 4; i++) {
				float d = 0.5F + 0.8F * i + 0.3F * hash(s.seed + i);
				if (d < tail) {
					o.glint(new Vector3f(back).mul(d).add(0, -0.1F * i, 0), s.camRight, s.camUp, 0.2F, i, argb(0.8F * (1 - d / 4), 0xFFF0A0));
				}
			}
		});
	}

	// ------------------------------------------------------------ Pashupata

	/** Divine Discus: a flat disc of light spinning round the chakram, toothed at its rim, two fainter discs left behind it. */
	private static void discus(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float spin = s.ageInTicks * 1.4F;
		Vector3f back = new Vector3f(s.forward).negate();
		float tail = trail(s, 1.6F);
		glow(ps, c, o -> {
			for (int echo = 0; echo < 3; echo++) {
				float d = echo * 0.8F;
				if (echo > 0 && d > tail) {
					break;
				}
				float a = echo == 0 ? 1.0F : 0.35F / echo;
				Vector3f at = new Vector3f(back).mul(d);
				disc(o, at, 0.36F, spin - echo * 0.6F, a, s.color);
			}
			o.sprite(new Vector3f(), s.camRight, s.camUp, 0.35F, argb(0.9F, 0xFFFFFF));
		});
	}

	/** A flat spinning disc of light at {@code c}: rim, spokes and teeth (shared with the great discus of Pashupata). */
	static void disc(final VfxOut o, final Vector3f c, final float r, final float spin, final float alpha, final int color) {
		disc(o, c, new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), r, spin, alpha, color);
	}

	/** {@link #disc} in the plane of {@code x} and {@code z} (unit vectors, square to each other). */
	static void disc(final VfxOut o, final Vector3f c, final Vector3f x, final Vector3f z, final float r, final float spin, final float alpha, final int color) {
		int rim = argb(0.9F * alpha, VfxDraw.whiten(color, 0.4F));
		o.circle(c, x, z, r, 0.035F * Math.max(1.0F, r), 32, rim);
		o.circle(c, x, z, r * 0.55F, 0.025F * Math.max(1.0F, r), 24, argb(0.7F * alpha, color));
		int teeth = 12;
		for (int i = 0; i < teeth; i++) {
			float a = spin + Mth.TWO_PI * i / teeth;
			Vector3f p0 = on(c, x, z, a, r);
			Vector3f p1 = on(c, x, z, a + 0.32F, r * 1.22F);
			Vector3f p2 = on(c, x, z, a + 0.45F, r);
			o.tri(p0, p1, p2, rim, argb(0.0F, color), rim);
			if (i % 3 == 0) {
				o.bar(on(c, x, z, a, r * 0.15F), on(c, x, z, a, r * 0.95F), 0.02F * Math.max(1.0F, r), 0.02F * Math.max(1.0F, r),
					argb(0.8F * alpha, 0xFFFFFF), argb(0.5F * alpha, color));
			}
		}
		// the blur of its spin
		o.sprite(c, x, z, r * 1.05F, argb(0.3F * alpha, color));
	}

	private static Vector3f on(final Vector3f c, final Vector3f x, final Vector3f z, final float a, final float r) {
		return new Vector3f(x).mul(Mth.cos(a) * r).add(new Vector3f(z).mul(Mth.sin(a) * r)).add(c);
	}

	private ArrowLooks() {
	}
}
