package com.minecraftmode.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.minecraftmode.city.CityGenerator;
import java.util.List;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Builds the capital at 0,0: chunks in the city core skip surface features and structures (ores
 * stay) and get the city after decoration; the surrounding ring is reshaped before decoration.
 */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
	@Inject(method = "applyBiomeDecoration", at = @At("HEAD"))
	private void minecraftMode$blendCity(final WorldGenLevel level, final ChunkAccess chunk, final StructureManager structureManager, final CallbackInfo ci) {
		CityGenerator.beforeDecoration((ChunkGenerator)(Object)this, level, chunk);
	}

	@Inject(method = "applyBiomeDecoration", at = @At("TAIL"))
	private void minecraftMode$buildCity(final WorldGenLevel level, final ChunkAccess chunk, final StructureManager structureManager, final CallbackInfo ci) {
		CityGenerator.afterDecoration((ChunkGenerator)(Object)this, level, chunk);
	}

	@WrapOperation(
		method = "applyBiomeDecoration",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/StructureManager;shouldGenerateStructures()Z")
	)
	private boolean minecraftMode$noStructuresInCity(
		final StructureManager structureManager, final Operation<Boolean> original, @Local(argsOnly = true) final WorldGenLevel level,
		@Local(argsOnly = true) final ChunkAccess chunk
	) {
		return original.call(structureManager) && !CityGenerator.isCore((ChunkGenerator)(Object)this, level, chunk);
	}

	@WrapOperation(
		method = "applyBiomeDecoration",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/BiomeGenerationSettings;features()Ljava/util/List;")
	)
	private List<HolderSet<PlacedFeature>> minecraftMode$oresOnlyInCity(
		final BiomeGenerationSettings settings, final Operation<List<HolderSet<PlacedFeature>>> original, @Local(argsOnly = true) final WorldGenLevel level,
		@Local(argsOnly = true) final ChunkAccess chunk
	) {
		List<HolderSet<PlacedFeature>> features = original.call(settings);
		return CityGenerator.isCore((ChunkGenerator)(Object)this, level, chunk) ? CityGenerator.filterFeatures(features) : features;
	}
}
