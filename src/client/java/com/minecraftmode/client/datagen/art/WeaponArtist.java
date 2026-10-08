package com.minecraftmode.client.datagen.art;

import com.minecraftmode.job.weapon.Archetype;
import com.minecraftmode.job.weapon.WeaponArt;
import com.minecraftmode.job.weapon.WeaponDef;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Draws the 16x16 texture of every class weapon from its {@link Archetype}, tier and
 * {@link WeaponArt} colors (no hand-made PNGs). Most shapes are laid out on the bottom-left to
 * top-right diagonal like vanilla tools: {@code a} runs along the weapon (handle low, tip high),
 * {@code c} across it (negative = upper-left side, which catches the light). Higher tiers add
 * fullers, gems and an accent rim. Bows also get three pulling frames.
 */
public final class WeaponArtist {
	static final int NONE = 0;
	static final int METAL = 1;
	static final int GRIP = 2;
	static final int ACCENT = 3;
	static final int WHITE = 4;

	/** Five tones from outline to highlight. */
	record Tones(int[] tone) {
		static Tones of(final int rgb) {
			return new Tones(new int[] {scale(rgb, 0.36F), scale(rgb, 0.66F), rgb, mix(rgb, 0xFFFFFF, 0.32F), mix(rgb, 0xFFFFFF, 0.65F)});
		}
	}

	static int scale(final int rgb, final float f) {
		int r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * f));
		int g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * f));
		int b = Math.min(255, Math.round((rgb & 0xFF) * f));
		return r << 16 | g << 8 | b;
	}

	static int mix(final int a, final int b, final float t) {
		int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
		int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
		int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
		return r << 16 | g << 8 | bl;
	}

	interface DiagShape {
		char at(int a, int c);
	}

	/** Pixel grid of (material, tone) with an automatic outline pass. */
	static final class Canvas {
		final int[][] material = new int[16][16];
		final int[][] tone = new int[16][16];

		boolean in(final int x, final int y) {
			return x >= 0 && y >= 0 && x < 16 && y < 16;
		}

		boolean filled(final int x, final int y) {
			return this.in(x, y) && this.material[y][x] != NONE;
		}

		void put(final int x, final int y, final char ch) {
			if (!this.in(x, y) || ch == '.') {
				return;
			}
			switch (ch) {
				case 'o', 'd', 'm', 'l', 'h' -> this.set(x, y, METAL, "odmlh".indexOf(ch));
				case 'O', 'D', 'M', 'L', 'H' -> this.set(x, y, GRIP, "ODMLH".indexOf(ch));
				case '1', '2', '3', '4', '5' -> this.set(x, y, ACCENT, ch - '1');
				case 'w' -> this.set(x, y, WHITE, 4);
				default -> throw new IllegalArgumentException("bad char " + ch);
			}
		}

		void set(final int x, final int y, final int mat, final int t) {
			if (this.in(x, y)) {
				this.material[y][x] = mat;
				this.tone[y][x] = t;
			}
		}

		void clear(final int x, final int y) {
			if (this.in(x, y)) {
				this.material[y][x] = NONE;
			}
		}

		void diag(final DiagShape shape) {
			for (int y = 0; y < 16; y++) {
				for (int x = 0; x < 16; x++) {
					this.put(x, y, shape.at(x - y, x + y - 15));
				}
			}
		}

		void rect(final int x0, final int y0, final int x1, final int y1, final char ch) {
			for (int y = y0; y <= y1; y++) {
				for (int x = x0; x <= x1; x++) {
					this.put(x, y, ch);
				}
			}
		}

		void line(int x0, int y0, final int x1, final int y1, final char ch) {
			int dx = Math.abs(x1 - x0);
			int dy = -Math.abs(y1 - y0);
			int sx = x0 < x1 ? 1 : -1;
			int sy = y0 < y1 ? 1 : -1;
			int err = dx + dy;
			while (true) {
				this.put(x0, y0, ch);
				if (x0 == x1 && y0 == y1) {
					break;
				}
				int e2 = 2 * err;
				if (e2 >= dy) {
					err += dy;
					x0 += sx;
				}
				if (e2 <= dx) {
					err += dx;
					y0 += sy;
				}
			}
		}

		/** Fills empty pixels next to the shape with the darkest tone of the touching material. */
		void outline() {
			int[][] mat = new int[16][16];
			for (int y = 0; y < 16; y++) {
				for (int x = 0; x < 16; x++) {
					if (this.material[y][x] != NONE) {
						continue;
					}
					int best = NONE;
					int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
					for (int[] d : dirs) {
						int nx = x + d[0];
						int ny = y + d[1];
						if (this.filled(nx, ny)) {
							int m = this.material[ny][nx];
							int rank = m == METAL || m == WHITE ? 3 : m == ACCENT ? 2 : 1;
							int bestRank = best == METAL ? 3 : best == ACCENT ? 2 : best == GRIP ? 1 : 0;
							if (rank > bestRank) {
								best = m == WHITE ? METAL : m;
							}
						}
					}
					mat[y][x] = best;
				}
			}
			for (int y = 0; y < 16; y++) {
				for (int x = 0; x < 16; x++) {
					if (mat[y][x] != NONE) {
						this.set(x, y, mat[y][x], 0);
					}
				}
			}
		}

		BufferedImage render(final Tones metal, final Tones grip, final Tones accent) {
			BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < 16; y++) {
				for (int x = 0; x < 16; x++) {
					int rgb = switch (this.material[y][x]) {
						case METAL -> metal.tone[this.tone[y][x]];
						case GRIP -> grip.tone[this.tone[y][x]];
						case ACCENT -> accent.tone[this.tone[y][x]];
						case WHITE -> 0xFFFFFF;
						default -> -1;
					};
					if (rgb != -1) {
						img.setRGB(x, y, 0xFF000000 | rgb);
					}
				}
			}
			return img;
		}
	}

	/** Texture name suffix ("" or "_pulling_N") -> image. */
	public static Map<String, BufferedImage> draw(final WeaponDef def) {
		WeaponArt art = def.art();
		Tones metal = Tones.of(art.metal());
		Tones grip = Tones.of(art.grip());
		Tones accent = Tones.of(art.accent());
		Map<String, BufferedImage> out = new LinkedHashMap<>();
		int t = def.tier();
		int v = Math.floorMod(def.id().hashCode(), 3);
		if (def.archetype().mode() == Archetype.BasicMode.DRAW) {
			for (int frame = -1; frame < 3; frame++) {
				Canvas canvas = new Canvas();
				Bows.bow(canvas, def.archetype(), t, v, frame);
				out.put(frame < 0 ? "" : "_pulling_" + frame, canvas.render(metal, grip, accent));
			}
			return out;
		}
		Canvas canvas = new Canvas();
		Shapes.draw(canvas, def.archetype(), t, v);
		out.put("", canvas.render(metal, grip, accent));
		return out;
	}

	private WeaponArtist() {
	}
}
