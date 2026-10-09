package com.minecraftmode.entity.named;

import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * Where a named monster lives: its biomes, a height band, and the total monster spawn weight of those
 * biomes (used to turn the wanted share of spawns into a spawn weight).
 */
public enum Habitat {
	ANY_OVERWORLD(BiomeSelectors.foundInOverworld(), -64, 320, 515),
	GRASSLAND(of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW, Biomes.FLOWER_FOREST, Biomes.FOREST, Biomes.BIRCH_FOREST), 50, 320, 515),
	DRYLAND(of(Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA, Biomes.PLAINS, Biomes.TAIGA, Biomes.WINDSWEPT_HILLS), 50, 320, 515),
	SWAMP(of(Biomes.SWAMP, Biomes.MANGROVE_SWAMP), 50, 320, 615),
	SNOW(of(Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA, Biomes.GROVE, Biomes.SNOWY_SLOPES, Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS, Biomes.ICE_SPIKES), 50, 320, 515),
	DESERT(of(Biomes.DESERT, Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS), 50, 320, 515),
	CAVES(BiomeSelectors.foundInOverworld(), -64, 40, 515),
	GLOOM(of(Biomes.DARK_FOREST, Biomes.MUSHROOM_FIELDS, Biomes.PALE_GARDEN), 50, 320, 515),
	JUNGLE(of(Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE), 50, 320, 515),
	COAST(of(Biomes.BEACH, Biomes.STONY_SHORE, Biomes.SNOWY_BEACH, Biomes.MANGROVE_SWAMP), 50, 320, 515),
	BADLANDS(of(Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS, Biomes.SAVANNA_PLATEAU), 50, 320, 515),
	TAIGA(of(Biomes.TAIGA, Biomes.SNOWY_TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.GROVE), 50, 320, 515),
	DEEP_CAVES(of(Biomes.DRIPSTONE_CAVES, Biomes.LUSH_CAVES), -64, 30, 515),
	OLD_FOREST(of(Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.FOREST, Biomes.DARK_FOREST), 50, 320, 515),
	NETHER_WASTES(of(Biomes.NETHER_WASTES, Biomes.BASALT_DELTAS), -64, 320, 168),
	SOUL_VALLEY(of(Biomes.SOUL_SAND_VALLEY), -64, 320, 71),
	CRIMSON(of(Biomes.CRIMSON_FOREST, Biomes.NETHER_WASTES), -64, 320, 150),
	WARPED(of(Biomes.WARPED_FOREST), -64, 320, 10),
	DEEP_DARK(of(Biomes.DEEP_DARK), -64, 0, 10),
	END_HIGHLANDS(of(Biomes.END_HIGHLANDS, Biomes.END_MIDLANDS, Biomes.END_BARRENS), -64, 320, 10),
	END_ISLANDS(of(Biomes.END_HIGHLANDS, Biomes.END_MIDLANDS, Biomes.SMALL_END_ISLANDS, Biomes.END_BARRENS), -64, 320, 10);

	private final Predicate<BiomeSelectionContext> biomes;
	private final int minY;
	private final int maxY;
	private final int totalWeight;

	Habitat(final Predicate<BiomeSelectionContext> biomes, final int minY, final int maxY, final int totalWeight) {
		this.biomes = biomes;
		this.minY = minY;
		this.maxY = maxY;
		this.totalWeight = totalWeight;
	}

	@SafeVarargs
	private static Predicate<BiomeSelectionContext> of(final ResourceKey<Biome>... keys) {
		return BiomeSelectors.includeByKey(List.of(keys));
	}

	public Predicate<BiomeSelectionContext> biomes() {
		return this.biomes;
	}

	public boolean allows(final int y) {
		return y >= this.minY && y < this.maxY;
	}

	/**
	 * Spawn weight for a wanted share of the biomes' monster spawns. Halved because several named
	 * monsters share biomes and each is also capped at one per 96 blocks.
	 */
	public int weight(final float share) {
		return Math.max(1, Math.round(share / (1.0F - share) * this.totalWeight * 0.5F));
	}
}
