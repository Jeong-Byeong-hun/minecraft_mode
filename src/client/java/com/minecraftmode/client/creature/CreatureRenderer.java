package com.minecraftmode.client.creature;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.entity.AnimatedCreature;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

/**
 * Renders any mob with {@link AnimatedCreature} with the {@link CreatureModel} of its plan, the painted texture
 * {@code textures/entity/creature/<id>.png}, and an emissive layer {@code <id>_glow.png} for eyes,
 * runes and glowing cracks.
 */
public class CreatureRenderer<T extends Mob & AnimatedCreature> extends MobRenderer<T, CreatureRenderState, CreatureModel> {
	private final BodyPlan plan;
	private final Identifier texture;
	/** Per entity: eased gait and the tick it was last updated. Weak keys, so removed entities drop out. */
	private final Map<Entity, float[]> gaits = new WeakHashMap<>();
	private static final float GAIT_EASE = 0.35F;
	private static final double TELEPORT = 1.5;

	public CreatureRenderer(final EntityRendererProvider.Context context, final BodyPlan plan, final ModelLayerLocation layer) {
		super(context, new CreatureModel(context.bakeLayer(layer), plan), 0.5F * plan.scale);
		this.plan = plan;
		this.texture = MinecraftMode.id("textures/entity/creature/" + plan.id + ".png");
		this.addLayer(new Glow(this, MinecraftMode.id("textures/entity/creature/" + plan.id + "_glow.png")));
	}

	@Override
	public CreatureRenderState createRenderState() {
		return new CreatureRenderState();
	}

	@Override
	public void extractRenderState(final T entity, final CreatureRenderState state, final float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.anim = entity.anim();
		state.animTime = entity.animTime(partialTicks);
		state.rest = entity.rest(partialTicks);
		state.resting = state.rest > 0.5F;
		state.fold = entity.wingsFolded(partialTicks);
		state.stand = entity.stand(partialTicks);
		state.flying = entity.isFlyingCreature() && !state.resting;
		state.phase = entity.phase();
		state.gait = this.gait(entity);
		state.roll = entity.roll(partialTicks);
		state.tilt = entity.tilt(partialTicks);
		state.hurt = entity.hurtTime > 0 && entity.hurtDuration > 0 && entity.deathTime == 0
			? Mth.clamp((entity.hurtTime - partialTicks) / entity.hurtDuration, 0.0F, 1.0F)
			: 0.0F;
	}

	/**
	 * Ground speed in blocks per tick, eased over a few ticks so a single jolt (a knockback, a charge's first step) does not flash the
	 * gallop pose; a jump longer than {@link #TELEPORT} in one tick is a teleport and counts as standing still.
	 */
	private float gait(final T entity) {
		float[] eased = this.gaits.computeIfAbsent(entity, e -> new float[] {0.0F, -1.0F});
		if (eased[1] != entity.tickCount) {
			eased[1] = entity.tickCount;
			double moved = Math.hypot(entity.getX() - entity.xo, entity.getZ() - entity.zo);
			eased[0] += ((moved > TELEPORT ? 0.0F : (float)moved) - eased[0]) * GAIT_EASE;
		}
		return eased[0];
	}

	/** Flyers bank and pitch around the middle of their body. */
	@Override
	protected void setupRotations(final CreatureRenderState state, final PoseStack poseStack, final float bodyRot, final float entityScale) {
		super.setupRotations(state, poseStack, bodyRot, entityScale);
		if (state.roll != 0.0F || state.tilt != 0.0F) {
			float mid = state.boundingBoxHeight / 2.0F / entityScale;
			poseStack.translate(0.0F, mid, 0.0F);
			poseStack.rotateDegrees(Axis.ZP, state.roll);
			poseStack.rotateDegrees(Axis.XP, state.tilt);
			poseStack.translate(0.0F, -mid, 0.0F);
		}
	}

	/** Wings, tails and tentacles reach well past the hitbox; do not cull them early. */
	@Override
	protected AABB getBoundingBoxForCulling(final T entity, final float partialTicks) {
		return super.getBoundingBoxForCulling(entity, partialTicks).inflate(this.plan.scale * 3.0F);
	}

	@Override
	public Identifier getTextureLocation(final CreatureRenderState state) {
		return this.texture;
	}

	@Override
	protected void scale(final CreatureRenderState state, final PoseStack poseStack) {
		poseStack.scale(this.plan.scale, this.plan.scale, this.plan.scale);
	}

	/** Eyes, runes and cracks drawn full-bright on top. */
	private static final class Glow extends EyesLayer<CreatureRenderState, CreatureModel> {
		private final RenderType type;

		Glow(final RenderLayerParent<CreatureRenderState, CreatureModel> parent, final Identifier texture) {
			super(parent);
			this.type = RenderTypes.eyes(texture);
		}

		@Override
		public RenderType renderType() {
			return this.type;
		}
	}
}
