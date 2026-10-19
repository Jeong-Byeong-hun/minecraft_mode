package com.minecraftmode.client.vfx;

import com.minecraftmode.MinecraftMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Geometry for drawn skill effects, written as position + color quads. {@link #GLOW} adds light (the lightning shader: additive, culled,
 * sorted, but without writing depth, so a glow's clear edges never cut holes in the glow behind them), {@link #SHADE} darkens
 * (translucent, both sides), so a black wave still shows on a dark night.
 */
final class VfxDraw {
	/** Vanilla's lightning pipeline minus the depth write. Built lazily on first use (never touches the GPU in static init). */
	private static final RenderPipeline GLOW_PIPELINE = RenderPipeline.builder(RenderPipelines.LIGHTNING_SNIPPET)
		.withLocation(MinecraftMode.id("pipeline/skill_glow"))
		.withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
		.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
		.build();
	/**
	 * With improved transparency (the Fabulous preset) every blended type goes through order-independent transparency, which throws for a
	 * type without OIT pipelines: vanilla lightning's OIT set (additive, no depth write there either) keeps it drawing.
	 */
	static final RenderType GLOW = RenderType.create("minecraft_mode_skill_glow",
		RenderSetup.builder(GLOW_PIPELINE).setOitPipelines(RenderPipelines.OIT_LIGHTNING).sortOnUpload().createRenderSetup());
	static final RenderType SHADE = RenderTypes.debugQuads();

	/** One quad seen from both sides (the glow type culls back faces). */
	static void quad(
		final PoseStack.Pose pose, final VertexConsumer buffer, final Vector3f a, final Vector3f b, final Vector3f c, final Vector3f d, final int ca, final int cb,
		final int cc, final int cd
	) {
		vertex(pose, buffer, a, ca);
		vertex(pose, buffer, b, cb);
		vertex(pose, buffer, c, cc);
		vertex(pose, buffer, d, cd);
		vertex(pose, buffer, d, cd);
		vertex(pose, buffer, c, cc);
		vertex(pose, buffer, b, cb);
		vertex(pose, buffer, a, ca);
	}

	/** One quad, one side (for {@link #SHADE}, which draws both sides already). */
	static void face(
		final PoseStack.Pose pose, final VertexConsumer buffer, final Vector3f a, final Vector3f b, final Vector3f c, final Vector3f d, final int ca, final int cb,
		final int cc, final int cd
	) {
		vertex(pose, buffer, a, ca);
		vertex(pose, buffer, b, cb);
		vertex(pose, buffer, c, cc);
		vertex(pose, buffer, d, cd);
	}

	private static void vertex(final PoseStack.Pose pose, final VertexConsumer buffer, final Vector3f p, final int color) {
		buffer.addVertex(pose, p.x, p.y, p.z).setColor(color);
	}

	/** {@code rgb} with {@code alpha} (0..1, clamped). */
	static int argb(final float alpha, final int rgb) {
		return ARGB.color(Mth.clamp(alpha, 0.0F, 1.0F), rgb & 0xFFFFFF);
	}

	/** {@code rgb} moved towards white by {@code t}. */
	static int whiten(final int rgb, final float t) {
		return ARGB.srgbLerp(t, rgb | 0xFF000000, 0xFFFFFFFF) & 0xFFFFFF;
	}

	/** Perceived brightness 0..1. */
	static float luma(final int rgb) {
		return (0.299F * (rgb >> 16 & 0xFF) + 0.587F * (rgb >> 8 & 0xFF) + 0.114F * (rgb & 0xFF)) / 255.0F;
	}

	/** Point on a sphere of {@code radius} around the origin: latitude {@code lat} (-pi/2..pi/2), longitude {@code lon}. */
	static Vector3f sphere(final float radius, final float lat, final float lon, final Vector3f out) {
		float c = Mth.cos(lat);
		return out.set(c * Mth.cos(lon) * radius, Mth.sin(lat) * radius, c * Mth.sin(lon) * radius);
	}

	/** A glowing ball: brightest where it faces nobody in particular (the glow type adds both halves up), {@code alpha} at its rim. */
	static void ball(final PoseStack.Pose pose, final VertexConsumer buffer, final float radius, final int rgb, final float alpha) {
		int lats = 8;
		int lons = 14;
		Vector3f a = new Vector3f();
		Vector3f b = new Vector3f();
		Vector3f c = new Vector3f();
		Vector3f d = new Vector3f();
		int color = argb(alpha, rgb);
		for (int i = 0; i < lats; i++) {
			float lat0 = -Mth.HALF_PI + Mth.PI * i / lats;
			float lat1 = -Mth.HALF_PI + Mth.PI * (i + 1) / lats;
			for (int j = 0; j < lons; j++) {
				float lon0 = Mth.TWO_PI * j / lons;
				float lon1 = Mth.TWO_PI * (j + 1) / lons;
				sphere(radius, lat0, lon0, a);
				sphere(radius, lat0, lon1, b);
				sphere(radius, lat1, lon1, c);
				sphere(radius, lat1, lon0, d);
				quad(pose, buffer, a, b, c, d, color, color, color, color);
			}
		}
	}

	/**
	 * A soft disc facing +z (draw it after rotating the pose to the camera): {@code inner} colour in the middle fading to nothing at
	 * {@code radius}.
	 */
	static void halo(final PoseStack.Pose pose, final VertexConsumer buffer, final float radius, final int rgb, final float alpha) {
		int n = 20;
		Vector3f o = new Vector3f();
		Vector3f m0 = new Vector3f();
		Vector3f m1 = new Vector3f();
		Vector3f e0 = new Vector3f();
		Vector3f e1 = new Vector3f();
		int core = argb(alpha, rgb);
		int mid = argb(alpha * 0.45F, rgb);
		int edge = argb(0.0F, rgb);
		for (int i = 0; i < n; i++) {
			float t0 = Mth.TWO_PI * i / n;
			float t1 = Mth.TWO_PI * (i + 1) / n;
			m0.set(Mth.cos(t0) * radius * 0.4F, Mth.sin(t0) * radius * 0.4F, 0.0F);
			m1.set(Mth.cos(t1) * radius * 0.4F, Mth.sin(t1) * radius * 0.4F, 0.0F);
			e0.set(Mth.cos(t0) * radius, Mth.sin(t0) * radius, 0.0F);
			e1.set(Mth.cos(t1) * radius, Mth.sin(t1) * radius, 0.0F);
			quad(pose, buffer, o, m0, m1, o, core, mid, mid, core);
			quad(pose, buffer, m0, e0, e1, m1, mid, edge, edge, mid);
		}
	}

	/** A flat ring at y 0 from {@code inner} to {@code outer} blocks, brightest in the middle of its width. */
	static void ring(final PoseStack.Pose pose, final VertexConsumer buffer, final float inner, final float outer, final int rgb, final float alpha) {
		int n = Mth.clamp((int)(outer * 12), 16, 72);
		float mid = (inner + outer) / 2;
		Vector3f a = new Vector3f();
		Vector3f b = new Vector3f();
		Vector3f c = new Vector3f();
		Vector3f d = new Vector3f();
		int edge = argb(0.0F, rgb);
		int core = argb(alpha, rgb);
		for (int i = 0; i < n; i++) {
			float t0 = Mth.TWO_PI * i / n;
			float t1 = Mth.TWO_PI * (i + 1) / n;
			float c0 = Mth.cos(t0);
			float s0 = Mth.sin(t0);
			float c1 = Mth.cos(t1);
			float s1 = Mth.sin(t1);
			a.set(c0 * inner, 0, s0 * inner);
			b.set(c0 * mid, 0, s0 * mid);
			c.set(c1 * mid, 0, s1 * mid);
			d.set(c1 * inner, 0, s1 * inner);
			quad(pose, buffer, a, b, c, d, edge, core, core, edge);
			a.set(c0 * outer, 0, s0 * outer);
			d.set(c1 * outer, 0, s1 * outer);
			quad(pose, buffer, b, a, d, c, core, edge, edge, core);
		}
	}

	/**
	 * Spiral bands wound over a sphere of {@code radius}, turned by {@code spin}: the Rasengan's swirl. Each band runs pole to pole
	 * {@code turns} times round, {@code width} blocks wide, fading at both ends.
	 */
	static void swirl(
		final PoseStack.Pose pose, final VertexConsumer buffer, final float radius, final int bands, final float turns, final float width, final float spin,
		final int rgb, final float alpha
	) {
		int steps = 18;
		Vector3f p0 = new Vector3f();
		Vector3f p1 = new Vector3f();
		Vector3f side0 = new Vector3f();
		Vector3f side1 = new Vector3f();
		Vector3f a = new Vector3f();
		Vector3f b = new Vector3f();
		Vector3f c = new Vector3f();
		Vector3f d = new Vector3f();
		for (int k = 0; k < bands; k++) {
			float start = spin + Mth.TWO_PI * k / bands;
			for (int i = 0; i < steps; i++) {
				float t0 = (float)i / steps;
				float t1 = (float)(i + 1) / steps;
				bandPoint(radius, start, turns, t0, p0);
				bandPoint(radius, start, turns, t1, p1);
				// across the band: along the surface, square to the band's direction
				Vector3f along = new Vector3f(p1).sub(p0);
				side0.set(p0).cross(along).normalize(width / 2);
				side1.set(p1).cross(along).normalize(width / 2);
				int c0 = argb(alpha * fade(t0), rgb);
				int c1 = argb(alpha * fade(t1), rgb);
				a.set(p0).sub(side0);
				b.set(p0).add(side0);
				c.set(p1).add(side1);
				d.set(p1).sub(side1);
				quad(pose, buffer, a, b, c, d, c0, c0, c1, c1);
			}
		}
	}

	private static void bandPoint(final float radius, final float start, final float turns, final float t, final Vector3f out) {
		float lat = (t - 0.5F) * Mth.PI * 0.92F;
		float lon = start + t * turns * Mth.TWO_PI;
		sphere(radius, lat, lon, out);
	}

	/** 0 at both ends of a band, 1 in the middle. */
	private static float fade(final float t) {
		return Mth.sin(t * Mth.PI);
	}

	private VfxDraw() {
	}
}
