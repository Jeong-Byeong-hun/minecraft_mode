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
			? "직업 무기나 방어구를 왼쪽 칸에 넣으세요. 같은 직업·종류의 다음 장비로 바꿀 수 있습니다 (해당 레벨대 진화의 에테르 %s개)."
			: "Put a class weapon or armor piece in the slot to turn it into the next piece of its kind (%s Evolution Ether of that bracket).");
		b.add("screen.minecraft_mode.upgrade.max", ko ? "더 진화할 장비가 없습니다." : "There is nothing further to evolve into.");
		b.add("screen.minecraft_mode.upgrade.cost", ko ? "Lv %s · 에테르(Lv.%s)" : "Lv %s · Ether (Lv %s)");
		b.add("screen.minecraft_mode.upgrade.keeps", ko ? "맞는 각인은 유지, 방어구 추가 옵션은 새로 결정" : "Fitting engravings are kept; armor options are rolled again");
	}

	private LootLang() {
	}
}
