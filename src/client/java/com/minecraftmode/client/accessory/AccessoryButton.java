package com.minecraftmode.client.accessory;

import com.minecraftmode.client.hud.TrashButton;
import com.minecraftmode.client.mixin.AbstractContainerScreenAccessor;
import com.minecraftmode.network.OpenAccessoriesPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

/** The accessory window's button on the right edge of the survival inventory, just above the trash button. */
public final class AccessoryButton {
	public static void init() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof InventoryScreen inventory)) {
				return;
			}
			Button button = Button.builder(Component.translatable("screen.minecraft_mode.accessories.button"), b -> open())
				.bounds(0, 0, TrashButton.WIDTH, TrashButton.HEIGHT)
				.tooltip(Tooltip.create(Component.translatable("screen.minecraft_mode.accessories.tooltip")))
				.build();
			place(inventory, button);
			Screens.getWidgets(screen).add(button);
			ScreenEvents.beforeExtract(screen).register((s, g, mouseX, mouseY, delta) -> place(inventory, button));
		});
	}

	/** Asks the server for the accessory window. */
	public static void open() {
		if (ClientPlayNetworking.canSend(OpenAccessoriesPayload.TYPE)) {
			ClientPlayNetworking.send(OpenAccessoriesPayload.INSTANCE);
		}
	}

	private static void place(final InventoryScreen screen, final Button button) {
		AbstractContainerScreenAccessor panel = (AbstractContainerScreenAccessor)screen;
		button.setX(panel.minecraftMode$leftPos() + panel.minecraftMode$imageWidth() + 2);
		button.setY(panel.minecraftMode$topPos() + 84 - 1 - TrashButton.HEIGHT - 2);
	}

	private AccessoryButton() {
	}
}
