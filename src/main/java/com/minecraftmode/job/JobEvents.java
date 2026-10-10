package com.minecraftmode.job;

import com.minecraftmode.city.TrainingGrounds;
import com.minecraftmode.enchantment.EnchantLevels;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.skill.Actions;
import com.minecraftmode.job.skill.CombatHooks;
import com.minecraftmode.job.skill.CombatState;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.progress.Contribution;
import com.minecraftmode.progress.Progress;
import java.util.List;
import com.minecraftmode.registry.ModBlocks;
import com.minecraftmode.registry.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Wires classes into the game: job experience and essence from kills and ores, MP regeneration,
 * stat refresh, death penalty, Avalon, and cleanup of summons.
 */
public final class JobEvents {
	/** Bosses (this much max health or more) drop condensed essence and give double experience. */
	private static final float BOSS_HEALTH = 100.0F;
	/** Cooldown key of Avalon in {@link JobData#cooldowns()}. */
	public static final String AVALON_COOLDOWN = "passive.avalon";

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(JobEvents::tick);
		ServerLivingEntityEvents.AFTER_DEATH.register(JobEvents::afterDeath);
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> !(entity instanceof ServerPlayer player) || allowDeath(player, source, amount));
		PlayerBlockBreakEvents.AFTER.register(JobEvents::afterBlockBreak);
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			JobStats.refresh(newPlayer);
			JobProgression.set(newPlayer, JobProgression.get(newPlayer).withMana(JobStats.maxMana(newPlayer)));
		});
		ServerPlayerEvents.JOIN.register(JobStats::refresh);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> CombatState.forget(handler.player));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> CombatState.clear());
		// players never drop their items on death (the job exp penalty still applies outside raids)
		ServerLifecycleEvents.SERVER_STARTED.register(server -> server.overworld().getGameRules().set(GameRules.KEEP_INVENTORY, true, server));
		// Summons are temporary; drop any that were saved with a chunk.
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity.entityTags().contains(Actions.SUMMON_TAG)) {
				entity.discard();
			}
		});
	}

	private static void tick(final MinecraftServer server) {
		int tick = server.getTickCount();
		if (tick % 10 != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			JobStats.tick(player, tick);
		}
	}

	// ------------------------------------------------------------------ kills

	private static void afterDeath(final LivingEntity entity, final DamageSource source) {
		if (entity instanceof ServerPlayer player) {
			JobProgression.applyDeathPenalty(player);
			return;
		}
		if (!(entity.level() instanceof ServerLevel level) || !(entity instanceof Enemy)) {
			return;
		}
		// everyone who hit it shares the experience by damage dealt (Contribution); the top contributor rolls the drops below
		List<Contribution.Share> shares = Contribution.shares(entity, source);
		if (shares.isEmpty()) {
			return;
		}
		ServerPlayer mvp = shares.getFirst().player();
		float maxHealth = entity.getMaxHealth();
		boolean boss = maxHealth >= BOSS_HEALTH;
		int solo = Math.max(3, Math.round(maxHealth * (boss ? 2 : 1)));
		for (int i = 0; i < shares.size(); i++) {
			Contribution.Share share = shares.get(i);
			int exp = Contribution.portion(solo, share, shares.size()) * TrainingGrounds.expMultiplier(entity, share.player());
			gainExp(share.player(), exp);
			if (shares.size() > 1) {
				share.player().sendOverlayMessage(Component.translatable("message.minecraft_mode.contribution.exp", Math.round(share.fraction() * 100.0F), i + 1, shares.size(), exp)
					.withStyle(i == 0 ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
			}
		}
		if (source.getEntity() instanceof ServerPlayer killer) {
			onKill(killer);
		}

		// Essence: monsters drop it at random, bosses always drop condensed essence
		if (boss) {
			drop(level, entity, new ItemStack(ModItems.CONDENSED_ESSENCE, 1 + (int)(maxHealth / 150.0F)));
		} else if (mvp.getRandom().nextFloat() < Math.min(0.6F, 0.06F + maxHealth * 0.0025F)) {
			drop(level, entity, new ItemStack(ModItems.ESSENCE));
		}

		// Coins: pirate passive and the Plunder engraving
		JobData data = JobProgression.get(mvp);
		float coinChance = (CombatHooks.has(data, JobClass.PIRATE, 1) ? 0.15F : 0.0F) + JobWeapons.activeTotals(mvp).fraction(EngraveStat.GOLD_FIND);
		if (coinChance > 0.0F && mvp.getRandom().nextFloat() < coinChance) {
			int copper = 1 + (int)(maxHealth / 20.0F);
			drop(level, entity, copper >= 9 ? new ItemStack(ModItems.SILVER_COIN, copper / 9) : new ItemStack(ModItems.COPPER_COIN, copper));
			level.sendParticles(ParticleTypes.WAX_ON, entity.getX(), entity.getY(0.5), entity.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
		}
	}

	/** Class EXP with the EXP bonus of the player's gear. */
	public static void gainExp(final ServerPlayer player, final int amount) {
		float bonus = JobWeapons.activeTotals(player).fraction(EngraveStat.EXP_BONUS);
		JobProgression.addExp(player, Math.round(amount * (1.0F + bonus)));
	}

	/** Kill procs from gear: heal, MP, stealth and swiftness. */
	private static void onKill(final ServerPlayer killer) {
		var mods = JobWeapons.activeTotals(killer);
		float heal = mods.get(EngraveStat.KILL_HEAL);
		if (heal > 0.0F) {
			killer.heal(heal);
		}
		int mana = JobStats.randomRound(mods.get(EngraveStat.KILL_MANA), killer.getRandom());
		if (mana > 0) {
			JobStats.addMana(killer, mana);
		}
		int stealth = Math.round(mods.get(EngraveStat.STEALTH_ON_KILL) * 20.0F);
		if (stealth > 0) {
			killer.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, stealth, 0, false, false, true));
		}
		int speed = Math.round(mods.get(EngraveStat.SPEED_ON_KILL) * 20.0F);
		if (speed > 0) {
			killer.addEffect(new MobEffectInstance(MobEffects.SPEED, speed, 1, false, false, true));
		}
	}

	private static void drop(final ServerLevel level, final LivingEntity entity, final ItemStack stack) {
		ItemEntity item = entity.spawnAtLocation(level, stack);
		if (item != null) {
			item.setGlowingTag(true);
		}
	}

	/** Avalon (warrior tier 4): survive a lethal blow once every 3 minutes. */
	private static boolean allowDeath(final ServerPlayer player, final DamageSource source, final float amount) {
		JobData data = JobProgression.get(player);
		if (!CombatHooks.has(data, JobClass.WARRIOR, 4)) {
			return true;
		}
		long now = player.level().getGameTime();
		if (now < data.readyAt(AVALON_COOLDOWN)) {
			return true;
		}
		// kept in the saved cooldown map so relogging does not reset it
		JobProgression.set(player, data.withCooldown(AVALON_COOLDOWN, now + 3600));
		player.setHealth(player.getMaxHealth() * 0.3F);
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 3));
		ServerLevel level = player.level();
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY(1.0), player.getZ(), 40, 0.5, 0.8, 0.5, 0.3);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.3F);
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.job.avalon").withStyle(ChatFormatting.GOLD));
		return false;
	}

	// ------------------------------------------------------------------ ores

	private static void afterBlockBreak(final Level level, final Player player, final BlockPos pos, final BlockState state, final Object blockEntity) {
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer) || player.isCreative()) {
			return;
		}
		OreReward reward = oreReward(state);
		if (reward == null || !player.hasCorrectToolForDrops(state)) {
			return;
		}
		// Silk Touch keeps the ore block, which could be placed and mined again forever: like vanilla's ore XP, it earns nothing.
		ItemStack tool = player.getMainHandItem();
		if (EnchantLevels.get(level, Enchantments.SILK_TOUCH, tool) > 0) {
			return;
		}
		gainExp(serverPlayer, reward.exp);
		Progress.oreMined(serverPlayer);
		if (player.getRandom().nextFloat() < reward.essenceChance) {
			ItemEntity item = new ItemEntity(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(ModItems.ESSENCE));
			item.setDefaultPickUpDelay();
			item.setGlowingTag(true);
			serverLevel.addFreshEntity(item);
		}
	}

	private record OreReward(int exp, float essenceChance) {
	}

	/** Every ore gives job experience and sometimes essence; the rarer the ore, the more. */
	private static OreReward oreReward(final BlockState state) {
		if (state.is(BlockItemTags.COAL_ORES.block())) {
			return new OreReward(2, 0.02F);
		}
		if (state.is(BlockItemTags.COPPER_ORES.block())) {
			return new OreReward(3, 0.03F);
		}
		// nether gold is common and only drops nuggets, so it sits below the other gold ores
		if (state.is(Blocks.NETHER_GOLD_ORE)) {
			return new OreReward(3, 0.03F);
		}
		if (state.is(Blocks.NETHER_QUARTZ_ORE)) {
			return new OreReward(4, 0.04F);
		}
		if (state.is(ModBlocks.ALUMINUM_ORE) || state.is(ModBlocks.DEEPSLATE_ALUMINUM_ORE)) {
			return new OreReward(5, 0.06F);
		}
		if (state.is(BlockItemTags.IRON_ORES.block())) {
			return new OreReward(5, 0.06F);
		}
		if (state.is(BlockItemTags.GOLD_ORES.block())) {
			return new OreReward(8, 0.08F);
		}
		if (state.is(BlockItemTags.REDSTONE_ORES.block())) {
			return new OreReward(5, 0.06F);
		}
		if (state.is(BlockItemTags.LAPIS_ORES.block())) {
			return new OreReward(8, 0.08F);
		}
		if (state.is(ModBlocks.MYTHRIL_ORE) || state.is(ModBlocks.DEEPSLATE_MYTHRIL_ORE)) {
			return new OreReward(15, 0.15F);
		}
		if (state.is(BlockItemTags.DIAMOND_ORES.block()) || state.is(BlockItemTags.EMERALD_ORES.block())) {
			return new OreReward(20, 0.30F);
		}
		if (state.is(Blocks.ANCIENT_DEBRIS)) {
			return new OreReward(40, 0.50F);
		}
		return null;
	}

	private JobEvents() {
	}
}
