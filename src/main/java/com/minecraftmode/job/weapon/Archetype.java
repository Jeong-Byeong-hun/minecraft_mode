package com.minecraftmode.job.weapon;

/**
 * Weapon shape: how the basic attack works and how strong/fast it is relative to the weapon's
 * power. {@link BasicMode#SHOOT} weapons also fire on right click; {@link BasicMode#DRAW} weapons
 * are drawn like a bow. Basic attacks work for everyone; skills need the right class.
 */
public enum Archetype {
	// Warrior
	GREATSWORD("greatsword", "Greatsword", "대검", BasicMode.MELEE, 1.40F, 0.8F, 0.5F),
	KATANA("katana", "Katana", "태도", BasicMode.MELEE, 1.00F, 1.6F, 0.0F),
	LONGSWORD("longsword", "Longsword", "장검", BasicMode.MELEE, 1.10F, 1.4F, 0.0F),
	SPEAR("spear", "Spear", "창", BasicMode.MELEE, 1.00F, 1.2F, 1.5F),
	HALBERD("halberd", "Halberd", "할버드", BasicMode.MELEE, 1.25F, 0.9F, 1.0F),
	WARHAMMER("warhammer", "Warhammer", "전투 망치", BasicMode.MELEE, 1.50F, 0.7F, 0.0F),
	BATTLEAXE("battleaxe", "Battleaxe", "전투 도끼", BasicMode.MELEE, 1.30F, 0.9F, 0.0F),
	SCYTHE("scythe", "Scythe", "대낫", BasicMode.MELEE, 1.20F, 1.0F, 1.0F),
	// Rogue
	DAGGER("dagger", "Dagger", "단검", BasicMode.MELEE, 0.70F, 2.2F, -0.5F),
	CLAW("claw", "Claw", "클로", BasicMode.MELEE, 0.65F, 2.5F, -0.5F),
	NINJATO("ninjato", "Ninjato", "닌자도", BasicMode.MELEE, 0.90F, 1.8F, 0.0F),
	KUSARIGAMA("kusarigama", "Kusarigama", "쇄겸", BasicMode.MELEE, 0.85F, 1.4F, 1.5F),
	SHURIKEN("shuriken", "Shuriken", "표창", 0.55F, 2.0F, -0.5F, ProjectileStyle.SHURIKEN, 10, 0.70F),
	KUNAI("kunai", "Kunai", "쿠나이", 0.60F, 2.0F, -0.5F, ProjectileStyle.KUNAI, 12, 0.80F),
	// Mage
	STAFF("staff", "Staff", "지팡이", 0.50F, 1.0F, 0.0F, ProjectileStyle.ORB, 16, 0.80F),
	WAND("wand", "Wand", "완드", 0.40F, 1.4F, 0.0F, ProjectileStyle.ORB, 10, 0.55F),
	ORB("orb", "Orb", "오브", 0.40F, 1.2F, 0.0F, ProjectileStyle.ORB, 14, 0.70F),
	GRIMOIRE("grimoire", "Grimoire", "마도서", 0.40F, 1.0F, 0.0F, ProjectileStyle.ORB, 18, 0.90F),
	SCEPTER("scepter", "Scepter", "홀", 0.60F, 1.0F, 0.0F, ProjectileStyle.ORB, 16, 0.80F),
	// Archer
	SHORTBOW("shortbow", "Shortbow", "단궁", 0.40F, 1.6F, 15, 0.90F),
	LONGBOW("longbow", "Longbow", "장궁", 0.40F, 1.2F, 25, 1.30F),
	GREATBOW("greatbow", "Greatbow", "대궁", 0.50F, 1.0F, 30, 1.60F),
	CROSSBOW("crossbow", "Crossbow", "석궁", 0.50F, 1.0F, 0.0F, ProjectileStyle.ARROW, 22, 1.10F),
	TWIN_BLADES("twin_blades", "Twin Blades", "쌍검", BasicMode.MELEE, 0.85F, 1.9F, 0.0F),
	// Pirate
	CUTLASS("cutlass", "Cutlass", "커틀러스", BasicMode.MELEE, 1.00F, 1.6F, 0.0F),
	RAPIER("rapier", "Rapier", "레이피어", BasicMode.MELEE, 0.85F, 2.0F, 0.5F),
	KNUCKLE("knuckle", "Knuckle", "너클", BasicMode.MELEE, 0.75F, 2.2F, -1.0F),
	ANCHOR("anchor", "Anchor", "닻", BasicMode.MELEE, 1.55F, 0.6F, 0.5F),
	PISTOL("pistol", "Pistol", "권총", 0.50F, 1.6F, 0.0F, ProjectileStyle.BULLET, 16, 0.85F),
	MUSKET("musket", "Musket", "머스킷", 0.60F, 1.0F, 0.0F, ProjectileStyle.BULLET, 30, 1.50F),
	HAND_CANNON("hand_cannon", "Hand Cannon", "핸드 캐논", 0.70F, 0.8F, 0.0F, ProjectileStyle.CANNONBALL, 40, 1.40F),
	HARPOON("harpoon", "Harpoon", "작살", 1.00F, 1.1F, 1.5F, ProjectileStyle.HARPOON, 30, 1.00F);

	public enum BasicMode {
		/** Plain melee attacks. */
		MELEE,
		/** Melee plus a right-click shot with a per-item cooldown. */
		SHOOT,
		/** Hold right click to draw, release to fire an arrow (no ammo needed). */
		DRAW
	}

	private final String id;
	private final String en;
	private final String ko;
	private final BasicMode mode;
	private final float damageMultiplier;
	private final float attackSpeed;
	private final float reach;
	private final ProjectileStyle shot;
	private final int shotCooldown;
	private final float shotMultiplier;

	Archetype(final String id, final String en, final String ko, final BasicMode mode, final float damageMultiplier, final float attackSpeed, final float reach) {
		this(id, en, ko, mode, damageMultiplier, attackSpeed, reach, null, 0, 0.0F);
	}

	/** Right-click shooter. */
	Archetype(
		final String id, final String en, final String ko, final float damageMultiplier, final float attackSpeed, final float reach, final ProjectileStyle shot,
		final int shotCooldown, final float shotMultiplier
	) {
		this(id, en, ko, BasicMode.SHOOT, damageMultiplier, attackSpeed, reach, shot, shotCooldown, shotMultiplier);
	}

	/** Bow: {@code drawTicks} to full power. */
	Archetype(final String id, final String en, final String ko, final float damageMultiplier, final float attackSpeed, final int drawTicks, final float shotMultiplier) {
		this(id, en, ko, BasicMode.DRAW, damageMultiplier, attackSpeed, 0.0F, ProjectileStyle.ARROW, drawTicks, shotMultiplier);
	}

	Archetype(
		final String id,
		final String en,
		final String ko,
		final BasicMode mode,
		final float damageMultiplier,
		final float attackSpeed,
		final float reach,
		final ProjectileStyle shot,
		final int shotCooldown,
		final float shotMultiplier
	) {
		this.id = id;
		this.en = en;
		this.ko = ko;
		this.mode = mode;
		this.damageMultiplier = damageMultiplier;
		this.attackSpeed = attackSpeed;
		this.reach = reach;
		this.shot = shot;
		this.shotCooldown = shotCooldown;
		this.shotMultiplier = shotMultiplier;
	}

	public String id() {
		return this.id;
	}

	public String en() {
		return this.en;
	}

	public String ko() {
		return this.ko;
	}

	public String nameKey() {
		return "archetype.minecraft_mode." + this.id;
	}

	public BasicMode mode() {
		return this.mode;
	}

	/** Melee damage = weapon power x this. */
	public float damageMultiplier() {
		return this.damageMultiplier;
	}

	/** Attacks per second. */
	public float attackSpeed() {
		return this.attackSpeed;
	}

	/** Extra entity reach in blocks (negative = shorter). */
	public float reach() {
		return this.reach;
	}

	public ProjectileStyle shot() {
		return this.shot;
	}

	/** SHOOT: cooldown in ticks. DRAW: ticks to full draw. */
	public int shotCooldown() {
		return this.shotCooldown;
	}

	public float shotMultiplier() {
		return this.shotMultiplier;
	}

	public boolean isRanged() {
		return this.mode != BasicMode.MELEE;
	}

	/** Thrusting weapons use the stab swing animation. */
	public boolean stabs() {
		return this == SPEAR || this == RAPIER || this == HARPOON;
	}
}
