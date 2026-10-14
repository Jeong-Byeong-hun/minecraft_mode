package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * Server -> client: what the loot buyer {@code entityId} would buy from this player (inventory first, then the bags), in the order
 * {@link LootSellPayload} picks from.
 */
public record OpenLootBuyerPayload(int entityId, List<Entry> entries) implements CustomPacketPayload {
	public static final Type<OpenLootBuyerPayload> TYPE = new Type<>(MinecraftMode.id("open_loot_buyer"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenLootBuyerPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, OpenLootBuyerPayload::entityId,
		Entry.STREAM_CODEC.apply(ByteBufCodecs.list(256)), OpenLootBuyerPayload::entries,
		OpenLootBuyerPayload::new
	);

	/**
	 * @param price copper paid for it
	 * @param mine gear of the player's own class (accessories count as anyone's)
	 * @param kept on the hotbar, enhanced or engraved: "select all" leaves it
	 */
	public record Entry(ItemStack stack, int price, boolean mine, boolean kept) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC, Entry::stack,
			ByteBufCodecs.VAR_INT, Entry::price,
			ByteBufCodecs.BOOL, Entry::mine,
			ByteBufCodecs.BOOL, Entry::kept,
			Entry::new
		);
	}

	@Override
	public Type<OpenLootBuyerPayload> type() {
		return TYPE;
	}
}
