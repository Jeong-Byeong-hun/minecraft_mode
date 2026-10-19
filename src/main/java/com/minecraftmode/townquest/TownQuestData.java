package com.minecraftmode.townquest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A player's town quests ({@link TownQuests}): the accepted ones with their progress, and the real day ({@code RealReset.day}) each
 * quest was last handed in. Daily quests open again on a later day; one-time quests stay done. Per character, kept on death, synced
 * to the owner. Keys are quest ids (saved keys).
 */
public record TownQuestData(Map<String, Integer> active, Map<String, Long> done) {
	public static final TownQuestData DEFAULT = new TownQuestData(Map.of(), Map.of());

	public static final Codec<TownQuestData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("active", Map.of()).forGetter(TownQuestData::active),
		Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("done", Map.of()).forGetter(TownQuestData::done)
	).apply(i, TownQuestData::new));

	public static final StreamCodec<ByteBuf, TownQuestData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

	public TownQuestData {
		active = Map.copyOf(active);
		done = Map.copyOf(done);
	}

	public boolean isActive(final String id) {
		return this.active.containsKey(id);
	}

	public int progress(final String id) {
		return this.active.getOrDefault(id, 0);
	}

	public TownQuestData withProgress(final String id, final int value) {
		Map<String, Integer> map = new HashMap<>(this.active);
		map.put(id, value);
		return new TownQuestData(map, this.done);
	}

	public TownQuestData without(final String id) {
		Map<String, Integer> map = new HashMap<>(this.active);
		map.remove(id);
		return new TownQuestData(map, this.done);
	}

	/** Handed in on {@code day}: no longer active, done that day. */
	public TownQuestData finished(final String id, final long day) {
		Map<String, Integer> map = new HashMap<>(this.active);
		map.remove(id);
		Map<String, Long> doneMap = new HashMap<>(this.done);
		doneMap.put(id, day);
		return new TownQuestData(map, doneMap);
	}
}
