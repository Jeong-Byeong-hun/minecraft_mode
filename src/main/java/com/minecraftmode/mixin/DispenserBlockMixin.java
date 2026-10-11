package com.minecraftmode.mixin;

import com.minecraftmode.residence.ResidenceRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Residence: dispensers and droppers facing into land with another owner do nothing (no water, fire, blocks, arrows or items). */
@Mixin({DispenserBlock.class, DropperBlock.class})
public abstract class DispenserBlockMixin {
	@Inject(method = "dispenseFrom", at = @At("HEAD"), cancellable = true)
	private void minecraftMode$stayOnTheLand(final ServerLevel level, final BlockState state, final BlockPos pos, final CallbackInfo ci) {
		if (ResidenceRules.crosses(level, pos, pos.relative(state.getValue(DispenserBlock.FACING)))) {
			ci.cancel();
		}
	}
}
