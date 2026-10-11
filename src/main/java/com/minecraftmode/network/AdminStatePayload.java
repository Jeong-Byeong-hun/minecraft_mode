package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * Server -> client: what the admin screen shows, sent to an operator when they open it and after each {@link AdminActionPayload}.
 * The market and the land claims come one page at a time ({@code market}/{@code land} are those pages, {@code marketPages} and
 * {@code landPages} their counts), so the payload stays small however many there are.
 */
public record AdminStatePayload(List<PlayerInfo> players, List<Instance> raids, List<Instance> dungeons, long ticksToDay, long ticksToCycle,
	List<Listing> listings, int marketPage, int marketPages, List<Plot> plots, int landPage, int landPages, int result) implements CustomPacketPayload {
	/** {@code result}: the answered action worked. */
	public static final int OK = 1;
	/** {@code result}: the answered action was refused or found nothing to do. */
	public static final int FAILED = 0;
	/** {@code result}: only a refresh. */
	public static final int NONE = -1;
	public static final Type<AdminStatePayload> TYPE = new Type<>(MinecraftMode.id("admin_state"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AdminStatePayload> CODEC = StreamCodec.ofMember(AdminStatePayload::write, AdminStatePayload::read);
	private static final int MAX_ROWS = 256;

	/**
	 * @param job {@link com.minecraftmode.job.JobClass} ordinal
	 * @param gameMode {@link net.minecraft.world.level.GameType} id
	 */
	public record PlayerInfo(UUID id, String name, int job, int tier, int level, int balance, String dimension, int gameMode, int chapter) {
	}

	/** A raid or dungeon run: {@code what} is the boss or dungeon id (dungeons add "+level"). */
	public record Instance(int id, String what, String state, String members) {
	}

	public record Listing(int id, ItemStack item, int price, String seller, long ticksLeft) {
	}

	public record Plot(UUID owner, String ownerName, int chunkX, int chunkZ, int minY, int trusted) {
	}

	private void write(final RegistryFriendlyByteBuf buf) {
		list(buf, this.players, p -> {
			buf.writeUUID(p.id());
			buf.writeUtf(p.name(), 32);
			buf.writeVarInt(p.job());
			buf.writeVarInt(p.tier());
			buf.writeVarInt(p.level());
			buf.writeVarInt(p.balance());
			buf.writeUtf(p.dimension(), 128);
			buf.writeVarInt(p.gameMode());
			buf.writeVarInt(p.chapter());
		});
		list(buf, this.raids, i -> instance(buf, i));
		list(buf, this.dungeons, i -> instance(buf, i));
		buf.writeVarLong(this.ticksToDay);
		buf.writeVarLong(this.ticksToCycle);
		list(buf, this.listings, l -> {
			buf.writeVarInt(l.id());
			ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, l.item());
			buf.writeVarInt(l.price());
			buf.writeUtf(l.seller(), 32);
			buf.writeVarLong(l.ticksLeft());
		});
		buf.writeVarInt(this.marketPage);
		buf.writeVarInt(this.marketPages);
		list(buf, this.plots, p -> {
			buf.writeUUID(p.owner());
			buf.writeUtf(p.ownerName(), 32);
			buf.writeVarInt(p.chunkX());
			buf.writeVarInt(p.chunkZ());
			buf.writeVarInt(p.minY());
			buf.writeVarInt(p.trusted());
		});
		buf.writeVarInt(this.landPage);
		buf.writeVarInt(this.landPages);
		buf.writeVarInt(this.result);
	}

	private static AdminStatePayload read(final RegistryFriendlyByteBuf buf) {
		List<PlayerInfo> players = list(buf, b -> new PlayerInfo(b.readUUID(), b.readUtf(32), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(),
			b.readUtf(128), b.readVarInt(), b.readVarInt()));
		List<Instance> raids = list(buf, AdminStatePayload::instance);
		List<Instance> dungeons = list(buf, AdminStatePayload::instance);
		long day = buf.readVarLong();
		long cycle = buf.readVarLong();
		List<Listing> listings = list(buf, b -> new Listing(b.readVarInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode(b), b.readVarInt(), b.readUtf(32), b.readVarLong()));
		int marketPage = buf.readVarInt();
		int marketPages = buf.readVarInt();
		List<Plot> plots = list(buf, b -> new Plot(b.readUUID(), b.readUtf(32), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt()));
		int landPage = buf.readVarInt();
		int landPages = buf.readVarInt();
		return new AdminStatePayload(players, raids, dungeons, day, cycle, listings, marketPage, marketPages, plots, landPage, landPages, buf.readVarInt());
	}

	private static void instance(final RegistryFriendlyByteBuf buf, final Instance i) {
		buf.writeVarInt(i.id());
		buf.writeUtf(i.what(), 64);
		buf.writeUtf(i.state(), 32);
		buf.writeUtf(i.members(), 512);
	}

	private static Instance instance(final RegistryFriendlyByteBuf buf) {
		return new Instance(buf.readVarInt(), buf.readUtf(64), buf.readUtf(32), buf.readUtf(512));
	}

	private static <T> void list(final RegistryFriendlyByteBuf buf, final List<T> list, final java.util.function.Consumer<T> each) {
		int n = Math.min(MAX_ROWS, list.size());
		buf.writeVarInt(n);
		for (int i = 0; i < n; i++) {
			each.accept(list.get(i));
		}
	}

	private static <T> List<T> list(final RegistryFriendlyByteBuf buf, final Function<RegistryFriendlyByteBuf, T> each) {
		int n = Math.min(MAX_ROWS, buf.readVarInt());
		List<T> out = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			out.add(each.apply(buf));
		}
		return out;
	}

	@Override
	public Type<AdminStatePayload> type() {
		return TYPE;
	}
}
