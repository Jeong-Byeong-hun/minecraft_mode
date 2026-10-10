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
	DRAW_SPEED("draw_speed", "+%s%% draw and reload speed", "시위 당기기·재장전 속도 +%s%%", 75),
	// armor options, set bonuses and level rewards
	COOLDOWN_FLAT("cooldown_flat", "-%ss skill cooldowns (never below 1s)", "스킬 재사용 대기시간 -%s초 (1초 미만으로는 줄지 않음)", 6),
	MANA_REGEN("mana_regen", "+%s MP per second", "초당 MP 회복 +%s", 0),
	HEALTH_REGEN("health_regen", "Heals %s every 4 seconds", "4초마다 체력 %s 회복", 0),
	THORNS("thorns", "Reflects %s%% of melee damage taken", "받은 근접 피해의 %s%% 반사", 100),
	ITEM_FIND("item_find", "+%s%% equipment drop chance", "장비 드롭 확률 +%s%%", 0),
	EXP_BONUS("exp_bonus", "+%s%% class EXP", "직업 경험치 +%s%%", 0),
	ARMOR("armor", "+%s armor", "방어력 +%s", 0),
	KNOCKBACK_RES("knockback_res", "+%s%% knockback resistance", "넉백 저항 +%s%%", 100),
	KILL_HEAL("kill_heal", "Heals %s on kill", "처치 시 체력 %s 회복", 0),
	KILL_MANA("kill_mana", "+%s MP on kill", "처치 시 MP +%s", 0),
	LAST_STAND("last_stand", "Below 30%% health: -%s%% damage taken", "체력 30%% 미만일 때 받는 피해 -%s%%", 60),
	FIRST_STRIKE("first_strike", "+%s%% damage to enemies above 90%% health", "체력 90%% 이상인 적에게 피해 +%s%%", 0),
	BOSS_DAMAGE("boss_damage", "+%s%% damage to bosses and named monsters", "보스·네임드에게 주는 피해 +%s%%", 0),
	CHAIN_LIGHTNING("chain_lightning", "%s%% chance on hit to chain lightning to 3 enemies", "적중 시 %s%% 확률로 적 3명에게 연쇄 번개", 50),
	STEALTH_ON_KILL("stealth_on_kill", "Invisible for %ss after a kill", "처치 후 %s초간 투명", 5),
	SPEED_ON_KILL("speed_on_kill", "Swiftness for %ss after a kill", "처치 후 %s초간 신속", 10),
	CRIT_REFUND("crit_refund", "Critical hits cut skill cooldowns by %ss", "치명타 시 스킬 재사용 대기시간 %s초 감소", 2),
	MANA_SHIELD("mana_shield", "%s%% of damage taken is paid with MP", "받는 피해의 %s%%를 MP로 대신 받음", 50),
	DOUBLE_STRIKE("double_strike", "%s%% chance for basic attacks to hit twice", "%s%% 확률로 기본 공격이 두 번 적중", 50),
	HEAL_ON_SKILL("heal_on_skill", "Casting a skill heals %s", "스킬 사용 시 체력 %s 회복", 0),
	// built into class armor (ClassDefense)
	PROTECTION("protection", "-%s%% damage taken (protection)", "물리 보호: 받는 피해 -%s%%", 70),
	MAGIC_DEFENSE("magic_defense", "-%s%% magic damage taken", "마법 방어: 받는 마법 피해 -%s%%", 70);

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

	/** Saved id (armor options); never rename. */
	public String id() {
		return this.id;
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
