package com.minecraftmode.client.datagen;

import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.worldgen.lair.LairDef;
import com.minecraftmode.worldgen.lair.NamedLairs;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;

/** Translations for named monsters, their lairs and raid bosses. */
final class MonsterLang {
	static void add(final TranslationBuilder b, final boolean ko) {
		for (NamedDef def : NamedMobs.all()) {
			b.add(NamedMobs.type(def), ko ? def.ko() : def.en());
			b.add(def.descKey(), ko ? def.descKo() : def.descEn());
			b.add(NamedMobs.egg(def), (ko ? def.ko() : def.en()) + (ko ? " 생성 알" : " Spawn Egg"));
		}
		b.add("message.minecraft_mode.named.drop", ko ? "%s이(가) %s을(를) 떨어뜨렸습니다!" : "%s dropped %s!");
		for (LairDef lair : NamedLairs.all()) {
			b.add(lair.nameKey(), ko ? lair.ko() : lair.en());
		}
		b.add("message.minecraft_mode.lair.enter", ko ? "%s의 소굴 · Lv %s-%s" : "Lair of the %s · Lv %s-%s");
	}

	private MonsterLang() {
	}
}
