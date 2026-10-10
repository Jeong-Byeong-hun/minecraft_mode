package com.minecraftmode.client.datagen;

import com.minecraftmode.registry.ModItems;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;

/**
 * Translations for the town comforts: portals, ender chests, the travel circles and the homestead plains, bags and the trash button,
 * the target health bar, Huntmaster Garrick's trial hunts and the advancement kit.
 */
final class TownLang {
	static void add(final TranslationBuilder b, final boolean ko) {
		// bags
		b.add(ModItems.GEAR_BAG, ko ? "장비 가방" : "Gear Bag");
		b.add(ModItems.SUPPLY_BAG, ko ? "소비 가방" : "Supply Bag");
		b.add(ModItems.ORE_BAG, ko ? "광물 가방" : "Ore Bag");
		b.add("item.minecraft_mode.gear_bag.tooltip", ko ? "주운 무기·방어구·도구가 자동으로 들어갑니다." : "Weapons, armor and tools you pick up go in by themselves.");
		b.add("item.minecraft_mode.supply_bag.tooltip", ko ? "주운 음식·물약·소모품이 자동으로 들어갑니다." : "Food, potions and consumables you pick up go in by themselves.");
		b.add("item.minecraft_mode.ore_bag.tooltip", ko ? "주운 광석·주괴·보석과 돌·흙이 자동으로 들어갑니다." : "Ores, ingots, gems, stone and dirt you pick up go in by themselves.");
		b.add("item.minecraft_mode.bag.slots", ko ? "%s/%s칸 사용 중" : "%s/%s slots used");
		b.add("item.minecraft_mode.bag.usage", ko ? "들고 우클릭 또는 인벤토리에서 우클릭: 열기 · 아이템을 들고 우클릭: 넣기"
			: "Right-click in hand or in the inventory to open; right-click it holding items to put them in");
		b.add("message.minecraft_mode.bags.given", ko ? "보급관 브람에게서 장비·소비·광물 가방을 받았습니다. 주운 물건이 종류별로 알아서 들어갑니다!"
			: "Quartermaster Bram gives you a gear bag, a supply bag and an ore bag - what you pick up sorts itself into them!");
		// trash
		b.add("screen.minecraft_mode.trash.button", ko ? "버림" : "Bin");
		b.add("screen.minecraft_mode.trash.tooltip", ko ? "쓰레기통: 커서에 든 아이템을 들고 누르면 없앱니다. 빈 커서로 누르면 마지막에 버린 것을 되돌립니다."
			: "Trash: click while holding a stack to destroy it. Click with an empty cursor to take the last one back.");
		// travel circles, portals and the homestead
		b.add("message.minecraft_mode.waystone.homestead", ko ? "건축 평야로 이동했습니다. 돌아갈 때는 가운데의 마법진을 우클릭하세요." : "You arrive at the homestead plains. Right-click the circle in the middle to go back.");
		b.add("message.minecraft_mode.waystone.plaza", ko ? "수도 광장으로 돌아왔습니다." : "You are back on the plaza.");
		b.add("screen.minecraft_mode.guide.portals", ko ? "네더·엔드 차원문 (성 안뜰)" : "Nether & End portals (keep)");
		b.add("screen.minecraft_mode.guide.nether_portal", ko ? "네더 차원문" : "Nether Portal");
		b.add("screen.minecraft_mode.guide.end_portal", ko ? "엔드 차원문" : "End Portal");
		b.add("screen.minecraft_mode.guide.waystone", ko ? "이동 마법진 (건축 평야행)" : "Travel circle (to the homestead)");
		b.add("screen.minecraft_mode.guide.homestead", ko ? "건축 평야" : "Homestead plains");
		b.add("screen.minecraft_mode.guide.enchanters_hall", ko ? "마법 부여소 (레벨 30 마법 부여대)" : "Enchanter's Hall (level 30 tables)");
		b.add("screen.minecraft_mode.guide.ender_chests", ko ? "엔더 상자 (광장 외 5곳)" : "Ender chests (plaza and 5 more)");
		b.add("screen.minecraft_mode.guide.places_hint", ko ? "줄을 누르면 지도에서 보여 줍니다." : "Click a line to see it on the map.");
		b.add("screen.minecraft_mode.guide.handbook", ko ? "안내서 새로 받기" : "New handbook");
		// the town crier's notice board
		b.add("screen.minecraft_mode.notice.title", ko ? "스톰홀드 알림판" : "Stormhold Notice Board");
		b.add("screen.minecraft_mode.notice.day", ko ? "하루 초기화(일일 의뢰·빵): %s 뒤" : "New day (daily bounties, bread) in %s");
		b.add("screen.minecraft_mode.notice.cycle", ko ? "주기 초기화(레이드·소굴·사흘 의뢰): %s 뒤" : "New cycle (raids, lairs, three-day bounty) in %s");
		b.add("screen.minecraft_mode.notice.next_titan", ko ? "다음 해 질 녘(%s 뒤): 황야에 거신이 나타납니다" : "Next dusk (in %s): a titan rises in the wilds");
		b.add("screen.minecraft_mode.notice.next_invasion", ko ? "다음 해 질 녘(%s 뒤): 수도가 침공당합니다" : "Next dusk (in %s): the capital will be invaded");
		b.add("screen.minecraft_mode.notice.titan_now", ko ? "지금 거신 %s이(가) 걷고 있습니다: %s, %s" : "A titan walks now: %s at %s, %s");
		b.add("screen.minecraft_mode.notice.invasion_now", ko ? "수도가 침공당하고 있습니다! %s번째 물결" : "The capital is under attack! Wave %s");
		b.add("screen.minecraft_mode.notice.quiet", ko ? "지금은 조용합니다." : "All is quiet for now.");
		b.add("screen.minecraft_mode.notice.affixes", ko ? "이번 주기 레이드 변이:" : "Raid modifiers this cycle:");
		b.add("screen.minecraft_mode.notice.records", ko ? "레이드 최고 기록 (가장 높은 난이도)" : "Fastest raids (hardest difficulty beaten)");
		b.add("screen.minecraft_mode.notice.no_records", ko ? "아직 기록이 없습니다. 첫 기록의 주인공이 되세요!" : "No records yet. Be the first!");
		// the stylist
		b.add("container.minecraft_mode.stylist", ko ? "재단사의 작업대" : "Stylist's Table");
		b.add("screen.minecraft_mode.stylist.apply", ko ? "모양 바꾸기" : "Restyle");
		b.add("screen.minecraft_mode.stylist.restore", ko ? "원래 모양" : "Original");
		b.add("screen.minecraft_mode.stylist.hint", ko ? "왼쪽에 바꿀 장비, 오른쪽에 원하는 모양의 장비를 넣으세요. 능력치·강화·각인·이름은 그대로입니다."
			: "Gear on the left, a piece with the look you want on the right. Stats, enhancement, engravings and the name stay.");
		b.add("screen.minecraft_mode.stylist.mismatch", ko ? "방어구는 같은 부위 방어구의 모양만, 무기·도구는 무기·도구의 모양만 입힐 수 있습니다."
			: "Armor takes the look of armor for the same slot; weapons and tools take the look of weapons and tools.");
		b.add("screen.minecraft_mode.stylist.used_up", ko ? "오른쪽 장비는 모양을 넘겨주고 사라집니다. 원래 모양은 언제든 무료로 되돌릴 수 있어요."
			: "The piece on the right is used up. The original look comes back for free any time.");
		b.add("message.minecraft_mode.stylist.done", ko ? "셀레스트: 자, 이제 %s 모양이에요!" : "Celeste: There - it looks like %s now!");
		b.add("tooltip.minecraft_mode.look", ko ? "외형: %s" : "Look: %s");
		b.add("message.minecraft_mode.guide_book.given", ko ? "넬라: 최신판 모험가 안내서예요. 예전 안내서는 제가 가져갈게요."
			: "Nella: Here is the latest Adventurer's Handbook. I'll take the old one off your hands.");
		// trial hunts
		b.add("message.minecraft_mode.hunt.no_trial", ko ? "진행 중인 시련이 없네. 교관에게 시련을 받아 오게." : "You have no trial. Take one from your trainer first.");
		b.add("message.minecraft_mode.hunt.busy", ko ? "지금은 들여보낼 수 없네." : "I cannot send you in right now.");
		b.add("message.minecraft_mode.hunt.nothing", ko ? "자네 시련에 더 사냥할 몬스터는 없네. 교관에게 가 보게." : "Your trial needs no more hunting. Go see your trainer.");
		b.add("message.minecraft_mode.hunt.dragon", ko ? "엔더 드래곤은 내 사냥터에 못 들이네. 성 안뜰의 엔드 차원문으로 가게." : "I cannot pen the Ender Dragon. Take the End portal in the keep courtyard.");
		b.add("message.minecraft_mode.hunt.full", ko ? "사냥터가 모두 차 있네. 잠시 뒤에 오게." : "Every pen is taken. Come back in a moment.");
		b.add("message.minecraft_mode.hunt.entered", ko ? "시련 사냥터: %s에 필요한 몬스터만 나옵니다. 입구의 자석석을 우클릭하면 돌아갑니다."
			: "Trial hunting grounds: only what %s needs comes out. Right-click the lodestone at the entrance to go back.");
		b.add("message.minecraft_mode.hunt.done", ko ? "시련에 필요한 사냥을 마쳤습니다! %s초 뒤에 돌아갑니다." : "Your trial needs nothing more! Back in %s seconds.");
		b.add("message.minecraft_mode.hunt.finished", ko ? "사냥을 마치고 돌아왔습니다." : "The hunt is over; you are back.");
		b.add("message.minecraft_mode.hunt.left", ko ? "사냥터에서 나왔습니다." : "You leave the hunting grounds.");
		// gap-closing skills
		b.add("tooltip.minecraft_mode.skill.engage", ko ? "돌진 보호: 이동 후 %s초 무적, 맞은 적 %s초 경직 (보스 %s초)" : "Charge guard: %ss untouchable after the move, hits stagger for %ss (bosses %ss)");
		// advancement kit
		b.add("message.minecraft_mode.advance_kit", ko ? "%s 전직 기념으로 지금 레벨에 맞는 직업 무기와 방어구 한 벌을 받았습니다!"
			: "For becoming %s you receive a class weapon and a full set of armor for your level!");
		// guide topic
		b.add("guide.minecraft_mode.town", ko ? "마을 편의 시설" : "Town comforts");
		b.add("guide.minecraft_mode.town.text", ko
			? "· 성 안뜰(레이드·던전 문 사이)에 켜진 네더 차원문과 엔드 차원문이 있습니다.\n· 엔더 상자: 광장, 성 안뜰, 모험가 길드, 시장, 남쪽 광장.\n· 건축 평야: 동문 밖의 넓은 평지에 집을 지을 수 있습니다(몬스터가 지상에 나오지 않음). 광장 스폰 뒤의 이동 마법진(자석석)을 우클릭하면 오가고, 귀환 주문서로도 돌아옵니다.\n· 부활: 평소에는 광장에서, 침대에서 자거나 우클릭하면 그 침대에서 부활합니다(침대가 부서지면 다시 광장).\n· 가방: 보급관 브람이 장비·소비·광물 가방을 줍니다(잡화점에서 추가 구매). 주운 물건이 종류별로 들어갑니다.\n· 인벤토리 오른쪽 '버림' 버튼: 커서의 아이템을 없앱니다(한 번 되돌리기 가능). 잡템은 잡화점이 사 줍니다.\n· 마지막으로 때린 몬스터의 체력이 화면 위에 보입니다.\n· 사냥터지기 개릭(광장): 시련에 필요한 몬스터만 나오는 개인 사냥터로 보내 줍니다.\n· 전직할 때마다 레벨에 맞는 직업 무기와 방어구 한 벌을 받습니다.\n· 포고관 오도(광장): 초기화까지 남은 시간, 오늘 밤 이벤트, 레이드 변이와 최고 기록을 알려 줍니다.\n· 재단사 셀레스트(시장 보석상 노점): 동전을 받고 장비의 겉모습을 다른 장비 모양으로 바꿔 줍니다. 원래 모양은 무료로 되돌립니다."
			: "· The keep courtyard (between the raid and dungeon gates) has a lit Nether portal and an active End portal.\n· Ender chests: plaza, keep courtyard, Adventurers' Guild, market and the south square.\n· Homestead plains: open flat land outside the east gate to build your home (no monsters on its surface). Right-click the travel circle (lodestone) behind the plaza spawn to go there and back; a return scroll also brings you home.\n· Respawning: on the plaza, or at the bed you last slept in or clicked (back to the plaza when it breaks).\n· Bags: Quartermaster Bram hands out a gear, supply and ore bag (more at the general store). What you pick up sorts itself into them.\n· The inventory's Bin button destroys the stack on your cursor (one undo). The general store buys odds and ends.\n· The health of the monster you hit last shows at the top of the screen.\n· Huntmaster Garrick (plaza) sends you to a private arena with only the monsters your trial needs.\n· Every advancement gives a class weapon and a full set of armor for your level.\n· Town Crier Odo (plaza): time to the next resets, tonight's event, the raid modifiers and the fastest raids.\n· Stylist Celeste (the jeweler's stall in the market): for coins, gear takes the look of another piece; the original look comes back for free.");
	}

	private TownLang() {
	}
}
