package com.minecraftmode.client.vfx;

import com.minecraftmode.job.skill.SkillLook;
import com.minecraftmode.job.skill.SkillProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Skill projectiles: their item at full brightness, or the drawn shape of their {@link SkillLook}. Getsuga Tensho is a crescent
 * standing across its flight, its middle leading and its horns trailing, a white-hot rim over a glowing body and two fading afterimages;
 * off a dark blade (Tensa Zangetsu) the body is black and the glow red.
 */
public class SkillProjectileRenderer extends ThrownItemRenderer<SkillProjectile> {
	/** Outer radius of the crescent's arc and its widest thickness, in blocks (the wave hits 1 block around its path). */
	private static final float RADIUS = 1.45F;
	private static final float THICKNESS = 0.55F;
	/** Half the arc, horn to middle. */
	private static final float HALF_ARC = 72.0F * Mth.DEG_TO_RAD;
	private static final int STEPS = 24;
	private static final int DARK_RIM = 0xE0242E;

	public SkillProjectileRenderer(final EntityRendererProvider.Context context) {
		super(context, 1.0F, true);
	}

	@Override
	public ThrownItemRenderState createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(final SkillProjectile entity, final ThrownItemRenderState state, final float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		State s = (State)state;
		s.look = entity.look();
		s.color = entity.color();
		Vec3 motion = entity.getDeltaMovement();
		if (motion.lengthSqr() < 1.0E-6) {
			motion = Vec3.directionFromRotation(entity.getXRot(), entity.getYRot());
		}
		s.forward.set((float)motion.x, (float)motion.y, (float)motion.z).normalize();
		// waves lean one way or the other, like diagonal sword swings
		int seed = entity.getId() * 0x9E3779B9;
		s.tilt = (0.3F + (seed >>> 8 & 0xFF) / 255.0F * 0.5F) * ((seed & 1) == 0 ? 1 : -1);
		s.mirror = (seed & 2) == 0;
	}

	@Override
	public void submit(final ThrownItemRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
		State s = (State)state;
		switch (s.look) {
			case GETSUGA -> submitGetsuga(s, poseStack, submitNodeCollector, false);
			case GETSUGA_DUAL -> submitGetsuga(s, poseStack, submitNodeCollector, true);
			case TAIMATSU -> submitTaimatsu(s, poseStack, submitNodeCollector, camera);
			default -> {
			}
		}
		super.submit(state, poseStack, submitNodeCollector, camera);
	}

	/**
	 * Taimatsu: a ball of fire, white at the heart, its flames streaming back from the flight in licking tongues, a glow round it all.
	 */
	private static void submitTaimatsu(final State s, final PoseStack poseStack, final SubmitNodeCollector collector, final CameraRenderState camera) {
		Vector3f back = new Vector3f(s.forward).negate();
		Vector3f right = new Vector3f(1.0F, 0.0F, 0.0F).rotate(camera.orientation);
		Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F).rotate(camera.orientation);
		float time = s.ageInTicks;
		float pulse = 1.0F + 0.08F * Mth.sin(time * 2.3F);
		poseStack.pushPose();
		poseStack.translate(0.0F, 0.15F, 0.0F);
		SkillVfxRenderer.glow(poseStack, collector, o -> {
			Vector3f c = new Vector3f();
			o.sprite(c, right, up, 1.6F * pulse, VfxOut.argb(0.55F, 0xFF5A1A));
			o.sprite(c, right, up, 0.85F * pulse, VfxOut.argb(0.9F, 0xFFB050));
			o.sprite(c, right, up, 0.4F, VfxOut.argb(1.0F, 0xFFF4D0));
			for (int i = 0; i < 10; i++) {
				float a = Mth.TWO_PI * i / 10 + time * 0.4F;
				Vector3f sideways = VfxOut.across(back).mul(Mth.cos(a) * 0.3F).add(new Vector3f(VfxOut.across(back)).cross(back).normalize().mul(Mth.sin(a) * 0.3F));
				o.flame(sideways, back, 1.3F + 0.9F * VfxOut.hash(i), 0.5F, time * 0.9F + i * 1.7F, VfxOut.argb(0.85F, 0xFFD27A), VfxOut.argb(0.0F, 0xFF2A00));
			}
		});
		collector.submitCustomGeometry(poseStack, VfxDraw.GLOW, (pose, buffer) -> VfxDraw.ball(pose, buffer, 0.38F * pulse, 0xFF9A3A, 0.35F));
		poseStack.popPose();
	}

	/**
	 * The crescent; {@code dual} (True Zangetsu) adds a black crescent riding inside the white one, the two sides of Ichigo's power.
	 */
	private static void submitGetsuga(final State s, final PoseStack poseStack, final SubmitNodeCollector collector, final boolean dual) {
		Vector3f f = new Vector3f(s.forward);
		Vector3f side = new Vector3f(f).cross(0.0F, 1.0F, 0.0F);
		if (side.lengthSquared() < 1.0E-4F) {
			side.set(1.0F, 0.0F, 0.0F);
		}
		side.normalize();
		Vector3f up = new Vector3f(side).cross(f).normalize();
		// the arch opens towards `axis`: mostly sideways (a standing crescent, like a downward swing), leaned by the tilt
		Vector3f axis = new Vector3f(side).mul(Mth.cos(s.tilt)).add(new Vector3f(up).mul(Mth.sin(s.tilt)));
		if (s.mirror) {
			axis.negate();
		}
		float grow = Math.min(1.0F, 0.55F + s.ageInTicks * 0.22F);
		float time = s.ageInTicks;
		boolean dark = VfxDraw.luma(s.color) < 0.3F;
		int glow = dark ? DARK_RIM : s.color;
		poseStack.pushPose();
		poseStack.translate(0.0F, 0.1F, 0.0F);
		// afterimages first: the wave a moment ago, fainter and a little smaller
		collector.submitCustomGeometry(poseStack, VfxDraw.GLOW, (pose, buffer) -> {
			for (int echo = 2; echo >= 1; echo--) {
				float back = echo * 0.55F;
				float alpha = echo == 1 ? 0.38F : 0.16F;
				Vector3f shift = new Vector3f(f).mul(-back);
				crescent(pose, buffer, f, axis, shift, RADIUS * grow * (1 - 0.07F * echo), THICKNESS * grow, glow, glow, alpha, time, 0.0F);
			}
			// a dark body still glows a deep red under its shade, or it vanishes against the night
			crescent(pose, buffer, f, axis, new Vector3f(), RADIUS * grow, THICKNESS * grow, glow, dark ? 0xFF8A80 : VfxDraw.whiten(s.color, dual ? 1.0F : 0.75F), 1.0F, time,
				dark ? 0.45F : 1.0F);
		});
		if (dark) {
			collector.submitCustomGeometry(poseStack, VfxDraw.SHADE, (pose, buffer) -> body(pose, buffer, f, axis, RADIUS * grow, THICKNESS * grow, time));
		} else if (dual) {
			collector.submitCustomGeometry(poseStack, VfxDraw.SHADE, (pose, buffer) -> body(pose, buffer, f, axis, RADIUS * grow * 0.8F, THICKNESS * grow * 0.6F, time));
		}
		poseStack.popPose();
	}

	/** Width of the crescent at angle {@code theta} from its middle: widest there, pointed at the horns. */
	private static float width(final float thickness, final float theta) {
		float t = theta / HALF_ARC;
		return thickness * (float)Math.pow(Math.max(0.0F, 1.0F - t * t), 0.7);
	}

	/**
	 * Point at angle {@code theta} along the arch, {@code r} from its centre. The arch stands across the flight (so it shows from behind)
	 * and its horns trail back along it (so it shows as a crescent from the side); {@code axis} points from the middle of the arc to its
	 * centre, which sits half a radius past the wave's own position so the drawing is centred on what it hits.
	 */
	private static Vector3f at(final Vector3f f, final Vector3f axis, final Vector3f shift, final float radius, final float theta, final float r, final Vector3f out) {
		Vector3f across = new Vector3f(f).cross(axis);
		out.set(shift).add(new Vector3f(axis).mul(radius * 0.5F - r * Mth.cos(theta))).add(across.mul(r * Mth.sin(theta)));
		return out.add(new Vector3f(f).mul(-(1.0F - Mth.cos(theta)) * radius * 0.8F));
	}

	/**
	 * The glowing layers of one crescent: a glow inside and outside its edges, the body ({@code body} of its full glow; a dark body is
	 * shaded by {@link #body} on top) and a hot rim line on the outer edge.
	 */
	private static void crescent(
		final PoseStack.Pose pose, final VertexConsumer buffer, final Vector3f f, final Vector3f axis, final Vector3f shift, final float radius,
		final float thickness, final int glow, final int rim, final float alpha, final float time, final float body
	) {
		Vector3f a = new Vector3f();
		Vector3f b = new Vector3f();
		Vector3f c = new Vector3f();
		Vector3f d = new Vector3f();
		for (int i = 0; i < STEPS; i++) {
			float t0 = -HALF_ARC + 2 * HALF_ARC * i / STEPS;
			float t1 = -HALF_ARC + 2 * HALF_ARC * (i + 1) / STEPS;
			float w0 = width(thickness, t0);
			float w1 = width(thickness, t1);
			// fades out towards the horns and flickers along the arc like a flame
			float l0 = alpha * taper(t0) * shimmer(time, t0);
			float l1 = alpha * taper(t1) * shimmer(time, t1);
			float g0 = 0.42F * w0 / thickness + 0.06F;
			float g1 = 0.42F * w1 / thickness + 0.06F;
			// inner glow
			strip(pose, buffer, f, axis, shift, radius, t0, t1, radius - w0 - g0 * 0.6F, radius - w1 - g1 * 0.6F, radius - w0, radius - w1, glow, glow,
				0.0F, 0.55F * l0, 0.55F * l1, 0.0F, a, b, c, d);
			if (body > 0.0F) {
				strip(pose, buffer, f, axis, shift, radius, t0, t1, radius - w0, radius - w1, radius - 0.07F, radius - 0.07F, glow, VfxDraw.whiten(glow, 0.45F),
					0.6F * body * l0, 0.85F * body * l0, 0.85F * body * l1, 0.6F * body * l1, a, b, c, d);
			}
			// the hot rim
			strip(pose, buffer, f, axis, shift, radius, t0, t1, radius - 0.07F, radius - 0.07F, radius, radius, rim, rim, l0, l0, l1, l1, a, b, c, d);
			// outer glow
			strip(pose, buffer, f, axis, shift, radius, t0, t1, radius, radius, radius + g0, radius + g1, glow, glow, 0.7F * l0, 0.0F, 0.0F, 0.7F * l1, a, b, c, d);
		}
	}

	/** A dark body between the edges (the translucent type, darkening what is behind). */
	private static void body(final PoseStack.Pose pose, final VertexConsumer buffer, final Vector3f f, final Vector3f axis, final float radius, final float thickness, final float time) {
		Vector3f shift = new Vector3f();
		Vector3f a = new Vector3f();
		Vector3f b = new Vector3f();
		Vector3f c = new Vector3f();
		Vector3f d = new Vector3f();
		for (int i = 0; i < STEPS; i++) {
			float t0 = -HALF_ARC + 2 * HALF_ARC * i / STEPS;
			float t1 = -HALF_ARC + 2 * HALF_ARC * (i + 1) / STEPS;
			float w0 = width(thickness, t0);
			float w1 = width(thickness, t1);
			int c0 = VfxDraw.argb(0.8F * taper(t0) * shimmer(time, t0), 0x12060A);
			int c1 = VfxDraw.argb(0.8F * taper(t1) * shimmer(time, t1), 0x12060A);
			at(f, axis, shift, radius, t0, radius - w0, a);
			at(f, axis, shift, radius, t0, radius - 0.04F, b);
			at(f, axis, shift, radius, t1, radius - 0.04F, c);
			at(f, axis, shift, radius, t1, radius - w1, d);
			VfxDraw.face(pose, buffer, a, b, c, d, c0, c0, c1, c1);
		}
	}

	/**
	 * A band of the arc between angles t0..t1, from radius {@code in} to {@code out} (each given at both angles); alphas at the corners
	 * inner t0, outer t0, outer t1, inner t1.
	 */
	private static void strip(
		final PoseStack.Pose pose, final VertexConsumer buffer, final Vector3f f, final Vector3f axis, final Vector3f shift, final float radius, final float t0,
		final float t1, final float in0, final float in1, final float out0, final float out1, final int inColor, final int outColor, final float alphaIn0,
		final float alphaOut0, final float alphaOut1, final float alphaIn1, final Vector3f a, final Vector3f b, final Vector3f c, final Vector3f d
	) {
		at(f, axis, shift, radius, t0, in0, a);
		at(f, axis, shift, radius, t0, out0, b);
		at(f, axis, shift, radius, t1, out1, c);
		at(f, axis, shift, radius, t1, in1, d);
		VfxDraw.quad(pose, buffer, a, b, c, d, VfxDraw.argb(alphaIn0, inColor), VfxDraw.argb(alphaOut0, outColor), VfxDraw.argb(alphaOut1, outColor),
			VfxDraw.argb(alphaIn1, inColor));
	}

	private static float taper(final float theta) {
		float t = Math.abs(theta) / HALF_ARC;
		return Mth.clamp(1.0F - t * t * t, 0.0F, 1.0F);
	}

	private static float shimmer(final float time, final float theta) {
		return 0.82F + 0.18F * Mth.sin(time * 1.9F + theta * 7.0F);
	}

	public static class State extends ThrownItemRenderState {
		SkillLook look = SkillLook.NONE;
		int color;
		float tilt;
		boolean mirror;
		final Vector3f forward = new Vector3f(0.0F, 0.0F, 1.0F);
	}
}
