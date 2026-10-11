package com.minecraftmode.residence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Gives abandoned land its first look back: every block of the 16x16x16 box becomes what the flat generator put there
 * ({@link ResidenceDimension#natural}), frames, paintings, armor stands, boats and minecarts in it are removed, and whatever they or
 * the containers held goes back through {@code back} (the owner's bags, or their market mailbox when an operator frees land of an
 * offline owner). Players inside are lifted onto the grass.
 */
public final class LandRestore {
	/** No drops, no container spill (they are emptied first), clients told. */
	private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;

	/** Restores {@code claim} and returns how many item stacks were handed to {@code back}. */
	public static int restore(final ServerLevel level, final LandClaims.Claim claim, final Consumer<ItemStack> back) {
		int x0 = claim.chunkX() << 4;
		int z0 = claim.chunkZ() << 4;
		AABB box = new AABB(x0, claim.minY(), z0, x0 + LandClaims.SIZE, claim.minY() + LandClaims.SIZE, z0 + LandClaims.SIZE);
		List<ItemStack> items = new ArrayList<>();
		for (Entity entity : level.getEntitiesOfClass(Entity.class, box,
			e -> e instanceof BlockAttachedEntity || e instanceof ArmorStand || e instanceof VehicleEntity)) {
			collect(entity, items);
			entity.discard();
		}
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int y = claim.minY(); y <= claim.maxY(); y++) {
			BlockState natural = ResidenceDimension.natural(y);
			for (int x = x0; x < x0 + LandClaims.SIZE; x++) {
				for (int z = z0; z < z0 + LandClaims.SIZE; z++) {
					pos.set(x, y, z);
					BlockEntity blockEntity = level.getBlockEntity(pos);
					if (blockEntity instanceof Container container) {
						take(container, items);
					}
					if (!level.getBlockState(pos).equals(natural)) {
						level.setBlock(pos, natural, FLAGS);
					}
				}
			}
		}
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
			if (!level.noCollision(player, player.getBoundingBox())) {
				player.teleportTo(player.getX(), ResidenceDimension.SURFACE_Y + 1, player.getZ());
				player.resetFallDistance();
			}
		}
		items.forEach(back);
		return items.size();
	}

	private static void collect(final Entity entity, final List<ItemStack> back) {
		if (entity instanceof Container container) {
			take(container, back);
		}
		if (entity instanceof ItemFrame frame && !frame.getItem().isEmpty()) {
			back.add(frame.getItem().copy());
			frame.setItem(ItemStack.EMPTY);
		}
		if (entity instanceof LivingEntity living) {
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				ItemStack worn = living.getItemBySlot(slot);
				if (!worn.isEmpty()) {
					back.add(worn.copy());
					living.setItemSlot(slot, ItemStack.EMPTY);
				}
			}
		}
	}

	private static void take(final Container container, final List<ItemStack> back) {
		for (int i = 0; i < container.getContainerSize(); i++) {
			ItemStack stack = container.getItem(i);
			if (!stack.isEmpty()) {
				back.add(stack.copy());
			}
		}
		container.clearContent();
	}

	private LandRestore() {
	}
}
