package com.minecraftmode.client.vfx;

import com.minecraftmode.job.skill.SkillLook;
import com.minecraftmode.job.skill.SkillVfx;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Draws {@link SkillVfx} looks. Rasengan: a white-hot core in a blue sphere wound with spinning spiral bands and two tilted orbit rings,
 * held at the caster's right hand (in first person low right of the view). Its burst: the same spiral sphere swelling out to the hit
 * radius while it fades, a flash at the middle and a shock ring running along the ground. The Soul Reaper bankai looks are drawn by
 * {@link BladeLooks}, {@link NatureLooks} and {@link SpiritLooks}.
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
		Vector3fc reach = entity.reach();
		AABB box = super.getBoundingBoxForCulling(entity, partialTicks);
		box = box.expandTowards(reach.x(), reach.y(), reach.z());
		double grow = entity.size() + 3.0;
		if (entity.look().hold() == SkillLook.Hold.TRAIL) {
			grow += 24.0;
		}
		return box.inflate(grow, grow + 10.0, grow);
	}

	@Override
	public void extractRenderState(final SkillVfx entity, final State state, final float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.look = entity.look();
		state.color = entity.color();
		state.size = entity.size();
		state.life = Math.max(1, entity.life());
		state.arg = entity.arg();
		state.seed = entity.getId();
		state.progress = Mth.clamp((entity.tickCount + partialTicks) / state.life, 0.0F, 1.0F);
		state.reach.set(entity.reach());
		state.forward.set(state.reach.x(), 0.0F, state.reach.z());
		if (state.forward.lengthSquared() < 1.0E-6F) {
			state.forward.set(0.0F, 0.0F, 1.0F);
		}
		state.forward.normalize();
		state.offset = Vec3.ZERO;
		state.bodyYaw = entity.getYRot();
		state.selfView = false;
		state.trail.clear();
		Entity anchor = entity.anchor();
		Vec3 origin = new Vec3(state.x, state.y, state.z);
		if (anchor != null) {
			Vec3 at = anchor.getPosition(partialTicks);
			Minecraft minecraft = Minecraft.getInstance();
			state.selfView = anchor == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson();
			if (anchor instanceof LivingEntity living) {
				state.bodyYaw = Mth.rotLerp(partialTicks, living.yBodyRotO, living.yBodyRot);
			}
			switch (state.look.hold()) {
				case HAND -> {
					// in front of the chest, a little to the right, where the view looks
					float yaw = anchor.getViewYRot(partialTicks) * Mth.DEG_TO_RAD;
					Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
					Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
					Vec3 hand = at.add(0.0, anchor.getEyeHeight() - 0.4, 0.0).add(forward.scale(0.8)).add(right.scale(0.36));
					state.offset = hand.subtract(origin);
				}
				case FOLLOW -> state.offset = at.subtract(origin);
				case TRAIL -> {
					for (Vec3 p : entity.trail) {
						state.trail.add(new Vector3f((float)(p.x - origin.x), (float)(p.y - origin.y), (float)(p.z - origin.z)));
					}
					state.trail.add(new Vector3f((float)(at.x - origin.x), (float)(at.y - origin.y), (float)(at.z - origin.z)));
				}
				default -> {
				}
			}
		}
	}

	@Override
	public void submit(final State state, final PoseStack poseStack, final SubmitNodeCollector collector, final CameraRenderState camera) {
		state.camRight.set(1.0F, 0.0F, 0.0F).rotate(camera.orientation);
		state.camUp.set(0.0F, 1.0F, 0.0F).rotate(camera.orientation);
		poseStack.pushPose();
		poseStack.translate(state.offset.x, state.offset.y, state.offset.z);
		switch (state.look) {
			case RASENGAN -> submitHeld(state, poseStack, collector, camera);
			case RASENGAN_BURST -> submitBurst(state, poseStack, collector, camera);
			case FLASH_TRAIL -> BladeLooks.flashTrail(state, poseStack, collector);
			case SLASH -> BladeLooks.slash(state, poseStack, collector);
			case CROSS_SLASH -> BladeLooks.crossSlash(state, poseStack, collector);
			case BONE_LASH -> BladeLooks.boneLash(state, poseStack, collector);
			case ZANKA -> BladeLooks.zanka(state, poseStack, collector);
			case SPEAR, SPEAR_KILL -> BladeLooks.spear(state, poseStack, collector);
			case HAKUTEIKEN -> BladeLooks.hakuteiken(state, poseStack, collector);
			case MUGETSU -> BladeLooks.mugetsu(state, poseStack, collector);
			case BONE_SNAKE -> BladeLooks.boneSnake(state, poseStack, collector);
			case PETAL_STORM -> NatureLooks.petalStorm(state, poseStack, collector);
			case PETAL_SPHERE -> NatureLooks.petalSphere(state, poseStack, collector);
			case SENKEI -> NatureLooks.senkei(state, poseStack, collector);
			case ICE_FLOWER -> NatureLooks.iceFlower(state, poseStack, collector);
			case ICE_PRISON -> NatureLooks.icePrison(state, poseStack, collector);
			case ICE_WINGS -> NatureLooks.iceWings(state, poseStack, collector);
			case ICE_BLOOM -> NatureLooks.iceBloom(state, poseStack, collector);
			case FLAME_BURST -> NatureLooks.flameBurst(state, poseStack, collector);
			case FLAME_WALL -> NatureLooks.flameWall(state, poseStack, collector);
			case ABYSS -> NatureLooks.abyss(state, poseStack, collector);
			case REIATSU -> SpiritLooks.reiatsu(state, poseStack, collector);
			case HOGYOKU -> SpiritLooks.hogyoku(state, poseStack, collector);
			case MIRROR -> SpiritLooks.mirror(state, poseStack, collector);
			case KUROHITSUGI -> SpiritLooks.kurohitsugi(state, poseStack, collector);
			case DUST -> SpiritLooks.dust(state, poseStack, collector);
			case BINDING -> SpiritLooks.binding(state, poseStack, collector);
			case MUGETSU_VOID -> SpiritLooks.mugetsuVoid(state, poseStack, collector);
			case BONE_FANGS -> SpiritLooks.boneFangs(state, poseStack, collector);
			case BONE_CAGE -> SpiritLooks.boneCage(state, poseStack, collector);
			default -> {
			}
		}
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}

	/** Additive light drawn by {@code draw}. */
	static void glow(final PoseStack poseStack, final SubmitNodeCollector collector, final Consumer<VfxOut> draw) {
		collector.submitCustomGeometry(poseStack, VfxDraw.GLOW, (pose, buffer) -> draw.accept(new VfxOut(pose, buffer, true)));
	}

	/** Translucent (darkening or pale) surfaces drawn by {@code draw}. */
	static void shade(final PoseStack poseStack, final SubmitNodeCollector collector, final Consumer<VfxOut> draw) {
		collector.submitCustomGeometry(poseStack, VfxDraw.SHADE, (pose, buffer) -> draw.accept(new VfxOut(pose, buffer, false)));
	}

	private static void submitHeld(final State s, final PoseStack poseStack, final SubmitNodeCollector collector, final CameraRenderState camera) {
		float time = s.ageInTicks;
		// springs into the palm, then hums
		float grow = Math.min(1.0F, 0.3F + s.progress * 4.0F);
		float r = HELD_RADIUS * grow * (1.0F + 0.04F * Mth.sin(time * 3.1F));
		sphere(s, poseStack, collector, camera, r, time, 1.0F, 1.0F);
	}

	private static void submitBurst(final State s, final PoseStack poseStack, final SubmitNodeCollector collector, final CameraRenderState camera) {
		float p = s.progress;
		float out = 1.0F - (1.0F - p) * (1.0F - p) * (1.0F - p);
		float fade = (1.0F - p) * (1.0F - p);
		float r = Math.max(HELD_RADIUS, s.size * out);
		poseStack.pushPose();
		poseStack.translate(0.0F, 1.0F, 0.0F);
		sphere(s, poseStack, collector, camera, r, s.ageInTicks * 1.6F, fade, 1.0F - p * 0.7F);
		poseStack.popPose();
		// the shock running along the ground, a little ahead of the sphere
		float ring = s.size * Math.min(1.0F, p * 1.5F) * 1.1F;
		poseStack.pushPose();
		poseStack.translate(0.0F, 0.2F, 0.0F);
		collector.submitCustomGeometry(poseStack, VfxDraw.GLOW, (pose, buffer) -> {
			VfxDraw.ring(pose, buffer, Math.max(0.0F, ring - 0.7F), ring, s.color, 0.8F * fade);
			VfxDraw.ring(pose, buffer, Math.max(0.0F, ring * 0.6F - 0.3F), ring * 0.6F, VfxDraw.whiten(s.color, 0.6F), 0.5F * fade);
		});
		poseStack.popPose();
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
		int life = 1;
		float arg;
		int seed;
		/** From the entity to where the look is drawn: the holder's hand or feet. */
		Vec3 offset = Vec3.ZERO;
		/** A beam's whole length, a cone's direction times its range, else the caster's flat look. */
		final Vector3f reach = new Vector3f();
		/** The flat direction the caster faced. */
		final Vector3f forward = new Vector3f(0.0F, 0.0F, 1.0F);
		/** The holder's body yaw in degrees (wings). */
		float bodyYaw;
		/** The holder is the camera, in first person: looks around them hold back so they do not fill the view. */
		boolean selfView;
		/** A trail's points from its start (0, 0, 0) to the holder now. */
		final List<Vector3f> trail = new ArrayList<>();
		final Vector3f camRight = new Vector3f(1.0F, 0.0F, 0.0F);
		final Vector3f camUp = new Vector3f(0.0F, 1.0F, 0.0F);

		/** Ticks since it appeared (with the partial tick). */
		float age() {
			return this.progress * this.life;
		}

		/** Dark looks (Tensa Zangetsu's black) glow red and shade black instead of glowing their own colour. */
		boolean dark() {
			return VfxDraw.luma(this.color) < 0.3F;
		}
	}
}
