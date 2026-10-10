package com.minecraftmode.job.content;

import static com.minecraftmode.job.skill.Actions.*;
import static com.minecraftmode.job.skill.CombatState.Stance.*;
import static com.minecraftmode.job.skill.SkillKind.*;
import static com.minecraftmode.job.weapon.Archetype.*;
import static com.minecraftmode.registry.ModEffects.BLEEDING;
import static com.minecraftmode.registry.ModEffects.STUN;
import static com.minecraftmode.registry.ModEffects.VULNERABLE;
import static net.minecraft.world.effect.MobEffects.*;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.skill.Fx;
import com.minecraftmode.job.weapon.ProjectileStyle;

/**
 * Pirate: Pirate -> Captain -> Emperor of the Sea -> Pirate King. Cutlasses, rapiers, knuckles and anchors up close;
 * pistols, muskets, hand cannons and harpoons at range. Mostly One Piece (the Straw Hats, Buggy, Ace, Blackbeard, Kaido,
 * Shanks, Big Mom, Whitebeard, Roger), plus Fate's Francis Drake and Anne Bonny &amp; Mary Read, Pirates of the Caribbean,
 * Moby-Dick and MapleStory's Corsair.
 */
public final class PirateContent extends ClassContent {
	public PirateContent() {
		super(JobClass.PIRATE);
	}

	@Override
	public void define() {
		// ---------------------------------------------------------------- tier 1: Pirate
		weapon("rusty_cutlass", 1, 10, CUTLASS, "Rusty Cutlass", "녹슨 커틀러스",
			art(0xA0886A, 0x5C3A21, 0xB5651D), fx(Fx.Kind.SLASH, 0xD9C29A),
			skill("plunder_slash", "Plunder Slash", "약탈 베기", ATTACK, 6, 10, slash(4.0, 140, 1.5), loot(15)),
			skill("boarding_rush", "Boarding Rush", "적선 돌입", MOVEMENT, 10, 8, dash(6, 0.9)),
			skill("swig_of_rum", "Swig of Rum", "럼주 한 모금", UTILITY, 18, 10, heal(12), buff(STRENGTH, 6, 0)));

		weapon("flintlock_pistol", 1, 15, PISTOL, "Flintlock Pistol", "플린트락 권총",
			art(0x7A7F87, 0x6B4226, 0xC9A227), fx(Fx.Kind.SMOKE, 0xA8A8A8),
			skill("lead_star", "Lead Star", "필살 납성", ATTACK, 5, 8, shoot(ProjectileStyle.BULLET, 1.7).pierce(1)),
			skill("tabasco_star", "Tabasco Star", "필살 타바스코성", UTILITY, 12, 10, shoot(ProjectileStyle.BULLET, 0.9), inflict(BLINDNESS, 3, 0), inflict(NAUSEA, 5, 0)),
			skill("tactical_retreat", "Tactical Retreat", "전략적 후퇴", MOVEMENT, 12, 8, push(3.5, 1.2), buff(SPEED, 5, 1)));

		weapon("brawler_knuckles", 1, 20, KNUCKLE, "Brawler's Knuckles", "싸움꾼의 너클",
			art(0xC08A3E, 0xC0392B, 0xF2D16B), fx(Fx.Kind.SPARK, 0xFFE08A),
			skill("gum_gum_pistol", "Gum-Gum Pistol", "고무고무 피스톨", ATTACK, 5, 8, beam(7, 1.6), stun(0.5)),
			skill("gum_gum_whip", "Gum-Gum Whip", "고무고무 채찍", ATTACK, 9, 12, slash(5, 220, 1.1), push(5, 1.0)),
			skill("gum_gum_rocket", "Gum-Gum Rocket", "고무고무 로켓", MOVEMENT, 12, 10, grapple(16), empower(2, 30, 6)));

		// ---------------------------------------------------------------- tier 2: Captain
		weapon("wado_ichimonji", 2, 25, CUTLASS, "Wado Ichimonji", "화도일문자",
			art(0xDDE4EA, 0xEDE8DA, 0x2E8B57), fx(Fx.Kind.SLASH, 0xA8F0C0),
			skill("oni_giri", "Santoryu: Oni Giri", "삼도류 오니기리", MOVEMENT, 9, 14, dash(7, 1.8), inflict(BLEEDING, 4, 0)),
			skill("shishi_sonson", "Ittoryu: Shishi Sonson", "일도류 사자손손", ATTACK, 8, 14, slash(5.5, 60, 2.2), inflict(VULNERABLE, 4, 0)),
			skill("tatsumaki", "Santoryu: Tatsumaki", "삼도류 타츠마키", ATTACK, 12, 18, launch(5, 4), repeat(3, 5, nova(4, 0.8))));

		weapon("soul_solid", 2, 28, RAPIER, "Soul Solid", "소울 솔리드",
			art(0xCFE4F2, 0x1E1E1E, 0x6FE3FF), fx(Fx.Kind.SHARD, 0xBFF4FF),
			skill("yahazu_giri", "Yahazu Giri", "야하즈 베기", ATTACK, 9, 14, dash(5, 0), delay(0.8, nova(4.5, 1.9))),
			skill("soul_parade", "Soul Parade", "소울 퍼레이드", ATTACK, 10, 16, slash(4.5, 120, 1.4), inflict(SLOWNESS, 4, 2)),
			skill("soul_king_live", "Soul King Live", "소울 킹 라이브", UTILITY, 22, 16, allyBuff(8, SPEED, 8, 1), allyBuff(8, REGENERATION, 6, 0)));

		weapon("ahab_harpoon", 2, 31, HARPOON, "Ahab's Harpoon", "에이해브의 작살",
			art(0xA9B4BE, 0x7A5230, 0xEDEDED), fx(Fx.Kind.BUBBLE, 0x6FB7E8),
			skill("whale_line", "Whale Line", "포경줄", ATTACK, 10, 14, shoot(ProjectileStyle.HARPOON, 1.4).pierce(1), delay(0.5, pull(7, 1.5))),
			skill("ahabs_obsession", "Ahab's Obsession", "에이해브의 집념", UTILITY, 18, 12, mark(20, 10, 30), buff(STRENGTH, 8, 0)),
			skill("whale_breach", "Breach", "고래의 도약", MOVEMENT, 12, 14, leap(5, 4, 1.6), inflict(SLOWNESS, 2, 1)));

		weapon("buccaneer_musket", 2, 34, MUSKET, "Buccaneer's Long Musket", "버커니어 장총",
			art(0x5E6670, 0x7A4A24, 0xB08D57), fx(Fx.Kind.SMOKE, 0xB8B8B8),
			skill("long_shot", "Long Shot", "장거리 저격", ATTACK, 7, 14, shoot(ProjectileStyle.BULLET, 2.2).pierce(3).speed(5.0F)),
			skill("buckshot", "Buckshot", "산탄 사격", ATTACK, 9, 16, shoot(ProjectileStyle.BULLET, 0.8).count(5).spread(45), push(3, 1.0)),
			skill("powder_smoke", "Powder Smoke", "화약 연막", DEFENSE, 18, 14, stealth(3, 60), buff(SPEED, 4, 1)));

		weapon("black_leg_knuckles", 2, 38, KNUCKLE, "Black Leg Knuckles", "검은 다리 너클",
			art(0x2E2E33, 0x1B1B2F, 0xFF7A1A), fx(Fx.Kind.SPARK, 0xFF8C1A),
			skill("diable_jambe", "Diable Jambe", "악마풍각", ATTACK, 10, 16, burn(5), repeat(3, 4, slash(3.5, 140, 0.9))),
			skill("concasse", "Concasse", "콩카세", ATTACK, 12, 16, leap(5, 3.5, 1.9), stun(1.0)),
			skill("sky_walk", "Sky Walk", "스카이 워크", MOVEMENT, 10, 12, blink(8), buff(JUMP_BOOST, 6, 1), buff(SLOW_FALLING, 4, 0)));

		weapon("galleon_cannon", 2, 42, HAND_CANNON, "Galleon Hand Cannon", "갤리온 핸드 캐논",
			art(0x3E4248, 0x6B4A2B, 0xD4AF37), fx(Fx.Kind.SMOKE, 0x8C8C8C),
			skill("broadside", "Broadside", "현측 일제 포격", ATTACK, 10, 18, shoot(ProjectileStyle.CANNONBALL, 1.3).count(3).spread(35).explode(2.0F)),
			skill("chain_shot", "Chain Shot", "사슬탄", UTILITY, 12, 14, shoot(ProjectileStyle.CANNONBALL, 1.6).pierce(3), inflict(SLOWNESS, 4, 2)),
			skill("brace_for_impact", "Brace for Impact", "충격 대비", DEFENSE, 20, 14, stance(GUARD, 5, 40), shield(8, 6)));

		// ---------------------------------------------------------------- tier 3: Emperor of the Sea
		weapon("buggy_cannon", 3, 45, HAND_CANNON, "Buggy's Hand Cannon", "버기의 핸드 캐논",
			art(0x4A4E57, 0x1F3A93, 0xE03C31), fx(Fx.Kind.SMOKE, 0xD95040),
			skill("buggy_ball", "Special Buggy Ball", "특제 버기 볼", ATTACK, 14, 24, shoot(ProjectileStyle.CANNONBALL, 2.6).explode(4.0F), burn(4)),
			skill("chop_chop_cannon", "Chop-Chop Cannon", "동강동강 포", ATTACK, 8, 18, shoot(ProjectileStyle.KNIFE, 1.3).count(3).spread(20).homing()),
			skill("chop_chop_escape", "Chop-Chop Emergency Escape", "동강동강 긴급 탈출", MOVEMENT, 14, 16, blink(10), stance(EVADE, 5, 50)));

		weapon("fire_fist_knuckles", 3, 49, KNUCKLE, "Fire Fist Knuckles", "불주먹 너클",
			art(0xD35400, 0x2B2B2B, 0xFFC93C), fx(Fx.Kind.ORB, 0xFF7A1A),
			skill("hiken", "Hiken", "화권", ATTACK, 10, 20, shoot(ProjectileStyle.FIREBALL, 2.4).pierce(6).explode(2.0F), burn(5)),
			skill("hotarubi", "Hotarubi: Hidaruma", "반딧불 화달마", ATTACK, 16, 22, zone(5, 4, 1.0).at(12), burn(3)),
			skill("flame_logia", "Flame Logia", "불꽃 자연계", DEFENSE, 20, 18, stance(EVADE, 6, 55), buff(FIRE_RESISTANCE, 10, 0)));

		weapon("blackbeard_flintlock", 3, 53, PISTOL, "Blackbeard's Flintlock", "검은 수염의 플린트락",
			art(0x2A2A2A, 0x4B2E1A, 0x6A0DAD), fx(Fx.Kind.SMOKE, 0x3A1F5C),
			skill("black_hole", "Black Hole", "블랙홀", ATTACK, 18, 26, zone(5, 5, 0.8).at(14).pull(1.2).effect(SLOWNESS, 1)),
			skill("black_vortex", "Black Vortex", "흑와", UTILITY, 12, 20, pull(9, 2.0), debuff(6, WEAKNESS, 6, 1)),
			skill("three_flintlocks", "Three Flintlocks", "세 자루 권총", ATTACK, 8, 18, shoot(ProjectileStyle.BULLET, 1.6).count(3).spread(10).interval(4).pierce(1)));

		weapon("yasopp_rifle", 3, 57, MUSKET, "Yasopp's Rifle", "야소프의 장총",
			art(0x6B6F75, 0x8B5A2B, 0xB22222), fx(Fx.Kind.SPARK, 0xFFE6A0),
			skill("ants_eye", "Ant's Eye Shot", "개미 눈 저격", ATTACK, 9, 20, mark(24, 8, 30), shoot(ProjectileStyle.BULLET, 2.6).pierce(2).speed(5.0F)),
			skill("observation_haki", "Observation Haki", "견문색 패기", DEFENSE, 20, 18, stance(EVADE, 6, 45), debuff(16, GLOWING, 8, 0)),
			skill("covering_fire", "Covering Fire", "엄호 사격", UTILITY, 14, 20, shoot(ProjectileStyle.BULLET, 1.3).count(4).spread(30).interval(3), allyBuff(8, SPEED, 6, 1)));

		weapon("hassaikai", 3, 61, ANCHOR, "Hassaikai", "팔재계",
			art(0x4A4F57, 0x3B2F2F, 0x3FA7D6), fx(Fx.Kind.BOLT, 0x9FD8FF),
			skill("thunder_bagua", "Thunder Bagua", "뇌명팔괘", ATTACK, 14, 26, dash(8, 2.6), stun(1.2)),
			skill("bolo_breath", "Bolo Breath", "열식", ATTACK, 12, 22, shoot(ProjectileStyle.FIREBALL, 1.5).count(3).spread(30).explode(2.5F), burn(4)),
			skill("strongest_creature", "World's Strongest Creature", "이 세상 최강의 생물", DEFENSE, 24, 22, buff(RESISTANCE, 8, 2), shield(12, 8), taunt(8)));

		weapon("gryphon_sabre", 3, 66, CUTLASS, "Gryphon", "그리폰",
			art(0xE1E6EC, 0x1E1E1E, 0xC0392B), fx(Fx.Kind.SLASH, 0xFF4B3E),
			skill("haki_burst", "Conqueror's Haki Burst", "패왕색 패기 방출", UTILITY, 22, 24, debuff(10, STUN, 1.5, 0), debuff(10, WEAKNESS, 6, 1)),
			skill("armament_slash", "Armament Slash", "무장색 참격", ATTACK, 9, 22, slash(5, 160, 2.4), inflict(VULNERABLE, 5, 1)),
			skill("end_this_war", "I Came to End This War", "전쟁을 끝내러 왔다", DEFENSE, 26, 22, stance(COUNTER, 6, 50), allyBuff(10, RESISTANCE, 8, 1)));

		// ---------------------------------------------------------------- tier 4: Pirate King
		weapon("battleship_cannon", 4, 70, HAND_CANNON, "Battleship Cannon", "배틀쉽 캐논",
			art(0x4F5B66, 0x2C3E50, 0xF1C40F), fx(Fx.Kind.SPARK, 0xFFC94D),
			skill("battleship", "Battleship", "배틀쉽", ULTIMATE, 50, 38, shield(16, 10), buff(RESISTANCE, 10, 1),
				shoot(ProjectileStyle.CANNONBALL, 3.2).count(3).spread(30).explode(3.0F)),
			skill("octopus", "Octopus", "옥토퍼스", ATTACK, 18, 24, zone(4, 6, 1.2).at(12).effect(BLINDNESS, 0)),
			skill("air_strike", "Air Strike", "에어 스트라이크", ATTACK, 14, 26, strike(20, 3.5, 5, 1.6)),
			skill("bullseye", "Bullseye", "불스아이", UTILITY, 20, 18, mark(24, 10, 40), empower(3, 50, 10)));

		weapon("el_draque", 4, 73, PISTOL, "El Draque", "엘 드라케",
			art(0xD4AF37, 0x5B1A1A, 0xFFF2A8), fx(Fx.Kind.COIN, 0xFFD700),
			skill("golden_wild_hunt", "Golden Wild Hunt", "황금 사슴과 폭풍의 밤", ULTIMATE, 50, 40, barrage(ProjectileStyle.CANNONBALL, 14, 0.6),
				delay(1.5, strike(22, 5, 1, 3.5)), loot(25)),
			skill("golden_rule", "Golden Rule", "황금률", ATTACK, 9, 20, shoot(ProjectileStyle.COIN, 1.6).count(5).spread(40).pierce(1), loot(30)),
			skill("voyager_of_the_storm", "Voyager of the Storm", "폭풍의 항해자", UTILITY, 26, 22, allyBuff(10, STRENGTH, 10, 1), allyBuff(10, DOLPHINS_GRACE, 10, 0)),
			skill("pioneer_of_the_stars", "Pioneer of the Stars", "별의 개척자", MOVEMENT, 12, 18, dash(10, 1.6), buff(SPEED, 8, 2)));

		weapon("bonny_read_musket", 4, 77, MUSKET, "Anne & Mary's Musket", "앤과 메리의 머스킷",
			art(0x8A9099, 0x5D3A1A, 0x29B6F6), fx(Fx.Kind.FEATHER, 0x7FDBFF),
			skill("caribbean_free_bird", "Caribbean Free Bird", "카리비안 프리 버드", ULTIMATE, 45, 38, shadowstep(16, 2.0),
				delay(0.5, shoot(ProjectileStyle.BULLET, 4.2).pierce(4)), inflict(VULNERABLE, 5, 1)),
			skill("marys_cutlass", "Mary's Cutlass", "메리의 커틀러스", ATTACK, 8, 20, slash(4.5, 160, 2.2), inflict(BLEEDING, 5, 1)),
			skill("annes_powder_shot", "Anne's Powder Shot", "앤의 화약 사격", ATTACK, 10, 22, shoot(ProjectileStyle.BULLET, 2.6).explode(2.0F), inflict(SLOWNESS, 3, 1)),
			skill("pirates_glory", "Pirate's Glory", "해적의 영광", DEFENSE, 22, 18, stance(EVADE, 6, 45), heal(15)));

		weapon("dutchman_harpoon", 4, 81, HARPOON, "Flying Dutchman's Harpoon", "플라잉 더치맨의 작살",
			art(0x5F7F74, 0x2E3B30, 0x3FFFD2), fx(Fx.Kind.BUBBLE, 0x3FC5A8),
			skill("release_the_kraken", "Release the Kraken", "크라켄을 풀어라", ULTIMATE, 55, 40, zone(6, 5, 1.0).at(16).pull(1.8).effect(SLOWNESS, 2),
				delay(2.0, strike(16, 5, 1, 3.5))),
			skill("tentacle_lash", "Tentacle Lash", "촉수 채찍", ATTACK, 8, 20, pull(6, 1.2), slash(5, 180, 2.2)),
			skill("fear_death", "Do You Fear Death?", "죽음이 두려운가?", UTILITY, 22, 20, debuff(10, WEAKNESS, 8, 1), debuff(10, DARKNESS, 5, 0)),
			skill("rise_from_the_deep", "Rise from the Deep", "심해에서 떠오르다", MOVEMENT, 14, 18, blink(12), buff(DOLPHINS_GRACE, 10, 0), buff(WATER_BREATHING, 15, 0)));

		weapon("napoleon_rapier", 4, 85, RAPIER, "Napoleon", "나폴레옹",
			art(0xE8E3D3, 0xD81B60, 0xFFC107), fx(Fx.Kind.PETAL, 0xFF8FC7),
			skill("ikoku_sovereignty", "Ikoku Sovereignty", "위국", ULTIMATE, 50, 40, shoot(ProjectileStyle.WAVE, 4.2).pierce(12), push(6, 2.0)),
			skill("soul_pocus", "Soul Pocus", "소울 포커스", ATTACK, 12, 26, lifesteal(35), nova(6, 1.8), inflict(WEAKNESS, 6, 1)),
			skill("zeus_raitei", "Zeus: Raitei", "제우스: 뇌정", ATTACK, 10, 22, lightning(18, 4, 2.6), stun(0.8)),
			skill("croquembouche", "Croquembouche Craving", "크로캉부슈 발작", UTILITY, 28, 20, heal(25), buff(STRENGTH, 10, 2), buff(SPEED, 10, 1)));

		weapon("moby_dick_anchor", 4, 89, ANCHOR, "Moby Dick's Anchor", "모비딕호의 닻",
			art(0x7E8790, 0x4A3424, 0xF0F0F0), fx(Fx.Kind.RING, 0xE0F0FF),
			skill("island_shaker", "Island Shaker", "섬 흔들기", ULTIMATE, 55, 40, nova(8, 3.5), stun(1.5), delay(0.5, launch(8, 5))),
			skill("seaquake", "Seaquake", "해진", ATTACK, 10, 24, shoot(ProjectileStyle.WAVE, 2.6).count(3).spread(25).pierce(10)),
			skill("anchor_drop", "Anchor Drop", "닻 내리기", MOVEMENT, 14, 22, leap(7, 5, 2.6), inflict(SLOWNESS, 3, 2)),
			skill("my_sons", "My Sons", "내 아들들", UTILITY, 30, 26, allyBuff(12, STRENGTH, 12, 1), allyBuff(12, RESISTANCE, 12, 0), allyHeal(12, 15)));

		weapon("nika_knuckles", 4, 94, KNUCKLE, "Nika's Knuckles", "니카의 너클",
			art(0xF5F5F5, 0xC0392B, 0xFFD54F), fx(Fx.Kind.SPARK, 0xFFF6D5),
			skill("bajrang_gun", "Gum-Gum Bajrang Gun", "고무고무 바지랑 건", ULTIMATE, 50, 40, strike(20, 6, 1, 4.5), stun(1.5)),
			skill("red_hawk", "Gum-Gum Red Hawk", "고무고무 레드 호크", ATTACK, 9, 22, beam(10, 2.6), burn(5)),
			skill("jet_gatling", "Gum-Gum Jet Gatling", "고무고무 제트 개틀링", ATTACK, 10, 24, buff(SPEED, 6, 1), repeat(5, 3, slash(4, 100, 1.0))),
			skill("drums_of_liberation", "Drums of Liberation", "해방의 드럼", DEFENSE, 24, 20, cleanse(), buff(JUMP_BOOST, 10, 2), stance(COUNTER, 6, 50)));

		weapon("pirate_kings_sabre", 4, 100, CUTLASS, "Pirate King's Sabre", "해적왕의 검",
			art(0xF2F2F2, 0x8B0000, 0xFFD700), fx(Fx.Kind.BOLT, 0xD01E2E),
			skill("divine_departure", "Divine Departure", "신피", ULTIMATE, 55, 40, slash(6, 180, 4.0), shoot(ProjectileStyle.WAVE, 5.0).pierce(15), stun(1.0)),
			skill("conquerors_clash", "Conqueror's Clash", "패왕색 충돌", ATTACK, 12, 26, nova(6, 3.4), push(8, 3.0)),
			skill("will_of_d", "Will of D.", "D의 의지", DEFENSE, 26, 22, cleanse(), heal(20), stance(GUARD, 6, 50)),
			skill("great_pirate_era", "Dawn of the Great Pirate Era", "대해적시대의 개막", UTILITY, 30, 24, allyBuff(12, STRENGTH, 12, 2), allyBuff(12, LUCK, 15, 1)));
	}
}
