package com.minecraftmode.client.creature;

import static com.minecraftmode.client.creature.BodyPlan.box;
import static com.minecraftmode.client.creature.Skin.Pattern.*;

import com.minecraftmode.client.creature.BodyPlan.Builder;
import com.minecraftmode.client.creature.BodyPlan.Part;
import com.minecraftmode.client.creature.BodyPlan.Rig;
import com.minecraftmode.client.creature.BodyPlan.Role;
import com.minecraftmode.client.creature.PlanKit.Humanoid;
import java.util.List;

/**
 * Body plans of the nine raid bosses (ids match their entity types). Their outlines stay inside the sizes their hitboxes were
 * measured from (build/creature-preview.png); detail goes on the surface and in what they carry.
 */
final class BossPlans {
	static List<BodyPlan> all() {
		return List.of(verdessa(), arachne(), gorvath(), nefrekhet(), kraken(), ignis(), raijin(), malachar(), aethryx(), SukunaPlans.sukuna(),
			SukunaPlans.mahoraga(), SukunaPlans.shrine());
	}

	/** Verdessa, the Thornmother: a tall dryad of bark and leaves with a crown of branches and thorny vine arms. */
	private static BodyPlan verdessa() {
		Builder b = BodyPlan.builder("verdessa", Rig.BIPED).scale(1.65F).shaded()
			.skin("bark", Skin.of(0x5A3E26, 0x8A6A3A, BARK))
			.skin("leaf", Skin.of(0x4E8A2E, 0x8AD04A, FUR))
			.skin("face", Skin.of(0xA8C878, 0x6A8A3A, SMOOTH))
			.skin("bloom", Skin.glowing(0xC84A8A, 0xFFB0D8, CRYSTAL))
			.skin("thorn", Skin.of(0x3A2A16, 0x6A5030, BONE));
		PlanKit.humanoid(b, new Humanoid(7, 8, 7, 9, 13, 6, 3, 15, 4, 15), "face", "bark", "bark", "bark", 2, 0xB8FF6A);
		b.part("leaf_skirt", "body", Role.EXTRA, 0, 10, 0, "leaf", box(-6, 0, -4, 12, 7, 8));
		b.part("bark_strips", "leaf_skirt", Role.EXTRA, 0, 7, 0, "bark", box(-5.5F, 0, -3.5F, 2, 3, 1), box(-1, 0, -3.5F, 2, 4, 1), box(3.5F, 0, -3.5F, 2, 2, 1));
		b.part("leaf_hair", "head", Role.EXTRA, 0, -8, 1, "leaf", box(-4.5F, 0, -2, 9, 11, 6));
		b.part("hair_fall", "body", Role.CAPE, 0, 0, 3.2F, "leaf", box(-4, 0, 0, 8, 12, 1));
		for (int i = 0; i < 4; i++) {
			Part branch = b.part("branch" + i, "head", Role.EXTRA, -3 + i * 2, -8, 0, "bark", box(-0.5F, -7 + (i % 2) * 2, -0.5F, 1, 7 - (i % 2) * 2, 1));
			branch.zRot = (i - 1.5F) * 0.35F;
		}
		b.part("bloom", "head", Role.EXTRA, 2, -9, -2, "bloom", box(-1.5F, -3, -1.5F, 3, 3, 3));
		b.part("bloom2", "head", Role.EXTRA, -3, -7, -1, "bloom", box(-1, -2, -1, 2, 2, 2));
		b.part("heart", "body", Role.EXTRA, 0, 4, -3.1F, "bloom", box(-1.5F, -1.5F, -0.5F, 3, 3, 1));
		for (String s : new String[] {"r", "l"}) {
			b.part("pauldron_" + s, "arm_" + s, Role.EXTRA, 0, -2, 0, "leaf", box(-2.5F, -2, -2.5F, 5, 2, 5));
			b.part("thorns_" + s, "arm_" + s, Role.EXTRA, 0, 0, 0, "thorn", box(-2.5F, 3, -0.5F, 1, 1, 1), box(1.5F, 7, -0.5F, 1, 1, 1), box(-0.5F, 10, -2.5F, 1, 1, 1));
			Part vine = b.part("vine_" + s, "arm_" + s, Role.EXTRA, 0, 13, 0, "leaf", box(-1, 0, -1, 2, 8, 2));
			b.part("vine_thorns_" + s, vine.name, Role.EXTRA, 0, 0, 0, "thorn", box(-2, 2, -0.5F, 1, 1, 1), box(1, 5, -0.5F, 1, 1, 1));
			b.part("toes_" + s, "shin_" + s, Role.EXTRA, 0, 6, 0, "bark", box(-2.5F, 0, -4, 5, 1, 3), box(-0.5F, 0, 1.5F, 1, 1, 2));
		}
		b.part("roots", "body", Role.EXTRA, 0, 14, 0, "bark", box(-5, 0, -3, 10, 2, 6));
		return b.build();
	}

	/** Nefrekhet, the Sand Pharaoh: a tall mummy king in linen wraps, a striped gold headdress, crook and flail. */
	private static BodyPlan nefrekhet() {
		Builder b = BodyPlan.builder("nefrekhet", Rig.BIPED).scale(1.6F).shaded()
			.skin("wrap", Skin.of(0xD8C8A0, 0x8A7A5A, CLOTH))
			.skin("gold", Skin.of(0xE0B030, 0x2A4AA0, METAL))
			.skin("lapis", Skin.of(0x2A4AA0, 0xE0B030, STONE))
			.skin("cape", Skin.of(0x2A4AA0, 0xE0B030, CLOTH))
			.skin("gem", Skin.glowing(0x3A8A6A, 0x7CFFD0, CRYSTAL));
		PlanKit.humanoid(b, new Humanoid(8, 8, 8, 8, 12, 5, 3, 13, 4, 14), "wrap", "wrap", "wrap", "wrap", 2, 0x7CFFD0);
		b.part("nemes", "head", Role.EXTRA, 0, -8, 0, "gold", box(-5, -1, -5, 10, 6, 10), box(-5.5F, 5, -3, 2, 7, 4), box(3.5F, 5, -3, 2, 7, 4));
		b.part("uraeus", "nemes", Role.EXTRA, 0, -1, -5, "gem", box(-1, -3, -1, 2, 3, 1));
		b.part("beard", "head", Role.EXTRA, 0, 0, -3, "lapis", box(-1, 0, -1, 2, 4, 1));
		b.part("collar", "body", Role.EXTRA, 0, 0, 0, "gold", box(-6, -1, -3.5F, 12, 3, 7));
		b.part("amulet", "collar", Role.EXTRA, 0, 3, -3.5F, "gem", box(-1.5F, 0, -0.6F, 3, 2, 1));
		b.part("cape", "body", Role.CAPE, 0, 1, 2.8F, "cape", box(-5, 0, 0, 10, 16, 1));
		b.part("belt", "body", Role.EXTRA, 0, 10, 0, "gold", box(-4.5F, 0, -3, 9, 2, 6));
		b.part("kilt", "body", Role.EXTRA, 0, 12, 0, "lapis", box(-4.5F, 0, -3, 9, 5, 6));
		for (String s : new String[] {"r", "l"}) {
			b.part("bracer_" + s, "arm_" + s, Role.EXTRA, 0, 7, 0, "gold", box(-1.5F, 0, -1.5F, 3, 3, 3, 0.3F));
			b.part("loose_wrap_" + s, "arm_" + s, Role.CAPE, 0, 3, 1.5F, "wrap", box(-1, 0, 0, 2, 7, 1));
			b.part("anklet_" + s, "shin_" + s, Role.EXTRA, 0, 4, 0, "gold", box(-2, 0, -2, 4, 1, 4, 0.3F));
		}
		b.part("crook", "arm_r", Role.EXTRA, 0, 11, -1, "gold", box(-0.5F, -6, -0.5F, 1, 14, 1), box(-0.5F, -8, -3, 1, 2, 3));
		b.part("flail", "arm_l", Role.EXTRA, 0, 11, -1, "gold", box(-0.5F, -4, -0.5F, 1, 10, 1), box(-2, -6, -0.5F, 4, 2, 1));
		return b.build();
	}

	/** Raijin, Lord of the Thunderclouds: a horned storm god on a cloud with a ring of drums behind him. */
	private static BodyPlan raijin() {
		Builder b = BodyPlan.builder("raijin", Rig.FLOATER).scale(1.8F).shaded()
			.skin("skin", Skin.of(0x3A6AC8, 0x8AB8FF, SMOOTH))
			.skin("cloud", Skin.of(0xE8EEF8, 0xB8C8E0, FUR))
			.skin("tiger", Skin.of(0xE8A030, 0x1A1A1A, SPOTS))
			.skin("drum", Skin.of(0x8A2A1A, 0xE0B030, METAL))
			.skin("bolt", Skin.glowing(0xC8A020, 0xFFF080, CRYSTAL))
			.skin("fang", Skin.of(0xF4F0E0, 0xC8C0A8, BONE));
		b.part("body", null, Role.BODY, 0, -6, 0, "skin", box(-6, 0, -3.5F, 12, 13, 7));
		b.part("loincloth", "body", Role.EXTRA, 0, 13, 0, "tiger", box(-6.5F, 0, -4, 13, 6, 8));
		Part sash = b.part("sash", "body", Role.EXTRA, 0, 6, -3.6F, "bolt", box(-8, -0.5F, -0.5F, 16, 1, 1));
		sash.zRot = 0.7F;
		Part cloud = b.part("cloud", "body", Role.EXTRA, 0, 19, 0, "cloud", box(-9, 0, -7, 18, 5, 14), box(-6, 3, -5, 12, 3, 10));
		Part puffs = b.part("puffs", cloud.name, Role.ORBIT, 0, 2, 0, "cloud");
		for (int i = 0; i < 4; i++) {
			double a = Math.PI * 2 * i / 4;
			b.part("puff" + i, puffs.name, Role.EXTRA, (float)Math.cos(a) * 10, 0, (float)Math.sin(a) * 8, "cloud", box(-2, -1.5F, -2, 4, 3, 4));
		}
		Part head = b.part("head", null, Role.HEAD, 0, -6, 0, "skin", box(-4, -8, -4, 8, 8, 8));
		head.eyes = 2;
		head.eyeColor = 0xFFF080;
		b.part("horn_r", "head", Role.EXTRA, -2.5F, -8, -1, "bolt", box(-0.5F, -4, -0.5F, 1, 4, 1)).zRot = -0.3F;
		b.part("horn_l", "head", Role.EXTRA, 2.5F, -8, -1, "bolt", box(-0.5F, -4, -0.5F, 1, 4, 1)).zRot = 0.3F;
		b.part("hair", "head", Role.EXTRA, 0, -8, 2, "cloud", box(-5, -2, -2, 10, 6, 5));
		for (int i = 0; i < 3; i++) {
			b.part("wild_hair" + i, "hair", Role.EXTRA, (i - 1) * 4, -2, 2, "cloud", box(-1, -3, -1, 2, 3, 2)).zRot = (i - 1) * 0.6F;
		}
		Part jaw = b.part("jaw", "head", Role.JAW, 0, -1, -1, "skin", box(-3, 0, -3, 6, 2, 4));
		b.part("fangs", jaw.name, Role.EXTRA, 0, 0, -3, "fang", box(-2.5F, -1.5F, -0.5F, 1, 2, 1), box(1.5F, -1.5F, -0.5F, 1, 2, 1));
		b.part("arm_r", null, Role.ARM_RIGHT, -7.5F, -4, 0, "skin", box(-2, -1, -2, 4, 14, 4));
		b.part("arm_l", null, Role.ARM_LEFT, 7.5F, -4, 0, "skin", box(-2, -1, -2, 4, 14, 4));
		b.part("band_r", "arm_r", Role.EXTRA, 0, 2, 0, "tiger", box(-2, 0, -2, 4, 2, 4, 0.3F));
		b.part("band_l", "arm_l", Role.EXTRA, 0, 2, 0, "tiger", box(-2, 0, -2, 4, 2, 4, 0.3F));
		b.part("drumstick_r", "arm_r", Role.EXTRA, 0, 12, 0, "drum", box(-0.5F, 0, -0.5F, 1, 7, 1));
		b.part("drumstick_l", "arm_l", Role.EXTRA, 0, 12, 0, "drum", box(-0.5F, 0, -0.5F, 1, 7, 1));
		b.part("orbit", "body", Role.ORBIT, 0, 4, 6, "drum");
		for (int i = 0; i < 6; i++) {
			double a = Math.PI * 2 * i / 6;
			Part drum = b.part("drum" + i, "orbit", Role.EXTRA, (float)Math.cos(a) * 13, 0, (float)Math.sin(a) * 13, "drum", box(-2.5F, -2.5F, -1.5F, 5, 5, 3));
			drum.yRot = (float)(-a);
			b.part("drum_face" + i, drum.name, Role.EXTRA, 0, 0, -1.5F, "bolt", box(-1.5F, -1.5F, -0.6F, 3, 3, 1));
		}
		return b.build();
	}

	/** Arachne, Brood Queen: a drider — a huge spider body with a pale queen's torso, crown and fangs. */
	private static BodyPlan arachne() {
		Builder b = BodyPlan.builder("arachne", Rig.ARACHNID).scale(2.3F).shaded()
			.skin("chitin", Skin.of(0x1E1418, 0xC0263A, CHITIN))
			.skin("abdomen", Skin.glowing(0x221418, 0xFF3A4A, CRACKED))
			.skin("skin", Skin.of(0xD8C8D8, 0x8A6A8A, SMOOTH))
			.skin("hair", Skin.of(0x1A1A24, 0x4A2A5A, FUR))
			.skin("crown", Skin.glowing(0x8A1A2A, 0xFF6A7A, CRYSTAL))
			.skin("leg", Skin.of(0x2A1A1E, 0x8A2A3A, CHITIN));
		b.part("body", null, Role.BODY, 0, 15, 0, "chitin", box(-6, -4, -7, 12, 8, 12));
		Part abdomen = b.part("abdomen", "body", Role.EXTRA, 0, -2, 5, "abdomen", box(-8, -7, 0, 16, 13, 17));
		b.part("hourglass", abdomen.name, Role.EXTRA, 0, -7, 5, "crown", box(-2, -0.6F, 0, 4, 1, 2), box(-1, -0.6F, 2, 2, 1, 2), box(-2, -0.6F, 4, 4, 1, 2));
		b.part("spinnerets", abdomen.name, Role.EXTRA, 0, 1, 17, "chitin", box(-2, -1, 0, 4, 2, 2));
		PlanKit.jointedSpiderLegs(b, "body", 4, 5, 1, -5, 3, 16, 3, "leg", "chitin");
		// the queen's torso rises from the front of the thorax
		Part torso = b.part("torso", "body", Role.EXTRA, 0, -4, -5, "skin", box(-4, -12, -2.5F, 8, 12, 5));
		b.part("bodice", torso.name, Role.EXTRA, 0, -6, 0, "chitin", box(-4, 0, -2.5F, 8, 6, 5, 0.3F));
		Part head = b.part("head", "torso", Role.HEAD, 0, -12, 0, "skin", box(-4, -8, -4, 8, 8, 8));
		head.eyes = 8;
		head.eyeColor = 0xFF2030;
		b.part("hair", "head", Role.EXTRA, 0, -8, 1, "hair", box(-4.5F, 0, -1, 9, 10, 5));
		b.part("hair_fall", torso.name, Role.CAPE, 0, -12, 3, "hair", box(-4, 0, 0, 8, 14, 1));
		Part crown = b.part("crown", "head", Role.EXTRA, 0, -8, 0, "crown", box(-4, -3, -4, 8, 3, 8));
		b.part("crown_spikes", crown.name, Role.EXTRA, 0, -3, 0, "crown", box(-4, -2, -4, 1, 2, 1), box(-0.5F, -3, -4, 1, 3, 1), box(3, -2, -4, 1, 2, 1));
		b.part("fangs", "head", Role.JAW, 0, -1, -4, "chitin", box(-2, 0, -1, 4, 3, 1));
		b.part("arm_r", "torso", Role.ARM_RIGHT, -5.5F, -10, 0, "skin", box(-1.5F, -1, -1.5F, 3, 13, 3));
		b.part("arm_l", "torso", Role.ARM_LEFT, 5.5F, -10, 0, "skin", box(-1.5F, -1, -1.5F, 3, 13, 3));
		b.part("pauldron_r", "arm_r", Role.EXTRA, 0, -1, 0, "chitin", box(-2.5F, -2, -2.5F, 5, 3, 5));
		b.part("pauldron_l", "arm_l", Role.EXTRA, 0, -1, 0, "chitin", box(-2.5F, -2, -2.5F, 5, 3, 5));
		b.part("claw_r", "arm_r", Role.EXTRA, 0, 11, 0, "chitin", box(-2, 0, -3, 4, 6, 4));
		b.part("claw_l", "arm_l", Role.EXTRA, 0, 11, 0, "chitin", box(-2, 0, -3, 4, 6, 4));
		return b.build();
	}

	/** Gorvath, the Mountain Colossus: a hill of stone and moss with a glowing magma heart. */
	private static BodyPlan gorvath() {
		Builder b = BodyPlan.builder("gorvath", Rig.BIPED).scale(3.4F).shaded()
			.skin("stone", Skin.of(0x6A6A70, 0x4E7A2E, STONE))
			.skin("moss", Skin.of(0x4E7A2E, 0x2E5A1E, FUR))
			.skin("core", Skin.glowing(0x3A1A10, 0xFF8A20, CRACKED))
			.skin("crystal", Skin.glowing(0x5A8ACF, 0xC0E8FF, CRYSTAL));
		PlanKit.humanoid(b, new Humanoid(10, 9, 9, 20, 18, 12, 8, 22, 8, 12), "stone", "stone", "stone", "stone", 2, 0xFFB030);
		b.get("body").xRot = 0.12F;
		b.part("heart", "body", Role.EXTRA, 0, 5, -6, "core", box(-4, 0, -1, 8, 8, 2));
		b.part("moss_back", "body", Role.EXTRA, 0, -1, 0, "moss", box(-11, -3, -7, 22, 4, 14));
		b.part("moss_beard", "head", Role.EXTRA, 0, 0, -4.6F, "moss", box(-3.5F, 0, -0.5F, 7, 4, 1));
		b.part("brow", "head", Role.EXTRA, 0, -6, -4.5F, "moss", box(-5, -1, -1, 10, 2, 1));
		b.part("head_crystal", "head", Role.EXTRA, 3, -9, 1, "crystal", box(-1, -3, -1, 2, 3, 2)).zRot = 0.3F;
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			String arm = "arm_" + s;
			Part boulder = b.part("boulder_" + s, arm, Role.EXTRA, 0, -3, 0, "stone", box(-6, -4, -6, 12, 7, 12));
			b.part("boulder_spike_" + s, boulder.name, Role.EXTRA, side * 3, -4, 0, "stone", box(-1.5F, -4, -1.5F, 3, 4, 3)).zRot = side * 0.4F;
			b.part("vein_" + s, arm, Role.EXTRA, 0, 6, -4.1F, "core", box(-0.5F, 0, -0.5F, 1, 9, 1));
			b.part("fist_" + s, arm, Role.EXTRA, 0, 17, 0, "stone", box(-5.5F, 0, -5.5F, 11, 8, 11));
			b.part("toes_" + s, "shin_" + s, Role.EXTRA, 0, 4, 0, "stone", box(-5, 0, -6, 10, 2, 4));
			b.part("moss_drape_" + s, arm, Role.CAPE, 0, 2, 4.5F, "moss", box(-3, 0, 0, 6, 8, 1));
		}
		for (int i = 0; i < 3; i++) {
			b.part("shard" + i, "body", Role.EXTRA, -6 + i * 6, -2, 4, "crystal", box(-1.5F, -8 + i, -1.5F, 3, 8 - i, 3)).zRot = (i - 1) * 0.3F;
		}
		return b.build();
	}

	/** The Kraken of the Abyss: a towering mantle over eight curling tentacles, golden eyes and a beak. */
	private static BodyPlan kraken() {
		Builder b = BodyPlan.builder("kraken", Rig.OCTOPUS).scale(3.0F).shaded()
			.skin("flesh", Skin.of(0x8A2A4A, 0xE07090, SPOTS))
			.skin("under", Skin.of(0xC86A7A, 0xF0A8B8, SMOOTH))
			.skin("beak", Skin.of(0x2A1A1A, 0x5A4A3A, CHITIN))
			.skin("ridge", Skin.of(0x5A1A30, 0x8A2A4A, SCALES));
		b.part("body", null, Role.BODY, 0, 4, 0, "flesh", box(-8, -18, -8, 16, 18, 16), box(-6, -24, -6, 12, 6, 12));
		for (int side = -1; side <= 1; side += 2) {
			b.part("fin_" + (side < 0 ? "r" : "l"), "body", Role.EXTRA, side * 6, -22, 0, "ridge", box(side < 0 ? -5 : 0, -1, -5, 5, 2, 10)).zRot = side * -0.5F;
			b.part("brow_" + (side < 0 ? "r" : "l"), "body", Role.EXTRA, side * 3.5F, -11, -8, "ridge", box(-3, -1, -1, 6, 2, 1)).zRot = side * -0.2F;
		}
		b.part("crest", "body", Role.EXTRA, 0, -24, 0, "ridge", box(-1, -2, -5, 2, 2, 10));
		Part head = b.part("head", "body", Role.HEAD, 0, -4, -8, "under", box(-7, -6, -1, 14, 6, 1));
		head.eyes = 2;
		head.eyeColor = 0xFFD040;
		b.part("beak", "body", Role.JAW, 0, 0, -4, "beak", box(-2, 0, -2, 4, 3, 4));
		for (int t = 0; t < 8; t++) {
			double a = Math.PI * 2 * t / 8;
			float x = (float)Math.cos(a) * 6;
			float z = (float)Math.sin(a) * 6;
			Part root = b.part("tentacle_root" + t, "body", Role.EXTRA, x, 0, z, "flesh");
			root.yRot = (float)(-a + Math.PI / 2);
			PlanKit.chain(b, "tentacle" + t + "_", "tentacle_root" + t, Role.TENTACLE, t * 10, 0, 0, 0, 4, 4, 9, 0.8F, 1, -0.25F, t % 2 == 0 ? "flesh" : "under");
		}
		return b.build();
	}

	/** Ignis, the Infernal Phoenix: a fire bird with burning wings and a fan of flame plumes. */
	private static BodyPlan ignis() {
		Builder b = BodyPlan.builder("ignis", Rig.FLYER).scale(3.0F).shaded().folds()
			.skin("feather", Skin.glowing(0xC8341C, 0xFFB030, FEATHER))
			.skin("flame", Skin.glowing(0xE8501A, 0xFFF0A0, FLAME))
			.skin("beak", Skin.of(0xE8C45A, 0x8A6A1A, SMOOTH))
			.skin("talon", Skin.of(0x2A1A10, 0x6A4A2A, CHITIN));
		b.part("body", null, Role.BODY, 0, 8, 0, "feather", box(-5, -5, -8, 10, 10, 16));
		b.part("breast", "body", Role.EXTRA, 0, 1, -7, "flame", box(-4, -3, -2, 8, 7, 3));
		String neck = PlanKit.chain(b, "neck", "body", Role.NECK, 0, 0, -3, -8, 2, 5, 5, 0.9F, -1, -0.35F, "feather");
		Part head = b.part("head", neck, Role.HEAD, 0, 0, -5, "feather", box(-3.5F, -4, -7, 7, 7, 7));
		head.eyes = 2;
		head.eyeColor = 0xFFFFE0;
		b.part("beak", "head", Role.JAW, 0, 0, -7, "beak", box(-1.5F, -1, -4, 3, 3, 4));
		b.part("beak_hook", "head", Role.EXTRA, 0, -1, -10, "beak", box(-1, 0, -1.5F, 2, 2, 1));
		Part crest = b.part("crest", "head", Role.EXTRA, 0, -4, -2, "flame", box(-0.5F, -7, -1, 1, 7, 6));
		crest.xRot = 0.5F;
		b.part("crest2", "head", Role.EXTRA, 0, -4, 1, "flame", box(-0.5F, -5, 0, 1, 5, 4)).xRot = 0.9F;
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			Role role = side < 0 ? Role.WING_RIGHT : Role.WING_LEFT;
			// chords start at the leading edge so a folded wing hangs down the flank
			b.part("wing_" + s, "body", role, 0, side * 5, -3, -7, "flame", box(side < 0 ? -14 : 0, -1, -1, 14, 1, 13));
			b.part("wingtip_" + s, "wing_" + s, role, 1, side * 14, 0, 0, "flame", box(side < 0 ? -12 : 0, -1, -1, 12, 1, 11));
			b.part("primaries_" + s, "wingtip_" + s, Role.EXTRA, side * 12, 0, 0, "feather",
				box(side < 0 ? -4 : 0, -1, 0, 4, 1, 2), box(side < 0 ? -5 : 0, -1, 4, 5, 1, 2), box(side < 0 ? -4 : 0, -1, 8, 4, 1, 2));
			Part leg = b.part("leg_" + (side < 0 ? "rt" : "lt"), "body", Role.EXTRA, side * 2.5F, 5, 2, "talon", box(-1, 0, -1, 2, 6, 2));
			b.part("talons_" + s, leg.name, Role.EXTRA, 0, 6, 0, "talon", box(-1.5F, 0, -3, 3, 1, 4));
		}
		for (int i = 0; i < 5; i++) {
			Part plume = b.part("plume" + i, "body", Role.TAIL, i, (i - 2) * 2, -2, 8, "flame", box(-1.5F, -0.5F, 0, 3, 1, 18 - Math.abs(i - 2) * 3));
			plume.yRot = (i - 2) * 0.22F;
			plume.xRot = 0.25F;
		}
		return b.build();
	}

	/** Malachar, the Lich King: a floating crowned skeleton in royal robes with a soul staff and orbiting skulls. */
	private static BodyPlan malachar() {
		Builder b = BodyPlan.builder("malachar", Rig.FLOATER).scale(2.2F).shaded()
			.skin("robe", Skin.of(0x2A1A3A, 0xD9B44A, CLOTH))
			.skin("bone", Skin.of(0xD8D0B8, 0x5A5040, BONE))
			.skin("gold", Skin.of(0xD9B44A, 0xFFF0A0, METAL))
			.skin("soul", Skin.glowing(0x1A4A3A, 0x7CFFB0, CRYSTAL));
		b.part("body", null, Role.BODY, 0, -6, 0, "robe", box(-5, 0, -3, 10, 16, 6));
		b.part("ribs", "body", Role.EXTRA, 0, 3, -3.1F, "bone", box(-3, 0, -0.5F, 6, 1, 1), box(-3, 2, -0.5F, 6, 1, 1), box(-3, 4, -0.5F, 6, 1, 1));
		b.part("soul_core", "body", Role.EXTRA, 0, 3, -3, "soul", box(-1, 0, -0.4F, 2, 5, 1));
		b.part("hem", "body", Role.EXTRA, 0, 16, 0, "robe", box(-6, 0, -4, 12, 10, 8));
		b.part("tatters", "hem", Role.EXTRA, 0, 10, 0, "robe", box(-6, 0, -4, 2, 4, 1), box(-1, 0, -4, 3, 2, 1), box(4, 0, -4, 2, 5, 1), box(-4, 0, 3, 3, 4, 1),
			box(2, 0, 3, 2, 3, 1));
		Part head = b.part("head", null, Role.HEAD, 0, -6, 0, "bone", box(-4, -9, -4, 8, 9, 8));
		head.eyes = 2;
		head.eyeColor = 0x7CFFB0;
		Part crown = b.part("crown", "head", Role.EXTRA, 0, -9, 0, "gold", box(-4.5F, -2, -4.5F, 9, 2, 9), box(-4.5F, -5, -4.5F, 2, 3, 2),
			box(2.5F, -5, -4.5F, 2, 3, 2), box(-1, -6, -4.5F, 2, 4, 2), box(-4.5F, -5, 2.5F, 2, 3, 2), box(2.5F, -5, 2.5F, 2, 3, 2));
		b.part("crown_flames", crown.name, Role.EXTRA, 0, -5, 0, "soul", box(-4, -2, -4, 1, 2, 1), box(3, -2, -4, 1, 2, 1), box(-0.5F, -3, -4, 1, 2, 1));
		b.part("jaw", "head", Role.JAW, 0, -1, -1, "bone", box(-3, 0, -3, 6, 2, 4));
		Part pauldrons = b.part("pauldrons", "body", Role.EXTRA, 0, -1, 0, "gold", box(-8, -1, -4, 16, 4, 8));
		b.part("pauldron_spikes", pauldrons.name, Role.EXTRA, 0, -1, 0, "bone", box(-7.5F, -3, -0.5F, 1, 3, 1), box(6.5F, -3, -0.5F, 1, 3, 1));
		b.part("arm_r", null, Role.ARM_RIGHT, -6.5F, -4, 0, "bone", box(-1.5F, -1, -1.5F, 3, 16, 3));
		b.part("arm_l", null, Role.ARM_LEFT, 6.5F, -4, 0, "bone", box(-1.5F, -1, -1.5F, 3, 16, 3));
		b.part("sleeve_r", "arm_r", Role.EXTRA, 0, 0, 0, "robe", box(-2, -1, -2, 4, 8, 4));
		b.part("sleeve_l", "arm_l", Role.EXTRA, 0, 0, 0, "robe", box(-2, -1, -2, 4, 8, 4));
		b.part("fingers_l", "arm_l", Role.EXTRA, 0, 15, 0, "bone", box(-1.5F, 0, -1.5F, 1, 3, 1), box(0.5F, 0, -1.5F, 1, 3, 1), box(-0.5F, 0, 0.5F, 1, 2, 1));
		b.part("staff", "arm_r", Role.EXTRA, 0, 14, 0, "gold", box(-0.5F, -22, -0.5F, 1, 30, 1));
		Part orb = b.part("staff_orb", "staff", Role.EXTRA, 0, -22, 0, "soul", box(-2.5F, -5, -2.5F, 5, 5, 5));
		b.part("orb_claws", orb.name, Role.EXTRA, 0, 0, 0, "gold", box(-3, -2, -0.5F, 1, 3, 1), box(2, -2, -0.5F, 1, 3, 1), box(-0.5F, -2, -3, 1, 3, 1));
		b.part("cape", "body", Role.CAPE, 0, 0, 3, "robe", box(-6, 0, 0, 12, 24, 1));
		b.part("orbit", "body", Role.ORBIT, 0, 6, 0, "bone");
		for (int i = 0; i < 3; i++) {
			double a = Math.PI * 2 * i / 3;
			Part skull = b.part("skull" + i, "orbit", Role.EXTRA, (float)Math.cos(a) * 14, 0, (float)Math.sin(a) * 14, "bone", box(-2.5F, -2.5F, -2.5F, 5, 5, 5));
			skull.eyes = 2;
			skull.eyeColor = 0x7CFFB0;
		}
		return b.build();
	}

	/** Aethryx, the Void Dragon: a vast black dragon of starlight — long neck and tail, horned head, four legs, two-part wings. */
	private static BodyPlan aethryx() {
		Builder b = BodyPlan.builder("aethryx", Rig.FLYER).scale(2.8F).shaded().folds()
			.skin("scale", Skin.glowing(0x150F22, 0xB080FF, VOID))
			.skin("belly", Skin.of(0x2A1E44, 0x5A3A8A, SCALES))
			.skin("horn", Skin.of(0x0E0A14, 0x7A4AC8, BONE))
			.skin("membrane", Skin.glowing(0x2A1048, 0xD0A0FF, VOID))
			.skin("core", Skin.glowing(0x3A1A6A, 0xE0B0FF, CRACKED));
		b.part("body", null, Role.BODY, 0, 8, 0, "scale", box(-7, -6, -13, 14, 12, 26), box(-5, 5, -10, 10, 2, 20));
		b.part("chest_core", "body", Role.EXTRA, 0, 1, -13, "core", box(-3, -3, -0.6F, 6, 6, 1));
		String neck = PlanKit.chain(b, "neck", "body", Role.NECK, 0, 0, -3, -13, 4, 6, 8, 0.92F, -1, -0.12F, "scale");
		for (int i = 0; i < 4; i++) {
			b.part("neck_spine" + i, "neck" + i, Role.EXTRA, 0, -3, -4, "horn", box(-0.5F, -3, -1, 1, 3, 2));
		}
		Part head = b.part("head", neck, Role.HEAD, 0, 0, -7, "scale", box(-5, -4, -12, 10, 8, 12));
		head.eyes = 2;
		head.eyeColor = 0xE0A0FF;
		Part jaw = b.part("jaw", "head", Role.JAW, 0, 3, -2, "belly", box(-4, 0, -10, 8, 2, 10));
		b.part("teeth", jaw.name, Role.EXTRA, 0, 0, -9, "horn", box(-3.5F, -1, -0.5F, 1, 1, 1), box(2.5F, -1, -0.5F, 1, 1, 1), box(-0.5F, -1, -0.5F, 1, 1, 1));
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			Part horn = b.part("horn_" + s, "head", Role.EXTRA, side * 3.5F, -4, -1, "horn", box(-1, -1, 0, 2, 2, 10));
			horn.xRot = 0.5F;
			b.part("horn_tip_" + s, horn.name, Role.EXTRA, 0, 0, 10, "horn", box(-0.5F, -0.5F, 0, 1, 1, 5)).xRot = -0.5F;
			Role role = side < 0 ? Role.WING_RIGHT : Role.WING_LEFT;
			Part wing = b.part("wing_" + s, "body", role, 0, side * 7, -5, -6, "membrane", box(side < 0 ? -24 : 0, -1, -2, 24, 2, 18));
			b.part("wing_bone_" + s, wing.name, Role.EXTRA, 0, -0.5F, 0, "horn", box(side < 0 ? -24 : 0, -1, -2.5F, 24, 1, 1));
			Part tip = b.part("wingtip_" + s, "wing_" + s, role, 1, side * 24, 0, 0, "membrane", box(side < 0 ? -16 : 0, -0.5F, -2, 16, 1, 16));
			b.part("wing_claw_" + s, tip.name, Role.EXTRA, 0, -0.5F, -2, "horn", box(-1, -2, -1, 2, 2, 2));
		}
		float[][] legs = {{5, -9}, {-5, -9}, {5, 9}, {-5, 9}};
		for (int i = 0; i < 4; i++) {
			b.part("leg" + i, "body", Role.LEG_QUAD, i, legs[i][0], 5, legs[i][1], "belly", box(-2, 0, -2, 4, 5, 4));
			b.part("shin" + i, "leg" + i, Role.SHIN, i, 0, 5, 0, "scale", box(-1.5F, 0, -1.5F, 3, 5, 3));
			b.part("claws" + i, "shin" + i, Role.EXTRA, 0, 4, 0, "horn", box(-2, 0, -3.5F, 4, 1, 5));
		}
		String tail = PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -2, 13, 6, 6, 10, 0.82F, 1, 0.08F, "scale");
		for (int i = 0; i < 6; i++) {
			b.part("tail_spine" + i, "tail" + i, Role.EXTRA, 0, -2, 3, "horn", box(-0.5F, -2, -1, 1, 2, 2));
		}
		b.part("tail_spike", tail, Role.EXTRA, 0, 0, 3, "horn", box(-0.5F, -3, 0, 1, 3, 4));
		for (int i = 0; i < 5; i++) {
			b.part("spine" + i, "body", Role.EXTRA, 0, -6, -10 + i * 5, "horn", box(-0.5F, -4 + (i % 2), -1, 1, 4 - (i % 2), 2));
		}
		return b.build();
	}

	private BossPlans() {
	}
}
