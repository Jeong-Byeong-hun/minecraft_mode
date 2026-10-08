package com.minecraftmode.registry;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.entity.MineRaider;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ModEntities {
	public static final ResourceKey<EntityType<?>> MINE_RAIDER_KEY = ResourceKey.create(Registries.ENTITY_TYPE, MinecraftMode.id("mine_raider"));

	public static final EntityType<MineRaider> MINE_RAIDER = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		MINE_RAIDER_KEY,
		EntityType.Builder.<MineRaider>of(MineRaider::new, MobCategory.MONSTER)
			.sized(0.6F, 1.95F)
			.eyeHeight(1.74F)
			.clientTrackingRange(8)
			.notInPeaceful()
			.build(MINE_RAIDER_KEY)
	);

	public static void init() {
		FabricDefaultAttributeRegistry.register(MINE_RAIDER, MineRaider.createAttributes());
		SpawnPlacements.register(MINE_RAIDER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MineRaider::checkMineRaiderSpawnRules);
		// Spawn checks reject Y >= 40, so in practice they only appear in caves.
		BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, MINE_RAIDER, 40, 1, 2);
	}

	private ModEntities() {
	}
}
