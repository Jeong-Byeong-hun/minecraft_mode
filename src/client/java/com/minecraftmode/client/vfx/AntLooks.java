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

/**
 * Looks of Beru's Talons (Solo Leveling): the ant king's rampage (claw rakes along the charge, claws slashing all round), Devour's mandibles,
 * the healing he stole and his wings.
 */
final class AntLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final int VIOLET = 0xB14EFF;
	private static final int VIOLET_PALE = 0xE6CCFF;
	private static final int CHITIN_LIT = 0x4A3868;
	private static final int CHITIN_DARK = 0x0A0612;
	private static final int INK = 0x07040C;
	private static final int HEAL = 0x7DFF9A;
	private static final int HEAL_PALE = 0xDFFFE6;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case ANT_DASH -> antDash(s, ps, c);
			case ANT_FRENZY -> antFrenzy(s, ps, c);
			case DEVOUR -> devour(s, ps, c);
			case STOLEN_HEALING -> stolenHealing(s, ps, c);
			case WING_BURST -> wingBurst(s, ps, c);
			default -> {
			}
		}
	}

	/** Ticks since a leaping holder came down (from its trail: the end of the last fall), or -1 while it is still in the air. */
	static float landed(final State s) {
		List<Vector3f> raw = s.trail;
		int n = raw.size();
		for (int i = n - 1; i >= 1; i--) {
			if (raw.get(i).y < raw.get(i - 1).y - 0.01F) {
				return i == n - 1 ? -1.0F : n - 1 - i;
			}
		}
		return s.age() > 14.0F ? 99.0F : -1.0F;
	}

	/** Where a leaping holder came down: the trail point at the end of its last fall (the holder when it never fell). */
	static Vector3f landing(final State s) {
		List<Vector3f> raw = s.trail;
		int n = raw.size();
		for (int i = n - 1; i >= 1; i--) {
			if (raw.get(i).y < raw.get(i - 1).y - 0.01F) {
				return new Vector3f(raw.get(i));
			}
		}
		return n == 0 ? new Vector3f() : new Vector3f(raw.getLast());
	}

	/**
	 * Three parallel claw marks {@code length} long centred on {@code c} along {@code dir}, curving a little towards {@code bend}, {@code gap}
	 * apart: a dark gash with a glowing edge each (on the buffer {@code o} is).
	 */
	private static void claws(final VfxOut o, final Vector3f c, final Vector3f dir, final Vector3f bend, final float length, final float gap, final float show,
		final int body, final int edge) {
		Vector3f d = new Vector3f(dir).normalize();
		Vector3f b = new Vector3f(bend).sub(new Vector3f(d).mul(d.dot(bend))).normalize();
		for (int k = -1; k <= 1; k++) {
			Vector3f prev = null;
			int steps = 8;
			for (int i = 0; i <= steps; i++) {
				float t = (float)i / steps * show;
				Vector3f q = new Vector3f(d).mul((t - 0.5F) * length).add(new Vector3f(b).mul(k * gap + length * 0.12F * Mth.sin(Mth.PI * t))).add(c);
				if (prev != null) {
					float w = 0.07F * Mth.sin(Mth.PI * t) + 0.015F;
					if (o.glow) {
						o.glowLine(prev, q, w * 2.0F, edge, edge);
					} else {
						o.bar(prev, q, w, w, body, body);
					}
				}
				prev = q;
			}
		}
	}

	/**
	 * Ant King's Rampage, the charge: Beru's claws rake the air along the caster's path, sets of three dark gashes crossing it from one side
	 * of the dash's reach to the other, their edges glowing violet, chitin flakes and smoke left behind.
	 */
	private static void antDash(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		List<Vector3f> points = VfxOut.path(s.trail);
		if (points.size() < 2) {
			return;
		}
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.45F, 1.0F);
		int n = points.size();
		int last = s.selfView ? Math.max(1, n - 2) : n - 1;
		float length = VfxOut.length(points, last);
		int sets = Math.max(1, Math.min(8, (int)(length / 1.4F)));
		// the dash hits 1.2 blocks round its path at chest height: each rake spans that
		float reach = 1.2F;
		shade(ps, c, o -> {
			for (int i = 0; i < sets; i++) {
				float t = (i + 0.5F) / sets;
				Vector3f at = VfxOut.along(points, last, t).add(0, 1.0F, 0);
				Vector3f dir = PendragonLooks.along(points, last, t);
				Vector3f side = VfxOut.across(dir);
				Vector3f slant = new Vector3f(side).mul(i % 2 == 0 ? 1 : -1).add(0, 0.55F, 0).add(new Vector3f(dir).mul(0.2F));
				claws(o, at, slant, dir, reach * 2.3F, 0.2F, 1.0F, argb(0.92F * fade, INK), 0);
			}
			for (int i = 0; i < 10; i++) {
				float t = hash(s.seed + i * 3);
				Vector3f at = VfxOut.along(points, last, t).add(0, 0.4F + 0.9F * hash(i * 5), 0);
				o.sprite(at, s.camRight, s.camUp, 0.35F + 0.3F * hash(i), argb(0.3F * fade, 0x140A1E));
			}
		});
		glow(ps, c, o -> {
			for (int i = 0; i < sets; i++) {
				float t = (i + 0.5F) / sets;
				Vector3f at = VfxOut.along(points, last, t).add(0, 1.0F, 0);
				Vector3f dir = PendragonLooks.along(points, last, t);
				Vector3f side = VfxOut.across(dir);
				Vector3f slant = new Vector3f(side).mul(i % 2 == 0 ? 1 : -1).add(0, 0.55F, 0).add(new Vector3f(dir).mul(0.2F));
				claws(o, at, slant, dir, reach * 2.3F, 0.2F, 1.0F, 0, argb(0.75F * fade, VIOLET));
			}
			List<Vector3f> line = new ArrayList<>();
			for (int i = 0; i <= last; i++) {
				line.add(new Vector3f(points.get(i)).add(0, 1.0F, 0));
			}
			o.glowPolyline(line, reach * 0.6F, argb(0.12F * fade, VIOLET));
			for (int i = 0; i < 12; i++) {
				float t = hash(s.seed * 7 + i);
				Vector3f at = VfxOut.along(points, last, t).add(BladeLooks.shardDir(s.seed, i).mul(0.6F)).add(0, 1.0F, 0);
				o.glint(at, s.camRight, s.camUp, 0.12F, i, argb(0.8F * fade, VIOLET_PALE));
			}
		});
	}

	/**
	 * Ant King's Rampage, the frenzy: three of Beru's claw strikes tear round the caster at once, each a fan of three curved gashes running
	 * out to the edge of the blast, a violet ring on it and chitin dust thrown up.
	 */
	private static void antFrenzy(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float r = s.size;
		float sweep = ease(age, 0.0F, 3.0F);
		float fade = 1.0F - ease(age, 3.0F, 9.0F);
		float turn = Mth.TWO_PI * hash(s.seed);
		shade(ps, c, o -> {
			for (int k = 0; k < 3; k++) {
				float a = turn + Mth.TWO_PI * k / 3;
				Vector3f out = polar(a, 1.0F, 0.0F);
				Vector3f tang = polar(a + Mth.HALF_PI, 1.0F, 0.0F);
				Vector3f at = new Vector3f(out).mul(r * 0.62F).add(0, 0.7F + 0.5F * hash(s.seed + k), 0);
				Vector3f dir = new Vector3f(out).add(0, (k % 2 == 0 ? 0.35F : -0.35F), 0).add(new Vector3f(tang).mul(0.3F));
				claws(o, at, dir, tang, r * 0.75F, 0.22F, sweep, argb(0.9F * fade, INK), 0);
			}
		});
		glow(ps, c, o -> {
			for (int k = 0; k < 3; k++) {
				float a = turn + Mth.TWO_PI * k / 3;
				Vector3f out = polar(a, 1.0F, 0.0F);
				Vector3f tang = polar(a + Mth.HALF_PI, 1.0F, 0.0F);
				Vector3f at = new Vector3f(out).mul(r * 0.62F).add(0, 0.7F + 0.5F * hash(s.seed + k), 0);
				Vector3f dir = new Vector3f(out).add(0, (k % 2 == 0 ? 0.35F : -0.35F), 0).add(new Vector3f(tang).mul(0.3F));
				claws(o, at, dir, tang, r * 0.75F, 0.22F, sweep, 0, argb(0.85F * fade, VIOLET));
			}
			o.ring(new Vector3f(0, 0.14F, 0), Math.max(0.0F, r - 0.35F), r, argb(0.65F * fade, VIOLET));
			float wave = ease(age, 0.0F, 5.0F);
			o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, r * wave - 0.6F), r * wave, argb(0.5F * (1.0F - wave), VIOLET_PALE));
			for (int i = 0; i < 10; i++) {
				Vector3f d = BladeLooks.shardDir(s.seed, i);
				d.y = Math.abs(d.y) * 0.6F + 0.2F;
				Vector3f at = new Vector3f(d).mul(0.6F + r * 0.6F * sweep).add(0, 0.5F, 0);
				o.glint(at, s.camRight, s.camUp, 0.12F, i, argb(0.8F * fade, VIOLET_PALE));
			}
		});
	}

	/**
	 * Devour: the ant king's great mandibles, dark chitin with a jagged inner edge, open wide either side of the target and snap shut on it;
	 * where they meet a violet flash, and the life they tear out flows back towards the caster in motes.
	 */
	private static void devour(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float h = Math.max(1.0F, s.size);
		float w = Math.max(0.6F, s.arg);
		float time = s.ageInTicks;
		float open = ease(age, 0.0F, 3.0F);
		float snap = ease(age, 3.0F, 5.5F);
		float fade = 1.0F - ease(p, 0.6F, 1.0F);
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		float y = h * 0.5F;
		float span = w * 0.5F + 0.9F;
		List<List<Vector3f>> jaws = new ArrayList<>();
		for (int k = -1; k <= 1; k += 2) {
			Vector3f root = new Vector3f(f).mul(-(w * 0.5F + 1.1F)).add(new Vector3f(side).mul(k * 0.55F)).add(0, y, 0);
			// opened: swung out wide; shut: curled round the target, the tips crossing in front of it
			float swing = (1.0F - snap) * open * 1.0F;
			List<Vector3f> pts = new ArrayList<>();
			for (int i = 0; i <= 9; i++) {
				float t = i / 9.0F;
				float a = (0.15F + 1.35F * t) * (1.0F - swing * 0.55F);
				Vector3f q = new Vector3f(f).mul(Mth.sin(a) * span * 1.25F).add(new Vector3f(side).mul(k * (Mth.cos(a) * span * (1.0F + swing) - span * 0.55F)));
				pts.add(q.add(root).add(0, 0.12F * Mth.sin(Mth.PI * t), 0));
			}
			jaws.add(pts);
		}
		int lit = argb(0.97F * fade * open, CHITIN_LIT);
		int dark = argb(0.97F * fade * open, CHITIN_DARK);
		shade(ps, c, o -> {
			for (int j = 0; j < 2; j++) {
				List<Vector3f> pts = jaws.get(j);
				o.tube(pts, 0.1F + h * 0.05F, 0.02F, 7, lit, dark);
				int k = j == 0 ? -1 : 1;
				for (int i = 2; i < pts.size() - 1; i += 2) {
					Vector3f along = new Vector3f(pts.get(i + 1)).sub(pts.get(i - 1));
					Vector3f in = new Vector3f(along).cross(UP).normalize().mul(-k);
					o.blade(pts.get(i), new Vector3f(pts.get(i)).add(new Vector3f(in).mul(0.22F)), UP, 0.05F, lit, argb(0.97F * fade * open, 0x8A7AA8));
				}
			}
		});
		glow(ps, c, o -> {
			for (List<Vector3f> pts : jaws) {
				o.glowPolyline(pts, 0.08F, argb(0.45F * fade * open, VIOLET));
			}
			if (snap > 0.6F) {
				float flash = 1.0F - ease(age, 5.0F, 11.0F);
				Vector3f mid = new Vector3f(0, y, 0);
				o.star(mid, s.camRight, s.camUp, h * 0.6F * flash + 0.1F, 6, 0.25F, s.seed, argb(0.9F * flash, VIOLET_PALE), argb(0.0F, VIOLET));
				for (int i = 0; i < 12; i++) {
					float q = (time * 0.06F + hash(s.seed + i)) % 1.0F;
					Vector3f at = new Vector3f(f).mul(-q * 3.0F).add(new Vector3f(side).mul((hash(i * 3) - 0.5F) * w * (1.0F - q))).add(0, y + 0.3F * Mth.sin(q * 6.0F + i), 0);
					o.sprite(at, s.camRight, s.camUp, 0.1F, argb(0.85F * fade, i % 3 == 0 ? 0xFF4060 : VIOLET_PALE));
				}
			}
		});
	}

	/**
	 * Stolen Healing Arts: the healing Beru took from a healer: a green circle opens under the caster, a ring of healing light washes out to
	 * the edge of its reach and stays there, and green and violet motes rise round the allies it touches.
	 */
	private static void stolenHealing(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = s.size;
		float time = s.ageInTicks;
		float open = ease(age, 0.0F, 6.0F);
		float fade = 1.0F - ease(p, 0.7F, 1.0F);
		float k = open * fade;
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			o.magicCircle(ground, new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), 1.6F * open, time * 0.03F, s.seed, 6, argb(0.8F * k, HEAL), argb(0.6F * k, VIOLET));
			float wave = ease(age, 0.0F, 14.0F);
			o.ring(new Vector3f(0, 0.16F, 0), Math.max(0.0F, r * wave - 0.9F), r * wave, argb(0.6F * (1.0F - wave * 0.6F) * fade, HEAL));
			o.ring(new Vector3f(0, 0.14F, 0), Math.max(0.0F, r - 0.3F), r, argb(0.55F * k * wave, HEAL));
			o.wall(new Vector3f(), 0.9F, 0.0F, 2.2F, argb(0.2F * k, HEAL), argb(0.0F, HEAL));
			for (int i = 0; i < 18; i++) {
				float q = (time * 0.035F + hash(s.seed + i * 3)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 7), 0.3F + 0.8F * hash(i * 11), q * 2.6F);
				if (s.selfView && at.y > 1.2F) {
					continue;
				}
				o.glint(at, s.camRight, s.camUp, 0.13F, time * 0.1F + i, argb(0.85F * k * Mth.sin(Mth.PI * q), i % 3 == 0 ? VIOLET_PALE : HEAL_PALE));
			}
		});
	}

	/**
	 * Wing Burst: two pairs of the ant king's wings spread from the caster's back while they fly, beating fast, violet veins glowing in
	 * them; where they land a crash of claws: a violet shock ring out to the slam's reach, cracks and dust.
	 */
	private static void wingBurst(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.82F, 1.0F);
		float time = s.ageInTicks;
		float since = landed(s);
		Vector3f holder = s.trail.isEmpty() ? new Vector3f() : new Vector3f(s.trail.getLast());
		float spread = ease(s.age(), 0.0F, 3.0F) * (since < 0.0F ? 1.0F : 1.0F - ease(since, 0.0F, 8.0F));
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		float r = s.size;
		Vector3f land = landing(s);
		shade(ps, c, o -> {
			if (spread > 0.02F && !s.selfView) {
				for (int k = -1; k <= 1; k += 2) {
					for (int pair = 0; pair < 2; pair++) {
						float beat = Mth.sin(time * 2.4F + pair * 1.3F) * 0.3F;
						Vector3f root = new Vector3f(holder).add(new Vector3f(ahead).mul(-0.25F)).add(new Vector3f(right).mul(k * 0.12F)).add(0, 1.35F - 0.12F * pair, 0);
						Vector3f out = new Vector3f(right).mul(k * Mth.cos(beat)).add(0, Mth.sin(beat) + (pair == 0 ? 0.3F : -0.2F), 0).add(new Vector3f(ahead).mul(pair == 0 ? -0.75F : -0.95F));
						o.insectWing(root, out, new Vector3f(ahead).mul(-1).add(0, -0.3F, 0), (pair == 0 ? 1.6F : 1.25F) * spread, 0.42F * spread,
							argb(0.4F * fade, 0x2A1A40), 0);
					}
				}
			}
			if (since >= 0.0F) {
				float q = ease(since, 0.0F, 12.0F);
				o.disc(new Vector3f(land).add(0, 0.13F, 0), r * 0.7F, argb(0.5F * fade * (1.0F - q * 0.5F), 0x140A20), argb(0.0F, 0x140A20));
			}
		});
		glow(ps, c, o -> {
			if (spread > 0.02F && !s.selfView) {
				for (int k = -1; k <= 1; k += 2) {
					for (int pair = 0; pair < 2; pair++) {
						float beat = Mth.sin(time * 2.4F + pair * 1.3F) * 0.3F;
						Vector3f root = new Vector3f(holder).add(new Vector3f(ahead).mul(-0.25F)).add(new Vector3f(right).mul(k * 0.12F)).add(0, 1.35F - 0.12F * pair, 0);
						Vector3f out = new Vector3f(right).mul(k * Mth.cos(beat)).add(0, Mth.sin(beat) + (pair == 0 ? 0.3F : -0.2F), 0).add(new Vector3f(ahead).mul(pair == 0 ? -0.75F : -0.95F));
						o.insectWing(root, out, new Vector3f(ahead).mul(-1).add(0, -0.3F, 0), (pair == 0 ? 1.6F : 1.25F) * spread, 0.42F * spread, 0,
							argb(0.8F * fade, VIOLET));
					}
				}
			}
			if (since < 0.0F) {
				return;
			}
			float impact = ease(since, 0.0F, 10.0F);
			Vector3f g = new Vector3f(land).add(0, 0.15F, 0);
			o.ring(g, Math.max(0.0F, r * impact - 0.6F), r * impact, argb(0.85F * (1.0F - impact), VIOLET_PALE));
			o.ring(new Vector3f(land).add(0, 0.14F, 0), Math.max(0.0F, r - 0.3F), r, argb(0.6F * fade * (1.0F - ease(since, 10.0F, 30.0F)), VIOLET));
			float flash = 1.0F - ease(since, 0.0F, 5.0F);
			o.star(new Vector3f(land).add(0, 0.6F, 0), s.camRight, s.camUp, r * 0.5F * flash + 0.1F, 6, 0.25F, s.seed, argb(0.8F * flash, VIOLET_PALE), argb(0.0F, VIOLET));
			for (int i = 0; i < 8; i++) {
				float a = Mth.TWO_PI * i / 8 + hash(s.seed + i);
				Vector3f to = polar(a, r * 0.85F * ease(since, 0.0F, 4.0F), 0.0F).add(g);
				o.bolt(g, to, s.seed + i, 0.35F, 6, 0.08F, argb(0.5F * fade, VIOLET), argb(0.8F * fade, VIOLET_PALE));
			}
		});
	}

	private AntLooks() {
	}
}
