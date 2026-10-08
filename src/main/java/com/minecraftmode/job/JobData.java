package com.minecraftmode.job;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A player's class progress, saved with the player and synced to that player only. Immutable:
 * every change goes through {@code setAttached}, which is what triggers the sync.
 *
 * @param tier      0 before the first advancement, then 1..4
 * @param exp       progress inside the current level
 * @param mana      current MP
 * @param cooldowns skill id -> game time at which the skill is ready again
 */
public record JobData(JobClass job, int tier, int level, int exp, int mana, Map<String, Long> cooldowns) {
	public static final JobData DEFAULT = new JobData(JobClass.NONE, 0, 1, 0, JobProgression.BASE_MANA, Map.of());

	public static final Codec<JobData> CODEC = RecordCodecBuilder.create(i -> i.group(
		JobClass.CODEC.optionalFieldOf("job", JobClass.NONE).forGetter(JobData::job),
		Codec.INT.optionalFieldOf("tier", 0).forGetter(JobData::tier),
		Codec.INT.optionalFieldOf("level", 1).forGetter(JobData::level),
		Codec.INT.optionalFieldOf("exp", 0).forGetter(JobData::exp),
		Codec.INT.optionalFieldOf("mana", JobProgression.BASE_MANA).forGetter(JobData::mana),
		Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("cooldowns", Map.of()).forGetter(JobData::cooldowns)
	).apply(i, JobData::new));

	public static final StreamCodec<ByteBuf, JobData> STREAM_CODEC = StreamCodec.composite(
		JobClass.STREAM_CODEC, JobData::job,
		ByteBufCodecs.VAR_INT, JobData::tier,
		ByteBufCodecs.VAR_INT, JobData::level,
		ByteBufCodecs.VAR_INT, JobData::exp,
		ByteBufCodecs.VAR_INT, JobData::mana,
		ByteBufCodecs.<ByteBuf, String, Long, Map<String, Long>>map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG), JobData::cooldowns,
		JobData::new
	);

	public JobData {
		cooldowns = Map.copyOf(cooldowns);
	}

	public JobData withJob(final JobClass job, final int tier) {
		return new JobData(job, tier, this.level, this.exp, this.mana, this.cooldowns);
	}

	public JobData withProgress(final int level, final int exp) {
		return new JobData(this.job, this.tier, level, exp, this.mana, this.cooldowns);
	}

	public JobData withMana(final int mana) {
		return new JobData(this.job, this.tier, this.level, this.exp, mana, this.cooldowns);
	}

	public JobData withCooldown(final String skill, final long readyAt) {
		Map<String, Long> map = new HashMap<>(this.cooldowns);
		map.put(skill, readyAt);
		return new JobData(this.job, this.tier, this.level, this.exp, this.mana, map);
	}

	public JobData withCooldowns(final Map<String, Long> cooldowns) {
		return new JobData(this.job, this.tier, this.level, this.exp, this.mana, cooldowns);
	}

	/** Drops cooldowns that are already over so the map does not grow forever. */
	public JobData pruneCooldowns(final long now) {
		if (this.cooldowns.values().stream().noneMatch(t -> t <= now)) {
			return this;
		}
		Map<String, Long> map = new HashMap<>(this.cooldowns);
		map.values().removeIf(t -> t <= now);
		return this.withCooldowns(map);
	}

	public long readyAt(final String skill) {
		return this.cooldowns.getOrDefault(skill, 0L);
	}

	public boolean hasClass() {
		return this.job != JobClass.NONE && this.tier > 0;
	}
}
