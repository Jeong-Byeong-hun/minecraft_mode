package com.minecraftmode.job.gear.content;

import static com.minecraftmode.job.engrave.EngraveStat.*;

import com.minecraftmode.job.JobClass;

/**
 * Archer gear: woodsmen and rangers, Robin Hood, Atalanta, EMIYA, Arash, Artemis, Arjuna and the
 * King of Heroes. Themes: extra arrows, shot damage, range and crits.
 */
public final class ArcherArmor extends ArmorContent {
	public ArcherArmor() {
		super(JobClass.ARCHER);
	}

	@Override
	public void define() {
		set("woodsman_garb", 10, "Woodsman's Garb", "나무꾼의 옷", 0x3E6B3A, 0x6B4A2B, 0xE8DCC0,
			two(line(SHOT_DAMAGE, 6)),
			three(line(MOVE_SPEED, 4)),
			four(line(CRIT_CHANCE, 6)));
		set("ranger_cloak", 20, "Ranger's Cloak", "레인저의 망토", 0x2F4F2F, 0x5A3E24, 0x8FBC8F,
			two(line(DRAW_SPEED, 10)),
			three(line(RANGE_BONUS, 3)),
			four(line(EXTRA_SHOT, 1)));
		set("sherwood_outlaw", 30, "Sherwood Outlaw", "셔우드의 무법자", 0x3B5E2B, 0x8C2E2E, 0xC8B488,
			two(line(POISON, 2)),
			three(line(DODGE, 5)),
			four(line(STEALTH_ON_KILL, 2), line(SHOT_DAMAGE, 8)));
		set("storm_hunter", 40, "Storm Hunter", "폭풍 사냥꾼", 0x4A5A6A, 0x2A3A4A, 0x9FD0FF,
			two(line(EXTRA_SHOT, 1)),
			three(line(EXTRA_SHOT, 1), line(SHOT_DAMAGE, 15)),
			four(line(CRIT_CHANCE, 15)));
		set("calydonian_huntress", 50, "Calydonian Huntress", "칼리돈의 사냥꾼", 0x4A7A3A, 0xD9B44A, 0xF4E8C0,
			two(line(MOVE_SPEED, 6)),
			three(line(SPEED_ON_KILL, 3)),
			four(line(BOSS_DAMAGE, 15), line(DRAW_SPEED, 15)));
		set("red_mantle", 60, "Red Mantle of the Archer", "붉은 성의", 0xA8282E, 0x141418, 0xE6E6E6,
			two(line(SKILL_DAMAGE, 10)),
			three(line(COOLDOWN_FLAT, 0.8F)),
			four(line(EXTRA_SHOT, 1), line(SHOT_DAMAGE, 12)));
		set("stella_bowman", 70, "Stella Bowman", "스텔라의 궁수", 0xD8C08A, 0x8A5A2A, 0x9FF4FF,
			two(line(RANGE_BONUS, 5)),
			three(line(SHOT_DAMAGE, 15)),
			four(line(CRIT_DAMAGE, 35)));
		set("grace_of_artemis", 80, "Grace of Artemis", "아르테미스의 은총", 0xD8DEE8, 0x5A7AB8, 0xE8FFFF,
			two(line(CRIT_CHANCE, 10)),
			three(line(CRIT_REFUND, 0.5F)),
			four(line(EXTRA_SHOT, 1), line(BOSS_DAMAGE, 15)));
		set("pashupata", 90, "Pashupata Raiment", "파슈파타 예복", 0xF4F4F0, 0x2E4E9E, 0xE8C45A,
			two(line(SHOT_DAMAGE, 15)),
			three(line(CHAIN_LIGHTNING, 12)),
			four(line(CRIT_CHANCE, 12), line(EXTRA_SHOT, 1)));
		set("king_of_heroes", 100, "Golden Armor of the King of Heroes", "영웅왕의 황금 갑주", 0xE8C45A, 0xA8282E, 0xFFF4C0,
			two(line(EXTRA_SHOT, 1), line(SHOT_DAMAGE, 15)),
			three(line(CRIT_CHANCE, 15), line(CRIT_DAMAGE, 40)),
			four(line(BOSS_DAMAGE, 25), line(EXTRA_SHOT, 1)));
	}
}
