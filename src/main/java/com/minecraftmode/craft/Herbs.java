package com.minecraftmode.craft;

import com.minecraftmode.registry.ModItems;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Gathering for alchemy and cooking: breaking grass, ferns, flowers, mushrooms and the plants of the Nether and the End
 * sometimes yields a herb of that place (sunleaf, moonpetal at night, frostroot in the cold, glowcap underground or from
 * mushrooms, emberbloom in the Nether, voidcap in the End). Alchemy raises the chance and gains a little experience.
 */
public final class Herbs {
	public static final int BASE_CHANCE = 12;

	public static void init() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel server && !player.isCreative()) {
				gather(serverPlayer, server, pos, state);
			}
		});
	}

	/** The herb {@code state} at {@code pos} can give, or null for blocks that give none. */
	public static @Nullable Item herbFor(final ServerLevel level, final BlockPos pos, final BlockState state) {
		if (level.dimension() == Level.NETHER) {
			return state.is(Blocks.CRIMSON_ROOTS) || state.is(Blocks.WARPED_ROOTS) || state.is(Blocks.NETHER_SPROUTS) || state.is(Blocks.CRIMSON_FUNGUS)
				|| state.is(Blocks.WARPED_FUNGUS) || state.is(Blocks.WEEPING_VINES) || state.is(Blocks.TWISTING_VINES) ? ModItems.EMBERBLOOM : null;
		}
		if (level.dimension() == Level.END) {
			return state.is(Blocks.CHORUS_FLOWER) || state.is(Blocks.CHORUS_PLANT) ? ModItems.VOIDCAP : null;
		}
		if (state.is(Blocks.RED_MUSHROOM) || state.is(Blocks.BROWN_MUSHROOM) || state.is(Blocks.GLOW_LICHEN)) {
			return ModItems.GLOWCAP;
		}
		boolean plant = state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)
			|| state.is(BlockTags.FLOWERS) || state.is(Blocks.SHORT_DRY_GRASS) || state.is(Blocks.TALL_DRY_GRASS) || state.is(Blocks.BUSH);
		if (!plant) {
			return null;
		}
		if (pos.getY() < level.getSeaLevel() - 20 && !level.canSeeSky(pos)) {
			return ModItems.GLOWCAP;
		}
		if (level.getBiome(pos).value().coldEnoughToSnow(pos, level.getSeaLevel())) {
			return ModItems.FROSTROOT;
		}
		if (state.is(BlockTags.FLOWERS) && level.isDarkOutside()) {
			return ModItems.MOONPETAL;
		}
		return ModItems.SUNLEAF;
	}

	private static void gather(final ServerPlayer player, final ServerLevel level, final BlockPos pos, final BlockState state) {
		Item herb = herbFor(level, pos, state);
		if (herb == null) {
			return;
		}
		int chance = BASE_CHANCE + Profession.ALCHEMY.level(player) / 2;
		if (player.getRandom().nextInt(100) >= chance) {
			return;
		}
		net.minecraft.world.level.block.Block.popResource(level, pos, new ItemStack(herb, 1 + (player.getRandom().nextInt(100) < Profession.ALCHEMY.doubleChance(player) ? 1 : 0)));
		Profession.ALCHEMY.addExp(player, 1);
	}

	private Herbs() {
	}
}
