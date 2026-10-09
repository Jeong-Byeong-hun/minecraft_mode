package com.minecraftmode.client.creature;

import static com.minecraftmode.client.creature.BodyPlan.box;

import com.minecraftmode.client.creature.BodyPlan.Builder;
import com.minecraftmode.client.creature.BodyPlan.Part;
import com.minecraftmode.client.creature.BodyPlan.Role;

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
		b.part("leg_r", null, Role.LEG_RIGHT, -hip, 24 - h.legL(), 0, legSkin, box(-h.legW() / 2.0F, 0, -h.legW() / 2.0F, h.legW(), h.legL(), h.legW()));
		b.part("leg_l", null, Role.LEG_LEFT, hip, 24 - h.legL(), 0, legSkin, box(-h.legW() / 2.0F, 0, -h.legW() / 2.0F, h.legW(), h.legL(), h.legW()));
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
