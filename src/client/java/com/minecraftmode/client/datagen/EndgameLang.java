package com.minecraftmode.client.datagen;

import com.minecraftmode.bounty.Bounties;
import com.minecraftmode.bounty.BountyKind;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.progress.Achievements;
import com.minecraftmode.raid.RaidAffix;
import com.minecraftmode.raid.RaidDifficulty;
import com.minecraftmode.registry.ModBlocks;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.talent.TalentTree;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;

/**
 * Translations for the endgame: the three-day reset, personal lair chests and lords, raid difficulties, modifiers and records,
 * guild bounties and the merit shop, enhancement, the market, achievements, titles, the codex and talents.
 */
final class EndgameLang {
	static void add(final TranslationBuilder b, final boolean ko) {
		b.add("time.minecraft_mode.remaining", ko ? "%s일 %s시간" : "%sd %sh");
		lairs(b, ko);
		raids(b, ko);
		bounties(b, ko);
		enhancement(b, ko);
		market(b, ko);
		progress(b, ko);
		talents(b, ko);
	}

	private static void lairs(final TranslationBuilder b, final boolean ko) {
		b.add(ModBlocks.LAIR_CHEST, ko ? "소굴 보물 상자" : "Lair Treasure Chest");
		b.add(ModBlocks.LAIR_CACHE, ko ? "소굴 보급품" : "Lair Cache");
		b.add("container.minecraft_mode.lair_chest", ko ? "소굴 보물 (이번 주기 개인 보상)" : "Lair Treasure (yours this cycle)");
		b.add("container.minecraft_mode.lair_cache", ko ? "소굴 보급품 (이번 주기 개인 보상)" : "Lair Cache (yours this cycle)");
		b.add(ModItems.LAIR_MAP, ko ? "소굴 지도" : "Lair Map");
		b.add("item.minecraft_mode.lair_map.filled", ko ? "소굴 지도 (%s, %s)" : "Lair Map (%s, %s)");
		b.add("item.minecraft_mode.lair_map.tooltip", ko ? "사용하면 가장 가까운 네임드 소굴을 표시한 지도가 됩니다." : "Use it to mark the nearest named lair on a map.");
		b.add("entity.minecraft_mode.lair_lord", ko ? "소굴의 군주 %s" : "Lair Lord %s");
		b.add("message.minecraft_mode.lair.sealed", ko ? "보물이 봉인되어 있다. 소굴의 군주 %s을(를) 쓰러뜨린 사람만 열 수 있다!" : "The treasure is sealed: only those who defeat the lair lord %s may open it!");
		b.add("message.minecraft_mode.lair.lord_defeated", ko ? "소굴의 군주 %s이(가) 쓰러졌다! 이번 주기 동안 보물 상자가 당신에게 열립니다." : "The lair lord %s falls! The treasure opens for you this cycle.");
		b.add("message.minecraft_mode.lair.personal", ko ? "이 보물은 당신만의 몫입니다. %s 뒤에 다시 채워집니다." : "This treasure is yours alone. It refills for you in %s.");
		b.add("message.minecraft_mode.lair.discovered", ko ? "새 소굴 발견: %s! 직업 경험치 +%s" : "Lair discovered: %s! +%s class experience");
		b.add("message.minecraft_mode.lair.first_clear", ko ? "%s 첫 공략! 직업 경험치 +%s" : "First clear of %s! +%s class experience");
		b.add("message.minecraft_mode.lair.chest_exp", ko ? "%s의 보물: 직업 경험치 +%s" : "Treasure of %s: +%s class experience");
		b.add("message.minecraft_mode.lair.lord_awakens", ko ? "%2$s의 군주 %1$s이(가) 깨어났다!" : "%s, lord of %s, awakens!");
		b.add("message.minecraft_mode.lair.no_map", ko ? "이 지도로 찾을 수 있는 소굴이 근처에 없습니다." : "There is no lair within reach of this map.");
	}

	private static void raids(final TranslationBuilder b, final boolean ko) {
		for (RaidDifficulty d : RaidDifficulty.values()) {
			b.add(d.nameKey(), switch (d) {
				case NORMAL -> ko ? "일반" : "Normal";
				case HEROIC -> ko ? "영웅" : "Heroic";
				case NIGHTMARE -> ko ? "악몽" : "Nightmare";
			});
		}
		for (RaidAffix a : RaidAffix.values()) {
			String[] t = switch (a) {
				case ENRAGE -> new String[] {"Enrage", "격노", "Below 30%% health the boss hits 30%% harder.", "체력 30%% 이하에서 보스의 피해가 30%% 늘어납니다."};
				case TURBULENT -> new String[] {"Turbulent", "격동", "Mechanics come 20%% more often.", "기믹이 20%% 더 자주 옵니다."};
				case FORTIFIED -> new String[] {"Fortified", "견고", "The boss takes 15%% less damage.", "보스가 받는 피해가 15%% 줄어듭니다."};
				case BLEEDING -> new String[] {"Bleeding", "출혈", "Every 20 seconds everyone bleeds.", "20초마다 모두에게 출혈이 걸립니다."};
				case GLOOM -> new String[] {"Gloom", "암흑", "Every 30 seconds darkness pulses over the arena.", "30초마다 경기장에 어둠이 퍼집니다."};
				case VOLATILE -> new String[] {"Volatile", "폭발", "At 75%%, 50%% and 25%% health the boss erupts around itself (telegraphed).",
					"체력 75%%·50%%·25%%에서 보스 주변이 폭발합니다 (예고 있음)."};
			};
			b.add(a.nameKey(), ko ? t[1] : t[0]);
			b.add(a.descKey(), ko ? t[3] : t[2]);
		}
		b.add("message.minecraft_mode.raid.practice", ko ? "이번 주기 보상은 이미 받았습니다 — 연습으로 참가합니다 (참가비·보상 없음). 보상 초기화까지 %s."
			: "You already took this reward this cycle - practice run (no fee, no rewards). Rewards reset in %s.");
		b.add("message.minecraft_mode.raid.practice_done", ko ? "연습 클리어! 이번에는 보상이 없습니다." : "Practice run cleared - no rewards this time.");
		b.add("message.minecraft_mode.raid.time", ko ? "클리어 시간: %s (%s)" : "Clear time: %s (%s)");
		b.add("message.minecraft_mode.raid.record", ko ? "신기록! 토벌 기록판 %s위!" : "New record: #%s on the raid board!");
		b.add("message.minecraft_mode.raid.problem.difficulty", ko ? "%s 난이도를 아직 클리어하지 않음" : "has not cleared %s yet");

		b.add("screen.minecraft_mode.raid.show_records", ko ? "기록 보기" : "Records");
		b.add("screen.minecraft_mode.raid.show_info", ko ? "정보 보기" : "Details");
		b.add("screen.minecraft_mode.raid.practice_fee", ko ? "참가비: 없음 (연습)" : "Fee: none (practice)");
		b.add("screen.minecraft_mode.raid.reset", ko ? "초기화 %s" : "Resets in %s");
		b.add("screen.minecraft_mode.raid.needs", ko ? "먼저 %s 난이도를 클리어해야 열립니다." : "Clear %s first to unlock this difficulty.");
		b.add("screen.minecraft_mode.raid.reward_open", ko ? "이번 주기 보상을 받을 수 있습니다." : "Reward available this cycle.");
		b.add("screen.minecraft_mode.raid.reward_taken", ko ? "이번 주기 보상을 이미 받았습니다 — 연습으로 참가 (참가비·보상 없음)."
			: "Reward taken this cycle - you join as practice (no fee, no reward).");
		b.add("screen.minecraft_mode.raid.difficulty_info", ko ? "보스 체력 %s, 피해 %s · 변형: %s" : "Boss health %s, damage %s · Modifiers: %s");
		b.add("screen.minecraft_mode.raid.records_title", ko ? "최단 클리어 · %s" : "Fastest clears · %s");
		b.add("screen.minecraft_mode.raid.no_records", ko ? "아직 기록이 없습니다." : "No clears yet.");
		b.add("screen.minecraft_mode.raid.record_day", ko ? "%s일차" : "Day %s");
		b.add("screen.minecraft_mode.raid.affix_title", ko ? "이번 주기 변형 (영웅·악몽)" : "This cycle's modifiers (Heroic, Nightmare)");
	}

	private static void bounties(final TranslationBuilder b, final boolean ko) {
		for (BountyKind kind : BountyKind.values()) {
			b.add(kind.key(), switch (kind) {
				case KILL_ANY -> ko ? "적대 몬스터 %s마리 처치" : "Defeat %s hostile monsters";
				case KILL_TYPE -> ko ? "%2$s %1$s마리 처치" : "Defeat %s %s";
				case KILL_NAMED -> ko ? "네임드 몬스터 %s마리 처치" : "Defeat %s named monsters";
				case CLEAR_LAIR -> ko ? "네임드 소굴 %s곳 정복" : "Conquer %s named lairs";
				case MINE_ORE -> ko ? "광석 %s개 채굴" : "Mine %s ores";
				case RAID -> ko ? "레이드 %s회 클리어" : "Clear %s raids";
				case DELIVER -> ko ? "%2$s %1$s개 납품" : "Deliver %s %s";
			});
		}
		for (Bounties.Offer offer : Bounties.SHOP) {
			b.add(offer.nameKey(), switch (offer.id()) {
				case "enhancement_stone" -> ko ? "강화석" : "Enhancement Stone";
				case "enhancement_stones" -> ko ? "강화석 5개" : "5 Enhancement Stones";
				case "protection_scroll" -> ko ? "보호 주문서" : "Protection Scroll";
				case "lair_map" -> ko ? "소굴 지도" : "Lair Map";
				case "condensed_essence" -> ko ? "응축된 정수 2개" : "2 Condensed Essence";
				case "ether" -> ko ? "진화의 에테르 10개" : "10 Evolution Ether";
				case "tier3" -> ko ? "3등급 소모품 2개" : "2 Tier-3 Supplies";
				case "tier4" -> ko ? "4등급 소모품" : "Tier-4 Supply";
				case "return_scrolls" -> ko ? "귀환 주문서 3개" : "3 Return Scrolls";
				case "pet_charm" -> ko ? "펫 부적 (무작위)" : "Pet Charm (random)";
				case "mount_whistle" -> ko ? "질풍마 호루라기" : "Swift Stallion Whistle";
				default -> offer.id();
			});
		}
		b.add("message.minecraft_mode.bounty.complete", ko ? "의뢰 완료: %s — 길드 접수원에게 보고하세요." : "Bounty complete: %s - report to the guild clerk.");
		b.add("message.minecraft_mode.bounty.claimed", ko ? "의뢰 보고 완료: %s (공적 +%s)" : "Bounty handed in: %s (+%s merit)");
		b.add("message.minecraft_mode.bounty.shop_owned", ko ? "이미 모두 가지고 있는 물건입니다." : "You already own everything this would give.");

		b.add("screen.minecraft_mode.bounty.title", ko ? "모험가 길드 의뢰" : "Adventurers' Guild Bounties");
		b.add("screen.minecraft_mode.bounty.merit", ko ? "공적 ★%s" : "Merit ★%s");
		b.add("screen.minecraft_mode.bounty.claim", ko ? "보고" : "Hand in");
		b.add("screen.minecraft_mode.bounty.done", ko ? "완료" : "Done");
		b.add("screen.minecraft_mode.bounty.none", ko ? "의뢰 없음" : "No bounty");
		b.add("screen.minecraft_mode.bounty.reward", ko ? "보상:" : "Reward:");
		b.add("screen.minecraft_mode.bounty.reward_merit", ko ? " ★ 공적 %s" : " ★ %s merit");
		b.add("screen.minecraft_mode.bounty.reward_ether", ko ? " ✦ 진화의 에테르 %s개" : " ✦ %s Evolution Ether");
		b.add("screen.minecraft_mode.bounty.reward_stones", ko ? " ◆ 강화석 %s개" : " ◆ %s Enhancement Stones");
		b.add("screen.minecraft_mode.bounty.deliver_hint", ko ? "보고할 때 인벤토리에서 가져갑니다." : "The items are taken from your inventory when you hand it in.");
		b.add("screen.minecraft_mode.bounty.kind_hint", ko ? "플레이하는 동안 자동으로 집계됩니다." : "Counts on its own while you play.");
		b.add("screen.minecraft_mode.bounty.daily_reset", ko ? "일일 갱신 %s" : "Dailies renew in %s");
		b.add("screen.minecraft_mode.bounty.cycle_reset", ko ? "★ 주기 갱신 %s" : "★ Cycle bounty in %s");
		b.add("screen.minecraft_mode.bounty.shop", ko ? "공적 상점" : "Merit Shop");
		b.add("screen.minecraft_mode.bounty.cost", ko ? "공적 %s 필요" : "Costs %s merit");
		b.add("screen.minecraft_mode.bounty.legend", ko ? "◎ 코인  ★ 공적  ✦ 에테르  ◆ 강화석" : "◎ coins  ★ merit  ✦ ether  ◆ stones");
	}

	private static void enhancement(final TranslationBuilder b, final boolean ko) {
		b.add(ModItems.ENHANCEMENT_STONE, ko ? "강화석" : "Enhancement Stone");
		b.add("item.minecraft_mode.enhancement_stone.tooltip", ko ? "강화 장인 브로크가 +6 이상 강화에 씁니다." : "Artisan Brokk uses it for +6 and higher.");
		b.add(ModItems.PROTECTION_SCROLL, ko ? "보호 주문서" : "Protection Scroll");
		b.add("item.minecraft_mode.protection_scroll.tooltip", ko ? "+11 이상 강화가 실패해도 단계를 지켜 줍니다 (지켜 줄 때만 소모)."
			: "Keeps the level when an enhancement to +11 or higher fails (used up only then).");
		b.add("container.minecraft_mode.enhance", ko ? "장비 강화" : "Enhancement");
		b.add("tooltip.minecraft_mode.enhance.level", ko ? "강화 +%s / %s" : "Enhancement +%s / %s");
		b.add("tooltip.minecraft_mode.enhance.pity", ko ? "장인의 기운 %s%%" : "artisan's spirit %s%%");
		b.add("message.minecraft_mode.enhance.success", ko ? "강화 성공! +%s" : "Enhancement succeeded! +%s");
		b.add("message.minecraft_mode.enhance.failed", ko ? "강화 실패 — +%s 유지 (장인의 기운 %s%%)" : "Enhancement failed - stays at +%s (artisan's spirit %s%%)");
		b.add("message.minecraft_mode.enhance.dropped", ko ? "강화 실패 — +%s로 하락 (장인의 기운 %s%%)" : "Enhancement failed - dropped to +%s (artisan's spirit %s%%)");
		b.add("message.minecraft_mode.enhance.saved", ko ? "강화 실패 — 보호 주문서가 +%s를 지켰습니다 (장인의 기운 %s%%)"
			: "Enhancement failed - the protection scroll kept +%s (artisan's spirit %s%%)");
		b.add("message.minecraft_mode.enhance.broadcast", ko ? "%s님이 %s +%s 강화에 성공했습니다!" : "%s enhanced %s to +%s!");
		b.add("screen.minecraft_mode.enhance.insert", ko ? "직업 무기나 방어구를 왼쪽 칸에 넣으세요. 강화할 때마다 무기는 피해, 방어구는 방어가 오릅니다 (최대 +15)."
			: "Put a class weapon or armor piece in the slot. Every level adds damage (weapons) or defense (armor), up to +15.");
		b.add("screen.minecraft_mode.enhance.max", ko ? "최대 강화에 도달했습니다." : "Fully enhanced.");
		b.add("screen.minecraft_mode.enhance.chance", ko ? "성공 %s%%" : "Success %s%%");
		b.add("screen.minecraft_mode.enhance.pity", ko ? "기본 %s%% + 장인의 기운 %s%%" : "Base %s%% + spirit %s%%");
		b.add("screen.minecraft_mode.enhance.coins", ko ? "코인" : "Coins");
		b.add("screen.minecraft_mode.enhance.risky", ko ? "실패하면 1단계 하락!" : "Failure drops a level!");
		b.add("screen.minecraft_mode.enhance.safe", ko ? "실패해도 단계 유지" : "Failure keeps the level");
		b.add("screen.minecraft_mode.enhance.button", ko ? "강화" : "Enhance");
		b.add("screen.minecraft_mode.enhance.protected", ko ? "보호 강화 (%s)" : "Protected (%s)");
		b.add("screen.minecraft_mode.enhance.next", ko ? "+%s 효과" : "At +%s");
		b.add("screen.minecraft_mode.enhance.protect_hint", ko ? "보호 강화: 실패해 하락할 때만 보호 주문서 1장 소모" : "Protected: a scroll is used only when it saves the level");
		b.add("screen.minecraft_mode.enhance.result.success", ko ? "성공!" : "Success!");
		b.add("screen.minecraft_mode.enhance.result.fail", ko ? "실패" : "Failed");
		b.add("screen.minecraft_mode.enhance.result.drop", ko ? "하락" : "Dropped");
		b.add("screen.minecraft_mode.enhance.result.saved", ko ? "보호됨" : "Saved");
	}

	private static void market(final TranslationBuilder b, final boolean ko) {
		b.add("message.minecraft_mode.market.not_sellable", ko ? "코인은 팔 수 없습니다." : "Coins cannot be sold.");
		b.add("message.minecraft_mode.market.full", ko ? "가방에 빈 칸이 없습니다." : "Your inventory has no free slot.");
		b.add("message.minecraft_mode.market.mail_left", ko ? "가방이 가득 차 %s개는 우편함에 남겨 두었습니다." : "%s item(s) stay in your mailbox - your inventory is full.");
		b.add("message.minecraft_mode.market.has_contents", ko ? "안에 물건이 든 상자나 꾸러미는 비우고 파세요." : "Empty boxes and bundles before selling them.");
		b.add("message.minecraft_mode.market.too_large", ko ? "데이터가 너무 큰 물건(긴 책 등)은 거래소에 올릴 수 없습니다." : "This item carries too much data (a long book?) to be listed.");
		b.add("message.minecraft_mode.market.bad_price", ko ? "가격은 1C에서 1000G 사이로 정하세요." : "Set a price between 1C and 1000G.");
		b.add("message.minecraft_mode.market.too_many", ko ? "이미 %s개를 등록했습니다." : "You already have %s listings up.");
		b.add("message.minecraft_mode.market.no_coins", ko ? "%s이(가) 필요합니다." : "You need %s.");
		b.add("message.minecraft_mode.market.listed", ko ? "%s x%s을(를) %s에 등록했습니다 (수수료 %s)." : "Listed %s x%s for %s (fee %s).");
		b.add("message.minecraft_mode.market.gone", ko ? "이미 없는 매물입니다." : "That listing is no longer there.");
		b.add("message.minecraft_mode.market.own", ko ? "내가 등록한 매물입니다." : "That is your own listing.");
		b.add("message.minecraft_mode.market.bought", ko ? "%s x%s을(를) %s에 샀습니다." : "Bought %s x%s for %s.");
		b.add("message.minecraft_mode.market.sold", ko ? "%s x%s이(가) 팔렸습니다! 대금 %s은 중개인 우편함에 있습니다." : "Your %s x%s sold! %s waits in your mailbox at the broker.");
		b.add("message.minecraft_mode.market.cancelled", ko ? "%s 등록을 취소했습니다." : "Took %s off the market.");
		b.add("message.minecraft_mode.market.no_mail", ko ? "우편함이 비어 있습니다." : "Your mailbox is empty.");
		b.add("message.minecraft_mode.market.claimed", ko ? "우편함에서 %s와 물품 %s개를 받았습니다." : "Collected %s and %s items from your mailbox.");

		b.add("screen.minecraft_mode.market.title", ko ? "거래소" : "Market");
		b.add("screen.minecraft_mode.market.tab.browse", ko ? "구매" : "Browse");
		b.add("screen.minecraft_mode.market.tab.sell", ko ? "판매" : "Sell");
		b.add("screen.minecraft_mode.market.tab.mine", ko ? "내 등록" : "Mine");
		b.add("screen.minecraft_mode.market.tab.mail", ko ? "우편함" : "Mailbox");
		b.add("screen.minecraft_mode.market.search", ko ? "검색..." : "Search...");
		b.add("screen.minecraft_mode.market.category.all", ko ? "전체" : "All");
		b.add("screen.minecraft_mode.market.category.gear", ko ? "장비" : "Gear");
		b.add("screen.minecraft_mode.market.category.consumable", ko ? "소모품" : "Consumables");
		b.add("screen.minecraft_mode.market.category.material", ko ? "재료·기타" : "Materials");
		b.add("screen.minecraft_mode.market.sort.cheap", ko ? "낮은 가격순" : "Cheapest");
		b.add("screen.minecraft_mode.market.sort.dear", ko ? "높은 가격순" : "Priciest");
		b.add("screen.minecraft_mode.market.sort.ending", ko ? "마감 임박순" : "Ending soon");
		b.add("screen.minecraft_mode.market.buy", ko ? "구매" : "Buy");
		b.add("screen.minecraft_mode.market.cancel", ko ? "취소" : "Cancel");
		b.add("screen.minecraft_mode.market.claim", ko ? "모두 받기" : "Collect all");
		b.add("screen.minecraft_mode.market.empty", ko ? "등록된 매물이 없습니다." : "Nothing for sale here.");
		b.add("screen.minecraft_mode.market.no_mine", ko ? "등록한 물건이 없습니다." : "You have nothing listed.");
		b.add("screen.minecraft_mode.market.left", ko ? "남은 시간 %s" : "%s left");
		b.add("screen.minecraft_mode.market.each", ko ? "개당 %s" : "%s each");
		b.add("screen.minecraft_mode.market.seller", ko ? "판매자: %s" : "Seller: %s");
		b.add("screen.minecraft_mode.market.mine", ko ? "등록: %s/%s" : "Listings: %s/%s");
		b.add("screen.minecraft_mode.market.loading", ko ? "불러오는 중..." : "Loading...");
		b.add("screen.minecraft_mode.market.results", ko ? "%s건" : "%s found");
		b.add("screen.minecraft_mode.market.pick", ko ? "팔 물건을 왼쪽 인벤토리에서 고르세요." : "Click an item in your inventory to sell it.");
		b.add("screen.minecraft_mode.market.price", ko ? "판매 가격" : "Price");
		b.add("screen.minecraft_mode.market.total", ko ? "합계: %s" : "Total: %s");
		b.add("screen.minecraft_mode.market.fee", ko ? "등록 수수료(%s%%): %s" : "Listing fee (%s%%): %s");
		b.add("screen.minecraft_mode.market.net", ko ? "팔리면 수령(-%s%%): %s" : "You get (-%s%%): %s");
		b.add("screen.minecraft_mode.market.duration", ko ? "등록 기간: %s" : "Listed for %s");
		b.add("screen.minecraft_mode.market.list", ko ? "판매 등록" : "List for sale");
		b.add("screen.minecraft_mode.market.sell_help", ko ? "물건을 고르면 지금 최저가가 자동으로 들어갑니다. 코인과 물건이 든 상자는 팔 수 없습니다."
			: "Picking an item fills in the cheapest current price. Coins and filled boxes cannot be sold.");
		b.add("screen.minecraft_mode.market.mail_coins", ko ? "판매 대금: %s" : "Proceeds: %s");
		b.add("screen.minecraft_mode.market.mail_items", ko ? "물품: %s개" : "Items: %s");
		b.add("screen.minecraft_mode.market.mail_help", ko ? "팔린 물건의 대금(수수료 5%% 제외)과 기간이 끝나거나 취소한 물건이 여기로 옵니다."
			: "Sales proceeds (minus the 5%% fee) and expired or cancelled items wait here.");
	}

	private static void progress(final TranslationBuilder b, final boolean ko) {
		for (Achievements.Achievement a : Achievements.all()) {
			b.add(a.nameKey(), ko ? a.ko() : a.en());
			b.add(a.descKey(), ko ? a.descKo() : a.descEn());
			if (a.hasTitle()) {
				b.add(a.titleKey(), ko ? a.titleKo() : a.titleEn());
			}
		}
		b.add("message.minecraft_mode.achievement.title", ko ? "업적 달성!" : "Achievement!");
		b.add("message.minecraft_mode.achievement.unlocked", ko ? "업적 달성: %s (공적 +%s)" : "Achievement unlocked: %s (+%s merit)");
		b.add("message.minecraft_mode.achievement.title_unlocked", ko ? "새 칭호: %s — 도감(J)에서 착용할 수 있습니다." : "New title: %s - wear it from the codex (J).");

		b.add("key.minecraft_mode.codex_screen", ko ? "도감" : "Codex");
		b.add("screen.minecraft_mode.codex.title", ko ? "도감" : "Codex");
		b.add("screen.minecraft_mode.codex.tab.codex", ko ? "도감" : "Codex");
		b.add("screen.minecraft_mode.codex.tab.achievements", ko ? "업적" : "Achievements");
		b.add("screen.minecraft_mode.codex.tab.titles", ko ? "칭호" : "Titles");
		b.add("screen.minecraft_mode.codex.named", ko ? "네임드 몬스터: 숙련 %s/%s (각 %s회 처치)" : "Named monsters: %s/%s mastered (%s kills each)");
		b.add("screen.minecraft_mode.codex.lairs", ko ? "정복한 소굴: %s/%s" : "Lairs conquered: %s/%s");
		b.add("screen.minecraft_mode.codex.unknown", ko ? "아직 만나지 못함" : "Not met yet");
		b.add("screen.minecraft_mode.codex.unknown_lair", ko ? "아직 정복하지 않음" : "Not conquered yet");
		b.add("screen.minecraft_mode.codex.level_range", "Lv %s-%s");
		b.add("screen.minecraft_mode.codex.kills", ko ? "처치: %s/%s" : "Defeated: %s/%s");
		b.add("screen.minecraft_mode.codex.clears", ko ? "%s회 정복" : "Conquered %s times");
		b.add("screen.minecraft_mode.codex.bonuses", ko ? "수집 보너스" : "Collection bonuses");
		b.add("screen.minecraft_mode.codex.no_bonus", ko ? "아직 없음: 네임드 숙련, 소굴 정복, 업적으로 쌓입니다." : "None yet: master named monsters, conquer lairs, earn achievements.");
		b.add("screen.minecraft_mode.codex.achieved", ko ? "달성한 업적: %s/%s" : "Achievements: %s/%s");
		b.add("screen.minecraft_mode.codex.merit", ko ? "보상: 공적 %s" : "Reward: %s merit");
		b.add("screen.minecraft_mode.codex.title_reward", ko ? "칭호: %s" : "Title: %s");
		b.add("screen.minecraft_mode.codex.title_from", ko ? "획득: %s" : "From: %s");
		b.add("screen.minecraft_mode.codex.wear", ko ? "착용" : "Wear");
		b.add("screen.minecraft_mode.codex.worn", ko ? "착용 중" : "Worn");
		b.add("screen.minecraft_mode.codex.take_off", ko ? "칭호 해제" : "Remove title");
		b.add("screen.minecraft_mode.codex.no_title", ko ? "착용한 칭호 없음" : "No title worn");
		b.add("screen.minecraft_mode.codex.wearing", ko ? "착용 중: %s" : "Wearing: %s");
	}

	private static void talents(final TranslationBuilder b, final boolean ko) {
		for (JobClass job : JobClass.PLAYABLE) {
			for (TalentTree.Branch branch : TalentTree.of(job)) {
				b.add(branch.nameKey(), ko ? branch.ko() : branch.en());
				for (TalentTree.Node node : branch.nodes()) {
					b.add(node.nameKey(), ko ? node.ko() : node.en());
				}
			}
		}
		b.add("key.minecraft_mode.talent_screen", ko ? "특성" : "Talents");
		b.add("message.minecraft_mode.talent.no_coins", ko ? "특성 초기화에는 %s이(가) 필요합니다." : "You need %s to reset your talents.");
		b.add("message.minecraft_mode.talent.reset", ko ? "특성을 초기화했습니다." : "Your talents were reset.");
		b.add("screen.minecraft_mode.talent.title", ko ? "특성" : "Talents");
		b.add("screen.minecraft_mode.talent.no_class", ko ? "먼저 스톰홀드의 직업 교관에게서 직업을 고르세요. 특성 포인트는 레벨에서 나옵니다 (10레벨 이후 3레벨마다 2점)."
			: "Choose a class at a trainer in Stormhold first. Talent points come from levels (two for every three levels after 10).");
		b.add("screen.minecraft_mode.talent.points", ko ? "포인트: %s / %s" : "Points: %s / %s");
		b.add("screen.minecraft_mode.talent.reset", ko ? "초기화 (%s)" : "Reset (%s)");
		b.add("screen.minecraft_mode.talent.locked", ko ? "%s점 필요" : "needs %s");
		b.add("screen.minecraft_mode.talent.locked_hint", ko ? "이 계열에 %s점을 넣으면 열립니다." : "Put %s points in this branch to open this tier.");
		b.add("screen.minecraft_mode.talent.rank", ko ? "랭크 %s/%s" : "Rank %s/%s");
		b.add("screen.minecraft_mode.talent.now", ko ? "현재:" : "Now:");
		b.add("screen.minecraft_mode.talent.next", ko ? "다음 랭크:" : "Next rank:");
	}

	private EndgameLang() {
	}
}
