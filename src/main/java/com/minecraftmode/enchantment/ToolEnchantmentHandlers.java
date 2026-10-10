package com.minecraftmode.enchantment;

import com.minecraftmode.bag.Bags;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * Code-driven tool enchantments: Vein Miner, Excavation, Timber (axe), Momentum Mining, Magnet,
 * Treasure Hunter and Self Repair.
 */
public final class ToolEnchantmentHandlers {
	private static final int SELF_REPAIR_INTERVAL = 40;
	/** Extra blocks broken by these enchantments must not trigger them again. */
	private static boolean breakingExtraBlocks;

	public static void init() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (breakingExtraBlocks || !(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
				return;
			}
			onBlockBroken(serverLevel, serverPlayer, pos, state);
		});
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
			ItemInstance tool = context.getOptional(LootContextParams.TOOL);
			if (state == null || tool == null || !(context.getOptional(LootContextParams.THIS_ENTITY) instanceof ServerPlayer player)) {
				return;
			}
			ServerLevel level = player.level();
			int treasure = EnchantLevels.get(level, ToolEnchantments.TREASURE_HUNTER, tool);
			if (treasure > 0 && state.is(BlockTags.BASE_STONE_OVERWORLD) && context.getRandom().nextFloat() < 0.01F * treasure) {
				drops.add(randomTreasure(context.getRandom()));
			}
			if (EnchantLevels.get(level, ToolEnchantments.MAGNET, tool) > 0) {
				drops.removeIf(stack -> {
					Bags.absorb(player, stack);
					player.getInventory().add(stack);
					return stack.isEmpty();
				});
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(ToolEnchantmentHandlers::selfRepair);
	}

	private static void onBlockBroken(final ServerLevel level, final ServerPlayer player, final BlockPos pos, final BlockState state) {
		ItemStack tool = player.getMainHandItem();
		int momentum = EnchantLevels.get(level, ToolEnchantments.MINING_MOMENTUM, tool);
		if (momentum > 0) {
			player.addEffect(new MobEffectInstance(MobEffects.HASTE, 40, momentum - 1, true, false, true));
		}

		int vein = EnchantLevels.get(level, ToolEnchantments.VEIN_MINER, tool);
		int excavation = EnchantLevels.get(level, ToolEnchantments.EXCAVATION, tool);
		int timber = EnchantLevels.get(level, WeaponEnchantments.TIMBER, tool);
		if (vein == 0 && excavation == 0 && timber == 0) {
			return;
		}

		breakingExtraBlocks = true;
		try {
			if (vein > 0 && state.is(ConventionalBlockTags.ORES)) {
				breakAll(player, connected(level, pos, s -> s.is(state.getBlock()), 4 * vein));
			}
			if (timber > 0 && state.is(BlockTags.LOGS)) {
				breakAll(player, connected(level, pos, s -> s.is(BlockTags.LOGS), 16 * timber));
			}
			if (excavation > 0) {
				breakAll(player, area(level, player, pos, excavation, tool));
			}
		} finally {
			breakingExtraBlocks = false;
		}
	}

	/** Breaks the blocks with the player's tool (durability, drops and enchantments all apply) until the tool breaks. */
	private static void breakAll(final ServerPlayer player, final List<BlockPos> positions) {
		for (BlockPos target : positions) {
			if (player.getMainHandItem().isEmpty()) {
				return;
			}
			player.gameMode.destroyBlock(target);
		}
	}

	/** Blocks touching the start (diagonals included) that match, breadth first, up to {@code limit}; the start itself excluded. */
	static List<BlockPos> connected(final ServerLevel level, final BlockPos start, final Predicate<BlockState> matches, final int limit) {
		List<BlockPos> found = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		Queue<BlockPos> queue = new ArrayDeque<>();
		seen.add(start);
		queue.add(start);
		while (!queue.isEmpty() && found.size() < limit) {
			BlockPos current = queue.poll();
			for (BlockPos next : BlockPos.betweenClosed(current.offset(-1, -1, -1), current.offset(1, 1, 1))) {
				if (found.size() >= limit) {
					break;
				}
				BlockPos immutable = next.immutable();
				if (seen.add(immutable) && matches.test(level.getBlockState(immutable))) {
					found.add(immutable);
					queue.add(immutable);
				}
			}
		}
		return found;
	}

	/** The (2r+1)x(2r+1) square around the broken block, perpendicular to where the player looks. */
	private static List<BlockPos> area(final ServerLevel level, final ServerPlayer player, final BlockPos center, final int radius, final ItemStack tool) {
		Direction face = Direction.getApproximateNearest(player.getLookAngle());
		List<BlockPos> found = new ArrayList<>();
		for (int a = -radius; a <= radius; a++) {
			for (int b = -radius; b <= radius; b++) {
				if (a == 0 && b == 0) {
					continue;
				}
				BlockPos target = switch (face.getAxis()) {
					case Y -> center.offset(a, 0, b);
					case X -> center.offset(0, a, b);
					case Z -> center.offset(a, b, 0);
				};
				BlockState state = level.getBlockState(target);
				if (!state.isAir() && state.getDestroySpeed(level, target) >= 0 && tool.isCorrectToolForDrops(state)) {
					found.add(target);
				}
			}
		}
		return found;
	}

	private static ItemStack randomTreasure(final RandomSource random) {
		float roll = random.nextFloat();
		if (roll < 0.35F) {
			return new ItemStack(Items.GOLD_NUGGET, 2 + random.nextInt(4));
		} else if (roll < 0.6F) {
			return new ItemStack(Items.LAPIS_LAZULI, 2 + random.nextInt(3));
		} else if (roll < 0.8F) {
			return new ItemStack(ModItems.RAW_MYTHRIL);
		} else if (roll < 0.95F) {
			return new ItemStack(Items.EMERALD);
		}
		return new ItemStack(Items.DIAMOND);
	}

	private static void selfRepair(final MinecraftServer server) {
		if (server.getTickCount() % SELF_REPAIR_INTERVAL != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			var inventory = player.getInventory();
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				ItemStack stack = inventory.getItem(slot);
				if (!stack.isDamaged()) {
					continue;
				}
				int level = EnchantLevels.get(player.level(), ToolEnchantments.SELF_REPAIR, stack);
				if (level > 0) {
					stack.setDamageValue(Math.max(0, stack.getDamageValue() - level));
				}
			}
		}
	}

	private ToolEnchantmentHandlers() {
	}
}
