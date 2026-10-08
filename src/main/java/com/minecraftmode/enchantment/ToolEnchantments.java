package com.minecraftmode.enchantment;

import static com.minecraftmode.enchantment.EnchantmentFactory.attribute;

import java.util.List;
import net.minecraft.core.HolderSet;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.MultiplyValue;

/**
 * Ten enchantments for mining tools (pickaxes, shovels, axes, hoes). Vein Miner, Excavation, Magnet,
 * Treasure Hunter, Self Repair and Momentum Mining are implemented by {@link ToolEnchantmentHandlers}.
 */
public final class ToolEnchantments {
	public static final EnchantInfo VEIN_MINER = EnchantInfo.of("vein_miner", "Vein Miner", "광맥 채굴",
		"Mining an ore also mines up to 4 connected ores of the same kind per level. Pickaxes only.",
		"광석을 캐면 연결된 같은 광석을 레벨당 4개까지 함께 캡니다. 곡괭이 전용.");
	public static final EnchantInfo EXCAVATION = EnchantInfo.of("excavation", "Excavation", "굴착",
		"Mines a 3x3 (I) or 5x5 (II) area facing you. Incompatible with Vein Miner.",
		"바라보는 면 기준 3x3(I) 또는 5x5(II) 범위를 한 번에 캡니다. 광맥 채굴과 함께 쓸 수 없습니다.");
	public static final EnchantInfo WISDOM = EnchantInfo.of("wisdom", "Wisdom", "지혜",
		"Blocks drop 50% more experience per level.", "블록에서 나오는 경험치가 레벨당 50% 늘어납니다.");
	public static final EnchantInfo OVERCLOCK = EnchantInfo.of("overclock", "Overclock", "과부하",
		"+15% mining speed per level.", "채굴 속도가 레벨당 15% 빨라집니다.");
	public static final EnchantInfo LONG_REACH = EnchantInfo.of("long_reach", "Long Reach", "긴 팔",
		"+0.5 block reach per level.", "블록에 닿는 거리가 레벨당 0.5블록 늘어납니다.");
	public static final EnchantInfo AQUAMINER = EnchantInfo.of("aquaminer", "Aquaminer", "수중 굴착",
		"Mines faster underwater; level IV mines as fast as on land.", "물속에서 더 빨리 캡니다. IV에서는 땅 위와 같은 속도입니다.");
	public static final EnchantInfo MAGNET = EnchantInfo.of("magnet", "Magnet", "자석",
		"Block drops go straight into your inventory.", "블록 드롭이 바로 인벤토리로 들어옵니다.");
	public static final EnchantInfo TREASURE_HUNTER = EnchantInfo.of("treasure_hunter", "Treasure Hunter", "보물 사냥꾼",
		"Mining stone has a 1% chance per level to unearth a treasure.", "돌을 캘 때 레벨당 1% 확률로 보물을 발견합니다.");
	public static final EnchantInfo SELF_REPAIR = EnchantInfo.of("self_repair", "Self Repair", "자가 수리",
		"Repairs 1 durability per level every 2 seconds while carried. Incompatible with Mending.",
		"지니고 있으면 2초마다 레벨당 내구도 1을 수리합니다. 수선과 함께 쓸 수 없습니다.");
	public static final EnchantInfo MINING_MOMENTUM = EnchantInfo.of("mining_momentum", "Momentum Mining", "채굴 탄력",
		"Breaking a block grants Haste (same level) for 2 seconds.", "블록을 부수면 2초 동안 같은 레벨의 성급함을 얻습니다.");

	public static final List<EnchantInfo> ALL = List.of(
		VEIN_MINER, EXCAVATION, WISDOM, OVERCLOCK, LONG_REACH, AQUAMINER, MAGNET, TREASURE_HUNTER, SELF_REPAIR, MINING_MOMENTUM
	);

	static void bootstrap(final EnchantmentFactory f) {
		f.register(VEIN_MINER, f.enchantment(ItemTags.PICKAXES, 2, 5, EquipmentSlotGroup.MAINHAND));
		f.register(EXCAVATION, f.enchantment(ItemTags.MINING_ENCHANTABLE, 1, 2, EquipmentSlotGroup.MAINHAND)
			.exclusiveWith(HolderSet.direct(f.enchantments.getOrThrow(VEIN_MINER.key()))));
		f.register(WISDOM, f.enchantment(ItemTags.MINING_ENCHANTABLE, 5, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.BLOCK_EXPERIENCE, new MultiplyValue(LevelBasedValue.perLevel(1.5F, 0.5F))));
		f.register(OVERCLOCK, f.enchantment(ItemTags.MINING_ENCHANTABLE, 5, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.ATTRIBUTES,
				attribute("overclock", Attributes.BLOCK_BREAK_SPEED, LevelBasedValue.perLevel(0.15F), AttributeModifier.Operation.ADD_MULTIPLIED_BASE)));
		f.register(LONG_REACH, f.enchantment(ItemTags.MINING_ENCHANTABLE, 2, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.ATTRIBUTES,
				attribute("long_reach", Attributes.BLOCK_INTERACTION_RANGE, LevelBasedValue.perLevel(0.5F), AttributeModifier.Operation.ADD_VALUE)));
		f.register(AQUAMINER, f.enchantment(ItemTags.MINING_ENCHANTABLE, 2, 4, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.ATTRIBUTES,
				attribute("aquaminer", Attributes.SUBMERGED_MINING_SPEED, LevelBasedValue.perLevel(0.2F), AttributeModifier.Operation.ADD_VALUE)));
		f.register(MAGNET, f.enchantment(ItemTags.MINING_ENCHANTABLE, 2, 1, EquipmentSlotGroup.MAINHAND));
		f.register(TREASURE_HUNTER, f.enchantment(ItemTags.MINING_ENCHANTABLE, 2, 5, EquipmentSlotGroup.MAINHAND));
		f.register(SELF_REPAIR, f.enchantment(ItemTags.MINING_ENCHANTABLE, 1, 3, EquipmentSlotGroup.MAINHAND)
			.exclusiveWith(HolderSet.direct(f.vanilla(Enchantments.MENDING))));
		f.register(MINING_MOMENTUM, f.enchantment(ItemTags.MINING_ENCHANTABLE, 2, 5, EquipmentSlotGroup.MAINHAND));
	}

	private ToolEnchantments() {
	}
}
