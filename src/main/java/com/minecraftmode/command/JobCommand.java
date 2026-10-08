package com.minecraftmode.command;

import com.minecraftmode.entity.ClassTrainer;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.JobStats;
import com.minecraftmode.job.quest.QuestData;
import com.minecraftmode.job.quest.QuestDef;
import com.minecraftmode.job.quest.QuestService;
import com.minecraftmode.job.skill.SkillCaster;
import com.minecraftmode.registry.ModEntities;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;

/**
 * Admin/testing command: {@code /job info|set|level|exp|mana|cooldowns|cast|trainer|quest}. Needs permission level 2.
 */
public final class JobCommand {
	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> register(dispatcher));
	}

	private static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal("job")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("info")
					.executes(c -> info(c.getSource(), c.getSource().getPlayerOrException()))
					.then(Commands.argument("target", EntityArgument.player()).executes(c -> info(c.getSource(), EntityArgument.getPlayer(c, "target")))))
				.then(Commands.literal("set")
					.then(Commands.argument("targets", EntityArgument.players())
						.then(Commands.argument("class", StringArgumentType.word())
							.suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(JobClass.values()).map(JobClass::id), b))
							.then(Commands.argument("tier", IntegerArgumentType.integer(0, 4)).executes(JobCommand::set)))))
				.then(Commands.literal("level")
					.then(Commands.argument("targets", EntityArgument.players())
						.then(Commands.argument("level", IntegerArgumentType.integer(1, JobProgression.MAX_LEVEL)).executes(JobCommand::level))))
				.then(Commands.literal("exp")
					.then(Commands.argument("targets", EntityArgument.players())
						.then(Commands.argument("amount", IntegerArgumentType.integer(1)).executes(JobCommand::exp))))
				.then(Commands.literal("mana")
					.then(Commands.argument("targets", EntityArgument.players()).executes(JobCommand::mana)))
				.then(Commands.literal("cooldowns")
					.then(Commands.argument("targets", EntityArgument.players()).executes(JobCommand::cooldowns)))
				.then(Commands.literal("cast")
					.then(Commands.argument("slot", IntegerArgumentType.integer(1, 4))
						.executes(c -> {
							SkillCaster.Result result = SkillCaster.tryCast(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "slot") - 1);
							c.getSource().sendSuccess(() -> Component.literal("cast: " + result), false);
							return result == SkillCaster.Result.OK ? 1 : 0;
						})))
				.then(Commands.literal("trainer")
					.then(Commands.argument("class", StringArgumentType.word())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(JobClass.PLAYABLE.stream().map(JobClass::id), b))
						.executes(JobCommand::trainer)))
				.then(Commands.literal("quest")
					.then(Commands.argument("targets", EntityArgument.players())
						.then(Commands.literal("clear").executes(c -> quest(c, false)))
						.then(Commands.literal("goals").executes(c -> quest(c, true)))))
		);
	}

	/** Summons a class trainer where the command runs (for worlds without the capital, or extra posts). */
	private static int trainer(final CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		JobClass job = JobClass.byId(StringArgumentType.getString(c, "class"));
		if (job == JobClass.NONE) {
			throw new SimpleCommandExceptionType(Component.translatable("commands.minecraft_mode.job.unknown_class")).create();
		}
		CommandSourceStack source = c.getSource();
		ClassTrainer trainer = ModEntities.CLASS_TRAINER.create(source.getLevel(), EntitySpawnReason.COMMAND);
		if (trainer == null) {
			return 0;
		}
		trainer.setJob(job);
		trainer.snapTo(source.getPosition().x, source.getPosition().y, source.getPosition().z, source.getRotation().y, 0.0F);
		source.getLevel().addFreshEntity(trainer);
		source.sendSuccess(() -> Component.translatable("commands.minecraft_mode.job.trainer", Component.translatable(ClassTrainer.nameKey(job))), true);
		return 1;
	}

	/** {@code clear} drops the active trial; {@code goals} completes its kill goals (tokens and materials are still needed). */
	private static int quest(final CommandContext<CommandSourceStack> c, final boolean goals) throws CommandSyntaxException {
		Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "targets");
		for (ServerPlayer player : players) {
			QuestDef quest = QuestService.active(player);
			if (quest == null) {
				continue;
			}
			QuestData data = QuestService.get(player);
			if (goals) {
				for (int i = 0; i < quest.kills().size(); i++) {
					data = data.withProgress(i, quest.kills().get(i).count());
				}
				QuestService.set(player, data);
			} else {
				QuestService.set(player, data.cleared());
			}
		}
		c.getSource().sendSuccess(() -> Component.translatable(goals ? "commands.minecraft_mode.job.quest_goals" : "commands.minecraft_mode.job.quest_clear", players.size()), true);
		return players.size();
	}

	private static int info(final CommandSourceStack source, final ServerPlayer player) {
		JobData data = JobProgression.get(player);
		source.sendSuccess(() -> Component.translatable(
			"commands.minecraft_mode.job.info",
			player.getDisplayName(),
			Component.translatable(data.job().tierKey(data.tier())),
			data.tier(),
			data.level(),
			data.exp(),
			JobProgression.expToNext(data.level()),
			data.mana(),
			JobStats.maxMana(player)
		), false);
		return data.level();
	}

	private static int set(final CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		JobClass job = JobClass.byId(StringArgumentType.getString(c, "class"));
		int tier = job == JobClass.NONE ? 0 : Math.max(1, IntegerArgumentType.getInteger(c, "tier"));
		Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "targets");
		for (ServerPlayer player : players) {
			JobProgression.set(player, JobProgression.get(player).withJob(job, tier).withCooldowns(Map.of()));
			JobStats.refresh(player);
		}
		c.getSource().sendSuccess(() -> Component.translatable("commands.minecraft_mode.job.set", players.size(), Component.translatable(job.tierKey(tier))), true);
		return players.size();
	}

	private static int level(final CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		int level = IntegerArgumentType.getInteger(c, "level");
		Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "targets");
		for (ServerPlayer player : players) {
			JobProgression.set(player, JobProgression.get(player).withProgress(level, 0));
			JobStats.refresh(player);
		}
		c.getSource().sendSuccess(() -> Component.translatable("commands.minecraft_mode.job.level", players.size(), level), true);
		return players.size();
	}

	private static int exp(final CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		int amount = IntegerArgumentType.getInteger(c, "amount");
		Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "targets");
		for (ServerPlayer player : players) {
			JobProgression.addExp(player, amount);
		}
		c.getSource().sendSuccess(() -> Component.translatable("commands.minecraft_mode.job.exp", amount, players.size()), true);
		return players.size();
	}

	private static int mana(final CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "targets");
		for (ServerPlayer player : players) {
			JobProgression.set(player, JobProgression.get(player).withMana(JobStats.maxMana(player)));
		}
		c.getSource().sendSuccess(() -> Component.translatable("commands.minecraft_mode.job.mana", players.size()), true);
		return players.size();
	}

	private static int cooldowns(final CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "targets");
		for (ServerPlayer player : players) {
			JobProgression.set(player, JobProgression.get(player).withCooldowns(Map.of()));
		}
		c.getSource().sendSuccess(() -> Component.translatable("commands.minecraft_mode.job.cooldowns", players.size()), true);
		return players.size();
	}

	private JobCommand() {
	}
}
