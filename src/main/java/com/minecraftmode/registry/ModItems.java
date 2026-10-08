package com.minecraftmode.registry;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.item.ClassResetScrollItem;
import com.minecraftmode.item.ModMaterials;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;

public final class ModItems {
	// Mythril
	public static final Item RAW_MYTHRIL = register("raw_mythril");
	public static final Item MYTHRIL_INGOT = register("mythril_ingot");
	public static final Item MYTHRIL_NUGGET = register("mythril_nugget");
	public static final Item MYTHRIL_SWORD = register("mythril_sword", new Item.Properties().sword(ModMaterials.MYTHRIL_TOOL, 3.0F, -2.4F));
	public static final Item MYTHRIL_PICKAXE = register("mythril_pickaxe", new Item.Properties().pickaxe(ModMaterials.MYTHRIL_TOOL, 1.0F, -2.8F));
	public static final Item MYTHRIL_AXE = register("mythril_axe", new Item.Properties().axe(ModMaterials.MYTHRIL_TOOL, 5.5F, -3.05F));
	public static final Item MYTHRIL_SHOVEL = register("mythril_shovel", new Item.Properties().shovel(ModMaterials.MYTHRIL_TOOL, 1.5F, -3.0F));
	public static final Item MYTHRIL_HOE = register("mythril_hoe", new Item.Properties().hoe(ModMaterials.MYTHRIL_TOOL, -2.5F, -0.5F));
	public static final Item MYTHRIL_HELMET = register("mythril_helmet", new Item.Properties().humanoidArmor(ModMaterials.MYTHRIL_ARMOR, ArmorType.HELMET));
	public static final Item MYTHRIL_CHESTPLATE = register(
		"mythril_chestplate", new Item.Properties().humanoidArmor(ModMaterials.MYTHRIL_ARMOR, ArmorType.CHESTPLATE)
	);
	public static final Item MYTHRIL_LEGGINGS = register("mythril_leggings", new Item.Properties().humanoidArmor(ModMaterials.MYTHRIL_ARMOR, ArmorType.LEGGINGS));
	public static final Item MYTHRIL_BOOTS = register("mythril_boots", new Item.Properties().humanoidArmor(ModMaterials.MYTHRIL_ARMOR, ArmorType.BOOTS));

	// Aluminum
	public static final Item RAW_ALUMINUM = register("raw_aluminum");
	public static final Item ALUMINUM_INGOT = register("aluminum_ingot");
	public static final Item ALUMINUM_NUGGET = register("aluminum_nugget");

	// Plastic
	public static final Item PLASTIC_SHEET = register("plastic_sheet");

	// Economy: 9 copper = 1 silver, 9 silver = 1 gold
	public static final Item COPPER_COIN = register("copper_coin");
	public static final Item SILVER_COIN = register("silver_coin", new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item GOLD_COIN = register("gold_coin", new Item.Properties().rarity(Rarity.RARE));

	// Classes: essence (engraving/advancement currency), advancement items
	public static final Item ESSENCE = register("essence", new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item CONDENSED_ESSENCE = register("condensed_essence", new Item.Properties().rarity(Rarity.RARE));
	public static final Item GOLEM_CORE = register("golem_core", new Item.Properties().rarity(Rarity.RARE).stacksTo(16));
	public static final Item CLASS_RESET_SCROLL = register(
		"class_reset_scroll", ClassResetScrollItem::new, new Item.Properties().rarity(Rarity.EPIC).stacksTo(16)
	);

	// Display-only items shown by skill projectiles (not in any creative tab)
	public static final Item PROJECTILE_SHURIKEN = register("projectile_shuriken");
	public static final Item PROJECTILE_KUNAI = register("projectile_kunai");
	public static final Item PROJECTILE_KNIFE = register("projectile_knife");
	public static final Item PROJECTILE_BULLET = register("projectile_bullet");
	public static final Item PROJECTILE_CANNONBALL = register("projectile_cannonball");
	public static final Item PROJECTILE_ICICLE = register("projectile_icicle");
	public static final Item PROJECTILE_HARPOON = register("projectile_harpoon");

	// Mobs
	public static final Item MINE_RAIDER_SPAWN_EGG = register(
		"mine_raider_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.MINE_RAIDER)
	);
	public static final Item MYTHRIL_GOLEM_SPAWN_EGG = register(
		"mythril_golem_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.MYTHRIL_GOLEM)
	);

	// Block items
	public static final Item MYTHRIL_ORE = registerBlockItem(ModBlocks.MYTHRIL_ORE);
	public static final Item DEEPSLATE_MYTHRIL_ORE = registerBlockItem(ModBlocks.DEEPSLATE_MYTHRIL_ORE);
	public static final Item MYTHRIL_BLOCK = registerBlockItem(ModBlocks.MYTHRIL_BLOCK);
	public static final Item RAW_MYTHRIL_BLOCK = registerBlockItem(ModBlocks.RAW_MYTHRIL_BLOCK);
	public static final Item ALUMINUM_ORE = registerBlockItem(ModBlocks.ALUMINUM_ORE);
	public static final Item DEEPSLATE_ALUMINUM_ORE = registerBlockItem(ModBlocks.DEEPSLATE_ALUMINUM_ORE);
	public static final Item ALUMINUM_BLOCK = registerBlockItem(ModBlocks.ALUMINUM_BLOCK);
	public static final Item RAW_ALUMINUM_BLOCK = registerBlockItem(ModBlocks.RAW_ALUMINUM_BLOCK);
	public static final Item PLASTIC_BLOCK = registerBlockItem(ModBlocks.PLASTIC_BLOCK);
	public static final Item SHOP_BLOCK = registerBlockItem(ModBlocks.SHOP_BLOCK);
	public static final Item BLACKSMITH_SHOP = registerBlockItem(ModBlocks.BLACKSMITH_SHOP);
	public static final Item GROCER_SHOP = registerBlockItem(ModBlocks.GROCER_SHOP);
	public static final Item JEWELER_SHOP = registerBlockItem(ModBlocks.JEWELER_SHOP);
	public static final Item GUILD_SHOP = registerBlockItem(ModBlocks.GUILD_SHOP);
	public static final Item ENGRAVING_TABLE = registerBlockItem(ModBlocks.ENGRAVING_TABLE);

	private static Item register(final String name) {
		return register(name, Item::new, new Item.Properties());
	}

	private static Item register(final String name, final Item.Properties properties) {
		return register(name, Item::new, properties);
	}

	private static Item register(final String name, final Function<Item.Properties, Item> factory, final Item.Properties properties) {
		return register(ResourceKey.create(Registries.ITEM, MinecraftMode.id(name)), factory, properties);
	}

	private static Item registerBlockItem(final Block block) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(block));
		return register(key, p -> new BlockItem(block, p), new Item.Properties().useBlockDescriptionPrefix());
	}

	private static Item register(final ResourceKey<Item> key, final Function<Item.Properties, Item> factory, final Item.Properties properties) {
		Item item = factory.apply(properties.setId(key));
		if (item instanceof BlockItem blockItem) {
			blockItem.registerBlocks(Item.BY_BLOCK, item);
		}

		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static void init() {
	}

	private ModItems() {
	}
}
