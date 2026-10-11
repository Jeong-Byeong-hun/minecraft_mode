package com.minecraftmode.entity.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Armor on players, without vanilla's cliff. Vanilla counts {@code armor - damage / (2 + toughness / 4)} (at most 20, at least a fifth
 * of the armor) and takes 4% per point: a straight line from the best case down to the floor. A hit that grows past where that line
 * leaves the 20-point plateau loses armor so fast that the damage taken grows with the square of the hit, and the mod's scaled monsters
 * hit right in that band: a few keystone levels or a little less gear took a maxed player's melee hit from 21 to 64 and a typical one's
 * from 78 to 132.
 *
 * <p>Here the counted armor starts down the same slope as vanilla (small hits lose the same) but bends towards the floor instead of
 * reaching it: {@code armor x (0.2 + 0.8 x s / (s + damage))} with {@code s = 0.8 x armor x (2 + toughness / 4)}, still at most 20.
 * Monsters keep vanilla armor.
 */
public final class PlayerArmor {
	/** Vanilla: at most 20 points count, each takes 4%. */
	public static final float MAX_ARMOR = 20.0F;
	public static final float PER_POINT = 1.0F / 25.0F;
	/** Vanilla's floor: a fifth of the armor always counts. */
	public static final float FLOOR = 0.2F;

	/** The armor points that count against a hit of {@code damage}. */
	public static float counted(final float damage, final float armor, final float toughness) {
		if (armor <= 0.0F) {
			return 0.0F;
		}
		float scale = (1.0F - FLOOR) * armor * (2.0F + Math.max(0.0F, toughness) / 4.0F);
		float counted = armor * (FLOOR + (1.0F - FLOOR) * scale / (scale + Math.max(0.0F, damage)));
		return Math.min(MAX_ARMOR, counted);
	}

	/** The share of a hit of {@code damage} that armor takes. */
	public static float fraction(final float damage, final float armor, final float toughness) {
		return counted(damage, armor, toughness) * PER_POINT;
	}

	/** What is left of {@code damage} after armor, like vanilla's {@code CombatRules.getDamageAfterAbsorb} (armor-breaching enchantments too). */
	public static float afterAbsorb(final LivingEntity victim, final float damage, final DamageSource source, final float armor, final float toughness) {
		float fraction = fraction(damage, armor, toughness);
		ItemStack weapon = source.getWeaponItem();
		if (weapon != null && victim.level() instanceof ServerLevel level) {
			fraction = Mth.clamp(EnchantmentHelper.modifyArmorEffectiveness(level, weapon, victim, source, fraction), 0.0F, 1.0F);
		}
		return damage * (1.0F - fraction);
	}

	private PlayerArmor() {
	}
}
