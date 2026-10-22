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

/**
 * Looks of the Zoldyck family (Hunter x Hunter): Silva's Nen orbs bursting, the presence of the head of the family and the assassin's
 * sharpened hand; Illumi's needles (the rain of them and their threads, the needles that change his face and the one that makes a puppet of
 * its target).
 */
final class ZoldyckLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final int ORANGE = 0xFF9A3A;
	private static final int ORANGE_PALE = 0xFFE6C0;
	private static final int SILVER = 0xD8E0F0;
	private static final int SILVER_DARK = 0x3A4050;
	private static final int ICE = 0xB8F0FF;
	private static final int SKIN = 0xE8D6C4;
	private static final int CLAW = 0xF4F6FA;
	private static final int BLOOD = 0xD01828;
	private static final int VIOLET = 0xB98CFF;
	private static final int VIOLET_PALE = 0xEADCFF;
	private static final int VIOLET_DEEP = 0x6A2AC8;
	private static final int PIN = 0x2A2434;
	private static final int PIN_HEAD = 0x6A3AB0;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case ORB_BURST -> orbBurst(s, ps, c);
			case SILVA_PRESENCE -> presence(s, ps, c);
			case ASSASSIN_HAND -> assassinHand(s, ps, c);
			case NEEDLE_RAIN -> needleRain(s, ps, c);
			case NEEDLE_DISGUISE -> disguise(s, ps, c);
			case NEEDLE_PUPPET -> puppet(s, ps, c);
			default -> {
			}
		}
	}

	/**
	 * Where a Nen orb bursts: the packed aura breaks out in a sphere as wide as the blast hits (its rim facing the camera and a ring level with
	 * the ground), shards of orange light flying out to it and a white flash at the heart.
	 */
	private static void orbBurst(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = Math.max(0.5F, s.size);
		float time = s.ageInTicks;
		float q = ease(age, 0.0F, 4.0F);
		float fade = 1.0F - ease(p, 0.4F, 1.0F);
		glow(ps, c, o -> {
			o.sprite(new Vector3f(), s.camRight, s.camUp, r * q, argb(0.35F * fade, ORANGE));
			o.circle(new Vector3f(), s.camRight, s.camUp, r * q, 0.06F, 32, argb(0.9F * fade, ORANGE_PALE));
			o.circle(new Vector3f(), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), r * q, 0.05F, 32, argb(0.55F * fade, ORANGE));
			for (int i = 0; i < 12; i++) {
				Vector3f d = BladeLooks.shardDir(s.seed, i);
				o.glowLine(new Vector3f(d).mul(r * 0.25F * q), new Vector3f(d).mul(r * q), 0.08F, argb(0.8F * fade, ORANGE_PALE), argb(0.0F, ORANGE));
			}
			float flash = 1.0F - ease(age, 0.0F, 5.0F);
			if (flash > 0.0F) {
				o.sprite(new Vector3f(), s.camRight, s.camUp, r * 0.6F * flash + 0.2F, argb(0.95F * flash, 0xFFFFFF));
			}
			for (int i = 0; i < 8; i++) {
				Vector3f d = BladeLooks.shardDir(s.seed * 3, i);
				o.glint(new Vector3f(d).mul(r * (0.4F + 0.6F * q)), s.camRight, s.camUp, 0.12F, time * 0.3F + i, argb(0.85F * fade, ORANGE_PALE));
			}
		});
	}

	/**
	 * Head of the Family: the patriarch's presence rolls out round the caster as a wave of pressure to the edge of what it reaches, a heavy
	 * silver haze over the ground, pulses running out again and again; a cold silver aura rises off him and his piercing eyes open over it.
	 */
	private static void presence(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float r = Math.max(1.0F, s.size);
		float wave = ease(age, 0.0F, 10.0F);
		float fade = 1.0F - ease(p, 0.75F, 1.0F);
		float self = s.selfView ? 0.0F : 1.0F;
		Vector3f eyes = new Vector3f(0, 3.4F, 0);
		shade(ps, c, o -> {
			for (int i = 0; i < 18; i++) {
				float a = Mth.TWO_PI * hash(s.seed + i * 3);
				Vector3f at = polar(a + time * 0.01F, r * wave * (0.25F + 0.7F * hash(i * 5)), 0.35F);
				o.sprite(at, s.camRight, s.camUp, 1.0F + 0.6F * hash(i * 7), argb(0.25F * fade, SILVER_DARK));
			}
		});
		glow(ps, c, o -> {
			o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, r * wave - 0.35F), r * wave, argb(0.8F * fade, SILVER));
			o.ring(new Vector3f(0, 0.16F, 0), Math.max(0.0F, r * wave - 0.07F), r * wave, argb(0.55F * fade, 0xFFFFFF));
			for (int i = 0; i < 2; i++) {
				float pulse = ((time * 0.04F + i * 0.5F) % 1.0F);
				o.ring(new Vector3f(0, 0.14F, 0), Math.max(0.0F, r * pulse - 0.6F), r * pulse, argb(0.35F * fade * (1.0F - pulse), SILVER));
			}
			if (self > 0.0F) {
				for (int i = 0; i < 12; i++) {
					float a = Mth.TWO_PI * i / 12 + hash(s.seed + i);
					o.flame(polar(a, 0.6F, 0.0F), UP, (1.6F + 1.0F * hash(i * 3 + (int)(time / 5))) * wave, 0.4F, time * 0.4F + i, argb(0.35F * fade, SILVER),
						argb(0.0F, SILVER));
				}
				float open = ease(age, 4.0F, 12.0F);
				for (int e = -1; e <= 1; e += 2) {
					Vector3f at = new Vector3f(s.camRight).mul(e * 0.7F).add(eyes);
					Vector3f tilt = new Vector3f(s.camRight).mul(1.0F).add(new Vector3f(s.camUp).mul(-0.22F * e)).normalize();
					Vector3f n = new Vector3f(tilt).cross(new Vector3f(s.camRight).cross(s.camUp)).normalize();
					o.diamond(at, tilt, n, 1.0F, 0.2F * open, argb(0.95F * fade, ICE), argb(0.95F * fade, 0xFFFFFF));
					o.sprite(at, s.camRight, s.camUp, 0.8F, argb(0.35F * fade * open, ICE));
					o.diamond(at, n, tilt, 0.18F * open, 0.05F, argb(fade, 0x10202A), argb(fade, 0x10202A));
				}
				o.sprite(eyes, s.camRight, s.camUp, 2.0F, argb(0.12F * fade, SILVER));
			}
		});
	}

	/**
	 * Assassin's Judgment: a hand with its nails grown into long claws drives through the target from the caster's side, then three claw cuts
	 * flash across it and a spray of red.
	 */
	private static void assassinHand(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float h = Math.max(1.0F, s.size);
		float w = Math.max(0.6F, s.arg);
		float time = s.ageInTicks;
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		Vector3f mid = new Vector3f(0, h * 0.55F, 0);
		float thrust = ease(age, 0.0F, 4.0F);
		float fade = 1.0F - ease(p, 0.55F, 1.0F);
		Vector3f hand = new Vector3f(f).mul(-(w * 0.5F + 1.4F) + (w + 2.0F) * thrust).add(mid);
		// the hand drawn twice life size round its own middle, so the claws read from a few blocks off
		ps.pushPose();
		ps.translate(hand.x, hand.y, hand.z);
		ps.scale(2.0F, 2.0F, 2.0F);
		Vector3f origin = new Vector3f();
		shade(ps, c, o -> {
			if (fade <= 0.0F) {
				return;
			}
			Vector3f hand0 = origin;
			Vector3f wrist = new Vector3f(hand0).add(new Vector3f(f).mul(-0.35F));
			o.tube(java.util.List.of(new Vector3f(wrist).add(new Vector3f(f).mul(-0.5F)), wrist), 0.09F, 0.08F, 6, argb(0.95F * fade, SKIN), argb(0.95F * fade, 0x8A6A58));
			o.ellipsoid(hand0, new Vector3f(side).mul(0.12F), new Vector3f(UP).mul(0.05F), new Vector3f(f).mul(0.16F), 4, 8, argb(0.95F * fade, SKIN),
				argb(0.95F * fade, 0x8A6A58));
			for (int i = 0; i < 4; i++) {
				Vector3f root = new Vector3f(hand0).add(new Vector3f(f).mul(0.12F)).add(new Vector3f(side).mul((i - 1.5F) * 0.055F));
				o.blade(root, new Vector3f(root).add(new Vector3f(f).mul(0.55F + 0.08F * (1.5F - Math.abs(i - 1.5F)))), UP, 0.04F, argb(0.97F * fade, CLAW),
					argb(0.97F * fade, 0xFFFFFF));
			}
			o.blade(new Vector3f(hand0).add(new Vector3f(side).mul(-0.13F)), new Vector3f(hand0).add(new Vector3f(side).mul(-0.2F)).add(new Vector3f(f).mul(0.4F)), UP, 0.035F,
				argb(0.97F * fade, CLAW), argb(0.97F * fade, 0xFFFFFF));
		});
		ps.popPose();
		glow(ps, c, o -> {
			o.glowLine(new Vector3f(hand).add(new Vector3f(f).mul(-0.8F)), new Vector3f(hand).add(new Vector3f(f).mul(0.65F)), 0.25F, argb(0.35F * fade, ORANGE),
				argb(0.5F * fade, ORANGE_PALE));
			float cut = ease(age, 4.0F, 8.0F);
			if (cut > 0.0F) {
				for (int i = 0; i < 3; i++) {
					Vector3f o0 = new Vector3f(side).mul(-0.8F * w - 0.3F).add(new Vector3f(UP).mul(0.5F * h - 0.25F * i)).add(new Vector3f(f).mul(-w * 0.5F - 0.1F)).add(mid);
					Vector3f o1 = new Vector3f(side).mul(0.8F * w + 0.3F).add(new Vector3f(UP).mul(-0.3F * h - 0.25F * i)).add(new Vector3f(f).mul(-w * 0.5F - 0.1F)).add(mid);
					o.glowLine(o0, new Vector3f(o0).lerp(o1, cut), 0.09F, argb(0.9F * fade, ORANGE_PALE), argb(0.9F * fade, 0xFFFFFF));
				}
				o.star(mid, s.camRight, s.camUp, 0.9F * (1.0F - cut * 0.5F), 6, 0.2F, s.seed, argb(0.8F * fade, 0xFFFFFF), argb(0.0F, ORANGE));
			}
			for (int i = 0; i < 10; i++) {
				float q = Math.min(1.0F, Math.max(0.0F, age - 4.0F) / 14.0F);
				Vector3f d = BladeLooks.shardDir(s.seed, i);
				Vector3f at = new Vector3f(d).mul(0.3F + 1.2F * q).add(new Vector3f(f).mul(0.8F * q)).add(mid).add(0, -1.0F * q * q, 0);
				o.sprite(at, s.camRight, s.camUp, 0.1F, argb(0.85F * fade * (age > 4.0F ? 1.0F : 0.0F), BLOOD));
			}
		});
	}

	/**
	 * Thousand Needles: needles stick up out of the aimed ground one after another inside a violet ring as wide as the rain falls, threads of
	 * Nen drawn from each of them up to a point over the middle and from needle to needle round it.
	 */
	private static void needleRain(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float r = Math.max(1.0F, s.size);
		float open = ease(age, 0.0F, 5.0F);
		float fade = 1.0F - ease(p, 0.75F, 1.0F);
		int count = 14;
		Vector3f[] base = new Vector3f[count];
		Vector3f[] head = new Vector3f[count];
		float[] shown = new float[count];
		for (int i = 0; i < count; i++) {
			float a = Mth.TWO_PI * i / count + hash(s.seed + i) * 0.4F;
			float d = r * (0.35F + 0.58F * hash(s.seed * 3 + i));
			Vector3f lean = polar(a + 1.0F, 0.15F, 0.0F);
			base[i] = polar(a, d, 0.05F);
			head[i] = new Vector3f(base[i]).add(lean).add(0, 0.75F, 0);
			shown[i] = ease(age, 8.0F + i * 1.6F, 10.0F + i * 1.6F);
		}
		Vector3f knot = new Vector3f(0, 4.5F, 0);
		shade(ps, c, o -> {
			for (int i = 0; i < count; i++) {
				if (shown[i] > 0.0F) {
					Vector3f top = new Vector3f(base[i]).lerp(head[i], shown[i]);
					o.pin(top, new Vector3f(base[i]).add(0, -0.1F, 0), 0.07F, argb(0.97F * fade, PIN), argb(0.97F * fade, PIN_HEAD));
				}
			}
		});
		glow(ps, c, o -> {
			o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, r * open - 0.3F), r * open, argb(0.8F * fade, VIOLET));
			o.ring(new Vector3f(0, 0.16F, 0), Math.max(0.0F, r * open - 0.06F), r * open, argb(0.55F * fade, VIOLET_PALE));
			o.disc(new Vector3f(0, 0.13F, 0), r * open, argb(0.1F * fade, VIOLET_DEEP), argb(0.04F * fade, VIOLET));
			int prev = -1;
			for (int i = 0; i < count; i++) {
				if (shown[i] <= 0.0F) {
					continue;
				}
				Vector3f top = new Vector3f(base[i]).lerp(head[i], shown[i]);
				o.sprite(top, s.camRight, s.camUp, 0.2F, argb(0.85F * fade, VIOLET));
				o.glowLine(top, new Vector3f(base[i]).add(0, -0.1F, 0), 0.07F, argb(0.5F * fade, VIOLET), argb(0.3F * fade, VIOLET));
				float sway = 0.05F * Mth.sin(time * 0.2F + i);
				o.glowLine(top, new Vector3f(knot).add(sway, 0, -sway), 0.025F, argb(0.6F * fade * shown[i], VIOLET_PALE), argb(0.15F * fade, VIOLET_PALE));
				if (prev >= 0) {
					o.glowLine(new Vector3f(base[prev]).lerp(head[prev], 0.6F), new Vector3f(base[i]).lerp(head[i], 0.6F), 0.02F, argb(0.45F * fade, VIOLET),
						argb(0.45F * fade, VIOLET));
				}
				prev = i;
			}
			o.sprite(knot, s.camRight, s.camUp, 0.3F, argb(0.5F * fade * open, VIOLET_PALE));
		});
	}

	/**
	 * Needle Disguise: needles drive one by one into the caster's head from every side, violet ripples pass over it and the face flickers
	 * as it changes into another, the needles left standing out of the head like a crown of spikes while the caster fades from sight. In
	 * first person only sparks at the feet.
	 */
	private static void disguise(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f head = new Vector3f(0, 1.62F, 0).add(new Vector3f(ahead).mul(0.03F));
		int count = 9;
		Vector3f[] dirs = new Vector3f[count];
		float[] in = new float[count];
		for (int i = 0; i < count; i++) {
			float a = Mth.TWO_PI * i / count + 0.3F;
			float up = 0.35F + 0.6F * hash(s.seed + i);
			dirs[i] = new Vector3f(Mth.cos(a), up, Mth.sin(a)).normalize();
			in[i] = ease(age, 1.0F + i * 1.3F, 4.0F + i * 1.3F);
		}
		shade(ps, c, o -> {
			if (s.selfView) {
				return;
			}
			for (int i = 0; i < count; i++) {
				if (in[i] <= 0.0F) {
					continue;
				}
				Vector3f d = dirs[i];
				float out = 0.25F + 0.45F + 0.8F * (1.0F - in[i]);
				o.pin(new Vector3f(d).mul(out).add(head), new Vector3f(d).mul(0.12F).add(head), 0.05F, argb(0.97F * fade, PIN), argb(0.97F * fade, PIN_HEAD));
			}
		});
		glow(ps, c, o -> {
			float change = ease(age, 8.0F, 20.0F) * (1.0F - ease(age, 22.0F, 30.0F));
			if (!s.selfView) {
				for (int i = 0; i < count; i++) {
					if (in[i] > 0.0F && in[i] < 1.0F) {
						o.sprite(new Vector3f(dirs[i]).mul(0.2F).add(head), s.camRight, s.camUp, 0.2F, argb(0.8F * (1.0F - in[i]), VIOLET_PALE));
					}
				}
				for (int i = 0; i < 3; i++) {
					float q = (time * 0.08F + i / 3.0F) % 1.0F;
					o.circle(head, s.camRight, s.camUp, 0.2F + 0.4F * q, 0.02F, 16, argb(0.6F * fade * (1.0F - q) * Math.max(0.2F, change), VIOLET));
				}
				// the face flickering as it changes: bands of violet static across it
				if (change > 0.0F) {
					for (int i = 0; i < 5; i++) {
						float y = -0.2F + 0.4F * hash((int)(time / 2) * 7 + i);
						Vector3f mid = new Vector3f(s.camUp).mul(y).add(head).add(new Vector3f(s.camRight).cross(s.camUp).mul(-0.3F));
						o.glowLine(new Vector3f(s.camRight).mul(-0.3F).add(mid), new Vector3f(s.camRight).mul(0.3F).add(mid), 0.03F, argb(0.7F * change, VIOLET_PALE),
							argb(0.7F * change, VIOLET));
					}
					o.sprite(head, s.camRight, s.camUp, 0.5F, argb(0.3F * change, VIOLET));
				}
			}
			for (int i = 0; i < 8; i++) {
				float q = (time * 0.06F + hash(s.seed + i)) % 1.0F;
				o.glint(polar(Mth.TWO_PI * hash(i * 3), 0.5F + 0.3F * q, 0.2F + 1.6F * q), s.camRight, s.camUp, 0.1F, i, argb(0.8F * fade * (1.0F - q), VIOLET_PALE));
			}
		});
	}

	/**
	 * Zoldyck Assassination: a long needle comes down and drives into the target's head, violet threads of Nen running up from it and from its
	 * shoulders to a hand's reach overhead, jerking it like a puppet, a pulse of violet running over it and a flash as it strikes.
	 */
	private static void puppet(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float h = Math.max(1.0F, s.size);
		float w = Math.max(0.6F, s.arg);
		float time = s.ageInTicks;
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		float drive = ease(age, 0.0F, 4.0F);
		float fade = 1.0F - ease(p, 0.65F, 1.0F);
		Vector3f crown = new Vector3f(0, h * 0.92F, 0);
		Vector3f d = new Vector3f(f).mul(-0.55F).add(0, 1.0F, 0).normalize();
		Vector3f tip = new Vector3f(crown).add(new Vector3f(d).mul(2.6F * (1.0F - drive) - 0.15F));
		Vector3f pinHead = new Vector3f(tip).add(new Vector3f(d).mul(1.1F));
		float jerk = 0.12F * Mth.sin(time * 0.9F);
		Vector3f hold = new Vector3f(0, h + 3.2F, 0).add(new Vector3f(f).mul(-1.2F));
		Vector3f[] strings = {pinHead, new Vector3f(side).mul(w * 0.5F + 0.1F).add(0, h * 0.75F, 0), new Vector3f(side).mul(-w * 0.5F - 0.1F).add(0, h * 0.75F, 0)};
		shade(ps, c, o -> o.pin(pinHead, tip, 0.13F, argb(0.97F * fade, PIN), argb(0.97F * fade, PIN_HEAD)));
		glow(ps, c, o -> {
			o.glowLine(pinHead, tip, 0.12F, argb(0.45F * fade, VIOLET), argb(0.6F * fade, VIOLET_PALE));
			o.sprite(pinHead, s.camRight, s.camUp, 0.35F, argb(0.8F * fade, VIOLET));
			if (drive >= 1.0F) {
				for (int i = 0; i < strings.length; i++) {
					Vector3f top = new Vector3f(hold).add(new Vector3f(side).mul((i - 1) * 0.35F)).add(0, i == 0 ? jerk : -jerk, 0);
					o.glowLine(strings[i], top, 0.03F, argb(0.8F * fade, VIOLET_PALE), argb(0.25F * fade, VIOLET_PALE));
				}
				o.sprite(hold, s.camRight, s.camUp, 0.25F, argb(0.5F * fade, VIOLET));
				for (int i = 0; i < 3; i++) {
					float q = (time * 0.07F + i / 3.0F) % 1.0F;
					o.circle(new Vector3f(0, h * q, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), w * 0.5F + 0.2F, 0.03F, 20, argb(0.6F * fade * Mth.sin(Mth.PI * q),
						VIOLET));
				}
			}
			float flash = 1.0F - ease(age, 3.0F, 10.0F);
			if (drive >= 1.0F && flash > 0.0F) {
				o.star(crown, s.camRight, s.camUp, 0.9F * flash + 0.2F, 6, 0.2F, s.seed, argb(0.9F * flash, 0xFFFFFF), argb(0.0F, VIOLET));
			}
		});
	}

	private ZoldyckLooks() {
	}
}
