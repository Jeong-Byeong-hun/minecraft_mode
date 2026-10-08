package com.minecraftmode.registry;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.entity.MineRaider;
import com.minecraftmode.entity.MythrilGolem;
import com.minecraftmode.job.skill.SkillProjectile;
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

	public static final ResourceKey<EntityType<?>> MYTHRIL_GOLEM_KEY = ResourceKey.create(Registries.ENTITY_TYPE, MinecraftMode.id("mythril_golem"));

	public static final EntityType<MythrilGolem> MYTHRIL_GOLEM = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		MYTHRIL_GOLEM_KEY,
		EntityType.Builder.<MythrilGolem>of(MythrilGolem::new, MobCategory.MONSTER)
			.sized(1.4F, 2.7F)
			.clientTrackingRange(10)
			.notInPeaceful()
			.build(MYTHRIL_GOLEM_KEY)
	);

	public static final ResourceKey<EntityType<?>> SKILL_PROJECTILE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, MinecraftMode.id("skill_projectile"));

	/** Shuriken, bullets, magic orbs... fired by class weapons. Never saved. */
	public static final EntityType<SkillProjectile> SKILL_PROJECTILE = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		SKILL_PROJECTILE_KEY,
		EntityType.Builder.<SkillProjectile>of(SkillProjectile::new, MobCategory.MISC)
			.sized(0.35F, 0.35F)
			.clientTrackingRange(6)
			.updateInterval(5)
			.noSave()
			.build(SKILL_PROJECTILE_KEY)
	);

	public static void init() {
		FabricDefaultAttributeRegistry.register(MYTHRIL_GOLEM, MythrilGolem.createAttributes());
		SpawnPlacements.register(MYTHRIL_GOLEM, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MythrilGolem::checkMythrilGolemSpawnRules);
		// Very rare: below Y=0 only and never two within 64 blocks.
		BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, MYTHRIL_GOLEM, 3, 1, 1);

		FabricDefaultAttributeRegistry.register(MINE_RAIDER, MineRaider.createAttributes());
		SpawnPlacements.register(MINE_RAIDER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MineRaider::checkMineRaiderSpawnRules);
		// Spawn checks reject Y >= 40, so in practice they only appear in caves. The weight is zombie-level
		// because most spawn attempts land above Y=40; at weight 40 only ~1.4% of monsters were raiders.
		BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, MINE_RAIDER, 100, 1, 2);
	}

	private ModEntities() {
	}
}
