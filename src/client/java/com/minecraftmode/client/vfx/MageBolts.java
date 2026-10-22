package com.minecraftmode.client.vfx;

import static com.minecraftmode.client.vfx.SkillVfxRenderer.glow;
import static com.minecraftmode.client.vfx.VfxOut.argb;
import static com.minecraftmode.client.vfx.VfxOut.hash;

import com.minecraftmode.client.vfx.SkillProjectileRenderer.State;
import com.minecraftmode.job.skill.SkillLook;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * The mage's projectile looks: Medea's witch bolts and runic syllables, the east wind's fireballs, Fairy Glitter, Merlin's petal orbs
 * and Frieren's Zoltraak. Trails never reach further back than the shot has flown.
 */
final class MageBolts {
	private static final int VIOLET = 0xC77DFF;
	private static final int PALE = 0xF4E8FF;
	private static final int FIRE = 0xFF6A1A;
	private static final int FIRE_CORE = 0xFFD27A;
	private static final int WIND = 0xDFF6FF;
	private static final int FAIRY = 0xFFF0B8;
	private static final int PINK = 0xFFB0DA;
	private static final int BLOSSOM = 0xFFF4FA;
	private static final int ELF = 0xE8F0FF;
	private static final int ELF_BLUE = 0x9FC8FF;

	/** How far round the projectile its look reaches (its culling box grows by this); 0 for the looks of other classes. */
	static double reach(final SkillLook look) {
		return switch (look) {
			case WITCH_BOLT, PETAL_ORB -> 3.0;
			case RUNE_SYLLABLE -> 2.5;
			case WIND_FIRE, FAIRY_GLITTER -> 3.5;
			case ZOLTRAAK -> 4.5;
			default -> 0.0;
		};
	}

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case WITCH_BOLT -> witchBolt(s, ps, c);
			case RUNE_SYLLABLE -> syllable(s, ps, c);
			case WIND_FIRE -> windFire(s, ps, c);
			case FAIRY_GLITTER -> fairyGlitter(s, ps, c);
			case PETAL_ORB -> petalOrb(s, ps, c);
			case ZOLTRAAK -> zoltraak(s, ps, c);
			default -> {
			}
		}
	}

	/** How far behind the projectile a trail of {@code length} may reach: no further than it has flown. */
	private static float trail(final State s, final float length) {
		return Math.min(length, Math.max(0.0F, s.ageInTicks * Math.max(0.6F, s.speed) - 0.4F));
	}

	/** A bolt of violet witchcraft: a bright head, a tail of violet light and runes tumbling in its wake. */
	private static void witchBolt(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f back = new Vector3f(s.forward).negate();
		float tail = trail(s, 2.6F);
		float time = s.ageInTicks;
		glow(ps, c, o -> {
			Vector3f head = new Vector3f();
			o.glowLine(head, new Vector3f(back).mul(tail), 0.32F, argb(0.6F, s.color), argb(0.0F, s.color));
			o.sprite(head, s.camRight, s.camUp, 0.55F, argb(0.6F, VIOLET));
			o.sprite(head, s.camRight, s.camUp, 0.22F, argb(1.0F, 0xFFFFFF));
			o.star(head, s.camRight, s.camUp, 0.5F, 4, 0.2F, time * 0.3F, argb(0.8F, PALE), argb(0.0F, VIOLET));
			for (int i = 0; i < 3; i++) {
				float d = 0.5F + 0.6F * i;
				if (d > tail) {
					continue;
				}
				float spin = time * 0.4F + i * 2.0F;
				Vector3f a = new Vector3f(s.camRight).mul(Mth.cos(spin)).add(new Vector3f(s.camUp).mul(Mth.sin(spin)));
				Vector3f b = new Vector3f(s.camRight).mul(-Mth.sin(spin)).add(new Vector3f(s.camUp).mul(Mth.cos(spin)));
				o.rune(new Vector3f(back).mul(d), a, b, 0.13F, s.seed * 3 + i, 0.018F, argb(0.9F * (1.0F - d / 2.6F), PALE));
			}
		});
	}

	/** High-Speed Divine Words: one runic syllable flying, spinning in a small ring of light, a thin tail behind it. */
	private static void syllable(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f back = new Vector3f(s.forward).negate();
		float tail = trail(s, 1.8F);
		float spin = s.ageInTicks * 0.3F;
		glow(ps, c, o -> {
			Vector3f head = new Vector3f();
			Vector3f a = new Vector3f(s.camRight).mul(Mth.cos(spin)).add(new Vector3f(s.camUp).mul(Mth.sin(spin)));
			Vector3f b = new Vector3f(s.camRight).mul(-Mth.sin(spin)).add(new Vector3f(s.camUp).mul(Mth.cos(spin)));
			// a plain orb: its ring about the shot's hit radius (0.3)
			o.sprite(head, s.camRight, s.camUp, 0.45F, argb(0.45F, s.color));
			o.rune(head, a, b, 0.26F, s.seed, 0.035F, argb(1.0F, PALE));
			o.circle(head, a, b, 0.35F, 0.02F, 18, argb(0.8F, s.color));
			o.glowLine(head, new Vector3f(back).mul(tail), 0.12F, argb(0.6F, s.color), argb(0.0F, s.color));
		});
	}

	/** A fireball riding the east wind: a ball of fire, flames streaming back and pale wind spiralling round its wake. */
	private static void windFire(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f back = new Vector3f(s.forward).negate();
		float tail = trail(s, 3.0F);
		float time = s.ageInTicks;
		Vector3f u = VfxOut.across(back);
		Vector3f v = new Vector3f(u).cross(back).normalize();
		glow(ps, c, o -> {
			Vector3f head = new Vector3f();
			o.sprite(head, s.camRight, s.camUp, 0.9F, argb(0.55F, FIRE));
			o.sprite(head, s.camRight, s.camUp, 0.45F, argb(0.9F, FIRE_CORE));
			o.sprite(head, s.camRight, s.camUp, 0.2F, argb(1.0F, 0xFFF4D0));
			for (int i = 0; i < 6; i++) {
				float a = Mth.TWO_PI * i / 6 + time * 0.4F;
				Vector3f off = new Vector3f(u).mul(Mth.cos(a) * 0.15F).add(new Vector3f(v).mul(Mth.sin(a) * 0.15F));
				o.flame(off, back, Math.min(tail, 0.9F + 0.6F * hash(i + s.seed)), 0.35F, time * 0.9F + i, argb(0.85F, FIRE_CORE), argb(0.0F, FIRE));
			}
			// the wind winding round the wake
			for (int w = 0; w < 2; w++) {
				Vector3f prev = null;
				int steps = 14;
				for (int i = 0; i <= steps; i++) {
					float d = tail * i / steps;
					float a = w * Mth.PI + d * 3.5F - time * 0.6F;
					float r = 0.3F + 0.15F * d;
					Vector3f q = new Vector3f(back).mul(d).add(new Vector3f(u).mul(Mth.cos(a) * r)).add(new Vector3f(v).mul(Mth.sin(a) * r));
					if (prev != null) {
						float al = 0.7F * (1.0F - (float)i / steps);
						o.glowLine(prev, q, 0.05F, argb(al, WIND), argb(al, WIND));
					}
					prev = q;
				}
			}
		});
	}

	/** Fairy Glitter: a golden-white orb with stars of every colour turning round it and a trail of glittering motes. */
	private static void fairyGlitter(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f back = new Vector3f(s.forward).negate();
		float tail = trail(s, 3.0F);
		float time = s.ageInTicks;
		glow(ps, c, o -> {
			Vector3f head = new Vector3f();
			o.sprite(head, s.camRight, s.camUp, 1.0F, argb(0.5F, FAIRY));
			o.sprite(head, s.camRight, s.camUp, 0.4F, argb(1.0F, 0xFFFFFF));
			o.star(head, s.camRight, s.camUp, 0.9F, 8, 0.2F, time * 0.1F, argb(0.8F, 0xFFFFFF), argb(0.0F, FAIRY));
			int[] rainbow = WitchLooks.RAINBOW;
			for (int i = 0; i < rainbow.length; i++) {
				float a = time * 0.25F + Mth.TWO_PI * i / rainbow.length;
				Vector3f at = new Vector3f(s.camRight).mul(Mth.cos(a) * 0.65F).add(new Vector3f(s.camUp).mul(Mth.sin(a) * 0.65F));
				o.star(at, s.camRight, s.camUp, 0.2F, 5, 0.45F, time * 0.2F + i, argb(0.95F, VfxDraw.whiten(rainbow[i], 0.4F)), argb(0.3F, rainbow[i]));
			}
			Vector3f side = VfxOut.across(back);
			for (int i = 0; i < 10; i++) {
				float d = 0.4F + 2.6F * hash(s.seed * 5 + i);
				if (d > tail) {
					continue;
				}
				Vector3f at = new Vector3f(back).mul(d).add(new Vector3f(side).mul((hash(i * 3) - 0.5F) * 0.6F)).add(0, (hash(i * 7) - 0.5F) * 0.6F, 0);
				o.glint(at, s.camRight, s.camUp, 0.2F, time * 0.2F + i, argb(0.9F * (1.0F - d / 3.2F), rainbow[i % rainbow.length]));
			}
		});
	}

	/** Flower Storm: a pink orb trailing a stream of tumbling petals. */
	private static void petalOrb(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f back = new Vector3f(s.forward).negate();
		float tail = trail(s, 2.6F);
		float time = s.ageInTicks;
		glow(ps, c, o -> {
			Vector3f head = new Vector3f();
			o.sprite(head, s.camRight, s.camUp, 0.5F, argb(0.6F, PINK));
			o.sprite(head, s.camRight, s.camUp, 0.2F, argb(1.0F, 0xFFFFFF));
			o.bloom(head, 5, 0.22F, 0.8F, time * 0.3F, argb(0.6F, PINK), argb(0.95F, BLOSSOM), argb(0.9F, 0xFFE070));
			o.glowLine(head, new Vector3f(back).mul(tail), 0.15F, argb(0.4F, PINK), argb(0.0F, PINK));
			Vector3f side = VfxOut.across(back);
			Vector3f up = new Vector3f(side).cross(back).normalize();
			for (int i = 0; i < 8; i++) {
				float d = 0.3F + 0.3F * i;
				if (d > tail) {
					continue;
				}
				float a = time * 0.3F + i * 1.3F;
				Vector3f at = new Vector3f(back).mul(d).add(new Vector3f(side).mul(Mth.cos(a) * 0.25F * d)).add(new Vector3f(up).mul(Mth.sin(a) * 0.25F * d));
				Vector3f dir = new Vector3f(Mth.cos(time * 0.3F + i), 0.6F, Mth.sin(time * 0.25F + i)).normalize();
				o.diamond(at, dir, VfxOut.across(dir), 0.2F, 0.12F, argb(0.3F, PINK), argb(0.9F * (1.0F - d / 3.0F), i % 2 == 0 ? BLOSSOM : PINK));
			}
		});
	}

	/** Zoltraak: a crisp white line of killing magic, a hair-thin core and a pale blue edge, a small ring round its head. */
	private static void zoltraak(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f back = new Vector3f(s.forward).negate();
		float tail = trail(s, 4.0F);
		glow(ps, c, o -> {
			Vector3f head = new Vector3f(s.forward).mul(0.3F);
			Vector3f end = new Vector3f(back).mul(tail);
			o.glowLine(head, end, 0.4F, argb(0.7F, ELF_BLUE), argb(0.0F, ELF_BLUE));
			o.bar(head, end, 0.07F, 0.04F, argb(1.0F, 0xFFFFFF), argb(0.3F, ELF));
			o.sprite(head, s.camRight, s.camUp, 0.3F, argb(0.9F, 0xFFFFFF));
			Vector3f a = VfxOut.across(s.forward);
			Vector3f b = new Vector3f(s.forward).cross(a).normalize();
			o.circle(new Vector3f(), a, b, 0.22F, 0.015F, 14, argb(0.8F, ELF));
		});
	}

	private MageBolts() {
	}
}
