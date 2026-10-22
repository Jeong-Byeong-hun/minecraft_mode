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
 * Looks of Ryomen Sukuna's raid (Jujutsu Kaisen), shown by the boss rather than a skill: Dismantle's hair-thin cuts, the flurry of
 * Cleave, Black Flash's black lightning, Spiderweb's cracks crawling over the floor, Fuga's bow, arrow and blast of fire, the
 * World-Cutting Slash tearing through space, and the cuts of the domain's sure hit.
 */
final class CurseLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final int CUT = 0xF4F4FF;
	private static final int RED = 0xC0182A;
	private static final int INK = 0x0A0204;
	private static final int FLAME = 0xFF7A1A;
	private static final int FLAME_HOT = 0xFFE090;
	/** Where an unseen cut shows, above the floor. */
	private static final float CHEST = 1.2F;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case DISMANTLE -> dismantle(s, ps, c);
			case CLEAVE_CUTS -> cleave(s, ps, c);
			case BLACK_FLASH -> blackFlash(s, ps, c);
			case SPIDERWEB -> spiderweb(s, ps, c);
			case FUGA_BOW -> fugaBow(s, ps, c);
			case FUGA_FLIGHT -> fugaFlight(s, ps, c);
			case FUGA_BLAST -> fugaBlast(s, ps, c);
			case WORLD_SLASH -> worldSlash(s, ps, c);
			case DOMAIN_CUT -> domainCut(s, ps, c);
			default -> {
			}
		}
	}

	/**
	 * Dismantle: for an instant the unseen cut shows as a hair-thin white line along its whole length with a red haze round it, a flash
	 * running from Sukuna to its end; a red scar stays on the floor under it and fades.
	 */
	private static void dismantle(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float flash = 1.0F - ease(age, 2.0F, 8.0F);
		float scar = 1.0F - ease(s.progress, 0.3F, 1.0F);
		Vector3f a = new Vector3f(0, CHEST, 0);
		Vector3f b = new Vector3f(s.reach).add(0, CHEST, 0);
		Vector3f run = new Vector3f(a).lerp(b, ease(age, 0.0F, 3.0F));
		glow(ps, c, o -> {
			if (flash > 0.0F) {
				o.glowLine(a, run, 0.12F, argb(0.95F * flash, CUT), argb(0.85F * flash, CUT));
				o.glowLine(a, run, 0.55F, argb(0.4F * flash, RED), argb(0.25F * flash, RED));
				o.sprite(run, s.camRight, s.camUp, 0.55F, argb(0.9F * flash, CUT));
			}
			o.glowLine(new Vector3f(0, 0.13F, 0), new Vector3f(s.reach.x(), 0.13F, s.reach.z()), 0.1F, argb(0.6F * scar, RED), argb(0.2F * scar, RED));
		});
	}

	/** Cleave: seven thin cuts flash one after another at random tilts inside the cone in front of him, white with a red edge. */
	private static void cleave(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float range = Math.max(1.0F, s.reach.length());
		float arc = Math.max(10.0F, s.arg) * Mth.DEG_TO_RAD;
		Vector3f side = VfxOut.across(s.forward);
		glow(ps, c, o -> {
			for (int i = 0; i < 7; i++) {
				float q = age - i * 0.8F;
				if (q < 0.0F || q > 6.0F) {
					continue;
				}
				float alpha = 1.0F - q / 6.0F;
				float angle = (hash(s.seed * 31 + i) - 0.5F) * arc;
				float dist = 1.2F + hash(s.seed * 17 + i) * (range - 1.5F);
				Vector3f center = new Vector3f(s.forward).rotateY(-angle).mul(dist).add(0, 0.6F + hash(s.seed * 7 + i) * 2.4F, 0);
				float tilt = (hash(s.seed * 13 + i) - 0.5F) * Mth.PI;
				float half = 1.4F + hash(s.seed * 19 + i) * 1.4F;
				Vector3f cut = new Vector3f(side).mul(Mth.cos(tilt)).add(new Vector3f(UP).mul(Mth.sin(tilt))).mul(half);
				Vector3f from = new Vector3f(center).sub(cut);
				Vector3f to = new Vector3f(center).add(cut);
				o.glowLine(from, to, 0.11F, argb(alpha, CUT), argb(alpha, CUT));
				o.glowLine(from, to, 0.5F, argb(0.45F * alpha, RED), argb(0.45F * alpha, RED));
			}
		});
	}

	/**
	 * Black Flash: the blow sparks black lightning, a red glow round every bolt, bursting out from the hit as wide as it reaches; a red
	 * flash at the middle and a crack ring running out over the floor.
	 */
	private static void blackFlash(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float out = ease(age, 0.0F, 3.0F);
		float fade = 1.0F - ease(s.progress, 0.35F, 1.0F);
		float flash = 1.0F - ease(age, 0.0F, 6.0F);
		Vector3f mid = new Vector3f(0, CHEST, 0);
		Vector3f[] tips = new Vector3f[10];
		for (int i = 0; i < tips.length; i++) {
			float a = Mth.TWO_PI * i / tips.length + hash(s.seed + i) * 0.5F;
			float rise = -0.5F + hash(s.seed * 3 + i) * 1.6F;
			float len = s.size * (0.7F + 0.6F * hash(s.seed * 5 + i)) * out;
			tips[i] = polar(a, len, rise * len * 0.6F).add(mid);
		}
		shade(ps, c, o -> {
			for (int i = 0; i < tips.length; i++) {
				o.polyline(VfxOut.zigzag(mid, tips[i], s.seed * 11 + i, 0.45F, 6), 0.05F, argb(0.92F * fade, INK));
			}
		});
		glow(ps, c, o -> {
			for (int i = 0; i < tips.length; i++) {
				o.glowPolyline(VfxOut.zigzag(mid, tips[i], s.seed * 11 + i, 0.45F, 6), 0.24F, argb(0.7F * fade, RED));
			}
			o.sprite(mid, s.camRight, s.camUp, s.size * 0.9F, argb(0.75F * flash, RED));
			o.sprite(mid, s.camRight, s.camUp, s.size * 0.35F, argb(0.6F * flash, CUT));
			float ring = s.size * 1.3F * out;
			o.ring(new Vector3f(0, 0.14F, 0), Math.max(0.0F, ring - 0.35F), ring, argb(0.6F * fade, RED));
		});
	}

	/**
	 * Spiderweb: cracks crawl out over the floor from his hand, eight jagged spokes and rings joining them, dark red glowing in black
	 * fissures; when the cut lands ({@code arg} ticks in) they flare white-red and a pulse runs round the middle ring, then they fade.
	 */
	private static void spiderweb(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float delay = Math.max(1.0F, s.arg);
		float grow = ease(age, 0.0F, delay);
		float burst = age >= delay ? 1.0F - ease(age, delay, delay + 10.0F) : 0.0F;
		float fade = 1.0F - ease(age, delay + 6.0F, s.life);
		float reach = s.size * grow;
		float turn = (float)Math.atan2(s.reach.z(), s.reach.x());
		float[] rings = {s.size * 0.25F, s.size * 0.54F, s.size * 0.8F};
		Vector3f center = new Vector3f(0, 0.13F, 0);
		Vector3f[][] spokes = new Vector3f[8][];
		for (int k = 0; k < 8; k++) {
			Vector3f end = polar(turn + k * Mth.PI / 4, s.size, 0.13F);
			List<Vector3f> line = VfxOut.zigzag(center, end, s.seed * 13 + k, 0.5F, 10);
			// only as far out as the cracks have crawled
			int n = Math.max(1, Math.round(10 * grow));
			spokes[k] = line.subList(0, n + 1).toArray(new Vector3f[0]);
		}
		int red = argb(Math.min(1.0F, 0.45F * fade + 0.5F * burst), burst > 0.0F ? VfxOut.lerp(RED, CUT, 0.4F * burst) : RED);
		shade(ps, c, o -> {
			for (Vector3f[] line : spokes) {
				o.polyline(List.of(line), 0.06F, argb(0.85F * fade, INK));
			}
		});
		glow(ps, c, o -> {
			for (Vector3f[] line : spokes) {
				o.glowPolyline(List.of(line), 0.16F, red);
			}
			for (float r : rings) {
				if (r > reach) {
					continue;
				}
				for (int k = 0; k < 8; k++) {
					// sagging threads between two spokes, as a web's
					Vector3f a = polar(turn + k * Mth.PI / 4, r, 0.14F);
					Vector3f b = polar(turn + (k + 1) * Mth.PI / 4, r, 0.14F);
					Vector3f m = polar(turn + (k + 0.5F) * Mth.PI / 4, r * 0.9F, 0.14F);
					o.glowLine(a, m, 0.12F, red, red);
					o.glowLine(m, b, 0.12F, red, red);
				}
			}
			if (burst > 0.0F) {
				o.ring(center, Math.max(0.0F, rings[1] - 0.6F), rings[1] + 0.6F, argb(0.7F * burst, CUT));
			}
		});
	}

	/**
	 * Fuga: a bow of flame drawn open between his hands in front of his chest ({@code arg} is his height), its string a white-hot line and
	 * a burning arrow nocked on it, all flaring as the shot comes. In the facing he had when he began.
	 */
	private static void fugaBow(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float draw = ease(age, 0.0F, 12.0F);
		float fade = 1.0F - ease(s.progress, 0.9F, 1.0F);
		float k = draw * fade;
		float h = Math.max(1.8F, s.arg);
		Vector3f f = s.forward;
		Vector3f side = VfxOut.across(f);
		Vector3f grip = new Vector3f(f).mul(1.3F).add(0, h * 0.58F, 0);
		float r = 1.5F;
		Vector3f top = new Vector3f(UP).mul(r).add(new Vector3f(f).mul(-0.5F)).add(grip);
		Vector3f bottom = new Vector3f(UP).mul(-r).add(new Vector3f(f).mul(-0.5F)).add(grip);
		Vector3f nock = new Vector3f(f).mul(-0.7F * draw - 0.4F).add(grip);
		glow(ps, c, o -> {
			// the bow's limbs: an arc of flame from top to bottom through the grip
			Vector3f prev = top;
			for (int i = 1; i <= 10; i++) {
				float t = i / 10.0F;
				float a = Mth.PI * (t - 0.5F);
				Vector3f p = new Vector3f(UP).mul(Mth.sin(a) * r).add(new Vector3f(f).mul(Mth.cos(a) * 0.5F - 0.5F)).add(grip);
				o.glowLine(prev, p, 0.18F, argb(0.85F * k, FLAME), argb(0.85F * k, FLAME));
				if (i % 2 == 0) {
					o.flame(p, UP, 0.5F + 0.3F * hash(s.seed + i), 0.18F, s.ageInTicks * 0.5F + i, argb(0.8F * k, FLAME), argb(0.0F, FLAME_HOT));
				}
				prev = p;
			}
			// the string drawn back to the nock, and the arrow on it
			o.glowLine(top, nock, 0.05F, argb(0.9F * k, FLAME_HOT), argb(0.9F * k, FLAME_HOT));
			o.glowLine(bottom, nock, 0.05F, argb(0.9F * k, FLAME_HOT), argb(0.9F * k, FLAME_HOT));
			Vector3f head = new Vector3f(f).mul(1.6F).add(grip);
			o.glowLine(nock, head, 0.12F, argb(0.95F * k, FLAME_HOT), argb(0.95F * k, 0xFFFFFF));
			o.blade(head, new Vector3f(f).mul(0.7F).add(head), side, 0.25F, argb(0.9F * k, FLAME), argb(0.9F * k, FLAME_HOT));
			o.sprite(grip, s.camRight, s.camUp, 1.8F, argb(0.25F * k, FLAME));
			o.sprite(head, s.camRight, s.camUp, 0.5F + 0.4F * ease(age, 20.0F, 40.0F), argb(0.8F * k, FLAME_HOT));
		});
	}

	/** Fuga's flight: the burning arrow streaking from his hands to where it lands, a white-hot head and a tail of fire behind it. */
	private static void fugaFlight(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float t = ease(s.age(), 0.0F, s.life);
		Vector3f head = new Vector3f(s.reach).mul(t);
		Vector3f tail = new Vector3f(s.reach).mul(Math.max(0.0F, t - 0.35F));
		Vector3f side = VfxOut.across(new Vector3f(s.reach).normalize());
		glow(ps, c, o -> {
			o.glowLine(tail, head, 0.45F, argb(0.6F, FLAME), argb(0.9F, FLAME));
			o.glowLine(tail, head, 0.12F, argb(0.4F, FLAME_HOT), argb(1.0F, 0xFFFFFF));
			o.blade(head, new Vector3f(s.reach).normalize().mul(1.1F).add(head), side, 0.35F, argb(0.95F, FLAME_HOT), argb(0.95F, 0xFFFFFF));
			o.sprite(head, s.camRight, s.camUp, 1.1F, argb(0.7F, FLAME));
		});
	}

	/**
	 * Fuga's blast: a dome of fire swelling to its full width with a flash and a shock ring, then the crater burning on: tongues of flame
	 * flickering over it and a scorched floor until it goes out.
	 */
	private static void fugaBlast(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float swell = ease(age, 0.0F, 8.0F);
		float dome = 1.0F - ease(age, 6.0F, 18.0F);
		float burn = 1.0F - ease(s.progress, 0.8F, 1.0F);
		float r = s.size;
		Vector3f ground = new Vector3f(0, 0.13F, 0);
		shade(ps, c, o -> o.disc(ground, r * 0.85F, argb(0.6F * burn, 0x140604), argb(0.0F, 0x140604)));
		if (dome > 0.0F) {
			ps.pushPose();
			ps.translate(0.0F, 0.5F, 0.0F);
			c.submitCustomGeometry(ps, VfxDraw.GLOW, (pose, buffer) -> {
				VfxDraw.ball(pose, buffer, r * swell, FLAME, 0.35F * dome);
				VfxDraw.ball(pose, buffer, r * 0.6F * swell, FLAME_HOT, 0.4F * dome);
			});
			ps.popPose();
		}
		glow(ps, c, o -> {
			float ring = r * 1.2F * swell;
			o.ring(ground, Math.max(0.0F, ring - 0.6F), ring, argb(0.7F * (1.0F - ease(age, 6.0F, 20.0F)), FLAME_HOT));
			for (int i = 0; i < 26; i++) {
				float a = Mth.TWO_PI * hash(s.seed * 7 + i);
				float d = r * 0.8F * Mth.sqrt(hash(s.seed * 3 + i));
				Vector3f base = polar(a, d, 0.1F);
				float height = (0.8F + 1.6F * hash(s.seed + i)) * burn * Math.min(1.0F, age / 6.0F);
				o.flame(base, UP, height, 0.35F, s.ageInTicks * 0.4F + i, argb(0.85F * burn, FLAME), argb(0.0F, FLAME_HOT));
			}
			o.disc(ground, r * 0.7F, argb(0.25F * burn, FLAME), argb(0.0F, FLAME));
		});
	}

	/**
	 * The World-Cutting Slash: while he chants ({@code arg} ticks) a crack in space flickers along the whole line, black with white glints
	 * and a widening red haze; then one white cut stands through it all, a sheet of light as tall as a house, and leaves a black scar in
	 * the air that fades.
	 */
	private static void worldSlash(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float delay = Math.max(1.0F, s.arg);
		float charge = ease(age, 0.0F, delay);
		float cut = age >= delay ? 1.0F - ease(age, delay, delay + 12.0F) : 0.0F;
		float scar = age >= delay ? 1.0F - ease(age, delay + 4.0F, s.life) : charge;
		Vector3f a = new Vector3f(0, 1.5F, 0);
		Vector3f b = new Vector3f(s.reach).add(0, 1.5F, 0);
		float flicker = 0.7F + 0.3F * Mth.sin(s.ageInTicks * 1.7F);
		shade(ps, c, o -> o.bar(a, b, 0.05F + 0.04F * charge, 0.05F + 0.04F * charge, argb(0.9F * scar, INK), argb(0.9F * scar, INK)));
		glow(ps, c, o -> {
			o.glowLine(a, b, 0.2F + 0.6F * charge, argb(0.35F * charge * flicker * (1.0F - cut), RED), argb(0.35F * charge * flicker * (1.0F - cut), RED));
			int glints = 14;
			for (int i = 0; i < glints; i++) {
				float t = hash(s.seed * 5 + i + (int)(s.ageInTicks * 0.5F));
				o.sprite(new Vector3f(a).lerp(b, t), s.camRight, s.camUp, 0.25F, argb(0.8F * charge * flicker, CUT));
			}
			if (cut > 0.0F) {
				// the cut itself: a sheet of white light standing along the line
				float tall = 6.0F;
				Vector3f a0 = new Vector3f(a).add(0, -1.5F, 0);
				Vector3f b0 = new Vector3f(b).add(0, -1.5F, 0);
				Vector3f a1 = new Vector3f(a0).add(0, tall, 0);
				Vector3f b1 = new Vector3f(b0).add(0, tall, 0);
				o.quad(a0, b0, b1, a1, argb(0.9F * cut, CUT), argb(0.9F * cut, CUT), argb(0.0F, CUT), argb(0.0F, CUT));
				o.glowLine(a, b, 0.6F * s.size * cut, argb(0.9F * cut, CUT), argb(0.9F * cut, CUT));
				o.glowLine(a, b, 1.6F * s.size * cut, argb(0.4F * cut, RED), argb(0.4F * cut, RED));
			}
		});
	}

	/** The domain's sure hit: three thin cuts flash round someone it reaches (round the body, never across their eyes). */
	private static void domainCut(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float alpha = 1.0F - s.progress;
		glow(ps, c, o -> {
			for (int i = 0; i < 3; i++) {
				float a = Mth.TWO_PI * hash(s.seed * 7 + i);
				Vector3f center = polar(a, 0.7F, 0.3F + 1.0F * hash(s.seed * 3 + i));
				float tilt = (hash(s.seed * 11 + i) - 0.5F) * Mth.PI;
				Vector3f cut = new Vector3f(VfxOut.across(polar(a, 1.0F, 0.0F))).mul(Mth.cos(tilt)).add(new Vector3f(UP).mul(Mth.sin(tilt))).mul(0.8F);
				Vector3f from = new Vector3f(center).sub(cut);
				Vector3f to = new Vector3f(center).add(cut);
				o.glowLine(from, to, 0.04F, argb(alpha, CUT), argb(alpha, CUT));
				o.glowLine(from, to, 0.2F, argb(0.5F * alpha, RED), argb(0.5F * alpha, RED));
			}
		});
	}

	private CurseLooks() {
	}
}
