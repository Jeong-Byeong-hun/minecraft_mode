package com.minecraftmode.worldgen.lair;

/**
 * The corridor geometry a lair is built in, saved with each {@link LairPiece} ("Layout"). A lair is built chunk by chunk as the
 * world generates, so one started before an update must finish in its own layout or its halves would not meet: pieces saved without
 * the tag are {@link #V1}, new lairs start in {@link #CURRENT}.
 *
 * @param cell    blocks per maze cell (corridor width + one wall)
 * @param roof    roof height above the floor (corridors are {@code roof - 1} high)
 * @param gentle  underground shafts climb with a landing at every corner, so each stair is walked onto from its low front
 * @param wide    wider approach corridor, a second tower door and wider ziggurat stairs
 */
public record LairLayout(int version, int cell, int roof, boolean gentle, boolean wide) {
	/** Until 0.5.47: 3-wide, 4-high corridors (big lords stood in the roof), a steep one-block spiral up the shaft. */
	public static final LairLayout V1 = new LairLayout(1, 4, 5, false, false);
	/** From 0.5.48: 4-wide, 6-high corridors, an easy spiral with landings, doors on both sides of the tower. */
	public static final LairLayout V2 = new LairLayout(2, 5, 7, true, true);
	public static final LairLayout CURRENT = V2;

	public static LairLayout of(final int version) {
		return version >= V2.version ? V2 : V1;
	}

	/** Side length of {@code def}'s maze in blocks. */
	public int size(final LairDef def) {
		return def.cells() * this.cell + 1;
	}
}
