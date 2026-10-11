package com.minecraftmode.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.minecraftmode.residence.ResidenceRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Residence: fire neither burns away nor spreads to blocks on land with another owner. */
@Mixin(FireBlock.class)
public abstract class FireBlockMixin {
	@WrapOperation(method = "tick", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/block/FireBlock;checkBurnOut(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;I)V"))
	private void minecraftMode$burnOnlyHere(final FireBlock fire, final Level level, final BlockPos target, final int chance, final RandomSource random, final int age,
		final Operation<Void> original, @Local(argsOnly = true) final BlockPos pos) {
		if (!ResidenceRules.crosses(level, pos, target)) {
			original.call(fire, level, target, chance, random, age);
		}
	}

	@WrapOperation(method = "tick", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean minecraftMode$spreadOnlyHere(final ServerLevel level, final BlockPos target, final BlockState state, final Operation<Boolean> original,
		@Local(argsOnly = true) final BlockPos pos) {
		return !ResidenceRules.crosses(level, pos, target) && original.call(level, target, state);
	}
}
