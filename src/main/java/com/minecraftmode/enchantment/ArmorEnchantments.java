package com.minecraftmode.enchantment;

import static com.minecraftmode.enchantment.EnchantmentFactory.attribute;

import java.util.List;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.LevelBasedValue;

/**
 * Armor enchantments come in two kinds:
 * <ul>
 *   <li>Buffs: affect only the wearer. Each buff belongs to one armor slot, so it can never be counted twice.</li>
 *   <li>Auras: affect every player within {@link ArmorAuras#RADIUS} blocks, the wearer included. The same aura
 *       never stacks: a player only gets the strongest level among all nearby sources. An aura and a buff of
 *       the same kind (e.g. Strength + Aura of Strength) do stack.</li>
 * </ul>
 * Buffs are data-driven attribute effects (except Night Sight, which is client-side). Auras are applied by {@link ArmorAuras}.
 */
public final class ArmorEnchantments {
	// --- Buffs: helmet
	public static final EnchantInfo NIGHT_SIGHT = EnchantInfo.of("night_sight", "Night Sight", "야간 시야",
		"Brightens darkness by 20% per level; level V sees at night like in daylight. Helmets only.",
		"어둠을 레벨당 20%씩 밝게 봅니다. V에서는 밤에도 낮처럼 보입니다. 투구 전용.");
	public static final EnchantInfo FOCUS = EnchantInfo.of("focus", "Focus", "집중",
		"+0.5 block and entity reach per level. Helmets only.", "블록·대상에 닿는 거리가 레벨당 0.5 늘어납니다. 투구 전용.");
	public static final EnchantInfo LUCKY_CHARM = EnchantInfo.of("lucky_charm", "Lucky Charm", "행운의 부적",
		"+1 Luck per level (better loot from chests and fishing). Helmets only.", "행운이 레벨당 1 오릅니다(상자·낚시 보상 향상). 투구 전용.");
	// --- Buffs: chestplate
	public static final EnchantInfo STRENGTH = EnchantInfo.of("strength", "Strength", "힘",
		"+1 attack damage per level. Chestplates only.", "공격력이 레벨당 1 오릅니다. 흉갑 전용.");
	public static final EnchantInfo VITALITY = EnchantInfo.of("vitality", "Vitality", "활력",
		"+2 max health (one heart) per level. Chestplates only.", "최대 체력이 레벨당 2(하트 1칸) 늘어납니다. 흉갑 전용.");
	public static final EnchantInfo FORTIFY = EnchantInfo.of("fortify", "Fortify", "방어 강화",
		"+1 armor per level. Chestplates only.", "방어력이 레벨당 1 오릅니다. 흉갑 전용.");
	// --- Buffs: leggings
	public static final EnchantInfo HASTE = EnchantInfo.of("haste", "Haste", "서두름",
		"+20% mining speed per level. Leggings only.", "채굴 속도가 레벨당 20% 빨라집니다. 레깅스 전용.");
	public static final EnchantInfo RESILIENCE = EnchantInfo.of("resilience", "Resilience", "강인함",
		"+1 armor toughness per level. Leggings only.", "방어 강도가 레벨당 1 오릅니다. 레깅스 전용.");
	public static final EnchantInfo STEADFAST = EnchantInfo.of("steadfast", "Steadfast", "불굴",
		"+10% knockback resistance per level. Leggings only.", "밀치기 저항이 레벨당 10% 오릅니다. 레깅스 전용.");
	// --- Buffs: boots
	public static final EnchantInfo LEAP = EnchantInfo.of("leap", "Leap", "도약",
		"+10% jump strength and +1 safe fall distance per level. Boots only.", "점프력이 레벨당 10%, 안전 낙하 거리가 레벨당 1 늘어납니다. 부츠 전용.");

	// --- Auras (any armor piece)
	public static final EnchantInfo MINING_AURA = EnchantInfo.of("mining_aura", "Aura of Haste", "채굴 오라",
		"Nearby players mine 20% faster per level. Does not stack with other Auras of Haste.",
		"주변 플레이어의 채굴 속도가 레벨당 20% 빨라집니다. 같은 오라는 중첩되지 않습니다.");
	public static final EnchantInfo STRENGTH_AURA = EnchantInfo.of("strength_aura", "Aura of Strength", "힘의 오라",
		"Nearby players deal +1 attack damage per level.", "주변 플레이어의 공격력이 레벨당 1 오릅니다. 같은 오라는 중첩되지 않습니다.");
	public static final EnchantInfo GUARDIAN_AURA = EnchantInfo.of("guardian_aura", "Aura of Protection", "수호 오라",
		"Nearby players gain +1 armor per level.", "주변 플레이어의 방어력이 레벨당 1 오릅니다. 같은 오라는 중첩되지 않습니다.");
	public static final EnchantInfo SWIFTNESS_AURA = EnchantInfo.of("swiftness_aura", "Aura of Swiftness", "신속 오라",
		"Nearby players move 5% faster per level.", "주변 플레이어의 이동 속도가 레벨당 5% 빨라집니다. 같은 오라는 중첩되지 않습니다.");
	public static final EnchantInfo REGENERATION_AURA = EnchantInfo.of("regeneration_aura", "Aura of Regeneration", "재생 오라",
		"Nearby players heal 0.5 health per level every 3 seconds.", "주변 플레이어가 3초마다 레벨당 체력 0.5를 회복합니다. 같은 오라는 중첩되지 않습니다.");
	public static final EnchantInfo VITALITY_AURA = EnchantInfo.of("vitality_aura", "Aura of Vitality", "생명 오라",
		"Nearby players gain +2 max health per level.", "주변 플레이어의 최대 체력이 레벨당 2 늘어납니다. 같은 오라는 중첩되지 않습니다.");
	public static final EnchantInfo FORTUNE_AURA = EnchantInfo.of("fortune_aura", "Aura of Fortune", "행운 오라",
		"Nearby players gain +1 Luck per level.", "주변 플레이어의 행운이 레벨당 1 오릅니다. 같은 오라는 중첩되지 않습니다.");
	public static final EnchantInfo TOUGHNESS_AURA = EnchantInfo.of("toughness_aura", "Aura of Resilience", "강인함 오라",
		"Nearby players gain +1 armor toughness per level.", "주변 플레이어의 방어 강도가 레벨당 1 오릅니다. 같은 오라는 중첩되지 않습니다.");
	public static final EnchantInfo LEAP_AURA = EnchantInfo.of("leap_aura", "Aura of Leaping", "도약 오라",
		"Nearby players jump 10% higher per level and gain +1 safe fall distance per level.",
		"주변 플레이어의 점프력이 레벨당 10%, 안전 낙하 거리가 레벨당 1 늘어납니다. 같은 오라는 중첩되지 않습니다.");
	public static final EnchantInfo SATIATION_AURA = EnchantInfo.of("satiation_aura", "Aura of Plenty", "포만 오라",
		"Nearby players regain 1 hunger per level every 10 seconds.", "주변 플레이어가 10초마다 레벨당 허기 1을 회복합니다. 같은 오라는 중첩되지 않습니다.");

	public static final List<EnchantInfo> BUFFS = List.of(
		NIGHT_SIGHT, FOCUS, LUCKY_CHARM, STRENGTH, VITALITY, FORTIFY, HASTE, RESILIENCE, STEADFAST, LEAP
	);
	public static final List<EnchantInfo> AURAS = List.of(
		MINING_AURA, STRENGTH_AURA, GUARDIAN_AURA, SWIFTNESS_AURA, REGENERATION_AURA,
		VITALITY_AURA, FORTUNE_AURA, TOUGHNESS_AURA, LEAP_AURA, SATIATION_AURA
	);

	static void bootstrap(final EnchantmentFactory f) {
		var add = AttributeModifier.Operation.ADD_VALUE;
		var percent = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;

		// Helmet
		f.register(NIGHT_SIGHT, f.enchantment(ItemTags.HEAD_ARMOR_ENCHANTABLE, 2, 5, EquipmentSlotGroup.HEAD));
		f.register(FOCUS, f.enchantment(ItemTags.HEAD_ARMOR_ENCHANTABLE, 2, 5, EquipmentSlotGroup.HEAD)
			.withEffect(EnchantmentEffectComponents.ATTRIBUTES, attribute("focus_blocks", Attributes.BLOCK_INTERACTION_RANGE, LevelBasedValue.perLevel(0.5F), add))
			.withEffect(EnchantmentEffectComponents.ATTRIBUTES, attribute("focus_entities", Attributes.ENTITY_INTERACTION_RANGE, LevelBasedValue.perLevel(0.5F), add)));
		f.register(LUCKY_CHARM, f.enchantment(ItemTags.HEAD_ARMOR_ENCHANTABLE, 2, 5, EquipmentSlotGroup.HEAD)
			.withEffect(EnchantmentEffectComponents.ATTRIBUTES, attribute("lucky_charm", Attributes.LUCK, LevelBasedValue.perLevel(1.0F), add)));
		// Chestplate
		f.register(STRENGTH, f.enchantment(ItemTags.CHEST_ARMOR_ENCHANTABLE, 2, 5, EquipmentSlotGroup.CHEST)
			.withEffect(EnchantmentEffectComponents.ATTRIBUTES, attribute("strength", Attributes.ATTACK_DAMAGE, LevelBasedValue.perLevel(1.0F), add)));
		f.register(VITALITY, f.enchantment(ItemTags.CHEST_ARMOR_ENCHANTABLE, 2, 5, EquipmentSlotGroup.CHEST)
			.withEffect(EnchantmentEffectComponents.ATTRIBUTES, attribute("vitality", Attributes.MAX_HEALTH, LevelBasedValue.perLevel(2.0F), add)));
		f.register(FORTIFY, f.enchantment(ItemTags.CHEST_ARMOR_ENCHANTABLE, 5, 5, EquipmentSlotGroup.CHEST)
			.withEffect(EnchantmentEffectComponents.ATTRIBUTES, attribute("fortify", Attributes.ARMOR, LevelBasedValue.perLevel(1.0F), add)));
		// Leggings
		f.register(HASTE, f.enchantment(ItemTags.LEG_ARMOR_ENCHANTABLE, 5, 5, EquipmentSlotGroup.LEGS)
			.withEffect(EnchantmentEffectComponents.ATTRIBUTES, attribute("haste", Attributes.BLOCK_BREAK_SPEED, LevelBasedValue.perLevel(0.2F), percent)));
		f.register(RESILIENCE, f.enchantment(ItemTags.LEG_ARMOR_ENCHANTABLE, 5, 5, EquipmentSlotGroup.LEGS)
			.withEffect(EnchantmentEffectComponents.ATTRIBUTES, attribute("resilience", Attributes.ARMOR_TOUGHNESS, LevelBasedValue.perLevel(1.0F), add)));
		f.register(STEADFAST, f.enchantment(ItemTags.LEG_ARMOR_ENCHANTABLE, 5, 5, EquipmentSlotGroup.LEGS)
			.withEffect(EnchantmentEffectComponents.ATTRIBUTES, attribute("steadfast", Attributes.KNOCKBACK_RESISTANCE, LevelBasedValue.perLevel(0.1F), add)));
		// Boots
		f.register(LEAP, f.enchantment(ItemTags.FOOT_ARMOR_ENCHANTABLE, 5, 5, EquipmentSlotGroup.FEET)
			.withEffect(EnchantmentEffectComponents.ATTRIBUTES, attribute("leap_jump", Attributes.JUMP_STRENGTH, LevelBasedValue.perLevel(0.1F), percent))
			.withEffect(EnchantmentEffectComponents.ATTRIBUTES, attribute("leap_fall", Attributes.SAFE_FALL_DISTANCE, LevelBasedValue.perLevel(1.0F), add)));

		// Auras: marker enchantments on any armor piece, applied by ArmorAuras.
		for (EnchantInfo aura : AURAS) {
			f.register(aura, f.enchantment(ItemTags.ARMOR_ENCHANTABLE, 1, 5, EquipmentSlotGroup.ARMOR));
		}
	}

	private ArmorEnchantments() {
	}
}
