package com.minecraftmode.client.vfx;

import static com.minecraftmode.client.vfx.SkillVfxRenderer.glow;
import static com.minecraftmode.client.vfx.SkillVfxRenderer.shade;
import static com.minecraftmode.client.vfx.VfxOut.argb;
import static com.minecraftmode.client.vfx.VfxOut.hash;

import com.minecraftmode.client.vfx.SkillProjectileRenderer.State;
import com.minecraftmode.job.skill.SkillLook;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * The hunter's projectile looks: Chrollo's Indoor Fish and the stolen abilities of the Bandit's Secret, Zeno's dragons (Dragon Head and the
 * dragons of Dragon Dive), Silva's Nen orbs and Illumi's needles. Orbs and needles hit {@link #HIT} blocks round their flight, a Dragon
 * Head (a sword wave) {@link #WAVE_HIT}; exploding ones burst wider where they hit ({@link SkillLook#DRAGON_BURST}, {@link SkillLook#ORB_BURST}).
 * Trails never reach further back than the shot has flown.
 */
final class HunterShots {
	/** How far round its flight an orb or a needle hits (ProjectileStyle.hitRadius, vanilla's margin). */
	private static final float HIT = 0.3F;
	/** How far round its flight a sword wave (the Dragon Head) hits. */
	private static final float WAVE_HIT = 1.0F;
	private static final float FISH_SCALE = 1.5F;
	private static final int FISH = 0x7FE0D8;
	private static final int FISH_PALE = 0xD8FFF8;
	private static final int FISH_BODY = 0x3A8C90;
	private static final int GOLD = 0xE0C080;
	private static final int GOLD_PALE = 0xFFF0C8;
	private static final int PAGE = 0xEDE2C8;
	private static final int INK = 0x2A1A10;
	private static final int DRAGON = 0x6CD0FF;
	private static final int DRAGON_PALE = 0xE4F8FF;
	private static final int DRAGON_EYE = 0xFFFFFF;
	private static final int ORANGE = 0xFF9A3A;
	private static final int ORANGE_PALE = 0xFFE6C0;
	private static final int STEEL = 0x2A2434;
	private static final int VIOLET = 0xB98CFF;
	private static final int VIOLET_PALE = 0xEADCFF;

	/** How far round the projectile its look reaches (its culling box grows by this); 0 for the looks of other classes. */
	static double reach(final SkillLook look) {
		return switch (look) {
			case DRAGON_HEAD -> 5.0;
			case DIVE_DRAGON -> 2.5;
			case INDOOR_FISH, STOLEN_SKILL, SILVA_ORB, NEN_NEEDLE -> 1.5;
			default -> 0.0;
		};
	}

	/** Looks drawn whole, without the projectile's item under them (a thrown knife under Illumi's needle). */
	static boolean hidesItem(final SkillLook look) {
		return look == SkillLook.NEN_NEEDLE;
	}

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case INDOOR_FISH -> indoorFish(s, ps, c);
			case STOLEN_SKILL -> stolenSkill(s, ps, c);
			case DRAGON_HEAD -> dragon(s, ps, c, 0.62F, 4.5F);
			case DIVE_DRAGON -> dragon(s, ps, c, 0.3F, 2.2F);
			case SILVA_ORB -> silvaOrb(s, ps, c);
			case NEN_NEEDLE -> nenNeedle(s, ps, c);
			default -> {
			}
		}
	}

	/** How far behind the projectile a trail of {@code length} may reach: no further than it has flown. */
	private static float trail(final State s, final float length) {
		return Math.min(length, Math.max(0.0F, s.ageInTicks * Math.max(0.6F, s.speed) - 0.4F));
	}

	/**
	 * Indoor Fish: a ghostly fish of Nen swimming nose first, its body swaying and its tail beating, a dorsal fin, jaws open on rows of
	 * teeth and a pale eye; a wake of rippling light behind it.
	 */
	private static void indoorFish(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		Vector3f up = new Vector3f(side).cross(f).normalize();
		float time = s.ageInTicks;
		float beat = Mth.sin(time * 1.1F + s.seed);
		float len = 0.95F;
		// the spine sways side to side, more towards the tail
		Vector3f[] spine = new Vector3f[7];
		for (int i = 0; i < 7; i++) {
			float t = i / 6.0F;
			float sway = beat * 0.16F * t * t;
			spine[i] = new Vector3f(f).mul(len * (0.45F - t)).add(new Vector3f(side).mul(sway));
		}
		float tail = trail(s, 1.4F) / FISH_SCALE;
		// drawn half as big again: a fish the size of a hand read as a fleck in flight
		ps.pushPose();
		ps.scale(FISH_SCALE, FISH_SCALE, FISH_SCALE);
		shade(ps, c, o -> {
			// the body: a flat lens in the upright plane, widest behind the head
			for (int i = 0; i < 6; i++) {
				float t0 = i / 6.0F;
				float t1 = (i + 1) / 6.0F;
				float h0 = 0.2F * Mth.sin(Mth.PI * Math.min(1.0F, 0.15F + t0 * 1.05F));
				float h1 = 0.2F * Mth.sin(Mth.PI * Math.min(1.0F, 0.15F + t1 * 1.05F));
				int col0 = VfxOut.lerp(argb(0.75F, FISH_BODY), argb(0.6F, FISH), t0);
				int col1 = VfxOut.lerp(argb(0.75F, FISH_BODY), argb(0.6F, FISH), t1);
				o.quad(new Vector3f(spine[i]).add(new Vector3f(up).mul(h0)), new Vector3f(spine[i + 1]).add(new Vector3f(up).mul(h1)), spine[i + 1], spine[i], col0, col1, col1,
					col0);
				o.quad(spine[i], spine[i + 1], new Vector3f(spine[i + 1]).add(new Vector3f(up).mul(-h1 * 0.8F)), new Vector3f(spine[i]).add(new Vector3f(up).mul(-h0 * 0.8F)),
					argb(0.45F, FISH_PALE), argb(0.4F, FISH_PALE), col1, col0);
			}
		});
		glow(ps, c, o -> {
			int edge = argb(0.85F, FISH_PALE);
			int soft = argb(0.5F, FISH);
			// outline of the back and belly
			for (int i = 0; i < 6; i++) {
				float t0 = i / 6.0F;
				float t1 = (i + 1) / 6.0F;
				float h0 = 0.2F * Mth.sin(Mth.PI * Math.min(1.0F, 0.15F + t0 * 1.05F));
				float h1 = 0.2F * Mth.sin(Mth.PI * Math.min(1.0F, 0.15F + t1 * 1.05F));
				o.glowLine(new Vector3f(spine[i]).add(new Vector3f(up).mul(h0)), new Vector3f(spine[i + 1]).add(new Vector3f(up).mul(h1)), 0.035F, edge, edge);
				o.glowLine(new Vector3f(spine[i]).add(new Vector3f(up).mul(-h0 * 0.8F)), new Vector3f(spine[i + 1]).add(new Vector3f(up).mul(-h1 * 0.8F)), 0.03F, soft, soft);
			}
			// the forked tail, beating
			Vector3f root = spine[6];
			Vector3f flick = new Vector3f(side).mul(beat * 0.12F);
			Vector3f top = new Vector3f(f).mul(-0.28F).add(new Vector3f(up).mul(0.24F)).add(flick).add(root);
			Vector3f low = new Vector3f(f).mul(-0.28F).add(new Vector3f(up).mul(-0.22F)).add(flick).add(root);
			Vector3f notch = new Vector3f(f).mul(-0.14F).add(flick).add(root);
			o.tri(root, top, notch, argb(0.6F, FISH), argb(0.15F, FISH), argb(0.35F, FISH));
			o.tri(root, notch, low, argb(0.6F, FISH), argb(0.35F, FISH), argb(0.15F, FISH));
			o.glowLine(root, top, 0.03F, edge, edge);
			o.glowLine(root, low, 0.03F, edge, edge);
			// the dorsal fin and the side fins
			Vector3f finBase = new Vector3f(spine[2]).add(new Vector3f(up).mul(0.19F));
			o.tri(finBase, new Vector3f(spine[3]).add(new Vector3f(up).mul(0.36F)).add(new Vector3f(f).mul(-0.12F)), new Vector3f(spine[3]).add(new Vector3f(up).mul(0.17F)),
				argb(0.6F, FISH), argb(0.1F, FISH), argb(0.4F, FISH));
			for (int k = -1; k <= 1; k += 2) {
				Vector3f pec = new Vector3f(spine[1]).add(new Vector3f(up).mul(-0.06F));
				o.tri(pec, new Vector3f(pec).add(new Vector3f(side).mul(k * 0.16F)).add(new Vector3f(f).mul(-0.16F)).add(new Vector3f(up).mul(-0.06F + 0.05F * beat * k)),
					new Vector3f(pec).add(new Vector3f(f).mul(-0.12F)), argb(0.5F, FISH), argb(0.1F, FISH), argb(0.3F, FISH));
			}
			// jaws open on teeth, and the eye
			Vector3f snout = new Vector3f(spine[0]).add(new Vector3f(f).mul(0.1F));
			Vector3f jaw = new Vector3f(spine[0]).add(new Vector3f(f).mul(0.06F)).add(new Vector3f(up).mul(-0.14F));
			o.glowLine(new Vector3f(spine[1]).add(new Vector3f(up).mul(-0.02F)), snout, 0.03F, edge, edge);
			o.glowLine(new Vector3f(spine[1]).add(new Vector3f(up).mul(-0.04F)), jaw, 0.03F, edge, edge);
			for (int i = 0; i < 4; i++) {
				float t = 0.25F + 0.22F * i;
				Vector3f u0 = new Vector3f(spine[1]).lerp(snout, t).add(new Vector3f(up).mul(-0.02F));
				o.blade(u0, new Vector3f(u0).add(new Vector3f(up).mul(-0.06F)), f, 0.015F, argb(0.95F, 0xFFFFFF), argb(0.95F, 0xFFFFFF));
				Vector3f l0 = new Vector3f(spine[1]).lerp(jaw, t).add(new Vector3f(up).mul(-0.03F));
				o.blade(l0, new Vector3f(l0).add(new Vector3f(up).mul(0.05F)), f, 0.013F, argb(0.9F, 0xFFFFFF), argb(0.9F, 0xFFFFFF));
			}
			for (int k = -1; k <= 1; k += 2) {
				Vector3f eye = new Vector3f(spine[1]).add(new Vector3f(f).mul(0.04F)).add(new Vector3f(up).mul(0.07F)).add(new Vector3f(side).mul(k * 0.06F));
				o.sprite(eye, s.camRight, s.camUp, 0.05F, argb(1.0F, 0xFFFFFF));
			}
			o.sprite(new Vector3f(), s.camRight, s.camUp, HIT + 0.25F, argb(0.15F, FISH));
			if (tail > 0.1F) {
				// the wake: two rippling ribbons
				for (int k = -1; k <= 1; k += 2) {
					Vector3f prev = null;
					for (int i = 0; i <= 8; i++) {
						float t = i / 8.0F;
						Vector3f q = new Vector3f(f).mul(-0.5F - tail * t).add(new Vector3f(side).mul(k * 0.08F * Mth.sin(time * 0.8F + t * 7.0F)))
							.add(new Vector3f(up).mul(k * 0.05F));
						if (prev != null) {
							o.glowLine(prev, q, 0.03F, argb(0.5F * (1.0F - t), FISH), argb(0.5F * (1.0F - t), FISH));
						}
						prev = q;
					}
				}
			}
		});
		ps.popPose();
	}

	/**
	 * Bandit's Secret: a stolen ability flying out of the book, a golden orb of Nen bound in a page that turns round it, the page's script
	 * glowing; flecks of script trailing behind.
	 */
	private static void stolenSkill(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		Vector3f up = new Vector3f(side).cross(f).normalize();
		float time = s.ageInTicks;
		float spin = time * 0.6F + s.seed;
		Vector3f a = new Vector3f(side).mul(Mth.cos(spin)).add(new Vector3f(up).mul(Mth.sin(spin)));
		Vector3f n = new Vector3f(f).cross(a).normalize();
		float tail = trail(s, 1.4F);
		// the page wrapped round the orb: a strip bent along a ring
		List<Vector3f> inner = new ArrayList<>();
		List<Vector3f> outer = new ArrayList<>();
		for (int i = 0; i <= 8; i++) {
			float t = -1.1F + 2.2F * i / 8.0F;
			Vector3f round = new Vector3f(a).mul(Mth.cos(t) * 0.32F).add(new Vector3f(n).mul(Mth.sin(t) * 0.32F));
			inner.add(new Vector3f(round).add(new Vector3f(f).mul(-0.17F)));
			outer.add(new Vector3f(round).add(new Vector3f(f).mul(0.17F)));
		}
		shade(ps, c, o -> {
			for (int i = 0; i < 8; i++) {
				o.quad(inner.get(i), inner.get(i + 1), outer.get(i + 1), outer.get(i), argb(0.9F, PAGE));
			}
		});
		glow(ps, c, o -> {
			o.sprite(new Vector3f(), s.camRight, s.camUp, 0.55F, argb(0.45F, GOLD));
			o.sprite(new Vector3f(), s.camRight, s.camUp, 0.22F, argb(0.95F, GOLD_PALE));
			for (int i = 0; i < 8; i += 2) {
				Vector3f mid = new Vector3f(inner.get(i)).lerp(outer.get(i + 1), 0.5F);
				Vector3f out = new Vector3f(mid).normalize();
				o.rune(new Vector3f(mid).add(new Vector3f(out).mul(0.02F)), f, VfxOut.across(out).cross(out).normalize(), 0.07F, s.seed * 5 + i, 0.012F, argb(0.95F, 0xC07A10));
			}
			o.glowLine(inner.getFirst(), outer.getFirst(), 0.04F, argb(0.8F, GOLD_PALE), argb(0.8F, GOLD_PALE));
			o.glowLine(inner.getLast(), outer.getLast(), 0.04F, argb(0.8F, GOLD_PALE), argb(0.8F, GOLD_PALE));
			if (tail > 0.1F) {
				o.glowLine(new Vector3f(), new Vector3f(f).mul(-tail), 0.2F, argb(0.35F, GOLD), argb(0.0F, GOLD));
				for (int i = 0; i < 5; i++) {
					float q = (time * 0.12F + hash(s.seed + i)) % 1.0F;
					Vector3f at = new Vector3f(f).mul(-q * tail).add(BladeLooks.shardDir(s.seed, i).mul(0.25F));
					o.rune(at, side, up, 0.06F, s.seed + i * 3, 0.01F, argb(0.8F * (1.0F - q), GOLD_PALE));
				}
			}
		});
		shade(ps, c, o -> o.sprite(new Vector3f(f).mul(-0.01F), s.camRight, s.camUp, 0.08F, argb(0.5F, INK)));
	}

	/**
	 * Zeno's dragon of aura: its head leading with the jaws open, the body streaming back {@code body} blocks (no further than it has flown)
	 * in slow waves, a glow round the head as wide as it hits; {@code r} the body's thickness.
	 */
	private static void dragon(final State s, final PoseStack ps, final SubmitNodeCollector c, final float r, final float body) {
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		Vector3f up = new Vector3f(side).cross(f).normalize();
		float time = s.ageInTicks;
		float tail = Math.max(r * 2.0F, trail(s, body));
		List<Vector3f> spine = new ArrayList<>();
		int n = 20;
		for (int i = 0; i <= n; i++) {
			float t = i / (float)n;
			float wave = Mth.sin(time * 0.5F - t * 6.0F + s.seed) * r * 0.9F * t;
			float lift = Mth.cos(time * 0.4F - t * 5.0F + s.seed) * r * 0.5F * t;
			spine.add(new Vector3f(f).mul(-tail * t).add(new Vector3f(side).mul(wave)).add(new Vector3f(up).mul(lift)));
		}
		boolean big = r > 0.4F;
		glow(ps, c, o -> {
			o.auraDragon(spine, r, 0.75F + 0.25F * Mth.sin(time * 0.6F), time, argb(0.7F, DRAGON), argb(0.95F, DRAGON_PALE), argb(1.0F, DRAGON_EYE));
			if (big) {
				o.circle(new Vector3f(f).mul(r * 0.8F), side, up, WAVE_HIT, 0.03F, 20, argb(0.2F, DRAGON_PALE));
				o.sprite(new Vector3f(f).mul(r), s.camRight, s.camUp, WAVE_HIT * 1.2F, argb(0.2F, DRAGON));
			} else {
				o.sprite(new Vector3f(f).mul(r), s.camRight, s.camUp, r * 2.0F, argb(0.25F, DRAGON));
			}
			for (int i = 0; i < (big ? 8 : 3); i++) {
				float q = (time * 0.1F + hash(s.seed + i)) % 1.0F;
				Vector3f at = new Vector3f(f).mul(-q * tail).add(BladeLooks.shardDir(s.seed, i).mul(r * 1.4F));
				o.glint(at, s.camRight, s.camUp, r * 0.35F, time * 0.2F + i, argb(0.8F * (1.0F - q), DRAGON_PALE));
			}
		});
	}

	/**
	 * Silva's Nen orb: aura packed into a dense sphere, a white-hot core in layered shells of orange light with bands winding round it, a
	 * short wake behind.
	 */
	private static void silvaOrb(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f f = new Vector3f(s.forward);
		float time = s.ageInTicks;
		float pulse = 1.0F + 0.06F * Mth.sin(time * 2.1F + s.seed);
		// the shell as wide as the orb hits (HIT); the burst where it lands is ORB_BURST
		float r = 0.32F * pulse;
		float tail = trail(s, 1.3F);
		glow(ps, c, o -> {
			o.sprite(new Vector3f(), s.camRight, s.camUp, r * 2.3F, argb(0.35F, ORANGE));
			o.sprite(new Vector3f(), s.camRight, s.camUp, r * 1.15F, argb(0.85F, ORANGE));
			o.sprite(new Vector3f(), s.camRight, s.camUp, r * 0.6F, argb(1.0F, ORANGE_PALE));
			o.sprite(new Vector3f(), s.camRight, s.camUp, r * 0.3F, argb(1.0F, 0xFFFFFF));
			o.circle(new Vector3f(), s.camRight, s.camUp, r, 0.03F, 20, argb(0.9F, ORANGE_PALE));
			for (int k = 0; k < 3; k++) {
				float tilt = Mth.TWO_PI * k / 3 + time * 0.25F;
				Vector3f a = new Vector3f(s.camRight).mul(Mth.cos(tilt)).add(new Vector3f(s.camUp).mul(Mth.sin(tilt)));
				Vector3f b = new Vector3f(s.camRight).cross(s.camUp).mul(1.0F).normalize();
				Vector3f b2 = new Vector3f(b).mul(Mth.cos(time * 0.3F + k)).add(new Vector3f(a).cross(b).normalize().mul(Mth.sin(time * 0.3F + k)));
				o.circle(new Vector3f(), a, b2, r * 0.82F, 0.02F, 16, argb(0.7F, ORANGE_PALE));
			}
			if (tail > 0.1F) {
				o.glowLine(new Vector3f(), new Vector3f(f).mul(-tail), r * 0.9F, argb(0.4F, ORANGE), argb(0.0F, ORANGE));
				o.glowLine(new Vector3f(), new Vector3f(f).mul(-tail * 0.6F), r * 0.3F, argb(0.7F, ORANGE_PALE), argb(0.0F, ORANGE_PALE));
			}
		});
	}

	/**
	 * Illumi's needle: a long dark pin flying point first with a violet ball for its head, a thread of Nen trailing back from it.
	 */
	private static void nenNeedle(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		Vector3f up = new Vector3f(side).cross(f).normalize();
		float time = s.ageInTicks;
		Vector3f tip = new Vector3f(f).mul(0.45F);
		Vector3f head = new Vector3f(f).mul(-0.4F);
		float tail = trail(s, 2.0F);
		shade(ps, c, o -> o.pin(head, tip, 0.09F, argb(0.98F, STEEL), argb(0.98F, 0x6A3AB0)));
		glow(ps, c, o -> {
			o.sprite(head, s.camRight, s.camUp, 0.24F, argb(0.85F, VIOLET));
			o.glowLine(head, tip, 0.09F, argb(0.5F, VIOLET), argb(0.75F, VIOLET_PALE));
			o.sprite(new Vector3f(), s.camRight, s.camUp, HIT, argb(0.12F, VIOLET));
			if (tail > 0.1F) {
				Vector3f prev = null;
				for (int i = 0; i <= 8; i++) {
					float t = i / 8.0F;
					Vector3f q = new Vector3f(f).mul(-0.4F - tail * t).add(new Vector3f(side).mul(0.06F * Mth.sin(time * 0.7F + t * 6.0F)))
						.add(new Vector3f(up).mul(0.04F * Mth.cos(time * 0.5F + t * 5.0F)));
					if (prev != null) {
						o.glowLine(prev, q, 0.02F, argb(0.8F * (1.0F - t), VIOLET_PALE), argb(0.8F * (1.0F - t), VIOLET_PALE));
					}
					prev = q;
				}
			}
		});
	}

	private HunterShots() {
	}
}
