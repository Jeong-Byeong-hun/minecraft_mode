package com.minecraftmode.client.datagen;

import com.google.common.hash.Hashing;
import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.creature.BodyPlan;
import com.minecraftmode.client.creature.CreaturePlans;
import com.minecraftmode.client.creature.Skin;
import com.minecraftmode.client.datagen.art.BlockbenchExport;
import com.minecraftmode.client.datagen.art.CreaturePainter;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import org.jspecify.annotations.Nullable;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Paints every creature plan ({@link CreaturePainter}) into {@code textures/entity/creature/<id>.png}
 * and its emissive {@code <id>_glow.png}, draws the spawn egg icons, and writes front/side views of all
 * plans to build/creature-preview.png for review.
 *
 * <p>Hand-painted textures win: {@code art/creature/<id>.png} (and optionally {@code <id>_glow.png}) in the project root
 * replaces the painted texture when its size matches the plan's texture (otherwise a warning, and the painted one is used —
 * the plan's boxes changed, so the layout moved). To start one, open {@code build/bbmodel/<id>.bbmodel} (written for every
 * plan, with the current texture) in Blockbench, paint, and save the texture there.
 */
public class CreatureTextureProvider implements DataProvider {
	private final PackOutput output;

	public CreatureTextureProvider(final FabricPackOutput output) {
		this.output = output;
	}

	@Override
	public CompletableFuture<?> run(final CachedOutput cache) {
		Path assets = this.output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(MinecraftMode.MOD_ID);
		List<CompletableFuture<?>> writes = new ArrayList<>();
		List<BufferedImage> previews = new ArrayList<>();
		@Nullable Path root = this.projectRoot();
		for (BodyPlan plan : CreaturePlans.all()) {
			CreaturePainter.Painted painted = CreaturePainter.paint(plan);
			BufferedImage texture = painted.texture();
			BufferedImage glow = painted.glow();
			BufferedImage hand = handPainted(root, plan, plan.id);
			if (hand != null) {
				texture = hand;
				// without its own glow file a hand-painted texture keeps the painted glow (same layout, so the eyes stay put)
				BufferedImage handGlow = handPainted(root, plan, plan.id + "_glow");
				if (handGlow != null) {
					glow = handGlow;
				}
			}
			writes.add(write(cache, assets.resolve("textures/entity/creature/" + plan.id + ".png"), texture));
			writes.add(write(cache, assets.resolve("textures/entity/creature/" + plan.id + "_glow.png"), glow));
			writes.add(write(cache, assets.resolve("textures/item/" + plan.id + "_spawn_egg.png"), egg(plan)));
			previews.add(CreaturePainter.preview(plan, texture, 220));
			writeBbmodel(root, plan, texture);
		}
		writePreview(previews);
		return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
	}

	/** The project root (src/main/generated is three levels down), or null when the output is somewhere else. */
	private @Nullable Path projectRoot() {
		@Nullable Path p = this.output.getOutputFolder();
		for (int i = 0; i < 3 && p != null; i++) {
			p = p.getParent();
		}
		return p;
	}

	/** {@code art/creature/<name>.png} if it exists and fits the plan's texture layout. */
	private static @Nullable BufferedImage handPainted(final @Nullable Path root, final BodyPlan plan, final String name) {
		if (root == null) {
			return null;
		}
		Path file = root.resolve("art/creature/" + name + ".png");
		if (!Files.isRegularFile(file)) {
			return null;
		}
		try {
			BufferedImage image = ImageIO.read(file.toFile());
			if (image == null) {
				MinecraftMode.LOGGER.warn("{} is not a PNG; using the painted texture", file);
				return null;
			}
			if (image.getWidth() != plan.textureWidth() || image.getHeight() != plan.textureHeight()) {
				MinecraftMode.LOGGER.warn("{} is {}x{} but the {} layout is {}x{} (its boxes changed?); using the painted texture. Re-export build/bbmodel/{}.bbmodel and repaint",
					file, image.getWidth(), image.getHeight(), plan.id, plan.textureWidth(), plan.textureHeight(), plan.id);
				return null;
			}
			BufferedImage argb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics g = argb.getGraphics();
			g.drawImage(image, 0, 0, null);
			g.dispose();
			return argb;
		} catch (IOException e) {
			MinecraftMode.LOGGER.warn("Could not read {}; using the painted texture", file, e);
			return null;
		}
	}

	/** build/bbmodel/<id>.bbmodel: the plan as a Blockbench project with its current texture, to paint over. */
	private static void writeBbmodel(final @Nullable Path root, final BodyPlan plan, final BufferedImage texture) {
		if (root == null) {
			return;
		}
		try {
			Path file = root.resolve("build/bbmodel/" + plan.id + ".bbmodel");
			Files.createDirectories(file.getParent());
			Files.writeString(file, BlockbenchExport.bbmodel(plan, texture));
		} catch (IOException e) {
			MinecraftMode.LOGGER.warn("Could not write the Blockbench model of {}", plan.id, e);
		}
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

	/** Spawn egg in the plan's first two skin colors. */
	private static BufferedImage egg(final BodyPlan plan) {
		List<Skin> skins = new ArrayList<>(plan.skins.values());
		int base = skins.getFirst().base();
		int spots = skins.size() > 1 ? skins.get(1).base() : skins.getFirst().accent();
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		int seed = plan.id.hashCode();
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double dx = (x - 7.5) / 5.2;
				double dy = (y - 8.8) / (y < 9 ? 7.2 : 6.0);
				double d = dx * dx + dy * dy;
				if (d > 1.0) {
					continue;
				}
				int rgb;
				if (d > 0.78) {
					rgb = scale(base, 0.45F);
				} else if (Math.floorMod(x * 7 + y * 13 + seed, 9) == 0 || Math.floorMod(x * 5 - y * 3 + seed, 11) == 0) {
					rgb = spots;
				} else {
					rgb = scale(base, x + y < 13 ? 1.12F : 0.95F);
				}
				img.setRGB(x, y, 0xFF000000 | rgb);
			}
		}
		img.setRGB(5, 4, 0xFFFFFFFF);
		return img;
	}

	private static int scale(final int rgb, final float f) {
		int r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * f));
		int g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * f));
		int b = Math.min(255, Math.round((rgb & 0xFF) * f));
		return r << 16 | g << 8 | b;
	}

	private void writePreview(final List<BufferedImage> previews) {
		if (previews.isEmpty()) {
			return;
		}
		int cols = 4;
		int w = previews.getFirst().getWidth();
		int h = previews.getFirst().getHeight();
		int rows = (previews.size() + cols - 1) / cols;
		BufferedImage sheet = new BufferedImage(cols * w, rows * h, BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < previews.size(); i++) {
			sheet.getGraphics().drawImage(previews.get(i), (i % cols) * w, (i / cols) * h, null);
		}
		Path root = this.projectRoot();
		if (root == null) {
			return;
		}
		try {
			Path file = root.resolve("build/creature-preview.png");
			Files.createDirectories(file.getParent());
			ImageIO.write(sheet, "png", file.toFile());
		} catch (IOException e) {
			MinecraftMode.LOGGER.warn("Could not write the creature preview", e);
		}
	}

	@Override
	public String getName() {
		return "Creature textures";
	}
}
