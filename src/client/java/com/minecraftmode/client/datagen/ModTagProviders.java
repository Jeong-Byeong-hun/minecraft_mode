package com.minecraftmode.client.datagen;

import com.minecraftmode.enchantment.EnchantInfo;
import com.minecraftmode.enchantment.ModEnchantments;
import com.minecraftmode.enchantment.RangedEnchantments;
import com.minecraftmode.registry.ModBlocks;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.registry.ModTags;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;

public final class ModTagProviders {
	public static class Blocks extends FabricTagsProvider.BlockTagsProvider {
		public Blocks(final FabricPackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
			super(output, registries);
		}

		@Override
		protected void addTags(final HolderLookup.Provider registries) {
			this.builder(BlockTags.MINEABLE_WITH_PICKAXE)
				.add(
					key(ModBlocks.MYTHRIL_ORE), key(ModBlocks.DEEPSLATE_MYTHRIL_ORE), key(ModBlocks.MYTHRIL_BLOCK), key(ModBlocks.RAW_MYTHRIL_BLOCK),
					key(ModBlocks.ALUMINUM_ORE), key(ModBlocks.DEEPSLATE_ALUMINUM_ORE), key(ModBlocks.ALUMINUM_BLOCK), key(ModBlocks.RAW_ALUMINUM_BLOCK),
					key(ModBlocks.PLASTIC_BLOCK)
				);
			this.builder(BlockTags.MINEABLE_WITH_AXE)
				.add(key(ModBlocks.SHOP_BLOCK), key(ModBlocks.BLACKSMITH_SHOP), key(ModBlocks.GROCER_SHOP), key(ModBlocks.JEWELER_SHOP));
			this.builder(BlockTags.NEEDS_IRON_TOOL)
				.add(key(ModBlocks.MYTHRIL_ORE), key(ModBlocks.DEEPSLATE_MYTHRIL_ORE), key(ModBlocks.MYTHRIL_BLOCK), key(ModBlocks.RAW_MYTHRIL_BLOCK));
			this.builder(BlockTags.NEEDS_STONE_TOOL)
				.add(key(ModBlocks.ALUMINUM_ORE), key(ModBlocks.DEEPSLATE_ALUMINUM_ORE), key(ModBlocks.ALUMINUM_BLOCK), key(ModBlocks.RAW_ALUMINUM_BLOCK));
			this.builder(BlockTags.BEACON_BASE_BLOCKS).add(key(ModBlocks.MYTHRIL_BLOCK), key(ModBlocks.ALUMINUM_BLOCK));
			// Coin Finder only triggers on c:ores.
			this.builder(ConventionalBlockTags.ORES)
				.add(key(ModBlocks.MYTHRIL_ORE), key(ModBlocks.DEEPSLATE_MYTHRIL_ORE), key(ModBlocks.ALUMINUM_ORE), key(ModBlocks.DEEPSLATE_ALUMINUM_ORE));
		}

		private static ResourceKey<Block> key(final Block block) {
			return BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow();
		}
	}

	public static class Items extends FabricTagsProvider.ItemTagsProvider {
		public Items(final FabricPackOutput output, final CompletableFuture<HolderLookup.Provider> registries, final Blocks blockTags) {
			super(output, registries, blockTags);
		}

		@Override
		protected void addTags(final HolderLookup.Provider registries) {
			this.builder(ModTags.MYTHRIL_TOOL_MATERIALS).add(key(ModItems.MYTHRIL_INGOT));
			this.builder(ModTags.REPAIRS_MYTHRIL_ARMOR).add(key(ModItems.MYTHRIL_INGOT));
			this.builder(ModTags.COINS).add(key(ModItems.COPPER_COIN), key(ModItems.SILVER_COIN), key(ModItems.GOLD_COIN));

			// Vanilla tool/armor tags make the items enchantable and usable as weapons.
			this.builder(ItemTags.SWORDS).add(key(ModItems.MYTHRIL_SWORD));
			this.builder(ItemTags.PICKAXES).add(key(ModItems.MYTHRIL_PICKAXE));
			this.builder(ItemTags.AXES).add(key(ModItems.MYTHRIL_AXE));
			this.builder(ItemTags.SHOVELS).add(key(ModItems.MYTHRIL_SHOVEL));
			this.builder(ItemTags.HOES).add(key(ModItems.MYTHRIL_HOE));
			this.builder(ItemTags.HEAD_ARMOR).add(key(ModItems.MYTHRIL_HELMET));
			this.builder(ItemTags.CHEST_ARMOR).add(key(ModItems.MYTHRIL_CHESTPLATE));
			this.builder(ItemTags.LEG_ARMOR).add(key(ModItems.MYTHRIL_LEGGINGS));
			this.builder(ItemTags.FOOT_ARMOR).add(key(ModItems.MYTHRIL_BOOTS));
			this.builder(ItemTags.BEACON_PAYMENT_ITEMS).add(key(ModItems.MYTHRIL_INGOT), key(ModItems.ALUMINUM_INGOT));
		}

		private static ResourceKey<Item> key(final Item item) {
			return BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow();
		}
	}

	public static class Enchantments extends FabricTagsProvider<Enchantment> {
		public Enchantments(final FabricPackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
			super(output, Registries.ENCHANTMENT, registries);
		}

		@Override
		protected void addTags(final HolderLookup.Provider registries) {
			// non_treasure feeds the enchanting table, villager trades and random loot.
			var nonTreasure = this.builder(EnchantmentTags.NON_TREASURE);
			for (EnchantInfo info : ModEnchantments.ALL) {
				nonTreasure.add(info.key());
			}
			// Barrage joins vanilla's crossbow set, so it excludes Multishot and Piercing (and they exclude it).
			this.builder(EnchantmentTags.CROSSBOW_EXCLUSIVE).add(RangedEnchantments.BARRAGE.key());
		}
	}

	private ModTagProviders() {
	}
}
