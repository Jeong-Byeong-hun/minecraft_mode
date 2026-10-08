package com.minecraftmode.client.entity;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.entity.MythrilGolem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.resources.Identifier;

/**
 * Reuses the iron golem model geometry and animation with the mod's own mythril texture.
 */
public class MythrilGolemRenderer extends MobRenderer<MythrilGolem, IronGolemRenderState, IronGolemModel> {
	private static final Identifier TEXTURE = MinecraftMode.id("textures/entity/mythril_golem.png");

	public MythrilGolemRenderer(final EntityRendererProvider.Context context) {
		super(context, new IronGolemModel(context.bakeLayer(ModelLayers.IRON_GOLEM)), 0.7F);
	}

	@Override
	public Identifier getTextureLocation(final IronGolemRenderState state) {
		return TEXTURE;
	}

	@Override
	public IronGolemRenderState createRenderState() {
		return new IronGolemRenderState();
	}

	@Override
	public void extractRenderState(final MythrilGolem entity, final IronGolemRenderState state, final float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.attackTicksRemaining = entity.getAttackAnimationTick() > 0 ? entity.getAttackAnimationTick() - partialTicks : 0.0F;
	}

	@Override
	protected void setupRotations(final IronGolemRenderState state, final PoseStack poseStack, final float bodyRot, final float entityScale) {
		super.setupRotations(state, poseStack, bodyRot, entityScale);
		// Same lumbering sway as the iron golem
		if (!(state.walkAnimationSpeed < 0.01)) {
			float wp = state.walkAnimationPos + 6.0F;
			float triangleWave = (Math.abs(wp % 13.0F - 6.5F) - 3.25F) / 3.25F;
			poseStack.rotateDegrees(Axis.ZP, 6.5F * triangleWave);
		}
	}
}
