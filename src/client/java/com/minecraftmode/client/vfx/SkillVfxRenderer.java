package com.minecraftmode.client.vfx;

import com.minecraftmode.job.skill.SkillLook;
import com.minecraftmode.job.skill.SkillVfx;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws {@link SkillVfx} looks. Rasengan: a white-hot core in a blue sphere wound with spinning spiral bands and two tilted orbit rings,
 * held at the caster's right hand (in first person low right of the view). Its burst: the same spiral sphere swelling out to the hit
 * radius while it fades, a flash at the middle and a shock ring running along the ground.
 */
public class SkillVfxRenderer extends EntityRenderer<SkillVfx, SkillVfxRenderer.State> {
	/** The held sphere's radius, in blocks. */
	private static final float HELD_RADIUS = 0.42F;

	public SkillVfxRenderer(final EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	protected AABB getBoundingBoxForCulling(final SkillVfx entity, final float partialTicks) {
		return super.getBoundingBoxForCulling(entity, partialTicks).inflate(entity.size() + 2.0);
	}

	@Override
	public void extractRenderState(final SkillVfx entity, final State state, final float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.look = entity.look();
		state.color = entity.color();
		state.size = entity.size();
		state.progress = state.look.ticks() <= 0 ? 1.0F : Mth.clamp((entity.tickCount + partialTicks) / state.look.ticks(), 0.0F, 1.0F);
		state.offset = Vec3.ZERO;
		Entity anchor = entity.anchor();
		if (anchor != null) {
			// in front of the chest, a little to the right, where the view looks
			Vec3 at = anchor.getPosition(partialTicks);
			float yaw = anchor.getViewYRot(partialTicks) * Mth.DEG_TO_RAD;
			Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
			Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
			Vec3 hand = at.add(0.0, anchor.getEyeHeight() - 0.4, 0.0).add(forward.scale(0.8)).add(right.scale(0.36));
			state.offset = hand.subtract(state.x, state.y, state.z);
		}
	}

	@Override
	public void submit(final State state, final PoseStack poseStack, final SubmitNodeCollector collector, final CameraRenderState camera) {
		switch (state.look) {
			case RASENGAN -> submitHeld(state, poseStack, collector, camera);
			case RASENGAN_BURST -> submitBurst(state, poseStack, collector, camera);
			default -> {
			}
		}
		super.submit(state, poseStack, collector, camera);
	}

	private static void submitHeld(final State s, final PoseStack poseStack, final SubmitNodeCollector collector, final CameraRenderState camera) {
		float time = s.ageInTicks;
		// springs into the palm, then hums
		float grow = Math.min(1.0F, 0.3F + s.progress * 4.0F);
		float r = HELD_RADIUS * grow * (1.0F + 0.04F * Mth.sin(time * 3.1F));
		poseStack.pushPose();
		poseStack.translate(s.offset.x, s.offset.y, s.offset.z);
		sphere(s, poseStack, collector, camera, r, time, 1.0F, 1.0F);
		poseStack.popPose();
	}

	private static void submitBurst(final State s, final PoseStack poseStack, final SubmitNodeCollector collector, final CameraRenderState camera) {
		float p = s.progress;
		float out = 1.0F - (1.0F - p) * (1.0F - p) * (1.0F - p);
		float fade = (1.0F - p) * (1.0F - p);
		float r = Math.max(HELD_RADIUS, s.size * out);
		poseStack.pushPose();
		poseStack.translate(0.0F, 0.8F, 0.0F);
		sphere(s, poseStack, collector, camera, r, s.ageInTicks * 1.6F, fade, 1.0F - p * 0.7F);
		poseStack.popPose();
		// the shock running along the ground, a little ahead of the sphere
		float ring = s.size * Math.min(1.0F, p * 1.5F) * 1.1F;
		collector.submitCustomGeometry(poseStack, VfxDraw.GLOW, (pose, buffer) -> {
			VfxDraw.ring(pose, buffer, Math.max(0.0F, ring - 0.7F), ring, s.color, 0.8F * fade);
			VfxDraw.ring(pose, buffer, Math.max(0.0F, ring * 0.6F - 0.3F), ring * 0.6F, VfxDraw.whiten(s.color, 0.6F), 0.5F * fade);
		});
	}

	/**
	 * The spiral sphere of {@code radius}: core, shell, swirl bands and orbit rings, faded by {@code alpha}; {@code core} scales the
	 * white-hot middle (the burst's thins out as it spreads).
	 */
	private static void sphere(
		final State s, final PoseStack poseStack, final SubmitNodeCollector collector, final CameraRenderState camera, final float radius, final float time,
		final float alpha, final float core
	) {
		int color = s.color;
		int pale = VfxDraw.whiten(color, 0.55F);
		// a soft halo facing the camera behind it all
		poseStack.pushPose();
		poseStack.rotate(camera.orientation);
		collector.submitCustomGeometry(poseStack, VfxDraw.GLOW, (pose, buffer) -> {
			VfxDraw.halo(pose, buffer, radius * 2.1F, color, 0.45F * alpha);
			VfxDraw.halo(pose, buffer, radius * 0.9F * core, 0xFFFFFF, 0.9F * alpha * core);
		});
		poseStack.popPose();
		// tumbling a little so the bands never line up into stripes
		poseStack.pushPose();
		poseStack.rotate(Axis.ZP.rotation(0.45F + 0.25F * Mth.sin(time * 0.21F)));
		poseStack.rotate(Axis.XP.rotation(0.3F * Mth.cos(time * 0.17F)));
		collector.submitCustomGeometry(poseStack, VfxDraw.GLOW, (pose, buffer) -> {
			VfxDraw.ball(pose, buffer, radius * 0.42F * core, 0xFFFFFF, 0.5F * alpha);
			VfxDraw.ball(pose, buffer, radius * 0.82F, color, 0.16F * alpha);
			VfxDraw.ball(pose, buffer, radius, pale, 0.07F * alpha);
			// two sets of bands spinning at different speeds, the outer one against the inner
			VfxDraw.swirl(pose, buffer, radius * 0.98F, 7, 1.15F, radius * 0.11F, -time * 0.85F, pale, 0.85F * alpha);
			VfxDraw.swirl(pose, buffer, radius * 0.78F, 5, -0.9F, radius * 0.09F, time * 0.6F, 0xFFFFFF, 0.6F * alpha);
		});
		poseStack.popPose();
		for (int i = 0; i < 2; i++) {
			poseStack.pushPose();
			poseStack.rotate(Axis.YP.rotation(time * (i == 0 ? 0.5F : -0.7F)));
			poseStack.rotate(Axis.XP.rotation(i == 0 ? 1.1F : -0.6F));
			collector.submitCustomGeometry(poseStack, VfxDraw.GLOW, (pose, buffer) -> VfxDraw.ring(pose, buffer, radius * 1.08F, radius * 1.22F, pale, 0.7F * alpha));
			poseStack.popPose();
		}
	}

	public static class State extends EntityRenderState {
		SkillLook look = SkillLook.NONE;
		int color;
		float size;
		float progress;
		Vec3 offset = Vec3.ZERO;
	}
}
