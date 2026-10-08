package com.minecraftmode.mixin;

import com.minecraftmode.city.CityServices;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** City protection: no block placing inside the walls (see {@link CityServices#blocksBuilding}). */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	@Inject(method = "place", at = @At("HEAD"), cancellable = true)
	private void minecraftMode$cityNoPlace(final BlockPlaceContext context, final CallbackInfoReturnable<InteractionResult> cir) {
		Player player = context.getPlayer();
		if (player != null && CityServices.blocksBuilding(player, context.getClickedPos())) {
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}
}
