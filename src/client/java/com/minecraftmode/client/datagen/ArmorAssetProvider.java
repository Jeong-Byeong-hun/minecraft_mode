package com.minecraftmode.client.datagen;

import com.google.common.hash.Hashing;
import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.datagen.art.ArmorArtist;
import com.minecraftmode.job.gear.ArmorPieceDef;
import com.minecraftmode.job.gear.ArmorSetDef;
import com.minecraftmode.job.gear.ClassArmor;
import com.minecraftmode.job.gear.GearSlot;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Class armor assets: item icons and worn textures drawn by {@link ArmorArtist}, the equipment asset
 * JSON of every set, and a review sheet (icons + worn layers) at build/armor-preview.png.
 */
public class ArmorAssetProvider implements DataProvider {
	private final PackOutput output;

	public ArmorAssetProvider(final FabricPackOutput output) {
		this.output = output;
	}

	@Override
	public CompletableFuture<?> run(final CachedOutput cache) {
		Path assets = this.output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(MinecraftMode.MOD_ID);
		List<CompletableFuture<?>> writes = new ArrayList<>();
		List<BufferedImage> sheet = new ArrayList<>();
		for (ArmorSetDef set : ClassArmor.sets()) {
			Map<GearSlot, BufferedImage> icons = ArmorArtist.icons(set);
			for (ArmorPieceDef piece : ClassArmor.piecesOf(set)) {
				BufferedImage icon = icons.get(piece.slot());
				writes.add(write(cache, assets.resolve("textures/item/" + piece.id() + ".png"), png(icon)));
				sheet.add(icon);
			}
			BufferedImage humanoid = ArmorArtist.layer(set, false);
			BufferedImage leggings = ArmorArtist.layer(set, true);
			writes.add(write(cache, assets.resolve("textures/entity/equipment/humanoid/" + set.id() + ".png"), png(humanoid)));
			writes.add(write(cache, assets.resolve("textures/entity/equipment/humanoid_leggings/" + set.id() + ".png"), png(leggings)));
			String texture = MinecraftMode.MOD_ID + ":" + set.id();
			String json = "{\n  \"layers\": {\n    \"humanoid\": [\n      {\n        \"texture\": \"" + texture + "\"\n      }\n    ],\n"
				+ "    \"humanoid_leggings\": [\n      {\n        \"texture\": \"" + texture + "\"\n      }\n    ]\n  }\n}\n";
			writes.add(write(cache, assets.resolve("equipment/" + set.id() + ".json"), json.getBytes(StandardCharsets.UTF_8)));
		}
		writePreview(sheet);
		return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
	}

	private static CompletableFuture<?> write(final CachedOutput cache, final Path path, final byte[] bytes) {
		return CompletableFuture.runAsync(() -> {
			try {
				cache.writeIfNeeded(path, bytes, Hashing.sha256().hashBytes(bytes));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	static byte[] png(final BufferedImage image) {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			ImageIO.write(image, "png", out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** One row per set: its 4 icons at 6x. Not part of the generated resources. */
	private void writePreview(final List<BufferedImage> icons) {
		int scale = 6;
		int cell = 16 * scale + 6;
		int cols = 8;
		int rows = (icons.size() + cols - 1) / cols;
		BufferedImage sheet = new BufferedImage(cols * cell, Math.max(1, rows) * cell, BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < icons.size(); i++) {
			int ox = (i % cols) * cell;
			int oy = (i / cols) * cell;
			for (int y = 0; y < cell; y++) {
				for (int x = 0; x < cell; x++) {
					sheet.setRGB(ox + x, oy + y, ((x / 6 + y / 6) % 2 == 0) ? 0xFF3A3A3A : 0xFF4A4A4A);
				}
			}
			BufferedImage img = icons.get(i);
			for (int y = 0; y < 16 * scale; y++) {
				for (int x = 0; x < 16 * scale; x++) {
					int argb = img.getRGB(x / scale, y / scale);
					if ((argb >>> 24) > 0) {
						sheet.setRGB(ox + 3 + x, oy + 3 + y, argb);
					}
				}
			}
		}
		try {
			Path root = this.output.getOutputFolder().getParent().getParent().getParent();
			Path file = root.resolve("build/armor-preview.png");
			Files.createDirectories(file.getParent());
			ImageIO.write(sheet, "png", file.toFile());
		} catch (IOException | NullPointerException e) {
			MinecraftMode.LOGGER.warn("Could not write the armor preview", e);
		}
	}

	@Override
	public String getName() {
		return "Class armor assets";
	}
}
