package com.minecraftmode.craft;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A profession station (kitchen, alchemy table, smithing bench): opens its profession's recipes. */
public class CraftStationBlock extends Block {
	private final Profession profession;

	public CraftStationBlock(final Profession profession, final Properties properties) {
		super(properties);
		this.profession = profession;
	}

	public Profession profession() {
		return this.profession;
	}

	@Override
	protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult) {
		if (!level.isClientSide()) {
			player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new CraftMenu(CraftMenu.type(this.profession), id, inventory, this.profession, pos),
				Component.translatable("container.minecraft_mode.station." + this.profession.id)));
		}
		return InteractionResult.SUCCESS;
	}

}
