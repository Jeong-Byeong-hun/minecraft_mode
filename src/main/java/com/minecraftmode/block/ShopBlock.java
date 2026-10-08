package com.minecraftmode.block;

import com.minecraftmode.economy.ShopMerchant;
import com.minecraftmode.economy.ShopType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Right-click opens the vanilla trading screen backed by a {@link ShopMerchant} for this shop's type.
 */
public class ShopBlock extends Block {
	private final ShopType type;

	public ShopBlock(final ShopType type, final Properties properties) {
		super(properties);
		this.type = type;
	}

	public ShopType type() {
		return this.type;
	}

	@Override
	protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult) {
		if (!level.isClientSide()) {
			new ShopMerchant(player, level, pos, this.type).openTradingScreen(player, Component.translatable(this.type.titleKey()), 1);
		}

		return InteractionResult.SUCCESS;
	}
}
