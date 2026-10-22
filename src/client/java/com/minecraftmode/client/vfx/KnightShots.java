package com.minecraftmode.client.vfx;

import static com.minecraftmode.client.vfx.SkillVfxRenderer.glow;
import static com.minecraftmode.client.vfx.SkillVfxRenderer.shade;
import static com.minecraftmode.client.vfx.VfxOut.argb;
import static com.minecraftmode.client.vfx.VfxOut.hash;

import com.minecraftmode.client.vfx.SkillProjectileRenderer.State;
import com.minecraftmode.job.skill.SkillLook;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * The warrior's projectile looks: Strike Air's tornado, Lake Slash's crescent of lake water and the heads of Yamata no Orochi. All are
 * sword waves, which hit {@link #HIT} blocks round their flight, so each is drawn that wide. Trails never reach further back than the
 * shot has flown.
 */
final class KnightShots {
	/** How far round its flight a sword wave hits (ProjectileStyle.WAVE.hitRadius). */
	private static final float HIT = 1.0F;
	private static final int AIR = 0xE4F4FF;
	private static final int LAKE = 0x3F7CFF;
	private static final int LAKE_PALE = 0xCFE4FF;
	private static final int SCALE = 0x2E7A5E;
	private static final int SCALE_DARK = 0x08241A;
	private static final int TEAL = 0x40E0D0;

	/** How far round the projectile its look reaches (its culling box grows by this); 0 for the looks of other classes. */
	static double reach(final SkillLook look) {
		return switch (look) {
			case STRIKE_AIR, SERPENT -> 4.0;
			case LAKE_WAVE -> 3.0;
			default -> 0.0;
		};
	}

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case STRIKE_AIR -> strikeAir(s, ps, c);
			case LAKE_WAVE -> lakeWave(s, ps, c);
			case SERPENT -> serpent(s, ps, c);
			default -> {
			}
		}
	}

	/** How far behind the projectile a trail of {@code length} may reach: no further than it has flown. */
	private static float trail(final State s, final float length) {
		return Math.min(length, Math.max(0.0F, s.ageInTicks * Math.max(0.6F, s.speed) - 0.4F));
	}

	/**
	 * Strike Air: the wind sheathing Excalibur hurled as a hammer, a tornado lying along the flight: wind strands spiralling round it out to
	 * the wave's reach, rings of pressure and a bright point at its head.
	 */
	private static void strikeAir(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		Vector3f up = new Vector3f(side).cross(f).normalize();
		float tail = trail(s, 3.2F);
		float time = s.ageInTicks;
		glow(ps, c, o -> {
			o.sprite(new Vector3f(), s.camRight, s.camUp, 0.6F, argb(0.6F, 0xFFFFFF));
			for (int k = 0; k < 5; k++) {
				Vector3f prev = null;
				for (int j = 0; j <= 14; j++) {
					float d = 0.5F - (tail + 0.5F) * j / 14;
					float t = (0.5F - d) / (tail + 0.5F);
					float rr = HIT * (0.35F + 0.65F * t);
					float a = time * 0.9F + d * 3.0F + Mth.TWO_PI * k / 5;
					Vector3f pt = new Vector3f(f).mul(d).add(new Vector3f(side).mul(Mth.cos(a) * rr)).add(new Vector3f(up).mul(Mth.sin(a) * rr));
					if (prev != null) {
						float al = 0.75F * (1.0F - 0.7F * t);
						o.glowLine(prev, pt, 0.07F, argb(al, AIR), argb(al, AIR));
					}
					prev = pt;
				}
			}
			for (int i = 0; i < 3; i++) {
				float d = ((i / 3.0F + time * 0.15F) % 1.0F) * tail;
				o.circle(new Vector3f(f).mul(-d), side, up, HIT * (0.4F + 0.6F * d / Math.max(0.5F, tail)), 0.03F, 20, argb(0.5F * (1.0F - d / (tail + 0.5F)), AIR));
			}
			for (int i = 0; i < 4; i++) {
				float q = (time * 0.12F + hash(s.seed + i)) % 1.0F;
				Vector3f at = new Vector3f(f).mul(-q * tail).add(BladeLooks.shardDir(s.seed, i).mul(HIT * 0.8F));
				o.glint(at, s.camRight, s.camUp, 0.12F, time * 0.3F + i, argb(0.8F * (1.0F - q), 0xFFF2B8));
			}
		});
	}

	/**
	 * Lake Slash: a crescent of lake water and light as wide as the wave hits, its body deep blue under a bright blue rim, two fading
	 * echoes and spray flung off its horns.
	 */
	private static void lakeWave(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		Vector3f up = new Vector3f(side).cross(f).normalize();
		Vector3f axis = new Vector3f(side).mul(Mth.cos(s.tilt)).add(new Vector3f(up).mul(Mth.sin(s.tilt)));
		if (s.mirror) {
			axis.negate();
		}
		float grow = Math.min(1.0F, 0.6F + s.ageInTicks * 0.2F);
		float time = s.ageInTicks;
		float radius = HIT * 1.05F * grow;
		float thick = 0.42F * grow;
		float tail = trail(s, 1.6F);
		ps.pushPose();
		ps.translate(0.0F, 0.1F, 0.0F);
		c.submitCustomGeometry(ps, VfxDraw.GLOW, (pose, buffer) -> {
			for (int echo = 2; echo >= 1; echo--) {
				float b = echo * 0.5F;
				if (b > tail) {
					continue;
				}
				SkillProjectileRenderer.crescent(pose, buffer, f, axis, new Vector3f(f).mul(-b), radius * (1 - 0.07F * echo), thick, LAKE, LAKE, echo == 1 ? 0.4F : 0.18F,
					time, 0.0F);
			}
			SkillProjectileRenderer.crescent(pose, buffer, f, axis, new Vector3f(), radius, thick, LAKE, 0xFFFFFF, 1.0F, time, 0.45F);
		});
		c.submitCustomGeometry(ps, VfxDraw.SHADE, (pose, buffer) -> SkillProjectileRenderer.body(pose, buffer, f, axis, radius, thick, time, 0x0A1430));
		ps.popPose();
		Vector3f across = new Vector3f(f).cross(axis).normalize();
		glow(ps, c, o -> {
			for (int i = 0; i < 8; i++) {
				float q = (time * 0.15F + hash(s.seed + i)) % 1.0F;
				int horn = i % 2 == 0 ? 1 : -1;
				Vector3f at = new Vector3f(axis).mul(radius * 0.5F - radius * Mth.cos(1.1F)).add(new Vector3f(across).mul(horn * radius * Mth.sin(1.1F)))
					.add(new Vector3f(f).mul(-0.4F - q * Math.max(0.3F, tail))).add(0, 0.1F - 0.6F * q * q, 0);
				o.glint(at, s.camRight, s.camUp, 0.1F, time * 0.3F + i, argb(0.85F * (1.0F - q), LAKE_PALE));
			}
		});
	}

	/**
	 * One head of Yamata no Orochi: a serpent's head, scaled dark green, its jaws open on red with white fangs and burning eyes, two horns
	 * swept back; its neck writhes behind it as far as the wave has flown, a teal glow along its spine and a halo as wide as the wave hits.
	 */
	private static void serpent(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		Vector3f up = new Vector3f(side).cross(f).normalize();
		float tail = trail(s, 3.4F);
		float time = s.ageInTicks;
		float bite = 0.3F + 0.15F * Mth.sin(time * 0.6F + s.seed);
		Vector3f skull = new Vector3f(f).mul(-0.1F).add(new Vector3f(up).mul(0.05F));
		Vector3f upperDir = new Vector3f(f).mul(Mth.cos(bite)).add(new Vector3f(up).mul(Mth.sin(bite)));
		Vector3f lowerDir = new Vector3f(f).mul(Mth.cos(bite * 1.4F)).sub(new Vector3f(up).mul(Mth.sin(bite * 1.4F)));
		Vector3f jaw = new Vector3f(f).mul(0.15F);
		Vector3f upper = new Vector3f(upperDir).mul(0.32F).add(jaw).add(new Vector3f(up).mul(0.04F));
		Vector3f lower = new Vector3f(lowerDir).mul(0.3F).add(jaw).add(new Vector3f(up).mul(-0.1F));
		List<Vector3f> neck = new ArrayList<>();
		int steps = 10;
		for (int i = 0; i <= steps; i++) {
			float d = 0.25F + tail * i / steps;
			float k = (float)i / steps;
			neck.add(new Vector3f(f).mul(-d).add(new Vector3f(side).mul(0.35F * Mth.sin(time * 0.5F - d * 1.8F + s.seed) * k))
				.add(new Vector3f(up).mul(0.15F * Mth.sin(time * 0.4F - d * 1.3F) * k)));
		}
		int lit = argb(0.98F, SCALE);
		int dark = argb(0.98F, SCALE_DARK);
		shade(ps, c, o -> {
			if (tail > 0.3F) {
				o.tube(neck, 0.26F, 0.12F, 8, lit, dark);
			}
			o.ellipsoid(skull, new Vector3f(side).mul(0.3F), new Vector3f(up).mul(0.2F), new Vector3f(f).mul(0.36F), 6, 12, lit, dark);
			o.ellipsoid(upper, new Vector3f(side).mul(0.21F), new Vector3f(VfxOut.across(upperDir)).cross(upperDir).normalize(0.08F), new Vector3f(upperDir).mul(0.3F), 5,
				10, lit, dark);
			o.ellipsoid(lower, new Vector3f(side).mul(0.18F), new Vector3f(VfxOut.across(lowerDir)).cross(lowerDir).normalize(0.06F), new Vector3f(lowerDir).mul(0.26F), 5,
				10, lit, dark);
			// the mouth's red inside
			o.quad(new Vector3f(side).mul(-0.14F).add(jaw), new Vector3f(side).mul(0.14F).add(jaw), new Vector3f(side).mul(0.12F).add(new Vector3f(lowerDir).mul(0.45F)).add(jaw),
				new Vector3f(side).mul(-0.12F).add(new Vector3f(lowerDir).mul(0.45F)).add(jaw), argb(0.95F, 0x8A1020));
			o.quad(new Vector3f(side).mul(-0.14F).add(jaw), new Vector3f(side).mul(0.14F).add(jaw), new Vector3f(side).mul(0.12F).add(new Vector3f(upperDir).mul(0.48F)).add(jaw),
				new Vector3f(side).mul(-0.12F).add(new Vector3f(upperDir).mul(0.48F)).add(jaw), argb(0.95F, 0x6A0A18));
			// fangs: two down from the upper jaw, two up from the lower
			for (int k = -1; k <= 1; k += 2) {
				Vector3f u0 = new Vector3f(upperDir).mul(0.5F).add(new Vector3f(side).mul(k * 0.1F)).add(jaw).add(new Vector3f(up).mul(-0.02F));
				o.blade(u0, new Vector3f(u0).sub(new Vector3f(up).mul(0.18F)), f, 0.035F, argb(0.98F, 0xF4F0E0), argb(0.98F, 0xFFFFFF));
				Vector3f l0 = new Vector3f(lowerDir).mul(0.42F).add(new Vector3f(side).mul(k * 0.08F)).add(jaw).add(new Vector3f(up).mul(-0.06F));
				o.blade(l0, new Vector3f(l0).add(new Vector3f(up).mul(0.12F)), f, 0.03F, argb(0.98F, 0xF4F0E0), argb(0.98F, 0xFFFFFF));
				// horns swept back
				Vector3f h0 = new Vector3f(skull).add(new Vector3f(side).mul(k * 0.15F)).add(new Vector3f(up).mul(0.15F));
				o.blade(h0, new Vector3f(h0).add(new Vector3f(f).mul(-0.45F)).add(new Vector3f(up).mul(0.2F)).add(new Vector3f(side).mul(k * 0.1F)), side, 0.05F,
					argb(0.98F, 0x1A2A20), argb(0.98F, 0x5A6A50));
			}
		});
		glow(ps, c, o -> {
			for (int k = -1; k <= 1; k += 2) {
				Vector3f eye = new Vector3f(skull).add(new Vector3f(side).mul(k * 0.24F)).add(new Vector3f(up).mul(0.1F)).add(new Vector3f(f).mul(0.12F));
				o.sprite(eye, s.camRight, s.camUp, 0.12F, argb(1.0F, 0xFFD040));
				o.sprite(eye, s.camRight, s.camUp, 0.28F, argb(0.6F, 0xFF3020));
			}
			if (tail > 0.3F) {
				o.glowPolyline(neck, 0.14F, argb(0.45F, TEAL));
			}
			o.sprite(new Vector3f(f).mul(0.1F), s.camRight, s.camUp, HIT, argb(0.18F, TEAL));
			o.circle(new Vector3f(f).mul(-0.1F), side, up, HIT, 0.02F, 24, argb(0.5F, TEAL));
		});
	}

	private KnightShots() {
	}
}
