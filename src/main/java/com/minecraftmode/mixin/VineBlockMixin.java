package com.minecraftmode.mixin;

import com.minecraftmode.jump.JumpForest;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The climbing vines of a jump course never spread: a vine growing onto a foothold's side would be a ladder past the jumps. */
@Mixin(VineBlock.class)
public abstract class VineBlockMixin {
	@Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
	private void minecraftMode$stillInCourse(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random, final CallbackInfo ci) {
		if (JumpForest.courseArea(level, pos)) {
			ci.cancel();
		}
	}
}
