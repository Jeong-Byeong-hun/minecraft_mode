package com.minecraftmode.light;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A light source held in either hand (torches, lanterns, glowstone, a lava bucket...) lights the player's surroundings as if it
 * were placed: the server keeps one invisible {@code minecraft:light} block at the player's head (or feet) and moves it with
 * them, so every client - and other players - see the light. The block only ever replaces air or water and is removed as soon
 * as the player moves, puts the item away, dies, changes dimension or logs off.
 */
public final class HeldLight {
	private record Placed(ResourceKey<Level> dimension, BlockPos pos, int level) {
	}

	private static final Map<UUID, Placed> PLACED = new HashMap<>();

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(HeldLight::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> remove(server, handler.player.getUUID()));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (UUID id : PLACED.keySet().toArray(UUID[]::new)) {
				remove(server, id);
			}
		});
	}

	/** The light level the held items give, 0 when none. */
	public static int heldLevel(final ServerPlayer player) {
		return Math.max(level(player.getItemInHand(InteractionHand.OFF_HAND)), level(player.getItemInHand(InteractionHand.MAIN_HAND)));
	}

	public static int level(final ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}
		if (stack.is(Items.LAVA_BUCKET)) {
			return 15;
		}
		if (stack.getItem() instanceof BlockItem item) {
			return item.getBlock().defaultBlockState().getLightEmission();
		}
		return 0;
	}

	private static void tick(final MinecraftServer server) {
		if (server.getTickCount() % 2 != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			int level = player.isAlive() && !player.isSpectator() ? heldLevel(player) : 0;
			BlockPos at = level > 0 ? spot(player) : null;
			Placed current = PLACED.get(player.getUUID());
			if (at == null) {
				if (current != null) {
					remove(server, player.getUUID());
				}
				continue;
			}
			Placed wanted = new Placed(player.level().dimension(), at, level);
			if (wanted.equals(current)) {
				continue;
			}
			if (current != null) {
				remove(server, player.getUUID());
			}
			BlockState state = player.level().getBlockState(at);
			BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, level).setValue(LightBlock.WATERLOGGED, state.is(Blocks.WATER));
			if (player.level().setBlock(at, light, Block.UPDATE_CLIENTS)) {
				PLACED.put(player.getUUID(), wanted);
			}
		}
	}

	/** The head block when it is free (air or water), else the feet block, else nothing. */
	private static @Nullable BlockPos spot(final ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos head = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
		if (free(level, head)) {
			return head;
		}
		BlockPos feet = player.blockPosition();
		return free(level, feet) ? feet : null;
	}

	private static boolean free(final ServerLevel level, final BlockPos pos) {
		if (!level.isLoaded(pos)) {
			return false;
		}
		BlockState state = level.getBlockState(pos);
		return state.isAir() || state.is(Blocks.WATER) && state.getFluidState().isSource() || state.is(Blocks.LIGHT);
	}

	private static void remove(final MinecraftServer server, final UUID id) {
		Placed placed = PLACED.remove(id);
		if (placed == null) {
			return;
		}
		for (Placed other : PLACED.values()) {
			if (other.dimension.equals(placed.dimension) && other.pos.equals(placed.pos)) {
				return; // someone else's light sits in the same block
			}
		}
		ServerLevel level = server.getLevel(placed.dimension);
		if (level == null || !level.isLoaded(placed.pos)) {
			return;
		}
		BlockState state = level.getBlockState(placed.pos);
		if (state.is(Blocks.LIGHT)) {
			level.setBlock(placed.pos, state.getValue(LightBlock.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	private HeldLight() {
	}
}
