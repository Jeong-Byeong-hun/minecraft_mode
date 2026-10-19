package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: the damage report of a raid boss just defeated (client {@code RaidReportScreen}). Entries are best contribution
 * first; with two or more members the first one is the MVP. {@code open}: the receiver is still in the arena, so the report opens
 * at once (members who left or fell only keep it for the loot screen's report button).
 */
public record RaidReportPayload(String bossKey, String difficultyKey, int fightTicks, List<Entry> entries, boolean open) implements CustomPacketPayload {
	public static final Type<RaidReportPayload> TYPE = new Type<>(MinecraftMode.id("raid_report"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RaidReportPayload> CODEC = StreamCodec.ofMember(RaidReportPayload::write, RaidReportPayload::read);

	/**
	 * @param id the member's UUID (the client draws their figure while it can see them)
	 * @param job {@link com.minecraftmode.job.JobClass} ordinal
	 * @param damage damage dealt to the boss (its real health, toughness included)
	 * @param share share of the whole contribution (damage plus support), 0..1
	 */
	public record Entry(UUID id, String name, int job, float damage, float share) {
	}

	private void write(final RegistryFriendlyByteBuf buf) {
		buf.writeUtf(this.bossKey);
		buf.writeUtf(this.difficultyKey);
		buf.writeVarInt(this.fightTicks);
		buf.writeVarInt(this.entries.size());
		for (Entry e : this.entries) {
			buf.writeUUID(e.id());
			buf.writeUtf(e.name(), 32);
			buf.writeVarInt(e.job());
			buf.writeFloat(e.damage());
			buf.writeFloat(e.share());
		}
		buf.writeBoolean(this.open);
	}

	private static RaidReportPayload read(final RegistryFriendlyByteBuf buf) {
		String boss = buf.readUtf();
		String difficulty = buf.readUtf();
		int ticks = buf.readVarInt();
		int n = Math.min(16, buf.readVarInt());
		List<Entry> entries = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			entries.add(new Entry(buf.readUUID(), buf.readUtf(32), buf.readVarInt(), buf.readFloat(), buf.readFloat()));
		}
		return new RaidReportPayload(boss, difficulty, ticks, entries, buf.readBoolean());
	}

	@Override
	public Type<RaidReportPayload> type() {
		return TYPE;
	}

	public RaidReportPayload opening(final boolean open) {
		return new RaidReportPayload(this.bossKey, this.difficultyKey, this.fightTicks, this.entries, open);
	}
}
