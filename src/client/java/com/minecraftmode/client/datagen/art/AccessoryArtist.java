package com.minecraftmode.client.datagen.art;

import com.minecraftmode.accessory.AccessoryDef;
import com.minecraftmode.client.datagen.art.WeaponArtist.Canvas;
import com.minecraftmode.client.datagen.art.WeaponArtist.Tones;
import java.awt.image.BufferedImage;

/**
 * Draws the 16x16 icon of every raid accessory from its slot (one shape each) and its set's colours: metal for chains, bands and
 * settings, the set's gem for stones, a dark cord (cream paper for talismans). Same pixel conventions as {@link Shapes}.
 */
public final class AccessoryArtist {
	private static final int CORD = 0x4A3426;
	private static final int PAPER = 0xEDE3C8;
	private static final int ORB_METAL = 0xE0B23A;
	private static final int ORB_GEM = 0xB070FF;

	public static BufferedImage draw(final AccessoryDef def) {
		Canvas k = new Canvas();
		switch (def.slot()) {
			case NECKLACE -> necklace(k);
			case EARRING -> earring(k);
			case RING -> ring(k);
			case BRACELET -> bracelet(k);
			case TALISMAN -> talisman(k);
		}
		k.outline();
		int grip = switch (def.slot()) {
			case TALISMAN -> PAPER;
			default -> CORD;
		};
		return k.render(Tones.of(def.set().metal()), Tones.of(grip), Tones.of(def.set().gem()));
	}

	/** The accessory orb (enhancement material): a violet sphere lit on its upper left, a glint and a small gold cradle. */
	public static BufferedImage orb() {
		Canvas k = new Canvas();
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double dx = x - 7.5;
				double dy = y - 6.5;
				double d = Math.hypot(dx, dy);
				if (d <= 5.4) {
					double shade = (dx + dy) / 5.4;
					k.put(x, y, d > 4.6 ? '2' : shade < -0.9 ? '5' : shade < -0.1 ? '4' : shade < 0.7 ? '3' : '2');
				}
			}
		}
		k.put(5, 3, 'w');
		k.put(6, 3, 'w');
		k.put(5, 4, 'w');
		k.rect(4, 12, 11, 12, 'l');
		k.rect(5, 13, 10, 13, 'd');
		k.put(3, 11, 'm');
		k.put(12, 11, 'd');
		k.outline();
		return k.render(Tones.of(ORB_METAL), Tones.of(CORD), Tones.of(ORB_GEM));
	}

	/** A cut stone: a diamond of radius {@code r} lit on its upper left. */
	private static void gem(final Canvas k, final double cx, final double cy, final double r) {
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double dx = x - cx;
				double dy = y - cy;
				double d = Math.abs(dx) + Math.abs(dy);
				if (d <= r) {
					k.put(x, y, d > r - 0.8 ? '2' : dx + dy < -0.5 ? '5' : dx + dy < 0.8 ? '4' : '3');
				}
			}
		}
	}

	private static void necklace(final Canvas k) {
		int lastY = -1;
		for (int x = 1; x <= 14; x++) {
			double t = (x - 7.5) / 6.5;
			int y = (int)Math.round(1 + 8.5 * (1 - t * t));
			if (lastY >= 0) {
				k.line(x - 1, lastY, x, y, 'm');
			}
			k.put(x, y, x % 2 == 0 ? 'l' : 'd');
			lastY = y;
		}
		k.put(7, 10, 'h');
		k.put(8, 10, 'm');
		gem(k, 7.5, 13.0, 2.6);
	}

	private static void earring(final Canvas k) {
		// hook
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double d = Math.hypot(x - 7.5, y - 3.0);
				if (d >= 1.4 && d <= 2.4 && (y <= 3 || x >= 8)) {
					k.put(x, y, x < 8 ? 'l' : 'd');
				}
			}
		}
		k.rect(7, 5, 8, 6, 'm');
		k.put(7, 7, 'h');
		k.put(8, 7, 'd');
		// drop
		for (int y = 7; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double dx = x - 7.5;
				double r = y < 10 ? (y - 7) * 0.9 : 3.2 - Math.max(0, y - 12) * 0.9;
				if (Math.abs(dx) <= r && y >= 8) {
					double shade = dx + (y - 11) * 0.7;
					k.put(x, y, Math.abs(dx) > r - 0.9 ? '2' : shade < -1.5 ? '5' : shade < 0.5 ? '4' : '3');
				}
			}
		}
	}

	private static void ring(final Canvas k) {
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double dx = (x - 7.5) / 5.6;
				double dy = (y - 10.0) / 4.4;
				double d = Math.sqrt(dx * dx + dy * dy);
				if (d <= 1.0 && d >= 0.62) {
					k.put(x, y, dy < 0 ? (dx < 0 ? 'h' : 'l') : dx < 0 ? 'm' : 'd');
				}
			}
		}
		k.rect(6, 6, 9, 6, 'm');
		gem(k, 7.5, 3.5, 2.6);
	}

	private static void bracelet(final Canvas k) {
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double dx = (x - 7.5) / 7.0;
				double dy = (y - 8.0) / 5.6;
				double d = Math.sqrt(dx * dx + dy * dy);
				if (d <= 1.0 && d >= 0.58) {
					boolean front = dy > 0;
					k.put(x, y, front ? (d > 0.86 ? 'd' : 'l') : (d > 0.86 ? 'm' : 'd'));
				}
			}
		}
		for (int[] p : new int[][] {{3, 11}, {7, 13}, {8, 13}, {12, 11}}) {
			k.put(p[0], p[1], '4');
		}
		k.put(7, 12, '5');
	}

	private static void talisman(final Canvas k) {
		k.rect(4, 2, 11, 13, 'L');
		k.rect(5, 3, 10, 12, 'H');
		k.rect(4, 2, 11, 2, 'm');
		k.rect(4, 13, 11, 13, 'm');
		// the script
		for (int y = 4; y <= 11; y++) {
			k.put(7, y, y % 3 == 0 ? '4' : '3');
		}
		k.rect(6, 5, 9, 5, '3');
		k.rect(5, 8, 10, 8, '2');
		k.put(8, 10, '3');
		k.put(6, 10, '3');
		// tassel
		k.put(7, 14, 'd');
		k.put(8, 14, 'd');
		k.put(7, 15, '3');
		k.put(8, 15, '2');
	}

	private AccessoryArtist() {
	}
}
