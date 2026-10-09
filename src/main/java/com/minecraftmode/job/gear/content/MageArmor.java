package com.minecraftmode.job.gear.content;

import static com.minecraftmode.job.engrave.EngraveStat.*;

import com.minecraftmode.job.JobClass;

/**
 * Mage robes: apprentices and elementalists, Fairy Tail, Kidō masters (Bleach), Merlin, the Crimson
 * Demons (Konosuba), Frieren and the Grand Caster. Themes: MP, cooldowns, echo and area.
 */
public final class MageArmor extends ArmorContent {
	public MageArmor() {
		super(JobClass.MAGE);
	}

	@Override
	public void define() {
		set("apprentice_robes", 10, "Apprentice Robes", "견습생의 로브", 0x3A5AA8, 0xE6E6E6, 0x9FD0FF,
			two(line(MAX_MANA, 15)),
			three(line(MANA_REGEN, 1)),
			four(line(SKILL_DAMAGE, 8)));
		set("ember_weave", 20, "Ember Weave", "불씨 직물", 0xA8340C, 0x3A1A0A, 0xFFC040,
			two(line(BURN, 1)),
			three(line(SKILL_AREA, 8)),
			four(line(SKILL_DAMAGE, 10)));
		set("frostbound", 30, "Frostbound Vestments", "서리 결속 예복", 0x8CD8F2, 0x2A5A8A, 0xE8FFFF,
			two(line(SLOW, 1)),
			three(line(MANA_SHIELD, 10)),
			four(line(COOLDOWN_FLAT, 0.5F), line(SKILL_DAMAGE, 8)));
		set("fairy_guild", 40, "Fairy Guild Coat", "요정 길드 코트", 0x1E2A5A, 0xE8C45A, 0xFF7FB0,
			two(line(MAX_MANA, 25)),
			three(line(KILL_MANA, 5)),
			four(line(ECHO, 10)));
		set("kido_master", 50, "Kido Master's Haori", "귀도 달인의 하오리", 0xF0F0F0, 0x141418, 0x6FA8FF,
			two(line(SKILL_DAMAGE, 10)),
			three(line(COOLDOWN_FLAT, 0.8F)),
			four(line(SKILL_AREA, 15), line(CRIT_CHANCE, 8)));
		set("flower_mage", 60, "Robe of the Flower Mage", "꽃의 마술사 로브", 0xF4F4F8, 0xB9A3DC, 0xF2A6C8,
			two(line(HEALTH_REGEN, 1)),
			three(line(HEAL_ON_SKILL, 2)),
			four(line(MANA_REGEN, 3), line(SKILL_DAMAGE, 12)));
		set("crimson_demon", 70, "Crimson Demon Attire", "홍마족 의상", 0x141418, 0xC0263A, 0xFFB347,
			two(line(SKILL_DAMAGE, 15)),
			three(line(SKILL_AREA, 15)),
			four(line(BOSS_DAMAGE, 20), line(MANA_COST, 15)));
		set("akashic_scholar", 80, "Akashic Scholar", "아카식 학자", 0xD9A632, 0x1E3A6E, 0x9FF4FF,
			two(line(COOLDOWN_FLAT, 1.0F)),
			three(line(ECHO, 12)),
			four(line(MAX_MANA, 50), line(MANA_REGEN, 3)));
		set("elf_mage", 90, "Mantle of the Elf Mage", "엘프 마법사의 망토", 0xF4F4F0, 0xD9B44A, 0x7FD6A0,
			two(line(SKILL_DAMAGE, 15)),
			three(line(MANA_SHIELD, 15)),
			four(line(CHAIN_LIGHTNING, 15), line(COOLDOWN_FLAT, 1.0F)));
		set("grand_caster", 100, "Grand Caster Regalia", "그랜드 캐스터 예장", 0x4A1E7A, 0xE8C45A, 0xE0A0FF,
			two(line(MAX_MANA, 60), line(MANA_REGEN, 4)),
			three(line(SKILL_DAMAGE, 25)),
			four(line(ECHO, 20), line(COOLDOWN_FLAT, 1.5F)));
	}
}
