package com.minecraftmode.client.datagen;

import com.minecraftmode.entity.CityNpc;
import com.minecraftmode.registry.ModEntities;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;

/** Translations for city service NPCs, gear evolution, drops and loot distribution. */
final class LootLang {
	static void add(final TranslationBuilder b, final boolean ko) {
		b.add(ModEntities.CITY_NPC, ko ? "도시 NPC" : "City NPC");
		for (CityNpc.Role role : CityNpc.Role.values()) {
			b.add(role.nameKey(), ko ? role.ko : role.en);
			b.add(role.greetingKey(), ko ? role.greetingKo : role.greetingEn);
		}

		b.add("container.minecraft_mode.upgrade", ko ? "장비 진화" : "Gear Evolution");
		b.add("screen.minecraft_mode.upgrade.insert", ko
			? "직업 무기나 방어구를 왼쪽 칸에 넣으세요. 같은 직업·종류의 다음 장비로 바꿀 수 있습니다 (해당 레벨대 진화의 에테르 %s~%s개)."
			: "Put a class weapon or armor piece in the slot to turn it into the next piece of its kind (%s-%s Evolution Ether of that bracket).");
		b.add("item.minecraft_mode.evolution_ether.cost", ko ? "Lv.%s 장비 진화에 %s개" : "Evolving into Lv %s gear takes %s");
		b.add("item.minecraft_mode.evolution_ether.fuse", ko ? "우클릭: %s개 → Lv.%s 에테르 1개" : "Use: %s → 1 Lv %s Ether");
		b.add("item.minecraft_mode.evolution_ether.split", ko ? "웅크리고 우클릭: 1개 → Lv.%2$s 에테르 %1$s개" : "Sneak-use: 1 → %s Lv %s Ether");
		b.add("message.minecraft_mode.ether.fused", ko ? "진화의 에테르 (Lv.%2$s) %1$s개로 합쳤습니다" : "Fused into %s Evolution Ether (Lv %s)");
		b.add("message.minecraft_mode.ether.split", ko ? "진화의 에테르 (Lv.%2$s) %1$s개로 나눴습니다" : "Broken into %s Evolution Ether (Lv %s)");
		b.add("message.minecraft_mode.ether.fuse_need", ko ? "합치려면 같은 등급 에테르 %s개를 한 칸에 들고 있어야 합니다" : "Hold %s Ether of one grade to fuse them");
		b.add("message.minecraft_mode.ether.fuse_max", ko ? "가장 높은 등급이라 더 합칠 수 없습니다" : "This is the highest grade");
		b.add("message.minecraft_mode.ether.split_min", ko ? "가장 낮은 등급이라 더 나눌 수 없습니다" : "This is the lowest grade");
		b.add("screen.minecraft_mode.upgrade.max", ko ? "더 진화할 장비가 없습니다." : "There is nothing further to evolve into.");
		b.add("screen.minecraft_mode.upgrade.cost", ko ? "Lv %s · 에테르(Lv.%s)" : "Lv %s · Ether (Lv %s)");
		b.add("screen.minecraft_mode.upgrade.keeps", ko ? "맞는 각인은 유지, 방어구 추가 옵션은 새로 결정" : "Fitting engravings are kept; armor options are rolled again");
	}

	private LootLang() {
	}
}
