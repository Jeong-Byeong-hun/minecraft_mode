package com.minecraftmode.mixin;

import com.minecraftmode.job.skill.CombatHooks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonSittingPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A perched dragon sets arrows on fire and takes nothing from them; class arrows still land (see {@link CombatHooks#classProjectile}). */
@Mixin(AbstractDragonSittingPhase.class)
public abstract class DragonSittingPhaseMixin {
	@Inject(method = "onHurt", at = @At("HEAD"), cancellable = true)
	private void minecraftMode$classArrowsLand(final DamageSource source, final float damage, final CallbackInfoReturnable<Float> cir) {
		if (CombatHooks.classProjectile(source.getDirectEntity())) {
			cir.setReturnValue(damage);
		}
	}
}
