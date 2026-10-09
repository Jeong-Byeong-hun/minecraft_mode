package com.minecraftmode.job.weapon;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.engrave.EngraveStat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.level.Level;

/**
 * A class weapon item. Melee stats come from its {@link WeaponDef}; shooters fire on right click
 * and bows are drawn like vanilla bows (no ammo). Unbreakable and not enchantable: class weapons
 * are improved with engravings instead.
 */
public class JobWeaponItem extends Item {
	/** Tier 1..4 name colors: green, blue, purple, orange. */
	private static final int[] TIER_COLORS = {0x9EE493, 0x5BC0FF, 0xC77DFF, 0xFFB347};
	public static final Identifier REACH_ID = MinecraftMode.id("weapon_reach");

	private final WeaponDef def;

	public JobWeaponItem(final Properties properties, final WeaponDef def) {
		super(properties);
		this.def = def;
	}

	public WeaponDef def() {
		return this.def;
	}

	static Properties properties(final WeaponDef def) {
		Archetype type = def.archetype();
		ItemAttributeModifiers.Builder attributes = ItemAttributeModifiers.builder()
			.add(
				Attributes.ATTACK_DAMAGE,
				new AttributeModifier(BASE_ATTACK_DAMAGE_ID, def.attackDamage() - 1.0, AttributeModifier.Operation.ADD_VALUE),
				EquipmentSlotGroup.MAINHAND
			)
			.add(
				Attributes.ATTACK_SPEED,
				new AttributeModifier(BASE_ATTACK_SPEED_ID, type.attackSpeed() - 4.0, AttributeModifier.Operation.ADD_VALUE),
				EquipmentSlotGroup.MAINHAND
			);
		if (type.reach() != 0.0F) {
			attributes.add(
				Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(REACH_ID, type.reach(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND
			);
		}
		Properties properties = new Properties()
			.stacksTo(1)
			.rarity(def.tier() >= 3 ? Rarity.EPIC : def.tier() == 2 ? Rarity.RARE : Rarity.UNCOMMON)
			.attributes(attributes.build())
			.component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
			.component(DataComponents.WEAPON, new Weapon(0));
		if (type.stabs()) {
			properties.component(DataComponents.ATTACK_ANIMATION, new SwingAnimation(SwingAnimationType.STAB, 8));
		}
		return properties;
	}

	public static int tierColor(final int tier) {
		return TIER_COLORS[Math.max(0, Math.min(3, tier - 1))];
	}

	@Override
	public Component getName(final ItemStack itemStack) {
		return Component.translatable(this.getDescriptionId()).withColor(tierColor(this.def.tier()));
	}

	@Override
	public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		switch (this.def.archetype().mode()) {
			case SHOOT -> {
				if (player instanceof ServerPlayer serverPlayer) {
					BasicAttacks.shoot(serverPlayer, stack, this.def);
				}
				player.getCooldowns().addCooldown(stack, shotCooldown(player, this.def));
				return InteractionResult.SUCCESS;
			}
			case DRAW -> {
				player.startUsingItem(hand);
				return InteractionResult.CONSUME;
			}
			default -> {
				return InteractionResult.PASS;
			}
		}
	}

	@Override
	public int getUseDuration(final ItemStack itemStack, final LivingEntity user) {
		return this.def.archetype().mode() == Archetype.BasicMode.DRAW ? 72000 : 0;
	}

	@Override
	public ItemUseAnimation getUseAnimation(final ItemStack itemStack) {
		return this.def.archetype().mode() == Archetype.BasicMode.DRAW ? ItemUseAnimation.BOW : ItemUseAnimation.NONE;
	}

	@Override
	public boolean releaseUsing(final ItemStack itemStack, final Level level, final LivingEntity entity, final int remainingTime) {
		if (this.def.archetype().mode() != Archetype.BasicMode.DRAW || !(entity instanceof Player player)) {
			return false;
		}
		int held = this.getUseDuration(itemStack, entity) - remainingTime;
		float power = Math.min(1.0F, held / (float)drawTicks(player, this.def));
		if (power < 0.2F) {
			return false;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			BasicAttacks.loose(serverPlayer, itemStack, this.def, power);
		}
		return true;
	}

	/** Shot cooldown after Quick Draw / Quick Reload. */
	public static int shotCooldown(final Player player, final WeaponDef def) {
		return Math.max(4, Math.round(def.archetype().shotCooldown() / (1.0F + speedBonus(player, def))));
	}

	public static int drawTicks(final Player player, final WeaponDef def) {
		return Math.max(5, Math.round(def.archetype().shotCooldown() / (1.0F + speedBonus(player, def))));
	}

	private static float speedBonus(final Player player, final WeaponDef def) {
		if (!JobWeapons.isActive(JobProgression.get(player), def)) {
			return 0.0F;
		}
		return JobWeapons.activeTotals(player).fraction(EngraveStat.DRAW_SPEED);
	}
}
