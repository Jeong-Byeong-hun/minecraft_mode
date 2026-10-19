package com.minecraftmode.jump;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The layout of the Forest of Tenacity, without any blocks: a square spiral of footholds ({@link Pad}) around one giant trunk, five
 * stages that each end on a checkpoint, a few climbs up vines or rope ladders, and the crown on top of the trunk (the goal). It is
 * a pure function of nothing (built once, the same every time), so {@link JumpForestBuilder} can rebuild the course block for block,
 * and {@link #problems()} checks that every jump can be made: at most {@link #MAX_GAP} empty blocks between footholds on the same
 * level, {@link #MAX_GAP_UP} when the next one is one higher, never more than one higher, and nothing solid in the air a player
 * needs to stand and jump ({@link #reserved}).
 *
 * <p>Coordinates are relative to the course's corner: x and z in {@code 0..SIZE-1}, y = 0 is the forest floor (a player stands at
 * y + 1 on a foothold whose blocks are at y).
 */
public final class JumpCourse {
	/** The course's square, in blocks (three chunks). */
	public static final int SIZE = 48;
	/** The trunk's axis. */
	public static final int CENTER = 24;
	/** Distance of the path's middle lane from the trunk's axis (the spiral is a square ring). */
	public static final int RING = 7;
	/** Trunk cells: dx² + dz² at most this (radius about 3). */
	public static final int TRUNK_RADIUS_SQ = 10;
	/** Crown (goal platform) cells: dx² + dz² at most this. */
	public static final int CROWN_RADIUS_SQ = 18;
	public static final int MAX_GAP = 3;
	public static final int MAX_GAP_UP = 2;
	/** Air a player needs above a foothold: feet, body, head and the top of a jump. */
	public static final int HEADROOM = 4;
	/** Cells of the ring (one lap of the spiral). */
	public static final int LAP = 8 * RING;

	/** What a foothold is made of (the builder picks the blocks). */
	public enum Surface {
		GROUND, MOSS, LEAVES, AZALEA, BLOSSOM, RED_CAP, BROWN_CAP, BRANCH, POST, CHECKPOINT, GOAL
	}

	/**
	 * One foothold: its cells (packed x/z, see {@link #xz}) at height {@code y}. {@code arc} is the ring position of its last cell along
	 * the path, {@code climb} whether it is reached by climbing (vines or a ladder) instead of a jump.
	 */
	public record Pad(int index, int stage, int y, Surface surface, List<Long> cells, int arc, boolean climb) {
		public boolean contains(final int x, final int z) {
			return this.cells.contains(xz(x, z));
		}
	}

	/**
	 * A climb: a column of vines (or a rope ladder) at x/z from {@code y0} up {@code height} blocks, hanging on a log pillar one block
	 * further in direction {@code dx}/{@code dz}; the pillar's top is the first cell of the next foothold.
	 */
	public record Climb(int x, int z, int y0, int height, int dx, int dz, boolean ladder) {
	}

	/** A step of a stage pattern: foothold length along the path, three lanes wide or one, empty blocks before it, rise. */
	private record Step(int length, boolean wide, int gap, int rise, Surface surface) {
	}

	private static JumpCourse tenacity;

	public final List<Pad> pads = new ArrayList<>();
	/** Indices into {@link #pads}: the start (0) and the last foothold of every stage. */
	public final List<Integer> checkpoints = new ArrayList<>();
	public final List<Climb> climbs = new ArrayList<>();
	/** Cells (packed x/y/z, see {@link #xyz}) that must stay free of anything solid. */
	public final Set<Long> reserved = new HashSet<>();
	/** Logs of the climb pillars (packed x/y/z). */
	public final Set<Long> pillars = new HashSet<>();
	public int goalPad;
	public int top;

	private int arc;
	private int y;
	private int stage;

	public static synchronized JumpCourse tenacity() {
		if (tenacity == null) {
			tenacity = new JumpCourse();
			tenacity.generate();
		}
		return tenacity;
	}

	public static long xz(final int x, final int z) {
		return (long)x << 16 | z & 0xFFFF;
	}

	public static long xyz(final int x, final int y, final int z) {
		return ((long)x & 0xFFFF) << 32 | ((long)y & 0xFFFF) << 16 | (long)z & 0xFFFF;
	}

	/** Ring cell {@code s} (any integer; one lap is {@link #LAP}) shifted {@code lane} blocks outward: {x, z}. */
	public static int[] ring(final int s, final int lane) {
		int side = Math.floorMod(s, LAP) / (2 * RING);
		int t = Math.floorMod(s, LAP) % (2 * RING) - RING;
		int[] d = direction(side);
		int[] n = normal(side);
		return new int[] {CENTER + t * d[0] + (RING + lane) * n[0], CENTER + t * d[1] + (RING + lane) * n[1]};
	}

	/** How far along its side of the square ring cell {@code s} is: -RING (the corner it starts at) to RING - 1. */
	public static int along(final int s) {
		return Math.floorMod(s, LAP) % (2 * RING) - RING;
	}

	/** Which side of the ring cell {@code s} is on (0 north, 1 east, 2 south, 3 west). */
	public static int side(final int s) {
		return Math.floorMod(s, LAP) / (2 * RING);
	}

	/** Walking direction along side {@code side}: east, south, west, north. */
	public static int[] direction(final int side) {
		return switch (side) {
			case 0 -> new int[] {1, 0};
			case 1 -> new int[] {0, 1};
			case 2 -> new int[] {-1, 0};
			default -> new int[] {0, -1};
		};
	}

	/** Outward from the trunk on side {@code side}: north, east, south, west. */
	public static int[] normal(final int side) {
		return switch (side) {
			case 0 -> new int[] {0, -1};
			case 1 -> new int[] {1, 0};
			case 2 -> new int[] {0, 1};
			default -> new int[] {-1, 0};
		};
	}

	public static boolean trunk(final int x, final int z) {
		int dx = x - CENTER;
		int dz = z - CENTER;
		return dx * dx + dz * dz <= TRUNK_RADIUS_SQ;
	}

	// ------------------------------------------------------------------ the path

	private void generate() {
		// the start: the forest floor around ring cell 0
		List<Long> start = new ArrayList<>();
		for (int s = -1; s <= 1; s++) {
			for (int lane = -1; lane <= 1; lane++) {
				int[] c = ring(s, lane);
				start.add(xz(c[0], c[1]));
			}
		}
		this.pads.add(new Pad(0, 0, 0, Surface.GROUND, dedupe(start), 1, false));
		this.checkpoints.add(0);
		this.arc = 1;
		this.y = 0;

		// 1. Sprout Trail: broad moss and leaf beds a block apart
		this.stage(1, 13, 6, 3, false, new Step[] {
			new Step(3, true, 1, 1, Surface.MOSS), new Step(2, true, 1, 1, Surface.LEAVES), new Step(3, true, 1, 0, Surface.AZALEA),
			new Step(2, true, 1, 1, Surface.MOSS), new Step(2, true, 2, 0, Surface.LEAVES), new Step(3, true, 1, 1, Surface.BLOSSOM)});
		// 2. Mushroom Steps: caps two blocks apart
		this.stage(2, 15, 8, 4, true, new Step[] {
			new Step(2, true, 2, 1, Surface.RED_CAP), new Step(2, true, 2, 0, Surface.BROWN_CAP), new Step(2, true, 2, 1, Surface.RED_CAP),
			new Step(1, true, 2, 1, Surface.BROWN_CAP), new Step(2, false, 3, 0, Surface.LEAVES)});
		// 3. Branch Walk: narrow branches, some three apart
		this.stage(3, 15, 7, 5, false, new Step[] {
			new Step(3, false, 2, 1, Surface.BRANCH), new Step(2, false, 2, 1, Surface.BRANCH), new Step(3, false, 3, 0, Surface.BRANCH),
			new Step(2, false, 2, 1, Surface.AZALEA), new Step(2, false, 3, 0, Surface.BRANCH)});
		// 4. Leaf Hops: single leaf blocks
		this.stage(4, 15, 8, 4, true, new Step[] {
			new Step(1, false, 2, 1, Surface.LEAVES), new Step(1, false, 3, 0, Surface.BLOSSOM), new Step(1, false, 2, 1, Surface.LEAVES),
			new Step(2, false, 2, 1, Surface.AZALEA), new Step(1, false, 3, 0, Surface.LEAVES)});
		// 5. The Crown Climb: posts three apart, then the last climb
		this.stage(5, 14, 9, 5, false, new Step[] {
			new Step(1, false, 3, 0, Surface.POST), new Step(1, false, 2, 1, Surface.POST), new Step(1, false, 2, 1, Surface.RED_CAP),
			new Step(1, false, 3, 0, Surface.POST), new Step(1, false, 2, 1, Surface.BRANCH)});
		this.crown();
	}

	/**
	 * Adds a stage: its pattern repeated until it has risen {@code rise} blocks, a climb of {@code climbHeight} once it has risen
	 * {@code climbAt}, then its checkpoint (a broad moss bed).
	 */
	private void stage(final int stage, final int rise, final int climbAt, final int climbHeight, final boolean ladder, final Step[] pattern) {
		this.stage = stage;
		int base = this.y;
		boolean climbed = false;
		for (int i = 0; this.y - base < rise; i++) {
			if (!climbed && this.y - base >= climbAt) {
				this.climb(climbHeight, ladder);
				climbed = true;
				continue;
			}
			Step step = pattern[i % pattern.length];
			this.place(step.length, step.wide, step.gap, step.rise, step.surface, false);
		}
		this.place(3, true, 1, 1, Surface.CHECKPOINT, false);
		this.checkpoints.add(this.pads.size() - 1);
	}

	private void place(final int length, final boolean wide, final int gap, final int rise, final Surface surface, final boolean climb) {
		int first = this.arc + gap + 1;
		int last = first + length - 1;
		List<Long> cells = new ArrayList<>();
		for (int s = first; s <= last; s++) {
			// round a corner the inner lane would reach into the next side's path: there a broad foothold keeps two lanes
			int t = along(s);
			boolean corner = t == -RING || t == -RING + 1 || t == RING - 1;
			for (int lane = wide && !corner ? -1 : 0; lane <= (wide ? 1 : 0); lane++) {
				int[] c = ring(s, lane);
				cells.add(xz(c[0], c[1]));
			}
		}
		this.y += rise;
		this.arc = last;
		this.pads.add(new Pad(this.pads.size(), this.stage, this.y, surface, dedupe(cells), last, climb));
	}

	/**
	 * A climb from the last foothold: the foothold grows one block on, vines (or a ladder) rise {@code height} blocks from it against a
	 * log pillar right after, and the pillar's top starts the next foothold.
	 */
	private void climb(final int height, final boolean ladder) {
		Pad from = this.pads.removeLast();
		int[] vine = ring(this.arc + 1, 0);
		int[] pillar = ring(this.arc + 2, 0);
		List<Long> cells = new ArrayList<>(from.cells());
		cells.add(xz(vine[0], vine[1]));
		this.pads.add(new Pad(from.index(), from.stage(), from.y(), from.surface(), dedupe(cells), this.arc + 1, from.climb()));
		this.climbs.add(new Climb(vine[0], vine[1], this.y + 1, height, pillar[0] - vine[0], pillar[1] - vine[1], ladder));
		for (int h = 1; h < height; h++) {
			this.pillars.add(xyz(pillar[0], this.y + h, pillar[1]));
		}
		// the next foothold starts on the pillar's top
		int first = this.arc + 2;
		List<Long> next = new ArrayList<>();
		for (int s = first; s <= first + 1; s++) {
			int[] c = ring(s, 0);
			next.add(xz(c[0], c[1]));
		}
		this.y += height;
		this.arc = first + 1;
		this.pads.add(new Pad(this.pads.size(), this.stage, this.y, Surface.BRANCH, next, this.arc, true));
	}

	/** The way onto the crown: footholds on until the path is near a side's middle, a bridge in, and the crown disc one higher. */
	private void crown() {
		this.stage = 6;
		while (Math.abs(along(this.arc + 2)) > 2) {
			this.place(1, false, 2, 0, Surface.LEAVES, false);
		}
		int side = side(this.arc + 2);
		int[] n = normal(side);
		int[] at = ring(this.arc + 2, 0);
		List<Long> cells = new ArrayList<>();
		for (int dx = -5; dx <= 5; dx++) {
			for (int dz = -5; dz <= 5; dz++) {
				if (dx * dx + dz * dz <= CROWN_RADIUS_SQ) {
					cells.add(xz(CENTER + dx, CENTER + dz));
				}
			}
		}
		// the bridge: from the crown's edge out to two blocks short of the ring lane
		for (int k = RING - 3; k <= RING - 2; k++) {
			cells.add(xz(at[0] - n[0] * RING + n[0] * k, at[1] - n[1] * RING + n[1] * k));
		}
		this.y += 1;
		this.top = this.y;
		this.pads.add(new Pad(this.pads.size(), this.stage, this.y, Surface.GOAL, dedupe(cells), this.arc + 2, false));
		this.goalPad = this.pads.size() - 1;
		this.reserve();
	}

	private static List<Long> dedupe(final List<Long> cells) {
		return List.copyOf(new java.util.LinkedHashSet<>(cells));
	}

	// ------------------------------------------------------------------ the air a player needs

	private void reserve() {
		for (Pad pad : this.pads) {
			for (long c : pad.cells()) {
				int x = (int)(c >> 16);
				int z = (int)(c & 0xFFFF);
				for (int h = 1; h <= HEADROOM; h++) {
					this.reserved.add(xyz(x, pad.y() + h, z));
				}
			}
		}
		for (Climb climb : this.climbs) {
			for (int h = 0; h < climb.height() + HEADROOM; h++) {
				this.reserved.add(xyz(climb.x(), climb.y0() + h, climb.z()));
			}
		}
		for (int i = 1; i < this.pads.size(); i++) {
			Pad a = this.pads.get(i - 1);
			Pad b = this.pads.get(i);
			if (b.climb()) {
				continue;
			}
			long[] pair = nearest(a, b);
			double ax = (int)(pair[0] >> 16) + 0.5;
			double az = (int)(pair[0] & 0xFFFF) + 0.5;
			double bx = (int)(pair[1] >> 16) + 0.5;
			double bz = (int)(pair[1] & 0xFFFF) + 0.5;
			int low = Math.min(a.y(), b.y());
			int high = Math.max(a.y(), b.y());
			for (double f = 0.0; f <= 1.0; f += 0.05) {
				double px = ax + (bx - ax) * f;
				double pz = az + (bz - az) * f;
				for (double ox : new double[] {-0.35, 0.35}) {
					for (double oz : new double[] {-0.35, 0.35}) {
						int cx = (int)Math.floor(px + ox);
						int cz = (int)Math.floor(pz + oz);
						for (int h = low + 1; h <= high + HEADROOM; h++) {
							// the higher foothold itself is where the jump lands, not air it needs
							if (h != b.y() || !b.contains(cx, cz)) {
								this.reserved.add(xyz(cx, h, cz));
							}
						}
					}
				}
			}
		}
	}

	/** The two closest cells of {@code a} and {@code b} (packed x/z). */
	private static long[] nearest(final Pad a, final Pad b) {
		long[] best = null;
		double bestDistance = Double.MAX_VALUE;
		for (long ca : a.cells()) {
			for (long cb : b.cells()) {
				double d = distance(ca, cb);
				if (d < bestDistance) {
					bestDistance = d;
					best = new long[] {ca, cb};
				}
			}
		}
		return best;
	}

	private static double distance(final long a, final long b) {
		int dx = (int)(a >> 16) - (int)(b >> 16);
		int dz = (int)(a & 0xFFFF) - (int)(b & 0xFFFF);
		return Math.sqrt(dx * dx + dz * dz);
	}

	/** Empty blocks to clear between {@code a} and {@code b}, measured between block centers (diagonals count their length). */
	public static double gap(final Pad a, final Pad b) {
		long[] pair = nearest(a, b);
		return distance(pair[0], pair[1]) - 1.0;
	}

	// ------------------------------------------------------------------ checks

	/** Everything wrong with the layout (empty when every jump can be made and nothing blocks the way). */
	public List<String> problems() {
		List<String> problems = new ArrayList<>();
		for (int i = 1; i < this.pads.size(); i++) {
			Pad a = this.pads.get(i - 1);
			Pad b = this.pads.get(i);
			double gap = gap(a, b);
			int rise = b.y() - a.y();
			if (b.climb()) {
				if (gap > 0.01) {
					problems.add("climb to pad " + i + " does not start beside pad " + (i - 1));
				}
				continue;
			}
			if (rise > 1) {
				problems.add("pad " + i + " is " + rise + " higher than pad " + (i - 1));
			}
			if (gap < 0.4) {
				problems.add("pad " + i + " touches pad " + (i - 1));
			}
			if (gap > (rise > 0 ? MAX_GAP_UP : MAX_GAP) + 0.01) {
				problems.add(String.format(java.util.Locale.ROOT, "pad %d is %.2f from pad %d (rise %d)", i, gap, i - 1, rise));
			}
		}
		for (Pad pad : this.pads) {
			for (long c : pad.cells()) {
				int x = (int)(c >> 16);
				int z = (int)(c & 0xFFFF);
				if (x < 2 || z < 2 || x >= SIZE - 2 || z >= SIZE - 2) {
					problems.add("pad " + pad.index() + " leaves the course");
				}
				if (pad.surface() != Surface.GOAL && trunk(x, z)) {
					problems.add("pad " + pad.index() + " is inside the trunk");
				}
				if (this.reserved.contains(xyz(x, pad.y(), z))) {
					problems.add("pad " + pad.index() + " at " + x + "," + pad.y() + "," + z + " blocks the way");
				}
			}
		}
		for (long p : this.pillars) {
			if (this.reserved.contains(p)) {
				problems.add("a climb pillar at " + (p >> 32) + "," + (p >> 16 & 0xFFFF) + "," + (p & 0xFFFF) + " blocks the way");
			}
		}
		return problems;
	}
}
