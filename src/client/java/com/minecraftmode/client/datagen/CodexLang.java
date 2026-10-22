package com.minecraftmode.client.datagen;

import com.minecraftmode.craft.Crops;
import com.minecraftmode.craft.Fishing;
import com.minecraftmode.economy.ShopOffers;
import com.minecraftmode.progress.Codex;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;

/** The monster and item tabs of the codex (0.4.2): places, boss sections, item filters and where items come from. */
final class CodexLang {
	static void add(final TranslationBuilder b, final boolean ko) {
		String c = "screen.minecraft_mode.codex.";
		b.add(c + "tab.monsters", ko ? "몬스터" : "Monsters");
		b.add(c + "tab.items", ko ? "아이템" : "Items");
		b.add(c + "lair_found", ko ? "발견함 · 아직 정복하지 않음" : "Found · not conquered yet");
		b.add(c + "lair_lord", ko ? "군주: %s (Lv %s-%s)" : "Lord: %s (Lv %s-%s)");

		// monsters
		b.add(c + "monsters", ko ? "일반 몬스터: %s/%s종 처치" : "Common monsters: %s/%s kinds defeated");
		b.add(c + "monster_kills", ko ? "처치 %s회" : "Defeated %s times");
		b.add(c + "next_milestone", ko ? "다음 테두리: %s회" : "Next frame at %s");
		b.add(c + "all_milestones", ko ? "금 테두리 달성" : "Gold frame reached");
		b.add(c + "raid_bosses", ko ? "레이드 보스: %s/%s 처치" : "Raid bosses: %s/%s defeated");
		b.add(c + "raid_where", ko ? "성채의 레이드 원수에게서 입장" : "Enter through the raid marshal in the keep");
		b.add(c + "dungeon_bosses", ko ? "던전 보스: %s/%s 처치" : "Dungeon bosses: %s/%s defeated");
		b.add(c + "dungeon_boss", ko ? "보스: %s" : "Boss: %s");
		b.add(c + "min_level", ko ? "Lv %s부터" : "From Lv %s");
		b.add(c + "dungeon_where", ko ? "성채 안뜰의 던전 관리인에게서 입장" : "Enter through the dungeon warden in the keep courtyard");
		b.add(c + "best_keystone", ko ? "최고 시간 내 쐐기: +%s" : "Best timed keystone: +%s");
		b.add(c + "titans", ko ? "거신 처치: %s회" : "Titans defeated: %s");
		b.add(c + "titans_hint", ko
			? "거신은 사흘 중 이틀, 해 질 녘에 Lv %s 이상 직업이 있는 모험가 근처에 깨어나는 거대한 네임드입니다. 셋째 날에는 침공이 옵니다."
			: "Titans are giant named monsters that rise at dusk two days out of three near classed adventurers of Lv %s and up. The third day brings an invasion.");
		String[][] places = {
			{"night", "밤·어두운 곳 (오버월드)", "Overworld, at night or in the dark"},
			{"variants", "사막·설원·늪·창백한 정원의 변종", "Desert, snow, swamp and pale garden variants"},
			{"caves", "동굴·폐광·시련의 회당·요새", "Caves, mineshafts, trial chambers, strongholds"},
			{"water", "바다·바다 유적", "Oceans and ocean monuments"},
			{"nether", "네더", "The Nether"},
			{"end", "엔드 도시", "End cities"},
			{"raiders", "약탈자 전초기지·삼림 대저택·습격", "Pillager outposts, woodland mansions, raids"},
			{"deep_dark", "깊은 어둠 (고대 도시)", "The Deep Dark (ancient cities)"},
			{"boss", "보스 (직접 소환·엔드)", "Bosses (summoned, the End)"}
		};
		for (String[] p : places) {
			b.add(c + "place." + p[0], ko ? p[1] : p[2]);
		}

		// items
		String i = c + "items.";
		b.add(i + "all", ko ? "전체" : "All");
		b.add(Codex.Category.WEAPON.key(), ko ? "무기" : "Weapons");
		b.add(Codex.Category.ARMOR.key(), ko ? "방어구" : "Armor");
		b.add(Codex.Category.CONSUMABLE.key(), ko ? "소모품" : "Consum.");
		b.add(Codex.Category.MATERIAL.key(), ko ? "재료·기타" : "Other");
		b.add(Codex.Category.ACCESSORY.key(), ko ? "장신구" : "Jewelry");
		b.add(i + "any_class", ko ? "직업 전체" : "Any class");
		b.add(i + "any_level", ko ? "Lv 전체" : "Any Lv");
		b.add(i + "search", ko ? "이름 검색" : "Search name");
		b.add(i + "count", ko ? "획득 %s / %s" : "Found %s / %s");
		b.add(i + "none", ko ? "조건에 맞는 아이템이 없습니다." : "No items match.");
		b.add(i + "gear", ko ? "%s · Lv %s" : "%s · Lv %s");
		b.add(i + "found", ko ? "✔ 획득함" : "✔ Found");
		b.add(i + "missing", ko ? "아직 얻지 못함" : "Not found yet");
		b.add(i + "sources", ko ? "얻는 곳" : "Where to get it");

		// sources
		String s = c + "source.";
		b.add(s + "shop", ko ? "상점: %s" : "Shop: %s");
		b.add(s + "craft", ko ? "제작: %s Lv %s" : "Crafting: %s Lv %s");
		b.add(s + "guild", ko ? "모험가 길드 상점 (Lv %s 구간, 직업이 맞으면)" : "Adventurers' Guild shop (Lv %s bracket, for its class)");
		b.add(s + "drops", ko ? "네임드·소굴·레이드·던전 드롭 (Lv %s 구간)" : "Named, lair, raid and dungeon drops (Lv %s bracket)");
		b.add(s + "evolve", ko ? "대장장이 진화 (아래 구간 장비 + 진화의 에테르)" : "Blacksmith evolution (lower gear + Evolution Ether)");
		b.add(s + "raid", ko ? "레이드 보상" : "Raid rewards");
		b.add(s + "raid_boss", ko ? "레이드 전용: %s 처치 분배 (Lv %s)" : "Raid only: %s's loot (Lv %s)");
		b.add(s + "raid_top", ko ? "영웅·악몽 난이도: 모든 레이드 보스 (3티어)" : "Heroic and Nightmare: every raid boss (tier 3)");
		b.add(s + "adventure", ko ? "모험 중 획득" : "Found while adventuring");
		String[][] known = {
			{"essence", "몬스터 처치·광석 채굴", "Monster kills and mining ores"},
			{"condensed_essence", "정수 9개 합치기, 레이드·던전·현상금 보상", "Combine 9 essence; raid, dungeon and bounty rewards"},
			{"evolution_ether", "네임드 처치(반드시), 소굴 보물, 레이드·던전·현상금·거신·침공 보상", "Named kills (always), lair treasure, raid, dungeon, bounty, titan and invasion rewards"},
			{"enhancement_stone", "소굴 군주, 레이드, 던전, 거신·침공, 현상금 공적, 주민 의뢰", "Lair lords, raids, dungeons, titans and invasions, bounty merit, town quests"},
			{"protection_scroll", "소굴 군주(10%), 레이드(난이도별 확률), 현상금 공적, 광부 듀건의 의뢰(한 번)", "Lair lords (10%), raids (chance by difficulty), bounty merit, Old Miner Dugan's quest (once)"},
			{"awakening_crystal", "쐐기 던전 +5 이상 보상, 모험가 로완의 의뢰(한 번)", "Keystone dungeons at +5 and up, Retired Adventurer Rowan's quest (once)"},
			{"accessory_inherit_1", "2티어 레이드(네프레케트·크라켄·이그니스) 분배 품목 (확률, 경매·주사위)", "Tier 2 raid loot: Nefrekhet, Kraken, Ignis (chance, auction or dice)"},
			{"accessory_inherit_2", "3티어 레이드(라이진·말라카르·에테릭스) 분배 품목 (확률, 경매·주사위)", "Tier 3 raid loot: Raijin, Malachar, Aethryx (chance, auction or dice)"},
			{"accessory_inherit_3", "스쿠나 레이드 분배 품목 (확률, 경매·주사위), 저주 유물 교환소 (손가락 " + ShopOffers.RELIC_TICKET_FINGERS + "개)",
				"Sukuna's raid loot (chance, auction or dice), the cursed relic exchange (" + ShopOffers.RELIC_TICKET_FINGERS + " fingers)"},
			{"sukuna_finger", "스쿠나 레이드 클리어 보상 (일반 1개, 영웅 2개, 악몽 3개)", "Sukuna raid clears (1 on Normal, 2 on Heroic, 3 on Nightmare)"},
			{"accessory_orb", "영웅·악몽 레이드 클리어 보상 (영웅 1개, 악몽 2개)", "Heroic and Nightmare raid clears (1 on Heroic, 2 on Nightmare)"},
			{"titan_shard", "거신 처치", "Titan kills"},
			{"lair_map", "소굴 정복(주기마다 소굴마다 1장), 메인 스토리(소굴 장, 2장), 현상금 공적", "Every lair clear (one per lair each cycle), the main story (lair chapter, 2), bounty merit"},
			{"dungeon_keystone", "던전 클리어·주기마다 던전 관리인", "Dungeon clears and the warden each cycle"},
			{"golem_core", "미스릴 골렘", "Mythril golems"},
			{"raw_mythril", "미스릴 광석 (깊은 곳)", "Mythril ore (deep)"},
			{"raw_aluminum", "알루미늄 광석", "Aluminum ore"},
			{"gear_bag", "보급관 브람 (처음 1회)", "Quartermaster Bram (once)"},
			{"supply_bag", "보급관 브람 (처음 1회)", "Quartermaster Bram (once)"},
			{"ore_bag", "보급관 브람 (처음 1회)", "Quartermaster Bram (once)"},
			{"land_deed", "현상금 공적 상점 (살 때마다 비싸짐)", "Bounty merit shop (dearer with every deed)"},
			{"sunleaf", "풀·꽃 베기 (따뜻한 곳, 낮)", "Cutting grass and flowers (warm places, by day)"},
			{"moonpetal", "꽃 베기 (밤)", "Cutting flowers at night"},
			{"frostroot", "풀·꽃 베기 (눈 내리는 추운 곳)", "Cutting grass and flowers where it snows"},
			{"glowcap", "버섯·발광 이끼, 깊은 지하의 풀", "Mushrooms, glow lichen, grass deep underground"},
			{"emberbloom", "네더의 뿌리·균·덩굴", "Nether roots, fungi and vines"},
			{"voidcap", "후렴초 (엔드)", "Chorus plants (the End)"}
		};
		for (String[] k : known) {
			b.add(s + k[0], ko ? k[1] : k[2]);
		}
		farm(b, ko, s);
	}

	/** Where crops, seeds and fish come from. */
	private static void farm(final TranslationBuilder b, final boolean ko, final String s) {
		int lv = Crops.ADVANCED_LEVEL;
		for (Crops.CropDef def : Crops.all()) {
			b.add(s + def.id(), ko ? "농사: " + (def.plantsItself() ? "심어서 수확" : "씨앗을 심어 수확") : "Farming: " + (def.plantsItself() ? "plant it and harvest" : "plant the seeds and harvest"));
			if (def.advanced()) {
				b.add(s + def.id() + "_seeds", ko ? "풀 베기, 씨앗 탐색 괭이로 기본 작물 수확 (농사 Lv " + lv + "부터)"
					: "Cutting grass, harvesting basic crops with Seed Finder (Farming level " + lv + "+)");
			} else if (Crops.seed(def) != null) {
				b.add(s + def.id() + "_seeds", ko ? "풀 베기" : "Cutting grass");
			}
		}
		for (Fishing.FishDef def : Fishing.all()) {
			String level = def.minLevel() > 1 ? (ko ? ", 낚시 Lv " + def.minLevel() + "부터" : ", Fishing level " + def.minLevel() + "+") : "";
			b.add(s + def.id(), ko ? "낚시: " + habitat(def.habitat(), true) + level : "Fishing: " + habitat(def.habitat(), false) + level);
		}
	}

	static String habitat(final Fishing.Habitat habitat, final boolean ko) {
		return switch (habitat) {
			case ANY -> ko ? "어디서나" : "anywhere";
			case OCEAN -> ko ? "바다" : "the sea";
			case WARM_OCEAN -> ko ? "따뜻한 바다" : "warm seas";
			case RIVER -> ko ? "강" : "rivers";
			case COLD -> ko ? "눈 내리는 추운 물" : "cold, snowy water";
			case SWAMP -> ko ? "늪·맹그로브 늪" : "swamps and mangroves";
		};
	}

	private CodexLang() {
	}
}
