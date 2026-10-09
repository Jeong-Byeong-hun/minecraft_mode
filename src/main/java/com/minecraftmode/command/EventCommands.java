package com.minecraftmode.command;

import com.minecraftmode.dungeon.DungeonDef;
import com.minecraftmode.dungeon.DungeonInstance;
import com.minecraftmode.dungeon.Dungeons;
import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.event.WorldEvents;
import com.minecraftmode.raid.Parties;
import com.minecraftmode.registry.ModAttachments;
import com.minecraftmode.story.Story;
import com.minecraftmode.story.StoryData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /dungeon leave} and {@code /story} for everyone; for operators {@code /dungeon start <id> [level]} (the party, no warden
 * or checks), {@code /dungeon list}, {@code /worldevent titan [named]|invasion|stop} and {@code /story set <chapter>}.
 */
public final class EventCommands {
	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> register(dispatcher));
	}

	private static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dungeon")
			.then(Commands.literal("leave").executes(c -> Dungeons.leave(player(c)) ? 1 : 0))
			.then(Commands.literal("start")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.argument("dungeon", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(Dungeons.ids(), b))
					.executes(c -> start(c, 0))
					.then(Commands.argument("level", IntegerArgumentType.integer(0, 30)).executes(c -> start(c, IntegerArgumentType.getInteger(c, "level"))))))
			.then(Commands.literal("list")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(EventCommands::list)));
		dispatcher.register(Commands.literal("worldevent")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("titan")
				.executes(c -> WorldEvents.spawnTitan(c.getSource().getLevel(), c.getSource().getPlayer(), null) != null ? 1 : 0)
				.then(Commands.argument("named", StringArgumentType.word())
					.suggests((c, b) -> SharedSuggestionProvider.suggest(NamedMobs.all().stream().map(NamedDef::id), b))
					.executes(c -> {
						NamedDef def = NamedMobs.byId(StringArgumentType.getString(c, "named"));
						return def != null && WorldEvents.spawnTitan(c.getSource().getLevel(), c.getSource().getPlayer(), def) != null ? 1 : 0;
					})))
			.then(Commands.literal("invasion").executes(c -> WorldEvents.startInvasion(c.getSource().getServer().overworld()) ? 1 : 0))
			.then(Commands.literal("stop").executes(c -> {
				WorldEvents.stopTitan(c.getSource().getServer().overworld());
				WorldEvents.stopInvasion(c.getSource().getServer().overworld());
				return 1;
			})));
		dispatcher.register(Commands.literal("story")
			.executes(c -> {
				Story.brief(player(c));
				return 1;
			})
			.then(Commands.literal("set")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.argument("chapter", IntegerArgumentType.integer(0, Story.chapters().size())).executes(c -> {
					ServerPlayer p = player(c);
					StoryData data = Story.data(p);
					p.setAttached(ModAttachments.STORY, new StoryData(IntegerArgumentType.getInteger(c, "chapter"), false, data.worldBosses()));
					return 1;
				}))));
	}

	private static int start(final CommandContext<CommandSourceStack> c, final int level) throws CommandSyntaxException {
		DungeonDef def = Dungeons.def(StringArgumentType.getString(c, "dungeon"));
		if (def == null) {
			c.getSource().sendFailure(Component.literal("Unknown dungeon"));
			return 0;
		}
		ServerPlayer p = player(c);
		List<ServerPlayer> players = Parties.onlineMembers(p);
		return Dungeons.start(c.getSource().getServer(), players, def, p.getUUID(), level, level > 0 ? p.getUUID() : null) != null ? 1 : 0;
	}

	private static int list(final CommandContext<CommandSourceStack> c) {
		List<DungeonInstance> instances = Dungeons.instances();
		c.getSource().sendSuccess(() -> Component.literal(instances.size() + " dungeon run(s)"), false);
		for (DungeonInstance instance : instances) {
			StringBuilder names = new StringBuilder();
			for (DungeonInstance.Member m : instance.members()) {
				names.append(names.isEmpty() ? "" : ", ").append(m.name()).append(m.active() ? "" : " (out)");
			}
			c.getSource().sendSuccess(() -> Component.literal(" " + instance.def().id() + " +" + instance.level() + " " + instance.state() + " room " + instance.room()
				+ " @" + instance.origin().toShortString() + " - " + names), false);
		}
		return instances.size();
	}

	private static ServerPlayer player(final CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		return c.getSource().getPlayerOrException();
	}

	private EventCommands() {
	}
}
