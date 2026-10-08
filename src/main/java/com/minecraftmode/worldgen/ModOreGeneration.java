package com.minecraftmode.worldgen;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.registry.ModBlocks;
import java.util.List;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

/**
 * Features and placements are data-driven: the bootstraps run during datagen,
 * {@link #init()} injects the placed features into overworld biomes at runtime.
 */
public final class ModOreGeneration {
	public static final ResourceKey<Feature> ORE_MYTHRIL = ResourceKey.create(Registries.FEATURE, MinecraftMode.id("ore_mythril"));
	public static final ResourceKey<Feature> ORE_ALUMINUM = ResourceKey.create(Registries.FEATURE, MinecraftMode.id("ore_aluminum"));

	public static final ResourceKey<PlacedFeature> ORE_MYTHRIL_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, MinecraftMode.id("ore_mythril"));
	public static final ResourceKey<PlacedFeature> ORE_ALUMINUM_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, MinecraftMode.id("ore_aluminum"));

	public static void bootstrapFeatures(final BootstrapContext<Feature> context) {
		// Mythril: mid-sized veins, partly discarded when exposed to air.
		// Measured in a normal world: ~35 blocks per chunk (diamond ~24, iron ~94).
		context.register(ORE_MYTHRIL, new OreFeature(oreTargets(ModBlocks.MYTHRIL_ORE, ModBlocks.DEEPSLATE_MYTHRIL_ORE), 7, 0.3F));
		// Aluminum: iron-sized veins.
		context.register(ORE_ALUMINUM, new OreFeature(oreTargets(ModBlocks.ALUMINUM_ORE, ModBlocks.DEEPSLATE_ALUMINUM_ORE), 9));
	}

	public static void bootstrapPlacedFeatures(final BootstrapContext<PlacedFeature> context) {
		HolderGetter<Feature> features = context.lookup(Registries.FEATURE);

		// 8 attempts per chunk, Y -64..16, most common around Y -24.
		PlacementUtils.register(
			context,
			ORE_MYTHRIL_PLACED,
			features.getOrThrow(ORE_MYTHRIL),
			List.of(
				CountPlacement.of(8),
				InSquarePlacement.spread(),
				HeightRangePlacement.triangle(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(16)),
				BiomeFilter.biome()
			)
		);
		// 12 attempts per chunk, Y -16..112, most common around Y 48.
		PlacementUtils.register(
			context,
			ORE_ALUMINUM_PLACED,
			features.getOrThrow(ORE_ALUMINUM),
			List.of(
				CountPlacement.of(12),
				InSquarePlacement.spread(),
				HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(112)),
				BiomeFilter.biome()
			)
		);
	}

	public static void init() {
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES, ORE_MYTHRIL_PLACED);
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES, ORE_ALUMINUM_PLACED);
	}

	private static List<BlockReplacement> oreTargets(final Block stoneOre, final Block deepslateOre) {
		RuleTest stone = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
		RuleTest deepslate = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
		return List.of(
			BlockReplacement.replace(stone, stoneOre.defaultBlockState()),
			BlockReplacement.replace(deepslate, deepslateOre.defaultBlockState())
		);
	}

	private ModOreGeneration() {
	}
}
