package com.minecraftmode.entity.combat;

import com.minecraftmode.job.skill.SkillScheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Damage helpers for monster and boss patterns: who stands in a circle, line or cone, hitting with a
 * multiple of the attacker's attack damage, knockback, effects and short-lived minions.
 */
public final class Attacks {
	/** Tag on minions so they can be cleaned up and do not drop loot. */
	public static final String MINION_TAG = "minecraft_mode_minion";

	/** Players that can be hurt (not creative or spectator). */
	public static boolean isTarget(final Entity e) {
		return e instanceof Player p && p.isAlive() && !p.isSpectator() && !p.isCreative();
	}

	public static List<LivingEntity> inCircle(final ServerLevel level, final Vec3 center, final double radius, final double height) {
		AABB box = new AABB(center.x - radius, center.y - 1.0, center.z - radius, center.x + radius, center.y + height, center.z + radius);
		List<LivingEntity> out = new ArrayList<>();
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, Attacks::isTarget)) {
			double dx = e.getX() - center.x;
			double dz = e.getZ() - center.z;
			if (dx * dx + dz * dz <= (radius + e.getBbWidth() / 2) * (radius + e.getBbWidth() / 2)) {
				out.add(e);
			}
		}
		return out;
	}

	public static List<LivingEntity> inLine(final ServerLevel level, final Vec3 from, final Vec3 to, final double width, final double height) {
		Vec3 dir = to.subtract(from).multiply(1, 0, 1);
		double length = dir.length();
		List<LivingEntity> out = new ArrayList<>();
		if (length < 0.01) {
			return out;
		}
		Vec3 unit = dir.scale(1.0 / length);
		AABB box = new AABB(from, to).inflate(width, height, width);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, Attacks::isTarget)) {
			Vec3 rel = e.position().subtract(from).multiply(1, 0, 1);
			double along = rel.dot(unit);
			double across = Math.abs(rel.x * unit.z - rel.z * unit.x);
			if (along >= -0.5 && along <= length + 0.5 && across <= width / 2 + e.getBbWidth() / 2 && Math.abs(e.getY() - from.y) <= height) {
				out.add(e);
			}
		}
		return out;
	}

	public static List<LivingEntity> inCone(final ServerLevel level, final Vec3 origin, final float yawDeg, final double range, final double halfAngle) {
		Vec3 facing = Vec3.directionFromRotation(0.0F, yawDeg);
		List<LivingEntity> out = new ArrayList<>();
		for (LivingEntity e : inCircle(level, origin, range, 6.0)) {
			Vec3 rel = e.position().subtract(origin).multiply(1, 0, 1);
			if (rel.lengthSqr() < 1.0 || Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, rel.normalize().dot(facing))))) <= halfAngle) {
				out.add(e);
			}
		}
		return out;
	}

	/** {@code power} x the attacker's attack damage. */
	public static float damage(final Mob attacker, final float power) {
		return (float)attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * power;
	}

	public static void hit(final Mob attacker, final LivingEntity target, final float amount) {
		if (attacker.level() instanceof ServerLevel level) {
			target.hurtServer(level, attacker.damageSources().mobAttack(attacker), amount);
		}
	}

	public static void hitAll(final Mob attacker, final List<LivingEntity> targets, final float amount, final @Nullable Consumer<LivingEntity> extra) {
		for (LivingEntity target : targets) {
			hit(attacker, target, amount);
			if (extra != null) {
				extra.accept(target);
			}
		}
	}

	public static void effect(final LivingEntity target, final Holder<MobEffect> effect, final int ticks, final int amplifier, final Entity source) {
		target.addEffect(new MobEffectInstance(effect, ticks, amplifier), source);
	}

	public static void knock(final LivingEntity target, final Vec3 from, final double strength, final double up) {
		Vec3 away = target.position().subtract(from).multiply(1, 0, 1);
		if (away.lengthSqr() < 1.0E-4) {
			away = new Vec3(1, 0, 0);
		}
		away = away.normalize().scale(strength);
		push(target, new Vec3(away.x, up, away.z));
	}

	public static void push(final Entity entity, final Vec3 velocity) {
		entity.setDeltaMovement(velocity);
		if (entity instanceof Player) {
			entity.syncVelocity = true;
		} else {
			entity.needsSync = true;
		}
	}

	/** Spawns {@code count} minions around {@code around}, aimed at {@code target}; they vanish after {@code lifeTicks}. */
	public static <T extends Mob> List<T> summon(final ServerLevel level, final EntityType<T> type, final Vec3 around, final int count, final @Nullable LivingEntity target,
		final int lifeTicks) {
		List<T> spawned = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			T mob = type.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (mob == null) {
				continue;
			}
			double a = Math.PI * 2 * i / Math.max(1, count) + level.getRandom().nextDouble() * 0.5;
			mob.snapTo(around.x + Math.cos(a) * 2.5, around.y, around.z + Math.sin(a) * 2.5, (float)Math.toDegrees(a), 0.0F);
			mob.addTag(MINION_TAG);
			if (target != null) {
				mob.setTarget(target);
			}
			level.addFreshEntity(mob);
			spawned.add(mob);
			SkillScheduler.schedule(lifeTicks, () -> {
				if (mob.isAlive()) {
					mob.discard();
				}
			});
		}
		return spawned;
	}

	private Attacks() {
	}
}
