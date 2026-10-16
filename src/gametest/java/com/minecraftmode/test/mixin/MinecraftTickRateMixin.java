package com.minecraftmode.test.mixin;

import com.minecraftmode.test.TickRateCap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The client never ticks faster than 20 a second ({@code Minecraft.getTickTargetMillis} takes the longer of 50 ms and the server's tick),
 * and the client game test API ticks client and server together: a test that raises the tick rate stays at real time. While
 * {@link TickRateCap#lifted} the client follows the server's rate instead.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftTickRateMixin {
	@Inject(method = "getTickTargetMillis", at = @At("RETURN"), cancellable = true)
	private void minecraftModeTest$followServer(final float defaultMs, final CallbackInfoReturnable<Float> cir) {
		ClientLevel level = ((Minecraft)(Object)this).level;
		if (TickRateCap.lifted && level != null && level.tickRateManager().runsNormally()) {
			cir.setReturnValue(level.tickRateManager().millisecondsPerTick());
		}
	}
}
