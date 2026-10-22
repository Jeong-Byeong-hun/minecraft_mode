package com.minecraftmode.client.vfx;

import com.minecraftmode.job.skill.SkillLook;
import java.util.EnumMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Where each drawn look's danger edge sits, as its renderer draws it (audited by hand against the renderer code, 0.5.43): a ring at
 * {@code k} x its size, a cone reaching {@code k} x its range over its arc, a beam {@code w} blocks either side of its line, a projectile
 * body {@code r} blocks across its flight, or no edge at all (a single target, a path, an aura). {@link LookRanges} compares these with
 * the area each skill really hits ({@code docs/SKILL-LOOKS.md}, {@code JobClientGameTest.checkLookRanges}). Change the entry together
 * with the renderer.
 */
public final class LookEdges {
	public enum Kind {
		/** A ring, disc, dome or wall round the spot at {@code k} x the look's size. */
		RING,
		/** A sector reaching {@code k} x the look's range over its arc. */
		CONE,
		/** Along a beam, {@code k} blocks either side of the line. */
		BEAM,
		/** Wraps the one target (no area). */
		BODY,
		/** Follows the caster's path (dash, blink). */
		TRAIL,
		/** On the caster, no danger edge (a buff, decoration). */
		AURA,
		/** A projectile's body, {@code k} blocks either side of its flight. */
		PROJECTILE
	}

	public record Edge(Kind kind, double k) {
		static Edge ring(final double k) {
			return new Edge(Kind.RING, k);
		}

		static Edge cone(final double k) {
			return new Edge(Kind.CONE, k);
		}

		static Edge beam(final double w) {
			return new Edge(Kind.BEAM, w);
		}

		static Edge body() {
			return new Edge(Kind.BODY, 0.0);
		}

		static Edge trail() {
			return new Edge(Kind.TRAIL, 0.0);
		}

		static Edge aura() {
			return new Edge(Kind.AURA, 0.0);
		}

		static Edge projectile(final double r) {
			return new Edge(Kind.PROJECTILE, r);
		}
	}

	private static final Map<SkillLook, Edge> EDGES = new EnumMap<>(SkillLook.class);

	private static void edge(final SkillLook look, final Edge edge) {
		Edge old = EDGES.put(look, edge);
		if (old != null && !old.equals(edge)) {
			throw new IllegalStateException(look + " has two edges: " + old + " and " + edge);
		}
	}

	/** The look's danger edge, or null when nobody has audited it. */
	public static @Nullable Edge of(final SkillLook look) {
		return EDGES.get(look);
	}

	static {
		// ------------------------------------------------------------ shared
		edge(SkillLook.RASENGAN, Edge.aura());             // held sphere (HELD_RADIUS 0.42)
		edge(SkillLook.RASENGAN_BURST, Edge.ring(1.0));    // ground shock ring
		edge(SkillLook.ALLY_RING, Edge.ring(1.0));         // pale ring at the allies' reach
		edge(SkillLook.LANDING_RING, Edge.ring(1.0));      // shock ring round where the leap lands

		// ------------------------------------------------------------ Soul Reaper
		edge(SkillLook.GETSUGA, Edge.projectile(1.05));      // crescent horns: radius 1.1 x sin72 either side of the path (WAVE hit 1.0)
		edge(SkillLook.GETSUGA_DUAL, Edge.projectile(1.05)); // same crescent, black inner crescent inside it
		edge(SkillLook.TAIMATSU, Edge.projectile(0.85));     // fireball glow body; the 4.0 blast is FLAME_BURST on impact
		edge(SkillLook.FLASH_TRAIL, Edge.trail());           // afterimage sheet on the path, speed lines ±1.2
		edge(SkillLook.REIATSU, Edge.aura());                // flames + ring on the caster (buff)
		edge(SkillLook.PETAL_STORM, Edge.ring(1.0));         // ground ring 0.8-1.0, petal cloud ≤1.04
		edge(SkillLook.PETAL_SPHERE, Edge.ring(1.0));        // burst petals + ground ring at 1.0 (closing sphere starts at the pull's reach)
		edge(SkillLook.SENKEI, Edge.ring(1.0));              // outer sword row + ring outer rim
		edge(SkillLook.HAKUTEIKEN, Edge.beam(0.9));          // pink glow round the blade; wings behind the hilt are decoration
		edge(SkillLook.ICE_FLOWER, Edge.body());             // flower round the stabbed target
		edge(SkillLook.ICE_PRISON, Edge.ring(1.0));          // ice ring 0.85-1.03, pillar outer faces at 1.0
		edge(SkillLook.ICE_WINGS, Edge.aura());              // wings on the caster
		edge(SkillLook.ICE_BLOOM, Edge.ring(1.0));           // frost disc + ring; cloud overhead is sky decoration
		edge(SkillLook.FLAME_BURST, Edge.ring(1.0));         // flame ring 0.85-1.05
		edge(SkillLook.FLAME_WALL, Edge.ring(1.0));          // fire wall at 1.0
		edge(SkillLook.ZANKA, Edge.ring(1.0));               // 360° blade arc rim + fire ring
		edge(SkillLook.SPEAR, Edge.beam(0.9));               // faint sheath round the hair-thin blade
		edge(SkillLook.SPEAR_KILL, Edge.beam(0.9));          // sheath; end crumble ≤0.9 ring
		edge(SkillLook.DUST, Edge.ring(1.0));                // silver ring + cracks
		edge(SkillLook.MIRROR, Edge.ring(1.0));              // ripple rings (Illusion Step: size 1, not an area)
		edge(SkillLook.KUROHITSUGI, Edge.ring(1.0));         // ground ring 0.85-1.02, coffin corners on 1.0
		edge(SkillLook.HOGYOKU, Edge.ring(1.0));             // wave ring + short wall
		edge(SkillLook.SLASH, Edge.cone(1.0));               // blade arc rim over the arc
		edge(SkillLook.CROSS_SLASH, Edge.cone(1.0));         // two arcs with their rim at 1.0
		edge(SkillLook.ABYSS, Edge.ring(1.0));               // dark pool + swell + ring
		edge(SkillLook.BINDING, Edge.body());                // cloth wound round the target
		edge(SkillLook.MUGETSU, Edge.beam(0.9));             // black crescent blade running along the beam
		edge(SkillLook.MUGETSU_VOID, Edge.ring(1.0));        // black dome + red ring
		edge(SkillLook.BONE_FANGS, Edge.ring(1.0));          // bone ring (fang bases) + ground ring
		edge(SkillLook.BONE_SNAKE, Edge.beam(0.9));          // snake sway + red glow
		edge(SkillLook.BONE_LASH, Edge.cone(1.0));           // whip tip + streak over the arc
		edge(SkillLook.BONE_CAGE, Edge.aura());              // ribs round the caster (shield)

		// ------------------------------------------------------------ Archer
		edge(SkillLook.ANTARES_MARK, Edge.body());          // red ring at the target's feet
		edge(SkillLook.PANKRATION, Edge.cone(1.0));         // force walls + cracks over the arc
		edge(SkillLook.ARROW_WARD, Edge.aura());            // circling arrows, buff
		edge(SkillLook.GALLOP, Edge.trail());               // hoofprints along the dash path
		edge(SkillLook.ULW_FIELD, Edge.ring(1.0));          // blood ring 0.95-1.05 + disc (rain circle)
		edge(SkillLook.GUN_KATA, Edge.ring(1.0));           // red ring growing to size (360 slash)
		edge(SkillLook.COUNTER_GUARDIAN, Edge.aura());      // shroud and circuits, buff
		edge(SkillLook.SUN_BEAM, Edge.beam(0.9));           // rim rings 0.78-0.90, glow fading at 1.0
		edge(SkillLook.HEEL_STRIKE, Edge.body());           // reticle and heel burst on one target
		edge(SkillLook.PAEAN, Edge.ring(1.0));              // healing rings out to the heal's reach
		edge(SkillLook.SUN_STEP, Edge.trail());             // sunlight streak (blink)
		edge(SkillLook.MOON_SIGIL, Edge.ring(1.0));         // sigil outer ring (slowness circle)
		edge(SkillLook.MOON_VEIL, Edge.aura());             // veil round the caster
		edge(SkillLook.PASHUPATA_BOLT, Edge.ring(1.0));     // rim ring, shock stops at the edge
		edge(SkillLook.ENDOWED, Edge.aura());               // halo and column, buff
		edge(SkillLook.MANA_FLAME, Edge.trail());           // flames along the dash path
		edge(SkillLook.VENUS_STAR, Edge.ring(1.0));         // gold circle, star points, shock stops at the edge
		edge(SkillLook.BEAUTY, Edge.ring(1.0));             // rose rim circle (weakness circle)
		edge(SkillLook.VENUS_WINGS, Edge.aura());           // wings
		edge(SkillLook.BABYLON_GATE, Edge.aura());          // gate where a treasure starts
		edge(SkillLook.BABYLON_SKY, Edge.ring(1.0));        // ground ring + disc (rain circle); sky gates decoration
		edge(SkillLook.ENKIDU, Edge.ring(1.0));             // gold ground ring (pull/stun circle); chains to targets inside
		edge(SkillLook.TREASURY_SHIELDS, Edge.aura());      // circling shields, buff
		edge(SkillLook.EA_CHARGE, Edge.ring(1.0));          // pull ring, wind starts at the edge
		edge(SkillLook.ENUMA_ELISH, Edge.beam(0.9));        // spirals and force rings; cracks are decoration
		edge(SkillLook.SHA_NAQBA, Edge.ring(1.0));          // sight ring sweeping out
		edge(SkillLook.GOLDEN_RULE, Edge.aura());           // gold flames on the caster
		edge(SkillLook.PASHUPATA_DISC, Edge.ring(1.0));     // circle, sigil inside, shock stops at the edge
		edge(SkillLook.STAR_ARROW, Edge.projectile(0.4));   // red glow round the arrow
		edge(SkillLook.TRACER, Edge.projectile(0.25));      // tracer glow
		edge(SkillLook.MOON_ARROW, Edge.projectile(0.32));  // moonlight glow
		edge(SkillLook.CRESCENT_ARROW, Edge.projectile(0.34)); // moon crescent horns
		edge(SkillLook.FLAME_ARROW, Edge.projectile(0.35)); // flame glow; the blast is FLAME_BURST on impact
		edge(SkillLook.GEM, Edge.projectile(0.3));          // cut gem
		edge(SkillLook.TREASURE, Edge.projectile(0.4));     // gold streak glow
		edge(SkillLook.SIN_WAVE, Edge.projectile(1.0));     // crimson crescent horns
		edge(SkillLook.DISCUS, Edge.projectile(0.4));       // disc and teeth

		// ------------------------------------------------------------ Mage
		edge(SkillLook.HECATIC_SKY, Edge.ring(1.0));       // ground witch circle (sky circles are decoration)
		edge(SkillLook.WITCH_CIRCLE, Edge.aura());         // gate a bolt flies out of
		edge(SkillLook.WITCH_BOLT, Edge.projectile(0.5));  // head halo/star
		edge(SkillLook.RULE_BREAKER, Edge.body());         // dagger + broken circle on the target
		edge(SkillLook.DIVINE_WORDS, Edge.aura());         // syllables round the caster
		edge(SkillLook.GOLDEN_FLEECE, Edge.aura());        // fleece on the caster
		edge(SkillLook.SPELL_BREAK, Edge.beam(1.05));      // rainbow bands + contract circles round the line
		edge(SkillLook.RUNE_SYLLABLE, Edge.projectile(0.35)); // syllable ring
		edge(SkillLook.WITCH_SLASH, Edge.cone(1.0));       // violet arc rim
		edge(SkillLook.TERRITORY, Edge.ring(1.0));         // ward circle + wall (allies' reach)
		edge(SkillLook.AMATERASU, Edge.ring(1.0));         // sun wash ring (allies' reach)
		edge(SkillLook.ENTEN, Edge.ring(1.0));             // fire ring on the aimed spot; each strike's ring
		edge(SkillLook.HYOTEN, Edge.ring(1.0));            // frost ring / snowflake tips
		edge(SkillLook.ICE_OFUDA, Edge.aura());            // gate talisman
		edge(SkillLook.FOX_FIRE, Edge.aura());             // fox fires + tails
		edge(SkillLook.STONE_MAZE, Edge.ring(1.0));        // pillars on the edge, bagua octagon 1.0-1.08
		edge(SkillLook.EAST_WIND, Edge.aura());            // wind behind the fireball fan (its 50°); the push(4) round the caster is knockback only
		edge(SkillLook.WIND_FIRE, Edge.projectile(0.45));  // fireball core
		edge(SkillLook.DISCERNING_EYE, Edge.body());       // reticle + eye on the target
		edge(SkillLook.WAR_BANNER, Edge.ring(1.0));        // command rings (allies' reach)
		edge(SkillLook.EXPLOSION, Edge.ring(1.0));         // chant circle, shock ring holding at the edge, scorch disc
		edge(SkillLook.DRAIN_TOUCH, Edge.ring(1.0));       // green ground ring; threads only to enemies inside
		edge(SkillLook.CRIMSON_POSE, Edge.aura());         // burst behind the pose
		edge(SkillLook.MANATITE, Edge.aura());             // crystal into the caster
		edge(SkillLook.FAIRY_LAW, Edge.ring(1.0));         // light dome + ground ring
		edge(SkillLook.FAIRY_GLITTER, Edge.projectile(0.9)); // orb star/halo; the blast is GLITTER_BURST
		edge(SkillLook.GLITTER_BURST, Edge.ring(1.0));     // gold ring = blast radius
		edge(SkillLook.FAIRY_SPHERE, Edge.aura());         // sphere on the caster
		edge(SkillLook.FAIRY_WINGS, Edge.aura());          // wings on the caster
		edge(SkillLook.AVALON, Edge.ring(1.0));            // flower field + ring (tower is decoration)
		edge(SkillLook.HERO_CREATION, Edge.ring(1.0));     // rose wash ring (allies' reach)
		edge(SkillLook.PETAL_ORB, Edge.projectile(0.3));   // bloom / halo
		edge(SkillLook.ILLUSION, Edge.trail());            // afterimage along the blink
		edge(SkillLook.ZOLTRAAK_CIRCLE, Edge.aura());      // gate circle
		edge(SkillLook.ZOLTRAAK, Edge.projectile(0.22));   // head ring
		edge(SkillLook.JUDRADJIM, Edge.ring(1.0));         // ground ring (sky web is decoration)
		edge(SkillLook.HEX_BARRIER, Edge.aura());          // hex wall on the caster
		edge(SkillLook.FLOWER_FIELD, Edge.ring(1.0));      // moon-weed field + ring
		edge(SkillLook.ARS_ALMADEL, Edge.ring(1.0));       // ground circle; each strike's ring (sky is decoration)
		edge(SkillLook.DEMON_PILLARS, Edge.ring(1.0));     // red ring; cracks/pillars inside
		edge(SkillLook.CLAIRVOYANCE, Edge.body());         // eye + circle round the target's feet
		edge(SkillLook.TEN_RINGS, Edge.aura());            // rings orbiting the caster

		// ------------------------------------------------------------ Pirate
		edge(SkillLook.BATTLESHIP, Edge.aura());          // galleon + sea round the caster (shield/boost only)
		edge(SkillLook.CANNON_SHOT, Edge.projectile(0.45)); // fire-wrapped ball (the blast is CANNON_BLAST)
		edge(SkillLook.CANNON_BLAST, Edge.ring(1.0));     // fire shock ring = blast radius
		edge(SkillLook.OCTOPUS, Edge.ring(1.0));          // rim of the ink pool
		edge(SkillLook.AIR_STRIKE, Edge.ring(1.0));       // red target ring (each strike's shock ring round its spot)
		edge(SkillLook.BULLSEYE, Edge.body());            // target board on the marked enemy
		edge(SkillLook.WILD_HUNT, Edge.aura());           // ghost fleet behind the caster
		edge(SkillLook.GHOST_CANNON, Edge.aura());        // barrage gate
		edge(SkillLook.GOLDEN_HIND, Edge.ring(1.0));      // gold ring before impact, shock ring stops at the edge
		edge(SkillLook.COIN_SHOT, Edge.projectile(0.24)); // gold coin
		edge(SkillLook.STORM_VOYAGE, Edge.ring(1.0));     // swells roll out to the allies' reach
		edge(SkillLook.STAR_WAKE, Edge.trail());          // sea wake along the dash
		edge(SkillLook.FREE_BIRD, Edge.body());           // wings and strokes round the target
		edge(SkillLook.BIRD_SHOT, Edge.projectile(0.7));  // bird of light round the bullet
		edge(SkillLook.CUTLASS_ARC, Edge.cone(1.0));      // blue sheet over the arc
		edge(SkillLook.MUSKET_SHOT, Edge.projectile(0.3)); // red-hot ball (the blast is CANNON_BLAST)
		edge(SkillLook.TWIN_BIRDS, Edge.aura());          // birds circling the caster
		edge(SkillLook.KRAKEN, Edge.ring(1.0));           // foam rim of the whirlpool
		edge(SkillLook.KRAKEN_SLAM, Edge.ring(1.0));      // ghost ring before the slam, sea ring stops at the edge
		edge(SkillLook.TENTACLE_LASH, Edge.cone(1.0));    // tentacle tip / water sheet over the arc
		edge(SkillLook.DAVY_JONES, Edge.ring(1.0));       // fog ring rolling out
		edge(SkillLook.DEEP_RISE, Edge.trail());          // whirlpool at the start, splash at the end
		edge(SkillLook.IKOKU_SWING, Edge.ring(1.0));      // shock ring round the caster = push radius
		edge(SkillLook.IKOKU_WAVE, Edge.projectile(1.1)); // pink crescent
		edge(SkillLook.SOUL_POCUS, Edge.ring(1.0));       // pink ground ring
		edge(SkillLook.ZEUS, Edge.ring(1.0));             // yellow ground ring under the cloud
		edge(SkillLook.CROQUEMBOUCHE, Edge.aura());       // tower and rings at the feet
		edge(SkillLook.QUAKE, Edge.ring(1.0));            // shell + ground ring round the feet
		edge(SkillLook.TSUNAMI, Edge.projectile(1.1));    // wall of sea across the flight
		edge(SkillLook.ANCHOR_DROP, Edge.ring(1.0));      // slam ring round the landing spot
		edge(SkillLook.MY_SONS, Edge.ring(1.0));          // rings to the allies' reach
		edge(SkillLook.BAJRANG_GUN, Edge.ring(1.0));      // sun ring before impact, shock rings stop at the edge
		edge(SkillLook.RED_HAWK, Edge.beam(0.9));         // haki fist + fire wings; end blast circle 1.0
		edge(SkillLook.JET_GATLING, Edge.cone(1.0));      // knuckles at full stretch
		edge(SkillLook.LIBERATION_DRUMS, Edge.aura());    // drumbeat rings and halo
		edge(SkillLook.KAMUSARI, Edge.cone(1.0));         // black band edged in crimson
		edge(SkillLook.KAMUSARI_WAVE, Edge.projectile(1.05)); // black crescent
		edge(SkillLook.HAKI_CLASH, Edge.ring(1.0));       // dome rings + ground ring = nova radius
		edge(SkillLook.WILL_OF_D, Edge.aura());           // straw hat + flame aura
		edge(SkillLook.PIRATE_ERA, Edge.ring(1.0));       // gold rings to the allies' reach; flag is decoration
		edge(SkillLook.BROADSIDE, Edge.ring(1.0));        // gold rim with lightning = rain circle
		edge(SkillLook.THUNDER_FLASH, Edge.aura());       // rain gate in the sky
		edge(SkillLook.THUNDER_SHOT, Edge.projectile(0.3)); // ball core
		edge(SkillLook.BUCKSHOT, Edge.cone(1.0));         // pellets over the arc
		edge(SkillLook.HAKI_SHOT, Edge.projectile(0.3));  // black slug
		edge(SkillLook.RECOIL, Edge.ring(1.0));           // shock ring at the start = push radius, then the trail

		// ------------------------------------------------------------ Warrior
		edge(SkillLook.EXCALIBUR, Edge.beam(0.9));        // white rims along the torrent
		edge(SkillLook.AIR_RELEASE, Edge.ring(1.0));      // gust ring at the push's reach
		edge(SkillLook.STRIKE_AIR, Edge.projectile(1.0)); // tornado mouth ring at the head
		edge(SkillLook.MANA_BURST, Edge.trail());         // mana jets along the dash
		edge(SkillLook.AVALON_SHEATH, Edge.aura());       // fragment dome on the caster
		edge(SkillLook.VORTIGERN, Edge.beam(0.9));        // crimson rims along the black torrent
		edge(SkillLook.DARK_BURST, Edge.ring(1.0));       // crimson ring at the blast edge
		edge(SkillLook.TYRANT_TRAIL, Edge.trail());       // red cracks along the path
		edge(SkillLook.BLACK_ARMOR, Edge.aura());         // dark armor on the caster
		edge(SkillLook.RHONGOMYNIAD, Edge.ring(1.0));     // gold ground ring and disc (lightning radius)
		edge(SkillLook.STORM_LANCE, Edge.beam(0.9));      // storm rings and strands round the line
		edge(SkillLook.LION_CHARGE, Edge.trail());        // storm spiral along the path
		edge(SkillLook.LANCE_WARD, Edge.ring(1.0));       // gold rings to the allies' reach
		edge(SkillLook.DRACONIS_SEAL, Edge.body());       // seal, wings and pin on the target
		edge(SkillLook.HOLY_LANCE, Edge.beam(0.9));       // holy rings down the lance
		edge(SkillLook.DRAGONBANE, Edge.beam(0.9));       // holy rings down the lance
		edge(SkillLook.MARTYR_SHIELD, Edge.ring(1.0));    // red taunt ring
		edge(SkillLook.SAINT_WARD, Edge.ring(1.0));       // pale circle and wall at the heal's reach
		edge(SkillLook.OVERLOAD_AURA, Edge.aura());       // lake mana flaring off the caster
		edge(SkillLook.ARONDIGHT_ARC, Edge.cone(1.0));    // white edge 0.95, glow fading to 1.05
		edge(SkillLook.LAKE_KNIGHT, Edge.aura());         // lake at the feet, sword behind
		edge(SkillLook.LAKE_WAVE, Edge.projectile(1.05)); // crescent
		edge(SkillLook.UNBLEMISHED, Edge.aura());         // flakes and rings on the caster
		edge(SkillLook.BLOOD_ARTHUR, Edge.beam(0.9));     // red rims along the lightning torrent
		edge(SkillLook.RED_BOLT, Edge.ring(1.0));         // red ring at the strike edge
		edge(SkillLook.RED_CHAIN, Edge.body());           // bolts leaping to the struck enemies
		edge(SkillLook.REBEL_TRAIL, Edge.trail());        // red lightning along the path
		edge(SkillLook.PEDIGREE_HELM, Edge.aura());       // helm and haze on the caster
		edge(SkillLook.GALATINE_SUN, Edge.ring(1.0));     // ground ring and sun-fire dome
		edge(SkillLook.SUNLIGHT, Edge.ring(1.0));         // rings to the allies' reach
		edge(SkillLook.SOLAR_FLARE, Edge.ring(1.0));      // fire shell and ground ring
		edge(SkillLook.NOON_ARC, Edge.cone(1.0));         // golden band and bright bar
		edge(SkillLook.MURAKUMO, Edge.ring(1.0));         // whirlwind base ring and blade rings (nova radius)
		edge(SkillLook.GRASS_CUTTER, Edge.cone(1.0));     // wind band and white bar
		edge(SkillLook.WIND_STEP, Edge.trail());          // wind streaks along the path
		edge(SkillLook.SERPENT, Edge.projectile(1.0));    // teal halo round the head
		edge(SkillLook.CAMELOT, Edge.ring(1.0));          // curtain wall and ground circle
		edge(SkillLook.SNOW_BULWARK, Edge.ring(1.0));     // frost rings to the allies' reach
		edge(SkillLook.ROUND_TABLE, Edge.cone(1.0));      // band, rim bar and shield rim
		edge(SkillLook.SHIELD_CHARGE, Edge.trail());      // wall of light along the path

		// ------------------------------------------------------------ Rogue
		edge(SkillLook.SERPENT_LAIR, Edge.ring(1.0));      // venom ring + outside of the coil
		edge(SkillLook.VENOM_BITE, Edge.body());           // jaws on the target
		edge(SkillLook.VENOM_MIST, Edge.ring(1.0));        // mist edge ring
		edge(SkillLook.SPRINT_TRAIL, Edge.trail());        // speed lines, no damage
		edge(SkillLook.KNIGHTS_BANE, Edge.body());         // mark + cross cut on the target
		edge(SkillLook.MUTILATION, Edge.cone(1.0));        // crescent outer edge
		edge(SkillLook.KILLING_INTENT, Edge.ring(1.0));    // red ring at the debuff edge
		edge(SkillLook.RIPOSTE, Edge.aura());              // crossed daggers + guard
		edge(SkillLook.ANT_DASH, Edge.trail());            // claw rakes ±1.2 across the path
		edge(SkillLook.ANT_FRENZY, Edge.ring(1.0));        // violet ring at the nova edge
		edge(SkillLook.DEVOUR, Edge.body());               // mandibles on the target
		edge(SkillLook.STOLEN_HEALING, Edge.ring(1.0));    // heal reach ring
		edge(SkillLook.WING_BURST, Edge.ring(1.0));        // shock ring round the landing
		edge(SkillLook.YELLOW_FLASH, Edge.body());         // streaks through the target
		edge(SkillLook.FORMULA_KUNAI, Edge.projectile(0.3)); // kunai + soft glow
		edge(SkillLook.HIRAISHIN, Edge.trail());           // streak to the arrival
		edge(SkillLook.SPACE_TIME_BARRIER, Edge.aura());   // barrier round the caster
		edge(SkillLook.SUSANOO_ITACHI, Edge.aura());       // Susanoo body (shield)
		edge(SkillLook.TOTSUKA, Edge.beam(0.9));           // side rims + rolling rings
		edge(SkillLook.BLACK_FLAMES, Edge.ring(1.0));      // ember ring + rim flames
		edge(SkillLook.TSUKUYOMI, Edge.beam(0.9));         // rims/rings, end eye ring
		edge(SkillLook.CROW_CLONE, Edge.aura());           // crows round the caster
		edge(SkillLook.MONARCH_DOMAIN, Edge.ring(1.0));    // ring + wall at the domain edge
		edge(SkillLook.RULERS_AUTHORITY, Edge.ring(1.0));  // ring + slam wave
		edge(SkillLook.REAPER_CHAIN, Edge.cone(1.0));      // streak along the arc edge
		edge(SkillLook.ABYSSAL_GUARD, Edge.aura());        // shield knights round the caster
		edge(SkillLook.KAMUI_SHURIKEN, Edge.projectile(0.93)); // chakra blades (contact ring 0.3; blast is KAMUI_VORTEX)
		edge(SkillLook.KAMUI_VORTEX, Edge.ring(1.0));      // rim circle = blast sphere outline
		edge(SkillLook.PURPLE_CHAIN, Edge.body());         // bolts to each enemy struck
		edge(SkillLook.KAMUI_WARP, Edge.aura());           // vortex + eye over the caster
		edge(SkillLook.KAMUI_RAIKIRI, Edge.ring(1.0));     // ring round the blink's arrival
		edge(SkillLook.ARISE, Edge.ring(1.0));             // ring + wall at the nova edge
		edge(SkillLook.DRAGON_FEAR, Edge.ring(1.0));       // red ring at the debuff edge
		edge(SkillLook.DRAGON_FANG, Edge.projectile(0.3)); // flame-wrapped blade
		edge(SkillLook.SHADOW_EXCHANGE, Edge.trail());     // pools + line to the arrival
		edge(SkillLook.UCHIHA_RETURN, Edge.ring(1.0));     // fire ring + blast wave
		edge(SkillLook.DESTROYER_FLAME, Edge.ring(1.0));   // flame-sea ring/disc on the aimed strike
		edge(SkillLook.GUNBAI_SWEEP, Edge.cone(1.0));      // outer wind band
		edge(SkillLook.GUNBAI_LEAP, Edge.ring(1.0));       // ring round the landing

		// ------------------------------------------------------------ Hunter
		edge(SkillLook.GODSPEED_STEP, Edge.trail());        // lightning streak to behind the target
		edge(SkillLook.GODSPEED_AURA, Edge.aura());         // crackling on the caster
		edge(SkillLook.THUNDERBOLT, Edge.ring(1.0));        // lit ring and disc on the aimed spot
		edge(SkillLook.NARUKAMI, Edge.body());              // bolts hop between struck enemies
		edge(SkillLook.JUDGMENT_CHAIN, Edge.beam(0.9));     // guide lines and rings along the beam
		edge(SkillLook.STEAL_CHAIN, Edge.ring(1.0));        // red ground ring round the caster
		edge(SkillLook.CHAIN_JAIL, Edge.ring(1.0));         // ring, disc and chain posts on the aimed spot
		edge(SkillLook.SCARLET_EYES, Edge.aura());          // eyes and chains round the caster
		edge(SkillLook.GON_RAGE, Edge.aura());              // aura
		edge(SkillLook.JAJANKEN_ROCK, Edge.ring(1.0));      // shock ring round the landing point
		edge(SkillLook.LONG_SCISSORS, Edge.beam(0.9));      // guide lines from the fingers to the tip
		edge(SkillLook.GON_ROCK, Edge.ring(1.0));           // preview ring growing to the blast rim
		edge(SkillLook.ROCK_BLAST, Edge.ring(1.0));         // rim ring, dome and wall
		edge(SkillLook.SUN_MOON, Edge.body());              // seals round the marked target
		edge(SkillLook.SEAL_CHAIN, Edge.body());            // streaks hop between struck enemies
		edge(SkillLook.INDOOR_FISH, Edge.projectile(0.3));  // fish body
		edge(SkillLook.FUN_CLOTH, Edge.aura());             // cloth wrapping the caster
		edge(SkillLook.BANDIT_BOOK, Edge.aura());           // book over the hand
		edge(SkillLook.PAGE_GATE, Edge.aura());             // page where a shot starts
		edge(SkillLook.STOLEN_SKILL, Edge.projectile(0.32)); // page band round the orb
		edge(SkillLook.DRAGON_HEAD, Edge.projectile(1.0));  // circle round the dragon's head
		edge(SkillLook.DRAGON_BURST, Edge.ring(1.0));       // blast sphere
		edge(SkillLook.DRAGON_LANCE, Edge.beam(0.9));       // guide lines along the beam
		edge(SkillLook.ZENO_AURA, Edge.aura());             // coiled dragon
		edge(SkillLook.DRAGON_DIVE, Edge.ring(1.0));        // rain circle ring and disc; the sky dragon is decoration
		edge(SkillLook.DIVE_DRAGON, Edge.projectile(0.3));  // dragon body
		edge(SkillLook.SILVA_ORB, Edge.projectile(0.32));   // orb shell
		edge(SkillLook.ORB_BURST, Edge.ring(1.0));          // blast sphere
		edge(SkillLook.SILVA_PRESENCE, Edge.ring(1.0));     // silver ground ring
		edge(SkillLook.ASSASSIN_HAND, Edge.body());         // clawed hand through the target
		edge(SkillLook.TAIL_PIERCE, Edge.beam(0.9));        // guide lines along the beam
		edge(SkillLook.PHOTON, Edge.ring(1.0));             // rim ring and blade sweeps
		edge(SkillLook.KINGS_AURA, Edge.ring(1.0));         // ground ring; the shadow is decoration
		edge(SkillLook.KING_ROSE, Edge.ring(1.0));          // fire ring and outer petals; the cloud rose is decoration
		edge(SkillLook.NETERO_PRAYER, Edge.aura());         // faint Guanyin and motes
		edge(SkillLook.GUANYIN_SWEEP, Edge.cone(1.0));      // arc rim
		edge(SkillLook.PALM_SLAM, Edge.ring(1.0));          // gold rim ring
		edge(SkillLook.ZERO_HAND, Edge.ring(1.0));          // gold ring, wall and palms on the aimed spot; the statue is decoration
		edge(SkillLook.ZERO_BEAM, Edge.beam(0.9));          // edge lines, wheels and mouth ring
		edge(SkillLook.NEEDLE_RAIN, Edge.ring(1.0));        // violet ring and disc on the aimed spot
		edge(SkillLook.NEN_NEEDLE, Edge.projectile(0.3));   // faint sprite round the needle
		edge(SkillLook.NEEDLE_DISGUISE, Edge.aura());       // needles in the caster's head
		edge(SkillLook.NEEDLE_PUPPET, Edge.body());         // needle and threads on the target
	}

	private LookEdges() {
	}
}
