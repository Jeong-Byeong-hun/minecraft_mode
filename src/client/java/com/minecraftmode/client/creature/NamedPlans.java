package com.minecraftmode.client.creature;

import static com.minecraftmode.client.creature.BodyPlan.box;
import static com.minecraftmode.client.creature.Skin.Pattern.*;

import com.minecraftmode.client.creature.BodyPlan.Builder;
import com.minecraftmode.client.creature.BodyPlan.Part;
import com.minecraftmode.client.creature.BodyPlan.Rig;
import com.minecraftmode.client.creature.BodyPlan.Role;
import com.minecraftmode.client.creature.PlanKit.Humanoid;
import com.minecraftmode.client.creature.PlanKit.Quad;
import java.util.List;

/** Body plans of the 22 named monsters (ids match their entity types). */
final class NamedPlans {
	static List<BodyPlan> all() {
		return List.of(
			goldenEnderman(), goblinWarchief(), banditCaptain(), bogHag(), frostAlpha(), duneScorpion(), caveTroll(), myconidShaman(), jungleStalker(), madPig(),
			drownedCorsair(), badlandsGunslinger(), wendigo(), amethystSentinel(), ancientTreant(), magmaBehemoth(), soulReaper(), witherKnight(), voidWatcher(),
			echoStalker(), chorusWraith(), astralKnight()
		);
	}

	private static BodyPlan goldenEnderman() {
		Builder b = BodyPlan.builder("golden_enderman", Rig.BIPED).scale(0.95F)
			.skin("gold", Skin.of(0xC89A2E, 0xFFE58A, METAL))
			.skin("head", Skin.of(0xB8862A, 0xFFE58A, SMOOTH))
			.skin("gem", Skin.glowing(0x8A2ACF, 0xE080FF, CRYSTAL));
		PlanKit.humanoid(b, new Humanoid(8, 8, 8, 8, 12, 4, 2, 30, 2, 30), "head", "gold", "gold", "gold", 2, 0xE080FF);
		b.part("crown", "head", Role.EXTRA, 0, -8, 0, "gem", box(-3, -2, -3, 6, 2, 6));
		return b.build();
	}

	private static BodyPlan goblinWarchief() {
		Builder b = BodyPlan.builder("goblin_warchief", Rig.BIPED)
			.skin("skin", Skin.of(0x6FAF3A, 0x9ACD5A, SMOOTH))
			.skin("leather", Skin.of(0x6B4A2B, 0xC9A227, CLOTH))
			.skin("bone", Skin.of(0xE8E0C8, 0x8A8070, BONE))
			.skin("iron", Skin.of(0x8A929E, 0xC0262D, METAL));
		PlanKit.humanoid(b, new Humanoid(9, 7, 8, 8, 9, 5, 3, 11, 4, 8), "skin", "leather", "skin", "leather", 2, 0xFFD040);
		b.part("ear_r", "head", Role.EXTRA, -4.5F, -4, 0, "skin", box(-5, -1, -0.5F, 5, 2, 1)).zRot = 0.2F;
		b.part("ear_l", "head", Role.EXTRA, 4.5F, -4, 0, "skin", box(0, -1, -0.5F, 5, 2, 1)).zRot = -0.2F;
		b.part("helmet", "head", Role.EXTRA, 0, -7, 0, "bone", box(-5, -2, -4.5F, 10, 3, 9));
		b.part("horn_r", "helmet", Role.EXTRA, -4, -2, -2, "bone", box(-1, -4, -1, 2, 4, 2)).zRot = -0.3F;
		b.part("horn_l", "helmet", Role.EXTRA, 4, -2, -2, "bone", box(-1, -4, -1, 2, 4, 2)).zRot = 0.3F;
		b.part("cleaver", "arm_r", Role.EXTRA, 0, 8, 0, "iron", box(-0.5F, 0, -1, 1, 7, 5));
		return b.build();
	}

	private static BodyPlan banditCaptain() {
		Builder b = BodyPlan.builder("bandit_captain", Rig.BIPED)
			.skin("skin", Skin.of(0xC99A72, 0xA87A52, SMOOTH))
			.skin("vest", Skin.of(0x6B4A2B, 0x2A1A10, CLOTH))
			.skin("pants", Skin.of(0x3E3328, 0x2A1E14, CLOTH))
			.skin("red", Skin.of(0xA8282E, 0xE6E6E6, CLOTH))
			.skin("steel", Skin.of(0xB8C0C8, 0x6B4A2B, METAL));
		PlanKit.humanoid(b, new Humanoid(8, 8, 8, 8, 12, 4, 4, 12, 4, 12), "skin", "vest", "skin", "pants", 2, 0xF0E0C0);
		b.part("bandana", "head", Role.EXTRA, 0, -8, 0, "red", box(-4.5F, -0.5F, -4.5F, 9, 3, 9));
		b.part("mask", "head", Role.EXTRA, 0, -3, -4, "red", box(-4.2F, 0, -0.6F, 8, 3, 1));
		b.part("scarf", "body", Role.CAPE, 0, 0, 2, "red", box(-4, 0, 0, 8, 10, 1));
		b.part("knife", "arm_r", Role.EXTRA, 0, 9, -1, "steel", box(-0.5F, 0, -4, 1, 1, 5));
		return b.build();
	}

	private static BodyPlan bogHag() {
		Builder b = BodyPlan.builder("bog_hag", Rig.BIPED)
			.skin("skin", Skin.of(0x7A8A5A, 0x5A6A3A, SMOOTH))
			.skin("robe", Skin.of(0x3A2A4A, 0x6AA84F, CLOTH))
			.skin("hat", Skin.of(0x1E1A24, 0x6AA84F, CLOTH));
		PlanKit.humanoid(b, new Humanoid(8, 8, 8, 8, 11, 5, 3, 13, 3, 9), "skin", "robe", "skin", "robe", 2, 0xB0FF40);
		b.get("body").xRot = 0.35F;
		b.part("skirt", "body", Role.EXTRA, 0, 8, 0, "robe", box(-5, 0, -3.5F, 10, 7, 7));
		b.part("nose", "head", Role.EXTRA, 0, -4, -4, "skin", box(-1, -1, -4, 2, 2, 4)).xRot = 0.3F;
		b.part("brim", "head", Role.EXTRA, 0, -8, 0, "hat", box(-7, -1, -7, 14, 1, 14));
		Part cone = b.part("hat1", "brim", Role.EXTRA, 0, -1, 0, "hat", box(-4, -5, -4, 8, 5, 8));
		cone.xRot = -0.15F;
		b.part("hat2", "hat1", Role.EXTRA, 0, -5, 0, "hat", box(-2, -5, -2, 4, 5, 4)).xRot = -0.3F;
		return b.build();
	}

	private static BodyPlan frostAlpha() {
		Builder b = BodyPlan.builder("frost_alpha", Rig.QUADRUPED).scale(1.15F)
			.skin("fur", Skin.of(0xE8F0F8, 0xA8C8E0, FUR))
			.skin("dark", Skin.of(0x8AA8C8, 0x5A7898, FUR))
			.skin("ice", Skin.glowing(0x9FD0FF, 0xE8FFFF, CRYSTAL));
		PlanKit.quadruped(b, new Quad(8, 8, 16, 8, 7, 7, 3, 9), "fur", "fur", "dark", 2, 0x6FD0FF);
		b.part("snout", "head", Role.EXTRA, 0, 1, -7, "fur", box(-2, -1.5F, -4, 4, 3, 4));
		b.part("jaw", "snout", Role.JAW, 0, 1.5F, 0, "dark", box(-2, 0, -4, 4, 1, 4));
		b.part("ear_r", "head", Role.EXTRA, -2.5F, -3.5F, -3, "fur", box(-1, -3, -1, 2, 3, 1));
		b.part("ear_l", "head", Role.EXTRA, 2.5F, -3.5F, -3, "fur", box(-1, -3, -1, 2, 3, 1));
		b.part("mane", "body", Role.EXTRA, 0, -1, -6, "fur", box(-5, -5, -3, 10, 9, 6));
		b.part("spikes", "body", Role.EXTRA, 0, -4, 0, "ice", box(-1, -3, -6, 2, 3, 12));
		PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -2, 8, 2, 3, 6, 0.85F, 1, 0.6F, "fur");
		return b.build();
	}

	private static BodyPlan duneScorpion() {
		Builder b = BodyPlan.builder("dune_scorpion", Rig.ARACHNID).scale(1.2F)
			.skin("shell", Skin.of(0xC8A050, 0x8A6A2A, CHITIN))
			.skin("dark", Skin.of(0x8A6A2A, 0x5A4220, CHITIN))
			.skin("sting", Skin.glowing(0x6A8A2A, 0xB0FF40, CRYSTAL));
		b.part("body", null, Role.BODY, 0, 18, 0, "shell", box(-5, -2.5F, -6, 10, 5, 8), box(-4.5F, -2, 2, 9, 4, 8));
		Part head = b.part("head", "body", Role.HEAD, 0, 0, -6, "shell", box(-3.5F, -2, -3, 7, 4, 3));
		head.eyes = 4;
		head.eyeColor = 0x101010;
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			Part arm = b.part("claw_arm_" + s, "body", Role.EXTRA, side * 4, 0, -5, "shell", box(-1.5F, -1.5F, -7, 3, 3, 7));
			arm.yRot = side * -0.4F;
			b.part("pincer_" + s, "claw_arm_" + s, Role.JAW, 0, 0, -7, "dark", box(-2.5F, -1.5F, -5, 5, 3, 5));
		}
		PlanKit.spiderLegs(b, "body", 5, 1, -3, 6, 10, "dark");
		String tip = PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -1, 10, 5, 4, 6, 0.85F, 1, -0.75F, "shell");
		b.part("stinger", tip, Role.EXTRA, 0, 0, 4, "sting", box(-1, -1, 0, 2, 2, 4)).xRot = -0.8F;
		return b.build();
	}

	private static BodyPlan caveTroll() {
		Builder b = BodyPlan.builder("cave_troll", Rig.BIPED).scale(1.55F)
			.skin("hide", Skin.of(0x6A7A5A, 0x4A5A3A, STONE))
			.skin("cloth", Skin.of(0x5A4630, 0x3A2A1C, CLOTH))
			.skin("club", Skin.of(0x6B4A2B, 0x3A2A1C, BARK));
		PlanKit.humanoid(b, new Humanoid(8, 7, 7, 12, 12, 8, 5, 17, 6, 8), "hide", "hide", "hide", "cloth", 2, 0xFFB030);
		b.get("body").xRot = 0.3F;
		Part head = b.get("head");
		head.xRot = 0.1F;
		b.part("brow", "head", Role.EXTRA, 0, -5, -3.5F, "hide", box(-4, -1, -1, 8, 2, 1));
		b.part("jaw", "head", Role.JAW, 0, -2, -1, "hide", box(-4, 0, -3, 8, 3, 4));
		b.part("club", "arm_r", Role.EXTRA, 0, 13, 0, "club", box(-2, 0, -2, 4, 12, 4), box(-3, 8, -3, 6, 6, 6));
		return b.build();
	}

	private static BodyPlan myconidShaman() {
		Builder b = BodyPlan.builder("myconid_shaman", Rig.BIPED).scale(1.05F)
			.skin("stalk", Skin.of(0xE8D8B8, 0xC8B898, SMOOTH))
			.skin("cap", Skin.of(0xC0262D, 0xF4F4F0, SPOTS))
			.skin("robe", Skin.of(0x5A3A2A, 0xD05050, CLOTH))
			.skin("staff", Skin.of(0x6B4A2B, 0x3A2A1C, BARK))
			.skin("spore", Skin.glowing(0xD05050, 0xFFB0B0, CRYSTAL));
		PlanKit.humanoid(b, new Humanoid(6, 7, 6, 7, 11, 4, 2, 13, 3, 12), "stalk", "robe", "stalk", "stalk", 2, 0xFFE070);
		b.part("cap", "head", Role.EXTRA, 0, -7, 0, "cap", box(-8, -5, -8, 16, 5, 16), box(-6, -7, -6, 12, 2, 12));
		b.part("staff", "arm_r", Role.EXTRA, 0, 10, 0, "staff", box(-0.5F, -14, -0.5F, 1, 20, 1));
		b.part("orb", "staff", Role.EXTRA, 0, -14, 0, "spore", box(-1.5F, -3, -1.5F, 3, 3, 3));
		return b.build();
	}

	private static BodyPlan jungleStalker() {
		Builder b = BodyPlan.builder("jungle_stalker", Rig.QUADRUPED)
			.skin("fur", Skin.of(0x1E1E24, 0x3A3A44, FUR))
			.skin("claw", Skin.of(0xD8D0B8, 0x8A8070, BONE));
		PlanKit.quadruped(b, new Quad(7, 7, 16, 6, 6, 6, 3, 9), "fur", "fur", "fur", 2, 0x40FF60);
		b.part("snout", "head", Role.EXTRA, 0, 1, -6, "fur", box(-1.5F, -1, -2, 3, 2, 2));
		b.part("ear_r", "head", Role.EXTRA, -2, -3, -2, "fur", box(-1, -2, -0.5F, 2, 2, 1));
		b.part("ear_l", "head", Role.EXTRA, 2, -3, -2, "fur", box(-1, -2, -0.5F, 2, 2, 1));
		b.part("fangs", "head", Role.JAW, 0, 2, -5, "claw", box(-1.5F, 0, -1, 3, 2, 1));
		PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -2, 8, 3, 2, 7, 0.9F, 1, 0.9F, "fur");
		return b.build();
	}

	private static BodyPlan madPig() {
		Builder b = BodyPlan.builder("mad_pig", Rig.QUADRUPED).scale(1.6F)
			.skin("skin", Skin.of(0xE8908A, 0xC0262D, SMOOTH))
			.skin("dark", Skin.of(0xB8605A, 0x8A2A2A, SMOOTH))
			.skin("tusk", Skin.of(0xF4F0E0, 0xC8C0A8, BONE));
		PlanKit.quadruped(b, new Quad(10, 8, 16, 8, 8, 8, 4, 6), "skin", "skin", "dark", 2, 0xFF2020);
		b.part("snout", "head", Role.EXTRA, 0, 1, -8, "dark", box(-2, -1.5F, -1, 4, 3, 1));
		b.part("tusk_r", "head", Role.EXTRA, -2.5F, 2, -7, "tusk", box(-0.5F, -3, -0.5F, 1, 3, 1));
		b.part("tusk_l", "head", Role.EXTRA, 2.5F, 2, -7, "tusk", box(-0.5F, -3, -0.5F, 1, 3, 1));
		b.part("bristles", "body", Role.EXTRA, 0, -4, 0, "dark", box(-1, -2, -7, 2, 2, 14));
		return b.build();
	}

	private static BodyPlan drownedCorsair() {
		Builder b = BodyPlan.builder("drowned_corsair", Rig.BIPED)
			.skin("skin", Skin.of(0x5A9A8A, 0x3A6A6A, SMOOTH))
			.skin("coat", Skin.of(0x2A3A5A, 0xC9A227, CLOTH))
			.skin("pants", Skin.of(0x3A3A40, 0x2A2A30, CLOTH))
			.skin("hat", Skin.of(0x1E1A1A, 0xC9A227, CLOTH))
			.skin("gun", Skin.of(0x5A3A26, 0x8A929E, METAL));
		PlanKit.humanoid(b, new Humanoid(8, 8, 8, 8, 12, 4, 4, 12, 4, 12), "skin", "coat", "coat", "pants", 2, 0x40FFE0);
		b.part("brim", "head", Role.EXTRA, 0, -8, 0, "hat", box(-6, -1, -5, 12, 1, 10));
		b.part("crown", "brim", Role.EXTRA, 0, -1, 0, "hat", box(-4, -3, -4, 8, 3, 8));
		b.part("coattail", "body", Role.CAPE, 0, 9, 2, "coat", box(-4, 0, 0, 8, 9, 1));
		b.part("pistol", "arm_r", Role.EXTRA, 0, 9, -1, "gun", box(-0.5F, -1, -5, 1, 2, 6));
		return b.build();
	}

	private static BodyPlan badlandsGunslinger() {
		Builder b = BodyPlan.builder("badlands_gunslinger", Rig.BIPED)
			.skin("bone", Skin.of(0xE8E0C8, 0x8A8070, BONE))
			.skin("poncho", Skin.of(0xA8582A, 0xE8C45A, CLOTH))
			.skin("hat", Skin.of(0x8A5A2A, 0x3A2A1C, CLOTH))
			.skin("gun", Skin.of(0x3A3A40, 0xC9A227, METAL));
		PlanKit.humanoid(b, new Humanoid(8, 8, 8, 8, 12, 4, 2, 12, 2, 12), "bone", "bone", "bone", "bone", 2, 0xFF3020);
		b.part("poncho", "body", Role.EXTRA, 0, 0, 0, "poncho", box(-5, 0, -3, 10, 7, 6));
		b.part("brim", "head", Role.EXTRA, 0, -8, 0, "hat", box(-7, -1, -7, 14, 1, 14));
		b.part("crown", "brim", Role.EXTRA, 0, -1, 0, "hat", box(-3.5F, -3, -3.5F, 7, 3, 7));
		b.part("gun_r", "arm_r", Role.EXTRA, 0, 9, 0, "gun", box(-0.5F, -1, -5, 1, 2, 6));
		b.part("gun_l", "arm_l", Role.EXTRA, 0, 9, 0, "gun", box(-0.5F, -1, -5, 1, 2, 6));
		return b.build();
	}

	private static BodyPlan wendigo() {
		Builder b = BodyPlan.builder("wendigo", Rig.BIPED).scale(1.3F)
			.skin("hide", Skin.of(0xC8C0B0, 0x8A8070, SMOOTH))
			.skin("skull", Skin.of(0xE8E0C8, 0x6A6050, BONE))
			.skin("fur", Skin.of(0x3A2A20, 0x5A4630, FUR))
			.skin("antler", Skin.of(0xD8C8A0, 0x8A7050, BONE));
		PlanKit.humanoid(b, new Humanoid(6, 8, 7, 7, 13, 4, 2, 21, 3, 18), "skull", "hide", "hide", "fur", 2, 0xFFFFFF);
		b.get("body").xRot = 0.15F;
		b.part("ribs", "body", Role.EXTRA, 0, 2, -2, "skull", box(-3, 0, -0.5F, 6, 6, 1));
		b.part("shoulders", "body", Role.EXTRA, 0, 0, 0, "fur", box(-5, -1, -3, 10, 4, 6));
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			Part base = b.part("antler_" + s, "head", Role.EXTRA, side * 2.5F, -8, 0, "antler", box(-0.5F, -6, -0.5F, 1, 6, 1));
			base.zRot = side * 0.4F;
			b.part("tine_" + s + "1", "antler_" + s, Role.EXTRA, 0, -4, 0, "antler", box(-0.5F, -4, -0.5F, 1, 4, 1)).zRot = side * 0.7F;
			b.part("tine_" + s + "2", "antler_" + s, Role.EXTRA, 0, -6, 0, "antler", box(-0.5F, -3, -0.5F, 1, 3, 1)).zRot = -side * 0.5F;
			b.part("claw_" + s, side < 0 ? "arm_r" : "arm_l", Role.EXTRA, 0, 18, -1, "antler", box(-1, 0, -1, 2, 3, 1));
		}
		return b.build();
	}

	private static BodyPlan amethystSentinel() {
		Builder b = BodyPlan.builder("amethyst_sentinel", Rig.BIPED).scale(1.4F)
			.skin("crystal", Skin.glowing(0x7A4AB8, 0xE0B0FF, CRYSTAL))
			.skin("rock", Skin.of(0x4A4458, 0x7A4AB8, STONE));
		PlanKit.humanoid(b, new Humanoid(7, 7, 7, 14, 12, 9, 5, 16, 6, 10), "rock", "rock", "rock", "rock", 1, 0xF0C0FF);
		b.part("core", "body", Role.EXTRA, 0, 3, -4.5F, "crystal", box(-3, 0, -1, 6, 6, 1));
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			Part spike = b.part("shard_" + s, "body", Role.EXTRA, side * 6, -1, 0, "crystal", box(-1.5F, -8, -1.5F, 3, 8, 3));
			spike.zRot = side * 0.35F;
			b.part("shard_" + s + "2", "body", Role.EXTRA, side * 3, -1, 3, "crystal", box(-1, -6, -1, 2, 6, 2)).zRot = side * 0.2F;
			b.part("fist_" + s, side < 0 ? "arm_r" : "arm_l", Role.EXTRA, 0, 13, 0, "crystal", box(-3, 0, -3, 6, 4, 6));
		}
		return b.build();
	}

	private static BodyPlan ancientTreant() {
		Builder b = BodyPlan.builder("ancient_treant", Rig.BIPED).scale(1.7F)
			.skin("bark", Skin.of(0x5A4024, 0x3A2A14, BARK))
			.skin("leaf", Skin.of(0x3E7A2A, 0x7FBF3A, FUR))
			.skin("moss", Skin.of(0x4E7A2E, 0x2E5A1E, SPOTS));
		PlanKit.humanoid(b, new Humanoid(10, 8, 8, 12, 16, 8, 5, 17, 6, 10), "bark", "bark", "bark", "bark", 2, 0xFFC040);
		b.part("crown", "head", Role.EXTRA, 0, -8, 0, "leaf", box(-11, -10, -9, 22, 10, 18));
		b.part("beard", "head", Role.EXTRA, 0, 0, -4, "moss", box(-4, -2, -1, 8, 6, 1));
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			b.part("branch_" + s, side < 0 ? "arm_r" : "arm_l", Role.EXTRA, side * 2, 4, 0, "bark", box(-1, -7, -1, 2, 8, 2)).zRot = side * 0.6F;
			b.part("twigs_" + s, side < 0 ? "arm_r" : "arm_l", Role.EXTRA, 0, 13, 0, "leaf", box(-3, 0, -3, 6, 4, 6));
		}
		b.part("roots", "body", Role.EXTRA, 0, 13, 0, "moss", box(-7, 0, -5, 14, 3, 10));
		return b.build();
	}

	private static BodyPlan magmaBehemoth() {
		Builder b = BodyPlan.builder("magma_behemoth", Rig.QUADRUPED).scale(1.5F)
			.skin("rock", Skin.glowing(0x2A1A16, 0xFF7A1A, CRACKED))
			.skin("horn", Skin.of(0x1A1210, 0xFF7A1A, STONE));
		PlanKit.quadruped(b, new Quad(14, 10, 20, 10, 8, 10, 5, 9), "rock", "rock", "rock", 2, 0xFFD040);
		b.part("horn", "head", Role.EXTRA, 0, -1, -9, "horn", box(-1.5F, -6, -1.5F, 3, 6, 3)).xRot = -0.5F;
		b.part("horn2", "head", Role.EXTRA, 0, -3, -5, "horn", box(-1, -4, -1, 2, 4, 2)).xRot = -0.3F;
		for (int i = 0; i < 4; i++) {
			b.part("spike" + i, "body", Role.EXTRA, 0, -5, -6 + i * 4, "horn", box(-1.5F, -4 + (i % 2), -1.5F, 3, 4 - (i % 2), 3));
		}
		b.part("jaw", "head", Role.JAW, 0, 3, -2, "rock", box(-4, 0, -8, 8, 2, 8));
		return b.build();
	}

	private static BodyPlan soulReaper() {
		Builder b = BodyPlan.builder("soul_reaper", Rig.FLOATER).scale(1.1F)
			.skin("cloak", Skin.of(0x1E2228, 0x3AA8C8, CLOTH))
			.skin("bone", Skin.of(0xC8D8D8, 0x6A8A8A, BONE))
			.skin("blade", Skin.glowing(0x6A8A9A, 0x9FF4FF, METAL))
			.skin("shaft", Skin.of(0x2A2420, 0x6A5A4A, BARK));
		b.part("body", null, Role.BODY, 0, -6, 0, "cloak", box(-4, 0, -3, 8, 14, 6));
		b.part("hem", "body", Role.EXTRA, 0, 14, 0, "cloak", box(-5, 0, -4, 10, 10, 8));
		Part hood = b.part("head", null, Role.HEAD, 0, -6, 0, "cloak", box(-4.5F, -9, -4.5F, 9, 9, 9));
		hood.eyes = 2;
		hood.eyeColor = 0x6FE0FF;
		b.part("face", "head", Role.EXTRA, 0, -2, -4.6F, "bone", box(-2.5F, -5, 0, 5, 5, 1));
		b.part("arm_r", null, Role.ARM_RIGHT, -5.5F, -4, 0, "bone", box(-1, -1, -1, 2, 14, 2));
		b.part("arm_l", null, Role.ARM_LEFT, 5.5F, -4, 0, "bone", box(-1, -1, -1, 2, 14, 2));
		b.part("scythe", "arm_r", Role.EXTRA, 0, 12, 0, "shaft", box(-0.5F, -18, -0.5F, 1, 26, 1));
		b.part("scythe_blade", "scythe", Role.EXTRA, 0, -18, 0, "blade", box(-0.5F, -1, -12, 1, 3, 12));
		return b.build();
	}

	private static BodyPlan witherKnight() {
		Builder b = BodyPlan.builder("wither_knight", Rig.BIPED).scale(1.25F)
			.skin("plate", Skin.of(0x2A2A30, 0x6A6A74, METAL))
			.skin("skull", Skin.of(0x3A3A3E, 0x1A1A1E, BONE))
			.skin("cape", Skin.of(0x5A1418, 0x2A0A0C, CLOTH))
			.skin("sword", Skin.glowing(0x3A3A44, 0x9A9AFF, METAL));
		PlanKit.humanoid(b, new Humanoid(8, 8, 8, 9, 12, 5, 4, 13, 4, 12), "skull", "plate", "plate", "plate", 2, 0xFFFFFF);
		b.part("pauldron_r", "arm_r", Role.EXTRA, 0, -2, 0, "plate", box(-3, -2, -3, 6, 4, 6));
		b.part("pauldron_l", "arm_l", Role.EXTRA, 0, -2, 0, "plate", box(-3, -2, -3, 6, 4, 6));
		b.part("horn_r", "head", Role.EXTRA, -4, -7, 0, "skull", box(-3, -1, -1, 3, 2, 2)).zRot = 0.5F;
		b.part("horn_l", "head", Role.EXTRA, 4, -7, 0, "skull", box(0, -1, -1, 3, 2, 2)).zRot = -0.5F;
		b.part("cape", "body", Role.CAPE, 0, 0, 2.5F, "cape", box(-5, 0, 0, 10, 20, 1));
		b.part("greatsword", "arm_r", Role.EXTRA, 0, 10, 0, "sword", box(-1, -24, -0.5F, 2, 24, 1), box(-3, 0, -1, 6, 1, 2));
		return b.build();
	}

	private static BodyPlan voidWatcher() {
		Builder b = BodyPlan.builder("void_watcher", Rig.FLOATER).scale(1.3F)
			.skin("void", Skin.glowing(0x1A1028, 0xC080FF, VOID))
			.skin("flesh", Skin.of(0x4A2A6A, 0x8A4ACF, SMOOTH))
			.skin("shard", Skin.glowing(0x6A2ACF, 0xE0B0FF, CRYSTAL));
		b.part("body", null, Role.BODY, 0, 4, 0, "void", box(-6, -12, -6, 12, 12, 12));
		Part eye = b.part("head", "body", Role.HEAD, 0, -6, -6, "flesh", box(-4, -4, -1, 8, 8, 1));
		eye.eyes = 1;
		eye.eyeColor = 0xFF40FF;
		b.part("lid_top", "body", Role.EXTRA, 0, -12, -6, "void", box(-6, 0, -1.5F, 12, 2, 2));
		for (int t = 0; t < 4; t++) {
			float a = (float)(Math.PI / 2 * t + Math.PI / 4);
			String root = "tentacle" + t;
			PlanKit.chain(b, root + "_", "body", Role.TENTACLE, t * 10, (float)Math.cos(a) * 4, 0, (float)Math.sin(a) * 4, 3, 2, 6, 0.85F, 0, 0.15F, "flesh");
		}
		b.part("orbit", "body", Role.ORBIT, 0, -6, 0, "shard");
		for (int i = 0; i < 3; i++) {
			double a = Math.PI * 2 * i / 3;
			b.part("orbit_shard" + i, "orbit", Role.EXTRA, (float)Math.cos(a) * 11, 0, (float)Math.sin(a) * 11, "shard", box(-1, -2, -1, 2, 4, 2));
		}
		return b.build();
	}

	private static BodyPlan echoStalker() {
		Builder b = BodyPlan.builder("echo_stalker", Rig.QUADRUPED).scale(1.3F)
			.skin("sculk", Skin.glowing(0x0E2A2E, 0x3AE0E0, CRACKED))
			.skin("bone", Skin.of(0x2A3A3E, 0x6AE0E0, BONE));
		PlanKit.quadruped(b, new Quad(12, 9, 18, 8, 7, 10, 4, 12), "sculk", "sculk", "sculk", 0, 0);
		b.part("jaw", "head", Role.JAW, 0, 3.5F, -1, "bone", box(-4, 0, -9, 8, 2, 9));
		for (int i = 0; i < 4; i++) {
			b.part("rib" + i, "body", Role.EXTRA, 0, -4, -6 + i * 4, "bone", box(-6, -4, -0.5F, 12, 4, 1));
		}
		b.part("feeler_r", "head", Role.EXTRA, -3, -3, -3, "bone", box(-0.5F, -6, -0.5F, 1, 6, 1)).zRot = -0.4F;
		b.part("feeler_l", "head", Role.EXTRA, 3, -3, -3, "bone", box(-0.5F, -6, -0.5F, 1, 6, 1)).zRot = 0.4F;
		PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -2, 9, 3, 4, 7, 0.8F, 1, 0.2F, "sculk");
		return b.build();
	}

	private static BodyPlan chorusWraith() {
		Builder b = BodyPlan.builder("chorus_wraith", Rig.FLOATER).scale(1.15F)
			.skin("robe", Skin.of(0x8A5AA8, 0x4A2A6A, CLOTH))
			.skin("chorus", Skin.glowing(0xC090D8, 0xF4D8FF, SPOTS))
			.skin("hand", Skin.of(0xD8C8E8, 0x8A7AA8, SMOOTH));
		b.part("body", null, Role.BODY, 0, -6, 0, "robe", box(-4, 0, -3, 8, 14, 6));
		b.part("hem", "body", Role.EXTRA, 0, 14, 0, "robe", box(-5, 0, -3.5F, 10, 9, 7));
		Part hood = b.part("head", null, Role.HEAD, 0, -6, 0, "robe", box(-4.5F, -9, -4.5F, 9, 9, 9));
		hood.eyes = 2;
		hood.eyeColor = 0xFFFFFF;
		b.part("arm_r", null, Role.ARM_RIGHT, -5.5F, -4, 0, "hand", box(-1, -1, -1, 2, 16, 2));
		b.part("arm_l", null, Role.ARM_LEFT, 5.5F, -4, 0, "hand", box(-1, -1, -1, 2, 16, 2));
		b.part("growth_r", "body", Role.EXTRA, -4, 0, 0, "chorus", box(-3, -6, -2, 4, 6, 4));
		b.part("growth_l", "body", Role.EXTRA, 4, 0, 0, "chorus", box(-1, -8, -2, 4, 8, 4));
		b.part("growth_top", "head", Role.EXTRA, 1, -9, 1, "chorus", box(-2, -4, -2, 3, 4, 3));
		return b.build();
	}

	private static BodyPlan astralKnight() {
		Builder b = BodyPlan.builder("astral_knight", Rig.BIPED).scale(1.3F)
			.skin("plate", Skin.glowing(0x1A2050, 0xFFF4C0, VOID))
			.skin("gold", Skin.of(0xD9B44A, 0xFFF4C0, METAL))
			.skin("cape", Skin.glowing(0x101838, 0x9FD0FF, VOID))
			.skin("halo", Skin.glowing(0xFFE58A, 0xFFFFFF, CRYSTAL));
		PlanKit.humanoid(b, new Humanoid(8, 8, 8, 9, 12, 5, 4, 13, 4, 12), "plate", "plate", "plate", "plate", 2, 0x9FD0FF);
		b.part("visor", "head", Role.EXTRA, 0, -5, -4.2F, "gold", box(-4, -1, -0.5F, 8, 1, 1));
		b.part("pauldron_r", "arm_r", Role.EXTRA, 0, -2, 0, "gold", box(-3, -2, -3, 6, 3, 6));
		b.part("pauldron_l", "arm_l", Role.EXTRA, 0, -2, 0, "gold", box(-3, -2, -3, 6, 3, 6));
		b.part("cape", "body", Role.CAPE, 0, 0, 2.5F, "cape", box(-5, 0, 0, 10, 22, 1));
		b.part("lance", "arm_r", Role.EXTRA, 0, 10, 0, "gold", box(-1, -1, -26, 2, 2, 30));
		b.part("halo", "head", Role.ORBIT, 0, -11, 0, "halo");
		for (int i = 0; i < 8; i++) {
			double a = Math.PI * 2 * i / 8;
			b.part("halo" + i, "halo", Role.EXTRA, (float)Math.cos(a) * 5, 0, (float)Math.sin(a) * 5, "halo", box(-1, -0.5F, -1, 2, 1, 2));
		}
		return b.build();
	}

	private NamedPlans() {
	}
}
