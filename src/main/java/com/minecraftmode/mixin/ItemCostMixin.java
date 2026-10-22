package com.minecraftmode.mixin;

import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bound items ({@code ModDataComponents.BOUND}) never pay for a trade, so no shop or villager buys them. Sukuna's fingers are the
 * exception: bound so nobody trades them, they are what the relic exchange takes (and no shop pays for them).
 */
@Mixin(ItemCost.class)
public abstract class ItemCostMixin {
	@Inject(method = "test", at = @At("HEAD"), cancellable = true)
	private void minecraftMode$refuseBound(final ItemStack stack, final CallbackInfoReturnable<Boolean> cir) {
		if (stack.has(ModDataComponents.BOUND) && !stack.is(ModItems.SUKUNA_FINGER)) {
			cir.setReturnValue(false);
		}
	}
}
