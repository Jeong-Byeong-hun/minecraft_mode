package com.minecraftmode.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the damage of the last accepted hit, which vanilla compares new hits against during the damage cooldown, and sets how far a
 * player's swing has charged (tests swing at full strength without waiting).
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
	@Accessor("lastHurt")
	float minecraftMode$lastHurt();

	@Accessor("attackStrengthTicker")
	void minecraftMode$setAttackStrengthTicker(int ticks);
}
