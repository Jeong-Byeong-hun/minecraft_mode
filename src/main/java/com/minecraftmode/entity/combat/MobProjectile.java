package com.minecraftmode.entity.combat;

import com.minecraftmode.job.skill.Fx;
import com.minecraftmode.registry.ModEntities;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Projectile of named monsters and bosses: shows an item and a tinted particle trail, hurts players
 * only, and can carry an effect, a small blast (no block damage), homing and an impact callback (for
 * pools, webs and similar). Never saved.
 */
public class MobProjectile extends ThrowableItemProjectile {
	private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(MobProjectile.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_TRAIL = SynchedEntityData.defineId(MobProjectile.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> DATA_GRAVITY = SynchedEntityData.defineId(MobProjectile.class, EntityDataSerializers.FLOAT);

	private float damage;
	private @Nullable Holder<MobEffect> effect;
	private int effectTicks;
	private int amplifier;
	private float blast;
	private int maxAge = 100;
	private @Nullable LivingEntity homing;
	private @Nullable Consumer<Vec3> impact;
	private boolean done;
	/** Only for show: hits nothing (the damage comes from a matching telegraph). */
	private boolean visual;
	/** A spell bolt ({@link MagicDamage}) rather than a thrown or shot object. */
	private boolean magic;

	public MobProjectile(final EntityType<? extends MobProjectile> type, final Level level) {
		super(type, level);
	}

	private MobProjectile(final Level level, final LivingEntity owner, final ItemStack display) {
		super(ModEntities.MOB_PROJECTILE, owner, level, display);
	}

	/** A projectile dealing {@code damage}; aim it with {@link #launch} and add it to the level. */
	public static MobProjectile of(final Mob owner, final ItemStack display, final Fx.Kind trail, final int color, final float gravity, final float damage) {
		MobProjectile p = new MobProjectile(owner.level(), owner, display);
		p.damage = damage;
		p.entityData.set(DATA_COLOR, color);
		p.entityData.set(DATA_TRAIL, trail.ordinal());
		p.entityData.set(DATA_GRAVITY, gravity);
		return p;
	}

	public MobProjectile magic(final boolean magic) {
		this.magic = magic;
		return this;
	}

	public MobProjectile effect(final Holder<MobEffect> effect, final int ticks, final int amplifier) {
		this.effect = effect;
		this.effectTicks = ticks;
		this.amplifier = amplifier;
		return this;
	}

	public MobProjectile blast(final float radius) {
		this.blast = radius;
		return this;
	}

	public MobProjectile homing(final @Nullable LivingEntity target) {
		this.homing = target;
		return this;
	}

	public MobProjectile visual() {
		this.visual = true;
		return this;
	}

	public MobProjectile maxAge(final int ticks) {
		this.maxAge = ticks;
		return this;
	}

	public MobProjectile onImpact(final Consumer<Vec3> impact) {
		this.impact = impact;
		return this;
	}

	public MobProjectile launch(final Vec3 from, final Vec3 direction, final float speed) {
		this.setPos(from.x, from.y, from.z);
		this.shoot(direction.x, direction.y, direction.z, speed, 0.0F);
		return this;
	}

	/** Fires at {@code target}'s chest with a little lead. */
	public MobProjectile at(final Vec3 from, final LivingEntity target, final float speed) {
		Vec3 aim = target.position().add(0, target.getBbHeight() * 0.55, 0).add(target.getDeltaMovement().scale(4.0));
		return this.launch(from, aim.subtract(from), speed);
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
			this.level().addParticle(ColorParticleOption.create(trail.type(), 0xFF000000 | this.entityData.get(DATA_COLOR)), this.getX(), this.getY() + 0.1, this.getZ(), 0, 0, 0);
			return;
		}
		if (this.tickCount > this.maxAge) {
			this.finish(this.position());
			return;
		}
		if (this.homing != null && this.homing.isAlive() && this.tickCount > 4) {
			Vec3 motion = this.getDeltaMovement();
			Vec3 want = this.homing.position().add(0, this.homing.getBbHeight() * 0.5, 0).subtract(this.position()).normalize().scale(motion.length());
			this.setDeltaMovement(motion.lerp(want, 0.12));
			this.needsSync = true;
		}
	}

	@Override
	protected boolean canHitEntity(final Entity entity) {
		return !this.visual && super.canHitEntity(entity) && Attacks.isTarget(entity);
	}

	@Override
	protected void onHitEntity(final EntityHitResult hitResult) {
		if (this.level().isClientSide() || !(hitResult.getEntity() instanceof LivingEntity target)) {
			return;
		}
		if (this.blast <= 0.0F) {
			this.hurt(target);
		}
		this.finish(hitResult.getLocation());
	}

	@Override
	protected void onHitBlock(final BlockHitResult hitResult) {
		super.onHitBlock(hitResult);
		if (!this.level().isClientSide()) {
			this.finish(hitResult.getLocation());
		}
	}

	private void hurt(final LivingEntity target) {
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		Entity owner = this.getOwner();
		LivingEntity shooter = owner instanceof LivingEntity living ? living : null;
		target.hurtServer(level, this.magic ? MagicDamage.source(level, this, shooter) : this.damageSources().mobProjectile(this, shooter), this.damage);
		if (this.effect != null) {
			target.addEffect(new MobEffectInstance(this.effect, this.effectTicks, this.amplifier), owner);
		}
	}

	private void finish(final Vec3 at) {
		if (this.done || !(this.level() instanceof ServerLevel level)) {
			return;
		}
		this.done = true;
		if (this.blast > 0.0F) {
			level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 2, this.blast / 3, 0.2, this.blast / 3, 0.0);
			level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.7F, 1.2F);
			for (LivingEntity e : Attacks.inCircle(level, at, this.blast, this.blast + 1.0)) {
				this.hurt(e);
			}
		}
		if (this.impact != null) {
			this.impact.accept(at);
		}
		this.discard();
	}
}
