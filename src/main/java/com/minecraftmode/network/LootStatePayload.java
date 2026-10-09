package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * Server -> client: the state of a raid loot session for one participant (sent on every change and
 * once a second). {@code open} asks the client to open the loot screen. A session with no lots
 * closes the screen.
 */
public record LootStatePayload(int session, String bossKey, boolean leader, boolean open, int current, List<Lot> lots) implements CustomPacketPayload {
	public static final Type<LootStatePayload> TYPE = new Type<>(MinecraftMode.id("loot_state"));
	public static final StreamCodec<RegistryFriendlyByteBuf, LootStatePayload> CODEC = StreamCodec.ofMember(LootStatePayload::write, LootStatePayload::read);

	/**
	 * @param mode 0 auction, 1 dice
	 * @param state 0 waiting, 1 running, 2 done
	 * @param bid highest bid (copper), 0 = none yet
	 * @param myRoll this player's roll (0 = not rolled, -1 = passed)
	 * @param eligible this player may act on the lot (dice re-rolls are only for the tied players)
	 * @param rolls "name: roll" lines of the dice round
	 */
	public record Lot(ItemStack stack, int mode, int state, int start, int step, int bid, String bidder, int ticksLeft, String winner, int price, int myRoll,
		boolean eligible, List<String> rolls) {
	}

	private void write(final RegistryFriendlyByteBuf buf) {
		buf.writeVarInt(this.session);
		buf.writeUtf(this.bossKey, 128);
		buf.writeBoolean(this.leader);
		buf.writeBoolean(this.open);
		buf.writeVarInt(this.current);
		buf.writeVarInt(this.lots.size());
		for (Lot lot : this.lots) {
			ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, lot.stack());
			buf.writeVarInt(lot.mode());
			buf.writeVarInt(lot.state());
			buf.writeVarInt(lot.start());
			buf.writeVarInt(lot.step());
			buf.writeVarInt(lot.bid());
			buf.writeUtf(lot.bidder(), 32);
			buf.writeVarInt(lot.ticksLeft());
			buf.writeUtf(lot.winner(), 32);
			buf.writeVarInt(lot.price());
			buf.writeVarInt(lot.myRoll());
			buf.writeBoolean(lot.eligible());
			buf.writeVarInt(lot.rolls().size());
			for (String r : lot.rolls()) {
				buf.writeUtf(r, 64);
			}
		}
	}

	private static LootStatePayload read(final RegistryFriendlyByteBuf buf) {
		int session = buf.readVarInt();
		String bossKey = buf.readUtf(128);
		boolean leader = buf.readBoolean();
		boolean open = buf.readBoolean();
		int current = buf.readVarInt();
		int n = Math.min(32, buf.readVarInt());
		List<Lot> lots = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
			int mode = buf.readVarInt();
			int state = buf.readVarInt();
			int start = buf.readVarInt();
			int step = buf.readVarInt();
			int bid = buf.readVarInt();
			String bidder = buf.readUtf(32);
			int ticksLeft = buf.readVarInt();
			String winner = buf.readUtf(32);
			int price = buf.readVarInt();
			int myRoll = buf.readVarInt();
			boolean eligible = buf.readBoolean();
			int r = Math.min(16, buf.readVarInt());
			List<String> rolls = new ArrayList<>(r);
			for (int k = 0; k < r; k++) {
				rolls.add(buf.readUtf(64));
			}
			lots.add(new Lot(stack, mode, state, start, step, bid, bidder, ticksLeft, winner, price, myRoll, eligible, rolls));
		}
		return new LootStatePayload(session, bossKey, leader, open, current, lots);
	}

	@Override
	public Type<LootStatePayload> type() {
		return TYPE;
	}
}
