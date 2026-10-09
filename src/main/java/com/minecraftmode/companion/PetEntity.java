package com.minecraftmode.companion;

import com.minecraftmode.entity.AnimatedCreature;
import com.minecraftmode.entity.CreatureAnim;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
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
 * cannot be hurt or targeted and is never saved with the world. It leaves when its owner is gone or in another world.
 */
public class PetEntity extends PathfinderMob implements AnimatedCreature {
	/** Server only; pets are never saved, so the owner never needs to survive a reload. */
	private @Nullable UUID owner;

	public PetEntity(final EntityType<? extends PetEntity> type, final Level level) {
		super(type, level);
		this.setPermanentlyInvulnerable(true);
		this.setPersistenceRequired();
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
		this.setNoGravity(this.isFlyingCreature());
		super.tick();
		if (!(this.level() instanceof ServerLevel level)) {
			return;
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
		double dist = this.distanceToSqr(owner);
		if (dist > 20 * 20) {
			Vec3 at = owner.position().add(owner.getLookAngle().scale(-1.5)).add(0, this.isFlyingCreature() ? 1.6 : 0.2, 0);
			this.snapTo(at.x, at.y, at.z, owner.getYRot(), 0.0F);
			this.getNavigation().stop();
			return;
		}
		if (this.isFlyingCreature()) {
			// hover by the shoulder
			Vec3 side = owner.getLookAngle().yRot((float)Math.PI / 2).normalize();
			Vec3 target = owner.position().add(side.scale(0.9)).add(0, owner.getBbHeight() + 0.3 + Math.sin(this.tickCount * 0.1) * 0.15, 0);
			Vec3 to = target.subtract(this.position());
			this.setDeltaMovement(to.scale(to.length() > 6 ? 0.25 : 0.12));
			this.getLookControl().setLookAt(owner, 30.0F, 30.0F);
		} else if (this.tickCount % 10 == 0) {
			if (dist > 3.5 * 3.5) {
				this.getNavigation().moveTo(owner, dist > 10 * 10 ? 1.6 : 1.15);
			} else {
				this.getNavigation().stop();
			}
		}
		if (!this.isFlyingCreature()) {
			this.getLookControl().setLookAt(owner, 30.0F, 30.0F);
		}
	}

	@Override
	public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
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
