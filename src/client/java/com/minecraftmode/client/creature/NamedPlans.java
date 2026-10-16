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
 * Body plans of the 22 named monsters (ids match their entity types): the humanoid and floating ones here, the beasts in
 * {@link NamedBeastPlans}. Humanoid legs bend at the knee and end in feet ({@link PlanKit#humanoid}).
 */
final class NamedPlans {
	static List<BodyPlan> all() {
		return List.of(
			goldenEnderman(), goblinWarchief(), banditCaptain(), bogHag(), NamedBeastPlans.frostAlpha(), NamedBeastPlans.duneScorpion(), caveTroll(),
			myconidShaman(), NamedBeastPlans.jungleStalker(), NamedBeastPlans.madPig(), drownedCorsair(), badlandsGunslinger(), wendigo(), amethystSentinel(),
			ancientTreant(), NamedBeastPlans.magmaBehemoth(), soulReaper(), witherKnight(), voidWatcher(), NamedBeastPlans.echoStalker(), chorusWraith(),
			astralKnight()
		);
	}

	/** Boots over the lower leg (shin_r/shin_l of {@link PlanKit#humanoid}), with a toe cap over the foot. */
	private static void boots(final Builder b, final Humanoid h, final String skin) {
		int shin = h.legL() - (h.legL() + 1) / 2;
		float half = h.legW() / 2.0F;
		int top = Math.max(1, shin / 2);
		for (String s : new String[] {"r", "l"}) {
			b.part("boot_" + s, "shin_" + s, Role.EXTRA, 0, 0, 0, skin, box(-half, top, -half, h.legW(), shin - top, h.legW(), 0.3F),
				box(-half, shin - 1, -half - 1, h.legW(), 1, 1, 0.3F));
		}
	}

	/** A strap across the chest from one shoulder to the other hip. */
	private static void sash(final Builder b, final Humanoid h, final String skin) {
		float depth = h.bodyD() / 2.0F + 0.1F;
		Part front = b.part("sash", "body", Role.EXTRA, 0, h.bodyH() / 2.0F, -depth, skin, box(-h.bodyW() * 0.75F, -0.5F, -0.5F, Math.round(h.bodyW() * 1.5F), 1, 1));
		front.zRot = 0.75F;
	}

	private static BodyPlan goldenEnderman() {
		Builder b = BodyPlan.builder("golden_enderman", Rig.BIPED).scale(0.95F).shaded()
			.skin("gold", Skin.of(0xC89A2E, 0xFFE58A, METAL))
			.skin("head", Skin.of(0xB8862A, 0xFFE58A, SMOOTH))
			.skin("gem", Skin.glowing(0x8A2ACF, 0xE080FF, CRYSTAL));
		PlanKit.humanoid(b, new Humanoid(8, 8, 8, 8, 12, 4, 2, 30, 2, 30), "head", "gold", "gold", "gold", 2, 0xE080FF);
		b.part("crown", "head", Role.EXTRA, 0, -8, 0, "gem", box(-3, -2, -3, 6, 2, 6));
		b.part("crown_spike", "crown", Role.EXTRA, 0, -2, 0, "gem", box(-1, -2, -1, 2, 2, 2));
		b.part("jaw", "head", Role.JAW, 0, -1, 0, "head", box(-3.5F, 0, -3.5F, 7, 2, 7));
		b.part("collar", "body", Role.EXTRA, 0, 0, 0, "gem", box(-4.5F, -0.5F, -2.5F, 9, 1, 5));
		b.part("pad_r", "arm_r", Role.EXTRA, 0, -2, 0, "gold", box(-1.5F, -1, -1.5F, 3, 2, 3));
		b.part("pad_l", "arm_l", Role.EXTRA, 0, -2, 0, "gold", box(-1.5F, -1, -1.5F, 3, 2, 3));
		b.part("orbit", "body", Role.ORBIT, 0, 5, 0, "gem");
		for (int i = 0; i < 3; i++) {
			double a = Math.PI * 2 * i / 3;
			b.part("gem" + i, "orbit", Role.EXTRA, (float)Math.cos(a) * 7, 0, (float)Math.sin(a) * 7, "gem", box(-1, -1, -1, 2, 2, 2));
		}
		return b.build();
	}

	private static BodyPlan goblinWarchief() {
		Builder b = BodyPlan.builder("goblin_warchief", Rig.BIPED).shaded()
			.skin("skin", Skin.of(0x6FAF3A, 0x9ACD5A, SMOOTH))
			.skin("leather", Skin.of(0x6B4A2B, 0xC9A227, CLOTH))
			.skin("bone", Skin.of(0xE8E0C8, 0x8A8070, BONE))
			.skin("iron", Skin.of(0x8A929E, 0xC0262D, METAL));
		Humanoid h = new Humanoid(9, 7, 8, 8, 9, 5, 3, 11, 4, 8);
		PlanKit.humanoid(b, h, "skin", "leather", "skin", "leather", 2, 0xFFD040);
		b.part("ear_r", "head", Role.EAR, 0, -4.5F, -4, 0, "skin", box(-5, -1, -0.5F, 5, 2, 1)).zRot = 0.2F;
		b.part("ear_l", "head", Role.EAR, 1, 4.5F, -4, 0, "skin", box(0, -1, -0.5F, 5, 2, 1)).zRot = -0.2F;
		b.part("nose", "head", Role.EXTRA, 0, -3, -4, "skin", box(-1, -1, -2, 2, 2, 2));
		b.part("tusk_r", "head", Role.EXTRA, -1.5F, -0.5F, -4, "bone", box(-0.5F, -1.5F, -1, 1, 2, 1));
		b.part("tusk_l", "head", Role.EXTRA, 1.5F, -0.5F, -4, "bone", box(-0.5F, -1.5F, -1, 1, 2, 1));
		b.part("helmet", "head", Role.EXTRA, 0, -7, 0, "bone", box(-5, -2, -4.5F, 10, 3, 9));
		b.part("horn_r", "helmet", Role.EXTRA, -4, -2, -2, "bone", box(-1, -4, -1, 2, 4, 2)).zRot = -0.3F;
		b.part("horn_l", "helmet", Role.EXTRA, 4, -2, -2, "bone", box(-1, -4, -1, 2, 4, 2)).zRot = 0.3F;
		b.part("belt", "body", Role.EXTRA, 0, 7, 0, "leather", box(-4.5F, 0, -3, 9, 1, 6));
		b.part("buckle", "belt", Role.EXTRA, 0, 0, -3, "bone", box(-1, -0.5F, -0.6F, 2, 2, 1));
		b.part("pauldron", "arm_l", Role.EXTRA, 0, -2, 0, "bone", box(-2, -2, -2.5F, 4, 3, 5));
		b.part("pauldron_spike", "pauldron", Role.EXTRA, 1, -2, 0, "bone", box(-0.5F, -3, -0.5F, 1, 3, 1)).zRot = -0.4F;
		b.part("cleaver", "arm_r", Role.EXTRA, 0, 8, 0, "iron", box(-0.5F, 0, -1.5F, 1, 8, 6));
		return b.build();
	}

	private static BodyPlan banditCaptain() {
		Builder b = BodyPlan.builder("bandit_captain", Rig.BIPED).shaded()
			.skin("skin", Skin.of(0xC99A72, 0xA87A52, SMOOTH))
			.skin("vest", Skin.of(0x6B4A2B, 0x2A1A10, CLOTH))
			.skin("pants", Skin.of(0x3E3328, 0x2A1E14, CLOTH))
			.skin("red", Skin.of(0xA8282E, 0xE6E6E6, CLOTH))
			.skin("steel", Skin.of(0xB8C0C8, 0x6B4A2B, METAL))
			.skin("boot", Skin.of(0x2A1E14, 0x6B4A2B, CLOTH))
			.skin("gold", Skin.of(0xD9B44A, 0xFFF0A0, METAL));
		Humanoid h = new Humanoid(8, 8, 8, 8, 12, 4, 4, 12, 4, 12);
		PlanKit.humanoid(b, h, "skin", "vest", "skin", "pants", 2, 0xF0E0C0);
		b.part("bandana", "head", Role.EXTRA, 0, -8, 0, "red", box(-4.5F, -0.5F, -4.5F, 9, 3, 9));
		b.part("knot", "bandana", Role.CAPE, 0, 1, 4.5F, "red", box(-1, 0, 0, 2, 5, 1));
		b.part("mask", "head", Role.EXTRA, 0, -3, -4, "red", box(-4.2F, 0, -0.6F, 8, 3, 1));
		b.part("scarf", "body", Role.CAPE, 0, 0, 2, "red", box(-4, 0, 0, 8, 10, 1));
		sash(b, h, "boot");
		b.part("belt", "body", Role.EXTRA, 0, 10, 0, "boot", box(-4.5F, 0, -2.5F, 9, 1, 5));
		b.part("buckle", "belt", Role.EXTRA, 0, 0, -2.5F, "gold", box(-1, -0.5F, -0.6F, 2, 2, 1));
		b.part("holster", "belt", Role.EXTRA, 4.5F, 1, 0, "boot", box(0, 0, -1, 1, 4, 2));
		b.part("cuff_r", "arm_r", Role.EXTRA, 0, 7, 0, "vest", box(-2, 0, -2, 4, 2, 4, 0.25F));
		b.part("cuff_l", "arm_l", Role.EXTRA, 0, 7, 0, "vest", box(-2, 0, -2, 4, 2, 4, 0.25F));
		boots(b, h, "boot");
		b.part("knife", "arm_r", Role.EXTRA, 0, 9, -1, "steel", box(-0.5F, 0, -4, 1, 1, 5));
		return b.build();
	}

	private static BodyPlan bogHag() {
		Builder b = BodyPlan.builder("bog_hag", Rig.BIPED).shaded()
			.skin("skin", Skin.of(0x7A8A5A, 0x5A6A3A, SMOOTH))
			.skin("robe", Skin.of(0x3A2A4A, 0x6AA84F, CLOTH))
			.skin("hat", Skin.of(0x1E1A24, 0x6AA84F, CLOTH))
			.skin("hair", Skin.of(0x8A8A80, 0x5A5A50, FUR))
			.skin("shawl", Skin.of(0x4A5A2A, 0x2A3A1A, CLOTH))
			.skin("claw", Skin.of(0x3A3A2A, 0x1A1A10, BONE))
			.skin("potion", Skin.glowing(0x3A8A2A, 0xB0FF40, CRYSTAL));
		PlanKit.humanoid(b, new Humanoid(8, 8, 8, 8, 11, 5, 3, 13, 3, 9), "skin", "robe", "skin", "robe", 2, 0xB0FF40);
		b.get("body").xRot = 0.35F;
		b.part("skirt", "body", Role.EXTRA, 0, 8, 0, "robe", box(-5, 0, -3.5F, 10, 7, 7));
		b.part("shawl", "body", Role.EXTRA, 0, 0, 0, "shawl", box(-5, -1, -3, 10, 3, 6));
		b.part("hair", "head", Role.CAPE, 0, -6, 3.5F, "hair", box(-4, 0, 0, 8, 10, 1));
		b.part("nose", "head", Role.EXTRA, 0, -4, -4, "skin", box(-1, -1, -4, 2, 2, 4)).xRot = 0.3F;
		b.part("wart", "nose", Role.EXTRA, 0.5F, -1, -3, "claw", box(0, -0.5F, -0.5F, 1, 1, 1));
		b.part("brim", "head", Role.EXTRA, 0, -8, 0, "hat", box(-7, -1, -7, 14, 1, 14));
		Part cone = b.part("hat1", "brim", Role.EXTRA, 0, -1, 0, "hat", box(-4, -5, -4, 8, 5, 8));
		cone.xRot = -0.15F;
		b.part("hat2", "hat1", Role.EXTRA, 0, -5, 0, "hat", box(-2, -5, -2, 4, 5, 4)).xRot = -0.3F;
		b.part("hatband", "hat1", Role.EXTRA, 0, 0, 0, "potion", box(-4, -1, -4, 8, 1, 8, 0.2F));
		for (String s : new String[] {"r", "l"}) {
			b.part("fingers_" + s, "arm_" + s, Role.EXTRA, 0, 10, 0, "claw", box(-1.5F, 0, -1.5F, 1, 3, 1), box(0.5F, 0, -1.5F, 1, 3, 1), box(-0.5F, 0, 0.5F, 1, 2, 1));
		}
		b.part("potion", "skirt", Role.EXTRA, 4, 1, -2, "potion", box(-1, 0, -1, 2, 3, 2), box(-0.5F, -1, -0.5F, 1, 1, 1));
		return b.build();
	}

	private static BodyPlan caveTroll() {
		Builder b = BodyPlan.builder("cave_troll", Rig.BIPED).scale(1.55F).shaded()
			.skin("hide", Skin.of(0x6A7A5A, 0x4A5A3A, STONE))
			.skin("belly", Skin.of(0x8A9A72, 0x6A7A5A, SMOOTH))
			.skin("cloth", Skin.of(0x5A4630, 0x3A2A1C, CLOTH))
			.skin("rock", Skin.of(0x5A5A60, 0x3A3A40, STONE))
			.skin("club", Skin.of(0x6B4A2B, 0x3A2A1C, BARK))
			.skin("tusk", Skin.of(0xE8E0C8, 0x8A8070, BONE));
		PlanKit.humanoid(b, new Humanoid(8, 7, 7, 12, 12, 8, 5, 17, 6, 8), "hide", "hide", "hide", "cloth", 2, 0xFFB030);
		b.get("body").xRot = 0.3F;
		Part head = b.get("head");
		head.xRot = 0.1F;
		b.part("belly", "body", Role.EXTRA, 0, 7, -4, "belly", box(-5, -3, -2, 10, 7, 3));
		b.part("loincloth", "body", Role.EXTRA, 0, 10, 0, "cloth", box(-6.5F, 0, -4.5F, 13, 4, 9));
		b.part("brow", "head", Role.EXTRA, 0, -5, -3.5F, "hide", box(-4, -1, -1, 8, 2, 1));
		b.part("nose", "head", Role.EXTRA, 0, -4, -3.5F, "hide", box(-1.5F, 0, -2, 3, 3, 2));
		Part jaw = b.part("jaw", "head", Role.JAW, 0, -2, -1, "hide", box(-4, 0, -3, 8, 3, 4));
		b.part("tusk_r", jaw.name, Role.EXTRA, -2.5F, 0, -3, "tusk", box(-0.5F, -3, -0.5F, 1, 3, 1));
		b.part("tusk_l", jaw.name, Role.EXTRA, 2.5F, 0, -3, "tusk", box(-0.5F, -3, -0.5F, 1, 3, 1));
		b.part("ear_r", "head", Role.EAR, 0, -4, -4, 0, "hide", box(-3, -1, -0.5F, 3, 3, 1)).zRot = 0.4F;
		b.part("ear_l", "head", Role.EAR, 1, 4, -4, 0, "hide", box(0, -1, -0.5F, 3, 3, 1)).zRot = -0.4F;
		b.part("rock_r", "arm_r", Role.EXTRA, 0, -2, 0, "rock", box(-3, -2, -3, 6, 3, 6));
		b.part("rock_l", "arm_l", Role.EXTRA, 0, -2, 0, "rock", box(-3, -2, -3, 6, 3, 6));
		b.part("fist_l", "arm_l", Role.EXTRA, 0, 13, 0, "hide", box(-3, 0, -3, 6, 4, 6));
		Part club = b.part("club", "arm_r", Role.EXTRA, 0, 13, 0, "club", box(-2, 0, -2, 4, 12, 4), box(-3, 8, -3, 6, 6, 6));
		b.part("club_spikes", club.name, Role.EXTRA, 0, 10, 0, "tusk", box(-4, 0, -0.5F, 1, 1, 1), box(3, 1, -0.5F, 1, 1, 1), box(-0.5F, 2, -4, 1, 1, 1),
			box(-0.5F, 0, 3, 1, 1, 1));
		return b.build();
	}

	private static BodyPlan myconidShaman() {
		Builder b = BodyPlan.builder("myconid_shaman", Rig.BIPED).scale(1.05F).shaded()
			.skin("stalk", Skin.of(0xE8D8B8, 0xC8B898, SMOOTH))
			.skin("cap", Skin.of(0xC0262D, 0xF4F4F0, SPOTS))
			.skin("gills", Skin.of(0xE8C0A8, 0xB88870, BARK))
			.skin("robe", Skin.of(0x5A3A2A, 0xD05050, CLOTH))
			.skin("staff", Skin.of(0x6B4A2B, 0x3A2A1C, BARK))
			.skin("spore", Skin.glowing(0xD05050, 0xFFB0B0, CRYSTAL));
		PlanKit.humanoid(b, new Humanoid(6, 7, 6, 7, 11, 4, 2, 13, 3, 12), "stalk", "robe", "stalk", "stalk", 2, 0xFFE070);
		b.part("cap", "head", Role.EXTRA, 0, -7, 0, "cap", box(-8, -5, -8, 16, 5, 16), box(-6, -7, -6, 12, 2, 12));
		b.part("gills", "cap", Role.EXTRA, 0, 0, 0, "gills", box(-7.5F, -0.6F, -7.5F, 15, 1, 15));
		b.part("skirt", "body", Role.EXTRA, 0, 9, 0, "robe", box(-4.5F, 0, -3, 9, 6, 6));
		b.part("pouch", "skirt", Role.EXTRA, 3.5F, 0, -3, "spore", box(-1, 0, -1, 2, 2, 1));
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			Part sprout = b.part("sprout_" + s, "body", Role.EXTRA, side * 3, 0, 1, "stalk", box(-0.5F, -3, -0.5F, 1, 3, 1));
			sprout.zRot = side * 0.4F;
			b.part("sprout_cap_" + s, sprout.name, Role.EXTRA, 0, -3, 0, "cap", box(-1.5F, -1, -1.5F, 3, 1, 3));
		}
		b.part("staff", "arm_r", Role.EXTRA, 0, 10, 0, "staff", box(-0.5F, -14, -0.5F, 1, 20, 1));
		b.part("orb", "staff", Role.EXTRA, 0, -14, 0, "spore", box(-1.5F, -3, -1.5F, 3, 3, 3));
		b.part("spores", "orb", Role.ORBIT, 0, -1.5F, 0, "spore");
		for (int i = 0; i < 3; i++) {
			double a = Math.PI * 2 * i / 3;
			b.part("spore" + i, "spores", Role.EXTRA, (float)Math.cos(a) * 4, 0, (float)Math.sin(a) * 4, "spore", box(-0.5F, -0.5F, -0.5F, 1, 1, 1));
		}
		return b.build();
	}

	private static BodyPlan drownedCorsair() {
		Builder b = BodyPlan.builder("drowned_corsair", Rig.BIPED).shaded()
			.skin("skin", Skin.of(0x5A9A8A, 0x3A6A6A, SMOOTH))
			.skin("coat", Skin.of(0x2A3A5A, 0xC9A227, CLOTH))
			.skin("pants", Skin.of(0x3A3A40, 0x2A2A30, CLOTH))
			.skin("hat", Skin.of(0x1E1A1A, 0xC9A227, CLOTH))
			.skin("gun", Skin.of(0x5A3A26, 0x8A929E, METAL))
			.skin("kelp", Skin.of(0x3A6A2A, 0x1E4A1A, FUR))
			.skin("coral", Skin.glowing(0xC84A6A, 0xFFB0C0, CRYSTAL))
			.skin("steel", Skin.of(0xB8C0C8, 0x6A7A8A, METAL));
		Humanoid h = new Humanoid(8, 8, 8, 8, 12, 4, 4, 12, 4, 12);
		PlanKit.humanoid(b, h, "skin", "coat", "coat", "pants", 2, 0x40FFE0);
		b.part("brim", "head", Role.EXTRA, 0, -8, 0, "hat", box(-6, -1, -5, 12, 1, 10));
		b.part("crown", "brim", Role.EXTRA, 0, -1, 0, "hat", box(-4, -3, -4, 8, 3, 8));
		b.part("patch", "head", Role.EXTRA, -1.5F, -5, -4, "hat", box(-1.5F, -1, -0.6F, 3, 2, 1));
		b.part("beard", "head", Role.CAPE, 0, -1, -4.2F, "kelp", box(-3, 0, -0.5F, 6, 4, 1));
		b.part("coattail", "body", Role.CAPE, 0, 9, 2, "coat", box(-4, 0, 0, 8, 9, 1));
		b.part("belt", "body", Role.EXTRA, 0, 10, 0, "hat", box(-4.5F, 0, -2.5F, 9, 1, 5));
		b.part("coral", "body", Role.EXTRA, 3, 0, 1, "coral", box(-1, -3, -1, 2, 3, 2));
		b.part("coral_twig", "coral", Role.EXTRA, 0, -2, 0, "coral", box(0, -2, -0.5F, 2, 1, 1)).zRot = -0.5F;
		boots(b, h, "hat");
		b.part("pistol", "arm_r", Role.EXTRA, 0, 9, -1, "gun", box(-0.5F, -1, -5, 1, 2, 6));
		b.part("cutlass", "arm_l", Role.EXTRA, 0, 9, 0, "steel", box(-0.5F, 0, -1, 1, 8, 2), box(-1, -1, -1.5F, 2, 1, 3));
		return b.build();
	}

	private static BodyPlan badlandsGunslinger() {
		Builder b = BodyPlan.builder("badlands_gunslinger", Rig.BIPED).shaded()
			.skin("bone", Skin.of(0xE8E0C8, 0x8A8070, BONE))
			.skin("poncho", Skin.of(0xA8582A, 0xE8C45A, CLOTH))
			.skin("hat", Skin.of(0x8A5A2A, 0x3A2A1C, CLOTH))
			.skin("gun", Skin.of(0x3A3A40, 0xC9A227, METAL))
			.skin("leather", Skin.of(0x5A3A20, 0xC9A227, METAL));
		Humanoid h = new Humanoid(8, 8, 8, 8, 12, 4, 2, 12, 2, 12);
		PlanKit.humanoid(b, h, "bone", "bone", "bone", "bone", 2, 0xFF3020);
		b.part("jaw", "head", Role.JAW, 0, -1, 0, "bone", box(-3, 0, -3.5F, 6, 2, 4));
		b.part("poncho", "body", Role.EXTRA, 0, 0, 0, "poncho", box(-5, 0, -3, 10, 7, 6));
		b.part("brim", "head", Role.EXTRA, 0, -8, 0, "hat", box(-7, -1, -7, 14, 1, 14));
		b.part("crown", "brim", Role.EXTRA, 0, -1, 0, "hat", box(-3.5F, -3, -3.5F, 7, 3, 7));
		b.part("hatband", "crown", Role.EXTRA, 0, 0, 0, "leather", box(-3.5F, -1, -3.5F, 7, 1, 7, 0.2F));
		sash(b, h, "leather");
		b.part("belt", "body", Role.EXTRA, 0, 10, 0, "leather", box(-4.5F, 0, -2.5F, 9, 1, 5));
		boots(b, h, "hat");
		for (String s : new String[] {"r", "l"}) {
			b.part("spur_" + s, "shin_" + s, Role.EXTRA, 0, 5, 1.5F, "gun", box(-0.5F, -0.5F, 0, 1, 1, 2));
		}
		b.part("gun_r", "arm_r", Role.EXTRA, 0, 9, 0, "gun", box(-0.5F, -1, -5, 1, 2, 6));
		b.part("gun_l", "arm_l", Role.EXTRA, 0, 9, 0, "gun", box(-0.5F, -1, -5, 1, 2, 6));
		return b.build();
	}

	private static BodyPlan wendigo() {
		Builder b = BodyPlan.builder("wendigo", Rig.BIPED).scale(1.3F).shaded()
			.skin("hide", Skin.of(0xC8C0B0, 0x8A8070, SMOOTH))
			.skin("skull", Skin.of(0xE8E0C8, 0x6A6050, BONE))
			.skin("fur", Skin.of(0x3A2A20, 0x5A4630, FUR))
			.skin("antler", Skin.of(0xD8C8A0, 0x8A7050, BONE));
		PlanKit.humanoid(b, new Humanoid(6, 8, 7, 7, 13, 4, 2, 21, 3, 18), "skull", "hide", "hide", "fur", 2, 0xFFFFFF);
		b.get("body").xRot = 0.15F;
		b.part("ribs", "body", Role.EXTRA, 0, 2, -2, "skull", box(-3, 0, -0.5F, 6, 6, 1));
		b.part("spine", "body", Role.EXTRA, 0, 1, 2, "skull", box(-0.5F, 0, 0, 1, 1, 1), box(-0.5F, 3, 0, 1, 1, 1), box(-0.5F, 6, 0, 1, 1, 1), box(-0.5F, 9, 0, 1, 1, 1));
		b.part("shoulders", "body", Role.EXTRA, 0, 0, 0, "fur", box(-5, -1, -3, 10, 4, 6));
		b.part("tatters", "body", Role.CAPE, 0, 2, 2, "fur", box(-4, 0, 0, 8, 10, 1));
		Part jaw = b.part("jaw", "head", Role.JAW, 0, -1, -0.5F, "skull", box(-2.5F, 0, -3.5F, 5, 2, 4));
		b.part("teeth", jaw.name, Role.EXTRA, 0, 0, -3.5F, "antler", box(-2, -1, -0.5F, 1, 1, 1), box(1, -1, -0.5F, 1, 1, 1));
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			Part base = b.part("antler_" + s, "head", Role.EXTRA, side * 2.5F, -8, 0, "antler", box(-0.5F, -6, -0.5F, 1, 6, 1));
			base.zRot = side * 0.4F;
			b.part("tine_" + s + "1", "antler_" + s, Role.EXTRA, 0, -4, 0, "antler", box(-0.5F, -4, -0.5F, 1, 4, 1)).zRot = side * 0.7F;
			b.part("tine_" + s + "2", "antler_" + s, Role.EXTRA, 0, -6, 0, "antler", box(-0.5F, -3, -0.5F, 1, 3, 1)).zRot = -side * 0.5F;
			b.part("tine_" + s + "3", "antler_" + s, Role.EXTRA, 0, -2, 0, "antler", box(-0.5F, -3, -0.5F, 1, 3, 1)).xRot = 0.6F;
			b.part("claw_" + s, side < 0 ? "arm_r" : "arm_l", Role.EXTRA, 0, 18, -1, "antler", box(-1, 0, -1, 2, 3, 1), box(-1, 0, 0.5F, 1, 2, 1));
		}
		return b.build();
	}

	private static BodyPlan amethystSentinel() {
		Builder b = BodyPlan.builder("amethyst_sentinel", Rig.BIPED).scale(1.4F).shaded()
			.skin("crystal", Skin.glowing(0x7A4AB8, 0xE0B0FF, CRYSTAL))
			.skin("rock", Skin.of(0x4A4458, 0x7A4AB8, STONE))
			.skin("plate", Skin.of(0x3A3448, 0x5A5468, STONE));
		PlanKit.humanoid(b, new Humanoid(7, 7, 7, 14, 12, 9, 5, 16, 6, 10), "rock", "rock", "rock", "rock", 1, 0xF0C0FF);
		b.part("core", "body", Role.EXTRA, 0, 3, -4.5F, "crystal", box(-3, 0, -1, 6, 6, 1));
		b.part("seam", "body", Role.EXTRA, 0, 0, -4.6F, "crystal", box(-0.5F, 9, -0.5F, 1, 3, 1));
		b.part("brow", "head", Role.EXTRA, 0, -5, -3.6F, "plate", box(-3.5F, -1, -0.5F, 7, 1, 1));
		for (int i = 0; i < 3; i++) {
			Part tip = b.part("crest" + i, "head", Role.EXTRA, (i - 1) * 2, -7, 0, "crystal", box(-1, -3 + (i == 1 ? -2 : 0), -1, 2, 3 + (i == 1 ? 2 : 0), 2));
			tip.zRot = (i - 1) * 0.3F;
		}
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			Part spike = b.part("shard_" + s, "body", Role.EXTRA, side * 6, -1, 0, "crystal", box(-1.5F, -8, -1.5F, 3, 8, 3));
			spike.zRot = side * 0.35F;
			b.part("shard_" + s + "2", "body", Role.EXTRA, side * 3, -1, 3, "crystal", box(-1, -6, -1, 2, 6, 2)).zRot = side * 0.2F;
			b.part("back_" + s, "body", Role.EXTRA, side * 2.5F, 4, 4.5F, "crystal", box(-1, -1, 0, 2, 2, 4)).xRot = -0.6F;
			b.part("plate_" + s, side < 0 ? "arm_r" : "arm_l", Role.EXTRA, 0, -2, 0, "plate", box(-3.5F, -2, -3.5F, 7, 3, 7));
			b.part("fist_" + s, side < 0 ? "arm_r" : "arm_l", Role.EXTRA, 0, 13, 0, "crystal", box(-3, 0, -3, 6, 4, 6));
			b.part("knee_" + s, "shin_" + s, Role.EXTRA, 0, 0, -3, "crystal", box(-1.5F, 0, -1, 3, 2, 1));
		}
		return b.build();
	}

	private static BodyPlan ancientTreant() {
		Builder b = BodyPlan.builder("ancient_treant", Rig.BIPED).scale(1.7F).shaded()
			.skin("bark", Skin.of(0x5A4024, 0x3A2A14, BARK))
			.skin("leaf", Skin.of(0x3E7A2A, 0x7FBF3A, FUR))
			.skin("moss", Skin.of(0x4E7A2E, 0x2E5A1E, SPOTS))
			.skin("hollow", Skin.of(0x140C06, 0x2A1A0C, SMOOTH))
			.skin("shroom", Skin.of(0xC0262D, 0xF4F4F0, SPOTS));
		PlanKit.humanoid(b, new Humanoid(10, 8, 8, 12, 16, 8, 5, 17, 6, 10), "bark", "bark", "bark", "bark", 2, 0xFFC040);
		b.part("crown", "head", Role.EXTRA, 0, -8, 0, "leaf", box(-11, -10, -9, 22, 10, 18));
		b.part("crown_top", "crown", Role.EXTRA, 0, -10, 0, "leaf", box(-7, -4, -6, 14, 4, 12));
		b.part("mouth", "head", Role.JAW, 0, -2, -4.2F, "hollow", box(-2.5F, -1, -0.5F, 5, 2, 1));
		b.part("beard", "head", Role.EXTRA, 0, 0, -4, "moss", box(-4, -2, -1, 8, 6, 1));
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			String arm = side < 0 ? "arm_r" : "arm_l";
			b.part("branch_" + s, arm, Role.EXTRA, side * 2, 4, 0, "bark", box(-1, -7, -1, 2, 8, 2)).zRot = side * 0.6F;
			b.part("twigs_" + s, arm, Role.EXTRA, 0, 13, 0, "leaf", box(-3, 0, -3, 6, 4, 6));
			b.part("hanging_moss_" + s, arm, Role.CAPE, 0, 5, 2.5F, "moss", box(-2, 0, 0, 4, 8, 1));
			b.part("toes_" + s, "shin_" + s, Role.EXTRA, 0, 4, 0, "bark", box(-4, 0, -5, 8, 1, 4), box(-1, 0, 1, 2, 1, 3));
		}
		b.part("roots", "body", Role.EXTRA, 0, 13, 0, "moss", box(-7, 0, -5, 14, 3, 10));
		b.part("shroom_stem", "body", Role.EXTRA, 6, 6, -2, "bark", box(0, -1, -1, 2, 1, 2));
		b.part("shroom", "shroom_stem", Role.EXTRA, 2, -1, 0, "shroom", box(-0.5F, -1, -2, 3, 1, 4));
		return b.build();
	}

	private static BodyPlan witherKnight() {
		Builder b = BodyPlan.builder("wither_knight", Rig.BIPED).scale(1.25F).shaded()
			.skin("plate", Skin.of(0x2A2A30, 0x6A6A74, METAL))
			.skin("skull", Skin.of(0x3A3A3E, 0x1A1A1E, BONE))
			.skin("cape", Skin.of(0x5A1418, 0x2A0A0C, CLOTH))
			.skin("sword", Skin.glowing(0x3A3A44, 0x9A9AFF, METAL))
			.skin("shield", Skin.of(0x1E1E24, 0x8A1A20, METAL));
		Humanoid h = new Humanoid(8, 8, 8, 9, 12, 5, 4, 13, 4, 12);
		PlanKit.humanoid(b, h, "skull", "plate", "plate", "plate", 2, 0xFFFFFF);
		b.part("pauldron_r", "arm_r", Role.EXTRA, 0, -2, 0, "plate", box(-3, -2, -3, 6, 4, 6));
		b.part("pauldron_l", "arm_l", Role.EXTRA, 0, -2, 0, "plate", box(-3, -2, -3, 6, 4, 6));
		b.part("spike_r", "pauldron_r", Role.EXTRA, -2, -2, 0, "skull", box(-0.5F, -3, -0.5F, 1, 3, 1)).zRot = -0.4F;
		b.part("spike_l", "pauldron_l", Role.EXTRA, 2, -2, 0, "skull", box(-0.5F, -3, -0.5F, 1, 3, 1)).zRot = 0.4F;
		b.part("horn_r", "head", Role.EXTRA, -4, -7, 0, "skull", box(-3, -1, -1, 3, 2, 2)).zRot = 0.5F;
		b.part("horn_l", "head", Role.EXTRA, 4, -7, 0, "skull", box(0, -1, -1, 3, 2, 2)).zRot = -0.5F;
		b.part("jaw", "head", Role.JAW, 0, -1, 0, "skull", box(-3, 0, -3.5F, 6, 2, 4));
		b.part("gorget", "body", Role.EXTRA, 0, 0, 0, "plate", box(-5, -1.5F, -3, 10, 2, 6));
		b.part("tassets", "body", Role.EXTRA, 0, 10, 0, "plate", box(-5, 0, -3, 10, 4, 6));
		b.part("cape", "body", Role.CAPE, 0, 0, 2.5F, "cape", box(-5, 0, 0, 10, 20, 1));
		b.part("shield", "arm_l", Role.EXTRA, 2, 6, 0, "shield", box(0, -5, -4, 1, 10, 8));
		b.part("boss", "shield", Role.EXTRA, 1, 0, 0, "skull", box(0, -1.5F, -1.5F, 1, 3, 3));
		b.part("greatsword", "arm_r", Role.EXTRA, 0, 10, 0, "sword", box(-1, -24, -0.5F, 2, 24, 1), box(-3, 0, -1, 6, 1, 2));
		return b.build();
	}

	private static BodyPlan astralKnight() {
		Builder b = BodyPlan.builder("astral_knight", Rig.BIPED).scale(1.3F).shaded()
			.skin("plate", Skin.glowing(0x1A2050, 0xFFF4C0, VOID))
			.skin("gold", Skin.of(0xD9B44A, 0xFFF4C0, METAL))
			.skin("cape", Skin.glowing(0x101838, 0x9FD0FF, VOID))
			.skin("halo", Skin.glowing(0xFFE58A, 0xFFFFFF, CRYSTAL));
		PlanKit.humanoid(b, new Humanoid(8, 8, 8, 9, 12, 5, 4, 13, 4, 12), "plate", "plate", "plate", "plate", 2, 0x9FD0FF);
		b.part("visor", "head", Role.EXTRA, 0, -5, -4.2F, "gold", box(-4, -1, -0.5F, 8, 1, 1));
		b.part("crest", "head", Role.EXTRA, 0, -8, 0, "gold", box(-0.5F, -3, -3, 1, 3, 7));
		b.part("pauldron_r", "arm_r", Role.EXTRA, 0, -2, 0, "gold", box(-3, -2, -3, 6, 3, 6));
		b.part("pauldron_l", "arm_l", Role.EXTRA, 0, -2, 0, "gold", box(-3, -2, -3, 6, 3, 6));
		b.part("gauntlet_r", "arm_r", Role.EXTRA, 0, 7, 0, "gold", box(-2, 0, -2, 4, 3, 4, 0.3F));
		b.part("gauntlet_l", "arm_l", Role.EXTRA, 0, 7, 0, "gold", box(-2, 0, -2, 4, 3, 4, 0.3F));
		b.part("tassets", "body", Role.EXTRA, 0, 10, 0, "gold", box(-5, 0, -3, 10, 3, 6));
		b.part("cape", "body", Role.CAPE, 0, 0, 2.5F, "cape", box(-5, 0, 0, 10, 22, 1));
		b.part("shield", "arm_l", Role.EXTRA, 2, 6, 0, "plate", box(0, -4, -4, 1, 8, 8));
		b.part("star", "shield", Role.EXTRA, 1, 0, 0, "halo", box(0, -2, -0.5F, 1, 4, 1), box(0, -0.5F, -2, 1, 1, 4));
		b.part("lance", "arm_r", Role.EXTRA, 0, 10, 0, "gold", box(-1, -1, -26, 2, 2, 30));
		b.part("halo", "head", Role.ORBIT, 0, -11, 0, "halo");
		for (int i = 0; i < 8; i++) {
			double a = Math.PI * 2 * i / 8;
			b.part("halo" + i, "halo", Role.EXTRA, (float)Math.cos(a) * 5, 0, (float)Math.sin(a) * 5, "halo", box(-1, -0.5F, -1, 2, 1, 2));
		}
		return b.build();
	}

	// ------------------------------------------------------------------ floating spirits

	private static BodyPlan soulReaper() {
		Builder b = BodyPlan.builder("soul_reaper", Rig.FLOATER).scale(1.1F).shaded()
			.skin("cloak", Skin.of(0x1E2228, 0x3AA8C8, CLOTH))
			.skin("bone", Skin.of(0xC8D8D8, 0x6A8A8A, BONE))
			.skin("blade", Skin.glowing(0x6A8A9A, 0x9FF4FF, METAL))
			.skin("shaft", Skin.of(0x2A2420, 0x6A5A4A, BARK))
			.skin("soul", Skin.glowing(0x1A4A5A, 0x9FF4FF, CRYSTAL));
		b.part("body", null, Role.BODY, 0, -6, 0, "cloak", box(-4, 0, -3, 8, 14, 6));
		b.part("hem", "body", Role.EXTRA, 0, 14, 0, "cloak", box(-5, 0, -4, 10, 10, 8));
		b.part("tatters", "hem", Role.EXTRA, 0, 10, 0, "cloak", box(-5, 0, -4, 2, 3, 1), box(-1, 0, -4, 2, 4, 1), box(3, 0, -4, 2, 2, 1),
			box(-4, 0, 3, 2, 4, 1), box(1, 0, 3, 2, 3, 1));
		Part hood = b.part("head", null, Role.HEAD, 0, -6, 0, "cloak", box(-4.5F, -9, -4.5F, 9, 9, 9));
		hood.eyes = 2;
		hood.eyeColor = 0x6FE0FF;
		b.part("hood_tip", "head", Role.EXTRA, 0, -8, 3, "cloak", box(-1.5F, -1, 0, 3, 2, 3)).xRot = 0.5F;
		b.part("face", "head", Role.EXTRA, 0, -2, -4.6F, "bone", box(-2.5F, -5, 0, 5, 5, 1));
		b.part("arm_r", null, Role.ARM_RIGHT, -5.5F, -4, 0, "bone", box(-1, -1, -1, 2, 14, 2));
		b.part("arm_l", null, Role.ARM_LEFT, 5.5F, -4, 0, "bone", box(-1, -1, -1, 2, 14, 2));
		b.part("sleeve_r", "arm_r", Role.EXTRA, 0, 0, 0, "cloak", box(-1.5F, -1, -1.5F, 3, 7, 3));
		b.part("sleeve_l", "arm_l", Role.EXTRA, 0, 0, 0, "cloak", box(-1.5F, -1, -1.5F, 3, 7, 3));
		b.part("chain", "arm_l", Role.EXTRA, 0, 13, 0, "bone", box(-0.5F, 0, -0.5F, 1, 3, 1));
		b.part("lantern", "chain", Role.EXTRA, 0, 3, 0, "soul", box(-1.5F, 0, -1.5F, 3, 4, 3));
		b.part("scythe", "arm_r", Role.EXTRA, 0, 12, 0, "shaft", box(-0.5F, -18, -0.5F, 1, 26, 1));
		b.part("scythe_blade", "scythe", Role.EXTRA, 0, -18, 0, "blade", box(-0.5F, -1, -12, 1, 3, 12), box(-0.5F, 2, -10, 1, 1, 6));
		return b.build();
	}

	private static BodyPlan voidWatcher() {
		Builder b = BodyPlan.builder("void_watcher", Rig.FLOATER).scale(1.3F).shaded()
			.skin("void", Skin.glowing(0x1A1028, 0xC080FF, VOID))
			.skin("flesh", Skin.of(0x4A2A6A, 0x8A4ACF, SMOOTH))
			.skin("iris", Skin.glowing(0xC040C0, 0xFFB0FF, CRYSTAL))
			.skin("shard", Skin.glowing(0x6A2ACF, 0xE0B0FF, CRYSTAL));
		b.part("body", null, Role.BODY, 0, 4, 0, "void", box(-6, -12, -6, 12, 12, 12));
		Part eye = b.part("head", "body", Role.HEAD, 0, -6, -6, "flesh", box(-4, -4, -1, 8, 8, 1));
		Part iris = b.part("iris", "head", Role.EXTRA, 0, 0, -1, "iris", box(-2, -2, -0.4F, 4, 4, 1));
		iris.eyes = 1;
		iris.eyeColor = 0x100010;
		b.part("lid_top", "body", Role.JAW, 0, -12, -6, "void", box(-6, 0, -1.5F, 12, 2, 2));
		b.part("lid_bottom", "body", Role.EXTRA, 0, -1, -6, "void", box(-6, -1, -1.5F, 12, 1, 2));
		for (int i = 0; i < 4; i++) {
			Part spike = b.part("spike" + i, "body", Role.EXTRA, i % 2 == 0 ? (i - 1) * 4 : 0, -12, i % 2 == 1 ? (i - 2) * 4 : 0, "shard",
				box(-1, -4, -1, 2, 4, 2));
			spike.xRot = i % 2 == 1 ? (i - 2) * 0.4F : 0.0F;
			spike.zRot = i % 2 == 0 ? (i - 1) * -0.4F : 0.0F;
		}
		for (int t = 0; t < 8; t++) {
			float a = (float)(Math.PI / 4 * t + Math.PI / 8);
			PlanKit.chain(b, "tentacle" + t + "_", "body", Role.TENTACLE, t * 10, (float)Math.cos(a) * 4.5F, 0, (float)Math.sin(a) * 4.5F, 3, 2, 6, 0.85F, 0,
				0.15F, "flesh");
		}
		b.part("orbit", "body", Role.ORBIT, 0, -6, 0, "shard");
		for (int i = 0; i < 3; i++) {
			double a = Math.PI * 2 * i / 3;
			b.part("orbit_shard" + i, "orbit", Role.EXTRA, (float)Math.cos(a) * 11, 0, (float)Math.sin(a) * 11, "shard", box(-1, -2, -1, 2, 4, 2));
		}
		return b.build();
	}

	private static BodyPlan chorusWraith() {
		Builder b = BodyPlan.builder("chorus_wraith", Rig.FLOATER).scale(1.15F).shaded()
			.skin("robe", Skin.of(0x8A5AA8, 0x4A2A6A, CLOTH))
			.skin("chorus", Skin.glowing(0xC090D8, 0xF4D8FF, SPOTS))
			.skin("hand", Skin.of(0xD8C8E8, 0x8A7AA8, SMOOTH))
			.skin("flower", Skin.glowing(0xF0E0FF, 0xFFFFFF, CRYSTAL));
		b.part("body", null, Role.BODY, 0, -6, 0, "robe", box(-4, 0, -3, 8, 14, 6));
		b.part("hem", "body", Role.EXTRA, 0, 14, 0, "robe", box(-5, 0, -3.5F, 10, 9, 7));
		b.part("tatters", "hem", Role.EXTRA, 0, 9, 0, "robe", box(-5, 0, -3.5F, 2, 3, 1), box(-1, 0, -3.5F, 2, 2, 1), box(3, 0, -3.5F, 2, 4, 1),
			box(-3, 0, 2.5F, 2, 3, 1));
		Part hood = b.part("head", null, Role.HEAD, 0, -6, 0, "robe", box(-4.5F, -9, -4.5F, 9, 9, 9));
		hood.eyes = 2;
		hood.eyeColor = 0xFFFFFF;
		b.part("arm_r", null, Role.ARM_RIGHT, -5.5F, -4, 0, "hand", box(-1, -1, -1, 2, 16, 2));
		b.part("arm_l", null, Role.ARM_LEFT, 5.5F, -4, 0, "hand", box(-1, -1, -1, 2, 16, 2));
		b.part("fingers_r", "arm_r", Role.EXTRA, 0, 15, 0, "hand", box(-1, 0, -1, 1, 3, 1), box(0, 0, 0, 1, 2, 1));
		b.part("fingers_l", "arm_l", Role.EXTRA, 0, 15, 0, "hand", box(0, 0, -1, 1, 3, 1), box(-1, 0, 0, 1, 2, 1));
		Part right = b.part("growth_r", "body", Role.EXTRA, -4, 0, 0, "chorus", box(-3, -6, -2, 4, 6, 4));
		b.part("growth_r2", right.name, Role.EXTRA, -2, -6, 0, "chorus", box(-1, -4, -1, 2, 4, 2)).zRot = -0.5F;
		b.part("bloom_r", "growth_r2", Role.EXTRA, 0, -4, 0, "flower", box(-1.5F, -2, -1.5F, 3, 2, 3));
		Part left = b.part("growth_l", "body", Role.EXTRA, 4, 0, 0, "chorus", box(-1, -8, -2, 4, 8, 4));
		b.part("growth_l2", left.name, Role.EXTRA, 2, -8, 0, "chorus", box(-1, -3, -1, 2, 3, 2)).zRot = 0.6F;
		b.part("bloom_l", "growth_l2", Role.EXTRA, 0, -3, 0, "flower", box(-1.5F, -2, -1.5F, 3, 2, 3));
		b.part("growth_top", "head", Role.EXTRA, 1, -9, 1, "chorus", box(-2, -4, -2, 3, 4, 3));
		b.part("bloom_top", "growth_top", Role.EXTRA, -0.5F, -4, -0.5F, "flower", box(-1.5F, -2, -1.5F, 3, 2, 3));
		return b.build();
	}

	private NamedPlans() {
	}
}
