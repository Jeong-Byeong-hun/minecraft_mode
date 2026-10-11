package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: open (or refresh) the land screen with the player's claims and the players they trust. */
public record OpenLandPayload(List<Plot> plots, List<Friend> friends, int maxPlots, int maxFriends) implements CustomPacketPayload {
	/** One claim: chunk coordinates and its lowest block. */
	public record Plot(int chunkX, int chunkZ, int minY) {
		static final StreamCodec<RegistryFriendlyByteBuf, Plot> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Plot::chunkX,
			ByteBufCodecs.VAR_INT, Plot::chunkZ,
			ByteBufCodecs.VAR_INT, Plot::minY,
			Plot::new
		);
	}

	public record Friend(UUID id, String name) {
		static final StreamCodec<RegistryFriendlyByteBuf, Friend> CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, Friend::id,
			ByteBufCodecs.STRING_UTF8, Friend::name,
			Friend::new
		);
	}

	public static final Type<OpenLandPayload> TYPE = new Type<>(MinecraftMode.id("open_land"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenLandPayload> CODEC = StreamCodec.composite(
		Plot.CODEC.apply(ByteBufCodecs.list()), OpenLandPayload::plots,
		Friend.CODEC.apply(ByteBufCodecs.list()), OpenLandPayload::friends,
		ByteBufCodecs.VAR_INT, OpenLandPayload::maxPlots,
		ByteBufCodecs.VAR_INT, OpenLandPayload::maxFriends,
		OpenLandPayload::new
	);

	@Override
	public Type<OpenLandPayload> type() {
		return TYPE;
	}
}
