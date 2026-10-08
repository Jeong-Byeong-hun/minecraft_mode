package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.job.JobClass;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: advance to the next tier ({@code choice} picks the class at the first advancement). */
public record AdvanceJobPayload(JobClass choice) implements CustomPacketPayload {
	public static final Type<AdvanceJobPayload> TYPE = new Type<>(MinecraftMode.id("advance_job"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AdvanceJobPayload> CODEC = StreamCodec.composite(
		JobClass.STREAM_CODEC, AdvanceJobPayload::choice, AdvanceJobPayload::new
	);

	@Override
	public Type<AdvanceJobPayload> type() {
		return TYPE;
	}
}
