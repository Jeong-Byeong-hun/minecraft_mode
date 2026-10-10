package com.minecraftmode.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.minecraftmode.client.hud.TooltipLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Tooltips taller than the screen start at its top and scroll with the wheel ({@link TooltipLayout}). */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {
	@WrapOperation(method = "tooltip", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;positionTooltip(IIIIII)Lorg/joml/Vector2ic;"))
	private Vector2ic minecraftMode$keepOnScreen(final ClientTooltipPositioner positioner, final int screenWidth, final int screenHeight, final int x, final int y,
		final int width, final int height, final Operation<Vector2ic> original) {
		Vector2ic placed = original.call(positioner, screenWidth, screenHeight, x, y, width, height);
		int top = TooltipLayout.place(screenHeight, placed.y(), width, height);
		return top == placed.y() ? placed : new Vector2i(placed.x(), top);
	}

	@WrapOperation(method = "tooltip", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;extractTooltipBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIILnet/minecraft/resources/Identifier;)V"))
	private void minecraftMode$scrollBar(final GuiGraphicsExtractor graphics, final int x, final int y, final int width, final int height, final @Nullable Identifier style,
		final Operation<Void> original) {
		original.call(graphics, x, y, width, height, style);
		TooltipLayout.scrollBar(graphics, x, width, height);
	}
}
