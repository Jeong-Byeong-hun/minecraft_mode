package com.minecraftmode.command;

import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.Parties;
import com.minecraftmode.raid.Party;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.raid.RaidInstance;
import com.minecraftmode.raid.Raids;
import com.minecraftmode.raid.loot.LootSessions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /party create|invite|accept|leave|kick|promote|disband|list} for everyone;
 * {@code /raid leave|loot} for everyone and {@code /raid start <boss> [players]|list} for operators
 * (starts a raid without the marshal or level checks, for testing).
 */
public final class RaidCommands {
	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> register(dispatcher));
	}

	private static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("party")
			.then(Commands.literal("create").executes(c -> ok(Parties.create(player(c)))))
			.then(Commands.literal("invite")
				.then(Commands.argument("player", EntityArgument.player()).executes(c -> ok(Parties.invite(player(c), EntityArgument.getPlayer(c, "player"))))))
			.then(Commands.literal("accept").executes(c -> ok(Parties.accept(player(c)))))
			.then(Commands.literal("leave").executes(c -> ok(Parties.leave(player(c)))))
			.then(Commands.literal("kick")
				.then(Commands.argument("name", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(memberNames(c), b))
					.executes(c -> ok(Parties.kick(player(c), StringArgumentType.getString(c, "name"))))))
			.then(Commands.literal("promote")
				.then(Commands.argument("name", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(memberNames(c), b))
					.executes(c -> ok(Parties.promote(player(c), StringArgumentType.getString(c, "name"))))))
			.then(Commands.literal("disband").executes(c -> ok(Parties.disband(player(c)))))
			.then(Commands.literal("list").executes(c -> {
				Parties.list(player(c));
				return 1;
			}))
		);
		dispatcher.register(Commands.literal("raid")
			.then(Commands.literal("leave").executes(c -> ok(Raids.leave(player(c)))))
			.then(Commands.literal("loot").executes(c -> ok(LootSessions.open(player(c)))))
			.then(Commands.literal("start")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.argument("boss", StringArgumentType.word())
					.suggests((c, b) -> SharedSuggestionProvider.suggest(RaidBosses.all().stream().map(BossDef::id), b))
					.executes(c -> start(c, Parties.onlineMembers(player(c))))
					.then(Commands.argument("players", EntityArgument.players())
						.executes(c -> start(c, new ArrayList<>(EntityArgument.getPlayers(c, "players")))))))
			.then(Commands.literal("list")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(RaidCommands::list))
		);
	}

	private static int start(final CommandContext<CommandSourceStack> c, final List<ServerPlayer> players) throws CommandSyntaxException {
		BossDef def = RaidBosses.byId(StringArgumentType.getString(c, "boss"));
		if (def == null) {
			c.getSource().sendFailure(Component.literal("Unknown boss"));
			return 0;
		}
		UUID leader = players.isEmpty() ? player(c).getUUID() : players.getFirst().getUUID();
		RaidInstance instance = Raids.start(c.getSource().getServer(), players, def, leader);
		return instance == null ? 0 : 1;
	}

	private static int list(final CommandContext<CommandSourceStack> c) {
		List<RaidInstance> instances = Raids.instances();
		c.getSource().sendSuccess(() -> Component.literal(instances.size() + " raid(s)"), false);
		for (RaidInstance instance : instances) {
			StringBuilder names = new StringBuilder();
			for (RaidInstance.Member m : instance.members()) {
				names.append(names.isEmpty() ? "" : ", ").append(m.name()).append(m.active() ? "" : m.fallen() ? " (fallen)" : " (out)");
			}
			c.getSource().sendSuccess(() -> Component.literal(" #" + instance.id() + " " + instance.boss().id() + " " + instance.state() + " @" + instance.center().toShortString()
				+ " - " + names), false);
		}
		return instances.size();
	}

	private static List<String> memberNames(final CommandContext<CommandSourceStack> c) {
		ServerPlayer p = c.getSource().getPlayer();
		Party party = p == null ? null : Parties.of(p.getUUID());
		List<String> names = new ArrayList<>();
		if (party != null) {
			for (UUID id : party.members()) {
				names.add(party.name(id));
			}
		}
		return names;
	}

	private static ServerPlayer player(final CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		return c.getSource().getPlayerOrException();
	}

	private static int ok(final boolean success) {
		return success ? 1 : 0;
	}

	private RaidCommands() {
	}
}
