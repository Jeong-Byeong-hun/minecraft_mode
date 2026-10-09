package com.minecraftmode.raid;

import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Look of a raid arena: floor mix, underside rock, rim wall, pillars and their light. {@link Arenas}
 * builds every arena with the same footprint (a floating disc with a rim and eight pillars) and adds
 * each theme's own features (webs, snow, a sea pool, a crater, candles, floating shards).
 */
public enum ArenaTheme {
	/** Arachne: a web-choked cave floor. */
	WEB_CAVE(
		List.of(Blocks.STONE, Blocks.STONE, Blocks.COBBLESTONE, Blocks.ANDESITE, Blocks.TUFF, Blocks.MOSSY_COBBLESTONE, Blocks.GRAVEL),
		List.of(Blocks.STONE, Blocks.DEEPSLATE, Blocks.TUFF), Blocks.DEEPSLATE_BRICKS, Blocks.COBBLED_DEEPSLATE_WALL, Blocks.POLISHED_DEEPSLATE, Blocks.OCHRE_FROGLIGHT, 5),
	/** Gorvath: a windswept summit. */
	MOUNTAIN(
		List.of(Blocks.STONE, Blocks.SNOW_BLOCK, Blocks.SNOW_BLOCK, Blocks.PACKED_ICE, Blocks.CALCITE, Blocks.ANDESITE, Blocks.GRAVEL),
		List.of(Blocks.STONE, Blocks.ANDESITE, Blocks.DIORITE), Blocks.STONE_BRICKS, Blocks.STONE_BRICK_WALL, Blocks.CHISELED_STONE_BRICKS, Blocks.SEA_LANTERN, 3),
	/** Kraken: the deck of a sunken ship around a deep pool. */
	SUNKEN_SHIP(
		List.of(Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.STRIPPED_SPRUCE_LOG, Blocks.PRISMARINE),
		List.of(Blocks.PRISMARINE, Blocks.DARK_PRISMARINE, Blocks.SAND), Blocks.DARK_PRISMARINE, Blocks.SPRUCE_FENCE, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.SEA_LANTERN, 3),
	/** Ignis: the floor of a volcanic crater. */
	VOLCANO(
		List.of(Blocks.BASALT, Blocks.BLACKSTONE, Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS, Blocks.SMOOTH_BASALT),
		List.of(Blocks.BASALT, Blocks.BLACKSTONE, Blocks.MAGMA_BLOCK), Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.BLACKSTONE_WALL, Blocks.BASALT, Blocks.SHROOMLIGHT, 4),
	/** Malachar: a sanctum of the dead. */
	NECROPOLIS(
		List.of(Blocks.DEEPSLATE_TILES, Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.SOUL_SOIL, Blocks.CRACKED_DEEPSLATE_TILES),
		List.of(Blocks.DEEPSLATE, Blocks.SOUL_SOIL, Blocks.BLACKSTONE), Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.DEEPSLATE_BRICK_WALL, Blocks.CHISELED_DEEPSLATE,
		Blocks.SOUL_LANTERN, 4),
	/** Aethryx: a floating island of the void. */
	VOID_ISLES(
		List.of(Blocks.END_STONE, Blocks.END_STONE, Blocks.END_STONE_BRICKS, Blocks.PURPUR_BLOCK, Blocks.OBSIDIAN),
		List.of(Blocks.END_STONE, Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN), Blocks.END_STONE_BRICKS, Blocks.END_STONE_BRICK_WALL, Blocks.OBSIDIAN, Blocks.END_ROD, 1);

	private final List<Block> floor;
	private final List<Block> underside;
	private final Block rim;
	private final Block rimTop;
	private final Block pillar;
	private final Block light;
	private final int rimHeight;

	ArenaTheme(final List<Block> floor, final List<Block> underside, final Block rim, final Block rimTop, final Block pillar, final Block light, final int rimHeight) {
		this.floor = floor;
		this.underside = underside;
		this.rim = rim;
		this.rimTop = rimTop;
		this.pillar = pillar;
		this.light = light;
		this.rimHeight = rimHeight;
	}

	public List<Block> floor() {
		return this.floor;
	}

	public List<Block> underside() {
		return this.underside;
	}

	public Block rim() {
		return this.rim;
	}

	public Block rimTop() {
		return this.rimTop;
	}

	public Block pillar() {
		return this.pillar;
	}

	public Block light() {
		return this.light;
	}

	public int rimHeight() {
		return this.rimHeight;
	}
}
