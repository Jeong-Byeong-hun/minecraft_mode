package com.minecraftmode.style;

import com.minecraftmode.economy.Wallet;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.registry.ModMenus;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Stylist Celeste's table: the gear on the left, a piece whose look it should take on the right ({@link Looks}). Button
 * {@link #BUTTON_APPLY} pays {@link Looks#price} and uses the right piece up; {@link #BUTTON_RESTORE} gives the gear its own look back.
 */
public class StylistMenu extends AbstractContainerMenu {
	public static final int WIDTH = 176;
	public static final int HEIGHT = 196;
	public static final int TARGET_X = 26;
	public static final int DONOR_X = 80;
	public static final int SLOT_Y = 24;
	public static final int INVENTORY_X = 8;
	public static final int INVENTORY_Y = 114;
	public static final int BUTTON_APPLY = 0;
	public static final int BUTTON_RESTORE = 1;

	private final @Nullable Entity npc;
	private final Container container = new SimpleContainer(2) {
		@Override
		public void setChanged() {
			super.setChanged();
			StylistMenu.this.slotsChanged(this);
		}
	};

	public StylistMenu(final int containerId, final Inventory inventory) {
		this(containerId, inventory, null);
	}

	public StylistMenu(final int containerId, final Inventory inventory, final @Nullable Entity npc) {
		super(ModMenus.STYLIST, containerId);
		this.npc = npc;
		for (int i = 0; i < 2; i++) {
			this.addSlot(new Slot(this.container, i, i == 0 ? TARGET_X : DONOR_X, SLOT_Y) {
				@Override
				public boolean mayPlace(final ItemStack stack) {
					return Looks.styleable(stack);
				}

				@Override
				public int getMaxStackSize() {
					return 1;
				}
			});
		}
		this.addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
	}

	public ItemStack target() {
		return this.container.getItem(0);
	}

	public ItemStack donor() {
		return this.container.getItem(1);
	}

	public boolean canApply() {
		return Looks.compatible(this.target(), this.donor());
	}

	@Override
	public boolean clickMenuButton(final Player player, final int buttonId) {
		ItemStack target = this.target();
		if (buttonId == BUTTON_RESTORE) {
			if (!Looks.styled(target)) {
				return false;
			}
			if (!player.level().isClientSide()) {
				this.container.setItem(0, Looks.restore(target));
				this.effects(player);
				this.broadcastChanges();
			}
			return true;
		}
		if (buttonId != BUTTON_APPLY || !this.canApply()) {
			return false;
		}
		int price = Looks.price(target);
		if (!player.isCreative() && Coins.total(player) < price) {
			return false;
		}
		if (player.level().isClientSide()) {
			return true;
		}
		if (!player.isCreative() && !Wallet.take(player, price)) {
			return false;
		}
		ItemStack donor = this.donor();
		this.container.setItem(0, Looks.apply(target, donor));
		this.container.setItem(1, ItemStack.EMPTY);
		this.effects(player);
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendSystemMessage(Component.translatable("message.minecraft_mode.stylist.done", donor.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		this.broadcastChanges();
		return true;
	}

	private void effects(final Player player) {
		if (player.level() instanceof ServerLevel level) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 0.8F, 1.2F);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.4F);
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY(1.0), player.getZ(), 12, 0.5, 0.5, 0.5, 0.1);
		}
	}

	@Override
	public ItemStack quickMoveStack(final Player player, final int slotIndex) {
		ItemStack clicked = ItemStack.EMPTY;
		Slot slot = this.slots.get(slotIndex);
		if (slot.hasItem()) {
			ItemStack stack = slot.getItem();
			clicked = stack.copy();
			if (slotIndex < 2) {
				if (!this.moveItemStackTo(stack, 2, 38, true)) {
					return ItemStack.EMPTY;
				}
			} else if (Looks.styleable(stack) && (!this.slots.get(0).hasItem() || !this.slots.get(1).hasItem())) {
				if (!this.moveItemStackTo(stack, 0, 2, false)) {
					return ItemStack.EMPTY;
				}
			} else if (slotIndex < 29) {
				if (!this.moveItemStackTo(stack, 29, 38, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(stack, 2, 29, false)) {
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
