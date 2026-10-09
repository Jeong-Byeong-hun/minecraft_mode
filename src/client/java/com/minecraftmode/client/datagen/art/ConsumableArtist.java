package com.minecraftmode.client.datagen.art;

import com.minecraftmode.consumable.ConsumableDef;
import com.minecraftmode.consumable.ConsumableDef.Shape;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * Icons for consumables (16x16) and their buff effects (18x18). Every shape is a character grid
 * tinted with the item's two colors:
 * <ul>
 * <li>{@code C/c/L} main color, darker, lighter; {@code A/a} accent and darker accent;</li>
 * <li>{@code o} outline (very dark main color), {@code W} white, {@code x} black;</li>
 * <li>{@code G/g} glass, {@code k/K} cork, {@code p/P} parchment, {@code v} leaf green.</li>
 * </ul>
 */
public final class ConsumableArtist {
	private static final Map<Shape, String[]> SHAPES = new EnumMap<>(Shape.class);

	static {
		SHAPES.put(Shape.BOTTLE, new String[] {
			"................",
			"......kkkk......",
			"......kKKk......",
			".......GG.......",
			".......Gg.......",
			".....oGGGgo.....",
			"....oLCCCCCo....",
			"...oCLWCCCCCo...",
			"...oCLLCCCCco...",
			"...oCCCCCCCco...",
			"...oCCCCCCcco...",
			"...oCCCCCccco...",
			"....occcccco....",
			".....oooooo.....",
			"................",
			"................"});
		SHAPES.put(Shape.VIAL, new String[] {
			"................",
			"......kkkk......",
			"......kKKk......",
			"......oGgo......",
			"......oGgo......",
			".....oGGGgo.....",
			".....oLCCCo.....",
			".....oWCCCo.....",
			".....oLCCCo.....",
			".....oLCCco.....",
			".....oCCCco.....",
			".....oCCcco.....",
			".....oCCcco.....",
			"......occo......",
			".......oo.......",
			"................"});
		SHAPES.put(Shape.FLASK, new String[] {
			"................",
			"......kkkk......",
			"......kKKk......",
			"......oGgo......",
			"......oGgo......",
			".....oGGGgo.....",
			".....oLCCCo.....",
			"....oCLCCCCo....",
			"....oLWCCCCo....",
			"...oCLCCCCCco...",
			"...oCCCCCCCco...",
			"..oCCCCCCCccco..",
			"..oCCCCCCcccco..",
			"..oocccccccccoo.",
			"...ooooooooooo..",
			"................"});
		SHAPES.put(Shape.CUP, new String[] {
			"................",
			".....W...W......",
			"......W...W.....",
			".....W...W......",
			"................",
			"...oooooooo.....",
			"...oCLCCCCoooo..",
			"...oAAAAAAo..o..",
			"...oAWAAAAo..o..",
			"...oAWAAAAoooo..",
			"...oAAAAAao.....",
			"...oAAAAaao.....",
			"....oaaaao......",
			".....oooo.......",
			"................",
			"................"});
		SHAPES.put(Shape.CAN, new String[] {
			"................",
			".....oooooo.....",
			"....oWWWWWWo....",
			"....oaaaaaao....",
			"....oCCCCCCo....",
			"....oCLCCCCo....",
			"....oLWAACCo....",
			"....oCLAACCo....",
			"....oCAAACCo....",
			"....oCLAACco....",
			"....oCCCCcco....",
			"....oCCCccco....",
			"....oaaaaaao....",
			".....oooooo.....",
			"................",
			"................"});
		SHAPES.put(Shape.BEAN, new String[] {
			"................",
			"................",
			"................",
			"................",
			"......oooo......",
			"....ooCLLCoo....",
			"...oCLWLCCCCo...",
			"..oCLLLCCCCCco..",
			"..oCCLCCCAAcco..",
			"..oCCCCCAACcco..",
			"...oCCCCCcccco..",
			"....ooccccooo...",
			"......oooo......",
			"................",
			"................",
			"................"});
		SHAPES.put(Shape.PILL, new String[] {
			"................",
			"................",
			"................",
			".....oooooo.....",
			"....oCLLCCCo....",
			"...oCLWLCCACo...",
			"...oCLLCCCCco...",
			"...oCCCACCCco...",
			"...oCCCCCCcco...",
			"...oCACCCccco...",
			"....occcccco....",
			".....oooooo.....",
			"................",
			"................",
			"................",
			"................"});
		SHAPES.put(Shape.ROLL, new String[] {
			"................",
			"................",
			".....oooooo.....",
			"....oCCCCCCo....",
			"...oCWWWWWWCo...",
			"..oCWWWAAWWWCo..",
			"..oCWWAavAWWCo..",
			"..oCWAaLLaAWCo..",
			"..oCWAaLLaAWCo..",
			"..oCWWAvaAWWCo..",
			"..oCWWWAAWWWCo..",
			"...oCWWWWWWCo...",
			"....oCCCCCCo....",
			".....oooooo.....",
			"................",
			"................"});
		SHAPES.put(Shape.BOWL, new String[] {
			"................",
			"................",
			"................",
			"................",
			"....LCCLWCLC....",
			"...LCCcLCCcCC...",
			"..LCLCCCCLCCcC..",
			"..oAAAAAAAAAAo..",
			"..oAWAAAAAAAao..",
			"...oAWAAAAAao...",
			"...oAAAAAAaao...",
			"....oAAAAaao....",
			".....oaaaao.....",
			"......oooo......",
			"................",
			"................"});
		SHAPES.put(Shape.MEAT, new String[] {
			"................",
			"..WW............",
			".WWWW...........",
			"..WWWW..........",
			"...WWoo.........",
			"....ooCCCo......",
			".....oCLCCCo....",
			"....oCLWCCCCo...",
			"....oCLCCCCCco..",
			"....oCCCCCCcco..",
			".....oCCCCcco...",
			"......occcco.WW.",
			".......oooo.WWWW",
			".............WW.",
			"................",
			"................"});
		SHAPES.put(Shape.FISH_BREAD, new String[] {
			"................",
			"................",
			"................",
			"................",
			".....ooooo....o.",
			"...ooCCCCCoo.oo.",
			"..oCLLCCcCCCoCo.",
			".oCxLCCcCCCCCCo.",
			".oCLLCCcCCCCcCo.",
			"..oCCCCcCCCcoCo.",
			"...ooCCCCCoo.oo.",
			".....ooooo....o.",
			"................",
			"................",
			"................",
			"................"});
		SHAPES.put(Shape.SWEET_POTATO, new String[] {
			"................",
			"................",
			"................",
			"......oooo......",
			"....ooCCCCoo....",
			"...oCLCCCCCCo...",
			"..oCLCAAAACCco..",
			"..oCCAALAAACco..",
			"..oCCAAAAAACco..",
			"...oCCAAAACco...",
			"....ooccccoo....",
			"......oooo......",
			"................",
			"................",
			"................",
			"................"});
		SHAPES.put(Shape.RICE_BALL, new String[] {
			"................",
			"................",
			".......oo.......",
			"......oWWo......",
			".....oWWWWo.....",
			"....oWWLCCCo....",
			"....oWLCCCCo....",
			"...oWCCCCCCCo...",
			"...oCCCCCCCCo...",
			"..oCCCAAAACCCo..",
			"..oCCCAAAACCco..",
			".oCCCCAAAACccco.",
			".ooooooooooooooo",
			"................",
			"................",
			"................"});
		SHAPES.put(Shape.COOKIE, new String[] {
			"................",
			"................",
			"................",
			"................",
			".....oooooo.....",
			"...ooCCCCCCoo...",
			"..oCLLCCCCCCCo..",
			".oCLWWCCCCcCCco.",
			".oCLCCCCCcccCco.",
			"..oCCCCCcAAAco..",
			"...ooccccAAoo...",
			".....ooooAo.....",
			"................",
			"................",
			"................",
			"................"});
		SHAPES.put(Shape.MUSHROOM, new String[] {
			"................",
			".....oooooo.....",
			"...ooCCWWCCoo...",
			"..oCCWWWWCCCCo..",
			".oCWWCCCCCWWCCo.",
			".oCWWCCCCCWWWCo.",
			".oCCCCWWCCCCCCo.",
			".oooooooooooooo.",
			"....oAAAAAAo....",
			"....oAxAAxAo....",
			"....oAxAAxAo....",
			"....oAAAAAao....",
			".....oaaaao.....",
			"......oooo......",
			"................",
			"................"});
		SHAPES.put(Shape.STAR, new String[] {
			".......oo.......",
			"......oCCo......",
			"......oLCo......",
			".....oCLCCo.....",
			"oooooCLWCCooooo.",
			"oCCCCCxCCxCCCCo.",
			".oCCCCxCCxCCCo..",
			"..oCCCCCCCCCo...",
			"...oCCCCCCCCo...",
			"...oCCCCoCCCo...",
			"..oCCCo..oCCCo..",
			"..oCCo....oCCo..",
			".oCo........oCo.",
			".oo..........oo.",
			"................",
			"................"});
		SHAPES.put(Shape.CANDY, new String[] {
			"................",
			"................",
			"................",
			"................",
			".oo..........oo.",
			".oAo..oooo..oAo.",
			".oAAooCCCCooAAo.",
			".oAAoCLWCCCoAAo.",
			".oAAoWWWWWWoAAo.",
			".oAAoCCCCCcoAAo.",
			".oAAooccccooAAo.",
			".oAo..oooo..oAo.",
			".oo..........oo.",
			"................",
			"................",
			"................"});
		SHAPES.put(Shape.FEATHER, new String[] {
			"............oo..",
			"..........ooAo..",
			".........oAAAo..",
			"........oCAAo...",
			".......oCCAo....",
			"......oCLCAo....",
			".....oCLCCo.....",
			"....oCLCCo......",
			"...oCLCCo.......",
			"..oCCCco........",
			"..oCcco.........",
			".oKoo...........",
			"oK..............",
			"................",
			"................",
			"................"});
		SHAPES.put(Shape.HEART, new String[] {
			"................",
			"................",
			"..oooo...oooo...",
			".oCCCCo.oCCCCo..",
			"oCLWCCCoCCCCCCo.",
			"oCLCCACCCCACCCo.",
			"oCCCCAACCAACCCo.",
			"oCCCCCAAAACCCco.",
			".oCCCCCAACCCco..",
			"..oCCCCCCCCco...",
			"...oCCCCCcco....",
			"....oCCCcco.....",
			".....oCcco......",
			"......ooo.......",
			"................",
			"................"});
		SHAPES.put(Shape.STONE, new String[] {
			"................",
			"................",
			"........oo......",
			".......oLLo.....",
			"......oLWCo.....",
			".....oLLCCCo....",
			".....oLCACco....",
			"....oLCCCCcco...",
			"....oLCACccco...",
			"...oLCCCCcccco..",
			"...oCCCCccccco..",
			"....oCCcccco....",
			".....ooooooo....",
			"................",
			"................",
			"................"});
		SHAPES.put(Shape.BOX, new String[] {
			"................",
			"................",
			"................",
			".oooooooooooooo.",
			".oCCCCCCCCCCCCo.",
			".oCWWWWoAAAAACo.",
			".oCWWxWoAAaAACo.",
			".oCWWWWoAaaAACo.",
			".oCWWWWooooooCo.",
			".oCWWWWovvLLvCo.",
			".oCWWWWovvvvLCo.",
			".oCccccccccccco.",
			".oooooooooooooo.",
			"................",
			"................",
			"................"});
		SHAPES.put(Shape.JERKY, new String[] {
			"................",
			"................",
			"....oo..........",
			"...oCCo.........",
			"...oCLCo..oo....",
			"...oCLCCooCCo...",
			"....oCCCCoCLCo..",
			".....oCCCoCLCCo.",
			"...oo.oCCcoCCCo.",
			"..oCCo.occooCCo.",
			"..oCLCo..oo.oco.",
			"...oCCCo.....o..",
			"....oCcco.......",
			".....ooo........",
			"................",
			"................"});
		SHAPES.put(Shape.SCROLL, new String[] {
			"................",
			"................",
			"..oooooooooooo..",
			".oAAAAAAAAAAAAo.",
			".oAaaaaaaaaaaAo.",
			"..oppppppppppo..",
			"..opPPPPPPPPpo..",
			"..oppppppppppo..",
			"..opPPPPPPpppo..",
			"..opppppppCCpo..",
			"..opppppppCLpo..",
			".oAaaaaaaaaaaAo.",
			".oAAAAAAAAAAAAo.",
			"..oooooooooooo..",
			"................",
			"................"});
	}

	/** Effect icon glyphs (12x12, drawn in the middle of an 18x18 tile). */
	private static final Map<String, String[]> GLYPHS = Map.of(
		"fury", new String[] {
			"..........oo",
			".........oWo",
			"........oWCo",
			".......oWCo.",
			"..o...oWCo..",
			"..oo.oWCo...",
			"...ooWCo....",
			"....oCo.....",
			"...oAoo.....",
			"..oAo.oo....",
			".oAo........",
			".oo........."},
		"ironskin", new String[] {
			"..oooooooo..",
			".oWWCCCCCCo.",
			".oWCCCCCCCo.",
			".oCCCCCCCco.",
			".oCCCAACCco.",
			".oCCAAAACco.",
			".oCCCAACcco.",
			"..oCCCCcco..",
			"..oCCCccco..",
			"...oCCcco...",
			"....occo....",
			".....oo....."},
		"precision", new String[] {
			".....oo.....",
			".....CC.....",
			"...oCCCCo...",
			"..oC.CC.Co..",
			".oC..CC..Co.",
			"oCCCCWWCCCCo",
			"oCCCCWWCCCCo",
			".oC..CC..Co.",
			"..oC.CC.Co..",
			"...oCCCCo...",
			".....CC.....",
			".....oo....."},
		"arcana", new String[] {
			".....oo.....",
			".....oCo....",
			"....oCWCo...",
			"oooooCWCooooo",
			"oCCCCCWCCCCo",
			".oCCWWWWCCo.",
			"..oCCWWCCo..",
			"..oCCCCCCo..",
			".oCCCooCCCo.",
			".oCCo..oCCo.",
			"oCo......oCo",
			"oo........oo"},
		"focus", new String[] {
			".oooooooooo.",
			".oAAAAAAAAo.",
			"..oWCCCCCo..",
			"...oCCCCo...",
			"....oCCo....",
			".....oo.....",
			".....oo.....",
			"....o..o....",
			"...o.CC.o...",
			"..oCCCCCCo..",
			".oAAAAAAAAo.",
			".oooooooooo."},
		"clarity", new String[] {
			".....oo.....",
			".....oCo....",
			"....oCCo....",
			"....oCCCo...",
			"...oCWCCo...",
			"...oCWCCCo..",
			"..oCWCCCCo..",
			"..oCCCCCCo..",
			"..oCCCCCco..",
			"...oCCCco...",
			"....oooo....",
			"............"},
		"rejuvenation", new String[] {
			"............",
			".ooo...ooo..",
			"oCCCo.oCCCo.",
			"oCWCCoCCCCo.",
			"oCCCCCCCCCo.",
			"oCCCWWWCCCo.",
			".oCCCWCCCo..",
			"..oCCWCCo...",
			"...oCCCo....",
			"....oCo.....",
			".....o......",
			"............"},
		"fortune", new String[] {
			"...oooooo...",
			"..oCCCCCCo..",
			".oCWWCCCCco.",
			"oCWCCAACCCco",
			"oCWCAAAACCco",
			"oCCCAACCCCco",
			"oCCCCAACCCco",
			"oCCCAAAACCco",
			"oCCCCAACCcco",
			".oCCCCCCcco.",
			"..occcccco..",
			"...oooooo..."},
		"wisdom", new String[] {
			"............",
			".oooo..oooo.",
			"oWWWWooWWWWo",
			"oWCCWooWCCWo",
			"oWWWWooWWWWo",
			"oWCCWooWCCWo",
			"oWWWWooWWWWo",
			"oWCCWooWCCWo",
			"oWWWWooWWWWo",
			"oooooooooooo",
			".oAAAAAAAAo.",
			"..oooooooo.."},
		"slayer", new String[] {
			"..oooooooo..",
			".oWWWWWWWWo.",
			"oWWWWWWWWWWo",
			"oWxxWWWWxxWo",
			"oWxxxWWxxxWo",
			"oWWxxWWxxWWo",
			"oWWWWxxWWWWo",
			".oWWWWWWWWo.",
			"..oWoWoWoWo.",
			"..oCoCoCoCo.",
			"...ooooooo..",
			"............"}
	);

	public static BufferedImage icon(final ConsumableDef def) {
		return paint(SHAPES.get(def.shape()), 16, 0, def.color(), def.accent());
	}

	/** The return scroll uses the SCROLL shape in blue and gold. */
	public static BufferedImage scroll() {
		return paint(SHAPES.get(Shape.SCROLL), 16, 0, 0x2E5AA8, 0xC89A50);
	}

	public static BufferedImage effectIcon(final String id, final int color) {
		String[] glyph = GLYPHS.get(id);
		if (glyph == null) {
			throw new IllegalArgumentException("no glyph for " + id);
		}
		return paint(glyph, 18, 3, color, 0xFFD24A);
	}

	private static BufferedImage paint(final String[] rows, final int size, final int offset, final int color, final int accent) {
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < rows.length; y++) {
			String row = rows[y];
			for (int x = 0; x < row.length(); x++) {
				int rgb = switch (row.charAt(x)) {
					case 'C' -> color;
					case 'c' -> shade(color, 0.72F);
					case 'L' -> lighten(color, 0.35F);
					case 'A' -> accent;
					case 'a' -> shade(accent, 0.72F);
					case 'o' -> shade(color, 0.3F);
					case 'W' -> 0xFFFFFF;
					case 'x' -> 0x1A1A1A;
					case 'G' -> 0xD8ECF8;
					case 'g' -> 0x9AB8D0;
					case 'k' -> 0xA0703C;
					case 'K' -> 0x6A4422;
					case 'p' -> 0xEEDDB4;
					case 'P' -> 0xB8A070;
					case 'v' -> 0x4CA82A;
					default -> -1;
				};
				int px = x + offset;
				int py = y + offset;
				if (rgb != -1 && px < size && py < size) {
					img.setRGB(px, py, 0xFF000000 | rgb);
				}
			}
		}
		return img;
	}

	static int shade(final int rgb, final float f) {
		int r = Math.round(((rgb >> 16) & 0xFF) * f);
		int g = Math.round(((rgb >> 8) & 0xFF) * f);
		int b = Math.round((rgb & 0xFF) * f);
		return r << 16 | g << 8 | b;
	}

	static int lighten(final int rgb, final float f) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		return Math.round(r + (255 - r) * f) << 16 | Math.round(g + (255 - g) * f) << 8 | Math.round(b + (255 - b) * f);
	}

	private ConsumableArtist() {
	}
}
