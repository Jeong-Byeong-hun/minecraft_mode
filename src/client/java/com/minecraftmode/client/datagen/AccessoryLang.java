package com.minecraftmode.client.datagen;

import com.minecraftmode.accessory.Accessories;
import com.minecraftmode.accessory.AccessoryDef;
import com.minecraftmode.accessory.AccessoryEnhance;
import com.minecraftmode.accessory.AccessorySet;
import com.minecraftmode.accessory.AccessorySlot;
import com.minecraftmode.registry.ModItems;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;
import net.minecraft.world.item.Item;

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
		b.add(t + "tier", ko ? "%s티어" : "Tier %s");
		b.add(t + "enhanced", ko ? "고정·추가 옵션 수치 +%s%%" : "Fixed line and options +%s%%");
		b.add(ModItems.ACCESSORY_ORB, ko ? "장신구 강화 구슬" : "Accessory Orb");
		b.add(ModItems.ACCESSORY_ORB.getDescriptionId() + ".tooltip", ko
			? "장인 브록의 강화대에서 레이드 장신구를 강화합니다 (최대 +" + AccessoryEnhance.MAX + "). 영웅·악몽 레이드 클리어 보상."
			: "Enhances raid accessories at Artisan Brokk's bench (up to +" + AccessoryEnhance.MAX + "). From Heroic and Nightmare raid clears.");
		b.add(t + "inherit", ko ? "%s: 대장장이에서 %s티어 장신구로 계승 (강화·옵션 유지)" : "%s: inherit into a tier %s accessory at the blacksmith (enhancement and options kept)");
		for (int tier = 1; tier < Accessories.PEAK_TIER; tier++) {
			Item ticket = Accessories.inheritTicket(tier);
			b.add(ticket, ko ? tier + "티어 장신구 계승권" : "Tier " + tier + " Accessory Inheritance Ticket");
			b.add(ticket.getDescriptionId() + ".tooltip", ko
				? "대장장이 진화대에서 " + tier + "티어 레이드 장신구를 같은 부위의 " + (tier + 1) + "티어 장신구로 바꿉니다 (세트 선택, 강화·장인의 기운·추가 옵션 유지)."
				: "At the blacksmith's bench, turns a tier " + tier + " raid accessory into the same slot of a tier " + (tier + 1)
					+ " set of your choice (enhancement, artisan's spirit and options kept).");
		}
		b.add(t + "wear", ko ? "인벤토리 오른쪽 \"보석\" 버튼이나 캐릭터 창(I)에서 착용" : "Wear it from the \"Gem\" button by the inventory or the character screen (I)");
	}

	private AccessoryLang() {
	}
}
