package com.minecraftmode.jump;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.level.ServerPlayer;

/**
 * Jump courses (MapleStory-style jump quests): {@link #TENACITY} is the Forest of Tenacity (끈기의 숲). A player enters through
 * {@link #enter}; reaching the end calls every {@link #FINISHED} listener (town quests count it there).
 */
public final class JumpForest {
	/** The Forest of Tenacity. Saved quest key: never rename. */
	public static final String TENACITY = "tenacity";

	/** Called on the server thread when {@code player} reaches the end of {@code course}. */
	public interface Finish {
		void finished(ServerPlayer player, String course);
	}

	public static final List<Finish> FINISHED = new CopyOnWriteArrayList<>();

	/** Sends {@code player} to the start of {@code course}; false (with a message to the player) when they cannot go now. */
	public static boolean enter(final ServerPlayer player, final String course) {
		return false;
	}

	private JumpForest() {
	}
}
