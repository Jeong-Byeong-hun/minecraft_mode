package com.minecraftmode.mixin;

import com.minecraftmode.job.skill.CombatHooks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Breezes (and anything tagged {@code deflects_projectiles}) send projectiles back; class projectiles go in instead (see
 * {@link CombatHooks#classProjectile}).
 */
@Mixin({Entity.class, Breeze.class})
public abstract class ProjectileDeflectionMixin {
	@Inject(method = "deflection", at = @At("HEAD"), cancellable = true)
	private void minecraftMode$classProjectilesGoIn(final Projectile projectile, final CallbackInfoReturnable<ProjectileDeflection> cir) {
		if (CombatHooks.classProjectile(projectile)) {
			cir.setReturnValue(ProjectileDeflection.NONE);
		}
	}
}
