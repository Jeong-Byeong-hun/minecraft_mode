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
import org.joml.Vector4f;

/**
 * Looks of the great casters of destruction and of kings: Megumin (EXPLOSION!, Drain Touch, the Crimson Demon pose, manatite) and Solomon
 * (Ars Almadel Salomonis, the demon pillars, his clairvoyance and his ten rings).
 */
final class ArcaneLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final Vector3f X = new Vector3f(1, 0, 0);
	private static final Vector3f Z = new Vector3f(0, 0, 1);
	private static final int CRIMSON = 0xFF2A3A;
	private static final int CRIMSON_PALE = 0xFFB0A0;
	private static final int BLAST_CORE = 0xFFF4D0;
	private static final int BLAST = 0xFF8A2A;
	private static final int DRAIN = 0x6AFF7A;
	private static final int MANA = 0x4AA8FF;
	private static final int MANA_PALE = 0xBFE8FF;
	private static final int GOLD = 0xFFD24A;
	private static final int GOLD_PALE = 0xFFF0B0;
	private static final int DEMON = 0xFF3A2A;
	private static final int[] GEMS = {0xFF2A4A, 0x3A6AFF, 0x2AE07A, 0xFFC02A, 0xC04AFF};

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case EXPLOSION -> explosion(s, ps, c);
			case DRAIN_TOUCH -> drainTouch(s, ps, c);
			case CRIMSON_POSE -> crimsonPose(s, ps, c);
			case MANATITE -> manatite(s, ps, c);
			case ARS_ALMADEL -> arsAlmadel(s, ps, c);
			case DEMON_PILLARS -> demonPillars(s, ps, c);
			case CLAIRVOYANCE -> clairvoyance(s, ps, c);
			case TEN_RINGS -> tenRings(s, ps, c);
			default -> {
			}
		}
	}

	/** A glowing ball of {@code radius} at {@code at}, squashed to {@code flat} of its height. */
	private static void ball(final PoseStack ps, final SubmitNodeCollector c, final Vector3f at, final float radius, final float flat, final int rgb,
		final float alpha) {
		if (radius <= 0.01F || alpha <= 0.01F) {
			return;
		}
		ps.pushPose();
		ps.translate(at.x, at.y, at.z);
		ps.scale(1.0F, flat, 1.0F);
		c.submitCustomGeometry(ps, VfxDraw.GLOW, (pose, buffer) -> VfxDraw.ball(pose, buffer, radius, rgb, alpha));
		ps.popPose();
	}

	// ------------------------------------------------------------ Megumin

	/**
	 * EXPLOSION!: crimson magic circles stack up over the aimed spot one above the other while the chant runs, a thin line of light drops
	 * through them all, and the blast: a white-hot fireball swelling out, a column of fire climbing into a mushroom cloud, a shock ring
	 * racing over the ground and smoke rolling out of it all.
	 */
	private static void explosion(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float time = s.ageInTicks;
		float r = s.size;
		// the strike lands 8 ticks after the cast
		float hit = 8.0F;
		int layers = 6;
		float[] height = {0.15F, 3.0F, 5.6F, 8.2F, 10.8F, 13.4F};
		float[] radius = {1.0F, 0.84F, 0.7F, 0.58F, 0.48F, 0.38F};
		float circles = 1.0F - ease(age, 12.0F, 24.0F);
		float ball = ease(age, hit, hit + 7.0F);
		float burn = 1.0F - ease(age, hit + 10.0F, s.life);
		float column = ease(age, hit + 2.0F, hit + 18.0F);
		float cap = ease(age, hit + 6.0F, hit + 24.0F);
		float shock = ease(age, hit, hit + 12.0F);
		float smoke = ease(age, hit + 5.0F, hit + 25.0F) * (1.0F - ease(s.progress, 0.85F, 1.0F));
		float top = r * 2.4F * column + 1.0F;
		shade(ps, c, o -> {
			if (age >= hit) {
				o.disc(new Vector3f(0, 0.13F, 0), r * 1.15F, argb(0.6F * ease(age, hit, hit + 6), 0x1A0805), argb(0.1F, 0x1A0805));
			}
			if (smoke > 0.0F) {
				// smoke rolling out of the cap and off the ground
				for (int i = 0; i < 14; i++) {
					float a = Mth.TWO_PI * i / 14 + hash(s.seed + i);
					boolean high = i % 2 == 0;
					float rr = high ? r * 0.9F * cap + 1.0F : r * (0.7F + 0.6F * smoke);
					Vector3f at = polar(a, rr, high ? top + 0.5F * smoke * 3.0F : 0.8F + smoke * 1.5F);
					o.sprite(at, s.camRight, s.camUp, (high ? 2.2F : 1.8F) * (0.6F + 0.6F * smoke), argb(0.55F * smoke, i % 3 == 0 ? 0x3A2A22 : 0x22160F));
				}
				o.bar(new Vector3f(0, 0.2F, 0), new Vector3f(0, top, 0), r * 0.2F, r * 0.22F, argb(0.35F * smoke, 0x2A1A14), argb(0.25F * smoke, 0x2A1A14));
			}
		});
		glow(ps, c, o -> {
			// the chant: circles stacking up one after another
			if (circles > 0.0F) {
				for (int i = 0; i < layers; i++) {
					float opened = ease(age - i * 1.3F, 0.0F, 3.0F);
					if (opened <= 0.0F) {
						continue;
					}
					Vector3f at = new Vector3f(0, height[i], 0);
					float rr = r * radius[i] * opened;
					o.magicCircle(at, X, Z, rr, time * (i % 2 == 0 ? 0.05F : -0.07F), s.seed + i, i % 2 == 0 ? 6 : 5, argb(0.95F * circles, CRIMSON),
						argb(0.85F * circles, CRIMSON_PALE));
					o.disc(at, rr * 0.4F, argb(0.3F * circles, CRIMSON), argb(0.0F, CRIMSON));
				}
				// the line of light dropping through them
				float drop = ease(age, 5.0F, hit) * (1.0F - ease(age, hit + 2.0F, hit + 6.0F));
				if (drop > 0.0F) {
					o.glowLine(new Vector3f(0, height[layers - 1], 0), new Vector3f(0, height[layers - 1] * (1.0F - ease(age, 5.0F, hit)), 0), 0.6F,
						argb(0.9F * drop, 0xFFFFFF), argb(0.9F * drop, CRIMSON_PALE));
				}
			}
			if (age < hit) {
				return;
			}
			float flash = 1.0F - ease(age, hit, hit + 5.0F);
			o.sprite(new Vector3f(0, 1.5F, 0), s.camRight, s.camUp, r * 2.2F * flash + 0.5F, argb(0.9F * flash, 0xFFFFFF));
			o.star(new Vector3f(0, 1.5F, 0), s.camRight, s.camUp, r * 2.6F * flash + 1.0F, 12, 0.2F, s.seed, argb(0.9F * flash, BLAST_CORE), argb(0.0F, BLAST));
			// the column of fire and the flames licking up it
			o.glowLine(new Vector3f(0, 0.3F, 0), new Vector3f(0, top, 0), r * 0.5F, argb(0.5F * burn, BLAST), argb(0.4F * burn, CRIMSON));
			o.glowLine(new Vector3f(0, 0.3F, 0), new Vector3f(0, top, 0), r * 0.2F, argb(0.7F * burn, BLAST_CORE), argb(0.5F * burn, BLAST));
			for (int i = 0; i < 10; i++) {
				float a = Mth.TWO_PI * i / 10 + time * 0.05F;
				Vector3f foot = polar(a, r * 0.25F + r * 0.5F * ball, 0.2F);
				o.flame(foot, new Vector3f(-Mth.cos(a) * 0.25F, 1, -Mth.sin(a) * 0.25F), (3.0F + 4.0F * hash(i + s.seed)) * column + 1.5F, 2.0F, time * 0.6F + i,
					argb(0.75F * burn, BLAST), argb(0.0F, CRIMSON));
			}
			// the cap's rim of rolling flame
			if (cap > 0.0F) {
				for (int i = 0; i < 12; i++) {
					float a = Mth.TWO_PI * i / 12 - time * 0.04F;
					Vector3f at = polar(a, r * 0.75F * cap, top);
					o.flame(at, new Vector3f(Mth.cos(a), -0.5F, Mth.sin(a)), 1.6F * cap, 1.2F, time * 0.5F + i, argb(0.7F * burn, BLAST), argb(0.0F, CRIMSON));
				}
			}
			// the shock running out over the ground
			float sr = r * 2.3F * shock;
			o.ring(new Vector3f(0, 0.16F, 0), Math.max(0.0F, sr - 1.2F), sr, argb(0.9F * (1.0F - shock), BLAST_CORE));
			o.ring(new Vector3f(0, 0.17F, 0), Math.max(0.0F, sr * 0.7F - 0.6F), sr * 0.7F, argb(0.7F * (1.0F - shock), CRIMSON));
			o.disc(new Vector3f(0, 0.15F, 0), r * 1.1F, argb(0.5F * burn, BLAST), argb(0.0F, CRIMSON));
			// embers thrown out
			for (int i = 0; i < 18; i++) {
				Vector3f d = BladeLooks.shardDir(s.seed, i);
				d.y = Math.abs(d.y) * 1.2F + 0.2F;
				float fly = ease(age, hit, hit + 20.0F);
				Vector3f at = new Vector3f(d).mul(1.0F + r * 1.4F * fly).add(0, 1.0F - 4.0F * fly * fly, 0);
				if (at.y > 0.2F) {
					o.glint(at, s.camRight, s.camUp, 0.3F, i, argb(0.9F * burn, i % 2 == 0 ? BLAST_CORE : BLAST));
				}
			}
		});
		if (age >= hit) {
			// the fireball and the mushroom cap
			float br = r * 1.0F * ball + 0.5F;
			ball(ps, c, new Vector3f(0, br * 0.45F, 0), br * 0.45F, 1.0F, 0xFFFFFF, 0.5F * burn);
			ball(ps, c, new Vector3f(0, br * 0.45F, 0), br * 0.8F, 0.8F, BLAST, 0.22F * burn);
			ball(ps, c, new Vector3f(0, br * 0.45F, 0), br, 0.7F, CRIMSON, 0.12F * burn);
			// billows of fire up the column
			for (int i = 0; i < 7; i++) {
				float y = top * (0.22F + 0.11F * i);
				float wob = 0.6F * Mth.sin(time * 0.2F + i * 1.7F);
				ball(ps, c, new Vector3f(wob, y, -wob * 0.5F), r * (0.34F - 0.015F * i) * column + 0.3F, 0.9F, i % 2 == 0 ? BLAST : CRIMSON, 0.16F * burn);
			}
			if (cap > 0.0F) {
				ball(ps, c, new Vector3f(0, top, 0), r * 0.85F * cap + 0.5F, 0.45F, BLAST, 0.25F * burn);
				ball(ps, c, new Vector3f(0, top, 0), r * 0.5F * cap + 0.3F, 0.5F, BLAST_CORE, 0.3F * burn);
				for (int i = 0; i < 6; i++) {
					Vector3f at = polar(Mth.TWO_PI * i / 6 + time * 0.03F, r * 0.75F * cap, top - 0.3F);
					ball(ps, c, at, r * 0.32F * cap + 0.2F, 0.7F, i % 2 == 0 ? BLAST : CRIMSON, 0.2F * burn);
				}
			}
			// by day the light alone washes out: an orange body under it
			float day = 0.16F * burn;
			ps.pushPose();
			ps.translate(0.0F, br * 0.45F, 0.0F);
			ps.scale(1.0F, 0.75F, 1.0F);
			c.submitCustomGeometry(ps, VfxDraw.SHADE, (pose, buffer) -> VfxDraw.ball(pose, buffer, br * 0.9F, 0xD0601A, day * 0.5F));
			ps.popPose();
			if (cap > 0.0F) {
				ps.pushPose();
				ps.translate(0.0F, top, 0.0F);
				ps.scale(1.0F, 0.45F, 1.0F);
				c.submitCustomGeometry(ps, VfxDraw.SHADE, (pose, buffer) -> VfxDraw.ball(pose, buffer, r * 0.85F * cap + 0.5F, 0xC0401A, day * 0.5F));
				ps.popPose();
			}
		}
	}

	/**
	 * Drain Touch: wavering green threads run from every enemy near into the caster's hand, motes of life flowing along them, a small
	 * circle glowing on the palm; with nobody near the threads reach out and curl back empty.
	 */
	private static void drainTouch(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float reach = ease(p, 0.0F, 0.25F);
		float fade = 1.0F - ease(p, 0.75F, 1.0F);
		float time = s.ageInTicks;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f hand = new Vector3f(ahead).mul(s.selfView ? 0.8F : 0.5F).add(new Vector3f(right).mul(0.3F)).add(0, s.selfView ? 1.3F : 1.15F, 0);
		glow(ps, c, o -> {
			o.sprite(hand, s.camRight, s.camUp, 0.45F, argb(0.7F * fade, DRAIN));
			o.magicCircle(hand, right, UP, 0.28F * reach, time * 0.2F, s.seed, 5, argb(0.9F * fade, DRAIN), argb(0.8F * fade, 0xD8FFD8));
			java.util.List<Vector3f> from = new java.util.ArrayList<>();
			for (Vector4f t : s.targets) {
				from.add(new Vector3f(t.x, t.y + t.w * 0.55F, t.z));
			}
			if (from.isEmpty()) {
				for (int i = 0; i < 5; i++) {
					float a = (i - 2) * 0.5F;
					Vector3f d = new Vector3f(ahead).mul(Mth.cos(a)).add(new Vector3f(right).mul(Mth.sin(a)));
					from.add(new Vector3f(d).mul(s.size * Mth.sin(Mth.PI * Math.min(1.0F, p * 1.4F))).add(0, 1.0F, 0));
				}
			}
			for (int j = 0; j < from.size(); j++) {
				Vector3f src = from.get(j);
				Vector3f line = new Vector3f(hand).sub(src);
				Vector3f side = VfxOut.across(line.lengthSquared() < 1.0E-4F ? new Vector3f(ahead) : new Vector3f(line).normalize());
				Vector3f lift = new Vector3f(line).cross(side).normalize();
				for (int k = 0; k < 3; k++) {
					Vector3f prev = null;
					int steps = 14;
					for (int i = 0; i <= (int)(steps * reach); i++) {
						float t = (float)i / steps;
						float wave = Mth.sin(t * Mth.PI * 3.0F + time * 0.5F + k * 2.1F + j) * 0.18F * Mth.sin(Mth.PI * t);
						Vector3f q = new Vector3f(src).lerp(hand, t).add(new Vector3f(side).mul(wave)).add(new Vector3f(lift).mul(wave * (k - 1) * 0.6F));
						if (prev != null) {
							o.glowLine(prev, q, 0.11F, argb(0.7F * fade, DRAIN), argb(0.7F * fade, DRAIN));
							o.bar(prev, q, 0.012F, 0.012F, argb(0.8F * fade, 0xD8FFD8), argb(0.8F * fade, 0xD8FFD8));
						}
						prev = q;
					}
				}
				for (int m = 0; m < 5; m++) {
					float t = (time * 0.07F + hash(j * 7 + m)) % 1.0F;
					if (t > reach) {
						continue;
					}
					Vector3f at = new Vector3f(src).lerp(hand, t * t);
					o.glint(at, s.camRight, s.camUp, 0.16F, time * 0.2F + m, argb(0.95F * fade, 0xD8FFD8));
				}
				o.sprite(src, s.camRight, s.camUp, 0.6F, argb(0.35F * fade * reach, DRAIN));
			}
		});
	}

	/**
	 * The Crimson Demon pose: the eyes flare crimson, and behind the pose a dramatic burst of colour goes off, rays of red, orange and
	 * gold and coloured smoke, as every proper Crimson Demon's entrance needs.
	 */
	private static void crimsonPose(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float burst = ease(p, 0.0F, 0.25F);
		float fade = 1.0F - ease(p, 0.6F, 1.0F);
		float flare = 1.0F - ease(p, 0.0F, 0.4F);
		float time = s.ageInTicks;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f back = new Vector3f(ahead).mul(-2.2F).add(0, 2.1F, 0);
		int[] colours = {0xFF2A3A, 0xFF8A2A, 0xFFD24A, 0xFF4AA0, 0x8A6AFF};
		if (!s.selfView) {
			shade(ps, c, o -> {
				for (int i = 0; i < 5; i++) {
					float a = Mth.PI * (0.1F + 0.8F * i / 4);
					Vector3f at = new Vector3f(right).mul(Mth.cos(a) * 2.0F * burst).add(new Vector3f(UP).mul(Mth.sin(a) * 1.4F * burst)).add(back).add(new Vector3f(ahead)
						.mul(-0.3F));
					o.sprite(at, s.camRight, s.camUp, 0.9F + 0.6F * burst, argb(0.45F * fade * burst, colours[i]));
				}
			});
		}
		glow(ps, c, o -> {
			if (!s.selfView) {
				// the burst behind the pose
				for (int i = 0; i < 16; i++) {
					float a = Mth.TWO_PI * i / 16 + 0.1F;
					float len = (1.3F + 1.1F * hash(s.seed + i)) * burst;
					Vector3f d = new Vector3f(right).mul(Mth.cos(a)).add(new Vector3f(UP).mul(Mth.sin(a)));
					o.blade(back, new Vector3f(d).mul(len).add(back), new Vector3f(ahead), 0.25F, argb(0.7F * fade, VfxDraw.whiten(colours[i % 5], 0.5F)), argb(0.0F, colours[i % 5]));
					o.glowLine(back, new Vector3f(d).mul(len * 1.2F).add(back), 0.3F, argb(0.5F * fade, colours[i % 5]), argb(0.0F, colours[i % 5]));
				}
				o.sprite(back, s.camRight, s.camUp, 1.0F * burst, argb(0.5F * fade, CRIMSON));
				o.star(back, right, UP, 0.8F * burst + 0.2F, 8, 0.35F, time * 0.02F, argb(0.9F * fade, 0xFFFFFF), argb(0.2F * fade, CRIMSON));
				// the eyes
				Vector3f eyes = new Vector3f(ahead).mul(0.26F).add(0, 1.62F, 0);
				for (int side = -1; side <= 1; side += 2) {
					Vector3f eye = new Vector3f(right).mul(side * 0.11F).add(eyes);
					o.sprite(eye, s.camRight, s.camUp, 0.18F + 0.35F * flare, argb(0.95F * fade, CRIMSON));
					o.sprite(eye, s.camRight, s.camUp, 0.05F, argb(fade, 0xFFFFFF));
					o.bar(new Vector3f(right).mul(-0.2F - 0.4F * flare).add(eye), new Vector3f(right).mul(0.2F + 0.4F * flare).add(eye), 0.02F, 0.02F, argb(0.8F * fade,
						CRIMSON_PALE), argb(0.8F * fade, CRIMSON_PALE));
				}
			}
			o.ring(new Vector3f(0, 0.14F, 0), 0.6F + 1.8F * burst, 0.9F + 1.8F * burst, argb(0.7F * fade * (1 - burst * 0.5F), CRIMSON));
			for (int i = 0; i < 10; i++) {
				float rise = (time * 0.04F + hash(i * 3 + s.seed)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 7), 0.7F + 0.5F * hash(i), rise * 2.2F);
				if (s.selfView && at.y > 1.0F) {
					continue;
				}
				o.glint(at, s.camRight, s.camUp, 0.15F, time * 0.2F + i, argb(0.9F * fade * Mth.sin(Mth.PI * rise), colours[i % 5]));
			}
		});
	}

	/** An octahedron gem of {@code r} at {@code c} turned by {@code spin}: filled (shade) or its lit edges. */
	private static void gem(final VfxOut o, final Vector3f c, final float r, final float spin, final int color, final boolean lit) {
		Vector3f top = new Vector3f(0, r * 1.5F, 0).add(c);
		Vector3f bottom = new Vector3f(0, -r * 1.5F, 0).add(c);
		for (int i = 0; i < 4; i++) {
			Vector3f a = polar(spin + Mth.HALF_PI * i, r, 0).add(c);
			Vector3f b = polar(spin + Mth.HALF_PI * (i + 1), r, 0).add(c);
			if (lit) {
				o.bar(a, b, 0.015F, 0.015F, color, color);
				o.bar(a, top, 0.015F, 0.015F, color, color);
				o.bar(a, bottom, 0.015F, 0.015F, color, color);
			} else {
				int shadeA = VfxOut.lerp(color, 0xFF000000 | 0x10305A, i % 2 == 0 ? 0.0F : 0.35F);
				o.tri(a, b, top, shadeA, shadeA, color);
				o.tri(b, a, bottom, shadeA, shadeA, shadeA);
			}
		}
	}

	/**
	 * Manatite: a blue crystal glows in the caster's hand, cracks and bursts, and its shards stream into the caster as mana, blue rings
	 * climbing round them.
	 */
	private static void manatite(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float whole = 1.0F - ease(p, 0.3F, 0.34F);
		float burst = ease(p, 0.32F, 0.5F);
		float absorb = ease(p, 0.45F, 0.85F);
		float time = s.ageInTicks;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f hand = new Vector3f(ahead).mul(s.selfView ? 0.8F : 0.55F).add(new Vector3f(right).mul(0.3F)).add(0, s.selfView ? 1.3F : 1.2F, 0);
		Vector3f chest = new Vector3f(0, 1.1F, 0);
		float glowUp = ease(p, 0.0F, 0.3F);
		shade(ps, c, o -> {
			if (whole > 0.0F) {
				gem(o, hand, 0.22F, time * 0.15F, argb(0.75F * whole, 0x3A7AFF), false);
			}
		});
		glow(ps, c, o -> {
			if (whole > 0.0F) {
				gem(o, hand, 0.22F, time * 0.15F, argb(whole, MANA_PALE), true);
				o.star(hand, s.camRight, s.camUp, 0.5F + 0.9F * glowUp, 6, 0.2F, time * 0.05F, argb(0.7F * whole, MANA_PALE), argb(0.0F, MANA));
				o.sprite(hand, s.camRight, s.camUp, 0.5F + 0.7F * glowUp, argb((0.3F + 0.5F * glowUp) * whole, MANA));
				// the cracks just before it bursts
				if (glowUp > 0.7F) {
					for (int i = 0; i < 4; i++) {
						Vector3f d = BladeLooks.shardDir(s.seed, i);
						o.bar(hand, new Vector3f(d).mul(0.2F).add(hand), 0.012F, 0.006F, argb(whole, 0xFFFFFF), argb(whole, 0xFFFFFF));
					}
				}
			}
			if (burst > 0.0F) {
				float flash = 1.0F - ease(p, 0.32F, 0.45F);
				o.sprite(hand, s.camRight, s.camUp, 0.8F * flash + 0.1F, argb(0.9F * flash, 0xFFFFFF));
				for (int i = 0; i < 14; i++) {
					Vector3f d = BladeLooks.shardDir(s.seed + 3, i);
					Vector3f out = new Vector3f(d).mul(0.9F * burst).add(hand);
					float u = Mth.clamp(absorb * 1.3F - hash(i) * 0.3F, 0.0F, 1.0F);
					// out from the hand, then curving into the chest
					Vector3f at = new Vector3f(out).lerp(chest, u * u);
					Vector3f prev = new Vector3f(out).lerp(chest, Math.max(0.0F, u - 0.15F) * Math.max(0.0F, u - 0.15F));
					if (u < 1.0F) {
						o.diamond(at, d, VfxOut.across(d), 0.26F * (1.0F - u) + 0.06F, 0.11F, argb(0.3F * fade, MANA), argb(0.95F * fade, MANA_PALE));
						o.glowLine(prev, at, 0.06F, argb(0.0F, MANA), argb(0.6F * fade, MANA));
					}
				}
			}
			for (int i = 0; i < 3; i++) {
				float q = (time * 0.04F + i / 3.0F) % 1.0F;
				o.ring(new Vector3f(0, 0.1F + q * (s.selfView ? 0.8F : 2.0F), 0), 0.6F, 0.7F, argb(0.7F * fade * absorb * Mth.sin(Mth.PI * q), MANA_PALE));
			}
			o.ring(new Vector3f(0, 0.14F, 0), 0.5F, 1.1F, argb(0.5F * fade * (0.4F + 0.6F * absorb), MANA));
		});
	}

	// ------------------------------------------------------------ Solomon

	/** Where strike {@code i} of {@code Actions.strike} lands: the first on the aimed spot, the rest scattered a third of the radius round it. */
	private static Vector3f strikeSpot(final State s, final int i) {
		if (i == 0) {
			return new Vector3f();
		}
		float scatter = s.size * 2.0F / 3.0F;
		return new Vector3f((hash(s.seed * 7 + i) - 0.5F) * scatter, 0, (hash(s.seed * 11 + i) - 0.5F) * scatter);
	}

	/**
	 * Ars Almadel Salomonis: over the aimed spot a great golden circle opens with Solomon's ten rings turning under it and the sigils of
	 * the 72 pillars round its rim; a pillar of light stands from it to the ground, and every strike comes down from it as a shaft of gold.
	 */
	private static void arsAlmadel(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float open = ease(age, 0.0F, 8.0F);
		float fade = 1.0F - ease(s.progress, 0.88F, 1.0F);
		float time = s.ageInTicks;
		float r = s.size;
		Vector3f sky = new Vector3f(0, 11.0F, 0);
		shade(ps, c, o -> o.disc(new Vector3f(sky).add(0, 0.05F, 0), r * 1.7F * open, argb(0.5F * fade, 0x24203A), argb(0.1F * fade, 0x24203A)));
		glow(ps, c, o -> {
			o.magicCircle(sky, X, Z, r * 1.25F * open, time * 0.01F, s.seed, 10, argb(0.95F * fade, GOLD), argb(0.85F * fade, GOLD_PALE));
			// the sigils of the 72 pillars round the rim
			int sigils = 72;
			float sr = r * 1.55F * open;
			for (int i = 0; i < sigils; i++) {
				float a = -time * 0.008F + Mth.TWO_PI * i / sigils;
				Vector3f out = polar(a, 1.0F, 0.0F);
				Vector3f along = polar(a + Mth.HALF_PI, 1.0F, 0.0F);
				Vector3f at = new Vector3f(out).mul(sr).add(sky);
				float lit = ease(age - i * 0.25F, 0.0F, 3.0F);
				o.rune(at, along, out, 0.2F, s.seed * 3 + i, 0.025F, argb(0.95F * fade * lit, i % 9 == 0 ? 0xFF6A4A : GOLD_PALE));
			}
			o.circle(sky, X, Z, sr + 0.4F, 0.04F, 48, argb(0.8F * fade, GOLD));
			o.circle(sky, X, Z, sr - 0.4F, 0.04F, 48, argb(0.8F * fade, GOLD));
			// the ten rings under it
			for (int i = 0; i < 10; i++) {
				float a = time * 0.03F + Mth.TWO_PI * i / 10;
				Vector3f at = polar(a, r * 0.6F * open, -1.4F).add(sky);
				Vector3f facing = polar(a + time * 0.05F, 1.0F, 0.0F);
				ring(o, at, facing, 0.45F * open, argb(0.95F * fade, GOLD), argb(0.95F * fade, GEMS[i % GEMS.length]));
			}
			// the pillar of light from the circle to the ground
			float pulse = 0.85F + 0.15F * Mth.sin(time * 0.6F);
			o.glowLine(sky, new Vector3f(0, 0.2F, 0), r * 0.35F * open, argb(0.35F * fade * pulse, GOLD), argb(0.25F * fade * pulse, GOLD));
			o.bar(sky, new Vector3f(0, 0.2F, 0), 0.08F, 0.08F, argb(0.8F * fade * open, 0xFFFFFF), argb(0.8F * fade * open, GOLD_PALE));
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			o.magicCircle(ground, X, Z, r * open, -time * 0.015F, s.seed + 1, 6, argb(0.9F * fade, GOLD), argb(0.8F * fade, GOLD_PALE));
			// the strikes, one every 6 ticks from tick 8
			for (int i = 0; i < 12; i++) {
				float local = (age - 8.0F - i * 6.0F) / 9.0F;
				if (local < 0.0F || local > 1.0F) {
					continue;
				}
				Vector3f spot = strikeSpot(s, i);
				float k = 1.0F - local;
				Vector3f from = new Vector3f(spot).add(0, 10.0F, 0);
				o.glowLine(from, spot, 1.2F * k + 0.2F, argb(0.6F * k, GOLD), argb(0.8F * k, GOLD_PALE));
				o.bar(from, spot, 0.12F * k, 0.15F * k, argb(k, 0xFFFFFF), argb(k, 0xFFFFFF));
				o.sprite(new Vector3f(spot).add(0, 0.6F, 0), s.camRight, s.camUp, 1.6F * k + 0.3F, argb(0.8F * k, GOLD_PALE));
				o.ring(new Vector3f(spot).add(0, 0.16F, 0), r * local, r * local + 0.5F, argb(0.8F * k, GOLD));
			}
		});
	}

	/** A gold ring {@code r} across facing {@code normal}, a gem set in it. */
	private static void ring(final VfxOut o, final Vector3f c, final Vector3f normal, final float r, final int gold, final int gem) {
		Vector3f a = VfxOut.across(normal);
		Vector3f b = new Vector3f(normal).cross(a).normalize();
		o.circle(c, a, b, r, r * 0.12F, 16, gold);
		o.circle(c, a, b, r * 0.8F, r * 0.05F, 16, gold);
		Vector3f setting = new Vector3f(b).mul(r * 1.05F).add(c);
		o.sprite(setting, a, b, r * 0.35F, gem);
		o.sprite(setting, a, new Vector3f(normal).normalize(), r * 0.35F, gem);
	}

	/**
	 * Demon Pillar Eruption: the ground cracks red round the caster and fleshy demon pillars burst out of it, one after another, red eyes
	 * opening all over them; they sink back as it ends.
	 */
	private static void demonPillars(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float sink = 1.0F - ease(p, 0.7F, 1.0F);
		float time = s.ageInTicks;
		int count = 8;
		Vector3f[] base = new Vector3f[count];
		float[] h = new float[count];
		float[] lean = new float[count];
		for (int i = 0; i < count; i++) {
			float a = Mth.TWO_PI * i / count + 0.4F * hash(s.seed + i);
			base[i] = polar(a, s.size * (0.45F + 0.45F * hash(s.seed * 3 + i)), 0.0F);
			h[i] = (2.4F + 2.2F * hash(i * 7 + s.seed)) * ease(age - i * 0.8F, 0.0F, 5.0F) * sink;
			lean[i] = a;
		}
		shade(ps, c, o -> {
			for (int i = 0; i < count; i++) {
				if (h[i] <= 0.01F) {
					continue;
				}
				Vector3f axis = polar(lean[i], 0.12F, 1.0F).normalize();
				Vector3f top = new Vector3f(axis).mul(h[i]).add(base[i]);
				column(o, base[i], top, 0.5F, 0.32F, argb(0.95F, 0x5A1418), argb(0.95F, 0x7A2A22));
			}
			o.disc(new Vector3f(0, 0.13F, 0), s.size * 1.05F, argb(0.45F * sink, 0x1A0508), argb(0.05F, 0x1A0508));
		});
		glow(ps, c, o -> {
			for (int i = 0; i < count; i++) {
				// the cracks the pillar burst out of
				for (int k = 0; k < 3; k++) {
					float a = lean[i] + (k - 1) * 1.6F + hash(i * 5 + k);
					o.bolt(new Vector3f(base[i]).add(0, 0.15F, 0), polar(a, 1.4F + hash(k + i), 0.15F).add(base[i]), s.seed + i * 3 + k, 0.2F, 4, 0.12F, argb(0.6F * sink,
						DEMON), argb(0.9F * sink, 0xFFB080));
				}
				if (h[i] <= 0.01F) {
					continue;
				}
				Vector3f axis = polar(lean[i], 0.12F, 1.0F).normalize();
				// eyes opening up the pillar, facing out from its axis
				for (int e = 0; e < 4; e++) {
					float y = h[i] * (0.2F + 0.2F * e);
					float a = Mth.TWO_PI * hash(s.seed * 13 + i * 4 + e);
					float opened = ease(age - i * 0.8F - 4.0F - e * 1.5F, 0.0F, 3.0F);
					Vector3f out = polar(a, 1.0F, 0.0F);
					Vector3f at = new Vector3f(axis).mul(y).add(base[i]).add(new Vector3f(out).mul(0.47F - 0.12F * y / Math.max(1.0F, h[i])));
					eye(o, at, out, 0.16F, opened, sink, time + i);
				}
				// veins
				Vector3f prev = null;
				for (int q = 0; q <= 8; q++) {
					float t = q / 8.0F;
					float a = lean[i] + t * 4.0F;
					Vector3f pt = new Vector3f(axis).mul(h[i] * t).add(base[i]).add(polar(a, 0.48F - 0.15F * t, 0.0F));
					if (prev != null) {
						o.bar(prev, pt, 0.02F, 0.02F, argb(0.6F * sink, 0xFF4A6A), argb(0.6F * sink, 0xFF4A6A));
					}
					prev = pt;
				}
			}
			float flash = 1.0F - ease(age, 0.0F, 6.0F);
			o.sprite(new Vector3f(0, 0.6F, 0), s.camRight, s.camUp, 2.5F * flash, argb(0.8F * flash, DEMON));
			o.ring(new Vector3f(0, 0.15F, 0), s.size * ease(age, 0.0F, 8.0F) - 0.4F, s.size * ease(age, 0.0F, 8.0F), argb(0.8F * sink, DEMON));
		});
	}

	/** A tapering six-sided column from {@code a} to {@code b}, {@code ra} to {@code rb} thick, ridged. */
	private static void column(final VfxOut o, final Vector3f a, final Vector3f b, final float ra, final float rb, final int c0, final int c1) {
		Vector3f d = new Vector3f(b).sub(a).normalize();
		Vector3f u = VfxOut.across(d);
		Vector3f v = new Vector3f(d).cross(u).normalize();
		int sides = 6;
		int rings = 4;
		for (int j = 0; j < rings; j++) {
			float t0 = (float)j / rings;
			float t1 = (float)(j + 1) / rings;
			Vector3f m0 = new Vector3f(a).lerp(b, t0);
			Vector3f m1 = new Vector3f(a).lerp(b, t1);
			float r0 = Mth.lerp(t0, ra, rb) * (j % 2 == 0 ? 1.0F : 0.9F);
			float r1 = Mth.lerp(t1, ra, rb) * (j % 2 == 0 ? 0.9F : 1.0F);
			for (int i = 0; i < sides; i++) {
				float s0 = Mth.TWO_PI * i / sides;
				float s1 = Mth.TWO_PI * (i + 1) / sides;
				Vector3f o0 = new Vector3f(u).mul(Mth.cos(s0)).add(new Vector3f(v).mul(Mth.sin(s0)));
				Vector3f o1 = new Vector3f(u).mul(Mth.cos(s1)).add(new Vector3f(v).mul(Mth.sin(s1)));
				int col = i % 2 == 0 ? c0 : c1;
				o.quad(new Vector3f(o0).mul(r0).add(m0), new Vector3f(o1).mul(r0).add(m0), new Vector3f(o1).mul(r1).add(m1), new Vector3f(o0).mul(r1).add(m1), col);
			}
		}
		// the top, closed
		for (int i = 0; i < sides; i++) {
			float s0 = Mth.TWO_PI * i / sides;
			float s1 = Mth.TWO_PI * (i + 1) / sides;
			Vector3f o0 = new Vector3f(u).mul(Mth.cos(s0) * rb).add(new Vector3f(v).mul(Mth.sin(s0) * rb));
			Vector3f o1 = new Vector3f(u).mul(Mth.cos(s1) * rb).add(new Vector3f(v).mul(Mth.sin(s1) * rb));
			o.tri(b, new Vector3f(o0).add(b), new Vector3f(o1).add(b), c1, c1, c1);
		}
	}

	/** A demon's eye on a pillar facing {@code out}: an almond of red light, a yellow slit, opening by {@code open}. */
	private static void eye(final VfxOut o, final Vector3f c, final Vector3f out, final float r, final float open, final float alpha, final float time) {
		if (open <= 0.01F) {
			return;
		}
		Vector3f a = VfxOut.across(out);
		Vector3f b = new Vector3f(UP);
		int lid = argb(0.95F * alpha, DEMON);
		Vector3f prevT = null;
		Vector3f prevB = null;
		for (int i = 0; i <= 8; i++) {
			float t = (float)i / 8;
			float x = (t * 2 - 1) * r;
			float y = Mth.sin(t * Mth.PI) * r * 0.45F * open;
			Vector3f top = new Vector3f(a).mul(x).add(new Vector3f(b).mul(y)).add(c);
			Vector3f bottom = new Vector3f(a).mul(x).add(new Vector3f(b).mul(-y)).add(c);
			if (prevT != null) {
				o.bar(prevT, top, 0.02F, 0.02F, lid, lid);
				o.bar(prevB, bottom, 0.02F, 0.02F, lid, lid);
				o.quad(prevT, top, bottom, prevB, argb(0.45F * alpha * open, DEMON));
			}
			prevT = top;
			prevB = bottom;
		}
		float look = 0.3F * Mth.sin(time * 0.1F);
		Vector3f pupil = new Vector3f(a).mul(look * r).add(c);
		o.bar(new Vector3f(b).mul(-r * 0.35F * open).add(pupil), new Vector3f(b).mul(r * 0.35F * open).add(pupil), 0.03F, 0.03F, argb(alpha, 0xFFE04A),
			argb(alpha, 0xFFE04A));
	}

	/**
	 * Clairvoyance: a golden eye opens over the target, the iris turning and its gaze falling on it, a circle of sight laid round its feet
	 * and lines of gold running out of it as it sees past and future.
	 */
	private static void clairvoyance(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float h = Math.max(0.6F, s.size);
		float w = Math.max(0.4F, s.arg);
		float p = s.progress;
		float open = ease(p, 0.0F, 0.25F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float time = s.ageInTicks;
		Vector3f eye = new Vector3f(0, h + 1.1F, 0);
		Vector3f right = s.camRight;
		Vector3f up = s.camUp;
		glow(ps, c, o -> {
			o.sprite(eye, right, up, 1.4F, argb(0.3F * fade, GOLD));
			Vector3f prevT = null;
			Vector3f prevB = null;
			for (int i = 0; i <= 14; i++) {
				float t = (float)i / 14;
				float x = (t * 2 - 1) * 0.85F;
				float y = Mth.sin(t * Mth.PI) * 0.36F * open;
				Vector3f top = new Vector3f(right).mul(x).add(new Vector3f(up).mul(y)).add(eye);
				Vector3f bottom = new Vector3f(right).mul(x).add(new Vector3f(up).mul(-y)).add(eye);
				if (prevT != null) {
					o.bar(prevT, top, 0.04F, 0.04F, argb(0.95F * fade, GOLD), argb(0.95F * fade, GOLD));
					o.bar(prevB, bottom, 0.04F, 0.04F, argb(0.95F * fade, GOLD), argb(0.95F * fade, GOLD));
				}
				prevT = top;
				prevB = bottom;
			}
			o.circle(eye, right, up, 0.3F * open, 0.035F, 20, argb(fade, GOLD_PALE));
			for (int i = 0; i < 12; i++) {
				float a = Mth.TWO_PI * i / 12 + time * 0.08F;
				Vector3f d = new Vector3f(right).mul(Mth.cos(a)).add(new Vector3f(up).mul(Mth.sin(a)));
				o.bar(new Vector3f(d).mul(0.16F * open).add(eye), new Vector3f(d).mul(0.27F * open).add(eye), 0.015F, 0.015F, argb(0.8F * fade, GOLD),
					argb(0.8F * fade, GOLD));
			}
			o.sprite(eye, right, up, 0.1F * open, argb(fade, 0xFFFFFF));
			// its gaze, and lines of sight running out of it
			o.glowLine(eye, new Vector3f(0, h * 0.6F, 0), 0.35F, argb(0.35F * fade * open, GOLD), argb(0.15F * fade, GOLD));
			for (int i = 0; i < 8; i++) {
				float a = Mth.TWO_PI * i / 8 + time * 0.02F;
				float len = 2.0F * ease(p, 0.1F + i * 0.03F, 0.5F);
				o.glowLine(eye, polar(a, len, h + 1.1F - len * 0.4F), 0.05F, argb(0.6F * fade, GOLD_PALE), argb(0.0F, GOLD));
			}
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			o.magicCircle(ground, X, Z, (w * 0.5F + 0.9F) * open, time * 0.03F, s.seed, 6, argb(0.85F * fade, GOLD), argb(0.75F * fade, GOLD_PALE));
		});
	}

	/** Rings of Solomon: ten gold rings, each set with a gem, orbiting the caster on two tilted orbits, glints trailing them. */
	private static void tenRings(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.15F);
		float fade = 1.0F - ease(p, 0.85F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		float orbit = s.size * (s.selfView ? 1.3F : 1.0F) * open;
		float alpha = s.selfView ? 0.6F : 1.0F;
		glow(ps, c, o -> {
			for (int i = 0; i < 10; i++) {
				int band = i / 5;
				float tilt = band == 0 ? 0.35F : -0.35F;
				float a = time * (band == 0 ? 0.06F : -0.05F) + Mth.TWO_PI * (i % 5) / 5 + band * 0.6F;
				Vector3f at = orbitPoint(a, orbit, tilt, band == 0 ? 1.0F : 1.25F);
				Vector3f facing = orbitPoint(a + Mth.HALF_PI, 1.0F, tilt, 0.0F).normalize();
				ring(o, at, facing, 0.26F, argb(0.95F * k * alpha, GOLD), argb(0.95F * k * alpha, GEMS[i % GEMS.length]));
				o.sprite(at, s.camRight, s.camUp, 0.4F, argb(0.3F * k * alpha, GOLD));
				for (int q = 1; q <= 4; q++) {
					float back = a - (band == 0 ? 1 : -1) * q * 0.12F;
					o.sprite(orbitPoint(back, orbit, tilt, band == 0 ? 1.0F : 1.25F), s.camRight, s.camUp, 0.08F, argb(0.5F * k * alpha / q, GOLD_PALE));
				}
			}
			o.ring(new Vector3f(0, 0.14F, 0), orbit - 0.1F, orbit + 0.1F, argb(0.6F * k, GOLD));
			o.sprite(new Vector3f(0, 1.1F, 0), s.camRight, s.camUp, s.selfView ? 0.0F : 1.0F, argb(0.15F * k, GOLD));
		});
	}

	/** A point on an orbit of {@code r} round the caster at height {@code y}, tilted {@code tilt} about the x axis. */
	private static Vector3f orbitPoint(final float a, final float r, final float tilt, final float y) {
		float x = Mth.cos(a) * r;
		float z = Mth.sin(a) * r;
		return new Vector3f(x, y + z * Mth.sin(tilt), z * Mth.cos(tilt));
	}

	private ArcaneLooks() {
	}
}
