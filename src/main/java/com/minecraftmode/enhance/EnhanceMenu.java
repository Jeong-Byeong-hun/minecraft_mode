package com.minecraftmode.enhance;

import com.minecraftmode.economy.Essence;
import com.minecraftmode.economy.Wallet;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.progress.Progress;
import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.registry.ModItems;
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
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Artisan Brokk's enhancement bench: put a class weapon or armor piece in the slot and try for the next level. Every attempt
 * costs coins and essence ({@link Enhancement#essence}; condensed from +6) and, from +6, enhancement stones. A failure keeps the
 * level and adds artisan's spirit; from +{@value Enhancement#RISKY_FROM} it also drops a level unless the attempt is protected
 * (button {@link #BUTTON_PROTECTED}), which uses up a protection scroll only when it saves the piece.
 */
public class EnhanceMenu extends AbstractContainerMenu {
	public static final int WIDTH = 200;
	public static final int HEIGHT = 196;
	public static final int SLOT_X = 14;
	public static final int SLOT_Y = 30;
	public static final int INVENTORY_X = 20;
	public static final int INVENTORY_Y = 114;
	public static final int BUTTON_ENHANCE = 0;
	public static final int BUTTON_PROTECTED = 1;
	/** At +15: awaken the piece one step (awakening crystals and coins, always succeeds). */
	public static final int BUTTON_AWAKEN = 2;

	public static final int RESULT_NONE = 0;
	public static final int RESULT_SUCCESS = 1;
	public static final int RESULT_FAIL = 2;
	public static final int RESULT_DROP = 3;
	public static final int RESULT_SAVED = 4;
	public static final int RESULT_AWAKENED = 5;

	private final @Nullable Entity npc;
	private final DataSlot result = DataSlot.standalone();
	private final Container container = new SimpleContainer(1) {
		@Override
		public void setChanged() {
			super.setChanged();
			EnhanceMenu.this.slotsChanged(this);
		}
	};

	public EnhanceMenu(final int containerId, final Inventory inventory) {
		this(containerId, inventory, null);
	}

	public EnhanceMenu(final int containerId, final Inventory inventory, final @Nullable Entity npc) {
		super(ModMenus.ENHANCE, containerId);
		this.npc = npc;
		this.addSlot(new Slot(this.container, 0, SLOT_X, SLOT_Y) {
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
		this.addDataSlot(this.result);
	}

	public ItemStack input() {
		return this.container.getItem(0);
	}

	/** The outcome of the last attempt ({@code RESULT_*}), for the screen. */
	public int result() {
		return this.result.get();
	}

	/** What one attempt at {@code target} on {@code gear} costs. */
	public record Cost(int coins, int essence, boolean condensed, int stones) {
		public static Cost of(final ClassGear gear, final int target) {
			return new Cost(Enhancement.coins(gear, target), Enhancement.essence(target), Enhancement.condensed(target), Enhancement.stones(target));
		}
	}

	/** Essence the player has toward {@code cost}: essence units up to +5, condensed essence after (either kind pays, see {@link Essence}). */
	public static int essenceHeld(final Inventory inventory, final Cost cost) {
		return Essence.held(inventory, cost.condensed() ? ModItems.CONDENSED_ESSENCE : ModItems.ESSENCE);
	}

	public static boolean canPay(final Player player, final Cost cost, final boolean protect) {
		if (player.isCreative()) {
			return true;
		}
		Inventory inventory = player.getInventory();
		return Coins.total(player) >= cost.coins()
			&& essenceHeld(inventory, cost) >= cost.essence()
			&& JobProgression.count(inventory, ModItems.ENHANCEMENT_STONE) >= cost.stones()
			&& (!protect || JobProgression.count(inventory, ModItems.PROTECTION_SCROLL) > 0);
	}

	/** True when the slot holds a +15 piece that can be awakened further. */
	public boolean canAwaken() {
		Enhancement e = Enhancement.of(this.input());
		return ClassGear.of(this.input()) != null && e.level() >= Enhancement.MAX && e.awaken() < Enhancement.MAX_AWAKEN;
	}

	public static boolean canPayAwaken(final Player player, final ClassGear gear, final int target) {
		return player.isCreative() || Coins.total(player) >= Enhancement.awakenCoins(gear, target)
			&& JobProgression.count(player.getInventory(), ModItems.AWAKENING_CRYSTAL) >= Enhancement.crystals(target);
	}

	private boolean awaken(final Player player) {
		ItemStack stack = this.input();
		ClassGear gear = ClassGear.of(stack);
		if (gear == null || !this.canAwaken()) {
			return false;
		}
		Enhancement current = Enhancement.of(stack);
		int target = current.awaken() + 1;
		if (!canPayAwaken(player, gear, target)) {
			return false;
		}
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return true;
		}
		if (!player.isCreative()) {
			if (!Wallet.take(player, Enhancement.awakenCoins(gear, target))) {
				return false;
			}
			JobProgression.removeItems(player.getInventory(), ModItems.AWAKENING_CRYSTAL, Enhancement.crystals(target));
		}
		stack.set(ModDataComponents.ENHANCEMENT, current.awakened());
		this.container.setChanged();
		this.result.set(RESULT_AWAKENED);
		ServerLevel level = serverPlayer.level();
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.2F);
		level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(1.0), player.getZ(), 40, 0.6, 0.8, 0.6, 0.1);
		serverPlayer.sendOverlayMessage(Component.translatable("message.minecraft_mode.enhance.awakened", target).withColor(0xFF55FF));
		level.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("message.minecraft_mode.enhance.awaken_broadcast", player.getDisplayName(),
			stack.getHoverName(), target).withColor(0xFF55FF), false);
		Progress.awakened(serverPlayer, target);
		this.broadcastChanges();
		return true;
	}

	@Override
	public boolean clickMenuButton(final Player player, final int buttonId) {
		if (buttonId == BUTTON_AWAKEN) {
			return this.awaken(player);
		}
		if (buttonId != BUTTON_ENHANCE && buttonId != BUTTON_PROTECTED) {
			return false;
		}
		ItemStack stack = this.input();
		ClassGear gear = ClassGear.of(stack);
		Enhancement current = Enhancement.of(stack);
		if (gear == null || current.level() >= Enhancement.MAX) {
			return false;
		}
		int target = current.level() + 1;
		boolean protect = buttonId == BUTTON_PROTECTED && Enhancement.risky(target);
		Cost cost = Cost.of(gear, target);
		if (!canPay(player, cost, protect)) {
			return false;
		}
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return true;
		}
		if (!player.isCreative()) {
			if (!Wallet.take(player, cost.coins())) {
				return false;
			}
			Essence.take(player.getInventory(), cost.condensed() ? ModItems.CONDENSED_ESSENCE : ModItems.ESSENCE, cost.essence());
			JobProgression.removeItems(player.getInventory(), ModItems.ENHANCEMENT_STONE, cost.stones());
		}
		ServerLevel level = serverPlayer.level();
		boolean success = player.getRandom().nextInt(100) < Enhancement.chance(target, current.pity());
		Enhancement next;
		int outcome;
		if (success) {
			next = current.succeeded();
			outcome = RESULT_SUCCESS;
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 1.3F);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.6F);
			level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY(1.0), player.getZ(), 25, 0.5, 0.6, 0.5, 0.2);
			serverPlayer.sendOverlayMessage(Component.translatable("message.minecraft_mode.enhance.success", next.level()).withColor(Enhancement.color(next.level())));
			if (next.level() >= 10) {
				Component name = stack.getHoverName();
				level.getServer().getPlayerList().broadcastSystemMessage(
					Component.translatable("message.minecraft_mode.enhance.broadcast", player.getDisplayName(), name, next.level()).withStyle(ChatFormatting.GOLD), false);
			}
		} else {
			boolean risky = Enhancement.risky(target);
			boolean saved = risky && protect;
			next = current.failed(risky && !saved);
			if (saved && !player.isCreative()) {
				JobProgression.removeItems(player.getInventory(), ModItems.PROTECTION_SCROLL, 1);
			}
			outcome = saved ? RESULT_SAVED : risky ? RESULT_DROP : RESULT_FAIL;
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6F, 0.8F);
			level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY(1.0), player.getZ(), 20, 0.4, 0.4, 0.4, 0.02);
			String key = saved ? "message.minecraft_mode.enhance.saved" : risky ? "message.minecraft_mode.enhance.dropped" : "message.minecraft_mode.enhance.failed";
			serverPlayer.sendOverlayMessage(Component.translatable(key, next.level(), next.pity()).withStyle(ChatFormatting.RED));
		}
		stack.set(ModDataComponents.ENHANCEMENT, next);
		this.container.setChanged();
		this.result.set(outcome);
		if (success) {
			Progress.enhanced(serverPlayer, next.level());
		}
		this.broadcastChanges();
		return true;
	}

	@Override
	public void slotsChanged(final Container changed) {
		super.slotsChanged(changed);
		this.result.set(RESULT_NONE);
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
		if (!player.level().isClientSide()) {
			this.clearContainer(player, this.container);
		}
	}

	@Override
	public boolean stillValid(final Player player) {
		return this.npc == null || this.npc.isAlive() && this.npc.distanceToSqr(player) < 64.0;
	}
}
