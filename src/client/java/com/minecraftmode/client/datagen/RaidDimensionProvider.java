package com.minecraftmode.client.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.minecraftmode.character.Lobby;
import com.minecraftmode.dungeon.DungeonDimension;
import com.minecraftmode.raid.RaidDimension;
import com.minecraftmode.residence.ResidenceDimension;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Writes the level stems of the raid and dungeon dimensions ({@code data/minecraft_mode/dimension/raid.json}, {@code dungeon.json}):
 * each dimension type with a flat generator that has no layers (an empty void) in the void biome; and the residence
 * ({@code residence.json}): flat plains on the overworld dimension type; and the character lobby ({@code lobby.json}): a void on the
 * overworld dimension type (sky and daylight behind the select screen). Level stems are not a datagen
 * dynamic registry, so this writes the JSON.
 */
public class RaidDimensionProvider implements DataProvider {
	private final PackOutput output;

	public RaidDimensionProvider(final FabricPackOutput output) {
		this.output = output;
	}

	@Override
	public CompletableFuture<?> run(final CachedOutput cache) {
		return CompletableFuture.allOf(
			this.stem(cache, RaidDimension.LEVEL, RaidDimension.TYPE),
			this.stem(cache, DungeonDimension.LEVEL, DungeonDimension.TYPE),
			this.stem(cache, Lobby.LEVEL, BuiltinDimensionTypes.OVERWORLD),
			this.residence(cache));
	}

	private CompletableFuture<?> stem(final CachedOutput cache, final ResourceKey<Level> level, final ResourceKey<DimensionType> type) {
		JsonObject settings = new JsonObject();
		settings.addProperty("biome", "minecraft:the_void");
		settings.addProperty("features", false);
		settings.addProperty("lakes", false);
		settings.add("layers", new JsonArray());
		return this.write(cache, level, type.identifier().toString(), settings);
	}

	/** Grassland on the overworld dimension type: bedrock, stone, dirt and grass at y {@link ResidenceDimension#SURFACE_Y}; no structures. */
	private CompletableFuture<?> residence(final CachedOutput cache) {
		JsonArray layers = new JsonArray();
		int dirt = ResidenceDimension.DIRT_DEPTH;
		layers.add(layer("minecraft:bedrock", 1));
		layers.add(layer("minecraft:stone", ResidenceDimension.SURFACE_Y - dirt - ResidenceDimension.BOTTOM_Y - 1));
		layers.add(layer("minecraft:dirt", dirt));
		layers.add(layer("minecraft:grass_block", 1));
		JsonObject settings = new JsonObject();
		settings.addProperty("biome", "minecraft:plains");
		settings.addProperty("features", false);
		settings.addProperty("lakes", false);
		settings.add("layers", layers);
		settings.add("structure_overrides", new JsonArray());
		return this.write(cache, ResidenceDimension.LEVEL, "minecraft:overworld", settings);
	}

	private static JsonObject layer(final String block, final int height) {
		JsonObject layer = new JsonObject();
		layer.addProperty("block", block);
		layer.addProperty("height", height);
		return layer;
	}

	private CompletableFuture<?> write(final CachedOutput cache, final ResourceKey<Level> level, final String type, final JsonObject settings) {
		JsonObject generator = new JsonObject();
		generator.addProperty("type", "minecraft:flat");
		generator.add("settings", settings);
		JsonObject stem = new JsonObject();
		stem.addProperty("type", type);
		stem.add("generator", generator);
		Path path = this.output.getOutputFolder(PackOutput.Target.DATA_PACK)
			.resolve(level.identifier().getNamespace())
			.resolve("dimension")
			.resolve(level.identifier().getPath() + ".json");
		return DataProvider.saveStable(cache, stem, path);
	}

	@Override
	public String getName() {
		return "Raid, dungeon, residence and lobby dimensions";
	}
}
