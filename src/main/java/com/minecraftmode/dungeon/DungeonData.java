package com.minecraftmode.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A player's dungeon records (attachment): clears per dungeon, best keystone level beaten in time per dungeon, and the reset cycle
 * of the last free keystone a keyless clear handed out ({@code -1}: never), so keys stashed in a chest do not earn a new one every run.
 */
public record DungeonData(Map<String, Integer> clears, Map<String, Integer> best, long freeKeystoneCycle) {
	public static final DungeonData DEFAULT = new DungeonData(Map.of(), Map.of(), -1L);
	public static final Codec<DungeonData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("clears", Map.of()).forGetter(DungeonData::clears),
		Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("best", Map.of()).forGetter(DungeonData::best),
		Codec.LONG.optionalFieldOf("free_keystone_cycle", -1L).forGetter(DungeonData::freeKeystoneCycle)
	).apply(i, DungeonData::new));
	public static final StreamCodec<ByteBuf, DungeonData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

	public DungeonData {
		clears = Map.copyOf(clears);
		best = Map.copyOf(best);
	}

	public int clears(final String dungeon) {
		return this.clears.getOrDefault(dungeon, 0);
	}

	public int totalClears() {
		return this.clears.values().stream().mapToInt(Integer::intValue).sum();
	}

	public int best(final String dungeon) {
		return this.best.getOrDefault(dungeon, 0);
	}

	/** Highest keystone beaten in time in any dungeon. */
	public int bestOverall() {
		return this.best.values().stream().mapToInt(Integer::intValue).max().orElse(0);
	}

	public DungeonData withClear(final String dungeon, final int timedLevel) {
		Map<String, Integer> c = new HashMap<>(this.clears);
		c.merge(dungeon, 1, Integer::sum);
		Map<String, Integer> b = new HashMap<>(this.best);
		if (timedLevel > b.getOrDefault(dungeon, 0)) {
			b.put(dungeon, timedLevel);
		}
		return new DungeonData(c, b, this.freeKeystoneCycle);
	}

	public DungeonData withFreeKeystone(final long cycle) {
		return new DungeonData(this.clears, this.best, cycle);
	}
}
