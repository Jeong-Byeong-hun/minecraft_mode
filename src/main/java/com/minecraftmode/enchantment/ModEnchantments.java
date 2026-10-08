package com.minecraftmode.enchantment;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.registry.ModEffects;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.advancements.predicates.DamageSourcePredicate;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.ApplyMobEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;
import net.minecraft.world.level.storage.loot.predicates.DamageSourceCondition;

/**
 * Enchantments are data-driven: this bootstrap is run by datagen to write the JSON files.
 * Coin Finder has no data effect; {@code ModEconomy} reads its level in code.
 * Auto Smelt is applied through block loot tables by {@link AutoSmeltLoot}.
 * The per-equipment sets live in {@link WeaponEnchantments}, {@link RangedEnchantments},
 * {@link ToolEnchantments} and {@link ArmorEnchantments}.
 */
public final class ModEnchantments {
	public static final ResourceKey<Enchantment> LIFESTEAL = key("lifesteal");
	public static final ResourceKey<Enchantment> BLEEDING_EDGE = key("bleeding_edge");
	public static final ResourceKey<Enchantment> COIN_FINDER = key("coin_finder");
	public static final ResourceKey<Enchantment> AUTO_SMELT = key("auto_smelt");
	public static final ResourceKey<Enchantment> SWIFT_STEP = key("swift_step");

	/** The first five enchantments of the mod (sharp weapons, mining tools, boots). */
	public static final List<EnchantInfo> ORIGINAL = List.of(
		new EnchantInfo(LIFESTEAL, "Lifesteal", "흡혈",
			"Melee hits grant Regeneration II for 1.5-3.5 seconds.", "근접 공격 시 1.5~3.5초 동안 재생 II를 얻습니다."),
		new EnchantInfo(BLEEDING_EDGE, "Bleeding Edge", "출혈의 칼날",
			"Melee hits make the target bleed for 3-7 seconds.", "근접 공격 시 대상을 3~7초 동안 출혈시킵니다."),
		new EnchantInfo(COIN_FINDER, "Coin Finder", "동전 탐색",
			"Mining an ore has a 12% chance per level to drop copper coins.", "광석을 캘 때 레벨당 12% 확률로 동화를 떨어뜨립니다."),
		new EnchantInfo(AUTO_SMELT, "Smelting Touch", "자동 제련",
			"Block drops come out smelted. Incompatible with Silk Touch. Pickaxes only.", "블록 드롭이 바로 제련되어 나옵니다. 섬세한 손길과 함께 쓸 수 없습니다. 곡괭이 전용."),
		new EnchantInfo(SWIFT_STEP, "Swift Step", "신속 보행",
			"+10% movement speed per level. Boots only.", "이동 속도가 레벨당 10% 빨라집니다. 부츠 전용.")
	);

	/** Every enchantment of the mod, for lang and tag datagen. */
	public static final List<EnchantInfo> ALL = Stream.of(
			ORIGINAL, WeaponEnchantments.ALL, RangedEnchantments.ALL, ToolEnchantments.ALL, ArmorEnchantments.BUFFS, ArmorEnchantments.AURAS
		)
		.flatMap(List::stream)
		.toList();

	public static void bootstrap(final BootstrapContext<Enchantment> context) {
		EnchantmentFactory factory = new EnchantmentFactory(context);
		WeaponEnchantments.bootstrap(factory);
		RangedEnchantments.bootstrap(factory);
		ToolEnchantments.bootstrap(factory);
		ArmorEnchantments.bootstrap(factory);
		bootstrapOriginal(context);
	}

	private static void bootstrapOriginal(final BootstrapContext<Enchantment> context) {
		HolderGetter<Item> items = context.lookup(Registries.ITEM);
		HolderGetter<Enchantment> enchantments = context.lookup(Registries.ENCHANTMENT);

		// Melee hit grants Regeneration II to the attacker (1.5s / 2.5s / 3.5s).
		register(
			context,
			LIFESTEAL,
			Enchantment.enchantment(
					Enchantment.definition(
						items.getOrThrow(ItemTags.SHARP_WEAPON_ENCHANTABLE),
						2,
						3,
						Enchantment.dynamicCost(10, 10),
						Enchantment.dynamicCost(40, 10),
						4,
						EquipmentSlotGroup.MAINHAND
					)
				)
				.withEffect(
					EnchantmentEffectComponents.POST_ATTACK,
					EnchantmentTarget.ATTACKER,
					EnchantmentTarget.ATTACKER,
					new ApplyMobEffect(
						HolderSet.direct(MobEffects.REGENERATION),
						LevelBasedValue.perLevel(1.5F, 1.0F),
						LevelBasedValue.perLevel(1.5F, 1.0F),
						LevelBasedValue.constant(1.0F),
						LevelBasedValue.constant(1.0F)
					),
					DamageSourceCondition.hasDamageSource(DamageSourcePredicate.Builder.damageType().isDirect(true))
				)
		);

		// Melee hit makes the victim bleed (3s / 5s / 7s, amplifier = level - 1).
		register(
			context,
			BLEEDING_EDGE,
			Enchantment.enchantment(
					Enchantment.definition(
						items.getOrThrow(ItemTags.SHARP_WEAPON_ENCHANTABLE),
						3,
						3,
						Enchantment.dynamicCost(5, 9),
						Enchantment.dynamicCost(35, 9),
						2,
						EquipmentSlotGroup.MAINHAND
					)
				)
				.withEffect(
					EnchantmentEffectComponents.POST_ATTACK,
					EnchantmentTarget.ATTACKER,
					EnchantmentTarget.VICTIM,
					new ApplyMobEffect(
						HolderSet.direct(ModEffects.BLEEDING),
						LevelBasedValue.perLevel(3.0F, 2.0F),
						LevelBasedValue.perLevel(3.0F, 2.0F),
						LevelBasedValue.perLevel(0.0F, 1.0F),
						LevelBasedValue.perLevel(0.0F, 1.0F)
					),
					DamageSourceCondition.hasDamageSource(DamageSourcePredicate.Builder.damageType().isDirect(true))
				)
		);

		// Mining an ore may drop copper coins (12% per level).
		register(
			context,
			COIN_FINDER,
			Enchantment.enchantment(
				Enchantment.definition(
					items.getOrThrow(ItemTags.MINING_ENCHANTABLE),
					2,
					3,
					Enchantment.dynamicCost(15, 9),
					Enchantment.dynamicCost(65, 9),
					4,
					EquipmentSlotGroup.MAINHAND
				)
			)
		);

		// Pickaxe drops come out smelted (iron ore -> iron ingot, stone -> stone, sand -> glass).
		// No data effect: AutoSmeltLoot adds a furnace_smelt function to block loot tables.
		register(
			context,
			AUTO_SMELT,
			Enchantment.enchantment(
					Enchantment.definition(
						items.getOrThrow(ItemTags.PICKAXES),
						2,
						1,
						Enchantment.constantCost(15),
						Enchantment.constantCost(65),
						4,
						EquipmentSlotGroup.MAINHAND
					)
				)
				.exclusiveWith(HolderSet.direct(enchantments.getOrThrow(Enchantments.SILK_TOUCH)))
		);

		// +10% movement speed per level.
		register(
			context,
			SWIFT_STEP,
			Enchantment.enchantment(
					Enchantment.definition(
						items.getOrThrow(ItemTags.FOOT_ARMOR_ENCHANTABLE),
						2,
						3,
						Enchantment.dynamicCost(10, 10),
						Enchantment.dynamicCost(40, 10),
						4,
						EquipmentSlotGroup.FEET
					)
				)
				.withEffect(
					EnchantmentEffectComponents.ATTRIBUTES,
					new EnchantmentAttributeEffect(
						MinecraftMode.id("enchantment.swift_step"), Attributes.MOVEMENT_SPEED, LevelBasedValue.perLevel(0.1F), AttributeModifier.Operation.ADD_MULTIPLIED_BASE
					)
				)
		);
	}

	private static void register(final BootstrapContext<Enchantment> context, final ResourceKey<Enchantment> key, final Enchantment.Builder builder) {
		context.register(key, builder.build(key.identifier()));
	}

	private static ResourceKey<Enchantment> key(final String name) {
		return ResourceKey.create(Registries.ENCHANTMENT, MinecraftMode.id(name));
	}

	private ModEnchantments() {
	}
}
