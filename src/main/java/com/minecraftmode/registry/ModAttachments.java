package com.minecraftmode.registry;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.job.JobData;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public final class ModAttachments {
	/** Class, level, MP and cooldowns. Kept through death; synced to the owning player for the HUD. */
	public static final AttachmentType<JobData> JOB = AttachmentRegistry.create(
		MinecraftMode.id("job"),
		builder -> builder.persistent(JobData.CODEC)
			.copyOnDeath()
			.initializer(() -> JobData.DEFAULT)
			.syncWith(JobData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	public static void init() {
	}

	private ModAttachments() {
	}
}
