package com.minecraftmode.raid;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.progress.PlayerRecords;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The fastest clears of each boss and difficulty (top {@link #KEEP} parties, fight time in ticks), kept
 * for the whole world and shown at the raid marshal.
 */
public class RaidRecordsData extends SavedData {
	public static final int KEEP = 10;

	/** One clear: the party (leader first), the fight time and the day it happened. */
	public record Entry(List<String> names, int ticks, long day) {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.listOf().fieldOf("names").forGetter(Entry::names),
			Codec.INT.fieldOf("ticks").forGetter(Entry::ticks),
			Codec.LONG.optionalFieldOf("day", 0L).forGetter(Entry::day)
		).apply(i, Entry::new));
	}

	public static final Codec<Map<String, List<Entry>>> TABLE_CODEC = Codec.unboundedMap(Codec.STRING, Entry.CODEC.listOf());

	private static final Codec<RaidRecordsData> CODEC = RecordCodecBuilder.create(i -> i.group(
		TABLE_CODEC.optionalFieldOf("records", Map.of()).forGetter(d -> d.records)
	).apply(i, RaidRecordsData::new));

	public static final SavedDataType<RaidRecordsData> TYPE = new SavedDataType<>(MinecraftMode.id("raid_records"), RaidRecordsData::new, CODEC, null);

	private final Map<String, List<Entry>> records;

	public RaidRecordsData() {
		this(Map.of());
	}

	private RaidRecordsData(final Map<String, List<Entry>> records) {
		this.records = new HashMap<>();
		records.forEach((k, v) -> this.records.put(k, new ArrayList<>(v)));
	}

	public static RaidRecordsData get(final MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	/** Adds a clear; returns its place (1-based) or 0 when it did not make the table. */
	public int submit(final BossDef boss, final RaidDifficulty difficulty, final List<String> names, final int ticks, final long day) {
		String key = PlayerRecords.raidKey(boss.id(), difficulty.id());
		List<Entry> list = this.records.computeIfAbsent(key, k -> new ArrayList<>());
		Entry entry = new Entry(List.copyOf(names), ticks, day);
		list.add(entry);
		list.sort(Comparator.comparingInt(Entry::ticks));
		while (list.size() > KEEP) {
			list.removeLast();
		}
		this.setDirty();
		int place = list.indexOf(entry);
		return place < 0 ? 0 : place + 1;
	}

	public List<Entry> top(final String key, final int count) {
		List<Entry> list = this.records.getOrDefault(key, List.of());
		return List.copyOf(list.subList(0, Math.min(count, list.size())));
	}

	/** The best {@code count} of every table (for the marshal's screen). */
	public Map<String, List<Entry>> snapshot(final int count) {
		Map<String, List<Entry>> out = new HashMap<>();
		this.records.forEach((k, v) -> out.put(k, List.copyOf(v.subList(0, Math.min(count, v.size())))));
		return out;
	}
}
