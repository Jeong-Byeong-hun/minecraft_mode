package com.minecraftmode.residence;

import com.minecraftmode.MinecraftMode;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Crops in the residence keep growing while nobody is near. Random ticks only reach block-ticking chunks (a player within
 * simulation distance), so a chunk with crops that stops ticking remembers the game time, and when it ticks again its crops get
 * the random ticks they missed (game rule speed, one crop's share per tick) at once, through the block's own {@code randomTick}:
 * light, farmland, neighbours and the mod's slower crops count as usual. Only the residence, only crops ({@link CropBlock}).
 */
public final class IdleGrowth extends SavedData {
	/** Random ticks one crop may catch up at once: far more than any crop needs to ripen, a ceiling for dark ones that never do. */
	private static final int MAX_TICKS = 4096;
	private static final int SECTION_BLOCKS = 16 * 16 * 16;

	private static final Codec<IdleGrowth> CODEC = Codec.unboundedMap(Codec.STRING, Codec.LONG)
		.xmap(IdleGrowth::new, d -> d.paused).fieldOf("paused").codec();

	public static final SavedDataType<IdleGrowth> TYPE = new SavedDataType<>(MinecraftMode.id("idle_growth"), IdleGrowth::new, CODEC, null);

	/** Chunks that started ticking this tick; caught up at the end of the tick, not inside the status change. */
	private static final Set<Long> WAKING = new HashSet<>();

	/** Packed chunk pos -> game time its crops stopped ticking. */
	private final Map<String, Long> paused;

	public IdleGrowth() {
		this(Map.of());
	}

	private IdleGrowth(final Map<String, Long> paused) {
		this.paused = new HashMap<>(paused);
	}

	public static void init() {
		ServerChunkEvents.FULL_CHUNK_STATUS_CHANGE.register((level, chunk, from, to) -> {
			if (!level.dimension().equals(ResidenceDimension.LEVEL)) {
				return;
			}
			boolean was = from.isOrAfter(FullChunkStatus.BLOCK_TICKING);
			boolean is = to.isOrAfter(FullChunkStatus.BLOCK_TICKING);
			if (was && !is && hasCrops(chunk)) {
				get(level).pause(chunk.getPos(), level.getGameTime());
			} else if (!was && is) {
				WAKING.add(chunk.getPos().pack());
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(IdleGrowth::wake);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> WAKING.clear());
	}

	private static IdleGrowth get(final ServerLevel level) {
		return level.getServer().getDataStorage().computeIfAbsent(TYPE);
	}

	/** Whether a chunk's crops wait to catch up (they stopped ticking and have not ticked since). */
	public static boolean waiting(final ServerLevel level, final ChunkPos pos) {
		return get(level).paused.containsKey(Long.toString(pos.pack()));
	}

	private void pause(final ChunkPos pos, final long now) {
		this.paused.put(Long.toString(pos.pack()), now);
		this.setDirty();
	}

	private static void wake(final MinecraftServer server) {
		if (WAKING.isEmpty()) {
			return;
		}
		ServerLevel level = server.getLevel(ResidenceDimension.LEVEL);
		if (level == null) {
			WAKING.clear();
			return;
		}
		// a copy: catching up reads blocks, and a neighbour chunk finishing its load there fires the status event (WAKING.add) again;
		// until 0.5.24 that changed the set under the loop and crashed the server
		List<Long> keys = new ArrayList<>(WAKING);
		WAKING.clear();
		IdleGrowth data = get(level);
		for (long key : keys) {
			ChunkPos pos = ChunkPos.unpack(key);
			LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x(), pos.z());
			if (chunk == null) {
				// unloaded again before it caught up: the paused time stays for the next wake
				continue;
			}
			if (!neighboursLoaded(level, pos)) {
				// crops read their neighbours' farmland, which would load (block on) a chunk still in progress: try next tick
				WAKING.add(key);
				continue;
			}
			Long since = data.paused.remove(Long.toString(key));
			if (since != null) {
				data.setDirty();
				catchUp(level, chunk, level.getGameTime() - since);
			}
		}
	}

	private static boolean neighboursLoaded(final ServerLevel level, final ChunkPos pos) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (level.getChunkSource().getChunkNow(pos.x() + dx, pos.z() + dz) == null) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean isCrop(final BlockState state) {
		return state.getBlock() instanceof CropBlock;
	}

	private static boolean hasCrops(final LevelChunk chunk) {
		for (LevelChunkSection section : chunk.getSections()) {
			if (section.maybeHas(IdleGrowth::isCrop)) {
				return true;
			}
		}
		return false;
	}

	/** Gives the chunk's crops the random ticks {@code elapsed} game ticks would have brought them. */
	public static void catchUp(final ServerLevel level, final LevelChunk chunk, final long elapsed) {
		double perCrop = elapsed * (double) level.getGameRules().get(GameRules.RANDOM_TICK_SPEED) / SECTION_BLOCKS;
		if (perCrop <= 0) {
			return;
		}
		RandomSource random = level.getRandom();
		ChunkPos chunkPos = chunk.getPos();
		LevelChunkSection[] sections = chunk.getSections();
		for (int i = 0; i < sections.length; i++) {
			LevelChunkSection section = sections[i];
			if (!section.maybeHas(IdleGrowth::isCrop)) {
				continue;
			}
			int minY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						if (isCrop(section.getBlockState(x, y, z))) {
							BlockPos pos = new BlockPos(chunkPos.getMinBlockX() + x, minY + y, chunkPos.getMinBlockZ() + z);
							grow(level, pos, (int) Math.min(MAX_TICKS, perCrop + random.nextDouble()), random);
						}
					}
				}
			}
		}
	}

	private static void grow(final ServerLevel level, final BlockPos pos, final int ticks, final RandomSource random) {
		for (int t = 0; t < ticks; t++) {
			BlockState state = level.getBlockState(pos);
			if (!(state.getBlock() instanceof CropBlock crop) || crop.isMaxAge(state)) {
				return;
			}
			state.randomTick(level, pos, random);
		}
	}
}
