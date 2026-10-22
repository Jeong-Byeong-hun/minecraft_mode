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
 * Looks of the Shadow Monarch (Solo Leveling): his chain sickle (the Monarch's Domain, Ruler's Authority, the reaper's chain and the abyssal
 * guard) and Kamish's Wrath (Arise, the dragon's fear and the shadow exchange).
 */
final class MonarchLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final int SHADOW = 0x9A5CFF;
	private static final int SHADOW_PALE = 0xDCC8FF;
	private static final int SHADOW_DEEP = 0x4B1F7A;
	private static final int SMOKE = 0x0C0716;
	private static final int SOLDIER_LIT = 0x52427E;
	private static final int SOLDIER_DARK = 0x07040E;
	private static final int STEEL_LIT = 0x5A5A68;
	private static final int STEEL_DARK = 0x101016;
	private static final int DRAGON_LIT = 0x4A1418;
	private static final int DRAGON_DARK = 0x0A0204;
	private static final int DRAGON_FIRE = 0xFF3A2A;
	private static final int DRAGON_PALE = 0xFFB080;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case MONARCH_DOMAIN -> domain(s, ps, c);
			case RULERS_AUTHORITY -> authority(s, ps, c);
			case REAPER_CHAIN -> reaperChain(s, ps, c);
			case ABYSSAL_GUARD -> abyssalGuard(s, ps, c);
			case ARISE -> arise(s, ps, c);
			case DRAGON_FEAR -> dragonFear(s, ps, c);
			case SHADOW_EXCHANGE -> shadowExchange(s, ps, c);
			default -> {
			}
		}
	}

	/** The violet eyes of a {@link VfxOut#shadowSoldier}. */
	private static void soldierEyes(final VfxOut o, final State s, final Vector3f foot, final Vector3f ahead, final float h, final float alpha) {
		for (int k = -1; k <= 1; k += 2) {
			Vector3f eye = VfxOut.soldierEye(foot, ahead, h, k);
			o.sprite(eye, s.camRight, s.camUp, 0.04F * h + 0.02F, argb(alpha, 0xF4ECFF));
			o.sprite(eye, s.camRight, s.camUp, 0.1F * h + 0.05F, argb(0.6F * alpha, SHADOW));
		}
	}

	// ------------------------------------------------------------ the chain sickle

	/**
	 * Monarch's Domain: the Shadow Monarch's domain spreads over the ground as far as it reaches: a dark field with his sigil turning on it,
	 * a ring of violet light and shadow smoke on its edge, and shadowy hands rising out of it, reaching and dragging inwards; the first
	 * blast rolls out to the edge as it opens.
	 */
	private static void domain(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = s.size;
		float time = s.ageInTicks;
		float open = ease(age, 0.0F, 8.0F);
		float fade = 1.0F - ease(p, 0.9F, 1.0F);
		float k = open * fade;
		int hands = 9;
		List<List<Vector3f>> arms = new ArrayList<>();
		for (int i = 0; i < hands; i++) {
			// each hand rises in turn, reaches in towards the middle, sinks and rises again elsewhere
			float cycle = (time * 0.02F + hash(s.seed + i * 7)) % 1.0F;
			int gen = (int)(time * 0.02F + hash(s.seed + i * 7));
			float a = Mth.TWO_PI * hash(i * 13 + gen * 31 + s.seed);
			float d = r * (0.35F + 0.55F * hash(i * 17 + gen * 7));
			float up = Mth.sin(Mth.PI * cycle) * open;
			Vector3f base = polar(a, d, 0.0F);
			Vector3f in = polar(a + Mth.PI, 1.0F, 0.0F);
			arms.add(VfxOut.tentacle(base, UP, in, (1.2F + 0.8F * hash(i * 3)) * up, 0.7F * up, 1.1F, time * 0.2F + i, 7));
		}
		shade(ps, c, o -> {
			o.disc(new Vector3f(0, 0.13F, 0), r * open, argb(0.5F * k, SMOKE), argb(0.25F * k, SMOKE));
			for (int i = 0; i < 24; i++) {
				float a = Mth.TWO_PI * i / 24 + hash(s.seed + i) * 0.2F;
				float q = (time * 0.03F + hash(i * 5)) % 1.0F;
				o.sprite(polar(a, r * open * (0.95F + 0.05F * q), 0.3F + 1.2F * q), s.camRight, s.camUp, 0.6F + 0.5F * q, argb(0.35F * k * (1.0F - q), SMOKE));
			}
			for (List<Vector3f> arm : arms) {
				if (arm.getLast().distance(arm.getFirst()) > 0.15F) {
					o.tube(arm, 0.13F, 0.05F, 6, argb(0.92F * k, SOLDIER_LIT), argb(0.92F * k, SOLDIER_DARK));
				}
			}
		});
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			o.magicCircle(ground, new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), r * 0.92F * open, time * 0.01F, s.seed, 6, argb(0.45F * k, SHADOW),
				argb(0.35F * k, SHADOW_PALE));
			o.ring(new Vector3f(0, 0.16F, 0), Math.max(0.0F, r - 0.35F), r, argb(0.8F * k, SHADOW));
			o.wall(new Vector3f(), r, 0.0F, 1.2F, argb(0.25F * k, SHADOW), argb(0.0F, SHADOW));
			float wave = ease(age, 0.0F, 8.0F);
			if (wave < 1.0F) {
				o.ring(new Vector3f(0, 0.17F, 0), Math.max(0.0F, r * wave - 0.9F), r * wave, argb(0.9F * (1.0F - wave), SHADOW_PALE));
			}
			for (List<Vector3f> arm : arms) {
				if (arm.getLast().distance(arm.getFirst()) > 0.15F) {
					o.glowPolyline(arm, 0.08F, argb(0.3F * k, SHADOW));
					o.sprite(arm.getLast(), s.camRight, s.camUp, 0.18F, argb(0.6F * k, SHADOW_PALE));
				}
			}
			for (int i = 0; i < 16; i++) {
				float q = (time * 0.025F + hash(s.seed * 3 + i)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 19), r * (float)Math.sqrt(hash(i * 23)), q * 2.5F);
				o.sprite(at, s.camRight, s.camUp, 0.08F, argb(0.8F * k * Mth.sin(Mth.PI * q), SHADOW_PALE));
			}
		});
	}

	/**
	 * Ruler's Authority: an unseen hand of violet force takes shape high over the caster while everything round them is lifted (rings of
	 * force rising, motes thrown up), then slams down flat: a shock ring out to the edge of the blast and a ring left on it.
	 */
	private static void authority(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = s.size;
		float time = s.ageInTicks;
		float form = ease(age, 0.0F, 6.0F);
		float drop = ease(age, 9.0F, 11.5F);
		float hit = ease(age, 10.0F, 16.0F);
		float fade = 1.0F - ease(p, 0.75F, 1.0F);
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		float span = r * 0.55F;
		float y = Mth.lerp(drop, 5.5F + r * 0.3F, 0.6F);
		Vector3f palm = new Vector3f(0, y, 0);
		// a hand palm down, fingers spread forward, its outline in light
		List<List<Vector3f>> fingers = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			float a = (i - 1.5F) * 0.32F;
			boolean thumb = i == 4;
			Vector3f dir = thumb ? new Vector3f(right).mul(-0.9F).add(new Vector3f(ahead).mul(0.45F)).normalize()
				: new Vector3f(ahead).mul(Mth.cos(a)).add(new Vector3f(right).mul(Mth.sin(a))).normalize();
			Vector3f root = thumb ? new Vector3f(right).mul(-span * 0.4F).add(palm) : new Vector3f(dir).mul(span * 0.45F).add(palm);
			float len = span * (thumb ? 0.45F : i == 1 || i == 2 ? 0.7F : 0.58F);
			List<Vector3f> pts = new ArrayList<>();
			for (int j = 0; j <= 4; j++) {
				float t = j / 4.0F;
				pts.add(new Vector3f(dir).mul(len * t).add(root).add(0, -0.25F * span * t * t * (1.0F - drop * 0.6F), 0));
			}
			fingers.add(pts);
		}
		shade(ps, c, o -> {
			if (form <= 0.0F || hit >= 1.0F) {
				return;
			}
			float a = 0.55F * form * (1.0F - hit);
			o.ellipsoid(palm, new Vector3f(right).mul(span * 0.48F), new Vector3f(0, span * 0.1F, 0), new Vector3f(ahead).mul(span * 0.5F), 4, 10, argb(a, SHADOW_DEEP),
				argb(a, SMOKE));
			for (List<Vector3f> f : fingers) {
				o.tube(f, span * 0.09F, span * 0.06F, 6, argb(a, SHADOW_DEEP), argb(a, SMOKE));
			}
		});
		glow(ps, c, o -> {
			if (form > 0.0F && hit < 1.0F) {
				float a = 0.95F * form * (1.0F - hit) * fade;
				for (List<Vector3f> f : fingers) {
					o.glowPolyline(f, 0.1F, argb(a, SHADOW_PALE));
					o.glowPolyline(f, 0.35F, argb(0.3F * a, SHADOW));
				}
				o.circle(palm, right, ahead, span * 0.48F, 0.04F, 20, argb(a, SHADOW));
			}
			// the lift: rings of force rising over the area, motes thrown up
			float lift = 1.0F - drop;
			for (int i = 0; i < 3; i++) {
				float q = (age * 0.06F + i / 3.0F) % 1.0F;
				o.circle(new Vector3f(0, 0.2F + 3.0F * q, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), r * (1.0F - 0.25F * q), 0.04F, 40,
					argb(0.4F * lift * form * (1.0F - q) * fade, SHADOW));
			}
			for (int i = 0; i < 14; i++) {
				float q = (time * 0.08F + hash(s.seed + i)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 7), r * (float)Math.sqrt(hash(i * 11)), q * 3.0F * lift);
				o.glint(at, s.camRight, s.camUp, 0.12F, i, argb(0.8F * lift * fade * (1.0F - q), SHADOW_PALE));
			}
			o.ring(new Vector3f(0, 0.14F, 0), Math.max(0.0F, r - 0.35F), r, argb((0.35F + 0.5F * hit) * fade, SHADOW));
			if (drop >= 1.0F) {
				o.ring(new Vector3f(0, 0.16F, 0), Math.max(0.0F, r * hit - 0.9F), r * hit, argb(0.9F * (1.0F - hit), SHADOW_PALE));
				float flash = 1.0F - ease(age, 11.0F, 16.0F);
				o.sprite(new Vector3f(0, 0.5F, 0), s.camRight, s.camUp, r * 0.6F * flash + 0.2F, argb(0.5F * flash, SHADOW_PALE));
				for (int i = 0; i < 10; i++) {
					float a = Mth.TWO_PI * i / 10 + hash(s.seed * 3 + i);
					o.bolt(new Vector3f(0, 0.16F, 0), polar(a, r * 0.9F * hit, 0.16F), s.seed + i, 0.3F, 6, 0.07F, argb(0.5F * fade, SHADOW), argb(0.8F * fade, SHADOW_PALE));
				}
			}
		});
	}

	/**
	 * Reaper's Chain: the sickle flung out to the end of its chain and swung round the whole arc, its blade riding the edge of the slash; a
	 * violet streak left where the blade passed and the chain running back to the caster's hand.
	 */
	private static void reaperChain(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float r = Math.max(1.0F, s.size);
		float arc = Math.max(20.0F, s.arg) * Mth.DEG_TO_RAD;
		float sweep = ease(age, 0.0F, 4.5F);
		float fade = 1.0F - ease(age, 4.5F, 10.0F);
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		float from = -arc / 2;
		float now = from + arc * sweep;
		Vector3f out = new Vector3f(f).mul(Mth.cos(now)).add(new Vector3f(side).mul(Mth.sin(now)));
		Vector3f tang = new Vector3f(f).mul(-Mth.sin(now)).add(new Vector3f(side).mul(Mth.cos(now)));
		Vector3f hand = new Vector3f(out).mul(0.5F).add(0, -0.1F, 0);
		// the sickle: its haft along the chain's end, the blade curving back so its edge lies on the slash's reach
		Vector3f haftEnd = new Vector3f(out).mul(r * 0.72F);
		List<Vector3f> blade = new ArrayList<>();
		for (int i = 0; i <= 8; i++) {
			float t = i / 8.0F;
			float a = t * 1.6F;
			blade.add(new Vector3f(out).mul(r * 0.72F + r * 0.25F * Mth.sin(a)).add(new Vector3f(tang).mul(-r * 0.28F * (1.0F - Mth.cos(a)) * 1.1F)));
		}
		shade(ps, c, o -> {
			o.tube(blade, 0.12F, 0.02F, 5, argb(0.97F * fade, STEEL_LIT), argb(0.97F * fade, STEEL_DARK));
			o.bar(new Vector3f(out).mul(r * 0.55F), haftEnd, 0.06F, 0.06F, argb(0.97F * fade, 0x2A1E3A), argb(0.97F * fade, 0x2A1E3A));
		});
		glow(ps, c, o -> {
			o.chain(hand, new Vector3f(out).mul(r * 0.55F), 0.3F, 0.0F, argb(0.85F * fade, SHADOW_PALE));
			o.glowPolyline(blade, 0.1F, argb(0.9F * fade, SHADOW_PALE));
			o.glowPolyline(blade, 0.3F, argb(0.3F * fade, SHADOW));
			int segs = 26;
			Vector3f prev = null;
			Vector3f prevIn = null;
			for (int i = 0; i <= segs; i++) {
				float t = (float)i / segs;
				float a = Mth.lerp(t, from, now);
				Vector3f d = new Vector3f(f).mul(Mth.cos(a)).add(new Vector3f(side).mul(Mth.sin(a)));
				Vector3f q = new Vector3f(d).mul(r);
				Vector3f qi = new Vector3f(d).mul(r * 0.7F);
				if (prev != null) {
					float al = fade * (0.2F + 0.8F * t);
					o.glowLine(prev, q, 0.1F, argb(0.8F * al, SHADOW_PALE), argb(0.8F * al, SHADOW_PALE));
					o.quad(prevIn, prev, q, qi, argb(0.0F, SHADOW), argb(0.4F * al, SHADOW), argb(0.4F * al, SHADOW), argb(0.0F, SHADOW));
				}
				prev = q;
				prevIn = qi;
			}
		});
	}

	/**
	 * Abyssal Guard: three shadow knights rise out of the ground round the caster (left, right and behind), shields raised, violet eyes
	 * burning, shadow smoke at their feet and a violet ring round their lord.
	 */
	private static void abyssalGuard(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float rise = ease(age, 0.0F, 12.0F);
		float fade = 1.0F - ease(p, 0.85F, 1.0F);
		Vector3f ahead = WitchLooks.ahead(s);
		float a0 = (float)Mth.atan2(ahead.z, ahead.x);
		float h = 2.2F;
		float[] angles = {a0 + Mth.HALF_PI * 1.15F, a0 - Mth.HALF_PI * 1.15F, a0 + Mth.PI};
		Vector3f[] feet = new Vector3f[3];
		Vector3f[] faces = new Vector3f[3];
		for (int i = 0; i < 3; i++) {
			feet[i] = polar(angles[i], 1.7F, -h * (1.0F - rise) * 1.05F);
			faces[i] = polar(angles[i], 1.0F, 0.0F).add(new Vector3f(ahead).mul(0.6F)).normalize();
		}
		shade(ps, c, o -> {
			for (int i = 0; i < 3; i++) {
				o.shadowSoldier(feet[i], faces[i], h, true, argb(0.95F * fade, SOLDIER_LIT), argb(0.95F * fade, SOLDIER_DARK));
				for (int j = 0; j < 4; j++) {
					float q = (time * 0.04F + hash(s.seed + i * 5 + j)) % 1.0F;
					Vector3f at = polar(angles[i] + (hash(j * 7) - 0.5F) * 0.6F, 1.7F + (hash(j * 3) - 0.5F) * 0.6F, 0.2F + q * 1.0F);
					o.sprite(at, s.camRight, s.camUp, 0.35F + 0.3F * q, argb(0.35F * fade * (1.0F - q), SMOKE));
				}
			}
		});
		glow(ps, c, o -> {
			for (int i = 0; i < 3; i++) {
				soldierEyes(o, s, feet[i], faces[i], h, fade * rise);
				o.ring(new Vector3f(polar(angles[i], 1.7F, 0.14F)), 0.2F, 0.6F, argb(0.5F * fade, SHADOW));
			}
			o.ring(new Vector3f(0, 0.14F, 0), 0.5F, 0.85F, argb(0.5F * fade * rise, SHADOW));
			o.wall(new Vector3f(), 0.8F, 0.0F, 1.6F, argb(0.12F * fade * rise, SHADOW), argb(0.0F, SHADOW));
		});
	}

	// ------------------------------------------------------------ Kamish's Wrath

	/**
	 * Arise: purple-black smoke boils up over the blast and its edge flares violet; out of the smoke shadow soldiers rise from the ground one
	 * after another, their eyes lighting as they stand.
	 */
	private static void arise(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = s.size;
		float time = s.ageInTicks;
		float open = ease(age, 0.0F, 6.0F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		int n = 6;
		float a0 = (float)Mth.atan2(s.forward.z, s.forward.x);
		Vector3f[] feet = new Vector3f[n];
		Vector3f[] faces = new Vector3f[n];
		float[] rise = new float[n];
		float[] hs = new float[n];
		for (int i = 0; i < n; i++) {
			float a = a0 + Mth.TWO_PI * (i + 0.5F) / n + (hash(s.seed + i) - 0.5F) * 0.5F;
			float d = r * (0.42F + 0.3F * hash(i * 7));
			hs[i] = 1.9F + 0.6F * hash(i * 11);
			rise[i] = ease(age, 4.0F + i * 2.5F, 14.0F + i * 2.5F);
			feet[i] = polar(a, d, -hs[i] * (1.0F - rise[i]) * 1.05F);
			faces[i] = polar(a, 1.0F, 0.0F);
		}
		shade(ps, c, o -> {
			o.disc(new Vector3f(0, 0.13F, 0), r * open, argb(0.55F * fade, SMOKE), argb(0.2F * fade, SMOKE));
			for (int i = 0; i < 30; i++) {
				float q = (time * 0.03F + hash(s.seed * 3 + i)) % 1.0F;
				float a = Mth.TWO_PI * hash(i * 5);
				float d = r * open * (i < 12 ? 0.95F : (float)Math.sqrt(hash(i * 13)) * 0.85F);
				o.sprite(polar(a, d, 0.2F + 2.2F * q), s.camRight, s.camUp, 0.6F + 0.8F * q, argb(0.38F * fade * (1.0F - q) * open, i % 2 == 0 ? SMOKE : 0x1A0C2A));
			}
			for (int i = 0; i < n; i++) {
				if (rise[i] > 0.0F) {
					o.shadowSoldier(feet[i], faces[i], hs[i], i % 2 == 0, argb(0.95F * fade, SOLDIER_LIT), argb(0.95F * fade, SOLDIER_DARK));
				}
			}
		});
		glow(ps, c, o -> {
			o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, r - 0.35F), r, argb(0.8F * fade * open, SHADOW));
			float wave = ease(age, 0.0F, 7.0F);
			if (wave < 1.0F) {
				o.ring(new Vector3f(0, 0.16F, 0), Math.max(0.0F, r * wave - 0.9F), r * wave, argb(0.9F * (1.0F - wave), SHADOW_PALE));
			}
			o.wall(new Vector3f(), r, 0.0F, 1.5F, argb(0.25F * fade * open, SHADOW), argb(0.0F, SHADOW));
			for (int i = 0; i < n; i++) {
				if (rise[i] > 0.0F) {
					soldierEyes(o, s, feet[i], faces[i], hs[i], fade * ease(rise[i], 0.6F, 1.0F));
					Vector3f g = new Vector3f(feet[i].x, 0.15F, feet[i].z);
					o.ring(g, 0.2F, 0.7F, argb(0.6F * fade * (1.0F - rise[i] * 0.5F), SHADOW));
				}
			}
			float flash = 1.0F - ease(age, 0.0F, 8.0F);
			o.star(new Vector3f(0, 1.2F, 0), s.camRight, s.camUp, 2.0F * flash + 0.2F, 8, 0.2F, s.seed, argb(0.8F * flash, SHADOW_PALE), argb(0.0F, SHADOW));
		});
	}

	/**
	 * Dragon's Fear: Kamish's head, black scales edged in red, rears up behind the caster and roars, fire in its throat and eyes burning,
	 * and rings of dread roll out from the caster to the edge of the fear, a red ring left there.
	 */
	private static void dragonFear(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = s.size;
		float time = s.ageInTicks;
		float rear = ease(age, 0.0F, 7.0F);
		float fade = 1.0F - ease(p, 0.75F, 1.0F);
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f head = new Vector3f(ahead).mul(-1.4F).add(0, 2.8F + 1.6F * rear, 0);
		Vector3f face = new Vector3f(ahead).add(0, -0.15F, 0).normalize();
		Vector3f fup = new Vector3f(VfxOut.across(face)).cross(face).normalize();
		float roar = 0.45F + 0.15F * Mth.sin(time * 0.6F);
		Vector3f upperDir = new Vector3f(face).mul(Mth.cos(roar * 0.5F)).add(new Vector3f(fup).mul(Mth.sin(roar * 0.5F)));
		Vector3f lowerDir = new Vector3f(face).mul(Mth.cos(roar)).sub(new Vector3f(fup).mul(Mth.sin(roar)));
		Vector3f jaw = new Vector3f(face).mul(0.5F).add(head);
		int lit = argb(0.97F * fade * rear, DRAGON_LIT);
		int dark = argb(0.97F * fade * rear, DRAGON_DARK);
		shade(ps, c, o -> {
			if (s.selfView || rear <= 0.05F) {
				return;
			}
			// the neck coming up from behind
			List<Vector3f> neck = new ArrayList<>();
			for (int i = 0; i <= 8; i++) {
				float t = i / 8.0F;
				neck.add(new Vector3f(ahead).mul(-3.2F + 1.6F * t).add(0, 0.3F + (head.y - 0.6F) * t * (2 - t), 0).add(new Vector3f(right).mul(0.3F * Mth.sin(t * 3.0F))));
			}
			o.tube(neck, 0.75F, 0.55F, 8, lit, dark);
			o.ellipsoid(head, new Vector3f(right).mul(0.7F), new Vector3f(fup).mul(0.55F), new Vector3f(face).mul(0.9F), 6, 12, lit, dark);
			o.ellipsoid(new Vector3f(upperDir).mul(0.75F).add(jaw), new Vector3f(right).mul(0.45F), new Vector3f(fup).mul(0.22F), new Vector3f(upperDir).mul(0.8F), 5, 10,
				lit, dark);
			o.ellipsoid(new Vector3f(lowerDir).mul(0.65F).add(jaw).add(new Vector3f(fup).mul(-0.2F)), new Vector3f(right).mul(0.38F), new Vector3f(fup).mul(0.16F),
				new Vector3f(lowerDir).mul(0.7F), 5, 10, lit, dark);
			for (int k = -1; k <= 1; k += 2) {
				// horns swept back and up, a crest of spikes down the neck
				Vector3f h0 = new Vector3f(head).add(new Vector3f(right).mul(k * 0.45F)).add(new Vector3f(fup).mul(0.4F));
				o.blade(h0, new Vector3f(h0).add(new Vector3f(face).mul(-1.4F)).add(new Vector3f(fup).mul(0.8F)).add(new Vector3f(right).mul(k * 0.4F)), right, 0.16F,
					argb(0.97F * fade * rear, 0x2A1A1A), argb(0.97F * fade * rear, 0x7A6A60));
				for (int j = 0; j < 3; j++) {
					Vector3f t0 = new Vector3f(upperDir).mul(0.9F + 0.35F * j).add(new Vector3f(right).mul(k * 0.3F)).add(jaw);
					o.blade(t0, new Vector3f(t0).sub(new Vector3f(fup).mul(0.22F)), face, 0.04F, argb(0.97F * fade * rear, 0xF0E8D8), argb(0.97F * fade * rear, 0xFFFFFF));
				}
			}
			for (int i = 1; i < neck.size() - 1; i++) {
				Vector3f at = new Vector3f(neck.get(i)).add(0, 0.6F, 0);
				o.blade(at, new Vector3f(at).add(0, 0.5F, 0).add(new Vector3f(ahead).mul(-0.2F)), ahead, 0.12F, lit, dark);
			}
		});
		glow(ps, c, o -> {
			if (!s.selfView && rear > 0.05F) {
				for (int k = -1; k <= 1; k += 2) {
					Vector3f eye = new Vector3f(head).add(new Vector3f(right).mul(k * 0.45F)).add(new Vector3f(fup).mul(0.25F)).add(new Vector3f(face).mul(0.45F));
					o.sprite(eye, s.camRight, s.camUp, 0.13F, argb(fade * rear, 0xFFE8A0));
					o.sprite(eye, s.camRight, s.camUp, 0.4F, argb(0.7F * fade * rear, DRAGON_FIRE));
				}
				Vector3f throat = new Vector3f(face).mul(0.9F).add(jaw);
				o.sprite(throat, s.camRight, s.camUp, 0.9F, argb(0.6F * fade * rear, DRAGON_FIRE));
				for (int i = 0; i < 6; i++) {
					o.flame(new Vector3f(throat).add(new Vector3f(right).mul((i - 2.5F) * 0.12F)), new Vector3f(face).add(0, 0.3F, 0).normalize(), 0.9F + 0.5F * hash(i), 0.4F,
						time * 0.9F + i, argb(0.7F * fade * rear, DRAGON_PALE), argb(0.0F, DRAGON_FIRE));
				}
			}
			o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, r - 0.4F), r, argb(0.75F * fade, DRAGON_FIRE));
			for (int wv = 0; wv < 3; wv++) {
				float q = (age - 3.0F - wv * 5.0F) / 12.0F;
				if (q > 0.0F && q < 1.0F) {
					float e = 1.0F - (1.0F - q) * (1.0F - q);
					o.ring(new Vector3f(0, 0.16F, 0), Math.max(0.0F, r * e - 1.0F), r * e, argb(0.6F * (1.0F - q) * fade, wv == 1 ? 0x7A0A14 : DRAGON_FIRE));
					o.wall(new Vector3f(), r * e, 0.0F, 1.4F * (1.0F - q), argb(0.18F * (1.0F - q) * fade, DRAGON_FIRE), argb(0.0F, DRAGON_FIRE));
				}
			}
		});
	}

	/**
	 * Shadow Exchange: where the caster stood a pool of shadow opens and a shadow soldier is left in their place, sinking into it; a dark line
	 * runs over the ground to where they rise out of a second pool in a column of shadow smoke.
	 */
	private static void shadowExchange(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float fade = 1.0F - ease(p, 0.6F, 1.0F);
		float open = ease(age, 0.0F, 4.0F);
		Vector3f end = ShinobiLooks.arrival(s);
		float sink = ease(age, 2.0F, 18.0F);
		float h = 1.9F;
		Vector3f face = new Vector3f(s.forward);
		Vector3f soldierFoot = new Vector3f(0, -h * sink * 1.05F, 0);
		shade(ps, c, o -> {
			o.disc(new Vector3f(0, 0.13F, 0), 1.1F * open, argb(0.85F * fade, SMOKE), argb(0.3F * fade, SMOKE));
			o.disc(new Vector3f(end).add(0, 0.13F, 0), 1.1F * open, argb(0.85F * fade, SMOKE), argb(0.3F * fade, SMOKE));
			if (sink < 1.0F) {
				o.shadowSoldier(soldierFoot, face, h, false, argb(0.95F * fade, SOLDIER_LIT), argb(0.95F * fade, SOLDIER_DARK));
			}
			Vector3f line = new Vector3f(end);
			line.y = 0;
			if (line.lengthSquared() > 0.5F) {
				o.bar(new Vector3f(0, 0.14F, 0), new Vector3f(end).add(0, 0.14F, 0), 0.12F, 0.12F, argb(0.6F * fade, SMOKE), argb(0.6F * fade, SMOKE));
			}
			for (int i = 0; i < 10; i++) {
				float q = (time * 0.05F + hash(s.seed + i)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 7), 0.5F * hash(i * 3), 0.2F + q * 2.4F).add(end);
				o.sprite(at, s.camRight, s.camUp, 0.4F + 0.4F * q, argb(0.4F * fade * (1.0F - q), SMOKE));
			}
		});
		glow(ps, c, o -> {
			o.ring(new Vector3f(0, 0.15F, 0), 0.8F * open, 1.15F * open, argb(0.6F * fade, SHADOW));
			o.ring(new Vector3f(end).add(0, 0.15F, 0), 0.8F * open, 1.15F * open, argb(0.6F * fade, SHADOW));
			if (sink < 1.0F) {
				soldierEyes(o, s, soldierFoot, face, h, fade * (1.0F - sink));
			}
			Vector3f flat = new Vector3f(end);
			flat.y = 0;
			if (flat.lengthSquared() > 0.5F) {
				o.glowLine(new Vector3f(0, 0.16F, 0), new Vector3f(end).add(0, 0.16F, 0), 0.25F, argb(0.35F * fade, SHADOW), argb(0.35F * fade, SHADOW));
			}
			float rise = 1.0F - ease(age, 0.0F, 10.0F);
			o.wall(new Vector3f(end), 0.7F, 0.0F, 2.4F, argb(0.35F * rise * fade, SHADOW), argb(0.0F, SHADOW));
		});
	}

	private MonarchLooks() {
	}
}
