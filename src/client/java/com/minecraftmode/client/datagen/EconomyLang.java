package com.minecraftmode.client.datagen;

import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;

/** Translations for the wallet, coin costs and raid fees. */
final class EconomyLang {
	static void add(final TranslationBuilder b, final boolean ko) {
		b.add("screen.minecraft_mode.wallet", ko ? "지갑: %s" : "Wallet: %s");
		String w = "message.minecraft_mode.wallet.";
		b.add(w + "balance", ko ? "지갑: %s" : "Wallet: %s");
		b.add(w + "bad_amount", ko ? "금액을 확인하세요. 예: 3g, 2g 5s, 40s, 120" : "Check the amount. Examples: 3g, 2g 5s, 40s, 120");
		b.add(w + "short", ko ? "동전이 부족합니다 (%s 필요, 보유 %s)." : "Not enough coins (%s needed, you have %s).");
		b.add(w + "paid", ko ? "%s을(를) %s님에게 보냈습니다." : "Sent %s to %s.");
		b.add(w + "received", ko ? "%s을(를) %s님에게서 받았습니다." : "Received %s from %s.");
		b.add("item.minecraft_mode.coin.wallet_hint", ko ? "주우면 지갑으로 들어갑니다" : "Goes straight into your wallet");
		b.add("screen.minecraft_mode.engraving.coins", ko ? "동전 %s" : "Coins %s");
		b.add("screen.minecraft_mode.upgrade.reroll", ko ? "추가 옵션 다시 굴리기" : "Roll the extra options again");
		b.add("screen.minecraft_mode.upgrade.reroll_essence", ko ? "응축 정수 %s개 (보유 %s, 정수 9개 = 1개)" : "Condensed essence %s (you have %s; 9 essence = 1)");
		b.add("screen.minecraft_mode.upgrade.reroll_hint", ko ? "새 옵션은 따로 보관됩니다. ✔로 적용하거나 ✖로 기존 옵션을 유지하세요." : "The new options wait beside the old ones: ✔ applies them, ✖ keeps the old ones.");
		b.add("screen.minecraft_mode.upgrade.apply", ko ? "새 옵션 적용" : "Take the new options");
		b.add("screen.minecraft_mode.upgrade.keep", ko ? "기존 옵션 유지 (새 옵션 버림)" : "Keep the old options (discard the new ones)");
		b.add("screen.minecraft_mode.upgrade.tab_evolve", ko ? "진화" : "Evolve");
		b.add("screen.minecraft_mode.upgrade.tab_options", ko ? "추가 옵션" : "Options");
		b.add("screen.minecraft_mode.upgrade.current", ko ? "현재 옵션" : "Current options");
		b.add("screen.minecraft_mode.upgrade.pending", ko ? "새 옵션" : "New options");
		b.add("screen.minecraft_mode.upgrade.no_pending", ko ? "⟳로 새 옵션을 굴려 보세요. 고르기 전까지 기존 옵션이 유지됩니다." : "Roll with ⟳. The old options stay until you choose.");
		b.add("screen.minecraft_mode.upgrade.armor_only", ko ? "추가 옵션은 직업 방어구와 레이드 장신구에만 있습니다. 칸에 넣으세요." : "Only class armor and raid accessories have extra options. Put one in the slot.");
		b.add("screen.minecraft_mode.upgrade.accessory", ko ? "3티어 장신구는 더 계승할 곳이 없습니다. 추가 옵션 탭에서 옵션을 다시 굴릴 수 있습니다 (줄 수는 그대로)."
			: "Tier 3 accessories go no further. Roll their options again in the Options tab (same number of lines).");
		b.add("screen.minecraft_mode.upgrade.inherit_cost", ko ? "Lv %s · %s티어 계승권" : "Lv %s · tier %s ticket");
		b.add("screen.minecraft_mode.upgrade.inherit_keeps", ko ? "%s 1장: 강화·장인의 기운·추가 옵션(새 레벨에 맞춰)이 이어집니다"
			: "1 %s: enhancement, artisan's spirit and options (at the new level) carry over");
		b.add("screen.minecraft_mode.buyback.title", ko ? "재구매" : "Buy back");
		b.add("screen.minecraft_mode.buyback.price", ko ? "재구매 가격 %s (판매가 %s)" : "Buy back for %s (sold for %s)");
		b.add("screen.minecraft_mode.buyback.click", ko ? "클릭해서 되사기 · 최근 판매 10개까지" : "Click to buy it back · last 10 sales");
		b.add("message.minecraft_mode.buyback.full", ko ? "가방과 인벤토리에 자리가 없습니다." : "No room in your bags or inventory.");
		b.add("message.minecraft_mode.buyback.short", ko ? "동전이 부족합니다 (%s 필요)." : "Not enough coins (%s needed).");
		String s = "screen.minecraft_mode.sell_all.";
		b.add(s + "one", ko ? "이 품목 다 팔기" : "Sell this");
		b.add(s + "all", ko ? "전부 팔기" : "Sell all");
		b.add(s + "confirm", ko ? "다시 눌러 확인" : "Click again");
		b.add(s + "one.title", ko ? "고른 품목을 가진 만큼 모두 팝니다" : "Sells all you carry of the chosen line");
		b.add(s + "all.title", ko ? "이 상점이 사는 물건을 전부 팝니다 (두 번 눌러 판매)" : "Sells everything this shop buys (click twice)");
		b.add(s + "line", ko ? "%s %s개 → 약 %s" : "%2$s %1$s → about %3$s");
		b.add(s + "more", ko ? "외 %s종" : "and %s more kinds");
		b.add(s + "total", ko ? "합계 %s개 → 약 %s" : "In all %s items → about %s");
		b.add(s + "pick", ko ? "파는 줄(물건을 주고 동전을 받는 거래)을 고르세요." : "Pick a line that pays coins for goods.");
		b.add(s + "nothing", ko ? "팔 물건이 없습니다." : "Nothing to sell.");
		b.add(s + "rules", ko ? "가방·인벤토리·거래 칸에서 팝니다. 단축바와 이름·인챈트·손상이 있는 물건은 팔지 않고, 팔수록 시세가 내려가는 것은 하나씩 팔 때와 같습니다."
			: "Sells from your bags, inventory and the trade slots. The hotbar and named, enchanted or damaged items stay, and prices fall as you sell, as with single trades.");
		b.add(s + "no_essence", ko ? "정수는 '전부 팔기'에서 빠집니다." : "Essence is never part of 'Sell all'.");
		String m = "message.minecraft_mode.sell_all.";
		b.add(m + "one", ko ? "%s %s개를 팔아 %s을(를) 받았습니다." : "Sold %2$s %1$s for %3$s.");
		b.add(m + "many", ko ? "%s종 %s개를 팔아 %s을(를) 받았습니다." : "Sold %2$s items of %1$s kinds for %3$s.");
		b.add(m + "none", ko ? "팔 수 있는 물건이 없습니다. 단축바와 이름·인챈트가 붙은 물건은 이렇게 팔지 않습니다."
			: "Nothing to sell. The hotbar and named or enchanted items are never sold this way.");
		String lb = "screen.minecraft_mode.loot_buyer.";
		b.add(lb + "title", ko ? "전리품 상인" : "Loot Buyer");
		b.add(lb + "help", ko ? "팔 장비를 클릭해 고르세요" : "Click the pieces to sell");
		b.add(lb + "all", ko ? "전체 선택" : "Select all");
		b.add(lb + "others", ko ? "다른 직업 장비" : "Other classes");
		b.add(lb + "clear", ko ? "선택 해제" : "Clear");
		b.add(lb + "sell", ko ? "선택한 %s개 판매" : "Sell %s picked");
		b.add(lb + "confirm", ko ? "%s에 판매 — 다시 눌러 확인" : "Sell for %s — click again");
		b.add(lb + "total", ko ? "선택 %s / %s개 · 합계 %s" : "Picked %s of %s · %s in all");
		b.add(lb + "warning", ko ? "강화·각인되었거나 단축바에 있는 장비 %s개가 포함되어 있습니다!" : "%s enhanced, engraved or hotbar piece(s) picked!");
		b.add(lb + "legend", ko ? "파란 칸: 다른 직업 · 빨간 점: 전체 선택에서 빠짐" : "Blue: other class · red dot: left out of Select all");
		b.add(lb + "empty", ko ? "팔 수 있는 직업 장비나 장신구가 없습니다." : "You carry no class gear or accessories to sell.");
		b.add(lb + "price", ko ? "매입가 %s (길드 시세의 30%%)" : "Pays %s (30%% of the guild price)");
		b.add(lb + "kept", ko ? "강화·각인 가치는 값에 들어가지 않습니다" : "Enhancement and engravings are not paid for");
		b.add(lb + "picked", ko ? "선택됨 · 클릭해서 빼기" : "Picked · click to drop");
		b.add(lb + "click", ko ? "클릭해서 고르기" : "Click to pick");
		b.add("message.minecraft_mode.loot_buyer.sold", ko ? "장비 %s개를 팔아 %s을(를) 받았습니다. 실수했다면 상점의 재구매 칸(최근 10개)에서 1.5배에 되살 수 있습니다."
			: "Sold %s pieces for %s. A shop's buyback panel (last 10 sales) returns a mistake at 1.5x.");
		b.add("message.minecraft_mode.loot_buyer.moved", ko ? "%s개는 그새 옮겨져 팔지 않았습니다." : "%s piece(s) had moved and were not sold.");
		b.add("tooltip.minecraft_mode.gear.pending",ko ? "새 옵션 (대장장이에게서 선택 대기)" : "New options (waiting for your choice at the blacksmith)");
	}

	private EconomyLang() {
	}
}
