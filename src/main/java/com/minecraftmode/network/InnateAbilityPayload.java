package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: the innate class ability key (B) was pressed. */
public record InnateAbilityPayload() implements CustomPacketPayload {
	public static final InnateAbilityPayload INSTANCE = new InnateAbilityPayload();
	public static final Type<InnateAbilityPayload> TYPE = new Type<>(MinecraftMode.id("innate_ability"));
	public static final StreamCodec<RegistryFriendlyByteBuf, InnateAbilityPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<InnateAbilityPayload> type() {
		return TYPE;
	}
}
