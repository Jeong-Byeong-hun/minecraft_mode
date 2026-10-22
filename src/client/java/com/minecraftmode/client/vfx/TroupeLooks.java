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
 * Looks of Chrollo Lucilfer's Skill Hunter (Hunter x Hunter): the Bandit's Secret book and the pages his stolen abilities fly out of, Sun and
 * Moon (the two seals and the blasts they set off) and the Fun Fun Cloth.
 */
final class TroupeLooks {
	private static final int SUN = 0xFFB030;
	private static final int SUN_PALE = 0xFFE8B0;
	private static final int MOON = 0xB8D0FF;
	private static final int MOON_PALE = 0xEEF4FF;
	private static final int INK = 0x1A1410;
	private static final int GOLD = 0xE0C080;
	private static final int GOLD_PALE = 0xFFF0C8;
	private static final int PAGE = 0xEDE2C8;
	private static final int COVER = 0x3A2414;
	private static final int CLOTH_A = 0xEDE4CC;
	private static final int CLOTH_B = 0xB0402A;
	private static final int CLOTH_C = 0x2A3A6A;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case SUN_MOON -> sunAndMoon(s, ps, c);
			case SEAL_CHAIN -> sealChain(s, ps, c);
			case FUN_CLOTH -> funCloth(s, ps, c);
			case BANDIT_BOOK -> banditBook(s, ps, c);
			case PAGE_GATE -> pageGate(s, ps, c);
			default -> {
			}
		}
	}

	/** The sun seal: a disc ringed with rays, facing the camera (glow buffer). */
	private static void sun(final VfxOut o, final State s, final Vector3f at, final float r, final float alpha) {
		o.star(at, s.camRight, s.camUp, r, 12, 0.62F, s.ageInTicks * 0.05F, argb(0.9F * alpha, SUN), argb(0.5F * alpha, SUN));
		o.sprite(at, s.camRight, s.camUp, r * 0.55F, argb(alpha, SUN_PALE));
		o.circle(at, s.camRight, s.camUp, r * 0.55F, 0.02F, 16, argb(alpha, 0xFFFFFF));
	}

	/** The moon seal: a crescent, facing the camera (glow buffer). */
	private static void moon(final VfxOut o, final State s, final Vector3f at, final float r, final float alpha) {
		Vector3f prevOut = null;
		Vector3f prevIn = null;
		int n = 12;
		for (int i = 0; i <= n; i++) {
			float t = Mth.PI * (0.5F + (float)i / n);
			Vector3f out = new Vector3f(s.camRight).mul(Mth.cos(t) * r).add(new Vector3f(s.camUp).mul(Mth.sin(t) * r)).add(at);
			Vector3f in = new Vector3f(s.camRight).mul(Mth.cos(t) * r * 0.55F - r * 0.2F).add(new Vector3f(s.camUp).mul(Mth.sin(t) * r * 0.82F)).add(at);
			if (prevOut != null) {
				o.quad(prevOut, out, in, prevIn, argb(0.95F * alpha, MOON_PALE), argb(0.95F * alpha, MOON_PALE), argb(0.6F * alpha, MOON), argb(0.6F * alpha, MOON));
			}
			prevOut = out;
			prevIn = in;
		}
		o.sprite(at, s.camRight, s.camUp, r * 1.3F, argb(0.3F * alpha, MOON));
	}

	/**
	 * Sun and Moon: a sun seal and a moon seal are stamped on the target and turn round it, closing in; when they touch (as the blasts go
	 * off) they burst in a flash of both colours.
	 */
	private static void sunAndMoon(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float time = s.ageInTicks;
		float h = Math.max(1.0F, s.size);
		float w = Math.max(0.6F, s.arg);
		Vector3f mid = new Vector3f(0, h * 0.55F, 0);
		float meet = 30.0F;
		float close = ease(age, 0.0F, meet);
		float sep = (w * 0.5F + 1.0F) * (1.0F - close);
		float a = time * 0.15F + s.seed;
		Vector3f sunAt = polar(a, sep, 0.0F).add(mid);
		Vector3f moonAt = polar(a + Mth.PI, sep, 0.0F).add(mid);
		float show = ease(age, 0.0F, 4.0F);
		float burst = ease(age, meet, meet + 9.0F);
		shade(ps, c, o -> {
			if (age < meet) {
				o.sprite(sunAt, s.camRight, s.camUp, 0.6F, argb(0.7F * show, INK));
				o.sprite(moonAt, s.camRight, s.camUp, 0.6F, argb(0.7F * show, INK));
			}
		});
		glow(ps, c, o -> {
			if (age < meet) {
				sun(o, s, sunAt, 0.65F * show, show);
				moon(o, s, moonAt, 0.58F * show, show);
				o.glowLine(sunAt, moonAt, 0.06F, argb(0.5F * show, SUN), argb(0.5F * show, MOON));
				o.circle(new Vector3f(0, h * 0.55F, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), sep, 0.02F, 24, argb(0.3F * show, GOLD));
			} else {
				float fade = 1.0F - burst;
				o.star(mid, s.camRight, s.camUp, 1.6F * burst + 0.4F, 12, 0.3F, s.seed, argb(fade, 0xFFFFFF), argb(0.0F, SUN));
				o.circle(mid, s.camRight, s.camUp, 0.4F + 1.4F * burst, 0.06F, 24, argb(0.8F * fade, SUN));
				o.circle(mid, s.camRight, s.camUp, 0.3F + 1.1F * burst, 0.05F, 24, argb(0.8F * fade, MOON));
				o.sprite(mid, s.camRight, s.camUp, 1.4F, argb(0.5F * fade, SUN_PALE));
			}
		});
	}

	/**
	 * Sun and Moon's blasts: from the caster's hand the seals' power leaps to the nearest enemy and on from each to the next within the
	 * chain's jump, a streak half sun and half moon between them and a blast of both on each one struck.
	 */
	private static void sealChain(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.5F, 1.0F);
		float time = s.ageInTicks;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f hand = new Vector3f(ahead).mul(0.6F).add(new Vector3f(right).mul(0.3F)).add(0, 1.3F, 0);
		List<Vector3f> hops = KilluaLooks.hops(s, hand, 4, 6.0F, new Vector3f(ahead).mul(6.0F).add(hand));
		float reach = ease(age, 0.0F, 5.0F) * (hops.size() - 1);
		glow(ps, c, o -> {
			for (int i = 0; i + 1 < hops.size() && i < reach; i++) {
				Vector3f a = hops.get(i);
				Vector3f b = new Vector3f(a).lerp(hops.get(i + 1), Math.min(1.0F, reach - i));
				Vector3f bend = new Vector3f(0, 0.5F, 0);
				Vector3f m = new Vector3f(a).lerp(b, 0.5F).add(bend);
				o.glowLine(a, m, 0.18F, argb(0.6F * fade, SUN), argb(0.6F * fade, SUN));
				o.glowLine(m, b, 0.18F, argb(0.6F * fade, MOON), argb(0.6F * fade, MOON));
				o.bar(a, m, 0.03F, 0.03F, argb(fade, SUN_PALE), argb(fade, 0xFFFFFF));
				o.bar(m, b, 0.03F, 0.03F, argb(fade, 0xFFFFFF), argb(fade, MOON_PALE));
			}
			for (int i = 1; i < hops.size() && i <= reach + 0.01F; i++) {
				Vector3f at = hops.get(i);
				float local = Mth.clamp(age - i * 5.0F / Math.max(1, hops.size() - 1), 0.0F, 16.0F);
				float b = ease(local, 0.0F, 8.0F);
				o.star(at, s.camRight, s.camUp, 0.8F + 0.6F * b, 12, 0.3F, s.seed + i, argb(0.9F * fade * (1.0F - b * 0.6F), 0xFFFFFF), argb(0.0F, SUN));
				o.circle(at, s.camRight, s.camUp, 0.3F + 0.9F * b, 0.05F, 20, argb(0.8F * fade * (1.0F - b), SUN));
				o.circle(at, s.camRight, s.camUp, 0.2F + 0.7F * b, 0.04F, 20, argb(0.8F * fade * (1.0F - b), MOON));
				sun(o, s, new Vector3f(s.camRight).mul(-0.35F).add(at), 0.18F, fade * (1.0F - b));
				moon(o, s, new Vector3f(s.camRight).mul(0.35F).add(at), 0.16F, fade * (1.0F - b));
			}
			o.sprite(hand, s.camRight, s.camUp, 0.35F, argb(0.6F * fade, GOLD_PALE));
		});
	}

	/**
	 * Fun Fun Cloth: a great patterned cloth drapes down over the caster from above, wraps them, then shrinks with them inside into a small
	 * bundle that vanishes in a puff of sparkle. Laid out as cells that never overlap (translucent quads sort by their middles); in first
	 * person faint.
	 */
	private static void funCloth(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float time = s.ageInTicks;
		float drape = ease(age, 0.0F, 8.0F);
		float squash = 1.0F - 0.82F * ease(age, 12.0F, 26.0F);
		float gone = ease(age, 26.0F, 34.0F);
		float alpha = (1.0F - gone) * (s.selfView ? 0.2F : 0.97F);
		int cols = 16;
		int rows = 6;
		float top = 2.2F * squash;
		float low = top * (1.0F - drape);
		float rad = 0.62F * (0.4F + 0.6F * squash);
		Vector3f[][] grid = new Vector3f[rows + 1][cols + 1];
		for (int j = 0; j <= rows; j++) {
			float y = Mth.lerp((float)j / rows, low, top);
			float rr = rad * (j == rows ? 0.85F : 1.0F) * (1.0F + 0.12F * (1.0F - drape) * (1.0F - (float)j / rows));
			for (int i = 0; i <= cols; i++) {
				float a = Mth.TWO_PI * i / cols;
				float ripple = 0.05F * Mth.sin(a * 3.0F + time * 0.4F + j);
				grid[j][i] = polar(a, rr + ripple, y);
			}
		}
		Vector3f peak = new Vector3f(0, top + 0.25F * squash, 0);
		shade(ps, c, o -> {
			if (alpha <= 0.01F) {
				return;
			}
			for (int j = 0; j < rows; j++) {
				for (int i = 0; i < cols; i++) {
					int pick = (i / 2 + j / 2) % 2;
					int base = pick == 0 ? CLOTH_A : CLOTH_B;
					if ((i + j) % 4 == 1 && pick == 0) {
						base = CLOTH_C;
					}
					o.quad(grid[j][i], grid[j][i + 1], grid[j + 1][i + 1], grid[j + 1][i], argb(alpha, base));
				}
			}
			for (int i = 0; i < cols; i++) {
				o.tri(grid[rows][i], grid[rows][i + 1], peak, argb(alpha, i % 2 == 0 ? CLOTH_A : CLOTH_B), argb(alpha, i % 2 == 0 ? CLOTH_A : CLOTH_B),
					argb(alpha, CLOTH_A));
			}
		});
		glow(ps, c, o -> {
			for (int i = 0; i < cols; i++) {
				o.glowLine(grid[0][i], grid[0][i + 1], 0.04F, argb(0.5F * (1.0F - gone), GOLD_PALE), argb(0.5F * (1.0F - gone), GOLD_PALE));
			}
			if (gone > 0.0F) {
				for (int i = 0; i < 14; i++) {
					Vector3f d = BladeLooks.shardDir(s.seed, i);
					Vector3f at = new Vector3f(d).mul(0.3F + 1.2F * gone).add(0, 0.4F, 0);
					o.glint(at, s.camRight, s.camUp, 0.16F, time * 0.3F + i, argb(0.9F * (1.0F - gone), GOLD_PALE));
				}
				o.sprite(new Vector3f(0, 0.4F, 0), s.camRight, s.camUp, 0.8F * (1.0F - gone), argb(0.5F * (1.0F - gone), 0xFFFFFF));
			}
		});
	}

	/**
	 * Bandit's Secret: the Skill Hunter book floats open over the caster's right hand, its dark cover round cream pages, a page turning over
	 * and over, lines of script glowing gold on them and runes rising off it.
	 */
	private static void banditBook(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float open = ease(age, 0.0F, 5.0F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float k = open * fade;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f at = new Vector3f(ahead).mul(0.6F).add(new Vector3f(right).mul(0.35F)).add(0, 1.25F + 0.04F * Mth.sin(time * 0.15F), 0);
		// the spine runs away from the caster; the pages open to either side, tilted back towards them
		Vector3f spine = new Vector3f(ahead).mul(0.8F).add(0, 0.6F, 0).normalize();
		Vector3f across = new Vector3f(right);
		Vector3f normal = new Vector3f(spine).cross(across).normalize();
		if (normal.y < 0.0F) {
			normal.negate();
		}
		float pw = 0.45F;
		float ph = 0.3F;
		float lift = 0.22F * open;
		shade(ps, c, o -> {
			for (int side = -1; side <= 1; side += 2) {
				// each half lifted a little at the outer edge, as an open book
				Vector3f e0 = new Vector3f(at).add(new Vector3f(spine).mul(-ph));
				Vector3f e1 = new Vector3f(at).add(new Vector3f(spine).mul(ph));
				Vector3f outDir = new Vector3f(across).mul(side * pw * open).add(new Vector3f(normal).mul(lift * 0.5F));
				Vector3f o0 = new Vector3f(e0).add(outDir);
				Vector3f o1 = new Vector3f(e1).add(outDir);
				o.quad(e0, e1, o1, o0, argb(0.97F * k, PAGE));
				// the cover's rim round the page (beside it, not under it)
				Vector3f rim = new Vector3f(across).mul(side * 0.06F);
				o.quad(o0, o1, new Vector3f(o1).add(rim), new Vector3f(o0).add(rim), argb(0.97F * k, COVER));
				Vector3f sp = new Vector3f(spine).mul(0.06F);
				o.quad(e1, o1, new Vector3f(o1).add(sp), new Vector3f(e1).add(sp), argb(0.97F * k, COVER));
				o.quad(e0, o0, new Vector3f(o0).sub(sp), new Vector3f(e0).sub(sp), argb(0.97F * k, COVER));
			}
			// the page turning over
			float turn = (time * 0.09F) % 1.0F;
			float ang = Mth.PI * turn;
			Vector3f leaf = new Vector3f(across).mul(Mth.cos(ang) * pw * open).add(new Vector3f(normal).mul(Mth.sin(ang) * pw * 0.9F + lift * 0.5F));
			Vector3f e0 = new Vector3f(at).add(new Vector3f(spine).mul(-ph * 0.96F));
			Vector3f e1 = new Vector3f(at).add(new Vector3f(spine).mul(ph * 0.96F));
			o.quad(e0, e1, new Vector3f(e1).add(leaf), new Vector3f(e0).add(leaf), argb(0.9F * k, 0xF6EEDA));
		});
		glow(ps, c, o -> {
			Vector3f lift2 = new Vector3f(normal).mul(0.02F);
			for (int side = -1; side <= 1; side += 2) {
				for (int line = 0; line < 5; line++) {
					float t = -0.75F + 1.5F * line / 4.0F;
					Vector3f l0 = new Vector3f(at).add(new Vector3f(spine).mul(t * ph)).add(new Vector3f(across).mul(side * pw * 0.15F * open)).add(lift2)
						.add(new Vector3f(normal).mul(lift * 0.08F));
					Vector3f l1 = new Vector3f(l0).add(new Vector3f(across).mul(side * pw * (0.4F + 0.3F * hash(s.seed + line + side * 7)) * open))
						.add(new Vector3f(normal).mul(lift * 0.25F));
					o.glowLine(l0, l1, 0.012F, argb(0.9F * k, 0xC07A10), argb(0.9F * k, 0xC07A10));
				}
			}
			o.sprite(new Vector3f(at).add(new Vector3f(normal).mul(0.15F)), s.camRight, s.camUp, 0.55F, argb(0.35F * k, GOLD));
			for (int i = 0; i < 8; i++) {
				float q = (time * 0.06F + hash(s.seed + i)) % 1.0F;
				Vector3f rise = new Vector3f(at).add(new Vector3f(normal).mul(0.15F + 1.2F * q)).add(new Vector3f(across).mul((hash(i * 7) - 0.5F) * 0.5F))
					.add(new Vector3f(spine).mul((hash(i * 11) - 0.5F) * 0.3F));
				o.rune(rise, s.camRight, s.camUp, 0.08F, s.seed * 3 + i, 0.012F, argb(0.9F * k * (1.0F - q), GOLD_PALE));
			}
		});
	}

	/**
	 * Bandit's Secret: a page torn from the book hangs where a stolen ability flies out, facing its flight, the ability's sigil glowing on it;
	 * it burns away from its edges as the shot leaves.
	 */
	private static void pageGate(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float time = s.ageInTicks;
		Vector3f f = PendragonLooks.beamDir(s);
		Vector3f side = VfxOut.across(f);
		Vector3f up = PendragonLooks.beamUp(f);
		float open = ease(age, 0.0F, 2.0F);
		float burn = ease(age, 4.0F, 14.0F);
		float w = 0.3F * open * (1.0F - burn);
		float h = 0.4F * open * (1.0F - burn);
		Vector3f c0 = new Vector3f(side).mul(-w).add(new Vector3f(up).mul(-h));
		Vector3f c1 = new Vector3f(side).mul(w).add(new Vector3f(up).mul(-h));
		Vector3f c2 = new Vector3f(side).mul(w).add(new Vector3f(up).mul(h));
		Vector3f c3 = new Vector3f(side).mul(-w).add(new Vector3f(up).mul(h));
		shade(ps, c, o -> {
			if (w > 0.01F) {
				o.quad(c0, c1, c2, c3, argb(0.92F, PAGE));
			}
		});
		glow(ps, c, o -> {
			Vector3f front = new Vector3f(f).mul(0.02F);
			if (w > 0.01F) {
				o.glowLine(c0, c1, 0.05F, argb(0.8F, SUN), argb(0.8F, SUN));
				o.glowLine(c1, c2, 0.05F, argb(0.8F, SUN), argb(0.8F, SUN));
				o.glowLine(c2, c3, 0.05F, argb(0.8F, SUN), argb(0.8F, SUN));
				o.glowLine(c3, c0, 0.05F, argb(0.8F, SUN), argb(0.8F, SUN));
				o.circle(front, side, up, w * 0.75F, 0.015F, 16, argb(0.95F, 0xC07A10));
				o.rune(front, side, up, w * 0.5F, s.seed, 0.015F, argb(0.95F, 0xC07A10));
			}
			float flash = 1.0F - ease(age, 0.0F, 6.0F);
			o.sprite(new Vector3f(), s.camRight, s.camUp, 0.5F * flash + 0.1F, argb(0.7F * flash, GOLD_PALE));
			for (int i = 0; i < 5; i++) {
				float q = (time * 0.1F + hash(s.seed + i)) % 1.0F;
				o.glint(new Vector3f(BladeLooks.shardDir(s.seed, i)).mul(0.2F + 0.4F * burn).add(0, 0.3F * q, 0), s.camRight, s.camUp, 0.07F, i,
					argb(0.9F * burn * (1.0F - q), SUN_PALE));
			}
		});
	}

	private TroupeLooks() {
	}
}
