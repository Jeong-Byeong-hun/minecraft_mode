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
import org.joml.Vector4f;

/**
 * Looks of the kings of Uruk: Gilgamesh (the Gates of Babylon, Enkidu, the Treasury's shields, Ea's Enuma Elish, the all-seeing eye,
 * the golden rule) and Ishtar (Venus falling, her star, her wings).
 */
final class KingLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final int GOLD = 0xFFD24A;
	private static final int GOLD_PALE = 0xFFF0B0;
	private static final int TEAL = 0x40E0D0;
	private static final int RUPTURE = 0xFF1E1E;
	private static final int VENUS = 0xFF4A3A;
	private static final int ROSE = 0xFF8FB0;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case BABYLON_GATE -> babylonGate(s, ps, c);
			case BABYLON_SKY -> babylonSky(s, ps, c);
			case ENKIDU -> enkidu(s, ps, c);
			case TREASURY_SHIELDS -> treasuryShields(s, ps, c);
			case EA_CHARGE -> eaCharge(s, ps, c);
			case ENUMA_ELISH -> enumaElish(s, ps, c);
			case SHA_NAQBA -> shaNaqba(s, ps, c);
			case GOLDEN_RULE -> goldenRule(s, ps, c);
			case VENUS_STAR -> venusStar(s, ps, c);
			case BEAUTY -> beauty(s, ps, c);
			case VENUS_WINGS -> venusWings(s, ps, c);
			default -> {
			}
		}
	}

	// ------------------------------------------------------------ the Gate of Babylon

	/**
	 * One Gate of Babylon facing along {@code normal}: a disc of golden light rippling like water, rings wavering round it, a rim of
	 * glyph ticks and glints; {@code open} 0..1 grows it from nothing.
	 */
	static void gate(final VfxOut o, final Vector3f c, final Vector3f normal, final float radius, final float open, final float alpha, final float time,
		final int seed) {
		if (open <= 0.01F || alpha <= 0.01F) {
			return;
		}
		Vector3f n = new Vector3f(normal).normalize();
		Vector3f u = VfxOut.across(n);
		Vector3f v = new Vector3f(n).cross(u).normalize();
		float r = radius * open;
		o.sprite(c, u, v, r * 1.25F, argb(0.35F * alpha, GOLD));
		o.sprite(c, u, v, r * 0.55F, argb(0.8F * alpha, GOLD_PALE));
		// ripples: rings that waver round their circumference and run outward
		for (int k = 0; k < 3; k++) {
			float q = (time * 0.05F + k / 3.0F) % 1.0F;
			float rk = r * (0.25F + 0.8F * q);
			int col = argb(0.9F * alpha * (1.0F - q * 0.7F), k == 0 ? 0xFFFFFF : GOLD_PALE);
			Vector3f prev = null;
			int steps = 28;
			for (int i = 0; i <= steps; i++) {
				float t = Mth.TWO_PI * i / steps;
				float w = rk * (1.0F + 0.07F * Mth.sin(t * 6.0F + time * 0.4F + k + seed));
				Vector3f p = new Vector3f(u).mul(Mth.cos(t) * w).add(new Vector3f(v).mul(Mth.sin(t) * w)).add(c);
				if (prev != null) {
					o.bar(prev, p, 0.022F, 0.022F, col, col);
				}
				prev = p;
			}
		}
		// the rim with its glyphs
		o.circle(c, u, v, r * 1.05F, 0.03F, 32, argb(0.95F * alpha, GOLD));
		for (int i = 0; i < 12; i++) {
			float t = Mth.TWO_PI * i / 12 + time * 0.03F;
			Vector3f d = new Vector3f(u).mul(Mth.cos(t)).add(new Vector3f(v).mul(Mth.sin(t)));
			o.bar(new Vector3f(d).mul(r * 1.08F).add(c), new Vector3f(d).mul(r * (i % 2 == 0 ? 1.28F : 1.18F)).add(c), 0.025F, 0.025F, argb(0.9F * alpha, GOLD),
				argb(0.4F * alpha, GOLD));
		}
	}

	/** The amber tint under a gate (translucent): additive gold alone washes out to white against a day sky. */
	static void gateTint(final VfxOut o, final Vector3f c, final Vector3f normal, final float radius, final float open, final float alpha) {
		if (open <= 0.01F || alpha <= 0.01F) {
			return;
		}
		Vector3f n = new Vector3f(normal).normalize();
		Vector3f u = VfxOut.across(n);
		Vector3f v = new Vector3f(n).cross(u).normalize();
		o.sprite(c, u, v, radius * open * 1.15F, argb(0.6F * alpha, 0x9A6408));
	}

	/** A Gate the treasure flies out of: opens fast, ripples, closes. */
	private static void babylonGate(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.2F);
		float fade = 1.0F - ease(p, 0.6F, 1.0F);
		Vector3f n = new Vector3f(s.reach);
		if (n.lengthSquared() < 1.0E-6F) {
			n.set(s.forward);
		}
		shade(ps, c, o -> gateTint(o, new Vector3f(), n, Math.max(0.6F, s.size) * 0.9F, open, fade));
		glow(ps, c, o -> {
			gate(o, new Vector3f(), n, Math.max(0.6F, s.size) * 0.9F, open, fade, s.ageInTicks, s.seed);
			o.glint(new Vector3f(n).normalize(0.15F), s.camRight, s.camUp, 0.7F * (1 - ease(p, 0.0F, 0.5F)) + 0.1F, s.seed, argb(fade, 0xFFFFFF));
		});
	}

	/**
	 * Full Open: the sky over the aimed spot fills with gates one after another, all facing down, the points of their treasures showing;
	 * a golden ring marks the ground below.
	 */
	private static void babylonSky(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float fade = 1.0F - ease(s.progress, 0.8F, 1.0F);
		int gates = 18;
		// where the rain starts: 14 up and a little off (Actions.sky)
		Vector3f sky = new Vector3f(0.6F, 14.0F, 0.6F);
		Vector3f[] at = new Vector3f[gates];
		float[] open = new float[gates];
		for (int i = 0; i < gates; i++) {
			float a = Mth.TWO_PI * hash(s.seed + i * 7);
			float d = s.size * 1.3F * (float)Math.sqrt(hash(s.seed * 3 + i * 11));
			at[i] = polar(a, d, -1.5F + 3.0F * hash(i * 13)).add(sky);
			open[i] = ease(age - i * 0.8F, 0.0F, 4.0F);
		}
		Vector3f down = new Vector3f(0, -1, 0);
		shade(ps, c, o -> {
			for (int i = 0; i < gates; i++) {
				gateTint(o, at[i], down, 1.0F + 0.4F * hash(i * 17), open[i], fade);
				if (open[i] > 0.3F) {
					// the treasure's point showing under the gate
					Vector3f side = polar(hash(i * 5) * Mth.TWO_PI, 1.0F, 0.0F);
					o.blade(new Vector3f(at[i]).add(0, -0.1F, 0), new Vector3f(at[i]).add(0, -0.6F - 0.8F * open[i], 0), side, 0.12F, argb(0.9F * fade, 0xB8BCC8),
						argb(0.9F * fade, 0xE8ECF4));
				}
			}
		});
		glow(ps, c, o -> {
			for (int i = 0; i < gates; i++) {
				gate(o, at[i], down, 1.0F + 0.4F * hash(i * 17), open[i], fade, s.ageInTicks + i * 3, s.seed + i);
			}
			o.sprite(sky, s.camRight, s.camUp, s.size * 2.2F, argb(0.15F * fade, GOLD));
			Vector3f ground = new Vector3f(0, 0.14F, 0);
			o.ring(ground, s.size * 0.95F, s.size * 1.05F, argb(0.8F * fade, GOLD));
			o.disc(ground, s.size, argb(0.2F * fade, GOLD), argb(0.0F, GOLD));
		});
	}

	/**
	 * Enkidu: gates open round the caster and golden chains shoot out of them to every enemy near, then coil round each one; with
	 * nobody near they shoot out and hang in the air.
	 */
	private static void enkidu(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.12F);
		float shoot = ease(p, 0.05F, 0.3F);
		float coil = ease(p, 0.25F, 0.5F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float time = s.ageInTicks;
		int gates = 6;
		Vector3f[] gate = new Vector3f[gates];
		Vector3f[] normal = new Vector3f[gates];
		for (int i = 0; i < gates; i++) {
			float a = Mth.TWO_PI * i / gates + 0.3F;
			normal[i] = polar(a, 1.0F, 0.0F);
			gate[i] = polar(a, 1.3F, 0.9F + 1.1F * hash(i * 3 + 1));
		}
		int chainColor = argb(0.95F * fade, GOLD);
		shade(ps, c, o -> {
			for (int i = 0; i < gates; i++) {
				gateTint(o, gate[i], normal[i], 0.55F, open, fade);
			}
		});
		glow(ps, c, o -> {
			for (int i = 0; i < gates; i++) {
				gate(o, gate[i], normal[i], 0.55F, open, fade, time + i * 2, i);
			}
			if (s.targets.isEmpty()) {
				for (int i = 0; i < gates; i++) {
					Vector3f end = new Vector3f(normal[i]).mul(5.0F * shoot).add(gate[i]).add(0, -1.0F * coil, 0);
					o.chain(gate[i], end, 0.45F, time * 0.1F, chainColor);
					o.glowLine(gate[i], end, 0.12F, argb(0.4F * fade, GOLD), argb(0.2F * fade, GOLD));
				}
				return;
			}
			for (int j = 0; j < s.targets.size(); j++) {
				Vector4f t = s.targets.get(j);
				float width = s.targetWidths.get(j);
				Vector3f mid = new Vector3f(t.x, t.y + t.w * 0.55F, t.z);
				// the gate facing it most
				int best = 0;
				float bestDot = -2.0F;
				Vector3f to = new Vector3f(mid.x, 0, mid.z);
				if (to.lengthSquared() > 1.0E-4F) {
					to.normalize();
				}
				for (int i = 0; i < gates; i++) {
					float d = normal[i].dot(to);
					if (d > bestDot) {
						bestDot = d;
						best = i;
					}
				}
				Vector3f from = gate[(best + j / gates) % gates];
				Vector3f end = new Vector3f(from).lerp(mid, shoot);
				o.chain(from, end, 0.45F, time * 0.1F + j, chainColor);
				o.glowLine(from, end, 0.12F, argb(0.4F * fade, GOLD), argb(0.4F * fade, GOLD));
				if (coil > 0.0F) {
					// the coil round the body
					float r = width * 0.6F + 0.15F;
					int steps = 16;
					Vector3f prev = null;
					for (int i = 0; i <= (int)(steps * coil); i++) {
						float u = (float)i / steps;
						float a = u * Mth.TWO_PI * 2.5F + j;
						Vector3f q = new Vector3f(t.x + Mth.cos(a) * r, t.y + t.w * (0.85F - 0.65F * u), t.z + Mth.sin(a) * r);
						if (prev != null) {
							o.chain(prev, q, 0.4F, a, chainColor);
						}
						prev = q;
					}
				}
			}
		});
	}

	/** Shields of the Treasury: golden shields set with a turquoise stone come out of gates round the caster and circle them. */
	private static void treasuryShields(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.12F);
		float out = ease(p, 0.06F, 0.25F);
		float fade = 1.0F - ease(p, 0.85F, 1.0F);
		float time = s.ageInTicks;
		float r = s.size * (s.selfView ? 1.25F : 1.0F);
		float alpha = s.selfView ? 0.5F : 1.0F;
		int count = 5;
		Vector3f[] at = new Vector3f[count];
		Vector3f[] normal = new Vector3f[count];
		for (int i = 0; i < count; i++) {
			float a = Mth.TWO_PI * i / count + time * 0.025F;
			normal[i] = polar(a, 1.0F, 0.0F);
			at[i] = polar(a, r + 0.35F * out, 1.05F + 0.15F * Mth.sin(time * 0.1F + i));
		}
		shade(ps, c, o -> {
			for (int i = 0; i < count; i++) {
				shield(o, at[i], normal[i], 0.8F * out, argb(0.9F * fade * alpha, 0xC8961E), argb(0.9F * fade * alpha, 0x2AB0A0), false);
			}
		});
		glow(ps, c, o -> {
			for (int i = 0; i < count; i++) {
				Vector3f behind = polar(Mth.TWO_PI * i / count + time * 0.025F, r - 0.1F, at[i].y);
				gate(o, behind, normal[i], 0.75F, open, fade * alpha * (1.0F - 0.75F * out), time + i * 3, i);
				shield(o, at[i], normal[i], 0.8F * out, argb(0.9F * fade * alpha, GOLD), argb(0.9F * fade * alpha, TEAL), true);
			}
			o.ring(new Vector3f(0, 0.14F, 0), r - 0.15F, r + 0.15F, argb(0.5F * fade, GOLD));
		});
	}

	/** A heater shield facing {@code normal}: a flat top, sides curving to a point, a rim and a stone in the middle. */
	private static void shield(final VfxOut o, final Vector3f c, final Vector3f normal, final float r, final int body, final int stone, final boolean edges) {
		if (r <= 0.01F) {
			return;
		}
		Vector3f u = VfxOut.across(normal);
		Vector3f v = new Vector3f(UP);
		int n = 10;
		Vector3f[] rim = new Vector3f[n * 2 + 2];
		int k = 0;
		rim[k++] = new Vector3f(u).mul(-r).add(new Vector3f(v).mul(r * 0.9F)).add(c);
		for (int i = 0; i <= n; i++) {
			float t = (float)i / n;
			// straight sides that curve in to the point in the lower part
			float x = -r * (1.0F - (float)Math.pow(t, 2.5));
			float y = r * 0.9F - r * 2.1F * t;
			rim[k++] = new Vector3f(u).mul(x).add(new Vector3f(v).mul(y)).add(c);
		}
		for (int i = n - 1; i >= 0; i--) {
			Vector3f m = rim[1 + i];
			Vector3f d = new Vector3f(m).sub(c);
			float x = d.dot(u);
			float y = d.dot(v);
			rim[k++] = new Vector3f(u).mul(-x).add(new Vector3f(v).mul(y)).add(c);
		}
		int count = k;
		Vector3f center = new Vector3f(v).mul(-r * 0.1F).add(c);
		for (int i = 0; i < count; i++) {
			Vector3f a = rim[i];
			Vector3f b = rim[(i + 1) % count];
			if (edges) {
				o.bar(a, b, 0.035F, 0.035F, body, body);
			} else {
				o.tri(center, a, b, body, body, body);
			}
		}
		if (edges) {
			o.sprite(center, u, v, r * 0.3F, stone);
			o.circle(center, u, v, r * 0.22F, 0.025F, 12, body);
		} else {
			o.diamond(center, v, u, r * 0.5F, r * 0.35F, stone, stone);
		}
	}

	// ------------------------------------------------------------ Ea

	/**
	 * Ea, the sword of rupture: three cylinders on one axis from {@code base} along {@code axis}, each turning against the next, dark with
	 * red glyph lines.
	 */
	private static void ea(final State s, final PoseStack ps, final SubmitNodeCollector c, final Vector3f base, final Vector3f axis, final float scale,
		final float alpha) {
		Vector3f a = new Vector3f(axis).normalize();
		Vector3f u = VfxOut.across(a);
		Vector3f v = new Vector3f(a).cross(u).normalize();
		float time = s.ageInTicks;
		float[] radius = {0.17F, 0.14F, 0.11F};
		float segment = 0.42F * scale;
		shade(ps, c, o -> {
			for (int k = 0; k < 3; k++) {
				Vector3f from = new Vector3f(a).mul(segment * k).add(base);
				Vector3f to = new Vector3f(a).mul(segment * (k + 0.92F)).add(base);
				prism(o, from, to, u, v, radius[k] * scale, 8, argb(0.95F * alpha, 0x1A1410), argb(0.95F * alpha, 0x3A2A1A));
			}
			Vector3f tip = new Vector3f(a).mul(segment * 3.0F).add(base);
			o.blade(tip, new Vector3f(a).mul(0.3F * scale).add(tip), u, 0.08F * scale, argb(0.95F * alpha, 0x3A2A1A), argb(0.95F * alpha, 0x1A1410));
			// the gold grip below
			o.bar(base, new Vector3f(a).mul(-0.35F * scale).add(base), 0.05F * scale, 0.05F * scale, argb(0.95F * alpha, 0xC8961E), argb(0.95F * alpha, 0xC8961E));
		});
		glow(ps, c, o -> {
			for (int k = 0; k < 3; k++) {
				float spin = time * (k % 2 == 0 ? 0.6F : -0.8F);
				float rr = radius[k] * scale * 1.06F;
				for (int g = 0; g < 4; g++) {
					// glyph lines winding round the cylinder
					float t0 = spin + Mth.HALF_PI * g;
					Vector3f from = new Vector3f(a).mul(segment * k).add(base).add(new Vector3f(u).mul(Mth.cos(t0) * rr)).add(new Vector3f(v).mul(Mth.sin(t0) * rr));
					Vector3f to = new Vector3f(a).mul(segment * (k + 0.9F)).add(base).add(new Vector3f(u).mul(Mth.cos(t0 + 0.8F) * rr))
						.add(new Vector3f(v).mul(Mth.sin(t0 + 0.8F) * rr));
					o.bar(from, to, 0.018F * scale, 0.018F * scale, argb(alpha, RUPTURE), argb(alpha, 0xFF8A6A));
				}
				o.circle(new Vector3f(a).mul(segment * k).add(base), u, v, rr, 0.015F * scale, 12, argb(alpha, RUPTURE));
			}
			o.glowLine(base, new Vector3f(a).mul(segment * 3.2F).add(base), 0.35F * scale, argb(0.4F * alpha, RUPTURE), argb(0.4F * alpha, RUPTURE));
		});
	}

	/** A closed prism of {@code sides} from {@code a} to {@code b}, coloured {@code c0} at one end to {@code c1} at the other. */
	private static void prism(final VfxOut o, final Vector3f a, final Vector3f b, final Vector3f u, final Vector3f v, final float r, final int sides, final int c0,
		final int c1) {
		for (int i = 0; i < sides; i++) {
			float t0 = Mth.TWO_PI * i / sides;
			float t1 = Mth.TWO_PI * (i + 1) / sides;
			Vector3f o0 = new Vector3f(u).mul(Mth.cos(t0) * r).add(new Vector3f(v).mul(Mth.sin(t0) * r));
			Vector3f o1 = new Vector3f(u).mul(Mth.cos(t1) * r).add(new Vector3f(v).mul(Mth.sin(t1) * r));
			o.quad(new Vector3f(a).add(o0), new Vector3f(a).add(o1), new Vector3f(b).add(o1), new Vector3f(b).add(o0), c0, c0, c1, c1);
			o.tri(a, new Vector3f(a).add(o0), new Vector3f(a).add(o1), c0, c0, c0);
			o.tri(b, new Vector3f(b).add(o1), new Vector3f(b).add(o0), c1, c1, c1);
		}
	}

	/** Enuma Elish winding up: Ea raised in the right hand, its cylinders turning, red wind spiralling in to it from all round. */
	private static void eaCharge(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.85F, 1.0F);
		float time = s.ageInTicks;
		float yaw = -s.bodyYaw * Mth.DEG_TO_RAD;
		ps.pushPose();
		ps.rotate(Axis.YP.rotation(yaw));
		// the camera's axes in the turned frame, so sprites still face it
		Vector3f camRight = new Vector3f(s.camRight).rotateY(-yaw);
		Vector3f camUp = new Vector3f(s.camUp).rotateY(-yaw);
		// in the body's frame: +z ahead, -x to the right
		Vector3f hand = s.selfView ? new Vector3f(-0.45F, 1.15F, 0.7F) : new Vector3f(-0.42F, 1.25F, 0.3F);
		Vector3f axis = new Vector3f(-0.1F, 0.85F, 0.45F);
		ea(s, ps, c, hand, axis, 1.0F, fade);
		glow(ps, c, o -> {
			Vector3f top = new Vector3f(axis).normalize(1.3F).add(hand);
			for (int arm = 0; arm < 4; arm++) {
				Vector3f prev = null;
				int steps = 22;
				for (int i = 0; i <= steps; i++) {
					float t = (float)i / steps;
					float a = arm * Mth.HALF_PI + t * 4.0F - time * 0.5F;
					float r = 5.0F * (1.0F - t) * (1.0F - 0.5F * ease(p, 0.0F, 1.0F)) + 0.2F;
					Vector3f q = new Vector3f(Mth.cos(a) * r, 0.4F + t * 1.3F, Mth.sin(a) * r).lerp(top, t * t);
					if (prev != null) {
						o.glowLine(prev, q, 0.12F, argb(0.7F * fade * t, RUPTURE), argb(0.7F * fade * (t + 1.0F / steps), RUPTURE));
					}
					prev = q;
				}
			}
			o.sprite(top, camRight, camUp, 0.9F, argb(0.7F * fade, RUPTURE));
			o.ring(new Vector3f(0, 0.14F, 0), 0.5F, 2.2F, argb(0.5F * fade, RUPTURE));
		});
		ps.popPose();
	}

	/**
	 * Enuma Elish: the rupture of heaven and earth along the beam: Ea turning at the caster's hand, a black-crimson core tearing
	 * straight out, three red spirals winding round it, rings of red force bursting outward along it and space cracking open beside it.
	 */
	private static void enumaElish(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float extend = ease(p, 0.0F, 0.22F);
		float fade = 1.0F - ease(p, 0.7F, 1.0F);
		float time = s.ageInTicks;
		Vector3f dir = new Vector3f(s.reach).normalize();
		Vector3f u = VfxOut.across(dir);
		Vector3f v = new Vector3f(dir).cross(u).normalize();
		float length = Math.max(1.0F, s.size * extend);
		float start = 1.6F;
		// Ea at the hand, pointing down the beam
		ea(s, ps, c, new Vector3f(dir).mul(0.3F).add(new Vector3f(u).mul(-0.25F)), dir, 1.2F, fade);
		int segments = Math.max(2, (int)(length / 1.0F));
		shade(ps, c, o -> {
			int sides = 8;
			for (int i = 0; i < segments; i++) {
				float d0 = start + (length - start) * i / segments;
				float d1 = start + (length - start) * (i + 1) / segments;
				float r0 = 0.3F + 0.6F * d0 / Math.max(1.0F, s.size);
				float r1 = 0.3F + 0.6F * d1 / Math.max(1.0F, s.size);
				for (int k = 0; k < sides; k++) {
					float t0 = Mth.TWO_PI * k / sides + time * 0.3F;
					float t1 = Mth.TWO_PI * (k + 1) / sides + time * 0.3F;
					o.quad(ringPoint(dir, u, v, d0, r0, t0), ringPoint(dir, u, v, d0, r0, t1), ringPoint(dir, u, v, d1, r1, t1), ringPoint(dir, u, v, d1, r1, t0),
						argb(0.9F * fade, k % 2 == 0 ? 0x050104 : 0x1E0206));
				}
			}
			// space torn open beside the beam: black cracks
			for (int k = 0; k < 6; k++) {
				float d = start + (length - start) * (0.15F + 0.7F * hash(s.seed + k * 3));
				crack(o, s, dir, u, v, d, k, 0.12F, argb(0.95F * fade, 0x050104));
			}
		});
		glow(ps, c, o -> {
			Vector3f from = new Vector3f(dir).mul(start);
			Vector3f tip = new Vector3f(dir).mul(length);
			o.glowLine(from, tip, 2.2F, argb(0.14F * fade, RUPTURE), argb(0.12F * fade, RUPTURE));
			// three spirals winding round the core
			for (int band = 0; band < 6; band++) {
				Vector3f prev = null;
				int steps = Math.min(96, segments * 3);
				for (int i = 0; i <= steps; i++) {
					float d = start + (length - start) * i / steps;
					float r = 0.9F + 1.6F * d / Math.max(1.0F, s.size);
					boolean back = band >= 3;
					float a = band * Mth.TWO_PI / 3 + (back ? -1 : 1) * (d * 0.9F - time * 1.1F);
					r *= back ? 1.35F : 1.0F;
					Vector3f q = ringPoint(dir, u, v, d, r, a);
					if (prev != null) {
						float al = band >= 3 ? 0.45F : 0.85F;
						o.glowLine(prev, q, 0.22F, argb(al * fade, RUPTURE), argb(al * fade, RUPTURE));
						o.bar(prev, q, 0.03F, 0.03F, argb(al * fade, 0xFFB0A0), argb(al * fade, 0xFFB0A0));
					}
					prev = q;
				}
			}
			// rings of force bursting outward along it
			for (int i = 0; i < 8; i++) {
				float d = start + (length - start) * (i + 0.5F) / 8;
				float q = (time * 0.07F + i * 0.37F) % 1.0F;
				o.circle(new Vector3f(dir).mul(d), u, v, 1.2F + 3.0F * q, 0.06F, 24, argb(0.7F * fade * (1.0F - q), RUPTURE));
			}
			for (int k = 0; k < 6; k++) {
				float d = start + (length - start) * (0.15F + 0.7F * hash(s.seed + k * 3));
				crack(o, s, dir, u, v, d, k, 0.22F, argb(0.6F * fade, RUPTURE));
			}
			o.star(tip, s.camRight, s.camUp, 3.0F, 8, 0.3F, time * 0.2F, argb(0.8F * fade, 0xFF8A70), argb(0.0F, RUPTURE));
			o.sprite(tip, s.camRight, s.camUp, 2.5F, argb(0.5F * fade, RUPTURE));
		});
	}

	private static Vector3f ringPoint(final Vector3f dir, final Vector3f u, final Vector3f v, final float d, final float r, final float a) {
		return new Vector3f(dir).mul(d).add(new Vector3f(u).mul(Mth.cos(a) * r)).add(new Vector3f(v).mul(Mth.sin(a) * r));
	}

	/** A jagged crack of torn space running out from the beam at {@code d}, with a branch. */
	private static void crack(final VfxOut o, final State s, final Vector3f dir, final Vector3f u, final Vector3f v, final float d, final int k, final float width,
		final int color) {
		float a = Mth.TWO_PI * hash(s.seed * 5 + k);
		Vector3f out = new Vector3f(u).mul(Mth.cos(a)).add(new Vector3f(v).mul(Mth.sin(a)));
		Vector3f prev = new Vector3f(dir).mul(d).add(new Vector3f(out).mul(1.2F));
		for (int j = 1; j <= 5; j++) {
			Vector3f next = new Vector3f(dir).mul(d + (hash(k * 13 + j) - 0.5F) * 1.6F).add(new Vector3f(out).mul(1.2F + 1.1F * j));
			o.bar(prev, next, width, width * 0.7F, color, color);
			if (j == 2) {
				Vector3f branch = new Vector3f(next).add(new Vector3f(dir).mul(1.0F)).add(new Vector3f(out).mul(0.7F));
				o.bar(next, branch, width * 0.7F, width * 0.3F, color, color);
			}
			prev = next;
		}
	}

	// ------------------------------------------------------------ the king's eye and body

	/**
	 * Sha Naqba Imuru: an eye opens over the caster, gold with a red slit; its sight sweeps the land in a ring of cuneiform marks
	 * running out to the skill's reach.
	 */
	private static void shaNaqba(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.2F);
		float sweep = ease(p, 0.05F, 0.55F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float time = s.ageInTicks;
		glow(ps, c, o -> {
			if (!s.selfView) {
				Vector3f eye = new Vector3f(0, 3.4F, 0);
				o.sprite(eye, s.camRight, s.camUp, 2.6F, argb(0.35F * fade, GOLD));
				// the lids: two arcs meeting at the corners
				int n = 14;
				Vector3f prevTop = null;
				Vector3f prevBottom = null;
				for (int i = 0; i <= n; i++) {
					float t = (float)i / n;
					float x = (t * 2 - 1) * 1.5F;
					float y = Mth.sin(t * Mth.PI) * 0.62F * open;
					Vector3f top = new Vector3f(s.camRight).mul(x).add(new Vector3f(s.camUp).mul(y)).add(eye);
					Vector3f bottom = new Vector3f(s.camRight).mul(x).add(new Vector3f(s.camUp).mul(-y)).add(eye);
					if (prevTop != null) {
						o.bar(prevTop, top, 0.05F, 0.05F, argb(0.95F * fade, GOLD), argb(0.95F * fade, GOLD));
						o.bar(prevBottom, bottom, 0.05F, 0.05F, argb(0.95F * fade, GOLD), argb(0.95F * fade, GOLD));
					}
					prevTop = top;
					prevBottom = bottom;
				}
				// iris, turning marks round it, and the red slit of the pupil
				o.circle(eye, s.camRight, s.camUp, 0.52F * open, 0.05F, 20, argb(fade, GOLD_PALE));
				for (int i = 0; i < 10; i++) {
					float a = Mth.TWO_PI * i / 10 + time * 0.05F;
					Vector3f d = new Vector3f(s.camRight).mul(Mth.cos(a)).add(new Vector3f(s.camUp).mul(Mth.sin(a)));
					o.bar(new Vector3f(d).mul(0.3F * open).add(eye), new Vector3f(d).mul(0.48F * open).add(eye), 0.02F, 0.02F, argb(0.8F * fade, GOLD),
						argb(0.8F * fade, GOLD));
				}
				o.bar(new Vector3f(s.camUp).mul(-0.36F * open).add(eye), new Vector3f(s.camUp).mul(0.36F * open).add(eye), 0.09F, 0.09F, argb(fade, 0xFF2020),
					argb(fade, 0xFF2020));
				// its gaze falling on the ground
				o.glowLine(eye, new Vector3f(0, 0.3F, 0), 0.5F, argb(0.3F * fade, GOLD), argb(0.0F, GOLD));
			}
			// the ring of sight running out, cuneiform marks on it, and the sweep
			Vector3f ground = new Vector3f(0, 0.14F, 0);
			float r = s.size * sweep;
			o.ring(ground, Math.max(0.0F, r - 0.6F), r, argb(0.85F * fade * (1.0F - 0.5F * sweep), GOLD));
			for (int i = 0; i < 40; i++) {
				float a = Mth.TWO_PI * i / 40;
				Vector3f at = polar(a, r - 0.3F, 0.15F);
				Vector3f d = polar(a, 1.0F, 0.0F);
				Vector3f side = new Vector3f(-d.z, 0, d.x);
				// a wedge: a short bar and its head
				o.bar(at, new Vector3f(d).mul(-0.35F).add(at), 0.03F, 0.03F, argb(0.8F * fade, GOLD_PALE), argb(0.8F * fade, GOLD_PALE));
				o.tri(at, new Vector3f(side).mul(0.1F).add(at).add(new Vector3f(d).mul(0.08F)), new Vector3f(side).mul(-0.1F).add(at).add(new Vector3f(d).mul(0.08F)),
					argb(0.8F * fade, GOLD_PALE), argb(0.8F * fade, GOLD_PALE), argb(0.8F * fade, GOLD_PALE));
			}
			float beam = time * 0.25F;
			o.quad(new Vector3f(0, 0.15F, 0), polar(beam, s.size * sweep, 0.15F), polar(beam - 0.5F, s.size * sweep, 0.15F), new Vector3f(0, 0.15F, 0),
				argb(0.5F * fade, GOLD), argb(0.4F * fade, GOLD), argb(0.0F, GOLD), argb(0.0F, GOLD));
		});
	}

	/** Golden Rule (Body): gold light rising off the caster in flames, rings of gold climbing the body and gold glints floating up. */
	private static void goldenRule(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.15F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		float top = s.selfView ? 0.9F : 2.3F;
		glow(ps, c, o -> {
			if (!s.selfView) {
				for (int i = 0; i < 10; i++) {
					float a = Mth.TWO_PI * i / 10;
					o.flame(polar(a, 0.5F, 0.1F), new Vector3f(Mth.cos(a) * 0.12F, 1, Mth.sin(a) * 0.12F), (1.6F + 0.8F * hash(i + s.seed)) * open, 0.45F,
						time * 0.6F + i * 1.9F, argb(0.55F * k, GOLD), argb(0.0F, GOLD));
				}
			}
			for (int i = 0; i < 3; i++) {
				float q = (time * 0.03F + i / 3.0F) % 1.0F;
				float y = q * top;
				float r = 0.85F - 0.3F * q;
				o.ring(new Vector3f(0, y + 0.1F, 0), r - 0.05F, r + 0.05F, argb(0.8F * k * Mth.sin(Mth.PI * q), GOLD_PALE));
			}
			for (int i = 0; i < 12; i++) {
				float rise = (time * 0.035F + hash(i * 5)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 11) + rise, 0.6F + 0.5F * hash(i), rise * (top + 0.6F));
				// a coin of light turning
				float spin = time * 0.3F + i;
				Vector3f face = new Vector3f(Mth.cos(spin), 0, Mth.sin(spin));
				o.sprite(at, face, UP, 0.1F, argb(0.95F * k * Mth.sin(Mth.PI * rise), GOLD));
			}
			o.ring(new Vector3f(0, 0.14F, 0), 0.6F, 1.4F, argb(0.6F * k, GOLD));
		});
	}

	// ------------------------------------------------------------ Ishtar

	/**
	 * An Gal Ta Kigal She: Venus's eight-pointed star opens on the aimed ground while the great star itself (a gem in its light) falls
	 * onto it out of the sky; where it lands, a blinding flash, a shock ring, a pillar of gold and gem shards flying.
	 */
	private static void venusStar(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float open = ease(age, 0.0F, 6.0F);
		float fade = 1.0F - ease(s.progress, 0.75F, 1.0F);
		// the strike lands 8 ticks after the cast
		float fall = Mth.clamp(age / 8.0F, 0.0F, 1.0F);
		float hit = Mth.clamp((age - 8.0F) / 14.0F, 0.0F, 1.0F);
		float time = s.ageInTicks;
		Vector3f x = new Vector3f(1, 0, 0);
		Vector3f z = new Vector3f(0, 0, 1);
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			o.star(ground, x, z, s.size * open, 8, 0.42F, time * 0.01F, argb(0.8F * fade, GOLD), argb(0.2F * fade, VENUS));
			o.star(new Vector3f(0, 0.16F, 0), x, z, s.size * 0.75F * open, 8, 0.4F, Mth.PI / 8 - time * 0.01F, argb(0.45F * fade, VENUS), argb(0.0F, GOLD));
			o.circle(ground, x, z, s.size * 1.05F * open, 0.06F, 48, argb(0.8F * fade, GOLD));
			if (hit <= 0.0F) {
				Vector3f star = new Vector3f(-1.5F * (1 - fall), 0.8F + 14.0F * (1.0F - fall * fall), -1.5F * (1 - fall));
				o.glowLine(star, new Vector3f(star).add(2.0F, 8.0F, 2.0F), 1.0F, argb(0.5F, GOLD), argb(0.0F, VENUS));
				o.sprite(star, s.camRight, s.camUp, 3.2F, argb(0.6F, VENUS));
				o.sprite(star, s.camRight, s.camUp, 1.4F, argb(0.95F, GOLD_PALE));
				o.star(star, s.camRight, s.camUp, 4.5F, 8, 0.2F, time * 0.1F, argb(0.95F, 0xFFFFFF), argb(0.0F, GOLD));
				gemEdges(o, star, 1.3F, time * 0.2F, argb(0.95F, 0xFFFFFF));
			} else {
				float k = 1.0F - hit;
				Vector3f mid = new Vector3f(0, 1.0F, 0);
				o.sprite(mid, s.camRight, s.camUp, 5.0F * k + 0.5F, argb(0.85F * k, GOLD_PALE));
				o.star(mid, s.camRight, s.camUp, 6.0F * k + 1.0F, 8, 0.15F, 0.3F, argb(0.9F * k, 0xFFFFFF), argb(0.0F, GOLD));
				o.ring(ground, s.size * 1.6F * hit, s.size * 1.6F * hit + 0.6F, argb(0.9F * k, VENUS));
				o.glowLine(new Vector3f(0, 0.1F, 0), new Vector3f(0, 16.0F, 0), 1.8F * k, argb(0.6F * k, GOLD), argb(0.0F, GOLD));
				for (int i = 0; i < 16; i++) {
					Vector3f d = BladeLooks.shardDir(s.seed, i);
					d.y = Math.abs(d.y) * 0.8F + 0.2F;
					Vector3f at = new Vector3f(d).mul(0.5F + 6.0F * hit).add(mid);
					o.diamond(at, d, VfxOut.across(d), 0.6F * k + 0.1F, 0.25F, argb(0.3F * k, VENUS), argb(0.95F * k, i % 2 == 0 ? GOLD_PALE : ROSE));
				}
			}
		});
	}

	/** A gem's lit edges: an octahedron of {@code r} turning round the vertical. */
	private static void gemEdges(final VfxOut o, final Vector3f c, final float r, final float spin, final int color) {
		Vector3f top = new Vector3f(0, r * 1.2F, 0).add(c);
		Vector3f bottom = new Vector3f(0, -r * 1.2F, 0).add(c);
		for (int i = 0; i < 4; i++) {
			Vector3f a = polar(spin + Mth.HALF_PI * i, r, 0).add(c);
			Vector3f b = polar(spin + Mth.HALF_PI * (i + 1), r, 0).add(c);
			o.bar(a, b, 0.04F, 0.04F, color, color);
			o.bar(a, top, 0.04F, 0.04F, color, color);
			o.bar(a, bottom, 0.04F, 0.04F, color, color);
		}
	}

	/** Manifestation of Beauty: Ishtar's star turning on the ground round the caster, a rose-gold glow rising, a ring washing out. */
	private static void beauty(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.2F);
		float fade = 1.0F - ease(p, 0.75F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		Vector3f x = new Vector3f(1, 0, 0);
		Vector3f z = new Vector3f(0, 0, 1);
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			o.star(ground, x, z, 4.0F * open, 8, 0.4F, time * 0.02F, argb(0.8F * k, GOLD), argb(0.15F * k, ROSE));
			o.circle(ground, x, z, 4.1F * open, 0.05F, 40, argb(0.8F * k, ROSE));
			float wash = ease(p, 0.05F, 0.6F);
			o.ring(ground, Math.max(0.0F, s.size * wash - 0.5F), s.size * wash, argb(0.7F * fade * (1.0F - wash * 0.6F), ROSE));
			if (!s.selfView) {
				o.wall(new Vector3f(), 1.0F, 0.0F, 3.0F * open, argb(0.3F * k, ROSE), argb(0.0F, GOLD));
			}
			for (int i = 0; i < 14; i++) {
				float rise = (time * 0.03F + hash(i * 7)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 3) + rise * 1.5F, 0.6F + 1.2F * hash(i), rise * 3.0F);
				if (s.selfView && at.y > 1.0F) {
					continue;
				}
				float a = k * Mth.sin(Mth.PI * rise);
				if (i % 3 == 0) {
					ArrowLooks.heart(o, at, s.camRight, s.camUp, 0.14F, argb(0.9F * a, ROSE));
				} else {
					o.glint(at, s.camRight, s.camUp, 0.18F, time * 0.1F + i, argb(0.9F * a, i % 2 == 0 ? GOLD_PALE : ROSE));
				}
			}
		});
	}

	/** Goddess's Flight: wings of golden light spread from the caster's back, ruby at their tips, glints falling off them. */
	private static void venusWings(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		if (s.selfView) {
			return;
		}
		float p = s.progress;
		float open = ease(p, 0.0F, 0.08F);
		float fade = 1.0F - ease(p, 0.85F, 1.0F);
		float yaw = -s.bodyYaw * Mth.DEG_TO_RAD;
		float time = s.ageInTicks;
		ps.pushPose();
		ps.translate(0.0F, 1.3F, 0.0F);
		ps.rotate(Axis.YP.rotation(yaw));
		// the camera's axes in the turned frame, so sprites still face it
		Vector3f camRight = new Vector3f(s.camRight).rotateY(-yaw);
		Vector3f camUp = new Vector3f(s.camUp).rotateY(-yaw);
		float beat = 0.2F * Mth.sin(time * 0.3F);
		glow(ps, c, o -> {
			Vector3f root = new Vector3f(0, 0.1F, -0.3F);
			for (int w = -1; w <= 1; w += 2) {
				for (int k = 0; k < 8; k++) {
					float a = (-0.4F + 1.3F * k / 7.0F) * open + beat;
					Vector3f d = new Vector3f(w * Mth.cos(a), Mth.sin(a) + 0.2F, -0.4F - 0.08F * k).normalize();
					float len = (1.2F + 1.6F * Mth.sin(Mth.PI * (k + 1) / 9.0F)) * (0.3F + 0.7F * open);
					Vector3f from = new Vector3f(w * 0.12F, 0, 0).add(root);
					Vector3f tip = new Vector3f(d).mul(len).add(from);
					o.blade(from, tip, new Vector3f(0, 0, 1).cross(d).normalize(), 0.16F, argb(0.85F * fade, GOLD_PALE), argb(0.8F * fade, VENUS));
					o.glowLine(from, tip, 0.12F, argb(0.35F * fade, GOLD), argb(0.0F, GOLD));
					if (k % 2 == 0) {
						float drop = (time * 0.05F + hash(k * 3 + w)) % 1.0F;
						o.glint(new Vector3f(tip).add(0, -drop * 1.5F, 0), camRight, camUp, 0.15F, time * 0.1F + k, argb(0.9F * fade * (1 - drop), GOLD_PALE));
					}
				}
			}
			o.sprite(root, camRight, camUp, 0.8F * open, argb(0.4F * fade, GOLD));
		});
		ps.popPose();
	}

	private KingLooks() {
	}
}
