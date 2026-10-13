package com.minecraftmode.client.datagen;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.accessory.Accessories;
import com.minecraftmode.accessory.AccessoryDef;
import com.minecraftmode.client.datagen.art.AccessoryArtist;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/** Draws the raid accessory icons ({@link AccessoryArtist}) and a review sheet at build/accessory-preview.png (one set per row). */
public class AccessoryAssetProvider implements DataProvider {
	private final PackOutput output;

	public AccessoryAssetProvider(final FabricPackOutput output) {
		this.output = output;
	}

	@Override
	public CompletableFuture<?> run(final CachedOutput cache) {
		Path assets = this.output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(MinecraftMode.MOD_ID);
		List<CompletableFuture<?>> writes = new ArrayList<>();
		List<BufferedImage> sheet = new ArrayList<>();
		int column = 0;
		for (AccessoryDef def : Accessories.all()) {
			BufferedImage icon = AccessoryArtist.draw(def);
			writes.add(ConsumableAssetProvider.write(cache, assets.resolve("textures/item/" + def.id() + ".png"), icon));
			sheet.add(icon);
			// five pieces, then pad the ten-wide sheet to the next row
			if (++column == Accessories.SLOTS) {
				for (int pad = column; pad < 10; pad++) {
					sheet.add(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
				}
				column = 0;
			}
		}
		ConsumableAssetProvider.preview(this.output, sheet, "accessory-preview.png");
		return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
	}

	@Override
	public String getName() {
		return "Raid accessory textures";
	}
}
