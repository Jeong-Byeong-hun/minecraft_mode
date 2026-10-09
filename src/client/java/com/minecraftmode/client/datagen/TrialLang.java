package com.minecraftmode.client.datagen;

import com.minecraftmode.entity.ClassTrainer;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.quest.QuestDef;
import com.minecraftmode.job.quest.Quests;
import com.minecraftmode.registry.ModEntities;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;

/** Translations for the trainers, advancement trials, trial tokens, the city and the guide book. */
final class TrialLang {
	static void add(final TranslationBuilder b, final boolean ko) {
		b.add(ModEntities.CLASS_TRAINER, ko ? "직업 교관" : "Class Trainer");
		String[][] trainers = {
			{"warrior", "Knight-Commander Bedivere", "기사단장 베디비어",
				"A blade is only as strong as the will behind it. Show me yours, and I will make a knight of you.",
				"검은 그것을 쥔 의지만큼만 강하다. 네 의지를 보여 준다면 기사로 만들어 주마."},
			{"rogue", "Hattori Hanzo", "핫토리 한조",
				"...You found me. Good. Those who can find shadows can learn to become one.",
				"...나를 찾아냈군. 좋다. 그림자를 찾는 자는 그림자가 되는 법도 배울 수 있다."},
			{"mage", "Archmage Merlin", "대마법사 멀린",
				"Oh, a new apprentice! Magic is just curiosity with a great deal of homework. Shall we begin?",
				"오, 새 제자로군! 마법이란 숙제가 아주 많은 호기심일 뿐이야. 시작해 볼까?"},
			{"archer", "Chiron the Wise", "현자 케이론",
				"Every hero I taught began with a single arrow. Breathe, aim, and let go.",
				"내가 가르친 영웅들도 모두 화살 한 발로 시작했다. 숨을 고르고, 겨누고, 놓아라."},
			{"pirate", "Captain Drake", "선장 드레이크",
				"Ahoy! Want to sail with me? The sea pays in gold, blood or glory - usually all three!",
				"어이! 나랑 같이 항해할 텐가? 바다는 금이나 피나 영광으로 값을 치르지. 대개는 셋 다야!"},
		};
		for (String[] t : trainers) {
			JobClass job = JobClass.byId(t[0]);
			b.add(ClassTrainer.nameKey(job), ko ? t[2] : t[1]);
			b.add(ClassTrainer.greetingKey(job), ko ? t[4] : t[3]);
		}

		String[][] tokens = {
			{"rusted_medal", "Rusted Medal", "녹슨 훈장"}, {"champions_laurel", "Champion's Laurel", "투사의 월계관"},
			{"frenzy_blood", "Drop of Frenzy", "광기의 핏방울"}, {"grail_shard", "Grail Shard", "성배의 파편"},
			{"thieves_token", "Thieves' Guild Token", "도적 길드 증표"}, {"ninja_scroll", "Ninja Scroll", "닌자 두루마리"},
			{"assassin_seal", "Assassin's Seal", "암살 교단 인장"}, {"monarchs_shadow", "Monarch's Shadow", "군주의 그림자"},
			{"arcane_dust", "Arcane Dust", "마력 가루"}, {"grimoire_page", "Grimoire Page", "마도서 페이지"},
			{"ember_core", "Ember Core", "불꽃의 핵"}, {"akashic_fragment", "Akashic Fragment", "아카식 단편"},
			{"steel_arrowhead", "Steel Arrowhead", "강철 화살촉"}, {"phantom_plume", "Phantom Plume", "팬텀 깃털"},
			{"spirit_arrow", "Spirit Arrow", "영령의 화살"}, {"golden_key", "Golden Key", "황금 열쇠"},
			{"map_scrap", "Treasure Map Scrap", "보물 지도 조각"}, {"bounty_poster", "Bounty Poster", "현상금 전단"},
			{"haki_crystal", "Haki Crystal", "패기의 결정"}, {"sea_kings_treasure", "Sea King's Treasure", "해왕의 보물"},
		};
		for (String[] t : tokens) {
			b.add("item.minecraft_mode." + t[0], ko ? t[2] : t[1]);
		}
		for (QuestDef quest : Quests.all()) {
			b.add(quest.nameKey(), ko ? quest.ko() : quest.en());
			b.add(quest.storyKey(), ko ? quest.storyKo() : quest.storyEn());
			for (int i = 0; i < quest.kills().size(); i++) {
				b.add(quest.goalKey(i), ko ? quest.kills().get(i).ko() : quest.kills().get(i).en());
			}
		}

		b.add("message.minecraft_mode.quest.accepted", ko ? "전직 시련 수락: %s. K 키로 목표를 확인하세요." : "Trial accepted: %s. Press K to see your goals.");
		b.add("message.minecraft_mode.quest.abandoned", ko ? "시련 포기: %s" : "Trial abandoned: %s");
		b.add("message.minecraft_mode.quest.progress", "%s %s/%s");
		b.add("message.minecraft_mode.quest.token", ko ? "시련 증표: %s (%s/%s)" : "Trial token: %s (%s/%s)");
		b.add("message.minecraft_mode.city.welcome", ko
			? "스톰홀드에 오신 것을 환영합니다! 인벤토리의 모험가 안내서를 읽어 보세요."
			: "Welcome to Stormhold! Read the Adventurer's Handbook in your inventory.");

		b.add("commands.minecraft_mode.job.trainer", ko ? "%s을(를) 소환했습니다" : "Summoned %s");
		b.add("commands.minecraft_mode.job.unknown_class", ko ? "알 수 없는 직업입니다" : "Unknown class");
		b.add("commands.minecraft_mode.job.quest_clear", ko ? "플레이어 %s명의 시련을 취소했습니다" : "Cleared the trial of %s player(s)");
		b.add("commands.minecraft_mode.job.quest_goals", ko ? "플레이어 %s명의 처치 목표를 완료했습니다" : "Completed the kill goals of %s player(s)");

		b.add("screen.minecraft_mode.trainer.accept", ko ? "시련 수락" : "Accept trial");
		b.add("screen.minecraft_mode.trainer.complete", ko ? "시련 완료" : "Complete trial");
		b.add("screen.minecraft_mode.trainer.abandon", ko ? "포기" : "Abandon");
		b.add("screen.minecraft_mode.trainer.of", ko ? "%s 교관" : "%s trainer");
		b.add("screen.minecraft_mode.trainer.other_class", ko
			? "당신은 이미 %s의 길을 걷고 있습니다. 당신의 교관을 찾아가세요. (직업 초기화 주문서로 다시 고를 수 있습니다)"
			: "You already walk the path of the %s. Seek your own trainer. (A Class Reset Scroll lets you choose again.)");
		b.add("screen.minecraft_mode.trainer.mastered", ko
			? "이 길의 모든 차수를 마스터했습니다. 더 가르칠 것이 없습니다."
			: "You have mastered every tier of this path. There is nothing left I can teach you.");
		b.add("screen.minecraft_mode.trainer.busy", ko
			? "이미 다른 시련(%s)을 진행 중입니다. 먼저 완료하거나 포기하세요."
			: "You are already on another trial (%s). Finish or abandon it first.");
		b.add("screen.minecraft_mode.trainer.trial", ko ? "%s차 시련: %s" : "Trial %s: %s");
		b.add("screen.minecraft_mode.trainer.reward", "→ %s");
		b.add("screen.minecraft_mode.trainer.goal_kill", ko ? "%s 처치" : "Defeat %s");
		b.add("screen.minecraft_mode.trainer.goal_item", "%s");
		b.add("screen.minecraft_mode.job.visit_any", ko ? "스톰홀드의 교관에게서 직업을 고르세요 (x, z):" : "Choose a class at any trainer in Stormhold (x, z):");
		b.add("screen.minecraft_mode.job.next_trial", ko ? "다음: %s차 시련 \"%s\" - %s (%s, %s)" : "Next: tier %s trial \"%s\" from %s (%s, %s)");
		b.add("screen.minecraft_mode.job.return_to", ko ? "준비되면 %s(%s, %s)에게 돌아가세요." : "Return to %s at %s, %s when ready.");

		b.add("book.minecraft_mode.guide.title", ko ? "모험가 안내서" : "Adventurer's Handbook");
		String[][] pages = {
			{"Welcome to Stormhold, the capital of adventurers!\n\nThe city around 0, 0 is a safe zone: monsters do not spawn, buildings cannot be broken and players cannot fight each other.",
				"모험가의 수도 스톰홀드에 오신 것을 환영합니다!\n\n0, 0 주변의 도시는 안전 지대입니다. 몬스터가 생기지 않고, 건물을 부술 수 없으며, 플레이어끼리 싸울 수 없습니다."},
			{"City map\n\nCenter: Plaza (spawn)\nNorth: the Keep\nNorthwest: Mage tower, Enchanter's Hall\nNortheast: Warrior arena\nWest: Old Town, Shadow Hall\nEast: Cathedral, homes\nSouthwest: Archer park\nSouth: Market, Guild\nSoutheast: Harbor",
				"도시 지도\n\n중앙: 광장 (시작 지점)\n북쪽: 왕성\n북서: 마법사 탑, 마법 부여소\n북동: 전사 투기장\n서쪽: 구시가지, 그림자 회관\n동쪽: 대성당, 주택가\n남서: 궁수 공원\n남쪽: 시장, 길드\n남동: 항구"},
			{"Classes\n\nReach level 10 by defeating monsters and mining iron or better ores, then talk to a trainer:\nBedivere - Warrior\nHanzo - Rogue\nMerlin - Mage\nChiron - Archer\nDrake - Pirate",
				"직업\n\n몬스터를 처치하고 철 이상의 광석을 캐서 레벨 10을 달성한 뒤 교관과 대화하세요.\n베디비어 - 전사\n한조 - 도적\n멀린 - 법사\n케이론 - 궁수\n드레이크 - 해적"},
			{"Advancement trials\n\nEvery advancement is a trial: defeat the listed enemies, collect the trial tokens they drop, and bring them back with essence.\n\nTiers unlock at levels 10, 25, 45 and 70.",
				"전직 시련\n\n모든 전직은 시련입니다. 목표 적을 처치하고, 그들이 떨어뜨리는 시련 증표를 모아 정수와 함께 교관에게 가져가세요.\n\n레벨 10, 25, 45, 70에 각 차수가 열립니다."},
			{"Weapons and skills\n\nClass weapons are sold at the Guild in the south market. Use skills with R, G, V and Z; open the class screen with K. Skills only work for the weapon's class, tier and level.",
				"무기와 스킬\n\n직업 무기는 남쪽 시장의 길드에서 팝니다. 스킬은 R, G, V, Z, 직업 창은 K입니다. 스킬은 무기의 직업·차수·레벨 조건을 만족해야 쓸 수 있습니다."},
			{"Essence\n\nEssence drops from monsters and iron or better ores. Engrave class gear at the engraving tables in the Guild: up to 3 lines on weapons and 4 on armor, and the same line stacks.",
				"정수\n\n정수는 몬스터와 철 이상의 광석에서 나옵니다. 길드의 정수 각인대에서 직업 무기는 3줄, 방어구는 4줄까지 각인할 수 있고, 같은 각인은 중첩됩니다."},
		};
		for (int i = 0; i < pages.length; i++) {
			b.add("book.minecraft_mode.guide.page" + (i + 1), ko ? pages[i][1] : pages[i][0]);
		}
	}

	private TrialLang() {
	}
}
