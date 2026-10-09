package com.minecraftmode.client.creature;

import static com.minecraftmode.client.creature.BodyPlan.box;
import static com.minecraftmode.client.creature.Skin.Pattern.*;

import com.minecraftmode.client.creature.BodyPlan.Builder;
import com.minecraftmode.client.creature.BodyPlan.Part;
import com.minecraftmode.client.creature.BodyPlan.Rig;
import com.minecraftmode.client.creature.BodyPlan.Role;
import com.minecraftmode.client.creature.PlanKit.Quad;
import java.util.List;

/** Body plans of the pets ({@code pet_<id>}) and mounts ({@code mount_<id>}); ids match their entity types. */
final class CompanionPlans {
	static List<BodyPlan> all() {
		return List.of(
			emberFox(), frostOwl(), goldenScarab(), mossTurtle(), sparkSprite(), shadowCat(), crystalSlime(), babyDragon(),
			swiftStallion(), duneRaptor(), frostWolf(), emberLion(), crystalStag(), stormGriffin()
		);
	}

	// ------------------------------------------------------------------ pets

	private static BodyPlan emberFox() {
		Builder b = BodyPlan.builder("pet_ember_fox", Rig.QUADRUPED).scale(0.55F)
			.skin("fur", Skin.of(0xE0702A, 0xF4A050, FUR))
			.skin("white", Skin.of(0xF4ECE0, 0xD8C8B0, FUR))
			.skin("ember", Skin.glowing(0xFF7A1A, 0xFFE070, FLAME));
		PlanKit.quadruped(b, new Quad(6, 6, 12, 7, 6, 6, 2, 6), "fur", "fur", "white", 2, 0x1A1A1A);
		b.part("snout", "head", Role.EXTRA, 0, 1, -6, "white", box(-1.5F, -1, -3, 3, 2, 3));
		b.part("ear_r", "head", Role.EXTRA, -2, -3, -3, "fur", box(-1, -3, -0.5F, 2, 3, 1));
		b.part("ear_l", "head", Role.EXTRA, 2, -3, -3, "fur", box(-1, -3, -0.5F, 2, 3, 1));
		String tail = PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -1, 6, 3, 3, 4, 0.9F, 1, 0.5F, "fur");
		b.part("tail_tip", tail, Role.EXTRA, 0, 0, 4, "ember", box(-2, -2, 0, 4, 4, 3));
		return b.build();
	}

	private static BodyPlan frostOwl() {
		Builder b = BodyPlan.builder("pet_frost_owl", Rig.FLYER).scale(0.5F)
			.skin("feather", Skin.of(0xE8F0F8, 0x9FB8D0, FEATHER))
			.skin("ice", Skin.glowing(0x9FD0FF, 0xE8FFFF, CRYSTAL))
			.skin("beak", Skin.of(0xE8C45A, 0x8A6A1A, SMOOTH));
		b.part("body", null, Role.BODY, 0, 12, 0, "feather", box(-4, -5, -3, 8, 10, 6));
		Part head = b.part("head", "body", Role.HEAD, 0, -5, 0, "feather", box(-4, -7, -4, 8, 7, 8));
		head.eyes = 2;
		head.eyeColor = 0x6FD0FF;
		b.part("beak", "head", Role.JAW, 0, -2, -4, "beak", box(-1, 0, -2, 2, 2, 2));
		b.part("tuft_r", "head", Role.EXTRA, -3, -7, -1, "ice", box(-1, -2, -1, 2, 2, 2));
		b.part("tuft_l", "head", Role.EXTRA, 3, -7, -1, "ice", box(-1, -2, -1, 2, 2, 2));
		for (int side = -1; side <= 1; side += 2) {
			Role role = side < 0 ? Role.WING_RIGHT : Role.WING_LEFT;
			b.part("wing_" + (side < 0 ? "r" : "l"), "body", role, side * 4, -3, 0, "feather", box(side < 0 ? -8 : 0, -1, -3, 8, 1, 7));
		}
		return b.build();
	}

	private static BodyPlan goldenScarab() {
		Builder b = BodyPlan.builder("pet_golden_scarab", Rig.QUADRUPED).scale(0.45F)
			.skin("shell", Skin.glowing(0xD9B44A, 0xFFF0A0, METAL))
			.skin("leg", Skin.of(0x5A4220, 0x2A1A10, CHITIN));
		PlanKit.quadruped(b, new Quad(8, 5, 10, 6, 4, 4, 1, 4), "shell", "leg", "leg", 2, 0x40FF80);
		b.part("dome", "body", Role.EXTRA, 0, -2.5F, 0, "shell", box(-4.5F, -2, -5, 9, 2, 10));
		b.part("horn", "head", Role.EXTRA, 0, -2, -4, "shell", box(-0.5F, -3, -2, 1, 3, 2)).xRot = -0.4F;
		return b.build();
	}

	private static BodyPlan mossTurtle() {
		Builder b = BodyPlan.builder("pet_moss_turtle", Rig.QUADRUPED).scale(0.5F)
			.skin("skin", Skin.of(0x6A9A5A, 0x4A7A3A, SCALES))
			.skin("shell", Skin.of(0x5A6A3A, 0x7AA850, BARK))
			.skin("moss", Skin.glowing(0x6AC850, 0xC0FF80, SPOTS));
		PlanKit.quadruped(b, new Quad(10, 4, 12, 5, 4, 5, 3, 3), "skin", "skin", "skin", 2, 0x1A1A1A);
		b.part("shell", "body", Role.EXTRA, 0, -3, 0, "shell", box(-6, -4, -7, 12, 5, 14));
		b.part("moss", "shell", Role.EXTRA, 0, -4, 0, "moss", box(-4, -1, -5, 8, 1, 10));
		return b.build();
	}

	private static BodyPlan sparkSprite() {
		Builder b = BodyPlan.builder("pet_spark_sprite", Rig.FLOATER).scale(0.45F)
			.skin("core", Skin.glowing(0x4A8CFF, 0xE0F0FF, CRYSTAL))
			.skin("wing", Skin.glowing(0x9FD0FF, 0xFFFFFF, VOID));
		b.part("body", null, Role.BODY, 0, 8, 0, "core", box(-4, -4, -4, 8, 8, 8));
		Part head = b.part("head", "body", Role.HEAD, 0, -4, -4, "core", box(-2, -2, -1, 4, 4, 1));
		head.eyes = 2;
		head.eyeColor = 0xFFFFFF;
		b.part("wing_r", "body", Role.WING_RIGHT, -4, -2, 1, "wing", box(-6, -4, 0, 6, 8, 1));
		b.part("wing_l", "body", Role.WING_LEFT, 4, -2, 1, "wing", box(0, -4, 0, 6, 8, 1));
		b.part("orbit", "body", Role.ORBIT, 0, 0, 0, "core");
		for (int i = 0; i < 3; i++) {
			double a = Math.PI * 2 * i / 3;
			b.part("spark" + i, "orbit", Role.EXTRA, (float)Math.cos(a) * 7, 0, (float)Math.sin(a) * 7, "wing", box(-0.5F, -0.5F, -0.5F, 1, 1, 1));
		}
		return b.build();
	}

	private static BodyPlan shadowCat() {
		Builder b = BodyPlan.builder("pet_shadow_cat", Rig.QUADRUPED).scale(0.5F)
			.skin("fur", Skin.of(0x1E1A2A, 0x3A3050, FUR))
			.skin("glow", Skin.glowing(0x8A4ACF, 0xE0B0FF, VOID));
		PlanKit.quadruped(b, new Quad(5, 6, 12, 6, 5, 5, 2, 6), "fur", "fur", "fur", 2, 0xC080FF);
		b.part("ear_r", "head", Role.EXTRA, -2, -2, -2, "fur", box(-1, -2, -0.5F, 2, 2, 1));
		b.part("ear_l", "head", Role.EXTRA, 2, -2, -2, "fur", box(-1, -2, -0.5F, 2, 2, 1));
		b.part("collar", "head", Role.EXTRA, 0, 2, -1, "glow", box(-3, 0, -1, 6, 1, 3));
		PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -2, 6, 3, 2, 4, 0.9F, 1, 0.9F, "fur");
		return b.build();
	}

	private static BodyPlan crystalSlime() {
		Builder b = BodyPlan.builder("pet_crystal_slime", Rig.FLOATER).scale(0.55F)
			.skin("gel", Skin.glowing(0x6AD8E8, 0xE0FFFF, CRYSTAL))
			.skin("core", Skin.glowing(0xC060FF, 0xFFE0FF, CRYSTAL));
		b.part("body", null, Role.BODY, 0, 16, 0, "gel", box(-6, -8, -6, 12, 10, 12));
		Part head = b.part("head", "body", Role.HEAD, 0, -4, -6, "gel", box(-3, -2, -1, 6, 3, 1));
		head.eyes = 2;
		head.eyeColor = 0x1A1A40;
		b.part("core", "body", Role.EXTRA, 0, -3, 0, "core", box(-2, -2, -2, 4, 4, 4));
		b.part("crown", "body", Role.EXTRA, 0, -8, 0, "core", box(-1, -3, -1, 2, 3, 2));
		return b.build();
	}

	private static BodyPlan babyDragon() {
		Builder b = BodyPlan.builder("pet_baby_dragon", Rig.FLYER).scale(0.5F)
			.skin("scale", Skin.of(0xC0262D, 0xFF7A3A, SCALES))
			.skin("belly", Skin.of(0xF0C878, 0xC09A50, SCALES))
			.skin("wing", Skin.glowing(0x8A1A20, 0xFF8040, FLAME))
			.skin("horn", Skin.of(0xE8E0C8, 0x8A8070, BONE));
		b.part("body", null, Role.BODY, 0, 12, 0, "scale", box(-4, -4, -6, 8, 8, 12));
		b.part("belly", "body", Role.EXTRA, 0, 3, 0, "belly", box(-3, 0, -5, 6, 2, 10));
		String neck = PlanKit.chain(b, "neck", "body", Role.NECK, 0, 0, -2, -6, 2, 3, 3, 0.9F, -1, -0.3F, "scale");
		Part head = b.part("head", neck, Role.HEAD, 0, 0, -3, "scale", box(-3, -3, -5, 6, 5, 6));
		head.eyes = 2;
		head.eyeColor = 0xFFE040;
		b.part("horn_r", "head", Role.EXTRA, -2, -3, 0, "horn", box(-0.5F, -3, -0.5F, 1, 3, 1)).xRot = 0.5F;
		b.part("horn_l", "head", Role.EXTRA, 2, -3, 0, "horn", box(-0.5F, -3, -0.5F, 1, 3, 1)).xRot = 0.5F;
		for (int side = -1; side <= 1; side += 2) {
			Role role = side < 0 ? Role.WING_RIGHT : Role.WING_LEFT;
			b.part("wing_" + (side < 0 ? "r" : "l"), "body", role, side * 4, -3, -1, "wing", box(side < 0 ? -10 : 0, -1, -4, 10, 1, 8));
			b.part("leg_" + (side < 0 ? "r" : "l"), "body", Role.EXTRA, side * 2.5F, 4, 2, "scale", box(-1, 0, -1, 2, 4, 2));
		}
		PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, 0, 6, 3, 2, 5, 0.85F, 1, 0.2F, "scale");
		return b.build();
	}

	// ------------------------------------------------------------------ mounts

	/** A quadruped's body and four legs without a head (for long-necked mounts). */
	private static void frame(final Builder b, final Quad q, final String bodySkin, final String legSkin) {
		float y = q.bodyY();
		b.part("body", null, Role.BODY, 0, y, 0, bodySkin, box(-q.bodyW() / 2.0F, -q.bodyH() / 2.0F, -q.bodyL() / 2.0F, q.bodyW(), q.bodyH(), q.bodyL()));
		float lx = q.bodyW() / 2.0F - q.legW() / 2.0F;
		float lz = q.bodyL() / 2.0F - q.legW() / 2.0F - 1;
		float[][] legs = {{lx, -lz}, {-lx, -lz}, {lx, lz}, {-lx, lz}};
		for (int i = 0; i < 4; i++) {
			b.part("leg" + i, null, Role.LEG_QUAD, i, legs[i][0], 24 - q.legL(), legs[i][1], legSkin,
				box(-q.legW() / 2.0F, 0, -q.legW() / 2.0F, q.legW(), q.legL(), q.legW()));
		}
	}

	/** A neck rising from the front of the body, tilted forward like a horse's, with the head at its top. */
	private static void longHead(final Builder b, final Quad q, final String skin, final BodyPlan.Box neck, final BodyPlan.Box head, final int eyeColor) {
		float top = q.bodyY() - q.bodyH() / 2.0F;
		Part n = b.part("neck", null, Role.EXTRA, 0, top + 3, -q.bodyL() / 2.0F + 2, skin, neck);
		n.xRot = 0.5F;
		Part h = b.part("head", "neck", Role.HEAD, 0, neck.y() + 2, 0, skin, head);
		h.eyes = 2;
		h.eyeColor = eyeColor;
	}

	private static BodyPlan swiftStallion() {
		Builder b = BodyPlan.builder("mount_swift_stallion", Rig.QUADRUPED).scale(1.0F)
			.skin("coat", Skin.of(0xF0EEE8, 0xC8C0B0, FUR))
			.skin("mane", Skin.glowing(0x9FD0FF, 0xE8FFFF, FUR))
			.skin("hoof", Skin.of(0x5A6070, 0x8A92A0, METAL));
		Quad q = new Quad(10, 10, 22, 6, 6, 10, 4, 12);
		frame(b, q, "coat", "coat");
		longHead(b, q, "coat", box(-3, -13, -3, 6, 14, 6), box(-3, -4, -9, 6, 6, 10), 0x3A3A3A);
		b.part("mane", "neck", Role.EXTRA, 0, 0, 0, "mane", box(-1, -14, 2, 2, 13, 3));
		b.part("ear_r", "head", Role.EXTRA, -2, -4, 0, "coat", box(-1, -3, -0.5F, 2, 3, 1));
		b.part("ear_l", "head", Role.EXTRA, 2, -4, 0, "coat", box(-1, -3, -0.5F, 2, 3, 1));
		for (int i = 0; i < 4; i++) {
			b.part("hoof" + i, "leg" + i, Role.EXTRA, 0, 10, 0, "hoof", box(-2.5F, 0, -2.5F, 5, 2, 5));
		}
		PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -3, 11, 2, 3, 6, 0.9F, 1, 0.6F, "mane");
		return b.build();
	}

	private static BodyPlan duneRaptor() {
		Builder b = BodyPlan.builder("mount_dune_raptor", Rig.BIPED).scale(1.0F)
			.skin("scale", Skin.of(0xC8A050, 0x8A6A2A, SCALES))
			.skin("stripe", Skin.of(0x8A3A1A, 0x5A2A10, SCALES))
			.skin("claw", Skin.of(0xE8E0C8, 0x8A8070, BONE));
		Part body = b.part("body", null, Role.BODY, 0, 4, 0, "scale", box(-5, -5, -10, 10, 10, 20));
		body.xRot = 0.05F;
		b.part("stripes", "body", Role.EXTRA, 0, -5, 0, "stripe", box(-4, -1, -8, 8, 1, 16));
		Part head = b.part("head", null, Role.HEAD, 0, -4, -11, "scale", box(-3.5F, -4, -10, 7, 7, 10));
		head.eyes = 2;
		head.eyeColor = 0xFFD040;
		b.part("jaw", "head", Role.JAW, 0, 2, -1, "scale", box(-3, 0, -9, 6, 2, 9));
		b.part("crest", "head", Role.EXTRA, 0, -4, -2, "stripe", box(-0.5F, -3, -4, 1, 3, 6));
		b.part("leg_r", null, Role.LEG_RIGHT, -3, 8, 2, "scale", box(-2, 0, -2, 4, 16, 4));
		b.part("leg_l", null, Role.LEG_LEFT, 3, 8, 2, "scale", box(-2, 0, -2, 4, 16, 4));
		b.part("claw_r", "leg_r", Role.EXTRA, 0, 15, -1, "claw", box(-2, 0, -3, 4, 1, 4));
		b.part("claw_l", "leg_l", Role.EXTRA, 0, 15, -1, "claw", box(-2, 0, -3, 4, 1, 4));
		b.part("arm_r", null, Role.ARM_RIGHT, -4, 2, -8, "scale", box(-1, 0, -1, 2, 6, 2));
		b.part("arm_l", null, Role.ARM_LEFT, 4, 2, -8, "scale", box(-1, 0, -1, 2, 6, 2));
		PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -1, 10, 3, 4, 7, 0.8F, 1, 0.1F, "scale");
		return b.build();
	}

	private static BodyPlan frostWolf() {
		Builder b = BodyPlan.builder("mount_frost_wolf", Rig.QUADRUPED).scale(1.1F)
			.skin("fur", Skin.of(0xE8F0F8, 0xA8C8E0, FUR))
			.skin("dark", Skin.of(0x8AA8C8, 0x5A7898, FUR))
			.skin("ice", Skin.glowing(0x9FD0FF, 0xE8FFFF, CRYSTAL));
		PlanKit.quadruped(b, new Quad(10, 9, 20, 9, 8, 8, 4, 11), "fur", "fur", "dark", 2, 0x6FD0FF);
		b.part("snout", "head", Role.EXTRA, 0, 1, -8, "fur", box(-2.5F, -2, -5, 5, 4, 5));
		b.part("ear_r", "head", Role.EXTRA, -3, -4, -3, "fur", box(-1, -3, -1, 2, 3, 2));
		b.part("ear_l", "head", Role.EXTRA, 3, -4, -3, "fur", box(-1, -3, -1, 2, 3, 2));
		b.part("mane", "body", Role.EXTRA, 0, -1, -7, "fur", box(-6, -6, -3, 12, 11, 7));
		b.part("spikes", "body", Role.EXTRA, 0, -4.5F, 2, "ice", box(-1, -3, -6, 2, 3, 12));
		PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -2, 10, 3, 4, 8, 0.85F, 1, 0.6F, "fur");
		return b.build();
	}

	private static BodyPlan emberLion() {
		Builder b = BodyPlan.builder("mount_ember_lion", Rig.QUADRUPED).scale(1.1F)
			.skin("fur", Skin.of(0xC88A3A, 0xE8B060, FUR))
			.skin("mane", Skin.glowing(0xE8501A, 0xFFF0A0, FLAME))
			.skin("paw", Skin.of(0x8A5A2A, 0x5A3A1A, FUR));
		PlanKit.quadruped(b, new Quad(10, 10, 20, 9, 9, 8, 4, 11), "fur", "fur", "paw", 2, 0xFFE040);
		b.part("snout", "head", Role.EXTRA, 0, 2, -8, "fur", box(-2.5F, -2, -3, 5, 4, 3));
		b.part("mane", "head", Role.EXTRA, 0, 0, -3, "mane", box(-7, -7, -2, 14, 14, 6));
		PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -2, 10, 2, 2, 8, 0.9F, 1, 0.7F, "fur");
		return b.build();
	}

	private static BodyPlan crystalStag() {
		Builder b = BodyPlan.builder("mount_crystal_stag", Rig.QUADRUPED).scale(1.1F)
			.skin("coat", Skin.of(0xB8A0D8, 0x8A70B0, FUR))
			.skin("crystal", Skin.glowing(0xC080FF, 0xF4E0FF, CRYSTAL))
			.skin("hoof", Skin.of(0x3A2A4A, 0x5A4A6A, SMOOTH));
		Quad q = new Quad(9, 9, 20, 6, 6, 9, 3, 14);
		frame(b, q, "coat", "coat");
		longHead(b, q, "coat", box(-2.5F, -11, -2.5F, 5, 12, 5), box(-3, -4, -8, 6, 6, 9), 0xE0B0FF);
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			Part antler = b.part("antler_" + s, "head", Role.EXTRA, side * 2, -3, -2, "crystal", box(-0.5F, -8, -0.5F, 1, 8, 1));
			antler.zRot = side * -0.4F;
			b.part("tine_" + s, "antler_" + s, Role.EXTRA, 0, -5, 0, "crystal", box(side < 0 ? -4 : 0, -0.5F, -0.5F, 4, 1, 1));
		}
		for (int i = 0; i < 4; i++) {
			b.part("hoof" + i, "leg" + i, Role.EXTRA, 0, 12, 0, "hoof", box(-2, 0, -2, 4, 2, 4));
		}
		return b.build();
	}

	private static BodyPlan stormGriffin() {
		Builder b = BodyPlan.builder("mount_storm_griffin", Rig.FLYER).scale(1.0F)
			.skin("feather", Skin.of(0xE8E0D0, 0xA89A80, FEATHER))
			.skin("fur", Skin.of(0xC89A50, 0x8A6A2A, FUR))
			.skin("storm", Skin.glowing(0x4A6ACF, 0xE0F0FF, FEATHER))
			.skin("beak", Skin.of(0xE8C45A, 0x8A6A1A, SMOOTH));
		b.part("body", null, Role.BODY, 0, 6, 0, "fur", box(-6, -6, -11, 12, 12, 22));
		b.part("chest", "body", Role.EXTRA, 0, -1, -9, "feather", box(-6.5F, -6, -4, 13, 12, 7));
		Part head = b.part("head", null, Role.HEAD, 0, -4, -13, "feather", box(-4, -5, -8, 8, 8, 8));
		head.eyes = 2;
		head.eyeColor = 0xFFE040;
		b.part("beak", "head", Role.JAW, 0, -1, -8, "beak", box(-1.5F, -1, -4, 3, 3, 4));
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			Role role = side < 0 ? Role.WING_RIGHT : Role.WING_LEFT;
			b.part("wing_" + s, "body", role, side * 6, -5, -4, "storm", box(side < 0 ? -18 : 0, -1, -6, 18, 1, 13));
			b.part("wingtip_" + s, "wing_" + s, role, side * 18, 0, 0, "storm", box(side < 0 ? -12 : 0, -1, -5, 12, 1, 10));
			b.part("leg_f" + s, "body", Role.EXTRA, side * 4, 5, -8, "beak", box(-1.5F, 0, -1.5F, 3, 13, 3));
			b.part("leg_b" + s, "body", Role.EXTRA, side * 4, 5, 8, "fur", box(-2, 0, -2, 4, 13, 4));
		}
		PlanKit.chain(b, "tail", "body", Role.TAIL, 0, 0, -3, 11, 2, 2, 8, 0.9F, 1, 0.5F, "fur");
		return b.build();
	}
}
