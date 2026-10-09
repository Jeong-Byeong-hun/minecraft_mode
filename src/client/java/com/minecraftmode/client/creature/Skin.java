package com.minecraftmode.client.creature;

/**
 * How a body part is painted: base color, an accent (spots, runes, cracks, trims) and a pattern. With
 * {@code glow} the accent pixels also go to the emissive layer (eyes are always emissive).
 */
public record Skin(int base, int accent, Pattern pattern, boolean glow) {
	public enum Pattern {
		/** Even surface with fine noise (skin, slime, cloth without folds). */
		SMOOTH,
		/** Vertical streaks (fur, hair). */
		FUR,
		/** Overlapping scales in diamond rows. */
		SCALES,
		/** Blotches and cracks. */
		STONE,
		/** Bevelled plates with rivets in the accent color. */
		METAL,
		/** Vertical folds and an accent hem. */
		CLOTH,
		/** Pale bone with dark seams. */
		BONE,
		/** Shiny segment plates (insects, scorpions). */
		CHITIN,
		/** Diagonal facets, accent highlights. */
		CRYSTAL,
		/** Fire: accent at the bottom fading to base at the top. */
		FLAME,
		/** Dark void with accent star speckles. */
		VOID,
		/** Vertical grain and knots. */
		BARK,
		/** Dark rock with accent cracks (magma, souls). */
		CRACKED,
		/** Feather chevrons. */
		FEATHER,
		/** Mushroom cap: base with round accent spots. */
		SPOTS
	}

	public static Skin of(final int base, final Pattern pattern) {
		return new Skin(base, lighten(base, 0.35F), pattern, false);
	}

	public static Skin of(final int base, final int accent, final Pattern pattern) {
		return new Skin(base, accent, pattern, false);
	}

	public static Skin glowing(final int base, final int accent, final Pattern pattern) {
		return new Skin(base, accent, pattern, true);
	}

	static int lighten(final int rgb, final float t) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		return Math.round(r + (255 - r) * t) << 16 | Math.round(g + (255 - g) * t) << 8 | Math.round(b + (255 - b) * t);
	}
}
