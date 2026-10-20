package com.minecraftmode.job.skill;

/**
 * Drawn skill effects that particles cannot show (a crescent sword wave, a spinning sphere). The client draws them as geometry
 * ({@code client/vfx}): the ones without ticks ({@link #projectile}: {@link #GETSUGA}, {@link #TAIMATSU}, the archer's arrows) are the
 * shape of a {@link SkillProjectile}, the others are {@link SkillVfx} entities that live {@link #ticks} ticks (or as long as {@link Actions.LookAction#lasting} says). Where
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
	BONE_CAGE(50, 1.0F, Hold.FOLLOW),

	// ------------------------------------------------------------ Archer, King of Heroes (0.5.38)
	/** Antares Snipe: an arrow of starlight, the red star Antares at its head and a tail of glinting stars (projectile). */
	STAR_ARROW(0, 1.0F, Hold.FREE),
	/** Antares Snipe's mark: the Scorpion's stars and their lines turning over the target, Antares burning red. */
	ANTARES_MARK(60, 1.0F, Hold.FOLLOW),
	/** Pankration: a palm strike's shock rolling out in front, the ground cracking under it (along a cone). */
	PANKRATION(12, 4.0F, Hold.FREE),
	/** Protection from Arrows: arrows of light circling the caster and a faint dome they turn on. */
	ARROW_WARD(50, 1.6F, Hold.FOLLOW),
	/** Centaur's Gallop: hoofprints of fire along the caster's path and wind streaking past. */
	GALLOP(24, 1.0F, Hold.TRAIL),
	/** A gunshot's tracer: a hot streak behind the bullet (projectile). */
	TRACER(0, 1.0F, Hold.FREE),
	/** Gun Kata: the black and white falchions whirling round the caster, muzzle flashes all round. */
	GUN_KATA(12, 4.0F, Hold.FREE),
	/** Unlimited Lost Works: a field of swords stuck in the ground, gears turning in a red sky over it. */
	ULW_FIELD(60, 5.0F, Hold.FREE),
	/** Counter Guardian: a tattered dark-red shroud and a red mantle of power round the caster. */
	COUNTER_GUARDIAN(60, 1.0F, Hold.FOLLOW),
	/** Hekatebolos: a sun disc behind the caster and a beam of sunlight shot from it (along a beam). */
	SUN_BEAM(24, 26.0F, Hold.FREE),
	/** Achilles' Heel: a golden reticle closing on the target and one pinpoint shaft of light into its heel. */
	HEEL_STRIKE(18, 1.0F, Hold.FOLLOW),
	/** Paean: golden notes of the hymn rising round the caster, rings of light washing out. */
	PAEAN(40, 10.0F, Hold.FOLLOW),
	/** Sunlit Step: a sunburst where the caster left and a streak of sunlight to where they are. */
	SUN_STEP(16, 1.0F, Hold.TRAIL),
	/** Tri-Star Amore Mio: a moonlit arrow trailing hearts and stars (projectile). */
	MOON_ARROW(0, 1.0F, Hold.FREE),
	/** Crescent Volley: a silver crescent moon riding each arrow (projectile). */
	CRESCENT_ARROW(0, 1.0F, Hold.FREE),
	/** Hounds of the Moon: a moon sigil on the ground, the full moon over it and crescents round its rim. */
	MOON_SIGIL(50, 6.0F, Hold.FREE),
	/** Moonlight Veil: a crescent moon over the caster and a shimmering silver veil falling round them. */
	MOON_VEIL(60, 1.0F, Hold.FOLLOW),
	/** Gandiva's Pashupata: the destroyer's third eye opening in the sky and a blue-white bolt thrown down from it. */
	PASHUPATA_BOLT(24, 6.0F, Hold.FREE),
	/** Agni Gandiva: an arrow wreathed in the fire god's flames (projectile). */
	FLAME_ARROW(0, 1.0F, Hold.FREE),
	/** Hero of the Endowed: a halo of blessing over the caster and motes of light raining round them. */
	ENDOWED(40, 1.0F, Hold.FOLLOW),
	/** Mana Burst (Flame): flames bursting off the caster's path. */
	MANA_FLAME(24, 1.0F, Hold.TRAIL),
	/** An Gal Ta Kigal She: Venus's eight-pointed star on the aimed ground, the great gem-star falling onto it and bursting. */
	VENUS_STAR(30, 6.0F, Hold.FREE),
	/** Gem Shot: a cut gemstone, glinting (projectile). */
	GEM(0, 1.0F, Hold.FREE),
	/** Manifestation of Beauty: Ishtar's star glowing on the ground round the caster, light rising. */
	BEAUTY(40, 10.0F, Hold.FOLLOW),
	/** Goddess's Flight: wings of golden light on the caster's back. */
	VENUS_WINGS(80, 1.0F, Hold.FOLLOW),
	/** One Gate of Babylon: a golden ripple in the air a treasure flies out of (facing along its reach). */
	BABYLON_GATE(16, 1.0F, Hold.FREE),
	/** A treasure of the Gate: a golden streak behind the flying weapon (projectile). */
	TREASURE(0, 1.0F, Hold.FREE),
	/** Full Open: the sky over the aimed spot filled with golden gates, blades showing in them. */
	BABYLON_SKY(40, 6.0F, Hold.FREE),
	/** Enkidu: golden chains shooting out of ripples round the caster and winding round every enemy near. */
	ENKIDU(30, 10.0F, Hold.FOLLOW),
	/** Shields of the Treasury: golden shields come out of gates and circle the caster. */
	TREASURY_SHIELDS(80, 1.6F, Hold.FOLLOW),
	/** Enuma Elish winding up: Ea raised, its three cylinders turning and red wind drawn into it. */
	EA_CHARGE(14, 1.0F, Hold.FOLLOW),
	/** Enuma Elish: a red-black spiral tearing through space, rings of red force round it (along a beam). */
	ENUMA_ELISH(30, 32.0F, Hold.FREE),
	/** Merodach, Original Sin: a crimson blade wave edged in gold (projectile). */
	SIN_WAVE(0, 1.0F, Hold.FREE),
	/** Sha Naqba Imuru: the all-seeing eye opening over the caster, its sight sweeping out in a ring. */
	SHA_NAQBA(40, 16.0F, Hold.FOLLOW),
	/** Golden Rule (Body): golden light rising off the caster, rings of gold round them. */
	GOLDEN_RULE(60, 1.0F, Hold.FOLLOW),
	/** Divine Discus: a spinning blue-white disc of light round the chakram (projectile). */
	DISCUS(0, 1.0F, Hold.FREE),
	/** Pashupata (chakram): the destroyer's sigil on the aimed ground and a great discus of light coming down onto it. */
	PASHUPATA_DISC(26, 5.0F, Hold.FREE),

	// ------------------------------------------------------------ Mage, Grand Caster (0.5.39)
	/** Hecatic Wizard: a sky of violet magic circles over the aimed spot and the witch's circle turning on the ground under it. */
	HECATIC_SKY(52, 7.0F, Hold.FREE),
	/** One witch's magic circle a bolt flies out of (facing along its reach). */
	WITCH_CIRCLE(14, 0.8F, Hold.FREE),
	/** A bolt of violet witchcraft with runes tumbling in its wake (projectile). */
	WITCH_BOLT(0, 1.0F, Hold.FREE),
	/** High-Speed Divine Words (the dagger's): one glowing runic syllable flying spinning (projectile). */
	RUNE_SYLLABLE(0, 1.0F, Hold.FREE),
	/** Rule Breaker in the target: the zigzag rainbow dagger stabbing in and the contracts round it cracking apart in light. */
	RULE_BREAKER(24, 1.0F, Hold.FOLLOW),
	/** High-Speed Divine Words: glowing runic syllables racing round the caster, small circles flashing open at a word each. */
	DIVINE_WORDS(36, 1.0F, Hold.FOLLOW),
	/** Argon Coin: the golden fleece's curls glowing over the caster's shoulders, warm light rising. */
	GOLDEN_FLEECE(50, 1.0F, Hold.FOLLOW),
	/** All Spells Must Be Broken: the rainbow dagger thrust out as a beam, contract circles shattering along it (along a beam). */
	SPELL_BREAK(22, 14.0F, Hold.FREE),
	/** Witch's Dagger: a violet dagger arc with runes in its wake (along a cone). */
	WITCH_SLASH(10, 4.0F, Hold.FREE),
	/** Territory Creation: the witch's ward circle on the ground, a wall of light rising on its rim. */
	TERRITORY(60, 10.0F, Hold.FREE),
	/** Eightfold Blessings of Amaterasu: the eight-lobed sun mirror over the caster, a torii of light and ofuda turning round. */
	AMATERASU(60, 12.0F, Hold.FOLLOW),
	/** Enten: fire talismans dropping onto the aimed ground and bursting into pillars of flame. */
	ENTEN(36, 3.5F, Hold.FREE),
	/** Hyoten: a ring of ice talismans over the aimed ground, frost spreading under it and ice jutting up. */
	HYOTEN(40, 4.0F, Hold.FREE),
	/** One ice talisman an icicle falls out of. */
	ICE_OFUDA(14, 0.6F, Hold.FREE),
	/** Shapeshift: fox-fire wisps circling the caster and nine fox tails of light fanning behind. */
	FOX_FIRE(60, 1.0F, Hold.FOLLOW),
	/** EXPLOSION!: crimson magic circles stacking in the sky over the aimed spot, then the blast: a fire column, a mushroom cloud, a shock ring. */
	EXPLOSION(70, 8.0F, Hold.FREE),
	/** Drain Touch: green threads of life drawn from every enemy near into the caster's hand. */
	DRAIN_TOUCH(24, 3.5F, Hold.FOLLOW),
	/** Crimson Demon Pose: the crimson eyes flaring and a burst of colour behind the pose. */
	CRIMSON_POSE(30, 1.0F, Hold.FOLLOW),
	/** Manatite: a blue crystal crushed in the hand, its shards and mana flowing into the caster. */
	MANATITE(30, 1.0F, Hold.FOLLOW),
	/** Stone Sentinel Maze: eight stone pillars standing in an octagon, the bagua's trigrams turning on the ground. */
	STONE_MAZE(120, 8.0F, Hold.FREE),
	/** East Wind of Red Cliffs: streams of wind sweeping forward with embers (along a cone). */
	EAST_WIND(22, 8.0F, Hold.FREE),
	/** A fireball riding the east wind (projectile). */
	WIND_FIRE(0, 1.0F, Hold.FREE),
	/** Discerning Eye: an analysis sigil locking onto the target. */
	DISCERNING_EYE(40, 1.0F, Hold.FOLLOW),
	/** Tactician's Advice: a command banner of light behind the caster, waves of order washing out. */
	WAR_BANNER(50, 10.0F, Hold.FOLLOW),
	/** Fairy Law: the fairy's sigil over the caster, light gathering, then a blinding dome of golden-white light. */
	FAIRY_LAW(46, 16.0F, Hold.FOLLOW),
	/** Fairy Glitter: a homing orb in a ring of rainbow stars (projectile). */
	FAIRY_GLITTER(0, 1.0F, Hold.FREE),
	/** Fairy Glitter landing: a burst of rainbow stars. */
	GLITTER_BURST(18, 3.5F, Hold.FREE),
	/** Fairy Sphere: a translucent golden sphere round the caster. */
	FAIRY_SPHERE(80, 1.8F, Hold.FOLLOW),
	/** Fairy Tactician: wings of light on the caster and motes rising. */
	FAIRY_WINGS(50, 1.0F, Hold.FOLLOW),
	/** Garden of Avalon: flowers blooming over the ground, petals drifting, the tower standing beyond. */
	AVALON(200, 10.0F, Hold.FREE),
	/** Hero Creation: sparkling rings rising round the caster and a crown of light. */
	HERO_CREATION(40, 10.0F, Hold.FOLLOW),
	/** Flower Storm: an orb trailing petals (projectile). */
	PETAL_ORB(0, 1.0F, Hold.FREE),
	/** Illusion: a shimmering afterimage where the caster stood and petals to where they are. */
	ILLUSION(24, 1.0F, Hold.TRAIL),
	/** A small white magic circle a Zoltraak flies out of (facing along its reach). */
	ZOLTRAAK_CIRCLE(14, 0.8F, Hold.FREE),
	/** Zoltraak: a crisp white line of killing magic (projectile). */
	ZOLTRAAK(0, 1.0F, Hold.FREE),
	/** Judradjim: a web of lightning spread over the sky and its bolt thrown down. */
	JUDRADJIM(26, 4.5F, Hold.FREE),
	/** Defensive Magic: hexagonal barrier panels in front of the caster. */
	HEX_BARRIER(60, 1.6F, Hold.FOLLOW),
	/** Spell to Make a Field of Flowers: blue moon-weed flowers blooming out in a circle. */
	FLOWER_FIELD(160, 7.0F, Hold.FREE),
	/** Ars Almadel Salomonis: Solomon's ten rings and a circle of 72 sigils over the aimed spot, pillars of light striking down. */
	ARS_ALMADEL(80, 7.0F, Hold.FREE),
	/** Demon Pillar Eruption: fleshy demon pillars bursting out of the ground round the caster, eyes opening on them. */
	DEMON_PILLARS(30, 6.0F, Hold.FREE),
	/** Clairvoyance: a golden eye opening over the target, its sigil under it. */
	CLAIRVOYANCE(40, 1.0F, Hold.FOLLOW),
	/** Rings of Solomon: ten gold rings orbiting the caster. */
	TEN_RINGS(80, 1.4F, Hold.FOLLOW);

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

	/** Drawn on a {@link SkillProjectile} ({@code shoot(...).look(...)}) rather than shown as a {@link SkillVfx}. */
	public boolean projectile() {
		return this != NONE && this.ticks == 0;
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
