package com.minecraftmode.client.datagen;

import com.minecraftmode.enhance.ImbueSlot;
import com.minecraftmode.registry.ModItems;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;

/**
 * Translations for tier lairs (0.5.49): tier maps and their lairs ({@code worldgen/lair/TierLairs}), the map combiner, pet evolution and
 * imbue scrolls.
 */
final class TierLang {
	private TierLang() {
	}

	static void add(final TranslationBuilder b, final boolean ko) {
		maps(b, ko);
		lairs(b, ko);
		combiner(b, ko);
		pets(b, ko);
		imbue(b, ko);
	}

	private static void maps(final TranslationBuilder b, final boolean ko) {
		b.add("item.minecraft_mode.lair_map.tiered", ko ? "티어 %s 소굴 지도" : "Tier %s Lair Map");
		b.add("item.minecraft_mode.lair_map.tier_tooltip", ko ? "읽으면 이 지도만의 소굴이 열립니다 (Lv 100 기준, 체력 x%s, 피해 x%s)"
			: "Read it to open a lair of its own (Lv 100, health x%s, damage x%s)");
		b.add("item.minecraft_mode.lair_map.tier_affixes", ko ? "던전 변이 %s개" : "%s dungeon affix(es)");
		b.add("message.minecraft_mode.lair.next_tier_map", ko ? "다음 지도를 받았습니다: 티어 1 소굴 지도! (레벨 상한에서는 티어 지도가 나옵니다)"
			: "You got the next map: a tier 1 lair map! (at the level cap maps come with a tier)");
		b.add("screen.minecraft_mode.tier_lair.title", ko ? "티어 %s 소굴" : "Tier %s Lair");
		b.add("screen.minecraft_mode.tier_lair.go", ko ? "열기" : "Open");
		b.add("screen.minecraft_mode.tier_lair.power", ko ? "Lv 100 기준 체력 x%s, 피해 x%s" : "Lv 100, health x%s, damage x%s");
		b.add("screen.minecraft_mode.tier_lair.affixes", ko ? "던전 변이 %s개가 붙습니다" : "%s dungeon affix(es)");
		b.add("screen.minecraft_mode.tier_lair.no_affixes", ko ? "변이 없음 (티어 6부터)" : "No affixes (from tier 6)");
		b.add("screen.minecraft_mode.tier_lair.party", ko ? "함께 가는 사람: %s" : "Going: %s");
		b.add("screen.minecraft_mode.tier_lair.lives", ko ? "쓰러지면 입구에서 깨어납니다 (%s번까지)" : "Falls wake you at the gate (%s times)");
		b.add("screen.minecraft_mode.tier_lair.note", ko ? "열면 지도를 씁니다. 군주를 쓰러뜨리면 보상과 다음 지도를 받습니다."
			: "Opening uses the map. Kill the lord for the rewards and the next maps.");
	}

	private static void lairs(final TranslationBuilder b, final boolean ko) {
		String m = "message.minecraft_mode.tier_lair.";
		b.add(m + "problem.level", ko ? "티어 소굴은 직업 Lv 100부터 들어갈 수 있습니다." : "Tier lairs open from class Lv 100.");
		b.add(m + "problem.busy", ko ? "다른 콘텐츠(레이드·던전·사냥터·숲·티어 소굴) 중입니다." : "Busy with something else (raid, dungeon, hunt, forest or tier lair).");
		b.add(m + "problem.here", ko ? "여기서는 소굴 지도를 쓸 수 없습니다." : "Lair maps do not work here.");
		b.add(m + "problem.dead", ko ? "쓰러진 상태입니다." : "Down.");
		b.add(m + "problem.too_far", ko ? "너무 멀리 있습니다." : "Too far away.");
		b.add(m + "problem.too_many", ko ? "자리가 없습니다 (최대 4명)." : "No room (4 at most).");
		b.add(m + "problem.leader", ko ? "파티장만 티어 지도를 열 수 있습니다." : "Only the party leader can open a tier map.");
		b.add(m + "no_map", ko ? "티어 %s 소굴 지도가 없습니다." : "You have no tier %s lair map.");
		b.add(m + "full", ko ? "티어 소굴이 모두 차 있습니다. 잠시 후에 다시 오세요." : "All tier lairs are taken. Come back in a moment.");
		b.add(m + "opening", ko ? "티어 %s 소굴이 열리고 있습니다..." : "A tier %s lair is opening...");
		b.add(m + "map_back", ko ? "소굴이 열리지 않아 티어 %s 지도를 우편함(경매장)으로 돌려드렸습니다."
			: "The lair did not open; your tier %s map is back in your mailbox (market).");
		b.add(m + "tier", ko ? "티어 %s" : "Tier %s");
		b.add(m + "entered", ko ? "%s (티어 %s)에 들어왔습니다. 목표 방의 군주를 쓰러뜨리세요! 쓰러져도 %s번까지 입구에서 깨어납니다."
			: "You enter %s (tier %s). Kill the lord in the goal room! You wake at the gate after a fall, %s times.");
		b.add(m + "fallen", ko ? "%s 님이 쓰러졌습니다! 입구에서 깨어납니다 (남은 기회 %s)." : "%s fell! Waking at the gate (%s chance(s) left).");
		b.add(m + "out", ko ? "%s 님이 더 버티지 못하고 돌아갑니다." : "%s cannot go on and heads home.");
		b.add(m + "left", ko ? "%s 님이 소굴을 떠났습니다." : "%s left the lair.");
		b.add(m + "failed", ko ? "티어 %s 소굴 공략에 실패했습니다. 지도는 돌아오지 않습니다." : "The tier %s lair was lost. The map is gone.");
		b.add(m + "victory", ko ? "%s (티어 %s)의 군주를 쓰러뜨렸습니다! 소굴은 %s초 뒤 닫힙니다." : "The lord of %s (tier %s) fell! The lair closes in %s s.");
		b.add(m + "closing", ko ? "소굴이 %s초 뒤 닫힙니다." : "The lair closes in %s s.");
		b.add(m + "map_count", ko ? "티어 %s x%s" : "tier %s x%s");
		b.add(m + "core", ko ? "진화의 핵을 얻었습니다!" : "You found an evolution core!");
		b.add(m + "scroll", ko ? "%s을(를) 얻었습니다!" : "You found %s!");
		b.add(m + "reward", ko ? "보상: 보물 %s개, 경험치 %s, 다음 지도 [%s], 진화의 파편 %s개" : "Rewards: %s treasure(s), %s exp, next maps [%s], %s evolution shard(s)");
		b.add("entity.minecraft_mode.tier_lord", ko ? "군주 %s" : "Lord %s");
	}

	private static void combiner(final TranslationBuilder b, final boolean ko) {
		b.add("container.minecraft_mode.map_combiner", ko ? "지도 조합" : "Map Combiner");
		String m = "message.minecraft_mode.map_combiner.";
		b.add(m + "success", ko ? "성공! 티어 %s 지도가 되었습니다." : "Success! It is a tier %s map now.");
		b.add(m + "failed", ko ? "실패... 티어 %s 지도는 그대로 남았습니다." : "Failed... the tier %s map is still there.");
		b.add(m + "merged", ko ? "지도 %s장(티어 %s)을 합쳐 티어 %s 지도를 만들었습니다." : "%s maps (tier %s) made one tier %s map.");
		b.add(m + "broadcast", ko ? "%s 님이 티어 %s 소굴 지도를 그려 냈습니다!" : "%s drew a tier %s lair map!");
		String s = "screen.minecraft_mode.map_combiner.";
		b.add(s + "insert", ko ? "티어 소굴 지도를 넣으세요. 같은 티어 지도는 여러 장 함께 넣을 수 있습니다."
			: "Put tier lair maps in the slot. Maps of one tier can go in together.");
		b.add(s + "top", ko ? "티어 %s이 가장 높은 티어입니다." : "Tier %s is the highest.");
		b.add(s + "chance", ko ? "성공 %s%%" : "%s%% chance");
		b.add(s + "essence", ko ? "응축된 정수" : "Condensed essence");
		b.add(s + "ether", ko ? "Lv 100 진화의 에테르" : "Lv 100 Evolution Ether");
		b.add(s + "keep", ko ? "실패해도 지도는 남습니다" : "A failure keeps the map");
		b.add(s + "raise", ko ? "강화" : "Raise");
		b.add(s + "merge", ko ? "%s장 조합" : "Merge %s");
		b.add(s + "result.success", ko ? "성공!" : "Success!");
		b.add(s + "result.merged", ko ? "조합!" : "Merged!");
		b.add(s + "result.fail", ko ? "실패" : "Failed");
	}

	private static void pets(final TranslationBuilder b, final boolean ko) {
		b.add(ModItems.EVOLUTION_SHARD, ko ? "진화의 파편" : "Evolution Shard");
		b.add("item.minecraft_mode.evolution_shard.tooltip", ko ? "펫 창(U)에서 펫을 진화시킵니다 (티어 소굴)." : "Evolves pets in the pet window (U) (tier lairs).");
		b.add(ModItems.EVOLUTION_CORE, ko ? "진화의 핵" : "Evolution Core");
		b.add("item.minecraft_mode.evolution_core.tooltip", ko ? "펫의 마지막 진화(★3)에 필요합니다 (티어 11 이상 소굴)." : "A pet's last evolution (★3) needs it (tier 11+ lairs).");
		String m = "message.minecraft_mode.pet.";
		b.add(m + "evolve_unknown", ko ? "그 펫이 없습니다." : "You do not have that pet.");
		b.add(m + "evolve_max", ko ? "이미 끝까지 진화했습니다." : "Already fully evolved.");
		b.add(m + "evolve_level", ko ? "펫이 Lv 10이 되어야 진화할 수 있습니다." : "The pet must be Lv 10 to evolve.");
		b.add(m + "evolve_materials", ko ? "진화 재료가 모자랍니다." : "Not enough evolution materials.");
		b.add(m + "evolved", ko ? "%s이(가) ★%s로 진화했습니다!" : "%s evolved to ★%s!");
		String s = "screen.minecraft_mode.companion.";
		b.add(s + "evolution", ko ? "진화 ★%s / %s (능력치 x%s)" : "Evolution ★%s / %s (bonus x%s)");
		b.add(s + "evolve", ko ? "★%s 진화" : "Evolve to ★%s");
		b.add(s + "evolve_bonus", ko ? "Lv 10 능력치: %s (x%s)" : "Lv 10 bonus: %s (x%s)");
		b.add(s + "evolve_level", ko ? "펫 Lv %s" : "Pet Lv %s");
		b.add(s + "evolve_shards", ko ? "진화의 파편 %s / %s" : "Evolution shards %s / %s");
		b.add(s + "evolve_cores", ko ? "진화의 핵 %s / %s" : "Evolution cores %s / %s");
		b.add(s + "evolve_source", ko ? "재료는 티어 소굴에서 나옵니다." : "Materials drop in tier lairs.");
	}

	private static void imbue(final TranslationBuilder b, final boolean ko) {
		for (ImbueSlot slot : ImbueSlot.values()) {
			String name = switch (slot) {
				case HELMET -> ko ? "투구" : "Helmet";
				case CHEST -> ko ? "상의" : "Chestplate";
				case LEGS -> ko ? "하의" : "Leggings";
				case BOOTS -> ko ? "신발" : "Boots";
				case NECKLACE -> ko ? "목걸이" : "Necklace";
				case EARRING -> ko ? "귀걸이" : "Earring";
				case RING -> ko ? "반지" : "Ring";
				case BRACELET -> ko ? "팔찌" : "Bracelet";
				case TALISMAN -> ko ? "부적" : "Talisman";
			};
			b.add(slot.nameKey(), name);
			b.add(ModItems.IMBUE_SCROLLS.get(slot), ko ? name + " 부여 주문서" : name + " Imbue Scroll");
			b.add(ModItems.GREATER_IMBUE_SCROLLS.get(slot), ko ? "상급 " + name + " 부여 주문서" : "Greater " + name + " Imbue Scroll");
		}
		b.add("item.minecraft_mode.imbue_scroll.line", ko ? "%s에 부여: %s" : "Imbues a %s with: %s");
		b.add("item.minecraft_mode.imbue_scroll.normal", ko ? "부위당 한 줄. 이미 부여된 장비에는 쓸 수 없습니다." : "One line per piece; not on a piece already imbued.");
		b.add("item.minecraft_mode.imbue_scroll.greater", ko ? "부위당 한 줄. 일반 부여 줄은 덮어쓸 수 있습니다." : "One line per piece; may replace a normal line.");
		b.add("item.minecraft_mode.imbue_scroll.use", ko ? "강화 장인 브로크의 강화대에서 '부여'로 씁니다." : "Use it with 'Imbue' at Artisan Brokk's bench.");
		b.add("tooltip.minecraft_mode.gear.imbued", ko ? "부여:" : "Imbued:");
		b.add("tooltip.minecraft_mode.gear.imbued_greater", ko ? "상급 부여:" : "Greater imbue:");
		b.add("screen.minecraft_mode.character.source.imbue", ko ? "부여" : "Imbue");
		b.add("message.minecraft_mode.enhance.imbued", ko ? "%s에 부여했습니다: %s" : "Imbued %s: %s");
		b.add("screen.minecraft_mode.enhance.imbue", ko ? "부여" : "Imbue");
		b.add("screen.minecraft_mode.enhance.imbue_title", ko ? "%s 사용" : "Use %s");
		b.add("screen.minecraft_mode.enhance.imbue_replace", ko ? "지금 붙은 일반 부여 줄을 바꿉니다." : "Replaces the normal line it has.");
		b.add("screen.minecraft_mode.enhance.imbue_note", ko ? "항상 성공하고 주문서 한 장을 씁니다. 진화·계승해도 남습니다."
			: "Always works and uses one scroll. It stays through evolution and inheritance.");
		b.add("screen.minecraft_mode.enhance.result.imbued", ko ? "부여!" : "Imbued!");
	}
}
