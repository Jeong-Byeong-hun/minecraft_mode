package com.minecraftmode.client.vfx;

import static com.minecraftmode.client.vfx.PendragonLooks.BEAM_R;
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
 * Looks of the Uchiha (Naruto): Itachi's Totsuka Blade (his crimson Susanoo with the Yata Mirror, the Totsuka sword of liquid poured from
 * its gourd, Amaterasu's black flames, Tsukuyomi and the crow clone) and Madara's gunbai (Uchiha Return, the Majestic Destroyer Flame,
 * the sweep and the leap).
 */
final class UchihaLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final int SUSANOO = 0xFF4D2E;
	private static final int SUSANOO_PALE = 0xFFB89A;
	private static final int SAKE = 0xFFF1D6;
	private static final int SAKE_GOLD = 0xFFD27A;
	private static final int GOURD = 0xE8D6A8;
	private static final int GOURD_DARK = 0x6A5030;
	private static final int MIRROR = 0xCFE8FF;
	private static final int IRIS = 0xD8101E;
	private static final int INK = 0x0A0404;
	private static final int BLACK = 0x050306;
	private static final int EMBER = 0xB0102E;
	private static final int FIRE = 0xFF6A1A;
	private static final int FIRE_PALE = 0xFFD070;
	private static final int FIRE_DEEP = 0xC82008;
	private static final int WIND = 0xF0EADC;
	private static final int DUST = 0xB8A890;
	/** How much bigger Itachi's Susanoo is drawn than its layout (ribs 1.4 blocks round the caster, its head 4.6 up). */
	private static final float SUSANOO_SCALE = 1.45F;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case SUSANOO_ITACHI -> susanoo(s, ps, c);
			case TOTSUKA -> totsuka(s, ps, c);
			case BLACK_FLAMES -> blackFlames(s, ps, c);
			case TSUKUYOMI -> tsukuyomi(s, ps, c);
			case CROW_CLONE -> crows(s, ps, c);
			case UCHIHA_RETURN -> uchihaReturn(s, ps, c);
			case DESTROYER_FLAME -> destroyerFlame(s, ps, c);
			case GUNBAI_SWEEP -> gunbaiSweep(s, ps, c);
			case GUNBAI_LEAP -> gunbaiLeap(s, ps, c);
			default -> {
			}
		}
	}

	// ------------------------------------------------------------ Itachi

	/**
	 * Susanoo: crimson chakra flares up round the caster and sets into Itachi's Susanoo: a ribcage of light round them on a spine, shoulders,
	 * a long-nosed head over them with burning eyes, and two great arms: the left raising the round Yata Mirror, the right holding the
	 * sealing gourd. Flames lick along every edge.
	 */
	private static void susanoo(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float rise = ease(age, 0.0F, 10.0F);
		float fade = 1.0F - ease(p, 0.85F, 1.0F);
		float self = s.selfView ? 0.35F : 1.0F;
		float k = rise * fade;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f spineLow = new Vector3f(ahead).mul(-0.6F).add(0, 0.45F, 0);
		Vector3f spineTop = new Vector3f(ahead).mul(-0.55F).add(0, 2.45F * rise, 0);
		Vector3f head = new Vector3f(ahead).mul(-0.25F).add(0, 3.15F * rise, 0);
		Vector3f[] shoulder = new Vector3f[2];
		Vector3f[] elbow = new Vector3f[2];
		Vector3f[] hand = new Vector3f[2];
		for (int i = 0; i < 2; i++) {
			float sg = i == 0 ? -1.0F : 1.0F;
			shoulder[i] = new Vector3f(right).mul(sg * 1.15F).add(new Vector3f(ahead).mul(-0.35F)).add(0, 2.35F * rise, 0);
			elbow[i] = new Vector3f(right).mul(sg * 1.6F).add(new Vector3f(ahead).mul(0.05F)).add(0, 1.65F * rise, 0);
			hand[i] = new Vector3f(right).mul(sg * (i == 0 ? 1.05F : 1.25F)).add(new Vector3f(ahead).mul(i == 0 ? 1.05F : 0.75F)).add(0, 1.6F * rise, 0);
		}
		Vector3f mirrorN = new Vector3f(ahead).mul(0.85F).sub(new Vector3f(right).mul(0.35F)).normalize();
		Vector3f mirrorA = VfxOut.across(mirrorN);
		Vector3f mirrorB = new Vector3f(mirrorA).cross(mirrorN).normalize();
		Vector3f gourd = new Vector3f(hand[1]).add(0, -0.1F, 0);
		// drawn half as big again as the shape above: the Susanoo towers over its user
		ps.pushPose();
		ps.scale(SUSANOO_SCALE, SUSANOO_SCALE, SUSANOO_SCALE);
		shade(ps, c, o -> {
			if (rise < 0.4F) {
				return;
			}
			float m = ease(age, 6.0F, 12.0F) * fade * self;
			o.plate(hand[0], mirrorA, mirrorB, 0.62F, 24, argb(0.35F * m, MIRROR));
			o.ellipsoid(gourd, new Vector3f(right).mul(0.24F), new Vector3f(0, 0.26F, 0), new Vector3f(ahead).mul(0.24F), 5, 10, argb(0.95F * m, GOURD),
				argb(0.95F * m, GOURD_DARK));
			o.ellipsoid(new Vector3f(gourd).add(0, 0.38F, 0), new Vector3f(right).mul(0.16F), new Vector3f(0, 0.16F, 0), new Vector3f(ahead).mul(0.16F), 5, 10,
				argb(0.95F * m, GOURD), argb(0.95F * m, GOURD_DARK));
			o.bar(new Vector3f(gourd).add(0, 0.2F, 0), new Vector3f(gourd).add(0, 0.24F, 0), 0.17F, 0.17F, argb(0.95F * m, 0xA01818), argb(0.95F * m, 0xA01818));
		});
		glow(ps, c, o -> {
			int main = argb(0.75F * k * self, SUSANOO);
			int pale = argb(0.8F * k * self, SUSANOO_PALE);
			// the ribs: six pairs curving from the spine round to the front, leaving the front open
			for (int i = 0; i < 6; i++) {
				float y = (0.75F + 0.28F * i) * rise;
				float rr = 0.95F - 0.05F * Math.abs(i - 2.5F);
				for (int sg = -1; sg <= 1; sg += 2) {
					Vector3f prev = null;
					for (int j = 0; j <= 10; j++) {
						float th = Mth.PI - (Mth.PI - 0.5F) * j / 10.0F;
						Vector3f q = new Vector3f(ahead).mul(Mth.cos(th) * rr).add(new Vector3f(right).mul(sg * Mth.sin(th) * rr)).add(0, y - 0.18F * j / 10.0F, 0);
						if (prev != null) {
							o.glowLine(prev, q, 0.12F, main, main);
						}
						prev = q;
					}
				}
			}
			o.glowLine(spineLow, spineTop, 0.12F, main, pale);
			for (int i = 0; i < 2; i++) {
				o.glowLine(spineTop, shoulder[i], 0.1F, main, main);
				o.glowLine(shoulder[i], elbow[i], 0.2F, main, main);
				o.glowLine(elbow[i], hand[i], 0.18F, main, pale);
				o.sprite(hand[i], s.camRight, s.camUp, 0.35F, argb(0.5F * k * self, SUSANOO));
			}
			if (!s.selfView && rise > 0.3F) {
				// the head: a skull of light with a long nose and burning eyes, horns swept back
				o.circle(head, right, UP, 0.45F, 0.05F, 20, pale);
				o.glowLine(new Vector3f(head).add(new Vector3f(ahead).mul(0.3F)), new Vector3f(head).add(new Vector3f(ahead).mul(0.85F)).add(0, -0.2F, 0), 0.08F, pale, pale);
				for (int sg = -1; sg <= 1; sg += 2) {
					Vector3f eye = new Vector3f(head).add(new Vector3f(right).mul(sg * 0.17F)).add(new Vector3f(ahead).mul(0.38F)).add(0, 0.05F, 0);
					o.sprite(eye, s.camRight, s.camUp, 0.12F, argb(k, 0xFFF0C0));
					o.sprite(eye, s.camRight, s.camUp, 0.3F, argb(0.6F * k, SUSANOO));
					Vector3f horn = new Vector3f(head).add(new Vector3f(right).mul(sg * 0.3F)).add(0, 0.3F, 0);
					o.glowLine(horn, new Vector3f(horn).add(new Vector3f(ahead).mul(-0.7F)).add(new Vector3f(right).mul(sg * 0.25F)).add(0, 0.55F, 0), 0.06F, main, pale);
				}
				o.sprite(head, s.camRight, s.camUp, 0.9F, argb(0.2F * k, SUSANOO));
			}
			if (rise > 0.4F) {
				float m = ease(age, 6.0F, 12.0F) * fade * self;
				o.circle(hand[0], mirrorA, mirrorB, 0.62F, 0.045F, 28, argb(0.9F * m, SUSANOO_PALE));
				o.circle(hand[0], mirrorA, mirrorB, 0.5F, 0.02F, 28, argb(0.7F * m, SUSANOO));
				for (int i = 0; i < 8; i++) {
					float a = Mth.TWO_PI * i / 8 + time * 0.02F;
					Vector3f at = new Vector3f(mirrorA).mul(Mth.cos(a) * 0.56F).add(new Vector3f(mirrorB).mul(Mth.sin(a) * 0.56F)).add(hand[0]);
					o.sprite(at, s.camRight, s.camUp, 0.06F, argb(0.9F * m, 0xFFFFFF));
				}
				float shine = (time * 0.03F) % 1.0F;
				o.glint(new Vector3f(mirrorA).mul(0.3F - 0.6F * shine).add(new Vector3f(mirrorB).mul(0.2F)).add(hand[0]), s.camRight, s.camUp, 0.25F, 0.4F,
					argb(0.8F * m * Mth.sin(Mth.PI * shine), 0xFFFFFF));
			}
			// crimson flames licking along it
			for (int i = 0; i < 22; i++) {
				float t = hash(s.seed + i * 3);
				Vector3f at;
				if (i < 12) {
					float th = Mth.PI - (Mth.PI - 0.5F) * hash(i * 5);
					float y = (0.75F + 0.28F * (i % 6)) * rise;
					at = new Vector3f(ahead).mul(Mth.cos(th) * 0.95F).add(new Vector3f(right).mul((i % 2 == 0 ? 1 : -1) * Mth.sin(th) * 0.95F)).add(0, y, 0);
				} else {
					at = new Vector3f(shoulder[i % 2]).lerp(elbow[i % 2], t);
				}
				float hgt = (0.45F + 0.5F * hash(i * 7 + (int)(time / 5))) * k;
				o.flame(at, UP, hgt, 0.3F, time * 0.7F + i, argb(0.5F * k * self, SUSANOO), argb(0.0F, SUSANOO_PALE));
			}
			float flare = 1.0F - ease(age, 0.0F, 12.0F);
			o.wall(new Vector3f(), 1.0F, 0.0F, 2.8F * rise, argb((0.12F + 0.25F * flare) * k * self, SUSANOO), argb(0.0F, SUSANOO));
		});
		ps.popPose();
	}

	/**
	 * The Totsuka Blade: liquid pours out of the sealing gourd and is thrust out along the beam as a sword, a flowing sheath of pale gold
	 * liquid as wide as the beam hits round a bright core, strands winding along it, rings rolling down it, a blade's point at its end and a
	 * seal flaring there; drops of it spray off.
	 */
	private static void totsuka(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float len = Math.max(2.0F, s.size);
		Vector3f dir = PendragonLooks.beamDir(s);
		Vector3f side = VfxOut.across(dir);
		Vector3f up = PendragonLooks.beamUp(dir);
		float thrust = ease(age, 1.0F, 6.0F);
		float fade = 1.0F - ease(p, 0.6F, 1.0F);
		Vector3f mouth = new Vector3f(dir).mul(0.7F).add(new Vector3f(side).mul(0.35F)).add(new Vector3f(up).mul(-0.15F));
		Vector3f start = new Vector3f(dir).mul(0.9F);
		float reach = Math.max(0.0F, (len - 0.9F) * thrust);
		Vector3f front = new Vector3f(dir).mul(0.9F + reach);
		shade(ps, c, o -> {
			if (s.selfView) {
				return;
			}
			Vector3f g = new Vector3f(mouth).sub(new Vector3f(dir).mul(0.35F));
			o.ellipsoid(g, new Vector3f(side).mul(0.17F), new Vector3f(up).mul(0.17F), new Vector3f(dir).mul(0.2F), 5, 10, argb(0.95F * fade, GOURD),
				argb(0.95F * fade, GOURD_DARK));
			o.ellipsoid(new Vector3f(g).add(new Vector3f(dir).mul(0.27F)), new Vector3f(side).mul(0.11F), new Vector3f(up).mul(0.11F), new Vector3f(dir).mul(0.13F), 5, 10,
				argb(0.95F * fade, GOURD), argb(0.95F * fade, GOURD_DARK));
		});
		glow(ps, c, o -> {
			o.glowLine(mouth, start, 0.2F, argb(0.7F * fade, SAKE), argb(0.7F * fade, SAKE));
			if (reach <= 0.0F) {
				return;
			}
			o.glowLine(start, front, BEAM_R, argb(0.28F * fade, SAKE_GOLD), argb(0.22F * fade, SAKE_GOLD));
			o.glowLine(start, front, 0.3F, argb(0.8F * fade, SAKE), argb(0.7F * fade, SAKE));
			o.bar(start, front, 0.07F, 0.05F, argb(fade, 0xFFFFFF), argb(fade, 0xFFFFFF));
			// liquid strands winding along it
			for (int j = 0; j < 4; j++) {
				Vector3f prev = null;
				int steps = Math.max(8, (int)(reach * 2.5F));
				for (int i = 0; i <= steps; i++) {
					float d = reach * i / steps;
					float a = Mth.TWO_PI * j / 4 + d * 1.3F - time * 0.6F;
					float rr = BEAM_R * (0.55F + 0.15F * Mth.sin(d * 2.0F + time * 0.3F + j));
					Vector3f q = new Vector3f(dir).mul(0.9F + d).add(new Vector3f(side).mul(Mth.cos(a) * rr)).add(new Vector3f(up).mul(Mth.sin(a) * rr));
					if (prev != null) {
						o.glowLine(prev, q, 0.05F, argb(0.65F * fade, SAKE), argb(0.65F * fade, SAKE_GOLD));
					}
					prev = q;
				}
			}
			// the edge of what it hits: two lines along its sides and rings rolling out
			for (int sg = -1; sg <= 1; sg += 2) {
				o.glowLine(new Vector3f(side).mul(sg * BEAM_R).add(start), new Vector3f(side).mul(sg * BEAM_R).add(front), 0.08F, argb(0.45F * fade, SAKE_GOLD),
					argb(0.45F * fade, SAKE_GOLD));
			}
			for (int i = 0; i < 6; i++) {
				float d = ((i / 6.0F + time * 0.04F) % 1.0F) * reach;
				o.circle(new Vector3f(dir).mul(0.9F + d), side, up, BEAM_R, 0.04F, 24, argb(0.5F * fade, SAKE));
			}
			o.blade(front, new Vector3f(front).add(new Vector3f(dir).mul(1.4F)), up, 0.32F, argb(0.9F * fade, SAKE), argb(0.9F * fade, 0xFFFFFF));
			float seal = ease(age, 5.0F, 9.0F);
			if (seal > 0.0F) {
				o.magicCircle(new Vector3f(front).add(new Vector3f(dir).mul(0.6F)), side, up, 1.1F * seal, time * 0.08F, s.seed, 5, argb(0.7F * fade * seal, SAKE_GOLD),
					argb(0.6F * fade * seal, SAKE));
			}
			for (int i = 0; i < 14; i++) {
				float q = (time * 0.06F + hash(s.seed + i)) % 1.0F;
				Vector3f at = new Vector3f(dir).mul(0.9F + reach * hash(i * 7)).add(new Vector3f(side).mul((hash(i * 3) - 0.5F) * 2.0F * BEAM_R)).add(new Vector3f(up)
					.mul(BEAM_R * 0.5F + q * 0.8F)).add(0, -1.2F * q * q, 0);
				o.glint(at, s.camRight, s.camUp, 0.1F, i, argb(0.85F * fade * (1.0F - q), SAKE));
			}
		});
	}

	/**
	 * Amaterasu: the aimed ground catches black flame that will not go out: black tongues of fire crowded over the zone and thickest round
	 * its edge, a dark crimson glow under them and embers rising.
	 */
	private static void blackFlames(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = s.size;
		float time = s.ageInTicks;
		float open = ease(age, 0.0F, 6.0F);
		float fade = 1.0F - ease(p, 0.9F, 1.0F);
		float k = open * fade;
		int rim = 30;
		int inside = 18;
		Vector3f[] base = new Vector3f[rim + inside];
		float[] height = new float[rim + inside];
		for (int i = 0; i < rim + inside; i++) {
			if (i < rim) {
				base[i] = polar(Mth.TWO_PI * i / rim + hash(s.seed + i) * 0.15F, r * 0.92F * open, 0.1F);
				height[i] = (0.9F + 0.7F * hash(i * 7 + (int)(time / 6))) * k;
			} else {
				base[i] = polar(Mth.TWO_PI * hash(s.seed * 3 + i), r * 0.85F * (float)Math.sqrt(hash(i * 11)) * open, 0.1F);
				height[i] = (1.1F + 0.9F * hash(i * 13 + (int)(time / 7))) * k;
			}
		}
		shade(ps, c, o -> {
			o.disc(new Vector3f(0, 0.13F, 0), r * open, argb(0.7F * k, 0x0A0608), argb(0.4F * k, 0x0A0608));
			for (int i = 0; i < base.length; i++) {
				o.flame(base[i], UP, height[i], 0.62F, time * 0.6F + i * 1.7F, argb(0.95F * k, BLACK), argb(0.0F, BLACK));
			}
		});
		glow(ps, c, o -> {
			for (int i = 0; i < base.length; i += 2) {
				o.flame(new Vector3f(base[i]).add(0, -0.05F, 0), UP, height[i] * 1.15F, 0.8F, time * 0.6F + i * 1.7F, argb(0.3F * k, EMBER), argb(0.0F, EMBER));
			}
			o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, r - 0.3F), r, argb(0.5F * k, EMBER));
			for (int i = 0; i < 14; i++) {
				float q = (time * 0.03F + hash(s.seed + i * 5)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 17), r * 0.9F * (float)Math.sqrt(hash(i * 19)), 0.4F + q * 2.6F);
				o.sprite(at, s.camRight, s.camUp, 0.07F, argb(0.85F * k * Mth.sin(Mth.PI * q), 0xFF4060));
			}
		});
	}

	/**
	 * Tsukuyomi: the Mangekyo Sharingan turns before the caster's eyes, its gaze runs to the target as a black ray edged in red (as wide as
	 * the beam hits), and at its end the eye opens again, huge, before a red moon.
	 */
	private static void tsukuyomi(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float len = Math.max(2.0F, s.size);
		Vector3f dir = PendragonLooks.beamDir(s);
		Vector3f side = VfxOut.across(dir);
		Vector3f up = PendragonLooks.beamUp(dir);
		float open = ease(age, 0.0F, 4.0F);
		float ray = ease(age, 2.0F, 7.0F);
		float fade = 1.0F - ease(p, 0.65F, 1.0F);
		Vector3f near = new Vector3f(dir).mul(0.7F).add(new Vector3f(up).mul(0.15F));
		Vector3f start = new Vector3f(dir).mul(1.0F);
		float reach = Math.max(0.0F, (len - 1.0F) * ray);
		Vector3f front = new Vector3f(dir).mul(1.0F + reach);
		Vector3f far = new Vector3f(dir).mul(Math.max(1.5F, len - 0.5F));
		float big = ease(age, 6.0F, 11.0F);
		shade(ps, c, o -> {
			if (!s.selfView) {
				o.eye(near, side, up, 0.3F * open, -time * 0.08F, VfxOut.ITACHI, argb(0.97F * fade, IRIS), argb(0.97F * fade, INK));
			}
			if (reach > 0.0F) {
				o.bar(start, front, 0.14F, 0.1F, argb(0.9F * fade, BLACK), argb(0.7F * fade, BLACK));
			}
			if (big > 0.0F) {
				o.eye(far, side, up, 1.15F * big, time * 0.05F, VfxOut.ITACHI, argb(0.95F * fade, IRIS), argb(0.95F * fade, INK));
			}
		});
		glow(ps, c, o -> {
			if (reach > 0.0F) {
				o.glowLine(start, front, BEAM_R, argb(0.25F * fade, IRIS), argb(0.2F * fade, IRIS));
				for (int k = 0; k < 4; k++) {
					Vector3f off = new Vector3f(k < 2 ? up : side).mul((k % 2 == 0 ? 1 : -1) * BEAM_R);
					o.glowLine(new Vector3f(start).add(off), new Vector3f(front).add(off), 0.07F, argb(0.5F * fade, 0xFF2A2A), argb(0.5F * fade, 0xFF2A2A));
				}
				for (int i = 0; i < 6; i++) {
					float d = ((i / 6.0F + time * 0.05F) % 1.0F) * reach;
					o.circle(new Vector3f(dir).mul(1.0F + d), side, up, BEAM_R, 0.035F, 24, argb(0.55F * fade, 0xFF3030));
				}
			}
			if (big > 0.0F) {
				// the red moon behind the eye
				o.sprite(new Vector3f(far).add(new Vector3f(dir).mul(0.4F)).add(new Vector3f(up).mul(0.5F)), s.camRight, s.camUp, 2.6F * big, argb(0.4F * fade * big, 0xC81020));
				o.circle(far, side, up, 1.2F * big, 0.05F, 32, argb(0.6F * fade, 0xFF3030));
			}
		});
	}

	/**
	 * Crow Clone: the caster bursts into a flock of black crows that fly out and wheel round them, their red Sharingan eyes glinting,
	 * black feathers drifting down.
	 */
	private static void crows(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float out = ease(age, 0.0F, 8.0F);
		float fade = 1.0F - ease(p, 0.75F, 1.0F);
		int n = 16;
		Vector3f[] at = new Vector3f[n];
		Vector3f[] heading = new Vector3f[n];
		for (int i = 0; i < n; i++) {
			float dirSign = i % 3 == 0 ? -1.0F : 1.0F;
			float a = Mth.TWO_PI * hash(s.seed + i) + dirSign * time * (0.06F + 0.03F * hash(i * 3));
			float rr = (1.3F + 1.4F * hash(i * 7)) * out;
			float y = 1.1F + (0.9F * hash(i * 11) - 0.3F) * out + 0.15F * Mth.sin(time * 0.15F + i);
			at[i] = polar(a, rr, y);
			heading[i] = polar(a + dirSign * Mth.HALF_PI, 1.0F, 0.1F * Mth.cos(time * 0.15F + i));
		}
		shade(ps, c, o -> {
			for (int i = 0; i < n; i++) {
				if (s.selfView && at[i].y > 0.9F && at[i].x * at[i].x + at[i].z * at[i].z < 1.0F) {
					continue;
				}
				o.bird(at[i], heading[i], UP, 0.5F, Mth.sin(time * 1.1F + i * 1.3F), argb(0.95F * fade, 0x101014), argb(0.92F * fade, 0x18181F));
			}
			for (int i = 0; i < 10; i++) {
				float q = (time * 0.02F + hash(s.seed * 5 + i)) % 1.0F;
				Vector3f f = polar(Mth.TWO_PI * hash(i * 13), 0.5F + 2.0F * hash(i * 17), 2.6F - 2.4F * q);
				float spin = time * 0.1F + i;
				o.diamond(f, new Vector3f(Mth.cos(spin), 0.3F, Mth.sin(spin)), new Vector3f(-Mth.sin(spin), 0.0F, Mth.cos(spin)), 0.22F, 0.07F,
					argb(0.85F * fade * Mth.sin(Mth.PI * q), 0x141418), argb(0.85F * fade * Mth.sin(Mth.PI * q), 0x141418));
			}
		});
		glow(ps, c, o -> {
			for (int i = 0; i < n; i++) {
				if (s.selfView && at[i].y > 0.9F && at[i].x * at[i].x + at[i].z * at[i].z < 1.0F) {
					continue;
				}
				o.sprite(new Vector3f(heading[i]).mul(0.24F).add(at[i]).add(0, 0.04F, 0), s.camRight, s.camUp, 0.06F, argb(fade, 0xFF2020));
				o.sprite(at[i], s.camRight, s.camUp, 0.6F, argb(0.1F * fade, 0xB8B0D0));
			}
			float burst = 1.0F - ease(age, 0.0F, 8.0F);
			o.sprite(new Vector3f(0, 1.1F, 0), s.camRight, s.camUp, 1.4F * burst, argb(0.3F * burst, IRIS));
		});
	}

	// ------------------------------------------------------------ Madara

	/**
	 * Uchiha Return: Madara's gunbai is raised before the caster and drinks in the force round it, spiralling into its face; when the blast is
	 * thrown back the fan flares white and a shock wave runs out to the edge of the blast, a ring flashing there.
	 */
	private static void uchihaReturn(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = s.size;
		float time = s.ageInTicks;
		float raise = ease(age, 0.0F, 4.0F);
		float blast = ease(age, 20.0F, 26.0F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		float fanR = s.selfView ? 0.5F : 0.75F;
		Vector3f fan = new Vector3f(ahead).mul(s.selfView ? 1.2F : 0.9F).add(new Vector3f(right).mul(s.selfView ? 0.35F : 0.0F)).add(0, (s.selfView ? 1.05F : 1.45F)
			+ 0.25F * (1.0F - raise), 0);
		shade(ps, c, o -> o.gunbai(fan, ahead, UP, fanR * raise, fade));
		glow(ps, c, o -> {
			if (age < 20.0F) {
				float charge = ease(age, 2.0F, 20.0F);
				for (int a = 0; a < 6; a++) {
					Vector3f prev = null;
					for (int i = 0; i <= 12; i++) {
						float t = i / 12.0F;
						float rr = 3.0F * (1.0F - t) + 0.1F;
						float ang = Mth.TWO_PI * a / 6 - time * 0.35F + t * 3.5F;
						Vector3f q = new Vector3f(right).mul(Mth.cos(ang) * rr).add(new Vector3f(UP).mul(Mth.sin(ang) * rr)).add(new Vector3f(ahead).mul(1.2F * (1.0F - t)))
							.add(fan);
						if (prev != null) {
							o.glowLine(prev, q, 0.06F, argb(0.6F * t * fade, FIRE_PALE), argb(0.6F * t * fade, 0xFFFFFF));
						}
						prev = q;
					}
				}
				o.sprite(fan, s.camRight, s.camUp, fanR * (0.8F + 0.8F * charge), argb(0.35F * charge, FIRE_PALE));
				// where the stored force will land
				o.ring(new Vector3f(0, 0.14F, 0), Math.max(0.0F, r - 0.25F), r, argb(0.25F * raise * fade, FIRE));
				return;
			}
			float flash = 1.0F - ease(age, 20.0F, 26.0F);
			o.star(fan, s.camRight, s.camUp, 2.5F * flash + 0.2F, 10, 0.25F, s.seed, argb(0.95F * flash, 0xFFFFFF), argb(0.0F, FIRE_PALE));
			o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, r * blast - 0.9F), r * blast, argb(0.9F * (1.0F - blast * 0.5F) * fade, 0xFFFFFF));
			o.wall(new Vector3f(), r * blast, 0.0F, 1.8F * (1.0F - blast), argb(0.35F * (1.0F - blast), FIRE_PALE), argb(0.0F, FIRE_PALE));
			o.ring(new Vector3f(0, 0.14F, 0), Math.max(0.0F, r - 0.4F), r, argb(0.8F * fade, FIRE));
			for (int i = 0; i < 16; i++) {
				float a = Mth.TWO_PI * i / 16 + hash(s.seed + i) * 0.3F;
				Vector3f d = polar(a, 1.0F, 0.0F);
				float d0 = r * Math.max(0.0F, blast - 0.35F);
				o.glowLine(new Vector3f(d).mul(d0).add(0, 0.9F, 0), new Vector3f(d).mul(r * blast).add(0, 0.9F, 0), 0.1F, argb(0.0F, WIND), argb(0.7F * fade, WIND));
			}
		});
	}

	/**
	 * Majestic Destroyer Flame: a wall of fire rolls out from the caster to the aimed spot, and there a sea of flame bursts up over the whole
	 * blast, a ring of fire on its edge, smoke rolling over it.
	 */
	private static void destroyerFlame(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float r = s.size;
		float time = s.ageInTicks;
		float dist = Math.max(0.0F, s.arg);
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		float roll = ease(age, 0.0F, 8.0F);
		float sea = ease(age, 7.0F, 11.0F);
		float fade = 1.0F - ease(p, 0.72F, 1.0F);
		// the wave: from the caster to the middle of the blast, widening from a stride to the blast's width
		float d = -dist * (1.0F - roll);
		float half = Mth.lerp(roll, 0.8F, r * 0.9F);
		float waveFade = 1.0F - ease(age, 8.0F, 12.0F);
		int waveN = 13;
		int seaN = 40;
		Vector3f[] base = new Vector3f[seaN];
		float[] height = new float[seaN];
		for (int i = 0; i < seaN; i++) {
			boolean edge = i < 18;
			float a = Mth.TWO_PI * (edge ? i / 18.0F : hash(s.seed + i * 3));
			float rr = edge ? r * 0.93F : r * 0.85F * (float)Math.sqrt(hash(i * 7));
			base[i] = polar(a, rr * sea, 0.1F);
			height[i] = (edge ? 1.2F : 1.6F + 1.2F * hash(i * 5 + (int)(time / 5))) * sea * fade;
		}
		shade(ps, c, o -> {
			if (sea > 0.0F) {
				for (int i = 0; i < 10; i++) {
					float q = (time * 0.025F + hash(s.seed * 7 + i)) % 1.0F;
					Vector3f at = polar(Mth.TWO_PI * hash(i * 9), r * 0.7F * hash(i * 13), 2.0F + q * 3.0F);
					o.sprite(at, s.camRight, s.camUp, 1.2F + 1.2F * q, argb(0.35F * sea * fade * (1.0F - q), 0x241810));
				}
			}
		});
		glow(ps, c, o -> {
			if (waveFade > 0.0F && dist > 0.5F) {
				for (int i = 0; i < waveN; i++) {
					float t = (i / (waveN - 1.0F) - 0.5F) * 2.0F;
					Vector3f at = new Vector3f(f).mul(d - 0.3F * t * t).add(new Vector3f(side).mul(t * half)).add(0, 0.1F, 0);
					float h = (1.8F + 1.0F * hash(i * 7 + (int)(time / 3))) * waveFade;
					o.flame(at, new Vector3f(f).mul(0.25F).add(UP).normalize(), h, 0.9F, time * 0.8F + i, argb(0.8F * waveFade, FIRE_PALE), argb(0.0F, FIRE_DEEP));
				}
			}
			if (sea <= 0.0F) {
				return;
			}
			o.disc(new Vector3f(0, 0.14F, 0), r * sea, argb(0.35F * fade, FIRE), argb(0.1F * fade, FIRE_DEEP));
			for (int i = 0; i < seaN; i++) {
				o.flame(base[i], UP, height[i], i < 18 ? 0.8F : 1.0F, time * 0.7F + i * 1.3F, argb(0.75F * fade, i % 3 == 0 ? FIRE_PALE : FIRE), argb(0.0F, FIRE_DEEP));
			}
			o.ring(new Vector3f(0, 0.16F, 0), Math.max(0.0F, r * sea - 0.4F), r * sea, argb(0.85F * fade, FIRE_PALE));
			float flash = 1.0F - ease(age, 8.0F, 14.0F);
			if (flash > 0.0F && age >= 8.0F) {
				o.sprite(new Vector3f(0, 1.0F, 0), s.camRight, s.camUp, r * flash + 0.5F, argb(0.7F * flash, FIRE_PALE));
			}
			for (int i = 0; i < 12; i++) {
				float q = (time * 0.05F + hash(s.seed + i)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 23), r * (float)Math.sqrt(hash(i * 29)), 0.5F + q * 3.5F);
				o.sprite(at, s.camRight, s.camUp, 0.08F, argb(0.9F * fade * Mth.sin(Mth.PI * q), FIRE_PALE));
			}
		});
	}

	/**
	 * Gunbai Sweep: the war fan swung across the slash's arc at arm's length, a gust of wind sweeping out in front of it in streaks to the
	 * edge of the arc, a bright line on that edge and dust thrown.
	 */
	private static void gunbaiSweep(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float r = Math.max(1.0F, s.size);
		float arc = Math.max(20.0F, s.arg) * Mth.DEG_TO_RAD;
		float sweep = ease(age, 0.0F, 4.0F);
		float fade = 1.0F - ease(age, 4.0F, 10.0F);
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		float from = arc / 2;
		float now = from - arc * sweep;
		Vector3f dirNow = new Vector3f(f).mul(Mth.cos(now)).add(new Vector3f(side).mul(Mth.sin(now)));
		Vector3f tangent = new Vector3f(f).mul(-Mth.sin(now)).add(new Vector3f(side).mul(Mth.cos(now))).negate();
		float hold = s.selfView ? 0.9F : 1.1F;
		Vector3f fan = new Vector3f(dirNow).mul(hold).add(0, 0.1F, 0);
		shade(ps, c, o -> o.gunbai(fan, tangent, new Vector3f(UP).add(new Vector3f(dirNow).mul(0.4F)), s.selfView ? 0.35F : 0.5F, fade));
		glow(ps, c, o -> {
			int segs = 20;
			for (int band = 0; band < 4; band++) {
				float rr = band == 3 ? r : r * (0.4F + 0.17F * band);
				float y = band == 3 ? 0.0F : 0.25F * (band - 1);
				Vector3f prev = null;
				for (int i = 0; i <= segs; i++) {
					float t = (float)i / segs;
					float a = Mth.lerp(t, from, now);
					Vector3f q = new Vector3f(f).mul(Mth.cos(a) * rr).add(new Vector3f(side).mul(Mth.sin(a) * rr)).add(0, y, 0);
					if (prev != null) {
						float al = (band == 3 ? 0.85F : 0.5F) * fade * (0.3F + 0.7F * t);
						o.glowLine(prev, q, band == 3 ? 0.09F : 0.06F, argb(al, WIND), argb(al, WIND));
					}
					prev = q;
				}
			}
			for (int i = 0; i < 10; i++) {
				float a = Mth.lerp(hash(s.seed + i), from, now);
				float rr = r * (0.5F + 0.5F * hash(i * 3));
				Vector3f at = new Vector3f(f).mul(Mth.cos(a) * rr).add(new Vector3f(side).mul(Mth.sin(a) * rr)).add(0, -0.3F + 0.6F * hash(i * 7), 0);
				o.glint(at, s.camRight, s.camUp, 0.1F, i, argb(0.7F * fade, DUST));
			}
		});
	}

	/**
	 * Gunbai Leap: the war fan held high over the leaping caster; when they land it comes down flat in front of them, a ring of wind and dust
	 * running out to the slam's reach and staying on its edge.
	 */
	private static void gunbaiLeap(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.82F, 1.0F);
		float since = AntLooks.landed(s);
		Vector3f holder = s.trail.isEmpty() ? new Vector3f() : new Vector3f(s.trail.getLast());
		Vector3f land = AntLooks.landing(s);
		Vector3f ahead = WitchLooks.ahead(s);
		float r = s.size;
		float slam = since < 0.0F ? 0.0F : ease(since, 0.0F, 3.0F);
		Vector3f overhead = new Vector3f(holder).add(new Vector3f(ahead).mul(-0.1F)).add(0, 2.7F, 0);
		Vector3f planted = new Vector3f(land).add(new Vector3f(ahead).mul(1.3F)).add(0, 0.25F, 0);
		Vector3f fan = new Vector3f(overhead).lerp(planted, slam);
		Vector3f normal = new Vector3f(ahead).lerp(new Vector3f(UP), slam).normalize();
		Vector3f fanUp = new Vector3f(UP).lerp(ahead, slam).normalize();
		shade(ps, c, o -> {
			if (!s.selfView || slam > 0.5F) {
				o.gunbai(fan, normal, fanUp, 0.75F, fade);
			}
			if (since >= 0.0F) {
				float q = ease(since, 0.0F, 14.0F);
				for (int i = 0; i < 12; i++) {
					float a = Mth.TWO_PI * i / 12 + hash(s.seed + i);
					Vector3f at = polar(a, r * (0.3F + 0.65F * q), 0.4F + 0.5F * q).add(land);
					o.sprite(at, s.camRight, s.camUp, 0.6F + 0.6F * q, argb(0.35F * (1.0F - q) * fade, 0x6A5A48));
				}
			}
		});
		glow(ps, c, o -> {
			if (since < 0.0F) {
				o.sprite(fan, s.camRight, s.camUp, 0.9F, argb(0.25F * fade, FIRE));
				return;
			}
			float impact = ease(since, 0.0F, 10.0F);
			Vector3f g = new Vector3f(land).add(0, 0.15F, 0);
			o.ring(g, Math.max(0.0F, r * impact - 0.6F), r * impact, argb(0.85F * (1.0F - impact), WIND));
			o.ring(new Vector3f(land).add(0, 0.14F, 0), Math.max(0.0F, r - 0.3F), r, argb(0.6F * fade * (1.0F - ease(since, 10.0F, 30.0F)), FIRE));
			for (int i = 0; i < 12; i++) {
				float a = Mth.TWO_PI * i / 12 + hash(s.seed * 3 + i);
				Vector3f d = polar(a, 1.0F, 0.0F);
				o.glowLine(new Vector3f(d).mul(r * Math.max(0.0F, impact - 0.4F)).add(g).add(0, 0.3F, 0), new Vector3f(d).mul(r * impact).add(g).add(0, 0.3F, 0), 0.08F,
					argb(0.0F, WIND), argb(0.6F * fade * (1.0F - impact * 0.5F), WIND));
			}
			float flash = 1.0F - ease(since, 0.0F, 5.0F);
			o.star(new Vector3f(planted).add(0, 0.3F, 0), s.camRight, s.camUp, 1.5F * flash + 0.1F, 8, 0.25F, s.seed, argb(0.8F * flash, 0xFFFFFF), argb(0.0F, FIRE));
		});
	}

	private UchihaLooks() {
	}
}
