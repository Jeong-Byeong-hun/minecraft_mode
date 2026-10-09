package com.minecraftmode.job.gear.content;

import static com.minecraftmode.job.engrave.EngraveStat.*;

import com.minecraftmode.job.JobClass;

/**
 * Rogue leathers: thieves and shinobi (Naruto), the Phantom Troupe and the Zoldyck family (Hunter x
 * Hunter), Hassan, the Akatsuki and the Shadow Monarch. Themes: crits, backstabs, stealth on kill.
 */
public final class RogueArmor extends ArmorContent {
	public RogueArmor() {
		super(JobClass.ROGUE);
	}

	@Override
	public void define() {
		set("pickpocket_leathers", 10, "Pickpocket's Leathers", "소매치기의 가죽옷", 0x6B4A2B, 0x4B4F57, 0xC9A227,
			two(line(MOVE_SPEED, 4)),
			three(line(GOLD_FIND, 5)),
			four(line(CRIT_CHANCE, 6)));
		set("nightstalker", 20, "Nightstalker", "밤사냥꾼", 0x1E2A44, 0x101014, 0x6FA8FF,
			two(line(BACKSTAB, 10)),
			three(line(DODGE, 5)),
			four(line(STEALTH_ON_KILL, 2)));
		set("leaf_shinobi", 30, "Leaf Shinobi", "나뭇잎 시노비", 0x3E6B3A, 0x1E2A44, 0xC0C8D0,
			two(line(ATTACK_SPEED, 6)),
			three(line(EXTRA_SHOT, 1)),
			four(line(CRIT_CHANCE, 8), line(COOLDOWN_FLAT, 0.5F)));
		set("venom_fang", 40, "Venom Fang", "독아", 0x2F5A2A, 0x4A2A5A, 0x9ACD32,
			two(line(POISON, 2)),
			three(line(CRIT_DAMAGE, 20)),
			four(line(EXECUTE, 25)));
		set("phantom_troupe", 50, "Phantom Troupe", "환영여단", 0x141418, 0xE6E6E6, 0x8C2E2E,
			two(line(CRIT_CHANCE, 8)),
			three(line(SPEED_ON_KILL, 3)),
			four(line(DOUBLE_STRIKE, 12), line(BACKSTAB, 15)));
		set("hundred_faces", 60, "Cloak of a Hundred Faces", "백모의 망토", 0x1A1A1E, 0xE8E0C8, 0x8A6ACF,
			two(line(DODGE, 6)),
			three(line(STEALTH_ON_KILL, 2), line(MOVE_SPEED, 5)),
			four(line(BACKSTAB, 30)));
		set("crimson_cloud", 70, "Crimson Cloud Robe", "붉은 구름 망토", 0x141418, 0xC0263A, 0xE6E6E6,
			two(line(SKILL_DAMAGE, 10)),
			three(line(COOLDOWN_FLAT, 1.0F)),
			four(line(CHAIN_LIGHTNING, 12)));
		set("zoldyck_garb", 80, "Zoldyck Garb", "조르딕가의 의복", 0x5E6670, 0x2E4E9E, 0x9FF4FF,
			two(line(ATTACK_SPEED, 10)),
			three(line(CHAIN_LIGHTNING, 10)),
			four(line(CRIT_CHANCE, 12), line(CRIT_DAMAGE, 30)));
		set("shadow_legion", 90, "Shadow Legion Armor", "그림자 군단 갑주", 0x1A1428, 0x3A1E5A, 0x7A4AC8,
			two(line(KILL_HEAL, 3), line(KILL_MANA, 5)),
			three(line(BOSS_DAMAGE, 15)),
			four(line(DOUBLE_STRIKE, 20)));
		set("shadow_monarch", 100, "Garb of the Shadow Monarch", "그림자 군주의 의복", 0x0E0E16, 0x2A2A44, 0x3AA8E8,
			two(line(DODGE, 8), line(MOVE_SPEED, 8)),
			three(line(CRIT_DAMAGE, 40), line(BACKSTAB, 25)),
			four(line(STEALTH_ON_KILL, 3), line(EXECUTE, 40)));
	}
}
