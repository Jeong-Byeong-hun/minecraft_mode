package com.minecraftmode.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.minecraftmode.job.skill.CombatHooks;
import com.minecraftmode.progress.Contribution;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Class damage adjustments (passives, engravings, skill buffs, marks, stances) are applied to the
 * incoming amount before vanilla armor and blocking (see {@link CombatHooks#modifyIncoming}).
 * The health really lost to each hit is recorded for kill sharing (see {@link Contribution}).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float minecraftMode$classDamage(final float damage, @Local(argsOnly = true) final DamageSource source) {
		return CombatHooks.modifyIncoming((LivingEntity)(Object)this, source, damage);
	}

	@Inject(method = "actuallyHurt", at = @At("HEAD"))
	private void minecraftMode$healthBefore(final ServerLevel level, final DamageSource source, final float damage, final CallbackInfo ci,
		@Share("healthBefore") final LocalFloatRef before) {
		before.set(((LivingEntity)(Object)this).getHealth());
	}

	@Inject(method = "actuallyHurt", at = @At("TAIL"))
	private void minecraftMode$recordContribution(final ServerLevel level, final DamageSource source, final float damage, final CallbackInfo ci,
		@Share("healthBefore") final LocalFloatRef before) {
		LivingEntity self = (LivingEntity)(Object)this;
		Contribution.record(self, source, before.get() - self.getHealth());
	}
}
