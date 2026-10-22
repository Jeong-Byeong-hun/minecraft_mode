package com.minecraftmode.entity.boss;

import com.minecraftmode.entity.CreatureMob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * The Malevolent Shrine that stands while Sukuna's domain is open: scenery only (no AI, cannot be hit or targeted, never saved).
 * Sukuna places it and takes it away again; one left without its domain removes itself after {@link #LIFE} ticks.
 */
public class MalevolentShrine extends CreatureMob {
	private static final int LIFE = 400;

	public MalevolentShrine(final EntityType<? extends MalevolentShrine> type, final Level level) {
		super(type, level);
		this.setNoAi(true);
		this.setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 100.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide() && this.tickCount > LIFE) {
			this.discard();
		}
	}

	@Override
	public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	/** Scenery: it never shoves anyone (a Simple Domain may overlap its footprint). */
	@Override
	protected void pushEntities() {
	}

	@Override
	public boolean removeWhenFarAway(final double distSqr) {
		return false;
	}
}
