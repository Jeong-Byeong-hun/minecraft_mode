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

/**
 * Looks of the Shadow Monarch's daggers (Solo Leveling): Kasaka's Venom Fang (the great serpent's lair, its paralytic bite, the venom
 * mist and Sprint) and the Knight Killer (Knight's Bane, Mutilation, Killing Intent and the riposte).
 */
final class VenomLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final int VENOM = 0x76FF03;
	private static final int VENOM_PALE = 0xD8FF9A;
	private static final int VENOM_DEEP = 0x1E7A10;
	private static final int SCALE_LIT = 0x3C7A40;
	private static final int SCALE_DARK = 0x0A2010;
	private static final int FANG = 0xF4F0E0;
	private static final int MOUTH = 0x7A1020;
	private static final int SHADOW = 0x9A5CFF;
	private static final int SHADOW_PALE = 0xDCC8FF;
	private static final int INK = 0x0A0612;
	private static final int BLOOD = 0xE0182E;
	private static final int STEEL = 0xB8BEC8;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case SERPENT_LAIR -> serpentLair(s, ps, c);
			case VENOM_BITE -> venomBite(s, ps, c);
			case VENOM_MIST -> venomMist(s, ps, c);
			case SPRINT_TRAIL -> sprint(s, ps, c);
			case KNIGHTS_BANE -> knightsBane(s, ps, c);
			case MUTILATION -> mutilation(s, ps, c);
			case KILLING_INTENT -> killingIntent(s, ps, c);
			case RIPOSTE -> riposte(s, ps, c);
			default -> {
			}
		}
	}

	// ------------------------------------------------------------ Kasaka's Venom Fang

	/**
	 * Great Serpent's Lair: Kasaka, the great green serpent, bursts up out of the ground and coils once round the caster, the outside of
	 * its coil on the edge of the blast, then rears up in front of them with its jaws wide and its fangs dripping, a ring of venom running
	 * out to the coil and poison mist hanging inside it.
	 */
	private static void serpentLair(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = s.size;
		float time = s.ageInTicks;
		float fade = 1.0F - ease(p, 0.72F, 1.0F);
		float grow = ease(age, 0.0F, 8.0F);
		float rear = ease(age, 3.0F, 11.0F);
		float thick = Mth.clamp(r * 0.1F, 0.38F, 0.75F);
		// the outside of the coil lies on the blast's edge
		float rc = r - thick;
		Vector3f ahead = new Vector3f(s.forward);
		float a0 = (float)Mth.atan2(ahead.z, ahead.x);
		List<Vector3f> body = new ArrayList<>();
		int n = 48;
		for (int i = 0; i <= n; i++) {
			float t = (float)i / n;
			if (t < 1.0F - grow) {
				continue;
			}
			float a = a0 + 0.45F + t * (Mth.TWO_PI - 0.45F);
			float y = thick * 0.85F + 0.12F * Mth.sin(t * 14.0F + time * 0.25F) * (1.0F - t);
			body.add(polar(a, rc + 0.08F * Mth.sin(t * 9.0F + time * 0.2F), y));
		}
		Vector3f neckBase = polar(a0, rc, thick * 0.85F);
		Vector3f head = new Vector3f(polar(a0, rc * 0.45F, 0.0F)).add(0, thick * 0.85F + (3.6F + r * 0.3F) * rear, 0);
		List<Vector3f> neck = new ArrayList<>();
		for (int i = 0; i <= 10; i++) {
			float t = i / 10.0F;
			Vector3f q = new Vector3f(neckBase).lerp(head, t);
			// an S-curve: out over the coil, then up and in over the caster's front
			q.add(new Vector3f(ahead).mul(0.9F * Mth.sin(t * Mth.PI) * rear));
			q.y = Mth.lerp(t * t * (3 - 2 * t), neckBase.y, head.y) + 0.15F * Mth.sin(time * 0.3F + t * 5.0F) * t;
			neck.add(q);
		}
		Vector3f face = new Vector3f(ahead).add(0, -0.45F, 0).normalize();
		float hs = thick * 2.7F;
		float bite = 0.35F + 0.25F * rear + 0.08F * Mth.sin(time * 0.5F);
		int lit = argb(0.97F * fade, SCALE_LIT);
		int dark = argb(0.97F * fade, SCALE_DARK);
		shade(ps, c, o -> {
			for (int i = 0; i < 18; i++) {
				float a = Mth.TWO_PI * hash(s.seed + i * 3);
				float d = rc * (0.25F + 0.65F * hash(i * 7));
				Vector3f at = polar(a, d, 0.25F + 0.5F * hash(i * 5) + 0.3F * ease(age, 0, 30));
				o.sprite(at, s.camRight, s.camUp, 0.8F + 0.5F * hash(i), argb(0.22F * fade * grow, 0x1E4A14));
			}
			if (body.size() >= 2) {
				o.tube(body, thick * 0.3F, thick, 8, lit, dark);
			}
			if (rear > 0.05F) {
				o.tube(neck, thick, thick * 0.8F, 8, lit, dark);
				serpentHead(o, head, face, hs, bite, lit, dark, fade);
			}
		});
		glow(ps, c, o -> {
			// the green markings down its back, the blast's edge and the venom running out to it
			if (body.size() >= 2) {
				List<Vector3f> spine = new ArrayList<>();
				for (Vector3f q : body) {
					spine.add(new Vector3f(q).add(0, thick * 0.8F, 0));
				}
				o.glowPolyline(spine, 0.12F, argb(0.55F * fade, VENOM));
				for (int i = 2; i < body.size() - 1; i += 3) {
					Vector3f q = new Vector3f(body.get(i)).add(0, thick * 0.92F, 0);
					Vector3f along = new Vector3f(body.get(i + 1)).sub(body.get(i - 1));
					o.diamond(q, along, VfxOut.across(along), thick * 0.7F, thick * 0.45F, argb(0.0F, VENOM), argb(0.6F * fade, VENOM_PALE));
				}
			}
			o.ring(new Vector3f(0, 0.14F, 0), Math.max(0.0F, r - 0.35F), r, argb(0.7F * fade * grow, VENOM));
			float wave = ease(age, 0.0F, 7.0F);
			if (wave < 1.0F) {
				o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, r * wave - 0.8F), r * wave, argb(0.85F * (1.0F - wave), VENOM_PALE));
			}
			if (rear > 0.05F) {
				Vector3f side = VfxOut.across(face);
				Vector3f up = new Vector3f(side).cross(face).normalize();
				for (int k = -1; k <= 1; k += 2) {
					Vector3f eye = new Vector3f(head).add(new Vector3f(side).mul(k * hs * 0.55F)).add(new Vector3f(up).mul(hs * 0.28F)).add(new Vector3f(face).mul(hs * 0.25F));
					o.sprite(eye, s.camRight, s.camUp, hs * 0.16F, argb(fade, 0xFFE040));
					o.sprite(eye, s.camRight, s.camUp, hs * 0.4F, argb(0.5F * fade, VENOM));
				}
				// venom dripping off the fangs
				for (int i = 0; i < 8; i++) {
					float q = (time * 0.06F + hash(s.seed + i * 5)) % 1.0F;
					int k = i % 2 == 0 ? 1 : -1;
					Vector3f fang = new Vector3f(head).add(new Vector3f(face).mul(hs * 1.1F)).add(new Vector3f(side).mul(k * hs * 0.22F)).add(new Vector3f(up).mul(-hs * 0.3F));
					o.glint(new Vector3f(fang).add(0, -q * 2.4F, 0), s.camRight, s.camUp, 0.13F, i, argb(0.9F * fade * (1.0F - q), VENOM_PALE));
				}
			}
		});
	}

	/**
	 * A serpent's head {@code size} long round {@code at} facing {@code face}: the skull, jaws open by {@code bite} on a red mouth, four white
	 * fangs and two horns swept back.
	 */
	private static void serpentHead(final VfxOut o, final Vector3f at, final Vector3f face, final float size, final float bite, final int lit, final int dark,
		final float fade) {
		Vector3f f = new Vector3f(face).normalize();
		Vector3f side = VfxOut.across(f);
		Vector3f up = new Vector3f(side).cross(f).normalize();
		Vector3f jaw = new Vector3f(f).mul(size * 0.35F).add(at);
		Vector3f upperDir = new Vector3f(f).mul(Mth.cos(bite)).add(new Vector3f(up).mul(Mth.sin(bite)));
		Vector3f lowerDir = new Vector3f(f).mul(Mth.cos(bite * 1.3F)).sub(new Vector3f(up).mul(Mth.sin(bite * 1.3F)));
		o.ellipsoid(at, new Vector3f(side).mul(size * 0.75F), new Vector3f(up).mul(size * 0.5F), new Vector3f(f).mul(size * 0.85F), 6, 12, lit, dark);
		o.ellipsoid(new Vector3f(upperDir).mul(size * 0.7F).add(jaw), new Vector3f(side).mul(size * 0.55F),
			new Vector3f(VfxOut.across(upperDir)).cross(upperDir).normalize(size * 0.18F), new Vector3f(upperDir).mul(size * 0.72F), 5, 10, lit, dark);
		o.ellipsoid(new Vector3f(lowerDir).mul(size * 0.62F).add(jaw).add(new Vector3f(up).mul(-size * 0.2F)), new Vector3f(side).mul(size * 0.46F),
			new Vector3f(VfxOut.across(lowerDir)).cross(lowerDir).normalize(size * 0.14F), new Vector3f(lowerDir).mul(size * 0.64F), 5, 10, lit, dark);
		int mouth = argb(0.95F * fade, MOUTH);
		Vector3f l0 = new Vector3f(side).mul(-size * 0.35F).add(jaw);
		Vector3f r0 = new Vector3f(side).mul(size * 0.35F).add(jaw);
		o.quad(l0, r0, new Vector3f(side).mul(size * 0.3F).add(new Vector3f(lowerDir).mul(size * 1.1F)).add(jaw),
			new Vector3f(side).mul(-size * 0.3F).add(new Vector3f(lowerDir).mul(size * 1.1F)).add(jaw), mouth);
		o.quad(l0, r0, new Vector3f(side).mul(size * 0.3F).add(new Vector3f(upperDir).mul(size * 1.2F)).add(jaw),
			new Vector3f(side).mul(-size * 0.3F).add(new Vector3f(upperDir).mul(size * 1.2F)).add(jaw), argb(0.95F * fade, 0x5A0A16));
		int fang = argb(0.98F * fade, FANG);
		for (int k = -1; k <= 1; k += 2) {
			Vector3f u0 = new Vector3f(upperDir).mul(size * 1.15F).add(new Vector3f(side).mul(k * size * 0.24F)).add(jaw);
			o.blade(u0, new Vector3f(u0).sub(new Vector3f(up).mul(size * 0.55F)).add(new Vector3f(f).mul(-size * 0.1F)), f, size * 0.08F, fang, fang);
			Vector3f d0 = new Vector3f(lowerDir).mul(size * 0.95F).add(new Vector3f(side).mul(k * size * 0.2F)).add(jaw).add(new Vector3f(up).mul(-size * 0.12F));
			o.blade(d0, new Vector3f(d0).add(new Vector3f(up).mul(size * 0.32F)), f, size * 0.06F, fang, fang);
			Vector3f h0 = new Vector3f(at).add(new Vector3f(side).mul(k * size * 0.4F)).add(new Vector3f(up).mul(size * 0.4F));
			o.blade(h0, new Vector3f(h0).add(new Vector3f(f).mul(-size * 1.1F)).add(new Vector3f(up).mul(size * 0.45F)).add(new Vector3f(side).mul(k * size * 0.2F)), side,
				size * 0.12F, argb(0.97F * fade, 0x14261A), argb(0.97F * fade, 0x50684A));
		}
	}

	/**
	 * Paralytic Bite: the ghost of Kasaka's jaws closes on the target from the caster's side: two great fangs come down from above and two
	 * up from below, the jaws' outline glowing green, and venom bursts where they sink in and drips off.
	 */
	private static void venomBite(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float h = Math.max(1.0F, s.size);
		float w = Math.max(0.6F, s.arg);
		float time = s.ageInTicks;
		float close = ease(age, 0.0F, 4.0F);
		float fade = 1.0F - ease(p, 0.55F, 1.0F);
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		Vector3f mid = new Vector3f(f).mul(-w * 0.6F).add(0, h * 0.55F, 0);
		float gape = h * 0.75F * (1.0F - close) + h * 0.2F;
		float fl = h * 0.55F;
		int fang = argb(0.98F * fade, FANG);
		shade(ps, c, o -> {
			for (int k = -1; k <= 1; k += 2) {
				Vector3f top = new Vector3f(mid).add(new Vector3f(side).mul(k * w * 0.45F)).add(0, gape, 0);
				o.blade(top, new Vector3f(top).add(0, -fl, 0).add(new Vector3f(f).mul(w * 0.35F)), f, fl * 0.16F, fang, fang);
				Vector3f low = new Vector3f(mid).add(new Vector3f(side).mul(k * w * 0.32F)).add(0, -gape, 0);
				o.blade(low, new Vector3f(low).add(0, fl * 0.7F, 0).add(new Vector3f(f).mul(w * 0.3F)), f, fl * 0.12F, fang, fang);
			}
		});
		glow(ps, c, o -> {
			// the jaws' outline: an upper and a lower arch round the fangs
			for (int j = -1; j <= 1; j += 2) {
				Vector3f prev = null;
				for (int i = 0; i <= 12; i++) {
					float t = i / 12.0F;
					float x = (t - 0.5F) * 2.0F;
					Vector3f q = new Vector3f(mid).add(new Vector3f(side).mul(x * w * 0.9F)).add(0, j * (gape + h * 0.15F * (1.0F - x * x)), 0)
						.add(new Vector3f(f).mul(-0.3F * (1.0F - x * x)));
					if (prev != null) {
						o.glowLine(prev, q, 0.1F, argb(0.7F * fade, VENOM), argb(0.7F * fade, VENOM));
					}
					prev = q;
				}
			}
			for (int k = -1; k <= 1; k += 2) {
				Vector3f top = new Vector3f(mid).add(new Vector3f(side).mul(k * w * 0.45F)).add(0, gape, 0);
				o.glowLine(top, new Vector3f(top).add(0, -fl, 0).add(new Vector3f(f).mul(w * 0.35F)), 0.12F, argb(0.5F * fade, VENOM), argb(0.8F * fade, VENOM_PALE));
			}
			if (close >= 1.0F) {
				float burst = 1.0F - ease(age, 4.0F, 10.0F);
				Vector3f bite = new Vector3f(mid).add(new Vector3f(f).mul(w * 0.6F));
				o.star(bite, s.camRight, s.camUp, h * 0.5F * burst + 0.15F, 8, 0.3F, s.seed, argb(0.9F * burst, VENOM_PALE), argb(0.0F, VENOM));
				for (int i = 0; i < 10; i++) {
					float q = (time * 0.05F + hash(s.seed + i)) % 1.0F;
					Vector3f at = new Vector3f(bite).add(new Vector3f(side).mul((hash(i * 3) - 0.5F) * w)).add(0, -q * h * 0.6F, 0);
					o.glint(at, s.camRight, s.camUp, 0.12F, i, argb(0.85F * fade * (1.0F - q), VENOM));
				}
				o.sprite(new Vector3f(bite), s.camRight, s.camUp, w + 0.4F, argb(0.25F * fade, VENOM_DEEP));
			}
		});
	}

	/**
	 * Venom Mist: a low cloud of toxic green mist over the aimed ground as wide as the zone, its edge glowing, bubbles of venom welling up and
	 * bursting in it and drops glinting.
	 */
	private static void venomMist(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = s.size;
		float time = s.ageInTicks;
		float open = ease(age, 0.0F, 8.0F);
		float fade = 1.0F - ease(p, 0.88F, 1.0F);
		float k = open * fade;
		shade(ps, c, o -> {
			o.disc(new Vector3f(0, 0.13F, 0), r * open, argb(0.35F * k, 0x16300C), argb(0.12F * k, 0x16300C));
			for (int i = 0; i < 36; i++) {
				float a = Mth.TWO_PI * hash(s.seed + i * 3) + time * 0.004F * (hash(i) - 0.5F);
				float d = r * open * (float)Math.sqrt(hash(i * 7 + 1)) * 0.88F;
				float y = 0.3F + 0.9F * hash(i * 5) + 0.15F * Mth.sin(time * 0.05F + i);
				o.sprite(polar(a, d, y), s.camRight, s.camUp, 0.9F + 0.7F * hash(i * 11), argb(0.2F * k, i % 3 == 0 ? 0x2E5A16 : 0x1C3A10));
			}
		});
		glow(ps, c, o -> {
			o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, r * open - 0.3F), r * open, argb(0.75F * k, VENOM));
			o.ring(new Vector3f(0, 0.14F, 0), r * open * 0.3F, r * open, argb(0.08F * k, VENOM));
			for (int i = 0; i < 26; i++) {
				float a = Mth.TWO_PI * hash(s.seed * 3 + i * 5);
				float d = r * open * (float)Math.sqrt(hash(i * 13 + 2)) * 0.9F;
				float y = 0.4F + 0.9F * hash(i * 17);
				o.sprite(polar(a, d, y), s.camRight, s.camUp, 0.7F + 0.6F * hash(i), argb(0.08F * k, VENOM));
			}
			// bubbles welling up, swelling and bursting into a ring of drops
			for (int i = 0; i < 12; i++) {
				float q = (time * 0.035F + hash(s.seed + i * 9)) % 1.0F;
				float a = Mth.TWO_PI * hash(i * 23 + (int)(time * 0.035F + hash(s.seed + i * 9)));
				float d = r * 0.85F * (float)Math.sqrt(hash(i * 29 + (int)(time * 0.035F + hash(s.seed + i * 9))));
				Vector3f at = polar(a, d, 0.2F + 0.4F * q);
				if (q < 0.8F) {
					o.circle(at, s.camRight, s.camUp, 0.08F + 0.25F * q, 0.018F, 12, argb(0.8F * k, VENOM_PALE));
				} else {
					float b = (q - 0.8F) / 0.2F;
					for (int j = 0; j < 5; j++) {
						Vector3f d2 = polar(Mth.TWO_PI * j / 5, 0.3F + 0.3F * b, 0.2F * b);
						o.glint(new Vector3f(at).add(d2), s.camRight, s.camUp, 0.08F, j, argb(0.8F * k * (1.0F - b), VENOM_PALE));
					}
				}
			}
		});
	}

	/**
	 * Sprint: green speed lines streaming back along the caster's last strides, a pale afterimage of them every few steps and sparks kicked
	 * up; a green glint at their heels.
	 */
	private static void sprint(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		List<Vector3f> points = VfxOut.path(s.trail);
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float time = s.ageInTicks;
		Vector3f holder = s.trail.isEmpty() ? new Vector3f() : new Vector3f(s.trail.getLast());
		int n = points.size();
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		glow(ps, c, o -> {
			o.ring(new Vector3f(holder).add(0, 0.14F, 0), 0.3F, 0.7F + 0.1F * Mth.sin(time * 0.5F), argb(0.45F * fade, VENOM));
			// the rush of air streaming back off them, running or not
			for (int i = 0; i < 9; i++) {
				float q = (time * 0.12F + hash(s.seed + i * 7)) % 1.0F;
				float y = 0.25F + 1.5F * hash(i * 3);
				if (s.selfView && y > 1.1F) {
					continue;
				}
				Vector3f at = new Vector3f(holder).add(new Vector3f(right).mul((hash(i * 5) - 0.5F) * 0.9F)).add(new Vector3f(ahead).mul(0.3F - 1.6F * q)).add(0, y, 0);
				o.glowLine(at, new Vector3f(at).add(new Vector3f(ahead).mul(-0.7F)), 0.04F, argb(0.7F * fade * Mth.sin(Mth.PI * q), VENOM_PALE), argb(0.0F, VENOM));
			}
			if (n < 2) {
				return;
			}
			int last = s.selfView ? Math.max(1, n - 2) : n - 1;
			for (int k = 0; k < 6; k++) {
				float y = 0.3F + 0.25F * k;
				float off = (hash(s.seed + k) - 0.5F) * 0.7F;
				List<Vector3f> line = new ArrayList<>();
				for (int i = Math.max(0, last - 14); i <= last; i++) {
					Vector3f dir = PendragonLooks.along(points, last, (float)i / Math.max(1, last));
					line.add(new Vector3f(points.get(i)).add(new Vector3f(VfxOut.across(dir)).mul(off)).add(0, y, 0));
				}
				for (int i = 0; i + 1 < line.size(); i++) {
					float t = (float)(i + 1) / line.size();
					o.glowLine(line.get(i), line.get(i + 1), 0.05F, argb(0.6F * fade * t * t, VENOM), argb(0.6F * fade * t * t, VENOM_PALE));
				}
			}
			// afterimages: the outline of a runner, fainter the older
			for (int i = last - 4; i > Math.max(0, last - 16); i -= 4) {
				float t = (float)(i - Math.max(0, last - 16)) / 16.0F;
				Vector3f at = points.get(i);
				float al = 0.3F * fade * t;
				o.sprite(new Vector3f(at).add(0, 1.55F, 0), s.camRight, s.camUp, 0.28F, argb(al, VENOM_PALE));
				o.glowLine(new Vector3f(at).add(0, 0.1F, 0), new Vector3f(at).add(0, 1.35F, 0), 0.3F, argb(al, VENOM), argb(al, VENOM));
			}
			for (int i = 0; i < 8; i++) {
				float q = (time * 0.1F + hash(s.seed * 3 + i)) % 1.0F;
				int idx = Math.max(0, last - (int)(q * 10));
				Vector3f at = new Vector3f(points.get(idx)).add(BladeLooks.shardDir(s.seed, i).mul(0.3F)).add(0, 0.2F + q * 0.6F, 0);
				o.glint(at, s.camRight, s.camUp, 0.1F, i, argb(0.8F * fade * (1.0F - q), VENOM_PALE));
			}
		});
	}

	// ------------------------------------------------------------ the Knight Killer

	/**
	 * Knight's Bane: the hunter's mark (a violet sigil turning over the target's head, a point stabbing down) and, as the caster appears
	 * behind it, a black cross cut through its body with violet light along the edges and shadow smoke curling off.
	 */
	private static void knightsBane(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float h = Math.max(1.0F, s.size);
		float w = Math.max(0.6F, s.arg);
		float time = s.ageInTicks;
		float fade = 1.0F - ease(p, 0.75F, 1.0F);
		float cut = ease(age, 1.0F, 4.0F);
		float cutFade = 1.0F - ease(age, 10.0F, 22.0F);
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		Vector3f mid = new Vector3f(0, h * 0.55F, 0).add(new Vector3f(f).mul(-w * 0.55F));
		float len = h * 0.8F + 0.6F;
		Vector3f[][] cuts = new Vector3f[2][];
		for (int k = 0; k < 2; k++) {
			float sg = k == 0 ? 1.0F : -1.0F;
			Vector3f d = new Vector3f(side).mul(sg * 0.72F).add(0, 0.7F, 0).normalize();
			Vector3f a = new Vector3f(mid).sub(new Vector3f(d).mul(len));
			Vector3f b = new Vector3f(a).lerp(new Vector3f(mid).add(new Vector3f(d).mul(len)), Math.max(0.0F, Math.min(1.0F, cut * 1.6F - k * 0.6F)));
			cuts[k] = new Vector3f[] {a, b};
		}
		shade(ps, c, o -> {
			if (cut > 0.0F && cutFade > 0.0F) {
				for (Vector3f[] cc : cuts) {
					o.blade(cc[0], cc[1], f, 0.16F, argb(0.95F * cutFade, INK), argb(0.95F * cutFade, INK));
				}
				for (int i = 0; i < 8; i++) {
					float q = (time * 0.05F + hash(s.seed + i)) % 1.0F;
					Vector3f at = new Vector3f(mid).add(BladeLooks.shardDir(s.seed, i).mul(0.3F + w * 0.6F * q)).add(0, q * 0.8F, 0);
					o.sprite(at, s.camRight, s.camUp, 0.25F + 0.3F * q, argb(0.4F * cutFade * (1.0F - q), INK));
				}
			}
		});
		glow(ps, c, o -> {
			// the mark over the head
			Vector3f top = new Vector3f(0, h + 0.55F + 0.05F * Mth.sin(time * 0.2F), 0);
			float mr = 0.38F + 0.1F * ease(age, 0.0F, 5.0F);
			o.circle(top, s.camRight, s.camUp, mr, 0.025F, 20, argb(0.85F * fade, SHADOW));
			o.polygram(top, s.camRight, s.camUp, mr * 0.85F, 3, 1, time * 0.06F + Mth.HALF_PI, 0.02F, argb(0.8F * fade, SHADOW_PALE));
			o.rune(top, s.camRight, s.camUp, mr * 0.35F, s.seed, 0.02F, argb(0.9F * fade, SHADOW_PALE));
			o.arrow(new Vector3f(top).add(0, -mr - 0.05F, 0), new Vector3f(top).add(0, -mr - 0.45F, 0), s.camRight, 0.12F, argb(0.8F * fade, SHADOW),
				argb(0.9F * fade, SHADOW_PALE));
			if (cut > 0.0F && cutFade > 0.0F) {
				for (Vector3f[] cc : cuts) {
					o.glowLine(cc[0], cc[1], 0.22F, argb(0.6F * cutFade, SHADOW), argb(0.9F * cutFade, SHADOW_PALE));
					o.glowLine(cc[0], cc[1], 0.05F, argb(0.9F * cutFade, 0xFFFFFF), argb(0.9F * cutFade, 0xFFFFFF));
				}
				float flash = 1.0F - ease(age, 3.0F, 9.0F);
				o.star(mid, s.camRight, s.camUp, h * 0.6F * flash + 0.1F, 4, 0.12F, 0.78F, argb(0.9F * flash, SHADOW_PALE), argb(0.0F, SHADOW));
			}
		});
	}

	/**
	 * Mutilation: one quick cut of the dagger, a thin black crescent with a violet edge sweeping across the slash's arc out to its reach,
	 * leaning one way or the other, a few drops of blood thrown off its end.
	 */
	private static void mutilation(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float r = Math.max(1.0F, s.size);
		float arc = Math.max(20.0F, s.arg) * Mth.DEG_TO_RAD;
		float sweep = ease(age, 0.0F, 2.5F);
		float fade = 1.0F - ease(age, 2.5F, 7.0F);
		boolean back = (s.seed & 1) == 0;
		float tilt = (hash(s.seed) - 0.5F) * 1.1F;
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		Vector3f up = new Vector3f(UP).mul(Mth.cos(tilt)).add(new Vector3f(side).mul(Mth.sin(tilt)));
		Vector3f across = new Vector3f(side).mul(Mth.cos(tilt)).sub(new Vector3f(UP).mul(Mth.sin(tilt)));
		int segs = 18;
		float from = back ? arc / 2 : -arc / 2;
		float to = from + (back ? -arc : arc) * sweep;
		Vector3f[] outer = new Vector3f[segs + 1];
		Vector3f[] inner = new Vector3f[segs + 1];
		for (int i = 0; i <= segs; i++) {
			float t = (float)i / segs;
			float a = Mth.lerp(t, from, to);
			Vector3f dir = new Vector3f(f).mul(Mth.cos(a)).add(new Vector3f(across).mul(Mth.sin(a)));
			// the cut is thickest just behind its leading end
			float thick = 0.12F + r * 0.1F * Mth.sin(Mth.PI * Math.min(1.0F, t * 1.1F));
			outer[i] = new Vector3f(dir).mul(r);
			inner[i] = new Vector3f(dir).mul(r - thick).add(new Vector3f(up).mul(-0.02F));
		}
		shade(ps, c, o -> {
			for (int i = 0; i < segs; i++) {
				float t = (float)i / segs;
				o.quad(inner[i], outer[i], outer[i + 1], inner[i + 1], argb(0.9F * fade * t, INK));
			}
		});
		glow(ps, c, o -> {
			for (int i = 0; i < segs; i++) {
				float t = (float)(i + 1) / segs;
				o.glowLine(outer[i], outer[i + 1], 0.07F, argb(0.85F * fade * t, SHADOW_PALE), argb(0.85F * fade * t, SHADOW_PALE));
				o.glowLine(inner[i], inner[i + 1], 0.05F, argb(0.4F * fade * t, SHADOW), argb(0.4F * fade * t, SHADOW));
			}
			for (int i = 0; i < 4; i++) {
				float q = ease(age, 0.0F, 7.0F);
				Vector3f at = new Vector3f(outer[segs]).mul(0.9F + 0.2F * q).add(BladeLooks.shardDir(s.seed, i).mul(0.5F * q)).add(0, -0.6F * q * q, 0);
				o.glint(at, s.camRight, s.camUp, 0.09F, i, argb(0.9F * fade, BLOOD));
			}
		});
	}

	/**
	 * Killing Intent: a red-black pressure rolls out from the caster in waves to the edge of the debuff, a ring of it left there; dark
	 * tendrils creep over the ground and a huge shadow of the hunter looms behind them with two burning eyes.
	 */
	private static void killingIntent(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = s.size;
		float time = s.ageInTicks;
		float open = ease(age, 0.0F, 10.0F);
		float fade = 1.0F - ease(p, 0.7F, 1.0F);
		float k = open * fade;
		Vector3f ahead = WitchLooks.ahead(s);
		// the hunter's shadow standing behind them, two and a half times their height, rising out of the ground
		float tall = 4.6F;
		Vector3f giant = new Vector3f(ahead).mul(-1.7F).add(0, -tall * (1.0F - open) * 0.6F, 0);
		shade(ps, c, o -> {
			o.disc(new Vector3f(0, 0.13F, 0), r * open, argb(0.4F * k, 0x14040A), argb(0.0F, 0x14040A));
			for (int i = 0; i < 14; i++) {
				float a = Mth.TWO_PI * i / 14 + hash(s.seed + i) * 0.4F;
				List<Vector3f> crack = new ArrayList<>();
				for (int j = 0; j <= 6; j++) {
					float t = j / 6.0F;
					crack.add(polar(a + 0.25F * Mth.sin(t * 6.0F + i), r * open * (0.15F + 0.8F * t), 0.135F));
				}
				o.polyline(crack, 0.05F * (1.0F - 0.5F * open), argb(0.7F * k, 0x080206));
			}
			if (!s.selfView) {
				o.shadowSoldier(giant, ahead, tall, false, argb(0.6F * k, 0x2A1A30), argb(0.6F * k, 0x050208));
			}
		});
		glow(ps, c, o -> {
			o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, r - 0.4F), r, argb(0.7F * k, BLOOD));
			for (int wv = 0; wv < 3; wv++) {
				float q = ((age - wv * 6.0F) / 14.0F);
				if (q > 0.0F && q < 1.0F) {
					float e = 1.0F - (1.0F - q) * (1.0F - q);
					o.ring(new Vector3f(0, 0.16F, 0), Math.max(0.0F, r * e - 0.9F), r * e, argb(0.55F * (1.0F - q) * fade, wv == 1 ? 0x6A0A20 : BLOOD));
				}
			}
			if (!s.selfView) {
				for (int side = -1; side <= 1; side += 2) {
					Vector3f eye = VfxOut.soldierEye(giant, ahead, tall, side).add(new Vector3f(ahead).mul(0.05F));
					o.sprite(eye, s.camRight, s.camUp, 0.14F, argb(k, 0xFFD0D0));
					o.sprite(eye, s.camRight, s.camUp, 0.45F, argb(0.6F * k, BLOOD));
				}
			}
			for (int i = 0; i < 12; i++) {
				float q = (time * 0.04F + hash(s.seed * 3 + i)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 7), 0.5F + 0.6F * hash(i), q * 2.2F);
				o.sprite(at, s.camRight, s.camUp, 0.09F, argb(0.8F * k * Mth.sin(Mth.PI * q), BLOOD));
			}
		});
	}

	/**
	 * Riposte: two daggers crossed in guard before the caster's chest, glinting, a dark violet shield shimmering round them that ripples
	 * now and then.
	 */
	private static void riposte(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float time = s.ageInTicks;
		float open = ease(p, 0.0F, 0.12F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float k = open * fade;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f center = new Vector3f(ahead).mul(s.selfView ? 0.75F : 0.55F).add(0, s.selfView ? 1.15F : 1.2F, 0);
		float len = 0.75F * open;
		Vector3f[][] daggers = new Vector3f[2][];
		for (int i = 0; i < 2; i++) {
			float sg = i == 0 ? 1.0F : -1.0F;
			Vector3f d = new Vector3f(right).mul(sg * 0.7F).add(0, 0.7F, 0).normalize();
			daggers[i] = new Vector3f[] {new Vector3f(center).sub(new Vector3f(d).mul(len * 0.35F)), new Vector3f(center).add(new Vector3f(d).mul(len * 0.65F)), d};
		}
		shade(ps, c, o -> {
			for (Vector3f[] d : daggers) {
				o.blade(d[0], d[1], ahead, 0.07F, argb(0.97F * k, STEEL), argb(0.97F * k, 0xE8ECF2));
				o.bar(new Vector3f(d[0]).sub(new Vector3f(d[2]).mul(0.22F)), d[0], 0.03F, 0.03F, argb(0.97F * k, 0x1A1418), argb(0.97F * k, 0x2A2026));
				Vector3f g = VfxOut.across(d[2]);
				o.bar(new Vector3f(d[0]).add(new Vector3f(g).mul(0.08F)), new Vector3f(d[0]).sub(new Vector3f(g).mul(0.08F)), 0.02F, 0.02F, argb(0.97F * k, BLOOD),
					argb(0.97F * k, BLOOD));
			}
		});
		glow(ps, c, o -> {
			for (Vector3f[] d : daggers) {
				o.glowLine(d[0], d[1], 0.06F, argb(0.5F * k, SHADOW), argb(0.7F * k, SHADOW_PALE));
			}
			float glint = Mth.sin(time * 0.15F);
			if (glint > 0.6F) {
				o.glint(daggers[(int)(time / 20) % 2][1], s.camRight, s.camUp, 0.35F * (glint - 0.6F) / 0.4F + 0.05F, time * 0.1F, argb(0.95F * k, 0xFFFFFF));
			}
			if (!s.selfView) {
				float ripple = (time * 0.05F) % 1.0F;
				o.wall(new Vector3f(), 0.85F, 0.05F, 2.0F, argb(0.12F * k, SHADOW), argb(0.0F, SHADOW));
				o.circle(new Vector3f(0, 0.2F + 1.8F * ripple, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), 0.85F, 0.025F, 24, argb(0.5F * k * (1.0F - ripple), SHADOW_PALE));
			}
			o.ring(new Vector3f(0, 0.14F, 0), 0.6F, 0.95F, argb(0.45F * k, SHADOW));
		});
	}

	private VenomLooks() {
	}
}
