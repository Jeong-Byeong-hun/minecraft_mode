package com.minecraftmode.enchantment;

import java.util.List;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;

/**
 * Nine enchantments for farming (hoes) and fishing (fishing rods). All are markers read in code: the hoe ones by
 * {@code craft/Crops} when a crop is harvested or grass is cut, the rod ones by {@code craft/Fishing} when a fish is landed.
 */
public final class LifeEnchantments {
	public static final EnchantInfo BOUNTIFUL_HARVEST = EnchantInfo.of("bountiful_harvest", "Bountiful Harvest", "풍작",
		"Ripe crops of this mod have a 15% chance per level to give one more. Hoes only.",
		"이 모드의 다 자란 작물을 수확하면 레벨당 15% 확률로 하나 더 얻습니다. 괭이 전용.");
	public static final EnchantInfo REAPING = EnchantInfo.of("reaping", "Reaping", "낫질",
		"Harvesting a ripe crop also harvests the ripe crops around it: 3x3 (I) or 5x5 (II). Unripe ones are left alone. Hoes only.",
		"다 자란 작물을 수확하면 주변의 다 자란 작물도 함께 수확합니다. 3x3(I) 또는 5x5(II). 덜 자란 작물은 그대로 둡니다. 괭이 전용.");
	public static final EnchantInfo REPLANT = EnchantInfo.of("replant", "Replant", "자동 파종",
		"A harvested crop is planted again with one of its seeds (from the drops, the inventory or the bags). Hoes only.",
		"수확한 작물을 씨앗 하나로 다시 심습니다(떨어진 것, 인벤토리, 가방 순). 괭이 전용.");
	public static final EnchantInfo SEED_FINDER = EnchantInfo.of("seed_finder", "Seed Finder", "씨앗 탐색",
		"Grass gives seeds more often and more of them advanced; ripe basic crops have a 3% chance per level to give an advanced seed. "
			+ "Advanced seeds still need Farming level 10. Hoes only.",
		"풀에서 씨앗이 더 자주, 고급 씨앗이 더 많이 나오고, 다 자란 기본 작물에서 레벨당 3% 확률로 고급 씨앗이 나옵니다. "
			+ "고급 씨앗은 농사 레벨 10부터. 괭이 전용.");
	public static final EnchantInfo FARMERS_WISDOM = EnchantInfo.of("farmers_wisdom", "Farmer's Wisdom", "농부의 지혜",
		"+20% Farming experience per level. Hoes only.", "농사 경험치가 레벨당 20% 늘어납니다. 괭이 전용.");
	public static final EnchantInfo ANGLERS_INSTINCT = EnchantInfo.of("anglers_instinct", "Angler's Instinct", "어부의 감",
		"Rare and better fish bite more often (+50% weight per level). Fishing level limits still apply. Fishing rods only.",
		"희귀 이상 물고기가 더 자주 낚입니다(레벨당 가중치 +50%). 낚시 레벨 제한은 그대로입니다. 낚싯대 전용.");
	public static final EnchantInfo NET_CAST = EnchantInfo.of("net_cast", "Net Cast", "그물질",
		"Landing one of this mod's fish has a 15% chance per level to land another of the same kind. Fishing rods only.",
		"이 모드의 물고기를 낚으면 레벨당 15% 확률로 같은 물고기를 하나 더 얻습니다. 낚싯대 전용.");
	public static final EnchantInfo ANGLERS_WISDOM = EnchantInfo.of("anglers_wisdom", "Angler's Wisdom", "강태공의 지혜",
		"+20% Fishing experience per level. Fishing rods only.", "낚시 경험치가 레벨당 20% 늘어납니다. 낚싯대 전용.");
	public static final EnchantInfo ANY_WATERS = EnchantInfo.of("any_waters", "Any Waters", "만능 어장",
		"Every place's fish bite anywhere, as often as at home. Fishing level limits still apply. Fishing rods only.",
		"어느 물에서나 모든 장소의 물고기가 제 서식지와 같은 확률로 낚입니다. 낚시 레벨 제한은 그대로입니다. 낚싯대 전용.");

	public static final List<EnchantInfo> ALL = List.of(
		BOUNTIFUL_HARVEST, REAPING, REPLANT, SEED_FINDER, FARMERS_WISDOM, ANGLERS_INSTINCT, NET_CAST, ANGLERS_WISDOM, ANY_WATERS
	);

	static void bootstrap(final EnchantmentFactory f) {
		f.register(BOUNTIFUL_HARVEST, f.enchantment(ItemTags.HOES, 2, 3, EquipmentSlotGroup.MAINHAND));
		f.register(REAPING, f.enchantment(ItemTags.HOES, 2, 2, EquipmentSlotGroup.MAINHAND));
		f.register(REPLANT, f.enchantment(ItemTags.HOES, 2, 1, EquipmentSlotGroup.MAINHAND));
		f.register(SEED_FINDER, f.enchantment(ItemTags.HOES, 2, 3, EquipmentSlotGroup.MAINHAND));
		f.register(FARMERS_WISDOM, f.enchantment(ItemTags.HOES, 2, 3, EquipmentSlotGroup.MAINHAND));
		f.register(ANGLERS_INSTINCT, f.enchantment(ItemTags.FISHING_ENCHANTABLE, 2, 3, EquipmentSlotGroup.MAINHAND));
		f.register(NET_CAST, f.enchantment(ItemTags.FISHING_ENCHANTABLE, 2, 2, EquipmentSlotGroup.MAINHAND));
		f.register(ANGLERS_WISDOM, f.enchantment(ItemTags.FISHING_ENCHANTABLE, 2, 3, EquipmentSlotGroup.MAINHAND));
		f.register(ANY_WATERS, f.enchantment(ItemTags.FISHING_ENCHANTABLE, 1, 1, EquipmentSlotGroup.MAINHAND));
	}

	private LifeEnchantments() {
	}
}
