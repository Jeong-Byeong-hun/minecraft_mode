package com.minecraftmode.command;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /dice} (and the dice button of the adventurer's menu): rolls 1-{@value #SIDES} and shows the result in everyone's chat.
 * A player rolls at most once every {@value #COOLDOWN_TICKS} ticks, so nobody floods the shared chat.
 */
public final class DiceCommand {
	public static final int SIDES = 1000;
	public static final int COOLDOWN_TICKS = 60;
	/** Server tick of each player's last roll (server memory; a smaller tick than the last means a new server, so no wait). */
	private static final Map<UUID, Integer> LAST = new HashMap<>();

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(Commands.literal("dice")
			.executes(c -> Math.max(0, roll(c.getSource().getPlayerOrException())))));
	}

	/** Rolls for {@code player} and tells the whole server; returns the roll, or -1 (with a note to the player) while the last one is too recent. */
	public static int roll(final ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		int now = server.getTickCount();
		Integer last = LAST.get(player.getUUID());
		if (last != null && now >= last && now - last < COOLDOWN_TICKS) {
			int seconds = (COOLDOWN_TICKS - (now - last) + 19) / 20;
			player.sendSystemMessage(Component.translatable("message.minecraft_mode.dice.wait", seconds).withStyle(ChatFormatting.RED));
			return -1;
		}
		LAST.put(player.getUUID(), now);
		int value = 1 + player.getRandom().nextInt(SIDES);
		server.getPlayerList().broadcastSystemMessage(Component.translatable("message.minecraft_mode.dice.rolled", player.getDisplayName(),
			Component.literal(String.valueOf(value)).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), SIDES).withStyle(ChatFormatting.GOLD), false);
		return value;
	}

	private DiceCommand() {
	}
}
