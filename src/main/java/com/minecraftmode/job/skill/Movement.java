package com.minecraftmode.job.skill;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Where blinks, dashes and steps may take a player, and how leaps land. Movement must never be what
 * kills its caster: a path climbs at most {@link #MAX_RISE} blocks besides stepping up blocks, stops
 * at a ledge that would drop the caster further than {@link #SAFE_DROP} (or than they already were
 * above the ground), and leaps only count the drop below the height they started from.
 */
public final class Movement {
	/** Highest a looking-up blink climbs over its whole distance (stepping up blocks comes on top). */
	public static final double MAX_RISE = 2.0;
	/** A move never ends higher above the ground than this, or than the caster already was. */
	public static final double SAFE_DROP = 3.0;
	private static final double STEP = 0.5;
	private static final double SCAN = 32.0;
	/** A leap that has not left the ground after this many ticks lands where it stands. */
	private static final int LIFTOFF_TICKS = 10;
	private static final int MAX_AIR_TICKS = 200;

	/** Horizontal facing of the player's yaw. */
	public static Vec3 facing(final ServerPlayer player) {
		return Vec3.directionFromRotation(0.0F, player.getYRot());
	}

	/** How much a blink of {@code distance} climbs for the player's pitch: never down, at most {@link #MAX_RISE}. */
	public static double rise(final ServerPlayer player, final double distance) {
		return Mth.clamp(player.getLookAngle().y * distance, 0.0, MAX_RISE);
	}

	/**
	 * Free positions every half block along the horizontal part of {@code dir}, climbing {@code rise}
	 * spread over the distance and stepping up single blocks. Stops before a wall or a ledge; empty
	 * when the first step is already blocked.
	 */
	public static List<Vec3> path(final ServerPlayer player, final Vec3 dir, final double distance, final double rise) {
		Vec3 origin = player.position();
		Vec3 flat = new Vec3(dir.x, 0.0, dir.z);
		flat = flat.lengthSqr() < 1.0E-4 ? facing(player) : flat.normalize();
		double allowed = allowedDrop(player);
		double perStep = distance > 0 ? rise * STEP / distance : 0.0;
		double risen = 0.0;
		double y = origin.y;
		List<Vec3> points = new ArrayList<>();
		for (double d = STEP; d <= distance + 1.0E-6; d += STEP) {
			Vec3 base = new Vec3(origin.x + flat.x * d, y, origin.z + flat.z * d);
			Vec3 next;
			if (risen < rise && free(player, base.add(0, perStep, 0))) {
				next = base.add(0, perStep, 0);
				risen += perStep;
			} else if (free(player, base)) {
				next = base;
			} else if (free(player, base.add(0, 1.0, 0))) {
				next = base.add(0, 1.0, 0);
			} else {
				break;
			}
			if (!grounded(player, next, allowed)) {
				break;
			}
			points.add(next);
			y = next.y;
		}
		return points;
	}

	/** Last point of {@link #path}, or where the player stands when they cannot move at all. */
	public static Vec3 end(final ServerPlayer player, final Vec3 dir, final double distance, final double rise) {
		List<Vec3> points = path(player, dir, distance, rise);
		return points.isEmpty() ? player.position() : points.getLast();
	}

	/** Whether the player could be put at {@code pos} without being stuck or left above a deadly drop. */
	public static boolean canStand(final ServerPlayer player, final Vec3 pos) {
		return free(player, pos) && grounded(player, pos, allowedDrop(player));
	}

	/**
	 * Nothing solid between two feet positions, checked low and high, so a step from one to the other cannot pass through a thin wall
	 * (glass pane, bars, fence) or under/over one.
	 */
	public static boolean clearLine(final ServerPlayer player, final Vec3 from, final Vec3 to) {
		for (double h : new double[] {0.3, 1.5}) {
			Vec3 a = from.add(0, h, 0);
			Vec3 b = to.add(0, h, 0);
			if (player.level().clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) {
				return false;
			}
		}
		return true;
	}

	public static void teleport(final ServerPlayer player, final Vec3 pos) {
		player.teleportTo(pos.x, pos.y, pos.z);
		player.resetFallDistance();
	}

	/**
	 * Follows a leap or pull until the player lands (after leaving the ground), then runs
	 * {@code onLand}. Meanwhile only the drop below the height the move started from counts as
	 * falling, so the height the skill gave never hurts but jumping off a cliff still does.
	 */
	public static void guardFall(final ServerPlayer player, final Runnable onLand) {
		ServerLevel level = player.level();
		guardStep(player, level, player.getY(), 0, false, onLand);
	}

	private static void guardStep(final ServerPlayer player, final ServerLevel level, final double startY, final int waited,
		final boolean wasAirborne, final Runnable onLand) {
		SkillScheduler.schedule(1, () -> {
			if (!player.isAlive() || player.isRemoved() || player.level() != level) {
				return;
			}
			player.fallDistance = Math.min(player.fallDistance, Math.max(0.0, startY - player.getY()));
			boolean airborne = wasAirborne || !player.onGround();
			boolean landed = airborne && (player.onGround() || player.isInWater() || player.onClimbable());
			if (landed || !airborne && waited >= LIFTOFF_TICKS || waited >= MAX_AIR_TICKS) {
				onLand.run();
			} else {
				guardStep(player, level, startY, waited + 1, airborne, onLand);
			}
		});
	}

	// ------------------------------------------------------------------ helpers

	private static double allowedDrop(final ServerPlayer player) {
		return Math.max(SAFE_DROP, depth(player, player.position()));
	}

	/** Blocks of air under {@code pos} (half-block steps, up to {@link #SCAN}); 0 when standing on something. */
	private static double depth(final ServerPlayer player, final Vec3 pos) {
		for (double dy = 0.0; dy < SCAN; dy += STEP) {
			if (grounded(player, pos, dy + STEP)) {
				return dy;
			}
		}
		return SCAN;
	}

	private static AABB boxAt(final ServerPlayer player, final Vec3 pos) {
		return player.getBoundingBox().move(pos.subtract(player.position()));
	}

	private static boolean free(final ServerPlayer player, final Vec3 pos) {
		return player.level().noCollision(player, boxAt(player, pos));
	}

	/** Something to land on (a block, or water to fall into) within {@code drop} blocks below {@code pos}. */
	private static boolean grounded(final ServerPlayer player, final Vec3 pos, final double drop) {
		AABB below = boxAt(player, pos).expandTowards(0.0, -drop, 0.0);
		ServerLevel level = player.level();
		return !level.noCollision(player, below) || level.getBlockStates(below).anyMatch(s -> s.getFluidState().is(FluidTags.WATER));
	}

	private Movement() {
	}
}
