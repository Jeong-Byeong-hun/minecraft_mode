package com.minecraftmode.job.skill;

/**
 * Drawn skill effects that particles cannot show (a crescent sword wave, a spinning sphere). The client draws them as geometry
 * ({@code client/vfx}): {@link #GETSUGA}, {@link #GETSUGA_DUAL} and {@link #TAIMATSU} are the shape of a {@link SkillProjectile}, the
 * others are {@link SkillVfx} entities that live {@link #ticks} ticks (or as long as {@link Actions.LookAction#lasting} says). Where
 * one appears (feet, aimed ground, a target, along a beam or a cone) is set by {@link Actions.LookAction}. Synced by ordinal: append
 * only.
 */
public enum SkillLook {
	NONE(0, 0.0F, Hold.FREE),
	/** Getsuga Tensho: a glowing crescent flying in place of the sword wave's trail (black with a red rim off a dark blade). */
	GETSUGA(0, 1.0F, Hold.FREE),
	/** Rasengan in the caster's hand while they dash in; lasts until the burst. */
	RASENGAN(6, 1.0F, Hold.HAND),
	/** Rasengan's spiral burst, as wide as the area it hits ({@link #size} is the radius before the Expansion engraving). */
	RASENGAN_BURST(14, 3.0F, Hold.FREE),

	// ------------------------------------------------------------ Soul Reaper bankai (0.5.36)
	/** True Zangetsu's Getsuga: a white crescent with a black one riding inside it (projectile). */
	GETSUGA_DUAL(0, 1.0F, Hold.FREE),
	/** Ryujin Jakka's Taimatsu: a ball of flame with licking tongues and a trail of fire (projectile). */
	TAIMATSU(0, 1.0F, Hold.FREE),
	/** Flash step at bankai speed: a streak of afterimages along the caster's path. */
	FLASH_TRAIL(14, 1.0F, Hold.TRAIL),
	/** Hollow reiatsu: black-red spiritual pressure flaring up around the caster, the mask's horns over it. */
	REIATSU(40, 1.0F, Hold.FOLLOW),
	/** Senbonzakura: streams of blade petals swirling around the caster. */
	PETAL_STORM(30, 6.0F, Hold.FOLLOW),
	/** Gokei: the petals close into a sphere around the caster and crush inward. */
	PETAL_SPHERE(20, 6.0F, Hold.FREE),
	/** Senkei: rows of great swords standing in a ring. */
	SENKEI(120, 6.0F, Hold.FREE),
	/** Shukei Hakuteiken: one white blade of condensed petals, wings of light behind the hilt (along a beam). */
	HAKUTEIKEN(16, 26.0F, Hold.FREE),
	/** Ryusenka: an ice flower blooming from the stabbed target. */
	ICE_FLOWER(40, 1.0F, Hold.FOLLOW),
	/** Sennen Hyoro: pillars of ice rising in a ring and closing over the aimed spot. */
	ICE_PRISON(44, 4.0F, Hold.FREE),
	/** Daiguren's wings of ice on the caster's back. */
	ICE_WINGS(120, 1.0F, Hold.FOLLOW),
	/** Hyoten Hyakkaso: a ring of cloud overhead, snow falling and ice flowers blooming where it lands. */
	ICE_BLOOM(120, 7.0F, Hold.FREE),
	/** Ryujin Jakka's shikai: a ring of flame tongues bursting out. */
	FLAME_BURST(18, 6.0F, Hold.FREE),
	/** Jokaku Enjo: a castle wall of flame pillars around the caster. */
	FLAME_WALL(120, 6.0F, Hold.FREE),
	/** Zanka no Tachi: a white-hot blade circle swept all the way round, fire left behind. */
	ZANKA(22, 8.0F, Hold.FREE),
	/** Kamishini no Yari: a hair-thin blade shooting out to its full length and back (along a beam). */
	SPEAR(8, 30.0F, Hold.FREE),
	/** Korose: the spear's line and the target dissolving in a burst of decay at its end. */
	SPEAR_KILL(18, 30.0F, Hold.FREE),
	/** Dust to dust: a crumbling ring and ash drifting up. */
	DUST(30, 8.0F, Hold.FREE),
	/** Kyoka Suigetsu: a mirror pane cracking and flying apart. */
	MIRROR(20, 1.0F, Hold.FREE),
	/** Hado #90 Kurohitsugi: a black coffin of walls rising around the aimed spot, spears piercing inside. */
	KUROHITSUGI(60, 4.0F, Hold.FREE),
	/** Hogyoku: a violet orb ringed with turning runes, a pillar of light and a ring of power. */
	HOGYOKU(26, 6.0F, Hold.FREE),
	/** A sweeping blade arc in front of the caster (along a cone). */
	SLASH(8, 5.0F, Hold.FREE),
	/** Two blade arcs crossing in an X in front of the caster (along a cone). */
	CROSS_SLASH(10, 4.0F, Hold.FREE),
	/** Drowning Abyss: a pool of dark water with a whirl and rising waves. */
	ABYSS(100, 6.0F, Hold.FREE),
	/** Itosabaki: strips of cloth winding round the target and pulling tight. */
	BINDING(30, 1.0F, Hold.FOLLOW),
	/** Mugetsu: a towering black blade of darkness torn forward (along a beam). */
	MUGETSU(30, 30.0F, Hold.FREE),
	/** Mugetsu's darkness lingering on the aimed spot: a black dome. */
	MUGETSU_VOID(80, 8.0F, Hold.FREE),
	/** Higa Zekko: Zabimaru's bone segments hanging in a ring round the caster, fangs in, darting at each strike. */
	BONE_FANGS(30, 6.0F, Hold.FOLLOW),
	/** Zaga Tesso: the bone snake lunging out with open jaws (along a beam). */
	BONE_SNAKE(12, 16.0F, Hold.FREE),
	/** A segmented bone whip sweeping its arc (along a cone). */
	BONE_LASH(10, 7.0F, Hold.FREE),
	/** Baboon bone armor: great ribs rising round the caster. */
	BONE_CAGE(50, 1.0F, Hold.FOLLOW);

	/** How a look moves once it is shown. */
	public enum Hold {
		/** Stays where it appeared. */
		FREE,
		/** Drawn at its holder's right hand (in first person low right of the view); gone with the holder. */
		HAND,
		/** Follows its holder (the caster, or the target it was put on) while it is there. */
		FOLLOW,
		/** Stays where it appeared; the client draws the holder's path from there. */
		TRAIL
	}

	private final int ticks;
	private final float size;
	private final Hold hold;

	SkillLook(final int ticks, final float size, final Hold hold) {
		this.ticks = ticks;
		this.size = size;
		this.hold = hold;
	}

	public int ticks() {
		return this.ticks;
	}

	public float size() {
		return this.size;
	}

	public Hold hold() {
		return this.hold;
	}

	/** Held by its caster (or a target): anything but {@link Hold#FREE}. */
	public boolean anchored() {
		return this.hold != Hold.FREE;
	}

	public static SkillLook byId(final int id) {
		SkillLook[] all = values();
		return id >= 0 && id < all.length ? all[id] : NONE;
	}
}
