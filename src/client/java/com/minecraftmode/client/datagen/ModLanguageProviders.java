package com.minecraftmode.client.datagen;

import com.minecraftmode.economy.ShopType;
import com.minecraftmode.enchantment.EnchantInfo;
import com.minecraftmode.enchantment.ModEnchantments;
import com.minecraftmode.registry.ModCreativeTabs;
import com.minecraftmode.registry.ModEffects;
import com.minecraftmode.registry.ModEntities;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.registry.ModTags;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;

public final class ModLanguageProviders {
	public static class English extends FabricLanguageProvider {
		public English(final FabricPackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
			super(output, "en_us", registries);
		}

		@Override
		public void generateTranslations(final HolderLookup.Provider registries, final TranslationBuilder builder) {
			builder.addCreativeModeTab(ModCreativeTabs.MAIN, "Minecraft Mode");

			builder.add(ModItems.MYTHRIL_ORE, "Mythril Ore");
			builder.add(ModItems.DEEPSLATE_MYTHRIL_ORE, "Deepslate Mythril Ore");
			builder.add(ModItems.RAW_MYTHRIL, "Raw Mythril");
			builder.add(ModItems.RAW_MYTHRIL_BLOCK, "Block of Raw Mythril");
			builder.add(ModItems.MYTHRIL_NUGGET, "Mythril Nugget");
			builder.add(ModItems.MYTHRIL_INGOT, "Mythril Ingot");
			builder.add(ModItems.MYTHRIL_BLOCK, "Block of Mythril");
			builder.add(ModItems.MYTHRIL_SWORD, "Mythril Sword");
			builder.add(ModItems.MYTHRIL_PICKAXE, "Mythril Pickaxe");
			builder.add(ModItems.MYTHRIL_AXE, "Mythril Axe");
			builder.add(ModItems.MYTHRIL_SHOVEL, "Mythril Shovel");
			builder.add(ModItems.MYTHRIL_HOE, "Mythril Hoe");
			builder.add(ModItems.MYTHRIL_HELMET, "Mythril Helmet");
			builder.add(ModItems.MYTHRIL_CHESTPLATE, "Mythril Chestplate");
			builder.add(ModItems.MYTHRIL_LEGGINGS, "Mythril Leggings");
			builder.add(ModItems.MYTHRIL_BOOTS, "Mythril Boots");

			builder.add(ModItems.ALUMINUM_ORE, "Aluminum Ore");
			builder.add(ModItems.DEEPSLATE_ALUMINUM_ORE, "Deepslate Aluminum Ore");
			builder.add(ModItems.RAW_ALUMINUM, "Raw Aluminum");
			builder.add(ModItems.RAW_ALUMINUM_BLOCK, "Block of Raw Aluminum");
			builder.add(ModItems.ALUMINUM_NUGGET, "Aluminum Nugget");
			builder.add(ModItems.ALUMINUM_INGOT, "Aluminum Ingot");
			builder.add(ModItems.ALUMINUM_BLOCK, "Block of Aluminum");

			builder.add(ModItems.PLASTIC_SHEET, "Plastic Sheet");
			builder.add(ModItems.PLASTIC_BLOCK, "Block of Plastic");

			builder.add(ModItems.COPPER_COIN, "Copper Coin");
			builder.add(ModItems.SILVER_COIN, "Silver Coin");
			builder.add(ModItems.GOLD_COIN, "Gold Coin");
			builder.add(ModItems.SHOP_BLOCK, "General Store");
			builder.add(ModItems.BLACKSMITH_SHOP, "Blacksmith");
			builder.add(ModItems.GROCER_SHOP, "Grocer");
			builder.add(ModItems.JEWELER_SHOP, "Jeweler");
			builder.add(ShopType.GENERAL.titleKey(), "General Store");
			builder.add(ShopType.BLACKSMITH.titleKey(), "Blacksmith");
			builder.add(ShopType.GROCER.titleKey(), "Grocer");
			builder.add(ShopType.JEWELER.titleKey(), "Jeweler");

			builder.add(ModEntities.MINE_RAIDER, "Mine Raider");
			builder.add(ModItems.MINE_RAIDER_SPAWN_EGG, "Mine Raider Spawn Egg");
			builder.add(ModEntities.MYTHRIL_GOLEM, "Mythril Golem");
			builder.add(ModItems.MYTHRIL_GOLEM_SPAWN_EGG, "Mythril Golem Spawn Egg");

			builder.add(ModEffects.BLEEDING.value(), "Bleeding");

			for (EnchantInfo info : ModEnchantments.ALL) {
				builder.addEnchantment(info.key(), info.en());
				builder.add(info.descriptionKey(), info.enDesc());
			}

			builder.add(ModTags.COINS, "Coins");
			builder.add(ModTags.MYTHRIL_TOOL_MATERIALS, "Mythril Tool Materials");
			builder.add(ModTags.REPAIRS_MYTHRIL_ARMOR, "Repairs Mythril Armor");

			JobLang.add(builder, false);
		}
	}

	public static class Korean extends FabricLanguageProvider {
		public Korean(final FabricPackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
			super(output, "ko_kr", registries);
		}

		@Override
		public void generateTranslations(final HolderLookup.Provider registries, final TranslationBuilder builder) {
			builder.addCreativeModeTab(ModCreativeTabs.MAIN, "마인크래프트 모드");

			builder.add(ModItems.MYTHRIL_ORE, "미스릴 광석");
			builder.add(ModItems.DEEPSLATE_MYTHRIL_ORE, "심층암 미스릴 광석");
			builder.add(ModItems.RAW_MYTHRIL, "미스릴 원석");
			builder.add(ModItems.RAW_MYTHRIL_BLOCK, "미스릴 원석 블록");
			builder.add(ModItems.MYTHRIL_NUGGET, "미스릴 조각");
			builder.add(ModItems.MYTHRIL_INGOT, "미스릴 주괴");
			builder.add(ModItems.MYTHRIL_BLOCK, "미스릴 블록");
			builder.add(ModItems.MYTHRIL_SWORD, "미스릴 검");
			builder.add(ModItems.MYTHRIL_PICKAXE, "미스릴 곡괭이");
			builder.add(ModItems.MYTHRIL_AXE, "미스릴 도끼");
			builder.add(ModItems.MYTHRIL_SHOVEL, "미스릴 삽");
			builder.add(ModItems.MYTHRIL_HOE, "미스릴 괭이");
			builder.add(ModItems.MYTHRIL_HELMET, "미스릴 투구");
			builder.add(ModItems.MYTHRIL_CHESTPLATE, "미스릴 흉갑");
			builder.add(ModItems.MYTHRIL_LEGGINGS, "미스릴 레깅스");
			builder.add(ModItems.MYTHRIL_BOOTS, "미스릴 부츠");

			builder.add(ModItems.ALUMINUM_ORE, "알루미늄 광석");
			builder.add(ModItems.DEEPSLATE_ALUMINUM_ORE, "심층암 알루미늄 광석");
			builder.add(ModItems.RAW_ALUMINUM, "알루미늄 원석");
			builder.add(ModItems.RAW_ALUMINUM_BLOCK, "알루미늄 원석 블록");
			builder.add(ModItems.ALUMINUM_NUGGET, "알루미늄 조각");
			builder.add(ModItems.ALUMINUM_INGOT, "알루미늄 주괴");
			builder.add(ModItems.ALUMINUM_BLOCK, "알루미늄 블록");

			builder.add(ModItems.PLASTIC_SHEET, "플라스틱 판");
			builder.add(ModItems.PLASTIC_BLOCK, "플라스틱 블록");

			builder.add(ModItems.COPPER_COIN, "동화");
			builder.add(ModItems.SILVER_COIN, "은화");
			builder.add(ModItems.GOLD_COIN, "금화");
			builder.add(ModItems.SHOP_BLOCK, "잡화점");
			builder.add(ModItems.BLACKSMITH_SHOP, "대장간");
			builder.add(ModItems.GROCER_SHOP, "식료품점");
			builder.add(ModItems.JEWELER_SHOP, "보석상");
			builder.add(ShopType.GENERAL.titleKey(), "잡화점");
			builder.add(ShopType.BLACKSMITH.titleKey(), "대장간");
			builder.add(ShopType.GROCER.titleKey(), "식료품점");
			builder.add(ShopType.JEWELER.titleKey(), "보석상");

			builder.add(ModEntities.MINE_RAIDER, "광산 약탈자");
			builder.add(ModItems.MINE_RAIDER_SPAWN_EGG, "광산 약탈자 생성 알");
			builder.add(ModEntities.MYTHRIL_GOLEM, "미스릴 골렘");
			builder.add(ModItems.MYTHRIL_GOLEM_SPAWN_EGG, "미스릴 골렘 생성 알");

			builder.add(ModEffects.BLEEDING.value(), "출혈");

			for (EnchantInfo info : ModEnchantments.ALL) {
				builder.addEnchantment(info.key(), info.ko());
				builder.add(info.descriptionKey(), info.koDesc());
			}

			builder.add(ModTags.COINS, "동전");
			builder.add(ModTags.MYTHRIL_TOOL_MATERIALS, "미스릴 도구 재료");
			builder.add(ModTags.REPAIRS_MYTHRIL_ARMOR, "미스릴 갑옷 수리 재료");

			JobLang.add(builder, true);
		}
	}

	private ModLanguageProviders() {
	}
}
