package com.minecraftmode.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.minecraftmode.job.skill.CombatHooks;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.monster.Enderman;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Endermen teleport away from every projectile; class projectiles hit them instead (see {@link CombatHooks#classProjectile}). */
@Mixin(Enderman.class)
public abstract class EndermanMixin {
	@WrapOperation(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"))
	private boolean minecraftMode$classProjectilesLand(final DamageSource source, final TagKey<DamageType> tag, final Operation<Boolean> original) {
		if (tag == DamageTypeTags.IS_PROJECTILE && CombatHooks.classProjectile(source.getDirectEntity())) {
			return false;
		}
		return original.call(source, tag);
	}
}
