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
 * Hunter (Hunter x Hunter): Hunter -> Nen User -> Pro Hunter -> Triple-Star Hunter. Nen in every
 * category: Gon's Jajanken, Killua's yo-yos, claws and Godspeed, Kurapika's chains, the Phantom
 * Troupe (Hisoka's cards, Franklin's fingers, Nobunaga's iai, Feitan's umbrella, Machi's threads,
 * Uvogin's fists, Chrollo's Skill Hunter), Kite's Crazy Slots, Morel's smoke, the Chimera Ants
 * (Neferpitou, Meruem), the Zoldyck elders (Zeno, Silva) and Chairman Netero.
 */
public final class HunterContent extends ClassContent {
	public HunterContent() {
		super(JobClass.HUNTER);
	}

	@Override
	public void define() {
		// ---------------------------------------------------------------- tier 1: Hunter
		weapon("gon_fishing_rod", 1, 10, SPEAR, "Whale Island Fishing Rod", "고래섬의 낚싯대",
			art(0xB89060, 0x3A6A2A, 0xE8E8E8), fx(Fx.Kind.SPARK, 0x7AD07A),
			skill("rod_lash", "Rod Lash", "낚싯대 휘두르기", ATTACK, 6, 10, beam(7, 1.4)),
			skill("reel_in", "Reel In", "낚아채기", UTILITY, 10, 10, pull(6, 1.2), stun(0.5)),
			skill("wild_senses", "Wild Senses", "야생의 감각", UTILITY, 16, 10, buff(SPEED, 6, 0), stance(EVADE, 4, 30)));

		weapon("exam_knife", 1, 15, DAGGER, "Hunter Exam Knife", "헌터 시험의 나이프",
			art(0xC8CCD4, 0x4A3A2A, 0xF5862B), fx(Fx.Kind.SLASH, 0xF5C08A),
			skill("survival_cut", "Survival Cut", "생존 베기", ATTACK, 5, 8, repeat(2, 4, slash(3, 100, 0.9))),
			skill("swamp_dash", "Numere Wetlands Dash", "누메레 습원 질주", MOVEMENT, 10, 8, dash(7, 1.2), buff(SPEED, 3, 1)),
			skill("trick_tower", "Trick Tower Ambush", "트릭 타워 매복", ATTACK, 12, 12, shadowstep(7, 1.6), inflict(BLEEDING, 4, 0)));

		weapon("killua_yoyo", 1, 20, KUSARIGAMA, "Killua's Yo-yos", "키르아의 요요",
			art(0xC8D0E0, 0x2A2A3A, 0x5AA0FF), fx(Fx.Kind.RING, 0x9AC8FF),
			skill("yoyo_strike", "Yo-yo Strike", "요요 강타", ATTACK, 6, 10, beam(8, 1.5), stun(0.4)),
			skill("twin_yoyo", "Twin Yo-yos", "쌍 요요", ATTACK, 9, 12, slash(6, 220, 1.2)),
			skill("assassin_step", "Assassin's Step", "암살자의 걸음", MOVEMENT, 14, 10, stealth(2, 40), dash(6, 1.0)));

		// ---------------------------------------------------------------- tier 2: Nen User
		weapon("jajanken", 2, 26, KNUCKLE, "Jajanken", "가위바위보",
			art(0x3A8A3A, 0xE8E0D0, 0xF5D040), fx(Fx.Kind.ORB, 0xF5D040),
			skill("rock", "Jajanken: Rock", "가위바위보: 바위", ATTACK, 8, 16, empower(1, 120, 6), slash(3, 80, 2.4)),
			skill("scissors", "Jajanken: Scissors", "가위바위보: 가위", ATTACK, 9, 16, beam(8, 1.8), inflict(BLEEDING, 4, 0)),
			skill("paper", "Jajanken: Paper", "가위바위보: 보", ATTACK, 10, 16, shoot(ProjectileStyle.ORB, 1.8).explode(2.0F)));

		weapon("hisoka_cards", 2, 29, SHURIKEN, "Hisoka's Trump Cards", "히소카의 트럼프 카드",
			art(0xF0F0F0, 0xE04080, 0x8A2AC8), fx(Fx.Kind.PETAL, 0xFF6AC8),
			skill("card_throw", "Card Throw", "카드 투척", ATTACK, 6, 12, shoot(ProjectileStyle.SHURIKEN, 1.2).count(4).spread(24)),
			skill("bungee_gum", "Bungee Gum", "번지 검", UTILITY, 12, 14, pull(8, 1.6), inflict(SLOWNESS, 4, 2)),
			skill("texture_surprise", "Texture Surprise", "얇은 거짓말", UTILITY, 16, 14, stealth(3, 50), stance(EVADE, 4, 40)));

		weapon("killua_claws", 2, 32, CLAW, "Killua's Claws", "키르아의 손톱",
			art(0xE0E4EA, 0xD8D8E0, 0x5AA0FF), fx(Fx.Kind.BOLT, 0x9AD0FF),
			skill("heart_snatch", "Heart Snatch", "심장 뽑기", ATTACK, 12, 16, execute(3, 2.0, 30, 2.5)),
			skill("rhythm_echo", "Rhythm Echo", "리듬 에코", MOVEMENT, 12, 14, shadowstep(8, 1.4), stealth(1.5, 40)),
			skill("lightning_palm", "Lightning Palm", "낙뢰", ATTACK, 10, 18, lightning(10, 2.5, 1.8), stun(0.8)));

		weapon("kurapika_chains", 2, 36, KUSARIGAMA, "Kurapika's Chains", "쿠라피카의 사슬",
			art(0xE8E8F0, 0x6A5A3A, 0xE8C040), fx(Fx.Kind.SPARK, 0xF0D080),
			skill("dowsing_chain", "Dowsing Chain", "다우징 체인", UTILITY, 12, 12, mark(16, 8, 25)),
			skill("chain_jail", "Chain Jail", "체인 제일", ATTACK, 14, 20, beam(10, 1.4), stun(2.0)),
			skill("holy_chain", "Holy Chain", "홀리 체인", DEFENSE, 18, 16, heal(20), cleanse()));

		weapon("franklin_fingers", 2, 40, PISTOL, "Franklin's Double Machine Gun", "프랭클린의 더블 머신건",
			art(0x8A8A90, 0x5A4A3A, 0xF5862B), fx(Fx.Kind.BOLT, 0xF5B060),
			skill("ten_fingers", "Ten Fingers", "열 손가락", ATTACK, 10, 18, shoot(ProjectileStyle.BULLET, 0.6).count(10).spread(40).interval(2)),
			skill("nen_bullet", "Nen Bullet", "념탄", ATTACK, 8, 14, shoot(ProjectileStyle.BULLET, 2.0).pierce(3)),
			skill("hold_the_line", "Hold the Line", "진지 사수", DEFENSE, 18, 14, stance(GUARD, 5, 40), shield(8, 6)));

		weapon("nobunaga_katana", 2, 43, KATANA, "Nobunaga's Katana", "노부나가의 일본도",
			art(0xE0E4EA, 0x1A1A24, 0x8A2A2A), fx(Fx.Kind.SLASH, 0xF0E0E0),
			skill("en_iai", "En: Iai", "원: 거합", ATTACK, 10, 18, stance(COUNTER, 3, 60), slash(4.5, 220, 2.0)),
			skill("troupe_slash", "Troupe Slash", "여단의 참격", ATTACK, 7, 14, dash(6, 1.6)),
			skill("spider_tattoo", "Spider Tattoo", "거미 문신", UTILITY, 20, 14, buff(STRENGTH, 8, 0), empower(3, 40, 8)));

		// ---------------------------------------------------------------- tier 3: Pro Hunter
		weapon("feitan_umbrella", 3, 46, RAPIER, "Feitan's Umbrella", "페이탄의 우산",
			art(0x2A2A30, 0x1A1A1A, 0xC82020), fx(Fx.Kind.SPARK, 0xFF4A2A),
			skill("hidden_blade", "Hidden Blade", "우산 속 칼날", ATTACK, 7, 18, repeat(4, 3, beam(6, 0.7))),
			skill("pain_packer", "Pain Packer", "페인 패커", DEFENSE, 20, 20, stance(COUNTER, 4, 80), shield(10, 4)),
			skill("rising_sun", "Rising Sun", "라이징 선", ATTACK, 18, 28, nova(7, 2.6), burn(8)));

		weapon("machi_threads", 3, 50, KUNAI, "Machi's Nen Threads", "마치의 념사",
			art(0xD8D8E0, 0xC8A0C8, 0xFF8AC8), fx(Fx.Kind.SPARK, 0xFFB0E0),
			skill("needle_volley", "Needle Volley", "바늘 연사", ATTACK, 7, 16, shoot(ProjectileStyle.KUNAI, 1.2).count(5).spread(20)),
			skill("thread_snare", "Thread Snare", "념사 올가미", UTILITY, 12, 16, debuff(7, SLOWNESS, 4, 3), pull(7, 0.8)),
			skill("nen_suture", "Nen Suture", "념사 봉합", DEFENSE, 20, 18, heal(25), allyHeal(8, 15)));

		weapon("uvogin_fists", 3, 54, KNUCKLE, "Uvogin's Fists", "우보긴의 주먹",
			art(0xC8A070, 0x5A3A1A, 0xFF5A2A), fx(Fx.Kind.RING, 0xFF8A4A),
			skill("big_bang_impact", "Big Bang Impact", "빅뱅 임팩트", ATTACK, 14, 26, slash(4, 90, 3.6), launch(4, 1.0)),
			skill("roar", "Roar", "포효", UTILITY, 16, 14, taunt(10), debuff(8, WEAKNESS, 5, 1)),
			skill("ken", "Ken", "견", DEFENSE, 18, 16, buff(RESISTANCE, 6, 1), stance(GUARD, 6, 40)));

		weapon("crazy_slots", 3, 58, SCYTHE, "Crazy Slots", "크레이지 슬롯",
			art(0xD8DCE4, 0x6A2A8A, 0xF5D040), fx(Fx.Kind.COIN, 0xF5D040),
			skill("reaper_scythe", "Reaper Scythe", "사신의 낫", ATTACK, 8, 20, slash(5, 300, 2.0)),
			skill("silent_waltz", "Silent Waltz", "침묵의 왈츠", ATTACK, 12, 22, repeat(3, 8, nova(4.5, 1.0)), inflict(BLEEDING, 5, 1)),
			skill("slot_spin", "Slot Spin", "슬롯 돌리기", UTILITY, 20, 16, refresh(30), restoreMana(15)));

		weapon("neferpitou_claws", 3, 62, CLAW, "Neferpitou's Claws", "네페르피토의 발톱",
			art(0xF0F0F0, 0xE8E0F0, 0x8A2AC8), fx(Fx.Kind.SLASH, 0xC8A0FF),
			skill("pounce", "Pounce", "도약 습격", MOVEMENT, 9, 18, leap(5, 3.5, 2.2)),
			skill("terpsichora", "Terpsichora", "테르프시코라", UTILITY, 22, 20, buff(STRENGTH, 8, 2), buff(SPEED, 8, 1)),
			skill("doctor_blythe", "Doctor Blythe", "닥터 블라이스", DEFENSE, 24, 24, heal(35), cleanse(), allyHeal(10, 20)));

		weapon("morel_pipe", 3, 66, STAFF, "Morel's Pipe", "모라우의 담뱃대",
			art(0x6A4A2A, 0x3A2A1A, 0xB0B0C0), fx(Fx.Kind.SMOKE, 0xC8C8D0),
			skill("deep_purple", "Deep Purple", "딥 퍼플", ATTACK, 16, 24, summon(Summon.SNOW_GOLEM, 3, 20), debuff(6, BLINDNESS, 3, 0)),
			skill("smoke_prison", "Smoke Prison", "연기 감옥", ATTACK, 14, 22, zone(4, 4, 0.8).at(10).effect(BLINDNESS, 0), stun(1.0)),
			skill("smoke_screen", "Smoke Screen", "연막", UTILITY, 18, 16, stealth(4, 50), debuff(6, BLINDNESS, 4, 0)));

		// ---------------------------------------------------------------- tier 4: Triple-Star Hunter
		weapon("godspeed", 4, 72, CLAW, "Godspeed", "신속",
			art(0xE8F4FF, 0xD8E8FF, 0x3AA0FF), fx(Fx.Kind.BOLT, 0x8AD0FF),
			skill("speed_of_lightning", "Speed of Lightning", "전광석화", MOVEMENT, 8, 18, shadowstep(12, 2.2), buff(SPEED, 4, 2)),
			skill("whirlwind", "Whirlwind", "질풍신뢰", DEFENSE, 20, 22, stance(COUNTER, 6, 100), stance(EVADE, 6, 30)),
			skill("thunderbolt", "Thunderbolt", "낙뢰", ATTACK, 12, 26, lightning(16, 3, 2.6), stun(1.2)),
			skill("narukami", "Narukami", "나루카미", ULTIMATE, 50, 45, chain(14, 6, 3.4), lightning(14, 4, 3.0)));

		weapon("emperor_time", 4, 75, KUSARIGAMA, "Emperor Time", "엠퍼러 타임",
			art(0xF0F0F8, 0x8A1A1A, 0xFF2A2A), fx(Fx.Kind.SPARK, 0xFF4A4A),
			skill("judgment_chain", "Judgment Chain", "저지먼트 체인", ATTACK, 14, 24, beam(14, 2.4), inflict(VULNERABLE, 8, 1), stun(1.5)),
			skill("steal_chain", "Steal Chain", "스틸 체인", UTILITY, 18, 20, inflict(WEAKNESS, 8, 2), restoreMana(20)),
			skill("chain_jail_kai", "Chain Jail", "체인 제일", ATTACK, 16, 26, zone(4, 3, 0.8).at(12).pull(1.0), stun(2.5)),
			skill("scarlet_eyes", "Scarlet Eyes", "주홍 눈", ULTIMATE, 60, 40, empower(10, 100, 15), buff(SPEED, 15, 1), refresh(50)));

		weapon("adult_gon", 4, 78, KNUCKLE, "Adult Gon", "어른 곤",
			art(0x2A6A2A, 0xE8E0D0, 0xF5D040), fx(Fx.Kind.ORB, 0xFFE070),
			skill("bonds_rage", "Rage of the Bond", "분노의 서약", UTILITY, 22, 20, buff(STRENGTH, 10, 3), lifesteal(10)),
			skill("first_comes_rock", "First Comes Rock", "최초는 주먹", ATTACK, 10, 26, leap(6, 4, 2.8), launch(4, 1.0)),
			skill("long_scissors", "Long Scissors", "긴 가위", ATTACK, 12, 26, beam(18, 2.6)),
			skill("thats_all_i_need", "This Is All I Need", "이것으로 끝이라도 좋아", ULTIMATE, 75, 60, delay(1.0, nova(7, 6.0)), stun(2.0)));

		weapon("skill_hunter", 4, 82, GRIMOIRE, "Skill Hunter", "도적의 극의",
			art(0x3A2A1A, 0x1A1A1A, 0xC8A040), fx(Fx.Kind.RUNE, 0xE0C080),
			skill("sun_and_moon", "Sun and Moon", "태양과 달", ATTACK, 12, 24, mark(18, 8, 40), delay(1.5, chain(14, 4, 2.2))),
			skill("indoor_fish", "Indoor Fish", "실내 물고기", ATTACK, 14, 24, shoot(ProjectileStyle.ORB, 1.6).count(3).spread(30).homing(), inflict(BLEEDING, 6, 1)),
			skill("fun_fun_cloth", "Fun Fun Cloth", "펀펀 클로스", DEFENSE, 20, 22, shield(16, 8), stealth(2, 30)),
			skill("bandits_secret", "Bandit's Secret", "도적의 비법", ULTIMATE, 55, 50, barrage(ProjectileStyle.ORB, 14, 1.4), restoreMana(30)));

		weapon("zeno_dragon", 4, 85, SCEPTER, "Zeno's Dragon Head", "제노의 용두",
			art(0xE8E0C0, 0x5A3A2A, 0x4AC8FF), fx(Fx.Kind.ORB, 0x8AE0FF),
			skill("dragon_head", "Dragon Head", "용두", ATTACK, 12, 26, shoot(ProjectileStyle.WAVE, 2.4).pierce(10).explode(2.5F)),
			skill("dragon_lance", "Dragon Lance", "용두쌍", ATTACK, 14, 26, beam(20, 2.6), stun(1.0)),
			skill("zoldyck_patience", "Zoldyck Patience", "조르딕가의 인내", DEFENSE, 20, 18, stance(GUARD, 6, 50), buff(RESISTANCE, 6, 1)),
			skill("dragon_dive", "Dragon Dive", "용성군", ULTIMATE, 60, 50, rain(ProjectileStyle.ORB, 24, 7, 30, 1.6, 1.5F)));

		weapon("silva_orbs", 4, 88, ORB, "Silva's Nen Orbs", "실바의 념구",
			art(0xE8E8F0, 0x5A5A6A, 0xF5862B), fx(Fx.Kind.ORB, 0xFFB060),
			skill("aura_orb", "Aura Orb", "오라 구", ATTACK, 8, 20, shoot(ProjectileStyle.ORB, 2.2).explode(3.0F)),
			skill("orb_volley", "Orb Volley", "념구 연사", ATTACK, 14, 26, shoot(ProjectileStyle.ORB, 1.4).count(6).spread(40).explode(1.5F)),
			skill("head_of_the_family", "Head of the Family", "가주의 위엄", UTILITY, 20, 18, debuff(10, SLOWNESS, 5, 2), taunt(10), buff(RESISTANCE, 5, 1)),
			skill("assassins_judgment", "Assassin's Judgment", "암살가의 심판", ULTIMATE, 55, 45, execute(8, 4.0, 40, 3.0), shoot(ProjectileStyle.ORB, 3.0).count(3).spread(25).explode(3.0F)));

		weapon("meruem_tail", 4, 92, SPEAR, "Meruem's Tail", "메르엠의 꼬리",
			art(0x6A8A3A, 0x2A3A1A, 0xC8F070), fx(Fx.Kind.SLASH, 0xC8F070),
			skill("tail_pierce", "Tail Pierce", "꼬리 관통", ATTACK, 8, 22, beam(10, 2.8)),
			skill("photon", "Photon", "광자", ATTACK, 14, 28, nova(6, 2.6), inflict(VULNERABLE, 6, 1)),
			skill("kings_aura", "King's Aura", "왕의 오라", UTILITY, 24, 22, debuff(12, WEAKNESS, 6, 2), debuff(12, SLOWNESS, 6, 1), lifesteal(15)),
			skill("king_of_ants", "King of the Chimera Ants", "키메라 앤트의 왕", ULTIMATE, 70, 55, buff(STRENGTH, 15, 3), buff(RESISTANCE, 15, 2), heal(50), nova(7, 3.0)));

		weapon("hundred_type_guanyin", 4, 100, KNUCKLE, "Hundred-Type Guanyin Bodhisattva", "백식관음",
			art(0xF0D890, 0xE8E0D0, 0xFFE070), fx(Fx.Kind.RING, 0xFFE070),
			skill("prayer", "Prayer", "기도", DEFENSE, 14, 20, stance(GUARD, 3, 60), restoreMana(15), heal(10)),
			skill("third_hand", "Third Hand", "3의 손", ATTACK, 10, 26, slash(8, 200, 3.0), launch(6, 1.0)),
			skill("ninety_ninth_hand", "Ninety-Ninth Hand", "99의 손", ATTACK, 18, 34, repeat(6, 4, nova(7, 1.2)), stun(1.5)),
			skill("zero_hand", "Zero Hand", "제로의 손", ULTIMATE, 90, 60, zone(9, 3, 3.0).at(6), beam(30, 6.0)));
	}
}
