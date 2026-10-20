package com.minecraftmode.client.vfx;

import static com.minecraftmode.client.vfx.SkillVfxRenderer.glow;
import static com.minecraftmode.client.vfx.SkillVfxRenderer.shade;
import static com.minecraftmode.client.vfx.VfxOut.argb;
import static com.minecraftmode.client.vfx.VfxOut.ease;
import static com.minecraftmode.client.vfx.VfxOut.hash;
import static com.minecraftmode.client.vfx.VfxOut.polar;

import com.minecraftmode.client.vfx.SkillVfxRenderer.State;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Looks of fairies, flowers and elves: Mavis (Fairy Law, Fairy Glitter's stars, Fairy Sphere, her wings), Merlin (the Garden of Avalon,
 * Hero Creation, his illusions) and Frieren (Zoltraak's circles, Judradjim, defensive magic and the field of flowers).
 */
final class FairyLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final Vector3f X = new Vector3f(1, 0, 0);
	private static final Vector3f Z = new Vector3f(0, 0, 1);
	private static final int FAIRY = 0xFFF0B8;
	private static final int FAIRY_GOLD = 0xFFD86A;
	private static final int FAIRY_SKY = 0xBFF6FF;
	private static final int PINK = 0xFFB0DA;
	private static final int LILAC = 0xD8B8FF;
	private static final int BLOSSOM = 0xFFF4FA;
	private static final int HEART = 0xFFE070;
	private static final int STEM = 0x6AC86A;
	private static final int ELF = 0xE8F0FF;
	private static final int ELF_BLUE = 0x9FC8FF;
	private static final int MOONWEED = 0x6FA8FF;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case FAIRY_LAW -> fairyLaw(s, ps, c);
			case GLITTER_BURST -> glitterBurst(s, ps, c);
			case FAIRY_SPHERE -> fairySphere(s, ps, c);
			case FAIRY_WINGS -> fairyWings(s, ps, c);
			case AVALON -> avalon(s, ps, c);
			case HERO_CREATION -> heroCreation(s, ps, c);
			case ILLUSION -> illusion(s, ps, c);
			case ZOLTRAAK_CIRCLE -> zoltraakCircle(s, ps, c);
			case JUDRADJIM -> judradjim(s, ps, c);
			case HEX_BARRIER -> hexBarrier(s, ps, c);
			case FLOWER_FIELD -> flowerField(s, ps, c);
			default -> {
			}
		}
	}

	// ------------------------------------------------------------ Mavis

	/**
	 * A fairy's sigil {@code r} across in the plane of {@code a} and {@code b}: a double ring round a fairy, two pairs of wings and a tail
	 * curling under her.
	 */
	static void fairySigil(final VfxOut o, final Vector3f c, final Vector3f a, final Vector3f b, final float r, final float spin, final int color) {
		float w = Math.max(0.02F, r * 0.025F);
		o.circle(c, a, b, r, w * 1.3F, 40, color);
		o.circle(c, a, b, r * 0.9F, w * 0.8F, 40, color);
		for (int i = 0; i < 16; i++) {
			float t = spin + Mth.TWO_PI * i / 16;
			Vector3f d = new Vector3f(a).mul(Mth.cos(t)).add(new Vector3f(b).mul(Mth.sin(t)));
			o.sprite(new Vector3f(d).mul(r * 0.95F).add(c), a, b, w * 1.8F, color);
		}
		Vector3f body = new Vector3f(b).mul(r * 0.08F).add(c);
		for (int side = -1; side <= 1; side += 2) {
			wing(o, body, c, a, b, new float[] {side * 0.62F * r, 0.5F * r}, 0.2F * r, w, color);
			wing(o, body, c, a, b, new float[] {side * 0.48F * r, -0.32F * r}, 0.13F * r, w, color);
		}
		Vector3f head = new Vector3f(b).mul(r * 0.36F).add(c);
		o.sprite(head, a, b, r * 0.08F, color);
		o.bar(new Vector3f(b).mul(r * 0.28F).add(c), new Vector3f(b).mul(-r * 0.35F).add(c), w * 1.4F, w * 0.6F, color, color);
		// the tail curling under her
		Vector3f prev = new Vector3f(b).mul(-r * 0.35F).add(c);
		for (int i = 1; i <= 10; i++) {
			float t = i / 10.0F;
			float ang = -Mth.HALF_PI + t * Mth.PI * 1.4F;
			float rr = r * 0.22F * (1.0F - t * 0.6F);
			Vector3f q = new Vector3f(a).mul(r * 0.22F + Mth.cos(ang) * rr).add(new Vector3f(b).mul(-r * 0.57F - Mth.sin(ang) * rr * -1.0F)).add(c);
			o.bar(prev, q, w, w * 0.8F, color, color);
			prev = q;
		}
	}

	/** One wing of the sigil: a teardrop from {@code root} to the point {@code tip} (a, b offsets from the centre), {@code width} at its widest. */
	private static void wing(final VfxOut o, final Vector3f root, final Vector3f c, final Vector3f a, final Vector3f b, final float[] tip, final float width,
		final float w, final int color) {
		Vector3f end = new Vector3f(a).mul(tip[0]).add(new Vector3f(b).mul(tip[1])).add(c);
		Vector3f d = new Vector3f(end).sub(root);
		Vector3f n = new Vector3f(a).mul(-d.dot(b)).add(new Vector3f(b).mul(d.dot(a))).normalize();
		for (int side = -1; side <= 1; side += 2) {
			Vector3f prev = new Vector3f(root);
			for (int i = 1; i <= 8; i++) {
				float t = i / 8.0F;
				Vector3f q = new Vector3f(root).lerp(end, t).add(new Vector3f(n).mul(side * width * Mth.sin(Mth.PI * t) * (1.0F - 0.3F * t)));
				o.bar(prev, q, w, w, color, color);
				prev = q;
			}
		}
	}

	/** A hemisphere of light of {@code r}: latitude bands from the ground to the top, brighter low. */
	private static void dome(final VfxOut o, final float r, final int rgb, final float alpha) {
		int lats = 6;
		int lons = 28;
		for (int i = 0; i < lats; i++) {
			float l0 = Mth.HALF_PI * i / lats;
			float l1 = Mth.HALF_PI * (i + 1) / lats;
			int c0 = argb(alpha * (1.0F - 0.6F * i / lats), rgb);
			int c1 = argb(alpha * (1.0F - 0.6F * (i + 1) / lats), rgb);
			for (int j = 0; j < lons; j++) {
				float t0 = Mth.TWO_PI * j / lons;
				float t1 = Mth.TWO_PI * (j + 1) / lons;
				o.quad(domePoint(r, l0, t0), domePoint(r, l0, t1), domePoint(r, l1, t1), domePoint(r, l1, t0), c0, c0, c1, c1);
			}
		}
	}

	private static Vector3f domePoint(final float r, final float lat, final float lon) {
		return new Vector3f(Mth.cos(lat) * Mth.cos(lon) * r, Mth.sin(lat) * r, Mth.cos(lat) * Mth.sin(lon) * r);
	}

	/**
	 * Fairy Law: the fairy's sigil opens over the caster while light streams in to it from all round; a second later it is released, a
	 * blinding dome of golden-white light swelling out to the whole reach, a pillar of light standing in the middle.
	 */
	private static void fairyLaw(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float time = s.ageInTicks;
		float release = 20.0F;
		float gather = ease(age, 0.0F, 8.0F) * (1.0F - ease(age, release, release + 4.0F));
		float swell = ease(age, release, release + 9.0F);
		float shine = age < release ? 0.0F : 1.0F - ease(age, release + 2.0F, s.life);
		float selfK = s.selfView ? 0.45F : 1.0F;
		Vector3f sigil = new Vector3f(0, 3.8F, 0);
		glow(ps, c, o -> {
			if (gather > 0.0F) {
				if (!s.selfView) {
					fairySigil(o, sigil, s.camRight, s.camUp, 1.7F * ease(age, 0.0F, 8.0F), time * 0.03F, argb(0.95F * gather, FAIRY));
					o.sprite(sigil, s.camRight, s.camUp, 2.4F, argb(0.3F * gather, FAIRY_GOLD));
				}
				o.glowLine(new Vector3f(0, 0.2F, 0), sigil, 0.6F, argb(0.4F * gather, FAIRY), argb(0.25F * gather, FAIRY_GOLD));
				// light streaming in from all round
				for (int i = 0; i < 18; i++) {
					float a = Mth.TWO_PI * hash(s.seed + i * 3);
					Vector3f far = polar(a, 7.0F + 4.0F * hash(i * 5), 1.0F + 5.0F * hash(i * 7));
					float u = (time * 0.05F + hash(i * 11)) % 1.0F;
					Vector3f at = new Vector3f(far).lerp(sigil, u * u);
					Vector3f behind = new Vector3f(far).lerp(sigil, Math.max(0.0F, u - 0.2F) * Math.max(0.0F, u - 0.2F));
					o.glowLine(behind, at, 0.08F, argb(0.0F, FAIRY), argb(0.8F * gather, FAIRY));
					o.glint(at, s.camRight, s.camUp, 0.18F, time * 0.2F + i, argb(0.9F * gather, 0xFFFFFF));
				}
				o.ring(new Vector3f(0, 0.14F, 0), 0.6F, 1.2F + 0.4F * Mth.sin(time * 0.3F), argb(0.6F * gather, FAIRY_GOLD));
			}
			if (age < release) {
				return;
			}
			float r = s.size * swell + 0.5F;
			dome(o, r, FAIRY, 0.35F * shine * selfK);
			dome(o, r * 0.97F, 0xFFFFFF, 0.18F * shine * selfK);
			for (int i = 0; i < 16; i++) {
				float a = Mth.TWO_PI * i / 16 + time * 0.01F;
				o.glowLine(new Vector3f(0, 0.17F, 0), polar(a, r, 0.17F), 0.35F, argb(0.7F * shine, 0xFFFFFF), argb(0.2F * shine, FAIRY));
			}
			o.ring(new Vector3f(0, 0.16F, 0), Math.max(0.0F, r - 1.2F), r, argb(0.95F * shine, FAIRY_GOLD));
			o.disc(new Vector3f(0, 0.15F, 0), r, argb(0.3F * shine, 0xFFFFFF), argb(0.05F * shine, FAIRY));
			float flash = 1.0F - ease(age, release, release + 6.0F);
			o.sprite(new Vector3f(0, 1.5F, 0), s.camRight, s.camUp, (s.selfView ? 3.0F : 6.0F) * flash + 0.5F, argb(0.9F * flash * selfK, 0xFFFFFF));
			o.star(new Vector3f(0, 2.0F, 0), s.camRight, s.camUp, 8.0F * flash + 2.0F, 8, 0.15F, time * 0.02F, argb(0.8F * shine * selfK, 0xFFFFFF), argb(0.0F, FAIRY));
			o.glowLine(new Vector3f(0, 0.2F, 0), new Vector3f(0, 28.0F, 0), 2.5F * shine, argb(0.45F * shine, FAIRY), argb(0.0F, FAIRY));
			if (!s.selfView) {
				fairySigil(o, new Vector3f(0, 0.17F, 0), X, Z, Math.min(r * 0.5F, 5.0F), -time * 0.02F, argb(0.8F * shine, FAIRY_GOLD));
			}
		});
	}

	/** Fairy Glitter landing: a white star flashes and stars of every colour fly out of it, a golden ring racing over the ground. */
	private static void glitterBurst(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float out = ease(p, 0.0F, 0.6F);
		float fade = 1.0F - ease(p, 0.4F, 1.0F);
		float time = s.ageInTicks;
		glow(ps, c, o -> {
			float flash = 1.0F - ease(p, 0.0F, 0.35F);
			o.sprite(new Vector3f(), s.camRight, s.camUp, 2.5F * flash + 0.3F, argb(0.9F * flash, 0xFFFFFF));
			o.star(new Vector3f(), s.camRight, s.camUp, s.size * 1.3F * out + 0.5F, 8, 0.15F, time * 0.05F, argb(0.9F * fade, 0xFFFFFF), argb(0.0F, FAIRY));
			for (int i = 0; i < 21; i++) {
				Vector3f d = BladeLooks.shardDir(s.seed, i);
				Vector3f at = new Vector3f(d).mul(s.size * out * (0.6F + 0.4F * hash(i))).add(0, 0.5F, 0);
				int color = WitchLooks.RAINBOW[i % WitchLooks.RAINBOW.length];
				o.star(at, s.camRight, s.camUp, 0.35F, 5, 0.45F, time * 0.2F + i, argb(0.95F * fade, VfxDraw.whiten(color, 0.4F)), argb(0.3F * fade, color));
				o.sprite(at, s.camRight, s.camUp, 0.3F, argb(0.4F * fade, color));
			}
			o.ring(new Vector3f(0, 0.05F, 0), Math.max(0.0F, s.size * out - 0.5F), s.size * out, argb(0.8F * fade, FAIRY_GOLD));
		});
	}

	/** Fairy Sphere: a translucent golden sphere round the caster, its meridians turning, a band of runes round its middle and glints on it. */
	private static void fairySphere(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.12F);
		float fade = 1.0F - ease(p, 0.85F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		float r = s.size * (s.selfView ? 1.3F : 1.0F) * open;
		Vector3f mid = new Vector3f(0, 1.0F, 0);
		float alpha = s.selfView ? 0.4F : 1.0F;
		ps.pushPose();
		ps.translate(mid.x, mid.y, mid.z);
		c.submitCustomGeometry(ps, VfxDraw.GLOW, (pose, buffer) -> VfxDraw.ball(pose, buffer, Math.max(0.05F, r), FAIRY_GOLD, 0.1F * k * alpha));
		ps.popPose();
		glow(ps, c, o -> {
			for (int i = 0; i < 6; i++) {
				float t = time * 0.02F + Mth.PI * i / 6;
				Vector3f a = new Vector3f(Mth.cos(t), 0, Mth.sin(t));
				o.circle(mid, a, UP, r, 0.018F, 32, argb(0.6F * k * alpha, FAIRY));
			}
			for (int j = -2; j <= 2; j++) {
				float lat = j * 0.5F;
				o.circle(new Vector3f(mid).add(0, Mth.sin(lat) * r, 0), X, Z, Mth.cos(lat) * r, 0.018F, 32, argb(0.6F * k * alpha, FAIRY));
			}
			for (int i = 0; i < 16; i++) {
				float a = -time * 0.03F + Mth.TWO_PI * i / 16;
				Vector3f out = polar(a, 1.0F, 0.0F);
				o.rune(new Vector3f(out).mul(r * 1.01F).add(mid), polar(a + Mth.HALF_PI, 1.0F, 0.0F), UP, 0.12F, s.seed + i, 0.018F, argb(0.9F * k * alpha, FAIRY_GOLD));
			}
			for (int i = 0; i < 10; i++) {
				float tw = 0.5F + 0.5F * Mth.sin(time * 0.3F + i * 2.3F);
				Vector3f d = BladeLooks.shardDir(s.seed + 5, i);
				o.glint(new Vector3f(d).mul(r).add(mid), s.camRight, s.camUp, 0.2F * tw, i, argb(0.9F * k * alpha, 0xFFFFFF));
			}
			o.ring(new Vector3f(0, 0.14F, 0), r * 0.85F, r, argb(0.5F * k, FAIRY_GOLD));
		});
	}

	/**
	 * Fairy Tactician: Mavis's little wings at her head and great wings of light on her back beating slowly, motes rising round her.
	 */
	private static void fairyWings(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.15F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		float yaw = -s.bodyYaw * Mth.DEG_TO_RAD;
		ps.pushPose();
		ps.rotate(Axis.YP.rotation(yaw));
		Vector3f camRight = new Vector3f(s.camRight).rotateY(-yaw);
		Vector3f camUp = new Vector3f(s.camUp).rotateY(-yaw);
		float beat = 0.25F * Mth.sin(time * 0.35F);
		glow(ps, c, o -> {
			if (!s.selfView) {
				Vector3f root = new Vector3f(0, 1.35F, -0.25F);
				for (int side = -1; side <= 1; side += 2) {
					float sway = side * beat;
					Vector3f upper = new Vector3f(side * Mth.cos(0.6F + sway), 0.85F, -0.35F - 0.3F * Math.abs(sway)).normalize();
					Vector3f lower = new Vector3f(side * Mth.cos(0.3F + sway), -0.55F, -0.35F).normalize();
					lobe(o, root, upper, 1.5F * open, 0.55F, argb(0.35F * k, FAIRY_SKY), argb(0.9F * k, 0xFFFFFF));
					lobe(o, root, lower, 1.0F * open, 0.4F, argb(0.35F * k, FAIRY), argb(0.9F * k, FAIRY_GOLD));
					// the little wings at her head
					Vector3f ear = new Vector3f(side * 0.27F, 1.78F, 0.02F);
					lobe(o, ear, new Vector3f(side * 0.6F, 0.8F, -0.2F).normalize(), 0.32F, 0.12F, argb(0.5F * k, 0xFFFFFF), argb(0.95F * k, 0xFFFFFF));
				}
				o.sprite(root, camRight, camUp, 0.7F, argb(0.3F * k, FAIRY));
			}
			for (int i = 0; i < 16; i++) {
				float rise = (time * 0.03F + hash(i * 5 + s.seed)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 3) + rise * 2.0F, 0.6F + 0.8F * hash(i), rise * 2.8F);
				if (s.selfView && at.y > 1.0F) {
					continue;
				}
				o.glint(at, camRight, camUp, 0.15F, time * 0.1F + i, argb(0.9F * k * Mth.sin(Mth.PI * rise), i % 2 == 0 ? FAIRY_SKY : FAIRY));
			}
			o.ring(new Vector3f(0, 0.14F, 0), 0.6F, 1.1F, argb(0.5F * k, FAIRY_SKY));
		});
		ps.popPose();
	}

	/** A wing lobe: a teardrop from {@code root} along {@code dir}, {@code width} across at its widest, filled faintly, its edge lit, a vein down it. */
	private static void lobe(final VfxOut o, final Vector3f root, final Vector3f dir, final float length, final float width, final int fill, final int edge) {
		if (length <= 0.01F) {
			return;
		}
		Vector3f d = new Vector3f(dir).normalize();
		Vector3f n = new Vector3f(d).cross(0, 0, 1);
		if (n.lengthSquared() < 1.0E-4F) {
			n.set(1, 0, 0);
		}
		n.normalize();
		int steps = 10;
		Vector3f prevA = new Vector3f(root);
		Vector3f prevB = new Vector3f(root);
		for (int i = 1; i <= steps; i++) {
			float t = (float)i / steps;
			float wdt = width * Mth.sin(Mth.PI * t) * (1.0F - 0.35F * t) * (0.4F + 0.6F * t);
			Vector3f mid = new Vector3f(d).mul(length * t).add(root);
			Vector3f a = new Vector3f(n).mul(wdt).add(mid);
			Vector3f b = new Vector3f(n).mul(-wdt).add(mid);
			o.quad(prevA, a, b, prevB, fill);
			o.bar(prevA, a, 0.015F, 0.015F, edge, edge);
			o.bar(prevB, b, 0.015F, 0.015F, edge, edge);
			prevA = a;
			prevB = b;
		}
		o.bar(root, new Vector3f(d).mul(length * 0.85F).add(root), 0.01F, 0.005F, edge, edge);
	}

	// ------------------------------------------------------------ Merlin

	/**
	 * A field of flowers round the middle: {@code count} flowers within {@code radius}, each blooming after a time that grows with its
	 * distance ({@code spread} ticks to the rim); petals {@code petal} to {@code tip}, stems green. The shade pass draws them solid enough
	 * to see by day.
	 */
	private static void field(final VfxOut o, final State s, final int count, final float radius, final float spread, final int[] petals, final int tip,
		final float alpha, final boolean lit) {
		float age = s.age();
		for (int i = 0; i < count; i++) {
			float d = radius * (float)Math.sqrt(hash(s.seed * 7 + i * 3));
			float a = Mth.TWO_PI * hash(s.seed * 5 + i * 11);
			float open = ease(age - d / Math.max(1.0F, radius) * spread - hash(i * 13) * 6.0F, 0.0F, 14.0F);
			if (open <= 0.0F) {
				continue;
			}
			float stem = 0.12F + 0.25F * hash(i * 17);
			Vector3f foot = polar(a, d, 0.12F);
			Vector3f head = new Vector3f(foot).add(0, stem * open, 0);
			float size = (0.3F + 0.2F * hash(i * 19)) * (0.4F + 0.6F * open);
			int petal = petals[i % petals.length];
			if (lit) {
				o.bloom(head, 5, size, open, hash(i * 23) * Mth.TWO_PI, argb(0.3F * alpha, petal), argb(0.5F * alpha, VfxDraw.whiten(petal, 0.3F)), argb(0.9F * alpha, HEART));
				o.bar(foot, head, 0.012F, 0.012F, argb(0.3F * alpha, STEM), argb(0.3F * alpha, STEM));
			} else {
				o.bloom(head, 5, size, open, hash(i * 23) * Mth.TWO_PI, argb(0.8F * alpha, VfxDraw.whiten(petal, 0.1F)), argb(0.8F * alpha, petal), argb(0.9F * alpha,
					0xE8C040));
				o.bar(foot, head, 0.02F, 0.02F, argb(0.9F * alpha, 0x3A8A3A), argb(0.9F * alpha, 0x4AA04A));
			}
		}
	}

	/**
	 * Garden of Avalon: flowers bloom out over the ground from the caster to the rim, petals drift up and round on the air, and beyond the
	 * garden the tower of Avalon stands in pale light.
	 */
	private static void avalon(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.08F);
		float fade = 1.0F - ease(p, 0.9F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		int[] petals = {PINK, BLOSSOM, LILAC};
		Vector3f tower = new Vector3f(s.forward).mul(s.size + 4.0F);
		shade(ps, c, o -> field(o, s, 90, s.size, 30.0F, petals, BLOSSOM, fade, false));
		glow(ps, c, o -> {
			field(o, s, 90, s.size, 30.0F, petals, BLOSSOM, fade, true);
			Vector3f ground = new Vector3f(0, 0.14F, 0);
			o.disc(ground, s.size * open, argb(0.18F * k, PINK), argb(0.0F, PINK));
			o.ring(ground, s.size * open - 0.2F, s.size * open + 0.2F, argb(0.7F * k, LILAC));
			// petals on the air
			for (int i = 0; i < 40; i++) {
				float rise = (time * 0.012F + hash(i * 3 + s.seed)) % 1.0F;
				float a = Mth.TWO_PI * hash(i * 7) + time * 0.02F + rise * 2.0F;
				Vector3f at = polar(a, s.size * (0.2F + 0.8F * hash(i * 5)), 0.3F + rise * 4.5F);
				Vector3f dir = new Vector3f(Mth.cos(time * 0.1F + i), 0.5F, Mth.sin(time * 0.13F + i)).normalize();
				o.diamond(at, dir, VfxOut.across(dir), 0.22F, 0.13F, argb(0.3F * k, petals[i % 3]), argb(0.9F * k * Mth.sin(Mth.PI * rise), petals[i % 3]));
			}
			// the tower beyond the garden, in pale light
			float th = 11.0F;
			float tr = 2.0F;
			int line = argb(0.4F * k, LILAC);
			for (int i = 0; i < 8; i++) {
				float a = Mth.TWO_PI * i / 8;
				o.bar(polar(a, tr, 0.0F).add(tower), polar(a, tr * 0.9F, th).add(tower), 0.04F, 0.04F, line, line);
				o.bar(polar(a, tr * 1.3F, th).add(tower), new Vector3f(tower).add(0, th + 4.5F, 0), 0.04F, 0.02F, line, argb(0.6F * k, BLOSSOM));
				o.bar(polar(a, tr * 1.3F, th).add(tower), polar(a + Mth.TWO_PI / 8, tr * 1.3F, th).add(tower), 0.04F, 0.04F, line, line);
			}
			for (int j = 0; j < 5; j++) {
				float y = th * j / 4;
				o.circle(new Vector3f(tower).add(0, y, 0), X, Z, Mth.lerp(j / 4.0F, tr, tr * 0.9F), 0.04F, 16, line);
			}
			for (int j = 0; j < 4; j++) {
				// its lit windows on the near side
				Vector3f window = new Vector3f(tower).add(0, 2.5F + j * 2.4F, 0).sub(new Vector3f(s.forward).mul(tr));
				o.glint(window, s.camRight, s.camUp, 0.3F, time * 0.05F + j, argb(0.8F * k, HEART));
			}
			o.sprite(new Vector3f(tower).add(0, th + 4.8F, 0), s.camRight, s.camUp, 1.0F, argb(0.6F * k, BLOSSOM));
		});
	}

	/** Hero Creation: rings of rose-gold light rising round the caster, stars sparkling up, a crown of light over their head, a ring washing out to allies. */
	private static void heroCreation(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.15F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		float top = s.selfView ? 0.9F : 2.3F;
		glow(ps, c, o -> {
			for (int i = 0; i < 3; i++) {
				float q = (time * 0.035F + i / 3.0F) % 1.0F;
				float r = 0.9F - 0.25F * q;
				Vector3f at = new Vector3f(0, 0.1F + q * top, 0);
				o.ring(at, r - 0.08F, r + 0.08F, argb(0.95F * k * Mth.sin(Mth.PI * q), PINK));
				for (int j = 0; j < 6; j++) {
					float a = time * 0.1F + Mth.TWO_PI * j / 6 + i;
					o.glint(polar(a, r, at.y), s.camRight, s.camUp, 0.14F, a, argb(0.9F * k * Mth.sin(Mth.PI * q), 0xFFFFFF));
				}
			}
			for (int i = 0; i < 12; i++) {
				float rise = (time * 0.03F + hash(i * 7 + s.seed)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 3) + rise, 0.6F + 0.7F * hash(i), rise * (top + 0.8F));
				o.star(at, s.camRight, s.camUp, 0.22F, 5, 0.45F, time * 0.1F + i, argb(k * Mth.sin(Mth.PI * rise), 0xFFFFFF), argb(0.7F * k * Mth.sin(Mth.PI * rise), PINK));
			}
			if (!s.selfView) {
				// a crown of light over the head
				Vector3f crown = new Vector3f(0, 2.25F + 0.05F * Mth.sin(time * 0.2F), 0);
				o.circle(crown, X, Z, 0.3F, 0.03F, 20, argb(0.95F * k, FAIRY_GOLD));
				for (int i = 0; i < 5; i++) {
					float a = time * 0.03F + Mth.TWO_PI * i / 5;
					Vector3f foot = polar(a, 0.3F, 0.0F).add(crown);
					Vector3f tip = polar(a, 0.33F, 0.28F).add(crown);
					o.bar(foot, tip, 0.03F, 0.01F, argb(0.95F * k, FAIRY_GOLD), argb(0.95F * k, 0xFFFFFF));
					o.sprite(tip, s.camRight, s.camUp, 0.06F, argb(k, i % 2 == 0 ? PINK : 0x8FD4FF));
				}
				o.sprite(crown, s.camRight, s.camUp, 0.7F, argb(0.25F * k, FAIRY_GOLD));
			}
			float wash = ease(p, 0.05F, 0.6F);
			o.ring(new Vector3f(0, 0.14F, 0), Math.max(0.0F, s.size * wash - 0.5F), s.size * wash, argb(0.7F * fade * (1.0F - 0.5F * wash), PINK));
		});
	}

	/**
	 * Illusion: where the caster stood, an afterimage of them shimmering in pastel colours and coming apart, petals streaming from it to
	 * where they are now.
	 */
	private static void illusion(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		List<Vector3f> points = VfxOut.path(s.trail);
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.25F, 1.0F);
		float apart = ease(p, 0.1F, 1.0F);
		float time = s.ageInTicks;
		Vector3f right = new Vector3f(s.camRight.x(), 0, s.camRight.z());
		if (right.lengthSquared() < 1.0E-4F) {
			right.set(1, 0, 0);
		}
		right.normalize();
		int[] hues = {PINK, LILAC, 0x9FE8FF, BLOSSOM};
		shade(ps, c, o -> figure(o, right, apart, time, argb(0.35F * fade, 0xF8F0FF), s.seed));
		glow(ps, c, o -> {
			int hue = hues[(int)(time * 0.25F) % hues.length];
			figure(o, right, apart, time, argb(0.7F * fade, hue), s.seed);
			o.ring(new Vector3f(0, 0.14F, 0), 0.4F, 0.9F + apart, argb(0.6F * fade, LILAC));
			if (points.size() < 2) {
				return;
			}
			int last = s.selfView ? Math.max(1, points.size() - 2) : points.size() - 1;
			Vector3f end = new Vector3f(points.get(last)).add(0, 1.0F, 0);
			for (int i = 0; i < 22; i++) {
				float t = (hash(s.seed + i) + time * 0.02F) % 1.0F;
				Vector3f at = VfxOut.along(points, last, t).add(0, 0.4F + 1.2F * hash(i * 3), 0).add(new Vector3f(right).mul((hash(i * 5) - 0.5F) * 0.8F));
				Vector3f dir = new Vector3f(Mth.cos(time * 0.2F + i), 0.6F, Mth.sin(time * 0.17F + i)).normalize();
				o.diamond(at, dir, VfxOut.across(dir), 0.35F, 0.2F, argb(0.3F * fade, hues[i % 4]), argb(0.9F * fade, hues[i % 4]));
			}
			if (!s.selfView) {
				o.star(end, s.camRight, s.camUp, 0.9F * (1.0F - apart) + 0.2F, 6, 0.4F, time * 0.1F, argb(0.8F * fade, 0xFFFFFF), argb(0.0F, PINK));
			}
		});
	}

	/** The afterimage: a head, a body, arms and legs facing the camera, sliced and slid apart sideways as it comes undone. */
	private static void figure(final VfxOut o, final Vector3f right, final float apart, final float time, final int color, final int seed) {
		int slices = 8;
		for (int i = 0; i < slices; i++) {
			float y0 = 1.85F * i / slices;
			float y1 = 1.85F * (i + 1) / slices;
			float shift = Mth.sin(time * 0.6F + i * 1.7F + seed) * 0.35F * apart;
			Vector3f off = new Vector3f(right).mul(shift).add(0, apart * 0.4F * (i - slices / 2.0F) / slices, 0);
			float w0 = width(y0);
			float w1 = width(y1);
			Vector3f a = new Vector3f(right).mul(-w0).add(0, y0, 0).add(off);
			Vector3f b = new Vector3f(right).mul(w0).add(0, y0, 0).add(off);
			Vector3f cc = new Vector3f(right).mul(w1).add(0, y1, 0).add(off);
			Vector3f d = new Vector3f(right).mul(-w1).add(0, y1, 0).add(off);
			o.quad(a, b, cc, d, color);
		}
	}

	/** Half the afterimage's width at height {@code y}: legs, a body with arms, a neck and a head. */
	private static float width(final float y) {
		if (y < 0.75F) {
			return 0.25F;
		}
		if (y < 1.45F) {
			return 0.42F;
		}
		if (y < 1.5F) {
			return 0.12F;
		}
		return 0.25F;
	}

	// ------------------------------------------------------------ Frieren

	/** A small white circle a Zoltraak flies out of: opens fast facing the shot's flight, a hexagram in it, flashes and fades. */
	private static void zoltraakCircle(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.2F);
		float fade = 1.0F - ease(p, 0.5F, 1.0F);
		Vector3f n = new Vector3f(s.reach);
		if (n.lengthSquared() < 1.0E-6F) {
			n.set(s.forward);
		}
		n.normalize();
		Vector3f a = VfxOut.across(n);
		Vector3f b = new Vector3f(n).cross(a).normalize();
		float r = Math.max(0.4F, s.size) * open;
		float spin = s.ageInTicks * 0.15F;
		glow(ps, c, o -> {
			Vector3f mid = new Vector3f();
			o.circle(mid, a, b, r, 0.022F, 28, argb(0.95F * fade, ELF));
			o.circle(mid, a, b, r * 0.82F, 0.014F, 28, argb(0.8F * fade, ELF_BLUE));
			o.polygram(mid, a, b, r * 0.82F, 6, 2, spin, 0.014F, argb(0.9F * fade, ELF));
			for (int i = 0; i < 12; i++) {
				float t = -spin + Mth.TWO_PI * i / 12;
				Vector3f d = new Vector3f(a).mul(Mth.cos(t)).add(new Vector3f(b).mul(Mth.sin(t)));
				o.bar(new Vector3f(d).mul(r * 0.86F).add(mid), new Vector3f(d).mul(r * (i % 3 == 0 ? 1.12F : 0.96F)).add(mid), 0.012F, 0.012F, argb(0.9F * fade, ELF),
					argb(0.9F * fade, ELF));
			}
			o.sprite(mid, a, b, r * 0.6F, argb(0.3F * fade, ELF_BLUE));
			float flash = 1.0F - ease(p, 0.0F, 0.35F);
			o.glint(new Vector3f(n).mul(0.05F), s.camRight, s.camUp, 0.15F + 0.5F * flash, s.seed, argb(fade, 0xFFFFFF));
		});
	}

	/**
	 * Judradjim: lightning spreads over the sky above the aimed spot in a web, flickering, and the great bolt is thrown down out of its
	 * middle; the ground flashes and sparks jump.
	 */
	private static void judradjim(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.35F, 1.0F);
		float bolt = 1.0F - ease(age, 4.0F, 14.0F);
		float spread = ease(age, 0.0F, 4.0F);
		int flicker = (int)(age / 2.0F);
		Vector3f sky = new Vector3f(0, 14.0F, 0);
		int nodes = 12;
		Vector3f[] node = new Vector3f[nodes];
		for (int i = 0; i < nodes; i++) {
			float a = Mth.TWO_PI * i / nodes + 0.3F * hash(s.seed + i);
			node[i] = polar(a, s.size * (0.8F + 1.0F * hash(s.seed * 3 + i)) * spread, 13.0F + 2.0F * hash(i * 7)).add(0, 0, 0);
		}
		glow(ps, c, o -> {
			int glowC = argb(0.5F * fade, ELF_BLUE);
			int core = argb(0.95F * fade, 0xFFFFFF);
			o.sprite(sky, s.camRight, s.camUp, s.size * 2.4F * spread, argb(0.25F * fade, ELF_BLUE));
			for (int i = 0; i < nodes; i++) {
				o.bolt(sky, node[i], s.seed + i + flicker * 31, 0.6F, 5, 0.25F, glowC, core);
				o.bolt(node[i], node[(i + 1) % nodes], s.seed * 3 + i + flicker * 17, 0.5F, 4, 0.18F, glowC, core);
				if (i % 2 == 0) {
					Vector3f out = new Vector3f(node[i]).add(new Vector3f(node[i].x, 0, node[i].z).normalize(1.5F)).add(0, -0.8F, 0);
					o.bolt(node[i], out, s.seed * 5 + i + flicker, 0.4F, 3, 0.14F, glowC, core);
				}
			}
			if (bolt > 0.0F) {
				Vector3f ground = new Vector3f(0, 0.1F, 0);
				o.bolt(sky, ground, s.seed * 7 + flicker * 13, 1.4F, 12, 0.9F * bolt + 0.2F, argb(0.7F * bolt, ELF_BLUE), argb(bolt, 0xFFFFFF));
				o.bolt(new Vector3f(sky).lerp(ground, 0.4F), polar(s.seed, s.size * 0.9F, 0.1F), s.seed * 11 + flicker, 0.6F, 6, 0.3F * bolt, argb(0.5F * bolt,
					ELF_BLUE), argb(0.9F * bolt, 0xFFFFFF));
				o.sprite(new Vector3f(0, 0.8F, 0), s.camRight, s.camUp, 3.5F * bolt, argb(0.8F * bolt, 0xFFFFFF));
			}
			float ring = ease(age, 0.0F, 10.0F);
			o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, s.size * ring - 0.5F), s.size * ring, argb(0.85F * fade, ELF_BLUE));
			for (int i = 0; i < 10; i++) {
				float a = Mth.TWO_PI * hash(s.seed + i * 3 + flicker);
				Vector3f from = polar(a, s.size * hash(i * 5 + flicker) * 0.8F, 0.15F);
				Vector3f to = new Vector3f(from).add(polar(a + 1.0F, 0.6F, 0.4F));
				o.bolt(from, to, s.seed + i + flicker * 7, 0.15F, 3, 0.08F, glowC, core);
			}
		});
	}

	/** Defensive Magic: a curved wall of hexagonal panels opens in front of the caster from the middle out, rippling with light. */
	private static void hexBarrier(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.85F, 1.0F);
		float time = s.ageInTicks;
		float r = s.size * (s.selfView ? 1.4F : 1.0F);
		float alpha = s.selfView ? 0.45F : 1.0F;
		Vector3f ahead = WitchLooks.ahead(s);
		float baseYaw = (float)Math.atan2(ahead.z, ahead.x);
		Vector3f mid = new Vector3f(0, 1.1F, 0);
		float step = 0.34F;
		shade(ps, c, o -> hexes(o, mid, baseYaw, r, step, age, time, fade * alpha, false));
		glow(ps, c, o -> {
			hexes(o, mid, baseYaw, r, step, age, time, fade * alpha, true);
			o.ring(new Vector3f(0, 0.14F, 0), r - 0.1F, r + 0.1F, argb(0.5F * fade, ELF_BLUE));
		});
	}

	private static void hexes(final VfxOut o, final Vector3f mid, final float baseYaw, final float r, final float step, final float age, final float time,
		final float alpha, final boolean lit) {
		for (int row = -2; row <= 3; row++) {
			for (int col = -3; col <= 3; col++) {
				float yaw = baseYaw + (col + (row % 2 != 0 ? 0.5F : 0.0F)) * step;
				float pitch = row * step * 0.87F;
				float dist = (float)Math.sqrt(col * col + row * row);
				float open = ease(age - dist * 1.5F, 0.0F, 4.0F);
				if (open <= 0.0F) {
					continue;
				}
				Vector3f n = new Vector3f(Mth.cos(pitch) * Mth.cos(yaw), Mth.sin(pitch), Mth.cos(pitch) * Mth.sin(yaw));
				Vector3f at = new Vector3f(n).mul(r).add(mid);
				float ripple = 0.6F + 0.4F * Mth.sin(time * 0.4F - dist * 1.3F);
				float size = r * step * 0.5F * open;
				if (lit) {
					o.hexagon(at, n, size, 0.0F, 0.012F, argb(0.9F * alpha * ripple, ELF), argb(0.18F * alpha * ripple, ELF_BLUE));
				} else {
					o.hexagon(at, n, size * 0.97F, 0.0F, 0.0F, 0, argb(0.22F * alpha, 0xD8E8FF));
				}
			}
		}
	}

	/**
	 * The Spell to Make a Field of Flowers: blue moon-weed blooms outward from the caster in a widening ring until the whole circle is in
	 * flower, fireflies of pale light drifting over it.
	 */
	private static void flowerField(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.9F, 1.0F);
		float time = s.ageInTicks;
		int[] petals = {MOONWEED, 0x8AB8FF, 0x5A90F0};
		float wave = ease(s.age(), 0.0F, 30.0F);
		shade(ps, c, o -> field(o, s, 70, s.size, 30.0F, petals, 0xCFE6FF, fade, false));
		glow(ps, c, o -> {
			field(o, s, 70, s.size, 30.0F, petals, 0xCFE6FF, fade, true);
			Vector3f ground = new Vector3f(0, 0.14F, 0);
			o.ring(ground, Math.max(0.0F, s.size * wave - 0.3F), s.size * wave, argb(0.7F * fade, MOONWEED));
			o.disc(ground, s.size * wave, argb(0.12F * fade, MOONWEED), argb(0.0F, MOONWEED));
			for (int i = 0; i < 18; i++) {
				float drift = (time * 0.015F + hash(i * 7 + s.seed)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 3) + drift, s.size * hash(i * 5 + 2), 0.3F + 1.3F * drift + 0.2F * Mth.sin(time * 0.1F + i));
				o.glint(at, s.camRight, s.camUp, 0.12F, time * 0.1F + i, argb(0.9F * fade * Mth.sin(Mth.PI * drift), 0xDDEEFF));
			}
		});
	}

	private FairyLooks() {
	}
}
