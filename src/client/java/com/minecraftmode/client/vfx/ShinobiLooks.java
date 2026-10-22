package com.minecraftmode.client.vfx;

import static com.minecraftmode.client.vfx.SkillVfxRenderer.glow;
import static com.minecraftmode.client.vfx.SkillVfxRenderer.shade;
import static com.minecraftmode.client.vfx.VfxOut.argb;
import static com.minecraftmode.client.vfx.VfxOut.ease;
import static com.minecraftmode.client.vfx.VfxOut.hash;
import static com.minecraftmode.client.vfx.VfxOut.polar;

import com.minecraftmode.client.vfx.SkillVfxRenderer.State;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Looks of the legendary shinobi (Naruto): Minato's Flying Thunder God kunai (the Yellow Flash, the technique itself and its space-time
 * barrier; the Rasengan it ends in is the Rasenshuriken's) and Kakashi's Kamui (the vortex where a Kamui Shuriken bursts, Purple
 * Lightning, Kamui and Kamui Raikiri).
 */
final class ShinobiLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final int YELLOW = 0xFFD400;
	private static final int YELLOW_PALE = 0xFFF6B8;
	private static final int STEEL = 0x8C949E;
	private static final int GRIP = 0x2A2A30;
	private static final int KAMUI = 0x9B59FF;
	private static final int KAMUI_PALE = 0xDCC8FF;
	private static final int KAMUI_DARK = 0x0E0618;
	private static final int IRIS = 0xD8101E;
	private static final int INK = 0x0A0404;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case YELLOW_FLASH -> yellowFlash(s, ps, c);
			case HIRAISHIN -> hiraishin(s, ps, c);
			case SPACE_TIME_BARRIER -> barrier(s, ps, c);
			case KAMUI_VORTEX -> vortex(s, ps, c);
			case PURPLE_CHAIN -> purpleChain(s, ps, c);
			case KAMUI_WARP -> kamuiWarp(s, ps, c);
			case KAMUI_RAIKIRI -> kamuiRaikiri(s, ps, c);
			default -> {
			}
		}
	}

	/** Where a blinking holder appeared: the first point of its trail well away from where the look began (the holder now when there is none). */
	static Vector3f arrival(final State s) {
		for (Vector3f q : s.trail) {
			if (q.lengthSquared() > 2.25F) {
				return new Vector3f(q);
			}
		}
		return s.trail.isEmpty() ? new Vector3f() : new Vector3f(s.trail.getLast());
	}

	/** A Flying Thunder God kunai stuck in the ground at {@code at}, leaning towards {@code lean}, its seal tag fluttering (shade or glow). */
	private static void plantedKunai(final VfxOut o, final Vector3f at, final Vector3f lean, final float time, final int seed, final float alpha) {
		Vector3f up = new Vector3f(lean).mul(0.35F).add(0, 1, 0).normalize();
		Vector3f tip = new Vector3f(at).add(new Vector3f(up).mul(-0.15F));
		Vector3f tail = new Vector3f(at).add(new Vector3f(up).mul(0.75F));
		Vector3f side = VfxOut.across(up);
		Vector3f tagTop = new Vector3f(tail).lerp(at, 0.3F);
		Vector3f tagLow = new Vector3f(tagTop).add(new Vector3f(side).mul(0.1F * Mth.sin(time * 0.4F + seed))).add(0, -0.32F, 0).add(new Vector3f(lean).mul(-0.05F));
		Vector3f n = new Vector3f(up).cross(side).normalize();
		if (o.glow) {
			o.glowLine(tip, tail, 0.06F, argb(0.5F * alpha, YELLOW), argb(0.5F * alpha, YELLOW));
			// the formula on its tag
			o.rune(new Vector3f(tagTop).lerp(tagLow, 0.5F).add(new Vector3f(n).mul(0.02F)), side, new Vector3f(tagTop).sub(tagLow).normalize(), 0.11F, seed, 0.012F,
				argb(0.9F * alpha, YELLOW_PALE));
		} else {
			o.kunai(tail, tip, side, argb(0.97F * alpha, STEEL), argb(0.97F * alpha, GRIP));
			Vector3f w = new Vector3f(side).mul(0.06F);
			o.quad(new Vector3f(tagTop).sub(w), new Vector3f(tagTop).add(w), new Vector3f(tagLow).add(w), new Vector3f(tagLow).sub(w), argb(0.92F * alpha, 0xEEE6D2));
		}
	}

	// ------------------------------------------------------------ Minato

	/**
	 * Yellow Flash: at each of the four strikes a streak of yellow light cuts clean through the target from a new side, a flash where it
	 * passes the body, and a formula kunai is left standing in the ground round it, its tag fluttering.
	 */
	private static void yellowFlash(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float h = Math.max(1.0F, s.size);
		float w = Math.max(0.6F, s.arg);
		float time = s.ageInTicks;
		float fade = 1.0F - ease(p, 0.75F, 1.0F);
		Vector3f mid = new Vector3f(0, h * 0.55F, 0);
		float len = 2.4F + w;
		shade(ps, c, o -> {
			for (int k = 0; k < 4; k++) {
				if (age < k * 5.0F) {
					continue;
				}
				float a = s.seed * 0.7F + k * 2.2F;
				Vector3f at = polar(a + Mth.PI, w * 0.5F + 1.3F, 0.0F);
				plantedKunai(o, at, polar(a + Mth.PI, 1.0F, 0.0F), time, s.seed + k, fade * ease(age - k * 5.0F, 0.0F, 2.0F));
			}
		});
		glow(ps, c, o -> {
			for (int k = 0; k < 4; k++) {
				float local = age - k * 5.0F;
				if (local < 0.0F) {
					continue;
				}
				float a = s.seed * 0.7F + k * 2.2F;
				Vector3f d = polar(a, 1.0F, (hash(s.seed + k) - 0.5F) * 0.5F).normalize();
				float flash = 1.0F - ease(local, 0.0F, 8.0F);
				float grow = ease(local, 0.0F, 1.5F);
				Vector3f from = new Vector3f(mid).sub(new Vector3f(d).mul(len));
				Vector3f to = new Vector3f(from).lerp(new Vector3f(mid).add(new Vector3f(d).mul(len)), grow);
				if (flash > 0.0F) {
					o.glowLine(from, to, 0.7F, argb(0.45F * flash, YELLOW), argb(0.6F * flash, YELLOW));
					o.glowLine(from, to, 0.22F, argb(0.9F * flash, YELLOW_PALE), argb(flash, YELLOW_PALE));
					o.bar(from, to, 0.03F, 0.03F, argb(flash, 0xFFFFFF), argb(flash, 0xFFFFFF));
					// the afterimage where the streak ends: the caster, an instant behind the target
					o.sprite(new Vector3f(to).add(0, 0.1F, 0), s.camRight, s.camUp, 0.9F * flash, argb(0.5F * flash, YELLOW));
				}
				float star = 1.0F - ease(local, 0.5F, 5.0F);
				if (star > 0.0F) {
					o.star(mid, s.camRight, s.camUp, h * 0.55F * star + 0.1F, 8, 0.22F, a, argb(0.95F * star, 0xFFFFFF), argb(0.0F, YELLOW));
				}
				Vector3f at = polar(a + Mth.PI, w * 0.5F + 1.3F, 0.0F);
				plantedKunai(o, at, polar(a + Mth.PI, 1.0F, 0.0F), time, s.seed + k, fade * ease(local, 0.0F, 2.0F));
			}
			o.ring(new Vector3f(0, 0.14F, 0), w * 0.5F + 1.0F, w * 0.5F + 1.5F, argb(0.3F * fade, YELLOW));
		});
	}

	/**
	 * Flying Thunder God: the caster is gone in a flash of yellow, a streak running to where they appear, and under them there the formula
	 * of the technique glows on the ground, a kunai standing in it.
	 */
	private static void hiraishin(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float flash = 1.0F - ease(age, 0.0F, 9.0F);
		float fade = 1.0F - ease(p, 0.6F, 1.0F);
		Vector3f end = arrival(s);
		Vector3f start = new Vector3f(0, 1.0F, 0);
		Vector3f to = new Vector3f(end).add(0, 1.0F, 0);
		Vector3f dir = new Vector3f(end).sub(0, end.y, 0);
		Vector3f lean = dir.lengthSquared() < 1.0E-4F ? new Vector3f(s.forward) : dir.normalize();
		Vector3f kunaiAt = new Vector3f(end).add(new Vector3f(VfxOut.across(lean)).mul(0.9F));
		shade(ps, c, o -> plantedKunai(o, kunaiAt, lean, time, s.seed, fade));
		glow(ps, c, o -> {
			if (flash > 0.0F) {
				o.glowLine(start, to, 0.7F, argb(0.3F * flash, YELLOW), argb(0.3F * flash, YELLOW));
				o.glowLine(start, to, 0.18F, argb(0.85F * flash, YELLOW_PALE), argb(0.85F * flash, YELLOW_PALE));
				o.glowLine(new Vector3f(0, 0.1F, 0), new Vector3f(0, 1.9F, 0), 0.5F, argb(0.55F * flash, YELLOW), argb(0.55F * flash, YELLOW));
				o.star(start, s.camRight, s.camUp, 1.3F * flash + 0.1F, 8, 0.2F, s.seed, argb(0.9F * flash, 0xFFFFFF), argb(0.0F, YELLOW));
			}
			seal(o, new Vector3f(end).add(0, 0.15F, 0), 1.0F * ease(age, 0.0F, 4.0F), time, s.seed, fade);
			plantedKunai(o, kunaiAt, lean, time, s.seed, fade);
			for (int i = 0; i < 10; i++) {
				float q = (time * 0.08F + hash(s.seed + i)) % 1.0F;
				Vector3f at = new Vector3f(to).add(BladeLooks.shardDir(s.seed, i).mul(0.3F + 0.9F * q));
				o.glint(at, s.camRight, s.camUp, 0.12F, i, argb(0.85F * fade * (1.0F - q), YELLOW_PALE));
			}
		});
	}

	/** The Flying Thunder God formula on the ground round {@code c}: a ring, a small one in the middle and strokes of script radiating out. */
	private static void seal(final VfxOut o, final Vector3f c, final float r, final float time, final int seed, final float alpha) {
		if (r <= 0.05F) {
			return;
		}
		Vector3f x = new Vector3f(1, 0, 0);
		Vector3f z = new Vector3f(0, 0, 1);
		int line = argb(0.85F * alpha, YELLOW_PALE);
		o.circle(c, x, z, r * 0.22F, 0.025F, 12, line);
		o.circle(c, x, z, r, 0.02F, 28, argb(0.6F * alpha, YELLOW));
		for (int i = 0; i < 10; i++) {
			float a = Mth.TWO_PI * i / 10 + 0.1F * Mth.sin(time * 0.05F);
			Vector3f out = polar(a, 1.0F, 0.0F);
			Vector3f tang = polar(a + Mth.HALF_PI, 1.0F, 0.0F);
			o.bar(new Vector3f(out).mul(r * 0.25F).add(c), new Vector3f(out).mul(r * (0.75F + 0.2F * hash(seed + i))).add(c), 0.018F, 0.012F, line, line);
			o.rune(new Vector3f(out).mul(r * 0.6F).add(new Vector3f(tang).mul(r * 0.1F)).add(c), tang, out, r * 0.09F, seed * 7 + i, 0.012F, line);
		}
	}

	/**
	 * Space-Time Barrier: the formula is written out on the ground round the caster, a band of script turns round them at the waist and a
	 * barrier of yellow light flickers over them, twisting now and then as if space bent.
	 */
	private static void barrier(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float time = s.ageInTicks;
		float open = ease(p, 0.0F, 0.15F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float k = open * fade;
		float r = Math.max(1.0F, s.size);
		glow(ps, c, o -> {
			seal(o, new Vector3f(0, 0.15F, 0), r * 1.1F * open, time, s.seed, k);
			float flicker = 0.6F + 0.4F * Mth.sin(time * 0.9F) * Mth.sin(time * 0.37F);
			o.wall(new Vector3f(), r, 0.0F, 2.3F, argb(0.22F * k * flicker, YELLOW), argb(0.0F, YELLOW));
			o.ring(new Vector3f(0, 0.14F, 0), r - 0.15F, r + 0.1F, argb(0.6F * k, YELLOW));
			if (!s.selfView) {
				for (int i = 0; i < 16; i++) {
					float a = Mth.TWO_PI * i / 16 + time * 0.03F;
					Vector3f out = polar(a, 1.0F, 0.0F);
					Vector3f tang = polar(a + Mth.HALF_PI, 1.0F, 0.0F);
					o.rune(new Vector3f(out).mul(r * 0.85F).add(0, 1.1F + 0.1F * Mth.sin(time * 0.1F + i), 0), tang, UP, 0.12F, s.seed * 3 + i, 0.015F,
						argb(0.85F * k, YELLOW_PALE));
				}
			}
			// a twist of space: the barrier's rings spun out of true for a moment
			float twist = (time * 0.04F + hash(s.seed)) % 1.0F;
			if (twist < 0.25F) {
				float q = twist / 0.25F;
				Vector3f a = new Vector3f(1, 0.4F * Mth.sin(q * Mth.PI), 0).normalize();
				o.circle(new Vector3f(0, 1.0F, 0), a, new Vector3f(0, 0, 1), r * (0.9F + 0.2F * q), 0.03F, 28, argb(0.6F * k * (1.0F - q), YELLOW_PALE));
			}
		});
	}

	// ------------------------------------------------------------ Kakashi

	/**
	 * Where a Kamui Shuriken bursts: space caves in round the spot: the edge of the burst flares as a ring, then spiral arms wind everything
	 * inside it into a dark hole that swells and closes.
	 */
	private static void vortex(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = s.size;
		float time = s.ageInTicks;
		float pull = ease(age, 0.0F, 16.0F);
		float edge = 1.0F - ease(age, 2.0F, 9.0F);
		float fade = 1.0F - ease(p, 0.75F, 1.0F);
		float core = Mth.sin(Mth.PI * Math.min(1.0F, age / (s.life * 0.9F))) * 1.2F + 0.2F;
		shade(ps, c, o -> {
			o.sprite(new Vector3f(), s.camRight, s.camUp, core * 1.4F, argb(0.85F * fade, KAMUI_DARK));
			// the torn space: dark arms winding in
			for (int k = 0; k < 4; k++) {
				List<Vector3f> arm = new ArrayList<>();
				for (int i = 0; i <= 14; i++) {
					float t = i / 14.0F;
					float rr = r * (1.0F - pull * 0.75F) * (1.0F - t) + core * 0.4F * t;
					float a = Mth.TWO_PI * k / 4 + time * 0.35F + t * 4.0F;
					arm.add(new Vector3f(s.camRight).mul(Mth.cos(a) * rr).add(new Vector3f(s.camUp).mul(Mth.sin(a) * rr)));
				}
				o.polyline(arm, 0.08F * (1.0F - pull * 0.5F), argb(0.6F * fade, KAMUI_DARK));
			}
		});
		glow(ps, c, o -> {
			o.circle(new Vector3f(), s.camRight, s.camUp, r, 0.06F, 40, argb(0.8F * edge, KAMUI_PALE));
			o.sprite(new Vector3f(), s.camRight, s.camUp, r, argb(0.18F * edge, KAMUI));
			for (int k = 0; k < 6; k++) {
				Vector3f prev = null;
				for (int i = 0; i <= 16; i++) {
					float t = i / 16.0F;
					float rr = r * (1.0F - pull * 0.7F) * (1.0F - t) + core * 0.45F * t;
					float a = Mth.TWO_PI * k / 6 + time * 0.4F + t * 4.2F;
					Vector3f q = new Vector3f(s.camRight).mul(Mth.cos(a) * rr).add(new Vector3f(s.camUp).mul(Mth.sin(a) * rr));
					if (prev != null) {
						float al = 0.7F * fade * (0.3F + 0.7F * t);
						o.glowLine(prev, q, 0.07F, argb(al, KAMUI), argb(al, KAMUI_PALE));
					}
					prev = q;
				}
			}
			o.circle(new Vector3f(), s.camRight, s.camUp, core * 0.75F, 0.05F, 24, argb(0.8F * fade, KAMUI_PALE));
			for (int i = 0; i < 12; i++) {
				float q = (time * 0.07F + hash(s.seed + i)) % 1.0F;
				float a = Mth.TWO_PI * hash(i * 5) + q * 5.0F;
				float rr = r * (1.0F - q);
				o.glint(new Vector3f(s.camRight).mul(Mth.cos(a) * rr).add(new Vector3f(s.camUp).mul(Mth.sin(a) * rr)), s.camRight, s.camUp, 0.12F, i,
					argb(0.85F * fade * q, KAMUI_PALE));
			}
		});
	}

	/**
	 * Purple Lightning: violet lightning leaps from the caster's hand to the nearest enemy and on from each to the next within the chain's
	 * jump, a burst of it on each one struck.
	 */
	private static void purpleChain(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.5F, 1.0F);
		float time = s.ageInTicks;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f hand = new Vector3f(ahead).mul(0.7F).add(new Vector3f(right).mul(-0.35F)).add(0, 1.25F, 0);
		int seed = s.seed * 19 + (int)time;
		List<Vector3f> hops = new ArrayList<>();
		hops.add(hand);
		List<Vector4f> left = new ArrayList<>();
		for (Vector4f t : s.targets) {
			if (t.x * t.x + t.z * t.z <= s.size * s.size) {
				left.add(t);
			}
		}
		Vector3f from = new Vector3f(hand);
		for (int i = 0; i < 6 && !left.isEmpty(); i++) {
			Vector4f best = null;
			float bestD = Float.MAX_VALUE;
			for (Vector4f t : left) {
				float d = from.distanceSquared(t.x, t.y + t.w * 0.5F, t.z);
				if (d < bestD) {
					bestD = d;
					best = t;
				}
			}
			// on from the first only to an enemy within 6 blocks of the last one struck
			if (i > 0 && bestD > 36.0F) {
				break;
			}
			left.remove(best);
			from = new Vector3f(best.x, best.y + best.w * 0.5F, best.z);
			hops.add(from);
		}
		if (hops.size() == 1) {
			hops.add(new Vector3f(ahead).mul(6.0F).add(hand).add(0, -0.4F, 0));
		}
		glow(ps, c, o -> {
			for (int i = 0; i + 1 < hops.size(); i++) {
				Vector3f a = hops.get(i);
				Vector3f b = hops.get(i + 1);
				float d = a.distance(b);
				o.bolt(a, b, seed + i, Math.min(1.0F, d * 0.12F), Math.max(4, (int)(d * 1.4F)), 0.24F, argb(0.65F * fade, KAMUI), argb(fade, 0xF2E8FF));
				o.bolt(a, b, seed * 3 + i, Math.min(1.2F, d * 0.15F), Math.max(4, (int)(d * 1.2F)), 0.08F, argb(0.45F * fade, KAMUI), argb(0.6F * fade, KAMUI_PALE));
			}
			for (int i = 1; i < hops.size(); i++) {
				Vector3f at = hops.get(i);
				o.star(at, s.camRight, s.camUp, 0.9F + 0.3F * hash(seed + i), 6, 0.3F, seed + i, argb(0.85F * fade, KAMUI_PALE), argb(0.0F, KAMUI));
				for (int j = 0; j < 3; j++) {
					o.bolt(at, new Vector3f(BladeLooks.shardDir(seed + i, j)).mul(0.7F).add(at), seed + i * 7 + j, 0.15F, 3, 0.06F, argb(0.6F * fade, KAMUI),
						argb(0.9F * fade, 0xFFFFFF));
				}
			}
			// the chirping lightning in the hand
			o.sprite(hand, s.camRight, s.camUp, 0.55F, argb(0.8F * fade, KAMUI_PALE));
			for (int j = 0; j < 4; j++) {
				o.bolt(hand, new Vector3f(BladeLooks.shardDir(seed, j + 9)).mul(0.5F).add(hand), seed + j * 13, 0.12F, 3, 0.05F, argb(0.7F * fade, KAMUI),
					argb(0.9F * fade, 0xFFFFFF));
			}
		});
	}

	/**
	 * Kamui: space round the caster twists into a vortex, dark arms winding round their body into a point, a spiral turning on the ground,
	 * and Kakashi's Mangekyo Sharingan opens over them.
	 */
	private static void kamuiWarp(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float time = s.ageInTicks;
		float open = ease(p, 0.0F, 0.2F);
		float fade = 1.0F - ease(p, 0.75F, 1.0F);
		float k = open * fade;
		Vector3f body = new Vector3f(0, 1.1F, 0);
		Vector3f eyeAt = new Vector3f(0, 2.75F + 0.1F * Mth.sin(time * 0.1F), 0);
		shade(ps, c, o -> {
			if (!s.selfView) {
				for (int a = 0; a < 3; a++) {
					List<Vector3f> arm = new ArrayList<>();
					for (int i = 0; i <= 14; i++) {
						float t = i / 14.0F;
						float rr = 1.3F * (1.0F - t) * open;
						float ang = Mth.TWO_PI * a / 3 + time * 0.3F + t * 4.0F;
						arm.add(new Vector3f(s.camRight).mul(Mth.cos(ang) * rr).add(new Vector3f(s.camUp).mul(Mth.sin(ang) * rr)).add(body));
					}
					o.polyline(arm, 0.07F, argb(0.6F * k, KAMUI_DARK));
				}
				o.sprite(body, s.camRight, s.camUp, 0.5F * open, argb(0.7F * k, KAMUI_DARK));
				o.eye(eyeAt, s.camRight, s.camUp, 0.42F * open, time * 0.05F, VfxOut.KAKASHI, argb(0.97F * k, IRIS), argb(0.97F * k, INK));
			}
		});
		glow(ps, c, o -> {
			if (!s.selfView) {
				o.sprite(new Vector3f(eyeAt).sub(new Vector3f(s.camRight).cross(s.camUp).mul(0.05F)), s.camRight, s.camUp, 0.75F * open, argb(0.35F * k, 0xFF2020));
				for (int a = 0; a < 5; a++) {
					Vector3f prev = null;
					for (int i = 0; i <= 14; i++) {
						float t = i / 14.0F;
						float rr = 1.5F * (1.0F - t) * open;
						float ang = Mth.TWO_PI * a / 5 + time * 0.3F + t * 4.0F;
						Vector3f q = new Vector3f(s.camRight).mul(Mth.cos(ang) * rr).add(new Vector3f(s.camUp).mul(Mth.sin(ang) * rr)).add(body);
						if (prev != null) {
							o.glowLine(prev, q, 0.06F, argb(0.5F * k * t, KAMUI), argb(0.5F * k * t, KAMUI_PALE));
						}
						prev = q;
					}
				}
			}
			for (int a = 0; a < 4; a++) {
				Vector3f prev = null;
				for (int i = 0; i <= 16; i++) {
					float t = i / 16.0F;
					float ang = Mth.TWO_PI * a / 4 - time * 0.15F + t * 3.5F;
					Vector3f q = polar(ang, 0.2F + 1.4F * t, 0.15F);
					if (prev != null) {
						o.glowLine(prev, q, 0.05F, argb(0.6F * k * (1.0F - t), KAMUI), argb(0.6F * k * (1.0F - t), KAMUI));
					}
					prev = q;
				}
			}
		});
	}

	/**
	 * Kamui Raikiri: where the caster was, space twists shut behind them; a jagged streak of violet lightning runs to where they strike,
	 * and there the Raikiri bursts: lightning lashing out over the ground to the edge of the blast, a ring on it.
	 */
	private static void kamuiRaikiri(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = s.size;
		float time = s.ageInTicks;
		float fade = 1.0F - ease(p, 0.55F, 1.0F);
		Vector3f end = arrival(s);
		Vector3f start = new Vector3f(0, 1.0F, 0);
		Vector3f to = new Vector3f(end).add(0, 1.0F, 0);
		float twist = ease(age, 0.0F, 10.0F);
		int seed = s.seed * 31 + (int)(time / 2);
		shade(ps, c, o -> {
			float cr = 0.9F * Mth.sin(Mth.PI * twist);
			if (cr > 0.02F) {
				o.sprite(start, s.camRight, s.camUp, cr, argb(0.85F * fade, KAMUI_DARK));
			}
		});
		glow(ps, c, o -> {
			for (int a = 0; a < 4; a++) {
				Vector3f prev = null;
				for (int i = 0; i <= 12; i++) {
					float t = i / 12.0F;
					float rr = 1.2F * (1.0F - twist) * (1.0F - t) + 0.1F;
					float ang = Mth.TWO_PI * a / 4 + time * 0.5F + t * 4.0F;
					Vector3f q = new Vector3f(s.camRight).mul(Mth.cos(ang) * rr).add(new Vector3f(s.camUp).mul(Mth.sin(ang) * rr)).add(start);
					if (prev != null) {
						o.glowLine(prev, q, 0.05F, argb(0.6F * fade * (1.0F - twist), KAMUI), argb(0.6F * fade * (1.0F - twist), KAMUI_PALE));
					}
					prev = q;
				}
			}
			if (to.distance(start) > 0.5F) {
				o.bolt(start, to, seed, 0.5F, Math.max(5, (int)(to.distance(start) * 1.2F)), 0.28F, argb(0.6F * fade, KAMUI), argb(fade, 0xF6F0FF));
			}
			Vector3f g = new Vector3f(end).add(0, 0.15F, 0);
			float burst = ease(age, 0.0F, 5.0F);
			o.ring(g, Math.max(0.0F, r - 0.3F), r, argb(0.7F * fade, KAMUI));
			o.ring(new Vector3f(g).add(0, 0.01F, 0), Math.max(0.0F, r * burst - 0.6F), r * burst, argb(0.8F * (1.0F - burst), KAMUI_PALE));
			for (int i = 0; i < 9; i++) {
				float a = Mth.TWO_PI * i / 9 + hash(seed + i);
				Vector3f tip = polar(a, r * (0.75F + 0.25F * hash(seed * 3 + i)) * burst, 0.0F).add(g);
				o.bolt(new Vector3f(to).add(0, -0.3F, 0), tip, seed + i * 5, 0.35F, 6, 0.12F, argb(0.55F * fade, KAMUI), argb(0.85F * fade, 0xFFFFFF));
			}
			float flash = 1.0F - ease(age, 0.0F, 6.0F);
			o.star(to, s.camRight, s.camUp, r * 0.5F * flash + 0.2F, 10, 0.25F, s.seed, argb(0.9F * flash, 0xFFFFFF), argb(0.0F, KAMUI));
			o.sprite(new Vector3f(to).add(0, -0.1F, 0), s.camRight, s.camUp, 0.6F, argb(0.6F * fade, KAMUI_PALE));
		});
	}

	private ShinobiLooks() {
	}
}
