package com.minecraftmode.client.datagen.art;

import com.minecraftmode.job.gear.ArmorSetDef;
import com.minecraftmode.job.gear.ArmorStyle;
import com.minecraftmode.job.gear.GearSlot;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * Draws class armor at datagen time from a set's style (one look per class) and its three colors:
 * the 16x16 item icon of every piece and the worn textures (64x32 humanoid and leggings layers,
 * laid out on the vanilla armor model UVs). Higher tiers get crests, gems and highlights.
 *
 * <p>Icon grids use: {@code o} outline, {@code d m l w} primary shades (dark to white), {@code s S}
 * secondary (light, dark), {@code a A} accent (light, dark), {@code .} transparent.
 */
public final class ArmorArtist {
	// ---------------------------------------------------------------- icons

	private static final Map<ArmorStyle, String[][]> ICONS = new EnumMap<>(ArmorStyle.class);

	static {
		ICONS.put(ArmorStyle.PLATE, new String[][] {
			{
				"................",
				"......aaaa......",
				".....oAaaAo.....",
				"...oolllllloo...",
				"..olllmmmmmmdo..",
				"..olmmmmmmmmdo..",
				"..ossssssssssdo.",
				"..odoooooooodo..",
				"..olmmmmmmmmdo..",
				"..olmmmaammmdo..",
				"..olmmmmmmmmdo..",
				"...olmmmmmmdo...",
				"....osssssso....",
				".....oooooo.....",
				"................",
				"................",
			},
			{
				"................",
				".oooo......oooo.",
				"ollllo....ollldo",
				"olmmmlooooolmmdo",
				"oddmmlllllllmddo",
				".odomlmmmmmmdod.",
				"..olmmmaammmdo..",
				"..olmmmaammmdo..",
				"..olmmmmmmmmdo..",
				"..olmmmmmmmmdo..",
				"..ossssssssssso.",
				"..olmmmmmmmmdo..",
				"..olmmmmmmmmdo..",
				"..oddddddddddo..",
				"...oooooooooo...",
				"................",
			},
			{
				"................",
				"...oooooooooo...",
				"...ossssssssso..",
				"...olmmmmmmmdo..",
				"...olmmmoolmdo..",
				"...olmmo..olmdo.",
				"...olmmo..olmdo.",
				"...oaamo..oaado.",
				"...olmmo..olmdo.",
				"...olmmo..olmdo.",
				"...olmmo..olmdo.",
				"...oddo...oddo..",
				"...oooo...oooo..",
				"................",
				"................",
				"................",
			},
			{
				"................",
				"................",
				"................",
				"..oooo....oooo..",
				"..olmo....olmo..",
				"..olmo....olmo..",
				"..oaao....oaao..",
				"..olmo....olmo..",
				"..olmdo...olmdo.",
				"..olmmdo..olmmdo",
				".oolmmmdo.olmmmo",
				".osssssso.osssso",
				".oooooooo.oooooo",
				"................",
				"................",
				"................",
			},
		});
		ICONS.put(ArmorStyle.LEATHER, new String[][] {
			{
				"................",
				".....oooooo.....",
				"....ollllllo....",
				"...olmmmmmmdo...",
				"..olmmmmmmmmdo..",
				"..olmoooooomdo..",
				"..olo......odo..",
				"..ols.ssss.sdo..",
				"..olsssssssssdo.",
				"..olmsssssssmdo.",
				"...olmmmmmmdo...",
				"...ooddddddoo...",
				"....oaaaaaao....",
				".....oooooo.....",
				"................",
				"................",
			},
			{
				"................",
				"...ooo....ooo...",
				"..ollmo..olmdo..",
				".olmmsoooosmmdo.",
				".olmmmssssmmmdo.",
				".odomsmmmmsmodo.",
				"..olmmsmmsmmdo..",
				"..olmmmssmmmdo..",
				"..olmmsmmsmmdo..",
				"..olmsmmmmsmdo..",
				"..oSSSSaaSSSSo..",
				"..olmmmmmmmmdo..",
				"..olmmmmmmmmdo..",
				"..oddddddddddo..",
				"...oooooooooo...",
				"................",
			},
			{
				"................",
				"...oooooooooo...",
				"...oSSSaaSSSo...",
				"...olmmmmmmdo...",
				"...olmmoolmdo...",
				"...olmo..olmo...",
				"...olso..olso...",
				"...olmo..olmo...",
				"...olso..olso...",
				"...olmo..olmo...",
				"...olso..olso...",
				"...oddo..oddo...",
				"...oooo..oooo...",
				"................",
				"................",
				"................",
			},
			{
				"................",
				"................",
				"................",
				"..oooo....oooo..",
				"..olmo....olmo..",
				"..osso....osso..",
				"..olmo....olmo..",
				"..olmdo...olmdo.",
				"..olmmdo..olmmdo",
				".oolmmmdo.olmmmo",
				".odddddddoddddo.",
				".oooooooo.ooooo.",
				"................",
				"................",
				"................",
				"................",
			},
		});
		ICONS.put(ArmorStyle.ROBE, new String[][] {
			{
				"..........oo....",
				".........oao....",
				"........olmo....",
				".......olmdo....",
				"......olmmdo....",
				".....olmmmdo....",
				"....olmmmmdo....",
				"...olmmmmmmdo...",
				"...ossssaaSso...",
				".ooolmmmmmmdooo.",
				"olllmmmmmmmmmddo",
				"oddddddddddddddo",
				".oooooooooooooo.",
				"................",
				"................",
				"................",
			},
			{
				"................",
				"..ooo......ooo..",
				".ollmoooooolmdo.",
				"olmmmsssssssmmdo",
				"olmmmmssaasmmmdo",
				"odomlmmsssmmldod",
				".odolmmmsmmmlod.",
				"..oolmmmsmmmdo..",
				"..olmmmmsmmmmdo.",
				"..olmmmmsmmmmdo.",
				".olmmmmmsmmmmmdo",
				".olmmmmmsmmmmmdo",
				"olmmmmmmsmmmmmmd",
				"osssssssssssssso",
				"oooooooooooooooo",
				"................",
			},
			{
				"................",
				"...oooooooooo...",
				"...osssaasssso..",
				"...olmmmmmmmdo..",
				"...olmmmmmmmdo..",
				"..olmmmmmmmmmdo.",
				"..olmmmmmmmmmdo.",
				"..olmmmoommmmdo.",
				".olmmmmo.olmmmdo",
				".olmmmo..olmmmdo",
				".osssso..ossssso",
				".oooooo..ooooooo",
				"................",
				"................",
				"................",
				"................",
			},
			{
				"................",
				"................",
				"................",
				"................",
				"................",
				"...ooo....ooo...",
				"..olmdo..olmdo..",
				"..olmmdo.olmmdo.",
				".olmmmmdoolmmmdo",
				".oaammmdooaammdo",
				".osssssso.osssso",
				"..oooooo..oooooo",
				"................",
				"................",
				"................",
				"................",
			},
		});
		ICONS.put(ArmorStyle.RANGER, new String[][] {
			{
				"................",
				"...........a....",
				"..........aA....",
				".....oooooaA....",
				"....ollllaAo....",
				"...olmmmmmmdo...",
				"..olmmmmmmmmdo..",
				"..olmmmmmmmmdo..",
				"..ossssssssssdo.",
				"..oSSSSSSSSSSo..",
				".oolllllllllldoo",
				"oddddddddddddddo",
				".ooooooooooooooo",
				"................",
				"................",
				"................",
			},
			{
				"................",
				"...ooo....ooo...",
				"..ollmo..olmdo..",
				".olmmmoooosmmdo.",
				".olmmmmmmsmmmdo.",
				".odomlmmsmmmodo.",
				"..olmmmsmmmmdo..",
				"..olmmsmmmmmdo..",
				"..olmsmmmmmmdo..",
				"..olsmmmmmmmdo..",
				"..oSSSSaaSSSSo..",
				"..olmmmmmmmmdo..",
				"..olmmmmmmmmdo..",
				"..oddddddddddo..",
				"...oooooooooo...",
				"................",
			},
			{
				"................",
				"...oooooooooo...",
				"...oSSSaaSSSo...",
				"...olmmmmmmdo...",
				"...olmmoolmdo...",
				"...olmo..olmo...",
				"...osso..osso...",
				"...olmo..olmo...",
				"...olmo..olmo...",
				"...olmo..olmo...",
				"...olmo..olmo...",
				"...oddo..oddo...",
				"...oooo..oooo...",
				"................",
				"................",
				"................",
			},
			{
				"................",
				"..oooo....oooo..",
				"..ossso...ossso.",
				"..olmo....olmo..",
				"..olmo....olmo..",
				"..olmo....olmo..",
				"..oaao....oaao..",
				"..olmo....olmo..",
				"..olmdo...olmdo.",
				"..olmmdo..olmmdo",
				".oolmmmdo.olmmmo",
				".odddddddoddddo.",
				".oooooooo.ooooo.",
				"................",
				"................",
				"................",
			},
		});
		ICONS.put(ArmorStyle.COAT, new String[][] {
			{
				"................",
				"................",
				"................",
				"......oooo......",
				"....oollllo.....",
				"..oolmmmmmmoo...",
				".oslmmmaammmso..",
				"osslmmmaammmsso.",
				"osSlmmmmmmmmSso.",
				".oSoooooooooSo..",
				"..oo........oo..",
				"................",
				"................",
				"................",
				"................",
				"................",
			},
			{
				"................",
				"..ooo......ooo..",
				".ollmoooooolmdo.",
				"olmmssmmmmssmmdo",
				"olmmmsammmasmmdo",
				"odomlsmmmmsmldod",
				".odolsammmasdod.",
				"..oolsmmmmsmdo..",
				"..olmsammmasmdo.",
				"..olmsmmmmsmmdo.",
				"..oSSSSaaSSSSdo.",
				"..olmsmmmmsmmdo.",
				"..olmso..osmmdo.",
				"..olmso..osmmdo.",
				"..ooooo..ooooo..",
				"................",
			},
			{
				"................",
				"...oooooooooo...",
				"...oSSSaaSSSo...",
				"...olmmmmmmdo...",
				"...olmmoolmdo...",
				"...olmo..olmo...",
				"...olmo..olmo...",
				"...olmo..olmo...",
				"...osso..osso...",
				"...olmo..olmo...",
				"...oooo..oooo...",
				"................",
				"................",
				"................",
				"................",
				"................",
			},
			{
				"................",
				"..oooo....oooo..",
				"..ossso...ossso.",
				"..oSSSo...oSSSo.",
				"..odmo....odmo..",
				"..odmo....odmo..",
				"..odmo....odmo..",
				"..odmo....odmo..",
				"..odmdo...odmdo.",
				"..odmmdo..odmmdo",
				".oodmmmdo.odmmmo",
				".oaaaaaao.oaaaao",
				".oooooooo.oooooo",
				"................",
				"................",
				"................",
			},
		});
	}

	/** Item icons of the four pieces, keyed by slot. */
	public static Map<GearSlot, BufferedImage> icons(final ArmorSetDef set) {
		Colors c = new Colors(set);
		String[][] grids = ICONS.get(set.style());
		Map<GearSlot, BufferedImage> out = new EnumMap<>(GearSlot.class);
		for (GearSlot slot : GearSlot.ARMOR) {
			BufferedImage img = grid(grids[slot.armorIndex()], c, set.id() + "/" + slot.suffix());
			if (set.tier() >= 4) {
				sparkle(img, c);
			}
			out.put(slot, img);
		}
		return out;
	}

	private static BufferedImage grid(final String[] rows, final Colors c, final String what) {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		if (rows.length != 16) {
			throw new IllegalStateException(what + ": icon needs 16 rows, has " + rows.length);
		}
		for (int y = 0; y < 16; y++) {
			String row = rows[y];
			for (int x = 0; x < Math.min(16, row.length()); x++) {
				int rgb = c.of(row.charAt(x));
				if (rgb != -1) {
					img.setRGB(x, y, 0xFF000000 | rgb);
				}
			}
		}
		return img;
	}

	/** Tier 4: a few white glints on light pixels. */
	private static void sparkle(final BufferedImage img, final Colors c) {
		int placed = 0;
		for (int y = 1; y < 15 && placed < 3; y += 4) {
			for (int x = 2; x < 14; x++) {
				if ((img.getRGB(x, y) & 0xFFFFFF) == c.light) {
					img.setRGB(x, y, 0xFFFFFFFF);
					placed++;
					break;
				}
			}
		}
	}

	// ---------------------------------------------------------------- worn layers

	private static final int TOP = 0;
	private static final int BOTTOM = 1;
	private static final int FRONT = 3;
	private static final int BACK = 5;

	interface Face {
		/** face: 0 top, 1 bottom, 2 right, 3 front, 4 left, 5 back. Returns a palette char or '.'. */
		char at(int face, int x, int y, int w, int h);
	}

	/** Humanoid layer (helmet, chest + arms, boots) when {@code leggings} is false, else the leggings layer. */
	public static BufferedImage layer(final ArmorSetDef set, final boolean leggings) {
		Colors c = new Colors(set);
		BufferedImage img = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
		ArmorStyle style = set.style();
		boolean crest = set.tier() >= 3;
		if (!leggings) {
			box(img, c, 0, 0, 8, 8, 8, head(style, crest));
			box(img, c, 16, 16, 8, 12, 4, body(style));
			box(img, c, 40, 16, 4, 12, 4, arm(style));
			box(img, c, 0, 16, 4, 12, 4, boots(style));
		} else {
			box(img, c, 0, 16, 4, 12, 4, legs(style));
			box(img, c, 16, 16, 8, 12, 4, waist(style));
		}
		return img;
	}

	private static void box(final BufferedImage img, final Colors c, final int u, final int v, final int w, final int h, final int d, final Face face) {
		int[][] faces = {
			{u + d, v, w, d},
			{u + d + w, v, w, d},
			{u, v + d, d, h},
			{u + d, v + d, w, h},
			{u + d + w, v + d, d, h},
			{u + d + w + d, v + d, w, h},
		};
		for (int f = 0; f < 6; f++) {
			int fx = faces[f][0];
			int fy = faces[f][1];
			int fw = faces[f][2];
			int fh = faces[f][3];
			for (int y = 0; y < fh; y++) {
				for (int x = 0; x < fw; x++) {
					int rgb = c.of(face.at(f, x, y, fw, fh));
					if (rgb != -1 && fx + x < img.getWidth() && fy + y < img.getHeight()) {
						img.setRGB(fx + x, fy + y, 0xFF000000 | rgb);
					}
				}
			}
		}
	}

	/** Bevelled shading: light top-left edge, dark bottom-right edge, sparse light speckles. */
	private static char shade(final int x, final int y, final int w, final int h) {
		if (y == 0 || x == 0) {
			return 'l';
		}
		if (y == h - 1 || x == w - 1) {
			return 'd';
		}
		return (x * 3 + y * 5) % 11 == 0 ? 'l' : 'm';
	}

	private static Face head(final ArmorStyle style, final boolean crest) {
		return switch (style) {
			case PLATE -> (f, x, y, w, h) -> {
				if (f == BOTTOM) {
					return '.';
				}
				if (f == TOP) {
					return crest && (x == 3 || x == 4) ? 'a' : x == 3 || x == 4 ? 'w' : shade(x, y, w, h);
				}
				if (f == FRONT) {
					boolean brow = y <= 2;
					boolean cheek = (x == 0 || x == 7) && y <= 6;
					boolean nose = (x == 3 || x == 4) && y <= 4;
					if (y == 2 && !cheek && !nose) {
						return 's';
					}
					return brow || cheek || nose ? shade(x, y, w, h) : '.';
				}
				int depth = f == BACK ? 6 : 5;
				return y > depth ? '.' : y == 2 ? 's' : y == depth ? 'd' : shade(x, y, w, depth + 1);
			};
			case LEATHER -> (f, x, y, w, h) -> {
				// hood over the head and a mask over mouth and nose
				if (f == BOTTOM) {
					return '.';
				}
				if (f == FRONT) {
					if (y <= 1 || x == 0 || x == 7) {
						return y == 1 ? 'd' : shade(x, y, w, h);
					}
					if (y >= 5) {
						return y == 5 ? 'S' : 's';
					}
					return '.';
				}
				return y == h - 1 && f != TOP ? 'S' : shade(x, y, w, h);
			};
			case ROBE -> (f, x, y, w, h) -> {
				// pointed-hat look: crown on top, accent band and a wide brim on the sides
				if (f == BOTTOM) {
					return '.';
				}
				if (f == TOP) {
					int cx = Math.abs(x * 2 - 7);
					int cy = Math.abs(y * 2 - 7);
					return Math.max(cx, cy) <= 2 ? (crest ? 'a' : 'l') : shade(x, y, w, h);
				}
				if (y > 3) {
					return '.';
				}
				return y == 2 ? 'a' : y == 3 ? 'd' : shade(x, y, w, 3);
			};
			case RANGER -> (f, x, y, w, h) -> {
				// cap with a band and a feather on the left side
				if (f == BOTTOM) {
					return '.';
				}
				if (f == 4 && y <= 3 && (x == 2 || x == 3) && y + x >= 3) {
					return y <= 1 ? 'a' : 'A';
				}
				if (f == TOP) {
					return shade(x, y, w, h);
				}
				if (y > 3) {
					return '.';
				}
				return y == 2 ? 's' : y == 3 ? 'S' : shade(x, y, w, 3);
			};
			case COAT -> (f, x, y, w, h) -> {
				// tricorn: dark crown, gold edging, skull badge on the front
				if (f == BOTTOM) {
					return '.';
				}
				if (f == TOP) {
					return x == 0 || y == 0 || x == w - 1 || y == h - 1 ? 's' : 'd';
				}
				if (y > 3) {
					return '.';
				}
				if (f == FRONT && y == 1 && (x == 3 || x == 4)) {
					return 'w';
				}
				return y == 3 ? 's' : 'd';
			};
		};
	}

	private static Face body(final ArmorStyle style) {
		return switch (style) {
			case PLATE -> (f, x, y, w, h) -> {
				if (f == BOTTOM) {
					return '.';
				}
				if (f == FRONT) {
					if ((x == 3 || x == 4) && (y == 3 || y == 4)) {
						return 'a';
					}
					if (x == 3 || x == 4) {
						return x == 3 ? 'l' : 'm';
					}
				}
				return f != TOP && y == h - 2 ? 's' : shade(x, y, w, h);
			};
			case LEATHER -> (f, x, y, w, h) -> {
				if (f == BOTTOM) {
					return '.';
				}
				if (f == FRONT && (x == y - 1 || x == w - y)) {
					return 's';
				}
				if (f != TOP && (y == 9 || y == 10)) {
					return f == FRONT && (x == 3 || x == 4) ? 'a' : 'S';
				}
				return shade(x, y, w, h);
			};
			case ROBE -> (f, x, y, w, h) -> {
				if (f == BOTTOM) {
					return 'm';
				}
				if (f == FRONT) {
					if ((x == 3 || x == 4) && y == 2) {
						return 'a';
					}
					if (x == 3 || x == 4) {
						return 's';
					}
				}
				if (f != TOP && y == 0) {
					return 's';
				}
				return shade(x, y, w, h);
			};
			case RANGER -> (f, x, y, w, h) -> {
				if (f == BOTTOM) {
					return '.';
				}
				if (f == FRONT && x == w - 1 - (y * 7 / 11)) {
					return 's';
				}
				if (f == BACK && x >= 2 && x <= 4 && y <= 9) {
					return y <= 1 ? 'a' : 'S';
				}
				if (f != TOP && y == 9) {
					return f == FRONT && (x == 3 || x == 4) ? 'a' : 'S';
				}
				return shade(x, y, w, h);
			};
			case COAT -> (f, x, y, w, h) -> {
				if (f == BOTTOM) {
					return 'm';
				}
				if (f == FRONT) {
					if (x == 2 || x == 5) {
						return 's';
					}
					if (x == 3 || x == 4) {
						return y % 3 == 1 ? 'a' : 'w';
					}
				}
				if (f != TOP && y == 9) {
					return 'S';
				}
				return shade(x, y, w, h);
			};
		};
	}

	private static Face arm(final ArmorStyle style) {
		return switch (style) {
			case PLATE -> (f, x, y, w, h) -> {
				if (f == BOTTOM) {
					return '.';
				}
				if (f == TOP) {
					return shade(x, y, w, h);
				}
				if (y > 5 && y < 9) {
					return '.';
				}
				if (y >= 9) {
					return y == 9 ? 's' : shade(x, y - 9, w, 3);
				}
				return y == 5 ? 'd' : y == 4 ? 's' : shade(x, y, w, 6);
			};
			case LEATHER -> (f, x, y, w, h) -> f == BOTTOM ? '.' : f != TOP && y % 3 == 2 ? 'S' : shade(x, y, w, h);
			case ROBE -> (f, x, y, w, h) -> f == BOTTOM ? 'm' : f != TOP && y >= 9 ? (y == 9 ? 'a' : 's') : shade(x, y, w, h);
			case RANGER -> (f, x, y, w, h) -> {
				if (f == BOTTOM) {
					return '.';
				}
				if (f == TOP || y <= 3) {
					return shade(x, y, w, 4);
				}
				return y >= 7 ? (y == 7 ? 'S' : 's') : '.';
			};
			case COAT -> (f, x, y, w, h) -> f == BOTTOM ? 'm' : f != TOP && y >= 9 ? (y == 9 ? 'a' : 's') : shade(x, y, w, h);
		};
	}

	private static Face boots(final ArmorStyle style) {
		int top = switch (style) {
			case ROBE -> 9;
			case RANGER, COAT -> 5;
			default -> 7;
		};
		return (f, x, y, w, h) -> {
			if (f == TOP) {
				return '.';
			}
			if (f == BOTTOM) {
				return 'd';
			}
			if (y < top) {
				return '.';
			}
			if (y == top) {
				return style == ArmorStyle.COAT ? 'a' : 's';
			}
			return y == h - 1 ? 'o' : shade(x, y - top, w, h - top);
		};
	}

	private static Face legs(final ArmorStyle style) {
		return switch (style) {
			case PLATE -> (f, x, y, w, h) -> {
				if (f == BOTTOM) {
					return '.';
				}
				if (f == TOP) {
					return shade(x, y, w, h);
				}
				if (y > 8) {
					return '.';
				}
				if (f == FRONT && (y == 5 || y == 6)) {
					return y == 5 ? 'a' : 's';
				}
				return y == 8 ? 'd' : shade(x, y, w, 9);
			};
			case ROBE -> (f, x, y, w, h) -> f == TOP ? shade(x, y, w, h) : f == BOTTOM ? '.' : y >= h - 2 ? 's' : shade(x, y, w, h);
			default -> (f, x, y, w, h) -> {
				if (f == BOTTOM) {
					return '.';
				}
				if (f == TOP) {
					return shade(x, y, w, h);
				}
				if (y > 9) {
					return '.';
				}
				if (f == FRONT && (y == 5 || y == 6)) {
					return 's';
				}
				return y == 9 ? 'd' : shade(x, y, w, 10);
			};
		};
	}

	private static Face waist(final ArmorStyle style) {
		return (f, x, y, w, h) -> {
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
				return style == ArmorStyle.ROBE ? 's' : 'S';
			}
			return f == FRONT && (x == 3 || x == 4) && y == 10 ? 'a' : shade(x, y - 9, w, 3);
		};
	}

	// ---------------------------------------------------------------- colors

	private static final class Colors {
		final int outline;
		final int dark;
		final int mid;
		final int light;
		final int white;
		final int second;
		final int secondDark;
		final int accent;
		final int accentDark;

		Colors(final ArmorSetDef set) {
			this.mid = set.primary();
			this.outline = scale(set.primary(), 0.32F);
			this.dark = scale(set.primary(), 0.68F);
			this.light = lighten(set.primary(), 0.28F);
			this.white = lighten(set.primary(), 0.65F);
			this.second = set.secondary();
			this.secondDark = scale(set.secondary(), 0.62F);
			this.accent = set.accent();
			this.accentDark = scale(set.accent(), 0.65F);
		}

		int of(final char c) {
			return switch (c) {
				case 'o' -> this.outline;
				case 'd' -> this.dark;
				case 'm' -> this.mid;
				case 'l' -> this.light;
				case 'w' -> this.white;
				case 's' -> this.second;
				case 'S' -> this.secondDark;
				case 'a' -> this.accent;
				case 'A' -> this.accentDark;
				default -> -1;
			};
		}
	}

	private static int scale(final int rgb, final float f) {
		int r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * f));
		int g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * f));
		int b = Math.min(255, Math.round((rgb & 0xFF) * f));
		return r << 16 | g << 8 | b;
	}

	private static int lighten(final int rgb, final float t) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		return Math.round(r + (255 - r) * t) << 16 | Math.round(g + (255 - g) * t) << 8 | Math.round(b + (255 - b) * t);
	}

	private ArmorArtist() {
	}
}
