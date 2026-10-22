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

	// ------------------------------------------------------------ magecraft

	/**
	 * A rune: three or four strokes between the points of a 3 x 3 grid picked by {@code seed}, standing in the plane of {@code a} (across)
	 * and {@code b} (up), {@code r} from its middle to the top.
	 */
	void rune(final Vector3fc c, final Vector3fc a, final Vector3fc b, final float r, final int seed, final float width, final int color) {
		int strokes = 3 + (hash(seed * 7 + 3) < 0.5F ? 0 : 1);
		int from = (int)(hash(seed * 11 + 1) * 8.99F);
		for (int i = 0; i < strokes; i++) {
			int to = (int)(hash(seed * 31 + i * 17 + 5) * 8.99F);
			if (to == from) {
				to = (to + 4) % 9;
			}
			this.bar(grid(c, a, b, r, from), grid(c, a, b, r, to), width, width, color, color);
			// mostly one stroke on from the last, sometimes a fresh one
			from = hash(seed * 13 + i * 5) < 0.7F ? to : (int)(hash(seed * 19 + i) * 8.99F);
		}
	}

	private static Vector3f grid(final Vector3fc c, final Vector3fc a, final Vector3fc b, final float r, final int k) {
		return new Vector3f(a).mul((k % 3 - 1) * r * 0.6F).add(new Vector3f(b).mul((k / 3 - 1) * r)).add(c);
	}

	/**
	 * A star polygon of {@code points} points {@code r} out in the plane of {@code a} and {@code b}, each line joining a point to the one
	 * {@code step} on ({@code step} 1: a plain polygon; 6 points step 2: a hexagram), turned by {@code spin}.
	 */
	void polygram(final Vector3fc c, final Vector3fc a, final Vector3fc b, final float r, final int points, final int step, final float spin, final float width,
		final int color) {
		for (int i = 0; i < points; i++) {
			float t0 = spin + Mth.TWO_PI * i / points;
			float t1 = spin + Mth.TWO_PI * (i + step) / points;
			Vector3f p0 = new Vector3f(a).mul(Mth.cos(t0) * r).add(new Vector3f(b).mul(Mth.sin(t0) * r)).add(c);
			Vector3f p1 = new Vector3f(a).mul(Mth.cos(t1) * r).add(new Vector3f(b).mul(Mth.sin(t1) * r)).add(c);
			this.bar(p0, p1, width, width, color, color);
		}
	}

	/**
	 * A magic circle of radius {@code r} in the plane of {@code a} and {@code b}: a double outer ring with a band of runes between (turning
	 * with {@code spin}), a {@code points}-pointed star polygon turning the other way, an inner ring and a small one in the middle;
	 * {@code color} for the lines, {@code dim} for the runes. Line widths and the number of runes grow with its size.
	 */
	void magicCircle(final Vector3fc c, final Vector3fc a, final Vector3fc b, final float r, final float spin, final int seed, final int points, final int color,
		final int dim) {
		if (r <= 0.02F) {
			return;
		}
		float w = Math.max(0.015F, r * 0.012F);
		int n = Mth.clamp((int)(r * 10), 16, 48);
		this.circle(c, a, b, r, w * 1.4F, n, color);
		this.circle(c, a, b, r * 0.84F, w, n, color);
		this.circle(c, a, b, r * 0.45F, w, Math.max(12, n / 2), color);
		this.circle(c, a, b, r * 0.12F, w, 10, color);
		int runes = Mth.clamp((int)(r * 5), 6, 24);
		for (int i = 0; i < runes; i++) {
			float t = spin + Mth.TWO_PI * i / runes;
			Vector3f out = new Vector3f(a).mul(Mth.cos(t)).add(new Vector3f(b).mul(Mth.sin(t)));
			Vector3f along = new Vector3f(a).mul(-Mth.sin(t)).add(new Vector3f(b).mul(Mth.cos(t)));
			this.rune(new Vector3f(out).mul(r * 0.92F).add(c), along, out, r * 0.055F, seed * 37 + i, w * 0.8F, dim);
		}
		this.polygram(c, a, b, r * 0.84F, points, points >= 6 ? 2 : Math.max(1, points / 2), -spin * 1.3F, w, color);
		// small runes on the inner ring
		for (int i = 0; i < 6; i++) {
			float t = -spin * 0.7F + Mth.TWO_PI * i / 6;
			Vector3f out = new Vector3f(a).mul(Mth.cos(t)).add(new Vector3f(b).mul(Mth.sin(t)));
			Vector3f along = new Vector3f(a).mul(-Mth.sin(t)).add(new Vector3f(b).mul(Mth.cos(t)));
			this.rune(new Vector3f(out).mul(r * 0.32F).add(c), along, out, r * 0.06F, seed * 53 + i, w * 0.8F, dim);
		}
	}

	/**
	 * A jagged bolt of lightning from {@code from} to {@code to}: {@code steps} kinks thrown out up to {@code jag} blocks (picked by
	 * {@code seed}), a soft glow {@code width} wide and a hot core.
	 */
	void bolt(final Vector3fc from, final Vector3fc to, final int seed, final float jag, final int steps, final float width, final int glow, final int core) {
		Vector3f d = new Vector3f(to).sub(from);
		Vector3f u = across(d.lengthSquared() < 1.0E-6F ? new Vector3f(0, 1, 0) : new Vector3f(d).normalize());
		Vector3f v = new Vector3f(d).cross(u).normalize();
		Vector3f prev = new Vector3f(from);
		for (int i = 1; i <= steps; i++) {
			float t = (float)i / steps;
			Vector3f next = new Vector3f(d).mul(t).add(from);
			if (i < steps) {
				float k = jag * Mth.sin(t * Mth.PI);
				next.add(new Vector3f(u).mul((hash(seed * 17 + i) - 0.5F) * 2 * k)).add(new Vector3f(v).mul((hash(seed * 29 + i) - 0.5F) * 2 * k));
			}
			this.glowLine(prev, next, width, glow, glow);
			this.bar(prev, next, width * 0.18F, width * 0.18F, core, core);
			prev = next;
		}
	}

	/** A flat hexagon panel facing {@code normal}: a faint fill and lit edges ({@code fill} 0 alpha leaves just the edges). */
	void hexagon(final Vector3fc c, final Vector3fc normal, final float r, final float spin, final float width, final int edge, final int fill) {
		Vector3f n = new Vector3f(normal).normalize();
		Vector3f a = across(n);
		Vector3f b = new Vector3f(n).cross(a).normalize();
		Vector3f prev = null;
		for (int i = 0; i <= 6; i++) {
			float t = spin + Mth.TWO_PI * i / 6;
			Vector3f p = new Vector3f(a).mul(Mth.cos(t) * r).add(new Vector3f(b).mul(Mth.sin(t) * r)).add(c);
			if (prev != null) {
				if ((fill >>> 24) > 0) {
					this.tri(c, prev, p, fill, fill & 0xFFFFFF, fill & 0xFFFFFF);
				}
				if (width > 0.0F) {
					this.bar(prev, p, width, width, edge, edge);
				}
			}
			prev = p;
		}
	}

	/**
	 * A flower of {@code petals} petals {@code r} long round {@code at}, opened {@code open} (0: a closed bud pointing up, 1: lying wide
	 * open, tips raised a little), turned by {@code turn}: petals {@code petal} at their base to {@code tip}, a {@code heart} dot in the
	 * middle.
	 */
	void bloom(final Vector3fc at, final int petals, final float r, final float open, final float turn, final int petal, final int tip, final int heart) {
		float lift = 1.2F - 0.9F * open;
		for (int k = 0; k < petals; k++) {
			float a = turn + Mth.TWO_PI * k / petals;
			Vector3f d = new Vector3f(Mth.cos(a), lift, Mth.sin(a)).normalize();
			Vector3f end = new Vector3f(d).mul(r).add(at);
			this.blade(at, end, new Vector3f(-Mth.sin(a), 0, Mth.cos(a)), r * 0.42F, petal, tip);
		}
		this.bar(at, new Vector3f(at).add(0, r * 0.25F, 0), r * 0.12F, r * 0.08F, heart, heart);
	}

	// ------------------------------------------------------------ bodies (the pirates' fists, tentacles, ships and flags)

	/** Where shaded bodies are lit from: above and a little to one side. */
	private static final Vector3fc LIGHT = new Vector3f(0.4F, 0.85F, 0.35F).normalize();

	/** {@code lit} where a surface faces the light, {@code dark} where it faces away (bodies that look round without a lit shader). */
	static int shadeBy(final Vector3fc normal, final int lit, final int dark) {
		return lerp(dark, lit, 0.5F + 0.5F * normal.dot(LIGHT));
	}

	/**
	 * An ellipsoid round {@code c} with half-axes {@code ax}, {@code ay} (to its poles) and {@code az}, {@code lats} x {@code lons} faces,
	 * shaded {@code lit} to {@code dark} ({@link #shadeBy}): fists, cream puffs, clouds, an octopus's head.
	 */
	void ellipsoid(final Vector3fc c, final Vector3fc ax, final Vector3fc ay, final Vector3fc az, final int lats, final int lons, final int lit, final int dark) {
		Vector3f nx = new Vector3f(ax).normalize();
		Vector3f ny = new Vector3f(ay).normalize();
		Vector3f nz = new Vector3f(az).normalize();
		Vector3f[] row = null;
		int[] rowColor = null;
		for (int i = 0; i <= lats; i++) {
			float lat = -Mth.HALF_PI + Mth.PI * i / lats;
			Vector3f[] next = new Vector3f[lons + 1];
			int[] nextColor = new int[lons + 1];
			for (int j = 0; j <= lons; j++) {
				float lon = Mth.TWO_PI * j / lons;
				float x = Mth.cos(lat) * Mth.cos(lon);
				float y = Mth.sin(lat);
				float z = Mth.cos(lat) * Mth.sin(lon);
				next[j] = new Vector3f(ax).mul(x).add(new Vector3f(ay).mul(y)).add(new Vector3f(az).mul(z)).add(c);
				Vector3f n = new Vector3f(nx).mul(x).add(new Vector3f(ny).mul(y)).add(new Vector3f(nz).mul(z));
				nextColor[j] = shadeBy(n.lengthSquared() < 1.0E-6F ? ny : n.normalize(), lit, dark);
			}
			if (row != null) {
				for (int j = 0; j < lons; j++) {
					this.quad(row[j], row[j + 1], next[j + 1], next[j], rowColor[j], rowColor[j + 1], nextColor[j + 1], nextColor[j]);
				}
			}
			row = next;
			rowColor = nextColor;
		}
	}

	/**
	 * A tube along {@code points} ({@code sides} faces round), {@code r0} thick at the first point tapering to {@code r1} at the last,
	 * shaded {@code lit} to {@code dark}; its frame is carried along the curve so it never twists where the curve stands straight up.
	 */
	void tube(final java.util.List<Vector3f> points, final float r0, final float r1, final int sides, final int lit, final int dark) {
		int n = points.size();
		if (n < 2) {
			return;
		}
		Vector3f[] us = new Vector3f[n];
		Vector3f[] vs = new Vector3f[n];
		Vector3f last = new Vector3f(0, 1, 0);
		for (int i = 0; i < n; i++) {
			Vector3f d = new Vector3f(points.get(Math.min(n - 1, i + 1))).sub(points.get(Math.max(0, i - 1)));
			if (d.lengthSquared() < 1.0E-8F) {
				d.set(last);
			}
			d.normalize();
			last.set(d);
			Vector3f u = i == 0 ? across(d) : new Vector3f(us[i - 1]).sub(new Vector3f(d).mul(us[i - 1].dot(d)));
			if (u.lengthSquared() < 1.0E-6F) {
				u = across(d);
			}
			us[i] = u.normalize();
			vs[i] = new Vector3f(d).cross(us[i]).normalize();
		}
		for (int i = 0; i < n - 1; i++) {
			float ra = Mth.lerp((float)i / (n - 1), r0, r1);
			float rb = Mth.lerp((float)(i + 1) / (n - 1), r0, r1);
			for (int k = 0; k < sides; k++) {
				float a0 = Mth.TWO_PI * k / sides;
				float a1 = Mth.TWO_PI * (k + 1) / sides;
				Vector3f da0 = new Vector3f(us[i]).mul(Mth.cos(a0)).add(new Vector3f(vs[i]).mul(Mth.sin(a0)));
				Vector3f da1 = new Vector3f(us[i]).mul(Mth.cos(a1)).add(new Vector3f(vs[i]).mul(Mth.sin(a1)));
				Vector3f db0 = new Vector3f(us[i + 1]).mul(Mth.cos(a0)).add(new Vector3f(vs[i + 1]).mul(Mth.sin(a0)));
				Vector3f db1 = new Vector3f(us[i + 1]).mul(Mth.cos(a1)).add(new Vector3f(vs[i + 1]).mul(Mth.sin(a1)));
				this.quad(new Vector3f(da0).mul(ra).add(points.get(i)), new Vector3f(da1).mul(ra).add(points.get(i)), new Vector3f(db1).mul(rb).add(points.get(i + 1)),
					new Vector3f(db0).mul(rb).add(points.get(i + 1)), shadeBy(da0, lit, dark), shadeBy(da1, lit, dark), shadeBy(db1, lit, dark), shadeBy(db0, lit, dark));
			}
		}
	}

	/**
	 * Points along a tentacle from {@code base} rising along {@code up} for {@code length}, bending towards {@code side} by {@code bend}
	 * (radians along its whole length), swaying with {@code phase} and curling over at the tip by {@code curl} (radians more at the tip):
	 * {@code steps} + 1 points.
	 */
	static java.util.List<Vector3f> tentacle(final Vector3fc base, final Vector3fc up, final Vector3fc side, final float length, final float bend, final float curl,
		final float phase, final int steps) {
		java.util.List<Vector3f> points = new java.util.ArrayList<>(steps + 1);
		Vector3f u = new Vector3f(up).normalize();
		Vector3f s = new Vector3f(side).normalize();
		Vector3f p = new Vector3f(base);
		points.add(new Vector3f(p));
		float step = length / steps;
		for (int i = 1; i <= steps; i++) {
			float t = (float)i / steps;
			float a = bend * t + curl * t * t * t + 0.35F * Mth.sin(phase + t * 4.0F) * t;
			p.add(new Vector3f(u).mul(Mth.cos(a) * step)).add(new Vector3f(s).mul(Mth.sin(a) * step));
			points.add(new Vector3f(p));
		}
		return points;
	}

	/** Suckers along the inner side of a tentacle ({@link #tentacle} points bending towards {@code side}): pale rings shrinking to the tip. */
	void suckers(final java.util.List<Vector3f> points, final Vector3fc side, final float r0, final float r1, final int color) {
		int n = points.size();
		for (int i = 1; i < n - 1; i++) {
			float t = (float)i / (n - 1);
			Vector3f d = new Vector3f(points.get(i + 1)).sub(points.get(i - 1));
			if (d.lengthSquared() < 1.0E-8F) {
				continue;
			}
			d.normalize();
			Vector3f in = new Vector3f(side).sub(new Vector3f(d).mul(d.dot(side)));
			if (in.lengthSquared() < 1.0E-6F) {
				continue;
			}
			in.normalize();
			float r = Mth.lerp(t, r0, r1);
			Vector3f at = new Vector3f(in).mul(r * 1.02F).add(points.get(i));
			Vector3f a = new Vector3f(d);
			Vector3f b = new Vector3f(in).cross(d).normalize();
			this.circle(at, a, b, r * 0.45F, r * 0.08F + 0.01F, 8, color);
		}
	}

	/**
	 * A cartoon fist about {@code size} blocks across round {@code c}: knuckles towards {@code punch}, the back of the hand towards
	 * {@code back}, a wrist and cuff behind; shaded {@code lit} to {@code dark}. The four fingers are folded in two rows (the knuckle bumps on
	 * top, the middle joints making the flat front) with the fingertips tucked under, the thumb laid across the front, and dark creases
	 * between the fingers so the shape reads at any size. {@code fine} false: fewer faces, no tips or creases (a crowd of afterimages).
	 */
	void fist(final Vector3fc c, final Vector3fc punch, final Vector3fc back, final float size, final int lit, final int dark, final boolean fine) {
		Vector3f f = new Vector3f(punch).normalize();
		Vector3f u = new Vector3f(back).sub(new Vector3f(f).mul(f.dot(back)));
		if (u.lengthSquared() < 1.0E-6F) {
			u = new Vector3f(f).cross(across(f));
		}
		u.normalize();
		Vector3f s = new Vector3f(f).cross(u).normalize();
		int la = fine ? 6 : 4;
		int lo = fine ? 10 : 7;
		float k = size * 0.5F;
		// the back of the hand, flattish
		this.ellipsoid(fistPoint(c, s, u, f, k, 0.0F, 0.12F, -0.1F), new Vector3f(s).mul(0.84F * k), new Vector3f(u).mul(0.48F * k), new Vector3f(f).mul(0.66F * k),
			la, lo, lit, dark);
		for (int i = 0; i < 4; i++) {
			float x = (i - 1.5F) * 0.44F;
			// the knuckle bumps along the top front edge
			this.ellipsoid(fistPoint(c, s, u, f, k, x, 0.42F, 0.62F), new Vector3f(s).mul(0.21F * k), new Vector3f(u).mul(0.25F * k), new Vector3f(f).mul(0.3F * k), la,
				lo, lit, dark);
			// the middle joints making the flat front of the fist
			this.ellipsoid(fistPoint(c, s, u, f, k, x, 0.02F, 0.86F), new Vector3f(s).mul(0.2F * k), new Vector3f(u).mul(0.28F * k), new Vector3f(f).mul(0.2F * k), la,
				lo, lit, dark);
			if (fine) {
				// the fingertips tucked in under
				this.ellipsoid(fistPoint(c, s, u, f, k, x, -0.4F, 0.6F), new Vector3f(s).mul(0.19F * k), new Vector3f(u).mul(0.17F * k), new Vector3f(f).mul(0.24F * k),
					la - 1, lo, lit, dark);
			}
		}
		// the thumb: its root on the side, laid across the front of the lower finger joints
		this.ellipsoid(fistPoint(c, s, u, f, k, -0.82F, -0.28F, 0.35F), new Vector3f(s).mul(0.26F * k), new Vector3f(u).mul(0.24F * k), new Vector3f(f).mul(0.34F * k),
			la, lo, lit, dark);
		this.ellipsoid(fistPoint(c, s, u, f, k, -0.28F, -0.26F, 1.0F), new Vector3f(s).mul(0.48F * k), new Vector3f(u).mul(0.16F * k), new Vector3f(f).mul(0.17F * k),
			la, lo, lit, dark);
		// the wrist and a cuff round it
		this.ellipsoid(fistPoint(c, s, u, f, k, 0.0F, 0.0F, -0.85F), new Vector3f(s).mul(0.56F * k), new Vector3f(u).mul(0.46F * k), new Vector3f(f).mul(0.5F * k), la,
			lo, lit, dark);
		if (!fine || this.glow) {
			return;
		}
		int crease = lerp(dark, dark & 0xFF000000 | 0x101018, 0.6F);
		java.util.List<Vector3f> cuff = new java.util.ArrayList<>();
		for (int i = 0; i <= 14; i++) {
			float a = Mth.TWO_PI * i / 14;
			cuff.add(new Vector3f(s).mul(Mth.cos(a) * 0.6F * k).add(new Vector3f(u).mul(Mth.sin(a) * 0.5F * k)).add(new Vector3f(f).mul(-0.62F * k)).add(c));
		}
		this.tube(cuff, 0.07F * k, 0.07F * k, 5, lerp(lit, dark, 0.25F), dark);
		// creases between the fingers, over the knuckles, down the front and under
		float w = 0.022F * k;
		for (int g = 0; g < 3; g++) {
			float x = (g - 1) * 0.44F;
			Vector3f a = fistPoint(c, s, u, f, k, x, 0.64F, 0.55F);
			Vector3f b = fistPoint(c, s, u, f, k, x, 0.3F, 1.07F);
			Vector3f d = fistPoint(c, s, u, f, k, x, -0.25F, 1.08F);
			Vector3f e = fistPoint(c, s, u, f, k, x, -0.58F, 0.72F);
			this.bar(a, b, w, w, crease, crease);
			this.bar(b, d, w, w, crease, crease);
			this.bar(d, e, w, w, crease, crease);
		}
		// the fold between the knuckles and the middle joints, across the front
		this.bar(fistPoint(c, s, u, f, k, -0.88F, 0.3F, 1.0F), fistPoint(c, s, u, f, k, 0.88F, 0.3F, 1.0F), w, w, crease, crease);
		// the fingers' lines running back over the back of the hand, and the thumb's fold along the bottom front and up its side
		for (int g = 0; g < 3; g++) {
			float x = (g - 1) * 0.44F;
			this.bar(fistPoint(c, s, u, f, k, x, 0.66F, 0.5F), fistPoint(c, s, u, f, k, x * 0.85F, 0.62F, -0.05F), w * 0.8F, w * 0.4F, crease, crease & 0xFFFFFF);
		}
		this.bar(fistPoint(c, s, u, f, k, -0.86F, -0.1F, 1.0F), fistPoint(c, s, u, f, k, 0.24F, -0.1F, 1.12F), w, w, crease, crease);
		this.bar(fistPoint(c, s, u, f, k, -1.0F, -0.08F, 0.75F), fistPoint(c, s, u, f, k, -1.04F, -0.2F, 0.1F), w, w, crease, crease);
	}

	/** A point {@code x} across, {@code y} up and {@code z} forward (in half-sizes {@code k}) of a {@link #fist} round {@code c}. */
	private static Vector3f fistPoint(final Vector3fc c, final Vector3f s, final Vector3f u, final Vector3f f, final float k, final float x, final float y, final float z) {
		return new Vector3f(s).mul(x * k).add(new Vector3f(u).mul(y * k)).add(new Vector3f(f).mul(z * k)).add(c);
	}


	/** A filled disc of {@code r} round {@code c} in the plane of {@code a} and {@code b}, one solid colour ({@link #sprite} fades to its rim). */
	void plate(final Vector3fc c, final Vector3fc a, final Vector3fc b, final float r, final int n, final int color) {
		Vector3f prev = null;
		for (int i = 0; i <= n; i++) {
			float t = Mth.TWO_PI * i / n;
			Vector3f p = new Vector3f(a).mul(Mth.cos(t) * r).add(new Vector3f(b).mul(Mth.sin(t) * r)).add(c);
			if (prev != null) {
				this.tri(c, prev, p, color, color, color);
			}
			prev = p;
		}
	}

	/** Jolly Roger styles: crossed bones; Whitebeard's (a crescent moustache, a cross behind); the Pirate King's (bones and a curled moustache). */
	static final int PLAIN = 0;
	static final int WHITEBEARD = 1;
	static final int ROGER = 2;

	/**
	 * A Jolly Roger in the plane of {@code a} (across) and {@code b} (up), {@code r} from its middle to the top of the skull: bones (or
	 * Whitebeard's cross) behind a skull in {@code bone}, its eyes, nose and teeth in {@code dark} laid on both faces, and the moustache of
	 * {@code style}. Drawn flat; on the glow buffer {@code dark} adds nothing, so draw it on the shade buffer and light it separately.
	 */
	void jollyRoger(final Vector3fc c, final Vector3fc a, final Vector3fc b, final float r, final int style, final int bone, final int dark) {
		Vector3f n = new Vector3f(a).cross(b).normalize(0.015F + r * 0.01F);
		if (style == WHITEBEARD) {
			this.bar(at(c, a, b, r, 0.0F, -1.05F), at(c, a, b, r, 0.0F, 1.0F), r * 0.13F, r * 0.13F, bone, bone);
			this.bar(at(c, a, b, r, -1.05F, 0.05F), at(c, a, b, r, 1.05F, 0.05F), r * 0.13F, r * 0.13F, bone, bone);
		} else {
			for (int k = -1; k <= 1; k += 2) {
				Vector3f p0 = at(c, a, b, r, -0.88F * k, -0.95F);
				Vector3f p1 = at(c, a, b, r, 0.88F * k, 0.3F);
				this.bar(p0, p1, r * 0.08F, r * 0.08F, bone, bone);
				for (Vector3f end : new Vector3f[] {p0, p1}) {
					Vector3f d = new Vector3f(p1).sub(p0).normalize();
					Vector3f side = new Vector3f(n).cross(d).normalize(r * 0.09F);
					this.plate(new Vector3f(end).add(side), a, b, r * 0.11F, 8, bone);
					this.plate(new Vector3f(end).sub(side), a, b, r * 0.11F, 8, bone);
				}
			}
		}
		// the skull: cranium and jaw
		this.plate(at(c, a, b, r, 0.0F, 0.15F), a, b, r * 0.56F, 16, bone);
		Vector3f j0 = at(c, a, b, r, -0.33F, -0.18F);
		Vector3f j1 = at(c, a, b, r, 0.33F, -0.18F);
		Vector3f j2 = at(c, a, b, r, 0.25F, -0.52F);
		Vector3f j3 = at(c, a, b, r, -0.25F, -0.52F);
		this.quad(j0, j1, j2, j3, bone);
		// moustaches go over the jaw, in bone with a dark edge
		if (style == WHITEBEARD) {
			this.crescentMoustache(c, a, b, r, n, bone, dark, false);
		} else if (style == ROGER) {
			this.crescentMoustache(c, a, b, r, n, bone, dark, true);
		}
		for (int side = -1; side <= 1; side += 2) {
			Vector3f off = new Vector3f(n).mul(side);
			this.plate(at(c, a, b, r, -0.21F, 0.1F).add(off), a, b, r * 0.15F, 10, dark);
			this.plate(at(c, a, b, r, 0.21F, 0.1F).add(off), a, b, r * 0.15F, 10, dark);
			this.tri(at(c, a, b, r, 0.0F, -0.06F).add(off), at(c, a, b, r, -0.07F, -0.2F).add(off), at(c, a, b, r, 0.07F, -0.2F).add(off), dark, dark, dark);
			if (style == PLAIN) {
				for (int t = -1; t <= 1; t++) {
					this.bar(at(c, a, b, r, t * 0.12F, -0.33F).add(off), at(c, a, b, r, t * 0.12F, -0.5F).add(off), r * 0.018F, r * 0.018F, dark, dark);
				}
			}
		}
	}

	/** A point {@code x}, {@code y} (in units of {@code r}) on the plane of {@code a} and {@code b} round {@code c}. */
	private static Vector3f at(final Vector3fc c, final Vector3fc a, final Vector3fc b, final float r, final float x, final float y) {
		return new Vector3f(a).mul(x * r).add(new Vector3f(b).mul(y * r)).add(c);
	}

	/** Whitebeard's crescent moustache under the nose, horns up; {@code curled} rolls the ends into curls (the Pirate King's). */
	private void crescentMoustache(final Vector3fc c, final Vector3fc a, final Vector3fc b, final float r, final Vector3f n, final int bone, final int dark,
		final boolean curled) {
		int steps = 12;
		Vector3f prevIn = null;
		Vector3f prevOut = null;
		float wide = curled ? 0.5F : 0.78F;
		for (int i = 0; i <= steps; i++) {
			float t = (float)i / steps * 2 - 1;
			float x = t * wide;
			float y = -0.27F + 0.32F * t * t;
			float thick = 0.12F * (1.0F - t * t * 0.85F);
			Vector3f inner = at(c, a, b, r, x, y + thick * 0.5F);
			Vector3f outer = at(c, a, b, r, x, y - thick * 0.5F);
			for (int side = -1; side <= 1; side += 2) {
				Vector3f off = new Vector3f(n).mul(side * 1.6F);
				if (prevIn != null) {
					this.quad(new Vector3f(prevIn).add(off), new Vector3f(inner).add(off), new Vector3f(outer).add(off), new Vector3f(prevOut).add(off), bone);
					this.bar(new Vector3f(prevOut).add(off).add(new Vector3f(n).mul(side * 0.3F)), new Vector3f(outer).add(off).add(new Vector3f(n).mul(side * 0.3F)),
						r * 0.012F, r * 0.012F, dark, dark);
				}
			}
			prevIn = inner;
			prevOut = outer;
		}
		if (curled) {
			// the ends rolled up into small curls
			for (int k = -1; k <= 1; k += 2) {
				Vector3f prev = null;
				for (int i = 0; i <= 10; i++) {
					float q = i / 10.0F;
					float ang = -Mth.HALF_PI * k + q * Mth.TWO_PI * 0.85F * k;
					float rr = 0.13F * (1.0F - 0.6F * q);
					Vector3f p = at(c, a, b, r, k * (wide + 0.02F) + Mth.cos(ang) * rr * k * -1.0F, 0.05F + 0.13F + Mth.sin(ang) * rr);
					if (prev != null) {
						for (int side = -1; side <= 1; side += 2) {
							Vector3f off = new Vector3f(n).mul(side * 1.6F);
							this.bar(new Vector3f(prev).add(off), new Vector3f(p).add(off), r * 0.04F * (1.0F - 0.5F * q), r * 0.04F * (1.0F - 0.5F * q), bone, bone);
						}
					}
					prev = p;
				}
			}
		}
	}

	/**
	 * A galleon whose deck is centred on {@code c}, bow towards {@code fwd} (pitched as it is): {@code length} long, {@code beam} wide, its
	 * hull {@code depth} deep under the deck, {@code masts} masts (1 to 3, from the stern) up to {@code mast} high, sails filled by
	 * {@code billow}. On the shade buffer the hull ({@code hull} lit to {@code hullDark}) and the sails ({@code sail}) are filled; on the
	 * glow buffer the same ship is drawn as lit lines (rails, keel, ribs, masts, yards, sail edges), so a ghost ship is all outline.
	 */
	void galleon(final Vector3fc c, final Vector3fc fwd, final float length, final float beam, final float depth, final int masts, final float mast,
		final float billow, final int hull, final int hullDark, final int sail) {
		Vector3f f = new Vector3f(fwd).normalize();
		Vector3f s = across(f);
		Vector3f up = new Vector3f(s).cross(f).normalize();
		int stations = 10;
		Vector3f[][] gun = new Vector3f[2][stations + 1];
		Vector3f[][] rail = new Vector3f[2][stations + 1];
		Vector3f[][] bilge = new Vector3f[2][stations + 1];
		Vector3f[][] keel = new Vector3f[2][stations + 1];
		for (int i = 0; i <= stations; i++) {
			float t = -1.0F + 2.0F * i / stations;
			float x = t * length / 2;
			float w = beam / 2 * (t > 0 ? Math.max(0.0F, 1.0F - t * t) : 1.0F - 0.22F * t * t);
			float sheer = depth * 0.35F * t * t + (t < -0.55F ? depth * 0.55F : 0.0F);
			float k = t > 0.7F ? 1.0F - (t - 0.7F) / 0.3F * 0.75F : 1.0F;
			for (int side = 0; side < 2; side++) {
				float sg = side == 0 ? 1.0F : -1.0F;
				gun[side][i] = local(c, f, s, up, x, sg * w, sheer);
				rail[side][i] = local(c, f, s, up, x, sg * w * 0.97F, sheer + depth * 0.42F);
				bilge[side][i] = local(c, f, s, up, x * 0.98F, sg * w * 0.82F, -depth * 0.55F * k);
				keel[side][i] = local(c, f, s, up, x * 0.95F, sg * w * 0.18F, -depth * k);
			}
		}
		int hullMid = lerp(hullDark, hull, 0.55F);
		for (int side = 0; side < 2; side++) {
			for (int i = 0; i < stations; i++) {
				if (this.glow) {
					this.bar(gun[side][i], gun[side][i + 1], 0.035F, 0.035F, hull, hull);
					this.bar(rail[side][i], rail[side][i + 1], 0.03F, 0.03F, hull, hull);
					this.bar(keel[side][i], keel[side][i + 1], 0.025F, 0.025F, hullDark, hullDark);
					if (i % 2 == 0) {
						this.bar(rail[side][i], gun[side][i], 0.02F, 0.02F, hull, hull);
						this.bar(gun[side][i], bilge[side][i], 0.02F, 0.02F, hullMid, hullMid);
						this.bar(bilge[side][i], keel[side][i], 0.02F, 0.02F, hullDark, hullDark);
					}
				} else {
					this.quad(rail[side][i], rail[side][i + 1], gun[side][i + 1], gun[side][i], hullMid);
					this.quad(gun[side][i], gun[side][i + 1], bilge[side][i + 1], bilge[side][i], hull, hull, hullMid, hullMid);
					this.quad(bilge[side][i], bilge[side][i + 1], keel[side][i + 1], keel[side][i], hullMid, hullMid, hullDark, hullDark);
				}
			}
		}
		if (!this.glow) {
			// the keel's bottom and the transom across the stern
			for (int i = 0; i < stations; i++) {
				this.quad(keel[0][i], keel[0][i + 1], keel[1][i + 1], keel[1][i], hullDark);
			}
			this.quad(rail[0][0], rail[1][0], gun[1][0], gun[0][0], hullMid);
			this.quad(gun[0][0], gun[1][0], bilge[1][0], bilge[0][0], hullMid);
			this.quad(bilge[0][0], bilge[1][0], keel[1][0], keel[0][0], hullDark);
		} else {
			this.bar(rail[0][0], rail[1][0], 0.03F, 0.03F, hull, hull);
			this.bar(gun[0][0], gun[1][0], 0.03F, 0.03F, hull, hull);
		}
		// the bowsprit
		Vector3f bow = gun[0][stations];
		this.bar(bow, new Vector3f(f).mul(length * 0.28F).add(new Vector3f(up).mul(length * 0.12F)).add(bow), 0.05F, 0.03F, hull, hull);
		// masts, yards and sails
		float[] spots = masts >= 3 ? new float[] {0.38F, -0.08F, -0.5F} : masts == 2 ? new float[] {-0.1F, -0.55F} : new float[] {-0.15F};
		float[] heights = masts >= 3 ? new float[] {0.82F, 1.0F, 0.72F} : masts == 2 ? new float[] {1.0F, 0.75F} : new float[] {1.0F};
		for (int m = 0; m < spots.length; m++) {
			float x = spots[m] * length / 2;
			float h = mast * heights[m];
			Vector3f foot = local(c, f, s, up, x, 0.0F, 0.0F);
			Vector3f top = local(c, f, s, up, x, 0.0F, h);
			this.bar(foot, top, 0.07F, 0.045F, hull, hull);
			for (int y = 0; y < 2; y++) {
				float y0 = h * (y == 0 ? 0.3F : 0.66F);
				float y1 = h * (y == 0 ? 0.62F : 0.92F);
				float w0 = beam * (y == 0 ? 0.95F : 0.7F);
				float w1 = beam * (y == 0 ? 0.75F : 0.5F);
				this.bar(local(c, f, s, up, x, -w1, y1), local(c, f, s, up, x, w1, y1), 0.03F, 0.03F, hull, hull);
				this.sail(c, f, s, up, x, y0, y1, w0, w1, billow * (1.0F - 0.3F * y), sail);
			}
			// the pennant at the top
			Vector3f pole = new Vector3f(top);
			this.tri(pole, local(c, f, s, up, x - length * 0.12F, 0.0F, h - 0.12F), local(c, f, s, up, x, 0.0F, h - 0.25F), sail, sail & 0x00FFFFFF | 0x40000000, sail);
		}
	}

	/** A point {@code x} along, {@code y} across and {@code z} up a ship's frame. */
	private static Vector3f local(final Vector3fc c, final Vector3fc f, final Vector3fc s, final Vector3fc up, final float x, final float y, final float z) {
		return new Vector3f(f).mul(x).add(new Vector3f(s).mul(y)).add(new Vector3f(up).mul(z)).add(c);
	}

	/** One square sail from {@code y0} (its foot, {@code w0} half wide) to {@code y1} up, bellied out forward by {@code billow}. */
	private void sail(final Vector3fc c, final Vector3fc f, final Vector3fc s, final Vector3fc up, final float x, final float y0, final float y1, final float w0,
		final float w1, final float billow, final int color) {
		int n = 4;
		Vector3f[][] p = new Vector3f[n + 1][n + 1];
		for (int i = 0; i <= n; i++) {
			float u = (float)i / n;
			for (int j = 0; j <= n; j++) {
				float v = (float)j / n;
				float w = Mth.lerp(v, w0, w1);
				float bulge = billow * Mth.sin(u * Mth.PI) * (0.4F + 0.6F * Mth.sin(v * Mth.PI));
				p[i][j] = local(c, f, s, up, x + bulge, (u * 2 - 1) * w, Mth.lerp(v, y0, y1));
			}
		}
		if (this.glow) {
			for (int i = 0; i < n; i++) {
				this.bar(p[i][0], p[i + 1][0], 0.025F, 0.025F, color, color);
				this.bar(p[i][n], p[i + 1][n], 0.025F, 0.025F, color, color);
				this.bar(p[0][i], p[0][i + 1], 0.025F, 0.025F, color, color);
				this.bar(p[n][i], p[n][i + 1], 0.025F, 0.025F, color, color);
				this.bar(p[n / 2][i], p[n / 2][i + 1], 0.015F, 0.015F, color, color);
			}
		} else {
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					int k = lerp(color, color & 0xFF000000 | 0x9A9080, 0.25F * (1.0F - Mth.sin((i + 0.5F) / n * Mth.PI)));
					this.quad(p[i][j], p[i + 1][j], p[i + 1][j + 1], p[i][j + 1], k);
				}
			}
		}
	}

	/** Where gun port {@code k} of {@code ports} on one side of a {@link #galleon} sits ({@code side} 1 starboard, -1 port), just under the rail. */
	static Vector3f gunPort(final Vector3fc c, final Vector3fc fwd, final float length, final float beam, final float depth, final int k, final int ports,
		final int side) {
		Vector3f f = new Vector3f(fwd).normalize();
		Vector3f s = across(f);
		Vector3f up = new Vector3f(s).cross(f).normalize();
		float t = -0.45F + 0.85F * (k + 0.5F) / ports;
		float w = beam / 2 * (t > 0 ? Math.max(0.0F, 1.0F - t * t) : 1.0F - 0.22F * t * t);
		return local(c, f, s, up, t * length / 2, side * (w + 0.05F), depth * 0.35F * t * t - depth * 0.12F);
	}

	/**
	 * A bird of light at {@code c} flying along {@code dir} (its back towards {@code up}): a slim body and head, a forked tail and two
	 * wings {@code size} long beating with {@code flap} (-1 down to 1 up).
	 */
	void bird(final Vector3fc c, final Vector3fc dir, final Vector3fc up, final float size, final float flap, final int body, final int wing) {
		Vector3f f = new Vector3f(dir).normalize();
		Vector3f u = new Vector3f(up).sub(new Vector3f(f).mul(f.dot(up)));
		if (u.lengthSquared() < 1.0E-6F) {
			u = new Vector3f(f).cross(across(f));
		}
		u.normalize();
		Vector3f s = new Vector3f(f).cross(u).normalize();
		this.diamond(c, f, s, size * 0.9F, size * 0.22F, body, body);
		this.diamond(new Vector3f(c).add(new Vector3f(f).mul(size * 0.45F)), f, s, size * 0.25F, size * 0.16F, body, body);
		for (int k = -1; k <= 1; k += 2) {
			// the tail's two prongs
			Vector3f tail = new Vector3f(f).mul(-size * 0.42F).add(c);
			this.tri(tail, new Vector3f(f).mul(-size * 0.85F).add(new Vector3f(s).mul(k * size * 0.22F)).add(c), new Vector3f(f).mul(-size * 0.62F).add(c), body,
				body & 0xFFFFFF, body);
			// each wing in two parts, the outer one bending further with the beat
			Vector3f root = new Vector3f(f).mul(size * 0.08F).add(c);
			Vector3f rootBack = new Vector3f(f).mul(-size * 0.2F).add(c);
			float a = flap * 0.7F;
			Vector3f elbow = new Vector3f(s).mul(k * size * 0.55F * Mth.cos(a)).add(new Vector3f(u).mul(size * 0.55F * Mth.sin(a))).add(c);
			float b = flap * 1.1F;
			Vector3f tip = new Vector3f(s).mul(k * size * 0.6F * Mth.cos(b)).add(new Vector3f(u).mul(size * 0.6F * Mth.sin(b))).add(elbow)
				.add(new Vector3f(f).mul(-size * 0.25F));
			this.quad(root, elbow, new Vector3f(elbow).add(new Vector3f(f).mul(-size * 0.35F)), rootBack, wing, wing, wing, wing);
			this.tri(elbow, tip, new Vector3f(elbow).add(new Vector3f(f).mul(-size * 0.35F)), wing, wing & 0xFFFFFF | 0x30000000, wing);
		}
	}

	/** The kinks of a jagged bolt from {@code from} to {@code to} ({@link #bolt}'s shape), so one bolt can be drawn on both buffers. */
	static java.util.List<Vector3f> zigzag(final Vector3fc from, final Vector3fc to, final int seed, final float jag, final int steps) {
		Vector3f d = new Vector3f(to).sub(from);
		Vector3f u = across(d.lengthSquared() < 1.0E-6F ? new Vector3f(0, 1, 0) : new Vector3f(d).normalize());
		Vector3f v = new Vector3f(d).cross(u).normalize();
		java.util.List<Vector3f> points = new java.util.ArrayList<>(steps + 1);
		points.add(new Vector3f(from));
		for (int i = 1; i <= steps; i++) {
			float t = (float)i / steps;
			Vector3f next = new Vector3f(d).mul(t).add(from);
			if (i < steps) {
				float k = jag * Mth.sin(t * Mth.PI);
				next.add(new Vector3f(u).mul((hash(seed * 17 + i) - 0.5F) * 2 * k)).add(new Vector3f(v).mul((hash(seed * 29 + i) - 0.5F) * 2 * k));
			}
			points.add(next);
		}
		return points;
	}

	/** A polyline of bars {@code width} thick ({@link #zigzag}'s bolts on the shade buffer: black lightning). */
	void polyline(final java.util.List<Vector3f> points, final float width, final int color) {
		for (int i = 0; i + 1 < points.size(); i++) {
			this.bar(points.get(i), points.get(i + 1), width, width, color, color);
		}
	}

	/** A polyline of glow lines {@code width} wide ({@link #zigzag}'s bolts lit). */
	void glowPolyline(final java.util.List<Vector3f> points, final float width, final int color) {
		for (int i = 0; i + 1 < points.size(); i++) {
			this.glowLine(points.get(i), points.get(i + 1), width, color, color);
		}
	}

	// ------------------------------------------------------------ the rogue's: Sharingan eyes, kunai, the gunbai, shadow soldiers, insect wings

	/** {@link #eye} patterns: the three tomoe, Itachi's Mangekyo, Kakashi's (Kamui) Mangekyo. */
	static final int SHARINGAN = 0;
	static final int ITACHI = 1;
	static final int KAKASHI = 2;

	/**
	 * A Sharingan of radius {@code r} round {@code c} in the plane of {@code a} and {@code b} (unit vectors), turned by {@code spin}: the iris
	 * {@code iris} (deeper at the pupil) under the {@code pattern} in {@code ink}, laid out in small polar cells of one colour each (blended
	 * where an edge crosses them) so nothing lies over anything else (translucent quads sort by their middles). For the shade buffer.
	 */
	void eye(final Vector3fc c, final Vector3fc a, final Vector3fc b, final float r, final float spin, final int pattern, final int iris, final int ink) {
		int rings = r > 1.0F ? 22 : r > 0.4F ? 16 : 10;
		int sectors = r > 1.0F ? 96 : r > 0.4F ? 72 : 44;
		int deep = lerp(iris, iris & 0xFF000000 | 0x3A0008, 0.55F);
		Vector3f[][] grid = new Vector3f[rings + 1][sectors + 1];
		for (int i = 0; i <= rings; i++) {
			float rr = r * i / rings;
			for (int j = 0; j <= sectors; j++) {
				float t = Mth.TWO_PI * j / sectors;
				grid[i][j] = new Vector3f(a).mul(Mth.cos(t) * rr).add(new Vector3f(b).mul(Mth.sin(t) * rr)).add(c);
			}
		}
		float dr = 1.0F / rings;
		float dt = Mth.TWO_PI / sectors;
		for (int i = 0; i < rings; i++) {
			for (int j = 0; j < sectors; j++) {
				float r0 = i * dr;
				float t0 = j * dt;
				int hits = 0;
				for (int k = 0; k < 5; k++) {
					float fr = k == 4 ? 0.5F : k % 2 == 0 ? 0.25F : 0.75F;
					float ft = k == 4 ? 0.5F : k < 2 ? 0.25F : 0.75F;
					if (inked(pattern, r0 + fr * dr, t0 + ft * dt - spin)) {
						hits++;
					}
				}
				int base = lerp(deep, iris, r0 + dr * 0.5F);
				int color = lerp(base, ink, hits / 5.0F);
				this.quad(grid[i][j], grid[i + 1][j], grid[i + 1][j + 1], grid[i][j + 1], color);
			}
		}
	}

	/** Whether the point {@code rho} (0 middle .. 1 rim) out at angle {@code theta} of a Sharingan lies in its black pattern. */
	private static boolean inked(final int pattern, final float rho, final float theta) {
		if (rho > 0.95F) {
			return true;
		}
		switch (pattern) {
			case ITACHI -> {
				// a small pupil, a black ring round it and three curved blades sweeping out to points at the rim
				if (rho < 0.11F || rho > 0.27F && rho < 0.35F) {
					return true;
				}
				if (rho < 0.3F || rho > 0.9F) {
					return false;
				}
				float k = (rho - 0.3F) / 0.6F;
				float w = 0.62F * (1.0F - k) * (1.0F - k) + 0.03F;
				for (int i = 0; i < 3; i++) {
					if (Math.abs(wrap(theta - (Mth.TWO_PI * i / 3 + 1.5F * k))) < w) {
						return true;
					}
				}
				return false;
			}
			case KAKASHI -> {
				// a pupil and three curved scythe blades wound into a pinwheel
				if (rho < 0.13F) {
					return true;
				}
				if (rho > 0.9F) {
					return false;
				}
				float k = (rho - 0.1F) / 0.8F;
				float w = 0.48F * Mth.sin(Mth.PI * Math.min(1.0F, k * 1.15F)) + 0.04F;
				for (int i = 0; i < 3; i++) {
					float mid = Mth.TWO_PI * i / 3 + 2.6F * k;
					float d = wrap(theta - mid);
					// the blade's trailing edge is hollowed into a crescent
					if (d > -w && d < w * (0.25F + 0.75F * k)) {
						return true;
					}
				}
				return false;
			}
			default -> {
				// three tomoe on a thin ring round the pupil, their tails trailing behind
				if (rho < 0.17F || Math.abs(rho - 0.58F) < 0.016F) {
					return true;
				}
				for (int i = 0; i < 3; i++) {
					float head = Mth.TWO_PI * i / 3;
					float x = rho * Mth.cos(theta) - 0.58F * Mth.cos(head);
					float y = rho * Mth.sin(theta) - 0.58F * Mth.sin(head);
					if (x * x + y * y < 0.135F * 0.135F) {
						return true;
					}
					float back = wrap(head - theta);
					if (back > 0.0F && back < 0.6F) {
						float q = back / 0.6F;
						if (Math.abs(rho - (0.58F + 0.1F * q)) < 0.12F * (1.0F - q)) {
							return true;
						}
					}
				}
				return false;
			}
		}
	}

	/** {@code angle} brought into -pi..pi. */
	static float wrap(final float angle) {
		float a = angle % Mth.TWO_PI;
		if (a > Mth.PI) {
			a -= Mth.TWO_PI;
		} else if (a < -Mth.PI) {
			a += Mth.TWO_PI;
		}
		return a;
	}

	/**
	 * Minato's Flying Thunder God kunai from {@code tail} (the ring of its pommel) to {@code tip}, flat in the plane of {@code side}: a long
	 * middle blade, a shorter prong swept forward on each side of it, a wrapped grip and the ring; {@code steel} the metal, {@code grip} the
	 * wrapping.
	 */
	void kunai(final Vector3fc tail, final Vector3fc tip, final Vector3fc side, final int steel, final int grip) {
		Vector3f d = new Vector3f(tip).sub(tail);
		float len = d.length();
		if (len < 1.0E-4F) {
			return;
		}
		d.div(len);
		Vector3f s = new Vector3f(side).sub(new Vector3f(d).mul(d.dot(side)));
		if (s.lengthSquared() < 1.0E-6F) {
			s = across(d);
		}
		s.normalize();
		Vector3f guard = new Vector3f(d).mul(len * 0.42F).add(tail);
		this.blade(guard, tip, s, len * 0.085F, steel, steel);
		for (int k = -1; k <= 1; k += 2) {
			Vector3f root = new Vector3f(d).mul(len * 0.45F).add(new Vector3f(s).mul(k * len * 0.05F)).add(tail);
			Vector3f prong = new Vector3f(d).mul(len * 0.74F).add(new Vector3f(s).mul(k * len * 0.19F)).add(tail);
			this.blade(root, prong, s, len * 0.04F, steel, steel);
		}
		this.bar(new Vector3f(d).mul(len * 0.11F).add(tail), guard, len * 0.035F, len * 0.035F, grip, grip);
		Vector3f n = new Vector3f(d).cross(s).normalize();
		this.circle(new Vector3f(d).mul(len * 0.06F).add(tail), d, n, len * 0.055F, len * 0.012F, 10, steel);
	}

	/**
	 * Madara's gunbai: a round war fan {@code r} across round {@code c}, its face towards {@code normal} and its top towards {@code up}: the
	 * upper part red and the lower white (the Uchiha crest's colours) in wedges that never overlap, a dark rim, and the handle running down
	 * {@code r} long from it. Faded by {@code alpha}. For the shade buffer.
	 */
	void gunbai(final Vector3fc c, final Vector3fc normal, final Vector3fc up, final float r, final float alpha) {
		Vector3f n = new Vector3f(normal).normalize();
		Vector3f u = new Vector3f(up).sub(new Vector3f(n).mul(n.dot(up)));
		if (u.lengthSquared() < 1.0E-6F) {
			u = new Vector3f(n).cross(across(n));
		}
		u.normalize();
		Vector3f s = new Vector3f(n).cross(u).normalize();
		int red = argb(0.96F * alpha, 0xB0241C);
		int white = argb(0.96F * alpha, 0xEDE6DA);
		int rim = argb(0.96F * alpha, 0x1A1210);
		int segs = 28;
		Vector3f prev = null;
		Vector3f prevOut = null;
		for (int i = 0; i <= segs; i++) {
			float t = Mth.TWO_PI * i / segs;
			// a little taller than wide, like the real fan
			Vector3f p = new Vector3f(s).mul(Mth.cos(t) * r).add(new Vector3f(u).mul(Mth.sin(t) * r * 1.12F)).add(c);
			Vector3f out = new Vector3f(s).mul(Mth.cos(t) * r * 1.09F).add(new Vector3f(u).mul(Mth.sin(t) * r * 1.21F)).add(c);
			if (prev != null) {
				float mid = Mth.sin(t - Mth.PI / segs);
				this.tri(c, prev, p, mid > -0.3F ? red : white, mid > -0.3F ? red : white, mid > -0.3F ? red : white);
				this.quad(prev, prevOut, out, p, rim);
			}
			prev = p;
			prevOut = out;
		}
		Vector3f bottom = new Vector3f(u).mul(-r * 1.2F).add(c);
		this.bar(bottom, new Vector3f(u).mul(-r * 2.2F).add(c), r * 0.08F, r * 0.07F, argb(0.96F * alpha, 0x3A2416), argb(0.96F * alpha, 0x22140C));
	}

	/**
	 * One of the Shadow Monarch's soldiers, {@code h} tall, standing at {@code foot} facing {@code ahead}: legs, an armoured body with
	 * pauldrons, a helmed head with a crest, a sword held down in the right hand and, {@code shield}, a kite shield on the left arm; shaded
	 * {@code lit} to {@code dark}. Its eyes are at {@link #soldierEye}.
	 */
	void shadowSoldier(final Vector3fc foot, final Vector3fc ahead, final float h, final boolean shield, final int lit, final int dark) {
		Vector3f f = new Vector3f(ahead.x(), 0, ahead.z());
		if (f.lengthSquared() < 1.0E-6F) {
			f.set(0, 0, 1);
		}
		f.normalize();
		Vector3f s = new Vector3f(f).cross(0, 1, 0).normalize();
		for (int k = -1; k <= 1; k += 2) {
			Vector3f hip = point(foot, s, f, h, k * 0.09F, 0.48F, 0.0F);
			Vector3f knee = point(foot, s, f, h, k * 0.1F, 0.25F, 0.03F);
			Vector3f sole = point(foot, s, f, h, k * 0.11F, 0.0F, 0.0F);
			this.tube(java.util.List.of(hip, knee, sole), h * 0.07F, h * 0.05F, 6, lit, dark);
			// the pauldrons and the arms hanging from them
			Vector3f shoulder = point(foot, s, f, h, k * 0.21F, 0.82F, 0.0F);
			this.ellipsoid(shoulder, new Vector3f(s).mul(h * 0.09F), new Vector3f(0, h * 0.06F, 0), new Vector3f(f).mul(h * 0.08F), 4, 8, lit, dark);
			Vector3f elbow = point(foot, s, f, h, k * 0.25F, 0.65F, 0.02F);
			Vector3f hand = point(foot, s, f, h, k * 0.25F, 0.5F, 0.08F);
			this.tube(java.util.List.of(shoulder, elbow, hand), h * 0.05F, h * 0.04F, 6, lit, dark);
		}
		this.ellipsoid(point(foot, s, f, h, 0.0F, 0.67F, 0.0F), new Vector3f(s).mul(h * 0.17F), new Vector3f(0, h * 0.2F, 0), new Vector3f(f).mul(h * 0.1F), 5, 10, lit,
			dark);
		Vector3f head = point(foot, s, f, h, 0.0F, 0.94F, 0.0F);
		this.ellipsoid(head, new Vector3f(s).mul(h * 0.075F), new Vector3f(0, h * 0.09F, 0), new Vector3f(f).mul(h * 0.085F), 5, 8, lit, dark);
		// the helmet's crest swept back
		this.blade(point(foot, s, f, h, 0.0F, 1.0F, 0.03F), point(foot, s, f, h, 0.0F, 1.08F, -0.16F), f, h * 0.03F, lit, dark);
		// the sword, point down by the right foot
		Vector3f grip = point(foot, s, f, h, 0.26F, 0.5F, 0.1F);
		this.blade(grip, point(foot, s, f, h, 0.3F, 0.05F, 0.2F), f, h * 0.035F, lit, dark);
		this.bar(point(foot, s, f, h, 0.2F, 0.52F, 0.1F), point(foot, s, f, h, 0.32F, 0.52F, 0.1F), h * 0.012F, h * 0.012F, dark, dark);
		if (shield) {
			Vector3f c = point(foot, s, f, h, -0.3F, 0.6F, 0.12F);
			Vector3f up = new Vector3f(0, 1, 0);
			Vector3f top = new Vector3f(up).mul(h * 0.2F).add(c);
			Vector3f low = new Vector3f(up).mul(-h * 0.26F).add(c);
			Vector3f left = new Vector3f(s).mul(-h * 0.13F).add(new Vector3f(up).mul(h * 0.1F)).add(c);
			Vector3f right = new Vector3f(s).mul(h * 0.13F).add(new Vector3f(up).mul(h * 0.1F)).add(c);
			int face = lerp(dark, lit, 0.35F);
			this.quad(top, right, low, left, face, face, dark, face);
		}
	}

	/** Where the {@code side} (-1 or 1) eye of a {@link #shadowSoldier} is. */
	static Vector3f soldierEye(final Vector3fc foot, final Vector3fc ahead, final float h, final int side) {
		Vector3f f = new Vector3f(ahead.x(), 0, ahead.z());
		if (f.lengthSquared() < 1.0E-6F) {
			f.set(0, 0, 1);
		}
		f.normalize();
		Vector3f s = new Vector3f(f).cross(0, 1, 0).normalize();
		return point(foot, s, f, h, side * 0.03F, 0.955F, 0.08F);
	}

	private static Vector3f point(final Vector3fc foot, final Vector3f s, final Vector3f f, final float h, final float x, final float y, final float z) {
		return new Vector3f(s).mul(x * h).add(new Vector3f(f).mul(z * h)).add(0, y * h, 0).add(foot);
	}

	/**
	 * An insect's wing from {@code root} out along {@code out}, {@code length} long and {@code width} at its widest, in the plane of
	 * {@code out} and {@code back} (the side its trailing edge bulges to): a long rounded membrane in {@code membrane}, and, {@code vein} not
	 * clear, its veins (a leading edge and three branching lines) in that colour.
	 */
	void insectWing(final Vector3fc root, final Vector3fc out, final Vector3fc back, final float length, final float width, final int membrane, final int vein) {
		Vector3f d = new Vector3f(out).normalize();
		Vector3f b = new Vector3f(back).sub(new Vector3f(d).mul(d.dot(back)));
		if (b.lengthSquared() < 1.0E-6F) {
			b = across(d);
		}
		b.normalize();
		int n = 10;
		Vector3f[] lead = new Vector3f[n + 1];
		Vector3f[] trail = new Vector3f[n + 1];
		for (int i = 0; i <= n; i++) {
			float t = (float)i / n;
			float w = width * Mth.sin(Mth.PI * Math.min(1.0F, (float)Math.pow(t, 0.7F)));
			Vector3f m = new Vector3f(d).mul(length * t).add(root);
			lead[i] = new Vector3f(b).mul(-w * 0.18F).add(m);
			trail[i] = new Vector3f(b).mul(w * 0.82F).add(m);
		}
		if ((membrane >>> 24) > 0) {
			for (int i = 0; i < n; i++) {
				this.quad(lead[i], lead[i + 1], trail[i + 1], trail[i], membrane);
			}
		}
		if ((vein >>> 24) == 0) {
			return;
		}
		float vw = Math.max(0.008F, width * 0.035F);
		for (int i = 0; i < n; i++) {
			this.bar(lead[i], lead[i + 1], vw, vw, vein, vein);
		}
		for (int k = 1; k <= 3; k++) {
			int from = 1 + k;
			int to = Math.min(n, from + 4 + k);
			this.bar(lead[from], new Vector3f(trail[to]).lerp(lead[to], 0.15F), vw * 0.7F, vw * 0.5F, vein, vein);
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
