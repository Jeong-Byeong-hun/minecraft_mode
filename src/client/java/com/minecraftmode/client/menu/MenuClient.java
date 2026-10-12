package com.minecraftmode.client.menu;

import com.minecraftmode.client.job.JobKeys;
import com.minecraftmode.dungeon.DungeonDimension;
import com.minecraftmode.network.AdminActionPayload;
import com.minecraftmode.network.AdminStatePayload;
import com.minecraftmode.network.CharacterListPayload;
import com.minecraftmode.network.MenuActionPayload;
import com.minecraftmode.raid.RaidDimension;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import java.util.Objects;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import org.jspecify.annotations.Nullable;

/**
 * Opens the adventurer's menu (semicolon, or a button in the top-left corner of the pause menu, which also offers "return home"
 * inside a raid or dungeon) and hands {@link AdminStatePayload}s to an open admin screen. Keeps the latest
 * {@link CharacterListPayload} for the characters tab, and keeps the {@link CharacterSelectScreen} open while the server says the
 * player is choosing (after joining with more than one slot).
 */
public final class MenuClient {
	public static final KeyMapping MENU = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.minecraft_mode.menu", InputConstants.Type.KEYBOARD, InputConstants.KEY_SEMICOLON, JobKeys.CATEGORY));
	private static final int PAUSE_BUTTON_W = 110;
	private static @Nullable CharacterListPayload characters;
	private static boolean choosing;

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			while (MENU.consumeClick()) {
				if (minecraft.player != null && minecraft.gui.screen() == null) {
					minecraft.gui.setScreen(new AdventurerScreen());
				}
			}
			// after the loading screen of a dimension change (to the lobby, or to the chosen character), never over it
			if (choosing && minecraft.player != null && !(minecraft.gui.screen() instanceof CharacterSelectScreen)
				&& !(minecraft.gui.screen() instanceof LevelLoadingScreen)) {
				minecraft.gui.setScreen(new CharacterSelectScreen());
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(CharacterListPayload.TYPE, (payload, context) -> context.client().execute(() -> {
			characters = payload;
			choosing = payload.prompt();
			if (context.client().gui.screen() instanceof AdventurerScreen menu) {
				menu.refresh();
			} else if (context.client().gui.screen() instanceof CharacterSelectScreen select) {
				if (choosing) {
					select.refresh();
				} else {
					context.client().gui.setScreen(null);
				}
			}
		}));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
			characters = null;
			choosing = false;
		}));
		ClientPlayNetworking.registerGlobalReceiver(AdminStatePayload.TYPE, (payload, context) -> context.client().execute(() -> {
			if (context.client().gui.screen() instanceof AdminScreen admin) {
				admin.update(payload);
			}
		}));
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof PauseScreen pause) || !pause.showsPauseMenu() || client.player == null) {
				return;
			}
			Screens.getWidgets(screen).add(Button.builder(Component.translatable("screen.minecraft_mode.menu.title"),
				b -> client.gui.setScreen(new AdventurerScreen())).bounds(4, 4, PAUSE_BUTTON_W, 20).build());
			if (inInstance(client)) {
				Screens.getWidgets(screen).add(Button.builder(Component.translatable("screen.minecraft_mode.menu.return_home").withStyle(ChatFormatting.GOLD), b -> {
					if (ClientPlayNetworking.canSend(MenuActionPayload.TYPE)) {
						ClientPlayNetworking.send(new MenuActionPayload(MenuActionPayload.LEAVE));
					}
					client.gui.setScreen(null);
				}).bounds(4, 28, PAUSE_BUTTON_W, 20).build());
			}
		});
	}

	/** In the raid or dungeon dimension, where "return home" ({@code /raid leave}, {@code /dungeon leave}) applies. */
	static boolean inInstance(final Minecraft minecraft) {
		return minecraft.level != null && (minecraft.level.dimension() == RaidDimension.LEVEL || minecraft.level.dimension() == DungeonDimension.LEVEL);
	}

	/** Whether this player may use the admin screen; the server checks every action again. */
	static boolean isOperator(final @Nullable Minecraft minecraft) {
		return minecraft != null && minecraft.player != null && minecraft.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}

	/** The account's character slots, as the server last sent them (null before the first). */
	static @Nullable CharacterListPayload characters() {
		return characters;
	}

	static void send(final int action, final int amount) {
		if (ClientPlayNetworking.canSend(MenuActionPayload.TYPE)) {
			ClientPlayNetworking.send(new MenuActionPayload(action, "", amount));
		}
	}

	public static void openAdmin() {
		Minecraft minecraft = Minecraft.getInstance();
		if (!isOperator(minecraft) || !ClientPlayNetworking.canSend(AdminActionPayload.TYPE)) {
			return;
		}
		minecraft.gui.setScreen(new AdminScreen());
		ClientPlayNetworking.send(AdminActionPayload.of(AdminActionPayload.REFRESH, ""));
	}

	/** Everyone on the server but this player, sorted by name (from the tab list). */
	static List<String> onlineNames(final Minecraft minecraft) {
		if (minecraft.getConnection() == null || minecraft.player == null) {
			return List.of();
		}
		String self = minecraft.player.getGameProfile().name();
		return minecraft.getConnection().getOnlinePlayers().stream()
			.map(PlayerInfo::getProfile)
			.map(profile -> profile.name())
			.filter(Objects::nonNull)
			.filter(name -> !name.equals(self))
			.sorted(String.CASE_INSENSITIVE_ORDER)
			.toList();
	}

	private MenuClient() {
	}
}
