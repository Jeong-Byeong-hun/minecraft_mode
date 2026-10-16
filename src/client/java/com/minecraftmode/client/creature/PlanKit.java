package com.minecraftmode.client.creature;

import static com.minecraftmode.client.creature.BodyPlan.box;

import com.minecraftmode.client.creature.BodyPlan.Builder;
import com.minecraftmode.client.creature.BodyPlan.Part;
import com.minecraftmode.client.creature.BodyPlan.Role;
import net.minecraft.util.Mth;

/**
 * Body-building helpers for creature plans: a humanoid frame, a quadruped frame, spider legs and
 * segmented chains (tails, necks, tentacles). Every helper keeps the feet on y = 24.
 */
final class PlanKit {
	/** Sizes of a humanoid frame (model units). */
	record Humanoid(int headW, int headH, int headD, int bodyW, int bodyH, int bodyD, int armW, int armL, int legW, int legL) {
		float bodyTop() {
			return 24 - this.legL - this.bodyH;
		}
	}

	/**
	 * head, body, arm_r, arm_l, leg_r, leg_l. Legs end on y = 24, the body sits on the legs, the head
	 * on the body and the arms hang from the shoulders.
	 */
	static void humanoid(final Builder b, final Humanoid h, final String headSkin, final String bodySkin, final String armSkin, final String legSkin, final int eyes,
		final int eyeColor) {
		float top = h.bodyTop();
		b.part("body", null, Role.BODY, 0, top, 0, bodySkin, box(-h.bodyW() / 2.0F, 0, -h.bodyD() / 2.0F, h.bodyW(), h.bodyH(), h.bodyD()));
		Part head = b.part("head", null, Role.HEAD, 0, top, 0, headSkin, box(-h.headW() / 2.0F, -h.headH(), -h.headD() / 2.0F, h.headW(), h.headH(), h.headD()));
		head.eyes = eyes;
		head.eyeColor = eyeColor;
		float shoulder = h.bodyW() / 2.0F + h.armW() / 2.0F;
		b.part("arm_r", null, Role.ARM_RIGHT, -shoulder, top + 2, 0, armSkin, box(-h.armW() / 2.0F, -2, -h.armW() / 2.0F, h.armW(), h.armL(), h.armW()));
		b.part("arm_l", null, Role.ARM_LEFT, shoulder, top + 2, 0, armSkin, box(-h.armW() / 2.0F, -2, -h.armW() / 2.0F, h.armW(), h.armL(), h.armW()));
		float hip = Math.max(h.legW() / 2.0F, h.bodyW() / 4.0F);
		// legs bend at the knee (shin_r/shin_l, Role.SHIN 10/11) and end in a foot that reaches a little forward
		int thigh = (h.legL() + 1) / 2;
		int shin = h.legL() - thigh;
		float half = h.legW() / 2.0F;
		for (int i = 0; i < 2; i++) {
			String s = i == 0 ? "r" : "l";
			b.part("leg_" + s, null, i == 0 ? Role.LEG_RIGHT : Role.LEG_LEFT, (i == 0 ? -1 : 1) * hip, 24 - h.legL(), 0, legSkin,
				box(-half, 0, -half, h.legW(), thigh, h.legW()));
			b.part("shin_" + s, "leg_" + s, Role.SHIN, 10 + i, 0, thigh, 0, legSkin,
				box(-half, 0, -half, h.legW(), shin, h.legW()),
				box(-half, shin - 1, -half - 1, h.legW(), 1, 1));
		}
	}

	/** Sizes of a four-legged frame. */
	record Quad(int bodyW, int bodyH, int bodyL, int headW, int headH, int headD, int legW, int legL) {
		float bodyY() {
			return 24 - this.legL - this.bodyH / 2.0F;
		}
	}

	/** body (horizontal), head at the front (-z), four legs (index 0 front-left .. 3 back-right). */
	static void quadruped(final Builder b, final Quad q, final String bodySkin, final String headSkin, final String legSkin, final int eyes, final int eyeColor) {
		float y = q.bodyY();
		b.part("body", null, Role.BODY, 0, y, 0, bodySkin, box(-q.bodyW() / 2.0F, -q.bodyH() / 2.0F, -q.bodyL() / 2.0F, q.bodyW(), q.bodyH(), q.bodyL()));
		Part head = b.part("head", null, Role.HEAD, 0, y - q.bodyH() / 2.0F + 1, -q.bodyL() / 2.0F, headSkin,
			box(-q.headW() / 2.0F, -q.headH() / 2.0F, -q.headD(), q.headW(), q.headH(), q.headD()));
		head.eyes = eyes;
		head.eyeColor = eyeColor;
		float lx = q.bodyW() / 2.0F - q.legW() / 2.0F;
		float lz = q.bodyL() / 2.0F - q.legW() / 2.0F - 1;
		float[][] legs = {{lx, -lz}, {-lx, -lz}, {lx, lz}, {-lx, lz}};
		for (int i = 0; i < 4; i++) {
			b.part("leg" + i, null, Role.LEG_QUAD, i, legs[i][0], 24 - q.legL(), legs[i][1], legSkin,
				box(-q.legW() / 2.0F, 0, -q.legW() / 2.0F, q.legW(), q.legL(), q.legW()));
		}
	}

	/**
	 * Four legs (index 0 front-left .. 3 back-right) split at the knee: {@code leg<i>} ({@link Role#LEG_QUAD}) holds the upper half,
	 * {@code shin<i>} ({@link Role#SHIN}) the lower one, which ends on y = 24. Positions match {@link #quadruped}.
	 */
	static void jointedLegs(final Builder b, final Quad q, final String upperSkin, final String lowerSkin) {
		float lx = q.bodyW() / 2.0F - q.legW() / 2.0F;
		float lz = q.bodyL() / 2.0F - q.legW() / 2.0F - 1;
		float[][] legs = {{lx, -lz}, {-lx, -lz}, {lx, lz}, {-lx, lz}};
		int upper = (q.legL() + 1) / 2;
		int lower = q.legL() - upper;
		// big legs get a thicker thigh over a slimmer shin
		int taper = q.legW() >= 4 ? 1 : 0;
		int thigh = q.legW() + taper;
		int shin = q.legW() - taper;
		for (int i = 0; i < 4; i++) {
			b.part("leg" + i, null, Role.LEG_QUAD, i, legs[i][0], 24 - q.legL(), legs[i][1], upperSkin, box(-thigh / 2.0F, 0, -thigh / 2.0F, thigh, upper, thigh));
			b.part("shin" + i, "leg" + i, Role.SHIN, i, 0, upper, 0, lowerSkin, box(-shin / 2.0F, 0, -shin / 2.0F, shin, lower, shin));
		}
	}

	/**
	 * A riding saddle on {@code parent} (pivot on the top of the back, {@code top} model units from the parent's pivot): a blanket
	 * with side flaps, the seat with pommel and cantle, and stirrups hanging on straps.
	 */
	static void saddle(final Builder b, final String parent, final float top, final float z, final int bodyW, final String leather, final String cloth,
		final String metal) {
		float half = bodyW / 2.0F;
		b.part("blanket", parent, Role.EXTRA, 0, top, z, cloth,
			box(-half - 0.5F, -0.5F, -5, bodyW + 1, 1, 10),
			box(-half - 1, -0.5F, -4, 1, 5, 8),
			box(half, -0.5F, -4, 1, 5, 8));
		b.part("saddle", parent, Role.EXTRA, 0, top, z, leather,
			box(-3, -1.5F, -3.5F, 6, 1, 7),
			box(-2, -3, -4.5F, 4, 2, 2),
			box(-2.5F, -3, 2.5F, 5, 2, 2));
		b.part("stirrups", parent, Role.EXTRA, 0, top, z, metal,
			box(-half - 1.5F, 0, -0.5F, 1, 6, 1),
			box(-half - 2, 6, -1, 2, 1, 2),
			box(half + 0.5F, 0, -0.5F, 1, 6, 1),
			box(half, 6, -1, 2, 1, 2));
	}

	/** Eyes on the sides of a long head (horses, deer), where the painted front eyes would sit on the nose. */
	static void sideEyes(final Builder b, final String head, final float halfWidth, final float y, final float z, final String skin) {
		b.part("eye_r", head, Role.EXTRA, -halfWidth, y, z, skin, box(-0.6F, -0.5F, -0.5F, 1, 1, 1));
		b.part("eye_l", head, Role.EXTRA, halfWidth, y, z, skin, box(-0.4F, -0.5F, -0.5F, 1, 1, 1));
	}

	/** Two ears ({@link Role#EAR}) on {@code head}; {@code tip} paints a one-pixel tip in another skin ("" for none). */
	static void ears(final Builder b, final String head, final float x, final float y, final float z, final int w, final int h, final int d, final String skin,
		final String tip) {
		for (int i = 0; i < 2; i++) {
			String s = i == 0 ? "r" : "l";
			float px = i == 0 ? -x : x;
			b.part("ear_" + s, head, Role.EAR, i, px, y, z, skin, box(-w / 2.0F, -h, -d / 2.0F, w, h, d));
			if (!tip.isEmpty()) {
				b.part("ear_tip_" + s, "ear_" + s, Role.EXTRA, 0, -h, 0, tip, box(-Math.max(1, w - 1) / 2.0F, -1, -d / 2.0F, Math.max(1, w - 1), 1, d));
			}
		}
	}

	/** A horizontal body for a {@link Quad}, without head or legs. */
	static Part torso(final Builder b, final Quad q, final String skin) {
		return b.part("body", null, Role.BODY, 0, q.bodyY(), 0, skin, box(-q.bodyW() / 2.0F, -q.bodyH() / 2.0F, -q.bodyL() / 2.0F, q.bodyW(), q.bodyH(), q.bodyL()));
	}

	/** The head of a {@link Quad} at the front top of its body, like {@link #quadruped}. */
	static Part head(final Builder b, final Quad q, final String skin, final int eyeColor) {
		Part head = b.part("head", null, Role.HEAD, 0, q.bodyY() - q.bodyH() / 2.0F + 1, -q.bodyL() / 2.0F, skin,
			box(-q.headW() / 2.0F, -q.headH() / 2.0F, -q.headD(), q.headW(), q.headH(), q.headD()));
		head.eyes = 2;
		head.eyeColor = eyeColor;
		return head;
	}

	/** Bends a chain made by {@link #chain}: the root by {@code root}, every later segment by {@code curl}. */
	static void bendChain(final Builder b, final String name, final int count, final float root, final float curl) {
		for (int i = 0; i < count; i++) {
			b.get(name + i).xRot = i == 0 ? root : curl;
		}
	}

	/** How far a jointed spider leg rises from the body to its knee, and how steeply the foot comes down (radians). */
	private static final float KNEE_RISE = 0.6F;
	private static final float FOOT_DROP = 0.95F;

	/**
	 * {@code count} jointed spider legs per side (index 0..count-1 left, 4.. right, as {@link Role#LEG_SPIDER}): each rises to a knee
	 * {@code length} out and comes down to a foot on y = 24. {@code parent} must be a root part whose pivot height is its own.
	 */
	static void jointedSpiderLegs(final Builder b, final String parent, final int count, final float halfWidth, final float y, final float zFront, final float zBack,
		final int length, final int thick, final String skin, final String footSkin) {
		float hipY = b.get(parent).py + y;
		float kneeY = hipY - length * Mth.sin(KNEE_RISE);
		int foot = Math.max(2, Math.round((24 - kneeY) / Mth.sin(FOOT_DROP)));
		float t = thick / 2.0F;
		for (int i = 0; i < count; i++) {
			float k = count == 1 ? 0.5F : i / (float)(count - 1);
			float z = zFront + (zBack - zFront) * k;
			float fan = 0.75F - 1.5F * k;
			Part left = b.part("leg_l" + i, parent, Role.LEG_SPIDER, i, halfWidth, y, z, skin, box(-t, -t, -t, length, thick, thick));
			left.zRot = -KNEE_RISE;
			left.yRot = fan;
			b.part("foot_l" + i, "leg_l" + i, Role.EXTRA, length - thick, 0, 0, footSkin, box(-t, -t, -t, foot, thick, thick)).zRot = KNEE_RISE + FOOT_DROP;
			Part right = b.part("leg_r" + i, parent, Role.LEG_SPIDER, i + 4, -halfWidth, y, z, skin, box(-length + t, -t, -t, length, thick, thick));
			right.zRot = KNEE_RISE;
			right.yRot = -fan;
			b.part("foot_r" + i, "leg_r" + i, Role.EXTRA, -(length - thick), 0, 0, footSkin, box(-foot + t, -t, -t, foot, thick, thick)).zRot = -(KNEE_RISE + FOOT_DROP);
		}
	}

	/** Eight spider legs from the body sides at height {@code y}, spread from {@code zFront} to {@code zBack}. */
	static void spiderLegs(final Builder b, final String parent, final float halfWidth, final float y, final float zFront, final float zBack, final int length,
		final String skin) {
		float[] fan = {0.75F, 0.25F, -0.25F, -0.75F};
		for (int i = 0; i < 4; i++) {
			float z = zFront + (zBack - zFront) * i / 3.0F;
			Part left = b.part("leg_l" + i, parent, Role.LEG_SPIDER, i, halfWidth, y, z, skin, box(-1, -1, -1, length, 2, 2));
			left.zRot = 0.55F;
			left.yRot = fan[i];
			Part right = b.part("leg_r" + i, parent, Role.LEG_SPIDER, i + 4, -halfWidth, y, z, skin, box(-length + 1, -1, -1, length, 2, 2));
			right.zRot = -0.55F;
			right.yRot = -fan[i];
		}
	}

	/**
	 * A chain of {@code count} segments hanging from {@code parent}: each segment is a child of the
	 * previous one, pivoting at its end; sizes shrink by {@code taper} per segment. The chain grows
	 * toward +z (tails) or -z (necks) when {@code dir} is +1 / -1, or downward when {@code dir} is 0.
	 */
	static String chain(final Builder b, final String name, final String parent, final Role role, final int indexBase, final float px, final float py, final float pz,
		final int count, final int width, final int length, final float taper, final int dir, final float bend, final String skin) {
		String previous = parent;
		float w = width;
		float l = length;
		int previousLength = 0;
		for (int i = 0; i < count; i++) {
			int wi = Math.max(1, Math.round(w));
			int li = Math.max(1, Math.round(l));
			// the first segment sits at (px, py, pz); later ones at the end of the previous segment
			float ox = i == 0 ? px : 0;
			float oy = i == 0 ? py : dir == 0 ? previousLength : 0;
			float oz = i == 0 ? pz : dir == 0 ? 0 : dir * previousLength;
			Part part = dir == 0
				? b.part(name + i, previous, role, indexBase + i, ox, oy, oz, skin, box(-wi / 2.0F, 0, -wi / 2.0F, wi, li, wi))
				: b.part(name + i, previous, role, indexBase + i, ox, oy, oz, skin, box(-wi / 2.0F, -wi / 2.0F, dir > 0 ? 0 : -li, wi, wi, li));
			part.xRot = bend;
			previous = name + i;
			previousLength = li;
			w *= taper;
			l *= taper;
		}
		return previous;
	}

	private PlanKit() {
	}
}
