package com.minecraftmode.job.skill;

import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.engrave.EngraveTotals;
import com.minecraftmode.job.weapon.WeaponDef;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
	/** Extra reach around a hitbox when aiming at it (vanilla projectiles end up with the same margin). */
	private static final double AIM_MARGIN = 0.3;
	/** How far the crosshair is searched for the enemy a skill locks on when it starts (the longest target-area range). */
	private static final double CAST_AIM_RANGE = 32.0;
	/** An area hit reaches this many targets in full; see {@link #areaShare}. */
	public static final int AREA_FULL = 5;
	/**
	 * Area hits (cones, novas, fields, strikes, beams, explosions) deal this share of their listed multiplier, already in the tooltips:
	 * they reach every monster in range, and with skills now critting too they pulled further ahead of single-target skills.
	 */
	public static final double AREA_SCALE = 0.85;

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
	/** Where the caster aimed when the skill started; target-area steps keep to it (see {@link #aimedEnemy}). */
	private final Vec3 castEye;
	private final Vec3 castLook;
	private final @Nullable LivingEntity castTarget;

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
		this.castEye = caster.getEyePosition();
		this.castLook = caster.getLookAngle();
		this.castTarget = aimed(caster, this.castEye, this.castLook, CAST_AIM_RANGE, e -> CombatHooks.isEnemy(caster, e));
	}

	/**
	 * Guards the caster for {@code ticks} when this skill closes in on enemies ({@link Skill#engages}); movement steps call it when
	 * they start and when they arrive.
	 */
	public void guard(final int ticks) {
		if (this.skill.engages()) {
			Engage.guard(this.caster, ticks);
		}
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

	/** Where the caster looks, up to {@code range}: the block hit, or the end of the ray. Passes through monsters (beams use it). */
	public Vec3 lookPoint(final double range) {
		return lookPoint(this.caster, range);
	}

	public static Vec3 lookPoint(final Player player, final double range) {
		return lookPoint(player, player.getEyePosition(), player.getLookAngle(), range);
	}

	private static Vec3 lookPoint(final Player player, final Vec3 eye, final Vec3 look, final double range) {
		Vec3 end = eye.add(look.scale(range));
		BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
	}

	/**
	 * {@code want}, or the last open spot on the way to it from the player's eyes: portals and blades launched beside the player
	 * must not start inside a wall or a low ceiling, where they would stop at once.
	 */
	public static Vec3 openPoint(final Player player, final Vec3 want) {
		Vec3 eye = player.getEyePosition();
		BlockHitResult hit = player.level().clip(new ClipContext(eye, want, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (hit.getType() == HitResult.Type.MISS) {
			return want;
		}
		Vec3 back = eye.subtract(hit.getLocation());
		double length = back.length();
		return length <= 0.4 ? eye : hit.getLocation().add(back.scale(0.4 / length));
	}

	/**
	 * The first target whose hitbox the crosshair meets within {@code range}, before any block. {@link #lookPoint} only stops at
	 * blocks, so a skill aimed at a monster right in front would land on the ground behind it.
	 */
	public static @Nullable LivingEntity aimed(final Player player, final double range, final Predicate<LivingEntity> filter) {
		return aimed(player, player.getEyePosition(), player.getLookAngle(), range, filter);
	}

	private static @Nullable LivingEntity aimed(final Player player, final Vec3 eye, final Vec3 look, final double range, final Predicate<LivingEntity> filter) {
		Vec3 end = lookPoint(player, eye, look, range);
		LivingEntity best = null;
		double nearest = Double.MAX_VALUE;
		for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(1.0), filter)) {
			AABB body = e.getBoundingBox();
			double d;
			if (body.contains(eye)) {
				// pressed into the caster: only the one they face
				d = look.dot(body.getCenter().subtract(eye)) > 0.0 ? 0.0 : Double.MAX_VALUE;
			} else {
				d = body.inflate(AIM_MARGIN).clip(eye, end).or(() -> body.clip(eye, end)).map(eye::distanceToSqr).orElse(Double.MAX_VALUE);
			}
			if (d < nearest) {
				nearest = d;
				best = e;
			}
		}
		return best;
	}

	/**
	 * The enemy in the crosshair when the skill started, when it is within {@code range}. Delayed and repeated steps (the bombardment
	 * after a barrage, repeated bolts) stay on it wherever it walks, and land on its last spot when it died meanwhile.
	 */
	public @Nullable LivingEntity aimedEnemy(final double range) {
		return this.castTarget != null && this.castTarget.level() == this.level
			&& this.castTarget.getBoundingBox().distanceToSqr(this.castEye) <= range * range ? this.castTarget : null;
	}

	/** {@link #lookPoint} as it was when the skill started. */
	private Vec3 castLookPoint(final double range) {
		return lookPoint(this.caster, this.castEye, this.castLook, range);
	}

	/** The centre of the enemy aimed at (see {@link #aimedEnemy}), otherwise where the crosshair pointed when the skill started. */
	public Vec3 aimPoint(final double range) {
		LivingEntity target = this.aimedEnemy(range);
		return target != null ? target.getBoundingBox().getCenter() : this.castLookPoint(range);
	}

	/**
	 * The feet of the enemy aimed at (see {@link #aimedEnemy}), otherwise where the crosshair pointed when the skill started, dropped
	 * onto the ground below it (up to 12 blocks).
	 */
	public Vec3 groundPoint(final double range) {
		LivingEntity target = this.aimedEnemy(range);
		if (target != null) {
			return target.position();
		}
		Vec3 p = this.castLookPoint(range);
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

	/**
	 * Enemies within {@code width} of the segment from {@code from} to {@code to}: measured from the body's centre, or from the
	 * hitbox itself so tall bodies are hit at their legs and heads too.
	 */
	public List<LivingEntity> enemiesAlong(final Vec3 from, final Vec3 to, final double width) {
		AABB box = new AABB(from, to).inflate(width + 1.0);
		return this.level.getEntitiesOfClass(LivingEntity.class, box, e -> CombatHooks.isEnemy(this.caster, e)
			&& (distanceToSegment(e.getBoundingBox().getCenter(), from, to) <= width + e.getBbWidth() / 2 || crosses(e.getBoundingBox().inflate(width), from, to)));
	}

	private static boolean crosses(final AABB box, final Vec3 from, final Vec3 to) {
		return box.contains(from) || box.clip(from, to).isPresent();
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

	/**
	 * One area hit (a cone, a nova, a field pulse, a strike, an explosion) on {@code targets}: each takes {@code multiplier}, scaled by
	 * {@link #areaShare} when there are more than {@link #AREA_FULL} of them.
	 */
	public void hitArea(final List<LivingEntity> targets, final double multiplier) {
		double each = multiplier * areaShare(targets.size());
		for (LivingEntity e : targets) {
			this.hit(e, each);
		}
	}

	/**
	 * Share of an area hit each of {@code targets} takes: all of it up to {@link #AREA_FULL}, then √(AREA_FULL / targets), so a pack of
	 * twenty takes half each (the whole hit still grows, slower). Area skills outdid single-target ones more with every extra monster.
	 */
	public static double areaShare(final int targets) {
		return targets <= AREA_FULL ? 1.0 : Math.sqrt((double)AREA_FULL / targets);
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
		if (this.skill.engages()) {
			Engage.stagger(target, this.caster);
		}
		for (BiConsumer<SkillContext, LivingEntity> effect : this.onHit) {
			effect.accept(this, target);
		}
	}
}
