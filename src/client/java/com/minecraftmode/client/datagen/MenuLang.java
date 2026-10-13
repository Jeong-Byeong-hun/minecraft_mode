package com.minecraftmode.client.datagen;

import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;

/** Translations for the adventurer's menu (party, paying, settings, return home) and the operators' admin screen. */
final class MenuLang {
	static void add(final TranslationBuilder b, final boolean ko) {
		menu(b, ko);
		admin(b, ko);
		b.add("message.minecraft_mode.wallet.not_online", ko ? "%s님은 접속해 있지 않습니다." : "%s is not online.");
		b.add("message.minecraft_mode.land.released", ko ? "관리자가 내 땅 하나를 해제했습니다. 증서와 보관함 물건 %s묶음을 돌려받았습니다."
			: "An operator freed one of your plots. The deed and %s stacks from it came back to you.");
		b.add("message.minecraft_mode.admin.kicked", ko ? "관리자가 서버에서 내보냈습니다." : "An operator removed you from the server.");
		b.add("message.minecraft_mode.admin.notice", ko ? "[공지] %s" : "[Notice] %s");
		b.add("message.minecraft_mode.admin.land_unloaded", ko ? "그 땅이 로드되어 있지 않습니다. 먼저 '이동'으로 가서 해제하세요."
			: "That land is not loaded. Go there first ('Go'), then free it.");
	}

	private static void menu(final TranslationBuilder b, final boolean ko) {
		String s = "screen.minecraft_mode.menu.";
		b.add("key.minecraft_mode.menu", ko ? "모험가 메뉴" : "Adventurer's menu");
		b.add(s + "title", ko ? "모험가 메뉴" : "Adventurer's Menu");
		b.add(s + "party", ko ? "파티" : "Party");
		b.add(s + "pay", ko ? "송금" : "Pay");
		b.add(s + "settings", ko ? "설정" : "Settings");
		b.add(s + "admin", ko ? "관리자" : "Admin");
		b.add(s + "sure", ko ? "확인?" : "Sure?");
		b.add(s + "return_home", ko ? "귀환" : "Return home");
		// party
		b.add(s + "no_party", ko ? "파티가 없습니다." : "You are not in a party.");
		b.add(s + "create", ko ? "파티 만들기" : "Create party");
		b.add(s + "invited", ko ? "%s님이 파티에 초대했습니다." : "%s invited you to a party.");
		b.add(s + "accept", ko ? "수락" : "Accept");
		b.add(s + "members", ko ? "파티원 (%s/%s) · ★ 파티장" : "Members (%s/%s) · ★ leader");
		b.add(s + "promote", ko ? "위임" : "Lead");
		b.add(s + "kick", ko ? "추방" : "Kick");
		b.add(s + "leave", ko ? "나가기" : "Leave");
		b.add(s + "disband", ko ? "해산" : "Disband");
		b.add(s + "invite", ko ? "초대하기 (누르면 초대, 1분 동안 유효)" : "Invite (click a name; lasts a minute)");
		b.add(s + "nobody", ko ? "접속한 다른 플레이어가 없습니다." : "Nobody else is online.");
		// pay
		b.add(s + "balance", ko ? "내 지갑:" : "Your wallet:");
		b.add(s + "payee", ko ? "받는 사람" : "Pay to");
		b.add(s + "amount", ko ? "금액" : "Amount");
		b.add(s + "gold", ko ? "금화" : "Gold");
		b.add(s + "silver", ko ? "은화" : "Silver");
		b.add(s + "copper", ko ? "동화" : "Copper");
		b.add(s + "pick_payee", ko ? "받는 사람을 고르세요" : "Pick someone first");
		b.add(s + "send", ko ? "%s님에게 보내기" : "Send to %s");
		// settings
		b.add(s + "setting.damage_numbers", ko ? "피해 숫자" : "Damage numbers");
		b.add(s + "setting.damage_numbers.hint", ko ? "때린 피해를 대상 위에 숫자로 띄웁니다." : "Floating numbers for the damage you deal.");
		b.add(s + "setting.enhance_effects", ko ? "강화 장비 파티클" : "Enhanced gear particles");
		b.add(s + "setting.enhance_effects.hint", ko ? "+10 이상 장비의 파티클입니다. 테두리와 광택은 꺼지지 않습니다." : "Particles of +10 gear. Frames and glint always stay.");
		b.add(s + "setting.hp_bar", ko ? "HP·MP 막대" : "HP and MP bars");
		b.add(s + "setting.hp_bar.hint", ko ? "끄면 바닐라 하트와 방어 아이콘으로 돌아갑니다." : "Off brings back vanilla hearts and armor icons.");
		// characters
		b.add(s + "characters", ko ? "캐릭터" : "Characters");
		b.add(s + "character.loading", ko ? "불러오는 중..." : "Loading...");
		b.add(s + "character.slot", ko ? "슬롯 %s" : "Slot %s");
		b.add(s + "character.empty", ko ? "비어 있음" : "Empty");
		b.add(s + "character.playing", ko ? "플레이 중" : "Playing");
		b.add(s + "character.play", ko ? "이 캐릭터로" : "Play");
		b.add(s + "character.new", ko ? "새 캐릭터" : "Create");
		b.add(s + "character.buy", ko ? "슬롯 구매 (기여도 %s)" : "Buy a slot (%s merit)");
		b.add(s + "character.max", ko ? "슬롯을 모두 열었습니다." : "All slots are open.");
		b.add(s + "character.select", ko ? "캐릭터 선택" : "Choose a Character");
		b.add(s + "character.continue", ko ? "계속하기" : "Continue");
		b.add(s + "character.choose", ko ? "플레이할 캐릭터를 고르세요. 완료를 누르면 지금 캐릭터로 들어갑니다." : "Pick a character to play; Done goes on with this one.");
		b.add(s + "character.town", ko ? "캐릭터는 수도 성벽 안에서만 바꿀 수 있습니다." : "Switch characters inside the capital's walls.");
		b.add(s + "character.shared", ko ? "엔더 상자·도감·땅·거래소는 모든 캐릭터가 함께 씁니다." : "Ender chest, codex, land and market are shared.");
		String m = "message.minecraft_mode.character.";
		b.add(m + "switched", ko ? "슬롯 %s의 캐릭터로 바꿨습니다." : "Now playing the character in slot %s.");
		b.add(m + "started", ko ? "슬롯 %s에서 새 캐릭터를 시작합니다." : "A new character starts in slot %s.");
		b.add(m + "bought", ko ? "캐릭터 슬롯이 %s개가 되었습니다." : "You now have %s character slots.");
		b.add(m + "not_here", ko ? "캐릭터는 수도 성벽 안에서만 바꿀 수 있습니다." : "You can only switch characters inside the capital's walls.");
		b.add(m + "busy", ko ? "레이드·분배·침공·거신 전투가 끝나야 캐릭터를 바꿀 수 있습니다." : "Finish your raid, its loot, the invasion or the titan fight before switching characters.");
		b.add(m + "failed", ko ? "캐릭터를 바꾸지 못했습니다. 지금 캐릭터 그대로입니다." : "The switch failed; you are still playing the same character.");
		b.add(m + "max", ko ? "더 이상 슬롯을 살 수 없습니다." : "You cannot buy more slots.");
		b.add(m + "no_merit", ko ? "기여도가 부족합니다." : "Not enough merit.");
	}

	private static void admin(final TranslationBuilder b, final boolean ko) {
		String s = "screen.minecraft_mode.admin.";
		b.add(s + "title", ko ? "관리자" : "Admin");
		b.add(s + "tab.players", ko ? "플레이어" : "Players");
		b.add(s + "tab.travel", ko ? "이동·소환" : "Travel");
		b.add(s + "tab.content", ko ? "콘텐츠" : "Content");
		b.add(s + "tab.server", ko ? "서버" : "Server");
		b.add(s + "tab.economy", ko ? "경제·토지" : "Economy");
		b.add(s + "back", ko ? "메뉴로" : "Menu");
		b.add(s + "loading", ko ? "불러오는 중..." : "Loading...");
		b.add(s + "target", ko ? "대상: %s" : "Target: %s");
		b.add(s + "me", ko ? "나" : "me");
		b.add(s + "ok", ko ? "✔ 완료" : "✔ Done");
		b.add(s + "failed", ko ? "✘ 실패 (대상·값 확인)" : "✘ Failed (check target, values)");
		b.add(s + "none", ko ? "없음" : "None");
		b.add(s + "go", ko ? "이동" : "Go");
		// players
		b.add(s + "online", ko ? "접속자 (%s)" : "Online (%s)");
		b.add(s + "gone", ko ? "고른 플레이어가 나갔습니다." : "That player has left.");
		b.add(s + "chapter_now", ko ? " · 스토리 %s장" : " · story chapter %s");
		b.add(s + "tier", ko ? "%s차" : "Tier %s");
		b.add(s + "job_set", ko ? "직업 설정" : "Set class");
		b.add(s + "level", ko ? "레벨" : "Lv");
		b.add(s + "level_set", ko ? "레벨 설정" : "Set level");
		b.add(s + "exp", ko ? "경험치" : "EXP");
		b.add(s + "exp_add", ko ? "경험치 지급" : "Give EXP");
		b.add(s + "mana", ko ? "MP 채우기" : "Fill MP");
		b.add(s + "cooldowns", ko ? "쿨다운 초기화" : "Reset cooldowns");
		b.add(s + "heal", ko ? "체력·배고픔 회복" : "Heal");
		b.add(s + "quest_clear", ko ? "진행 중인 시련 취소" : "Drop active trial");
		b.add(s + "quest_goals", ko ? "시련 처치 목표 달성" : "Complete trial kills");
		b.add(s + "money", ko ? "예: 3g 2s 5" : "e.g. 3g 2s 5");
		b.add(s + "money_give", ko ? "돈 지급" : "Give money");
		b.add(s + "money_set", ko ? "잔액 설정" : "Set balance");
		b.add(s + "goto", ko ? "가기" : "Go to");
		b.add(s + "bring", ko ? "불러오기" : "Bring");
		b.add(s + "mode.survival", ko ? "생존" : "S");
		b.add(s + "mode.creative", ko ? "창작" : "C");
		b.add(s + "mode.adventure", ko ? "모험" : "A");
		b.add(s + "mode.spectator", ko ? "관전" : "Sp");
		b.add(s + "chapter", ko ? "장" : "Ch");
		b.add(s + "chapter_set", ko ? "스토리 설정" : "Set story");
		b.add(s + "reason", ko ? "추방 사유 (선택)" : "Kick reason (optional)");
		b.add(s + "kick", ko ? "추방" : "Kick");
		// travel
		b.add(s + "coords", ko ? "좌표로 이동 (Y를 비우면 지표)" : "Coordinates (empty Y = surface)");
		b.add(s + "surface", ko ? "Y (지표)" : "Y (top)");
		b.add(s + "places", ko ? "장소로 이동" : "Places");
		b.add(s + "gather", ko ? "전원 소집" : "Gather all");
		b.add(s + "summon", ko ? "몹 소환 (대상 위치)" : "Summon (at the target)");
		b.add(s + "named_pick", ko ? "네임드 몬스터 고르기 ▸" : "Pick a named monster ▸");
		b.add(s + "entity", ko ? "엔티티 id (예: minecraft:zombie)" : "Entity id (e.g. minecraft:zombie)");
		b.add(s + "count", ko ? "수" : "No.");
		b.add(s + "at_feet", ko ? "발밑에" : "At feet");
		b.add(s + "at_look", ko ? "보는 곳에" : "Where looking");
		b.add(s + "summon_go", ko ? "소환" : "Summon");
		b.add(s + "summon_hint", ko ? "한 번에 최대 %s마리. 소환한 몹은 도시 경비병이 쫓아내지 않습니다." : "Up to %s at once. City guards leave summoned monsters alone.");
		// content
		b.add(s + "dungeon", ko ? "던전" : "Dungeon");
		b.add(s + "keystone", ko ? "+단계" : "+lvl");
		b.add(s + "start_party", ko ? "대상 파티로 시작" : "Start (target's party)");
		b.add(s + "raid", ko ? "레이드" : "Raid");
		b.add(s + "running_raids", ko ? "진행 중인 레이드 (%s) · *는 이탈" : "Running raids (%s) · * = out");
		b.add(s + "running_dungeons", ko ? "진행 중인 던전 (%s) · *는 이탈" : "Running dungeons (%s) · * = out");
		b.add(s + "stop", ko ? "종료" : "Stop");
		b.add(s + "more", ko ? "외 %s개" : "and %s more");
		b.add(s + "events", ko ? "월드 이벤트" : "World events");
		b.add(s + "titan_random", ko ? "타이탄: 무작위 ▸" : "Titan: random ▸");
		b.add(s + "titan", ko ? "타이탄" : "Titan");
		b.add(s + "invasion", ko ? "침공" : "Invasion");
		b.add(s + "event_stop", ko ? "이벤트 중지" : "Stop events");
		// server
		b.add(s + "time", ko ? "시간" : "Time");
		b.add(s + "time.day", ko ? "아침" : "Day");
		b.add(s + "time.noon", ko ? "정오" : "Noon");
		b.add(s + "time.night", ko ? "밤" : "Night");
		b.add(s + "time.midnight", ko ? "자정" : "Midnight");
		b.add(s + "weather", ko ? "날씨" : "Weather");
		b.add(s + "weather.clear", ko ? "맑음" : "Clear");
		b.add(s + "weather.rain", ko ? "비" : "Rain");
		b.add(s + "weather.thunder", ko ? "뇌우" : "Thunder");
		b.add(s + "resets", ko ? "초기화 (현실 시간)" : "Resets (real time)");
		b.add(s + "next_day", ko ? "다음 일일 초기화: %s" : "Next daily reset: %s");
		b.add(s + "next_cycle", ko ? "다음 레이드 주기: %s" : "Next raid cycle: %s");
		b.add(s + "skip_day", ko ? "하루 넘기기" : "Skip a day");
		b.add(s + "skip_cycle", ko ? "%s일 넘기기" : "Skip %s days");
		b.add(s + "skip_hint", ko ? "레이드 잠금·수정자와 현상금이 바로 갱신됩니다 (되돌릴 수 없음)" : "Raid lockouts, modifiers and bounties renew at once (no undo)");
		b.add(s + "broadcast", ko ? "전체 공지 (채팅과 화면 제목)" : "Notice to everyone (chat and title)");
		b.add(s + "notice_hint", ko ? "공지 내용" : "Message");
		b.add(s + "notice_send", ko ? "공지" : "Send");
		// market and land
		b.add(s + "market", ko ? "거래소" : "Market");
		b.add(s + "land", ko ? "토지" : "Land");
		b.add(s + "search", ko ? "아이템 이름(영어)·id·판매자" : "Item name, id or seller");
		b.add(s + "search_go", ko ? "검색" : "Search");
		b.add(s + "remove", ko ? "회수" : "Remove");
		b.add(s + "remove_hint", ko ? "회수한 물건은 판매자 우편함으로" : "Removed items go to the seller's mailbox");
		b.add(s + "plot", ko ? "  X %s · Z %s · Y %s · 허용 %s명" : "  X %s · Z %s · Y %s · %s trusted");
		b.add(s + "release", ko ? "해제" : "Free");
		b.add(s + "release_hint", ko ? "해제하면 초원으로 되돌리고 물건·증서는 주인에게" : "Freed land resets; items and deed go to the owner");
	}

	private MenuLang() {
	}
}
