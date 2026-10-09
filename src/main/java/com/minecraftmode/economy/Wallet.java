package com.minecraftmode.economy;

import com.minecraftmode.loot.Coins;
import com.minecraftmode.registry.ModAttachments;
import com.minecraftmode.registry.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The coin wallet: coins never take inventory space. Coin items that reach a player's inventory are
 * moved into the wallet (a copper balance stored on the player, kept through death) twice a second;
 * shops take their coin costs out of it ({@code MerchantMenuMixin}) and every payment in the mod goes
 * through {@link #take}.
 */
public final class Wallet {
	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 10 == 0) {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					deposit(player);
				}
			}
		});
	}

	/** Copper in the wallet (the synced value on the client). */
	public static int balance(final Player player) {
		return player.getAttachedOrElse(ModAttachments.WALLET, 0);
	}

	public static void add(final Player player, final int copper) {
		if (copper > 0) {
			player.setAttached(ModAttachments.WALLET, (int)Math.min(Integer.MAX_VALUE, (long)balance(player) + copper));
		}
	}

	/** Pays {@code copper} from the wallet (and any coins still in the inventory); false and nothing taken when short. */
	public static boolean take(final Player player, final int copper) {
		if (copper <= 0) {
			return true;
		}
		deposit(player);
		int have = balance(player);
		if (have < copper) {
			return false;
		}
		player.setAttached(ModAttachments.WALLET, have - copper);
		return true;
	}

	/** Copper value of one coin item (0 for anything else). */
	public static int value(final Item item) {
		if (item == ModItems.COPPER_COIN) {
			return 1;
		}
		if (item == ModItems.SILVER_COIN) {
			return Coins.SILVER;
		}
		if (item == ModItems.GOLD_COIN) {
			return Coins.GOLD;
		}
		return 0;
	}

	/** Moves every coin item from the inventory into the wallet. */
	public static void deposit(final Player player) {
		Inventory inventory = player.getInventory();
		int gained = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			int each = value(stack.getItem());
			if (each > 0) {
				gained += each * stack.getCount();
				inventory.setItem(i, ItemStack.EMPTY);
			}
		}
		add(player, gained);
	}

	private Wallet() {
	}
}
