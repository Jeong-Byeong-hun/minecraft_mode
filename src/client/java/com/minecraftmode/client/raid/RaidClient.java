package com.minecraftmode.client.raid;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.job.JobKeys;
import com.minecraftmode.network.LootActionPayload;
import com.minecraftmode.network.LootStatePayload;
import com.minecraftmode.network.OpenRaidPayload;
import com.minecraftmode.network.PartySyncPayload;
import com.minecraftmode.network.RaidReportPayload;
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
 * draws the party HUD. A raid victory opens the fight report first ({@link RaidReportScreen}); the loot screen waits behind it.
 */
public final class RaidClient {
	public static final KeyMapping LOOT = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.minecraft_mode.loot_screen", InputConstants.Type.KEYBOARD, InputConstants.KEY_Y, JobKeys.CATEGORY));

	private static List<PartySyncPayload.Member> party = List.of();
	/** The leader whose party invite this player can still accept ("" = none). */
	private static String invitedBy = "";
	private static @Nullable LootStatePayload loot;
	private static @Nullable OpenRaidPayload raidInfo;
	private static @Nullable RaidReportPayload report;

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(OpenRaidPayload.TYPE, (payload, context) -> context.client().execute(() -> {
			raidInfo = payload;
			context.client().gui.setScreen(new RaidScreen(payload.entityId()));
		}));
		ClientPlayNetworking.registerGlobalReceiver(PartySyncPayload.TYPE, (payload, context) -> context.client().execute(() -> {
			// keep the same list while nothing changed, so screens that watch it rebuild only on a real change
			List<PartySyncPayload.Member> members = List.copyOf(payload.members());
			if (!members.equals(party)) {
				party = members;
			}
			invitedBy = payload.invitedBy();
		}));
		ClientPlayNetworking.registerGlobalReceiver(LootStatePayload.TYPE, (payload, context) -> context.client().execute(() -> {
			Minecraft minecraft = context.client();
			if (payload.lots().isEmpty()) {
				// left that session (or it is gone): forget it unless a newer one is already shown
				if (loot != null && loot.session() == payload.session()) {
					loot = null;
					if (minecraft.gui.screen() instanceof LootScreen) {
						minecraft.gui.setScreen(null);
					}
				}
				return;
			}
			loot = payload;
			if (payload.open() && !(minecraft.gui.screen() instanceof LootScreen) && !(minecraft.gui.screen() instanceof RaidReportScreen)) {
				minecraft.gui.setScreen(new LootScreen());
			}
		}));
		ClientPlayNetworking.registerGlobalReceiver(RaidReportPayload.TYPE, (payload, context) -> context.client().execute(() -> {
			report = payload;
			if (payload.open()) {
				context.client().gui.setScreen(new RaidReportScreen(payload));
			}
		}));
		// a last sync from the old server can still be queued when DISCONNECT clears, so a new
		// connection starts clean too (JOIN runs on the client thread); a party re-syncs within 10 ticks
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(RaidClient::reset));
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			while (LOOT.consumeClick()) {
				openLoot(minecraft);
			}
		});
		// after the class HUD, which tells the party frames where its panels end
		HudElementRegistry.attachElementAfter(MinecraftMode.id("job_hud"), MinecraftMode.id("party_hud"), PartyHud::extract);
	}

	private static void reset() {
		party = List.of();
		invitedBy = "";
		loot = null;
		raidInfo = null;
		report = null;
	}

	/** The last raid victory's fight report. */
	public static @Nullable RaidReportPayload report() {
		return report;
	}

	public static List<PartySyncPayload.Member> party() {
		return party;
	}

	public static String invitedBy() {
		return invitedBy;
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
