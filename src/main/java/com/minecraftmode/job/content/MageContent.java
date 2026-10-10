package com.minecraftmode.job.content;

import static com.minecraftmode.job.skill.Actions.*;
import static com.minecraftmode.job.skill.CombatState.Stance.*;
import static com.minecraftmode.job.skill.SkillKind.*;
import static com.minecraftmode.job.weapon.Archetype.*;
import static com.minecraftmode.registry.ModEffects.MANA_FLOW;
import static com.minecraftmode.registry.ModEffects.STUN;
import static com.minecraftmode.registry.ModEffects.VULNERABLE;
import static net.minecraft.world.effect.MobEffects.*;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.skill.Fx;
import com.minecraftmode.job.weapon.ProjectileStyle;

/**
 * Mage: Mage -> Caster -> Archmage -> Grand Caster. Staves, wands, orbs, grimoires and scepters across the
 * elemental schools; borrows from MapleStory (Magician, Fire-Poison, Ice-Lightning, Bishop), Harry Potter,
 * Frieren (Fern, Zoltraak), Fairy Tail (Celestial keys, Mystogan, Jellal, Fairy Law), Konosuba (Megumin's EXPLOSION)
 * and the Fate Casters (Nursery Rhyme, Gilles de Rais, Medea, Tamamo-no-Mae, Zhuge Liang, Merlin, Solomon).
 */
public final class MageContent extends ClassContent {
	public MageContent() {
		super(JobClass.MAGE);
	}

	@Override
	public void define() {
		// ---------------------------------------------------------------- tier 1: Mage
		weapon("apprentice_staff", 1, 10, STAFF, "Apprentice's Staff", "견습 마법사의 지팡이",
			art(0x9AA3AD, 0x7A5634, 0x5DA9FF), fx(Fx.Kind.ORB, 0x8FC4FF),
			skill("magic_claw", "Magic Claw", "매직 클로", ATTACK, 5, 8, shoot(ProjectileStyle.ORB, 0.9).count(2).interval(3)),
			skill("magic_guard", "Magic Guard", "매직 가드", DEFENSE, 16, 10, stance(GUARD, 6, 30)),
			skill("teleport", "Teleport", "텔레포트", MOVEMENT, 8, 8, blink(7)));

		weapon("holly_wand", 1, 15, WAND, "Holly Wand", "호랑가시나무 지팡이",
			art(0xC8A060, 0x7B4A2D, 0xFF8A3D), fx(Fx.Kind.BOLT, 0xFF5050),
			skill("stupefy", "Stupefy", "스투페파이", ATTACK, 7, 10, shoot(ProjectileStyle.ORB, 1.3).speed(2.4F), stun(1.0)),
			skill("expelliarmus", "Expelliarmus", "엑스펠리아르무스", UTILITY, 12, 10, push(5, 1.6), debuff(5, WEAKNESS, 5, 0)),
			skill("protego", "Protego", "프로테고", DEFENSE, 15, 12, stance(COUNTER, 5, 40)));

		weapon("frostglass_orb", 1, 21, ORB, "Frostglass Orb", "서리유리 오브",
			art(0x8FA3B8, 0x4A6A8A, 0x9FE3FF), fx(Fx.Kind.SHARD, 0xB8ECFF),
			skill("ice_shard", "Ice Shard", "아이스 샤드", ATTACK, 6, 10, shoot(ProjectileStyle.ICICLE, 0.8).count(3).spread(18), inflict(SLOWNESS, 3, 0)),
			skill("frost_nova", "Frost Nova", "프로스트 노바", UTILITY, 12, 12, nova(4, 1.0), inflict(SLOWNESS, 3, 2)),
			skill("ice_armor", "Ice Armor", "아이스 아머", DEFENSE, 18, 12, shield(8, 10), buff(RESISTANCE, 6, 0)));

		// ---------------------------------------------------------------- tier 2: Caster
		weapon("fire_poison_wand", 2, 25, WAND, "Fire-Poison Wand", "불독의 완드",
			art(0xB04A2A, 0x5A2E22, 0x8FE04A), fx(Fx.Kind.ORB, 0xFF7A2E),
			skill("fire_arrow", "Fire Arrow", "파이어 애로우", ATTACK, 6, 12, shoot(ProjectileStyle.FIREBALL, 1.8).explode(1.5F), burn(4)),
			skill("poison_breath", "Poison Breath", "포이즌 브레스", ATTACK, 9, 14, slash(6, 70, 1.1), inflict(POISON, 6, 1)),
			skill("meditation", "Meditation", "메디테이션", UTILITY, 22, 12, buff(MANA_FLOW, 12, 0), empower(4, 40, 12)));

		weapon("ice_lightning_wand", 2, 28, WAND, "Ice-Lightning Wand", "썬콜의 완드",
			art(0xC8D8E8, 0x2F4A6B, 0xFFE34D), fx(Fx.Kind.BOLT, 0xA8E4FF),
			skill("cold_beam", "Cold Beam", "콜드 빔", ATTACK, 6, 12, beam(10, 1.6), inflict(SLOWNESS, 3, 1)),
			skill("thunder_bolt", "Thunder Bolt", "썬더 볼트", ATTACK, 9, 16, lightning(14, 3.5, 1.8)),
			skill("chilling_step", "Chilling Step", "칠링 스텝", MOVEMENT, 10, 12, debuff(4, SLOWNESS, 3, 1), blink(9)));

		weapon("fern_staff", 2, 31, STAFF, "Fern's Staff", "페른의 지팡이",
			art(0xE6E6F0, 0x8A6A52, 0x9A7BFF), fx(Fx.Kind.ORB, 0xC8B8FF),
			skill("rapid_zoltraak", "Rapid-Fire Zoltraak", "졸트라크 속사", ATTACK, 7, 14, shoot(ProjectileStyle.ORB, 0.75).count(6).speed(2.6F).interval(3)),
			skill("mana_suppression", "Mana Suppression", "마력 제한", UTILITY, 20, 14, stealth(6, 60), buff(MANA_FLOW, 6, 0)),
			skill("flight_magic", "Flight Magic", "비행 마법", MOVEMENT, 11, 12, blink(10), buff(SLOW_FALLING, 5, 0)));

		weapon("nursery_rhyme", 2, 35, GRIMOIRE, "Nursery Rhyme", "너서리 라임",
			art(0xE8C060, 0x6FB7E8, 0xFF8FB8), fx(Fx.Kind.PETAL, 0xFF8FB8),
			skill("vorpal_sword", "Vorpal Sword", "보팔 소드", ATTACK, 8, 14, repeat(4, 3, beam(9, 0.65))),
			skill("jabberwock", "Jabberwock", "재버워크", UTILITY, 28, 20, summon(Summon.IRON_GOLEM, 1, 20), debuff(6, WEAKNESS, 4, 0)),
			skill("mad_tea_party", "Mad Tea Party", "이상한 다과회", DEFENSE, 22, 16, sanctuary(5, 6, 4), debuff(6, SLOWNESS, 4, 1)));

		weapon("celestial_keys", 2, 39, SCEPTER, "Celestial Spirit Keys", "성령의 열쇠",
			art(0xF2C94C, 0x7A5A2A, 0x5FC8FF), fx(Fx.Kind.SPARK, 0xFFE27A),
			skill("urano_metria", "Urano Metria", "우라노 메트리아", ATTACK, 12, 18, strike(16, 3.5, 6, 1.0)),
			skill("gate_of_sagittarius", "Gate of the Archer: Sagittarius", "인마궁의 문: 사지타리우스", ATTACK, 8, 14,
				shoot(ProjectileStyle.ARROW, 1.0).count(5).spread(24)),
			skill("gate_of_aquarius", "Gate of the Water Bearer: Aquarius", "보병궁의 문: 아쿠아리우스", DEFENSE, 16, 16, nova(6, 1.0), push(7, 2.4)));

		weapon("yggdrasil_seed", 2, 43, ORB, "Seed of Yggdrasil", "이그드라실의 씨앗",
			art(0x7A5A34, 0x4A3A22, 0x7FE36B), fx(Fx.Kind.PETAL, 0x8CE36B),
			skill("entangling_roots", "Entangling Roots", "휘감는 뿌리", UTILITY, 14, 16, zone(4, 4, 0.6).at(14).pull(0.8).effect(SLOWNESS, 3)),
			skill("thorn_volley", "Thorn Volley", "가시 난사", ATTACK, 8, 14, shoot(ProjectileStyle.ORB, 1.0).count(5).spread(35), inflict(POISON, 4, 0)),
			skill("photosynthesis", "Photosynthesis", "광합성", DEFENSE, 20, 16, heal(10), buff(REGENERATION, 8, 1), buff(MANA_FLOW, 8, 0)));

		// ---------------------------------------------------------------- tier 3: Archmage
		weapon("elder_wand", 3, 45, WAND, "Elder Wand", "딱총나무 지팡이",
			art(0x9A9A9A, 0xBFAE8E, 0x4CE06A), fx(Fx.Kind.SPARK, 0x5CFF7A),
			skill("avada_kedavra", "Avada Kedavra", "아바다 케다브라", ATTACK, 14, 24, execute(18, 2.2, 30, 2.5)),
			skill("expecto_patronum", "Expecto Patronum", "익스펙토 패트로눔", DEFENSE, 26, 22, cleanse(), summon(Summon.WOLF, 2, 15)),
			skill("apparate", "Apparate", "순간이동술", MOVEMENT, 10, 16, blink(13), buff(SPEED, 4, 1)));

		weapon("mystogan_staves", 3, 49, STAFF, "Mystogan's Staves", "미스트간의 지팡이",
			art(0x6E7B8C, 0x2B3A4F, 0xB59CFF), fx(Fx.Kind.RUNE, 0xC4A8FF),
			skill("sleep_magic", "Sleep Magic", "수면 마법", UTILITY, 22, 20, debuff(8, STUN, 2, 0), debuff(8, BLINDNESS, 4, 0)),
			skill("skyscraper", "Skyscraper", "마천루", ATTACK, 14, 22, launch(7, 5), debuff(7, NAUSEA, 5, 0), delay(0.8, nova(7, 1.6))),
			skill("mikagura", "Five-Layered Magic Circle: Mikagura", "오중 마법진: 미카구라", ATTACK, 18, 26, strike(18, 4.5, 1, 2.6), stun(1.0)));

		weapon("heavenly_body_orb", 3, 53, ORB, "Orb of the Heavenly Bodies", "천체 마법의 오브",
			art(0xC0C0D8, 0x1A1B40, 0x5A4FD8), fx(Fx.Kind.SPARK, 0xB0A4FF),
			skill("meteor", "Meteor", "유성", MOVEMENT, 10, 16, dash(11, 1.4), buff(SPEED, 5, 1)),
			skill("altairis", "Altairis", "알테어리스", ATTACK, 15, 24, zone(4.5, 3, 1.6).at(16).pull(2.0)),
			skill("grand_chariot", "Grand Chariot", "그랑 샤리오", ATTACK, 18, 26, strike(20, 3.5, 7, 1.2)));

		weapon("prelati_spellbook", 3, 57, GRIMOIRE, "Prelati's Spellbook", "프렐라티즈 스펠북",
			art(0xB8A888, 0x5E6B4E, 0x2EE6A8), fx(Fx.Kind.SMOKE, 0x2FBF8F),
			skill("abyssal_horrors", "Summon Abyssal Horrors", "심해의 마물 소환", UTILITY, 28, 26, summon(Summon.WOLF, 3, 18)),
			skill("mental_pollution", "Mental Pollution", "정신 오염", UTILITY, 18, 18, debuff(9, VULNERABLE, 6, 1), debuff(9, NAUSEA, 6, 0)),
			skill("tentacle_bind", "Tentacle Bind", "촉수 구속", ATTACK, 13, 24, pull(7, 1.6), delay(0.3, zone(4.5, 4, 1.2).effect(SLOWNESS, 2))));

		weapon("laevateinn", 3, 62, WAND, "Laevateinn", "레바테인",
			art(0x4A2A22, 0x3A2018, 0xFF6A1A), fx(Fx.Kind.ORB, 0xFF5A1A),
			skill("muspel_fire_rain", "Fire Rain of Muspelheim", "무스펠헤임의 불비", ATTACK, 14, 24, rain(ProjectileStyle.FIREBALL, 18, 5, 12, 0.45, 1.5F), burn(3)),
			skill("twig_of_ruin", "Twig of Ruin", "파멸의 가지", ATTACK, 9, 22, beam(16, 2.6), burn(5)),
			skill("surtr_mantle", "Surtr's Mantle", "수르트의 외투", DEFENSE, 22, 20, buff(FIRE_RESISTANCE, 12, 0), stance(COUNTER, 6, 45)));

		weapon("bishop_scepter", 3, 66, SCEPTER, "Bishop's Holy Scepter", "비숍의 성홀",
			art(0xEDE8DA, 0xC9A64A, 0xFFD84A), fx(Fx.Kind.RING, 0xFFF6D6),
			skill("angel_ray", "Angel Ray", "엔젤레이", ATTACK, 7, 18, shoot(ProjectileStyle.ORB, 1.8).pierce(3), allyHeal(6, 6)),
			skill("holy_symbol", "Holy Symbol", "홀리 심볼", UTILITY, 28, 22, allyBuff(12, LUCK, 15, 1), allyBuff(12, RESISTANCE, 15, 0)),
			skill("genesis", "Genesis", "제네시스", ATTACK, 22, 28, repeat(3, 10, lightning(20, 5, 1.3))));

		// ---------------------------------------------------------------- tier 4: Grand Caster
		weapon("colchis_scepter", 4, 70, SCEPTER, "Scepter of Colchis", "콜키스의 홀",
			art(0x5B3A8C, 0x2A1A3A, 0xC77DFF), fx(Fx.Kind.ORB, 0xD070FF),
			skill("hecatic_wizard", "Hecatic Wizard", "헤카틱 위저드", ULTIMATE, 50, 38, rain(ProjectileStyle.ORB, 24, 7, 20, 0.5, 2.0F), inflict(SLOWNESS, 3, 1)),
			skill("rule_breaker", "Rule Breaker", "룰 브레이커", ATTACK, 14, 24, shadowstep(10, 1.6), inflict(VULNERABLE, 8, 2)),
			skill("high_speed_divine_words", "High-Speed Divine Words", "고속신언", UTILITY, 30, 16, refresh(50), buff(MANA_FLOW, 10, 0)),
			skill("argon_coin", "Argon Coin", "아르곤 코인", DEFENSE, 26, 22, heal(25), shield(8, 10)));

		weapon("yata_no_kagami", 4, 74, ORB, "Yata no Kagami", "야타노카가미",
			art(0xC0303A, 0x8A1A22, 0xF2DFA8), fx(Fx.Kind.ORB, 0x6FCBFF),
			skill("eightfold_blessings", "Eightfold Blessings of Amaterasu", "수천일광 천조팔야진석", ULTIMATE, 50, 36,
				allyHeal(12, 35), allyBuff(12, MANA_FLOW, 15, 0), restoreMana(30), refresh(60)),
			skill("enten", "Enten: Blazing Heaven", "염천", ATTACK, 9, 20, strike(18, 3.5, 3, 1.8), burn(4)),
			skill("hyoten", "Hyoten: Frozen Heaven", "빙천", ATTACK, 11, 22, rain(ProjectileStyle.ICICLE, 18, 4, 10, 0.5), inflict(SLOWNESS, 4, 2)),
			skill("shapeshift", "Shapeshift", "변화", DEFENSE, 24, 20, stance(GUARD, 7, 50), cleanse()));

		weapon("megumin_staff", 4, 78, STAFF, "Megumin's Staff", "메구밍의 지팡이",
			art(0xD4AF37, 0x4A2E1A, 0xE0213B), fx(Fx.Kind.SPARK, 0xFF3A2A),
			skill("explosion", "EXPLOSION!", "익스플로전!", ULTIMATE, 60, 40, strike(24, 8, 1, 10.0), burn(6), buff(SLOWNESS, 5, 2)),
			skill("drain_touch", "Drain Touch", "드레인 터치", ATTACK, 10, 16, nova(3.5, 1.8), lifesteal(40), restoreMana(12)),
			skill("crimson_demon_pose", "Crimson Demon Pose", "홍마족의 포즈", UTILITY, 18, 16, empower(4, 70, 10), buff(SPEED, 8, 1)),
			skill("manatite", "Manatite Crystal", "마나타이트", UTILITY, 30, 16, restoreMana(40), refresh(25)));

		weapon("sleeping_dragon_treatise", 4, 82, GRIMOIRE, "Treatise of the Sleeping Dragon", "와룡의 병법서",
			art(0xD4AF37, 0x2E4A6E, 0x7FC8FF), fx(Fx.Kind.RUNE, 0x8FD4FF),
			skill("stone_sentinel_maze", "Stone Sentinel Maze", "석병팔진", ULTIMATE, 55, 38,
				zone(8, 6, 1.2).at(20).pull(1.0).effect(STUN, 0), inflict(VULNERABLE, 8, 1)),
			skill("red_cliffs_east_wind", "East Wind of Red Cliffs", "적벽의 동남풍", ATTACK, 12, 26,
				push(4, 1.2), shoot(ProjectileStyle.FIREBALL, 1.6).count(5).spread(50).explode(2.0F), burn(5)),
			skill("discerning_eye", "Discerning Eye", "감식안", UTILITY, 16, 18, mark(24, 10, 40), allyBuff(10, STRENGTH, 10, 1)),
			skill("tacticians_advice", "Tactician's Advice", "군사의 충언", DEFENSE, 26, 24, allyBuff(10, RESISTANCE, 10, 1), allyHeal(10, 15)));

		weapon("lumen_histoire", 4, 86, ORB, "Lumen Histoire", "루멘 히스토리아",
			art(0xE8D8A8, 0x7A6A9A, 0xBFF6FF), fx(Fx.Kind.SPARK, 0xFFF0B8),
			skill("fairy_law", "Fairy Law", "페어리 로우", ULTIMATE, 55, 40, debuff(16, SLOWNESS, 2, 2), delay(1.0, nova(16, 3.6))),
			skill("fairy_glitter", "Fairy Glitter", "페어리 글리터", ATTACK, 12, 28, shoot(ProjectileStyle.ORB, 2.6).explode(3.5F).homing(), inflict(VULNERABLE, 6, 1)),
			skill("fairy_sphere", "Fairy Sphere", "페어리 스피어", DEFENSE, 30, 30, stance(GUARD, 8, 60), allyBuff(10, RESISTANCE, 8, 2)),
			skill("fairy_tactician", "Fairy Tactician", "요정군사", UTILITY, 20, 20, allyBuff(12, SPEED, 10, 1), allyBuff(12, MANA_FLOW, 10, 0), refresh(20)));

		weapon("merlin_staff", 4, 90, STAFF, "Merlin's Staff", "멀린의 지팡이",
			art(0xE8E8F8, 0x9A8AC8, 0xFF9AD5), fx(Fx.Kind.PETAL, 0xF5C8FF),
			skill("garden_of_avalon", "Garden of Avalon", "가든 오브 아발론", ULTIMATE, 55, 40, sanctuary(10, 10, 5), allyBuff(10, RESISTANCE, 10, 1)),
			skill("hero_creation", "Hero Creation", "영웅작성", UTILITY, 24, 24, allyBuff(10, STRENGTH, 12, 1), allyBuff(10, ABSORPTION, 12, 1)),
			skill("flower_storm", "Flower Storm", "꽃보라", ATTACK, 9, 22, shoot(ProjectileStyle.ORB, 1.1).count(7).spread(70).homing(), inflict(WEAKNESS, 4, 0)),
			skill("illusion", "Illusion", "환술", DEFENSE, 18, 18, stance(EVADE, 7, 50), blink(8)));

		weapon("frieren_staff", 4, 95, STAFF, "Frieren's Staff", "프리렌의 지팡이",
			art(0xD4AF37, 0xEDEAE0, 0xD9304A), fx(Fx.Kind.BOLT, 0xE8F0FF),
			skill("zoltraak_barrage", "Zoltraak Barrage", "졸트라크 일제 사격", ULTIMATE, 50, 40, barrage(ProjectileStyle.ORB, 30, 0.4)),
			skill("judradjim", "Judradjim", "쥬드라질름", ATTACK, 10, 26, lightning(22, 4.5, 2.6), stun(0.8)),
			skill("defensive_magic", "Defensive Magic", "방어 마법", DEFENSE, 18, 20, stance(GUARD, 6, 55), shield(12, 6)),
			skill("flower_field", "Spell to Make a Field of Flowers", "꽃밭을 만드는 마법", UTILITY, 30, 24, sanctuary(7, 8, 4), cleanse()));

		weapon("lemegeton", 4, 100, GRIMOIRE, "Lemegeton", "레메게톤",
			art(0xD4AF37, 0x24203A, 0xFF4A3A), fx(Fx.Kind.RUNE, 0xFFC94A),
			skill("ars_almadel_salomonis", "Ars Almadel Salomonis", "아르스 알마델 살로모니스", ULTIMATE, 60, 40, strike(24, 7, 12, 1.9), inflict(VULNERABLE, 8, 1)),
			skill("demon_pillar_eruption", "Demon Pillar Eruption", "마신주 강림", ATTACK, 12, 28, launch(6, 5), nova(6, 3.4)),
			skill("clairvoyance", "Clairvoyance", "천리안", UTILITY, 18, 20, mark(24, 12, 50), debuff(14, GLOWING, 10, 0)),
			skill("rings_of_solomon", "Rings of Solomon", "솔로몬의 반지", DEFENSE, 28, 26, stance(COUNTER, 6, 50), shield(12, 10)));
	}
}
