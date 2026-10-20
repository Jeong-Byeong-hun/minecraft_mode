package com.minecraftmode.client.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Shapes for drawn skill looks, written into one buffer: {@link VfxDraw#GLOW} (additive light, culled, so every quad is written both
 * ways) or {@link VfxDraw#SHADE} (translucent, both sides already). All points are relative to the pose.
 */
final class VfxOut {
	private static final Vector3fc UP = new Vector3f(0.0F, 1.0F, 0.0F);

	final PoseStack.Pose pose;
	final VertexConsumer buffer;
	final boolean glow;

	VfxOut(final PoseStack.Pose pose, final VertexConsumer buffer, final boolean glow) {
		this.pose = pose;
		this.buffer = buffer;
		this.glow = glow;
	}

	void quad(final Vector3fc a, final Vector3fc b, final Vector3fc c, final Vector3fc d, final int ca, final int cb, final int cc, final int cd) {
		this.vertex(a, ca);
		this.vertex(b, cb);
		this.vertex(c, cc);
		this.vertex(d, cd);
		if (this.glow) {
			this.vertex(d, cd);
			this.vertex(c, cc);
			this.vertex(b, cb);
			this.vertex(a, ca);
		}
	}

	void quad(final Vector3fc a, final Vector3fc b, final Vector3fc c, final Vector3fc d, final int color) {
		this.quad(a, b, c, d, color, color, color, color);
	}

	void tri(final Vector3fc a, final Vector3fc b, final Vector3fc c, final int ca, final int cb, final int cc) {
		this.quad(a, b, c, c, ca, cb, cc, cc);
	}

	private void vertex(final Vector3fc p, final int color) {
		this.buffer.addVertex(this.pose, p.x(), p.y(), p.z()).setColor(color);
	}

	// ------------------------------------------------------------ axes

	/** A unit vector square to {@code d} (horizontal when it can be). */
	static Vector3f across(final Vector3fc d) {
		Vector3f side = new Vector3f(d).cross(UP);
		if (side.lengthSquared() < 1.0E-5F) {
			side.set(1.0F, 0.0F, 0.0F);
		}
		return side.normalize();
	}

	/** Unit vector at {@code angle} round the y axis (0 = +x, quarter turn = +z) scaled by {@code r}, at height {@code y}. */
	static Vector3f polar(final float angle, final float r, final float y) {
		return new Vector3f(Mth.cos(angle) * r, y, Mth.sin(angle) * r);
	}

	// ------------------------------------------------------------ lines and blades

	/** A solid bar from {@code a} to {@code b}, half as wide as {@code wa}/{@code wb} at each end, as two crossed quads. */
	void bar(final Vector3fc a, final Vector3fc b, final float wa, final float wb, final int ca, final int cb) {
		Vector3f d = new Vector3f(b).sub(a);
		if (d.lengthSquared() < 1.0E-8F) {
			return;
		}
		d.normalize();
		Vector3f u = across(d);
		Vector3f v = new Vector3f(d).cross(u).normalize();
		this.plane(a, b, u, wa, wb, ca, cb);
		this.plane(a, b, v, wa, wb, ca, cb);
	}

	private void plane(final Vector3fc a, final Vector3fc b, final Vector3fc u, final float wa, final float wb, final int ca, final int cb) {
		this.quad(new Vector3f(u).mul(-wa).add(a), new Vector3f(u).mul(wa).add(a), new Vector3f(u).mul(wb).add(b), new Vector3f(u).mul(-wb).add(b), ca, ca, cb, cb);
	}

	/** A glowing line: bright along the middle, fading to nothing {@code width} out on both sides, seen from every side. */
	void glowLine(final Vector3fc a, final Vector3fc b, final float width, final int ca, final int cb) {
		Vector3f d = new Vector3f(b).sub(a);
		if (d.lengthSquared() < 1.0E-8F) {
			return;
		}
		d.normalize();
		Vector3f u = across(d);
		Vector3f v = new Vector3f(d).cross(u).normalize();
		int ea = ca & 0xFFFFFF;
		int eb = cb & 0xFFFFFF;
		for (Vector3f axis : new Vector3f[] {u, v}) {
			Vector3f off = new Vector3f(axis).mul(width);
			this.quad(a, new Vector3f(a).add(off), new Vector3f(b).add(off), b, ca, ea, eb, cb);
			this.quad(a, new Vector3f(a).sub(off), new Vector3f(b).sub(off), b, ca, ea, eb, cb);
		}
	}

	/**
	 * A blade from {@code base} to {@code tip}: a long diamond in the plane of {@code side}, widest ({@code width}) a fifth of the way
	 * up, with a thinner crossed fin so it shows edge on.
	 */
	void blade(final Vector3fc base, final Vector3fc tip, final Vector3fc side, final float width, final int cBase, final int cTip) {
		Vector3f d = new Vector3f(tip).sub(base);
		Vector3f mid = new Vector3f(d).mul(0.2F).add(base);
		Vector3f s = new Vector3f(side).normalize(width);
		int cMid = lerp(cBase, cTip, 0.2F);
		this.quad(base, new Vector3f(mid).add(s), tip, new Vector3f(mid).sub(s), cBase, cMid, cTip, cMid);
		Vector3f fin = new Vector3f(d).cross(s).normalize(width * 0.35F);
		this.quad(base, new Vector3f(mid).add(fin), tip, new Vector3f(mid).sub(fin), cBase, cMid, cTip, cMid);
	}

	/** A flat diamond centred on {@code center}, {@code length} along {@code dir}, {@code width} along {@code side} (petals, shards). */
	void diamond(final Vector3fc center, final Vector3fc dir, final Vector3fc side, final float length, final float width, final int cEnds, final int cMid) {
		Vector3f l = new Vector3f(dir).normalize(length / 2);
		Vector3f w = new Vector3f(side).normalize(width / 2);
		this.quad(new Vector3f(center).sub(l), new Vector3f(center).add(w), new Vector3f(center).add(l), new Vector3f(center).sub(w), cEnds, cMid, cEnds, cMid);
	}

	// ------------------------------------------------------------ camera-facing

	/** A soft round sprite facing the camera ({@code right}/{@code up} from its orientation): {@code color} in the middle, clear at {@code r}. */
	void sprite(final Vector3fc c, final Vector3fc right, final Vector3fc up, final float r, final int color) {
		int edge = color & 0xFFFFFF;
		Vector3f prev = null;
		int n = 8;
		for (int i = 0; i <= n; i++) {
			float t = Mth.TWO_PI * i / n;
			Vector3f p = new Vector3f(right).mul(Mth.cos(t) * r).add(new Vector3f(up).mul(Mth.sin(t) * r)).add(c);
			if (prev != null) {
				this.tri(c, prev, p, color, edge, edge);
			}
			prev = p;
		}
	}

	/** A flat square facing the camera, {@code r} from its middle to each side. */
	void square(final Vector3fc c, final Vector3fc right, final Vector3fc up, final float r, final float spin, final int color) {
		Vector3f a = new Vector3f(right).mul(Mth.cos(spin) * r).add(new Vector3f(up).mul(Mth.sin(spin) * r));
		Vector3f b = new Vector3f(right).mul(-Mth.sin(spin) * r).add(new Vector3f(up).mul(Mth.cos(spin) * r));
		this.quad(new Vector3f(c).add(a), new Vector3f(c).add(b), new Vector3f(c).sub(a), new Vector3f(c).sub(b), color);
	}

	// ------------------------------------------------------------ fire

	/**
	 * A flame tongue rising {@code height} from {@code base} along {@code up}, {@code width} across at the bottom and pointed at the
	 * top, swaying with {@code phase}: two crossed planes, {@code cBase} at the root fading to {@code cTip}.
	 */
	void flame(final Vector3fc base, final Vector3fc up, final float height, final float width, final float phase, final int cBase, final int cTip) {
		Vector3f u = new Vector3f(up).normalize();
		Vector3f s1 = across(u);
		Vector3f s2 = new Vector3f(u).cross(s1).normalize();
		int steps = 5;
		for (Vector3f side : new Vector3f[] {s1, s2}) {
			Vector3f l0 = null;
			Vector3f r0 = null;
			int c0 = cBase;
			for (int i = 0; i <= steps; i++) {
				float t = (float)i / steps;
				float sway = Mth.sin(phase + t * 3.2F) * 0.22F * height * t * t;
				float w = width * 0.5F * (1.0F - t) * (0.6F + 0.4F * Mth.sin(t * Mth.PI + 0.4F));
				Vector3f mid = new Vector3f(u).mul(height * t).add(new Vector3f(s1).mul(sway)).add(base);
				Vector3f l = new Vector3f(side).mul(-w).add(mid);
				Vector3f r = new Vector3f(side).mul(w).add(mid);
				int c = lerp(cBase, cTip, t);
				if (l0 != null) {
					this.quad(l0, r0, r, l, c0, c0, c, c);
				}
				l0 = l;
				r0 = r;
				c0 = c;
			}
		}
	}

	// ------------------------------------------------------------ ground

	/** A flat ring at height {@code y} around {@code center}, from {@code inner} to {@code outer}, brightest in the middle of its width. */
	void ring(final Vector3fc center, final float inner, final float outer, final int color) {
		int n = Mth.clamp((int)(outer * 10), 16, 64);
		float mid = (inner + outer) / 2;
		int edge = color & 0xFFFFFF;
		for (int i = 0; i < n; i++) {
			float t0 = Mth.TWO_PI * i / n;
			float t1 = Mth.TWO_PI * (i + 1) / n;
			Vector3f i0 = polar(t0, inner, 0).add(center);
			Vector3f i1 = polar(t1, inner, 0).add(center);
			Vector3f m0 = polar(t0, mid, 0).add(center);
			Vector3f m1 = polar(t1, mid, 0).add(center);
			Vector3f o0 = polar(t0, outer, 0).add(center);
			Vector3f o1 = polar(t1, outer, 0).add(center);
			this.quad(i0, m0, m1, i1, edge, color, color, edge);
			this.quad(m0, o0, o1, m1, color, edge, edge, color);
		}
	}

	/** A flat disc at {@code center}: {@code core} in the middle to {@code rim} at {@code radius}. */
	void disc(final Vector3fc center, final float radius, final int core, final int rim) {
		int n = Mth.clamp((int)(radius * 8), 16, 48);
		for (int i = 0; i < n; i++) {
			float t0 = Mth.TWO_PI * i / n;
			float t1 = Mth.TWO_PI * (i + 1) / n;
			this.tri(center, polar(t0, radius, 0).add(center), polar(t1, radius, 0).add(center), core, rim, rim);
		}
	}

	/** An upright band round {@code center} from {@code y0} to {@code y1} high at {@code radius}: {@code c0} at the bottom, {@code c1} at the top. */
	void wall(final Vector3fc center, final float radius, final float y0, final float y1, final int c0, final int c1) {
		int n = Mth.clamp((int)(radius * 8), 16, 64);
		for (int i = 0; i < n; i++) {
			float t0 = Mth.TWO_PI * i / n;
			float t1 = Mth.TWO_PI * (i + 1) / n;
			this.quad(polar(t0, radius, y0).add(center), polar(t1, radius, y0).add(center), polar(t1, radius, y1).add(center), polar(t0, radius, y1).add(center), c0, c0, c1, c1);
		}
	}

	// ------------------------------------------------------------ crystal

	/**
	 * A crystal: a {@code sides}-sided prism from {@code base} along {@code axis} ({@code length} long, {@code radius} thick) with a
	 * pointed tip a third as long again. {@code body} colours the faces; on the glow buffer they are lit edges instead.
	 */
	void crystal(final Vector3fc base, final Vector3fc axis, final float length, final float radius, final int sides, final int body, final int tip) {
		Vector3f a = new Vector3f(axis).normalize();
		Vector3f u = across(a);
		Vector3f v = new Vector3f(a).cross(u).normalize();
		Vector3f top = new Vector3f(a).mul(length).add(base);
		Vector3f point = new Vector3f(a).mul(length * 1.35F).add(base);
		for (int i = 0; i < sides; i++) {
			float t0 = Mth.TWO_PI * i / sides;
			float t1 = Mth.TWO_PI * (i + 1) / sides;
			Vector3f o0 = new Vector3f(u).mul(Mth.cos(t0) * radius).add(new Vector3f(v).mul(Mth.sin(t0) * radius));
			Vector3f o1 = new Vector3f(u).mul(Mth.cos(t1) * radius).add(new Vector3f(v).mul(Mth.sin(t1) * radius));
			Vector3f b0 = new Vector3f(base).add(new Vector3f(o0).mul(0.75F));
			Vector3f b1 = new Vector3f(base).add(new Vector3f(o1).mul(0.75F));
			Vector3f h0 = new Vector3f(top).add(o0);
			Vector3f h1 = new Vector3f(top).add(o1);
			if (this.glow) {
				// lit edges: a thin line up each corner and round the shoulder
				this.bar(b0, h0, 0.025F, 0.025F, body, body);
				this.bar(h0, point, 0.025F, 0.01F, body, tip);
				this.bar(h0, h1, 0.02F, 0.02F, body, body);
			} else {
				this.quad(b0, b1, h1, h0, body, body, body, body);
				this.tri(h0, h1, point, body, body, tip);
			}
		}
	}

	// ------------------------------------------------------------ stars, arrows, chains

	/**
	 * A flat star of {@code points} points in the plane of {@code a} and {@code b} (unit vectors; the camera's right and up make it face
	 * the camera): tips {@code r} out, the notches {@code inner} times that, turned by {@code spin}; {@code core} in the middle fading to
	 * {@code tip}.
	 */
	void star(final Vector3fc c, final Vector3fc a, final Vector3fc b, final float r, final int points, final float inner, final float spin, final int core,
		final int tip) {
		int n = points * 2;
		Vector3f prev = null;
		for (int i = 0; i <= n; i++) {
			float t = spin + Mth.TWO_PI * i / n;
			float rr = i % 2 == 0 ? r : r * inner;
			Vector3f p = new Vector3f(a).mul(Mth.cos(t) * rr).add(new Vector3f(b).mul(Mth.sin(t) * rr)).add(c);
			if (prev != null) {
				this.tri(c, prev, p, core, i % 2 == 0 ? lerp(core, tip, 0.5F) : tip, i % 2 == 0 ? tip : lerp(core, tip, 0.5F));
			}
			prev = p;
		}
	}

	/** A twinkle: a thin four-pointed glint ({@link #star} with deep notches) and a soft dot, facing the camera. */
	void glint(final Vector3fc c, final Vector3fc right, final Vector3fc up, final float r, final float spin, final int color) {
		this.star(c, right, up, r, 4, 0.12F, spin, color, color & 0xFFFFFF);
		this.sprite(c, right, up, r * 0.35F, color);
	}

	/** An arrow from {@code tail} to {@code tip}: a shaft, a head and fletching, flat in the plane of {@code side}. */
	void arrow(final Vector3fc tail, final Vector3fc tip, final Vector3fc side, final float width, final int shaft, final int head) {
		Vector3f d = new Vector3f(tip).sub(tail);
		float length = d.length();
		if (length < 1.0E-4F) {
			return;
		}
		d.div(length);
		Vector3f neck = new Vector3f(d).mul(-Math.min(0.45F, length * 0.3F)).add(tip);
		this.bar(tail, neck, width * 0.25F, width * 0.25F, shaft, shaft);
		this.blade(neck, tip, side, width, head, head);
		Vector3f fl = new Vector3f(d).mul(Math.min(0.35F, length * 0.25F)).add(tail);
		this.diamond(new Vector3f(tail).lerp(fl, 0.5F).add(new Vector3f(side).normalize(width * 0.6F)), d, side, Math.min(0.35F, length * 0.25F), width * 0.7F,
			shaft & 0xFFFFFF, shaft);
		this.diamond(new Vector3f(tail).lerp(fl, 0.5F).sub(new Vector3f(side).normalize(width * 0.6F)), d, side, Math.min(0.35F, length * 0.25F), width * 0.7F,
			shaft & 0xFFFFFF, shaft);
	}

	/**
	 * A chain from {@code from} to {@code to}: oval links {@code link} long, each turned a quarter from the last, drawn as thin bars;
	 * {@code turn} rolls the whole chain.
	 */
	void chain(final Vector3fc from, final Vector3fc to, final float link, final float turn, final int color) {
		Vector3f d = new Vector3f(to).sub(from);
		float length = d.length();
		if (length < 1.0E-3F) {
			return;
		}
		d.div(length);
		Vector3f u = across(d);
		Vector3f v = new Vector3f(d).cross(u).normalize();
		int links = Math.min(64, Math.max(1, (int)(length / (link * 0.8F))));
		float step = length / links;
		for (int i = 0; i < links; i++) {
			float a = turn + (i % 2 == 0 ? 0.0F : Mth.HALF_PI);
			Vector3f w = new Vector3f(u).mul(Mth.cos(a)).add(new Vector3f(v).mul(Mth.sin(a))).mul(link * 0.28F);
			Vector3f s = new Vector3f(d).mul(step * i).add(from);
			Vector3f e = new Vector3f(d).mul(step * i + link).add(from);
			Vector3f s1 = new Vector3f(d).mul(step * i + link * 0.22F).add(from);
			Vector3f e1 = new Vector3f(d).mul(step * i + link * 0.78F).add(from);
			// an octagon-ish loop: two long sides and the rounded ends
			this.bar(new Vector3f(s1).add(w), new Vector3f(e1).add(w), link * 0.12F, link * 0.12F, color, color);
			this.bar(new Vector3f(s1).sub(w), new Vector3f(e1).sub(w), link * 0.12F, link * 0.12F, color, color);
			this.bar(s, new Vector3f(s1).add(w), link * 0.1F, link * 0.1F, color, color);
			this.bar(s, new Vector3f(s1).sub(w), link * 0.1F, link * 0.1F, color, color);
			this.bar(e, new Vector3f(e1).add(w), link * 0.1F, link * 0.1F, color, color);
			this.bar(e, new Vector3f(e1).sub(w), link * 0.1F, link * 0.1F, color, color);
		}
	}

	/** A circle (a polyline of {@code n} glow lines) round {@code c} in the plane of {@code a} and {@code b}. */
	void circle(final Vector3fc c, final Vector3fc a, final Vector3fc b, final float r, final float width, final int n, final int color) {
		Vector3f prev = null;
		for (int i = 0; i <= n; i++) {
			float t = Mth.TWO_PI * i / n;
			Vector3f p = new Vector3f(a).mul(Mth.cos(t) * r).add(new Vector3f(b).mul(Mth.sin(t) * r)).add(c);
			if (prev != null) {
				this.bar(prev, p, width, width, color, color);
			}
			prev = p;
		}
	}

	// ------------------------------------------------------------ paths

	/** A trail's points with the ones closer than 0.2 blocks to the last kept dropped (a still holder leaves no zero-length parts). */
	static java.util.List<Vector3f> path(final java.util.List<Vector3f> trail) {
		java.util.List<Vector3f> points = new java.util.ArrayList<>();
		points.add(new Vector3f());
		for (Vector3f p : trail) {
			if (p.distanceSquared(points.getLast()) > 0.04F) {
				points.add(p);
			}
		}
		return points;
	}

	/** The point {@code t} (0..1) of the way along a polyline up to point {@code last}. */
	static Vector3f along(final java.util.List<Vector3f> points, final int last, final float t) {
		float total = 0.0F;
		for (int i = 0; i < last; i++) {
			total += points.get(i).distance(points.get(i + 1));
		}
		float want = total * t;
		for (int i = 0; i < last; i++) {
			float d = points.get(i).distance(points.get(i + 1));
			if (want <= d && d > 0) {
				return new Vector3f(points.get(i)).lerp(points.get(i + 1), want / d);
			}
			want -= d;
		}
		return new Vector3f(points.get(last));
	}

	/** Length of a polyline up to point {@code last}. */
	static float length(final java.util.List<Vector3f> points, final int last) {
		float total = 0.0F;
		for (int i = 0; i < last; i++) {
			total += points.get(i).distance(points.get(i + 1));
		}
		return total;
	}

	// ------------------------------------------------------------ colour

	static int lerp(final int a, final int b, final float t) {
		float k = Mth.clamp(t, 0.0F, 1.0F);
		int aa = a >>> 24;
		int ar = a >> 16 & 0xFF;
		int ag = a >> 8 & 0xFF;
		int ab = a & 0xFF;
		return (int)(aa + ((b >>> 24) - aa) * k) << 24 | (int)(ar + ((b >> 16 & 0xFF) - ar) * k) << 16 | (int)(ag + ((b >> 8 & 0xFF) - ag) * k) << 8
			| (int)(ab + ((b & 0xFF) - ab) * k);
	}

	/** {@code rgb} with {@code alpha} (0..1, clamped). */
	static int argb(final float alpha, final int rgb) {
		return VfxDraw.argb(alpha, rgb);
	}

	/** A steady pseudo-random 0..1 for {@code i} (positions of petals, flakes and shards that must not jump between frames). */
	static float hash(final int i) {
		int h = i * 0x9E3779B9;
		h ^= h >>> 15;
		h *= 0x85EBCA6B;
		h ^= h >>> 13;
		return (h & 0xFFFF) / 65535.0F;
	}

	/** 0 to 1 over {@code from}..{@code to} of {@code t}, eased out. */
	static float ease(final float t, final float from, final float to) {
		float k = Mth.clamp((t - from) / (to - from), 0.0F, 1.0F);
		return 1.0F - (1.0F - k) * (1.0F - k);
	}
}
