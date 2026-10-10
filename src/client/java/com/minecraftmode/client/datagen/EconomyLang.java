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
		b.add("screen.minecraft_mode.upgrade.armor_only", ko ? "추가 옵션은 직업 방어구에만 있습니다. 방어구를 칸에 넣으세요." : "Only class armor has extra options. Put a piece in the slot.");
		b.add("screen.minecraft_mode.buyback.title", ko ? "재구매" : "Buy back");
		b.add("screen.minecraft_mode.buyback.price", ko ? "재구매 가격 %s (판매가 %s)" : "Buy back for %s (sold for %s)");
		b.add("screen.minecraft_mode.buyback.click", ko ? "클릭해서 되사기 · 최근 판매 10개까지" : "Click to buy it back · last 10 sales");
		b.add("message.minecraft_mode.buyback.full", ko ? "가방과 인벤토리에 자리가 없습니다." : "No room in your bags or inventory.");
		b.add("message.minecraft_mode.buyback.short", ko ? "동전이 부족합니다 (%s 필요)." : "Not enough coins (%s needed).");
		b.add("tooltip.minecraft_mode.gear.pending",ko ? "새 옵션 (대장장이에게서 선택 대기)" : "New options (waiting for your choice at the blacksmith)");
	}

	private EconomyLang() {
	}
}
