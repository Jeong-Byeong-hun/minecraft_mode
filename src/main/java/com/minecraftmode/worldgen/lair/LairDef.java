package com.minecraftmode.worldgen.lair;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMobs;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jspecify.annotations.Nullable;

/**
 * One named monster's lair: a big walled maze under a landmark shell (pyramid, ziggurat, fortress,
 * dome, spire or giant tree), or an underground maze under a beacon tower. The maze, the goal room and
 * the guardian come from {@link LairPiece}; this record holds what differs per lair.
 *
 * @param id       the named monster's id (the lair is {@code minecraft_mode:lair_<id>})
 * @param depth    floor height of underground lairs
 * @param decor    blocks scattered through the corridors
 */
public record LairDef(
	String id, String en, String ko, Setting setting, Shape shape, Palette palette, List<ResourceKey<Biome>> biomes, LairMobs mobs, int depth,
	List<Decor> decor
) {
	public LairDef {
		biomes = List.copyOf(biomes);
		decor = List.copyOf(decor);
	}

	/** Where the lair sits. */
	public enum Setting {
		SURFACE,
		UNDERGROUND,
		NETHER,
		END
	}

	/** The landmark built on top of the maze (underground lairs get a tower instead). */
	public enum Shape {
		PYRAMID,
		ZIGGURAT,
		FORTRESS,
		DOME,
		SPIRE,
		TREE,
		NONE
	}

	/**
	 * @param leaves canopy of {@link Shape#TREE} lairs (null = a dead tree)
	 */
	public record Palette(Block wall, Block floor, Block roof, Block accent, Block light, Block stairs, Block foundation, @Nullable Block leaves) {
	}

	/** Where a decoration goes in a corridor block: replacing the floor, standing on it, or hanging from the roof. */
	public enum Place {
		FLOOR,
		GROUND,
		CEILING
	}

	/** @param perMille chance per corridor column */
	public record Decor(Block block, Place place, int perMille) {
	}

	public NamedDef named() {
		return NamedMobs.byId(this.id);
	}

	/** Maze cells per side: 13 for the weakest lairs up to 19 for the strongest. */
	public int cells() {
		return 13 + 2 * Math.min(3, Math.max(0, (this.named().lo() - 10) / 20));
	}

	/** Side length of the maze in blocks (cells of a 3-wide corridor plus a wall). */
	public int size() {
		return this.cells() * LairPiece.CELL + 1;
	}

	public String structureId() {
		return "lair_" + this.id;
	}

	public ResourceKey<Structure> key() {
		return ResourceKey.create(Registries.STRUCTURE, MinecraftMode.id(this.structureId()));
	}

	public String nameKey() {
		return "structure.minecraft_mode." + this.structureId();
	}
}
