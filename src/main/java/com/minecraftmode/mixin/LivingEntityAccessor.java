package com.minecraftmode.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the damage of the last accepted hit, which vanilla compares new hits against during the damage cooldown. */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
	@Accessor("lastHurt")
	float minecraftMode$lastHurt();
}
