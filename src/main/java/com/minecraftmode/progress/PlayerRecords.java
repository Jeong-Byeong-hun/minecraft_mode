package com.minecraftmode.progress;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Everything a player has done that the codex, achievements, lockouts and collection bonuses read:
 * named monster kills, lair clears, raid clears (key {@code boss:difficulty}), the cycle each raid was
 * last rewarded, bounties finished, items sold on the market, the best enhancement reached, unlocked
 * achievements, the chosen title, the highest awakening and the invasions repelled. Kept through death, synced to the owner.
 */
public record PlayerRecords(
	Map<String, Integer> namedKills, Map<String, Integer> lairClears, Map<String, Integer> raidClears, Map<String, Long> raidLocks, int bountiesDone,
	int marketSold, int maxEnhance, List<String> achievements, String title, int maxAwaken, int invasions
) {
	public static final PlayerRecords DEFAULT = new PlayerRecords(Map.of(), Map.of(), Map.of(), Map.of(), 0, 0, 0, List.of(), "", 0, 0);

	public static final Codec<PlayerRecords> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("named_kills", Map.of()).forGetter(PlayerRecords::namedKills),
		Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("lair_clears", Map.of()).forGetter(PlayerRecords::lairClears),
		Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("raid_clears", Map.of()).forGetter(PlayerRecords::raidClears),
		Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("raid_locks", Map.of()).forGetter(PlayerRecords::raidLocks),
		Codec.INT.optionalFieldOf("bounties_done", 0).forGetter(PlayerRecords::bountiesDone),
		Codec.INT.optionalFieldOf("market_sold", 0).forGetter(PlayerRecords::marketSold),
		Codec.INT.optionalFieldOf("max_enhance", 0).forGetter(PlayerRecords::maxEnhance),
		Codec.STRING.listOf().optionalFieldOf("achievements", List.of()).forGetter(PlayerRecords::achievements),
		Codec.STRING.optionalFieldOf("title", "").forGetter(PlayerRecords::title),
		Codec.INT.optionalFieldOf("max_awaken", 0).forGetter(PlayerRecords::maxAwaken),
		Codec.INT.optionalFieldOf("invasions", 0).forGetter(PlayerRecords::invasions)
	).apply(i, PlayerRecords::new));

	public static final StreamCodec<ByteBuf, PlayerRecords> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

	public PlayerRecords {
		namedKills = Map.copyOf(namedKills);
		lairClears = Map.copyOf(lairClears);
		raidClears = Map.copyOf(raidClears);
		raidLocks = Map.copyOf(raidLocks);
		achievements = List.copyOf(achievements);
	}

	public static String raidKey(final String boss, final String difficulty) {
		return boss + ":" + difficulty;
	}

	public int kills(final String named) {
		return this.namedKills.getOrDefault(named, 0);
	}

	public int lairClears(final String lair) {
		return this.lairClears.getOrDefault(lair, 0);
	}

	public int raidClears(final String key) {
		return this.raidClears.getOrDefault(key, 0);
	}

	/** True when this raid key already paid out in {@code cycle}. */
	public boolean raidLocked(final String key, final long cycle) {
		return this.raidLocks.getOrDefault(key, -1L) == cycle;
	}

	public boolean has(final String achievement) {
		return this.achievements.contains(achievement);
	}

	public int totalNamedKills() {
		return this.namedKills.values().stream().mapToInt(Integer::intValue).sum();
	}

	public PlayerRecords withNamedKill(final String named) {
		Map<String, Integer> map = new HashMap<>(this.namedKills);
		map.merge(named, 1, Integer::sum);
		return new PlayerRecords(map, this.lairClears, this.raidClears, this.raidLocks, this.bountiesDone, this.marketSold, this.maxEnhance, this.achievements,
			this.title, this.maxAwaken, this.invasions);
	}

	public PlayerRecords withLairClear(final String lair) {
		Map<String, Integer> map = new HashMap<>(this.lairClears);
		map.merge(lair, 1, Integer::sum);
		return new PlayerRecords(this.namedKills, map, this.raidClears, this.raidLocks, this.bountiesDone, this.marketSold, this.maxEnhance, this.achievements,
			this.title, this.maxAwaken, this.invasions);
	}

	public PlayerRecords withRaidClear(final String key, final long cycle) {
		Map<String, Integer> clears = new HashMap<>(this.raidClears);
		clears.merge(key, 1, Integer::sum);
		Map<String, Long> locks = new HashMap<>(this.raidLocks);
		locks.put(key, cycle);
		return new PlayerRecords(this.namedKills, this.lairClears, clears, locks, this.bountiesDone, this.marketSold, this.maxEnhance, this.achievements,
			this.title, this.maxAwaken, this.invasions);
	}

	public PlayerRecords withBountyDone() {
		return new PlayerRecords(this.namedKills, this.lairClears, this.raidClears, this.raidLocks, this.bountiesDone + 1, this.marketSold, this.maxEnhance,
			this.achievements, this.title, this.maxAwaken, this.invasions);
	}

	public PlayerRecords withMarketSale() {
		return new PlayerRecords(this.namedKills, this.lairClears, this.raidClears, this.raidLocks, this.bountiesDone, this.marketSold + 1, this.maxEnhance,
			this.achievements, this.title, this.maxAwaken, this.invasions);
	}

	public PlayerRecords withEnhance(final int level) {
		return new PlayerRecords(this.namedKills, this.lairClears, this.raidClears, this.raidLocks, this.bountiesDone, this.marketSold,
			Math.max(this.maxEnhance, level), this.achievements, this.title, this.maxAwaken, this.invasions);
	}

	public PlayerRecords withAchievement(final String id) {
		if (this.has(id)) {
			return this;
		}
		List<String> list = new ArrayList<>(this.achievements);
		list.add(id);
		return new PlayerRecords(this.namedKills, this.lairClears, this.raidClears, this.raidLocks, this.bountiesDone, this.marketSold, this.maxEnhance, list,
			this.title, this.maxAwaken, this.invasions);
	}

	public PlayerRecords withAwaken(final int awaken) {
		return new PlayerRecords(this.namedKills, this.lairClears, this.raidClears, this.raidLocks, this.bountiesDone, this.marketSold, this.maxEnhance,
			this.achievements, this.title, Math.max(this.maxAwaken, awaken), this.invasions);
	}

	public PlayerRecords withInvasion() {
		return new PlayerRecords(this.namedKills, this.lairClears, this.raidClears, this.raidLocks, this.bountiesDone, this.marketSold, this.maxEnhance,
			this.achievements, this.title, this.maxAwaken, this.invasions + 1);
	}

	public PlayerRecords withTitle(final String title) {
		return new PlayerRecords(this.namedKills, this.lairClears, this.raidClears, this.raidLocks, this.bountiesDone, this.marketSold, this.maxEnhance,
			this.achievements, title, this.maxAwaken, this.invasions);
	}
}
