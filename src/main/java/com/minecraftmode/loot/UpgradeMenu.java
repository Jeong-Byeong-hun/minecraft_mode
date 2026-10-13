package com.minecraftmode.loot;

import com.minecraftmode.accessory.AccessoryDef;
import com.minecraftmode.accessory.Accessories;
import com.minecraftmode.bag.Bags;
import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.job.gear.ClassArmor;
import com.minecraftmode.job.gear.ArmorPieceDef;
import com.minecraftmode.job.gear.ArmorOptions;
import com.minecraftmode.economy.Essence;
import com.minecraftmode.economy.Wallet;
import com.minecraftmode.job.gear.GearRolls;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.registry.ModMenus;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The blacksmith's evolution bench: put a class weapon or armor piece in the slot, pick one of the
 * offered next pieces ({@link GearUpgrades#targets}) and pay {@link GearUpgrades#etherCost}
 * Evolution Ether of that piece's bracket plus coins. Raid accessories of tier 1 and 2 are inherited instead (0.5.24): the targets are
 * the same slot of every next-tier set, paid with the tier's inheritance ticket and coins ({@link Accessories#inherit}). Armor and raid
 * accessories can also have their extra options rolled again for coins. Button id = index of the target, or {@link #BUTTON_REROLL}.
 */
public class UpgradeMenu extends AbstractContainerMenu {
	public static final int WIDTH = 200;
	public static final int HEIGHT = 222;
	public static final int SLOT_X = 14;
	public static final int SLOT_Y = 30;
	public static final int INVENTORY_X = 20;
	public static final int INVENTORY_Y = 140;
	public static final int BUTTON_REROLL = 10;
	public static final int BUTTON_KEEP = 11;
	public static final int BUTTON_APPLY = 12;

	private final @Nullable Entity npc;
	private final Container container = new SimpleContainer(1) {
		@Override
		public void setChanged() {
			super.setChanged();
			UpgradeMenu.this.slotsChanged(this);
		}
	};

	public UpgradeMenu(final int containerId, final Inventory inventory) {
		this(containerId, inventory, null);
	}

	public UpgradeMenu(final int containerId, final Inventory inventory, final @Nullable Entity npc) {
		super(ModMenus.UPGRADE, containerId);
		this.npc = npc;
		this.addSlot(new Slot(this.container, 0, SLOT_X, SLOT_Y) {
			@Override
			public boolean mayPlace(final ItemStack itemStack) {
				return fits(itemStack);
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});
		this.addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
	}

	public ItemStack input() {
		return this.container.getItem(0);
	}

	/** Class gear (evolution, armor options) or a raid accessory (options only). */
	public static boolean fits(final ItemStack stack) {
		return ClassGear.of(stack) != null || Accessories.def(stack) != null;
	}

	public List<ClassGear> targets() {
		ClassGear gear = ClassGear.of(this.input());
		return gear == null ? List.of() : GearUpgrades.targets(gear);
	}

	/** Ether of {@code grade} in the inventory. */
	public static int ether(final Inventory inventory, final int grade) {
		int total = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(ModItems.EVOLUTION_ETHER) && EvolutionEtherItem.grade(stack) == grade) {
				total += stack.getCount();
			}
		}
		return total;
	}

	private static void takeEther(final Inventory inventory, final int grade, int amount) {
		for (int i = 0; i < inventory.getContainerSize() && amount > 0; i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(ModItems.EVOLUTION_ETHER) && EvolutionEtherItem.grade(stack) == grade) {
				int take = Math.min(amount, stack.getCount());
				stack.shrink(take);
				amount -= take;
			}
		}
	}

	/** True when the slot holds class armor or a raid accessory, whose options can be rolled again. */
	public boolean canReroll() {
		return ClassArmor.def(this.input()) != null || Accessories.def(this.input()) != null;
	}

	/** Coins for rolling the options of {@code stack} again: a quarter of its bracket's guild price (0 for anything else). */
	public static int rerollCoins(final ItemStack stack) {
		ClassGear gear = ClassGear.of(stack);
		if (gear != null) {
			return GearUpgrades.rerollCost(gear);
		}
		AccessoryDef accessory = Accessories.def(stack);
		return accessory == null ? 0 : GearUpgrades.rerollCost(ItemLevels.bracket(accessory.level()));
	}

	/** Keeps the old options ({@code apply} false) or takes the pending ones; either way the pending set is gone. */
	private boolean choose(final Player player, final boolean apply) {
		ItemStack stack = this.input();
		GearRolls pending = stack.get(ModDataComponents.GEAR_ROLLS_PENDING);
		if (pending == null) {
			return false;
		}
		if (player.level().isClientSide()) {
			return true;
		}
		if (apply) {
			stack.set(ModDataComponents.GEAR_ROLLS, pending);
		}
		stack.remove(ModDataComponents.GEAR_ROLLS_PENDING);
		this.container.setChanged();
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), apply ? SoundEvents.SMITHING_TABLE_USE : SoundEvents.UI_BUTTON_CLICK.value(),
			SoundSource.BLOCKS, 0.8F, 1.0F);
		this.broadcastChanges();
		return true;
	}

	@Override
	public boolean clickMenuButton(final Player player, final int buttonId) {
		if (buttonId == BUTTON_REROLL) {
			return this.reroll(player);
		}
		if (buttonId == BUTTON_KEEP || buttonId == BUTTON_APPLY) {
			return this.choose(player, buttonId == BUTTON_APPLY);
		}
		AccessoryDef accessory = Accessories.def(this.input());
		if (accessory != null) {
			return this.inherit(player, accessory, buttonId);
		}
		List<ClassGear> targets = this.targets();
		if (buttonId < 0 || buttonId >= targets.size()) {
			return false;
		}
		ClassGear target = targets.get(buttonId);
		int grade = GearUpgrades.grade(target);
		int coins = GearUpgrades.coinCost(target, this.input());
		int etherCost = GearUpgrades.etherCost(target);
		if (!player.isCreative() && (ether(player.getInventory(), grade) < etherCost || Coins.total(player) < coins)) {
			return false;
		}
		if (player.level().isClientSide()) {
			return true;
		}
		if (!player.isCreative()) {
			if (!Wallet.take(player, coins)) {
				return false;
			}
			takeEther(player.getInventory(), grade, etherCost);
		}
		ItemStack evolved = GearUpgrades.evolve(this.input(), target, player.getRandom());
		this.container.setItem(0, evolved);
		if (player.level() instanceof ServerLevel level) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 1.1F);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.6F);
			level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY(1.0), player.getZ(), 30, 0.5, 0.6, 0.5, 0.2);
		}
		this.broadcastChanges();
		return true;
	}

	/** Whether {@code player} can pay inheriting {@code from} into {@code target}: one inheritance ticket of its tier (bags count) and coins. */
	public static boolean canPayInherit(final Player player, final AccessoryDef from, final AccessoryDef target) {
		Item ticket = Accessories.inheritTicket(from.set().tier());
		return ticket != null && (player.isCreative()
			|| Bags.count(player, ticket) >= 1 && Coins.total(player) >= Accessories.inheritCoins(target));
	}

	/** Inherits the accessory in the slot into target {@code index} of {@link Accessories#inheritTargets}. */
	private boolean inherit(final Player player, final AccessoryDef from, final int index) {
		List<AccessoryDef> targets = Accessories.inheritTargets(from);
		if (index < 0 || index >= targets.size()) {
			return false;
		}
		AccessoryDef target = targets.get(index);
		if (!canPayInherit(player, from, target)) {
			return false;
		}
		if (player.level().isClientSide()) {
			return true;
		}
		if (!player.isCreative()) {
			if (!Wallet.take(player, Accessories.inheritCoins(target))) {
				return false;
			}
			Bags.take(player, Accessories.inheritTicket(from.set().tier()), 1);
		}
		this.container.setItem(0, Accessories.inherit(this.input(), target));
		if (player.level() instanceof ServerLevel level) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.6F);
			level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(1.0), player.getZ(), 30, 0.5, 0.6, 0.5, 0.05);
		}
		this.broadcastChanges();
		return true;
	}

	/** Whether {@code player} can pay a reroll of the piece in the slot: condensed essence (loose essence counts) and coins. */
	public boolean canPayReroll(final Player player) {
		return this.canReroll() && (player.isCreative()
			|| Essence.held(player.getInventory(), ModItems.CONDENSED_ESSENCE) >= GearUpgrades.REROLL_CONDENSED && Coins.total(player) >= rerollCoins(this.input()));
	}

	/**
	 * Rolls every extra option again into a pending set kept on the piece: the player then keeps the old options ({@link #BUTTON_KEEP})
	 * or takes the new ones ({@link #BUTTON_APPLY}). Rolling again replaces the pending set. The pending set stays on the piece when the
	 * bench is closed, so the choice can wait.
	 */
	private boolean reroll(final Player player) {
		ItemStack stack = this.input();
		ArmorPieceDef piece = ClassArmor.def(stack);
		AccessoryDef accessory = Accessories.def(stack);
		if (piece == null && accessory == null || !this.canPayReroll(player)) {
			return false;
		}
		if (player.level().isClientSide()) {
			return true;
		}
		if (!player.isCreative()) {
			if (!Wallet.take(player, rerollCoins(stack))) {
				return false;
			}
			Essence.take(player.getInventory(), ModItems.CONDENSED_ESSENCE, GearUpgrades.REROLL_CONDENSED);
		}
		// an accessory keeps its number of lines (that is what its raid difficulty gave it), armor its level's count
		GearRolls rolled = piece != null ? ArmorOptions.roll(piece, player.getRandom())
			: Accessories.roll(accessory, Math.max(1, stack.getOrDefault(ModDataComponents.GEAR_ROLLS, GearRolls.EMPTY).lines().size()), player.getRandom());
		stack.set(ModDataComponents.GEAR_ROLLS_PENDING, rolled);
		this.container.setChanged();
		if (player.level() instanceof ServerLevel level) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
			level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY(1.0), player.getZ(), 20, 0.5, 0.5, 0.5, 0.4);
		}
		this.broadcastChanges();
		return true;
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
			} else if (fits(stack) && !this.slots.get(0).hasItem()) {
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
		if (!player.level().isClientSide()) {
			this.clearContainer(player, this.container);
		}
	}

	@Override
	public boolean stillValid(final Player player) {
		return this.npc == null || this.npc.isAlive() && this.npc.distanceToSqr(player) < 64.0;
	}
}
