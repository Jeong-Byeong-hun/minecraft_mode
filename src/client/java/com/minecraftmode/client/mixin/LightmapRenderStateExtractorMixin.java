package com.minecraftmode.client.mixin;

import com.minecraftmode.client.domain.DomainClient;
import com.minecraftmode.domain.DomainStyle;
import com.minecraftmode.enchantment.ArmorEnchantments;
import com.minecraftmode.enchantment.EnchantLevels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Night Sight helmets brighten the lightmap like Night Vision, 20% per level (V = full Night Vision). A domain the player stands in
 * darkens and tints it ({@link DomainClient}).
 */
@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapRenderStateExtractorMixin {
	@Inject(method = "extract", at = @At("RETURN"))
	private void minecraftMode$nightSight(final LightmapRenderState renderState, final float partialTicks, final CallbackInfo ci) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		int nightSight = EnchantLevels.get(player.level(), ArmorEnchantments.NIGHT_SIGHT, player.getItemBySlot(EquipmentSlot.HEAD));
		if (nightSight > 0) {
			renderState.nightVisionEffectIntensity = Math.max(renderState.nightVisionEffectIntensity, Math.min(1.0F, nightSight / 5.0F));
		}
		// inside a domain the world darkens and takes its light (Sukuna's: a blood-red dusk)
		float domain = DomainClient.strength(partialTicks);
		if (domain > 0.0F) {
			DomainStyle style = DomainClient.style();
			Vector3f light = new Vector3f(((style.light >> 16) & 0xFF) / 255.0F, ((style.light >> 8) & 0xFF) / 255.0F, (style.light & 0xFF) / 255.0F);
			renderState.bossOverlayWorldDarkening = Math.max(renderState.bossOverlayWorldDarkening, style.darkening * domain);
			renderState.skyLightColor = new Vector3f(renderState.skyLightColor).lerp(light, 0.7F * domain);
			renderState.ambientColor = new Vector3f(renderState.ambientColor).lerp(light, 0.65F * domain);
			renderState.needsUpdate = true;
		}
	}
}
