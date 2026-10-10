package com.minecraftmode.worldgen.lair;

import com.minecraftmode.bag.Bags;
import com.minecraftmode.companion.Companions;
import com.minecraftmode.progress.Progress;
import com.minecraftmode.progress.ResetCycle;
import com.minecraftmode.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A lair's treasure chest or supply cache. Unbreakable; every player opens their own contents, renewed
 * every {@link ResetCycle} cycle. The treasure stays sealed while the lair's lord lives.
 */
public class LairChestBlock extends BaseEntityBlock {
	private final boolean cache;

	public LairChestBlock(final boolean cache, final Properties properties) {
		super(properties);
		this.cache = cache;
	}

	public boolean isCache() {
		return this.cache;
	}

	@Override
	public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
		return new LairChestBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(final BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.LAIR_CHEST, LairChestBlockEntity::serverTick);
	}

	@Override
	protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(level.getBlockEntity(pos) instanceof LairChestBlockEntity chest) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}
		open(serverPlayer, chest);
		return InteractionResult.SUCCESS;
	}

	/** A plain chest menu over the player's contents, except that shift-clicking loot out fills the bags that take it first, like a pickup. */
	private static ChestMenu menu(final int id, final Inventory inventory, final Container container) {
		return new ChestMenu(MenuType.GENERIC_9x3, id, inventory, container, 3) {
			@Override
			public ItemStack quickMoveStack(final Player player, final int slotIndex) {
				if (slotIndex < container.getContainerSize()) {
					Slot slot = this.slots.get(slotIndex);
					ItemStack stack = slot.getItem();
					if (!stack.isEmpty() && Bags.absorb(player, stack) > 0) {
						slot.setChanged();
						if (stack.isEmpty()) {
							slot.setByPlayer(ItemStack.EMPTY);
							return ItemStack.EMPTY;
						}
					}
				}
				return super.quickMoveStack(player, slotIndex);
			}
		};
	}

	/** Opens {@code player}'s own contents (or says why not). Returns true when the menu opened. */
	public static boolean open(final ServerPlayer player, final LairChestBlockEntity chest) {
		Level level = player.level();
		LairDef def = chest.def();
		long cycle = ResetCycle.cycle(level);
		if (!chest.victor(player, cycle)) {
			// only those who beat the lord may open it; reaching the chest first (a pearl, a fast run) wakes one now
			if (chest.lord() == null && level instanceof ServerLevel server && !player.isSpectator()) {
				chest.wakeLord(server, cycle);
			}
			player.sendOverlayMessage(Component.translatable("message.minecraft_mode.lair.sealed",
				def == null ? Component.empty() : Component.translatable(def.named().nameKey())).withStyle(ChatFormatting.RED));
			level.playSound(null, chest.getBlockPos(), SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 1.0F);
			return false;
		}
		boolean fresh = chest.fresh(player, cycle);
		SimpleContainer container = chest.container(player, cycle);
		Component title = Component.translatable(chest.isCache() ? "container.minecraft_mode.lair_cache" : "container.minecraft_mode.lair_chest");
		player.openMenu(new SimpleMenuProvider((id, inventory, p) -> menu(id, inventory, container), title));
		level.playSound(null, chest.getBlockPos(), chest.isCache() ? SoundEvents.BARREL_OPEN : SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.8F, 1.0F);
		if (fresh) {
			player.sendSystemMessage(Component.translatable("message.minecraft_mode.lair.personal", ResetCycle.remaining(ResetCycle.ticksToNextCycle(level)))
				.withStyle(ChatFormatting.GOLD));
			if (!chest.isCache() && def != null) {
				boolean firstClear = Progress.get(player).lairClears(def.id()) == 0;
				Progress.lairCleared(player, def);
				LairExp.chestOpened(player, def, false, firstClear);
				Companions.rollDrop(player, Companions.LAIR_DROP, Rarity.RARE);
			} else if (def != null) {
				LairExp.chestOpened(player, def, true, false);
			}
		}
		return true;
	}
}
