package com.minecraftmode.entity;

import com.minecraftmode.registry.ModEffects;
import com.minecraftmode.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

/**
 * A greedy miner that lurks in caves (below Y=40). Swings a pickaxe and
 * has a chance to make its target bleed.
 */
public class MineRaider extends Monster {
	private static final int MAX_SPAWN_Y = 40;
	private static final float BLEED_CHANCE = 0.4F;
	private static final int BLEED_TICKS = 100;

	public MineRaider(final EntityType<? extends MineRaider> type, final Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 26.0)
			.add(Attributes.MOVEMENT_SPEED, 0.27)
			.add(Attributes.ATTACK_DAMAGE, 4.0)
			.add(Attributes.FOLLOW_RANGE, 24.0)
			.add(Attributes.ARMOR, 2.0);
	}

	public static boolean checkMineRaiderSpawnRules(
		final EntityType<MineRaider> type, final ServerLevelAccessor level, final EntitySpawnReason spawnReason, final BlockPos pos, final RandomSource random
	) {
		if (spawnReason == EntitySpawnReason.NATURAL && pos.getY() >= MAX_SPAWN_Y) {
			return false;
		}

		return Monster.checkMonsterSpawnRules(type, level, spawnReason, pos, random);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(
		final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
	) {
		SpawnGroupData spawnGroupData = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
		this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
		return spawnGroupData;
	}

	@Override
	protected void populateDefaultEquipmentSlots(final RandomSource random, final DifficultyInstance difficulty) {
		boolean mythril = random.nextFloat() < 0.15F;
		this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(mythril ? ModItems.MYTHRIL_PICKAXE : Items.IRON_PICKAXE));
		this.setDropChance(EquipmentSlot.MAINHAND, 0.05F);
	}

	@Override
	public boolean doHurtTarget(final ServerLevel level, final Entity target) {
		boolean hurt = super.doHurtTarget(level, target);
		if (hurt && target instanceof LivingEntity living && this.getRandom().nextFloat() < BLEED_CHANCE) {
			living.addEffect(new MobEffectInstance(ModEffects.BLEEDING, BLEED_TICKS, 0), this);
		}

		return hurt;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.PILLAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(final DamageSource source) {
		return SoundEvents.PILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.PILLAGER_DEATH;
	}
}
