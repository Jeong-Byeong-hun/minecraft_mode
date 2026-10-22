package com.minecraftmode.client.vfx;

import static com.minecraftmode.client.vfx.SkillVfxRenderer.glow;
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
 * Looks of Zeno Zoldyck's dragons (Hunter x Hunter): where a Dragon Head bursts, the Dragon Lance, the aura dragon coiled round him while
 * he bides his time and the great dragon of Dragon Dive over the ground its dragons rain on (the flying dragons themselves are
 * {@link HunterShots}). Every dragon is {@link VfxOut#auraDragon}.
 */
final class DragonLooks {
	private static final int AURA = 0x6CD0FF;
	private static final int PALE = 0xE4F8FF;
	private static final int DEEP = 0x2A8AE0;
	private static final int EYE = 0xFFFFFF;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case DRAGON_BURST -> burst(s, ps, c);
			case DRAGON_LANCE -> lance(s, ps, c);
			case ZENO_AURA -> coiled(s, ps, c);
			case DRAGON_DIVE -> dive(s, ps, c);
			default -> {
			}
		}
	}

	/**
	 * Where a Dragon Head bursts: the dragon's aura breaks out in a sphere as wide as the blast hits (its rim facing the camera), tongues of
	 * blue flame thrown out to it, a flash at the heart and the dragon's head roaring up out of it as it fades.
	 */
	private static void burst(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = Math.max(0.5F, s.size);
		float time = s.ageInTicks;
		float q = ease(age, 0.0F, 5.0F);
		float fade = 1.0F - ease(p, 0.45F, 1.0F);
		glow(ps, c, o -> {
			o.sprite(new Vector3f(), s.camRight, s.camUp, r * q, argb(0.3F * fade, AURA));
			o.circle(new Vector3f(), s.camRight, s.camUp, r * q, 0.07F, 32, argb(0.85F * fade, PALE));
			o.circle(new Vector3f(), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), r * q, 0.05F, 32, argb(0.5F * fade, AURA));
			for (int i = 0; i < 12; i++) {
				Vector3f d = BladeLooks.shardDir(s.seed, i);
				o.flame(new Vector3f(d).mul(r * 0.2F), d, r * 0.8F * q, 0.35F, time * 0.6F + i, argb(0.6F * fade, AURA), argb(0.0F, PALE));
			}
			float flash = 1.0F - ease(age, 0.0F, 6.0F);
			if (flash > 0.0F) {
				o.star(new Vector3f(), s.camRight, s.camUp, r * 0.9F * flash + 0.2F, 10, 0.3F, s.seed, argb(0.95F * flash, 0xFFFFFF), argb(0.0F, AURA));
			}
			// the head rising out of the blast and fading
			float rise = ease(age, 2.0F, 18.0F);
			List<Vector3f> spine = new ArrayList<>();
			for (int i = 0; i <= 6; i++) {
				float t = i / 6.0F;
				spine.add(new Vector3f(0.4F * Mth.sin(t * 3.0F + time * 0.2F), r * 0.4F + 2.2F * rise - 1.6F * t, 0.3F * t));
			}
			o.auraDragon(spine, 0.35F, 0.8F, time, argb(0.6F * fade * (1.0F - rise * 0.5F), AURA), argb(0.85F * fade, PALE), argb(fade, EYE));
		});
	}

	/**
	 * Dragon Lance: a long aura dragon shoots straight out along the beam, its open jaws leading, its body weaving inside the width the
	 * beam hits ({@link PendragonLooks#BEAM_R}, faint lines along it), sparks streaming off.
	 */
	private static void lance(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float len = Math.max(2.0F, s.size);
		Vector3f dir = PendragonLooks.beamDir(s);
		Vector3f side = VfxOut.across(dir);
		Vector3f up = PendragonLooks.beamUp(dir);
		float fade = 1.0F - ease(p, 0.55F, 1.0F);
		float shoot = ease(age, 0.0F, 6.0F);
		float r = PendragonLooks.BEAM_R;
		float head = 0.8F + (len - 0.8F) * shoot;
		List<Vector3f> spine = new ArrayList<>();
		int n = Math.max(8, (int)(head * 1.2F));
		for (int i = 0; i <= n; i++) {
			float t = i / (float)n;
			float d = head * (1.0F - t) + 0.6F * t;
			float wave = Mth.sin(d * 0.9F - time * 0.6F) * 0.35F * Math.min(1.0F, t * 4.0F);
			spine.add(new Vector3f(dir).mul(d).add(new Vector3f(side).mul(wave)).add(new Vector3f(up).mul(0.2F * Mth.cos(d * 0.7F - time * 0.5F))));
		}
		glow(ps, c, o -> {
			o.auraDragon(spine, 0.42F, 0.9F, time, argb(0.65F * fade, AURA), argb(0.95F * fade, PALE), argb(fade, EYE));
			Vector3f from = new Vector3f(dir).mul(0.8F);
			Vector3f front = new Vector3f(dir).mul(head);
			if (head > 1.0F) {
				for (int k = 0; k < 4; k++) {
					Vector3f off = new Vector3f(k < 2 ? up : side).mul((k % 2 == 0 ? 1 : -1) * r);
					o.glowLine(new Vector3f(from).add(off), new Vector3f(front).add(off), 0.06F, argb(0.3F * fade, AURA), argb(0.3F * fade, AURA));
				}
				o.glowLine(from, front, r, argb(0.12F * fade, AURA), argb(0.12F * fade, AURA));
			}
			for (int i = 0; i < 12; i++) {
				float q = (time * 0.06F + hash(s.seed + i)) % 1.0F;
				Vector3f at = new Vector3f(dir).mul(0.8F + (head - 0.8F) * hash(i * 7)).add(new Vector3f(side).mul((hash(i * 3) - 0.5F) * 1.4F))
					.add(new Vector3f(up).mul(-0.5F + 1.6F * q));
				o.glint(at, s.camRight, s.camUp, 0.15F, time * 0.2F + i, argb(0.85F * fade * Mth.sin(q * Mth.PI), PALE));
			}
		});
	}

	/**
	 * Zoldyck Patience: a small dragon of aura coils round the caster, its body winding up round them and its head resting over their
	 * shoulder, turning slowly; a calm blue ring at their feet. In first person faint.
	 */
	private static void coiled(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float open = ease(age, 0.0F, 10.0F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float k = open * fade * (s.selfView ? 0.3F : 1.0F);
		List<Vector3f> spine = new ArrayList<>();
		int n = 20;
		float turn = time * 0.04F;
		for (int i = 0; i <= n; i++) {
			// head first: from over the shoulder down round the body
			float t = i / (float)n;
			float a = turn + Mth.TWO_PI * 1.6F * t * open;
			spine.add(polar(a, 0.95F + 0.15F * Mth.sin(t * 6.0F + time * 0.1F), 2.3F - 2.0F * t * open));
		}
		glow(ps, c, o -> {
			o.auraDragon(spine, 0.24F, 0.35F + 0.25F * Mth.sin(time * 0.12F), time, argb(0.6F * k, AURA), argb(0.9F * k, PALE), argb(k, EYE));
			o.ring(new Vector3f(0, 0.15F, 0), 0.9F, 1.3F, argb(0.35F * k, AURA));
			for (int i = 0; i < 8; i++) {
				float q = (time * 0.05F + hash(s.seed + i)) % 1.0F;
				o.glint(polar(Mth.TWO_PI * hash(i * 5) + time * 0.05F, 1.0F, 0.2F + 2.0F * q), s.camRight, s.camUp, 0.1F, i, argb(0.8F * k * (1.0F - q), PALE));
			}
		});
	}

	/**
	 * Dragon Dive: over the aimed ground a great dragon of aura circles in the sky, high over the circle its dragons rain on, coming apart into
	 * sparks as they fall; the circle (as wide as the rain falls) glows on the ground with a ring of runes turning on it.
	 */
	private static void dive(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float r = Math.max(1.0F, s.size);
		float open = ease(age, 0.0F, 8.0F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float scatter = ease(age, 16.0F, 50.0F);
		float k = open * fade;
		float sky = Math.min(9.0F, 3.0F + r);
		List<Vector3f> spine = new ArrayList<>();
		int n = 26;
		for (int i = 0; i <= n; i++) {
			float t = i / (float)n;
			float a = time * 0.1F - Mth.TWO_PI * 0.85F * t;
			// a spiral winding down, the head lowest: diving on the circle
			spine.add(polar(a, r * 0.6F * (1.0F - 0.25F * t), sky - 1.8F + 3.6F * t + 0.4F * Mth.sin(t * 6.0F + time * 0.15F)));
		}
		glow(ps, c, o -> {
			o.auraDragon(spine, 1.1F, 0.9F, time, argb(0.65F * k * (1.0F - scatter * 0.85F), AURA), argb(0.9F * k * (1.0F - scatter * 0.7F), PALE),
				argb(k * (1.0F - scatter * 0.5F), EYE));
			o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, r * open - 0.35F), r * open, argb(0.85F * fade, AURA));
			o.ring(new Vector3f(0, 0.16F, 0), Math.max(0.0F, r * open - 0.07F), r * open, argb(0.6F * fade, PALE));
			o.disc(new Vector3f(0, 0.13F, 0), r * open, argb(0.1F * fade, AURA), argb(0.05F * fade, DEEP));
			o.circle(new Vector3f(0, 0.16F, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), r * 0.82F * open, 0.03F, 40, argb(0.5F * fade, AURA));
			for (int i = 0; i < 16; i++) {
				float a = time * 0.02F + Mth.TWO_PI * i / 16;
				o.rune(polar(a, r * 0.91F * open, 0.17F), polar(a + Mth.HALF_PI, 1.0F, 0.0F), polar(a, 1.0F, 0.0F), 0.2F, s.seed + i, 0.025F, argb(0.7F * fade, PALE));
			}
			// sparks shed by the great dragon coming apart
			for (int i = 0; i < 16; i++) {
				float q = (time * 0.05F + hash(s.seed + i)) % 1.0F;
				Vector3f at = new Vector3f(spine.get((int)(hash(i * 7) * n))).add(0, -sky * q * scatter, 0);
				o.glint(at, s.camRight, s.camUp, 0.2F, time * 0.2F + i, argb(0.8F * fade * scatter * (1.0F - q), PALE));
			}
		});
	}

	private DragonLooks() {
	}
}
