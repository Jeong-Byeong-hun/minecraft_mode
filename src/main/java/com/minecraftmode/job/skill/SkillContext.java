package com.minecraftmode.job.skill;

import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.engrave.EngraveTotals;
import com.minecraftmode.job.weapon.WeaponDef;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Everything a skill step needs: the caster, the weapon, the damage base and the hit modifiers
 * collected from the skill's modifier steps. Kept alive by delayed steps and projectiles, so
 * {@link #valid()} must be checked before acting later.
 */
public final class SkillContext {
	public final ServerPlayer caster;
	public final ServerLevel level;
	public final WeaponDef weapon;
	public final Skill skill;
	public final ItemStack stack;
	public final Fx fx;
	public final EngraveTotals mods;
	/** Weapon power with every skill-damage bonus applied; damage = power x multiplier. */
	public final double power;
	private final double areaScale;
	private final List<BiConsumer<SkillContext, LivingEntity>> onHit = new ArrayList<>();
	private double lifesteal;

	public SkillContext(final ServerPlayer caster, final WeaponDef weapon, final Skill skill, final ItemStack stack, final EngraveTotals mods, final double powerBonus) {
		this.caster = caster;
		this.level = caster.level();
		this.weapon = weapon;
		this.skill = skill;
		this.stack = stack.copy();
		this.mods = mods;
		Fx base = weapon.fx();
		Fx own = skill.fx();
		this.fx = own == null ? base : own.color() < 0 ? new Fx(own.kind(), base.color()) : own;
		this.power = weapon.power() * (1.0 + mods.fraction(EngraveStat.SKILL_DAMAGE) + powerBonus);
		this.areaScale = 1.0 + mods.fraction(EngraveStat.SKILL_AREA);
	}

	public boolean valid() {
		return this.caster.isAlive() && !this.caster.isRemoved() && this.caster.level() == this.level;
	}

	// ------------------------------------------------------------ modifiers

	public void addOnHit(final BiConsumer<SkillContext, LivingEntity> effect) {
		this.onHit.add(effect);
	}

	public void addLifesteal(final double fraction) {
		this.lifesteal += fraction;
	}

	/** Skill area radius after the Expansion engraving. */
	public double area(final double radius) {
		return radius * this.areaScale;
	}

	// ------------------------------------------------------------ geometry

	public Vec3 eye() {
		return this.caster.getEyePosition();
	}

	public Vec3 look() {
		return this.caster.getLookAngle();
	}

	public Vec3 flatLook() {
		Vec3 look = this.look();
		Vec3 flat = new Vec3(look.x, 0.0, look.z);
		return flat.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, this.caster.getYRot()) : flat.normalize();
	}

	/** Where the caster looks, up to {@code range}: the block hit, or the end of the ray. */
	public Vec3 lookPoint(final double range) {
		Vec3 eye = this.eye();
		Vec3 end = eye.add(this.look().scale(range));
		BlockHitResult hit = this.level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.caster));
		return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
	}

	/** {@link #lookPoint} dropped onto the ground below it (up to 12 blocks). */
	public Vec3 groundPoint(final double range) {
		Vec3 p = this.lookPoint(range);
		BlockHitResult down = this.level.clip(new ClipContext(p.add(0, 0.5, 0), p.add(0, -12, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.caster));
		return down.getType() == HitResult.Type.MISS ? p : down.getLocation();
	}

	public List<LivingEntity> enemiesNear(final Vec3 center, final double radius) {
		AABB box = new AABB(center, center).inflate(radius);
		return this.level.getEntitiesOfClass(LivingEntity.class, box, e -> CombatHooks.isEnemy(this.caster, e)
			&& e.getBoundingBox().distanceToSqr(center) <= radius * radius);
	}

	/** Enemies in front of the caster within {@code range} and a horizontal arc of {@code arcDeg}. */
	public List<LivingEntity> enemiesInCone(final double range, final double arcDeg) {
		Vec3 eye = this.eye();
		Vec3 dir = this.flatLook();
		double cos = Math.cos(Math.toRadians(arcDeg / 2));
		List<LivingEntity> result = new ArrayList<>();
		for (LivingEntity e : this.enemiesNear(eye, range)) {
			Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
			Vec3 flat = new Vec3(to.x, 0.0, to.z);
			if (flat.lengthSqr() < 1.0 || flat.normalize().dot(dir) >= cos) {
				result.add(e);
			}
		}
		return result;
	}

	/** Enemies within {@code width} of the segment from {@code from} to {@code to}. */
	public List<LivingEntity> enemiesAlong(final Vec3 from, final Vec3 to, final double width) {
		AABB box = new AABB(from, to).inflate(width + 1.0);
		return this.level.getEntitiesOfClass(LivingEntity.class, box, e -> CombatHooks.isEnemy(this.caster, e)
			&& distanceToSegment(e.getBoundingBox().getCenter(), from, to) <= width + e.getBbWidth() / 2);
	}

	/** The enemy closest to the crosshair within {@code range}, if any is roughly in view. */
	public @Nullable LivingEntity lookTarget(final double range) {
		Vec3 eye = this.eye();
		Vec3 look = this.look();
		Vec3 end = this.lookPoint(range);
		return this.enemiesAlong(eye, end, 1.5).stream()
			.filter(e -> this.caster.hasLineOfSight(e))
			.min(Comparator.comparingDouble(e -> {
				Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
				return to.length() * (1.5 - to.normalize().dot(look));
			}))
			.orElse(null);
	}

	private static double distanceToSegment(final Vec3 p, final Vec3 a, final Vec3 b) {
		Vec3 ab = b.subtract(a);
		double len = ab.lengthSqr();
		double t = len < 1.0E-6 ? 0.0 : Mth.clamp(p.subtract(a).dot(ab) / len, 0.0, 1.0);
		return p.distanceTo(a.add(ab.scale(t)));
	}

	// ------------------------------------------------------------ damage

	public float damageFor(final double multiplier) {
		return (float)(this.power * multiplier);
	}

	/** Hits with the caster's melee damage source. */
	public void hit(final LivingEntity target, final double multiplier) {
		this.hit(target, multiplier, this.caster.damageSources().playerAttack(this.caster));
	}

	public void hit(final LivingEntity target, final double multiplier, final DamageSource source) {
		if (!target.isAlive() || multiplier <= 0.0) {
			if (target.isAlive()) {
				this.applyOnHit(target);
			}
			return;
		}
		float before = target.getHealth();
		CombatHooks.deal(this.caster, target, this.damageFor(multiplier), source, CombatHooks.DamageKind.SKILL);
		float dealt = Math.max(0.0F, before - target.getHealth());
		if (this.lifesteal > 0.0 && dealt > 0.0F) {
			this.caster.heal((float)(dealt * this.lifesteal));
		}
		this.applyOnHit(target);
	}

	/** A vanilla arrow fired by this skill hit {@code target} (damage already dealt). */
	void afterArrowHit(final LivingEntity target, final float dealt) {
		if (this.lifesteal > 0.0 && dealt > 0.0F && this.valid()) {
			this.caster.heal((float)(dealt * this.lifesteal));
		}
		this.applyOnHit(target);
	}

	private void applyOnHit(final LivingEntity target) {
		if (!target.isAlive()) {
			return;
		}
		for (BiConsumer<SkillContext, LivingEntity> effect : this.onHit) {
			effect.accept(this, target);
		}
	}
}
