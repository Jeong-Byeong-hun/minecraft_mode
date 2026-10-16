package com.minecraftmode.test;

import com.minecraftmode.worldgen.lair.LairDef;
import com.minecraftmode.worldgen.lair.LairPiece;
import com.minecraftmode.worldgen.lair.LairStructure;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Where {@link SiteShotsClientGameTest} puts its camera: a spot that sees a creature whole from open air, a frame that fits a whole
 * lair, and the world edits an underground lair needs to be seen at all. Pure placement, server side; no screenshots here.
 */
final class SiteShotCamera {
	/** The spectator's eye above the feet /tp moves. */
	static final double EYE_HEIGHT = 1.62;
	/** The game's default vertical field of view, and how much room to leave around a lair in the frame. */
	private static final double FOV_DEGREES = 70.0;
	private static final double FRAME_MARGIN = 0.8;
	/** The Nether's thick fog swallows anything much farther than this. */
	private static final double NETHER_DISTANCE = 40.0;
	/** How steeply the lair camera looks down: enough to clear the trees around it. */
	private static final double LAIR_ELEVATION_DEGREES = 28.0;
	/** How steeply to look into an uncovered underground lair, over the rock walls of the pit. */
	private static final double PIT_ELEVATION_DEGREES = 62.0;
	private static final int FILL_LIMIT = 32768;
	/** How far around an uncovered lair water and lava are turned to stone so they do not pour in. */
	private static final int UNCOVER_MARGIN = 8;
	/** Horizontal directions to try the camera in, south (a monster's default facing) first. */
	private static final double[][] CAMERA_DIRECTIONS = {{0, 1}, {1, 1}, {-1, 1}, {1, 0}, {-1, 0}, {1, -1}, {-1, -1}, {0, -1}};
	/** Where around a biome sample to try standing a creature; all inside a force-loaded 2-chunk radius. */
	private static final int[][] SAMPLE_OFFSETS = {{0, 0}, {12, 0}, {-12, 0}, {0, 12}, {0, -12}, {12, 12}, {-12, -12}, {12, -12}, {-12, 12},
		{24, 0}, {-24, 0}, {0, 24}, {0, -24}};

	/** Where a creature stands, its middle, and the camera eye that sees it. */
	record Spot(BlockPos feet, Vec3 middle, Vec3 eye) {
	}

	/** Where the camera eye is and what it looks at. */
	record View(Vec3 eye, Vec3 target) {
	}

	private SiteShotCamera() {
	}

	/** The dimension a lair (and its named monster) lives in. */
	static ResourceKey<Level> dimension(final LairDef.Setting setting) {
		return switch (setting) {
			case NETHER -> Level.NETHER;
			case END -> Level.END;
			default -> Level.OVERWORLD;
		};
	}

	/** The yaw and pitch (degrees, as /tp takes them) that look from {@code eye} at {@code target}. */
	static double[] yawPitch(final Vec3 eye, final Vec3 target) {
		Vec3 to = target.subtract(eye);
		return new double[] {Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG - 90.0, -Mth.atan2(to.y, to.horizontalDistance()) * Mth.RAD_TO_DEG};
	}

	/** The yaw an entity at {@code from} needs to face {@code toward}. */
	static float facingYaw(final Vec3 from, final Vec3 toward) {
		return (float)(Mth.atan2(toward.z - from.z, toward.x - from.x) * Mth.RAD_TO_DEG) - 90.0F;
	}

	/**
	 * A place near {@code sample} to stand a creature of {@code height} and see it from: first any spot that works at the full
	 * {@code distance} (a roomy cave, a clearing in the jungle), only then cramped ones from closer. Null when there is none.
	 */
	static Spot spot(final ServerLevel level, final BlockPos sample, final boolean surface, final double height, final double distance) {
		for (boolean closer : new boolean[] {false, true}) {
			for (int[] offset : SAMPLE_OFFSETS) {
				BlockPos feet = floor(level, sample.offset(offset[0], 0, offset[1]), surface);
				Vec3 middle = Vec3.atBottomCenterOf(feet).add(0.0, height * 0.5, 0.0);
				Vec3 eye = camera(level, middle, distance, height * 0.15 + 0.4, closer);
				if (eye != null) {
					return new Spot(feet, middle, eye);
				}
			}
		}
		return null;
	}

	/**
	 * Where a creature can stand: the top block on the surface, otherwise (caves, the Nether) the nearest floor with three free
	 * blocks above it, searched down then up from the sample. Falls back to the sample itself.
	 */
	private static BlockPos floor(final ServerLevel level, final BlockPos sample, final boolean surface) {
		if (surface) {
			return new BlockPos(sample.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sample.getX(), sample.getZ()), sample.getZ());
		}
		for (int dy = 0; dy <= 32; dy++) {
			for (int y : new int[] {sample.getY() - dy, sample.getY() + dy}) {
				BlockPos feet = new BlockPos(sample.getX(), y, sample.getZ());
				if (level.getBlockState(feet.below()).isSolid() && open(level, feet) && level.getBlockState(feet.above(2)).isAir()) {
					return feet;
				}
			}
		}
		return sample;
	}

	/**
	 * A camera eye about {@code distance} from {@code target} and {@code rise} above it with open air around it and a clear line to
	 * the target, on any side; with {@code closer} also nearer ones. Null when there is none. Keeps a cave or Nether shot from
	 * starting inside the rock, which draws the terrain inside out.
	 */
	private static Vec3 camera(final ServerLevel level, final Vec3 target, final double distance, final double rise, final boolean closer) {
		double nearest = closer ? 2.5 : distance;
		for (double d = distance; d >= nearest; d *= 0.8) {
			for (double[] dir : CAMERA_DIRECTIONS) {
				double scale = d / Math.hypot(dir[0], dir[1]);
				Vec3 eye = target.add(dir[0] * scale, rise, dir[1] * scale);
				if (!open(level, BlockPos.containing(eye)) || !level.getBlockState(BlockPos.containing(eye).below()).isAir()) {
					continue;
				}
				ClipContext line = new ClipContext(eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
				if (level.clip(line).getType() == HitResult.Type.MISS) {
					return eye;
				}
			}
		}
		return null;
	}

	/** The cell and the one above it are air. */
	private static boolean open(final ServerLevel level, final BlockPos cell) {
		return level.getBlockState(cell).isAir() && level.getBlockState(cell.above()).isAir();
	}

	/**
	 * The whole lair from above its south side: far enough back that its bounding sphere (walls to landmark top, from the lair's own
	 * size) fits the vertical field of view. Underground lairs ({@link #uncover uncovered}) are looked into steeply from above; in the
	 * Nether the thick fog hides anything far, so the camera stays close, and when it lands in rock it moves toward the lair.
	 */
	static View wholeLair(final ServerLevel level, final LairDef def, final BlockPos center) {
		int reach = def.size() / 2;
		int floor = switch (def.setting()) {
			case NETHER -> LairStructure.NETHER_FLOOR;
			case UNDERGROUND -> def.depth();
			// the apron just outside the walls lies at the lair's floor
			default -> level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.getX() + reach + 2, center.getZ() + reach + 2);
		};
		double half = LairPiece.topOffset(def) / 2.0;
		Vec3 target = new Vec3(center.getX() + 0.5, floor + half, center.getZ() + 0.5);
		double radius = Math.sqrt(2.0 * reach * reach + half * half);
		double distance = radius / Math.sin(Math.toRadians(FOV_DEGREES / 2.0)) * FRAME_MARGIN;
		if (def.setting() == LairDef.Setting.NETHER) {
			distance = Math.min(distance, NETHER_DISTANCE);
		}
		double elevation = Math.toRadians(def.setting() == LairDef.Setting.UNDERGROUND ? PIT_ELEVATION_DEGREES : LAIR_ELEVATION_DEGREES);
		Vec3 eye = target.add(new Vec3(Math.cos(elevation) * 0.5, Math.sin(elevation), Math.cos(elevation) * 0.866).scale(distance));
		return new View(openAir(level, eye, target), target);
	}

	/** {@code eye} if it is in open air, else the first open spot on the way from it to {@code target}. */
	private static Vec3 openAir(final ServerLevel level, final Vec3 eye, final Vec3 target) {
		Vec3 step = target.subtract(eye).normalize();
		double length = eye.distanceTo(target);
		for (double d = 0; d < length; d += 1.0) {
			Vec3 at = eye.add(step.scale(d));
			BlockPos cell = BlockPos.containing(at);
			if (open(level, cell) && level.getBlockState(cell.below()).isAir()) {
				return at;
			}
		}
		return eye;
	}

	/**
	 * An underground lair is a roofed box deep in the rock with nothing to see from outside. For the picture (in this throwaway test
	 * world only) its roof and everything above it up to the surface is cleared, so the maze shows from the sky like a cutaway.
	 */
	static void uncover(final TestServerContext server, final LairDef def, final BlockPos center) {
		int x0 = center.getX() - def.size() / 2 - 1;
		int z0 = center.getZ() - def.size() / 2 - 1;
		int x1 = x0 + def.size() + 1;
		int z1 = z0 + def.size() + 1;
		// the maze passages have their own ceiling at floor + ROOF under the box's lid; a roofed lair's topOffset is ROOF + 2.
		// Clearing from there leaves the maze walls standing.
		int ceiling = def.depth() + LairPiece.topOffset(def) - 2;
		int top = server.computeOnServer(s -> {
			int highest = ceiling;
			for (int x = x0; x <= x1; x += 4) {
				for (int z = z0; z <= z1; z += 4) {
					highest = Math.max(highest, s.overworld().getHeight(Heightmap.Types.WORLD_SURFACE, x, z));
				}
			}
			return highest;
		});
		// water and lava around would pour into the opened pit: turn them to stone first, a little beyond its walls
		int m = UNCOVER_MARGIN;
		for (String fluid : new String[] {"minecraft:water", "minecraft:lava"}) {
			fillLayers(server, x0 - m, ceiling - m, z0 - m, x1 + m, top, z1 + m, "stone replace " + fluid);
		}
		fillLayers(server, x0, ceiling, z0, x1, top, z1, "air");
	}

	/** {@code /fill} from y0 to y1 in slabs, since one fill takes at most {@link #FILL_LIMIT} blocks. */
	private static void fillLayers(final TestServerContext server, final int x0, final int y0, final int z0, final int x1, final int y1, final int z1,
		final String with) {
		int layers = Math.max(1, FILL_LIMIT / ((x1 - x0 + 1) * (z1 - z0 + 1)));
		for (int y = y0; y <= y1; y += layers) {
			server.runCommand("fill " + x0 + " " + y + " " + z0 + " " + x1 + " " + Math.min(y1, y + layers - 1) + " " + z1 + " " + with);
		}
	}
}
