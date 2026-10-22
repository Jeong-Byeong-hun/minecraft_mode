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
 * Looks of Adult Gon (Hunter x Hunter): the towering black aura of his rage, Jajanken's Rock (the charged fist in a leap and the crash where
 * it lands, the giant fist of "this is all I need" and its blast) and Scissors grown into a long blade of aura.
 */
final class GonLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final Vector3f DOWN = new Vector3f(0, -1, 0);
	private static final int GOLD = 0xFFD040;
	private static final int GOLD_PALE = 0xFFF4C0;
	private static final int GOLD_DEEP = 0xE08A10;
	private static final int DARK = 0x0A0806;
	private static final int HAIR = 0x050404;
	private static final int FIST_LIT = 0xFFF0B8;
	private static final int FIST_DARK = 0xC88A28;
	private static final int CRACK = 0x1A120A;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case GON_RAGE -> rage(s, ps, c);
			case JAJANKEN_ROCK -> firstComesRock(s, ps, c);
			case LONG_SCISSORS -> longScissors(s, ps, c);
			case GON_ROCK -> giantFist(s, ps, c);
			case ROCK_BLAST -> rockBlast(s, ps, c);
			default -> {
			}
		}
	}

	/**
	 * Rage of the Bond: a towering black aura flares up round the caster, twice their height, with a rim of gold; Adult Gon's long black hair
	 * streams up out of it, and the ground round their feet glows and cracks. In first person only the ground and the sparks.
	 */
	private static void rage(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float open = ease(age, 0.0F, 10.0F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float k = open * fade;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		shade(ps, c, o -> {
			if (s.selfView) {
				return;
			}
			for (int i = 0; i < 14; i++) {
				float a = Mth.TWO_PI * i / 14 + hash(s.seed + i);
				float hgt = (2.6F + 1.6F * hash(i * 7 + (int)(time / 6))) * k;
				o.flame(polar(a, 0.55F, 0.0F), UP, hgt, 0.7F, time * 0.5F + i * 1.3F, argb(0.55F * k, DARK), argb(0.0F, DARK));
			}
			// the long hair streaming up and back off the head
			Vector3f head = new Vector3f(ahead).mul(-0.05F).add(0, 1.75F, 0);
			for (int i = 0; i < 9; i++) {
				float t = (i - 4) / 4.0F;
				float sway = 0.15F * Mth.sin(time * 0.15F + i);
				Vector3f root = new Vector3f(right).mul(t * 0.22F).add(head);
				Vector3f tip = new Vector3f(right).mul(t * 0.9F + sway).add(new Vector3f(ahead).mul(-0.5F - 0.4F * Math.abs(t))).add(head)
					.add(0, (1.6F + 1.2F * (1.0F - Math.abs(t))) * open, 0);
				o.blade(root, tip, right, 0.17F, argb(0.95F * k, HAIR), argb(0.9F * k, HAIR));
			}
		});
		glow(ps, c, o -> {
			float self = s.selfView ? 0.4F : 1.0F;
			if (!s.selfView) {
				for (int i = 0; i < 14; i++) {
					float a = Mth.TWO_PI * i / 14 + hash(s.seed * 3 + i);
					float hgt = (2.0F + 1.4F * hash(i * 5 + (int)(time / 5))) * k;
					o.flame(polar(a, 0.7F, 0.0F), UP, hgt, 0.4F, time * 0.6F + i, argb(0.4F * k, GOLD), argb(0.0F, GOLD_DEEP));
				}
				o.wall(new Vector3f(), 0.75F, 0.0F, 3.5F * open, argb(0.12F * k, GOLD), argb(0.0F, GOLD));
			}
			o.ring(new Vector3f(0, 0.15F, 0), 0.6F, 1.5F, argb(0.45F * k * self, GOLD));
			for (int i = 0; i < 8; i++) {
				float a = Mth.TWO_PI * i / 8 + hash(s.seed + i * 3);
				o.glowLine(polar(a, 0.6F, 0.14F), polar(a + 0.2F, 1.4F + 0.5F * hash(i), 0.14F), 0.06F, argb(0.7F * k * self, GOLD_PALE), argb(0.0F, GOLD));
			}
			for (int i = 0; i < 10; i++) {
				float q = (time * 0.06F + hash(s.seed + i)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 9), 0.5F + 0.4F * hash(i * 13), 0.2F + 3.4F * q);
				o.glint(at, s.camRight, s.camUp, 0.12F, time * 0.2F + i, argb(0.85F * k * self * (1.0F - q), GOLD_PALE));
			}
		});
	}

	/**
	 * First Comes Rock: in the air the caster's fist is wrapped in a blaze of yellow aura, a streak behind it; where they land it is slammed
	 * into the ground: a flash, a ring of shock running out to the edge of what the slam hits, the ground cracking to it.
	 */
	private static void firstComesRock(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float time = s.ageInTicks;
		float r = Math.max(1.0F, s.size);
		float land = AntLooks.landed(s);
		List<Vector3f> path = VfxOut.path(s.trail);
		Vector3f now = path.getLast();
		Vector3f ahead = PendragonLooks.heading(s, path, path.size() - 1);
		Vector3f right = VfxOut.across(ahead);
		if (land < 0.0F) {
			Vector3f fistAt = new Vector3f(now).add(new Vector3f(ahead).mul(0.6F)).add(new Vector3f(right).mul(0.3F)).add(0, 1.15F, 0);
			Vector3f punch = new Vector3f(ahead).add(0, -0.4F, 0).normalize();
			shade(ps, c, o -> {
				if (!s.selfView) {
					o.fist(fistAt, punch, UP, 0.5F, argb(0.97F, FIST_LIT), argb(0.97F, FIST_DARK), true);
				}
			});
			glow(ps, c, o -> {
				o.sprite(fistAt, s.camRight, s.camUp, 0.75F, argb(0.6F, GOLD));
				o.sprite(fistAt, s.camRight, s.camUp, 0.35F, argb(0.7F, GOLD_PALE));
				for (int i = 0; i < 8; i++) {
					float a = Mth.TWO_PI * i / 8 + time * 0.5F;
					Vector3f off = new Vector3f(right).mul(Mth.cos(a) * 0.25F).add(new Vector3f(UP).mul(Mth.sin(a) * 0.25F));
					o.flame(new Vector3f(fistAt).add(off), new Vector3f(punch).negate(), 0.6F + 0.4F * hash(i + (int)(time / 3)), 0.25F, time * 0.8F + i,
						argb(0.6F, GOLD), argb(0.0F, GOLD_DEEP));
				}
				for (int i = 0; i + 1 < path.size(); i++) {
					float t = (float)i / Math.max(1, path.size() - 1);
					o.glowLine(new Vector3f(path.get(i)).add(0, 1.15F, 0), new Vector3f(path.get(i + 1)).add(0, 1.15F, 0), 0.25F, argb(0.3F * t, GOLD), argb(0.3F * t, GOLD));
				}
			});
			return;
		}
		float q = land;
		Vector3f at = AntLooks.landing(s);
		Vector3f g = new Vector3f(at).add(0, 0.15F, 0);
		float shock = ease(q, 0.0F, 5.0F);
		float fade = 1.0F - ease(q, 14.0F, 26.0F);
		float fist = 1.0F - ease(q, 3.0F, 10.0F);
		shade(ps, c, o -> {
			for (int i = 0; i < 10; i++) {
				float a = Mth.TWO_PI * i / 10 + hash(s.seed + i);
				Vector3f mid = polar(a + 0.15F * (hash(i * 3) - 0.5F), r * 0.5F * shock, 0.0F).add(g).add(0, -0.02F, 0);
				o.bar(new Vector3f(g).add(0, -0.02F, 0), mid, 0.12F, 0.08F, argb(0.7F * fade, CRACK), argb(0.7F * fade, CRACK));
				o.bar(mid, polar(a, r * 0.95F * shock, 0.0F).add(g).add(0, -0.02F, 0), 0.08F, 0.02F, argb(0.7F * fade, CRACK), argb(0.2F * fade, CRACK));
			}
			if (fist > 0.0F && !s.selfView) {
				o.fist(new Vector3f(at).add(new Vector3f(ahead).mul(0.6F)).add(0, 0.55F, 0), DOWN, ahead, 0.9F * (0.8F + 0.2F * fist), argb(0.95F * fist, FIST_LIT),
					argb(0.95F * fist, FIST_DARK), true);
			}
		});
		glow(ps, c, o -> {
			o.ring(g, Math.max(0.0F, r - 0.3F), r, argb(0.85F * fade * Math.max(0.4F, shock), GOLD));
			o.ring(new Vector3f(g).add(0, 0.01F, 0), Math.max(0.0F, r - 0.07F), r, argb(0.6F * fade, GOLD_PALE));
			if (shock < 1.0F) {
				o.ring(new Vector3f(g).add(0, 0.02F, 0), Math.max(0.0F, r * shock - 0.8F), r * shock, argb(0.8F * (1.0F - shock), GOLD_PALE));
			}
			o.disc(g, r, argb(0.3F * fist, GOLD), argb(0.05F * fist, GOLD));
			for (int i = 0; i < 10; i++) {
				float a = Mth.TWO_PI * i / 10 + hash(s.seed + i);
				o.glowLine(g, polar(a, r * 0.9F * shock, 0.0F).add(g), 0.1F, argb(0.6F * fade, GOLD), argb(0.0F, GOLD));
			}
			float flash = 1.0F - ease(q, 0.0F, 7.0F);
			if (flash > 0.0F) {
				o.star(new Vector3f(at).add(0, 0.8F, 0), s.camRight, s.camUp, r * 0.6F * flash + 0.3F, 10, 0.25F, s.seed, argb(0.9F * flash, 0xFFFFFF), argb(0.0F, GOLD));
				o.glowLine(g, new Vector3f(g).add(0, 4.0F, 0), 1.0F * flash, argb(0.4F * flash, GOLD), argb(0.0F, GOLD));
			}
			for (int i = 0; i < 10; i++) {
				float u = Math.min(1.0F, q / 18.0F);
				Vector3f d = BladeLooks.shardDir(s.seed, i);
				Vector3f pt = new Vector3f(d.x, Math.abs(d.y) + 0.4F, d.z).mul(r * 0.6F * u).add(at).add(0, -2.5F * u * u + 0.3F, 0);
				o.glint(pt, s.camRight, s.camUp, 0.14F, time * 0.3F + i, argb(0.85F * fade, GOLD_PALE));
			}
		});
	}

	/**
	 * Long Scissors: two fingers held out in a V and from them a blade of yellow aura grown far along the beam, widest where it leaves the
	 * fingers (as wide as the beam hits, {@link PendragonLooks#BEAM_R}) and tapering to a point at its end, a bright edge and a sheen running
	 * up it; faint lines along the beam mark how far round it the cut reaches.
	 */
	private static void longScissors(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float len = Math.max(2.0F, s.size);
		Vector3f dir = PendragonLooks.beamDir(s);
		Vector3f side = VfxOut.across(dir);
		Vector3f up = PendragonLooks.beamUp(dir);
		float fade = 1.0F - ease(p, 0.55F, 1.0F);
		float grow = ease(age, 0.0F, 4.0F);
		float r = PendragonLooks.BEAM_R;
		Vector3f hand = new Vector3f(side).mul(0.3F).add(new Vector3f(up).mul(-0.3F)).add(new Vector3f(dir).mul(0.35F));
		Vector3f root = new Vector3f(dir).mul(0.75F);
		Vector3f front = new Vector3f(dir).mul(0.75F + (len - 0.75F) * grow);
		shade(ps, c, o -> {
			if (s.selfView) {
				return;
			}
			for (int k = -1; k <= 1; k += 2) {
				Vector3f knuckle = new Vector3f(hand).add(new Vector3f(side).mul(k * 0.04F));
				o.bar(knuckle, new Vector3f(knuckle).add(new Vector3f(dir).mul(0.28F)).add(new Vector3f(side).mul(k * 0.09F)), 0.035F, 0.03F, argb(0.95F * fade, FIST_LIT),
					argb(0.95F * fade, FIST_LIT));
			}
		});
		glow(ps, c, o -> {
			// the blade: flat in the side plane, its fin standing across it
			o.blade(root, front, side, r, argb(0.5F * fade, GOLD), argb(0.25F * fade, GOLD));
			o.blade(root, front, side, r * 0.55F, argb(0.5F * fade, GOLD_PALE), argb(0.3F * fade, GOLD_PALE));
			o.bar(root, front, 0.07F, 0.02F, argb(fade, 0xFFFFFF), argb(0.8F * fade, GOLD_PALE));
			Vector3f widest = new Vector3f(root).lerp(front, 0.2F);
			for (int k = -1; k <= 1; k += 2) {
				Vector3f edge = new Vector3f(widest).add(new Vector3f(side).mul(k * r));
				o.glowLine(root, edge, 0.05F, argb(0.8F * fade, GOLD_PALE), argb(0.9F * fade, GOLD_PALE));
				o.glowLine(edge, front, 0.05F, argb(0.9F * fade, GOLD_PALE), argb(0.6F * fade, 0xFFFFFF));
			}
			float sheen = (time * 0.12F) % 1.0F;
			o.sprite(new Vector3f(root).lerp(front, sheen), s.camRight, s.camUp, 0.5F, argb(0.6F * fade, 0xFFFFFF));
			o.sprite(hand, s.camRight, s.camUp, 0.45F, argb(0.6F * fade, GOLD));
			// how far round the line it cuts
			if (front.length() > 1.0F) {
				for (int k = 0; k < 4; k++) {
					Vector3f off = new Vector3f(k < 2 ? up : side).mul((k % 2 == 0 ? 1 : -1) * r);
					o.glowLine(new Vector3f(widest).add(off), new Vector3f(front).add(off), 0.05F, argb(0.22F * fade, GOLD), argb(0.22F * fade, GOLD));
				}
			}
			float flash = 1.0F - ease(age, 2.0F, 9.0F);
			if (flash > 0.0F) {
				o.star(front, s.camRight, s.camUp, 0.9F * flash + 0.2F, 4, 0.15F, 0.3F, argb(0.9F * flash, 0xFFFFFF), argb(0.0F, GOLD));
			}
		});
	}

	/**
	 * This Is All I Need: aura streams into a giant fist over the caster's head, growing and burning gold, while a faint ring on the ground
	 * shows where the blast will reach; then it is brought down in front of them.
	 */
	private static void giantFist(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float time = s.ageInTicks;
		float r = Math.max(1.0F, s.size);
		float charge = ease(age, 0.0F, 16.0F);
		float slam = ease(age, 17.0F, 21.0F);
		float fade = 1.0F - ease(age, 21.0F, 24.0F);
		Vector3f ahead = WitchLooks.ahead(s);
		float size = 0.6F + 2.0F * charge;
		Vector3f high = new Vector3f(ahead).mul(0.6F).add(0, 3.0F + 0.9F * charge, 0);
		Vector3f low = new Vector3f(ahead).mul(1.8F).add(0, size * 0.45F, 0);
		Vector3f at = new Vector3f(high).lerp(low, slam);
		Vector3f punch = new Vector3f(ahead).add(0, 0.6F, 0).normalize().lerp(DOWN, slam).normalize();
		Vector3f back = new Vector3f(UP).lerp(ahead, slam).normalize();
		float self = s.selfView ? 0.45F : 1.0F;
		shade(ps, c, o -> o.fist(at, punch, back, size, argb(0.95F * fade * self, FIST_LIT), argb(0.95F * fade * self, FIST_DARK), true));
		glow(ps, c, o -> {
			o.sprite(at, s.camRight, s.camUp, size * 1.15F, argb(0.45F * fade * self, GOLD));
			for (int i = 0; i < 12; i++) {
				float a = Mth.TWO_PI * i / 12 + time * 0.3F;
				o.flame(new Vector3f(at).add(polar(a, size * 0.4F, -size * 0.2F)), UP, size * (0.5F + 0.4F * hash(i + (int)(time / 3))), size * 0.3F, time * 0.7F + i,
					argb(0.55F * fade * self, GOLD), argb(0.0F, GOLD_DEEP));
			}
			// aura streaming into the fist
			for (int i = 0; i < 14; i++) {
				float q = (time * 0.07F + hash(s.seed + i)) % 1.0F;
				Vector3f from = polar(Mth.TWO_PI * hash(i * 7), 3.5F, 0.3F + 2.0F * hash(i * 11));
				o.glint(new Vector3f(from).lerp(at, q), s.camRight, s.camUp, 0.15F, time * 0.3F + i, argb(0.85F * (1.0F - slam) * q, GOLD_PALE));
			}
			float pulse = 0.5F + 0.5F * Mth.sin(time * 0.8F);
			o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, r * charge - 0.25F), r * charge, argb((0.3F + 0.25F * pulse) * fade, GOLD));
		});
	}

	/**
	 * The giant fist's blast: a flash and a dome of gold light swelling out to the edge of what it hits, a ring of shock running over the
	 * ground to it, a pillar of aura standing up from the middle, the ground cracked out to the rim and debris thrown up.
	 */
	private static void rockBlast(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float r = Math.max(1.0F, s.size);
		float q = ease(age, 0.0F, 6.0F);
		float fade = 1.0F - ease(p, 0.5F, 1.0F);
		float pillar = 1.0F - ease(age, 4.0F, 18.0F);
		Vector3f g = new Vector3f(0, 0.15F, 0);
		shade(ps, c, o -> {
			for (int i = 0; i < 14; i++) {
				float a = Mth.TWO_PI * i / 14 + hash(s.seed + i);
				Vector3f mid = polar(a + 0.2F * (hash(i * 3) - 0.5F), r * 0.5F * q, 0.13F);
				o.bar(new Vector3f(0, 0.13F, 0), mid, 0.2F, 0.12F, argb(0.7F * fade, CRACK), argb(0.7F * fade, CRACK));
				o.bar(mid, polar(a, r * 0.97F * q, 0.13F), 0.12F, 0.02F, argb(0.7F * fade, CRACK), argb(0.2F * fade, CRACK));
			}
			for (int i = 0; i < 16; i++) {
				float u = Math.min(1.0F, age / 22.0F);
				Vector3f d = BladeLooks.shardDir(s.seed * 3, i);
				Vector3f pt = new Vector3f(d.x, Math.abs(d.y) + 0.6F, d.z).mul(r * 0.8F * u).add(0, -5.0F * u * u + 0.5F, 0);
				if (pt.y > 0.1F) {
					o.square(pt, s.camRight, s.camUp, 0.18F + 0.1F * hash(i), time * 0.2F + i, argb(0.9F * fade, 0x4A3A28));
				}
			}
		});
		glow(ps, c, o -> {
			// the dome swelling out to the rim
			for (int i = 0; i < 6; i++) {
				float lat = Mth.HALF_PI * i / 6.0F;
				float rr = r * q * Mth.cos(lat);
				float y = r * q * Mth.sin(lat) * 0.75F;
				o.circle(new Vector3f(0, 0.15F + y, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), rr, 0.06F, 40, argb(0.5F * fade * (1.0F - 0.12F * i), GOLD_PALE));
			}
			o.wall(new Vector3f(0, 0.15F, 0), r * q, 0.0F, 1.6F, argb(0.35F * fade, GOLD), argb(0.0F, GOLD));
			o.ring(g, Math.max(0.0F, r - 0.35F), r, argb(0.9F * fade, GOLD));
			o.ring(new Vector3f(g).add(0, 0.01F, 0), Math.max(0.0F, r - 0.08F), r, argb(0.7F * fade, 0xFFFFFF));
			o.disc(new Vector3f(0, 0.14F, 0), r * q, argb(0.35F * (1.0F - q * 0.6F) * fade, GOLD_PALE), argb(0.1F * fade, GOLD));
			if (pillar > 0.0F) {
				o.glowLine(g, new Vector3f(0, 10.0F, 0), 1.6F * pillar, argb(0.5F * pillar, GOLD), argb(0.0F, GOLD));
				o.glowLine(g, new Vector3f(0, 7.0F, 0), 0.5F * pillar, argb(0.8F * pillar, GOLD_PALE), argb(0.0F, GOLD_PALE));
			}
			float flash = 1.0F - ease(age, 0.0F, 8.0F);
			if (flash > 0.0F) {
				o.star(new Vector3f(0, 1.2F, 0), s.camRight, s.camUp, r * 0.8F * flash + 0.3F, 12, 0.3F, s.seed, argb(0.95F * flash, 0xFFFFFF), argb(0.0F, GOLD));
			}
			for (int i = 0; i < 14; i++) {
				float a = Mth.TWO_PI * i / 14 + hash(s.seed + i);
				o.glowLine(polar(a, 0.3F, 0.14F), polar(a, r * 0.9F * q, 0.14F), 0.1F, argb(0.55F * fade, GOLD), argb(0.0F, GOLD));
			}
		});
	}

	private GonLooks() {
	}
}
