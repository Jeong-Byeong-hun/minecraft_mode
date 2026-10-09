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
		guide(b, ko);
		paragon(b, ko);
		professions(b, ko);
		companions(b, ko);
		dungeons(b, ko);
		events(b, ko);
		story(b, ko);
	}

	/** Guide Nella's topics and Quartermaster Bram's starter kit. */
	private static void guide(final TranslationBuilder b, final boolean ko) {
		b.add("screen.minecraft_mode.guide.title", ko ? "안내원 넬라 — 무엇이든 물어보세요" : "Guide Nella - ask me anything");
		b.add("screen.minecraft_mode.guide.plaza", ko ? "광장 (시작 지점)" : "Plaza (spawn)");
		b.add("screen.minecraft_mode.guide.leveling_left", ko ? "지금 Lv %s · 레벨 %3$s까지 경험치 %2$s 남음" : "Now Lv %s · %s EXP to level %s");
		b.add("screen.minecraft_mode.guide.leveling_done", ko ? "레벨 10 달성! 원하는 직업의 교관을 찾아가세요 (길 안내)." : "Level 10 reached! Visit the trainer of your class (see Directions).");
		b.add("message.minecraft_mode.starter_kit.given", ko ? "보급관 브람에게서 철 갑옷 한 벌과 철 검·곡괭이·도끼를 받았습니다!"
			: "Quartermaster Bram hands you a set of iron armor and an iron sword, pickaxe and axe!");
		b.add("message.minecraft_mode.starter_kit.taken", ko ? "보급품은 모험가마다 한 번만 받을 수 있네." : "The kit is one per adventurer - you already have yours.");
		String[][] topics = {
			{"start", "Getting started", "처음 시작",
				"Defeat monsters and mine iron or better ores to gain class experience (see Leveling to 10). At level 10, take the trial of the trainer whose class you want.\n\nFirst pick up a set of iron gear from Quartermaster Bram here on the plaza. Coins go straight into your wallet (/wallet), and you keep your items when you die.",
				"몬스터를 처치하고 철 이상의 광석을 캐면 직업 경험치가 오릅니다(빠른 성장 참고). 레벨 10이 되면 원하는 직업의 교관에게서 시련을 받아 직업을 고르세요.\n\n먼저 바로 옆 보급관 브람에게서 철 장비 한 벌을 받아 가세요. 동전은 지갑에 바로 들어가고(/wallet), 죽어도 아이템은 잃지 않습니다."},
			{"leveling", "Leveling to 10", "빠른 성장 (Lv 10)",
				"Level 10 takes %s EXP. Hunting is fastest: a monster gives EXP equal to its max health (zombie or skeleton 20, enderman 40, named 40-60), so about %s zombies. Ores are slow (iron 2, diamond 8).\n\n· Wear Bram's iron gear and hunt outside the walls at night\n· A monster spawner room is the best spot\n· Scholar's Coffee from the alchemist in the market (3 silver): +15%% EXP for 10 min\n· Dying costs 10%% of this level's EXP",
				"레벨 10까지 경험치 %s. 사냥이 가장 빠릅니다: 몬스터 경험치는 최대 체력과 같아서 좀비·스켈레톤 20, 엔더맨 40, 네임드 40~60이니 좀비 약 %s마리면 됩니다. 광석은 철 2, 다이아 8로 느립니다.\n\n· 브람의 철 장비를 입고 밤에 성벽 밖에서 사냥\n· 몬스터 스포너가 있는 방이 최고의 사냥터\n· 시장 연금술사의 학자의 커피(은화 3개): 10분간 경험치 +15%%\n· 죽으면 이번 레벨 경험치의 10%%를 잃습니다"},
			{"classes", "Classes", "직업과 전직",
				"Seven classes: Warrior, Rogue, Mage, Archer, Pirate, Soul Reaper and Hunter. Every advancement is a trial from the class trainer: defeat the listed monsters, collect their trial tokens and hand them in with essence. Tiers open at levels 10, 25, 45 and 70.\n\nThe class screen (K) shows your next trial and where its trainer stands. Past level 100 you gain paragon levels.",
				"직업은 전사·도적·법사·궁수·해적·사신·헌터 7가지입니다. 모든 전직은 교관의 시련입니다: 목표 몬스터를 처치해 시련 증표를 모으고 정수와 함께 제출하세요. 레벨 10·25·45·70에 1~4차가 열립니다.\n\n직업 창(K)에서 다음 시련과 교관 위치를 볼 수 있습니다. 레벨 100 이후에는 초월 레벨이 쌓입니다."},
			{"keys", "Keys and commands", "키와 명령어",
				"K class screen\nN talents and paragon\nJ codex, achievements, titles, story\nR / G / V / Z skills\nB innate ability (level 20)\nP pets and mounts\nH call or dismiss your mount\nL raid loot\n\n/wallet, /party, /raid leave, /dungeon leave, /story",
				"K 직업 창\nN 특성·초월\nJ 도감·업적·칭호·스토리\nR / G / V / Z 스킬\nB 고유 기술 (레벨 20)\nP 펫·탈것\nH 탈것 호출/내리기\nL 레이드 전리품\n\n/wallet, /party, /raid leave, /dungeon leave, /story"},
			{"places", "Directions (x, z)", "길 안내 (x, z)", "", ""},
			{"money", "Money and shops", "돈·상점·거래소",
				"9 copper = 1 silver, 9 silver = 1 gold. Monsters and named monsters drop coins.\n\nThe south market has the general store, blacksmith, grocer, jeweler and alchemist, the Adventurers' Guild (class gear) and Broker Morgan's market where players trade with each other. Prices rise for a while as an item is traded a lot.",
				"동화 9개 = 은화 1개, 은화 9개 = 금화 1개. 몬스터와 네임드가 동전을 떨어뜨립니다.\n\n남쪽 시장에 잡화점·대장간·식료품점·보석상·연금술사, 모험가 길드(직업 장비), 플레이어끼리 사고파는 중개인 모건의 거래소가 있습니다. 많이 거래되는 물건은 한동안 값이 오릅니다."},
			{"gear", "Growing your gear", "장비 성장",
				"The Guild sells one class weapon and one armor piece per 10 levels; the rest drops. Engrave gear with essence at the engraving tables, evolve it with Master Smith Volund (Evolution Ether), and enhance it to +15 and awaken it to ✦5 with Artisan Brokk.\n\nWeapon skills only work when your class, tier and level match the weapon.",
				"길드는 10레벨마다 직업 무기와 방어구를 한 종씩 팔고, 나머지는 드롭으로 얻습니다. 정수 각인대에서 각인, 명장 볼룬드에게서 진화(진화의 에테르), 강화 장인 브로크에게서 +15 강화와 ✦5 각성을 합니다.\n\n무기 스킬은 직업·차수·레벨이 맞아야 작동합니다."},
			{"named", "Named monsters and lairs", "네임드와 소굴",
				"Named monsters ([Lv.N] name tags) appear now and then in their biomes and drop gear, Evolution Ether and coins. Each has a lair: defeat its lair lord and the treasure is yours again every three days. A lair map points to the nearest one.",
				"네임드 몬스터([Lv.N] 이름표)는 서식 바이옴에 가끔 나타나 장비·진화의 에테르·동전을 줍니다. 네임드마다 소굴이 있어서, 소굴의 군주를 쓰러뜨리면 보물 상자를 사흘마다 개인 보상으로 엽니다. 소굴 지도로 가장 가까운 소굴을 찾으세요."},
			{"dungeons", "Raids and dungeons", "레이드와 던전",
				"Raids: Raid Marshal Aldric by the keep, parties of up to 6, six bosses on Normal, Heroic and Nightmare; rewards once every three days.\n\nDungeons: Dungeon Warden Kael in the keep courtyard, 2-4 players, five dungeons. Your first clear gives a keystone for harder, timed runs. You lose nothing when you fall in a raid or dungeon.",
				"레이드: 왕성 앞 토벌 사령관 알드릭, 파티 최대 6명, 보스 6종, 일반·영웅·악몽. 보상은 사흘에 한 번.\n\n던전: 성 안뜰의 던전 관리인 카엘, 2~4명, 5곳. 처음 깨면 쐐기돌을 받아 더 어렵고 시간 제한이 있는 도전을 합니다. 레이드·던전에서는 쓰러져도 잃는 것이 없습니다."},
			{"bounties", "Bounties, achievements, story", "의뢰·업적·스토리",
				"Guild Clerk Lina hands out three daily bounties and one three-day bounty; they pay merit for the merit shop.\n\nAchievements give merit and titles; wear titles from the codex (J). Royal Herald Elric here on the plaza tells the main story.",
				"길드 접수원 리나가 일일 의뢰 3개와 사흘 의뢰 1개를 줍니다. 보상으로 받는 공적은 공적 상점에서 씁니다.\n\n업적은 공적과 칭호를 주고, 칭호는 도감(J)에서 착용합니다. 이 광장의 왕실 전령 엘릭이 메인 스토리를 들려줍니다."},
			{"companions", "Pets, mounts and crafts", "펫·탈것·생활 기술",
				"Pet charms and mount whistles come from the merit shop, named monsters, lairs, dungeons, raids and the story. P opens your collection, H calls your mount.\n\nCook and brew at the stations in the market and smith at the forge; herbs drop from grass and flowers.",
				"펫 부적과 탈것 호루라기는 공적 상점, 네임드, 소굴, 던전, 레이드, 스토리에서 얻습니다. P로 수집 창, H로 탈것을 부릅니다.\n\n시장의 작업대에서 요리와 연금술, 대장간에서 대장 기술을 익히세요. 약초는 풀과 꽃을 부수면 나옵니다."},
			{"events", "World events and resets", "월드 이벤트와 초기화",
				"Every day at dusk a titan rises in the wilds (two days of three) or the capital is invaded (the third). Everyone who fights is rewarded.\n\nLair treasure, raid rewards and the three-day bounty renew every three Minecraft days.",
				"매일 해 질 녘에 황야에 거신이 나타나거나(사흘 중 이틀), 수도가 침공당합니다(셋째 날). 함께 싸운 모두가 보상을 받습니다.\n\n소굴 보물·레이드 보상·사흘 의뢰는 마인크래프트 날짜로 3일마다 초기화됩니다."},
		};
		for (String[] t : topics) {
			b.add("guide.minecraft_mode." + t[0], ko ? t[2] : t[1]);
			if (!t[3].isEmpty()) {
				b.add("guide.minecraft_mode." + t[0] + ".text", ko ? t[4] : t[3]);
			}
		}
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
