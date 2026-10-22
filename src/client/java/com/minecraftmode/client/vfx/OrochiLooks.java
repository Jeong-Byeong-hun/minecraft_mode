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
 * Looks of Kusanagi no Tsurugi, the sword Susanoo drew from Yamata no Orochi's tail: Ame-no-Murakumo (the sword of the gathering
 * clouds), the grass cutter that saved Yamato Takeru from the burning field, and the wind step. The eight heads are {@link KnightShots}.
 */
final class OrochiLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final Vector3f X = new Vector3f(1, 0, 0);
	private static final Vector3f Z = new Vector3f(0, 0, 1);
	private static final int WIND = 0xD4FFF2;
	private static final int TEAL = 0x40E0D0;
	private static final int CLOUD = 0x384048;
	private static final int GRASS = 0x5ACB5A;
	private static final int GRASS_TIP = 0xD8FFB0;
	private static final int LEAF = 0x7ADA6A;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case MURAKUMO -> murakumo(s, ps, c);
			case GRASS_CUTTER -> grassCutter(s, ps, c);
			case WIND_STEP -> windStep(s, ps, c);
			default -> {
			}
		}
	}

	/** A leaf tumbling at {@code at}: a small green diamond turned by {@code spin}. */
	private static void leaf(final VfxOut o, final Vector3f at, final float spin, final float size, final float alpha) {
		Vector3f d = new Vector3f(Mth.cos(spin), 0.4F * Mth.sin(spin * 1.3F), Mth.sin(spin)).normalize();
		o.diamond(at, d, VfxOut.across(d), size, size * 0.45F, argb(0.3F * alpha, LEAF), argb(0.9F * alpha, GRASS_TIP));
	}

	/**
	 * Ame-no-Murakumo: dark clouds gather and wheel over the caster, lightning in them; wind streams in from the pull's reach, a whirlwind
	 * stands on the blast's radius and with each of the four cuts a ring of wind blades flies out to that edge, leaves whirling in it.
	 */
	private static void murakumo(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = s.size;
		float pull = r * 8.0F / 6.0F;
		float time = s.ageInTicks;
		float gather = ease(age, 0.0F, 10.0F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		shade(ps, c, o -> {
			for (int i = 0; i < 22; i++) {
				float a = Mth.TWO_PI * hash(s.seed + i) + time * 0.05F * (1.0F + hash(i * 7));
				float rr = (1.5F + r * 1.1F * hash(i * 3)) * (1.4F - 0.5F * gather);
				Vector3f at = polar(a, rr, 6.8F + 1.2F * hash(i * 5));
				o.sprite(at, s.camRight, s.camUp, 1.6F + 1.4F * hash(i * 11), argb(0.6F * gather * fade, i % 3 == 0 ? 0x4A525C : CLOUD));
			}
		});
		glow(ps, c, o -> {
			int tick = (int)(time / 4);
			if (hash(s.seed + tick * 7) > 0.4F) {
				Vector3f a = polar(Mth.TWO_PI * hash(tick * 5), 1.0F + r * hash(tick * 11), 7.0F);
				Vector3f b = polar(Mth.TWO_PI * hash(tick * 13), 1.0F + r * hash(tick * 17), 6.6F);
				o.bolt(a, b, s.seed + tick, 0.6F, 6, 0.15F, argb(0.6F * gather * fade, TEAL), argb(0.9F * gather * fade, 0xFFFFFF));
			}
			// wind drawn in from as far as the pull reaches
			float in = ease(age, 0.0F, 12.0F);
			if (in < 1.0F) {
				for (int i = 0; i < 12; i++) {
					float a = Mth.TWO_PI * i / 12 + hash(s.seed + i);
					float from = Mth.lerp(in, pull, r);
					o.glowLine(polar(a, from, 0.8F + hash(i * 3)), polar(a + 0.35F, from - 1.2F, 0.9F + hash(i * 3)), 0.07F, argb(0.0F, WIND), argb(0.6F * (1.0F - in), WIND));
				}
				o.circle(new Vector3f(0, 0.14F, 0), X, Z, pull, 0.02F, 48, argb(0.35F * (1.0F - in), WIND));
			}
			// the whirlwind standing on the blast's edge
			for (int k = 0; k < 5; k++) {
				Vector3f prev = null;
				for (int j = 0; j <= 16; j++) {
					float t = j / 16.0F;
					float a = t * 3.5F + time * 0.45F + Mth.TWO_PI * k / 5;
					Vector3f pt = polar(a, r * (1.0F - 0.35F * t), 0.2F + 5.0F * t);
					if (prev != null) {
						o.glowLine(prev, pt, 0.1F, argb(0.8F * fade * gather * (1.0F - 0.5F * t), WIND), argb(0.8F * fade * gather * (1.0F - 0.5F * t), WIND));
					}
					prev = pt;
				}
			}
			o.ring(new Vector3f(0, 0.14F, 0), Math.max(0.0F, r - 0.35F), r, argb(0.7F * fade, TEAL));
			// a ring of wind blades for each cut
			for (int b = 0; b < 4; b++) {
				float q = ease(age - b * 6.0F, 0.0F, 7.0F);
				if (age < b * 6.0F || q >= 1.0F) {
					continue;
				}
				for (int i = 0; i < 8; i++) {
					float a0 = Mth.TWO_PI * i / 8 + b * 0.4F;
					float y = 0.6F + 0.8F * hash(b * 13 + i);
					float rr = Math.max(0.4F, r * q);
					Vector3f prev = null;
					for (int j = 0; j <= 5; j++) {
						float t = j / 5.0F;
						Vector3f pt = polar(a0 + t * 0.55F, rr, y);
						if (prev != null) {
							float w = 0.12F * Mth.sin(t * Mth.PI) + 0.02F;
							o.bar(prev, pt, w, w, argb(0.9F * (1.0F - q * 0.7F), 0xFFFFFF), argb(0.9F * (1.0F - q * 0.7F), WIND));
						}
						prev = pt;
					}
				}
			}
			for (int i = 0; i < 18; i++) {
				float t = (time * 0.03F + hash(s.seed * 3 + i)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 7) + time * 0.15F, r * (0.5F + 0.45F * hash(i * 9)), 0.3F + 4.0F * t);
				leaf(o, at, time * 0.3F + i, 0.22F, fade * gather * Mth.sin(t * Mth.PI));
			}
		});
	}

	/**
	 * Grass Cutter: one low sweep of wind almost all the way round, as far as the cut reaches, cut grass flung up out of the swept ground
	 * and tumbling, wind streaks running past its edge.
	 */
	private static void grassCutter(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float age = s.age();
		float half = Math.max(30.0F, s.arg) * Mth.DEG_TO_RAD / 2;
		float r = s.size;
		Vector3f f = new Vector3f(s.forward);
		float swing = ease(age, 0.0F, 4.0F);
		float fade = 1.0F - ease(p, 0.5F, 1.0F);
		float time = s.ageInTicks;
		float y = -0.7F;
		glow(ps, c, o -> {
			int n = 36;
			for (int i = 0; i < n; i++) {
				float t0 = half - 2 * half * swing * i / n;
				float t1 = half - 2 * half * swing * (i + 1) / n;
				Vector3f d0 = new Vector3f(f).rotateY(-t0);
				Vector3f d1 = new Vector3f(f).rotateY(-t1);
				float lead = (float)(i + 1) / n;
				float al = fade * (0.15F + 0.75F * lead * lead);
				o.quad(new Vector3f(d0).mul(r * 0.35F).add(0, y, 0), new Vector3f(d0).mul(r).add(0, y, 0), new Vector3f(d1).mul(r).add(0, y, 0),
					new Vector3f(d1).mul(r * 0.35F).add(0, y, 0), argb(0.0F, TEAL), argb(0.7F * al, WIND), argb(0.7F * al, WIND), argb(0.0F, TEAL));
				o.bar(new Vector3f(d0).mul(r).add(0, y, 0), new Vector3f(d1).mul(r).add(0, y, 0), 0.03F, 0.03F, argb(al, 0xFFFFFF), argb(al, 0xFFFFFF));
				if (i % 4 == 0) {
					// a streak of wind running on past the edge, along the sweep
					Vector3f d2 = new Vector3f(f).rotateY(-t0 + 0.25F);
					o.glowLine(new Vector3f(d0).mul(r * 0.8F).add(0, y + 0.3F, 0), new Vector3f(d2).mul(r * 0.85F).add(0, y + 0.35F, 0), 0.05F, argb(0.5F * al, WIND),
						argb(0.0F, WIND));
				}
			}
			for (int i = 0; i < 30; i++) {
				float t = half - 2 * half * Math.min(swing, hash(s.seed + i));
				if (hash(s.seed + i) > swing) {
					continue;
				}
				float q = ease(age, 1.0F + hash(i * 3) * 3.0F, 12.0F);
				Vector3f d = new Vector3f(f).rotateY(-t);
				float from = r * (0.35F + 0.6F * hash(i * 5));
				Vector3f base = new Vector3f(d).mul(from + 0.8F * q).add(0, -1.1F + 2.2F * q - 1.8F * q * q, 0);
				Vector3f spin = new Vector3f(Mth.cos(age * 0.5F + i), 1.0F, Mth.sin(age * 0.4F + i)).normalize();
				o.blade(base, new Vector3f(spin).mul(0.35F).add(base), VfxOut.across(spin), 0.04F, argb(0.9F * fade, GRASS), argb(0.9F * fade, GRASS_TIP));
			}
		});
	}

	/** Wind Step: streaks of wind along the caster's path at several heights, leaves whirling in their wake and a gust where they set off. */
	private static void windStep(final State s, final PoseStack ps, final SubmitNodeCollector c) {
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
			for (int k = 0; k < 6; k++) {
				float off = (k - 2.5F) * 0.35F;
				float h = 0.3F + 1.6F * hash(s.seed + k * 7);
				Vector3f prev = null;
				int steps = Math.max(6, (int)(length / 0.4F));
				for (int j = 0; j <= steps; j++) {
					float t = (float)j / steps;
					Vector3f side = VfxOut.across(PendragonLooks.along(points, last, t));
					Vector3f pt = VfxOut.along(points, last, t).add(new Vector3f(side).mul(off + 0.15F * Mth.sin(t * 6.0F + k))).add(0, h + 0.1F * Mth.sin(t * 9.0F + k), 0);
					if (prev != null) {
						o.glowLine(prev, pt, 0.06F, argb(0.65F * fade * t, WIND), argb(0.65F * fade * t, WIND));
					}
					prev = pt;
				}
			}
			for (int i = 0; i < 16; i++) {
				float t = hash(s.seed * 3 + i) * 0.8F;
				float q = (time * 0.06F + hash(i * 5)) % 1.0F;
				Vector3f at = VfxOut.along(points, last, t).add(polar(Mth.TWO_PI * hash(i * 7) + time * 0.2F, 0.4F + 0.5F * q, 0.3F + 1.4F * q));
				leaf(o, at, time * 0.4F + i, 0.2F, fade * (1.0F - q));
			}
			float burst = ease(p, 0.0F, 0.4F);
			o.ring(new Vector3f(0, 0.15F, 0), 0.3F + 1.8F * burst, 0.6F + 1.8F * burst, argb(0.55F * (1.0F - burst), WIND));
		});
	}

	private OrochiLooks() {
	}
}
