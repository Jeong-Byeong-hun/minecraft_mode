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
	TEN_RINGS(80, 1.4F, Hold.FOLLOW),

	// ------------------------------------------------------------ Pirate, Pirate King (0.5.40)
	/** Battleship: a galleon of the caster's own coming together round them, sails filling, its guns run out and firing. */
	BATTLESHIP(50, 1.0F, Hold.FOLLOW),
	/** A cannonball ablaze, a tail of fire and powder smoke behind it (projectile). */
	CANNON_SHOT(0, 1.0F, Hold.FREE),
	/** A shot bursting where it lands: a fireball, a ring of smoke and splinters flying. */
	CANNON_BLAST(18, 3.0F, Hold.FREE),
	/** Octopus: the Corsair's octopus gunner sitting in a pool of ink on the aimed ground, eight arms waving, squirting ink. */
	OCTOPUS(120, 4.0F, Hold.FREE),
	/** Air Strike: a target circle on the ground and bombs whistling down onto it out of a flight of planes passing over. */
	AIR_STRIKE(46, 3.5F, Hold.FREE),
	/** Bullseye: a red and white target board over the target, a crosshair closing on its middle. */
	BULLSEYE(40, 1.0F, Hold.FOLLOW),
	/** Golden Wild Hunt: the ghost fleet rising behind the caster, spectral galleons with golden sails, their guns run out. */
	WILD_HUNT(50, 1.0F, Hold.FOLLOW),
	/** One ghostly cannon of the fleet a shot flies out of (facing along its reach). */
	GHOST_CANNON(14, 0.7F, Hold.FREE),
	/** The Golden Hind ramming the aimed spot: a ghost galleon plunging bow first and a burst of gold coins. */
	GOLDEN_HIND(34, 5.0F, Hold.FREE),
	/** Golden Rule: a spinning gold coin of light with a golden streak (projectile). */
	COIN_SHOT(0, 1.0F, Hold.FREE),
	/** Voyager of the Storm: a ship's wheel of gold turning behind the caster, storm wind round them and a swell rolling out. */
	STORM_VOYAGE(50, 10.0F, Hold.FOLLOW),
	/** Pioneer of the Stars: a ship's wake of foam along the caster's path, stars glinting over it. */
	STAR_WAKE(24, 1.0F, Hold.TRAIL),
	/** Caribbean Free Bird: Mary's cutlass crossing the target, the pair's wings closing round it and Anne's shot flashing. */
	FREE_BIRD(26, 1.0F, Hold.FOLLOW),
	/** Caribbean Free Bird's shot: a bullet flying on wings of light (projectile). */
	BIRD_SHOT(0, 1.0F, Hold.FREE),
	/** Mary's Cutlass: a broad cutlass sweeping its arc, feathers scattering (along a cone). */
	CUTLASS_ARC(10, 4.5F, Hold.FREE),
	/** Anne's Powder Shot: a red-hot musket ball trailing sparks and smoke (projectile). */
	MUSKET_SHOT(0, 1.0F, Hold.FREE),
	/** Pirate's Glory: two birds of light, a blue and a red, circling the caster, feathers falling. */
	TWIN_BIRDS(50, 1.0F, Hold.FOLLOW),
	/** Release the Kraken: a green ghostly whirlpool on the aimed ground and the kraken's arms rising out of it. */
	KRAKEN(100, 6.0F, Hold.FREE),
	/** The kraken's great arm rearing up beyond the aimed spot and slamming down on it. */
	KRAKEN_SLAM(30, 5.0F, Hold.FREE),
	/** Tentacle Lash: a suckered tentacle sweeping its arc (along a cone). */
	TENTACLE_LASH(12, 5.0F, Hold.FREE),
	/** Do You Fear Death?: Davy Jones's face of ghost light over the caster, its beard of tentacles writhing, a sea fog rolling out. */
	DAVY_JONES(50, 10.0F, Hold.FOLLOW),
	/** Rise from the Deep: a whirlpool swallowing where the caster was and the sea bursting up where they rise. */
	DEEP_RISE(24, 1.0F, Hold.TRAIL),
	/** Ikoku Sovereignty: Napoleon's great swing, a sweep of pink and gold force (along a cone). */
	IKOKU_SWING(14, 6.0F, Hold.FREE),
	/** Ikoku Sovereignty's wave: a vast crescent of pink and gold force (projectile). */
	IKOKU_WAVE(0, 1.0F, Hold.FREE),
	/** Soul Pocus: souls pulled out of every enemy near and drawn into the caster's hand. */
	SOUL_POCUS(28, 6.0F, Hold.FOLLOW),
	/** Zeus: the thundercloud homie over the aimed spot, grinning, and the bolt it throws down. */
	ZEUS(26, 4.0F, Hold.FREE),
	/** Croquembouche Craving: a tower of cream puffs gobbled up, and Prometheus the sun blazing over the caster's shoulder. */
	CROQUEMBOUCHE(50, 1.0F, Hold.FOLLOW),
	/** Island Shaker: the air cracking like glass in front of the caster and the quake rolling out over cracked ground. */
	QUAKE(34, 8.0F, Hold.FREE),
	/** Seaquake: a wall of sea curling over, white foam on its crest (projectile). */
	TSUNAMI(0, 1.0F, Hold.FREE),
	/** Anchor Drop: a great anchor raised over the caster on its chain, and the crash where they land. */
	ANCHOR_DROP(60, 5.0F, Hold.TRAIL),
	/** My Sons: the Whitebeard Pirates' mark over the caster and light washing out to the crew. */
	MY_SONS(50, 12.0F, Hold.FOLLOW),
	/** Bajrang Gun: the caster's arm stretching up into the sky and a gigantic white fist coming down on the aimed spot. */
	BAJRANG_GUN(36, 6.0F, Hold.FREE),
	/** Red Hawk: an arm stretching out with its fist in flames (along a beam). */
	RED_HAWK(14, 10.0F, Hold.FREE),
	/** Jet Gatling: a storm of fist afterimages pounding forward (along a cone). */
	JET_GATLING(20, 4.0F, Hold.FREE),
	/** Drums of Liberation: the drumbeat pulsing out in rings of white cloud, the Sun God's mane of cloud round the caster. */
	LIBERATION_DRUMS(50, 1.0F, Hold.FOLLOW),
	/** Kamusari: a red-black slash of Conqueror's Haki with black lightning crackling round it (along a cone). */
	KAMUSARI(14, 6.0F, Hold.FREE),
	/** Kamusari's wave: a black crescent rimmed in red, black lightning crackling round it (projectile). */
	KAMUSARI_WAVE(0, 1.0F, Hold.FREE),
	/** Conqueror's Clash: black lightning bursting out of the caster in every direction, the air splitting red and black. */
	HAKI_CLASH(24, 8.0F, Hold.FREE),
	/** Will of D.: a straw hat floating down onto the caster and a crimson aura rising. */
	WILL_OF_D(50, 1.0F, Hold.FOLLOW),
	/** Dawn of the Great Pirate Era: the Pirate King's Jolly Roger flying behind the caster, treasure glittering and gold washing out. */
	PIRATE_ERA(60, 12.0F, Hold.FOLLOW),
	/** Thunderclap Broadside: the target zone on the ground, lightning crawling round its rim and shots striking it. */
	BROADSIDE(50, 6.0F, Hold.FREE),
	/** A crack of thunder a cannonball falls out of (facing along its fall). */
	THUNDER_FLASH(12, 0.8F, Hold.FREE),
	/** A cannonball wrapped in crackling lightning (projectile). */
	THUNDER_SHOT(0, 1.0F, Hold.FREE),
	/** Buckshot Storm: a roaring muzzle flash and a cone of pellets with smoke behind (along a cone). */
	BUCKSHOT(12, 6.0F, Hold.FREE),
	/** Armament Round: a bullet coated black in Armament Haki, a red sheen on it (projectile). */
	HAKI_SHOT(0, 1.0F, Hold.FREE),
	/** Recoil Leap: the blunderbuss's blast behind where the caster was and a streak of smoke along their flight. */
	RECOIL(20, 1.0F, Hold.TRAIL),

	// ------------------------------------------------------------ Warrior, King of Knights (0.5.41)
	/** Excalibur: the sword raised in a pillar of gold, light streaming into it, then swung down in a torrent of golden light (along a beam). */
	EXCALIBUR(36, 24.0F, Hold.FREE),
	/** Invisible Air unbound: the wind sheathing the sword bursting out round the caster in whirling gusts. */
	AIR_RELEASE(14, 5.0F, Hold.FREE),
	/** Strike Air: a hammer of wind, a tornado lying along its flight (projectile). */
	STRIKE_AIR(0, 1.0F, Hold.FREE),
	/** Mana Burst: jets of blue mana blasting off the caster's path. */
	MANA_BURST(20, 1.0F, Hold.TRAIL),
	/** Avalon: the blue and gold scabbard over the caster coming apart into a dome of shining fragments. */
	AVALON_SHEATH(60, 1.6F, Hold.FOLLOW),
	/** Excalibur Morgan: the black sword raised in a pillar of darkness, then a torrent of black light rimmed in red (along a beam). */
	VORTIGERN(36, 22.0F, Hold.FREE),
	/** Mana Burst (dark): black flame erupting round the caster, red at the rims, a crimson shock ring rolling out. */
	DARK_BURST(18, 5.0F, Hold.FREE),
	/** Tyrant's Charge: black mana streaming off the caster's path, the ground cracking red under it. */
	TYRANT_TRAIL(20, 1.0F, Hold.TRAIL),
	/** Black Armor: Saber Alter's dark armor: black flame rising round the caster, red lines running over it, the visor glowing. */
	BLACK_ARMOR(60, 1.0F, Hold.FOLLOW),
	/** Rhongomyniad: storm clouds over the aimed spot and the lance at the end of the world coming down out of them as a tower of light. */
	RHONGOMYNIAD(50, 5.0F, Hold.FREE),
	/** Storm Thrust: a lance of light thrust out, storm wind drilling round it (along a beam). */
	STORM_LANCE(14, 12.0F, Hold.FREE),
	/** Lion King's Charge: the lance levelled at the caster's front in a cone of storm wind, the storm spiralling along their path. */
	LION_CHARGE(20, 1.0F, Hold.TRAIL),
	/** Holy Lance Ward: the holy lance standing upright behind the caster, a storm turning round it, blessings washing out. */
	LANCE_WARD(50, 8.0F, Hold.FOLLOW),
	/** Arondight Overload: lake-blue mana overflowing off the caster in dark streams, crackling. */
	OVERLOAD_AURA(30, 1.0F, Hold.FOLLOW),
	/** One overloaded Arondight cut: a blue-black blade arc crackling with blue light (along a cone). */
	ARONDIGHT_ARC(9, 5.0F, Hold.FREE),
	/** Knight of the Lake: the lake rippling at the caster's feet, mist rising, a sword of light rising out of the water behind them. */
	LAKE_KNIGHT(50, 1.0F, Hold.FOLLOW),
	/** Lake Slash: a crescent of lake water and light throwing off spray (projectile). */
	LAKE_WAVE(0, 1.0F, Hold.FREE),
	/** Unblemished: stains flaking off the caster and turning into motes of light, a clear ring at the feet. */
	UNBLEMISHED(40, 1.0F, Hold.FOLLOW),
	/** Clarent Blood Arthur: red lightning gathering into the raised sword, then a torrent of crimson lightning (along a beam). */
	BLOOD_ARTHUR(30, 18.0F, Hold.FREE),
	/** A crimson lightning strike on the aimed ground, branching and bursting. */
	RED_BOLT(16, 5.0F, Hold.FREE),
	/** Red Lightning: crimson bolts leaping from the caster's sword from enemy to enemy. */
	RED_CHAIN(14, 12.0F, Hold.FOLLOW),
	/** Rebel's Charge: red lightning crackling along the caster's path, the ground scorched under it. */
	REBEL_TRAIL(20, 1.0F, Hold.TRAIL),
	/** Secret of Pedigree: Mordred's horned helm closing over the caster's head and a red haze swallowing their outline. */
	PEDIGREE_HELM(40, 1.0F, Hold.FOLLOW),
	/** Excalibur Galatine: a sun forming over the aimed spot and swung down onto it, bursting in a dome of solar fire. */
	GALATINE_SUN(34, 5.0F, Hold.FREE),
	/** Sunlight Blessing: the noon sun blazing over the caster, its rays falling round them and a sun sigil on the ground. */
	SUNLIGHT(50, 10.0F, Hold.FOLLOW),
	/** Solar Flare: a shell of solar fire swelling round the caster, loops of flame arching off it. */
	SOLAR_FLARE(18, 5.0F, Hold.FREE),
	/** Noon Strike: a golden blade arc trailing sun-fire (along a cone). */
	NOON_ARC(9, 5.0F, Hold.FREE),
	/** Abyssus Draconis: the target branded a dragon: a holy seal under it, dragon wings of shadow behind it, pinned by a shaft of light. */
	DRACONIS_SEAL(50, 1.0F, Hold.FOLLOW),
	/** Ascalon's thrust: a great lance of holy light and St. George's cross flaring at its tip (along a beam). */
	HOLY_LANCE(26, 16.0F, Hold.FREE),
	/** Dragonbane Thrust: a spear of light with dragon scales shattering off along it (along a beam). */
	DRAGONBANE(14, 10.0F, Hold.FREE),
	/** Martyr's Shield: a great shield of light bearing St. George's red cross in front of the caster, a halo over them. */
	MARTYR_SHIELD(50, 1.6F, Hold.FOLLOW),
	/** Saint's Ward: holy ground round the caster, St. George's cross and roses on it, light rising. */
	SAINT_WARD(50, 10.0F, Hold.FOLLOW),
	/** Ame-no-Murakumo: storm clouds gathering over the caster, a whirlwind rising and blades of wind cutting out. */
	MURAKUMO(34, 6.0F, Hold.FREE),
	/** Grass Cutter: a sweep of wind almost all the way round, cut grass flying (along a cone). */
	GRASS_CUTTER(12, 6.0F, Hold.FREE),
	/** Wind Step: streaks of wind along the caster's path, leaves whirling behind. */
	WIND_STEP(18, 1.0F, Hold.TRAIL),
	/** Eight-Headed Bane: one head of Yamata no Orochi, jaws open, its neck writhing behind (projectile). */
	SERPENT(0, 1.0F, Hold.FREE),
	/** Lord Camelot: the shield planted and the white castle rising round the caster, walls and towers of light on the Round Table's sigil. */
	CAMELOT(60, 6.0F, Hold.FREE),
	/** Bulwark of Snowflakes: snowflakes of light turning round the caster, frost at their feet. */
	SNOW_BULWARK(50, 1.6F, Hold.FOLLOW),
	/** Round Table Bash: the great round shield swept along its arc (along a cone). */
	ROUND_TABLE(10, 5.0F, Hold.FREE),
	/** Camelot Charge: a shield of light leading the caster's charge, a wall of light left along the path. */
	SHIELD_CHARGE(20, 1.0F, Hold.TRAIL),

	// ------------------------------------------------------------ Rogue, Shadow Monarch (0.5.42)
	/** Great Serpent's Lair: Kasaka, the great venom serpent, coiled round the caster as wide as the blast, rearing with its fangs bared. */
	SERPENT_LAIR(34, 6.0F, Hold.FREE),
	/** Paralytic Bite: Kasaka's two venom fangs closing on the target, green venom dripping. */
	VENOM_BITE(22, 1.0F, Hold.FOLLOW),
	/** Venom Mist: a pool of toxic green mist on the aimed ground, bubbles bursting in it. */
	VENOM_MIST(120, 5.0F, Hold.FREE),
	/** Sprint: green afterimages and streaks of speed left along the caster's path. */
	SPRINT_TRAIL(80, 1.0F, Hold.TRAIL),
	/** Knight's Bane: the hunter's mark over the target and a black-violet cross cut through it. */
	KNIGHTS_BANE(30, 1.0F, Hold.FOLLOW),
	/** Mutilation: one black-violet dagger cut (along a cone). */
	MUTILATION(7, 3.5F, Hold.FREE),
	/** Killing Intent: a red-black pressure of killing intent rolling out round the caster, a looming shadow behind them. */
	KILLING_INTENT(40, 8.0F, Hold.FOLLOW),
	/** Riposte: two daggers crossed in guard in front of the caster, a dark shield shimmering round them. */
	RIPOSTE(50, 1.0F, Hold.FOLLOW),
	/** Ant King's Rampage: Beru's claw rakes torn along the caster's charge. */
	ANT_DASH(20, 1.0F, Hold.TRAIL),
	/** Ant King's Rampage: Beru's claws slashing all round the caster, as wide as each blast. */
	ANT_FRENZY(9, 5.0F, Hold.FREE),
	/** Devour: the ant king's great mandibles closing on the target. */
	DEVOUR(20, 1.0F, Hold.FOLLOW),
	/** Stolen Healing Arts: a ring of green healing light washing out to the allies it reaches. */
	STOLEN_HEALING(40, 10.0F, Hold.FOLLOW),
	/** Wing Burst: the ant king's insect wings on the caster's back in flight and the crash where they land. */
	WING_BURST(44, 4.0F, Hold.TRAIL),
	/** Yellow Flash: yellow flashes cutting through the target from every side, formula kunai left round it. */
	YELLOW_FLASH(32, 1.0F, Hold.FOLLOW),
	/** Formula Kunai: Minato's three-pronged kunai, its seal tag streaming (projectile). */
	FORMULA_KUNAI(0, 1.0F, Hold.FREE),
	/** Flying Thunder God: a yellow flash from where the caster was to the formula seal where they appear. */
	HIRAISHIN(18, 1.0F, Hold.TRAIL),
	/** Space-Time Barrier: the Flying Thunder God formula written round the caster and a barrier flickering over them. */
	SPACE_TIME_BARRIER(50, 1.4F, Hold.FOLLOW),
	/** Susanoo: Itachi's crimson Susanoo rising round the caster, ribs, arms and the Yata Mirror. */
	SUSANOO_ITACHI(60, 1.0F, Hold.FOLLOW),
	/** The Totsuka Blade: a blade of liquid sake poured out of the sealing gourd and thrust along the beam (along a beam). */
	TOTSUKA(26, 16.0F, Hold.FREE),
	/** Amaterasu: black flames that never go out burning over the aimed ground. */
	BLACK_FLAMES(120, 3.5F, Hold.FREE),
	/** Tsukuyomi: the Mangekyo Sharingan's gaze, a red-black ray to the target under a red moon (along a beam). */
	TSUKUYOMI(30, 16.0F, Hold.FREE),
	/** Crow Clone: the caster bursting into a flock of black crows that wheel round them. */
	CROW_CLONE(50, 1.0F, Hold.FOLLOW),
	/** Monarch's Domain: the Shadow Monarch's domain spread over the ground, shadow mist and hands drawing enemies in. */
	MONARCH_DOMAIN(160, 8.0F, Hold.FREE),
	/** Ruler's Authority: an unseen hand of violet force lifting everything round the caster and slamming it down. */
	RULERS_AUTHORITY(26, 7.0F, Hold.FREE),
	/** Reaper's Chain: the chain sickle swung round on its chain (along a cone). */
	REAPER_CHAIN(10, 7.0F, Hold.FREE),
	/** Abyssal Guard: shadow knights rising round the caster, shields raised. */
	ABYSSAL_GUARD(60, 1.0F, Hold.FOLLOW),
	/** Susanoo: Kamui Shuriken: a great four-bladed shuriken of violet chakra, a space-time vortex turning in its middle (projectile). */
	KAMUI_SHURIKEN(0, 1.0F, Hold.FREE),
	/** Where a Kamui Shuriken bursts: a space-time vortex warping everything in its reach away. */
	KAMUI_VORTEX(20, 4.0F, Hold.FREE),
	/** Purple Lightning: violet lightning leaping from the caster's hand from enemy to enemy. */
	PURPLE_CHAIN(14, 16.0F, Hold.FOLLOW),
	/** Kamui: the space round the caster twisting into a vortex, the Mangekyo Sharingan over it. */
	KAMUI_WARP(40, 1.0F, Hold.FOLLOW),
	/** Kamui Raikiri: a vortex where the caster vanished, a streak of violet lightning and the Raikiri's burst where they strike. */
	KAMUI_RAIKIRI(20, 3.5F, Hold.TRAIL),
	/** Arise: purple-black smoke boiling up round the caster and shadow soldiers rising out of the ground. */
	ARISE(50, 7.0F, Hold.FREE),
	/** Dragon's Fear: Kamish's head roaring over the caster, rings of dread rolling out. */
	DRAGON_FEAR(36, 10.0F, Hold.FOLLOW),
	/** Dragon Fang Volley: a dagger wreathed in Kamish's red-black dragon flame (projectile). */
	DRAGON_FANG(0, 1.0F, Hold.FREE),
	/** Shadow Exchange: the caster sinking into a pool of shadow and rising out of another. */
	SHADOW_EXCHANGE(20, 1.0F, Hold.TRAIL),
	/** Uchiha Return: the gunbai raised, drinking in what hits it, then the stored force thrown back out in a blast. */
	UCHIHA_RETURN(34, 7.0F, Hold.FOLLOW),
	/** Majestic Destroyer Flame: a wall of fire rolling out to the aimed spot and a sea of flame on it. */
	DESTROYER_FLAME(32, 5.0F, Hold.FREE),
	/** Gunbai Sweep: the war fan swept along its arc, a gust of wind in front of it (along a cone). */
	GUNBAI_SWEEP(10, 6.0F, Hold.FREE),
	/** Gunbai Leap: the war fan raised over the leaping caster and slammed down where they land. */
	GUNBAI_LEAP(44, 4.0F, Hold.TRAIL);

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
