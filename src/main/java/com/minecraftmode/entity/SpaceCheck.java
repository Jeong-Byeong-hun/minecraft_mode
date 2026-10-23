package com.minecraftmode.entity;

import com.minecraftmode.entity.combat.Attacks;
import com.minecraftmode.entity.named.NamedMob;
import com.minecraftmode.worldgen.lair.NamedLairs;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Room for big monsters. Lair corridors are four blocks high, and until 0.5.46 a lord grew 1.2x on waking (a treant lord stood 4.3
 * high), a lord dragged off was teleported home and minions were dropped 2.5 blocks out, all without looking for room: big monsters
 * stuck in the roof or a wall and choked to death on their own. Now they are placed where they fit, a lord only grows as far as its
 * spot allows, and a monster that chokes in a wall anyway (named monsters, minions, anything on lair grounds) is moved out instead.
 */
public final class SpaceCheck {
	/** How far (blocks) a stuck monster or a placement looks for room. */
	public static final int SEARCH = 4;

	private SpaceCheck() {
	}

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) -> {
			if (!source.is(DamageTypes.IN_WALL) || !(victim instanceof Mob mob) || !(victim.level() instanceof ServerLevel level) || !watched(level, mob)) {
				return true;
			}
			Vec3 free = nearestFree(level, mob, mob.position(), SEARCH);
			if (free == null) {
				return true;
			}
			mob.teleportTo(free.x, free.y, free.z);
			mob.getNavigation().stop();
			return false;
		});
	}

	/** Monsters this looks after: named monsters, minions and anything on a lair's grounds (vanilla handles the rest). */
	private static boolean watched(final ServerLevel level, final Mob mob) {
		return mob instanceof NamedMob || mob.entityTags().contains(Attacks.MINION_TAG) || NamedLairs.at(level, mob.blockPosition()) != null;
	}

	/** Whether {@code entity} at its current size would stand free of blocks with its feet at {@code feet}. */
	public static boolean fits(final ServerLevel level, final Entity entity, final Vec3 feet) {
		return level.noCollision(entity, entity.getDimensions(entity.getPose()).makeBoundingBox(feet));
	}

	/**
	 * {@code feet} if {@code entity} fits there, else the nearest block centre within {@code reach} (same height first, then one or two up,
	 * then one down) where it fits standing on something; null if there is none.
	 */
	public static @Nullable Vec3 nearestFree(final ServerLevel level, final Entity entity, final Vec3 feet, final int reach) {
		if (fits(level, entity, feet)) {
			return feet;
		}
		BlockPos base = BlockPos.containing(feet);
		for (int dy : new int[] {0, 1, 2, -1}) {
			Vec3 best = null;
			double bestDistance = Double.MAX_VALUE;
			for (int dx = -reach; dx <= reach; dx++) {
				for (int dz = -reach; dz <= reach; dz++) {
					BlockPos at = base.offset(dx, dy, dz);
					Vec3 spot = new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
					double distance = spot.distanceToSqr(feet);
					if (distance >= bestDistance || level.getBlockState(at.below()).getCollisionShape(level, at.below()).isEmpty()) {
						continue;
					}
					if (fits(level, entity, spot)) {
						best = spot;
						bestDistance = distance;
					}
				}
			}
			if (best != null) {
				return best;
			}
		}
		return null;
	}

	/**
	 * Sets {@code mob}'s scale to {@code wanted}, or less (down to 1) when it would not fit where it stands at that size: a lord grows only
	 * as far as the corridor it wakes in allows.
	 */
	public static void growTo(final ServerLevel level, final LivingEntity mob, final float wanted) {
		EntityDimensions base = mob.getType().getDimensions();
		float scale = wanted;
		while (scale > 1.0F) {
			AABB box = base.scale(scale).makeBoundingBox(mob.position());
			if (level.noCollision(mob, box)) {
				break;
			}
			scale = Math.max(1.0F, scale - 0.05F);
		}
		mob.getAttribute(Attributes.SCALE).setBaseValue(scale);
	}
}
