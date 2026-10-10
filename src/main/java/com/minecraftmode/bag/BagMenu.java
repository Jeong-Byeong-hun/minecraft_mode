package com.minecraftmode.bag;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The open bag: a 3-row chest screen (vanilla {@code generic_9x3} on the client) over a container that writes back into the bag's
 * stack on every change. The bag itself cannot be moved while it is open, and bags never go inside.
 */
public final class BagMenu extends AbstractContainerMenu {
	private final Inventory inventory;
	private final int bagSlot;
	private final ItemStack bag;
	private final SimpleContainer container;

	private BagMenu(final int id, final Inventory inventory, final int bagSlot) {
		super(MenuType.GENERIC_9x3, id);
		this.inventory = inventory;
		this.bagSlot = bagSlot;
		this.bag = inventory.getItem(bagSlot);
		NonNullList<ItemStack> items = Bags.contents(this.bag);
		this.container = new SimpleContainer(Bags.SIZE) {
			@Override
			public void setChanged() {
				super.setChanged();
				BagMenu.this.save();
			}
		};
		for (int i = 0; i < Bags.SIZE; i++) {
			this.container.setItem(i, items.get(i));
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(this.container, col + row * 9, 8 + col * 18, 18 + row * 18) {
					@Override
					public boolean mayPlace(final ItemStack stack) {
						return stack.getItem().canFitInsideContainerItems();
					}
				});
			}
		}
		this.addStandardInventorySlots(inventory, 8, 18 + 3 * 18 + 13);
	}

	/** Opens the bag in {@code slot} of the player's inventory (does nothing when that is not a bag). */
	public static void open(final ServerPlayer player, final int slot) {
		ItemStack stack = player.getInventory().getItem(slot);
		if (!(stack.getItem() instanceof BagItem)) {
			return;
		}
		player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new BagMenu(id, inventory, slot), stack.getHoverName()));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 0.6F, 1.2F);
	}

	public ItemStack bag() {
		return this.bag;
	}

	private boolean bagPresent() {
		return this.inventory.getItem(this.bagSlot) == this.bag;
	}

	private void save() {
		if (this.bagPresent()) {
			Bags.store(this.bag, this.container.getItems());
		}
	}

	@Override
	public void clicked(final int slotId, final int button, final ContainerInput input, final Player player) {
		if (slotId >= 0 && slotId < this.slots.size()) {
			Slot slot = this.slots.get(slotId);
			if (slot.container == this.inventory && slot.getContainerSlot() == this.bagSlot) {
				return;
			}
		}
		if (input == ContainerInput.SWAP && button == this.bagSlot) {
			return;
		}
		super.clicked(slotId, button, input, player);
	}

	@Override
	public ItemStack quickMoveStack(final Player player, final int index) {
		ItemStack moved = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot.hasItem()) {
			ItemStack stack = slot.getItem();
			moved = stack.copy();
			if (index < Bags.SIZE) {
				if (!this.moveItemStackTo(stack, Bags.SIZE, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(stack, 0, Bags.SIZE, false)) {
				return ItemStack.EMPTY;
			}
			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}
		return moved;
	}

	@Override
	public boolean stillValid(final Player player) {
		return player.isAlive() && this.bagPresent();
	}

	@Override
	public void removed(final Player player) {
		super.removed(player);
		this.save();
	}
}
