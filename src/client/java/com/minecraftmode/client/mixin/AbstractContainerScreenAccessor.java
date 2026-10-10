package com.minecraftmode.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Where a container screen's panel is (it moves when the recipe book opens), for the inventory's trash button. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("leftPos")
	int minecraftMode$leftPos();

	@Accessor("topPos")
	int minecraftMode$topPos();

	@Accessor("imageWidth")
	int minecraftMode$imageWidth();
}
