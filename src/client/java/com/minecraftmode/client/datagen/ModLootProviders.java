package com.minecraftmode.client.datagen;

import com.minecraftmode.registry.ModBlocks;
import com.minecraftmode.registry.ModEntities;
import com.minecraftmode.registry.ModItems;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricEntityLootSubProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

public final class ModLootProviders {
	public static class Blocks extends FabricBlockLootSubProvider {
		public Blocks(final FabricPackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
			super(output, registries);
		}

		@Override
		public void generate() {
			// Ores drop their raw form (Fortune applies); Silk Touch drops the ore block.
			this.add(ModBlocks.MYTHRIL_ORE, this.createOreDrop(ModBlocks.MYTHRIL_ORE, ModItems.RAW_MYTHRIL));
			this.add(ModBlocks.DEEPSLATE_MYTHRIL_ORE, this.createOreDrop(ModBlocks.DEEPSLATE_MYTHRIL_ORE, ModItems.RAW_MYTHRIL));
			this.add(ModBlocks.ALUMINUM_ORE, this.createOreDrop(ModBlocks.ALUMINUM_ORE, ModItems.RAW_ALUMINUM));
			this.add(ModBlocks.DEEPSLATE_ALUMINUM_ORE, this.createOreDrop(ModBlocks.DEEPSLATE_ALUMINUM_ORE, ModItems.RAW_ALUMINUM));

			this.dropSelf(ModBlocks.MYTHRIL_BLOCK);
			this.dropSelf(ModBlocks.RAW_MYTHRIL_BLOCK);
			this.dropSelf(ModBlocks.ALUMINUM_BLOCK);
			this.dropSelf(ModBlocks.RAW_ALUMINUM_BLOCK);
			this.dropSelf(ModBlocks.PLASTIC_BLOCK);
			this.dropSelf(ModBlocks.SHOP_BLOCK);
			this.dropSelf(ModBlocks.BLACKSMITH_SHOP);
			this.dropSelf(ModBlocks.GROCER_SHOP);
			this.dropSelf(ModBlocks.JEWELER_SHOP);
			this.dropSelf(ModBlocks.ALCHEMIST_SHOP);
			this.dropSelf(ModBlocks.GUILD_SHOP);
			this.dropSelf(ModBlocks.ENGRAVING_TABLE);
			this.dropSelf(ModBlocks.KITCHEN_STATION);
			this.dropSelf(ModBlocks.ALCHEMY_STATION);
			this.dropSelf(ModBlocks.SMITHING_STATION);
		}
	}

	public static class Entities extends FabricEntityLootSubProvider {
		public Entities(final FabricPackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
			super(output, registries);
		}

		@Override
		public void generate() {
			this.add(
				ModEntities.MINE_RAIDER,
				LootTable.lootTable()
					// 0-2 raw mythril (+0-1 per Looting level)
					.withPool(
						LootPool.lootPool()
							.setRolls(ContextIntProviders.exactly(1))
							.add(
								LootItem.lootTableItem(ModItems.RAW_MYTHRIL)
									.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 2)))
									.apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.enchantments, ContextFloatProviders.between(0.0F, 1.0F)))
							)
					)
					// 1-4 copper coins
					.withPool(
						LootPool.lootPool()
							.setRolls(ContextIntProviders.exactly(1))
							.add(LootItem.lootTableItem(ModItems.COPPER_COIN).apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 4))))
					)
					// 10% silver coin when killed by a player
					.withPool(
						LootPool.lootPool()
							.setRolls(ContextIntProviders.exactly(1))
							.add(LootItem.lootTableItem(ModItems.SILVER_COIN))
							.when(LootItemKilledByPlayerCondition.killedByPlayer())
							.when(LootItemRandomChanceCondition.randomChance(0.1F))
					)
			);

			this.add(
				ModEntities.MYTHRIL_GOLEM,
				LootTable.lootTable()
					// 1 block of raw mythril
					.withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).add(LootItem.lootTableItem(ModItems.RAW_MYTHRIL_BLOCK)))
					// 2-4 mythril ingots (+0-1 per Looting level)
					.withPool(
						LootPool.lootPool()
							.setRolls(ContextIntProviders.exactly(1))
							.add(
								LootItem.lootTableItem(ModItems.MYTHRIL_INGOT)
									.apply(SetItemCountFunction.setCount(ContextIntProviders.between(2, 4)))
									.apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.enchantments, ContextFloatProviders.between(0.0F, 1.0F)))
							)
					)
					// 3-6 silver coins
					.withPool(
						LootPool.lootPool()
							.setRolls(ContextIntProviders.exactly(1))
							.add(LootItem.lootTableItem(ModItems.SILVER_COIN).apply(SetItemCountFunction.setCount(ContextIntProviders.between(3, 6))))
					)
					// a gold coin when killed by a player
					.withPool(
						LootPool.lootPool()
							.setRolls(ContextIntProviders.exactly(1))
							.add(LootItem.lootTableItem(ModItems.GOLD_COIN))
							.when(LootItemKilledByPlayerCondition.killedByPlayer())
					)
					// the core needed for the third class advancement, always
					.withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).add(LootItem.lootTableItem(ModItems.GOLEM_CORE)))
			);
		}
	}

	private ModLootProviders() {
	}
}
