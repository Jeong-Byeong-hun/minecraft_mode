package com.minecraftmode.client.vfx;

import static com.minecraftmode.client.vfx.SkillVfxRenderer.glow;
import static com.minecraftmode.client.vfx.SkillVfxRenderer.shade;
import static com.minecraftmode.client.vfx.VfxOut.argb;
import static com.minecraftmode.client.vfx.VfxOut.hash;

import com.minecraftmode.client.vfx.SkillProjectileRenderer.State;
import com.minecraftmode.job.skill.SkillLook;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * The pirate's projectile looks: cannonballs ablaze or wrapped in lightning, Drake's gold coins, Anne's musket balls and the bird of the
 * Free Bird, Big Mom's Ikoku wave, Whitebeard's tsunami, the Pirate King's Kamusari and an Armament-coated bullet. Trails never reach
 * further back than the shot has flown.
 */
final class PirateShots {
	private static final int FIRE = 0xFF6A1A;
	private static final int FIRE_CORE = 0xFFD27A;
	private static final int SMOKE = 0x3A3530;
	private static final int GOLD = 0xFFD24A;
	private static final int GOLD_PALE = 0xFFF2B0;
	private static final int SKY = 0x7FDBFF;
	private static final int PINK = 0xFF7AB8;
	private static final int PINK_PALE = 0xFFD8EA;
	private static final int SEA = 0x2A78C8;
	private static final int SEA_DEEP = 0x123E78;
	private static final int FOAM = 0xEAF6FF;
	private static final int CRIMSON = 0xD01E2E;
	private static final int BOLT = 0xFFE066;
	private static final int HAKI = 0x1A0A14;
	/** The tsunami's profile (along its flight, up): the back slope, over the crest and the lip curling down in front. */
	private static final float[][] PROFILE = {{-1.45F, 0.0F}, {-1.1F, 0.7F}, {-0.75F, 1.4F}, {-0.35F, 1.95F}, {0.15F, 2.2F}, {0.6F, 2.0F}, {0.85F, 1.5F},
		{0.65F, 1.05F}};

	/** How far round the projectile its look reaches (its culling box grows by this); 0 for the looks of other classes. */
	static double reach(final SkillLook look) {
		return switch (look) {
			case CANNON_SHOT, THUNDER_SHOT -> 4.0;
			case COIN_SHOT, MUSKET_SHOT, HAKI_SHOT, BIRD_SHOT -> 3.0;
			case IKOKU_WAVE -> 5.0;
			case TSUNAMI, KAMUSARI_WAVE -> 4.0;
			default -> 0.0;
		};
	}

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case CANNON_SHOT -> cannonShot(s, ps, c);
			case COIN_SHOT -> coinShot(s, ps, c);
			case BIRD_SHOT -> birdShot(s, ps, c);
			case MUSKET_SHOT -> musketShot(s, ps, c);
			case IKOKU_WAVE -> ikokuWave(s, ps, c);
			case TSUNAMI -> tsunami(s, ps, c);
			case KAMUSARI_WAVE -> kamusariWave(s, ps, c);
			case THUNDER_SHOT -> thunderShot(s, ps, c);
			case HAKI_SHOT -> hakiShot(s, ps, c);
			default -> {
			}
		}
	}

	/** How far behind the projectile a trail of {@code length} may reach: no further than it has flown. */
	private static float trail(final State s, final float length) {
		return Math.min(length, Math.max(0.0F, s.ageInTicks * Math.max(0.6F, s.speed) - 0.4F));
	}

	/** Puffs of powder smoke along the wake, growing and thinning further back. */
	private static void smoke(final State s, final PoseStack ps, final SubmitNodeCollector c, final float tail, final int puffs, final float size, final int rgb) {
		Vector3f back = new Vector3f(s.forward).negate();
		shade(ps, c, o -> {
			for (int i = 0; i < puffs; i++) {
				float d = 0.5F + (tail - 0.5F) * i / Math.max(1, puffs - 1);
				if (d > tail || d < 0.3F) {
					continue;
				}
				float k = d / Math.max(0.5F, tail);
				Vector3f at = new Vector3f(back).mul(d).add(0, 0.15F * k + (hash(s.seed + i) - 0.5F) * 0.2F, 0);
				o.sprite(at, s.camRight, s.camUp, size * (0.5F + 0.8F * k), argb(0.5F * (1.0F - k), rgb));
			}
		});
	}

	/** A cannonball ablaze: the ball in a halo of fire, flames streaming back off it and powder smoke rolling out behind. */
	private static void cannonShot(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f back = new Vector3f(s.forward).negate();
		float tail = trail(s, 3.4F);
		float time = s.ageInTicks;
		Vector3f u = VfxOut.across(back);
		Vector3f v = new Vector3f(u).cross(back).normalize();
		smoke(s, ps, c, tail, 6, 0.7F, SMOKE);
		glow(ps, c, o -> {
			Vector3f head = new Vector3f();
			o.sprite(head, s.camRight, s.camUp, 0.85F, argb(0.55F, FIRE));
			o.sprite(head, s.camRight, s.camUp, 0.45F, argb(0.8F, VfxOut.lerp(0xFF000000 | FIRE_CORE, 0xFF000000 | s.color, 0.3F)));
			for (int i = 0; i < 6; i++) {
				float a = Mth.TWO_PI * i / 6 + time * 0.5F;
				Vector3f off = new Vector3f(u).mul(Mth.cos(a) * 0.18F).add(new Vector3f(v).mul(Mth.sin(a) * 0.18F));
				o.flame(off, back, Math.min(tail, 0.9F + 0.8F * hash(i + s.seed)), 0.4F, time * 0.9F + i, argb(0.85F, FIRE_CORE), argb(0.0F, FIRE));
			}
			for (int i = 0; i < 5; i++) {
				float d = 0.4F + 0.5F * i;
				if (d > tail) {
					continue;
				}
				Vector3f at = new Vector3f(back).mul(d).add(new Vector3f(u).mul((hash(s.seed * 3 + i + (int)time) - 0.5F) * 0.5F)).add(0, (hash(i * 7 + (int)time) - 0.5F) * 0.4F,
					0);
				o.glint(at, s.camRight, s.camUp, 0.14F, time * 0.3F + i, argb(0.9F * (1.0F - d / 3.0F), FIRE_CORE));
			}
		});
	}

	/** Golden Rule: a gold coin of light spinning end over end, a golden streak and glints falling off it. */
	private static void coinShot(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f back = new Vector3f(s.forward).negate();
		float tail = trail(s, 2.4F);
		float spin = s.ageInTicks * 0.9F + s.seed;
		Vector3f side = VfxOut.across(s.forward);
		// the coin's face turns round the flight's sideways axis
		Vector3f a = new Vector3f(side);
		Vector3f b = new Vector3f(s.forward).mul(Mth.cos(spin)).add(new Vector3f(0, 1, 0).mul(Mth.sin(spin)));
		if (b.lengthSquared() < 1.0E-4F) {
			b.set(0, 1, 0);
		}
		b.normalize();
		shade(ps, c, o -> o.plate(new Vector3f(), a, b, 0.24F, 14, argb(0.85F, 0xC89A1A)));
		glow(ps, c, o -> {
			Vector3f head = new Vector3f();
			o.sprite(head, s.camRight, s.camUp, 0.55F, argb(0.45F, GOLD));
			o.circle(head, a, b, 0.24F, 0.025F, 14, argb(0.95F, GOLD_PALE));
			o.circle(head, a, b, 0.16F, 0.012F, 12, argb(0.8F, GOLD));
			o.star(head, a, b, 0.1F, 4, 0.4F, 0.0F, argb(0.9F, GOLD_PALE), argb(0.3F, GOLD));
			o.glowLine(head, new Vector3f(back).mul(tail), 0.14F, argb(0.6F, GOLD), argb(0.0F, GOLD));
			for (int i = 0; i < 4; i++) {
				float d = 0.4F + 0.5F * i;
				if (d > tail) {
					continue;
				}
				Vector3f at = new Vector3f(back).mul(d).add(new Vector3f(side).mul((hash(s.seed + i * 3) - 0.5F) * 0.4F)).add(0, -0.1F * i, 0);
				o.glint(at, s.camRight, s.camUp, 0.16F, spin * 0.2F + i, argb(0.9F * (1.0F - d / 2.6F), GOLD_PALE));
			}
		});
	}

	/** Caribbean Free Bird's shot: a white-hot bullet carried by a bird of sky-blue light beating its wings, feathers shed behind. */
	private static void birdShot(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f back = new Vector3f(s.forward).negate();
		float tail = trail(s, 2.6F);
		float time = s.ageInTicks;
		float flap = Mth.sin(time * 1.3F);
		glow(ps, c, o -> {
			Vector3f head = new Vector3f();
			o.sprite(head, s.camRight, s.camUp, 0.5F, argb(0.5F, SKY));
			o.sprite(head, s.camRight, s.camUp, 0.16F, argb(1.0F, 0xFFFFFF));
			o.bird(new Vector3f(back).mul(0.6F), s.forward, new Vector3f(0, 1, 0), 1.5F, flap, argb(0.9F, 0xEAF8FF), argb(0.7F, SKY));
			o.glowLine(head, new Vector3f(back).mul(tail), 0.18F, argb(0.6F, SKY), argb(0.0F, SKY));
			Vector3f side = VfxOut.across(back);
			for (int i = 0; i < 5; i++) {
				float d = 0.6F + 0.45F * i;
				if (d > tail) {
					continue;
				}
				Vector3f at = new Vector3f(back).mul(d).add(new Vector3f(side).mul(Mth.sin(time * 0.4F + i) * 0.3F)).add(0, -0.08F * i, 0);
				Vector3f dir = new Vector3f(Mth.cos(time * 0.2F + i), 0.5F, Mth.sin(time * 0.25F + i)).normalize();
				o.diamond(at, dir, VfxOut.across(dir), 0.28F, 0.09F, argb(0.2F, SKY), argb(0.85F * (1.0F - d / 3.0F), 0xEAF8FF));
			}
		});
	}

	/** Anne's powder shot: a red-hot ball, sparks spitting off it and a thin streak of smoke. */
	private static void musketShot(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f back = new Vector3f(s.forward).negate();
		float tail = trail(s, 2.2F);
		float time = s.ageInTicks;
		smoke(s, ps, c, tail, 4, 0.35F, 0x4A4440);
		glow(ps, c, o -> {
			Vector3f head = new Vector3f();
			o.sprite(head, s.camRight, s.camUp, 0.8F, argb(0.6F, FIRE));
			o.sprite(head, s.camRight, s.camUp, 0.28F, argb(1.0F, 0xFFF0C0));
			o.glowLine(head, new Vector3f(back).mul(tail), 0.25F, argb(0.85F, FIRE_CORE), argb(0.0F, FIRE));
			o.bar(head, new Vector3f(back).mul(Math.min(tail, 1.0F)), 0.03F, 0.01F, argb(1.0F, 0xFFFFFF), argb(0.0F, FIRE_CORE));
			for (int i = 0; i < 6; i++) {
				float d = 0.2F + 0.3F * i;
				if (d > tail) {
					continue;
				}
				Vector3f dir = BladeLooks.shardDir(s.seed + (int)time, i);
				Vector3f at = new Vector3f(back).mul(d).add(new Vector3f(dir).mul(0.15F + 0.1F * i));
				o.bar(at, new Vector3f(dir).mul(0.12F).add(at), 0.02F, 0.005F, argb(0.95F, FIRE_CORE), argb(0.0F, FIRE));
			}
		});
	}

	/**
	 * Ikoku Sovereignty: a vast crescent of pink force with a golden rim, standing across its flight, two fading echoes behind it and a ring
	 * of shock rolling round its front.
	 */
	private static void ikokuWave(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		Vector3f up = new Vector3f(side).cross(f).normalize();
		// a great swing comes down slanted
		Vector3f axis = new Vector3f(side).mul(Mth.cos(0.5F)).add(new Vector3f(up).mul(Mth.sin(0.5F)));
		float grow = Math.min(1.0F, 0.5F + s.ageInTicks * 0.2F);
		float time = s.ageInTicks;
		float radius = 3.2F * grow;
		float thick = 1.1F * grow;
		float tail = trail(s, 3.0F);
		ps.pushPose();
		ps.translate(0.0F, 0.1F, 0.0F);
		c.submitCustomGeometry(ps, VfxDraw.GLOW, (pose, buffer) -> {
			for (int echo = 2; echo >= 1; echo--) {
				float b = echo * 0.8F;
				if (b > tail) {
					continue;
				}
				SkillProjectileRenderer.crescent(pose, buffer, f, axis, new Vector3f(f).mul(-b), radius * (1 - 0.06F * echo), thick, PINK, PINK, echo == 1 ? 0.4F : 0.18F,
					time, 0.0F);
			}
			SkillProjectileRenderer.crescent(pose, buffer, f, axis, new Vector3f(), radius, thick, PINK, GOLD_PALE, 1.0F, time, 1.0F);
		});
		ps.popPose();
		glow(ps, c, o -> {
			Vector3f a = VfxOut.across(f);
			Vector3f b = new Vector3f(f).cross(a).normalize();
			// shock rings round the front
			for (int k = 0; k < 2; k++) {
				float q = (time * 0.12F + k * 0.5F) % 1.0F;
				o.circle(new Vector3f(f).mul(0.4F - q * 1.5F), a, b, radius * (0.5F + 0.6F * q), 0.03F, 24, argb(0.5F * (1.0F - q), PINK_PALE));
			}
			// golden sparkles shed off the rim
			for (int i = 0; i < 8; i++) {
				float d = 0.3F + 0.35F * i;
				if (d > tail) {
					continue;
				}
				float ang = (hash(s.seed + i) - 0.5F) * 2.4F;
				Vector3f at = new Vector3f(axis).mul(-Mth.cos(ang) * radius * 0.5F).add(new Vector3f(f).cross(axis).normalize(Mth.sin(ang) * radius)).add(
					new Vector3f(f).mul(-d));
				o.glint(at, s.camRight, s.camUp, 0.22F, time * 0.2F + i, argb(0.9F * (1.0F - d / 3.2F), i % 2 == 0 ? GOLD_PALE : PINK_PALE));
			}
		});
	}

	/**
	 * Seaquake: a wall of sea about 3.5 blocks across rising from the ground under the shot, curling forward over its own front, white foam
	 * along the crest and spray thrown off it.
	 */
	private static void tsunami(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f f = new Vector3f(s.forward.x, 0, s.forward.z);
		if (f.lengthSquared() < 1.0E-4F) {
			f.set(0, 0, 1);
		}
		f.normalize();
		Vector3f side = VfxOut.across(f);
		float time = s.ageInTicks;
		float grow = Math.min(1.0F, 0.4F + time * 0.15F);
		float half = 1.9F * grow;
		// the shot flies at about chest height: the wave stands on the ground under it
		float ground = -1.35F;
		int cols = 10;
		int rows = PROFILE.length - 1;
		Vector3f[][] p = new Vector3f[cols + 1][rows + 1];
		for (int i = 0; i <= cols; i++) {
			float u = (float)i / cols * 2 - 1;
			float h = (1.0F - u * u * 0.75F) * grow * (0.9F + 0.1F * Mth.sin(time * 0.5F + u * 3.0F));
			for (int j = 0; j <= rows; j++) {
				p[i][j] = new Vector3f(side).mul(u * half).add(new Vector3f(f).mul(PROFILE[j][0] * h)).add(0, ground + PROFILE[j][1] * h, 0);
			}
		}
		shade(ps, c, o -> {
			for (int i = 0; i < cols; i++) {
				for (int j = 0; j < rows; j++) {
					float v = (j + 0.5F) / rows;
					int col = VfxOut.lerp(argb(0.6F, SEA_DEEP), argb(0.5F, SEA), v);
					o.quad(p[i][j], p[i + 1][j], p[i + 1][j + 1], p[i][j + 1], col);
				}
			}
		});
		glow(ps, c, o -> {
			int crest = rows * 3 / 5;
			for (int i = 0; i < cols; i++) {
				o.glowLine(p[i][crest], p[i + 1][crest], 0.22F, argb(0.85F, FOAM), argb(0.85F, FOAM));
				o.glowLine(p[i][rows], p[i + 1][rows], 0.14F, argb(0.6F, FOAM), argb(0.6F, FOAM));
				o.glowLine(p[i][2], p[i + 1][2], 0.1F, argb(0.3F, 0x9AD8FF), argb(0.3F, 0x9AD8FF));
			}
			// streaks of water running up the face
			for (int i = 1; i < cols; i += 2) {
				for (int j = 0; j < crest; j++) {
					o.bar(p[i][j], p[i][j + 1], 0.015F, 0.015F, argb(0.35F, FOAM), argb(0.5F, FOAM));
				}
			}
			// spray thrown off the crest
			for (int i = 0; i < 10; i++) {
				float q = (time * 0.08F + hash(s.seed + i)) % 1.0F;
				int col = Math.min(cols, (int)(hash(i * 7) * cols));
				Vector3f at = new Vector3f(p[col][crest]).add(new Vector3f(f).mul(0.5F * q)).add(0, 0.6F * q, 0);
				o.glint(at, s.camRight, s.camUp, 0.2F, time * 0.2F + i, argb(0.9F * (1.0F - q), FOAM));
			}
			// a foam line on the ground at its foot
			o.glowLine(new Vector3f(side).mul(-half).add(new Vector3f(f).mul(-1.1F)).add(0, ground + 0.15F, 0),
				new Vector3f(side).mul(half).add(new Vector3f(f).mul(-1.1F)).add(0, ground + 0.15F, 0), 0.3F, argb(0.5F, FOAM), argb(0.5F, FOAM));
		});
	}

	/**
	 * Kamusari's wave: a black crescent rimmed in crimson (Conqueror's Haki), two red echoes behind it and black lightning crackling round
	 * it, the bolts thrown anew every other tick.
	 */
	private static void kamusariWave(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		Vector3f up = new Vector3f(side).cross(f).normalize();
		Vector3f axis = new Vector3f(side).mul(Mth.cos(s.tilt)).add(new Vector3f(up).mul(Mth.sin(s.tilt)));
		if (s.mirror) {
			axis.negate();
		}
		float grow = Math.min(1.0F, 0.55F + s.ageInTicks * 0.22F);
		float time = s.ageInTicks;
		float radius = 2.2F * grow;
		float thick = 0.8F * grow;
		float tail = trail(s, 2.0F);
		ps.pushPose();
		ps.translate(0.0F, 0.1F, 0.0F);
		c.submitCustomGeometry(ps, VfxDraw.GLOW, (pose, buffer) -> {
			for (int echo = 2; echo >= 1; echo--) {
				float b = echo * 0.6F;
				if (b > tail) {
					continue;
				}
				SkillProjectileRenderer.crescent(pose, buffer, f, axis, new Vector3f(f).mul(-b), radius * (1 - 0.07F * echo), thick, CRIMSON, CRIMSON, echo == 1 ? 0.4F : 0.18F,
					time, 0.0F);
			}
			SkillProjectileRenderer.crescent(pose, buffer, f, axis, new Vector3f(), radius, thick, CRIMSON, 0xFF8A80, 1.0F, time, 0.45F);
		});
		c.submitCustomGeometry(ps, VfxDraw.SHADE, (pose, buffer) -> SkillProjectileRenderer.body(pose, buffer, f, axis, radius, thick, time, 0x0A0306));
		ps.popPose();
		int seed = s.seed * 31 + (int)(time / 2);
		java.util.List<java.util.List<Vector3f>> bolts = new java.util.ArrayList<>();
		Vector3f across = new Vector3f(f).cross(axis).normalize();
		for (int i = 0; i < 5; i++) {
			float ang = (hash(seed + i * 3) - 0.5F) * 2.2F;
			Vector3f from = new Vector3f(axis).mul(radius * 0.5F - radius * Mth.cos(ang)).add(new Vector3f(across).mul(radius * Mth.sin(ang))).add(0, 0.1F, 0);
			Vector3f d = BladeLooks.shardDir(seed, i);
			Vector3f to = new Vector3f(d).mul(1.0F + hash(seed + i) * 1.2F).add(from).add(new Vector3f(f).mul(-0.6F));
			bolts.add(VfxOut.zigzag(from, to, seed + i, 0.35F, 5));
		}
		shade(ps, c, o -> {
			for (java.util.List<Vector3f> bolt : bolts) {
				o.polyline(bolt, 0.06F, argb(0.95F, HAKI));
			}
		});
		glow(ps, c, o -> {
			for (java.util.List<Vector3f> bolt : bolts) {
				o.glowPolyline(bolt, 0.2F, argb(0.5F, CRIMSON));
			}
		});
	}

	/** Thunderclap shot: a cannonball wrapped in crackling yellow lightning, a jagged bolt trailing it. */
	private static void thunderShot(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f back = new Vector3f(s.forward).negate();
		float tail = trail(s, 3.4F);
		float time = s.ageInTicks;
		int seed = s.seed * 13 + (int)(time / 2);
		smoke(s, ps, c, tail, 4, 0.5F, 0x2A2A30);
		glow(ps, c, o -> {
			Vector3f head = new Vector3f();
			o.sprite(head, s.camRight, s.camUp, 0.9F, argb(0.45F, BOLT));
			o.sprite(head, s.camRight, s.camUp, 0.3F, argb(0.9F, 0xFFFFFF));
			for (int i = 0; i < 5; i++) {
				Vector3f d0 = BladeLooks.shardDir(seed, i);
				Vector3f d1 = BladeLooks.shardDir(seed + 7, i);
				o.bolt(new Vector3f(d0).mul(0.3F), new Vector3f(d1).mul(0.75F).add(new Vector3f(back).mul(0.2F)), seed + i, 0.15F, 3, 0.1F, argb(0.7F, BOLT),
					argb(1.0F, 0xFFFFFF));
			}
			if (tail > 0.5F) {
				o.bolt(new Vector3f(back).mul(0.4F), new Vector3f(back).mul(tail), seed, 0.3F, 6, 0.14F, argb(0.6F, BOLT), argb(0.9F, 0xFFFBE0));
			}
		});
	}

	/** Armament Round: a bullet coated glossy black in Armament Haki, a crimson sheen round it and a dark streak behind. */
	private static void hakiShot(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f f = new Vector3f(s.forward);
		Vector3f back = new Vector3f(f).negate();
		float tail = trail(s, 2.4F);
		float time = s.ageInTicks;
		Vector3f side = VfxOut.across(f);
		Vector3f up = new Vector3f(side).cross(f).normalize();
		shade(ps, c, o -> {
			o.ellipsoid(new Vector3f(back).mul(0.05F), new Vector3f(side).mul(0.34F), new Vector3f(up).mul(0.34F), new Vector3f(f).mul(0.7F), 5, 8, argb(0.97F, 0x4A3448),
				argb(0.98F, 0x050208));
			o.bar(new Vector3f(back).mul(0.4F), new Vector3f(back).mul(tail), 0.18F, 0.02F, argb(0.7F, HAKI), argb(0.0F, HAKI));
		});
		glow(ps, c, o -> {
			o.sprite(new Vector3f(), s.camRight, s.camUp, 1.0F, argb(0.22F, CRIMSON));
			o.glowLine(new Vector3f(f).mul(0.5F), new Vector3f(back).mul(tail), 0.38F, argb(0.5F, CRIMSON), argb(0.0F, CRIMSON));
			// the sheen running along the coat
			for (int k = 0; k < 3; k++) {
				float a = time * 0.5F + k * Mth.TWO_PI / 3;
				Vector3f off = new Vector3f(side).mul(Mth.cos(a) * 0.27F).add(new Vector3f(up).mul(Mth.sin(a) * 0.27F));
				o.bar(new Vector3f(f).mul(0.25F).add(off), new Vector3f(back).mul(0.35F).add(off), 0.012F, 0.012F, argb(0.8F, 0xFF6A7A), argb(0.0F, CRIMSON));
			}
			for (int i = 0; i < 4; i++) {
				float d = 0.4F + 0.45F * i;
				if (d > tail) {
					continue;
				}
				Vector3f dir = BladeLooks.shardDir(s.seed + (int)time, i);
				Vector3f at = new Vector3f(back).mul(d);
				o.bar(at, new Vector3f(dir).mul(0.2F).add(at), 0.02F, 0.006F, argb(0.9F, 0xFF5A6A), argb(0.0F, CRIMSON));
			}
		});
	}

	private PirateShots() {
	}
}
