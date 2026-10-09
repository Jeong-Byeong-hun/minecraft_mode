package com.minecraftmode.job.engrave;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.gear.GearSlot;
import com.minecraftmode.job.weapon.Archetype;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Class engravings: lines added to class weapons at the engraving table with essence instead of
 * experience. Each class has its own pool; some lines only fit certain weapon shapes. Ids are
 * saved on items, so never rename them.
 */
public enum Engraving {
	// Warrior
	CRUSH("crush", JobClass.WARRIOR, EngraveStat.BASIC_DAMAGE, 8, "Crushing Force", "분쇄"),
	WIDE_SWING("wide_swing", JobClass.WARRIOR, EngraveStat.SPLASH, 1.5F, "Wide Swing", "평타 반경",
		Archetype.GREATSWORD, Archetype.WARHAMMER, Archetype.BATTLEAXE, Archetype.HALBERD, Archetype.SCYTHE),
	BLOODTHIRST("bloodthirst", JobClass.WARRIOR, EngraveStat.LIFESTEAL, 3, "Bloodthirst", "피의 갈증"),
	IRON_SKIN("iron_skin", JobClass.WARRIOR, EngraveStat.DAMAGE_REDUCTION, 5, "Iron Skin", "강철 피부"),
	GIANTS_VIGOR("giants_vigor", JobClass.WARRIOR, EngraveStat.MAX_HEALTH, 4, "Giant's Vigor", "거인의 활력"),
	FIGHTING_SPIRIT("fighting_spirit", JobClass.WARRIOR, EngraveStat.SKILL_DAMAGE, 8, "Fighting Spirit", "투지"),
	EXECUTIONER("executioner", JobClass.WARRIOR, EngraveStat.EXECUTE, 15, "Executioner", "처형인"),
	LONG_SHAFT("long_shaft", JobClass.WARRIOR, EngraveStat.REACH, 0.5F, "Long Shaft", "긴 자루", Archetype.SPEAR, Archetype.HALBERD, Archetype.SCYTHE),
	LACERATE("lacerate", JobClass.WARRIOR, EngraveStat.BLEED, 2, "Lacerate", "열상"),
	RAGE("rage", JobClass.WARRIOR, EngraveStat.MANA_ON_HIT, 1, "Rage", "분노"),
	SWIFT_BLADE("swift_blade", JobClass.WARRIOR, EngraveStat.ATTACK_SPEED, 6, "Swift Blade", "빠른 칼날", Archetype.KATANA, Archetype.LONGSWORD),

	// Rogue
	AMBUSH("ambush", JobClass.ROGUE, EngraveStat.BACKSTAB, 20, "Ambush", "암습"),
	KEEN_EDGE("keen_edge", JobClass.ROGUE, EngraveStat.CRIT_CHANCE, 6, "Keen Edge", "예리함"),
	VITAL_POINTS("vital_points", JobClass.ROGUE, EngraveStat.CRIT_DAMAGE, 15, "Vital Points", "급소"),
	VENOM("venom", JobClass.ROGUE, EngraveStat.POISON, 2, "Venom", "맹독"),
	SWIFTNESS("swiftness", JobClass.ROGUE, EngraveStat.MOVE_SPEED, 5, "Swiftness", "신속"),
	FLURRY("flurry", JobClass.ROGUE, EngraveStat.ATTACK_SPEED, 6, "Flurry", "연격"),
	EVASION("evasion", JobClass.ROGUE, EngraveStat.DODGE, 4, "Evasion", "회피"),
	FAN_THROW("fan_throw", JobClass.ROGUE, EngraveStat.EXTRA_SHOT, 1, "Fan Throw", "부채 투척", Archetype.SHURIKEN, Archetype.KUNAI),
	SHADOW_ARTS("shadow_arts", JobClass.ROGUE, EngraveStat.COOLDOWN, 5, "Shadow Arts", "그림자 술법"),
	FINISHER("finisher", JobClass.ROGUE, EngraveStat.EXECUTE, 15, "Finisher", "마무리 일격"),

	// Mage
	AMPLIFY("amplify", JobClass.MAGE, EngraveStat.SKILL_DAMAGE, 10, "Amplify", "마력 증폭"),
	QUICK_CAST("quick_cast", JobClass.MAGE, EngraveStat.COOLDOWN, 6, "Quick Cast", "고속 영창"),
	MANA_EFFICIENCY("mana_efficiency", JobClass.MAGE, EngraveStat.MANA_COST, 8, "Mana Efficiency", "마나 절약"),
	MANA_WELL("mana_well", JobClass.MAGE, EngraveStat.MAX_MANA, 15, "Mana Well", "마나 샘"),
	SPELL_ECHO("spell_echo", JobClass.MAGE, EngraveStat.ECHO, 5, "Spell Echo", "메아리"),
	EXPANSION("expansion", JobClass.MAGE, EngraveStat.SKILL_AREA, 10, "Expansion", "광역화"),
	IGNITE("ignite", JobClass.MAGE, EngraveStat.BURN, 2, "Ignite", "점화"),
	FROSTBITE("frostbite", JobClass.MAGE, EngraveStat.SLOW, 2, "Frostbite", "동상"),
	MANA_SIPHON("mana_siphon", JobClass.MAGE, EngraveStat.MANA_ON_HIT, 2, "Mana Siphon", "마력 흡수"),
	FOCUSED_BOLT("focused_bolt", JobClass.MAGE, EngraveStat.SHOT_DAMAGE, 12, "Focused Bolt", "집중 마탄"),

	// Archer
	HEADSHOT("headshot", JobClass.ARCHER, EngraveStat.RANGE_BONUS, 6, "Headshot", "헤드샷"),
	MULTISHOT("multishot", JobClass.ARCHER, EngraveStat.EXTRA_SHOT, 1, "Multishot", "다중 사격",
		Archetype.SHORTBOW, Archetype.LONGBOW, Archetype.GREATBOW, Archetype.CROSSBOW),
	HEAVY_DRAW("heavy_draw", JobClass.ARCHER, EngraveStat.SHOT_DAMAGE, 10, "Heavy Draw", "강궁"),
	KEEN_EYE("keen_eye", JobClass.ARCHER, EngraveStat.CRIT_CHANCE, 5, "Keen Eye", "예리한 눈"),
	WINDSTEP("windstep", JobClass.ARCHER, EngraveStat.MOVE_SPEED, 5, "Windstep", "바람의 발"),
	QUICK_DRAW("quick_draw", JobClass.ARCHER, EngraveStat.DRAW_SPEED, 10, "Quick Draw", "속사",
		Archetype.SHORTBOW, Archetype.LONGBOW, Archetype.GREATBOW, Archetype.CROSSBOW),
	FOCUS("focus", JobClass.ARCHER, EngraveStat.SKILL_DAMAGE, 8, "Focus", "집중"),
	CRIPPLING_SHOT("crippling_shot", JobClass.ARCHER, EngraveStat.SLOW, 2, "Crippling Shot", "다리 저격"),
	POISON_TIP("poison_tip", JobClass.ARCHER, EngraveStat.POISON, 2, "Poison Tip", "독촉"),
	TACTICS("tactics", JobClass.ARCHER, EngraveStat.COOLDOWN, 5, "Tactics", "전술"),
	TWIN_FANGS("twin_fangs", JobClass.ARCHER, EngraveStat.BASIC_DAMAGE, 8, "Twin Fangs", "쌍아", Archetype.TWIN_BLADES),

	// Pirate
	PLUNDER("plunder", JobClass.PIRATE, EngraveStat.GOLD_FIND, 4, "Plunder", "약탈"),
	GUNPOWDER("gunpowder", JobClass.PIRATE, EngraveStat.SHOT_DAMAGE, 10, "Gunpowder", "화약",
		Archetype.PISTOL, Archetype.MUSKET, Archetype.HAND_CANNON, Archetype.HARPOON),
	RUM_RATION("rum_ration", JobClass.PIRATE, EngraveStat.LIFESTEAL, 3, "Rum Ration", "럼주"),
	BOARDING_PARTY("boarding_party", JobClass.PIRATE, EngraveStat.BASIC_DAMAGE, 8, "Boarding Party", "백병전"),
	LADY_LUCK("lady_luck", JobClass.PIRATE, EngraveStat.CRIT_CHANCE, 5, "Lady Luck", "행운의 여신"),
	CAPTAINS_COMMAND("captains_command", JobClass.PIRATE, EngraveStat.SKILL_DAMAGE, 8, "Captain's Command", "선장의 호령"),
	DOUBLE_BARREL("double_barrel", JobClass.PIRATE, EngraveStat.EXTRA_SHOT, 1, "Double Barrel", "연발", Archetype.PISTOL, Archetype.MUSKET, Archetype.HAND_CANNON),
	INCENDIARY("incendiary", JobClass.PIRATE, EngraveStat.BURN, 2, "Incendiary", "소이탄"),
	SEAS_BLESSING("seas_blessing", JobClass.PIRATE, EngraveStat.DAMAGE_REDUCTION, 4, "Sea's Blessing", "바다의 가호"),
	QUICK_RELOAD("quick_reload", JobClass.PIRATE, EngraveStat.DRAW_SPEED, 10, "Quick Reload", "빠른 장전",
		Archetype.PISTOL, Archetype.MUSKET, Archetype.HAND_CANNON, Archetype.HARPOON),
	IRON_FIST("iron_fist", JobClass.PIRATE, EngraveStat.ATTACK_SPEED, 6, "Iron Fist", "철권", Archetype.KNUCKLE, Archetype.CUTLASS, Archetype.RAPIER),

	// ---------------------------------------------------------------- armor (4 lines per piece)
	// Warrior
	BULWARK("bulwark", JobClass.WARRIOR, EngraveStat.DAMAGE_REDUCTION, 3, "Bulwark", "철벽", GearSlot.CHEST, GearSlot.LEGS),
	TITAN_BLOOD("titan_blood", JobClass.WARRIOR, EngraveStat.MAX_HEALTH, 3, "Titan Blood", "거인의 피", GearSlot.HEAD, GearSlot.CHEST),
	BATTLE_FOCUS("battle_focus", JobClass.WARRIOR, EngraveStat.COOLDOWN_FLAT, 0.3F, "Battle Focus", "전투 집중", GearSlot.HEAD),
	SPIKED_PLATE("spiked_plate", JobClass.WARRIOR, EngraveStat.THORNS, 8, "Spiked Plate", "가시 갑주", GearSlot.CHEST),
	UNSTOPPABLE("unstoppable", JobClass.WARRIOR, EngraveStat.KNOCKBACK_RES, 10, "Unstoppable", "불굴", GearSlot.LEGS, GearSlot.FEET),
	WARPATH("warpath", JobClass.WARRIOR, EngraveStat.MOVE_SPEED, 3, "Warpath", "진격", GearSlot.FEET),
	HEAVY_HAND("heavy_hand", JobClass.WARRIOR, EngraveStat.BASIC_DAMAGE, 4, "Heavy Hand", "묵직한 손", GearSlot.HEAD, GearSlot.CHEST, GearSlot.LEGS, GearSlot.FEET),
	// Rogue
	SHADOW_STEP("shadow_step", JobClass.ROGUE, EngraveStat.DODGE, 2, "Shadow Step", "그림자 걸음", GearSlot.LEGS, GearSlot.FEET),
	CUTTHROAT("cutthroat", JobClass.ROGUE, EngraveStat.CRIT_DAMAGE, 8, "Cutthroat", "목따기", GearSlot.HEAD, GearSlot.CHEST),
	FLEET_FOOT("fleet_foot", JobClass.ROGUE, EngraveStat.MOVE_SPEED, 3, "Fleet Foot", "빠른 발", GearSlot.FEET),
	VANISHING_ACT("vanishing_act", JobClass.ROGUE, EngraveStat.STEALTH_ON_KILL, 1, "Vanishing Act", "사라지는 기술", GearSlot.CHEST),
	NIMBLE_FINGERS("nimble_fingers", JobClass.ROGUE, EngraveStat.ATTACK_SPEED, 3, "Nimble Fingers", "재빠른 손", GearSlot.CHEST, GearSlot.LEGS),
	KILLERS_INSTINCT("killers_instinct", JobClass.ROGUE, EngraveStat.CRIT_CHANCE, 3, "Killer's Instinct", "살수의 본능", GearSlot.HEAD),
	// Mage
	ARCANE_MIND("arcane_mind", JobClass.MAGE, EngraveStat.MAX_MANA, 10, "Arcane Mind", "비전의 정신", GearSlot.HEAD, GearSlot.CHEST),
	FLOWING_MANA("flowing_mana", JobClass.MAGE, EngraveStat.MANA_REGEN, 1, "Flowing Mana", "흐르는 마나", GearSlot.HEAD, GearSlot.LEGS),
	SPELL_HASTE("spell_haste", JobClass.MAGE, EngraveStat.COOLDOWN_FLAT, 0.3F, "Spell Haste", "주문 가속", GearSlot.HEAD),
	MANA_WARD("mana_ward", JobClass.MAGE, EngraveStat.MANA_SHIELD, 5, "Mana Ward", "마나 보호막", GearSlot.CHEST),
	POWER_SURGE("power_surge", JobClass.MAGE, EngraveStat.SKILL_DAMAGE, 4, "Power Surge", "마력 쇄도", GearSlot.HEAD, GearSlot.CHEST, GearSlot.LEGS, GearSlot.FEET),
	SKY_WALK("sky_walk", JobClass.MAGE, EngraveStat.MOVE_SPEED, 3, "Sky Walk", "천공 보행", GearSlot.FEET),
	// Archer
	EAGLE_SIGHT("eagle_sight", JobClass.ARCHER, EngraveStat.CRIT_CHANCE, 3, "Eagle Sight", "독수리 눈", GearSlot.HEAD),
	STEADY_AIM("steady_aim", JobClass.ARCHER, EngraveStat.SHOT_DAMAGE, 4, "Steady Aim", "안정된 조준", GearSlot.CHEST, GearSlot.LEGS),
	SWIFT_QUIVER("swift_quiver", JobClass.ARCHER, EngraveStat.DRAW_SPEED, 5, "Swift Quiver", "빠른 화살통", GearSlot.CHEST),
	WIND_RUNNER("wind_runner", JobClass.ARCHER, EngraveStat.MOVE_SPEED, 3, "Wind Runner", "바람 질주", GearSlot.LEGS, GearSlot.FEET),
	HUNTERS_TALLY("hunters_tally", JobClass.ARCHER, EngraveStat.KILL_HEAL, 1, "Hunter's Tally", "사냥꾼의 셈", GearSlot.CHEST, GearSlot.LEGS),
	FAR_SIGHT("far_sight", JobClass.ARCHER, EngraveStat.RANGE_BONUS, 2, "Far Sight", "원시", GearSlot.HEAD),
	// Pirate
	SEA_DOG("sea_dog", JobClass.PIRATE, EngraveStat.MAX_HEALTH, 3, "Sea Dog", "바다의 개", GearSlot.CHEST),
	TREASURE_NOSE("treasure_nose", JobClass.PIRATE, EngraveStat.ITEM_FIND, 3, "Treasure Nose", "보물 냄새", GearSlot.HEAD, GearSlot.FEET),
	GOLD_TOOTH("gold_tooth", JobClass.PIRATE, EngraveStat.GOLD_FIND, 2, "Gold Tooth", "금니", GearSlot.HEAD),
	BRAWLERS_BELT("brawlers_belt", JobClass.PIRATE, EngraveStat.BASIC_DAMAGE, 4, "Brawler's Belt", "싸움꾼의 허리띠", GearSlot.LEGS),
	DECK_LEGS("deck_legs", JobClass.PIRATE, EngraveStat.DODGE, 2, "Deck Legs", "갑판 다리", GearSlot.FEET),
	GROG("grog", JobClass.PIRATE, EngraveStat.KILL_HEAL, 1, "Grog", "그로그주", GearSlot.CHEST);

	private final String id;
	private final JobClass job;
	private final EngraveStat stat;
	private final float value;
	private final String en;
	private final String ko;
	private final Set<Archetype> only;
	private final Set<GearSlot> slots;

	/** Weapon line, optionally only for some weapon shapes. */
	Engraving(final String id, final JobClass job, final EngraveStat stat, final float value, final String en, final String ko, final Archetype... only) {
		this.id = id;
		this.job = job;
		this.stat = stat;
		this.value = value;
		this.en = en;
		this.ko = ko;
		this.only = only.length == 0 ? EnumSet.allOf(Archetype.class) : EnumSet.copyOf(Arrays.asList(only));
		this.slots = EnumSet.of(GearSlot.WEAPON);
	}

	/** Armor line for the given slots. */
	Engraving(
		final String id, final JobClass job, final EngraveStat stat, final float value, final String en, final String ko, final GearSlot slot,
		final GearSlot... more
	) {
		this.id = id;
		this.job = job;
		this.stat = stat;
		this.value = value;
		this.en = en;
		this.ko = ko;
		this.only = EnumSet.noneOf(Archetype.class);
		this.slots = EnumSet.of(slot, more);
	}

	public String id() {
		return this.id;
	}

	public JobClass job() {
		return this.job;
	}

	public EngraveStat stat() {
		return this.stat;
	}

	/** Amount one line adds. */
	public float value() {
		return this.value;
	}

	public String en() {
		return this.en;
	}

	public String ko() {
		return this.ko;
	}

	public String nameKey() {
		return "engraving.minecraft_mode." + this.id;
	}

	/** Fits a class weapon of this class and shape. */
	public boolean fits(final JobClass job, final Archetype archetype) {
		return this.job == job && this.slots.contains(GearSlot.WEAPON) && this.only.contains(archetype);
	}

	/** Fits a class armor piece of this class and slot. */
	public boolean fits(final JobClass job, final GearSlot slot) {
		return this.job == job && this.slots.contains(slot);
	}

	public boolean isArmor() {
		return !this.slots.contains(GearSlot.WEAPON);
	}

	public Set<GearSlot> slots() {
		return this.slots;
	}

	public static List<Engraving> pool(final JobClass job, final Archetype archetype) {
		return Arrays.stream(values()).filter(e -> e.fits(job, archetype)).toList();
	}

	public static List<Engraving> pool(final JobClass job, final GearSlot slot) {
		return Arrays.stream(values()).filter(e -> e.fits(job, slot)).toList();
	}

	public static Engraving byId(final String id) {
		for (Engraving e : values()) {
			if (e.id.equals(id)) {
				return e;
			}
		}
		return null;
	}
}
