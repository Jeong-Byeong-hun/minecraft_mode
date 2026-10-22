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
 * Looks of the holy lances: the Lion King's Rhongomyniad (the tower of light at the end of the world, the storm thrust, the charge and the
 * ward) and St. George's Ascalon (Abyssus Draconis's seal and lance, the dragonbane thrust, the martyr's shield and the saint's holy ground).
 */
final class LanceLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final Vector3f X = new Vector3f(1, 0, 0);
	private static final Vector3f Z = new Vector3f(0, 0, 1);
	private static final int LIGHT = 0xFFF6DC;
	private static final int GOLD = 0xFFD24A;
	private static final int STORM = 0xBFEFFF;
	private static final int CLOUD = 0x2A3242;
	private static final int HOLY = 0x9CFFC8;
	private static final int HOLY_PALE = 0xE8FFF2;
	private static final int CROSS_RED = 0xC81E28;
	private static final int SHIELD_WHITE = 0xE8ECF2;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case RHONGOMYNIAD -> rhongomyniad(s, ps, c);
			case STORM_LANCE -> stormLance(s, ps, c);
			case LION_CHARGE -> lionCharge(s, ps, c);
			case LANCE_WARD -> lanceWard(s, ps, c);
			case DRACONIS_SEAL -> draconisSeal(s, ps, c);
			case HOLY_LANCE -> holyLance(s, ps, c, true);
			case DRAGONBANE -> holyLance(s, ps, c, false);
			case MARTYR_SHIELD -> martyrShield(s, ps, c);
			case SAINT_WARD -> saintWard(s, ps, c);
			default -> {
			}
		}
	}

	/**
	 * A lance of light: a cone from {@code tip} running {@code length} back along {@code back} to a base {@code radius} wide, spiral grooves
	 * wound round it (turned by {@code spin}), a vamplate ring at its base and the grip behind, faded by {@code alpha}.
	 */
	static void lance(final VfxOut o, final Vector3f tip, final Vector3f back, final float length, final float radius, final float spin, final int fill,
		final int edge, final float alpha) {
		Vector3f b = new Vector3f(back).normalize();
		Vector3f u = VfxOut.across(b);
		Vector3f v = new Vector3f(b).cross(u).normalize();
		Vector3f base = new Vector3f(b).mul(length).add(tip);
		int sides = 12;
		for (int i = 0; i < sides; i++) {
			float a0 = Mth.TWO_PI * i / sides;
			float a1 = Mth.TWO_PI * (i + 1) / sides;
			Vector3f p0 = new Vector3f(u).mul(Mth.cos(a0) * radius).add(new Vector3f(v).mul(Mth.sin(a0) * radius)).add(base);
			Vector3f p1 = new Vector3f(u).mul(Mth.cos(a1) * radius).add(new Vector3f(v).mul(Mth.sin(a1) * radius)).add(base);
			o.tri(tip, p0, p1, argb(0.6F * alpha, fill), argb(0.25F * alpha, fill), argb(0.25F * alpha, fill));
		}
		for (int k = 0; k < 6; k++) {
			Vector3f prev = new Vector3f(tip);
			for (int j = 1; j <= 10; j++) {
				float t = j / 10.0F;
				float a = spin + Mth.TWO_PI * k / 6 + t * 2.4F;
				Vector3f pt = new Vector3f(u).mul(Mth.cos(a) * radius * t).add(new Vector3f(v).mul(Mth.sin(a) * radius * t)).add(new Vector3f(b).mul(length * t)).add(tip);
				o.bar(prev, pt, 0.012F + 0.03F * t * radius, 0.012F + 0.03F * t * radius, argb(0.9F * alpha, edge), argb(0.9F * alpha, edge));
				prev = pt;
			}
		}
		o.circle(base, u, v, radius, 0.03F + radius * 0.03F, 20, argb(0.95F * alpha, edge));
		o.circle(new Vector3f(b).mul(radius * 0.25F).add(base), u, v, radius * 1.15F, 0.03F + radius * 0.03F, 20, argb(0.8F * alpha, GOLD));
		o.bar(base, new Vector3f(b).mul(length * 0.3F + 0.4F).add(base), radius * 0.16F, radius * 0.12F, argb(0.8F * alpha, edge), argb(0.0F, edge));
		o.glowLine(tip, base, radius * 0.6F + 0.1F, argb(0.6F * alpha, 0xFFFFFF), argb(0.2F * alpha, fill));
	}

	// ------------------------------------------------------------ Rhongomyniad

	/**
	 * Rhongomyniad, the lance that shines at the end of the world: storm clouds wheel together over the aimed spot, lightning in them, and
	 * out of their eye the lance comes down as a tower of light, its grooves turning like a drill and storm wind spiralling round it; where
	 * it stands the ground bursts in rings of light, once for each strike.
	 */
	private static void rhongomyniad(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = s.size;
		float time = s.ageInTicks;
		float appear = ease(age, 0.0F, 6.0F);
		float fade = 1.0F - ease(p, 0.75F, 1.0F);
		float descend = ease(age, 2.0F, 8.0F);
		Vector3f tip = new Vector3f(0, Mth.lerp(descend * descend, 16.0F, 0.05F), 0);
		float rad = 0.32F * r;
		shade(ps, c, o -> {
			for (int i = 0; i < 26; i++) {
				float a = Mth.TWO_PI * hash(s.seed + i) + time * 0.03F * (1.0F + hash(i * 7));
				float rr = 1.5F + r * 1.3F * hash(i * 3);
				Vector3f at = polar(a, rr, 14.0F + 1.6F * hash(i * 5));
				o.sprite(at, s.camRight, s.camUp, 2.0F + 1.6F * hash(i * 11), argb(0.62F * appear * fade, i % 3 == 0 ? 0x3A4256 : CLOUD));
			}
			if (descend >= 1.0F) {
				o.disc(new Vector3f(0, 0.135F, 0), r * 0.9F, argb(0.35F * fade, 0x101820), argb(0.0F, 0x101820));
			}
		});
		glow(ps, c, o -> {
			o.sprite(new Vector3f(0, 14.0F, 0), s.camRight, s.camUp, 3.5F * appear, argb(0.45F * appear * fade, LIGHT));
			// lightning flickering in the clouds
			int tick = (int)(time / 3);
			for (int i = 0; i < 3; i++) {
				if (hash(s.seed * 7 + tick * 3 + i) < 0.45F) {
					continue;
				}
				Vector3f a = polar(Mth.TWO_PI * hash(tick * 5 + i), 2.0F + r * hash(tick * 11 + i), 14.0F + hash(tick + i));
				Vector3f b = polar(Mth.TWO_PI * hash(tick * 13 + i), 2.0F + r * hash(tick * 17 + i), 13.5F + hash(tick * 3 + i));
				o.bolt(a, b, s.seed + tick + i, 0.8F, 6, 0.18F, argb(0.6F * appear * fade, STORM), argb(0.9F * appear * fade, 0xFFFFFF));
			}
			lance(o, tip, UP, 14.0F, rad, time * 0.25F, LIGHT, GOLD, appear * fade);
			o.sprite(tip, s.camRight, s.camUp, 1.6F, argb(0.7F * fade * appear, 0xFFFFFF));
			// the storm wound round the tower
			for (int k = 0; k < 3; k++) {
				Vector3f prev = null;
				for (int j = 0; j <= 22; j++) {
					float t = j / 22.0F;
					float a = t * 7.0F + time * 0.35F + Mth.TWO_PI * k / 3;
					Vector3f pt = polar(a, r * (1.0F - 0.5F * t), 0.3F + 12.0F * t);
					if (prev != null) {
						o.glowLine(prev, pt, 0.09F, argb(0.55F * fade * appear * (1.0F - 0.6F * t), STORM), argb(0.5F * fade * appear, STORM));
					}
					prev = pt;
				}
			}
			if (descend < 1.0F) {
				o.ring(new Vector3f(0, 0.15F, 0), r - 0.35F, r, argb(0.7F * appear, GOLD));
				return;
			}
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			o.disc(ground, r, argb(0.35F * fade, LIGHT), argb(0.0F, GOLD));
			o.ring(ground, r - 0.35F, r, argb(0.7F * fade, GOLD));
			for (int w = 0; w < 3; w++) {
				float q = ease(age - 8.0F - w * 6.0F, 0.0F, 12.0F);
				if (q <= 0.0F || q >= 1.0F) {
					continue;
				}
				o.ring(new Vector3f(0, 0.16F + w * 0.01F, 0), Math.max(0.0F, r * q - 0.6F), r * q, argb(0.85F * (1.0F - q), w == 0 ? 0xFFFFFF : GOLD));
			}
			for (int i = 0; i < 10; i++) {
				float a = Mth.TWO_PI * i / 10 + hash(s.seed + i);
				o.glowLine(new Vector3f(0, 0.16F, 0), polar(a, r * (0.6F + 0.4F * hash(i * 3)), 0.16F), 0.12F, argb(0.8F * fade, GOLD), argb(0.0F, GOLD));
			}
			float flash = 1.0F - ease(age, 8.0F, 14.0F);
			o.star(new Vector3f(0, 1.0F, 0), s.camRight, s.camUp, r * flash + 0.3F, 12, 0.3F, s.seed, argb(0.95F * flash, 0xFFFFFF), argb(0.0F, GOLD));
		});
	}

	/** Storm Thrust: the lance's point of light thrust out along the line, storm wind drilling round the shaft and a burst at its head. */
	private static void stormLance(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float len = Math.max(1.5F, s.size);
		Vector3f dir = PendragonLooks.beamDir(s);
		Vector3f side = VfxOut.across(dir);
		Vector3f up = PendragonLooks.beamUp(dir);
		float thrust = ease(age, 0.0F, 3.0F);
		float fade = 1.0F - ease(p, 0.5F, 1.0F);
		float reach = Math.max(0.8F, len * thrust);
		Vector3f start = new Vector3f(dir).mul(0.5F).add(new Vector3f(side).mul(0.3F));
		Vector3f tip = new Vector3f(dir).mul(reach);
		glow(ps, c, o -> {
			lance(o, tip, new Vector3f(dir).negate(), Math.min(2.4F, reach * 0.5F), 0.42F, time * 0.6F, LIGHT, GOLD, fade);
			o.glowLine(start, tip, 0.5F, argb(0.35F * fade, STORM), argb(0.55F * fade, STORM));
			for (int k = 0; k < 3; k++) {
				Vector3f prev = null;
				int steps = Math.max(6, (int)(reach / 0.35F));
				for (int j = 0; j <= steps; j++) {
					float t = (float)j / steps;
					float d = 0.5F + (reach - 0.5F) * t;
					float a = d * 1.8F - time * 0.9F + Mth.TWO_PI * k / 3;
					float rr = 0.75F - 0.35F * t;
					Vector3f pt = new Vector3f(dir).mul(d).add(new Vector3f(side).mul(Mth.cos(a) * rr)).add(new Vector3f(up).mul(Mth.sin(a) * rr));
					if (prev != null) {
						o.glowLine(prev, pt, 0.07F, argb(0.65F * fade * (0.3F + 0.7F * t), STORM), argb(0.65F * fade * (0.3F + 0.7F * t), STORM));
					}
					prev = pt;
				}
			}
			for (int i = 0; i < 4; i++) {
				float d = ((i / 4.0F + time * 0.1F) % 1.0F) * reach;
				o.circle(new Vector3f(dir).mul(d), side, up, 0.6F + 0.3F * d / len, 0.035F, 18, argb(0.5F * fade, STORM));
			}
			float burst = ease(age, 2.0F, 9.0F);
			if (burst > 0.0F && burst < 1.0F) {
				o.star(tip, s.camRight, s.camUp, 1.2F * (1.0F - burst) + 0.3F, 8, 0.3F, s.seed, argb(0.9F * (1.0F - burst), 0xFFFFFF), argb(0.0F, STORM));
				o.circle(tip, side, up, PendragonLooks.BEAM_R * burst, 0.05F, 24, argb(0.8F * (1.0F - burst), STORM));
			}
		});
	}

	/** Lion King's Charge: the lance levelled at the caster's front in a cone of storm wind, a storm spiralling along the path and crackling. */
	private static void lionCharge(final State s, final PoseStack ps, final SubmitNodeCollector c) {
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
		Vector3f head = PendragonLooks.heading(s, points, n - 1);
		Vector3f end = new Vector3f(points.get(n - 1));
		glow(ps, c, o -> {
			for (int k = 0; k < 2; k++) {
				Vector3f prev = null;
				int steps = Math.max(8, (int)(length / 0.3F));
				for (int j = 0; j <= steps; j++) {
					float t = (float)j / steps;
					Vector3f at = VfxOut.along(points, last, t).add(0, 1.0F, 0);
					Vector3f d = PendragonLooks.along(points, last, t);
					Vector3f side = VfxOut.across(d);
					float a = t * length * 1.6F + time * 0.4F + k * Mth.PI;
					float rr = 0.4F + 0.5F * t;
					Vector3f pt = at.add(new Vector3f(side).mul(Mth.cos(a) * rr)).add(0, Mth.sin(a) * rr, 0);
					if (prev != null) {
						o.glowLine(prev, pt, 0.08F, argb(0.6F * fade * t, STORM), argb(0.6F * fade * t, STORM));
					}
					prev = pt;
				}
			}
			o.glowPolyline(points.subList(0, last + 1).stream().map(v -> new Vector3f(v).add(0, 0.15F, 0)).toList(), 0.3F, argb(0.45F * fade, GOLD));
			// sparks of the storm along the path
			int seed = s.seed * 13 + (int)(time / 2);
			for (int i = 0; i < 4; i++) {
				Vector3f a = VfxOut.along(points, last, hash(seed + i)).add(0, 0.6F + hash(seed * 3 + i), 0);
				o.bolt(a, new Vector3f(a).add(BladeLooks.shardDir(seed, i).mul(0.9F)), seed + i, 0.2F, 4, 0.08F, argb(0.6F * fade, STORM), argb(0.9F * fade, 0xFFFFFF));
			}
			// the lance levelled at the front, in a cone of wind
			float al = (s.selfView ? 0.45F : 1.0F) * fade;
			Vector3f side = VfxOut.across(head);
			Vector3f grip = new Vector3f(end).add(0, 1.15F, 0).add(new Vector3f(side).mul(0.35F)).add(new Vector3f(head).mul(0.3F));
			Vector3f tip = new Vector3f(head).mul(1.6F).add(grip);
			lance(o, tip, new Vector3f(head).negate(), 1.3F, 0.32F, time * 0.5F, LIGHT, GOLD, al);
			Vector3f up = new Vector3f(side).cross(head).normalize();
			for (int i = 0; i < 4; i++) {
				float d = ((i / 4.0F + time * 0.12F) % 1.0F) * 1.5F;
				o.circle(new Vector3f(head).mul(-d).add(tip), side, up, 0.25F + 0.5F * d, 0.03F, 18, argb(0.55F * al * (1.0F - d / 1.7F), STORM));
			}
			o.sprite(tip, s.camRight, s.camUp, 0.7F, argb(0.7F * al, 0xFFFFFF));
		});
	}

	/** Holy Lance Ward: the lance planted upright behind the caster as a tower of light, storm wind turning round it, blessings washing out to allies. */
	private static void lanceWard(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.2F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f foot = new Vector3f(ahead).mul(-0.9F).add(new Vector3f(right).mul(0.55F));
		Vector3f tip = new Vector3f(foot).add(0, 5.0F - 3.0F * (1.0F - open), 0);
		glow(ps, c, o -> {
			lance(o, tip, new Vector3f(0, -1, 0), 3.0F, 0.45F, time * 0.15F, LIGHT, GOLD, k);
			o.bar(new Vector3f(tip).add(0, -3.0F, 0), new Vector3f(foot).add(0, 0.1F, 0), 0.06F, 0.06F, argb(0.9F * k, GOLD), argb(0.4F * k, GOLD));
			o.glowLine(tip, new Vector3f(tip).add(0, 4.0F, 0), 0.6F, argb(0.4F * k, LIGHT), argb(0.0F, LIGHT));
			o.ring(new Vector3f(foot).add(0, 0.14F, 0), 0.2F, 0.8F, argb(0.6F * k, GOLD));
			for (int j = 0; j < 2; j++) {
				Vector3f prev = null;
				for (int i = 0; i <= 18; i++) {
					float t = i / 18.0F;
					float a = t * 6.0F + time * 0.3F + j * Mth.PI;
					Vector3f pt = polar(a, 0.95F - 0.35F * t, 0.4F + 4.6F * t).add(foot);
					if (prev != null) {
						o.glowLine(prev, pt, 0.07F, argb(0.6F * k, STORM), argb(0.6F * k, STORM));
					}
					prev = pt;
				}
			}
			for (int w = 0; w < 2; w++) {
				float q = ease(p, 0.1F + 0.25F * w, 0.55F + 0.25F * w);
				if (q > 0.0F && q < 1.0F) {
					o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, s.size * q - 0.6F), s.size * q, argb(0.75F * (1.0F - q), w == 0 ? GOLD : LIGHT));
				}
			}
			for (int i = 0; i < 14; i++) {
				float q = (time * 0.03F + hash(s.seed + i * 3)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 11), 0.6F + 1.8F * hash(i * 13), 0.1F + 2.6F * q);
				if (s.selfView && at.y > 1.1F && at.length() < 1.6F) {
					continue;
				}
				o.glint(at, s.camRight, s.camUp, 0.18F, time * 0.1F + i, argb(0.9F * k * Mth.sin(q * Mth.PI), i % 2 == 0 ? GOLD : LIGHT));
			}
		});
	}

	// ------------------------------------------------------------ Ascalon

	/**
	 * Abyssus Draconis: the target is named a dragon. A holy seal with St. George's cross opens under it, wings of shadow and horns grow
	 * from it as the dragon is made to show itself, and a shaft of light comes down and pins it to the seal.
	 */
	private static void draconisSeal(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float time = s.ageInTicks;
		float h = Math.max(1.0F, s.size);
		float w = Math.max(0.6F, s.arg);
		float open = ease(p, 0.0F, 0.15F);
		float wings = ease(p, 0.08F, 0.35F);
		float pin = ease(p, 0.3F, 0.4F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		Vector3f f = new Vector3f(s.forward);
		Vector3f right = VfxOut.across(f);
		float span = (h * 0.9F + 0.6F) * wings;
		Vector3f root = new Vector3f(f).mul(w * 0.35F).add(0, h * 0.72F, 0);
		Vector3f[][] fingers = new Vector3f[2][];
		for (int k = 0; k < 2; k++) {
			float sg = k == 0 ? 1.0F : -1.0F;
			float flap = 0.08F * Mth.sin(time * 0.2F);
			Vector3f elbow = new Vector3f(right).mul(sg * span * 0.45F).add(new Vector3f(f).mul(0.25F * span)).add(0, span * (0.35F + flap), 0).add(root);
			fingers[k] = new Vector3f[] {
				root,
				elbow,
				new Vector3f(right).mul(sg * span * 1.05F).add(new Vector3f(f).mul(0.45F * span)).add(0, span * (0.55F + flap), 0).add(root),
				new Vector3f(right).mul(sg * span * 1.15F).add(new Vector3f(f).mul(0.5F * span)).add(0, span * (0.05F + flap), 0).add(root),
				new Vector3f(right).mul(sg * span * 0.85F).add(new Vector3f(f).mul(0.45F * span)).add(0, -span * 0.35F, 0).add(root),
				new Vector3f(right).mul(sg * span * 0.35F).add(new Vector3f(f).mul(0.3F * span)).add(0, -span * 0.45F, 0).add(root)
			};
		}
		float sealR = w * 1.4F + 0.9F;
		shade(ps, c, o -> {
			if (wings <= 0.0F) {
				return;
			}
			int membrane = argb(0.8F * fade * wings, 0x0E1A12);
			for (Vector3f[] wing : fingers) {
				// the membrane fanned between the fingers, scalloped towards the elbow
				for (int i = 2; i + 1 < wing.length; i++) {
					Vector3f mid = new Vector3f(wing[i]).lerp(wing[i + 1], 0.5F).lerp(wing[1], 0.25F);
					o.tri(wing[1], wing[i], mid, membrane, membrane, membrane);
					o.tri(wing[1], mid, wing[i + 1], membrane, membrane, membrane);
				}
				o.tri(wing[0], wing[1], wing[wing.length - 1], membrane, membrane, membrane);
			}
			// horns
			for (int k = -1; k <= 1; k += 2) {
				Vector3f base = new Vector3f(right).mul(k * w * 0.22F).add(0, h * 0.97F, 0);
				Vector3f tip = new Vector3f(right).mul(k * w * 0.55F).add(new Vector3f(f).mul(0.35F)).add(0, h + 0.55F * wings, 0);
				o.blade(base, tip, f, 0.08F, argb(0.9F * fade * wings, 0x1A241C), argb(0.9F * fade * wings, 0x3A4A3A));
			}
		});
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.14F, 0);
			o.magicCircle(ground, X, Z, sealR * open, -time * 0.02F, s.seed, 4, argb(0.85F * fade, HOLY), argb(0.6F * fade, HOLY_PALE));
			o.disc(ground, sealR * open, argb(0.18F * fade, HOLY), argb(0.0F, HOLY));
			if (wings > 0.0F) {
				int bone = argb(0.75F * fade * wings, HOLY);
				for (Vector3f[] wing : fingers) {
					o.bar(wing[0], wing[1], 0.04F, 0.03F, bone, bone);
					for (int i = 2; i < wing.length; i++) {
						o.bar(wing[1], wing[i], 0.025F, 0.01F, bone, argb(0.0F, HOLY));
					}
					// the membrane's scalloped trailing edge lit faintly, so the wings show against the night
					for (int i = 2; i + 1 < wing.length; i++) {
						Vector3f mid = new Vector3f(wing[i]).lerp(wing[i + 1], 0.5F).lerp(wing[1], 0.25F);
						o.bar(wing[i], mid, 0.02F, 0.02F, bone, bone);
						o.bar(mid, wing[i + 1], 0.02F, 0.02F, bone, bone);
						o.tri(wing[1], wing[i], wing[i + 1], argb(0.1F * fade * wings, 0x2A6A48), argb(0.03F * fade * wings, 0x2A6A48), argb(0.03F * fade * wings, 0x2A6A48));
					}
				}
			}
			if (pin > 0.0F) {
				Vector3f top = new Vector3f(0, h + 9.0F, 0);
				Vector3f end = new Vector3f(0, Mth.lerp(pin, h + 9.0F, 0.2F), 0);
				o.glowLine(top, end, 0.7F, argb(0.45F * fade, HOLY), argb(0.6F * fade, HOLY_PALE));
				o.bar(top, end, 0.1F, 0.1F, argb(fade, 0xFFFFFF), argb(fade, 0xFFFFFF));
				float flash = ease(p, 0.38F, 0.42F) * (1.0F - ease(p, 0.42F, 0.6F));
				Vector3f mid = new Vector3f(0, h * 0.55F, 0);
				o.star(mid, s.camRight, s.camUp, 2.2F * flash + 0.01F, 4, 0.12F, 0.0F, argb(0.95F * flash, 0xFFFFFF), argb(0.0F, HOLY));
				o.sprite(mid, s.camRight, s.camUp, 1.0F + h * 0.4F, argb(0.25F * fade, HOLY));
			}
			for (int i = 0; i < 10; i++) {
				float q = (time * 0.04F + hash(s.seed + i)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 7), sealR * (0.4F + 0.6F * hash(i * 3)), 0.1F + 2.0F * q);
				o.glint(at, s.camRight, s.camUp, 0.15F, time * 0.1F + i, argb(0.9F * fade * open * Mth.sin(q * Mth.PI), HOLY_PALE));
			}
		});
	}

	/**
	 * Ascalon's thrust ({@code great}: Abyssus Draconis; else the Dragonbane Thrust): a lance of green-white holy light shoots out along the
	 * line, rings running down it, St. George's cross flaring white and red at its tip. The great one is wider and longer lived; the
	 * thrust sheds dragon scales that shatter off along the line.
	 */
	private static void holyLance(final State s, final PoseStack ps, final SubmitNodeCollector c, final boolean great) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float len = Math.max(1.5F, s.size);
		Vector3f dir = PendragonLooks.beamDir(s);
		Vector3f side = VfxOut.across(dir);
		Vector3f up = PendragonLooks.beamUp(dir);
		float thrust = ease(age, 0.0F, great ? 4.0F : 2.5F);
		float fade = 1.0F - ease(p, great ? 0.6F : 0.5F, 1.0F);
		float reach = Math.max(0.8F, len * thrust);
		Vector3f start = new Vector3f(dir).mul(0.5F).add(new Vector3f(side).mul(0.3F));
		Vector3f tip = new Vector3f(dir).mul(reach);
		float scale = great ? 1.0F : 0.6F;
		float cross = (1.0F - ease(age, great ? 4.0F : 3.0F, great ? 16.0F : 10.0F)) * (thrust >= 0.95F ? 1.0F : 0.0F);
		int scales = great ? 0 : 14;
		Vector3f[] scaleAt = new Vector3f[scales];
		Vector3f[] scaleDir = new Vector3f[scales];
		for (int i = 0; i < scales; i++) {
			float d = 0.8F + (reach - 0.8F) * hash(s.seed + i * 3);
			float q = ease(age, 1.0F + 2.0F * d / len, 12.0F);
			Vector3f out = BladeLooks.shardDir(s.seed, i);
			scaleAt[i] = new Vector3f(dir).mul(d).add(new Vector3f(out).mul(0.3F + 0.7F * q)).add(0, -0.5F * q * q, 0);
			scaleDir[i] = new Vector3f(Mth.cos(time * 0.4F + i), 0.6F, Mth.sin(time * 0.3F + i)).normalize();
		}
		if (cross > 0.0F || scales > 0) {
			shade(ps, c, o -> {
				for (int i = 0; i < scales; i++) {
					o.diamond(scaleAt[i], scaleDir[i], VfxOut.across(scaleDir[i]), 0.32F, 0.22F, argb(0.9F * fade, 0x1E4A32), argb(0.9F * fade, 0x2E6A48));
				}
				if (cross > 0.0F) {
					// St. George's red cross over the white flare
					Vector3f n = new Vector3f(s.camRight).cross(s.camUp).normalize(0.05F);
					Vector3f at = new Vector3f(tip).add(n);
					float r = 1.0F * scale * (0.6F + 0.4F * cross);
					int red = argb(0.95F * cross, CROSS_RED);
					o.quad(new Vector3f(s.camRight).mul(-0.13F * r).add(new Vector3f(s.camUp).mul(r)).add(at), new Vector3f(s.camRight).mul(0.13F * r).add(new Vector3f(s.camUp).mul(r)).add(at),
						new Vector3f(s.camRight).mul(0.13F * r).add(new Vector3f(s.camUp).mul(-r)).add(at), new Vector3f(s.camRight).mul(-0.13F * r).add(new Vector3f(s.camUp).mul(-r)).add(at),
						red);
					o.quad(new Vector3f(s.camUp).mul(-0.13F * r).add(new Vector3f(s.camRight).mul(r)).add(at), new Vector3f(s.camUp).mul(0.13F * r).add(new Vector3f(s.camRight).mul(r)).add(at),
						new Vector3f(s.camUp).mul(0.13F * r).add(new Vector3f(s.camRight).mul(-r)).add(at), new Vector3f(s.camUp).mul(-0.13F * r).add(new Vector3f(s.camRight).mul(-r)).add(at),
						red);
				}
			});
		}
		glow(ps, c, o -> {
			lance(o, tip, new Vector3f(dir).negate(), Math.min(great ? 4.0F : 2.2F, reach * 0.5F), 0.6F * scale, time * 0.3F, HOLY_PALE, HOLY, fade);
			o.glowLine(start, tip, PendragonLooks.BEAM_R * 1.1F, argb(0.25F * fade, HOLY), argb(0.3F * fade, HOLY));
			o.glowLine(start, tip, 0.5F * scale, argb(0.7F * fade, HOLY_PALE), argb(0.7F * fade, HOLY_PALE));
			o.bar(start, tip, 0.07F * scale, 0.05F * scale, argb(fade, 0xFFFFFF), argb(fade, 0xFFFFFF));
			for (int i = 0; i < (great ? 7 : 4); i++) {
				float d = ((i / (great ? 7.0F : 4.0F) + time * 0.05F) % 1.0F) * reach;
				o.circle(new Vector3f(dir).mul(d), side, up, PendragonLooks.BEAM_R, 0.04F, 20, argb(0.55F * fade, HOLY));
			}
			for (int i = 0; i < scales; i++) {
				o.glint(scaleAt[i], s.camRight, s.camUp, 0.14F, time * 0.2F + i, argb(0.8F * fade, HOLY_PALE));
			}
			if (cross > 0.0F) {
				float r = 1.0F * scale * (0.6F + 0.4F * cross);
				o.sprite(tip, s.camRight, s.camUp, r * 1.5F, argb(0.7F * cross, 0xFFFFFF));
				o.star(tip, s.camRight, s.camUp, r * 1.25F, 4, 0.18F, 0.0F, argb(0.9F * cross, 0xFFFFFF), argb(0.0F, HOLY));
			}
		});
	}

	/**
	 * Martyr's Shield: a great kite shield of light rises before the caster, white with St. George's red cross and a gold rim, a halo
	 * over the caster's head and a ring calling every foe near rolling out from their feet. In first person the shield is only an outline.
	 */
	private static void martyrShield(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.15F);
		float fade = 1.0F - ease(p, 0.82F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f center = new Vector3f(ahead).mul(0.95F).add(0, 1.05F + 0.6F * (1.0F - open), 0);
		float[][] outline = {{-0.6F, 0.7F}, {0.6F, 0.7F}, {0.6F, 0.0F}, {0.45F, -0.45F}, {0.0F, -0.9F}, {-0.45F, -0.45F}, {-0.6F, 0.0F}};
		Vector3f[] edge = new Vector3f[outline.length];
		for (int i = 0; i < outline.length; i++) {
			edge[i] = new Vector3f(right).mul(outline[i][0] * open).add(new Vector3f(UP).mul(outline[i][1] * open)).add(center);
		}
		if (!s.selfView) {
			shade(ps, c, o -> {
				// the face laid out in small cells, white or the red of the cross, never one over the other (translucent quads sort by
				// their middles, so a big white face over a red cross would hide half of it)
				int white = argb(0.9F * k, SHIELD_WHITE);
				int red = argb(0.95F * k, CROSS_RED);
				float cell = 0.075F;
				for (float y = -0.9F; y < 0.7F - 1.0E-4F; y += cell) {
					for (float x = -0.6F; x < 0.6F - 1.0E-4F; x += cell) {
						float cx = x + cell / 2;
						float cy = y + cell / 2;
						float limit = cy >= 0.0F ? 0.6F : cy >= -0.45F ? Mth.lerp(-cy / 0.45F, 0.6F, 0.45F) : Mth.lerp((-cy - 0.45F) / 0.45F, 0.45F, 0.0F);
						if (Math.abs(cx) > limit) {
							continue;
						}
						boolean cross = Math.abs(cx) <= 0.1F || cy >= 0.08F && cy <= 0.28F;
						o.quad(at(center, right, x * open, y * open), at(center, right, (x + cell) * open, y * open), at(center, right, (x + cell) * open, (y + cell) * open),
							at(center, right, x * open, (y + cell) * open), cross ? red : white);
					}
				}
			});
		}
		glow(ps, c, o -> {
			float rim = s.selfView ? 0.5F : 1.0F;
			for (int i = 0; i < edge.length; i++) {
				o.bar(edge[i], edge[(i + 1) % edge.length], 0.035F, 0.035F, argb(0.95F * k * rim, GOLD), argb(0.95F * k * rim, GOLD));
			}
			if (!s.selfView) {
				o.sprite(center, s.camRight, s.camUp, 1.6F, argb(0.2F * k, 0xFFFFFF));
				Vector3f halo = new Vector3f(0, 2.25F, 0);
				o.circle(halo, X, Z, 0.42F, 0.04F, 24, argb(0.9F * k, GOLD));
				o.sprite(halo, s.camRight, s.camUp, 0.7F, argb(0.3F * k, LIGHT));
			}
			float q = ease(p, 0.05F, 0.5F);
			if (q < 1.0F) {
				o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, s.size * q - 0.7F), s.size * q, argb(0.75F * (1.0F - q), CROSS_RED));
			}
			o.ring(new Vector3f(0, 0.14F, 0), 0.6F, 1.3F, argb(0.5F * k, GOLD));
			for (int i = 0; i < 8; i++) {
				float t = (time * 0.04F + hash(s.seed + i)) % 1.0F;
				Vector3f pt = polar(Mth.TWO_PI * hash(i * 7), 0.7F, 0.2F + 2.0F * t);
				if (s.selfView && pt.y > 1.0F) {
					continue;
				}
				o.glint(pt, s.camRight, s.camUp, 0.14F, time * 0.1F + i, argb(0.8F * k * Mth.sin(t * Mth.PI), LIGHT));
			}
		});
	}

	/** A point {@code x} across and {@code y} up a shield facing the caster's front round {@code c}. */
	private static Vector3f at(final Vector3f c, final Vector3f right, final float x, final float y) {
		return new Vector3f(right).mul(x).add(0, y, 0).add(c);
	}

	/**
	 * Saint's Ward: holy ground opens round the caster, a circle with St. George's cross across it and roses blooming at the cross's ends,
	 * a wall of light rising on its rim, motes drifting up and the blessing washing out to the allies near.
	 */
	private static void saintWard(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.2F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		float R = Math.min(3.6F, s.size * 0.4F) * open;
		Vector3f ground = new Vector3f(0, 0.14F, 0);
		Vector3f[] ends = {
			new Vector3f(ahead).mul(R).add(ground), new Vector3f(ahead).mul(-R).add(ground), new Vector3f(right).mul(R).add(ground), new Vector3f(right).mul(-R).add(ground)
		};
		float bloom = ease(p, 0.15F, 0.4F);
		shade(ps, c, o -> {
			for (int i = 0; i < ends.length; i++) {
				o.bloom(new Vector3f(ends[i]).add(0, 0.02F, 0), 6, 0.35F, bloom, time * 0.01F + i, argb(0.95F * fade * bloom, 0xB01828), argb(0.95F * fade * bloom, 0xE04050),
					argb(0.95F * fade * bloom, 0xFFD24A));
			}
		});
		glow(ps, c, o -> {
			o.circle(ground, X, Z, R, 0.05F, 48, argb(0.9F * k, HOLY_PALE));
			o.circle(ground, X, Z, R * 0.9F, 0.03F, 48, argb(0.7F * k, HOLY));
			o.disc(ground, R, argb(0.2F * k, HOLY), argb(0.05F * k, HOLY));
			// St. George's cross
			o.bar(ends[0], ends[1], 0.14F, 0.14F, argb(0.85F * k, 0xFF5A5A), argb(0.85F * k, 0xFF5A5A));
			o.bar(ends[2], ends[3], 0.14F, 0.14F, argb(0.85F * k, 0xFF5A5A), argb(0.85F * k, 0xFF5A5A));
			for (Vector3f e : ends) {
				o.sprite(new Vector3f(e).add(0, 0.25F, 0), s.camRight, s.camUp, 0.5F * bloom, argb(0.5F * fade * bloom, 0xFF8A90));
			}
			o.wall(new Vector3f(), R, 0.0F, 2.6F * open, argb(0.28F * k, HOLY), argb(0.0F, HOLY));
			for (int w = 0; w < 2; w++) {
				float q = ease(p, 0.1F + 0.25F * w, 0.55F + 0.25F * w);
				if (q > 0.0F && q < 1.0F) {
					o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, s.size * q - 0.6F), s.size * q, argb(0.7F * (1.0F - q), HOLY_PALE));
				}
			}
			for (int i = 0; i < 20; i++) {
				float q = (time * 0.03F + hash(s.seed + i * 3)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 11), R * (float)Math.sqrt(hash(i * 13)), 0.1F + 3.0F * q);
				if (s.selfView && at.y > 1.1F && at.length() < 1.6F) {
					continue;
				}
				o.glint(at, s.camRight, s.camUp, 0.17F, time * 0.1F + i, argb(0.9F * k * Mth.sin(q * Mth.PI), i % 3 == 0 ? 0xFFFFFF : HOLY_PALE));
			}
		});
	}

	private LanceLooks() {
	}
}
