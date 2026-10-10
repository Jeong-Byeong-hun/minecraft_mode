package com.minecraftmode.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.minecraftmode.job.skill.CombatHooks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A closed shulker ignores arrows; class arrows still land on its shell (see {@link CombatHooks#classProjectile}). */
@Mixin(Shulker.class)
public abstract class ShulkerMixin {
	@ModifyExpressionValue(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Shulker;isClosed()Z"))
	private boolean minecraftMode$classArrowsLand(final boolean closed, @Local(argsOnly = true) final DamageSource source) {
		return closed && !CombatHooks.classProjectile(source.getDirectEntity());
	}
}
