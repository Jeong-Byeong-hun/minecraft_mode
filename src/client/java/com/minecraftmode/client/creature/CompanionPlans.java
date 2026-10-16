package com.minecraftmode.client.creature;

import static com.minecraftmode.client.creature.BodyPlan.box;
import static com.minecraftmode.client.creature.Skin.Pattern.*;

import com.minecraftmode.client.creature.BodyPlan.Builder;
import com.minecraftmode.client.creature.BodyPlan.Part;
import com.minecraftmode.client.creature.BodyPlan.Rest;
import com.minecraftmode.client.creature.BodyPlan.Rig;
import com.minecraftmode.client.creature.BodyPlan.Role;
import com.minecraftmode.client.creature.PlanKit.Quad;
import java.util.List;

/**
 * Body plans of the pets ({@code pet_<id>}) and mounts ({@code mount_<id>}); ids match their entity types. Walking pets sit or
 * lie down while their owner stands still ({@link Rest}), winged ones fold their wings ({@link Builder#folds()}); four-legged
 * ones bend at the knee ({@link Role#SHIN}) and ground mounts carry a saddle.
 */
final class CompanionPlans {
	static List<BodyPlan> all() {
		return List.of(
			emberFox(), frostOwl(), goldenScarab(), mossTurtle(), sparkSprite(), shadowCat(), crystalSlime(), babyDragon(),
			swiftStallion(), duneRaptor(), frostWolf(), emberLion(), crystalStag(), stormGriffin()
		);
	}

	// ------------------------------------------------------------------ pets

	private static BodyPlan emberFox() {
		Builder b = BodyPlan.builder("pet_ember_fox", Rig.QUADRUPED).scale(0.6F).shaded().rest(Rest.SIT)
			.skin("fur", Skin.of(0xEC8A3C, 0xC2521C, COAT))
			.skin("white", Skin.of(0xF4ECE0, 0xD8C8B0, FUR))
			.skin("dark", Skin.of(0x3A2418, 0x5A3A24, FUR))
			.skin("ember", Skin.glowing(0xFF7A1A, 0xFFE070, FLAME))
			.skin("nose", Skin.of(0x1A1414, 0x3A3030, SMOOTH));
		Quad q = new Quad(6, 6, 12, 6, 5, 5, 2, 6);
		PlanKit.torso(b, q, "fur");
		b.part("bib", "body", Role.EXTRA, 0, 1, -5, "white", box(-2.5F, -2, -2, 5, 4, 3));
		b.part("belly", "body", Role.EXTRA, 0, 3, 0, "white", box(-2, -0.5F, -4, 4, 1, 8));
		PlanKit.head(b, q, "fur", 0xFFC040);
		Part snout = b.part("snout", "head", Role.EXTRA, 0, 1, -5, "white", box(-1.5F, -1, -3, 3, 2, 3));
		b.part("nose", "snout", Role.EXTRA, 0, 0, 0, "nose", box(-0.5F, -1.2F, -3.4F, 1, 1, 1));
		snout.xRot = 0.05F;
		b.part("jaw", "head", Role.JAW, 0, 2, -5, "white", box(-1, 0, -2.5F, 2, 1, 3));
		for (int side = -1; side <= 1; side += 2) {
			b.part("cheek_" + (side < 0 ? "r" : "l"), "head", Role.EXTRA, side * 3, 1, -2, "white", box(side < 0 ? -1 : 0, -1.5F, -1.5F, 1, 3, 3));
		}
		PlanKit.ears(b, "head", 2, -2.5F, -2, 2, 3, 1, "fur", "dark");
		PlanKit.jointedLegs(b, q, "fur", "dark");
		String tail = PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -1, 6, 3, 4, 4, 1.0F, 1, 0.0F, "fur");
		PlanKit.bendChain(b, "tail", 3, -0.55F, 0.3F);
		b.part("tail_tip", tail, Role.EXTRA, 0, 0, 4, "ember", box(-2, -2, 0, 4, 4, 3));
		return b.build();
	}

	private static BodyPlan frostOwl() {
		Builder b = BodyPlan.builder("pet_frost_owl", Rig.FLYER).scale(0.5F).shaded().folds()
			.skin("feather", Skin.of(0xE8F0F8, 0x9FB8D0, FEATHER))
			.skin("belly", Skin.of(0xF8FCFF, 0xB8C8D8, FEATHER))
			.skin("face", Skin.of(0xFFFFFF, 0xD0E0F0, SMOOTH))
			.skin("tip", Skin.of(0x8AA0C0, 0xC8D8F0, FEATHER))
			.skin("ice", Skin.glowing(0x9FD0FF, 0xE8FFFF, CRYSTAL))
			.skin("beak", Skin.of(0xE8C45A, 0x8A6A1A, SMOOTH));
		b.part("body", null, Role.BODY, 0, 13, 0, "feather", box(-4, -5, -3.5F, 8, 10, 7));
		b.part("chest", "body", Role.EXTRA, 0, -2, -3.5F, "belly", box(-3, -2, -1, 6, 8, 1));
		b.part("head", "body", Role.HEAD, 0, -5, 0, "feather", box(-4.5F, -7, -4, 9, 7, 8));
		Part face = b.part("face", "head", Role.EXTRA, 0, -3.5F, -4, "face", box(-3.5F, -3, -1, 7, 5, 1));
		face.eyes = 2;
		face.eyeColor = 0x6FD0FF;
		b.part("beak", "face", Role.JAW, 0, 0.5F, -1, "beak", box(-0.5F, 0, -1.5F, 1, 2, 2));
		PlanKit.ears(b, "head", 3, -7, -1, 2, 3, 2, "ice", "");
		b.get("ear_r").zRot = -0.35F;
		b.get("ear_l").zRot = 0.35F;
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			Role role = side < 0 ? Role.WING_RIGHT : Role.WING_LEFT;
			b.part("wing_" + s, "body", role, 0, side * 4, -3, -3, "feather", box(side < 0 ? -8 : 0, -0.5F, 0, 8, 1, 8));
			b.part("primaries_" + s, "wing_" + s, Role.EXTRA, 0, 0, 0, "tip", box(side < 0 ? -8 : 4, -0.5F, 6, 4, 1, 4));
			b.part("talon_" + s, "body", Role.EXTRA, side * 1.5F, 5, -1, "beak", box(-1, 0, -1.5F, 2, 2, 3));
		}
		Part tail = b.part("tail", "body", Role.EXTRA, 0, 3, 3, "tip", box(-2.5F, 0, 0, 5, 1, 4));
		tail.xRot = -0.5F;
		return b.build();
	}

	private static BodyPlan goldenScarab() {
		Builder b = BodyPlan.builder("pet_golden_scarab", Rig.QUADRUPED).scale(0.5F).shaded().rest(Rest.LIE)
			.skin("shell", Skin.glowing(0xD9B44A, 0xFFF0A0, METAL))
			.skin("under", Skin.of(0x3A2A14, 0x6A4A20, CHITIN))
			.skin("leg", Skin.of(0x5A4220, 0x2A1A10, CHITIN))
			.skin("gem", Skin.glowing(0x40C870, 0xC0FFD0, CRYSTAL));
		b.part("body", null, Role.BODY, 0, 18, 0, "under", box(-4, -2.5F, -5, 8, 5, 10));
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			Role role = side < 0 ? Role.WING_RIGHT : Role.WING_LEFT;
			b.part("elytron_" + s, "body", role, 0, 0, -2.5F, -4, "shell",
				box(side < 0 ? -4.5F : 0.5F, -2, 0, 4, 3, 10),
				box(side < 0 ? -3.5F : 0.5F, -3, 1, 3, 1, 8));
		}
		b.part("pronotum", "body", Role.EXTRA, 0, -1, -5, "shell", box(-3.5F, -2.5F, -3, 7, 4, 3));
		b.part("gem", "pronotum", Role.EXTRA, 0, -2.5F, -1.5F, "gem", box(-1, -1, -1, 2, 1, 2));
		Part head = b.part("head", "body", Role.HEAD, 0, 0.5F, -8, "under", box(-2.5F, -1.5F, -2, 5, 3, 2));
		head.eyes = 2;
		head.eyeColor = 0x40FF80;
		Part horn = b.part("horn", "head", Role.EXTRA, 0, -1.5F, -2, "shell", box(-0.5F, -4, -1, 1, 4, 1));
		horn.xRot = -0.5F;
		b.part("horn_tip", "horn", Role.EXTRA, 0, -4, 0, "shell", box(-0.5F, -2, -1, 1, 2, 1)).xRot = -0.7F;
		PlanKit.ears(b, "head", 1.5F, -1.5F, -1.5F, 1, 3, 1, "leg", "shell");
		b.get("ear_r").zRot = -0.5F;
		b.get("ear_l").zRot = 0.5F;
		float[] fan = {0.6F, 0.0F, -0.6F};
		for (int i = 0; i < 3; i++) {
			float z = -3 + i * 3;
			Part left = b.part("leg_l" + i, "body", Role.LEG_SPIDER, i, 4, 1, z, "leg", box(-0.5F, -0.5F, -0.5F, 6, 1, 1));
			left.zRot = 0.95F;
			left.yRot = fan[i];
			Part right = b.part("leg_r" + i, "body", Role.LEG_SPIDER, i + 4, -4, 1, z, "leg", box(-5.5F, -0.5F, -0.5F, 6, 1, 1));
			right.zRot = -0.95F;
			right.yRot = -fan[i];
		}
		return b.build();
	}

	private static BodyPlan mossTurtle() {
		Builder b = BodyPlan.builder("pet_moss_turtle", Rig.QUADRUPED).scale(0.5F).shaded().rest(Rest.LIE)
			.skin("skin", Skin.of(0x6A9A5A, 0x4A7A3A, SCALES))
			.skin("belly", Skin.of(0xC8C080, 0xA09A60, SMOOTH))
			.skin("shell", Skin.of(0x5A6A3A, 0x7AA850, BARK))
			.skin("moss", Skin.glowing(0x6AC850, 0xC0FF80, SPOTS))
			.skin("beak", Skin.of(0x3A3A20, 0x5A5A30, SMOOTH))
			.skin("bloom", Skin.glowing(0xF070A0, 0xFFE070, SPOTS));
		b.part("body", null, Role.BODY, 0, 19, 0, "belly", box(-5, -2, -6, 10, 4, 12));
		b.part("shell", "body", Role.EXTRA, 0, -2, 0, "shell",
			box(-6.5F, -1.5F, -7.5F, 13, 2, 15),
			box(-5.5F, -3.5F, -6.5F, 11, 2, 13),
			box(-4, -5, -5, 8, 2, 10));
		b.part("moss", "shell", Role.EXTRA, 0, -5, 0, "moss", box(-3.5F, -0.6F, -4, 7, 1, 6), box(-5, 1.4F, 2, 3, 1, 4));
		b.part("stem", "shell", Role.EXTRA, 1.5F, -5.5F, 1, "skin", box(-0.5F, -2, -0.5F, 1, 2, 1));
		b.part("bloom", "stem", Role.EXTRA, 0, -2, 0, "bloom", box(-1, -1, -1, 2, 1, 2));
		Part head = b.part("head", null, Role.HEAD, 0, 18.5F, -6, "skin", box(-2, -2, -4, 4, 4, 4));
		head.eyes = 2;
		head.eyeColor = 0x2A2A10;
		b.part("beak", "head", Role.JAW, 0, 0.5F, -4, "beak", box(-1.5F, 0, -1, 3, 1, 1));
		float[][] legs = {{5, -4.5F}, {-5, -4.5F}, {5, 4.5F}, {-5, 4.5F}};
		for (int i = 0; i < 4; i++) {
			Part leg = b.part("leg" + i, null, Role.LEG_QUAD, i, legs[i][0], 21, legs[i][1], "skin", box(-1.5F, 0, -1.5F, 3, 3, 3));
			leg.zRot = legs[i][0] > 0 ? -0.3F : 0.3F;
			b.part("claw" + i, "leg" + i, Role.EXTRA, 0, 2, 0, "beak", box(-1.5F, 0, -2, 3, 1, 1));
		}
		PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, 0.5F, 6, 2, 2, 2, 0.7F, 1, -0.2F, "skin");
		return b.build();
	}

	private static BodyPlan sparkSprite() {
		Builder b = BodyPlan.builder("pet_spark_sprite", Rig.FLOATER).scale(0.45F).shaded()
			.skin("core", Skin.glowing(0x4A8CFF, 0xE0F0FF, CRYSTAL))
			.skin("face", Skin.of(0x1A2A5A, 0x2A3A7A, SMOOTH))
			.skin("wing", Skin.glowing(0x9FD0FF, 0xFFFFFF, VOID))
			.skin("halo", Skin.glowing(0xFFF0A0, 0xFFFFFF, CRYSTAL));
		b.part("body", null, Role.BODY, 0, 9, 0, "core", box(-3, -3, -3, 6, 6, 6));
		Part head = b.part("head", "body", Role.HEAD, 0, 0, -3, "face", box(-2.5F, -2, -1, 5, 4, 1));
		head.eyes = 2;
		head.eyeColor = 0xFFFFFF;
		// a diamond shard on top and below, tilted 45 degrees
		Part top = b.part("crest", "body", Role.EXTRA, 0, -4, 0, "core", box(-1.5F, -1.5F, -1.5F, 3, 3, 3));
		top.xRot = 0.785F;
		top.zRot = 0.785F;
		Part bottom = b.part("keel", "body", Role.EXTRA, 0, 4, 0, "core", box(-1, -1, -1, 2, 2, 2));
		bottom.xRot = 0.785F;
		bottom.zRot = 0.785F;
		b.part("halo", "body", Role.ORBIT, 0, -6.5F, 0, "halo",
			box(-3, 0, -3, 6, 1, 1), box(-3, 0, 2, 6, 1, 1), box(-3, 0, -2, 1, 1, 4), box(2, 0, -2, 1, 1, 4));
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			Role role = side < 0 ? Role.WING_RIGHT : Role.WING_LEFT;
			b.part("wing_" + s, "body", role, side * 3, -1.5F, 1.5F, "wing", box(side < 0 ? -7 : 0, -6, 0, 7, 7, 1)).zRot = side * 0.2F;
			b.part("hindwing_" + s, "body", role, 1, side * 3, 1, 1.5F, "wing", box(side < 0 ? -5 : 0, 0, 0, 5, 4, 1)).zRot = side * -0.3F;
		}
		b.part("orbit", "body", Role.ORBIT, 0, 0, 0, "core");
		for (int i = 0; i < 3; i++) {
			double a = Math.PI * 2 * i / 3;
			b.part("spark" + i, "orbit", Role.EXTRA, (float)Math.cos(a) * 7, 0, (float)Math.sin(a) * 7, "wing", box(-0.5F, -0.5F, -0.5F, 1, 1, 1));
		}
		Part tilted = b.part("orbit2", "body", Role.ORBIT, 0, 0, 0, "core");
		tilted.zRot = 0.6F;
		for (int i = 0; i < 2; i++) {
			b.part("mote" + i, "orbit2", Role.EXTRA, i == 0 ? 9 : -9, 0, 0, "halo", box(-0.5F, -0.5F, -0.5F, 1, 1, 1));
		}
		PlanKit.chain(b, "wisp", "body", Role.TAIL, 0, 0, 3, 1, 3, 2, 3, 0.7F, 0, 0.15F, "wing");
		return b.build();
	}

	private static BodyPlan shadowCat() {
		Builder b = BodyPlan.builder("pet_shadow_cat", Rig.QUADRUPED).scale(0.5F).shaded().rest(Rest.SIT)
			.skin("fur", Skin.of(0x1E1A2A, 0x6A4A9A, VOID))
			.skin("muzzle", Skin.of(0x2E2A3C, 0x4A4060, FUR))
			.skin("glow", Skin.glowing(0x8A4ACF, 0xE0B0FF, VOID))
			.skin("bell", Skin.glowing(0xE8C050, 0xFFF0A0, METAL));
		Quad q = new Quad(5, 5, 11, 5, 4, 5, 2, 6);
		PlanKit.torso(b, q, "fur");
		b.part("rune", "body", Role.EXTRA, 0, -2.5F, 0, "glow", box(-0.5F, -0.6F, -4, 1, 1, 8));
		PlanKit.head(b, q, "fur", 0xC080FF);
		b.part("muzzle", "head", Role.EXTRA, 0, 1, -5, "muzzle", box(-1.5F, -1, -1.5F, 3, 2, 2));
		b.part("jaw", "head", Role.JAW, 0, 2, -5, "muzzle", box(-1, 0, -1.5F, 2, 1, 2));
		PlanKit.ears(b, "head", 1.5F, -2, -2, 2, 2, 1, "fur", "glow");
		b.part("collar", "head", Role.EXTRA, 0, 2, -1, "glow", box(-3, 0, -1, 6, 1, 3));
		b.part("bell", "collar", Role.EXTRA, 0, 1, -1.5F, "bell", box(-1, 0, -1, 2, 2, 2));
		PlanKit.jointedLegs(b, q, "fur", "fur");
		String tail = PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -1.5F, 5.5F, 5, 2, 3, 1.0F, 1, 0.0F, "fur");
		PlanKit.bendChain(b, "tail", 5, -0.7F, 0.35F);
		b.part("tail_tip", tail, Role.EXTRA, 0, 0, 3, "glow", box(-1, -1, 0, 2, 2, 2));
		return b.build();
	}

	private static BodyPlan crystalSlime() {
		Builder b = BodyPlan.builder("pet_crystal_slime", Rig.FLOATER).scale(0.55F).shaded()
			.skin("gel", Skin.glowing(0x6AD8E8, 0xE0FFFF, CRYSTAL))
			.skin("face", Skin.of(0x4AB8C8, 0x2A8898, SMOOTH))
			.skin("mouth", Skin.of(0x1A3A50, 0x1A3A50, SMOOTH))
			.skin("core", Skin.glowing(0xC060FF, 0xFFE0FF, CRYSTAL));
		b.part("body", null, Role.BODY, 0, 21, 0, "gel",
			box(-6, -9, -6, 12, 9, 12),
			box(-5, -11, -5, 10, 2, 10),
			box(-6.5F, -2, -6.5F, 13, 2, 13));
		Part head = b.part("head", "body", Role.HEAD, 0, -5, -6, "face", box(-3.5F, -2.5F, -0.5F, 7, 4, 1));
		head.eyes = 2;
		head.eyeColor = 0x1A1A40;
		b.part("mouth", "head", Role.JAW, 0, 1, -0.6F, "mouth", box(-1, 0, 0, 2, 1, 1));
		Part crown = b.part("crown", "body", Role.EXTRA, 0, -11, 0, "core", box(-1, -4, -1, 2, 4, 2));
		crown.yRot = 0.4F;
		b.part("shard_r", "body", Role.EXTRA, -2.5F, -10.5F, 1.5F, "core", box(-1, -3, -1, 2, 3, 2)).zRot = -0.45F;
		b.part("shard_l", "body", Role.EXTRA, 2.5F, -10.5F, -1, "core", box(-1, -3, -1, 2, 3, 2)).zRot = 0.5F;
		b.part("shard_side", "body", Role.EXTRA, 6, -4, 2, "core", box(0, -1, -1, 2, 2, 2)).zRot = -0.3F;
		Part ring = b.part("ring", "body", Role.ORBIT, 0, -4, 0, "gel");
		for (int i = 0; i < 3; i++) {
			double a = Math.PI * 2 * i / 3;
			b.part("drop" + i, "ring", Role.EXTRA, (float)Math.cos(a) * 9, 0, (float)Math.sin(a) * 9, "gel", box(-1, -1, -1, 2, 2, 2));
		}
		ring.zRot = 0.15F;
		return b.build();
	}

	private static BodyPlan babyDragon() {
		Builder b = BodyPlan.builder("pet_baby_dragon", Rig.FLYER).scale(0.5F).shaded().folds()
			.skin("scale", Skin.of(0xC0262D, 0xFF7A3A, SCALES))
			.skin("belly", Skin.of(0xF0C878, 0xC09A50, SCALES))
			.skin("wing", Skin.glowing(0x8A1A20, 0xFF8040, FLAME))
			.skin("horn", Skin.of(0xE8E0C8, 0x8A8070, BONE));
		b.part("body", null, Role.BODY, 0, 14, 0, "scale", box(-3.5F, -3.5F, -6, 7, 7, 12));
		b.part("belly", "body", Role.EXTRA, 0, 3, 0, "belly", box(-2.5F, 0, -5, 5, 1, 10));
		b.part("spines", "body", Role.EXTRA, 0, -3.5F, 0, "horn",
			box(-0.5F, -1.5F, -4, 1, 2, 1), box(-0.5F, -2, -1, 1, 2, 1), box(-0.5F, -1.5F, 2, 1, 2, 1), box(-0.5F, -1, 4.5F, 1, 1, 1));
		String neck = PlanKit.chain(b, "neck", "body", Role.NECK, 0, 0, -1.5F, -5, 2, 3, 3, 0.9F, -1, -0.35F, "scale");
		Part head = b.part("head", neck, Role.HEAD, 0, 0, -3, "scale", box(-2.5F, -2.5F, -4, 5, 4, 4));
		head.eyes = 2;
		head.eyeColor = 0xFFE040;
		head.xRot = 0.6F;
		b.part("snout", "head", Role.EXTRA, 0, 1, -4, "scale", box(-1.5F, -1, -3, 3, 2, 3));
		b.part("jaw", "head", Role.JAW, 0, 2, -3, "scale", box(-1.5F, 0, -3.5F, 3, 1, 4));
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			Part horn = b.part("horn_" + s, "head", Role.EXTRA, side * 1.5F, -2.5F, -1, "horn", box(-0.5F, -3, -0.5F, 1, 3, 1));
			horn.xRot = -0.8F;
			horn.zRot = side * 0.2F;
			b.part("horn_tip_" + s, "horn_" + s, Role.EXTRA, 0, -3, 0, "horn", box(-0.5F, -2, -0.5F, 1, 2, 1)).xRot = -0.5F;
			Role role = side < 0 ? Role.WING_RIGHT : Role.WING_LEFT;
			b.part("frill_" + s, "head", Role.EAR, side < 0 ? 0 : 1, side * 2.5F, -1, 0, "wing", box(side < 0 ? -3 : 0, -1.5F, 0, 3, 3, 1)).zRot = side * 0.3F;
			b.part("wing_" + s, "body", role, 0, side * 3.5F, -3, -2, "wing", box(side < 0 ? -7 : 0, -0.5F, -1, 7, 1, 7));
			b.part("wing_bone_" + s, "wing_" + s, Role.EXTRA, 0, 0, 0, "horn", box(side < 0 ? -7 : 0, -1, -1.5F, 7, 1, 1));
			b.part("wingtip_" + s, "wing_" + s, role, 1, side * 7, 0, 0, "wing", box(side < 0 ? -6 : 0, -0.5F, -1, 6, 1, 5));
			b.part("arm_" + s, "body", Role.EXTRA, side * 2, 2.5F, -4, "scale", box(-0.5F, 0, -0.5F, 1, 3, 1));
			b.part("thigh_" + s, "body", Role.EXTRA, side * 2.5F, 2, 3, "scale", box(-1, 0, -1.5F, 2, 3, 3));
			b.part("foot_" + s, "thigh_" + s, Role.EXTRA, 0, 3, 0, "horn", box(-1, 0, -2.5F, 2, 1, 3));
		}
		String tail = PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -1, 6, 5, 3, 3, 0.8F, 1, 0.1F, "scale");
		b.part("tail_spade", tail, Role.EXTRA, 0, 0, 2, "wing", box(-2, -0.5F, 0, 4, 1, 3));
		return b.build();
	}

	// ------------------------------------------------------------------ mounts

	/** A neck rising from the front of the body, tilted forward like a horse's, with the head at its top. */
	private static void longHead(final Builder b, final Quad q, final String skin, final BodyPlan.Box neck, final BodyPlan.Box head) {
		float top = q.bodyY() - q.bodyH() / 2.0F;
		Part n = b.part("neck", null, Role.EXTRA, 0, top + 3, -q.bodyL() / 2.0F + 2, skin, neck);
		n.xRot = 0.7F;
		// the face hangs down from the poll instead of following the neck straight on
		b.part("head", "neck", Role.HEAD, 0, neck.y() + 2, 0, skin, head).xRot = 0.35F;
	}

	private static BodyPlan swiftStallion() {
		Builder b = BodyPlan.builder("mount_swift_stallion", Rig.QUADRUPED).scale(1.0F).shaded()
			.skin("coat", Skin.of(0xF6F4F0, 0xBEB8AE, COAT))
			.skin("mane", Skin.glowing(0x9FD0FF, 0xE8FFFF, FUR))
			.skin("muzzle", Skin.of(0x9A9890, 0x7A7870, SMOOTH))
			.skin("hoof", Skin.of(0x5A6070, 0x8A92A0, METAL))
			.skin("eye", Skin.of(0x1A1A2A, 0x3A3A5A, SMOOTH))
			.skin("leather", Skin.of(0x6A3A1E, 0xC8A050, CLOTH))
			.skin("cloth", Skin.of(0x2A4A8A, 0xE8D8A0, CLOTH))
			.skin("metal", Skin.of(0xC8C8D0, 0xFFFFFF, METAL));
		Quad q = new Quad(10, 10, 22, 6, 6, 10, 4, 12);
		PlanKit.torso(b, q, "coat");
		longHead(b, q, "coat", box(-3, -13, -3, 6, 14, 6), box(-3, -4, -5, 6, 5, 7));
		b.part("muzzle", "head", Role.EXTRA, 0, -1, -5, "muzzle", box(-2.5F, -1.5F, -5, 5, 4, 5));
		b.part("bridle", "muzzle", Role.EXTRA, 0, 0, -2, "leather", box(-2.5F, -1.5F, -0.5F, 5, 4, 1, 0.25F));
		PlanKit.sideEyes(b, "head", 3, -2.5F, -3, "eye");
		PlanKit.ears(b, "head", 2, -4, 0, 2, 3, 1, "coat", "");
		b.part("forelock", "head", Role.EXTRA, 0, -4, -1, "mane", box(-1, -1, -3, 2, 2, 3));
		b.part("mane", "neck", Role.EXTRA, 0, 0, 0, "mane", box(-1, -14, 2.5F, 2, 14, 3), box(-0.5F, -12, 5, 1, 9, 2));
		PlanKit.jointedLegs(b, q, "coat", "coat");
		for (int i = 0; i < 4; i++) {
			b.part("fetlock" + i, "shin" + i, Role.EXTRA, 0, 3, 0, "mane", box(-2, 0, -2, 4, 1, 4));
			b.part("hoof" + i, "shin" + i, Role.EXTRA, 0, 4, 0, "hoof", box(-2.5F, 0, -2.5F, 5, 2, 5));
		}
		PlanKit.saddle(b, "body", -5, -1, 10, "leather", "cloth", "metal");
		PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -3.5F, 11, 3, 3, 5, 0.85F, 1, 0.0F, "mane");
		PlanKit.bendChain(b, "tail", 3, -0.9F, -0.3F);
		return b.build();
	}

	private static BodyPlan duneRaptor() {
		Builder b = BodyPlan.builder("mount_dune_raptor", Rig.BIPED).scale(1.0F).shaded()
			.skin("scale", Skin.of(0xC8A050, 0x8A6A2A, SCALES))
			.skin("stripe", Skin.of(0x8A3A1A, 0x5A2A10, SCALES))
			.skin("crest", Skin.glowing(0xE86A1A, 0xFFD070, FEATHER))
			.skin("claw", Skin.of(0xE8E0C8, 0x8A8070, BONE))
			.skin("leather", Skin.of(0x5A3A20, 0xC8A050, CLOTH))
			.skin("cloth", Skin.of(0x8A2A1A, 0xE8C060, CLOTH))
			.skin("metal", Skin.of(0xB89A50, 0xFFF0A0, METAL));
		Part body = b.part("body", null, Role.BODY, 0, 4, 0, "scale", box(-5, -5, -10, 10, 10, 20));
		body.xRot = 0.05F;
		b.part("stripes", "body", Role.EXTRA, 0, -5, 0, "stripe", box(-4, -1, -8, 8, 1, 6), box(-4, -1, 4, 8, 1, 4));
		String neck = PlanKit.chain(b, "neck", "body", Role.NECK, 0, 0, -2, -9, 2, 5, 4, 0.9F, -1, -0.5F, "scale");
		Part head = b.part("head", neck, Role.HEAD, 0, 0, -3, "scale", box(-3, -3.5F, -6, 6, 5, 7));
		// the neck rises 1 rad; the head looks ahead again
		head.xRot = 0.95F;
		head.eyes = 2;
		head.eyeColor = 0xFFD040;
		b.part("snout", "head", Role.EXTRA, 0, 0, -6, "scale", box(-2.5F, -1, -5, 5, 2, 5));
		Part jaw = b.part("jaw", "head", Role.JAW, 0, 1, -5, "scale", box(-2, 0, -6, 4, 1, 6));
		b.part("teeth", jaw.name, Role.EXTRA, 0, 0, 0, "claw", box(-2, -1, -5.5F, 4, 1, 1));
		b.part("crest", "head", Role.EXTRA, 0, -3.5F, -2, "crest", box(-0.5F, -3, 0, 1, 3, 6), box(-1.5F, -2, 3, 1, 2, 4), box(0.5F, -2, 3, 1, 2, 4)).xRot = 0.4F;
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			Role leg = side < 0 ? Role.LEG_RIGHT : Role.LEG_LEFT;
			// a bird's leg: thigh forward, shin back, foot flat again
			b.part("leg_" + s, null, leg, side * 3.5F, 8, 2, "scale", box(-2.5F, 0, -3, 5, 7, 6)).xRot = -0.35F;
			b.part("shin_" + s, "leg_" + s, Role.SHIN, side < 0 ? 10 : 11, 0, 7, 0, "stripe", box(-1.5F, 0, -1.5F, 3, 8, 3)).xRot = 0.7F;
			Part foot = b.part("foot_" + s, "shin_" + s, Role.EXTRA, 0, 8, 0, "claw", box(-2, 0, -3, 4, 2, 4));
			foot.xRot = -0.35F;
			b.part("sickle_" + s, foot.name, Role.EXTRA, side * 1.5F, 0, -2, "claw", box(-0.5F, -2, -2, 1, 2, 2)).xRot = 0.5F;
			b.part("arm_" + s, null, side < 0 ? Role.ARM_RIGHT : Role.ARM_LEFT, side * 4, 2, -8, "scale", box(-1, 0, -1, 2, 5, 2));
			b.part("hand_" + s, "arm_" + s, Role.EXTRA, 0, 5, 0, "claw", box(-1, 0, -2, 2, 1, 2));
		}
		PlanKit.saddle(b, "body", -5, -1, 10, "leather", "cloth", "metal");
		String tail = PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -1, 10, 6, 4, 4, 0.82F, 1, 0.05F, "scale");
		b.part("tail_fan", tail, Role.EXTRA, 0, 0, 1, "crest", box(-2, -0.5F, 0, 4, 1, 4));
		return b.build();
	}

	private static BodyPlan frostWolf() {
		Builder b = BodyPlan.builder("mount_frost_wolf", Rig.QUADRUPED).scale(1.1F).shaded()
			.skin("fur", Skin.of(0xF2F6FA, 0x8CA6C2, COAT))
			.skin("dark", Skin.of(0x8AA8C8, 0x5A7898, FUR))
			.skin("nose", Skin.of(0x1A2230, 0x3A4A60, SMOOTH))
			.skin("ice", Skin.glowing(0x9FD0FF, 0xE8FFFF, CRYSTAL))
			.skin("leather", Skin.of(0x3A3A4A, 0x9FD0FF, CLOTH))
			.skin("cloth", Skin.of(0x2A4A7A, 0xC0E0FF, CLOTH))
			.skin("metal", Skin.of(0xC8D8E8, 0xFFFFFF, METAL));
		Quad q = new Quad(10, 9, 20, 8, 7, 6, 4, 11);
		PlanKit.torso(b, q, "fur");
		PlanKit.head(b, q, "fur", 0x6FD0FF);
		b.part("snout", "head", Role.EXTRA, 0, 1, -6, "fur", box(-2.5F, -2, -5, 5, 3, 5));
		b.part("nose", "snout", Role.EXTRA, 0, -2, -5, "nose", box(-1, 0, -0.5F, 2, 1, 1));
		b.part("jaw", "head", Role.JAW, 0, 2, -6, "dark", box(-2, 0, -4.5F, 4, 1, 5));
		PlanKit.ears(b, "head", 2.5F, -3.5F, -2, 3, 4, 2, "fur", "dark");
		b.part("ruff", "body", Role.EXTRA, 0, -1, -7, "fur", box(-6, -6, -3, 12, 11, 7), box(-4, 5, -2, 8, 2, 4));
		for (int i = 0; i < 2; i++) {
			Part spike = b.part("spike" + i, "body", Role.EXTRA, 0, -4.5F, 6 + i * 2.5F, "ice", box(-1, -4 + i, -1, 2, 4 - i, 2));
			spike.xRot = -0.4F;
		}
		for (int side = -1; side <= 1; side += 2) {
			b.part("shoulder_ice_" + (side < 0 ? "r" : "l"), "ruff", Role.EXTRA, side * 5, -5, 0, "ice", box(-1, -4, -1, 2, 4, 2)).zRot = side * 0.6F;
		}
		PlanKit.jointedLegs(b, q, "fur", "dark");
		for (int i = 0; i < 4; i++) {
			b.part("paw" + i, "shin" + i, Role.EXTRA, 0, 3, 0, "dark", box(-2, 0, -2.5F, 4, 2, 4));
		}
		PlanKit.saddle(b, "body", -4.5F, 0, 10, "leather", "cloth", "metal");
		PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -2, 10, 3, 4, 5, 0.9F, 1, 0.0F, "fur");
		PlanKit.bendChain(b, "tail", 3, -0.6F, 0.2F);
		return b.build();
	}

	private static BodyPlan emberLion() {
		Builder b = BodyPlan.builder("mount_ember_lion", Rig.QUADRUPED).scale(1.1F).shaded()
			.skin("fur", Skin.of(0xE2AE5E, 0xB0742E, COAT))
			.skin("mane", Skin.glowing(0xE8501A, 0xFFF0A0, FLAME))
			.skin("muzzle", Skin.of(0xE8C890, 0xC8A070, FUR))
			.skin("nose", Skin.of(0x3A1A14, 0x5A2A20, SMOOTH))
			.skin("paw", Skin.of(0x8A5A2A, 0x5A3A1A, FUR))
			.skin("leather", Skin.of(0x4A2414, 0xE8C050, CLOTH))
			.skin("cloth", Skin.of(0x8A1A1A, 0xE8C050, CLOTH))
			.skin("metal", Skin.of(0xD8B040, 0xFFF0A0, METAL));
		Quad q = new Quad(10, 10, 20, 8, 8, 5, 4, 11);
		PlanKit.torso(b, q, "fur");
		PlanKit.head(b, q, "fur", 0xFFE040);
		Part muzzle = b.part("muzzle", "head", Role.EXTRA, 0, 2, -5, "muzzle", box(-2.5F, -2, -3, 5, 4, 3));
		b.part("nose", muzzle.name, Role.EXTRA, 0, -2, -3, "nose", box(-1, 0, -0.5F, 2, 1, 1));
		b.part("jaw", "head", Role.JAW, 0, 3.5F, -5, "muzzle", box(-2, 0, -2.5F, 4, 1, 3));
		b.part("mane", "head", Role.EXTRA, 0, 0, -2, "mane", box(-7, -7, -1, 14, 14, 5), box(-5, -9, 1, 10, 3, 4), box(-4, 6, 0, 8, 3, 4));
		b.part("back_mane", "body", Role.EXTRA, 0, -5, -6, "mane", box(-4, -2, -4, 8, 3, 8));
		PlanKit.jointedLegs(b, q, "fur", "paw");
		for (int i = 0; i < 4; i++) {
			b.part("paw" + i, "shin" + i, Role.EXTRA, 0, 3, 0, "paw", box(-2, 0, -2.5F, 4, 2, 4));
		}
		PlanKit.saddle(b, "body", -5, 1, 10, "leather", "cloth", "metal");
		String tail = PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -2, 10, 4, 2, 4, 0.95F, 1, 0.0F, "fur");
		PlanKit.bendChain(b, "tail", 4, -0.7F, 0.25F);
		b.part("tail_tuft", tail, Role.EXTRA, 0, 0, 3, "mane", box(-1.5F, -1.5F, 0, 3, 3, 3));
		return b.build();
	}

	private static BodyPlan crystalStag() {
		Builder b = BodyPlan.builder("mount_crystal_stag", Rig.QUADRUPED).scale(1.1F).shaded()
			.skin("coat", Skin.of(0xCAB6E4, 0x806AA8, COAT))
			.skin("tuft", Skin.of(0xF4EEFF, 0xD8C8F0, FUR))
			.skin("muzzle", Skin.of(0x6A5A8A, 0x4A3A6A, SMOOTH))
			.skin("crystal", Skin.glowing(0xC080FF, 0xF4E0FF, CRYSTAL))
			.skin("hoof", Skin.glowing(0x3A2A4A, 0xC080FF, CRYSTAL))
			.skin("leather", Skin.of(0x3A2A4A, 0xD8D8F0, CLOTH))
			.skin("cloth", Skin.of(0x5A3A8A, 0xE0E0FF, CLOTH))
			.skin("metal", Skin.of(0xD0D0E0, 0xFFFFFF, METAL));
		Quad q = new Quad(9, 9, 20, 6, 6, 9, 3, 14);
		PlanKit.torso(b, q, "coat");
		b.part("chest_tuft", "body", Role.EXTRA, 0, 0, -10, "tuft", box(-3, -3, -1, 6, 6, 2));
		b.part("scut", "body", Role.EXTRA, 0, -3, 10, "tuft", box(-1.5F, 0, 0, 3, 4, 2)).xRot = 0.3F;
		longHead(b, q, "coat", box(-2.5F, -11, -2.5F, 5, 12, 5), box(-3, -4, -4, 6, 5, 6));
		b.part("muzzle", "head", Role.EXTRA, 0, -1, -4, "muzzle", box(-2, -1.5F, -4, 4, 3, 4));
		PlanKit.sideEyes(b, "head", 3, -2.5F, -2, "crystal");
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			b.part("ear_" + s, "head", Role.EAR, side < 0 ? 0 : 1, side * 3, -3, 0, "coat", box(side < 0 ? -3 : 0, -1, -0.5F, 3, 2, 1)).zRot = side * -0.3F;
			Part beam = b.part("antler_" + s, "head", Role.EXTRA, side * 1.5F, -4, -1, "crystal", box(-0.5F, -4, -0.5F, 1, 4, 1));
			beam.zRot = side * 0.5F;
			beam.xRot = -0.3F;
			Part upper = b.part("antler_top_" + s, beam.name, Role.EXTRA, 0, -4, 0, "crystal", box(-0.5F, -5, -0.5F, 1, 5, 1));
			upper.zRot = side * -0.35F;
			b.part("brow_tine_" + s, beam.name, Role.EXTRA, 0, -1.5F, 0, "crystal", box(-0.5F, -3, -0.5F, 1, 3, 1)).xRot = 0.9F;
			b.part("mid_tine_" + s, upper.name, Role.EXTRA, 0, -2, 0, "crystal", box(-0.5F, -3, -0.5F, 1, 3, 1)).zRot = side * 0.7F;
			b.part("fork_front_" + s, upper.name, Role.EXTRA, 0, -5, 0, "crystal", box(-0.5F, -2, -0.5F, 1, 2, 1)).xRot = 0.5F;
			b.part("fork_back_" + s, upper.name, Role.EXTRA, 0, -5, 0, "crystal", box(-0.5F, -3, -0.5F, 1, 3, 1)).xRot = -0.5F;
		}
		for (int i = 0; i < 3; i++) {
			Part shard = b.part("back_crystal" + i, "body", Role.EXTRA, (i - 1) * 2, -4.5F, 6.5F + (i % 2), "crystal", box(-1, -3 + (i % 2), -1, 2, 3 - (i % 2), 2));
			shard.xRot = -0.3F;
			shard.zRot = (i - 1) * 0.3F;
		}
		PlanKit.jointedLegs(b, q, "coat", "coat");
		for (int i = 0; i < 4; i++) {
			b.part("hoof" + i, "shin" + i, Role.EXTRA, 0, 5, 0, "hoof", box(-2, 0, -2, 4, 2, 4));
		}
		PlanKit.saddle(b, "body", -4.5F, -1, 9, "leather", "cloth", "metal");
		return b.build();
	}

	private static BodyPlan stormGriffin() {
		Builder b = BodyPlan.builder("mount_storm_griffin", Rig.FLYER).scale(1.0F).shaded().folds()
			.skin("feather", Skin.of(0xE8E0D0, 0xA89A80, FEATHER))
			.skin("fur", Skin.of(0xDAAE66, 0x94682C, COAT))
			.skin("storm", Skin.glowing(0x4A6ACF, 0xE0F0FF, FEATHER))
			.skin("beak", Skin.of(0xE8C45A, 0x8A6A1A, SMOOTH))
			.skin("talon", Skin.of(0x2A2A2A, 0x5A5A5A, SMOOTH))
			.skin("leather", Skin.of(0x3A2A1A, 0x8FB0FF, CLOTH))
			.skin("cloth", Skin.of(0x2A3A7A, 0xE0F0FF, CLOTH))
			.skin("metal", Skin.of(0xC8C8D0, 0xFFFFFF, METAL));
		b.part("body", null, Role.BODY, 0, 6, 0, "fur", box(-6, -6, -11, 12, 12, 22));
		b.part("chest", "body", Role.EXTRA, 0, -1, -9, "feather", box(-6.5F, -6, -4, 13, 12, 7), box(-5, 6, -3, 10, 2, 5));
		Part head = b.part("head", "body", Role.HEAD, 0, -10, -13, "feather", box(-3.5F, -4, -6, 7, 7, 7));
		head.eyes = 2;
		head.eyeColor = 0xFFE040;
		b.part("beak", "head", Role.EXTRA, 0, 0.5F, -6, "beak", box(-1.5F, -1, -3, 3, 2, 3), box(-1, 1, -4, 2, 1, 2));
		b.part("lower_beak", "head", Role.JAW, 0, 2, -6, "beak", box(-1, 0, -2.5F, 2, 1, 3));
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			b.part("tuft_" + s, "head", Role.EAR, side < 0 ? 0 : 1, side * 2.5F, -4, -1, "storm", box(-1, -1, 0, 2, 2, 4)).xRot = 0.4F;
			Role wing = side < 0 ? Role.WING_RIGHT : Role.WING_LEFT;
			b.part("wing_" + s, "body", wing, 0, side * 6, -5, -6, "feather", box(side < 0 ? -14 : 0, -0.5F, -1, 14, 1, 12));
			b.part("wingtip_" + s, "wing_" + s, wing, 1, side * 14, 0, 0, "storm", box(side < 0 ? -12 : 0, -0.5F, -1, 12, 1, 10));
			b.part("primaries_" + s, "wingtip_" + s, Role.EXTRA, side * 12, 0, 0, "storm",
				box(side < 0 ? -4 : 0, -0.5F, 0, 4, 1, 2), box(side < 0 ? -5 : 0, -0.5F, 3, 5, 1, 2), box(side < 0 ? -4 : 0, -0.5F, 6, 4, 1, 2));
			int front = side < 0 ? 1 : 0;
			int back = side < 0 ? 3 : 2;
			b.part("leg" + front, "body", Role.LEG_QUAD, front, side * 4, 5, -8, "feather", box(-2, 0, -2, 4, 7, 4));
			b.part("shin" + front, "leg" + front, Role.SHIN, front, 0, 7, 0, "beak", box(-1, 0, -1, 2, 5, 2));
			b.part("talons" + front, "shin" + front, Role.EXTRA, 0, 5, 0, "talon", box(-1.5F, 0, -3, 3, 1, 4));
			b.part("leg" + back, "body", Role.LEG_QUAD, back, side * 4, 5, 8, "fur", box(-2, 0, -2, 4, 7, 4));
			b.part("shin" + back, "leg" + back, Role.SHIN, back, 0, 7, 0, "fur", box(-1.5F, 0, -1.5F, 3, 5, 3));
			b.part("paw" + back, "shin" + back, Role.EXTRA, 0, 5, 0, "fur", box(-2, 0, -2.5F, 4, 1, 4));
		}
		PlanKit.saddle(b, "body", -6, 2, 12, "leather", "cloth", "metal");
		String tail = PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -3, 11, 4, 2, 4, 0.95F, 1, 0.0F, "fur");
		PlanKit.bendChain(b, "tail", 4, -0.5F, 0.2F);
		b.part("tail_tuft", tail, Role.EXTRA, 0, 0, 3, "storm", box(-1.5F, -1.5F, 0, 3, 3, 4));
		return b.build();
	}
}
