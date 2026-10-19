package com.minecraftmode.job.skill;

import com.minecraftmode.registry.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A drawn skill effect ({@link SkillLook}) with no body: it only carries what the client draws (look, color, size, the entity it is
 * held by) and removes itself after the look's ticks. Never saved, never hit.
 */
public class SkillVfx extends Entity {
	private static final EntityDataAccessor<Integer> DATA_LOOK = SynchedEntityData.defineId(SkillVfx.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(SkillVfx.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(SkillVfx.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> DATA_ANCHOR = SynchedEntityData.defineId(SkillVfx.class, EntityDataSerializers.INT);

	public SkillVfx(final EntityType<? extends SkillVfx> type, final Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	/** Shows {@code look} where the caster stands (held by them for anchored looks), sized by the Expansion engraving. */
	public static void spawn(final SkillContext ctx, final SkillLook look) {
		if (look == SkillLook.NONE || look.ticks() <= 0) {
			return;
		}
		SkillVfx vfx = new SkillVfx(ModEntities.SKILL_VFX, ctx.level);
		Vec3 at = look.anchored() ? ctx.caster.position() : ctx.caster.position().add(0, 0.2, 0);
		vfx.setPos(at.x, at.y, at.z);
		vfx.entityData.set(DATA_LOOK, look.ordinal());
		vfx.entityData.set(DATA_COLOR, ctx.fx.color());
		vfx.entityData.set(DATA_SIZE, (float)ctx.area(look.size()));
		vfx.entityData.set(DATA_ANCHOR, look.anchored() ? ctx.caster.getId() : -1);
		ctx.level.addFreshEntity(vfx);
	}

	public SkillLook look() {
		return SkillLook.byId(this.entityData.get(DATA_LOOK));
	}

	public int color() {
		return this.entityData.get(DATA_COLOR);
	}

	public float size() {
		return this.entityData.get(DATA_SIZE);
	}

	public @Nullable Entity anchor() {
		int id = this.entityData.get(DATA_ANCHOR);
		return id < 0 ? null : this.level().getEntity(id);
	}

	@Override
	protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
		entityData.define(DATA_LOOK, SkillLook.NONE.ordinal());
		entityData.define(DATA_COLOR, 0xFFFFFF);
		entityData.define(DATA_SIZE, 1.0F);
		entityData.define(DATA_ANCHOR, -1);
	}

	@Override
	public void tick() {
		if (this.level().isClientSide()) {
			return;
		}
		Entity anchor = this.anchor();
		if (this.tickCount >= this.look().ticks() || this.look().anchored() && (anchor == null || anchor.isRemoved())) {
			this.discard();
			return;
		}
		if (anchor != null) {
			// keeps it tracked (and culled) where the holder is; the client draws it at the holder's hand
			this.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
		}
	}

	@Override
	protected void readAdditionalSaveData(final ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(final ValueOutput output) {
	}

	@Override
	protected boolean couldAcceptPassenger() {
		return false;
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE_ENTITY;
	}

	@Override
	public boolean isIgnoringBlockTriggers() {
		return true;
	}

	@Override
	public final boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
		return false;
	}
}
