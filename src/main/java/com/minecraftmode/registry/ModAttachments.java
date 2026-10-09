package com.minecraftmode.registry;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.quest.QuestData;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModAttachments {
	/** Class, level, MP and cooldowns. Kept through death; synced to the owning player for the HUD. */
	public static final AttachmentType<JobData> JOB = AttachmentRegistry.create(
		MinecraftMode.id("job"),
		builder -> builder.persistent(JobData.CODEC)
			.copyOnDeath()
			.initializer(() -> JobData.DEFAULT)
			.syncWith(JobData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	/** Active advancement trial and its kill counts, plus the first-visit flag for the city. */
	public static final AttachmentType<QuestData> QUEST = AttachmentRegistry.create(
		MinecraftMode.id("quest"),
		builder -> builder.persistent(QuestData.CODEC)
			.copyOnDeath()
			.initializer(() -> QuestData.DEFAULT)
			.syncWith(QuestData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	/** Coins in the wallet, in copper (1 silver = 9, 1 gold = 81). Kept through death; synced for the HUD. */
	public static final AttachmentType<Integer> WALLET = AttachmentRegistry.create(
		MinecraftMode.id("wallet"),
		builder -> builder.persistent(Codec.INT)
			.copyOnDeath()
			.initializer(() -> 0)
			.syncWith(ByteBufCodecs.VAR_INT.cast(), AttachmentSyncPredicate.targetOnly())
	);

	public static void init() {
	}

	private ModAttachments() {
	}
}
