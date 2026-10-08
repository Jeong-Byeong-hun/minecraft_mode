import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Draws every texture of the mod from scratch (no vanilla assets are read).
 *
 * - Item sprites are hand-drawn 16x16 character grids rendered through a 5-tone palette.
 * - Tools are drawn on a 45-degree axis (handle bottom-left, head top-right) from shape functions.
 * - Stone/deepslate, ore crystals, metal blocks and raw blocks are procedural (seeded, tileable).
 * - Armor layers are painted per model face using the humanoid box UV layout.
 *
 * Usage: java tools/TextureGen.java <assets/minecraft_mode/textures dir> [preview.png]
 */
public class TextureGen {
	/** Five tones: outline, dark, mid, light, highlight. Grid chars o/d/m/l/h pick them. */
	record Palette(int o, int d, int m, int l, int h) {
		int of(final char c) {
			return switch (Character.toLowerCase(c)) {
				case 'o' -> o;
				case 'd' -> d;
				case 'm' -> m;
				case 'l' -> l;
				case 'h' -> h;
				default -> throw new IllegalArgumentException("bad palette char " + c);
			};
		}
	}

	static final Palette MYTHRIL = new Palette(0x16233F, 0x2B4783, 0x4C7BC8, 0x8AB2EE, 0xD8E9FF);
	static final Palette RAW_MYTHRIL = new Palette(0x16233F, 0x31508C, 0x5A85C8, 0x99BBEE, 0xE2EFFF);
	static final Palette ALUMINUM = new Palette(0x3A414B, 0x7B8592, 0xADB7C3, 0xD3DBE4, 0xF5F8FC);
	static final Palette BAUXITE = new Palette(0x3B190D, 0x77381E, 0xA35831, 0xC78052, 0xE5A979);
	static final Palette PLASTIC = new Palette(0x4F7E99, 0x8DB8CF, 0xB7D9E9, 0xDCEFF8, 0xFFFFFF);
	static final Palette WOOD = new Palette(0x271809, 0x47301C, 0x6A4A2A, 0x8C6940, 0xA8844F);

	static final int[] STONE = {0x5E5E5E, 0x6C6C6C, 0x7A7A7A, 0x878787, 0x959595};
	static final int[] DEEPSLATE = {0x2D2D34, 0x383840, 0x43434C, 0x4E4E58, 0x5A5A65};

	static Path out;
	static final List<BufferedImage> preview = new ArrayList<>();
	static final List<Boolean> previewTiled = new ArrayList<>();

	public static void main(final String[] args) throws IOException {
		out = Path.of(args[0]);

		// --- Mythril ---
		write("block/mythril_ore", ore(stone(11, false), MYTHRIL, CRYSTALS, CRYSTAL_SPOTS_A, true));
		write("block/deepslate_mythril_ore", ore(stone(12, true), MYTHRIL, CRYSTALS, CRYSTAL_SPOTS_B, true));
		write("block/mythril_block", paneledBlock(MYTHRIL));
		write("block/raw_mythril_block", chunkyBlock(RAW_MYTHRIL, 21));
		write("item/raw_mythril", sprite(RAW_CHUNK, RAW_MYTHRIL));
		write("item/mythril_ingot", sprite(INGOT, MYTHRIL));
		write("item/mythril_nugget", sprite(NUGGET, MYTHRIL));
		write("item/mythril_sword", sprite(diagonal(TextureGen::sword), MYTHRIL));
		write("item/mythril_pickaxe", sprite(diagonal(TextureGen::pickaxe), MYTHRIL));
		write("item/mythril_axe", sprite(diagonal(TextureGen::axe), MYTHRIL));
		write("item/mythril_shovel", sprite(diagonal(TextureGen::shovel), MYTHRIL));
		write("item/mythril_hoe", sprite(diagonal(TextureGen::hoe), MYTHRIL));
		write("item/mythril_helmet", sprite(mirror(HELMET), MYTHRIL));
		write("item/mythril_chestplate", sprite(mirror(CHESTPLATE), MYTHRIL));
		write("item/mythril_leggings", sprite(mirror(LEGGINGS), MYTHRIL));
		write("item/mythril_boots", sprite(mirror(BOOTS), MYTHRIL));
		write("entity/equipment/humanoid/mythril", armorLayer(MYTHRIL, false));
		write("entity/equipment/humanoid_leggings/mythril", armorLayer(MYTHRIL, true));
		write("entity/equipment/humanoid_baby/mythril", babyArmorLayer(MYTHRIL));

		// --- Aluminum ---
		write("block/aluminum_ore", ore(stone(31, false), BAUXITE, NODULES, NODULE_SPOTS_A, false));
		write("block/deepslate_aluminum_ore", ore(stone(32, true), BAUXITE, NODULES, NODULE_SPOTS_B, false));
		write("block/aluminum_block", brushedBlock(ALUMINUM, 41));
		write("block/raw_aluminum_block", chunkyBlock(BAUXITE, 42));
		write("item/raw_aluminum", sprite(RAW_CHUNK, BAUXITE));
		write("item/aluminum_ingot", sprite(INGOT, ALUMINUM));
		write("item/aluminum_nugget", sprite(NUGGET, ALUMINUM));

		// --- Plastic ---
		write("item/plastic_sheet", sprite(PLASTIC_SHEET, PLASTIC));
		write("block/plastic_block", plasticBlock());

		// --- Economy ---
		write("item/copper_coin", coin(0xC8743A, 0x8E4A22, 0xF0A56A, 0x5A2C12));
		write("item/silver_coin", coin(0xC9CED6, 0x8A929E, 0xF4F7FB, 0x4E5560));
		write("item/gold_coin", coin(0xF2C230, 0xB8860B, 0xFFF0A0, 0x6E4F05));
		write("block/shop_block_side", shopSide(0xC0392B, 0xF2F2F2, coinEmblem()));
		write("block/shop_block_top", shopTop(0xC0392B, 0xF2F2F2));
		write("block/blacksmith_shop_side", shopSide(0x3B3B3B, 0xE07B26, emblem(ANVIL)));
		write("block/blacksmith_shop_top", shopTop(0x3B3B3B, 0xE07B26));
		write("block/grocer_shop_side", shopSide(0x2E8B3A, 0xF2F2F2, emblem(APPLE)));
		write("block/grocer_shop_top", shopTop(0x2E8B3A, 0xF2F2F2));
		write("block/jeweler_shop_side", shopSide(0x6A3D9A, 0xF2F2F2, emblem(GEM)));
		write("block/jeweler_shop_top", shopTop(0x6A3D9A, 0xF2F2F2));

		// --- Mob & effect ---
		write("item/mine_raider_spawn_egg", spawnEgg(0x6B4423, 0xE3B92C, 0x2E1C0F));
		write("entity/mine_raider", mineRaiderSkin());
		write("item/mythril_golem_spawn_egg", spawnEgg(0x4C7BC8, 0x9FF4FF, 0x16233F));
		write("entity/mythril_golem", mythrilGolemSkin());
		write("mob_effect/bleeding", bleedingIcon());

		if (args.length > 1) {
			writePreview(Path.of(args[1]));
		}
		System.out.println("Wrote textures to " + out.toAbsolutePath());
	}

	// ---------------------------------------------------------------- IO

	static void write(final String name, final BufferedImage img) throws IOException {
		File file = out.resolve(name + ".png").toFile();
		Files.createDirectories(file.toPath().getParent());
		ImageIO.write(img, "png", file);
		preview.add(img);
		previewTiled.add(name.startsWith("block/") && !name.startsWith("block/shop"));
	}

	/** Contact sheet; block textures are shown tiled 2x2 so seams are visible. */
	static void writePreview(final Path path) throws IOException {
		int cell = 192, cols = 8;
		int rows = (preview.size() + cols - 1) / cols;
		BufferedImage sheet = new BufferedImage(cols * cell, rows * cell, BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < preview.size(); i++) {
			BufferedImage img = preview.get(i);
			boolean tiled = previewTiled.get(i);
			int repeat = tiled ? 2 : 1;
			int s = Math.max(1, cell / (Math.max(img.getWidth(), img.getHeight()) * repeat));
			int ox = (i % cols) * cell, oy = (i / cols) * cell;
			for (int y = 0; y < cell; y++) {
				for (int x = 0; x < cell; x++) {
					boolean dark = ((x / 8) + (y / 8)) % 2 == 0;
					sheet.setRGB(ox + x, oy + y, dark ? 0xFF3A3A3A : 0xFF4A4A4A);
				}
			}
			int w = img.getWidth() * s * repeat, h = img.getHeight() * s * repeat;
			for (int y = 0; y < Math.min(h, cell); y++) {
				for (int x = 0; x < Math.min(w, cell); x++) {
					int argb = img.getRGB((x / s) % img.getWidth(), (y / s) % img.getHeight());
					if ((argb >>> 24) > 0) {
						sheet.setRGB(ox + x, oy + y, argb | 0xFF000000);
					}
				}
			}
		}
		ImageIO.write(sheet, "png", path.toFile());
	}

	// ---------------------------------------------------------------- grid sprites

	/** Lowercase o/d/m/l/h use the material palette, uppercase uses wood, '.' is transparent. */
	static BufferedImage sprite(final String[] rows, final Palette material) {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) {
			if (rows[y].length() != 16) {
				throw new IllegalArgumentException("row " + y + " has length " + rows[y].length() + ": " + rows[y]);
			}
			for (int x = 0; x < 16; x++) {
				char c = rows[y].charAt(x);
				if (c != '.') {
					int rgb = Character.isUpperCase(c) ? WOOD.of(c) : material.of(c);
					img.setRGB(x, y, opaque(rgb));
				}
			}
		}
		return img;
	}

	/** Builds a symmetric sprite from its left half; the right half is one shade darker (light comes from the left). */
	static String[] mirror(final String[] leftHalf) {
		String[] rows = new String[16];
		for (int y = 0; y < 16; y++) {
			StringBuilder right = new StringBuilder();
			for (int x = 7; x >= 0; x--) {
				char c = leftHalf[y].charAt(x);
				right.append(switch (c) {
					case 'h' -> 'l';
					case 'l' -> 'm';
					default -> c;
				});
			}
			rows[y] = leftHalf[y] + right;
		}
		return rows;
	}

	static final String[] INGOT = {
		"................",
		"................",
		"................",
		"................",
		"................",
		".....ooooooooo..",
		"....ohhhhhhhhlo.",
		"...ohllllllllldo",
		"..ohmmmmmmmmmmdo",
		"..ommmmmmmmmmmdo",
		"..odddddddddddo.",
		"...ooooooooooo..",
		"................",
		"................",
		"................",
		"................",
	};

	static final String[] NUGGET = {
		"................",
		"................",
		"................",
		"................",
		"................",
		"................",
		"......ooo.......",
		".....ohhlo......",
		"....ohllmmo.....",
		"....olmmmmdo....",
		"....ommmmddo.oo.",
		".....oddddo.olo.",
		"......oooo..oo..",
		"................",
		"................",
		"................",
	};

	static final String[] RAW_CHUNK = {
		"................",
		"................",
		"................",
		"................",
		".......oooo.....",
		".....oohhllo....",
		"....ohllllmmo...",
		"...ohlllmmmmdo..",
		"..oohlmmmmmmddo.",
		".ohllmmmmmdmddo.",
		".olllmmmmddddddo",
		".ommmmddmddddoo.",
		"..oddddddddoo...",
		"...oooooooo.....",
		"................",
		"................",
	};

	static final String[] PLASTIC_SHEET = {
		"................",
		"................",
		"..oooooooooooo..",
		"..ohhhhhhhhhlo..",
		"..ohllllllllmo..",
		"..ollhllllllmo..",
		"..olllhlllllmo..",
		"..ollllhllllmo..",
		"..olllllhlllmo..",
		"..ollllllhllmo..",
		"..olllllllhlmo..",
		"..olllllllllmo..",
		"..ollllllllodo..",
		"..olllllllodo...",
		"..ooooooooo.....",
		"................",
	};

	// Left halves (8 columns) of the armor icons; see mirror().
	static final String[] HELMET = {
		"........",
		"........",
		".....ooo",
		"...oohhh",
		"..ohhllh",
		".ohlllmh",
		".ohllmmh",
		"ohlmmmmh",
		"olmooooo",
		"ohlmmmdo",
		"omlmmmdo",
		"omdmmddo",
		"oddddddo",
		".ooooooo",
		"........",
		"........",
	};

	static final String[] CHESTPLATE = {
		"........",
		".oooo...",
		"ohhhlo..",
		"ohlllmoo",
		"ohllllml",
		"oolllmml",
		".olllmmh",
		".ollmmmh",
		".ollmmml",
		".olmmmml",
		".olmmmdl",
		".ommmmdl",
		".odddddd",
		".ollllll",
		".ooooooo",
		"........",
	};

	static final String[] LEGGINGS = {
		"........",
		"........",
		"oooooooo",
		"ohhhhhhh",
		"oddddddd",
		"ohllmmmm",
		"ohlmmmmm",
		"ohlmmmdo",
		"ohlmmdo.",
		"ohlmmdo.",
		"ohlmmdo.",
		"ohhlmdo.",
		"ohlmmdo.",
		"olmmddo.",
		"ooooooo.",
		"........",
	};

	static final String[] BOOTS = {
		"........",
		"........",
		"........",
		"........",
		"..ooooo.",
		"..ohhlo.",
		"..odddo.",
		"..ohlmo.",
		"..olmmo.",
		"..olmmo.",
		".oolmmo.",
		"ohhlmmo.",
		"ohllmmo.",
		"olmmmdo.",
		"ooooooo.",
		"........",
	};

	// ---------------------------------------------------------------- tools (45-degree axis)

	interface DiagonalShape {
		/** along = position on the handle-to-tip axis, across = signed distance from it (negative = upper-left). */
		char at(int along, int across);
	}

	/** Renders a shape on the bottom-left to top-right diagonal and outlines it (metal 'o', wood 'O'). */
	static String[] diagonal(final DiagonalShape shape) {
		char[][] g = new char[16][16];
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				g[y][x] = shape.at(x - y, x + y - 15);
			}
		}
		char[][] outlined = new char[16][16];
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				outlined[y][x] = g[y][x];
				if (g[y][x] != '.') {
					continue;
				}
				boolean metal = false, wood = false;
				int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
				for (int[] d : dirs) {
					int nx = x + d[0], ny = y + d[1];
					if (nx < 0 || ny < 0 || nx > 15 || ny > 15 || g[ny][nx] == '.') {
						continue;
					}
					if (Character.isUpperCase(g[ny][nx])) {
						wood = true;
					} else {
						metal = true;
					}
				}
				if (metal) {
					outlined[y][x] = 'o';
				} else if (wood) {
					outlined[y][x] = 'O';
				}
			}
		}
		String[] rows = new String[16];
		for (int y = 0; y < 16; y++) {
			rows[y] = new String(outlined[y]);
		}
		return rows;
	}

	static char handle(final int along) {
		return Math.floorMod(along, 4) < 2 ? 'L' : 'M';
	}

	static char sword(final int a, final int c) {
		int ac = Math.abs(c);
		if (ac <= 1 && a >= -3 && a <= 13 - ac) {
			return c < 0 ? 'h' : c == 0 ? 'l' : 'd';
		}
		if (a >= -5 && a <= -4 && ac <= 4) {
			return ac >= 3 ? 'd' : 'm';
		}
		if (c == 0 && a >= -11 && a <= -6) {
			return handle(a);
		}
		if (a >= -13 && a <= -12 && ac <= 1) {
			return 'm';
		}
		return '.';
	}

	static char pickaxe(final int a, final int c) {
		if (c == 0 && a >= -13 && a <= 6) {
			return handle(a);
		}
		int ac = Math.abs(c);
		double center = 8.5 - c * c / 9.0;
		if (ac <= 8 && a >= center - 1.6 && a <= center + 0.6) {
			if (ac >= 7) {
				return 'd';
			}
			boolean upper = a >= center - 0.5;
			if (upper) {
				return c < 0 ? 'h' : 'l';
			}
			return c < 0 ? 'm' : 'd';
		}
		return '.';
	}

	static char axe(final int a, final int c) {
		if (c == 0 && a >= -13 && a <= 8) {
			return handle(a);
		}
		// Blade on the upper-left side, flaring towards the cutting edge
		if (c <= -1 && c >= -6) {
			int k = -c - 1;
			double lo = 2 - k * 0.8;
			double hi = Math.min(7 + k * 0.3, 13 + c);
			if (a >= lo && a <= hi) {
				if (c <= -5) {
					return a >= hi - 1 || a <= lo + 1 ? 'l' : 'h';
				}
				return c == -1 ? 'd' : 'm';
			}
		}
		// Short poll on the back
		if (c >= 1 && c <= 2 && a >= 4 && a <= 7) {
			return c == 1 ? 'm' : 'd';
		}
		return '.';
	}

	static char shovel(final int a, final int c) {
		int ac = Math.abs(c);
		if (c == 0 && a >= -13 && a <= 2) {
			return handle(a);
		}
		if (a >= 2 && a <= 3 && ac <= 1) {
			return 'd';
		}
		double half = 3.0 - Math.max(0, a - 10) * 0.9;
		if (a >= 4 && a <= 13 && ac <= half) {
			return c < 0 ? 'h' : c == 0 ? 'l' : ac == 1 ? 'm' : 'd';
		}
		return '.';
	}

	static char hoe(final int a, final int c) {
		if (c == 0 && a >= -13 && a <= 7) {
			return handle(a);
		}
		if (a >= 7 && a <= 9 && c >= -6 && c <= 1) {
			return a == 9 ? 'l' : a == 8 ? 'm' : 'd';
		}
		if (c >= -6 && c <= -5 && a >= 4 && a <= 8) {
			return c == -6 ? 'h' : 'm';
		}
		return '.';
	}

	// ---------------------------------------------------------------- stone, ores and blocks

	/** Tileable stone or deepslate base, as RGB values. */
	static int[][] stone(final long seed, final boolean deepslate) {
		Random random = new Random(seed);
		float[][] coarse = new float[4][4];
		for (int y = 0; y < 4; y++) {
			for (int x = 0; x < 4; x++) {
				coarse[y][x] = random.nextFloat();
			}
		}
		int[] palette = deepslate ? DEEPSLATE : STONE;
		int[][] px = new int[16][16];
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				float v = 0.55F * sample(coarse, x / 4.0F, y / 4.0F) + 0.45F * random.nextFloat();
				if (deepslate) {
					// Horizontal layering
					v = 0.6F * v + 0.4F * (0.5F + 0.5F * (float) Math.sin(y * 1.6 + seed));
				}
				px[y][x] = palette[Math.max(0, Math.min(4, (int) (v * 5)))];
			}
		}
		return px;
	}

	static float sample(final float[][] grid, final float gx, final float gy) {
		int n = grid.length;
		int x0 = (int) Math.floor(gx), y0 = (int) Math.floor(gy);
		float fx = gx - x0, fy = gy - y0;
		float a = grid[Math.floorMod(y0, n)][Math.floorMod(x0, n)];
		float b = grid[Math.floorMod(y0, n)][Math.floorMod(x0 + 1, n)];
		float c = grid[Math.floorMod(y0 + 1, n)][Math.floorMod(x0, n)];
		float d = grid[Math.floorMod(y0 + 1, n)][Math.floorMod(x0 + 1, n)];
		return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy;
	}

	// Mythril crystals and bauxite nodules (o/d/m/l/h grids).
	static final String[][] CRYSTALS = {
		{".h.", "hlm", ".md"},
		{"hl", "md"},
		{"h..", "lm.", ".md"},
	};
	static final String[][] NODULES = {
		{".lm.", "lmmd", "mmdd", ".dd."},
		{"lm", "md"},
		{".l.", "lmd", ".d."},
	};
	// {template index, x, y}
	static final int[][] CRYSTAL_SPOTS_A = {{0, 2, 2}, {1, 10, 2}, {2, 6, 6}, {0, 11, 9}, {1, 3, 11}, {2, 8, 12}};
	static final int[][] CRYSTAL_SPOTS_B = {{2, 1, 1}, {0, 9, 3}, {1, 5, 7}, {0, 12, 10}, {2, 2, 11}, {1, 9, 13}};
	static final int[][] NODULE_SPOTS_A = {{0, 1, 1}, {1, 10, 2}, {2, 6, 6}, {0, 11, 9}, {1, 2, 11}, {2, 7, 12}};
	static final int[][] NODULE_SPOTS_B = {{2, 2, 1}, {0, 9, 2}, {1, 5, 7}, {0, 11, 10}, {2, 1, 11}, {1, 8, 13}};

	/**
	 * Stamps templates onto the stone. Crystals get a full dark rim; nodules only a bottom/right shadow,
	 * so they read as embedded lumps.
	 */
	static BufferedImage ore(final int[][] base, final Palette p, final String[][] templates, final int[][] spots, final boolean fullRim) {
		char[][] mask = new char[16][16];
		for (char[] row : mask) {
			java.util.Arrays.fill(row, '.');
		}
		for (int[] spot : spots) {
			String[] t = templates[spot[0]];
			for (int ty = 0; ty < t.length; ty++) {
				for (int tx = 0; tx < t[ty].length(); tx++) {
					char c = t[ty].charAt(tx);
					if (c != '.') {
						mask[(spot[2] + ty) & 15][(spot[1] + tx) & 15] = c;
					}
				}
			}
		}
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int rgb = base[y][x];
				if (mask[y][x] != '.') {
					rgb = p.of(mask[y][x]);
				} else {
					boolean rim = fullRim
						? isSet(mask, x - 1, y) || isSet(mask, x + 1, y) || isSet(mask, x, y - 1) || isSet(mask, x, y + 1)
						: isSet(mask, x - 1, y) || isSet(mask, x, y - 1);
					if (rim) {
						rgb = fullRim ? p.o() : shade(rgb, 0.7F);
					}
				}
				img.setRGB(x, y, opaque(rgb));
			}
		}
		return img;
	}

	static boolean isSet(final char[][] mask, final int x, final int y) {
		return mask[y & 15][x & 15] != '.';
	}

	/** Four bevelled 8x8 panels with an engraved diamond. */
	static BufferedImage paneledBlock(final Palette p) {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int px = x % 8, py = y % 8;
				char c;
				if (py == 0 || px == 0) {
					c = 'l';
				} else if (py == 7 || px == 7) {
					c = 'd';
				} else {
					double diamond = Math.abs(px - 3.5) + Math.abs(py - 3.5);
					if (diamond >= 1.9 && diamond <= 2.1) {
						c = px + py < 7 ? 'h' : 'm';
					} else if (diamond < 1.9) {
						c = 'l';
					} else {
						c = px + py <= 5 ? 'l' : 'm';
					}
				}
				img.setRGB(x, y, opaque(p.of(c)));
			}
		}
		img.setRGB(1, 1, opaque(p.h()));
		img.setRGB(9, 9, opaque(p.h()));
		return img;
	}

	/** Two brushed metal plates with rivets. */
	static BufferedImage brushedBlock(final Palette p, final long seed) {
		Random random = new Random(seed);
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) {
			int rowShift = random.nextInt(3) - 1;
			for (int x = 0; x < 16; x++) {
				int py = y % 8;
				char c;
				if (py == 0) {
					c = 'h';
				} else if (py == 7) {
					c = 'd';
				} else if (x == 0) {
					c = 'l';
				} else if (x == 15) {
					c = 'd';
				} else {
					// Horizontal streaks: brightness varies per row and per short run
					int tone = rowShift + ((x + y * 3) % 7 == 0 ? 1 : 0) - ((x * 5 + y) % 11 == 0 ? 1 : 0);
					c = tone > 0 ? 'l' : tone < 0 ? 'd' : 'm';
				}
				img.setRGB(x, y, opaque(p.of(c)));
			}
		}
		for (int plate = 0; plate < 2; plate++) {
			for (int rx : new int[] {2, 13}) {
				int ry = plate * 8 + 3;
				img.setRGB(rx, ry, opaque(p.o()));
				img.setRGB(rx - 1, ry - 1, opaque(p.h()));
			}
		}
		return img;
	}

	/** Raw ore block: tileable Voronoi lumps with dark seams and lit top-left rims. */
	static BufferedImage chunkyBlock(final Palette p, final long seed) {
		Random random = new Random(seed);
		int n = 9;
		float[][] pts = new float[n][2];
		char[] shades = new char[n];
		char[] options = {'d', 'm', 'm', 'l'};
		for (int i = 0; i < n; i++) {
			pts[i][0] = random.nextFloat() * 16;
			pts[i][1] = random.nextFloat() * 16;
			shades[i] = options[random.nextInt(options.length)];
		}
		int[][] owner = new int[16][16];
		boolean[][] seam = new boolean[16][16];
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				float best = Float.MAX_VALUE, second = Float.MAX_VALUE;
				int bestIndex = 0;
				for (int i = 0; i < n; i++) {
					float dx = Math.abs(x + 0.5F - pts[i][0]), dy = Math.abs(y + 0.5F - pts[i][1]);
					dx = Math.min(dx, 16 - dx);
					dy = Math.min(dy, 16 - dy);
					float dist = (float) Math.sqrt(dx * dx + dy * dy);
					if (dist < best) {
						second = best;
						best = dist;
						bestIndex = i;
					} else if (dist < second) {
						second = dist;
					}
				}
				owner[y][x] = bestIndex;
				seam[y][x] = second - best < 0.8F;
			}
		}
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				char c;
				if (seam[y][x]) {
					c = 'o';
				} else if (seam[(y + 15) & 15][x] || seam[y][(x + 15) & 15]) {
					c = shades[owner[y][x]] == 'l' ? 'h' : 'l';
				} else if (seam[(y + 1) & 15][x] || seam[y][(x + 1) & 15]) {
					c = 'd';
				} else {
					c = shades[owner[y][x]];
				}
				img.setRGB(x, y, opaque(p.of(c)));
			}
		}
		return img;
	}

	// ---------------------------------------------------------------- armor layers

	interface FacePainter {
		/** face: 0 top, 1 bottom, 2 right, 3 front, 4 left, 5 back. Returns a palette char or '.'. */
		char at(int face, int x, int y, int w, int h);
	}

	/** Paints a model box laid out with the standard cube UV unwrap starting at (u, v). */
	static void paintBox(final BufferedImage img, final Palette p, final int u, final int v, final int w, final int h, final int d, final FacePainter painter) {
		int[][] faces = {
			{u + d, v, w, d},
			{u + d + w, v, w, d},
			{u, v + d, d, h},
			{u + d, v + d, w, h},
			{u + d + w, v + d, d, h},
			{u + d + w + d, v + d, w, h},
		};
		for (int f = 0; f < 6; f++) {
			int fx = faces[f][0], fy = faces[f][1], fw = faces[f][2], fh = faces[f][3];
			for (int y = 0; y < fh; y++) {
				for (int x = 0; x < fw; x++) {
					char c = painter.at(f, x, y, fw, fh);
					if (c != '.' && fx + x < img.getWidth() && fy + y < img.getHeight()) {
						img.setRGB(fx + x, fy + y, opaque(p.of(c)));
					}
				}
			}
		}
	}

	/** Bevelled plate shading for one face. */
	static char plate(final int x, final int y, final int w, final int h) {
		if (y == 0 || x == 0) {
			return 'l';
		}
		if (y == h - 1 || x == w - 1) {
			return 'd';
		}
		return (x + y) % 5 == 0 ? 'l' : 'm';
	}

	static final int TOP = 0, BOTTOM = 1, FRONT = 3, BACK = 5;

	/** Adult layers (64x32): humanoid = helmet, chestplate, boots; humanoid_leggings = leggings. */
	static BufferedImage armorLayer(final Palette p, final boolean leggings) {
		BufferedImage img = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
		if (!leggings) {
			// Helmet: closed dome, brow band, cheek guards and a nose guard on the front.
			paintBox(img, p, 0, 0, 8, 8, 8, (f, x, y, w, h) -> {
				if (f == BOTTOM) {
					return '.';
				}
				if (f == TOP) {
					return x == 3 || x == 4 ? 'h' : plate(x, y, w, h);
				}
				if (f == FRONT) {
					boolean brow = y <= 2, cheek = (x == 0 || x == 7) && y <= 6, nose = (x == 3 || x == 4) && y <= 4;
					if (y == 2 && !cheek && !nose) {
						return 'd';
					}
					return brow || cheek || nose ? plate(x, y, w, h) : '.';
				}
				int depth = f == BACK ? 6 : 5;
				if (y > depth) {
					return '.';
				}
				return y == depth ? 'd' : plate(x, y, w, h + 1);
			});
			// Chestplate: body with a central ridge and a gem, trim along the bottom.
			paintBox(img, p, 16, 16, 8, 12, 4, (f, x, y, w, h) -> {
				if (f == BOTTOM) {
					return '.';
				}
				if (f == FRONT) {
					if ((x == 3 || x == 4) && (y == 3 || y == 4)) {
						return 'h';
					}
					if (x == 3 || x == 4) {
						return x == 3 ? 'l' : 'm';
					}
				}
				if (f != TOP && y == h - 2) {
					return 'l';
				}
				return plate(x, y, w, h);
			});
			// Shoulder pauldrons on the upper arm.
			paintBox(img, p, 40, 16, 4, 12, 4, (f, x, y, w, h) -> {
				if (f == BOTTOM) {
					return '.';
				}
				if (f == TOP) {
					return plate(x, y, w, h);
				}
				if (y > 5) {
					return '.';
				}
				return y == 5 ? 'd' : y == 4 ? 'l' : plate(x, y, w, 6);
			});
			// Boots: lower part of the legs plus the sole.
			paintBox(img, p, 0, 16, 4, 12, 4, (f, x, y, w, h) -> {
				if (f == TOP) {
					return '.';
				}
				if (f == BOTTOM) {
					return 'd';
				}
				if (y < 7) {
					return '.';
				}
				if (y == 7) {
					return 'l';
				}
				return y == h - 1 ? 'o' : plate(x, y - 7, w, h - 7);
			});
		} else {
			// Leggings: upper legs with knee plates.
			paintBox(img, p, 0, 16, 4, 12, 4, (f, x, y, w, h) -> {
				if (f == BOTTOM) {
					return '.';
				}
				if (f == TOP) {
					return plate(x, y, w, h);
				}
				if (y > 8) {
					return '.';
				}
				if (f == FRONT && (y == 5 || y == 6)) {
					return y == 5 ? 'h' : 'l';
				}
				return y == 8 ? 'd' : plate(x, y, w, 9);
			});
			// Belt around the waist.
			paintBox(img, p, 16, 16, 8, 12, 4, (f, x, y, w, h) -> {
				if (f == TOP) {
					return '.';
				}
				if (f == BOTTOM) {
					return 'm';
				}
				if (y < 9) {
					return '.';
				}
				if (y == 9) {
					return 'd';
				}
				return f == FRONT && (x == 3 || x == 4) && y == 10 ? 'h' : plate(x, y - 9, w, 3);
			});
		}
		return img;
	}

	/** Baby layer (64x64): every armor part lives in one texture with its own box layout. */
	static BufferedImage babyArmorLayer(final Palette p) {
		BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		FacePainter full = (f, x, y, w, h) -> f == BOTTOM ? 'd' : plate(x, y, w, h);
		paintBox(img, p, 0, 0, 9, 8, 8, (f, x, y, w, h) -> {
			if (f == BOTTOM) {
				return '.';
			}
			if (f == FRONT) {
				boolean brow = y <= 2, cheek = (x == 0 || x == w - 1) && y <= 6;
				return brow || cheek ? plate(x, y, w, h) : '.';
			}
			return y > 5 && f != BACK ? '.' : plate(x, y, w, h);
		});
		paintBox(img, p, 0, 17, 6, 5, 3, full);    // body
		paintBox(img, p, 0, 36, 6, 2, 3, full);    // waist
		paintBox(img, p, 30, 17, 2, 5, 3, full);   // left arm
		paintBox(img, p, 30, 25, 2, 5, 3, full);   // right arm
		paintBox(img, p, 18, 17, 3, 4, 3, full);   // right leg
		paintBox(img, p, 18, 24, 3, 4, 3, full);   // left leg
		paintBox(img, p, 0, 25, 3, 1, 3, full);    // right foot
		paintBox(img, p, 0, 29, 3, 1, 3, full);    // left foot
		return img;
	}

	// ---------------------------------------------------------------- Mythril Golem (128x128, iron golem model layout)

	static final Palette GOLEM = new Palette(0x121C33, 0x29406E, 0x4568A8, 0x7C9FD8, 0xCFE3FF);
	static final int GOLEM_EYE = 0x9FF4FF;

	/** Bevelled armor plates with seams every 6 rows, rough dark patches and a few crystal glints. */
	static FacePainter golemSurface(final int seed) {
		return (f, x, y, w, h) -> {
			char base = plate(x, y, w, h);
			if (base != 'm' && base != 'l') {
				return base;
			}
			int n = ((x * 73856093) ^ (y * 19349663) ^ ((f + seed) * 83492791)) & 0x7FFFFFFF;
			if (n % 29 == 0) {
				return 'h';
			}
			if (n % 7 == 0) {
				return 'd';
			}
			if (f != TOP && f != BOTTOM && y % 6 == 5) {
				return 'd';
			}
			return base;
		};
	}

	static BufferedImage mythrilGolemSkin() {
		BufferedImage img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
		paintBox(img, GOLEM, 0, 0, 8, 10, 8, golemSurface(1));    // head
		paintBox(img, GOLEM, 24, 0, 2, 4, 2, golemSurface(2));    // nose
		paintBox(img, GOLEM, 0, 40, 18, 12, 11, golemSurface(3)); // body
		paintBox(img, GOLEM, 0, 70, 9, 5, 6, golemSurface(4));    // waist
		paintBox(img, GOLEM, 60, 21, 4, 30, 6, golemSurface(5));  // right arm
		paintBox(img, GOLEM, 60, 58, 4, 30, 6, golemSurface(6));  // left arm
		paintBox(img, GOLEM, 37, 0, 6, 16, 5, golemSurface(7));   // right leg
		paintBox(img, GOLEM, 60, 0, 6, 16, 5, golemSurface(8));   // left leg

		// Face (head front is at 8,8 and 8x10): dark brow and two glowing eyes
		for (int x = 9; x <= 14; x++) {
			img.setRGB(x, 11, opaque(GOLEM.o()));
		}
		for (int x : new int[] {9, 10, 13, 14}) {
			img.setRGB(x, 12, opaque(GOLEM_EYE));
		}
		img.setRGB(9, 13, opaque(shade(GOLEM_EYE, 0.6F)));
		img.setRGB(14, 13, opaque(shade(GOLEM_EYE, 0.6F)));

		// Crystal core in the middle of the chest (body front is at 11,51 and 18x12)
		int cx = 11 + 9, cy = 51 + 5;
		for (int y = -3; y <= 3; y++) {
			for (int x = -3; x <= 3; x++) {
				int d = Math.abs(x) + Math.abs(y);
				if (d == 3) {
					img.setRGB(cx + x, cy + y, opaque(GOLEM.o()));
				} else if (d < 3) {
					img.setRGB(cx + x, cy + y, opaque(x + y < 0 ? GOLEM_EYE : (d == 0 ? 0xFFFFFF : shade(GOLEM_EYE, 0.75F))));
				}
			}
		}
		return img;
	}

	// ---------------------------------------------------------------- helpers

	static int opaque(final int rgb) {
		return 0xFF000000 | rgb;
	}

	static int shade(final int rgb, final float factor) {
		int r = Math.min(255, Math.max(0, Math.round(((rgb >> 16) & 0xFF) * factor)));
		int g = Math.min(255, Math.max(0, Math.round(((rgb >> 8) & 0xFF) * factor)));
		int b = Math.min(255, Math.max(0, Math.round((rgb & 0xFF) * factor)));
		return (r << 16) | (g << 8) | b;
	}

	// ---------------------------------------------------------------- economy, plastic, mob, effect

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

	// 6x6 shop sign emblems: A/B/C = dark/mid/light gray, R/r/W = red/dark red/highlight, G = leaf, S = stem, D/M/H = gem tones
	static final String[] ANVIL = {
		"......",
		"CCCCCC",
		"BBBBBA",
		".BBA..",
		".BBA..",
		"BBBBA.",
	};
	static final String[] APPLE = {
		"..SG..",
		".RRRR.",
		"RWRRRr",
		"RRRRRr",
		"RRRRrr",
		".rrrr.",
	};
	static final String[] GEM = {
		"......",
		".HMMD.",
		"HMMMMD",
		".HMMD.",
		"..MD..",
		"......",
	};

	static BufferedImage emblem(final String[] rows) {
		BufferedImage img = new BufferedImage(6, 6, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 6; y++) {
			for (int x = 0; x < 6; x++) {
				int rgb = switch (rows[y].charAt(x)) {
					case 'A' -> 0x2E2E2E;
					case 'B' -> 0x5A5A5A;
					case 'C' -> 0x8C8C8C;
					case 'R' -> 0xC0262D;
					case 'r' -> 0x7A1418;
					case 'W' -> 0xF07070;
					case 'G' -> 0x3E8E2E;
					case 'S' -> 0x5A3A1E;
					case 'D' -> 0x1E7F8C;
					case 'M' -> 0x3CCFE0;
					case 'H' -> 0xC8F8FF;
					default -> -1;
				};
				if (rgb != -1) {
					img.setRGB(x, y, opaque(rgb));
				}
			}
		}
		return img;
	}

	/** The gold coin scaled down to a 6x6 sign. */
	static BufferedImage coinEmblem() {
		BufferedImage coin = coin(0xF2C230, 0xB8860B, 0xFFF0A0, 0x6E4F05);
		BufferedImage img = new BufferedImage(6, 6, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 6; y++) {
			for (int x = 0; x < 6; x++) {
				img.setRGB(x, y, coin.getRGB(x * 16 / 6 + 1, y * 16 / 6 + 1));
			}
		}
		return img;
	}

	static BufferedImage shopSide(final int stripeA, final int stripeB, final BufferedImage emblem) {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		planks(img, 0, 0, 16, 16, 3);
		// Striped awning
		for (int y = 0; y < 6; y++) {
			for (int x = 0; x < 16; x++) {
				int color = (x / 2) % 2 == 0 ? stripeA : stripeB;
				img.setRGB(x, y, opaque(y == 0 ? shade(color, 0.8F) : color));
			}
		}
		// Scalloped edge
		for (int x = 0; x < 16; x++) {
			if (x % 2 == 0) {
				int color = (x / 2) % 2 == 0 ? stripeA : stripeB;
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
		// Shop sign on the counter
		for (int y = 0; y < 6; y++) {
			for (int x = 0; x < 6; x++) {
				int argb = emblem.getRGB(x, y);
				if ((argb >>> 24) > 0) {
					img.setRGB(5 + x, 9 + y, argb);
				}
			}
		}
		return img;
	}

	static BufferedImage shopTop(final int stripeA, final int stripeB) {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int color = (x / 2) % 2 == 0 ? stripeA : stripeB;
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
