package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.job.JobClass;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: open the dialog of the trainer entity {@code entityId}. */
public record OpenTrainerPayload(int entityId, JobClass job) implements CustomPacketPayload {
	public static final Type<OpenTrainerPayload> TYPE = new Type<>(MinecraftMode.id("open_trainer"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenTrainerPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, OpenTrainerPayload::entityId, JobClass.STREAM_CODEC, OpenTrainerPayload::job, OpenTrainerPayload::new
	);

	@Override
	public Type<OpenTrainerPayload> type() {
		return TYPE;
	}
}
