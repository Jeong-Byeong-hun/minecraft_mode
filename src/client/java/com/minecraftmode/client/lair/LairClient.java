package com.minecraftmode.client.lair;

import com.minecraftmode.network.LairClearPayload;
import com.minecraftmode.network.LairOfferPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.jspecify.annotations.Nullable;

/**
 * The lair map loop on the client: the confirm screen of a lair map ({@link LairTravelScreen}) opens at once; the clear screen
 * ({@link LairClearScreen}) waits until no other screen is open, because the clear arrives while the treasure chest is still open.
 */
public final class LairClient {
	private static @Nullable LairClearPayload pendingClear;

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(LairOfferPayload.TYPE, (payload, context) -> context.client().execute(
			() -> context.client().gui.setScreen(new LairTravelScreen(payload))
		));
		ClientPlayNetworking.registerGlobalReceiver(LairClearPayload.TYPE, (payload, context) -> context.client().execute(() -> pendingClear = payload));
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			if (pendingClear == null) {
				return;
			}
			if (minecraft.player == null) {
				pendingClear = null;
			} else if (minecraft.gui.screen() == null) {
				LairClearPayload clear = pendingClear;
				pendingClear = null;
				minecraft.gui.setScreen(new LairClearScreen(clear));
			}
		});
	}

	/** A clear waiting for the open screen to close (tests). */
	public static @Nullable LairClearPayload pendingClear() {
		return pendingClear;
	}

	private LairClient() {
	}
}
