package com.minecraftmode.city;

import com.minecraftmode.MinecraftMode;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;

/**
 * Brings the homestead plains to worlds generated before they existed. Once per world, right after the server starts, every chunk of
 * the plains and their blend ring is loaded (or generated) and flattened like the generator would have done
 * ({@link CityGenerator#reshapeHomestead}: only the plains change, never the city), then the road and the travel circle are built.
 * One chunk per tick, in a fixed order; how far it got is saved, so a restart picks up where it stopped and a finished world is
 * never flattened again (whatever players build there later stays). In a world made with the plains the pass changes nothing.
 * Chunks are asked for with a ticket a few ahead and flattened once they are there, so the server never waits on one: until 0.5.32
 * each was loaded with a blocking {@code getChunk}, and a chunk whose generation threw never arrived and froze the server.
 */
public final class HomesteadLand extends SavedData {
	private static final Codec<HomesteadLand> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.optionalFieldOf("progress", 0).forGetter(d -> d.progress)
	).apply(i, HomesteadLand::new));

	public static final SavedDataType<HomesteadLand> TYPE = new SavedDataType<>(MinecraftMode.id("homestead_land"), HomesteadLand::new, CODEC, null);

	/** Loads a chunk of the pass and its neighbours (never ticking them) until it is flattened; not saved, a restart asks again. */
	private static final TicketType TICKET = Registry.register(BuiltInRegistries.TICKET_TYPE, MinecraftMode.id("homestead"),
		new TicketType(TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING));
	/** Chunks asked for ahead of the next one to flatten, so loading and generation keep up with one chunk per tick. */
	private static final int AHEAD = 8;
	/** How long the next chunk may take to arrive before the pass skips it (one whose generation failed never does). */
	private static final int GIVE_UP_TICKS = 1200;

	private static List<ChunkPos> chunks;
	private int progress;
	/** Chunks with a ticket out, the next one to flatten first (server memory only). */
	private final ArrayDeque<ChunkPos> asked = new ArrayDeque<>();
	private int waited;

	public HomesteadLand() {
		this(0);
	}

	private HomesteadLand(final int progress) {
		this.progress = progress;
	}

	public static HomesteadLand get(final ServerLevel overworld) {
		return overworld.getServer().getDataStorage().computeIfAbsent(TYPE);
	}

	/** Every chunk the plains or their blend ring touch, west to east, north to south. */
	public static List<ChunkPos> chunks() {
		if (chunks == null) {
			List<ChunkPos> list = new ArrayList<>();
			int reach = CityZone.BLEND;
			for (int cx = (Homestead.X0 - reach) >> 4; cx <= (Homestead.X1 + reach) >> 4; cx++) {
				for (int cz = (-Homestead.HALF_Z - reach) >> 4; cz <= (Homestead.HALF_Z + reach) >> 4; cz++) {
					list.add(new ChunkPos(cx, cz));
				}
			}
			chunks = List.copyOf(list);
		}
		return chunks;
	}

	public boolean done() {
		return this.progress >= chunks().size();
	}

	public int progress() {
		return this.progress;
	}

	/** Registers the pass's chunk ticket (mod init; the ticket registry is frozen by the time the server ticks). */
	public static void init() {
	}

	/** Called every server tick: flattens the next chunk until the whole plains are done. */
	public static void tick(final ServerLevel overworld) {
		if (!CityZone.isCityLevel(overworld)) {
			return;
		}
		HomesteadLand land = get(overworld);
		if (!land.done()) {
			land.step(overworld);
		}
	}

	/** Asks for the next few chunks and flattens the next one once it and its neighbours are loaded, or skips it after waiting too long. */
	private void step(final ServerLevel level) {
		ServerChunkCache cache = level.getChunkSource();
		List<ChunkPos> all = chunks();
		for (int i = this.progress + this.asked.size(); i < Math.min(all.size(), this.progress + AHEAD); i++) {
			cache.addTicketWithRadius(TICKET, all.get(i), 1);
			this.asked.addLast(all.get(i));
		}
		ChunkPos pos = this.asked.getFirst();
		if (loaded(cache, pos)) {
			reshape(level, pos.x(), pos.z());
		} else if (++this.waited < GIVE_UP_TICKS) {
			return;
		} else {
			MinecraftMode.LOGGER.warn("The homestead pass skips chunk {}: it did not load in {} ticks", pos, GIVE_UP_TICKS);
		}
		cache.removeTicketWithRadius(TICKET, pos, 1);
		this.asked.removeFirst();
		this.waited = 0;
		this.progress++;
		this.setDirty();
		if (this.done()) {
			MinecraftMode.LOGGER.info("The homestead plains are ready ({} chunks)", all.size());
		}
	}

	/** The chunk and its eight neighbours are loaded, so flattening it (block updates reach one block past its edge) loads nothing. */
	private static boolean loaded(final ServerChunkCache cache, final ChunkPos pos) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (cache.getChunkNow(pos.x() + dx, pos.z() + dz) == null) {
					return false;
				}
			}
		}
		return true;
	}

	/** Flattens the plains in one chunk (loading or generating it), builds the road and circle there and lifts anyone it buried. */
	public static void reshape(final ServerLevel level, final int cx, final int cz) {
		ChunkAccess chunk = level.getChunk(cx, cz);
		CityGenerator.reshapeHomestead(level, chunk);
		Homestead.build(new Build(level, CityZone.baseY(level), cx << 4, cz << 4));
		AABB box = new AABB(cx << 4, level.getMinY(), cz << 4, (cx << 4) + 16, level.getMaxY(), (cz << 4) + 16);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
			int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, entity.getBlockX(), entity.getBlockZ());
			boolean buried = !level.noCollision(entity, entity.getBoundingBox());
			if (buried || entity.getY() > top + 1.0) {
				if (entity instanceof ServerPlayer player) {
					player.teleportTo(entity.getX(), top, entity.getZ());
				} else {
					entity.teleportTo(entity.getX(), top, entity.getZ());
				}
				entity.resetFallDistance();
			}
		}
	}
}
