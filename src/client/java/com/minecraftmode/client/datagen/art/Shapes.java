package com.minecraftmode.client.datagen.art;

import com.minecraftmode.client.datagen.art.WeaponArtist.Canvas;
import com.minecraftmode.job.weapon.Archetype;

/**
 * One drawing per archetype. Diagonal shapes use (a, c): a along the weapon, c across it; a + c is
 * always odd, so a one-pixel line uses c == 0 with odd a. Chars: metal o d m l h, grip O D M L H,
 * accent 1-5, white w. {@code t} = tier 1..4, {@code v} = small per-weapon variation 0..2.
 */
final class Shapes {
	static void draw(final Canvas k, final Archetype type, final int t, final int v) {
		switch (type) {
			case GREATSWORD -> greatsword(k, t, v);
			case KATANA -> katana(k, t, v);
			case LONGSWORD -> longsword(k, t, v);
			case SPEAR -> spear(k, t, v);
			case HALBERD -> halberd(k, t, v);
			case WARHAMMER -> warhammer(k, t, v);
			case BATTLEAXE -> battleaxe(k, t, v);
			case SCYTHE -> scythe(k, t, v);
			case DAGGER -> dagger(k, t, v);
			case CLAW -> claw(k, t, v);
			case NINJATO -> ninjato(k, t, v);
			case KUSARIGAMA -> kusarigama(k, t, v);
			case SHURIKEN -> shuriken(k, t, v);
			case KUNAI -> kunai(k, t, v);
			case STAFF -> staff(k, t, v);
			case WAND -> wand(k, t, v);
			case ORB -> orb(k, t, v);
			case GRIMOIRE -> grimoire(k, t, v);
			case SCEPTER -> scepter(k, t, v);
			case CROSSBOW -> crossbow(k, t, v);
			case TWIN_BLADES -> twinBlades(k, t, v);
			case CUTLASS -> cutlass(k, t, v);
			case RAPIER -> rapier(k, t, v);
			case KNUCKLE -> knuckle(k, t, v);
			case ANCHOR -> anchor(k, t, v);
			case PISTOL -> pistol(k, t, v);
			case MUSKET -> musket(k, t, v);
			case HAND_CANNON -> handCannon(k, t, v);
			case HARPOON -> harpoon(k, t, v);
			default -> throw new IllegalArgumentException("no drawing for " + type);
		}
	}

	/** Wrapped handle: alternating light/mid grip, accent bands from tier 3. */
	static char wrap(final int a, final int t) {
		int m = Math.floorMod(a, 4);
		if (t >= 3 && m == 0) {
			return '3';
		}
		return m < 2 ? 'L' : 'M';
	}

	/** Blade shading across: lit upper-left side. */
	static char edge(final int c, final int half) {
		if (c <= -half) {
			return 'h';
		}
		if (c < 0) {
			return 'l';
		}
		if (c == 0) {
			return 'm';
		}
		return c >= half ? 'd' : 'm';
	}

	// ------------------------------------------------------------------ warrior

	static void greatsword(final Canvas k, final int t, final int v) {
		k.diag((a, c) -> {
			int ac = Math.abs(c);
			if (ac <= 2 && a >= -4 && a <= 12 - ac) {
				if (t >= 4 && c == -2) {
					return '5';
				}
				if (t >= 2 && (c == 0 || c == 1) && a <= 7) {
					return t >= 3 ? '3' : 'd';
				}
				return edge(c, 2);
			}
			if (a >= -7 && a <= -5 && ac <= 4 + (v == 1 ? 1 : 0)) {
				if (t >= 2 && ac <= 1 && a == -6) {
					return '4';
				}
				return ac >= 3 ? (t >= 3 ? '3' : 'd') : 'm';
			}
			if (c == 0 && a >= -12 && a <= -8) {
				return wrap(a, t);
			}
			if (a >= -15 && a <= -13 && ac <= 1) {
				return t >= 2 ? '3' : 'd';
			}
			return '.';
		});
		k.outline();
	}

	static void katana(final Canvas k, final int t, final int v) {
		k.diag((a, c) -> {
			int bend = a > 0 ? a * a / (40 + v * 6) : 0;
			int cc = c + bend;
			if (a >= -3 && a <= 13 && cc >= -1 && cc <= (a >= 12 ? 0 : 1)) {
				if (cc == 1) {
					return t >= 3 ? '5' : 'h';
				}
				return cc == 0 ? 'l' : 'd';
			}
			if (a >= -5 && a <= -4 && Math.abs(c) <= 2) {
				return Math.abs(c) == 2 ? '2' : '3';
			}
			if (c == 0 && a >= -13 && a <= -6) {
				return Math.floorMod(a, 4) < 2 ? 'D' : (t >= 2 ? '4' : 'L');
			}
			if (c == 0 && a == -15) {
				return 'd';
			}
			return '.';
		});
		k.outline();
	}

	static void longsword(final Canvas k, final int t, final int v) {
		k.diag((a, c) -> {
			int ac = Math.abs(c);
			if (ac <= 1 && a >= -2 && a <= 13 - ac) {
				if (t >= 2 && c == 0 && a <= 9) {
					return t >= 4 ? '5' : 'd';
				}
				return c < 0 ? 'h' : c == 0 ? 'l' : 'd';
			}
			if (a >= -4 && a <= -3 && ac <= 4) {
				if (ac >= 3 && t >= 2) {
					return '3';
				}
				return ac <= 1 && t >= 3 ? '4' : 'm';
			}
			if (c == 0 && a >= -11 && a <= -5) {
				return wrap(a, t);
			}
			if (a >= -14 && a <= -12 && ac <= 1) {
				return t >= 2 ? '4' : 'd';
			}
			return '.';
		});
		k.outline();
	}

	static void spear(final Canvas k, final int t, final int v) {
		k.diag((a, c) -> {
			int ac = Math.abs(c);
			if (a >= 6 && a <= 14) {
				int w = a <= 9 ? (a - 5) / 2 : (14 - a) / 2;
				if (ac <= w) {
					if (t >= 4 && ac == w && ac > 0) {
						return '4';
					}
					return edge(c, Math.max(1, w));
				}
			}
			if (a >= 4 && a <= 5 && ac <= 1) {
				return t >= 2 ? '3' : 'd';
			}
			if (t >= 3 && a >= 1 && a <= 3 && (c == -2 || c == 2)) {
				return '4';
			}
			if (c == 0 && a >= -15 && a <= 3) {
				return a == -3 && t >= 2 ? '3' : (Math.floorMod(a, 6) < 3 ? 'L' : 'M');
			}
			return '.';
		});
		k.outline();
	}

	static void halberd(final Canvas k, final int t, final int v) {
		k.diag((a, c) -> {
			if (c == 0 && a >= 11 && a <= 15) {
				return a >= 13 ? 'h' : 'l';
			}
			if (a >= 3 && a <= 10 && c >= 1 && c <= 5) {
				boolean corner = (a <= 4 || a >= 9) && c >= 4;
				if (!corner) {
					if (t >= 2 && a == 6 && c == 1) {
						return '4';
					}
					return c >= 4 ? (t >= 4 ? '5' : 'h') : c == 1 ? 'd' : 'm';
				}
			}
			if (a >= 6 && a <= 8 && c >= -3 && c <= -1) {
				return c == -3 ? 'l' : 'd';
			}
			if (c == 0 && a >= -15 && a <= 10) {
				return t >= 3 && Math.floorMod(a, 8) == 1 ? '3' : (Math.floorMod(a, 6) < 3 ? 'L' : 'M');
			}
			return '.';
		});
		k.outline();
	}

	static void warhammer(final Canvas k, final int t, final int v) {
		k.diag((a, c) -> {
			int ac = Math.abs(c);
			if (a >= 4 && a <= 10 && ac <= 4) {
				if (t >= 2 && a >= 6 && a <= 8 && ac <= 1) {
					return t >= 4 ? '5' : '3';
				}
				if (a == 10) {
					return 'h';
				}
				return c <= -3 ? 'l' : c >= 3 ? 'd' : 'm';
			}
			if (t >= 3 && a >= 6 && a <= 8 && ac == 5) {
				return '4';
			}
			if (c == 0 && a >= -15 && a <= 3) {
				return a == -9 && t >= 2 ? '3' : (Math.floorMod(a, 4) < 2 ? 'L' : 'M');
			}
			return '.';
		});
		k.outline();
	}

	static void battleaxe(final Canvas k, final int t, final int v) {
		k.diag((a, c) -> {
			if (a >= 2 && a <= 11 && c >= 1) {
				int reach = 6 - Math.abs(a - 6) / 2;
				if (c <= reach) {
					if (c == reach) {
						return t >= 4 ? '5' : 'h';
					}
					if (t >= 2 && c == 2 && a >= 5 && a <= 7) {
						return '3';
					}
					return c == 1 ? 'd' : 'm';
				}
			}
			if (a >= 5 && a <= 8 && c >= -2 && c <= -1) {
				return t >= 3 ? '4' : 'd';
			}
			if (c == 0 && a >= 10 && a <= 12) {
				return 'l';
			}
			if (c == 0 && a >= -15 && a <= 9) {
				return wrap(a, t);
			}
			return '.';
		});
		k.outline();
	}

	static void scythe(final Canvas k, final int t, final int v) {
		k.diag((a, c) -> {
			if (c <= -1 && c >= -10) {
				double center = 11.5 - (c * c) / 18.0;
				double d = a - center;
				if (d >= -1.5 && d <= 0.5 + (c > -4 ? 0.5 : 0)) {
					if (d < -0.5) {
						return t >= 4 ? '5' : 'h';
					}
					return c == -10 || c == -9 ? 'l' : 'm';
				}
			}
			if (c == 0 && a >= 9 && a <= 12) {
				return t >= 3 ? '3' : 'd';
			}
			if (c == 0 && a >= -15 && a <= 8) {
				return Math.floorMod(a, 6) < 3 ? 'D' : 'M';
			}
			return '.';
		});
		k.outline();
	}

	// ------------------------------------------------------------------ rogue

	static void dagger(final Canvas k, final int t, final int v) {
		k.diag((a, c) -> {
			int ac = Math.abs(c);
			if (ac <= 1 && a >= 1 && a <= 12 - ac * (v == 2 ? 2 : 1)) {
				if (t >= 3 && c == 0) {
					return '4';
				}
				return c < 0 ? 'h' : c == 0 ? 'l' : 'd';
			}
			if (a >= -1 && a <= 0 && ac <= 3) {
				return ac == 3 && t >= 2 ? '3' : 'm';
			}
			if (c == 0 && a >= -7 && a <= -2) {
				return Math.floorMod(a, 4) < 2 ? 'D' : 'M';
			}
			if (a >= -10 && a <= -8 && ac <= 1) {
				return t >= 2 ? '4' : 'd';
			}
			return '.';
		});
		k.outline();
	}

	static void claw(final Canvas k, final int t, final int v) {
		k.diag((a, c) -> {
			for (int i = -1; i <= 1; i++) {
				int c0 = i * 3;
				int len = 11 - Math.abs(i) * 2;
				int bend = a > 6 ? 1 : 0;
				if (c == c0 - bend && a >= 1 && a <= len) {
					return t >= 4 ? '5' : 'h';
				}
				if (c == c0 + 1 - bend && a >= 1 && a <= len - 2) {
					return 'm';
				}
			}
			if (a >= -3 && a <= 0 && Math.abs(c) <= 5) {
				return Math.abs(c) == 5 ? 'd' : (t >= 2 && Math.abs(c) <= 1 ? '3' : 'm');
			}
			if (a >= -11 && a <= -4 && Math.abs(c) <= 2) {
				return Math.abs(c) == 2 ? 'D' : 'M';
			}
			return '.';
		});
		k.outline();
	}

	static void ninjato(final Canvas k, final int t, final int v) {
		k.diag((a, c) -> {
			int ac = Math.abs(c);
			if (ac <= 1 && a >= -3 && a <= 12 && !(a >= 11 && c == 1)) {
				return c < 0 ? (t >= 3 ? '5' : 'h') : c == 0 ? 'l' : 'd';
			}
			if (a >= -5 && a <= -4 && ac <= 2) {
				return t >= 2 ? (ac == 2 ? '2' : '3') : 'd';
			}
			if (c == 0 && a >= -13 && a <= -6) {
				return Math.floorMod(a, 4) < 2 ? 'O' : 'D';
			}
			if (c == 0 && a == -15) {
				return 'm';
			}
			return '.';
		});
		k.outline();
	}

	static void kusarigama(final Canvas k, final int t, final int v) {
		k.diag((a, c) -> {
			if (c == 0 && a >= 1 && a <= 7) {
				return wrap(a, t);
			}
			if (a >= 7 && a <= 10 && c <= -1 && c >= -7) {
				int center = c <= -5 ? 8 : 9;
				if (a == center || a == center + 1) {
					return a == center ? (t >= 4 ? '5' : 'h') : 'm';
				}
			}
			if (c == 0 && a >= -11 && a <= 0) {
				return Math.floorMod(a, 4) == 1 ? 'd' : Math.floorMod(a, 4) == 3 ? 'm' : '.';
			}
			if (a >= -15 && a <= -13 && Math.abs(c) <= 1) {
				return t >= 2 ? '3' : 'd';
			}
			return '.';
		});
		k.outline();
	}

	static void shuriken(final Canvas k, final int t, final int v) {
		int points = v == 2 ? 6 : 4;
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double dx = x - 7.5;
				double dy = y - 7.5;
				double r = Math.sqrt(dx * dx + dy * dy);
				double angle = Math.atan2(dy, dx) + (v == 1 ? Math.PI / 4 : 0.0);
				double reach = 2.4 + 4.9 * Math.pow(Math.abs(Math.cos(points / 2.0 * angle)), 4);
				if (r <= reach && r >= 1.1) {
					char ch;
					if (t >= 2 && r >= 1.8 && r <= 2.6) {
						ch = t >= 4 ? '5' : '3';
					} else if (r > reach - 1.0 && r > 4.0) {
						ch = 'h';
					} else {
						ch = dx + dy < 0 ? 'l' : 'd';
					}
					k.put(x, y, ch);
				}
			}
		}
		k.outline();
	}

	static void kunai(final Canvas k, final int t, final int v) {
		k.diag((a, c) -> {
			int ac = Math.abs(c);
			if (a >= 3 && a <= 14) {
				int w = a <= 7 ? (a - 3) / 2 : (14 - a) / 2;
				if (ac <= w) {
					return edge(c, Math.max(1, w));
				}
			}
			if (c == 0 && a >= -7 && a <= 2) {
				return t >= 2 && Math.floorMod(a, 4) == 1 ? '3' : 'D';
			}
			return '.';
		});
		ring(k, 2.3, 12.7, 1.4, 2.3, t >= 3 ? '3' : 'm');
		k.outline();
	}

	// ------------------------------------------------------------------ mage

	static void staff(final Canvas k, final int t, final int v) {
		k.diag((a, c) -> {
			int ac = Math.abs(c);
			int d = Math.abs(a - 10) + ac;
			if (d <= 3) {
				if (a > 10 && c < 0 && d <= 2) {
					return '5';
				}
				return d == 3 ? '2' : d == 2 ? '3' : '4';
			}
			if (t >= 2 && a >= 6 && a <= 8 && ac >= 2 && ac <= 3) {
				return ac == 3 ? 'l' : 'm';
			}
			if (t >= 3 && a >= 12 && a <= 13 && ac == 3) {
				return 'l';
			}
			if (c == 0 && a >= -15 && a <= 7) {
				return a == 5 || a == 3 ? 'd' : (Math.floorMod(a, 6) < 3 ? 'L' : 'M');
			}
			return '.';
		});
		if (t >= 4) {
			k.put(12, 2, 'w');
			k.put(14, 4, 'w');
		}
		k.outline();
	}

	static void wand(final Canvas k, final int t, final int v) {
		k.diag((a, c) -> {
			int ac = Math.abs(c);
			if (Math.abs(a - 8) + ac <= 2) {
				return ac == 0 && a >= 8 ? '5' : '4';
			}
			if (t >= 3 && a == 6 && ac == 3) {
				return '3';
			}
			if (c == 0 && a >= -11 && a <= 5) {
				if (a <= -5) {
					return t >= 2 && Math.floorMod(a, 4) == 1 ? '3' : 'D';
				}
				return 'L';
			}
			return '.';
		});
		if (t >= 4) {
			k.put(14, 1, 'w');
		}
		k.outline();
	}

	static void orb(final Canvas k, final int t, final int v) {
		double cx = 7.5;
		double cy = 6.5;
		double radius = 5.2;
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double dx = x - cx;
				double dy = y - cy;
				double r = Math.sqrt(dx * dx + dy * dy);
				if (r <= radius) {
					double light = -(dx + dy) / radius;
					char ch = light > 0.9 ? '5' : light > 0.2 ? '4' : light > -0.6 ? '3' : '2';
					if (t >= 2 && Math.abs(r - 2.6) < 0.5 && (x + y + v) % 3 == 0) {
						ch = '5';
					}
					k.put(x, y, ch);
				}
			}
		}
		// stand
		k.rect(5, 12, 10, 12, 'm');
		k.rect(6, 13, 9, 14, 'd');
		if (t >= 2) {
			k.put(4, 11, 'l');
			k.put(11, 11, 'l');
		}
		if (t >= 3) {
			k.put(3, 9, 'l');
			k.put(12, 9, 'l');
		}
		if (t >= 4) {
			k.put(5, 3, 'w');
		}
		k.outline();
	}

	static void grimoire(final Canvas k, final int t, final int v) {
		k.rect(3, 2, 12, 14, 'M');
		k.rect(3, 2, 4, 14, 'D');
		k.rect(13, 3, 13, 13, 'w');
		for (int x = 3; x <= 12; x++) {
			k.put(x, 2, 'L');
		}
		// emblem
		int cx = 8;
		int cy = 8;
		for (int y = cy - 2; y <= cy + 2; y++) {
			for (int x = cx - 2; x <= cx + 2; x++) {
				int d = Math.abs(x - cx) + Math.abs(y - cy);
				if (d <= 2) {
					k.put(x, y, d == 0 ? (t >= 4 ? 'w' : '5') : d == 1 ? '4' : '3');
				}
			}
		}
		// clasp and corners
		k.rect(12, 7, 13, 9, 'l');
		if (t >= 2) {
			k.put(5, 3, 'l');
			k.put(12, 3, 'l');
			k.put(5, 13, 'l');
			k.put(12, 13, 'l');
		}
		if (t >= 3) {
			for (int x = 6; x <= 10; x += 2) {
				k.put(x, 4, '3');
				k.put(x, 12, '3');
			}
		}
		k.outline();
	}

	static void scepter(final Canvas k, final int t, final int v) {
		k.diag((a, c) -> {
			int ac = Math.abs(c);
			if (a >= 6 && a <= 13 && ac <= 1) {
				if (a == 9 && c == 0 || a == 10 && ac == 1) {
					return '4';
				}
				return a >= 12 ? 'h' : c < 0 ? 'l' : 'm';
			}
			if (a >= 8 && a <= 10 && ac >= 2 && ac <= 3) {
				return ac == 3 ? (t >= 2 ? '3' : 'l') : 'm';
			}
			if (t >= 3 && a >= 13 && a <= 14 && ac <= 1) {
				return '5';
			}
			if (c == 0 && a >= -14 && a <= 5) {
				if (a <= -9) {
					return Math.floorMod(a, 4) < 2 ? 'L' : 'M';
				}
				return (a == -5 || a == 1) && t >= 2 ? '3' : 'l';
			}
			return '.';
		});
		k.outline();
	}

	// ------------------------------------------------------------------ archer

	static void crossbow(final Canvas k, final int t, final int v) {
		k.diag((a, c) -> {
			int ac = Math.abs(c);
			// prod (bow arms) across the stock near the front, swept back toward the tips
			int arm = 5 - ac / 2;
			if (ac >= 1 && ac <= 7 && (a == arm || a == arm - 1)) {
				return ac >= 6 ? (t >= 2 ? '3' : 'd') : a == arm ? 'l' : 'm';
			}
			// string from the arm tips back to the nut
			if (ac >= 1 && ac <= 7 && a == arm - 2 - (7 - ac) / 2 && ac >= 5) {
				return 'H';
			}
			// bolt on the stock
			if (c == 0 && a >= 5 && a <= 13) {
				return a >= 12 ? 'h' : 'l';
			}
			// stock
			if (ac <= 1 && a >= -13 && a <= 3) {
				if (a <= -9) {
					return c < 0 ? 'L' : 'D';
				}
				return c == 0 ? 'M' : c < 0 ? 'L' : 'D';
			}
			if (a == -5 && c == 2) {
				return 'd';
			}
			return '.';
		});
		// string from arm tips to the nut
		k.outline();
		if (t >= 4) {
			k.put(14, 1, 'w');
		}
	}

	static void twinBlades(final Canvas k, final int t, final int v) {
		// second blade (accent colored) on the other diagonal, drawn first so the first sits on top
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				k.put(15 - x, y, shortBlade(x - y, x + y - 15, t, true));
			}
		}
		k.diag((a, c) -> shortBlade(a, c, t, false));
		k.outline();
	}

	private static char shortBlade(final int a, final int c, final int t, final boolean accent) {
		int ac = Math.abs(c);
		if (ac <= 1 && a >= 0 && a <= 12 - ac) {
			if (accent) {
				return c < 0 ? '5' : c == 0 ? '4' : '3';
			}
			return c < 0 ? 'h' : c == 0 ? 'l' : 'd';
		}
		if (a >= -2 && a <= -1 && ac <= 2) {
			return accent ? '2' : 'd';
		}
		if (c == 0 && a >= -9 && a <= -3) {
			return Math.floorMod(a, 4) < 2 ? 'D' : 'M';
		}
		return '.';
	}

	// ------------------------------------------------------------------ pirate

	static void cutlass(final Canvas k, final int t, final int v) {
		k.diag((a, c) -> {
			int bend = a > 2 ? (a - 2) * (a - 2) / 36 : 0;
			int cc = c - bend;
			int width = a > 7 ? 2 : 1;
			if (a >= -2 && a <= 13 && cc >= -width && cc <= 0 && !(a == 13 && cc < 0)) {
				return cc == -width ? (t >= 4 ? '5' : 'h') : cc == 0 ? 'd' : 'l';
			}
			if (a >= -4 && a <= -3 && Math.abs(c) <= 2) {
				return 'm';
			}
			// knuckle bow
			if (c == 3 && a >= -10 && a <= -4 || a == -10 && c >= 1 && c <= 3) {
				return t >= 2 ? '3' : 'd';
			}
			if (c == 0 && a >= -11 && a <= -5) {
				return wrap(a, t);
			}
			if (c == 0 && a == -13) {
				return t >= 2 ? '4' : 'd';
			}
			return '.';
		});
		k.outline();
	}

	static void rapier(final Canvas k, final int t, final int v) {
		k.diag((a, c) -> {
			if (c == 0 && a >= -3 && a <= 15) {
				return a >= 13 ? 'h' : 'l';
			}
			if (a == -5 && Math.abs(c) <= 3) {
				return 'm';
			}
			boolean loop = c >= 1 && c <= 3 && a >= -10 && a <= -5 && (c == 3 || a == -10);
			boolean loop2 = c <= -1 && c >= -2 && a >= -8 && a <= -5 && (c == -2 || a == -8);
			if (loop || loop2) {
				return t >= 2 ? '3' : 'd';
			}
			if (c == 0 && a >= -11 && a <= -6) {
				return wrap(a, t);
			}
			if (a >= -14 && a <= -13 && Math.abs(c) <= 1) {
				return t >= 3 ? '4' : 'd';
			}
			return '.';
		});
		k.outline();
	}

	static void knuckle(final Canvas k, final int t, final int v) {
		for (int i = 0; i < 4; i++) {
			ring(k, 3.0 + i * 3.2, 6.0, 0.9, 1.9, 'l');
		}
		k.rect(1, 8, 14, 9, 'm');
		k.rect(1, 9, 14, 9, 'd');
		k.rect(4, 10, 11, 13, 'M');
		k.rect(4, 13, 11, 13, 'D');
		if (t >= 2) {
			for (int i = 0; i < 4; i++) {
				k.put(3 + (int)Math.round(i * 3.2), 3, t >= 4 ? '5' : '3');
			}
		}
		if (t >= 3) {
			k.rect(6, 11, 9, 11, '4');
		}
		k.outline();
	}

	static void anchor(final Canvas k, final int t, final int v) {
		ring(k, 7.5, 2.5, 0.9, 2.0, 'l');
		k.rect(7, 4, 8, 13, 'm');
		k.rect(7, 4, 7, 12, 'l');
		k.rect(4, 6, 11, 6, 'M');
		if (t >= 2) {
			for (int y = 8; y <= 10; y += 2) {
				k.put(7, y, '3');
				k.put(8, y, '3');
			}
		}
		for (int x = 1; x <= 14; x++) {
			double ya = 13.5 - (x - 7.5) * (x - 7.5) / 10.0;
			for (int y = 0; y < 16; y++) {
				if (Math.abs(y - ya) < 0.75) {
					k.put(x, y, x <= 7 ? 'l' : 'd');
				}
			}
		}
		// flukes
		k.put(1, 7, 'h');
		k.put(2, 7, 'm');
		k.put(14, 7, 'h');
		k.put(13, 7, 'm');
		if (t >= 3) {
			k.put(1, 8, '4');
			k.put(14, 8, '4');
		}
		if (t >= 4) {
			k.put(7, 14, 'w');
		}
		k.outline();
	}

	static void pistol(final Canvas k, final int t, final int v) {
		k.rect(6, 5, 15, 5, 'l');
		k.rect(6, 6, 15, 6, 'd');
		k.put(15, 5, 'h');
		k.rect(4, 5, 8, 7, 'm');
		k.rect(4, 3, 5, 4, 'd');
		for (int y = 7; y <= 13; y++) {
			int shift = (y - 7) / 3;
			for (int x = 3 - shift; x <= 6 - shift; x++) {
				k.put(x, y, x == 3 - shift ? 'L' : x == 6 - shift ? 'D' : 'M');
			}
		}
		k.put(7, 8, 'd');
		k.put(8, 9, 'd');
		k.put(7, 9, 'd');
		if (t >= 2) {
			k.rect(8, 5, 9, 6, '3');
		}
		if (t >= 3) {
			k.put(12, 5, '4');
			k.put(12, 6, '3');
			k.put(4, 13 - 2, '3');
		}
		if (t >= 4) {
			k.put(15, 6, 'w');
		}
		k.outline();
	}

	static void musket(final Canvas k, final int t, final int v) {
		k.diag((a, c) -> {
			if (a >= -15 && a <= -6) {
				int w = a <= -11 ? 2 : 1;
				if (Math.abs(c) <= w) {
					return c < 0 ? 'L' : c == 0 ? 'M' : 'D';
				}
			}
			if (a >= -6 && a <= -3 && c >= -1 && c <= 1) {
				return t >= 2 && c == 0 ? '3' : 'm';
			}
			if (a == -7 && c == 2) {
				return 'd';
			}
			if (a >= -3 && a <= 14 && c >= -1 && c <= 0) {
				if (t >= 2 && (a == 1 || a == 2 || a == 8 || a == 7)) {
					return '3';
				}
				return c == -1 ? 'l' : 'd';
			}
			if (t >= 3 && c == 1 && a >= 12 && a <= 14) {
				return 'h';
			}
			return '.';
		});
		k.outline();
	}

	static void handCannon(final Canvas k, final int t, final int v) {
		k.diag((a, c) -> {
			int ac = Math.abs(c);
			if (a >= 11 && a <= 13 && ac <= 3) {
				return t >= 2 ? (ac == 3 ? '2' : '3') : (c < 0 ? 'l' : 'd');
			}
			if (a >= -2 && a <= 10 && ac <= 2) {
				return c == -2 ? 'l' : c == 2 ? 'o' : c == 1 ? 'd' : 'm';
			}
			if (a >= -4 && a <= -3 && ac <= 3) {
				return 'd';
			}
			if (ac <= 1 && a >= -14 && a <= -5) {
				return c < 0 ? 'L' : c == 0 ? 'M' : 'D';
			}
			if (t >= 2 && a == 0 && c == -3) {
				return '4';
			}
			return '.';
		});
		if (t >= 2) {
			k.put(5, 8, 'w');
		}
		k.outline();
	}

	static void harpoon(final Canvas k, final int t, final int v) {
		k.diag((a, c) -> {
			int ac = Math.abs(c);
			if (a >= 9 && a <= 14) {
				int w = (14 - a) / 2 + (a <= 10 ? 1 : 0);
				if (ac <= w) {
					return edge(c, Math.max(1, w));
				}
			}
			if (a >= 7 && a <= 8 && ac >= 2 && ac <= 3) {
				return ac == 3 ? 'd' : 'm';
			}
			if (c == 0 && a >= 5 && a <= 8) {
				return 'm';
			}
			if (c == 0 && a >= -15 && a <= 4) {
				if (a >= -10 && a <= -4) {
					return t >= 2 ? (Math.floorMod(a, 4) < 2 ? '3' : 'L') : 'L';
				}
				return Math.floorMod(a, 6) < 3 ? 'M' : 'D';
			}
			return '.';
		});
		k.outline();
	}

	/** Hollow circle (ring) in pixel space. */
	static void ring(final Canvas k, final double cx, final double cy, final double inner, final double outer, final char ch) {
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double d = Math.hypot(x - cx, y - cy);
				if (d >= inner && d <= outer) {
					k.put(x, y, ch);
				}
			}
		}
	}

	private Shapes() {
	}
}
