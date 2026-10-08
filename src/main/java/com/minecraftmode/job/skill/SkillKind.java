package com.minecraftmode.job.skill;

/** Role of a skill; only used for display (tooltip/HUD color and label). */
public enum SkillKind {
	ATTACK("attack", "Attack", "공격", 0xFF6B5B),
	DEFENSE("defense", "Defense", "방어", 0x5BA8FF),
	UTILITY("utility", "Utility", "유틸", 0x6BE07A),
	MOVEMENT("movement", "Movement", "이동", 0xFFD84A),
	ULTIMATE("ultimate", "Ultimate", "궁극기", 0xFF9F1C);

	private final String id;
	private final String en;
	private final String ko;
	private final int color;

	SkillKind(final String id, final String en, final String ko, final int color) {
		this.id = id;
		this.en = en;
		this.ko = ko;
		this.color = color;
	}

	public String nameKey() {
		return "skill_kind.minecraft_mode." + this.id;
	}

	public String en() {
		return this.en;
	}

	public String ko() {
		return this.ko;
	}

	public int color() {
		return this.color;
	}
}
