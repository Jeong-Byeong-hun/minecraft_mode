package com.minecraftmode.client.datagen.art;

import com.minecraftmode.client.creature.BodyPlan;
import com.minecraftmode.client.creature.BodyPlan.Part;
import com.minecraftmode.client.creature.BodyPlan.PlacedBox;
import com.minecraftmode.client.creature.Skin;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Paints a {@link BodyPlan} into its texture at datagen time: every box face gets the part's skin
 * pattern, per-face shading and a darker rim; heads get eyes. Glowing pixels (eyes, and the accent of
 * glowing skins) also go into a separate emissive texture. {@link #preview} draws a front and side
 * view for review.
 */
public final class CreaturePainter {
	public static final int TOP = 0;
	public static final int BOTTOM = 1;
	public static final int RIGHT = 2;
	public static final int FRONT = 3;
	public static final int LEFT = 4;
	public static final int BACK = 5;

	public record Painted(BufferedImage texture, BufferedImage glow) {
	}

	public static Painted paint(final BodyPlan plan) {
		int w = plan.textureWidth();
		int h = plan.textureHeight();
		BufferedImage tex = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		BufferedImage glow = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		int seed = plan.id.hashCode();
		for (PlacedBox p : plan.pack()) {
			Skin skin = plan.skin(p.part());
			int[][] faces = faces(p);
			for (int f = 0; f < 6; f++) {
				int fx = faces[f][0];
				int fy = faces[f][1];
				int fw = faces[f][2];
				int fh = faces[f][3];
				for (int y = 0; y < fh; y++) {
					for (int x = 0; x < fw; x++) {
						int px = fx + x;
						int py = fy + y;
						if (px >= w || py >= h) {
							continue;
						}
						Pixel pixel = pattern(skin, f, x, y, fw, fh, seed + p.part().name.hashCode() * 31 + p.boxIndex() * 7);
						float shade = switch (f) {
							case TOP -> 1.10F;
							case BOTTOM -> 0.72F;
							case BACK -> 0.88F;
							case LEFT, RIGHT -> 0.94F;
							default -> 1.0F;
						};
						boolean rim = fw > 2 && fh > 2 && (x == 0 || y == 0 || x == fw - 1 || y == fh - 1);
						if (rim && !pixel.glow) {
							shade *= 0.82F;
						}
						tex.setRGB(px, py, 0xFF000000 | scale(pixel.rgb, pixel.glow ? 1.0F : shade));
						if (pixel.glow && skin.glow()) {
							glow.setRGB(px, py, 0xFF000000 | pixel.rgb);
						}
					}
				}
			}
			if (p.boxIndex() == 0 && p.part().eyes > 0) {
				eyes(tex, glow, faces[FRONT], p.part());
			}
		}
		return new Painted(tex, glow);
	}

	/** The six face rectangles of a box's UV unwrap: {x, y, w, h}. */
	static int[][] faces(final PlacedBox p) {
		int u = p.u();
		int v = p.v();
		int w = p.box().w();
		int h = p.box().h();
		int d = p.box().d();
		return new int[][] {
			{u + d, v, w, d},
			{u + d + w, v, w, d},
			{u, v + d, d, h},
			{u + d, v + d, w, h},
			{u + d + w, v + d, d, h},
			{u + d + w + d, v + d, w, h},
		};
	}

	private static void eyes(final BufferedImage tex, final BufferedImage glow, final int[] face, final Part part) {
		int fx = face[0];
		int fy = face[1];
		int fw = face[2];
		int fh = face[3];
		int row = Math.max(1, Math.round(fh * 0.35F));
		int size = fw >= 10 ? 2 : 1;
		List<int[]> spots = new ArrayList<>();
		switch (part.eyes) {
			case 1 -> spots.add(new int[] {fw / 2 - (fw >= 6 ? 1 : 0), row, fw >= 6 ? 2 : 1, fw >= 6 ? 2 : 1});
			case 2 -> {
				int inset = Math.max(1, Math.round(fw * 0.22F));
				spots.add(new int[] {inset, row, size, 1});
				spots.add(new int[] {fw - inset - size, row, size, 1});
			}
			case 3 -> {
				spots.add(new int[] {fw / 2, row - 1, 1, 1});
				spots.add(new int[] {Math.max(1, fw / 4), row, 1, 1});
				spots.add(new int[] {fw - 1 - Math.max(1, fw / 4), row, 1, 1});
			}
			default -> {
				// many eyes in two rows (spiders)
				int perRow = Math.max(2, part.eyes / 2);
				for (int r = 0; r < 2; r++) {
					for (int i = 0; i < perRow; i++) {
						int x = 1 + Math.round(i * (fw - 3) / (float)Math.max(1, perRow - 1));
						spots.add(new int[] {x, row + r * 2 - 1, 1, 1});
					}
				}
			}
		}
		for (int[] s : spots) {
			for (int dy = 0; dy < s[3]; dy++) {
				for (int dx = 0; dx < s[2]; dx++) {
					int x = fx + Math.min(fw - 1, Math.max(0, s[0] + dx));
					int y = fy + Math.min(fh - 1, Math.max(0, s[1] + dy));
					if (x < tex.getWidth() && y < tex.getHeight()) {
						tex.setRGB(x, y, 0xFF000000 | part.eyeColor);
						glow.setRGB(x, y, 0xFF000000 | part.eyeColor);
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------ patterns

	private record Pixel(int rgb, boolean glow) {
	}

	private static Pixel pattern(final Skin skin, final int face, final int x, final int y, final int w, final int h, final int seed) {
		int base = skin.base();
		int accent = skin.accent();
		float n = noise(x, y, seed);
		return switch (skin.pattern()) {
			case SMOOTH -> new Pixel(scale(base, 0.94F + n * 0.12F), false);
			case FUR -> {
				float streak = noise(x, 0, seed + 5) * 0.18F + n * 0.08F;
				yield new Pixel(scale(base, 0.86F + streak + (face == BOTTOM ? -0.1F : 0.0F)), false);
			}
			case SCALES -> {
				boolean light = (x + (y % 2) * 1) % 2 == 0 && y % 2 == 0;
				boolean dark = (x + (y % 2)) % 2 == 1 && y % 2 == 1;
				yield new Pixel(scale(light ? mix(base, accent, 0.35F) : base, dark ? 0.8F : 0.95F + n * 0.08F), false);
			}
			case STONE -> {
				boolean crack = hash(x, y, seed) % 17 == 0 || (hash(x / 3, y / 3, seed) % 7 == 0 && (x + y) % 3 == 0);
				float blotch = noise(x / 2, y / 2, seed + 9);
				yield new Pixel(crack ? scale(base, 0.6F) : scale(mix(base, accent, blotch * 0.3F), 0.9F + n * 0.15F), false);
			}
			case METAL -> {
				boolean seam = x % 5 == 4 || y % 6 == 5;
				boolean rivet = x % 5 == 1 && y % 6 == 1;
				if (rivet) {
					yield new Pixel(accent, false);
				}
				float hl = y % 6 == 0 ? 1.15F : 1.0F;
				yield new Pixel(scale(base, seam ? 0.72F : hl * (0.95F + n * 0.06F)), false);
			}
			case CLOTH -> {
				boolean hem = face != TOP && face != BOTTOM && y >= h - 2;
				float fold = 0.92F + 0.1F * (float)Math.sin(x * 1.3 + seed % 7);
				yield new Pixel(hem ? accent : scale(base, fold + n * 0.05F), false);
			}
			case BONE -> {
				boolean seam = y % 3 == 2 && hash(x, y, seed) % 3 != 0;
				yield new Pixel(scale(base, seam ? 0.7F : 0.95F + n * 0.08F), false);
			}
			case CHITIN -> {
				int plate = y % 4;
				float gloss = plate == 0 ? 1.18F : plate == 3 ? 0.75F : 1.0F;
				yield new Pixel(scale(plate == 0 ? mix(base, accent, 0.3F) : base, gloss + n * 0.05F), false);
			}
			case CRYSTAL -> {
				int band = Math.floorMod(x + y, 6);
				boolean shine = band == 0;
				yield new Pixel(shine ? accent : scale(base, band < 3 ? 1.08F : 0.85F), shine);
			}
			case FLAME -> {
				float t = h <= 1 ? 1.0F : y / (float)(h - 1);
				int rgb = mix(base, accent, Math.min(1.0F, t * 0.9F + n * 0.3F));
				yield new Pixel(rgb, true);
			}
			case VOID -> {
				boolean star = hash(x, y, seed) % 23 == 0;
				yield new Pixel(star ? accent : scale(base, 0.85F + n * 0.25F), star);
			}
			case BARK -> {
				boolean grain = Math.floorMod(x + hash(0, y / 4, seed) % 3, 3) == 0;
				boolean knot = hash(x / 2, y / 2, seed) % 29 == 0;
				yield new Pixel(knot ? scale(base, 0.55F) : scale(base, grain ? 0.78F : 0.98F + n * 0.08F), false);
			}
			case CRACKED -> {
				boolean crack = (hash(x, y, seed) % 9 == 0) || (Math.floorMod(x - y + hash(0, y / 3, seed), 7) == 0);
				yield crack ? new Pixel(accent, true) : new Pixel(scale(base, 0.85F + n * 0.2F), false);
			}
			case FEATHER -> {
				boolean edge = Math.floorMod(y - Math.abs(x - w / 2), 3) == 0;
				yield new Pixel(edge ? scale(accent, 0.95F) : scale(base, 0.95F + n * 0.08F), false);
			}
			case SPOTS -> {
				boolean spot = face != BOTTOM && Math.floorMod(x * 3 + y * 5 + hash(x / 3, y / 3, seed) % 4, 11) == 0;
				yield new Pixel(spot ? accent : scale(base, 0.95F + n * 0.08F), false);
			}
		};
	}

	static int hash(final int x, final int y, final int seed) {
		int h = x * 374761393 + y * 668265263 + seed * 982451653;
		h = (h ^ (h >>> 13)) * 1274126177;
		return (h ^ (h >>> 16)) & 0x7FFFFFFF;
	}

	/** 0..1. */
	static float noise(final int x, final int y, final int seed) {
		return (hash(x, y, seed) % 1000) / 1000.0F;
	}

	static int scale(final int rgb, final float f) {
		int r = Math.max(0, Math.min(255, Math.round(((rgb >> 16) & 0xFF) * f)));
		int g = Math.max(0, Math.min(255, Math.round(((rgb >> 8) & 0xFF) * f)));
		int b = Math.max(0, Math.min(255, Math.round((rgb & 0xFF) * f)));
		return r << 16 | g << 8 | b;
	}

	static int mix(final int a, final int b, final float t) {
		float k = Math.max(0.0F, Math.min(1.0F, t));
		int r = Math.round(((a >> 16) & 0xFF) * (1 - k) + ((b >> 16) & 0xFF) * k);
		int g = Math.round(((a >> 8) & 0xFF) * (1 - k) + ((b >> 8) & 0xFF) * k);
		int bl = Math.round((a & 0xFF) * (1 - k) + (b & 0xFF) * k);
		return r << 16 | g << 8 | bl;
	}

	// ------------------------------------------------------------------ preview

	/**
	 * Front and side views of the plan (parts in their rest pose, rotations of chains included in a
	 * simplified way: only pivot offsets), drawn with the painted front/side faces.
	 */
	public static BufferedImage preview(final BodyPlan plan, final BufferedImage texture, final int cell) {
		BufferedImage out = new BufferedImage(cell * 2, cell + 14, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setColor(new Color(0x30, 0x30, 0x36));
		g.fillRect(0, 0, out.getWidth(), out.getHeight());
		g.dispose();
		Map<String, float[]> origin = new HashMap<>();
		List<float[]> boxesFront = new ArrayList<>();
		float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE, minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
		List<PlacedBox> placed = plan.pack();
		for (Part part : plan.parts) {
			float[] parent = part.parent == null ? new float[] {0, 0, 0} : origin.get(part.parent);
			float[] o = {parent[0] + part.px, parent[1] + part.py, parent[2] + part.pz};
			origin.put(part.name, o);
		}
		for (PlacedBox p : placed) {
			float[] o = origin.get(p.part().name);
			BodyPlan.Box b = p.box();
			float x0 = o[0] + b.x();
			float y0 = o[1] + b.y();
			float z0 = o[2] + b.z();
			minX = Math.min(minX, x0);
			maxX = Math.max(maxX, x0 + b.w());
			minY = Math.min(minY, y0);
			maxY = Math.max(maxY, y0 + b.h());
			minZ = Math.min(minZ, z0);
			maxZ = Math.max(maxZ, z0 + b.d());
			boxesFront.add(new float[] {x0, y0, z0, placed.indexOf(p)});
		}
		float span = Math.max(Math.max(maxX - minX, maxY - minY), maxZ - minZ);
		float s = (cell - 8) / Math.max(1.0F, span);
		// label with the unrotated size in blocks (width x height x length), to size hitboxes
		Graphics2D label = out.createGraphics();
		label.setColor(Color.WHITE);
		label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
		float k = plan.scale / 16.0F;
		label.drawString(String.format(java.util.Locale.ROOT, "%s x%.2f  %.1f x %.1f x %.1f", plan.id, plan.scale, (maxX - minX) * k, (maxY - minY) * k, (maxZ - minZ) * k), 4,
			cell + 11);
		label.dispose();
		// front view: x -> screen x (mirrored so the creature's right is on the left), y down; far boxes first
		boxesFront.sort(Comparator.comparingDouble(b -> -b[2]));
		for (float[] fb : boxesFront) {
			PlacedBox p = placed.get((int)fb[3]);
			int[] face = faces(p)[FRONT];
			blit(out, texture, face, 4 + Math.round((maxX - (fb[0] + p.box().w())) * s), 4 + Math.round((fb[1] - minY) * s), s, false);
		}
		// side view: z -> screen x, from the creature's right side
		boxesFront.sort(Comparator.comparingDouble(b -> b[0]));
		for (float[] fb : boxesFront) {
			PlacedBox p = placed.get((int)fb[3]);
			int[] face = faces(p)[RIGHT];
			blit(out, texture, face, cell + 4 + Math.round((fb[2] - minZ) * s), 4 + Math.round((fb[1] - minY) * s), s, false);
		}
		return out;
	}

	private static void blit(final BufferedImage out, final BufferedImage tex, final int[] face, final int ox, final int oy, final float s, final boolean flip) {
		int w = Math.max(1, Math.round(face[2] * s));
		int h = Math.max(1, Math.round(face[3] * s));
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int tx = face[0] + Math.min(face[2] - 1, (int)(x / s));
				int ty = face[1] + Math.min(face[3] - 1, (int)(y / s));
				if (tx < 0 || ty < 0 || tx >= tex.getWidth() || ty >= tex.getHeight()) {
					continue;
				}
				int argb = tex.getRGB(tx, ty);
				int px = ox + x;
				int py = oy + y;
				if ((argb >>> 24) > 0 && px >= 0 && py >= 0 && px < out.getWidth() && py < out.getHeight()) {
					out.setRGB(px, py, argb);
				}
			}
		}
	}

	private CreaturePainter() {
	}
}
