package com.minecraftmode.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.minecraftmode.job.skill.CombatHooks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Below half health the Wither's armor stops arrows; class arrows still land (see {@link CombatHooks#classProjectile}). */
@Mixin(WitherBoss.class)
public abstract class WitherBossMixin {
	@ModifyExpressionValue(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;isPowered()Z"))
	private boolean minecraftMode$classArrowsLand(final boolean powered, @Local(argsOnly = true) final DamageSource source) {
		return powered && !CombatHooks.classProjectile(source.getDirectEntity());
	}
}
