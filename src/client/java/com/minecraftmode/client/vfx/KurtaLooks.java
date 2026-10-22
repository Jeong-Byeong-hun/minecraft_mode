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
 * Looks of Kurapika's Emperor Time (Hunter x Hunter): the chains of his right hand, one on each finger (the Holy Chain's cross, the Dowsing
 * Chain's weight, Chain Jail's hook, the Steal Chain's needle and the Judgment Chain's blade), and the Kurta clan's scarlet eyes.
 */
final class KurtaLooks {
	private static final int STEEL = 0xC8CCD6;
	private static final int STEEL_DARK = 0x4A4C56;
	private static final int RED = 0xFF2A2A;
	private static final int RED_PALE = 0xFFB0A8;
	private static final int SCARLET = 0xFF1A10;
	private static final int EYE_DARK = 0x1A0204;
	/** The chain on each finger: thumb to little finger. */
	private static final int HOLY = 0;
	private static final int DOWSING = 1;
	private static final int JAIL = 2;
	private static final int STEAL = 3;
	private static final int JUDGMENT = 4;
	private static final float LINK = 0.3F;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case JUDGMENT_CHAIN -> judgmentChain(s, ps, c);
			case STEAL_CHAIN -> stealChain(s, ps, c);
			case CHAIN_JAIL -> chainJail(s, ps, c);
			case SCARLET_EYES -> scarletEyes(s, ps, c);
			default -> {
			}
		}
	}

	/**
	 * The tip of the chain {@code kind}, its point at {@code at} facing {@code dir}, about {@code size} long: the Holy Chain's cross, the
	 * Dowsing Chain's pointed weight, Chain Jail's hook, the Steal Chain's syringe and the Judgment Chain's blade. Shade buffer.
	 */
	private static void tip(final VfxOut o, final int kind, final Vector3f at, final Vector3f dir, final float size, final float alpha) {
		Vector3f d = new Vector3f(dir).normalize();
		Vector3f side = VfxOut.across(d);
		Vector3f base = new Vector3f(d).mul(-size).add(at);
		int steel = argb(0.97F * alpha, STEEL);
		int dark = argb(0.97F * alpha, STEEL_DARK);
		switch (kind) {
			case HOLY -> {
				o.bar(base, at, size * 0.09F, size * 0.09F, steel, steel);
				Vector3f mid = new Vector3f(base).lerp(at, 0.62F);
				o.bar(new Vector3f(side).mul(-size * 0.32F).add(mid), new Vector3f(side).mul(size * 0.32F).add(mid), size * 0.09F, size * 0.09F, steel, steel);
			}
			case DOWSING -> {
				Vector3f mid = new Vector3f(base).lerp(at, 0.4F);
				Vector3f up = new Vector3f(d).cross(side).normalize();
				o.ellipsoid(mid, new Vector3f(side).mul(size * 0.22F), new Vector3f(d).mul(size * 0.3F), new Vector3f(up).mul(size * 0.22F), 4, 8, steel, dark);
				o.bar(new Vector3f(base).lerp(at, 0.6F), at, size * 0.12F, size * 0.01F, steel, steel);
			}
			case JAIL -> {
				List<Vector3f> hook = new ArrayList<>();
				for (int i = 0; i <= 6; i++) {
					float t = i / 6.0F;
					float a = t * Mth.PI * 1.1F;
					hook.add(new Vector3f(d).mul(-size + size * 0.5F * Mth.sin(a) + size * 0.5F).add(new Vector3f(side).mul(size * 0.3F * (1.0F - Mth.cos(a)))).add(base)
						.sub(new Vector3f(d).mul(-size * 0.0F)));
				}
				o.tube(hook, size * 0.07F, size * 0.04F, 5, steel, dark);
				o.blade(hook.getLast(), new Vector3f(hook.getLast()).add(new Vector3f(d).mul(-size * 0.3F)), side, size * 0.06F, steel, steel);
			}
			case STEAL -> {
				Vector3f barrelEnd = new Vector3f(base).lerp(at, 0.55F);
				o.bar(base, barrelEnd, size * 0.11F, size * 0.11F, argb(0.85F * alpha, 0xE8ECF4), argb(0.85F * alpha, 0xE8ECF4));
				o.bar(new Vector3f(d).mul(-size * 0.12F).add(base), base, size * 0.03F, size * 0.03F, dark, dark);
				o.bar(new Vector3f(side).mul(-size * 0.16F).add(base), new Vector3f(side).mul(size * 0.16F).add(base), size * 0.025F, size * 0.025F, dark, dark);
				o.bar(barrelEnd, at, size * 0.02F, size * 0.006F, steel, steel);
			}
			default -> o.blade(base, at, side, size * 0.16F, steel, argb(0.97F * alpha, 0xFFFFFF));
		}
	}

	/** A chain along {@code points} (each stretch its own links), steel on the shade buffer. */
	private static void chainAlong(final VfxOut o, final List<Vector3f> points, final float turn, final int color) {
		for (int i = 0; i + 1 < points.size(); i++) {
			o.chain(points.get(i), points.get(i + 1), LINK, turn + i * Mth.HALF_PI, color);
		}
	}

	/** A soft red glow along {@code points}, so the chain reads at night. */
	private static void glowAlong(final VfxOut o, final List<Vector3f> points, final float width, final int color) {
		for (int i = 0; i + 1 < points.size(); i++) {
			o.glowLine(points.get(i), points.get(i + 1), width, color, color);
		}
	}

	/**
	 * Judgment Chain: the chain on the little finger shoots out from the hand along the beam, the blade on its end leading; where it stops a
	 * loop of chain closes round the heart of what it struck and a scarlet flash. Faint red lines and rings run along it as wide as the beam
	 * hits ({@link PendragonLooks#BEAM_R}).
	 */
	private static void judgmentChain(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float len = Math.max(2.0F, s.size);
		Vector3f dir = PendragonLooks.beamDir(s);
		Vector3f side = VfxOut.across(dir);
		Vector3f up = PendragonLooks.beamUp(dir);
		float fade = 1.0F - ease(p, 0.6F, 1.0F);
		float shoot = ease(age, 0.0F, 5.0F);
		Vector3f hand = new Vector3f(side).mul(0.3F).add(new Vector3f(up).mul(-0.25F)).add(new Vector3f(dir).mul(0.3F));
		Vector3f front = new Vector3f(dir).mul(0.5F + (len - 0.5F) * shoot);
		float r = PendragonLooks.BEAM_R;
		Vector3f heart = new Vector3f(front).add(new Vector3f(dir).mul(-0.9F));
		float bind = ease(age, 5.0F, 10.0F);
		List<Vector3f> loop = new ArrayList<>();
		for (int i = 0; i <= 10; i++) {
			float a = Mth.TWO_PI * i / 10 + time * 0.05F;
			loop.add(new Vector3f(side).mul(Mth.cos(a) * 0.5F * bind).add(new Vector3f(up).mul(Mth.sin(a) * 0.5F * bind)).add(heart));
		}
		shade(ps, c, o -> {
			o.chain(hand, new Vector3f(front).add(new Vector3f(dir).mul(-0.75F)), LINK, time * 0.05F, argb(0.97F * fade, STEEL));
			tip(o, JUDGMENT, front, dir, 0.8F, fade);
			if (bind > 0.05F) {
				chainAlong(o, loop, 0.0F, argb(0.97F * fade, STEEL));
			}
		});
		glow(ps, c, o -> {
			o.glowLine(hand, front, 0.2F, argb(0.5F * fade, RED), argb(0.5F * fade, RED));
			if (bind > 0.05F) {
				glowAlong(o, loop, 0.12F, argb(0.6F * fade, RED));
				float beat = 0.8F + 0.2F * Mth.sin(time * 0.8F);
				o.sprite(heart, s.camRight, s.camUp, 0.55F * beat, argb(0.75F * fade, SCARLET));
				o.sprite(heart, s.camRight, s.camUp, 0.2F, argb(0.9F * fade, RED_PALE));
			}
			float flash = 1.0F - ease(age, 4.0F, 12.0F);
			if (shoot >= 1.0F && flash > 0.0F) {
				o.star(front, s.camRight, s.camUp, 1.3F * flash + 0.2F, 6, 0.25F, s.seed, argb(0.9F * flash, RED_PALE), argb(0.0F, RED));
			}
			// how wide the beam reaches
			Vector3f from = new Vector3f(dir).mul(0.8F);
			if (front.length() > 0.9F) {
				for (int k = 0; k < 4; k++) {
					Vector3f off = new Vector3f(k < 2 ? up : side).mul((k % 2 == 0 ? 1 : -1) * r);
					o.glowLine(new Vector3f(from).add(off), new Vector3f(front).add(off), 0.07F, argb(0.35F * fade, RED), argb(0.35F * fade, RED));
				}
				for (int i = 0; i < 5; i++) {
					float d = 0.8F + ((i / 5.0F + time * 0.03F) % 1.0F) * (front.length() - 0.8F);
					o.circle(new Vector3f(dir).mul(d), side, up, r, 0.03F, 20, argb(0.45F * fade, RED_PALE));
				}
			}
		});
	}

	/**
	 * Steal Chain: chains lash out of the caster's right hand into every enemy in reach, their needle tips stuck in, sparks of stolen aura
	 * running back along them; a red ring on the ground marks how far it reaches. With nobody near, the chains lash out to it and back.
	 */
	private static void stealChain(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float r = Math.max(1.0F, s.size);
		float fade = 1.0F - ease(p, 0.65F, 1.0F);
		float out = ease(age, 0.0F, 5.0F);
		float grow = ease(age, 0.0F, 6.0F);
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f hand = new Vector3f(ahead).mul(0.5F).add(new Vector3f(right).mul(0.4F)).add(0, 1.2F, 0);
		List<Vector3f> ends = new ArrayList<>();
		for (Vector4f t : s.targets) {
			if (t.x * t.x + t.z * t.z <= r * r && ends.size() < 6) {
				ends.add(new Vector3f(t.x, t.y + t.w * 0.55F, t.z));
			}
		}
		boolean none = ends.isEmpty();
		if (none) {
			float lash = Mth.sin(Mth.PI * Math.min(1.0F, age / 16.0F));
			for (int i = 0; i < 5; i++) {
				float a = (float)Mth.atan2(ahead.z, ahead.x) + (i - 2) * 0.6F;
				ends.add(polar(a, Math.max(0.6F, (r - 0.4F) * lash), 1.0F));
			}
		}
		List<Vector3f> tips = new ArrayList<>();
		for (Vector3f e : ends) {
			tips.add(none ? e : new Vector3f(hand).lerp(e, out));
		}
		shade(ps, c, o -> {
			for (int i = 0; i < tips.size(); i++) {
				Vector3f t = tips.get(i);
				Vector3f d = new Vector3f(t).sub(hand);
				if (d.lengthSquared() < 0.04F) {
					continue;
				}
				d.normalize();
				o.chain(hand, new Vector3f(t).add(new Vector3f(d).mul(-0.45F)), LINK * 0.85F, time * 0.04F + i, argb(0.97F * fade, STEEL));
				tip(o, STEAL, new Vector3f(t).add(new Vector3f(d).mul(0.15F)), d, 0.6F, fade);
			}
		});
		glow(ps, c, o -> {
			o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, r * grow - 0.3F), r * grow, argb(0.7F * fade, RED));
			o.ring(new Vector3f(0, 0.16F, 0), Math.max(0.0F, r * grow - 0.06F), r * grow, argb(0.5F * fade, RED_PALE));
			o.sprite(hand, s.camRight, s.camUp, 0.35F, argb(0.6F * fade, RED));
			for (int i = 0; i < tips.size(); i++) {
				Vector3f t = tips.get(i);
				o.glowLine(hand, t, 0.14F, argb(0.45F * fade, RED), argb(0.45F * fade, RED));
				if (!none && out >= 1.0F) {
					o.sprite(t, s.camRight, s.camUp, 0.45F, argb(0.5F * fade, SCARLET));
					// the stolen aura running back to the hand
					for (int j = 0; j < 3; j++) {
						float q = (time * 0.08F + hash(s.seed + i * 3 + j)) % 1.0F;
						o.glint(new Vector3f(t).lerp(hand, q), s.camRight, s.camUp, 0.14F, time * 0.3F + j, argb(0.9F * fade, RED_PALE));
					}
				}
			}
		});
	}

	/**
	 * Chain Jail: chains rise out of the ground on six posts round the aimed spot, as wide as the field it holds, and arch over it into a cage,
	 * a chain winding round the middle binding whatever is caught; a red ring on the ground marks the edge and streaks of light pull in to the
	 * middle.
	 */
	private static void chainJail(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float r = Math.max(1.0F, s.size);
		float rise = ease(age, 0.0F, 10.0F);
		float wind = ease(age, 8.0F, 18.0F);
		float fade = 1.0F - ease(p, 0.85F, 1.0F);
		float h = 1.2F + r * 0.55F;
		int posts = 6;
		List<List<Vector3f>> arcs = new ArrayList<>();
		for (int k = 0; k < posts; k++) {
			float a = Mth.TWO_PI * k / posts + hash(s.seed) * 0.5F;
			List<Vector3f> arc = new ArrayList<>();
			int n = 7;
			for (int i = 0; i <= n; i++) {
				float th = Mth.HALF_PI * i / n * rise;
				arc.add(polar(a, r * Mth.cos(th), 0.15F + h * Mth.sin(th)));
			}
			arcs.add(arc);
		}
		List<Vector3f> coil = new ArrayList<>();
		int turns = 16;
		for (int i = 0; i <= turns; i++) {
			float t = i / (float)turns;
			if (t > wind) {
				break;
			}
			coil.add(polar(time * 0.03F + t * Mth.TWO_PI * 2.2F, 0.85F, 0.35F + 1.5F * t));
		}
		shade(ps, c, o -> {
			for (List<Vector3f> arc : arcs) {
				chainAlong(o, arc, 0.0F, argb(0.97F * fade, STEEL));
				Vector3f base = arc.getFirst();
				o.blade(new Vector3f(base).add(0, -0.05F, 0), new Vector3f(base).add(0, 0.6F, 0).sub(new Vector3f(base.x, 0, base.z).normalize().mul(-0.1F)),
					VfxOut.across(new Vector3f(base.x, 0, base.z)), 0.12F, argb(0.97F * fade, STEEL_DARK), argb(0.97F * fade, STEEL));
			}
			if (coil.size() > 1) {
				chainAlong(o, coil, 0.0F, argb(0.97F * fade, STEEL));
			}
		});
		glow(ps, c, o -> {
			o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, r - 0.3F), r, argb(0.8F * fade * Math.max(0.3F, rise), RED));
			o.ring(new Vector3f(0, 0.16F, 0), Math.max(0.0F, r - 0.06F), r, argb(0.6F * fade, RED_PALE));
			o.disc(new Vector3f(0, 0.13F, 0), r, argb(0.12F * fade, RED), argb(0.04F * fade, RED));
			for (List<Vector3f> arc : arcs) {
				glowAlong(o, arc, 0.12F, argb(0.4F * fade, RED));
			}
			if (coil.size() > 1) {
				glowAlong(o, coil, 0.12F, argb(0.45F * fade, RED));
			}
			// the pull: streaks of light running in over the ground
			for (int i = 0; i < 10; i++) {
				float q = (time * 0.06F + hash(s.seed + i)) % 1.0F;
				float a = Mth.TWO_PI * hash(i * 7 + 3);
				Vector3f from = polar(a, r * (1.0F - q), 0.2F);
				Vector3f to = polar(a, Math.max(0.0F, r * (1.0F - q) - 0.8F), 0.2F);
				o.glowLine(from, to, 0.08F, argb(0.6F * fade * (1.0F - q), RED_PALE), argb(0.0F, RED));
			}
		});
	}

	/**
	 * Scarlet Eyes: Emperor Time: the Kurta clan's eyes burn scarlet over the caster, the five chains of the right hand swirl round them from
	 * the fingers, each with its own tip, and a red aura rises at their feet. In first person only the chains, faded.
	 */
	private static void scarletEyes(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float open = ease(age, 0.0F, 8.0F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float k = open * fade;
		float self = s.selfView ? 0.3F : 1.0F;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f hand = new Vector3f(ahead).mul(0.4F).add(new Vector3f(right).mul(0.45F)).add(0, 1.1F, 0);
		List<List<Vector3f>> chains = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			List<Vector3f> chain = new ArrayList<>();
			chain.add(new Vector3f(hand));
			float a0 = (float)Mth.atan2(hand.z, hand.x);
			for (int j = 1; j <= 6; j++) {
				float t = j / 6.0F * open;
				// each finger's chain swings out on its own orbit, one above the other
				float a = a0 + t * (2.2F + 0.5F * i) + time * (0.05F + 0.015F * i) * (i % 2 == 0 ? 1 : -1);
				float y = 1.1F + (i - 2) * 0.48F * Math.min(1.0F, t * 2.0F) + 0.12F * Mth.sin(time * 0.1F + i + t * 3.0F);
				chain.add(polar(a, 0.6F + 0.75F * Math.min(1.0F, t * 1.5F), y));
			}
			chains.add(chain);
		}
		Vector3f eyes = new Vector3f(0, 2.75F + 0.05F * Mth.sin(time * 0.1F), 0);
		shade(ps, c, o -> {
			for (int i = 0; i < 5; i++) {
				List<Vector3f> chain = chains.get(i);
				chainAlong(o, chain, 0.0F, argb(0.97F * fade * self, STEEL));
				Vector3f last = chain.getLast();
				Vector3f d = new Vector3f(last).sub(chain.get(chain.size() - 2));
				if (d.lengthSquared() > 1.0E-4F) {
					tip(o, i, new Vector3f(last).add(new Vector3f(d).normalize().mul(0.45F)), d, 0.45F, fade * self);
				}
			}
			if (!s.selfView) {
				for (int e = -1; e <= 1; e += 2) {
					almond(o, new Vector3f(s.camRight).mul(e * 0.3F).add(eyes), s, 0.22F * open, e, argb(0.9F * k, EYE_DARK));
				}
			}
		});
		glow(ps, c, o -> {
			for (List<Vector3f> chain : chains) {
				glowAlong(o, chain, 0.07F, argb(0.35F * k * self, RED));
			}
			o.sprite(hand, s.camRight, s.camUp, 0.35F, argb(0.6F * k * self, RED));
			if (!s.selfView) {
				for (int e = -1; e <= 1; e += 2) {
					Vector3f at = new Vector3f(s.camRight).mul(e * 0.3F).add(eyes).add(new Vector3f(s.camRight).cross(s.camUp).mul(-0.02F));
					o.sprite(at, s.camRight, s.camUp, 0.1F * open, argb(k, SCARLET));
					o.circle(at, s.camRight, s.camUp, 0.085F * open, 0.012F, 14, argb(k, 0xFF6040));
					o.sprite(at, s.camRight, s.camUp, 0.3F * open, argb(0.45F * k, SCARLET));
					o.glint(new Vector3f(at).add(new Vector3f(s.camRight).mul(-0.03F)).add(new Vector3f(s.camUp).mul(0.03F)), s.camRight, s.camUp, 0.05F, 0.3F,
						argb(0.9F * k, 0xFFFFFF));
				}
				o.sprite(eyes, s.camRight, s.camUp, 0.9F, argb(0.2F * k, SCARLET));
			}
			for (int i = 0; i < 12; i++) {
				float a = Mth.TWO_PI * i / 12 + hash(s.seed + i);
				o.flame(polar(a, 0.55F, 0.05F), new Vector3f(0, 1, 0), (0.4F + 0.5F * hash(i * 3 + (int)(time / 4))) * k, 0.25F, time * 0.6F + i, argb(0.45F * k * self, RED),
					argb(0.0F, RED_PALE));
			}
		});
	}

	/** An almond-shaped eye {@code r} across round {@code c} facing the camera, the {@code side} eye of two (its outer corner lifted). */
	private static void almond(final VfxOut o, final Vector3f c, final State s, final float r, final int side, final int color) {
		if (r < 0.01F) {
			return;
		}
		Vector3f prev = null;
		Vector3f prevLow = null;
		int n = 10;
		for (int i = 0; i <= n; i++) {
			float t = -1.0F + 2.0F * i / n;
			float lift = 0.25F * t * side;
			float h = r * 0.48F * (1.0F - t * t);
			Vector3f top = new Vector3f(s.camRight).mul(t * r).add(new Vector3f(s.camUp).mul(h + lift * r)).add(c);
			Vector3f low = new Vector3f(s.camRight).mul(t * r).add(new Vector3f(s.camUp).mul(-h * 0.8F + lift * r)).add(c);
			if (prev != null) {
				o.quad(prev, top, low, prevLow, color);
			}
			prev = top;
			prevLow = low;
		}
	}

	private KurtaLooks() {
	}
}
