package com.minecraftmode.test;

import com.minecraftmode.client.creature.BodyPlan;
import com.minecraftmode.client.creature.CreaturePlans;
import java.util.HashMap;
import java.util.Map;

/**
 * How much room a creature's model takes, in blocks, from its body plan's rest pose (pivots added up, rotations left out, like the
 * datagen preview). Cameras frame models with it, since wings, tails and weapons reach well past the hitbox.
 *
 * @param span the largest side
 * @param above how far the model rises above the entity's feet
 */
record ModelSize(double span, double above) {
	static final ModelSize NONE = new ModelSize(0.0, 0.0);

	/** The model of entity {@code id} (a plan id), or {@link #NONE} when it has no body plan. */
	static ModelSize of(final String id) {
		BodyPlan plan = CreaturePlans.get(id);
		if (plan == null) {
			return NONE;
		}
		Map<String, float[]> origin = new HashMap<>();
		float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
		float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
		for (BodyPlan.Part part : plan.parts) {
			float[] parent = part.parent == null ? new float[3] : origin.get(part.parent);
			float[] o = {parent[0] + part.px, parent[1] + part.py, parent[2] + part.pz};
			origin.put(part.name, o);
			for (BodyPlan.Box box : part.boxes) {
				float[] lo = {o[0] + box.x(), o[1] + box.y(), o[2] + box.z()};
				float[] size = {box.w(), box.h(), box.d()};
				for (int k = 0; k < 3; k++) {
					min[k] = Math.min(min[k], lo[k]);
					max[k] = Math.max(max[k], lo[k] + size[k]);
				}
			}
		}
		double blocks = plan.scale / 16.0;
		double span = Math.max(max[0] - min[0], Math.max(max[1] - min[1], max[2] - min[2])) * blocks;
		// model y grows downward and the feet are at y = 24
		return new ModelSize(span, Math.max(0.0, 24 - min[1]) * blocks);
	}
}
