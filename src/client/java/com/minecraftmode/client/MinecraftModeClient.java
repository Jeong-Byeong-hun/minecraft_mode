package com.minecraftmode.client;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.entity.MineRaiderRenderer;
import com.minecraftmode.client.entity.MythrilGolemRenderer;
import com.minecraftmode.client.job.EngravingScreen;
import com.minecraftmode.client.job.JobHud;
import com.minecraftmode.client.job.JobKeys;
import com.minecraftmode.client.job.JobTooltips;
import com.minecraftmode.client.particle.SkillParticle;
import com.minecraftmode.job.skill.Fx;
import com.minecraftmode.registry.ModEntities;
import com.minecraftmode.registry.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class MinecraftModeClient implements ClientModInitializer {
	public static final ModelLayerLocation MINE_RAIDER_LAYER = new ModelLayerLocation(MinecraftMode.id("mine_raider"), "main");

	@Override
	public void onInitializeClient() {
		// Standard 64x64 humanoid (player/zombie) layout.
		ModelLayerRegistry.registerModelLayer(MINE_RAIDER_LAYER, () -> LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64));
		EntityRenderers.register(ModEntities.MINE_RAIDER, MineRaiderRenderer::new);
		EntityRenderers.register(ModEntities.MYTHRIL_GOLEM, MythrilGolemRenderer::new);
		// Skill projectiles show their item (or nothing) at full brightness; the trail is particles.
		EntityRenderers.register(ModEntities.SKILL_PROJECTILE, context -> new ThrownItemRenderer<>(context, 1.0F, true));

		for (Fx.Kind kind : Fx.Kind.values()) {
			ParticleProviderRegistry.getInstance().register(kind.type(), sprites -> new SkillParticle.Provider(sprites, kind));
		}

		MenuScreens.register(ModMenus.ENGRAVING, EngravingScreen::new);
		JobKeys.init();
		JobHud.init();
		JobTooltips.init();
	}
}
