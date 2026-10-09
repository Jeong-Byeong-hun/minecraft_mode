package com.minecraftmode.client.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.minecraftmode.dungeon.DungeonDimension;
import com.minecraftmode.raid.RaidDimension;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Writes the level stems of the raid and dungeon dimensions ({@code data/minecraft_mode/dimension/raid.json}, {@code dungeon.json}):
 * each dimension type with a flat generator that has no layers (an empty void) in the void biome. Level stems are not a datagen
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
			this.stem(cache, DungeonDimension.LEVEL, DungeonDimension.TYPE));
	}

	private CompletableFuture<?> stem(final CachedOutput cache, final ResourceKey<Level> level, final ResourceKey<DimensionType> type) {
		JsonObject settings = new JsonObject();
		settings.addProperty("biome", "minecraft:the_void");
		settings.addProperty("features", false);
		settings.addProperty("lakes", false);
		settings.add("layers", new JsonArray());
		JsonObject generator = new JsonObject();
		generator.addProperty("type", "minecraft:flat");
		generator.add("settings", settings);
		JsonObject stem = new JsonObject();
		stem.addProperty("type", type.identifier().toString());
		stem.add("generator", generator);
		Path path = this.output.getOutputFolder(PackOutput.Target.DATA_PACK)
			.resolve(level.identifier().getNamespace())
			.resolve("dimension")
			.resolve(level.identifier().getPath() + ".json");
		return DataProvider.saveStable(cache, stem, path);
	}

	@Override
	public String getName() {
		return "Raid and dungeon dimensions";
	}
}
