package com.minecraftmode.mixin;

import com.minecraftmode.city.CityServices;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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

/** No hostile natural spawns inside the capital's walls or on its outskirts, and no creepers anywhere (see ModEntities). */
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
}
