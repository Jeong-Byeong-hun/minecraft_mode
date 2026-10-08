package com.minecraftmode.job.skill;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

/**
 * Runs delayed skill steps (multi-hit, meteors, lingering fields, summon expiry) on the server
 * thread. Tasks are dropped when the server stops.
 */
public final class SkillScheduler {
	private static final List<Task> TASKS = new ArrayList<>();
	private static final List<Task> PENDING = new ArrayList<>();
	private static long tick;

	private record Task(long at, Runnable action) {
	}

	public static void schedule(final int delayTicks, final Runnable action) {
		PENDING.add(new Task(tick + Math.max(1, delayTicks), action));
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(SkillScheduler::run);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			TASKS.clear();
			PENDING.clear();
		});
	}

	private static void run(final MinecraftServer server) {
		tick++;
		TASKS.addAll(PENDING);
		PENDING.clear();
		Iterator<Task> it = TASKS.iterator();
		List<Task> due = new ArrayList<>();
		while (it.hasNext()) {
			Task task = it.next();
			if (task.at <= tick) {
				due.add(task);
				it.remove();
			}
		}
		for (Task task : due) {
			task.action.run();
		}
	}

	private SkillScheduler() {
	}
}
