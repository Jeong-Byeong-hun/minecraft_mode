package com.minecraftmode.client.hud;

import com.minecraftmode.client.mixin.AbstractContainerScreenAccessor;
import com.minecraftmode.network.TrashPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

/**
 * A trash button on the right edge of the survival inventory: click it with a stack on the cursor to destroy that stack, click it
 * with an empty cursor to take the last destroyed stack back (see {@code Trash}). It follows the panel when the recipe book opens.
 */
public final class TrashButton {
	public static final int WIDTH = 24;
	public static final int HEIGHT = 20;

	public static void init() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof InventoryScreen inventory)) {
				return;
			}
			Button button = Button.builder(Component.translatable("screen.minecraft_mode.trash.button"), b -> ClientPlayNetworking.send(TrashPayload.INSTANCE))
				.bounds(0, 0, WIDTH, HEIGHT)
				.tooltip(Tooltip.create(Component.translatable("screen.minecraft_mode.trash.tooltip")))
				.build();
			place(inventory, button);
			Screens.getWidgets(screen).add(button);
			ScreenEvents.beforeExtract(screen).register((s, g, mouseX, mouseY, delta) -> place(inventory, button));
		});
	}

	/** Just outside the panel's right edge, level with the top of the main inventory rows. */
	private static void place(final InventoryScreen screen, final Button button) {
		AbstractContainerScreenAccessor panel = (AbstractContainerScreenAccessor)screen;
		button.setX(panel.minecraftMode$leftPos() + panel.minecraftMode$imageWidth() + 2);
		button.setY(panel.minecraftMode$topPos() + 84 - 1);
	}

	private TrashButton() {
	}
}
