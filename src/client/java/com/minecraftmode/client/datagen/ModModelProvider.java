package com.minecraftmode.client.datagen;

import com.minecraftmode.accessory.Accessories;
import com.minecraftmode.accessory.AccessoryDef;
import com.minecraftmode.companion.Companions;
import com.minecraftmode.consumable.ConsumableDef;
import com.minecraftmode.consumable.Consumables;
import com.minecraftmode.craft.Crops;
import com.minecraftmode.craft.FarmCropBlock;
import com.minecraftmode.craft.Fishing;
import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.job.gear.ArmorPieceDef;
import com.minecraftmode.job.gear.ClassArmor;
import com.minecraftmode.job.quest.Quests;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.WeaponDef;
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
		generators.createTrivialBlock(ModBlocks.ALCHEMIST_SHOP, TexturedModel.COLUMN);
		generators.createTrivialBlock(ModBlocks.FARMER_SHOP, TexturedModel.COLUMN);
		generators.createTrivialBlock(ModBlocks.FISHMONGER_SHOP, TexturedModel.COLUMN);
		generators.createTrivialBlock(ModBlocks.GUILD_SHOP, TexturedModel.COLUMN);
		// engraving_table_top / _side / _bottom
		generators.createTrivialBlock(ModBlocks.ENGRAVING_TABLE, TexturedModel.CUBE_BOTTOM_TOP);
		// lair_chest_side / _top, lair_cache_side / _top
		generators.createTrivialBlock(ModBlocks.LAIR_CHEST, TexturedModel.COLUMN);
		generators.createTrivialBlock(ModBlocks.LAIR_CACHE, TexturedModel.COLUMN);
		// <station>_top / _side / _bottom
		generators.createTrivialBlock(ModBlocks.KITCHEN_STATION, TexturedModel.CUBE_BOTTOM_TOP);
		generators.createTrivialBlock(ModBlocks.ALCHEMY_STATION, TexturedModel.CUBE_BOTTOM_TOP);
		generators.createTrivialBlock(ModBlocks.SMITHING_STATION, TexturedModel.CUBE_BOTTOM_TOP);
		// <crop>_crop_stage0..3 (FarmArtist); also the flat model of what is planted (the seed, or the crop itself)
		for (Crops.CropDef def : Crops.all()) {
			generators.createCropBlock(Crops.block(def), FarmCropBlock.AGE, 0, 1, 2, 3);
		}
	}

	@Override
	public void generateItemModels(final ItemModelGenerators generators) {
		for (Item item : new Item[] {
			ModItems.RAW_MYTHRIL, ModItems.MYTHRIL_INGOT, ModItems.MYTHRIL_NUGGET,
			ModItems.RAW_ALUMINUM, ModItems.ALUMINUM_INGOT, ModItems.ALUMINUM_NUGGET,
			ModItems.PLASTIC_SHEET,
			ModItems.COPPER_COIN, ModItems.SILVER_COIN, ModItems.GOLD_COIN,
			ModItems.MYTHRIL_HELMET, ModItems.MYTHRIL_CHESTPLATE, ModItems.MYTHRIL_LEGGINGS, ModItems.MYTHRIL_BOOTS,
			ModItems.MINE_RAIDER_SPAWN_EGG, ModItems.MYTHRIL_GOLEM_SPAWN_EGG,
			ModItems.ESSENCE, ModItems.CONDENSED_ESSENCE, ModItems.GOLEM_CORE, ModItems.CLASS_RESET_SCROLL, ModItems.EVOLUTION_ETHER,
			ModItems.PROJECTILE_SHURIKEN, ModItems.PROJECTILE_KUNAI, ModItems.PROJECTILE_KNIFE, ModItems.PROJECTILE_BULLET,
			ModItems.PROJECTILE_CANNONBALL, ModItems.PROJECTILE_ICICLE, ModItems.PROJECTILE_HARPOON
		}) {
			generators.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
		}
		for (ArmorPieceDef piece : ClassArmor.pieces()) {
			generators.generateFlatItem(ClassArmor.item(piece), ModelTemplates.FLAT_ITEM);
		}
		for (NamedDef def : NamedMobs.all()) {
			generators.generateFlatItem(NamedMobs.egg(def), ModelTemplates.FLAT_ITEM);
		}
		for (Item token : Quests.tokens()) {
			generators.generateFlatItem(token, ModelTemplates.FLAT_ITEM);
		}
		for (ConsumableDef def : Consumables.all()) {
			generators.generateFlatItem(Consumables.item(def), ModelTemplates.FLAT_ITEM);
		}
		generators.generateFlatItem(ModItems.RETURN_SCROLL, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(ModItems.GEAR_BAG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(ModItems.SUPPLY_BAG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(ModItems.ORE_BAG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(ModItems.ENHANCEMENT_STONE, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(ModItems.PROTECTION_SCROLL, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(ModItems.LAIR_MAP, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(ModItems.LAND_DEED, ModelTemplates.FLAT_ITEM);
		for (Item item : new Item[] {
			ModItems.AWAKENING_CRYSTAL, ModItems.ACCESSORY_ORB, ModItems.ACCESSORY_INHERIT_1, ModItems.ACCESSORY_INHERIT_2, ModItems.TITAN_SHARD, ModItems.DUNGEON_KEYSTONE,
			ModItems.SUNLEAF, ModItems.MOONPETAL, ModItems.FROSTROOT, ModItems.GLOWCAP, ModItems.EMBERBLOOM, ModItems.VOIDCAP
		}) {
			generators.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
		}
		for (Crops.CropDef def : Crops.all()) {
			if (Crops.seed(def) != null) {
				generators.generateFlatItem(Crops.produce(def), ModelTemplates.FLAT_ITEM);
			}
		}
		for (Item fish : Fishing.items()) {
			generators.generateFlatItem(fish, ModelTemplates.FLAT_ITEM);
		}
		for (Companions.PetDef def : Companions.pets()) {
			generators.generateFlatItem(Companions.petItem(def.id()), ModelTemplates.FLAT_ITEM);
		}
		for (Companions.MountDef def : Companions.mounts()) {
			generators.generateFlatItem(Companions.mountItem(def.id()), ModelTemplates.FLAT_ITEM);
		}

		for (Item tool : new Item[] {
			ModItems.MYTHRIL_SWORD, ModItems.MYTHRIL_PICKAXE, ModItems.MYTHRIL_AXE, ModItems.MYTHRIL_SHOVEL, ModItems.MYTHRIL_HOE
		}) {
			generators.generateFlatItem(tool, ModelTemplates.FLAT_HANDHELD_ITEM);
		}

		// Raid accessories: icons come from AccessoryAssetProvider.
		for (AccessoryDef def : Accessories.all()) {
			generators.generateFlatItem(Accessories.item(def), ModelTemplates.FLAT_ITEM);
		}

		// Class weapons: textures come from WeaponTextureProvider; bows get vanilla-style pulling models.
		for (WeaponDef def : JobWeapons.all()) {
			Item item = JobWeapons.item(def);
			switch (def.archetype()) {
				case SHORTBOW, LONGBOW, GREATBOW -> {
					generators.createFlatItemModel(item, ModelTemplates.BOW);
					generators.generateBow(item);
				}
				case SHURIKEN, ORB, GRIMOIRE, KNUCKLE, GREATSHIELD, WAR_FAN, CHAKRAM -> generators.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
				default -> generators.generateFlatItem(item, ModelTemplates.FLAT_HANDHELD_ITEM);
			}
		}
	}
}
