package com.minecraftmode.progress;

import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.job.JobData;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.raid.RaidDifficulty;
import com.minecraftmode.worldgen.lair.LairDef;
import com.minecraftmode.worldgen.lair.NamedLairs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import org.jspecify.annotations.Nullable;

/**
 * The achievements: hunting, lairs, raids, bounties, enhancement, the market, levels and wealth. Each
 * has a counter and a goal, gives merit when reached, and some give a title. Ids are saved keys.
 */
public final class Achievements {
	/** What an achievement counts, read from a player's state. */
	public record State(PlayerRecords records, JobData job, int wallet, int talentPoints) {
	}

	/**
	 * @param titleEn  title it unlocks ("" for none)
	 */
	public record Achievement(String id, String category, String en, String ko, String descEn, String descKo, int goal, int merit, String titleEn,
		String titleKo, ToIntFunction<State> counter) {
		public int progress(final State state) {
			return Math.min(this.goal, this.counter.applyAsInt(state));
		}

		public boolean done(final State state) {
			return this.counter.applyAsInt(state) >= this.goal;
		}

		public boolean hasTitle() {
			return !this.titleEn.isEmpty();
		}

		public String nameKey() {
			return "achievement.minecraft_mode." + this.id;
		}

		public String descKey() {
			return this.nameKey() + ".desc";
		}

		public String titleKey() {
			return "title.minecraft_mode." + this.id;
		}
	}

	private static final Map<String, Achievement> ALL = new LinkedHashMap<>();

	static {
		// hunting
		add("first_named", "hunt", "First Hunt", "첫 사냥", "Defeat a named monster.", "네임드 몬스터를 처치하세요.", 1, 10, "", "",
			s -> s.records().totalNamedKills());
		add("named_50", "hunt", "Named Hunter", "네임드 사냥꾼", "Defeat 50 named monsters.", "네임드 몬스터 50마리를 처치하세요.", 50, 40, "Named Hunter", "네임드 사냥꾼",
			s -> s.records().totalNamedKills());
		add("named_250", "hunt", "Slayer", "학살자", "Defeat 250 named monsters.", "네임드 몬스터 250마리를 처치하세요.", 250, 100, "Slayer", "학살자",
			s -> s.records().totalNamedKills());
		add("named_all", "hunt", "Field Guide", "도감 수집가", "Defeat every kind of named monster.", "모든 종류의 네임드 몬스터를 처치하세요.", NamedMobs.all().size(), 80,
			"Collector", "도감 수집가", s -> kinds(s.records(), 1));
		add("named_master", "hunt", "Monster Scholar", "마물 학자", "Defeat every kind of named monster 10 times.", "모든 네임드 몬스터를 종류마다 10번씩 처치하세요.",
			NamedMobs.all().size(), 200, "Monster Scholar", "마물 학자", s -> kinds(s.records(), 10));
		// lairs
		add("first_lair", "lair", "Treasure Seeker", "보물 탐색자", "Clear a named lair.", "네임드 소굴을 정복하세요.", 1, 10, "", "",
			s -> lairTotal(s.records()));
		add("lair_10", "lair", "Lair Raider", "소굴 습격자", "Clear lairs 10 times.", "소굴을 10번 정복하세요.", 10, 40, "", "", s -> lairTotal(s.records()));
		add("lair_50", "lair", "Treasure Hunter", "보물 사냥꾼", "Clear lairs 50 times.", "소굴을 50번 정복하세요.", 50, 120, "Treasure Hunter", "보물 사냥꾼",
			s -> lairTotal(s.records()));
		add("lair_all", "lair", "Lair Conqueror", "소굴 정복자", "Clear every kind of lair.", "모든 종류의 소굴을 정복하세요.", NamedLairs.all().size(), 150,
			"Lair Conqueror", "소굴 정복자", s -> lairKinds(s.records()));
		// raids
		add("raid_first", "raid", "Into the Breach", "첫 토벌", "Clear a raid.", "레이드를 클리어하세요.", 1, 20, "", "",
			s -> s.records().raidClears().values().stream().mapToInt(Integer::intValue).sum());
		add("raid_all_normal", "raid", "Raider", "토벌대원", "Clear every raid boss on Normal.", "모든 레이드 보스를 일반 난이도로 클리어하세요.", RaidBosses.all().size(), 60,
			"Raider", "토벌대원", s -> bosses(s.records(), RaidDifficulty.NORMAL));
		add("raid_heroic", "raid", "Heroic", "영웅의 길", "Clear a raid on Heroic.", "영웅 난이도 레이드를 클리어하세요.", 1, 40, "", "",
			s -> bosses(s.records(), RaidDifficulty.HEROIC));
		add("raid_all_heroic", "raid", "Hero", "영웅", "Clear every raid boss on Heroic.", "모든 레이드 보스를 영웅 난이도로 클리어하세요.", RaidBosses.all().size(), 150,
			"Hero", "영웅", s -> bosses(s.records(), RaidDifficulty.HEROIC));
		add("raid_nightmare", "raid", "Nightmare", "악몽", "Clear a raid on Nightmare.", "악몽 난이도 레이드를 클리어하세요.", 1, 80, "", "",
			s -> bosses(s.records(), RaidDifficulty.NIGHTMARE));
		add("raid_all_nightmare", "raid", "Beyond the Nightmare", "악몽을 넘어선 자", "Clear every raid boss on Nightmare.",
			"모든 레이드 보스를 악몽 난이도로 클리어하세요.", RaidBosses.all().size(), 300, "Nightmare Breaker", "악몽을 넘어선 자",
			s -> bosses(s.records(), RaidDifficulty.NIGHTMARE));
		add("void_slayer", "raid", "Voidslayer", "공허를 벤 자", "Defeat Aethryx on Nightmare.", "악몽 난이도의 에테릭스를 처치하세요.", 1, 150, "Voidslayer", "공허를 벤 자",
			s -> s.records().raidClears(PlayerRecords.raidKey(RaidBosses.AETHRYX.id(), RaidDifficulty.NIGHTMARE.id())));
		// bounties
		add("bounty_1", "bounty", "Guild Member", "길드원", "Finish a bounty.", "의뢰를 하나 완료하세요.", 1, 5, "", "", s -> s.records().bountiesDone());
		add("bounty_30", "bounty", "Guild Worker", "길드의 일꾼", "Finish 30 bounties.", "의뢰를 30개 완료하세요.", 30, 50, "Guild Worker", "길드의 일꾼",
			s -> s.records().bountiesDone());
		add("bounty_100", "bounty", "Bounty King", "의뢰왕", "Finish 100 bounties.", "의뢰를 100개 완료하세요.", 100, 150, "Bounty King", "의뢰왕",
			s -> s.records().bountiesDone());
		// enhancement
		add("enhance_5", "enhance", "Apprentice Smith", "견습 대장장이", "Enhance gear to +5.", "장비를 +5까지 강화하세요.", 5, 10, "", "", s -> s.records().maxEnhance());
		add("enhance_10", "enhance", "Master Smith", "강화 장인", "Enhance gear to +10.", "장비를 +10까지 강화하세요.", 10, 60, "Master Smith", "강화 장인",
			s -> s.records().maxEnhance());
		add("enhance_15", "enhance", "Legend of +15", "+15의 전설", "Enhance gear to +15.", "장비를 +15까지 강화하세요.", 15, 200, "Legend of +15", "+15의 전설",
			s -> s.records().maxEnhance());
		// market
		add("market_1", "market", "First Sale", "첫 판매", "Sell an item on the market.", "거래소에서 물건을 판매하세요.", 1, 5, "", "", s -> s.records().marketSold());
		add("market_50", "market", "Merchant Prince", "거상", "Sell 50 items on the market.", "거래소에서 물건 50개를 판매하세요.", 50, 80, "Merchant Prince", "거상",
			s -> s.records().marketSold());
		// growth
		add("level_25", "growth", "Seasoned", "숙련된 모험가", "Reach level 25.", "레벨 25를 달성하세요.", 25, 10, "", "", s -> s.job().level());
		add("level_50", "growth", "Veteran", "베테랑", "Reach level 50.", "레벨 50을 달성하세요.", 50, 30, "", "", s -> s.job().level());
		add("level_100", "growth", "Pinnacle", "정점", "Reach level 100.", "레벨 100을 달성하세요.", 100, 150, "Pinnacle", "정점의 모험가", s -> s.job().level());
		add("tier_4", "growth", "Fourth Awakening", "4차 각성", "Reach the fourth tier of a class.", "직업 4차 전직을 달성하세요.", 4, 60, "Awakened", "각성자",
			s -> s.job().hasClass() ? s.job().tier() : 0);
		add("talents_30", "growth", "Specialist", "전문가", "Spend 30 talent points.", "특성 포인트 30개를 투자하세요.", 30, 40, "", "", s -> s.talentPoints());
		// wealth
		add("wallet_10g", "wealth", "Well Off", "넉넉한 지갑", "Hold 10 gold coins in your wallet.", "지갑에 금화 10개 이상을 모으세요.", 810, 20, "", "",
			s -> s.wallet());
		add("wallet_100g", "wealth", "Tycoon", "부호", "Hold 100 gold coins in your wallet.", "지갑에 금화 100개 이상을 모으세요.", 8100, 80, "Tycoon", "부호",
			s -> s.wallet());
	}

	private static void add(final String id, final String category, final String en, final String ko, final String descEn, final String descKo, final int goal,
		final int merit, final String titleEn, final String titleKo, final ToIntFunction<State> counter) {
		ALL.put(id, new Achievement(id, category, en, ko, descEn, descKo, goal, merit, titleEn, titleKo, counter));
	}

	private static int kinds(final PlayerRecords records, final int min) {
		int n = 0;
		for (NamedDef def : NamedMobs.all()) {
			if (records.kills(def.id()) >= min) {
				n++;
			}
		}
		return n;
	}

	private static int lairTotal(final PlayerRecords records) {
		return records.lairClears().values().stream().mapToInt(Integer::intValue).sum();
	}

	private static int lairKinds(final PlayerRecords records) {
		int n = 0;
		for (LairDef def : NamedLairs.all()) {
			if (records.lairClears(def.id()) > 0) {
				n++;
			}
		}
		return n;
	}

	private static int bosses(final PlayerRecords records, final RaidDifficulty difficulty) {
		int n = 0;
		for (BossDef def : RaidBosses.all()) {
			if (records.raidClears(PlayerRecords.raidKey(def.id(), difficulty.id())) > 0) {
				n++;
			}
		}
		return n;
	}

	public static List<Achievement> all() {
		return Collections.unmodifiableList(new ArrayList<>(ALL.values()));
	}

	public static @Nullable Achievement get(final String id) {
		return ALL.get(id);
	}

	/** Achievements that give a title and are unlocked in {@code records}. */
	public static List<Achievement> titles(final PlayerRecords records) {
		return ALL.values().stream().filter(a -> a.hasTitle() && records.has(a.id())).toList();
	}

	private Achievements() {
	}
}
