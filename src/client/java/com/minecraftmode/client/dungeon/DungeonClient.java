package com.minecraftmode.client.dungeon;

import com.minecraftmode.network.OpenDungeonPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Opens the dungeon screen when the warden answers. */
public final class DungeonClient {
	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(OpenDungeonPayload.TYPE, (payload, context) -> context.client().execute(
			() -> context.client().gui.setScreen(new DungeonScreen(payload))
		));
	}

	private DungeonClient() {
	}
}
