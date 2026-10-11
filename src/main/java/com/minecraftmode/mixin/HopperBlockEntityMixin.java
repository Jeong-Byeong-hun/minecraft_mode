package com.minecraftmode.mixin;

import com.minecraftmode.residence.ResidenceRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Residence: a hopper never pushes into a container on land with another owner (checked before Fabric's transfer API gets a
 * chance to find the container another way). It pulls from the block above, which is in the same chunk column and so on the
 * same owner's land or on land nobody can build on.
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
	@Inject(method = "ejectItems", at = @At("HEAD"), cancellable = true)
	private static void minecraftMode$stayOnTheLand(final Level level, final BlockPos pos, final HopperBlockEntity hopper, final CallbackInfoReturnable<Boolean> cir) {
		if (ResidenceRules.crosses(level, pos, pos.relative(hopper.getBlockState().getValue(HopperBlock.FACING)))) {
			cir.setReturnValue(false);
		}
	}
}
