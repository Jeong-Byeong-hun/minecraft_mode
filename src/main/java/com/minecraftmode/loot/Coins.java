package com.minecraftmode.loot;

import com.minecraftmode.economy.Wallet;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Coin amounts in copper units (1 silver = 9 copper, 1 gold = 9 silver = 81 copper): what a player
 * can spend, paying and paying out (all through the {@link Wallet}), and formatting ("3G 2S 1C").
 */
public final class Coins {
	public static final int SILVER = 9;
	public static final int GOLD = 81;

	/** What {@code player} can spend: the wallet plus any coins not yet deposited. */
	public static int total(final Player player) {
		Inventory inventory = player.getInventory();
		return Wallet.balance(player) + JobProgression.count(inventory, ModItems.COPPER_COIN)
			+ SILVER * JobProgression.count(inventory, ModItems.SILVER_COIN)
			+ GOLD * JobProgression.count(inventory, ModItems.GOLD_COIN);
	}

	/** Pays {@code amount} copper from the wallet; false (and nothing taken) when the player cannot pay. */
	public static boolean take(final ServerPlayer player, final int amount) {
		return Wallet.take(player, amount);
	}

	/** Puts {@code amount} copper into the wallet. */
	public static void give(final ServerPlayer player, final int amount) {
		Wallet.add(player, amount);
	}

	/** {@code amount} copper as coin items, gold first (for drops). */
	public static List<ItemStack> asItems(final int amount) {
		List<ItemStack> out = new ArrayList<>();
		int left = Math.max(0, amount);
		if (left / GOLD > 0) {
			out.add(new ItemStack(ModItems.GOLD_COIN, Math.min(64, left / GOLD)));
		}
		left %= GOLD;
		if (left / SILVER > 0) {
			out.add(new ItemStack(ModItems.SILVER_COIN, left / SILVER));
		}
		if (left % SILVER > 0) {
			out.add(new ItemStack(ModItems.COPPER_COIN, left % SILVER));
		}
		return out;
	}

	/** "3G 2S 1C" (only the non-zero parts; "0C" for nothing). */
	public static String format(final int amount) {
		int gold = amount / GOLD;
		int silver = amount % GOLD / SILVER;
		int copper = amount % SILVER;
		StringBuilder out = new StringBuilder();
		if (gold > 0) {
			out.append(gold).append('G');
		}
		if (silver > 0) {
			out.append(out.isEmpty() ? "" : " ").append(silver).append('S');
		}
		if (copper > 0 || out.isEmpty()) {
			out.append(out.isEmpty() ? "" : " ").append(copper).append('C');
		}
		return out.toString();
	}

	public static MutableComponent component(final int amount) {
		return Component.literal(format(amount)).withStyle(ChatFormatting.GOLD);
	}

	/**
	 * A bid step that is about 5% of {@code price}, rounded up to a coin-friendly amount (1C, 3C, 1S,
	 * 3S, 1G, 3G, 5G, 10G, ...), so bids move in sizes people can count.
	 */
	public static int increment(final int price) {
		int target = Math.max(1, Math.round(price * 0.05F));
		int[] steps = {1, 3, SILVER, 3 * SILVER, GOLD, 3 * GOLD, 5 * GOLD, 10 * GOLD, 25 * GOLD, 50 * GOLD, 100 * GOLD};
		for (int step : steps) {
			if (step >= target) {
				return step;
			}
		}
		return 100 * GOLD;
	}

	private Coins() {
	}
}
