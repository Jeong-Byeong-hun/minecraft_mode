package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: sell these entries (indices into the last {@link OpenLootBuyerPayload}) to the loot buyer {@code entityId}. */
public record LootSellPayload(int entityId, List<Integer> picks) implements CustomPacketPayload {
	public static final Type<LootSellPayload> TYPE = new Type<>(MinecraftMode.id("loot_sell"));
	public static final StreamCodec<RegistryFriendlyByteBuf, LootSellPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, LootSellPayload::entityId,
		ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(256)), LootSellPayload::picks,
		LootSellPayload::new
	);

	@Override
	public Type<LootSellPayload> type() {
		return TYPE;
	}
}
