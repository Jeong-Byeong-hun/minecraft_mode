import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Class-system art for TextureGen: tinted skill particles (white/grey sprites, the color comes from
 * the particle option), essence items, projectile display items, the engraving table, the guild
 * shop and the new effect icons. Class weapon textures are not here: datagen draws them from the
 * weapon definitions (client/datagen/art).
 */
final class ClassArt {
	static final String[] PARTICLES = {"spark", "slash", "orb", "ring", "rune", "shard", "smoke", "petal", "coin", "bolt", "feather", "bubble"};

	static void writeAll() throws IOException {
		TextureGen.write("particle/spark", spark());
		TextureGen.write("particle/slash", slash());
		TextureGen.write("particle/orb", orb());
		TextureGen.write("particle/ring", ring());
		TextureGen.write("particle/rune", rune());
		TextureGen.write("particle/shard", shard());
		TextureGen.write("particle/smoke", smoke());
		TextureGen.write("particle/petal", petal());
		TextureGen.write("particle/coin", coinParticle());
		TextureGen.write("particle/bolt", bolt());
		TextureGen.write("particle/feather", feather());
		TextureGen.write("particle/bubble", bubble());
		Path particles = TextureGen.out.getParent().resolve("particles");
		Files.createDirectories(particles);
		for (String name : PARTICLES) {
			String json = "{\n  \"textures\": [\n    \"minecraft_mode:" + name + "\"\n  ]\n}\n";
			Files.writeString(particles.resolve(name + ".json"), json, StandardCharsets.UTF_8);
		}

		TextureGen.write("item/essence", essence(false));
		TextureGen.write("item/condensed_essence", essence(true));
		TextureGen.write("item/golem_core", golemCore());
		TextureGen.write("item/class_reset_scroll", scroll());
		TextureGen.write("item/projectile_shuriken", shuriken());
		TextureGen.write("item/projectile_kunai", kunai());
		TextureGen.write("item/projectile_knife", knife());
		TextureGen.write("item/projectile_bullet", bullet());
		TextureGen.write("item/projectile_cannonball", cannonball());
		TextureGen.write("item/projectile_icicle", icicle());
		TextureGen.write("item/projectile_harpoon", harpoonHead());

		TextureGen.write("block/engraving_table_top", engravingTop());
		TextureGen.write("block/engraving_table_side", engravingSide());
		TextureGen.write("block/engraving_table_bottom", obsidian(91));
		TextureGen.write("block/guild_shop_side", TextureGen.shopSide(0x2B2D6E, 0xE0B23A, TextureGen.emblem(SWORDS)));
		TextureGen.write("block/guild_shop_top", TextureGen.shopTop(0x2B2D6E, 0xE0B23A));

		TextureGen.write("mob_effect/stun", stunIcon());
		TextureGen.write("mob_effect/vulnerable", vulnerableIcon());
		TextureGen.write("mob_effect/mana_flow", manaIcon());
	}

	static final String[] SWORDS = {
		"C....C",
		".C..C.",
		"..CC..",
		"..CC..",
		".SBBS.",
		"S....S",
	};

	// ---------------------------------------------------------------- helpers

	static BufferedImage image(final int size) {
		return new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
	}

	/** Grey level 0..255 at alpha 0..255. */
	static void grey(final BufferedImage img, final int x, final int y, final int level, final int alpha) {
		if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight() || alpha <= 0) {
			return;
		}
		int l = Math.max(0, Math.min(255, level));
		int a = Math.max(0, Math.min(255, alpha));
		int existing = img.getRGB(x, y);
		if ((existing >>> 24) >= a) {
			return;
		}
		img.setRGB(x, y, a << 24 | l << 16 | l << 8 | l);
	}

	static void rgb(final BufferedImage img, final int x, final int y, final int rgb) {
		if (x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight()) {
			img.setRGB(x, y, 0xFF000000 | rgb);
		}
	}

	// ---------------------------------------------------------------- particles (tinted)

	static BufferedImage spark() {
		BufferedImage img = image(8);
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 8; x++) {
				double dx = Math.abs(x - 3.5);
				double dy = Math.abs(y - 3.5);
				double star = Math.min(dx, dy) * 2.2 + Math.max(dx, dy) * 0.55;
				if (star < 2.6) {
					grey(img, x, y, 255, (int)(255 * (1.0 - star / 2.8)) + 40);
				}
			}
		}
		return img;
	}

	static BufferedImage slash() {
		BufferedImage img = image(16);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double d1 = Math.hypot(x - 7.5, y - 11.0);
				double d2 = Math.hypot(x - 7.5, y - 13.5);
				if (d1 <= 8.0 && d2 >= 8.0 && y <= 10) {
					double edge = 8.0 - d1;
					double fade = 1.0 - Math.abs(x - 7.5) / 8.0;
					int alpha = (int)(255 * Math.min(1.0, fade * 1.6));
					grey(img, x, y, edge < 1.0 ? 255 : 210, alpha);
				}
			}
		}
		return img;
	}

	static BufferedImage orb() {
		BufferedImage img = image(8);
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 8; x++) {
				double d = Math.hypot(x - 3.5, y - 3.5);
				if (d <= 3.8) {
					grey(img, x, y, d < 1.5 ? 255 : 220, (int)(255 * (1.0 - d / 4.2)) + 30);
				}
			}
		}
		return img;
	}

	static BufferedImage ring() {
		BufferedImage img = image(16);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double d = Math.hypot(x - 7.5, y - 7.5);
				double off = Math.abs(d - 6.4);
				if (off < 1.2) {
					grey(img, x, y, 255, (int)(255 * (1.0 - off / 1.3)));
				}
			}
		}
		return img;
	}

	static BufferedImage rune() {
		BufferedImage img = image(16);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double d = Math.hypot(x - 7.5, y - 7.5);
				if (Math.abs(d - 7.0) < 0.6 || Math.abs(d - 4.6) < 0.5) {
					grey(img, x, y, 255, 230);
				}
			}
		}
		// hexagram between the circles
		for (int i = 0; i < 6; i++) {
			double a1 = Math.PI / 3 * i;
			double a2 = Math.PI / 3 * (i + 2);
			line(img, 7.5 + Math.cos(a1) * 4.6, 7.5 + Math.sin(a1) * 4.6, 7.5 + Math.cos(a2) * 4.6, 7.5 + Math.sin(a2) * 4.6, 200);
		}
		// glyph ticks on the outer band
		for (int i = 0; i < 12; i++) {
			double a = Math.PI / 6 * i + 0.2;
			grey(img, (int)Math.round(7.5 + Math.cos(a) * 5.8), (int)Math.round(7.5 + Math.sin(a) * 5.8), 255, 255);
		}
		return img;
	}

	static void line(final BufferedImage img, final double x0, final double y0, final double x1, final double y1, final int alpha) {
		int steps = 24;
		for (int i = 0; i <= steps; i++) {
			double t = i / (double)steps;
			grey(img, (int)Math.round(x0 + (x1 - x0) * t), (int)Math.round(y0 + (y1 - y0) * t), 235, alpha);
		}
	}

	static BufferedImage shard() {
		BufferedImage img = image(8);
		String[] rows = {
			"...#....",
			"...##...",
			"..###...",
			"..####..",
			".#####..",
			".####...",
			"..##....",
			"........",
		};
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 8; x++) {
				if (rows[y].charAt(x) == '#') {
					grey(img, x, y, x + y < 7 ? 255 : 185, 255);
				}
			}
		}
		return img;
	}

	static BufferedImage smoke() {
		BufferedImage img = image(8);
		Random random = new Random(5);
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 8; x++) {
				double d = Math.hypot(x - 3.5, y - 3.8) + random.nextDouble() * 0.9;
				if (d < 3.9) {
					grey(img, x, y, 200 + (int)(x + y < 7 ? 40 : 0), (int)(220 * (1.0 - d / 4.4)) + 25);
				}
			}
		}
		return img;
	}

	static BufferedImage petal() {
		BufferedImage img = image(8);
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 8; x++) {
				double dx = (x - 3.5) / 3.2;
				double dy = (y - 3.5) / 2.0;
				double rx = dx * 0.8 + dy * 0.6;
				double ry = -dx * 0.6 + dy * 0.8;
				if (rx * rx + ry * ry * 2.2 <= 1.0) {
					grey(img, x, y, ry < 0 ? 255 : 215, 255);
				}
			}
		}
		grey(img, 6, 1, 0, 0);
		return img;
	}

	static BufferedImage coinParticle() {
		BufferedImage img = image(8);
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 8; x++) {
				double d = Math.hypot(x - 3.5, y - 3.5);
				if (d <= 3.6) {
					int level = d > 2.8 ? 150 : Math.abs(x - 3.5) + Math.abs(y - 3.5) <= 1.5 ? 255 : 215;
					grey(img, x, y, level, 255);
				}
			}
		}
		return img;
	}

	static BufferedImage bolt() {
		BufferedImage img = image(8);
		int[][] path = {{5, 0}, {4, 1}, {3, 2}, {4, 3}, {5, 3}, {4, 4}, {3, 5}, {2, 6}, {2, 7}};
		for (int[] p : path) {
			grey(img, p[0], p[1], 255, 255);
			grey(img, p[0] + 1, p[1], 200, 150);
			grey(img, p[0] - 1, p[1], 200, 110);
		}
		return img;
	}

	static BufferedImage feather() {
		BufferedImage img = image(8);
		for (int i = 0; i < 8; i++) {
			grey(img, i, 7 - i, 170, 255);
		}
		for (int i = 1; i < 7; i++) {
			int w = i < 4 ? i / 2 + 1 : (8 - i) / 2 + 1;
			for (int k = 1; k <= w; k++) {
				grey(img, i - k, 7 - i - k, 255, 230);
				grey(img, i + k, 7 - i + k, 225, 230);
			}
		}
		return img;
	}

	static BufferedImage bubble() {
		BufferedImage img = image(8);
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 8; x++) {
				double d = Math.hypot(x - 3.5, y - 3.5);
				if (Math.abs(d - 3.0) < 0.75) {
					grey(img, x, y, 255, 230);
				} else if (d < 2.4) {
					grey(img, x, y, 255, 60);
				}
			}
		}
		grey(img, 2, 2, 255, 255);
		return img;
	}

	// ---------------------------------------------------------------- items

	/** Glowing violet wisp in a crystal; condensed = larger, brighter cluster. */
	static BufferedImage essence(final boolean condensed) {
		BufferedImage img = image(16);
		int outline = 0x2A0E4A;
		int dark = 0x5B21A6;
		int mid = 0x9B4DFF;
		int light = 0xD3A6FF;
		int core = 0xFFF2FF;
		double size = condensed ? 6.6 : 5.0;
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				// diamond with a flame tip on top
				double dx = Math.abs(x - 7.5);
				double dy = y - 8.5;
				double shape = dx + Math.abs(dy) * (dy < 0 ? 0.62 : 0.9);
				if (shape <= size) {
					int color = shape > size - 1.0 ? outline : shape > size - 2.2 ? dark : shape > size - 3.6 ? mid : shape > 1.2 ? light : core;
					if (shape > size - 3.6 && x < 7 && y < 9 && shape <= size - 1.0) {
						color = shape > size - 2.2 ? mid : light;
					}
					rgb(img, x, y, color);
				}
			}
		}
		if (condensed) {
			// two small satellite crystals
			int[][] sat = {{2, 12}, {13, 11}};
			for (int[] s : sat) {
				rgb(img, s[0], s[1] - 1, light);
				rgb(img, s[0], s[1], mid);
				rgb(img, s[0] - 1, s[1], dark);
				rgb(img, s[0] + 1, s[1], dark);
				rgb(img, s[0], s[1] + 1, outline);
			}
			rgb(img, 7, 3, core);
		}
		rgb(img, 6, 6, core);
		return img;
	}

	static BufferedImage golemCore() {
		BufferedImage img = image(16);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double d = Math.hypot(x - 7.5, y - 7.5);
				double oct = Math.max(Math.abs(x - 7.5), Math.abs(y - 7.5)) * 0.75 + Math.min(Math.abs(x - 7.5), Math.abs(y - 7.5)) * 0.55;
				if (oct <= 6.2) {
					int color = oct > 5.4 ? 0x16233F : oct > 4.3 ? 0x4C7BC8 : d > 2.6 ? 0x2B4783 : d > 1.2 ? 0x7FF6FF : 0xE8FFFF;
					if (oct <= 4.3 && oct > 2.6 && x + y < 15) {
						color = 0x8AB2EE;
					}
					rgb(img, x, y, color);
				}
			}
		}
		// rune ticks
		rgb(img, 7, 3, 0x9FF4FF);
		rgb(img, 3, 8, 0x9FF4FF);
		rgb(img, 12, 7, 0x9FF4FF);
		rgb(img, 8, 12, 0x9FF4FF);
		return img;
	}

	static BufferedImage scroll() {
		BufferedImage img = image(16);
		int paper = 0xF1E2B8;
		int paperDark = 0xC9B27A;
		int outline = 0x5A4320;
		// rolled ends
		for (int x = 2; x <= 13; x++) {
			rgb(img, x, 2, outline);
			rgb(img, x, 3, x < 5 ? 0xFFF4D6 : paperDark);
			rgb(img, x, 4, outline);
			rgb(img, x, 12, outline);
			rgb(img, x, 13, paperDark);
			rgb(img, x, 14, outline);
		}
		for (int y = 5; y <= 11; y++) {
			rgb(img, 3, y, outline);
			rgb(img, 12, y, outline);
			for (int x = 4; x <= 11; x++) {
				rgb(img, x, y, x + y < 12 ? 0xFFF4D6 : paper);
			}
		}
		// writing
		for (int x = 5; x <= 10; x++) {
			if (x != 8) {
				rgb(img, x, 6, 0x8A7550);
				rgb(img, x, 8, 0x8A7550);
			}
		}
		// red wax seal
		rgb(img, 9, 10, 0xC0262D);
		rgb(img, 10, 10, 0x7A1418);
		rgb(img, 9, 11, 0x7A1418);
		rgb(img, 10, 11, 0xC0262D);
		rgb(img, 8, 10, 0xF07070);
		return img;
	}

	static BufferedImage shuriken() {
		BufferedImage img = image(16);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double dx = x - 7.5;
				double dy = y - 7.5;
				double r = Math.hypot(dx, dy);
				double reach = 2.2 + 5.2 * Math.pow(Math.abs(Math.cos(2 * Math.atan2(dy, dx))), 5);
				if (r <= reach && r >= 1.0) {
					rgb(img, x, y, r > reach - 0.9 ? 0x2E3238 : dx + dy < 0 ? 0xC9D1DA : 0x7D8793);
				}
			}
		}
		return img;
	}

	static BufferedImage kunai() {
		BufferedImage img = image(16);
		String[] rows = {
			"...............o",
			"..............oh",
			".............ohm",
			"............ohmd",
			"...........ohmdo",
			"..........ohmdo.",
			".........ohmdo..",
			"........ommdo...",
			".......oWWo.....",
			"......oWWo......",
			".....oBWo.......",
			"....oWWo........",
			"...oooo.........",
			"..o..o..........",
			"..o..o..........",
			"...oo...........",
		};
		return grid(rows);
	}

	static BufferedImage knife() {
		BufferedImage img = grid(new String[] {
			"................",
			"..............o.",
			".............oho",
			"............ohmo",
			"...........ohmo.",
			"..........ohmo..",
			".........ohmo...",
			"........ohmo....",
			".......ohmo.....",
			"......oooo......",
			".....oBBo.......",
			"....oBBo........",
			"...oBBo.........",
			"...ooo..........",
			"................",
			"................",
		});
		return img;
	}

	static BufferedImage bullet() {
		return grid(new String[] {
			"................",
			"................",
			"................",
			"................",
			"................",
			"......ooo.......",
			".....oGGGo......",
			"....oGgggGo.....",
			"....oGgggGo.....",
			"....oYYYYYo.....",
			"....oYyyyYo.....",
			"....oYyyyYo.....",
			"....oYyyyYo.....",
			"....ooooooo.....",
			"................",
			"................",
		});
	}

	static BufferedImage cannonball() {
		BufferedImage img = image(16);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double d = Math.hypot(x - 7.5, y - 8.0);
				if (d <= 5.6) {
					int color = d > 4.9 ? 0x0E0E10 : x + y < 12 ? 0x55585E : x + y < 16 ? 0x3A3D42 : 0x26282C;
					rgb(img, x, y, color);
				}
			}
		}
		rgb(img, 5, 5, 0x9AA0A8);
		return img;
	}

	static BufferedImage icicle() {
		return grid(new String[] {
			"..............o.",
			".............oIo",
			"............oIio",
			"...........oIiio",
			"..........oIiio.",
			".........oIiio..",
			"........oIiio...",
			".......oIiio....",
			"......oIiio.....",
			".....oIiio......",
			"....oIiio.......",
			"...oIiio........",
			"..oIiio.........",
			".oIiio..........",
			".oioo...........",
			"..o.............",
		});
	}

	static BufferedImage harpoonHead() {
		return grid(new String[] {
			"..........oooo..",
			".........ohhmo..",
			"........ohmmo...",
			".......ohmmdo...",
			"...o..ohmmdo....",
			"..omoohmmdo.....",
			"...ommmmdo......",
			"....ommdo.......",
			"...oBBoo........",
			"..oBBo..........",
			".oBBo...........",
			"oBBo............",
			"BBo.............",
			"Bo..............",
			"................",
			"................",
		});
	}

	/** Small fixed palette: o outline, h/m/d steel, B wood, W wrap, G/g copper tip, Y/y brass, I/i ice. */
	static BufferedImage grid(final String[] rows) {
		BufferedImage img = image(16);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int color = switch (rows[y].charAt(x)) {
					case 'o' -> 0x1F2328;
					case 'h' -> 0xE6ECF2;
					case 'm' -> 0xAAB4BE;
					case 'd' -> 0x6B7480;
					case 'B' -> 0x6A4A2A;
					case 'W' -> 0x8C2E2E;
					case 'G' -> 0xB86A3A;
					case 'g' -> 0xE09A62;
					case 'Y' -> 0xB8902E;
					case 'y' -> 0xE8C45A;
					case 'I' -> 0xCFF4FF;
					case 'i' -> 0x8CD8F2;
					default -> -1;
				};
				if (color != -1) {
					rgb(img, x, y, color);
				}
			}
		}
		return img;
	}

	// ---------------------------------------------------------------- blocks

	static BufferedImage obsidian(final long seed) {
		BufferedImage img = image(16);
		Random random = new Random(seed);
		int[] tones = {0x0F0A18, 0x1A1028, 0x241638, 0x2F1D48, 0x3D2A5C};
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int t = (int)Math.floor(1.5 + random.nextGaussian() * 0.9);
				rgb(img, x, y, tones[Math.max(0, Math.min(4, t))]);
			}
		}
		return img;
	}

	static BufferedImage engravingTop() {
		BufferedImage img = obsidian(77);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double d = Math.hypot(x - 7.5, y - 7.5);
				if (Math.abs(d - 6.3) < 0.55 || Math.abs(d - 3.6) < 0.5) {
					rgb(img, x, y, 0xB36BFF);
				}
			}
		}
		for (int i = 0; i < 6; i++) {
			double a = Math.PI / 3 * i + Math.PI / 6;
			rgb(img, (int)Math.round(7.5 + Math.cos(a) * 5.0), (int)Math.round(7.5 + Math.sin(a) * 5.0), 0xE7CCFF);
		}
		rgb(img, 7, 7, 0xFFFFFF);
		rgb(img, 8, 8, 0xE7CCFF);
		rgb(img, 7, 8, 0x9B4DFF);
		rgb(img, 8, 7, 0x9B4DFF);
		// metal corners
		int[][] corners = {{0, 0}, {15, 0}, {0, 15}, {15, 15}};
		for (int[] c : corners) {
			rgb(img, c[0], c[1], 0x8AB2EE);
		}
		return img;
	}

	static BufferedImage engravingSide() {
		BufferedImage img = obsidian(78);
		// mythril trim on top and bottom
		for (int x = 0; x < 16; x++) {
			rgb(img, x, 0, 0x8AB2EE);
			rgb(img, x, 1, 0x4C7BC8);
			rgb(img, x, 14, 0x4C7BC8);
			rgb(img, x, 15, 0x2B4783);
		}
		// glowing essence runes
		int[][] runes = {{3, 5}, {3, 6}, {4, 7}, {3, 8}, {7, 4}, {8, 5}, {7, 6}, {8, 7}, {7, 8}, {12, 5}, {11, 6}, {12, 7}, {12, 8}, {11, 9}};
		for (int[] r : runes) {
			rgb(img, r[0], r[1], 0xB36BFF);
		}
		for (int x = 2; x <= 13; x++) {
			rgb(img, x, 11, x % 3 == 0 ? 0xE7CCFF : 0x9B4DFF);
		}
		return img;
	}

	// ---------------------------------------------------------------- effect icons (18x18)

	static BufferedImage stunIcon() {
		BufferedImage img = image(18);
		// three stars circling
		int[][] stars = {{4, 7}, {9, 3}, {14, 7}};
		for (int[] s : stars) {
			for (int dy = -2; dy <= 2; dy++) {
				for (int dx = -2; dx <= 2; dx++) {
					if (Math.abs(dx) + Math.abs(dy) <= 2 && (dx == 0 || dy == 0)) {
						rgb(img, s[0] + dx, s[1] + dy, Math.abs(dx) + Math.abs(dy) == 2 ? 0xC79A10 : 0xFFE45A);
					}
				}
			}
			rgb(img, s[0], s[1], 0xFFFFFF);
		}
		// swirl
		for (int i = 0; i < 40; i++) {
			double a = i * 0.32;
			double r = 1.0 + i * 0.12;
			rgb(img, (int)Math.round(9 + Math.cos(a) * r), (int)Math.round(12 + Math.sin(a) * r * 0.6), 0xF2D649);
		}
		return img;
	}

	static BufferedImage vulnerableIcon() {
		BufferedImage img = image(18);
		// cracked shield
		for (int y = 2; y < 16; y++) {
			for (int x = 3; x < 15; x++) {
				double half = y < 9 ? 6.0 : 6.0 - (y - 9) * 0.85;
				if (Math.abs(x - 8.5) <= half) {
					boolean edge = Math.abs(x - 8.5) > half - 1.0 || y == 2;
					rgb(img, x, y, edge ? 0x5A0A2E : x < 9 ? 0xE04888 : 0xC2185B);
				}
			}
		}
		int[][] crack = {{9, 3}, {8, 5}, {10, 7}, {8, 9}, {9, 11}, {8, 13}};
		for (int i = 0; i + 1 < crack.length; i++) {
			int x0 = crack[i][0];
			int y0 = crack[i][1];
			int x1 = crack[i + 1][0];
			int y1 = crack[i + 1][1];
			for (int s = 0; s <= 4; s++) {
				rgb(img, x0 + (x1 - x0) * s / 4, y0 + (y1 - y0) * s / 4, 0x1A0010);
			}
		}
		return img;
	}

	static BufferedImage manaIcon() {
		BufferedImage img = image(18);
		for (int y = 1; y < 17; y++) {
			for (int x = 1; x < 17; x++) {
				double round = Math.hypot(x - 8.5, y - 11.0);
				boolean tip = y >= 2 && y <= 11 && Math.abs(x - 8.5) <= (y - 2) * 0.55;
				if (round <= 5.2 || tip) {
					boolean border = round > 4.3 && !tip || tip && Math.abs(x - 8.5) > (y - 2) * 0.55 - 0.9 && round > 4.3;
					rgb(img, x, y, border ? 0x0E2A6B : x < 8 && y > 8 && y < 12 ? 0x9CC3FF : 0x3D7BFF);
				}
			}
		}
		rgb(img, 7, 9, 0xFFFFFF);
		return img;
	}

	private ClassArt() {
	}
}
