package com.minecraftmode.client.datagen;

import com.google.common.hash.Hashing;
import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.datagen.art.WeaponArtist;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.WeaponDef;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
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
 * Writes the generated class weapon textures ({@link WeaponArtist}) into the resource output, and a
 * contact sheet of all of them to build/weapon-preview.png for review.
 */
public class WeaponTextureProvider implements DataProvider {
	private final PackOutput output;

	public WeaponTextureProvider(final FabricPackOutput output) {
		this.output = output;
	}

	@Override
	public CompletableFuture<?> run(final CachedOutput cache) {
		Path textures = this.output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(MinecraftMode.MOD_ID).resolve("textures/item");
		List<CompletableFuture<?>> writes = new ArrayList<>();
		List<BufferedImage> sheet = new ArrayList<>();
		for (JobClass job : JobClass.PLAYABLE) {
			for (WeaponDef def : JobWeapons.of(job)) {
				for (Map.Entry<String, BufferedImage> entry : WeaponArtist.draw(def).entrySet()) {
					byte[] png = png(entry.getValue());
					Path path = textures.resolve(def.id() + entry.getKey() + ".png");
					writes.add(CompletableFuture.runAsync(() -> {
						try {
							cache.writeIfNeeded(path, png, Hashing.sha256().hashBytes(png));
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					}));
					if (entry.getKey().isEmpty() || entry.getKey().endsWith("_2")) {
						sheet.add(entry.getValue());
					}
				}
			}
		}
		writePreview(sheet);
		return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
	}

	private static byte[] png(final BufferedImage image) {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			ImageIO.write(image, "png", out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** 12 per row, 8x scale, checkerboard behind; not part of the generated resources. */
	private void writePreview(final List<BufferedImage> images) {
		int scale = 8;
		int cell = 16 * scale + 8;
		int cols = 12;
		int rows = (images.size() + cols - 1) / cols;
		BufferedImage sheet = new BufferedImage(cols * cell, Math.max(1, rows) * cell, BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < images.size(); i++) {
			int ox = (i % cols) * cell;
			int oy = (i / cols) * cell;
			BufferedImage img = images.get(i);
			for (int y = 0; y < cell; y++) {
				for (int x = 0; x < cell; x++) {
					boolean dark = ((x / 8) + (y / 8)) % 2 == 0;
					sheet.setRGB(ox + x, oy + y, dark ? 0xFF3A3A3A : 0xFF4A4A4A);
				}
			}
			for (int y = 0; y < 16 * scale; y++) {
				for (int x = 0; x < 16 * scale; x++) {
					int argb = img.getRGB(x / scale, y / scale);
					if ((argb >>> 24) != 0) {
						sheet.setRGB(ox + 4 + x, oy + 4 + y, argb);
					}
				}
			}
		}
		try {
			Path root = this.output.getOutputFolder().getParent().getParent().getParent();
			Path file = root.resolve("build/weapon-preview.png");
			Files.createDirectories(file.getParent());
			ImageIO.write(sheet, "png", file.toFile());
		} catch (IOException | NullPointerException e) {
			MinecraftMode.LOGGER.warn("Could not write weapon preview", e);
		}
	}

	@Override
	public String getName() {
		return "Class weapon textures";
	}
}
