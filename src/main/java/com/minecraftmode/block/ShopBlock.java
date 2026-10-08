package com.minecraftmode.block;

import com.minecraftmode.economy.ShopMerchant;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Right-click opens the vanilla trading screen backed by a {@link ShopMerchant}.
 */
public class ShopBlock extends Block {
	private static final Component TITLE = Component.translatable("container.minecraft_mode.shop");

	public ShopBlock(final Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult) {
		if (!level.isClientSide()) {
			new ShopMerchant(player, level, pos).openTradingScreen(player, TITLE, 1);
		}

		return InteractionResult.SUCCESS;
	}
}
