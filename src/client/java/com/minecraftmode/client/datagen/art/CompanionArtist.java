package com.minecraftmode.client.datagen.art;

import java.awt.image.BufferedImage;

/**
 * Icons of pet charms and mount whistles (16x16), tinted with the creature's two main skin colors and a rim in its rarity color:
 * {@code C/c/L} creature color, darker, lighter; {@code A/a} second color and darker; {@code R/r} rim and darker rim; {@code o}
 * outline; {@code W} white; {@code k} cord.
 */
public final class CompanionArtist {
	private static final String[] CHARM = {
		"......kkkk......",
		".....k....k.....",
		"......k..k......",
		".....orrrro.....",
		"...orRRRRRRro...",
		"..oRRLLCCCCRRo..",
		"..oRLLWCCCCcRo..",
		".oRLCCAACAAccRo.",
		".oRCCCAACAAccRo.",
		".oRCCCCCCCCccRo.",
		".oRCCCAAAACccRo.",
		"..oRCCAAAAccRo..",
		"..oRRcCAACcRRo..",
		"...orRRRRRRro...",
		".....orrrro.....",
		"................"};

	private static final String[] WHISTLE = {
		"................",
		"................",
		"............ooo.",
		"...........oLCo.",
		"..........oLCco.",
		".........oRRRo..",
		"........oCCrro..",
		"..kk...oLCcco...",
		".k..k.oLCCco....",
		".k...oRRRro.....",
		"..k.oCCCco......",
		"...oLCCcco......",
		"..oACCcco.......",
		"..oaAAco........",
		"...oooo.........",
		"................"};

	/** A pet charm: a round medallion with a paw print. */
	public static BufferedImage charm(final int color, final int second, final int rim) {
		return paint(CHARM, color, second, rim);
	}

	/** A mount whistle: a curved horn with bands. */
	public static BufferedImage whistle(final int color, final int second, final int rim) {
		return paint(WHISTLE, color, second, rim);
	}

	private static BufferedImage paint(final String[] rows, final int color, final int second, final int rim) {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < rows.length; y++) {
			String row = rows[y];
			for (int x = 0; x < row.length(); x++) {
				int rgb = switch (row.charAt(x)) {
					case 'C' -> color;
					case 'c' -> ConsumableArtist.shade(color, 0.72F);
					case 'L' -> ConsumableArtist.lighten(color, 0.35F);
					case 'A' -> second;
					case 'a' -> ConsumableArtist.shade(second, 0.72F);
					case 'R' -> rim;
					case 'r' -> ConsumableArtist.shade(rim, 0.7F);
					case 'o' -> ConsumableArtist.shade(color, 0.25F);
					case 'W' -> 0xFFFFFF;
					case 'k' -> 0x7A5A3A;
					default -> -1;
				};
				if (rgb != -1) {
					img.setRGB(x, y, 0xFF000000 | rgb);
				}
			}
		}
		return img;
	}

	private CompanionArtist() {
	}
}
