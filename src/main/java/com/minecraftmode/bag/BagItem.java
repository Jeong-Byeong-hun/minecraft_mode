package com.minecraftmode.bag;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A bag of {@link Bags#SIZE} slots kept in the stack's {@code minecraft:container} component. Use it in hand, or right-click it in
 * the inventory with an empty cursor, to open it; right-clicking it while holding items puts them in. Items its {@link BagKind}
 * accepts go straight into it when picked up ({@code ItemEntityMixin}). Bags never go into bags or shulker boxes.
 */
public class BagItem extends Item {
	private final BagKind kind;

	public BagItem(final BagKind kind, final Properties properties) {
		super(properties);
		this.kind = kind;
	}

	public BagKind kind() {
		return this.kind;
	}

	@Override
	public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND;
			BagMenu.open(serverPlayer, slot);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean overrideOtherStackedOnMe(final ItemStack bag, final ItemStack carried, final Slot slot, final ClickAction action, final Player player,
		final SlotAccess carriedAccess) {
		if (action != ClickAction.SECONDARY || slot.container != player.getInventory() || !slot.allowModification(player)) {
			return false;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			if (carried.isEmpty()) {
				int index = slot.getContainerSlot();
				serverPlayer.level().getServer().execute(() -> BagMenu.open(serverPlayer, index));
			} else {
				Bags.insert(bag, carried);
			}
		}
		return true;
	}

	@Override
	public boolean canFitInsideContainerItems() {
		return false;
	}
}
