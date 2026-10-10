package com.minecraftmode.bag;

import com.minecraftmode.job.JobProgression;
import com.minecraftmode.registry.ModAttachments;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Bag contents and the automatic pickup: an item a carried bag of the right kind ({@link BagKind#accepts}) has room for goes into
 * that bag instead of the inventory. Quartermaster Bram hands every adventurer one bag of each kind ({@link #giveStarter}); more are
 * sold at the general store.
 */
public final class Bags {
	public static final int SIZE = 27;

	public static List<Item> items() {
		return List.of(ModItems.GEAR_BAG, ModItems.SUPPLY_BAG, ModItems.ORE_BAG);
	}

	public static NonNullList<ItemStack> contents(final ItemStack bag) {
		NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
		return items;
	}

	public static void store(final ItemStack bag, final List<ItemStack> items) {
		bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
	}

	/**
	 * Moves as much of {@code stack} into {@code bag} as fits (topping up partial stacks first) and returns how many went in; the
	 * stack shrinks by that much. Anything a bag may not hold stays out.
	 */
	public static int insert(final ItemStack bag, final ItemStack stack) {
		if (stack.isEmpty() || !stack.getItem().canFitInsideContainerItems()) {
			return 0;
		}
		NonNullList<ItemStack> items = contents(bag);
		int before = stack.getCount();
		for (ItemStack held : items) {
			if (stack.isEmpty()) {
				break;
			}
			if (!held.isEmpty() && ItemStack.isSameItemSameComponents(held, stack) && held.getCount() < held.getMaxStackSize()) {
				int move = Math.min(stack.getCount(), held.getMaxStackSize() - held.getCount());
				held.grow(move);
				stack.shrink(move);
			}
		}
		for (int i = 0; i < items.size() && !stack.isEmpty(); i++) {
			if (items.get(i).isEmpty()) {
				items.set(i, stack.split(Math.min(stack.getCount(), stack.getMaxStackSize())));
			}
		}
		int moved = before - stack.getCount();
		if (moved > 0) {
			store(bag, items);
		}
		return moved;
	}

	/** The bags {@code player} carries, except one open on screen (its menu writes it back, so it is not read or changed here). */
	private static List<ItemStack> carried(final Player player) {
		List<ItemStack> bags = new ArrayList<>();
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack bag = inventory.getItem(i);
			if (bag.getItem() instanceof BagItem && !(player.containerMenu instanceof BagMenu menu && menu.bag() == bag)) {
				bags.add(bag);
			}
		}
		return bags;
	}

	/** How many of {@code item} {@code player} carries in the inventory and the bags (crafting stations and deliveries count both). */
	public static int count(final Player player, final Item item) {
		int total = JobProgression.count(player.getInventory(), item);
		for (ItemStack bag : carried(player)) {
			for (ItemStack stack : contents(bag)) {
				if (stack.is(item)) {
					total += stack.getCount();
				}
			}
		}
		return total;
	}

	/** Takes {@code amount} of {@code item} from the inventory first, then from the bags. */
	public static void take(final Player player, final Item item, final int amount) {
		int left = amount - Math.min(amount, JobProgression.count(player.getInventory(), item));
		JobProgression.removeItems(player.getInventory(), item, amount - left);
		for (ItemStack bag : carried(player)) {
			if (left <= 0) {
				break;
			}
			NonNullList<ItemStack> items = contents(bag);
			boolean changed = false;
			for (ItemStack stack : items) {
				if (left > 0 && stack.is(item)) {
					int take = Math.min(left, stack.getCount());
					stack.shrink(take);
					left -= take;
					changed = true;
				}
			}
			if (changed) {
				store(bag, items);
			}
		}
	}

	/** Puts what it can of a picked-up {@code stack} into the player's bags that take it; returns how many went in. */
	public static int absorb(final Player player, final ItemStack stack) {
		Inventory inventory = player.getInventory();
		int moved = 0;
		for (int i = 0; i < inventory.getContainerSize() && !stack.isEmpty(); i++) {
			ItemStack bag = inventory.getItem(i);
			// a bag open on screen is written back from its menu, so it takes nothing meanwhile
			boolean open = player.containerMenu instanceof BagMenu menu && menu.bag() == bag;
			if (!open && bag.getItem() instanceof BagItem item && item.kind().accepts(stack)) {
				moved += insert(bag, stack);
			}
		}
		return moved;
	}

	/** Called by {@code ItemEntityMixin} before vanilla picks {@code entity} up: what fits goes into the bags. */
	public static void pickup(final ItemEntity entity, final ServerPlayer player) {
		ItemStack stack = entity.getItem();
		Item item = stack.getItem();
		int moved = absorb(player, stack);
		if (moved <= 0) {
			return;
		}
		player.take(entity, moved);
		player.awardStat(Stats.ITEM_PICKED_UP.get(item), moved);
		if (stack.isEmpty()) {
			entity.discard();
		} else {
			entity.setItem(stack);
		}
	}

	public static boolean starterTaken(final Player player) {
		return player.getAttachedOrElse(ModAttachments.STARTER_BAGS, false);
	}

	/** One bag of each kind, once per player. Returns false when the player already had them. */
	public static boolean giveStarter(final ServerPlayer player) {
		if (starterTaken(player)) {
			return false;
		}
		player.setAttached(ModAttachments.STARTER_BAGS, true);
		for (Item item : items()) {
			player.getInventory().placeItemBackInInventory(new ItemStack(item), Prediction.SERVER_ONLY);
		}
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.bags.given").withStyle(ChatFormatting.GREEN));
		return true;
	}

	private Bags() {
	}
}
