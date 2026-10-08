package com.minecraftmode.job.skill;

import com.minecraftmode.registry.ModEntities;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Projectile of class weapons and skills. Shows an item (or nothing) plus a tinted particle trail
 * drawn by the client. The hit behaviour lives only on the server and the entity is never saved.
 * Either a skill ({@link #ctx} + multiplier) or a basic shot ({@link #basicDamage}) owns it.
 */
public class SkillProjectile extends ThrowableItemProjectile {
	private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(SkillProjectile.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_TRAIL = SynchedEntityData.defineId(SkillProjectile.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> DATA_GRAVITY = SynchedEntityData.defineId(SkillProjectile.class, EntityDataSerializers.FLOAT);

	private @Nullable SkillContext ctx;
	private double multiplier;
	private @Nullable ServerPlayer basicOwner;
	private float basicDamage;
	private int pierce;
	private float explode;
	private int maxAge = 60;
	private boolean homing;
	private @Nullable BiConsumer<SkillContext, Vec3> impact;
	private final Set<UUID> hits = new HashSet<>();

	public SkillProjectile(final EntityType<? extends SkillProjectile> type, final Level level) {
		super(type, level);
	}

	private SkillProjectile(final Level level, final ServerPlayer owner, final ItemStack display) {
		super(ModEntities.SKILL_PROJECTILE, owner, level, display);
	}

	/** A skill projectile; position it with {@link #launch} and add it to the level. */
	public static SkillProjectile forSkill(final SkillContext ctx, final ItemStack display, final Fx.Kind trail, final float gravity, final double multiplier) {
		SkillProjectile p = new SkillProjectile(ctx.level, ctx.caster, display);
		p.ctx = ctx;
		p.multiplier = multiplier;
		p.setVisuals(ctx.fx.color(), trail, gravity);
		return p;
	}

	public static SkillProjectile forBasicShot(final ServerPlayer owner, final ItemStack display, final int color, final Fx.Kind trail, final float gravity, final float damage) {
		SkillProjectile p = new SkillProjectile(owner.level(), owner, display);
		p.basicOwner = owner;
		p.basicDamage = damage;
		p.setVisuals(color, trail, gravity);
		return p;
	}

	private void setVisuals(final int color, final Fx.Kind trail, final float gravity) {
		this.entityData.set(DATA_COLOR, color);
		this.entityData.set(DATA_TRAIL, trail.ordinal());
		this.entityData.set(DATA_GRAVITY, gravity);
	}

	public SkillProjectile pierce(final int pierce) {
		this.pierce = pierce;
		return this;
	}

	public SkillProjectile explode(final float radius) {
		this.explode = radius;
		return this;
	}

	public SkillProjectile maxAge(final int ticks) {
		this.maxAge = ticks;
		return this;
	}

	public SkillProjectile homing(final boolean homing) {
		this.homing = homing;
		return this;
	}

	public SkillProjectile onImpact(final @Nullable BiConsumer<SkillContext, Vec3> impact) {
		this.impact = impact;
		return this;
	}

	/** Sets position and velocity. */
	public SkillProjectile launch(final Vec3 from, final Vec3 direction, final float speed) {
		this.setPos(from.x, from.y, from.z);
		this.shoot(direction.x, direction.y, direction.z, speed, 0.0F);
		return this;
	}

	@Override
	protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_COLOR, 0xFFFFFF);
		entityData.define(DATA_TRAIL, Fx.Kind.SPARK.ordinal());
		entityData.define(DATA_GRAVITY, 0.0F);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.AIR;
	}

	@Override
	protected double getDefaultGravity() {
		return this.entityData.get(DATA_GRAVITY);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			Fx.Kind trail = Fx.Kind.values()[Math.floorMod(this.entityData.get(DATA_TRAIL), Fx.Kind.values().length)];
			ColorParticleOption option = ColorParticleOption.create(trail.type(), 0xFF000000 | this.entityData.get(DATA_COLOR));
			this.level().addParticle(option, this.getX(), this.getY() + 0.1, this.getZ(), 0.0, 0.0, 0.0);
			return;
		}
		if (this.ctx == null && this.basicOwner == null || this.tickCount > this.maxAge) {
			this.impactAt(this.position());
			this.discard();
			return;
		}
		if (this.homing && this.tickCount > 3) {
			this.steer();
		}
	}

	private void steer() {
		ServerPlayer owner = this.owner();
		if (owner == null) {
			return;
		}
		AABB box = this.getBoundingBox().inflate(8.0);
		LivingEntity target = null;
		double best = Double.MAX_VALUE;
		for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, box, e -> CombatHooks.isEnemy(owner, e) && !this.hits.contains(e.getUUID()))) {
			double d = e.distanceToSqr(this);
			if (d < best) {
				best = d;
				target = e;
			}
		}
		if (target != null) {
			Vec3 motion = this.getDeltaMovement();
			Vec3 want = target.getBoundingBox().getCenter().subtract(this.position()).normalize().scale(motion.length());
			this.setDeltaMovement(motion.lerp(want, 0.25));
			this.needsSync = true;
		}
	}

	private @Nullable ServerPlayer owner() {
		return this.ctx != null ? this.ctx.caster : this.basicOwner;
	}

	@Override
	protected boolean canHitEntity(final Entity entity) {
		if (!super.canHitEntity(entity) || this.hits.contains(entity.getUUID())) {
			return false;
		}
		ServerPlayer owner = this.owner();
		if (owner == null) {
			return !this.level().isClientSide() ? false : entity != this.getOwner();
		}
		return this.ctx != null ? CombatHooks.isEnemy(owner, entity) : CombatHooks.canHarm(owner, entity);
	}

	@Override
	protected void onHitEntity(final EntityHitResult hitResult) {
		if (this.level().isClientSide() || !(hitResult.getEntity() instanceof LivingEntity target)) {
			return;
		}
		if (this.explode > 0.0F) {
			this.impactAt(hitResult.getLocation());
			this.discard();
			return;
		}
		this.hits.add(target.getUUID());
		this.damage(target);
		if (--this.pierce < 0) {
			this.impactAt(hitResult.getLocation());
			this.discard();
		}
	}

	@Override
	protected void onHitBlock(final BlockHitResult hitResult) {
		super.onHitBlock(hitResult);
		if (!this.level().isClientSide()) {
			this.impactAt(hitResult.getLocation());
			this.discard();
		}
	}

	private void damage(final LivingEntity target) {
		if (this.ctx != null) {
			if (this.ctx.valid()) {
				this.ctx.hit(target, this.multiplier, this.damageSources().thrown(this, this.ctx.caster));
			}
		} else if (this.basicOwner != null) {
			CombatHooks.deal(this.basicOwner, target, this.basicDamage, this.damageSources().thrown(this, this.basicOwner), CombatHooks.DamageKind.SHOT);
		}
	}

	private void impactAt(final Vec3 pos) {
		if (!(this.level() instanceof ServerLevel level) || this.isRemoved()) {
			return;
		}
		if (this.explode > 0.0F) {
			ServerPlayer owner = this.owner();
			if (owner != null) {
				AABB box = new AABB(pos, pos).inflate(this.explode);
				for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.getBoundingBox().distanceToSqr(pos) <= this.explode * this.explode)) {
					if (this.ctx != null ? CombatHooks.isEnemy(owner, e) : CombatHooks.canHarm(owner, e)) {
						this.damage(e);
					}
				}
			}
			level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
			int color = this.entityData.get(DATA_COLOR);
			level.sendParticles(ColorParticleOption.create(Fx.Kind.SMOKE.type(), 0xFF000000 | color), pos.x, pos.y, pos.z, 12, this.explode / 3, 0.3, this.explode / 3, 0.02);
			level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7F, 1.2F);
		}
		if (this.impact != null && this.ctx != null && this.ctx.valid()) {
			BiConsumer<SkillContext, Vec3> action = this.impact;
			this.impact = null;
			action.accept(this.ctx, pos);
		}
	}
}
