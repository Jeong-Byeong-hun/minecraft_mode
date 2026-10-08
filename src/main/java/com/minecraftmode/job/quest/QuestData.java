package com.minecraftmode.job.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The player's active trial (empty id = none) and the kill count of each of its goals.
 * Saved with the player, kept on death, synced to that player for the tracker.
 */
public record QuestData(String active, List<Integer> progress, boolean visitedCity) {
	public static final QuestData DEFAULT = new QuestData("", List.of(), false);

	public static final Codec<QuestData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.optionalFieldOf("active", "").forGetter(QuestData::active),
		Codec.INT.listOf().optionalFieldOf("progress", List.of()).forGetter(QuestData::progress),
		Codec.BOOL.optionalFieldOf("visited_city", false).forGetter(QuestData::visitedCity)
	).apply(i, QuestData::new));

	public static final StreamCodec<ByteBuf, QuestData> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, QuestData::active,
		ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), QuestData::progress,
		ByteBufCodecs.BOOL, QuestData::visitedCity,
		QuestData::new
	);

	public QuestData {
		progress = List.copyOf(progress);
	}

	public boolean hasQuest() {
		return !this.active.isEmpty();
	}

	public int progress(final int goal) {
		return goal < this.progress.size() ? this.progress.get(goal) : 0;
	}

	public QuestData start(final QuestDef quest) {
		return new QuestData(quest.id(), Collections.nCopies(quest.kills().size(), 0), this.visitedCity);
	}

	public QuestData cleared() {
		return new QuestData("", List.of(), this.visitedCity);
	}

	public QuestData withProgress(final int goal, final int value) {
		List<Integer> list = new ArrayList<>(this.progress);
		while (list.size() <= goal) {
			list.add(0);
		}
		list.set(goal, value);
		return new QuestData(this.active, list, this.visitedCity);
	}

	public QuestData visited() {
		return new QuestData(this.active, this.progress, true);
	}
}
