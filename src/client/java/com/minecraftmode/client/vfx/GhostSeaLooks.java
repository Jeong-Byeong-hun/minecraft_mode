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
 * Looks of the ghostly sea: Francis Drake's Golden Wild Hunt (the ghost fleet, its cannons and the Golden Hind), her storm voyage and
 * starlit wake (Fate), and the Flying Dutchman's kraken, tentacles, Davy Jones and the deep (Pirates of the Caribbean).
 */
final class GhostSeaLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final int GOLD = 0xFFD24A;
	private static final int GOLD_PALE = 0xFFF2B0;
	private static final int SPECTRAL = 0x7AFFD8;
	private static final int SEA = 0x2A78C8;
	private static final int SEA_DARK = 0x0A2A28;
	private static final int FOAM = 0xEAF6FF;
	private static final int GHOST = 0x3FC5A8;
	private static final int GHOST_PALE = 0xB8FFE8;
	private static final int FLESH = 0x8AB8A4;
	private static final int FLESH_DARK = 0x2A4A40;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case WILD_HUNT -> wildHunt(s, ps, c);
			case GHOST_CANNON -> ghostCannon(s, ps, c);
			case GOLDEN_HIND -> goldenHind(s, ps, c);
			case STORM_VOYAGE -> stormVoyage(s, ps, c);
			case STAR_WAKE -> starWake(s, ps, c);
			case KRAKEN -> kraken(s, ps, c);
			case KRAKEN_SLAM -> krakenSlam(s, ps, c);
			case TENTACLE_LASH -> tentacleLash(s, ps, c);
			case DAVY_JONES -> davyJones(s, ps, c);
			case DEEP_RISE -> deepRise(s, ps, c);
			default -> {
			}
		}
	}

	// ------------------------------------------------------------ Francis Drake

	/**
	 * Golden Wild Hunt: the ghost fleet rises out of a golden mist behind the caster, three spectral galleons broadside on, gold outlines
	 * and sails of pale gold over faint green-grey hulls, the guns along their near side flashing.
	 */
	private static void wildHunt(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float age = s.age();
		float rise = ease(p, 0.0F, 0.3F);
		float fade = 1.0F - ease(p, 0.78F, 1.0F);
		float k = rise * fade;
		float time = s.ageInTicks;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f[] centers = new Vector3f[3];
		float[] scale = {1.0F, 0.72F, 0.72F};
		for (int i = 0; i < 3; i++) {
			Vector3f back = new Vector3f(ahead).mul(i == 0 ? -3.6F : -7.5F).add(new Vector3f(right).mul(i == 0 ? 0.0F : (i == 1 ? 6.0F : -6.0F)));
			float hover = (i == 0 ? 1.4F : 3.4F) + 0.15F * Mth.sin(time * 0.1F + i * 2.0F) - (1.0F - rise) * 3.0F;
			centers[i] = back.add(0, hover, 0);
		}
		shade(ps, c, o -> {
			for (int i = 0; i < 3; i++) {
				float sc = scale[i];
				o.galleon(centers[i], right, 7.5F * sc, 2.2F * sc, 1.3F * sc, 3, 4.6F * sc, 0.5F * sc, argb(0.3F * k, 0x2A4A40), argb(0.35F * k, 0x0A1A16),
					argb(0.32F * k, 0xC8A040));
			}
			for (int i = 0; i < 10; i++) {
				Vector3f at = new Vector3f(ahead).mul(-3.0F - 4.0F * hash(s.seed + i)).add(new Vector3f(right).mul((hash(i * 7) - 0.5F) * 12.0F)).add(0,
					0.4F + 0.6F * hash(i * 3), 0);
				o.sprite(at, s.camRight, s.camUp, 1.6F + 0.8F * hash(i), argb(0.2F * k, 0x6A6A50));
			}
		});
		glow(ps, c, o -> {
			for (int i = 0; i < 3; i++) {
				float sc = scale[i];
				o.galleon(centers[i], right, 7.5F * sc, 2.2F * sc, 1.3F * sc, 3, 4.6F * sc, 0.5F * sc, argb(0.85F * k, GOLD), argb(0.6F * k, SPECTRAL),
					argb(0.55F * k, GOLD_PALE));
				// the guns on the near side, flashing in turn
				for (int g = 0; g < 6; g++) {
					Vector3f port = VfxOut.gunPort(centers[i], right, 7.5F * sc, 2.2F * sc, 1.3F * sc, g, 6, -1);
					o.sprite(port, s.camRight, s.camUp, 0.18F * sc, argb(0.8F * k, GOLD_PALE));
					float shot = (age - 4.0F - g * 2.3F - i * 1.1F) % 9.0F;
					if (age > 4.0F && shot >= 0.0F && shot < 3.0F) {
						NavyLooks.muzzleFlash(o, s, new Vector3f(ahead).mul(0.3F).add(port), new Vector3f(ahead), 0.4F * sc, (1.0F - shot / 3.0F) * fade, s.seed + g + i * 7);
					}
				}
			}
			// golden mist they rise from
			for (int i = 0; i < 14; i++) {
				float q = (time * 0.03F + hash(s.seed + i * 3)) % 1.0F;
				Vector3f at = new Vector3f(ahead).mul(-2.0F - 6.0F * hash(i * 5)).add(new Vector3f(right).mul((hash(i * 11) - 0.5F) * 13.0F)).add(0, 0.3F + 3.0F * q, 0);
				o.glint(at, s.camRight, s.camUp, 0.22F, time * 0.1F + i, argb(0.8F * k * Mth.sin(q * Mth.PI), GOLD_PALE));
			}
		});
	}

	/** One gun of the ghost fleet: a spectral barrel taking shape facing where its shot flies, the flash as it fires and a ring of mist. */
	private static void ghostCannon(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float age = s.age();
		float fade = 1.0F - ease(p, 0.45F, 1.0F);
		float flash = 1.0F - ease(age, 0.0F, 6.0F);
		Vector3f n = new Vector3f(s.reach);
		if (n.lengthSquared() < 1.0E-6F) {
			n.set(s.forward);
		}
		n.normalize();
		Vector3f a = VfxOut.across(n);
		Vector3f b = new Vector3f(n).cross(a).normalize();
		float r = Math.max(0.4F, s.size);
		// the barrel recoils back a little as it fires
		float kick = 0.3F * Mth.sin(Math.min(1.0F, age / 5.0F) * Mth.PI);
		Vector3f breech = new Vector3f(n).mul(-1.4F * r - kick);
		Vector3f muzzle = new Vector3f(n).mul(0.2F - kick);
		shade(ps, c, o -> {
			o.tube(List.of(breech, new Vector3f(breech).lerp(muzzle, 0.5F), muzzle), 0.3F * r, 0.22F * r, 8, argb(0.4F * fade, 0x3A5A50), argb(0.5F * fade, 0x0A1A16));
			o.sprite(new Vector3f(n).mul(0.6F), s.camRight, s.camUp, r * (0.6F + 1.4F * ease(age, 0.0F, 10.0F)), argb(0.35F * fade, 0x6A6458));
		});
		glow(ps, c, o -> {
			o.circle(breech, a, b, 0.32F * r, 0.03F, 12, argb(0.85F * fade, GOLD));
			o.circle(new Vector3f(breech).lerp(muzzle, 0.5F), a, b, 0.28F * r, 0.025F, 12, argb(0.7F * fade, GOLD));
			o.circle(muzzle, a, b, 0.26F * r, 0.035F, 12, argb(0.95F * fade, GOLD_PALE));
			for (int i = 0; i < 4; i++) {
				float ang = Mth.HALF_PI * i + 0.4F;
				Vector3f off = new Vector3f(a).mul(Mth.cos(ang)).add(new Vector3f(b).mul(Mth.sin(ang)));
				o.bar(new Vector3f(off).mul(0.3F * r).add(breech), new Vector3f(off).mul(0.24F * r).add(muzzle), 0.015F, 0.015F, argb(0.6F * fade, SPECTRAL),
					argb(0.6F * fade, SPECTRAL));
			}
			NavyLooks.muzzleFlash(o, s, new Vector3f(n).mul(0.3F), n, 0.55F * r, flash, s.seed);
			o.circle(new Vector3f(n).mul(0.4F + ease(age, 0.0F, 10.0F)), a, b, r * (0.3F + 1.2F * ease(age, 0.0F, 10.0F)), 0.03F, 16, argb(0.6F * fade, GOLD_PALE));
		});
	}

	/**
	 * The Golden Hind: Drake's flagship plunges bow first out of the sky beyond the caster onto the aimed spot, all golden outline, and
	 * bursts there into a fountain of gold coins, a pillar of golden light and shock rings, dissolving into sparks.
	 */
	private static void goldenHind(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float hit = 8.0F;
		float time = s.ageInTicks;
		float r = s.size;
		Vector3f f = new Vector3f(s.forward);
		float away = Math.max(4.0F, Math.min(16.0F, s.arg));
		Vector3f start = new Vector3f(f).mul(-Math.max(9.0F, away * 0.9F)).add(0, 7.0F, 0);
		Vector3f dir = new Vector3f(start).negate().normalize();
		float fall = Mth.clamp(age / hit, 0.0F, 1.0F);
		Vector3f bowTip = new Vector3f(start).lerp(new Vector3f(dir).mul(0.8F), fall * fall);
		float length = 8.0F;
		Vector3f center = new Vector3f(bowTip).sub(new Vector3f(dir).mul(length / 2));
		float ship = ease(age, 0.0F, 3.0F) * (1.0F - ease(age, hit + 1.0F, hit + 10.0F));
		float after = age - hit;
		float burst = after >= 0.0F ? 1.0F - ease(after, 0.0F, 22.0F) : 0.0F;
		shade(ps, c, o -> {
			if (ship > 0.01F) {
				o.galleon(center, dir, length, 2.6F, 1.4F, 3, 5.0F, 0.6F, argb(0.35F * ship, 0x6A5020), argb(0.4F * ship, 0x2A1A08), argb(0.4F * ship, 0xE8C060));
			}
			if (after >= 0.0F) {
				o.disc(new Vector3f(0, 0.135F, 0), r * 0.9F, argb(0.5F * burst, 0x2A1A08), argb(0.0F, 0x2A1A08));
				// gold coins flung out of the burst, spinning
				for (int i = 0; i < 18; i++) {
					Vector3f d = BladeLooks.shardDir(s.seed, i);
					d.y = 0;
					d.normalize();
					float q = ease(after, 0.0F, 18.0F);
					float h = 0.5F + (4.0F + 2.0F * hash(i)) * q - 6.5F * q * q;
					if (h < 0.2F && q > 0.5F) {
						continue;
					}
					Vector3f at = new Vector3f(d).mul(r * (0.3F + 1.1F * q * (0.6F + 0.4F * hash(i * 3)))).add(0, Math.max(0.2F, h), 0);
					float spin = time * 0.6F + i;
					Vector3f a = new Vector3f(d);
					Vector3f b = new Vector3f(0, Mth.cos(spin), 0).add(new Vector3f(d).cross(UP).mul(Mth.sin(spin))).normalize();
					o.plate(at, a, b, 0.2F, 10, argb(0.95F * burst, 0xD8A820));
				}
			}
		});
		glow(ps, c, o -> {
			if (ship > 0.01F) {
				o.galleon(center, dir, length, 2.6F, 1.4F, 3, 5.0F, 0.6F, argb(0.95F * ship, GOLD), argb(0.7F * ship, GOLD_PALE), argb(0.7F * ship, GOLD_PALE));
				// a wake of gold streaming off it
				for (int i = 0; i < 8; i++) {
					Vector3f at = new Vector3f(center).add(new Vector3f(dir).mul(-length * (0.2F + 0.6F * hash(i)))).add(VfxOut.across(dir).mul((hash(i * 3) - 0.5F) * 2.6F));
					o.glowLine(at, new Vector3f(at).sub(new Vector3f(dir).mul(3.0F)), 0.12F, argb(0.6F * ship, GOLD_PALE), argb(0.0F, GOLD));
				}
			}
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			if (after < 0.0F) {
				// the mark it is coming down on
				o.ring(ground, r * (1.0F - fall) + 0.2F, r * (1.0F - fall) + 0.5F, argb(0.8F, GOLD));
				o.ring(ground, r - 0.2F, r, argb(0.5F, GOLD));
				return;
			}
			float flash = 1.0F - ease(after, 0.0F, 6.0F);
			Vector3f mid = new Vector3f(0, 1.0F, 0);
			o.sprite(mid, s.camRight, s.camUp, r * 1.8F * flash + 0.3F, argb(0.9F * flash, 0xFFFFFF));
			o.star(mid, s.camRight, s.camUp, r * 2.2F * flash + 0.5F, 12, 0.22F, s.seed, argb(0.95F * flash, GOLD_PALE), argb(0.0F, GOLD));
			float q = ease(after, 0.0F, 14.0F);
			o.ring(ground, r * 1.6F * q, r * 1.6F * q + 0.7F, argb(0.85F * (1.0F - q), GOLD_PALE));
			o.ring(ground, r * 1.0F * q, r * 1.0F * q + 0.4F, argb(0.7F * (1.0F - q), GOLD));
			o.glowLine(new Vector3f(0, 0.2F, 0), new Vector3f(0, 9.0F * burst, 0), r * 0.4F, argb(0.45F * burst, GOLD), argb(0.0F, GOLD_PALE));
			for (int i = 0; i < 18; i++) {
				Vector3f d = BladeLooks.shardDir(s.seed, i);
				d.y = 0;
				d.normalize();
				float qq = ease(after, 0.0F, 18.0F);
				float h = 0.5F + (4.0F + 2.0F * hash(i)) * qq - 6.5F * qq * qq;
				if (h < 0.2F && qq > 0.5F) {
					continue;
				}
				Vector3f at = new Vector3f(d).mul(r * (0.3F + 1.1F * qq * (0.6F + 0.4F * hash(i * 3)))).add(0, Math.max(0.2F, h), 0);
				o.glint(at, s.camRight, s.camUp, 0.3F, time * 0.3F + i, argb(0.9F * burst, GOLD_PALE));
			}
		});
	}

	/**
	 * Voyager of the Storm: a ship's wheel of gold turning behind the caster, storm wind spiralling up round them and two swells rolling
	 * out over the ground as far as the allies it reaches.
	 */
	private static void stormVoyage(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.2F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f hub = new Vector3f(ahead).mul(-0.75F).add(0, 1.45F, 0);
		float spin = time * 0.08F;
		float wheel = 0.95F * open;
		float self = s.selfView ? 0.45F : 1.0F;
		shade(ps, c, o -> {
			if (!s.selfView) {
				o.plate(hub, right, UP, 0.14F * open, 10, argb(0.9F * k, 0x5A3A1A));
				for (int i = 0; i < 8; i++) {
					float a = spin + Mth.TWO_PI * i / 8;
					Vector3f d = new Vector3f(right).mul(Mth.cos(a)).add(new Vector3f(UP).mul(Mth.sin(a)));
					o.bar(new Vector3f(d).mul(0.1F).add(hub), new Vector3f(d).mul(wheel * 1.3F).add(hub), 0.04F, 0.03F, argb(0.9F * k, 0x7A5030), argb(0.9F * k, 0x5A3A1A));
				}
			}
			// the swells: low walls of sea rolling out
			for (int w = 0; w < 2; w++) {
				float q = ease(p, 0.05F + w * 0.2F, 0.6F + w * 0.2F);
				if (q <= 0.0F || q >= 1.0F) {
					continue;
				}
				o.wall(new Vector3f(0, 0.1F, 0), s.size * q, 0.05F, 0.55F * (1.0F - q * 0.5F), argb(0.4F * (1.0F - q), SEA), argb(0.0F, SEA));
			}
		});
		glow(ps, c, o -> {
			if (!s.selfView) {
				o.circle(hub, right, UP, wheel, 0.05F, 24, argb(0.95F * k, GOLD));
				o.circle(hub, right, UP, wheel * 0.85F, 0.025F, 24, argb(0.7F * k, GOLD_PALE));
				for (int i = 0; i < 8; i++) {
					float a = spin + Mth.TWO_PI * i / 8;
					Vector3f d = new Vector3f(right).mul(Mth.cos(a)).add(new Vector3f(UP).mul(Mth.sin(a)));
					o.sprite(new Vector3f(d).mul(wheel * 1.3F).add(hub), s.camRight, s.camUp, 0.1F, argb(0.9F * k, GOLD_PALE));
				}
				o.sprite(hub, s.camRight, s.camUp, 0.9F, argb(0.3F * k, GOLD));
			}
			// storm wind spiralling up round the caster
			for (int w = 0; w < 3; w++) {
				Vector3f prev = null;
				for (int i = 0; i <= 20; i++) {
					float t = i / 20.0F;
					float a = w * Mth.TWO_PI / 3 + t * 5.0F + time * 0.3F;
					Vector3f q = polar(a, 1.2F + 0.4F * t, 0.2F + 2.4F * t);
					if (prev != null) {
						float al = 0.6F * k * self * Mth.sin(t * Mth.PI);
						o.glowLine(prev, q, 0.06F, argb(al, 0xDFF6FF), argb(al, 0xDFF6FF));
					}
					prev = q;
				}
			}
			for (int w = 0; w < 2; w++) {
				float q = ease(p, 0.05F + w * 0.2F, 0.6F + w * 0.2F);
				if (q <= 0.0F || q >= 1.0F) {
					continue;
				}
				o.ring(new Vector3f(0, 0.62F * (1.0F - q * 0.5F), 0), s.size * q - 0.25F, s.size * q + 0.1F, argb(0.7F * (1.0F - q), FOAM));
				o.ring(new Vector3f(0, 0.15F, 0), s.size * q - 0.6F, s.size * q, argb(0.4F * (1.0F - q), GOLD));
			}
			// spray thrown by the wind
			for (int i = 0; i < 10; i++) {
				float q = (time * 0.05F + hash(s.seed + i)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 7) + time * 0.1F, 1.0F + 2.5F * q, 0.3F + 1.5F * Mth.sin(q * Mth.PI));
				o.glint(at, s.camRight, s.camUp, 0.15F, time * 0.2F + i, argb(0.8F * k * self * (1.0F - q), FOAM));
			}
		});
	}

	/**
	 * Pioneer of the Stars: a ship's wake left on the ground along the caster's path, a band of sea widening behind them with lines of foam
	 * at its edges, and stars glinting over it.
	 */
	private static void starWake(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		List<Vector3f> points = VfxOut.path(s.trail);
		int n = points.size();
		if (n < 2) {
			return;
		}
		float fade = 1.0F - ease(s.progress, 0.35F, 1.0F);
		float time = s.ageInTicks;
		float length = VfxOut.length(points, n - 1);
		int steps = Math.max(2, Math.min(24, (int)(length / 0.5F)));
		Vector3f[] left = new Vector3f[steps + 1];
		Vector3f[] right = new Vector3f[steps + 1];
		for (int i = 0; i <= steps; i++) {
			float t = (float)i / steps;
			Vector3f at = VfxOut.along(points, n - 1, t);
			Vector3f d = VfxOut.along(points, n - 1, Math.min(1.0F, t + 0.03F)).sub(VfxOut.along(points, n - 1, Math.max(0.0F, t - 0.03F)));
			d.y = 0;
			if (d.lengthSquared() < 1.0E-6F) {
				d.set(s.forward);
			}
			Vector3f side = VfxOut.across(d.normalize());
			// widest at the start, narrow at the bow
			float w = 0.35F + 1.2F * (1.0F - t);
			left[i] = new Vector3f(side).mul(w).add(at).add(0, 0.14F, 0);
			right[i] = new Vector3f(side).mul(-w).add(at).add(0, 0.14F, 0);
		}
		shade(ps, c, o -> {
			for (int i = 0; i < steps; i++) {
				o.quad(left[i], left[i + 1], right[i + 1], right[i], argb(0.4F * fade, SEA));
			}
		});
		glow(ps, c, o -> {
			for (int i = 0; i < steps; i++) {
				float al = 0.85F * fade;
				o.glowLine(left[i], left[i + 1], 0.16F, argb(al, FOAM), argb(al, FOAM));
				o.glowLine(right[i], right[i + 1], 0.16F, argb(al, FOAM), argb(al, FOAM));
				if (i % 3 == 0) {
					o.glowLine(new Vector3f(left[i]).lerp(right[i], 0.3F), new Vector3f(left[i + 1]).lerp(right[i + 1], 0.35F), 0.06F, argb(0.4F * fade, FOAM),
						argb(0.4F * fade, FOAM));
				}
			}
			for (int i = 0; i < 16; i++) {
				Vector3f at = VfxOut.along(points, n - 1, hash(s.seed + i * 3)).add(0, 0.5F + 2.0F * hash(i * 7), 0).add((hash(i * 5) - 0.5F) * 1.6F, 0,
					(hash(i * 11) - 0.5F) * 1.6F);
				float tw = 0.6F + 0.4F * Mth.sin(time * 0.6F + i * 1.7F);
				if (i % 3 == 0) {
					o.star(at, s.camRight, s.camUp, 0.3F * tw, 5, 0.45F, time * 0.05F + i, argb(0.95F * fade, GOLD_PALE), argb(0.2F * fade, GOLD));
				} else {
					o.glint(at, s.camRight, s.camUp, 0.25F * tw, time * 0.1F + i, argb(0.9F * fade, i % 2 == 0 ? 0xFFFFFF : GOLD_PALE));
				}
			}
			Vector3f bow = new Vector3f(points.getLast()).add(0, 0.15F, 0);
			o.ring(bow, 0.5F, 1.1F, argb(0.6F * fade, FOAM));
		});
	}

	// ------------------------------------------------------------ the Flying Dutchman

	/** Ghost light clinging round a tentacle, {@code width} out from its middle, so its dark flesh still shows at night. */
	private static void aura(final VfxOut o, final List<Vector3f> points, final float width, final int color) {
		for (int i = 0; i + 1 < points.size(); i++) {
			float k = 1.0F - 0.6F * i / points.size();
			o.glowLine(points.get(i), points.get(i + 1), width * k, color, color);
		}
	}

	/** One of the kraken's arms rising out of the sea at {@code base}: mottled grey-green flesh, pale suckers on its inner side. */
	private static void arm(final VfxOut shadeOut, final List<Vector3f> points, final float r0, final float r1, final float alpha) {
		shadeOut.tube(points, r0, r1, 7, argb(0.97F * alpha, FLESH), argb(0.97F * alpha, FLESH_DARK));
	}

	/**
	 * Release the Kraken: a whirlpool of dark green water opens on the aimed ground, its arms of foam winding in, a great eye glowing in its
	 * depths, and the kraken's arms rise out of it one after another, writhing and curling over before they sink again.
	 */
	private static void kraken(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float time = s.ageInTicks;
		float r = s.size;
		float open = ease(age, 0.0F, 10.0F);
		float sink = 1.0F - ease(s.progress, 0.85F, 1.0F);
		int count = 6;
		List<List<Vector3f>> arms = new ArrayList<>();
		List<Vector3f> inward = new ArrayList<>();
		float[] thick = new float[count];
		for (int i = 0; i < count; i++) {
			float a = Mth.TWO_PI * i / count + 0.5F * hash(s.seed + i);
			Vector3f base = polar(a, r * (0.45F + 0.25F * hash(s.seed * 3 + i)), -0.4F);
			Vector3f out = polar(a, 1.0F, 0.0F);
			float grow = ease(age - i * 3.0F, 2.0F, 18.0F) * sink;
			float len = (4.0F + 2.0F * hash(i * 7 + s.seed)) * grow;
			thick[i] = 0.7F * Math.max(0.2F, grow);
			arms.add(VfxOut.tentacle(base, UP, out, len + 0.05F, 0.45F, -2.6F, time * 0.12F + i * 1.9F, 12));
			inward.add(new Vector3f(out).negate());
		}
		shade(ps, c, o -> {
			o.disc(new Vector3f(0, 0.13F, 0), r * open, argb(0.75F * sink, SEA_DARK), argb(0.35F * sink, SEA_DARK));
			for (int i = 0; i < count; i++) {
				arm(o, arms.get(i), thick[i], 0.05F, 1.0F);
			}
			for (int i = 0; i < 8; i++) {
				Vector3f at = polar(Mth.TWO_PI * hash(s.seed + i * 5) + time * 0.01F, r * (0.7F + 0.5F * hash(i)), 0.5F + 0.4F * hash(i * 3));
				o.sprite(at, s.camRight, s.camUp, 1.4F, argb(0.25F * sink * open, 0x3A5A52));
			}
		});
		glow(ps, c, o -> {
			// the whirl's arms of foam winding in
			for (int w = 0; w < 4; w++) {
				Vector3f prev = null;
				for (int i = 0; i <= 18; i++) {
					float t = i / 18.0F;
					float a = w * Mth.HALF_PI + t * 3.4F - time * 0.15F;
					Vector3f q = polar(a, r * open * (1.0F - 0.85F * t), 0.15F + 0.02F * w);
					if (prev != null) {
						float al = 0.75F * sink * (0.3F + 0.7F * t);
						o.glowLine(prev, q, 0.14F, argb(al, GHOST), argb(al, GHOST_PALE));
					}
					prev = q;
				}
			}
			o.ring(new Vector3f(0, 0.16F, 0), r * open - 0.35F, r * open, argb(0.7F * sink, FOAM));
			// the eye in the depths
			float blink = Mth.clamp(1.0F - Math.abs(Mth.sin(time * 0.05F)) * 0.15F, 0.0F, 1.0F);
			Vector3f eye = new Vector3f(0, 0.17F, 0);
			Vector3f a = new Vector3f(s.forward).negate();
			Vector3f b = VfxOut.across(a);
			o.plate(eye, b, a, 0.9F * open, 16, argb(0.35F * sink, 0xC8FF60));
			o.bar(new Vector3f(b).mul(-0.6F * open * blink).add(eye), new Vector3f(b).mul(0.6F * open * blink).add(eye), 0.05F, 0.05F, argb(0.9F * sink, 0xFFFFA0),
				argb(0.9F * sink, 0xFFFFA0));
			for (int i = 0; i < count; i++) {
				o.suckers(arms.get(i), inward.get(i), thick[i], 0.05F, argb(0.55F * sink, GHOST_PALE));
				aura(o, arms.get(i), thick[i] * 1.6F, argb(0.3F * sink, GHOST));
				// the water streaming off where each arm breaks the surface
				Vector3f foot = new Vector3f(arms.get(i).getFirst()).add(0, 0.55F, 0);
				o.ring(foot, thick[i] + 0.1F, thick[i] + 0.45F, argb(0.6F * sink, FOAM));
			}
		});
	}

	/**
	 * The kraken's slam: one great arm rears up out of the ground just beyond the aimed spot, hooks over, and comes crashing down across
	 * it towards the caster with a burst of spray and a ring of sea, then slides back into the ground.
	 */
	private static void krakenSlam(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float time = s.ageInTicks;
		float r = s.size;
		float hit = 8.0F;
		Vector3f f = new Vector3f(s.forward);
		Vector3f toward = new Vector3f(f).negate();
		Vector3f base = new Vector3f(f).mul(r * 1.1F).add(0, -0.3F, 0);
		float grow = ease(age, 0.0F, 5.0F) * (1.0F - ease(age, 20.0F, 30.0F));
		float slam = ease(age, 4.0F, hit);
		float bend = Mth.lerp(slam, 0.25F, 2.5F);
		float curl = Mth.lerp(slam, 1.4F, 0.1F);
		float len = 9.0F * grow + 0.05F;
		List<Vector3f> arm = VfxOut.tentacle(base, UP, toward, len, bend, curl, time * 0.1F, 14);
		for (Vector3f q : arm) {
			q.y = Math.max(q.y, 0.3F);
		}
		float after = age - hit;
		shade(ps, c, o -> {
			o.disc(new Vector3f(base).add(0, 0.435F, 0), 1.6F * grow, argb(0.7F, SEA_DARK), argb(0.0F, SEA_DARK));
			arm(o, arm, 0.9F * Math.max(0.3F, grow), 0.08F, 1.0F);
			if (after >= 0.0F) {
				o.disc(new Vector3f(0, 0.135F, 0), r * 0.9F, argb(0.5F * (1.0F - ease(after, 10.0F, 22.0F)), SEA_DARK), argb(0.0F, SEA_DARK));
			}
		});
		glow(ps, c, o -> {
			o.suckers(arm, toward, 0.9F * Math.max(0.3F, grow), 0.08F, argb(0.5F, GHOST_PALE));
			aura(o, arm, 1.4F * Math.max(0.3F, grow), argb(0.3F, GHOST));
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			if (after < 0.0F) {
				o.ring(ground, r - 0.3F, r, argb(0.6F, GHOST));
				return;
			}
			float q = ease(after, 0.0F, 14.0F);
			o.ring(ground, r * 1.4F * q, r * 1.4F * q + 0.6F, argb(0.85F * (1.0F - q), FOAM));
			o.ring(ground, r * 0.8F * q, r * 0.8F * q + 0.35F, argb(0.7F * (1.0F - q), GHOST));
			// spray thrown up both sides of the arm
			for (int i = 0; i < 14; i++) {
				Vector3f d = BladeLooks.shardDir(s.seed, i);
				d.y = Math.abs(d.y) * 1.5F + 0.6F;
				d.normalize();
				Vector3f foot = polar(Mth.TWO_PI * hash(i), r * 0.4F * hash(i * 3), 0.2F);
				float h = 3.0F * q * (1.2F - q);
				o.blade(foot, new Vector3f(d).mul(1.0F + h * 1.5F).add(foot), VfxOut.across(d), 0.18F, argb(0.8F * (1.0F - q), FOAM), argb(0.0F, GHOST_PALE));
			}
			float flash = 1.0F - ease(after, 0.0F, 5.0F);
			o.sprite(new Vector3f(0, 0.8F, 0), s.camRight, s.camUp, r * 1.4F * flash + 0.2F, argb(0.6F * flash, GHOST_PALE));
		});
	}

	/**
	 * Tentacle Lash: a suckered tentacle lashes out of the caster's side and sweeps its arc low over the ground, its tip lagging behind
	 * like a whip, leaving a sheet of green water and spray at its tip.
	 */
	private static void tentacleLash(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float age = s.age();
		float half = Math.max(30.0F, s.arg) * Mth.DEG_TO_RAD / 2;
		float r = s.size;
		float fade = 1.0F - ease(p, 0.6F, 1.0F);
		float out = ease(age, 0.0F, 3.0F) * (1.0F - ease(p, 0.75F, 1.0F) * 0.6F);
		float sweep = half - 2.0F * half * ease(age, 1.0F, 8.0F);
		Vector3f f = new Vector3f(s.forward);
		Vector3f right = VfxOut.across(f);
		Vector3f root = new Vector3f(right).mul(0.35F).add(new Vector3f(f).mul(0.2F));
		float ground = -1.18F;
		List<Vector3f> arm = new ArrayList<>();
		int steps = 14;
		for (int j = 0; j <= steps; j++) {
			float t = (float)j / steps;
			float ang = sweep + 0.7F * t * t * (sweep > -half ? 1.0F : 0.0F);
			Vector3f d = new Vector3f(f).rotateY(-ang);
			float wave = 0.15F * Mth.sin(t * 7.0F - s.ageInTicks * 0.9F) * t;
			arm.add(new Vector3f(d).mul(0.3F + (r - 0.3F) * t * out).add(root).add(0, Mth.lerp(t, 0.0F, ground + 0.45F) + wave, 0));
		}
		Vector3f lead = new Vector3f(f).rotateY(-sweep + Mth.HALF_PI);
		shade(ps, c, o -> o.tube(arm, 0.32F, 0.05F, 7, argb(0.97F * fade, FLESH), argb(0.97F * fade, FLESH_DARK)));
		glow(ps, c, o -> {
			o.suckers(arm, lead, 0.32F, 0.05F, argb(0.6F * fade, GHOST_PALE));
			aura(o, arm, 0.55F, argb(0.35F * fade, GHOST));
			// the sheet of water swept over the arc
			int n = 18;
			float swept = ease(age, 1.0F, 8.0F);
			for (int i = 0; i < n; i++) {
				float t0 = half - 2 * half * swept * i / n;
				float t1 = half - 2 * half * swept * (i + 1) / n;
				Vector3f a0 = new Vector3f(f).rotateY(-t0).mul(r * 0.95F).add(0, ground + 0.4F, 0);
				Vector3f a1 = new Vector3f(f).rotateY(-t1).mul(r * 0.95F).add(0, ground + 0.4F, 0);
				Vector3f b0 = new Vector3f(f).rotateY(-t0).mul(r * 0.45F).add(0, ground + 0.3F, 0);
				Vector3f b1 = new Vector3f(f).rotateY(-t1).mul(r * 0.45F).add(0, ground + 0.3F, 0);
				float al = 0.45F * fade * (float)(i + 1) / n;
				o.quad(b0, a0, a1, b1, argb(0.0F, GHOST), argb(al, GHOST_PALE), argb(al, GHOST_PALE), argb(0.0F, GHOST));
			}
			Vector3f tip = arm.getLast();
			for (int i = 0; i < 6; i++) {
				Vector3f d = BladeLooks.shardDir(s.seed + (int)age, i);
				o.glint(new Vector3f(d).mul(0.5F).add(tip), s.camRight, s.camUp, 0.2F, i, argb(0.8F * fade, FOAM));
			}
		});
	}

	/**
	 * Do You Fear Death?: Davy Jones's face looms over the caster in ghost light, facing whoever looks: a dome of a head under a tricorn
	 * hat, glowing eyes, a beard of tentacles writhing beneath, and a green sea fog rolling out over the ground.
	 */
	private static void davyJones(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.2F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		float self = s.selfView ? 0.5F : 1.0F;
		Vector3f face = new Vector3f(s.camRight).cross(s.camUp).normalize();
		face.y = 0;
		if (face.lengthSquared() < 1.0E-4F) {
			face.set(s.forward);
		}
		face.normalize();
		Vector3f side = VfxOut.across(face);
		Vector3f head = new Vector3f(0, 3.6F + (1.0F - open) * 0.8F, 0);
		float hr = 0.95F;
		List<List<Vector3f>> beard = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			float x = (i - 4) / 4.0F;
			Vector3f base = new Vector3f(side).mul(x * hr * 0.7F).add(new Vector3f(face).mul(hr * 0.55F * (1.0F - x * x * 0.4F))).add(head).add(0, -hr * 0.45F, 0);
			List<Vector3f> t = VfxOut.tentacle(base, new Vector3f(0, -1, 0).add(new Vector3f(face).mul(0.25F)), new Vector3f(side).mul(x < 0 ? -1 : 1),
				(1.0F + 0.6F * hash(i + s.seed)) * open, 0.2F * x, 1.8F * (i % 2 == 0 ? 1 : -1), time * 0.15F + i * 1.3F, 7);
			beard.add(t);
		}
		shade(ps, c, o -> {
			o.ellipsoid(head, new Vector3f(side).mul(hr), new Vector3f(UP).mul(hr * 1.1F), new Vector3f(face).mul(hr), 7, 12, argb(0.55F * k * self, 0x8FD8C0),
				argb(0.6F * k * self, 0x163A32));
			for (List<Vector3f> t : beard) {
				o.tube(t, 0.11F, 0.02F, 5, argb(0.7F * k * self, 0x7FC8B0), argb(0.75F * k * self, 0x163A32));
			}
			// the tricorn: a brim turned up in three corners and its crown
			Vector3f brim = new Vector3f(head).add(0, hr * 0.75F, 0);
			for (int i = 0; i < 3; i++) {
				float a0 = Mth.TWO_PI * i / 3 + 0.5F;
				float a1 = Mth.TWO_PI * (i + 1) / 3 + 0.5F;
				Vector3f p0 = polar(a0, hr * 1.45F, 0.35F).add(brim);
				Vector3f p1 = polar(a1, hr * 1.45F, 0.35F).add(brim);
				Vector3f mid = polar((a0 + a1) / 2, hr * 0.95F, 0.0F).add(brim);
				o.tri(brim, p0, mid, argb(0.9F * k * self, 0x2A2420), argb(0.9F * k * self, 0x3A3028), argb(0.9F * k * self, 0x1A1410));
				o.tri(brim, mid, p1, argb(0.9F * k * self, 0x2A2420), argb(0.9F * k * self, 0x1A1410), argb(0.9F * k * self, 0x3A3028));
			}
			o.ellipsoid(new Vector3f(brim).add(0, hr * 0.25F, 0), new Vector3f(side).mul(hr * 0.75F), new Vector3f(UP).mul(hr * 0.42F), new Vector3f(face).mul(hr * 0.75F),
				5, 10, argb(0.9F * k * self, 0x3A3028), argb(0.9F * k * self, 0x120E0A));
			// dark sockets for the eyes
			for (int t = -1; t <= 1; t += 2) {
				o.plate(new Vector3f(face).mul(hr * 0.93F).add(new Vector3f(side).mul(t * 0.32F)).add(head).add(0, 0.12F, 0), side, UP, 0.18F, 10,
					argb(0.85F * k * self, 0x061410));
			}
			// the fog
			float roll = ease(p, 0.0F, 0.6F);
			for (int i = 0; i < 16; i++) {
				float a = Mth.TWO_PI * i / 16 + hash(s.seed + i);
				Vector3f at = polar(a, s.size * roll * (0.6F + 0.4F * hash(i * 3)), 0.5F + 0.3F * hash(i));
				o.sprite(at, s.camRight, s.camUp, 1.6F + 1.0F * roll, argb(0.3F * fade * (1.0F - roll * 0.5F), 0x4A6A60));
			}
		});
		glow(ps, c, o -> {
			o.sprite(head, s.camRight, s.camUp, 2.2F, argb(0.25F * k * self, GHOST));
			for (int t = -1; t <= 1; t += 2) {
				Vector3f eye = new Vector3f(face).mul(hr * 0.97F).add(new Vector3f(side).mul(t * 0.32F)).add(head).add(0, 0.12F, 0);
				o.sprite(eye, s.camRight, s.camUp, 0.3F, argb(0.9F * k, 0xC8FF80));
				o.sprite(eye, s.camRight, s.camUp, 0.08F, argb(k, 0xFFFFFF));
			}
			for (List<Vector3f> t : beard) {
				o.sprite(t.getLast(), s.camRight, s.camUp, 0.12F, argb(0.6F * k * self, GHOST_PALE));
			}
			float roll = ease(p, 0.0F, 0.6F);
			o.ring(new Vector3f(0, 0.15F, 0), s.size * roll - 0.5F, s.size * roll, argb(0.6F * fade * (1.0F - roll * 0.6F), GHOST));
			// ghost light dripping off the head
			for (int i = 0; i < 8; i++) {
				float q = (time * 0.05F + hash(s.seed + i * 3)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 5), hr * 0.8F, -hr * 0.4F - 1.6F * q).add(head);
				o.glint(at, s.camRight, s.camUp, 0.12F, i, argb(0.8F * k * self * (1.0F - q), GHOST_PALE));
			}
		});
	}

	/**
	 * Rise from the Deep: a whirlpool closing over where the caster went down, bubbles streaming along the way they went, and the sea
	 * bursting up round them where they rise, spray falling back and a green flash of ghost light.
	 */
	private static void deepRise(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		List<Vector3f> points = VfxOut.path(s.trail);
		int n = points.size();
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.5F, 1.0F);
		float time = s.ageInTicks;
		float close = 1.0F - ease(p, 0.3F, 0.9F);
		Vector3f end = new Vector3f(points.getLast());
		float burst = ease(p, 0.0F, 0.35F);
		shade(ps, c, o -> {
			o.disc(new Vector3f(0, 0.13F, 0), 1.9F * close, argb(0.75F, SEA_DARK), argb(0.2F, SEA_DARK));
			o.disc(new Vector3f(end).add(0, 0.13F, 0), 2.0F * burst, argb(0.45F * fade, SEA), argb(0.0F, SEA));
		});
		glow(ps, c, o -> {
			for (int w = 0; w < 3; w++) {
				Vector3f prev = null;
				for (int i = 0; i <= 14; i++) {
					float t = i / 14.0F;
					float a = w * Mth.TWO_PI / 3 + t * 3.2F - time * 0.3F;
					Vector3f q = polar(a, 1.9F * close * (1.0F - 0.85F * t), 0.15F);
					if (prev != null) {
						o.glowLine(prev, q, 0.1F, argb(0.7F * close, GHOST), argb(0.7F * close, GHOST_PALE));
					}
					prev = q;
				}
			}
			// bubbles along the way
			if (n >= 2) {
				for (int i = 0; i < 12; i++) {
					float t = hash(s.seed + i * 3);
					Vector3f at = VfxOut.along(points, n - 1, t).add(0, 0.4F + 1.6F * ((time * 0.05F + hash(i)) % 1.0F), 0);
					o.circle(at, s.camRight, s.camUp, 0.08F + 0.08F * hash(i * 7), 0.012F, 8, argb(0.8F * fade, FOAM));
				}
			}
			// the sea bursting up round them
			float fall = ease(p, 0.25F, 0.9F);
			for (int i = 0; i < 12; i++) {
				float a = Mth.TWO_PI * i / 12 + hash(s.seed + i);
				Vector3f foot = polar(a, 0.6F + 0.5F * burst, 0.15F).add(end);
				float h = (2.2F + 1.2F * hash(i * 3)) * burst * (1.0F - fall * 0.8F);
				Vector3f top = polar(a, 0.9F + 1.2F * burst, h).add(end);
				o.blade(foot, top, polar(a + Mth.HALF_PI, 1.0F, 0.0F), 0.22F, argb(0.8F * fade, FOAM), argb(0.0F, GHOST_PALE));
			}
			for (int i = 0; i < 14; i++) {
				Vector3f d = BladeLooks.shardDir(s.seed, i);
				d.y = Math.abs(d.y);
				Vector3f at = new Vector3f(d).mul(1.0F + 1.8F * burst).add(end).add(0, 2.5F * burst - 3.5F * fall * fall, 0);
				if (at.y - end.y > 0.2F) {
					o.glint(at, s.camRight, s.camUp, 0.16F, i, argb(0.9F * fade, FOAM));
				}
			}
			o.ring(new Vector3f(end).add(0, 0.16F, 0), 2.4F * burst, 2.4F * burst + 0.4F, argb(0.7F * (1.0F - burst), FOAM));
			float flash = 1.0F - ease(p, 0.0F, 0.3F);
			o.sprite(new Vector3f(end).add(0, 1.0F, 0), s.camRight, s.camUp, 2.0F * flash + 0.1F, argb(0.6F * flash, GHOST));
		});
	}

	private GhostSeaLooks() {
	}
}
