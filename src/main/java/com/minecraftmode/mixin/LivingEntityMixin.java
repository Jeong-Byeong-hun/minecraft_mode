package com.minecraftmode.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.minecraftmode.job.skill.CombatHooks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Class damage adjustments (passives, engravings, skill buffs, marks, stances) are applied to the
 * incoming amount before vanilla armor and blocking (see {@link CombatHooks#modifyIncoming}).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float minecraftMode$classDamage(final float damage, @Local(argsOnly = true) final DamageSource source) {
		return CombatHooks.modifyIncoming((LivingEntity)(Object)this, source, damage);
	}
}
