package com.minecraftmode.progress;

import com.minecraftmode.companion.Companions;
import com.minecraftmode.craft.Profession;
import com.minecraftmode.dungeon.Dungeons;
import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.Paragon;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.raid.RaidDifficulty;
import com.minecraftmode.story.Story;
import com.minecraftmode.worldgen.lair.LairDef;
import com.minecraftmode.worldgen.lair.NamedLairs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * The achievements: hunting, lairs, raids, bounties, enhancement, the market, levels and wealth. Each
 * has a counter and a goal, gives merit when reached, and some give a title. Ids are saved keys.
 */
public final class Achievements {
	/** What an achievement counts, read from a player's state. */
	public record State(PlayerRecords records, JobData job, int wallet, int talentPoints, Player player) {
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

	static {
		// added with dungeons, paragon, awakening, professions, companions, world bosses and the story (append only: ids are saved)
		add("dungeon_first", "dungeon", "Delver", "탐험가", "Clear a dungeon.", "던전을 클리어하세요.", 1, 15, "", "", s -> Dungeons.data(s.player()).totalClears());
		add("dungeon_25", "dungeon", "Dungeon Crawler", "던전 탐험가", "Clear dungeons 25 times.", "던전을 25번 클리어하세요.", 25, 60, "", "",
			s -> Dungeons.data(s.player()).totalClears());
		add("keystone_5", "dungeon", "Keyholder", "쐐기돌 소지자", "Time a +5 keystone.", "+5 쐐기돌을 시간 안에 돌파하세요.", 5, 40, "", "",
			s -> Dungeons.data(s.player()).bestOverall());
		add("keystone_10", "dungeon", "Keystone Master", "쐐기돌 달인", "Time a +10 keystone.", "+10 쐐기돌을 시간 안에 돌파하세요.", 10, 100, "Keystone Master", "쐐기돌 달인",
			s -> Dungeons.data(s.player()).bestOverall());
		add("keystone_15", "dungeon", "Keystone Legend", "쐐기돌의 전설", "Time a +15 keystone.", "+15 쐐기돌을 시간 안에 돌파하세요.", 15, 200, "Keystone Legend",
			"쐐기돌의 전설", s -> Dungeons.data(s.player()).bestOverall());
		add("paragon_10", "growth", "Beyond the Peak", "정점 너머", "Reach paragon level 10.", "초월 레벨 10을 달성하세요.", 10, 40, "", "",
			s -> Paragon.get(s.player()).level());
		add("paragon_50", "growth", "Transcendent", "초월자", "Reach paragon level 50.", "초월 레벨 50을 달성하세요.", 50, 120, "Transcendent", "초월자",
			s -> Paragon.get(s.player()).level());
		add("awaken_1", "enhance", "Awakener", "각성자", "Awaken a piece of gear.", "장비를 각성하세요.", 1, 40, "", "", s -> s.records().maxAwaken());
		add("awaken_5", "enhance", "Fully Awakened", "완전 각성", "Awaken a piece of gear to ✦5.", "장비를 ✦5까지 각성하세요.", 5, 150, "Fully Awakened", "완전 각성자",
			s -> s.records().maxAwaken());
		add("profession_10", "craft", "Apprentice", "견습생", "Reach level 10 in a profession.", "생활 기술 레벨 10을 달성하세요.", 10, 15, "", "",
			s -> bestProfession(s.player()));
		add("profession_50", "craft", "Grand Artisan", "대장인", "Master a profession (level 50).", "생활 기술 하나를 마스터하세요(레벨 50).", 50, 100, "Grand Artisan", "대장인",
			s -> bestProfession(s.player()));
		add("pets_all", "companion", "Beast Friend", "짐승의 벗", "Collect every pet.", "모든 펫을 모으세요.", Companions.pets().size(), 80, "Beast Friend", "짐승의 벗",
			s -> Companions.data(s.player()).pets().size());
		add("mounts_all", "companion", "Rider of Legends", "전설의 기수", "Collect every mount.", "모든 탈것을 모으세요.", Companions.mounts().size(), 80, "Rider of Legends",
			"전설의 기수", s -> Companions.data(s.player()).mounts().size());
		add("world_boss", "event", "Titan Hunter", "거신 사냥꾼", "Help defeat a world boss.", "월드 보스 처치에 참여하세요.", 1, 30, "", "",
			s -> Story.data(s.player()).worldBosses());
		add("world_boss_10", "event", "Titan Slayer", "거신 학살자", "Help defeat 10 world bosses.", "월드 보스 10마리 처치에 참여하세요.", 10, 100, "Titan Slayer", "거신 학살자",
			s -> Story.data(s.player()).worldBosses());
		add("invasion", "event", "Defender of Stormhold", "스톰홀드 수호자", "Help repel an invasion of the capital.", "수도 침공을 막아 내세요.", 1, 30, "", "",
			s -> s.records().invasions());
		add("story_done", "story", "Hero of Stormhold", "스톰홀드의 영웅", "Finish the main story.", "메인 스토리를 완료하세요.", Story.chapters().size(), 150,
			"Hero of Stormhold", "스톰홀드의 영웅", s -> Story.data(s.player()).chapter());
	}

	private static int bestProfession(final Player player) {
		int best = 0;
		for (Profession profession : Profession.values()) {
			best = Math.max(best, profession.level(player));
		}
		return best;
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
