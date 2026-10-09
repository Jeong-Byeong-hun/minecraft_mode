package com.minecraftmode.raid;

import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.network.PartySyncPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Parties (server memory only; they end when the server stops). {@code /party} drives them: create,
 * invite (expires after a minute), accept, leave, kick, promote, disband. Members get their party's
 * state twice a second ({@link PartySyncPayload}) for the HUD. A player without a party counts as a
 * party of one for raids.
 */
public final class Parties {
	public static final int MAX_SIZE = 6;
	private static final int INVITE_TICKS = 20 * 60;
	private static final Map<UUID, Party> BY_PLAYER = new HashMap<>();
	private static final Map<UUID, Invite> INVITES = new HashMap<>();

	private record Invite(Party party, String from, long expires) {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 10 == 0) {
				sync(server);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> server.execute(() -> leaderLeft(server, handler.player.getUUID())));
		ServerPlayerEvents.JOIN.register(player -> {
			Party party = of(player.getUUID());
			if (party != null) {
				party.add(player.getUUID(), player.getPlainTextName());
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			BY_PLAYER.clear();
			INVITES.clear();
		});
	}

	public static @Nullable Party of(final UUID player) {
		return BY_PLAYER.get(player);
	}

	/** True when both players are in the same party (never for a player without one). */
	public static boolean together(final UUID a, final UUID b) {
		Party party = BY_PLAYER.get(a);
		return party != null && party == BY_PLAYER.get(b);
	}

	/** Online members of {@code player}'s party, or just the player. */
	public static List<ServerPlayer> onlineMembers(final ServerPlayer player) {
		Party party = of(player.getUUID());
		if (party == null) {
			return List.of(player);
		}
		List<ServerPlayer> out = new ArrayList<>();
		for (UUID id : party.members()) {
			ServerPlayer p = player.level().getServer().getPlayerList().getPlayer(id);
			if (p != null) {
				out.add(p);
			}
		}
		return out;
	}

	/** True for a party leader or a player without a party. */
	public static boolean canLead(final ServerPlayer player) {
		Party party = of(player.getUUID());
		return party == null || party.isLeader(player.getUUID());
	}

	// ------------------------------------------------------------------ actions (each answers the player)

	public static boolean create(final ServerPlayer player) {
		if (of(player.getUUID()) != null) {
			return fail(player, "already_in");
		}
		Party party = new Party(player.getUUID(), player.getPlainTextName());
		BY_PLAYER.put(player.getUUID(), party);
		player.sendSystemMessage(msg("created").withStyle(ChatFormatting.GREEN));
		return true;
	}

	public static boolean invite(final ServerPlayer leader, final ServerPlayer target) {
		Party party = of(leader.getUUID());
		if (party == null) {
			create(leader);
			party = of(leader.getUUID());
		}
		if (party == null || !party.isLeader(leader.getUUID())) {
			return fail(leader, "not_leader");
		}
		if (target == leader) {
			return fail(leader, "self");
		}
		if (of(target.getUUID()) != null) {
			return fail(leader, "target_in_party", target.getDisplayName());
		}
		if (party.size() >= MAX_SIZE) {
			return fail(leader, "full", MAX_SIZE);
		}
		INVITES.put(target.getUUID(), new Invite(party, leader.getPlainTextName(), leader.level().getGameTime() + INVITE_TICKS));
		leader.sendSystemMessage(msg("invited", target.getDisplayName()).withStyle(ChatFormatting.GREEN));
		MutableComponent accept = Component.translatable("message.minecraft_mode.party.accept_button")
			.withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withUnderlined(true).withClickEvent(new ClickEvent.RunCommand("/party accept")));
		target.sendSystemMessage(msg("invite_received", leader.getDisplayName()).withStyle(ChatFormatting.GOLD).append(" ").append(accept));
		return true;
	}

	public static boolean accept(final ServerPlayer player) {
		Invite invite = INVITES.remove(player.getUUID());
		if (invite == null || invite.expires() < player.level().getGameTime() || invite.party().size() == 0) {
			return fail(player, "no_invite");
		}
		if (of(player.getUUID()) != null) {
			return fail(player, "already_in");
		}
		Party party = invite.party();
		if (party.size() >= MAX_SIZE) {
			return fail(player, "full", MAX_SIZE);
		}
		party.add(player.getUUID(), player.getPlainTextName());
		BY_PLAYER.put(player.getUUID(), party);
		tell(player.level().getServer(), party, msg("joined", player.getDisplayName()).withStyle(ChatFormatting.GREEN));
		return true;
	}

	public static boolean leave(final ServerPlayer player) {
		Party party = of(player.getUUID());
		if (party == null) {
			return fail(player, "none");
		}
		MinecraftServer server = player.level().getServer();
		remove(server, party, player.getUUID());
		player.sendSystemMessage(msg("left_self").withStyle(ChatFormatting.YELLOW));
		tell(server, party, msg("left", player.getDisplayName()).withStyle(ChatFormatting.YELLOW));
		return true;
	}

	public static boolean kick(final ServerPlayer leader, final String name) {
		Party party = of(leader.getUUID());
		if (party == null || !party.isLeader(leader.getUUID())) {
			return fail(leader, "not_leader");
		}
		for (UUID id : party.members()) {
			if (!id.equals(leader.getUUID()) && party.name(id).equalsIgnoreCase(name)) {
				MinecraftServer server = leader.level().getServer();
				String kicked = party.name(id);
				remove(server, party, id);
				ServerPlayer target = server.getPlayerList().getPlayer(id);
				if (target != null) {
					target.sendSystemMessage(msg("kicked_self").withStyle(ChatFormatting.RED));
				}
				tell(server, party, msg("kicked", kicked).withStyle(ChatFormatting.YELLOW));
				return true;
			}
		}
		return fail(leader, "not_member", name);
	}

	public static boolean promote(final ServerPlayer leader, final String name) {
		Party party = of(leader.getUUID());
		if (party == null || !party.isLeader(leader.getUUID())) {
			return fail(leader, "not_leader");
		}
		for (UUID id : party.members()) {
			if (party.name(id).equalsIgnoreCase(name)) {
				party.promote(id);
				tell(leader.level().getServer(), party, msg("promoted", party.name(id)).withStyle(ChatFormatting.GOLD));
				return true;
			}
		}
		return fail(leader, "not_member", name);
	}

	public static boolean disband(final ServerPlayer leader) {
		Party party = of(leader.getUUID());
		if (party == null || !party.isLeader(leader.getUUID())) {
			return fail(leader, "not_leader");
		}
		MinecraftServer server = leader.level().getServer();
		tell(server, party, msg("disbanded").withStyle(ChatFormatting.YELLOW));
		for (UUID id : new ArrayList<>(party.members())) {
			remove(server, party, id);
		}
		return true;
	}

	public static void list(final ServerPlayer player) {
		Party party = of(player.getUUID());
		if (party == null) {
			player.sendSystemMessage(msg("none").withStyle(ChatFormatting.GRAY));
			return;
		}
		player.sendSystemMessage(msg("list_header", party.size(), MAX_SIZE).withStyle(ChatFormatting.GOLD));
		MinecraftServer server = player.level().getServer();
		for (UUID id : party.members()) {
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			MutableComponent line = Component.literal(" - " + party.name(id)).withStyle(p != null ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY);
			if (party.isLeader(id)) {
				line.append(Component.literal(" ★").withStyle(ChatFormatting.GOLD));
			}
			if (p != null) {
				JobData data = JobProgression.get(p);
				line.append(Component.literal("  Lv." + data.level() + " ").withStyle(ChatFormatting.GRAY)).append(Component.translatable(data.job().tierKey(data.tier())).withColor(data.job().color()));
			}
			player.sendSystemMessage(line);
		}
	}

	// ------------------------------------------------------------------ internals

	private static void remove(final MinecraftServer server, final Party party, final UUID id) {
		party.remove(id);
		BY_PLAYER.remove(id);
		ServerPlayer p = server.getPlayerList().getPlayer(id);
		if (p != null && ServerPlayNetworking.canSend(p, PartySyncPayload.TYPE)) {
			ServerPlayNetworking.send(p, new PartySyncPayload(List.of()));
		}
		if (party.size() == 1) {
			// a party of one is no party
			UUID last = party.leader();
			party.remove(last);
			BY_PLAYER.remove(last);
			ServerPlayer lastPlayer = server.getPlayerList().getPlayer(last);
			if (lastPlayer != null) {
				lastPlayer.sendSystemMessage(msg("disbanded").withStyle(ChatFormatting.YELLOW));
				if (ServerPlayNetworking.canSend(lastPlayer, PartySyncPayload.TYPE)) {
					ServerPlayNetworking.send(lastPlayer, new PartySyncPayload(List.of()));
				}
			}
		}
	}

	/** When a leader logs off, the first online member leads. */
	private static void leaderLeft(final MinecraftServer server, final UUID id) {
		Party party = of(id);
		if (party == null || !party.isLeader(id)) {
			return;
		}
		for (UUID other : party.members()) {
			if (!other.equals(id) && server.getPlayerList().getPlayer(other) != null) {
				party.promote(other);
				tell(server, party, msg("promoted", party.name(other)).withStyle(ChatFormatting.GOLD));
				return;
			}
		}
	}

	private static void sync(final MinecraftServer server) {
		long now = server.overworld().getGameTime();
		INVITES.values().removeIf(invite -> invite.expires() < now);
		java.util.Set<Party> parties = new java.util.HashSet<>(BY_PLAYER.values());
		for (Party party : parties) {
			List<PartySyncPayload.Member> members = new ArrayList<>();
			for (UUID id : party.members()) {
				ServerPlayer p = server.getPlayerList().getPlayer(id);
				if (p == null) {
					members.add(new PartySyncPayload.Member(id, party.name(id), 0.0F, 20.0F, 0, 0, 0, false, false));
				} else {
					JobData data = JobProgression.get(p);
					members.add(new PartySyncPayload.Member(id, party.name(id), p.getHealth(), p.getMaxHealth(), data.job().ordinal(), data.tier(), data.level(), true,
						Raids.instanceOf(p) != null));
				}
			}
			PartySyncPayload payload = new PartySyncPayload(members);
			for (UUID id : party.members()) {
				ServerPlayer p = server.getPlayerList().getPlayer(id);
				if (p != null && ServerPlayNetworking.canSend(p, PartySyncPayload.TYPE)) {
					ServerPlayNetworking.send(p, payload);
				}
			}
		}
	}

	private static void tell(final MinecraftServer server, final Party party, final Component message) {
		for (UUID id : party.members()) {
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			if (p != null) {
				p.sendSystemMessage(message);
			}
		}
	}

	static MutableComponent msg(final String key, final Object... args) {
		return Component.translatable("message.minecraft_mode.party." + key, args);
	}

	private static boolean fail(final ServerPlayer player, final String key, final Object... args) {
		player.sendSystemMessage(msg(key, args).withStyle(ChatFormatting.RED));
		return false;
	}

	private Parties() {
	}
}
