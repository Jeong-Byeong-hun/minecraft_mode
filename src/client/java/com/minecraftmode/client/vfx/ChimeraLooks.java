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
 * Looks of Meruem, King of the Chimera Ants (Hunter x Hunter): his segmented tail's spear, Photon's turning flashes, the King's aura with his
 * shadow over it, and the rose of fire he rose from (the Poor Man's Rose) when the King of the Chimera Ants awakens.
 */
final class ChimeraLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final int GREEN = 0xC8F070;
	private static final int GREEN_PALE = 0xF0FFD0;
	private static final int GREEN_DEEP = 0x5A8A2A;
	private static final int TAIL_LIT = 0x8AAA52;
	private static final int TAIL_DARK = 0x1A2810;
	private static final int TIP = 0xE8F0D8;
	private static final int SHADOW = 0x0C1408;
	private static final int ROSE = 0xFF2A48;
	private static final int ROSE_DEEP = 0xA0102A;
	private static final int FLAME = 0xFFA040;
	private static final int SMOKE = 0x2A1A18;
	private static final int[] GUARDS = {0xFF8AD8, 0xC8A0FF, 0xFFD060};

	static void submit(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		switch (s.look) {
			case TAIL_PIERCE -> tailPierce(s, ps, c);
			case PHOTON -> photon(s, ps, c);
			case KINGS_AURA -> kingsAura(s, ps, c);
			case KING_ROSE -> kingRose(s, ps, c);
			default -> {
			}
		}
	}

	/**
	 * Tail Pierce: Meruem's tail lunges out from behind the caster along the beam, plated segment after segment, its spear tip leading, and
	 * draws back; a green streak and faint lines as wide as the beam hits ({@link PendragonLooks#BEAM_R}) along it, a flash where it stops.
	 */
	private static void tailPierce(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float len = Math.max(2.0F, s.size);
		Vector3f dir = PendragonLooks.beamDir(s);
		Vector3f side = VfxOut.across(dir);
		Vector3f up = PendragonLooks.beamUp(dir);
		float lunge = ease(age, 0.0F, 3.0F);
		float back = ease(age, 8.0F, 14.0F);
		float reach = Math.max(1.4F, (len - 1.0F) * lunge * (1.0F - 0.85F * back));
		float fade = 1.0F - ease(p, 0.75F, 1.0F);
		float r = PendragonLooks.BEAM_R;
		// from the small of the back, curving up and over into the beam's line
		Vector3f hip = new Vector3f(up).mul(-0.85F).add(new Vector3f(dir).mul(-0.35F));
		List<Vector3f> path = new ArrayList<>();
		for (int i = 0; i <= 4; i++) {
			float t = i / 4.0F;
			Vector3f a = new Vector3f(hip).lerp(new Vector3f(up).mul(0.25F).add(new Vector3f(dir).mul(0.1F)), t);
			Vector3f b = new Vector3f(up).mul(0.25F).add(new Vector3f(dir).mul(0.1F)).lerp(new Vector3f(dir).mul(1.0F), t);
			path.add(a.lerp(b, t));
		}
		int segs = Math.max(2, (int)(reach / 0.42F));
		for (int i = 1; i <= segs; i++) {
			path.add(new Vector3f(dir).mul(1.0F + (reach - 1.0F) * i / segs));
		}
		Vector3f end = path.getLast();
		shade(ps, c, o -> {
			int n = path.size();
			for (int i = 0; i + 1 < n; i++) {
				Vector3f a = path.get(i);
				Vector3f b = path.get(i + 1);
				Vector3f d = new Vector3f(b).sub(a);
				float l = d.length();
				if (l < 1.0E-3F) {
					continue;
				}
				d.div(l);
				float t = (float)i / n;
				float thick = 0.19F - 0.08F * t;
				Vector3f u = VfxOut.across(d);
				Vector3f v = new Vector3f(d).cross(u).normalize();
				o.ellipsoid(new Vector3f(a).lerp(b, 0.5F), new Vector3f(u).mul(thick), new Vector3f(d).mul(l * 0.62F), new Vector3f(v).mul(thick), 4, 8,
					argb(0.97F * fade, TAIL_LIT), argb(0.97F * fade, TAIL_DARK));
			}
			o.blade(end, new Vector3f(end).add(new Vector3f(dir).mul(1.0F)), side, 0.22F, argb(0.97F * fade, TIP), argb(0.97F * fade, 0xFFFFFF));
			o.blade(new Vector3f(end).add(new Vector3f(dir).mul(-0.1F)), new Vector3f(end).add(new Vector3f(dir).mul(0.25F)).add(new Vector3f(side).mul(0.28F)), dir, 0.06F,
				argb(0.97F * fade, TIP), argb(0.97F * fade, TIP));
			o.blade(new Vector3f(end).add(new Vector3f(dir).mul(-0.1F)), new Vector3f(end).add(new Vector3f(dir).mul(0.25F)).add(new Vector3f(side).mul(-0.28F)), dir,
				0.06F, argb(0.97F * fade, TIP), argb(0.97F * fade, TIP));
		});
		glow(ps, c, o -> {
			Vector3f front = new Vector3f(end).add(new Vector3f(dir).mul(1.0F));
			o.glowLine(new Vector3f(dir).mul(0.8F), front, 0.3F, argb(0.3F * fade, GREEN), argb(0.4F * fade, GREEN_PALE));
			o.glowLine(end, front, 0.12F, argb(0.6F * fade, GREEN_PALE), argb(0.9F * fade, 0xFFFFFF));
			if (back <= 0.0F) {
				float ln = front.length();
				for (int k = 0; k < 4; k++) {
					Vector3f off = new Vector3f(k < 2 ? up : side).mul((k % 2 == 0 ? 1 : -1) * r);
					o.glowLine(new Vector3f(dir).mul(0.8F).add(off), new Vector3f(dir).mul(ln).add(off), 0.05F, argb(0.3F * fade, GREEN), argb(0.3F * fade, GREEN));
				}
			}
			float flash = 1.0F - ease(age, 2.0F, 9.0F);
			if (flash > 0.0F && lunge >= 1.0F) {
				o.star(front, s.camRight, s.camUp, 1.1F * flash + 0.2F, 6, 0.2F, s.seed, argb(0.9F * flash, 0xFFFFFF), argb(0.0F, GREEN));
			}
			for (int i = 0; i < 8; i++) {
				float q = (time * 0.15F + hash(s.seed + i)) % 1.0F;
				Vector3f at = new Vector3f(dir).mul(0.8F + (reach) * q).add(new Vector3f(side).mul((hash(i * 3) - 0.5F) * 1.2F)).add(new Vector3f(up).mul((hash(i * 5) - 0.5F) * 1.2F));
				o.glowLine(at, new Vector3f(at).add(new Vector3f(dir).mul(-0.8F)), 0.03F, argb(0.6F * fade, GREEN_PALE), argb(0.0F, GREEN));
			}
		});
	}

	/**
	 * Photon: blades of light flash out round the caster in three turning sweeps as wide as the blast hits, three motes of the Royal Guards'
	 * colours circling with them; a shock ring runs out to the rim and the rim glows on the ground.
	 */
	private static void photon(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float r = Math.max(1.0F, s.size);
		float q = ease(age, 0.0F, 5.0F);
		float fade = 1.0F - ease(p, 0.45F, 1.0F);
		glow(ps, c, o -> {
			for (int g = 0; g < 3; g++) {
				float spin = time * (0.45F + 0.1F * g) * (g % 2 == 0 ? 1 : -1) + g * 2.1F;
				float y = 0.6F + 0.45F * g;
				for (int i = 0; i < 4; i++) {
					float a = spin + Mth.TWO_PI * i / 4;
					Vector3f base = polar(a, 0.4F, y);
					Vector3f tip = polar(a + 0.3F, r * q, Math.max(0.2F, y * 0.4F));
					Vector3f lean = VfxOut.across(new Vector3f(tip).sub(base));
					o.blade(base, tip, new Vector3f(lean).cross(new Vector3f(tip).sub(base)).normalize(), 0.18F, argb(0.85F * fade, 0xFFFFFF), argb(0.4F * fade, GREEN));
					// the sweep it leaves: an arc of light at the rim
					Vector3f prev = null;
					for (int j = 0; j <= 6; j++) {
						float b = a + 0.3F - 0.7F * j / 6.0F;
						Vector3f pt = polar(b, r * q * (0.92F + 0.08F * j / 6.0F), Math.max(0.2F, y * 0.4F));
						if (prev != null) {
							o.glowLine(prev, pt, 0.12F, argb(0.6F * fade * (1.0F - j / 6.0F), GREEN_PALE), argb(0.6F * fade * (1.0F - (j - 1) / 6.0F), GREEN_PALE));
						}
						prev = pt;
					}
				}
			}
			for (int g = 0; g < 3; g++) {
				float a = time * 0.35F + Mth.TWO_PI * g / 3;
				Vector3f at = polar(a, r * 0.5F * q, 1.2F + 0.2F * Mth.sin(time * 0.3F + g));
				o.sprite(at, s.camRight, s.camUp, 0.45F, argb(0.7F * fade, GUARDS[g]));
				o.sprite(at, s.camRight, s.camUp, 0.16F, argb(0.95F * fade, 0xFFFFFF));
			}
			o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, r - 0.3F), r, argb(0.85F * fade, GREEN));
			o.ring(new Vector3f(0, 0.16F, 0), Math.max(0.0F, r - 0.07F), r, argb(0.6F * fade, GREEN_PALE));
			o.ring(new Vector3f(0, 0.17F, 0), Math.max(0.0F, r * q - 0.7F), r * q, argb(0.75F * (1.0F - q * 0.7F) * fade, 0xFFFFFF));
			float flash = 1.0F - ease(age, 0.0F, 7.0F);
			if (flash > 0.0F) {
				o.star(new Vector3f(0, 1.1F, 0), s.camRight, s.camUp, 2.0F * flash + 0.3F, 12, 0.2F, time * 0.1F, argb(0.95F * flash, 0xFFFFFF), argb(0.0F, GREEN));
			}
		});
	}

	/**
	 * King's Aura: the king's aura rolls out over the ground to the edge of what it reaches, a dark green haze swirling in it and pulses
	 * running out; behind the caster rises Meruem's shadow, head crested and swept back, his eyes and the edge of his crest lit green, his
	 * tail curling at his feet. In first person only the ground.
	 */
	private static void kingsAura(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float r = Math.max(1.0F, s.size);
		float wave = ease(age, 0.0F, 12.0F);
		float rise = ease(age, 2.0F, 14.0F);
		float fade = 1.0F - ease(p, 0.75F, 1.0F);
		Vector3f ahead = WitchLooks.ahead(s);
		Vector3f right = VfxOut.across(ahead);
		Vector3f body = new Vector3f(ahead).mul(-1.3F);
		Vector3f head = new Vector3f(body).add(0, 3.9F * rise + 0.5F, 0);
		Vector3f crestTip = new Vector3f(head).add(new Vector3f(ahead).mul(-1.6F)).add(0, 0.9F, 0);
		shade(ps, c, o -> {
			for (int i = 0; i < 20; i++) {
				float a = Mth.TWO_PI * hash(s.seed + i * 3) + time * 0.015F * (i % 2 == 0 ? 1 : -1);
				Vector3f at = polar(a, r * wave * (0.2F + 0.75F * hash(i * 5)), 0.4F);
				o.sprite(at, s.camRight, s.camUp, 1.1F + 0.6F * hash(i * 7), argb(0.28F * fade, SHADOW));
			}
			if (s.selfView || rise <= 0.05F) {
				return;
			}
			int dark = argb(0.6F * fade * rise, SHADOW);
			int dim = argb(0.6F * fade * rise, 0x1E2A14);
			// the shoulders and body, the head and its crest swept back
			// a slim waist widening to broad shoulders, the arms hanging, the long helmet-like head
			o.ellipsoid(new Vector3f(body).add(0, 2.2F * rise, 0), new Vector3f(right).mul(0.55F), new Vector3f(UP).mul(1.2F * rise), new Vector3f(ahead).mul(0.4F), 5,
				10, dim, dark);
			o.ellipsoid(new Vector3f(body).add(0, 3.3F * rise, 0), new Vector3f(right).mul(1.15F), new Vector3f(UP).mul(0.45F * rise), new Vector3f(ahead).mul(0.45F), 4,
				10, dim, dark);
			for (int k = -1; k <= 1; k += 2) {
				Vector3f shoulder = new Vector3f(body).add(new Vector3f(right).mul(k * 1.15F)).add(0, 3.3F * rise, 0);
				o.tube(List.of(shoulder, new Vector3f(body).add(new Vector3f(right).mul(k * 1.35F)).add(0, 2.2F * rise, 0),
					new Vector3f(body).add(new Vector3f(right).mul(k * 1.25F)).add(new Vector3f(ahead).mul(0.3F)).add(0, 1.3F * rise, 0)), 0.28F, 0.2F, 6, dim, dark);
			}
			o.ellipsoid(new Vector3f(head).add(new Vector3f(ahead).mul(-0.25F)), new Vector3f(right).mul(0.45F), new Vector3f(UP).mul(0.6F), new Vector3f(ahead).mul(0.75F),
				5, 10, dim, dark);
			o.blade(new Vector3f(head).add(0, 0.35F, 0), crestTip, right, 0.3F, dim, dark);
			for (int k = -1; k <= 1; k += 2) {
				o.blade(new Vector3f(head).add(new Vector3f(right).mul(k * 0.4F)).add(0, 0.1F, 0),
					new Vector3f(head).add(new Vector3f(right).mul(k * 0.95F)).add(new Vector3f(ahead).mul(-0.7F)).add(0, 0.5F, 0), UP, 0.12F, dim, dark);
			}
		});
		glow(ps, c, o -> {
			o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, r * wave - 0.35F), r * wave, argb(0.8F * fade, GREEN));
			o.ring(new Vector3f(0, 0.16F, 0), Math.max(0.0F, r * wave - 0.07F), r * wave, argb(0.55F * fade, GREEN_PALE));
			for (int i = 0; i < 2; i++) {
				float pulse = (time * 0.035F + i * 0.5F) % 1.0F;
				o.ring(new Vector3f(0, 0.14F, 0), Math.max(0.0F, r * pulse - 0.6F), r * pulse, argb(0.35F * fade * (1.0F - pulse), GREEN));
			}
			if (s.selfView) {
				return;
			}
			for (int i = 0; i < 12; i++) {
				float a = Mth.TWO_PI * i / 12 + hash(s.seed + i);
				o.flame(polar(a, 0.6F, 0.0F), UP, (1.4F + 0.9F * hash(i * 3 + (int)(time / 5))) * wave, 0.4F, time * 0.5F + i, argb(0.35F * fade, GREEN),
					argb(0.0F, GREEN_DEEP));
			}
			if (rise > 0.3F) {
				float k = fade * rise;
				for (int e = -1; e <= 1; e += 2) {
					Vector3f eye = new Vector3f(head).add(new Vector3f(right).mul(e * 0.24F)).add(new Vector3f(ahead).mul(0.5F)).add(0, 0.05F, 0);
					o.diamond(eye, right, UP, 0.3F, 0.08F, argb(k, GREEN_PALE), argb(k, 0xFFFFFF));
					o.sprite(eye, s.camRight, s.camUp, 0.3F, argb(0.5F * k, GREEN));
				}
				o.glowLine(new Vector3f(head).add(0, 0.6F, 0), crestTip, 0.08F, argb(0.6F * k, GREEN), argb(0.2F * k, GREEN));
				// his tail curling round at the shadow's feet
				Vector3f prev = null;
				for (int i = 0; i <= 12; i++) {
					float t = i / 12.0F;
					float a = (float)Mth.atan2(-ahead.z, -ahead.x) + 1.2F + t * 3.6F + 0.2F * Mth.sin(time * 0.1F);
					Vector3f pt = polar(a, 1.6F + 0.6F * t, 0.3F + 1.2F * Mth.sin(Mth.PI * t)).add(new Vector3f(body).mul(0.5F));
					if (prev != null) {
						o.glowLine(prev, pt, 0.18F * (1.0F - t * 0.6F), argb(0.45F * k, GREEN), argb(0.45F * k, GREEN));
					}
					prev = pt;
				}
				o.blade(prev, new Vector3f(prev).add(0, 0.6F, 0), right, 0.1F, argb(0.8F * k, GREEN_PALE), argb(0.8F * k, 0xFFFFFF));
			}
		});
	}

	/**
	 * King of the Chimera Ants: a flash, then a rose of fire opens over the ground, its outer petals reaching the edge of what the blast hits,
	 * and its cloud climbs on a stem of fire into a second rose blooming overhead under rolling smoke.
	 */
	private static void kingRose(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float age = s.age();
		float p = s.progress;
		float time = s.ageInTicks;
		float r = Math.max(1.0F, s.size);
		float bloom = ease(age, 1.0F, 10.0F);
		float rise = ease(age, 4.0F, 26.0F);
		float fade = 1.0F - ease(p, 0.65F, 1.0F);
		float stem = 2.0F + 7.0F * rise;
		Vector3f cap = new Vector3f(0, stem, 0);
		shade(ps, c, o -> {
			for (int i = 0; i < 12; i++) {
				float a = Mth.TWO_PI * i / 12 + hash(s.seed + i) + time * 0.01F;
				Vector3f at = new Vector3f(cap).add(polar(a, r * 0.45F * rise * (0.7F + 0.3F * hash(i * 3)), 0.4F * (hash(i * 5) - 0.3F)));
				o.sprite(at, s.camRight, s.camUp, 1.0F + 0.8F * rise, argb(0.45F * fade * rise, SMOKE));
			}
		});
		glow(ps, c, o -> {
			Vector3f g = new Vector3f(0, 0.18F, 0);
			// the rose on the ground: two rings of petals, the outer ones reaching the rim
			for (int layer = 0; layer < 3; layer++) {
				int petals = layer == 0 ? 7 : layer == 1 ? 6 : 5;
				float len = r * (layer == 0 ? 1.0F : layer == 1 ? 0.68F : 0.38F) * bloom;
				float turn = layer * 0.4F + hash(s.seed) * 2.0F;
				for (int i = 0; i < petals; i++) {
					float a = turn + Mth.TWO_PI * i / petals;
					Vector3f base = new Vector3f(g).add(0, layer * 0.05F, 0);
					Vector3f tip = polar(a, len, 0.18F + (layer == 0 ? 0.0F : layer == 1 ? 0.8F : 1.5F) * bloom);
					Vector3f across = polar(a + Mth.HALF_PI, 1.0F, 0.0F);
					petal(o, base, tip, across, len * (layer == 0 ? 0.36F : 0.42F), 0.35F + 0.3F * layer, argb(0.55F * fade, layer == 0 ? ROSE_DEEP : ROSE),
						argb(0.7F * fade, layer == 2 ? FLAME : ROSE), argb(0.75F * fade, layer == 0 ? FLAME : 0xFFC0A0));
				}
			}
			o.ring(new Vector3f(0, 0.15F, 0), Math.max(0.0F, r - 0.35F), r, argb(0.85F * fade, FLAME));
			o.ring(new Vector3f(0, 0.16F, 0), Math.max(0.0F, r - 0.08F), r, argb(0.6F * fade, 0xFFE0B0));
			// the stem and the rose of the cloud
			o.glowLine(g, cap, 0.9F + 0.6F * (1.0F - rise), argb(0.45F * fade, FLAME), argb(0.3F * fade, ROSE));
			o.glowLine(g, cap, 0.3F, argb(0.7F * fade, 0xFFE0B0), argb(0.4F * fade, FLAME));
			for (int layer = 0; layer < 2; layer++) {
				int petals = layer == 0 ? 6 : 5;
				float len = r * (layer == 0 ? 0.45F : 0.26F) * rise;
				for (int i = 0; i < petals; i++) {
					float a = time * 0.02F + layer * 0.5F + Mth.TWO_PI * i / petals;
					Vector3f tip = new Vector3f(cap).add(polar(a, len, (layer == 0 ? 0.5F : 1.0F) * rise));
					petal(o, new Vector3f(cap).add(0, layer * 0.15F, 0), tip, polar(a + Mth.HALF_PI, 1.0F, 0.0F), len * 0.42F, 0.5F, argb(0.6F * fade * rise, layer == 0 ? ROSE : ROSE_DEEP),
						argb(0.75F * fade * rise, layer == 0 ? FLAME : ROSE), argb(0.8F * fade * rise, 0xFFE0B0));
				}
			}
			float flash = 1.0F - ease(age, 0.0F, 8.0F);
			if (flash > 0.0F) {
				o.sprite(new Vector3f(0, 1.5F, 0), s.camRight, s.camUp, r * 1.2F * flash, argb(0.9F * flash, 0xFFFFFF));
				o.star(new Vector3f(0, 1.5F, 0), s.camRight, s.camUp, r * flash + 0.5F, 12, 0.25F, s.seed, argb(flash, 0xFFFFFF), argb(0.0F, FLAME));
			}
		});
	}

	/**
	 * A rose petal from {@code base} to {@code tip}: narrow at the base, widest ({@code width}) past its middle and rounded at the end, its
	 * edges curling up by {@code curl}; {@code cBase} to {@code cTip}, lit rim {@code rim}. Glow buffer.
	 */
	private static void petal(final VfxOut o, final Vector3f base, final Vector3f tip, final Vector3f across, final float width, final float curl, final int cBase,
		final int cTip, final int rim) {
		int n = 7;
		Vector3f prevL = null;
		Vector3f prevR = null;
		int prevC = cBase;
		for (int i = 0; i <= n; i++) {
			float u = (float)i / n;
			float w = u < 0.6F ? width * Mth.sin(Mth.HALF_PI * u / 0.6F) : width * (float)Math.sqrt(Math.max(0.0F, 1.0F - (u - 0.6F) * (u - 0.6F) / 0.16F));
			Vector3f m = new Vector3f(base).lerp(tip, u);
			Vector3f l = new Vector3f(across).mul(-w).add(m).add(0, curl * w, 0);
			Vector3f r = new Vector3f(across).mul(w).add(m).add(0, curl * w, 0);
			int col = VfxOut.lerp(cBase, cTip, u);
			if (prevL != null) {
				o.quad(prevL, prevR, r, l, prevC, prevC, col, col);
				o.glowLine(prevL, l, 0.035F, rim, rim);
				o.glowLine(prevR, r, 0.035F, rim, rim);
			}
			prevL = l;
			prevR = r;
			prevC = col;
		}
	}

	private ChimeraLooks() {
	}
}
