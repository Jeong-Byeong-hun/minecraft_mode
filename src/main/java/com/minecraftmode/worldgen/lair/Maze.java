package com.minecraftmode.worldgen.lair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A square grid maze, a pure function of its seed (so every chunk of a lair builds the same one). A
 * depth-first maze with extra openings so there are loops to explore; the entrance is in the middle
 * of the south side. The goal is somewhere new in every lair: right by the entrance, at the deepest
 * cell, or in some far dead end.
 */
public final class Maze {
	/** Where the goal ended up (for logs and tests). */
	public enum GoalKind {
		NEAR_ENTRANCE,
		DEEPEST,
		DEAD_END
	}

	private final int n;
	/** Open passage from cell (x, z) to the east / south. */
	private final boolean[][] east;
	private final boolean[][] south;
	private final int[][] distance;
	private final int goalX;
	private final int goalZ;
	private final GoalKind goalKind;
	private final List<int[]> caches = new ArrayList<>();

	public Maze(final int n, final long seed) {
		this.n = n;
		this.east = new boolean[n][n];
		this.south = new boolean[n][n];
		Random random = new Random(seed);
		this.carve(random);
		// extra openings: about one wall in twelve, so most dead ends stay but there are loops
		for (int x = 0; x < n; x++) {
			for (int z = 0; z < n; z++) {
				if (x < n - 1 && !this.east[x][z] && random.nextInt(12) == 0) {
					this.east[x][z] = true;
				}
				if (z < n - 1 && !this.south[x][z] && random.nextInt(12) == 0) {
					this.south[x][z] = true;
				}
			}
		}
		this.distance = this.distances();
		List<int[]> deadEnds = new ArrayList<>();
		int[] deepest = {this.entranceX(), n - 1};
		for (int x = 0; x < n; x++) {
			for (int z = 0; z < n; z++) {
				if (this.distance[x][z] > this.distance[deepest[0]][deepest[1]]) {
					deepest = new int[] {x, z};
				}
				if (this.openSides(x, z) == 1 && this.distance[x][z] >= 4) {
					deadEnds.add(new int[] {x, z});
				}
			}
		}
		int roll = random.nextInt(100);
		int[] goal;
		if (roll < 20) {
			List<int[]> near = new ArrayList<>();
			for (int x = 0; x < n; x++) {
				for (int z = 0; z < n; z++) {
					if (this.distance[x][z] >= 2 && this.distance[x][z] <= 4) {
						near.add(new int[] {x, z});
					}
				}
			}
			goal = near.isEmpty() ? deepest : near.get(random.nextInt(near.size()));
			this.goalKind = near.isEmpty() ? GoalKind.DEEPEST : GoalKind.NEAR_ENTRANCE;
		} else if (roll < 55 || deadEnds.isEmpty()) {
			goal = deepest;
			this.goalKind = GoalKind.DEEPEST;
		} else {
			goal = deadEnds.get(random.nextInt(deadEnds.size()));
			this.goalKind = GoalKind.DEAD_END;
		}
		this.goalX = goal[0];
		this.goalZ = goal[1];
		// supply caches in up to three other dead ends
		Collections.shuffle(deadEnds, random);
		for (int[] cell : deadEnds) {
			if (this.caches.size() >= 3) {
				break;
			}
			if (cell[0] != this.goalX || cell[1] != this.goalZ) {
				this.caches.add(cell);
			}
		}
	}

	/** Iterative depth-first carving from the entrance cell. */
	private void carve(final Random random) {
		boolean[][] seen = new boolean[this.n][this.n];
		ArrayDeque<int[]> stack = new ArrayDeque<>();
		stack.push(new int[] {this.entranceX(), this.n - 1});
		seen[this.entranceX()][this.n - 1] = true;
		int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		while (!stack.isEmpty()) {
			int[] cell = stack.peek();
			List<int[]> next = new ArrayList<>();
			for (int[] d : dirs) {
				int nx = cell[0] + d[0];
				int nz = cell[1] + d[1];
				if (nx >= 0 && nz >= 0 && nx < this.n && nz < this.n && !seen[nx][nz]) {
					next.add(new int[] {nx, nz});
				}
			}
			if (next.isEmpty()) {
				stack.pop();
				continue;
			}
			int[] to = next.get(random.nextInt(next.size()));
			this.open(cell[0], cell[1], to[0], to[1]);
			seen[to[0]][to[1]] = true;
			stack.push(to);
		}
	}

	private void open(final int ax, final int az, final int bx, final int bz) {
		if (ax == bx) {
			this.south[ax][Math.min(az, bz)] = true;
		} else {
			this.east[Math.min(ax, bx)][az] = true;
		}
	}

	private int[][] distances() {
		int[][] d = new int[this.n][this.n];
		for (int[] row : d) {
			Arrays.fill(row, -1);
		}
		ArrayDeque<int[]> queue = new ArrayDeque<>();
		queue.add(new int[] {this.entranceX(), this.n - 1});
		d[this.entranceX()][this.n - 1] = 0;
		while (!queue.isEmpty()) {
			int[] c = queue.poll();
			int x = c[0];
			int z = c[1];
			int[][] around = {
				{x + 1, z, this.passable(x, z, x + 1, z) ? 1 : 0}, {x - 1, z, this.passable(x, z, x - 1, z) ? 1 : 0},
				{x, z + 1, this.passable(x, z, x, z + 1) ? 1 : 0}, {x, z - 1, this.passable(x, z, x, z - 1) ? 1 : 0}};
			for (int[] a : around) {
				if (a[2] == 1 && d[a[0]][a[1]] < 0) {
					d[a[0]][a[1]] = d[x][z] + 1;
					queue.add(new int[] {a[0], a[1]});
				}
			}
		}
		return d;
	}

	/** True when cells (ax, az) and (bx, bz) are neighbours with an open passage. */
	public boolean passable(final int ax, final int az, final int bx, final int bz) {
		if (bx < 0 || bz < 0 || bx >= this.n || bz >= this.n || ax < 0 || az < 0 || ax >= this.n || az >= this.n) {
			return false;
		}
		if (ax == bx && Math.abs(az - bz) == 1) {
			return this.south[ax][Math.min(az, bz)];
		}
		if (az == bz && Math.abs(ax - bx) == 1) {
			return this.east[Math.min(ax, bx)][az];
		}
		return false;
	}

	public int openSides(final int x, final int z) {
		int open = 0;
		open += this.passable(x, z, x + 1, z) ? 1 : 0;
		open += this.passable(x, z, x - 1, z) ? 1 : 0;
		open += this.passable(x, z, x, z + 1) ? 1 : 0;
		open += this.passable(x, z, x, z - 1) ? 1 : 0;
		return open + (x == this.entranceX() && z == this.n - 1 ? 1 : 0);
	}

	/** Whether the wall block at local maze coordinates (0..size-1) is solid. */
	public boolean wall(final int lx, final int lz) {
		int size = this.n * LairPiece.CELL + 1;
		boolean onX = lx % LairPiece.CELL == 0;
		boolean onZ = lz % LairPiece.CELL == 0;
		if (onX && onZ) {
			return true;
		}
		if (onX) {
			int i = lx / LairPiece.CELL;
			int j = lz / LairPiece.CELL;
			return i == 0 || i == this.n || !this.east[i - 1][j];
		}
		if (onZ) {
			int i = lx / LairPiece.CELL;
			int j = lz / LairPiece.CELL;
			if (j == this.n && i == this.entranceX() && lz == size - 1) {
				return false;
			}
			return j == 0 || j == this.n || !this.south[i][j - 1];
		}
		return false;
	}

	public int cells() {
		return this.n;
	}

	public int entranceX() {
		return this.n / 2;
	}

	public int goalX() {
		return this.goalX;
	}

	public int goalZ() {
		return this.goalZ;
	}

	public GoalKind goalKind() {
		return this.goalKind;
	}

	public int distance(final int x, final int z) {
		return this.distance[x][z];
	}

	/** Dead-end cells with a small supply cache. */
	public List<int[]> caches() {
		return Collections.unmodifiableList(this.caches);
	}
}
