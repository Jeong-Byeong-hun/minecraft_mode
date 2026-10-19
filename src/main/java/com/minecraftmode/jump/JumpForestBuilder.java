package com.minecraftmode.jump;

import com.minecraftmode.jump.JumpCourse.Climb;
import com.minecraftmode.jump.JumpCourse.Pad;
import com.minecraftmode.jump.JumpCourse.Surface;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Turns a {@link JumpCourse} into blocks: the forest floor, the giant trunk with its roots, glowing knots and moss, the footholds
 * (moss beds, leaves, mushroom caps, branches and posts) with the stems and branches that hold them, the climbs, the checkpoints
 * (moss with a verdant froglight in the middle), the crown with its golden goal and the canopy over it, a ring of old trees and a
 * hedge around the edge (with barrier walls behind, since the course floats in the void), lanterns and light. Every block comes from
 * the layout and a coordinate hash, so the course is the same every time; decorations never go where the layout keeps air
 * ({@link JumpCourse#reserved}).
 */
final class JumpForestBuilder {
	/** Lowest layer built (the dirt under the floor), relative to the floor. */
	static final int BOTTOM = -3;
	/** Blocks built above the crown (the canopy and the walls). */
	static final int ABOVE_TOP = 14;
	/** setBlock flags: tell clients, and never update neighbours' shapes (vines, lanterns and leaves stay as placed). */
	private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

	private final JumpCourse course;
	private final Map<Long, BlockState> plan = new HashMap<>();

	private JumpForestBuilder(final JumpCourse course) {
		this.course = course;
	}

	/** Builds the course with its corner at {@code origin} (the floor is at origin's y). Every chunk it covers must be loaded. */
	static void build(final ServerLevel level, final BlockPos origin, final JumpCourse course) {
		JumpForestBuilder builder = new JumpForestBuilder(course);
		builder.design();
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int top = course.top + ABOVE_TOP;
		for (int x = 0; x < JumpCourse.SIZE; x++) {
			for (int z = 0; z < JumpCourse.SIZE; z++) {
				for (int y = BOTTOM; y <= top; y++) {
					BlockState state = builder.plan.getOrDefault(JumpCourse.xyz(x, y, z), air);
					pos.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
					if (level.getBlockState(pos) != state) {
						level.setBlock(pos, state, FLAGS);
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------ the design

	private void design() {
		this.floor();
		this.trunk();
		this.forestRing();
		this.footholds();
		this.climbs();
		this.crown();
		this.groundCover();
		this.lights();
	}

	/** The forest floor: moss, grass, podzol and coarse dirt over dirt, a mossy cobblestone circle at the start, barriers all round. */
	private void floor() {
		for (int x = 0; x < JumpCourse.SIZE; x++) {
			for (int z = 0; z < JumpCourse.SIZE; z++) {
				boolean edge = x == 0 || z == 0 || x == JumpCourse.SIZE - 1 || z == JumpCourse.SIZE - 1;
				for (int y = BOTTOM; y < 0; y++) {
					this.put(x, y, z, Blocks.DIRT.defaultBlockState());
				}
				int h = hash(x, 0, z) % 100;
				BlockState top = h < 35 ? Blocks.MOSS_BLOCK.defaultBlockState() : h < 70 ? Blocks.GRASS_BLOCK.defaultBlockState()
					: h < 88 ? Blocks.PODZOL.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
				this.put(x, 0, z, top);
				if (edge) {
					for (int y = 1; y <= this.course.top + ABOVE_TOP; y++) {
						this.put(x, y, z, Blocks.BARRIER.defaultBlockState());
					}
				}
			}
		}
		for (long c : this.course.pads.getFirst().cells()) {
			this.put(x(c), 0, z(c), Blocks.MOSSY_COBBLESTONE.defaultBlockState());
		}
	}

	/** The giant trunk up to the crown, flaring into roots at the bottom, with moss patches and glowing knots (shroomlight). */
	private void trunk() {
		int c = JumpCourse.CENTER;
		for (int dx = -6; dx <= 6; dx++) {
			for (int dz = -6; dz <= 6; dz++) {
				int r2 = dx * dx + dz * dz;
				for (int y = 1; y < this.course.top; y++) {
					boolean core = r2 <= JumpCourse.TRUNK_RADIUS_SQ;
					// roots: wider at the bottom, with four arms reaching out along the diagonals
					boolean arm = Math.abs(Math.abs(dx) - Math.abs(dz)) <= 1 && r2 <= 26 - y * 6;
					boolean flare = r2 <= 18 - y * 4;
					if (!core && !arm && !flare) {
						continue;
					}
					int x = c + dx;
					int z = c + dz;
					BlockState state = log(Blocks.OAK_LOG, Direction.Axis.Y);
					boolean surface = r2 > 4;
					int h = hash(x, y, z) % 100;
					if (!core) {
						state = h < 30 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.OAK_WOOD.defaultBlockState();
					} else if (surface && h < 9) {
						state = Blocks.MOSS_BLOCK.defaultBlockState();
					} else if (surface && h < 11 && y > 3) {
						state = Blocks.SHROOMLIGHT.defaultBlockState();
					}
					this.decor(x, y, z, state);
				}
			}
		}
	}

	/** Old trees all round, out of reach of the path, and a leaf hedge along the barrier walls. */
	private void forestRing() {
		int[][] trees = {
			{5, 6}, {16, 4}, {27, 5}, {38, 4}, {43, 13}, {44, 25}, {42, 36}, {33, 43}, {21, 44}, {9, 42}, {4, 31}, {5, 19}
		};
		for (int i = 0; i < trees.length; i++) {
			int height = 30 + hash(i, 1, 7) % (this.course.top - 20);
			this.tree(trees[i][0], trees[i][1], height, i % 3 == 0 ? Blocks.SPRUCE_LOG : i % 3 == 1 ? Blocks.DARK_OAK_LOG : Blocks.OAK_LOG,
				i % 3 == 0 ? Blocks.SPRUCE_LEAVES : i % 3 == 1 ? Blocks.DARK_OAK_LEAVES : Blocks.OAK_LEAVES);
		}
		for (int x = 1; x < JumpCourse.SIZE - 1; x++) {
			for (int z = 1; z < JumpCourse.SIZE - 1; z++) {
				int edge = Math.min(Math.min(x, z), Math.min(JumpCourse.SIZE - 1 - x, JumpCourse.SIZE - 1 - z));
				if (edge > 2) {
					continue;
				}
				int height = (edge == 1 ? 7 : 3) + hash(x, 2, z) % 5;
				for (int y = 1; y <= height; y++) {
					this.decor(x, y, z, leaves(hash(x, y, z) % 4 == 0 ? Blocks.AZALEA_LEAVES : Blocks.OAK_LEAVES));
				}
			}
		}
	}

	/** A tree with a 2x2 trunk and a leaf cloud every few blocks up its top half. */
	private void tree(final int x0, final int z0, final int height, final Block log, final Block leaves) {
		for (int y = 1; y <= height; y++) {
			for (int dx = 0; dx <= 1; dx++) {
				for (int dz = 0; dz <= 1; dz++) {
					this.decor(x0 + dx, y, z0 + dz, log(log, Direction.Axis.Y));
				}
			}
		}
		for (int y = height / 2; y <= height + 2; y += 5) {
			int radius = y >= height ? 3 : 2 + hash(x0, y, z0) % 2;
			for (int dx = -radius; dx <= radius + 1; dx++) {
				for (int dz = -radius; dz <= radius + 1; dz++) {
					for (int dy = -1; dy <= 2; dy++) {
						double ex = dx - 0.5;
						double ez = dz - 0.5;
						if (ex * ex + ez * ez + dy * dy * 2.5 <= radius * radius + 1 && hash(x0 + dx, y + dy, z0 + dz) % 9 != 0) {
							this.decor(x0 + dx, y + dy, z0 + dz, leaves(leaves));
						}
					}
				}
			}
		}
	}

	private void footholds() {
		JumpCourse course = this.course;
		for (Pad pad : course.pads) {
			if (pad.surface() == Surface.GROUND || pad.surface() == Surface.GOAL) {
				continue;
			}
			int side = JumpCourse.side(pad.arc());
			Direction.Axis along = JumpCourse.direction(side)[0] != 0 ? Direction.Axis.X : Direction.Axis.Z;
			long middle = middle(pad);
			for (long c : pad.cells()) {
				int x = x(c);
				int z = z(c);
				BlockState state = switch (pad.surface()) {
					case MOSS -> Blocks.MOSS_BLOCK.defaultBlockState();
					case LEAVES -> leaves(Blocks.OAK_LEAVES);
					case AZALEA -> leaves(Blocks.AZALEA_LEAVES);
					case BLOSSOM -> leaves(Blocks.FLOWERING_AZALEA_LEAVES);
					case RED_CAP -> Blocks.RED_MUSHROOM_BLOCK.defaultBlockState();
					case BROWN_CAP -> Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState();
					case BRANCH -> log(Blocks.OAK_LOG, along);
					case POST -> log(Blocks.OAK_LOG, Direction.Axis.Y);
					case CHECKPOINT -> c == middle ? Blocks.VERDANT_FROGLIGHT.defaultBlockState() : Blocks.MOSS_BLOCK.defaultBlockState();
					default -> Blocks.MOSS_BLOCK.defaultBlockState();
				};
				this.put(x, pad.y(), z, state);
				// leaf beds are two thick where there is room
				if (pad.surface() == Surface.LEAVES || pad.surface() == Surface.AZALEA || pad.surface() == Surface.BLOSSOM) {
					this.decor(x, pad.y() - 1, z, leaves(Blocks.OAK_LEAVES));
				}
			}
			// what holds the foothold up: a stem under a cap, a post's own log, a branch from the trunk under the rest
			int mx = x(middle);
			int mz = z(middle);
			switch (pad.surface()) {
				case RED_CAP, BROWN_CAP -> this.column(mx, pad.y() - 1, mz, 3, Blocks.MUSHROOM_STEM.defaultBlockState());
				case POST -> this.column(mx, pad.y() - 1, mz, 3, log(Blocks.OAK_LOG, Direction.Axis.Y));
				default -> this.branchToTrunk(mx, pad.y() - 1, mz, side);
			}
			// a lantern under every third foothold's outer edge, every checkpoint's both ends
			if (pad.index() % 3 == 0 || pad.surface() == Surface.CHECKPOINT) {
				int[] n = JumpCourse.normal(side);
				for (long c : pad.cells()) {
					int x = x(c);
					int z = z(c);
					if (!pad.contains(x + n[0], z + n[1]) && (pad.surface() == Surface.CHECKPOINT ? c != middle && hash(x, pad.y(), z) % 2 == 0 : c == middle)) {
						this.lantern(x, pad.y() - 1, z);
					}
				}
			}
		}
	}

	/** A horizontal log from under a foothold in to the trunk (only when the trunk is straight in, near the middle of a side). */
	private void branchToTrunk(final int x, final int y, final int z, final int side) {
		int[] n = JumpCourse.normal(side);
		Direction.Axis axis = n[0] != 0 ? Direction.Axis.X : Direction.Axis.Z;
		for (int k = 0; k <= JumpCourse.RING; k++) {
			int bx = x - n[0] * k;
			int bz = z - n[1] * k;
			if (JumpCourse.trunk(bx, bz)) {
				return;
			}
			if (k > 0 && this.course.reserved.contains(JumpCourse.xyz(bx, y, bz))) {
				return;
			}
		}
		for (int k = 0; k <= JumpCourse.RING; k++) {
			int bx = x - n[0] * k;
			int bz = z - n[1] * k;
			if (JumpCourse.trunk(bx, bz)) {
				return;
			}
			this.decor(bx, y, bz, log(Blocks.OAK_LOG, axis));
		}
	}

	private void column(final int x, final int top, final int z, final int length, final BlockState state) {
		for (int y = top; y > top - length; y--) {
			if (!this.decor(x, y, z, state)) {
				return;
			}
		}
	}

	private void lantern(final int x, final int y, final int z) {
		this.decor(x, y, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
	}

	/** Vines (or a rope ladder) up the climbs, on log pillars. */
	private void climbs() {
		for (long p : this.course.pillars) {
			this.put((int)(p >> 32), (int)(short)(p >> 16 & 0xFFFF), (int)(p & 0xFFFF), log(Blocks.OAK_LOG, Direction.Axis.Y));
		}
		for (Climb climb : this.course.climbs) {
			Direction toPillar = Direction.getApproximateNearest(climb.dx(), 0, climb.dz());
			BlockState state = climb.ladder() ? Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, toPillar.getOpposite())
				: Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(toPillar), true);
			for (int h = 0; h < climb.height(); h++) {
				this.put(climb.x(), climb.y0() + h, climb.z(), state);
			}
		}
	}

	/** The crown: a moss disc on the trunk's top with the golden goal in the middle, the bridge out to the path, and the canopy. */
	private void crown() {
		Pad goal = this.course.pads.get(this.course.goalPad);
		int c = JumpCourse.CENTER;
		for (long cell : goal.cells()) {
			int x = x(cell);
			int z = z(cell);
			int dx = x - c;
			int dz = z - c;
			BlockState state;
			if (dx == 0 && dz == 0) {
				state = Blocks.GOLD_BLOCK.defaultBlockState();
			} else if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
				state = Blocks.OCHRE_FROGLIGHT.defaultBlockState();
			} else if (dx * dx + dz * dz > JumpCourse.CROWN_RADIUS_SQ) {
				state = log(Blocks.OAK_LOG, Math.abs(dx) > Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z);
			} else {
				state = hash(x, goal.y(), z) % 5 == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.MOSS_BLOCK.defaultBlockState();
			}
			this.put(x, goal.y(), z, state);
		}
		// the canopy: a wide dome of leaves well over the crown's headroom
		int base = goal.y() + JumpCourse.HEADROOM + 2;
		for (int dx = -11; dx <= 11; dx++) {
			for (int dz = -11; dz <= 11; dz++) {
				for (int dy = 0; dy <= 6; dy++) {
					double r = (dx * dx + dz * dz) / 121.0 + (dy - 2) * (dy - 2) / 16.0;
					if (r <= 1.0 && (r > 0.45 || dy >= 4) && hash(c + dx, base + dy, c + dz) % 11 != 0) {
						int h = hash(c + dx, base + dy, c + dz) % 6;
						this.decor(c + dx, base + dy, c + dz, leaves(h == 0 ? Blocks.FLOWERING_AZALEA_LEAVES : h == 1 ? Blocks.AZALEA_LEAVES : Blocks.OAK_LEAVES));
					}
				}
				// spore blossoms hang under the canopy here and there
				if (dx * dx + dz * dz <= 64 && hash(c + dx, base, c + dz) % 13 == 0) {
					this.decor(c + dx, base - 1, c + dz, Blocks.SPORE_BLOSSOM.defaultBlockState());
				}
			}
		}
		// the trunk goes on through the canopy
		for (int y = goal.y() + JumpCourse.HEADROOM + 1; y <= base + 6; y++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (Math.abs(dx) + Math.abs(dz) <= 1) {
						this.decor(c + dx, y, c + dz, log(Blocks.OAK_LOG, Direction.Axis.Y));
					}
				}
			}
		}
	}

	/** Grass, ferns, flowers, mushrooms, fallen leaves and fireflies on the forest floor. */
	private void groundCover() {
		for (int x = 1; x < JumpCourse.SIZE - 1; x++) {
			for (int z = 1; z < JumpCourse.SIZE - 1; z++) {
				if (this.plan.containsKey(JumpCourse.xyz(x, 1, z))) {
					continue;
				}
				BlockState ground = this.plan.get(JumpCourse.xyz(x, 0, z));
				if (ground == null || ground.is(Blocks.MOSSY_COBBLESTONE)) {
					continue;
				}
				int h = hash(x, 1, z) % 100;
				BlockState state;
				if (ground.is(Blocks.PODZOL) && h < 12) {
					state = (h % 2 == 0 ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState();
				} else if (h < 28) {
					state = Blocks.SHORT_GRASS.defaultBlockState();
				} else if (h < 38) {
					state = Blocks.FERN.defaultBlockState();
				} else if (h < 46) {
					state = Blocks.LEAF_LITTER.defaultBlockState();
				} else if (h < 50) {
					state = Blocks.LILY_OF_THE_VALLEY.defaultBlockState();
				} else if (h < 53) {
					state = Blocks.BLUE_ORCHID.defaultBlockState();
				} else if (h < 56) {
					state = Blocks.FIREFLY_BUSH.defaultBlockState();
				} else if (h < 58) {
					state = Blocks.AZALEA.defaultBlockState();
				} else if (h < 64 && ground.is(Blocks.MOSS_BLOCK)) {
					state = Blocks.MOSS_CARPET.defaultBlockState();
				} else {
					continue;
				}
				this.decor(x, 1, z, state);
			}
		}
	}

	/** Invisible light blocks through the air (the dungeon dimension has no sun), dimmer near the floor. */
	private void lights() {
		for (int x = 3; x < JumpCourse.SIZE - 2; x += 5) {
			for (int z = 3; z < JumpCourse.SIZE - 2; z += 5) {
				for (int y = 2; y <= this.course.top + 6; y += 5) {
					if (!this.plan.containsKey(JumpCourse.xyz(x, y, z))) {
						this.plan.put(JumpCourse.xyz(x, y, z), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, y < 6 ? 10 : 13));
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------ helpers

	/** Places a structural block (overrides decorations). */
	private void put(final int x, final int y, final int z, final BlockState state) {
		this.plan.put(JumpCourse.xyz(x, y, z), state);
	}

	/** Places a decoration unless that air belongs to the path or something is already there; false when it was not placed. */
	private boolean decor(final int x, final int y, final int z, final BlockState state) {
		if (x < 1 || z < 1 || x >= JumpCourse.SIZE - 1 || z >= JumpCourse.SIZE - 1) {
			return false;
		}
		long key = JumpCourse.xyz(x, y, z);
		if (this.course.reserved.contains(key) || this.plan.containsKey(key) || this.course.pillars.contains(key)) {
			return false;
		}
		this.plan.put(key, state);
		return true;
	}

	/** The cell of {@code pad} nearest its middle. */
	static long middle(final Pad pad) {
		double mx = pad.cells().stream().mapToInt(JumpForestBuilder::x).average().orElse(0);
		double mz = pad.cells().stream().mapToInt(JumpForestBuilder::z).average().orElse(0);
		long best = pad.cells().getFirst();
		double bestDistance = Double.MAX_VALUE;
		for (long c : pad.cells()) {
			double d = (x(c) - mx) * (x(c) - mx) + (z(c) - mz) * (z(c) - mz);
			if (d < bestDistance) {
				bestDistance = d;
				best = c;
			}
		}
		return best;
	}

	static int x(final long cell) {
		return (int)(cell >> 16);
	}

	static int z(final long cell) {
		return (int)(cell & 0xFFFF);
	}

	private static BlockState log(final Block log, final Direction.Axis axis) {
		return log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
	}

	private static BlockState leaves(final Block leaves) {
		return leaves.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
	}

	private static int hash(final int x, final int y, final int z) {
		int h = x * 73856093 ^ y * 19349663 ^ z * 83492791;
		h ^= h >>> 13;
		h *= 0x5bd1e995;
		h ^= h >>> 15;
		return h & 0x7FFFFFFF;
	}
}
