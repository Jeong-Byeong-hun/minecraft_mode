package com.minecraftmode.worldgen.lair;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/** Read it to turn it into an explorer map of the nearest named lair (sold at the general store and the merit shop). */
public class LairMapItem extends Item {
	/** Search radius in chunks. */
	public static final int SEARCH = 64;

	public LairMapItem(final Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
		if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}
		ItemStack map = locate(server, player.blockPosition());
		if (map.isEmpty()) {
			serverPlayer.sendOverlayMessage(Component.translatable("message.minecraft_mode.lair.no_map").withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}
		ItemStack held = player.getItemInHand(hand);
		if (!player.isCreative()) {
			held.shrink(1);
		}
		player.getInventory().placeItemBackInInventory(map, net.minecraft.util.Prediction.SERVER_ONLY);
		server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.0F);
		return InteractionResult.SUCCESS;
	}

	/** An explorer map marking the nearest lair from {@code from}, or empty when none is in range. */
	public static ItemStack locate(final ServerLevel level, final BlockPos from) {
		var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		List<Holder<Structure>> lairs = NamedLairs.all().stream().<Holder<Structure>>map(d -> registry.getOrThrow(d.key())).toList();
		Pair<BlockPos, Holder<Structure>> nearest = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(lairs), from, SEARCH,
			false);
		if (nearest == null) {
			return ItemStack.EMPTY;
		}
		BlockPos at = nearest.getFirst().offset(8, 0, 8);
		ItemStack map = MapItem.create(level, at.getX(), at.getZ(), (byte)2, true, true);
		MapItem.renderBiomePreviewMap(level, map);
		MapItemSavedData.addTargetDecoration(map, at, "+", MapDecorationTypes.RED_X);
		map.set(DataComponents.ITEM_NAME, Component.translatable("item.minecraft_mode.lair_map.filled", at.getX(), at.getZ()));
		return map;
	}
}
