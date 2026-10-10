package com.minecraftmode.bag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * The inventory's trash button: clicking it with a stack on the cursor destroys that stack; clicking it with an empty cursor takes
 * the last destroyed stack back (one undo, kept in server memory until the player logs out or bins something else).
 */
public final class Trash {
	private static final Map<UUID, ItemStack> LAST = new HashMap<>();

	public static void click(final ServerPlayer player) {
		AbstractContainerMenu menu = player.containerMenu;
		if (menu != player.inventoryMenu) {
			return;
		}
		ItemStack carried = menu.getCarried();
		if (!carried.isEmpty()) {
			LAST.put(player.getUUID(), carried.copy());
			menu.setCarried(ItemStack.EMPTY);
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 0.4F, 1.6F);
		} else {
			ItemStack last = LAST.remove(player.getUUID());
			if (last == null) {
				return;
			}
			menu.setCarried(last);
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4F, 1.0F);
		}
		menu.broadcastChanges();
	}

	public static boolean hasUndo(final ServerPlayer player) {
		return LAST.containsKey(player.getUUID());
	}

	public static void forget(final ServerPlayer player) {
		LAST.remove(player.getUUID());
	}

	public static void clear() {
		LAST.clear();
	}

	private Trash() {
	}
}
