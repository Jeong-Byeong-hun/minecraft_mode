package com.minecraftmode.worldgen.lair;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Finds where a lair goes: centered on the start chunk, on the averaged ground (surface and End), at a
 * fixed height in the Nether, or deep underground with a tower above. Lairs stay away from the capital
 * and from the End's central island.
 */
public class LairStructure extends Structure {
	public static final MapCodec<LairStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		settingsCodec(i),
		Codec.STRING.fieldOf("lair").forGetter(LairStructure::lair)
	).apply(i, LairStructure::new));

	/** No surface or underground lair within this many blocks of 0, 0 (the capital). */
	private static final int CAPITAL_CLEARANCE = 320;
	/** No End lair on the central island. */
	private static final int END_CLEARANCE = 1100;
	/** Surface lairs skip ground that varies more than this. */
	private static final int MAX_ROUGHNESS = 28;
	public static final int NETHER_FLOOR = 40;

	private final String lair;

	public LairStructure(final StructureSettings settings, final String lair) {
		super(settings);
		this.lair = lair;
	}

	public String lair() {
		return this.lair;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(final GenerationContext context) {
		LairDef def = NamedLairs.def(this.lair);
		if (def == null) {
			return Optional.empty();
		}
		ChunkPos chunk = context.chunkPos();
		int cx = chunk.getMiddleBlockX();
		int cz = chunk.getMiddleBlockZ();
		long distance = (long)cx * cx + (long)cz * cz;
		ChunkGenerator generator = context.chunkGenerator();
		LevelHeightAccessor heights = context.heightAccessor();
		RandomState random = context.randomState();
		int reach = def.size() / 2 + LairPiece.APRON;
		int floor;
		int surface = 0;
		switch (def.setting()) {
			case SURFACE -> {
				if (distance < (long)CAPITAL_CLEARANCE * CAPITAL_CLEARANCE || !context.couldValidBiomeExistOnTopOfChunkCenter()) {
					return Optional.empty();
				}
				int[] h = this.samples(generator, heights, random, cx, cz, reach);
				if (h[h.length - 1] - h[0] > MAX_ROUGHNESS) {
					return Optional.empty();
				}
				floor = Math.max(Arrays.stream(h).sum() / h.length, generator.getSeaLevel());
			}
			case END -> {
				if (distance < (long)END_CLEARANCE * END_CLEARANCE) {
					return Optional.empty();
				}
				int[] h = this.samples(generator, heights, random, cx, cz, reach);
				if (h[0] < 40 || h[h.length - 1] - h[0] > MAX_ROUGHNESS) {
					return Optional.empty();
				}
				floor = Arrays.stream(h).sum() / h.length;
			}
			case NETHER -> floor = NETHER_FLOOR;
			default -> {
				if (distance < (long)CAPITAL_CLEARANCE * CAPITAL_CLEARANCE) {
					return Optional.empty();
				}
				floor = def.depth();
				surface = generator.getFirstOccupiedHeight(cx, LairPiece.shaftZ(def, cz), Heightmap.Types.WORLD_SURFACE_WG, heights, random);
				surface = Math.max(surface, generator.getSeaLevel());
				if (surface < floor + 16) {
					return Optional.empty();
				}
			}
		}
		if (floor + LairPiece.topOffset(def) >= heights.getMaxY() - 2 || floor - 2 <= heights.getMinY()) {
			return Optional.empty();
		}
		int checkY = def.setting() == LairDef.Setting.UNDERGROUND ? floor + 2 : floor + 1;
		long seed = context.random().nextLong();
		int finalFloor = floor;
		int finalSurface = surface;
		return Optional.of(new GenerationStub(new BlockPos(cx, checkY, cz),
			builder -> builder.addPiece(new LairPiece(def, cx, finalFloor, cz, finalSurface, seed))));
	}

	/** Ground heights at the center, corners and edge midpoints of the lair, sorted. */
	private int[] samples(final ChunkGenerator generator, final LevelHeightAccessor heights, final RandomState random, final int cx, final int cz,
		final int reach) {
		int[] h = new int[9];
		int i = 0;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				h[i++] = generator.getFirstOccupiedHeight(cx + dx * reach, cz + dz * reach, Heightmap.Types.WORLD_SURFACE_WG, heights, random);
			}
		}
		Arrays.sort(h);
		return h;
	}

	@Override
	public StructureType<?> type() {
		return NamedLairs.TYPE;
	}
}
