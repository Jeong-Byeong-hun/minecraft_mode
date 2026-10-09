package com.minecraftmode.client.datagen;

import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMobs;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;

/** Translations for named monsters and raid bosses. */
final class MonsterLang {
	static void add(final TranslationBuilder b, final boolean ko) {
		for (NamedDef def : NamedMobs.all()) {
			b.add(NamedMobs.type(def), ko ? def.ko() : def.en());
			b.add(def.descKey(), ko ? def.descKo() : def.descEn());
			b.add(NamedMobs.egg(def), (ko ? def.ko() : def.en()) + (ko ? " 생성 알" : " Spawn Egg"));
		}
		b.add("message.minecraft_mode.named.drop", ko ? "%s이(가) %s을(를) 떨어뜨렸습니다!" : "%s dropped %s!");
	}

	private MonsterLang() {
	}
}
