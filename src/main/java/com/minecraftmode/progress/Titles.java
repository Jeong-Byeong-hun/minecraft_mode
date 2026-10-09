package com.minecraftmode.progress;

import com.minecraftmode.registry.ModAttachments;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jspecify.annotations.Nullable;

/**
 * Worn titles. The title id lives in its own attachment ({@link ModAttachments#TITLE}) that is synced to every player, and the
 * player's display name gets the title in front ({@code PlayerNameMixin}): above the head, in chat and in the tab list. No
 * scoreboard teams are used, so titles never clash with teams from other mods, plugins or {@code /team}.
 */
public final class Titles {
	/** Teams of the older scoreboard-based titles; removed when their player joins. */
	private static final String LEGACY_PREFIX = "mm_title_";

	/** The title {@code player} wears (achievement id, "" for none); the synced value on the client. */
	public static String of(final Player player) {
		return player.getAttachedOrElse(ModAttachments.TITLE, "");
	}

	/** Brings the shown title in line with the records and tells every client the new tab-list name. */
	public static void apply(final ServerPlayer player) {
		removeLegacyTeam(player);
		String title = Progress.get(player).title();
		Achievements.Achievement a = title.isEmpty() ? null : Achievements.get(title);
		String shown = a != null && a.hasTitle() ? title : "";
		if (!shown.equals(of(player))) {
			if (shown.isEmpty()) {
				player.removeAttached(ModAttachments.TITLE);
			} else {
				player.setAttached(ModAttachments.TITLE, shown);
			}
		}
		player.level().getServer().getPlayerList().broadcastAll(
			new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME), List.of(player)));
	}

	/** {@code name} with the worn title in front ("[Tycoon] Alice"), or {@code name} when none is worn. */
	public static Component decorate(final Player player, final Component name) {
		MutableComponent prefix = prefix(of(player));
		return prefix == null ? name : prefix.append(name);
	}

	private static @Nullable MutableComponent prefix(final String title) {
		Achievements.Achievement a = title.isEmpty() ? null : Achievements.get(title);
		if (a == null || !a.hasTitle()) {
			return null;
		}
		return Component.empty().append(Component.literal("[").append(Component.translatable(a.titleKey())).append("] ").withStyle(ChatFormatting.GOLD));
	}

	private static void removeLegacyTeam(final ServerPlayer player) {
		Scoreboard scoreboard = player.level().getServer().getScoreboard();
		PlayerTeam legacy = scoreboard.getPlayerTeam(LEGACY_PREFIX + player.getScoreboardName());
		if (legacy != null) {
			scoreboard.removePlayerTeam(legacy);
		}
	}

	private Titles() {
	}
}
