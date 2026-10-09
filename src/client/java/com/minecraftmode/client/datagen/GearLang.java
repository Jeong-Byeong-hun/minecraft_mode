package com.minecraftmode.client.datagen;

import com.minecraftmode.job.gear.ArmorPieceDef;
import com.minecraftmode.job.gear.ArmorSetDef;
import com.minecraftmode.job.gear.ArmorStyle;
import com.minecraftmode.job.gear.ClassAbilities;
import com.minecraftmode.job.gear.ClassArmor;
import com.minecraftmode.job.gear.GearSlot;
import com.minecraftmode.registry.ModItems;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;

/** Translations for class armor, set bonuses, innate abilities and level rewards. */
final class GearLang {
	static void add(final TranslationBuilder b, final boolean ko) {
		for (ArmorSetDef set : ClassArmor.sets()) {
			b.add(set.nameKey(), ko ? set.ko() : set.en());
			ArmorStyle style = set.style();
			for (ArmorPieceDef piece : ClassArmor.piecesOf(set)) {
				b.add(piece.nameKey(), ko ? set.ko() + " " + style.pieceKo(piece.slot()) : set.en() + " " + style.pieceEn(piece.slot()));
			}
		}
		String[][] slots = {{"Weapon", "무기"}, {"Head", "머리"}, {"Chest", "몸통"}, {"Legs", "다리"}, {"Feet", "발"}};
		for (GearSlot slot : GearSlot.values()) {
			b.add(slot.nameKey(), slots[slot.ordinal()][ko ? 1 : 0]);
		}

		b.add("tooltip.minecraft_mode.gear.locked", ko ? "착용 조건 미충족: 입을 수 없음" : "Requirements not met: cannot be worn");
		b.add("tooltip.minecraft_mode.gear.set", ko ? "%s 세트 (%s/4 착용)" : "%s set (%s/4 worn)");
		b.add("tooltip.minecraft_mode.gear.base", ko ? "기본 옵션" : "Base option");
		b.add("tooltip.minecraft_mode.gear.options", ko ? "추가 옵션" : "Extra options");
		b.add("tooltip.minecraft_mode.gear.unrolled", ko ? " 획득하면 %s줄이 정해집니다" : " %s line(s), rolled when obtained");
		b.add("tooltip.minecraft_mode.gear.set_bonus", ko ? "%s 세트 효과" : "%s set bonus");
		b.add("tooltip.minecraft_mode.gear.bracket", ko ? "%s레벨대 장비" : "Level %s bracket");
		b.add("message.minecraft_mode.gear.cannot_wear", ko ? "%s 전용 방어구입니다 (요구 Lv %s, 해당 차수 필요)" : "Only a %s can wear this (Lv %s and its tier)");

		b.add(ModItems.EVOLUTION_ETHER, ko ? "진화의 에테르" : "Evolution Ether");
		b.add("item.minecraft_mode.evolution_ether.graded", ko ? "진화의 에테르 (Lv.%s)" : "Evolution Ether (Lv %s)");

		b.add("key.minecraft_mode.innate_ability", ko ? "직업 고유 기술" : "Innate Class Ability");
		for (ClassAbilities.Ability ability : ClassAbilities.Ability.values()) {
			b.add(ability.nameKey(), ko ? ability.ko : ability.en);
			b.add(ability.descKey(), ko ? ability.descKo : ability.descEn);
		}
		b.add("message.minecraft_mode.ability.no_class", ko ? "직업이 있어야 고유 기술을 쓸 수 있습니다." : "You need a class to use an innate ability.");
		b.add("message.minecraft_mode.ability.locked", ko ? "레벨 %s에 직업 고유 기술이 열립니다." : "Your innate ability unlocks at level %s.");
		b.add("message.minecraft_mode.ability.no_anchor", ko ? "갈고리를 걸 곳이 없습니다." : "Nothing to hook onto.");
		b.add("message.minecraft_mode.ability.en", ko ? "원: 적 %s명의 기척을 느꼈습니다" : "En: you sense %s enemies");

		b.add("screen.minecraft_mode.job.level_bonus", ko ? "레벨 보너스" : "Level bonus");
		b.add("screen.minecraft_mode.job.innate", ko ? "고유 기술 [%s] %s" : "Innate [%s] %s");
		b.add("screen.minecraft_mode.job.innate_locked", ko ? "고유 기술: Lv %s에 해금" : "Innate ability: unlocks at Lv %s");
		b.add("screen.minecraft_mode.job.sets", ko ? "세트 효과" : "Set bonuses");
	}

	private GearLang() {
	}
}
