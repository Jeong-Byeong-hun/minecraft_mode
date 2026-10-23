package com.minecraftmode.worldgen.lair;

import com.minecraftmode.network.MapMarkPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Read it to find a named lair for your level (sold at the general store and the merit shop, made by cartographers, and handed out by every
 * lair cleared): the lair becomes a waypoint on the player's map (M) and minimap, its name and coordinates go to chat, and a confirm screen
 * offers the trip to its gate, which uses the map up ({@link LairTravel}). A map with a tier ({@link LairTier}, given to Lv 100 characters
 * from 0.5.49) opens a tier lair of its own instead ({@link TierLairs}).
 */
public class LairMapItem extends Item {
	/** Waypoint colour of a lair (orange). */
	public static final int MARK_COLOR = 0xFFFF9020;

	/** Ticks before a lair map can be read again. */
	public static final int USE_COOLDOWN = 40;

	public LairMapItem(final Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
		if (!(level instanceof ServerLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}
		// the search may generate structure starts on the server thread: holding use down must not repeat it every few ticks
		serverPlayer.getCooldowns().addCooldown(player.getItemInHand(hand), USE_COOLDOWN);
		// a tier map opens a tier lair of its own instead of leading to one in the world (0.5.49)
		int tier = LairTier.of(player.getItemInHand(hand));
		if (tier > 0) {
			return TierLairs.offer(serverPlayer, tier) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
		}
		return LairTravel.offer(serverPlayer) == null ? InteractionResult.FAIL : InteractionResult.SUCCESS;
	}

	@Override
	public Component getName(final ItemStack stack) {
		int tier = LairTier.of(stack);
		return tier > 0 ? Component.translatable("item.minecraft_mode.lair_map.tiered", tier) : super.getName(stack);
	}

	/** Names the lair and its coordinates in chat and puts it on the player's map as a waypoint at {@code at}. */
	public static void mark(final ServerPlayer player, final ServerLevel level, final LairDef lair, final BlockPos at) {
		Component name = Component.translatable(lair.nameKey());
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.lair.map_found", name.copy().withStyle(ChatFormatting.GOLD),
			Component.translatable(lair.named().nameKey()), lair.named().lo(), lair.named().hi(), at.getX(), at.getZ()).withStyle(ChatFormatting.YELLOW));
		ServerPlayNetworking.send(player, new MapMarkPayload(level.dimension().identifier().toString(), at, name, MARK_COLOR));
	}
}
