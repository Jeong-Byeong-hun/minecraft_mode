package com.minecraftmode.worldgen.lair;

import com.minecraftmode.network.MapMarkPayload;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jspecify.annotations.Nullable;

/**
 * Read it to find the nearest named lair (sold at the general store and the merit shop): the lair becomes a waypoint on the player's
 * map (M) and minimap, its name and coordinates go to chat, and the item turns into an explorer map of the place.
 */
public class LairMapItem extends Item {
	/** Search radius in chunks. */
	public static final int SEARCH = 64;
	/** Waypoint colour of a lair (orange). */
	public static final int MARK_COLOR = 0xFFFF9020;

	public LairMapItem(final Properties properties) {
		super(properties);
	}

	/** The lair found from a spot: which one and its middle (at the surface). */
	public record Found(LairDef lair, BlockPos pos) {
	}

	@Override
	public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
		if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}
		Found found = find(server, player.blockPosition());
		if (found == null) {
			serverPlayer.sendOverlayMessage(Component.translatable("message.minecraft_mode.lair.no_map").withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}
		ItemStack held = player.getItemInHand(hand);
		if (!player.isCreative()) {
			held.shrink(1);
		}
		player.getInventory().placeItemBackInInventory(map(server, found), net.minecraft.util.Prediction.SERVER_ONLY);
		mark(serverPlayer, server, found);
		server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.0F);
		return InteractionResult.SUCCESS;
	}

	/** The nearest lair from {@code from}, or null when none is within {@link #SEARCH} chunks. */
	public static @Nullable Found find(final ServerLevel level, final BlockPos from) {
		var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		List<Holder<Structure>> lairs = NamedLairs.all().stream().<Holder<Structure>>map(d -> registry.getOrThrow(d.key())).toList();
		Pair<BlockPos, Holder<Structure>> nearest = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(lairs), from, SEARCH,
			false);
		if (nearest == null || !(nearest.getSecond().value() instanceof LairStructure structure)) {
			return null;
		}
		LairDef lair = NamedLairs.def(structure.lair());
		if (lair == null) {
			return null;
		}
		BlockPos at = nearest.getFirst().offset(8, 0, 8);
		int y = level.getChunkSource().getGenerator().getBaseHeight(at.getX(), at.getZ(), Heightmap.Types.WORLD_SURFACE_WG, level,
			level.getChunkSource().randomState());
		return new Found(lair, new BlockPos(at.getX(), y, at.getZ()));
	}

	/** An explorer map marking {@code found}. */
	private static ItemStack map(final ServerLevel level, final Found found) {
		BlockPos at = found.pos();
		ItemStack map = MapItem.create(level, at.getX(), at.getZ(), (byte)2, true, true);
		MapItem.renderBiomePreviewMap(level, map);
		MapItemSavedData.addTargetDecoration(map, at, "+", MapDecorationTypes.RED_X);
		map.set(DataComponents.ITEM_NAME, Component.translatable("item.minecraft_mode.lair_map.filled", at.getX(), at.getZ()));
		return map;
	}

	/** Names the lair and its coordinates in chat and puts it on the player's map as a waypoint. */
	public static void mark(final ServerPlayer player, final ServerLevel level, final Found found) {
		LairDef lair = found.lair();
		BlockPos at = found.pos();
		Component name = Component.translatable(lair.nameKey());
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.lair.map_found", name.copy().withStyle(ChatFormatting.GOLD),
			Component.translatable(lair.named().nameKey()), lair.named().lo(), lair.named().hi(), at.getX(), at.getZ()).withStyle(ChatFormatting.YELLOW));
		ServerPlayNetworking.send(player, new MapMarkPayload(level.dimension().identifier().toString(), at, name, MARK_COLOR));
	}
}
