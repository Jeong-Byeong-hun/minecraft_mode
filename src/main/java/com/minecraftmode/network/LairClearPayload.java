package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: the player just cleared {@code lair} (a lair id) for this cycle; the client offers the way back to its gate
 * ({@code LairClearScreen}, answered with {@link LairTravelPayload#EXIT}) once no other screen is open. {@code nextMap}: a lair map was handed out.
 */
public record LairClearPayload(String lair, boolean nextMap) implements CustomPacketPayload {
	public static final Type<LairClearPayload> TYPE = new Type<>(MinecraftMode.id("lair_clear"));
	public static final StreamCodec<RegistryFriendlyByteBuf, LairClearPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, LairClearPayload::lair,
		ByteBufCodecs.BOOL, LairClearPayload::nextMap,
		LairClearPayload::new
	);

	@Override
	public Type<LairClearPayload> type() {
		return TYPE;
	}
}
