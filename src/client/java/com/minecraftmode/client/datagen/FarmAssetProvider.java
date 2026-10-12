package com.minecraftmode.client.datagen;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.datagen.art.FarmArtist;
import com.minecraftmode.craft.Crops;
import com.minecraftmode.craft.Fishing;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Draws the crop, seed and fish icons and the crop block stages ({@link FarmArtist}), and a review sheet of all of them at
 * build/farm-preview.png (each crop: produce, seed, stages 0-3; then the fish).
 */
public class FarmAssetProvider implements DataProvider {
	private final PackOutput output;

	public FarmAssetProvider(final FabricPackOutput output) {
		this.output = output;
	}

	@Override
	public CompletableFuture<?> run(final CachedOutput cache) {
		Path assets = this.output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(MinecraftMode.MOD_ID);
		List<CompletableFuture<?>> writes = new ArrayList<>();
		List<BufferedImage> sheet = new ArrayList<>();
		for (Crops.CropDef def : Crops.all()) {
			BufferedImage produce = FarmArtist.produce(def);
			writes.add(ConsumableAssetProvider.write(cache, assets.resolve("textures/item/" + def.id() + ".png"), produce));
			sheet.add(produce);
			if (Crops.seed(def) != null) {
				BufferedImage seed = FarmArtist.seed(def);
				writes.add(ConsumableAssetProvider.write(cache, assets.resolve("textures/item/" + def.id() + "_seeds.png"), seed));
				sheet.add(seed);
			} else {
				sheet.add(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
			}
			for (int stage = 0; stage < 4; stage++) {
				BufferedImage block = FarmArtist.stage(def, stage);
				writes.add(ConsumableAssetProvider.write(cache, assets.resolve("textures/block/" + def.id() + "_crop_stage" + stage + ".png"), block));
				sheet.add(block);
			}
			// one crop per row of the ten-wide sheet
			for (int pad = 0; pad < 4; pad++) {
				sheet.add(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
			}
		}
		for (Fishing.FishDef def : Fishing.all()) {
			BufferedImage fish = FarmArtist.fish(def);
			writes.add(ConsumableAssetProvider.write(cache, assets.resolve("textures/item/" + def.id() + ".png"), fish));
			sheet.add(fish);
		}
		ConsumableAssetProvider.preview(this.output, sheet, "farm-preview.png");
		return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
	}

	@Override
	public String getName() {
		return "Crop and fish textures";
	}
}
