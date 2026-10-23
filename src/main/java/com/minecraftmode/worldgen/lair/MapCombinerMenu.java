package com.minecraftmode.worldgen.lair;

import com.minecraftmode.bag.Bags;
import com.minecraftmode.economy.Essence;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.loot.EvolutionEtherItem;
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
 * Cartographer Ysolde's table (0.5.49, {@code CityNpc.Role.MAP_COMBINER}): put tier lair maps in the slot and raise one a tier. An attempt
 * ({@link #BUTTON_UPGRADE}) uses one condensed essence below tier {@link LairTier#ETHER_FROM} or one Lv 100 Evolution Ether from it, and
 * succeeds with {@link LairTier#combineChance}; a failure loses only the material (the map stays, the chance does not grow). Three maps of
 * one tier always make one of the next ({@link #BUTTON_MERGE}). A raised map goes back into the slot when it was the only one, else to
 * the inventory.
 */
public class MapCombinerMenu extends AbstractContainerMenu {
	public static final int WIDTH = 200;
	public static final int HEIGHT = 178;
	public static final int SLOT_X = 14;
	public static final int SLOT_Y = 30;
	public static final int INVENTORY_X = 20;
	public static final int INVENTORY_Y = 96;
	public static final int BUTTON_UPGRADE = 0;
	public static final int BUTTON_MERGE = 1;

	public static final int RESULT_NONE = 0;
	public static final int RESULT_SUCCESS = 1;
	public static final int RESULT_FAIL = 2;
	public static final int RESULT_MERGED = 3;

	private final @Nullable Entity npc;
	private final DataSlot result = DataSlot.standalone();
	private final Container container = new SimpleContainer(1) {
		@Override
		public void setChanged() {
			super.setChanged();
			MapCombinerMenu.this.slotsChanged(this);
		}
	};

	public MapCombinerMenu(final int containerId, final Inventory inventory) {
		this(containerId, inventory, null);
	}

	public MapCombinerMenu(final int containerId, final Inventory inventory, final @Nullable Entity npc) {
		super(ModMenus.MAP_COMBINER, containerId);
		this.npc = npc;
		this.addSlot(new Slot(this.container, 0, SLOT_X, SLOT_Y) {
			@Override
			public boolean mayPlace(final ItemStack stack) {
				return LairTier.of(stack) > 0;
			}
		});
		this.addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
		this.addDataSlot(this.result);
	}

	public ItemStack input() {
		return this.container.getItem(0);
	}

	public int result() {
		return this.result.get();
	}

	/** The tier of the maps in the slot (0 = none). */
	public int tier() {
		return LairTier.of(this.input());
	}

	/** Lv 100 Evolution Ether in the inventory (the grade the combiner takes). */
	public static int ether(final Inventory inventory) {
		int total = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(ModItems.EVOLUTION_ETHER) && EvolutionEtherItem.grade(stack) == ItemLevels.MAX_BRACKET) {
				total += stack.getCount();
			}
		}
		return total;
	}

	/** Whether {@code player} holds the material of one attempt from {@code tier}. */
	public static boolean canPay(final Player player, final int tier) {
		if (player.isCreative()) {
			return true;
		}
		return tier >= LairTier.ETHER_FROM ? ether(player.getInventory()) >= 1 : Essence.held(player.getInventory(), ModItems.CONDENSED_ESSENCE) >= 1;
	}

	private static void pay(final Player player, final int tier) {
		if (player.isCreative()) {
			return;
		}
		Inventory inventory = player.getInventory();
		if (tier < LairTier.ETHER_FROM) {
			Essence.take(inventory, ModItems.CONDENSED_ESSENCE, 1);
			return;
		}
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(ModItems.EVOLUTION_ETHER) && EvolutionEtherItem.grade(stack) == ItemLevels.MAX_BRACKET) {
				stack.shrink(1);
				return;
			}
		}
	}

	@Override
	public boolean clickMenuButton(final Player player, final int buttonId) {
		int tier = this.tier();
		if (tier <= 0 || tier >= LairTier.MAX) {
			return false;
		}
		if (buttonId == BUTTON_MERGE) {
			if (this.input().getCount() < LairTier.MERGE) {
				return false;
			}
			if (player instanceof ServerPlayer serverPlayer) {
				this.raise(serverPlayer, tier, LairTier.MERGE);
				this.result.set(RESULT_MERGED);
				serverPlayer.sendOverlayMessage(Component.translatable("message.minecraft_mode.map_combiner.merged", LairTier.MERGE, tier, tier + 1).withStyle(ChatFormatting.GOLD));
				this.broadcastChanges();
			}
			return true;
		}
		if (buttonId != BUTTON_UPGRADE || !canPay(player, tier)) {
			return false;
		}
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return true;
		}
		pay(player, tier);
		ServerLevel level = serverPlayer.level();
		boolean success = player.getRandom().nextInt(100) < LairTier.combineChance(tier);
		if (success) {
			this.raise(serverPlayer, tier, 1);
			this.result.set(RESULT_SUCCESS);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.2F);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.6F);
			level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(1.0), player.getZ(), 20, 0.5, 0.6, 0.5, 0.05);
			serverPlayer.sendOverlayMessage(Component.translatable("message.minecraft_mode.map_combiner.success", tier + 1).withStyle(ChatFormatting.GOLD));
			if (tier + 1 >= 15) {
				level.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("message.minecraft_mode.map_combiner.broadcast", player.getDisplayName(),
					tier + 1).withStyle(ChatFormatting.LIGHT_PURPLE), false);
			}
		} else {
			this.result.set(RESULT_FAIL);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.6F);
			level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY(1.0), player.getZ(), 12, 0.4, 0.4, 0.4, 0.02);
			serverPlayer.sendOverlayMessage(Component.translatable("message.minecraft_mode.map_combiner.failed", tier).withStyle(ChatFormatting.RED));
		}
		this.broadcastChanges();
		return true;
	}

	/** Takes {@code used} maps of {@code tier} out of the slot and puts one of the next tier in their place (or in the inventory). */
	private void raise(final ServerPlayer player, final int tier, final int used) {
		ItemStack slot = this.input();
		slot.shrink(used);
		ItemStack raised = LairTier.map(tier + 1, 1);
		if (slot.isEmpty()) {
			this.container.setItem(0, raised);
		} else {
			Bags.give(player, raised);
		}
		this.container.setChanged();
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
			} else if (LairTier.of(stack) > 0) {
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
