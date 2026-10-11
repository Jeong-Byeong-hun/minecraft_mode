package com.minecraftmode.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.minecraftmode.residence.ResidenceRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Residence: lava does not set fire to blocks on land with another owner. */
@Mixin(LavaFluid.class)
public abstract class LavaFluidMixin {
	@WrapOperation(method = "randomTick", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean minecraftMode$igniteOnlyHere(final ServerLevel level, final BlockPos target, final BlockState state, final Operation<Boolean> original,
		@Local(argsOnly = true) final BlockPos pos) {
		return !ResidenceRules.crosses(level, pos, target) && original.call(level, target, state);
	}
}
