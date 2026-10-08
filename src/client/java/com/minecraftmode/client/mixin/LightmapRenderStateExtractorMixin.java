package com.minecraftmode.client.mixin;

import com.minecraftmode.enchantment.ArmorEnchantments;
import com.minecraftmode.enchantment.EnchantLevels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Night Sight helmets brighten the lightmap like Night Vision, 20% per level (V = full Night Vision).
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
	}
}
