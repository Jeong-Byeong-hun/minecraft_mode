package com.minecraftmode.client.vfx;

import static com.minecraftmode.client.vfx.SkillVfxRenderer.glow;
import static com.minecraftmode.client.vfx.SkillVfxRenderer.shade;
import static com.minecraftmode.client.vfx.VfxOut.argb;
import static com.minecraftmode.client.vfx.VfxOut.ease;
import static com.minecraftmode.client.vfx.VfxOut.hash;

import com.minecraftmode.client.vfx.SkillVfxRenderer.State;
import com.minecraftmode.job.skill.SkillLook;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Blade looks: bankai speed's afterimages, slashes and the whip, Zanka no Tachi's circle, Kamishini no Yari's line, Hakuteiken,
 * Mugetsu's wall of darkness and Zabimaru's bone snake.
 */
final class BladeLooks {
	private static final int DARK_RIM = 0xE0242E;
	private static final int BONE = 0xEFE6D2;
	private static final int BONE_GLOW = 0xFF3030;

	// ------------------------------------------------------------ flash step

	/** Afterimages along the caster's path: a fading sheet of their height, ghost figures and speed lines. */
	static void flashTrail(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		List<Vector3f> points = VfxOut.path(s.trail);
		if (points.size() < 2) {
			return;
		}
		float fade = 1.0F - ease(s.progress, 0.25F, 1.0F);
		boolean dark = s.dark();
		int tone = dark ? DARK_RIM : s.color;
		int n = points.size();
		// the last part (where the caster is now) is left out in first person: it would be drawn through the camera
		int last = s.selfView ? Math.max(1, n - 2) : n - 1;
		if (dark) {
			shade(ps, c, o -> {
				for (int i = 0; i < last; i++) {
					float a0 = 0.75F * fade * (0.25F + 0.75F * i / (n - 1));
					float a1 = 0.75F * fade * (0.25F + 0.75F * (i + 1) / (n - 1));
					sheet(o, points.get(i), points.get(i + 1), 0.15F, 1.75F, argb(a0, 0x0A0306), argb(a1, 0x0A0306));
				}
			});
		}
		glow(ps, c, o -> {
			for (int i = 0; i < last; i++) {
				float k0 = (float)i / (n - 1);
				float k1 = (float)(i + 1) / (n - 1);
				float a0 = fade * (0.15F + 0.5F * k0);
				float a1 = fade * (0.15F + 0.5F * k1);
				Vector3f p0 = points.get(i);
				Vector3f p1 = points.get(i + 1);
				if (!dark) {
					sheet(o, p0, p1, 0.15F, 1.75F, argb(a0 * 0.45F, s.color), argb(a1 * 0.45F, s.color));
				}
				// bright edges top and bottom, a white streak through the middle
				o.glowLine(new Vector3f(p0).add(0, 1.75F, 0), new Vector3f(p1).add(0, 1.75F, 0), 0.12F, argb(a0, tone), argb(a1, tone));
				o.glowLine(new Vector3f(p0).add(0, 0.15F, 0), new Vector3f(p1).add(0, 0.15F, 0), 0.12F, argb(a0, tone), argb(a1, tone));
				o.glowLine(new Vector3f(p0).add(0, 1.0F, 0), new Vector3f(p1).add(0, 1.0F, 0), 0.05F, argb(a0, 0xFFFFFF), argb(a1, 0xFFFFFF));
			}
			// speed lines either side of the path
			Vector3f start = points.getFirst();
			Vector3f end = points.get(last);
			Vector3f dir = new Vector3f(end).sub(start);
			float length = dir.length();
			if (length > 0.5F) {
				Vector3f side = VfxOut.across(dir);
				for (int i = 0; i < 10; i++) {
					float h = 0.2F + 1.6F * hash(s.seed * 31 + i);
					float off = (hash(s.seed * 17 + i) - 0.5F) * 2.4F;
					float from = hash(s.seed * 7 + i) * 0.5F;
					float to = Math.min(1.0F, from + 0.3F + 0.4F * hash(s.seed * 3 + i));
					Vector3f a = new Vector3f(dir).mul(from).add(start).add(new Vector3f(side).mul(off)).add(0, h, 0);
					Vector3f b = new Vector3f(dir).mul(to).add(start).add(new Vector3f(side).mul(off)).add(0, h, 0);
					o.glowLine(a, b, 0.04F, argb(0.0F, tone), argb(0.8F * fade, VfxDraw.whiten(tone, 0.5F)));
				}
			}
			// ghost figures where the caster passed
			for (int g = 1; g <= 3; g++) {
				Vector3f at = VfxOut.along(points, last, g / 4.0F);
				float a = fade * (0.2F + 0.15F * g);
				ghost(o, s, at, argb(a, tone), argb(a * 0.8F, VfxDraw.whiten(tone, 0.4F)));
			}
		});
	}

	/** An upright sheet between two points from {@code y0} to {@code y1}. */
	private static void sheet(final VfxOut o, final Vector3f a, final Vector3f b, final float y0, final float y1, final int ca, final int cb) {
		o.quad(new Vector3f(a).add(0, y0, 0), new Vector3f(b).add(0, y0, 0), new Vector3f(b).add(0, y1, 0), new Vector3f(a).add(0, y1, 0), ca, cb, cb, ca);
	}

	/** A soft figure facing the camera: body and head. */
	private static void ghost(final VfxOut o, final State s, final Vector3f feet, final int body, final int head) {
		Vector3f right = new Vector3f(s.camRight).mul(0.38F);
		Vector3f up = new Vector3f(s.camUp).mul(0.75F);
		o.sprite(new Vector3f(feet).add(0, 0.85F, 0), right, up, 1.0F, body);
		o.sprite(new Vector3f(feet).add(0, 1.65F, 0), s.camRight, s.camUp, 0.26F, head);
	}

	// ------------------------------------------------------------ slashes

	/** One sweeping arc in front of the caster, leaning a little one way or the other. */
	static void slash(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float lean = ((s.seed & 1) == 0 ? 1 : -1) * (0.18F + 0.2F * hash(s.seed));
		arc(s, ps, c, s.progress, lean, s.arg, s.size, 1.0F);
	}

	/** Two arcs crossing in an X, the second a moment later. */
	static void crossSlash(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		arc(s, ps, c, s.progress, 0.75F, s.arg, s.size, 1.0F);
		float late = Mth.clamp((s.age() - 2.0F) / (s.life - 2.0F), 0.0F, 1.0F);
		if (late > 0.0F) {
			arc(s, ps, c, late, -0.75F, s.arg, s.size, -1.0F);
		}
	}

	/** Zanka no Tachi: a white-hot circle cut all the way round, a ring of fire left where it passed, a glowing ground. */
	static void zanka(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.5F, 1.0F);
		ps.pushPose();
		ps.translate(0.0F, 1.0F, 0.0F);
		arc(s, ps, c, Math.min(1.0F, p * 1.3F), 0.0F, 360.0F, s.size, 1.0F, 0xFFF2C8, 0xFF7A1A);
		ps.popPose();
		float rise = ease(p, 0.15F, 0.45F);
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			o.ring(ground, s.size * 0.85F, s.size * 1.05F, argb(0.8F * fade, 0xFF5A1A));
			o.disc(ground, s.size, argb(0.35F * fade, 0xFF9A3A), argb(0.0F, 0xFF3A00));
			int n = 36;
			for (int i = 0; i < n; i++) {
				float a = Mth.TWO_PI * i / n;
				float h = (1.0F + 1.6F * hash(s.seed + i)) * rise * fade;
				if (h > 0.05F) {
					Vector3f base = VfxOut.polar(a, s.size * (0.92F + 0.12F * hash(i * 3)), 0.05F);
					o.flame(base, new Vector3f(0, 1, 0), h, 0.55F, s.ageInTicks * 0.6F + i, argb(0.9F * fade, 0xFFD27A), argb(0.0F, 0xFF3A00));
				}
			}
		});
	}

	private static void arc(final State s, final PoseStack ps, final SubmitNodeCollector c, final float p, final float lean, final float arcDeg, final float radius,
		final float direction) {
		boolean dark = s.dark();
		int body = dark ? DARK_RIM : s.color;
		arc(s, ps, c, p, lean, arcDeg, radius, direction, dark ? 0xFF8A80 : VfxDraw.whiten(s.color, 0.8F), body);
	}

	/**
	 * A blade arc: the edge sweeps across {@code arcDeg} (in front of the caster, tilted {@code lean} radians round the view) in the first
	 * part of {@code p} and leaves a crescent band behind it that fades from the start of the swing; {@code direction} picks the side it
	 * starts on.
	 */
	private static void arc(final State s, final PoseStack ps, final SubmitNodeCollector c, final float p, final float lean, final float arcDeg, final float radius,
		final float direction, final int rim, final int body) {
		float half = arcDeg * Mth.DEG_TO_RAD / 2;
		float sweep = ease(p, 0.0F, 0.4F);
		float fade = 1.0F - ease(p, 0.35F, 1.0F);
		if (fade <= 0.01F) {
			return;
		}
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		Vector3f up = new Vector3f(side).cross(f).normalize();
		Vector3f plane = new Vector3f(side).mul(Mth.cos(lean)).add(new Vector3f(up).mul(Mth.sin(lean)));
		int steps = Mth.clamp((int)(arcDeg / 7), 10, 52);
		float lead = -half + 2 * half * sweep;
		boolean dark = s.dark();
		if (dark) {
			shade(ps, c, o -> band(o, f, plane, steps, half, lead, radius, direction, 0x12060A, 0x12060A, 0.85F * fade, 0.0F));
		}
		glow(ps, c, o -> {
			band(o, f, plane, steps, half, lead, radius, direction, body, body, 0.8F * fade * (dark ? 0.6F : 1.0F), 0.0F);
			band(o, f, plane, steps, half, lead, radius, direction, body, body, 0.6F * fade, 2.0F);
			band(o, f, plane, steps, half, lead, radius, direction, rim, rim, fade, 1.0F);
			// the edge itself, brightest at the leading line
			if (sweep < 1.0F) {
				Vector3f in = point(f, plane, lead * direction, radius * 0.7F);
				Vector3f out = point(f, plane, lead * direction, radius * 1.02F);
				o.glowLine(in, out, 0.07F, argb(0.0F, rim), argb(fade, 0xFFFFFF));
			}
		});
	}

	/**
	 * The swept crescent from the start of the arc to {@code lead}: thickest in the middle of its sweep, from {@code radius} inward;
	 * {@code rimOnly} 1 draws just its outer edge as a bright line, 2 the glow spilling past it.
	 */
	private static void band(final VfxOut o, final Vector3f f, final Vector3f plane, final int steps, final float half, final float lead, final float radius,
		final float direction, final int inner, final int outer, final float alpha, final float rimOnly) {
		float span = lead + half;
		if (span <= 0.0F) {
			return;
		}
		for (int i = 0; i < steps; i++) {
			float t0 = -half + span * i / steps;
			float t1 = -half + span * (i + 1) / steps;
			// fades in from the start of the swing, full at the leading edge
			float k0 = (float)i / steps;
			float k1 = (float)(i + 1) / steps;
			float a0 = alpha * k0 * k0;
			float a1 = alpha * k1 * k1;
			// a full circle (Zanka no Tachi) keeps a thin blade trail instead of filling the disc
			float thick = half > 3.0F ? 0.3F : 1.0F;
			float w0 = thick * (0.08F + 0.26F * Mth.sin(Mth.PI * Mth.clamp((t0 + half) / (2 * half), 0, 1)));
			float w1 = thick * (0.08F + 0.26F * Mth.sin(Mth.PI * Mth.clamp((t1 + half) / (2 * half), 0, 1)));
			if (rimOnly > 1.5F) {
				// the glow spilling past the edge
				o.quad(point(f, plane, t0 * direction, radius), point(f, plane, t0 * direction, radius + 0.35F), point(f, plane, t1 * direction, radius + 0.35F),
					point(f, plane, t1 * direction, radius), argb(a0, outer), argb(0.0F, outer), argb(0.0F, outer), argb(a1, outer));
			} else if (rimOnly > 0.0F) {
				o.quad(point(f, plane, t0 * direction, radius - 0.07F), point(f, plane, t0 * direction, radius), point(f, plane, t1 * direction, radius),
					point(f, plane, t1 * direction, radius - 0.07F), argb(a0, outer), argb(a0, outer), argb(a1, outer), argb(a1, outer));
			} else {
				o.quad(point(f, plane, t0 * direction, radius * (1 - w0)), point(f, plane, t0 * direction, radius),
					point(f, plane, t1 * direction, radius), point(f, plane, t1 * direction, radius * (1 - w1)),
					argb(0.0F, inner), argb(a0, outer), argb(a1, outer), argb(0.0F, inner));
			}
		}
	}

	private static Vector3f point(final Vector3f f, final Vector3f plane, final float theta, final float r) {
		return new Vector3f(f).mul(Mth.cos(theta) * r).add(new Vector3f(plane).mul(Mth.sin(theta) * r));
	}

	/** Zabimaru's whip: a chain of bone blades swung across the arc, the tip trailing, a red streak behind. */
	static void boneLash(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float half = s.arg * Mth.DEG_TO_RAD / 2;
		float sweep = ease(p, 0.0F, 0.55F);
		float fade = 1.0F - ease(p, 0.6F, 1.0F);
		Vector3f f = new Vector3f(s.forward);
		Vector3f plane = VfxOut.across(f);
		float lead = -half + 2 * half * sweep;
		int n = 12;
		Vector3f[] joints = new Vector3f[n + 1];
		for (int j = 0; j <= n; j++) {
			float k = (float)j / n;
			float theta = lead - 0.9F * k * k * (1.0F - sweep * 0.6F);
			// the whip sags and ripples a little along its length
			joints[j] = point(f, plane, theta, s.size * (0.1F + 0.9F * k)).add(0, -0.25F * k + 0.12F * Mth.sin(k * 9.0F - s.ageInTicks * 1.4F), 0);
		}
		shade(ps, c, o -> {
			for (int j = 0; j < n; j++) {
				Vector3f a = joints[j];
				Vector3f b = joints[j + 1];
				Vector3f mid = new Vector3f(a).lerp(b, 0.5F);
				float w = 0.16F * (1.0F - 0.5F * j / n);
				o.bar(new Vector3f(a).lerp(b, 0.08F), new Vector3f(a).lerp(b, 0.92F), w, w * 0.7F, argb(0.95F * fade, BONE), argb(0.95F * fade, 0xD8CBB0));
				// a spike on each segment
				Vector3f out = new Vector3f(b).sub(a).cross(0, 1, 0).normalize(w * 2.2F);
				o.tri(new Vector3f(mid).add(new Vector3f(b).sub(a).mul(0.25F)), new Vector3f(mid).sub(new Vector3f(b).sub(a).mul(0.15F)), new Vector3f(mid).add(out),
					argb(0.95F * fade, BONE), argb(0.95F * fade, BONE), argb(0.95F * fade, 0xFFFFFF));
			}
		});
		glow(ps, c, o -> {
			for (int j = 0; j < n; j++) {
				o.glowLine(joints[j], joints[j + 1], 0.22F, argb(0.6F * fade, BONE_GLOW), argb(0.6F * fade, BONE_GLOW));
			}
			// the swept streak the tip leaves
			Vector3f up = new Vector3f(0, 1, 0);
			band(o, f, new Vector3f(plane).add(new Vector3f(up).mul(-0.05F)), 30, half, lead, s.size, 1.0F, BONE_GLOW, 0xFF6A50, 0.45F * fade, 0.0F);
		});
	}

	// ------------------------------------------------------------ beams

	/**
	 * Kamishini no Yari: a hair-thin blade shooting out to the beam's full length and drawing back. Korose adds the end of the line
	 * dissolving: shards of decay flying apart from where it stops.
	 */
	static void spear(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		boolean kill = s.look == SkillLook.SPEAR_KILL;
		float out = ease(p, 0.0F, kill ? 0.18F : 0.3F);
		float back = kill ? ease(p, 0.5F, 0.8F) : ease(p, 0.6F, 1.0F);
		float extend = out * (1.0F - back);
		Vector3f dir = new Vector3f(s.reach).normalize();
		float length = s.size;
		Vector3f start = new Vector3f(dir).mul(0.6F);
		Vector3f tip = new Vector3f(dir).mul(Math.max(0.7F, length * extend));
		Vector3f end = new Vector3f(dir).mul(length);
		glow(ps, c, o -> {
			if (extend > 0.01F) {
				o.glowLine(start, tip, kill ? 0.32F : 0.2F, argb(0.5F, s.color), argb(0.9F, s.color));
				o.bar(start, tip, 0.025F, 0.02F, argb(1.0F, 0xFFFFFF), argb(1.0F, 0xFFFFFF));
				Vector3f side = VfxOut.across(dir);
				o.blade(new Vector3f(tip).sub(new Vector3f(dir).mul(0.7F)), new Vector3f(tip).add(new Vector3f(dir).mul(0.25F)), side, 0.09F, argb(0.9F, s.color),
					argb(1.0F, 0xFFFFFF));
				// a ring where it leaves the hand
				o.sprite(start, s.camRight, s.camUp, 0.35F * (1.0F - p), argb(0.8F, 0xFFFFFF));
			}
			if (kill && p > 0.12F) {
				float q = ease(p, 0.12F, 1.0F);
				float fade = 1.0F - q;
				o.sprite(end, s.camRight, s.camUp, 1.0F + 4.0F * q, argb(0.75F * fade, 0xB01040));
				o.ring(new Vector3f(end).add(0, -0.5F, 0), 4.5F * q, 4.5F * q + 0.4F, argb(0.7F * fade, 0xFF5A8A));
				o.sprite(end, s.camRight, s.camUp, 0.5F + 1.2F * q, argb(0.9F * fade, 0xFFD0E0));
				for (int i = 0; i < 28; i++) {
					Vector3f d = shardDir(s.seed, i);
					Vector3f at = new Vector3f(d).mul(0.4F + 4.5F * q).add(end);
					o.diamond(at, d, VfxOut.across(d), 0.9F * fade + 0.2F, 0.24F, argb(0.2F * fade, 0x8A1030), argb(0.95F * fade, 0xFF5A8A));
				}
			}
		});
	}

	/** A steady random direction for shard {@code i}. */
	static Vector3f shardDir(final int seed, final int i) {
		float yaw = Mth.TWO_PI * hash(seed * 13 + i);
		float pitch = (hash(seed * 29 + i * 7) - 0.4F) * 2.4F;
		return new Vector3f(Mth.cos(yaw) * Mth.cos(pitch), Mth.sin(pitch), Mth.sin(yaw) * Mth.cos(pitch)).normalize();
	}

	/** Shukei Hakuteiken: one long white blade of light thrust out, wings of light spread behind the hilt. */
	static void hakuteiken(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float extend = ease(p, 0.0F, 0.3F);
		float fade = 1.0F - ease(p, 0.55F, 1.0F);
		Vector3f dir = new Vector3f(s.reach).normalize();
		Vector3f side = VfxOut.across(dir);
		Vector3f up = new Vector3f(side).cross(dir).normalize();
		Vector3f base = new Vector3f(dir).mul(0.4F);
		Vector3f tip = new Vector3f(dir).mul(Math.max(1.0F, s.size * extend));
		int pink = s.color;
		glow(ps, c, o -> {
			o.glowLine(base, tip, 0.7F, argb(0.3F * fade, pink), argb(0.2F * fade, pink));
			o.blade(base, tip, side, 0.32F, argb(0.7F * fade, 0xFFFFFF), argb(0.6F * fade, 0xFFE0EC));
			o.blade(base, tip, up, 0.32F, argb(0.45F * fade, 0xFFFFFF), argb(0.4F * fade, 0xFFE0EC));
			o.bar(base, tip, 0.06F, 0.02F, argb(fade, 0xFFFFFF), argb(fade, 0xFFFFFF));
			// wings of light behind the hilt: a fan of long feathers each side
			float spread = ease(p, 0.05F, 0.4F);
			Vector3f root = new Vector3f(dir).mul(-0.9F).add(new Vector3f(up).mul(0.3F));
			for (int w = -1; w <= 1; w += 2) {
				for (int k = 0; k < 7; k++) {
					float a = (-0.25F + 1.35F * k / 6.0F) * spread;
					Vector3f fd = new Vector3f(side).mul(w * Mth.cos(a)).add(new Vector3f(up).mul(Mth.sin(a))).add(new Vector3f(dir).mul(-0.35F)).normalize();
					float len = (1.4F + 1.4F * Mth.sin(Mth.PI * (k + 0.5F) / 7.0F)) * (0.4F + 0.6F * spread);
					Vector3f from = new Vector3f(fd).mul(0.25F).add(root);
					o.blade(from, new Vector3f(fd).mul(len).add(from), dir, 0.16F, argb(0.8F * fade, 0xFFFFFF), argb(0.1F * fade, pink));
				}
			}
			o.sprite(root, s.camRight, s.camUp, 1.4F * spread, argb(0.5F * fade, pink));
		});
	}

	/**
	 * Mugetsu: a towering crescent wall of darkness running out along the beam, its rim burning a deep red, black flames on its crest,
	 * and the dark it leaves along the ground.
	 */
	static void mugetsu(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float run = ease(p, 0.0F, 0.55F);
		float fade = 1.0F - ease(p, 0.6F, 1.0F);
		float grow = ease(p, 0.0F, 0.2F);
		Vector3f dir = new Vector3f(s.reach).normalize();
		Vector3f flat = new Vector3f(dir.x, 0, dir.z);
		if (flat.lengthSquared() < 1.0E-4F) {
			flat.set(s.forward);
		}
		flat.normalize();
		Vector3f side = VfxOut.across(flat);
		Vector3f front = new Vector3f(dir).mul(2.0F + (s.size - 2.0F) * run);
		float width = 7.0F * grow;
		float height = 12.0F * grow;
		int n = 16;
		Vector3f[] lowEdge = new Vector3f[n + 1];
		Vector3f[] topEdge = new Vector3f[n + 1];
		for (int i = 0; i <= n; i++) {
			float k = (float)i / n * 2 - 1;
			float bend = k * k * 3.0F;
			Vector3f foot = new Vector3f(side).mul(k * width).add(new Vector3f(flat).mul(-bend)).add(front).add(0, -1.6F, 0);
			lowEdge[i] = foot;
			topEdge[i] = new Vector3f(foot).add(0, height * (float)Math.sqrt(Math.max(0.0F, 1 - k * k)) + 0.2F, 0);
		}
		shade(ps, c, o -> {
			int black = argb(0.92F * fade, 0x050208);
			for (int i = 0; i < n; i++) {
				o.quad(lowEdge[i], lowEdge[i + 1], topEdge[i + 1], topEdge[i], black);
			}
			// the darkness it leaves along the way, thinning out behind it
			Vector3f a = new Vector3f(flat).mul(1.0F).add(0, -1.5F, 0);
			Vector3f b = new Vector3f(front).add(0, -1.5F, 0);
			for (int w = -1; w <= 1; w += 2) {
				Vector3f off = new Vector3f(side).mul(w * width * 0.7F);
				o.quad(a, b, new Vector3f(b).add(off), new Vector3f(a).add(new Vector3f(off).mul(0.3F)), argb(0.0F, 0x050208), argb(0.6F * fade, 0x050208),
					argb(0.0F, 0x050208), argb(0.0F, 0x050208));
			}
			o.quad(a, b, new Vector3f(b).add(0, height * 0.6F, 0), new Vector3f(a).add(0, 0.3F, 0), argb(0.0F, 0x050208), argb(0.55F * fade, 0x050208),
				argb(0.0F, 0x050208), argb(0.0F, 0x050208));
			// black flames licking up from the crest
			for (int i = 0; i <= n; i++) {
				o.flame(topEdge[i], new Vector3f(0, 1, 0).add(new Vector3f(flat).mul(-0.4F)), 1.8F + 2.4F * hash(s.seed + i), 1.4F, s.ageInTicks * 0.5F + i,
					argb(0.9F * fade, 0x050208), argb(0.0F, 0x050208));
			}
		});
		glow(ps, c, o -> {
			for (int i = 0; i < n; i++) {
				o.glowLine(topEdge[i], topEdge[i + 1], 0.5F, argb(0.7F * fade, 0x9A0A1E), argb(0.7F * fade, 0x9A0A1E));
				o.glowLine(topEdge[i], topEdge[i + 1], 0.08F, argb(0.9F * fade, 0xFF6A6A), argb(0.9F * fade, 0xFF6A6A));
			}
			o.glowLine(lowEdge[0], topEdge[0], 0.3F, argb(0.5F * fade, 0x9A0A1E), argb(0.5F * fade, 0x9A0A1E));
			o.glowLine(lowEdge[n], topEdge[n], 0.3F, argb(0.5F * fade, 0x9A0A1E), argb(0.5F * fade, 0x9A0A1E));
		});
	}

	/** Zaga Tesso: Zabimaru's bone snake lunging along the beam, its jaws open, and drawing back. */
	static void boneSnake(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float extend = ease(p, 0.0F, 0.35F) * (1.0F - ease(p, 0.7F, 1.0F));
		float fade = 1.0F - ease(p, 0.85F, 1.0F);
		Vector3f dir = new Vector3f(s.reach).normalize();
		Vector3f side = VfxOut.across(dir);
		Vector3f up = new Vector3f(side).cross(dir).normalize();
		float reach = Math.max(1.2F, s.size * extend);
		int n = 16;
		Vector3f[] spine = new Vector3f[n + 1];
		for (int j = 0; j <= n; j++) {
			float k = (float)j / n;
			float wave = Mth.sin(k * 7.0F - s.ageInTicks * 0.9F) * 0.45F * Mth.sin(Mth.PI * k);
			spine[j] = new Vector3f(dir).mul(0.7F + (reach - 0.7F) * k).add(new Vector3f(side).mul(wave)).add(new Vector3f(up).mul(wave * 0.4F));
		}
		Vector3f head = spine[n];
		Vector3f jawUp = new Vector3f(dir).mul(Mth.cos(0.5F)).add(new Vector3f(up).mul(Mth.sin(0.5F)));
		Vector3f jawDown = new Vector3f(dir).mul(Mth.cos(0.45F)).add(new Vector3f(up).mul(-Mth.sin(0.45F)));
		shade(ps, c, o -> {
			int bone = argb(0.97F * fade, BONE);
			int dim = argb(0.97F * fade, 0xCDBE9F);
			for (int j = 0; j < n; j++) {
				float w = 0.2F * (0.6F + 0.4F * j / n);
				o.bar(new Vector3f(spine[j]).lerp(spine[j + 1], 0.1F), new Vector3f(spine[j]).lerp(spine[j + 1], 0.9F), w, w * 0.8F, bone, dim);
				// a pair of spikes on every other segment
				if (j % 2 == 0) {
					Vector3f mid = new Vector3f(spine[j]).lerp(spine[j + 1], 0.5F);
					for (int sgn = -1; sgn <= 1; sgn += 2) {
						Vector3f out = new Vector3f(side).mul(sgn * 0.45F).add(new Vector3f(up).mul(0.25F)).add(new Vector3f(dir).mul(-0.2F));
						o.tri(new Vector3f(mid).add(new Vector3f(dir).mul(0.12F)), new Vector3f(mid).sub(new Vector3f(dir).mul(0.12F)), new Vector3f(mid).add(out), bone, bone,
							argb(0.97F * fade, 0xFFFFFF));
					}
				}
			}
			// the skull: two jaws, the upper one wider
			o.blade(head, new Vector3f(jawUp).mul(1.1F).add(head), side, 0.38F, bone, argb(0.97F * fade, 0xFFFFFF));
			o.blade(head, new Vector3f(jawDown).mul(0.95F).add(head), side, 0.3F, bone, argb(0.97F * fade, 0xFFFFFF));
		});
		glow(ps, c, o -> {
			for (int j = 0; j < n; j++) {
				o.glowLine(spine[j], spine[j + 1], 0.35F, argb(0.45F * fade, BONE_GLOW), argb(0.45F * fade, BONE_GLOW));
			}
			// red eyes and the glow of the open mouth
			Vector3f eyes = new Vector3f(jawUp).mul(0.35F).add(head).add(new Vector3f(up).mul(0.18F));
			o.sprite(new Vector3f(side).mul(0.18F).add(eyes), s.camRight, s.camUp, 0.16F, argb(fade, 0xFF2020));
			o.sprite(new Vector3f(side).mul(-0.18F).add(eyes), s.camRight, s.camUp, 0.16F, argb(fade, 0xFF2020));
			o.sprite(new Vector3f(dir).mul(0.5F).add(head), s.camRight, s.camUp, 0.7F, argb(0.5F * fade, BONE_GLOW));
		});
	}

	private BladeLooks() {
	}
}
