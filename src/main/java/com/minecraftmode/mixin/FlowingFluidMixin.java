package com.minecraftmode.mixin;

import com.minecraftmode.residence.ResidenceRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Residence: water and lava never flow into land with another owner (every sideways and downward spread asks this first). */
@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {
	@Inject(method = "canMaybePassThrough", at = @At("HEAD"), cancellable = true)
	private void minecraftMode$stayOnTheLand(final BlockGetter getter, final BlockPos from, final BlockState fromState, final Direction direction, final BlockPos to,
		final BlockState toState, final FluidState fluid, final CallbackInfoReturnable<Boolean> cir) {
		if (getter instanceof Level level && ResidenceRules.crosses(level, from, to)) {
			cir.setReturnValue(false);
		}
	}
}
