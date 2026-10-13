package com.minecraftmode.client.datagen;

import com.minecraftmode.accessory.Accessories;
import com.minecraftmode.accessory.AccessoryDef;
import com.minecraftmode.accessory.AccessorySet;
import com.minecraftmode.accessory.AccessorySlot;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;

/** The raid accessories (0.5.24): slots, sets, item names, the accessory window and tooltips. */
final class AccessoryLang {
	static void add(final TranslationBuilder b, final boolean ko) {
		for (AccessorySlot slot : AccessorySlot.values()) {
			b.add(slot.nameKey(), ko ? slot.ko() : slot.en());
		}
		for (AccessorySet set : Accessories.sets()) {
			b.add(set.nameKey(), ko ? set.adjKo() + " 장신구" : set.adjEn() + " Jewelry");
		}
		for (AccessoryDef def : Accessories.all()) {
			b.add(def.nameKey(), ko ? def.set().adjKo() + " " + def.slot().ko() : def.set().adjEn() + " " + def.slot().en());
		}
		b.add("container.minecraft_mode.accessories", ko ? "장신구" : "Accessories");
		String s = "screen.minecraft_mode.accessories.";
		b.add(s + "button", ko ? "보석" : "Gem");
		b.add(s + "tooltip", ko ? "장신구 (목걸이·귀걸이·반지·팔찌·부적)" : "Accessories (necklace, earring, ring, bracelet, talisman)");
		b.add(s + "open", ko ? "장신구" : "Jewelry");
		b.add(s + "hint", ko ? "레이드 보스 전용 장신구" : "From raid bosses only");
		b.add(s + "none", ko ? "착용한 장신구 세트가 없습니다." : "No accessory set worn.");
		b.add(s + "set", ko ? "%s %s/%s" : "%s %s/%s");
		b.add(s + "raid_only", ko ? "레이드 보스 처치 분배로만 얻습니다." : "Only from raid boss loot.");
		String t = "tooltip.minecraft_mode.accessory.";
		b.add(t + "source", ko ? "레이드 전용 · %s" : "Raid only · %s");
		b.add(t + "wear", ko ? "인벤토리 오른쪽 \"보석\" 버튼이나 캐릭터 창(I)에서 착용" : "Wear it from the \"Gem\" button by the inventory or the character screen (I)");
	}

	private AccessoryLang() {
	}
}
