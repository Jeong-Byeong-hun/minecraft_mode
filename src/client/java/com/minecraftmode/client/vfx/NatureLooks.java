package com.minecraftmode.client.vfx;

import static com.minecraftmode.client.vfx.SkillVfxRenderer.glow;
import static com.minecraftmode.client.vfx.SkillVfxRenderer.shade;
import static com.minecraftmode.client.vfx.VfxOut.argb;
import static com.minecraftmode.client.vfx.VfxOut.ease;
import static com.minecraftmode.client.vfx.VfxOut.hash;
import static com.minecraftmode.client.vfx.VfxOut.polar;

import com.minecraftmode.client.vfx.SkillVfxRenderer.State;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/** Senbonzakura's petals and swords, Hyorinmaru's ice, Ryujin Jakka's fire and the drowning abyss. */
final class NatureLooks {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final int ICE_BODY = 0xD6F3FF;
	private static final int ICE_EDGE = 0x9FE6FF;
	private static final int FLAME_CORE = 0xFFE7A0;
	private static final int FLAME_TIP = 0xFF3A00;

	// ------------------------------------------------------------ Senbonzakura

	/** A Thousand Blades: six streams of blade petals wound round the caster, rising and falling as they circle. */
	static void petalStorm(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.2F);
		float fade = 1.0F - ease(p, 0.75F, 1.0F);
		float time = s.ageInTicks;
		float size = s.size;
		glow(ps, c, o -> {
			int streams = 6;
			int per = 56;
			for (int k = 0; k < streams; k++) {
				float base = Mth.TWO_PI * k / streams;
				for (int i = 0; i < per; i++) {
					int id = k * per + i;
					float u = (float)i / per;
					// each stream is a loose cloud of petals, not a rope: every petal strays a little from its line
					float angle = base + time * 0.32F + u * 2.4F + (hash(id) - 0.5F) * 0.25F;
					float r = size * open * (0.35F + 0.6F * (0.5F + 0.5F * Mth.sin(u * 5.0F + k * 1.7F))) + (hash(id * 3) - 0.5F) * 1.1F;
					float y = 0.4F + 1.9F * (0.5F + 0.5F * Mth.sin(u * Mth.TWO_PI + k + time * 0.15F)) + (hash(id * 5) - 0.5F) * 0.8F;
					Vector3f at = polar(angle, Math.max(0.5F, r), y);
					// petals lie along the stream (the tangent of the circle with a little climb), each tumbling round it
					Vector3f along = new Vector3f(-Mth.sin(angle), 0.25F * Mth.cos(u * Mth.TWO_PI + k), Mth.cos(angle));
					float tumble = time * (0.3F + 0.4F * hash(id * 7)) + hash(id * 11) * Mth.TWO_PI;
					Vector3f flat = new Vector3f(along).cross(UP).normalize().mul(Mth.cos(tumble)).add(0, Mth.sin(tumble), 0);
					float a = fade * (0.35F + 0.65F * Mth.sin(Mth.PI * u));
					int hue = (i + k) % 3 == 0 ? 0xFFFFFF : s.color;
					o.diamond(at, along, flat, 0.55F, 0.2F, argb(0.15F * a, hue), argb(0.95F * a, hue));
				}
			}
			o.ring(new Vector3f(0, 0.15F, 0), size * open * 0.8F, size * open, argb(0.45F * fade, s.color));
		});
	}

	/** Gokei: petals close in from all sides into a sphere round the caster, crush inward with a flash and scatter. */
	static void petalSphere(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float close = ease(p, 0.0F, 0.5F);
		float burst = ease(p, 0.52F, 1.0F);
		float radius = s.size * (1.35F - 0.7F * close) + s.size * 0.9F * burst;
		float fade = 1.0F - burst;
		float time = s.ageInTicks;
		glow(ps, c, o -> {
			int n = 260;
			Vector3f center = new Vector3f(0, 1.0F, 0);
			for (int i = 0; i < n; i++) {
				// a Fibonacci sphere, turning faster as it closes
				float yy = 1.0F - 2.0F * (i + 0.5F) / n;
				float rr = (float)Math.sqrt(1.0F - yy * yy);
				float lon = i * 2.39996F + time * (0.15F + 0.25F * close) * (i % 2 == 0 ? 1 : -1);
				Vector3f d = new Vector3f(Mth.cos(lon) * rr, yy, Mth.sin(lon) * rr);
				Vector3f at = new Vector3f(d).mul(radius).add(center);
				if (at.y < 0.05F) {
					continue;
				}
				Vector3f along = new Vector3f(-Mth.sin(lon), 0.0F, Mth.cos(lon));
				int hue = i % 4 == 0 ? 0xFFFFFF : s.color;
				o.diamond(at, along, d, 0.6F, 0.22F, argb(0.15F * fade, hue), argb(0.9F * fade, hue));
			}
			// the crush: a flash in the middle
			float flash = Mth.clamp(1.0F - Math.abs(p - 0.5F) * 6.0F, 0.0F, 1.0F);
			if (flash > 0.0F) {
				o.sprite(center, s.camRight, s.camUp, s.size * 0.8F, argb(0.8F * flash, s.color));
				o.sprite(center, s.camRight, s.camUp, s.size * 0.35F, argb(flash, 0xFFFFFF));
			}
		});
	}

	/** Senkei: three rows of great swords rising round the field, points up, a ring of pink light on the ground. */
	static void senkei(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.88F, 1.0F);
		float age = s.age();
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			o.ring(ground, s.size * 0.9F, s.size * 1.45F, argb(0.5F * fade, s.color));
			for (int row = 0; row < 3; row++) {
				int count = 26 + row * 4;
				float r = s.size * (1.0F + row * 0.17F);
				for (int i = 0; i < count; i++) {
					float a = Mth.TWO_PI * (i + row * 0.5F) / count;
					// each sword rises out of the ground in turn
					float rise = ease(age, 2.0F + 6.0F * hash(row * 97 + i), 9.0F + 6.0F * hash(row * 97 + i));
					if (rise <= 0.0F) {
						continue;
					}
					float hover = 0.15F * Mth.sin(s.ageInTicks * 0.08F + i);
					float tall = 2.6F + row * 0.5F;
					Vector3f base = polar(a, r, -1.0F + (1.3F + hover) * rise);
					Vector3f tip = new Vector3f(base).add(0, tall, 0);
					Vector3f tangent = new Vector3f(-Mth.sin(a), 0, Mth.cos(a));
					float alpha = fade * (0.55F + 0.45F * rise);
					o.blade(new Vector3f(base).add(0, 0.55F, 0), tip, tangent, 0.2F, argb(0.85F * alpha, 0xFFFFFF), argb(0.6F * alpha, s.color));
					// guard and grip
					o.bar(new Vector3f(base).add(new Vector3f(tangent).mul(-0.32F)).add(0, 0.55F, 0), new Vector3f(base).add(new Vector3f(tangent).mul(0.32F)).add(0, 0.55F, 0),
						0.05F, 0.05F, argb(0.9F * alpha, s.color), argb(0.9F * alpha, s.color));
					o.bar(base, new Vector3f(base).add(0, 0.5F, 0), 0.05F, 0.05F, argb(0.6F * alpha, s.color), argb(0.6F * alpha, s.color));
				}
			}
		});
	}

	// ------------------------------------------------------------ Hyorinmaru

	/** Ryusenka: an ice flower bursts open round the stabbed body, crystals spearing out of the ground beneath it. */
	static void iceFlower(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float bloom = ease(p, 0.0F, 0.2F);
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float height = Math.max(1.0F, s.size);
		float width = Math.max(0.6F, s.arg);
		Vector3f heart = new Vector3f(0, height * 0.55F, 0);
		java.util.function.Consumer<VfxOut> shape = o -> {
			int body = o.glow ? argb(0.9F * fade, ICE_EDGE) : argb(0.42F * fade, ICE_BODY);
			int tip = o.glow ? argb(fade, 0xFFFFFF) : argb(0.6F * fade, 0xFFFFFF);
			// eight petals spread round the body, tilted up like a lotus
			for (int i = 0; i < 8; i++) {
				float a = Mth.TWO_PI * i / 8 + 0.3F;
				Vector3f d = new Vector3f(Mth.cos(a), 0.55F + 0.25F * (i % 2), Mth.sin(a)).normalize();
				Vector3f from = new Vector3f(d).mul(width * 0.3F).add(heart);
				o.crystal(from, d, (0.9F + 0.4F * (i % 2)) * bloom * width * 1.4F, 0.16F * width + 0.05F, 4, body, tip);
			}
			// spikes out of the ground, leaning out
			for (int i = 0; i < 6; i++) {
				float a = Mth.TWO_PI * i / 6 + 0.9F;
				Vector3f base = polar(a, width * 0.9F, 0.0F);
				Vector3f d = new Vector3f(Mth.cos(a) * 0.35F, 1.0F, Mth.sin(a) * 0.35F);
				o.crystal(base, d, height * (0.5F + 0.3F * hash(i)) * bloom, 0.13F, 5, body, tip);
			}
		};
		shade(ps, c, shape);
		glow(ps, c, o -> {
			shape.accept(o);
			o.sprite(heart, s.camRight, s.camUp, width * 1.5F * bloom, argb(0.4F * fade, ICE_EDGE));
			o.ring(new Vector3f(0, 0.15F, 0), 0.2F, width * 2.2F * bloom, argb(0.4F * fade, ICE_BODY));
		});
	}

	/** Sennen Hyoro: a ring of ice pillars rising round the aimed spot and leaning in over it. */
	static void icePrison(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.8F, 1.0F);
		float age = s.age();
		java.util.function.Consumer<VfxOut> pillars = o -> {
			int n = 12;
			for (int i = 0; i < n; i++) {
				float a = Mth.TWO_PI * i / n;
				float rise = ease(age, i * 0.6F, i * 0.6F + 6.0F);
				if (rise <= 0.0F) {
					continue;
				}
				Vector3f base = polar(a, s.size * 1.05F, -0.3F);
				// leaning in over the middle
				Vector3f d = new Vector3f(-Mth.cos(a) * 0.42F, 1.0F, -Mth.sin(a) * 0.42F);
				int body = o.glow ? argb(0.85F * fade, ICE_EDGE) : argb(0.45F * fade, ICE_BODY);
				o.crystal(base, d, (4.0F + 1.2F * hash(i * 5)) * rise, 0.42F, 6, body, o.glow ? argb(fade, 0xFFFFFF) : body);
			}
		};
		shade(ps, c, pillars);
		glow(ps, c, o -> {
			pillars.accept(o);
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			o.disc(ground, s.size * 1.2F, argb(0.35F * fade, 0xFFFFFF), argb(0.0F, ICE_EDGE));
			o.ring(ground, s.size * 0.95F, s.size * 1.25F, argb(0.6F * fade, ICE_EDGE));
		});
	}

	/** Daiguren's wings: a fan of long ice feathers each side of the back, beating slowly. */
	static void iceWings(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.06F);
		float fade = 1.0F - ease(p, 0.9F, 1.0F);
		float yaw = -s.bodyYaw * Mth.DEG_TO_RAD;
		ps.pushPose();
		ps.translate(0.0F, 1.3F, 0.0F);
		ps.rotate(com.mojang.math.Axis.YP.rotation(yaw));
		// in the body's frame: +z ahead, +x to its left
		float beat = 0.18F * Mth.sin(s.ageInTicks * 0.25F);
		java.util.function.Consumer<VfxOut> wings = o -> {
			Vector3f root = new Vector3f(0, 0, -0.3F);
			int body = o.glow ? argb(0.8F * fade, ICE_EDGE) : argb(0.45F * fade, ICE_BODY);
			int tip = o.glow ? argb(fade, 0xFFFFFF) : argb(0.55F * fade, 0xFFFFFF);
			for (int w = -1; w <= 1; w += 2) {
				for (int k = 0; k < 7; k++) {
					float a = (-0.35F + 1.25F * k / 6.0F) * open + beat;
					// feathers sweep out sideways and up, and back
					Vector3f d = new Vector3f(w * Mth.cos(a), Mth.sin(a) + 0.15F, -0.45F - 0.1F * k).normalize();
					float len = (1.1F + 1.5F * Mth.sin(Mth.PI * (k + 1) / 8.0F)) * (0.3F + 0.7F * open);
					Vector3f from = new Vector3f(w * 0.12F, 0.1F, 0).add(root);
					o.crystal(from, d, len, 0.11F, 4, body, tip);
				}
			}
		};
		if (!s.selfView) {
			shade(ps, c, wings);
		}
		glow(ps, c, o -> {
			if (!s.selfView) {
				wings.accept(o);
			}
		});
		ps.popPose();
	}

	/**
	 * Hyoten Hyakkaso: a ring of cloud opens overhead, snow drifts down out of it and ice flowers bloom across the ground where it
	 * lands.
	 */
	static void iceBloom(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float fade = 1.0F - ease(p, 0.88F, 1.0F);
		float open = ease(p, 0.0F, 0.15F);
		float age = s.age();
		float top = 9.0F;
		shade(ps, c, o -> {
			// the cloud: puffs in a ring round the hole the snow falls through
			for (int i = 0; i < 40; i++) {
				float a = Mth.TWO_PI * i / 40 + hash(i) * 0.3F;
				float r = s.size * (0.75F + 0.5F * hash(i * 3));
				Vector3f at = polar(a, r * open, top + hash(i * 7) * 1.2F);
				o.sprite(at, s.camRight, s.camUp, 1.6F + hash(i * 11), argb(0.4F * fade * open, 0xA8B4C2));
			}
		});
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			o.disc(ground, s.size, argb(0.3F * fade, 0xFFFFFF), argb(0.0F, ICE_EDGE));
			o.ring(ground, s.size * 0.9F, s.size * 1.05F, argb(0.5F * fade, ICE_EDGE));
			// snowflakes falling
			for (int i = 0; i < 90; i++) {
				float r = s.size * (float)Math.sqrt(hash(i * 13));
				float a = Mth.TWO_PI * hash(i * 17);
				float fall = (s.ageInTicks * (0.12F + 0.06F * hash(i)) + hash(i * 19) * top) % top;
				Vector3f at = polar(a + s.ageInTicks * 0.01F, r, top - fall);
				o.square(at, s.camRight, s.camUp, 0.08F, s.ageInTicks * 0.2F + i, argb(0.9F * fade, 0xFFFFFF));
			}
			// ice flowers blooming one after another
			for (int i = 0; i < 16; i++) {
				float r = s.size * 0.9F * (float)Math.sqrt(hash(i * 23 + 1));
				float a = Mth.TWO_PI * hash(i * 29 + 2);
				float bloom = ease(age, 4.0F + 50.0F * hash(i * 31), 14.0F + 50.0F * hash(i * 31));
				if (bloom <= 0.0F) {
					continue;
				}
				flower(o, polar(a, r, 0.1F), (0.6F + 0.5F * hash(i)) * bloom, a, argb(0.85F * fade, ICE_EDGE), argb(fade, 0xFFFFFF));
			}
		});
	}

	/** A six-petalled ice flower lying open on the ground, petals tipped up. */
	private static void flower(final VfxOut o, final Vector3f at, final float size, final float turn, final int edge, final int core) {
		for (int k = 0; k < 6; k++) {
			float a = turn + Mth.TWO_PI * k / 6;
			Vector3f d = new Vector3f(Mth.cos(a), 0.55F, Mth.sin(a)).normalize();
			Vector3f tip = new Vector3f(d).mul(size).add(at);
			o.blade(at, tip, new Vector3f(-Mth.sin(a), 0, Mth.cos(a)), size * 0.28F, core, edge);
		}
		o.bar(at, new Vector3f(at).add(0, size * 0.5F, 0), 0.04F, 0.0F, core, edge);
	}

	// ------------------------------------------------------------ Ryujin Jakka

	/** Reduce All Creation to Ash: a ring of flame tongues bursting outward, the middle flaring up. */
	static void flameBurst(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float out = ease(p, 0.0F, 0.45F);
		float fade = 1.0F - ease(p, 0.4F, 1.0F);
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			o.disc(ground, s.size * out, argb(0.5F * fade, 0xFFB050), argb(0.0F, FLAME_TIP));
			o.ring(ground, s.size * out * 0.85F, s.size * out * 1.05F, argb(0.9F * fade, 0xFF7A20));
			int n = 34;
			for (int i = 0; i < n; i++) {
				float a = Mth.TWO_PI * (i + 0.5F * hash(i)) / n;
				float r = s.size * out * (0.75F + 0.3F * hash(i * 3));
				float h = (1.2F + 2.4F * hash(i * 7)) * Mth.sin(Mth.PI * Math.min(1.0F, p * 1.6F));
				if (h > 0.05F) {
					o.flame(polar(a, r, 0.05F), new Vector3f(Mth.cos(a) * 0.3F, 1, Mth.sin(a) * 0.3F), h, 0.7F, s.ageInTicks * 0.7F + i, argb(0.9F * fade, FLAME_CORE),
						argb(0.0F, FLAME_TIP));
				}
			}
			// the middle flaring up
			float column = Mth.sin(Mth.PI * Math.min(1.0F, p * 1.4F));
			for (int i = 0; i < 6; i++) {
				float a = Mth.TWO_PI * i / 6;
				o.flame(polar(a, 0.4F, 0.05F), UP, 3.5F * column, 1.0F, s.ageInTicks * 0.8F + i * 2, argb(0.8F * fade, 0xFFFFFF), argb(0.0F, 0xFF6A00));
			}
		});
	}

	/** Jokaku Enjo: a castle wall of fire pillars standing round the caster, a burning ring at its foot. */
	static void flameWall(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float rise = ease(p, 0.0F, 0.08F);
		float fade = 1.0F - ease(p, 0.9F, 1.0F);
		float k = rise * fade;
		glow(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.15F, 0);
			o.ring(ground, s.size * 0.85F, s.size * 1.1F, argb(0.85F * k, 0xFF7A20));
			o.wall(ground, s.size, 0.0F, 3.5F * k, argb(0.4F * k, 0xFF8A30), argb(0.0F, FLAME_TIP));
			int n = 40;
			for (int i = 0; i < n; i++) {
				float a = Mth.TWO_PI * i / n;
				float flicker = 0.8F + 0.2F * Mth.sin(s.ageInTicks * 0.9F + i * 2.3F);
				float h = (3.5F + 2.5F * hash(i * 5)) * k * flicker;
				o.flame(polar(a, s.size * (0.97F + 0.06F * hash(i)), 0.05F), UP, h, 1.0F, s.ageInTicks * 0.45F + i * 1.3F, argb(0.85F * k, FLAME_CORE), argb(0.0F, FLAME_TIP));
			}
		});
	}

	// ------------------------------------------------------------ Karamatsu Shinju

	/** Drowning Abyss: a pool of dark water with a whirl turning in it and rings of waves running out, a swell rising at its edge. */
	static void abyss(final State s, final PoseStack ps, final SubmitNodeCollector c) {
		float p = s.progress;
		float open = ease(p, 0.0F, 0.12F);
		float fade = 1.0F - ease(p, 0.88F, 1.0F);
		float r = s.size * open;
		float time = s.ageInTicks;
		shade(ps, c, o -> {
			Vector3f ground = new Vector3f(0, 0.12F, 0);
			o.disc(ground, r, argb(0.85F * fade, 0x040A1E), argb(0.55F * fade, 0x0A2A55));
			// the swell at the edge, rising and falling
			float swell = 1.2F + 0.5F * Mth.sin(time * 0.3F);
			o.wall(ground, r, 0.0F, swell * open, argb(0.55F * fade, 0x123E78), argb(0.0F, 0x2A6AB0));
		});
		glow(ps, c, o -> {
			Vector3f surface = new Vector3f(0, 0.18F, 0);
			// whirl arms
			int arms = 5;
			for (int k = 0; k < arms; k++) {
				Vector3f prev = null;
				for (int i = 0; i <= 20; i++) {
					float u = i / 20.0F;
					float a = Mth.TWO_PI * k / arms + u * 3.4F - time * 0.18F;
					Vector3f at = polar(a, r * u, 0.18F);
					if (prev != null) {
						o.glowLine(prev, at, 0.18F, argb(0.6F * fade * u, 0x3AB0FF), argb(0.6F * fade * u, 0x3AB0FF));
					}
					prev = at;
				}
			}
			// waves running out
			for (int w = 0; w < 3; w++) {
				float q = ((time * 0.03F) + w / 3.0F) % 1.0F;
				o.ring(surface, r * q, r * q + 0.35F, argb(0.5F * fade * (1 - q), 0x8AD8FF));
			}
			o.ring(surface, r - 0.25F, r + 0.1F, argb(0.6F * fade, 0xFF5AA0));
			o.sprite(new Vector3f(0, 0.3F, 0), s.camRight, s.camUp, 1.2F, argb(0.35F * fade, 0x3AB0FF));
		});
	}

	private NatureLooks() {
	}
}
