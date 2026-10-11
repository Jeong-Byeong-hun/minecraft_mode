package com.minecraftmode.client.datagen;

import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;

/** Translations for the adventurer's menu (party, paying, settings, return home). */
final class MenuLang {
	static void add(final TranslationBuilder b, final boolean ko) {
		menu(b, ko);
		b.add("message.minecraft_mode.wallet.not_online", ko ? "%s님은 접속해 있지 않습니다." : "%s is not online.");
	}

	private static void menu(final TranslationBuilder b, final boolean ko) {
		String s = "screen.minecraft_mode.menu.";
		b.add("key.minecraft_mode.menu", ko ? "모험가 메뉴" : "Adventurer's menu");
		b.add(s + "title", ko ? "모험가 메뉴" : "Adventurer's Menu");
		b.add(s + "party", ko ? "파티" : "Party");
		b.add(s + "pay", ko ? "송금" : "Pay");
		b.add(s + "settings", ko ? "설정" : "Settings");
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
	}

	private MenuLang() {
	}
}
