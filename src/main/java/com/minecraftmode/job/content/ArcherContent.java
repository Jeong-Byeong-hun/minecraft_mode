package com.minecraftmode.job.content;

import static com.minecraftmode.job.skill.Actions.*;
import static com.minecraftmode.job.skill.CombatState.Stance.*;
import static com.minecraftmode.job.skill.SkillKind.*;
import static com.minecraftmode.job.weapon.Archetype.*;
import static com.minecraftmode.registry.ModEffects.BLEEDING;
import static com.minecraftmode.registry.ModEffects.MANA_FLOW;
import static com.minecraftmode.registry.ModEffects.STUN;
import static com.minecraftmode.registry.ModEffects.VULNERABLE;
import static net.minecraft.world.effect.MobEffects.*;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.skill.Fx;
import com.minecraftmode.job.weapon.ProjectileStyle;

/**
 * Archer: Bowman -> Ranger -> Heroic Archer -> King of Heroes. Bows and crossbows with a few twin blades;
 * tier 1-2 are hunters and rangers (Legolas, Zhuge Liang's repeater, trick arrows), tier 3 borrows the Fate
 * Archers (EMIYA, Arash, Atalanta, Robin Hood, Tristan) and tier 4 Chiron, EMIYA Alter, Paris (Apollo),
 * Orion (Artemis), Arjuna, Ishtar and Gilgamesh (Gate of Babylon, Enkidu, Ea).
 */
public final class ArcherContent extends ClassContent {
	public ArcherContent() {
		super(JobClass.ARCHER);
	}

	@Override
	public void define() {
		// ---------------------------------------------------------------- tier 1: Bowman
		weapon("hunter_shortbow", 1, 10, SHORTBOW, "Hunter's Shortbow", "사냥꾼의 단궁",
			art(0x8B5A2B, 0xE8DCC0, 0x58C04A), fx(Fx.Kind.FEATHER, 0x9BE07A),
			skill("double_shot", "Double Shot", "더블 샷", ATTACK, 5, 8, shoot(ProjectileStyle.ARROW, 1.0).count(2).spread(6)),
			skill("hunters_mark", "Hunter's Mark", "사냥꾼의 표식", UTILITY, 14, 10, mark(16, 8, 25), empower(2, 25, 8)),
			skill("nimble_step", "Nimble Step", "날렵한 발걸음", MOVEMENT, 10, 8, buff(SPEED, 6, 1), stance(EVADE, 4, 30)));

		weapon("trapper_crossbow", 1, 15, CROSSBOW, "Trapper's Crossbow", "덫사냥꾼의 석궁",
			art(0x7A7F86, 0x5C3A1E, 0xC0392B), fx(Fx.Kind.SPARK, 0xD9C27A),
			skill("heavy_bolt", "Heavy Bolt", "헤비 볼트", ATTACK, 7, 10, shoot(ProjectileStyle.ARROW, 1.6).pierce(2).speed(3.6F), stun(0.5)),
			skill("snare_trap", "Snare Trap", "올가미 덫", UTILITY, 14, 12, zone(3, 5, 0.4).at(12).effect(SLOWNESS, 2)),
			skill("rope_hook", "Rope Hook", "갈고리 밧줄", MOVEMENT, 12, 8, grapple(16), buff(SLOW_FALLING, 4, 0)));

		weapon("elven_longbow", 1, 20, LONGBOW, "Elven Longbow", "엘프의 장궁",
			art(0xD8C89A, 0x3E6B3A, 0x7FD6A0), fx(Fx.Kind.FEATHER, 0xB8F0C0),
			skill("arrow_volley", "Arrow Volley", "화살 세례", ATTACK, 12, 14, rain(ProjectileStyle.ARROW, 18, 3.5, 8, 0.4)),
			skill("greenleaf_shot", "Greenleaf Shot", "그린리프 샷", ATTACK, 6, 10, shoot(ProjectileStyle.ARROW, 0.9).count(3).spread(10).homing()),
			skill("leaf_on_the_wind", "Leaf on the Wind", "바람 위의 나뭇잎", MOVEMENT, 11, 10, blink(8), empower(2, 30, 6)));

		// ---------------------------------------------------------------- tier 2: Ranger
		weapon("ranger_longbow", 2, 25, LONGBOW, "Ranger's Longbow", "레인저의 장궁",
			art(0x5A3E24, 0x2F4F2F, 0x8FBC8F), fx(Fx.Kind.FEATHER, 0x7CC47A),
			skill("strafe", "Strafe", "스트레이프", ATTACK, 6, 12, shoot(ProjectileStyle.ARROW, 1.0).count(4).interval(3)),
			skill("trackers_eye", "Tracker's Eye", "추적자의 눈", UTILITY, 16, 12, mark(20, 10, 30), buff(NIGHT_VISION, 15, 0)),
			skill("call_of_the_pack", "Call of the Pack", "무리의 부름", UTILITY, 28, 18, summon(Summon.WOLF, 2, 20), allyBuff(8, SPEED, 8, 0)));

		weapon("viper_shortbow", 2, 28, SHORTBOW, "Viper Shortbow", "독사의 단궁",
			art(0x3B5E2B, 0x1E1E1E, 0x9ACD32), fx(Fx.Kind.SMOKE, 0x8BD15A),
			skill("viper_fang", "Viper Fang", "독사의 송곳니", ATTACK, 6, 12, shoot(ProjectileStyle.FEATHER, 1.0).count(3).spread(20), inflict(POISON, 6, 1)),
			skill("toxic_cloud", "Toxic Cloud", "독안개", ATTACK, 14, 16, zone(4, 6, 0.5).at(14).effect(POISON, 0)),
			skill("serpent_slip", "Serpent Slip", "뱀의 몸놀림", MOVEMENT, 10, 12, dash(6, 0), stance(EVADE, 4, 35)));

		weapon("zhuge_repeater", 2, 31, CROSSBOW, "Zhuge Repeating Crossbow", "제갈연노",
			art(0x9C6B30, 0x4A2E1A, 0xB22222), fx(Fx.Kind.SPARK, 0xFFB347),
			skill("bolt_storm", "Bolt Storm", "연노 난사", ATTACK, 8, 16, shoot(ProjectileStyle.ARROW, 0.7).count(6).spread(24).interval(2)),
			skill("caltrops", "Caltrops", "마름쇠", UTILITY, 16, 14, zone(3.5, 6, 0.4).effect(SLOWNESS, 1)),
			skill("tactical_roll", "Tactical Roll", "전술 구르기", MOVEMENT, 9, 12, dash(5, 0), empower(3, 35, 6)));

		weapon("wolf_fang_blades", 2, 34, TWIN_BLADES, "Wolf Fang Blades", "낭아쌍검",
			art(0xC8CCD0, 0x4A3B2A, 0x6FA8DC), fx(Fx.Kind.SLASH, 0xDCE8F0),
			skill("fang_flurry", "Fang Flurry", "낭아 난무", ATTACK, 6, 14, repeat(4, 3, slash(3.5, 120, 0.6)), inflict(BLEEDING, 4, 0)),
			skill("hamstring", "Hamstring", "힘줄 끊기", ATTACK, 9, 12, slash(3.5, 90, 1.5), inflict(SLOWNESS, 4, 2)),
			skill("predators_lunge", "Predator's Lunge", "포식자의 도약", MOVEMENT, 10, 14, shadowstep(10, 1.6), empower(2, 30, 5)));

		weapon("gale_greatbow", 2, 38, GREATBOW, "Gale Greatbow", "질풍의 대궁",
			art(0xB0C4DE, 0x2F4F4F, 0x00CED1), fx(Fx.Kind.FEATHER, 0xAEEEEE),
			skill("gale_arrow", "Gale Arrow", "질풍의 화살", ATTACK, 8, 16, shoot(ProjectileStyle.ARROW, 2.0).pierce(5).speed(4.2F)),
			skill("cyclone_arrow", "Cyclone Arrow", "회오리 화살", ATTACK, 14, 18, shoot(ProjectileStyle.FEATHER, 1.2).explode(2.0F), zone(3.5, 3, 0.6).at(16).pull(1.2)),
			skill("tailwind", "Tailwind", "순풍", MOVEMENT, 14, 12, buff(SPEED, 8, 2), buff(JUMP_BOOST, 8, 1)));

		weapon("trickshot_bow", 2, 42, SHORTBOW, "Trickshot Bow", "트릭샷 활",
			art(0x4B0082, 0x222222, 0xFF8C00), fx(Fx.Kind.SPARK, 0xFFA040),
			skill("explosive_tip", "Explosive Tip", "폭발 화살촉", ATTACK, 9, 16, shoot(ProjectileStyle.ARROW, 1.8).explode(2.5F)),
			skill("net_arrow", "Net Arrow", "그물 화살", UTILITY, 12, 14, shoot(ProjectileStyle.ARROW, 1.0).homing(), inflict(SLOWNESS, 5, 2), stun(0.8)),
			skill("grapple_arrow", "Grapple Arrow", "갈고리 화살", MOVEMENT, 10, 12, grapple(22), stance(EVADE, 4, 40)));

		// ---------------------------------------------------------------- tier 3: Heroic Archer
		weapon("kanshou_bakuya", 3, 45, TWIN_BLADES, "Kanshou & Bakuya", "간장·막야",
			art(0x2A2A2A, 0x8B1A1A, 0xF2F2F2), fx(Fx.Kind.SLASH, 0xF0F0F0),
			skill("crane_wings", "Crane Wings, Three Realms", "학익삼련", ATTACK, 14, 24,
				shoot(ProjectileStyle.BLADE, 1.2).count(2).spread(60).homing(), delay(0.6, dash(7, 2.2))),
			skill("overedge", "Overedge", "오버엣지", ATTACK, 8, 18, repeat(3, 4, slash(4, 170, 1.0)), inflict(BLEEDING, 4, 1)),
			skill("minds_eye_true", "Mind's Eye (True)", "심안(진)", DEFENSE, 20, 18, stance(COUNTER, 6, 45), buff(RESISTANCE, 6, 0)));

		weapon("emiya_black_bow", 3, 49, LONGBOW, "EMIYA's Black Bow", "에미야의 흑궁",
			art(0x1E1E1E, 0x7A0F0F, 0xBFC6CF), fx(Fx.Kind.SHARD, 0xFF4A3A),
			skill("caladbolg_ii", "Caladbolg II", "칼라드볼그 II", ATTACK, 15, 24, shoot(ProjectileStyle.ARROW, 2.6).speed(4.5F).explode(3.5F)),
			skill("rho_aias", "Rho Aias", "로 아이아스", DEFENSE, 26, 24, stance(GUARD, 6, 60), allyBuff(6, RESISTANCE, 6, 1)),
			skill("unlimited_blade_works", "Unlimited Blade Works", "무한의 검제", ATTACK, 24, 28,
				rain(ProjectileStyle.BLADE, 18, 5, 16, 0.35), inflict(BLEEDING, 4, 0)));

		weapon("arash_greatbow", 3, 53, GREATBOW, "Arash's Greatbow", "아라쉬의 대궁",
			art(0xB8864B, 0x4A2C14, 0x6EC6FF), fx(Fx.Kind.BOLT, 0xBDE9FF),
			skill("stella", "Stella", "유성일조", ATTACK, 30, 28, strike(24, 5.5, 1, 6.0), stun(1.0)),
			skill("clairvoyance", "Clairvoyance", "천리안", UTILITY, 18, 18, debuff(18, GLOWING, 10, 0), mark(24, 10, 35)),
			skill("toughness", "Toughness", "강인함", DEFENSE, 22, 18, buff(RESISTANCE, 8, 1), heal(20), cleanse()));

		weapon("tauropolos", 3, 57, SHORTBOW, "Tauropolos", "타우로폴로스",
			art(0x556B2F, 0x2F1E0E, 0xFFD54A), fx(Fx.Kind.FEATHER, 0xFFE27A),
			skill("phoebus_catastrophe", "Phoebus Catastrophe", "포이보스 카타스트로페", ATTACK, 24, 26, rain(ProjectileStyle.ARROW, 22, 6, 20, 0.3)),
			skill("calydonian_hunt", "Calydonian Hunt", "칼리돈의 사냥", ATTACK, 10, 20,
				shoot(ProjectileStyle.ARROW, 1.4).count(3).spread(12).homing(), inflict(VULNERABLE, 6, 0)),
			skill("arcadia_overrun", "Arcadia Overrun", "아르카디아 월주", MOVEMENT, 10, 16, dash(10, 1.4), buff(SPEED, 6, 2)));

		weapon("sherwood_yew", 3, 61, CROSSBOW, "Yew Bow of Sherwood", "셔우드의 주목궁",
			art(0x3E5C2A, 0x2B1D10, 0x9CFF57), fx(Fx.Kind.SMOKE, 0x7BCB4A),
			skill("yew_bow", "Yew Bow", "기도의 활", ATTACK, 14, 24,
				shoot(ProjectileStyle.ARROW, 2.0).homing(), inflict(POISON, 8, 2), inflict(WEAKNESS, 6, 0)),
			skill("no_face_may_king", "No Face May King", "얼굴 없는 왕", UTILITY, 22, 20, stealth(6, 80), buff(SPEED, 6, 1)),
			skill("sabotage", "Sabotage", "파괴 공작", ATTACK, 16, 20, zone(4.5, 6, 0.6).at(16).effect(WEAKNESS, 1)));

		weapon("failnaught", 3, 66, LONGBOW, "Failnaught", "페일노트",
			art(0xD4AF37, 0x7A1F2B, 0xFFB6C1), fx(Fx.Kind.SPARK, 0xFF8FB1),
			skill("lamenting_phantom_song", "Lamenting Phantom Song", "통곡의 환주", ATTACK, 16, 26, chain(18, 6, 1.6), inflict(SLOWNESS, 3, 1)),
			skill("sonic_string", "Sonic String", "음속의 현", ATTACK, 7, 18, repeat(3, 4, beam(20, 1.0))),
			skill("harp_of_healing", "Harp of Healing", "치유의 하프", UTILITY, 24, 22, allyHeal(8, 20), allyBuff(8, REGENERATION, 6, 0), cleanse()));

		// ---------------------------------------------------------------- tier 4: King of Heroes
		weapon("chiron_greatbow", 4, 70, GREATBOW, "Chiron's Greatbow", "케이론의 대궁",
			art(0x6D4C41, 0x263238, 0xFF4500), fx(Fx.Kind.BOLT, 0xFF7043),
			skill("antares_snipe", "Antares Snipe", "천갈일사", ULTIMATE, 45, 36, mark(24, 6, 40), shoot(ProjectileStyle.ARROW, 3.6).speed(5.0F).pierce(6)),
			skill("pankration", "Pankration", "판크라티온", ATTACK, 10, 20, slash(4, 140, 1.8), stun(1.0), push(4, 1.6)),
			skill("arrow_protection", "Protection from Arrows", "화살 회피", DEFENSE, 22, 20, stance(EVADE, 6, 55), buff(RESISTANCE, 6, 0)),
			skill("centaur_gallop", "Centaur's Gallop", "켄타우로스의 질주", MOVEMENT, 10, 16, buff(SPEED, 6, 2), dash(12, 1.4)));

		weapon("kanshou_bakuya_alter", 4, 73, TWIN_BLADES, "Kanshou & Bakuya (Alter)", "간장·막야 (얼터)",
			art(0x3A3A42, 0x151515, 0xE03030), fx(Fx.Kind.SPARK, 0xFF5A5A),
			skill("unlimited_lost_works", "Unlimited Lost Works", "언리미티드 로스트 웍스", ULTIMATE, 48, 38,
				shoot(ProjectileStyle.BULLET, 0.9).count(8).spread(40).interval(2), delay(0.8, rain(ProjectileStyle.BLADE, 14, 5, 16, 0.45))),
			skill("gun_kata", "Gun Kata", "건 카타", ATTACK, 8, 22, slash(4, 360, 1.4), shoot(ProjectileStyle.BULLET, 1.2).count(5).spread(60)),
			skill("counter_guardian", "Counter Guardian", "카운터 가디언", DEFENSE, 22, 22, stance(COUNTER, 6, 50), shield(12, 8)),
			skill("afterimage_shot", "Afterimage Shot", "잔상 사격", MOVEMENT, 10, 16, blink(10), delay(0.2, shoot(ProjectileStyle.BULLET, 1.5).count(3).spread(20))));

		weapon("apollo_bow", 4, 76, LONGBOW, "Bow of Apollo", "아폴론의 활",
			art(0xFFD24A, 0xFFF3D6, 0xFF7A00), fx(Fx.Kind.SPARK, 0xFFC93C),
			skill("hekatebolos", "Hekatebolos", "헤카테볼로스", ULTIMATE, 45, 38, beam(26, 3.8), burn(6), inflict(BLINDNESS, 3, 0)),
			skill("achilles_heel", "Achilles' Heel", "아킬레스건", ATTACK, 14, 24, execute(24, 2.0, 40, 2.0)),
			skill("paean", "Paean", "파이안", UTILITY, 26, 24, allyHeal(10, 25), allyBuff(10, REGENERATION, 8, 1), cleanse()),
			skill("sunlit_step", "Sunlit Step", "햇살 걸음", MOVEMENT, 9, 16, blink(12), buff(SPEED, 6, 1)));

		weapon("artemis_bow", 4, 79, SHORTBOW, "Bow of Artemis", "아르테미스의 활",
			art(0xE6ECFF, 0xB8C2DC, 0x8FB2FF), fx(Fx.Kind.FEATHER, 0xC8D8FF),
			skill("tri_star_amore_mio", "Tri-Star Amore Mio", "트라이스타 아모레 미오", ULTIMATE, 50, 38,
				repeat(3, 8, shoot(ProjectileStyle.ARROW, 1.4).count(3).spread(15).homing()), inflict(VULNERABLE, 6, 1)),
			skill("crescent_volley", "Crescent Volley", "초승달 연사", ATTACK, 8, 22, shoot(ProjectileStyle.ARROW, 1.5).count(5).spread(60).pierce(2)),
			skill("hounds_of_the_moon", "Hounds of the Moon", "달의 사냥개", UTILITY, 28, 24, summon(Summon.WOLF, 3, 18), debuff(12, SLOWNESS, 5, 1)),
			skill("moonlight_veil", "Moonlight Veil", "월광의 장막", DEFENSE, 22, 20, stealth(4, 60), stance(EVADE, 6, 45)));

		weapon("gandiva", 4, 83, GREATBOW, "Gandiva", "간디바",
			art(0xF2F2F2, 0x1E2A44, 0x4AA8FF), fx(Fx.Kind.BOLT, 0x7FB8FF),
			skill("pashupata", "Pashupata", "파슈파타", ULTIMATE, 55, 40, lightning(24, 6, 3.8), inflict(WITHER, 6, 1)),
			skill("agni_gandiva", "Agni Gandiva", "아그니 간디바", ATTACK, 10, 24, shoot(ProjectileStyle.ARROW, 1.6).count(3).spread(12).explode(2.0F), burn(5)),
			skill("hero_of_the_endowed", "Hero of the Endowed", "수여의 영웅", UTILITY, 26, 20, restoreMana(25), buff(MANA_FLOW, 12, 0), refresh(30)),
			skill("flame_burst", "Mana Burst (Flame)", "마력 방출(불꽃)", MOVEMENT, 10, 18, dash(10, 1.8), burn(3)));

		weapon("maanna", 4, 87, LONGBOW, "Maanna", "마안나",
			art(0xE8D27A, 0x1A1A2E, 0xD7263D), fx(Fx.Kind.COIN, 0xFFD700),
			skill("an_gal_ta_kigal_she", "An Gal Ta Kigal Shè", "산맥진동 명봉 쿠르", ULTIMATE, 50, 40, stun(1.5), strike(24, 6, 1, 4.2)),
			skill("gem_shot", "Gem Shot", "보석 사격", ATTACK, 8, 22, shoot(ProjectileStyle.ORB, 1.2).count(5).spread(40).homing(), loot(15)),
			skill("manifestation_of_beauty", "Manifestation of Beauty", "미의 화신", UTILITY, 24, 24, allyBuff(10, STRENGTH, 10, 1), debuff(10, WEAKNESS, 6, 1)),
			skill("goddess_flight", "Goddess's Flight", "여신의 비행", MOVEMENT, 12, 18, blink(12), buff(SLOW_FALLING, 8, 0), buff(SPEED, 8, 1)));

		weapon("gate_of_babylon", 4, 92, CROSSBOW, "Gate of Babylon", "왕의 재보",
			art(0xFFD700, 0x5A0A0A, 0x40E0D0), fx(Fx.Kind.RUNE, 0xFFE066),
			skill("full_open", "Gate of Babylon: Full Open", "왕의 재보: 전면 개방", ULTIMATE, 50, 38,
				barrage(ProjectileStyle.BLADE, 24, 0.4), delay(1.5, rain(ProjectileStyle.BLADE, 20, 6, 16, 0.15))),
			skill("treasure_volley", "Treasure Volley", "보구 사출", ATTACK, 8, 22, barrage(ProjectileStyle.BLADE, 8, 0.6)),
			skill("enkidu", "Enkidu: Chains of Heaven", "천의 사슬 (엔키두)", UTILITY, 18, 26, pull(10, 1.8), debuff(10, STUN, 1.5, 0), debuff(10, VULNERABLE, 6, 1)),
			skill("treasury_shields", "Shields of the Treasury", "보물고의 방패", DEFENSE, 26, 24, shield(20, 10), stance(GUARD, 5, 50)));

		weapon("ea_and_merodach", 4, 100, TWIN_BLADES, "Ea & Merodach", "에아 & 메로닥",
			art(0x8B1010, 0xFFD700, 0xFF3030), fx(Fx.Kind.BOLT, 0xFF3030),
			skill("enuma_elish", "Enuma Elish", "에누마 엘리시", ULTIMATE, 60, 40, pull(10, 1.4), delay(0.6, beam(32, 8.0)), inflict(VULNERABLE, 8, 2)),
			skill("original_sin", "Merodach: Original Sin", "원죄 (메로닥)", ATTACK, 9, 26, slash(5, 200, 2.8), shoot(ProjectileStyle.WAVE, 2.6).pierce(6)),
			skill("sha_naqba_imuru", "Sha Naqba Imuru", "모든 것을 본 자", UTILITY, 24, 24, debuff(16, GLOWING, 12, 0), mark(24, 12, 45), restoreMana(25)),
			skill("golden_rule_body", "Golden Rule (Body)", "황금률(체)", DEFENSE, 28, 26, buff(RESISTANCE, 8, 1), buff(REGENERATION, 8, 1), heal(20)));
	}
}
