package com.minecraftmode.network;

import com.minecraftmode.entity.ClassTrainer;
import com.minecraftmode.job.quest.QuestService;
import com.minecraftmode.job.skill.SkillCaster;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class ModNetworking {
	/** Trainer actions must be done within this distance of the trainer. */
	private static final double TRAINER_RANGE = 8.0;

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(CastSkillPayload.TYPE, CastSkillPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(QuestActionPayload.TYPE, QuestActionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenTrainerPayload.TYPE, OpenTrainerPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(CastSkillPayload.TYPE, (payload, context) -> context.server().execute(
			() -> SkillCaster.tryCast(context.player(), payload.slot())
		));
		ServerPlayNetworking.registerGlobalReceiver(QuestActionPayload.TYPE, (payload, context) -> context.server().execute(
			() -> handleQuest(context.player(), payload)
		));
	}

	private static void handleQuest(final ServerPlayer player, final QuestActionPayload payload) {
		if (payload.action() == QuestActionPayload.Action.ABANDON) {
			QuestService.abandon(player);
			return;
		}
		if (!(player.level().getEntity(payload.entityId()) instanceof ClassTrainer trainer) || trainer.distanceTo(player) > TRAINER_RANGE) {
			return;
		}
		switch (payload.action()) {
			case ACCEPT -> QuestService.accept(player, trainer.job());
			case COMPLETE -> QuestService.complete(player, trainer.job());
			default -> {
			}
		}
	}

	private ModNetworking() {
	}
}
