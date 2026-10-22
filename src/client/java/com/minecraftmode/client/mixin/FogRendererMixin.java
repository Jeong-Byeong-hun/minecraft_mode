package com.minecraftmode.client.mixin;

import com.minecraftmode.client.domain.DomainClient;
import com.minecraftmode.domain.DomainStyle;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.util.Mth;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Inside a domain the fog closes in and the sky turns to the domain's color ({@link DomainClient}). */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
	@Inject(method = "setupFog", at = @At("RETURN"))
	private void minecraftMode$domainFog(final Camera camera, final int renderDistance, final DeltaTracker delta, final float darken, final ClientLevel level,
		final CallbackInfoReturnable<FogData> cir) {
		float s = DomainClient.strength(delta.getGameTimeDeltaPartialTick(false));
		if (s <= 0.0F) {
			return;
		}
		FogData fog = cir.getReturnValue();
		DomainStyle style = DomainClient.style();
		fog.color.lerp(new Vector4f(((style.fog >> 16) & 0xFF) / 255.0F, ((style.fog >> 8) & 0xFF) / 255.0F, (style.fog & 0xFF) / 255.0F, 1.0F), s);
		fog.environmentalStart = Mth.lerp(s, fog.environmentalStart, 2.0F);
		fog.environmentalEnd = Mth.lerp(s, fog.environmentalEnd, Math.min(fog.environmentalEnd, style.fogEnd));
		fog.renderDistanceStart = Mth.lerp(s, fog.renderDistanceStart, 2.0F);
		fog.renderDistanceEnd = Mth.lerp(s, fog.renderDistanceEnd, Math.min(fog.renderDistanceEnd, style.fogEnd));
		fog.skyEnd = Mth.lerp(s, fog.skyEnd, style.fogEnd * 0.5F);
		fog.cloudEnd = Mth.lerp(s, fog.cloudEnd, style.fogEnd);
	}
}
