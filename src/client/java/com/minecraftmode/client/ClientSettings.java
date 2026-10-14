package com.minecraftmode.client;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.map.MapClient;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

/**
 * This client's own display choices (the settings tab of the adventurer's menu, or a command), saved in
 * {@code <gamedir>/minecraft_mode/client.properties}: floating damage numbers
 * ({@code /damagenumbers [true|false]}), the particles of enhanced gear ({@code /enhanceeffects [true|false]}) and the HP/MP bars
 * instead of vanilla hearts ({@code /hpbar [true|false]}) and the held tool's durability beside the hotbar ({@code /durability [true|false]}).
 */
public final class ClientSettings {
	public static boolean damageNumbers = true;
	public static boolean enhanceEffects = true;
	public static boolean hpBar = true;
	public static boolean durability = true;

	private static Path file() {
		return MapClient.root().resolve("client.properties");
	}

	public static void init() {
		load();
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			toggle(dispatcher, "damagenumbers", "damage_numbers", () -> damageNumbers, on -> damageNumbers = on);
			toggle(dispatcher, "enhanceeffects", "enhance_effects", () -> enhanceEffects, on -> enhanceEffects = on);
			toggle(dispatcher, "hpbar", "hp_bar", () -> hpBar, on -> hpBar = on);
			toggle(dispatcher, "durability", "durability", () -> durability, on -> durability = on);
		});
	}

	private static void toggle(final CommandDispatcher<FabricClientCommandSource> dispatcher,
		final String command, final String key, final Supplier<Boolean> get, final Consumer<Boolean> set) {
		dispatcher.register(ClientCommands.literal(command)
			.executes(context -> {
				set.accept(!get.get());
				save();
				context.getSource().sendFeedback(feedback(key, get.get()));
				return 1;
			})
			.then(ClientCommands.argument("on", BoolArgumentType.bool()).executes(context -> {
				set.accept(BoolArgumentType.getBool(context, "on"));
				save();
				context.getSource().sendFeedback(feedback(key, get.get()));
				return 1;
			})));
	}

	private static Component feedback(final String key, final boolean on) {
		return Component.translatable("message.minecraft_mode." + key + (on ? ".on" : ".off"));
	}

	public static void setDamageNumbers(final boolean on) {
		damageNumbers = on;
		save();
	}

	public static void setEnhanceEffects(final boolean on) {
		enhanceEffects = on;
		save();
	}

	public static void setHpBar(final boolean on) {
		hpBar = on;
		save();
	}

	public static void setDurability(final boolean on) {
		durability = on;
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
			enhanceEffects = Boolean.parseBoolean(p.getProperty("enhance_effects", "true"));
			hpBar = Boolean.parseBoolean(p.getProperty("hp_bar", "true"));
			durability = Boolean.parseBoolean(p.getProperty("durability", "true"));
		} catch (IOException | RuntimeException e) {
			MinecraftMode.LOGGER.warn("Could not read {}", file, e);
		}
	}

	private static void save() {
		Properties p = new Properties();
		p.setProperty("damage_numbers", String.valueOf(damageNumbers));
		p.setProperty("enhance_effects", String.valueOf(enhanceEffects));
		p.setProperty("hp_bar", String.valueOf(hpBar));
		p.setProperty("durability", String.valueOf(durability));
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
