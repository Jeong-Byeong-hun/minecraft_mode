package com.minecraftmode.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.minecraftmode.enchantment.EnchantLevels;
import com.minecraftmode.enchantment.RangedEnchantments;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Quick Draw: the bow counts as drawn 25% longer per level, so it reaches full power sooner.
 * The pull animation itself is unchanged.
 */
@Mixin(BowItem.class)
public abstract class BowItemMixin {
	@ModifyArg(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BowItem;getPowerForTime(I)F"))
	private int minecraftMode$quickDraw(final int timeHeld, @Local(argsOnly = true) final ItemStack itemStack, @Local(argsOnly = true) final Level level) {
		int quickDraw = EnchantLevels.get(level, RangedEnchantments.QUICK_DRAW, itemStack);
		return quickDraw > 0 ? Math.round(timeHeld * (1.0F + 0.25F * quickDraw)) : timeHeld;
	}
}
