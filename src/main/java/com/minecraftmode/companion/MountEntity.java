package com.minecraftmode.companion;

import com.minecraftmode.entity.AnimatedCreature;
import com.minecraftmode.entity.CreatureAnim;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A summoned mount: a saddled, tamed horse body with the mount's own model, speed and jump. Only its owner rides it; it cannot
 * be hurt, has no inventory, is never saved and leaves a few seconds after its rider gets off. Sprinting runs it
 * {@link #SPRINT_BOOST} faster. A flying mount takes off and climbs while jump is held, goes where its rider looks while moving
 * forward, strafes sideways and glides down otherwise; it banks into turns.
 */
public class MountEntity extends AbstractHorse implements AnimatedCreature {
	private static final int EMPTY_TICKS = 60;
	public static final float SPRINT_BOOST = 1.3F;
	/** Climb per tick while jump is held in flight, as a share of the flight speed. */
	private static final double CLIMB = 0.6;
	private static final double GLIDE = -0.05;
	/** How much of the wanted velocity a flyer takes each tick (the rest is momentum). */
	private static final double FLIGHT_RESPONSE = 0.25;
	private static final float MAX_ROLL = 30.0F;
	private static final float MAX_TILT = 25.0F;

	private @Nullable UUID owner;
	private int emptyTicks;
	/** Client only: bank and pitch of a flyer, smoothed. */
	private float roll;
	private float rollO;
	private float tilt;
	private float tiltO;
	/** Client only: eased wing fold of a flyer (folded on the ground). */
	private float fold = 1.0F;
	private float foldO = 1.0F;

	public MountEntity(final EntityType<? extends MountEntity> type, final Level level) {
		super(type, level);
		this.setPermanentlyInvulnerable(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return AbstractHorse.createBaseHorseAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.STEP_HEIGHT, 1.1);
	}

	public Companions.@Nullable MountDef def() {
		String path = BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()).getPath();
		return Companions.mount(path.substring("mount_".length()));
	}

	/** Tame to {@code owner} and give the mount's speed and jump. */
	public void setup(final ServerPlayer owner) {
		Companions.MountDef def = this.def();
		this.owner = owner.getUUID();
		this.setTamed(true);
		this.setOwner(owner);
		if (def != null) {
			this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(def.speed());
			this.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(def.jump());
		}
		this.setHealth(this.getMaxHealth());
	}

	/** Always ready to ride (a saddle item would need the type in the vanilla saddle tag). */
	@Override
	public boolean isSaddled() {
		return true;
	}

	/**
	 * Flying only with a rider and off the ground. (An empty mount stands still, and the client never learns that a mob that has not
	 * moved is on the ground, so {@code onGround} alone would spread its wings.)
	 */
	@Override
	public boolean isFlyingCreature() {
		return this.flies() && this.isVehicle() && !this.onGround();
	}

	@Override
	public CreatureAnim anim() {
		return CreatureAnim.NONE;
	}

	@Override
	public float animTime(final float partialTicks) {
		return 0.0F;
	}

	@Override
	public int phase() {
		return 0;
	}

	@Override
	public float roll(final float partialTicks) {
		return Mth.lerp(partialTicks, this.rollO, this.roll);
	}

	@Override
	public float tilt(final float partialTicks) {
		return Mth.lerp(partialTicks, this.tiltO, this.tilt);
	}

	@Override
	public float wingsFolded(final float partialTicks) {
		return Mth.lerp(partialTicks, this.foldO, this.fold);
	}

	/** Vanilla rears a horse up while its rider charges a jump. */
	@Override
	public float stand(final float partialTicks) {
		return this.getStandAnim(partialTicks);
	}

	/** Riders may sprint on it (vanilla lets a rider sprint only on mounts that allow it). */
	@Override
	public boolean canSprint() {
		return true;
	}

	/** Flyers take off with the jump key held instead of a charged jump. */
	@Override
	public boolean canJump() {
		return !this.flies() && super.canJump();
	}

	@Override
	protected float getRiddenSpeed(final Player controller) {
		return super.getRiddenSpeed(controller) * (controller.isSprinting() ? SPRINT_BOOST : 1.0F);
	}

	@Override
	public void tick() {
		boolean flies = this.flies();
		this.setNoGravity(flies && this.isVehicle() && !this.onGround());
		if (flies) {
			this.resetFallDistance();
		}
		// a sprinting rider makes the mount sprint too: vanilla then kicks up the ground under it
		this.setSprinting(this.getControllingPassenger() instanceof Player rider && rider.isSprinting());
		super.tick();
		if (!(this.level() instanceof ServerLevel level)) {
			this.clientTick();
			return;
		}
		ServerPlayer ownerPlayer = this.owner == null ? null : level.getServer().getPlayerList().getPlayer(this.owner);
		if (ownerPlayer == null) {
			this.discard();
			return;
		}
		this.emptyTicks = this.isVehicle() ? 0 : this.emptyTicks + 1;
		if (this.emptyTicks > EMPTY_TICKS) {
			Flair.leave(level, this);
			this.discard();
		}
	}

	/** Bank into turns and pitch with the climb while flying; trail particles while moving or flying. */
	private void clientTick() {
		this.rollO = this.roll;
		this.tiltO = this.tilt;
		this.foldO = this.fold;
		boolean airborne = this.isFlyingCreature();
		this.fold += ((airborne ? 0.0F : 1.0F) - this.fold) * 0.2F;
		float turn = Mth.wrapDegrees(this.getYRot() - this.yRotO);
		float wantRoll = airborne ? Mth.clamp(-turn * 3.0F, -MAX_ROLL, MAX_ROLL) : 0.0F;
		float wantTilt = airborne ? Mth.clamp((float)(this.getY() - this.yo) * 60.0F, -MAX_TILT, MAX_TILT) : 0.0F;
		this.roll += (wantRoll - this.roll) * 0.2F;
		this.tilt += (wantTilt - this.tilt) * 0.2F;
		Companions.MountDef def = this.def();
		boolean moving = Math.hypot(this.getX() - this.xo, this.getZ() - this.zo) > 0.05;
		if (def != null && (moving || airborne)) {
			Flair.trail(this, def.id());
		}
	}

	@Override
	public void travel(final Vec3 input) {
		Companions.MountDef def = this.def();
		if (def != null && def.flying() && this.isVehicle() && this.getControllingPassenger() instanceof Player rider
			&& (!this.onGround() || rider.isJumping())) {
			this.setDeltaMovement(this.getDeltaMovement().scale(1.0 - FLIGHT_RESPONSE).add(this.flight(rider, input).scale(FLIGHT_RESPONSE)));
			this.move(MoverType.SELF, this.getDeltaMovement());
			this.calculateEntityAnimation(true);
			return;
		}
		super.travel(input);
	}

	/** The velocity the rider asks for: forward where they look, sideways, up while jump is held, else a slow glide down. */
	private Vec3 flight(final Player rider, final Vec3 input) {
		float pitch = rider.getXRot() * Mth.DEG_TO_RAD;
		float yaw = this.getYRot() * Mth.DEG_TO_RAD;
		double speed = this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 2.0 * (rider.isSprinting() ? SPRINT_BOOST : 1.0);
		Vec3 wanted = Vec3.ZERO;
		if (input.z > 0) {
			wanted = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch)).scale(speed * input.z);
		} else if (input.z < 0) {
			wanted = new Vec3(Mth.sin(yaw), 0, -Mth.cos(yaw)).scale(speed * 0.5);
		}
		// ridden input x is already halved; positive is left
		wanted = wanted.add(new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw)).scale(speed * input.x));
		double up = rider.isJumping() ? speed * CLIMB : input.z > 0 ? 0.0 : GLIDE;
		return wanted.add(0, up, 0);
	}

	/** Paws and claws pad; only hooves clop like a horse. */
	@Override
	protected void playStepSound(final BlockPos pos, final BlockState blockState) {
		Companions.MountDef def = this.def();
		if (def != null && Flair.hooved(def.id())) {
			super.playStepSound(pos, blockState);
			return;
		}
		SoundType type = blockState.getSoundType();
		this.playSound(type.getStepSound(), type.getVolume() * 0.2F, type.getPitch() * 0.9F);
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		Companions.MountDef def = this.def();
		return def == null ? null : Flair.of(def.id()).voice();
	}

	@Override
	public float getVoicePitch() {
		Companions.MountDef def = this.def();
		float pitch = def == null ? 1.0F : Flair.of(def.id()).pitch();
		return pitch + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F;
	}

	@Override
	protected float getSoundVolume() {
		Companions.MountDef def = this.def();
		return def == null ? 0.5F : Flair.of(def.id()).volume();
	}

	@Override
	public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
		if (player.getUUID().equals(this.owner) && !this.isVehicle()) {
			if (!this.level().isClientSide()) {
				player.startRiding(this);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Override
	public void openCustomInventoryScreen(final Player player) {
	}

	@Override
	public boolean isFood(final ItemStack itemStack) {
		return false;
	}

	@Override
	public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
		return false;
	}

	@Override
	public boolean isInvulnerableTo(final ServerLevel level, final DamageSource source) {
		return true;
	}

	/** Projectiles fly through (it is never hurt, so it only caught shots); it stays pickable to be ridden. */
	@Override
	public boolean canBeHitByProjectile() {
		return false;
	}

	/**
	 * The mount itself is never hurt. A flying mount lands wherever its rider steers it, so a landing is never a fall (its fall
	 * distance would otherwise add up every block it glided down since takeoff). A ground mount passes a fall on to its rider only
	 * when a horse would have felt it (safe for 6 blocks, half damage), so a charged jump on flat ground costs nothing.
	 */
	@Override
	public boolean causeFallDamage(final double fallDistance, final float damageModifier, final DamageSource damageSource) {
		if (this.flies()) {
			return false;
		}
		if (this.calculateFallDamage(fallDistance, damageModifier) > 0) {
			this.propagateFallToPassengers(fallDistance, damageModifier, damageSource);
		}
		return false;
	}

	public boolean flies() {
		Companions.MountDef def = this.def();
		return def != null && def.flying();
	}

	/**
	 * Never saved, neither with the world nor with its rider. (The type itself must stay serializable: players can only ride
	 * entities whose type can be saved.)
	 */
	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean saveAsPassenger(final ValueOutput output) {
		return false;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
		return null;
	}
}
