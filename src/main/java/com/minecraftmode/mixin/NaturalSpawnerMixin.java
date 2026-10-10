package com.minecraftmode.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.minecraftmode.city.CityServices;
import com.minecraftmode.entity.SpawnCandidates;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No hostile natural spawns inside the capital's walls or on its outskirts, and no creepers anywhere (see ModEntities). The type picked
 * for a spawn group only comes from the mobs that could spawn at that spot ({@link SpawnCandidates}).
 */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {
	@Inject(method = "isValidSpawnPostitionForType", at = @At("HEAD"), cancellable = true)
	private static void minecraftMode$safeCity(
		final ServerLevel level,
		final MobCategory category,
		final StructureManager structureManager,
		final ChunkGenerator generator,
		final MobSpawnSettings.SpawnerData data,
		final BlockPos.MutableBlockPos pos,
		final double nearestPlayerDistanceSqr,
		final CallbackInfoReturnable<Boolean> cir
	) {
		if (data.type() == EntityTypes.CREEPER || CityServices.blocksSpawn(level, pos, category == MobCategory.MONSTER || data.type().getCategory() == MobCategory.MONSTER)) {
			cir.setReturnValue(false);
		}
	}

	@WrapOperation(
		method = "getRandomSpawnMobAt",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner;mobsAt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/util/random/WeightedList;")
	)
	private static WeightedList<MobSpawnSettings.SpawnerData> minecraftMode$onlyFitting(
		final ServerLevel level,
		final StructureManager structureManager,
		final ChunkGenerator generator,
		final MobCategory category,
		final BlockPos pos,
		final Operation<WeightedList<MobSpawnSettings.SpawnerData>> original
	) {
		return SpawnCandidates.filter(original.call(level, structureManager, generator, category, pos), level, pos);
	}
}
