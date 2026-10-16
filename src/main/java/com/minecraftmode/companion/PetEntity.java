package com.minecraftmode.companion;

import com.minecraftmode.entity.AnimatedCreature;
import com.minecraftmode.entity.CreatureAnim;
import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A summoned pet: follows its owner (walkers path after them, flyers hover by their shoulder), teleports back when left behind,
 * cannot be hurt or targeted and is never saved with the world. It leaves when its owner is gone or in another world. Once its owner
 * has stood still for {@link #SETTLE_TICKS}, a walker sits or lies down by them and a flyer lands on their shoulder.
 */
public class PetEntity extends PathfinderMob implements AnimatedCreature {
	/** How long the owner must stand still before the pet settles down. */
	public static final int SETTLE_TICKS = 60;
	/** Owner movement per tick (blocks) that still counts as standing still. */
	private static final double STILL = 0.03;
	private static final EntityDataAccessor<Boolean> DATA_RESTING = SynchedEntityData.defineId(PetEntity.class, EntityDataSerializers.BOOLEAN);
	/** Low 8 bits: {@link CreatureAnim} ordinal; higher bits: a counter so the same animation can replay. */
	private static final EntityDataAccessor<Integer> DATA_ANIM = SynchedEntityData.defineId(PetEntity.class, EntityDataSerializers.INT);
	/** Share of the way to the rest pose (or folded wings) the client moves each tick. */
	private static final float EASE = 0.2F;
	/** Ticks between two reactions to the owner's kills or crouches. */
	private static final int REACT_COOLDOWN = 60;
	/** How close a kill or a crouching owner must be for the pet to react. */
	private static final double REACT_RANGE = 24.0;
	private static final double CHEER_RANGE = 4.0;

	/** Server only; pets are never saved, so the owner never needs to survive a reload. */
	private @Nullable UUID owner;
	private int ownerStill;
	private @Nullable Vec3 ownerLast;
	private boolean ownerCrouched;
	private int reactCooldown;
	/** Where the pet keeps looking after a kill, and for how many more ticks. */
	private @Nullable Vec3 watching;
	private int watchTicks;
	private static final int WATCH_TICKS = 20;
	private int animCounter;
	/** Client: tickCount when the current animation arrived. Server: when it started. */
	private int animStart;
	/** Client only: eased rest pose and wing fold. */
	private float restO;
	private float restNow;
	private float foldO;
	private float foldNow = 1.0F;

	public PetEntity(final EntityType<? extends PetEntity> type, final Level level) {
		super(type, level);
		this.setPermanentlyInvulnerable(true);
		this.setPersistenceRequired();
	}

	@Override
	protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_RESTING, false);
		entityData.define(DATA_ANIM, 0);
	}

	@Override
	public void onSyncedDataUpdated(final EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (DATA_ANIM.equals(key)) {
			// the first value arrives with the spawn (tick 0): an old reaction, already over, not one to play now
			this.animStart = this.tickCount == 0 ? -CreatureAnim.values().length * 1000 : this.tickCount;
		}
	}

	public void playAnim(final CreatureAnim anim) {
		this.animCounter = (this.animCounter + 1) & 0xFFFF;
		this.entityData.set(DATA_ANIM, anim.ordinal() | this.animCounter << 8);
		this.animStart = this.tickCount;
	}

	@Override
	public float rest(final float partialTicks) {
		return Mth.lerp(partialTicks, this.restO, this.restNow);
	}

	@Override
	public float wingsFolded(final float partialTicks) {
		return Mth.lerp(partialTicks, this.foldO, this.foldNow);
	}

	@Override
	public boolean isResting() {
		return this.entityData.get(DATA_RESTING);
	}

	public void setResting(final boolean resting) {
		this.entityData.set(DATA_RESTING, resting);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.FLYING_SPEED, 0.5)
			.add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.STEP_HEIGHT, 1.0);
	}

	public void setOwner(final UUID owner) {
		this.owner = owner;
	}

	public @Nullable UUID owner() {
		return this.owner;
	}

	public Companions.@Nullable PetDef def() {
		String path = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()).getPath();
		return Companions.pet(path.substring("pet_".length()));
	}

	@Override
	public boolean isFlyingCreature() {
		Companions.PetDef def = this.def();
		return def != null && def.flying();
	}

	@Override
	public CreatureAnim anim() {
		CreatureAnim anim = CreatureAnim.byId(this.entityData.get(DATA_ANIM) & 0xFF);
		return this.tickCount - this.animStart > anim.ticks() ? CreatureAnim.NONE : anim;
	}

	@Override
	public float animTime(final float partialTicks) {
		return this.anim() == CreatureAnim.NONE ? 0.0F : this.tickCount - this.animStart + partialTicks;
	}

	@Override
	public int phase() {
		return 0;
	}

	@Override
	public void tick() {
		this.setNoGravity(this.isFlyingCreature());
		super.tick();
		if (!(this.level() instanceof ServerLevel level)) {
			this.ease();
			Companions.PetDef def = this.def();
			if (def != null) {
				Flair.trail(this, def.id());
			}
			return;
		}
		if (this.reactCooldown > 0) {
			this.reactCooldown--;
		}
		UUID id = this.owner();
		ServerPlayer owner = id == null ? null : level.getServer().getPlayerList().getPlayer(id);
		if (owner == null || owner.level() != level || owner.isRemoved()) {
			this.discard();
			return;
		}
		if (this.isNoAi()) {
			return;
		}
		this.watch(owner);
		if (this.watchTicks > 0) {
			this.watchTicks--;
		}
		double dist = this.distanceToSqr(owner);
		if (dist > 20 * 20) {
			Vec3 at = owner.position().add(owner.getLookAngle().scale(-1.5)).add(0, this.isFlyingCreature() ? 1.6 : 0.2, 0);
			this.snapTo(at.x, at.y, at.z, owner.getYRot(), 0.0F);
			this.getNavigation().stop();
			this.setResting(false);
			return;
		}
		boolean settled = this.ownerStill >= SETTLE_TICKS;
		if (this.isFlyingCreature()) {
			this.fly(owner, settled);
		} else {
			this.walk(owner, dist, settled);
		}
	}

	/** Client: ease into and out of the rest pose and the folded wings. */
	private void ease() {
		this.restO = this.restNow;
		this.foldO = this.foldNow;
		this.restNow += ((this.isResting() ? 1.0F : 0.0F) - this.restNow) * EASE;
		this.foldNow += ((this.isFlyingCreature() && !this.isResting() ? 0.0F : 1.0F) - this.foldNow) * EASE;
	}

	/** Counts how long the owner has stood still; an owner who crouches next to the pet gets a happy hop. */
	private void watch(final ServerPlayer owner) {
		Vec3 now = owner.position();
		boolean still = this.ownerLast != null && now.distanceToSqr(this.ownerLast) < STILL * STILL && !owner.isPassenger();
		this.ownerStill = still ? this.ownerStill + 1 : 0;
		this.ownerLast = now;
		boolean crouched = owner.isShiftKeyDown();
		if (crouched && !this.ownerCrouched && this.distanceToSqr(owner) < CHEER_RANGE * CHEER_RANGE) {
			this.cheer();
		}
		this.ownerCrouched = crouched;
	}

	/** A happy hop (a spin for flyers) with hearts, when the owner crouches by it. */
	private void cheer() {
		if (this.reactCooldown > 0 || !(this.level() instanceof ServerLevel level)) {
			return;
		}
		this.reactCooldown = REACT_COOLDOWN / 3;
		this.ownerStill = 0;
		this.hop(this.isFlyingCreature() ? CreatureAnim.SPIN : CreatureAnim.LEAP);
		level.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getBbHeight() + 0.2, this.getZ(), 4, 0.3, 0.15, 0.3, 0.0);
		this.playAmbientSound();
	}

	/**
	 * The owner (or their party with them) killed {@code victim} nearby: the pet turns to it and shows off in its own way (see
	 * {@link Flair#react}).
	 */
	public void react(final LivingEntity victim) {
		Companions.PetDef def = this.def();
		if (def == null || this.reactCooldown > 0 || this.isNoAi() || this.distanceToSqr(victim) > REACT_RANGE * REACT_RANGE
			|| !(this.level() instanceof ServerLevel level)) {
			return;
		}
		this.reactCooldown = REACT_COOLDOWN;
		this.ownerStill = 0;
		this.watching = victim.getEyePosition();
		this.watchTicks = WATCH_TICKS;
		this.hop(Flair.of(def.id()).react());
		Flair.react(level, this, victim, def.id());
	}

	/** Turns to the owner, or to a fresh kill while it still watches one. */
	private void lookAt(final ServerPlayer owner) {
		if (this.watchTicks > 0 && this.watching != null) {
			this.getLookControl().setLookAt(this.watching.x, this.watching.y, this.watching.z, 90.0F, 90.0F);
		} else {
			this.getLookControl().setLookAt(owner, 30.0F, 30.0F);
		}
	}

	/** Plays {@code anim}; a leap also lifts a walker off the ground. */
	private void hop(final CreatureAnim anim) {
		this.playAnim(anim);
		if (anim == CreatureAnim.LEAP && this.onGround() && !this.isFlyingCreature()) {
			this.setDeltaMovement(this.getDeltaMovement().add(0, 0.42, 0));
		}
	}

	/** Hover by the shoulder; once the owner settles, land on it and fold the wings. */
	private void fly(final ServerPlayer owner, final boolean settled) {
		Vec3 side = Vec3.directionFromRotation(0.0F, owner.yBodyRot + 90.0F);
		Vec3 target = settled
			? owner.position().add(side.scale(0.38)).add(0, owner.getBbHeight() * 0.78, 0)
			: owner.position().add(owner.getLookAngle().yRot((float)Math.PI / 2).normalize().scale(0.9))
				.add(0, owner.getBbHeight() + 0.3 + Math.sin(this.tickCount * 0.1) * 0.15, 0);
		Vec3 to = target.subtract(this.position());
		boolean perched = settled && to.lengthSqr() < 0.15 * 0.15;
		if (perched) {
			this.setPos(target);
			this.setDeltaMovement(Vec3.ZERO);
			this.setYRot(owner.yBodyRot);
			this.yBodyRot = owner.yBodyRot;
			this.yHeadRot = owner.getYHeadRot();
		} else {
			this.setDeltaMovement(to.scale(to.length() > 6 ? 0.25 : settled ? 0.3 : 0.12));
			this.lookAt(owner);
		}
		this.setResting(perched);
	}

	/** Path after the owner; once they settle and the pet is close, sit or lie down facing the way they look. */
	private void walk(final ServerPlayer owner, final double dist, final boolean settled) {
		boolean rest = settled && dist < 3.5 * 3.5 && this.onGround();
		if (rest) {
			this.getNavigation().stop();
			this.getLookControl().setLookAt(owner.getEyePosition().add(owner.getLookAngle().scale(4.0)));
		} else {
			if (this.tickCount % 10 == 0) {
				if (dist > 3.5 * 3.5) {
					this.getNavigation().moveTo(owner, dist > 10 * 10 ? 1.6 : 1.15);
				} else {
					this.getNavigation().stop();
				}
			}
			this.lookAt(owner);
		}
		this.setResting(rest);
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		Companions.PetDef def = this.def();
		return def == null ? null : Flair.of(def.id()).voice();
	}

	@Override
	public float getVoicePitch() {
		Companions.PetDef def = this.def();
		float pitch = def == null ? 1.0F : Flair.of(def.id()).pitch();
		return pitch + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F;
	}

	@Override
	protected float getSoundVolume() {
		Companions.PetDef def = this.def();
		return def == null ? 0.5F : Flair.of(def.id()).volume();
	}

	@Override
	public int getAmbientSoundInterval() {
		return 400;
	}

	@Override
	public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
		return false;
	}

	/**
	 * Never in the way: shots, skill projectiles and melee swings pass through (the crosshair and every projectile skip unpickable
	 * entities). Until 0.5.20 a pet by the shoulder caught its owner's arrows, which bounced off it (it is never hurt).
	 */
	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean canBeSeenAsEnemy() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(final net.minecraft.world.entity.Entity entity) {
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(final double distSqr) {
		return false;
	}

	@Override
	public boolean causeFallDamage(final double fallDistance, final float damageModifier, final DamageSource damageSource) {
		return false;
	}

	@Override
	public boolean isInvulnerableTo(final ServerLevel level, final DamageSource source) {
		return true;
	}


}
