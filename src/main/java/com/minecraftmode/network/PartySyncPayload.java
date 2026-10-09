package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: the receiver's party (empty = no party), sent twice a second for the party HUD
 * and the raid screen. The first member is the leader.
 */
public record PartySyncPayload(List<Member> members) implements CustomPacketPayload {
	public static final Type<PartySyncPayload> TYPE = new Type<>(MinecraftMode.id("party_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PartySyncPayload> CODEC = StreamCodec.ofMember(PartySyncPayload::write, PartySyncPayload::read);

	/**
	 * @param job {@link com.minecraftmode.job.JobClass} ordinal
	 * @param inRaid in a raid instance right now
	 */
	public record Member(UUID id, String name, float health, float maxHealth, int job, int tier, int level, boolean online, boolean inRaid) {
	}

	private void write(final RegistryFriendlyByteBuf buf) {
		buf.writeVarInt(this.members.size());
		for (Member m : this.members) {
			buf.writeUUID(m.id());
			buf.writeUtf(m.name(), 32);
			buf.writeFloat(m.health());
			buf.writeFloat(m.maxHealth());
			buf.writeVarInt(m.job());
			buf.writeVarInt(m.tier());
			buf.writeVarInt(m.level());
			buf.writeBoolean(m.online());
			buf.writeBoolean(m.inRaid());
		}
	}

	private static PartySyncPayload read(final RegistryFriendlyByteBuf buf) {
		int n = Math.min(16, buf.readVarInt());
		List<Member> members = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			members.add(new Member(buf.readUUID(), buf.readUtf(32), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(),
				buf.readBoolean()));
		}
		return new PartySyncPayload(members);
	}

	@Override
	public Type<PartySyncPayload> type() {
		return TYPE;
	}
}
