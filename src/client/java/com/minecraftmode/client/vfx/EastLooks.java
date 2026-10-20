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
 * Looks of the East: Tamamo-no-Mae (the sun mirror Yata no Kagami, her fire and ice talismans, fox fire and nine tails) and Zhuge Liang
 * (the Stone Sentinel Maze with the bagua, the east wind of Red Cliffs, the discerning eye, the tactician's banner and feather fan).
 */
final class EastLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final Vector3f X = new Vector3f(1, 0, 0);
	private static final Vector3f Z = new Vector3f(0, 0, 1);
	private static final int PAPER = 0xF4EBD8;
	private static final int INK = 0xA01818;
	private static final int VERMILION = 0xFF4A2A;
	private static final int SUN = 0xFFE6A0;
	private static final int SUN_CORE = 0xFFF8E0;
	private static final int BRONZE = 0x9A6A24;
	private static final int FIRE = 0xFF6A1A;
	private static final int FIRE_CORE = 0xFFD27A;
	private static final int ICE = 0xCFF4FF;
	private static final int ICE_DEEP = 0x6FCBFF;
	private static final int FOX_FIRE = 0x6AB4FF;
	private static final int TAIL = 0xFFB860;
	private static final int SAGE = 0x8FD4FF;
	private static final int GOLD = 0xFFD24A;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case AMATERASU -> amaterasu(s, ps, c);
			case ENTEN -> enten(s, ps, c);
			case HYOTEN -> hyoten(s, ps, c);
			case ICE_OFUDA -> iceOfuda(s, ps, c);
			case FOX_FIRE -> foxFire(s, ps, c);
			case STONE_MAZE -> stoneMaze(s, ps, c);
			case EAST_WIND -> eastWind(s, ps, c);
			case DISCERNING_EYE -> discerningEye(s, ps, c);
			case WAR_BANNER -> warBanner(s, ps, c);
			default -> {
			}
		}
	}

	// ------------------------------------------------------------ ofuda

	/**
	 * An ofuda, a paper talisman {@code w} wide and {@code h} tall at {@code c} in the plane of {@code a} (across) and {@code b} (up). The
	 * shade pass ({@code lit} false) draws the paper and its red ink a hair off both faces; the glow pass a lit edge and the ink glowing
	 * {@code glow}.
	 */
	static void ofuda(final VfxOut o, final Vector3f c, final Vector3f a, final Vector3f b, final float w, final float h, final int seed, final float alpha,
		final boolean lit, final int glow) {
		if (alpha <= 0.01F) {
			return;
		}
		Vector3f ha = new Vector3f(a).normalize(w / 2);
		Vector3f hb = new Vector3f(b).normalize(h / 2);
		Vector3f p0 = new Vector3f(c).sub(ha).sub(hb);
		Vector3f p1 = new Vector3f(c).add(ha).sub(hb);
		Vector3f p2 = new Vector3f(c).add(ha).add(hb);
		Vector3f p3 = new Vector3f(c).sub(ha).add(hb);
		Vector3f n = new Vector3f(a).cross(b).normalize(0.012F);
		if (!lit) {
			o.quad(p0, p1, p2, p3, argb(0.9F * alpha, PAPER));
			for (int side = -1; side <= 1; side += 2) {
				Vector3f off = new Vector3f(n).mul(side);
				ink(o, new Vector3f(c).add(off), a, b, w, h, seed, argb(0.95F * alpha, INK));
			}
		} else {
			int edge = argb(0.8F * alpha, glow);
			o.bar(p0, p1, 0.012F, 0.012F, edge, edge);
			o.bar(p1, p2, 0.012F, 0.012F, edge, edge);
			o.bar(p2, p3, 0.012F, 0.012F, edge, edge);
			o.bar(p3, p0, 0.012F, 0.012F, edge, edge);
			ink(o, new Vector3f(c), a, b, w, h, seed, argb(0.7F * alpha, glow));
		}
	}

	/** The talisman's writing: a band across the top, a stroke down the middle and two characters. */
	private static void ink(final VfxOut o, final Vector3f c, final Vector3f a, final Vector3f b, final float w, final float h, final int seed, final int color) {
		Vector3f na = new Vector3f(a).normalize();
		Vector3f nb = new Vector3f(b).normalize();
		Vector3f top = new Vector3f(nb).mul(h * 0.36F).add(c);
		o.bar(new Vector3f(na).mul(-w * 0.38F).add(top), new Vector3f(na).mul(w * 0.38F).add(top), 0.012F, 0.012F, color, color);
		o.rune(new Vector3f(nb).mul(h * 0.15F).add(c), na, nb, w * 0.32F, seed, 0.012F, color);
		o.rune(new Vector3f(nb).mul(-h * 0.16F).add(c), na, nb, w * 0.32F, seed + 1, 0.012F, color);
		o.bar(new Vector3f(nb).mul(-h * 0.32F).add(c), new Vector3f(nb).mul(-h * 0.45F).add(c), 0.012F, 0.006F, color, color);
	}

	// ------------------------------------------------------------ Tamamo-no-Mae

	/**
	 * Eightfold Blessings of Amaterasu: Yata no Kagami, the eight-lobed sun mirror, shining over a torii of vermilion light behind the
	 * caster, its light falling on them, ofuda circling them and the sun's warmth washing out over everyone in reach.
	 */
	private static void amaterasu(final State s, final PoseStack ps, final SubmitNodeCollector c) {
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
		Vector3f mirror = new Vector3f(0, 3.7F, -1.5F);
		float mr = 1.0F * open;
		float orbit = s.selfView ? 1.7F : 1.3F;
		shade(ps, c, o -> {
			if (!s.selfView) {
				// the bronze back of the mirror, its rim in eight lobes
				int n = 48;
				for (int i = 0; i < n; i++) {
					float t0 = Mth.TWO_PI * i / n;
					float t1 = Mth.TWO_PI * (i + 1) / n;
					o.tri(mirror, lobe(mirror, t0, mr * 1.08F), lobe(mirror, t1, mr * 1.08F), argb(0.75F * k, BRONZE), argb(0.75F * k, 0x6A4414),
						argb(0.75F * k, 0x6A4414));
				}
				torii(o, -1.45F, k, false);
			}
			for (int i = 0; i < 8; i++) {
				float a = time * 0.06F + Mth.TWO_PI * i / 8;
				Vector3f at = polar(a, orbit, 1.0F + 0.2F * Mth.sin(time * 0.1F + i));
				ofuda(o, at, new Vector3f(-Mth.sin(a), 0, Mth.cos(a)), UP, 0.22F, 0.55F, s.seed + i * 3, k, false, VERMILION);
			}
		});
		glow(ps, c, o -> {
			if (!s.selfView) {
				// the mirror's face: the sun in it, rays turning, the lobed rim and its bosses
				o.sprite(mirror, X, UP, mr * 0.85F, argb(0.95F * k, SUN_CORE));
				o.sprite(mirror, X, UP, mr * 2.6F, argb(0.35F * k, SUN));
				o.star(new Vector3f(mirror).add(0, 0, 0.02F), X, UP, mr * 2.4F, 16, 0.25F, time * 0.01F, argb(0.7F * k, SUN_CORE), argb(0.0F, SUN));
				Vector3f prev = null;
				for (int i = 0; i <= 48; i++) {
					Vector3f q = lobe(mirror, Mth.TWO_PI * i / 48, mr * 1.08F).add(0, 0, 0.03F);
					if (prev != null) {
						o.bar(prev, q, 0.035F, 0.035F, argb(0.95F * k, GOLD), argb(0.95F * k, GOLD));
					}
					prev = q;
				}
				o.circle(new Vector3f(mirror).add(0, 0, 0.03F), X, UP, mr * 0.7F, 0.02F, 32, argb(0.8F * k, GOLD));
				for (int i = 0; i < 8; i++) {
					float t = Mth.TWO_PI * i / 8;
					o.sprite(new Vector3f(mirror).add(Mth.cos(t) * mr * 0.88F, Mth.sin(t) * mr * 0.88F, 0.04F), X, UP, 0.08F, argb(k, 0xFFFFFF));
				}
				torii(o, -1.45F, k, true);
				// its light falling on the caster
				o.glowLine(mirror, new Vector3f(0, 0.2F, 0), 0.9F, argb(0.25F * k, SUN), argb(0.15F * k, SUN));
			}
			for (int i = 0; i < 8; i++) {
				float a = time * 0.06F + Mth.TWO_PI * i / 8;
				Vector3f at = polar(a, orbit, 1.0F + 0.2F * Mth.sin(time * 0.1F + i));
				ofuda(o, at, new Vector3f(-Mth.sin(a), 0, Mth.cos(a)), UP, 0.22F, 0.55F, s.seed + i * 3, k, true, VERMILION);
				o.sprite(at, camRight, camUp, 0.3F, argb(0.25F * k, SUN));
			}
			Vector3f ground = new Vector3f(0, 0.14F, 0);
			o.disc(ground, 1.6F * open, argb(0.5F * k, SUN_CORE), argb(0.0F, SUN));
			float wash = ease(p, 0.05F, 0.6F);
			o.ring(ground, Math.max(0.0F, s.size * wash - 0.6F), s.size * wash, argb(0.7F * fade * (1.0F - 0.5F * wash), SUN));
		});
		ps.popPose();
	}

	/** A point of the mirror's eight-lobed rim at angle {@code t}, in the plane facing +z. */
	private static Vector3f lobe(final Vector3f c, final float t, final float r) {
		float rr = r * (0.92F + 0.08F * Math.abs(Mth.cos(t * 4.0F)));
		return new Vector3f(c).add(Mth.cos(t) * rr, Mth.sin(t) * rr, 0);
	}

	/** A torii standing at {@code z} in the body's frame, facing ahead: pillars, the curved top beam and the tie beam under it. */
	private static void torii(final VfxOut o, final float z, final float alpha, final boolean lit) {
		int body = lit ? argb(0.9F * alpha, VERMILION) : argb(0.8F * alpha, 0xB0281A);
		float w = lit ? 0.05F : 0.09F;
		for (int side = -1; side <= 1; side += 2) {
			o.bar(new Vector3f(side * 0.95F, 0.0F, z), new Vector3f(side * 0.9F, 2.65F, z), w, w * 0.9F, body, body);
		}
		Vector3f prev = null;
		for (int i = 0; i <= 8; i++) {
			float x = -1.45F + 2.9F * i / 8;
			Vector3f q = new Vector3f(x, 2.7F + 0.12F * (x / 1.45F) * (x / 1.45F), z);
			if (prev != null) {
				o.bar(prev, q, w * 1.3F, w * 1.3F, lit ? body : argb(0.85F * alpha, 0x2A1A14), lit ? body : argb(0.85F * alpha, 0x2A1A14));
			}
			prev = q;
		}
		o.bar(new Vector3f(-1.25F, 2.5F, z), new Vector3f(1.25F, 2.5F, z), w, w, body, body);
		o.bar(new Vector3f(-1.15F, 2.05F, z), new Vector3f(1.15F, 2.05F, z), w * 0.9F, w * 0.9F, body, body);
		o.bar(new Vector3f(0, 2.05F, z), new Vector3f(0, 2.5F, z), w * 0.8F, w * 0.8F, body, body);
	}

	/** Where a strike of {@code Actions.strike} lands: the first on the aimed spot, the rest scattered a third of the radius round it. */
	private static Vector3f strikeSpot(final State s, final int i) {
		if (i == 0) {
			return new Vector3f();
		}
		float scatter = s.size * 2.0F / 3.0F;
		return new Vector3f((hash(s.seed * 7 + i) - 0.5F) * scatter, 0, (hash(s.seed * 11 + i) - 0.5F) * scatter);
	}

	/**
	 * Enten: three fire talismans drop out of the sky one after another, each wrapped in flame, and burst where they land into a pillar of
	 * fire with a ring of flame tongues; a ring of fire marks the ground.
	 */
	private static void enten(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float open = ease(age, 0.0F, 5.0F);
		float fade = 1.0F - ease(s.progress, 0.8F, 1.0F);
		float time = s.ageInTicks;
		shade(ps, c, o -> {
			for (int i = 0; i < 3; i++) {
				float land = 8 + i * 6;
				float fall = Mth.clamp((age - land + 8.0F) / 8.0F, 0.0F, 1.0F);
				Vector3f spot = strikeSpot(s, i);
				if (fall > 0.0F && fall < 1.0F) {
					Vector3f at = new Vector3f(spot).add(0, 0.6F + 8.0F * (1.0F - fall * fall), 0);
					float spin = time * 0.4F + i;
					ofuda(o, at, new Vector3f(Mth.cos(spin), 0, Mth.sin(spin)), UP, 0.3F, 0.75F, s.seed + i, fade, false, FIRE);
				}
				float b = Mth.clamp((age - land) / 14.0F, 0.0F, 1.0F);
				if (b > 0.0F) {
					o.disc(new Vector3f(spot).add(0, 0.13F, 0), 1.2F + s.size * 0.4F, argb(0.3F * fade, 0x1A0A05), argb(0.0F, 0x1A0A05));
				}
			}
		});
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			o.ring(ground, s.size * open - 0.15F, s.size * open + 0.15F, argb(0.8F * fade, FIRE));
			for (int i = 0; i < 12; i++) {
				float a = time * 0.02F + Mth.TWO_PI * i / 12;
				Vector3f at = polar(a, s.size * open, 0.15F);
				o.flame(at, UP, 0.5F + 0.3F * hash(i + s.seed), 0.3F, time * 0.7F + i, argb(0.7F * fade, FIRE_CORE), argb(0.0F, FIRE));
			}
			for (int i = 0; i < 3; i++) {
				float land = 8 + i * 6;
				float fall = Mth.clamp((age - land + 8.0F) / 8.0F, 0.0F, 1.0F);
				Vector3f spot = strikeSpot(s, i);
				if (fall > 0.0F && fall < 1.0F) {
					Vector3f at = new Vector3f(spot).add(0, 0.6F + 8.0F * (1.0F - fall * fall), 0);
					float spin = time * 0.4F + i;
					ofuda(o, at, new Vector3f(Mth.cos(spin), 0, Mth.sin(spin)), UP, 0.3F, 0.75F, s.seed + i, fade, true, FIRE_CORE);
					o.flame(new Vector3f(at).add(0, -0.4F, 0), UP, 1.3F, 0.6F, time + i, argb(0.8F * fade, FIRE_CORE), argb(0.0F, FIRE));
					o.sprite(at, s.camRight, s.camUp, 0.7F, argb(0.5F * fade, FIRE));
				}
				float b = Mth.clamp((age - land) / 14.0F, 0.0F, 1.0F);
				if (b <= 0.0F || b >= 1.0F) {
					continue;
				}
				float kb = 1.0F - b;
				o.sprite(new Vector3f(spot).add(0, 0.8F, 0), s.camRight, s.camUp, 0.6F + 2.2F * kb, argb(0.8F * kb, FIRE_CORE));
				o.flame(new Vector3f(spot).add(0, 0.1F, 0), UP, 5.5F * kb + 0.8F, 2.2F, time * 0.8F, argb(0.95F * kb, 0xFFF4D0), argb(0.0F, FIRE));
				o.flame(new Vector3f(spot).add(0, 0.1F, 0), UP, 4.0F * kb + 0.6F, 1.4F, time * 0.6F + 2.0F, argb(0.9F * kb, FIRE_CORE), argb(0.0F, VERMILION));
				o.glowLine(new Vector3f(spot).add(0, 0.2F, 0), new Vector3f(spot).add(0, 5.0F * kb + 0.5F, 0), 1.0F, argb(0.5F * kb, FIRE), argb(0.0F, FIRE));
				for (int j = 0; j < 10; j++) {
					float a = Mth.TWO_PI * j / 10 + i;
					Vector3f foot = polar(a, 0.4F + s.size * 0.55F * b, 0.12F).add(spot);
					o.flame(foot, new Vector3f(Mth.cos(a) * 0.4F, 1, Mth.sin(a) * 0.4F), 2.6F * kb + 0.4F, 0.85F, time * 0.9F + j, argb(0.9F * kb, FIRE_CORE),
						argb(0.0F, FIRE));
				}
				o.ring(new Vector3f(spot).add(0, 0.16F, 0), s.size * b, s.size * b + 0.5F, argb(0.8F * kb, FIRE));
			}
		});
	}

	/**
	 * Hyoten: a ring of ice talismans hangs in the sky over the aimed spot while frost spreads over the ground below in a great
	 * snowflake, ice jutting up through it and a cold mist lying on it.
	 */
	private static void hyoten(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float age = s.age();
		float open = ease(p, 0.0F, 0.15F);
		float grow = ease(p, 0.05F, 0.5F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float time = s.ageInTicks;
		float r = s.size;
		Vector3f sky = new Vector3f(0.6F, 13.2F, 0.6F);
		int spikes = 7;
		Vector3f[] spike = new Vector3f[spikes];
		Vector3f[] lean = new Vector3f[spikes];
		float[] rise = new float[spikes];
		for (int i = 0; i < spikes; i++) {
			float a = Mth.TWO_PI * hash(s.seed + i * 5);
			float d = r * (0.25F + 0.65F * hash(s.seed * 3 + i));
			spike[i] = polar(a, d, 0.0F);
			lean[i] = polar(a, 0.35F, 1.0F).normalize();
			rise[i] = ease(age - 6.0F - i * 2.0F, 0.0F, 4.0F);
		}
		shade(ps, c, o -> {
			o.disc(new Vector3f(0, 0.13F, 0), r * grow, argb(0.3F * fade, 0xE0F6FF), argb(0.05F * fade, 0xE0F6FF));
			for (int i = 0; i < 8; i++) {
				float a = time * 0.03F + Mth.TWO_PI * i / 8;
				Vector3f at = polar(a, r * 1.1F, 0.0F).add(sky);
				ofuda(o, at, new Vector3f(-Mth.sin(a), 0, Mth.cos(a)), UP, 0.5F, 1.3F, s.seed + i, open * fade, false, ICE);
			}
			for (int i = 0; i < spikes; i++) {
				if (rise[i] > 0.0F) {
					o.crystal(spike[i], lean[i], 2.2F * rise[i] * (0.7F + 0.5F * hash(i)), 0.3F, 6, argb(0.55F * fade, 0xBFEAFF), argb(0.55F * fade, 0xFFFFFF));
				}
			}
		});
		glow(ps, c, o -> {
			for (int i = 0; i < 8; i++) {
				float a = time * 0.03F + Mth.TWO_PI * i / 8;
				Vector3f at = polar(a, r * 1.1F, 0.0F).add(sky);
				ofuda(o, at, new Vector3f(-Mth.sin(a), 0, Mth.cos(a)), UP, 0.5F, 1.3F, s.seed + i, open * fade, true, ICE_DEEP);
				o.sprite(at, s.camRight, s.camUp, 0.6F, argb(0.35F * open * fade, ICE));
			}
			o.circle(sky, X, Z, r * 1.1F * open, 0.06F, 32, argb(0.7F * fade, ICE));
			// the frost: a six-armed snowflake spreading over the ground
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			int col = argb(0.85F * fade, ICE);
			for (int arm = 0; arm < 6; arm++) {
				float a = Mth.TWO_PI * arm / 6 + 0.2F;
				Vector3f end = polar(a, r * grow, 0.15F);
				o.bar(ground, end, 0.09F, 0.04F, col, col);
				for (int j = 1; j <= 3; j++) {
					float t = 0.25F * j;
					Vector3f root = polar(a, r * grow * t, 0.15F);
					float len = r * grow * 0.3F * (1.0F - t * 0.6F);
					o.bar(root, new Vector3f(root).add(polar(a + 0.7F, len, 0)), 0.06F, 0.02F, col, col);
					o.bar(root, new Vector3f(root).add(polar(a - 0.7F, len, 0)), 0.06F, 0.02F, col, col);
				}
			}
			o.ring(ground, r * grow - 0.12F, r * grow + 0.12F, argb(0.7F * fade, ICE_DEEP));
			o.polygram(ground, X, Z, r * grow * 0.5F, 6, 1, 0.2F, 0.03F, argb(0.7F * fade, ICE));
			for (int i = 0; i < spikes; i++) {
				if (rise[i] > 0.0F) {
					o.crystal(spike[i], lean[i], 2.2F * rise[i] * (0.7F + 0.5F * hash(i)), 0.3F, 6, argb(0.8F * fade, ICE), argb(0.9F * fade, 0xFFFFFF));
				}
			}
			for (int i = 0; i < 14; i++) {
				float drift = (time * 0.02F + hash(i * 3 + s.seed)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 9), r * hash(i * 5 + 1), 0.3F + drift * 1.5F);
				o.glint(at, s.camRight, s.camUp, 0.16F, i, argb(0.8F * fade * Mth.sin(Mth.PI * drift), 0xFFFFFF));
			}
		});
	}

	/** One ice talisman an icicle drops out of: it flares with frost, a snow star bursts under it and it tears in two. */
	private static void iceOfuda(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.2F);
		float tear = ease(p, 0.35F, 1.0F);
		float fade = 1.0F - ease(p, 0.55F, 1.0F);
		Vector3f a = new Vector3f(s.camRight.x(), 0, s.camRight.z());
		if (a.lengthSquared() < 1.0E-4F) {
			a.set(1, 0, 0);
		}
		a.normalize();
		float w = 0.35F * open;
		Vector3f left = new Vector3f(a).mul(-w * 0.25F - tear * 0.4F).add(0, tear * 0.2F, 0);
		Vector3f right = new Vector3f(a).mul(w * 0.25F + tear * 0.4F).add(0, -tear * 0.3F, 0);
		shade(ps, c, o -> {
			ofuda(o, left, a, UP, w * 0.5F, 0.9F, s.seed, fade, false, ICE);
			ofuda(o, right, a, UP, w * 0.5F, 0.9F, s.seed + 1, fade, false, ICE);
		});
		glow(ps, c, o -> {
			ofuda(o, left, a, UP, w * 0.5F, 0.9F, s.seed, fade, true, ICE_DEEP);
			ofuda(o, right, a, UP, w * 0.5F, 0.9F, s.seed + 1, fade, true, ICE_DEEP);
			Vector3f below = new Vector3f(0, -0.6F, 0);
			o.star(below, s.camRight, s.camUp, 0.3F + 0.8F * ease(p, 0.1F, 0.6F), 6, 0.3F, s.seed, argb(0.9F * fade, 0xFFFFFF), argb(0.0F, ICE));
			o.sprite(below, s.camRight, s.camUp, 0.7F, argb(0.5F * fade, ICE_DEEP));
		});
	}

	/**
	 * Shapeshift: kitsunebi, pale blue fox fires, circle the caster bobbing, and nine golden fox tails of light fan out behind them,
	 * swaying.
	 */
	private static void foxFire(final State s, final PoseStack ps, final SubmitNodeCollector c) {
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
		int tails = 9;
		int steps = 9;
		Vector3f[][] tail = new Vector3f[tails][steps + 1];
		for (int j = 0; j < tails; j++) {
			float phi = -1.15F + 2.3F * j / (tails - 1);
			float sway = 0.18F * Mth.sin(time * 0.12F + j * 0.8F);
			for (int i = 0; i <= steps; i++) {
				float t = (float)i / steps;
				float len = 1.7F * open;
				tail[j][i] = new Vector3f(Mth.sin(phi + sway * t) * len * 0.75F * t, 0.85F + len * (0.25F * t + 0.55F * t * t), -0.25F - len * (0.45F * t - 0.15F * t * t));
			}
		}
		if (!s.selfView) {
			shade(ps, c, o -> {
				for (int j = 0; j < tails; j++) {
					for (int i = 0; i < steps; i++) {
						float t0 = (float)i / steps;
						float t1 = (float)(i + 1) / steps;
						o.bar(tail[j][i], tail[j][i + 1], tailWidth(t0), tailWidth(t1), argb(0.55F * k, 0xE08A30), argb(0.55F * k, t1 > 0.8F ? 0xFFF0E0 : 0xE89A40));
					}
				}
			});
		}
		glow(ps, c, o -> {
			if (!s.selfView) {
				for (int j = 0; j < tails; j++) {
					for (int i = 0; i < steps; i++) {
						float t0 = (float)i / steps;
						float t1 = (float)(i + 1) / steps;
						o.glowLine(tail[j][i], tail[j][i + 1], tailWidth(t0) * 1.6F, argb(0.35F * k, TAIL), argb(0.35F * k, t1 > 0.8F ? 0xFFFFFF : TAIL));
					}
					o.sprite(tail[j][steps], camRight, camUp, 0.2F, argb(0.8F * k, 0xFFFFFF));
				}
			}
			float orbit = s.selfView ? 1.5F : 1.0F;
			for (int i = 0; i < 6; i++) {
				float a = time * 0.07F + Mth.TWO_PI * i / 6;
				Vector3f at = polar(a, orbit, 0.9F + 0.3F * Mth.sin(time * 0.15F + i * 1.7F));
				o.flame(new Vector3f(at).add(0, -0.15F, 0), UP, 0.55F, 0.32F, time * 0.6F + i * 2.0F, argb(0.9F * k, 0xE0F6FF), argb(0.0F, FOX_FIRE));
				o.sprite(at, camRight, camUp, 0.32F, argb(0.6F * k, FOX_FIRE));
				o.sprite(at, camRight, camUp, 0.1F, argb(k, 0xFFFFFF));
				// a wisp of its trail
				for (int q = 1; q <= 3; q++) {
					Vector3f back = polar(a - q * 0.18F, orbit, at.y - q * 0.05F);
					o.sprite(back, camRight, camUp, 0.16F - q * 0.03F, argb(0.4F * k / q, FOX_FIRE));
				}
			}
			o.ring(new Vector3f(0, 0.14F, 0), 0.8F, 1.15F, argb(0.5F * k, FOX_FIRE));
		});
		ps.popPose();
	}

	/** A fox tail is fattest a little past its middle and comes to a point. */
	private static float tailWidth(final float t) {
		return 0.05F + 0.17F * Mth.sin(Mth.PI * (0.15F + 0.85F * t));
	}

	// ------------------------------------------------------------ Zhuge Liang

	/** The trigram of {@code index} (0..7): bit 0 the bottom line, a set bit a whole (yang) line, else a broken (yin) one. */
	private static void trigram(final VfxOut o, final Vector3f c, final Vector3f across, final Vector3f up, final float w, final int index, final float width,
		final int color) {
		Vector3f a = new Vector3f(across).normalize();
		Vector3f u = new Vector3f(up).normalize();
		for (int line = 0; line < 3; line++) {
			Vector3f mid = new Vector3f(u).mul((line - 1) * w * 0.42F).add(c);
			boolean yang = (index >> line & 1) == 1;
			if (yang) {
				o.bar(new Vector3f(a).mul(-w / 2).add(mid), new Vector3f(a).mul(w / 2).add(mid), width, width, color, color);
			} else {
				o.bar(new Vector3f(a).mul(-w / 2).add(mid), new Vector3f(a).mul(-w * 0.1F).add(mid), width, width, color, color);
				o.bar(new Vector3f(a).mul(w * 0.1F).add(mid), new Vector3f(a).mul(w / 2).add(mid), width, width, color, color);
			}
		}
	}

	/**
	 * Stone Sentinel Maze: eight stone pillars rise in an octagon round the aimed spot, each with its trigram glowing on the inner face;
	 * the bagua is drawn on the ground (two octagons, the trigrams between them) round a turning taijitu, mist lying low.
	 */
	private static void stoneMaze(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float rise = ease(age, 0.0F, 10.0F);
		float fade = 1.0F - ease(s.progress, 0.88F, 1.0F);
		float time = s.ageInTicks;
		float r = s.size;
		float height = 4.6F * rise;
		shade(ps, c, o -> {
			o.disc(new Vector3f(0, 0.13F, 0), r * 1.1F, argb(0.25F * fade, 0xDDE6EE), argb(0.05F * fade, 0xDDE6EE));
			for (int i = 0; i < 8; i++) {
				float a = Mth.PI / 8 + Mth.TWO_PI * i / 8;
				pillar(o, polar(a, r, 0.0F), polar(a, 1.0F, 0.0F), 0.95F, height, fade);
			}
			// the taijitu's dark tadpole: between the outer half circle and the S
			float spin = time * 0.02F;
			float tr = r * 0.28F;
			int n = 16;
			int dark = argb(0.6F * fade, 0x10202A);
			for (int i = 0; i < n; i++) {
				float t0 = (float)i / n;
				float t1 = (float)(i + 1) / n;
				o.quad(polar(spin - Mth.PI * t0, tr, 0.14F), polar(spin - Mth.PI * t1, tr, 0.14F), taijitu(spin, tr, t1, 0.14F), taijitu(spin, tr, t0, 0.14F), dark);
			}
		});
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			int line = argb(0.85F * fade, SAGE);
			o.polygram(ground, X, Z, r * 1.08F, 8, 1, Mth.PI / 8, 0.05F, line);
			o.polygram(ground, X, Z, r * 0.6F, 8, 1, Mth.PI / 8, 0.04F, line);
			o.circle(ground, X, Z, r * 0.3F, 0.04F, 32, line);
			for (int i = 0; i < 8; i++) {
				float a = Mth.TWO_PI * i / 8;
				Vector3f out = polar(a, 1.0F, 0.0F);
				Vector3f along = polar(a + Mth.HALF_PI, 1.0F, 0.0F);
				trigram(o, polar(a, r * 0.84F, 0.15F), along, out, r * 0.22F, i, 0.05F, argb(0.9F * fade, SAGE));
				// the trigram on the pillar's inner face
				float pa = Mth.PI / 8 + Mth.TWO_PI * i / 8;
				Vector3f face = polar(pa, r - 0.5F, 0.0F).add(0, height * 0.62F, 0);
				trigram(o, face, polar(pa + Mth.HALF_PI, 1.0F, 0.0F), UP, 0.6F, i, 0.05F, argb(0.95F * fade * rise, 0xBFE8FF));
				o.sprite(face, s.camRight, s.camUp, 0.7F, argb(0.3F * fade * rise, SAGE));
			}
			// the taijitu: its S and its two eyes
			float spin = time * 0.02F;
			float tr = r * 0.28F;
			Vector3f prev = null;
			for (int i = 0; i <= 16; i++) {
				Vector3f q = taijitu(spin, tr, i / 16.0F, 0.15F);
				if (prev != null) {
					o.bar(prev, q, 0.03F, 0.03F, line, line);
				}
				prev = q;
			}
			o.sprite(polar(spin, tr * 0.5F, 0.16F), X, Z, tr * 0.12F, argb(0.9F * fade, 0xFFFFFF));
			o.sprite(polar(spin + Mth.PI, tr * 0.5F, 0.16F), X, Z, tr * 0.12F, argb(0.9F * fade, SAGE));
			// mist lines drifting between the stones
			for (int i = 0; i < 10; i++) {
				float a = time * 0.01F + Mth.TWO_PI * hash(s.seed + i);
				float y = 0.3F + 0.6F * hash(i * 3);
				o.glowLine(polar(a, r * 0.7F, y), polar(a + 0.6F, r * 0.75F, y), 0.25F, argb(0.15F * fade, 0xE0F0FF), argb(0.0F, 0xE0F0FF));
			}
		});
	}

	/**
	 * The taijitu's S, {@code t} 0..1 of the way from its rim at {@code spin} through the middle to the rim opposite: two half circles of
	 * half the radius bulging to either side.
	 */
	private static Vector3f taijitu(final float spin, final float r, final float t, final float y) {
		return t <= 0.5F ? polar(spin, r * 0.5F, y).add(polar(spin + Mth.TWO_PI * t, r * 0.5F, 0.0F))
			: polar(spin + Mth.PI, r * 0.5F, y).add(polar(spin - Mth.TWO_PI * (t - 0.5F), r * 0.5F, 0.0F));
	}

	/** A standing stone {@code w} wide and {@code h} high at {@code base}, one face turned to {@code out}: four grey sides and a darker top. */
	private static void pillar(final VfxOut o, final Vector3f base, final Vector3f out, final float w, final float h, final float alpha) {
		if (h <= 0.01F) {
			return;
		}
		Vector3f n = new Vector3f(out).normalize(w / 2);
		Vector3f t = new Vector3f(-n.z, 0, n.x);
		Vector3f[] corner = {new Vector3f(base).add(n).add(t), new Vector3f(base).add(n).sub(t), new Vector3f(base).sub(n).sub(t), new Vector3f(base).sub(n).add(t)};
		int[] shade = {0x7A766E, 0x67635C, 0x5A5650, 0x6E6A62};
		for (int i = 0; i < 4; i++) {
			Vector3f a = corner[i];
			Vector3f b = corner[(i + 1) % 4];
			int col = argb(0.95F * alpha, shade[i]);
			int top = argb(0.95F * alpha, VfxDraw.whiten(shade[i], 0.15F));
			o.quad(a, b, new Vector3f(b).add(0, h, 0), new Vector3f(a).add(0, h, 0), col, col, top, top);
		}
		int cap = argb(0.95F * alpha, 0x4A4640);
		o.quad(new Vector3f(corner[0]).add(0, h, 0), new Vector3f(corner[1]).add(0, h, 0), new Vector3f(corner[2]).add(0, h, 0), new Vector3f(corner[3]).add(0, h, 0),
			cap);
	}

	/**
	 * East Wind of Red Cliffs: streams of wind sweep forward from behind the caster and fan out over the cone, gusts racing along them and
	 * embers carried on them.
	 */
	private static void eastWind(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.15F);
		float fade = 1.0F - ease(p, 0.55F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		Vector3f f = new Vector3f(s.reach.x(), 0, s.reach.z());
		if (f.lengthSquared() < 1.0E-6F) {
			f.set(s.forward);
		}
		f.normalize();
		Vector3f right = VfxOut.across(f);
		float r = Math.max(2.0F, s.size);
		float half = Math.max(20.0F, s.arg) * 0.5F * Mth.DEG_TO_RAD;
		glow(ps, c, o -> {
			int streams = 9;
			int steps = 16;
			for (int i = 0; i < streams; i++) {
				float theta = -half + 2.0F * half * i / (streams - 1);
				float y = -0.7F + 1.8F * hash(s.seed + i * 3);
				float from = -1.5F;
				float to = r * 1.1F;
				float head = from + (time * 0.9F + hash(i * 7) * r) % (to - from + 3.0F);
				Vector3f prev = null;
				for (int j = 0; j <= steps; j++) {
					float d = from + (to - from) * j / steps;
					Vector3f q = windPoint(f, right, theta, d, r, y, time, i);
					if (prev != null) {
						float gust = Mth.clamp(1.0F - Math.abs(d - head) / 3.0F, 0.0F, 1.0F);
						// soft at both ends of the stream
						float ends = Mth.sin(Mth.PI * j / steps);
						float al = (0.12F + 0.6F * gust * gust) * k * ends;
						o.glowLine(prev, q, 0.1F + 0.14F * gust, argb(al, 0xDFF6FF), argb(al, 0xDFF6FF));
					}
					prev = q;
				}
			}
			for (int i = 0; i < 16; i++) {
				int stream = i % 9;
				float theta = -half + 2.0F * half * stream / 8;
				float d = -1.0F + (time * 0.7F + hash(i * 13 + s.seed) * r) % (r + 1.5F);
				float y = -0.7F + 1.8F * hash(s.seed + stream * 3) + 0.2F * Mth.sin(time * 0.3F + i);
				Vector3f at = windPoint(f, right, theta, d, r, y, time, stream);
				o.glint(at, s.camRight, s.camUp, 0.16F, time * 0.2F + i, argb(0.95F * k, i % 3 == 0 ? FIRE_CORE : FIRE));
			}
		});
	}

	/** A point {@code d} along wind stream {@code i}: fanning out to {@code theta} as it goes, waving up and down. */
	private static Vector3f windPoint(final Vector3f f, final Vector3f right, final float theta, final float d, final float r, final float y, final float time,
		final int i) {
		float t = Mth.clamp(d / r, 0.0F, 1.2F);
		float a = theta * t;
		return new Vector3f(f).mul(Mth.cos(a) * d).add(new Vector3f(right).mul(Mth.sin(a) * d)).add(0, y + 0.25F * Mth.sin(d * 0.9F - time * 0.5F + i), 0);
	}

	/**
	 * Discerning Eye: a reticle closes on the target and locks, rings of measure turning round it, an eye opening over its head and lines
	 * of what it has read appearing beside it.
	 */
	private static void discerningEye(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float h = Math.max(0.6F, s.size);
		float w = Math.max(0.4F, s.arg);
		float p = s.progress;
		float lock = ease(p, 0.0F, 0.25F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float time = s.ageInTicks;
		Vector3f mid = new Vector3f(0, h * 0.55F, 0);
		Vector3f right = s.camRight;
		Vector3f up = s.camUp;
		glow(ps, c, o -> {
			int col = argb(0.95F * fade, SAGE);
			// the reticle's four corners closing in
			float d = (w * 0.5F + h * 0.35F) + 1.8F * (1.0F - lock);
			float spin = (1.0F - lock) * 1.6F;
			for (int i = 0; i < 4; i++) {
				float a = spin + Mth.HALF_PI * i + Mth.PI / 4;
				Vector3f dir = new Vector3f(right).mul(Mth.cos(a)).add(new Vector3f(up).mul(Mth.sin(a)));
				Vector3f corner = new Vector3f(dir).mul(d).add(mid);
				Vector3f e1 = new Vector3f(right).mul(Mth.cos(a + Mth.PI * 0.75F)).add(new Vector3f(up).mul(Mth.sin(a + Mth.PI * 0.75F)));
				Vector3f e2 = new Vector3f(right).mul(Mth.cos(a - Mth.PI * 0.75F)).add(new Vector3f(up).mul(Mth.sin(a - Mth.PI * 0.75F)));
				o.bar(corner, new Vector3f(e1).mul(0.35F).add(corner), 0.03F, 0.03F, col, col);
				o.bar(corner, new Vector3f(e2).mul(0.35F).add(corner), 0.03F, 0.03F, col, col);
			}
			// rings of measure
			float rr = w * 0.5F + h * 0.25F;
			o.circle(mid, right, up, rr, 0.02F, 32, argb(0.7F * fade * lock, SAGE));
			for (int i = 0; i < 24; i++) {
				float a = time * 0.03F + Mth.TWO_PI * i / 24;
				Vector3f dir = new Vector3f(right).mul(Mth.cos(a)).add(new Vector3f(up).mul(Mth.sin(a)));
				o.bar(new Vector3f(dir).mul(rr).add(mid), new Vector3f(dir).mul(rr + (i % 3 == 0 ? 0.18F : 0.09F)).add(mid), 0.015F, 0.015F, argb(0.8F * fade * lock,
					SAGE), argb(0.8F * fade * lock, SAGE));
			}
			for (int i = 0; i < 12; i++) {
				float a0 = -time * 0.05F + Mth.TWO_PI * i / 12;
				Vector3f prev = null;
				for (int j = 0; j <= 3; j++) {
					float a = a0 + 0.3F * j / 3;
					Vector3f q = new Vector3f(right).mul(Mth.cos(a) * rr * 1.35F).add(new Vector3f(up).mul(Mth.sin(a) * rr * 1.35F)).add(mid);
					if (prev != null) {
						o.bar(prev, q, 0.018F, 0.018F, argb(0.6F * fade * lock, SAGE), argb(0.6F * fade * lock, SAGE));
					}
					prev = q;
				}
			}
			// the eye over its head
			Vector3f eye = new Vector3f(0, h + 0.6F, 0);
			float open = ease(p, 0.15F, 0.35F);
			Vector3f prevT = null;
			Vector3f prevB = null;
			for (int i = 0; i <= 12; i++) {
				float t = (float)i / 12;
				float x = (t * 2 - 1) * 0.5F;
				float y = Mth.sin(t * Mth.PI) * 0.2F * open;
				Vector3f top = new Vector3f(right).mul(x).add(new Vector3f(up).mul(y)).add(eye);
				Vector3f bottom = new Vector3f(right).mul(x).add(new Vector3f(up).mul(-y)).add(eye);
				if (prevT != null) {
					o.bar(prevT, top, 0.025F, 0.025F, col, col);
					o.bar(prevB, bottom, 0.025F, 0.025F, col, col);
				}
				prevT = top;
				prevB = bottom;
			}
			o.circle(eye, right, up, 0.13F * open, 0.02F, 14, col);
			o.sprite(eye, right, up, 0.07F * open, argb(fade, 0xFFFFFF));
			// what it read: lines of text appearing one by one beside the target
			Vector3f text = new Vector3f(right).mul(w * 0.5F + h * 0.45F + 0.5F).add(mid).add(new Vector3f(up).mul(0.35F));
			o.bar(new Vector3f(right).mul(d * 0.7F).add(mid), new Vector3f(text).sub(new Vector3f(right).mul(0.1F)), 0.012F, 0.012F, argb(0.6F * fade * lock, SAGE),
				argb(0.6F * fade * lock, SAGE));
			for (int i = 0; i < 5; i++) {
				float shown = ease(p, 0.25F + i * 0.06F, 0.3F + i * 0.06F);
				if (shown <= 0.0F) {
					continue;
				}
				Vector3f start = new Vector3f(up).mul(-i * 0.16F).add(text);
				float len = (0.4F + 0.5F * hash(s.seed + i)) * shown;
				o.bar(start, new Vector3f(right).mul(len).add(start), 0.025F, 0.025F, argb(0.85F * fade, i == 0 ? 0xFFFFFF : SAGE), argb(0.85F * fade, SAGE));
			}
			o.ring(new Vector3f(0, 0.14F, 0), w * 0.5F + 0.3F, w * 0.5F + 0.5F, argb(0.6F * fade * lock, SAGE));
		});
	}

	/**
	 * Tactician's Advice: a tall war banner of deep blue and gold planted behind the caster, the trigram of fire on it, the white feather
	 * fan in their hand, and rings of command washing out to every ally in reach, chevrons on them pointing the way.
	 */
	private static void warBanner(final State s, final PoseStack ps, final SubmitNodeCollector c) {
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
		// the banner: the pole behind the left shoulder (+x in the body's frame), the cloth flying out from it
		Vector3f pole = new Vector3f(0.55F, 0.0F, -0.5F);
		float top = 4.4F * open;
		int cols = 6;
		int rows = 4;
		Vector3f[][] cloth = new Vector3f[cols + 1][rows + 1];
		for (int i = 0; i <= cols; i++) {
			float t = (float)i / cols;
			for (int j = 0; j <= rows; j++) {
				float u = (float)j / rows;
				float x = pole.x + 1.5F * t;
				float wave = 0.14F * Mth.sin(t * 4.0F - time * 0.35F) * t;
				cloth[i][j] = new Vector3f(x, top - 0.15F - 2.2F * u * open, pole.z + wave);
			}
		}
		// the feather fan in the right hand (-x)
		Vector3f hand = new Vector3f(-0.42F, 1.15F, 0.3F);
		if (!s.selfView) {
			shade(ps, c, o -> {
				o.bar(pole, new Vector3f(pole).add(0, top, 0), 0.045F, 0.04F, argb(0.9F * k, 0x4A3220), argb(0.9F * k, 0x4A3220));
				for (int i = 0; i < cols; i++) {
					for (int j = 0; j < rows; j++) {
						o.quad(cloth[i][j], cloth[i + 1][j], cloth[i + 1][j + 1], cloth[i][j + 1], argb(0.85F * k, (i + j) % 2 == 0 ? 0x2E4A6E : 0x26405F));
					}
				}
				fan(o, hand, k, false, time);
			});
		}
		glow(ps, c, o -> {
			if (!s.selfView) {
				int trim = argb(0.9F * k, GOLD);
				for (int i = 0; i < cols; i++) {
					o.bar(cloth[i][0], cloth[i + 1][0], 0.03F, 0.03F, trim, trim);
					o.bar(cloth[i][rows], cloth[i + 1][rows], 0.03F, 0.03F, trim, trim);
				}
				for (int j = 0; j < rows; j++) {
					o.bar(cloth[cols][j], cloth[cols][j + 1], 0.03F, 0.03F, trim, trim);
				}
				o.sprite(new Vector3f(pole).add(0, top + 0.1F, 0), camRight, camUp, 0.2F, argb(k, GOLD));
				// the trigram of fire in a ring, on both faces
				Vector3f mid = new Vector3f(cloth[cols / 2][rows / 2]);
				for (int side = -1; side <= 1; side += 2) {
					Vector3f at = new Vector3f(mid).add(0, 0, side * 0.03F);
					o.circle(at, X, UP, 0.65F, 0.035F, 24, argb(0.9F * k, GOLD));
					trigram(o, at, X, UP, 0.62F, 5, 0.05F, argb(0.95F * k, 0xFFE8B0));
				}
				o.sprite(mid, camRight, camUp, 1.0F, argb(0.2F * k, SAGE));
				fan(o, hand, k, true, time);
			}
			// rings of command washing out, chevrons on them
			for (int w = 0; w < 3; w++) {
				float q = ease(p, 0.05F + 0.15F * w, 0.55F + 0.15F * w);
				if (q <= 0.0F || q >= 1.0F) {
					continue;
				}
				float r = s.size * q;
				float a = fade * (1.0F - q);
				o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, r - 0.35F), r, argb(0.8F * a, SAGE));
				for (int i = 0; i < 8; i++) {
					float t = Mth.TWO_PI * i / 8 + w * 0.4F;
					Vector3f at = polar(t, r + 0.3F, 0.16F);
					Vector3f out = polar(t, 0.3F, 0.0F);
					Vector3f side = polar(t + Mth.HALF_PI, 0.25F, 0.0F);
					o.bar(new Vector3f(at).sub(side), new Vector3f(at).add(out), 0.04F, 0.04F, argb(0.9F * a, GOLD), argb(0.9F * a, GOLD));
					o.bar(new Vector3f(at).add(side), new Vector3f(at).add(out), 0.04F, 0.04F, argb(0.9F * a, GOLD), argb(0.9F * a, GOLD));
				}
			}
			o.ring(new Vector3f(0, 0.14F, 0), 0.6F, 1.0F, argb(0.5F * k, SAGE));
		});
		ps.popPose();
	}

	/** Zhuge Liang's white feather fan, held up in the hand: nine feathers fanning from a gold grip. */
	private static void fan(final VfxOut o, final Vector3f hand, final float alpha, final boolean lit, final float time) {
		Vector3f grip = new Vector3f(hand).add(0, 0.2F, 0);
		float wave = 0.15F * Mth.sin(time * 0.2F);
		for (int i = 0; i < 9; i++) {
			float a = -0.9F + 1.8F * i / 8 + wave;
			Vector3f d = new Vector3f(Mth.sin(a) * 0.6F, Mth.cos(a), 0.2F).normalize();
			Vector3f tip = new Vector3f(d).mul(0.55F).add(grip);
			if (lit) {
				o.bar(grip, tip, 0.008F, 0.004F, argb(0.6F * alpha, 0xFFFFFF), argb(0.4F * alpha, 0xFFFFFF));
			} else {
				o.blade(grip, tip, new Vector3f(Mth.cos(a), -Mth.sin(a), 0), 0.08F, argb(0.9F * alpha, 0xF4F4F0), argb(0.9F * alpha, 0xE0E0DA));
			}
		}
		if (lit) {
			o.bar(hand, grip, 0.025F, 0.025F, argb(alpha, GOLD), argb(alpha, GOLD));
		}
	}

	private EastLooks() {
	}
}
