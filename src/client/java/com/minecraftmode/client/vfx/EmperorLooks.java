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
import org.joml.Vector4f;

/**
 * Looks of the pirate crews and emperors: Anne Bonny and Mary Read (Fate: the Free Bird, Mary's cutlass, their twin birds), Big Mom
 * (One Piece: Ikoku Sovereignty, Soul Pocus, Zeus, the croquembouche and Prometheus) and Whitebeard (the Island Shaker, his anchor and
 * his crew's mark).
 */
final class EmperorLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final int SKY = 0x7FDBFF;
	private static final int SKY_PALE = 0xE4F8FF;
	private static final int ROSE = 0xFF6A5A;
	private static final int GOLD = 0xFFD24A;
	private static final int GOLD_PALE = 0xFFF2B0;
	private static final int PINK = 0xFF7AB8;
	private static final int PINK_PALE = 0xFFD8EA;
	private static final int SOUL = 0xFFE8F4;
	private static final int FIRE = 0xFF6A1A;
	private static final int FIRE_CORE = 0xFFD27A;
	private static final int QUAKE = 0xE0F0FF;
	private static final int STEEL = 0x6A7078;
	private static final int STEEL_DARK = 0x22262C;
	/** Zeus is drawn this many times the size of his puffs. */
	private static final float CLOUD_SCALE = 1.7F;

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case FREE_BIRD -> freeBird(s, ps, c);
			case CUTLASS_ARC -> cutlassArc(s, ps, c);
			case TWIN_BIRDS -> twinBirds(s, ps, c);
			case IKOKU_SWING -> ikokuSwing(s, ps, c);
			case SOUL_POCUS -> soulPocus(s, ps, c);
			case ZEUS -> zeus(s, ps, c);
			case CROQUEMBOUCHE -> croquembouche(s, ps, c);
			case QUAKE -> quake(s, ps, c);
			case ANCHOR_DROP -> anchorDrop(s, ps, c);
			case MY_SONS -> mySons(s, ps, c);
			default -> {
			}
		}
	}

	// ------------------------------------------------------------ Anne Bonny and Mary Read

	/** A wing of {@code feathers} long feathers fanned out from {@code root} between the directions {@code from} and {@code to}. */
	private static void wing(final VfxOut o, final Vector3f root, final Vector3f from, final Vector3f to, final Vector3f normal, final float length, final int feathers,
		final int base, final int tip) {
		Vector3f prevMid = null;
		for (int i = 0; i < feathers; i++) {
			float t = (float)i / Math.max(1, feathers - 1);
			Vector3f d = new Vector3f(from).lerp(to, t).normalize();
			float len = length * (1.0F - 0.35F * t);
			Vector3f side = new Vector3f(d).cross(normal).normalize();
			// a broad flight feather: narrow at the quill, widest three quarters out, a rounded point
			Vector3f q0 = new Vector3f(d).mul(len * 0.3F).add(root);
			Vector3f mid = new Vector3f(d).mul(len * 0.78F).add(root);
			Vector3f end = new Vector3f(d).mul(len).add(root);
			float w = length * 0.11F;
			o.quad(new Vector3f(side).mul(-w * 0.4F).add(q0), new Vector3f(side).mul(w * 0.4F).add(q0), new Vector3f(side).mul(w).add(mid),
				new Vector3f(side).mul(-w).add(mid), base, base, VfxOut.lerp(base, tip, 0.5F), VfxOut.lerp(base, tip, 0.5F));
			o.quad(new Vector3f(side).mul(-w).add(mid), new Vector3f(side).mul(w).add(mid), end, end, VfxOut.lerp(base, tip, 0.5F), VfxOut.lerp(base, tip, 0.5F), tip, tip);
			o.bar(root, end, length * 0.008F, length * 0.004F, base, tip);
			// the coverts: the wing filled between the feathers near the body
			if (prevMid != null) {
				o.tri(root, prevMid, new Vector3f(d).mul(len * 0.55F).add(root), base, base & 0xFFFFFF | 0x60000000, base & 0xFFFFFF | 0x60000000);
			}
			prevMid = new Vector3f(d).mul(len * 0.55F).add(root);
		}
	}

	/**
	 * Caribbean Free Bird: Mary's cutlass crosses the target in two silver strokes, the pair's wings open behind it (Mary's sky blue, Anne's
	 * rose) and close in round it, and Anne's shot flashes point blank as the feathers scatter.
	 */
	private static void freeBird(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float h = Math.max(0.6F, s.size);
		float w = Math.max(0.4F, s.arg);
		float age = s.age();
		float fade = 1.0F - ease(s.progress, 0.7F, 1.0F);
		Vector3f mid = new Vector3f(0, h * 0.55F, 0);
		float r = Math.max(0.8F, Math.max(w, h * 0.55F));
		Vector3f right = s.camRight;
		Vector3f up = s.camUp;
		Vector3f away = new Vector3f(right).cross(up).normalize().negate();
		glow(ps, c, o -> {
			// the cutlass strokes
			for (int k = 0; k < 2; k++) {
				float q = ease(age - k * 2.0F, 0.0F, 4.0F);
				float gone = 1.0F - ease(age - k * 2.0F, 5.0F, 12.0F);
				if (q <= 0.0F || gone <= 0.0F) {
					continue;
				}
				Vector3f a = new Vector3f(right).mul(k == 0 ? -1.2F * r : 1.2F * r).add(new Vector3f(up).mul(1.1F * r)).add(mid);
				Vector3f b = new Vector3f(right).mul(k == 0 ? 1.2F * r : -1.2F * r).add(new Vector3f(up).mul(-1.1F * r)).add(mid);
				Vector3f end = new Vector3f(a).lerp(b, q);
				o.glowLine(a, end, 0.4F, argb(0.6F * gone, SKY), argb(0.8F * gone, SKY_PALE));
				o.bar(a, end, 0.03F, 0.05F, argb(gone, 0xFFFFFF), argb(gone, 0xFFFFFF));
			}
			// the wings behind the target, opening and folding round it
			float open = ease(age, 3.0F, 9.0F);
			float fold = ease(age, 12.0F, 20.0F);
			if (open > 0.0F) {
				Vector3f root = new Vector3f(away).mul(0.4F).add(mid).add(new Vector3f(up).mul(0.2F * r));
				for (int side = -1; side <= 1; side += 2) {
					Vector3f outward = new Vector3f(right).mul(side);
					Vector3f from = new Vector3f(up).mul(1.0F).add(new Vector3f(outward).mul(0.3F)).lerp(new Vector3f(outward).mul(0.5F).add(new Vector3f(away).mul(-1.0F)), fold);
					Vector3f to = new Vector3f(outward).add(new Vector3f(up).mul(-0.7F)).lerp(new Vector3f(outward).mul(0.2F).add(new Vector3f(away).mul(-1.0F)).add(
						new Vector3f(up).mul(-0.5F)), fold);
					int base = side < 0 ? argb(0.85F * fade * open, SKY_PALE) : argb(0.85F * fade * open, 0xFFD0C8);
					int tip = side < 0 ? argb(0.1F * fade, SKY) : argb(0.1F * fade, ROSE);
					wing(o, new Vector3f(right).mul(side * 0.2F).add(root), from, to, away, 1.9F * r * open, 7, base, tip);
				}
			}
			// Anne's shot, point blank
			float shot = age - 10.0F;
			if (shot >= 0.0F && shot < 6.0F) {
				float k = 1.0F - shot / 6.0F;
				Vector3f muzzle = new Vector3f(right).mul(-1.0F * r).add(mid).add(new Vector3f(away).mul(-0.5F));
				o.star(muzzle, right, up, 1.2F * k + 0.2F, 6, 0.25F, 0.3F, argb(0.95F * k, 0xFFFFFF), argb(0.0F, ROSE));
				o.glowLine(muzzle, new Vector3f(mid).add(new Vector3f(right).mul(1.5F * r)), 0.15F, argb(0.9F * k, 0xFFF0C0), argb(0.0F, ROSE));
			}
			// feathers scattering
			for (int i = 0; i < 12; i++) {
				float q = ease(age, 4.0F + hash(i) * 4.0F, 26.0F);
				if (q <= 0.0F) {
					continue;
				}
				Vector3f d = BladeLooks.shardDir(s.seed, i);
				Vector3f at = new Vector3f(d).mul(r * (0.4F + 1.6F * q)).add(mid).add(0, -0.8F * q * q, 0);
				Vector3f spin = new Vector3f(Mth.cos(age * 0.3F + i), 0.6F, Mth.sin(age * 0.2F + i)).normalize();
				o.diamond(at, spin, VfxOut.across(spin), 0.3F, 0.1F, argb(0.2F * fade, SKY), argb(0.9F * fade * (1.0F - q * 0.6F), i % 2 == 0 ? SKY_PALE : 0xFFD0C8));
			}
		});
	}

	/**
	 * Mary's Cutlass: a broad cutlass sweeps its arc across the front from right to left, a pale blue sheet of light trailing it and
	 * feathers tossed off the edge.
	 */
	private static void cutlassArc(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float age = s.age();
		float half = Math.max(30.0F, s.arg) * Mth.DEG_TO_RAD / 2;
		float r = s.size;
		Vector3f f = new Vector3f(s.forward);
		float swing = ease(age, 0.0F, 5.0F);
		float fade = 1.0F - ease(p, 0.55F, 1.0F);
		float now = half - 2 * half * swing;
		glow(ps, c, o -> {
			int n = 20;
			for (int i = 0; i < n; i++) {
				float t0 = half - 2 * half * swing * i / n;
				float t1 = half - 2 * half * swing * (i + 1) / n;
				float y0 = 0.45F - 0.9F * (half - t0) / (2 * half);
				float y1 = 0.45F - 0.9F * (half - t1) / (2 * half);
				Vector3f d0 = new Vector3f(f).rotateY(-t0);
				Vector3f d1 = new Vector3f(f).rotateY(-t1);
				float lead = (float)(i + 1) / n;
				float al = fade * (0.15F + 0.75F * lead * lead);
				o.quad(new Vector3f(d0).mul(r * 0.3F).add(0, y0, 0), new Vector3f(d0).mul(r).add(0, y0, 0), new Vector3f(d1).mul(r).add(0, y1, 0),
					new Vector3f(d1).mul(r * 0.3F).add(0, y1, 0), argb(0.0F, SKY), argb(al * 0.8F, SKY_PALE), argb(al * 0.8F, SKY_PALE), argb(0.0F, SKY));
				o.bar(new Vector3f(d0).mul(r).add(0, y0, 0), new Vector3f(d1).mul(r).add(0, y1, 0), 0.03F, 0.03F, argb(al, 0xFFFFFF), argb(al, 0xFFFFFF));
			}
			// the cutlass itself at the head of the sweep: a broad, slightly curved blade, its guard gold
			if (swing < 1.0F || p < 0.6F) {
				float y = 0.45F - 0.9F * (half - now) / (2 * half);
				Vector3f d = new Vector3f(f).rotateY(-now);
				Vector3f bend = new Vector3f(f).rotateY(-now - 0.12F);
				Vector3f hilt = new Vector3f(d).mul(0.5F).add(0, y, 0);
				Vector3f midBlade = new Vector3f(d).mul(r * 0.6F).add(0, y, 0);
				Vector3f tip = new Vector3f(bend).mul(r * 0.98F).add(0, y + 0.05F, 0);
				o.blade(hilt, midBlade, UP, 0.3F, argb(fade, 0xE8F0F8), argb(fade, 0xFFFFFF));
				o.blade(midBlade, tip, UP, 0.26F, argb(fade, 0xFFFFFF), argb(0.6F * fade, SKY_PALE));
				Vector3f across = VfxOut.across(d);
				o.bar(new Vector3f(across).mul(-0.2F).add(hilt), new Vector3f(across).mul(0.2F).add(hilt), 0.04F, 0.04F, argb(fade, GOLD), argb(fade, GOLD));
				o.sprite(tip, s.camRight, s.camUp, 0.4F, argb(0.7F * fade, SKY_PALE));
			}
			for (int i = 0; i < 9; i++) {
				float t = half - 2 * half * Math.min(swing, hash(s.seed + i));
				float q = ease(age, 1.0F + hash(i * 3) * 4.0F, 10.0F);
				Vector3f d = new Vector3f(f).rotateY(-t);
				Vector3f at = new Vector3f(d).mul(r * (0.85F + 0.2F * q)).add(0, 0.3F - 1.0F * q * q + (hash(i * 7) - 0.5F) * 0.6F, 0);
				Vector3f spin = new Vector3f(Mth.cos(age * 0.4F + i), 0.5F, Mth.sin(age * 0.3F + i)).normalize();
				o.diamond(at, spin, VfxOut.across(spin), 0.28F, 0.09F, argb(0.2F * fade, SKY), argb(0.85F * fade, SKY_PALE));
			}
		});
	}

	/**
	 * Pirate's Glory: two birds of light, Mary's sky blue and Anne's rose gold, wheel round the caster one above the other, trailing light,
	 * feathers drifting down round them and a golden ring of glory at their feet.
	 */
	private static void twinBirds(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.15F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		float self = s.selfView ? 0.55F : 1.0F;
		glow(ps, c, o -> {
			for (int b = 0; b < 2; b++) {
				float a = time * 0.13F + b * Mth.PI;
				float rr = 1.5F * open;
				float y = (b == 0 ? 1.9F : 1.2F) + 0.25F * Mth.sin(time * 0.1F + b);
				Vector3f at = polar(a, rr, y);
				Vector3f dir = polar(a + Mth.HALF_PI, 1.0F, 0.0F);
				int body = b == 0 ? argb(0.95F * k * self, SKY_PALE) : argb(0.95F * k * self, 0xFFE0D0);
				int wingCol = b == 0 ? argb(0.8F * k * self, SKY) : argb(0.8F * k * self, ROSE);
				o.bird(at, dir, UP, 1.4F, Mth.sin(time * 0.7F + b * 1.3F), body, wingCol);
				// the trail of light behind each
				Vector3f prev = at;
				for (int q = 1; q <= 8; q++) {
					Vector3f back = polar(a - q * 0.12F, rr, y - 0.03F * q);
					o.glowLine(prev, back, 0.08F, argb(0.5F * k * self * (1.0F - q / 9.0F), b == 0 ? SKY : ROSE), argb(0.4F * k * self * (1.0F - q / 9.0F),
						b == 0 ? SKY : ROSE));
					prev = back;
				}
			}
			for (int i = 0; i < 14; i++) {
				float q = (time * 0.025F + hash(s.seed + i * 3)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 5) + q * 2.0F, 0.9F + 0.8F * hash(i), 2.6F - 2.4F * q);
				Vector3f spin = new Vector3f(Mth.cos(time * 0.2F + i), 0.6F, Mth.sin(time * 0.15F + i)).normalize();
				o.diamond(at, spin, VfxOut.across(spin), 0.26F, 0.08F, argb(0.2F * k, SKY), argb(0.8F * k * Mth.sin(q * Mth.PI), i % 2 == 0 ? SKY_PALE : 0xFFD0C8));
			}
			o.ring(new Vector3f(0, 0.14F, 0), 1.1F, 1.35F, argb(0.7F * k, GOLD));
			o.ring(new Vector3f(0, 0.15F, 0), 0.5F, 0.6F, argb(0.5F * k, GOLD_PALE));
		});
	}

	// ------------------------------------------------------------ Big Mom

	/**
	 * Ikoku Sovereignty: Napoleon swung overhead and down across the front in one great diagonal stroke, a broad sheet of pink force with a
	 * golden edge left behind it, the bicorne on its hilt, and a shock rolling over the ground round the caster as far as the push.
	 */
	private static void ikokuSwing(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float age = s.age();
		float fade = 1.0F - ease(p, 0.3F, 0.8F);
		float swing = ease(age, 0.0F, 5.0F);
		Vector3f f = new Vector3f(s.forward);
		Vector3f right = VfxOut.across(f);
		Vector3f b = new Vector3f(UP).mul(0.82F).add(new Vector3f(right).mul(0.57F)).normalize();
		// the stroke itself hits nothing (the wave does, about a block either side of its line): kept to about 1.3 blocks either side
		float r = s.size * 0.4F;
		float from = 2.3F;
		float to = -0.95F;
		float now = Mth.lerp(swing, from, to);
		float ground = -1.18F;
		glow(ps, c, o -> {
			int n = 22;
			for (int i = 0; i < n; i++) {
				float a0 = Mth.lerp((float)i / n, from, now);
				float a1 = Mth.lerp((float)(i + 1) / n, from, now);
				Vector3f d0 = new Vector3f(f).mul(Mth.cos(a0)).add(new Vector3f(b).mul(Mth.sin(a0)));
				Vector3f d1 = new Vector3f(f).mul(Mth.cos(a1)).add(new Vector3f(b).mul(Mth.sin(a1)));
				float lead = (float)(i + 1) / n;
				float al = fade * (0.2F + 0.7F * lead);
				o.quad(new Vector3f(d0).mul(0.8F), new Vector3f(d0).mul(r), new Vector3f(d1).mul(r), new Vector3f(d1).mul(0.8F), argb(0.0F, PINK), argb(al * 0.85F, PINK),
					argb(al * 0.85F, PINK), argb(0.0F, PINK));
				o.bar(new Vector3f(d0).mul(r), new Vector3f(d1).mul(r), 0.07F, 0.07F, argb(al, GOLD_PALE), argb(al, GOLD_PALE));
				o.glowLine(new Vector3f(d0).mul(r), new Vector3f(d1).mul(r), 0.4F, argb(al * 0.6F, GOLD), argb(al * 0.6F, GOLD));
			}
			Vector3f d = new Vector3f(f).mul(Mth.cos(now)).add(new Vector3f(b).mul(Mth.sin(now)));
			if (p < 0.6F) {
				Vector3f hilt = new Vector3f(d).mul(0.6F);
				Vector3f along = new Vector3f(f).mul(-Mth.sin(now)).add(new Vector3f(b).mul(Mth.cos(now)));
				o.blade(hilt, new Vector3f(d).mul(r + 0.4F).add(hilt), along, 0.2F, argb(fade, 0xFFFFFF), argb(fade, PINK_PALE));
				o.sprite(hilt, s.camRight, s.camUp, 0.5F, argb(0.6F * fade, GOLD));
			}
			// the shock rolling over the ground round the caster, out to the push (the cone's range is the push radius)
			float q = ease(age, 3.0F, 12.0F);
			o.ring(new Vector3f(0, ground + 0.14F, 0), Math.max(0.0F, s.size * q - 0.5F), s.size * q, argb(0.6F * (1.0F - q), PINK_PALE));
			o.ring(new Vector3f(0, ground + 0.15F, 0), Math.max(0.0F, s.size - 0.15F), s.size, argb(0.55F * q * fade, PINK_PALE));
			for (int i = 0; i < 10; i++) {
				Vector3f at = new Vector3f(f).rotateY((hash(s.seed + i) - 0.5F) * 2.0F).mul(2.0F + 4.0F * q * hash(i * 3)).add(0, ground + 0.5F + 1.5F * hash(i * 5) * q, 0);
				o.glint(at, s.camRight, s.camUp, 0.25F, i + age * 0.2F, argb(0.8F * fade, i % 2 == 0 ? GOLD_PALE : PINK_PALE));
			}
		});
		// the bicorne on Napoleon's hilt
		if (p < 0.6F) {
			Vector3f d = new Vector3f(f).mul(Mth.cos(now)).add(new Vector3f(b).mul(Mth.sin(now)));
			Vector3f hat = new Vector3f(d).mul(0.55F);
			Vector3f across = new Vector3f(d).cross(b).normalize();
			shade(ps, c, o -> {
				o.tri(new Vector3f(across).mul(-0.42F).add(hat), new Vector3f(across).mul(0.42F).add(hat), new Vector3f(d).mul(-0.25F).add(hat), argb(0.95F * fade, 0x1A1418),
					argb(0.95F * fade, 0x1A1418), argb(0.95F * fade, 0x3A2A30));
				o.tri(new Vector3f(across).mul(-0.42F).add(hat), new Vector3f(across).mul(0.42F).add(hat), new Vector3f(d).mul(0.18F).add(hat), argb(0.95F * fade, 0x1A1418),
					argb(0.95F * fade, 0x1A1418), argb(0.95F * fade, 0x2A2026));
			});
		}
	}

	/** The enemies' chests (or, with nobody near, spots round the caster) a look reaches for. */
	private static List<Vector3f> targetsOr(final State s, final int fallback, final float radius, final float y) {
		List<Vector3f> from = new ArrayList<>();
		for (Vector4f t : s.targets) {
			from.add(new Vector3f(t.x, t.y + t.w * 0.55F, t.z));
		}
		if (from.isEmpty()) {
			for (int i = 0; i < fallback; i++) {
				from.add(polar(Mth.TWO_PI * i / fallback + 0.3F, radius, y));
			}
		}
		return from;
	}

	/**
	 * Soul Pocus: "life or death?" Pale souls are pulled out of every enemy near, three a body: each a little ghost (a round head with dark
	 * eyes and a wailing mouth, a wavering tail streaming back to the body it left) that flies along a pink thread into the caster's raised
	 * hand; a pink ring rolls out over the ground as far as it reaches.
	 */
	private static void soulPocus(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float time = s.ageInTicks;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f hand = new Vector3f(ahead).mul(s.selfView ? 0.8F : 0.45F).add(new Vector3f(right).mul(0.35F)).add(0, s.selfView ? 1.35F : 1.9F, 0);
		List<Vector3f> from = targetsOr(s, 3, s.size * 0.7F, 1.0F);
		Vector3f toward = new Vector3f(s.camRight).cross(s.camUp).normalize();
		List<Vector3f> heads = new ArrayList<>();
		List<List<Vector3f>> tails = new ArrayList<>();
		List<Vector3f> origins = new ArrayList<>();
		for (int j = 0; j < from.size(); j++) {
			for (int k = 0; k < 3; k++) {
				float start = 0.12F + 0.12F * k + 0.03F * j;
				float q = ease(p, start, start + 0.45F);
				if (q >= 1.0F) {
					continue;
				}
				Vector3f src = from.get(j);
				// pulled up out of the body first, then flying on an arc into the hand
				float out = ease(p, start - 0.1F, start);
				Vector3f side = new Vector3f(right).mul((k - 1) * 0.5F);
				Vector3f pulled = new Vector3f(src).add(side).add(0, 0.7F * out, 0);
				Vector3f head = new Vector3f(pulled).lerp(hand, q * q).add(0, 1.3F * Mth.sin(q * Mth.PI), 0);
				Vector3f back = new Vector3f(src).sub(head);
				if (back.lengthSquared() < 1.0E-4F) {
					back.set(0, -1, 0);
				}
				back.normalize();
				Vector3f wave = VfxOut.across(back);
				List<Vector3f> tail = new ArrayList<>();
				for (int i = 0; i <= 7; i++) {
					float t = i / 7.0F;
					float sway = 0.18F * t * Mth.sin(time * 0.7F + t * 5.0F + j * 2 + k);
					tail.add(new Vector3f(back).mul(t * 1.3F * Math.max(0.3F, out)).add(head).add(new Vector3f(wave).mul(sway)));
				}
				heads.add(head);
				tails.add(tail);
				origins.add(src);
			}
		}
		shade(ps, c, o -> {
			for (int i = 0; i < heads.size(); i++) {
				Vector3f head = heads.get(i);
				o.tube(tails.get(i), 0.27F, 0.02F, 6, argb(0.7F * fade, SOUL), argb(0.7F * fade, 0xB8A8D8));
				o.ellipsoid(head, new Vector3f(0.3F, 0, 0), new Vector3f(0, 0.33F, 0), new Vector3f(0, 0, 0.3F), 6, 10, argb(0.88F * fade, 0xFFFFFF),
					argb(0.88F * fade, 0xC8B8E8));
				// the face, on the side towards the viewer: two dark eyes and a long wailing mouth
				Vector3f face = new Vector3f(toward).mul(0.31F).add(head);
				for (int t = -1; t <= 1; t += 2) {
					o.plate(new Vector3f(s.camRight).mul(t * 0.11F).add(face).add(new Vector3f(s.camUp).mul(0.07F)), s.camRight, s.camUp, 0.065F, 8,
						argb(0.95F * fade, 0x24142C));
				}
				o.ellipsoid(new Vector3f(s.camUp).mul(-0.12F).add(face), new Vector3f(s.camRight).mul(0.05F), new Vector3f(s.camUp).mul(0.09F),
					new Vector3f(toward).mul(0.01F), 3, 8, argb(0.95F * fade, 0x24142C), argb(0.95F * fade, 0x24142C));
			}
		});
		glow(ps, c, o -> {
			for (int i = 0; i < heads.size(); i++) {
				Vector3f head = heads.get(i);
				// the thread it is drawn out on
				o.glowLine(origins.get(i), head, 0.07F, argb(0.35F * fade, PINK), argb(0.55F * fade, PINK_PALE));
				o.sprite(head, s.camRight, s.camUp, 0.75F, argb(0.22F * fade, PINK));
				List<Vector3f> tail = tails.get(i);
				for (int k = 0; k + 1 < tail.size(); k++) {
					o.glowLine(tail.get(k), tail.get(k + 1), 0.2F * (1.0F - k / 8.0F), argb(0.25F * fade, PINK_PALE), argb(0.2F * fade, PINK_PALE));
				}
			}
			o.sprite(hand, s.camRight, s.camUp, 0.45F + 0.25F * ease(p, 0.5F, 0.8F), argb(0.5F * fade, PINK));
			o.circle(hand, s.camRight, s.camUp, 0.35F, 0.025F, 14, argb(0.8F * fade, PINK_PALE));
			float q = ease(p, 0.0F, 0.5F);
			o.ring(new Vector3f(0, 0.15F, 0), s.size * q - 0.4F, s.size * q, argb(0.6F * (1.0F - q * 0.7F) * fade, PINK));
		});
	}

	/**
	 * Zeus: the thundercloud homie gathers over the aimed spot, grinning down, crackling, and throws his bolt: jagged lightning from his
	 * belly to the ground, flickering, with a flash and a ring where it strikes.
	 */
	private static void zeus(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float form = ease(age, 0.0F, 4.0F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float k = form * fade;
		float time = s.ageInTicks;
		Vector3f face = new Vector3f(s.camRight).cross(s.camUp).normalize();
		face.y = 0;
		if (face.lengthSquared() < 1.0E-4F) {
			face.set(new Vector3f(s.forward).negate());
		}
		face.normalize();
		Vector3f side = VfxOut.across(face);
		Vector3f cloud = new Vector3f(0, 6.4F + 0.1F * Mth.sin(time * 0.15F), 0);
		float[][] puffs = {{0, 0, 1.15F}, {-1.0F, -0.15F, 0.85F}, {1.0F, -0.15F, 0.85F}, {-1.8F, -0.35F, 0.55F}, {1.8F, -0.35F, 0.55F}, {-0.5F, 0.65F, 0.7F},
			{0.55F, 0.6F, 0.65F}};
		// a big cloud: drawn at 1.7 times the size of its parts round its middle
		ps.pushPose();
		ps.translate(cloud.x, cloud.y, cloud.z);
		ps.scale(CLOUD_SCALE, CLOUD_SCALE, CLOUD_SCALE);
		ps.translate(-cloud.x, -cloud.y, -cloud.z);
		shade(ps, c, o -> {
			for (float[] q : puffs) {
				Vector3f at = new Vector3f(side).mul(q[0] * form).add(cloud).add(0, q[1] * form, 0);
				float r = q[2] * form;
				o.ellipsoid(at, new Vector3f(side).mul(r), new Vector3f(UP).mul(r * 0.85F), new Vector3f(face).mul(r * 0.9F), 6, 10, argb(0.97F * fade, 0xA8A8C8),
					argb(0.97F * fade, 0x343450));
			}
			if (form > 0.5F) {
				// the face: eyes looking down and a wide grin
				Vector3f front = new Vector3f(face).mul(1.08F * form).add(cloud);
				for (int t = -1; t <= 1; t += 2) {
					Vector3f eye = new Vector3f(side).mul(t * 0.38F).add(front).add(0, 0.2F, 0);
					o.plate(eye, side, UP, 0.2F, 10, argb(0.98F * fade, 0xFFFFFF));
					o.plate(new Vector3f(face).mul(0.02F).add(eye).add(0, -0.07F, 0), side, UP, 0.09F, 8, argb(0.98F * fade, 0x101018));
				}
				Vector3f prevA = null;
				Vector3f prevB = null;
				for (int i = 0; i <= 10; i++) {
					float t = i / 10.0F * 2 - 1;
					Vector3f a = new Vector3f(side).mul(t * 0.55F).add(front).add(0, -0.25F + 0.18F * t * t, 0);
					Vector3f b = new Vector3f(side).mul(t * 0.5F).add(front).add(0, -0.25F + 0.18F * t * t - 0.2F * (1.0F - t * t), 0);
					if (prevA != null) {
						o.quad(prevA, a, b, prevB, argb(0.98F * fade, 0xFFFFFF));
						o.bar(prevB, b, 0.02F, 0.02F, argb(0.98F * fade, 0x101018), argb(0.98F * fade, 0x101018));
					}
					prevA = a;
					prevB = b;
				}
			}
		});
		ps.popPose();
		glow(ps, c, o -> {
			// crackling over the cloud
			int seed = s.seed * 7 + (int)(time / 2);
			for (int i = 0; i < 4; i++) {
				Vector3f a = new Vector3f(side).mul((hash(seed + i) - 0.5F) * 3.4F * CLOUD_SCALE * form).add(cloud).add(0, -0.4F + hash(seed * 3 + i) * 0.8F, 0)
					.add(new Vector3f(face).mul(1.5F));
				Vector3f b = new Vector3f(side).mul((hash(seed + i * 5) - 0.5F) * 3.4F * CLOUD_SCALE * form).add(cloud).add(0, -0.6F, 0).add(new Vector3f(face).mul(1.5F));
				o.bolt(a, b, seed + i, 0.25F, 4, 0.08F, argb(0.6F * k, 0xFFF080), argb(0.9F * k, 0xFFFFFF));
			}
			// the bolt, flickering for its first half second
			boolean on = age < 12.0F && hash((int)age * 13 + s.seed) > 0.25F;
			if (on) {
				float w = 1.0F - age / 14.0F;
				Vector3f from = new Vector3f(cloud).add(0, -1.2F, 0);
				o.bolt(from, new Vector3f(0, 0.1F, 0), s.seed + (int)age, 0.9F, 9, 0.5F * w + 0.1F, argb(0.8F * w, 0xFFF080), argb(w, 0xFFFFFF));
				for (int i = 0; i < 2; i++) {
					Vector3f mid = new Vector3f(from).lerp(new Vector3f(0, 0.1F, 0), 0.35F + 0.25F * i);
					Vector3f d = BladeLooks.shardDir(s.seed + (int)age, i);
					d.y = -Math.abs(d.y) - 0.4F;
					o.bolt(mid, new Vector3f(d).mul(2.0F).add(mid), s.seed + i * 3 + (int)age, 0.4F, 5, 0.18F, argb(0.6F * w, 0xFFF080), argb(0.8F * w, 0xFFFFFF));
				}
			}
			float hit = ease(age, 0.0F, 10.0F);
			o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, s.size * hit - 0.5F), s.size * hit, argb(0.8F * (1.0F - hit), 0xFFF080));
			float flash = 1.0F - ease(age, 0.0F, 6.0F);
			o.sprite(new Vector3f(0, 0.8F, 0), s.camRight, s.camUp, s.size * flash + 0.2F, argb(0.8F * flash, 0xFFFFFF));
			o.ring(new Vector3f(0, 0.15F, 0), s.size - 0.25F, s.size, argb(0.5F * k, 0xFFF080));
		});
	}

	/**
	 * Croquembouche Craving: a tower of cream puffs bound in caramel threads stands beside the caster and is gobbled up puff by puff, then
	 * Prometheus, the sun homie, blazes up over the caster's shoulder grinning, and a pink glow rises round them.
	 */
	private static void croquembouche(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.82F, 1.0F);
		float time = s.ageInTicks;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f tower = new Vector3f(ahead).mul(1.0F).add(new Vector3f(right).mul(1.0F));
		Vector3f mouth = new Vector3f(ahead).mul(0.25F).add(0, 1.55F, 0);
		float stand = ease(p, 0.0F, 0.12F);
		List<Vector3f> puffs = new ArrayList<>();
		for (int row = 0; row < 6; row++) {
			int n = Math.max(1, 8 - row * 1 - (row >= 4 ? 2 : 0));
			float rr = 0.55F * (1.0F - row / 6.0F);
			for (int i = 0; i < n; i++) {
				float a = Mth.TWO_PI * i / n + row * 0.4F;
				puffs.add(polar(a, n == 1 ? 0.0F : rr, (0.2F + row * 0.25F) * stand).add(tower));
			}
		}
		int total = puffs.size();
		float eat = ease(p, 0.25F, 0.7F) * total;
		Vector3f sun = new Vector3f(right).mul(-0.95F).add(new Vector3f(ahead).mul(-0.45F)).add(0, 2.5F, 0);
		float blaze = ease(p, 0.3F, 0.45F) * fade;
		Vector3f face = new Vector3f(s.camRight).cross(s.camUp).normalize();
		shade(ps, c, o -> {
			for (int i = 0; i < total; i++) {
				// eaten from the top down
				int order = total - 1 - i;
				float gone = eat - order;
				Vector3f at = new Vector3f(puffs.get(i));
				if (gone >= 1.0F) {
					continue;
				}
				if (gone > 0.0F) {
					at.lerp(mouth, gone);
				}
				float r = 0.15F * (gone > 0.0F ? 1.0F - 0.5F * gone : 1.0F) * stand;
				o.ellipsoid(at, new Vector3f(r, 0, 0), new Vector3f(0, r * 0.85F, 0), new Vector3f(0, 0, r), 4, 7, argb(0.97F * fade, 0xF4C878), argb(0.97F * fade, 0x9A5A20));
			}
			if (blaze > 0.0F && !s.selfView) {
				Vector3f fside = VfxOut.across(face);
				Vector3f front = new Vector3f(face).mul(0.62F * blaze).add(sun);
				for (int t = -1; t <= 1; t += 2) {
					o.plate(new Vector3f(fside).mul(t * 0.17F).add(front).add(0, 0.1F, 0), fside, UP, 0.1F * blaze, 8, argb(0.95F, 0x3A1004));
				}
				Vector3f prev = null;
				for (int i = 0; i <= 8; i++) {
					float t = i / 8.0F * 2 - 1;
					Vector3f m = new Vector3f(fside).mul(t * 0.24F * blaze).add(front).add(0, -0.1F + 0.08F * t * t - 0.06F, 0);
					if (prev != null) {
						o.bar(prev, m, 0.03F, 0.03F, argb(0.95F, 0x3A1004), argb(0.95F, 0x3A1004));
					}
					prev = m;
				}
			}
		});
		glow(ps, c, o -> {
			// caramel threads spun round the tower
			if (eat < total * 0.7F) {
				float keep = 1.0F - eat / (total * 0.7F);
				for (int w = 0; w < 2; w++) {
					Vector3f prev = null;
					for (int i = 0; i <= 24; i++) {
						float t = i / 24.0F;
						float a = w * Mth.PI + t * 12.0F + time * 0.05F;
						Vector3f q = polar(a, 0.62F * (1.0F - t) + 0.05F, (0.15F + 1.4F * t) * stand).add(tower);
						if (prev != null) {
							o.glowLine(prev, q, 0.035F, argb(0.8F * keep * fade, GOLD), argb(0.8F * keep * fade, GOLD));
						}
						prev = q;
					}
				}
			}
			if (blaze > 0.0F && !s.selfView) {
				o.sprite(sun, s.camRight, s.camUp, 1.3F * blaze, argb(0.6F * blaze, FIRE));
				o.sprite(sun, s.camRight, s.camUp, 0.6F * blaze, argb(0.9F * blaze, FIRE_CORE));
				for (int i = 0; i < 12; i++) {
					float a = Mth.TWO_PI * i / 12 + time * 0.05F;
					Vector3f d = new Vector3f(s.camRight).mul(Mth.cos(a)).add(new Vector3f(s.camUp).mul(Mth.sin(a)));
					o.flame(new Vector3f(d).mul(0.45F * blaze).add(sun), d, (0.5F + 0.3F * hash(i + (int)(time / 3))) * blaze, 0.35F, time * 0.7F + i,
						argb(0.85F * blaze, FIRE_CORE), argb(0.0F, FIRE));
				}
			}
			float rise = ease(p, 0.35F, 0.6F) * fade;
			for (int i = 0; i < 3; i++) {
				float q = (time * 0.04F + i / 3.0F) % 1.0F;
				o.ring(new Vector3f(0, 0.1F + q * (s.selfView ? 0.8F : 2.0F), 0), 0.7F, 0.85F, argb(0.6F * rise * Mth.sin(Mth.PI * q), PINK));
			}
			o.ring(new Vector3f(0, 0.14F, 0), 0.6F, 1.2F, argb(0.5F * rise, PINK_PALE));
		});
		if (blaze > 0.0F && !s.selfView) {
			NavyLooks.ball(ps, c, sun, 0.5F * blaze, 1.0F, 0xFF9A3A, 0.45F * blaze);
			ps.pushPose();
			ps.translate(sun.x, sun.y, sun.z);
			c.submitCustomGeometry(ps, VfxDraw.SHADE, (pose, buffer) -> VfxDraw.ball(pose, buffer, 0.48F * blaze, 0xFF8A20, 0.55F * blaze));
			ps.popPose();
		}
	}

	// ------------------------------------------------------------ Whitebeard

	/**
	 * Island Shaker: the fist strikes the air in front of the caster and the air cracks like glass round it, then the quake bursts out in
	 * a shell of white force round the caster, the ground splitting outward from their feet and rubble thrown up, as far as the nova.
	 */
	private static void quake(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.7F, 1.0F);
		float r = s.size;
		Vector3f f = new Vector3f(s.forward);
		Vector3f right = VfxOut.across(f);
		Vector3f punch = new Vector3f(f).mul(1.3F).add(0, 1.3F, 0);
		float crack = ease(age, 0.0F, 5.0F);
		float glass = 1.0F - ease(age, 8.0F, 16.0F);
		float shell = ease(age, 3.0F, 16.0F);
		List<List<Vector3f>> cracks = new ArrayList<>();
		for (int i = 0; i < 12; i++) {
			float a = Mth.TWO_PI * i / 12 + hash(s.seed + i) * 0.4F;
			Vector3f d = new Vector3f(right).mul(Mth.cos(a)).add(new Vector3f(UP).mul(Mth.sin(a)));
			cracks.add(VfxOut.zigzag(punch, new Vector3f(d).mul((1.2F + 0.8F * hash(i * 3)) * crack).add(punch), s.seed + i, 0.18F, 5));
		}
		// the quake (nova + launch) is centred on the caster's feet: the shell and the ground cracks are too, the glass stays at the fist
		Vector3f core = new Vector3f(0, 1.3F, 0);
		List<List<Vector3f>> ground = new ArrayList<>();
		float run = ease(age, 3.0F, 14.0F);
		Vector3f foot = new Vector3f(0, 0.16F, 0);
		for (int i = 0; i < 10; i++) {
			float a = Mth.TWO_PI * i / 10 + hash(s.seed * 5 + i) * 0.5F;
			ground.add(VfxOut.zigzag(foot, polar(a, r * (0.6F + 0.4F * hash(i * 7)) * run, 0.0F).add(foot), s.seed * 3 + i, 0.6F, 8));
		}
		shade(ps, c, o -> {
			for (List<Vector3f> g : ground) {
				o.polyline(g, 0.09F, argb(0.85F * fade, 0x141210));
			}
			// rubble thrown up as the ground heaves (the launch half a second in)
			for (int i = 0; i < 14; i++) {
				float t = (age - 10.0F - hash(i) * 3.0F) / 16.0F;
				if (t <= 0.0F || t >= 1.0F) {
					continue;
				}
				Vector3f at = polar(Mth.TWO_PI * hash(s.seed + i * 3), r * (0.3F + 0.6F * hash(i * 5)), 0.2F + 4.0F * t * (1.0F - t) * (1.0F + hash(i * 7)));
				float sz = 0.15F + 0.15F * hash(i * 11);
				o.ellipsoid(at, new Vector3f(sz, 0, 0), new Vector3f(0, sz * 0.8F, 0), new Vector3f(0, 0, sz), 3, 4, argb(0.97F, 0x8A8070), argb(0.97F, 0x3A342C));
			}
		});
		glow(ps, c, o -> {
			// the air cracking like glass round the blow
			if (glass > 0.0F) {
				o.sprite(punch, s.camRight, s.camUp, 1.0F + 1.2F * crack, argb(0.6F * glass, 0xFFFFFF));
				o.circle(punch, right, UP, 1.6F * crack, 0.035F, 24, argb(0.8F * glass, QUAKE));
				for (List<Vector3f> k : cracks) {
					o.glowPolyline(k, 0.08F, argb(0.6F * glass, QUAKE));
					o.polyline(k, 0.02F, argb(glass, 0xFFFFFF));
				}
				// shards of the broken air
				for (int i = 0; i < 10; i++) {
					Vector3f d = BladeLooks.shardDir(s.seed, i);
					Vector3f at = new Vector3f(d).mul(0.4F + 1.8F * shell).add(punch);
					o.diamond(at, d, VfxOut.across(d), 0.35F, 0.15F, argb(0.3F * glass, QUAKE), argb(0.9F * glass, 0xFFFFFF));
				}
			}
			// the shell of the quake bursting out
			if (shell > 0.0F && shell < 1.0F) {
				float rr = r * shell;
				float al = 0.8F * (1.0F - shell);
				o.circle(core, right, UP, rr, 0.06F, 36, argb(al, QUAKE));
				o.circle(core, new Vector3f(f), UP, rr, 0.05F, 36, argb(al * 0.8F, QUAKE));
				o.circle(core, right, new Vector3f(f), rr, 0.05F, 36, argb(al * 0.8F, QUAKE));
				o.ring(new Vector3f(0, 0.16F, 0), Math.max(0.0F, rr - 0.8F), rr, argb(al, QUAKE));
			}
			for (List<Vector3f> g : ground) {
				o.glowPolyline(g, 0.2F, argb(0.45F * fade, QUAKE));
			}
		});
	}

	/** An anchor hanging from its ring at {@code top} along {@code down}, {@code size} long, its arms spread along {@code side}. */
	private static void anchor(final VfxOut o, final Vector3f top, final Vector3f down, final Vector3f side, final float size, final float width, final int color) {
		Vector3f dn = new Vector3f(down).normalize();
		Vector3f sd = new Vector3f(side).sub(new Vector3f(dn).mul(dn.dot(side))).normalize();
		Vector3f normal = new Vector3f(dn).cross(sd).normalize();
		Vector3f crown = new Vector3f(dn).mul(size).add(top);
		o.circle(new Vector3f(dn).mul(-size * 0.1F).add(top), sd, dn, size * 0.1F, width * 0.6F, 10, color);
		o.bar(top, crown, width, width * 1.15F, color, color);
		Vector3f stock = new Vector3f(dn).mul(size * 0.16F).add(top);
		o.bar(new Vector3f(normal).mul(-size * 0.28F).add(stock), new Vector3f(normal).mul(size * 0.28F).add(stock), width * 0.8F, width * 0.8F, color, color);
		for (int k = -1; k <= 1; k += 2) {
			Vector3f prev = crown;
			for (int i = 1; i <= 5; i++) {
				float t = i / 5.0F;
				float a = t * 1.25F;
				Vector3f q = new Vector3f(sd).mul(k * size * 0.38F * Mth.sin(a)).add(new Vector3f(dn).mul(-size * 0.38F * (1.0F - Mth.cos(a)))).add(crown);
				o.bar(prev, q, width, width, color, color);
				prev = q;
			}
			// the fluke: a spade at the arm's end, pointing up and out
			Vector3f tipDir = new Vector3f(sd).mul(k * 0.45F).add(new Vector3f(dn).mul(-1.0F)).normalize();
			o.blade(new Vector3f(tipDir).mul(-size * 0.08F).add(prev), new Vector3f(tipDir).mul(size * 0.16F).add(prev), sd, size * 0.09F, color, color);
		}
	}

	/** Ticks since a leaping holder came down (from its trail: the end of the last fall), or -1 while it is still in the air. */
	private static float landed(final State s) {
		List<Vector3f> raw = s.trail;
		int n = raw.size();
		for (int i = n - 1; i >= 1; i--) {
			if (raw.get(i).y < raw.get(i - 1).y - 0.01F) {
				return i == n - 1 ? -1.0F : n - 1 - i;
			}
		}
		return s.age() > 14.0F ? 99.0F : -1.0F;
	}

	/**
	 * Anchor Drop: a great anchor of steel and white light hangs raised over the caster on its chain as they leap, the chain paying out from
	 * where they jumped; when they land it crashes down flukes first in front of them with a shock ring and cracks.
	 */
	private static void anchorDrop(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float fade = 1.0F - ease(s.progress, 0.82F, 1.0F);
		Vector3f holder = s.trail.isEmpty() ? new Vector3f() : new Vector3f(s.trail.getLast());
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = VfxOut.across(f);
		float since = landed(s);
		float size = 3.6F;
		float drop = since < 0.0F ? 0.0F : ease(since, 0.0F, 3.0F);
		Vector3f raised = new Vector3f(holder).add(new Vector3f(f).mul(-0.3F)).add(0, 2.6F + size, 0);
		Vector3f planted = new Vector3f(holder).add(new Vector3f(f).mul(1.7F)).add(0, size - 0.1F, 0);
		Vector3f top = new Vector3f(raised).lerp(planted, drop);
		Vector3f down = new Vector3f(0, -1, 0).add(new Vector3f(f).mul(0.35F * (1.0F - drop)));
		float impact = since < 0.0F ? 0.0F : ease(since, 0.0F, 14.0F);
		float r = s.size;
		Vector3f foot = new Vector3f(planted).add(0, -size + 0.25F, 0);
		shade(ps, c, o -> {
			anchor(o, top, down, side, size, 0.16F, argb(0.95F * fade, STEEL));
			o.chain(new Vector3f(0, 0.4F, 0), new Vector3f(top).add(0, 0.2F, 0), 0.4F, 0.0F, argb(0.9F * fade, STEEL_DARK));
			if (since >= 0.0F) {
				o.disc(new Vector3f(foot).add(0, -0.1F, 0), r * 0.6F, argb(0.6F * fade, 0x141210), argb(0.0F, 0x141210));
			}
		});
		glow(ps, c, o -> {
			anchor(o, top, down, side, size, 0.05F, argb(0.8F * fade, QUAKE));
			o.sprite(new Vector3f(top).add(new Vector3f(down).normalize(size * 0.5F)), s.camRight, s.camUp, 1.6F, argb(0.2F * fade, QUAKE));
			if (since < 0.0F) {
				return;
			}
			float flash = 1.0F - ease(since, 0.0F, 6.0F);
			o.sprite(new Vector3f(foot).add(0, 0.5F, 0), s.camRight, s.camUp, r * 0.9F * flash + 0.2F, argb(0.8F * flash, 0xFFFFFF));
			// the slam hits round where the caster landed (not round the anchor in front of them): the shock ring and the cracks are centred there
			Vector3f landing = new Vector3f(holder.x, holder.y + 0.15F, holder.z);
			o.ring(landing, Math.max(0.0F, r * impact - 0.6F), r * impact, argb(0.85F * (1.0F - impact), QUAKE));
			for (int i = 0; i < 8; i++) {
				float a = Mth.TWO_PI * i / 8 + hash(s.seed + i);
				o.bolt(new Vector3f(landing).add(0, 0.01F, 0), polar(a, r * 0.85F * ease(since, 0.0F, 6.0F), 0.01F).add(landing), s.seed + i, 0.4F, 6, 0.1F,
					argb(0.5F * fade, QUAKE), argb(0.8F * fade, 0xFFFFFF));
			}
			for (int i = 0; i < 10; i++) {
				Vector3f d = BladeLooks.shardDir(s.seed, i);
				d.y = Math.abs(d.y) + 0.3F;
				float q = ease(since, 0.0F, 12.0F);
				Vector3f at = new Vector3f(d).mul(0.4F + 2.2F * q).add(foot).add(0, -2.5F * q * q, 0);
				if (at.y > holder.y + 0.2F) {
					o.glint(at, s.camRight, s.camUp, 0.2F, i, argb(0.8F * (1.0F - q), QUAKE));
				}
			}
		});
	}

	/**
	 * My Sons: the Whitebeard Pirates' mark (a skull with a crescent moustache over a cross) rises behind the caster on a dark field, rays of
	 * light behind it, while rings of light wash out over the ground to the crew round them.
	 */
	private static void mySons(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.2F);
		float fade = 1.0F - ease(p, 0.82F, 1.0F);
		float k = open * fade;
		float time = s.ageInTicks;
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f mark = new Vector3f(ahead).mul(-1.3F).add(0, 3.3F + 0.4F * open, 0);
		float r = 1.1F * open;
		Vector3f behind = new Vector3f(ahead).mul(-0.04F);
		shade(ps, c, o -> {
			if (r > 0.05F) {
				o.plate(new Vector3f(mark).add(behind), right, UP, r * 1.55F, 28, argb(0.7F * fade, 0x1A1A2A));
				o.jollyRoger(mark, right, UP, r, VfxOut.WHITEBEARD, argb(0.97F * fade, 0xF4F4F0), argb(0.97F * fade, 0x101018));
			}
		});
		glow(ps, c, o -> {
			if (r > 0.05F) {
				o.circle(mark, right, UP, r * 1.55F, 0.05F, 32, argb(0.9F * k, QUAKE));
				o.circle(mark, right, UP, r * 1.7F, 0.025F, 32, argb(0.5F * k, QUAKE));
				for (int i = 0; i < 16; i++) {
					float a = Mth.TWO_PI * i / 16 + time * 0.01F;
					Vector3f d = new Vector3f(right).mul(Mth.cos(a)).add(new Vector3f(UP).mul(Mth.sin(a)));
					o.glowLine(new Vector3f(d).mul(r * 1.7F).add(mark).add(new Vector3f(behind).mul(2.0F)),
						new Vector3f(d).mul(r * (2.4F + 0.6F * hash(i))).add(mark).add(new Vector3f(behind).mul(2.0F)), 0.12F, argb(0.5F * k, QUAKE), argb(0.0F, QUAKE));
				}
			}
			for (int w = 0; w < 3; w++) {
				float q = ease(p, 0.05F + w * 0.15F, 0.55F + w * 0.15F);
				if (q <= 0.0F || q >= 1.0F) {
					continue;
				}
				o.ring(new Vector3f(0, 0.15F, 0), s.size * q - 0.5F, s.size * q, argb(0.7F * (1.0F - q), QUAKE));
			}
			for (int i = 0; i < 12; i++) {
				float q = (time * 0.03F + hash(s.seed + i)) % 1.0F;
				Vector3f at = polar(Mth.TWO_PI * hash(i * 7), 1.0F + 2.0F * hash(i * 3), q * 2.6F);
				o.glint(at, s.camRight, s.camUp, 0.16F, i + time * 0.1F, argb(0.8F * k * Mth.sin(q * Mth.PI), 0xFFFFFF));
			}
		});
	}

	private EmperorLooks() {
	}
}
