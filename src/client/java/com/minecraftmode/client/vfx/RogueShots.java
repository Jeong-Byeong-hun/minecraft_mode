package com.minecraftmode.client.vfx;

import static com.minecraftmode.client.vfx.SkillVfxRenderer.glow;
import static com.minecraftmode.client.vfx.SkillVfxRenderer.shade;
import static com.minecraftmode.client.vfx.VfxOut.argb;
import static com.minecraftmode.client.vfx.VfxOut.hash;

import com.minecraftmode.client.vfx.SkillProjectileRenderer.State;
import com.minecraftmode.job.skill.SkillLook;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * The rogue's projectile looks: Minato's formula kunai, the Perfect Susanoo's Kamui Shuriken and Kamish's dragon fangs. Kunai, shuriken
 * and thrown blades hit {@link #HIT} blocks round their flight; the shuriken's blades of chakra reach past that (it bursts where it hits,
 * {@link SkillLook#KAMUI_VORTEX}). Trails never reach further back than the shot has flown.
 */
final class RogueShots {
	/** How far round its flight a kunai, shuriken or thrown blade hits (ProjectileStyle.hitRadius, vanilla's margin). */
	private static final float HIT = 0.3F;
	private static final int YELLOW = 0xFFD400;
	private static final int YELLOW_PALE = 0xFFF6B8;
	private static final int STEEL = 0x9AA2AC;
	private static final int GRIP = 0x2A2A30;
	private static final int KAMUI = 0x9B59FF;
	private static final int KAMUI_PALE = 0xDCC8FF;
	private static final int KAMUI_DARK = 0x0E0618;
	private static final int DRAGON_FIRE = 0xFF3A2A;
	private static final int DRAGON_PALE = 0xFFB080;
	private static final int BLACK = 0x060204;

	/** How far round the projectile its look reaches (its culling box grows by this); 0 for the looks of other classes. */
	static double reach(final SkillLook look) {
		return switch (look) {
			case FORMULA_KUNAI -> 2.0;
			case KAMUI_SHURIKEN -> 2.5;
			case DRAGON_FANG -> 2.5;
			default -> 0.0;
		};
	}

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case FORMULA_KUNAI -> formulaKunai(s, ps, c);
			case KAMUI_SHURIKEN -> kamuiShuriken(s, ps, c);
			case DRAGON_FANG -> dragonFang(s, ps, c);
			default -> {
			}
		}
	}

	/** How far behind the projectile a trail of {@code length} may reach: no further than it has flown. */
	private static float trail(final State s, final float length) {
		return Math.min(length, Math.max(0.0F, s.ageInTicks * Math.max(0.6F, s.speed) - 0.4F));
	}

	/**
	 * Formula Kunai: Minato's three-pronged kunai flying point first, the paper tag with its formula streaming from the ring, a thin streak
	 * of yellow behind and a glow as wide as it hits.
	 */
	private static void formulaKunai(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		Vector3f up = new Vector3f(side).cross(f).normalize();
		float time = s.ageInTicks;
		float roll = time * 0.3F + s.seed;
		Vector3f flat = new Vector3f(side).mul(Mth.cos(roll)).add(new Vector3f(up).mul(Mth.sin(roll)));
		Vector3f tip = new Vector3f(f).mul(0.45F);
		Vector3f ring = new Vector3f(f).mul(-0.45F);
		float tail = trail(s, 1.6F);
		Vector3f tag0 = new Vector3f(ring).add(new Vector3f(f).mul(-0.08F));
		Vector3f wave = new Vector3f(up).mul(0.08F * Mth.sin(time * 1.3F + s.seed));
		Vector3f tag1 = new Vector3f(tag0).add(new Vector3f(f).mul(-Math.min(0.5F, tail))).add(wave);
		shade(ps, c, o -> {
			o.kunai(ring, tip, flat, argb(0.98F, STEEL), argb(0.98F, GRIP));
			if (tail > 0.1F) {
				Vector3f w = new Vector3f(VfxOut.across(f)).mul(0.07F);
				o.quad(new Vector3f(tag0).sub(w), new Vector3f(tag0).add(w), new Vector3f(tag1).add(w), new Vector3f(tag1).sub(w), argb(0.95F, 0xEEE6D2));
			}
		});
		glow(ps, c, o -> {
			o.sprite(new Vector3f(), s.camRight, s.camUp, HIT + 0.15F, argb(0.3F, YELLOW));
			if (tail > 0.1F) {
				o.glowLine(new Vector3f(f).mul(-0.4F), new Vector3f(f).mul(-0.4F - tail), 0.12F, argb(0.6F, YELLOW_PALE), argb(0.0F, YELLOW));
				Vector3f mid = new Vector3f(tag0).lerp(tag1, 0.5F).add(new Vector3f(up).mul(0.01F));
				o.rune(mid, VfxOut.across(f), new Vector3f(f).negate(), 0.13F, s.seed, 0.012F, argb(0.9F, 0xFFB000));
			}
			o.glowLine(ring, tip, 0.05F, argb(0.5F, YELLOW_PALE), argb(0.8F, 0xFFFFFF));
		});
	}

	/**
	 * Perfect Susanoo: Kamui Shuriken: a great shuriken of violet chakra spinning flat as it flies, four curved blades round a space-time
	 * vortex turning the other way in its middle, a dark eye at its heart and a violet wake behind.
	 */
	private static void kamuiShuriken(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		// the plane it spins in: level with the ground as far as its flight allows
		Vector3f a = new Vector3f(f);
		Vector3f b = new Vector3f(side);
		float time = s.ageInTicks;
		float spin = time * 0.9F + s.seed;
		float grow = Math.min(1.0F, 0.5F + time * 0.25F);
		float bladeR = 0.85F * grow;
		float tail = trail(s, 2.0F);
		shade(ps, c, o -> {
			o.sprite(new Vector3f(), s.camRight, s.camUp, HIT, argb(0.9F, KAMUI_DARK));
			for (int k = 0; k < 4; k++) {
				float ang = spin + Mth.HALF_PI * k;
				Vector3f out = new Vector3f(a).mul(Mth.cos(ang)).add(new Vector3f(b).mul(Mth.sin(ang)));
				Vector3f tang = new Vector3f(a).mul(-Mth.sin(ang)).add(new Vector3f(b).mul(Mth.cos(ang)));
				Vector3f root0 = new Vector3f(tang).mul(HIT * 0.8F);
				Vector3f root1 = new Vector3f(tang).mul(-HIT * 0.5F).add(new Vector3f(out).mul(HIT * 0.6F));
				Vector3f tip = new Vector3f(out).mul(bladeR).add(new Vector3f(tang).mul(-bladeR * 0.45F));
				Vector3f belly = new Vector3f(out).mul(bladeR * 0.55F).add(new Vector3f(tang).mul(bladeR * 0.12F));
				o.tri(root0, belly, tip, argb(0.55F, 0x2A1450), argb(0.55F, 0x2A1450), argb(0.3F, 0x2A1450));
				o.tri(root0, tip, root1, argb(0.55F, 0x2A1450), argb(0.3F, 0x2A1450), argb(0.55F, 0x2A1450));
			}
		});
		glow(ps, c, o -> {
			for (int k = 0; k < 4; k++) {
				float ang = spin + Mth.HALF_PI * k;
				Vector3f out = new Vector3f(a).mul(Mth.cos(ang)).add(new Vector3f(b).mul(Mth.sin(ang)));
				Vector3f tang = new Vector3f(a).mul(-Mth.sin(ang)).add(new Vector3f(b).mul(Mth.cos(ang)));
				Vector3f root0 = new Vector3f(tang).mul(HIT * 0.8F);
				Vector3f root1 = new Vector3f(tang).mul(-HIT * 0.5F).add(new Vector3f(out).mul(HIT * 0.6F));
				Vector3f tip = new Vector3f(out).mul(bladeR).add(new Vector3f(tang).mul(-bladeR * 0.45F));
				Vector3f belly = new Vector3f(out).mul(bladeR * 0.55F).add(new Vector3f(tang).mul(bladeR * 0.12F));
				o.glowLine(root0, belly, 0.05F, argb(0.85F, KAMUI), argb(0.9F, KAMUI_PALE));
				o.glowLine(belly, tip, 0.05F, argb(0.9F, KAMUI_PALE), argb(0.9F, 0xFFFFFF));
				o.glowLine(tip, root1, 0.04F, argb(0.8F, KAMUI_PALE), argb(0.7F, KAMUI));
			}
			// the vortex at its heart, turning against the blades
			for (int k = 0; k < 3; k++) {
				Vector3f prev = null;
				for (int i = 0; i <= 10; i++) {
					float t = i / 10.0F;
					float ang = -spin * 1.4F + Mth.TWO_PI * k / 3 + t * 3.5F;
					float rr = HIT * (1.0F - t) + 0.02F;
					Vector3f q = new Vector3f(s.camRight).mul(Mth.cos(ang) * rr).add(new Vector3f(s.camUp).mul(Mth.sin(ang) * rr));
					if (prev != null) {
						o.glowLine(prev, q, 0.03F, argb(0.9F * t, KAMUI_PALE), argb(0.9F * t, KAMUI_PALE));
					}
					prev = q;
				}
			}
			o.circle(new Vector3f(), a, b, HIT, 0.025F, 16, argb(0.85F, KAMUI_PALE));
			o.sprite(new Vector3f(), s.camRight, s.camUp, bladeR * 1.1F, argb(0.18F, KAMUI));
			if (tail > 0.1F) {
				o.glowLine(new Vector3f(), new Vector3f(f).mul(-tail), bladeR * 0.6F, argb(0.25F, KAMUI), argb(0.0F, KAMUI));
			}
			for (int i = 0; i < 6; i++) {
				float q = (time * 0.15F + hash(s.seed + i)) % 1.0F;
				Vector3f at = new Vector3f(f).mul(-q * Math.max(0.3F, tail)).add(BladeLooks.shardDir(s.seed, i).mul(bladeR * 0.7F));
				o.glint(at, s.camRight, s.camUp, 0.1F, time * 0.3F + i, argb(0.8F * (1.0F - q), KAMUI_PALE));
			}
		});
	}

	/**
	 * Dragon Fang Volley: each dagger thrown wrapped in Kamish's flame, black tongues streaming back off it over red ones, a red-hot glow
	 * as wide as it hits round the blade.
	 */
	private static void dragonFang(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f back = new Vector3f(s.forward).negate();
		Vector3f side = VfxOut.across(back);
		Vector3f up = new Vector3f(side).cross(back).normalize();
		float time = s.ageInTicks;
		float tail = trail(s, 1.8F);
		shade(ps, c, o -> {
			for (int i = 0; i < 5; i++) {
				float ang = Mth.TWO_PI * i / 5 + time * 0.5F;
				Vector3f at = new Vector3f(side).mul(Mth.cos(ang) * 0.12F).add(new Vector3f(up).mul(Mth.sin(ang) * 0.12F));
				o.flame(at, back, Math.max(0.2F, tail * (0.6F + 0.4F * hash(s.seed + i))), 0.32F, time * 0.9F + i * 1.7F, argb(0.9F, BLACK), argb(0.0F, BLACK));
			}
		});
		glow(ps, c, o -> {
			o.sprite(new Vector3f(), s.camRight, s.camUp, HIT + 0.2F, argb(0.55F, DRAGON_FIRE));
			o.sprite(new Vector3f(), s.camRight, s.camUp, 0.15F, argb(0.9F, DRAGON_PALE));
			for (int i = 0; i < 6; i++) {
				float ang = Mth.TWO_PI * i / 6 + time * 0.4F + 0.5F;
				Vector3f at = new Vector3f(side).mul(Mth.cos(ang) * 0.16F).add(new Vector3f(up).mul(Mth.sin(ang) * 0.16F));
				o.flame(at, back, Math.max(0.25F, tail * 1.1F * (0.6F + 0.4F * hash(s.seed * 3 + i))), 0.4F, time * 0.9F + i * 1.3F, argb(0.65F, DRAGON_FIRE),
					argb(0.0F, DRAGON_FIRE));
			}
		});
	}

	private RogueShots() {
	}
}
