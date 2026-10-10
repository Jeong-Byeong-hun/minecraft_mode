package com.minecraftmode.city;

import com.minecraftmode.MinecraftMode;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 */
public final class HomesteadLand extends SavedData {
	private static final Codec<HomesteadLand> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.optionalFieldOf("progress", 0).forGetter(d -> d.progress)
	).apply(i, HomesteadLand::new));

	public static final SavedDataType<HomesteadLand> TYPE = new SavedDataType<>(MinecraftMode.id("homestead_land"), HomesteadLand::new, CODEC, null);

	private static List<ChunkPos> chunks;
	private int progress;

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

	/** Called every server tick: flattens the next chunk until the whole plains are done. */
	public static void tick(final ServerLevel overworld) {
		if (!CityZone.isCityLevel(overworld)) {
			return;
		}
		HomesteadLand land = get(overworld);
		if (land.done()) {
			return;
		}
		ChunkPos pos = chunks().get(land.progress);
		reshape(overworld, pos.x(), pos.z());
		land.progress++;
		land.setDirty();
		if (land.done()) {
			MinecraftMode.LOGGER.info("The homestead plains are ready ({} chunks)", chunks().size());
		}
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
