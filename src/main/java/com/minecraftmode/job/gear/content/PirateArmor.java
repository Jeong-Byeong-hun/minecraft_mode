package com.minecraftmode.job.gear.content;

import static com.minecraftmode.job.engrave.EngraveStat.*;

import com.minecraftmode.job.JobClass;

/**
 * Pirate coats: deckhands and privateers, the Straw Hats and the Marines (One Piece), Blackbeard,
 * Drake's Golden Hind, the Emperors and Roger. Themes: loot and gold, guns, brawling and haki.
 */
public final class PirateArmor extends ArmorContent {
	public PirateArmor() {
		super(JobClass.PIRATE);
	}

	@Override
	public void define() {
		set("deckhand_clothes", 10, "Deckhand's Clothes", "갑판원의 옷", 0x2E4E9E, 0xE6E6E6, 0xC0262D,
			two(line(GOLD_FIND, 5)),
			three(line(MAX_HEALTH, 2)),
			four(line(BASIC_DAMAGE, 8)));
		set("privateer_coat", 20, "Privateer's Coat", "사략선장의 코트", 0x8C2E2E, 0x2A1A1A, 0xE8C45A,
			two(line(SHOT_DAMAGE, 8)),
			three(line(ITEM_FIND, 10)),
			four(line(DRAW_SPEED, 15)));
		set("straw_hat_crew", 30, "Straw Hat Crew", "밀짚모자 일당", 0xC0262D, 0x2E4E9E, 0xE8C45A,
			two(line(MAX_HEALTH, 3)),
			three(line(BASIC_DAMAGE, 10)),
			four(line(KILL_HEAL, 2), line(ITEM_FIND, 10)));
		set("vice_admiral", 40, "Vice Admiral's Coat", "해군 중장 코트", 0xF0F0F0, 0x2E4E9E, 0xE8C45A,
			two(line(DAMAGE_REDUCTION, 5)),
			three(line(SHOT_DAMAGE, 12)),
			four(line(EXTRA_SHOT, 1)));
		set("blackbeard_garb", 50, "Blackbeard's Garb", "검은수염의 의복", 0x1E1A1A, 0x5A3A26, 0x7A4AC8,
			two(line(LIFESTEAL, 3)),
			three(line(BOSS_DAMAGE, 10)),
			four(line(GOLD_FIND, 10), line(ITEM_FIND, 15)));
		set("golden_hind", 60, "Golden Hind Captain", "골든 하인드 선장", 0x9E1E28, 0xE8C45A, 0xF4E8C0,
			two(line(GOLD_FIND, 8)),
			three(line(SHOT_DAMAGE, 15)),
			four(line(CRIT_CHANCE, 10), line(ITEM_FIND, 15)));
		set("emperors_cape", 70, "Emperor's Cape", "사황의 망토", 0x141418, 0xA8282E, 0xE8C45A,
			two(line(KNOCKBACK_RES, 30)),
			three(line(SKILL_DAMAGE, 12)),
			four(line(CHAIN_LIGHTNING, 12)));
		set("whitebeard_mantle", 80, "Whitebeard's Mantle", "흰수염의 망토", 0xF0F0F0, 0x6B4A2B, 0xE8C45A,
			two(line(MAX_HEALTH, 6)),
			three(line(SKILL_AREA, 15)),
			four(line(BOSS_DAMAGE, 20), line(DAMAGE_REDUCTION, 6)));
		set("roger_coat", 90, "Roger's Captain Coat", "로저의 선장 코트", 0xA8282E, 0x141418, 0xE8C45A,
			two(line(ITEM_FIND, 15)),
			three(line(CRIT_DAMAGE, 30)),
			four(line(DOUBLE_STRIKE, 15), line(GOLD_FIND, 10)));
		set("pirate_king", 100, "Pirate King's Regalia", "해적왕의 예복", 0xE8C45A, 0xA8282E, 0xF4F4F0,
			two(line(ITEM_FIND, 20), line(GOLD_FIND, 10)),
			three(line(BASIC_DAMAGE, 20), line(SHOT_DAMAGE, 20)),
			four(line(CHAIN_LIGHTNING, 15), line(BOSS_DAMAGE, 25)));
	}
}
