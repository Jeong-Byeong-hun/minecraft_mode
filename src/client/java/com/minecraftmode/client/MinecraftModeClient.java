package com.minecraftmode.client;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.companion.CompanionClient;
import com.minecraftmode.client.craft.CraftScreen;
import com.minecraftmode.client.creature.CreaturePlans;
import com.minecraftmode.client.creature.CreatureRenderer;
import com.minecraftmode.client.dungeon.DungeonClient;
import com.minecraftmode.client.endgame.EndgameClient;
import com.minecraftmode.client.endgame.EnhanceScreen;
import com.minecraftmode.client.entity.CityNpcRenderer;
import com.minecraftmode.client.entity.ClassTrainerRenderer;
import com.minecraftmode.client.entity.MineRaiderRenderer;
import com.minecraftmode.client.entity.MythrilGolemRenderer;
import com.minecraftmode.client.guide.GuideScreen;
import com.minecraftmode.client.job.EngravingScreen;
import com.minecraftmode.client.job.JobHud;
import com.minecraftmode.client.job.JobKeys;
import com.minecraftmode.client.job.JobTooltips;
import com.minecraftmode.client.job.TrainerScreen;
import com.minecraftmode.client.job.UpgradeScreen;
import com.minecraftmode.client.particle.SkillParticle;
import com.minecraftmode.client.raid.RaidClient;
import com.minecraftmode.entity.CreatureMob;
import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.job.skill.Fx;
import com.minecraftmode.network.OpenGuidePayload;
import com.minecraftmode.network.OpenTrainerPayload;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.registry.ModEntities;
import com.minecraftmode.registry.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.EntityType;

public class MinecraftModeClient implements ClientModInitializer {
	public static final ModelLayerLocation MINE_RAIDER_LAYER = new ModelLayerLocation(MinecraftMode.id("mine_raider"), "main");

	@Override
	public void onInitializeClient() {
		// Standard 64x64 humanoid (player/zombie) layout.
		ModelLayerRegistry.registerModelLayer(MINE_RAIDER_LAYER, () -> LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64));
		EntityRenderers.register(ModEntities.MINE_RAIDER, MineRaiderRenderer::new);
		EntityRenderers.register(ModEntities.MYTHRIL_GOLEM, MythrilGolemRenderer::new);
		EntityRenderers.register(ModEntities.CLASS_TRAINER, ClassTrainerRenderer::new);
		EntityRenderers.register(ModEntities.CITY_NPC, CityNpcRenderer::new);
		CreaturePlans.registerLayers();
		for (NamedDef def : NamedMobs.all()) {
			EntityRenderers.register(NamedMobs.type(def), context -> new CreatureRenderer<>(context, CreaturePlans.get(def.id()), CreaturePlans.layer(def.id())));
		}
		for (BossDef def : RaidBosses.all()) {
			creature(RaidBosses.type(def), def.id());
		}
		CompanionClient.init();
		ClientPlayNetworking.registerGlobalReceiver(OpenGuidePayload.TYPE, (payload, context) -> context.client().execute(
			() -> context.client().gui.setScreen(new GuideScreen())
		));
		ClientPlayNetworking.registerGlobalReceiver(OpenTrainerPayload.TYPE, (payload, context) -> context.client().execute(
			() -> context.client().gui.setScreen(new TrainerScreen(payload.entityId(), payload.job()))
		));
		// Skill projectiles show their item (or nothing) at full brightness; the trail is particles.
		EntityRenderers.register(ModEntities.SKILL_PROJECTILE, context -> new ThrownItemRenderer<>(context, 1.0F, true));
		EntityRenderers.register(ModEntities.MOB_PROJECTILE, context -> new ThrownItemRenderer<>(context, 1.6F, true));

		for (Fx.Kind kind : Fx.Kind.values()) {
			ParticleProviderRegistry.getInstance().register(kind.type(), sprites -> new SkillParticle.Provider(sprites, kind));
		}

		MenuScreens.register(ModMenus.ENGRAVING, EngravingScreen::new);
		MenuScreens.register(ModMenus.UPGRADE, UpgradeScreen::new);
		MenuScreens.register(ModMenus.ENHANCE, EnhanceScreen::new);
		MenuScreens.register(ModMenus.CRAFT_COOKING, CraftScreen::new);
		MenuScreens.register(ModMenus.CRAFT_ALCHEMY, CraftScreen::new);
		MenuScreens.register(ModMenus.CRAFT_SMITHING, CraftScreen::new);
		JobKeys.init();
		JobHud.init();
		JobTooltips.init();
		RaidClient.init();
		EndgameClient.init();
		DungeonClient.init();
		WalletDisplay.init();
		ConsumableTooltips.init();
	}

	private static <T extends CreatureMob> void creature(final EntityType<T> type, final String id) {
		EntityRenderers.register(type, context -> new CreatureRenderer<>(context, CreaturePlans.get(id), CreaturePlans.layer(id)));
	}
}
