package com.minecraftmode.client.datagen;

import com.minecraftmode.consumable.ConsumableDef;
import com.minecraftmode.consumable.Consumables;
import com.minecraftmode.registry.ModEffects;
import com.minecraftmode.registry.ModItems;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;

/** Translations for consumables, their buff effects and tooltips. */
final class ConsumableLang {
	static void add(final TranslationBuilder b, final boolean ko) {
		for (ConsumableDef def : Consumables.all()) {
			b.add(Consumables.item(def), ko ? def.ko() : def.en());
			b.add(def.flavorKey(), ko ? def.flavorKo() : def.flavorEn());
		}
		b.add(ModItems.RETURN_SCROLL, ko ? "귀환 주문서" : "Return Scroll");
		b.add("item.minecraft_mode.return_scroll.tooltip", ko ? "5초 동안 움직이지 않고 읽으면 수도 광장으로 돌아갑니다 (레이드 불가)."
			: "Read it for 5 seconds without moving to return to the capital (not in raids).");
		b.add("message.minecraft_mode.return_scroll.reading", ko ? "주문서를 읽는 중... 움직이지 마세요." : "Reading the scroll... do not move.");
		b.add("message.minecraft_mode.return_scroll.moved", ko ? "움직여서 귀환이 취소되었습니다." : "You moved; the return was cancelled.");
		b.add("message.minecraft_mode.return_scroll.raid", ko ? "레이드 중에는 쓸 수 없습니다." : "It does not work in a raid.");
		b.add("message.minecraft_mode.consumable.revived", ko ? "피닉스의 깃털이 타오르며 다시 일어났습니다!" : "The Phoenix Feather burns and you rise again!");

		effect(b, ModEffects.FURY, ko ? "격노" : "Fury");
		effect(b, ModEffects.IRONSKIN, ko ? "철갑" : "Ironskin");
		effect(b, ModEffects.PRECISION, ko ? "정밀" : "Precision");
		effect(b, ModEffects.ARCANA, ko ? "비전" : "Arcana");
		effect(b, ModEffects.FOCUS, ko ? "집중" : "Focus");
		effect(b, ModEffects.CLARITY, ko ? "명경" : "Clarity");
		effect(b, ModEffects.REJUVENATION, ko ? "재생력" : "Rejuvenation");
		effect(b, ModEffects.FORTUNE, ko ? "행운" : "Fortune");
		effect(b, ModEffects.WISDOM, ko ? "지혜" : "Wisdom");
		effect(b, ModEffects.SLAYER, ko ? "사냥꾼" : "Slayer");

		String k = "consumable.minecraft_mode.";
		b.add(k + "heal", ko ? "체력 %s%% 즉시 회복" : "Restores %s%% health at once");
		b.add(k + "mana", ko ? "MP %s%% 즉시 회복" : "Restores %s%% MP at once");
		b.add(k + "cleanse", ko ? "해로운 효과 모두 제거" : "Removes every harmful effect");
		b.add(k + "effect", ko ? "%s (%s분)" : "%s (%s min)");
		b.add(k + "revive", ko ? "지니고 있으면 죽을 피해를 받을 때 체력 50%로 부활 (소모)" : "While carried: survive a lethal blow at 50% health (used up)");
		b.add(k + "exp", ko ? "직업 경험치: 다음 레벨까지 필요한 양의 절반" : "Class EXP: half of what your level needs");
		b.add(k + "star", ko ? "8초 동안 무적(저항 V)과 신속 II" : "8 seconds of invulnerability (Resistance V) and Speed II");
		b.add(k + "feast", ko ? "배고픔을 가득 채움" : "Fills your hunger completely");
		b.add(k + "cooldown", ko ? "재사용 대기 %s초 (%s 공유)" : "%ss cooldown (shared: %s)");
		for (String[] g : new String[][] {{"heal", "Healing", "체력 회복"}, {"mana", "MP", "MP 회복"}, {"cure", "Cures", "해독"}, {"snack", "Snacks", "간식"},
			{"star", "Invulnerability", "무적"}, {"revive", "Revival", "부활"}}) {
			b.add(k + "group." + g[0], ko ? g[2] : g[1]);
		}
	}

	private static void effect(final TranslationBuilder b, final Holder<MobEffect> effect, final String name) {
		b.add(effect.value().getDescriptionId(), name);
	}

	private ConsumableLang() {
	}
}
