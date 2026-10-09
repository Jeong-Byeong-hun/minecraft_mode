package com.minecraftmode.companion;

import com.minecraftmode.entity.AnimatedCreature;
import com.minecraftmode.entity.CreatureAnim;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A summoned mount: a saddled, tamed horse body with the mount's own model, speed and jump. Only its owner rides it; it cannot
 * be hurt, has no inventory, is never saved and leaves a few seconds after its rider gets off. A flying mount flies while
 * airborne: it goes where its rider looks while moving forward and glides down otherwise.
 */
public class MountEntity extends AbstractHorse implements AnimatedCreature {
	private static final int EMPTY_TICKS = 60;

	private @Nullable UUID owner;
	private int emptyTicks;

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

	@Override
	public boolean isFlyingCreature() {
		Companions.MountDef def = this.def();
		return def != null && def.flying() && !this.onGround();
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
	public void tick() {
		Companions.MountDef def = this.def();
		this.setNoGravity(def != null && def.flying() && this.isVehicle() && !this.onGround());
		super.tick();
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		ServerPlayer ownerPlayer = this.owner == null ? null : level.getServer().getPlayerList().getPlayer(this.owner);
		if (ownerPlayer == null) {
			this.discard();
			return;
		}
		this.emptyTicks = this.isVehicle() ? 0 : this.emptyTicks + 1;
		if (this.emptyTicks > EMPTY_TICKS) {
			this.discard();
		}
	}

	@Override
	public void travel(final Vec3 input) {
		Companions.MountDef def = this.def();
		if (def != null && def.flying() && this.isVehicle() && this.getControllingPassenger() instanceof Player rider && !this.onGround()) {
			float pitch = rider.getXRot() * Mth.DEG_TO_RAD;
			float yaw = this.getYRot() * Mth.DEG_TO_RAD;
			double speed = this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 2.0;
			Vec3 wanted = input.z > 0
				? new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch)).scale(speed * input.z)
				: new Vec3(0, -0.06, 0);
			this.setDeltaMovement(this.getDeltaMovement().scale(0.7).add(wanted.scale(0.3)));
			this.move(MoverType.SELF, this.getDeltaMovement());
			this.calculateEntityAnimation(true);
			return;
		}
		super.travel(input);
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

	@Override
	public boolean causeFallDamage(final double fallDistance, final float damageModifier, final DamageSource damageSource) {
		// the mount itself cannot be hurt, but like a horse it passes the fall on to its rider
		this.propagateFallToPassengers(fallDistance, damageModifier, damageSource);
		return false;
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
