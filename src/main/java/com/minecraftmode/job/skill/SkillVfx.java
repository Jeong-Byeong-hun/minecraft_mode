package com.minecraftmode.job.skill;

import com.minecraftmode.registry.ModEntities;
import java.util.ArrayList;
import java.util.List;
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
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * A drawn skill effect ({@link SkillLook}) with no body: it only carries what the client draws (look, color, size, how long it lasts,
 * where it points, the entity it is held by) and removes itself after its ticks. Never saved, never hit.
 */
public class SkillVfx extends Entity {
	private static final EntityDataAccessor<Integer> DATA_LOOK = SynchedEntityData.defineId(SkillVfx.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(SkillVfx.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(SkillVfx.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> DATA_ANCHOR = SynchedEntityData.defineId(SkillVfx.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_TICKS = SynchedEntityData.defineId(SkillVfx.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Vector3fc> DATA_REACH = SynchedEntityData.defineId(SkillVfx.class, EntityDataSerializers.VECTOR3);
	private static final EntityDataAccessor<Float> DATA_ARG = SynchedEntityData.defineId(SkillVfx.class, EntityDataSerializers.FLOAT);
	/** How many positions of its holder a {@link SkillLook.Hold#TRAIL} look keeps on the client. */
	private static final int TRAIL_POINTS = 24;

	/** Client only: where the holder of a {@link SkillLook.Hold#TRAIL} look has been, oldest first. */
	public final List<Vec3> trail = new ArrayList<>();

	public SkillVfx(final EntityType<? extends SkillVfx> type, final Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	/** Shows {@code look} where the caster stands (held by them for anchored looks), sized by the Expansion engraving. */
	public static void spawn(final SkillContext ctx, final SkillLook look) {
		spawn(ctx, look, ctx.caster.position(), ctx.flatLook(), (float)ctx.area(look.size()), 0.0F, look.ticks(), look.anchored() ? ctx.caster : null);
	}

	/**
	 * Shows {@code look} at {@code at} for {@code ticks}, pointing along {@code reach} (a beam's whole length, a cone's direction times its
	 * range), {@code size} blocks across (its radius or length), {@code arg} for whatever else the look needs (a cone's arc), held by
	 * {@code holder} for anchored looks.
	 */
	public static void spawn(
		final SkillContext ctx, final SkillLook look, final Vec3 at, final Vec3 reach, final float size, final float arg, final int ticks, final @Nullable Entity holder
	) {
		spawn(ctx.level, ctx.caster, look, at, reach, size, arg, ticks, ctx.fx.color(), holder);
	}

	/**
	 * As {@link #spawn(SkillContext, SkillLook, Vec3, Vec3, float, float, int, Entity)} for a look shown by anything, not only a skill:
	 * {@code source} gives its facing (a raid boss's patterns), {@code color} its tint.
	 */
	public static void spawn(final ServerLevel level, final Entity source, final SkillLook look, final Vec3 at, final Vec3 reach, final float size, final float arg,
		final int ticks, final int color, final @Nullable Entity holder) {
		if (look == SkillLook.NONE || ticks <= 0) {
			return;
		}
		SkillVfx vfx = new SkillVfx(ModEntities.SKILL_VFX, level);
		vfx.setPos(at.x, at.y, at.z);
		vfx.setYRot(source.getYRot());
		vfx.setXRot(source.getXRot());
		vfx.entityData.set(DATA_LOOK, look.ordinal());
		vfx.entityData.set(DATA_COLOR, color);
		vfx.entityData.set(DATA_SIZE, size);
		vfx.entityData.set(DATA_TICKS, ticks);
		vfx.entityData.set(DATA_REACH, new Vector3f((float)reach.x, (float)reach.y, (float)reach.z));
		vfx.entityData.set(DATA_ARG, arg);
		vfx.entityData.set(DATA_ANCHOR, look.anchored() && holder != null ? holder.getId() : -1);
		level.addFreshEntity(vfx);
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

	/** How many ticks it is shown. */
	public int life() {
		return this.entityData.get(DATA_TICKS);
	}

	/** Where it points from its position: a beam's whole length, a cone's direction times its range. */
	public Vector3fc reach() {
		return this.entityData.get(DATA_REACH);
	}

	/** The look's own extra number (a cone's arc in degrees). */
	public float arg() {
		return this.entityData.get(DATA_ARG);
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
		entityData.define(DATA_TICKS, 20);
		entityData.define(DATA_REACH, new Vector3f(0.0F, 0.0F, 1.0F));
		entityData.define(DATA_ARG, 0.0F);
	}

	@Override
	public void tick() {
		SkillLook look = this.look();
		Entity anchor = this.anchor();
		if (this.level().isClientSide()) {
			if (look.hold() == SkillLook.Hold.TRAIL && anchor != null) {
				this.trail.add(anchor.position());
				if (this.trail.size() > TRAIL_POINTS) {
					this.trail.removeFirst();
				}
			}
			return;
		}
		// a held or following look goes with its holder (logged out, dead, gone to another dimension)
		boolean holderGone = anchor == null || anchor.isRemoved() || !anchor.isAlive();
		if (this.tickCount >= this.life() || look.hold() == SkillLook.Hold.HAND && holderGone
			|| look.hold() == SkillLook.Hold.FOLLOW && this.entityData.get(DATA_ANCHOR) >= 0 && holderGone) {
			this.discard();
			return;
		}
		if (anchor != null && !anchor.isRemoved() && (look.hold() == SkillLook.Hold.HAND || look.hold() == SkillLook.Hold.FOLLOW)) {
			// keeps it tracked (and culled) where the holder is; the client draws it at the holder itself
			this.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
		}
	}

	/** Drawn as far as it is tracked: its tiny box would hide a 30-block beam a few blocks away. */
	@Override
	public boolean shouldRenderAtSqrDistance(final double distance) {
		return distance < 128.0 * 128.0;
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
