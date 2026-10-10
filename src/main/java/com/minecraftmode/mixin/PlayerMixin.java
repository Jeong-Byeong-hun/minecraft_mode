package com.minecraftmode.mixin;

import com.minecraftmode.city.CityServices;
import com.minecraftmode.job.skill.CombatState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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

	/** Marks the hits of a sweep attack so the class system treats them as plain damage (no basic-hit procs). */
	@Inject(method = "doSweepAttack", at = @At("HEAD"))
	private void minecraftMode$sweepStart(final CallbackInfo ci) {
		CombatState.of((Player)(Object)this).sweep = true;
	}

	@Inject(method = "doSweepAttack", at = @At("RETURN"))
	private void minecraftMode$sweepEnd(final CallbackInfo ci) {
		CombatState.of((Player)(Object)this).sweep = false;
	}

	@Inject(method = "mayUseItemAt", at = @At("HEAD"), cancellable = true)
	private void minecraftMode$cityNoPlace(final BlockPos pos, final Direction direction, final ItemStack itemStack, final CallbackInfoReturnable<Boolean> cir) {
		if (CityServices.blocksBuilding((Player)(Object)this, pos)) {
			cir.setReturnValue(false);
		}
	}
}
