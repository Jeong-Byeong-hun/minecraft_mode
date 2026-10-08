package com.minecraftmode.job.engrave;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Opens the {@link EngravingMenu}. */
public class EngravingTableBlock extends Block {
	private static final Component TITLE = Component.translatable("container.minecraft_mode.engraving_table");

	public EngravingTableBlock(final Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult) {
		if (!level.isClientSide()) {
			player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new EngravingMenu(id, inventory, ContainerLevelAccess.create(level, pos)), TITLE));
		}
		return InteractionResult.SUCCESS;
	}
}
