package com.minecraftmode.client.mixin;

import com.minecraftmode.bag.BagMenu;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bags open with the cursor where the last screen left it instead of the window centre: vanilla centres the cursor whenever a screen
 * opens from the game, and bags are opened over and over.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;
	@Shadow
	private boolean mouseGrabbed;
	@Shadow
	private double xpos;
	@Shadow
	private double ypos;
	@Unique
	private double minecraft_mode$lastX = -1;
	@Unique
	private double minecraft_mode$lastY = -1;
	@Unique
	private boolean minecraft_mode$wasGrabbed;

	@Inject(method = "grabMouse", at = @At("HEAD"))
	private void minecraft_mode$remember(final CallbackInfo ci) {
		if (!this.mouseGrabbed) {
			this.minecraft_mode$lastX = this.xpos;
			this.minecraft_mode$lastY = this.ypos;
		}
	}

	@Inject(method = "releaseMouse", at = @At("HEAD"))
	private void minecraft_mode$grabbed(final CallbackInfo ci) {
		this.minecraft_mode$wasGrabbed = this.mouseGrabbed;
	}

	@Inject(method = "releaseMouse", at = @At("TAIL"))
	private void minecraft_mode$restore(final CallbackInfo ci) {
		if (!this.minecraft_mode$wasGrabbed || this.minecraft_mode$lastX < 0 || !this.minecraft_mode$bagScreen()) {
			return;
		}
		this.xpos = this.minecraft_mode$lastX;
		this.ypos = this.minecraft_mode$lastY;
		InputConstants.releaseMouse(this.minecraft.getWindow(), this.xpos, this.ypos);
	}

	/** The screen being opened shows a bag ({@link BagMenu#isBagTitle}). */
	@Unique
	private boolean minecraft_mode$bagScreen() {
		return this.minecraft.gui.screen() instanceof ContainerScreen screen && BagMenu.isBagTitle(screen.getTitle());
	}
}
