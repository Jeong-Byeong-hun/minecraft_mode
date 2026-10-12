package com.minecraftmode.mixin;

import com.minecraftmode.registry.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bound items ({@code ModDataComponents.BOUND}) never pay for a trade, so no shop or villager buys them. */
@Mixin(ItemCost.class)
public abstract class ItemCostMixin {
	@Inject(method = "test", at = @At("HEAD"), cancellable = true)
	private void minecraftMode$refuseBound(final ItemStack stack, final CallbackInfoReturnable<Boolean> cir) {
		if (stack.has(ModDataComponents.BOUND)) {
			cir.setReturnValue(false);
		}
	}
}
