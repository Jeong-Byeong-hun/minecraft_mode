package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server: wear a title ({@link #TITLE}, value = achievement id or ""), spend a talent point ({@link #TALENT}, value =
 * node id) or reset the talents ({@link #TALENT_RESET}).
 */
public record ProgressActionPayload(int action, String value) implements CustomPacketPayload {
	public static final int TITLE = 0;
	public static final int TALENT = 1;
	public static final int TALENT_RESET = 2;
	public static final Type<ProgressActionPayload> TYPE = new Type<>(MinecraftMode.id("progress_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ProgressActionPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, ProgressActionPayload::action,
		ByteBufCodecs.stringUtf8(96), ProgressActionPayload::value,
		ProgressActionPayload::new
	);

	@Override
	public Type<ProgressActionPayload> type() {
		return TYPE;
	}
}
