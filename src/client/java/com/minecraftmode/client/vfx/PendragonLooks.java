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
 * Looks of the King of Knights (Fate): Artoria's Excalibur (the sword of promised victory, Invisible Air, Mana Burst and Avalon) and
 * Saber Alter's Excalibur Morgan (Vortigern, the dark mana burst, the tyrant's charge and the black armor).
 */
final class PendragonLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final int GOLD = 0xFFD24A;
	private static final int GOLD_DEEP = 0xFFA020;
	private static final int GOLD_PALE = 0xFFF2B8;
	private static final int AIR = 0xE4F4FF;
	private static final int MANA = 0x4F9CFF;
	private static final int MANA_CORE = 0xDDF0FF;
	private static final int AVALON_BLUE = 0x2E5CC8;
	private static final int CRIMSON = 0xD0102A;
	private static final int CRIMSON_PALE = 0xFF7A80;
	private static final int BLACK = 0x07030A;
	/** How far round its line a beam hits (SkillContext.enemiesAlong width): the torrents are drawn this wide. */
	static final float BEAM_R = 0.9F;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case EXCALIBUR -> torrent(s, ps, c, false);
			case VORTIGERN -> torrent(s, ps, c, true);
			case AIR_RELEASE -> airRelease(s, ps, c);
			case MANA_BURST -> manaBurst(s, ps, c);
			case AVALON_SHEATH -> avalon(s, ps, c);
			case DARK_BURST -> darkBurst(s, ps, c);
			case TYRANT_TRAIL -> tyrantTrail(s, ps, c);
			case BLACK_ARMOR -> blackArmor(s, ps, c);
			default -> {
			}
		}
	}

	/** A beam look's direction (its reach), the caster's flat look when it has none. */
	static Vector3f beamDir(final State s) {
		return s.reach.lengthSquared() < 1.0E-6F ? new Vector3f(s.forward) : new Vector3f(s.reach).normalize();
	}

	/** Square to a beam's direction, pointing up as far as it can. */
	static Vector3f beamUp(final Vector3f dir) {
		return new Vector3f(VfxOut.across(dir)).cross(dir).normalize();
	}

	/** The flat direction of a trail's last stretch (where the holder was heading), the look's own forward when it never moved. */
	static Vector3f heading(final State s, final List<Vector3f> points, final int last) {
		Vector3f d = new Vector3f(points.get(last)).sub(points.get(Math.max(0, last - 1)));
		d.y = 0;
		if (d.lengthSquared() < 1.0E-4F) {
			d = new Vector3f(points.get(last));
			d.y = 0;
		}
		return d.lengthSquared() < 1.0E-4F ? new Vector3f(s.forward) : d.normalize();
	}

	/** The flat direction a trail runs at {@code t} (0..1 of the way along it). */
	static Vector3f along(final List<Vector3f> points, final int last, final float t) {
		Vector3f a = VfxOut.along(points, last, Math.max(0.0F, t - 0.03F));
		Vector3f b = VfxOut.along(points, last, Math.min(1.0F, t + 0.03F));
		Vector3f d = b.sub(a);
		d.y = 0;
		return d.lengthSquared() < 1.0E-5F ? new Vector3f(0, 0, 1) : d.normalize();
	}

	// ------------------------------------------------------------ Excalibur and Excalibur Morgan

	/**
	 * Excalibur (or, {@code dark}, Excalibur Morgan): the sword raised overhead while a pillar of light stands from it and motes stream into
	 * its blade, then swung down: a torrent of light runs out along the beam, a great sheet of it standing over the line and widening with
	 * distance, rings rolling along and a flare at its head. Excalibur is gold; Morgan's torrent is black under a red rim, red lightning
	 * crawling over it.
	 */
	private static void torrent(final State s, final PoseStack ps, final SubmitNodeCollector c, final boolean dark) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float len = Math.max(2.0F, s.size);
		Vector3f dir = beamDir(s);
		Vector3f side = VfxOut.across(dir);
		Vector3f up = beamUp(dir);
		float fade = 1.0F - ease(p, 0.6F, 1.0F);
		float rise = ease(age, 0.0F, 4.0F);
		float swing = ease(age, 5.0F, 9.0F);
		Vector3f hand = new Vector3f(side).mul(0.35F).add(new Vector3f(dir).mul(0.35F)).add(0, -0.3F, 0);
		Vector3f bladeDir = new Vector3f(UP).lerp(dir, swing).normalize();
		Vector3f tip = new Vector3f(bladeDir).mul(1.5F).add(hand);
		float pillar = 7.0F * rise * (1.0F - swing);
		Vector3f start = new Vector3f(dir).mul(0.8F);
		float reach = Math.max(0.0F, len * ease(age, 7.0F, 13.0F) - 0.8F);
		Vector3f front = new Vector3f(dir).mul(0.8F + reach);
		int segs = 20;
		int main = dark ? CRIMSON : GOLD;
		int deep = dark ? 0x8A0418 : GOLD_DEEP;
		int pale = dark ? CRIMSON_PALE : GOLD_PALE;
		// the bolts crawling over Morgan's torrent, thrown anew every other tick
		List<List<Vector3f>> bolts = new ArrayList<>();
		if (dark && reach > 1.0F) {
			int seed = s.seed * 31 + (int)(time / 2);
			for (int i = 0; i < 6; i++) {
				float d0 = reach * hash(seed + i) * 0.7F;
				float d1 = Math.min(reach, d0 + 2.0F + 4.0F * hash(seed * 3 + i));
				float h = (hash(seed * 7 + i) - 0.5F) * 1.4F;
				Vector3f off = new Vector3f(side).mul((hash(seed * 11 + i) - 0.5F) * 1.4F).add(new Vector3f(up).mul(h));
				bolts.add(VfxOut.zigzag(new Vector3f(dir).mul(0.8F + d0).add(off), new Vector3f(dir).mul(0.8F + d1).add(off).add(new Vector3f(up).mul(0.4F)), seed + i,
					0.5F, 8));
			}
		}
		if (dark) {
			shade(ps, c, o -> {
				// the black blade and the pillar of darkness standing from it
				o.blade(hand, tip, side, 0.13F, argb(0.97F * fade, 0x14141A), argb(0.97F * fade, 0x2A2A34));
				if (pillar > 0.1F) {
					o.bar(tip, new Vector3f(bladeDir).mul(pillar).add(tip), 0.35F, 0.12F, argb(0.85F, BLACK), argb(0.0F, BLACK));
				}
				if (reach > 0.0F) {
					sheet(o, dir, side, up, reach, segs, argb(0.92F * fade, BLACK), argb(0.55F * fade, BLACK), argb(0.85F * fade, BLACK));
					for (int i = 0; i < 14; i++) {
						float d = 0.8F + reach * hash(s.seed + i * 5);
						float q = (time * 0.05F + hash(i * 3)) % 1.0F;
						Vector3f at = new Vector3f(dir).mul(d).add(new Vector3f(up).mul(-0.5F + q)).add(new Vector3f(side).mul((hash(i * 7) - 0.5F) * 1.0F));
						o.sprite(at, s.camRight, s.camUp, 0.45F + 0.2F * q, argb(0.5F * fade * (1.0F - q), 0x120610));
					}
				}
				for (List<Vector3f> b : bolts) {
					o.polyline(b, 0.025F, argb(0.95F * fade, BLACK));
				}
			});
		}
		glow(ps, c, o -> {
			if (!dark) {
				o.blade(hand, tip, side, 0.12F, argb(fade, 0xFFFFFF), argb(fade, GOLD_PALE));
			}
			o.glowLine(hand, tip, 0.12F, argb(0.8F * fade, main), argb(0.8F * fade, pale));
			o.sprite(tip, s.camRight, s.camUp, 0.6F + 0.4F * rise, argb(0.7F * fade, pale));
			if (pillar > 0.1F) {
				// the pillar of light standing from the raised sword, and the light streaming into it
				Vector3f top = new Vector3f(bladeDir).mul(pillar).add(tip);
				o.glowLine(tip, top, 1.0F, argb(0.45F, main), argb(0.0F, main));
				o.glowLine(tip, top, 0.35F, argb(0.8F, pale), argb(0.0F, pale));
				for (int i = 0; i < 20; i++) {
					float q = (age * 0.07F + hash(s.seed + i * 3)) % 1.0F;
					Vector3f at = BladeLooks.shardDir(s.seed, i).mul(3.8F * (1.0F - q)).add(tip);
					o.glint(at, s.camRight, s.camUp, 0.16F, time * 0.2F + i, argb(0.95F * (1.0F - swing) * Mth.sin(q * Mth.PI), i % 3 == 0 ? 0xFFFFFF : pale));
				}
			}
			if (reach <= 0.0F) {
				return;
			}
			o.glowLine(start, front, BEAM_R * 1.1F, argb((dark ? 0.12F : 0.3F) * fade, deep), argb((dark ? 0.1F : 0.25F) * fade, deep));
			o.glowLine(start, front, BEAM_R * 0.7F, argb((dark ? 0.1F : 0.5F) * fade, main), argb((dark ? 0.08F : 0.4F) * fade, main));
			if (!dark) {
				o.glowLine(start, front, 0.3F, argb(0.85F * fade, 0xFFFFFF), argb(0.7F * fade, 0xFFFFFF));
				o.bar(start, front, 0.12F, 0.08F, argb(fade, 0xFFFFFF), argb(fade, GOLD_PALE));
				sheet(o, dir, side, up, reach, segs, argb(0.42F * fade, GOLD), argb(0.0F, GOLD), argb(0.25F * fade, GOLD_DEEP));
			}
			// the lit edges at the hit boundary: Morgan's red, Excalibur's white, along the four sides of the body
			int rim = dark ? CRIMSON : 0xFFFFFF;
			for (int k = 0; k < 4; k++) {
				Vector3f off = new Vector3f(k < 2 ? up : side).mul((k % 2 == 0 ? 1 : -1) * BEAM_R);
				float al = (k == 0 ? 0.6F : 0.4F) * fade;
				o.glowLine(new Vector3f(start).add(off), new Vector3f(front).add(off), 0.18F, argb(al, rim), argb(al, rim));
			}
			for (List<Vector3f> b : bolts) {
				o.glowPolyline(b, 0.09F, argb(0.85F * fade, CRIMSON));
				o.glowPolyline(b, 0.25F, argb(0.2F * fade, CRIMSON));
			}
			// rings of light rolling along it
			for (int i = 0; i < 7; i++) {
				float d = ((i / 7.0F + time * 0.035F) % 1.0F) * reach;
				Vector3f at = new Vector3f(dir).mul(0.8F + d);
				float r = BEAM_R;
				o.circle(at, side, up, r, 0.05F, 24, argb(0.6F * fade * (1.0F - d / (reach + 1.0F) * 0.5F), pale));
			}
			// motes thrown up off it
			for (int i = 0; i < 18; i++) {
				float q = (time * 0.04F + hash(s.seed * 5 + i)) % 1.0F;
				float d = 0.8F + reach * hash(i * 13);
				Vector3f at = new Vector3f(dir).mul(d).add(new Vector3f(side).mul((hash(i * 17) - 0.5F) * 1.6F)).add(new Vector3f(up).mul(-0.5F + 3.0F * q));
				o.glint(at, s.camRight, s.camUp, 0.2F, time * 0.1F + i, argb(0.9F * fade * Mth.sin(q * Mth.PI), dark ? CRIMSON_PALE : GOLD_PALE));
			}
			float flare = 1.0F - ease(age, 11.0F, 20.0F);
			if (flare > 0.0F) {
				o.star(front, s.camRight, s.camUp, 1.6F * flare + 0.4F, 10, 0.3F, s.seed, argb(0.9F * flare, dark ? CRIMSON_PALE : 0xFFFFFF), argb(0.0F, main));
			}
			o.sprite(front, s.camRight, s.camUp, BEAM_R * 1.3F, argb(0.35F * fade, main));
		});
	}

	/**
	 * The torrent's body, {@link #BEAM_R} round the line (what the beam hits): a standing fin and a lying one crossed along it, {@code low}
	 * along the middle fading to {@code high} at the top and bottom edges, {@code floor} to clear at the side edges.
	 */
	private static void sheet(final VfxOut o, final Vector3f dir, final Vector3f side, final Vector3f up, final float reach, final int segs, final int low,
		final int high, final int floor) {
		int edge = floor & 0xFFFFFF;
		for (int i = 0; i < segs; i++) {
			float d0 = 0.8F + reach * i / segs;
			float d1 = 0.8F + reach * (i + 1) / segs;
			Vector3f m0 = new Vector3f(dir).mul(d0);
			Vector3f m1 = new Vector3f(dir).mul(d1);
			for (int k = -1; k <= 1; k += 2) {
				Vector3f u0 = new Vector3f(up).mul(k * BEAM_R).add(m0);
				Vector3f u1 = new Vector3f(up).mul(k * BEAM_R).add(m1);
				o.quad(m0, m1, u1, u0, low, low, high, high);
				Vector3f e0 = new Vector3f(side).mul(k * BEAM_R).add(m0);
				Vector3f e1 = new Vector3f(side).mul(k * BEAM_R).add(m1);
				o.quad(m0, m1, e1, e0, floor, floor, edge, edge);
			}
		}
	}

	/** Invisible Air unbound: whirling gusts burst out round the caster, dust kicked up in a ring and a glint where the sword shows. */
	private static void airRelease(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = s.size;
		float q = ease(age, 0.0F, 10.0F);
		float fade = 1.0F - ease(p, 0.45F, 1.0F);
		Vector3f ahead = new Vector3f(s.forward);
		Vector3f right = VfxOut.across(ahead);
		shade(ps, c, o -> {
			for (int i = 0; i < 14; i++) {
				float a = Mth.TWO_PI * i / 14 + hash(s.seed + i);
				Vector3f at = polar(a, r * q * (0.7F + 0.3F * hash(i * 3)), 0.3F + 0.3F * hash(i * 5));
				o.sprite(at, s.camRight, s.camUp, 0.5F + 0.6F * q, argb(0.3F * (1.0F - q), i % 2 == 0 ? 0xC8C0B0 : 0xA8A090));
			}
		});
		glow(ps, c, o -> {
			for (int k = 0; k < 12; k++) {
				float a0 = Mth.TWO_PI * k / 12 + age * 0.3F + hash(s.seed + k);
				float y = 0.3F + 1.8F * hash(k * 7);
				float rad = Math.max(0.5F, r * q * (0.75F + 0.25F * hash(k * 3)));
				Vector3f prev = null;
				for (int j = 0; j <= 8; j++) {
					float t = j / 8.0F;
					Vector3f pt = polar(a0 - t * 1.5F, rad * (1.0F - 0.2F * t), y + 0.25F * t);
					if (prev != null) {
						float al = 0.7F * fade * (1.0F - t);
						o.glowLine(prev, pt, 0.08F, argb(al, AIR), argb(al * 0.8F, AIR));
					}
					prev = pt;
				}
			}
			o.ring(new Vector3f(0, 0.14F, 0), Math.max(0.0F, r * q - 0.5F), r * q, argb(0.5F * fade, AIR));
			float flash = 1.0F - ease(age, 0.0F, 6.0F);
			o.star(new Vector3f(ahead).mul(0.7F).add(new Vector3f(right).mul(0.35F)).add(0, 1.1F, 0), s.camRight, s.camUp, 0.9F * flash + 0.1F, 4, 0.15F, 0.4F,
				argb(0.95F * flash, 0xFFFFFF), argb(0.0F, GOLD));
		});
	}

	/** Mana Burst: blue-white jets of mana blasting backwards off the path the caster shot along, sparks thrown and a burst where they set off. */
	private static void manaBurst(final State s, final PoseStack ps, final SubmitNodeCollector c) {
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
		glow(ps, c, o -> {
			float burst = ease(p, 0.0F, 0.4F);
			o.ring(new Vector3f(0, 0.14F, 0), 0.3F + 2.2F * burst, 0.8F + 2.2F * burst, argb(0.7F * (1.0F - burst), MANA));
			o.sprite(new Vector3f(0, 1.0F, 0), s.camRight, s.camUp, 1.8F * (1.0F - burst) + 0.2F, argb(0.7F * fade, MANA_CORE));
			List<Vector3f> line = new ArrayList<>();
			for (int i = 0; i <= last; i++) {
				line.add(new Vector3f(points.get(i)).add(0, 1.0F, 0));
			}
			o.glowPolyline(line, 0.6F, argb(0.35F * fade, MANA));
			o.glowPolyline(line, 0.18F, argb(0.8F * fade, MANA_CORE));
			int jets = Math.min(32, (int)(length / 0.45F));
			for (int i = 0; i <= jets; i++) {
				float t = jets == 0 ? 0.0F : (float)i / jets;
				Vector3f at = VfxOut.along(points, last, t).add(0, 0.5F + 0.9F * hash(s.seed + i), 0);
				Vector3f back = along(points, last, t).negate();
				Vector3f d = new Vector3f(back).add(new Vector3f(VfxOut.across(back)).mul((hash(i * 3) - 0.5F) * 1.4F)).add(0, (hash(i * 5) - 0.3F) * 0.8F, 0).normalize();
				float h = (0.9F + 1.4F * hash(i * 7)) * fade;
				o.flame(at, d, h, 0.42F, time * 0.9F + i, argb(0.85F * fade, MANA_CORE), argb(0.0F, MANA));
			}
			for (int i = 0; i < 12; i++) {
				float t = hash(s.seed * 3 + i);
				float q = (time * 0.08F + hash(i * 11)) % 1.0F;
				Vector3f at = VfxOut.along(points, last, t).add(BladeLooks.shardDir(s.seed, i).mul(0.4F + 1.2F * q)).add(0, 1.0F, 0);
				o.glint(at, s.camRight, s.camUp, 0.16F, time * 0.2F + i, argb(0.9F * fade * (1.0F - q), MANA_CORE));
			}
		});
	}

	/**
	 * Avalon: the scabbard of blue and gold appears before the caster, then comes apart into shining fragments that fly out and lock into a
	 * dome round them, turning slowly; the dome shimmers until it fades.
	 */
	private static void avalon(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float time = s.ageInTicks;
		float open = ease(p, 0.0F, 0.1F);
		float show = 1.0F - ease(p, 0.2F, 0.3F);
		float burst = ease(p, 0.2F, 0.45F);
		float fade = 1.0F - ease(p, 0.82F, 1.0F);
		float self = s.selfView ? 0.4F : 1.0F;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f center = new Vector3f(ahead).mul(0.75F).add(0, 1.25F, 0);
		Vector3f bottom = new Vector3f(center).add(0, -0.66F, 0);
		Vector3f top = new Vector3f(center).add(0, 0.66F, 0);
		float R = Math.max(1.3F, s.size);
		int frags = 30;
		Vector3f[] at = new Vector3f[frags];
		Vector3f[] normal = new Vector3f[frags];
		for (int i = 0; i < frags; i++) {
			float lat = 0.08F + 1.25F * (float)Math.sqrt(hash(s.seed + i * 3));
			float lon = Mth.TWO_PI * i / frags * 3.0F + hash(i * 7) + time * 0.01F * burst;
			Vector3f target = new Vector3f(Mth.cos(lat) * Mth.cos(lon) * R, 0.15F + Mth.sin(lat) * R * 1.15F, Mth.cos(lat) * Mth.sin(lon) * R);
			Vector3f from = new Vector3f(bottom).lerp(top, (float)i / frags);
			at[i] = from.lerp(target, burst);
			normal[i] = new Vector3f(target).sub(0, 0.6F, 0).normalize();
		}
		if (show > 0.0F) {
			shade(ps, c, o -> o.tube(List.of(bottom, new Vector3f(bottom).lerp(top, 0.5F), top), 0.05F, 0.1F, 8, argb(0.95F * show * open * self, 0x3E6EE0),
				argb(0.95F * show * open * self, 0x10204E)));
		}
		glow(ps, c, o -> {
			if (show > 0.0F) {
				float k = show * open * self;
				// gold trim down both edges, the throat and chape in gold, filigree down the middle
				for (int side = -1; side <= 1; side += 2) {
					Vector3f off = new Vector3f(right).mul(side * 0.085F);
					o.bar(new Vector3f(bottom).add(new Vector3f(off).mul(0.5F)), new Vector3f(top).add(off), 0.012F, 0.016F, argb(0.95F * k, GOLD), argb(0.95F * k, GOLD));
				}
				o.circle(top, right, new Vector3f(ahead), 0.11F, 0.02F, 14, argb(0.95F * k, GOLD));
				o.circle(new Vector3f(top).add(0, -0.1F, 0), right, new Vector3f(ahead), 0.105F, 0.015F, 14, argb(0.9F * k, GOLD));
				o.diamond(new Vector3f(bottom).add(0, 0.06F, 0), UP, right, 0.2F, 0.09F, argb(0.95F * k, GOLD), argb(0.95F * k, GOLD_PALE));
				for (int i = 0; i < 4; i++) {
					Vector3f m = new Vector3f(bottom).lerp(top, 0.25F + 0.17F * i).add(new Vector3f(ahead).mul(-0.1F));
					o.diamond(m, UP, right, 0.16F, 0.08F, argb(0.9F * k, GOLD), argb(0.9F * k, GOLD_PALE));
				}
				o.sprite(center, s.camRight, s.camUp, 1.2F, argb(0.3F * k, MANA));
				float flash = ease(p, 0.15F, 0.22F) * show;
				o.sprite(center, s.camRight, s.camUp, 1.6F * flash, argb(0.8F * flash, 0xFFFFFF));
			}
			if (burst <= 0.0F) {
				return;
			}
			float k = burst * fade * self;
			for (int i = 0; i < frags; i++) {
				if (s.selfView && at[i].y > 1.3F && Math.abs(at[i].x) + Math.abs(at[i].z) < 1.2F) {
					continue;
				}
				o.hexagon(at[i], normal[i], 0.22F, time * 0.03F + i, 0.014F, argb(0.95F * k, GOLD), argb(0.3F * k, AVALON_BLUE));
				o.glint(at[i], s.camRight, s.camUp, 0.12F, time * 0.1F + i, argb(0.7F * k * (0.5F + 0.5F * Mth.sin(time * 0.3F + i)), 0xFFFFFF));
			}
			o.wall(new Vector3f(), R * 0.98F, 0.0F, 1.2F, argb(0.2F * k, AVALON_BLUE), argb(0.0F, AVALON_BLUE));
			o.ring(new Vector3f(0, 0.14F, 0), R * 0.9F, R * 1.02F, argb(0.6F * k, GOLD));
		});
	}

	/** Dark Mana Burst: black flame erupts round the caster, its rims burning red, red bolts lash out and a crimson shock ring rolls along the ground. */
	private static void darkBurst(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = s.size;
		float out = ease(age, 0.0F, 8.0F);
		float fade = 1.0F - ease(p, 0.45F, 1.0F);
		float time = s.ageInTicks;
		int tongues = 18;
		Vector3f[] base = new Vector3f[tongues];
		Vector3f[] dir = new Vector3f[tongues];
		float[] h = new float[tongues];
		for (int i = 0; i < tongues; i++) {
			float a = Mth.TWO_PI * i / tongues + hash(s.seed + i) * 0.3F;
			base[i] = polar(a, 0.3F + r * 0.45F * out, 0.1F);
			dir[i] = polar(a, 0.6F, 1.0F).normalize();
			h[i] = (1.2F + 1.2F * hash(i * 3)) * (0.4F + 0.6F * out) * fade;
		}
		List<List<Vector3f>> bolts = new ArrayList<>();
		int seed = s.seed * 31 + (int)(time / 2);
		for (int i = 0; i < 8; i++) {
			Vector3f d = BladeLooks.shardDir(seed, i);
			d.y = Math.abs(d.y) * 0.4F;
			bolts.add(VfxOut.zigzag(new Vector3f(0, 1.0F, 0), new Vector3f(d).mul(r * out * (0.6F + 0.4F * hash(seed + i))).add(0, 1.0F, 0), seed + i, 0.4F, 7));
		}
		shade(ps, c, o -> {
			o.disc(new Vector3f(0, 0.135F, 0), r * out, argb(0.6F * fade, 0x100408), argb(0.0F, 0x100408));
			for (int i = 0; i < tongues; i++) {
				o.flame(base[i], dir[i], h[i], 0.8F, time * 0.8F + i, argb(0.92F * fade, BLACK), argb(0.0F, BLACK));
			}
			o.sprite(new Vector3f(0, 1.0F, 0), s.camRight, s.camUp, 1.0F + r * 0.4F * out, argb(0.5F * fade * (1.0F - out * 0.6F), BLACK));
			for (List<Vector3f> b : bolts) {
				o.polyline(b, 0.02F, argb(0.95F * fade, BLACK));
			}
		});
		glow(ps, c, o -> {
			for (int i = 0; i < tongues; i++) {
				o.flame(new Vector3f(base[i]).add(new Vector3f(dir[i]).mul(-0.1F)), dir[i], h[i] * 1.1F, 1.0F, time * 0.8F + i, argb(0.35F * fade, CRIMSON),
					argb(0.0F, CRIMSON));
			}
			for (List<Vector3f> b : bolts) {
				o.glowPolyline(b, 0.08F, argb(0.85F * fade, CRIMSON));
				o.glowPolyline(b, 0.22F, argb(0.2F * fade, CRIMSON));
			}
			// the reach of the blast, where it stops
			o.ring(new Vector3f(0, 0.14F, 0), Math.max(0.0F, r - 0.35F), r, argb(0.6F * fade * out, CRIMSON));
			for (int w = 0; w < 2; w++) {
				float q = ease(age - w * 3.0F, 0.0F, 8.0F);
				if (q > 0.0F && q < 1.0F) {
					o.ring(new Vector3f(0, 0.15F + 0.01F * w, 0), Math.max(0.0F, r * q - 0.6F), r * q, argb(0.85F * (1.0F - q), w == 0 ? CRIMSON : CRIMSON_PALE));
				}
			}
			float flash = 1.0F - ease(age, 0.0F, 6.0F);
			o.star(new Vector3f(0, 1.0F, 0), s.camRight, s.camUp, r * 0.7F * flash + 0.3F, 8, 0.25F, s.seed, argb(0.9F * flash, CRIMSON_PALE), argb(0.0F, CRIMSON));
		});
	}

	/** Tyrant's Charge: black mana streaming off the path, the ground cracked red along it and red sparks thrown. */
	private static void tyrantTrail(final State s, final PoseStack ps, final SubmitNodeCollector c) {
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
		int tongues = Math.min(28, (int)(length / 0.5F));
		// cracks: a zigzag line along the ground with branches
		List<Vector3f> crack = new ArrayList<>();
		int steps = Math.max(2, (int)(length / 0.6F));
		for (int i = 0; i <= steps; i++) {
			float t = (float)i / steps;
			Vector3f at = VfxOut.along(points, last, t);
			Vector3f side = VfxOut.across(along(points, last, t));
			crack.add(at.add(new Vector3f(side).mul((hash(s.seed + i) - 0.5F) * 0.7F)).add(0, 0.14F, 0));
		}
		shade(ps, c, o -> {
			for (int i = 0; i <= tongues; i++) {
				float t = tongues == 0 ? 0.0F : (float)i / tongues;
				Vector3f at = VfxOut.along(points, last, t).add(0, 0.2F + 0.6F * hash(s.seed + i), 0);
				Vector3f back = along(points, last, t).negate().add(0, 0.9F, 0).normalize();
				o.flame(at, back, (1.0F + 1.4F * hash(i * 7)) * fade, 0.6F, time * 0.8F + i, argb(0.85F * fade, BLACK), argb(0.0F, BLACK));
			}
			o.polyline(crack, 0.06F, argb(0.8F * fade, 0x140408));
		});
		glow(ps, c, o -> {
			o.glowPolyline(crack, 0.12F, argb(0.85F * fade, CRIMSON));
			o.glowPolyline(crack, 0.35F, argb(0.25F * fade, CRIMSON));
			for (int i = 1; i < crack.size() - 1; i += 2) {
				Vector3f a = crack.get(i);
				Vector3f d = polar(Mth.TWO_PI * hash(s.seed * 3 + i), 0.5F + 0.5F * hash(i), 0.0F);
				o.bar(a, new Vector3f(a).add(d), 0.03F, 0.01F, argb(0.8F * fade, CRIMSON), argb(0.0F, CRIMSON));
			}
			for (int i = 0; i <= tongues; i += 2) {
				float t = tongues == 0 ? 0.0F : (float)i / tongues;
				Vector3f at = VfxOut.along(points, last, t).add(0, 0.2F + 0.6F * hash(s.seed + i), 0);
				Vector3f back = along(points, last, t).negate().add(0, 0.9F, 0).normalize();
				o.flame(at, back, (1.1F + 1.4F * hash(i * 7)) * fade, 0.8F, time * 0.8F + i, argb(0.3F * fade, CRIMSON), argb(0.0F, CRIMSON));
			}
			for (int i = 0; i < 12; i++) {
				float t = hash(s.seed * 5 + i);
				float q = (time * 0.07F + hash(i * 3)) % 1.0F;
				Vector3f at = VfxOut.along(points, last, t).add(BladeLooks.shardDir(s.seed, i).mul(0.3F + q)).add(0, 0.6F + q, 0);
				o.glint(at, s.camRight, s.camUp, 0.13F, time * 0.2F + i, argb(0.9F * fade * (1.0F - q), CRIMSON_PALE));
			}
			o.ring(new Vector3f(0, 0.15F, 0), 0.4F, 0.4F + 2.0F * ease(p, 0.0F, 0.4F), argb(0.6F * fade, CRIMSON));
		});
	}

	/**
	 * Black Armor: Saber Alter's armor of dark mana closes round the caster: black flame rising round them, curved black pauldrons on the
	 * shoulders, red lines running up the body like the armor's own, the visor's slit glowing and embers rising.
	 */
	private static void blackArmor(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.15F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		float top = s.selfView ? 0.9F : 2.3F;
		shade(ps, c, o -> {
			for (int i = 0; i < 12; i++) {
				float a = Mth.TWO_PI * i / 12 + time * 0.02F;
				Vector3f foot = polar(a, 0.55F, 0.05F);
				float h = Math.min(top, (1.3F + 0.9F * hash(i + (int)(time / 4))) * open);
				o.flame(foot, new Vector3f(-Mth.cos(a) * 0.15F, 1, -Mth.sin(a) * 0.15F), h, 0.55F, time * 0.6F + i, argb(0.7F * k, BLACK), argb(0.0F, BLACK));
			}
			if (!s.selfView) {
				// the pauldrons: curved black plates over each shoulder, a little lifted, settling as the armor closes
				float lift = (1.0F - open) * 0.5F;
				for (int side = -1; side <= 1; side += 2) {
					Vector3f center = new Vector3f(right).mul(side * 0.36F).add(0, 1.5F + lift, 0);
					for (int i = 0; i < 4; i++) {
						float a0 = -0.9F + 1.8F * i / 4;
						float a1 = -0.9F + 1.8F * (i + 1) / 4;
						for (int j = 0; j < 2; j++) {
							Vector3f p00 = pauldron(center, ahead, right, side, a0, j);
							Vector3f p10 = pauldron(center, ahead, right, side, a1, j);
							Vector3f p11 = pauldron(center, ahead, right, side, a1, j + 1);
							Vector3f p01 = pauldron(center, ahead, right, side, a0, j + 1);
							int col = argb(0.97F * k, j == 0 ? 0x24242C : 0x101016);
							o.quad(p00, p10, p11, p01, col);
						}
					}
				}
			}
		});
		glow(ps, c, o -> {
			// red lines climbing the body, turning at sharp angles like the armor's own lines
			float climb = ease(p, 0.0F, 0.3F);
			for (int i = 0; i < 8; i++) {
				float a = Mth.TWO_PI * i / 8 + 0.4F;
				float lineTop = (s.selfView ? 0.6F : 1.75F) * climb;
				Vector3f prev = polar(a, 0.36F, 0.1F);
				for (int j = 1; j <= 5; j++) {
					float y = 0.1F + lineTop * j / 5;
					float turn = (j % 2 == 0 ? 1 : -1) * 0.18F * hash(i * 5 + j);
					Vector3f next = polar(a + turn, 0.36F, y);
					o.bar(prev, next, 0.018F, 0.018F, argb(0.9F * k, CRIMSON), argb(0.9F * k, CRIMSON_PALE));
					prev = next;
				}
			}
			if (!s.selfView) {
				for (int side = -1; side <= 1; side += 2) {
					Vector3f center = new Vector3f(right).mul(side * 0.36F).add(0, 1.5F + (1.0F - open) * 0.5F, 0);
					Vector3f prev = null;
					for (int i = 0; i <= 4; i++) {
						Vector3f pt = pauldron(center, ahead, right, side, -0.9F + 1.8F * i / 4, 2);
						if (prev != null) {
							o.bar(prev, pt, 0.015F, 0.015F, argb(0.9F * k, CRIMSON), argb(0.9F * k, CRIMSON));
						}
						prev = pt;
					}
				}
				// the visor's slit
				Vector3f visor = new Vector3f(ahead).mul(0.27F).add(0, 1.66F, 0);
				o.bar(new Vector3f(right).mul(-0.14F).add(visor), new Vector3f(right).mul(0.14F).add(visor), 0.02F, 0.02F, argb(k, CRIMSON_PALE), argb(k, CRIMSON_PALE));
				o.sprite(visor, s.camRight, s.camUp, 0.25F, argb(0.6F * k, CRIMSON));
			}
			o.ring(new Vector3f(0, 0.14F, 0), 0.45F, 1.2F, argb(0.55F * k, CRIMSON));
			for (int i = 0; i < 12; i++) {
				float q = (time * 0.04F + hash(i * 3)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 9), 0.5F + 0.4F * hash(i), q * 2.4F);
				if (s.selfView && at.y > 1.0F) {
					continue;
				}
				o.sprite(at, s.camRight, s.camUp, 0.07F, argb(0.9F * k * Mth.sin(Mth.PI * q), 0xFF5A40));
			}
		});
	}

	/** A point on a pauldron over the {@code side} shoulder: {@code a} round from front to back, row {@code j} (0 top .. 2 rim) down its curve. */
	private static Vector3f pauldron(final Vector3f center, final Vector3f ahead, final Vector3f right, final int side, final float a, final int j) {
		float down = 0.35F * j;
		float r = 0.2F + 0.07F * j;
		return new Vector3f(ahead).mul(Mth.sin(a) * r).add(new Vector3f(right).mul(side * (0.05F + Mth.cos(a) * r * 0.6F + 0.08F * j))).add(0, -down * 0.5F + 0.08F, 0)
			.add(center);
	}

	private PendragonLooks() {
	}
}
