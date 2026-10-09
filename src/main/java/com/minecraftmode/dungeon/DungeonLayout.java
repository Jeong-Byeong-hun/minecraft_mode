package com.minecraftmode.dungeon;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Builds a dungeon run into the dungeon dimension along +x from its origin: the entrance, three halls and the boss room, joined by
 * corridors with a sealed door each. Door {@code i} leads from room {@code i} to room {@code i + 1} (room 0 is the entrance, 4 the
 * boss room). Runs are rebuilt (cleared first) every time, so a run never sees what the last one left behind.
 */
public final class DungeonLayout {
	/** A room's outer walls (relative x from the origin, z half width) and its ceiling height. */
	public record Room(int x0, int x1, int halfZ, int height) {
		public int centerX() {
			return (this.x0 + this.x1) / 2;
		}
	}

	public static final List<Room> ROOMS = List.of(
		new Room(0, 10, 5, 6),
		new Room(18, 34, 7, 7),
		new Room(42, 58, 7, 7),
		new Room(66, 82, 7, 7),
		new Room(90, 114, 11, 9));
	public static final int HALLS = 3;
	public static final int BOSS_ROOM = 4;
	private static final int CORRIDOR_HEIGHT = 4;
	private static final int UPDATE = Block.UPDATE_CLIENTS;

	/** The door between room {@code i} and {@code i + 1}: the corridor's middle. */
	public static int doorX(final int i) {
		return (ROOMS.get(i).x1() + ROOMS.get(i + 1).x0()) / 2;
	}

	/** Where players arrive (and come back after falling): the entrance, facing the first door. */
	public static Vec3 entrance(final BlockPos origin, final int index) {
		return new Vec3(origin.getX() + 3.5, origin.getY(), origin.getZ() + 0.5 + (index % 4 - 1.5) * 2.0);
	}

	public static Vec3 bossSpawn(final BlockPos origin) {
		Room boss = ROOMS.get(BOSS_ROOM);
		return new Vec3(origin.getX() + boss.x1() - 6 + 0.5, origin.getY(), origin.getZ() + 0.5);
	}

	/** Where room {@code room}'s monsters appear: spread over its far half. */
	public static Vec3 spawnPoint(final BlockPos origin, final int room, final RandomSource random) {
		Room r = ROOMS.get(room);
		double x = r.centerX() + 1 + random.nextInt(Math.max(1, (r.x1() - r.centerX()) - 2));
		double z = -r.halfZ() + 3 + random.nextInt(r.halfZ() * 2 - 5);
		return new Vec3(origin.getX() + x + 0.5, origin.getY(), origin.getZ() + z + 0.5);
	}

	/** True when {@code pos} is inside room {@code room} (past its entry wall). */
	public static boolean inRoom(final BlockPos origin, final int room, final Vec3 pos) {
		Room r = ROOMS.get(room);
		double x = pos.x - origin.getX();
		double z = pos.z - origin.getZ();
		return x > r.x0() + 1 && x < r.x1() && Math.abs(z) < r.halfZ() && pos.y > origin.getY() - 2 && pos.y < origin.getY() + r.height();
	}

	/** True when {@code pos} is anywhere in the run (rooms and corridors), with a margin. */
	public static boolean inside(final BlockPos origin, final Vec3 pos, final double margin) {
		return bounds(origin).inflate(margin).contains(pos);
	}

	public static AABB bounds(final BlockPos origin) {
		Room last = ROOMS.getLast();
		return new AABB(origin.getX() - 1, origin.getY() - 2, origin.getZ() - last.halfZ() - 1, origin.getX() + last.x1() + 2, origin.getY() + last.height() + 2,
			origin.getZ() + last.halfZ() + 2);
	}

	public static void build(final ServerLevel level, final BlockPos origin, final DungeonDef.Theme theme, final RandomSource random) {
		clear(level, origin);
		for (Room room : ROOMS) {
			room(level, origin, room, theme, random);
		}
		for (int i = 0; i < ROOMS.size() - 1; i++) {
			corridor(level, origin, ROOMS.get(i).x1(), ROOMS.get(i + 1).x0(), theme, random);
			setDoor(level, origin, i, true, theme);
		}
	}

	/** Seals ({@code closed}) or opens door {@code i}. */
	public static void setDoor(final ServerLevel level, final BlockPos origin, final int i, final boolean closed, final DungeonDef.Theme theme) {
		BlockState state = closed ? theme.door().defaultBlockState() : Blocks.AIR.defaultBlockState();
		int x = doorX(i);
		for (int z = -1; z <= 1; z++) {
			for (int y = 0; y < CORRIDOR_HEIGHT; y++) {
				level.setBlock(origin.offset(x, y, z), state, Block.UPDATE_ALL);
			}
		}
	}

	private static void room(final ServerLevel level, final BlockPos origin, final Room r, final DungeonDef.Theme theme, final RandomSource random) {
		BlockState wall = theme.wall().defaultBlockState();
		for (int x = r.x0(); x <= r.x1(); x++) {
			for (int z = -r.halfZ(); z <= r.halfZ(); z++) {
				boolean edge = x == r.x0() || x == r.x1() || z == -r.halfZ() || z == r.halfZ();
				boolean rim = !edge && (x == r.x0() + 1 || x == r.x1() - 1 || z == -r.halfZ() + 1 || z == r.halfZ() - 1);
				set(level, origin, x, -2, z, wall);
				set(level, origin, x, -1, z, edge ? wall : rim ? theme.trim().defaultBlockState() : pick(theme.floor(), random));
				for (int y = 0; y < r.height(); y++) {
					set(level, origin, x, y, z, edge ? wall : Blocks.AIR.defaultBlockState());
				}
				// ceiling with trim beams every fourth block
				set(level, origin, x, r.height(), z, !edge && (x - r.x0()) % 4 == 0 ? theme.trim().defaultBlockState() : wall);
			}
		}
		// pillars two blocks in from the corners
		for (int[] c : new int[][] {{r.x0() + 2, -r.halfZ() + 2}, {r.x1() - 2, -r.halfZ() + 2}, {r.x0() + 2, r.halfZ() - 2}, {r.x1() - 2, r.halfZ() - 2}}) {
			for (int y = 0; y < r.height(); y++) {
				set(level, origin, c[0], y, c[1], theme.pillar().defaultBlockState());
			}
			set(level, origin, c[0], r.height() - 2, c[1], theme.light().defaultBlockState());
		}
		// wall lights and hidden light blocks so the halls are never pitch dark
		for (int x = r.x0() + 4; x < r.x1() - 1; x += 5) {
			set(level, origin, x, 2, -r.halfZ(), theme.light().defaultBlockState());
			set(level, origin, x, 2, r.halfZ(), theme.light().defaultBlockState());
		}
		for (int x = r.x0() + 3; x < r.x1(); x += 5) {
			for (int z = -r.halfZ() + 3; z < r.halfZ(); z += 5) {
				set(level, origin, x, r.height() - 1, z, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 12));
			}
		}
	}

	private static void corridor(final ServerLevel level, final BlockPos origin, final int x0, final int x1, final DungeonDef.Theme theme, final RandomSource random) {
		BlockState wall = theme.wall().defaultBlockState();
		for (int x = x0; x <= x1; x++) {
			for (int z = -2; z <= 2; z++) {
				boolean side = Math.abs(z) == 2;
				boolean shared = x == x0 || x == x1;
				set(level, origin, x, -2, z, wall);
				if (!shared || !side) {
					set(level, origin, x, -1, z, side ? wall : pick(theme.floor(), random));
				}
				for (int y = 0; y < CORRIDOR_HEIGHT; y++) {
					if (side) {
						if (!shared) {
							set(level, origin, x, y, z, wall);
						}
					} else {
						set(level, origin, x, y, z, Blocks.AIR.defaultBlockState());
					}
				}
				if (!shared) {
					set(level, origin, x, CORRIDOR_HEIGHT, z, wall);
				}
			}
		}
		int mid = (x0 + x1) / 2;
		set(level, origin, mid - 2, 2, -2, theme.light().defaultBlockState());
		set(level, origin, mid + 2, 2, 2, theme.light().defaultBlockState());
	}

	/** Removes everything in the run's box. */
	public static void clear(final ServerLevel level, final BlockPos origin) {
		BlockState air = Blocks.AIR.defaultBlockState();
		Room last = ROOMS.getLast();
		for (int x = -2; x <= last.x1() + 2; x++) {
			for (int z = -last.halfZ() - 2; z <= last.halfZ() + 2; z++) {
				for (int y = -3; y <= last.height() + 2; y++) {
					BlockPos p = origin.offset(x, y, z);
					if (!level.getBlockState(p).isAir()) {
						level.setBlock(p, air, UPDATE);
					}
				}
			}
		}
	}

	private static void set(final ServerLevel level, final BlockPos origin, final int x, final int y, final int z, final BlockState state) {
		level.setBlock(origin.offset(x, y, z), state, UPDATE);
	}

	private static BlockState pick(final List<Block> blocks, final RandomSource random) {
		return blocks.get(random.nextInt(blocks.size())).defaultBlockState();
	}

	private DungeonLayout() {
	}
}
