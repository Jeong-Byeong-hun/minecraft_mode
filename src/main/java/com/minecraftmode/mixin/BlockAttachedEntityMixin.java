package com.minecraftmode.mixin;

import com.minecraftmode.residence.ResidenceRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Residence: item frames, paintings, leash knots, boats and minecarts on someone's land take no damage from players who may not
 * touch them there (arrows, tridents, TNT they lit). Living entities are guarded through {@code ALLOW_DAMAGE} in ResidenceRules.
 */
@Mixin({BlockAttachedEntity.class, VehicleEntity.class})
public abstract class BlockAttachedEntityMixin {
	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void minecraftMode$guardOnTheLand(final ServerLevel level, final DamageSource source, final float amount, final CallbackInfoReturnable<Boolean> cir) {
		if (ResidenceRules.blocksDamage((Entity)(Object)this, source)) {
			cir.setReturnValue(false);
		}
	}
}
