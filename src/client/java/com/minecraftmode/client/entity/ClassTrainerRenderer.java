package com.minecraftmode.client.entity;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.MinecraftModeClient;
import com.minecraftmode.entity.ClassTrainer;
import com.minecraftmode.job.JobClass;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;

/** Class trainers: humanoid model, one skin per class, signature weapon raised. */
public class ClassTrainerRenderer extends HumanoidMobRenderer<ClassTrainer, ClassTrainerRenderer.State, HumanoidModel<ClassTrainerRenderer.State>> {
	private static final Map<JobClass, Identifier> TEXTURES = new EnumMap<>(JobClass.class);

	static {
		for (JobClass job : JobClass.PLAYABLE) {
			TEXTURES.put(job, MinecraftMode.id("textures/entity/trainer/" + job.id() + ".png"));
		}
	}

	public static class State extends HumanoidRenderState {
		JobClass job = JobClass.WARRIOR;
	}

	public ClassTrainerRenderer(final EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(MinecraftModeClient.MINE_RAIDER_LAYER)), 0.5F);
	}

	@Override
	protected HumanoidModel.ArmPose getArmPose(final ClassTrainer mob, final HumanoidArm arm) {
		return mob.getItemHeldByArm(arm).isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(final ClassTrainer entity, final State state, final float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.job = entity.job();
	}

	@Override
	public Identifier getTextureLocation(final State state) {
		return TEXTURES.getOrDefault(state.job, TEXTURES.get(JobClass.WARRIOR));
	}
}
