package com.minecraftmode.client.creature;

import static com.minecraftmode.client.creature.BodyPlan.box;
import static com.minecraftmode.client.creature.Skin.Pattern.*;

import com.minecraftmode.client.creature.BodyPlan.Box;
import com.minecraftmode.client.creature.BodyPlan.Builder;
import com.minecraftmode.client.creature.BodyPlan.Mark;
import com.minecraftmode.client.creature.BodyPlan.Part;
import com.minecraftmode.client.creature.BodyPlan.Rig;
import com.minecraftmode.client.creature.BodyPlan.Role;
import net.minecraft.util.Mth;

/**
 * Sukuna's raid: Ryomen Sukuna in his true form (four arms, the second face as a bone mask over his right eye, the mouth in his
 * belly, black bands and marks) and his shikigami Mahoraga (the wheel of adaptation, wings on its head, the Sword of
 * Extermination grown from its right wrist). Mark coordinates are in each part's own units; pixel centres fall on whole units
 * for boxes that start on a half unit and on half units for boxes that start on a whole one.
 */
final class SukunaPlans {
	/** Sukuna's tattoos. */
	private static final int INK = 0x1E1418;
	/** Muscle lines on his skin. */
	private static final int CREASE = 0xC08676;
	private static final int TEETH = 0xEEE6D4;
	private static final int EYE_RED = 0xE0202E;

	/** Ryomen Sukuna: a tall four-armed curse in a navy hakama with a white sash, holding a three-pronged spear. */
	static BodyPlan sukuna() {
		Builder b = BodyPlan.builder("sukuna", Rig.BIPED).scale(1.1F).shaded()
			.skin("skin", Skin.of(0xF0C0AE, 0xF8D8CA, SMOOTH))
			.skin("hair", Skin.of(0xEE8A88, 0xFAB8B2, FUR))
			.skin("bone", Skin.of(0xF4EEE2, 0xB8AE9A, BONE))
			.skin("hakama", Skin.of(0x2C3358, 0x1A1E38, CLOTH))
			.skin("obi", Skin.of(0xE6E2D8, 0xB8B2A6, CLOTH))
			.skin("shaft", Skin.of(0x2A2024, 0x4A3A40, METAL))
			.skin("steel", Skin.of(0xC4C8D0, 0xF0F4FA, METAL))
			.skin("brass", Skin.of(0xC0903A, 0xF0D080, METAL));

		// torso: a broad chest over a narrower belly, pecs standing out
		Part body = b.part("body", null, Role.BODY, 0, -18, 0, "skin",
			box(-7.5F, 0, -4, 15, 11, 8), box(-6, 11, -3.5F, 12, 9, 7), box(-6.5F, 1, -4.8F, 6, 5, 1), box(0.5F, 1, -4.8F, 6, 5, 1));
		chestMarks(body);
		bellyMouth(body);

		Part head = b.part("head", null, Role.HEAD, 0, -18, 0, "skin", box(-4.5F, -10, -4.5F, 9, 10, 9));
		head.eyes = 0;
		face(head);
		// the second face: a bone mask over the right half, with its own two eyes
		// layered plates: the brow, the cheek and a ridge over the nose, seams between them
		Part mask = b.part("mask", "head", Role.EXTRA, 0, 0, 0, "bone", box(-5, -9, -5.5F, 5, 8, 1), box(-5.5F, -9, -5, 1, 8, 5), box(-5.5F, -10, -5.8F, 4, 2, 1),
			box(-1, -8, -6, 1, 5, 1));
		mask.glowMark(Mark.FRONT, -4, -7, -2, -6, 0xFFF4EE).glowMark(Mark.FRONT, -3, -7, -2, -6, EYE_RED)
			.glowMark(Mark.FRONT, -4, -5, -2, -4, 0xFFF4EE).glowMark(Mark.FRONT, -3, -5, -2, -4, EYE_RED)
			.mark(Mark.FRONT, -5, -3, 0, -2, 0x9A9080).mark(Mark.FRONT, -5, -6, -4, -5, 0x9A9080).mark(Mark.SIDES, -9, -6, -5, -5, 0x9A9080)
			.mark(Mark.RIGHT, -5, -4, 0, -3, 0x9A9080);
		b.part("ear_r", "head", Role.EXTRA, -4.5F, -5, 0, "skin", box(-1, -1.5F, -1, 1, 3, 2));
		b.part("ear_l", "head", Role.EXTRA, 4.5F, -5, 0, "skin", box(0, -1.5F, -1, 1, 3, 2));
		hair(b);

		// upper arms from the shoulders, lower arms from under the chest; black bands on all four
		for (int i = 0; i < 2; i++) {
			int side = i == 0 ? -1 : 1;
			String s = i == 0 ? "r" : "l";
			Role role = i == 0 ? Role.ARM_RIGHT : Role.ARM_LEFT;
			Part upper = b.part("arm_" + s, null, role, 0, side * 10, -16, 0, "skin",
				box(-3, -2.5F, -3, 6, 6, 6), box(-2.5F, -2, -2.5F, 5, 17, 5), box(-3, 15, -3, 6, 4, 6));
			upper.zRot = -side * 0.12F;
			int outer = i == 0 ? Mark.RIGHT : Mark.LEFT;
			upper.mark(outer, -1, -0.5F, 1, 2.5F, INK).mark(outer, -2, 0.5F, 2, 1.5F, INK);
			upper.band(5, 7, INK).band(11, 12, INK).band(13, 14, INK);
			Part lower = b.part("arm_" + s + "1", null, role, 1, side * 9, -6, -1, "skin", box(-2.5F, -2, -2.5F, 5, 15, 5), box(-3, 13, -3, 6, 4, 6));
			lower.zRot = -side * 0.4F;
			lower.xRot = -0.45F;
			lower.band(3, 5, INK).band(9, 10, INK).band(11, 12, INK);
		}

		// white sash over a wide navy hakama, bare feet with black anklets
		Part obi = b.part("obi", "body", Role.EXTRA, 0, 19, 0, "obi", box(-7, 0, -4.5F, 14, 3, 9));
		b.part("obi_tails", obi.name, Role.EXTRA, 0, 0, 0, "obi", box(-4, 2, -5, 3, 9, 1), box(1.5F, 2, -5, 2, 7, 1));
		b.part("hakama", "body", Role.EXTRA, 0, 21, 0, "hakama", box(-7.5F, 0, -5, 15, 10, 10));
		for (int i = 0; i < 2; i++) {
			String s = i == 0 ? "r" : "l";
			b.part("leg_" + s, null, i == 0 ? Role.LEG_RIGHT : Role.LEG_LEFT, (i == 0 ? -1 : 1) * 3.5F, 2, 0, "hakama", box(-3.5F, 0, -3.5F, 7, 11, 7));
			b.part("shin_" + s, "leg_" + s, Role.SHIN, 10 + i, 0, 11, 0, "hakama", box(-4, 0, -4, 8, 9, 8));
			b.part("foot_" + s, "shin_" + s, Role.EXTRA, 0, 9, 0, "skin", box(-2.5F, 0, -4, 5, 2, 6)).band(0, 1, INK);
		}

		// the spear: a dark shaft, a steel blade between two curled brass prongs, a white cloth tied under the head
		Part spear = b.part("spear", "arm_r", Role.EXTRA, 0, 17, -0.5F, "shaft", box(-0.5F, -32, -0.5F, 1, 46, 1));
		Part blade = b.part("spear_head", spear.name, Role.EXTRA, 0, -32, 0, "steel", box(-1, -9, -0.5F, 2, 9, 1), box(-0.5F, -11, -0.5F, 1, 2, 1));
		b.part("spear_prongs", blade.name, Role.EXTRA, 0, 0, 0, "brass", box(-1, 0, -1, 2, 2, 2),
			box(-4, -1, -0.5F, 3, 1, 1), box(-4, -5, -0.5F, 1, 4, 1), box(-5, -6, -0.5F, 1, 1, 1),
			box(1, -1, -0.5F, 3, 1, 1), box(3, -5, -0.5F, 1, 4, 1), box(4, -6, -0.5F, 1, 1, 1));
		b.part("spear_cloth", spear.name, Role.EXTRA, 0, -28, 0, "obi", box(-1, 0, -1, 2, 2, 2, 0.2F), box(-0.5F, 2, -1.3F, 1, 5, 1));
		return b.build();
	}

	/** A "U" under each pec, bars at the collarbones, a zigzag down the sternum and the lines of the abdomen. */
	private static void chestMarks(final Part body) {
		for (int side = -1; side <= 1; side += 2) {
			float a = side < 0 ? -6.5F : 2.5F;
			// on the chest pixel centres are whole units: [a, a + 1) is one column
			body.mark(Mark.FRONT, a, 3, a + 1, 8, INK).mark(Mark.FRONT, a + 3, 3, a + 4, 8, INK).mark(Mark.FRONT, a, 7, a + 4, 8, INK);
			float c = side < 0 ? -6.5F : 3.5F;
			body.mark(Mark.FRONT, c, 1, c + 3, 2, INK);
		}
		body.mark(Mark.FRONT, -0.5F, 5, 0.5F, 7, INK).mark(Mark.FRONT, 0.5F, 7, 1.5F, 8, INK).mark(Mark.FRONT, -0.5F, 8, 0.5F, 10, INK);
		// the belly box starts on a whole unit, so its pixel centres sit on half units
		body.mark(Mark.FRONT, -0.5F, 11, 0.5F, 13, CREASE).mark(Mark.FRONT, -4, 12, 4, 13, CREASE);
	}

	/** The mouth across his belly: lips curling up at the corners, two rows of teeth, the dark gap between them. */
	private static void bellyMouth(final Part body) {
		body.mark(Mark.FRONT, -5, 14, 5, 19, 0x8E4450).mark(Mark.FRONT, -5, 13, -4, 14, 0x8E4450).mark(Mark.FRONT, 4, 13, 5, 14, 0x8E4450)
			.mark(Mark.FRONT, -4, 15, 4, 18, 0x200A10)
			.mark(Mark.FRONT, -3, 15, 3, 16, TEETH).mark(Mark.FRONT, -3, 17, 3, 18, TEETH)
			.mark(Mark.FRONT, -2, 15, -1, 16, 0xB8AE9C).mark(Mark.FRONT, 1, 15, 2, 16, 0xB8AE9C)
			.mark(Mark.FRONT, -2, 17, -1, 18, 0xB8AE9C).mark(Mark.FRONT, 1, 17, 2, 18, 0xB8AE9C);
	}

	/** His own face on the left half: a red eye, the black mark under it and a toothy grin. */
	private static void face(final Part head) {
		head.glowMark(Mark.FRONT, 1.5F, -6, 3.5F, -5, 0xF4ECE4).glowMark(Mark.FRONT, 1.5F, -6, 2.5F, -5, EYE_RED)
			.mark(Mark.FRONT, 1.5F, -7, 3.5F, -6, 0x6A3A40)
			.mark(Mark.FRONT, 1.5F, -4, 3.5F, -3, 0x2A1A2C).mark(Mark.FRONT, 3.5F, -4, 4.5F, -2, 0x2A1A2C)
			.mark(Mark.FRONT, -1.5F, -3, 3.5F, -2, 0x3A1018).mark(Mark.FRONT, -0.5F, -3, 2.5F, -2, TEETH)
			.mark(Mark.FRONT, 0.5F, -1, 1.5F, 0, 0x2A1A2C).mark(Mark.FRONT, 2.5F, -1, 3.5F, 0, 0x2A1A2C);
	}

	/** Spiky pink hair swept up and back. */
	private static void hair(final Builder b) {
		Part hair = b.part("hair", "head", Role.EXTRA, 0, 0, 0, "hair", box(-5, -12, -5, 10, 4, 10), box(-5.5F, -9, -1, 1, 4, 6), box(4.5F, -9, -1, 1, 4, 6));
		float[][] spikes = {
			// x, z, height, zRot, xRot: a crown of thick spikes swept up and back, each with a thin tip
			{0, -3, 7, 0, -0.2F}, {-2.5F, -3, 6, -0.3F, -0.15F}, {2.5F, -3, 6, 0.3F, -0.15F}, {-4, -1, 6, -0.6F, -0.2F}, {4, -1, 6, 0.6F, -0.2F},
			{-1.5F, 0, 8, -0.15F, -0.45F}, {1.5F, 0, 8, 0.15F, -0.45F}, {-4, 2, 5, -0.8F, -0.5F}, {4, 2, 5, 0.8F, -0.5F},
			{-2, 3, 6, -0.35F, -0.8F}, {2, 3, 6, 0.35F, -0.8F}, {0, 3.5F, 6, 0, -1.0F},
		};
		for (int i = 0; i < spikes.length; i++) {
			float[] s = spikes[i];
			int h = (int)s[2];
			Part spike = b.part("hair_spike" + i, hair.name, Role.EXTRA, s[0], -11, s[1], "hair", box(-1.5F, -h, -1.5F, 3, h, 3));
			spike.zRot = s[3];
			spike.xRot = s[4];
			b.part("hair_tip" + i, spike.name, Role.EXTRA, 0, -h, 0, "hair", box(-0.5F, -3, -0.5F, 1, 3, 1));
		}
	}

	// ------------------------------------------------------------------ Mahoraga

	private static final int MAHO_INK = 0x16161A;
	private static final int MAHO_LINE = 0xB4B0AA;

	/** Mahoraga: a broad white giant with a grinning beaked face, four feathered wings on its head and the eight-spoked wheel above. */
	static BodyPlan mahoraga() {
		Builder b = BodyPlan.builder("mahoraga", Rig.BIPED).scale(1.25F).shaded()
			.skin("flesh", Skin.of(0xF6F4F0, 0xFFFFFF, SMOOTH))
			.skin("skirt", Skin.of(0x2A3432, 0x141A18, CLOTH))
			.skin("feather", Skin.of(0xF6F4F0, 0xD0CCC6, SMOOTH))
			.skin("gold", Skin.of(0xE0B450, 0xFFE8A0, METAL))
			.skin("wrap", Skin.of(0x9A9894, 0x6A6864, CLOTH))
			.skin("sash", Skin.of(0x8C8A86, 0x5E5C58, CLOTH))
			.skin("black", Skin.of(0x1C1C20, 0x4A4A52, METAL))
			.skin("blade", Skin.of(0x2E2E34, 0x6A6A74, METAL));

		Part body = b.part("body", null, Role.BODY, 0, -18, 0, "flesh",
			box(-8.5F, 0, -4.5F, 17, 11, 9), box(-6.5F, 11, -4, 13, 9, 8), box(-7.5F, 1, -5.3F, 7, 5, 1), box(0.5F, 1, -5.3F, 7, 5, 1),
			box(-5, -2, -2.5F, 10, 2, 5));
		// the black necklace across the chest: a cord with three rings, a tassel under each, and the lines of the abdomen
		body.mark(Mark.FRONT, -6.5F, 1, 6.5F, 2, MAHO_INK);
		for (int c = -4; c <= 4; c += 4) {
			body.mark(Mark.FRONT, c - 1.5F, 2, c + 1.5F, 5, MAHO_INK).mark(Mark.FRONT, c - 0.5F, 3, c + 0.5F, 4, 0xF6F4F0)
				.mark(Mark.FRONT, c - 0.5F, 5, c + 0.5F, 7, MAHO_INK).mark(Mark.FRONT, c - 1.5F, 7, c + 1.5F, 8, MAHO_INK);
		}
		body.mark(Mark.FRONT, -0.5F, 11, 0.5F, 20, MAHO_LINE).mark(Mark.FRONT, -5.5F, 13, 5.5F, 14, MAHO_LINE).mark(Mark.FRONT, -5.5F, 16, 5.5F, 17, MAHO_LINE);

		Part head = b.part("head", null, Role.HEAD, 0, -18, 0, "flesh", box(-4, -11, -4.5F, 8, 11, 9), box(-3.5F, -6, -5.5F, 7, 5, 1));
		head.eyes = 0;
		// no eyes: a brow ridge over a grin full of teeth
		head.mark(Mark.FRONT, -3, -8, 3, -7, MAHO_LINE)
			.mark(Mark.FRONT, -3.5F, -5, 3.5F, -2, 0x0E0C0E).mark(Mark.FRONT, -3.5F, -6, -2.5F, -5, 0x0E0C0E).mark(Mark.FRONT, 2.5F, -6, 3.5F, -5, 0x0E0C0E)
			.mark(Mark.FRONT, -3.5F, -5, 3.5F, -4, 0xF2EEE6).mark(Mark.FRONT, -3.5F, -3, 3.5F, -2, 0xF2EEE6)
			.mark(Mark.FRONT, -2.5F, -5, -1.5F, -4, 0xA8A49C).mark(Mark.FRONT, -0.5F, -5, 0.5F, -4, 0xA8A49C).mark(Mark.FRONT, 1.5F, -5, 2.5F, -4, 0xA8A49C)
			.mark(Mark.FRONT, -1.5F, -3, -0.5F, -2, 0xA8A49C).mark(Mark.FRONT, 0.5F, -3, 1.5F, -2, 0xA8A49C);
		b.part("crest", "head", Role.EXTRA, 0, 0, 0, "feather", box(-1, -15, -3, 2, 5, 7));
		// two feathered wings on each side of the head, each a fan of three feathers with grey edges
		for (int side = -1; side <= 1; side += 2) {
			String s = side < 0 ? "r" : "l";
			wing(b, "hwing_" + s, side, -8, 1, new int[] {17, 14, 10}, 0.4F);
			wing(b, "hwing_low_" + s, side, -4, 1.5F, new int[] {12, 9}, 0.05F);
		}
		wheel(b);

		for (int i = 0; i < 2; i++) {
			int side = i == 0 ? -1 : 1;
			String s = i == 0 ? "r" : "l";
			Part arm = b.part("arm_" + s, null, i == 0 ? Role.ARM_RIGHT : Role.ARM_LEFT, 0, side * 11.5F, -16, 0, "flesh",
				box(-3.5F, -3, -3.5F, 7, 7, 7), box(-3, -2, -3, 6, 18, 6), box(-3.5F, 16, -3.5F, 7, 5, 7));
			arm.zRot = -side * 0.15F;
		}
		// the right forearm is wrapped and ends in the Sword of Extermination; a black ring on each wrist
		b.part("wrap_r", "arm_r", Role.EXTRA, 0, 0, 0, "wrap", box(-3.5F, 8, -3.5F, 7, 8, 7)).band(9, 10, 0x7A7874).band(11, 12, 0x7A7874).band(13, 14, 0x7A7874);
		b.part("ring_r", "arm_r", Role.EXTRA, 0, 0, 0, "black", box(-3.5F, 15.5F, -3.5F, 7, 1, 7, 0.35F));
		b.part("ring_l", "arm_l", Role.EXTRA, 0, 0, 0, "black", box(-3, 14, -3, 6, 1, 6, 0.35F));
		Part sword = b.part("sword", "arm_r", Role.EXTRA, 0, 19, -1, "blade", box(-0.5F, 0, -1.5F, 1, 24, 3), box(-0.5F, 24, -1, 1, 3, 2), box(-0.5F, 27, -0.5F, 1, 2, 1));
		// angled forward so the tip stays above the ground while the arm hangs
		sword.xRot = -0.75F;

		// a grey sash knotted over a long dark skirt; bare clawed feet with black anklets
		Part sash = b.part("sash", "body", Role.EXTRA, 0, 18, 0, "sash", box(-7.5F, 0, -5, 15, 3, 10));
		b.part("sash_knot", sash.name, Role.EXTRA, 0, 0, 0, "sash", box(-2, 0, -6, 4, 3, 1), box(-2.5F, 3, -5.7F, 5, 14, 1), box(3, 2, -5.6F, 2, 9, 1));
		b.part("skirt", "body", Role.EXTRA, 0, 21, 0, "skirt", box(-8, 0, -5.5F, 16, 15, 11));
		for (int i = 0; i < 2; i++) {
			String s = i == 0 ? "r" : "l";
			b.part("leg_" + s, null, i == 0 ? Role.LEG_RIGHT : Role.LEG_LEFT, (i == 0 ? -1 : 1) * 4, 2, 0, "flesh", box(-3.5F, 0, -3.5F, 7, 11, 7));
			b.part("shin_" + s, "leg_" + s, Role.SHIN, 10 + i, 0, 11, 0, "flesh", box(-3, 0, -3, 6, 9, 6), box(-3, 9, -5, 6, 2, 7))
				.mark(Mark.FRONT, -3, 9, 3, 11, 0xC8C4BE).mark(Mark.FRONT, -2, 10, -1, 11, 0x5A5856).mark(Mark.FRONT, 1, 10, 2, 11, 0x5A5856);
			b.part("anklet_" + s, "shin_" + s, Role.EXTRA, 0, 0, 0, "black", box(-3, 6, -3, 6, 1, 6, 0.35F));
		}
		return b.build();
	}

	/**
	 * A head wing on {@code side} (-1 right, 1 left): a fan of feathers, longest on top, each tilted a little further down and ending in
	 * a narrower tip; {@code lift} raises the whole fan.
	 */
	private static void wing(final Builder b, final String name, final int side, final float y, final float z, final int[] lengths, final float lift) {
		Part root = b.part(name, "head", Role.EXTRA, side * 4, y, z, "feather");
		root.zRot = -side * lift;
		for (int i = 0; i < lengths.length; i++) {
			int l = lengths[i];
			Part feather = b.part(name + "_f" + i, root.name, Role.EXTRA, 0, i * 1.5F, 0, "feather", box(side < 0 ? -l : 0, -1, -0.5F, l, 3, 1));
			feather.zRot = side * 0.12F * i;
			feather.band(1, 2, 0xC4C0BA);
			b.part(name + "_t" + i, feather.name, Role.EXTRA, side * l, 0, 0, "feather", box(side < 0 ? -4 : 0, -0.5F, -0.5F, 4, 2, 1)).zRot = -side * 0.2F;
		}
	}

	/** The wheel of adaptation floating over the head: eight spokes ending in balls beyond an eight-sided rim, a hub in the middle. */
	private static void wheel(final Builder b) {
		Part wheel = b.part("wheel", "head", Role.WHEEL, 0, -19, 0, "gold");
		b.part("wheel_hub", wheel.name, Role.EXTRA, 0, 0, 0, "gold", box(-1.5F, -1.5F, -1.5F, 3, 3, 3));
		for (int i = 0; i < 8; i++) {
			Part spoke = b.part("wheel_spoke" + i, wheel.name, Role.EXTRA, 0, 0, 0, "gold", box(-0.5F, -0.5F, 1, 1, 1, 8), box(-1.5F, -1.5F, 9, 3, 3, 3));
			spoke.yRot = i * Mth.PI / 4;
			b.part("wheel_rim" + i, wheel.name, Role.EXTRA, 0, 0, 0, "gold", box(-3, -0.5F, 7, 6, 1, 1)).yRot = i * Mth.PI / 4 + Mth.PI / 8;
		}
	}

	// ------------------------------------------------------------------ the Malevolent Shrine

	/**
	 * The Malevolent Shrine: a two-roofed temple whose hall is one giant mouth, horns curling up from every roof corner, horned
	 * skulls piled in front and a pool of blood under it. The lower jaw is the plan's {@link Role#JAW}, so it gapes when the
	 * shrine roars.
	 */
	static BodyPlan shrine() {
		// towering over the fight: about 14 blocks tall and 13 wide
		Builder b = BodyPlan.builder("malevolent_shrine", Rig.BIPED).scale(4.6F).shaded()
			.skin("wood", Skin.of(0x3A2424, 0x5A3434, BARK))
			.skin("roof", Skin.of(0x2A2026, 0xB08A40, METAL))
			.skin("bone", Skin.of(0xECE4D0, 0x9A8E78, BONE))
			.skin("gum", Skin.of(0x8A2A34, 0xC04A58, SMOOTH))
			.skin("teeth", Skin.of(0xF4ECDA, 0xC8BEA8, BONE))
			.skin("dark", Skin.of(0x140C10, 0x2A1A1E, SMOOTH))
			.skin("blood", Skin.of(0x5A0A12, 0x8A1A24, SMOOTH));
		b.part("body", null, Role.BODY, 0, 0, 0, "dark", box(-17, 21, -13, 34, 3, 26));
		b.part("pool", "body", Role.EXTRA, 0, 0, 0, "blood", box(-23, 23, -19, 46, 1, 38));
		b.part("hall", "body", Role.EXTRA, 0, 0, 0, "dark", box(-11, -5, 0, 22, 26, 3));
		int pillar = 0;
		for (int x = -12; x <= 12; x += 24) {
			for (int z = -8; z <= 6; z += 14) {
				b.part("pillar" + pillar++, "body", Role.EXTRA, x, 0, z, "wood", box(-1.5F, -6, -1.5F, 3, 27, 3));
			}
		}
		// the mouth: gums above and a jaw below, a row of teeth on each, fangs at the corners
		b.part("gum_up", "body", Role.EXTRA, 0, 0, 0, "gum", box(-11, -6, -6, 22, 5, 7));
		Box[] upper = new Box[10];
		for (int i = 0; i < 10; i++) {
			int h = i == 0 || i == 9 ? 6 : 3 + (i % 2);
			upper[i] = box(-10 + i * 2, -1, -5.5F, 2, h, 1);
		}
		b.part("teeth_up", "body", Role.EXTRA, 0, 0, 0, "teeth", upper);
		b.part("jaw", "body", Role.JAW, 0, 14, 1, "gum", box(-11, 0, -7, 22, 5, 7));
		Box[] lower = new Box[10];
		for (int i = 0; i < 10; i++) {
			int h = i == 0 || i == 9 ? 5 : 2 + ((i + 1) % 2);
			lower[i] = box(-10 + i * 2, -h, -6.5F, 2, h, 1);
		}
		b.part("teeth_low", "jaw", Role.EXTRA, 0, 0, 0, "teeth", lower);
		// two roofs and the hall between them
		b.part("roof", "body", Role.EXTRA, 0, 0, 0, "roof", box(-21, -10, -15, 42, 3, 30), box(-19, -12, -13, 38, 2, 26));
		b.part("eaves", "body", Role.EXTRA, 0, 0, 0, "wood", box(-20, -7, -14, 40, 1, 28));
		b.part("upper", "body", Role.EXTRA, 0, 0, 0, "wood", box(-9, -18, -6, 18, 6, 12));
		b.part("roof2", "body", Role.EXTRA, 0, 0, 0, "roof", box(-14, -21, -10, 28, 3, 20), box(-12, -23, -8, 24, 2, 16));
		b.part("ridge", "body", Role.EXTRA, 0, 0, 0, "roof", box(-15, -25, -1, 30, 2, 2));
		int horn = 0;
		for (float[] c : new float[][] {{20, -10, 14, 7}, {13, -21, 9, 5}, {15, -25, 0, 6}}) {
			for (int sx = -1; sx <= 1; sx += 2) {
				for (int sz = c[2] == 0 ? 1 : -1; sz <= 1; sz += 2) {
					horn(b, "horn" + horn++, sx * c[0], c[1], sz * c[2], sx, (int)c[3]);
				}
			}
		}
		// horned skulls heaped in front and along the sides
		float[][] skulls = {{-15, -16, 0.3F}, {-9, -17, -0.2F}, {9, -17, 0.2F}, {15, -16, -0.3F}, {-20, -5, 1.2F}, {20, -5, -1.2F}, {-20, 6, 1.6F},
			{20, 6, -1.6F}};
		for (int i = 0; i < skulls.length; i++) {
			skull(b, "skull" + i, skulls[i][0], skulls[i][1], skulls[i][2]);
		}
		return b.build();
	}

	/** A horn curling up and outward ({@code side} -1 right, 1 left) from a roof corner. */
	private static void horn(final Builder b, final String name, final float x, final float y, final float z, final int side, final int length) {
		Part base = b.part(name, "body", Role.EXTRA, x, y, z, "bone", box(-1, -length, -1, 2, length, 2));
		base.zRot = side * 0.45F;
		Part tip = b.part(name + "_tip", name, Role.EXTRA, 0, -length, 0, "bone", box(-0.5F, -length + 1, -0.5F, 1, length - 1, 1));
		tip.zRot = -side * 0.5F;
		base.mark(Mark.SIDES, 0, -length, 0, -length + 1, 0x9A8E78);
	}

	/** A horned cow skull lying on the ground, turned by {@code yaw}. */
	private static void skull(final Builder b, final String name, final float x, final float z, final float yaw) {
		Part skull = b.part(name, "body", Role.EXTRA, x, 24, z, "bone", box(-3, -5, -3, 6, 5, 6), box(-2, -3, -5, 4, 3, 2));
		skull.yRot = yaw;
		skull.mark(Mark.FRONT, -3, -4, -1, -3, 0x2A2018).mark(Mark.FRONT, 1, -4, 3, -3, 0x2A2018)
			.mark(Mark.FRONT, -2, -1, -1, 0, 0x2A2018).mark(Mark.FRONT, 1, -1, 2, 0, 0x2A2018);
		b.part(name + "_horns", name, Role.EXTRA, 0, 0, 0, "bone",
			box(-6, -5, -0.5F, 3, 1, 1), box(-6, -8, -0.5F, 1, 3, 1), box(3, -5, -0.5F, 3, 1, 1), box(5, -8, -0.5F, 1, 3, 1));
	}

	private SukunaPlans() {
	}
}
