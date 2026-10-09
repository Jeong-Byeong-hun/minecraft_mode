import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

/**
 * Endgame art for TextureGen: enhancement stone, protection scroll, lair map, and the personal lair chest and cache blocks
 * (side and top, tileable 16x16).
 */
final class EndgameArt {
	static void writeAll() throws IOException {
		TextureGen.write("item/enhancement_stone", QuestArt.grid(new String[] {
			"................",
			"......oooo......",
			".....olllho.....",
			"....ollhlmmo....",
			"...olllmmmmdo...",
			"..ollmmgmmmddo..",
			"..olmmgggmmddo..",
			".olmmmmgmmmmddo.",
			".olmmmmgmmmmddo.",
			".odmmmgggmmdddo.",
			"..odmmmmmmdddo..",
			"..oddmmmmddddo..",
			"...oddddddddo...",
			"....oddddddo....",
			".....oooooo.....",
			"................"
		}, "o0E2A3A d1E5A7A m2E9AC0 l7AD8F0 hE0FAFF gFFE07A"));
		TextureGen.write("item/protection_scroll", QuestArt.grid(new String[] {
			"................",
			".rrrrrrrrrrrrrr.",
			".orrrrrrrrrrrro.",
			"..oppppppppppo..",
			"..opppggggpppo..",
			"..oppgbbbbgppo..",
			"..oppgbwwbgppo..",
			"..oppgbwwbgppo..",
			"..oppgbbbbgppo..",
			"..opppgbbgpppo..",
			"..oppppggppppo..",
			"..ospppppppspo..",
			"..osssssssssso..",
			".orrrrrrrrrrrro.",
			".rrrrrrrrrrrrrr.",
			"................"
		}, "o5A3A1A r8A5A2A pF0DCA8 sC8A86A b3A6ED8 gE8C24A wFFFFFF"));
		TextureGen.write("item/lair_map", QuestArt.grid(new String[] {
			"................",
			".oooooooooooooo.",
			".oppppppppbbppo.",
			".opggppppbbpppo.",
			".ogggppppbppppo.",
			".opgpppppbppppo.",
			".oppppippbbpppo.",
			".opppippppbpppo.",
			".oppippppprprpo.",
			".opippppppprppo.",
			".oipppiiiprprpo.",
			".osppppppppspso.",
			".osssssssssssso.",
			".oooooooooooooo.",
			"................",
			"................"
		}, "o4A3218 pE8D4A0 sC8AE70 i5A4A3A rC0262D g5A8A3A b3A6AA8"));
		TextureGen.write("block/lair_chest_side", chestSide(0x2A1E2E, 0x3A2A3E, 0xD9B44A, 0xFFE07A, 777L));
		TextureGen.write("block/lair_chest_top", chestTop(0x2A1E2E, 0x3A2A3E, 0xD9B44A, 0xC0263A, 778L));
		TextureGen.write("block/lair_cache_side", cacheSide(779L));
		TextureGen.write("block/lair_cache_top", cacheTop(780L));
	}

	private static int shade(final int rgb, final float f) {
		int r = Math.min(255, Math.round((rgb >> 16 & 0xFF) * f));
		int g = Math.min(255, Math.round((rgb >> 8 & 0xFF) * f));
		int b = Math.min(255, Math.round((rgb & 0xFF) * f));
		return r << 16 | g << 8 | b;
	}

	private static void px(final BufferedImage img, final int x, final int y, final int rgb) {
		ClassArt.rgb(img, x, y, rgb);
	}

	/** Dark planks with grain, gold bands along the rim, gold corner posts and a gold lock with a glowing gem. */
	static BufferedImage chestSide(final int dark, final int wood, final int gold, final int gem, final long seed) {
		BufferedImage img = ClassArt.image(16);
		Random random = new Random(seed);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int base = y % 4 == 3 ? dark : wood;
				px(img, x, y, shade(base, 0.88F + random.nextFloat() * 0.24F));
			}
		}
		for (int x = 0; x < 16; x++) {
			px(img, x, 0, shade(gold, 0.8F));
			px(img, x, 1, gold);
			px(img, x, 14, gold);
			px(img, x, 15, shade(gold, 0.7F));
		}
		for (int y = 0; y < 16; y++) {
			px(img, 0, y, shade(gold, 0.75F));
			px(img, 15, y, shade(gold, 0.6F));
		}
		for (int y = 5; y <= 11; y++) {
			for (int x = 6; x <= 9; x++) {
				px(img, x, y, y == 5 || x == 6 ? shade(gold, 1.15F) : x == 9 || y == 11 ? shade(gold, 0.7F) : gold);
			}
		}
		px(img, 7, 6, gem);
		px(img, 8, 6, shade(gem, 0.8F));
		px(img, 7, 8, 0x1A1010);
		px(img, 8, 8, 0x1A1010);
		px(img, 7, 9, 0x1A1010);
		px(img, 7, 10, 0x1A1010);
		return img;
	}

	/** The lid: planks inside a gold frame with a red crest in the middle. */
	static BufferedImage chestTop(final int dark, final int wood, final int gold, final int crest, final long seed) {
		BufferedImage img = ClassArt.image(16);
		Random random = new Random(seed);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int base = x % 4 == 3 ? dark : wood;
				px(img, x, y, shade(base, 0.88F + random.nextFloat() * 0.24F));
			}
		}
		for (int i = 0; i < 16; i++) {
			px(img, i, 0, gold);
			px(img, i, 15, shade(gold, 0.7F));
			px(img, 0, i, shade(gold, 0.9F));
			px(img, 15, i, shade(gold, 0.7F));
			px(img, i, 1, shade(gold, 0.6F));
		}
		int[][] diamond = {{7, 5}, {8, 5}, {6, 6}, {7, 6}, {8, 6}, {9, 6}, {5, 7}, {6, 7}, {7, 7}, {8, 7}, {9, 7}, {10, 7}, {5, 8}, {6, 8}, {7, 8}, {8, 8}, {9, 8},
			{10, 8}, {6, 9}, {7, 9}, {8, 9}, {9, 9}, {7, 10}, {8, 10}};
		for (int[] p : diamond) {
			px(img, p[0], p[1], gold);
		}
		for (int[] p : new int[][] {{7, 6}, {8, 6}, {6, 7}, {7, 7}, {8, 7}, {9, 7}, {6, 8}, {7, 8}, {8, 8}, {9, 8}, {7, 9}, {8, 9}}) {
			px(img, p[0], p[1], p[1] < 8 ? shade(crest, 1.2F) : crest);
		}
		return img;
	}

	/** Supply crate: vertical boards, two iron straps with rivets. */
	static BufferedImage cacheSide(final long seed) {
		BufferedImage img = ClassArt.image(16);
		Random random = new Random(seed);
		int wood = 0x7A5A32;
		int dark = 0x5A3E22;
		int iron = 0x5E6068;
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int base = x % 5 == 4 ? dark : wood;
				px(img, x, y, shade(base, 0.88F + random.nextFloat() * 0.24F));
			}
		}
		for (int x = 0; x < 16; x++) {
			for (int y : new int[] {2, 3, 12, 13}) {
				px(img, x, y, y == 2 || y == 12 ? shade(iron, 1.2F) : iron);
			}
			px(img, x, 0, dark);
			px(img, x, 15, shade(dark, 0.8F));
		}
		for (int x : new int[] {2, 7, 13}) {
			px(img, x, 2, 0xB0B4BC);
			px(img, x, 12, 0xB0B4BC);
		}
		return img;
	}

	/** Crate lid: boards with a cross brace. */
	static BufferedImage cacheTop(final long seed) {
		BufferedImage img = ClassArt.image(16);
		Random random = new Random(seed);
		int wood = 0x7A5A32;
		int dark = 0x5A3E22;
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int base = y % 5 == 4 ? dark : wood;
				px(img, x, y, shade(base, 0.88F + random.nextFloat() * 0.24F));
			}
		}
		for (int i = 0; i < 16; i++) {
			px(img, i, i, shade(wood, 1.2F));
			px(img, 15 - i, i, shade(wood, 1.2F));
			px(img, i, 0, dark);
			px(img, 0, i, dark);
			px(img, i, 15, shade(dark, 0.8F));
			px(img, 15, i, shade(dark, 0.8F));
		}
		return img;
	}

	private EndgameArt() {
	}
}
