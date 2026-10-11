package com.minecraftmode.mixin;

import com.minecraftmode.residence.ResidenceRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Residence: a crafter facing into land with another owner does not craft (it would drop or insert the result there). */
@Mixin(CrafterBlock.class)
public abstract class CrafterBlockMixin {
	@Inject(method = "dispenseFrom", at = @At("HEAD"), cancellable = true)
	private void minecraftMode$stayOnTheLand(final BlockState state, final ServerLevel level, final BlockPos pos, final CallbackInfo ci) {
		if (ResidenceRules.crosses(level, pos, pos.relative(state.getValue(BlockStateProperties.ORIENTATION).front()))) {
			ci.cancel();
		}
	}
}
