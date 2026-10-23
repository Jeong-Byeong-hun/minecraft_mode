package com.minecraftmode.job.content;

import static com.minecraftmode.job.skill.Actions.*;
import static com.minecraftmode.job.skill.Boost.*;
import static com.minecraftmode.job.skill.SkillKind.*;
import static com.minecraftmode.job.weapon.Archetype.*;
import static com.minecraftmode.registry.ModEffects.BLEEDING;
import static com.minecraftmode.registry.ModEffects.MANA_FLOW;
import static com.minecraftmode.registry.ModEffects.STUN;
import static com.minecraftmode.registry.ModEffects.VULNERABLE;
import static net.minecraft.world.effect.MobEffects.*;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.skill.Fx;
import com.minecraftmode.job.skill.SkillLook;
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
			skill("double_shot", "Double Shot", "더블 샷", ATTACK, 5, 8, shoot(ProjectileStyle.ARROW, 1.6).count(2).spread(6)),
			skill("hunters_mark", "Hunter's Mark", "사냥꾼의 표식", UTILITY, 14, 10, mark(16, 8, 25), empower(2, 25, 8)),
			skill("nimble_step", "Nimble Step", "날렵한 발걸음", MOVEMENT, 10, 8, buff(SPEED, 6, 1), boost(CRIT_CHANCE, 4, 15)));

		weapon("trapper_crossbow", 1, 15, CROSSBOW, "Trapper's Crossbow", "덫사냥꾼의 쇠뇌",
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
			skill("strafe", "Strafe", "스트레이프", ATTACK, 6, 12, shoot(ProjectileStyle.ARROW, 0.5).count(4).interval(3)),
			skill("trackers_eye", "Tracker's Eye", "추적자의 눈", UTILITY, 16, 12, mark(20, 10, 30), buff(NIGHT_VISION, 15, 0)),
			skill("call_of_the_pack", "Call of the Pack", "무리의 부름", UTILITY, 28, 18, summon(Summon.WOLF, 2, 20), allyBuff(8, SPEED, 8, 0)));

		weapon("viper_shortbow", 2, 28, SHORTBOW, "Viper Shortbow", "독사의 단궁",
			art(0x3B5E2B, 0x1E1E1E, 0x9ACD32), fx(Fx.Kind.SMOKE, 0x8BD15A),
			skill("viper_fang", "Viper Fang", "독사의 송곳니", ATTACK, 6, 12, shoot(ProjectileStyle.FEATHER, 1.0).count(3).spread(20), inflict(POISON, 6, 1)),
			skill("toxic_cloud", "Toxic Cloud", "독안개", ATTACK, 14, 16, zone(4, 6, 0.5).at(14).effect(POISON, 0)),
			skill("serpent_slip", "Serpent Slip", "뱀의 몸놀림", MOVEMENT, 10, 12, dash(6, 0), boost(CRIT_CHANCE, 4, 20)));

		weapon("zhuge_repeater", 2, 31, CROSSBOW, "Zhuge Repeating Crossbow", "제갈연노",
			art(0x9C6B30, 0x4A2E1A, 0xB22222), fx(Fx.Kind.SPARK, 0xFFB347),
			skill("bolt_storm", "Bolt Storm", "연노 난사", ATTACK, 8, 16, shoot(ProjectileStyle.ARROW, 1.4).count(6).spread(24).interval(2)),
			skill("caltrops", "Caltrops", "마름쇠", UTILITY, 16, 14, zone(3.5, 6, 0.4).effect(SLOWNESS, 1)),
			skill("tactical_roll", "Tactical Roll", "전술 구르기", MOVEMENT, 9, 12, dash(5, 0), empower(3, 35, 6)));

		weapon("wolf_fang_blades", 2, 34, TWIN_BLADES, "Wolf Fang Blades", "낭아쌍검",
			art(0xC8CCD0, 0x4A3B2A, 0x6FA8DC), fx(Fx.Kind.SLASH, 0xDCE8F0),
			skill("fang_flurry", "Fang Flurry", "낭아 난무", ATTACK, 6, 14, repeat(4, 3, slash(3.5, 120, 0.42)), inflict(BLEEDING, 4, 0)),
			skill("hamstring", "Hamstring", "힘줄 끊기", ATTACK, 9, 12, slash(3.5, 90, 1.5), inflict(SLOWNESS, 4, 2)),
			skill("predators_lunge", "Predator's Lunge", "포식자의 도약", MOVEMENT, 10, 14, shadowstep(10, 1.36), empower(2, 30, 5)));

		weapon("gale_greatbow", 2, 38, GREATBOW, "Gale Greatbow", "질풍의 대궁",
			art(0xB0C4DE, 0x2F4F4F, 0x00CED1), fx(Fx.Kind.FEATHER, 0xAEEEEE),
			skill("gale_arrow", "Gale Arrow", "질풍의 화살", ATTACK, 8, 16, shoot(ProjectileStyle.ARROW, 2.0).pierce(5).speed(4.2F)),
			skill("cyclone_arrow", "Cyclone Arrow", "회오리 화살", ATTACK, 14, 18, shoot(ProjectileStyle.FEATHER, 1.2).explode(2.0F), zone(3.5, 3, 0.6).at(16).pull(1.2)),
			skill("tailwind", "Tailwind", "순풍", MOVEMENT, 14, 12, buff(SPEED, 8, 2), buff(JUMP_BOOST, 8, 1)));

		weapon("trickshot_bow", 2, 42, SHORTBOW, "Trickshot Bow", "트릭샷 활",
			art(0x4B0082, 0x222222, 0xFF8C00), fx(Fx.Kind.SPARK, 0xFFA040),
			skill("explosive_tip", "Explosive Tip", "폭발 화살촉", ATTACK, 9, 16, shoot(ProjectileStyle.ARROW, 1.8).explode(2.5F)),
			skill("net_arrow", "Net Arrow", "그물 화살", UTILITY, 12, 14, shoot(ProjectileStyle.ARROW, 1.0).homing(), inflict(SLOWNESS, 5, 2), stun(0.8)),
			skill("grapple_arrow", "Grapple Arrow", "갈고리 화살", MOVEMENT, 10, 12, grapple(22), boost(CRIT_CHANCE, 4, 20)));

		// 0.5.24: the chakram
		weapon("windwheel_chakram", 2, 44, CHAKRAM, "Windwheel Chakram", "풍륜 차크람",
			art(0xC0C8D0, 0x3E6B4A, 0x7FFFB0), fx(Fx.Kind.SLASH, 0xBFFFE0),
			skill("returning_disc", "Returning Disc", "돌아오는 원반", ATTACK, 8, 14, shoot(ProjectileStyle.BLADE, 1.5).pierce(5)),
			skill("twin_discs", "Twin Discs", "쌍원반", ATTACK, 10, 16, repeat(2, 6, shoot(ProjectileStyle.BLADE, 0.95))),
			skill("disc_vault", "Disc Vault", "원반 도약", MOVEMENT, 12, 10, leap(4, 3, 1.0)));

		// ---------------------------------------------------------------- tier 3: Heroic Archer
		weapon("kanshou_bakuya", 3, 45, TWIN_BLADES, "Kanshou & Bakuya", "간장·막야",
			art(0x2A2A2A, 0x8B1A1A, 0xF2F2F2), fx(Fx.Kind.SLASH, 0xF0F0F0),
			skill("crane_wings", "Crane Wings, Three Realms", "학익삼련", ATTACK, 14, 24,
				shoot(ProjectileStyle.BLADE, 1.2).count(2).spread(60).homing(), delay(0.6, dash(7, 2.2))),
			skill("overedge", "Overedge", "오버엣지", ATTACK, 8, 18, repeat(3, 4, slash(4, 170, 0.85)), inflict(BLEEDING, 4, 1)),
			skill("minds_eye_true", "Mind's Eye (True)", "심안(진짜)", UTILITY, 20, 18, boost(CRIT_DAMAGE, 6, 50), boost(DAMAGE, 6, 10)));

		weapon("emiya_black_bow", 3, 49, LONGBOW, "EMIYA's Black Bow", "에미야의 흑궁",
			art(0x1E1E1E, 0x7A0F0F, 0xBFC6CF), fx(Fx.Kind.SHARD, 0xFF4A3A),
			skill("caladbolg_ii", "Caladbolg II", "칼라드볼그 II", ATTACK, 15, 24, shoot(ProjectileStyle.ARROW, 2.6).speed(4.5F).explode(3.5F)),
			skill("rho_aias", "Rho Aias", "로 아이아스", UTILITY, 26, 24, boost(CRIT_CHANCE, 6, 30), allyBoost(6, DAMAGE, 6, 20)),
			skill("unlimited_blade_works", "Unlimited Blade Works", "무한의 검제", ATTACK, 24, 28,
				rain(ProjectileStyle.BLADE, 18, 5, 16, 0.35), inflict(BLEEDING, 4, 0)));

		weapon("arash_greatbow", 3, 53, GREATBOW, "Arash's Greatbow", "아라쉬의 대궁",
			art(0xB8864B, 0x4A2C14, 0x6EC6FF), fx(Fx.Kind.BOLT, 0xBDE9FF),
			skill("stella", "Stella", "유성일조", ATTACK, 30, 28, strike(24, 5.5, 1, 8.4), stun(1.0)),
			skill("clairvoyance", "Clairvoyance", "천리안", UTILITY, 18, 18, debuff(18, GLOWING, 10, 0), mark(24, 10, 35)),
			skill("toughness", "Toughness", "강건", UTILITY, 22, 18, boost(DAMAGE, 8, 20), boost(CRIT_CHANCE, 8, 15), cleanse()));

		weapon("tauropolos", 3, 57, SHORTBOW, "Tauropolos", "타우로폴로스",
			art(0x556B2F, 0x2F1E0E, 0xFFD54A), fx(Fx.Kind.FEATHER, 0xFFE27A),
			skill("phoebus_catastrophe", "Phoebus Catastrophe", "청원의 화살 편지", ATTACK, 24, 26, rain(ProjectileStyle.ARROW, 22, 6, 20, 0.3)),
			skill("calydonian_hunt", "Calydonian Hunt", "칼리돈 사냥", ATTACK, 10, 20,
				shoot(ProjectileStyle.ARROW, 1.4).count(3).spread(12).homing(), inflict(VULNERABLE, 6, 0)),
			skill("arcadia_overrun", "Arcadia Overrun", "아르카디아 너머", MOVEMENT, 10, 16, dash(10, 1.4), buff(SPEED, 6, 2)));

		weapon("sherwood_yew", 3, 61, CROSSBOW, "Yew Bow of Sherwood", "셔우드의 주목궁",
			art(0x3E5C2A, 0x2B1D10, 0x9CFF57), fx(Fx.Kind.SMOKE, 0x7BCB4A),
			skill("yew_bow", "Yew Bow", "기원의 활", ATTACK, 14, 24,
				shoot(ProjectileStyle.ARROW, 2.0).homing(), inflict(POISON, 8, 2), inflict(WEAKNESS, 6, 0)),
			skill("no_face_may_king", "No Face May King", "얼굴 없는 왕", UTILITY, 22, 20, stealth(6, 80), buff(SPEED, 6, 1)),
			skill("sabotage", "Sabotage", "파괴공작", ATTACK, 16, 20, zone(4.5, 6, 0.6).at(16).effect(WEAKNESS, 1)));

		weapon("failnaught", 3, 66, LONGBOW, "Failnaught", "페일노트",
			art(0xD4AF37, 0x7A1F2B, 0xFFB6C1), fx(Fx.Kind.SPARK, 0xFF8FB1),
			skill("lamenting_phantom_song", "Lamenting Phantom Song", "통곡의 환주", ATTACK, 16, 26, chain(18, 6, 1.6), inflict(SLOWNESS, 3, 1)),
			skill("sonic_string", "Sonic String", "음속의 현", ATTACK, 7, 18, repeat(3, 4, beam(20, 0.8))),
			skill("harp_of_healing", "Harp of Courage", "용기의 하프", UTILITY, 24, 22, allyBoost(8, DAMAGE, 8, 15), allyBoost(8, CRIT_CHANCE, 8, 10), cleanse()));

		weapon("sudarshana", 3, 59, CHAKRAM, "Sudarshana", "수다르샤나",
			art(0xFFD24A, 0x8B5A2B, 0xFF6A00), fx(Fx.Kind.ORB, 0xFFB347),
			skill("sudarshana_spin", "Sudarshana Spin", "수다르샤나 회전", ATTACK, 9, 20, shoot(ProjectileStyle.BLADE, 2.0).pierce(8)),
			skill("blazing_orbit", "Blazing Orbit", "불타는 궤도", ATTACK, 12, 22, burn(4), repeat(3, 8, nova(4, 0.9))),
			skill("hunting_focus", "Hunting Focus", "사냥 집중", UTILITY, 20, 14, boost(CRIT_DAMAGE, 8, 40)));

		// ---------------------------------------------------------------- tier 4: King of Heroes
		// Every tier 4 skill draws its own look (SkillLook, 0.5.38): visual only, the numbers are the wrapped actions'.
		// Where the weapon's big sprites (glyph bolts) would bury the drawing, the skill sheds small sparks instead (withFx SPARK).
		weapon("chiron_greatbow", 4, 70, GREATBOW, "Chiron's Greatbow", "케이론의 대궁",
			art(0x6D4C41, 0x263238, 0xFF4500), fx(Fx.Kind.BOLT, 0xFF7043),
			skill("antares_snipe", "Antares Snipe", "천갈일사", ULTIMATE, 45, 36, look(SkillLook.ANTARES_MARK, mark(24, 6, 40)).onTarget(24),
				shoot(ProjectileStyle.ARROW, 6.48).speed(5.0F).pierce(6).look(SkillLook.STAR_ARROW)).withFx(Fx.Kind.SPARK),
			skill("pankration", "Pankration", "판크라티온", ATTACK, 10, 20, look(SkillLook.PANKRATION, slash(4, 140, 1.8)).cone(4, 140), stun(1.0), push(4, 1.6)).withFx(Fx.Kind.SPARK),
			skill("arrow_protection", "Protection from Arrows", "화살 회피의 가호", UTILITY, 22, 20, look(SkillLook.ARROW_WARD, boost(CRIT_CHANCE, 6, 30)),
				boost(DAMAGE, 6, 10)),
			skill("centaur_gallop", "Centaur's Gallop", "켄타우로스의 질주", MOVEMENT, 10, 16, buff(SPEED, 6, 2), look(SkillLook.GALLOP, dash(12, 1.4)).lasting(1.2)).withFx(Fx.Kind.SPARK));

		weapon("kanshou_bakuya_alter", 4, 73, TWIN_BLADES, "Kanshou & Bakuya (Alter)", "간장·막야 (얼터)",
			art(0x3A3A42, 0x151515, 0xE03030), fx(Fx.Kind.SPARK, 0xFF5A5A),
			skill("unlimited_lost_works", "Unlimited Lost Works", "언리미티드 로스트 워크스", ULTIMATE, 48, 38,
				shoot(ProjectileStyle.BULLET, 0.9).count(8).spread(40).interval(2).look(SkillLook.TRACER),
				delay(0.8, look(SkillLook.ULW_FIELD, rain(ProjectileStyle.BLADE, 14, 5, 16, 0.45)).at(14).size(5).lasting(3))),
			skill("gun_kata", "Gun Kata", "건 카타", ATTACK, 8, 22, look(SkillLook.GUN_KATA, slash(4, 360, 1.19)).size(4),
				shoot(ProjectileStyle.BULLET, 1.02).count(5).spread(60).look(SkillLook.TRACER)),
			skill("counter_guardian", "Counter Guardian", "카운터 가디언", UTILITY, 22, 22, look(SkillLook.COUNTER_GUARDIAN, boost(CRIT_DAMAGE, 6, 50)), boost(DAMAGE, 6, 15)),
			skill("afterimage_shot", "Afterimage Shot", "잔상 사격", MOVEMENT, 10, 16, look(SkillLook.FLASH_TRAIL, blink(10)),
				delay(0.2, shoot(ProjectileStyle.BULLET, 1.5).count(3).spread(20).look(SkillLook.TRACER))));

		weapon("apollo_bow", 4, 76, LONGBOW, "Bow of Apollo", "아폴론의 활",
			art(0xFFD24A, 0xFFF3D6, 0xFF7A00), fx(Fx.Kind.SPARK, 0xFFC93C),
			skill("hekatebolos", "Hekatebolos", "헤카테볼로스", ULTIMATE, 45, 38, look(SkillLook.SUN_BEAM, beam(26, 7.6)).beam(26), burn(6), inflict(BLINDNESS, 3, 0)),
			skill("achilles_heel", "Achilles' Heel", "아킬레스건", ATTACK, 14, 24, look(SkillLook.HEEL_STRIKE, execute(24, 2.6, 40, 2.0)).onTarget(24)),
			skill("paean", "Paean", "파이안", UTILITY, 26, 24, look(SkillLook.PAEAN, allyBoost(10, DAMAGE, 10, 20)).size(10).unscaled(), allyBoost(10, CRIT_DAMAGE, 10, 20), cleanse()),
			skill("sunlit_step", "Sunlit Step", "햇살 걸음", MOVEMENT, 9, 16, look(SkillLook.SUN_STEP, blink(12)), buff(SPEED, 6, 1)));

		weapon("artemis_bow", 4, 79, SHORTBOW, "Bow of Artemis", "아르테미스의 활",
			art(0xE6ECFF, 0xB8C2DC, 0x8FB2FF), fx(Fx.Kind.FEATHER, 0xC8D8FF),
			skill("tri_star_amore_mio", "Tri-Star Amore Mio", "트라이스타 아모레 미오", ULTIMATE, 50, 38,
				repeat(3, 8, shoot(ProjectileStyle.ARROW, 2.1).count(3).spread(15).homing().look(SkillLook.MOON_ARROW)), inflict(VULNERABLE, 6, 1)),
			skill("crescent_volley", "Crescent Volley", "초승달 연사", ATTACK, 8, 22,
				shoot(ProjectileStyle.ARROW, 1.5).count(5).spread(60).pierce(2).look(SkillLook.CRESCENT_ARROW)),
			skill("hounds_of_the_moon", "Hounds of the Moon", "달의 사냥개", UTILITY, 28, 24, look(SkillLook.MOON_SIGIL, summon(Summon.WOLF, 3, 18)).size(12),
				debuff(12, SLOWNESS, 5, 1)),
			skill("moonlight_veil", "Moonlight Veil", "월광의 장막", UTILITY, 22, 20, look(SkillLook.MOON_VEIL, stealth(4, 60)).lasting(4), boost(CRIT_CHANCE, 6, 25)));

		weapon("gandiva", 4, 83, GREATBOW, "Gandiva", "간디바",
			art(0xF2F2F2, 0x1E2A44, 0x4AA8FF), fx(Fx.Kind.BOLT, 0x7FB8FF),
			skill("pashupata", "Pashupata", "파괴신의 손짓", ULTIMATE, 55, 40, look(SkillLook.PASHUPATA_BOLT, lightning(24, 6, 7.6)).at(24).size(6), inflict(WITHER, 6, 1)),
			skill("agni_gandiva", "Agni Gandiva", "아그니 간디바", ATTACK, 10, 24,
				shoot(ProjectileStyle.ARROW, 1.28).count(3).spread(12).explode(2.0F).look(SkillLook.FLAME_ARROW)
					.onImpact(lookWhereItBursts(SkillLook.FLAME_BURST, 2.0)), burn(5)),
			skill("hero_of_the_endowed", "Hero of the Endowed", "축복받는 영웅", UTILITY, 26, 20, look(SkillLook.ENDOWED, restoreMana(25)), buff(MANA_FLOW, 12, 0), refresh(30)),
			skill("flame_burst", "Mana Burst (Flame)", "마력방출(불꽃)", MOVEMENT, 10, 18, look(SkillLook.MANA_FLAME, dash(10, 1.8)).lasting(1.2), burn(3)));

		weapon("maanna", 4, 87, LONGBOW, "Maanna", "마안나",
			art(0xE8D27A, 0x1A1A2E, 0xD7263D), fx(Fx.Kind.COIN, 0xFFD700),
			skill("an_gal_ta_kigal_she", "An Gal Ta Kigal Shè", "앙갈타 키갈슈", ULTIMATE, 50, 40, stun(1.5),
				look(SkillLook.VENUS_STAR, strike(24, 6, 1, 7.98)).at(24).size(6)),
			skill("gem_shot", "Gem Shot", "보석 사격", ATTACK, 8, 22, shoot(ProjectileStyle.ORB, 1.66).count(5).spread(40).homing().look(SkillLook.GEM), loot(15)),
			skill("manifestation_of_beauty", "Manifestation of Beauty", "미의 현현", UTILITY, 24, 24, look(SkillLook.BEAUTY, allyBoost(10, DAMAGE, 10, 20)).size(10),
				debuff(10, WEAKNESS, 6, 1)),
			skill("goddess_flight", "Goddess's Flight", "여신의 비행", MOVEMENT, 12, 18, look(SkillLook.VENUS_WINGS, blink(12)).lasting(4), buff(SLOW_FALLING, 8, 0),
				buff(SPEED, 8, 1)));

		weapon("gate_of_babylon", 4, 92, CROSSBOW, "Gate of Babylon", "왕의 재보",
			art(0xFFD700, 0x5A0A0A, 0x40E0D0), fx(Fx.Kind.RUNE, 0xFFE066),
			skill("full_open", "Gate of Babylon: Full Open", "왕의 재보: 전면 개방", ULTIMATE, 50, 38,
				barrage(ProjectileStyle.BLADE, 24, 0.28, SkillLook.BABYLON_GATE, SkillLook.TREASURE),
				delay(1.5, look(SkillLook.BABYLON_SKY, rain(ProjectileStyle.BLADE, 20, 6, 16, 0.11, 0.0F, SkillLook.BABYLON_GATE, SkillLook.TREASURE)).at(20).size(6)
					.lasting(2.2))),
			skill("treasure_volley", "Treasure Volley", "보구 사출", ATTACK, 8, 22, barrage(ProjectileStyle.BLADE, 8, 0.3, SkillLook.BABYLON_GATE, SkillLook.TREASURE)),
			skill("enkidu", "Enkidu: Chains of Heaven", "하늘의 사슬", UTILITY, 18, 26, look(SkillLook.ENKIDU, pull(10, 1.8)).size(10), debuff(10, STUN, 1.5, 0),
				debuff(10, VULNERABLE, 6, 1)),
			skill("treasury_shields", "Arms of the Treasury", "보물고의 무구", UTILITY, 26, 24, look(SkillLook.TREASURY_SHIELDS, boost(CRIT_CHANCE, 8, 20)).lasting(4),
				boost(DAMAGE, 8, 25)));

		weapon("ea_and_merodach", 4, 100, TWIN_BLADES, "Ea & Merodach", "에아 & 메로닥",
			art(0x8B1010, 0xFFD700, 0xFF3030), fx(Fx.Kind.BOLT, 0xFF3030),
			skill("enuma_elish", "Enuma Elish", "에누마 엘리시", ULTIMATE, 60, 40, look(SkillLook.EA_CHARGE, pull(10, 1.4)).size(10),
				delay(0.6, look(SkillLook.ENUMA_ELISH, beam(32, 10.0)).beam(32)), inflict(VULNERABLE, 8, 2)),
			skill("original_sin", "Merodach: Original Sin", "원죄 (메로닥)", ATTACK, 9, 26, look(SkillLook.SLASH, slash(5, 200, 1.4)).cone(5, 200),
				shoot(ProjectileStyle.WAVE, 1.3).pierce(6).look(SkillLook.SIN_WAVE)).withFx(Fx.Kind.SPARK),
			skill("sha_naqba_imuru", "Sha Naqba Imuru", "모든 것을 본 자", UTILITY, 24, 24, look(SkillLook.SHA_NAQBA, debuff(16, GLOWING, 12, 0)).size(16),
				mark(24, 12, 45), restoreMana(25)),
			skill("golden_rule_body", "Golden Rule (Body)", "황금률(몸)", UTILITY, 28, 26, look(SkillLook.GOLDEN_RULE, boost(DAMAGE, 8, 20)), boost(CRIT_DAMAGE, 8, 30)));

		weapon("pashupata", 4, 97, CHAKRAM, "Pashupata", "파슈파타",
			art(0xE8F0FF, 0x1F2A5C, 0x4AD7FF), fx(Fx.Kind.BOLT, 0x9FE8FF),
			skill("pashupata", "Pashupata", "파슈파타", ULTIMATE, 50, 40, mark(20, 8, 30), look(SkillLook.PASHUPATA_DISC, strike(20, 5, 1, 6.5)).at(20).size(5)),
			skill("divine_discus", "Divine Discus", "신성한 원반", ATTACK, 7, 18, shoot(ProjectileStyle.BLADE, 1.7).count(3).spread(30).pierce(3).look(SkillLook.DISCUS)),
			skill("mana_burst_flame", "Mana Burst (Flame)", "마력방출(불꽃)", ATTACK, 10, 20, burn(5), look(SkillLook.FLAME_BURST, nova(5, 2.2)).size(5)),
			skill("hero_of_the_endowed", "Hero of the Endowed", "축복받는 영웅", UTILITY, 26, 20, look(SkillLook.ENDOWED, boost(CRIT_CHANCE, 8, 15)), boost(DAMAGE, 8, 15)));
	}
}
