package com.minecraftmode.client.entity;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.MinecraftModeClient;
import com.minecraftmode.entity.CityNpc;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;

/** City service NPCs: humanoid model, one skin per role. */
public class CityNpcRenderer extends HumanoidMobRenderer<CityNpc, CityNpcRenderer.State, HumanoidModel<CityNpcRenderer.State>> {
	private static final Map<CityNpc.Role, Identifier> TEXTURES = new EnumMap<>(CityNpc.Role.class);

	static {
		for (CityNpc.Role role : CityNpc.Role.values()) {
			TEXTURES.put(role, MinecraftMode.id("textures/entity/npc/" + role.id() + ".png"));
		}
	}

	public static class State extends HumanoidRenderState {
		CityNpc.Role role = CityNpc.Role.BLACKSMITH;
	}

	public CityNpcRenderer(final EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(MinecraftModeClient.MINE_RAIDER_LAYER)), 0.5F);
	}

	@Override
	protected HumanoidModel.ArmPose getArmPose(final CityNpc mob, final HumanoidArm arm) {
		return mob.getItemHeldByArm(arm).isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(final CityNpc entity, final State state, final float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.role = entity.role();
	}

	@Override
	public Identifier getTextureLocation(final State state) {
		return TEXTURES.get(state.role);
	}
}
