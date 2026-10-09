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
	}

	private EconomyLang() {
	}
}
