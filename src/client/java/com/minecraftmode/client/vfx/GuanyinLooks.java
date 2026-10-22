package com.minecraftmode.client.vfx;

import static com.minecraftmode.client.vfx.SkillVfxRenderer.glow;
import static com.minecraftmode.client.vfx.SkillVfxRenderer.shade;
import static com.minecraftmode.client.vfx.VfxOut.argb;
import static com.minecraftmode.client.vfx.VfxOut.ease;
import static com.minecraftmode.client.vfx.VfxOut.hash;
import static com.minecraftmode.client.vfx.VfxOut.polar;

import com.minecraftmode.client.vfx.SkillVfxRenderer.State;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Looks of Chairman Netero's Hundred-Type Guanyin Bodhisattva (Hunter x Hunter): his prayer, the Bodhisattva's giant golden palms (the
 * Third Hand's sweep and the Ninety-Ninth Hand's blows) and the Zero Hand: the golden Guanyin rising behind him, its hands closing on the
 * ground before it, and the beam of aura it lets loose.
 */
final class GuanyinLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final Vector3f DOWN = new Vector3f(0, -1, 0);
	private static final int GOLD = 0xFFD060;
	private static final int GOLD_PALE = 0xFFF4C8;
	private static final int GOLD_DEEP = 0xE09A20;
	private static final int PALM_LIT = 0xFFE8A0;
	private static final int PALM_DARK = 0xB07A18;
	/** How much bigger than its ten blocks the Zero Hand's Guanyin stands. */
	private static final float STATUE = 1.25F;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case NETERO_PRAYER -> prayer(s, ps, c);
			case GUANYIN_SWEEP -> thirdHand(s, ps, c);
			case PALM_SLAM -> palmSlam(s, ps, c);
			case ZERO_HAND -> zeroHand(s, ps, c);
			case ZERO_BEAM -> zeroBeam(s, ps, c);
			default -> {
			}
		}
	}

	/**
	 * The Hundred-Type Guanyin Bodhisattva of light, {@code scale} times its full height (about ten blocks), seated at {@code base} facing
	 * {@code f}: a lotus under it, the robed body, the head with its topknot before a great halo of rays, the hands pressed together in prayer
	 * at its chest and a fan of arms spread behind it, each ending in an open palm. Faded by {@code alpha}; glow buffer (the praying hands
	 * are {@link #prayingHands}).
	 */
	private static void guanyin(final VfxOut o, final State s, final Vector3f base, final Vector3f f, final float scale, final float alpha, final float time) {
		Vector3f side = VfxOut.across(f);
		float k = scale;
		int line = argb(0.75F * alpha, GOLD);
		int pale = argb(0.85F * alpha, GOLD_PALE);
		int fill = argb(0.12F * alpha, GOLD);
		// the lotus
		for (int i = 0; i < 12; i++) {
			float a = Mth.TWO_PI * i / 12;
			Vector3f out = polar(a, 1.0F, 0.0F);
			Vector3f tip = new Vector3f(out).mul(2.3F * k).add(0, 0.7F * k, 0).add(base);
			o.blade(new Vector3f(out).mul(0.6F * k).add(base).add(0, 0.2F * k, 0), tip, polar(a + Mth.HALF_PI, 1.0F, 0.0F), 0.45F * k, line, argb(0.3F * alpha, GOLD_PALE));
		}
		// the robed body: a trapezoid of light from the knees to the shoulders
		Vector3f kneeL = local(base, f, side, k, -2.3F, 0.9F, 0.2F);
		Vector3f kneeR = local(base, f, side, k, 2.3F, 0.9F, 0.2F);
		Vector3f shL = local(base, f, side, k, -1.45F, 5.2F, 0.0F);
		Vector3f shR = local(base, f, side, k, 1.45F, 5.2F, 0.0F);
		Vector3f waistL = local(base, f, side, k, -1.2F, 3.0F, 0.1F);
		Vector3f waistR = local(base, f, side, k, 1.2F, 3.0F, 0.1F);
		o.quad(kneeL, kneeR, waistR, waistL, fill);
		o.quad(waistL, waistR, shR, shL, fill);
		o.glowLine(kneeL, waistL, 0.09F * k, line, line);
		o.glowLine(waistL, shL, 0.09F * k, line, line);
		o.glowLine(kneeR, waistR, 0.09F * k, line, line);
		o.glowLine(waistR, shR, 0.09F * k, line, line);
		o.glowLine(kneeL, kneeR, 0.09F * k, line, line);
		o.glowLine(shL, shR, 0.07F * k, line, line);
		// the folds of the robe
		for (int i = 0; i < 4; i++) {
			float x = -0.9F + 0.6F * i;
			o.glowLine(local(base, f, side, k, x * 0.8F, 4.6F, 0.05F), local(base, f, side, k, x * 1.4F, 1.1F, 0.2F), 0.03F * k, argb(0.4F * alpha, GOLD), argb(0.4F * alpha, GOLD));
		}
		// the head, the topknot and the halo of rays behind it
		Vector3f head = local(base, f, side, k, 0.0F, 6.3F, 0.05F);
		Vector3f halo = local(base, f, side, k, 0.0F, 6.3F, -0.35F);
		o.circle(halo, side, UP, 2.0F * k, 0.08F * k, 40, line);
		o.circle(halo, side, UP, 1.7F * k, 0.04F * k, 40, argb(0.5F * alpha, GOLD));
		for (int i = 0; i < 24; i++) {
			float a = Mth.TWO_PI * i / 24 + time * 0.01F;
			Vector3f d = new Vector3f(side).mul(Mth.cos(a)).add(new Vector3f(UP).mul(Mth.sin(a)));
			o.glowLine(new Vector3f(d).mul(1.0F * k).add(halo), new Vector3f(d).mul(1.65F * k).add(halo), 0.03F * k, argb(0.5F * alpha, GOLD_PALE), argb(0.1F * alpha, GOLD));
		}
		o.sprite(halo, s.camRight, s.camUp, 2.2F * k, argb(0.15F * alpha, GOLD));
		o.circle(head, side, UP, 0.75F * k, 0.07F * k, 24, pale);
		o.plate(head, side, UP, 0.72F * k, 20, argb(0.18F * alpha, GOLD));
		o.circle(local(base, f, side, k, 0.0F, 7.25F, 0.05F), side, UP, 0.32F * k, 0.06F * k, 16, pale);
		// the closed eyes and the mark on the brow
		for (int e = -1; e <= 1; e += 2) {
			Vector3f eye = local(base, f, side, k, e * 0.28F, 6.38F, 0.12F);
			o.glowLine(new Vector3f(eye).add(new Vector3f(side).mul(-0.13F * k)), new Vector3f(eye).add(new Vector3f(side).mul(0.13F * k)).add(0, -0.03F * k, 0), 0.03F * k,
				pale, pale);
		}
		o.sprite(local(base, f, side, k, 0.0F, 6.62F, 0.12F), s.camRight, s.camUp, 0.09F * k, argb(alpha, 0xFFFFFF));
		// the hundred arms fanned out behind, each with an open palm of light
		for (int row = 0; row < 2; row++) {
			int n = row == 0 ? 9 : 11;
			for (int i = 0; i < n; i++) {
				float a = Mth.PI * (0.06F + 0.88F * i / (n - 1));
				float reach = (row == 0 ? 3.6F : 4.8F) * k;
				Vector3f root = local(base, f, side, k, Mth.cos(a) * 1.2F, 4.6F + 0.4F * row, -0.4F - 0.3F * row);
				Vector3f out = new Vector3f(side).mul(Mth.cos(a)).add(new Vector3f(UP).mul(Mth.sin(a) * 0.9F + 0.15F));
				Vector3f hand = new Vector3f(out).mul(reach).add(root).add(0, Mth.sin(time * 0.05F + i + row) * 0.1F * k, 0);
				o.glowLine(root, hand, 0.07F * k, argb(0.5F * alpha, GOLD), argb(0.65F * alpha, GOLD_PALE));
				o.sprite(hand, s.camRight, s.camUp, 0.32F * k, argb(0.6F * alpha, GOLD_PALE));
				o.glowLine(hand, new Vector3f(out).mul(0.5F * k).add(hand), 0.12F * k, argb(0.6F * alpha, GOLD_PALE), argb(0.0F, GOLD));
			}
		}
	}

	/** A point {@code x} across, {@code y} up and {@code z} forward (scaled by {@code k}) of something standing at {@code base} facing {@code f}. */
	private static Vector3f local(final Vector3f base, final Vector3f f, final Vector3f side, final float k, final float x, final float y, final float z) {
		return new Vector3f(side).mul(x * k).add(new Vector3f(f).mul(z * k)).add(0, y * k, 0).add(base);
	}

	/** Two palms pressed together in prayer at {@code at}, fingers up, the palms facing across {@code f}: shade buffer. */
	private static void prayingHands(final VfxOut o, final Vector3f at, final Vector3f f, final float size, final float alpha) {
		Vector3f side = VfxOut.across(f);
		for (int k = -1; k <= 1; k += 2) {
			Vector3f c = new Vector3f(side).mul(k * size * 0.09F).add(at);
			o.palm(c, new Vector3f(side).mul(-k), new Vector3f(UP).add(new Vector3f(f).mul(0.15F)), size, argb(0.97F * alpha, PALM_LIT), argb(0.97F * alpha, PALM_DARK));
		}
	}

	/**
	 * Prayer: Netero presses his hands together; behind him the Bodhisattva glimmers into sight, faint, its halo glowing, and motes of gold
	 * rise round him. In first person only the motes.
	 */
	private static void prayer(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float open = ease(age, 0.0F, 10.0F);
		float fade = 1.0F - ease(p, 0.75F, 1.0F);
		float k = open * fade;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f hands = new Vector3f(ahead).mul(0.42F).add(0, 1.25F, 0);
		shade(ps, c, o -> {
			if (!s.selfView) {
				prayingHands(o, hands, ahead, 0.36F, k);
			}
		});
		glow(ps, c, o -> {
			if (!s.selfView) {
				guanyin(o, s, new Vector3f(ahead).mul(-1.6F).add(0, -0.4F, 0), ahead, 0.42F, 0.55F * k, time);
				o.sprite(hands, s.camRight, s.camUp, 0.45F, argb(0.5F * k, GOLD));
			}
			o.ring(new Vector3f(0, 0.15F, 0), 0.8F, 1.25F, argb(0.4F * k, GOLD));
			for (int i = 0; i < 12; i++) {
				float q = (time * 0.05F + hash(s.seed + i)) % 1.0F;
				o.glint(polar(Mth.TWO_PI * hash(i * 7) + time * 0.03F, 0.7F + 0.4F * hash(i * 3), 0.2F + 2.4F * q), s.camRight, s.camUp, 0.12F, i,
					argb(0.85F * k * (1.0F - q), GOLD_PALE));
			}
		});
	}

	/**
	 * Third Hand: the Bodhisattva's giant golden palm sweeps along the arc in front of the caster, its fingertips at the edge of what it hits,
	 * a fan of light left behind it and the arc's rim lit.
	 */
	private static void thirdHand(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = Math.max(1.0F, s.size);
		float arc = Math.max(10.0F, s.arg) * Mth.DEG_TO_RAD;
		float fade = 1.0F - ease(p, 0.6F, 1.0F);
		float sweep = ease(age, 0.0F, 7.0F);
		Vector3f fwd = new Vector3f(s.forward);
		float yaw0 = (float)Mth.atan2(fwd.z, fwd.x);
		float from = yaw0 - arc / 2;
		float now = from + arc * sweep;
		float size = r * 0.75F;
		float centre = r - 0.49F * size;
		float y = -0.4F;
		Vector3f out = polar(now, 1.0F, 0.0F);
		Vector3f tang = polar(now + Mth.HALF_PI, 1.0F, 0.0F);
		Vector3f palmAt = new Vector3f(out).mul(centre).add(0, y, 0);
		// the palm leads the sweep, tipped up so it shows its face from above as well as from the side
		Vector3f face = new Vector3f(tang).add(0, 1.1F, 0).normalize();
		shade(ps, c, o -> o.palm(palmAt, face, out, size, argb(0.92F * fade, PALM_LIT), argb(0.92F * fade, PALM_DARK)));
		glow(ps, c, o -> {
			int steps = 18;
			for (int i = 0; i < steps; i++) {
				float a0 = from + arc * sweep * i / steps;
				float a1 = from + arc * sweep * (i + 1) / steps;
				float t = (float)i / steps;
				int col = argb(0.3F * fade * (0.3F + 0.7F * t), GOLD);
				o.quad(polar(a0, r * 0.3F, y), polar(a0, r, y), polar(a1, r, y), polar(a1, r * 0.3F, y), argb(0.0F, GOLD), col, col, argb(0.0F, GOLD));
				o.glowLine(polar(a0, r, y), polar(a1, r, y), 0.12F, argb(0.85F * fade * (0.4F + 0.6F * t), GOLD_PALE), argb(0.85F * fade * (0.4F + 0.6F * t), GOLD_PALE));
			}
			// the whole arc it covers, faint
			for (int i = 0; i < 20; i++) {
				float a0 = from + arc * i / 20;
				float a1 = from + arc * (i + 1) / 20;
				o.glowLine(polar(a0, r, y), polar(a1, r, y), 0.05F, argb(0.3F * fade, GOLD), argb(0.3F * fade, GOLD));
			}
			o.glowLine(polar(from, 0.5F, y), polar(from, r, y), 0.05F, argb(0.3F * fade, GOLD), argb(0.3F * fade, GOLD));
			o.glowLine(polar(from + arc, 0.5F, y), polar(from + arc, r, y), 0.05F, argb(0.3F * fade, GOLD), argb(0.3F * fade, GOLD));
			o.sprite(palmAt, s.camRight, s.camUp, size * 0.6F, argb(0.3F * fade, GOLD));
		});
	}

	/**
	 * Ninety-Ninth Hand (one blow of six): a giant golden palm comes down flat from above onto the ground near the caster, a print of light
	 * where it lands and a ring of shock running out to the edge of what the blow hits.
	 */
	private static void palmSlam(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = Math.max(1.0F, s.size);
		float fall = ease(age, 0.0F, 3.0F);
		float fade = 1.0F - ease(p, 0.5F, 1.0F);
		float shock = ease(age, 2.0F, 8.0F);
		float a = Mth.TWO_PI * hash(s.seed);
		float d = r * 0.4F * hash(s.seed * 3 + 1);
		Vector3f out = polar(a, 1.0F, 0.0F);
		float size = 2.6F;
		Vector3f spot = polar(a, d, 0.0F);
		Vector3f palmAt = new Vector3f(spot).add(0, 0.35F + 5.0F * (1.0F - fall), 0);
		shade(ps, c, o -> o.palm(palmAt, DOWN, out, size, argb(0.92F * fade, PALM_LIT), argb(0.92F * fade, PALM_DARK)));
		glow(ps, c, o -> {
			o.sprite(palmAt, s.camRight, s.camUp, size * 0.7F, argb(0.3F * fade, GOLD));
			if (fall >= 1.0F) {
				o.disc(new Vector3f(spot).add(0, 0.14F, 0), size * 0.55F, argb(0.5F * fade, GOLD_PALE), argb(0.1F * fade, GOLD));
				o.ring(new Vector3f(0, 0.16F, 0), Math.max(0.0F, r * shock - 0.6F), r * shock, argb(0.75F * (1.0F - shock * 0.5F) * fade, GOLD_PALE));
			}
			o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, r - 0.3F), r, argb(0.75F * fade, GOLD));
			o.ring(new Vector3f(0, 0.155F, 0), Math.max(0.0F, r - 0.06F), r, argb(0.5F * fade, 0xFFFFFF));
		});
	}

	/**
	 * Zero Hand: the golden Hundred-Type Guanyin rises behind the caster, towering over them; its two great hands come round to the aimed
	 * ground and stand at the edge of the field it holds, palms in, while a wall of gold light rises along that edge.
	 */
	private static void zeroHand(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float r = Math.max(1.0F, s.size);
		float rise = ease(age, 0.0F, 12.0F);
		float close = ease(age, 8.0F, 20.0F);
		float fade = 1.0F - ease(p, 0.85F, 1.0F);
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		float away = Math.max(0.0F, s.arg);
		Vector3f caster = new Vector3f(f).mul(-away);
		Vector3f base = new Vector3f(caster).add(new Vector3f(f).mul(-2.5F)).add(0, -3.0F * (1.0F - rise), 0);
		float size = 4.6F;
		Vector3f[] palms = new Vector3f[2];
		Vector3f[] faces = new Vector3f[2];
		Vector3f[] fingers = new Vector3f[2];
		for (int i = 0; i < 2; i++) {
			int k = i == 0 ? -1 : 1;
			Vector3f start = local(base, f, side, STATUE, k * 4.0F, 5.0F, 0.0F);
			Vector3f end = new Vector3f(side).mul(k * r).add(0, 2.2F, 0);
			palms[i] = new Vector3f(start).lerp(end, close).add(0, 1.2F * Mth.sin(Mth.PI * close), 0);
			faces[i] = new Vector3f(side).mul(-k).lerp(new Vector3f(f), 0.15F * (1.0F - close)).normalize();
			fingers[i] = new Vector3f(UP).lerp(new Vector3f(f), 0.25F).normalize();
		}
		shade(ps, c, o -> {
			for (int i = 0; i < 2; i++) {
				o.palm(palms[i], faces[i], fingers[i], size, argb(0.95F * fade * rise, PALM_LIT), argb(0.95F * fade * rise, PALM_DARK));
			}
			if (!s.selfView || away > 4.0F) {
				prayingHands(o, local(base, f, side, STATUE, 0.0F, 4.0F, 0.6F), f, 1.3F * STATUE, 0.95F * fade * rise);
			}
		});
		glow(ps, c, o -> {
			guanyin(o, s, base, f, STATUE, fade * rise, time);
			for (int i = 0; i < 2; i++) {
				o.sprite(palms[i], s.camRight, s.camUp, size * 0.7F, argb(0.3F * fade * rise, GOLD));
				// the arm of light reaching from the statue's shoulder
				o.glowLine(local(base, f, side, STATUE, (i == 0 ? -1 : 1) * 1.45F, 5.2F, 0.0F), new Vector3f(palms[i]).add(new Vector3f(fingers[i]).mul(-size * 0.5F)), 0.4F,
					argb(0.35F * fade * rise, GOLD), argb(0.35F * fade * rise, GOLD));
			}
			o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, r * rise - 0.4F), r * rise, argb(0.85F * fade, GOLD));
			o.ring(new Vector3f(0, 0.16F, 0), Math.max(0.0F, r * rise - 0.08F), r * rise, argb(0.6F * fade, 0xFFFFFF));
			o.wall(new Vector3f(0, 0.1F, 0), r * rise, 0.0F, 2.4F * close, argb(0.28F * fade, GOLD), argb(0.0F, GOLD));
			o.disc(new Vector3f(0, 0.13F, 0), r * rise, argb(0.1F * fade, GOLD_PALE), argb(0.05F * fade, GOLD));
			for (int i = 0; i < 14; i++) {
				float q = (time * 0.05F + hash(s.seed + i)) % 1.0F;
				o.glint(polar(Mth.TWO_PI * hash(i * 5), r * (0.95F + 0.05F * hash(i)), 0.2F + 2.4F * q), s.camRight, s.camUp, 0.18F, i, argb(0.8F * fade * (1.0F - q), GOLD_PALE));
			}
		});
	}

	/**
	 * Zero Hand's beam: a torrent of golden aura runs out along the beam as wide as it hits ({@link PendragonLooks#BEAM_R}), white at its
	 * core, lit lines at its edge and wheels of light rolling along it, a flare at its head.
	 */
	private static void zeroBeam(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float len = Math.max(2.0F, s.size);
		Vector3f dir = PendragonLooks.beamDir(s);
		Vector3f side = VfxOut.across(dir);
		Vector3f up = PendragonLooks.beamUp(dir);
		float fade = 1.0F - ease(p, 0.6F, 1.0F);
		float reach = Math.max(0.0F, (len - 0.8F) * ease(age, 0.0F, 6.0F));
		float r = PendragonLooks.BEAM_R;
		Vector3f start = new Vector3f(dir).mul(0.8F);
		Vector3f front = new Vector3f(dir).mul(0.8F + reach);
		glow(ps, c, o -> {
			if (reach <= 0.0F) {
				return;
			}
			o.glowLine(start, front, r * 1.05F, argb(0.35F * fade, GOLD_DEEP), argb(0.3F * fade, GOLD_DEEP));
			o.glowLine(start, front, r * 0.7F, argb(0.55F * fade, GOLD), argb(0.45F * fade, GOLD));
			o.glowLine(start, front, 0.3F, argb(0.9F * fade, 0xFFFFFF), argb(0.75F * fade, 0xFFFFFF));
			o.bar(start, front, 0.12F, 0.08F, argb(fade, 0xFFFFFF), argb(fade, GOLD_PALE));
			for (int k = 0; k < 4; k++) {
				Vector3f off = new Vector3f(k < 2 ? up : side).mul((k % 2 == 0 ? 1 : -1) * r);
				o.glowLine(new Vector3f(start).add(off), new Vector3f(front).add(off), 0.14F, argb(0.5F * fade, GOLD_PALE), argb(0.5F * fade, GOLD_PALE));
			}
			// wheels of light rolling along it
			for (int i = 0; i < 8; i++) {
				float d = ((i / 8.0F + time * 0.03F) % 1.0F) * reach;
				Vector3f at = new Vector3f(dir).mul(0.8F + d);
				int col = argb(0.6F * fade, GOLD_PALE);
				o.circle(at, side, up, r, 0.05F, 24, col);
				float spin = time * 0.2F + i;
				for (int j = 0; j < 8; j++) {
					float a = spin + Mth.TWO_PI * j / 8;
					Vector3f spoke = new Vector3f(side).mul(Mth.cos(a)).add(new Vector3f(up).mul(Mth.sin(a)));
					o.glowLine(new Vector3f(spoke).mul(r * 0.2F).add(at), new Vector3f(spoke).mul(r).add(at), 0.025F, col, col);
				}
			}
			for (int i = 0; i < 16; i++) {
				float q = (time * 0.05F + hash(s.seed * 5 + i)) % 1.0F;
				float d = 0.8F + reach * hash(i * 13);
				Vector3f at = new Vector3f(dir).mul(d).add(new Vector3f(side).mul((hash(i * 17) - 0.5F) * 1.6F)).add(new Vector3f(up).mul(-0.5F + 2.5F * q));
				o.glint(at, s.camRight, s.camUp, 0.2F, time * 0.1F + i, argb(0.9F * fade * Mth.sin(q * Mth.PI), GOLD_PALE));
			}
			float flare = 1.0F - ease(age, 5.0F, 16.0F);
			if (flare > 0.0F) {
				o.star(front, s.camRight, s.camUp, 1.8F * flare + 0.4F, 10, 0.3F, s.seed, argb(0.9F * flare, 0xFFFFFF), argb(0.0F, GOLD));
			}
			o.sprite(front, s.camRight, s.camUp, r * 1.3F, argb(0.35F * fade, GOLD));
			o.circle(start, side, up, r, 0.06F, 28, argb(0.6F * fade, GOLD_PALE));
		});
	}

	private GuanyinLooks() {
	}
}
