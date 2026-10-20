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
 * Looks of the archers of war: EMIYA Alter (Gun Kata's falchions, Unlimited Lost Works, the Counter Guardian's shroud) and the heroes of
 * India (Pashupata's third eye and its bolt, the great discus, the blessing of the endowed, mana burst flames).
 */
final class WarLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final int BLOOD = 0xE02A2A;
	private static final int STEEL = 0x9AA0AA;
	private static final int DIVINE = 0xBFEFFF;
	private static final int FLAME_CORE = 0xFFE6A0;
	private static final int FLAME_TIP = 0xFF4A00;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case GUN_KATA -> gunKata(s, ps, c);
			case ULW_FIELD -> ulwField(s, ps, c);
			case COUNTER_GUARDIAN -> counterGuardian(s, ps, c);
			case PASHUPATA_BOLT -> pashupataBolt(s, ps, c);
			case PASHUPATA_DISC -> pashupataDisc(s, ps, c);
			case ENDOWED -> endowed(s, ps, c);
			case MANA_FLAME -> manaFlame(s, ps, c);
			default -> {
			}
		}
	}

	// ------------------------------------------------------------ EMIYA Alter

	/**
	 * Gun Kata: Kanshou (black, red-edged) and Bakuya (white) whirl round the caster twice, each leaving its arc; muzzle flashes burst
	 * out over the fan the shots fly in, and a red ring runs out over the cut.
	 */
	private static void gunKata(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float spin = ease(p, 0.0F, 0.7F) * Mth.TWO_PI * 2.0F;
		float fade = 1.0F - ease(p, 0.6F, 1.0F);
		float yaw = (float)Math.atan2(s.forward.z, s.forward.x);
		float r = Math.min(2.0F, s.size * 0.5F);
		float y = 1.1F;
		Vector3f[] tips = new Vector3f[2];
		Vector3f[] roots = new Vector3f[2];
		for (int k = 0; k < 2; k++) {
			float a = yaw + spin + k * Mth.PI;
			roots[k] = polar(a, 0.5F, y);
			tips[k] = polar(a + 0.25F, r + 0.8F, y + (k == 0 ? 0.15F : -0.15F));
		}
		shade(ps, c, o -> {
			// Kanshou's black body
			o.blade(roots[0], tips[0], UP, 0.26F, argb(0.95F * fade, 0x101014), argb(0.95F * fade, 0x24242A));
		});
		glow(ps, c, o -> {
			for (int k = 0; k < 2; k++) {
				int edge = k == 0 ? BLOOD : 0xFFFFFF;
				// the arc each blade leaves behind
				Vector3f prev = tips[k];
				for (int j = 1; j <= 10; j++) {
					float a = yaw + spin + k * Mth.PI + 0.25F - 0.18F * j;
					Vector3f q = polar(a, r + 0.8F, y + (k == 0 ? 0.15F : -0.15F));
					float al = 0.8F * fade * (1 - j / 11.0F);
					o.quad(prev, q, new Vector3f(q).mul(0.75F, 1, 0.75F), new Vector3f(prev).mul(0.75F, 1, 0.75F), argb(al, edge), argb(al * 0.8F, edge),
						argb(0.0F, edge), argb(0.0F, edge));
					prev = q;
				}
				if (k == 1) {
					o.blade(roots[1], tips[1], UP, 0.26F, argb(0.95F * fade, 0xFFFFFF), argb(0.9F * fade, 0xE8ECF4));
				}
				o.glowLine(roots[k], tips[k], 0.05F, argb(0.9F * fade, edge), argb(0.9F * fade, edge));
			}
			// muzzle flashes over the shots' fan
			float flash = 1.0F - ease(p, 0.05F, 0.4F);
			if (flash > 0.0F) {
				for (int i = 0; i < 5; i++) {
					float a = yaw + (-30.0F + 15.0F * i) * Mth.DEG_TO_RAD;
					Vector3f at = polar(a, 1.0F, 1.45F);
					o.star(at, s.camRight, s.camUp, 0.9F * flash + 0.2F, 6, 0.3F, i, argb(0.95F * flash, 0xFFF0C0), argb(0.0F, 0xFF7A20));
				}
			}
			float ring = ease(p, 0.0F, 0.5F) * s.size;
			o.ring(new Vector3f(0, 0.14F, 0), Math.max(0.0F, ring - 0.4F), ring, argb(0.8F * fade, BLOOD));
		});
	}

	/**
	 * Unlimited Lost Works: swords stab down out of the sky and stay standing in the ground all over the circle, dark steel with red
	 * light in their edges; a red glow under them and great gears turning slowly in the sky above.
	 */
	private static void ulwField(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float fade = 1.0F - ease(s.progress, 0.85F, 1.0F);
		float r = s.size;
		int swords = 22;
		Vector3f[] base = new Vector3f[swords];
		Vector3f[] tip = new Vector3f[swords];
		Vector3f[] side = new Vector3f[swords];
		float[] land = new float[swords];
		for (int i = 0; i < swords; i++) {
			float a = Mth.TWO_PI * hash(s.seed + i * 3);
			float d = r * (float)Math.sqrt(hash(s.seed * 7 + i * 5));
			float lean = (hash(i * 13) - 0.5F) * 0.5F;
			Vector3f dir = new Vector3f(Mth.sin(lean) * Mth.cos(a * 3), Mth.cos(lean), Mth.sin(lean) * Mth.sin(a * 3)).normalize();
			// each falls in a few ticks after the one before and sinks a third of its length in
			float start = i * 0.9F;
			float drop = Mth.clamp((age - start) / 3.0F, 0.0F, 1.0F);
			land[i] = drop;
			float length = 2.2F + 1.2F * hash(i * 17);
			Vector3f ground = polar(a, d, 0.0F);
			float above = (1.0F - drop) * 9.0F;
			base[i] = new Vector3f(dir).mul(-0.5F + above).add(ground);
			tip[i] = new Vector3f(dir).mul(length + above).add(ground);
			side[i] = VfxOut.across(dir).rotateAxis(hash(i * 19) * Mth.PI, dir.x, dir.y, dir.z);
		}
		shade(ps, c, o -> {
			for (int i = 0; i < swords; i++) {
				if (land[i] <= 0.0F) {
					continue;
				}
				Vector3f d = new Vector3f(tip[i]).sub(base[i]).normalize();
				// the blade rises out of the ground (its foot is under the floor), a crossguard and hilt over it
				Vector3f guard = new Vector3f(tip[i]);
				Vector3f bladeTip = new Vector3f(base[i]);
				o.blade(guard, bladeTip, side[i], 0.2F, argb(0.95F * fade, 0x2A2C32), argb(0.95F * fade, 0x50545C));
				o.bar(new Vector3f(side[i]).mul(-0.28F).add(guard), new Vector3f(side[i]).mul(0.28F).add(guard), 0.05F, 0.05F, argb(0.95F * fade, 0x3A2018),
					argb(0.95F * fade, 0x3A2018));
				o.bar(guard, new Vector3f(d).mul(0.4F).add(guard), 0.04F, 0.035F, argb(0.95F * fade, 0x2A1A14), argb(0.95F * fade, 0x2A1A14));
			}
			// the gears' dark bodies
			for (int g = 0; g < 3; g++) {
				gear(o, s, g, argb(0.55F * fade * ease(age, 4.0F, 20.0F), 0x2A1410), true);
			}
		});
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.14F, 0);
			o.disc(ground, r * 1.1F, argb(0.35F * fade, 0xA0201A), argb(0.0F, BLOOD));
			o.ring(ground, r * 1.05F, r * 1.15F, argb(0.8F * fade, BLOOD));
			for (int i = 0; i < swords; i++) {
				if (land[i] <= 0.0F) {
					continue;
				}
				float hot = land[i] < 1.0F ? 1.0F : 0.6F;
				o.glowLine(base[i], tip[i], 0.06F, argb(0.8F * hot * fade, BLOOD), argb(0.4F * fade, 0xFF6A50));
				if (land[i] < 1.0F) {
					// the streak of its fall
					Vector3f up = new Vector3f(tip[i]).add(0, 3.0F, 0);
					o.glowLine(tip[i], up, 0.15F, argb(0.6F * fade, BLOOD), argb(0.0F, BLOOD));
				} else {
					Vector3f foot = new Vector3f(tip[i]).sub(base[i]).normalize().mul(0.5F).add(base[i]);
					o.sprite(new Vector3f(foot.x, 0.2F, foot.z), s.camRight, s.camUp, 0.45F, argb(0.5F * fade, 0xFF5A30));
				}
			}
			for (int g = 0; g < 3; g++) {
				gear(o, s, g, argb(0.7F * fade * ease(age, 4.0F, 20.0F), 0xFF4A2A), false);
			}
			o.sprite(new Vector3f(0, 10.0F, 0), s.camRight, s.camUp, r * 2.2F, argb(0.18F * fade * ease(age, 4.0F, 20.0F), 0xB0201A));
		});
	}

	/** One of the gears over Unlimited Lost Works, lying tilted in the sky: a toothed wheel with spokes. */
	private static void gear(final VfxOut o, final State s, final int g, final int color, final boolean body) {
		float r = 2.2F + 1.3F * g;
		Vector3f center = polar(Mth.TWO_PI * g / 3 + 0.6F, s.size * 0.7F, 9.0F + 1.5F * g);
		float tilt = 0.35F + 0.15F * g;
		Vector3f a = new Vector3f(1, Mth.sin(tilt), 0).normalize();
		Vector3f b = new Vector3f(0, Mth.sin(tilt * 0.6F), 1).normalize();
		float spin = s.ageInTicks * 0.015F * (g % 2 == 0 ? 1 : -1);
		int teeth = 12 + 4 * g;
		for (int i = 0; i < teeth; i++) {
			float t0 = spin + Mth.TWO_PI * i / teeth;
			float t1 = spin + Mth.TWO_PI * (i + 0.5F) / teeth;
			float t2 = spin + Mth.TWO_PI * (i + 1) / teeth;
			Vector3f in0 = point(center, a, b, t0, r * 0.82F);
			Vector3f in2 = point(center, a, b, t2, r * 0.82F);
			Vector3f out0 = point(center, a, b, t0, r);
			Vector3f out1 = point(center, a, b, t1, r);
			Vector3f tooth0 = point(center, a, b, t0 + 0.04F, r * 1.14F);
			Vector3f tooth1 = point(center, a, b, t1 - 0.04F, r * 1.14F);
			if (body) {
				o.quad(in0, out0, point(center, a, b, t2, r), in2, color);
				o.quad(out0, tooth0, tooth1, out1, color);
			} else {
				o.bar(out0, tooth0, 0.04F, 0.04F, color, color);
				o.bar(tooth0, tooth1, 0.04F, 0.04F, color, color);
				o.bar(tooth1, out1, 0.04F, 0.04F, color, color);
				o.bar(out1, point(center, a, b, t2, r), 0.04F, 0.04F, color, color);
			}
		}
		if (body) {
			o.quad(point(center, a, b, spin, r * 0.3F), point(center, a, b, spin + 2.1F, r * 0.3F), point(center, a, b, spin + 4.2F, r * 0.3F),
				point(center, a, b, spin + 4.2F, r * 0.3F), color);
		} else {
			o.circle(center, a, b, r * 0.82F, 0.035F, 32, color);
			o.circle(center, a, b, r * 0.3F, 0.035F, 16, color);
			for (int i = 0; i < 6; i++) {
				float t = spin + Mth.TWO_PI * i / 6;
				o.bar(point(center, a, b, t, r * 0.3F), point(center, a, b, t, r * 0.82F), 0.035F, 0.035F, color, color);
			}
		}
	}

	private static Vector3f point(final Vector3f c, final Vector3f a, final Vector3f b, final float t, final float r) {
		return new Vector3f(a).mul(Mth.cos(t) * r).add(new Vector3f(b).mul(Mth.sin(t) * r)).add(c);
	}

	/**
	 * Counter Guardian: a tattered dark shroud streaming off the caster's back, red lines like magic circuits running up the body, a
	 * red ring at the feet and embers rising. In first person only the ring, the circuits at the feet and the embers show.
	 */
	private static void counterGuardian(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.15F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		float yaw = -s.bodyYaw * Mth.DEG_TO_RAD;
		ps.pushPose();
		ps.rotate(com.mojang.math.Axis.YP.rotation(yaw));
		// the camera's axes in the turned frame, so sprites still face it
		Vector3f camRight = new Vector3f(s.camRight).rotateY(-yaw);
		Vector3f camUp = new Vector3f(s.camUp).rotateY(-yaw);
		// in the body's frame: +z ahead
		int strips = 7;
		if (!s.selfView) {
			shade(ps, c, o -> {
				for (int i = 0; i < strips; i++) {
					float x = -0.4F + 0.8F * i / (strips - 1);
					float len = (1.3F + 0.6F * hash(s.seed + i)) * open;
					Vector3f prevL = new Vector3f(x - 0.06F, 1.55F, -0.2F);
					Vector3f prevR = new Vector3f(x + 0.06F, 1.55F, -0.2F);
					int steps = 6;
					for (int j = 1; j <= steps; j++) {
						float t = (float)j / steps;
						float wave = Mth.sin(time * 0.35F + i * 1.3F + t * 4.0F) * 0.18F * t;
						float spread = 1.0F + 0.6F * t;
						Vector3f l = new Vector3f((x - 0.11F) * spread + wave, 1.55F - len * t, -0.2F - 0.55F * t - 0.1F * Mth.sin(time * 0.2F + i));
						Vector3f r = new Vector3f((x + 0.11F) * spread + wave, 1.55F - len * t, -0.2F - 0.55F * t - 0.1F * Mth.sin(time * 0.2F + i));
						int col = argb(0.85F * k * (1.0F - 0.6F * t), j % 2 == 0 ? 0x1A0606 : 0x2A0A0A);
						o.quad(prevL, prevR, r, l, col);
						prevL = l;
						prevR = r;
					}
				}
				// a hood-like mantle over the shoulders
				o.quad(new Vector3f(-0.45F, 1.45F, -0.15F), new Vector3f(0.45F, 1.45F, -0.15F), new Vector3f(0.3F, 1.75F, -0.25F), new Vector3f(-0.3F, 1.75F, -0.25F),
					argb(0.8F * k, 0x1A0606));
			});
		}
		glow(ps, c, o -> {
			o.ring(new Vector3f(0, 0.14F, 0), 0.8F, 1.6F, argb(0.6F * k, BLOOD));
			// circuits: red lines climbing the body, branching at right angles
			float climb = ease(p, 0.0F, 0.3F);
			for (int i = 0; i < 8; i++) {
				float a = Mth.TWO_PI * i / 8 + 0.2F;
				float r = 0.42F;
				float top = (s.selfView ? 0.5F : 1.7F) * climb;
				Vector3f prev = polar(a, r, 0.1F);
				int steps = 4;
				for (int j = 1; j <= steps; j++) {
					float y = 0.1F + top * j / steps;
					float turn = (hash(i * 7 + j) - 0.5F) * 0.5F;
					Vector3f across = polar(a + turn, r, prev.y);
					Vector3f upTo = polar(a + turn, r, y);
					o.bar(prev, across, 0.02F, 0.02F, argb(0.9F * k, BLOOD), argb(0.9F * k, BLOOD));
					o.bar(across, upTo, 0.02F, 0.02F, argb(0.9F * k, BLOOD), argb(0.9F * k, 0xFF8A70));
					prev = upTo;
				}
			}
			if (!s.selfView) {
				for (int i = 0; i < strips; i += 2) {
					float x = -0.4F + 0.8F * i / (strips - 1);
					float len = (1.3F + 0.6F * hash(s.seed + i)) * open;
					o.glowLine(new Vector3f(x, 1.55F, -0.2F), new Vector3f(x * 1.6F, 1.55F - len, -0.75F), 0.06F, argb(0.5F * k, BLOOD), argb(0.0F, BLOOD));
					// the torn hem glowing
					o.sprite(new Vector3f(x * 1.6F, 1.55F - len, -0.75F), camRight, camUp, 0.25F, argb(0.6F * k, BLOOD));
				}
			}
			for (int i = 0; i < 12; i++) {
				float rise = (time * 0.04F + hash(i * 3)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 9), 0.5F + 0.7F * hash(i), rise * 2.6F);
				if (s.selfView && at.y > 1.0F) {
					continue;
				}
				o.sprite(at, camRight, camUp, 0.08F, argb(0.9F * k * Mth.sin(Mth.PI * rise), 0xFF6A40));
			}
		});
		ps.popPose();
	}

	// ------------------------------------------------------------ Arjuna and the destroyer

	/**
	 * Gandiva's Pashupata: high over the aimed spot the destroyer's third eye opens between three stripes of ash, a blue-white bolt
	 * leaps from it to the ground and the ground bursts in a ring under a sigil.
	 */
	private static void pashupataBolt(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.15F);
		float fade = 1.0F - ease(p, 0.7F, 1.0F);
		float strike = ease(p, 0.08F, 0.2F);
		Vector3f eye = new Vector3f(0, 12.0F, 0);
		glow(ps, c, o -> {
			thirdEye(o, s, eye, 2.4F, open, fade);
			if (strike > 0.0F) {
				// the bolt: jagged, re-struck every other tick
				int seed = s.seed * 31 + (int)(s.ageInTicks / 2);
				Vector3f prev = new Vector3f(eye).add(0, -2.4F, 0);
				int steps = 10;
				for (int i = 1; i <= steps; i++) {
					float t = (float)i / steps * strike;
					Vector3f next = new Vector3f(eye).lerp(new Vector3f(0, 0.1F, 0), t);
					if (i < steps) {
						next.add((hash(seed + i) - 0.5F) * 1.4F, 0, (hash(seed * 3 + i) - 0.5F) * 1.4F);
					}
					o.glowLine(prev, next, 1.1F, argb(0.55F * fade, s.color), argb(0.55F * fade, s.color));
					o.bar(prev, next, 0.13F, 0.13F, argb(fade, 0xFFFFFF), argb(fade, 0xFFFFFF));
					prev = next;
				}
			}
			float burst = ease(p, 0.15F, 0.6F);
			if (burst > 0.0F) {
				Vector3f ground = new Vector3f(0, 0.14F, 0);
				o.ring(ground, s.size * burst, s.size * burst + 0.5F, argb(0.85F * (1 - burst), DIVINE));
				o.disc(ground, s.size * 0.9F, argb(0.4F * fade, s.color), argb(0.0F, s.color));
				sigil(o, s, ground, s.size * 0.8F, argb(0.8F * fade, DIVINE));
				o.sprite(new Vector3f(0, 0.8F, 0), s.camRight, s.camUp, 2.5F * (1 - burst) + 0.5F, argb(0.8F * fade, 0xFFFFFF));
			}
		});
	}

	/** The destroyer's third eye facing the camera: an upright almond opening, a blazing iris, three stripes of ash across over it. */
	private static void thirdEye(final VfxOut o, final State s, final Vector3f c, final float r, final float open, final float alpha) {
		Vector3f right = s.camRight;
		Vector3f up = s.camUp;
		o.sprite(c, right, up, r * 2.4F, argb(0.3F * alpha, s.color));
		// the lids: an upright almond (the third eye stands on the brow)
		int n = 12;
		Vector3f prevL = null;
		Vector3f prevR = null;
		for (int i = 0; i <= n; i++) {
			float t = (float)i / n;
			float y = (t * 2 - 1) * r;
			float x = Mth.sin(t * Mth.PI) * r * 0.42F * open;
			Vector3f l = new Vector3f(right).mul(-x).add(new Vector3f(up).mul(y)).add(c);
			Vector3f rr = new Vector3f(right).mul(x).add(new Vector3f(up).mul(y)).add(c);
			if (prevL != null) {
				o.bar(prevL, l, 0.06F, 0.06F, argb(0.95F * alpha, DIVINE), argb(0.95F * alpha, DIVINE));
				o.bar(prevR, rr, 0.06F, 0.06F, argb(0.95F * alpha, DIVINE), argb(0.95F * alpha, DIVINE));
				o.quad(prevL, prevR, rr, l, argb(0.35F * alpha * open, s.color));
			}
			prevL = l;
			prevR = rr;
		}
		o.sprite(c, right, up, r * 0.32F * open, argb(alpha, 0xFFFFFF));
		o.sprite(c, right, up, r * 0.6F * open, argb(0.7F * alpha, s.color));
		// three stripes of ash across the brow
		for (int i = -1; i <= 1; i++) {
			Vector3f mid = new Vector3f(up).mul(r * 1.45F + i * 0.28F).add(c);
			o.bar(new Vector3f(right).mul(-r * 1.1F).add(mid), new Vector3f(right).mul(r * 1.1F).add(mid), 0.07F, 0.07F, argb(0.85F * alpha, 0xFFFFFF),
				argb(0.85F * alpha, 0xFFFFFF));
		}
	}

	/** A sigil on the ground: two interlaced triangles in a circle, small circles at their points. */
	private static void sigil(final VfxOut o, final State s, final Vector3f c, final float r, final int color) {
		Vector3f x = new Vector3f(1, 0, 0);
		Vector3f z = new Vector3f(0, 0, 1);
		o.circle(c, x, z, r, 0.05F, 40, color);
		float spin = s.ageInTicks * 0.02F;
		for (int t = 0; t < 2; t++) {
			for (int i = 0; i < 3; i++) {
				float a0 = spin + t * Mth.PI / 3 + Mth.TWO_PI * i / 3;
				float a1 = spin + t * Mth.PI / 3 + Mth.TWO_PI * (i + 1) / 3;
				o.bar(polar(a0, r, 0).add(c), polar(a1, r, 0).add(c), 0.05F, 0.05F, color, color);
				o.circle(polar(a0, r, 0).add(c), x, z, r * 0.1F, 0.03F, 12, color);
			}
		}
	}

	/**
	 * The chakram's Pashupata: the destroyer's sigil opens on the aimed ground while a great discus of light spins down onto it out of
	 * the sky; where it lands, a flash, a ring and a pillar of light, and the discus grinds on, fading.
	 */
	private static void pashupataDisc(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.75F, 1.0F);
		float open = ease(p, 0.0F, 0.2F);
		// the strike lands 8 ticks after the cast
		float fall = Mth.clamp(s.age() / 8.0F, 0.0F, 1.0F);
		float hit = Mth.clamp((s.age() - 8.0F) / 10.0F, 0.0F, 1.0F);
		float time = s.ageInTicks;
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.14F, 0);
			sigil(o, s, ground, s.size * open, argb(0.85F * fade, DIVINE));
			o.circle(ground, new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), s.size * open * 1.1F, 0.04F, 40, argb(0.6F * fade, s.color));
			Vector3f disc = new Vector3f(0, 0.4F + 10.0F * (1.0F - fall * fall), 0);
			// tilted so it shows its face from most sides instead of a line
			ArrowLooks.disc(o, disc, new Vector3f(1, 0, 0), new Vector3f(0, Mth.sin(0.8F), Mth.cos(0.8F)), s.size * 0.55F, time * 0.9F, fade, s.color);
			o.sprite(disc, s.camRight, s.camUp, 1.2F, argb(0.6F * fade, 0xFFFFFF));
			if (fall < 1.0F) {
				o.glowLine(disc, new Vector3f(disc).add(0, 4.0F, 0), 0.6F, argb(0.5F * fade, s.color), argb(0.0F, s.color));
			}
			if (hit > 0.0F) {
				float k = 1.0F - hit;
				o.ring(ground, s.size * 1.4F * hit, s.size * 1.4F * hit + 0.5F, argb(0.9F * k, DIVINE));
				o.sprite(new Vector3f(0, 1.0F, 0), s.camRight, s.camUp, 3.0F * k + 0.5F, argb(0.9F * k, 0xFFFFFF));
				o.glowLine(new Vector3f(0, 0.1F, 0), new Vector3f(0, 12.0F, 0), 1.2F * k, argb(0.6F * k, s.color), argb(0.0F, s.color));
			}
		});
	}

	/**
	 * Hero of the Endowed: a halo of blessing over the head, light falling round the caster in a soft column with motes drifting down
	 * in it, and a ring of the weapon's colour at the feet.
	 */
	private static void endowed(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.2F);
		float fade = 1.0F - ease(p, 0.75F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		glow(ps, c, o -> {
			if (!s.selfView) {
				Vector3f halo = new Vector3f(0, 2.25F, 0);
				o.circle(halo, new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), 0.55F, 0.06F, 24, argb(0.95F * k, 0xFFE07A));
				o.ring(halo, 0.35F, 0.8F, argb(0.6F * k, 0xFFE07A));
				for (int i = 0; i < 8; i++) {
					float a = Mth.TWO_PI * i / 8 + time * 0.08F;
					o.glint(polar(a, 0.55F, 2.25F), s.camRight, s.camUp, 0.12F, i, argb(0.9F * k, 0xFFFFFF));
				}
				// the column of light, clear at both ends
				o.wall(new Vector3f(), 0.95F, 0.0F, 1.5F, argb(0.0F, s.color), argb(0.22F * k, s.color));
				o.wall(new Vector3f(), 0.95F, 1.5F, 1.5F + 3.5F * open, argb(0.22F * k, s.color), argb(0.0F, s.color));
			}
			o.ring(new Vector3f(0, 0.14F, 0), 0.7F, 1.5F * open, argb(0.7F * k, s.color));
			for (int i = 0; i < 16; i++) {
				float fall = (time * 0.025F + hash(i * 7)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 13), 0.3F + 0.7F * hash(i), 4.0F * (1.0F - fall));
				if (s.selfView && at.y > 1.2F) {
					continue;
				}
				o.glint(at, s.camRight, s.camUp, 0.16F, time * 0.1F + i, argb(0.9F * k * Mth.sin(Mth.PI * fall), i % 2 == 0 ? 0xFFF0B0 : DIVINE));
			}
		});
	}

	/** Mana Burst (Flame): flames bursting off the path the caster dashed, white at the root, and a scorched ring where they set off. */
	private static void manaFlame(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		List<Vector3f> points = VfxOut.path(s.trail);
		if (points.size() < 2) {
			return;
		}
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.35F, 1.0F);
		int n = points.size();
		int last = s.selfView ? Math.max(1, n - 2) : n - 1;
		float length = VfxOut.length(points, last);
		float time = s.ageInTicks;
		glow(ps, c, o -> {
			o.ring(new Vector3f(0, 0.14F, 0), 0.5F, 1.8F * ease(p, 0.0F, 0.4F) + 0.6F, argb(0.7F * fade, 0xFF7A20));
			int flames = Math.min(40, (int)(length / 0.45F));
			for (int i = 0; i <= flames; i++) {
				float t = flames == 0 ? 0.0F : (float)i / flames;
				Vector3f at = VfxOut.along(points, last, t);
				float h = (1.3F + 2.2F * hash(s.seed + i)) * fade * (0.5F + 0.5F * t);
				Vector3f side = new Vector3f((hash(i * 3) - 0.5F) * 0.8F, 0, (hash(i * 5) - 0.5F) * 0.8F);
				o.flame(new Vector3f(at).add(side).add(0, 0.05F, 0), UP, h, 0.75F, time * 0.8F + i * 1.7F, argb(0.9F * fade, FLAME_CORE), argb(0.0F, FLAME_TIP));
			}
			// the blast of the start
			o.sprite(new Vector3f(0, 1.0F, 0), s.camRight, s.camUp, 1.6F * (1 - ease(p, 0.0F, 0.4F)) + 0.2F, argb(0.7F * fade, 0xFF9A3A));
		});
	}

	private WarLooks() {
	}
}
