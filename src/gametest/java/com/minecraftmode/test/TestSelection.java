package com.minecraftmode.test;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.minecraftmode.MinecraftMode;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;

/**
 * Which client game tests this run executes. Gradle passes the picked names in {@code -Dminecraft_mode.tests}
 * ({@code ./gradlew runClientGameTest -Ptests=job,lair}, or one slice per shard from {@code runClientGameTestParallel});
 * a name is the class name without "ClientGameTest", lower case. Unset runs every test except the manual ones.
 * Every test stays listed in fabric.mod.json and returns at once when it is not picked, so Fabric's checks between
 * tests (title screen, no server) still hold.
 */
final class TestSelection {
	private static final Set<String> MANUAL = Set.of("multiplayer");
	private static final Set<String> PICKED = Arrays.stream(System.getProperty("minecraft_mode.tests", "").split(","))
		.map(s -> s.trim().toLowerCase(Locale.ROOT))
		.filter(s -> !s.isEmpty())
		.collect(Collectors.toUnmodifiableSet());

	private TestSelection() {
	}

	static boolean skip(FabricClientGameTest test) {
		String name = name(test.getClass());
		boolean run = PICKED.isEmpty() ? !MANUAL.contains(name) : PICKED.contains(name);
		if (!run) {
			MinecraftMode.LOGGER.info("[tests] skipping {}", name);
		}
		return !run;
	}

	static String name(Class<?> test) {
		return test.getSimpleName().replace("ClientGameTest", "").toLowerCase(Locale.ROOT);
	}
}
