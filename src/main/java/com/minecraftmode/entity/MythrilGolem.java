package com.minecraftmode.entity;

import com.minecraftmode.registry.ModEffects;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Rare mini-boss of the deep caves (below Y=0, at most one per 64 blocks). Hits like an iron golem
 * and periodically slams the ground, hurting, knocking back and cutting nearby players.
 */
public class MythrilGolem extends Monster {
	private static final int MAX_SPAWN_Y = 0;
	private static final double SPAWN_EXCLUSION_RADIUS = 64.0;
	private static final int SLAM_COOLDOWN_TICKS = 160;
	private static final double SLAM_RADIUS = 4.5;
	private static final float SLAM_DAMAGE = 8.0F;

	private final ServerBossEvent bossEvent = new ServerBossEvent(
		Mth.createInsecureUUID(this.random), this.getDisplayName(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10
	);
	private int attackAnimationTick;
	private int slamCooldown = SLAM_COOLDOWN_TICKS;

	public MythrilGolem(final EntityType<? extends MythrilGolem> type, final Level level) {
		super(type, level);
		this.xpReward = 50;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 150.0)
			.add(Attributes.ARMOR, 10.0)
			.add(Attributes.MOVEMENT_SPEED, 0.23)
			.add(Attributes.ATTACK_DAMAGE, 12.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.FOLLOW_RANGE, 32.0)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	public static boolean checkMythrilGolemSpawnRules(
		final EntityType<MythrilGolem> type, final ServerLevelAccessor level, final EntitySpawnReason spawnReason, final BlockPos pos, final RandomSource random
	) {
		if (spawnReason == EntitySpawnReason.NATURAL) {
			if (pos.getY() >= MAX_SPAWN_Y) {
				return false;
			}
			if (!level.getEntitiesOfClass(MythrilGolem.class, new AABB(pos).inflate(SPAWN_EXCLUSION_RADIUS)).isEmpty()) {
				return false;
			}
		}

		return Monster.checkMonsterSpawnRules(type, level, spawnReason, pos, random);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.attackAnimationTick > 0) {
			this.attackAnimationTick--;
		}
	}

	@Override
	protected void customServerAiStep(final ServerLevel level) {
		super.customServerAiStep(level);
		this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
		if (this.slamCooldown > 0) {
			this.slamCooldown--;
		}
		LivingEntity target = this.getTarget();
		if (this.slamCooldown == 0 && target != null && this.distanceToSqr(target) < SLAM_RADIUS * SLAM_RADIUS) {
			this.groundSlam(level);
			this.slamCooldown = SLAM_COOLDOWN_TICKS;
		}
	}

	/** Damages, knocks back and cuts every player within {@link #SLAM_RADIUS}. Returns how many were hit. */
	public int groundSlam(final ServerLevel level) {
		this.attackAnimationTick = 10;
		level.broadcastEntityEvent(this, (byte) 4);
		level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.2, this.getZ(), 6, SLAM_RADIUS / 2, 0.1, SLAM_RADIUS / 2, 0.0);
		this.playSound(SoundEvents.ANVIL_LAND, 1.2F, 0.6F);

		List<Player> players = level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(SLAM_RADIUS), p -> !p.isSpectator() && !p.isCreative());
		for (Player player : players) {
			if (player.hurtServer(level, this.damageSources().mobAttack(this), SLAM_DAMAGE)) {
				Vec3 away = player.position().subtract(this.position()).multiply(1, 0, 1).normalize().scale(1.2);
				player.push(away.x, 0.5, away.z);
				player.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 60, 0), this);
			}
		}
		return players.size();
	}

	@Override
	public boolean doHurtTarget(final ServerLevel level, final Entity target) {
		this.attackAnimationTick = 10;
		level.broadcastEntityEvent(this, (byte) 4);
		boolean hurt = super.doHurtTarget(level, target);
		if (hurt) {
			// Iron-golem style uppercut
			target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.4, 0.0));
		}
		return hurt;
	}

	@Override
	public void handleEntityEvent(final @EntityEvent.Value byte id) {
		if (id == 4) {
			this.attackAnimationTick = 10;
			this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.8F);
		} else {
			super.handleEntityEvent(id);
		}
	}

	public int getAttackAnimationTick() {
		return this.attackAnimationTick;
	}

	@Override
	public boolean canBeAffected(final MobEffectInstance effect) {
		// Made of metal: it does not bleed.
		return !effect.is(ModEffects.BLEEDING) && super.canBeAffected(effect);
	}

	@Override
	public void startSeenByPlayer(final ServerPlayer player) {
		super.startSeenByPlayer(player);
		this.bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(final ServerPlayer player) {
		super.stopSeenByPlayer(player);
		this.bossEvent.removePlayer(player);
	}

	public ServerBossEvent bossEvent() {
		return this.bossEvent;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.IRON_GOLEM_STEP;
	}

	@Override
	protected SoundEvent getHurtSound(final DamageSource source) {
		return SoundEvents.IRON_GOLEM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.IRON_GOLEM_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.7F;
	}
}
