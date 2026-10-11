package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: every claim in the residence with what it is to this player, for the border lines (client {@code LandBorders}).
 * Sent when the player comes into the residence and whenever claims or trust lists change.
 */
public record LandBordersPayload(List<Border> borders) implements CustomPacketPayload {
	public static final int MINE = 0;
	public static final int TRUSTED = 1;
	public static final int OTHER = 2;

	/** One claim: chunk, lowest block and {@link #MINE}, {@link #TRUSTED} or {@link #OTHER}. */
	public record Border(int chunkX, int chunkZ, int minY, int relation) {
		static final StreamCodec<RegistryFriendlyByteBuf, Border> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Border::chunkX,
			ByteBufCodecs.VAR_INT, Border::chunkZ,
			ByteBufCodecs.VAR_INT, Border::minY,
			ByteBufCodecs.VAR_INT, Border::relation,
			Border::new
		);
	}

	public static final Type<LandBordersPayload> TYPE = new Type<>(MinecraftMode.id("land_borders"));
	public static final StreamCodec<RegistryFriendlyByteBuf, LandBordersPayload> CODEC = StreamCodec.composite(
		Border.CODEC.apply(ByteBufCodecs.list()), LandBordersPayload::borders,
		LandBordersPayload::new
	);

	@Override
	public Type<LandBordersPayload> type() {
		return TYPE;
	}
}
