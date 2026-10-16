package com.minecraftmode.client.datagen.art;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.minecraftmode.client.creature.BodyPlan;
import com.minecraftmode.client.creature.BodyPlan.Part;
import com.minecraftmode.client.creature.BodyPlan.PlacedBox;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;

/**
 * Writes a {@link BodyPlan} as a Blockbench project (Modded Entity format, box UV) with its texture embedded, so the texture
 * can be painted over in 3D. Save the painted texture as {@code art/creature/<id>.png} and datagen uses it instead of the
 * generated one (see {@code CreatureTextureProvider}). The cubes are for reference only: the model itself stays in code.
 *
 * <p>Coordinates: Blockbench's Java entity space is the vanilla model space turned half a turn around z (x and y flipped, feet
 * on y = 0 instead of y = 24), so a pivot (px, py, pz) becomes (-px, 24 - py, pz) and rotations around x and y change sign.
 * Blockbench stores pivots and cubes in absolute, unrotated coordinates, so pivots add up along the parent chain.
 */
public final class BlockbenchExport {
	public static String bbmodel(final BodyPlan plan, final BufferedImage texture) {
		int w = texture.getWidth();
		int h = texture.getHeight();
		JsonObject root = new JsonObject();
		JsonObject meta = new JsonObject();
		meta.addProperty("format_version", "4.10");
		meta.addProperty("model_format", "modded_entity");
		meta.addProperty("box_uv", true);
		root.add("meta", meta);
		root.addProperty("name", plan.id);
		root.addProperty("model_identifier", plan.id);
		root.addProperty("modded_entity_version", "1.17");
		root.addProperty("modded_entity_flip_y", true);
		JsonObject resolution = new JsonObject();
		resolution.addProperty("width", w);
		resolution.addProperty("height", h);
		root.add("resolution", resolution);

		// absolute pivots in model space (rotations do not move stored coordinates in Blockbench)
		Map<String, float[]> pivot = new HashMap<>();
		Map<String, JsonObject> groups = new HashMap<>();
		JsonArray outliner = new JsonArray();
		for (Part part : plan.parts) {
			float[] parent = part.parent == null ? new float[3] : pivot.get(part.parent);
			float[] p = {parent[0] + part.px, parent[1] + part.py, parent[2] + part.pz};
			pivot.put(part.name, p);
			JsonObject group = new JsonObject();
			group.addProperty("name", part.name);
			group.add("origin", vec(-p[0], 24 - p[1], p[2]));
			if (part.xRot != 0 || part.yRot != 0 || part.zRot != 0) {
				group.add("rotation", vec((float)-Math.toDegrees(part.xRot), (float)-Math.toDegrees(part.yRot), (float)Math.toDegrees(part.zRot)));
			}
			group.addProperty("uuid", uuid(plan, "group/" + part.name));
			group.addProperty("export", true);
			group.addProperty("isOpen", false);
			group.addProperty("visibility", true);
			group.add("children", new JsonArray());
			groups.put(part.name, group);
			if (part.parent == null) {
				outliner.add(group);
			} else {
				groups.get(part.parent).getAsJsonArray("children").add(group);
			}
		}

		JsonArray elements = new JsonArray();
		List<PlacedBox> placed = plan.pack();
		for (PlacedBox pb : placed) {
			Part part = pb.part();
			BodyPlan.Box b = pb.box();
			float[] p = pivot.get(part.name);
			float x0 = p[0] + b.x();
			float y0 = p[1] + b.y();
			float z0 = p[2] + b.z();
			JsonObject cube = new JsonObject();
			String id = uuid(plan, "cube/" + part.name + "/" + pb.boxIndex());
			cube.addProperty("name", part.name + (pb.boxIndex() == 0 ? "" : "_" + pb.boxIndex()));
			cube.addProperty("box_uv", true);
			cube.addProperty("rescale", false);
			cube.add("from", vec(-(x0 + b.w()), 24 - (y0 + b.h()), z0));
			cube.add("to", vec(-x0, 24 - y0, z0 + b.d()));
			cube.addProperty("autouv", 0);
			cube.addProperty("color", 0);
			if (b.inflate() != 0) {
				cube.addProperty("inflate", b.inflate());
			}
			cube.add("origin", vec(-p[0], 24 - p[1], p[2]));
			JsonArray uv = new JsonArray();
			uv.add(pb.u());
			uv.add(pb.v());
			cube.add("uv_offset", uv);
			cube.add("faces", faces(pb.u(), pb.v(), b.w(), b.h(), b.d()));
			cube.addProperty("type", "cube");
			cube.addProperty("uuid", id);
			elements.add(cube);
			groups.get(part.name).getAsJsonArray("children").add(id);
		}
		root.add("elements", elements);
		root.add("outliner", outliner);

		JsonObject tex = new JsonObject();
		tex.addProperty("path", "");
		tex.addProperty("name", plan.id + ".png");
		tex.addProperty("folder", "");
		tex.addProperty("namespace", "");
		tex.addProperty("id", "0");
		tex.addProperty("width", w);
		tex.addProperty("height", h);
		tex.addProperty("uv_width", w);
		tex.addProperty("uv_height", h);
		tex.addProperty("particle", false);
		tex.addProperty("render_mode", "default");
		tex.addProperty("visible", true);
		tex.addProperty("internal", true);
		tex.addProperty("saved", false);
		tex.addProperty("uuid", uuid(plan, "texture"));
		tex.addProperty("source", "data:image/png;base64," + Base64.getEncoder().encodeToString(png(texture)));
		JsonArray textures = new JsonArray();
		textures.add(tex);
		root.add("textures", textures);
		return new GsonBuilder().setPrettyPrinting().create().toJson(root);
	}

	/**
	 * Box UV faces as Blockbench lays them out (vanilla's unwrap): north is the creature's front, east its right side (vanilla's -x),
	 * up and down mirrored.
	 */
	private static JsonObject faces(final int u, final int v, final int w, final int h, final int d) {
		JsonObject faces = new JsonObject();
		faces.add("north", face(u + d, v + d, u + d + w, v + d + h));
		faces.add("east", face(u, v + d, u + d, v + d + h));
		faces.add("south", face(u + d + w + d, v + d, u + d + w + d + w, v + d + h));
		faces.add("west", face(u + d + w, v + d, u + d + w + d, v + d + h));
		faces.add("up", face(u + d + w, v + d, u + d, v));
		faces.add("down", face(u + d + w + w, v, u + d + w, v + d));
		return faces;
	}

	private static JsonObject face(final float u0, final float v0, final float u1, final float v1) {
		JsonObject face = new JsonObject();
		JsonArray uv = new JsonArray();
		uv.add(u0);
		uv.add(v0);
		uv.add(u1);
		uv.add(v1);
		face.add("uv", uv);
		face.addProperty("texture", 0);
		return face;
	}

	private static JsonArray vec(final float x, final float y, final float z) {
		JsonArray a = new JsonArray();
		a.add(clean(x));
		a.add(clean(y));
		a.add(clean(z));
		return a;
	}

	/** Drops float noise (and -0) so the file diffs cleanly. */
	private static float clean(final float f) {
		float r = Math.round(f * 10000.0F) / 10000.0F;
		return r == 0.0F ? 0.0F : r;
	}

	/** Stable ids, so re-exporting the same plan gives the same file. */
	private static String uuid(final BodyPlan plan, final String key) {
		return UUID.nameUUIDFromBytes((plan.id + "/" + key).getBytes(StandardCharsets.UTF_8)).toString();
	}

	private static byte[] png(final BufferedImage image) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(image, "png", out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private BlockbenchExport() {
	}
}
