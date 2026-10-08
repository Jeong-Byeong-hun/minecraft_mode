package com.minecraftmode.client.entity;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.MinecraftModeClient;
import com.minecraftmode.entity.MineRaider;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;

public class MineRaiderRenderer extends HumanoidMobRenderer<MineRaider, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	private static final Identifier TEXTURE = MinecraftMode.id("textures/entity/mine_raider.png");

	public MineRaiderRenderer(final EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(MinecraftModeClient.MINE_RAIDER_LAYER)), 0.5F);
	}

	@Override
	protected HumanoidModel.ArmPose getArmPose(final MineRaider mob, final HumanoidArm arm) {
		// Hold the pickaxe out instead of letting it dangle.
		return mob.getItemHeldByArm(arm).isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public Identifier getTextureLocation(final HumanoidRenderState state) {
		return TEXTURE;
	}
}
