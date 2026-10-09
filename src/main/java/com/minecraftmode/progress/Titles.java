package com.minecraftmode.progress;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * Shows a player's chosen title before their name (above the head, in chat and in the tab list)
 * through a scoreboard team of their own ({@code mm_title_<name>}). Players in other teams keep them.
 */
public final class Titles {
	public static final String PREFIX = "mm_title_";

	public static void apply(final ServerPlayer player) {
		Scoreboard scoreboard = player.level().getServer().getScoreboard();
		String name = player.getScoreboardName();
		String teamName = PREFIX + name;
		PlayerTeam current = scoreboard.getPlayersTeam(name);
		if (current != null && !current.getName().equals(teamName)) {
			return;
		}
		String title = Progress.get(player).title();
		Achievements.Achievement a = title.isEmpty() ? null : Achievements.get(title);
		PlayerTeam team = scoreboard.getPlayerTeam(teamName);
		if (a == null) {
			if (team != null) {
				scoreboard.removePlayerTeam(team);
			}
			return;
		}
		if (team == null) {
			team = scoreboard.addPlayerTeam(teamName);
		}
		team.setPlayerPrefix(Component.literal("[").append(Component.translatable(a.titleKey())).append("] ").withStyle(ChatFormatting.GOLD));
		scoreboard.addPlayerToTeam(name, team);
	}

	private Titles() {
	}
}
