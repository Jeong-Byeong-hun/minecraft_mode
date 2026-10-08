package com.minecraftmode.client;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.entity.MineRaiderRenderer;
import com.minecraftmode.client.entity.MythrilGolemRenderer;
import com.minecraftmode.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;

public class MinecraftModeClient implements ClientModInitializer {
	public static final ModelLayerLocation MINE_RAIDER_LAYER = new ModelLayerLocation(MinecraftMode.id("mine_raider"), "main");

	@Override
	public void onInitializeClient() {
		// Standard 64x64 humanoid (player/zombie) layout.
		ModelLayerRegistry.registerModelLayer(MINE_RAIDER_LAYER, () -> LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64));
		EntityRendererRegistry.register(ModEntities.MINE_RAIDER, MineRaiderRenderer::new);
		EntityRendererRegistry.register(ModEntities.MYTHRIL_GOLEM, MythrilGolemRenderer::new);
	}
}
