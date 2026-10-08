import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

/**
 * Generates the mod's placeholder textures.
 *
 * Metals/ores/tools/armor are hue-shifted from the vanilla textures in the Minecraft client jar
 * (fine for personal use; replace with original art before publishing). Coins, the spawn egg,
 * the shop block, plastic, the bleeding icon and the Mine Raider skin are drawn procedurally.
 *
 * Usage: java tools/TextureGen.java <minecraft-client.jar> <assets/minecraft_mode/textures dir> [preview.png]
 */
public class TextureGen {
	// Mythril: silvery azure. Aluminum: cool near-white. Bauxite (raw aluminum): red-brown.
	static final float MYTHRIL_HUE = 0.62F;
	static final float ALUMINUM_HUE = 0.58F;
	static final float BAUXITE_HUE = 0.045F;

	static ZipFile jar;
	static Path out;
	static final List<BufferedImage> preview = new ArrayList<>();

	public static void main(final String[] args) throws IOException {
		jar = new ZipFile(args[0]);
		out = Path.of(args[1]);

		// --- Mythril (from diamond / iron) ---
		write("block/mythril_ore", shiftHue(vanilla("block/diamond_ore"), TextureGen::isCyan, MYTHRIL_HUE, 0.8F, 1.05F));
		write("block/deepslate_mythril_ore", shiftHue(vanilla("block/deepslate_diamond_ore"), TextureGen::isCyan, MYTHRIL_HUE, 0.8F, 1.05F));
		write("block/mythril_block", shiftHue(vanilla("block/diamond_block"), TextureGen::isCyan, MYTHRIL_HUE, 0.75F, 1.0F));
		write("block/raw_mythril_block", colorize(vanilla("block/raw_iron_block"), MYTHRIL_HUE, 0.3F, 0.35F, 0.95F));
		write("item/raw_mythril", colorize(vanilla("item/raw_iron"), MYTHRIL_HUE, 0.3F, 0.35F, 0.95F));
		write("item/mythril_ingot", colorize(vanilla("item/iron_ingot"), MYTHRIL_HUE, 0.3F, 0.45F, 0.98F));
		write("item/mythril_nugget", colorize(vanilla("item/iron_nugget"), MYTHRIL_HUE, 0.3F, 0.45F, 0.98F));
		for (String tool : new String[] {"sword", "pickaxe", "axe", "shovel", "hoe", "helmet", "chestplate", "leggings", "boots"}) {
			write("item/mythril_" + tool, shiftHue(vanilla("item/diamond_" + tool), TextureGen::isCyan, MYTHRIL_HUE, 0.8F, 1.0F));
		}
		for (String layer : new String[] {"humanoid", "humanoid_leggings", "humanoid_baby"}) {
			write("entity/equipment/" + layer + "/mythril", shiftHue(vanilla("entity/equipment/" + layer + "/diamond"), TextureGen::isCyan, MYTHRIL_HUE, 0.8F, 1.0F));
		}

		// --- Aluminum (from iron / copper) ---
		write("block/aluminum_ore", shiftHue(vanilla("block/iron_ore"), TextureGen::isTinted, BAUXITE_HUE, 1.6F, 0.92F));
		write("block/deepslate_aluminum_ore", shiftHue(vanilla("block/deepslate_iron_ore"), TextureGen::isTinted, BAUXITE_HUE, 1.6F, 0.92F));
		write("block/raw_aluminum_block", colorize(vanilla("block/raw_iron_block"), BAUXITE_HUE, 0.45F, 0.2F, 0.85F));
		write("item/raw_aluminum", colorize(vanilla("item/raw_iron"), BAUXITE_HUE, 0.45F, 0.2F, 0.85F));
		write("block/aluminum_block", colorize(vanilla("block/iron_block"), ALUMINUM_HUE, 0.04F, 0.12F, 1.04F));
		write("item/aluminum_ingot", colorize(vanilla("item/iron_ingot"), ALUMINUM_HUE, 0.04F, 0.14F, 1.05F));
		write("item/aluminum_nugget", colorize(vanilla("item/iron_nugget"), ALUMINUM_HUE, 0.04F, 0.14F, 1.05F));

		// --- Plastic ---
		write("item/plastic_sheet", colorize(vanilla("item/paper"), 0.53F, 0.18F, 0.25F, 1.02F));
		write("block/plastic_block", plasticBlock());

		// --- Economy ---
		write("item/copper_coin", coin(0xC8743A, 0x8E4A22, 0xF0A56A, 0x5A2C12));
		write("item/silver_coin", coin(0xC9CED6, 0x8A929E, 0xF4F7FB, 0x4E5560));
		write("item/gold_coin", coin(0xF2C230, 0xB8860B, 0xFFF0A0, 0x6E4F05));
		write("block/shop_block_side", shopSide());
		write("block/shop_block_top", shopTop());

		// --- Mob & effect ---
		write("item/mine_raider_spawn_egg", spawnEgg(0x6B4423, 0xE3B92C, 0x2E1C0F));
		write("entity/mine_raider", mineRaiderSkin());
		write("mob_effect/bleeding", bleedingIcon());

		if (args.length > 2) {
			writePreview(Path.of(args[2]));
		}
		System.out.println("Wrote textures to " + out.toAbsolutePath());
	}

	// ---------------------------------------------------------------- IO

	static BufferedImage vanilla(final String name) throws IOException {
		ZipEntry entry = jar.getEntry("assets/minecraft/textures/" + name + ".png");
		if (entry == null) {
			throw new IOException("Missing vanilla texture " + name);
		}
		try (InputStream in = jar.getInputStream(entry)) {
			BufferedImage src = ImageIO.read(in);
			BufferedImage argb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
			argb.getGraphics().drawImage(src, 0, 0, null);
			return argb;
		}
	}

	static void write(final String name, final BufferedImage img) throws IOException {
		File file = out.resolve(name + ".png").toFile();
		Files.createDirectories(file.toPath().getParent());
		ImageIO.write(img, "png", file);
		preview.add(img);
	}

	static void writePreview(final Path path) throws IOException {
		int scale = 6, cell = 64 * scale / 2, cols = 8;
		int rows = (preview.size() + cols - 1) / cols;
		BufferedImage sheet = new BufferedImage(cols * cell, rows * cell, BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < preview.size(); i++) {
			BufferedImage img = preview.get(i);
			int s = Math.max(1, cell / Math.max(img.getWidth(), img.getHeight()));
			int ox = (i % cols) * cell, oy = (i / cols) * cell;
			for (int y = 0; y < cell; y++) {
				for (int x = 0; x < cell; x++) {
					boolean dark = ((x / 8) + (y / 8)) % 2 == 0;
					sheet.setRGB(ox + x, oy + y, dark ? 0xFF3A3A3A : 0xFF4A4A4A);
				}
			}
			for (int y = 0; y < img.getHeight() * s && y < cell; y++) {
				for (int x = 0; x < img.getWidth() * s && x < cell; x++) {
					int argb = img.getRGB(x / s, y / s);
					if ((argb >>> 24) > 0) {
						sheet.setRGB(ox + x, oy + y, argb | 0xFF000000);
					}
				}
			}
		}
		ImageIO.write(sheet, "png", path.toFile());
	}

	// ---------------------------------------------------------------- recolor

	interface PixelTest {
		boolean test(float hue, float sat, float bri);
	}

	static boolean isCyan(final float h, final float s, final float b) {
		return h > 0.40F && h < 0.62F && s > 0.12F;
	}

	static boolean isTinted(final float h, final float s, final float b) {
		return s > 0.15F;
	}

	/** Re-hue only the matching pixels, keeping their brightness structure. */
	static BufferedImage shiftHue(final BufferedImage src, final PixelTest test, final float hue, final float satMul, final float briMul) {
		BufferedImage dst = copy(src);
		float[] hsb = new float[3];
		for (int y = 0; y < src.getHeight(); y++) {
			for (int x = 0; x < src.getWidth(); x++) {
				int argb = src.getRGB(x, y);
				int a = argb >>> 24;
				if (a == 0) {
					continue;
				}
				Color.RGBtoHSB((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, hsb);
				if (test.test(hsb[0], hsb[1], hsb[2])) {
					int rgb = Color.HSBtoRGB(hue, clamp(hsb[1] * satMul), clamp(hsb[2] * briMul));
					dst.setRGB(x, y, (a << 24) | (rgb & 0xFFFFFF));
				}
			}
		}
		return dst;
	}

	/** Re-hue every pixel; darker pixels get more saturation so shading stays readable. */
	static BufferedImage colorize(final BufferedImage src, final float hue, final float baseSat, final float shadowSat, final float briMul) {
		BufferedImage dst = copy(src);
		float[] hsb = new float[3];
		for (int y = 0; y < src.getHeight(); y++) {
			for (int x = 0; x < src.getWidth(); x++) {
				int argb = src.getRGB(x, y);
				int a = argb >>> 24;
				if (a == 0) {
					continue;
				}
				Color.RGBtoHSB((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, hsb);
				float sat = clamp(baseSat + shadowSat * (1.0F - hsb[2]));
				int rgb = Color.HSBtoRGB(hue, sat, clamp(hsb[2] * briMul));
				dst.setRGB(x, y, (a << 24) | (rgb & 0xFFFFFF));
			}
		}
		return dst;
	}

	static BufferedImage copy(final BufferedImage src) {
		BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
		dst.getGraphics().drawImage(src, 0, 0, null);
		return dst;
	}

	static float clamp(final float v) {
		return Math.max(0.0F, Math.min(1.0F, v));
	}

	// ---------------------------------------------------------------- procedural

	static int opaque(final int rgb) {
		return 0xFF000000 | rgb;
	}

	static int shade(final int rgb, final float factor) {
		int r = Math.min(255, Math.max(0, Math.round(((rgb >> 16) & 0xFF) * factor)));
		int g = Math.min(255, Math.max(0, Math.round(((rgb >> 8) & 0xFF) * factor)));
		int b = Math.min(255, Math.max(0, Math.round((rgb & 0xFF) * factor)));
		return (r << 16) | (g << 8) | b;
	}

	static BufferedImage coin(final int base, final int rim, final int highlight, final int outline) {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		double cx = 7.5, cy = 7.5;
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double d = Math.hypot(x - cx, y - cy);
				int color;
				if (d > 7.0) {
					continue;
				} else if (d > 6.2) {
					color = outline;
				} else if (d > 5.0) {
					// Rim: lit from the top-left
					color = (x + y < 15) ? shade(rim, 1.25F) : rim;
				} else {
					color = base;
					double diamond = Math.abs(x - cx) + Math.abs(y - cy);
					if (diamond <= 2.0) {
						color = highlight;
					} else if (diamond <= 3.0) {
						color = shade(base, 0.72F);
					} else if (x + y < 10) {
						color = shade(base, 1.12F);
					} else if (x + y > 19) {
						color = shade(base, 0.88F);
					}
				}
				img.setRGB(x, y, opaque(color));
			}
		}
		img.setRGB(5, 4, opaque(highlight));
		img.setRGB(4, 5, opaque(highlight));
		return img;
	}

	static BufferedImage spawnEgg(final int base, final int spots, final int outline) {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		double cx = 7.5, cy = 8.6, rx = 5.4, ry = 6.9;
		int[][] spotCenters = {{5, 5}, {10, 7}, {6, 11}, {10, 12}, {8, 3}};
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				// Narrower towards the top, like an egg
				double widen = 1.0 + (y - cy) / 30.0;
				double nx = (x - cx) / (rx * widen), ny = (y - cy) / ry;
				double d = nx * nx + ny * ny;
				if (d > 1.0) {
					continue;
				}
				int color = base;
				for (int[] s : spotCenters) {
					if (Math.hypot(x - s[0], y - s[1]) < 1.3) {
						color = spots;
					}
				}
				if (d > 0.78) {
					color = outline;
				} else if (x < cx - 1 && y < cy - 1 && color == base) {
					color = shade(base, 1.25F);
				}
				img.setRGB(x, y, opaque(color));
			}
		}
		img.setRGB(5, 3, opaque(0xFFFFFF));
		return img;
	}

	static BufferedImage bleedingIcon() {
		BufferedImage img = new BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB);
		int red = 0xB0101A, dark = 0x5A0008, light = 0xFF5A5A;
		for (int y = 0; y < 18; y++) {
			for (int x = 0; x < 18; x++) {
				double round = Math.hypot(x - 8.5, y - 11.0);
				// Tip: a triangle narrowing towards y=2
				boolean tip = y >= 2 && y <= 11 && Math.abs(x - 8.5) <= (y - 2) * 0.55;
				boolean inside = round <= 5.2 || tip;
				boolean border = !inside && (Math.hypot(x - 8.5, y - 11.0) <= 6.1 || (y >= 1 && y <= 11 && Math.abs(x - 8.5) <= (y - 1) * 0.55 + 0.6));
				if (inside) {
					int color = red;
					if (x < 8 && y >= 8 && y <= 11 && x >= 5) {
						color = light;
					} else if (x > 10 && y > 11) {
						color = shade(red, 0.75F);
					}
					img.setRGB(x, y, opaque(color));
				} else if (border) {
					img.setRGB(x, y, opaque(dark));
				}
			}
		}
		return img;
	}

	static BufferedImage plasticBlock() {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Random random = new Random(7);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int color = 0xE6F1F7;
				if (x == 0 || y == 0) {
					color = 0xF7FCFF;
				} else if (x == 15 || y == 15) {
					color = 0xB7CFDC;
				} else if (x - y == 4 || x - y == 5) {
					color = 0xFFFFFF; // gloss streak
				} else {
					color = shade(color, 0.97F + random.nextFloat() * 0.04F);
				}
				img.setRGB(x, y, opaque(color));
			}
		}
		return img;
	}

	static void planks(final BufferedImage img, final int x0, final int y0, final int w, final int h, final int seed) {
		Random random = new Random(seed);
		int wood = 0x8B5A2B;
		for (int y = y0; y < y0 + h; y++) {
			for (int x = x0; x < x0 + w; x++) {
				int row = y - y0;
				int color = shade(wood, 0.92F + random.nextFloat() * 0.14F);
				if (row % 4 == 3) {
					color = shade(wood, 0.62F);
				} else if ((x * 7 + (row / 4) * 5) % 11 == 0) {
					color = shade(wood, 0.78F); // grain
				}
				img.setRGB(x, y, opaque(color));
			}
		}
	}

	static BufferedImage shopSide() {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		planks(img, 0, 0, 16, 16, 3);
		// Striped awning
		for (int y = 0; y < 6; y++) {
			for (int x = 0; x < 16; x++) {
				int color = (x / 2) % 2 == 0 ? 0xC0392B : 0xF2F2F2;
				img.setRGB(x, y, opaque(y == 0 ? shade(color, 0.8F) : color));
			}
		}
		// Scalloped edge
		for (int x = 0; x < 16; x++) {
			if (x % 2 == 0) {
				int color = (x / 2) % 2 == 0 ? 0xC0392B : 0xF2F2F2;
				img.setRGB(x, 6, opaque(shade(color, 0.85F)));
			}
		}
		// Counter frame
		for (int y = 7; y < 16; y++) {
			img.setRGB(0, y, opaque(0x5C3A1B));
			img.setRGB(15, y, opaque(0x5C3A1B));
		}
		for (int x = 0; x < 16; x++) {
			img.setRGB(x, 8, opaque(0x5C3A1B));
			img.setRGB(x, 15, opaque(0x4A2E15));
		}
		// Gold coin emblem
		BufferedImage coin = coin(0xF2C230, 0xB8860B, 0xFFF0A0, 0x6E4F05);
		for (int y = 0; y < 6; y++) {
			for (int x = 0; x < 6; x++) {
				int argb = coin.getRGB(x * 16 / 6 + 1, y * 16 / 6 + 1);
				if ((argb >>> 24) > 0) {
					img.setRGB(5 + x, 9 + y, argb);
				}
			}
		}
		return img;
	}

	static BufferedImage shopTop() {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int color = (x / 2) % 2 == 0 ? 0xC0392B : 0xF2F2F2;
				if (x == 0 || y == 0 || x == 15 || y == 15) {
					color = shade(color, 0.75F);
				}
				img.setRGB(x, y, opaque(color));
			}
		}
		return img;
	}

	// ---------------------------------------------------------------- Mine Raider skin (64x64, legacy humanoid layout)

	static BufferedImage skin;
	static Random skinNoise;

	static void fill(final int x0, final int y0, final int w, final int h, final int rgb) {
		for (int y = y0; y < y0 + h; y++) {
			for (int x = x0; x < x0 + w; x++) {
				skin.setRGB(x, y, opaque(shade(rgb, 0.93F + skinNoise.nextFloat() * 0.12F)));
			}
		}
	}

	static void px(final int x, final int y, final int rgb) {
		skin.setRGB(x, y, opaque(rgb));
	}

	static BufferedImage mineRaiderSkin() {
		skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		skinNoise = new Random(42);
		int skinTone = 0xA88565, helmet = 0xE3B92C, shirt = 0x4B4F57, vest = 0x6B4423, pants = 0x3E3328, boots = 0x2B2118, glove = 0x5A3A1E;

		// Head (0,0): top, bottom, then the four sides at y=8
		fill(8, 0, 8, 8, helmet);          // top = hard hat
		fill(16, 0, 8, 8, skinTone);       // bottom (chin)
		fill(0, 8, 32, 8, skinTone);       // all sides
		fill(0, 8, 32, 2, helmet);         // hard hat band
		for (int x = 0; x < 32; x++) {
			px(x, 10, shade(helmet, 0.7F)); // brim shadow
		}
		// Headlamp on the front
		px(11, 8, 0xFFF6B0);
		px(12, 8, 0xFFF6B0);
		px(11, 9, 0xD9C46A);
		px(12, 9, 0xD9C46A);
		// Face (front = 8..15, 8..15)
		px(9, 12, 0x2A1C14);
		px(10, 12, 0xFF3B1F);
		px(13, 12, 0xFF3B1F);
		px(14, 12, 0x2A1C14);
		px(11, 13, shade(skinTone, 0.82F));
		px(12, 13, shade(skinTone, 0.82F));
		fill(9, 14, 6, 2, 0x4A3222);       // stubble
		px(11, 14, 0x2B1A10);
		px(12, 14, 0x2B1A10);
		fill(24, 11, 8, 5, 0x3B2A1E);      // hair on the back

		// Body (16,16): top, bottom, then sides at y=20
		fill(20, 16, 8, 4, shirt);
		fill(28, 16, 8, 4, shirt);
		fill(16, 20, 24, 12, shirt);
		fill(20, 20, 2, 12, vest);         // vest panels on the front
		fill(26, 20, 2, 12, vest);
		fill(32, 20, 8, 12, vest);         // vest back
		fill(16, 29, 24, 2, 0x3A2414);     // belt
		px(23, 29, 0xC9A227);
		px(24, 29, 0xC9A227);
		px(23, 30, 0xC9A227);
		px(24, 30, 0xC9A227);

		// Arm (40,16)
		fill(44, 16, 4, 4, shirt);
		fill(48, 16, 4, 4, glove);
		fill(40, 20, 16, 7, shirt);
		fill(40, 27, 16, 2, skinTone);
		fill(40, 29, 16, 3, glove);

		// Leg (0,16)
		fill(4, 16, 4, 4, pants);
		fill(8, 16, 4, 4, boots);
		fill(0, 20, 16, 8, pants);
		fill(0, 28, 16, 4, boots);
		for (int x = 0; x < 16; x++) {
			px(x, 31, 0x1A130D);           // soles
		}
		return skin;
	}
}
