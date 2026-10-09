package com.minecraftmode.entity;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.WeaponDef;
import com.minecraftmode.network.OpenTrainerPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Class trainer NPC: stands still, cannot be hurt, and opens the trainer dialog (class choice and
 * advancement trials) when right-clicked. One per class lives in the capital;
 * {@code CityServices.keepTrainers} respawns missing ones and brings wanderers back.
 */
public class ClassTrainer extends PathfinderMob {
	private static final EntityDataAccessor<Integer> DATA_JOB = SynchedEntityData.defineId(ClassTrainer.class, EntityDataSerializers.INT);

	public ClassTrainer(final EntityType<? extends ClassTrainer> type, final Level level) {
		super(type, level);
		this.setPermanentlyInvulnerable(true);
		this.setPersistenceRequired();
		this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_JOB, JobClass.WARRIOR.ordinal());
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10.0F, 1.0F));
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
	}

	public JobClass job() {
		int index = this.entityData.get(DATA_JOB);
		return index >= 0 && index < JobClass.values().length ? JobClass.values()[index] : JobClass.WARRIOR;
	}

	/** Sets the class, name tag and signature weapon. */
	public void setJob(final JobClass job) {
		this.entityData.set(DATA_JOB, job.ordinal());
		this.setCustomName(Component.translatable(nameKey(job)).withColor(job.color()));
		this.setCustomNameVisible(true);
		WeaponDef weapon = JobWeapons.def(signatureWeapon(job));
		this.setItemSlot(EquipmentSlot.MAINHAND, weapon == null ? ItemStack.EMPTY : new ItemStack(JobWeapons.item(weapon)));
	}

	public static String nameKey(final JobClass job) {
		return "entity.minecraft_mode.class_trainer." + job.id();
	}

	public static String greetingKey(final JobClass job) {
		return nameKey(job) + ".greeting";
	}

	private static String signatureWeapon(final JobClass job) {
		return switch (job) {
			case ROGUE -> "kamish_wrath";
			case MAGE -> "merlin_staff";
			case ARCHER -> "chiron_greatbow";
			case PIRATE -> "el_draque";
			case SHINIGAMI -> "benihime";
			case HUNTER -> "jajanken";
			default -> "excalibur";
		};
	}

	@Override
	protected InteractionResult mobInteract(final Player player, final InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer && ServerPlayNetworking.canSend(serverPlayer, OpenTrainerPayload.TYPE)) {
			ServerPlayNetworking.send(serverPlayer, new OpenTrainerPayload(this.getId(), this.job()));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void tick() {
		super.tick();
		// summoned without data: give it a name and weapon for its (default) class
		if (!this.level().isClientSide() && !this.hasCustomName()) {
			this.setJob(this.job());
		}
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(final double distSqr) {
		return false;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(final ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("Job", this.job().id());
	}

	@Override
	protected void readAdditionalSaveData(final ValueInput input) {
		super.readAdditionalSaveData(input);
		JobClass job = JobClass.byId(input.getStringOr("Job", "warrior"));
		this.setJob(job == JobClass.NONE ? JobClass.WARRIOR : job);
	}
}
