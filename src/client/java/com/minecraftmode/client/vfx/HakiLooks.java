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
 * Looks of the Sun God and the Pirate King (One Piece): Luffy's Gear 5 (Bajrang Gun, Red Hawk, Jet Gatling, the Drums of Liberation)
 * and Gol D. Roger's Conqueror's Haki (Kamusari, the clash of Conquerors, the Will of D. and the Jolly Roger of the Great Pirate Era).
 */
final class HakiLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final int NIKA = 0xFFFFFF;
	private static final int NIKA_SHADOW = 0x6E6A88;
	private static final int CLOUD = 0xF4F4FF;
	private static final int SUN = 0xFFE08A;
	private static final int FIRE = 0xFF5A1A;
	private static final int FIRE_CORE = 0xFFD27A;
	private static final int CRIMSON = 0xD01E2E;
	private static final int CRIMSON_PALE = 0xFF8A80;
	private static final int HAKI = 0x0A0306;
	private static final int GOLD = 0xFFD24A;
	private static final int GOLD_PALE = 0xFFF2B0;
	private static final int STRAW = 0xE8C060;
	/** How many of Jet Gatling's fists get the finer shape (the rest are blurred afterimages). */
	private static final int FINE_FISTS = 6;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case BAJRANG_GUN -> bajrangGun(s, ps, c);
			case RED_HAWK -> redHawk(s, ps, c);
			case JET_GATLING -> jetGatling(s, ps, c);
			case LIBERATION_DRUMS -> drums(s, ps, c);
			case KAMUSARI -> kamusari(s, ps, c);
			case HAKI_CLASH -> hakiClash(s, ps, c);
			case WILL_OF_D -> willOfD(s, ps, c);
			case PIRATE_ERA -> pirateEra(s, ps, c);
			default -> {
			}
		}
	}

	/** A quadratic curve from {@code a} through the pull of {@code ctrl} to {@code b}, {@code steps} + 1 points. */
	private static List<Vector3f> curve(final Vector3f a, final Vector3f ctrl, final Vector3f b, final int steps) {
		List<Vector3f> points = new ArrayList<>(steps + 1);
		for (int i = 0; i <= steps; i++) {
			float t = (float)i / steps;
			float u = 1.0F - t;
			points.add(new Vector3f(a).mul(u * u).add(new Vector3f(ctrl).mul(2 * u * t)).add(new Vector3f(b).mul(t * t)));
		}
		return points;
	}

	// ------------------------------------------------------------ Nika

	/**
	 * Gum-Gum Bajrang Gun: the caster's arm stretches up into the sky and swells into a gigantic white fist that comes down on the aimed
	 * spot, squashes, bounces back up like rubber and shrinks away; cartoon shock rings, stars and dust burst out where it lands.
	 */
	private static void bajrangGun(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float hit = 8.0F;
		float time = s.ageInTicks;
		float r = s.size;
		Vector3f f = new Vector3f(s.forward);
		Vector3f right = VfxOut.across(f);
		float away = Mth.clamp(s.arg, 3.0F, 22.0F);
		Vector3f shoulder = new Vector3f(f).mul(-away).add(new Vector3f(right).mul(0.35F)).add(0, 1.4F, 0);
		float size = r * 1.35F;
		float k = size * 0.5F;
		float swell = ease(age, 0.0F, hit);
		float fistSize = size * (0.35F + 0.65F * swell) * (1.0F - 0.7F * ease(age, 22.0F, 34.0F));
		Vector3f high = new Vector3f(f).mul(-Math.max(6.0F, away * 0.6F)).add(0, 11.0F, 0);
		Vector3f landed = new Vector3f(0, k * 0.95F, 0);
		float after = age - hit;
		float fall = Mth.clamp(age / hit, 0.0F, 1.0F);
		// down from the sky, then it springs back up like rubber and draws back towards the caster
		float bounce = Mth.sin(Mth.clamp(after / 8.0F, 0.0F, 1.0F) * Mth.PI) * 2.5F;
		Vector3f fist = after < 0.0F ? new Vector3f(high).lerp(landed, fall * fall)
			: new Vector3f(landed).add(0, bounce, 0).lerp(new Vector3f(shoulder).add(0, 6.0F, 0), ease(age, 20.0F, 34.0F));
		Vector3f punch = new Vector3f(landed).sub(high).normalize();
		Vector3f wrist = new Vector3f(punch).mul(-fistSize * 0.45F).add(fist);
		Vector3f ctrl = new Vector3f(shoulder).lerp(wrist, 0.45F).add(0, 9.0F, 0);
		List<Vector3f> arm = curve(shoulder, ctrl, wrist, 18);
		float fade = 1.0F - ease(s.progress, 0.85F, 1.0F);
		shade(ps, c, o -> {
			o.tube(arm, 0.22F, fistSize * 0.2F, 8, argb(0.97F * fade, NIKA), argb(0.97F * fade, NIKA_SHADOW));
			o.fist(fist, punch, f, fistSize, argb(0.98F * fade, NIKA), argb(0.98F * fade, NIKA_SHADOW), true);
			// its shadow growing on the ground, then the dent and the dust
			if (after < 0.0F) {
				o.disc(new Vector3f(0, 0.135F, 0), k * (0.4F + 0.8F * swell), argb(0.45F * swell, 0x101018), argb(0.0F, 0x101018));
			} else {
				o.disc(new Vector3f(0, 0.135F, 0), r * 1.1F, argb(0.55F * fade, 0x181410), argb(0.0F, 0x181410));
				float q = ease(after, 0.0F, 18.0F);
				for (int i = 0; i < 16; i++) {
					float a = Mth.TWO_PI * i / 16 + hash(s.seed + i);
					Vector3f at = polar(a, r * (0.8F + 1.0F * q), 0.5F + 1.2F * q * hash(i * 3));
					o.sprite(at, s.camRight, s.camUp, 1.0F + 1.6F * q, argb(0.45F * (1.0F - q), i % 2 == 0 ? 0xC8C0B0 : 0x9A9080));
				}
			}
		});
		glow(ps, c, o -> {
			o.sprite(fist, s.camRight, s.camUp, fistSize * 0.9F, argb(0.08F * fade, CLOUD));
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			if (after < 0.0F) {
				o.ring(ground, r - 0.3F, r, argb(0.7F, SUN));
				// speed lines down along its fall
				for (int i = 0; i < 8; i++) {
					Vector3f off = new Vector3f(right).mul((hash(s.seed + i) - 0.5F) * fistSize).add(new Vector3f(f).mul((hash(i * 3) - 0.5F) * fistSize * 0.6F));
					o.glowLine(new Vector3f(fist).add(off), new Vector3f(fist).add(off).sub(new Vector3f(punch).mul(4.0F)), 0.12F, argb(0.6F, 0xFFFFFF), argb(0.0F, 0xFFFFFF));
				}
				return;
			}
			float q = ease(after, 0.0F, 12.0F);
			float flash = 1.0F - ease(after, 0.0F, 6.0F);
			Vector3f mid = new Vector3f(0, 1.0F, 0);
			o.star(mid, s.camRight, s.camUp, r * 2.4F * flash + 0.5F, 12, 0.3F, s.seed, argb(0.95F * flash, 0xFFFFFF), argb(0.0F, SUN));
			for (int w = 0; w < 3; w++) {
				float qq = ease(after - w * 2.0F, 0.0F, 14.0F);
				if (qq <= 0.0F || qq >= 1.0F) {
					continue;
				}
				o.ring(new Vector3f(0, 0.16F + w * 0.01F, 0), r * 2.0F * qq, r * 2.0F * qq + 0.7F, argb(0.85F * (1.0F - qq), w == 1 ? SUN : 0xFFFFFF));
			}
			// cartoon stars and sparkles bouncing out
			for (int i = 0; i < 12; i++) {
				Vector3f d = BladeLooks.shardDir(s.seed, i);
				d.y = Math.abs(d.y) + 0.3F;
				Vector3f at = new Vector3f(d).mul(1.0F + r * 1.4F * q).add(0, 1.0F - 3.0F * q * q, 0);
				if (at.y > 0.2F) {
					o.star(at, s.camRight, s.camUp, 0.45F, 5, 0.45F, time * 0.2F + i, argb(0.95F * (1.0F - q), i % 2 == 0 ? SUN : 0xFFFFFF), argb(0.3F * (1.0F - q), SUN));
				}
			}
			for (int i = 0; i < 8; i++) {
				float a = Mth.TWO_PI * i / 8 + hash(s.seed * 3 + i);
				o.bolt(new Vector3f(0, 0.16F, 0), polar(a, r * 1.2F * ease(after, 0.0F, 5.0F), 0.16F), s.seed + i, 0.5F, 6, 0.12F, argb(0.6F * fade, SUN),
					argb(0.9F * fade, 0xFFFFFF));
			}
		});
	}

	/**
	 * Gum-Gum Red Hawk: the arm, coated black in Armament Haki, stretches out along the line and its fist bursts into flame, fire streaming
	 * back off it in two wings like a hawk's and along the arm, with a blast of fire where it lands.
	 */
	private static void redHawk(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float time = s.ageInTicks;
		Vector3f reach = new Vector3f(s.reach);
		float len = Math.max(1.0F, reach.length());
		Vector3f d = reach.lengthSquared() < 1.0E-6F ? new Vector3f(s.forward) : new Vector3f(reach).normalize();
		Vector3f right = VfxOut.across(d);
		Vector3f up = new Vector3f(right).cross(d).normalize();
		float ext = ease(age, 0.0F, 3.0F) * (1.0F - ease(age, 9.0F, 14.0F));
		Vector3f start = new Vector3f(d).mul(0.6F).add(new Vector3f(right).mul(0.35F)).add(0, -0.25F, 0);
		Vector3f fist = new Vector3f(d).mul((len - 0.6F) * ext).add(start);
		Vector3f wrist = new Vector3f(d).mul(-0.4F).add(fist);
		float fade = 1.0F - ease(s.progress, 0.7F, 1.0F);
		float flare = ease(age, 1.0F, 4.0F) * fade;
		shade(ps, c, o -> {
			if (wrist.distanceSquared(start) > 0.01F) {
				o.tube(List.of(start, new Vector3f(start).lerp(wrist, 0.5F), wrist), 0.13F, 0.16F, 7, argb(0.97F * fade, 0x5A4448), argb(0.97F * fade, 0x0A0506));
			}
			o.fist(fist, d, up, 0.75F, argb(0.98F * fade, 0x6A5058), argb(0.98F * fade, 0x080404), true);
		});
		glow(ps, c, o -> {
			o.sprite(fist, s.camRight, s.camUp, 1.3F * flare + 0.2F, argb(0.6F * flare, FIRE));
			o.sprite(fist, s.camRight, s.camUp, 0.6F * flare + 0.1F, argb(0.8F * flare, FIRE_CORE));
			Vector3f back = new Vector3f(d).negate();
			for (int i = 0; i < 10; i++) {
				float a = Mth.TWO_PI * i / 10 + time * 0.5F;
				Vector3f off = new Vector3f(right).mul(Mth.cos(a) * 0.3F).add(new Vector3f(up).mul(Mth.sin(a) * 0.3F));
				o.flame(new Vector3f(fist).add(off), back, (1.0F + 0.8F * hash(i + s.seed)) * flare, 0.45F, time * 0.9F + i, argb(0.85F * flare, FIRE_CORE),
					argb(0.0F, FIRE));
			}
			// two wings of fire swept back off the fist
			for (int side = -1; side <= 1; side += 2) {
				for (int i = 0; i < 5; i++) {
					Vector3f dir = new Vector3f(back).mul(1.0F).add(new Vector3f(right).mul(side * (0.6F + 0.25F * i))).add(new Vector3f(up).mul(0.25F - 0.12F * i))
						.normalize();
					o.flame(fist, dir, (1.6F - 0.2F * i) * flare, 0.5F, time * 0.8F + i + side, argb(0.8F * flare, i == 0 ? FIRE_CORE : 0xFF8A3A), argb(0.0F, FIRE));
				}
			}
			// fire running back along the arm
			float armLen = fist.distance(start);
			for (int i = 0; i < (int)(armLen / 0.8F); i++) {
				Vector3f at = new Vector3f(d).mul(0.6F + i * 0.8F).add(start);
				o.flame(at, new Vector3f(back).add(0, 0.6F, 0).normalize(), 0.7F * fade, 0.35F, time * 0.7F + i, argb(0.7F * fade, 0xFF8A3A), argb(0.0F, FIRE));
			}
			// the blast at full stretch
			float burst = ease(age, 3.0F, 9.0F);
			if (burst > 0.0F && burst < 1.0F) {
				Vector3f end = new Vector3f(d).mul(len - 0.6F).add(start);
				o.star(end, s.camRight, s.camUp, 2.2F * (1.0F - burst) + 0.4F, 10, 0.3F, s.seed, argb(0.9F * (1.0F - burst), FIRE_CORE), argb(0.0F, FIRE));
				o.circle(end, right, up, 2.0F * burst, 0.06F, 20, argb(0.8F * (1.0F - burst), FIRE));
			}
		});
	}

	/**
	 * Gum-Gum Jet Gatling: a storm of fists hammering forward faster than the eye, each a white afterimage on a streak of an arm, sparks
	 * where they land and steam puffing off the caster.
	 */
	private static void jetGatling(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float half = Math.max(30.0F, s.arg) * Mth.DEG_TO_RAD / 2;
		float r = s.size;
		Vector3f f = new Vector3f(s.forward);
		float fade = 1.0F - ease(p, 0.75F, 1.0F);
		float on = ease(age, 0.0F, 2.0F) * fade;
		int count = 10;
		Vector3f[] fists = new Vector3f[count];
		Vector3f[] dirs = new Vector3f[count];
		float[] ext = new float[count];
		Vector3f start = new Vector3f(f).mul(0.5F).add(0, 0.1F, 0);
		for (int i = 0; i < count; i++) {
			float a = (hash(s.seed + i * 7) - 0.5F) * 2 * half * 0.8F;
			Vector3f d = new Vector3f(f).rotateY(-a);
			d.y += (hash(s.seed * 3 + i) - 0.5F) * 0.5F;
			d.normalize();
			float phase = ((age + hash(i * 11) * 3.0F) / 3.0F) % 1.0F;
			ext[i] = Mth.sin(phase * Mth.PI);
			dirs[i] = d;
			fists[i] = new Vector3f(d).mul(0.6F + (r - 0.6F) * ext[i]).add(start);
		}
		Vector3f up = VfxOut.across(f).cross(f).normalize();
		shade(ps, c, o -> {
			for (int i = 0; i < count; i++) {
				o.fist(fists[i], dirs[i], up, 0.9F, argb(0.95F * on, 0xFFF8F4), argb(0.95F * on, 0x5A5068), i < FINE_FISTS);
			}
			for (int i = 0; i < 5; i++) {
				float q = (age * 0.06F + hash(s.seed + i)) % 1.0F;
				Vector3f at = new Vector3f(VfxOut.across(f)).mul((hash(i * 3) - 0.5F) * 1.2F).add(0, 0.3F + 1.2F * q, 0).add(new Vector3f(f).mul(-0.3F));
				o.sprite(at, s.camRight, s.camUp, 0.4F + 0.5F * q, argb(0.3F * fade * (1.0F - q), 0xFFE0E8));
			}
		});
		glow(ps, c, o -> {
			for (int i = 0; i < count; i++) {
				o.glowLine(start, fists[i], 0.1F, argb(0.1F * on, 0xFFFFFF), argb(0.3F * on, 0xFFFFFF));
				// speed lines behind each fist
				for (int k = 0; k < 2; k++) {
					Vector3f off = new Vector3f(VfxOut.across(dirs[i])).mul((k - 0.5F) * 0.35F);
					o.glowLine(new Vector3f(fists[i]).add(off), new Vector3f(fists[i]).add(off).sub(new Vector3f(dirs[i]).mul(1.0F + ext[i])), 0.05F,
						argb(0.6F * on, 0xFFFFFF), argb(0.0F, 0xFFFFFF));
				}
				if (ext[i] > 0.9F) {
					o.star(new Vector3f(dirs[i]).mul(0.3F).add(fists[i]), s.camRight, s.camUp, 0.5F, 6, 0.3F, i + age, argb(0.9F * on, SUN), argb(0.0F, 0xFFFFFF));
				}
			}
		});
	}

	/**
	 * Drums of Liberation: the heartbeat of the Sun God, "dum, dum, du-dum": each beat throws a ring of white cloud out from the caster's
	 * chest, a halo of white cloud-flame (Nika's mane) burns behind their head and white clouds rise round them.
	 */
	private static void drums(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float open = ease(p, 0.0F, 0.12F);
		float fade = 1.0F - ease(p, 0.82F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		float self = s.selfView ? 0.4F : 1.0F;
		List<Float> beats = new ArrayList<>();
		for (int bar = 0; bar < 3; bar++) {
			for (float b : new float[] {0.0F, 5.0F, 10.0F, 13.0F}) {
				beats.add(bar * 18.0F + b);
			}
		}
		shade(ps, c, o -> {
			for (float b : beats) {
				float q = (age - b) / 12.0F;
				if (q < 0.0F || q > 1.0F) {
					continue;
				}
				float rr = 0.6F + 3.2F * q;
				for (int i = 0; i < 14; i++) {
					Vector3f at = polar(Mth.TWO_PI * i / 14 + b, rr, 1.0F + 0.1F * Mth.sin(i * 2.0F));
					o.sprite(at, s.camRight, s.camUp, 0.45F + 0.3F * q, argb(0.55F * (1.0F - q) * fade * self, CLOUD));
				}
			}
			for (int i = 0; i < 10; i++) {
				float q = (time * 0.03F + hash(s.seed + i * 3)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 7) + q, 0.8F + 0.5F * hash(i), 0.2F + 2.4F * q);
				if (s.selfView && at.y > 1.0F) {
					continue;
				}
				o.sprite(at, s.camRight, s.camUp, 0.35F + 0.35F * q, argb(0.45F * k * Mth.sin(q * Mth.PI), CLOUD));
			}
		});
		glow(ps, c, o -> {
			float pulse = 0.0F;
			for (float b : beats) {
				float q = (age - b) / 12.0F;
				if (q >= 0.0F && q <= 1.0F) {
					pulse = Math.max(pulse, 1.0F - ease(age - b, 0.0F, 4.0F));
					o.ring(new Vector3f(0, 1.0F, 0), 0.6F + 3.2F * q - 0.15F, 0.6F + 3.2F * q, argb(0.7F * (1.0F - q) * fade * self, 0xFFFFFF));
					o.ring(new Vector3f(0, 0.15F, 0), 0.6F + 3.6F * q - 0.3F, 0.6F + 3.6F * q, argb(0.5F * (1.0F - q) * fade, SUN));
				}
			}
			Vector3f chest = new Vector3f(ahead).mul(0.25F).add(0, 1.2F, 0);
			o.sprite(chest, s.camRight, s.camUp, 0.4F + 0.5F * pulse, argb((0.3F + 0.6F * pulse) * k * self, 0xFFFFFF));
			if (!s.selfView) {
				// the Sun God's halo and mane of white cloud-flame behind the head
				Vector3f halo = new Vector3f(ahead).mul(-0.35F).add(0, 1.75F, 0);
				o.circle(halo, right, UP, 0.7F * open, 0.05F, 24, argb(0.9F * k, SUN));
				o.sprite(halo, right, UP, 1.0F + 0.2F * pulse, argb(0.35F * k, SUN));
				for (int i = 0; i < 14; i++) {
					float a = Mth.TWO_PI * i / 14 + time * 0.03F;
					Vector3f d = new Vector3f(right).mul(Mth.cos(a)).add(new Vector3f(UP).mul(Mth.sin(a) * 0.9F + 0.35F)).normalize();
					o.flame(new Vector3f(d).mul(0.55F * open).add(halo), d, (0.6F + 0.35F * hash(i + (int)(time / 4)) + 0.3F * pulse) * open, 0.45F, time * 0.5F + i,
						argb(0.85F * k, 0xFFFFFF), argb(0.0F, CLOUD));
				}
			}
		});
	}

	// ------------------------------------------------------------ the Pirate King

	/** Black lightning: {@code count} bolts from round {@code from} out to {@code length}, thrown anew every other tick; dark cores, red glow. */
	private static List<List<Vector3f>> blackBolts(final State s, final Vector3f from, final float spread, final float length, final int count, final boolean flat) {
		int seed = s.seed * 31 + (int)(s.ageInTicks / 2);
		List<List<Vector3f>> bolts = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			Vector3f d = BladeLooks.shardDir(seed, i);
			if (flat) {
				d.y *= 0.3F;
				d.normalize();
			}
			Vector3f a = new Vector3f(BladeLooks.shardDir(seed + 5, i)).mul(spread).add(from);
			bolts.add(VfxOut.zigzag(a, new Vector3f(d).mul(length * (0.5F + 0.5F * hash(seed + i))).add(a), seed + i, length * 0.12F, 6));
		}
		return bolts;
	}

	/**
	 * Kamusari: one great stroke of Conqueror's Haki across the front, a band of black edged in crimson, black lightning crackling off it
	 * into the ground and the air round it.
	 */
	private static void kamusari(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float age = s.age();
		float half = Math.max(30.0F, s.arg) * Mth.DEG_TO_RAD / 2;
		float r = s.size;
		Vector3f f = new Vector3f(s.forward);
		float swing = ease(age, 0.0F, 4.0F);
		float fade = 1.0F - ease(p, 0.5F, 1.0F);
		int n = 24;
		Vector3f[] inner = new Vector3f[n + 1];
		Vector3f[] outer = new Vector3f[n + 1];
		Vector3f[] edge = new Vector3f[n + 1];
		for (int i = 0; i <= n; i++) {
			float t = half - 2 * half * swing * i / n;
			float y = 0.8F - 1.4F * (half - t) / (2 * half);
			Vector3f d = new Vector3f(f).rotateY(-t);
			inner[i] = new Vector3f(d).mul(r * 0.35F).add(0, y, 0);
			outer[i] = new Vector3f(d).mul(r * 0.9F).add(0, y, 0);
			edge[i] = new Vector3f(d).mul(r).add(0, y, 0);
		}
		List<List<Vector3f>> bolts = new ArrayList<>();
		int seed = s.seed * 17 + (int)(s.ageInTicks / 2);
		for (int i = 0; i < 7; i++) {
			int at = Math.min(n, (int)(hash(seed + i) * n * swing));
			Vector3f from = new Vector3f(outer[at]);
			Vector3f d = BladeLooks.shardDir(seed, i);
			d.y = -Math.abs(d.y) - 0.3F;
			Vector3f to = new Vector3f(d).mul(1.5F + 1.5F * hash(i * 3)).add(from);
			to.y = Math.max(to.y, -1.1F);
			bolts.add(VfxOut.zigzag(from, to, seed + i, 0.35F, 6));
		}
		shade(ps, c, o -> {
			for (int i = 0; i < n; i++) {
				float lead = (float)(i + 1) / n;
				float al = fade * (0.35F + 0.6F * lead);
				o.quad(inner[i], outer[i], outer[i + 1], inner[i + 1], argb(0.0F, HAKI), argb(0.9F * al, HAKI), argb(0.9F * al, HAKI), argb(0.0F, HAKI));
			}
			for (List<Vector3f> b : bolts) {
				o.polyline(b, 0.07F, argb(0.95F * fade, HAKI));
			}
		});
		glow(ps, c, o -> {
			for (int i = 0; i < n; i++) {
				float lead = (float)(i + 1) / n;
				float al = fade * (0.35F + 0.6F * lead);
				o.quad(outer[i], edge[i], edge[i + 1], outer[i + 1], argb(0.85F * al, CRIMSON), argb(0.0F, CRIMSON), argb(0.0F, CRIMSON), argb(0.85F * al, CRIMSON));
				o.bar(outer[i], outer[i + 1], 0.04F, 0.04F, argb(al, CRIMSON_PALE), argb(al, CRIMSON_PALE));
				o.glowLine(inner[i], inner[i + 1], 0.25F, argb(0.4F * al, CRIMSON), argb(0.4F * al, CRIMSON));
			}
			for (List<Vector3f> b : bolts) {
				o.glowPolyline(b, 0.24F, argb(0.5F * fade, CRIMSON));
			}
			float flash = 1.0F - ease(age, 0.0F, 5.0F);
			o.sprite(new Vector3f(f).mul(1.0F), s.camRight, s.camUp, 1.5F * flash, argb(0.6F * flash, CRIMSON));
		});
	}

	/**
	 * Conqueror's Clash: black lightning bursts out of the caster in every direction, a dark dome of pressure swells round them edged in red,
	 * rings of crimson force roll out and two great black bolts split the sky overhead.
	 */
	private static void hakiClash(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = s.size;
		float fade = 1.0F - ease(p, 0.6F, 1.0F);
		float out = ease(age, 0.0F, 5.0F);
		Vector3f mid = new Vector3f(0, 1.2F, 0);
		List<List<Vector3f>> bolts = blackBolts(s, mid, 0.4F, r * out, 14, false);
		List<List<Vector3f>> sky = new ArrayList<>();
		int seed = s.seed * 7 + (int)(s.ageInTicks / 3);
		for (int i = 0; i < 2; i++) {
			sky.add(VfxOut.zigzag(new Vector3f(mid).add(0, 0.8F, 0), new Vector3f((hash(seed + i) - 0.5F) * 6.0F, 9.0F * out, (hash(seed * 3 + i) - 0.5F) * 6.0F), seed + i,
				1.2F, 9));
		}
		float dome = ease(age, 0.0F, 10.0F);
		shade(ps, c, o -> {
			for (List<Vector3f> b : bolts) {
				o.polyline(b, 0.1F, argb(0.95F * fade, HAKI));
			}
			for (List<Vector3f> b : sky) {
				o.polyline(b, 0.16F, argb(0.95F * fade, HAKI));
			}
			o.disc(new Vector3f(0, 0.135F, 0), r * 0.8F * dome, argb(0.55F * fade, 0x140408), argb(0.0F, 0x140408));
		});
		ps.pushPose();
		ps.translate(mid.x, mid.y, mid.z);
		c.submitCustomGeometry(ps, VfxDraw.SHADE, (pose, buffer) -> VfxDraw.ball(pose, buffer, r * 0.7F * dome + 0.3F, 0x200408, 0.18F * fade * (1.0F - dome * 0.6F)));
		ps.popPose();
		glow(ps, c, o -> {
			for (List<Vector3f> b : bolts) {
				o.glowPolyline(b, 0.3F, argb(0.45F * fade, CRIMSON));
			}
			for (List<Vector3f> b : sky) {
				o.glowPolyline(b, 0.4F, argb(0.6F * fade, CRIMSON));
			}
			float flash = 1.0F - ease(age, 0.0F, 6.0F);
			o.star(mid, s.camRight, s.camUp, r * 0.8F * flash + 0.3F, 8, 0.2F, s.seed, argb(0.9F * flash, CRIMSON_PALE), argb(0.0F, CRIMSON));
			for (int w = 0; w < 3; w++) {
				float q = ease(age - w * 2.0F, 0.0F, 14.0F);
				if (q <= 0.0F || q >= 1.0F) {
					continue;
				}
				Vector3f a = polar(w * 1.1F, 1.0F, 0.0F);
				Vector3f b = new Vector3f(UP).mul(Mth.cos(0.5F * w)).add(new Vector3f(a).cross(UP).normalize(Mth.sin(0.5F * w)));
				o.circle(mid, a, b, r * q, 0.06F, 36, argb(0.7F * (1.0F - q), CRIMSON));
			}
			float q = ease(age, 0.0F, 12.0F);
			o.ring(new Vector3f(0, 0.16F, 0), Math.max(0.0F, r * q - 0.8F), r * q, argb(0.8F * (1.0F - q), CRIMSON));
		});
		NavyLooks.ball(ps, c, mid, r * 0.7F * dome + 0.3F, 1.0F, CRIMSON, 0.08F * fade * (1.0F - dome * 0.5F));
	}

	/**
	 * Will of D.: a straw hat with a red band floats down out of the air, turning, and settles on the caster's head (Roger's hat, handed on),
	 * while a crimson aura of flame rises round them.
	 */
	private static void willOfD(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.82F, 1.0F);
		float time = s.ageInTicks;
		float settle = ease(p, 0.0F, 0.4F);
		float aura = ease(p, 0.1F, 0.3F) * fade;
		float self = s.selfView ? 0.45F : 1.0F;
		Vector3f crown = new Vector3f(0, Mth.lerp(settle, 4.6F, 1.9F), 0);
		float spin = (1.0F - settle) * 6.0F + time * 0.0F;
		float tilt = 0.35F * (1.0F - settle) * Mth.sin(time * 0.3F);
		Vector3f a = new Vector3f(Mth.cos(spin), Mth.sin(tilt), Mth.sin(spin)).normalize();
		Vector3f b = new Vector3f(-Mth.sin(spin), 0, Mth.cos(spin));
		Vector3f n = new Vector3f(a).cross(b).normalize().negate();
		if (n.y < 0) {
			n.negate();
		}
		shade(ps, c, o -> {
			if (s.selfView) {
				return;
			}
			int straw = argb(0.97F * fade, STRAW);
			int strawDark = argb(0.97F * fade, 0xA07828);
			// the brim
			int seg = 20;
			for (int i = 0; i < seg; i++) {
				float t0 = Mth.TWO_PI * i / seg;
				float t1 = Mth.TWO_PI * (i + 1) / seg;
				Vector3f i0 = ring(crown, a, b, 0.27F, t0);
				Vector3f i1 = ring(crown, a, b, 0.27F, t1);
				Vector3f o0 = ring(crown, a, b, 0.6F, t0).add(new Vector3f(n).mul(-0.04F));
				Vector3f o1 = ring(crown, a, b, 0.6F, t1).add(new Vector3f(n).mul(-0.04F));
				o.quad(i0, i1, o1, o0, i % 2 == 0 ? straw : VfxOut.lerp(straw, strawDark, 0.3F));
				// the crown's wall and the red band round its foot
				Vector3f w0 = new Vector3f(i0).add(new Vector3f(n).mul(0.1F));
				Vector3f w1 = new Vector3f(i1).add(new Vector3f(n).mul(0.1F));
				Vector3f c0 = new Vector3f(i0).add(new Vector3f(n).mul(0.28F)).sub(new Vector3f(i0).sub(crown).mul(0.12F));
				Vector3f c1 = new Vector3f(i1).add(new Vector3f(n).mul(0.28F)).sub(new Vector3f(i1).sub(crown).mul(0.12F));
				o.quad(i0, i1, w1, w0, argb(0.97F * fade, 0xC81E1E));
				o.quad(w0, w1, c1, c0, VfxOut.shadeBy(new Vector3f(i0).sub(crown).normalize(), straw, strawDark));
				o.tri(new Vector3f(crown).add(new Vector3f(n).mul(0.3F)), c0, c1, straw, straw, straw);
			}
			// the weave
			for (int i = 0; i < 12; i++) {
				float t = Mth.TWO_PI * i / 12;
				o.bar(ring(crown, a, b, 0.3F, t).add(new Vector3f(n).mul(0.012F)), ring(crown, a, b, 0.58F, t).add(new Vector3f(n).mul(-0.025F)), 0.006F, 0.006F, strawDark,
					strawDark);
			}
		});
		glow(ps, c, o -> {
			if (!s.selfView) {
				o.circle(crown, a, b, 0.6F, 0.02F, 24, argb(0.6F * fade, GOLD_PALE));
				if (settle < 1.0F) {
					o.sprite(crown, s.camRight, s.camUp, 0.9F, argb(0.3F * (1.0F - settle), GOLD_PALE));
				} else {
					float glint = 1.0F - ease(p, 0.4F, 0.55F);
					o.star(new Vector3f(crown).add(0, 0.3F, 0), s.camRight, s.camUp, 0.9F * glint, 4, 0.12F, 0.3F, argb(0.9F * glint, 0xFFFFFF), argb(0.0F, GOLD));
				}
			}
			// the crimson aura
			for (int i = 0; i < 12; i++) {
				float ang = Mth.TWO_PI * i / 12 + time * 0.02F;
				Vector3f foot = polar(ang, 0.65F, 0.1F);
				o.flame(foot, new Vector3f(-Mth.cos(ang) * 0.15F, 1, -Mth.sin(ang) * 0.15F), (1.4F + 0.8F * hash(i + (int)(time / 3))) * aura, 0.6F, time * 0.6F + i,
					argb(0.6F * aura * self, CRIMSON_PALE), argb(0.0F, CRIMSON));
			}
			o.ring(new Vector3f(0, 0.14F, 0), 0.5F, 1.1F, argb(0.6F * aura, CRIMSON));
			for (int i = 0; i < 8; i++) {
				float q = (time * 0.04F + hash(s.seed + i)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 3), 0.8F, q * 2.4F);
				if (s.selfView && at.y > 1.0F) {
					continue;
				}
				o.glint(at, s.camRight, s.camUp, 0.15F, i, argb(0.8F * aura * Mth.sin(q * Mth.PI), GOLD_PALE));
			}
		});
	}

	/** The point at {@code t} round a circle of {@code r} about {@code c} in the plane of {@code a} and {@code b}. */
	private static Vector3f ring(final Vector3f c, final Vector3f a, final Vector3f b, final float r, final float t) {
		return new Vector3f(a).mul(Mth.cos(t) * r).add(new Vector3f(b).mul(Mth.sin(t) * r)).add(c);
	}

	/**
	 * Dawn of the Great Pirate Era: the Pirate King's Jolly Roger is run up a pole behind the caster and streams out in the wind, treasure
	 * glittering up out of the ground round them and rings of gold washing out to everyone near.
	 */
	private static void pirateEra(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.85F, 1.0F);
		float hoist = ease(p, 0.0F, 0.3F);
		float time = s.ageInTicks;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f pole = new Vector3f(ahead).mul(-1.3F).add(new Vector3f(right).mul(0.6F));
		float poleTop = 5.4F;
		float width = 3.0F;
		float height = 2.0F;
		float top = Mth.lerp(hoist, 1.6F + height, poleTop - 0.1F);
		Vector3f fly = new Vector3f(right).negate();
		int cols = 8;
		int rows = 4;
		Vector3f[][] cloth = new Vector3f[cols + 1][rows + 1];
		for (int i = 0; i <= cols; i++) {
			float u = (float)i / cols;
			float amp = 0.45F * Math.max(0.0F, u - 0.55F) / 0.45F + 0.04F * u;
			for (int j = 0; j <= rows; j++) {
				float v = (float)j / rows;
				float wave = amp * Mth.sin(time * 0.3F - u * 6.0F + v * 0.8F);
				cloth[i][j] = new Vector3f(fly).mul(u * width).add(new Vector3f(ahead).mul(wave)).add(pole).add(0, top - v * height + 0.15F * u * u * Mth.sin(time * 0.2F),
					0);
			}
		}
		Vector3f skull = new Vector3f(fly).mul(width * 0.38F).add(pole).add(0, top - height * 0.5F, 0);
		shade(ps, c, o -> {
			o.bar(new Vector3f(pole), new Vector3f(pole).add(0, poleTop, 0), 0.07F, 0.05F, argb(0.97F * fade, 0x5A3A1A), argb(0.97F * fade, 0x3A2410));
			for (int i = 0; i < cols; i++) {
				for (int j = 0; j < rows; j++) {
					float slope = cloth[i + 1][j].dot(ahead) - cloth[i][j].dot(ahead);
					int col = VfxOut.lerp(argb(0.95F * fade, 0x1A1A1E), argb(0.95F * fade, 0x050506), Mth.clamp(0.5F + slope * 2.0F, 0.0F, 1.0F));
					o.quad(cloth[i][j], cloth[i + 1][j], cloth[i + 1][j + 1], cloth[i][j + 1], col);
				}
			}
			o.jollyRoger(skull, fly, UP, 0.62F, VfxOut.ROGER, argb(0.98F * fade, 0xF4F4F0), argb(0.98F * fade, 0x08080A));
			// coins thrown up out of the ground
			for (int i = 0; i < 10; i++) {
				float q = (time * 0.035F + hash(s.seed + i * 3)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 5), 1.2F + 1.8F * hash(i * 7), 0.2F + 2.2F * q * (1.2F - q));
				float spin = time * 0.4F + i;
				Vector3f cb = new Vector3f(Mth.cos(spin), 0, Mth.sin(spin));
				o.plate(at, cb, UP, 0.16F, 10, argb(0.95F * fade * Mth.sin(q * Mth.PI), 0xD8A820));
			}
		});
		glow(ps, c, o -> {
			o.sprite(new Vector3f(pole).add(0, poleTop + 0.1F, 0), s.camRight, s.camUp, 0.3F, argb(0.9F * fade, GOLD));
			for (int i = 0; i < cols; i++) {
				o.bar(cloth[i][0], cloth[i + 1][0], 0.02F, 0.02F, argb(0.6F * fade, GOLD), argb(0.6F * fade, GOLD));
				o.bar(cloth[i][rows], cloth[i + 1][rows], 0.02F, 0.02F, argb(0.6F * fade, GOLD), argb(0.6F * fade, GOLD));
			}
			o.sprite(skull, s.camRight, s.camUp, 1.2F, argb(0.12F * fade * hoist, 0xFFFFFF));
			for (int w = 0; w < 2; w++) {
				float q = ease(p, 0.15F + w * 0.2F, 0.6F + w * 0.2F);
				if (q <= 0.0F || q >= 1.0F) {
					continue;
				}
				o.ring(new Vector3f(0, 0.15F, 0), s.size * q - 0.6F, s.size * q, argb(0.75F * (1.0F - q), GOLD));
			}
			for (int i = 0; i < 16; i++) {
				float q = (time * 0.03F + hash(s.seed * 5 + i)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 11), 0.8F + 2.6F * hash(i * 13), 0.1F + 2.0F * q);
				o.glint(at, s.camRight, s.camUp, 0.22F, time * 0.1F + i, argb(0.9F * fade * Mth.sin(q * Mth.PI), i % 3 == 0 ? 0xFFFFFF : GOLD_PALE));
			}
			o.disc(new Vector3f(0, 0.14F, 0), 2.6F, argb(0.3F * fade * hoist, GOLD), argb(0.0F, GOLD));
		});
	}

	private HakiLooks() {
	}
}
