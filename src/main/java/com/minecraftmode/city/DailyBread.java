package com.minecraftmode.city;

import com.minecraftmode.progress.ResetCycle;
import com.minecraftmode.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Baker Hanna's bread: one stack of {@link #COUNT} bread per player per Minecraft day ({@link ResetCycle#day}, so sleeping counts like
 * everywhere else). The day of the last handout is the attachment {@code ModAttachments.DAILY_BREAD}, kept on death.
 */
public final class DailyBread {
	public static final int COUNT = 64;

	/** True when the player already had today's bread. */
	public static boolean takenToday(final Player player) {
		return player.getAttachedOrElse(ModAttachments.DAILY_BREAD, -1L) == ResetCycle.day(player.level());
	}

	/** Hands out today's bread (what does not fit drops at the player's feet). Returns false when it was already taken today. */
	public static boolean give(final ServerPlayer player) {
		if (takenToday(player)) {
			player.sendSystemMessage(Component.translatable("message.minecraft_mode.daily_bread.taken", ResetCycle.remaining(ResetCycle.ticksToNextDay(player.level())))
				.withStyle(ChatFormatting.YELLOW));
			return false;
		}
		player.setAttached(ModAttachments.DAILY_BREAD, ResetCycle.day(player.level()));
		player.getInventory().placeItemBackInInventory(new ItemStack(Items.BREAD, COUNT), net.minecraft.util.Prediction.SERVER_ONLY);
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.daily_bread.given", COUNT).withStyle(ChatFormatting.GREEN));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8F, 0.8F);
		return true;
	}

	private DailyBread() {
	}
}
