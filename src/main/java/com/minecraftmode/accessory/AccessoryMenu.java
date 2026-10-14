package com.minecraftmode.accessory;

import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.gear.GearStats;
import com.minecraftmode.registry.ModMenus;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The accessory window: the five accessory slots over the player's inventory. On the server the slots are the character's worn
 * accessories ({@link Accessories#worn}) and every change is written straight back, so nothing lives only in the window. A slot takes
 * only its own kind of piece, and only one the player has the level for.
 */
public class AccessoryMenu extends AbstractContainerMenu {
	public static final int WIDTH = 176;
	public static final int HEIGHT = 172;
	public static final int SLOT_Y = 34;
	public static final int FIRST_X = 36;
	public static final int STEP = 22;
	public static final int INVENTORY_Y = 90;

	private final Player player;
	/** The wearer on the server; null on the client, which only mirrors the slots. */
	private final @Nullable ServerPlayer owner;
	private final SimpleContainer container = new SimpleContainer(Accessories.SLOTS) {
		@Override
		public void setChanged() {
			super.setChanged();
			AccessoryMenu.this.save();
		}
	};

	public AccessoryMenu(final int id, final Inventory inventory) {
		this(id, inventory, null);
	}

	private AccessoryMenu(final int id, final Inventory inventory, final @Nullable ServerPlayer owner) {
		super(ModMenus.ACCESSORIES, id);
		this.player = inventory.player;
		this.owner = owner;
		if (owner != null) {
			java.util.List<ItemStack> worn = Accessories.worn(owner);
			for (int i = 0; i < Accessories.SLOTS; i++) {
				this.container.getItems().set(i, worn.get(i).copy());
			}
		}
		for (AccessorySlot kind : AccessorySlot.values()) {
			int index = kind.ordinal();
			this.addSlot(new Slot(this.container, index, FIRST_X + index * STEP, SLOT_Y) {
				@Override
				public boolean mayPlace(final ItemStack stack) {
					return fits(AccessoryMenu.this.player, kind, stack);
				}

				@Override
				public int getMaxStackSize() {
					return 1;
				}
			});
		}
		this.addStandardInventorySlots(inventory, 8, INVENTORY_Y);
	}

	/** Whether {@code stack} may go into slot {@code kind} for {@code player}. */
	public static boolean fits(final Player player, final AccessorySlot kind, final ItemStack stack) {
		AccessoryDef def = Accessories.def(stack);
		return def != null && def.slot() == kind && Accessories.canWear(JobProgression.get(player), def);
	}

	/** Opens the window for {@code player}. */
	public static void open(final ServerPlayer player) {
		player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new AccessoryMenu(id, inventory, player),
			Component.translatable("container.minecraft_mode.accessories")));
	}

	private void save() {
		if (this.owner != null) {
			Accessories.setWorn(this.owner, this.container.getItems());
			GearStats.invalidate(this.owner);
		}
	}

	@Override
	public ItemStack quickMoveStack(final Player player, final int index) {
		ItemStack moved = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) {
			return moved;
		}
		ItemStack stack = slot.getItem();
		moved = stack.copy();
		int slots = Accessories.SLOTS;
		AccessoryDef def = Accessories.def(stack);
		if (index < slots) {
			if (!this.moveItemStackTo(stack, slots, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (def != null && this.slots.get(def.slot().ordinal()).mayPlace(stack) && !this.slots.get(def.slot().ordinal()).hasItem()) {
			if (!this.moveItemStackTo(stack, def.slot().ordinal(), def.slot().ordinal() + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (index < slots + 27) {
			if (!this.moveItemStackTo(stack, slots + 27, this.slots.size(), false)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.moveItemStackTo(stack, slots, slots + 27, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack.getCount() == moved.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, stack);
		return moved;
	}

	@Override
	public boolean stillValid(final Player player) {
		return player.isAlive();
	}

	@Override
	public void removed(final Player player) {
		super.removed(player);
		this.save();
	}
}
