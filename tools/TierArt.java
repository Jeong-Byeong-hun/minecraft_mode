import java.io.IOException;

/**
 * Tier lair art for TextureGen (0.5.49): the evolution shard and core, and the imbue scrolls (one per slot, a rune in the slot's colour;
 * greater scrolls have a gold border and seal).
 */
final class TierArt {
	/** Slot ids in ImbueSlot order and the rune colour of each (matching the stat it adds). */
	private static final String[] SLOTS = {"helmet", "chest", "legs", "boots", "necklace", "earring", "ring", "bracelet", "talisman"};
	private static final int[] COLORS = {0xE04A4A, 0xE0902A, 0x3AB0E0, 0x6AD04A, 0xB04AE0, 0xF0D040, 0xE06AA8, 0xF07A2A, 0x6A8AF0};

	private TierArt() {
	}

	static void writeAll() throws IOException {
		TextureGen.write("item/evolution_shard", QuestArt.grid(new String[] {
			"................",
			"........o.......",
			".......olo......",
			"......olhlo.....",
			"......olhmo.....",
			".....olhmmdo....",
			".....olhmmdo....",
			"....olhmmmddo...",
			"....olhmmmddo...",
			"....olmmmmddo...",
			".....olmmmdo....",
			".....olmmddo....",
			"......olmdo.....",
			"......oldo......",
			".......oo.......",
			"................"
		}, "o2A1A4A d6A3AB0 mA060E0 lD0A0F8 hFFF0FF"));
		TextureGen.write("item/evolution_core", QuestArt.grid(new String[] {
			"................",
			".....oooooo.....",
			"....ollllllo....",
			"...ollhhhhllo...",
			"..ollhggggllmo..",
			"..olhgwwwwglmo..",
			"..olhgwyywgmmo..",
			"..olhgwyywgmmo..",
			"..olhgwwwwgmmo..",
			"..ollgggggmmmo..",
			"..olllmmmmmmdo..",
			"...olmmmmmmdo...",
			"....oddddddo....",
			".....oooooo.....",
			"................",
			"................"
		}, "o2A123A d5A2A8A m8A4AD0 lB070F0 hE0C0FF gF0A0FF wFFE0FF yFFFFA0"));
		for (int i = 0; i < SLOTS.length; i++) {
			TextureGen.write("item/imbue_scroll_" + SLOTS[i], scroll(COLORS[i], false));
			TextureGen.write("item/greater_imbue_scroll_" + SLOTS[i], scroll(COLORS[i], true));
		}
	}

	/** A rolled scroll with a rune in {@code rune}'s colour; greater scrolls get a gold border and a red wax seal. */
	private static java.awt.image.BufferedImage scroll(final int rune, final boolean greater) {
		String palette = "o5A3A1A r8A5A2A pF0DCA8 sC8A86A g" + hex(greater ? 0xE8C24A : 0xB89A70) + " b" + hex(rune) + " w" + hex(lighter(rune))
			+ " x" + hex(greater ? 0xC0283A : 0xF0DCA8);
		return QuestArt.grid(new String[] {
			"................",
			".rrrrrrrrrrrrrr.",
			".orrrrrrrrrrrro.",
			"..oggggggggggo..",
			"..ogppppppppgo..",
			"..ogpppbbpppgo..",
			"..ogppbwwbppgo..",
			"..ogpbwbbwbpgo..",
			"..ogppbwwbppgo..",
			"..ogpppbbpppgo..",
			"..ogppppppxxgo..",
			"..ogssssssxxgo..",
			"..oggggggggggo..",
			".orrrrrrrrrrrro.",
			".rrrrrrrrrrrrrr.",
			"................"
		}, palette);
	}

	private static int lighter(final int rgb) {
		int r = Math.min(255, (rgb >> 16 & 0xFF) + 80);
		int g = Math.min(255, (rgb >> 8 & 0xFF) + 80);
		int b = Math.min(255, (rgb & 0xFF) + 80);
		return r << 16 | g << 8 | b;
	}

	private static String hex(final int rgb) {
		return String.format("%06X", rgb & 0xFFFFFF);
	}
}
