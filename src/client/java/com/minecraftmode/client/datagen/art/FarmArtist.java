package com.minecraftmode.client.datagen.art;

import com.minecraftmode.craft.Crops;
import com.minecraftmode.craft.Fishing;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Icons of the crops, their seeds and the fish (16x16 character grids, see {@link ConsumableArtist}), and the four growth stages of
 * every crop block, drawn from code: stalks and leaves in two greens, the crop's own colors on the last stage.
 */
public final class FarmArtist {
	private static final int LEAF = 0x4CA82A;
	private static final int DARK_LEAF = 0x2E7A1A;
	private static final Map<Crops.Shape, String[]> PRODUCE = new EnumMap<>(Crops.Shape.class);
	/** Fish drawn long and thin. */
	private static final Set<String> LONG_FISH = Set.of("eel", "loach");

	static {
		// a sack of rice (A sack, C grains)
		PRODUCE.put(Crops.Shape.GRAIN, new String[] {
			"................",
			"......o..o......",
			".....oLoCoo.....",
			"....oLCLCLCo....",
			"...oCLWLCLCCo...",
			"...oAoCLCCoAo...",
			"...oAAooooAAo...",
			"..oAAaAAAAaAAo..",
			"..oAAAaAAaAAAo..",
			"..oAAAAaaAAAAo..",
			"..oAAAAAAAAAao..",
			"..oAAAAAAAAaao..",
			"...oAAAAAAaao...",
			"...oaaaaaaaao...",
			"....oooooooo....",
			"................"});
		PRODUCE.put(Crops.Shape.LEAFY, new String[] {
			"................",
			"......oooo......",
			".....oAAAAo.....",
			"....oAALLAAo....",
			"...oAALLLLAAo...",
			"...oCALLLLACo...",
			"..oCCALLLLACCo..",
			"..oCLALLLLACCo..",
			"..oCCAALLAACCo..",
			"..oCLCAAAACCCo..",
			"..ocCCCAACCCco..",
			"...ocCCCCCCco...",
			"...occCCCCcco...",
			"....occcccco....",
			".....oooooo.....",
			"................"});
		PRODUCE.put(Crops.Shape.STALK, new String[] {
			"....o..o..o.....",
			"...oCo.oCo.oo...",
			"...oCooCCooCo...",
			"...oCCoCCoCCo...",
			"....oCCLCCLo....",
			"....oCLCCLCo....",
			".....oCLCCo.....",
			".....oCCCCo.....",
			".....oCCCco.....",
			".....oCCCco.....",
			".....oAAAAo.....",
			".....oAWAAo.....",
			".....oAWAAo.....",
			".....oAAAao.....",
			"......oAao......",
			"......o.o.o....."});
		PRODUCE.put(Crops.Shape.POD, new String[] {
			"................",
			"................",
			"............oo..",
			"..........ooAAo.",
			"........ooAALAo.",
			"......ooACCCAo..",
			"....ooAACLCAo...",
			"...oAACCCAAo....",
			"..oAACLCAAo.....",
			"..oACCCAAo......",
			"..oAAAAAo.......",
			"...oaaao........",
			"....ooo.........",
			"................",
			"................",
			"................"});
		PRODUCE.put(Crops.Shape.COB, new String[] {
			"................",
			".......oo.......",
			"......oLCo......",
			"......oCLCo.....",
			".....oLCcCo.....",
			".....oCLCco.....",
			"....AoLCcCoA....",
			"....AoCLCcoA....",
			"...AAoLCcCoAA...",
			"...AaoCLCcoaA...",
			"...aAAoCcoAAa...",
			"....aAAooAAa....",
			".....aAAAAa.....",
			"......aAAa......",
			".......aa.......",
			"................"});
		PRODUCE.put(Crops.Shape.ROUND, new String[] {
			"................",
			"................",
			".......vV.......",
			".....VvVVvV.....",
			"....ooVvvVoo....",
			"...oCCVVVVCCo...",
			"..oCLWLCCCCCco..",
			"..oCLLCCCCCCco..",
			"..oCLCCCCCCcco..",
			"..oCCCCCCCCcco..",
			"..oCCCCCCCccco..",
			"...oCCCCCccco...",
			"....occcccco....",
			".....oooooo.....",
			"................",
			"................"});
		PRODUCE.put(Crops.Shape.ROOT, new String[] {
			"................",
			".v..v...........",
			"..vVVv..........",
			".vVvVV.v........",
			"...VvVVV........",
			"....oooo........",
			"...oLLCCo.......",
			"...oLCCCCo......",
			"....oCCCCCo.....",
			"....oCLCCCco....",
			".....oCCCCcco...",
			"......oCCccco...",
			".......oCccco...",
			"........occco...",
			".........ooo....",
			"................"});
		PRODUCE.put(Crops.Shape.BULB, new String[] {
			"................",
			".......oo.......",
			".......oAo......",
			"......oLCo......",
			".....oLCCCo.....",
			"....oLCACCCo....",
			"...oLCCACCACo...",
			"..oLCCACCCACco..",
			"..oCCACCCCACco..",
			"..oCCACCCACCco..",
			"..oCCACCCACcco..",
			"...oCCACACcco...",
			"....occcccco....",
			".....oaaaao.....",
			"................",
			"................"});
		PRODUCE.put(Crops.Shape.PEPPER, new String[] {
			"................",
			"..........VV....",
			".........VvV....",
			"........ooVo....",
			".......oLCCo....",
			"......oLWCco....",
			".....oLLCCco....",
			".....oLCCcco....",
			"....oLCCcco.....",
			"....oCCCcco.....",
			"...oCCCcco......",
			"...oCCcco.......",
			"..oCCcco........",
			"..occoo.........",
			"...oo...........",
			"................"});
		PRODUCE.put(Crops.Shape.SEEDS, new String[] {
			"................",
			"................",
			"................",
			"................",
			"........oo......",
			"......ooLCoo....",
			"....ooCLCAcCoo..",
			"...oCAcLCcCLAco.",
			"..oCLcCAcCLcCco.",
			"..oCcLCcACcACco.",
			"..oACcCLcCcCaco.",
			"...oocCcAcCcoo..",
			".....ooooooo....",
			"................",
			"................",
			"................"});
		PRODUCE.put(Crops.Shape.BERRY, new String[] {
			"................",
			"......V..V......",
			"....VvVvvVvV....",
			"....oVvvvvVo....",
			"...oCCVvvVCCo...",
			"..oCLWCCCCYCco..",
			"..oCLCCYCCCCco..",
			"..oCYCCCCYCcco..",
			"...oCCYCCCcco...",
			"...oCCCCYCcco...",
			"....oCYCCcco....",
			".....oCCcco.....",
			"......occo......",
			".......oo.......",
			"................",
			"................"});
	}

	/** A seed packet: the crop's color in a window, its accent in a band. */
	private static final String[] PACKET = {
		"................",
		"...oooooooooo...",
		"...oPPPPPPPPo...",
		"...oppppppppo...",
		"...oppooooppo...",
		"...opoLCCCopo...",
		"...opoCLCcopo...",
		"...opoCCccopo...",
		"...oppooooppo...",
		"...oppppppppo...",
		"...opAAAAAApo...",
		"...oppppppppo...",
		"...opppppppPo...",
		"...oPPPPPPPPo...",
		"...oooooooooo...",
		"................"};

	private static final String[] FISH = {
		"................",
		"................",
		"................",
		"................",
		"......oooo......",
		"....ooCCCCoo..oo",
		"...oCCCAACCCooAo",
		"..oCxCCCCCCCCAAo",
		".oCCCCCCCCCCCAo.",
		".oLLCCCCCCCcCAo.",
		"..oLLLLLLLccAAAo",
		"...ooLLLLccooAAo",
		".....oooooo..oo.",
		"................",
		"................",
		"................"};

	private static final String[] LONG = {
		"................",
		"................",
		"................",
		"................",
		"................",
		"..oo............",
		".oCxo........oo.",
		".oCCCooo....oAAo",
		"..oCCCCCoooCCAo.",
		"...oLLCCCCCCcAo.",
		"....ooLLLLLcco..",
		"......oooooo....",
		"................",
		"................",
		"................",
		"................"};

	public static BufferedImage produce(final Crops.CropDef def) {
		return ConsumableArtist.paint(PRODUCE.get(def.shape()), 16, 0, def.color(), def.accent());
	}

	public static BufferedImage seed(final Crops.CropDef def) {
		return ConsumableArtist.paint(PACKET, 16, 0, def.color(), def.accent());
	}

	public static BufferedImage fish(final Fishing.FishDef def) {
		return ConsumableArtist.paint(LONG_FISH.contains(def.id()) ? LONG : FISH, 16, 0, def.color(), def.accent());
	}

	// ------------------------------------------------------------------ crop blocks

	private enum Growth {
		TALL, BUSH, LOW
	}

	private static Growth growth(final Crops.Shape shape) {
		return switch (shape) {
			case GRAIN, STALK, COB -> Growth.TALL;
			case ROOT, BULB -> Growth.LOW;
			default -> Growth.BUSH;
		};
	}

	/** Stage {@code stage} (0..3) of the crop block: five stalks with leaves, the ripe crop on the last stage. */
	public static BufferedImage stage(final Crops.CropDef def, final int stage) {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Growth growth = growth(def.shape());
		int[] heights = switch (growth) {
			case TALL -> new int[] {4, 8, 12, 15};
			case BUSH -> new int[] {3, 6, 9, 11};
			case LOW -> new int[] {3, 5, 7, 8};
		};
		int[] stalks = {2, 5, 8, 11, 13};
		boolean onion = def.shape() == Crops.Shape.STALK;
		for (int i = 0; i < stalks.length; i++) {
			int x = stalks[i];
			int h = heights[stage] - (i % 2 == 0 ? 0 : Math.min(2, heights[stage] / 4));
			for (int y = 15; y > 15 - h; y--) {
				int stem = onion && stage == 3 ? (y >= 13 ? def.accent() : def.color()) : DARK_LEAF;
				set(img, x, y, stem);
				// a leaf every third pixel, alternating sides
				if ((15 - y) % 3 == 1 && !onion) {
					set(img, x + ((y + i) % 2 == 0 ? 1 : -1), y, LEAF);
				}
			}
			if (growth == Growth.BUSH && stage >= 2) {
				int top = 15 - h + 1;
				set(img, x - 1, top + 1, LEAF);
				set(img, x + 1, top + 1, LEAF);
				set(img, x, top, LEAF);
			}
		}
		if (stage == 3) {
			ripe(img, def, growth, heights[3], stalks);
		}
		return img;
	}

	private static void ripe(final BufferedImage img, final Crops.CropDef def, final Growth growth, final int height, final int[] stalks) {
		int color = def.color();
		int dark = ConsumableArtist.shade(color, 0.72F);
		switch (def.shape()) {
			case GRAIN -> {
				for (int x : stalks) {
					for (int y = 16 - height; y < 19 - height; y++) {
						set(img, x, y, def.accent());
						set(img, x + (y % 2 == 0 ? 1 : -1), y + 1, color);
					}
				}
			}
			case COB -> {
				for (int x : new int[] {stalks[1], stalks[3]}) {
					for (int y = 6; y < 10; y++) {
						set(img, x + 1, y, y % 2 == 0 ? color : dark);
						set(img, x + 2, y, color);
					}
					set(img, x + 1, 10, def.accent());
				}
			}
			case STALK -> {
			}
			case LEAFY -> {
				for (int y = 9; y < 16; y++) {
					int half = Math.min(6, (y - 8) + 2);
					for (int x = 8 - half; x < 8 + half; x++) {
						boolean edge = x == 8 - half || x == 8 + half - 1;
						set(img, x, y, edge ? dark : Math.abs(x - 8) < 2 ? def.accent() : color);
					}
				}
			}
			case ROOT, BULB -> {
				for (int x : new int[] {stalks[0], stalks[2], stalks[4]}) {
					for (int dx = -1; dx <= 1; dx++) {
						set(img, x + dx, 14, color);
						set(img, x + dx, 15, dark);
					}
					set(img, x, 13, color);
				}
			}
			default -> {
				// fruit hanging among the leaves: tomatoes, peppers, pods, sesame, strawberries
				int bottom = 15;
				int top = 16 - height;
				for (int i = 0; i < stalks.length; i++) {
					int x = stalks[i] + (i % 2 == 0 ? 1 : -1);
					int y = top + 2 + (i * 3) % Math.max(1, bottom - top - 3);
					boolean tall = def.shape() == Crops.Shape.PEPPER || def.shape() == Crops.Shape.POD;
					set(img, x, y, color);
					set(img, x, y + 1, tall ? dark : color);
					if (!tall) {
						set(img, x + 1, y, color);
						set(img, x + 1, y + 1, dark);
					} else {
						set(img, x, y + 2, dark);
					}
				}
			}
		}
	}

	private static void set(final BufferedImage img, final int x, final int y, final int rgb) {
		if (x >= 0 && x < 16 && y >= 0 && y < 16) {
			img.setRGB(x, y, 0xFF000000 | rgb);
		}
	}

	private FarmArtist() {
	}
}
