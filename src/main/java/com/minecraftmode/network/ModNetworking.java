package com.minecraftmode.network;

import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.skill.SkillCaster;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class ModNetworking {
	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(CastSkillPayload.TYPE, CastSkillPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(AdvanceJobPayload.TYPE, AdvanceJobPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(CastSkillPayload.TYPE, (payload, context) -> context.server().execute(
			() -> SkillCaster.tryCast(context.player(), payload.slot())
		));
		ServerPlayNetworking.registerGlobalReceiver(AdvanceJobPayload.TYPE, (payload, context) -> context.server().execute(() -> {
			JobProgression.AdvanceResult result = JobProgression.advance(context.player(), payload.choice());
			if (result != JobProgression.AdvanceResult.OK) {
				context.player().sendOverlayMessage(
					Component.translatable("message.minecraft_mode.job.advance_" + result.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.RED)
				);
			}
		}));
	}

	private ModNetworking() {
	}
}
