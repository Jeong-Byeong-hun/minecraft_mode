package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: the receiver's party (empty = no party), sent twice a second for the party HUD, the raid screen and the
 * adventurer's menu. The first member is the leader. {@code invitedBy} names the leader whose invite a player without a party can
 * still accept (empty = none).
 */
public record PartySyncPayload(List<Member> members, String invitedBy) implements CustomPacketPayload {
	public static final PartySyncPayload NONE = new PartySyncPayload(List.of(), "");
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
		buf.writeUtf(this.invitedBy, 32);
	}

	private static PartySyncPayload read(final RegistryFriendlyByteBuf buf) {
		int n = Math.min(16, buf.readVarInt());
		List<Member> members = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			members.add(new Member(buf.readUUID(), buf.readUtf(32), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(),
				buf.readBoolean()));
		}
		return new PartySyncPayload(members, buf.readUtf(32));
	}

	@Override
	public Type<PartySyncPayload> type() {
		return TYPE;
	}
}
