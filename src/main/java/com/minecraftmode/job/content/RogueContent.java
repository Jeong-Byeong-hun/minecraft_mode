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
 * Rogue: Thief -> Ninja -> Assassin -> Shadow Monarch. Daggers, claws, ninjato, kusarigama and thrown
 * shuriken/kunai built on stealth, shadowsteps, marks, executes, poison and bleeding. Tier 1 is street
 * thieves (pickpockets, burglars, a phantom thief); tier 2 borrows from Naruto (explosive tags, Fuma
 * shuriken, shadow clones, Chidori, Rasengan); tier 3 from Fate's Assassins (Hassan's Zabaniya, Sasaki
 * Kojiro's Tsubame Gaeshi, Jack the Ripper), Assassin's Creed, Akame ga Kill's Murasame and Tsukihime's
 * Nanaya; tier 4 from Solo Leveling (Kasaka's Venom Fang, Knight Killer, Beru, Monarch's Domain,
 * Ruler's Authority, Kamish's Wrath and "Arise") plus the legendary shinobi Minato, Itachi and Kakashi.
 */
public final class RogueContent extends ClassContent {
	public RogueContent() {
		super(JobClass.ROGUE);
	}

	@Override
	public void define() {
		// ---------------------------------------------------------------- tier 1: Thief
		weapon("pickpocket_shiv", 1, 10, DAGGER, "Pickpocket's Shiv", "소매치기의 단검",
			art(0xA8ADB4, 0x4A3A2E, 0x8E44AD), fx(Fx.Kind.SMOKE, 0x7D6B91),
			skill("backstab", "Backstab", "뒤치기", ATTACK, 7, 10, shadowstep(8, 1.6)),
			skill("pickpocket", "Pickpocket", "소매치기", UTILITY, 12, 8, loot(60), slash(3, 90, 0.8), buff(SPEED, 4, 0)),
			skill("smoke_bomb", "Smoke Bomb", "연막탄", DEFENSE, 16, 12, debuff(4, BLINDNESS, 3, 0), stealth(4, 50)));

		weapon("burglar_kusarigama", 1, 16, KUSARIGAMA, "Burglar's Kusarigama", "밤도둑의 쇄겸",
			art(0x9A9EA6, 0x5C4632, 0xC9A227), fx(Fx.Kind.SHARD, 0xB8BCC8),
			skill("snag_and_reel", "Snag and Reel", "걸어 당기기", UTILITY, 10, 10, pull(6, 1.3), delay(0.35, slash(3.5, 140, 1.2)), inflict(SLOWNESS, 3, 0)),
			skill("chain_whirl", "Chain Whirl", "사슬 휘돌리기", ATTACK, 8, 12, repeat(2, 6, nova(4, 0.9))),
			skill("hook_and_line", "Hook and Line", "갈고리 줄타기", MOVEMENT, 9, 8, grapple(16)));

		weapon("phantom_thief_claws", 1, 21, CLAW, "Phantom Thief's Claws", "괴도의 발톱",
			art(0xE6E9EE, 0x1F2A44, 0x3D7BFF), fx(Fx.Kind.SPARK, 0xDDE6FF),
			skill("calling_card", "Calling Card", "예고장", UTILITY, 14, 10, mark(14, 8, 25), empower(2, 30, 8)),
			skill("rapid_scratch", "Rapid Scratch", "연속 할퀴기", ATTACK, 8, 12, repeat(4, 3, slash(3, 100, 0.6)), inflict(BLEEDING, 4, 0)),
			skill("rooftop_escape", "Rooftop Escape", "지붕 위 도주", MOVEMENT, 11, 10, leap(5, 2.5, 0.8), buff(SPEED, 5, 1)));

		// ---------------------------------------------------------------- tier 2: Ninja
		weapon("leaf_kunai", 2, 25, KUNAI, "Hidden Leaf Kunai", "나뭇잎 마을 쿠나이",
			art(0x5E6670, 0x2F3A4A, 0xF28C28), fx(Fx.Kind.SMOKE, 0xE6E6E6),
			skill("explosive_tag", "Explosive Tag Kunai", "기폭찰 쿠나이", ATTACK, 9, 14, shoot(ProjectileStyle.KUNAI, 1.6).explode(2.5F), burn(3)),
			skill("shadow_clone", "Shadow Clone Jutsu", "그림자 분신술", UTILITY, 24, 18, summon(Summon.WOLF, 2, 10), barrage(ProjectileStyle.KUNAI, 6, 0.5)),
			skill("substitution", "Substitution Jutsu", "바꿔치기술", DEFENSE, 15, 12, cleanse(), blink(8), stance(EVADE, 4, 40)));

		weapon("fuma_shuriken", 2, 28, SHURIKEN, "Fuma Shuriken", "풍마수리검",
			art(0x7F8C8D, 0x34495E, 0xC0392B), fx(Fx.Kind.SPARK, 0xC8D0D8),
			skill("shadow_windmill", "Shadow Windmill", "영풍차", ATTACK, 8, 14, shoot(ProjectileStyle.SHURIKEN, 1.4).count(2).interval(4).pierce(4)),
			skill("shuriken_shadow_clone", "Shuriken Shadow Clone", "수리검 그림자 분신술", ATTACK, 14, 18, barrage(ProjectileStyle.SHURIKEN, 10, 0.4)),
			skill("gale_step", "Gale Step", "질풍보", MOVEMENT, 10, 12, dash(8, 1.0), buff(SPEED, 5, 1)));

		weapon("chakra_blades", 2, 31, CLAW, "Chakra Blades", "차크라 칼날",
			art(0xA9B3BC, 0x2C2C2C, 0x5DADE2), fx(Fx.Kind.SLASH, 0x9FE2FF),
			skill("flying_swallow", "Wind Release: Flying Swallow", "풍둔 비연", ATTACK, 7, 14, slash(6, 110, 1.8)),
			skill("ash_pile_burning", "Fire Release: Ash Pile Burning", "화둔 회적소", ATTACK, 15, 18, zone(4, 4, 0.6).at(10).effect(BLINDNESS, 0), burn(3)),
			skill("body_flicker", "Body Flicker", "순신술", MOVEMENT, 9, 12, blink(10), empower(2, 40, 5)));

		weapon("iga_kusarigama", 2, 35, KUSARIGAMA, "Iga Kusarigama", "이가류 쇄겸",
			art(0x8A9199, 0x3B2F2F, 0x7D3C98), fx(Fx.Kind.SHARD, 0x9B7FB8),
			skill("chain_snare", "Chain Snare", "사슬 포박", UTILITY, 12, 14, pull(7, 1.5), delay(0.35, nova(3.5, 1.3)), stun(1.0)),
			skill("reaping_whirlwind", "Reaping Whirlwind", "수확의 회오리", ATTACK, 10, 16, repeat(3, 5, nova(4.5, 0.8)), inflict(BLEEDING, 4, 0)),
			skill("chain_swing", "Chain Swing", "사슬 그네", MOVEMENT, 8, 12, grapple(20), empower(2, 50, 4)));

		weapon("chidori_blade", 2, 39, NINJATO, "Chidori Blade", "치도리 도",
			art(0xD6E4F0, 0x1C1C3C, 0x6EC6FF), fx(Fx.Kind.BOLT, 0x8FD3FF),
			skill("chidori", "Chidori", "치도리", ATTACK, 9, 18, dash(8, 2.0), stun(0.8)),
			skill("chidori_current", "Chidori Current", "치도리 나가시", DEFENSE, 14, 16, nova(3.5, 1.2), stun(1.2)),
			skill("sharingan", "Sharingan", "사륜안", UTILITY, 18, 14, stance(EVADE, 6, 40), mark(16, 8, 25)));

		weapon("rasenshuriken", 2, 43, SHURIKEN, "Rasenshuriken", "나선수리검",
			art(0xE8F6FF, 0x2E86C1, 0x00E5FF), fx(Fx.Kind.ORB, 0x7FDBFF),
			skill("rasengan", "Rasengan", "나선환", ATTACK, 10, 16, dash(5, 1.0), delay(0.3, nova(3, 1.8)), delay(0.3, push(4, 1.6))),
			skill("wind_rasenshuriken", "Wind Release: Rasenshuriken", "풍둔 나선수리검", ATTACK, 16, 20,
				shoot(ProjectileStyle.SHURIKEN, 2.0).speed(1.4F).explode(3.5F), inflict(BLEEDING, 5, 1)),
			skill("sage_mode", "Sage Mode", "선인 모드", UTILITY, 26, 12, buff(STRENGTH, 10, 1), buff(RESISTANCE, 10, 0), restoreMana(15)));

		// ---------------------------------------------------------------- tier 3: Assassin
		weapon("hidden_blade", 3, 45, CLAW, "Hidden Blade", "히든 블레이드",
			art(0xC8CCD2, 0xEDE6D6, 0xA31515), fx(Fx.Kind.SLASH, 0xF2F2F2),
			skill("silent_assassination", "Silent Assassination", "소리 없는 암살", ATTACK, 10, 20, execute(6, 2.0, 40, 2.2), stealth(2, 40)),
			skill("eagle_vision", "Eagle Vision", "독수리의 눈", UTILITY, 18, 16, debuff(20, GLOWING, 10, 0), mark(20, 10, 30)),
			skill("leap_of_faith", "Leap of Faith", "신념의 도약", MOVEMENT, 12, 16, leap(7, 3, 1.6), stealth(3, 30)));

		weapon("hassan_dirks", 3, 49, KUNAI, "Hassan's Dirks", "하산의 단검",
			art(0x3A3A40, 0x1A1A1A, 0xEDEDED), fx(Fx.Kind.SMOKE, 0x3B2A4D),
			skill("delusional_heartbeat", "Zabaniya: Delusional Heartbeat", "자바니야: 망상심음", ATTACK, 16, 26, execute(14, 2.2, 50, 2.0), inflict(WITHER, 5, 1)),
			skill("delusional_illusion", "Zabaniya: Delusional Illusion", "자바니야: 망상환상", ATTACK, 14, 24, barrage(ProjectileStyle.KNIFE, 12, 0.4), inflict(POISON, 4, 1)),
			skill("presence_concealment", "Presence Concealment", "기척 차단", DEFENSE, 20, 18, stealth(6, 70), buff(SPEED, 6, 1)));

		weapon("monohoshizao", 3, 53, NINJATO, "Monohoshizao", "모노호시자오",
			art(0xE6EEF5, 0x3E2C5A, 0xC9A0DC), fx(Fx.Kind.FEATHER, 0xB8C4FF),
			skill("tsubame_gaeshi", "Secret Sword: Tsubame Gaeshi", "비검 츠바메가에시", ATTACK, 12, 24, repeat(3, 1, slash(5, 100, 1.3))),
			skill("minds_eye_fake", "Mind's Eye (Fake)", "심안(가)", DEFENSE, 18, 18, stance(EVADE, 6, 50), empower(2, 50, 6)),
			skill("gatekeeper_stride", "Gatekeeper's Stride", "산문지기의 보법", MOVEMENT, 10, 16, dash(9, 1.4), delay(0.3, slash(4, 200, 1.2))));

		weapon("ripper_knives", 3, 57, DAGGER, "Jack the Ripper's Knives", "잭 더 리퍼의 나이프",
			art(0xB0B6BE, 0x2A1A2E, 0xC2185B), fx(Fx.Kind.SMOKE, 0x5A4A5E),
			skill("maria_the_ripper", "Maria the Ripper", "해체 성모", ATTACK, 16, 26, shadowstep(14, 1.6), repeat(4, 2, slash(3, 360, 0.6)), inflict(BLEEDING, 6, 1)),
			skill("the_mist", "The Mist", "암흑무도", UTILITY, 22, 20, zone(5, 6, 0.4).effect(WEAKNESS, 0), stealth(4, 50)),
			skill("surgical_procedure", "Surgical Procedure", "외과 수술", DEFENSE, 24, 18, heal(25), cleanse()));

		weapon("murasame", 3, 61, NINJATO, "One-Cut Killer: Murasame", "일참필살 무라사메",
			art(0x2A2A2E, 0x7B0F1A, 0xD10A2A), fx(Fx.Kind.SLASH, 0xB0001E),
			skill("eliminate", "Eliminate", "처단", ATTACK, 14, 24, execute(6, 2.0, 50, 2.4), inflict(POISON, 6, 2)),
			skill("cursed_pattern", "Cursed Pattern", "저주의 문양", ATTACK, 9, 18, slash(5, 160, 1.6), inflict(WITHER, 6, 1), inflict(VULNERABLE, 6, 0)),
			skill("trump_card", "Trump Card", "비장의 수", UTILITY, 28, 22, buff(STRENGTH, 8, 2), buff(SPEED, 8, 1), empower(4, 60, 8)));

		weapon("nanatsu_yoru", 3, 66, DAGGER, "Nanatsu-Yoru", "칠야",
			art(0xD8D8E0, 0x5B3A1E, 0x2E86DE), fx(Fx.Kind.SLASH, 0x2E86DE),
			skill("mystic_eyes", "Mystic Eyes of Death Perception", "직사의 마안", UTILITY, 18, 20, mark(20, 10, 40), debuff(10, VULNERABLE, 6, 1)),
			skill("seventeen_dismemberment", "Seventeen Dismemberment", "17분할", ATTACK, 16, 28, shadowstep(10, 1.4), repeat(17, 1, slash(3, 120, 0.2))),
			skill("flash_run", "Nanaya Arts: Flash Run", "칠야 비전: 섬주", MOVEMENT, 9, 16, blink(12), slash(3.5, 360, 1.4)));

		// ---------------------------------------------------------------- tier 4: Shadow Monarch
		weapon("kasaka_venom_fang", 4, 70, DAGGER, "Kasaka's Venom Fang", "카사카의 독니",
			art(0xE8E4D0, 0x1F4D3A, 0x76FF03), fx(Fx.Kind.BUBBLE, 0x7CFC00),
			skill("serpent_lair", "Great Serpent's Lair", "거대 독사의 소굴", ULTIMATE, 45, 38, nova(6, 3.4), inflict(POISON, 8, 2), stun(1.0)),
			skill("paralytic_bite", "Paralytic Bite", "마비의 이빨", ATTACK, 9, 20, shadowstep(12, 2.0), stun(1.5), inflict(POISON, 5, 1)),
			skill("venom_mist", "Venom Mist", "독안개", UTILITY, 16, 22, zone(5, 6, 0.5).at(14).effect(POISON, 1)),
			skill("sprint", "Sprint", "질주", MOVEMENT, 14, 16, buff(SPEED, 8, 2), cleanse()));

		weapon("knight_killer", 4, 74, DAGGER, "Knight Killer", "나이트 킬러",
			art(0x8C939C, 0x1B1B1F, 0xB03A2E), fx(Fx.Kind.SHARD, 0xC0C6CE),
			skill("knights_bane", "Knight's Bane", "기사의 재앙", ULTIMATE, 45, 36, mark(16, 10, 50), shadowstep(16, 3.6), inflict(VULNERABLE, 8, 2)),
			skill("mutilation", "Mutilation", "난도질", ATTACK, 10, 24, repeat(6, 2, slash(3.5, 140, 0.55)), inflict(BLEEDING, 6, 1)),
			skill("killing_intent", "Killing Intent", "살기", UTILITY, 20, 20, debuff(8, WEAKNESS, 6, 1), debuff(8, SLOWNESS, 6, 1)),
			skill("riposte", "Riposte", "되받아치기", DEFENSE, 18, 20, stance(COUNTER, 5, 50), shield(8, 5)));

		weapon("beru_talons", 4, 78, CLAW, "Beru's Talons", "베르의 발톱",
			art(0x1C1C2A, 0x3B1F4A, 0xB14EFF), fx(Fx.Kind.SMOKE, 0x6A0DAD),
			skill("ant_king_rampage", "Ant King's Rampage", "개미왕의 폭주", ULTIMATE, 50, 38, lifesteal(20), dash(8, 1.6), delay(0.4, repeat(4, 4, nova(5, 1.2)))),
			skill("devour", "Devour", "포식", ATTACK, 12, 22, execute(5, 2.2, 30, 2.2), lifesteal(40)),
			skill("stolen_healing", "Stolen Healing Arts", "빼앗은 치유술", DEFENSE, 30, 24, allyHeal(10, 25), buff(REGENERATION, 6, 1)),
			skill("wing_burst", "Wing Burst", "날개 돌격", MOVEMENT, 10, 18, leap(6, 4, 1.8)));

		weapon("hiraishin_kunai", 4, 82, KUNAI, "Flying Thunder God Kunai", "비뢰신 쿠나이",
			art(0x8C949E, 0xF2F2F2, 0xFFD400), fx(Fx.Kind.BOLT, 0xFFE34D),
			skill("yellow_flash", "Yellow Flash", "노란 섬광", ULTIMATE, 45, 38, repeat(4, 5, shadowstep(20, 1.0)), delay(1.0, nova(5, 3.0))),
			skill("formula_kunai", "Formula Kunai", "술식 쿠나이", ATTACK, 7, 18, shoot(ProjectileStyle.KUNAI, 1.6).count(3).spread(24).pierce(2), mark(20, 8, 25)),
			skill("flying_thunder_god", "Flying Thunder God Technique", "비뢰신의 술", MOVEMENT, 8, 18, blink(14), stance(EVADE, 4, 50)),
			skill("space_time_barrier", "Space-Time Barrier", "시공간 결계", DEFENSE, 24, 22, stance(COUNTER, 5, 60), cleanse()));

		weapon("totsuka_blade", 4, 86, NINJATO, "Totsuka Blade", "토츠카의 검",
			art(0xFFD9C7, 0x6B1A1A, 0xFF4D2E), fx(Fx.Kind.RUNE, 0xFF5A3C),
			skill("susanoo", "Susanoo", "수사노오", ULTIMATE, 50, 40, shield(20, 10), beam(16, 3.8), stun(1.5)),
			skill("amaterasu", "Amaterasu", "아마테라스", ATTACK, 14, 26, zone(3.5, 6, 0.8).at(16), burn(6)),
			skill("tsukuyomi", "Tsukuyomi", "츠쿠요미", UTILITY, 22, 24, beam(16, 1.4), stun(2.0), inflict(WEAKNESS, 6, 1)),
			skill("crow_clone", "Crow Clone", "까마귀 분신술", DEFENSE, 18, 20, stance(EVADE, 5, 50), stealth(3, 60)));

		weapon("monarch_chain_sickle", 4, 90, KUSARIGAMA, "Shadow Monarch's Chain Sickle", "그림자 군주의 쇄겸",
			art(0x15121F, 0x2C2340, 0x8A2BE2), fx(Fx.Kind.SMOKE, 0x4B1F7A),
			skill("monarchs_domain", "Monarch's Domain", "군주의 영역", ULTIMATE, 50, 38, nova(8, 3.0), zone(8, 8, 0.6).pull(0.5), buff(STRENGTH, 8, 1)),
			skill("rulers_authority", "Ruler's Authority", "지배자의 권능", UTILITY, 14, 26, launch(7, 4), delay(0.5, nova(7, 2.2)), stun(1.0)),
			skill("reapers_chain", "Reaper's Chain", "사신의 사슬", ATTACK, 8, 24, slash(7, 240, 2.2), inflict(BLEEDING, 6, 2)),
			skill("abyssal_guard", "Abyssal Guard", "심연의 수호", DEFENSE, 24, 24, shield(16, 8), stance(GUARD, 6, 40)));

		weapon("kamui_shuriken", 4, 95, SHURIKEN, "Kamui Shuriken", "카무이 수리검",
			art(0x1B2A3A, 0x0E0E14, 0x9B59FF), fx(Fx.Kind.RING, 0x9B6BFF),
			skill("susanoo_kamui_shuriken", "Perfect Susanoo: Kamui Shuriken", "완성체 수사노오: 카무이 수리검", ULTIMATE, 50, 40,
				shoot(ProjectileStyle.SHURIKEN, 3.6).count(2).spread(16).speed(1.2F).pierce(10).explode(4.0F)),
			skill("purple_lightning", "Purple Lightning", "자전", ATTACK, 10, 26, chain(16, 6, 1.8), stun(0.6)),
			skill("kamui", "Kamui", "카무이", DEFENSE, 18, 24, stance(EVADE, 5, 60), cleanse()),
			skill("kamui_raikiri", "Kamui Raikiri", "카무이 뇌절", MOVEMENT, 10, 24, blink(10), nova(3.5, 2.6), stun(0.6)));

		weapon("kamish_wrath", 4, 100, DAGGER, "Kamish's Wrath", "카미쉬의 분노",
			art(0x24182E, 0x0B0B12, 0xE040FB), fx(Fx.Kind.SLASH, 0xC77DFF),
			skill("arise", "Arise", "일어나라", ULTIMATE, 60, 40, nova(7, 3.0), summon(Summon.IRON_GOLEM, 2, 20), summon(Summon.WOLF, 4, 20)),
			skill("dragons_fear", "Dragon's Fear", "용의 공포", UTILITY, 22, 26, debuff(10, STUN, 2, 0), debuff(10, WEAKNESS, 6, 1)),
			skill("dragon_fang_volley", "Dragon Fang Volley", "용아 투척", ATTACK, 9, 28,
				shoot(ProjectileStyle.BLADE, 1.0).count(8).spread(60).pierce(3), inflict(VULNERABLE, 6, 1)),
			skill("shadow_exchange", "Shadow Exchange", "그림자 교환", MOVEMENT, 12, 20, cleanse(), blink(14), stealth(2, 40)));
	}
}
