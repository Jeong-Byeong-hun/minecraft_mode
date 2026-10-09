package com.minecraftmode.worldgen.lair;

import com.minecraftmode.entity.named.NamedMob;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.registry.ModBlocks;
import com.minecraftmode.worldgen.lair.LairDef.Decor;
import com.minecraftmode.worldgen.lair.LairDef.Palette;
import com.minecraftmode.worldgen.lair.LairDef.Setting;
import com.minecraftmode.worldgen.lair.LairDef.Shape;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.jspecify.annotations.Nullable;

/**
 * Builds one lair, clipped to whichever chunk is being decorated. Everything is a pure function of
 * the lair's position and seed, so the chunks agree on one maze and one goal. The treasure and the caches are personal
 * {@link LairChestBlock lair chests}; the treasure wakes the lair's lord each cycle.
 * <p>
 * Layout: a square maze of 3-wide, 4-high corridors (cells of {@link #CELL} blocks) with a floor and a
 * roof, the entrance in the middle of the south wall, an apron of {@link #APRON} blocks around it, and
 * the landmark on the roof. The piece's box also covers {@link #GROUNDS} blocks of land around the lair:
 * the lair's spawn list applies in all of it.
 */
public class LairPiece extends StructurePiece {
	public static final int CELL = 4;
	public static final int APRON = 6;
	public static final int GROUNDS = 40;
	/** Corridor height: air from floor + 1 to floor + 4, roof at floor + 5. */
	private static final int ROOF = 5;

	private final String lair;
	private final int cx;
	private final int cz;
	private final int floor;
	private final int surface;
	private final long seed;
	private @Nullable Maze maze;

	public LairPiece(final LairDef def, final int cx, final int floor, final int cz, final int surface, final long seed) {
		super(NamedLairs.PIECE, 0, box(def, cx, floor, cz, surface));
		this.lair = def.id();
		this.cx = cx;
		this.cz = cz;
		this.floor = floor;
		this.surface = surface;
		this.seed = seed;
	}

	public LairPiece(final CompoundTag tag) {
		super(NamedLairs.PIECE, tag);
		this.lair = tag.getStringOr("Lair", "");
		this.cx = tag.getIntOr("CX", 0);
		this.cz = tag.getIntOr("CZ", 0);
		this.floor = tag.getIntOr("Floor", 0);
		this.surface = tag.getIntOr("Surface", 0);
		this.seed = tag.getLongOr("Seed", 0L);
	}

	@Override
	protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
		tag.putString("Lair", this.lair);
		tag.putInt("CX", this.cx);
		tag.putInt("CZ", this.cz);
		tag.putInt("Floor", this.floor);
		tag.putInt("Surface", this.surface);
		tag.putLong("Seed", this.seed);
	}

	private static BoundingBox box(final LairDef def, final int cx, final int floor, final int cz, final int surface) {
		int reach = def.size() / 2 + APRON + GROUNDS;
		int minY = def.setting() == Setting.UNDERGROUND ? floor - 6 : floor - 32;
		int maxY = def.setting() == Setting.UNDERGROUND ? surface + 32 : floor + topOffset(def) + 20;
		return new BoundingBox(cx - reach, minY, cz - reach, cx + reach, maxY, cz + reach);
	}

	/** Height of the landmark's top above the floor. */
	public static int topOffset(final LairDef def) {
		int half = def.size() / 2;
		return ROOF + switch (def.shape()) {
			case PYRAMID, DOME -> half + 3;
			case ZIGGURAT -> half + 10;
			case FORTRESS -> 24;
			case SPIRE -> def.size() + 8;
			case TREE -> 52;
			case NONE -> 2;
		};
	}

	/** Z of the shaft under an underground lair's tower, just south of the entrance. */
	public static int shaftZ(final LairDef def, final int cz) {
		return cz + def.size() / 2 + 4;
	}

	public @Nullable LairDef def() {
		return NamedLairs.def(this.lair);
	}

	public Maze maze() {
		LairDef def = this.def();
		if (this.maze == null && def != null) {
			this.maze = new Maze(def.cells(), this.seed);
		}
		return this.maze;
	}

	/** World position of the goal chest. */
	public BlockPos goalPos() {
		LairDef def = this.def();
		Maze m = this.maze();
		int x0 = this.cx - def.size() / 2;
		int z0 = this.cz - def.size() / 2;
		return new BlockPos(x0 + 2 + CELL * m.goalX(), this.floor + 1, z0 + 2 + CELL * m.goalZ());
	}

	public int floor() {
		return this.floor;
	}

	@Override
	public void postProcess(final WorldGenLevel level, final StructureManager structureManager, final ChunkGenerator generator, final RandomSource random,
		final BoundingBox chunkBB, final ChunkPos chunkPos, final BlockPos referencePos) {
		LairDef def = this.def();
		if (def == null) {
			return;
		}
		Maze m = this.maze();
		Placer p = new Placer(level, chunkBB);
		int size = def.size();
		int x0 = this.cx - size / 2;
		int z0 = this.cz - size / 2;
		if (def.setting() == Setting.UNDERGROUND) {
			this.underground(p, def, x0, z0, size);
		} else {
			this.ground(p, def, x0, z0, size);
		}
		this.mazeBlocks(p, def, m, x0, z0, size);
		if (def.setting() != Setting.UNDERGROUND) {
			this.landmark(p, def, x0, z0, size);
		}
		this.gate(p, def, m, x0, z0, size);
		this.goal(p, level, def, m, x0, z0);
		for (int[] cache : m.caches()) {
			BlockPos at = new BlockPos(x0 + 2 + CELL * cache[0], this.floor + 1, z0 + 2 + CELL * cache[1]);
			if (chunkBB.isInside(at)) {
				p.set(at.getX(), at.getY(), at.getZ(), ModBlocks.LAIR_CACHE.defaultBlockState());
				if (level.getBlockEntity(at) instanceof LairChestBlockEntity cacheChest) {
					cacheChest.setup(def.id(), this.seed ^ at.asLong(), true);
				}
			}
		}
	}

	// ------------------------------------------------------------------ ground work

	/** Clears the space above the lair and its apron and builds a foundation under them. */
	private void ground(final Placer p, final LairDef def, final int x0, final int z0, final int size) {
		Palette pal = def.palette();
		int top = this.floor + topOffset(def) + 4;
		int extra = APRON;
		for (int x = x0 - extra; x < x0 + size + extra; x++) {
			for (int z = z0 - extra; z < z0 + size + extra; z++) {
				if (!p.column(x, z)) {
					continue;
				}
				for (int y = this.floor + 1; y <= top; y++) {
					p.clear(x, y, z);
				}
				boolean inside = x >= x0 && x < x0 + size && z >= z0 && z < z0 + size;
				boolean edge = !inside && (x == x0 - extra || z == z0 - extra || x == x0 + size + extra - 1 || z == z0 + size + extra - 1);
				p.set(x, this.floor, z, inside ? pal.floor() : edge ? pal.accent() : pal.foundation());
				for (int y = this.floor - 1; y > this.floor - 40; y--) {
					BlockState below = p.get(x, y, z);
					if (!(below.isAir() || !below.getFluidState().isEmpty() || below.canBeReplaced() || below.is(BlockTags.LEAVES) || below.is(BlockTags.LOGS))) {
						break;
					}
					p.set(x, y, z, pal.foundation());
				}
			}
		}
	}

	/** Seals an underground lair in its own shell and digs the shaft and tower that lead down to it. */
	private void underground(final Placer p, final LairDef def, final int x0, final int z0, final int size) {
		Palette pal = def.palette();
		for (int x = x0 - 1; x <= x0 + size; x++) {
			for (int z = z0 - 1; z <= z0 + size; z++) {
				if (!p.column(x, z)) {
					continue;
				}
				p.set(x, this.floor - 1, z, pal.foundation());
				p.set(x, this.floor + ROOF + 1, z, pal.wall());
				if (x == x0 - 1 || z == z0 - 1 || x == x0 + size || z == z0 + size) {
					for (int y = this.floor; y <= this.floor + ROOF; y++) {
						p.set(x, y, z, pal.wall());
					}
				}
			}
		}
		int sx = this.cx;
		int sz = shaftZ(def, this.cz);
		// corridor from the maze gate to the shaft
		for (int x = sx - 2; x <= sx + 2; x++) {
			for (int z = z0 + size; z <= sz - 2; z++) {
				if (!p.column(x, z)) {
					continue;
				}
				boolean side = Math.abs(x - sx) == 2;
				p.set(x, this.floor, z, pal.floor());
				p.set(x, this.floor + ROOF, z, pal.roof());
				for (int y = this.floor + 1; y < this.floor + ROOF; y++) {
					p.set(x, y, z, side ? pal.wall().defaultBlockState() : Blocks.CAVE_AIR.defaultBlockState());
				}
			}
		}
		// shaft with a spiral stair around a central pillar
		int[][] ring = {{0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};
		for (int x = sx - 2; x <= sx + 2; x++) {
			for (int z = sz - 2; z <= sz + 2; z++) {
				if (!p.column(x, z)) {
					continue;
				}
				int dx = x - sx;
				int dz = z - sz;
				boolean wall = Math.abs(dx) == 2 || Math.abs(dz) == 2;
				p.set(x, this.floor, z, pal.floor());
				for (int y = this.floor + 1; y <= this.surface; y++) {
					if (wall) {
						boolean door = dz == -2 && Math.abs(dx) <= 1 && y < this.floor + ROOF;
						boolean lamp = (y - this.floor) % 7 == 0 && Math.abs(dx) == 2 && Math.abs(dz) == 2;
						p.set(x, y, z, door ? Blocks.CAVE_AIR.defaultBlockState() : (lamp ? pal.light() : pal.wall()).defaultBlockState());
					} else if (dx == 0 && dz == 0) {
						p.set(x, y, z, pal.accent());
					} else {
						int step = y - this.floor - 1;
						int[] at = ring[Math.floorMod(step, ring.length)];
						if (at[0] == dx && at[1] == dz) {
							int[] next = ring[Math.floorMod(step + 1, ring.length)];
							Direction facing = horizontal(next[0] - dx, next[1] - dz);
							p.set(x, y, z, facing == null ? pal.wall().defaultBlockState()
								: pal.stairs().defaultBlockState().setValue(StairBlock.FACING, facing));
						} else {
							p.set(x, y, z, Blocks.CAVE_AIR.defaultBlockState());
						}
					}
				}
			}
		}
		this.tower(p, def, sx, sz);
	}

	/** The landmark of an underground lair: a tall tower over the shaft with a beacon on top. */
	private void tower(final Placer p, final LairDef def, final int sx, final int sz) {
		Palette pal = def.palette();
		int s = this.surface;
		for (int x = sx - 5; x <= sx + 5; x++) {
			for (int z = sz - 5; z <= sz + 5; z++) {
				if (!p.column(x, z)) {
					continue;
				}
				int dx = x - sx;
				int dz = z - sz;
				boolean shaft = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
				boolean wall = Math.abs(dx) == 4 || Math.abs(dz) == 4;
				boolean outside = Math.abs(dx) == 5 || Math.abs(dz) == 5;
				for (int y = s + 1; y <= s + 26; y++) {
					p.clear(x, y, z);
				}
				for (int y = s - 1; y > s - 16 && (Math.abs(dx) > 2 || Math.abs(dz) > 2); y--) {
					BlockState below = p.get(x, y, z);
					if (!(below.isAir() || !below.getFluidState().isEmpty() || below.canBeReplaced() || below.is(BlockTags.LEAVES))) {
						break;
					}
					p.set(x, y, z, pal.foundation());
				}
				if (outside) {
					p.set(x, s, z, pal.accent());
					continue;
				}
				if (!shaft) {
					p.set(x, s, z, pal.floor());
				}
				if (wall) {
					boolean door = dz == 4 && Math.abs(dx) <= 1;
					for (int y = s + 1; y <= s + 18; y++) {
						if (door && y <= s + 3) {
							continue;
						}
						boolean corner = Math.abs(dx) == 4 && Math.abs(dz) == 4;
						boolean window = !corner && (dx == 0 || dz == 0) && (y - s) % 6 == 0;
						p.set(x, y, z, window ? pal.light() : corner || (y - s) % 6 == 5 ? pal.accent() : pal.wall());
					}
					boolean crenel = (dx + dz) % 2 == 0;
					if (crenel) {
						p.set(x, s + 19, z, pal.accent());
					}
				} else {
					p.set(x, s + 18, z, pal.roof());
				}
			}
		}
		// beacon spike
		for (int y = s + 19; y <= s + 23; y++) {
			p.set(sx, y, sz, pal.accent());
		}
		p.set(sx, s + 24, sz, pal.light());
	}

	// ------------------------------------------------------------------ the maze

	private void mazeBlocks(final Placer p, final LairDef def, final Maze m, final int x0, final int z0, final int size) {
		Palette pal = def.palette();
		int gx = m.goalX();
		int gz = m.goalZ();
		for (int x = x0; x < x0 + size; x++) {
			for (int z = z0; z < z0 + size; z++) {
				if (!p.column(x, z)) {
					continue;
				}
				int lx = x - x0;
				int lz = z - z0;
				boolean wall = m.wall(lx, lz);
				boolean pillar = lx % CELL == 0 && lz % CELL == 0;
				p.set(x, this.floor, z, pal.floor());
				for (int y = this.floor + 1; y < this.floor + ROOF; y++) {
					p.set(x, y, z, wall ? (pillar ? pal.accent() : pal.wall()).defaultBlockState() : Blocks.CAVE_AIR.defaultBlockState());
				}
				p.set(x, this.floor + ROOF, z, pal.roof());
				if (wall) {
					continue;
				}
				int cellX = lx / CELL;
				int cellZ = lz / CELL;
				boolean special = (cellX == gx && cellZ == gz) || (cellX == m.entranceX() && cellZ == m.cells() - 1) || isCache(m, cellX, cellZ);
				if (special) {
					continue;
				}
				List<Decor> decor = def.decor();
				for (int i = 0; i < decor.size(); i++) {
					Decor d = decor.get(i);
					if (hash(x, i, z, this.seed) % 1000 < d.perMille()) {
						int y = switch (d.place()) {
							case FLOOR -> this.floor;
							case GROUND -> this.floor + 1;
							case CEILING -> this.floor + ROOF - 1;
						};
						p.set(x, y, z, d.block());
						break;
					}
				}
			}
		}
	}

	private static boolean isCache(final Maze m, final int x, final int z) {
		for (int[] c : m.caches()) {
			if (c[0] == x && c[1] == z) {
				return true;
			}
		}
		return false;
	}

	/** The gate in the south wall: a framed opening with two lights and a short paved approach. */
	private void gate(final Placer p, final LairDef def, final Maze m, final int x0, final int z0, final int size) {
		Palette pal = def.palette();
		int left = x0 + 1 + CELL * m.entranceX();
		int right = left + 2;
		int zWall = z0 + size - 1;
		for (int x = left - 1; x <= right + 1; x++) {
			boolean frame = x == left - 1 || x == right + 1;
			for (int y = this.floor + 1; y <= this.floor + ROOF + 1; y++) {
				if (frame || y >= this.floor + ROOF) {
					p.set(x, y, zWall + 1, y == this.floor + 3 && frame ? pal.light() : pal.accent());
				} else {
					p.set(x, y, zWall + 1, Blocks.AIR);
				}
			}
			if (def.setting() != Setting.UNDERGROUND && !frame) {
				for (int z = zWall + 1; z <= zWall + APRON; z++) {
					p.set(x, this.floor, z, pal.accent());
					for (int y = this.floor + 1; y <= this.floor + 4; y++) {
						p.clear(x, y, z);
					}
				}
			}
		}
	}

	/** The goal: a lit room with the treasure chest; its lord wakes when someone comes near (once per cycle). */
	private void goal(final Placer p, final WorldGenLevel level, final LairDef def, final Maze m, final int x0, final int z0) {
		Palette pal = def.palette();
		int gx = x0 + 2 + CELL * m.goalX();
		int gz = z0 + 2 + CELL * m.goalZ();
		for (int x = gx - 1; x <= gx + 1; x++) {
			for (int z = gz - 1; z <= gz + 1; z++) {
				p.set(x, this.floor, z, x == gx && z == gz ? pal.light() : pal.accent());
			}
		}
		p.set(gx, this.floor + ROOF, gz, pal.light());
		BlockPos chest = new BlockPos(gx, this.floor + 1, gz);
		if (!p.box().isInside(chest)) {
			return;
		}
		p.set(gx, this.floor + 1, gz, ModBlocks.LAIR_CHEST.defaultBlockState());
		if (level.getBlockEntity(chest) instanceof LairChestBlockEntity treasure) {
			treasure.setup(def.id(), this.seed, false);
		}
	}

	// ------------------------------------------------------------------ landmarks

	private void landmark(final Placer p, final LairDef def, final int x0, final int z0, final int size) {
		switch (def.shape()) {
			case PYRAMID -> this.pyramid(p, def, x0, z0, size);
			case ZIGGURAT -> this.ziggurat(p, def, x0, z0, size);
			case FORTRESS -> this.fortress(p, def, x0, z0, size);
			case DOME -> this.dome(p, def, x0, z0, size);
			case SPIRE -> this.spire(p, def, x0, z0, size);
			case TREE -> this.tree(p, def, x0, z0, size);
			case NONE -> {
			}
		}
	}

	private void pyramid(final Placer p, final LairDef def, final int x0, final int z0, final int size) {
		Palette pal = def.palette();
		int roof = this.floor + ROOF;
		int half = size / 2;
		for (int x = x0; x < x0 + size; x++) {
			for (int z = z0; z < z0 + size; z++) {
				if (!p.column(x, z)) {
					continue;
				}
				int k = inset(x - x0, z - z0, size);
				int y = roof + 1 + k;
				if (k == half) {
					p.set(x, y, z, pal.accent());
					p.set(x, y + 1, z, pal.light());
					continue;
				}
				p.set(x, y, z, k % 6 == 5 || k >= half - 2 ? pal.accent() : pal.roof());
			}
		}
	}

	private void ziggurat(final Placer p, final LairDef def, final int x0, final int z0, final int size) {
		Palette pal = def.palette();
		int roof = this.floor + ROOF;
		int half = size / 2;
		int tier = 6;
		int tiers = Math.max(1, (half - 6) / tier);
		int topInset = tier * tiers;
		int topY = roof + tier * tiers;
		for (int x = x0; x < x0 + size; x++) {
			for (int z = z0; z < z0 + size; z++) {
				if (!p.column(x, z)) {
					continue;
				}
				int lx = x - x0;
				int lz = z - z0;
				int k = inset(lx, lz, size);
				boolean stair = Math.abs(x - this.cx) <= 1 && lz > half && k < topInset;
				boolean rail = Math.abs(x - this.cx) == 2 && lz > half && k < topInset;
				if (k < topInset) {
					int t = k / tier;
					int terrace = roof + tier * (t + 1);
					if (k % tier == 0) {
						for (int y = roof + tier * t + 1; y < terrace; y++) {
							p.set(x, y, z, y == terrace - 1 ? pal.accent() : pal.wall());
						}
					}
					p.set(x, terrace, z, pal.roof());
				} else {
					// top platform and a small temple
					p.set(x, topY, z, pal.accent());
					int d = k - topInset;
					int templeEdge = 2;
					if (d == templeEdge) {
						boolean opening = Math.abs(x - this.cx) <= 1 || Math.abs(z - this.cz) <= 1;
						for (int y = topY + 1; y <= topY + 5; y++) {
							if (!opening || y > topY + 3) {
								p.set(x, y, z, (x + z) % 4 == 0 ? pal.accent() : pal.wall());
							}
						}
					}
					if (d >= templeEdge) {
						p.set(x, topY + 6, z, pal.roof());
					}
					if (x == this.cx && z == this.cz) {
						p.set(x, topY + 7, z, pal.accent());
						p.set(x, topY + 8, z, pal.light());
					}
				}
				if (stair) {
					int y = roof + 1 + k;
					for (int a = y + 1; a <= y + 4; a++) {
						p.set(x, a, z, Blocks.AIR);
					}
					p.set(x, y, z, pal.stairs().defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
				} else if (rail) {
					int y = roof + 1 + k;
					p.set(x, y, z, pal.wall());
					p.set(x, y + 1, z, pal.accent());
				}
			}
		}
	}

	private void fortress(final Placer p, final LairDef def, final int x0, final int z0, final int size) {
		Palette pal = def.palette();
		int roof = this.floor + ROOF;
		int x1 = x0 + size - 1;
		int z1 = z0 + size - 1;
		// curtain wall over the maze's outer wall, with battlements
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				if (!p.column(x, z) || !(x == x0 || z == z0 || x == x1 || z == z1)) {
					continue;
				}
				for (int y = roof + 1; y <= roof + 7; y++) {
					p.set(x, y, z, y == roof + 4 ? pal.accent() : pal.wall());
				}
				if ((x + z) % 2 == 0) {
					p.set(x, roof + 8, z, pal.wall());
				}
			}
		}
		// four corner towers
		int[][] corners = {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}};
		for (int[] c : corners) {
			this.towerBlock(p, pal, c[0], c[1], 5, this.floor + 1, roof + 18, roof);
		}
		// central keep
		this.towerBlock(p, pal, this.cx, this.cz, 8, roof + 1, roof + 17, roof);
		for (int y = roof + 18; y <= roof + 24; y++) {
			p.set(this.cx, y, this.cz, pal.accent());
		}
		p.set(this.cx, roof + 25, this.cz, pal.light());
	}

	/**
	 * A square tower of radius {@code r} around (tx, tz): walls from {@code bottom} to {@code top}, a roof and battlements. Inside the maze
	 * footprint it only starts above the maze roof so the corridors stay intact.
	 */
	private void towerBlock(final Placer p, final Palette pal, final int tx, final int tz, final int r, final int bottom, final int top, final int roof) {
		LairDef def = this.def();
		int size = def.size();
		int x0 = this.cx - size / 2;
		int z0 = this.cz - size / 2;
		for (int x = tx - r; x <= tx + r; x++) {
			for (int z = tz - r; z <= tz + r; z++) {
				if (!p.column(x, z)) {
					continue;
				}
				boolean inMaze = x >= x0 && x < x0 + size && z >= z0 && z < z0 + size;
				int from = inMaze ? Math.max(bottom, roof + 1) : bottom;
				boolean ring = Math.abs(x - tx) == r || Math.abs(z - tz) == r;
				if (ring) {
					for (int y = from; y <= top; y++) {
						boolean window = (y - bottom) % 6 == 3 && (x == tx || z == tz);
						p.set(x, y, z, window ? pal.light() : (y - bottom) % 6 == 5 ? pal.accent() : pal.wall());
					}
					if ((x + z) % 2 == 0) {
						p.set(x, top + 1, z, pal.accent());
					}
				} else if (!inMaze) {
					for (int y = from; y < top; y++) {
						p.clear(x, y, z);
					}
				}
				if (!ring) {
					p.set(x, top, z, pal.roof());
				}
			}
		}
		p.set(tx, top + 1, tz, pal.light());
	}

	private void dome(final Placer p, final LairDef def, final int x0, final int z0, final int size) {
		Palette pal = def.palette();
		int roof = this.floor + ROOF;
		double r = size / 2.0 - 1.0;
		for (int x = x0; x < x0 + size; x++) {
			for (int z = z0; z < z0 + size; z++) {
				if (!p.column(x, z)) {
					continue;
				}
				double d = Math.hypot(x - this.cx, z - this.cz);
				if (d > r) {
					if ((x + z) % 2 == 0) {
						p.set(x, roof + 1, z, pal.accent());
					}
					continue;
				}
				int out = roof + (int)Math.round(Math.sqrt(r * r - d * d));
				int in = d < r - 2.0 ? roof + (int)Math.round(Math.sqrt((r - 2.0) * (r - 2.0) - d * d)) : roof;
				for (int y = Math.max(roof + 1, in); y <= out; y++) {
					boolean band = (y - roof) % 8 == 7;
					boolean rib = Math.abs(x - this.cx) <= 0 || Math.abs(z - this.cz) <= 0;
					p.set(x, y, z, d < 2.5 ? pal.light() : band || rib ? pal.accent() : pal.roof());
				}
			}
		}
		p.set(this.cx, roof + (int)Math.round(r) + 1, this.cz, pal.accent());
		p.set(this.cx, roof + (int)Math.round(r) + 2, this.cz, pal.light());
	}

	private void spire(final Placer p, final LairDef def, final int x0, final int z0, final int size) {
		int roof = this.floor + ROOF;
		int quarter = size / 4;
		double base = Math.max(6.0, size / 6.0);
		this.spireAt(p, def, this.cx, this.cz, base, size + 4, roof);
		this.spireAt(p, def, this.cx - quarter, this.cz - quarter, base / 2.0 + 1.0, size / 2, roof);
		this.spireAt(p, def, this.cx + quarter, this.cz - quarter, base / 2.0 + 1.0, size / 2, roof);
		this.spireAt(p, def, this.cx - quarter, this.cz + quarter, base / 2.0 + 1.0, size / 2, roof);
		this.spireAt(p, def, this.cx + quarter, this.cz + quarter, base / 2.0 + 1.0, size / 2, roof);
	}

	private void spireAt(final Placer p, final LairDef def, final int sx, final int sz, final double base, final int height, final int roof) {
		Palette pal = def.palette();
		int reach = (int)Math.ceil(base);
		for (int x = sx - reach; x <= sx + reach; x++) {
			for (int z = sz - reach; z <= sz + reach; z++) {
				if (!p.column(x, z)) {
					continue;
				}
				double d = Math.hypot(x - sx, z - sz);
				for (int h = 1; h <= height; h++) {
					double r = Math.max(0.6, base * Math.pow(1.0 - (double)h / height, 0.85));
					if (d > r) {
						continue;
					}
					boolean shell = d > r - 1.4;
					boolean deck = h % 10 == 0;
					if (shell || deck) {
						boolean window = shell && h % 10 == 5 && (x == sx || z == sz);
						p.set(x, roof + h, z, window ? pal.light() : h % 10 == 0 || h % 10 == 9 ? pal.accent() : pal.wall());
					}
				}
			}
		}
		p.set(sx, roof + height + 1, sz, pal.accent());
		p.set(sx, roof + height + 2, sz, pal.light());
	}

	private void tree(final Placer p, final LairDef def, final int x0, final int z0, final int size) {
		Palette pal = def.palette();
		int roof = this.floor + ROOF;
		int half = size / 2;
		double trunk = 5.0;
		int crown = roof + 36;
		double canopy = Math.min(17.0, half - 4.0);
		BlockState leaves = pal.leaves() == null ? null : leaves(pal.leaves());
		for (int x = x0; x < x0 + size; x++) {
			for (int z = z0; z < z0 + size; z++) {
				if (!p.column(x, z)) {
					continue;
				}
				int dx = x - this.cx;
				int dz = z - this.cz;
				double d = Math.hypot(dx, dz);
				boolean diagonal = Math.abs(Math.abs(dx) - Math.abs(dz)) <= 1;
				boolean axis = Math.abs(dx) <= 1 || Math.abs(dz) <= 1;
				// solid trunk with knotted bark bands
				if (d <= trunk) {
					for (int y = roof + 1; y <= roof + 32; y++) {
						boolean band = d > trunk - 1.0 && (y - roof) % 6 == 0;
						p.set(x, y, z, band ? pal.accent() : pal.wall());
					}
				}
				// roots spreading over the roof
				if ((diagonal || axis) && d > trunk && d < half - 1) {
					int h = (int)Math.round(6.0 * (1.0 - (d - trunk) / (half - trunk)));
					for (int y = roof + 1; y <= roof + 1 + h; y++) {
						p.set(x, y, z, pal.wall());
					}
				}
				// branches rising out of the crown
				if (diagonal && d > trunk && d <= canopy) {
					int y = roof + 24 + (int)Math.round((d - trunk) * 0.7);
					p.set(x, y, z, pal.wall());
					p.set(x, y + 1, z, pal.wall());
					if (leaves == null && d > canopy - 2.0) {
						p.set(x, y + 2, z, pal.accent());
						p.set(x, y + 3, z, pal.accent());
					}
				}
				// canopy: a thick shell of leaves with lights inside
				if (leaves != null && d <= canopy) {
					double dy = Math.sqrt(canopy * canopy - d * d) * 0.6;
					int topY = crown + (int)Math.round(dy);
					int bottomY = crown - (int)Math.round(dy * 0.5);
					for (int y = topY - 2; y <= topY; y++) {
						p.set(x, y, z, leaves);
					}
					for (int y = bottomY; y <= bottomY + 1; y++) {
						p.set(x, y, z, leaves);
					}
					if (hash(x, 3, z, this.seed) % 23 == 0) {
						p.set(x, topY - 3, z, pal.light());
					}
				}
			}
		}
		p.set(this.cx, roof + 33, this.cz, pal.light());
	}

	private static BlockState leaves(final Block block) {
		BlockState state = block.defaultBlockState();
		return state.hasProperty(LeavesBlock.PERSISTENT) ? state.setValue(LeavesBlock.PERSISTENT, true) : state;
	}

	// ------------------------------------------------------------------ helpers

	private static @Nullable Direction horizontal(final int dx, final int dz) {
		if (dx == 0 && dz == 0) {
			return null;
		}
		return Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
	}

	/** Distance in blocks from the nearest edge of the square. */
	private static int inset(final int lx, final int lz, final int size) {
		return Math.min(Math.min(lx, lz), Math.min(size - 1 - lx, size - 1 - lz));
	}

	static int hash(final int x, final int y, final int z, final long seed) {
		long h = x * 374761393L + y * 668265263L + z * 2147483647L + seed * 982451653L;
		h = (h ^ (h >>> 13)) * 1274126177L;
		return (int)((h ^ (h >>> 16)) & 0x7FFFFFFF);
	}

	/** Block writes clipped to the chunk being decorated. */
	private record Placer(WorldGenLevel level, BoundingBox box) {
		boolean column(final int x, final int z) {
			return x >= this.box.minX() && x <= this.box.maxX() && z >= this.box.minZ() && z <= this.box.maxZ();
		}

		void set(final int x, final int y, final int z, final BlockState state) {
			if (this.box.isInside(x, y, z)) {
				this.level.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_CLIENTS);
			}
		}

		void set(final int x, final int y, final int z, final Block block) {
			this.set(x, y, z, block.defaultBlockState());
		}

		BlockState get(final int x, final int y, final int z) {
			return this.box.isInside(x, y, z) ? this.level.getBlockState(new BlockPos(x, y, z)) : Blocks.STONE.defaultBlockState();
		}

		void clear(final int x, final int y, final int z) {
			if (this.box.isInside(x, y, z) && !this.level.getBlockState(new BlockPos(x, y, z)).isAir()) {
				this.level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
	}
}
