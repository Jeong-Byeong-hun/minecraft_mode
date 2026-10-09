package com.minecraftmode.client.creature;

import static com.minecraftmode.client.creature.BodyPlan.box;
import static com.minecraftmode.client.creature.Skin.Pattern.*;

import com.minecraftmode.client.creature.BodyPlan.Builder;
import com.minecraftmode.client.creature.BodyPlan.Part;
import com.minecraftmode.client.creature.BodyPlan.Rig;
import com.minecraftmode.client.creature.BodyPlan.Role;
import com.minecraftmode.client.creature.PlanKit.Humanoid;
import java.util.List;

/** Body plans of the six raid bosses (ids match their entity types). */
final class BossPlans {
	static List<BodyPlan> all() {
		return List.of(arachne(), gorvath(), kraken(), ignis(), malachar(), aethryx());
	}

	/** Arachne, Brood Queen: a drider — a huge spider body with a pale queen's torso, crown and fangs. */
	private static BodyPlan arachne() {
		Builder b = BodyPlan.builder("arachne", Rig.ARACHNID).scale(2.3F)
			.skin("chitin", Skin.of(0x1E1418, 0xC0263A, CHITIN))
			.skin("abdomen", Skin.glowing(0x221418, 0xFF3A4A, CRACKED))
			.skin("skin", Skin.of(0xD8C8D8, 0x8A6A8A, SMOOTH))
			.skin("hair", Skin.of(0x1A1A24, 0x4A2A5A, FUR))
			.skin("crown", Skin.glowing(0x8A1A2A, 0xFF6A7A, CRYSTAL))
			.skin("leg", Skin.of(0x2A1A1E, 0x8A2A3A, CHITIN));
		b.part("body", null, Role.BODY, 0, 15, 0, "chitin", box(-6, -4, -7, 12, 8, 12));
		b.part("abdomen", "body", Role.EXTRA, 0, -2, 5, "abdomen", box(-8, -7, 0, 16, 13, 17));
		PlanKit.spiderLegs(b, "body", 5, 1, -5, 3, 22, "leg");
		// the queen's torso rises from the front of the thorax
		b.part("torso", "body", Role.EXTRA, 0, -4, -5, "skin", box(-4, -12, -2.5F, 8, 12, 5));
		Part head = b.part("head", "torso", Role.HEAD, 0, -12, 0, "skin", box(-4, -8, -4, 8, 8, 8));
		head.eyes = 8;
		head.eyeColor = 0xFF2030;
		b.part("hair", "head", Role.EXTRA, 0, -8, 1, "hair", box(-4.5F, 0, -1, 9, 10, 5));
		b.part("crown", "head", Role.EXTRA, 0, -8, 0, "crown", box(-4, -3, -4, 8, 3, 8));
		b.part("fangs", "head", Role.JAW, 0, -1, -4, "chitin", box(-2, 0, -1, 4, 3, 1));
		b.part("arm_r", "torso", Role.ARM_RIGHT, -5.5F, -10, 0, "skin", box(-1.5F, -1, -1.5F, 3, 13, 3));
		b.part("arm_l", "torso", Role.ARM_LEFT, 5.5F, -10, 0, "skin", box(-1.5F, -1, -1.5F, 3, 13, 3));
		b.part("claw_r", "arm_r", Role.EXTRA, 0, 11, 0, "chitin", box(-2, 0, -3, 4, 6, 4));
		b.part("claw_l", "arm_l", Role.EXTRA, 0, 11, 0, "chitin", box(-2, 0, -3, 4, 6, 4));
		return b.build();
	}

	/** Gorvath, the Mountain Colossus: a hill of stone and moss with a glowing magma heart. */
	private static BodyPlan gorvath() {
		Builder b = BodyPlan.builder("gorvath", Rig.BIPED).scale(3.4F)
			.skin("stone", Skin.of(0x6A6A70, 0x4E7A2E, STONE))
			.skin("moss", Skin.of(0x4E7A2E, 0x2E5A1E, FUR))
			.skin("core", Skin.glowing(0x3A1A10, 0xFF8A20, CRACKED))
			.skin("crystal", Skin.glowing(0x5A8ACF, 0xC0E8FF, CRYSTAL));
		PlanKit.humanoid(b, new Humanoid(10, 9, 9, 20, 18, 12, 8, 22, 8, 12), "stone", "stone", "stone", "stone", 2, 0xFFB030);
		b.get("body").xRot = 0.12F;
		b.part("heart", "body", Role.EXTRA, 0, 5, -6, "core", box(-4, 0, -1, 8, 8, 2));
		b.part("moss_back", "body", Role.EXTRA, 0, -1, 0, "moss", box(-11, -3, -7, 22, 4, 14));
		b.part("boulder_r", "arm_r", Role.EXTRA, 0, -3, 0, "stone", box(-6, -4, -6, 12, 7, 12));
		b.part("boulder_l", "arm_l", Role.EXTRA, 0, -3, 0, "stone", box(-6, -4, -6, 12, 7, 12));
		b.part("fist_r", "arm_r", Role.EXTRA, 0, 17, 0, "stone", box(-5.5F, 0, -5.5F, 11, 8, 11));
		b.part("fist_l", "arm_l", Role.EXTRA, 0, 17, 0, "stone", box(-5.5F, 0, -5.5F, 11, 8, 11));
		b.part("brow", "head", Role.EXTRA, 0, -6, -4.5F, "moss", box(-5, -1, -1, 10, 2, 1));
		for (int i = 0; i < 3; i++) {
			b.part("shard" + i, "body", Role.EXTRA, -6 + i * 6, -2, 4, "crystal", box(-1.5F, -8 + i, -1.5F, 3, 8 - i, 3)).zRot = (i - 1) * 0.3F;
		}
		return b.build();
	}

	/** The Kraken of the Abyss: a towering mantle over eight curling tentacles, golden eyes and a beak. */
	private static BodyPlan kraken() {
		Builder b = BodyPlan.builder("kraken", Rig.OCTOPUS).scale(3.0F)
			.skin("flesh", Skin.of(0x8A2A4A, 0xE07090, SPOTS))
			.skin("under", Skin.of(0xC86A7A, 0xF0A8B8, SMOOTH))
			.skin("beak", Skin.of(0x2A1A1A, 0x5A4A3A, CHITIN));
		b.part("body", null, Role.BODY, 0, 4, 0, "flesh", box(-8, -18, -8, 16, 18, 16), box(-6, -24, -6, 12, 6, 12));
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
		Builder b = BodyPlan.builder("ignis", Rig.FLYER).scale(3.0F)
			.skin("feather", Skin.glowing(0xC8341C, 0xFFB030, FEATHER))
			.skin("flame", Skin.glowing(0xE8501A, 0xFFF0A0, FLAME))
			.skin("beak", Skin.of(0xE8C45A, 0x8A6A1A, SMOOTH))
			.skin("talon", Skin.of(0x2A1A10, 0x6A4A2A, CHITIN));
		b.part("body", null, Role.BODY, 0, 8, 0, "feather", box(-5, -5, -8, 10, 10, 16));
		String neck = PlanKit.chain(b, "neck", "body", Role.NECK, 0, 0, -3, -8, 2, 5, 5, 0.9F, -1, -0.35F, "feather");
		Part head = b.part("head", neck, Role.HEAD, 0, 0, -5, "feather", box(-3.5F, -4, -7, 7, 7, 7));
		head.eyes = 2;
		head.eyeColor = 0xFFFFE0;
		b.part("beak", "head", Role.JAW, 0, 0, -7, "beak", box(-1.5F, -1, -4, 3, 3, 4));
		b.part("crest", "head", Role.EXTRA, 0, -4, -2, "flame", box(-0.5F, -7, -1, 1, 7, 6)).xRot = 0.5F;
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			Role role = side < 0 ? Role.WING_RIGHT : Role.WING_LEFT;
			b.part("wing_" + s, "body", role, side * 5, -3, -2, "flame", box(side < 0 ? -14 : 0, -1, -6, 14, 1, 13));
			b.part("wingtip_" + s, "wing_" + s, role, side * 14, 0, 0, "flame", box(side < 0 ? -12 : 0, -1, -5, 12, 1, 11));
			b.part("leg_" + (side < 0 ? "rt" : "lt"), "body", Role.EXTRA, side * 2.5F, 5, 2, "talon", box(-1, 0, -1, 2, 6, 2));
		}
		for (int i = 0; i < 3; i++) {
			Part plume = b.part("plume" + i, "body", Role.TAIL, i, (i - 1) * 3, -2, 8, "flame", box(-1.5F, -0.5F, 0, 3, 1, 18));
			plume.yRot = (i - 1) * 0.35F;
			plume.xRot = 0.25F;
		}
		return b.build();
	}

	/** Malachar, the Lich King: a floating crowned skeleton in royal robes with a soul staff and orbiting skulls. */
	private static BodyPlan malachar() {
		Builder b = BodyPlan.builder("malachar", Rig.FLOATER).scale(2.2F)
			.skin("robe", Skin.of(0x2A1A3A, 0xD9B44A, CLOTH))
			.skin("bone", Skin.of(0xD8D0B8, 0x5A5040, BONE))
			.skin("gold", Skin.of(0xD9B44A, 0xFFF0A0, METAL))
			.skin("soul", Skin.glowing(0x1A4A3A, 0x7CFFB0, CRYSTAL));
		b.part("body", null, Role.BODY, 0, -6, 0, "robe", box(-5, 0, -3, 10, 16, 6));
		b.part("hem", "body", Role.EXTRA, 0, 16, 0, "robe", box(-6, 0, -4, 12, 10, 8));
		Part head = b.part("head", null, Role.HEAD, 0, -6, 0, "bone", box(-4, -9, -4, 8, 9, 8));
		head.eyes = 2;
		head.eyeColor = 0x7CFFB0;
		b.part("crown", "head", Role.EXTRA, 0, -9, 0, "gold", box(-4.5F, -2, -4.5F, 9, 2, 9), box(-4.5F, -5, -4.5F, 2, 3, 2), box(2.5F, -5, -4.5F, 2, 3, 2),
			box(-1, -6, -4.5F, 2, 4, 2), box(-4.5F, -5, 2.5F, 2, 3, 2), box(2.5F, -5, 2.5F, 2, 3, 2));
		b.part("jaw", "head", Role.JAW, 0, -1, -1, "bone", box(-3, 0, -3, 6, 2, 4));
		b.part("pauldrons", "body", Role.EXTRA, 0, -1, 0, "gold", box(-8, -1, -4, 16, 4, 8));
		b.part("arm_r", null, Role.ARM_RIGHT, -6.5F, -4, 0, "bone", box(-1.5F, -1, -1.5F, 3, 16, 3));
		b.part("arm_l", null, Role.ARM_LEFT, 6.5F, -4, 0, "bone", box(-1.5F, -1, -1.5F, 3, 16, 3));
		b.part("staff", "arm_r", Role.EXTRA, 0, 14, 0, "gold", box(-0.5F, -22, -0.5F, 1, 30, 1));
		b.part("staff_orb", "staff", Role.EXTRA, 0, -22, 0, "soul", box(-2.5F, -5, -2.5F, 5, 5, 5));
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
		Builder b = BodyPlan.builder("aethryx", Rig.FLYER).scale(2.8F)
			.skin("scale", Skin.glowing(0x150F22, 0xB080FF, VOID))
			.skin("belly", Skin.of(0x2A1E44, 0x5A3A8A, SCALES))
			.skin("horn", Skin.of(0x0E0A14, 0x7A4AC8, BONE))
			.skin("membrane", Skin.glowing(0x2A1048, 0xD0A0FF, VOID));
		b.part("body", null, Role.BODY, 0, 8, 0, "scale", box(-7, -6, -13, 14, 12, 26), box(-5, 5, -10, 10, 2, 20));
		String neck = PlanKit.chain(b, "neck", "body", Role.NECK, 0, 0, -3, -13, 4, 6, 8, 0.92F, -1, -0.12F, "scale");
		Part head = b.part("head", neck, Role.HEAD, 0, 0, -7, "scale", box(-5, -4, -12, 10, 8, 12));
		head.eyes = 2;
		head.eyeColor = 0xE0A0FF;
		b.part("jaw", "head", Role.JAW, 0, 3, -2, "belly", box(-4, 0, -10, 8, 2, 10));
		b.part("horn_r", "head", Role.EXTRA, -3.5F, -4, -1, "horn", box(-1, -1, 0, 2, 2, 10)).xRot = 0.5F;
		b.part("horn_l", "head", Role.EXTRA, 3.5F, -4, -1, "horn", box(-1, -1, 0, 2, 2, 10)).xRot = 0.5F;
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			Role role = side < 0 ? Role.WING_RIGHT : Role.WING_LEFT;
			b.part("wing_" + s, "body", role, side * 7, -5, -6, "membrane", box(side < 0 ? -24 : 0, -1, -2, 24, 2, 18));
			b.part("wingtip_" + s, "wing_" + s, role, side * 24, 0, 0, "membrane", box(side < 0 ? -16 : 0, -0.5F, -2, 16, 1, 16));
		}
		float[][] legs = {{5, -9}, {-5, -9}, {5, 9}, {-5, 9}};
		for (int i = 0; i < 4; i++) {
			b.part("leg" + i, "body", Role.LEG_QUAD, i, legs[i][0], 5, legs[i][1], "belly", box(-2, 0, -2, 4, 10, 4));
		}
		String tail = PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -2, 13, 6, 6, 10, 0.82F, 1, 0.08F, "scale");
		b.part("tail_spike", tail, Role.EXTRA, 0, 0, 3, "horn", box(-0.5F, -3, 0, 1, 3, 4));
		for (int i = 0; i < 5; i++) {
			b.part("spine" + i, "body", Role.EXTRA, 0, -6, -10 + i * 5, "horn", box(-0.5F, -4 + (i % 2), -1, 1, 4 - (i % 2), 2));
		}
		return b.build();
	}

	private BossPlans() {
	}
}
