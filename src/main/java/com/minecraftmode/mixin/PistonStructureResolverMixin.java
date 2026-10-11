package com.minecraftmode.mixin;

import com.minecraftmode.residence.ResidenceRules;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Residence: a piston only moves when its head, every block it pushes or pulls (and where that block ends up) and every block it
 * breaks stay on its owner's land; otherwise it does not fire at all.
 */
@Mixin(PistonStructureResolver.class)
public abstract class PistonStructureResolverMixin {
	@Shadow @Final private Level level;
	@Shadow @Final private BlockPos pistonPos;
	@Shadow @Final private boolean extending;
	@Shadow @Final private Direction pushDirection;
	@Shadow @Final private Direction pistonDirection;
	@Shadow @Final private List<BlockPos> toPush;
	@Shadow @Final private List<BlockPos> toDestroy;

	@Inject(method = "resolve", at = @At("RETURN"), cancellable = true)
	private void minecraftMode$stayOnTheLand(final CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && this.leavesTheLand()) {
			cir.setReturnValue(false);
		}
	}

	private boolean leavesTheLand() {
		if (this.extending && ResidenceRules.crosses(this.level, this.pistonPos, this.pistonPos.relative(this.pistonDirection))) {
			return true;
		}
		for (BlockPos pos : this.toPush) {
			if (ResidenceRules.crosses(this.level, this.pistonPos, pos) || ResidenceRules.crosses(this.level, this.pistonPos, pos.relative(this.pushDirection))) {
				return true;
			}
		}
		for (BlockPos pos : this.toDestroy) {
			if (ResidenceRules.crosses(this.level, this.pistonPos, pos)) {
				return true;
			}
		}
		return false;
	}
}
