package com.minecraftmode.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.minecraftmode.city.CityServices;
import com.minecraftmode.job.skill.CombatHooks;
import com.minecraftmode.job.skill.CombatState;
import com.minecraftmode.jump.JumpForest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** City protection: non-operators in survival cannot break blocks or use items on blocks inside the walls. */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@Inject(method = "blockActionRestricted", at = @At("HEAD"), cancellable = true)
	private void minecraftMode$cityNoBreak(final Level level, final BlockPos pos, final GameType gameType, final CallbackInfoReturnable<Boolean> cir) {
		if (CityServices.blocksBuilding((Player)(Object)this, pos)) {
			cir.setReturnValue(true);
		}
	}

	/** No elytra gliding in a jump course ({@code JumpForest}): the server refuses the start, which also stops the client's glide. */
	@Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
	private void minecraftMode$noGlideInCourse(final CallbackInfoReturnable<Boolean> cir) {
		Player self = (Player)(Object)this;
		if (self instanceof net.minecraft.server.level.ServerPlayer && !self.isCreative() && JumpForest.inCourse(self)) {
			cir.setReturnValue(false);
		}
	}

	/** Marks a fully charged swing: a class weapon's full-strength hit lands through the target's hurt cooldown ({@code CombatHooks}). */
	@Inject(method = "attack", at = @At("HEAD"))
	private void minecraftMode$swingStart(final Entity entity, final CallbackInfo ci) {
		Player self = (Player)(Object)this;
		CombatState.of(self).fullSwing = self.getAttackStrengthScale(0.5F) > 0.9F;
	}

	@Inject(method = "attack", at = @At("RETURN"))
	private void minecraftMode$swingEnd(final Entity entity, final CallbackInfo ci) {
		CombatState.of((Player)(Object)this).fullSwing = false;
	}

	/** Marks the hits of a sweep attack so the class system treats them as plain damage (no basic-hit procs). */
	@Inject(method = "doSweepAttack", at = @At("HEAD"))
	private void minecraftMode$sweepStart(final CallbackInfo ci) {
		CombatState.of((Player)(Object)this).sweep = true;
	}

	@Inject(method = "doSweepAttack", at = @At("RETURN"))
	private void minecraftMode$sweepEnd(final CallbackInfo ci) {
		CombatState.of((Player)(Object)this).sweep = false;
	}

	/** A hit that got through (not dodged, guarded, bounced or shrugged off): the mana shield pays and thorns/counter hit back, before armor. */
	@ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true)
	private float minecraftMode$landed(final float damage, @Local(argsOnly = true) final DamageSource source) {
		return CombatHooks.landed((Player)(Object)this, source, damage);
	}

	@Inject(method = "mayUseItemAt", at = @At("HEAD"), cancellable = true)
	private void minecraftMode$cityNoPlace(final BlockPos pos, final Direction direction, final ItemStack itemStack, final CallbackInfoReturnable<Boolean> cir) {
		if (CityServices.blocksBuilding((Player)(Object)this, pos)) {
			cir.setReturnValue(false);
		}
	}
}
