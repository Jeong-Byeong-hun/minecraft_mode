package com.minecraftmode.mixin;

import com.minecraftmode.city.CityServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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

	@Inject(method = "mayUseItemAt", at = @At("HEAD"), cancellable = true)
	private void minecraftMode$cityNoPlace(final BlockPos pos, final Direction direction, final ItemStack itemStack, final CallbackInfoReturnable<Boolean> cir) {
		if (CityServices.blocksBuilding((Player)(Object)this, pos)) {
			cir.setReturnValue(false);
		}
	}
}
