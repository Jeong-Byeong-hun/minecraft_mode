package com.minecraftmode.client.raid;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.job.JobKeys;
import com.minecraftmode.network.LootActionPayload;
import com.minecraftmode.network.LootStatePayload;
import com.minecraftmode.network.OpenRaidPayload;
import com.minecraftmode.network.PartySyncPayload;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Client side of parties, raids and loot: keeps the last synced party and loot session, opens the
 * raid screen (marshal) and the loot screen (when a session starts, or with the loot key L), and
 * draws the party HUD.
 */
public final class RaidClient {
	public static final KeyMapping LOOT = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.minecraft_mode.loot_screen", InputConstants.Type.KEYBOARD, InputConstants.KEY_Y, JobKeys.CATEGORY));

	private static List<PartySyncPayload.Member> party = List.of();
	private static @Nullable LootStatePayload loot;
	private static @Nullable OpenRaidPayload raidInfo;

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(OpenRaidPayload.TYPE, (payload, context) -> context.client().execute(() -> {
			raidInfo = payload;
			context.client().gui.setScreen(new RaidScreen(payload.entityId()));
		}));
		ClientPlayNetworking.registerGlobalReceiver(PartySyncPayload.TYPE, (payload, context) -> context.client().execute(() -> party = List.copyOf(payload.members())));
		ClientPlayNetworking.registerGlobalReceiver(LootStatePayload.TYPE, (payload, context) -> context.client().execute(() -> {
			loot = payload;
			Minecraft minecraft = context.client();
			if (payload.open() && !(minecraft.gui.screen() instanceof LootScreen)) {
				minecraft.gui.setScreen(new LootScreen());
			}
		}));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
			party = List.of();
			loot = null;
			raidInfo = null;
		}));
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			while (LOOT.consumeClick()) {
				openLoot(minecraft);
			}
		});
		// after the class HUD, which tells the party frames where its panels end
		HudElementRegistry.attachElementAfter(MinecraftMode.id("job_hud"), MinecraftMode.id("party_hud"), PartyHud::extract);
	}

	public static List<PartySyncPayload.Member> party() {
		return party;
	}

	/** What the marshal last sent: the cycle, its modifiers and the record tables. */
	public static @Nullable OpenRaidPayload raidInfo() {
		return raidInfo;
	}

	public static @Nullable LootStatePayload loot() {
		return loot;
	}

	static void openLoot(final Minecraft minecraft) {
		if (minecraft.player == null) {
			return;
		}
		if (loot == null || loot.lots().isEmpty()) {
			minecraft.player.sendOverlayMessage(Component.translatable("message.minecraft_mode.loot.none"));
			return;
		}
		send(LootActionPayload.Action.REFRESH, 0);
		minecraft.gui.setScreen(new LootScreen());
	}

	static void send(final LootActionPayload.Action action, final int value) {
		if (loot != null && ClientPlayNetworking.canSend(LootActionPayload.TYPE)) {
			ClientPlayNetworking.send(new LootActionPayload(loot.session(), action, value));
		}
	}

	private RaidClient() {
	}
}
