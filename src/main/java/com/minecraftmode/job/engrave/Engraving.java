package com.minecraftmode.job.engrave;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.gear.GearSlot;
import com.minecraftmode.job.weapon.Archetype;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Class engravings: lines added to class weapons at the engraving table with essence instead of
 * experience. Each class has its own pool; some lines only fit certain weapon shapes. Ids are
 * saved on items, so never rename them.
 */
public enum Engraving {
	// Warrior
	CRUSH("crush", JobClass.WARRIOR, EngraveStat.BASIC_DAMAGE, 8, "Crushing Force", "분쇄"),
	WIDE_SWING("wide_swing", JobClass.WARRIOR, EngraveStat.SPLASH, 1.5F, "Wide Swing", "평타 반경",
		Archetype.GREATSWORD, Archetype.WARHAMMER, Archetype.BATTLEAXE, Archetype.HALBERD, Archetype.SCYTHE, Archetype.GREATSHIELD),
	BLOODTHIRST("bloodthirst", JobClass.WARRIOR, EngraveStat.LIFESTEAL, 3, "Bloodthirst", "피의 갈증"),
	IRON_SKIN("iron_skin", JobClass.WARRIOR, EngraveStat.DAMAGE_REDUCTION, 5, "Iron Skin", "강철 피부"),
	GIANTS_VIGOR("giants_vigor", JobClass.WARRIOR, EngraveStat.MAX_HEALTH, 4, "Giant's Vigor", "거인의 활력"),
	FIGHTING_SPIRIT("fighting_spirit", JobClass.WARRIOR, EngraveStat.SKILL_DAMAGE, 8, "Fighting Spirit", "투지"),
	EXECUTIONER("executioner", JobClass.WARRIOR, EngraveStat.EXECUTE, 15, "Executioner", "처형인"),
	LONG_SHAFT("long_shaft", JobClass.WARRIOR, EngraveStat.REACH, 0.5F, "Long Shaft", "긴 자루", Archetype.SPEAR, Archetype.HALBERD, Archetype.SCYTHE),
	LACERATE("lacerate", JobClass.WARRIOR, EngraveStat.BLEED, 2, "Lacerate", "열상"),
	RAGE("rage", JobClass.WARRIOR, EngraveStat.MANA_ON_HIT, 1, "Rage", "분노"),
	SWIFT_BLADE("swift_blade", JobClass.WARRIOR, EngraveStat.ATTACK_SPEED, 6, "Swift Blade", "빠른 칼날", Archetype.KATANA, Archetype.LONGSWORD),

	// Rogue
	AMBUSH("ambush", JobClass.ROGUE, EngraveStat.BACKSTAB, 20, "Ambush", "암습"),
	KEEN_EDGE("keen_edge", JobClass.ROGUE, EngraveStat.CRIT_CHANCE, 6, "Keen Edge", "예리함"),
	VITAL_POINTS("vital_points", JobClass.ROGUE, EngraveStat.CRIT_DAMAGE, 15, "Vital Points", "급소"),
	VENOM("venom", JobClass.ROGUE, EngraveStat.POISON, 2, "Venom", "맹독"),
	SWIFTNESS("swiftness", JobClass.ROGUE, EngraveStat.MOVE_SPEED, 5, "Swiftness", "신속"),
	FLURRY("flurry", JobClass.ROGUE, EngraveStat.ATTACK_SPEED, 6, "Flurry", "연격"),
	EVASION("evasion", JobClass.ROGUE, EngraveStat.DODGE, 4, "Evasion", "회피"),
	FAN_THROW("fan_throw", JobClass.ROGUE, EngraveStat.EXTRA_SHOT, 1, "Fan Throw", "부채 투척", Archetype.SHURIKEN, Archetype.KUNAI, Archetype.WAR_FAN),
	SHADOW_ARTS("shadow_arts", JobClass.ROGUE, EngraveStat.COOLDOWN, 5, "Shadow Arts", "그림자 술법"),
	FINISHER("finisher", JobClass.ROGUE, EngraveStat.EXECUTE, 15, "Finisher", "마무리 일격"),

	// Mage
	AMPLIFY("amplify", JobClass.MAGE, EngraveStat.SKILL_DAMAGE, 10, "Amplify", "마력 증폭"),
	QUICK_CAST("quick_cast", JobClass.MAGE, EngraveStat.COOLDOWN, 6, "Quick Cast", "고속 영창"),
	MANA_EFFICIENCY("mana_efficiency", JobClass.MAGE, EngraveStat.MANA_COST, 8, "Mana Efficiency", "마나 절약"),
	MANA_WELL("mana_well", JobClass.MAGE, EngraveStat.MAX_MANA, 15, "Mana Well", "마나 샘"),
	SPELL_ECHO("spell_echo", JobClass.MAGE, EngraveStat.ECHO, 5, "Spell Echo", "메아리"),
	EXPANSION("expansion", JobClass.MAGE, EngraveStat.SKILL_AREA, 10, "Expansion", "광역화"),
	IGNITE("ignite", JobClass.MAGE, EngraveStat.BURN, 2, "Ignite", "점화"),
	FROSTBITE("frostbite", JobClass.MAGE, EngraveStat.SLOW, 2, "Frostbite", "동상"),
	MANA_SIPHON("mana_siphon", JobClass.MAGE, EngraveStat.MANA_ON_HIT, 2, "Mana Siphon", "마력 흡수"),
	FOCUSED_BOLT("focused_bolt", JobClass.MAGE, EngraveStat.SHOT_DAMAGE, 12, "Focused Bolt", "집중 마탄"),

	// Archer
	HEADSHOT("headshot", JobClass.ARCHER, EngraveStat.RANGE_BONUS, 6, "Headshot", "헤드샷"),
	MULTISHOT("multishot", JobClass.ARCHER, EngraveStat.EXTRA_SHOT, 1, "Multishot", "다중 사격",
		Archetype.SHORTBOW, Archetype.LONGBOW, Archetype.GREATBOW, Archetype.CROSSBOW, Archetype.CHAKRAM),
	HEAVY_DRAW("heavy_draw", JobClass.ARCHER, EngraveStat.SHOT_DAMAGE, 10, "Heavy Draw", "강궁"),
	KEEN_EYE("keen_eye", JobClass.ARCHER, EngraveStat.CRIT_CHANCE, 5, "Keen Eye", "예리한 눈"),
	WINDSTEP("windstep", JobClass.ARCHER, EngraveStat.MOVE_SPEED, 5, "Windstep", "바람의 발"),
	QUICK_DRAW("quick_draw", JobClass.ARCHER, EngraveStat.DRAW_SPEED, 10, "Quick Draw", "속사",
		Archetype.SHORTBOW, Archetype.LONGBOW, Archetype.GREATBOW, Archetype.CROSSBOW, Archetype.CHAKRAM),
	FOCUS("focus", JobClass.ARCHER, EngraveStat.SKILL_DAMAGE, 8, "Focus", "집중"),
	CRIPPLING_SHOT("crippling_shot", JobClass.ARCHER, EngraveStat.SLOW, 2, "Crippling Shot", "다리 저격"),
	POISON_TIP("poison_tip", JobClass.ARCHER, EngraveStat.POISON, 2, "Poison Tip", "독촉"),
	TACTICS("tactics", JobClass.ARCHER, EngraveStat.COOLDOWN, 5, "Tactics", "전술"),
	TWIN_FANGS("twin_fangs", JobClass.ARCHER, EngraveStat.BASIC_DAMAGE, 8, "Twin Fangs", "쌍아", Archetype.TWIN_BLADES),

	// Pirate
	PLUNDER("plunder", JobClass.PIRATE, EngraveStat.GOLD_FIND, 4, "Plunder", "약탈"),
	GUNPOWDER("gunpowder", JobClass.PIRATE, EngraveStat.SHOT_DAMAGE, 10, "Gunpowder", "화약",
		Archetype.PISTOL, Archetype.MUSKET, Archetype.HAND_CANNON, Archetype.HARPOON, Archetype.BLUNDERBUSS),
	RUM_RATION("rum_ration", JobClass.PIRATE, EngraveStat.LIFESTEAL, 3, "Rum Ration", "럼주"),
	BOARDING_PARTY("boarding_party", JobClass.PIRATE, EngraveStat.BASIC_DAMAGE, 8, "Boarding Party", "백병전"),
	LADY_LUCK("lady_luck", JobClass.PIRATE, EngraveStat.CRIT_CHANCE, 5, "Lady Luck", "행운의 여신"),
	CAPTAINS_COMMAND("captains_command", JobClass.PIRATE, EngraveStat.SKILL_DAMAGE, 8, "Captain's Command", "선장의 호령"),
	DOUBLE_BARREL("double_barrel", JobClass.PIRATE, EngraveStat.EXTRA_SHOT, 1, "Double Barrel", "연발", Archetype.PISTOL, Archetype.MUSKET, Archetype.HAND_CANNON, Archetype.BLUNDERBUSS),
	INCENDIARY("incendiary", JobClass.PIRATE, EngraveStat.BURN, 2, "Incendiary", "소이탄"),
	SEAS_BLESSING("seas_blessing", JobClass.PIRATE, EngraveStat.DAMAGE_REDUCTION, 4, "Sea's Blessing", "바다의 가호"),
	QUICK_RELOAD("quick_reload", JobClass.PIRATE, EngraveStat.DRAW_SPEED, 10, "Quick Reload", "빠른 장전",
		Archetype.PISTOL, Archetype.MUSKET, Archetype.HAND_CANNON, Archetype.HARPOON, Archetype.BLUNDERBUSS),
	IRON_FIST("iron_fist", JobClass.PIRATE, EngraveStat.ATTACK_SPEED, 6, "Iron Fist", "철권", Archetype.KNUCKLE, Archetype.CUTLASS, Archetype.RAPIER),
	// Soul Reaper
	REIATSU("reiatsu", JobClass.SHINIGAMI, EngraveStat.SKILL_DAMAGE, 10, "Spiritual Pressure", "영압"),
	KIDO_MASTERY("kido_mastery", JobClass.SHINIGAMI, EngraveStat.COOLDOWN, 6, "Kido Mastery", "귀도 숙련"),
	ZANJUTSU("zanjutsu", JobClass.SHINIGAMI, EngraveStat.BASIC_DAMAGE, 8, "Zanjutsu", "참술"),
	HOHO("hoho", JobClass.SHINIGAMI, EngraveStat.MOVE_SPEED, 5, "Hoho", "보법"),
	SOUL_BURIAL("soul_burial", JobClass.SHINIGAMI, EngraveStat.EXECUTE, 15, "Soul Burial", "혼장"),
	RELEASE_COMMAND("release_command", JobClass.SHINIGAMI, EngraveStat.MANA_COST, 8, "Release Command", "해방 언령"),
	HOLLOWFICATION("hollowfication", JobClass.SHINIGAMI, EngraveStat.LIFESTEAL, 3, "Hollowfication", "호로화"),
	BANKAI_TRAINING("bankai_training", JobClass.SHINIGAMI, EngraveStat.BOSS_DAMAGE, 10, "Bankai Training", "만해 수행"),
	FROZEN_EDGE("frozen_edge", JobClass.SHINIGAMI, EngraveStat.SLOW, 2, "Frozen Edge", "얼어붙는 칼날",
		Archetype.KATANA, Archetype.NINJATO, Archetype.TWIN_BLADES, Archetype.CLAW),
	EXTENDING_BLADE("extending_blade", JobClass.SHINIGAMI, EngraveStat.REACH, 0.5F, "Extending Blade", "늘어나는 칼날",
		Archetype.SPEAR, Archetype.KUSARIGAMA, Archetype.GREATSWORD, Archetype.LONGSWORD, Archetype.WHIP_SWORD),
	// Hunter
	REN("ren", JobClass.HUNTER, EngraveStat.SKILL_DAMAGE, 8, "Ren", "렌"),
	RYU("ryu", JobClass.HUNTER, EngraveStat.CRIT_DAMAGE, 15, "Ryu", "류"),
	ZETSU("zetsu", JobClass.HUNTER, EngraveStat.BACKSTAB, 20, "Zetsu", "제츠"),
	SHU("shu", JobClass.HUNTER, EngraveStat.BASIC_DAMAGE, 8, "Shu", "슈"),
	IN("in", JobClass.HUNTER, EngraveStat.FIRST_STRIKE, 20, "In", "인"),
	ENHANCER("enhancer", JobClass.HUNTER, EngraveStat.DOUBLE_STRIKE, 5, "Enhancer", "강화계"),
	EMITTER("emitter", JobClass.HUNTER, EngraveStat.SHOT_DAMAGE, 10, "Emitter", "방출계",
		Archetype.SHURIKEN, Archetype.KUNAI, Archetype.PISTOL, Archetype.STAFF, Archetype.ORB, Archetype.GRIMOIRE, Archetype.SCEPTER, Archetype.NEEDLE),
	CONJURER("conjurer", JobClass.HUNTER, EngraveStat.MANA_COST, 8, "Conjurer", "구현화계"),
	TRANSMUTER("transmuter", JobClass.HUNTER, EngraveStat.BLEED, 2, "Transmuter", "변화계"),
	SPECIALIST("specialist", JobClass.HUNTER, EngraveStat.ECHO, 5, "Specialist", "특질계"),

	// ---------------------------------------------------------------- armor (4 lines per piece)
	// Warrior
	BULWARK("bulwark", JobClass.WARRIOR, EngraveStat.DAMAGE_REDUCTION, 3, "Bulwark", "철벽", GearSlot.CHEST, GearSlot.LEGS),
	TITAN_BLOOD("titan_blood", JobClass.WARRIOR, EngraveStat.MAX_HEALTH, 3, "Titan Blood", "거인의 피", GearSlot.HEAD, GearSlot.CHEST),
	BATTLE_FOCUS("battle_focus", JobClass.WARRIOR, EngraveStat.COOLDOWN_FLAT, 0.3F, "Battle Focus", "전투 집중", GearSlot.HEAD),
	SPIKED_PLATE("spiked_plate", JobClass.WARRIOR, EngraveStat.THORNS, 8, "Spiked Plate", "가시 갑주", GearSlot.CHEST),
	UNSTOPPABLE("unstoppable", JobClass.WARRIOR, EngraveStat.KNOCKBACK_RES, 10, "Unstoppable", "불굴", GearSlot.LEGS, GearSlot.FEET),
	WARPATH("warpath", JobClass.WARRIOR, EngraveStat.MOVE_SPEED, 3, "Warpath", "진격", GearSlot.FEET),
	HEAVY_HAND("heavy_hand", JobClass.WARRIOR, EngraveStat.BASIC_DAMAGE, 4, "Heavy Hand", "묵직한 손", GearSlot.HEAD, GearSlot.CHEST, GearSlot.LEGS, GearSlot.FEET),
	BERSERKER_RAGE("berserker_rage", JobClass.WARRIOR, EngraveStat.CRIT_DAMAGE, 8, "Berserker's Rage", "광전사의 분노", GearSlot.HEAD, GearSlot.CHEST, GearSlot.LEGS),
	LAST_BASTION("last_bastion", JobClass.WARRIOR, EngraveStat.LAST_STAND, 5, "Last Bastion", "최후의 보루", GearSlot.CHEST, GearSlot.LEGS, GearSlot.FEET),
	KNIGHTS_OATH("knights_oath", JobClass.WARRIOR, EngraveStat.PROTECTION, 2, "Knight's Oath", "기사의 맹세", GearSlot.HEAD, GearSlot.CHEST, GearSlot.LEGS),
	GIANT_SLAYER("giant_slayer", JobClass.WARRIOR, EngraveStat.BOSS_DAMAGE, 5, "Giant Slayer", "거인 사냥꾼", GearSlot.HEAD, GearSlot.LEGS, GearSlot.FEET),
	BLOOD_OATH("blood_oath", JobClass.WARRIOR, EngraveStat.LIFESTEAL, 1, "Blood Oath", "피의 맹세", GearSlot.HEAD, GearSlot.CHEST),
	SECOND_WIND("second_wind", JobClass.WARRIOR, EngraveStat.HEALTH_REGEN, 0.5F, "Second Wind", "재기", GearSlot.CHEST, GearSlot.LEGS, GearSlot.FEET),
	SWEEPING_STRIKES("sweeping_strikes", JobClass.WARRIOR, EngraveStat.SKILL_AREA, 4, "Sweeping Strikes", "휩쓸기 강타", GearSlot.HEAD, GearSlot.LEGS, GearSlot.FEET),
	HEADSMANS_MARK("headsmans_mark", JobClass.WARRIOR, EngraveStat.EXECUTE, 6, "Headsman's Mark", "망나니의 표식", GearSlot.HEAD, GearSlot.FEET),
	WAR_DRUMS("war_drums", JobClass.WARRIOR, EngraveStat.SKILL_DAMAGE, 4, "War Drums", "전쟁의 북", GearSlot.HEAD, GearSlot.FEET),
	ROUND_TABLE_VOW("round_table_vow", JobClass.WARRIOR, EngraveStat.MAGIC_DEFENSE, 2, "Round Table Vow", "원탁의 서약", GearSlot.HEAD, GearSlot.LEGS, GearSlot.FEET),
	DRAGONSLAYER_GRIP("dragonslayer_grip", JobClass.WARRIOR, EngraveStat.FIRST_STRIKE, 8, "Dragonslayer's Grip", "용살자의 악력", GearSlot.CHEST, GearSlot.FEET),
	// Rogue
	SHADOW_STEP("shadow_step", JobClass.ROGUE, EngraveStat.DODGE, 2, "Shadow Step", "그림자 걸음", GearSlot.LEGS, GearSlot.FEET),
	CUTTHROAT("cutthroat", JobClass.ROGUE, EngraveStat.CRIT_DAMAGE, 8, "Cutthroat", "목따기", GearSlot.HEAD, GearSlot.CHEST),
	FLEET_FOOT("fleet_foot", JobClass.ROGUE, EngraveStat.MOVE_SPEED, 3, "Fleet Foot", "빠른 발", GearSlot.FEET),
	VANISHING_ACT("vanishing_act", JobClass.ROGUE, EngraveStat.STEALTH_ON_KILL, 1, "Vanishing Act", "사라지는 기술", GearSlot.CHEST),
	NIMBLE_FINGERS("nimble_fingers", JobClass.ROGUE, EngraveStat.ATTACK_SPEED, 3, "Nimble Fingers", "재빠른 손", GearSlot.CHEST, GearSlot.LEGS),
	KILLERS_INSTINCT("killers_instinct", JobClass.ROGUE, EngraveStat.CRIT_CHANCE, 3, "Killer's Instinct", "살수의 본능", GearSlot.HEAD),
	CONCEALED_EDGE("concealed_edge", JobClass.ROGUE, EngraveStat.BACKSTAB, 8, "Concealed Edge", "숨긴 칼날", GearSlot.HEAD, GearSlot.CHEST, GearSlot.LEGS, GearSlot.FEET),
	POISON_POUCH("poison_pouch", JobClass.ROGUE, EngraveStat.POISON, 0.5F, "Poison Pouch", "독 주머니", GearSlot.CHEST, GearSlot.LEGS),
	CHAKRA_CONTROL("chakra_control", JobClass.ROGUE, EngraveStat.MANA_COST, 3, "Chakra Control", "차크라 조절", GearSlot.HEAD, GearSlot.CHEST),
	NARROW_ESCAPE("narrow_escape", JobClass.ROGUE, EngraveStat.LAST_STAND, 5, "Narrow Escape", "구사일생", GearSlot.CHEST, GearSlot.LEGS, GearSlot.FEET),
	ASSASSINS_MARK("assassins_mark", JobClass.ROGUE, EngraveStat.EXECUTE, 6, "Assassin's Mark", "암살 표식", GearSlot.HEAD, GearSlot.LEGS, GearSlot.FEET),
	SILENT_KILLING("silent_killing", JobClass.ROGUE, EngraveStat.FIRST_STRIKE, 8, "Silent Killing", "무음살인술", GearSlot.HEAD, GearSlot.FEET),
	TWIN_STRIKE("twin_strike", JobClass.ROGUE, EngraveStat.DOUBLE_STRIKE, 2, "Twin Strike", "쌍격", GearSlot.CHEST, GearSlot.LEGS, GearSlot.FEET),
	FLEEING_SHADOW("fleeing_shadow", JobClass.ROGUE, EngraveStat.SPEED_ON_KILL, 1, "Fleeing Shadow", "도주하는 그림자", GearSlot.HEAD, GearSlot.LEGS, GearSlot.FEET),
	BLOOD_PRICE("blood_price", JobClass.ROGUE, EngraveStat.LIFESTEAL, 1, "Blood Price", "피의 대가", GearSlot.HEAD, GearSlot.CHEST),
	TOOL_POUCH("tool_pouch", JobClass.ROGUE, EngraveStat.SHOT_DAMAGE, 4, "Ninja Tool Pouch", "닌자 도구 주머니", GearSlot.HEAD, GearSlot.CHEST, GearSlot.FEET),
	BYAKUGAN("byakugan", JobClass.ROGUE, EngraveStat.CRIT_CHANCE, 2, "Byakugan", "백안", GearSlot.HEAD, GearSlot.LEGS),
	COLD_BLOOD("cold_blood", JobClass.ROGUE, EngraveStat.SKILL_DAMAGE, 4, "Cold Blood", "냉혈", GearSlot.LEGS, GearSlot.FEET),
	// Mage
	ARCANE_MIND("arcane_mind", JobClass.MAGE, EngraveStat.MAX_MANA, 10, "Arcane Mind", "비전의 정신", GearSlot.HEAD, GearSlot.CHEST),
	FLOWING_MANA("flowing_mana", JobClass.MAGE, EngraveStat.MANA_REGEN, 1, "Flowing Mana", "흐르는 마나", GearSlot.HEAD, GearSlot.LEGS),
	SPELL_HASTE("spell_haste", JobClass.MAGE, EngraveStat.COOLDOWN_FLAT, 0.3F, "Spell Haste", "주문 가속", GearSlot.HEAD),
	MANA_WARD("mana_ward", JobClass.MAGE, EngraveStat.MANA_SHIELD, 5, "Mana Ward", "마나 보호막", GearSlot.CHEST),
	POWER_SURGE("power_surge", JobClass.MAGE, EngraveStat.SKILL_DAMAGE, 4, "Power Surge", "마력 쇄도", GearSlot.HEAD, GearSlot.CHEST, GearSlot.LEGS, GearSlot.FEET),
	SKY_WALK("sky_walk", JobClass.MAGE, EngraveStat.MOVE_SPEED, 3, "Sky Walk", "천공 보행", GearSlot.FEET),
	ELEMENTAL_ATTUNEMENT("elemental_attunement", JobClass.MAGE, EngraveStat.BURN, 0.5F, "Elemental Attunement", "원소 조율", GearSlot.CHEST, GearSlot.LEGS, GearSlot.FEET),
	FROST_WEAVE("frost_weave", JobClass.MAGE, EngraveStat.SLOW, 0.5F, "Frost Weave", "서리 직조", GearSlot.CHEST, GearSlot.LEGS, GearSlot.FEET),
	ARCANE_ECHO("arcane_echo", JobClass.MAGE, EngraveStat.ECHO, 1, "Arcane Echo", "비전 메아리", GearSlot.HEAD, GearSlot.LEGS),
	RUNIC_WARD("runic_ward", JobClass.MAGE, EngraveStat.MAGIC_DEFENSE, 2, "Runic Ward", "룬 결계", GearSlot.HEAD, GearSlot.CHEST, GearSlot.FEET),
	LEY_LINE("ley_line", JobClass.MAGE, EngraveStat.MANA_COST, 3, "Ley Line", "영맥", GearSlot.CHEST, GearSlot.LEGS, GearSlot.FEET),
	WIDE_CIRCLE("wide_circle", JobClass.MAGE, EngraveStat.SKILL_AREA, 4, "Wide Circle", "확장 마법진", GearSlot.HEAD, GearSlot.LEGS, GearSlot.FEET),
	SOUL_SIPHON("soul_siphon", JobClass.MAGE, EngraveStat.KILL_MANA, 2, "Soul Siphon", "영혼 흡수", GearSlot.HEAD, GearSlot.CHEST, GearSlot.FEET),
	BLINK("blink", JobClass.MAGE, EngraveStat.DODGE, 2, "Blink", "점멸", GearSlot.LEGS, GearSlot.FEET),
	FOCUSED_MIND("focused_mind", JobClass.MAGE, EngraveStat.CRIT_CHANCE, 2, "Focused Mind", "정신 집중", GearSlot.HEAD, GearSlot.CHEST),
	HEALING_RUNE("healing_rune", JobClass.MAGE, EngraveStat.HEAL_ON_SKILL, 1.5F, "Healing Rune", "치유의 룬", GearSlot.CHEST, GearSlot.LEGS),
	STORM_CALLER("storm_caller", JobClass.MAGE, EngraveStat.CHAIN_LIGHTNING, 2, "Storm Caller", "폭풍 부름", GearSlot.HEAD, GearSlot.LEGS, GearSlot.FEET),
	// Archer
	EAGLE_SIGHT("eagle_sight", JobClass.ARCHER, EngraveStat.CRIT_CHANCE, 3, "Eagle Sight", "독수리 눈", GearSlot.HEAD),
	STEADY_AIM("steady_aim", JobClass.ARCHER, EngraveStat.SHOT_DAMAGE, 4, "Steady Aim", "안정된 조준", GearSlot.CHEST, GearSlot.LEGS),
	SWIFT_QUIVER("swift_quiver", JobClass.ARCHER, EngraveStat.DRAW_SPEED, 5, "Swift Quiver", "빠른 화살통", GearSlot.CHEST),
	WIND_RUNNER("wind_runner", JobClass.ARCHER, EngraveStat.MOVE_SPEED, 3, "Wind Runner", "바람 질주", GearSlot.LEGS, GearSlot.FEET),
	HUNTERS_TALLY("hunters_tally", JobClass.ARCHER, EngraveStat.KILL_HEAL, 1, "Hunter's Tally", "사냥꾼의 셈", GearSlot.CHEST, GearSlot.LEGS),
	FAR_SIGHT("far_sight", JobClass.ARCHER, EngraveStat.RANGE_BONUS, 2, "Far Sight", "원시", GearSlot.HEAD),
	SHARP_GAZE("sharp_gaze", JobClass.ARCHER, EngraveStat.CRIT_DAMAGE, 8, "Sharp Gaze", "날카로운 시선", GearSlot.HEAD, GearSlot.CHEST, GearSlot.LEGS, GearSlot.FEET),
	SNIPERS_PATIENCE("snipers_patience", JobClass.ARCHER, EngraveStat.FIRST_STRIKE, 8, "Sniper's Patience", "저격수의 인내", GearSlot.HEAD, GearSlot.CHEST, GearSlot.FEET),
	FLETCHERS_CRAFT("fletchers_craft", JobClass.ARCHER, EngraveStat.SHOT_DAMAGE, 3, "Fletcher's Craft", "화살 장인", GearSlot.HEAD, GearSlot.FEET),
	BIG_GAME_HUNTER("big_game_hunter", JobClass.ARCHER, EngraveStat.BOSS_DAMAGE, 5, "Big Game Hunter", "대물 사냥꾼", GearSlot.HEAD, GearSlot.CHEST, GearSlot.LEGS),
	CAMOUFLAGE("camouflage", JobClass.ARCHER, EngraveStat.DODGE, 2, "Camouflage", "위장", GearSlot.CHEST, GearSlot.LEGS, GearSlot.FEET),
	LIGHT_STEP("light_step", JobClass.ARCHER, EngraveStat.SPEED_ON_KILL, 1, "Light Step", "가벼운 발걸음", GearSlot.LEGS, GearSlot.FEET),
	TRACKER("tracker", JobClass.ARCHER, EngraveStat.ITEM_FIND, 3, "Tracker", "추적자", GearSlot.HEAD, GearSlot.FEET),
	VOLLEY_DRILL("volley_drill", JobClass.ARCHER, EngraveStat.SKILL_DAMAGE, 4, "Volley Drill", "일제 사격 훈련", GearSlot.HEAD, GearSlot.CHEST, GearSlot.FEET),
	RANGERS_CLOAK("rangers_cloak", JobClass.ARCHER, EngraveStat.PROTECTION, 2, "Ranger's Cloak", "순찰자의 망토", GearSlot.HEAD, GearSlot.CHEST, GearSlot.FEET),
	KILL_SHOT("kill_shot", JobClass.ARCHER, EngraveStat.EXECUTE, 6, "Kill Shot", "결정타", GearSlot.HEAD, GearSlot.LEGS, GearSlot.FEET),
	FOLLOWING_WIND("following_wind", JobClass.ARCHER, EngraveStat.COOLDOWN, 2, "Following Wind", "등바람", GearSlot.LEGS, GearSlot.FEET),
	THRILL_OF_THE_HUNT("thrill_of_the_hunt", JobClass.ARCHER, EngraveStat.KILL_MANA, 2, "Thrill of the Hunt", "사냥의 전율", GearSlot.CHEST, GearSlot.LEGS),
	// Pirate
	SEA_DOG("sea_dog", JobClass.PIRATE, EngraveStat.MAX_HEALTH, 3, "Sea Dog", "바다의 개", GearSlot.CHEST),
	TREASURE_NOSE("treasure_nose", JobClass.PIRATE, EngraveStat.ITEM_FIND, 3, "Treasure Nose", "보물 냄새", GearSlot.HEAD, GearSlot.FEET),
	GOLD_TOOTH("gold_tooth", JobClass.PIRATE, EngraveStat.GOLD_FIND, 2, "Gold Tooth", "금니", GearSlot.HEAD),
	BRAWLERS_BELT("brawlers_belt", JobClass.PIRATE, EngraveStat.BASIC_DAMAGE, 4, "Brawler's Belt", "싸움꾼의 허리띠", GearSlot.LEGS),
	DECK_LEGS("deck_legs", JobClass.PIRATE, EngraveStat.DODGE, 2, "Deck Legs", "갑판 다리", GearSlot.FEET),
	GROG("grog", JobClass.PIRATE, EngraveStat.KILL_HEAL, 1, "Grog", "그로그주", GearSlot.CHEST),
	ARMAMENT_HAKI("armament_haki", JobClass.PIRATE, EngraveStat.PROTECTION, 2, "Armament Haki", "무장색 패기", GearSlot.HEAD, GearSlot.CHEST, GearSlot.LEGS, GearSlot.FEET),
	DANGER_SENSE("danger_sense", JobClass.PIRATE, EngraveStat.DODGE, 2, "Danger Sense", "위기 감지", GearSlot.HEAD, GearSlot.CHEST, GearSlot.LEGS),
	PIRATE_KINGS_WILL("pirate_kings_will", JobClass.PIRATE, EngraveStat.BOSS_DAMAGE, 5, "Pirate King's Will", "해적왕의 의지", GearSlot.HEAD, GearSlot.CHEST),
	POWDER_KEG("powder_keg", JobClass.PIRATE, EngraveStat.SHOT_DAMAGE, 4, "Powder Keg", "화약통", GearSlot.CHEST, GearSlot.LEGS, GearSlot.FEET),
	STEADY_FOOTING("steady_footing", JobClass.PIRATE, EngraveStat.KNOCKBACK_RES, 10, "Steady Footing", "흔들림 없는 발", GearSlot.LEGS, GearSlot.FEET),
	BOARDING_HOOK("boarding_hook", JobClass.PIRATE, EngraveStat.FIRST_STRIKE, 8, "Boarding Hook", "접현 갈고리", GearSlot.HEAD, GearSlot.LEGS, GearSlot.FEET),
	JOLLY_ROGER("jolly_roger", JobClass.PIRATE, EngraveStat.SKILL_DAMAGE, 4, "Jolly Roger", "해적 깃발", GearSlot.HEAD, GearSlot.CHEST, GearSlot.FEET),
	SALTED_HIDE("salted_hide", JobClass.PIRATE, EngraveStat.DAMAGE_REDUCTION, 3, "Salted Hide", "소금 먹은 가죽", GearSlot.CHEST, GearSlot.LEGS),
	LOOTERS_LUCK("looters_luck", JobClass.PIRATE, EngraveStat.CRIT_CHANCE, 3, "Looter's Luck", "약탈자의 행운", GearSlot.HEAD, GearSlot.LEGS, GearSlot.FEET),
	CAPTAINS_COAT("captains_coat", JobClass.PIRATE, EngraveStat.LIFESTEAL, 1, "Captain's Coat", "선장의 코트", GearSlot.CHEST, GearSlot.LEGS),
	SEA_SHANTY("sea_shanty", JobClass.PIRATE, EngraveStat.HEALTH_REGEN, 0.5F, "Sea Shanty", "뱃노래", GearSlot.HEAD, GearSlot.LEGS, GearSlot.FEET),
	ROPE_SWING("rope_swing", JobClass.PIRATE, EngraveStat.MOVE_SPEED, 3, "Rope Swing", "밧줄 타기", GearSlot.LEGS, GearSlot.FEET),
	RUM_COURAGE("rum_courage", JobClass.PIRATE, EngraveStat.LAST_STAND, 5, "Rum Courage", "럼주의 용기", GearSlot.HEAD, GearSlot.CHEST),
	// Soul Reaper
	SHIHAKUSHO_WEAVE("shihakusho_weave", JobClass.SHINIGAMI, EngraveStat.DAMAGE_REDUCTION, 3, "Shihakusho Weave", "사패장 직조", GearSlot.CHEST, GearSlot.LEGS),
	HAORI_CREST("haori_crest", JobClass.SHINIGAMI, EngraveStat.SKILL_DAMAGE, 4, "Haori Crest", "하오리 문장", GearSlot.CHEST),
	SAKKETSU("sakketsu", JobClass.SHINIGAMI, EngraveStat.MAX_MANA, 10, "Sakketsu", "쇄결", GearSlot.HEAD, GearSlot.CHEST),
	HAKUSUI("hakusui", JobClass.SHINIGAMI, EngraveStat.MANA_REGEN, 1, "Hakusui", "백수", GearSlot.HEAD, GearSlot.LEGS),
	VISORED_MASK("visored_mask", JobClass.SHINIGAMI, EngraveStat.CRIT_DAMAGE, 8, "Visored Mask", "가면", GearSlot.HEAD),
	WARAJI("waraji", JobClass.SHINIGAMI, EngraveStat.MOVE_SPEED, 3, "Waraji", "짚신", GearSlot.FEET),
	SHUNPO_STEP("shunpo_step", JobClass.SHINIGAMI, EngraveStat.DODGE, 2, "Flash Step", "순보", GearSlot.LEGS, GearSlot.FEET),
	HAKUDA_MASTERY("hakuda_mastery", JobClass.SHINIGAMI, EngraveStat.BASIC_DAMAGE, 4, "Hakuda Mastery", "백타 숙련", GearSlot.CHEST, GearSlot.LEGS, GearSlot.FEET),
	BAKUDO("bakudo", JobClass.SHINIGAMI, EngraveStat.SLOW, 0.5F, "Bakudo", "박도", GearSlot.HEAD, GearSlot.CHEST, GearSlot.LEGS),
	HADO("hado", JobClass.SHINIGAMI, EngraveStat.SKILL_AREA, 4, "Hado", "파도", GearSlot.HEAD, GearSlot.LEGS, GearSlot.FEET),
	INCANTATION("incantation", JobClass.SHINIGAMI, EngraveStat.COOLDOWN, 2, "Incantation", "영창", GearSlot.HEAD, GearSlot.FEET),
	HIERRO("hierro", JobClass.SHINIGAMI, EngraveStat.PROTECTION, 2, "Hierro", "강피", GearSlot.CHEST, GearSlot.LEGS, GearSlot.FEET),
	GOTEI_DISCIPLINE("gotei_discipline", JobClass.SHINIGAMI, EngraveStat.BOSS_DAMAGE, 5, "Gotei Discipline", "호정 13대의 규율", GearSlot.HEAD, GearSlot.CHEST),
	REIRYOKU("reiryoku", JobClass.SHINIGAMI, EngraveStat.MANA_COST, 3, "Reiryoku", "영력", GearSlot.HEAD, GearSlot.CHEST, GearSlot.LEGS),
	HOLLOW_INSTINCT("hollow_instinct", JobClass.SHINIGAMI, EngraveStat.LIFESTEAL, 1, "Hollow Instinct", "호로의 본능", GearSlot.HEAD, GearSlot.LEGS, GearSlot.FEET),
	ZANPAKUTO_BOND("zanpakuto_bond", JobClass.SHINIGAMI, EngraveStat.CRIT_CHANCE, 3, "Zanpakuto Bond", "참백도와의 유대", GearSlot.CHEST, GearSlot.FEET),
	LIEUTENANT_BADGE("lieutenant_badge", JobClass.SHINIGAMI, EngraveStat.FIRST_STRIKE, 8, "Lieutenant's Badge", "부대장 완장", GearSlot.HEAD, GearSlot.CHEST, GearSlot.FEET),
	SONIDO("sonido", JobClass.SHINIGAMI, EngraveStat.SPEED_ON_KILL, 1, "Sonido", "향전", GearSlot.LEGS, GearSlot.FEET),
	// Hunter
	HUNTER_LICENSE("hunter_license", JobClass.HUNTER, EngraveStat.ITEM_FIND, 3, "Hunter License", "헌터 자격증", GearSlot.HEAD),
	TEN_AURA("ten_aura", JobClass.HUNTER, EngraveStat.DAMAGE_REDUCTION, 3, "Ten", "텐", GearSlot.CHEST, GearSlot.LEGS),
	KEN_GUARD("ken_guard", JobClass.HUNTER, EngraveStat.MAX_HEALTH, 3, "Ken", "켄", GearSlot.HEAD, GearSlot.CHEST),
	GYO_SIGHT("gyo_sight", JobClass.HUNTER, EngraveStat.CRIT_CHANCE, 3, "Gyo", "교", GearSlot.HEAD),
	ZOLDYCK_TRAINING("zoldyck_training", JobClass.HUNTER, EngraveStat.KNOCKBACK_RES, 10, "Zoldyck Training", "조르딕가의 수련", GearSlot.LEGS, GearSlot.FEET),
	SWIFT_HUNTER("swift_hunter", JobClass.HUNTER, EngraveStat.MOVE_SPEED, 3, "Swift Hunter", "날쌘 사냥꾼", GearSlot.FEET),
	MEDITATION("meditation", JobClass.HUNTER, EngraveStat.MANA_REGEN, 1, "Meditation", "명상", GearSlot.CHEST, GearSlot.LEGS),
	EN("en", JobClass.HUNTER, EngraveStat.SKILL_AREA, 4, "En", "엔", GearSlot.HEAD, GearSlot.CHEST, GearSlot.LEGS),
	CONCENTRATED_AURA("concentrated_aura", JobClass.HUNTER, EngraveStat.CRIT_DAMAGE, 8, "Concentrated Aura", "오라 집중", GearSlot.CHEST, GearSlot.LEGS, GearSlot.FEET),
	HATSU("hatsu", JobClass.HUNTER, EngraveStat.SKILL_DAMAGE, 4, "Hatsu", "발", GearSlot.HEAD, GearSlot.CHEST, GearSlot.FEET),
	NEN_PACT("nen_pact", JobClass.HUNTER, EngraveStat.BOSS_DAMAGE, 5, "Nen Pact", "넨의 맹약", GearSlot.HEAD, GearSlot.LEGS),
	ASSASSINS_GAIT("assassins_gait", JobClass.HUNTER, EngraveStat.DODGE, 2, "Assassin's Gait", "암살자의 걸음", GearSlot.LEGS, GearSlot.FEET),
	GREED_ISLAND("greed_island", JobClass.HUNTER, EngraveStat.EXP_BONUS, 4, "Greed Island", "그리드 아일랜드", GearSlot.HEAD, GearSlot.FEET),
	AURA_RECOVERY("aura_recovery", JobClass.HUNTER, EngraveStat.HEALTH_REGEN, 0.5F, "Aura Recovery", "오라 회복", GearSlot.CHEST, GearSlot.LEGS, GearSlot.FEET),
	HUNTERS_INSTINCT("hunters_instinct", JobClass.HUNTER, EngraveStat.FIRST_STRIKE, 8, "Hunter's Instinct", "사냥꾼의 본능", GearSlot.HEAD, GearSlot.FEET),
	POISON_TOLERANCE("poison_tolerance", JobClass.HUNTER, EngraveStat.PROTECTION, 2, "Poison Tolerance", "독 내성", GearSlot.CHEST, GearSlot.LEGS, GearSlot.FEET),
	EMPEROR_TIME("emperor_time", JobClass.HUNTER, EngraveStat.COOLDOWN, 2, "Emperor Time", "절대시간", GearSlot.HEAD, GearSlot.CHEST),
	JAJANKEN("jajanken", JobClass.HUNTER, EngraveStat.EXECUTE, 6, "Jajanken", "짱껨뽀", GearSlot.HEAD, GearSlot.LEGS, GearSlot.FEET),
	WATER_DIVINATION("water_divination", JobClass.HUNTER, EngraveStat.MAX_MANA, 10, "Water Divination", "수견식", GearSlot.HEAD, GearSlot.CHEST);

	/** Each class offers at least this many engravings on every armor slot (0.5.24). */
	public static final int MIN_ARMOR_POOL = 10;

	private final String id;
	private final JobClass job;
	private final EngraveStat stat;
	private final float value;
	private final String en;
	private final String ko;
	private final Set<Archetype> only;
	private final Set<GearSlot> slots;

	/** Weapon line, optionally only for some weapon shapes. */
	Engraving(final String id, final JobClass job, final EngraveStat stat, final float value, final String en, final String ko, final Archetype... only) {
		this.id = id;
		this.job = job;
		this.stat = stat;
		this.value = value;
		this.en = en;
		this.ko = ko;
		this.only = only.length == 0 ? EnumSet.allOf(Archetype.class) : EnumSet.copyOf(Arrays.asList(only));
		this.slots = EnumSet.of(GearSlot.WEAPON);
	}

	/** Armor line for the given slots. */
	Engraving(
		final String id, final JobClass job, final EngraveStat stat, final float value, final String en, final String ko, final GearSlot slot,
		final GearSlot... more
	) {
		this.id = id;
		this.job = job;
		this.stat = stat;
		this.value = value;
		this.en = en;
		this.ko = ko;
		this.only = EnumSet.noneOf(Archetype.class);
		this.slots = EnumSet.of(slot, more);
	}

	public String id() {
		return this.id;
	}

	public JobClass job() {
		return this.job;
	}

	public EngraveStat stat() {
		return this.stat;
	}

	/** Amount one line adds. */
	public float value() {
		return this.value;
	}

	public String en() {
		return this.en;
	}

	public String ko() {
		return this.ko;
	}

	public String nameKey() {
		return "engraving.minecraft_mode." + this.id;
	}

	/** Fits a class weapon of this class and shape. */
	public boolean fits(final JobClass job, final Archetype archetype) {
		return this.job == job && this.slots.contains(GearSlot.WEAPON) && this.only.contains(archetype);
	}

	/** Fits a class armor piece of this class and slot. */
	public boolean fits(final JobClass job, final GearSlot slot) {
		return this.job == job && this.slots.contains(slot);
	}

	public boolean isArmor() {
		return !this.slots.contains(GearSlot.WEAPON);
	}

	public Set<GearSlot> slots() {
		return this.slots;
	}

	public static List<Engraving> pool(final JobClass job, final Archetype archetype) {
		return Arrays.stream(values()).filter(e -> e.fits(job, archetype)).toList();
	}

	public static List<Engraving> pool(final JobClass job, final GearSlot slot) {
		return Arrays.stream(values()).filter(e -> e.fits(job, slot)).toList();
	}

	public static Engraving byId(final String id) {
		for (Engraving e : values()) {
			if (e.id.equals(id)) {
				return e;
			}
		}
		return null;
	}
}
