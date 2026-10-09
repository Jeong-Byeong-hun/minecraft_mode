import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

/**
 * Advancement-trial art for TextureGen: the 20 trial token items and the five class trainer skins
 * (64x64, legacy humanoid layout like the Mine Raider; the hat layer at 32,0 is used for hats).
 */
final class QuestArt {
	static void writeAll() throws IOException {
		TextureGen.write("item/rusted_medal", rustedMedal());
		TextureGen.write("item/champions_laurel", laurel());
		TextureGen.write("item/frenzy_blood", bloodDrop());
		TextureGen.write("item/grail_shard", grail());
		TextureGen.write("item/thieves_token", thievesToken());
		TextureGen.write("item/ninja_scroll", ninjaScroll());
		TextureGen.write("item/assassin_seal", assassinSeal());
		TextureGen.write("item/monarchs_shadow", monarchsShadow());
		TextureGen.write("item/arcane_dust", arcaneDust());
		TextureGen.write("item/grimoire_page", grimoirePage());
		TextureGen.write("item/ember_core", emberCore());
		TextureGen.write("item/akashic_fragment", akashicFragment());
		TextureGen.write("item/steel_arrowhead", grid(ARROWHEAD, "o1F2328 hE6ECF2 mAAB4BE d6B7480 B6A4A2A"));
		TextureGen.write("item/phantom_plume", plume());
		TextureGen.write("item/spirit_arrow", spiritArrow());
		TextureGen.write("item/golden_key", goldenKey());
		TextureGen.write("item/map_scrap", mapScrap());
		TextureGen.write("item/bounty_poster", bountyPoster());
		TextureGen.write("item/haki_crystal", hakiCrystal());
		TextureGen.write("item/sea_kings_treasure", treasureChest());

		TextureGen.write("item/evolution_ether", evolutionEther());

		TextureGen.write("entity/trainer/warrior", bedivere());
		TextureGen.write("entity/trainer/rogue", hanzo());
		TextureGen.write("entity/trainer/mage", merlin());
		TextureGen.write("entity/trainer/archer", chiron());
		TextureGen.write("entity/trainer/pirate", drake());
		TextureGen.write("entity/npc/blacksmith", volund());
		TextureGen.write("entity/npc/raid_marshal", aldric());
	}

	// ---------------------------------------------------------------- helpers

	interface Inside {
		boolean at(int x, int y);
	}

	interface Shader {
		int at(int x, int y);
	}

	static BufferedImage image() {
		return ClassArt.image(16);
	}

	static void rgb(final BufferedImage img, final int x, final int y, final int rgb) {
		ClassArt.rgb(img, x, y, rgb);
	}

	static boolean in(final Inside shape, final int x, final int y) {
		return x >= 0 && y >= 0 && x < 16 && y < 16 && shape.at(x, y);
	}

	/** Fills a shape; pixels on its border get the outline colour, the rest come from the shader. */
	static void shape(final BufferedImage img, final Inside shape, final int outline, final Shader shader) {
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				if (in(shape, x, y)) {
					boolean edge = !in(shape, x - 1, y) || !in(shape, x + 1, y) || !in(shape, x, y - 1) || !in(shape, x, y + 1);
					rgb(img, x, y, edge ? outline : shader.at(x, y));
				}
			}
		}
	}

	/** Lit from the top left: light, mid or dark by how far the pixel sits toward the light. */
	static Shader lit(final double cx, final double cy, final double r, final int dark, final int mid, final int light) {
		return (x, y) -> {
			double toward = ((cx - x) + (cy - y)) / r;
			return toward > 0.45 ? light : toward < -0.55 ? dark : mid;
		};
	}

	/** A 3x3 twinkle: bright centre, dim arms. */
	static void sparkle(final BufferedImage img, final int x, final int y, final int center, final int arm) {
		rgb(img, x, y, center);
		rgb(img, x - 1, y, arm);
		rgb(img, x + 1, y, arm);
		rgb(img, x, y - 1, arm);
		rgb(img, x, y + 1, arm);
	}

	static void line(final BufferedImage img, final int x0, final int y0, final int x1, final int y1, final int rgb) {
		int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
		for (int i = 0; i <= steps; i++) {
			double t = steps == 0 ? 0 : (double)i / steps;
			rgb(img, (int)Math.round(x0 + (x1 - x0) * t), (int)Math.round(y0 + (y1 - y0) * t), rgb);
		}
	}

	static int hash(final int x, final int y, final int seed) {
		int h = x * 374761393 + y * 668265263 + seed * 982451653;
		h = (h ^ (h >>> 13)) * 1274126177;
		return (h ^ (h >>> 16)) & 0x7FFFFFFF;
	}

	/** 16x16 grid; the palette is "cRRGGBB" entries separated by spaces, '.' is transparent. */
	static BufferedImage grid(final String[] rows, final String palette) {
		int[] colors = new int[128];
		java.util.Arrays.fill(colors, -1);
		for (String entry : palette.split(" ")) {
			colors[entry.charAt(0)] = Integer.parseInt(entry.substring(1), 16);
		}
		BufferedImage img = image();
		for (int y = 0; y < 16; y++) {
			if (rows[y].length() != 16) {
				throw new IllegalArgumentException("row " + y + " has length " + rows[y].length() + ": " + rows[y]);
			}
			for (int x = 0; x < 16; x++) {
				char c = rows[y].charAt(x);
				if (c != '.') {
					if (colors[c] == -1) {
						throw new IllegalArgumentException("no colour for '" + c + "'");
					}
					rgb(img, x, y, colors[c]);
				}
			}
		}
		return img;
	}

	// ---------------------------------------------------------------- warrior tokens

	static BufferedImage rustedMedal() {
		BufferedImage img = image();
		// ribbon: red and blue halves
		for (int y = 0; y <= 7; y++) {
			rgb(img, 4, y, 0x1A1014);
			rgb(img, 11, y, 0x1A1014);
			for (int x = 5; x <= 10; x++) {
				int color = y == 0 ? 0x1A1014 : x <= 7 ? (x == 5 ? 0xD0505A : 0xA8323A) : (x == 10 ? 0x1E3266 : 0x2E4E9E);
				rgb(img, x, y, color);
			}
		}
		shape(img, (x, y) -> Math.hypot(x - 7.5, y - 10.5) <= 5.2, 0x3A2210, lit(7.5, 10.5, 5.0, 0x7A4A22, 0xA8693A, 0xD89A5A));
		// embossed star and rust / patina spots
		int[][] star = {{7, 8}, {8, 8}, {6, 10}, {7, 10}, {8, 10}, {9, 10}, {7, 9}, {8, 9}, {7, 11}, {8, 11}, {6, 12}, {9, 12}};
		for (int[] p : star) {
			rgb(img, p[0], p[1], 0xE8B070);
		}
		int[][] patina = {{4, 11}, {5, 13}, {10, 13}, {11, 10}, {9, 14}};
		for (int[] p : patina) {
			rgb(img, p[0], p[1], 0x4E8A7A);
		}
		rgb(img, 5, 9, 0xB8582A);
		rgb(img, 10, 8, 0xB8582A);
		return img;
	}

	static BufferedImage laurel() {
		BufferedImage img = image();
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double dx = x - 7.5;
				double dy = y - 7.5;
				double r = Math.hypot(dx, dy);
				double angle = Math.atan2(dy, dx);
				if (Math.abs(angle + Math.PI / 2) < 0.5) {
					continue; // gap at the top
				}
				// leaves run from the bottom up both sides; mirror the phase so both halves match
				double phase = Math.abs(Math.atan2(dx, dy)) / Math.PI * 7.0;
				double t = phase - Math.floor(phase);
				double width = 0.35 + 1.35 * Math.sin(Math.PI * t);
				double off = Math.abs(r - 5.4);
				if (off <= width) {
					int color = off > width - 0.6 ? 0x24461A : t < 0.5 ? 0x8CC84A : 0x5E9A32;
					rgb(img, x, y, color);
				}
			}
		}
		// gold ribbon knot at the bottom
		int[][] ribbon = {{6, 13}, {9, 13}, {5, 14}, {10, 14}, {4, 15}, {11, 15}};
		for (int[] p : ribbon) {
			rgb(img, p[0], p[1], 0xB8902E);
		}
		rgb(img, 7, 13, 0xFFF0A0);
		rgb(img, 8, 13, 0xE8C45A);
		rgb(img, 7, 12, 0xE8C45A);
		rgb(img, 8, 12, 0xB8902E);
		return img;
	}

	static BufferedImage bloodDrop() {
		BufferedImage img = image();
		Inside drop = (x, y) -> Math.hypot(x - 7.5, y - 10.0) <= 4.8 || (y >= 1 && y < 10 && Math.abs(x - 7.5) <= 4.8 * (y - 0.5) / 9.5);
		shape(img, drop, 0x3A0408, lit(7.5, 9.5, 4.5, 0x6E0A14, 0xB01624, 0xE04050));
		rgb(img, 6, 6, 0xFFB0B8);
		rgb(img, 5, 9, 0xFFB0B8);
		rgb(img, 5, 10, 0xF07080);
		rgb(img, 5, 8, 0xF07080);
		return img;
	}

	static final String[] GRAIL = {
		"................",
		"...oooooooooo...",
		"...ohhllllmmo...",
		"...olllcllmdo...",
		"....ollcmmdo....",
		"....olllcmdo....",
		".....olrrmo.....",
		"......omdo......",
		".......md.......",
		".......md.......",
		"......omdo......",
		".....olmmdo.....",
		"....olllmmdo....",
		"....oooooooo....",
		"................",
		"................",
	};

	static BufferedImage grail() {
		BufferedImage img = grid(GRAIL, "o5A3A08 dA8741A mD9A632 lF2D06A hFFF4C0 c3A2406 rC0263A");
		sparkle(img, 13, 2, 0xFFFFFF, 0xFFF4C0);
		return img;
	}

	// ---------------------------------------------------------------- rogue tokens

	static BufferedImage thievesToken() {
		BufferedImage img = image();
		shape(img, (x, y) -> Math.hypot(x - 7.5, y - 7.5) <= 7.2, 0x101014, (x, y) -> {
			double d = Math.hypot(x - 7.5, y - 7.5);
			if (d > 5.2) {
				return x + y < 15 ? 0xE0E4EA : 0x9AA2AE; // silver rim
			}
			return x + y < 13 ? 0x5E5E6A : 0x40404A;
		});
		// black domino mask with eye holes
		for (int x = 3; x <= 12; x++) {
			rgb(img, x, 7, 0x0C0C10);
			if (x >= 4 && x <= 11) {
				rgb(img, x, 8, 0x0C0C10);
			}
		}
		for (int x : new int[] {4, 5, 6, 9, 10, 11}) {
			rgb(img, x, 6, 0x0C0C10);
		}
		rgb(img, 5, 7, 0xE0E4EA);
		rgb(img, 10, 7, 0xE0E4EA);
		rgb(img, 7, 9, 0x0C0C10);
		rgb(img, 8, 9, 0x0C0C10);
		rgb(img, 2, 7, 0x0C0C10);
		rgb(img, 13, 7, 0x0C0C10);
		return img;
	}

	static BufferedImage ninjaScroll() {
		BufferedImage img = image();
		// paper
		shape(img, (x, y) -> x >= 2 && x <= 13 && y >= 5 && y <= 10, 0x4A3A20, (x, y) -> y >= 9 ? 0xC8B488 : 0xEADBB4);
		// rolled ends
		for (int side : new int[] {1, 12}) {
			for (int y = 3; y <= 12; y++) {
				for (int x = side; x <= side + 2; x++) {
					boolean edge = y == 3 || y == 12 || x == side || x == side + 2;
					rgb(img, x, y, edge ? 0x2A0A0A : x == side + 1 && y < 7 ? 0xC04040 : 0x8A2424);
				}
			}
		}
		// brush strokes
		int[][] ink = {{5, 6}, {6, 6}, {7, 6}, {6, 7}, {5, 8}, {6, 8}, {7, 8}, {6, 9}, {9, 6}, {9, 7}, {10, 7}, {9, 8}, {10, 9}, {11, 6}};
		for (int[] p : ink) {
			rgb(img, p[0], p[1], 0x1E1A16);
		}
		// red cord
		rgb(img, 8, 11, 0xC0262D);
		rgb(img, 8, 12, 0xC0262D);
		rgb(img, 9, 13, 0xC0262D);
		rgb(img, 7, 13, 0xC0262D);
		rgb(img, 9, 14, 0x7A1418);
		rgb(img, 7, 14, 0x7A1418);
		return img;
	}

	static BufferedImage assassinSeal() {
		BufferedImage img = image();
		shape(img, (x, y) -> {
			double dx = x - 7.5;
			double dy = y - 7.5;
			return Math.hypot(dx, dy) <= 6.4 + 0.7 * Math.sin(5 * Math.atan2(dy, dx));
		}, 0x3A0408, lit(7.5, 7.5, 6.0, 0x7A1018, 0xB01E28, 0xD8505A));
		// insignia: a hooded "A" with a centre spike, embossed
		int[][] mark = {{7, 3}, {8, 3}, {6, 4}, {9, 4}, {6, 5}, {9, 5}, {5, 6}, {10, 6}, {5, 7}, {10, 7}, {4, 8}, {11, 8}, {4, 9}, {11, 9}, {3, 10}, {12, 10},
			{7, 7}, {8, 7}, {7, 8}, {8, 8}, {7, 9}, {8, 9}};
		for (int[] p : mark) {
			rgb(img, p[0] + 1, p[1] + 1, 0x5A0A10);
		}
		for (int[] p : mark) {
			rgb(img, p[0], p[1], 0xF4C0C4);
		}
		return img;
	}

	static BufferedImage monarchsShadow() {
		BufferedImage img = image();
		Inside flame = (x, y) -> Math.hypot(x - 7.5, y - 10.0) <= 5.3
			|| (y >= 0 && y < 10 && Math.abs(x - 7.5 - 1.1 * Math.sin(y * 0.8)) <= 5.3 * (y + 0.2) / 10.0);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				if (!in(flame, x, y)) {
					continue;
				}
				boolean edge = !in(flame, x - 1, y) || !in(flame, x + 1, y) || !in(flame, x, y - 1) || !in(flame, x, y + 1);
				boolean nearEdge = !in(flame, x - 2, y) || !in(flame, x + 2, y) || !in(flame, x, y - 2);
				rgb(img, x, y, edge ? 0x7A4AC8 : nearEdge ? 0x3A1E5A : 0x140A22);
			}
		}
		// glowing eyes
		rgb(img, 5, 9, 0x9FF4FF);
		rgb(img, 6, 9, 0x3AA8E8);
		rgb(img, 9, 9, 0x3AA8E8);
		rgb(img, 10, 9, 0x9FF4FF);
		rgb(img, 7, 1, 0xB080F0);
		return img;
	}

	// ---------------------------------------------------------------- mage tokens

	static BufferedImage arcaneDust() {
		BufferedImage img = image();
		Inside pile = (x, y) -> y >= 8 && y <= 14 && Math.pow((x - 7.5) / 6.8, 2) + Math.pow((y - 14.5) / 6.0, 2) <= 1.0;
		shape(img, pile, 0x12305A, (x, y) -> {
			int h = hash(x, y, 7) % 7;
			return h == 0 ? 0xE0F4FF : x + (14 - y) * 1.2 < 9 ? 0x9FD0FF : x > 9 ? 0x2A5AA8 : 0x4A8AE8;
		});
		sparkle(img, 4, 4, 0xFFFFFF, 0x6FB0F0);
		sparkle(img, 11, 2, 0xFFFFFF, 0x6FB0F0);
		rgb(img, 8, 5, 0xBFE6FF);
		rgb(img, 13, 6, 0xBFE6FF);
		rgb(img, 2, 7, 0x9FD0FF);
		return img;
	}

	static BufferedImage grimoirePage() {
		BufferedImage img = image();
		Inside page = (x, y) -> x >= 3 && x <= 12 && y >= 1 && y <= 14 && x + y <= 24 && !(x == 3 && hash(0, y, 3) % 3 == 0);
		shape(img, page, 0x5A4320, (x, y) -> x >= 11 || y >= 13 ? 0xCDB888 : 0xE8D9B0);
		// folded corner
		rgb(img, 11, 13, 0xB8A070);
		rgb(img, 10, 13, 0xB8A070);
		rgb(img, 11, 12, 0xB8A070);
		// rune circle
		for (int y = 3; y <= 12; y++) {
			for (int x = 4; x <= 11; x++) {
				double d = Math.hypot(x - 7.5, y - 7.5);
				if (Math.abs(d - 3.3) < 0.55) {
					rgb(img, x, y, 0x7A3AC8);
				}
			}
		}
		rgb(img, 7, 6, 0xB070F0);
		rgb(img, 8, 7, 0xB070F0);
		rgb(img, 7, 8, 0xB070F0);
		rgb(img, 8, 9, 0xB070F0);
		for (int x = 5; x <= 10; x++) {
			if (x != 7) {
				rgb(img, x, 12, 0x8A7550);
			}
		}
		return img;
	}

	static BufferedImage emberCore() {
		BufferedImage img = image();
		shape(img, (x, y) -> Math.hypot(x - 7.5, y - 9.0) <= 5.6, 0x3A0E04, (x, y) -> {
			double d = Math.hypot(x - 6.8, y - 8.3);
			return d < 1.5 ? 0xFFF4C0 : d < 2.8 ? 0xFFC040 : d < 4.0 ? 0xF07A1A : 0xA8340C;
		});
		// flame licks
		int[][] flame = {{7, 3}, {8, 2}, {8, 3}, {6, 2}, {10, 3}, {5, 3}, {9, 1}};
		for (int[] p : flame) {
			rgb(img, p[0], p[1], p[1] <= 2 ? 0xFFD060 : 0xF07A1A);
		}
		rgb(img, 3, 6, 0xF07A1A);
		rgb(img, 12, 5, 0xFFC040);
		return img;
	}

	static BufferedImage akashicFragment() {
		BufferedImage img = image();
		double angle = Math.toRadians(30);
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		Inside crystal = (x, y) -> {
			double u = (x - 7.5) * cos - (y - 7.5) * sin;
			double v = (x - 7.5) * sin + (y - 7.5) * cos;
			return Math.abs(u) <= 2.9 && Math.abs(v) + Math.abs(u) * 1.1 <= 7.2;
		};
		shape(img, crystal, 0x0E3A4A, (x, y) -> {
			double u = (x - 7.5) * cos - (y - 7.5) * sin;
			double v = (x - 7.5) * sin + (y - 7.5) * cos;
			if (Math.abs(u) < 0.55) {
				return 0xFFFFFF;
			}
			if (v > 3.0) {
				return 0x2A8AA8;
			}
			return u < 0 ? 0xBFF8FF : 0x5ED0E8;
		});
		sparkle(img, 2, 3, 0xFFFFFF, 0x9FF4FF);
		sparkle(img, 13, 12, 0xFFFFFF, 0x9FF4FF);
		rgb(img, 13, 3, 0xE8C45A);
		rgb(img, 3, 12, 0xE8C45A);
		return img;
	}

	// ---------------------------------------------------------------- archer tokens

	static final String[] ARROWHEAD = {
		"................",
		".......oo.......",
		"......ohdo......",
		"......ohdo......",
		".....ohmddo.....",
		".....ohmddo.....",
		"....ohhmdddo....",
		"....ohmmdddo....",
		"...ohhmmddddo...",
		"...ohmmmddddo...",
		"..ohhmmmdddddo..",
		"..ooooomdooooo..",
		"......omdo......",
		"......omdo......",
		"......oBBo......",
		".......BB.......",
	};

	/** Diagonal feather: a = position along the quill, k = signed distance from it. */
	static BufferedImage plume() {
		BufferedImage img = image();
		Inside vane = (x, y) -> {
			int a = x - y;
			int k = x + y - 15;
			if (a < -8 || a > 14) {
				return false;
			}
			double width = 5.2 * Math.sin(Math.PI * (a + 9) / 24.0);
			boolean notch = Math.floorMod(a + 1, 6) == 0 && Math.abs(k) > 2;
			return Math.abs(k) <= width && !notch;
		};
		shape(img, vane, 0x2E3A4A, (x, y) -> {
			int k = x + y - 15;
			return k == 0 ? 0xE8F4F6 : k < 0 ? 0xB9D3D8 : 0x7FA6B0;
		});
		for (int x = 1; x <= 3; x++) {
			rgb(img, x, 15 - x, 0x4A5A6A);
		}
		rgb(img, 11, 3, 0x9FF4FF);
		return img;
	}

	static BufferedImage spiritArrow() {
		BufferedImage img = image();
		// shaft
		for (int i = 3; i <= 11; i++) {
			rgb(img, i, 15 - i, 0xFFF0A0);
			rgb(img, i + 1, 15 - i, 0xB8902E);
		}
		// head
		int[][] head = {{11, 3}, {12, 3}, {13, 3}, {12, 2}, {13, 2}, {13, 1}, {14, 1}, {12, 4}, {14, 2}};
		for (int[] p : head) {
			rgb(img, p[0], p[1], 0xBFF8FF);
		}
		rgb(img, 13, 2, 0xFFFFFF);
		rgb(img, 14, 1, 0xFFFFFF);
		// spirit fletching
		int[][] feathers = {{1, 11}, {2, 11}, {1, 12}, {2, 13}, {3, 13}, {3, 14}, {4, 14}, {2, 12}};
		for (int[] p : feathers) {
			rgb(img, p[0], p[1], (p[0] + p[1]) % 2 == 0 ? 0x6FE0FF : 0x3AA8E8);
		}
		rgb(img, 5, 7, 0x9FF4FF);
		rgb(img, 9, 4, 0x9FF4FF);
		rgb(img, 11, 9, 0x9FF4FF);
		return img;
	}

	static BufferedImage goldenKey() {
		BufferedImage img = image();
		shape(img, (x, y) -> {
			double d = Math.hypot(x - 4.5, y - 4.5);
			return d <= 4.0 && d >= 1.4;
		}, 0x6E4F05, lit(4.5, 4.5, 3.5, 0xB8860B, 0xE0B030, 0xFFE680));
		// shaft
		for (int i = 7; i <= 13; i++) {
			rgb(img, i, i, 0xFFE680);
			rgb(img, i + 1, i, 0xB8860B);
			rgb(img, i, i + 1, 0x6E4F05);
		}
		rgb(img, 14, 14, 0x6E4F05);
		// teeth
		int[][] teeth = {{9, 12}, {8, 13}, {12, 14}, {11, 15}, {10, 12}};
		for (int[] p : teeth) {
			rgb(img, p[0], p[1], 0xE0B030);
		}
		rgb(img, 8, 14, 0x6E4F05);
		rgb(img, 10, 15, 0x6E4F05);
		rgb(img, 2, 2, 0xFFFFFF);
		return img;
	}

	// ---------------------------------------------------------------- pirate tokens

	static BufferedImage mapScrap() {
		BufferedImage img = image();
		Inside paper = (x, y) -> x >= 1 && x <= 14 && y >= 3 && y <= 12
			&& !((y == 3 || y == 12) && hash(x, y, 11) % 3 == 0)
			&& !((x == 1 || x == 14) && hash(x, y, 12) % 3 == 0);
		shape(img, paper, 0x6A4E24, (x, y) -> x + y > 18 ? 0xC8AE78 : 0xE6D2A0);
		int[][] path = {{3, 10}, {5, 9}, {7, 9}, {8, 7}, {9, 6}};
		for (int[] p : path) {
			rgb(img, p[0], p[1], 0x7A5A30);
		}
		int[][] x = {{10, 4}, {12, 4}, {11, 5}, {10, 6}, {12, 6}};
		for (int[] p : x) {
			rgb(img, p[0], p[1], 0xC0262D);
		}
		rgb(img, 3, 5, 0x4A7AA8);
		rgb(img, 4, 5, 0x4A7AA8);
		rgb(img, 4, 6, 0x4A7AA8);
		rgb(img, 5, 6, 0x4A7AA8);
		return img;
	}

	static BufferedImage bountyPoster() {
		BufferedImage img = image();
		shape(img, (x, y) -> x >= 2 && x <= 13 && y >= 1 && y <= 14, 0x5A4320, (x, y) -> x >= 12 || y >= 13 ? 0xCDB888 : 0xE8D8B0);
		// "WANTED"
		for (int x = 4; x <= 11; x++) {
			if (x != 6 && x != 9) {
				rgb(img, x, 3, 0x3A2A1A);
			}
			if (x % 2 == 0) {
				rgb(img, x, 2, 0x3A2A1A);
			}
		}
		// portrait with a straw hat
		for (int y = 5; y <= 10; y++) {
			for (int x = 5; x <= 10; x++) {
				rgb(img, x, y, 0xBFA878);
			}
		}
		for (int x = 6; x <= 9; x++) {
			rgb(img, x, 5, 0xE8C45A);        // hat crown
			rgb(img, x, 6, 0xC0262D);        // red band
			rgb(img, x, 8, 0xE8B894);        // face
			rgb(img, x, 9, 0xE8B894);
		}
		for (int x = 5; x <= 10; x++) {
			rgb(img, x, 7, 0xD9A632);        // brim
			rgb(img, x, 10, 0xA8323A);       // red vest
		}
		rgb(img, 6, 8, 0x1E1A16);
		rgb(img, 9, 8, 0x1E1A16);
		rgb(img, 7, 9, 0xFFFFFF);            // grin
		rgb(img, 8, 9, 0xFFFFFF);
		rgb(img, 6, 9, 0x8A3A2A);            // scar under the eye
		// bounty
		for (int x : new int[] {4, 6, 7, 9, 10, 11}) {
			rgb(img, x, 12, 0x3A2A1A);
		}
		rgb(img, 3, 2, 0x8A8A8A);
		rgb(img, 12, 2, 0x8A8A8A);
		return img;
	}

	static BufferedImage hakiCrystal() {
		BufferedImage img = image();
		Inside crystal = (x, y) -> Math.abs(x - 7.5) <= Math.min(4.0, Math.min((y - 0.5) * 0.95, (15.5 - y) * 0.95));
		shape(img, crystal, 0x0A0204, (x, y) -> x == 7 || x == 8 ? 0x5A1424 : x < 7 ? 0x3A0E1A : 0x24080F);
		// red lightning
		int[][] bolt = {{2, 4}, {3, 5}, {4, 5}, {5, 6}, {6, 7}, {7, 7}, {8, 8}, {9, 9}, {10, 9}, {11, 10}, {12, 11}, {13, 12}};
		for (int[] p : bolt) {
			rgb(img, p[0], p[1], 0xFF2A3A);
		}
		rgb(img, 7, 7, 0xFF9090);
		rgb(img, 9, 9, 0xFF9090);
		rgb(img, 12, 3, 0xFF2A3A);
		rgb(img, 13, 2, 0xFF2A3A);
		rgb(img, 3, 12, 0xFF2A3A);
		return img;
	}

	static BufferedImage treasureChest() {
		BufferedImage img = image();
		// open lid behind the gold
		shape(img, (x, y) -> x >= 2 && x <= 13 && y >= 3 && y <= 7, 0x2A1606, (x, y) -> y == 4 ? 0x7A4A22 : 0x5A3416);
		// gold pile
		Random r = new Random(5);
		for (int y = 5; y <= 8; y++) {
			for (int x = 3; x <= 12; x++) {
				if (y > 5 || r.nextInt(3) > 0) {
					int roll = r.nextInt(5);
					rgb(img, x, y, roll == 0 ? 0xFFF0A0 : roll < 3 ? 0xE8C45A : 0xB8902E);
				}
			}
		}
		rgb(img, 5, 6, 0xD02030);
		rgb(img, 10, 5, 0x3A7AE8);
		rgb(img, 8, 6, 0x30C060);
		// chest body
		shape(img, (x, y) -> x >= 2 && x <= 13 && y >= 8 && y <= 14, 0x2A1606, (x, y) -> {
			if (x == 4 || x == 11) {
				return 0xC9A227; // gold bands
			}
			return y == 9 ? 0x9A6230 : x > 9 ? 0x5A3416 : 0x7A4A22;
		});
		rgb(img, 7, 10, 0xE8C45A);
		rgb(img, 8, 10, 0xE8C45A);
		rgb(img, 7, 11, 0x2A1A08);
		rgb(img, 8, 11, 0xC9A227);
		rgb(img, 1, 14, 0xE8C45A);
		rgb(img, 14, 13, 0xE8C45A);
		rgb(img, 14, 14, 0xB8902E);
		sparkle(img, 12, 1, 0xFFFFFF, 0xFFF0A0);
		return img;
	}

	/** Evolution Ether: a stoppered crystal phial with a swirling teal-gold glow. */
	static BufferedImage evolutionEther() {
		BufferedImage img = image();
		// glass body (round flask)
		shape(img, (x, y) -> Math.hypot(x - 7.5, y - 10.0) <= 5.2 || (y >= 3 && y <= 6 && x >= 6 && x <= 9), 0x1E3A4A, (x, y) -> {
			double d = Math.hypot(x - 7.5, y - 10.0);
			if (y <= 6) {
				return 0xBFE6F0;
			}
			if (d < 1.6) {
				return 0xFFF8D0;
			}
			double swirl = Math.sin(Math.atan2(y - 10.0, x - 7.5) * 2 + d * 1.3);
			return swirl > 0.3 ? 0x5EE0D0 : swirl > -0.4 ? 0x2FA8B8 : 0xE8C45A;
		});
		// cork
		for (int x = 6; x <= 9; x++) {
			rgb(img, x, 2, 0x8A5A2A);
			rgb(img, x, 1, x == 6 || x == 9 ? 0x5A3A1A : 0xA87440);
		}
		rgb(img, 5, 8, 0xFFFFFF);
		rgb(img, 5, 9, 0xE8FFFF);
		sparkle(img, 13, 4, 0xFFF8D0, 0x5EE0D0);
		sparkle(img, 2, 13, 0xFFF8D0, 0x5EE0D0);
		return img;
	}

	// ---------------------------------------------------------------- trainer skins

	static BufferedImage newSkin(final long seed) {
		TextureGen.skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		TextureGen.skinNoise = new Random(seed);
		return TextureGen.skin;
	}

	static void fill(final int x, final int y, final int w, final int h, final int rgb) {
		TextureGen.fill(x, y, w, h, rgb);
	}

	static void px(final int x, final int y, final int rgb) {
		TextureGen.px(x, y, rgb);
	}

	/** Head: skin on the sides and chin, hair on top. */
	static void head(final int skin, final int hair) {
		fill(8, 0, 8, 8, hair);
		fill(16, 0, 8, 8, skin);
		fill(0, 8, 32, 8, skin);
	}

	/** Hair cover: rows of hair on the front, sides and back of the head (counted from the top). */
	static void hair(final int hair, final int front, final int sides, final int back) {
		fill(8, 8, 8, front, hair);
		fill(0, 8, 8, sides, hair);
		fill(16, 8, 8, sides, hair);
		fill(24, 8, 8, back, hair);
	}

	static void eyes(final int y, final int white, final int iris) {
		px(9, y, white);
		px(10, y, iris);
		px(13, y, iris);
		px(14, y, white);
	}

	/** Body sides (all four) plus top and bottom. */
	static void body(final int rgb) {
		fill(20, 16, 8, 4, rgb);
		fill(28, 16, 8, 4, rgb);
		fill(16, 20, 24, 12, rgb);
	}

	static void arms(final int rgb) {
		fill(44, 16, 4, 4, rgb);
		fill(48, 16, 4, 4, rgb);
		fill(40, 20, 16, 12, rgb);
	}

	static void legs(final int rgb) {
		fill(4, 16, 4, 4, rgb);
		fill(8, 16, 4, 4, rgb);
		fill(0, 20, 16, 12, rgb);
	}

	/** Knight-Commander Bedivere: silver hair, plate armour, blue tabard, a gleaming silver arm. */
	static BufferedImage bedivere() {
		BufferedImage img = newSkin(101);
		int skin = 0xE8C3A0, hair = 0xC8CCD6, steel = 0xA8B0BC, darkSteel = 0x5E6670, blue = 0x2E4E9E, gold = 0xD9B44A;
		head(skin, hair);
		hair(hair, 2, 4, 7);
		px(8, 10, hair);
		px(15, 10, hair);
		px(9, 10, hair);
		eyes(12, 0xFFFFFF, 0x3A8A6E);
		px(11, 14, 0xC8987A);
		px(12, 14, 0xC8987A);

		body(steel);
		fill(20, 20, 8, 12, blue);           // tabard front
		fill(32, 20, 8, 12, blue);           // tabard back
		fill(20, 20, 8, 1, gold);
		fill(23, 22, 2, 5, gold);            // cross
		fill(22, 23, 4, 1, gold);
		fill(16, 29, 24, 1, 0x3A2414);       // belt
		px(23, 29, gold);
		px(24, 29, gold);

		arms(steel);
		fill(40, 20, 16, 3, darkSteel);      // pauldrons
		fill(40, 23, 16, 5, blue);           // sleeves
		fill(40, 28, 16, 4, 0xE4ECF6);       // silver gauntlet
		fill(48, 16, 4, 4, 0xE4ECF6);

		legs(0x2A3A6E);
		fill(0, 25, 16, 7, steel);           // greaves
		fill(0, 30, 16, 2, darkSteel);
		fill(8, 16, 4, 4, darkSteel);
		return img;
	}

	/** Hattori Hanzo: black hood and mask, red scarf, arm and shin wraps. */
	static BufferedImage hanzo() {
		BufferedImage img = newSkin(102);
		int black = 0x1E1E24, charcoal = 0x2A2C33, wrap = 0x5A5D66, red = 0xA52A2A, skin = 0xD9B08C;
		head(black, black);
		fill(0, 8, 32, 8, black);
		fill(9, 11, 6, 2, skin);             // eye slit
		eyes(11, 0xF0E8C8, 0x1A1A1A);
		px(9, 12, 0x9E7A5C);
		px(14, 12, 0x9E7A5C);
		fill(24, 13, 8, 3, red);             // knot of the hood
		px(27, 12, red);
		px(28, 12, red);

		body(charcoal);
		fill(16, 20, 24, 2, red);            // scarf
		fill(33, 20, 3, 8, red);             // scarf tail on the back
		fill(20, 22, 1, 7, 0x3C3F48);        // wrap seam
		fill(16, 28, 24, 2, 0x5B2A2A);       // sash
		px(25, 28, red);

		arms(charcoal);
		for (int y = 25; y <= 30; y += 2) {
			fill(40, y, 16, 1, wrap);
		}
		fill(40, 30, 16, 2, black);
		fill(48, 16, 4, 4, black);

		legs(charcoal);
		for (int y = 25; y <= 29; y += 2) {
			fill(0, y, 16, 1, wrap);
		}
		fill(0, 30, 16, 2, 0x141418);        // tabi
		fill(8, 16, 4, 4, 0x141418);
		return img;
	}

	/** Archmage Merlin: long white hair, a white and lavender robe with violet trim and flowers. */
	static BufferedImage merlin() {
		BufferedImage img = newSkin(103);
		int skin = 0xF2D5C0, hair = 0xF4F4F8, hairShade = 0xD8D0E8, robe = 0xEDEBF2, lavender = 0xB9A3DC, violet = 0x6B3FA0, pink = 0xF2A6C8;
		head(skin, hair);
		hair(hair, 2, 8, 8);
		fill(0, 14, 8, 2, hairShade);
		fill(16, 14, 8, 2, hairShade);
		px(8, 10, hair);
		px(15, 10, hair);
		px(12, 10, hair);
		eyes(12, 0xFFFFFF, 0x8E5AC8);
		px(11, 14, 0xE0A8A0);
		px(12, 14, 0xE0A8A0);
		px(10, 9, pink);                     // flower in the hair

		body(robe);
		fill(32, 20, 8, 7, hair);            // hair down the back
		fill(32, 27, 8, 1, hairShade);
		fill(23, 20, 2, 12, violet);         // trim
		fill(20, 20, 8, 1, lavender);
		px(23, 22, 0xE8C45A);
		px(24, 22, 0xE8C45A);
		fill(16, 29, 24, 1, lavender);
		px(21, 25, pink);
		px(26, 27, pink);

		arms(robe);
		fill(40, 26, 16, 3, lavender);       // wide cuffs
		fill(40, 28, 16, 1, violet);
		fill(40, 29, 16, 3, skin);
		fill(48, 16, 4, 4, skin);

		legs(robe);
		fill(0, 26, 16, 3, lavender);
		fill(0, 29, 16, 3, violet);
		fill(8, 16, 4, 4, violet);
		return img;
	}

	/** Chiron the Wise: brown hair and beard, green tunic, leather strap and quiver, bracers. */
	static BufferedImage chiron() {
		BufferedImage img = newSkin(104);
		int skin = 0xC99A72, hair = 0x4A3020, green = 0x3E7A3A, darkGreen = 0x2A5228, leather = 0x6A4A2A, darkLeather = 0x3A2A1C;
		head(skin, hair);
		hair(hair, 2, 6, 8);
		px(8, 10, hair);
		px(15, 10, hair);
		eyes(11, 0xFFFFFF, 0x3C8A4A);
		fill(9, 13, 6, 3, hair);             // beard
		fill(11, 13, 2, 1, 0x7A4A3A);
		fill(16, 0, 8, 8, hair);
		fill(0, 13, 2, 3, hair);
		fill(22, 13, 2, 3, hair);

		body(green);
		fill(32, 20, 8, 12, darkGreen);
		for (int i = 0; i < 8; i++) {
			px(20 + i, 20 + i, leather);     // strap across the chest
			px(20 + i, 21 + i, leather);
		}
		fill(34, 20, 4, 10, leather);        // quiver
		fill(34, 20, 4, 1, darkLeather);
		px(34, 19, 0xF0F0F0);
		px(36, 19, 0xC0262D);
		fill(16, 28, 24, 1, darkLeather);

		arms(skin);
		fill(40, 20, 16, 3, green);
		fill(44, 16, 4, 4, green);
		fill(40, 27, 16, 4, leather);        // bracers
		fill(40, 27, 16, 1, darkLeather);

		legs(0x5A4630);
		fill(0, 25, 16, 7, darkLeather);
		fill(0, 25, 16, 1, leather);
		fill(8, 16, 4, 4, darkLeather);
		return img;
	}

	/** Captain Drake: rose-red hair, a scar, red coat with gold trim and a black tricorn. */
	static BufferedImage drake() {
		BufferedImage img = newSkin(105);
		int skin = 0xE8B894, hair = 0xC8505A, red = 0x9E1E28, gold = 0xD9B44A, black = 0x1E1A1A;
		head(skin, hair);
		hair(hair, 1, 7, 8);
		px(8, 9, hair);
		px(15, 9, hair);
		px(9, 9, hair);
		eyes(11, 0xFFFFFF, 0x4A8AC8);
		px(14, 12, 0xF4D8C0);                // scar
		px(13, 13, 0xF4D8C0);
		px(11, 14, 0xB8605A);
		px(12, 14, 0xB8605A);

		// tricorn on the hat layer
		fill(40, 0, 8, 8, black);
		fill(32, 8, 32, 3, black);
		fill(32, 10, 32, 1, gold);
		px(43, 8, 0xF0F0F0);                 // skull badge
		px(44, 8, 0xF0F0F0);

		body(red);
		fill(22, 20, 4, 9, 0xEDE6D6);        // shirt
		fill(21, 20, 1, 12, gold);           // coat trim
		fill(26, 20, 1, 12, gold);
		fill(16, 28, 24, 1, black);          // belt
		px(23, 28, gold);
		px(24, 28, gold);
		fill(32, 20, 8, 4, hair);            // hair over the back

		arms(red);
		fill(40, 27, 16, 2, gold);           // cuffs
		fill(40, 29, 16, 3, skin);
		fill(48, 16, 4, 4, skin);

		legs(0x2C2430);
		fill(0, 24, 16, 8, 0x3A2418);        // boots
		fill(0, 24, 16, 1, 0x5A3A26);
		fill(8, 16, 4, 4, 0x3A2418);
		return img;
	}

	/** Master Smith Volund: bald, a great red beard, leather apron over a soot-grey shirt, heavy gloves. */
	static BufferedImage volund() {
		BufferedImage img = newSkin(106);
		int skin = 0xC08A64, beard = 0xA8442A, apron = 0x6B4A2B, shirt = 0x4E4E56, glove = 0x3A2A1C, soot = 0x2A2A2E;
		head(skin, skin);
		fill(8, 0, 8, 8, shade(skin, 1.08F));
		eyes(11, 0xFFFFFF, 0x3A5A8A);
		fill(9, 10, 6, 1, beard);
		fill(9, 13, 6, 3, beard);
		fill(8, 12, 8, 1, beard);
		fill(0, 13, 8, 3, beard);
		fill(16, 13, 8, 3, beard);
		px(11, 13, 0x7A2A1A);
		px(12, 13, 0x7A2A1A);
		fill(16, 0, 8, 8, beard);

		body(shirt);
		fill(20, 20, 8, 12, apron);
		fill(20, 20, 8, 1, 0x4A3220);
		fill(32, 20, 8, 1, 0x4A3220);
		px(21, 25, 0x9A7A4A);
		px(26, 25, 0x9A7A4A);
		fill(16, 29, 24, 1, 0x2A1A10);

		arms(skin);
		fill(40, 20, 16, 3, shirt);
		fill(44, 16, 4, 4, shirt);
		fill(40, 23, 16, 3, soot);
		fill(40, 26, 16, 6, glove);
		fill(48, 16, 4, 4, glove);

		legs(0x3A3A40);
		fill(0, 26, 16, 6, 0x2A1E14);
		fill(8, 16, 4, 4, 0x2A1E14);
		return img;
	}

	/** Raid Marshal Aldric: black hair, a scar, dark plate with a crimson tabard and cape. */
	static BufferedImage aldric() {
		BufferedImage img = newSkin(107);
		int skin = 0xD8A888, hair = 0x1E1A1A, steel = 0x4A4E58, light = 0x8A92A0, crimson = 0x8C1E28, gold = 0xD9B44A;
		head(skin, hair);
		hair(hair, 2, 4, 7);
		eyes(12, 0xFFFFFF, 0x6A4A2A);
		px(9, 11, 0xF4D8C0);
		px(10, 13, 0xF4D8C0);
		px(11, 14, 0x9A6A5A);
		px(12, 14, 0x9A6A5A);

		body(steel);
		fill(21, 20, 6, 12, crimson);
		fill(21, 20, 6, 1, gold);
		px(23, 23, gold);
		px(24, 23, gold);
		px(23, 24, gold);
		px(24, 24, gold);
		fill(32, 20, 8, 12, crimson);
		fill(16, 29, 24, 1, 0x2A1A10);
		px(23, 29, gold);

		arms(steel);
		fill(40, 20, 16, 3, light);
		fill(44, 16, 4, 4, light);
		fill(40, 28, 16, 4, 0x2E3038);

		legs(steel);
		fill(0, 24, 16, 2, light);
		fill(0, 29, 16, 3, 0x2E3038);
		fill(8, 16, 4, 4, 0x2E3038);
		return img;
	}

	static int shade(final int rgb, final float factor) {
		return TextureGen.shade(rgb, factor);
	}

	private QuestArt() {
	}
}
