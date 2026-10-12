package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * Server -> client: the account's character slots ({@code character/Characters}), one entry per owned slot. Sent on join and after
 * every switch, purchase or request; {@code prompt} is true while the player is choosing (the select screen stays open until it
 * turns false).
 */
public record CharacterListPayload(List<Entry> entries, int active, int maxSlots, int price, boolean prompt) implements CustomPacketPayload {
	private static final int MAX_GEAR = 8;
	public static final Type<CharacterListPayload> TYPE = new Type<>(MinecraftMode.id("character_list"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CharacterListPayload> CODEC = StreamCodec.ofMember(CharacterListPayload::write, CharacterListPayload::read);

	/**
	 * @param job {@link com.minecraftmode.job.JobClass} ordinal
	 * @param gear what the character wears and holds, in {@code Characters.GEAR_SLOTS} order (empty for an empty slot)
	 */
	public record Entry(boolean empty, int job, int tier, int level, List<ItemStack> gear) {
		public static final Entry EMPTY = new Entry(true, 0, 0, 0, List.of());
	}

	private void write(final RegistryFriendlyByteBuf buf) {
		buf.writeVarInt(this.entries.size());
		for (Entry e : this.entries) {
			buf.writeBoolean(e.empty());
			buf.writeVarInt(e.job());
			buf.writeVarInt(e.tier());
			buf.writeVarInt(e.level());
			buf.writeVarInt(e.gear().size());
			for (ItemStack stack : e.gear()) {
				ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
			}
		}
		buf.writeVarInt(this.active);
		buf.writeVarInt(this.maxSlots);
		buf.writeVarInt(this.price);
		buf.writeBoolean(this.prompt);
	}

	private static CharacterListPayload read(final RegistryFriendlyByteBuf buf) {
		int size = Math.min(buf.readVarInt(), 64);
		List<Entry> entries = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			boolean empty = buf.readBoolean();
			int job = buf.readVarInt();
			int tier = buf.readVarInt();
			int level = buf.readVarInt();
			int count = Math.min(buf.readVarInt(), MAX_GEAR);
			List<ItemStack> gear = new ArrayList<>();
			for (int g = 0; g < count; g++) {
				gear.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
			}
			entries.add(new Entry(empty, job, tier, level, List.copyOf(gear)));
		}
		return new CharacterListPayload(List.copyOf(entries), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
	}

	@Override
	public Type<CharacterListPayload> type() {
		return TYPE;
	}
}
