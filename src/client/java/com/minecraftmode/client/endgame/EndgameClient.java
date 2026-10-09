package com.minecraftmode.client.endgame;

import com.minecraftmode.client.job.JobKeys;
import com.minecraftmode.network.AuctionStatePayload;
import com.minecraftmode.network.OpenBountyPayload;
import com.minecraftmode.network.ProgressActionPayload;
import com.minecraftmode.registry.ModItems;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Client side of the endgame systems: opens the bounty board and the market when their NPCs answer, keeps the last market state,
 * and the talent (N) and codex (J) keys.
 */
public final class EndgameClient {
	public static final KeyMapping TALENTS = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.minecraft_mode.talent_screen", InputConstants.Type.KEYBOARD, InputConstants.KEY_N, JobKeys.CATEGORY));
	public static final KeyMapping CODEX = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.minecraft_mode.codex_screen", InputConstants.Type.KEYBOARD, InputConstants.KEY_J, JobKeys.CATEGORY));

	private static @Nullable AuctionStatePayload auction;

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(OpenBountyPayload.TYPE, (payload, context) -> context.client().execute(() -> {
			Minecraft minecraft = context.client();
			if (minecraft.gui.screen() instanceof BountyScreen screen && screen.clerk() == payload.entityId()) {
				screen.update(payload);
			} else {
				minecraft.gui.setScreen(new BountyScreen(payload));
			}
		}));
		ClientPlayNetworking.registerGlobalReceiver(AuctionStatePayload.TYPE, (payload, context) -> context.client().execute(() -> {
			auction = payload;
			Minecraft minecraft = context.client();
			if (!(minecraft.gui.screen() instanceof AuctionScreen)) {
				minecraft.gui.setScreen(new AuctionScreen(payload.entityId()));
			}
		}));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> auction = null));
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (stack.is(ModItems.ENHANCEMENT_STONE) || stack.is(ModItems.PROTECTION_SCROLL) || stack.is(ModItems.LAIR_MAP)) {
				lines.add(1, Component.translatable(stack.getItem().getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			while (TALENTS.consumeClick()) {
				if (minecraft.player != null && minecraft.gui.screen() == null) {
					minecraft.gui.setScreen(new TalentScreen());
				}
			}
			while (CODEX.consumeClick()) {
				if (minecraft.player != null && minecraft.gui.screen() == null) {
					minecraft.gui.setScreen(new CodexScreen());
				}
			}
		});
	}

	public static @Nullable AuctionStatePayload auction() {
		return auction;
	}

	static void progress(final int action, final String value) {
		if (ClientPlayNetworking.canSend(ProgressActionPayload.TYPE)) {
			ClientPlayNetworking.send(new ProgressActionPayload(action, value));
		}
	}

	private EndgameClient() {
	}
}
