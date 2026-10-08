package com.minecraftmode.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Cannot move (attribute modifiers) and mobs drop their target every tick, so they neither chase
 * nor attack. Stunned players cannot cast skills.
 */
public class StunEffect extends MobEffect {
	public StunEffect(final MobEffectCategory category, final int color) {
		super(category, color);
	}

	@Override
	public boolean applyEffectTick(final ServerLevel level, final LivingEntity entity, final int amplification) {
		if (entity instanceof Mob mob) {
			mob.getNavigation().stop();
			mob.setTarget(null);
		}
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(final int tickCount, final int amplification) {
		return true;
	}
}
