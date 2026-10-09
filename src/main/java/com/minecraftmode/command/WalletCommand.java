package com.minecraftmode.command;

import com.minecraftmode.economy.Wallet;
import com.minecraftmode.loot.Coins;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /wallet} (balance) and {@code /wallet pay <player> <amount>} for everyone; {@code /wallet
 * give|set <players> <amount>} for operators. Amounts are copper or use units: {@code 3g}, {@code 2g 5s},
 * {@code 40s}, {@code 120}.
 */
public final class WalletCommand {
	private static final Pattern PART = Pattern.compile("(\\d{1,9})\\s*([gGsScC]?)");

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> register(dispatcher));
	}

	private static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("wallet")
			.executes(c -> {
				ServerPlayer player = c.getSource().getPlayerOrException();
				player.sendSystemMessage(msg("balance", Coins.component(Wallet.balance(player))).withStyle(ChatFormatting.GOLD));
				return 1;
			})
			.then(Commands.literal("pay")
				.then(Commands.argument("player", EntityArgument.player())
					.then(Commands.argument("amount", StringArgumentType.greedyString()).executes(WalletCommand::pay))))
			.then(Commands.literal("give")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.argument("targets", EntityArgument.players())
					.then(Commands.argument("amount", StringArgumentType.greedyString()).executes(c -> admin(c, false)))))
			.then(Commands.literal("set")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.argument("targets", EntityArgument.players())
					.then(Commands.argument("amount", StringArgumentType.greedyString()).executes(c -> admin(c, true)))))
		);
	}

	private static int pay(final CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		ServerPlayer from = c.getSource().getPlayerOrException();
		ServerPlayer to = EntityArgument.getPlayer(c, "player");
		int amount = parse(StringArgumentType.getString(c, "amount"));
		if (amount <= 0 || to == from) {
			from.sendSystemMessage(msg("bad_amount").withStyle(ChatFormatting.RED));
			return 0;
		}
		if (!Wallet.take(from, amount)) {
			from.sendSystemMessage(msg("short", Coins.component(amount), Coins.component(Wallet.balance(from))).withStyle(ChatFormatting.RED));
			return 0;
		}
		Wallet.add(to, amount);
		from.sendSystemMessage(msg("paid", Coins.component(amount), to.getDisplayName()).withStyle(ChatFormatting.GREEN));
		to.sendSystemMessage(msg("received", Coins.component(amount), from.getDisplayName()).withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int admin(final CommandContext<CommandSourceStack> c, final boolean set) throws CommandSyntaxException {
		Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "targets");
		int amount = parse(StringArgumentType.getString(c, "amount"));
		for (ServerPlayer target : targets) {
			if (set) {
				Wallet.take(target, Wallet.balance(target));
			}
			Wallet.add(target, amount);
		}
		c.getSource().sendSuccess(() -> Component.literal((set ? "Set " : "Gave ") + Coins.format(amount) + " to " + targets.size() + " player(s)"), true);
		return targets.size();
	}

	/** "3g 2s 1c", "40s", "120" -> copper (-1 when nothing could be read). */
	public static int parse(final String text) {
		Matcher m = PART.matcher(text.trim());
		long total = 0;
		boolean any = false;
		while (m.find()) {
			any = true;
			long n = Long.parseLong(m.group(1));
			total += switch (m.group(2).toLowerCase(java.util.Locale.ROOT)) {
				case "g" -> n * Coins.GOLD;
				case "s" -> n * Coins.SILVER;
				default -> n;
			};
		}
		return any ? (int)Math.min(Integer.MAX_VALUE, total) : -1;
	}

	private static net.minecraft.network.chat.MutableComponent msg(final String key, final Object... args) {
		return Component.translatable("message.minecraft_mode.wallet." + key, args);
	}

	private WalletCommand() {
	}
}
