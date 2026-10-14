package com.minecraftmode.economy;

import com.minecraftmode.accessory.Accessories;
import com.minecraftmode.accessory.AccessoryDef;
import com.minecraftmode.bag.BagItem;
import com.minecraftmode.bag.BagMenu;
import com.minecraftmode.bag.Bags;
import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.engrave.Engravings;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.loot.GearShop;
import com.minecraftmode.network.OpenLootBuyerPayload;
import com.minecraftmode.registry.ModDataComponents;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The loot buyer ({@code CityNpc.Role.LOOT_BUYER}, beside the raid marshal) buys class gear and raid accessories in bulk: a
 * right-click lists every piece in the main inventory (hotbar included) and the carried bags, the player picks some and sells
 * them at once. Prices are the guild's buyback ({@link GearShop#buybackPrice}, {@link GearShop#BUYBACK_SHARE} of the guild price)
 * and the same share of an accessory's bracket weapon price; enhancement, engravings and options are not paid for, as at the
 * guild. Bound pieces never sell. Every sale is remembered by {@link Buyback} like a shop sale, so a mistake can be bought back
 * from any shop at {@link Buyback#MARKUP}. The list sent to the player is kept (copies) and a sale only takes pieces that are
 * still exactly where and what they were.
 */
public final class LootBuyer {
	/** Where a listed piece lies: an inventory slot, or slot {@code bagIndex} of the bag in inventory slot {@code slot}. */
	private record Spot(int slot, int bagIndex, ItemStack stack, int price) {
	}

	/** Inventory slots of the hotbar: pieces there are probably in use, so "select all" leaves them. */
	private static final int HOTBAR = 9;
	private static final Map<UUID, List<Spot>> LISTED = new HashMap<>();

	public static void init() {
		ServerPlayerEvents.LEAVE.register(player -> LISTED.remove(player.getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> LISTED.clear());
	}

	/** Copper the loot buyer pays for {@code stack}, 0 when it buys no such thing. */
	public static int price(final ItemStack stack) {
		if (stack.isEmpty() || stack.has(ModDataComponents.BOUND)) {
			return 0;
		}
		ClassGear gear = ClassGear.of(stack);
		if (gear != null) {
			return GearShop.buybackPrice(gear) * stack.getCount();
		}
		AccessoryDef accessory = Accessories.def(stack);
		if (accessory != null) {
			return Math.max(1, Math.round(GearShop.bracketPrice(ItemLevels.bracket(accessory.level())) * GearShop.BUYBACK_SHARE)) * stack.getCount();
		}
		return 0;
	}

	/** Pieces worth more than their price (enhanced or engraved), which "select all" leaves alone. */
	public static boolean valuable(final ItemStack stack) {
		return Enhancement.level(stack) > 0 || !stack.getOrDefault(ModDataComponents.ENGRAVINGS, Engravings.EMPTY).lines().isEmpty();
	}

	private static List<Spot> scan(final ServerPlayer player) {
		List<Spot> out = new ArrayList<>();
		Inventory inventory = player.getInventory();
		for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
			ItemStack stack = inventory.getItem(i);
			int price = price(stack);
			if (price > 0) {
				out.add(new Spot(i, -1, stack.copy(), price));
			}
		}
		for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
			ItemStack bag = inventory.getItem(i);
			if (!openBag(player, bag)) {
				continue;
			}
			NonNullList<ItemStack> contents = Bags.contents(bag);
			for (int k = 0; k < contents.size(); k++) {
				int price = price(contents.get(k));
				if (price > 0) {
					out.add(new Spot(i, k, contents.get(k).copy(), price));
				}
			}
		}
		return out;
	}

	/** A bag the buyer may look into: carried and not open on screen (its menu writes it back). */
	private static boolean openBag(final ServerPlayer player, final ItemStack bag) {
		return bag.getItem() instanceof BagItem && !(player.containerMenu instanceof BagMenu menu && menu.bag() == bag);
	}

	/** Lists what {@code player} could sell to the loot buyer {@code npcId} and opens the screen. */
	public static void open(final ServerPlayer player, final int npcId) {
		List<Spot> spots = scan(player);
		LISTED.put(player.getUUID(), spots);
		JobClass job = JobProgression.get(player).job();
		List<OpenLootBuyerPayload.Entry> entries = new ArrayList<>(spots.size());
		for (Spot spot : spots) {
			ClassGear gear = ClassGear.of(spot.stack());
			boolean mine = gear == null || gear.job() == job;
			entries.add(new OpenLootBuyerPayload.Entry(spot.stack(), spot.price(), mine, spot.bagIndex() < 0 && spot.slot() < HOTBAR || valuable(spot.stack())));
		}
		if (ServerPlayNetworking.canSend(player, OpenLootBuyerPayload.TYPE)) {
			ServerPlayNetworking.send(player, new OpenLootBuyerPayload(npcId, entries));
		}
	}

	/**
	 * Sells the listed pieces {@code picks} (indices into the last list sent) that are still where they were, then sends a fresh
	 * list. Returns the copper paid.
	 */
	public static int sell(final ServerPlayer player, final int npcId, final List<Integer> picks) {
		List<Spot> spots = LISTED.get(player.getUUID());
		if (spots == null) {
			return 0;
		}
		Inventory inventory = player.getInventory();
		List<Buyback.Entry> sales = new ArrayList<>();
		int total = 0;
		int missing = 0;
		for (int index : picks.stream().distinct().toList()) {
			if (index < 0 || index >= spots.size()) {
				continue;
			}
			Spot spot = spots.get(index);
			if (!take(player, inventory, spot)) {
				missing++;
				continue;
			}
			total += spot.price();
			sales.add(new Buyback.Entry(spot.stack().copy(), spot.price()));
		}
		inventory.setChanged();
		if (total > 0) {
			Wallet.add(player, total);
			Buyback.recordAll(player, sales);
			player.sendSystemMessage(Component.translatable("message.minecraft_mode.loot_buyer.sold", sales.size(), Coins.component(total)).withStyle(ChatFormatting.GOLD));
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 1.1F);
		}
		if (missing > 0) {
			player.sendSystemMessage(Component.translatable("message.minecraft_mode.loot_buyer.moved", missing).withStyle(ChatFormatting.GRAY));
		}
		open(player, npcId);
		return total;
	}

	private static boolean take(final ServerPlayer player, final Inventory inventory, final Spot spot) {
		if (spot.bagIndex() < 0) {
			if (!ItemStack.matches(inventory.getItem(spot.slot()), spot.stack())) {
				return false;
			}
			inventory.setItem(spot.slot(), ItemStack.EMPTY);
			return true;
		}
		ItemStack bag = inventory.getItem(spot.slot());
		if (!openBag(player, bag)) {
			return false;
		}
		NonNullList<ItemStack> contents = Bags.contents(bag);
		if (spot.bagIndex() >= contents.size() || !ItemStack.matches(contents.get(spot.bagIndex()), spot.stack())) {
			return false;
		}
		contents.set(spot.bagIndex(), ItemStack.EMPTY);
		Bags.store(bag, contents);
		return true;
	}

	/** For tests: the price list last sent to {@code player}, or null. */
	public static @Nullable List<Integer> listedPrices(final ServerPlayer player) {
		List<Spot> spots = LISTED.get(player.getUUID());
		return spots == null ? null : spots.stream().map(Spot::price).toList();
	}

	private LootBuyer() {
	}
}
