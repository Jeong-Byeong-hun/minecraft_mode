package com.minecraftmode.job.content;

import static com.minecraftmode.job.skill.Actions.*;
import static com.minecraftmode.job.skill.CombatState.Stance.*;
import static com.minecraftmode.job.skill.SkillKind.*;
import static com.minecraftmode.job.weapon.Archetype.*;
import static com.minecraftmode.registry.ModEffects.BLEEDING;
import static com.minecraftmode.registry.ModEffects.VULNERABLE;
import static net.minecraft.world.effect.MobEffects.*;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.skill.Fx;
import com.minecraftmode.job.weapon.ProjectileStyle;

/**
 * Warrior: Warrior -> Gladiator -> Berserker -> King of Knights. Heavy blades, spears and polearms;
 * tier 3-4 borrow from Berserk (Dragonslayer), Lu Bu and the Knights of the Round Table.
 */
public final class WarriorContent extends ClassContent {
	public WarriorContent() {
		super(JobClass.WARRIOR);
	}

	@Override
	public void define() {
		// ---------------------------------------------------------------- tier 1: Warrior
		weapon("iron_greatsword", 1, 10, GREATSWORD, "Iron Greatsword", "철 대검",
			art(0xB8C0C8, 0x6B4A2B, 0xA33A2A), fx(Fx.Kind.SLASH, 0xE0E0E0),
			skill("crushing_cleave", "Crushing Cleave", "분쇄 베기", ATTACK, 6, 10, slash(4.0, 150, 1.6), inflict(SLOWNESS, 2, 0)),
			skill("iron_wall", "Iron Wall", "철벽", DEFENSE, 14, 12, stance(GUARD, 5, 40)),
			skill("charging_step", "Charging Step", "돌격 보법", MOVEMENT, 10, 8, dash(5, 0.8)));

		weapon("soldier_spear", 1, 12, SPEAR, "Soldier's Spear", "병사의 창",
			art(0xC9CED6, 0x8C6940, 0x2E6DB4), fx(Fx.Kind.SPARK, 0xCFE8FF),
			skill("piercing_thrust", "Piercing Thrust", "꿰뚫는 찌르기", ATTACK, 5, 8, beam(6, 1.5)),
			skill("sweeping_arc", "Sweeping Arc", "휩쓸기", ATTACK, 9, 12, slash(4.5, 240, 1.2), push(4, 1.0)),
			skill("pole_vault", "Pole Vault", "장대 도약", MOVEMENT, 12, 8, leap(4, 3, 1.0)));

		weapon("ronin_katana", 1, 15, KATANA, "Ronin's Katana", "낭인의 태도",
			art(0xDDE3EA, 0x2B2B3A, 0xC0392B), fx(Fx.Kind.PETAL, 0xFFB7C5),
			skill("iaido", "Iaido", "발도술", ATTACK, 6, 10, dash(4, 1.8)),
			skill("petal_storm", "Petal Storm", "꽃잎 폭풍", ATTACK, 10, 14, repeat(3, 6, nova(3.5, 0.7))),
			skill("focused_breath", "Focused Breath", "호흡 집중", UTILITY, 16, 10, buff(SPEED, 6, 1), empower(3, 40, 8)));

		// ---------------------------------------------------------------- tier 2: Gladiator
		weapon("gladiator_longsword", 2, 25, LONGSWORD, "Gladiator's Longsword", "투사의 장검",
			art(0xC9A26B, 0x5A2D1A, 0xD94141), fx(Fx.Kind.SLASH, 0xFFD27F),
			skill("arena_combo", "Arena Combo", "투기장 연격", ATTACK, 7, 14, repeat(3, 5, slash(3.5, 120, 0.9))),
			skill("crowds_roar", "Crowd's Roar", "관중의 함성", UTILITY, 20, 16, allyBuff(8, STRENGTH, 10, 0), taunt(8)),
			skill("pommel_bash", "Pommel Bash", "자루 강타", ATTACK, 10, 12, stun(1.5), slash(3, 90, 1.4)));

		weapon("retiarius_trident", 2, 27, SPEAR, "Retiarius Trident", "레티아리우스 삼지창",
			art(0x9FB8C8, 0x3E5A6B, 0x1ABC9C), fx(Fx.Kind.BUBBLE, 0x7FD6FF),
			skill("net_cast", "Net Cast", "그물 투척", UTILITY, 12, 12, shoot(ProjectileStyle.HARPOON, 1.2).pierce(2), inflict(SLOWNESS, 4, 2)),
			skill("trident_rush", "Trident Rush", "삼지창 돌격", MOVEMENT, 9, 12, dash(7, 1.4)),
			skill("tidal_spin", "Tidal Spin", "조수 회전", ATTACK, 11, 16, pull(6, 1.2), delay(0.4, nova(4.5, 1.6))));

		weapon("executioner_axe", 2, 30, BATTLEAXE, "Executioner's Axe", "처형인의 도끼",
			art(0x8E8E8E, 0x3B2416, 0x7A0000), fx(Fx.Kind.SHARD, 0xB22222),
			skill("behead", "Behead", "참수", ATTACK, 12, 16, execute(4, 2.2, 30, 2.0)),
			skill("bloody_whirl", "Bloody Whirl", "피의 선풍", ATTACK, 10, 16, lifesteal(20), repeat(2, 8, nova(3.5, 1.0))),
			skill("intimidate", "Intimidate", "위압", UTILITY, 16, 10, debuff(5, WEAKNESS, 6, 1)));

		weapon("war_maul", 2, 33, WARHAMMER, "War Maul", "전쟁 망치",
			art(0x7D7F86, 0x4A3424, 0xE67E22), fx(Fx.Kind.SHARD, 0xC8A27A),
			skill("earthshaker", "Earthshaker", "대지 진동", ATTACK, 9, 16, nova(5, 1.5), stun(1.0)),
			skill("meteor_smash", "Meteor Smash", "운석 강타", ATTACK, 14, 20, leap(6, 4.5, 2.2)),
			skill("iron_will", "Iron Will", "강철 의지", DEFENSE, 18, 14, buff(RESISTANCE, 6, 1), cleanse()));

		weapon("champion_halberd", 2, 36, HALBERD, "Champion's Halberd", "챔피언의 할버드",
			art(0xCED6E0, 0x6B3E26, 0x2980B9), fx(Fx.Kind.SLASH, 0xA9D6FF),
			skill("halberd_sweep", "Halberd Sweep", "할버드 휩쓸기", ATTACK, 7, 14, slash(5.5, 200, 1.4)),
			skill("hook_pull", "Hook Pull", "갈고리 당기기", UTILITY, 10, 10, pull(7, 1.5), debuff(7, SLOWNESS, 3, 1)),
			skill("impale", "Impale", "꿰뚫기", ATTACK, 12, 18, beam(8, 2.0), inflict(BLEEDING, 5, 0)));

		weapon("flamberge", 2, 40, GREATSWORD, "Flamberge", "플랑베르주",
			art(0xC0C4CA, 0x5B2C17, 0xFF6A00), fx(Fx.Kind.ORB, 0xFF7A1A),
			skill("flame_wave", "Flame Wave", "화염파", ATTACK, 8, 16, shoot(ProjectileStyle.WAVE, 1.6).pierce(5), burn(4)),
			skill("blazing_cleave", "Blazing Cleave", "업화 베기", ATTACK, 10, 18, slash(4.5, 160, 2.0), burn(5)),
			skill("firewall", "Firewall", "불의 장벽", UTILITY, 18, 20, zone(4, 5, 0.8).at(12)));

		// ---------------------------------------------------------------- tier 3: Berserker
		weapon("dragonslayer", 3, 45, GREATSWORD, "Dragonslayer", "드래곤 슬레이어",
			art(0x5A5A5A, 0x2A1A10, 0x8B0000), fx(Fx.Kind.SHARD, 0x8B0000),
			skill("dragon_cleaver", "Dragon Cleaver", "용 베기", ATTACK, 9, 20, slash(5.5, 180, 2.6)),
			skill("berserker_armor", "Berserker Armor", "광전사의 갑주", DEFENSE, 30, 25, buff(STRENGTH, 10, 2), buff(RESISTANCE, 10, 1), empower(8, 50, 10)),
			skill("black_swordsman", "Black Swordsman", "검은 검사", ATTACK, 14, 24, lifesteal(10), repeat(4, 5, nova(4, 1.1))));

		weapon("bloodreaver", 3, 48, BATTLEAXE, "Bloodreaver", "피의 도끼",
			art(0x6E1F1F, 0x2B1B12, 0xFF2D2D), fx(Fx.Kind.SHARD, 0xFF2D2D),
			skill("rend", "Rend", "찢어발기기", ATTACK, 8, 18, slash(4, 120, 1.8), inflict(BLEEDING, 8, 1)),
			skill("blood_frenzy", "Blood Frenzy", "피의 광란", UTILITY, 20, 20, buff(HASTE, 10, 2), buff(SPEED, 10, 1)),
			skill("crimson_harvest", "Crimson Harvest", "진홍의 수확", ATTACK, 15, 26, lifesteal(30), nova(5, 2.0)));

		weapon("ravager_maul", 3, 52, WARHAMMER, "Ravager Maul", "약탈자의 철퇴",
			art(0x4F5359, 0x3B2A1E, 0xB8860B), fx(Fx.Kind.SHARD, 0xA0A0A0),
			skill("cataclysm", "Cataclysm", "대격변", ATTACK, 15, 26, leap(8, 6, 2.8), stun(1.5)),
			skill("shockwave", "Shockwave", "충격파", ATTACK, 9, 18, shoot(ProjectileStyle.WAVE, 1.8).count(3).spread(40).pierce(8)),
			skill("unstoppable", "Unstoppable", "저지 불가", DEFENSE, 22, 20, cleanse(), buff(RESISTANCE, 6, 2), buff(SPEED, 6, 1)));

		weapon("sky_piercer", 3, 55, HALBERD, "Sky Piercer", "방천화극",
			art(0xE0C060, 0x7A1E1E, 0xFF4040), fx(Fx.Kind.SLASH, 0xFF5A36),
			skill("heaven_thrust", "Heaven Thrust", "천공 찌르기", ATTACK, 9, 20, beam(10, 2.4), inflict(BLEEDING, 4, 1)),
			skill("god_force", "God Force", "군신오병", ATTACK, 16, 28, strike(14, 3.5, 5, 1.6)),
			skill("red_hare", "Red Hare", "적토마", MOVEMENT, 14, 14, buff(SPEED, 8, 2), dash(8, 1.2)));

		weapon("muramasa", 3, 60, KATANA, "Muramasa", "무라마사",
			art(0xE8E8F0, 0x1A1A1A, 0x9B111E), fx(Fx.Kind.PETAL, 0xD7263D),
			skill("cursed_edge", "Cursed Edge", "요도의 저주", ATTACK, 8, 18, inflict(WITHER, 5, 1), dash(6, 2.0)),
			skill("thousand_cuts", "Thousand Cuts", "천 번 베기", ATTACK, 13, 24, repeat(6, 3, slash(4, 360, 0.6))),
			skill("bloodlust", "Bloodlust", "혈욕", UTILITY, 20, 18, empower(5, 80, 10), buff(SPEED, 10, 1)));

		weapon("scythe_of_ruin", 3, 65, SCYTHE, "Scythe of Ruin", "파멸의 대낫",
			art(0x9AA0A8, 0x2E2E2E, 0x6A0DAD), fx(Fx.Kind.SMOKE, 0x6A0DAD),
			skill("reap", "Reap", "수확", ATTACK, 8, 18, lifesteal(15), slash(5, 270, 1.8)),
			skill("deaths_pull", "Death's Pull", "죽음의 인도", UTILITY, 12, 16, pull(8, 1.8), debuff(8, WEAKNESS, 5, 1)),
			skill("grim_harvest", "Grim Harvest", "사신의 수확", ATTACK, 18, 28, execute(5, 2.5, 35, 2.5)));

		// ---------------------------------------------------------------- tier 4: King of Knights
		weapon("excalibur", 4, 70, LONGSWORD, "Excalibur", "엑스칼리버",
			art(0xF0F4FF, 0x1F3A93, 0xFFD700), fx(Fx.Kind.SLASH, 0xFFE680),
			skill("promised_victory", "Sword of Promised Victory", "약속된 승리의 검", ULTIMATE, 45, 40, beam(24, 4.5)),
			skill("strike_air", "Strike Air", "풍왕철추", ATTACK, 16, 18, push(5, 2.0), shoot(ProjectileStyle.WAVE, 2.0).pierce(10)),
			skill("mana_burst", "Mana Burst", "마력 방출", MOVEMENT, 10, 16, dash(8, 1.6)),
			skill("avalon_guard", "Avalon's Protection", "아발론의 가호", DEFENSE, 30, 25, stance(GUARD, 6, 60), heal(25)));

		weapon("excalibur_morgan", 4, 74, GREATSWORD, "Excalibur Morgan", "엑스칼리버 모르간",
			art(0x2B2B33, 0x111111, 0xC0102E), fx(Fx.Kind.SMOKE, 0xB0102E),
			skill("vortigern", "Vortigern", "보티건", ULTIMATE, 45, 40, inflict(WITHER, 6, 1), beam(22, 4.0)),
			skill("dark_mana_burst", "Dark Mana Burst", "흑마력 방출", ATTACK, 10, 20, nova(5, 2.2), push(5, 1.5)),
			skill("tyrants_charge", "Tyrant's Charge", "폭군의 돌진", MOVEMENT, 12, 16, stun(1.0), dash(9, 2.0)),
			skill("black_armor", "Black Armor", "흑기사의 갑주", DEFENSE, 25, 24, stance(COUNTER, 6, 60), buff(RESISTANCE, 6, 1)));

		weapon("rhongomyniad", 4, 76, SPEAR, "Rhongomyniad", "롱고미니아드",
			art(0xEAF2FF, 0xD4AF37, 0x00BFFF), fx(Fx.Kind.BOLT, 0xBFEFFF),
			skill("pillar_of_light", "Pillar of Light", "최후의 탑", ULTIMATE, 50, 40, lightning(20, 5, 3.5), strike(20, 4, 3, 1.8)),
			skill("storm_thrust", "Storm Thrust", "폭풍 찌르기", ATTACK, 7, 18, beam(12, 2.4)),
			skill("lion_kings_charge", "Lion King's Charge", "사자왕의 돌격", MOVEMENT, 12, 18, dash(10, 1.8)),
			skill("holy_lance_ward", "Holy Lance Ward", "성창의 가호", DEFENSE, 24, 22, allyBuff(8, RESISTANCE, 8, 1), allyHeal(8, 20)));

		weapon("arondight", 4, 80, LONGSWORD, "Arondight", "아론다이트",
			art(0xBFC9D9, 0x16213E, 0x4169E1), fx(Fx.Kind.SLASH, 0x7FA7FF),
			skill("overload", "Arondight Overload", "무궁의 무련", ULTIMATE, 40, 36, lifesteal(10), repeat(5, 4, slash(5, 200, 1.4))),
			skill("knight_of_the_lake", "Knight of the Lake", "호수의 기사", DEFENSE, 20, 20, stance(EVADE, 6, 40), buff(SPEED, 6, 1)),
			skill("lake_slash", "Lake Slash", "호수 베기", ATTACK, 7, 18, shoot(ProjectileStyle.WAVE, 2.2).count(3).spread(30).pierce(4)),
			skill("unblemished", "Unblemished", "무결", UTILITY, 26, 18, cleanse(), heal(20), buff(REGENERATION, 6, 1)));

		weapon("clarent", 4, 84, GREATSWORD, "Clarent", "클래런트",
			art(0xD9D9D9, 0x8B0000, 0xFF3030), fx(Fx.Kind.BOLT, 0xFF4040),
			skill("blood_arthur", "Clarent Blood Arthur", "아버지에게 바치는 반역", ULTIMATE, 45, 40, lightning(16, 5, 3.0), beam(18, 3.0)),
			skill("red_lightning", "Red Lightning", "적뢰", ATTACK, 9, 20, chain(12, 5, 1.8)),
			skill("rebels_charge", "Rebel's Charge", "반역의 돌진", MOVEMENT, 11, 16, stun(1.0), dash(9, 1.8)),
			skill("secret_of_pedigree", "Secret of Pedigree", "불명의 투구", UTILITY, 25, 20, stealth(4, 80), buff(SPEED, 4, 2)));

		weapon("galatine", 4, 88, LONGSWORD, "Galatine", "갈라틴",
			art(0xFFF2CC, 0x8B5A2B, 0xFF8C00), fx(Fx.Kind.ORB, 0xFFB347),
			skill("excalibur_galatine", "Excalibur Galatine", "전승의 태양검", ULTIMATE, 45, 40, burn(6), strike(16, 5, 1, 4.5)),
			skill("sunlight_blessing", "Sunlight Blessing", "태양의 축복", UTILITY, 28, 22, allyBuff(10, STRENGTH, 12, 1), allyBuff(10, REGENERATION, 6, 0)),
			skill("solar_flare", "Solar Flare", "태양 폭발", ATTACK, 10, 20, burn(5), nova(5, 2.2)),
			skill("noon_strike", "Noon Strike", "정오의 일격", ATTACK, 7, 16, slash(5, 150, 2.4)));

		weapon("ascalon", 4, 92, SPEAR, "Ascalon", "아스칼론",
			art(0xE6E6E6, 0x4B3621, 0x2ECC71), fx(Fx.Kind.RING, 0x7DFFB0),
			skill("abyssus_draconis", "Abyssus Draconis", "역성의 용", ULTIMATE, 50, 40, mark(20, 10, 60), beam(16, 3.5)),
			skill("dragonbane_thrust", "Dragonbane Thrust", "용살 찌르기", ATTACK, 7, 18, inflict(VULNERABLE, 6, 1), beam(10, 2.6)),
			skill("martyrs_shield", "Martyr's Shield", "순교자의 방패", DEFENSE, 26, 24, shield(16, 10), taunt(10)),
			skill("saints_ward", "Saint's Ward", "성자의 수호", UTILITY, 30, 26, allyHeal(10, 30), cleanse()));

		weapon("kusanagi", 4, 96, KATANA, "Kusanagi no Tsurugi", "쿠사나기노츠루기",
			art(0xF5F5F5, 0x0B3D2E, 0x40E0D0), fx(Fx.Kind.PETAL, 0x9EFFE6),
			skill("ame_no_murakumo", "Ame-no-Murakumo", "천총운검", ULTIMATE, 50, 40, pull(8, 1.0), repeat(4, 6, nova(6, 1.6))),
			skill("grass_cutter", "Grass Cutter", "풀베기", ATTACK, 6, 16, slash(6, 300, 1.8)),
			skill("wind_step", "Wind Step", "풍보", MOVEMENT, 8, 12, dash(10, 1.2)),
			skill("eight_headed_bane", "Eight-Headed Bane", "팔기의 화근", ATTACK, 16, 24, shoot(ProjectileStyle.WAVE, 1.4).count(8).spread(315).pierce(3)));
	}
}
