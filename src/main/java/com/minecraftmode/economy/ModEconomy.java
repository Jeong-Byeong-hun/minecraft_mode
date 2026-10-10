package com.minecraftmode.economy;

import com.minecraftmode.dungeon.DungeonDimension;
import com.minecraftmode.enchantment.ModEnchantments;
import com.minecraftmode.entity.MobPower;
import com.minecraftmode.entity.boss.RaidBoss;
import com.minecraftmode.entity.named.NamedMob;
import com.minecraftmode.event.WorldEvents;
import com.minecraftmode.progress.Contribution;
import com.minecraftmode.raid.RaidDimension;
import com.minecraftmode.registry.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Coin sources: hostile mobs killed by players (see {@link Contribution}), and ores mined with Coin Finder.
 */
public final class ModEconomy {
	private static final float COIN_DROP_CHANCE = 0.5F;
	private static final float COIN_FINDER_CHANCE_PER_LEVEL = 0.12F;

	public static void init() {
		ServerLivingEntityEvents.AFTER_DEATH.register(ModEconomy::dropMobCoins);
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> dropOreCoins(level, player, pos, state));
	}

	private static void dropMobCoins(final LivingEntity entity, final DamageSource source) {
		if (!(entity.level() instanceof ServerLevel level) || !(entity instanceof Enemy) || Contribution.shares(entity, source).isEmpty()) {
			return;
		}
		// Only ordinary mobs: the health thresholds below would pay gold for every health-scaled dungeon, invasion or event mob,
		// and named monsters, raid bosses, lairs and instances have their own coin rewards.
		if (entity instanceof NamedMob || entity instanceof RaidBoss || MobPower.has(entity.getUUID()) || entity.entityTags().contains(WorldEvents.INVADER_TAG)
			|| RaidDimension.is(level) || DungeonDimension.is(level)) {
			return;
		}

		RandomSource random = entity.getRandom();
		float maxHealth = entity.getMaxHealth();
		if (maxHealth >= 200.0F) {
			// Bosses (Wither, Ender Dragon, ...)
			entity.spawnAtLocation(level, new ItemStack(ModItems.GOLD_COIN, 1 + random.nextInt(3)));
		} else if (maxHealth >= 50.0F) {
			entity.spawnAtLocation(level, new ItemStack(ModItems.SILVER_COIN, 1 + random.nextInt(2)));
		} else if (random.nextFloat() < COIN_DROP_CHANCE) {
			entity.spawnAtLocation(level, new ItemStack(ModItems.COPPER_COIN, 1 + random.nextInt(2)));
		}
	}

	private static void dropOreCoins(final Level level, final Player player, final BlockPos pos, final BlockState state) {
		if (!(level instanceof ServerLevel) || !state.is(ConventionalBlockTags.ORES)) {
			return;
		}

		int enchantLevel = level.registryAccess()
			.get(ModEnchantments.COIN_FINDER)
			.map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, player.getMainHandItem()))
			.orElse(0);
		if (enchantLevel > 0 && level.getRandom().nextFloat() < COIN_FINDER_CHANCE_PER_LEVEL * enchantLevel) {
			Block.popResource(level, pos, new ItemStack(ModItems.COPPER_COIN, 1 + level.getRandom().nextInt(enchantLevel)));
		}
	}

	private ModEconomy() {
	}
}
