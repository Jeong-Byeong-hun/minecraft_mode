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
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;

/**
 * Paints a {@link BodyPlan} into its texture at datagen time: every box face gets the part's skin
 * pattern, then light: per-face shading, a whole-body gradient (light from above, in model space so stacked
 * boxes read as one body), a soft bevel (bright top edge, dark bottom edge) and ambient occlusion where parts
 * meet, computed from the rest pose. Darkening shifts hue toward cool violet and brightening toward warm
 * yellow instead of plain RGB scaling, so shadows stay saturated. Heads get eyes with brow shadows (and a
 * mouth on bare faces). Finally every pixel snaps to its skin's palette: a few hue-shifted steps of the base, the
 * accent and their blend, the way a pixel artist works from a limited ramp instead of a smooth gradient. Glowing pixels (eyes, and the accent of
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
		Geometry geo = new Geometry(plan);
		// which skin's palette each pixel snaps to (index + 1); 0 for glowing pixels, which keep their color
		int[] owner = new int[w * h];
		List<Skin> skinList = new ArrayList<>(plan.skins.values());
		List<int[]> palettes = new ArrayList<>();
		for (Skin s : skinList) {
			palettes.add(palette(s));
		}
		List<Runnable> eyeJobs = new ArrayList<>();
		float gradTop = plan.shaded ? 1.10F : 1.06F;
		float gradSpan = plan.shaded ? 0.20F : 0.12F;
		for (PlacedBox p : plan.pack()) {
			Skin skin = plan.skin(p.part());
			int skinIndex = skinList.indexOf(skin);
			WorldBox wb = geo.box(p);
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
						if (pixel.glow) {
							tex.setRGB(px, py, 0xFF000000 | pixel.rgb);
							if (skin.glow()) {
								glow.setRGB(px, py, 0xFF000000 | pixel.rgb);
							}
							continue;
						}
						float[] pos = wb.surface(f, (x + 0.5F) / fw, (y + 0.5F) / fh);
						// whole-body light from above: the head is lit, the feet sit in shade
						float light = gradTop - gradSpan * geo.height(pos[1]);
						light *= faceLight(plan.shaded, f, x, y, fw, fh);
						// contact shadow where this box meets another (neck, armpits, under belts)
						light *= 1.0F - 0.4F * geo.occlusion(wb, pos);
						tex.setRGB(px, py, 0xFF000000 | scale(pixel.rgb, light));
						owner[py * w + px] = skinIndex + 1;
					}
				}
			}
			if (p.boxIndex() == 0 && p.part().eyes > 0) {
				int[] front = faces[FRONT];
				int[] pal = palettes.get(skinIndex);
				WorldBox head = wb;
				// after the palette pass, so the brows snap to the same palette and the eyes stay exact
				eyeJobs.add(() -> eyes(geo, head, skin, pal, tex, glow, front, p.part()));
			}
		}
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int o = owner[y * w + x];
				if (o > 0) {
					tex.setRGB(x, y, 0xFF000000 | nearest(palettes.get(o - 1), tex.getRGB(x, y) & 0xFFFFFF));
				}
			}
		}
		eyeJobs.forEach(Runnable::run);
		return new Painted(tex, glow);
	}

	/** Light of one pixel within its box: the face's direction, a per-box falloff and a soft bevel. */
	private static float faceLight(final boolean shaded, final int f, final int x, final int y, final int fw, final int fh) {
		boolean side = f != TOP && f != BOTTOM;
		float light = switch (f) {
			case TOP -> 1.10F;
			case BOTTOM -> 0.70F;
			case BACK -> 0.86F;
			case LEFT, RIGHT -> 0.93F;
			default -> 1.0F;
		};
		if (shaded && side && fh > 1) {
			// a little per-box falloff on top so each box still reads as a volume
			light *= 1.03F - 0.07F * y / (fh - 1);
		}
		// soft bevel instead of a dark rim on every edge (which reads as Lego bricks)
		if (fw > 2 && fh > 2) {
			if (side && y == 0) {
				light *= 1.07F;
			} else if (side && y == fh - 1) {
				light *= 0.88F;
			} else if (x == 0 || x == fw - 1 || (!side && (y == 0 || y == fh - 1))) {
				light *= 0.93F;
			}
		}
		return light;
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

	private static void eyes(final Geometry geo, final WorldBox head, final Skin skin, final int[] pal, final BufferedImage tex, final BufferedImage glow,
		final int[] face, final Part part) {
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
		if (part.eyes <= 2) {
			// brow shadow above each eye and a softer one below, so the eyes sit in sockets; the darkest
			// wins where two eyes' shadows meet, so close eyes do not get a doubled shadow between them
			Map<Integer, Float> shadow = new HashMap<>();
			for (int[] s : spots) {
				for (int dx = -1; dx <= s[2]; dx++) {
					if (s[0] + dx < 0 || s[0] + dx >= fw) {
						continue;
					}
					boolean edge = dx < 0 || dx == s[2];
					shadow.merge((s[1] - 1) * fw + s[0] + dx, edge ? 0.86F : 0.72F, Math::min);
					shadow.merge((s[1] + s[3]) * fw + s[0] + dx, edge ? 1.0F : 0.88F, Math::min);
				}
			}
			shadow.forEach((at, f) -> darken(pal, tex, fx, fy, fw, fh, Math.floorMod(at, fw), Math.floorDiv(at, fw), f));
			int mouthY = Math.min(fh - 2, Math.round(fh * 0.72F));
			int mw = Math.max(2, fw / 4);
			int mx = (fw - mw) / 2;
			// a mouth only on a bare face: never where a jaw, beak or snout sits in front of it
			if (part.eyes == 2 && fw >= 6 && fh >= 6 && skin.pattern() == Skin.Pattern.SMOOTH
				&& !geo.covered(head, head.surface(FRONT, (mx + mw * 0.5F) / fw, (mouthY + 0.5F) / fh))) {
				for (int i = 0; i < mw; i++) {
					darken(pal, tex, fx, fy, fw, fh, mx + i, mouthY, 0.62F);
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

	private static void darken(final int[] pal, final BufferedImage tex, final int fx, final int fy, final int fw, final int fh, final int x, final int y, final float f) {
		if (f >= 1.0F || x < 0 || y < 0 || x >= fw || y >= fh || fx + x >= tex.getWidth() || fy + y >= tex.getHeight()) {
			return;
		}
		int rgb = tex.getRGB(fx + x, fy + y);
		tex.setRGB(fx + x, fy + y, 0xFF000000 | nearest(pal, scale(rgb & 0xFFFFFF, f)));
	}

	// ------------------------------------------------------------------ patterns

	private record Pixel(int rgb, boolean glow) {
	}

	private static Pixel pattern(final Skin skin, final int face, final int x, final int y, final int w, final int h, final int seed) {
		int base = skin.base();
		int accent = skin.accent();
		float n = clustered(x, y, seed);
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
				// plates sized to the face: seams split it evenly (no seam on a small face), a rivet in each plate's corner
				boolean seam = seam(x, w, 5) || seam(y, h, 6);
				boolean rivet = w >= 4 && h >= 4 && seam(x - 2, w, 5, true) && seam(y - 2, h, 6, true);
				if (rivet) {
					yield new Pixel(accent, false);
				}
				float hl = seam(y - 1, h, 6, true) ? 1.15F : 1.0F;
				yield new Pixel(scale(base, seam ? 0.72F : hl * (0.95F + n * 0.06F)), false);
			}
			case CLOTH -> {
				boolean hem = face != TOP && face != BOTTOM && y >= h - 2;
				float fold = 0.92F + 0.1F * (float)Math.sin(x * 1.3 + seed % 7);
				yield new Pixel(hem ? accent : scale(base, fold + n * 0.05F), false);
			}
			case BONE -> {
				boolean seam = seam(y, h, 3) && hash(x, y, seed) % 3 != 0;
				yield new Pixel(scale(base, seam ? 0.7F : 0.95F + n * 0.08F), false);
			}
			case CHITIN -> {
				// plate edges spread evenly over the face: a glossy top row, a dark bottom row
				boolean top = seam(y - 1, h, 4, true);
				boolean bottom = seam(y, h, 4) || y == h - 1;
				float gloss = top ? 1.18F : bottom ? 0.75F : 1.0F;
				yield new Pixel(scale(top ? mix(base, accent, 0.3F) : base, gloss + n * 0.05F), false);
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
			case COAT -> {
				float streak = noise(x, 0, seed + 5) * 0.14F + n * 0.06F;
				float belly = switch (face) {
					case TOP -> 0.0F;
					case BOTTOM -> 1.0F;
					default -> h <= 1 ? 0.5F : Math.min(1.0F, y / (float)(h - 1) * 1.3F);
				};
				yield new Pixel(scale(mix(accent, base, belly), 0.9F + streak), false);
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

	/**
	 * 0..1, in clumps: mostly one value per 2x2 cell (cells offset every other row so they do not line up into a grid),
	 * with a little per-pixel variation. Pixel artists texture with clusters; per-pixel noise reads as static.
	 */
	static float clustered(final int x, final int y, final int seed) {
		int cy = Math.floorDiv(y, 2);
		int cx = Math.floorDiv(x + (cy & 1), 2);
		return noise(cx, cy, seed + 101) * 0.75F + noise(x, y, seed) * 0.25F;
	}

	/** Whether {@code i} is the last pixel of a section when {@code size} pixels split into sections of about {@code target}. */
	static boolean seam(final int i, final int size, final int target) {
		return seam(i, size, target, false);
	}

	/**
	 * With {@code edge} false: the last pixel of each section except the final one (a seam between sections; none when the face fits
	 * one section). With {@code edge} true: the last pixel of every section, also the final one and -1 (so {@code i - k} tests "k
	 * pixels into a section").
	 */
	static boolean seam(final int i, final int size, final int target, final boolean edge) {
		int sections = Math.max(1, Math.round(size / (float)target));
		if (i == -1) {
			return edge;
		}
		if (i < 0 || i >= size) {
			return false;
		}
		if (i == size - 1) {
			return edge;
		}
		return (i + 1) * sections / size != i * sections / size;
	}

	/** A skin's palette: hue-shifted steps of the base, the accent, and the blend some patterns use. */
	static int[] palette(final Skin skin) {
		float[] steps = {0.46F, 0.6F, 0.74F, 0.88F, 1.02F, 1.16F};
		int[] ramps = {skin.base(), skin.accent(), mix(skin.base(), skin.accent(), 0.35F)};
		int[] out = new int[steps.length * ramps.length];
		int k = 0;
		for (int ramp : ramps) {
			for (float f : steps) {
				out[k++] = scale(ramp, f);
			}
		}
		return out;
	}

	/** The palette color closest to {@code rgb} (in CIE Lab, so the eye's sense of "close" decides). */
	static int nearest(final int[] palette, final int rgb) {
		float[] lab = lab(rgb);
		int best = rgb;
		float bestD = Float.MAX_VALUE;
		for (int c : palette) {
			float[] o = lab(c);
			float d = (o[0] - lab[0]) * (o[0] - lab[0]) + (o[1] - lab[1]) * (o[1] - lab[1]) + (o[2] - lab[2]) * (o[2] - lab[2]);
			if (d < bestD) {
				bestD = d;
				best = c;
			}
		}
		return best;
	}

	/** Lab of every color seen; datagen only, bounded by the colors the plans produce. */
	private static final Map<Integer, float[]> LAB = new ConcurrentHashMap<>();

	private static float[] lab(final int rgb) {
		return LAB.computeIfAbsent(rgb & 0xFFFFFF, c -> {
			double r = lin(((c >> 16) & 0xFF) / 255.0);
			double g = lin(((c >> 8) & 0xFF) / 255.0);
			double b = lin((c & 0xFF) / 255.0);
			double x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / 0.95047;
			double y = 0.2126 * r + 0.7152 * g + 0.0722 * b;
			double z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / 1.08883;
			double fx = labF(x);
			double fy = labF(y);
			double fz = labF(z);
			return new float[] {(float)(116 * fy - 16), (float)(500 * (fx - fy)), (float)(200 * (fy - fz))};
		});
	}

	private static double lin(final double c) {
		return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
	}

	private static double labF(final double t) {
		return t > 0.008856 ? Math.cbrt(t) : 7.787 * t + 16.0 / 116.0;
	}

	/**
	 * Brightens or darkens a color like a pixel artist would: darker tones drift toward cool violet and
	 * gain saturation, brighter tones drift toward warm yellow and lose a little saturation. Plain RGB
	 * scaling turns every shadow muddy brown-grey.
	 */
	static int scale(final int rgb, final float f) {
		if (f == 1.0F) {
			return rgb & 0xFFFFFF;
		}
		float[] hsb = Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
		float hue = hsb[0];
		float sat = hsb[1];
		float bri = hsb[2] * f;
		if (f < 1.0F) {
			float k = Math.min(1.0F, 1.0F - f);
			// the hue only matters once there is some color; greys pick up just a hint of blue
			hue = towardHue(hue, 0.70F, k * 0.22F * Math.min(1.0F, sat * 2.5F));
			if (sat < 0.08F) {
				hue = 0.66F;
			}
			sat = Math.min(1.0F, sat + k * (sat < 0.08F ? 0.06F : 0.12F));
		} else {
			float k = Math.min(1.0F, f - 1.0F);
			hue = towardHue(hue, 0.14F, k * 0.45F * Math.min(1.0F, sat * 2.5F));
			sat = Math.max(0.0F, sat * (1.0F - k * 0.6F));
			if (bri > 1.0F) {
				// past full brightness, keep lightening by washing toward white
				sat = Math.max(0.0F, sat - (bri - 1.0F));
				bri = 1.0F;
			}
		}
		return Color.HSBtoRGB(hue, sat, Math.max(0.0F, Math.min(1.0F, bri))) & 0xFFFFFF;
	}

	private static float towardHue(final float hue, final float target, final float amount) {
		float d = target - hue;
		if (d > 0.5F) {
			d -= 1.0F;
		} else if (d < -0.5F) {
			d += 1.0F;
		}
		float out = hue + d * amount;
		return out - (float)Math.floor(out);
	}

	static int mix(final int a, final int b, final float t) {
		float k = Math.max(0.0F, Math.min(1.0F, t));
		int r = Math.round(((a >> 16) & 0xFF) * (1 - k) + ((b >> 16) & 0xFF) * k);
		int g = Math.round(((a >> 8) & 0xFF) * (1 - k) + ((b >> 8) & 0xFF) * k);
		int bl = Math.round((a & 0xFF) * (1 - k) + (b & 0xFF) * k);
		return r << 16 | g << 8 | bl;
	}

	// ------------------------------------------------------------------ geometry (for light and occlusion)

	/** A box placed in model space: rotation {@code r} (row-major 3x3), translation {@code t}, local bounds. */
	private static final class WorldBox {
		final float[] r;
		final float[] t;
		final float x0, y0, z0, x1, y1, z1;

		WorldBox(final float[] r, final float[] t, final BodyPlan.Box b) {
			this.r = r;
			this.t = t;
			float g = b.inflate();
			this.x0 = b.x() - g;
			this.y0 = b.y() - g;
			this.z0 = b.z() - g;
			this.x1 = b.x() + b.w() + g;
			this.y1 = b.y() + b.h() + g;
			this.z1 = b.z() + b.d() + g;
		}

		/**
		 * The model-space point and outward normal at (u, v) in 0..1 across a face, following vanilla's cube
		 * unwrap: right | front | left | back wrap around the box, the top's lower edge meets the front.
		 * Returns {x, y, z, nx, ny, nz}.
		 */
		float[] surface(final int face, final float u, final float v) {
			float[] l = switch (face) {
				case TOP -> new float[] {lerp(this.x0, this.x1, u), this.y0, lerp(this.z1, this.z0, v), 0, -1, 0};
				case BOTTOM -> new float[] {lerp(this.x0, this.x1, u), this.y1, lerp(this.z0, this.z1, v), 0, 1, 0};
				case RIGHT -> new float[] {this.x0, lerp(this.y0, this.y1, v), lerp(this.z1, this.z0, u), -1, 0, 0};
				case FRONT -> new float[] {lerp(this.x0, this.x1, u), lerp(this.y0, this.y1, v), this.z0, 0, 0, -1};
				case LEFT -> new float[] {this.x1, lerp(this.y0, this.y1, v), lerp(this.z0, this.z1, u), 1, 0, 0};
				default -> new float[] {lerp(this.x1, this.x0, u), lerp(this.y0, this.y1, v), this.z1, 0, 0, 1};
			};
			float[] p = apply(this.r, l[0], l[1], l[2]);
			float[] n = apply(this.r, l[3], l[4], l[5]);
			return new float[] {p[0] + this.t[0], p[1] + this.t[1], p[2] + this.t[2], n[0], n[1], n[2]};
		}

		boolean contains(final float x, final float y, final float z) {
			float dx = x - this.t[0];
			float dy = y - this.t[1];
			float dz = z - this.t[2];
			// inverse rotation = transpose
			float lx = this.r[0] * dx + this.r[3] * dy + this.r[6] * dz;
			float ly = this.r[1] * dx + this.r[4] * dy + this.r[7] * dz;
			float lz = this.r[2] * dx + this.r[5] * dy + this.r[8] * dz;
			return lx > this.x0 && lx < this.x1 && ly > this.y0 && ly < this.y1 && lz > this.z0 && lz < this.z1;
		}
	}

	/** Every box of a plan in model space (rest pose, part rotations applied like vanilla's ZYX). */
	private static final class Geometry {
		private final Map<PlacedBox, WorldBox> boxes = new HashMap<>();
		private final List<WorldBox> all = new ArrayList<>();
		private final float minY;
		private final float maxY;

		Geometry(final BodyPlan plan) {
			Map<String, float[][]> pose = new HashMap<>();
			for (Part part : plan.parts) {
				float @Nullable [][] parent = part.parent == null ? null : pose.get(part.parent);
				float[] pr = parent == null ? identity() : parent[0];
				float[] pt = parent == null ? new float[3] : parent[1];
				float[] off = apply(pr, part.px, part.py, part.pz);
				float[] r = mul(pr, rotationZYX(part.zRot, part.yRot, part.xRot));
				pose.put(part.name, new float[][] {r, {pt[0] + off[0], pt[1] + off[1], pt[2] + off[2]}});
			}
			float lo = Float.MAX_VALUE;
			float hi = -Float.MAX_VALUE;
			for (PlacedBox p : plan.pack()) {
				float[][] rt = pose.get(p.part().name);
				WorldBox wb = new WorldBox(rt[0], rt[1], p.box());
				this.boxes.put(p, wb);
				this.all.add(wb);
				for (int c = 0; c < 8; c++) {
					float[] q = apply(wb.r, (c & 1) == 0 ? wb.x0 : wb.x1, (c & 2) == 0 ? wb.y0 : wb.y1, (c & 4) == 0 ? wb.z0 : wb.z1);
					lo = Math.min(lo, q[1] + wb.t[1]);
					hi = Math.max(hi, q[1] + wb.t[1]);
				}
			}
			this.minY = lo;
			this.maxY = hi;
		}

		WorldBox box(final PlacedBox p) {
			return this.boxes.get(p);
		}

		/** 0 at the top of the body, 1 at the bottom. */
		float height(final float y) {
			return this.maxY > this.minY ? Math.max(0.0F, Math.min(1.0F, (y - this.minY) / (this.maxY - this.minY))) : 0.5F;
		}

		/** Whether another box sits right in front of this surface point (within 2 units along its normal). */
		boolean covered(final WorldBox self, final float[] s) {
			for (float dist : new float[] {0.5F, 1.5F}) {
				float x = s[0] + s[3] * dist;
				float y = s[1] + s[4] * dist;
				float z = s[2] + s[5] * dist;
				for (WorldBox other : this.all) {
					if (other != self && other.contains(x, y, z)) {
						return true;
					}
				}
			}
			return false;
		}

		/** 0..1: how much of the space just outside this surface point is filled by other boxes. */
		float occlusion(final WorldBox self, final float[] s) {
			float nx = s[3];
			float ny = s[4];
			float nz = s[5];
			// two tangents perpendicular to the normal
			float[] t1 = Math.abs(ny) > 0.9F ? cross(nx, ny, nz, 1, 0, 0) : cross(nx, ny, nz, 0, 1, 0);
			float[] t2 = cross(nx, ny, nz, t1[0], t1[1], t1[2]);
			float hit = 0.0F;
			float total = 0.0F;
			float[][] dirs = {
				{nx, ny, nz},
				{nx + 0.8F * t1[0], ny + 0.8F * t1[1], nz + 0.8F * t1[2]},
				{nx - 0.8F * t1[0], ny - 0.8F * t1[1], nz - 0.8F * t1[2]},
				{nx + 0.8F * t2[0], ny + 0.8F * t2[1], nz + 0.8F * t2[2]},
				{nx - 0.8F * t2[0], ny - 0.8F * t2[1], nz - 0.8F * t2[2]},
			};
			for (float dist : new float[] {1.0F, 2.5F}) {
				float weight = dist < 2.0F ? 1.0F : 0.6F;
				for (float[] d : dirs) {
					float x = s[0] + nx * 0.05F + d[0] * dist;
					float y = s[1] + ny * 0.05F + d[1] * dist;
					float z = s[2] + nz * 0.05F + d[2] * dist;
					total += weight;
					for (WorldBox other : this.all) {
						if (other != self && other.contains(x, y, z)) {
							hit += weight;
							break;
						}
					}
				}
			}
			return hit / total;
		}
	}

	private static float lerp(final float a, final float b, final float t) {
		return a + (b - a) * t;
	}

	private static float[] identity() {
		return new float[] {1, 0, 0, 0, 1, 0, 0, 0, 1};
	}

	private static float[] apply(final float[] m, final float x, final float y, final float z) {
		return new float[] {m[0] * x + m[1] * y + m[2] * z, m[3] * x + m[4] * y + m[5] * z, m[6] * x + m[7] * y + m[8] * z};
	}

	private static float[] mul(final float[] a, final float[] b) {
		float[] o = new float[9];
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				o[i * 3 + j] = a[i * 3] * b[j] + a[i * 3 + 1] * b[3 + j] + a[i * 3 + 2] * b[6 + j];
			}
		}
		return o;
	}

	/** Rz * Ry * Rx, the order vanilla's PartPose applies. */
	private static float[] rotationZYX(final float z, final float y, final float x) {
		float cz = (float)Math.cos(z), sz = (float)Math.sin(z);
		float cy = (float)Math.cos(y), sy = (float)Math.sin(y);
		float cx = (float)Math.cos(x), sx = (float)Math.sin(x);
		float[] rz = {cz, -sz, 0, sz, cz, 0, 0, 0, 1};
		float[] ry = {cy, 0, sy, 0, 1, 0, -sy, 0, cy};
		float[] rx = {1, 0, 0, 0, cx, -sx, 0, sx, cx};
		return mul(mul(rz, ry), rx);
	}

	private static float[] cross(final float ax, final float ay, final float az, final float bx, final float by, final float bz) {
		float x = ay * bz - az * by;
		float y = az * bx - ax * bz;
		float z = ax * by - ay * bx;
		float len = (float)Math.sqrt(x * x + y * y + z * z);
		return len < 1.0E-6F ? new float[] {1, 0, 0} : new float[] {x / len, y / len, z / len};
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
		// side view: z -> screen x, from the creature's right side (min x is nearest, so it is drawn last)
		boxesFront.sort(Comparator.comparingDouble(b -> -b[0]));
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
