package com.minecraftmode.client.vfx;

import static com.minecraftmode.client.vfx.SkillVfxRenderer.glow;
import static com.minecraftmode.client.vfx.SkillVfxRenderer.shade;
import static com.minecraftmode.client.vfx.VfxOut.argb;
import static com.minecraftmode.client.vfx.VfxOut.ease;
import static com.minecraftmode.client.vfx.VfxOut.hash;
import static com.minecraftmode.client.vfx.VfxOut.polar;

import com.minecraftmode.client.vfx.SkillVfxRenderer.State;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Looks of the archers of the sky: Chiron (the Scorpion's stars, pankration, arrows turned aside, the centaur's gallop), Apollo
 * (Hekatebolos's sunlight, Achilles' heel, the Paean, the sunlit step) and Artemis (the moon sigil and veil).
 */
final class HeavenLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final int ANTARES = 0xFF3A1E;
	private static final int SUN = 0xFFC93C;
	private static final int SUN_CORE = 0xFFF4C8;
	private static final int MOON = 0xC8D8FF;
	private static final int MOON_DEEP = 0x7F9CFF;
	/** Scorpius, as seen on the sky (x right, y up): claws, head, Antares (index 4), the body and the curled sting. */
	private static final float[][] SCORPIUS = {
		{-0.95F, 0.95F}, {-0.7F, 1.05F}, {-0.82F, 0.68F}, {-0.58F, 0.62F}, {-0.38F, 0.32F}, {-0.24F, 0.12F}, {-0.08F, -0.1F}, {0.05F, -0.35F},
		{0.22F, -0.6F}, {0.45F, -0.78F}, {0.7F, -0.76F}, {0.88F, -0.55F}, {0.9F, -0.3F}, {0.76F, -0.12F}, {0.6F, -0.05F}
	};
	/** Lines between the stars above (index pairs). */
	private static final int[][] SCORPIUS_LINES = {
		{0, 3}, {1, 3}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 7}, {7, 8}, {8, 9}, {9, 10}, {10, 11}, {11, 12}, {12, 13}, {13, 14}
	};

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case ANTARES_MARK -> antaresMark(s, ps, c);
			case PANKRATION -> pankration(s, ps, c);
			case ARROW_WARD -> arrowWard(s, ps, c);
			case GALLOP -> gallop(s, ps, c);
			case SUN_BEAM -> sunBeam(s, ps, c);
			case HEEL_STRIKE -> heelStrike(s, ps, c);
			case PAEAN -> paean(s, ps, c);
			case SUN_STEP -> sunStep(s, ps, c);
			case MOON_SIGIL -> moonSigil(s, ps, c);
			case MOON_VEIL -> moonVeil(s, ps, c);
			default -> {
			}
		}
	}

	// ------------------------------------------------------------ Chiron

	/**
	 * Antares Snipe's mark: the Scorpion drawn star by star over the target (facing the camera), Antares burning red in it, a thread of
	 * red light down to the target and a red ring at its feet.
	 */
	private static void antaresMark(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.85F, 1.0F);
		float draw = ease(s.age(), 0.0F, 10.0F);
		float height = Math.max(1.0F, s.size);
		float width = Math.max(0.5F, s.arg);
		// high enough to clear the mark's own rune over the head
		Vector3f center = new Vector3f(0, height + 2.9F, 0);
		float scale = 1.45F;
		Vector3f[] stars = new Vector3f[SCORPIUS.length];
		for (int i = 0; i < stars.length; i++) {
			stars[i] = new Vector3f(s.camRight).mul(SCORPIUS[i][0] * scale).add(new Vector3f(s.camUp).mul(SCORPIUS[i][1] * scale)).add(center);
		}
		float time = s.ageInTicks;
		glow(ps, c, o -> {
			// the lines drawn in one after another
			for (int k = 0; k < SCORPIUS_LINES.length; k++) {
				float t = Mth.clamp(draw * SCORPIUS_LINES.length - k, 0.0F, 1.0F);
				if (t <= 0.0F) {
					break;
				}
				Vector3f a = stars[SCORPIUS_LINES[k][0]];
				Vector3f b = new Vector3f(a).lerp(stars[SCORPIUS_LINES[k][1]], t);
				o.glowLine(a, b, 0.09F, argb(0.8F * fade, 0xFFB0A0), argb(0.8F * fade, 0xFFB0A0));
				o.bar(a, b, 0.012F, 0.012F, argb(0.9F * fade, 0xFFFFFF), argb(0.9F * fade, 0xFFFFFF));
			}
			for (int i = 0; i < stars.length; i++) {
				float twinkle = 0.75F + 0.25F * Mth.sin(time * 0.7F + i * 1.9F);
				if (i == 4) {
					o.sprite(stars[i], s.camRight, s.camUp, 0.75F * twinkle, argb(0.9F * fade, ANTARES));
					o.glint(stars[i], s.camRight, s.camUp, 1.0F, time * 0.05F, argb(0.95F * fade, 0xFFD0C0));
				} else {
					o.glint(stars[i], s.camRight, s.camUp, 0.4F * twinkle, i, argb(0.9F * fade, 0xFFF2E8));
				}
			}
			o.sprite(center, s.camRight, s.camUp, 1.9F, argb(0.12F * fade, ANTARES));
			// the thread down to the target and the ring at its feet
			o.glowLine(stars[4], new Vector3f(0, height * 0.6F, 0), 0.04F, argb(0.6F * fade, ANTARES), argb(0.0F, ANTARES));
			float r = width * 1.1F + 0.3F;
			o.ring(new Vector3f(0, 0.14F, 0), r, r + 0.18F, argb(0.85F * fade, ANTARES));
			for (int i = 0; i < 4; i++) {
				float a = Mth.HALF_PI * i + time * 0.06F;
				o.bar(polar(a, r + 0.1F, 0.14F), polar(a, r + 0.55F, 0.14F), 0.04F, 0.01F, argb(0.9F * fade, 0xFF7A60), argb(0.0F, ANTARES));
			}
		});
	}

	/**
	 * Pankration: the shock of a bare-handed blow rolling out in front: a flash where it lands, walls of force spreading over the arc,
	 * cracks running through the ground and dust thrown up.
	 */
	private static void pankration(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.45F, 1.0F);
		float half = Math.max(30.0F, s.arg) * Mth.DEG_TO_RAD / 2;
		Vector3f f = new Vector3f(s.forward);
		float r = s.size;
		// drawn from the chest (eye - 0.4): the ground is about 1.2 below
		float ground = -1.18F;
		Vector3f impact = new Vector3f(f).mul(1.1F);
		int tone = s.color;
		glow(ps, c, o -> {
			float flash = 1.0F - ease(p, 0.0F, 0.3F);
			o.star(impact, s.camRight, s.camUp, 1.6F * flash + 0.3F, 8, 0.3F, s.seed, argb(0.95F * flash, 0xFFFFFF), argb(0.0F, tone));
			o.sprite(impact, s.camRight, s.camUp, 1.3F, argb(0.6F * flash, tone));
			// three walls of force, one after another, curving round the arc
			for (int w = 0; w < 3; w++) {
				float q = Mth.clamp(p * 1.6F - w * 0.15F, 0.0F, 1.0F);
				if (q <= 0.0F || q >= 1.0F) {
					continue;
				}
				float rad = 0.8F + (r - 0.8F) * ease(q, 0.0F, 1.0F);
				float a = 0.75F * (1.0F - q) * fade;
				int n = 18;
				for (int i = 0; i < n; i++) {
					float t0 = -half + 2 * half * i / n;
					float t1 = -half + 2 * half * (i + 1) / n;
					Vector3f d0 = new Vector3f(f).rotateY(-t0);
					Vector3f d1 = new Vector3f(f).rotateY(-t1);
					float h0 = 2.0F * Mth.cos(t0 / half * Mth.HALF_PI * 0.9F);
					float h1 = 2.0F * Mth.cos(t1 / half * Mth.HALF_PI * 0.9F);
					Vector3f b0 = new Vector3f(d0).mul(rad).add(0, ground + 0.1F, 0);
					Vector3f b1 = new Vector3f(d1).mul(rad).add(0, ground + 0.1F, 0);
					o.quad(b0, b1, new Vector3f(b1).add(0, h1 + 0.3F, 0), new Vector3f(b0).add(0, h0 + 0.3F, 0), argb(a, tone), argb(a, tone), argb(0.0F, tone),
						argb(0.0F, tone));
				}
			}
			// cracks running out through the ground
			float run = ease(p, 0.0F, 0.35F);
			for (int k = 0; k < 7; k++) {
				float a = -half * 0.9F + 1.8F * half * k / 6.0F + (hash(s.seed + k) - 0.5F) * 0.2F;
				Vector3f dir = new Vector3f(f).rotateY(-a);
				Vector3f side = VfxOut.across(dir);
				Vector3f prev = new Vector3f(f).mul(0.9F).add(0, ground + 0.13F, 0);
				int steps = 5;
				for (int j = 1; j <= steps; j++) {
					float d = (0.9F + (r * (0.7F + 0.3F * hash(k * 5)) - 0.9F) * j / steps) * run;
					Vector3f next = new Vector3f(dir).mul(d).add(new Vector3f(side).mul((hash(s.seed * 7 + k * 11 + j) - 0.5F) * 0.5F)).add(0, ground + 0.13F, 0);
					o.bar(prev, next, 0.04F, 0.025F, argb(0.6F * fade, 0xFFB070), argb(0.4F * fade, tone));
					prev = next;
				}
			}
		});
		shade(ps, c, o -> {
			// dust thrown up along the front
			for (int i = 0; i < 12; i++) {
				float a = -half + 2 * half * hash(s.seed * 3 + i);
				float d = (1.0F + (r - 1.0F) * hash(i * 7)) * ease(p, 0.0F, 0.5F);
				Vector3f at = new Vector3f(f).rotateY(-a).mul(d).add(0, ground + 0.3F + 0.8F * ease(p, 0.0F, 1.0F) * hash(i), 0);
				o.sprite(at, s.camRight, s.camUp, 0.5F + 0.6F * p, argb(0.35F * fade, 0x8A7A62));
			}
		});
	}

	/** Protection from Arrows: arrows of light circling the caster at different heights, a faint dome they turn on. */
	private static void arrowWard(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.12F);
		float fade = 1.0F - ease(p, 0.85F, 1.0F);
		float k = open * fade;
		float r = s.size * (0.6F + 0.4F * open);
		float time = s.ageInTicks;
		int tone = s.color;
		glow(ps, c, o -> {
			int n = 10;
			for (int i = 0; i < n; i++) {
				float dirSign = i % 2 == 0 ? 1.0F : -1.0F;
				float a = Mth.TWO_PI * i / n + time * 0.16F * dirSign;
				float y = 0.5F + 1.3F * hash(i * 3 + 1);
				Vector3f at = polar(a, r, y);
				Vector3f tangent = new Vector3f(-Mth.sin(a), 0, Mth.cos(a)).mul(dirSign);
				Vector3f tail = new Vector3f(tangent).mul(-0.8F).add(at);
				Vector3f tip = new Vector3f(tangent).mul(0.8F).add(at);
				o.glowLine(tail, tip, 0.18F, argb(0.4F * k, tone), argb(0.4F * k, tone));
				o.arrow(tail, tip, UP, 0.2F, argb(0.9F * k, VfxDraw.whiten(tone, 0.5F)), argb(k, 0xFFFFFF));
				// the arc it has just run
				Vector3f prev = at;
				for (int j = 1; j <= 4; j++) {
					Vector3f q = polar(a - 0.12F * j * dirSign, r, y);
					o.bar(prev, q, 0.035F, 0.035F, argb(0.7F * k * (1 - j / 5.0F), tone), argb(0.5F * k * (1 - (j + 1) / 5.0F), tone));
					prev = q;
				}
			}
			if (!s.selfView) {
				// the dome: a few faint rings over the caster
				for (int j = 0; j < 4; j++) {
					float lat = j / 4.0F * 1.2F;
					float y = 0.1F + r * 1.15F * Mth.sin(lat);
					float rr = r * 1.15F * Mth.cos(lat);
					o.ring(new Vector3f(0, y, 0), rr - 0.05F, rr + 0.05F, argb(0.25F * k, tone));
				}
			}
			o.ring(new Vector3f(0, 0.14F, 0), r - 0.1F, r + 0.15F, argb(0.5F * k, tone));
		});
	}

	/** Centaur's Gallop: hoofprints burning along the path, wind streaking past and dust kicked up. */
	private static void gallop(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		List<Vector3f> points = VfxOut.path(s.trail);
		if (points.size() < 2) {
			return;
		}
		float fade = 1.0F - ease(s.progress, 0.3F, 1.0F);
		int n = points.size();
		int last = s.selfView ? Math.max(1, n - 2) : n - 1;
		float length = VfxOut.length(points, n - 1);
		int tone = s.color;
		glow(ps, c, o -> {
			// hoofprints: pairs of crescents, left and right by turns, every 0.9 blocks
			int prints = Math.min(30, (int)(length / 0.9F));
			for (int i = 1; i <= prints; i++) {
				float t = i * 0.9F / Math.max(0.01F, length);
				Vector3f at = VfxOut.along(points, n - 1, Math.min(1.0F, t));
				Vector3f ahead = VfxOut.along(points, n - 1, Math.min(1.0F, t + 0.02F)).sub(at);
				ahead.y = 0;
				if (ahead.lengthSquared() < 1.0E-6F) {
					ahead.set(s.forward);
				}
				ahead.normalize();
				Vector3f side = VfxOut.across(ahead).mul(i % 2 == 0 ? 0.3F : -0.3F);
				float a = fade * (0.4F + 0.6F * t);
				Vector3f foot = new Vector3f(at).add(side).add(0, 0.14F, 0);
				hoof(o, foot, ahead, argb(0.95F * a, 0xFFB070), argb(0.0F, tone));
				o.disc(foot, 0.5F, argb(0.35F * a, tone), argb(0.0F, tone));
			}
			// wind streaking along the path
			for (int i = 0; i < 12; i++) {
				float from = hash(s.seed * 7 + i) * 0.6F;
				float to = Math.min(1.0F, from + 0.25F + 0.35F * hash(s.seed * 3 + i));
				float h = 0.3F + 1.8F * hash(s.seed * 31 + i);
				float off = (hash(s.seed * 17 + i) - 0.5F) * 2.0F;
				Vector3f a = VfxOut.along(points, last, from);
				Vector3f b = VfxOut.along(points, last, to);
				Vector3f side = VfxOut.across(new Vector3f(b).sub(a).add(0, 1.0E-4F, 0)).mul(off);
				o.glowLine(a.add(side).add(0, h, 0), b.add(side).add(0, h, 0), 0.05F, argb(0.0F, tone), argb(0.8F * fade, VfxDraw.whiten(tone, 0.5F)));
			}
			// the bow wave at the front
			if (!s.selfView) {
				Vector3f head = points.getLast();
				o.ring(new Vector3f(head).add(0, 0.14F, 0), 0.6F, 1.4F, argb(0.5F * fade, tone));
			}
		});
		shade(ps, c, o -> {
			for (int i = 0; i < 10; i++) {
				Vector3f at = VfxOut.along(points, last, hash(s.seed + i * 5));
				o.sprite(at.add(0, 0.3F + 0.4F * hash(i), 0), s.camRight, s.camUp, 0.5F + 0.4F * s.progress, argb(0.3F * fade, 0x8A7A62));
			}
		});
	}

	/** A hoofprint: a crescent open to the back, pointing along {@code ahead}. */
	private static void hoof(final VfxOut o, final Vector3f at, final Vector3f ahead, final int core, final int edge) {
		float yaw = (float)Math.atan2(ahead.z, ahead.x);
		int n = 7;
		for (int i = 0; i < n; i++) {
			float a0 = yaw - 1.9F + 3.8F * i / n;
			float a1 = yaw - 1.9F + 3.8F * (i + 1) / n;
			Vector3f i0 = polar(a0, 0.2F, 0).add(at);
			Vector3f i1 = polar(a1, 0.2F, 0).add(at);
			Vector3f o0 = polar(a0, 0.4F, 0).add(at);
			Vector3f o1 = polar(a1, 0.4F, 0).add(at);
			o.quad(i0, o0, o1, i1, edge, core, core, edge);
		}
	}

	// ------------------------------------------------------------ Apollo

	/**
	 * Hekatebolos: a sun disc rises behind the caster's shoulder, its rays turning, and a beam of sunlight is shot from it along the
	 * aim: a broad golden glow, a white core, rings of light running down it and a flare where it ends.
	 */
	private static void sunBeam(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float extend = ease(p, 0.05F, 0.25F);
		float fade = 1.0F - ease(p, 0.6F, 1.0F);
		Vector3f dir = new Vector3f(s.reach).normalize();
		float length = s.size;
		Vector3f side = VfxOut.across(dir);
		Vector3f up = new Vector3f(side).cross(dir).normalize();
		Vector3f sun = new Vector3f(dir).mul(-1.6F).add(new Vector3f(up).mul(1.7F));
		Vector3f start = new Vector3f(dir).mul(0.6F);
		Vector3f tip = new Vector3f(dir).mul(Math.max(0.8F, length * extend));
		float time = s.ageInTicks;
		float rise = ease(p, 0.0F, 0.15F);
		glow(ps, c, o -> {
			// the sun: a white heart, a golden disc, rays turning round it
			o.sprite(sun, s.camRight, s.camUp, 2.6F * rise, argb(0.45F * fade, 0xFF9A20));
			o.sprite(sun, s.camRight, s.camUp, 1.3F * rise, argb(0.9F * fade, SUN));
			o.sprite(sun, s.camRight, s.camUp, 0.6F * rise, argb(fade, SUN_CORE));
			o.star(sun, s.camRight, s.camUp, 2.4F * rise, 12, 0.45F, time * 0.04F, argb(0.6F * fade, SUN_CORE), argb(0.0F, SUN));
			o.star(sun, s.camRight, s.camUp, 1.8F * rise, 12, 0.5F, -time * 0.06F + 0.26F, argb(0.5F * fade, 0xFFFFFF), argb(0.0F, SUN));
			// sunlight poured from it into the shot
			o.glowLine(sun, start, 0.5F, argb(0.4F * fade, SUN), argb(0.5F * fade, SUN));
			if (extend > 0.0F) {
				o.glowLine(start, tip, 1.1F, argb(0.35F * fade, 0xFF9A20), argb(0.25F * fade, 0xFF9A20));
				o.glowLine(start, tip, 0.45F, argb(0.7F * fade, SUN), argb(0.6F * fade, SUN));
				o.bar(start, tip, 0.09F, 0.06F, argb(fade, 0xFFFFFF), argb(fade, SUN_CORE));
				// rings of light running down the beam
				float reach = start.distance(tip);
				for (int i = 0; i < 8; i++) {
					float d = ((i / 8.0F + time * 0.05F) % 1.0F) * reach;
					Vector3f at = new Vector3f(dir).mul(0.6F + d);
					float rr = 0.5F + 0.35F * Mth.sin(time * 0.5F + i);
					o.circle(at, side, up, rr + 0.3F, 0.05F, 20, argb(0.8F * fade, SUN_CORE));
				}
				o.star(tip, s.camRight, s.camUp, 2.0F, 8, 0.3F, time * 0.1F, argb(0.85F * fade, 0xFFFFFF), argb(0.0F, SUN));
				o.sprite(tip, s.camRight, s.camUp, 1.6F, argb(0.6F * fade, SUN));
			}
		});
	}

	/**
	 * Achilles' Heel: a golden reticle closes on the target's feet, then one shaft of light falls from behind and above into its heel
	 * and bursts there.
	 */
	private static void heelStrike(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float close = ease(p, 0.0F, 0.35F);
		float strike = ease(p, 0.3F, 0.45F);
		float burst = ease(p, 0.42F, 1.0F);
		float fade = 1.0F - ease(p, 0.7F, 1.0F);
		float width = Math.max(0.5F, s.arg);
		Vector3f f = new Vector3f(s.forward);
		Vector3f heel = new Vector3f(f).mul(-width * 0.45F).add(0, 0.2F, 0);
		Vector3f from = new Vector3f(f).mul(-7.0F).add(0, 5.0F, 0).add(heel);
		glow(ps, c, o -> {
			float r = width * (2.6F - 1.9F * close) + 0.2F;
			Vector3f ground = new Vector3f(0, 0.14F, 0);
			o.ring(ground, r, r + 0.1F, argb(0.9F * fade, SUN));
			o.ring(ground, r * 0.55F, r * 0.55F + 0.05F, argb(0.6F * fade, SUN_CORE));
			for (int i = 0; i < 4; i++) {
				float a = Mth.HALF_PI * i + Mth.PI / 4 + (1 - close) * 1.2F;
				o.bar(polar(a, r * 0.75F, 0.14F), polar(a, r * 1.3F, 0.14F), 0.05F, 0.05F, argb(0.9F * fade, SUN_CORE), argb(0.3F * fade, SUN));
			}
			if (strike > 0.0F) {
				Vector3f tip = new Vector3f(from).lerp(heel, strike);
				o.glowLine(from, tip, 0.3F, argb(0.0F, SUN), argb(0.7F * fade, SUN));
				o.bar(new Vector3f(from).lerp(heel, Math.max(0.0F, strike - 0.25F)), tip, 0.01F, 0.04F, argb(0.0F, 0xFFFFFF), argb(fade, 0xFFFFFF));
				Vector3f dir = new Vector3f(heel).sub(from).normalize();
				o.arrow(new Vector3f(tip).sub(new Vector3f(dir).mul(1.1F)), tip, VfxOut.across(dir), 0.14F, argb(fade, SUN), argb(fade, 0xFFFFFF));
			}
			if (burst > 0.0F) {
				float k = 1.0F - burst;
				o.star(heel, s.camRight, s.camUp, 0.5F + 1.8F * burst, 8, 0.25F, s.seed, argb(0.95F * k, 0xFFFFFF), argb(0.0F, SUN));
				o.ring(new Vector3f(heel).add(0, -0.06F, 0), 2.5F * burst, 2.5F * burst + 0.25F, argb(0.8F * k, SUN));
				o.sprite(heel, s.camRight, s.camUp, 1.2F, argb(0.6F * k, SUN));
			}
		});
	}

	/** Paean: golden notes of the hymn rising round the caster, a small lyre glowing over them and rings of light washing out. */
	private static void paean(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.7F, 1.0F);
		float time = s.ageInTicks;
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.14F, 0);
			for (int w = 0; w < 3; w++) {
				float q = Mth.clamp(p * 1.5F - w * 0.2F, 0.0F, 1.0F);
				if (q > 0.0F && q < 1.0F) {
					o.ring(ground, s.size * q, s.size * q + 0.4F, argb(0.7F * (1 - q), SUN));
				}
			}
			o.disc(ground, 1.6F, argb(0.35F * fade, SUN), argb(0.0F, SUN));
			// notes spiralling up
			for (int i = 0; i < 10; i++) {
				float rise = (time * 0.035F + hash(i * 7)) % 1.0F;
				float a = Mth.TWO_PI * hash(i * 3) + rise * 2.5F;
				Vector3f at = polar(a, 1.0F + 0.6F * hash(i), 0.3F + rise * 2.8F);
				float k = fade * Mth.sin(Mth.PI * rise);
				if (s.selfView && at.y > 1.2F) {
					continue;
				}
				note(o, at, s.camRight, s.camUp, 0.34F, i % 3 == 0, argb(0.95F * k, i % 2 == 0 ? SUN : SUN_CORE));
			}
			if (!s.selfView) {
				lyre(o, new Vector3f(0, 3.0F, 0), s.camRight, s.camUp, 0.75F, argb(0.9F * fade, SUN), argb(0.6F * fade, SUN_CORE), time);
			}
		});
	}

	/** A note facing the camera: an oval head, a stem and a flag ({@code pair}: two heads joined by a beam). */
	private static void note(final VfxOut o, final Vector3f at, final Vector3f right, final Vector3f up, final float r, final boolean pair, final int color) {
		Vector3f wide = new Vector3f(right).mul(1.25F).add(new Vector3f(up).mul(0.35F));
		Vector3f tall = new Vector3f(up).mul(0.85F).sub(new Vector3f(right).mul(0.2F));
		o.sprite(at, wide, tall, r, color);
		Vector3f stemFoot = new Vector3f(right).mul(r * 1.05F).add(at);
		Vector3f stemTop = new Vector3f(up).mul(r * 4.0F).add(stemFoot);
		o.bar(stemFoot, stemTop, r * 0.12F, r * 0.12F, color, color);
		if (pair) {
			Vector3f other = new Vector3f(right).mul(r * 3.0F).add(at);
			o.sprite(other, wide, tall, r, color);
			Vector3f otherTop = new Vector3f(right).mul(r * 3.0F).add(stemTop);
			o.bar(new Vector3f(right).mul(r * 3.0F).add(stemFoot), otherTop, r * 0.12F, r * 0.12F, color, color);
			o.bar(stemTop, otherTop, r * 0.35F, r * 0.35F, color, color);
		} else {
			o.tri(stemTop, new Vector3f(right).mul(r * 1.2F).add(new Vector3f(up).mul(-r * 1.2F)).add(stemTop), new Vector3f(up).mul(-r * 0.8F).add(stemTop), color,
				color & 0xFFFFFF, color);
		}
	}

	/** Apollo's lyre: two curved arms on a crossbar, strings between, a glow behind. */
	private static void lyre(final VfxOut o, final Vector3f c, final Vector3f right, final Vector3f up, final float r, final int frame, final int strings, final float time) {
		o.sprite(c, right, up, r * 3.0F, VfxOut.argb(0.25F * ((frame >>> 24) / 255.0F), SUN));
		Vector3f prevL = null;
		Vector3f prevR = null;
		int n = 8;
		for (int i = 0; i <= n; i++) {
			float t = (float)i / n;
			float x = r * (0.55F + 0.5F * Mth.sin(t * Mth.PI * 0.9F)) - r * 0.25F * t;
			Vector3f l = new Vector3f(right).mul(-x).add(new Vector3f(up).mul(r * (2.2F * t - 1.0F))).add(c);
			Vector3f rr = new Vector3f(right).mul(x).add(new Vector3f(up).mul(r * (2.2F * t - 1.0F))).add(c);
			if (prevL != null) {
				o.bar(prevL, l, 0.04F, 0.04F, frame, frame);
				o.bar(prevR, rr, 0.04F, 0.04F, frame, frame);
			}
			prevL = l;
			prevR = rr;
		}
		Vector3f barL = new Vector3f(right).mul(-r * 0.5F).add(new Vector3f(up).mul(r * 0.95F)).add(c);
		Vector3f barR = new Vector3f(right).mul(r * 0.5F).add(new Vector3f(up).mul(r * 0.95F)).add(c);
		o.bar(barL, barR, 0.04F, 0.04F, frame, frame);
		Vector3f base = new Vector3f(up).mul(-r).add(c);
		o.bar(new Vector3f(right).mul(-r * 0.55F).add(base), new Vector3f(right).mul(r * 0.55F).add(base), 0.05F, 0.05F, frame, frame);
		for (int i = 0; i < 4; i++) {
			float x = -0.3F + 0.2F * i;
			float hum = 0.012F * Mth.sin(time * 2.0F + i);
			o.bar(new Vector3f(right).mul(r * (x + hum)).add(new Vector3f(up).mul(-r * 0.95F)).add(c), new Vector3f(right).mul(r * x).add(new Vector3f(up).mul(r * 0.95F)).add(c),
				0.012F, 0.012F, strings, strings);
		}
	}

	/** Sunlit Step: a sunburst where the caster left, a streak of sunlight to where they are now, a flash on arrival. */
	private static void sunStep(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		List<Vector3f> points = VfxOut.path(s.trail);
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.2F, 1.0F);
		float burst = ease(p, 0.0F, 0.5F);
		float time = s.ageInTicks;
		glow(ps, c, o -> {
			Vector3f start = new Vector3f(0, 1.0F, 0);
			o.star(start, s.camRight, s.camUp, 1.0F + 1.6F * burst, 12, 0.35F, time * 0.05F, argb(0.9F * fade, SUN_CORE), argb(0.0F, SUN));
			o.sprite(start, s.camRight, s.camUp, 1.2F, argb(0.7F * fade, SUN));
			o.ring(new Vector3f(0, 0.14F, 0), 2.6F * burst, 2.6F * burst + 0.3F, argb(0.8F * fade, SUN));
			if (points.size() >= 2) {
				int n = points.size();
				int last = s.selfView ? Math.max(1, n - 2) : n - 1;
				Vector3f end = new Vector3f(points.get(last)).add(0, 1.0F, 0);
				o.glowLine(start, end, 0.7F, argb(0.15F * fade, SUN), argb(0.5F * fade, SUN));
				o.glowLine(start, end, 0.12F, argb(0.3F * fade, SUN_CORE), argb(0.95F * fade, 0xFFFFFF));
				for (int i = 0; i < 6; i++) {
					Vector3f at = new Vector3f(start).lerp(end, hash(s.seed + i)).add(0, (hash(i * 3) - 0.5F) * 1.2F, 0);
					o.glint(at, s.camRight, s.camUp, 0.3F, i, argb(0.8F * fade, SUN_CORE));
				}
				if (!s.selfView) {
					o.sprite(end, s.camRight, s.camUp, 1.0F * (1 - burst) + 0.3F, argb(0.7F * fade, SUN_CORE));
				}
			}
		});
	}

	// ------------------------------------------------------------ Artemis

	/**
	 * Hounds of the Moon: a moon sigil drawn on the ground (a full moon in the middle, a ring of crescents waxing and waning round it,
	 * runes turning on the rim) and the full moon itself over the caster.
	 */
	private static void moonSigil(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.2F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float k = open * fade;
		float r = s.size * open;
		float time = s.ageInTicks;
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.14F, 0);
			o.ring(ground, r * 0.96F, r * 1.04F, argb(0.9F * k, MOON));
			o.ring(ground, r * 0.7F, r * 0.74F, argb(0.7F * k, MOON));
			o.disc(ground, r * 0.25F, argb(0.6F * k, 0xE8F0FF), argb(0.1F * k, MOON));
			o.ring(ground, r * 0.25F, r * 0.28F, argb(0.9F * k, 0xFFFFFF));
			// eight moons round the ring, phase by phase
			for (int i = 0; i < 8; i++) {
				float a = Mth.TWO_PI * i / 8 + time * 0.01F;
				Vector3f at = polar(a, r * 0.86F, 0.15F);
				crescentFlat(o, at, r * 0.09F, i / 8.0F, a, argb(0.9F * k, 0xF0F4FF));
			}
			// rune ticks turning between the rings
			int n = 36;
			for (int i = 0; i < n; i++) {
				float a = Mth.TWO_PI * i / n - time * 0.02F;
				float len = i % 3 == 0 ? 0.18F : 0.08F;
				o.bar(polar(a, r * 0.74F + 0.05F, 0.15F), polar(a, r * 0.74F + 0.05F + len * r / 6.0F, 0.15F), 0.03F, 0.03F, argb(0.7F * k, MOON),
					argb(0.7F * k, MOON));
			}
			// the full moon above
			if (!s.selfView) {
				Vector3f moon = new Vector3f(0, 5.0F, 0);
				o.sprite(moon, s.camRight, s.camUp, 2.6F * open, argb(0.35F * fade, MOON_DEEP));
				o.sprite(moon, s.camRight, s.camUp, 1.1F * open, argb(0.95F * fade, 0xF4F8FF));
				o.glowLine(moon, new Vector3f(0, 0.3F, 0), 0.9F, argb(0.15F * k, MOON), argb(0.3F * k, MOON));
			}
		});
	}

	/** A moon lying on the ground at phase {@code phase} (0 new .. 0.5 full .. 1 new), turned to face out from the middle. */
	private static void crescentFlat(final VfxOut o, final Vector3f at, final float r, final float phase, final float facing, final int color) {
		// a full disc with a darker disc offset over it would need shading: draw the lit part as a lune between two arcs
		float lit = Mth.cos(phase * Mth.TWO_PI);
		int n = 12;
		Vector3f prevOut = null;
		Vector3f prevIn = null;
		for (int i = 0; i <= n; i++) {
			float t = -Mth.HALF_PI + Mth.PI * i / n;
			float x = Mth.cos(t);
			float y = Mth.sin(t);
			Vector3f outer = new Vector3f(x * r, 0, y * r).rotateY(-facing).add(at);
			Vector3f inner = new Vector3f(x * r * lit, 0, y * r).rotateY(-facing).add(at);
			if (prevOut != null) {
				o.quad(prevIn, prevOut, outer, inner, color);
			}
			prevOut = outer;
			prevIn = inner;
		}
	}

	/**
	 * Moonlight Veil: a crescent moon hanging over the caster, a veil of silver light falling round them in shimmering folds and
	 * motes drifting down. In first person only the motes and the ring at the feet show.
	 */
	private static void moonVeil(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.15F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		int folds = 24;
		float r = 1.05F;
		float top = 2.4F;
		if (!s.selfView) {
			shade(ps, c, o -> {
				for (int i = 0; i < folds; i++) {
					float a0 = Mth.TWO_PI * i / folds;
					float a1 = Mth.TWO_PI * (i + 1) / folds;
					float w0 = 0.08F * Mth.sin(time * 0.3F + i * 1.3F);
					float w1 = 0.08F * Mth.sin(time * 0.3F + (i + 1) * 1.3F);
					float h = top * open;
					float alpha = 0.18F * k * (0.6F + 0.4F * Mth.sin(time * 0.2F + i * 0.7F));
					o.quad(polar(a0, r + w0, top - h), polar(a1, r + w1, top - h), polar(a1, r * 0.8F, top), polar(a0, r * 0.8F, top), argb(0.0F, 0xE8ECFF),
						argb(0.0F, 0xE8ECFF), argb(alpha, 0xE8ECFF), argb(alpha, 0xE8ECFF));
				}
			});
		}
		glow(ps, c, o -> {
			if (!s.selfView) {
				for (int i = 0; i < folds; i += 2) {
					float a = Mth.TWO_PI * i / folds;
					float w = 0.08F * Mth.sin(time * 0.3F + i * 1.3F);
					o.glowLine(polar(a, r * 0.8F, top), polar(a, r + w, top - top * open * 0.9F), 0.04F, argb(0.5F * k, MOON), argb(0.0F, MOON));
				}
				Vector3f moon = new Vector3f(0, top + 0.6F, 0);
				o.sprite(moon, s.camRight, s.camUp, 0.9F, argb(0.3F * k, MOON_DEEP));
				crescentFacing(o, moon, s.camRight, s.camUp, 0.45F, argb(0.95F * k, 0xF4F8FF));
			}
			for (int i = 0; i < 14; i++) {
				float fall = (time * 0.03F + hash(i * 5)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 11), 0.5F + 0.8F * hash(i), top * (1.0F - fall));
				o.glint(at, s.camRight, s.camUp, 0.14F, time * 0.1F + i, argb(0.9F * k * Mth.sin(Mth.PI * fall), 0xF4F8FF));
			}
			o.ring(new Vector3f(0, 0.14F, 0), r - 0.1F, r + 0.2F, argb(0.6F * k, MOON));
		});
	}

	/** A crescent moon facing the camera, horns to the upper right. */
	static void crescentFacing(final VfxOut o, final Vector3f c, final Vector3f right, final Vector3f up, final float r, final int color) {
		int n = 14;
		Vector3f prevOut = null;
		Vector3f prevIn = null;
		Vector3f a = new Vector3f(right).mul(Mth.cos(0.6F)).add(new Vector3f(up).mul(Mth.sin(0.6F)));
		Vector3f b = new Vector3f(up).mul(Mth.cos(0.6F)).sub(new Vector3f(right).mul(Mth.sin(0.6F)));
		for (int i = 0; i <= n; i++) {
			float t = -Mth.HALF_PI + Mth.PI * i / n;
			// the lit limb and, inside it, the terminator: an ellipse through the horns
			Vector3f outer = new Vector3f(a).mul(Mth.cos(t) * r).add(new Vector3f(b).mul(Mth.sin(t) * r)).add(c);
			Vector3f inner = new Vector3f(a).mul(Mth.cos(t) * r * 0.45F).add(new Vector3f(b).mul(Mth.sin(t) * r)).add(c);
			if (prevOut != null) {
				o.quad(prevIn, prevOut, outer, inner, color);
			}
			prevOut = outer;
			prevIn = inner;
		}
	}

	private HeavenLooks() {
	}
}
