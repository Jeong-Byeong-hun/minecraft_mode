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
 * Looks of guns and cannons: the Corsair's Battleship, Octopus, Air Strike and Bullseye (MapleStory), cannon shots bursting, and the
 * Thunderclap Blunderbuss's broadside, buckshot and recoil.
 */
final class NavyLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final Vector3f X = new Vector3f(1, 0, 0);
	private static final Vector3f Z = new Vector3f(0, 0, 1);
	private static final int FIRE = 0xFF6A1A;
	private static final int FIRE_CORE = 0xFFD27A;
	private static final int SMOKE = 0x3A3530;
	private static final int WOOD = 0x7A5030;
	private static final int WOOD_DARK = 0x3A2412;
	private static final int CANVAS = 0xF2E8CC;
	private static final int IRON = 0x24282C;
	private static final int SEA = 0x2A78C8;
	private static final int FOAM = 0xEAF6FF;
	private static final int RED = 0xE02A2A;
	private static final int BOLT = 0xFFE066;
	/** How much bigger than life the Air Strike planes are drawn (they fly high). */
	private static final float PLANE = 2.2F;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case BATTLESHIP -> battleship(s, ps, c);
			case CANNON_BLAST -> cannonBlast(s, ps, c);
			case OCTOPUS -> octopus(s, ps, c);
			case AIR_STRIKE -> airStrike(s, ps, c);
			case BULLSEYE -> bullseye(s, ps, c);
			case BROADSIDE -> broadside(s, ps, c);
			case THUNDER_FLASH -> thunderFlash(s, ps, c);
			case BUCKSHOT -> buckshot(s, ps, c);
			case RECOIL -> recoil(s, ps, c);
			default -> {
			}
		}
	}

	/** A glowing ball of {@code radius} at {@code at}, squashed to {@code flat} of its height. */
	static void ball(final PoseStack ps, final SubmitNodeCollector c, final Vector3f at, final float radius, final float flat, final int rgb, final float alpha) {
		if (radius <= 0.01F || alpha <= 0.01F) {
			return;
		}
		ps.pushPose();
		ps.translate(at.x, at.y, at.z);
		ps.scale(1.0F, flat, 1.0F);
		c.submitCustomGeometry(ps, VfxDraw.GLOW, (pose, buffer) -> VfxDraw.ball(pose, buffer, radius, rgb, alpha));
		ps.popPose();
	}

	/** A cannon's flash at {@code muzzle} firing along {@code dir}: a star of fire, flames licking out and (on the shade pass) a puff of smoke. */
	static void muzzleFlash(final VfxOut o, final State s, final Vector3f muzzle, final Vector3f dir, final float size, final float k, final int seed) {
		if (k <= 0.01F) {
			return;
		}
		o.sprite(muzzle, s.camRight, s.camUp, size * 1.2F, argb(0.7F * k, FIRE));
		o.star(muzzle, s.camRight, s.camUp, size * 1.4F, 6, 0.3F, seed, argb(0.95F * k, 0xFFFFFF), argb(0.0F, FIRE_CORE));
		for (int i = 0; i < 3; i++) {
			Vector3f d = new Vector3f(dir).add(new Vector3f(BladeLooks.shardDir(seed, i)).mul(0.25F)).normalize();
			o.flame(muzzle, d, size * (1.2F + 0.6F * hash(seed + i)), size * 0.6F, seed + i, argb(0.85F * k, FIRE_CORE), argb(0.0F, FIRE));
		}
	}

	// ------------------------------------------------------------ the Corsair

	/**
	 * Battleship: the caster's own galleon comes together round them on a ghostly swell, the caster on its foredeck: hull, deck and rails
	 * of dark wood trimmed in gold, two masts behind with sails filling, guns run out along both sides firing one after another.
	 */
	private static void battleship(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float age = s.age();
		float build = ease(p, 0.0F, 0.25F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float k = build * fade;
		float time = s.ageInTicks;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		float length = 8.0F;
		float beam = 2.8F;
		float depth = 1.1F;
		float mast = 5.2F;
		// the caster stands on the foredeck; the deck sits just above the ground
		Vector3f deck = new Vector3f(ahead).mul(-1.0F).add(0, 0.16F - (1.0F - build) * 0.6F, 0);
		float self = s.selfView ? 0.5F : 1.0F;
		shade(ps, c, o -> {
			o.disc(new Vector3f(0, 0.13F, 0).add(new Vector3f(ahead).mul(-1.0F)), 5.2F * build, argb(0.4F * k, SEA), argb(0.0F, SEA));
			o.galleon(deck, ahead, length, beam, depth, 2, mast, 0.55F * build, argb(0.9F * k * self, WOOD), argb(0.95F * k * self, WOOD_DARK),
				argb(0.85F * k * self, CANVAS));
			// the deck's planks
			for (int i = -4; i <= 4; i++) {
				float t = i / 4.0F;
				float w = beam / 2 * (t > 0 ? Math.max(0.0F, 1.0F - t * t) : 1.0F - 0.22F * t * t);
				Vector3f m = new Vector3f(ahead).mul(t * length / 2).add(deck).add(0, 0.02F, 0);
				o.quad(new Vector3f(right).mul(-w).add(m), new Vector3f(right).mul(w).add(m),
					new Vector3f(right).mul(w).add(m).add(new Vector3f(ahead).mul(length / 8)), new Vector3f(right).mul(-w).add(m).add(new Vector3f(ahead).mul(length / 8)),
					argb(0.75F * k * self, i % 2 == 0 ? 0x8A6038 : 0x7A5430));
			}
			// the guns run out on both sides
			for (int side = -1; side <= 1; side += 2) {
				for (int g = 0; g < 4; g++) {
					Vector3f port = VfxOut.gunPort(deck, ahead, length, beam, depth, g, 4, side);
					Vector3f out = new Vector3f(right).mul(side * 0.6F * build).add(port);
					o.bar(new Vector3f(right).mul(-side * 0.2F).add(port), out, 0.1F, 0.085F, argb(0.95F * k, IRON), argb(0.95F * k, IRON));
				}
			}
			// powder smoke from the guns that have fired
			for (int side = -1; side <= 1; side += 2) {
				for (int g = 0; g < 4; g++) {
					float shot = age - fireTime(s, g, side);
					if (shot < 0.0F || shot > 16.0F) {
						continue;
					}
					Vector3f port = VfxOut.gunPort(deck, ahead, length, beam, depth, g, 4, side);
					float q = shot / 16.0F;
					Vector3f at = new Vector3f(right).mul(side * (0.9F + 1.6F * q)).add(port).add(0, 0.6F * q, 0);
					o.sprite(at, s.camRight, s.camUp, 0.5F + 0.9F * q, argb(0.5F * (1.0F - q) * fade, 0x8A847A));
				}
			}
		});
		glow(ps, c, o -> {
			o.galleon(deck, ahead, length, beam, depth, 2, mast, 0.55F * build, argb(0.75F * k * self, s.color), argb(0.35F * k * self, 0xB07A2A),
				argb(0.35F * k * self, s.color));
			// the swell it rides
			for (int i = 0; i < 3; i++) {
				float q = (time * 0.03F + i / 3.0F) % 1.0F;
				o.ring(new Vector3f(0, 0.15F, 0).add(new Vector3f(ahead).mul(-1.0F)), 3.0F + 2.5F * q, 3.3F + 2.5F * q, argb(0.45F * k * (1.0F - q), FOAM));
			}
			for (int side = -1; side <= 1; side += 2) {
				for (int g = 0; g < 4; g++) {
					Vector3f port = VfxOut.gunPort(deck, ahead, length, beam, depth, g, 4, side);
					Vector3f muzzle = new Vector3f(right).mul(side * 0.62F * build).add(port);
					o.circle(muzzle, new Vector3f(ahead), new Vector3f(UP), 0.1F, 0.02F, 8, argb(0.8F * k, s.color));
					float shot = age - fireTime(s, g, side);
					if (shot >= 0.0F && shot < 5.0F) {
						muzzleFlash(o, s, muzzle, new Vector3f(right).mul(side), 0.45F, (1.0F - shot / 5.0F) * fade, s.seed + g * 3 + side);
					}
				}
			}
			// gold motes gathering into the timbers while it forms
			if (build < 1.0F) {
				for (int i = 0; i < 14; i++) {
					float a = Mth.TWO_PI * hash(s.seed + i);
					Vector3f from = polar(a, 4.0F, 0.5F + 3.0F * hash(i * 5));
					Vector3f to = polar(a, 1.2F, 0.4F + 1.5F * hash(i * 3)).add(new Vector3f(ahead).mul(-1.0F));
					o.glint(new Vector3f(from).lerp(to, build), s.camRight, s.camUp, 0.2F, i, argb(0.9F * (1.0F - build), s.color));
				}
			}
		});
	}

	/** When gun {@code g} on {@code side} fires: a ripple down each side from the bow, a few ticks apart, once the ship has formed. */
	private static float fireTime(final State s, final int g, final int side) {
		return 12.0F + (3 - g) * 3.0F + (side > 0 ? 0.0F : 1.5F) + 2.0F * hash(s.seed + g * 5 + side);
	}

	/** A shot bursting: a white flash, a fireball swelling, a smoke ring rolling out, the shock running round and splinters flung. */
	private static void cannonBlast(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float r = Math.max(0.8F, s.size);
		float out = ease(p, 0.0F, 0.45F);
		float fade = 1.0F - ease(p, 0.35F, 1.0F);
		float flash = 1.0F - ease(p, 0.0F, 0.3F);
		shade(ps, c, o -> {
			for (int i = 0; i < 9; i++) {
				float a = Mth.TWO_PI * i / 9 + hash(s.seed + i);
				Vector3f at = polar(a, r * (0.3F + 0.6F * out), 0.2F + 0.9F * out * hash(i * 3));
				o.sprite(at, s.camRight, s.camUp, r * (0.35F + 0.35F * out), argb(0.55F * (1.0F - ease(p, 0.5F, 1.0F)), i % 3 == 0 ? 0x5A524A : SMOKE));
			}
		});
		glow(ps, c, o -> {
			Vector3f mid = new Vector3f(0, 0.3F, 0);
			o.sprite(mid, s.camRight, s.camUp, r * 1.6F * flash + 0.2F, argb(0.95F * flash, 0xFFFFFF));
			o.star(mid, s.camRight, s.camUp, r * 1.8F * flash + 0.3F, 10, 0.25F, s.seed, argb(0.9F * flash, FIRE_CORE), argb(0.0F, FIRE));
			o.ring(new Vector3f(0, 0.05F, 0), Math.max(0.0F, r * 1.3F * out - 0.5F), r * 1.3F * out, argb(0.8F * fade, FIRE_CORE));
			for (int i = 0; i < 12; i++) {
				Vector3f d = BladeLooks.shardDir(s.seed, i);
				d.y = Math.abs(d.y) * 0.9F + 0.15F;
				Vector3f at = new Vector3f(d).mul(r * 1.4F * out).add(0, -1.6F * out * out, 0).add(mid);
				o.bar(at, new Vector3f(d).mul(0.25F).add(at), 0.04F, 0.01F, argb(0.9F * fade, FIRE_CORE), argb(0.0F, FIRE));
			}
		});
		ball(ps, c, new Vector3f(0, 0.3F, 0), r * 0.75F * ease(p, 0.0F, 0.25F) + 0.1F, 0.85F, FIRE, 0.3F * fade);
		ball(ps, c, new Vector3f(0, 0.3F, 0), r * 0.4F * ease(p, 0.0F, 0.2F) + 0.1F, 0.9F, FIRE_CORE, 0.4F * fade);
	}

	/**
	 * Octopus: the Corsair's octopus gunner pops up in a pool of ink on the aimed ground, facing the caster: a round red head in a pirate's
	 * bandana, big eyes, eight arms waving round it and an ink squirt every so often, splashing dark over the pool.
	 */
	private static void octopus(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float time = s.ageInTicks;
		float pop = Mth.clamp(ease(age, 0.0F, 6.0F) * (1.0F + 0.15F * Mth.sin(Mth.clamp(age / 8.0F, 0.0F, 1.0F) * Mth.PI)), 0.0F, 1.2F);
		float sink = 1.0F - ease(s.progress, 0.9F, 1.0F);
		float k = Math.min(1.0F, pop) * sink;
		float r = s.size;
		Vector3f face = new Vector3f(s.forward).negate();
		Vector3f side = VfxOut.across(face);
		float bob = 0.08F * Mth.sin(time * 0.25F);
		Vector3f head = new Vector3f(0, (1.25F + bob) * pop * sink, 0);
		int body = argb(0.97F, 0xF06A4A);
		int bodyDark = argb(0.97F, 0x9A2A2A);
		List<List<Vector3f>> arms = new java.util.ArrayList<>();
		for (int i = 0; i < 8; i++) {
			float a = Mth.TWO_PI * i / 8 + 0.2F;
			Vector3f outward = polar(a, 1.0F, -0.25F).normalize();
			Vector3f base = polar(a, 0.45F, 0.55F * pop * sink);
			List<Vector3f> arm = VfxOut.tentacle(base, outward, UP, (1.6F + 0.4F * hash(i + s.seed)) * Math.min(1.0F, pop) * sink + 0.05F, -0.5F, 2.4F,
				time * 0.18F + i * 1.7F, 9);
			for (Vector3f q : arm) {
				q.y = Math.max(q.y, 0.2F);
			}
			arms.add(arm);
		}
		shade(ps, c, o -> {
			o.disc(new Vector3f(0, 0.13F, 0), r * ease(age, 0.0F, 8.0F), argb(0.6F * sink, 0x1A1028), argb(0.25F * sink, 0x1A1028));
			if (k <= 0.01F) {
				return;
			}
			for (List<Vector3f> arm : arms) {
				o.tube(arm, 0.2F, 0.04F, 6, body, bodyDark);
			}
			o.ellipsoid(head, new Vector3f(side).mul(0.8F * k), new Vector3f(UP).mul(0.95F * k), new Vector3f(face).mul(0.8F * k), 7, 12, body, bodyDark);
			// the siphon it squirts ink from
			List<Vector3f> siphon = List.of(new Vector3f(face).mul(0.55F * k).add(head).add(0, -0.45F * k, 0),
				new Vector3f(face).mul(0.85F * k).add(head).add(0, -0.55F * k, 0), new Vector3f(face).mul(1.05F * k).add(head).add(0, -0.5F * k, 0));
			o.tube(siphon, 0.16F * k, 0.12F * k, 6, body, bodyDark);
			// the bandana round its head, its knot behind
			List<Vector3f> band = new java.util.ArrayList<>();
			for (int i = 0; i <= 16; i++) {
				float a = Mth.TWO_PI * i / 16;
				band.add(new Vector3f(side).mul(Mth.cos(a) * 0.79F * k).add(new Vector3f(face).mul(Mth.sin(a) * 0.79F * k)).add(head).add(0, 0.3F * k, 0));
			}
			o.tube(band, 0.09F * k, 0.09F * k, 5, argb(0.97F, 0xE02020), argb(0.97F, 0x801010));
			Vector3f knot = new Vector3f(face).mul(-0.8F * k).add(head).add(0, 0.3F * k, 0);
			for (int t = -1; t <= 1; t += 2) {
				float sway = 0.15F * Mth.sin(time * 0.3F + t);
				o.tube(List.of(knot, new Vector3f(face).mul(-1.15F * k).add(new Vector3f(side).mul(t * 0.25F * k)).add(head).add(0, (0.05F + sway) * k, 0)), 0.07F * k,
					0.03F * k, 4, argb(0.97F, 0xE02020), argb(0.97F, 0x801010));
			}
			// big eyes on its face
			for (int t = -1; t <= 1; t += 2) {
				Vector3f eye = new Vector3f(face).mul(0.74F * k).add(new Vector3f(side).mul(t * 0.3F * k)).add(head).add(0, 0.02F * k, 0);
				o.plate(eye, side, UP, 0.22F * k, 12, argb(0.98F, 0xFFFFFF));
				Vector3f look = new Vector3f(face).mul(0.03F).add(eye).add(new Vector3f(side).mul(0.05F * Mth.sin(time * 0.1F) * k)).add(0, -0.03F * k, 0);
				o.plate(look, side, UP, 0.11F * k, 10, argb(0.98F, 0x101018));
				o.plate(new Vector3f(face).mul(0.01F).add(look).add(new Vector3f(side).mul(0.04F * k)).add(0, 0.04F * k, 0), side, UP, 0.035F * k, 6, argb(0.98F, 0xFFFFFF));
			}
			// ink: squirted out in blobs that splash on the pool
			for (int q = 0; q < 6; q++) {
				float start = 10.0F + q * 16.0F;
				float u = (age - start) / 9.0F;
				Vector3f from = siphon.get(2);
				Vector3f to = polar(Mth.TWO_PI * hash(s.seed + q * 7), r * (0.5F + 0.4F * hash(q * 3)), 0.14F);
				if (u > 0.0F && u < 1.0F) {
					Vector3f at = new Vector3f(from).lerp(to, u).add(0, 1.2F * Mth.sin(u * Mth.PI), 0);
					o.sprite(at, s.camRight, s.camUp, 0.3F, argb(0.9F, 0x140A1E));
				} else if (u >= 1.0F && u < 4.0F) {
					o.disc(to, 0.4F + 0.5F * ease(u, 1.0F, 1.6F), argb(0.8F * (1.0F - ease(u, 2.5F, 4.0F)) * sink, 0x0A0612), argb(0.0F, 0x0A0612));
				}
			}
		});
		glow(ps, c, o -> {
			o.ring(new Vector3f(0, 0.15F, 0), r - 0.3F, r, argb(0.55F * sink, 0x8A4AD0));
			if (k <= 0.01F) {
				return;
			}
			for (int i = 0; i < arms.size(); i++) {
				o.suckers(arms.get(i), UP, 0.2F, 0.04F, argb(0.5F * k, 0xFFD0C0));
			}
			for (int q = 0; q < 6; q++) {
				float start = 10.0F + q * 16.0F;
				float u = (age - start) / 9.0F;
				if (u > 0.0F && u < 1.0F) {
					Vector3f mouth = new Vector3f(face).mul(1.1F * k).add(head).add(0, -0.5F * k, 0);
					o.sprite(mouth, s.camRight, s.camUp, 0.4F * (1.0F - u), argb(0.6F, 0xB070FF));
				}
			}
		});
	}

	/** Where strike {@code i} of {@code Actions.strike} lands: the first on the aimed spot, the rest scattered a third of the radius round it. */
	static Vector3f strikeSpot(final State s, final int i) {
		if (i == 0) {
			return new Vector3f();
		}
		float scatter = s.size * 2.0F / 3.0F;
		return new Vector3f((hash(s.seed * 7 + i) - 0.5F) * scatter, 0, (hash(s.seed * 11 + i) - 0.5F) * scatter);
	}

	/**
	 * Air Strike: a red target circle laid on the aimed ground, and a flight of three planes passing over it, each bomb whistling down
	 * on its mark and bursting in a flash and smoke; scorch marks stay behind.
	 */
	private static void airStrike(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float time = s.ageInTicks;
		float r = s.size;
		float open = ease(age, 0.0F, 5.0F);
		float fade = 1.0F - ease(s.progress, 0.85F, 1.0F);
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		float height = 9.0F;
		int strikes = 5;
		// the leader over the spot at tick 8, a block and a half a tick
		Vector3f[] planes = new Vector3f[3];
		for (int i = 0; i < 3; i++) {
			float along = (age - 8.0F) * 1.5F - (i == 0 ? 0.0F : 4.5F);
			planes[i] = new Vector3f(f).mul(along).add(new Vector3f(side).mul(i == 0 ? 0.0F : (i == 1 ? 5.5F : -5.5F))).add(0, height + (i == 0 ? 0.6F : 0.0F), 0);
		}
		shade(ps, c, o -> {
			for (Vector3f plane : planes) {
				if (plane.lengthSquared() > 30 * 30) {
					continue;
				}
				plane(o, plane, new Vector3f(f).mul(PLANE), new Vector3f(side).mul(PLANE), argb(0.97F * fade, 0x7A8A62), argb(0.97F * fade, 0x2E3824));
			}
			for (int i = 0; i < strikes; i++) {
				float hit = 8.0F + i * 6.0F;
				Vector3f spot = strikeSpot(s, i);
				float fall = (age - (hit - 7.0F)) / 7.0F;
				if (fall > 0.0F && fall < 1.0F) {
					Vector3f from = new Vector3f(spot).add(new Vector3f(f).mul(-5.0F)).add(0, height - 0.5F, 0);
					Vector3f at = new Vector3f(from).lerp(spot, fall * fall).add(0, 0.3F, 0);
					Vector3f dir = new Vector3f(spot).sub(from).normalize();
					o.ellipsoid(at, new Vector3f(side).mul(0.16F), new Vector3f(dir).cross(side).normalize(0.16F), new Vector3f(dir).mul(0.34F), 4, 7, argb(0.97F, 0x5A6050),
						argb(0.97F, 0x161A12));
				}
				float after = age - hit;
				if (after > 0.0F) {
					o.disc(new Vector3f(spot).add(0, 0.135F, 0), r * 0.55F, argb(0.6F * fade, 0x1A1410), argb(0.0F, 0x1A1410));
					if (after < 18.0F) {
						float q = after / 18.0F;
						for (int j = 0; j < 5; j++) {
							Vector3f at = polar(Mth.TWO_PI * j / 5 + i, r * 0.3F * (0.4F + q), 0.5F + 1.6F * q).add(spot);
							o.sprite(at, s.camRight, s.camUp, 0.6F + 0.9F * q, argb(0.5F * (1.0F - q), SMOKE));
						}
					}
				}
			}
		});
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			o.ring(ground, (r - 0.2F) * open, r * open, argb(0.85F * fade, RED));
			o.ring(ground, r * 0.5F * open - 0.12F, r * 0.5F * open, argb(0.7F * fade, 0xFFFFFF));
			o.ring(ground, 0.0F, 0.25F, argb(0.9F * fade, RED));
			for (int t = 0; t < 4; t++) {
				float a = Mth.HALF_PI * t + time * 0.03F;
				o.bar(polar(a, r * 0.6F * open, 0.16F), polar(a, r * 1.2F * open, 0.16F), 0.05F, 0.05F, argb(0.85F * fade, RED), argb(0.85F * fade, RED));
			}
			for (Vector3f plane : planes) {
				if (plane.lengthSquared() > 30 * 30) {
					continue;
				}
				// the propeller's blur and the exhaust
				Vector3f nose = new Vector3f(f).mul(0.75F * PLANE).add(plane);
				o.circle(nose, side, new Vector3f(UP), 0.35F * PLANE, 0.04F, 12, argb(0.6F * fade, 0xE8E8E8));
				o.glowLine(new Vector3f(f).mul(-0.9F).add(plane), new Vector3f(f).mul(-3.5F).add(plane), 0.12F, argb(0.35F * fade, 0xFFFFFF), argb(0.0F, 0xFFFFFF));
			}
			for (int i = 0; i < strikes; i++) {
				float hit = 8.0F + i * 6.0F;
				Vector3f spot = strikeSpot(s, i);
				float fall = (age - (hit - 7.0F)) / 7.0F;
				if (fall > 0.0F && fall < 1.0F) {
					Vector3f from = new Vector3f(spot).add(new Vector3f(f).mul(-5.0F)).add(0, height - 0.5F, 0);
					Vector3f at = new Vector3f(from).lerp(spot, fall * fall).add(0, 0.3F, 0);
					Vector3f prev = new Vector3f(from).lerp(spot, Math.max(0.0F, fall - 0.3F) * Math.max(0.0F, fall - 0.3F)).add(0, 0.3F, 0);
					o.glowLine(prev, at, 0.08F, argb(0.0F, 0xFFFFFF), argb(0.6F, 0xFFE0C0));
					// the marker under it closing in
					o.ring(new Vector3f(spot).add(0, 0.16F, 0), 1.2F * (1.0F - fall), 1.2F * (1.0F - fall) + 0.12F, argb(0.9F, RED));
				}
				float after = age - hit;
				if (after >= 0.0F && after < 10.0F) {
					float q = after / 10.0F;
					Vector3f mid = new Vector3f(spot).add(0, 0.6F, 0);
					o.sprite(mid, s.camRight, s.camUp, r * 1.3F * (1.0F - q) + 0.3F, argb(0.9F * (1.0F - q), 0xFFFFFF));
					o.star(mid, s.camRight, s.camUp, r * 1.4F * (1.0F - q) + 0.4F, 8, 0.3F, i, argb(0.9F * (1.0F - q), FIRE_CORE), argb(0.0F, FIRE));
					o.ring(new Vector3f(spot).add(0, 0.16F, 0), r * q, r * q + 0.5F, argb(0.8F * (1.0F - q), FIRE_CORE));
					for (int j = 0; j < 4; j++) {
						Vector3f d = BladeLooks.shardDir(s.seed + i, j);
						d.y = Math.abs(d.y) + 0.4F;
						o.flame(new Vector3f(spot).add(0, 0.2F, 0), d, 1.6F * (1.0F - q) + 0.2F, 0.7F, time * 0.6F + j, argb(0.8F * (1.0F - q), FIRE_CORE),
							argb(0.0F, FIRE));
					}
				}
			}
		});
	}

	/** A small propeller plane at {@code c} flying along {@code f}: a fuselage, wings, a tail and its fin. */
	private static void plane(final VfxOut o, final Vector3f c, final Vector3f f, final Vector3f side, final int lit, final int dark) {
		Vector3f up = new Vector3f(side).cross(f).normalize();
		o.ellipsoid(c, new Vector3f(side).mul(0.2F), new Vector3f(up).mul(0.2F), new Vector3f(f).mul(0.85F), 5, 8, lit, dark);
		Vector3f wing = new Vector3f(f).mul(0.15F).add(c);
		o.quad(new Vector3f(side).mul(-1.3F).add(wing), new Vector3f(side).mul(1.3F).add(wing), new Vector3f(side).mul(1.2F).add(wing).add(new Vector3f(f).mul(-0.4F)),
			new Vector3f(side).mul(-1.2F).add(wing).add(new Vector3f(f).mul(-0.4F)), lit);
		Vector3f tail = new Vector3f(f).mul(-0.75F).add(c);
		o.quad(new Vector3f(side).mul(-0.5F).add(tail), new Vector3f(side).mul(0.5F).add(tail), new Vector3f(side).mul(0.45F).add(tail).add(new Vector3f(f).mul(-0.22F)),
			new Vector3f(side).mul(-0.45F).add(tail).add(new Vector3f(f).mul(-0.22F)), dark);
		o.tri(tail, new Vector3f(up).mul(0.45F).add(tail).add(new Vector3f(f).mul(-0.2F)), new Vector3f(f).mul(-0.25F).add(tail), dark, dark, dark);
		// red roundels on the wings
		for (int t = -1; t <= 1; t += 2) {
			o.plate(new Vector3f(side).mul(t * 0.85F).add(wing).add(new Vector3f(f).mul(-0.2F)).add(new Vector3f(up).mul(0.01F)), side, f, 0.13F, 8, argb(0.97F, RED));
		}
	}

	/**
	 * Bullseye: a target board of red and white rings stands over the target, turning slowly, while crosshair ticks close in on it from
	 * all four sides and lock on its middle with a flash.
	 */
	private static void bullseye(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float h = Math.max(0.6F, s.size);
		float w = Math.max(0.4F, s.arg);
		float p = s.progress;
		float lock = ease(p, 0.0F, 0.3F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float time = s.ageInTicks;
		Vector3f mid = new Vector3f(0, h * 0.55F, 0);
		float r = Math.max(0.55F, Math.max(w * 0.6F, h * 0.35F)) * 1.5F + 0.4F;
		Vector3f right = s.camRight;
		Vector3f up = s.camUp;
		Vector3f toward = new Vector3f(right).cross(up).normalize(0.02F);
		// stood a little in front of the target, towards the viewer
		mid.add(new Vector3f(toward).mul(30.0F));
		shade(ps, c, o -> {
			for (int i = 0; i < 4; i++) {
				o.plate(new Vector3f(toward).mul(i).add(mid), right, up, r * (1.0F - 0.24F * i) * lock, 20, argb(0.35F * fade, i % 2 == 0 ? RED : 0xFFFFFF));
			}
		});
		glow(ps, c, o -> {
			float spin = time * 0.05F;
			for (int i = 0; i < 4; i++) {
				o.circle(mid, right, up, r * (1.0F - 0.24F * i) * lock, 0.03F, 24, argb(0.9F * fade, i % 2 == 0 ? RED : 0xFFFFFF));
			}
			o.sprite(mid, right, up, 0.12F + 0.1F * lock, argb(fade, RED));
			// the crosshair: four ticks closing from far out, then the cross through the middle
			float gap = r * (1.0F + 2.0F * (1.0F - lock));
			for (int t = 0; t < 4; t++) {
				float a = spin + Mth.HALF_PI * t;
				Vector3f d = new Vector3f(right).mul(Mth.cos(a)).add(new Vector3f(up).mul(Mth.sin(a)));
				o.bar(new Vector3f(d).mul(gap).add(mid), new Vector3f(d).mul(gap + 0.5F).add(mid), 0.035F, 0.035F, argb(0.95F * fade, 0xFFFFFF), argb(0.95F * fade, RED));
				if (lock >= 1.0F) {
					o.bar(new Vector3f(d).mul(0.2F).add(mid), new Vector3f(d).mul(r * 0.95F).add(mid), 0.012F, 0.012F, argb(0.7F * fade, 0xFFFFFF),
						argb(0.7F * fade, 0xFFFFFF));
				}
			}
			float locked = ease(p, 0.3F, 0.4F) * (1.0F - ease(p, 0.4F, 0.6F));
			o.star(mid, right, up, r * 1.5F * locked, 4, 0.15F, 0.0F, argb(0.9F * locked, 0xFFFFFF), argb(0.0F, RED));
			o.ring(new Vector3f(0, 0.15F, 0), w * 0.5F + 0.4F, w * 0.5F + 0.55F, argb(0.7F * fade * lock, RED));
		});
	}

	// ------------------------------------------------------------ the Thunderclap Blunderbuss

	/**
	 * Thunderclap Broadside: a gold target circle laid on the aimed ground with lightning crawling round its rim, and each shot striking
	 * inside it with a flash and a thunder ring, leaving a scorch.
	 */
	private static void broadside(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float time = s.ageInTicks;
		float r = s.size;
		// the rim just outside the circle of sprites the rain lays down, so it shows past them
		float rim = r * 1.15F;
		float open = ease(age, 0.0F, 6.0F);
		float fade = 1.0F - ease(s.progress, 0.8F, 1.0F);
		int shots = 8;
		Vector3f[] spots = new Vector3f[shots];
		float[] hits = new float[shots];
		for (int i = 0; i < shots; i++) {
			// the shots fall on the enemies inside (a lone one takes them all): drawn round the middle
			spots[i] = polar(Mth.TWO_PI * hash(s.seed + i * 5), r * 0.4F * (float)Math.sqrt(hash(s.seed * 3 + i)), 0.0F);
			// fired from tick 4 two ticks apart, about 8 ticks falling from the sky
			hits[i] = 4.0F + i * 2.0F + 8.0F;
		}
		shade(ps, c, o -> {
			for (int i = 0; i < shots; i++) {
				if (age > hits[i]) {
					o.disc(new Vector3f(spots[i]).add(0, 0.135F, 0), 1.0F, argb(0.6F * fade, 0x161210), argb(0.0F, 0x161210));
				}
			}
		});
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			o.ring(ground, (rim - 0.3F) * open, rim * open, argb(0.9F * fade, s.color));
			o.ring(ground, (r * 0.62F - 0.12F) * open, r * 0.62F * open, argb(0.6F * fade, s.color));
			for (int t = 0; t < 12; t++) {
				float a = Mth.TWO_PI * t / 12 - time * 0.02F;
				o.bar(polar(a, (rim - 0.2F) * open, 0.16F), polar(a, (rim + 0.6F) * open, 0.16F), 0.04F, 0.04F, argb(0.8F * fade, s.color), argb(0.0F, s.color));
			}
			// lightning crawling round the rim, thrown anew every other tick
			int seed = s.seed * 7 + (int)(time / 2);
			for (int b = 0; b < 4; b++) {
				float a0 = Mth.TWO_PI * hash(seed + b * 3);
				float a1 = a0 + 0.5F + 0.6F * hash(seed + b * 5);
				o.bolt(polar(a0, rim * open, 0.3F), polar(a1, rim * open, 0.3F), seed + b, 0.5F, 6, 0.12F, argb(0.7F * fade, BOLT), argb(0.95F * fade, 0xFFFFFF));
			}
			for (int i = 0; i < shots; i++) {
				float after = age - hits[i];
				if (after < -6.0F || after > 10.0F) {
					continue;
				}
				Vector3f spot = new Vector3f(spots[i]).add(0, 0.16F, 0);
				if (after < 0.0F) {
					float q = 1.0F + after / 6.0F;
					o.ring(spot, 1.4F * (1.0F - q) + 0.2F, 1.4F * (1.0F - q) + 0.35F, argb(0.8F * q, BOLT));
					continue;
				}
				float q = after / 10.0F;
				Vector3f mid = new Vector3f(spot).add(0, 0.5F, 0);
				o.sprite(mid, s.camRight, s.camUp, 2.0F * (1.0F - q) + 0.2F, argb(0.85F * (1.0F - q), 0xFFFFFF));
				o.star(mid, s.camRight, s.camUp, 2.2F * (1.0F - q) + 0.3F, 8, 0.25F, i, argb(0.9F * (1.0F - q), BOLT), argb(0.0F, FIRE));
				o.ring(spot, 2.2F * q, 2.2F * q + 0.4F, argb(0.8F * (1.0F - q), BOLT));
				for (int j = 0; j < 3; j++) {
					Vector3f d = BladeLooks.shardDir(s.seed + i, j);
					d.y = Math.abs(d.y);
					o.bolt(mid, new Vector3f(d).mul(1.6F).add(mid), s.seed + i * 3 + j, 0.25F, 4, 0.1F, argb(0.8F * (1.0F - q), BOLT), argb(1.0F - q, 0xFFFFFF));
				}
			}
		});
	}

	/** One crack of thunder a cannonball falls out of: a flash, bolts thrown out round its line of fall and a ring of smoke. */
	private static void thunderFlash(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float flash = 1.0F - ease(p, 0.0F, 0.5F);
		float out = ease(p, 0.0F, 0.6F);
		Vector3f n = new Vector3f(s.reach);
		if (n.lengthSquared() < 1.0E-6F) {
			n.set(0, -1, 0);
		}
		n.normalize();
		Vector3f a = VfxOut.across(n);
		Vector3f b = new Vector3f(n).cross(a).normalize();
		float r = Math.max(0.5F, s.size);
		shade(ps, c, o -> o.sprite(new Vector3f(), s.camRight, s.camUp, r * (1.0F + 1.5F * out), argb(0.45F * (1.0F - out), 0x2A2A30)));
		glow(ps, c, o -> {
			Vector3f mid = new Vector3f();
			o.sprite(mid, s.camRight, s.camUp, r * 2.0F * flash + 0.1F, argb(0.85F * flash, BOLT));
			o.star(mid, s.camRight, s.camUp, r * 2.4F * flash + 0.1F, 6, 0.2F, s.seed, argb(flash, 0xFFFFFF), argb(0.0F, BOLT));
			o.circle(mid, a, b, r * (0.5F + 1.5F * out), 0.04F, 16, argb(0.8F * (1.0F - out), BOLT));
			for (int i = 0; i < 5; i++) {
				float ang = Mth.TWO_PI * i / 5 + hash(s.seed + i);
				Vector3f d = new Vector3f(a).mul(Mth.cos(ang)).add(new Vector3f(b).mul(Mth.sin(ang))).add(new Vector3f(n).mul(-0.3F)).normalize();
				o.bolt(mid, new Vector3f(d).mul(r * (1.5F + hash(i * 3)) * (0.5F + out)).add(mid), s.seed * 3 + i, 0.25F * r, 4, 0.1F, argb(0.8F * flash, BOLT),
					argb(flash, 0xFFFFFF));
			}
		});
	}

	/**
	 * Buckshot Storm: a roaring muzzle flash and a tongue of fire out of the bell, a storm of pellets streaking out over the whole cone and
	 * powder smoke billowing after them.
	 */
	private static void buckshot(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float half = Math.max(30.0F, s.arg) * Mth.DEG_TO_RAD / 2;
		Vector3f f = new Vector3f(s.forward);
		float r = s.size;
		float fade = 1.0F - ease(p, 0.5F, 1.0F);
		float flash = 1.0F - ease(p, 0.0F, 0.35F);
		Vector3f muzzle = new Vector3f(f).mul(0.9F);
		shade(ps, c, o -> {
			for (int i = 0; i < 10; i++) {
				float a = (hash(s.seed + i * 3) - 0.5F) * 2 * half * 0.7F;
				float d = 1.0F + (r * 0.75F) * hash(s.seed * 5 + i) * ease(p, 0.0F, 0.7F);
				Vector3f at = new Vector3f(f).rotateY(-a).mul(d).add(0, (hash(i * 7) - 0.3F) * 0.6F + 0.4F * p, 0);
				o.sprite(at, s.camRight, s.camUp, 0.5F + 1.2F * p, argb(0.45F * (1.0F - ease(p, 0.3F, 1.0F)), i % 2 == 0 ? 0x6A6458 : 0x4A443C));
			}
		});
		glow(ps, c, o -> {
			muzzleFlash(o, s, muzzle, f, 0.9F, flash, s.seed);
			o.flame(muzzle, f, 2.4F * flash + 0.2F, 1.4F, s.seed, argb(0.8F * flash, FIRE_CORE), argb(0.0F, FIRE));
			int pellets = 28;
			for (int i = 0; i < pellets; i++) {
				float a = (hash(s.seed + i * 13) - 0.5F) * 2 * half;
				float pitch = (hash(s.seed * 3 + i) - 0.45F) * 0.35F;
				float delay = hash(i * 7) * 0.15F;
				float q = ease(p, delay, delay + 0.45F);
				if (q <= 0.0F) {
					continue;
				}
				Vector3f d = new Vector3f(f).rotateY(-a);
				d.y += pitch;
				d.normalize();
				float far = 0.9F + (r - 0.9F) * q;
				Vector3f head = new Vector3f(d).mul(far);
				Vector3f tailEnd = new Vector3f(d).mul(Math.max(0.9F, far - 1.4F));
				o.glowLine(tailEnd, head, 0.06F, argb(0.0F, FIRE), argb(0.85F * fade, s.color));
				o.sprite(head, s.camRight, s.camUp, 0.09F, argb(fade, 0xFFFFFF));
				if (q >= 1.0F) {
					o.glint(head, s.camRight, s.camUp, 0.25F * (1.0F - ease(p, 0.5F, 0.8F)), i, argb(0.9F * fade, FIRE_CORE));
				}
			}
		});
	}

	/**
	 * Recoil Leap: where the caster was, the blunderbuss's blast thrown back the way they flew from (fire, a flash and a shock ring) and a
	 * streak of powder smoke along their flight with sparks in it.
	 */
	private static void recoil(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		List<Vector3f> points = VfxOut.path(s.trail);
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.35F, 1.0F);
		float flash = 1.0F - ease(p, 0.0F, 0.6F);
		Vector3f dir = new Vector3f(points.getLast()).sub(points.getFirst());
		dir.y = 0;
		if (dir.lengthSquared() < 1.0E-4F) {
			dir.set(s.forward);
		}
		dir.normalize();
		Vector3f back = new Vector3f(dir).negate();
		Vector3f blast = new Vector3f(0, 1.0F, 0);
		int n = points.size();
		shade(ps, c, o -> {
			for (int i = 0; i < 7; i++) {
				float q = ease(p, 0.0F, 0.6F);
				Vector3f at = new Vector3f(back).mul(0.6F + 2.2F * q * hash(s.seed + i)).add(blast).add(new Vector3f(VfxOut.across(back)).mul((hash(i * 5) - 0.5F) * 1.6F * q))
					.add(0, (hash(i * 3) - 0.5F) * q, 0);
				o.sprite(at, s.camRight, s.camUp, 0.6F + 1.0F * q, argb(0.5F * fade, i % 2 == 0 ? 0x5A544A : 0x3A3530));
			}
			if (n >= 2) {
				for (int i = 0; i < 8; i++) {
					Vector3f at = VfxOut.along(points, n - 1, (i + 0.5F) / 8).add(0, 0.9F, 0);
					o.sprite(at, s.camRight, s.camUp, 0.35F + 0.3F * p, argb(0.35F * fade * (1.0F - i / 10.0F), 0x6A6458));
				}
			}
		});
		glow(ps, c, o -> {
			muzzleFlash(o, s, new Vector3f(back).mul(0.6F).add(blast), back, 1.2F, flash, s.seed);
			o.flame(new Vector3f(back).mul(0.4F).add(blast), back, 3.6F * flash + 0.1F, 2.0F, s.seed, argb(0.8F * flash, FIRE_CORE), argb(0.0F, FIRE));
			float q = ease(p, 0.0F, 0.5F);
			o.ring(new Vector3f(0, 0.15F, 0), 2.6F * q, 2.6F * q + 0.45F, argb(0.7F * (1.0F - q), s.color));
			if (n >= 2) {
				for (int i = 0; i < 6; i++) {
					float from = hash(s.seed * 7 + i) * 0.7F;
					Vector3f a = VfxOut.along(points, n - 1, from).add(0, 0.6F + 0.8F * hash(i), 0);
					Vector3f b = VfxOut.along(points, n - 1, Math.min(1.0F, from + 0.25F)).add(0, 0.6F + 0.8F * hash(i), 0);
					o.glowLine(a, b, 0.05F, argb(0.0F, FIRE), argb(0.8F * fade, FIRE_CORE));
				}
			}
		});
	}

	private NavyLooks() {
	}
}
