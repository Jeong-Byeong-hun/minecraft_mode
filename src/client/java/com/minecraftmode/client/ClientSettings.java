package com.minecraftmode.client;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.map.MapClient;
import com.mojang.brigadier.arguments.BoolArgumentType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;

/**
 * This client's own display choices, saved in {@code <gamedir>/minecraft_mode/client.properties}: floating damage numbers
 * ({@code /damagenumbers [true|false]}).
 */
public final class ClientSettings {
	public static boolean damageNumbers = true;

	private static Path file() {
		return MapClient.root().resolve("client.properties");
	}

	public static void init() {
		load();
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommands.literal("damagenumbers")
			.executes(context -> {
				setDamageNumbers(!damageNumbers);
				context.getSource().sendFeedback(feedback());
				return 1;
			})
			.then(ClientCommands.argument("on", BoolArgumentType.bool()).executes(context -> {
				setDamageNumbers(BoolArgumentType.getBool(context, "on"));
				context.getSource().sendFeedback(feedback());
				return 1;
			}))));
	}

	private static Component feedback() {
		return Component.translatable(damageNumbers ? "message.minecraft_mode.damage_numbers.on" : "message.minecraft_mode.damage_numbers.off");
	}

	public static void setDamageNumbers(final boolean on) {
		damageNumbers = on;
		save();
	}

	private static void load() {
		Path file = file();
		if (!Files.isRegularFile(file)) {
			return;
		}
		Properties p = new Properties();
		try (var in = Files.newInputStream(file)) {
			p.load(in);
			damageNumbers = Boolean.parseBoolean(p.getProperty("damage_numbers", "true"));
		} catch (IOException | RuntimeException e) {
			MinecraftMode.LOGGER.warn("Could not read {}", file, e);
		}
	}

	private static void save() {
		Properties p = new Properties();
		p.setProperty("damage_numbers", String.valueOf(damageNumbers));
		Path file = file();
		try {
			Files.createDirectories(file.getParent());
			try (var out = Files.newOutputStream(file)) {
				p.store(out, "Minecraft Mode client settings");
			}
		} catch (IOException e) {
			MinecraftMode.LOGGER.warn("Could not write {}", file, e);
		}
	}

	private ClientSettings() {
	}
}
