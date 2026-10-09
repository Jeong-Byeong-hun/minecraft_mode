package com.minecraftmode.client.datagen;

import com.minecraftmode.companion.Companions;
import com.minecraftmode.craft.Profession;
import com.minecraftmode.dungeon.DungeonAffix;
import com.minecraftmode.dungeon.DungeonDef;
import com.minecraftmode.dungeon.Dungeons;
import com.minecraftmode.job.Paragon;
import com.minecraftmode.registry.ModBlocks;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.story.Story;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;

/**
 * Translations for the content after the endgame: titles on name tags, paragon levels and awakening, professions and herbs, pets
 * and mounts, dungeons and keystones, world events and the main story.
 */
final class ContentLang {
	static void add(final TranslationBuilder b, final boolean ko) {
		paragon(b, ko);
		professions(b, ko);
		companions(b, ko);
		dungeons(b, ko);
		events(b, ko);
		story(b, ko);
	}

	private static void paragon(final TranslationBuilder b, final boolean ko) {
		for (Paragon.Stat stat : Paragon.Stat.values()) {
			b.add(stat.nameKey(), ko ? stat.ko : stat.en);
		}
		b.add("screen.minecraft_mode.paragon.tab", ko ? "초월" : "Paragon");
		b.add("screen.minecraft_mode.paragon.title", ko ? "초월" : "Paragon");
		b.add("screen.minecraft_mode.paragon.locked", ko ? "레벨 %s에 도달하면 경험치가 초월 레벨로 쌓입니다. 초월 레벨마다 능력치 하나에 포인트를 줄 수 있습니다."
			: "At level %s experience goes into paragon levels; every paragon level gives a point for one of these stats.");
		b.add("screen.minecraft_mode.paragon.level", ko ? "초월 레벨 %s" : "Paragon level %s");
		b.add("message.minecraft_mode.paragon.level_up", ko ? "초월 레벨 %s! 특성 창(N)의 초월 탭에서 포인트를 쓰세요." : "Paragon level %s! Spend the point in the Paragon tab (N).");
		b.add("message.minecraft_mode.paragon.reset", ko ? "초월 포인트를 모두 돌려받았습니다." : "Your paragon points were refunded.");

		b.add(ModItems.AWAKENING_CRYSTAL, ko ? "각성의 결정" : "Awakening Crystal");
		b.add(ModItems.TITAN_SHARD, ko ? "거신의 파편" : "Titan Shard");
		b.add("screen.minecraft_mode.enhance.awaken", ko ? "각성" : "Awaken");
		b.add("screen.minecraft_mode.enhance.awaken_to", ko ? "각성 ✦%s / %s" : "Awaken to ✦%s of %s");
		b.add("screen.minecraft_mode.enhance.awaken_safe", ko ? "각성은 실패하지 않습니다." : "Awakening never fails.");
		b.add("screen.minecraft_mode.enhance.awaken_next", ko ? "✦%s 각성 시 추가 능력치:" : "Awakening ✦%s adds:");
		b.add("screen.minecraft_mode.enhance.result.awakened", ko ? "각성 성공!" : "Awakened!");
		b.add("message.minecraft_mode.enhance.awakened", ko ? "각성 ✦%s!" : "Awakened to ✦%s!");
		b.add("message.minecraft_mode.enhance.awaken_broadcast", ko ? "%s님이 %s을(를) ✦%s 각성했습니다!" : "%s awakened %s to ✦%s!");
		b.add("tooltip.minecraft_mode.enhance.awaken", ko ? "각성 ✦%s/%s" : "Awakened ✦%s/%s");
	}

	private static void professions(final TranslationBuilder b, final boolean ko) {
		for (Profession p : Profession.values()) {
			b.add(p.nameKey(), ko ? p.ko : p.en);
		}
		b.add(ModBlocks.KITCHEN_STATION, ko ? "조리대" : "Kitchen Station");
		b.add(ModBlocks.ALCHEMY_STATION, ko ? "연금술 작업대" : "Alchemy Station");
		b.add(ModBlocks.SMITHING_STATION, ko ? "대장 작업대" : "Smithing Station");
		b.add("container.minecraft_mode.station.cooking", ko ? "요리" : "Cooking");
		b.add("container.minecraft_mode.station.alchemy", ko ? "연금술" : "Alchemy");
		b.add("container.minecraft_mode.station.smithing", ko ? "대장 기술" : "Smithing");
		b.add(ModItems.SUNLEAF, ko ? "햇살잎" : "Sunleaf");
		b.add(ModItems.MOONPETAL, ko ? "달꽃잎" : "Moonpetal");
		b.add(ModItems.FROSTROOT, ko ? "서리뿌리" : "Frostroot");
		b.add(ModItems.GLOWCAP, ko ? "빛버섯" : "Glowcap");
		b.add(ModItems.EMBERBLOOM, ko ? "잿불꽃" : "Emberbloom");
		b.add(ModItems.VOIDCAP, ko ? "공허버섯" : "Voidcap");
		b.add("screen.minecraft_mode.profession.level", ko ? "Lv %s/%s" : "Lv %s/%s");
		b.add("screen.minecraft_mode.profession.needs", ko ? "필요 레벨 %s" : "Needs level %s");
		b.add("screen.minecraft_mode.profession.exp", ko ? "숙련도 +%s" : "+%s skill");
		b.add("screen.minecraft_mode.profession.have", ko ? "%s/%s" : "%s/%s");
		b.add("message.minecraft_mode.profession.crafted", ko ? "%s ×%s 제작" : "Made %s ×%s");
		b.add("message.minecraft_mode.profession.crafted_double", ko ? "%s ×%s 제작 (숙련 보너스!)" : "Made %s ×%s (mastery bonus!)");
		b.add("message.minecraft_mode.profession.level_up", ko ? "%s 레벨 %s!" : "%s level %s!");
	}

	private static void companions(final TranslationBuilder b, final boolean ko) {
		for (Companions.PetDef def : Companions.pets()) {
			b.add(def.nameKey(), ko ? def.ko() : def.en());
			b.add(def.nameKey() + ".desc", ko ? def.descKo() : def.descEn());
		}
		for (Companions.MountDef def : Companions.mounts()) {
			b.add(def.nameKey(), ko ? def.ko() : def.en());
			b.add(def.nameKey() + ".desc", ko ? def.descKo() : def.descEn());
		}
		b.add("item.minecraft_mode.pet_charm", ko ? "%s 부적" : "%s Charm");
		b.add("item.minecraft_mode.mount_whistle", ko ? "%s 호루라기" : "%s Whistle");
		b.add("tooltip.minecraft_mode.pet.bonus", ko ? "보너스 (Lv 1, Lv 10까지 성장):" : "Bonus (Lv 1, grows to Lv 10):");
		b.add("tooltip.minecraft_mode.mount.speed", ko ? "속도 %s블록/초" : "Speed %s blocks/s");
		b.add("tooltip.minecraft_mode.mount.flying", ko ? "비행 · 속도 %s블록/초" : "Flies · speed %s blocks/s");
		b.add("tooltip.minecraft_mode.companion.use", ko ? "사용: 수집품에 추가 (P로 소환)" : "Use: add it to your collection (summon with P)");
		b.add("message.minecraft_mode.pet.learned", ko ? "새 펫: %s! 수집품(P)에서 소환할 수 있습니다." : "New pet: %s! Summon it from your collection (P).");
		b.add("message.minecraft_mode.mount.learned", ko ? "새 탈것: %s! H를 누르면 탑니다." : "New mount: %s! Press H to ride.");
		b.add("message.minecraft_mode.companion.known", ko ? "이미 가지고 있습니다." : "You already have it.");
		b.add("message.minecraft_mode.companion.found", ko ? "희귀한 발견: %s!" : "A rare find: %s!");
		b.add("message.minecraft_mode.pet.level_up", ko ? "%s의 레벨이 %s이(가) 되었습니다!" : "%s reached level %s!");
		b.add("message.minecraft_mode.mount.not_here", ko ? "여기서는 탈것을 부를 수 없습니다." : "You cannot call a mount here.");
		b.add("message.minecraft_mode.mount.none", ko ? "탈것이 없습니다. 공적 상점에서 호루라기를 구해 보세요." : "You have no mount yet. The merit shop sells a whistle.");
		b.add("key.minecraft_mode.companion_screen", ko ? "펫·탈것" : "Pets & Mounts");
		b.add("key.minecraft_mode.mount", ko ? "탈것 호출/내리기" : "Call / dismiss mount");
		b.add("screen.minecraft_mode.companion.title", ko ? "펫·탈것" : "Pets & Mounts");
		b.add("screen.minecraft_mode.companion.pets", ko ? "펫 %s/%s" : "Pets %s/%s");
		b.add("screen.minecraft_mode.companion.mounts", ko ? "탈것 %s/%s" : "Mounts %s/%s");
		b.add("screen.minecraft_mode.companion.summon", ko ? "소환" : "Summon");
		b.add("screen.minecraft_mode.companion.dismiss", ko ? "돌려보내기" : "Dismiss");
		b.add("screen.minecraft_mode.companion.ride", ko ? "타기" : "Ride");
		b.add("screen.minecraft_mode.companion.speed", ko ? "%s블록/초" : "%s blocks/s");
		b.add("screen.minecraft_mode.companion.flying", ko ? "비행 %s블록/초" : "Flies, %s b/s");
		b.add("screen.minecraft_mode.companion.mount_key", ko ? "H: 마지막 탈것 호출 / 내리기" : "H: call the last mount / get off");
		b.add("screen.minecraft_mode.companion.bonus", ko ? "펫 보너스: %s" : "Pet bonus: %s");
		b.add("screen.minecraft_mode.companion.max_bonus", ko ? "Lv 10: %s" : "At Lv 10: %s");
		b.add("screen.minecraft_mode.companion.no_pet", ko ? "소환한 펫이 없습니다." : "No pet summoned.");
		b.add("screen.minecraft_mode.companion.unknown", ko ? "아직 만나지 못했습니다." : "Not found yet.");
		b.add("screen.minecraft_mode.companion.level", ko ? "레벨 %s/%s" : "Level %s/%s");
		b.add("screen.minecraft_mode.companion.grow", ko ? "소환한 채 몬스터를 사냥하면 자랍니다 (네임드 10, 보스 50)." : "Grows while summoned as you hunt (named 10, bosses 50).");
		b.add("screen.minecraft_mode.companion.source.merit_pet", ko ? "길드 공적 상점, 메인 스토리" : "Guild merit shop, main story");
		b.add("screen.minecraft_mode.companion.source.merit_mount", ko ? "길드 공적 상점, 메인 스토리" : "Guild merit shop, main story");
		b.add("screen.minecraft_mode.companion.source.rare", ko ? "네임드 몬스터, 소굴 보물, 던전" : "Named monsters, lair treasure, dungeons");
		b.add("screen.minecraft_mode.companion.source.epic", ko ? "레이드, 월드 보스, +10 이상 던전, 메인 스토리" : "Raids, world bosses, +10 dungeons, main story");
	}

	private static void dungeons(final TranslationBuilder b, final boolean ko) {
		for (DungeonDef def : Dungeons.all()) {
			b.add(def.nameKey(), ko ? def.ko() : def.en());
			b.add(def.descKey(), ko ? def.descKo() : def.descEn());
		}
		for (DungeonAffix a : DungeonAffix.values()) {
			String[] t = switch (a) {
				case FORTIFIED -> new String[] {"Fortified", "견고", "Hall monsters have 25%% more health and hit 15%% harder.", "홀 몬스터의 체력이 25%%, 피해가 15%% 늘어납니다."};
				case TYRANNICAL -> new String[] {"Tyrannical", "폭군", "The boss has 40%% more health and hits 15%% harder.", "보스의 체력이 40%%, 피해가 15%% 늘어납니다."};
				case BOLSTERING -> new String[] {"Bolstering", "강화", "When a hall monster dies, those near it grow stronger.", "홀 몬스터가 죽으면 근처의 몬스터가 강해집니다."};
				case RAGING -> new String[] {"Raging", "분노", "Hall monsters enrage below 30%% health.", "홀 몬스터가 체력 30%% 이하에서 격노합니다."};
				case VOLCANIC -> new String[] {"Volcanic", "화산", "Fire erupts under a player every few seconds (telegraphed).", "몇 초마다 플레이어 발밑에서 불이 솟습니다 (예고 있음)."};
			};
			b.add(a.nameKey(), ko ? t[1] : t[0]);
			b.add(a.descKey(), ko ? t[3] : t[2]);
		}
		b.add(ModItems.DUNGEON_KEYSTONE, ko ? "던전 쐐기돌" : "Dungeon Keystone");
		b.add("item.minecraft_mode.dungeon_keystone.named", ko ? "쐐기돌: %s +%s" : "Keystone: %s +%s");
		b.add("tooltip.minecraft_mode.keystone.level", ko ? "입장 레벨 %s" : "Entry level %s");
		b.add("tooltip.minecraft_mode.keystone.scaling", ko ? "몬스터 체력 +%s%%, 피해 +%s%%" : "Monsters: +%s%% health, +%s%% damage");
		b.add("tooltip.minecraft_mode.keystone.affix_4", ko ? "+4: 견고 또는 폭군" : "+4: Fortified or Tyrannical");
		b.add("tooltip.minecraft_mode.keystone.affix_7", ko ? "+7: 강화 또는 분노" : "+7: Bolstering or Raging");
		b.add("tooltip.minecraft_mode.keystone.affix_10", ko ? "+10: 화산" : "+10: Volcanic");
		b.add("tooltip.minecraft_mode.keystone.time", ko ? "제한 시간 %s분" : "Time limit %s min");
		b.add("tooltip.minecraft_mode.keystone.use", ko ? "던전 관리인에게 가져가세요. 시간 안에 돌파하면 강해집니다." : "Bring it to the dungeon warden; beat the timer to raise it.");

		b.add("dungeon.minecraft_mode.keystone_level", ko ? "쐐기돌 +%s" : "Keystone +%s");
		b.add("dungeon.minecraft_mode.normal", ko ? "일반" : "Normal");
		b.add("dungeon.minecraft_mode.go", ko ? "문이 열렸다!" : "The door opens!");
		b.add("dungeon.minecraft_mode.boss", ko ? "던전의 주인" : "Master of the dungeon");
		b.add("entity.minecraft_mode.dungeon_boss", ko ? "던전 보스 %s" : "Dungeon Boss %s");
		b.add("entity.minecraft_mode.dungeon_elite", ko ? "정예 %s" : "Elite %s");

		b.add("screen.minecraft_mode.dungeon.title", ko ? "던전" : "Dungeons");
		b.add("screen.minecraft_mode.dungeon.party", ko ? "파티장 입장 · 최대 %s명" : "Leader enters · up to %s");
		b.add("screen.minecraft_mode.dungeon.enter", ko ? "입장" : "Enter");
		b.add("screen.minecraft_mode.dungeon.info", ko ? "Lv %s+ · 제한 %s" : "Lv %s+ · limit %s");
		b.add("screen.minecraft_mode.dungeon.boss", ko ? "보스: %s" : "Boss: %s");
		b.add("screen.minecraft_mode.dungeon.clears", ko ? "클리어 %s회 · 최고 시간 내 +%s" : "Cleared %s times · best timed +%s");
		b.add("screen.minecraft_mode.dungeon.keystone_hint", ko ? "일반으로 처음 클리어하면 +2 쐐기돌을 받습니다. 오른쪽 버튼은 쐐기돌 입장."
			: "Your first normal clear gives a +2 keystone. The right button enters with it.");
		b.add("screen.minecraft_mode.dungeon.affixes", ko ? "이번 주기 속성:" : "This cycle:");
		b.add("screen.minecraft_mode.dungeon.footer", ko ? "쓰러지면 입구에서 깨어나고 15초를 잃습니다." : "Falling sends you to the entrance (-15 s).");

		String m = "message.minecraft_mode.dungeon.";
		b.add(m + "not_leader", ko ? "파티장만 던전에 입장시킬 수 있습니다." : "Only the party leader can take the party in.");
		b.add(m + "too_many", ko ? "던전은 최대 %s명까지입니다." : "Dungeons take at most %s players.");
		b.add(m + "no_keystone", ko ? "%s 쐐기돌이 없습니다." : "You have no keystone for %s.");
		b.add(m + "not_ready", ko ? "파티가 준비되지 않았습니다:" : "The party is not ready:");
		b.add(m + "problem.busy", ko ? "다른 레이드나 던전에 있음" : "is in another raid or dungeon");
		b.add(m + "problem.too_far", ko ? "너무 멀리 있음" : "is too far away");
		b.add(m + "problem.level", ko ? "레벨 %s 미만" : "is below level %s");
		b.add(m + "problem.dead", ko ? "쓰러져 있음" : "is down");
		b.add(m + "full", ko ? "모든 던전이 사용 중입니다. 잠시 후 다시 시도하세요." : "Every dungeon is taken; try again shortly.");
		b.add(m + "entered", ko ? "%s에 들어왔습니다. %s초 뒤 문이 열립니다!" : "You entered %s. The door opens in %s seconds!");
		b.add(m + "timer", ko ? "제한 시간: %s" : "Time limit: %s");
		b.add(m + "not_in_dungeon", ko ? "던전 안에 있지 않습니다." : "You are not in a dungeon.");
		b.add(m + "left", ko ? "%s님이 던전을 떠났습니다." : "%s left the dungeon.");
		b.add(m + "fallen", ko ? "%s님이 쓰러져 입구에서 깨어납니다 (-%s초)." : "%s fell and wakes at the entrance (-%s s).");
		b.add(m + "failed", ko ? "%s 공략에 실패했습니다." : "The run through %s failed.");
		b.add(m + "hall_cleared", ko ? "홀 %s/%s 정리! 다음 문이 열립니다." : "Hall %s/%s cleared! The next door opens.");
		b.add(m + "returning", ko ? "%s초 뒤 귀환합니다 (/dungeon leave)" : "Returning in %s s (/dungeon leave)");
		b.add(m + "victory", ko ? "%s 클리어! 시간 %s" : "%s cleared in %s!");
		b.add(m + "timed", ko ? "제한 시간(%s) 안에 돌파했습니다!" : "Beaten within the limit (%s)!");
		b.add(m + "late", ko ? "제한 시간(%s)을 넘겼습니다." : "Over the limit (%s).");
		b.add(m + "keystone_new", ko ? "새 쐐기돌: %s" : "New keystone: %s");
		b.add(m + "keystone_up", ko ? "쐐기돌이 강해졌습니다: %s" : "Your keystone grew: %s");
		b.add(m + "keystone_down", ko ? "쐐기돌이 약해졌습니다: %s" : "Your keystone weakened: %s");
	}

	private static void events(final TranslationBuilder b, final boolean ko) {
		b.add("entity.minecraft_mode.world_boss", ko ? "거신 %s" : "Titan %s");
		b.add("entity.minecraft_mode.warlord", ko ? "침공 군주 %s" : "Warlord %s");
		String m = "message.minecraft_mode.event.";
		b.add(m + "titan_rises", ko ? "⚠ 거신 %s이(가) 깨어났다! (%s, %s)" : "⚠ The titan %s rises at (%s, %s)!");
		b.add(m + "titan_title", ko ? "거신의 각성" : "A Titan Wakes");
		b.add(m + "titan_left", ko ? "거신 %s이(가) 땅속으로 사라졌다." : "The titan %s sinks back into the earth.");
		b.add(m + "titan_reward", ko ? "거신 토벌 보상! (가한 피해 %s)" : "Titan reward! (damage dealt: %s)");
		b.add(m + "titan_fallen", ko ? "거신 %s이(가) 쓰러졌다! 보상 %s명 · 최고 공헌: %s" : "The titan %s has fallen! %s rewarded · top: %s");
		b.add(m + "invasion_start", ko ? "⚔ 수도가 공격받고 있다! 스톰홀드를 지켜라!" : "⚔ The capital is under attack! Defend Stormhold!");
		b.add(m + "invasion_title", ko ? "수도 침공" : "Invasion");
		b.add(m + "invasion_subtitle", ko ? "성문으로 몰려온다!" : "They come through the gates!");
		b.add(m + "invasion_bar", ko ? "수도 침공 · %s/%s 웨이브" : "Invasion · wave %s/%s");
		b.add(m + "invasion_wave", ko ? "웨이브 %s/%s" : "Wave %s/%s");
		b.add(m + "invasion_retreat", ko ? "침공군이 물러갔다." : "The invaders withdraw.");
		b.add(m + "invasion_reward", ko ? "수도 방어 보상을 받았습니다!" : "You were rewarded for defending the capital!");
		b.add(m + "invasion_won", ko ? "침공을 막아 냈다! 수비대 %s명이 보상을 받았습니다." : "The invasion is repelled! %s defenders rewarded.");
	}

	private static void story(final TranslationBuilder b, final boolean ko) {
		for (Story.Chapter c : Story.chapters()) {
			b.add(c.key(), ko ? c.titleKo() : c.titleEn());
			b.add(c.key() + ".text", ko ? c.textKo() : c.textEn());
			b.add(c.key() + ".goal", ko ? c.goalKo() : c.goalEn());
			b.add(c.key() + ".reward", ko ? c.rewardKo() : c.rewardEn());
		}
		String m = "message.minecraft_mode.story.";
		b.add(m + "chapter", ko ? "[메인 스토리] %s장: %s" : "[Main story] Chapter %s: %s");
		b.add(m + "goal", ko ? "목표: %s (%s/%s)" : "Goal: %s (%s/%s)");
		b.add(m + "goal_done", ko ? "[메인 스토리] '%s' 목표 달성! 광장의 왕실 전령에게 돌아가세요." : "[Main story] '%s' done! Return to the royal herald on the plaza.");
		b.add(m + "chapter_done", ko ? "%s 완료!" : "%s complete!");
		b.add(m + "reward", ko ? "보상: %s, %s" : "Reward: %s, %s");
		b.add(m + "finished", ko ? "그대의 이름은 스톰홀드의 영웅으로 영원히 기억될 것이오." : "Your name will be remembered forever as the Hero of Stormhold.");
		b.add("screen.minecraft_mode.codex.tab.story", ko ? "스토리" : "Story");
		b.add("screen.minecraft_mode.codex.chapter", ko ? "%s장" : "Chapter %s");
		b.add("screen.minecraft_mode.codex.chapter_goal", ko ? "목표: %s" : "Goal: %s");
		b.add("screen.minecraft_mode.codex.chapter_reward", ko ? "보상: %s (+ 코인)" : "Reward: %s (+ coins)");
		b.add("screen.minecraft_mode.codex.story_hint", ko ? "광장의 왕실 전령에게 다음 장을 들으세요 (/story)." : "Hear the next chapter from the royal herald on the plaza (/story).");
		b.add("screen.minecraft_mode.codex.story_done", ko ? "메인 스토리를 모두 마쳤습니다!" : "You finished the main story!");
	}

	private ContentLang() {
	}
}
