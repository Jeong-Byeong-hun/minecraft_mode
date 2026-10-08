package com.minecraftmode.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Deals 1 damage per interval (40 ticks, halved per amplifier level).
 * Unlike poison it can kill.
 */
public class BleedingEffect extends MobEffect {
	private static final int BASE_INTERVAL = 40;

	public BleedingEffect(final MobEffectCategory category, final int color) {
		super(category, color);
	}

	@Override
	public boolean applyEffectTick(final ServerLevel level, final LivingEntity mob, final int amplification) {
		mob.hurtServer(level, mob.damageSources().generic(), 1.0F);
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(final int tickCount, final int amplification) {
		int interval = BASE_INTERVAL >> amplification;
		return interval <= 0 || tickCount % interval == 0;
	}
}
