package com.minecraftmode.client.datagen;

import com.minecraftmode.progress.RealReset;
import com.minecraftmode.townquest.TownQuests;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;

/** Town quests (0.5.36): every quest's title and the giver's words, the objective templates, the quest screen, messages and the guide topic. */
final class TownQuestLang {
	static void add(final TranslationBuilder b, final boolean ko) {
		for (TownQuests.Quest quest : TownQuests.all()) {
			b.add(quest.titleKey(), ko ? quest.titleKo() : quest.titleEn());
			b.add(quest.textKey(), ko ? quest.textKo() : quest.textEn());
		}
		// objectives: %1$s = how many, %2$s = the monster, item or profession
		String[][] goals = {
			{"kill", "Defeat %s %s", "%2$s %1$s마리 처치"},
			{"kill_named", "Defeat %s named monster(s)", "네임드 몬스터 %s마리 처치"},
			{"kill_lord", "Defeat %s lair lord(s)", "소굴의 군주 %s마리 처치"},
			{"deliver", "Bring %s %s", "%2$s %1$s개 가져오기"},
			{"mine_ore", "Mine %s ores", "광석 %s개 캐기"},
			{"fish", "Catch %s fish with a rod", "낚싯대로 물고기 %s마리 낚기"},
			{"harvest", "Harvest %s ripe crops grown from the mod's seeds", "모드 씨앗으로 키운 다 익은 작물 %s개 수확"},
			{"craft", "Make %s things at the %s station", "%2$s 작업대에서 %1$s번 만들기"},
			{"clear_lair", "Clear %s lair(s): beat the lord, open the treasure", "소굴 %s곳 정복 (군주를 쓰러뜨리고 보물 열기)"},
			{"dungeon", "Clear %s dungeon run(s)", "던전 %s번 돌파"},
			{"jump", "Reach the end of the Forest of Tenacity (no mounts or flying)", "끈기의 숲 끝까지 가기 (탈것·비행 금지)"},
		};
		for (String[] g : goals) {
			b.add("townquest.minecraft_mode.goal." + g[0], ko ? g[2] : g[1]);
		}

		String s = "screen.minecraft_mode.townquest.";
		b.add(s + "title", ko ? "주민 의뢰" : "Town Quests");
		b.add(s + "accept", ko ? "수락" : "Accept");
		b.add(s + "turn_in", ko ? "완료 보고" : "Turn in");
		b.add(s + "abandon", ko ? "포기" : "Abandon");
		b.add(s + "abandon_confirm", ko ? "정말 포기?" : "Sure?");
		b.add(s + "enter", ko ? "숲으로 가기" : "Go to the forest");
		b.add(s + "status.locked", ko ? "Lv %s부터" : "From Lv %s");
		b.add(s + "status.available", ko ? "새 의뢰!" : "New!");
		b.add(s + "status.active", ko ? "진행 중 %s/%s" : "In progress %s/%s");
		b.add(s + "status.ready", ko ? "완료! 보고하기" : "Ready to turn in!");
		b.add(s + "status.done", ko ? "오늘 완료" : "Done today");
		b.add(s + "status.done_once", ko ? "완료" : "Done");
		b.add(s + "daily", ko ? "일일 의뢰" : "Daily quest");
		b.add(s + "once", ko ? "★ 한 번뿐인 의뢰" : "★ One-time quest");
		b.add(s + "level", ko ? "Lv %s 이상" : "Lv %s+");
		b.add(s + "locked", ko ? "직업 레벨 %s부터 받을 수 있습니다." : "Needs class level %s.");
		b.add(s + "done_today", ko ? "오늘은 완료했습니다. %s 뒤에 다시 받을 수 있습니다." : "Done for today. Again in %s.");
		b.add(s + "done_once", ko ? "이미 끝낸 의뢰입니다." : "You have already done this one.");
		b.add(s + "ready", ko ? "다 했습니다! 완료 보고를 누르세요." : "All done! Press Turn in.");
		b.add(s + "deliver_hint", ko ? "완료 보고할 때 인벤토리와 가방에서 가져갑니다." : "Taken from your inventory and bags when you turn it in.");
		b.add(s + "reward", ko ? "보상" : "Reward");
		b.add(s + "reward_exp", ko ? "직업 경험치 %s" : "%s class EXP");
		b.add(s + "reset", ko ? "일일 의뢰 갱신까지 %s" : "Daily quests renew in %s");

		String m = "message.minecraft_mode.townquest.";
		b.add(m + "accepted", ko ? "의뢰 수락: %s — %s" : "Quest accepted: %s - %s");
		b.add(m + "progress", ko ? "%s: %s/%s" : "%s: %s/%s");
		b.add(m + "complete", ko ? "의뢰 달성: %s! %s에게 돌아가 보고하세요." : "Quest done: %s! Return to %s.");
		b.add(m + "turned_in", ko ? "의뢰 완료: %s! 보상: %s" : "Quest complete: %s! Reward: %s");
		b.add(m + "reward", ko ? "경험치 %s, %s, 강화석 %s개" : "%s EXP, %s, %s enhancement stone(s)");
		b.add(m + "abandoned", ko ? "의뢰를 포기했습니다: %s" : "Quest abandoned: %s");

		b.add("guide.minecraft_mode.quests", ko ? "주민 의뢰" : "Town quests");
		b.add("guide.minecraft_mode.quests.text", ko
			? "스톰홀드 주민 여섯 명이 의뢰를 줍니다. 우클릭하면 의뢰 목록이 열리고, 수락한 의뢰만 진행됩니다.\n\n"
				+ "· 농부의 아내 마사(시장 식료품점 노점 남쪽): 밀·배추 납품, 작물 수확, 요리.\n"
				+ "· 늙은 광부 듀건(남쪽 광장, 대장간 앞): 광석 채굴, 철·미스릴 원석과 다이아몬드 납품, 스켈레톤.\n"
				+ "· 어부 펠(항구 서쪽 부두): 낚시, 붕어 납품, 드라운드.\n"
				+ "· 은퇴한 모험가 로완(모험가 길드 앞): 네임드, 소굴 정복, 소굴의 군주, 던전, 엔더맨.\n"
				+ "· 꼬마 핍(광장 남동쪽 정원): 끈기의 숲 점프 코스(탈것·비행은 쓸 수 없음), 거미, 달콤한 열매.\n"
				+ "· 나무꾼 비요른(궁수 공원 숲): 묘목 납품, 좀비.\n\n"
				+ "처치는 파티원이 근처에 있으면 함께 셉니다. 납품은 완료 보고할 때 인벤토리와 가방에서 가져갑니다. 보상은 직업 경험치(지금 레벨 기준), 동전, 강화석이고, 한 번뿐인 의뢰(★)는 보호 주문서나 각성의 결정도 줍니다. 일일 의뢰는 현실 시간 "
				+ RealReset.RESET_HOUR + "시에 다시 열립니다."
			: "Six residents of Stormhold hand out quests. Right-click one for their list; only quests you accept make progress.\n\n"
				+ "· Farmer's Wife Martha (south of the grocer's stall in the market): wheat and cabbage, harvesting, cooking.\n"
				+ "· Old Miner Dugan (south square, by the forge): mining, raw iron, raw mythril and diamonds, skeletons.\n"
				+ "· Fisherman Pell (the west pier of the harbor): fishing, crucian carp, drowned.\n"
				+ "· Retired Adventurer Rowan (outside the Adventurers' Guild): named monsters, lairs, lair lords, dungeons, endermen.\n"
				+ "· Little Pip (the plaza's south-east garden): the Forest of Tenacity jump course (no mounts or flying), spiders, sweet berries.\n"
				+ "· Lumberjack Bjorn (the archer park's woods): saplings, zombies.\n\n"
				+ "Kills count for party members nearby too. Deliveries are taken from your inventory and bags when you turn the quest in. Rewards are class EXP (measured at your level), coins and enhancement stones; one-time quests (★) also give a protection scroll or an awakening crystal. Daily quests open again at "
				+ RealReset.RESET_HOUR + ":00 real time.");
	}

	private TownQuestLang() {
	}
}
