package com.minecraftmode.job.gear.content;

import static com.minecraftmode.job.engrave.EngraveStat.*;

import com.minecraftmode.job.JobClass;

/**
 * Warrior plate: from a recruit's mail to the Knights of the Round and Avalon. Themes: toughness,
 * lifesteal and execution for the berserker line, damage reduction and thorns for the bastions.
 */
public final class WarriorArmor extends ArmorContent {
	public WarriorArmor() {
		super(JobClass.WARRIOR);
	}

	@Override
	public void define() {
		set("recruit_mail", 10, "Recruit's Mail", "신병의 사슬갑옷", 0x9AA3AD, 0x6B4A2B, 0xC9A227,
			two(line(MAX_HEALTH, 2)),
			three(line(DAMAGE_REDUCTION, 3)),
			four(line(BASIC_DAMAGE, 8)));
		set("gladiator_harness", 20, "Gladiator's Harness", "검투사의 마구", 0xB0773A, 0x8C2E2E, 0xE8C45A,
			two(line(ATTACK_SPEED, 5)),
			three(line(LIFESTEAL, 2)),
			four(line(DOUBLE_STRIKE, 8)));
		set("iron_bastion", 30, "Iron Bastion", "강철 보루", 0x5A6270, 0x2E4E9E, 0xD9B44A,
			two(line(ARMOR, 2)),
			three(line(KNOCKBACK_RES, 30), line(THORNS, 10)),
			four(line(LAST_STAND, 20)));
		set("crimson_berserker", 40, "Crimson Berserker", "진홍 광전사", 0x7A1418, 0x2A1A1A, 0xFF4A3A,
			two(line(BASIC_DAMAGE, 10)),
			three(line(LIFESTEAL, 3), line(KILL_HEAL, 2)),
			four(line(EXECUTE, 25)));
		set("dragonbone_plate", 50, "Dragonbone Plate", "용골 판금", 0xD8D0B8, 0x3E6B3A, 0x7FD67A,
			two(line(MAX_HEALTH, 4)),
			three(line(BOSS_DAMAGE, 10)),
			four(line(SKILL_DAMAGE, 15), line(DAMAGE_REDUCTION, 5)));
		set("round_table", 60, "Knight of the Round", "원탁의 기사", 0xC8D0DC, 0x2E4E9E, 0xE8C45A,
			two(line(DAMAGE_REDUCTION, 5)),
			three(line(SKILL_DAMAGE, 10)),
			four(line(HEALTH_REGEN, 2), line(LAST_STAND, 20)));
		set("mad_enhancement", 70, "Mad Enhancement", "광화", 0x1E1A24, 0x5A1E5A, 0xC0263A,
			two(line(ATTACK_SPEED, 8)),
			three(line(CRIT_CHANCE, 10)),
			four(line(DOUBLE_STRIKE, 15), line(BASIC_DAMAGE, 10)));
		set("titan_aegis", 80, "Titan's Aegis", "타이탄의 아이기스", 0xD9A632, 0x8A5A2A, 0xFFF0A0,
			two(line(ARMOR, 4)),
			three(line(THORNS, 25)),
			four(line(MAX_HEALTH, 8), line(KNOCKBACK_RES, 50)));
		set("king_of_knights", 90, "Regalia of the King of Knights", "기사왕의 예장", 0x2E4E9E, 0xDDE6F0, 0xE8C45A,
			two(line(SKILL_DAMAGE, 12)),
			three(line(COOLDOWN_FLAT, 1.0F)),
			four(line(BOSS_DAMAGE, 20), line(CRIT_DAMAGE, 30)));
		set("avalon_raiment", 100, "Avalon Raiment", "아발론의 성의", 0xF4F4F0, 0xE8C45A, 0x9FF4FF,
			two(line(DAMAGE_REDUCTION, 8), line(MAX_HEALTH, 6)),
			three(line(BASIC_DAMAGE, 20), line(LIFESTEAL, 4)),
			four(line(LAST_STAND, 40), line(HEALTH_REGEN, 4)));
	}
}
