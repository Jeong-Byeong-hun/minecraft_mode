package com.minecraftmode.enchantment;

import com.minecraftmode.registry.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Code-driven axe enchantments: Cleave, Plunder and Guillotine. */
public final class CombatEnchantmentHandlers {
	private static final double CLEAVE_RADIUS = 2.5;
	private static final float CLEAVE_RATIO_PER_LEVEL = 0.2F;
	/** Cleave damage must not cleave again. */
	private static boolean cleaving;

	public static void init() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> cleave(entity, source, damageTaken));
		ServerLivingEntityEvents.AFTER_DEATH.register(CombatEnchantmentHandlers::onDeath);
	}

	private static void cleave(final LivingEntity victim, final DamageSource source, final float damageTaken) {
		if (cleaving || damageTaken <= 0 || !(victim.level() instanceof ServerLevel level)) {
			return;
		}
		// Melee only: the attacker itself dealt the hit
		if (!(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() != attacker) {
			return;
		}
		int cleave = EnchantLevels.get(level, WeaponEnchantments.CLEAVE, attacker.getMainHandItem());
		if (cleave <= 0) {
			return;
		}
		float splash = damageTaken * CLEAVE_RATIO_PER_LEVEL * cleave;
		cleaving = true;
		try {
			for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(CLEAVE_RADIUS), e -> isCleaveTarget(e, victim, attacker))) {
				other.hurtServer(level, source, splash);
			}
		} finally {
			cleaving = false;
		}
	}

	private static boolean isCleaveTarget(final LivingEntity entity, final LivingEntity victim, final LivingEntity attacker) {
		if (entity == victim || entity == attacker || !entity.isAlive() || entity instanceof Player || entity.isAlliedTo(attacker)) {
			return false;
		}
		// Never hit the attacker's pets
		return !(entity instanceof OwnableEntity pet && pet.getOwner() == attacker);
	}

	private static void onDeath(final LivingEntity entity, final DamageSource source) {
		if (!(entity.level() instanceof ServerLevel level) || !(source.getEntity() instanceof Player player)) {
			return;
		}
		ItemStack weapon = player.getMainHandItem();

		int plunder = EnchantLevels.get(level, WeaponEnchantments.PLUNDER, weapon);
		if (plunder > 0 && entity instanceof Enemy) {
			int coins = entity.getRandom().nextInt(plunder + 1);
			if (coins > 0) {
				entity.spawnAtLocation(level, new ItemStack(ModItems.COPPER_COIN, coins));
			}
		}

		int guillotine = EnchantLevels.get(level, WeaponEnchantments.GUILLOTINE, weapon);
		if (guillotine > 0) {
			ItemStack head = headOf(entity);
			float chance = (entity.getType() == EntityTypes.WITHER_SKELETON ? 0.02F : 0.05F) * guillotine;
			if (!head.isEmpty() && entity.getRandom().nextFloat() < chance) {
				entity.spawnAtLocation(level, head);
			}
		}
	}

	private static ItemStack headOf(final LivingEntity entity) {
		var type = entity.getType();
		if (type == EntityTypes.ZOMBIE) {
			return new ItemStack(Items.ZOMBIE_HEAD);
		} else if (type == EntityTypes.SKELETON) {
			return new ItemStack(Items.SKELETON_SKULL);
		} else if (type == EntityTypes.CREEPER) {
			return new ItemStack(Items.CREEPER_HEAD);
		} else if (type == EntityTypes.PIGLIN) {
			return new ItemStack(Items.PIGLIN_HEAD);
		} else if (type == EntityTypes.WITHER_SKELETON) {
			return new ItemStack(Items.WITHER_SKELETON_SKULL);
		}
		return ItemStack.EMPTY;
	}

	private CombatEnchantmentHandlers() {
	}
}
