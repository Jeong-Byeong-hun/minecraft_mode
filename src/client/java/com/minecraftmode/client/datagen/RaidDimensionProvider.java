package com.minecraftmode.client.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.minecraftmode.raid.RaidDimension;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Writes the raid dimension's level stem ({@code data/minecraft_mode/dimension/raid.json}): the
 * {@link RaidDimension#TYPE raid dimension type} with a flat generator that has no layers (an empty
 * void) in the void biome. Level stems are not a datagen dynamic registry, so this writes the JSON.
 */
public class RaidDimensionProvider implements DataProvider {
	private final PackOutput output;

	public RaidDimensionProvider(final FabricPackOutput output) {
		this.output = output;
	}

	@Override
	public CompletableFuture<?> run(final CachedOutput cache) {
		JsonObject settings = new JsonObject();
		settings.addProperty("biome", "minecraft:the_void");
		settings.addProperty("features", false);
		settings.addProperty("lakes", false);
		settings.add("layers", new JsonArray());
		JsonObject generator = new JsonObject();
		generator.addProperty("type", "minecraft:flat");
		generator.add("settings", settings);
		JsonObject stem = new JsonObject();
		stem.addProperty("type", RaidDimension.TYPE.identifier().toString());
		stem.add("generator", generator);
		Path path = this.output.getOutputFolder(PackOutput.Target.DATA_PACK)
			.resolve(RaidDimension.LEVEL.identifier().getNamespace())
			.resolve("dimension")
			.resolve(RaidDimension.LEVEL.identifier().getPath() + ".json");
		return DataProvider.saveStable(cache, stem, path);
	}

	@Override
	public String getName() {
		return "Raid dimension";
	}
}
