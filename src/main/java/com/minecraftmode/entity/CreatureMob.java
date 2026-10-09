package com.minecraftmode.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Base of the mod's custom-modelled monsters (named monsters and raid bosses): synced one-shot
 * animations for the generic creature model and the {@link EliteMob} marker.
 */
public abstract class CreatureMob extends Monster implements EliteMob {
	/** Low 8 bits: {@link CreatureAnim} ordinal; higher bits: a counter so the same animation can replay. */
	private static final EntityDataAccessor<Integer> DATA_ANIM = SynchedEntityData.defineId(CreatureMob.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(CreatureMob.class, EntityDataSerializers.INT);

	private int animCounter;
	/** Client: tickCount when the current animation arrived. Server: when it started. */
	private int animStart;

	protected CreatureMob(final EntityType<? extends CreatureMob> type, final Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_ANIM, 0);
		entityData.define(DATA_PHASE, 0);
	}

	public void playAnim(final CreatureAnim anim) {
		this.animCounter = (this.animCounter + 1) & 0xFFFF;
		this.entityData.set(DATA_ANIM, anim.ordinal() | this.animCounter << 8);
		this.animStart = this.tickCount;
	}

	@Override
	public void onSyncedDataUpdated(final EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (DATA_ANIM.equals(key)) {
			this.animStart = this.tickCount;
		}
	}

	public CreatureAnim anim() {
		CreatureAnim anim = CreatureAnim.byId(this.entityData.get(DATA_ANIM) & 0xFF);
		return this.tickCount - this.animStart > anim.ticks() ? CreatureAnim.NONE : anim;
	}

	/** Ticks since the current animation started (0 when none). */
	public float animTime(final float partialTicks) {
		return this.anim() == CreatureAnim.NONE ? 0.0F : this.tickCount - this.animStart + partialTicks;
	}

	public int phase() {
		return this.entityData.get(DATA_PHASE);
	}

	protected void setPhase(final int phase) {
		this.entityData.set(DATA_PHASE, phase);
	}

	/** Flying creatures flap and do not swing their legs. */
	public boolean isFlyingCreature() {
		return false;
	}
}
