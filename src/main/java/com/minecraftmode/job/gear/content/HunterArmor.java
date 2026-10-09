package com.minecraftmode.job.gear.content;

import static com.minecraftmode.job.engrave.EngraveStat.*;

import com.minecraftmode.job.JobClass;

/**
 * Hunter outfits (Hunter x Hunter): the Hunter Exam, Heavens Arena, the Zoldyck butlers, Greed
 * Island, the Nostrade bodyguards, the Chimera Ant extermination team, the Zodiacs, Killua's Godspeed,
 * the Kurta clan and Chairman Netero's gi. Themes: criticals and double strikes, toughness,
 * finding loot.
 */
public final class HunterArmor extends ArmorContent {
	public HunterArmor() {
		super(JobClass.HUNTER);
	}

	@Override
	public void define() {
		set("exam_outfit", 10, "Hunter Exam Outfit", "헌터 시험 복장", 0x3A8A3A, 0x2A2A30, 0xF5862B,
			two(line(MAX_HEALTH, 2)),
			three(line(ITEM_FIND, 8)),
			four(line(CRIT_CHANCE, 6)));
		set("heavens_arena", 20, "Heavens Arena Gi", "천공투기장 도복", 0xF0F0F0, 0x2A2A30, 0xC0262D,
			two(line(BASIC_DAMAGE, 6)),
			three(line(DAMAGE_REDUCTION, 4)),
			four(line(DOUBLE_STRIKE, 6)));
		set("zoldyck_butler", 30, "Zoldyck Butler's Suit", "조르딕가 집사복", 0x18181C, 0xF0F0F0, 0x5AA0FF,
			two(line(KNOCKBACK_RES, 20)),
			three(line(CRIT_DAMAGE, 15)),
			four(line(EXECUTE, 15), line(MOVE_SPEED, 4)));
		set("greed_island", 40, "Greed Island Gear", "그리드 아일랜드 장비", 0x6A4A2A, 0xE8E0C0, 0xE8C040,
			two(line(ITEM_FIND, 12)),
			three(line(GOLD_FIND, 6)),
			four(line(SKILL_DAMAGE, 12), line(KILL_HEAL, 2)));
		set("nostrade_guard", 50, "Nostrade Bodyguard Suit", "노스트라드 경호원 정장", 0x1A1A20, 0xF0F0F0, 0xC8202A,
			two(line(LIFESTEAL, 3)),
			three(line(CRIT_CHANCE, 8)),
			four(line(BACKSTAB, 20), line(CRIT_DAMAGE, 20)));
		set("chimera_hunt", 60, "Chimera Ant Hunt Gear", "키메라 앤트 토벌 장비", 0x4A5A3A, 0x2A2A20, 0xF5862B,
			two(line(DAMAGE_REDUCTION, 5)),
			three(line(BOSS_DAMAGE, 12)),
			four(line(MAX_HEALTH, 6), line(DOUBLE_STRIKE, 8)));
		set("zodiac_suit", 70, "Zodiac Suit", "십이지 정장", 0x2A2A3A, 0xE8E8F0, 0xE8C45A,
			two(line(SKILL_DAMAGE, 10)),
			three(line(COOLDOWN, 8)),
			four(line(CRIT_CHANCE, 10), line(ITEM_FIND, 15)));
		set("godspeed_gear", 80, "Godspeed Gear", "신속의 장비", 0xE8F4FF, 0x5AA0FF, 0xF0F0F0,
			two(line(MOVE_SPEED, 8)),
			three(line(ATTACK_SPEED, 12)),
			four(line(CHAIN_LIGHTNING, 12), line(DODGE, 8)));
		set("kurta_garb", 90, "Kurta Clan Garb", "쿠르타족 의상", 0x2E4E9E, 0xE8E0C0, 0xFF2A2A,
			two(line(MANA_REGEN, 1), line(MAX_MANA, 20)),
			three(line(SKILL_DAMAGE, 15)),
			four(line(CRIT_DAMAGE, 30), line(BOSS_DAMAGE, 15)));
		set("netero_gi", 100, "Chairman Netero's Gi", "네테로 회장의 도복", 0xF4F4F0, 0x18181C, 0xFFE070,
			two(line(DAMAGE_REDUCTION, 6), line(MAX_HEALTH, 6)),
			three(line(DOUBLE_STRIKE, 15), line(BASIC_DAMAGE, 20)),
			four(line(CRIT_CHANCE, 12), line(BOSS_DAMAGE, 25)));
	}
}
