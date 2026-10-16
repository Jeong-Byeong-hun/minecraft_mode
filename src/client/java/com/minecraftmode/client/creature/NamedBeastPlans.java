package com.minecraftmode.client.creature;

import static com.minecraftmode.client.creature.BodyPlan.box;
import static com.minecraftmode.client.creature.Skin.Pattern.*;

import com.minecraftmode.client.creature.BodyPlan.Builder;
import com.minecraftmode.client.creature.BodyPlan.Part;
import com.minecraftmode.client.creature.BodyPlan.Rig;
import com.minecraftmode.client.creature.BodyPlan.Role;
import com.minecraftmode.client.creature.PlanKit.Quad;
import net.minecraft.util.Mth;

/** The beasts among the named monsters (listed in {@link NamedPlans#all()}): four-legged ones bend at the knee, the scorpion's legs at a raised joint. */
final class NamedBeastPlans {
	static BodyPlan frostAlpha() {
		Builder b = BodyPlan.builder("frost_alpha", Rig.QUADRUPED).scale(1.15F).shaded()
			.skin("fur", Skin.of(0xF2F6FA, 0x8CA6C2, COAT))
			.skin("dark", Skin.of(0x8AA8C8, 0x5A7898, FUR))
			.skin("nose", Skin.of(0x1A2230, 0x3A4A60, SMOOTH))
			.skin("ice", Skin.glowing(0x9FD0FF, 0xE8FFFF, CRYSTAL));
		Quad q = new Quad(8, 8, 16, 8, 7, 7, 3, 9);
		PlanKit.torso(b, q, "fur");
		PlanKit.head(b, q, "fur", 0x6FD0FF);
		Part snout = b.part("snout", "head", Role.EXTRA, 0, 1, -7, "fur", box(-2, -1.5F, -4, 4, 3, 4));
		b.part("nose", snout.name, Role.EXTRA, 0, -1.5F, -4, "nose", box(-1, 0, -0.5F, 2, 1, 1));
		b.part("jaw", snout.name, Role.JAW, 0, 1.5F, 0, "dark", box(-2, 0, -4, 4, 1, 4));
		b.part("fangs", "jaw", Role.EXTRA, 0, 0, -3.5F, "ice", box(-1.5F, -1, -0.5F, 1, 1, 1), box(0.5F, -1, -0.5F, 1, 1, 1));
		PlanKit.ears(b, "head", 2.5F, -3.5F, -3, 2, 3, 1, "fur", "dark");
		b.part("mane", "body", Role.EXTRA, 0, -1, -6, "fur", box(-5, -5, -3, 10, 9, 6), box(-3.5F, 4, -2, 7, 2, 4));
		for (int i = 0; i < 4; i++) {
			Part spike = b.part("spike" + i, "body", Role.EXTRA, (i % 2 == 0 ? -1 : 1) * 1.5F, -4, -3 + i * 2.5F, "ice", box(-1, -4 + (i % 2), -1, 2, 4 - (i % 2), 2));
			spike.xRot = -0.35F;
			spike.zRot = (i % 2 == 0 ? -1 : 1) * 0.25F;
		}
		PlanKit.jointedLegs(b, q, "fur", "dark");
		for (int i = 0; i < 4; i++) {
			b.part("paw" + i, "shin" + i, Role.EXTRA, 0, 3, 0, "dark", box(-1.5F, 0, -2.5F, 3, 1, 3));
		}
		PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -2, 8, 3, 3, 5, 0.9F, 1, 0.0F, "fur");
		PlanKit.bendChain(b, "tail", 3, -0.5F, 0.2F);
		return b.build();
	}

	static BodyPlan duneScorpion() {
		Builder b = BodyPlan.builder("dune_scorpion", Rig.ARACHNID).scale(1.2F).shaded()
			.skin("shell", Skin.of(0xC8A050, 0x8A6A2A, CHITIN))
			.skin("dark", Skin.of(0x8A6A2A, 0x5A4220, CHITIN))
			.skin("sting", Skin.glowing(0x6A8A2A, 0xB0FF40, CRYSTAL));
		b.part("body", null, Role.BODY, 0, 18, 0, "shell", box(-5, -2.5F, -6, 10, 5, 8), box(-4.5F, -2, 2, 9, 4, 8));
		b.part("ridge", "body", Role.EXTRA, 0, -2.5F, 0, "dark", box(-1, -1, -5, 2, 1, 14));
		Part head = b.part("head", "body", Role.HEAD, 0, 0, -6, "shell", box(-3.5F, -2, -3, 7, 4, 3));
		head.eyes = 4;
		head.eyeColor = 0x101010;
		b.part("mandible_r", "head", Role.JAW, -1.5F, 1, -3, "dark", box(-0.5F, 0, -2, 1, 1, 2));
		b.part("mandible_l", "head", Role.JAW, 1.5F, 1, -3, "dark", box(-0.5F, 0, -2, 1, 1, 2));
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			Part arm = b.part("claw_arm_" + s, "body", Role.EXTRA, side * 4, 0, -5, "shell", box(-1.5F, -1.5F, -7, 3, 3, 7));
			arm.yRot = side * -0.4F;
			Part pincer = b.part("pincer_" + s, "claw_arm_" + s, Role.JAW, 0, 0, -7, "dark", box(-2.5F, -1.5F, -5, 5, 3, 5));
			b.part("finger_" + s, pincer.name, Role.EXTRA, side * -2, 0, -5, "dark", box(-0.5F, -1, -3, 1, 2, 3)).yRot = side * 0.3F;
		}
		PlanKit.jointedSpiderLegs(b, "body", 4, 5, 1, -3, 6, 6, 2, "dark", "dark");
		String tip = PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -1, 10, 5, 4, 6, 0.85F, 1, -0.75F, "shell");
		b.part("stinger", tip, Role.EXTRA, 0, 0, 4, "sting", box(-1, -1, 0, 2, 2, 4)).xRot = -0.8F;
		return b.build();
	}

	static BodyPlan jungleStalker() {
		Builder b = BodyPlan.builder("jungle_stalker", Rig.QUADRUPED).shaded()
			.skin("fur", Skin.of(0x2E2E38, 0x121216, COAT))
			.skin("muzzle", Skin.of(0x4A4A56, 0x3A3A44, FUR))
			.skin("claw", Skin.of(0xD8D0B8, 0x8A8070, BONE));
		Quad q = new Quad(7, 7, 16, 6, 6, 6, 3, 9);
		PlanKit.torso(b, q, "fur");
		b.part("shoulders", "body", Role.EXTRA, 0, -3.5F, -5, "fur", box(-3, -1, -2, 6, 1, 4));
		PlanKit.head(b, q, "fur", 0x40FF60);
		b.part("snout", "head", Role.EXTRA, 0, 1, -6, "muzzle", box(-1.5F, -1, -2, 3, 2, 2));
		b.part("fangs", "head", Role.JAW, 0, 2, -5, "claw", box(-1.5F, 0, -1, 3, 2, 1));
		PlanKit.ears(b, "head", 2, -3, -2, 2, 2, 1, "fur", "");
		PlanKit.jointedLegs(b, q, "fur", "fur");
		for (int i = 0; i < 2; i++) {
			b.part("claws" + i, "shin" + i, Role.EXTRA, 0, 3, 0, "claw", box(-1.5F, 0, -2.5F, 3, 1, 1));
		}
		PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -2, 8, 5, 2, 4, 0.95F, 1, 0.0F, "fur");
		PlanKit.bendChain(b, "tail", 5, -0.6F, 0.25F);
		return b.build();
	}

	static BodyPlan madPig() {
		Builder b = BodyPlan.builder("mad_pig", Rig.QUADRUPED).scale(1.6F).shaded()
			.skin("skin", Skin.of(0xE8908A, 0xC0262D, SMOOTH))
			.skin("dark", Skin.of(0xB8605A, 0x8A2A2A, SMOOTH))
			.skin("bristle", Skin.of(0x5A2A20, 0x3A1A14, FUR))
			.skin("hoof", Skin.of(0x3A2A24, 0x5A4A40, SMOOTH))
			.skin("tusk", Skin.of(0xF4F0E0, 0xC8C0A8, BONE));
		Quad q = new Quad(10, 8, 16, 8, 8, 8, 4, 6);
		PlanKit.torso(b, q, "skin");
		PlanKit.head(b, q, "skin", 0xFF2020);
		Part snout = b.part("snout", "head", Role.EXTRA, 0, 1, -8, "dark", box(-2.5F, -2, -2, 5, 4, 2));
		b.part("nostrils", snout.name, Role.EXTRA, 0, 0, -2, "hoof", box(-1.5F, -0.5F, -0.4F, 1, 1, 1), box(0.5F, -0.5F, -0.4F, 1, 1, 1));
		b.part("brow", "head", Role.EXTRA, 0, -3, -8, "dark", box(-3.5F, -1, -0.5F, 7, 1, 1)).xRot = 0.2F;
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			Part tusk = b.part("tusk_" + s, "head", Role.EXTRA, side * 2.5F, 2, -7, "tusk", box(-0.5F, -3, -0.5F, 1, 3, 1));
			tusk.zRot = side * 0.2F;
			b.part("tusk_tip_" + s, tusk.name, Role.EXTRA, 0, -3, 0, "tusk", box(-0.5F, -2, -0.5F, 1, 2, 1)).zRot = side * 0.6F;
			b.part("ear_" + s, "head", Role.EAR, side < 0 ? 0 : 1, side * 3, -3.5F, -2, "dark", box(side < 0 ? -3 : 0, -1, -0.5F, 3, 3, 1)).zRot = side * -0.6F;
		}
		b.part("bristles", "body", Role.EXTRA, 0, -4, 0, "bristle", box(-1, -2, -7, 2, 2, 14), box(-0.5F, -3, -6, 1, 1, 4));
		b.part("mohawk", "head", Role.EXTRA, 0, -4, -3, "bristle", box(-0.5F, -2, -2, 1, 2, 5));
		PlanKit.jointedLegs(b, q, "skin", "dark");
		for (int i = 0; i < 4; i++) {
			b.part("hoof" + i, "shin" + i, Role.EXTRA, 0, 2, 0, "hoof", box(-2, 0, -2, 4, 1, 4));
		}
		PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -2, 8, 3, 1, 2, 1.0F, 1, 0.9F, "dark");
		return b.build();
	}

	static BodyPlan magmaBehemoth() {
		Builder b = BodyPlan.builder("magma_behemoth", Rig.QUADRUPED).scale(1.5F).shaded()
			.skin("rock", Skin.glowing(0x2A1A16, 0xFF7A1A, CRACKED))
			.skin("horn", Skin.of(0x1A1210, 0xFF7A1A, STONE))
			.skin("lava", Skin.glowing(0xE8501A, 0xFFF0A0, FLAME));
		Quad q = new Quad(14, 10, 20, 10, 8, 10, 5, 9);
		PlanKit.torso(b, q, "rock");
		b.part("chest_crack", "body", Role.EXTRA, 0, 0, -10, "lava", box(-2, -3, -0.6F, 4, 6, 1));
		PlanKit.head(b, q, "rock", 0xFFD040);
		b.part("horn", "head", Role.EXTRA, 0, -1, -9, "horn", box(-1.5F, -6, -1.5F, 3, 6, 3)).xRot = -0.5F;
		b.part("horn2", "head", Role.EXTRA, 0, -3, -5, "horn", box(-1, -4, -1, 2, 4, 2)).xRot = -0.3F;
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			Part base = b.part("side_horn_" + s, "head", Role.EXTRA, side * 5, -2, -3, "horn", box(side < 0 ? -4 : 0, -1.5F, -1.5F, 4, 3, 3));
			base.zRot = side * -0.3F;
			b.part("side_horn_tip_" + s, base.name, Role.EXTRA, side * 4, 0, 0, "horn", box(-1, -4, -1, 2, 4, 2)).zRot = side * 0.4F;
		}
		Part jaw = b.part("jaw", "head", Role.JAW, 0, 3, -2, "rock", box(-4, 0, -8, 8, 2, 8));
		b.part("drool", jaw.name, Role.EXTRA, 0, 2, -6, "lava", box(-2.5F, 0, -1, 1, 2, 1), box(1.5F, 0, 0, 1, 3, 1));
		for (int i = 0; i < 4; i++) {
			b.part("spike" + i, "body", Role.EXTRA, 0, -5, -6 + i * 4, "horn", box(-1.5F, -4 + (i % 2), -1.5F, 3, 4 - (i % 2), 3));
		}
		PlanKit.jointedLegs(b, q, "rock", "rock");
		for (int i = 0; i < 4; i++) {
			b.part("foot" + i, "shin" + i, Role.EXTRA, 0, 2, 0, "horn", box(-3, 0, -3.5F, 6, 2, 6));
		}
		PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -1, 10, 3, 5, 4, 0.75F, 1, -0.3F, "rock");
		return b.build();
	}

	static BodyPlan echoStalker() {
		Builder b = BodyPlan.builder("echo_stalker", Rig.QUADRUPED).scale(1.3F).shaded()
			.skin("sculk", Skin.glowing(0x0E2A2E, 0x3AE0E0, CRACKED))
			.skin("bone", Skin.of(0x2A3A3E, 0x6AE0E0, BONE))
			.skin("glow", Skin.glowing(0x0E4A4E, 0x9FFFFF, CRYSTAL));
		Quad q = new Quad(12, 9, 18, 8, 7, 10, 4, 12);
		PlanKit.torso(b, q, "sculk");
		Part head = PlanKit.head(b, q, "sculk", 0);
		head.eyes = 0;
		Part jaw = b.part("jaw", "head", Role.JAW, 0, 3.5F, -1, "bone", box(-4, 0, -9, 8, 2, 9));
		b.part("teeth", jaw.name, Role.EXTRA, 0, 0, -8, "glow", box(-3.5F, -1, -0.5F, 1, 1, 1), box(-1, -1, -0.5F, 1, 1, 1), box(2.5F, -1, -0.5F, 1, 1, 1));
		for (int i = 0; i < 4; i++) {
			b.part("rib" + i, "body", Role.EXTRA, 0, -4, -6 + i * 4, "bone", box(-6, -4, -0.5F, 12, 4, 1));
		}
		b.part("feeler_r", "head", Role.EAR, 0, -3, -3, -3, "bone", box(-0.5F, -6, -0.5F, 1, 6, 1)).zRot = -0.4F;
		b.part("feeler_l", "head", Role.EAR, 1, 3, -3, -3, "bone", box(-0.5F, -6, -0.5F, 1, 6, 1)).zRot = 0.4F;
		b.part("feeler_tips", "head", Role.EXTRA, 0, -3, -3, "glow", box(-5.5F, -6.5F, -0.5F, 1, 1, 1), box(4.5F, -6.5F, -0.5F, 1, 1, 1));
		for (int t = 0; t < 3; t++) {
			Part root = b.part("tendril_root" + t, "body", Role.EXTRA, (t - 1) * 3.5F, -4.5F, -2 + t * 3, "sculk");
			root.xRot = Mth.PI;
			PlanKit.chain(b, "tendril" + t + "_", root.name, Role.TENTACLE, t * 10, 0, 0, 0, 3, 2, 3, 0.8F, 0, 0.2F, t == 1 ? "glow" : "sculk");
		}
		PlanKit.jointedLegs(b, q, "sculk", "sculk");
		PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -2, 9, 3, 4, 7, 0.8F, 1, 0.2F, "sculk");
		return b.build();
	}

	private NamedBeastPlans() {
	}
}
