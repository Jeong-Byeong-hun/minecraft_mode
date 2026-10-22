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
 * Looks of Camelot: Gawain's Galatine, the sword of the sun (Excalibur Galatine, the sunlight's blessing, the solar flare, the noon
 * strike), and the Round Table's great shield that Mash and Galahad carry (Lord Camelot, the bulwark of snowflakes, the bash and the
 * charge). Danger areas are drawn as far as the skills hit: the sun's burst and the castle's wall stand on their radius, the arcs
 * reach the cone's range.
 */
final class CamelotLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final Vector3f X = new Vector3f(1, 0, 0);
	private static final Vector3f Z = new Vector3f(0, 0, 1);
	private static final int SUN = 0xFFC040;
	private static final int SUN_CORE = 0xFFF4C8;
	private static final int FIRE = 0xFF6A1A;
	private static final int CASTLE = 0xDCEBFF;
	private static final int CASTLE_EDGE = 0xF4FAFF;
	private static final int SHIELD = 0x9AA8BC;
	private static final int SHIELD_DARK = 0x3B4A6B;
	private static final int FROST = 0xCFF0FF;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case GALATINE_SUN -> galatineSun(s, ps, c);
			case SUNLIGHT -> sunlight(s, ps, c);
			case SOLAR_FLARE -> solarFlare(s, ps, c);
			case NOON_ARC -> noonArc(s, ps, c);
			case CAMELOT -> camelot(s, ps, c);
			case SNOW_BULWARK -> snowBulwark(s, ps, c);
			case ROUND_TABLE -> roundTable(s, ps, c);
			case SHIELD_CHARGE -> shieldCharge(s, ps, c);
			default -> {
			}
		}
	}

	/** A sun of radius {@code r} at {@code at}: a white heart, a golden body and a corona of flame tongues. */
	private static void sun(final VfxOut o, final State s, final Vector3f at, final float r, final float alpha) {
		float time = s.ageInTicks;
		o.sprite(at, s.camRight, s.camUp, r * 2.6F, argb(0.35F * alpha, FIRE));
		o.sprite(at, s.camRight, s.camUp, r * 1.5F, argb(0.8F * alpha, SUN));
		o.sprite(at, s.camRight, s.camUp, r * 0.8F, argb(alpha, SUN_CORE));
		o.star(at, s.camRight, s.camUp, r * 2.2F, 12, 0.5F, time * 0.04F, argb(0.5F * alpha, SUN_CORE), argb(0.0F, SUN));
		for (int i = 0; i < 14; i++) {
			Vector3f d = BladeLooks.shardDir(s.seed, i);
			o.flame(new Vector3f(d).mul(r * 0.8F).add(at), d, r * (0.6F + 0.5F * hash(i + (int)(time / 3))), r * 0.45F, time * 0.7F + i, argb(0.8F * alpha, SUN_CORE),
				argb(0.0F, FIRE));
		}
	}

	// ------------------------------------------------------------ Galatine

	/**
	 * Excalibur Galatine: a sun gathers high over the aimed spot, then is swung down onto it (the strike lands eight ticks after the cast)
	 * and bursts: a dome of solar fire swells to the strike's radius, flames burst out along the ground, a ring stands on the edge of what
	 * it hit and embers rise.
	 */
	private static void galatineSun(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = s.size;
		float time = s.ageInTicks;
		float form = ease(age, 0.0F, 5.0F);
		float fade = 1.0F - ease(p, 0.65F, 1.0F);
		float fall = Mth.clamp((age - 5.0F) / 3.0F, 0.0F, 1.0F);
		Vector3f at = new Vector3f(0, Mth.lerp(fall * fall, 7.5F, 0.6F), 0);
		float hit = age - 8.0F;
		float q = ease(hit, 0.0F, 8.0F);
		if (hit >= 0.0F) {
			shade(ps, c, o -> o.disc(new Vector3f(0, 0.135F, 0), r, argb(0.5F * fade, 0x2A140A), argb(0.0F, 0x2A140A)));
			ps.pushPose();
			c.submitCustomGeometry(ps, VfxDraw.GLOW, (pose, buffer) -> {
				VfxDraw.ball(pose, buffer, Math.max(0.3F, r * q), SUN, 0.16F * (1.0F - ease(hit, 6.0F, 16.0F)));
				VfxDraw.ball(pose, buffer, Math.max(0.2F, r * q * 0.6F), SUN_CORE, 0.2F * (1.0F - ease(hit, 4.0F, 12.0F)));
			});
			ps.popPose();
		}
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			if (hit < 0.0F) {
				o.ring(ground, Math.max(0.0F, r - 0.35F), r, argb(0.8F * form, SUN));
				o.disc(ground, r, argb(0.2F * form, SUN), argb(0.0F, SUN));
				sun(o, s, at, 1.3F * form, form);
				if (fall > 0.0F) {
					o.glowLine(at, new Vector3f(at).add(0, 4.0F, 0), 1.0F, argb(0.6F, FIRE), argb(0.0F, FIRE));
				}
				return;
			}
			o.ring(ground, Math.max(0.0F, r - 0.35F), r, argb(0.85F * fade, SUN));
			o.ring(ground, Math.max(0.0F, r * q - 0.6F), r * q, argb(0.8F * (1.0F - q), SUN_CORE));
			sun(o, s, new Vector3f(0, 0.6F, 0), 1.3F * (1.0F - q * 0.6F), 1.0F - ease(hit, 0.0F, 14.0F));
			// flames bursting out along the ground, no further than the strike reaches
			for (int i = 0; i < 20; i++) {
				float a = Mth.TWO_PI * i / 20 + hash(s.seed + i) * 0.3F;
				Vector3f base = polar(a, r * q * (0.3F + 0.5F * hash(i * 3)), 0.1F);
				Vector3f d = polar(a, 0.5F, 1.0F).normalize();
				o.flame(base, d, (1.2F + 1.6F * hash(i * 5)) * fade, 0.8F, time * 0.8F + i, argb(0.85F * fade, SUN_CORE), argb(0.0F, FIRE));
			}
			for (int i = 0; i < 16; i++) {
				float e = (time * 0.05F + hash(s.seed * 3 + i)) % 1.0F;
				Vector3f pt = polar(Mth.TWO_PI * hash(i * 7), r * (float)Math.sqrt(hash(i * 11)), 0.2F + 3.0F * e);
				o.sprite(pt, s.camRight, s.camUp, 0.08F, argb(0.9F * fade * Mth.sin(e * Mth.PI), 0xFF8A3A));
			}
			float flash = 1.0F - ease(hit, 0.0F, 6.0F);
			o.star(new Vector3f(0, 1.2F, 0), s.camRight, s.camUp, r * flash + 0.3F, 12, 0.3F, s.seed, argb(0.95F * flash, 0xFFFFFF), argb(0.0F, SUN));
		});
	}

	/**
	 * Sunlight Blessing: the noon sun stands blazing over the caster, its rays falling round them onto a sun sigil, and the blessing washes
	 * out as far as it reaches (rings to the allies' radius).
	 */
	private static void sunlight(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.15F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		Vector3f high = new Vector3f(0, 5.2F, 0);
		glow(ps, c, o -> {
			sun(o, s, high, 0.9F * open, k);
			for (int i = 0; i < 10; i++) {
				float a = Mth.TWO_PI * i / 10 + time * 0.01F;
				o.glowLine(new Vector3f(high).add(0, -0.5F, 0), polar(a, 2.4F, 0.15F), 0.3F, argb(0.3F * k, SUN), argb(0.12F * k, SUN));
			}
			// the sun sigil on the ground: a disc with rays round it
			Vector3f ground = new Vector3f(0, 0.14F, 0);
			o.circle(ground, X, Z, 1.6F * open, 0.05F, 32, argb(0.9F * k, SUN));
			o.circle(ground, X, Z, 1.1F * open, 0.03F, 28, argb(0.7F * k, SUN_CORE));
			for (int i = 0; i < 12; i++) {
				float a = Mth.TWO_PI * i / 12 + time * 0.005F;
				o.tri(polar(a - 0.12F, 1.65F * open, 0.14F), polar(a + 0.12F, 1.65F * open, 0.14F), polar(a, 2.5F * open, 0.14F), argb(0.7F * k, SUN), argb(0.7F * k, SUN),
					argb(0.1F * k, SUN));
			}
			for (int w = 0; w < 2; w++) {
				float q = ease(p, 0.1F + 0.25F * w, 0.55F + 0.25F * w);
				if (q > 0.0F && q < 1.0F) {
					o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, s.size * q - 0.6F), s.size * q, argb(0.75F * (1.0F - q), w == 0 ? SUN : SUN_CORE));
				}
			}
			for (int i = 0; i < 16; i++) {
				float q = (time * 0.025F + hash(s.seed + i * 3)) % 1.0F;
				Vector3f pt = polar(Mth.TWO_PI * hash(i * 11), 0.5F + 2.0F * hash(i * 13), 4.5F * (1.0F - q));
				if (s.selfView && pt.y > 1.1F && pt.length() < 1.6F) {
					continue;
				}
				o.glint(pt, s.camRight, s.camUp, 0.17F, time * 0.1F + i, argb(0.9F * k * Mth.sin(q * Mth.PI), i % 2 == 0 ? SUN_CORE : SUN));
			}
		});
	}

	/**
	 * Solar Flare: a shell of solar fire swells round the caster out to the nova's radius, loops of flame arching off its surface and
	 * tongues bursting out, a ring burning on the edge of the blast.
	 */
	private static void solarFlare(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = s.size;
		float time = s.ageInTicks;
		float out = ease(age, 0.0F, 6.0F);
		float fade = 1.0F - ease(p, 0.45F, 1.0F);
		float shell = Math.max(0.3F, r * out);
		Vector3f mid = new Vector3f(0, 0.2F, 0);
		ps.pushPose();
		ps.translate(mid.x, mid.y, mid.z);
		c.submitCustomGeometry(ps, VfxDraw.GLOW, (pose, buffer) -> {
			VfxDraw.ball(pose, buffer, shell, SUN, 0.12F * fade);
			VfxDraw.ball(pose, buffer, shell * 0.55F, SUN_CORE, 0.18F * fade * (1.0F - out * 0.5F));
		});
		ps.popPose();
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			o.ring(ground, Math.max(0.0F, shell - 0.4F), shell, argb(0.9F * fade, SUN));
			o.disc(ground, shell, argb(0.25F * fade, FIRE), argb(0.0F, FIRE));
			// prominences: loops of flame rising off the shell and falling back onto it
			for (int i = 0; i < 6; i++) {
				float a0 = Mth.TWO_PI * hash(s.seed + i * 3);
				float a1 = a0 + 0.5F + 0.4F * hash(i * 5);
				float lat = 0.3F + 0.8F * hash(i * 7);
				Vector3f prev = null;
				for (int j = 0; j <= 10; j++) {
					float t = j / 10.0F;
					float a = Mth.lerp(t, a0, a1);
					float rr = shell * (0.85F + 0.25F * Mth.sin(t * Mth.PI) * out);
					Vector3f pt = new Vector3f(Mth.cos(lat) * Mth.cos(a) * rr, Mth.sin(lat) * rr, Mth.cos(lat) * Mth.sin(a) * rr).add(mid);
					if (prev != null) {
						o.glowLine(prev, pt, 0.25F, argb(0.6F * fade, FIRE), argb(0.6F * fade, FIRE));
						o.bar(prev, pt, 0.05F, 0.05F, argb(0.9F * fade, SUN_CORE), argb(0.9F * fade, SUN_CORE));
					}
					prev = pt;
				}
			}
			for (int i = 0; i < 16; i++) {
				Vector3f d = BladeLooks.shardDir(s.seed * 3, i);
				d.y = Math.abs(d.y);
				o.flame(new Vector3f(d).mul(shell * 0.75F).add(mid), d, shell * 0.3F * fade, 0.5F, time * 0.8F + i, argb(0.8F * fade, SUN_CORE), argb(0.0F, FIRE));
			}
			float flash = 1.0F - ease(age, 0.0F, 5.0F);
			o.star(new Vector3f(0, 1.0F, 0), s.camRight, s.camUp, r * 0.6F * flash + 0.2F, 12, 0.3F, s.seed, argb(0.9F * flash, 0xFFFFFF), argb(0.0F, SUN));
		});
	}

	/** Noon Strike: Galatine sweeps the cone, a band of golden light out to its range, sun-fire rising off the edge and the blade at its head. */
	private static void noonArc(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float age = s.age();
		float half = Math.max(30.0F, s.arg) * Mth.DEG_TO_RAD / 2;
		float r = s.size;
		Vector3f f = new Vector3f(s.forward);
		float swing = ease(age, 0.0F, 4.0F);
		float fade = 1.0F - ease(p, 0.5F, 1.0F);
		float time = s.ageInTicks;
		float now = half - 2 * half * swing;
		int n = 20;
		glow(ps, c, o -> {
			for (int i = 0; i < n; i++) {
				float t0 = half - 2 * half * swing * i / n;
				float t1 = half - 2 * half * swing * (i + 1) / n;
				float y0 = 0.5F - 1.0F * (half - t0) / (2 * half);
				float y1 = 0.5F - 1.0F * (half - t1) / (2 * half);
				Vector3f d0 = new Vector3f(f).rotateY(-t0);
				Vector3f d1 = new Vector3f(f).rotateY(-t1);
				float lead = (float)(i + 1) / n;
				float al = fade * (0.2F + 0.75F * lead);
				Vector3f o0 = new Vector3f(d0).mul(r).add(0, y0, 0);
				Vector3f o1 = new Vector3f(d1).mul(r).add(0, y1, 0);
				o.quad(new Vector3f(d0).mul(r * 0.3F).add(0, y0, 0), o0, o1, new Vector3f(d1).mul(r * 0.3F).add(0, y1, 0), argb(0.0F, SUN), argb(0.8F * al, SUN),
					argb(0.8F * al, SUN), argb(0.0F, SUN));
				o.bar(o0, o1, 0.04F, 0.04F, argb(al, SUN_CORE), argb(al, SUN_CORE));
				if (i % 3 == 0) {
					o.flame(new Vector3f(d0).mul(r * 0.85F).add(0, y0, 0), UP, (0.6F + 0.6F * hash(s.seed + i)) * al, 0.4F, time * 0.8F + i, argb(0.8F * al, SUN_CORE),
						argb(0.0F, FIRE));
				}
			}
			if (swing < 1.0F || p < 0.5F) {
				float y = 0.5F - 1.0F * (half - now) / (2 * half);
				Vector3f d = new Vector3f(f).rotateY(-now);
				Vector3f hilt = new Vector3f(d).mul(0.5F).add(0, y, 0);
				Vector3f tip = new Vector3f(d).mul(r * 0.95F).add(0, y, 0);
				o.blade(hilt, tip, UP, 0.16F, argb(fade, 0xFFFFFF), argb(fade, SUN_CORE));
				o.glowLine(hilt, tip, 0.3F, argb(0.6F * fade, SUN), argb(0.6F * fade, SUN));
				o.star(tip, s.camRight, s.camUp, 0.8F, 8, 0.35F, time * 0.1F, argb(0.9F * fade, SUN_CORE), argb(0.0F, SUN));
			}
		});
	}

	// ------------------------------------------------------------ the Round Table's shield

	/** The great round shield facing {@code n} (its rim in the plane of {@code a} and {@code b}): a steel disc, its rim and the cross on it. */
	private static void shieldBody(final VfxOut o, final Vector3f c, final Vector3f a, final Vector3f b, final float r, final float alpha) {
		o.plate(c, a, b, r, 24, argb(0.92F * alpha, SHIELD));
		o.plate(new Vector3f(a).cross(b).normalize(0.02F).add(c), a, b, r * 0.32F, 16, argb(0.95F * alpha, SHIELD_DARK));
		o.plate(new Vector3f(a).cross(b).normalize(-0.02F).add(c), a, b, r * 0.32F, 16, argb(0.95F * alpha, SHIELD_DARK));
	}

	/** The shield's lit side: a white rim, a cross of light through its boss and a glow behind. */
	private static void shieldLight(final VfxOut o, final State s, final Vector3f c, final Vector3f a, final Vector3f b, final float r, final float alpha) {
		o.circle(c, a, b, r, 0.04F, 28, argb(0.95F * alpha, CASTLE_EDGE));
		o.circle(c, a, b, r * 0.32F, 0.025F, 18, argb(0.9F * alpha, CASTLE_EDGE));
		o.bar(new Vector3f(a).mul(-r * 0.9F).add(c), new Vector3f(a).mul(r * 0.9F).add(c), 0.035F, 0.035F, argb(0.8F * alpha, CASTLE), argb(0.8F * alpha, CASTLE));
		o.bar(new Vector3f(b).mul(-r * 0.9F).add(c), new Vector3f(b).mul(r * 0.9F).add(c), 0.035F, 0.035F, argb(0.8F * alpha, CASTLE), argb(0.8F * alpha, CASTLE));
		o.sprite(c, s.camRight, s.camUp, r * 1.4F, argb(0.2F * alpha, CASTLE));
	}

	/**
	 * Lord Camelot: the shield is planted before the caster and the white castle rises round them on the Round Table's sigil: a curtain
	 * wall of light standing on the nova's radius with battlements along its top, towers with pointed roofs on it, all in pale light.
	 */
	private static void camelot(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float rise = ease(p, 0.0F, 0.2F);
		float fade = 1.0F - ease(p, 0.82F, 1.0F);
		float k = fade;
		float time = s.ageInTicks;
		float R = s.size;
		float wall = 2.6F * rise;
		Vector3f ahead = new Vector3f(s.forward);
		Vector3f right = VfxOut.across(ahead);
		Vector3f shieldAt = new Vector3f(ahead).mul(1.3F).add(0, 1.0F, 0);
		float yaw = (float)Math.atan2(ahead.z, ahead.x);
		if (!s.selfView) {
			shade(ps, c, o -> shieldBody(o, shieldAt, right, UP, 1.0F, k));
		}
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.14F, 0);
			// the Round Table: a ring of thirteen seats round a circle
			o.circle(ground, X, Z, R * 0.45F, 0.05F, 40, argb(0.85F * k, CASTLE_EDGE));
			o.circle(ground, X, Z, R * 0.2F, 0.035F, 24, argb(0.7F * k, CASTLE));
			for (int i = 0; i < 13; i++) {
				float a = Mth.TWO_PI * i / 13 + yaw;
				o.bar(polar(a, R * 0.2F, 0.14F), polar(a, R * 0.45F, 0.14F), 0.03F, 0.03F, argb(0.7F * k, CASTLE), argb(0.7F * k, CASTLE));
			}
			o.disc(ground, R, argb(0.15F * k, CASTLE), argb(0.05F * k, CASTLE));
			// the curtain wall on the edge of what the shield's blast reaches
			o.wall(new Vector3f(), R, 0.0F, wall, argb(0.3F * k, CASTLE), argb(0.12F * k, CASTLE));
			o.circle(new Vector3f(0, wall, 0), X, Z, R, 0.04F, 64, argb(0.9F * k, CASTLE_EDGE));
			o.circle(ground, X, Z, R, 0.05F, 64, argb(0.9F * k, CASTLE_EDGE));
			o.circle(new Vector3f(0, wall * 0.5F, 0), X, Z, R, 0.015F, 64, argb(0.4F * k, CASTLE_EDGE));
			int merlons = Math.max(16, (int)(R * 5));
			for (int i = 0; i < merlons; i++) {
				float a0 = Mth.TWO_PI * i / merlons;
				float a1 = Mth.TWO_PI * (i + 0.5F) / merlons;
				Vector3f b0 = polar(a0, R, wall);
				Vector3f b1 = polar(a1, R, wall);
				Vector3f t0 = polar(a0, R, wall + 0.4F * rise);
				Vector3f t1 = polar(a1, R, wall + 0.4F * rise);
				o.quad(b0, b1, t1, t0, argb(0.25F * k, CASTLE));
				o.bar(b0, t0, 0.02F, 0.02F, argb(0.8F * k, CASTLE_EDGE), argb(0.8F * k, CASTLE_EDGE));
				o.bar(t0, t1, 0.02F, 0.02F, argb(0.8F * k, CASTLE_EDGE), argb(0.8F * k, CASTLE_EDGE));
				o.bar(t1, b1, 0.02F, 0.02F, argb(0.8F * k, CASTLE_EDGE), argb(0.8F * k, CASTLE_EDGE));
			}
			// towers standing on the wall, the gate tower over the caster's front
			for (int t = 0; t < 6; t++) {
				float a = yaw + Mth.TWO_PI * t / 6;
				Vector3f foot = polar(a, R, 0.0F);
				float h = wall + 1.4F * rise;
				o.wall(foot, 0.55F, 0.0F, h, argb(0.25F * k, CASTLE), argb(0.12F * k, CASTLE));
				o.circle(new Vector3f(foot).add(0, h, 0), X, Z, 0.55F, 0.03F, 14, argb(0.9F * k, CASTLE_EDGE));
				Vector3f apex = new Vector3f(foot).add(0, h + 1.1F * rise, 0);
				for (int j = 0; j < 6; j++) {
					o.bar(polar(Mth.TWO_PI * j / 6, 0.6F, h).add(foot), apex, 0.02F, 0.01F, argb(0.85F * k, CASTLE_EDGE), argb(0.85F * k, CASTLE_EDGE));
				}
				o.tri(apex, new Vector3f(apex).add(new Vector3f(right).mul(0.4F)).add(0, -0.12F, 0), new Vector3f(apex).add(0, -0.25F, 0), argb(0.8F * k, 0xFFFFFF),
					argb(0.4F * k, CASTLE), argb(0.8F * k, 0xFFFFFF));
				o.glint(new Vector3f(foot).add(0, h * 0.6F, 0).add(polar(a, 0.56F, 0.0F)), s.camRight, s.camUp, 0.15F, time * 0.1F + t, argb(0.8F * k, 0xFFFFFF));
			}
			if (!s.selfView) {
				shieldLight(o, s, shieldAt, right, UP, 1.0F, k);
			}
			for (int i = 0; i < 18; i++) {
				float q = (time * 0.03F + hash(s.seed + i * 3)) % 1.0F;
				Vector3f pt = polar(Mth.TWO_PI * hash(i * 11), R * (float)Math.sqrt(hash(i * 13)), 0.1F + 3.0F * q);
				if (s.selfView && pt.y > 1.1F && pt.length() < 1.6F) {
					continue;
				}
				o.glint(pt, s.camRight, s.camUp, 0.15F, time * 0.1F + i, argb(0.85F * k * Mth.sin(q * Mth.PI), CASTLE_EDGE));
			}
		});
	}

	/** One snowflake of light: six arms with two pairs of branches each, in the plane of {@code a} and {@code b}. */
	private static void snowflake(final VfxOut o, final Vector3f c, final Vector3f a, final Vector3f b, final float r, final float spin, final int color) {
		for (int i = 0; i < 6; i++) {
			float t = spin + Mth.TWO_PI * i / 6;
			Vector3f d = new Vector3f(a).mul(Mth.cos(t)).add(new Vector3f(b).mul(Mth.sin(t)));
			Vector3f tip = new Vector3f(d).mul(r).add(c);
			o.bar(c, tip, r * 0.06F, r * 0.04F, color, color);
			for (float at : new float[] {0.45F, 0.72F}) {
				Vector3f root = new Vector3f(d).mul(r * at).add(c);
				for (int k = -1; k <= 1; k += 2) {
					float tb = t + k * 0.8F;
					Vector3f db = new Vector3f(a).mul(Mth.cos(tb)).add(new Vector3f(b).mul(Mth.sin(tb)));
					o.bar(root, new Vector3f(db).mul(r * (0.9F - at) * 0.6F).add(root), r * 0.04F, r * 0.03F, color, color);
				}
			}
		}
	}

	/**
	 * Bulwark of Snowflakes: snowflakes of light turn slowly round the caster at shield's reach, snow drifting down, a hexagon of frost at
	 * their feet and the healing washing out to the allies' radius.
	 */
	private static void snowBulwark(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.15F);
		float fade = 1.0F - ease(p, 0.82F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		glow(ps, c, o -> {
			for (int i = 0; i < 12; i++) {
				float a = Mth.TWO_PI * i / 12 + time * 0.02F;
				Vector3f at = polar(a, (1.1F + 0.4F * hash(s.seed + i)) * open, 0.3F + 1.9F * hash(i * 5) + 0.1F * Mth.sin(time * 0.1F + i));
				if (s.selfView && at.y > 1.0F) {
					continue;
				}
				float spin = time * 0.03F * (i % 2 == 0 ? 1 : -1) + i;
				snowflake(o, at, s.camRight, s.camUp, 0.28F, spin, argb(0.95F * k, FROST));
				o.sprite(at, s.camRight, s.camUp, 0.35F, argb(0.25F * k, FROST));
			}
			o.hexagon(new Vector3f(0, 0.14F, 0), UP, 1.7F * open, time * 0.01F, 0.04F, argb(0.9F * k, FROST), argb(0.15F * k, FROST));
			o.hexagon(new Vector3f(0, 0.145F, 0), UP, 1.1F * open, -time * 0.01F, 0.025F, argb(0.7F * k, 0xFFFFFF), argb(0.0F, FROST));
			for (int i = 0; i < 16; i++) {
				float q = (time * 0.02F + hash(s.seed * 3 + i)) % 1.0F;
				Vector3f pt = polar(Mth.TWO_PI * hash(i * 7), 0.4F + 1.4F * hash(i * 9), 3.0F * (1.0F - q));
				if (s.selfView && pt.y > 1.1F) {
					continue;
				}
				o.glint(pt, s.camRight, s.camUp, 0.1F, time * 0.1F + i, argb(0.85F * k * Mth.sin(q * Mth.PI), 0xFFFFFF));
			}
			float q = ease(p, 0.05F, 0.5F);
			if (q < 1.0F) {
				o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, s.size * q - 0.6F), s.size * q, argb(0.7F * (1.0F - q), FROST));
			}
		});
	}

	/** Round Table Bash: the great round shield swept along the cone, its rim reaching the range, a trail of white light behind and a flash. */
	private static void roundTable(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float age = s.age();
		float half = Math.max(30.0F, s.arg) * Mth.DEG_TO_RAD / 2;
		float r = s.size;
		Vector3f f = new Vector3f(s.forward);
		float swing = ease(age, 0.0F, 5.0F);
		float fade = 1.0F - ease(p, 0.55F, 1.0F);
		float now = half - 2 * half * swing;
		float disc = Math.min(1.2F, r * 0.3F);
		float y = 0.1F;
		Vector3f d = new Vector3f(f).rotateY(-now);
		Vector3f center = new Vector3f(d).mul(r - disc).add(0, y, 0);
		Vector3f tangent = new Vector3f(d).cross(UP).normalize();
		if (!s.selfView || swing < 1.0F) {
			shade(ps, c, o -> shieldBody(o, center, tangent, UP, disc, fade));
		}
		glow(ps, c, o -> {
			int n = 20;
			for (int i = 0; i < n; i++) {
				float t0 = half - 2 * half * swing * i / n;
				float t1 = half - 2 * half * swing * (i + 1) / n;
				Vector3f d0 = new Vector3f(f).rotateY(-t0);
				Vector3f d1 = new Vector3f(f).rotateY(-t1);
				float lead = (float)(i + 1) / n;
				float al = fade * (0.15F + 0.7F * lead * lead);
				o.quad(new Vector3f(d0).mul(r * 0.3F).add(0, y, 0), new Vector3f(d0).mul(r).add(0, y, 0), new Vector3f(d1).mul(r).add(0, y, 0),
					new Vector3f(d1).mul(r * 0.3F).add(0, y, 0), argb(0.0F, CASTLE), argb(0.7F * al, CASTLE), argb(0.7F * al, CASTLE), argb(0.0F, CASTLE));
				o.bar(new Vector3f(d0).mul(r).add(0, y, 0), new Vector3f(d1).mul(r).add(0, y, 0), 0.03F, 0.03F, argb(al, CASTLE_EDGE), argb(al, CASTLE_EDGE));
			}
			shieldLight(o, s, center, tangent, UP, disc, fade);
			float flash = ease(age, 3.0F, 5.0F) * (1.0F - ease(age, 5.0F, 9.0F));
			o.star(new Vector3f(d).mul(r).add(0, y, 0), s.camRight, s.camUp, 1.4F * flash + 0.01F, 8, 0.3F, s.seed, argb(0.9F * flash, 0xFFFFFF), argb(0.0F, CASTLE));
		});
	}

	/** Camelot Charge: the shield of light carried at the front of the charge, a wall of pale light left standing along the path and dust kicked up. */
	private static void shieldCharge(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		List<Vector3f> points = VfxOut.path(s.trail);
		if (points.size() < 2) {
			return;
		}
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.35F, 1.0F);
		int n = points.size();
		int last = s.selfView ? Math.max(1, n - 2) : n - 1;
		float time = s.ageInTicks;
		Vector3f head = PendragonLooks.heading(s, points, n - 1);
		Vector3f side = VfxOut.across(head);
		Vector3f front = new Vector3f(points.get(n - 1)).add(new Vector3f(head).mul(0.9F)).add(0, 1.1F, 0);
		shade(ps, c, o -> {
			for (int i = 0; i <= last; i++) {
				Vector3f at = points.get(i);
				float q = (time * 0.05F + hash(s.seed + i)) % 1.0F;
				o.sprite(new Vector3f(at).add(0, 0.3F + 0.4F * q, 0), s.camRight, s.camUp, 0.5F + 0.4F * q, argb(0.3F * fade * (1.0F - q), 0xB8B0A0));
			}
			if (!s.selfView) {
				shieldBody(o, front, side, UP, 0.85F, fade);
			}
		});
		glow(ps, c, o -> {
			// the wall of light: as high and wide as the charge sweeps (1.2 blocks round its line at chest height)
			for (int i = 0; i < last; i++) {
				Vector3f a = points.get(i);
				Vector3f b = points.get(i + 1);
				float t = (float)(i + 1) / Math.max(1, last);
				int bottom = argb(0.35F * fade * t, CASTLE);
				o.quad(a, b, new Vector3f(b).add(0, 2.2F, 0), new Vector3f(a).add(0, 2.2F, 0), bottom, bottom, argb(0.0F, CASTLE), argb(0.0F, CASTLE));
				o.bar(new Vector3f(a).add(0, 0.15F, 0), new Vector3f(b).add(0, 0.15F, 0), 0.04F, 0.04F, argb(0.8F * fade, CASTLE_EDGE), argb(0.8F * fade, CASTLE_EDGE));
			}
			shieldLight(o, s, front, side, UP, 0.85F, fade * (s.selfView ? 0.4F : 1.0F));
			float burst = ease(p, 0.0F, 0.4F);
			o.ring(new Vector3f(0, 0.15F, 0), 0.3F + 1.5F * burst, 0.7F + 1.5F * burst, argb(0.6F * (1.0F - burst), CASTLE));
		});
	}

	private CamelotLooks() {
	}
}
