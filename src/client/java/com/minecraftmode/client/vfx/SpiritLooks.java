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
 * Spiritual looks: Hollow reiatsu, the Hogyoku, Kyoka Suigetsu's mirror, Kurohitsugi, dust to dust, Itosabaki's cloth, Mugetsu's
 * dark dome and Zabimaru's bones.
 */
final class SpiritLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final int BONE = 0xEFE6D2;
	private static final int BONE_GLOW = 0xFF3030;
	private static final int VIOLET = 0xB08AFF;

	/**
	 * Hollow reiatsu: black spiritual pressure flaring up round the caster in tongues, edged in red, a red ring on the ground. Seen
	 * from inside (first person) only the ring and a low flicker show.
	 */
	static void reiatsu(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float flare = ease(p, 0.0F, 0.12F);
		float fade = 1.0F - ease(p, 0.6F, 1.0F);
		float k = flare * fade;
		boolean dark = s.dark();
		int tone = dark ? 0xE0242E : s.color;
		float tall = s.selfView ? 0.9F : 2.8F;
		float inside = s.selfView ? 0.4F : 1.0F;
		int n = 20;
		if (dark) {
			shade(ps, c, o -> {
				for (int i = 0; i < n; i++) {
					float a = Mth.TWO_PI * i / n;
					float h = tall * (0.6F + 0.4F * hash(i + s.seed)) * k;
					o.flame(polar(a, 0.75F + 0.15F * hash(i * 3), 0.0F), new Vector3f(Mth.cos(a) * 0.15F, 1, Mth.sin(a) * 0.15F), h, 0.75F, s.ageInTicks * 0.7F + i * 1.7F,
						argb(0.8F * k * inside, 0x080206), argb(0.0F, 0x080206));
				}
			});
		}
		glow(ps, c, o -> {
			o.ring(new Vector3f(0, 0.15F, 0), 0.6F, 1.5F + 1.0F * flare, argb(0.7F * k, tone));
			for (int i = 0; i < n; i++) {
				float a = Mth.TWO_PI * (i + 0.5F) / n;
				float h = tall * (0.5F + 0.5F * hash(i * 5 + s.seed)) * k;
				o.flame(polar(a, 0.95F, 0.0F), new Vector3f(Mth.cos(a) * 0.2F, 1, Mth.sin(a) * 0.2F), h, 0.5F, s.ageInTicks * 0.8F + i * 2.1F,
					argb(0.75F * k * inside, tone), argb(0.0F, tone));
			}
			// pressure rising off it in streaks
			if (!s.selfView) {
				for (int i = 0; i < 12; i++) {
					float a = Mth.TWO_PI * hash(i * 7 + s.seed);
					float q = (s.ageInTicks * 0.06F + hash(i * 13)) % 1.0F;
					Vector3f from = polar(a, 0.6F + 0.4F * hash(i), q * 3.2F);
					o.glowLine(from, new Vector3f(from).add(0, 0.6F, 0), 0.05F, argb(0.0F, tone), argb(0.8F * k * (1 - q), VfxDraw.whiten(tone, 0.4F)));
				}
			}
		});
	}

	/** Hogyoku: a violet orb at the chest inside three turning rune rings, a pillar of light, and the power washing out in a ring. */
	static void hogyoku(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.55F, 1.0F);
		float wave = ease(p, 0.05F, 0.6F);
		float time = s.ageInTicks;
		Vector3f heart = new Vector3f(0, 1.3F, 0);
		glow(ps, c, o -> {
			o.sprite(heart, s.camRight, s.camUp, 1.3F, argb(0.6F * fade, VIOLET));
			o.sprite(heart, s.camRight, s.camUp, 0.45F, argb(fade, 0xFFFFFF));
			// rune rings: a band with ticks round it, each tilted and turning
			for (int r = 0; r < 3; r++) {
				float tilt = 0.5F + r * 0.9F;
				float spin = time * (0.12F + 0.05F * r) * (r % 2 == 0 ? 1 : -1);
				float radius = 0.7F + 0.2F * r;
				Vector3f ax = new Vector3f(Mth.cos(tilt), 0, Mth.sin(tilt));
				Vector3f ay = new Vector3f(-Mth.sin(tilt) * Mth.sin(0.6F + r), Mth.cos(0.6F + r), Mth.cos(tilt) * Mth.sin(0.6F + r)).normalize();
				Vector3f prev = null;
				int n = 28;
				for (int i = 0; i <= n; i++) {
					float a = spin + Mth.TWO_PI * i / n;
					Vector3f at = new Vector3f(ax).mul(Mth.cos(a) * radius).add(new Vector3f(ay).mul(Mth.sin(a) * radius)).add(heart);
					if (prev != null) {
						o.glowLine(prev, at, 0.05F, argb(0.8F * fade, VIOLET), argb(0.8F * fade, VIOLET));
					}
					if (i % 4 == 0) {
						Vector3f out = new Vector3f(at).sub(heart).normalize(0.12F);
						o.bar(at, new Vector3f(at).add(out), 0.03F, 0.03F, argb(fade, 0xFFFFFF), argb(fade, 0xE0D0FF));
					}
					prev = at;
				}
			}
			// the pillar of light
			o.glowLine(new Vector3f(0, 0.1F, 0), new Vector3f(0, 9.0F, 0), 0.6F * (1 - wave * 0.5F), argb(0.6F * fade, VIOLET), argb(0.0F, VIOLET));
			o.bar(new Vector3f(0, 0.1F, 0), new Vector3f(0, 7.0F, 0), 0.04F, 0.0F, argb(0.8F * fade, 0xFFFFFF), argb(0.0F, 0xFFFFFF));
			// the ring washing out over the area, a short wall riding it
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			o.ring(ground, Math.max(0.0F, s.size * wave - 0.8F), s.size * wave, argb(0.9F * fade, VIOLET));
			o.wall(ground, s.size * wave, 0.0F, 1.2F, argb(0.45F * fade, VIOLET), argb(0.0F, VIOLET));
		});
	}

	/** Kyoka Suigetsu: a mirror pane in front of the caster cracks from the middle and flies apart; ripples spread on the ground. */
	static void mirror(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float crack = ease(p, 0.0F, 0.25F);
		float fly = ease(p, 0.25F, 1.0F);
		float fade = 1.0F - ease(p, 0.55F, 1.0F);
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		Vector3f center = new Vector3f(f).mul(0.9F).add(0, 1.1F, 0);
		float halfW = 0.75F;
		float halfH = 1.05F;
		// shards: wedges from the crack point to the frame, split again halfway out
		int wedges = 10;
		Vector3f crackAt = new Vector3f(side).mul(0.1F).add(0, 0.15F, 0).add(center);
		Vector3f[] rim = new Vector3f[wedges];
		for (int i = 0; i < wedges; i++) {
			float a = Mth.TWO_PI * (i + 0.35F * hash(s.seed + i)) / wedges;
			float x = Mth.cos(a);
			float y = Mth.sin(a);
			float scale = 1.0F / Math.max(Math.abs(x) / halfW, Math.abs(y) / halfH);
			rim[i] = new Vector3f(side).mul(x * scale).add(0, y * scale, 0).add(center);
		}
		java.util.function.Consumer<VfxOut> shards = o -> {
			for (int i = 0; i < wedges; i++) {
				Vector3f a = rim[i];
				Vector3f b = rim[(i + 1) % wedges];
				for (int half = 0; half < 2; half++) {
					Vector3f p0 = half == 0 ? new Vector3f(crackAt) : new Vector3f(crackAt).lerp(a, 0.5F);
					Vector3f p1 = half == 0 ? new Vector3f(crackAt).lerp(a, 0.5F) : new Vector3f(a);
					Vector3f p2 = half == 0 ? new Vector3f(crackAt).lerp(b, 0.5F) : new Vector3f(b);
					Vector3f p3 = half == 0 ? new Vector3f(crackAt).lerp(b, 0.5F) : new Vector3f(crackAt).lerp(b, 0.5F);
					Vector3f mid = new Vector3f(p0).add(p1).add(p2).div(3.0F);
					// flying out and forward, turning
					Vector3f away = new Vector3f(mid).sub(crackAt).normalize().mul(1.5F + half).add(new Vector3f(f).mul(1.2F + hash(i * 3 + half) * 1.5F));
					away.y += (hash(i * 7 + half) - 0.3F) * 1.2F;
					Vector3f move = new Vector3f(away).mul(fly * 2.0F);
					float turn = fly * (hash(i * 11 + half) - 0.5F) * 6.0F;
					Vector3f q0 = turnAbout(p0, mid, turn).add(move);
					Vector3f q1 = turnAbout(p1, mid, turn).add(move);
					Vector3f q2 = turnAbout(p2, mid, turn).add(move);
					Vector3f q3 = turnAbout(p3, mid, turn).add(move);
					if (o.glow) {
						int edge = argb(0.9F * fade, half == 0 ? 0xFFFFFF : VIOLET);
						o.glowLine(q0, q1, 0.035F, edge, edge);
						o.glowLine(q1, q2, 0.035F, edge, edge);
						o.glowLine(q2, q3, 0.035F, edge, edge);
						o.glowLine(q3, q0, 0.035F, edge, edge);
					} else {
						int glass = argb(0.4F * fade, 0xDCD4F4);
						int sheen = argb(0.55F * fade, 0xFFFFFF);
						o.quad(q0, q1, q2, q3, sheen, glass, glass, glass);
					}
				}
			}
		};
		shade(ps, c, shards);
		glow(ps, c, o -> {
			shards.accept(o);
			o.sprite(crackAt, s.camRight, s.camUp, 0.6F * (1 - fly), argb(0.9F * crack, 0xFFFFFF));
			// the water moon: rings spreading on the ground out to the size (what Complete Hypnosis blinds; a block round the feet
			// for Illusion Step, which hits nothing) and resting there as they fade
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			float rest = 1.0F - ease(p, 0.75F, 1.0F);
			for (int w = 0; w < 3; w++) {
				float q = Mth.clamp(p * 1.4F - w * 0.18F, 0.0F, 1.0F);
				if (q > 0) {
					o.ring(ground, Math.max(0.0F, s.size * q - 0.3F), s.size * q, argb(0.6F * (1 - 0.5F * q) * rest, VIOLET));
				}
			}
		});
	}

	/** {@code point} turned {@code angle} round the vertical through {@code pivot}. */
	private static Vector3f turnAbout(final Vector3f point, final Vector3f pivot, final float angle) {
		Vector3f d = new Vector3f(point).sub(pivot);
		float cs = Mth.cos(angle);
		float sn = Mth.sin(angle);
		return new Vector3f(d.x * cs - d.z * sn, d.y, d.x * sn + d.z * cs).add(pivot);
	}

	/**
	 * Hado #90 Kurohitsugi: black walls rise round the aimed spot and close into a coffin, violet light running along their seams and
	 * spears driven in through them, before it breaks apart. The coffin's corners and the violet ring on the ground stand on the zone's
	 * edge (the size).
	 */
	static void kurohitsugi(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float rise = ease(p, 0.0F, 0.2F);
		float spears = ease(p, 0.3F, 0.45F);
		float fade = 1.0F - ease(p, 0.85F, 1.0F);
		float half = Math.max(1.4F, s.size * 0.71F);
		float height = 4.0F * rise;
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		Vector3f[] corners = new Vector3f[4];
		for (int i = 0; i < 4; i++) {
			float sx = i == 0 || i == 3 ? -1 : 1;
			float sz = i < 2 ? -1 : 1;
			corners[i] = new Vector3f(side).mul(sx * half).add(new Vector3f(f).mul(sz * half));
		}
		shade(ps, c, o -> {
			int black = argb(0.9F * fade, 0x07040C);
			int deep = argb(0.9F * fade, 0x1A0E2E);
			for (int i = 0; i < 4; i++) {
				Vector3f a = corners[i];
				Vector3f b = corners[(i + 1) % 4];
				o.quad(a, b, new Vector3f(b).add(0, height, 0), new Vector3f(a).add(0, height, 0), deep, deep, black, black);
			}
			if (rise >= 1.0F) {
				o.quad(new Vector3f(corners[0]).add(0, height, 0), new Vector3f(corners[1]).add(0, height, 0), new Vector3f(corners[2]).add(0, height, 0),
					new Vector3f(corners[3]).add(0, height, 0), black);
			}
			// spears driven in through the walls
			for (int i = 0; i < 12 && spears > 0; i++) {
				int wall = i % 4;
				Vector3f a = corners[wall];
				Vector3f b = corners[(wall + 1) % 4];
				Vector3f at = new Vector3f(a).lerp(b, 0.2F + 0.6F * hash(i * 3 + s.seed)).add(0, 0.6F + 2.8F * hash(i * 5), 0);
				Vector3f in = new Vector3f(0, at.y, 0).sub(at).normalize();
				Vector3f outside = new Vector3f(at).sub(new Vector3f(in).mul(0.9F));
				Vector3f tip = new Vector3f(at).add(new Vector3f(in).mul(half * 0.85F * spears));
				o.blade(outside, tip, UP, 0.12F, black, argb(0.95F * fade, 0x3A2060));
			}
		});
		glow(ps, c, o -> {
			int seam = argb(0.85F * fade, 0x9A6AFF);
			for (int i = 0; i < 4; i++) {
				Vector3f a = corners[i];
				Vector3f b = corners[(i + 1) % 4];
				o.glowLine(new Vector3f(a).add(0, 0.15F, 0), new Vector3f(b).add(0, 0.15F, 0), 0.2F, seam, seam);
				o.glowLine(new Vector3f(a).add(0, height, 0), new Vector3f(b).add(0, height, 0), 0.15F, seam, seam);
				o.glowLine(a, new Vector3f(a).add(0, height, 0), 0.15F, seam, seam);
				// violet glyph lines across each wall
				for (int l = 1; l <= 3; l++) {
					float y = height * l / 4.0F;
					o.glowLine(new Vector3f(a).lerp(b, 0.15F).add(0, y, 0), new Vector3f(a).lerp(b, 0.85F).add(0, y, 0), 0.06F, argb(0.5F * fade, VIOLET),
						argb(0.5F * fade, VIOLET));
				}
			}
			o.ring(new Vector3f(0, 0.15F, 0), s.size * 0.85F, s.size * 1.02F, argb(0.5F * fade, VIOLET));
		});
	}

	/** Dust to dust: a silver ring runs out over the area, cracks spread from the middle and ash drifts up out of the ground. */
	static void dust(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float out = ease(p, 0.0F, 0.35F);
		float fade = 1.0F - ease(p, 0.5F, 1.0F);
		shade(ps, c, o -> {
			for (int i = 0; i < 70; i++) {
				float r = s.size * out * (float)Math.sqrt(hash(i * 13 + s.seed));
				float a = Mth.TWO_PI * hash(i * 17);
				float q = (p * (1.2F + hash(i)) + hash(i * 19)) % 1.0F;
				Vector3f at = polar(a + q * 0.5F, r, 0.1F + q * 2.4F);
				o.square(at, s.camRight, s.camUp, 0.06F + 0.05F * hash(i * 23), q * 8.0F + i, argb(0.8F * fade * (1 - q), 0x4A3A50));
			}
		});
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			o.ring(ground, Math.max(0.0F, s.size * out - 0.35F), s.size * out, argb(0.8F * fade, s.color));
			o.disc(ground, s.size * out, argb(0.0F, 0x7A3AA0), argb(0.25F * fade, 0x7A3AA0));
			// cracks: jagged lines from the middle out to the ring
			for (int k = 0; k < 9; k++) {
				float a = Mth.TWO_PI * (k + hash(k + s.seed) * 0.6F) / 9;
				Vector3f prev = new Vector3f(0, 0.17F, 0);
				for (int j = 1; j <= 5; j++) {
					float r = s.size * out * j / 5.0F;
					Vector3f at = polar(a + (hash(k * 31 + j) - 0.5F) * 0.35F, r, 0.17F);
					o.glowLine(prev, at, 0.07F, argb(0.8F * fade, 0xE8F0FF), argb(0.6F * fade, 0xB070FF));
					prev = at;
				}
			}
			for (int i = 0; i < 24; i++) {
				float q = (p * 1.5F + hash(i * 7)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 3), s.size * out * hash(i * 11), q * 2.8F);
				o.sprite(at, s.camRight, s.camUp, 0.1F, argb(0.8F * fade * (1 - q), 0xD8B8FF));
			}
		});
	}

	/** Itosabaki: three strips of cloth wind round the target from its feet to its head and pull tight, their trim glowing. */
	static void binding(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float tight = ease(p, 0.0F, 0.4F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float height = Math.max(1.0F, s.size);
		float width = Math.max(0.5F, s.arg);
		float time = s.ageInTicks * (1.0F - 0.8F * tight);
		int strips = 3;
		int steps = 36;
		float turns = 2.6F;
		Vector3f[][] lo = new Vector3f[strips][steps + 1];
		Vector3f[][] hi = new Vector3f[strips][steps + 1];
		for (int k = 0; k < strips; k++) {
			for (int i = 0; i <= steps; i++) {
				float u = (float)i / steps;
				float a = Mth.TWO_PI * (k / (float)strips + u * turns) + time * 0.25F;
				float r = width * (1.5F - 0.85F * tight) * (0.85F + 0.15F * Mth.sin(u * 9.0F + k));
				float y = height * (0.02F + 1.0F * u);
				float band = 0.22F;
				lo[k][i] = polar(a, r, y - band / 2);
				hi[k][i] = polar(a, r, y + band / 2);
			}
		}
		shade(ps, c, o -> {
			int cloth = argb(0.95F * fade, 0xEDE4F2);
			int shadow = argb(0.95F * fade, 0xB8A8C8);
			for (int k = 0; k < strips; k++) {
				for (int i = 0; i < steps; i++) {
					int col = i % 2 == 0 ? cloth : shadow;
					o.quad(lo[k][i], lo[k][i + 1], hi[k][i + 1], hi[k][i], col);
				}
			}
			// loose ends fluttering out from the top
			for (int k = 0; k < strips; k++) {
				Vector3f end = hi[k][steps];
				float a = Mth.TWO_PI * k / strips + time * 0.25F;
				Vector3f out = polar(a, 0.9F, 0.4F + 0.2F * Mth.sin(s.ageInTicks * 0.5F + k)).add(end);
				o.quad(lo[k][steps], end, out, new Vector3f(out).add(0, -0.22F, 0), cloth, cloth, argb(0.0F, 0xEDE4F2), argb(0.0F, 0xEDE4F2));
			}
		});
		glow(ps, c, o -> {
			int trim = argb(0.7F * fade, s.color);
			for (int k = 0; k < strips; k++) {
				for (int i = 0; i < steps; i++) {
					o.glowLine(hi[k][i], hi[k][i + 1], 0.05F, trim, trim);
					o.glowLine(lo[k][i], lo[k][i + 1], 0.05F, trim, trim);
				}
			}
			o.ring(new Vector3f(0, 0.15F, 0), width * 0.8F, width * 1.8F, argb(0.5F * fade, s.color));
		});
	}

	/** Mugetsu's darkness settling on the aimed spot: a black dome swelling out of the ground, its rim smouldering red. */
	static void mugetsuVoid(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float grow = ease(p, 0.0F, 0.15F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float r = s.size * grow;
		int lats = 6;
		int lons = 24;
		shade(ps, c, o -> {
			int black = argb(0.82F * fade, 0x040206);
			for (int i = 0; i < lats; i++) {
				float l0 = Mth.HALF_PI * i / lats;
				float l1 = Mth.HALF_PI * (i + 1) / lats;
				for (int j = 0; j < lons; j++) {
					float a0 = Mth.TWO_PI * j / lons;
					float a1 = Mth.TWO_PI * (j + 1) / lons;
					o.quad(dome(r, l0, a0), dome(r, l0, a1), dome(r, l1, a1), dome(r, l1, a0), black);
				}
			}
			for (int i = 0; i < 18; i++) {
				float a = Mth.TWO_PI * i / 18;
				o.flame(polar(a, r, 0.0F), new Vector3f(Mth.cos(a) * 0.15F, 1, Mth.sin(a) * 0.15F), 1.5F + 1.5F * hash(i + s.seed), 1.2F, s.ageInTicks * 0.4F + i,
					argb(0.85F * fade, 0x040206), argb(0.0F, 0x040206));
			}
		});
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			o.ring(ground, r * 0.88F, r * 1.03F, argb(0.7F * fade, 0x9A0A1E));
			o.wall(ground, r * 1.01F, 0.0F, 0.8F, argb(0.5F * fade, 0xC81E2E), argb(0.0F, 0x9A0A1E));
		});
	}

	private static Vector3f dome(final float r, final float lat, final float lon) {
		return new Vector3f(Mth.cos(lat) * Mth.cos(lon) * r, Mth.sin(lat) * r * 0.85F, Mth.cos(lat) * Mth.sin(lon) * r);
	}

	/**
	 * Higa Zekko: Zabimaru's bone segments hang in a ring round the caster on the strikes' edge (the size), fangs turned in, and dart
	 * inward together at each of the three strikes.
	 */
	static void boneFangs(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.12F);
		float fade = 1.0F - ease(p, 0.85F, 1.0F);
		float age = s.age();
		// strikes at 0, 8 and 16 ticks: in fast, back slower
		float dart = 0.0F;
		for (int k = 0; k < 3; k++) {
			float t = age - k * 8.0F - 1.0F;
			if (t >= 0.0F && t < 7.0F) {
				dart = Math.max(dart, t < 1.5F ? t / 1.5F : 1.0F - (t - 1.5F) / 5.5F);
			}
		}
		float d = dart;
		int n = 14;
		Vector3f[] base = new Vector3f[n];
		Vector3f[] tip = new Vector3f[n];
		for (int i = 0; i < n; i++) {
			float a = Mth.TWO_PI * i / n + s.ageInTicks * 0.04F;
			float r = s.size * open * (1.0F - 0.8F * d);
			float y = 1.0F + 0.5F * Mth.sin(i * 1.9F + s.ageInTicks * 0.2F);
			base[i] = polar(a, r, y + 0.35F);
			tip[i] = polar(a, Math.max(0.3F, r - 2.0F), y);
		}
		shade(ps, c, o -> {
			for (int i = 0; i < n; i++) {
				Vector3f tangent = new Vector3f(base[i]).sub(tip[i]).cross(UP);
				o.blade(base[i], tip[i], tangent, 0.4F, argb(0.95F * fade, 0xD8CBB0), argb(0.95F * fade, BONE));
			}
		});
		glow(ps, c, o -> {
			for (int i = 0; i < n; i++) {
				o.glowLine(base[i], base[(i + 1) % n], 0.12F, argb(0.6F * fade, BONE_GLOW), argb(0.6F * fade, BONE_GLOW));
				o.glowLine(base[i], tip[i], 0.15F, argb(0.2F * fade, BONE_GLOW), argb(0.7F * fade, 0xFF8A70));
			}
			if (d > 0.6F) {
				o.sprite(new Vector3f(0, 1.0F, 0), s.camRight, s.camUp, 1.8F * d, argb(0.6F * d * fade, BONE_GLOW));
			}
			o.ring(new Vector3f(0, 0.15F, 0), s.size * open * 0.85F, s.size * open, argb(0.45F * fade, BONE_GLOW));
		});
	}

	/** Baboon bone armor: great ribs curving up out of the ground round the caster, red light along them. */
	static void boneCage(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float rise = ease(p, 0.0F, 0.2F);
		float sink = ease(p, 0.8F, 1.0F);
		float k = rise * (1.0F - sink);
		int ribs = 8;
		int segs = 7;
		Vector3f[][] spine = new Vector3f[ribs][segs + 1];
		for (int r = 0; r < ribs; r++) {
			float a = Mth.TWO_PI * (r + 0.5F) / ribs;
			for (int j = 0; j <= segs; j++) {
				float u = (float)j / segs * k;
				// up and out, then curling in overhead
				float radius = 1.35F + 0.35F * Mth.sin(u * Mth.PI) - 0.9F * u * u;
				float y = 2.5F * Mth.sin(u * Mth.HALF_PI * 1.15F) - 0.2F;
				spine[r][j] = polar(a, radius, y);
			}
		}
		shade(ps, c, o -> {
			for (int r = 0; r < ribs; r++) {
				for (int j = 0; j < segs; j++) {
					float w = 0.16F * (1.0F - 0.6F * j / segs);
					o.bar(spine[r][j], spine[r][j + 1], w, w * 0.85F, argb(0.95F, 0xD8CBB0), argb(0.95F, BONE));
				}
				// a spike off each rib's knee
				Vector3f knee = spine[r][segs / 2];
				o.blade(knee, new Vector3f(knee).mul(1.35F, 1.0F, 1.35F).add(0, 0.3F, 0), UP, 0.1F, argb(0.95F, BONE), argb(0.95F, 0xFFFFFF));
			}
		});
		glow(ps, c, o -> {
			for (int r = 0; r < ribs; r++) {
				for (int j = 0; j < segs; j++) {
					o.glowLine(spine[r][j], spine[r][j + 1], 0.22F, argb(0.4F * k, BONE_GLOW), argb(0.4F * k, BONE_GLOW));
				}
			}
			o.ring(new Vector3f(0, 0.15F, 0), 0.9F, 1.9F, argb(0.6F * k, BONE_GLOW));
		});
	}

	private SpiritLooks() {
	}
}
