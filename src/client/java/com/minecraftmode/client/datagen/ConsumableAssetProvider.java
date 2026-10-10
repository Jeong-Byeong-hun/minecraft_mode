package com.minecraftmode.client.datagen;

import com.google.common.hash.Hashing;
import com.minecraftmode.MinecraftMode;
import com.minecraftmode.bag.BagKind;
import com.minecraftmode.client.datagen.art.ConsumableArtist;
import com.minecraftmode.consumable.BuffEffects;
import com.minecraftmode.consumable.ConsumableDef;
import com.minecraftmode.consumable.Consumables;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.effect.MobEffect;

/**
 * Draws the consumable icons, the bags, the return scroll and the buff effect icons ({@link ConsumableArtist}),
 * and a review sheet of all of them at build/consumable-preview.png.
 */
public class ConsumableAssetProvider implements DataProvider {
	private final PackOutput output;

	public ConsumableAssetProvider(final FabricPackOutput output) {
		this.output = output;
	}

	@Override
	public CompletableFuture<?> run(final CachedOutput cache) {
		Path assets = this.output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(MinecraftMode.MOD_ID);
		List<CompletableFuture<?>> writes = new ArrayList<>();
		List<BufferedImage> sheet = new ArrayList<>();
		for (ConsumableDef def : Consumables.all()) {
			BufferedImage icon = ConsumableArtist.icon(def);
			writes.add(write(cache, assets.resolve("textures/item/" + def.id() + ".png"), icon));
			sheet.add(icon);
		}
		for (BagKind kind : BagKind.values()) {
			BufferedImage bag = ConsumableArtist.bag(kind);
			writes.add(write(cache, assets.resolve("textures/item/" + kind.id() + ".png"), bag));
			sheet.add(bag);
		}
		BufferedImage scroll = ConsumableArtist.scroll();
		writes.add(write(cache, assets.resolve("textures/item/return_scroll.png"), scroll));
		sheet.add(scroll);
		for (Holder<MobEffect> effect : BuffEffects.all()) {
			String id = BuiltInRegistries.MOB_EFFECT.getKey(effect.value()).getPath();
			BufferedImage icon = ConsumableArtist.effectIcon(id, effect.value().getColor() & 0xFFFFFF);
			writes.add(write(cache, assets.resolve("textures/mob_effect/" + id + ".png"), icon));
			sheet.add(icon);
		}
		this.preview(sheet);
		return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
	}

	private static CompletableFuture<?> write(final CachedOutput cache, final Path path, final BufferedImage image) {
		byte[] bytes = ArmorAssetProvider.png(image);
		return CompletableFuture.runAsync(() -> {
			try {
				cache.writeIfNeeded(path, bytes, Hashing.sha256().hashBytes(bytes));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	/** Every icon at 4x on a dark sheet, 10 per row. */
	private void preview(final List<BufferedImage> icons) {
		int scale = 4;
		int cell = 18 * scale + 8;
		int cols = 10;
		int rows = (icons.size() + cols - 1) / cols;
		BufferedImage out = new BufferedImage(cols * cell, rows * cell, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < out.getHeight(); y++) {
			for (int x = 0; x < out.getWidth(); x++) {
				out.setRGB(x, y, 0xFF303036);
			}
		}
		for (int i = 0; i < icons.size(); i++) {
			BufferedImage icon = icons.get(i);
			int ox = (i % cols) * cell + 4;
			int oy = (i / cols) * cell + 4;
			for (int y = 0; y < icon.getHeight() * scale; y++) {
				for (int x = 0; x < icon.getWidth() * scale; x++) {
					int argb = icon.getRGB(x / scale, y / scale);
					if ((argb >>> 24) > 0) {
						out.setRGB(ox + x, oy + y, argb);
					}
				}
			}
		}
		try {
			Path root = this.output.getOutputFolder().getParent().getParent().getParent();
			Path file = root.resolve("build/consumable-preview.png");
			Files.createDirectories(file.getParent());
			ImageIO.write(out, "png", file.toFile());
		} catch (IOException | NullPointerException e) {
			MinecraftMode.LOGGER.warn("Could not write the consumable preview", e);
		}
	}

	@Override
	public String getName() {
		return "Consumable textures";
	}
}
