package com.minecraftmode.client.datagen;

import com.minecraftmode.registry.ModBlocks;
import com.minecraftmode.registry.ModItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.world.item.Item;

public class ModModelProvider extends FabricModelProvider {
	public ModModelProvider(final FabricPackOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(final BlockModelGenerators generators) {
		generators.createTrivialCube(ModBlocks.MYTHRIL_ORE);
		generators.createTrivialCube(ModBlocks.DEEPSLATE_MYTHRIL_ORE);
		generators.createTrivialCube(ModBlocks.MYTHRIL_BLOCK);
		generators.createTrivialCube(ModBlocks.RAW_MYTHRIL_BLOCK);
		generators.createTrivialCube(ModBlocks.ALUMINUM_ORE);
		generators.createTrivialCube(ModBlocks.DEEPSLATE_ALUMINUM_ORE);
		generators.createTrivialCube(ModBlocks.ALUMINUM_BLOCK);
		generators.createTrivialCube(ModBlocks.RAW_ALUMINUM_BLOCK);
		generators.createTrivialCube(ModBlocks.PLASTIC_BLOCK);
		// <shop>_side.png on all four sides, <shop>_top.png on top and bottom.
		generators.createTrivialBlock(ModBlocks.SHOP_BLOCK, TexturedModel.COLUMN);
		generators.createTrivialBlock(ModBlocks.BLACKSMITH_SHOP, TexturedModel.COLUMN);
		generators.createTrivialBlock(ModBlocks.GROCER_SHOP, TexturedModel.COLUMN);
		generators.createTrivialBlock(ModBlocks.JEWELER_SHOP, TexturedModel.COLUMN);
	}

	@Override
	public void generateItemModels(final ItemModelGenerators generators) {
		for (Item item : new Item[] {
			ModItems.RAW_MYTHRIL, ModItems.MYTHRIL_INGOT, ModItems.MYTHRIL_NUGGET,
			ModItems.RAW_ALUMINUM, ModItems.ALUMINUM_INGOT, ModItems.ALUMINUM_NUGGET,
			ModItems.PLASTIC_SHEET,
			ModItems.COPPER_COIN, ModItems.SILVER_COIN, ModItems.GOLD_COIN,
			ModItems.MYTHRIL_HELMET, ModItems.MYTHRIL_CHESTPLATE, ModItems.MYTHRIL_LEGGINGS, ModItems.MYTHRIL_BOOTS,
			ModItems.MINE_RAIDER_SPAWN_EGG, ModItems.MYTHRIL_GOLEM_SPAWN_EGG
		}) {
			generators.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
		}

		for (Item tool : new Item[] {
			ModItems.MYTHRIL_SWORD, ModItems.MYTHRIL_PICKAXE, ModItems.MYTHRIL_AXE, ModItems.MYTHRIL_SHOVEL, ModItems.MYTHRIL_HOE
		}) {
			generators.generateFlatItem(tool, ModelTemplates.FLAT_HANDHELD_ITEM);
		}
	}
}
