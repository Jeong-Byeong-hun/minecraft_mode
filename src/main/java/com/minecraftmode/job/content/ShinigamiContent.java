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
 * Soul Reaper (Bleach): Soul Reaper -> Lieutenant -> Captain -> Captain-Commander. Zanpakuto of every
 * shape (katana, whip-blade, spear, twin blades, cleaver), Flash Step, Kido (Hado destruction,
 * Bakudo binding) and the release stages Shikai and Bankai. Tier 1 is the academy and the first
 * blades; tier 2 the lieutenants' Shikai (Zabimaru, Sode no Shirayuki, Hozukimaru, Haineko,
 * Tobiume, Benihime); tier 3 the captains (Senbonzakura, Hyorinmaru, Suzumebachi, Katen Kyokotsu,
 * Shinso, Nozarashi); tier 4 the Bankai and the strongest blades (Tensa Zangetsu, Senbonzakura
 * Kageyoshi, Daiguren Hyorinmaru, Ryujin Jakka, Kamishini no Yari, Kyoka Suigetsu, Katen Kyokotsu:
 * Karamatsu Shinju and Ichigo's true Zangetsu).
 */
public final class ShinigamiContent extends ClassContent {
	public ShinigamiContent() {
		super(JobClass.SHINIGAMI);
	}

	@Override
	public void define() {
		// ---------------------------------------------------------------- tier 1: Soul Reaper
		weapon("nameless_zanpakuto", 1, 10, KATANA, "Nameless Zanpakuto", "이름 없는 참백도",
			art(0xD8DEE6, 0x1A1A24, 0xE8E8F0), fx(Fx.Kind.SLASH, 0xE8F0FF),
			skill("zanjutsu", "Zanjutsu", "참술", ATTACK, 6, 10, slash(4, 140, 1.6)),
			skill("shunpo", "Flash Step", "순보", MOVEMENT, 9, 8, blink(8), empower(2, 30, 5)),
			skill("hado_4_byakurai", "Hado #4: Byakurai", "파도 4번 백뢰", ATTACK, 10, 12, beam(10, 1.6), stun(0.4)));

		weapon("academy_bokken", 1, 15, LONGSWORD, "Academy Training Blade", "진앙학원 수련검",
			art(0xC8A878, 0x5A3A20, 0x3A6AD8), fx(Fx.Kind.SPARK, 0xBFD8FF),
			skill("hakuda", "Hakuda", "백타", ATTACK, 5, 8, repeat(3, 4, slash(3, 90, 0.6)), push(3, 0.8)),
			skill("bakudo_1_sai", "Bakudo #1: Sai", "박도 1번 새", UTILITY, 12, 10, debuff(6, SLOWNESS, 4, 2)),
			skill("hado_31_shakkaho", "Hado #31: Shakkaho", "파도 31번 적화포", ATTACK, 9, 14, shoot(ProjectileStyle.FIREBALL, 1.7).explode(2.0F), burn(3)));

		weapon("hollow_hunter_blade", 1, 20, NINJATO, "Hollow Hunter's Blade", "호로 사냥 참백도",
			art(0xE0E4EA, 0x2A2A3A, 0x8A2AC8), fx(Fx.Kind.SLASH, 0xC8A0FF),
			skill("konso", "Konso", "혼장", UTILITY, 14, 10, execute(4, 1.6, 30, 2.0), heal(8)),
			skill("hollow_cut", "Hollow Cut", "호로 베기", ATTACK, 7, 12, dash(6, 1.5), inflict(BLEEDING, 4, 0)),
			skill("reiatsu_flare", "Spiritual Pressure", "영압 해방", DEFENSE, 18, 12, push(5, 1.4), buff(RESISTANCE, 5, 0)));

		// ---------------------------------------------------------------- tier 2: Lieutenant (Shikai)
		weapon("zabimaru", 2, 25, KUSARIGAMA, "Zabimaru", "사미환",
			art(0xC8C0B0, 0x8A1A1A, 0xE04040), fx(Fx.Kind.SLASH, 0xFF6060),
			skill("howl_zabimaru", "Howl, Zabimaru", "짖어라, 사미환", ATTACK, 7, 14, beam(9, 1.8), inflict(BLEEDING, 4, 0)),
			skill("whip_blade_sweep", "Whip-Blade Sweep", "사절 휘두르기", ATTACK, 10, 16, slash(6.5, 240, 1.4)),
			skill("hihio_zabimaru", "Hihio Zabimaru", "비비왕 사미환", ATTACK, 16, 20, strike(12, 3, 3, 1.4), pull(6, 1.0)));

		weapon("sode_no_shirayuki", 2, 29, KATANA, "Sode no Shirayuki", "소데노시라유키",
			art(0xF4F8FF, 0xF0F0F8, 0x9FD0FF), fx(Fx.Kind.SHARD, 0xCFEFFF),
			skill("some_no_mai_tsukishiro", "First Dance: Tsukishiro", "첫 번째 춤 월백", ATTACK, 10, 16, zone(3.5, 2, 1.0).at(10).effect(SLOWNESS, 3), stun(0.8)),
			skill("tsugi_no_mai_hakuren", "Second Dance: Hakuren", "두 번째 춤 백련", ATTACK, 12, 18, shoot(ProjectileStyle.ICICLE, 1.6).count(5).spread(40).pierce(3),
				inflict(SLOWNESS, 4, 1)),
			skill("san_no_mai_shirafune", "Third Dance: Shirafune", "세 번째 춤 백도", DEFENSE, 16, 14, shield(8, 6), nova(3, 1.0)));

		weapon("hozukimaru", 2, 32, SPEAR, "Hozukimaru", "호오즈키마루",
			art(0xC8A060, 0x6A3A1A, 0xF0F0F0), fx(Fx.Kind.SPARK, 0xFFD27F),
			skill("split_spear", "Split Spear", "삼절곤 분리", ATTACK, 7, 14, repeat(3, 5, beam(6, 0.8))),
			skill("battle_lust", "Eleventh's Battle Lust", "11번대의 전투광", UTILITY, 20, 14, buff(STRENGTH, 8, 0), taunt(8)),
			skill("ryumon_hozukimaru", "Ryumon Hozukimaru", "용문귀등환", ATTACK, 15, 22, leap(5, 4, 2.2), inflict(BLEEDING, 5, 1)));

		weapon("haineko", 2, 36, NINJATO, "Haineko", "하이네코",
			art(0xD8D4CC, 0x8A6A3A, 0xE8B0C8), fx(Fx.Kind.SMOKE, 0xD8D0C8),
			skill("growl_haineko", "Growl, Haineko", "울어라, 하이네코", ATTACK, 9, 16, zone(4, 3, 0.8).at(8)),
			skill("ash_cloud", "Ash Cloud", "잿빛 구름", UTILITY, 14, 14, debuff(6, BLINDNESS, 3, 0), stealth(3, 40)),
			skill("ash_storm", "Ash Storm", "잿빛 폭풍", ATTACK, 14, 20, repeat(4, 5, nova(4.5, 0.8)), inflict(BLEEDING, 4, 0)));

		weapon("tobiume", 2, 40, KATANA, "Tobiume", "토비우메",
			art(0xF0E8E8, 0x8A2A2A, 0xFF8A3A), fx(Fx.Kind.ORB, 0xFF8A3A),
			skill("snap_tobiume", "Snap, Tobiume", "튀어라, 토비우메", ATTACK, 7, 14, shoot(ProjectileStyle.FIREBALL, 1.6).count(3).spread(30).explode(1.5F), burn(3)),
			skill("hado_63_raikoho", "Hado #63: Raikoho", "파도 63번 뇌후포", ATTACK, 12, 20, beam(14, 2.2), stun(0.6)),
			skill("bakudo_8_seki", "Bakudo #8: Seki", "박도 8번 척", DEFENSE, 14, 12, stance(COUNTER, 4, 50), push(4, 1.2)));

		weapon("benihime", 2, 43, KATANA, "Benihime", "홍희",
			art(0xD8DCE4, 0x2A2A24, 0xC8202A), fx(Fx.Kind.SLASH, 0xFF4A5A),
			skill("nake_benihime", "Cry, Benihime", "울어라, 홍희", ATTACK, 8, 16, shoot(ProjectileStyle.WAVE, 1.8).pierce(4)),
			skill("chikasumi_no_tate", "Chikasumi no Tate", "혈하의 방패", DEFENSE, 16, 14, shield(10, 6), stance(GUARD, 3, 40)),
			skill("shibari_benihime", "Shibari Benihime", "묶어라, 홍희", UTILITY, 14, 18, debuff(7, SLOWNESS, 4, 4), inflict(VULNERABLE, 6, 0), stun(1.0)));

		// ---------------------------------------------------------------- tier 3: Captain
		weapon("senbonzakura", 3, 45, KATANA, "Senbonzakura", "천본앵",
			art(0xF0E8F0, 0x2A2A3A, 0xFFB7C5), fx(Fx.Kind.PETAL, 0xFFB7C5),
			skill("scatter_senbonzakura", "Scatter, Senbonzakura", "흩날려라, 천본앵", ATTACK, 9, 20, repeat(5, 4, nova(5, 0.6)), inflict(BLEEDING, 4, 1)),
			skill("petal_volley", "Petal Volley", "꽃잎 연격", ATTACK, 10, 20, barrage(ProjectileStyle.BLADE, 10, 0.4)),
			skill("senka", "Senka", "섬화", MOVEMENT, 12, 16, shadowstep(10, 2.2)));

		weapon("hyorinmaru", 3, 50, KATANA, "Hyorinmaru", "빙륜환",
			art(0xE0F4FF, 0x1A3A6A, 0x5AC8FF), fx(Fx.Kind.SHARD, 0x9FE2FF),
			skill("sit_upon_frozen_heavens", "Sit Upon the Frozen Heavens", "서리 하늘에 앉아라", ATTACK, 9, 20, shoot(ProjectileStyle.ICICLE, 2.2).pierce(6).explode(2.0F),
				inflict(SLOWNESS, 4, 2)),
			skill("ice_dragon", "Ice Dragon", "빙룡", ATTACK, 14, 24, beam(16, 2.6), stun(1.0)),
			skill("ice_wall", "Six-Flower Ice Wall", "육화 빙벽", DEFENSE, 18, 18, shield(12, 8), debuff(6, SLOWNESS, 4, 1)));

		weapon("suzumebachi", 3, 54, CLAW, "Suzumebachi", "스즈메바치",
			art(0xE8C040, 0x1A1A1A, 0xF0F0F0), fx(Fx.Kind.SPARK, 0xFFE070),
			skill("sting_all_enemies", "Sting All Enemies to Death", "적을 쏘아 죽여라", ATTACK, 7, 18, shadowstep(8, 1.8), mark(16, 6, 30)),
			skill("nigeki_kessatsu", "Nigeki Kessatsu", "이격결살", ATTACK, 16, 26, execute(4, 2.6, 40, 3.0)),
			skill("onmitsukido", "Onmitsukido", "은밀기동", UTILITY, 18, 16, stealth(4, 60), buff(SPEED, 5, 1)));

		weapon("katen_kyokotsu", 3, 58, TWIN_BLADES, "Katen Kyokotsu", "카텐쿄코츠",
			art(0xD8DCE4, 0x6A2A8A, 0xFF8AC8), fx(Fx.Kind.PETAL, 0xFF8AC8),
			skill("bushogoma", "Bushogoma", "부쇼고마", ATTACK, 8, 18, slash(5, 200, 2.0)),
			skill("kageoni", "Kageoni", "그림자 술래", ATTACK, 12, 22, shadowstep(12, 2.4), stealth(2, 50)),
			skill("takaoni", "Takaoni", "높은 술래", ATTACK, 14, 22, leap(5, 4, 2.4)));

		weapon("shinso", 3, 62, SPEAR, "Shinso", "신창",
			art(0xE8E8E8, 0x8A8A9A, 0x9AA8C0), fx(Fx.Kind.SLASH, 0xE8F0FF),
			skill("shoot_to_kill", "Shoot to Kill, Shinso", "쏘아 죽여라, 신창", ATTACK, 8, 20, beam(20, 2.4)),
			skill("butou_renjin", "Butou Renjin", "무도연인", ATTACK, 14, 24, repeat(5, 3, beam(14, 0.8))),
			skill("fox_smile", "Fox's Smile", "여우의 미소", UTILITY, 18, 16, stance(EVADE, 6, 40), empower(3, 60, 8)));

		weapon("nozarashi", 3, 66, GREATSWORD, "Nozarashi", "노자라시",
			art(0x9AA0A8, 0x3A2A1A, 0xE0E040), fx(Fx.Kind.SHARD, 0xE0E040),
			skill("drink_nozarashi", "Drink, Nozarashi", "마셔라, 노자라시", ATTACK, 10, 22, slash(6, 180, 2.8), push(5, 1.5)),
			skill("kendo", "Kendo", "검도", ATTACK, 14, 24, leap(7, 5, 2.6), stun(1.0)),
			skill("eyepatch_off", "Eyepatch Off", "안대 해제", UTILITY, 24, 20, buff(STRENGTH, 10, 2), empower(6, 50, 10), taunt(10)));

		// ---------------------------------------------------------------- tier 4: Captain-Commander (Bankai)
		weapon("tensa_zangetsu", 4, 70, KATANA, "Tensa Zangetsu", "천쇄참월",
			art(0x1A1A1A, 0x2A1A1A, 0xC8202A), fx(Fx.Kind.SLASH, 0x1A1A1A),
			skill("getsuga_tensho", "Getsuga Tensho", "월아천충", ATTACK, 8, 20, shoot(ProjectileStyle.WAVE, 2.6).pierce(10)),
			skill("bankai_speed", "Bankai Speed", "만해의 속도", MOVEMENT, 10, 16, blink(12), dash(6, 1.8)),
			skill("black_getsuga", "Black Getsuga Tensho", "검은 월아천충", ATTACK, 16, 28, shoot(ProjectileStyle.WAVE, 3.0).count(3).spread(30).pierce(10), inflict(WITHER, 4, 1)),
			skill("hollow_mask", "Hollow Mask", "호로화", ULTIMATE, 50, 40, buff(STRENGTH, 12, 2), buff(SPEED, 12, 1), lifesteal(15), empower(8, 80, 12)));

		weapon("senbonzakura_kageyoshi", 4, 75, KATANA, "Senbonzakura Kageyoshi", "천본앵 경엄",
			art(0xFFE0EC, 0x2A2A3A, 0xFF7AA0), fx(Fx.Kind.PETAL, 0xFF9AB8),
			skill("thousand_blades", "A Thousand Blades", "천 개의 칼날", ATTACK, 10, 24, repeat(6, 4, nova(6, 0.7)), inflict(BLEEDING, 6, 1)),
			skill("gokei", "Gokei", "항경", ATTACK, 14, 26, pull(8, 1.4), delay(0.5, nova(6, 2.4))),
			skill("senkei", "Senkei", "섬경", DEFENSE, 22, 24, stance(GUARD, 8, 50), zone(6, 6, 0.8)),
			skill("shukei_hakuteiken", "Shukei: Hakuteiken", "종경 백제검", ULTIMATE, 55, 45, beam(26, 5.0), dash(8, 2.0)));

		weapon("daiguren_hyorinmaru", 4, 78, KATANA, "Daiguren Hyorinmaru", "대홍련 빙륜환",
			art(0xD0F0FF, 0x1A3A6A, 0x8AE0FF), fx(Fx.Kind.SHARD, 0xBFEFFF),
			skill("ryusenka", "Ryusenka", "용산가", ATTACK, 9, 22, shadowstep(10, 2.4), inflict(SLOWNESS, 5, 3)),
			skill("sennen_hyoro", "Sennen Hyoro", "천년빙뢰", ATTACK, 16, 28, rain(ProjectileStyle.ICICLE, 16, 4, 14, 0.45), inflict(SLOWNESS, 5, 2)),
			skill("ice_wings", "Ice Wings", "얼음 날개", MOVEMENT, 12, 18, leap(8, 3, 1.4), buff(SLOW_FALLING, 6, 0)),
			skill("hyoten_hyakkaso", "Hyoten Hyakkaso", "빙천백화장", ULTIMATE, 55, 45, zone(7, 6, 1.2).at(14).effect(SLOWNESS, 4), stun(2.0)));

		weapon("ryujin_jakka", 4, 82, KATANA, "Ryujin Jakka", "류인약화",
			art(0x6A5A4A, 0x2A1A0A, 0xFF5A1A), fx(Fx.Kind.ORB, 0xFF5A1A),
			skill("all_things_to_ash", "Reduce All Creation to Ash", "삼라만상 모두 재로 만들어라", ATTACK, 10, 24, nova(6, 2.0), burn(6)),
			skill("jokaku_enjo", "Jokaku Enjo", "성곽염상", DEFENSE, 22, 26, zone(6, 6, 1.0), shield(14, 6)),
			skill("taimatsu", "Taimatsu", "송명", ATTACK, 14, 26, shoot(ProjectileStyle.FIREBALL, 3.0).explode(4.0F), burn(6)),
			skill("zanka_no_tachi", "Zanka no Tachi", "잔화의 태도", ULTIMATE, 60, 50, slash(8, 360, 5.5), burn(8)));

		weapon("kamishini_no_yari", 4, 85, SPEAR, "Kamishini no Yari", "신살창",
			art(0xF0F0F0, 0x8A8A9A, 0xC8D0E0), fx(Fx.Kind.SLASH, 0xE8F0FF),
			skill("thirteen_kilometers", "Thirteen Kilometers", "13킬로미터", ATTACK, 10, 24, beam(30, 2.8)),
			skill("butou_renjin_kai", "Butou Renjin", "무도연인", ATTACK, 14, 26, repeat(6, 3, beam(20, 0.9))),
			skill("dust_to_dust", "Dust to Dust", "먼지로", UTILITY, 18, 20, inflict(WITHER, 6, 2), mark(20, 8, 40)),
			skill("korose", "Korose, Kamishini no Yari", "죽여라, 신살창", ULTIMATE, 55, 45, beam(30, 4.0), inflict(WITHER, 8, 3)));

		weapon("kyoka_suigetsu", 4, 88, KATANA, "Kyoka Suigetsu", "경화수월",
			art(0xE8E4F0, 0x3A2A5A, 0x8A5AFF), fx(Fx.Kind.RUNE, 0xB08AFF),
			skill("complete_hypnosis", "Complete Hypnosis", "완전최면", UTILITY, 20, 22, stealth(5, 80), debuff(8, BLINDNESS, 5, 0)),
			skill("hado_90_kurohitsugi", "Hado #90: Kurohitsugi", "파도 90번 흑관", ATTACK, 18, 32, zone(4, 3, 1.6).at(16).pull(0.8), inflict(VULNERABLE, 6, 1)),
			skill("illusion_step", "Illusion Step", "환영 보법", MOVEMENT, 10, 16, blink(12), stance(EVADE, 3, 60)),
			skill("hogyoku", "Hogyoku", "붕옥", ULTIMATE, 60, 50, buff(STRENGTH, 15, 2), buff(RESISTANCE, 15, 1), restoreMana(40), nova(6, 3.0)));

		weapon("karamatsu_shinju", 4, 92, TWIN_BLADES, "Katen Kyokotsu: Karamatsu Shinju", "카텐쿄코츠 낙엽송심중",
			art(0xC8C0D8, 0x4A2A6A, 0xFF5AA0), fx(Fx.Kind.PETAL, 0xFF5AA0),
			skill("first_act", "First Act: Tragedy of Wounds Shared", "1단: 상처를 나누는 비극", ATTACK, 12, 24, lifesteal(25), repeat(3, 6, slash(5, 200, 1.0))),
			skill("second_act", "Second Act: Shame of Wounds Undone", "2단: 낫지 않는 상처", ATTACK, 14, 26, inflict(BLEEDING, 8, 2), inflict(WEAKNESS, 8, 1), slash(4, 160, 1.6)),
			skill("third_act", "Third Act: Drowning Abyss", "3단: 빠져드는 심연", UTILITY, 18, 24, zone(6, 5, 0.8).at(10).pull(1.0), stun(1.5)),
			skill("final_act", "Final Act: Itosabaki", "종단: 실 끊기", ULTIMATE, 60, 50, execute(6, 4.0, 50, 3.0), inflict(STUN, 2, 0)));

		weapon("true_zangetsu", 4, 100, GREATSWORD, "True Zangetsu", "진짜 참월",
			art(0xE8E8F0, 0x1A1A1A, 0x3AA0FF), fx(Fx.Kind.SLASH, 0x6AC8FF),
			skill("true_getsuga", "Getsuga Tensho", "월아천충", ATTACK, 8, 22, shoot(ProjectileStyle.WAVE, 3.6).speed(2.2F).pierce(12)),
			skill("twin_getsuga", "Twin Getsuga", "쌍 월아천충", ATTACK, 14, 28, shoot(ProjectileStyle.WAVE, 3.0).count(2).parallel(1.8).speed(2.2F).pierce(12),
				slash(5, 180, 2.0)),
			skill("final_flash_step", "Final Flash Step", "최후의 순보", MOVEMENT, 10, 18, blink(14), empower(3, 80, 6)),
			skill("mugetsu", "Mugetsu", "무월", ULTIMATE, 90, 60, zone(8, 4, 2.0).at(12), beam(30, 6.0)));
	}
}
