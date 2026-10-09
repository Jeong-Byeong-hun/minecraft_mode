import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

/**
 * Art for TextureGen after the endgame: the six herbs, the awakening crystal, the titan shard, the dungeon keystone, and the
 * kitchen, alchemy and smithing stations (top, side and bottom, tileable 16x16).
 */
final class ContentArt {
	static void writeAll() throws IOException {
		TextureGen.write("item/sunleaf", QuestArt.grid(new String[] {
			"................",
			"..........gg....",
			".........gLLg...",
			"........gLLLg...",
			".......gLLyLg...",
			"......gLLyLLg...",
			".....gLLyLLLg...",
			"....gLLyLLLg....",
			"...gLLyLLLg.....",
			"...gLyLLLg......",
			"..gLyLLgg.......",
			"..gyLgg.........",
			".sgg............",
			".s..............",
			"s...............",
			"................"
		}, "g2E7A2A L6AC850 yFFE070 s7A5A32"));
		TextureGen.write("item/moonpetal", flower("p6A5AA8 PC0B0F0 c9A8AD8 YFFFFE0 s3A6A3A"));
		TextureGen.write("item/emberbloom", flower("p8A1A10 PFF7A2A cE8501A YFFE070 s3A2A1A"));
		TextureGen.write("item/frostroot", QuestArt.grid(new String[] {
			"................",
			"......aaaa......",
			".....aAAAAa.....",
			"......aAAa......",
			".......ww.......",
			"......wWWw......",
			".....wWWWWw.....",
			"....wWWwWWWw....",
			"....wWwWWwWw....",
			".....wWWwWw.....",
			"......wWwWw.....",
			".....w.wWw.w....",
			"....w...w...w...",
			"...w....w....w..",
			"........w.......",
			"................"
		}, "a5A9AA8 AB8E8F0 w6A98C0 WE0F4FF"));
		TextureGen.write("item/glowcap", mushroom("o2A6A5A C4AD8B0 yE0FFF0 c2A8A70 sC8C0A8 SF0E8D0"));
		TextureGen.write("item/voidcap", mushroom("o1A0A2A C5A2A8A yE0B0FF c3A1A5A s2A2A3A S6A5A8A"));
		TextureGen.write("item/awakening_crystal", QuestArt.grid(new String[] {
			"................",
			".......oo.......",
			"......oWLo......",
			".....oWLLMo.....",
			".....oLLMMo.....",
			"....oWLLMMdo....",
			"....oLLLMMdo....",
			"...oWLLMMMddo...",
			"...oLLLMMMddo...",
			"...oLLMMMMddo...",
			"....oLMMMddo....",
			"....oLMMMddo....",
			".....oMMddo.....",
			"......oddo......",
			".......oo.......",
			"................"
		}, "o3A1A4A WFFFFFF LF0B0FF MC060FF d7A2AA0"));
		TextureGen.write("item/titan_shard", QuestArt.grid(new String[] {
			"................",
			"................",
			"......ooo.......",
			"....ooGggo......",
			"...oGGgggoo.....",
			"..oGggsggggo....",
			"..oggssyggggo...",
			".oGgsyysgggdo...",
			".oggsyyssggddo..",
			".ogggsssgggddo..",
			"..oggggsggdddo..",
			"..ooggggddddo...",
			"....oogdddoo....",
			"......oooo......",
			"................",
			"................"
		}, "o2A2420 GB0A890 g8A8270 sC8A040 yFFE07A d5A5248"));
		TextureGen.write("item/dungeon_keystone", QuestArt.grid(new String[] {
			"................",
			"......oooo......",
			"....ooaAAaoo....",
			"...oaAssssAao...",
			"..oaAsssgsssAo..",
			"..oAssggGggsAo..",
			".oaAsgGGGGgsAao.",
			".oAssgGWGGgssAo.",
			".oAssgGGGGgssAo.",
			".oaAsgGGGGgsAao.",
			"..oAssggGggsAo..",
			"..oaAsssgsssAo..",
			"...oaAssssAao...",
			"....ooaAAaoo....",
			"......oooo......",
			"................"
		}, "o1E2A24 a8A6A2A AD9B44A s4A5A50 g2E8A5A G5AE8A0 WE0FFF0"));

		TextureGen.write("block/kitchen_station_top", kitchenTop(901L));
		TextureGen.write("block/kitchen_station_side", kitchenSide(902L));
		TextureGen.write("block/kitchen_station_bottom", planks(0x8A6438, 903L));
		TextureGen.write("block/alchemy_station_top", alchemyTop(904L));
		TextureGen.write("block/alchemy_station_side", alchemySide(905L));
		TextureGen.write("block/alchemy_station_bottom", planks(0x5A3E5A, 906L));
		TextureGen.write("block/smithing_station_top", smithingTop(907L));
		TextureGen.write("block/smithing_station_side", smithingSide(908L));
		TextureGen.write("block/smithing_station_bottom", stone(0x6A6A70, 909L));
	}

	private static BufferedImage flower(final String palette) {
		return QuestArt.grid(new String[] {
			"................",
			"................",
			"......pp........",
			".....pPPp.pp....",
			".....pPPppPPp...",
			"..pp..pPPPPp....",
			".pPPp.pccccp....",
			".pPPPpcYYYcPp...",
			"..ppPcYYYYcPPp..",
			"....pcYYYcpPPp..",
			"...pPPccccPppp..",
			"..pPPp..s.pPPp..",
			"..ppp...s..pp...",
			"........s.......",
			".......s........",
			".......s........"
		}, palette);
	}

	private static BufferedImage mushroom(final String palette) {
		return QuestArt.grid(new String[] {
			"................",
			"................",
			".....oooooo.....",
			"....oCCyCCCo....",
			"...oCyCCCyCCo...",
			"..oCCCCyCCCCCo..",
			"..oCyCCCCCyCCo..",
			"..ooccccccccoo..",
			"......sSSs......",
			"......sSSs......",
			"......sSSs......",
			".....ssSSss.....",
			"....ssSSSSss....",
			"................",
			"................",
			"................"
		}, palette);
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

	private static void fill(final BufferedImage img, final int x0, final int y0, final int w, final int h, final int rgb) {
		for (int y = y0; y < y0 + h; y++) {
			for (int x = x0; x < x0 + w; x++) {
				px(img, x, y, rgb);
			}
		}
	}

	/** Horizontal planks with seams and grain. */
	static BufferedImage planks(final int wood, final long seed) {
		BufferedImage img = ClassArt.image(16);
		Random random = new Random(seed);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				px(img, x, y, shade(y % 4 == 3 ? shade(wood, 0.7F) : wood, 0.88F + random.nextFloat() * 0.24F));
			}
		}
		return img;
	}

	/** Cobbled stone with dark joints. */
	static BufferedImage stone(final int rock, final long seed) {
		BufferedImage img = ClassArt.image(16);
		Random random = new Random(seed);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				boolean joint = y % 8 == 7 || (x + (y / 8) * 4) % 8 == 7;
				px(img, x, y, shade(joint ? shade(rock, 0.6F) : rock, 0.85F + random.nextFloat() * 0.3F));
			}
		}
		return img;
	}

	/** A counter: planks around a cutting board with a knife and a small copper pot. */
	static BufferedImage kitchenTop(final long seed) {
		BufferedImage img = planks(0x9A7040, seed);
		fill(img, 2, 2, 7, 6, 0xD8B888);
		fill(img, 2, 7, 7, 1, 0xA88858);
		fill(img, 3, 3, 5, 1, 0xC0C4CC);
		px(img, 2, 3, 0x3A2A1A);
		fill(img, 9, 8, 6, 6, 0x9A5A2A);
		fill(img, 10, 9, 4, 4, 0x3A2416);
		fill(img, 11, 10, 2, 2, 0xE8A040);
		for (int i = 0; i < 16; i++) {
			px(img, i, 0, 0x5A3E22);
			px(img, 0, i, 0x5A3E22);
			px(img, i, 15, 0x4A3018);
			px(img, 15, i, 0x4A3018);
		}
		return img;
	}

	/** The front: planks with a stove opening glowing orange and two hanging pans. */
	static BufferedImage kitchenSide(final long seed) {
		BufferedImage img = planks(0x8A6438, seed);
		fill(img, 0, 0, 16, 2, 0xC8A070);
		fill(img, 4, 7, 8, 7, 0x2A2420);
		fill(img, 5, 10, 6, 3, 0xE85A1A);
		fill(img, 6, 11, 4, 2, 0xFFC040);
		fill(img, 4, 6, 8, 1, 0x5A5A60);
		fill(img, 1, 3, 2, 3, 0x5A5A60);
		fill(img, 13, 3, 2, 3, 0x5A5A60);
		return img;
	}

	/** A bench with a brewing ring and three bottles. */
	static BufferedImage alchemyTop(final long seed) {
		BufferedImage img = planks(0x6A4A6A, seed);
		for (int y = 3; y <= 12; y++) {
			for (int x = 3; x <= 12; x++) {
				double d = Math.hypot(x - 7.5, y - 7.5);
				if (d > 3.5 && d < 4.8) {
					px(img, x, y, 0xD9B44A);
				}
			}
		}
		fill(img, 6, 6, 4, 4, 0x3A2A3A);
		fill(img, 7, 7, 2, 2, 0x5AE8A0);
		fill(img, 1, 1, 2, 2, 0x4A8CFF);
		fill(img, 13, 1, 2, 2, 0xE84A6A);
		fill(img, 13, 13, 2, 2, 0x6AD860);
		return img;
	}

	/** Shelves of coloured bottles. */
	static BufferedImage alchemySide(final long seed) {
		BufferedImage img = planks(0x5A3E5A, seed);
		int[] colors = {0x4A8CFF, 0xE84A6A, 0x6AD860, 0xE8C040, 0xC060FF};
		Random random = new Random(seed);
		for (int shelf : new int[] {6, 13}) {
			fill(img, 0, shelf, 16, 1, 0x3A2414);
			for (int x = 1; x < 15; x += 3) {
				int c = colors[random.nextInt(colors.length)];
				fill(img, x, shelf - 3, 2, 3, c);
				px(img, x, shelf - 3, shade(c, 1.3F));
				px(img, x + 1, shelf - 4, 0xA0703C);
			}
		}
		return img;
	}

	/** An iron working surface with a hammer and tongs. */
	static BufferedImage smithingTop(final long seed) {
		BufferedImage img = stone(0x5A5A62, seed);
		fill(img, 2, 2, 12, 12, 0x6E7078);
		fill(img, 3, 3, 10, 10, 0x8A8E98);
		fill(img, 4, 5, 7, 2, 0x5A3E22);
		fill(img, 10, 4, 3, 4, 0x3A3A40);
		fill(img, 5, 10, 6, 1, 0x3A3A40);
		px(img, 4, 11, 0x3A3A40);
		px(img, 11, 9, 0x3A3A40);
		return img;
	}

	/** Stone with a glowing forge mouth. */
	static BufferedImage smithingSide(final long seed) {
		BufferedImage img = stone(0x6A6A70, seed);
		fill(img, 0, 0, 16, 2, 0x8A8E98);
		fill(img, 4, 7, 8, 6, 0x2A2420);
		fill(img, 5, 9, 6, 4, 0xE85A1A);
		fill(img, 6, 10, 4, 2, 0xFFD040);
		fill(img, 3, 6, 10, 1, 0x3A3A40);
		return img;
	}

	private ContentArt() {
	}
}
