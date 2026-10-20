package com.minecraftmode.client.vfx;

import static com.minecraftmode.client.vfx.SkillVfxRenderer.glow;
import static com.minecraftmode.client.vfx.SkillVfxRenderer.shade;
import static com.minecraftmode.client.vfx.VfxOut.argb;
import static com.minecraftmode.client.vfx.VfxOut.ease;
import static com.minecraftmode.client.vfx.VfxOut.hash;
import static com.minecraftmode.client.vfx.VfxOut.polar;

import com.minecraftmode.client.vfx.SkillVfxRenderer.State;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Looks of the witch of Colchis, Medea: Hecatic Wizard's sky of magic circles, Rule Breaker (the zigzag rainbow dagger that breaks
 * every contract), High-Speed Divine Words, the golden fleece, Witch's Dagger and her territory's ward circle.
 */
final class WitchLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final Vector3f X = new Vector3f(1, 0, 0);
	private static final Vector3f Z = new Vector3f(0, 0, 1);
	private static final int PALE = 0xF4E8FF;
	private static final int DUSK = 0x2A0A4A;
	private static final int GOLD = 0xFFD24A;
	private static final int GOLD_PALE = 0xFFF0B0;
	/** Rule Breaker's blade, kink by kink. */
	static final int[] RAINBOW = {0xFF4A6A, 0xFFA04A, 0xFFE84A, 0x6AF07A, 0x4AC8FF, 0x8A6AFF, 0xE06AFF};

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case HECATIC_SKY -> hecaticSky(s, ps, c);
			case WITCH_CIRCLE -> witchCircle(s, ps, c);
			case RULE_BREAKER -> ruleBreaker(s, ps, c);
			case DIVINE_WORDS -> divineWords(s, ps, c);
			case GOLDEN_FLEECE -> goldenFleece(s, ps, c);
			case SPELL_BREAK -> spellBreak(s, ps, c);
			case WITCH_SLASH -> witchSlash(s, ps, c);
			case TERRITORY -> territory(s, ps, c);
			default -> {
			}
		}
	}

	/** The holder's flat facing from their body yaw (+z ahead at yaw 0). */
	static Vector3f ahead(final State s) {
		float yaw = s.bodyYaw * Mth.DEG_TO_RAD;
		return new Vector3f(-Mth.sin(yaw), 0, Mth.cos(yaw));
	}

	// ------------------------------------------------------------ Hecatic Wizard

	/**
	 * Hecatic Wizard: a great witch's circle opens in the sky over the aimed spot and eight smaller ones round it, one after another, all
	 * facing down (the bolts fall out of them); a pentagram circle turns on the ground beneath.
	 */
	private static void hecaticSky(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float open = ease(s.progress, 0.0F, 0.12F);
		float fade = 1.0F - ease(s.progress, 0.85F, 1.0F);
		float time = s.ageInTicks;
		int count = 9;
		// where the rain starts: 14 up and a little off (Actions.sky)
		Vector3f sky = new Vector3f(0.6F, 13.6F, 0.6F);
		Vector3f[] at = new Vector3f[count];
		float[] radius = new float[count];
		float[] opened = new float[count];
		for (int i = 0; i < count; i++) {
			if (i == 0) {
				at[i] = new Vector3f(sky);
				radius[i] = s.size * 0.75F;
			} else {
				float a = Mth.TWO_PI * (i - 1) / (count - 1) + 0.4F;
				at[i] = polar(a, s.size * 1.05F, -1.2F + 2.4F * hash(s.seed + i * 5)).add(sky);
				radius[i] = 1.1F + 0.6F * hash(i * 7 + 3);
			}
			opened[i] = ease(age - i * 1.1F, 0.0F, 5.0F);
		}
		shade(ps, c, o -> {
			for (int i = 0; i < count; i++) {
				// a dusk under each circle: violet light alone washes out against a day sky
				o.disc(new Vector3f(at[i]).add(0, 0.05F, 0), radius[i] * 1.1F * opened[i], argb(0.55F * fade, DUSK), argb(0.1F * fade, DUSK));
			}
		});
		glow(ps, c, o -> {
			for (int i = 0; i < count; i++) {
				if (opened[i] <= 0.01F) {
					continue;
				}
				float spin = time * (i % 2 == 0 ? 0.04F : -0.06F) + i;
				o.magicCircle(at[i], X, Z, radius[i] * opened[i], spin, s.seed + i, i == 0 ? 6 : 5, argb(0.95F * fade, s.color), argb(0.85F * fade, PALE));
				o.disc(at[i], radius[i] * 0.5F * opened[i], argb(0.4F * fade, PALE), argb(0.0F, s.color));
			}
			o.sprite(sky, s.camRight, s.camUp, s.size * 1.6F * open, argb(0.18F * fade, s.color));
			// the light falling from the great circle and the circle on the ground
			o.glowLine(sky, new Vector3f(0, 0.2F, 0), s.size * 0.4F, argb(0.1F * fade, s.color), argb(0.05F * fade, s.color));
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			o.magicCircle(ground, X, Z, s.size * open, -time * 0.03F, s.seed * 3, 5, argb(0.9F * fade, s.color), argb(0.7F * fade, PALE));
			o.disc(new Vector3f(0, 0.14F, 0), s.size, argb(0.15F * fade, s.color), argb(0.0F, s.color));
		});
	}

	/** One witch's circle a bolt flies out of: opens fast facing the bolt's flight, turns, flashes and fades. */
	private static void witchCircle(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.25F);
		float fade = 1.0F - ease(p, 0.55F, 1.0F);
		Vector3f n = new Vector3f(s.reach);
		if (n.lengthSquared() < 1.0E-6F) {
			n.set(s.forward);
		}
		n.normalize();
		Vector3f a = VfxOut.across(n);
		Vector3f b = new Vector3f(n).cross(a).normalize();
		float r = Math.max(0.5F, s.size) * open;
		Vector3f mid = new Vector3f();
		shade(ps, c, o -> o.sprite(mid, a, b, r * 1.1F, argb(0.5F * fade, DUSK)));
		glow(ps, c, o -> {
			o.magicCircle(mid, a, b, r, s.ageInTicks * 0.2F, s.seed, 6, argb(0.95F * fade, s.color), argb(0.9F * fade, PALE));
			o.sprite(mid, a, b, r * 0.8F, argb(0.35F * fade, s.color));
			float flash = 1.0F - ease(p, 0.0F, 0.4F);
			o.glint(new Vector3f(n).mul(0.1F), s.camRight, s.camUp, 0.15F + 0.6F * flash, s.seed, argb(fade, 0xFFFFFF));
		});
	}

	// ------------------------------------------------------------ Rule Breaker

	/**
	 * Rule Breaker, the dagger of Colchis: a kinked blade like a bolt of lightning, every kink another colour, a gilt guard and a dark
	 * grip; {@code plane} sets the plane its kinks zigzag in. In the shade pass ({@code lit} false) it is a translucent body that still
	 * shows by day.
	 */
	static void dagger(final VfxOut o, final Vector3f base, final Vector3f dir, final Vector3f plane, final float length, final float alpha, final boolean lit) {
		Vector3f d = new Vector3f(dir).normalize();
		Vector3f side = new Vector3f(plane).sub(new Vector3f(d).mul(new Vector3f(plane).dot(d)));
		if (side.lengthSquared() < 1.0E-6F) {
			side = VfxOut.across(d);
		}
		side.normalize();
		int kinks = 7;
		float w = length * 0.12F;
		Vector3f prevL = null;
		Vector3f prevR = null;
		Vector3f prevC = null;
		for (int i = 0; i <= kinks; i++) {
			float t = (float)i / kinks;
			float zig = i == 0 || i == kinks ? 0.0F : i % 2 == 0 ? 1.0F : -1.0F;
			Vector3f center = new Vector3f(d).mul(length * t).add(base).add(new Vector3f(side).mul(zig * w * 0.7F * (1.0F - t * 0.6F)));
			float half = w * 0.55F * (1.0F - t) + 0.01F;
			Vector3f l = new Vector3f(side).mul(-half).add(center);
			Vector3f r = new Vector3f(side).mul(half).add(center);
			if (prevL != null) {
				int col = RAINBOW[(i - 1) % RAINBOW.length];
				if (lit) {
					o.quad(prevL, prevR, r, l, argb(0.75F * alpha, col));
					o.bar(prevC, center, 0.02F, 0.015F, argb(alpha, 0xFFFFFF), argb(alpha, 0xFFFFFF));
				} else {
					o.quad(prevL, prevR, r, l, argb(0.6F * alpha, VfxDraw.whiten(col, 0.2F)));
				}
			}
			prevL = l;
			prevR = r;
			prevC = center;
		}
		Vector3f grip = new Vector3f(d).mul(-length * 0.32F).add(base);
		if (lit) {
			o.glowLine(base, new Vector3f(d).mul(length).add(base), w * 1.6F, argb(0.35F * alpha, 0xE8D0FF), argb(0.2F * alpha, 0xE8D0FF));
			o.bar(new Vector3f(side).mul(-w * 1.3F).add(base), new Vector3f(side).mul(w * 1.3F).add(base), 0.03F, 0.03F, argb(alpha, GOLD), argb(alpha, GOLD));
			o.bar(base, grip, 0.02F, 0.02F, argb(0.8F * alpha, 0xB070FF), argb(0.8F * alpha, 0xB070FF));
			o.sprite(grip, side, new Vector3f(d).cross(side).normalize(), 0.06F, argb(alpha, GOLD_PALE));
		} else {
			o.bar(base, grip, 0.035F, 0.03F, argb(0.9F * alpha, 0x3A1F5C), argb(0.9F * alpha, 0x3A1F5C));
		}
	}

	/**
	 * A contract's circle round {@code c} breaking apart: cracks run over it, then its eight arcs drift outward ({@code broken} 0..1)
	 * and fade, a rune on each.
	 */
	static void brokenCircle(final VfxOut o, final Vector3f c, final Vector3f a, final Vector3f b, final float r, final float broken, final int seed,
		final float alpha, final int color) {
		int arcs = 8;
		float k = alpha * (1.0F - broken * broken);
		for (int i = 0; i < arcs; i++) {
			float t0 = Mth.TWO_PI * i / arcs + 0.06F;
			float t1 = Mth.TWO_PI * (i + 1) / arcs - 0.06F;
			float tm = (t0 + t1) / 2;
			Vector3f out = new Vector3f(a).mul(Mth.cos(tm)).add(new Vector3f(b).mul(Mth.sin(tm)));
			Vector3f shift = new Vector3f(out).mul(broken * r * (0.6F + 0.6F * hash(seed + i)));
			Vector3f prev = null;
			for (int j = 0; j <= 4; j++) {
				float t = Mth.lerp(j / 4.0F, t0, t1);
				Vector3f q = new Vector3f(a).mul(Mth.cos(t) * r).add(new Vector3f(b).mul(Mth.sin(t) * r)).add(c).add(shift);
				if (prev != null) {
					o.bar(prev, q, 0.03F, 0.03F, argb(0.95F * k, color), argb(0.95F * k, color));
				}
				prev = q;
			}
			Vector3f along = new Vector3f(a).mul(-Mth.sin(tm)).add(new Vector3f(b).mul(Mth.cos(tm)));
			o.rune(new Vector3f(out).mul(r * 0.85F).add(c).add(shift), along, out, r * 0.08F, seed * 7 + i, 0.015F, argb(0.9F * k, PALE));
		}
		// the cracks, white, before it falls apart
		if (broken < 0.5F) {
			float ck = alpha * (1.0F - broken * 2.0F);
			for (int i = 0; i < 5; i++) {
				float t = Mth.TWO_PI * hash(seed * 3 + i);
				Vector3f out = new Vector3f(a).mul(Mth.cos(t)).add(new Vector3f(b).mul(Mth.sin(t)));
				o.bolt(new Vector3f(out).mul(r * 0.2F).add(c), new Vector3f(out).mul(r * 1.15F).add(c), seed + i, r * 0.08F, 4, 0.06F, argb(0.4F * ck, PALE),
					argb(0.95F * ck, 0xFFFFFF));
			}
		}
	}

	/**
	 * Rule Breaker: the dagger stabs into the target and every contract on it breaks: a flash, rainbow cracks of light over the body and
	 * a circle round it falling to pieces, shards flying.
	 */
	private static void ruleBreaker(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float h = Math.max(0.6F, s.size);
		float w = Math.max(0.4F, s.arg);
		float p = s.progress;
		float stab = ease(p, 0.0F, 0.16F);
		float broken = ease(p, 0.2F, 0.75F);
		float fade = 1.0F - ease(p, 0.7F, 1.0F);
		Vector3f mid = new Vector3f(0, h * 0.55F, 0);
		// stabbed down into the body from above and in front, so the blade stands out over the target's shoulders
		Vector3f side = VfxOut.across(s.forward);
		Vector3f f = new Vector3f(s.forward).mul(0.55F).add(0, -0.85F, 0).add(new Vector3f(side).mul(0.3F)).normalize();
		Vector3f plane = new Vector3f(side);
		float length = Math.max(1.5F, h * 0.95F);
		Vector3f tip = new Vector3f(f).mul(-(w * 0.5F + length * 0.9F) * (1.0F - stab) + w * 0.25F).add(mid);
		Vector3f base = new Vector3f(f).mul(-length).add(tip);
		float time = s.ageInTicks;
		shade(ps, c, o -> dagger(o, base, f, plane, length, fade, false));
		glow(ps, c, o -> {
			dagger(o, base, f, plane, length, fade, true);
			if (stab < 1.0F) {
				return;
			}
			float flash = 1.0F - ease(p, 0.16F, 0.4F);
			o.sprite(mid, s.camRight, s.camUp, 0.4F + 1.4F * flash, argb(0.9F * flash, 0xFFFFFF));
			o.star(mid, s.camRight, s.camUp, 0.6F + 2.0F * flash, 8, 0.2F, time * 0.05F, argb(0.9F * fade, 0xFFFFFF), argb(0.0F, PALE));
			// rainbow cracks running over the body
			for (int i = 0; i < 7; i++) {
				float a = Mth.TWO_PI * i / 7 + hash(s.seed + i);
				Vector3f out = new Vector3f(s.camRight).mul(Mth.cos(a)).add(new Vector3f(s.camUp).mul(Mth.sin(a) * 1.3F));
				Vector3f end = new Vector3f(out).mul((w * 0.5F + h * 0.45F) * ease(p, 0.16F, 0.45F)).add(mid);
				o.bolt(mid, end, s.seed * 5 + i, 0.15F, 5, 0.09F, argb(0.55F * fade, RAINBOW[i]), argb(0.95F * fade, 0xFFFFFF));
			}
			brokenCircle(o, mid, X, Z, w * 0.6F + 0.7F, broken, s.seed, fade, s.color);
			// shards of the broken spells flying off
			for (int i = 0; i < 12; i++) {
				Vector3f d = BladeLooks.shardDir(s.seed, i);
				Vector3f at = new Vector3f(d).mul(0.3F + 2.2F * broken).add(mid);
				o.diamond(at, d, VfxOut.across(d), 0.25F * (1.0F - broken) + 0.08F, 0.1F, argb(0.2F * fade, RAINBOW[i % 7]), argb(0.9F * fade, RAINBOW[i % 7]));
			}
		});
	}

	/**
	 * All Spells Must Be Broken: Rule Breaker thrust out at the hand, a white line of its power running down the beam with rainbow bands
	 * winding round it, and every contract circle it passes shattering.
	 */
	private static void spellBreak(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float extend = ease(p, 0.0F, 0.2F);
		float fade = 1.0F - ease(p, 0.6F, 1.0F);
		float time = s.ageInTicks;
		Vector3f dir = new Vector3f(s.reach);
		if (dir.lengthSquared() < 1.0E-6F) {
			dir.set(s.forward);
		}
		dir.normalize();
		Vector3f u = VfxOut.across(dir);
		Vector3f v = new Vector3f(dir).cross(u).normalize();
		float length = Math.max(1.0F, s.size * extend);
		float start = 0.9F;
		Vector3f hand = new Vector3f(dir).mul(0.2F).add(new Vector3f(u).mul(0.3F));
		shade(ps, c, o -> dagger(o, hand, dir, v, 1.6F, fade, false));
		glow(ps, c, o -> {
			dagger(o, hand, dir, v, 1.6F, fade, true);
			Vector3f from = new Vector3f(dir).mul(start);
			Vector3f tip = new Vector3f(dir).mul(length);
			o.glowLine(from, tip, 0.55F, argb(0.3F * fade, s.color), argb(0.2F * fade, s.color));
			o.bar(from, tip, 0.05F, 0.03F, argb(fade, 0xFFFFFF), argb(0.9F * fade, PALE));
			int steps = Math.min(64, Math.max(6, (int)(length * 3)));
			for (int band = 0; band < RAINBOW.length; band++) {
				Vector3f prev = null;
				for (int i = 0; i <= steps; i++) {
					float d = start + (length - start) * i / steps;
					float a = band * Mth.TWO_PI / RAINBOW.length + d * 1.1F - time * 0.7F;
					float r = 0.32F + 0.06F * Mth.sin(d * 2.0F + band);
					Vector3f q = new Vector3f(dir).mul(d).add(new Vector3f(u).mul(Mth.cos(a) * r)).add(new Vector3f(v).mul(Mth.sin(a) * r));
					if (prev != null) {
						o.bar(prev, q, 0.025F, 0.025F, argb(0.9F * fade, RAINBOW[band]), argb(0.9F * fade, RAINBOW[band]));
					}
					prev = q;
				}
			}
			// contracts on the way, shattering once the line has passed them
			for (int i = 0; i < 4; i++) {
				float d = start + (s.size - start) * (0.25F + 0.2F * i);
				if (d > length) {
					continue;
				}
				float broken = ease(p, 0.15F + 0.08F * i, 0.6F + 0.08F * i);
				brokenCircle(o, new Vector3f(dir).mul(d), u, v, 1.1F, broken, s.seed + i * 13, fade, s.color);
				for (int k = 0; k < 6; k++) {
					Vector3f sd = BladeLooks.shardDir(s.seed + i, k);
					Vector3f at = new Vector3f(sd).mul(0.4F + 2.0F * broken).add(new Vector3f(dir).mul(d));
					o.diamond(at, sd, VfxOut.across(sd), 0.22F, 0.09F, argb(0.2F * fade, RAINBOW[(i + k) % 7]), argb(0.85F * fade * (1 - broken * 0.5F),
						RAINBOW[(i + k) % 7]));
				}
			}
			o.star(tip, s.camRight, s.camUp, 1.4F, 8, 0.25F, time * 0.1F, argb(0.85F * fade, 0xFFFFFF), argb(0.0F, PALE));
		});
	}

	// ------------------------------------------------------------ the witch's own magecraft

	/**
	 * High-Speed Divine Words: runic syllables race round the caster on three orbits, streaks behind them, and a small circle flashes
	 * open in front of them at every word.
	 */
	private static void divineWords(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float age = s.age();
		float open = ease(p, 0.0F, 0.12F);
		float fade = 1.0F - ease(p, 0.75F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		Vector3f ahead = ahead(s);
		Vector3f right = VfxOut.across(ahead);
		glow(ps, c, o -> {
			int orbits = s.selfView ? 2 : 3;
			for (int j = 0; j < orbits; j++) {
				float r = (s.selfView ? 1.4F : 0.85F) + 0.12F * j;
				float speed = (0.32F + 0.08F * j) * (j % 2 == 0 ? 1.0F : -1.0F);
				for (int i = 0; i < 8; i++) {
					float a = time * speed + Mth.TWO_PI * i / 8 + j;
					float y = 0.45F + 0.55F * j + 0.18F * Mth.sin(a * 2.0F + j);
					Vector3f at = polar(a, r, y);
					Vector3f along = new Vector3f(-Mth.sin(a), 0, Mth.cos(a));
					o.rune(at, along, UP, 0.15F, s.seed * 7 + j * 8 + i + (int)(time * 0.25F), 0.02F, argb(k, PALE));
					o.sprite(at, s.camRight, s.camUp, 0.16F, argb(0.35F * k, s.color));
					Vector3f back = polar(a - Math.signum(speed) * 0.55F, r, y);
					o.glowLine(at, back, 0.06F, argb(0.5F * k, s.color), argb(0.0F, s.color));
				}
			}
			// a word: a small circle flashing open in front of the caster
			for (int w = 0; w < 6; w++) {
				float local = (age - w * 5.0F) / 8.0F;
				if (local < 0.0F || local > 1.0F) {
					continue;
				}
				float side = (hash(s.seed + w * 3) - 0.5F) * 1.4F;
				float y = 0.9F + 0.8F * hash(s.seed + w * 7);
				Vector3f at = new Vector3f(ahead).mul(s.selfView ? 1.5F : 0.9F).add(new Vector3f(right).mul(side)).add(0, y, 0);
				float r = 0.35F * ease(local, 0.0F, 0.3F);
				float a = fade * (1.0F - ease(local, 0.5F, 1.0F));
				o.magicCircle(at, right, UP, r, time * 0.3F, s.seed + w, 6, argb(0.95F * a, s.color), argb(0.9F * a, PALE));
			}
			o.ring(new Vector3f(0, 0.14F, 0), 0.7F, 1.05F, argb(0.5F * k, s.color));
		});
	}

	/**
	 * Argon Coin: the golden fleece laid over the caster's shoulders, its curls glinting, warm golden light rising round them and a ring
	 * of it on the ground.
	 */
	private static void goldenFleece(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.15F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		float yaw = -s.bodyYaw * Mth.DEG_TO_RAD;
		ps.pushPose();
		ps.rotate(Axis.YP.rotation(yaw));
		Vector3f camRight = new Vector3f(s.camRight).rotateY(-yaw);
		Vector3f camUp = new Vector3f(s.camUp).rotateY(-yaw);
		int cols = 7;
		int rows = 5;
		// the fleece in the body's frame: +z ahead, so it hangs round the back (-z), flaring towards the hem
		Vector3f[][] at = new Vector3f[cols + 1][rows + 1];
		for (int i = 0; i <= cols; i++) {
			float phi = -1.35F + 2.7F * i / cols;
			for (int j = 0; j <= rows; j++) {
				float t = (float)j / rows;
				float flare = 1.0F + 0.35F * t;
				float sway = 0.04F * Mth.sin(time * 0.15F + i * 0.7F) * t;
				at[i][j] = new Vector3f(Mth.sin(phi) * 0.42F * flare, 1.5F - 0.95F * t * open, -Mth.cos(phi) * 0.36F * flare - sway);
			}
		}
		if (!s.selfView) {
			shade(ps, c, o -> {
				for (int i = 0; i < cols; i++) {
					for (int j = 0; j < rows; j++) {
						o.quad(at[i][j], at[i + 1][j], at[i + 1][j + 1], at[i][j + 1], argb(0.55F * k, j % 2 == 0 ? 0xC8901E : 0xB07A14));
					}
				}
			});
		}
		glow(ps, c, o -> {
			if (!s.selfView) {
				// curls: little spirals of gold over the fleece
				for (int i = 0; i < cols; i++) {
					for (int j = 0; j < rows; j++) {
						Vector3f mid = new Vector3f(at[i][j]).add(at[i + 1][j + 1]).mul(0.5F);
						Vector3f out = new Vector3f(mid.x, 0, mid.z).normalize().mul(0.03F);
						mid.add(out);
						Vector3f along = new Vector3f(at[i + 1][j]).sub(at[i][j]).normalize();
						float spin = time * 0.05F + i + j;
						Vector3f prev = null;
						for (int q = 0; q <= 7; q++) {
							float t = (float)q / 7;
							float a = spin + t * Mth.TWO_PI * 1.2F;
							float r = 0.075F * t;
							Vector3f pt = new Vector3f(along).mul(Mth.cos(a) * r).add(new Vector3f(UP).mul(Mth.sin(a) * r)).add(mid);
							if (prev != null) {
								o.bar(prev, pt, 0.012F, 0.012F, argb(0.9F * k, GOLD_PALE), argb(0.9F * k, GOLD_PALE));
							}
							prev = pt;
						}
					}
				}
				o.sprite(new Vector3f(0, 1.1F, -0.3F), camRight, camUp, 1.3F, argb(0.3F * k, GOLD));
			}
			for (int i = 0; i < 14; i++) {
				float rise = (time * 0.03F + hash(i * 5 + s.seed)) % 1.0F;
				Vector3f m = polar(Mth.TWO_PI * hash(i * 11) + rise, 0.5F + 0.4F * hash(i), rise * 2.4F);
				if (s.selfView && m.y > 1.0F) {
					continue;
				}
				o.glint(m, camRight, camUp, 0.14F, time * 0.1F + i, argb(0.9F * k * Mth.sin(Mth.PI * rise), GOLD_PALE));
			}
			o.ring(new Vector3f(0, 0.14F, 0), 0.5F, 1.3F, argb(0.6F * k, GOLD));
			o.circle(new Vector3f(0, 0.15F, 0), X, Z, 1.35F, 0.03F, 32, argb(0.8F * k, GOLD_PALE));
		});
		ps.popPose();
	}

	/** Witch's Dagger: a violet arc swept across the front with the dagger at its edge, runes left glowing in its wake. */
	private static void witchSlash(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float sweep = ease(p, 0.0F, 0.45F);
		float fade = 1.0F - ease(p, 0.45F, 1.0F);
		Vector3f f = new Vector3f(s.reach.x(), 0, s.reach.z());
		if (f.lengthSquared() < 1.0E-6F) {
			f.set(s.forward);
		}
		f.normalize();
		Vector3f right = VfxOut.across(f);
		float r = Math.max(1.5F, s.size);
		float half = Math.max(30.0F, s.arg) * 0.5F * Mth.DEG_TO_RAD;
		float lead = -half + 2.0F * half * sweep;
		glow(ps, c, o -> {
			int steps = 22;
			float from = Math.max(-half, lead - 2.2F);
			for (int i = 0; i < steps; i++) {
				float t0 = Mth.lerp((float)i / steps, from, lead);
				float t1 = Mth.lerp((float)(i + 1) / steps, from, lead);
				float a0 = fade * (float)i / steps;
				float a1 = fade * (float)(i + 1) / steps;
				Vector3f i0 = arcPoint(f, right, t0, r * 0.35F);
				Vector3f o0 = arcPoint(f, right, t0, r);
				Vector3f i1 = arcPoint(f, right, t1, r * 0.35F);
				Vector3f o1 = arcPoint(f, right, t1, r);
				o.quad(i0, o0, o1, i1, argb(0.0F, s.color), argb(0.75F * a0, s.color), argb(0.75F * a1, s.color), argb(0.0F, s.color));
				o.bar(o0, o1, 0.035F, 0.035F, argb(0.95F * a0, PALE), argb(0.95F * a1, PALE));
			}
			// the dagger at the leading edge
			Vector3f d = arcPoint(f, right, lead, 1.0F);
			dagger(o, arcPoint(f, right, lead, r * 0.45F), d, UP, r * 0.5F, fade, true);
			for (int i = 0; i < 7; i++) {
				float t = -half + 2.0F * half * (i + 0.5F) / 7;
				if (t > lead) {
					continue;
				}
				Vector3f at = arcPoint(f, right, t, r * 0.72F);
				Vector3f along = arcPoint(f, right, t + Mth.HALF_PI, 1.0F);
				o.rune(at.add(0, 0.25F, 0), along, UP, 0.16F, s.seed * 11 + i, 0.02F, argb(0.9F * fade, PALE));
			}
		});
	}

	/** A point of the slash's arc: {@code theta} round from straight ahead towards the right, {@code r} out, leaning a little (a diagonal cut). */
	private static Vector3f arcPoint(final Vector3f f, final Vector3f right, final float theta, final float r) {
		return new Vector3f(f).mul(Mth.cos(theta) * r).add(new Vector3f(right).mul(Mth.sin(theta) * r)).add(0, -Mth.sin(theta) * r * 0.22F, 0);
	}

	/**
	 * Territory Creation: the witch's ward drawn on the ground round the caster (a pentagram circle with a smaller one turning inside), a
	 * wall of violet light rising on its rim, five pillars of light at its points and motes rising.
	 */
	private static void territory(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.2F);
		float fade = 1.0F - ease(p, 0.75F, 1.0F);
		float k = open * fade;
		float r = s.size * open;
		float time = s.ageInTicks;
		shade(ps, c, o -> o.disc(new Vector3f(0, 0.13F, 0), r, argb(0.25F * k, DUSK), argb(0.1F * k, DUSK)));
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			o.magicCircle(ground, X, Z, r, time * 0.015F, s.seed, 5, argb(0.9F * k, s.color), argb(0.8F * k, PALE));
			o.magicCircle(new Vector3f(0, 0.16F, 0), X, Z, r * 0.3F, -time * 0.04F, s.seed + 1, 6, argb(0.9F * k, s.color), argb(0.8F * k, PALE));
			o.wall(new Vector3f(0, 0.1F, 0), r, 0.0F, 2.6F * open, argb(0.6F * k, s.color), argb(0.0F, s.color));
			for (int i = 0; i < 5; i++) {
				float a = time * 0.015F + Mth.TWO_PI * i / 5;
				Vector3f foot = polar(a, r * 0.84F, 0.15F);
				o.glowLine(foot, new Vector3f(foot).add(0, 3.2F * open, 0), 0.25F, argb(0.6F * k, s.color), argb(0.0F, s.color));
				o.sprite(new Vector3f(foot).add(0, 0.3F, 0), s.camRight, s.camUp, 0.5F, argb(0.7F * k, PALE));
			}
			for (int i = 0; i < 24; i++) {
				float rise = (time * 0.025F + hash(i * 7 + s.seed)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 3), r * (0.9F + 0.1F * hash(i)), rise * 3.0F);
				o.glint(at, s.camRight, s.camUp, 0.2F, time * 0.1F + i, argb(0.9F * k * Mth.sin(Mth.PI * rise), PALE));
			}
		});
	}

	private WitchLooks() {
	}
}
