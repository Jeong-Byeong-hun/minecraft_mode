package com.minecraftmode.test;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.registry.ModBlocks;
import com.minecraftmode.worldgen.lair.LairDef;
import com.minecraftmode.worldgen.lair.LairLayout;
import com.minecraftmode.worldgen.lair.LairPiece;
import com.minecraftmode.worldgen.lair.NamedLairs;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Lair layouts (0.5.48): a lair piece saved before (no "Layout" tag) still builds in the old geometry, so a lair half generated before an
 * update meets its other half, and new lairs build in the new one. Both are built for real, chunk by chunk, side by side: corridor height,
 * gate width, the goal chest, the shaft (the new easy spiral climbs with a landing at every corner, every stair met from its low front),
 * the tower's doors. Screenshots of the new shaft and corridors.
 */
final class LairLayoutChecks {
	private static final String LAIR = "cave_troll";
	private static final int FLOOR = -40;
	private static final int SURFACE = -20;
	private static final int OLD_X = 3008;
	private static final int NEW_X = 3408;
	private static final int Z = 8;

	private LairLayoutChecks() {
	}

	static void run(final ClientGameTestContext context, final TestServerContext server) {
		LairDef def = NamedLairs.def(LAIR);
		require(def != null && def.setting() == LairDef.Setting.UNDERGROUND, LAIR + " should be an underground lair");
		for (int x : new int[] {OLD_X, NEW_X}) {
			int reach = 100;
			server.runCommand("forceload add " + (x - reach) + " " + (Z - reach) + " " + (x + reach) + " " + (Z + reach));
		}
		context.waitTicks(40);
		String report = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			LairPiece old = oldPiece(def, OLD_X);
			require(old.layout() == LairLayout.V1, "a piece saved without a layout should read as the old one, got " + old.layout());
			LairPiece fresh = new LairPiece(def, NEW_X, FLOOR, Z, SURFACE, 7L);
			require(fresh.layout() == LairLayout.CURRENT && LairLayout.CURRENT.cell() == 5 && LairLayout.CURRENT.roof() == 7,
				"new lairs should build 4-wide, 6-high corridors");
			require(LairLayout.V1.size(def) == def.cells() * 4 + 1 && def.size() == def.cells() * 5 + 1, "sizes per layout");
			build(level, old);
			build(level, fresh);
			StringBuilder out = new StringBuilder();
			for (LairPiece piece : new LairPiece[] {old, fresh}) {
				LairLayout layout = piece.layout();
				int x = piece.center().getX();
				BlockPos gate = piece.entrance().north(1);
				// the gate's opening: corridor high, a cell minus its wall wide
				int high = 0;
				while (level.getBlockState(gate.above(high)).isAir() && high < 12) {
					high++;
				}
				require(high == layout.roof() - 1, "layout " + layout.version() + ": the gate should be " + (layout.roof() - 1) + " high, got " + high);
				int wide = 0;
				for (int dx = -4; dx <= 4; dx++) {
					wide += level.getBlockState(gate.offset(dx, 0, 0)).isAir() ? 1 : 0;
				}
				require(wide == layout.cell() - 1, "layout " + layout.version() + ": the gate should be " + (layout.cell() - 1) + " wide, got " + wide);
				require(level.getBlockState(piece.goalPos()).is(ModBlocks.LAIR_CHEST), "layout " + layout.version() + ": no goal chest at " + piece.goalPos());
				int sz = LairPiece.shaftZ(def, layout, Z);
				boolean northDoor = level.getBlockState(new BlockPos(x, SURFACE + 1, sz - 4)).isAir();
				require(northDoor == layout.wide(), "layout " + layout.version() + ": a door towards the lair " + (layout.wide() ? "" : "only from 0.5.48"));
				if (layout.gentle()) {
					out.append(climb(level, x, sz));
				} else {
					// the old spiral: a stair right by the door, one up every block round the pillar
					BlockState first = level.getBlockState(new BlockPos(x, FLOOR + 1, sz - 1));
					require(first.getBlock() instanceof StairBlock, "the old spiral should start by the door, got " + first);
				}
				out.append(" layout ").append(layout.version()).append(": gate ").append(wide).append('x').append(high).append(';');
			}
			return out.toString();
		});
		MinecraftMode.LOGGER.info("[lair] layouts:{}", report);
		// pictures, HUD hidden: the gate seen from the approach corridor, and each shaft cut open on its east side
		context.runOnClient(minecraft -> {
			if (!minecraft.gui.hud.isHidden()) {
				minecraft.gui.hud.toggle();
			}
		});
		server.runCommand("gamemode spectator @p");
		server.runCommand("effect give @p minecraft:night_vision 120 0 true");
		for (LairLayout layout : new LairLayout[] {LairLayout.V1, LairLayout.CURRENT}) {
			int x = layout == LairLayout.V1 ? OLD_X : NEW_X;
			BlockPos gate = LairPiece.entrance(def, layout, x, FLOOR, Z);
			String name = layout == LairLayout.V1 ? "old" : "new";
			server.runCommand("tp @p " + (gate.getX() + (layout == LairLayout.V1 ? 0.5 : 0.0)) + " " + (FLOOR + 1.6) + " " + (gate.getZ() + 2.8) + " 180 0");
			context.waitTicks(40);
			context.takeScreenshot("lair_gate_" + name);
			int sz = LairPiece.shaftZ(def, layout, Z);
			// the shaft wall and the tower foundation round it, east side
			server.runCommand("fill " + (x + 2) + " " + (FLOOR + 1) + " " + (sz - 5) + " " + (x + 6) + " " + (SURFACE - 1) + " " + (sz + 5) + " minecraft:air");
			server.runCommand("tp @p " + (x + 12.5) + " " + (FLOOR + 10) + " " + (sz + 0.5) + " 90 10");
			context.waitTicks(40);
			context.takeScreenshot("lair_shaft_" + name);
		}
		context.runOnClient(minecraft -> {
			if (minecraft.gui.hud.isHidden()) {
				minecraft.gui.hud.toggle();
			}
		});
		server.runCommand("effect clear @p");
		server.runCommand("gamemode creative @p");
		server.runCommand("tp @p 0.5 -60 0.5");
		for (int x : new int[] {OLD_X, NEW_X}) {
			server.runCommand("forceload remove " + (x - 100) + " " + (Z - 100) + " " + (x + 100) + " " + (Z + 100));
		}
	}

	/**
	 * Walks the new spiral up from the floor: round the ring from the corner by the door, each side's middle a stair one higher facing on,
	 * each corner a landing level with the stair before it, never more than one up between neighbours, until the tower floor.
	 */
	private static String climb(final ServerLevel level, final int sx, final int sz) {
		int[][] ring = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};
		int top = FLOOR;
		int steps = 0;
		for (int i = 1; top < SURFACE; i++) {
			int[] at = ring[i % 8];
			boolean side = i % 2 == 1;
			int y = side ? FLOOR + 1 + (i - 1) / 2 : FLOOR + i / 2;
			BlockState state = level.getBlockState(new BlockPos(sx + at[0], y, sz + at[1]));
			if (side) {
				int[] next = ring[(i + 1) % 8];
				int dx = next[0] - at[0];
				int dz = next[1] - at[1];
				Direction facing = dx > 0 ? Direction.EAST : dx < 0 ? Direction.WEST : dz > 0 ? Direction.SOUTH : Direction.NORTH;
				require(state.getBlock() instanceof StairBlock && state.getValue(StairBlock.FACING) == facing,
					"step " + i + " at " + (sx + at[0]) + " " + y + " " + (sz + at[1]) + " should be a stair facing " + facing + ", got " + state);
			} else {
				require(!state.isAir() && !(state.getBlock() instanceof StairBlock), "corner " + i + " should be a landing, got " + state);
			}
			for (int h = 1; h <= 2; h++) {
				require(level.getBlockState(new BlockPos(sx + at[0], y + h, sz + at[1])).isAir(), "no headroom above step " + i);
			}
			require(y - top <= 1, "a step of more than one at " + i);
			top = y;
			steps++;
		}
		return " new spiral " + steps + " steps to the tower floor;";
	}

	/** A piece as saved before 0.5.48: the same fields, no "Layout". */
	private static LairPiece oldPiece(final LairDef def, final int x) {
		CompoundTag tag = new CompoundTag();
		int reach = LairLayout.V1.size(def) / 2 + LairPiece.APRON + LairPiece.GROUNDS;
		tag.put("BB", BoundingBox.CODEC.encodeStart(NbtOps.INSTANCE, new BoundingBox(x - reach, FLOOR - 6, Z - reach, x + reach, SURFACE + 32, Z + reach))
			.getOrThrow());
		tag.putString("Lair", def.id());
		tag.putInt("CX", x);
		tag.putInt("CZ", Z);
		tag.putInt("Floor", FLOOR);
		tag.putInt("Surface", SURFACE);
		tag.putLong("Seed", 7L);
		return new LairPiece(tag);
	}

	/** Builds {@code piece} chunk by chunk, the way world generation decorates it. */
	private static void build(final ServerLevel level, final LairPiece piece) {
		BoundingBox box = piece.getBoundingBox();
		for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
			for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
				BoundingBox chunk = new BoundingBox(cx << 4, box.minY(), cz << 4, (cx << 4) + 15, box.maxY(), (cz << 4) + 15);
				piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(1L), chunk,
					new ChunkPos(cx, cz), new BlockPos(cx << 4, box.minY(), cz << 4));
			}
		}
	}

	private static void require(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
