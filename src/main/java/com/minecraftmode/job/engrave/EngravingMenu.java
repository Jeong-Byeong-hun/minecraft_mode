package com.minecraftmode.job.engrave;

import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.registry.ModBlocks;
import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.registry.ModMenus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Engraving table: put a class weapon or armor piece in the slot, then pick one of three offered
 * lines from its pool (class and weapon shape, or class and armor slot), paid with essence from the
 * inventory (condensed essence counts as 9). Lines stack, up to 3 on weapons and 4 on armor; a line
 * can be removed and the offers rerolled.
 *
 * <p>Buttons: 0-2 engrave offer, 3 reroll offers, 10-12 remove line.
 */
public class EngravingMenu extends AbstractContainerMenu {
	public static final int WIDTH = 200;
	public static final int HEIGHT = 214;
	public static final int WEAPON_X = 14;
	public static final int WEAPON_Y = 24;
	public static final int INVENTORY_X = 20;
	public static final int INVENTORY_Y = 132;
	public static final int BUTTON_REROLL = 3;
	public static final int BUTTON_REMOVE = 10;

	private final ContainerLevelAccess access;
	private final Container container = new SimpleContainer(1) {
		@Override
		public void setChanged() {
			super.setChanged();
			EngravingMenu.this.slotsChanged(this);
		}
	};
	/** offer 0..2 (engraving ordinal, -1 = none), engrave cost, reroll cost, remove cost */
	private final ContainerData data = new SimpleContainerData(6);

	public EngravingMenu(final int containerId, final Inventory inventory) {
		this(containerId, inventory, ContainerLevelAccess.NULL);
	}

	public EngravingMenu(final int containerId, final Inventory inventory, final ContainerLevelAccess access) {
		super(ModMenus.ENGRAVING, containerId);
		this.access = access;
		this.addSlot(new Slot(this.container, 0, WEAPON_X, WEAPON_Y) {
			@Override
			public boolean mayPlace(final ItemStack itemStack) {
				return ClassGear.of(itemStack) != null;
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});
		this.addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
		this.addDataSlots(this.data);
		for (int i = 0; i < 3; i++) {
			this.data.set(i, -1);
		}
	}

	public ItemStack weapon() {
		return this.container.getItem(0);
	}

	/** Offered engraving, or null. */
	public Engraving offer(final int index) {
		int ordinal = this.data.get(index);
		return ordinal < 0 || ordinal >= Engraving.values().length ? null : Engraving.values()[ordinal];
	}

	public int engraveCost() {
		return this.data.get(3);
	}

	public int rerollCost() {
		return this.data.get(4);
	}

	public int removeCost() {
		return this.data.get(5);
	}

	@Override
	public void slotsChanged(final Container container) {
		super.slotsChanged(container);
		this.updateOffers();
	}

	private void updateOffers() {
		ItemStack stack = this.weapon();
		ClassGear gear = ClassGear.of(stack);
		if (gear == null) {
			for (int i = 0; i < 6; i++) {
				this.data.set(i, i < 3 ? -1 : 0);
			}
			return;
		}
		Engravings engravings = engravings(stack);
		List<Engraving> offers = offers(gear, engravings);
		for (int i = 0; i < 3; i++) {
			this.data.set(i, i < offers.size() && engravings.lines().size() < gear.maxLines() ? offers.get(i).ordinal() : -1);
		}
		this.data.set(3, (gear.isWeapon() ? 4 : 3) * gear.tier() * (engravings.lines().size() + 1));
		this.data.set(4, 2 * gear.tier());
		this.data.set(5, 2 * gear.tier());
	}

	public static Engravings engravings(final ItemStack stack) {
		return stack.getOrDefault(ModDataComponents.ENGRAVINGS, Engravings.EMPTY);
	}

	/** Three distinct lines from the item's pool, fixed by its seed and line count. */
	public static List<Engraving> offers(final ClassGear gear, final Engravings engravings) {
		List<Engraving> pool = new ArrayList<>(gear.engravingPool());
		Collections.shuffle(pool, new Random(engravings.seed() * 31L + engravings.lines().size()));
		return pool.subList(0, Math.min(3, pool.size()));
	}

	@Override
	public boolean clickMenuButton(final Player player, final int buttonId) {
		ItemStack stack = this.weapon();
		ClassGear gear = ClassGear.of(stack);
		if (gear == null) {
			return false;
		}
		Engravings engravings = engravings(stack);
		boolean engrave = buttonId >= 0 && buttonId < 3;
		boolean remove = buttonId >= BUTTON_REMOVE && buttonId < BUTTON_REMOVE + engravings.lines().size();
		if (!engrave && !remove && buttonId != BUTTON_REROLL) {
			return false;
		}
		int cost = engrave ? this.engraveCost() : buttonId == BUTTON_REROLL ? this.rerollCost() : this.removeCost();
		if (engrave && (this.offer(buttonId) == null || engravings.lines().size() >= gear.maxLines())) {
			return false;
		}
		if (!player.isCreative() && essence(player.getInventory()) < cost) {
			return false;
		}
		if (player.level().isClientSide()) {
			return true;
		}
		pay(player, cost);
		int seed = player.getRandom().nextInt();
		Engravings updated;
		if (engrave) {
			updated = engravings.with(this.offer(buttonId), seed);
		} else if (remove) {
			updated = engravings.without(buttonId - BUTTON_REMOVE, seed);
		} else {
			updated = engravings.withSeed(seed);
		}
		stack.set(ModDataComponents.ENGRAVINGS, updated);
		this.container.setChanged();
		this.access.execute((level, pos) -> {
			level.playSound(null, pos, engrave ? SoundEvents.ENCHANTMENT_TABLE_USE : SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, engrave ? 1.2F : 0.8F);
			if (level instanceof ServerLevel serverLevel) {
				serverLevel.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.5);
			}
		});
		this.broadcastChanges();
		return true;
	}

	/** Essence units in the inventory; condensed essence counts as 9. */
	public static int essence(final Inventory inventory) {
		return JobProgression.count(inventory, ModItems.ESSENCE) + 9 * JobProgression.count(inventory, ModItems.CONDENSED_ESSENCE);
	}

	private static void pay(final Player player, int units) {
		if (player.isCreative() || units <= 0) {
			return;
		}
		Inventory inventory = player.getInventory();
		int loose = Math.min(units, JobProgression.count(inventory, ModItems.ESSENCE));
		JobProgression.removeItems(inventory, ModItems.ESSENCE, loose);
		units -= loose;
		if (units > 0) {
			int condensed = (units + 8) / 9;
			JobProgression.removeItems(inventory, ModItems.CONDENSED_ESSENCE, condensed);
			int change = condensed * 9 - units;
			if (change > 0) {
				inventory.placeItemBackInInventory(new ItemStack(ModItems.ESSENCE, change), Prediction.SERVER_ONLY);
			}
		}
	}

	@Override
	public ItemStack quickMoveStack(final Player player, final int slotIndex) {
		ItemStack clicked = ItemStack.EMPTY;
		Slot slot = this.slots.get(slotIndex);
		if (slot.hasItem()) {
			ItemStack stack = slot.getItem();
			clicked = stack.copy();
			if (slotIndex == 0) {
				if (!this.moveItemStackTo(stack, 1, 37, true)) {
					return ItemStack.EMPTY;
				}
			} else if (ClassGear.of(stack) != null && !this.slots.get(0).hasItem()) {
				if (!this.moveItemStackTo(stack, 0, 1, false)) {
					return ItemStack.EMPTY;
				}
			} else if (slotIndex < 28) {
				if (!this.moveItemStackTo(stack, 28, 37, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(stack, 1, 28, false)) {
				return ItemStack.EMPTY;
			}
			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
			if (stack.getCount() == clicked.getCount()) {
				return ItemStack.EMPTY;
			}
			slot.onTake(player, stack);
		}
		return clicked;
	}

	@Override
	public void removed(final Player player) {
		super.removed(player);
		this.access.execute((level, pos) -> this.clearContainer(player, this.container));
	}

	@Override
	public boolean stillValid(final Player player) {
		return stillValid(this.access, player, ModBlocks.ENGRAVING_TABLE);
	}
}
