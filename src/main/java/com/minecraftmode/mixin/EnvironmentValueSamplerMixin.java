package com.minecraftmode.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla race in 26.3: {@code getDimensionValue} checks the tick cache and reads it a second time to return it, while the server
 * thread clears it every tick ({@code invalidateTickCache}). A chunk-generation worker asking for the moon phase (spawn difficulty in
 * {@code ServerLevel.getMoonBrightness}) could read null in between; that chunk then never finished generating, and the homestead pass
 * waited for it forever (0.5.32). A null result is computed again instead.
 */
@Mixin(targets = "net.minecraft.world.attribute.EnvironmentAttributeSystem$ValueSampler")
public abstract class EnvironmentValueSamplerMixin {
	@Shadow
	private Object computeValueNotPositional() {
		throw new AssertionError();
	}

	@ModifyReturnValue(method = "getDimensionValue", at = @At("RETURN"))
	private Object minecraftMode$neverNull(final Object value) {
		return value != null ? value : this.computeValueNotPositional();
	}
}
