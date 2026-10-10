package com.minecraftmode.item;

import com.minecraftmode.economy.Essence;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Essence and condensed essence change into each other on use (the crafting recipes do the same):
 * condensed essence breaks into 9 essence (sneak: the whole stack), and 9 essence condense into one
 * (sneak: all loose essence in the inventory, the rest stays loose).
 */
public class EssenceItem extends Item {
	public EssenceItem(final Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		Inventory inventory = player.getInventory();
		boolean condensed = held.is(ModItems.CONDENSED_ESSENCE);
		int count = condensed
			? (player.isShiftKeyDown() ? held.getCount() : 1)
			: Math.min(player.isShiftKeyDown() ? Integer.MAX_VALUE : 1, JobProgression.count(inventory, ModItems.ESSENCE) / Essence.CONDENSED);
		if (count <= 0) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable("message.minecraft_mode.essence.too_few", Essence.CONDENSED).withStyle(ChatFormatting.RED));
			}
			return InteractionResult.FAIL;
		}
		if (!level.isClientSide()) {
			if (condensed) {
				held.shrink(count);
				Essence.give(inventory, ModItems.ESSENCE, count * Essence.CONDENSED);
			} else {
				JobProgression.removeItems(inventory, ModItems.ESSENCE, count * Essence.CONDENSED);
				Essence.give(inventory, ModItems.CONDENSED_ESSENCE, count);
			}
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, condensed ? 1.4F : 0.8F);
		}
		return InteractionResult.SUCCESS;
	}
}
