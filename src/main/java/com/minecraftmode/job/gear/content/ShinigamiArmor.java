package com.minecraftmode.job.gear.content;

import static com.minecraftmode.job.engrave.EngraveStat.*;

import com.minecraftmode.job.JobClass;

/**
 * Soul Reaper robes (Bleach): the academy uniform, the black shihakusho of the Gotei 13, the
 * lieutenant's armband, the Onmitsukido, the Kido Corps, the captain's haori, the Visored, the
 * Royal Guard, Tensa Zangetsu's coat and the form of Mugetsu. Themes: skills and cooldowns,
 * speed and dodging, boss damage.
 */
public final class ShinigamiArmor extends ArmorContent {
	public ShinigamiArmor() {
		super(JobClass.SHINIGAMI);
	}

	@Override
	public void define() {
		set("academy_uniform", 10, "Academy Uniform", "진앙학원 교복", 0xF0F0F0, 0x3A6AD8, 0xC0262D,
			two(line(MAX_MANA, 10)),
			three(line(MOVE_SPEED, 4)),
			four(line(SKILL_DAMAGE, 8)));
		set("shihakusho", 20, "Shihakusho", "사패장", 0x18181C, 0xF0F0F0, 0x9FD8E8,
			two(line(DODGE, 3)),
			three(line(BASIC_DAMAGE, 8)),
			four(line(COOLDOWN, 6)));
		set("lieutenant_armband", 30, "Lieutenant's Armband", "부대장 완장", 0x18181C, 0xE8E8E8, 0xE8C45A,
			two(line(SKILL_DAMAGE, 8)),
			three(line(MANA_REGEN, 1)),
			four(line(CRIT_CHANCE, 8)));
		set("onmitsukido", 40, "Onmitsukido Garb", "은밀기동 복장", 0x101014, 0x2A2A30, 0xE8C040,
			two(line(MOVE_SPEED, 6)),
			three(line(BACKSTAB, 20)),
			four(line(DODGE, 6), line(CRIT_DAMAGE, 20)));
		set("kido_corps", 50, "Kido Corps Vestments", "귀도중 법의", 0x2A2A3A, 0xE8E0D0, 0x8A5AFF,
			two(line(MAX_MANA, 20)),
			three(line(SKILL_AREA, 12)),
			four(line(SKILL_DAMAGE, 14), line(MANA_COST, 8)));
		set("captains_haori", 60, "Captain's Haori", "대장 하오리", 0xF4F4F0, 0x18181C, 0x3A3A44,
			two(line(DAMAGE_REDUCTION, 5)),
			three(line(BOSS_DAMAGE, 12)),
			four(line(COOLDOWN, 10), line(SKILL_DAMAGE, 10)));
		set("visored", 70, "Visored", "가면의 군세", 0xE8E8E8, 0x18181C, 0xC8202A,
			two(line(LIFESTEAL, 3)),
			three(line(CRIT_DAMAGE, 25)),
			four(line(BASIC_DAMAGE, 15), line(ATTACK_SPEED, 10)));
		set("royal_guard", 80, "Royal Guard", "영왕 호위대", 0xE8E0C0, 0xF4F4F0, 0xE8C45A,
			two(line(MAX_HEALTH, 6)),
			three(line(SKILL_AREA, 15)),
			four(line(BOSS_DAMAGE, 20), line(DAMAGE_REDUCTION, 6)));
		set("tensa_coat", 90, "Coat of Tensa Zangetsu", "천쇄참월의 코트", 0x101014, 0x2A1A1A, 0xC8202A,
			two(line(MOVE_SPEED, 8)),
			three(line(SKILL_DAMAGE, 15)),
			four(line(DODGE, 8), line(ECHO, 8)));
		set("mugetsu", 100, "Final Getsuga Tensho", "무월", 0x0C0C10, 0x3A1A1A, 0x6AC8FF,
			two(line(COOLDOWN, 10), line(MAX_MANA, 30)),
			three(line(SKILL_DAMAGE, 20), line(BASIC_DAMAGE, 20)),
			four(line(BOSS_DAMAGE, 25), line(CRIT_DAMAGE, 30)));
	}
}
