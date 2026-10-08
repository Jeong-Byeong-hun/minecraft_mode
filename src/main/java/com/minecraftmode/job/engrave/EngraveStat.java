package com.minecraftmode.job.engrave;

/**
 * What an engraving line adds. Lines of the same stat add up (three "+1.5 splash radius" lines give
 * 4.5); {@link #cap()} bounds the total for stats that would break the game past a point.
 */
public enum EngraveStat {
	BASIC_DAMAGE("basic_damage", "+%s%% basic attack damage", "기본 공격 피해 +%s%%", 0),
	SKILL_DAMAGE("skill_damage", "+%s%% skill damage", "스킬 피해 +%s%%", 0),
	COOLDOWN("cooldown", "-%s%% skill cooldowns", "스킬 재사용 대기시간 -%s%%", 50),
	MANA_COST("mana_cost", "-%s%% skill MP cost", "스킬 MP 소모 -%s%%", 60),
	CRIT_CHANCE("crit_chance", "+%s%% critical chance", "치명타 확률 +%s%%", 0),
	CRIT_DAMAGE("crit_damage", "+%s%% critical damage", "치명타 피해 +%s%%", 0),
	LIFESTEAL("lifesteal", "%s%% lifesteal", "흡혈 %s%%", 30),
	SPLASH("splash", "Basic attacks also hit enemies within %s blocks of the target", "기본 공격이 대상 주변 %s블록의 적도 타격", 0),
	ATTACK_SPEED("attack_speed", "+%s%% attack speed", "공격 속도 +%s%%", 0),
	MOVE_SPEED("move_speed", "+%s%% movement speed", "이동 속도 +%s%%", 0),
	REACH("reach", "+%s attack reach", "공격 사거리 +%s", 0),
	MAX_HEALTH("max_health", "+%s max health", "최대 체력 +%s", 0),
	DAMAGE_REDUCTION("damage_reduction", "-%s%% damage taken", "받는 피해 -%s%%", 50),
	MANA_ON_HIT("mana_on_hit", "+%s MP per basic hit", "기본 공격 적중 시 MP +%s", 0),
	EXECUTE("execute", "+%s%% damage to enemies below 30%% health", "체력 30%% 미만 적에게 피해 +%s%%", 0),
	BACKSTAB("backstab", "+%s%% damage from behind", "뒤에서 공격 시 피해 +%s%%", 0),
	POISON("poison", "Basic hits poison for %ss", "기본 공격 적중 시 %s초 중독", 0),
	BLEED("bleed", "Basic hits cause bleeding for %ss", "기본 공격 적중 시 %s초 출혈", 0),
	BURN("burn", "Basic hits ignite for %ss", "기본 공격 적중 시 %s초 화상", 0),
	SLOW("slow", "Basic hits slow for %ss", "기본 공격 적중 시 %s초 둔화", 0),
	SKILL_AREA("skill_area", "+%s%% skill area", "스킬 범위 +%s%%", 0),
	EXTRA_SHOT("extra_shot", "+%s projectiles per basic shot", "기본 사격 투사체 +%s", 0),
	SHOT_DAMAGE("shot_damage", "+%s%% basic shot damage", "기본 사격 피해 +%s%%", 0),
	RANGE_BONUS("range_bonus", "+%s%% damage per 10 blocks of distance", "거리 10블록당 피해 +%s%%", 0),
	GOLD_FIND("gold_find", "%s%% chance to drop coins on kill", "처치 시 %s%% 확률로 동전 획득", 100),
	MAX_MANA("max_mana", "+%s max MP", "최대 MP +%s", 0),
	ECHO("echo", "%s%% chance a skill skips its cooldown", "%s%% 확률로 스킬 재사용 대기시간 없음", 50),
	DODGE("dodge", "%s%% chance to dodge attacks", "%s%% 확률로 공격 회피", 40),
	DRAW_SPEED("draw_speed", "+%s%% draw and reload speed", "시위 당기기·재장전 속도 +%s%%", 75);

	private final String id;
	private final String en;
	private final String ko;
	private final float cap;

	EngraveStat(final String id, final String en, final String ko, final float cap) {
		this.id = id;
		this.en = en;
		this.ko = ko;
		this.cap = cap;
	}

	/** Translation key whose single argument is the value. */
	public String key() {
		return "engrave_stat.minecraft_mode." + this.id;
	}

	public String en() {
		return this.en;
	}

	public String ko() {
		return this.ko;
	}

	/** 0 = no cap. */
	public float cap() {
		return this.cap;
	}
}
