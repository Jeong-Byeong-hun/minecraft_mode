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
 * Looks of two knights of the Round Table: Lancelot's Arondight (the overload, the Lady of the Lake, the unblemished knight) and Mordred's
 * Clarent (Clarent Blood Arthur, red lightning, the rebel's charge and the helm that hides her lineage).
 */
final class KnightLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final Vector3f X = new Vector3f(1, 0, 0);
	private static final Vector3f Z = new Vector3f(0, 0, 1);
	private static final int LAKE = 0x3F7CFF;
	private static final int LAKE_PALE = 0xCFE4FF;
	private static final int NAVY = 0x070C24;
	private static final int WATER = 0x10305A;
	private static final int RED = 0xE8202E;
	private static final int RED_PALE = 0xFF9AA0;
	private static final int STEEL = 0xC8CCD4;
	private static final int STEEL_DARK = 0x4A4E58;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case OVERLOAD_AURA -> overloadAura(s, ps, c);
			case ARONDIGHT_ARC -> arondightArc(s, ps, c);
			case LAKE_KNIGHT -> lakeKnight(s, ps, c);
			case UNBLEMISHED -> unblemished(s, ps, c);
			case BLOOD_ARTHUR -> bloodArthur(s, ps, c);
			case RED_BOLT -> redBolt(s, ps, c);
			case RED_CHAIN -> redChain(s, ps, c);
			case REBEL_TRAIL -> rebelTrail(s, ps, c);
			case PEDIGREE_HELM -> pedigreeHelm(s, ps, c);
			default -> {
			}
		}
	}

	// ------------------------------------------------------------ Arondight

	/** Arondight Overload: the lake's blue mana overflowing off the caster in dark streams, blue arcs crackling round them. */
	private static void overloadAura(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.12F);
		float fade = 1.0F - ease(p, 0.75F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		float top = s.selfView ? 0.9F : 2.6F;
		List<List<Vector3f>> arcs = new ArrayList<>();
		int seed = s.seed * 17 + (int)(time / 2);
		for (int i = 0; i < 5; i++) {
			Vector3f a = polar(Mth.TWO_PI * hash(seed + i), 0.55F, 0.2F + Math.min(top, 2.0F) * hash(seed * 3 + i));
			Vector3f b = polar(Mth.TWO_PI * hash(seed * 7 + i), 0.75F, 0.2F + Math.min(top, 2.0F) * hash(seed * 11 + i));
			arcs.add(VfxOut.zigzag(a, b, seed + i, 0.25F, 6));
		}
		shade(ps, c, o -> {
			for (int i = 0; i < 12; i++) {
				float a = Mth.TWO_PI * i / 12 + time * 0.03F;
				Vector3f foot = polar(a, 0.5F, 0.05F);
				float h = Math.min(top, (1.6F + 0.9F * hash(i + (int)(time / 3))) * open);
				o.flame(foot, new Vector3f(-Mth.cos(a) * 0.1F, 1, -Mth.sin(a) * 0.1F), h, 0.6F, time * 0.7F + i, argb(0.75F * k, NAVY), argb(0.0F, NAVY));
			}
		});
		glow(ps, c, o -> {
			for (int i = 0; i < 12; i += 2) {
				float a = Mth.TWO_PI * i / 12 + time * 0.03F;
				Vector3f foot = polar(a, 0.6F, 0.05F);
				float h = Math.min(top, (1.8F + 0.9F * hash(i + (int)(time / 3))) * open);
				o.flame(foot, new Vector3f(-Mth.cos(a) * 0.1F, 1, -Mth.sin(a) * 0.1F), h, 0.7F, time * 0.7F + i, argb(0.45F * k, LAKE), argb(0.0F, LAKE));
			}
			for (List<Vector3f> arc : arcs) {
				o.glowPolyline(arc, 0.06F, argb(0.85F * k, LAKE_PALE));
				o.glowPolyline(arc, 0.18F, argb(0.3F * k, LAKE));
			}
			o.ring(new Vector3f(0, 0.14F, 0), 0.4F, 1.6F, argb(0.55F * k, LAKE));
		});
	}

	/**
	 * One overloaded cut of Arondight: a blue-black band swept across the front (every other cut the other way), a blazing blue edge with a
	 * white line on it, blue arcs crackling off and droplets of light flung.
	 */
	private static void arondightArc(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float age = s.age();
		float half = Math.max(30.0F, s.arg) * Mth.DEG_TO_RAD / 2;
		float r = s.size;
		Vector3f f = new Vector3f(s.forward);
		float swing = ease(age, 0.0F, 3.5F);
		float fade = 1.0F - ease(p, 0.45F, 1.0F);
		boolean flip = (s.seed & 1) == 0;
		int n = 22;
		Vector3f[] inner = new Vector3f[n + 1];
		Vector3f[] outer = new Vector3f[n + 1];
		Vector3f[] edge = new Vector3f[n + 1];
		for (int i = 0; i <= n; i++) {
			float t = (half - 2 * half * swing * i / n) * (flip ? -1 : 1);
			float y = (flip ? -0.6F : 0.7F) + (flip ? 1.3F : -1.3F) * (swing * i / n);
			Vector3f d = new Vector3f(f).rotateY(-t);
			// the white edge just inside the cut's reach and its blue glow fading out over it, so the cut reads as far as it hits
			inner[i] = new Vector3f(d).mul(r * 0.3F).add(0, y, 0);
			outer[i] = new Vector3f(d).mul(r * 0.95F).add(0, y, 0);
			edge[i] = new Vector3f(d).mul(r * 1.05F).add(0, y, 0);
		}
		List<List<Vector3f>> bolts = new ArrayList<>();
		int seed = s.seed * 13 + (int)(s.ageInTicks / 2);
		for (int i = 0; i < 4; i++) {
			int at = Math.min(n, (int)(hash(seed + i) * n * swing));
			Vector3f from = new Vector3f(edge[at]);
			bolts.add(VfxOut.zigzag(from, new Vector3f(BladeLooks.shardDir(seed, i)).mul(0.8F + hash(i * 3)).add(from), seed + i, 0.2F, 5));
		}
		shade(ps, c, o -> {
			for (int i = 0; i < n; i++) {
				float lead = (float)(i + 1) / n;
				float al = fade * (0.3F + 0.65F * lead);
				o.quad(inner[i], outer[i], outer[i + 1], inner[i + 1], argb(0.0F, NAVY), argb(0.85F * al, NAVY), argb(0.85F * al, NAVY), argb(0.0F, NAVY));
			}
		});
		glow(ps, c, o -> {
			for (int i = 0; i < n; i++) {
				float lead = (float)(i + 1) / n;
				float al = fade * (0.25F + 0.7F * lead);
				o.quad(outer[i], edge[i], edge[i + 1], outer[i + 1], argb(0.9F * al, LAKE), argb(0.0F, LAKE), argb(0.0F, LAKE), argb(0.9F * al, LAKE));
				o.bar(outer[i], outer[i + 1], 0.035F, 0.035F, argb(al, 0xFFFFFF), argb(al, 0xFFFFFF));
				o.glowLine(inner[i], outer[i], 0.15F, argb(0.12F * al, LAKE), argb(0.3F * al, LAKE));
			}
			for (List<Vector3f> b : bolts) {
				o.glowPolyline(b, 0.05F, argb(0.9F * fade, LAKE_PALE));
				o.glowPolyline(b, 0.15F, argb(0.3F * fade, LAKE));
			}
			for (int i = 0; i < 8; i++) {
				int at = Math.min(n, (int)(hash(s.seed * 3 + i) * n * swing));
				float q = ease(age, 1.0F + hash(i) * 3.0F, 9.0F);
				Vector3f pt = new Vector3f(edge[at]).mul(1.0F + 0.25F * q).add(0, -0.8F * q * q, 0);
				o.glint(pt, s.camRight, s.camUp, 0.14F, age * 0.3F + i, argb(0.9F * fade * (1.0F - q), LAKE_PALE));
			}
		});
	}

	/**
	 * Knight of the Lake: the lake opens at the caster's feet, dark water with ripples running out and mist rising, and behind them a sword
	 * rises out of it, held up on a sleeve of white samite, water running off the blade.
	 */
	private static void lakeKnight(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.15F);
		float rise = ease(p, 0.05F, 0.35F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f spot = new Vector3f(ahead).mul(-1.0F);
		Vector3f hilt = new Vector3f(spot).add(0, -0.6F + 1.1F * rise, 0);
		Vector3f guard = new Vector3f(hilt).add(0, 0.18F, 0);
		Vector3f tip = new Vector3f(guard).add(0, 1.9F, 0);
		boolean sword = !s.selfView && rise > 0.0F;
		shade(ps, c, o -> {
			o.disc(new Vector3f(0, 0.135F, 0), 1.9F * open, argb(0.6F * k, WATER), argb(0.0F, WATER));
			for (int i = 0; i < 10; i++) {
				float q = (time * 0.02F + hash(s.seed + i)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 7) + q, 0.6F + 1.2F * hash(i * 3), 0.2F + 0.7F * q);
				o.sprite(at, s.camRight, s.camUp, 0.6F + 0.5F * q, argb(0.22F * k * Mth.sin(q * Mth.PI), 0xDDE8F2));
			}
			if (sword) {
				// the sleeve of white samite holding it up out of the water
				o.tube(List.of(new Vector3f(spot).add(0, 0.12F, 0), new Vector3f(spot).add(0, 0.12F + Math.max(0.0F, hilt.y - 0.12F) * 0.5F, 0), new Vector3f(hilt)), 0.11F,
					0.08F, 7, argb(0.92F * fade, 0xF2F4F8), argb(0.92F * fade, 0x8A94A8));
				o.ellipsoid(new Vector3f(hilt).add(0, 0.05F, 0), new Vector3f(right).mul(0.09F), new Vector3f(UP).mul(0.08F), new Vector3f(ahead).mul(0.09F), 4, 8,
					argb(0.95F * fade, 0xF2E0D0), argb(0.95F * fade, 0x8A7060));
				o.blade(guard, tip, right, 0.13F, argb(0.97F * fade, STEEL), argb(0.97F * fade, 0xE8EEF8));
				o.bar(new Vector3f(right).mul(-0.24F).add(guard), new Vector3f(right).mul(0.24F).add(guard), 0.04F, 0.04F, argb(0.97F * fade, 0x2A3A6A),
					argb(0.97F * fade, 0x2A3A6A));
				o.bar(hilt, guard, 0.035F, 0.035F, argb(0.97F * fade, 0x1A2238), argb(0.97F * fade, 0x1A2238));
			}
		});
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			o.ring(ground, 1.75F * open, 1.9F * open, argb(0.6F * k, LAKE_PALE));
			for (int w = 0; w < 3; w++) {
				float q = ((time + w * 7.0F) / 21.0F) % 1.0F;
				o.ring(ground, 0.3F + 1.5F * q, 0.38F + 1.5F * q, argb(0.55F * k * (1.0F - q), LAKE_PALE));
			}
			if (rise > 0.0F && !s.selfView) {
				float wq = ((time) / 14.0F) % 1.0F;
				o.ring(new Vector3f(spot).add(0, 0.15F, 0), 0.1F + 0.9F * wq, 0.16F + 0.9F * wq, argb(0.7F * fade * (1.0F - wq), 0xFFFFFF));
			}
			if (sword) {
				o.glowLine(guard, tip, 0.18F, argb(0.6F * fade, LAKE), argb(0.4F * fade, LAKE_PALE));
				o.star(tip, s.camRight, s.camUp, 0.6F, 4, 0.15F, 0.3F, argb(0.9F * fade, 0xFFFFFF), argb(0.0F, LAKE_PALE));
				for (int i = 0; i < 6; i++) {
					float q = (time * 0.06F + hash(i * 5)) % 1.0F;
					Vector3f at = new Vector3f(guard).lerp(tip, hash(i * 3)).add(new Vector3f(right).mul((hash(i) - 0.5F) * 0.2F)).add(0, -1.6F * q, 0);
					if (at.y > 0.2F) {
						o.glint(at, s.camRight, s.camUp, 0.08F, time * 0.2F + i, argb(0.8F * fade * (1.0F - q), LAKE_PALE));
					}
				}
				o.sprite(new Vector3f(guard).lerp(tip, 0.5F), s.camRight, s.camUp, 1.2F, argb(0.2F * fade, LAKE));
			}
		});
	}

	/**
	 * Unblemished: dark stains flake off the caster's body and turn into motes of light as they rise, a ring of light sweeps up the body as
	 * it is cleansed and a clear white ring settles at the feet.
	 */
	private static void unblemished(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float time = s.ageInTicks;
		int flakes = 22;
		Vector3f[] at = new Vector3f[flakes];
		float[] q = new float[flakes];
		for (int i = 0; i < flakes; i++) {
			float start = 0.05F + 0.35F * hash(s.seed + i);
			q[i] = ease(p, start, start + 0.4F);
			float a = Mth.TWO_PI * hash(i * 7);
			float y = 0.2F + 1.6F * hash(i * 3);
			at[i] = polar(a + q[i] * 0.8F, 0.38F + 0.6F * q[i], y + 1.4F * q[i]);
		}
		float top = s.selfView ? 1.0F : 2.4F;
		shade(ps, c, o -> {
			for (int i = 0; i < flakes; i++) {
				if (q[i] >= 0.6F || s.selfView && at[i].y > top) {
					continue;
				}
				Vector3f d = new Vector3f(Mth.cos(time * 0.3F + i), 0.5F, Mth.sin(time * 0.2F + i)).normalize();
				float size = 0.2F * (1.0F - q[i] / 0.6F) + 0.04F;
				o.diamond(at[i], d, VfxOut.across(d), size, size * 0.7F, argb(0.9F * fade, 0x14141C), argb(0.9F * fade, 0x24242E));
			}
		});
		glow(ps, c, o -> {
			for (int i = 0; i < flakes; i++) {
				if (q[i] < 0.4F || s.selfView && at[i].y > top) {
					continue;
				}
				float k = Mth.clamp((q[i] - 0.4F) / 0.3F, 0.0F, 1.0F) * (1.0F - ease(q[i], 0.8F, 1.0F));
				o.glint(at[i], s.camRight, s.camUp, 0.16F, time * 0.2F + i, argb(0.95F * k * fade, i % 2 == 0 ? 0xFFFFFF : LAKE_PALE));
			}
			float sweep = ease(p, 0.1F, 0.55F);
			if (sweep > 0.0F && sweep < 1.0F) {
				float y = Math.min(top, 2.1F * sweep);
				o.circle(new Vector3f(0, y, 0), X, Z, 0.55F, 0.04F, 24, argb(0.9F * (1.0F - sweep * 0.5F), 0xFFFFFF));
				o.ring(new Vector3f(0, y, 0), 0.4F, 0.8F, argb(0.4F * (1.0F - sweep * 0.5F), LAKE_PALE));
			}
			float settle = ease(p, 0.4F, 0.6F);
			o.ring(new Vector3f(0, 0.14F, 0), 0.5F, 0.5F + 0.8F * settle, argb(0.7F * fade * settle, 0xFFFFFF));
			if (!s.selfView) {
				o.sprite(new Vector3f(0, 1.1F, 0), s.camRight, s.camUp, 1.4F, argb(0.18F * fade * settle, LAKE_PALE));
			}
		});
	}

	// ------------------------------------------------------------ Clarent

	/**
	 * Clarent Blood Arthur: the sword raised while red lightning comes down out of the sky into it, then swung: a torrent of crimson
	 * lightning tears along the beam, bolts crawling all through it, branches cracking into the ground, a flare at its head.
	 */
	private static void bloodArthur(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float len = Math.max(2.0F, s.size);
		Vector3f dir = PendragonLooks.beamDir(s);
		Vector3f side = VfxOut.across(dir);
		Vector3f up = PendragonLooks.beamUp(dir);
		float fade = 1.0F - ease(p, 0.6F, 1.0F);
		float swing = ease(age, 4.0F, 7.0F);
		Vector3f hand = new Vector3f(side).mul(0.35F).add(new Vector3f(dir).mul(0.35F)).add(0, -0.3F, 0);
		Vector3f bladeDir = new Vector3f(UP).lerp(dir, swing).normalize();
		Vector3f tip = new Vector3f(bladeDir).mul(1.6F).add(hand);
		float reach = Math.max(0.0F, len * ease(age, 6.0F, 10.0F) - 0.8F);
		Vector3f start = new Vector3f(dir).mul(0.8F);
		Vector3f front = new Vector3f(dir).mul(0.8F + reach);
		int seed = s.seed * 31 + (int)(time / 2);
		glow(ps, c, o -> {
			o.blade(hand, tip, side, 0.14F, argb(fade, 0xF0F0F4), argb(fade, RED_PALE));
			o.glowLine(hand, tip, 0.14F, argb(0.8F * fade, RED), argb(0.8F * fade, RED));
			if (swing < 1.0F) {
				// red lightning coming down out of the sky into the raised blade
				for (int i = 0; i < 3; i++) {
					Vector3f from = new Vector3f(tip).add((hash(seed + i) - 0.5F) * 6.0F, 9.0F, (hash(seed * 3 + i) - 0.5F) * 6.0F);
					o.bolt(from, tip, seed + i, 0.9F, 10, 0.22F, argb(0.6F * (1.0F - swing), RED), argb(0.95F * (1.0F - swing), 0xFFE0E0));
				}
				o.sprite(tip, s.camRight, s.camUp, 1.0F + 0.5F * hash(seed), argb(0.7F * (1.0F - swing), RED));
			}
			if (reach <= 0.0F) {
				return;
			}
			o.glowLine(start, front, PendragonLooks.BEAM_R * 1.1F, argb(0.25F * fade, RED), argb(0.2F * fade, RED));
			o.glowLine(start, front, 0.5F, argb(0.45F * fade, RED), argb(0.4F * fade, RED));
			// the lit edges where the beam stops hitting, along the four sides of its body
			for (int k = 0; k < 4; k++) {
				Vector3f off = new Vector3f(k < 2 ? up : side).mul((k % 2 == 0 ? 1 : -1) * PendragonLooks.BEAM_R);
				o.glowLine(new Vector3f(start).add(off), new Vector3f(front).add(off), 0.14F, argb(0.4F * fade, RED), argb(0.35F * fade, RED));
			}
			for (int i = 0; i < 7; i++) {
				Vector3f off0 = new Vector3f(side).mul((hash(seed + i * 3) - 0.5F) * 1.6F).add(new Vector3f(up).mul((hash(seed * 5 + i) - 0.5F) * 1.6F));
				Vector3f off1 = new Vector3f(side).mul((hash(seed * 7 + i) - 0.5F) * 1.6F).add(new Vector3f(up).mul((hash(seed * 9 + i) - 0.5F) * 1.6F));
				o.bolt(new Vector3f(start).add(off0), new Vector3f(front).add(off1), seed + i, 0.35F, Math.max(6, (int)(reach / 1.2F)), 0.2F, argb(0.65F * fade, RED),
					argb(0.95F * fade, 0xFFE8E8));
			}
			// branches cracking down into the ground
			for (int i = 0; i < 4; i++) {
				Vector3f from = new Vector3f(dir).mul(0.8F + reach * hash(seed * 11 + i));
				Vector3f to = new Vector3f(from).add(new Vector3f(side).mul((hash(seed * 13 + i) - 0.5F) * 1.2F)).add(0, -1.4F, 0);
				o.bolt(from, to, seed * 3 + i, 0.3F, 5, 0.12F, argb(0.6F * fade, RED), argb(0.9F * fade, 0xFFE0E0));
			}
			float flare = 1.0F - ease(age, 9.0F, 18.0F);
			o.star(front, s.camRight, s.camUp, 1.5F * flare + 0.4F, 10, 0.3F, s.seed, argb(0.9F * flare + 0.1F * fade, RED_PALE), argb(0.0F, RED));
		});
	}

	/** A crimson lightning strike on the aimed ground: a jagged bolt with branches, a red flash, a ring blasting out and cracks of red light. */
	private static void redBolt(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = s.size;
		float time = s.ageInTicks;
		float fade = 1.0F - ease(p, 0.5F, 1.0F);
		int seed = s.seed * 29 + (int)(time / 2);
		glow(ps, c, o -> {
			if (age < 9.0F) {
				Vector3f top = new Vector3f((hash(seed) - 0.5F) * 2.0F, 15.0F, (hash(seed * 3) - 0.5F) * 2.0F);
				o.bolt(top, new Vector3f(0, 0.1F, 0), seed, 1.3F, 14, 0.7F, argb(0.6F, RED), argb(1.0F, 0xFFEAEA));
				for (int i = 0; i < 3; i++) {
					Vector3f from = new Vector3f(top).lerp(new Vector3f(0, 0.1F, 0), 0.3F + 0.4F * hash(seed + i));
					Vector3f to = new Vector3f(from).add((hash(seed * 7 + i) - 0.5F) * 6.0F, -3.0F - 2.0F * hash(i), (hash(seed * 11 + i) - 0.5F) * 6.0F);
					o.bolt(from, to, seed + i * 5, 0.6F, 6, 0.25F, argb(0.5F, RED), argb(0.8F, 0xFFD0D0));
				}
			}
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			float q = ease(age, 0.0F, 10.0F);
			o.ring(ground, Math.max(0.0F, r * q - 0.7F), r * q, argb(0.85F * (1.0F - q), RED));
			o.ring(ground, Math.max(0.0F, r - 0.3F), r, argb(0.6F * fade, RED));
			o.disc(ground, r * 0.8F, argb(0.35F * fade, RED), argb(0.0F, RED));
			for (int i = 0; i < 8; i++) {
				float a = Mth.TWO_PI * i / 8 + hash(s.seed + i);
				o.bolt(ground, polar(a, r * (0.6F + 0.4F * hash(i * 3)), 0.15F), s.seed + i, 0.35F, 5, 0.12F, argb(0.6F * fade, RED), argb(0.9F * fade, RED_PALE));
			}
			float flash = 1.0F - ease(age, 0.0F, 6.0F);
			o.star(new Vector3f(0, 1.0F, 0), s.camRight, s.camUp, r * flash + 0.3F, 8, 0.25F, s.seed, argb(0.95F * flash, RED_PALE), argb(0.0F, RED));
		});
	}

	/**
	 * Red Lightning: crimson bolts leap from the caster's sword to the nearest enemy and on from enemy to enemy (as many as the chain jumps),
	 * thrown anew every tick, each enemy crackling where it is struck. With nobody near, the bolt spends itself in the air ahead.
	 */
	private static void redChain(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.5F, 1.0F);
		float time = s.ageInTicks;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f sword = new Vector3f(ahead).mul(0.7F).add(new Vector3f(right).mul(0.35F)).add(0, 1.3F, 0);
		int seed = s.seed * 19 + (int)time;
		List<Vector3f> hops = new ArrayList<>();
		hops.add(sword);
		List<Vector4f> left = new ArrayList<>(s.targets);
		Vector3f from = new Vector3f(sword);
		for (int i = 0; i < 5 && !left.isEmpty(); i++) {
			Vector4f best = null;
			float bestD = Float.MAX_VALUE;
			for (Vector4f t : left) {
				float d = from.distanceSquared(t.x, t.y + t.w * 0.5F, t.z);
				if (d < bestD) {
					bestD = d;
					best = t;
				}
			}
			// the chain jumps on only to an enemy within 6 blocks of the last one (the first within its range of the caster)
			if (i > 0 && bestD > 36.0F) {
				break;
			}
			left.remove(best);
			from = new Vector3f(best.x, best.y + best.w * 0.5F, best.z);
			hops.add(from);
		}
		if (hops.size() == 1) {
			hops.add(new Vector3f(ahead).mul(6.0F).add(sword).add(0, -0.4F, 0));
		}
		glow(ps, c, o -> {
			for (int i = 0; i + 1 < hops.size(); i++) {
				Vector3f a = hops.get(i);
				Vector3f b = hops.get(i + 1);
				float d = a.distance(b);
				o.bolt(a, b, seed + i, Math.min(1.0F, d * 0.12F), Math.max(4, (int)(d * 1.4F)), 0.22F, argb(0.65F * fade, RED), argb(fade, 0xFFE8E8));
				o.bolt(a, b, seed * 3 + i, Math.min(1.2F, d * 0.15F), Math.max(4, (int)(d * 1.2F)), 0.08F, argb(0.4F * fade, RED), argb(0.6F * fade, RED_PALE));
			}
			for (int i = 1; i < hops.size(); i++) {
				Vector3f at = hops.get(i);
				o.star(at, s.camRight, s.camUp, 0.9F + 0.3F * hash(seed + i), 6, 0.3F, seed + i, argb(0.85F * fade, RED_PALE), argb(0.0F, RED));
				for (int j = 0; j < 3; j++) {
					o.bolt(at, new Vector3f(BladeLooks.shardDir(seed + i, j)).mul(0.7F).add(at), seed + i * 7 + j, 0.15F, 3, 0.06F, argb(0.6F * fade, RED),
						argb(0.9F * fade, 0xFFFFFF));
				}
			}
			o.sprite(sword, s.camRight, s.camUp, 0.5F, argb(0.8F * fade, RED_PALE));
		});
	}

	/** Rebel's Charge: red lightning crackling along the caster's path, branches striking the ground, a scorched line and a red burst where they set off. */
	private static void rebelTrail(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		List<Vector3f> points = VfxOut.path(s.trail);
		if (points.size() < 2) {
			return;
		}
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.35F, 1.0F);
		int n = points.size();
		int last = s.selfView ? Math.max(1, n - 2) : n - 1;
		float length = VfxOut.length(points, last);
		float time = s.ageInTicks;
		int seed = s.seed * 23 + (int)(time / 2);
		List<List<Vector3f>> bolts = new ArrayList<>();
		int steps = Math.max(3, (int)(length / 0.6F));
		for (int k = 0; k < 3; k++) {
			List<Vector3f> bolt = new ArrayList<>();
			for (int i = 0; i <= steps; i++) {
				float t = (float)i / steps;
				Vector3f side = VfxOut.across(PendragonLooks.along(points, last, t));
				bolt.add(VfxOut.along(points, last, t).add(new Vector3f(side).mul((hash(seed * 3 + k * 31 + i) - 0.5F) * 0.8F))
					.add(0, 0.5F + 0.45F * k + (hash(seed * 7 + k * 17 + i) - 0.5F) * 0.5F, 0));
			}
			bolts.add(bolt);
		}
		shade(ps, c, o -> {
			for (int i = 0; i <= steps; i += 2) {
				Vector3f at = VfxOut.along(points, last, (float)i / steps).add(0, 0.135F, 0);
				o.disc(at, 0.5F, argb(0.45F * fade, 0x180608), argb(0.0F, 0x180608));
			}
		});
		glow(ps, c, o -> {
			for (List<Vector3f> b : bolts) {
				o.glowPolyline(b, 0.18F, argb(0.5F * fade, RED));
				o.polyline(b, 0.025F, argb(0.95F * fade, 0xFFE0E0));
			}
			for (int i = 0; i < 4; i++) {
				Vector3f a = VfxOut.along(points, last, hash(seed + i)).add(0, 1.0F, 0);
				o.bolt(a, new Vector3f(a).add((hash(seed * 5 + i) - 0.5F) * 1.2F, -0.9F, (hash(seed * 9 + i) - 0.5F) * 1.2F), seed + i, 0.25F, 4, 0.1F,
					argb(0.6F * fade, RED), argb(0.9F * fade, RED_PALE));
			}
			o.glowPolyline(points.subList(0, last + 1).stream().map(v -> new Vector3f(v).add(0, 0.15F, 0)).toList(), 0.25F, argb(0.4F * fade, RED));
			float burst = ease(p, 0.0F, 0.4F);
			o.ring(new Vector3f(0, 0.15F, 0), 0.3F + 2.0F * burst, 0.8F + 2.0F * burst, argb(0.7F * (1.0F - burst), RED));
			o.star(new Vector3f(0, 1.0F, 0), s.camRight, s.camUp, 1.6F * (1.0F - burst) + 0.1F, 8, 0.25F, s.seed, argb(0.8F * (1.0F - burst), RED_PALE), argb(0.0F, RED));
		});
	}

	/**
	 * Secret of Pedigree: Mordred's horned helm comes down over the caster's head and closes, its visor slit flashing red; then a red haze
	 * swallows their outline and the helm fades with them. In first person only the haze shows.
	 */
	private static void pedigreeHelm(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float time = s.ageInTicks;
		float settle = ease(p, 0.0F, 0.22F);
		float haze = ease(p, 0.25F, 0.5F) * (1.0F - ease(p, 0.85F, 1.0F));
		float helm = ease(p, 0.0F, 0.08F) * (1.0F - ease(p, 0.55F, 0.85F));
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f head = new Vector3f(0, 1.62F + (1.0F - settle) * 1.3F, 0);
		float spin = (1.0F - settle) * 2.5F;
		Vector3f fwd = new Vector3f(ahead).rotateY(spin);
		Vector3f side = VfxOut.across(fwd);
		if (!s.selfView && helm > 0.0F) {
			shade(ps, c, o -> {
				int lit = argb(0.97F * helm, STEEL);
				int dark = argb(0.97F * helm, STEEL_DARK);
				o.ellipsoid(head, new Vector3f(side).mul(0.3F), new Vector3f(UP).mul(0.34F), new Vector3f(fwd).mul(0.33F), 7, 14, lit, dark);
				// the horns sweeping up off the sides
				for (int k = -1; k <= 1; k += 2) {
					Vector3f root = new Vector3f(side).mul(k * 0.26F).add(0, 0.12F, 0).add(head);
					List<Vector3f> horn = List.of(root, new Vector3f(side).mul(k * 0.42F).add(0, 0.25F, 0).add(new Vector3f(fwd).mul(-0.05F)).add(head),
						new Vector3f(side).mul(k * 0.48F).add(0, 0.55F, 0).add(new Vector3f(fwd).mul(-0.15F)).add(head),
						new Vector3f(side).mul(k * 0.42F).add(0, 0.8F, 0).add(new Vector3f(fwd).mul(-0.25F)).add(head));
					o.tube(horn, 0.065F, 0.01F, 6, lit, dark);
				}
				// the crest along the top
				o.tube(List.of(new Vector3f(fwd).mul(0.28F).add(0, 0.22F, 0).add(head), new Vector3f(0, 0.37F, 0).add(head),
					new Vector3f(fwd).mul(-0.3F).add(0, 0.2F, 0).add(head)), 0.04F, 0.025F, 5, argb(0.97F * helm, 0xA8121E), argb(0.97F * helm, 0x4A060C));
			});
		}
		glow(ps, c, o -> {
			if (!s.selfView && helm > 0.0F) {
				Vector3f visor = new Vector3f(fwd).mul(0.335F).add(head).add(0, 0.03F, 0);
				o.bar(new Vector3f(side).mul(-0.17F).add(visor), new Vector3f(side).mul(0.17F).add(visor), 0.022F, 0.022F, argb(helm, RED_PALE), argb(helm, RED_PALE));
				o.bar(visor, new Vector3f(visor).add(0, -0.2F, 0).add(new Vector3f(fwd).mul(-0.02F)), 0.02F, 0.02F, argb(helm, RED_PALE), argb(helm, RED_PALE));
				o.circle(new Vector3f(head).add(0, 0.06F, 0), side, fwd, 0.32F, 0.012F, 20, argb(0.7F * helm, RED));
				float flash = ease(p, 0.18F, 0.22F) * (1.0F - ease(p, 0.22F, 0.35F));
				o.star(visor, s.camRight, s.camUp, 1.2F * flash + 0.01F, 4, 0.12F, 0.0F, argb(0.95F * flash, 0xFFFFFF), argb(0.0F, RED));
			}
			if (haze <= 0.0F) {
				return;
			}
			for (int i = 0; i < 16; i++) {
				float q = (time * 0.05F + hash(s.seed + i)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 7) + q, 0.3F + 0.5F * hash(i * 3), 0.2F + 1.9F * hash(i * 5));
				if (s.selfView && at.y > 1.0F) {
					continue;
				}
				o.sprite(at, s.camRight, s.camUp, 0.5F + 0.4F * q, argb(0.14F * haze * Mth.sin(q * Mth.PI), RED));
			}
			// the outline wavering and coming apart
			for (int i = 0; i < 6; i++) {
				float a = Mth.TWO_PI * i / 6 + time * 0.05F;
				float y0 = 0.2F + 0.3F * hash(i + (int)(time / 3));
				float y1 = Math.min(s.selfView ? 1.0F : 2.0F, y0 + 0.8F + 0.6F * hash(i * 3 + (int)(time / 3)));
				o.glowLine(polar(a, 0.45F, y0), polar(a + 0.15F * Mth.sin(time * 0.3F + i), 0.45F, y1), 0.05F, argb(0.5F * haze, RED), argb(0.0F, RED));
			}
			o.ring(new Vector3f(0, 0.14F, 0), 0.4F, 1.1F, argb(0.4F * haze, RED));
		});
	}

	private KnightLooks() {
	}
}
