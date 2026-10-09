package com.minecraftmode.loot;

import com.minecraftmode.job.JobProgression;
import com.minecraftmode.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Coin amounts in copper units (1 silver = 9 copper, 1 gold = 9 silver = 81 copper): counting what
 * a player carries, paying with change, paying out, and formatting ("3G 2S 1C").
 */
public final class Coins {
	public static final int SILVER = 9;
	public static final int GOLD = 81;

	public static int total(final Inventory inventory) {
		return JobProgression.count(inventory, ModItems.COPPER_COIN)
			+ SILVER * JobProgression.count(inventory, ModItems.SILVER_COIN)
			+ GOLD * JobProgression.count(inventory, ModItems.GOLD_COIN);
	}

	/** Takes {@code amount} copper worth of coins, giving change; false (and nothing taken) when the player cannot pay. */
	public static boolean take(final ServerPlayer player, final int amount) {
		if (amount <= 0) {
			return true;
		}
		Inventory inventory = player.getInventory();
		int have = total(inventory);
		if (have < amount) {
			return false;
		}
		// simplest exact way: remove every coin, give back the rest in the fewest coins
		JobProgression.removeItems(inventory, ModItems.COPPER_COIN, Integer.MAX_VALUE);
		JobProgression.removeItems(inventory, ModItems.SILVER_COIN, Integer.MAX_VALUE);
		JobProgression.removeItems(inventory, ModItems.GOLD_COIN, Integer.MAX_VALUE);
		give(player, have - amount);
		return true;
	}

	/** Gives {@code amount} copper worth of coins (gold first). */
	public static void give(final ServerPlayer player, final int amount) {
		int left = Math.max(0, amount);
		giveStack(player, ModItems.GOLD_COIN, left / GOLD);
		left %= GOLD;
		giveStack(player, ModItems.SILVER_COIN, left / SILVER);
		giveStack(player, ModItems.COPPER_COIN, left % SILVER);
	}

	private static void giveStack(final ServerPlayer player, final Item coin, int count) {
		while (count > 0) {
			int n = Math.min(count, coin.getDefaultMaxStackSize());
			ItemStack stack = new ItemStack(coin, n);
			player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
			count -= n;
		}
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
