package com.minecraftmode.test;

import java.lang.reflect.Field;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;

/**
 * The last step of every singleplayer block, right before the context closes the world.
 *
 * <p>Fabric's client gametest {@code close()} (6.0.8) defers {@code Minecraft.disconnect} to the start of the next tick phase, where
 * {@code IntegratedServer.halt} submits a blocking task to the server. The server only runs a fresh task while it has time left in the
 * tick ({@code MinecraftServer.shouldRun}: {@code haveTime()}, or the task is 3 ticks old). A server behind schedule has none, so it
 * skips the task and parks on the test phaser - the render thread then waits on the task and the server on the render thread forever,
 * and no amount of settle ticks guarantees the server has caught up. Pushing the next tick deadline out gives that one tick an idle
 * window long enough for the halt task to run.
 */
final class WorldClose {
	private static final long IDLE_WINDOW_NANOS = 2_000_000_000L;
	private static final Field NEXT_TICK_TIME;

	static {
		try {
			NEXT_TICK_TIME = MinecraftServer.class.getDeclaredField("nextTickTimeNanos");
			NEXT_TICK_TIME.setAccessible(true);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("MinecraftServer.nextTickTimeNanos is gone; recheck the world close workaround", e);
		}
	}

	private WorldClose() {
	}

	static void prepare(final ClientGameTestContext context, final TestServerContext server) {
		context.waitTicks(5);
		server.runOnServer(s -> {
			try {
				NEXT_TICK_TIME.setLong(s, Util.getNanos() + IDLE_WINDOW_NANOS);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		});
	}
}
