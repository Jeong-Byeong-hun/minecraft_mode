package com.minecraftmode.item;

import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.quest.QuestService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Use to drop your class and tier (level and experience stay), so you can pick another class. */
public class ClassResetScrollItem extends Item {
	public ClassResetScrollItem(final Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
		JobData data = JobProgression.get(player);
		if (!data.hasClass()) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable("message.minecraft_mode.job.reset_none").withStyle(ChatFormatting.RED));
			}
			return InteractionResult.FAIL;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			// a trial of the old class would block every other trainer and could not be completed sensibly
			QuestService.abandon(serverPlayer);
			JobProgression.resetClass(serverPlayer);
			player.getItemInHand(hand).consume(1, player);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.6F, 1.4F);
			player.sendSystemMessage(Component.translatable("message.minecraft_mode.job.reset_done").withStyle(ChatFormatting.YELLOW));
		}
		return InteractionResult.SUCCESS;
	}
}
