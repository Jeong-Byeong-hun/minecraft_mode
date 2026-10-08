package com.minecraftmode.client.datagen.art;

import com.minecraftmode.client.datagen.art.WeaponArtist.Canvas;
import com.minecraftmode.job.weapon.Archetype;

/**
 * Bow textures: limbs on a curve bulging to the upper left, grip in the middle, string between the
 * tips. Pulling frames 0-2 draw the string back toward the lower right with an arrow nocked.
 */
final class Bows {
	static void bow(final Canvas k, final Archetype type, final int t, final int v, final int frame) {
		double[] p0;
		double[] p2;
		double[] control;
		int thickness;
		switch (type) {
			case SHORTBOW -> {
				p0 = new double[] {3, 13};
				p2 = new double[] {13, 3};
				control = new double[] {2.5, 2.5};
				thickness = 1;
			}
			case GREATBOW -> {
				p0 = new double[] {1, 14};
				p2 = new double[] {14, 1};
				control = new double[] {-0.5, -0.5};
				thickness = 2;
			}
			default -> {
				p0 = new double[] {1, 15};
				p2 = new double[] {15, 1};
				control = new double[] {0.5, 0.5};
				thickness = 1;
			}
		}
		// limbs
		for (int i = 0; i <= 200; i++) {
			double s = i / 200.0;
			double x = (1 - s) * (1 - s) * p0[0] + 2 * (1 - s) * s * control[0] + s * s * p2[0];
			double y = (1 - s) * (1 - s) * p0[1] + 2 * (1 - s) * s * control[1] + s * s * p2[1];
			int px = (int)Math.round(x);
			int py = (int)Math.round(y);
			boolean grip = Math.abs(s - 0.5) < 0.09;
			boolean tip = s < 0.06 || s > 0.94;
			char ch;
			if (grip) {
				ch = t >= 3 && Math.abs(s - 0.5) < 0.03 ? '4' : 'M';
			} else if (tip && t >= 2) {
				ch = '3';
			} else if (t >= 4 && i % 24 < 3) {
				ch = '5';
			} else {
				ch = s < 0.5 ? 'l' : 'm';
			}
			k.put(px, py, ch);
			if (thickness > 1 || grip) {
				k.put(px + 1, py + 1, grip ? 'D' : 'd');
			}
		}
		k.outline();
		// string and arrow (not outlined, stay one pixel wide)
		if (frame < 0) {
			k.line((int)p0[0], (int)p0[1], (int)p2[0], (int)p2[1], 'H');
			return;
		}
		int pull = 1 + frame;
		int qx = (int)Math.round((p0[0] + p2[0]) / 2) + pull;
		int qy = (int)Math.round((p0[1] + p2[1]) / 2) + pull;
		k.line((int)p0[0], (int)p0[1], qx, qy, 'H');
		k.line(qx, qy, (int)p2[0], (int)p2[1], 'H');
		int len = 7 + frame;
		k.line(qx - 1, qy - 1, qx - len, qy - len, 'O');
		k.put(qx - len, qy - len, 'h');
		k.put(qx - len - 1, qy - len - 1, 'l');
		k.put(qx, qy - 1, '3');
		k.put(qx - 1, qy, '3');
	}

	private Bows() {
	}
}
