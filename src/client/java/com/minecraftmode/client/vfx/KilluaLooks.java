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
import org.joml.Vector4f;

/**
 * Looks of Killua's Godspeed (Hunter x Hunter): Speed of Lightning (the streak and his afterimages), Whirlwind (the electricity crackling
 * over him), Thunderbolt (the lightning coming down on the aimed spot) and Narukami (lightning leaping from enemy to enemy).
 */
final class KilluaLooks {
	private static final int BLUE = 0x4AA8FF;
	private static final int PALE = 0xD8F0FF;
	private static final int WHITE = 0xFFFFFF;
	private static final int SCORCH = 0x0C1018;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case GODSPEED_STEP -> speedOfLightning(s, ps, c);
			case GODSPEED_AURA -> whirlwind(s, ps, c);
			case THUNDERBOLT -> thunderbolt(s, ps, c);
			case NARUKAMI -> narukami(s, ps, c);
			default -> {
			}
		}
	}

	/**
	 * The points a chain of hits leaps along (Narukami, Purple Lightning, the Sun and Moon blasts): {@code from}, then the enemy nearest to it
	 * within the look's size, then on {@code jumps} times in all to the next nearest within {@code hop} of the last one struck; a point
	 * {@code fallback} ahead when there is no enemy.
	 */
	static List<Vector3f> hops(final State s, final Vector3f from, final int jumps, final float hop, final Vector3f fallback) {
		List<Vector3f> hops = new ArrayList<>();
		hops.add(new Vector3f(from));
		List<Vector4f> left = new ArrayList<>();
		for (Vector4f t : s.targets) {
			if (t.x * t.x + t.z * t.z <= s.size * s.size) {
				left.add(t);
			}
		}
		Vector3f at = new Vector3f(from);
		for (int i = 0; i < jumps && !left.isEmpty(); i++) {
			Vector4f best = null;
			float bestD = Float.MAX_VALUE;
			for (Vector4f t : left) {
				float d = at.distanceSquared(t.x, t.y + t.w * 0.5F, t.z);
				if (d < bestD) {
					bestD = d;
					best = t;
				}
			}
			if (i > 0 && bestD > hop * hop) {
				break;
			}
			left.remove(best);
			at = new Vector3f(best.x, best.y + best.w * 0.5F, best.z);
			hops.add(at);
		}
		if (hops.size() == 1) {
			hops.add(fallback);
		}
		return hops;
	}

	/**
	 * One of Killua's afterimages at {@code foot}, running towards {@code dir}: a figure of blue-white light leaning into the run, arms swept
	 * back, legs in stride and his spiky hair standing up off the head; faded by {@code alpha}.
	 */
	private static void afterimage(final VfxOut o, final State s, final Vector3f foot, final Vector3f dir, final float alpha, final int seed) {
		Vector3f side = VfxOut.across(dir);
		Vector3f hip = new Vector3f(foot).add(0, 0.85F, 0);
		Vector3f neck = new Vector3f(foot).add(0, 1.45F, 0).add(new Vector3f(dir).mul(0.3F));
		Vector3f head = new Vector3f(neck).add(0, 0.22F, 0).add(new Vector3f(dir).mul(0.06F));
		int body = argb(0.45F * alpha, BLUE);
		int core = argb(0.85F * alpha, PALE);
		o.glowLine(hip, neck, 0.16F, body, body);
		o.bar(hip, neck, 0.04F, 0.04F, core, core);
		for (int k = -1; k <= 1; k += 2) {
			Vector3f shoulder = new Vector3f(neck).add(new Vector3f(side).mul(k * 0.18F)).add(0, -0.05F, 0);
			Vector3f hand = new Vector3f(shoulder).add(new Vector3f(dir).mul(-0.55F)).add(new Vector3f(side).mul(k * 0.12F)).add(0, -0.3F, 0);
			o.glowLine(shoulder, hand, 0.1F, body, argb(0.1F * alpha, BLUE));
			float stride = k > 0 ? 0.45F : -0.5F;
			Vector3f knee = new Vector3f(hip).add(new Vector3f(dir).mul(stride * 0.75F)).add(new Vector3f(side).mul(k * 0.08F)).add(0, -0.4F, 0);
			Vector3f sole = new Vector3f(foot).add(new Vector3f(dir).mul(stride)).add(new Vector3f(side).mul(k * 0.1F)).add(0, k > 0 ? 0.05F : 0.3F, 0);
			o.glowLine(hip, knee, 0.11F, body, body);
			o.glowLine(knee, sole, 0.1F, body, argb(0.15F * alpha, BLUE));
		}
		o.sprite(head, s.camRight, s.camUp, 0.2F, argb(0.6F * alpha, PALE));
		// the spiky white hair
		for (int i = 0; i < 7; i++) {
			float a = -1.3F + 2.6F * i / 6.0F;
			Vector3f out = new Vector3f(side).mul(Mth.sin(a) * 0.32F).add(0, Mth.cos(a) * 0.3F + 0.05F, 0).add(new Vector3f(dir).mul(-0.12F - 0.08F * hash(seed + i)));
			o.blade(new Vector3f(head).add(new Vector3f(out).mul(0.4F)), new Vector3f(head).add(out), dir, 0.05F, core, argb(0.0F, PALE));
		}
	}

	/**
	 * Speed of Lightning: where the caster stood a flash; a jagged streak of blue-white lightning runs to where they appear behind the target,
	 * three afterimages of the run fading along it, a scorched line on the ground, and lightning crackling round them as they land.
	 */
	private static void speedOfLightning(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float flash = 1.0F - ease(age, 0.0F, 10.0F);
		float fade = 1.0F - ease(p, 0.45F, 1.0F);
		Vector3f end = ShinobiLooks.arrival(s);
		Vector3f start = new Vector3f(0, 1.0F, 0);
		Vector3f to = new Vector3f(end).add(0, 1.0F, 0);
		Vector3f run = new Vector3f(end.x, 0, end.z);
		Vector3f dir = run.lengthSquared() < 1.0E-4F ? new Vector3f(s.forward) : new Vector3f(run).normalize();
		float dist = to.distance(start);
		int seed = s.seed * 31 + (int)(time / 2);
		shade(ps, c, o -> {
			if (dist > 0.5F) {
				o.bar(new Vector3f(0, 0.13F, 0), new Vector3f(end).add(0, 0.13F, 0), 0.12F, 0.12F, argb(0.45F * fade, SCORCH), argb(0.45F * fade, SCORCH));
			}
		});
		glow(ps, c, o -> {
			if (dist > 0.5F) {
				int steps = Math.max(5, (int)(dist * 1.3F));
				o.bolt(start, to, seed, 0.6F, steps, 0.32F, argb(0.6F * fade, BLUE), argb(fade, WHITE));
				o.bolt(start, to, seed * 3 + 1, 0.9F, steps, 0.1F, argb(0.4F * fade, BLUE), argb(0.7F * fade, PALE));
				for (int i = 0; i < 3; i++) {
					float t = 0.25F + 0.25F * i;
					Vector3f foot = new Vector3f(end).mul(t);
					afterimage(o, s, foot, dir, fade * (0.35F + 0.25F * i), s.seed + i * 7);
				}
				o.glowLine(new Vector3f(0, 0.14F, 0), new Vector3f(end).add(0, 0.14F, 0), 0.3F, argb(0.3F * fade, BLUE), argb(0.3F * fade, BLUE));
			}
			if (flash > 0.0F) {
				o.star(start, s.camRight, s.camUp, 1.1F * flash + 0.1F, 8, 0.2F, s.seed, argb(0.85F * flash, WHITE), argb(0.0F, BLUE));
			}
			// crackling round the caster where they land
			for (int i = 0; i < 6; i++) {
				Vector3f from = new Vector3f(to).add(BladeLooks.shardDir(seed, i).mul(0.25F));
				Vector3f tip = new Vector3f(to).add(BladeLooks.shardDir(seed + 5, i).mul(0.8F + 0.4F * hash(seed + i)));
				o.bolt(from, tip, seed + i * 11, 0.18F, 4, 0.07F, argb(0.6F * fade, BLUE), argb(0.95F * fade, WHITE));
			}
			o.sprite(to, s.camRight, s.camUp, 0.9F, argb(0.3F * fade, BLUE));
		});
	}

	/**
	 * Whirlwind: Godspeed: a flash rolls out over the ground as it starts, then blue-white electricity crackles over the caster's body for as
	 * long as it lasts, arcs snapping down to the ground round their feet and sparks leaping off (in first person only the arcs at the feet).
	 */
	private static void whirlwind(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float open = ease(age, 0.0F, 6.0F);
		float fade = 1.0F - ease(p, 0.85F, 1.0F);
		float k = open * fade;
		float burst = ease(age, 0.0F, 8.0F);
		int seed = s.seed * 17 + (int)(time / 2);
		glow(ps, c, o -> {
			if (burst < 1.0F) {
				o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, 2.4F * burst - 0.5F), 2.4F * burst, argb(0.75F * (1.0F - burst), PALE));
			}
			if (!s.selfView) {
				o.wall(new Vector3f(), 0.5F, 0.0F, 1.95F, argb(0.14F * k, BLUE), argb(0.0F, BLUE));
				for (int i = 0; i < 8; i++) {
					float a0 = Mth.TWO_PI * hash(seed + i);
					float y0 = 0.2F + 1.6F * hash(seed * 3 + i);
					float a1 = a0 + (hash(seed * 5 + i) - 0.5F) * 2.4F;
					float y1 = Mth.clamp(y0 + (hash(seed * 7 + i) - 0.5F) * 1.0F, 0.1F, 2.0F);
					o.bolt(polar(a0, 0.42F, y0), polar(a1, 0.5F, y1), seed + i * 13, 0.18F, 4, 0.08F, argb(0.6F * k, BLUE), argb(0.95F * k, WHITE));
				}
				// the hair standing up with the charge
				for (int i = 0; i < 3; i++) {
					float a = Mth.TWO_PI * hash(seed * 11 + i);
					Vector3f from = polar(a, 0.15F, 1.8F);
					o.bolt(from, polar(a, 0.35F, 2.25F + 0.2F * hash(seed + i * 3)), seed * 2 + i, 0.08F, 3, 0.05F, argb(0.5F * k, BLUE), argb(0.9F * k, WHITE));
				}
			}
			float self = s.selfView ? 0.5F : 1.0F;
			for (int i = 0; i < 5; i++) {
				float a = Mth.TWO_PI * hash(seed * 7 + i);
				Vector3f from = polar(a, 0.4F, 0.3F + 0.8F * hash(seed * 9 + i));
				Vector3f to = polar(a + 0.3F, 1.0F + 0.6F * hash(seed * 13 + i), 0.08F);
				o.bolt(from, to, seed * 5 + i, 0.2F, 5, 0.07F, argb(0.55F * k * self, BLUE), argb(0.9F * k * self, WHITE));
			}
			for (int i = 0; i < 8; i++) {
				float q = (time * 0.09F + hash(s.seed + i)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 5) + time * 0.1F, 0.6F + 0.5F * q, 0.2F + 1.8F * q);
				o.glint(at, s.camRight, s.camUp, 0.1F, time * 0.3F + i, argb(0.85F * k * self * (1.0F - q), PALE));
			}
		});
	}

	/**
	 * Thunderbolt: lightning comes down from the sky on the aimed spot, branching on the way, a pillar of light where it lands; a lit ring
	 * marks the edge of what it hits and arcs race over the ground out to it, leaving the ground scorched.
	 */
	private static void thunderbolt(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = Math.max(0.5F, s.size);
		float time = s.ageInTicks;
		float strike = 1.0F - ease(age, 2.0F, 12.0F);
		float fade = 1.0F - ease(p, 0.55F, 1.0F);
		float grow = ease(age, 0.0F, 4.0F);
		int seed = s.seed * 13 + (int)(time / 2);
		Vector3f ground = new Vector3f(0, 0.1F, 0);
		Vector3f top = new Vector3f(0.5F, 14.0F, 0.3F);
		shade(ps, c, o -> {
			for (int i = 0; i < 9; i++) {
				float a = Mth.TWO_PI * i / 9 + hash(s.seed + i);
				o.bar(polar(a, 0.3F, 0.12F), polar(a, r * (0.55F + 0.35F * hash(s.seed * 3 + i)), 0.12F), 0.1F, 0.02F, argb(0.5F * fade, SCORCH), argb(0.0F, SCORCH));
			}
		});
		glow(ps, c, o -> {
			if (strike > 0.0F) {
				o.bolt(top, ground, seed, 1.2F, 16, 0.75F, argb(0.5F * strike, BLUE), argb(strike, WHITE));
				o.bolt(top, ground, seed * 7 + 3, 0.8F, 12, 0.25F, argb(0.5F * strike, PALE), argb(0.8F * strike, WHITE));
				for (int i = 0; i < 4; i++) {
					float t = 0.3F + 0.15F * i;
					Vector3f from = new Vector3f(top).lerp(ground, t);
					Vector3f to = new Vector3f(from).add(BladeLooks.shardDir(seed, i).mul(2.5F)).add(0, -1.5F, 0);
					o.bolt(from, to, seed + i * 17, 0.5F, 6, 0.12F, argb(0.45F * strike, BLUE), argb(0.8F * strike, WHITE));
				}
				o.glowLine(ground, new Vector3f(0, 7.0F, 0), r * 0.45F, argb(0.3F * strike, BLUE), argb(0.0F, BLUE));
				o.disc(new Vector3f(0, 0.13F, 0), r, argb(0.4F * strike, PALE), argb(0.1F * strike, BLUE));
				o.star(new Vector3f(0, 0.8F, 0), s.camRight, s.camUp, r * 0.8F * strike + 0.2F, 10, 0.25F, s.seed, argb(0.9F * strike, WHITE), argb(0.0F, BLUE));
			}
			// the edge of what it hits
			o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, r * grow - 0.3F), r * grow, argb(0.85F * fade, PALE));
			o.ring(new Vector3f(0, 0.16F, 0), Math.max(0.0F, r * grow - 0.08F), r * grow, argb(0.6F * fade, WHITE));
			for (int i = 0; i < 10; i++) {
				float a = Mth.TWO_PI * i / 10 + hash(seed + i) * 0.5F;
				float flick = hash(seed * 3 + i) > 0.35F ? 1.0F : 0.3F;
				Vector3f tip = polar(a, r * (0.88F + 0.12F * hash(seed * 5 + i)) * grow, 0.15F);
				o.bolt(polar(a + 0.2F, 0.3F, 0.15F), tip, seed + i * 7, 0.35F, 6, 0.1F, argb(0.6F * fade * flick, BLUE), argb(0.9F * fade * flick, WHITE));
			}
			for (int i = 0; i < 10; i++) {
				float q = (time * 0.07F + hash(s.seed + i * 3)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 7), r * hash(i * 11), 0.2F + 2.5F * q);
				o.glint(at, s.camRight, s.camUp, 0.14F, time * 0.2F + i, argb(0.85F * fade * (1.0F - q), PALE));
			}
		});
	}

	/**
	 * Narukami: lightning thrown from the caster's hand leaps to the nearest enemy and on to the next within the chain's jump (six at most), a
	 * thick jagged bolt between each, a flash and arcs on each one struck, the hand still crackling.
	 */
	private static void narukami(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.5F, 1.0F);
		float time = s.ageInTicks;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f hand = new Vector3f(ahead).mul(0.7F).add(new Vector3f(right).mul(0.35F)).add(0, 1.25F, 0);
		List<Vector3f> hops = hops(s, hand, 6, 6.0F, new Vector3f(ahead).mul(7.0F).add(hand).add(0, -0.4F, 0));
		int seed = s.seed * 19 + (int)(time / 2);
		float reach = ease(age, 0.0F, 4.0F) * (hops.size() - 1);
		glow(ps, c, o -> {
			for (int i = 0; i + 1 < hops.size(); i++) {
				if (i >= reach) {
					break;
				}
				Vector3f a = hops.get(i);
				Vector3f b = new Vector3f(a).lerp(hops.get(i + 1), Math.min(1.0F, reach - i));
				float d = a.distance(b);
				int steps = Math.max(4, (int)(d * 1.4F));
				o.bolt(a, b, seed + i, Math.min(1.0F, d * 0.12F), steps, 0.38F, argb(0.55F * fade, BLUE), argb(fade, WHITE));
				o.bolt(a, b, seed * 3 + i, Math.min(1.3F, d * 0.16F), steps, 0.12F, argb(0.45F * fade, BLUE), argb(0.7F * fade, PALE));
			}
			for (int i = 1; i < hops.size(); i++) {
				if (i > reach + 0.01F) {
					break;
				}
				Vector3f at = hops.get(i);
				o.star(at, s.camRight, s.camUp, 1.0F + 0.3F * hash(seed + i), 8, 0.25F, seed + i, argb(0.9F * fade, WHITE), argb(0.0F, BLUE));
				o.sprite(at, s.camRight, s.camUp, 1.1F, argb(0.3F * fade, BLUE));
				for (int j = 0; j < 4; j++) {
					o.bolt(at, new Vector3f(BladeLooks.shardDir(seed + i, j)).mul(0.9F).add(at), seed + i * 7 + j, 0.2F, 3, 0.07F, argb(0.6F * fade, BLUE),
						argb(0.9F * fade, WHITE));
				}
			}
			o.sprite(hand, s.camRight, s.camUp, 0.6F, argb(0.85F * fade, PALE));
			for (int j = 0; j < 5; j++) {
				o.bolt(hand, new Vector3f(BladeLooks.shardDir(seed, j + 9)).mul(0.55F).add(hand), seed + j * 13, 0.14F, 3, 0.06F, argb(0.7F * fade, BLUE),
					argb(0.95F * fade, WHITE));
			}
		});
	}

	private KilluaLooks() {
	}
}
